package Controllers;

import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
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

import ColorChooser.ColorChooser;
import ColorChooser.ColorChooserController;
import Utilities.StringRes;
import Views.ClusterDialog;
import Views.ClusterView;
import edu.stanford.genetics.treeview.CdtFilter;
import edu.stanford.genetics.treeview.DataMatrix;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.DataModelFileType;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.GeneListMaker;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.PreferencesMenu;
import edu.stanford.genetics.treeview.TreeSelection;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.UrlExtractor;
import edu.stanford.genetics.treeview.UrlPresets;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.model.DataModelWriter;
import edu.stanford.genetics.treeview.model.ModelLoader;
import edu.stanford.genetics.treeview.model.ReorderedDataModel;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.plugin.dendroview.DoubleArrayDrawer;

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
	private final String[] selectedLabels;

	public TVController(final TreeViewFrame tvFrame, final DataModel model) {

		this.model = model;
		this.tvFrame = tvFrame;
		this.dendroController = new DendroController(tvFrame);
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
			final int option = JOptionPane.showConfirmDialog(
					Frame.getFrames()[0],
					"Are you sure you want to reset preferences and "
							+ "close TreeView?", "Reset Preferences?",
							JOptionPane.YES_NO_OPTION);

			switch (option) {

			case JOptionPane.YES_OPTION:
				tvFrame.saveSettings();
				tvFrame.getConfigNode().parent().removeNode();
				tvFrame.getAppFrame().dispose();
				break;

			case JOptionPane.NO_OPTION:
				break;

			default:
				return;
			}

		} catch (final BackingStoreException e) {
			final String message = "Issue while resetting preferences.";
			JOptionPane.showMessageDialog(Frame.getFrames()[0], message,
					"Error", JOptionPane.ERROR_MESSAGE);
			LogBuffer.println(e.getMessage());
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

	// public void setSearchVisible() {
	//
	// /* Don't do anything if search panel is visible (closing only via X) */
	// if (!tvFrame.getDendroView().isSearchVisible()) {
	// dendroController.setSearchVisible(true);
	// }
	// }

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

			openFile();
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

			/* Notify user if no previous file can be found. */
			if (last == null) {
				tvFrame.getWelcomeView().setWarning();

			} else {
				loadData(last);
			}
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
	 * Passes the resize call for the matrix to the DendroController.
	 *
	 * @param mode
	 */
	protected void setMatrixSize(final int mode) {

		dendroController.setMatrixSize(mode);
	}

	public void loadData(final FileSet fileSet) {

		fileMenuSet = (fileSet != null) ? fileSet : tvFrame.getFileSet(file);

		/* Setting loading screen */
		tvFrame.generateView(TreeViewFrame.PROGRESS_VIEW);

		/* Loading TVModel */
		final TVModel tvModel = (TVModel) model;

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
				final ModelLoader loader = new ModelLoader(tvModel, this);
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

			if (fileMenuSet != null) {
				fileMenuSet = tvFrame.getFileMRU().addUnique(fileMenuSet);
				tvFrame.getFileMRU().setLast(fileMenuSet);
				fileMenuSet = null;

			} else {
				LogBuffer.println("FileSet is null.");
			}

			tvFrame.setTitleString(model.getSource());

			/* Will notify view of successful loading. */
			((TVModel) model).setLoaded(true);

			setDataModel();

			dendroController.setNew(tvFrame.getDendroView(), model);

			/* set the selected label type to the old one */
			resetLabelSelection();

			LogBuffer.println("Successfully loaded: " + model.getSource());

		} else {
			final String message = "No data matrix could be set.";
			JOptionPane.showMessageDialog(Frame.getFrames()[0], message,
					"Alert", JOptionPane.WARNING_MESSAGE);
			LogBuffer.println("Alert: " + message);

			tvFrame.setLoadErrorMessage("Data in file unusable.");

			/* Set model status, which will update the view. */
			((TVModel) model).setLoaded(false);
		}

		addViewListeners();
		addMenuListeners();
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
		final int geneIncluded = dendroController.getGeneIncluded()[0];
		final int arrayIncluded = dendroController.getArrayIncluded()[0];

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
	public void openFile() {

		try {
			file = tvFrame.selectFile();

			/* Only run loader, if JFileChooser wasn't canceled. */
			if (file != null) {
				loadData(tvFrame.getFileSet(file));

			} else {
				LogBuffer.println("Selected file was null. Cannot begin"
						+ " loading data.");
			}
		} catch (final LoadException e) {
			LogBuffer.println("Loading the FileSet was interrupted.");
			LogBuffer.logException(e);
		}
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

		/* Erase selection */
		dendroController.deselectAll();

		/* Making a new Window to display clustering components */
		final ClusterDialog clusterView = new ClusterDialog(clusterType);

		/* Creating the Controller for this view. */
		new ClusterController(clusterView, TVController.this);

		clusterView.setVisible(true);
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
					DataModel.NODATA);

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
					DataModel.NODATA);

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

		} else {
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

			tvFrame.generateView(TreeViewFrame.PROGRESS_VIEW);
			loadData(fileMenuSet);
			// new LoadWorker().execute();

			// } catch (final LoadException e) {
			// if (e.getType() == LoadException.INTPARSE) {
			//
			// } else {
			// final int result = FileMruEditor.offerSearch(
			// tvFrame.getFileMenuSet(),
			// tvFrame, "Could not Load "
			// + tvFrame.getFileMenuSet().getCdt());
			//
			// if (result == FileMruEditor.FIND) {
			// tvFrame.getFileMRU().notifyFileSetModified();
			// tvFrame.getFileMRU().notifyObservers();
			//
			// actionPerformed(actionEvent); // REPROCESS...
			// return; // EARLY RETURN
			//
			// } else if (result == FileMruEditor.REMOVE) {
			// tvFrame.getFileMRU().removeFileSet(
			// tvFrame.getFileMenuSet());
			// tvFrame.getFileMRU().notifyObservers();
			// }
			// }
			// tvFrame.setLoaded(false);
			// }
			// dataModel.notifyObservers();
			// }
			// };
			// SwingUtilities.invokeLater(update);
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
		final PreferencesMenu preferences = new PreferencesMenu(tvFrame);

		if (menu.equalsIgnoreCase(StringRes.menu_Color)) {

			final Double min = model.getDataMatrix().getMinVal();
			final Double max = model.getDataMatrix().getMaxVal();

			/* View */
			final ColorChooser gradientPick = new ColorChooser(
					((DoubleArrayDrawer) dendroController.getArrayDrawer())
					.getColorExtractor(),
					min, max);

			/*
			 * Adding GradientColorChooser configurations to DendroView node.
			 */
			gradientPick.setConfigNode(((TVModel) model).getDocumentConfig());

			/* Controller */
			new ColorChooserController(gradientPick);

			preferences.setGradientChooser(gradientPick);

		}

		if (menu.equalsIgnoreCase(StringRes.menu_RowAndCol)) {
			preferences.setHeaderInfo(model.getRowHeaderInfo(),
					model.getColumnHeaderInfo());
		}

		preferences.setupLayout(menu);

		preferences.setConfigNode(tvFrame.getConfigNode().node(
				StringRes.pnode_Preferences));

		// Controller
		new PreferencesController(tvFrame, model, preferences);

		preferences.setVisible(true);
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
}
