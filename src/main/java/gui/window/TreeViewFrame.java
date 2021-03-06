/* BEGIN_HEADER                                                   TreeView 3
/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package gui.window;

import app.TreeViewApp;
import gui.DendroView;
import gui.FileMruEditor;
import gui.GUIFactory;
import gui.ViewType;
import gui.WelcomeView;
import gui.general.AboutDialog;
import gui.general.LogMessagesPanel;
import gui.general.LogSettingsPanel;
import gui.general.ShortcutDialog;
import gui.general.StatsDialog;
import gui.labels.url.UrlPresets;
import gui.matrix.DendroPanel;
import model.data.matrix.DataModel;
import model.data.matrix.TVModel;
import model.data.trees.TreeSelection;
import model.fileType.FileSet;
import model.fileType.FileSetListener;
import preferences.ConfigNodePersistent;
import preferences.FileMru;
import util.LogBuffer;
import util.StringRes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.prefs.Preferences;

/**
 * This class is the main window of TreeView 3. It holds all views to be
 * displayed and serves as parent JFrame to other windows.
 */
public class TreeViewFrame extends ViewFrame implements FileSetListener,
		ConfigNodePersistent {

	protected final JPanel viewPanel;
	protected final JPanel mainPanel;
	protected DendroPanel running;

	private final TreeViewApp treeView;
	private String title;

	// Different Views to be displayed
	private final WelcomeView welcomeView;
	private final DendroView dendroView;

	private FileSet mruFileSet;

	// Menu Items to be added to menu bar
	private List<JMenuItem> stackMenuList;
	private List<JMenuItem> mruFilesMenuList;
	private List<FileSet> mruFileSetList;

	private JMenuBar menuBar;

	private boolean loaded;


	public TreeViewFrame(final TreeViewApp treeview) {

		this(treeview, StringRes.appName);
	}

	public TreeViewFrame(final TreeViewApp treeView, final String appName) {

		super(appName, treeView.getGlobalConfig().node(StringRes.pnode_TVFrame));
		this.treeView = treeView;

		// Initialize main views
		this.welcomeView = new WelcomeView();
		this.dendroView = new DendroView(this);
		this.mainPanel = GUIFactory.createJPanel(true, GUIFactory.NO_INSETS);

		/* Setting up main panels */
		this.viewPanel = GUIFactory.createJPanel(true, GUIFactory.NO_INSETS);

		/* Add main background panel to the application frame's contentPane */
		appFrame.add(mainPanel);

		/* Most recently used files */
		setupFileMru();
		setLoaded(false);

		//Set the java executable icon for non-mac systems
		if(!isMac()) {
			URL iconPath =
				getClass().getClassLoader().getResource("logo.png");
			appFrame.setIconImage(new ImageIcon(iconPath).getImage());
		}
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode == null) {
			LogBuffer.println("Could not set configNode in "
					+ this.getClass().toString() + " because parent was null.");
			return;
		}

		this.configNode = parentNode.node(StringRes.pnode_TVFrame);
	}
	
	@Override
	public Preferences getConfigNode() {

		return configNode;
	}
	
	@Override
	public void requestStoredState() {
		
		importStateFrom(configNode);
	}

	@Override
	public void storeState() {
		
		/* store screen size and position */
		final int left = appFrame.getX();
		final int top = appFrame.getY();
		final int width = appFrame.getWidth();
		final int height = appFrame.getHeight();

		configNode.putInt("frame_left", left);
		configNode.putInt("frame_top", top);
		configNode.putInt("frame_width", width);
		configNode.putInt("frame_height", height);
	}
	
	@Override
	public void importStateFrom(final Preferences oldNode) {
		return; // nothing to import yet
	}

	/**
	 * Generates the appropriate view panel.
	 */
	@Override
	public void generateView(final ViewType view_choice) {

		JPanel view;

		switch (view_choice) {

		case WELCOME_VIEW:
			FileSet mruLast = getFileMRU().getLast();
			view = welcomeView.makeWelcome(mruLast);
			break;

		case PROGRESS_VIEW:
			view = welcomeView.makeLoading();
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

		displayView(view);
	}

	/**
	 * Setting the JPanel to be displayed within TVFrame
	 *
	 * @param view
	 */
	public void displayView(final JPanel view) {

		viewPanel.removeAll();

		viewPanel.add(view, "push, grow");
		mainPanel.add(viewPanel, "push, grow");

		viewPanel.revalidate();
		viewPanel.repaint();
	}

	/**
	 * Displays an editor for recently used files.
	 */
	public void showRecentFileEditor() {

		new FileMruEditor(fileMru).showDialog(appFrame);
	}

	/**
	 * Shows a panel which displays the current stats of the loaded model.
	 */
	public static void openStatsView(final String source, final int rowNum,
			final int colNum) {

		final StatsDialog stats = new StatsDialog(source, rowNum, colNum);
		stats.setVisible(true);
	}

	/**
	 * Displays messages in a JDialog which were logged by TreeView.
	 */
	public void showLogMessages() {

		final JPanel inner = GUIFactory.createJPanel(false,
				GUIFactory.NO_INSETS, null);
		inner.add(new JLabel("JTV Messages"), "span, wrap");
		inner.add(new JScrollPane(
				new LogMessagesPanel(LogBuffer.getSingleton())),
				"push, grow, wrap");

		final LogBuffer buffer = LogBuffer.getSingleton();
		buffer.setLog(true);
		inner.add(new LogSettingsPanel(buffer), "span, push");

		final JDialog top = new JDialog(appFrame, "JTV Messages", false);
		top.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		top.setContentPane(inner);
		top.pack();
		top.setLocationRelativeTo(appFrame);
		top.setVisible(true);
	}

	/**
	 * Displays a window with some helpful information about TreeView 3.
	 */
	public static void showAboutWindow() {

		new AboutDialog().setVisible(true);
	}

	/**
	 * Displays a window with some helpful information about TreeView 3.
	 */
	public static void showShortcuts() {

		new ShortcutDialog().setVisible(true);
	}

	/**
	 * Shows a "Work in Progress" message for unfinished components.
	 */
	public void displayWIP() {

		final JDialog dialog = new JDialog(appFrame);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		final JPanel panel = GUIFactory.createJPanel(true,
				GUIFactory.NO_INSETS, null);
		final JLabel l1 = GUIFactory.createLabel("Work in progress.",
				GUIFactory.FONTS);

		panel.add(l1, "push, alignx 50%");
		dialog.add(panel);

		dialog.pack();
		dialog.setLocationRelativeTo(appFrame);

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
	 */
	public void setRunning(final boolean isDendroLoaded) {

		running = (isDendroLoaded) ? dendroView : null;
	}

	@Override
	public void update(final Observable o, final Object obj) {

		if (o instanceof FileMru) {
			buildMenuBar();
			setChanged();
			notifyObservers();
		} 
		else if (o instanceof TVModel) {
			// TVModel passes a boolean object to notify if it was loaded.
			setRunning((Boolean) obj);
			setLoaded((Boolean) obj);
		} 
		else {
			LogBuffer.println("Observable is: " + o.getClass());
			LogBuffer.println("Got weird update");
		}
	}

	/**
	 * This should be called whenever the loaded status changes It's
	 * responsibility is to change the look of the main view only
	 *
	 */
	@Override
	public void setLoaded(final boolean isModelLoaded) {

		// setGeneFinder(null);
		this.loaded = isModelLoaded;

		if (loaded) {
			if (running == null) {
				setTitleString(appFrame.getName());
				generateView(ViewType.WELCOME_VIEW);
				// JOptionPane.showMessageDialog(applicationFrame,
				// "TreeViewFrame 253: No plugins to display");
			} 
			else {
				setLoadedTitle();
				generateView(ViewType.DENDRO_VIEW);
			}
		} else {
			appFrame.setTitle(StringRes.appName);
			setTitleString(appFrame.getName());
			generateView(ViewType.WELCOME_VIEW);
		}

		buildMenuBar();
	}

	/**
	 * Builds the JMenubar, fills it with JMenuItems and adds listeners to them.
	 * The listeners are called in MenubarController.
	 */
	private void buildMenuBar() {

		LogBuffer.println("Building new MenuBar.");

		menuBar = new JMenuBar();
		appFrame.setJMenuBar(menuBar);

		stackMenuList = new ArrayList<JMenuItem>();

		constructFileMenu(isMac());
		constructHelpMenu(isMac());

		// This will cause TVController to attach appropriate listeners 
		// to JMenuItems.
		setChanged();
		notifyObservers();
	}

	/**
	 * @param isMac - (TODO unused but may change) indicates whether the program
	 * runs on macOS or not.
	 */
	private void constructFileMenu(final boolean isMac) {

		// File
		final JMenu fileMenu = new JMenu(StringRes.mbar_File);
		fileMenu.setMnemonic(KeyEvent.VK_F);

		// Open new file menu
		final JMenuItem openMenuItem = new JMenuItem(StringRes.menu_Open,
			KeyEvent.VK_O);
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		fileMenu.add(openMenuItem);
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
		// Most recently used (mru) files (Open Recent)
		mruFilesMenuList = new ArrayList<JMenuItem>();
		mruFileSetList = new ArrayList<FileSet>();

		final JMenu recentSubMenu = new JMenu(StringRes.menu_OpenRecent);

		final Preferences[] mruFileNodes = fileMru.getConfigs();
		final String mruFileNames[] = fileMru.getFileNames();

		for (int j = mruFileNodes.length; j > 0; j--) {

			mruFileSet = new FileSet(mruFileNodes[j - 1]);
			mruFileSetList.add(mruFileSet);

			final JMenuItem fileSetMenuItem = new JMenuItem(mruFileNames[j - 1]);
			recentSubMenu.add(fileSetMenuItem);
			mruFilesMenuList.add(fileSetMenuItem);
		}

		fileMenu.add(recentSubMenu);

		final JMenuItem editRecentMenuItem = new JMenuItem(
			StringRes.menu_EditRecent);
		fileMenu.add(editRecentMenuItem);
		stackMenuList.add(editRecentMenuItem);

		fileMenu.addSeparator();

		/* Quit Program Menu */
		final JMenuItem quitMenuItem = new JMenuItem(StringRes.menu_QuitWindow,
			KeyEvent.VK_W);
		quitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		fileMenu.add(quitMenuItem);
		stackMenuList.add(quitMenuItem);
		
		if(running != null) {
			fileMenu.addSeparator();
			// Open new file with import dialog menu
			final JMenuItem importMenuItem = new JMenuItem(StringRes.menu_Import, KeyEvent.VK_I);
			importMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			fileMenu.add(importMenuItem);
			stackMenuList.add(importMenuItem);
			
			// Export Menu
			final JMenuItem exportMenuItem = 
				new JMenuItem(StringRes.menu_Export, KeyEvent.VK_E);
			exportMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			fileMenu.add(exportMenuItem);
			stackMenuList.add(exportMenuItem);
			
			fileMenu.addSeparator();

			final JMenuItem saveMenuItem = new JMenuItem(StringRes.menu_SaveAs, 
			                                             KeyEvent.VK_S);
			saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			fileMenu.add(saveMenuItem);
			stackMenuList.add(saveMenuItem);
		}
		
//		 final JMenuItem saveAsMenuItem = new
//		 JMenuItem(StringRes.menu_SaveAs);
//		 fileMenu.add(saveAsMenuItem);
//		 stackMenuList.add(saveAsMenuItem);

		menuBar.add(fileMenu);

		// Preferences
		final JMenu prefSubMenu = new JMenu(StringRes.menu_Prefs);

		// Delete all preferences nodes below 'File'
		final JMenuItem resetPrefs = new JMenuItem(StringRes.menu_ResetPrefs);
		prefSubMenu.add(resetPrefs);
		stackMenuList.add(resetPrefs);

		// Preferences specific to DendroView
		if (running != null) {
			final JMenu viewMenu = new JMenu(StringRes.mbar_View);
			viewMenu.setMnemonic(KeyEvent.VK_V);
			running.addViewMenus(viewMenu);
			menuBar.add(viewMenu);

			// final JMenu searchMenu = new JMenu("Search");
			// searchMenu.setMnemonic(KeyEvent.VK_S);
			// running.addSearchMenus(searchMenu);
			// menuBar.add(searchMenu);

			final JMenu clusterMenu = new JMenu(StringRes.mbar_Cluster);
			clusterMenu.setMnemonic(KeyEvent.VK_C);
			running.addClusterMenus(clusterMenu);
			menuBar.add(clusterMenu);

			// final JMenuItem fontMenuItem = new
			// JMenuItem(StringRes.menu_Font);
			// prefSubMenu.add(fontMenuItem);
			// stackMenuList.add(fontMenuItem);
			
			// Comments the code to disable the URL menu item (File -> Preferences)
			// final JMenuItem urlMenuItem = new JMenuItem(StringRes.menu_URL);
			// prefSubMenu.add(urlMenuItem);
			// stackMenuList.add(urlMenuItem);

			// Functional Enrichment Menu
			// TODO Create the feature.... :)
			// JMenuItem funcEnrMenuItem = new
			// JMenuItem("Functional Enrichment");
			// stackMenu.add(funcEnrMenuItem);
			// stackMenuList.add(funcEnrMenuItem);
		}

		fileMenu.addSeparator();
		fileMenu.add(prefSubMenu);
	}

	/**
	 * @param isMac - (TODO unused but may change) indicates whether the program runs on macOS or not.
	 */
	private void constructHelpMenu(final boolean isMac) {

		/* Help */
		final JMenu helpMenu = new JMenu(StringRes.mbar_Help);
		helpMenu.setMnemonic(KeyEvent.VK_H);

		// if(isMac) {
		// new MacOSXAboutHandler();
		// } else {
		final JMenuItem aboutMenuItem = new JMenuItem(StringRes.menu_About);
		helpMenu.add(aboutMenuItem);
		stackMenuList.add(aboutMenuItem);
		// }

		if (running != null) {
			final JMenuItem statsMenuItem = new JMenuItem(StringRes.menu_Stats);
			helpMenu.add(statsMenuItem);
			stackMenuList.add(statsMenuItem);
		}

		final JMenuItem shortcutMenuItem = new JMenuItem(
				StringRes.menu_Shortcuts);
		helpMenu.add(shortcutMenuItem);
		stackMenuList.add(shortcutMenuItem);

		final JMenuItem logMenuItem = new JMenuItem(StringRes.menu_ShowLog);
		helpMenu.add(logMenuItem);
		stackMenuList.add(logMenuItem);

		// There's currently no documentation page - don't need it for the first
		// release
		// final JMenuItem documentMenuItem = new JMenuItem(
		// StringRes.menu_Docs);
		// helpSubMenu.add(documentMenuItem);
		// stackMenuList.add(documentMenuItem);

		// menubar.addMenuItem("Plugins...");
		// , new ActionListener() {
		//
		// @Override
		// public void actionPerformed(final ActionEvent actionEvent) {
		//
		// displayPluginInfo();
		// }
		// });

		// final JMenuItem feedbackMenuItem = new JMenuItem(
		// StringRes.menu_Feedback);
		// helpMenu.add(feedbackMenuItem);
		// stackMenuList.add(feedbackMenuItem);

		menuBar.add(helpMenu);
	}

	// /**
	// * This method populates the Export Menu with MenuItems.
	// *
	// * @param menubar2
	// */
	// protected void populateExportMenu(final TreeviewMenuBarI menubar) {
	//
	// /*
	// * MenuItem menuItem2 = new MenuItem("Export to Text File... ");
	// * menuItem2.addActionListener(new ActionListener() { public void
	// * actionPerformed(ActionEvent actiosnEvent) { ViewFrame viewFrame =
	// * TreeViewFrame.this; FileSet source = getDataModel().getFileSet();
	// * GeneListMaker t = new GeneListMaker(viewFrame, getGeneSelection(),
	// * getDataModel().getGeneHeaderInfo(), source.getDir()+source.getRoot()
	// * + ".txt"); t.setDataMatrix(getDataModel().getDataMatrix(),
	// * getDataModel().getArrayHeaderInfo(), DataModel.NODATA);
	// * t.bindConfig(getDataModel().getDocumentConfig()
	// * .getNode("GeneListMaker")); t.makeList(); } });
	// * exportMenu.add(menuItem2);
	// */
	//
	// // Save List Menu
	// final JMenuItem saveListMenuItem = (JMenuItem) menubar
	// .addMenuItem("Save List");
	// menubar.setMnemonic(KeyEvent.VK_L);
	// menuList.add(saveListMenuItem);
	//
	// // Save Data Menu
	// final JMenuItem saveDataMenuItem = (JMenuItem) menubar
	// .addMenuItem("Save Data");
	// menubar.setMnemonic(KeyEvent.VK_D);
	// menuList.add(saveDataMenuItem);
	// }

	/* >>>>>>>>>> OSX menu handlers <<<<<<<<< */
	// public class MacOSAboutHandler extends Application {
	// public MacOSAboutHandler() {
	// addApplicationListener(new AboutBoxHandler());
	// }
	// }

	// Various Methods
	/**
	 * Displays the save dialog. Saving is handled by TVController.
	 *
	 * @deprecated
	 */
	@Deprecated
	public void openSaveDialog(final boolean writerEmpty, final String msg) {

		final JDialog dialog = new JDialog(appFrame);
		dialog.setTitle("Information");
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);

		final JPanel panel = GUIFactory.createJPanel(true,
				GUIFactory.NO_INSETS);

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

			final JLabel l3 = GUIFactory
					.createLabel("- Tree Node flips "
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

		return DataModel.NAN;
	}

	/**
	 * This method displays the current plugin info. I set it up as a method so
	 * that it can be overridden by AppletViewFrame
	 */
	// protected void displayPluginInfo() {
	//
	// final MenuHelpPluginsFrame frame = new MenuHelpPluginsFrame(
	// "Current Plugins", this);
	// final File f_currdir = new File(".");
	//
	// try {
	// frame.setSourceText(f_currdir.getCanonicalPath() + File.separator
	// + "plugins" + File.separator);
	//
	// } catch (final IOException e) {
	// frame.setSourceText("Unable to read default plugins directory.");
	// LogBuffer.println("IOException while trying to display "
	// + "Plugin info: " + e.getMessage());
	// }
	//
	// frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	// frame.setVisible(true);
	// }

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

	// @Override
	// public void scrollToGene(final int i) {
	//
	// dendroController.scrollToGene(i);
	// }
	//
	// @Override
	// public void scrollToArray(final int i) {
	//
	// dendroController.scrollToArray(i);
	// }

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

		String newTitle;
		if(isMac()) {
			// no app name in frame title
			newTitle = title;
		} else {
			newTitle = StringRes.appName + ": " + title;
		}

		appFrame.setTitle(newTitle);
	}

	public void setTitleString(final String title) {

		this.title = title;
	}

	/**
	 * Returns the FileSet which has been chosen in the 'Open Recent' menu.
	 * FileSet is found by matching the index of the MenuItem to the index of
	 * the FileSet list which is created by checking the 'FileMRU' Preferences
	 * node for previously stored fileSets.
	 *
	 * @param menuItem
	 * @return FileSet
	 */
	public FileSet findFileSet(final JMenuItem menuItem) {

		if (mruFileSetList.size() != mruFilesMenuList.size()) {
			LogBuffer.println("Sizes of FileSetList and FileMenuList in " +
				"TVFrame don't match.");
			return null;
		}

		int index = -1;
		for (int i = 0; i < mruFilesMenuList.size(); i++) {
			if (mruFilesMenuList.get(i).getText()
					.equalsIgnoreCase(menuItem.getText())) {
				index = i;
				break;
			}
		}

		return mruFileSetList.get(index);
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

		for (final JMenuItem item : mruFilesMenuList) {

			if (item.getActionListeners().length == 0) {
				item.addActionListener(listener);
			}
		}
	}

	public FileSet getFileMenuSet() {

		return mruFileSet;
	}

	public List<JMenuItem> getStackMenus() {

		return stackMenuList;
	}

	public void setArraySelection(final TreeSelection aSelect) {

		this.colSelection = aSelect;
	}

	public void setGeneSelection(final TreeSelection aSelect) {

		this.rowSelection = aSelect;
	}

	// Getters
	@Override
	public TreeViewApp getApp() {

		return treeView;
	}

	@Override
	public UrlPresets getGeneUrlPresets() {

		return treeView.getRowLabelUrlPresets();
	}

	@Override
	public UrlPresets getArrayUrlPresets() {

		return treeView.getColumnLabelUrlPresets();
	}

	@Override
	public boolean isLoaded() {

		return loaded;
	}

	/**
	 * Returns TVFrame's instance of FileMRU
	 *
	 * @return
	 */
	public FileMru getFileMRU() {

		return fileMru;
	}

	public InputMap getInputMap() {

		return viewPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	public ActionMap getActionMap() {

		return viewPanel.getActionMap();
	}

	/**
	 * Returns the parent JPanel of TVFrame which holds the different views.
	 *
	 * @return
	 */
	public DendroPanel getRunning() {

		return running;
	}
}
