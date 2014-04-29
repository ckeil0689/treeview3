/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: TreeViewFrame.java,v $w
 * $Revision: 1.76 $
 * $Date: 2010-05-02 13:34:53 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. 
 * Modified by Alex Segal 2004/08/13. Modifications Copyright (C) 
 * Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular, 
 *
 * 1) If you modify a source file, make a comment in it containing your name 
 * and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView 
 * maintainers at alok@genome.stanford.edu when they make a useful addition. 
 * It would be nice if significant contributions could be merged into 
 * the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import Controllers.DendroController;
import Views.LoadErrorView;
import Views.WelcomeView;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.core.FileMru;
import edu.stanford.genetics.treeview.core.FileMruEditor;
import edu.stanford.genetics.treeview.core.LogMessagesPanel;
import edu.stanford.genetics.treeview.core.LogSettingsPanel;
import edu.stanford.genetics.treeview.core.MenuHelpPluginsFrame;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView2;

/**
 * This class is the main window of TreeView 3. It holds all views 
 * to be displayed and serves as parent JFrame to other windows.
 * 
 * @author ckeil
 * 
 */
public class TreeViewFrame extends ViewFrame implements FileSetListener, 
ConfigNodePersistent {

	// Instance Variables
	protected JPanel backgroundPanel;
	protected JPanel menuPanel;
	protected JPanel waiting;
	protected DendroPanel running;
	protected DataModel dataModel;
	protected JDialog presetsFrame = null;
	protected TabbedSettingsPanel presetsPanel = null;

	private final TreeViewApp treeView;
	private ProgramMenu programMenu;
	
	// Different Views
	private WelcomeView welcomeView;
	private LoadErrorView loadErrorView;
	private DendroView2 dendroView;
	
	private String loadErrorMessage;
	
	private DendroController dendroController;
	
	private FileSet fileMenuSet;
	
	// Menu Items
	private ArrayList<JMenuItem> menuList;
	private ArrayList<JMenuItem> stackMenuList;
	private ArrayList<JMenuItem> fileMenuList;
	
//	private JButton stackButton;
	private JButton searchButton;
	private JButton treeButton;
	
//	private JPopupMenu stackMenu;
	private JMenuBar menuBar;
	
	private boolean loaded;

	// Constructors
	/**
	 * Chained constructor
	 * 
	 * @param TreeViewApp
	 *            treeview
	 */
	public TreeViewFrame(final TreeViewApp treeview) {

		this(treeview, StringRes.appName);
	}

	/**
	 * Main Constructor
	 * 
	 * @param TreeViewApp
	 *            treeview
	 * @param String
	 *            appName
	 */
	public TreeViewFrame(final TreeViewApp treeview, final String appName) {

		super(appName);
		treeView = treeview;
		configNode = treeView.getGlobalConfig().node("TreeViewFrame");
		
		setWindowActive(true);

		// Set up other stuff
		setupMainPanels();
		setupPresets();	
		
//		setupMenuBar();
		buildMenu();

		setupFrameSize();
		setLoaded(false);
	}
	
	/**
	 * Initializes all the main panels that make up TreeView.
	 */
	public void setupMainPanels() {
		
		backgroundPanel = new JPanel();
		backgroundPanel.setLayout(new MigLayout("ins 0"));
		
		menuPanel = new JPanel();
		menuPanel.setLayout(new MigLayout("ins 0"));
		
		waiting = new JPanel();
		waiting.setLayout(new MigLayout("ins 0"));
		
		backgroundPanel.add(menuPanel, "pushx, grow, wrap");
		backgroundPanel.add(waiting, "pushx, grow, h 98%");
		
		// Add the main background panel to the contentPane
		applicationFrame.getContentPane().add(backgroundPanel);
	}
	
	@Override
	public void setConfigNode(Preferences parentNode) {
		
		if (parentNode != null) {
			configNode = parentNode.node("TreeViewFrame");
		}
	}
	
	@Override
	public void saveSettings() {
		
		if(dendroController != null) {
			dendroController.saveSettings();
		}
	}
	
	// Setting different views
	/** 
	 * Choosing JPanel to be displayed.
	 */
	@Override
	public void setView(String viewName) {
		
		resetViews();
		
		JPanel view = new JPanel();
		
		// Set view dependent of 'loaded'
		if(!viewName.equalsIgnoreCase("DendroView")) {
			if(viewName.equalsIgnoreCase("WelcomeView")) {
				welcomeView = new WelcomeView(this);
				view = welcomeView.makeInitial();
				
			} else if(viewName.equalsIgnoreCase("LoadProgressView")) {
				welcomeView = new WelcomeView(this);
				view = welcomeView.makeLoading();
				
			} else if(viewName.equalsIgnoreCase("LoadErrorView")) {
				loadErrorView = new LoadErrorView(this, loadErrorMessage);
				view = loadErrorView.makeErrorPanel();
				
			} else {
				view.setLayout(new MigLayout());
				view.setOpaque(false);
				
				JLabel error = new JLabel("No view could be loaded.");
				error.setFont(GUIParams.FONTL);
				error.setForeground(GUIParams.TEXT);
				
				view.add(error, "push, alignx 50%");
			}
			
			// Rebuild Menus
			buildMenu();
			
		} else {
			dendroView = new DendroView2(this);
			setRunning(dendroView);
			
			// Rebuild Menus
			buildMenu();
			
			dendroController = new DendroController(dendroView, this, 
					(TVModel)dataModel);
			
			view = dendroView.makeDendroPanel();
		}
		
		displayView(view);
	}
	
	/**
	 * Sets all views to null to free up memory.
	 */
	public void resetViews() {
		
		welcomeView = null;
		dendroView = null;
	}
	
	/**
	 * Displays a screen to notify the user of a loading issue. Offers
	 * the user to load a different file by providing the appropriate button.
	 * @param errorMessage
	 */
	public void setLoadErrorMessage(String errorMessage) {
		
		this.loadErrorMessage = errorMessage;
	}
	
	/**
	 * Setting the JPanel to be displayed within TVFrame
	 * @param view
	 */
	public void displayView(JPanel view) {
		
		waiting.removeAll();
		waiting.setBackground(GUIParams.BG_COLOR);
		waiting.add(view, "push, grow");
		
		backgroundPanel.setBackground(GUIParams.BG_COLOR);
		backgroundPanel.revalidate();
	}
	
	/**
	 * Displays an editor for recently used files.
	 */
	public void showRecentFileEditor() {
		
		final FileMruEditor fme = new FileMruEditor(fileMru);
		fme.showDialog(applicationFrame);
	}
	
	/**
	 * Shows a panel which displays the current stats of the loaded model.
	 */
	public void openStatsView() {
		
		StatsPanel stats = new StatsPanel(TreeViewFrame.this);
		stats.setVisible(true);
	}
	
	/**
	 * Displays messages in a JDialog which were logged by TreeView.
	 */
	public void showLogMessages() {
		
		final JPanel inner = new JPanel();
		inner.setLayout(new BorderLayout());
		inner.add(new JLabel("JTV Messages"), BorderLayout.NORTH);
		inner.add(
				new JScrollPane(new LogMessagesPanel(LogBuffer
						.getSingleton())), BorderLayout.CENTER);

		final LogBuffer buffer = LogBuffer.getSingleton();
		buffer.setLog(true);
		inner.add(new LogSettingsPanel(buffer), BorderLayout.SOUTH);

		final JDialog top = new JDialog(applicationFrame,
				"JTV Messages", false);
		top.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		top.setContentPane(inner);
		top.pack();
		top.setLocationRelativeTo(applicationFrame);
		top.setVisible(true);
	}
	
	/**
	 * Displays a window with some helpful information about TreeView 3.
	 */
	public void showAboutWindow() {
		
		new AboutDialog(this).openAboutDialog();
	}
	
	/**
	 * opens a helpful screen with links to documentation for TreeView.
	 */
	public void showDocumentation() {
		
		final JPanel message = new JPanel();
		message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));
		message.add(new JLabel(StringRes.appName
				+ " documentation is available from the website."));

		final String docUrl = StringRes.updateUrl + "/manual.html";
		message.add(new JTextField(docUrl));

		final JButton lButton = new JButton("Launch Browser");
		lButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {

				displayURL(docUrl);
			}
		});
		message.add(lButton);
		JOptionPane.showMessageDialog(applicationFrame, message);
	}
	
	/**
	 * Opens a window to let the user give feedback.
	 */
	public void showFeedbackDialog() {
		
		final JPanel feedback = new JPanel();
		feedback.setLayout(new BoxLayout(feedback, BoxLayout.Y_AXIS));
		
		JComponent tmp = new JLabel("Please report bugs to ");
		tmp.setAlignmentX((float) 0.0);
		feedback.add(tmp);

		tmp = new JLabel("For support, bugs reports, and requests " +
				"send email to ");
		tmp.setAlignmentX((float) 0.0);
		feedback.add(tmp);
		final String supportURL = "ckeil@princeton.edu";
		tmp = new JTextField(supportURL);
		// tmp.setAlignmentX((float) 1.0);
		feedback.add(tmp);
		JButton yesB = new JButton("Email Support");
		yesB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				displayURL("mailto:" + supportURL);
			}
		});
		feedback.add(yesB);

		JOptionPane.showMessageDialog(applicationFrame, feedback,
				"Feedback...", JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Shows a "Work in Progress" message for unfinished components.
	 */
	public void displayWIP() {
		
		final JDialog dialog = new JDialog(applicationFrame);
		
		Dimension screenSize = GUIParams.getScreenSize();
		dialog.setSize(new Dimension(screenSize.width * 1/2, 
				screenSize.height * 1/2));
		
		dialog.setLocationRelativeTo(applicationFrame);
		dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		final JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());
		panel.setBackground(GUIParams.BG_COLOR);

		final JLabel l1 = new JLabel("Work in progress.");
		l1.setFont(GUIParams.FONTS);
		l1.setForeground(GUIParams.TEXT);
		
		panel.add(l1, "push, alignx 50%");
		dialog.add(panel);
		
		dialog.setVisible(true);
	}
	
	// Getters for Views
	/**
	 * Returns TVFrame's current WelcomeView instance
	 * @return welcomeView
	 */
	public WelcomeView getWelcomeView() {
		
		return welcomeView;
	}
	
	/**
	 * Returns TVFrame's current LoadErrorView instance
	 * @return welcomeView
	 */
	public LoadErrorView getLoadErrorView() {
		
		return loadErrorView;
	}
	
	/**
	 * Returns TVFrame's current DendroView instance
	 * @return dendroView
	 */
	public DendroView2 getDendroView() {
		
		return dendroView;
	}
	
	/**
	 * Sets the currently running panel for TVFrame. TVFrame is updated by
	 * setLoaded(true).
	 * @param panel
	 */
	public void setRunning(DendroPanel panel) {
		
		running = panel;
	}

	// Observer
	@Override
	public void update(final Observable observable, final Object object) {
		
		System.out.println("Updating fileMRU in TVFrame.");
		
		if (observable == fileMru) {
			// System.out.println("Rebuilding file menu");
//			programMenu.rebuild();

		} else {
			System.out.println("Got weird update");
		}
	}

	/**
	 * This should be called whenever the loaded status changes It's
	 * responsibility is to change the look of the main view only
	 * 
	 * @param boolean flag
	 */
	@Override
	public void setLoaded(final boolean flag) {

		// reset persistent popups
//		setGeneFinder(null);
		loaded = flag;
//		backgroundPanel.removeAll();
//		waiting.removeAll();

		if (loaded) {
			if (running == null) {
				JOptionPane.showMessageDialog(applicationFrame, 
						"TreeViewFrame 253: No plugins to display");
			} else {
				setLoadedTitle();
//				treeView.getGlobalConfig().store();
			}

		} else {
//			backgroundPanel.add(waiting);
			applicationFrame.setTitle(StringRes.appName);
		}

		// menubar.rebuild...
//		rebuildMainPanelMenu();
//		treeView.rebuildWindowMenus();
	}

	/**
	 * Builds the two menu buttons for TVFrame.
	 */
	public void buildMenu() {
		
		menuPanel.removeAll();
		menuPanel.setBackground(GUIParams.BG_COLOR);
		menuBar = new JMenuBar();
		applicationFrame.setJMenuBar(menuBar);
		
		generateMenuBar();
		
		if(running != null) {
			generateSearchMenu();
			generateTreeMenu();
		}
		
		// Only for debugging Preferences API
//		JButton clearButton = GUIParams.setButtonLayout("Mac MenuBar Property", 
//				null);
//		clearButton.addActionListener(new ActionListener() {
//
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//	
//				try {
//					treeView.getGlobalConfig().removeNode();
//				} catch (BackingStoreException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				LogBuffer.println(System.getProperty(
//						"apple.laf.useScreenMenuBar"));
//			}
//		});
//		menuPanel.add(clearButton);
	}
	
	public void generateMenuBar() {
		
		stackMenuList = new ArrayList<JMenuItem>();
		
		// File
		JMenu fileSubMenu = new JMenu("File");
		// Open new file Menu
		JMenuItem openMenuItem = new JMenuItem("Open");
		fileSubMenu.add(openMenuItem);
		stackMenuList.add(openMenuItem);
	
		// menubar.addMenuItem("Open Url...", new ActionListener() {
		// public void actionPerformed(ActionEvent actionEvent) {
		// try {
		// FileSet fileSet = offerUrlSelection();
		// loadFileSet(fileSet);
		// fileSet = fileMru.addUnique(fileSet);
		// fileMru.setLast(fileSet);
		// fileMru.notifyObservers();
		// setLoaded(true);
		// } catch (LoadException e) {
		// LogBuffer.println("could not load url: "
		// + e.getMessage());
		// // setLoaded(false);
		// }
		// }
		// });
		// menubar.setMnemonic(KeyEvent.VK_U);
		
		//-------
		fileMenuList = new ArrayList<JMenuItem>();
		
		JMenu recentSubMenu = new JMenu("Open Recent");
		
		final Preferences[] aconfigNode = fileMru.getConfigs();
		final String astring[] = fileMru.getFileNames();

		for (int j = aconfigNode.length; j > 0; j--) {
			
			fileMenuSet = new FileSet(aconfigNode[j - 1]);
			
			JMenuItem fileSetMenuItem = new JMenuItem(astring[j - 1]);
			recentSubMenu.add(fileSetMenuItem);
			fileMenuList.add(fileSetMenuItem);
		}
		
		fileSubMenu.add(recentSubMenu);
		
		JMenuItem editRecentMenuItem = new JMenuItem("Edit Recent Files");
		fileSubMenu.add(editRecentMenuItem);
		stackMenuList.add(editRecentMenuItem);
		//--------

		fileSubMenu.addSeparator();
		
		JMenuItem saveMenuItem = new JMenuItem("Save");
		fileSubMenu.add(saveMenuItem);
		stackMenuList.add(saveMenuItem);

		JMenuItem saveAsMenuItem = new JMenuItem("Save as");
		fileSubMenu.add(saveAsMenuItem);
		stackMenuList.add(saveAsMenuItem);
		
		menuBar.add(fileSubMenu);

		if(running != null) {
			JMenu viewMenu = new JMenu("View");
			running.addDendroMenus(viewMenu);
			menuBar.add(viewMenu);
			
			JMenu clusterMenu = new JMenu("Cluster");
			running.addClusterMenus(clusterMenu);
			menuBar.add(clusterMenu);
			
			// Functional Enrichment Menu
//			JMenuItem funcEnrMenuItem = new JMenuItem("Functional Enrichment");
//			stackMenu.add(funcEnrMenuItem);
//			stackMenuList.add(funcEnrMenuItem);
		}
		
		// Preferences
		JMenu prefSubMenu = new JMenu("Preferences");
		
		if(running == null) {
			JMenuItem themeMenuItem = new JMenuItem(StringRes.menu_title_Theme);
			stackMenuList.add(themeMenuItem);
			prefSubMenu.add(themeMenuItem);
			
			prefSubMenu.addSeparator();
			
			JMenuItem clearPrefsMenuItem = new JMenuItem(
					StringRes.menubar_clearPrefs);
			stackMenuList.add(clearPrefsMenuItem);
			prefSubMenu.add(clearPrefsMenuItem);
			
		} else {
			JMenuItem themeMenuItem = new JMenuItem(StringRes.menu_title_Theme);
			stackMenuList.add(themeMenuItem);
			prefSubMenu.add(themeMenuItem);
			
			JMenuItem fontMenuItem = new JMenuItem(StringRes.menu_title_Font);
			stackMenuList.add(fontMenuItem);
			prefSubMenu.add(fontMenuItem);
			
			JMenuItem urlMenuItem = new JMenuItem(StringRes.menu_title_URL);
			stackMenuList.add(urlMenuItem);
			prefSubMenu.add(urlMenuItem);
			
			prefSubMenu.addSeparator();
			
//			JMenuItem clearPrefsMenuItem = new JMenuItem(
//					StringRes.menubar_clearPrefs);
//			stackMenuList.add(clearPrefsMenuItem);
//			prefSubMenu.add(clearPrefsMenuItem);
		}

		fileSubMenu.addSeparator();
		fileSubMenu.add(prefSubMenu);
		
		// Help
		JMenu helpSubMenu = new JMenu("Help");
		
		if(running != null) {
			JMenuItem statsMenuItem = new JMenuItem("Stats");
			helpSubMenu.add(statsMenuItem);
			stackMenuList.add(statsMenuItem);
		}
					
		JMenuItem aboutMenuItem = new JMenuItem("About...");
		helpSubMenu.add(aboutMenuItem);
		stackMenuList.add(aboutMenuItem);

		JMenuItem logMenuItem = new JMenuItem("Show Log");
		helpSubMenu.add(logMenuItem);
		stackMenuList.add(logMenuItem);

		JMenuItem documentMenuItem = new JMenuItem("Documentation...");
		helpSubMenu.add(documentMenuItem);
		stackMenuList.add(documentMenuItem);

//				menubar.addMenuItem("Plugins...");
//				, new ActionListener() {
//
//					@Override
//					public void actionPerformed(final ActionEvent actionEvent) {
//
//						displayPluginInfo();
//					}
//				});

//				menubar.addMenuItem("Registration...");
//				, new ActionListener() {
//
//					@Override
//					public void actionPerformed(final ActionEvent actionEvent) {
//
//						final ConfigNode node = treeView.getGlobalConfig().getNode(
//								"Registration");
//						if (node != null) {
//							try {
//								edu.stanford.genetics.treeview.reg.RegEngine
//										.reverify(node);
//							} catch (final Exception e) {
//								LogBuffer.println("registration error " + e);
//								e.printStackTrace();
//							}
//						}
//					}
//				});

		JMenuItem feedbackMenuItem = new JMenuItem("Feedback");
		helpSubMenu.add(feedbackMenuItem);
		stackMenuList.add(feedbackMenuItem);
		
		menuBar.add(helpSubMenu);
		
		fileSubMenu.addSeparator();
		
		// New Window
		JMenuItem newWindowMenuItem = new JMenuItem("New Window");
		stackMenuList.add(newWindowMenuItem);
		fileSubMenu.add(newWindowMenuItem);
		
		// Quit Program Menu
		JMenuItem quitMenuItem = new JMenuItem("Quit Program");
		fileSubMenu.add(quitMenuItem);
		stackMenuList.add(quitMenuItem);
	}
	
	public void generateSearchMenu() {
		
		searchButton = GUIParams.setMenuButtonLayout("SEARCH LABELS", null);
		searchButton.setToolTipText("Find Row or Column Elements");
		
		menuPanel.add(searchButton);
	}
	
	public void generateTreeMenu() {
		
		treeButton = GUIParams.setMenuButtonLayout("SHOW TREES", null);
		treeButton.setToolTipText("Determine Dendrogram Visibility");
		
		// Initially disabled, will be enabled if gtrview or atrview in
		// DendroView are enabled.
		treeButton.setEnabled(false);
		
		menuPanel.add(treeButton);
	}

//	/**
//	 * This method sets up a JDialog to call an option for the user to change
//	 * some of the presets.
//	 */
//	protected void setupPresetsPanel() {
//
//		presetsFrame = new JDialog(TreeViewFrame.this, "Presets", true);
//		presetsPanel = new TabbedSettingsPanel();
//
//		UrlPresetsEditor presetEditor = new UrlPresetsEditor(
//				getGeneUrlPresets());
//		presetEditor.setTitle("Gene Url");
//		presetsPanel.addSettingsPanel("Gene", presetEditor);
//
//		presetEditor = new UrlPresetsEditor(getArrayUrlPresets());
//		presetEditor.setTitle("Array Url");
//		presetsPanel.addSettingsPanel("Array", presetEditor);
//
//		final SettingsPanelHolder innerPanel = new SettingsPanelHolder(
//				presetsFrame, getApp().getGlobalConfig().getRoot());
//		innerPanel.addSettingsPanel(presetsPanel);
//
//		presetsFrame.getContentPane().add(innerPanel);
//		presetsFrame.pack();
//	}

	// Populating various menus
	/**
	 * This methods populates the Settings menu with several MenuItems and
	 * subMenus.
	 * 
	 * @param menubar
	 */
	protected void populateSettingsMenu(final TreeviewMenuBarI menubar) {

		menubar.setMenu(TreeviewMenuBarI.documentMenu);
		
//		menubar.addMenuItem("Gene Url", new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent actionEvent) {
//
//				if (presetsPanel == null) {
//					setupPresetsPanel();
//				}
//
//				presetsPanel.synchronizeFrom();
//				presetsPanel.setSelectedIndex(0);
//				presetsFrame.setVisible(true);
//			}
//		});
//		menubar.setAccelerator(KeyEvent.VK_P);
//		menubar.setMnemonic(KeyEvent.VK_G);
//
//		menubar.addMenuItem("Array Url", new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent actionEvent) {
//				if (presetsPanel == null) {
//					setupPresetsPanel();
//				}
//				presetsPanel.synchronizeFrom();
//				presetsPanel.setSelectedIndex(1);
//				presetsFrame.setVisible(true);
//			}
//		});
//		menubar.setMnemonic(KeyEvent.VK_A);
//
//		final PluginFactory[] plugins = PluginManager.getPluginManager()
//				.getPluginFactories();
//
//		if (plugins.length == 0) {
//			menubar.addMenuItem("Color", new ActionListener() {
//
//				@Override
//				public void actionPerformed(final ActionEvent actionEvent) {
//					if (presetsPanel == null)
//						setupPresetsPanel();
//					presetsPanel.synchronizeFrom();
//					presetsPanel.setSelectedIndex(2);
//					presetsFrame.setVisible(true);
//				}
//			});
//			menubar.setMnemonic(KeyEvent.VK_C);
//		} else {
//			for (int i = 0; i < plugins.length; i++) {
//				plugins[i].addPluginConfig(menubar, this);
//			}
//		}

		// menubar.addSeparator();
		//
		// menubar.addMenuItem("Change Menubar", new ActionListener() {
		// public void actionPerformed(ActionEvent actionEvent) {
		// getApp().getPrefs().showEditor();
		// getApp().getGlobalConfig().store();
		// }
		// });
	}

	/**
	 * This method populates the Export Menu with MenuItems.
	 * 
	 * @param menubar2
	 */
	protected void populateExportMenu(final TreeviewMenuBarI menubar) {

		/*
		 * MenuItem menuItem2 = new MenuItem("Export to Text File... ");
		 * menuItem2.addActionListener(new ActionListener() { public void
		 * actionPerformed(ActionEvent actiosnEvent) { ViewFrame viewFrame =
		 * TreeViewFrame.this; FileSet source = getDataModel().getFileSet();
		 * GeneListMaker t = new GeneListMaker(viewFrame, getGeneSelection(),
		 * getDataModel().getGeneHeaderInfo(), source.getDir()+source.getRoot()
		 * + ".txt"); t.setDataMatrix(getDataModel().getDataMatrix(),
		 * getDataModel().getArrayHeaderInfo(), DataModel.NODATA);
		 * t.bindConfig(getDataModel().getDocumentConfig()
		 * .getNode("GeneListMaker")); t.makeList(); } });
		 * exportMenu.add(menuItem2);
		 */

		// Save List Menu
		JMenuItem saveListMenuItem = 
				(JMenuItem) menubar.addMenuItem("Save List");
		menubar.setMnemonic(KeyEvent.VK_L);
		menuList.add(saveListMenuItem);

		// Save Data Menu
		JMenuItem saveDataMenuItem = 
				(JMenuItem) menubar.addMenuItem("Save Data"); 
		menubar.setMnemonic(KeyEvent.VK_D);
		menuList.add(saveDataMenuItem);
	}

	/**
	 * Populates the Analysis Menu with MenuItems and subMenus
	 * 
	 * @param menubar2
	 */
	protected void populateAnalysisMenu(final TreeviewMenuBarI menubar) {

		// Cluster Menu
		JMenuItem clusterMenuItem = (JMenuItem) menubar.addMenuItem("Cluster");
		menubar.setAccelerator(KeyEvent.VK_C);
		menuList.add(clusterMenuItem);
		
		menubar.setMenu(TreeviewMenuBarI.analysisMenu);
		menubar.addSeparator();
		
		// Functional Enrichment Menu
		JMenuItem funcEnrMenuItem = (JMenuItem) menubar.addMenuItem(
				"Functional Enrichment");
		menubar.setAccelerator(KeyEvent.VK_F);
		menuList.add(funcEnrMenuItem);
		
		// Stats Menu
		JMenuItem statsMenuItem = (JMenuItem) menubar.addMenuItem("Stats");
		menubar.setAccelerator(KeyEvent.VK_S);
		menuList.add(statsMenuItem);
	}

	/**
	 * Populates the HelpMenu with MenuItems and subMenus.
	 * 
	 * @param menubar
	 */
	private void populateHelpMenu(final TreeviewMenuBarI menubar) {

		JMenuItem aboutMenuItem = (JMenuItem) menubar.addMenuItem("About...");
		menuList.add(aboutMenuItem);
		menubar.setMnemonic(KeyEvent.VK_A);

		JMenuItem logMenuItem = (JMenuItem) menubar.addMenuItem("Show Log");
		menuList.add(logMenuItem);
		menubar.setMnemonic(KeyEvent.VK_M);

		JMenuItem documentMenuItem = 
				(JMenuItem) menubar.addMenuItem("Documentation...");
		menuList.add(documentMenuItem);
		menubar.setMnemonic(KeyEvent.VK_D);

		menubar.addMenuItem("Plugins...");
//		, new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent actionEvent) {
//
//				displayPluginInfo();
//			}
//		});
		menubar.setMnemonic(KeyEvent.VK_P);

		menubar.addMenuItem("Registration...");
//		, new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent actionEvent) {
//
//				final ConfigNode node = treeView.getGlobalConfig().getNode(
//						"Registration");
//				if (node != null) {
//					try {
//						edu.stanford.genetics.treeview.reg.RegEngine
//								.reverify(node);
//					} catch (final Exception e) {
//						LogBuffer.println("registration error " + e);
//						e.printStackTrace();
//					}
//				}
//			}
//		});
		menubar.setMnemonic(KeyEvent.VK_R);

		JMenuItem feedbackMenuItem = (JMenuItem) menubar.addMenuItem("Feedback");
		menuList.add(feedbackMenuItem);
		menubar.setMnemonic(KeyEvent.VK_F);
		
//		menubar.addSeparator();
//
//		menubar.addMenuItem("Memory...");
//		, new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent e) {
//
//				final MemMonitor m = new MemMonitor();
//				m.start();
//			}
//		});
//		menubar.setMnemonic(KeyEvent.VK_M);
//
//		menubar.addMenuItem("Threads...");
//		, new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent e) {
//
//				final ThreadListener t = new ThreadListener();
//				t.start();
//			}
//		});
//		menubar.setMnemonic(KeyEvent.VK_T);
//
//		menubar.addMenuItem("Global Pref Info...");
//		, new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent e) {
//
//				final GlobalPrefInfo gpi = new GlobalPrefInfo(getApp());
//				JOptionPane.showMessageDialog(null, gpi, "Global Pref Info...",
//						JOptionPane.INFORMATION_MESSAGE);
//			}
//		});

		/*
		 * This is to help debug plugin instance naming. MenuItem pluginSearch =
		 * new MenuItem("Search for instances");
		 * pluginSearch.addActionListener(new ActionListener() { public void
		 * actionPerformed(ActionEvent e) { JTextField name = new
		 * JTextField(20); JOptionPane.showMessageDialog(TreeViewFrame.this,
		 * name); MainPanel [] list = getPluginsByName(name.getText()); JPanel
		 * res = new JPanel(); for (int i = 0; i < list.length; i++) res.add(new
		 * JLabel(list[i].getName()));
		 * JOptionPane.showMessageDialog(TreeViewFrame.this,res); } });
		 * menu.add(pluginSearch);
		 */
	}

	// Various Methods
	/**
	 * Displays the save dialog. Saving is handled by TVController.
	 * @param incremental
	 * @return
	 */
	public void openSaveDialog(final boolean writerEmpty, final String msg) {

		final JDialog dialog = new JDialog(applicationFrame);
		dialog.setTitle("Information");
		dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		dialog.setLocationRelativeTo(null);

		final JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());
		panel.setBackground(GUIParams.BG_COLOR);

		final JButton button = GUIParams.setButtonLayout("OK", null);
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {
				dialog.dispose();
			}
		});

		if (writerEmpty) {
			final JLabel l1 = new JLabel("No Model changes were written.");
			l1.setFont(GUIParams.FONTS);
			l1.setForeground(GUIParams.TEXT);

			final JLabel l2 = new JLabel("Only the following changes require "
					+ "explicit saving:");
			l2.setFont(GUIParams.FONTS);
			l2.setForeground(GUIParams.TEXT);

			final JLabel l3 = new JLabel("- Tree Node flips (Analysis->Flip "
					+ "Array/Gene Tree Node)");
			l3.setFont(GUIParams.FONTS);
			l3.setForeground(GUIParams.TEXT);

			final JLabel l4 = new JLabel("- Tree Node Annotations "
					+ "(Analysis->Array/Gene TreeAnno)");
			l4.setFont(GUIParams.FONTS);
			l4.setForeground(GUIParams.TEXT);

			panel.add(l1, "pushx, wrap");
			panel.add(l2, "pushx, wrap");
			panel.add(l3, "pushx, wrap");
			panel.add(l4, "pushx, wrap");
			panel.add(button, "pushx, alignx 50%");

			dialog.add(panel);

			dialog.pack();
			dialog.setVisible(true);

		} else {

			final JLabel l1 = new JLabel(msg);
			l1.setFont(GUIParams.FONTS);
			l1.setForeground(GUIParams.TEXT);

			panel.add(l1, "pushx, wrap");
			panel.add(button, "pushx, alignx 50%");

			dialog.add(panel);

			dialog.pack();
			dialog.setVisible(true);
		}
	}

	/**
	 * Subclass which exists to setup the "File" menu in the MenuBar.
	 * 
	 * @author CKeil
	 */
	private class ProgramMenu {

		/**
		 * Constructor
		 */
		ProgramMenu() {

			synchronized (menubar) {

				menubar.setMenu(TreeviewMenuBarI.programMenu);
				
				// Open new file Menu
				JMenuItem openMenuItem = 
						(JMenuItem) menubar.addMenuItem("Open");
				menubar.setAccelerator(KeyEvent.VK_O);
				menubar.setMnemonic(KeyEvent.VK_O);
				menuList.add(openMenuItem);
			

				// menubar.addMenuItem("Open Url...", new ActionListener() {
				// public void actionPerformed(ActionEvent actionEvent) {
				// try {
				// FileSet fileSet = offerUrlSelection();
				// loadFileSet(fileSet);
				// fileSet = fileMru.addUnique(fileSet);
				// fileMru.setLast(fileSet);
				// fileMru.notifyObservers();
				// setLoaded(true);
				// } catch (LoadException e) {
				// LogBuffer.println("could not load url: "
				// + e.getMessage());
				// // setLoaded(false);
				// }
				// }
				// });
				// menubar.setMnemonic(KeyEvent.VK_U);

				JMenuItem saveMenuItem = 
						(JMenuItem) menubar.addMenuItem("Save");
				menuList.add(saveMenuItem);
				menubar.setMnemonic(KeyEvent.VK_S);
				menubar.setAccelerator(KeyEvent.VK_S);

				JMenuItem saveAsMenuItem = 
						(JMenuItem) menubar.addMenuItem("Save as");
				menuList.add(saveAsMenuItem);

				menubar.addSubMenu(TreeviewMenuBarI.mruSubMenu);
				menubar.setMenuMnemonic(KeyEvent.VK_R);
				
				menubar.setMenu(TreeviewMenuBarI.programMenu);

				menubar.addSeparator();
				
				menubar.addSubMenu(TreeviewMenuBarI.prefSubMenu);
				
				populatePreferencesMenu(menubar);
				
				menubar.setMenu(TreeviewMenuBarI.programMenu);
				menubar.addSeparator();
				
				// Quit Program Menu
				JMenuItem quitMenuItem = 
						(JMenuItem)menubar.addMenuItem("Quit Program");
				menuList.add(quitMenuItem);
				menubar.setMnemonic(KeyEvent.VK_Q);
				menubar.setAccelerator(KeyEvent.VK_Q);
			}
		}

		/**
		 * This method removes all items from the MRUSubMenu and sets up new
		 * FileMenuListeners.
		 */
		public void rebuild() {
			
			fileMenuList = new ArrayList<JMenuItem>();
			
			synchronized (menubar) {
				menubar.setMenu(TreeviewMenuBarI.programMenu);
				menubar.setSubMenu(TreeviewMenuBarI.mruSubMenu);
				menubar.removeAll();

				final Preferences[] aconfigNode = fileMru.getConfigs();
				final String astring[] = fileMru.getFileNames();

				for (int j = aconfigNode.length; j > 0; j--) {
					
					fileMenuSet = new FileSet(aconfigNode[j - 1]);
					
					JMenuItem fileSetMenuItem = 
							(JMenuItem) menubar.addMenuItem(astring[j - 1]);
					fileMenuList.add(fileSetMenuItem);
				}
				
				menubar.addSeparator();
				JMenuItem editRecentMenuItem = 
						(JMenuItem) menubar.addMenuItem("Edit Recent Files");
				menuList.add(editRecentMenuItem);
				menubar.setMnemonic(KeyEvent.VK_E);
			}
		}
		
		/**
		 * Adds MenuItems to the Preferences subMenu.
		 * @param menubar
		 */
		public void populatePreferencesMenu(final TreeviewMenuBarI menubar) {

			menubar.setSubMenu(TreeviewMenuBarI.prefSubMenu);
			menubar.removeAll();
			
			if(running == null) {
				JMenuItem themeMenuItem = 
						(JMenuItem)menubar.addMenuItem("Theme");
				menuList.add(themeMenuItem);
				
			} else {
				JMenuItem themeMenuItem = 
						(JMenuItem)menubar.addMenuItem("Theme");
				menuList.add(themeMenuItem);
				
				JMenuItem fontMenuItem = 
						(JMenuItem)menubar.addMenuItem("Font");
				menuList.add(fontMenuItem);
				
				JMenuItem urlMenuItem = (JMenuItem)menubar.addMenuItem("URL");
				menuList.add(urlMenuItem);
			}
		}
	}

	@Override
	public double noData() {

		return DataModel.NODATA;
	}

	/**
	 * This method displays the current plugin info. I set it up as a method so
	 * that it can be overridden by AppletViewFrame
	 */
	protected void displayPluginInfo() {
		
		final MenuHelpPluginsFrame frame = new MenuHelpPluginsFrame(
				"Current Plugins", this);
		final File f_currdir = new File(".");

		try {
			frame.setSourceText(f_currdir.getCanonicalPath() + File.separator
					+ "plugins" + File.separator);
			
		} catch (final IOException e) {
			frame.setSourceText("Unable to read default plugins directory.");
			LogBuffer.println("IOException while trying to display " +
					"Plugin info: " + e.getMessage());
		}

		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
	}

//	@Override
//	public MainPanel[] getMainPanelsByName(final String name) {
//
//		if (running != null) {
//			// is the current running a match?
//			if (name.equals(running.getName())) {
//				final MainPanel[] list = new MainPanel[1];
//				list[0] = running;
//				return list;
//			}
//
//			// okay, is the current running a linkedPanel?
//			try {
//				final LinkedPanel linked = (LinkedPanel) running;
//				return linked.getMainPanelsByName(name);
//
//			} catch (final ClassCastException e) {
//				e.printStackTrace();
//			}
//
//		} else {
//			// fall through to end
//		}
//		final MainPanel[] list = new MainPanel[0];
//		return list;
//	}

//	@Override
//	public MainPanel[] getMainPanels() {
//		
//		if (running == null) {
//			final MainPanel[] list = new MainPanel[0];
//			return list;
//		}
//
//		// okay, is the current running a linkedPanel?
//		try {
//			final LinkedPanel linked = (LinkedPanel) running;
//			return linked.getMainPanels();
//		} catch (final ClassCastException e) {
//
//		}
//
//		final MainPanel[] list = new MainPanel[1];
//		list[0] = running;
//		return list;
//	}

	@Override
	public void scrollToGene(final int i) {

		dendroController.scrollToGene(i);
	}

	@Override
	public void scrollToArray(final int i) {

		dendroController.scrollToArray(i);
	}

	// Why do these methods exist? They appear to do nothing but
	// calling the setLoadedTitle() method...
	@Override
	public void onFileSetMoved(final FileSet fileset) {

		setLoadedTitle();
	}

	// Setters
	/**
	 * Sets the title of the app to the source filepath of the currently loaded
	 * dataModel
	 */
	private void setLoadedTitle() {

		applicationFrame.setTitle(StringRes.appName + " : " 
				+ dataModel.getSource());
	}
	
	@Override
	public void setDataModel(DataModel model) {
		
		this.dataModel = model;
	}
	
	// Adding MenuActionListeners
	public void addMenuActionListeners(ActionListener l) {
		
		for(JMenuItem item : stackMenuList){
			
			if(item.getActionListeners().length == 0) {
				item.addActionListener(l);
			}
		}
	}
	
	/**
	 * used for the views to add some exclusive JMenuItems
	 * @param menu
	 */
	public void addToStackMenuList(JMenuItem menu) {
		
		stackMenuList.add(menu);
	}
	
	/**
	 * Adds FileMenuListeners to list of recent files that can be loaded.
	 * @param listener
	 */
	public void addFileMenuListeners(ActionListener listener) {
		
		for(JMenuItem item : fileMenuList){
			
			if(item.getActionListeners().length == 0) {
				item.addActionListener(listener);
			}
		}
	}
	
//	public void addStackButtonListener(MouseListener l) {
//		
//		stackButton.addMouseListener(l);
//	}
	
	public void addSearchButtonListener(MouseListener l) {
		
		searchButton.addMouseListener(l);
	}
	
	public void addTreeButtonListener(MouseListener l) {
		
		treeButton.addMouseListener(l);
	}
	
//	public JPopupMenu getStackMenu() {
//		
//		return stackMenu;
//	}
	
	public FileSet getFileMenuSet() {
		
		return fileMenuSet;
	}
	
	public ArrayList<JMenuItem> getStackMenus() {
		
		return stackMenuList;
	}
	
//	/**
//	 * Setter for geneFinder
//	 * 
//	 * @param HeaderFinder
//	 *            geneFinder
//	 */
//	public void setGeneFinder(final HeaderFinder geneFinder) {
//
//		this.geneFinder = geneFinder;
//	}
//
//	/** Setter for geneFinder */
//	public void setArrayFinder(final HeaderFinder geneFinder) {
//
//		this.geneFinder = geneFinder;
//	}
	
	public void setArraySelection(TreeSelection aSelect) {
		
		this.arraySelection = aSelect;
	}
	
	public void setGeneSelection(TreeSelection aSelect) {
		
		this.geneSelection = aSelect;
	}

	// Getters
	@Override
	public TreeViewApp getApp() {

		return treeView;
	}

	@Override
	public UrlPresets getGeneUrlPresets() {

		return treeView.getGeneUrlPresets();
	}

	@Override
	public UrlPresets getArrayUrlPresets() {

		return treeView.getArrayUrlPresets();
	}

	@Override
	public boolean getLoaded() {

		return loaded;
	}

//	@Override
//	public HeaderFinder getGeneFinder() {
//
//		if (geneFinder == null) {
//			geneFinder = new GeneFinder(TreeViewFrame.this, getDataModel()
//					.getGeneHeaderInfo(), getGeneSelection());
//		}
//
//		return geneFinder;
//	}
//
//	/**
//	 * Getter for geneFinder
//	 * 
//	 * @return HeaderFinder arrayFinder
//	 */
//	public HeaderFinder getArrayFinder() {
//
//		if (arrayFinder == null) {
//
//			arrayFinder = new ArrayFinder(TreeViewFrame.this, getDataModel()
//					.getArrayHeaderInfo(), getArraySelection());
//		}
//		return arrayFinder;
//	}
	
	public DendroController getDendroController() {
		
		return dendroController;
	}
	
	/**
	 * Returns TreeViewFrame's configNode.
	 * @return
	 */
	public Preferences getConfigNode() {
		
		return configNode;
	}

	/**
	 * Returns TVFrame's instance of dataModel
	 */
	@Override
	public DataModel getDataModel() {

		return dataModel;
	}
	
	/**
	 * Returns TVFrame's instance of FileMRU
	 * @return
	 */
	public FileMru getFileMRU() {
		
		return fileMru;
	}
	
	/**
	 * Returns TVFrame's instance of ProgramMenu.
	 * @return
	 */
	public void rebuildProgramMenu() {
		
		programMenu.rebuild();
	}
	
	/**
	 * Returns the parent JPanel of TVFrame which holds the different views.
	 * @return
	 */
	public DendroPanel getRunning() {
		
		return running;
	}
	
	public JButton getTreeButton() {
		
		return treeButton;
	}
	
	public JButton getSearchButton() {
		
		return searchButton;
	}

	/** 
	 * 
	 * Setting up all saved configurations related to TVFrame.
	 */
	protected void setupPresets() {
		
		// Load Theme
		String default_theme = "dark";
		String savedTheme = getConfigNode().get("theme", 
				default_theme);
		
		// Since changing the theme resets the layout
		if(savedTheme.equalsIgnoreCase("dark")) {
			GUIParams.setNight();
			
		} else {
			GUIParams.setDayLight();
		}
		
		// Load FileMRU
		setupFileMru();
	}
}
