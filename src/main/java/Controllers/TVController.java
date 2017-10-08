package Controllers;

import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import ColorChooser.ColorChooserController;
import ColorChooser.ColorChooserUI;
import Utilities.StringRes;
import Views.ClusterDialog;
import Views.ClusterView;
import Views.DataImportController;
import Views.DataImportDialog;
import edu.stanford.genetics.treeview.AllowedFilesFilter;
import edu.stanford.genetics.treeview.CopyType;
import edu.stanford.genetics.treeview.DataMatrix;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.ExportDialog;
import edu.stanford.genetics.treeview.ExportDialogController;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LabelSettings;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.RowListMaker;
import edu.stanford.genetics.treeview.TreeSelection;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.UrlExtractor;
import edu.stanford.genetics.treeview.UrlPresets;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.ViewType;
import edu.stanford.genetics.treeview.model.DataLoadInfo;
import edu.stanford.genetics.treeview.model.ModelLoader;
import edu.stanford.genetics.treeview.model.ModelSaver;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;

/** This class controls user interaction with TVFrame and its views. */
public class TVController implements Observer {

	private final TreeViewFrame tvFrame;
	private final DendroController dendroController;
	private DataModel model;
	private MenubarController menuController;
	private File file;

	public TVController(final TreeViewFrame tvFrame, final DataModel model) {

		this.model = model;
		this.tvFrame = tvFrame;
		this.dendroController = new DendroController(tvFrame, TVController.this);
		this.menuController = new MenubarController(tvFrame, TVController.this);

		// Add the view as observer to the model
		((TVModel) model).addObserver(tvFrame);
		((TVModel) model).addObserver(this);

		tvFrame.addObserver(this);
		tvFrame.getAppFrame().addComponentListener(new AppFrameListener());
		tvFrame.addWindowListener(new WindowActivityListener());

		addViewListeners();
		addMenuListeners();
		addKeyBindings();
	}

	/** Removes all children nodes of the 'File'node. This has the effect that
	 * all preferences stored specific to different loaded data sets are reset. */
	public void resetPreferences() {

		try {
			CustomDetailsConfirmDialog dlg = new CustomDetailsConfirmDialog("Reset Preferences?",
																																			"Are you sure you want to reset the preferences and quit " +
																																														"TreeView?<BR>\nCustom settings such as colors will be reset to " +
																																														"default for all files.",
																																			"Resetting the application-wide preferences can frequently " +
																																																											"resolve behavior and display problems. TreeView3 keeps track " +
																																																											"of input-file-specific settings independent of the input " +
																																																											"file itself, so resetting the preferences affects things " +
																																																											"like custom label and color settings for all previously " +
																																																											"viewed files. These are things like, selected fonts, custom " +
																																																											"color selections, the data values associated with those " +
																																																											"colors in the chosen spectrum, minimum font size, and the " +
																																																											"selected label type to display as the row/column labels. " +
																																																											"Other things such as 'last file opened', the starting " +
																																																											"directory in the open file dialog, and window size/position " +
																																																											"will also be lost. Your data in the files remains untouched. " +
																																																											"Only superficial data is lost. Clustering, trees, and data " +
																																																											"values remain intact.",
																																			"Reset");
			int option = dlg.showDialog();

			switch(option) {

				case CustomDetailsConfirmDialog.OK_OPTION:
					LogBuffer.println("Resetting preferences and quitting.");
					tvFrame.getConfigNode().parent().removeNode();
					tvFrame.getAppFrame().dispose();
					System.exit(0);
					break;

				case CustomDetailsConfirmDialog.CANCEL_OPTION:
					LogBuffer.println("Canceling reset prefs.");
					return;

				default:
					LogBuffer.println("Reset prefs dialog was closed.");
					return;
			}

		}
		catch(final BackingStoreException e) {
			final String message = "Issue while resetting preferences.";
			JOptionPane.showMessageDialog(Frame.getFrames()[0], message, "Error", JOptionPane.ERROR_MESSAGE);
			LogBuffer.println(e.getMessage());
			return;
		}
	}

	/** Adds keyboard mapping to general TreeViewFrame. The keyboard mapping here
	 * is general application stuff, as opposed to GlobalView specific etc. */
	private void addKeyBindings() {

		final InputMap input_map = tvFrame.getInputMap();
		final ActionMap action_map = tvFrame.getActionMap();

		/* Gets the system's modifier key (Ctrl or Cmd) */
		final int modifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, modifier), "openCluster");
		action_map.put("openCluster", new ClusterAction());
	}

	/* ------------ Add listeners -------------------------------- */
	/** Adds listeners to views that are instantiated in TVFrame. */
	private void addViewListeners() {

		if(tvFrame.getWelcomeView() != null) {
			tvFrame.getWelcomeView().addLoadListener(new LoadButtonListener());
			tvFrame	.getWelcomeView()
							.addLoadLastListener(new LoadLastButtonListener());
		}
	}

	public void toggleTrees() {

		dendroController.toggleTrees();
	}

	/** Generates the MenuBar controller. Causes listeners to be added for the
	 * main MenuBar as well as the listed file names in 'Recent Files'. file
	 * names in . */
	private void addMenuListeners() {

		LogBuffer.println("ADDING MENU LISTENERS -----");

		tvFrame.addMenuActionListeners(new StackMenuListener());
		tvFrame.addFileMenuListeners(new FileMenuListener());
	}

	/** Calls a sequence of methods related to loading a file, when its button is
	 * clicked.*/
	private class LoadButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			openFile(null, false);
		}
	}

	/** Initiates loading of last used file.*/
	private class LoadLastButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			final FileSet last = tvFrame.getFileMRU().getLast();
			openFile(last, false);
		}
	}

	/** Opens hierarchical cluster menu */
	private class ClusterAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			setupClusterView(ClusterView.HIER);
		}
	}

	/* >>>>>>>>> Component listeners <<<<<<<<<<<< */
	/** Adds the listeners to all JMenuItems in the main menubar.
	 */
	private class StackMenuListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			if(e.getSource() instanceof JMenuItem) {

				final String title = ((JMenuItem) e.getSource()).getText();
				menuController.execute(title);
			}
		}
	}

	/** The window position/size should be saved when it changes (or after
	 * a move has finished). There are two reasons for this:
	 * 1) Quitting the app via the app menu or command-q does not initiate
	 * the save (which could be rectified via packaging).
	 * 2) If the app crashes, the position/size will be lost
	 * 
	 * Listens to the resizing of DendroView and makes changes to MapContainers
	 * as a result. */
	private class AppFrameListener extends ComponentAdapter {

		// Timer to prevent repeatedly saving window dimensions upon resize
		private final int saveResizeDelay = 1000;
		private javax.swing.Timer saveResizeTimer;
		ActionListener saveWindowAttrs = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent evt) {
				if(evt.getSource() == saveResizeTimer) {
					/* Stop timer */
					saveResizeTimer.stop();
					saveResizeTimer = null;

					tvFrame.storeState();
				}
			}
		};

		@Override
		public void componentResized(final ComponentEvent arg0) {

			// Previously, resetMapContainers was called here, but that caused
			// the zoom level to change when the user resized the window, so I
			// added a way to track the currently visible area in mapContainer
			// and implemented these functions to make the necessary
			// adjustments to the image when that happens
			if(dendroController != null) {
				dendroController.refocusViewPort();
			}

			// Save the new dimensions/position if it's done changing
			if(this.saveResizeTimer == null) {
				/*
				 * Start waiting for saveResizeDelay millis to elapse and then
				 * call actionPerformed of the ActionListener "saveWindowAttrs".
				 */
				this.saveResizeTimer = new Timer(this.saveResizeDelay, saveWindowAttrs);
				this.saveResizeTimer.start();

			}
			else {
				/* Event came too soon, swallow it by resetting the timer.. */
				this.saveResizeTimer.restart();
			}
		}
	}

	/**
	 * Controls window activity.
	 */
	private class WindowActivityListener extends WindowAdapter {
		
		@Override
		public void windowActivated(final WindowEvent windowEvent) {

			tvFrame.setWindowActive(true);
		}

		@Override
		public void windowClosing(final WindowEvent windowEvent) {

			closeWindow();
		}

		@Override
		public void windowDeactivated(final WindowEvent windowEvent) {

			tvFrame.setWindowActive(false);
		}
	}
	
	/** Load data into the model.
	 * 
	 * @param fileSet - The fileSet to be loaded.
	 * @param dataInfo - Contains information on how the data should be loaded.
	 *          This information is determined by the
	 *          user in the import dialog. If the file has been loaded before, the
	 *          information can come from stored preferences
	 *          data. */
	public void loadData(final FileSet fileSet, final DataLoadInfo dataInfo) {

		// Loading TVModel
		final TVModel tvModel = (TVModel) model;

		try {
			// first, ensure reset of the model data
			tvModel.resetState();
			tvModel.setSource(fileSet);
	    LogBuffer.println("Loading FileSet: " + fileSet);

	    tvFrame.generateView(ViewType.PROGRESS_VIEW);
	    
			final ModelLoader loader = new ModelLoader(tvModel, this, dataInfo);
			loader.execute();
		}
		catch(final OutOfMemoryError e) {
			final String oomError = "The data file is too large. " +
															"Increase the JVM's heap size. Error: " + e
																																					.getMessage();
			JOptionPane.showMessageDialog(Frame.getFrames()[0], oomError, "Out of memory", JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Finish up loading by setting the model loaded status to true and creating
	 * a new DendroView.
	 * 
	 * @param dataInfo - An object containing important information for data
	 *          loading including user selections from the import dialog, if
	 *          available. */
	public void finishLoading(final DataLoadInfo dataInfo) {

		if(model.getDataMatrix().getNumRow() > 0) {
			/* Will notify view of successful loading. */
			((TVModel) model).setLoaded(true);
			setDataModel();
			dendroController.setNewMatrix(tvFrame.getDendroView(), model);

//			if(fileMenuSet != null) {
//				/*
//				 * TODO Needs to happen after setNewMatrix because a new
//				 * ColorExtractor object is created, which would void the 
//				 * updated ColorExtractor state if copying happens before. 
//				 * Implement a nicer solution one day...
//				 */
//				importOldPreferencesFrom(dataInfo.getOldNode());
//
//				this.fileMenuSet = tvFrame.getFileMRU().addUnique(fileMenuSet);
//				tvFrame.getFileMRU().setLast(fileMenuSet);
//				fileMenuSet = null;
//			}
			// Check if TVModel FileSet was updated
			if(dataInfo.getOldFileSet() != null && 
				!model.getFileSet().equals(dataInfo.getOldFileSet())) {
				LogBuffer.println("Importing old preferences.");
				/*
				 * TODO Needs to happen after setNewMatrix because a new
				 * ColorExtractor object is created, which would void the 
				 * updated ColorExtractor state if copying happens before. 
				 * Implement a nicer solution one day...
				 */
				importOldPreferencesFrom(dataInfo.getOldNode());
			}
			else {
				LogBuffer.println("FileSet is null. Could not load old preferences.");
			}

			dendroController.restoreComponentStates();
			updateFileMRU();
			LogBuffer.println("Successfully loaded: " + model.getSource());
		}
		else {
			final String message = "No numeric data could be found in the " +
															"input file.\nThe input file must contain tab-delimited " +
															"numeric values.";
			JOptionPane.showMessageDialog(Frame.getFrames()[0], message, "Alert", JOptionPane.WARNING_MESSAGE);
			LogBuffer.println("Alert: " + message);

			// Set model status, which will update the view.
			((TVModel) model).setLoaded(false);

			//Bring the user back to the load dialog to try again or cancel load
			openFile(null, true);
		}

		addViewListeners();
		addMenuListeners();
	}

	/** Checks if the root File node is parent to a node with the supplied
	 * srcName and srcExtension stored as name values.
	 * 
	 * @param srcFileSetName
	 * @return The target Preferences node, if it exists. Otherwise null. */
	private Preferences getModelNodeFromFileSet(final String srcFileSetName) {

		try {
			/* First, get the relevant old node... */
			Preferences fileNode;
			if(getConfigNode().nodeExists("File")) {
				fileNode = getConfigNode().node("File");
			}
			else {
				LogBuffer.println("File node not found. Could not copy data.");
				return null;
			}

			return getTargetModelNode(fileNode, srcFileSetName);
		}
		catch(BackingStoreException e) {
			LogBuffer.logException(e);
			return null;
		}
	}

	/** Copies label and color data from the pre-clustered file to the
	 * Preferences node of the post-clustered file.
	 * 
	 * @param loadedNode - A node from which to import preferences settings. */
	private void importOldPreferencesFrom(final Preferences loadedNode) {

		if(loadedNode == null) {
			LogBuffer.println("No old node was found when trying to copy old" +
												" preferences. Aborting import attempt.");
			return;
		}

		try {
			dendroController.importLabelPreferences(loadedNode);
			dendroController.importColorPreferences(loadedNode);

		}
		catch(BackingStoreException e) {
			LogBuffer.logException(e);
			return;
		}
	}

	/** Finds a specific sub-node in a root node by looking for keys with the
	 * srcName and srcExtension.
	 * 
	 * @param fileNode - The File node under which all Model nodes are stored.
	 * @param srcFileSetName
	 * @return The target Preferences node.
	 * @throws BackingStoreException */
	private static Preferences getTargetModelNode(	final Preferences fileNode,
																						final String srcFileSetName) 
																							throws BackingStoreException {

		LogBuffer.println("Looking for target node (" + srcFileSetName + ")");
		
		String[] fileNodes = fileNode.childrenNames();
		Preferences targetNode = null;
		for(String nodeName : fileNodes) {
			Preferences node = fileNode.node(nodeName);
			String connectedFS = node.get("connectedFileSet", "generic-tv-file");
			
			LogBuffer.println("Checking node (" + connectedFS + ")");
			if(connectedFS.equalsIgnoreCase(srcFileSetName)) {
				LogBuffer.println("Success. Target node found.");
				targetNode = node;
				break;
			}
		}

		/* Interrupt if no node is found to copy old data. */
		if(targetNode == null) {
			LogBuffer.println("Target node not found. Could not" + " copy data.");
			return null;
		}

		return targetNode;
	}

	/** This method opens a file dialog to open either the visualization view or
	 * the cluster view depending on which file type is chosen.
	 * 
	 * @param newFileSet - A FileSet object representing the files to be loaded.
	 * @param shouldUseImport - Explicitly tells the loader function called in
	 *          this method to use the import dialog
	 *          for opening a file. Only used through menubar's 'File > Open File
	 *          With Import Dialog...' at the moment.
	 * @throws LoadException */
	public void openFile(FileSet newFileSet, final boolean shouldUseImport) {

		String message;
		FileSet loadFileSet = newFileSet;
		DataLoadInfo dataLoadInfo;
		
		if(model.isLoaded() && model.getModified()) {
			LogBuffer.println("Another Model is loaded and modified. " +
				"Asking for save.");
			askSaveModified();
			// Only continue if finished
		}

		try {
			if(loadFileSet == null) {
				this.file = tvFrame.selectFile();

				// Only run loader, if JFileChooser wasn't canceled.
				if(file == null) {
					message = "File selection was cancelled.";
					LogBuffer.println(message);
					return; 
				}

				loadFileSet = ViewFrame.getFileSet(file);
			}

			dataLoadInfo = getDataLoadInfo(loadFileSet, null, 
			                               false, shouldUseImport);
			
			// Cannot load without this information.
			if(dataLoadInfo == null) {
				message = "Could not get DataLoadInfo for loading. Aborting.";
				LogBuffer.println(message);
				return;
			}
			
			loadData(loadFileSet, dataLoadInfo);
		}
		catch(final LoadException e) {
			message = "Loading the file was interrupted.";
			showWarning(message);
			LogBuffer.logException(e);
			return;
		}
	}
	
	/**
	 * Updates all TVModel data, labels and generates a temporary 
	 * preferences node without saving to file. This is exclusively used 
	 * post-clustering for now.
	 */
	public void loadClusteredModel(final TVModel newModel) {
		
		FileSet oldFileSet = model.getFileSet();
		String oldFileSetName = oldFileSet.getName();
		Preferences oldModelNode = getModelNodeFromFileSet(oldFileSetName);
		DataLoadInfo dataLoadInfo = getStoredDataLoadInfo(oldFileSet, oldModelNode);
		
		if(dataLoadInfo == null) {
			String message = "Updating data after clustering was interrupted.";
			LogBuffer.println(message);
			return;
		}

		dataLoadInfo.setIsClusteredFile(true);
		model = newModel;
		finishLoading(dataLoadInfo);
	}

	/** Used to transfer information to ModelLoader about the data
	 * for proper loading and import of settings from old Preferences.
	 * Either through saved information (stored preferences) or by offering
	 * a dialog to the user in which they can specify parameters.
	 * The identifiers for the old FileSet are passed as Strings because
	 * passing another FileSet object does not work. They self-update their
	 * state to the 'active' FileSet using a Preferences node, overwriting
	 * their old data.
	 * 
	 * @param newFileSet File name + directory information object.
	 * @param oldFileSetName The root name of the old FileSet (FileSet.getRoot())
	 * @param oldExt The extension of the old FileSet (FileSet.getExt())
	 * @param isFromCluster Whether the loading happens as a result of
	 *          clustering. 
	 * @return A DataLoadInfo object containing all the relevant information
	 * for loading a data set.
	 */
	private DataLoadInfo getDataLoadInfo(final FileSet newFileSet, 
	                                     final String oldFileSetName,
	                                     boolean isFromCluster,
	                                     boolean shouldUseImport) {
		LogBuffer.println("GET DATA LOAD INFO");
		LogBuffer.println("Old Root: " + oldFileSetName);
		Preferences oldModelNode;
		// Step 1) Retrieve possible old data load information
		// Transfer settings to clustered file
		// Case 1) Coming from clustering. Solution: transfer existing node info
		// to new node.
		//if(isFromCluster && oldRoot != null && oldExt != null) {
		if(oldFileSetName != null) {
			LogBuffer.println("Getting preferences for transfer to clustered file.");
			oldModelNode = getModelNodeFromFileSet(oldFileSetName);
		}
		// Case 2) Not from cluster, but the same file might have been loaded 
		// before such that a Preferences node already exists. Solution: use the
		// old node, if it exists.
		else {
			LogBuffer.println("Checking if preferences exist for new file.");
			// TODO naming here is confusing: this looks for potentially existing 
			// nodes of the new FileSet
			oldModelNode = getModelNodeFromFileSet(newFileSet.getName());
		}
		
		// Case 3) A new file is created (saved) from the currently open file.
		// Solution: transfer info from the existing node (same as Case 1)

		// Step 2) Get the information from a node or use import dialog to define 
		// new information
		
		// Case 1) No node was found, or import dialog should explicitly be used
		DataLoadInfo dataLoadInfo;
		if(oldModelNode == null || shouldUseImport) {
			LogBuffer.println("Using import dialog.");
			dataLoadInfo = useImportDialog(newFileSet);
		}
		else {
			LogBuffer.println("Loading with info from existing node.");
			dataLoadInfo = getStoredDataLoadInfo(newFileSet, oldModelNode);
		}

		// If we don't have valid information here, something was 
		// cancelled or messed up.
		if(dataLoadInfo == null) {
			String message = "DataLoadInfo could not be defined.";
			LogBuffer.println(message);
			return null;
		}

		dataLoadInfo.setIsClusteredFile(isFromCluster);
		return dataLoadInfo;
	}

	/** Show a dialog for the user to specify how his data should be loaded.
	 * Retrieves the user chosen options and returns them in a DataInfo object.
	 * 
	 * @param filename
	 * @return Options/ parameters for data loading. */
	private static DataLoadInfo useImportDialog(final FileSet fileSet) {

		DataImportDialog loadPreview = new DataImportDialog(fileSet.getName());
		DataImportController importController = 
			new DataImportController(loadPreview);

		String[][] previewData;
		importController.setFileSet(fileSet);
		previewData = importController.loadPreviewData();

		loadPreview.setNewTable(previewData);
		importController.initDialog();

		DataLoadInfo dataInfo = loadPreview.showDialog();

		return dataInfo;
	}

	/** Load stored info for a specific file.
	 * 
	 * @param fileSet The <code>FileSet</code> for the file to be loaded.
	 * @return A <code>DataLoadInfo</code> object which contains information
	 *         relevant for setting up the <code>DataLoadDialog</code>. */
	public static DataLoadInfo getStoredDataLoadInfo(	final FileSet fileSet,
																										final Preferences node) {

		DataLoadInfo dataInfo = new DataLoadInfo(node);

		// Amount of label types may vary when loading, so they have to be re-detected
		DataImportController importController = new DataImportController(dataInfo
																																							.getDelimiter());
		importController.setFileSet(fileSet);

		// FIXME detection does not work
		int[] newDataCoords = importController.detectDataBoundaries(dataInfo);

		// the number of label types may have been altered, e.g. by clustering
		if(dataInfo.needsDataCoordsUpdate(newDataCoords)) {
			LogBuffer.println("Data start coordinates have shifted because more " +
												"label types were added.");
			dataInfo.setDataStartCoords(newDataCoords);

			/* prevent old node from updating coordinates unless fileSet represents
			 * the exact same file as the node. In this case coordinates need to 
			 * be updated here, as found by dataInfo.needsDataCoordsUpdate() but
			 * otherwise the new coordinates are unrelated to the old node! */
			String oldFileName = node.get("name", "") + node.get("extension", "");
			String newFileName = fileSet.getRoot() + fileSet.getExt();

			if(oldFileName.equals(newFileName)) {
				node.putInt("rowCoord", newDataCoords[0]);
				node.putInt("colCoord", newDataCoords[1]);
			}
		}

		return dataInfo;
	}

	/** Allows user to load a file from a URL
	 *
	 * @return FileSet
	 * @throws LoadException */
	protected FileSet offerUrlSelection() throws LoadException {

		FileSet fileSet1;
		/*
		 * JTextField textField = new JTextField(); JPanel prompt = new
		 * JPanel(); prompt.setLayout(new BorderLayout()); prompt.add(new
		 * JLabel("Enter a Url"), BorderLayout.NORTH); prompt.add(textField,
		 * BorderLayout.CENTER);
		 */
		// get string from user...
		final String urlString = JOptionPane.showInputDialog(this, "Enter a Url");

		if(urlString != null) {
			// must parse out name, parent + sep...
			final int postfix = urlString.lastIndexOf("/") + 1;
			final String name = urlString.substring(postfix);
			final String parent = urlString.substring(0, postfix);
			fileSet1 = new FileSet(name, parent);

		}
		else {
			throw new LoadException("Input Dialog closed without selection...",
															LoadException.NOFILE);
		}

		return fileSet1;
	}

	/** Sets up the following: 1) urlExtractor, an object that generates urls
	 * from gene indexes 2) arrayUrlExtractor, similarly 3) geneSelection and 4)
	 * arraySelection, the two selection objects. It is important that these are
	 * set up before any plugins are instantiated. This is called before
	 * setupRunning by setDataModel. */
	protected void setupExtractors() {

		final DataMatrix matrix = model.getDataMatrix();
		final int ngene = matrix.getNumRow();
		final int nexpr = matrix.getNumCol();

		final Preferences documentConfig = model.getDocumentConfigRoot();

		// extractors...
		final UrlPresets genePresets = tvFrame.getGeneUrlPresets();
		final UrlExtractor urlExtractor = new UrlExtractor(	model.getRowLabelInfo(),
																												genePresets);

		urlExtractor.bindConfig(documentConfig.node("UrlExtractor"));
		tvFrame.setUrlExtractor(urlExtractor);

		final UrlPresets arrayPresets = tvFrame.getArrayUrlPresets();
		final UrlExtractor arrayUrlExtractor = new UrlExtractor(model.getColLabelInfo(),
																														arrayPresets);

		arrayUrlExtractor.bindConfig(documentConfig.node("ArrayUrlExtractor"));
		tvFrame.setArrayUrlExtractor(arrayUrlExtractor);

		tvFrame.setGeneSelection(new TreeSelection(ngene));
		tvFrame.setArraySelection(new TreeSelection(nexpr));
	}

	/** Opens the ClusterViewFrame with either the options for hierarchical
	 * clustering or K-Means, depending on the boolean parameter.
	 *
	 * @param hierarchical */
	public void setupClusterView(final int clusterType) {

		/* Erase selection */
		dendroController.deselectAll();

		/* Making a new dialog to display cluster UI */
		final ClusterDialog clusterView = new ClusterDialog(clusterType);

		/* Creating the Controller for this view. */
		ClusterDialogController cController = new ClusterDialogController(
																																			clusterView,
																																			TVController.this);

		cController.displayView();
	}

	/** Sets extractors and FileSetListeners. */
	public void setDataModel() {

		if(model != null) {
			model.clearFileSetListeners();
			model.addFileSetListener(tvFrame);

			setupExtractors();
		}
	}

	/** Opens an instance of GeneListMaker used to save a list of genes. */
	public void saveList() {

		if(warnSelectionEmpty()) {
			final FileSet source = model.getFileSet();
			String def = model.getName() + "_list.txt";

			if(source != null) {

				def = source.getDir() + source.getRoot() + "_list.txt";
			}

			final RowListMaker t = new RowListMaker((JFrame) Frame.getFrames()[0],
																							tvFrame.getRowSelection(), model
																																							.getRowLabelInfo(),
																							def);

			t.setDataMatrix(model.getDataMatrix(), model.getColLabelInfo(), DataModel.NAN);

			t.setConfigNode(tvFrame.getConfigNode());

			t.pack();
			t.setVisible(true);
		}
	}

	/** Opens instance of GeneListMaker to save data. */
	public void saveData() {

		if(warnSelectionEmpty()) {
			final FileSet source = model.getFileSet();
			final RowListMaker t = new RowListMaker((JFrame) Frame.getFrames()[0],
																							tvFrame.getRowSelection(), model
																																							.getRowLabelInfo(),
																							source.getDir() + source.getRoot() + "_data.cdt");

			t.setDataMatrix(model.getDataMatrix(), model.getColLabelInfo(), DataModel.NAN);

			t.setConfigNode(tvFrame.getConfigNode());

			t.includeAll();
			t.pack();
			t.setVisible(true);
		}
	}

	/** Generates a warning message if TreeSelectionI object is null and returns
	 * false in that case.
	 *
	 * @return boolean */
	public boolean warnSelectionEmpty() {

		final TreeSelectionI treeSelection = tvFrame.getRowSelection();

		if((treeSelection == null) || (treeSelection.getNSelectedIndexes() <= 0)) {

			JOptionPane.showMessageDialog(Frame.getFrames()[0], "Cannot generate gene list, no gene selected");
			return false;
		}
		return true;
	}

	/** Saves the current model, GUI handled by TVFrame.
	 *
	 * @param The path at which the file should be saved.
	 * @return String name of the saved file */
	private boolean doModelSave(final Path path) {

		if(path == null) {
			String msg = "No defined file path. Could not save the file.";
			JOptionPane.showMessageDialog(JFrame.getFrames()[0], msg);
			LogBuffer.println(msg);
			return false;
		}
		
		ModelSaver ms = new ModelSaver(TVController.this);
		return ms.save(model, path);
	}
	
	/**
	 * When the worker thread in ModelSaver is done, it  
	 * @param wasSuccessful
	 * @param oldFileSetName - The name of the FileSet attached to the Model 
	 * before the new one was added during the saving process. It is needed to 
	 * transfer Preferences information.
	 */
	public void finishModelSave(final boolean wasSuccessful, 
	                            final String oldFileSetName) {
		
		if(wasSuccessful) {
			// Update Model node associated with the TVModel to permanent storage.
			Preferences fileNode = getConfigNode().node("File");
			boolean isFromCluster = model.isRowClustered() || model.isColClustered();
			LogBuffer.println("Successful save, old root: " + oldFileSetName);
			DataLoadInfo dataLoadInfo = getDataLoadInfo(model.getFileSet(),
			                                            oldFileSetName, 
			                                            isFromCluster, false);
			ModelLoader.storeDataLoadInfo(fileNode, model, dataLoadInfo);
			updateFileMRU();
		}
		
		// Transfer old Preferences to new Model entry
		
		// Model now counts as not modified
		model.setModified(false);
		LogBuffer.println("doModelSave() finished with FileSet " 
		+ model.getFileSet());
	}

	/** Saves the model as a user specified file. */
	public void saveModelAs() {

		if(model.getFileSet() == null) {
			JOptionPane.showMessageDialog(Frame.getFrames()[0], 
			                              "Saving of datamodels not backed by "
			                              	+ "files is not yet supported.");
			return;
		}
	  
		final JFileChooser fileDialog = new JFileChooser();
	  final AllowedFilesFilter ff = new AllowedFilesFilter();
		fileDialog.setFileFilter(ff);

		final String dir = model.getFileSet().getDir();
		if(dir != null) {
			fileDialog.setCurrentDirectory(new File(dir));
		}

		final int retVal = fileDialog.showSaveDialog(JFrame.getFrames()[0]);
		if(retVal == JFileChooser.APPROVE_OPTION) {
			LogBuffer.println("Saving file.");
			// File extension is not taken care of
			String selectedPath = fileDialog.getSelectedFile().getAbsolutePath();
			Path path = Paths.get(selectedPath);
			doModelSave(path);
		}
	}
	
	/**
	 * If the Model was modified, this method will pop up a dialog asking 
	 * the user if he would like to save the modified data. If confirmed, 
	 * the saving process is initialized.
	 */
	private void askSaveModified() {
		
		if(!model.getModified()) {
			LogBuffer.println("Dialog for saving modified data was attempted to " +
				"be opened although the model does not appear modified.");
			return;
		}
		
		String title = "Save Modified Data";
		String message = "The current file seems to be modified. " +
			"Would you like to save modifications?";
		int retVal = JOptionPane.showConfirmDialog(Frame.getFrames()[0], 
		                                           message, 
		                                           title, 
		                                           JOptionPane.YES_NO_OPTION,  
		                                           JOptionPane.QUESTION_MESSAGE);
		if(retVal == JOptionPane.YES_OPTION) {
			saveModelAs();
		}
		else {
			LogBuffer.println("Modified data not saved.");
		}
	}
	
	/**
	 * Updates the FileMRU according to the current FileSet attached to the model.
	 */
	private void updateFileMRU() {
		tvFrame.getFileMRU().addUnique(model.getFileSet());
		tvFrame.getFileMRU().setLast(model.getFileSet());
		tvFrame.addFileMenuListeners(new FileMenuListener());
	}

	/** This class is an ActionListener which overrides the run() function. */
	
	private class FileMenuListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent actionEvent) {

			//tvFrame.getFileMRU().setLast(tvFrame.getFileMenuSet());
			tvFrame.getFileMRU().setLast(model.getFileSet());
			tvFrame.getFileMRU().notifyObservers();

			FileSet newFS = tvFrame.findFileSet((JMenuItem) actionEvent.getSource());
			openFile(newFS, false);
		}
	}
	

	/** Opens the preferences menu and sets the displayed menu to the specified
	 * option using a string as identification.
	 *
	 * @param menu - The type of opened menu distinguished by its String name. */
	@SuppressWarnings("unused") // LabelSettingsController doesn't need to be stored in a variable
	public void openLabelMenu(final String menu) {

		final LabelSettings labelSettingsView = new LabelSettings(tvFrame);

		if(menu.equalsIgnoreCase(StringRes.menu_RowAndCol)) {
			labelSettingsView.setLabelInfo(model.getRowLabelInfo(), model.getColLabelInfo());
		}

		labelSettingsView.setMenu(menu);
		new LabelSettingsController(tvFrame, model, labelSettingsView);
		labelSettingsView.setVisible(true);
	}

	/** Opens the preferences menu and sets the displayed menu to the specified
	 * option using a string as identification.
	 *
	 * @param menu */
	@SuppressWarnings("unused") // ExportDialogController doesn't need to be stored in a variable
	public void openExportMenu() {

		if(tvFrame.getDendroView() == null || !tvFrame.isLoaded()) {
			LogBuffer.println("DendroView is not instantiated. " +
												"Nothing to export.");
			return;
		}

		ExportHandler eh = new ExportHandler(	tvFrame.getDendroView(),
																					dendroController.getInteractiveXMap(),
																					dendroController.getInteractiveYMap(),
																					tvFrame.getColSelection(), tvFrame
																																						.getRowSelection());

		boolean selectionsExist = (tvFrame.getColSelection() != null && tvFrame
																																						.getColSelection()
																																						.getNSelectedIndexes() > 0);

		ExportDialog exportDialog = new ExportDialog(selectionsExist, eh);

		new ExportDialogController(	exportDialog, tvFrame, dendroController
																																			.getInteractiveXMap(),
																dendroController.getInteractiveYMap(), model);
	}

	/** Opens up the color chooser dialog. */
	public void openColorMenu() {

		// --> move to dendroController
		final double min = model.getDataMatrix().getMinVal();
		final double max = model.getDataMatrix().getMaxVal();
		final double mean = model.getDataMatrix().getMean();
		final double median = model.getDataMatrix().getMedian();

		ColorExtractor colorExtractor = dendroController.getColorExtractor();

		final ColorChooserUI colorChooserUI = new ColorChooserUI(	colorExtractor,
																															min, max, mean,
																															median);
		ColorChooserController controller = new ColorChooserController(
																																		colorChooserUI);

		controller.addObserver(dendroController.getInteractiveMatrixView());
		controller.addObserver(dendroController.getGlobalMatrixView());

		colorChooserUI.setVisible(true);
	}

	private void showWarning(final String message) {

		JOptionPane.showMessageDialog(tvFrame.getAppFrame(), message, "Warning", JOptionPane.WARNING_MESSAGE);
		LogBuffer.println(message);
	}

	/*
	 * Gets the main view's config node.
	 */
	public Preferences getConfigNode() {

		return tvFrame.getConfigNode();
	}

	/** Returns TVController's model.
	 *
	 * @return */
	public DataModel getDataModel() {

		return model;
	}

	@Override
	public void update(final Observable o, final Object arg) {

		/* when tvFrame rebuilds its menu */
		if(o instanceof ViewFrame) {
			LogBuffer.println("UPDATE FROM VIEWFRAME");
			addMenuListeners();
		} 
		else if(o instanceof TVModel) {
			if(model.getModified()) {
				tvFrame.setTitleString(model.getFileName() + "-[modified]");
			} 
			else {
				tvFrame.setTitleString(model.getFileName());
			}
		}
	}

	/** Relays copy call to dendroController.
	 * 
	 * @param copyType
	 * @param isRows */
	public void copyLabels(final CopyType copyType, final boolean isRows) {

		dendroController.copyLabels(copyType, isRows);
	}
	
	/**
	 * checks if DataModel has been modified. Stores a configFile when closing
	 * the window.
	 */
	public void closeWindow() {

		// Confirm user's intent to exit the application.
		String[] options = {"Quit","Cancel"};
		final int choice = JOptionPane.showOptionDialog(tvFrame.getAppFrame(),
			"Quit TreeView?", "Quit TreeView?",JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE,null,options,options[1]);

		switch (choice) {
			case JOptionPane.OK_OPTION:
				LogBuffer.println("Saving settings before window close.");

				// Not sure a call to storeState is necessary anymore because
				// added calls upon window resize and window move in
				// DendroController and ViewFrame respectively. If it does
				// something other than save those two things, then sure,
				// there's reason to keep it. However, note that resizing the
				// window without data loaded does not save settings because
				// it's tied to the matrix jpanel
				if(model != null && model.getModified()) {
					if(userWantsToSave()) saveModelAs();
				}
				
				tvFrame.storeState();
				tvFrame.getAppFrame().dispose();

				//The JVM exits when the last non-daemon thread exits.
				//System.exit(0) is called here because there are some threads
				//which I have found to be in wait-mode after execution (via
				//profiling). Plain termination of the main() method does not
				//guarantee that the JVM fully shuts down, since non-daemon
				//threads may still be alive.  It's a fail-safe for now and
				//could probably be removed at some point if we can ensure those
				//threads get cleaned up.  Refer to issue #74 on bitbucket for
				//more info:
				//https://bitbucket.org/TreeView3Dev/treeview3/issues/74/selecting-close-window-leaves-the-app-in-a
				System.exit(0);

				break;
			case JOptionPane.CANCEL_OPTION:
				LogBuffer.println("User decided not to quit treeview.");
				return;
			default:
				LogBuffer.println("User closed the confirmation window (same " +
					"as cancel).");
				return;
		}
	}
	
	/**
	 * Opens a dialog for the user, asking if he/she wants to save the data model.
	 * @return boolean - whether the user wants to save the modified DataModel to a new
	 * file.
	 */
	public boolean userWantsToSave() {
		
		// No need to ask or save anything, if the DataModel was not modified.
		if(!model.getModified()) {
			LogBuffer.println("DataModel not recognized as modified. " +
				"Nothing to save.");
			return false;
		}

		// Confirm user's intent to exit the application.
		final int choice = JOptionPane.showOptionDialog(tvFrame.getAppFrame(),
			"It appears there is unsaved data. Do you want to save now?", 
			"Save data", JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE,null,null,null);

		switch (choice) {
			case JOptionPane.YES_OPTION:
				LogBuffer.println("User decided to save to file.");
				return true;
			case JOptionPane.NO_OPTION:
				LogBuffer.println("User decided not to save to file.");
				return false;
			default:
				LogBuffer.println("User closed the confirmation window (same " +
					"as not saving to file).");
				return false;
		}
	}
}
