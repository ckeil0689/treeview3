package Controllers;

import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
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

import ColorChooser.ColorChooserController;
import ColorChooser.ColorChooserUI;
import Utilities.StringRes;
import Views.ClusterDialog;
import Views.ClusterView;
import Views.DataImportController;
import Views.DataImportDialog;
import edu.stanford.genetics.treeview.CdtFilter;
import edu.stanford.genetics.treeview.CopyType;
import edu.stanford.genetics.treeview.DataMatrix;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.DataModelFileType;
import edu.stanford.genetics.treeview.ExportDialog;
import edu.stanford.genetics.treeview.ExportDialogController;
import edu.stanford.genetics.treeview.ExportPreviewMatrix;
import edu.stanford.genetics.treeview.ExportPreviewTrees;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.GeneListMaker;
import edu.stanford.genetics.treeview.LabelSettings;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeSelection;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.UrlExtractor;
import edu.stanford.genetics.treeview.UrlPresets;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.model.DataLoadInfo;
import edu.stanford.genetics.treeview.model.DataModelWriter;
import edu.stanford.genetics.treeview.model.ModelLoader;
import edu.stanford.genetics.treeview.model.ReorderedDataModel;
import edu.stanford.genetics.treeview.model.TVModel;
//<<<<<<< HEAD
//import edu.stanford.genetics.treeview.plugin.dendroview.ColorPresets;
//import edu.stanford.genetics.treeview.plugin.dendroview.ColorSet;
//=======
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;
import edu.stanford.genetics.treeview.plugin.dendroview.TRView;
//>>>>>>> colorUpdate

/**
 * This class controls user interaction with TVFrame and its views.
 *
 * @author CKeil
 *
 */
public class TVController implements Observer {

	private final DataModel model;
	private final TreeViewFrame tvFrame;
	private final DendroController dendroController;
	private MenubarController menuController;
	private File file;
	private FileSet fileMenuSet;

	private Preferences oldNode;
	private String[] clusterNodeSourceKeys;
	private final String[] selectedLabels;

	public TVController(final TreeViewFrame tvFrame, final DataModel model) {

		this.model = model;
		this.tvFrame = tvFrame;
		this.dendroController = new DendroController(tvFrame, this);
		this.selectedLabels = new String[2];

		/* Add the view as observer to the model */
		((TVModel) model).addObserver(tvFrame);

		tvFrame.addObserver(this);

		addViewListeners();
		addMenuListeners();
		addKeyBindings();
	}

	/**
	 * Removes all children nodes of the 'File'node. This has the effect that
	 * all preferences stored specific to different loaded data sets are reset.
	 */
	public void resetPreferences() {

		try {
			int option = (new CustomDetailsConfirmDialog(
				(JFrame) JFrame.getFrames()[0],"Reset Preferences?",
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
				"values remain intact.","Reset")).getSelection();

			switch (option) {

			case JOptionPane.YES_OPTION:
				tvFrame.getConfigNode().parent().removeNode();
				tvFrame.getAppFrame().dispose();
				System.exit(0);
				break;

			case JOptionPane.NO_OPTION:
				return;

			default:
				return;
			}

		} catch (final BackingStoreException e) {
			final String message = "Issue while resetting preferences.";
			JOptionPane.showMessageDialog(Frame.getFrames()[0], message,
					"Error", JOptionPane.ERROR_MESSAGE);
			LogBuffer.println(e.getMessage());
			return;
		}
	}

	/**
	 * Adds keyboard mapping to general TreeViewFrame. The keyboard mapping here
	 * is general application stuff, as opposed to GlobalView specific etc.
	 */
	private void addKeyBindings() {

		final InputMap input_map = tvFrame.getInputMap();
		final ActionMap action_map = tvFrame.getActionMap();

		/* Gets the system's modifier key (Ctrl or Cmd) */
		final int modifier = Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask();

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, modifier),
				"openCluster");
		action_map.put("openCluster", new ClusterAction());
	}

	/* ------------ Add listeners -------------------------------- */
	/**
	 * Adds listeners to views that are instantiated in TVFrame.
	 */
	private void addViewListeners() {

		if (tvFrame.getWelcomeView() != null) {
			tvFrame.getWelcomeView().addLoadListener(new LoadButtonListener());
			tvFrame.getWelcomeView().addLoadLastListener(
					new LoadLastButtonListener());
		}

		if (tvFrame.getLoadErrorView() != null) {
			tvFrame.getLoadErrorView().addLoadNewListener(
					new LoadButtonListener());
		}
	}

	public void toggleTrees() {

		dendroController.toggleTrees();
	}

	/**
	 * Generates the menubar controller. Causes listeners to be added for the
	 * main menubar as well as the listed file names in 'Recent Files'. file
	 * names in .
	 */
	private void addMenuListeners() {

		menuController = new MenubarController(tvFrame, TVController.this);

		tvFrame.addMenuActionListeners(new StackMenuListener());
		tvFrame.addFileMenuListeners(new FileMenuListener());
	}

	/**
	 * Calls a sequence of methods related to loading a file, when its button is
	 * clicked.
	 *
	 * @author CKeil
	 */
	private class LoadButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			openFile(null);
		}
	}

	/**
	 * Initiates loading of last used file.
	 *
	 * @author CKeil
	 */
	private class LoadLastButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			final FileSet last = tvFrame.getFileMRU().getLast();
			openFile(last);
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
	/**
	 * Adds the listeners to all JMenuItems in the main menubar.
	 *
	 * @author CKeil
	 *
	 */
	private class StackMenuListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			if (e.getSource() instanceof JMenuItem) {

				final String title = ((JMenuItem) e.getSource()).getText();
				menuController.execute(title);
			}
		}
	}

	/**
	 * Load data into the model.
	 * 
	 * @param fileSet
	 *            The fileSet to be loaded.
	 * @param isClusterFile
	 *            Whether a clustered file is loaded or not. This is important
	 *            when figuring out whether preferences from the previous file
	 *            should be copied to the new one. It should only occur when a
	 *            file is being clustered.
	 */
	public void loadData(final FileSet fileSet, final boolean isClusterFile,
			final DataLoadInfo dataInfo) {

		/* Setting loading screen */
		tvFrame.generateView(TreeViewFrame.PROGRESS_VIEW);

		/* Loading TVModel */
		final TVModel tvModel = (TVModel) model;

		if (isClusterFile && clusterNodeSourceKeys != null) {
			String srcName = clusterNodeSourceKeys[0];
			String srcExtension = clusterNodeSourceKeys[1];
			this.oldNode = getOldPreferences(srcName, srcExtension);

		} else {
			this.oldNode = null;
		}

		fileMenuSet = (fileSet != null) ? fileSet : tvFrame.getFileSet(file);

		try {
			if (dendroController.hasDendroView()) {
				final String[] geneNames = tvModel.getRowHeaderInfo()
						.getNames();
				final String[] arrayNames = tvModel.getColumnHeaderInfo()
						.getNames();

				storeSelectedLabels(geneNames, arrayNames);
			}

			/* ensure reset of the model data */
			tvModel.resetState();
			tvModel.setSource(fileMenuSet);

			if (tvModel.getColumnHeaderInfo().getNumHeaders() == 0) {
				/* ------ Load Process -------- */
				final ModelLoader loader = new ModelLoader(tvModel, this,
						dataInfo);
				loader.execute();

			} else {
				LogBuffer.println("ArrayHeaders not reset, aborted loading.");
			}

		} catch (final OutOfMemoryError e) {
			final String oomError = "The data file is too large. "
					+ "Increase the JVM's heap size. Error: " + e.getMessage();
			tvFrame.setLoadErrorMessage(oomError);
		}
	}

	/**
	 * Finish up loading by setting the model loaded status to true and creating
	 * a new DendroView.
	 */
	public void finishLoading() {

		if (model.getDataMatrix().getNumRow() > 0) {

			tvFrame.setTitleString(model.getSource());

			/* Will notify view of successful loading. */
			((TVModel) model).setLoaded(true);

			setDataModel();

			dendroController.setNewMatrix(tvFrame.getDendroView(), model);

			/*
			 * TODO Needs to happen after setNewMatrix because a new
			 * ColorExtractor object is created, which would void the updated
			 * ColorExtractor state if copying happens before. Implement a nicer
			 * solution one day...
			 */
			Preferences loadedNode = getOldPreferences(fileMenuSet.getRoot(),
					fileMenuSet.getExt());
			copyOldPreferencesTo(loadedNode);

			if (fileMenuSet != null) {
				fileMenuSet = tvFrame.getFileMRU().addUnique(fileMenuSet);
				tvFrame.getFileMRU().setLast(fileMenuSet);
				fileMenuSet = null;

			} else {
				LogBuffer.println("FileSet is null.");
			}

			/* set the selected label type to the old one */
			resetLabelSelection();

			LogBuffer.println("Successfully loaded: " + model.getSource());

		} else {
			final String message = "No data matrix could be set.";
			JOptionPane.showMessageDialog(Frame.getFrames()[0], message,
					"Alert", JOptionPane.WARNING_MESSAGE);
			LogBuffer.println("Alert: " + message);

			tvFrame.setLoadErrorMessage("Data was not loaded.");

			/* Set model status, which will update the view. */
			((TVModel) model).setLoaded(false);
		}

		addViewListeners();
		addMenuListeners();
	}

	private Preferences getOldPreferences(String srcName, String srcExtension) {

		try {
			/* First, get the relevant old node... */
			Preferences root;
			if (getConfigNode().nodeExists("File")) {
				root = getConfigNode().node("File");
				
			} else {
				LogBuffer.println("File node not found. Could not"
						+ " copy data.");
				return null;
			}

			return getTargetNode(root, srcName, srcExtension);

		} catch (BackingStoreException e) {
			LogBuffer.logException(e);
			return null;
		}
	}

	/**
	 * Copies label and color data from the pre-clustered file to the
	 * Preferences node of the post-clustered file.
	 * 
	 * @param srcName
	 *            Source name of the post-clustered file.
	 * @param srcExtension
	 *            Source file extension of the post-clustered file.
	 */
	private void copyOldPreferencesTo(final Preferences loadedNode) {

		if (oldNode == null) {
			LogBuffer.println("No old node was found when trying to copy old"
					+ " preferences.");
			return;
		}
		
		LogBuffer.println("Old node: " + oldNode.name());

		try {
			dendroController.importLabelPreferences(oldNode);

			if (oldNode.nodeExists("ColorPresets")) {
				dendroController.importColorPreferences(oldNode);

				// set node here? maybe it disappears because it's a local var
			} else {
				LogBuffer.println("ColorPresets node not found when trying" 
						+ " to import previous color settings.");
			}

		} catch (BackingStoreException e) {
			LogBuffer.logException(e);
		}

		oldNode = null;
	}

	/**
	 * Finds a specific sub-node in a root node.
	 * 
	 * @param root
	 * @param srcName
	 * @param srcExtension
	 * @return The target Preferences node.
	 * @throws BackingStoreException
	 */
	private static Preferences getTargetNode(Preferences root, String srcName,
			String srcExtension) throws BackingStoreException {

		String[] fileNodes = root.childrenNames();

		Preferences targetNode = null;
		for (String nodeName : fileNodes) {
			Preferences node = root.node(nodeName);
			String name = node.get("name", "none");
			String extension = node.get("extension", ".txt");

			if (name.equalsIgnoreCase(srcName)
					&& extension.equalsIgnoreCase(srcExtension)) {
				targetNode = node;
				break;
			}
		}

		/* Interrupt if no node is found to copy old data. */
		if (targetNode == null) {
			LogBuffer.println("Target node not found. Could not"
					+ " copy data.");
			return null;
		}

		return targetNode;
	}

	/**
	 * Store the currently selected labels.
	 *
	 * @param geneNames
	 * @param arrayNames
	 */
	private void storeSelectedLabels(final String[] geneNames,
			final String[] arrayNames) {

		/* save label type selection before loading new file */
		final int geneIncluded = dendroController.getRowIncluded()[0];
		final int arrayIncluded = dendroController.getColumnIncluded()[0];

		if (geneNames.length > 0 && arrayNames.length > 0) {
			selectedLabels[0] = geneNames[geneIncluded];
			selectedLabels[1] = arrayNames[arrayIncluded];
		}
	}

	/**
	 * TODO move this to DendroController! Use the selected labels that have
	 * been stored and select them again in the model. This should happen right
	 * after the model has been loaded and the new DendroView was set up.
	 */
	private void resetLabelSelection() {

		final int[] newGSelected = new int[] { 0 };
		final String[] geneNames = model.getRowHeaderInfo().getNames();
		for (int i = 0; i < geneNames.length; i++) {
			if (geneNames[i].equalsIgnoreCase(selectedLabels[0])) {
				newGSelected[0] = i;
				break;
			}
		}

		final int[] newASelected = new int[] { 0 };
		final String[] arrayNames = model.getColumnHeaderInfo().getNames();
		for (int i = 0; i < arrayNames.length; i++) {
			if (arrayNames[i].equalsIgnoreCase(selectedLabels[1])) {
				newASelected[0] = i;
				break;
			}
		}

		dendroController.setNewIncluded(newGSelected, newASelected);
	}

	/**
	 * This method opens a file dialog to open either the visualization view or
	 * the cluster view depending on which file type is chosen.
	 *
	 * @throws LoadException
	 */
	public void openFile(FileSet fileSet) {

		String message;
		try {
			if(fileSet == null) {
				file = tvFrame.selectFile();
	
				/* Only run loader, if JFileChooser wasn't canceled. */
				if (file != null) {
					fileSet = tvFrame.getFileSet(file);
	
				} else {
					return;
				}
			}
			
			getDataInfoAndLoad(fileSet, false);
			
		} catch (final LoadException e) {
			message = "Loading the file was interrupted.";
			showWarning(message);
			LogBuffer.logException(e);
			return;
		}
	}

	/**
	 * Used to transfer information to ModelLoader about the data 
	 * for proper loading. 
	 * Either through saved information (stored preferences) or by offering
	 * a dialog to the user in which they can specify parameters.
	 * @param fileSet File name + directory information object.
	 * @param isFromCluster Whether the loading happens as a result of 
	 * clustering.
	 */
	public void getDataInfoAndLoad(FileSet fileSet, boolean isFromCluster) {

		/* To check if file was loaded before */
		Preferences node = getOldPreferences(fileSet.getRoot(),
				fileSet.getExt());

		DataLoadInfo dataInfo;
		if ((FileSet.TRV).equalsIgnoreCase(fileSet.getExt()) && node != null) {
			dataInfo = getDataLoadInfo(fileSet);
			
		} else {
			dataInfo = useImportDialog(fileSet);
		}

		if (dataInfo != null) {
			loadData(fileSet, isFromCluster, dataInfo);
			
		} else {
			String message = "Data loading was interrupted.";
			LogBuffer.println(message);
		}
	}

	/**
	 * Show a dialog for the user to specify how his data should be loaded.
	 * Retrieves the user chosen options and returns them in a DataInfo object.
	 * 
	 * @param filename
	 * @return Options/ parameters for data loading.
	 */
	private static DataLoadInfo useImportDialog(final FileSet fileSet) {

		DataImportDialog loadPreview = new DataImportDialog(fileSet.getRoot()
				+ fileSet.getExt());

		DataImportController importController = new DataImportController(
				loadPreview);

		String[][] previewData;
		importController.setFileSet(fileSet);
		previewData = importController.loadPreviewData();

		loadPreview.setNewTable(previewData);
		importController.initDialog();
		
		/* Auto run before showing dialog */
		importController.detectDataBoundaries();

		DataLoadInfo dataInfo = loadPreview.showDialog();

		return dataInfo;
	}

	/**
	 * Load stored info for a specific file.
	 * @param fileSet The FileSet for the file to be loaded.
	 * @return A DataLoadInfo object which contains information relevant for
	 * setting up the DataLoadDialog.
	 */
	public DataLoadInfo getDataLoadInfo(FileSet fileSet) {

		Preferences node = getOldPreferences(fileSet.getRoot(),
				fileSet.getExt());

		String delimiter = node.get("delimiter", ModelLoader.DEFAULT_DELIM);
		int[] dataCoords = new int[] { node.getInt("rowCoord", 0),
				node.getInt("colCoord", 0) };

		return new DataLoadInfo(dataCoords, delimiter);
	}

	/**
	 * Allows user to load a file from a URL
	 *
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
		final String urlString = JOptionPane.showInputDialog(this,
				"Enter a Url");

		if (urlString != null) {
			// must parse out name, parent + sep...
			final int postfix = urlString.lastIndexOf("/") + 1;
			final String name = urlString.substring(postfix);
			final String parent = urlString.substring(0, postfix);
			fileSet1 = new FileSet(name, parent);

		} else
			throw new LoadException("Input Dialog closed without selection...",
					LoadException.NOFILE);

		return fileSet1;
	}

	/**
	 * Sets up the following: 1) urlExtractor, an object that generates urls
	 * from gene indexes 2) arrayUrlExtractor, similarly 3) geneSelection and 4)
	 * arraySelection, the two selection objects. It is important that these are
	 * set up before any plugins are instantiated. This is called before
	 * setupRunning by setDataModel.
	 */
	protected void setupExtractors() {

		final DataMatrix matrix = model.getDataMatrix();
		final int ngene = matrix.getNumRow();
		final int nexpr = matrix.getNumCol();

		final Preferences documentConfig = model.getDocumentConfigRoot();

		// extractors...
		final UrlPresets genePresets = tvFrame.getGeneUrlPresets();
		final UrlExtractor urlExtractor = new UrlExtractor(
				model.getRowHeaderInfo(), genePresets);

		urlExtractor.bindConfig(documentConfig.node("UrlExtractor"));
		tvFrame.setUrlExtractor(urlExtractor);

		final UrlPresets arrayPresets = tvFrame.getArrayUrlPresets();
		final UrlExtractor arrayUrlExtractor = new UrlExtractor(
				model.getColumnHeaderInfo(), arrayPresets);

		arrayUrlExtractor.bindConfig(documentConfig.node("ArrayUrlExtractor"));
		tvFrame.setArrayUrlExtractor(arrayUrlExtractor);

		LogBuffer.println("Set new selection objects.");
		tvFrame.setGeneSelection(new TreeSelection(ngene));
		tvFrame.setArraySelection(new TreeSelection(nexpr));
	}

	/**
	 * Opens the ClusterViewFrame with either the options for hierarchical
	 * clustering or K-Means, depending on the boolean parameter.
	 *
	 * @param hierarchical
	 */
	public void setupClusterView(final int clusterType) {

		/*
		 * To quickly find Preferences node of pre-cluster file to carry over
		 * settings like color and font.
		 */
		FileSet fs = ((TVModel) model).getFileSet();
		this.clusterNodeSourceKeys = new String[] { fs.getRoot(), fs.getExt() };

		/* Erase selection */
		dendroController.deselectAll();

		/* Making a new dialog to display cluster UI */
		final ClusterDialog clusterView = new ClusterDialog(clusterType);

		/* Creating the Controller for this view. */
		ClusterDialogController cController = new ClusterDialogController(clusterView,
				TVController.this);

		cController.displayView();
	}

	/**
	 * Sets extractors and FileSetListeners.
	 */
	public void setDataModel() {

		if (model != null) {
			model.clearFileSetListeners();
			model.addFileSetListener(tvFrame);

			setupExtractors();
		}
	}

	public void showSubDataModel(final int[] geneIndexes,
			final int[] arrayIndexes, final String source, final String name) {

		final ReorderedDataModel dataModel = new ReorderedDataModel(model,
				geneIndexes, arrayIndexes);
		if (source != null) {
			dataModel.setSource(source);
		}

		if (name != null) {
			dataModel.setName(name);
		}

		// final ViewFrame window = getApp().openNew();
		// window.setDataModel(dataModel);
		// window.setLoaded(true);
		// window.getAppFrame().setVisible(true);
		// tvFrame.setDataModel(dataModel);
		// setViewChoice(false);
	}

	/**
	 * Opens an instance of GeneListMaker used to save a list of genes.
	 */
	public void saveList() {

		if (warnSelectionEmpty()) {
			final FileSet source = model.getFileSet();
			String def = model.getName() + "_list.txt";

			if (source != null) {

				def = source.getDir() + source.getRoot() + "_list.txt";
			}

			final GeneListMaker t = new GeneListMaker(
					(JFrame) Frame.getFrames()[0], tvFrame.getRowSelection(),
					model.getRowHeaderInfo(), def);

			t.setDataMatrix(model.getDataMatrix(), model.getColumnHeaderInfo(),
					DataModel.NAN);

			t.setConfigNode(tvFrame.getConfigNode());

			t.pack();
			t.setVisible(true);
		}
	}

	/**
	 * Opens instance of GeneListMaker to save data.
	 */
	public void saveData() {

		if (warnSelectionEmpty()) {
			final FileSet source = model.getFileSet();
			final GeneListMaker t = new GeneListMaker(
					(JFrame) Frame.getFrames()[0], tvFrame.getRowSelection(),
					model.getRowHeaderInfo(), source.getDir()
							+ source.getRoot() + "_data.cdt");

			t.setDataMatrix(model.getDataMatrix(), model.getColumnHeaderInfo(),
					DataModel.NAN);

			t.setConfigNode(tvFrame.getConfigNode());

			t.includeAll();
			t.pack();
			t.setVisible(true);
		}
	}

	/**
	 * Generates a warning message if TreeSelectionI object is null and returns
	 * false in that case.
	 *
	 * @return boolean
	 */
	public boolean warnSelectionEmpty() {

		final TreeSelectionI treeSelection = tvFrame.getRowSelection();

		if ((treeSelection == null)
				|| (treeSelection.getNSelectedIndexes() <= 0)) {

			JOptionPane.showMessageDialog(Frame.getFrames()[0],
					"Cannot generate gene list, no gene selected");
			return false;
		}
		return true;
	}

	/**
	 * Saves the current model, GUI handled by TVFrame.
	 *
	 * @param incremental
	 * @return
	 */
	public boolean doModelSave(final boolean incremental) {

		final DataModelWriter writer = new DataModelWriter(model);
		final Set<DataModelFileType> written;

		if (incremental) {
			written = writer.writeIncremental(model.getFileSet());

		} else {
			written = writer.writeAll(model.getFileSet());
		}

		if (written.isEmpty()) {
			tvFrame.openSaveDialog(written.isEmpty(), null);
			return false;
		}

		String msg = "Model changes were written to ";
		int i = 0;

		for (final DataModelFileType type : written) {
			msg += type.name();
			i++;

			if (i == written.size()) {
				// nothing after last one.

			} else if (i + 1 == written.size()) {
				msg += " and ";

			} else {
				msg += ",";
			}
		}

		tvFrame.openSaveDialog(written.isEmpty(), msg);
		return true;

	}

	/**
	 * Saves the model as a user specified file.
	 */
	public void saveModelAs() {

		if (model.getFileSet() == null) {
			JOptionPane.showMessageDialog(Frame.getFrames()[0],
					"Saving of datamodels not backed by "
							+ "files is not yet supported.");

		} else {
			final JFileChooser fileDialog = new JFileChooser();
			final CdtFilter ff = new CdtFilter();
			fileDialog.setFileFilter(ff);

			final String string = model.getFileSet().getDir();

			if (string != null) {

				fileDialog.setCurrentDirectory(new File(string));
			}

			final int retVal = fileDialog.showSaveDialog(Frame.getFrames()[0]);

			if (retVal == JFileChooser.APPROVE_OPTION) {
				final File chosen = fileDialog.getSelectedFile();
				String name = chosen.getName();

				if (!name.toLowerCase().endsWith(".cdt")
						&& !name.toLowerCase().endsWith(".pcl")) {
					name += ".cdt";
				}

				FileSet fileSet2 = new FileSet(name, chosen.getParent()
						+ File.separator);
				fileSet2.copyState(model.getFileSet());

				final FileSet fileSet1 = new FileSet(name, chosen.getParent()
						+ File.separator);
				fileSet1.setName(model.getFileSet().getName());

				model.getFileSet().copyState(fileSet1);
				doModelSave(false);

				model.getFileSet().notifyMoved();
				tvFrame.getFileMRU().removeDuplicates(model.getFileSet());
				fileSet2 = tvFrame.getFileMRU().addUnique(fileSet2);
				tvFrame.getFileMRU().setLast(model.getFileSet());
				tvFrame.addFileMenuListeners(new FileMenuListener());

				if (model instanceof TVModel) {
					((TVModel) model).getDocumentConfig().put("jtv",
							model.getFileSet().getJtv());
				}
			}
		}
	}

	/**
	 * This class is an ActionListener which overrides the run() function.
	 */
	private class FileMenuListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent actionEvent) {

			tvFrame.getFileMRU().setLast(tvFrame.getFileMenuSet());
			tvFrame.getFileMRU().notifyObservers();

			fileMenuSet = tvFrame.findFileSet((JMenuItem) actionEvent
					.getSource());// tvFrame.getFileMenuSet();

			openFile(fileMenuSet);
		}
	}

	/**
	 * Opens the preferences menu and sets the displayed menu to the specified
	 * option using a string as identification.
	 *
	 * @param menu
	 */
	public void openPrefMenu(final String menu) {

		// View
		final LabelSettings preferences = new LabelSettings(tvFrame);

		if (menu.equalsIgnoreCase(StringRes.menu_RowAndCol)) {
			preferences.setHeaderInfo(model.getRowHeaderInfo(),
					model.getColumnHeaderInfo());
		}

		preferences.setConfigNode(tvFrame.getConfigNode().node(
				StringRes.pnode_Preferences));
		preferences.setMenu(menu);

		// Controller
		new PreferencesController(tvFrame, model, preferences);

		preferences.setVisible(true);
	}
	
	/**
	 * Opens the preferences menu and sets the displayed menu to the specified
	 * option using a string as identification.
	 *
	 * @param menu
	 */
	public void openExportMenu() {

		if(tvFrame.getDendroView() == null) {
			LogBuffer.println("DendroView is not instantiated. "
					+ "Nothing to export.");
			return;
		}
		
		/* Set up tree images */
		ExportPreviewTrees expRowTrees = getTreeSnapshot(
				tvFrame.getDendroView().getRowTreeView(), true);
		ExportPreviewTrees expColTrees = getTreeSnapshot(
				tvFrame.getDendroView().getColumnTreeView(), false);
		
		/* Set up matrix image */
		BufferedImage matrix = tvFrame.getDendroView()
				.getInteractiveMatrixView().getVisibleImage();
		ExportPreviewMatrix expMatrix = new ExportPreviewMatrix(matrix);

		ExportHandler eh = new ExportHandler(tvFrame.getDendroView(),
			dendroController.getInteractiveXMap(),
			dendroController.getInteractiveYMap(),tvFrame.getColSelection(),
			tvFrame.getRowSelection());
		boolean selectionsExist = (tvFrame.getColSelection() != null &&
			tvFrame.getColSelection().getNSelectedIndexes() > 0);
		ExportDialog exportDialog = new ExportDialog(selectionsExist,eh);
		exportDialog.setPreview(expRowTrees, expColTrees, expMatrix);
		
		new ExportDialogController(exportDialog,tvFrame,
			dendroController.getInteractiveXMap(),
			dendroController.getInteractiveYMap(),model);
		
		exportDialog.setVisible(true);
	}
	
	private ExportPreviewTrees getTreeSnapshot(TRView treeAxisView, 
			final boolean isRows) {
		
		int width;
		int height;
		if(isRows) {
			width = ExportPreviewTrees.HEIGHT;
			height = ExportPreviewTrees.WIDTH;
		} else {
			width = ExportPreviewTrees.WIDTH;
			height = ExportPreviewTrees.HEIGHT;
		}
		
		/* Set up column tree image */
		BufferedImage treeSnapshot = null;
		ExportPreviewTrees expTrees = null;
		if(treeAxisView.isEnabled()) {
			treeSnapshot = treeAxisView.getSnapshot(width, height);
			expTrees = new ExportPreviewTrees(treeSnapshot, isRows);
		}
		
		return expTrees;
	}

	/*
	 * TODO implement this and others to deprecate PreferencesMenu, which is a
	 * remnant of a unified menu system (as opposed to separate dialogs)
	 */
	public void openLabelMenu() {

	}

	/**
	 * Opens up the color chooser dialog.
	 */
	public void openColorMenu() {

		// --> move to dendroController
		final double min = model.getDataMatrix().getMinVal();
		final double max = model.getDataMatrix().getMaxVal();
		final double mean = model.getDataMatrix().getMean();
		final double median = model.getDataMatrix().getMedian();

		/* View */
		ColorExtractor colorExtractor = dendroController.getColorExtractor();

		final ColorChooserUI gradientPick = new ColorChooserUI(colorExtractor, 
				min, max, mean, median);

		/* Controller */
		ColorChooserController controller = new ColorChooserController(
				gradientPick);

		/* Adding GradientColorChooser configurations to DendroView node. */
		controller.setConfigNode(((TVModel) model).getDocumentConfig());
		
		controller.addObserver(dendroController.getInteractiveMatrixView());
		controller.addObserver(dendroController.getGlobalMatrixView());

		gradientPick.setVisible(true);
	}
	
	private void showWarning(final String message) {

		JOptionPane.showMessageDialog(tvFrame.getAppFrame(), 
				message, "Warning", JOptionPane.WARNING_MESSAGE);
		LogBuffer.println(message);
	}

	/*
	 * Gets the main view's config node.
	 */
	public Preferences getConfigNode() {

		return tvFrame.getConfigNode();
	}

	/**
	 * Returns TVController's model.
	 *
	 * @return
	 */
	public DataModel getDataModel() {

		return model;
	}

	@Override
	public void update(final Observable o, final Object arg) {

		LogBuffer.println("Updating TVController");
		/* when tvFrame rebuilds its menu */
		if (o instanceof ViewFrame) {
			addMenuListeners();
		}

	}
	
	/**
	 * Relays copy call to dendroController.
	 * @param copyType
	 * @param isRows
	 */
	public void copyLabels(final CopyType copyType, final boolean isRows) {
		
		dendroController.copyLabels(copyType, isRows);
	}
}
