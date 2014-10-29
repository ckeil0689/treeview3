package Controllers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import Utilities.StringRes;
import Views.ClusterDialog;
import ColorChooser.ColorChooser;
import ColorChooser.ColorChooserController;
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
import edu.stanford.genetics.treeview.model.DataModelWriter;
import edu.stanford.genetics.treeview.model.ReorderedDataModel;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.plugin.dendroview.DoubleArrayDrawer;

/**
 * This class controls user interaction with TVFrame and its views.
 * 
 * @author CKeil
 * 
 */
public class TVController {

	private final DataModel model;
	private final TreeViewFrame tvFrame;
	private final JFrame applicationFrame;
	private final DendroController dendroController;
	private MenubarController menuActions;

	private File file;
	private FileSet fileMenuSet;

	public TVController(final TreeViewFrame tvFrame, 
			final DataModel model) {

		this.model = model;
		this.tvFrame = tvFrame;
		this.applicationFrame = tvFrame.getAppFrame();
		
		dendroController = new DendroController(tvFrame);
		
		setViewChoice(false);
		addViewListeners();
	}
	
	/**
	 * Removes all children nodes of the 'File'node. This has the effect that
	 * all preferences stored specific to different loaded data sets are reset.
	 */
	public void resetPreferences() {
		
		try {
			final int option = JOptionPane.showConfirmDialog(applicationFrame,
					"Are you sure you want to reset preferences?", 
					"Reset Preferences?", JOptionPane.YES_NO_OPTION);

			switch (option) {

				case JOptionPane.YES_OPTION:	
					tvFrame.getConfigNode().node("File").removeNode();
					break;
												
				case JOptionPane.NO_OPTION:		
					break;
				
				default:						
					return;
			}
			
		} catch (BackingStoreException e) {
			String message = "Issue while resetting preferences.";
			JOptionPane.showMessageDialog(applicationFrame, message, 
					"Error", JOptionPane.ERROR_MESSAGE);
			LogBuffer.println(e.getMessage());
		}
	}

	/**
	 * Adds listeners to views that are instantiated in TVFrame.
	 */
	public void addViewListeners() {

		if (tvFrame.getWelcomeView() != null) {
			tvFrame.getWelcomeView().addLoadListener(
					new LoadButtonListener());
		}

		if (tvFrame.getLoadErrorView() != null) {
			tvFrame.getLoadErrorView().addLoadNewListener(
					new LoadButtonListener());
		}
	}
	
	/**
	 * Adds listeners to the menubar.
	 */
	private void addMenuListeners() {
		
		menuActions = new MenubarController(tvFrame, TVController.this);

		tvFrame.addMenuActionListeners(new StackMenuListener());
		tvFrame.addFileMenuListeners(new FileMenuListener());
	}

	/**
	 * Handles the new loading of data.
	 * 
	 * @author CKeil
	 */
	private class LoadButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			openFile();
		}
	}

	private class StackMenuListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			final List<JMenuItem> menuList = tvFrame.getStackMenus();

			for (int i = 0; i < menuList.size(); i++) {
				if (e.getSource() == menuList.get(i)) {

					menuActions.execute(menuList.get(i).getText());
				}
			}
		}
	}

	public void setViewChoice(boolean hasError) {

		if (hasError) {
			tvFrame.setRunning(false);
			tvFrame.setLoaded(false);
			tvFrame.setView(StringRes.view_LoadError);

			LogBuffer.println(StringRes.clusterError_notLoaded);

		} else {
			if (model.isLoaded()) {
				LogBuffer.println("Setting DendroView.");
				tvFrame.setRunning(true);
				tvFrame.setLoaded(true);
				dendroController.setNew(tvFrame.getDendroView(), 
						(TVModel) model);
				tvFrame.setView(StringRes.view_Dendro);

			} else {
				LogBuffer.println("Setting WelcomeView.");
				tvFrame.setRunning(false);
				tvFrame.setLoaded(false);
				tvFrame.setView(StringRes.view_Welcome);
			}
		}
		
		addMenuListeners();
	}
	
	/**
	 * Passes the resize call for the matrix to the DendroController.
	 * @param mode
	 */
	public void setMatrixSize(String mode) {
		
		dendroController.setMatrixSize(mode);
	}

	/**
	 * Setting up a worker thread to load the file selected by the user. 
	 * This prevents the GUI from locking up and allows the ProgressBar 
	 * to display progress.
	 */
	private class LoadWorker extends SwingWorker<Void, Void> {

		@Override
		public Void doInBackground() {

			FileSet fileSet = null;

			if (fileMenuSet == null) {
				fileSet = tvFrame.getFileSet(file);

			} else {
				fileSet = fileMenuSet;
			}

			/* Loading TVModel */
			loadFileSet(fileSet);

			if (fileSet != null) {
				fileSet = tvFrame.getFileMRU().addUnique(fileSet);
				tvFrame.getFileMRU().setLast(fileSet);

			} else {
				LogBuffer.println("FileSet is null.");
			}

			return null;
		}

		@Override
		protected void done() {

			boolean hasError = false;
			
			if (model.isLoaded()) {
				fileMenuSet = null;
				setDataModel();

			} else {
				hasError = true;
			}
			
			tvFrame.setTitleString(model.getSource());
			setViewChoice(hasError);
		}
	}

	/**
	 * This method opens a file dialog to open either the visualization view or
	 * the cluster view depending on which file type is chosen.
	 * 
	 * @throws IOException
	 * 
	 * @throws LoadException
	 */
	public void openFile() {

		SwingWorker<Void, Void> worker = new LoadWorker();

		try {
			file = tvFrame.selectFile();

			// Only run loader, if JFileChooser wasn't canceled.
			if (file != null) {
				worker.execute();
			}

		} catch (final LoadException e) {
			LogBuffer.println("Loading the FileSet was interrupted. "
					+ "Cause: " + e.getCause());
			e.printStackTrace();
		}
	}

	/**
	 * Loads a FileSet and calls setLoaded(true) to reset the MainPanel.
	 * 
	 * @param fileSet
	 * @throws LoadException
	 */
	public void load(FileSet fileSet) throws LoadException {

		loadFileSet(fileSet);

		fileSet = tvFrame.getFileMRU().addUnique(fileSet);
		tvFrame.getFileMRU().setLast(fileSet);
		tvFrame.getFileMRU().notifyObservers();
	}

	/**
	 * r * This is the workhorse. It creates a new DataModel of the file, and
	 * then sets the Datamodel. A side effect of setting the datamodel is to
	 * update the running window.
	 */
	public void loadFileSet(final FileSet fileSet) {

		/* Make local TVModel object */
		TVModel tvModel = (TVModel) model;
		tvModel.setFrame(tvFrame);

		tvFrame.setView("LoadProgressView");

		try {
			// load instance variables of TVModel with data
			tvModel.loadNew(fileSet);

		} catch (final OutOfMemoryError e) {
			final String oomError = "The data file is too large. "
					+ "Increase the JVM's heap size. Error: " + e.getMessage();
			tvFrame.setLoadErrorMessage(oomError);
			e.printStackTrace();

		} catch (final LoadException e) {
			if (e.getType() != LoadException.INTPARSE) {
				JOptionPane.showMessageDialog(applicationFrame, e);
			}
		} catch (final InterruptedException e) {
			LogBuffer.println("InterruptedException in "
					+ "loadFileSet(): " + e.getMessage());
			e.printStackTrace();

		} catch (final ExecutionException e) {
			LogBuffer.println("ExecutionException in "
					+ "loadFileSet(): " + e.getMessage());
			e.printStackTrace();
		}
	}

	// Loading Methods
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

		} else {
			throw new LoadException("Input Dialog closed without selection...",
					LoadException.NOFILE);
		}

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
		final UrlExtractor urlExtractor = new UrlExtractor(model
				.getGeneHeaderInfo(), genePresets);
		urlExtractor.bindConfig(documentConfig.node("UrlExtractor"));
		tvFrame.setUrlExtractor(urlExtractor);

		final UrlPresets arrayPresets = tvFrame.getArrayUrlPresets();
		final UrlExtractor arrayUrlExtractor = new UrlExtractor(model
				.getArrayHeaderInfo(), arrayPresets);
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

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				
				/* Making a new Window to display clustering components */
				final ClusterDialog clusterView = 
						new ClusterDialog(clusterType);

				/* Creating the Controller for this view. */
				new ClusterController(clusterView, TVController.this);
				
				clusterView.setVisible(true);
			}
		});
	}

	/**
	 * Setter for dataModel for TVFrame, also sets extractors, running.
	 * 
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

		final ReorderedDataModel dataModel = new ReorderedDataModel(
				model, geneIndexes, arrayIndexes);
		if (source != null) {
			dataModel.setSource(source);
		}

		if (name != null) {
			dataModel.setName(name);
		}

//		final ViewFrame window = getApp().openNew();
//		window.setDataModel(dataModel);
//		window.setLoaded(true);
//		window.getAppFrame().setVisible(true);
//		tvFrame.setDataModel(dataModel);
		setViewChoice(false);
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

			final GeneListMaker t = new GeneListMaker(applicationFrame,
					tvFrame.getGeneSelection(), model.getGeneHeaderInfo(), def);

			t.setDataMatrix(model.getDataMatrix(), model.getArrayHeaderInfo(), 
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

			final GeneListMaker t = new GeneListMaker(applicationFrame,
					tvFrame.getGeneSelection(), model.getGeneHeaderInfo(), 
					source.getDir() + source.getRoot() + "_data.cdt");

			t.setDataMatrix(model.getDataMatrix(), model.getArrayHeaderInfo(), 
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

		final TreeSelectionI treeSelection = tvFrame.getGeneSelection();

		if ((treeSelection == null)
				|| (treeSelection.getNSelectedIndexes() <= 0)) {

			JOptionPane.showMessageDialog(applicationFrame,
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
			JOptionPane.showMessageDialog(applicationFrame,
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

			final int retVal = fileDialog.showSaveDialog(applicationFrame);

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

			fileMenuSet = tvFrame.findFileSet(
					(JMenuItem)actionEvent.getSource());//tvFrame.getFileMenuSet();

			SwingWorker<Void, Void> worker = new LoadWorker();
			tvFrame.setView(StringRes.view_LoadProg); //change
			worker.execute();

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

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				
				// View
				final PreferencesMenu preferences = 
						new PreferencesMenu(tvFrame);
				
				if(menu.equalsIgnoreCase(StringRes.menu_Color)) {
					
					int min = (int)model.getDataMatrix().getMinVal();
					int max = (int)model.getDataMatrix().getMaxVal();
					
					/* View */
					ColorChooser gradientPick = 
							new ColorChooser(((DoubleArrayDrawer) 
									dendroController.getArrayDrawer())
									.getColorExtractor(), min, max);

					/*
					 *  Adding GradientColorChooser configurations to 
					 *  DendroView node.
					 */
					gradientPick.setConfigNode(((TVModel) model)
							.getDocumentConfig());
					
					/* Controller */
					new ColorChooserController(gradientPick);
					
					preferences.setGradientChooser(gradientPick);
					
				} else if(menu.equalsIgnoreCase(StringRes.menu_RowAndCol)) {
					preferences.setHeaderInfo(model.getGeneHeaderInfo(), 
							model.getArrayHeaderInfo());
				} else {
					String message = "A menu like this does not exist.";
					JOptionPane.showMessageDialog(applicationFrame, message, 
							"Warning", JOptionPane.WARNING_MESSAGE);
				}
				
				preferences.setupLayout(menu);
				
				preferences.setConfigNode(tvFrame.getConfigNode().node(
						StringRes.pnode_Preferences));

				// Controller
				new PreferencesController(tvFrame, model, preferences);

				preferences.setVisible(true);
			}
		});
	}

	/**
	 * Returns TVFrameController's model.
	 * 
	 * @return
	 */
	public DataModel getDataModel() {

		return model;
	}
}
