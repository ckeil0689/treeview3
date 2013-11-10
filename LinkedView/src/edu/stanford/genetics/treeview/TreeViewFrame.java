/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: TreeViewFrame.java,v $w
 * $Revision: 1.76 $
 * $Date: 2010-05-02 13:34:53 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular, 
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

import Cluster.ClusterFrame;
import Cluster.ClusterFrameWindow;
import Cluster.ClusterView;

import edu.stanford.genetics.treeview.core.ArrayFinder;
import edu.stanford.genetics.treeview.core.FileMruEditor;
import edu.stanford.genetics.treeview.core.GeneFinder;
import edu.stanford.genetics.treeview.core.GlobalPrefInfo;
import edu.stanford.genetics.treeview.core.HeaderFinder;
import edu.stanford.genetics.treeview.core.LogMessagesPanel;
import edu.stanford.genetics.treeview.core.LogSettingsPanel;
import edu.stanford.genetics.treeview.core.MemMonitor;
import edu.stanford.genetics.treeview.core.MenuHelpPluginsFrame;
import edu.stanford.genetics.treeview.core.PluginManager;
import edu.stanford.genetics.treeview.core.TreeViewJMenuBar;
import edu.stanford.genetics.treeview.model.CDTCreator;
import edu.stanford.genetics.treeview.model.DataModelWriter;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView2;

/**
 * This class is the main window of Java TreeView.
 * In practice, it serves as the base class for the LinkedViewFrame, and
 * the AppletViewFrame.
 * 
 * @author aloksaldanha
 *
 */
public class TreeViewFrame extends ViewFrame implements FileSetListener {
	
	//Instance Variables
	private static final long serialVersionUID = 1L;
	private static String appName = "TreeView 3.0";
	
	protected JPanel waiting;
	protected MainPanel running;
	protected DataModel dataModel;
	protected JDialog presetsFrame = null; 
	protected TabbedSettingsPanel presetsPanel = null;
	
	private final Color BLUE1 = new Color(60, 180, 220, 255);
	private final Color GRAY1 = new Color(150, 150, 150, 255);
	private final Color GRAY2 = new Color(100, 100, 100, 255);
	private final Color BG_COLOR = new Color(252, 252, 252, 255);

	private TreeViewApp treeView;
	private ProgramMenu programMenu;
	private HeaderFinder geneFinder = null;
	private HeaderFinder arrayFinder = null;
	private ClusterFrame clusterDialog = null;
	
	private boolean loaded;
	
	//Constructors
	/**
	 * Chained constructor
	 * @param TreeViewApp treeview
	 */
	public TreeViewFrame(TreeViewApp treeview) {
		
		this(treeview, appName);
	}
	
	/**
	 * Main Constructor
	 * @param TreeViewApp treeview
	 * @param String appName 
	 */
	public TreeViewFrame(TreeViewApp treeview, String appName) {
		
		super(appName);
		treeView = treeview;
		loaded = false;
		setWindowActive(true);
		
		try{
			UIManager.setLookAndFeel(
					UIManager.getCrossPlatformLookAndFeelClassName());
			
		} catch (Exception e){
			
		}
		
		setupLayout();
		setupPresets();
		setupMenuBar();
		setupFileMru(treeView.getGlobalConfig().getNode("FileMru"));

		centerOnscreen();
		setLoaded(false);
	}
	
	
	//Window and Frame methods
	@Override
	public void closeWindow(){
		
		if (running != null) {
			running.syncConfig();
		}
		
		super.closeWindow();
	}
	
	/** 
	 * Getter for ClusterDialog 
	 * @param dataModel
	 * @return ClusterFrame clusterDialog
	 */
	@Override
	public ClusterFrame getClusterDialogWindow(DataModel dataModel) {
			clusterDialog = new ClusterFrameWindow(
					TreeViewFrame.this, dataModel); 
			
		return clusterDialog;
	}	
	
	//Loading Methods
	/**
	 * Allows user to load a file from a URL
	 * @return FileSet
	 * @throws LoadException
	 */
	protected FileSet offerUrlSelection() throws LoadException {
		
		FileSet fileSet1;
		/*
		 * JTextField textField = new JTextField(); JPanel prompt = new
		 * JPanel(); prompt.setLayout(new BorderLayout()); prompt.add(new
		 * JLabel("Enter a Url"), BorderLayout.NORTH); prompt.add(textField,
		 * BorderLayout.CENTER);
		 */
		// get string from user...
		String urlString = JOptionPane.showInputDialog(this, "Enter a Url");
		
		if (urlString != null) {
			// must parse out name, parent + sep...
			int postfix = urlString.lastIndexOf("/") + 1;
			String name = urlString.substring(postfix);
			String parent = urlString.substring(0, postfix);
			fileSet1 = new FileSet(name, parent);
			
		} else {
			throw new LoadException("Input Dialog closed without selection...",
					LoadException.NOFILE);
		}
		
		return fileSet1;
	}
	
	/**
	 * Loads a FileSet and calls setLoaded(true) to reset the MainPanel.
	 * @param fileSet
	 * @throws LoadException
	 */
	public void load(FileSet fileSet) throws LoadException {
		
		loadFileSet(fileSet);
		
		fileSet = fileMru.addUnique(fileSet);
		fileMru.setLast(fileSet);
		fileMru.notifyObservers();
		
		setLoaded(true);
	}

	/**
	 * To load any fileset without using the event queue thread
	 */
	public void loadNW(FileSet fileSet) throws LoadException {
		
		loadFileSetNW(fileSet);
		
		fileSet = fileMru.addUnique(fileSet);
		fileMru.setLast(fileSet);
		fileMru.notifyObservers();
		
		setLoaded(true);
	}
	
	/**
	 * r * This is the workhorse. It creates a new DataModel of the file, and
	 * then sets the Datamodel. A side effect of setting the datamodel is to
	 * update the running window.
	 */
	public void loadFileSet(FileSet fileSet) throws LoadException {
		
		TVModel tvModel = new TVModel();
		tvModel.setFrame(this);
		
		try {
			tvModel.loadNew(fileSet);
			setDataModel(tvModel, false);
			
		} catch (LoadException e) {
			if (e.getType() != LoadException.INTPARSE) {
				JOptionPane.showMessageDialog(this, e);
				throw e;
			}
		}
	}
	
	/**
	 * To load any FileSet without using the event queue thread
	 */
	public void loadFileSetNW(FileSet fileSet) throws LoadException {
		
		TVModel tvModel = new TVModel();
		tvModel.setFrame(this);
		
		try {
			tvModel.loadNewNW(fileSet);
			setDataModel(tvModel, false);
		} catch (LoadException e) {
			if (e.getType() != LoadException.INTPARSE) {
				JOptionPane.showMessageDialog(this, e);
				throw e;
			}
		}
	}
	
	/**
	 * This method opens a file dialog to open either 
	 * the visualization view or the cluster view
	 * depending on which file type is chosen.
	 * @throws LoadException
	 */
	public void openFile(){
		
		try {
			File file = selectFile();
			
			int suffixLength = 3;
			String fileName = file.getAbsolutePath();
			String fileType = file.getAbsolutePath().substring(
					fileName.length() - suffixLength, fileName.length());
			
			if(!fileType.equalsIgnoreCase("cdt")) {
				CDTCreator fileChanger = new CDTCreator(file);
				fileChanger.readFile();
			}
			
			FileSet fileSet = getFileSet(file); //Type: 0 (Auto)
			loadFileSet(fileSet);
			
			fileSet = fileMru.addUnique(fileSet);
			fileMru.setLast(fileSet);
			
			confirmLoaded();

		} catch (LoadException e) {
			if ((e.getType() != LoadException.INTPARSE)
					&& (e.getType() != LoadException.NOFILE)) {
				LogBuffer.println("Could not open file: "
						+ e.getMessage());
			}
		}
	}
	
	
	//Methods to setup views
	/**
	 * Sets up the following: 1) urlExtractor, an object that generates urls
	 * from gene indexes 2) arrayUrlExtractor, similarly 3) geneSelection and 4)
	 * arraySelection, the two selection objects. It is important that these are
	 * set up before any plugins are instantiated. This is called before
	 * setupRunning by setDataModel.
	 */
	protected void setupExtractors() {
		
		DataMatrix matrix = getDataModel().getDataMatrix();
		int ngene = matrix.getNumRow();
		int nexpr = matrix.getNumCol();
		
		ConfigNode documentConfig = getDataModel().getDocumentConfigRoot();
		// extractors...
		UrlPresets genePresets = getGeneUrlPresets();
		UrlExtractor urlExtractor = new UrlExtractor(getDataModel()
				.getGeneHeaderInfo(), genePresets);
		urlExtractor.bindConfig(documentConfig.fetchOrCreate("UrlExtractor"));
		setUrlExtractor(urlExtractor);
		
		UrlPresets arrayPresets = getArrayUrlPresets();
		UrlExtractor arrayUrlExtractor = new UrlExtractor(getDataModel()
				.getArrayHeaderInfo(), arrayPresets);
		arrayUrlExtractor.bindConfig(documentConfig
				.fetchOrCreate("ArrayUrlExtractor"));
		setArrayUrlExtractor(arrayUrlExtractor);
		
		geneSelection = new TreeSelection(ngene);
		arraySelection = new TreeSelection(nexpr);
	}
	
	/**
	 * Generates a DendroView object and sets the current running MainPanel
	 * to it. As a result the View is displayed in the TreeViewFrame
	 */
	protected void setupRunning() {
		
		DendroView2 dv2 = new DendroView2(getDataModel(), this);						
		running = dv2;
	}
	
	/**
	 * Generates a ClusterView object and sets the current running MainPanel
	 * to it. As a result the View is displayed in the TreeViewFrame
	 */
	protected void setupClusterRunning() {
		
		ClusterView cv = new ClusterView(getDataModel(), this);
		running = cv;
	}

	//Observer
	@Override
	public void update(Observable observable, Object object) {
		if (observable == fileMru) {
			// System.out.println("Rebuilding file menu");
			programMenu.rebuild();
			
		} else {
			System.out.println("Got weird update");
		}
	}

	/**
	 * This should be called whenever the loaded status changes It's
	 * responsibility is to change the look of the main view only
	 * @param boolean flag
	 */
	@Override
	public void setLoaded(boolean flag) {
		//reset persistent popups
		setGeneFinder(null);
		loaded = flag;
		getContentPane().removeAll();
		
		if (loaded) {
			if (running == null) {			
				JOptionPane.showMessageDialog(this, "TreeViewFrame 253: " +
						"No plugins to display");
			} else {
				getContentPane().add((JComponent) running);
				setLoadedTitle();
				treeView.getGlobalConfig().store();
			}
			
		} else {
			getContentPane().add(waiting);
			setTitle(getAppName());
		}

		// menubar.rebuild...
		rebuildMainPanelMenu();
		treeView.rebuildWindowMenus();
		
		validate();
		repaint();
	}
	
	
	//Methods to setup MenuBar
	/**
	 * This method sets up the main MenuBar with all the subMenus.
	 * It also calls methods to populate the subMenus.
	 */
	protected void setupMenuBar() {
		
		menubar = new TreeViewJMenuBar();
		setJMenuBar(new JMenuBar());
		
		((TreeViewJMenuBar) menubar).setUnderlyingMenuBar(getJMenuBar());

		synchronized(menubar) {
			menubar.addMenu(TreeviewMenuBarI.programMenu);
			menubar.setMenuMnemonic(KeyEvent.VK_F);
			menubar.setMenuMnemonic(KeyEvent.VK_C);
			programMenu = new ProgramMenu(); // rebuilt when fileMru notifies
			menubar.addMenu(TreeviewMenuBarI.documentMenu);
			menubar.setMenuMnemonic(KeyEvent.VK_S);
			populateSettingsMenu(menubar);
			menubar.addMenu(TreeviewMenuBarI.analysisMenu);
			menubar.setMenuMnemonic(KeyEvent.VK_A);
			menubar.addMenu(TreeviewMenuBarI.exportMenu);
			menubar.setMenuMnemonic(KeyEvent.VK_E);
			menubar.addMenu(TreeviewMenuBarI.windowMenu);			
			menubar.setMenuMnemonic(KeyEvent.VK_W);
			menubar.addMenu(TreeviewMenuBarI.helpMenu);
			menubar.setMenuMnemonic(KeyEvent.VK_H);
			populateHelpMenu(menubar);
		}		
	}	
	
	/**
	 * When this method is called some components of the main MenuBar
	 * are rearranged and restructured. Depending whether the boolean loaded is
	 * true some subMenus are populated or not.
	 */
	public void rebuildMainPanelMenu() {
		
		synchronized(menubar) {
//			menubar.setMenu(TreeviewMenuBarI.documentMenu);
//			menubar.removeMenuItems();
			menubar.setMenu(TreeviewMenuBarI.analysisMenu);
			menubar.removeAll();
			menubar.setEnabled(true);
			menubar.setMenu(TreeviewMenuBarI.exportMenu);
			menubar.removeAll();
		}
		
		if (getLoaded()) {
			menubar.setMenu(TreeviewMenuBarI.analysisMenu);
			populateAnalysisMenu(menubar);
			menubar.setMenu(TreeviewMenuBarI.exportMenu);
			populateExportMenu(menubar);
			
			if (running != null) {
				menubar.setMenu(TreeviewMenuBarI.documentMenu);
				running.populateSettingsMenu(menubar);
				menubar.setMenu(TreeviewMenuBarI.analysisMenu);
				running.populateAnalysisMenu(menubar);
				menubar.setMenu(TreeviewMenuBarI.exportMenu);
				
				if (menubar.getItemCount() > 0) {
					menubar.addSeparator();
				}
				running.populateExportMenu(menubar);
			}
			
			menubar.setMenu(TreeviewMenuBarI.analysisMenu);
			
			if (menubar.getItemCount() > 0) {
				menubar.addSeparator();
			}
		}

		menubar.setMenu(TreeviewMenuBarI.documentMenu);
		menubar.setEnabled(true);
	}
	
	/**
	 * This method sets up a JDialog to call an option for the user
	 * to change some of the presets.
	 */
	protected void setupPresetsPanel() {
		
		presetsFrame = new JDialog(TreeViewFrame.this, "Presets", true);
		presetsPanel = new TabbedSettingsPanel();

		UrlPresetsEditor presetEditor = new UrlPresetsEditor(
				getGeneUrlPresets());
		presetEditor.setTitle("Gene Url");
		presetsPanel.addSettingsPanel("Gene", presetEditor);

		presetEditor = new UrlPresetsEditor(getArrayUrlPresets());
		presetEditor.setTitle("Array Url");
		presetsPanel.addSettingsPanel("Array", presetEditor);

		SettingsPanelHolder innerPanel = new SettingsPanelHolder(presetsFrame,
				getApp().getGlobalConfig().getRoot());
		innerPanel.addSettingsPanel(presetsPanel);
		
		presetsFrame.getContentPane().add(innerPanel);
		presetsFrame.pack();
	}
	
	//Populating various menus
	/**
	 * This methods populates the Settings menu with several MenuItems
	 * and subMenus.
	 * @param menubar
	 */
	protected void populateSettingsMenu(TreeviewMenuBarI menubar) {
		menubar.addSeparator();
//		menubar.addSubMenu(TreeviewMenuBarI.presetsSubMenu);
		menubar.addMenuItem("Gene Url",new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (presetsPanel == null) {
					setupPresetsPanel();
				}
				
				presetsPanel.synchronizeFrom();
				presetsPanel.setSelectedIndex(0);
				presetsFrame.setVisible(true);
			}
		});
		menubar.setAccelerator(KeyEvent.VK_P);
		menubar.setMnemonic(KeyEvent.VK_G);
		
		menubar.addMenuItem("Array Url", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (presetsPanel == null) {
					setupPresetsPanel();
				}
				presetsPanel.synchronizeFrom();
				presetsPanel.setSelectedIndex(1);
				presetsFrame.setVisible(true);
			}
		});
		menubar.setMnemonic(KeyEvent.VK_A);

		PluginFactory[] plugins = 
				PluginManager.getPluginManager().getPluginFactories();
		
		if (plugins.length == 0) {
			menubar.addMenuItem("Color", new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					if (presetsPanel == null)
						setupPresetsPanel();
					presetsPanel.synchronizeFrom();
					presetsPanel.setSelectedIndex(2);
					presetsFrame.setVisible(true);
				}
			});
			menubar.setMnemonic(KeyEvent.VK_C);
		} else {
			for (int i = 0; i < plugins.length; i++) {
				plugins[i].addPluginConfig(menubar, this);
			}
		}
		
//		menubar.addSeparator();
//		
//		menubar.addMenuItem("Change Menubar", new ActionListener() {
//			public void actionPerformed(ActionEvent actionEvent) {
//				getApp().getPrefs().showEditor();
//				getApp().getGlobalConfig().store();
//			}
//		});
	}
	
	/**
	 * This method populates the Export Menu with MenuItems.
	 * @param menubar2
	 */
	protected void populateExportMenu(TreeviewMenuBarI menubar) {
		
		/*
		 * MenuItem menuItem2 = new MenuItem("Export to Text File... ");
		 * menuItem2.addActionListener(new ActionListener() { public void
		 * actionPerformed(ActionEvent actiosnEvent) { ViewFrame viewFrame =
		 * TreeViewFrame.this; FileSet source = getDataModel().getFileSet();
		 * GeneListMaker t = new GeneListMaker(viewFrame, getGeneSelection(),
		 * getDataModel().getGeneHeaderInfo(), source.getDir()+source.getRoot() +
		 * ".txt"); t.setDataMatrix(getDataModel().getDataMatrix(),
		 * getDataModel().getArrayHeaderInfo(), DataModel.NODATA);
		 * t.bindConfig(getDataModel().getDocumentConfig()
		 *.getNode("GeneListMaker"));
		 * t.makeList(); } }); exportMenu.add(menuItem2);
		 */

		menubar.addMenuItem("Save List", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actiosnEvent) {
				
				if (warnSelectionEmpty()) {
					
					ViewFrame viewFrame = TreeViewFrame.this;
					FileSet source = getDataModel().getFileSet();
					String def = getDataModel().getName() + "_list.txt";
					
					if (source != null) {
						
						def = source.getDir() + source.getRoot() + "_list.txt";
					}
					
					GeneListMaker t = new GeneListMaker(viewFrame,
							getGeneSelection(), getDataModel()
									.getGeneHeaderInfo(), def);
					
					t.setDataMatrix(getDataModel().getDataMatrix(),
							getDataModel().getArrayHeaderInfo(),
							DataModel.NODATA);
					
					t.bindConfig(getDataModel().getDocumentConfigRoot()
							.fetchOrCreate("GeneListMaker"));
					
					t.pack();
					t.setVisible(true);
				}
			}
		});
		menubar.setMnemonic(KeyEvent.VK_L);

		menubar.addMenuItem("Save Data", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actiosnEvent) {
				
				if (warnSelectionEmpty()) {
					
					ViewFrame viewFrame = TreeViewFrame.this;
					FileSet source = getDataModel().getFileSet();
					
					GeneListMaker t = new GeneListMaker(viewFrame,
							getGeneSelection(), getDataModel()
									.getGeneHeaderInfo(), source.getDir()
									+ source.getRoot() + "_data.cdt");
					
					t.setDataMatrix(getDataModel().getDataMatrix(),
							getDataModel().getArrayHeaderInfo(),
							DataModel.NODATA);
					
					t.bindConfig(getDataModel().getDocumentConfigRoot()
							.fetchOrCreate("GeneListMaker"));
					
					t.includeAll();
					t.pack();
					t.setVisible(true);
				}
			}
		});
		menubar.setMnemonic(KeyEvent.VK_D);
	}
	
	/**
	 * Populates the Analysis Menu with MenuItems and subMenus
	 * @param menubar2
	 */
	protected void populateAnalysisMenu(TreeviewMenuBarI menubar2) {
		
		menubar.addMenuItem("Find Genes...", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				
				getGeneFinder().setVisible(true);
			}
		});
		menubar.setAccelerator(KeyEvent.VK_G);
		
		menubar.addMenuItem("Find Arrays...",new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				
				getArrayFinder().setVisible(true);
			}
		});
		menubar.setAccelerator(KeyEvent.VK_A);
		
		menubar.addMenuItem("Stats...", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				JOptionPane.showMessageDialog(TreeViewFrame.this,
						new JTextArea(getDataModel().toString()));
			}
		});
		menubar.setAccelerator(KeyEvent.VK_S);
	}
	
	/**
	 * Populates the HelpMenu with MenuItems and subMenus.
	 * @param menubar
	 */
	private void populateHelpMenu(TreeviewMenuBarI menubar) {
		menubar.addMenuItem("About...",  new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				/*
				 * Popup popup = new Popup(TreeViewFrame.this,getAppName(), new
				 * String [] { "Java TreeView was created by Alok
				 * (alok@genome).", "It is an extensible, crossplatform port of
				 * Eisen's TreeView.", "Version: " +
				 * TreeViewApp.getVersionTag(), "Homepage:
				 * http://genetics.stanford.edu/~alok/TreeView/" });
				 */
				JPanel message = new JPanel();
				// message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));
				message.setLayout(new GridLayout(0, 1));
				message
						.add(new JLabel(
								getAppName()
										+ " was created by Alok " +
										"(alokito@users.sourceforge.net)."));
				message.add(new JLabel("Version: "
						+ TreeViewApp.getVersionTag()));

				JPanel home = new JPanel();
				home.add(new JLabel("Homepage"));
				home.add(new JTextField(TreeViewApp.getUpdateUrl()));
				
				JButton yesB = new JButton("Open");
				yesB.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent arg0) {
						displayURL(TreeViewApp.getUpdateUrl());
					}

				});
				home.add(yesB);
				message.add(home);

				home = new JPanel();
				home.add(new JLabel("Announcements"));
				home.add(new JTextField(TreeViewApp.getAnnouncementUrl()));
				
				yesB = new JButton("Sign Up");
				yesB.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						displayURL(TreeViewApp.getAnnouncementUrl());
					}

				});
				home.add(yesB);
				message.add(home);

				JOptionPane.showMessageDialog(TreeViewFrame.this, message, 
						"About...", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		menubar.setMnemonic(KeyEvent.VK_A);

		
		menubar.addMenuItem("Messages...", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				JPanel inner = new JPanel();
				inner.setLayout(new BorderLayout());
				inner.add(new JLabel("JTV Messages"), BorderLayout.NORTH);
				inner.add(new JScrollPane(new LogMessagesPanel(LogBuffer
						.getSingleton())), BorderLayout.CENTER);
				
				LogBuffer buffer = LogBuffer.getSingleton();
				buffer.setLog(true);
				inner.add(new LogSettingsPanel(buffer),
						BorderLayout.SOUTH);
				
				final JDialog top = new JDialog(TreeViewFrame.this,
						"JTV Messages", false);
				top.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				top.setContentPane(inner);
				top.pack();
				top.setLocationRelativeTo(TreeViewFrame.this);
				top.setVisible(true);
			}
		});
		menubar.setMnemonic(KeyEvent.VK_M);

		menubar.addMenuItem("Documentation...", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				// Popup popup = new Popup(TreeViewFrame.this, "Java TreeView:
				// Color", new String [] { "I'm going to add better help
				// later.", "For now, point a web browser at index.html in the
				// doc subdirectory of the Java TreeView folder.", "(that is, if
				// it doesn't open automatically...)" });
				// String classPath = System.getProperty("java.class.path");
				JPanel message = new JPanel();
				message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));
				message.add(new JLabel(getAppName()
						+ " documentation is available from the website."));
				
				final String docUrl = TreeViewApp.getUpdateUrl()
						+ "/manual.html";
				message.add(new JTextField(docUrl));
				
				JButton lButton = new JButton("Launch Browser");
				lButton.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						
						displayURL(docUrl);
					}
				});
				message.add(lButton);
				JOptionPane.showMessageDialog(TreeViewFrame.this, message);
			}
		});
		menubar.setMnemonic(KeyEvent.VK_D);

		menubar.addMenuItem("Plugins...",new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				
				displayPluginInfo();
			}
		});		
		menubar.setMnemonic(KeyEvent.VK_P);

		menubar.addMenuItem("Registration...",new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				
				ConfigNode node = treeView.getGlobalConfig().getNode(
						"Registration");
				if (node != null) {
					try {
						edu.stanford.genetics.treeview.reg.RegEngine
								.reverify(node);
					} catch (Exception e) {
						LogBuffer.println("registration error " + e);
						e.printStackTrace();
					}
				}
			}
		});
		menubar.setMnemonic(KeyEvent.VK_R);

		menubar.addMenuItem("Feedback...",new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				JPanel feedback = new JPanel();
				feedback.setLayout(new BoxLayout(feedback, BoxLayout.Y_AXIS));
				
				JComponent tmp = new JLabel("Please report bugs at ");
				tmp.setAlignmentX((float) 0.0);
				feedback.add(tmp);
				
				final String bugsURL = "http://sourceforge.net/" +
						"tracker/?group_id=84593&atid=573298";
				tmp = new JTextField(bugsURL);
				// tmp.setAlignmentX((float) 1.0);
				feedback.add(tmp);
				
				JButton yesB = new JButton("Report Bug");
				yesB.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent arg0) {
						
						displayURL(bugsURL);
					}
				});
				feedback.add(yesB);

				tmp = new JLabel("Please request features at ");
				tmp.setAlignmentX((float) 0.0);
				feedback.add(tmp);
				
				final String featureURL = "https://sourceforge.net/" +
						"tracker/?group_id=84593&atid=573301";
				tmp = new JTextField(featureURL);
				// tmp.setAlignmentX((float) 1.0);
				feedback.add(tmp);
				
				yesB = new JButton("Request Feature");
				yesB.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent arg0) {
						
						displayURL(featureURL);
					}
				});
				feedback.add(yesB);

				tmp = new JLabel("For support, send email to ");
				tmp.setAlignmentX((float) 0.0);
				feedback.add(tmp);
				final String supportURL = 
						"jtreeview-users@lists.sourceforge.net";
				tmp = new JTextField(supportURL);
				// tmp.setAlignmentX((float) 1.0);
				feedback.add(tmp);
				yesB = new JButton("Email Support");
				yesB.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						displayURL("mailto:"+supportURL);
					}
				});
				feedback.add(yesB);

				tmp = new JLabel("You may also search the list archives at ");
				tmp.setAlignmentX((float) 0.0);
				feedback.add(tmp);
				
				final String archiveURL = "http://sourceforge.net/" +
						"mailarchive/forum.php?forum_id=36027";
				tmp = new JTextField(archiveURL);
				// tmp.setAlignmentX((float) 1.0);
				feedback.add(tmp);
				
				yesB = new JButton("Browse Archive");
				yesB.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent arg0) {
						
						displayURL(archiveURL);
					}
				});
				feedback.add(yesB);
				
				JOptionPane.showMessageDialog(TreeViewFrame.this, feedback, 
						"Feedback...", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		menubar.setMnemonic(KeyEvent.VK_F);
		menubar.addSeparator();

		
		menubar.addMenuItem("Memory...", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				MemMonitor m = new MemMonitor();
				m.start();
			}
		});
		menubar.setMnemonic(KeyEvent.VK_M);

		menubar.addMenuItem("Threads...",new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				ThreadListener t = new ThreadListener();
				t.start();
			}
		});
		menubar.setMnemonic(KeyEvent.VK_T);
		
		menubar.addMenuItem("Global Pref Info...", new ActionListener () {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				GlobalPrefInfo gpi = new GlobalPrefInfo(getApp());
				JOptionPane.showMessageDialog(null, gpi, "Global Pref Info...", 
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		
		/* This is to help debug plugin instance naming.
		MenuItem pluginSearch = new MenuItem("Search for instances");
		pluginSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JTextField name = new JTextField(20);
				JOptionPane.showMessageDialog(TreeViewFrame.this, name);
				MainPanel [] list = getPluginsByName(name.getText());
				JPanel res = new JPanel();
				for (int i = 0; i < list.length; i++)
					res.add(new JLabel(list[i].getName()));
				JOptionPane.showMessageDialog(TreeViewFrame.this,res);
			}
		});
		menu.add(pluginSearch);
		*/
	}
	
	//Various Methods
	/**
	 * Generates a warning message if TreeSelectionI object is null and 
	 * returns false in that case.
	 * @return boolean
	 */
	public boolean warnSelectionEmpty() {
		
		TreeSelectionI treeSelection = getGeneSelection();
		
		if ((treeSelection == null)
				|| (treeSelection.getNSelectedIndexes() <= 0)) {

			JOptionPane.showMessageDialog(this,
					"Cannot generate gene list, no gene selected");
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param incremental
	 * @return
	 */
	private boolean doModelSave(boolean incremental) {
		
		DataModelWriter writer = new DataModelWriter(getDataModel());
		final Set<DataModelFileType> written;
		
		if (incremental) {
			written =  writer.writeIncremental(getDataModel().getFileSet());
			
		} else {
			written =  writer.writeAll(getDataModel().getFileSet());
		}
		
		if (written.isEmpty()) {
			JOptionPane.showMessageDialog(TreeViewFrame.this, 
					"No Model changes were written\nOnly the following " +
					"changes require explicit saving:\n\n"+
					" - Tree Node flips (Analysis->Flip " +
					"Array/Gene Tree Node)\n" +
					" - Tree Node Annotations " +
					"(Analysis->Array/Gene TreeAnno)\n");
			return false;
			
		} else {
			String msg = "Model changes were written to ";
			int i = 0;
			
			for (DataModelFileType type : written) {
				msg += type.name();
				i++;
				
				if (i == written.size()) {
					// nothing after last one.
					
				} else if (i+1 == written.size()) {
					msg += " and ";
					
				} else {
					msg += ",";
				}
			}
			
			JOptionPane.showMessageDialog(TreeViewFrame.this, msg);
			return true;
		}
	}
	
	/**
	 * Subclass which exists to setup the "File" menu in the MenuBar.
	 * @author CKeil
	 */
	private class ProgramMenu {
		
		/**
		 * Constructor
		 */
		ProgramMenu() {
			
			synchronized(menubar) {
				
				menubar.setMenu(TreeviewMenuBarI.programMenu);
				menubar.addMenuItem("Open...", new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent actionEvent) {
						
						openFile();
						resetLayout();
					}
				});
				menubar.setAccelerator(KeyEvent.VK_O);
				menubar.setMnemonic(KeyEvent.VK_O);

//			menubar.addMenuItem("Open Url...", new ActionListener() {
//				public void actionPerformed(ActionEvent actionEvent) {
//					try {
//						FileSet fileSet = offerUrlSelection();
//						loadFileSet(fileSet);
//						fileSet = fileMru.addUnique(fileSet);
//						fileMru.setLast(fileSet);
//						fileMru.notifyObservers();
//						setLoaded(true);
//					} catch (LoadException e) {
//						LogBuffer.println("could not load url: "
//								+ e.getMessage());
//						// setLoaded(false);
//					}
//				}
//			});
//			menubar.setMnemonic(KeyEvent.VK_U);
			
				menubar.addMenuItem("Save",new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent actionEvent) {
						
						doModelSave(true);
					}
				});
				menubar.setMnemonic(KeyEvent.VK_S);
				menubar.setAccelerator(KeyEvent.VK_S);
	
				menubar.addMenuItem("Save as..",new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent actionEvent) {
						
						if (getDataModel().getFileSet() == null) {
							JOptionPane.showMessageDialog(TreeViewFrame.this, 
									"Saving of datamodels not backed by " +
									"files is not yet supported.");
							
						} else {
							JFileChooser fileDialog = new JFileChooser();
							CdtFilter ff = new CdtFilter();
							fileDialog.setFileFilter(ff);
		
							String string = 
									getDataModel().getFileSet().getDir();
							
							if (string != null) {
								
								fileDialog.setCurrentDirectory(
										new File(string));
							}
							
							int retVal = fileDialog.showSaveDialog(
									TreeViewFrame.this);
							
							if (retVal == JFileChooser.APPROVE_OPTION) {
								File chosen = fileDialog.getSelectedFile();
								String name = chosen.getName();
								
								if (!name.toLowerCase().endsWith(".cdt") && 
										!name.toLowerCase().endsWith(".pcl")) {
									name += ".cdt";
								}
								
								FileSet fileSet2 = new FileSet(name, 
										chosen.getParent()+File.separator);
								fileSet2.copyState(getDataModel().getFileSet());
								
								FileSet fileSet1 = new FileSet(name, 
										chosen.getParent()+File.separator);
								fileSet1.setName(
										getDataModel().getFileSet().getName());
								
								getDataModel().getFileSet().copyState(fileSet1);
								doModelSave(false);
								
								getDataModel().getFileSet().notifyMoved();
								fileMru.removeDuplicates(
										getDataModel().getFileSet());
								fileSet2 = fileMru.addUnique(fileSet2);
								fileMru.setLast(getDataModel().getFileSet());
								rebuild();
								
								if (getDataModel() instanceof TVModel) {
									((TVModel) getDataModel())
									.getDocumentConfig().setFile(getDataModel()
											.getFileSet().getJtv());
								}
							}
						}
					}
				});
					
	
				menubar.addSubMenu(TreeviewMenuBarI.mruSubMenu);
				menubar.setMenuMnemonic(KeyEvent.VK_R);
				menubar.setMenu(TreeviewMenuBarI.programMenu);
				
				menubar.addMenuItem("Edit Recent Files...", 
						new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent actionEvent) {
						
						FileMruEditor fme = new FileMruEditor(fileMru);
						fme.showDialog(TreeViewFrame.this);
					}
				});
				menubar.setMnemonic(KeyEvent.VK_E);
				
	//			menubar.addMenuItem("Edit Preferences...", 
//				new ActionListener() {
	//				public void actionPerformed(ActionEvent actionEvent) {
	//					getApp().getPrefs().showEditor();
	//					getApp().getGlobalConfig().store();
	//				}
	//			});
	//			menubar.setMnemonic(KeyEvent.VK_P);
	
				menubar.addSeparator();
				menubar.addMenuItem("Quit Program", new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent actionEvent) {
						try {
							treeView.closeAllWindows();
						} catch (Exception e) {
							System.out.println(
									"While trying to exit, got error " + e);
							System.exit(1);
						}
					}
				});
				menubar.setMnemonic(KeyEvent.VK_Q);
				menubar.setAccelerator(KeyEvent.VK_Q);
			}
		}
		
		/**
		 * This method removes all items from the MRUSubMenu and sets up new
		 * FileMenuListeners.
		 */
		public void rebuild() {
			/*
			removeAll();
			add(openItem);
			add(urlItem);
			add(saveItem);
*/
			synchronized(menubar) {
			menubar.setMenu(TreeviewMenuBarI.programMenu);
			menubar.setSubMenu(TreeviewMenuBarI.mruSubMenu);
			menubar.removeAll();
			
			ConfigNode aconfigNode[] = fileMru.getConfigs();
			String astring[] = fileMru.getFileNames();
			
			for (int j = aconfigNode.length; j > 0; j--) {
				FileMenuListener fileMenuListener = new FileMenuListener(
						new FileSet(aconfigNode[j - 1]));
				menubar.addMenuItem(astring[j - 1], fileMenuListener);
			}
			}
			/*
			add(mruItem);
			
			add(fmeItem);
			addSeparator();

			add(exitItem);
*/
		}
	}
	
	/**
	 * This class is an ActionListener which overrides the run() function
	 * @author CKeil
	 *
	 */
	class FileMenuListener implements ActionListener {
		private FileSet fileSet;
		
		/**
		 * Constructor
		 * @param set
		 */
		FileMenuListener(FileSet set) {
			fileSet = set;
		}

		@Override
		public void actionPerformed(ActionEvent argActionEvent) {
			
			final ActionEvent actionEvent = argActionEvent;
			Runnable update = new Runnable() {
				
				@Override
				public void run() {
					try {
						fileMru.setLast(fileSet);
						fileMru.notifyObservers();
						
						if (running != null) {
							running.syncConfig();
						}
						loadFileSet(fileSet);
						setLoaded(getDataModel().isLoaded());
						
					} catch (LoadException e) {
						if (e.getType() == LoadException.INTPARSE) {
							// System.out.println("Parsing cancelled...");
						} else {
							// System.out.println("Could not load: " + e);
							int result = FileMruEditor.offerSearch(fileSet,
									TreeViewFrame.this, "Could not Load "
											+ fileSet.getCdt());
							
							if (result == FileMruEditor.FIND) {
								fileMru.notifyFileSetModified();
								fileMru.notifyObservers();
								
								actionPerformed(actionEvent); // REPROCESS...
								return; // EARLY RETURN
								
							} else if (result == FileMruEditor.REMOVE) {
								fileMru.removeFileSet(fileSet);
								fileMru.notifyObservers();
							}
						}
						setLoaded(false);
					}
					// dataModel.notifyObservers();
				}
			};
			SwingUtilities.invokeLater(update);
		}
	}
	

	@Override
	public double noData() {
		
		return DataModel.NODATA;
	}
	
	//Layout setups
	/**
	 * This method sets up the Swing layout of the starting screen
	 * and calls resetLayout() once data has been loaded
	 */
	public void setupLayout() {
		
		JPanel title_bg;
		JLabel jl; 
		JLabel jl2;
		
		waiting = new JPanel();
		waiting.setLayout(new MigLayout("ins 0"));
		waiting.setBackground(BG_COLOR);
		
		title_bg = new JPanel();
		title_bg.setLayout(new MigLayout());
		title_bg.setBackground(BLUE1);
		
		jl = new JLabel("Hello! How are you Gentlepeople?");
		jl.setFont(new Font("Sans Serif", Font.PLAIN, 30));
		jl.setForeground(BG_COLOR);
		
		jl2 = new JLabel("Welcome to " + getAppName());
		jl2.setFont(new Font("Sans Serif", Font.BOLD, 50));
		jl2.setForeground(BG_COLOR);
		
		ClickableImage load_Icon = new ClickableImage(this, 
				"Load Data >");
		
		title_bg.add(jl, "push, alignx 50%, span, wrap");
		title_bg.add(jl2, "push, alignx 50%, span");
		
		waiting.add(title_bg, "pushx, growx, alignx 50%, span, " +
				"height 20%::, wrap");
		waiting.add(load_Icon, "push, alignx 50%, span");
	}
	
	/**
	 * This method clears the initial starting frame and adds new components
	 * to let the user select between options for processing/ viewing his data.
	 */
	public void resetLayout(){
		
		JPanel confirmPanel; 
		JPanel labelPanel;
		JLabel confirm;
		JLabel ques; 
		JLabel loadNew;
		
		waiting.removeAll();
		
		confirmPanel = new JPanel();
		confirmPanel.setLayout(new MigLayout());
		confirmPanel.setOpaque(false);
		
		labelPanel = new JPanel();
		labelPanel.setLayout(new MigLayout());
		labelPanel.setOpaque(false);
		
		confirm = new JLabel("Loaded File: " + dataModel.getFileName());
		confirm.setFont(new Font("Sans Serif", Font.BOLD, 25));
		confirm.setForeground(GRAY1);
		
		ques = new JLabel("What would you like to do?");
		ques.setFont(new Font("Sans Serif", Font.PLAIN, 50));
		ques.setForeground(GRAY2);
		
		ClickableImage clus_icon = new ClickableImage(this, "Cluster >", 
				(TVModel)dataModel);
		
		ClickableImage viz_icon = new ClickableImage(this, "Visualize >",  
				(TVModel)dataModel);
		
		loadNew = new JLabel("  Load New File");
		loadNew.setFont(new Font("Sans Serif", Font.BOLD, 25));
		loadNew.setForeground(BLUE1);
		
		loadNew.addMouseListener(new SSMouseListener(loadNew){
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				openFile();
			}
		});
		
		confirmPanel.add(confirm, "push, alignx 50%");
		confirmPanel.add(loadNew, "pushx");
		labelPanel.add(ques, "alignx 50%, push, grow, span");
		
		waiting.add(labelPanel, "push, alignx 50%, span, wrap");
		waiting.add(clus_icon, "push, grow, alignx 50%");
		waiting.add(viz_icon, "push, grow, alignx 50%, wrap");
		waiting.add(confirmPanel, "pushx, alignx 50%, span");
		
		waiting.repaint();
		waiting.revalidate();
	}
	
	/**
	 * This method clears the initial starting frame and adds new components
	 * to let the user select between options for processing/ viewing his data.
	 */
	public void confirmLoaded(){
		
		LoadCheckView confirmPanel; 
		
		waiting.removeAll();
		
		confirmPanel = new LoadCheckView(dataModel, this);
		
		waiting.add(confirmPanel, "push, grow");
		
		waiting.repaint();
		waiting.revalidate();
	}
	
	/**
	 * This method displays the current plugin info. 
	 * I set it up as a method so that it can be overridden
	 * by AppletViewFrame
	 */
	protected void displayPluginInfo() {
		MenuHelpPluginsFrame frame = new MenuHelpPluginsFrame(
				"Current Plugins", this);
		File f_currdir = new File(".");
		
		try {
			frame.setSourceText(f_currdir.getCanonicalPath() + 
					File.separator +"plugins" + File.separator);
		} catch (IOException e) {
			frame.setSourceText("Unable to read default plugins directory.");
		}
		
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
	}

	@Override
	public MainPanel[] getMainPanelsByName(String name) {
		
		if (running != null) {
			// is the current running a match?
			if (name.equals(running.getName())) {
				MainPanel [] list = new MainPanel[1];
				list[0] = running;
				return list;
			}
			
			// okay, is the current running a linkedPanel?
			try {
				LinkedPanel linked = (LinkedPanel) running;
				return linked.getMainPanelsByName(name);
				
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
			
		} else {
			// fall through to end
		}
		MainPanel [] list = new MainPanel[0];
		return list;
	}

	@Override
	public MainPanel[] getMainPanels() {
		if (running == null) {
			MainPanel [] list = new MainPanel[0];
			return list;			
		}
		
		// okay, is the current running a linkedPanel?
		try {
			LinkedPanel linked = (LinkedPanel) running;
			return linked.getMainPanels();
		} catch (ClassCastException e) {
			
		}

		MainPanel [] list = new MainPanel[1];
		list[0] = running;
		return list;
	}
	
	@Override
	public void scrollToGene(int i) {
		
		running.scrollToGene(i);
	}

	@Override
	public void scrollToArray(int i) {
		
		running.scrollToArray(i);
	}
	
	//Why do these methods exist? They appear to do nothing but
	//calling the setLoadedTitle() method...
	@Override
	public void onFileSetMoved(FileSet fileset) {
		
		setLoadedTitle();
	}
	
	//Setters
	/**
	 * Sets the title of the app to the source filepath of the 
	 * currently loaded dataModel
	 */
	private void setLoadedTitle() {
		
		setTitle(getAppName() + " : " + dataModel.getSource());
	}
	
	/**
	 * Setter for dataModel, also sets extractors, running.
	 * @param DataModel newModel
	 */
	@Override
	public void setDataModel(DataModel newModel, boolean cluster) {									
		
		if (dataModel != null) {
			dataModel.clearFileSetListeners();
		}
		
		dataModel = newModel;
		
		if (dataModel != null) {
			dataModel.addFileSetListener(this);
		}
		
		setupExtractors();
		
		if(cluster) {
			setupClusterRunning();
			
		} else {
			setupRunning();
		}
	}
	
	/** 
	 * Setter for geneFinder
	 * @param HeaderFinder geneFinder 
	 */
	public void setGeneFinder(HeaderFinder geneFinder) {
		
		this.geneFinder = geneFinder;
	}
	
	/** Setter for geneFinder */
	public void setArrayFinder(HeaderFinder geneFinder) {

		this.geneFinder = geneFinder;
	}
	
	//Getters
	/**
	 * Returns the name of the app
	 * @return String name
	 */
	public String getAppName() {
		
		return appName;
	}
	
	@Override
	public TreeViewApp getApp() {
		
		return treeView;
	}

	@Override
	public UrlPresets getGeneUrlPresets(){
		
		return treeView.getGeneUrlPresets();
	}
	

	@Override
	public UrlPresets getArrayUrlPresets(){
		
		return treeView.getArrayUrlPresets();
	}
	

	@Override
	public boolean getLoaded() {
		
		return loaded;
	}
	
	@Override
	public HeaderFinder getGeneFinder() {
		if (geneFinder == null) {
			geneFinder = new GeneFinder(TreeViewFrame.this, getDataModel()
					.getGeneHeaderInfo(), getGeneSelection());
		}
		
		return geneFinder;
	}
	
	/** 
	 * Getter for geneFinder 
	 * @return HeaderFinder arrayFinder
	 */
	public HeaderFinder getArrayFinder() {
		
		if (arrayFinder == null) {
			
			arrayFinder = new ArrayFinder(TreeViewFrame.this, getDataModel()
					.getArrayHeaderInfo(), getArraySelection());
		}
		return arrayFinder;
	}
	
	@Override
	public DataModel getDataModel() {
		
		return dataModel;
	}
	
	//Empty Methods
	protected void setupPresets() {}

}
