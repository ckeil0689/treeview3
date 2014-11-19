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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import Utilities.GUIFactory;
import Utilities.StringRes;
import Views.LoadErrorView;
import Views.WelcomeView;
import edu.stanford.genetics.treeview.core.FileMru;
import edu.stanford.genetics.treeview.core.FileMruEditor;
import edu.stanford.genetics.treeview.core.LogMessagesPanel;
import edu.stanford.genetics.treeview.core.LogSettingsPanel;
import edu.stanford.genetics.treeview.core.MenuHelpPluginsFrame;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;

/**
 * This class is the main window of TreeView 3. It holds all views to be
 * displayed and serves as parent JFrame to other windows.
 * 
 * @author ckeil
 * 
 */
public class TreeViewFrame extends ViewFrame implements FileSetListener,
		ConfigNodePersistent {
	
	public static final int WELCOME_VIEW = 0;
	public static final int LOADERROR_VIEW = 1;
	public static final int PROGRESS_VIEW = 2;
	public static final int DENDRO_VIEW = 3;

	protected final JPanel bgPanel;
	protected final JPanel waiting;
	protected DendroPanel running;
	protected JDialog presetsFrame = null;
	protected TabbedSettingsPanel presetsPanel = null;

	private final TreeViewApp treeView;
	private String title;

	// Different Views to be displayed
	private final WelcomeView welcomeView;
	private final LoadErrorView loadErrorView;
	private final DendroView dendroView;

	private String loadErrorMessage;
	
	private FileSet fileMenuSet;

	// Menu Items to be added to menu bar
	private List<JMenuItem> stackMenuList;
	private List<JMenuItem> fileMenuList;
	private List<FileSet> fileSetList;

	private JMenuBar menuBar;

	private boolean loaded;

	/**
	 * Chained constructor
	 * 
	 * @param TreeViewApp treeview
	 */
	public TreeViewFrame (final TreeViewApp treeview) {

		this(treeview, StringRes.appName);
	}

	/**
	 * Main Constructor
	 * 
	 * @param TreeViewApp The instance of the current running application.
	 * @param String The name of the application.
	 */
	public TreeViewFrame (final TreeViewApp treeview, final String appName) {

		super(appName);
		treeView = treeview;
		
		/* Get configurations */
		configNode = treeView.getGlobalConfig().node(StringRes.pnode_TVFrame);
		
		/* Initialize main views */
		welcomeView = new WelcomeView();
		loadErrorView = new LoadErrorView();
		dendroView = new DendroView(this);

		setWindowActive(true);

		/* Setting up main panels */
		bgPanel = GUIFactory.createJPanel(true, GUIFactory.NO_PADDING, null);
		waiting = GUIFactory.createJPanel(true, GUIFactory.NO_PADDING, null);

		/* Add main background panel to the application frame's contentPane */
		applicationFrame.getContentPane().add(bgPanel);

		/* Most recently used files */
		setupFileMru();

		setupFrameSize();
		
		/* Initial view */
		generateView(WELCOME_VIEW);
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			configNode = parentNode.node(StringRes.pnode_TVFrame);
		}
	}

	@Override
	public void saveSettings() {

		if (isLoaded()) {
//			dendroController.saveSettings();
		}
	}

	/**
	 * Generates the appropriate view panel. 
	 */
	@Override
	public void generateView(final int view_choice) {

		JPanel view;
			
		switch(view_choice) {
		
			case WELCOME_VIEW:
				view = welcomeView.makeWelcome();
				break;
				
			case PROGRESS_VIEW: 
				view = welcomeView.makeLoading();
				break;
				
			case LOADERROR_VIEW:
				loadErrorView.setErrorMessage(loadErrorMessage);
				view = loadErrorView.makeError();
				break;
				
			case DENDRO_VIEW:
				view = dendroView.makeDendro();
				break;
				
			default:
				view = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
				final JLabel error = GUIFactory.createLabel("No view "
						+ "could be loaded.", GUIFactory.FONTS);
				view.add(error, "push, alignx 50%");
				break;
		}

		buildMenuBar();
		displayView(view);
	}

	/**
	 * Displays a screen to notify the user of a loading issue. Offers the user
	 * to load a different file by providing the appropriate button.
	 * 
	 * @param errorMessage
	 */
	public void setLoadErrorMessage(final String errorMessage) {

		this.loadErrorMessage = errorMessage;
	}

	/**
	 * Setting the JPanel to be displayed within TVFrame
	 * 
	 * @param view
	 */
	public void displayView(final JPanel view) {

		bgPanel.removeAll();
		waiting.removeAll();
		
		waiting.add(view, "push, grow");
		
		bgPanel.add(waiting, "push, grow, h 97%");
		
		bgPanel.revalidate();
		bgPanel.repaint();
	}

	/**
	 * Displays an editor for recently used files.
	 */
	public void showRecentFileEditor() {

		new FileMruEditor(fileMru).showDialog(applicationFrame);
	}

	/**
	 * Shows a panel which displays the current stats of the loaded model.
	 */
	public void openStatsView(final String source, final int rowNum, 
			final int colNum) {

		final StatsPanel stats = new StatsPanel(TreeViewFrame.this);
		stats.setupLayout(source, rowNum, colNum);
		stats.setVisible(true);
	}

	/**
	 * Displays messages in a JDialog which were logged by TreeView.
	 */
	public void showLogMessages() {
				
		final JPanel inner = GUIFactory.createJPanel(false, 
				GUIFactory.NO_PADDING, null);
		inner.add(new JLabel("JTV Messages"), "span, wrap");
		inner.add(new JScrollPane(
				new LogMessagesPanel(LogBuffer.getSingleton())),
				"push, grow, wrap");

		final LogBuffer buffer = LogBuffer.getSingleton();
		buffer.setLog(true);
		inner.add(new LogSettingsPanel(buffer), "span, push");

		final JDialog top = new JDialog(applicationFrame, "JTV Messages", 
				false);
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
	
		new AboutDialog(TreeViewFrame.this).setVisible(true);
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

		tmp = new JLabel("For support, bugs reports, and requests "
				+ "send email to ");
		tmp.setAlignmentX((float) 0.0);
		feedback.add(tmp);
		final String supportURL = "ckeil@princeton.edu";
		tmp = new JTextField(supportURL);
		// tmp.setAlignmentX((float) 1.0);
		feedback.add(tmp);
		final JButton yesB = new JButton("Email Support");
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
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		final JPanel panel = GUIFactory.createJPanel(true, 
				GUIFactory.NO_PADDING, null);
		final JLabel l1 = GUIFactory.createLabel("Work in progress.", 
				GUIFactory.FONTS);

		panel.add(l1, "push, alignx 50%");
		dialog.add(panel);
		
		dialog.pack();
		dialog.setLocationRelativeTo(applicationFrame);

		dialog.setVisible(true);
	}

	// Getters for Views
	/**
	 * Returns TVFrame's current WelcomeView instance
	 * 
	 * @return welcomeView
	 */
	public WelcomeView getWelcomeView() {

		return welcomeView;
	}

	/**
	 * Returns TVFrame's current LoadErrorView instance
	 * 
	 * @return welcomeView
	 */
	public LoadErrorView getLoadErrorView() {

		return loadErrorView;
	}

	/**
	 * Returns TVFrame's current DendroView instance
	 * 
	 * @return dendroView
	 */
	public DendroView getDendroView() {

		return dendroView;
	}

	/**
	 * Sets the currently running panel for TVFrame. TVFrame is updated by
	 * setLoaded(true).
	 * 
	 * @param boolean isDendroLoaded
	 */
	public void setRunning(final boolean isDendroLoaded) {

		running = (isDendroLoaded) ? dendroView : null;
	}

	// Observer
	@Override
	public void update(final Observable o, final Object obj) {

		if (o instanceof FileMru) {// && menuBar != null) {
			LogBuffer.println("Updating fileMRU in TVFrame.");
			// System.out.println("Rebuilding file menu");
			buildMenuBar();
			
		} else if (o instanceof TVModel) {
			/* TVModel passes a boolean object to notify if it was loaded. */
			setRunning((Boolean)obj);
			setLoaded((Boolean)obj);
			
		} else {
			LogBuffer.println("Observable is: " + o.getClass());
			LogBuffer.println("Got weird update");
		}
	}

	/**
	 * This should be called whenever the loaded status changes It's
	 * responsibility is to change the look of the main view only
	 * 
	 * @param boolean flag
	 */
	@Override
	public void setLoaded(final boolean isModelLoaded) {
		
		// setGeneFinder(null);
		this.loaded = isModelLoaded;

		if (loaded) {
			if (running == null) {
				setTitleString(applicationFrame.getName());
				generateView(WELCOME_VIEW);
//				JOptionPane.showMessageDialog(applicationFrame,
//						"TreeViewFrame 253: No plugins to display");
			} else {
				setLoadedTitle();
				generateView(DENDRO_VIEW);
			}

		} else {
			applicationFrame.setTitle(StringRes.appName);
			setTitleString(applicationFrame.getName());
			generateView(LOADERROR_VIEW);
		}

		buildMenuBar();
	}

	/**
	 * Builds the JMenubar, fills it with JMenuItems and adds listeners
	 * to them. The listeners are called in MenubarController.
	 */
	public void buildMenuBar() {

		menuBar = new JMenuBar();
		applicationFrame.setJMenuBar(menuBar);
		
		stackMenuList = new ArrayList<JMenuItem>();

		// File
		final JMenu fileSubMenu = new JMenu(StringRes.mbar_File);
		
		// Open new file Menu
		final JMenuItem openMenuItem = new JMenuItem(StringRes.menu_Open,
				KeyEvent.VK_O);
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, 
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
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

		// -------
		fileMenuList = new ArrayList<JMenuItem>();
		fileSetList = new ArrayList<FileSet>();

		final JMenu recentSubMenu = new JMenu(StringRes.menu_OpenRecent);

		final Preferences[] aconfigNode = fileMru.getConfigs();
		final String astring[] = fileMru.getFileNames();

		for (int j = aconfigNode.length; j > 0; j--) {

			fileMenuSet = new FileSet(aconfigNode[j - 1]);
			fileSetList.add(fileMenuSet);

			final JMenuItem fileSetMenuItem = new JMenuItem(astring[j - 1]);
			recentSubMenu.add(fileSetMenuItem);
			fileMenuList.add(fileSetMenuItem);
		}

		fileSubMenu.add(recentSubMenu);

		final JMenuItem editRecentMenuItem = new JMenuItem(
				StringRes.menu_EditRecent);
		fileSubMenu.add(editRecentMenuItem);
		stackMenuList.add(editRecentMenuItem);
		// --------

		fileSubMenu.addSeparator();
		
		// Quit Program Menu
		final JMenuItem quitMenuItem = new JMenuItem(
				StringRes.menu_QuitWindow, KeyEvent.VK_W);
		quitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, 
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		fileSubMenu.add(quitMenuItem);
		stackMenuList.add(quitMenuItem);

		final JMenuItem saveMenuItem = new JMenuItem(StringRes.menu_Save, 
				KeyEvent.VK_S);
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		fileSubMenu.add(saveMenuItem);
		stackMenuList.add(saveMenuItem);

		final JMenuItem saveAsMenuItem = new JMenuItem(
				StringRes.menu_SaveAs);
		fileSubMenu.add(saveAsMenuItem);
		stackMenuList.add(saveAsMenuItem);

		menuBar.add(fileSubMenu);

		// Preferences
		final JMenu prefSubMenu = new JMenu(StringRes.menu_Prefs);
		
		// Delete all preferences nodes below 'File'
		final JMenuItem resetPrefs = 
				new JMenuItem(StringRes.menu_ResetPrefs);
		prefSubMenu.add(resetPrefs);
		stackMenuList.add(resetPrefs);
		
		// Preferences specific to DendroView
		if (running != null) {
			final JMenu viewMenu = new JMenu(StringRes.mbar_View);
			running.addDendroMenus(viewMenu);
			menuBar.add(viewMenu);

			final JMenu clusterMenu = new JMenu(StringRes.mbar_Cluster);
			running.addClusterMenus(clusterMenu);
			menuBar.add(clusterMenu);
			
			final JMenuItem fontMenuItem = new JMenuItem(
					StringRes.menu_Font);
			prefSubMenu.add(fontMenuItem);
			stackMenuList.add(fontMenuItem);
			
			final JMenuItem urlMenuItem = new JMenuItem(
					StringRes.menu_URL);
			prefSubMenu.add(urlMenuItem);
			stackMenuList.add(urlMenuItem);

			// Functional Enrichment Menu
			// TODO Create the feature.... :)
			// JMenuItem funcEnrMenuItem = new
			// JMenuItem("Functional Enrichment");
			// stackMenu.add(funcEnrMenuItem);
			// stackMenuList.add(funcEnrMenuItem);
		}

		fileSubMenu.addSeparator();
		fileSubMenu.add(prefSubMenu);

		// Help
		final JMenu helpSubMenu = new JMenu(StringRes.mbar_Help);

		if (running != null) {
			final JMenuItem statsMenuItem = new JMenuItem(
					StringRes.menu_Stats);
			helpSubMenu.add(statsMenuItem);
			stackMenuList.add(statsMenuItem);
		}

		final JMenuItem aboutMenuItem = new JMenuItem(
				StringRes.menu_About);
		helpSubMenu.add(aboutMenuItem);
		stackMenuList.add(aboutMenuItem);

		final JMenuItem logMenuItem = new JMenuItem(
				StringRes.menu_ShowLog);
		helpSubMenu.add(logMenuItem);
		stackMenuList.add(logMenuItem);

		final JMenuItem documentMenuItem = new JMenuItem(
				StringRes.menu_Docs);
		helpSubMenu.add(documentMenuItem);
		stackMenuList.add(documentMenuItem);

		// menubar.addMenuItem("Plugins...");
		// , new ActionListener() {
		//
		// @Override
		// public void actionPerformed(final ActionEvent actionEvent) {
		//
		// displayPluginInfo();
		// }
		// });

		final JMenuItem feedbackMenuItem = new JMenuItem(
				StringRes.menu_Feedback);
		helpSubMenu.add(feedbackMenuItem);
		stackMenuList.add(feedbackMenuItem);

		menuBar.add(helpSubMenu);

//		// New Window
//		final JMenuItem newWindowMenuItem = new JMenuItem(
//				StringRes.menu_title_NewWindow, KeyEvent.VK_N);
//		newWindowMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, 
//				ActionEvent.CTRL_MASK));
//		stackMenuList.add(newWindowMenuItem);
//		fileSubMenu.add(newWindowMenuItem);
	}

//	/**
//	 * This method populates the Export Menu with MenuItems.
//	 * 
//	 * @param menubar2
//	 */
//	protected void populateExportMenu(final TreeviewMenuBarI menubar) {
//
//		/*
//		 * MenuItem menuItem2 = new MenuItem("Export to Text File... ");
//		 * menuItem2.addActionListener(new ActionListener() { public void
//		 * actionPerformed(ActionEvent actiosnEvent) { ViewFrame viewFrame =
//		 * TreeViewFrame.this; FileSet source = getDataModel().getFileSet();
//		 * GeneListMaker t = new GeneListMaker(viewFrame, getGeneSelection(),
//		 * getDataModel().getGeneHeaderInfo(), source.getDir()+source.getRoot()
//		 * + ".txt"); t.setDataMatrix(getDataModel().getDataMatrix(),
//		 * getDataModel().getArrayHeaderInfo(), DataModel.NODATA);
//		 * t.bindConfig(getDataModel().getDocumentConfig()
//		 * .getNode("GeneListMaker")); t.makeList(); } });
//		 * exportMenu.add(menuItem2);
//		 */
//
//		// Save List Menu
//		final JMenuItem saveListMenuItem = (JMenuItem) menubar
//				.addMenuItem("Save List");
//		menubar.setMnemonic(KeyEvent.VK_L);
//		menuList.add(saveListMenuItem);
//
//		// Save Data Menu
//		final JMenuItem saveDataMenuItem = (JMenuItem) menubar
//				.addMenuItem("Save Data");
//		menubar.setMnemonic(KeyEvent.VK_D);
//		menuList.add(saveDataMenuItem);
//	}


	// Various Methods
	/**
	 * Displays the save dialog. Saving is handled by TVController.
	 * 
	 * @param incremental
	 * @return
	 */
	public void openSaveDialog(final boolean writerEmpty, final String msg) {

		final JDialog dialog = new JDialog(applicationFrame);
		dialog.setTitle("Information");
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setModalityType(JDialog.DEFAULT_MODALITY_TYPE);

		final JPanel panel = GUIFactory.createJPanel(true, 
				GUIFactory.NO_PADDING, null);

		final JButton button = GUIFactory.createBtn("OK");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {
				dialog.dispose();
			}
		});

		if (writerEmpty) {
			final JLabel l1 = GUIFactory.createLabel("No Model changes "
					+ "were written.", GUIFactory.FONTS);

			final JLabel l2 = GUIFactory.createLabel("Only the following "
					+ "changes require explicit saving: ", GUIFactory.FONTS);

			final JLabel l3 = GUIFactory.createLabel("- Tree Node flips "
					+ "(Analysis->Flip Array/Gene Tree Node)", 
					GUIFactory.FONTS);

			final JLabel l4 = GUIFactory.createLabel("- Tree Node Annotations "
					+ "(Analysis->Array/Gene TreeAnno)", GUIFactory.FONTS);

			panel.add(l1, "pushx, wrap");
			panel.add(l2, "pushx, wrap");
			panel.add(l3, "pushx, wrap");
			panel.add(l4, "pushx, wrap");
			panel.add(button, "pushx, alignx 50%");

			dialog.add(panel);

		} else {
			final JLabel l1 = GUIFactory.createLabel(msg, GUIFactory.FONTS);

			panel.add(l1, "pushx, wrap");
			panel.add(button, "pushx, alignx 50%");

			dialog.add(panel);
		}
		
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
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
			LogBuffer.println("IOException while trying to display "
					+ "Plugin info: " + e.getMessage());
		}

		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
	}

	// @Override
	// public MainPanel[] getMainPanelsByName(final String name) {
	//
	// if (running != null) {
	// // is the current running a match?
	// if (name.equals(running.getName())) {
	// final MainPanel[] list = new MainPanel[1];
	// list[0] = running;
	// return list;
	// }
	//
	// // okay, is the current running a linkedPanel?
	// try {
	// final LinkedPanel linked = (LinkedPanel) running;
	// return linked.getMainPanelsByName(name);
	//
	// } catch (final ClassCastException e) {
	// e.printStackTrace();
	// }
	//
	// } else {
	// // fall through to end
	// }
	// final MainPanel[] list = new MainPanel[0];
	// return list;
	// }

	// @Override
	// public MainPanel[] getMainPanels() {
	//
	// if (running == null) {
	// final MainPanel[] list = new MainPanel[0];
	// return list;
	// }
	//
	// // okay, is the current running a linkedPanel?
	// try {
	// final LinkedPanel linked = (LinkedPanel) running;
	// return linked.getMainPanels();
	// } catch (final ClassCastException e) {
	//
	// }
	//
	// final MainPanel[] list = new MainPanel[1];
	// list[0] = running;
	// return list;
	// }

//	@Override
//	public void scrollToGene(final int i) {
//
//		dendroController.scrollToGene(i);
//	}
//
//	@Override
//	public void scrollToArray(final int i) {
//
//		dendroController.scrollToArray(i);
//	}

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

		applicationFrame.setTitle(StringRes.appName + ": " + title);
	}

//	@Override
//	public void setDataModel(final DataModel model) {
//
//		this.dataModel = model;
//	}
	
	public void setTitleString(final String title) {

		this.title = title;
	}
	
	/**
	 * Returns the FileSet which has been chosen in the 'Open Recent' menu.
	 * FileSet is found by matching the index of the MenuItem to the index of
	 * the FileSet list which is created by checking the 'FileMRU' 
	 * Preferences node for previously stored fileSets.
	 * @param menuItem
	 * @return FileSet
	 */
	public FileSet findFileSet(JMenuItem menuItem) {
		
		int index = fileMenuList.indexOf(menuItem);
		
		if(fileSetList.size() == fileMenuList.size()) {
			return fileSetList.get(index);
			
		} else {
			LogBuffer.println("Sizes of FileSetList and FileMenuList in TVFrame"
					+ "don't match.");
			return null;
		}
	}

	/* Adding MenuActionListeners */
	public void addMenuActionListeners(final ActionListener l) {

		for (final JMenuItem item : stackMenuList) {

			if (item.getActionListeners().length == 0) {
				item.addActionListener(l);
			}
		}
	}

	/**
	 * used for the views to add some exclusive JMenuItems
	 * 
	 * @param menu
	 */
	public void addToStackMenuList(final JMenuItem menu) {

		stackMenuList.add(menu);
	}

	/**
	 * Adds FileMenuListeners to list of recent files that can be loaded.
	 * 
	 * @param listener
	 */
	public void addFileMenuListeners(final ActionListener listener) {

		for (final JMenuItem item : fileMenuList) {

			if (item.getActionListeners().length == 0) {
				item.addActionListener(listener);
			}
		}
	}

	public FileSet getFileMenuSet() {

		return fileMenuSet;
	}

	public List<JMenuItem> getStackMenus() {

		return stackMenuList;
	}

	// /**
	// * Setter for geneFinder
	// *
	// * @param HeaderFinder
	// * geneFinder
	// */
	// public void setGeneFinder(final HeaderFinder geneFinder) {
	//
	// this.geneFinder = geneFinder;
	// }
	//
	// /** Setter for geneFinder */
	// public void setArrayFinder(final HeaderFinder geneFinder) {
	//
	// this.geneFinder = geneFinder;
	// }

	public void setArraySelection(final TreeSelection aSelect) {

		this.arraySelection = aSelect;
	}

	public void setGeneSelection(final TreeSelection aSelect) {

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
	public boolean isLoaded() {

		return loaded;
	}

	// @Override
	// public HeaderFinder getGeneFinder() {
	//
	// if (geneFinder == null) {
	// geneFinder = new GeneFinder(TreeViewFrame.this, getDataModel()
	// .getGeneHeaderInfo(), getGeneSelection());
	// }
	//
	// return geneFinder;
	// }
	//
	// /**
	// * Getter for geneFinder
	// *
	// * @return HeaderFinder arrayFinder
	// */
	// public HeaderFinder getArrayFinder() {
	//
	// if (arrayFinder == null) {
	//
	// arrayFinder = new ArrayFinder(TreeViewFrame.this, getDataModel()
	// .getArrayHeaderInfo(), getArraySelection());
	// }
	// return arrayFinder;
	// }

//	public DendroController getDendroController() {
//
//		return dendroController;
//	}

	/**
	 * Returns TreeViewFrame's configNode.
	 * 
	 * @return
	 */
	public Preferences getConfigNode() {

		return configNode;
	}

//	/**
//	 * Returns TVFrame's instance of dataModel
//	 */
//	@Override
//	public DataModel getDataModel() {
//
//		return dataModel;
//	}

	/**
	 * Returns TVFrame's instance of FileMRU
	 * 
	 * @return
	 */
	public FileMru getFileMRU() {

		return fileMru;
	}

	/**
	 * Returns the parent JPanel of TVFrame which holds the different views.
	 * 
	 * @return
	 */
	public DendroPanel getRunning() {

		return running;
	}

//	public JToggleButton getTreeButton() {
//
//		return treeToggleBtn;
//	}
//
//	public JButton getSearchBtn() {
//
//		return searchBtn;
//	}
}
