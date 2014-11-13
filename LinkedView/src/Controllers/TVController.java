package Controllers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import ColorChooser.ColorChooser;
import ColorChooser.ColorChooserController;
import Utilities.StringRes;
import Views.ClusterDialog;
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
public class TVController {

	private final DataModel model;
	private final TreeViewFrame tvFrame;
	private final DendroController dendroController;
	private MenubarController menuController;

	private File file;
	private FileSet fileMenuSet;

	public TVController(final TreeViewFrame tvFrame, final DataModel model) {

		this.model = model;
		this.tvFrame = tvFrame;
		this.dendroController = new DendroController(tvFrame);
		
		/* Add the view as observer to the model */
		((TVModel) model).addObserver(tvFrame);
		
		addViewListeners();
	}
	
	/**
	 * Removes all children nodes of the 'File'node. This has the effect that
	 * all preferences stored specific to different loaded data sets are reset.
	 */
	public void resetPreferences() {
		
		try {
			final int option = 
					JOptionPane.showConfirmDialog(JFrame.getFrames()[0],
					"Are you sure you want to reset preferences and "
					+ "close TreeView?", "Reset Preferences?", 
					JOptionPane.YES_NO_OPTION);

			switch (option) {

				case JOptionPane.YES_OPTION:	
					tvFrame.getConfigNode().node("File").removeNode();
					tvFrame.saveSettings();
					tvFrame.getAppFrame().dispose();
					break;
												
				case JOptionPane.NO_OPTION:		
					break;
				
				default:						
					return;
			}
			
		} catch (BackingStoreException e) {
			String message = "Issue while resetting preferences.";
			JOptionPane.showMessageDialog(JFrame.getFrames()[0], message, 
					"Error", JOptionPane.ERROR_MESSAGE);
			LogBuffer.println(e.getMessage());
		}
	}

	/* ------------ Add listeners --------------------------------*/
	/**
	 * Adds listeners to views that are instantiated in TVFrame.
	 */
	private void addViewListeners() {

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
	 * Generates the menubar controller. Causes listeners to be added 
	 * for the main menubar as well as the listed file names in 'Recent Files'.
	 * file names in . 
	 */
	private void addMenuListeners() {
		
		menuController = new MenubarController(tvFrame, TVController.this);

		tvFrame.addMenuActionListeners(new StackMenuListener());
		tvFrame.addFileMenuListeners(new FileMenuListener());
	}

	/**
	 * Calls a sequence of methods related to loading a file, when its
	 * button is clicked.
	 * @author CKeil
	 */
	private class LoadButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			openFile();
		}
	}

	/**
	 * Adds the listeners to all JMenuItems in the main menubar.
	 * @author CKeil
	 *
	 */
	private class StackMenuListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {
			
			if(e.getSource() instanceof JMenuItem) {
				menuController.execute(((JMenuItem)e.getSource()).getText());
			}
		}
	}

	/**
	 * Instructs the main application frame (tvFrame) to set a specific
	 * view panel based on the model's data status or if an error occured 
	 * in the preceding code. 
	 * @param hasError Whether an error occurred in the code preceding the
	 * call of setViewChoice().
	 */
	protected void setViewChoice(boolean hasError) {

		if (hasError) {
//			tvFrame.setRunning(false);
//			tvFrame.setLoaded(false);

			LogBuffer.println(StringRes.clusterError_notLoaded);

		} else {
			if (model.isLoaded()) {
				LogBuffer.println("Setting DendroView.");
				dendroController.setNew(tvFrame.getDendroView(), 
						(TVModel) model);

			} 
//			else {
//				LogBuffer.println("Setting WelcomeView.");
//				tvFrame.setRunning(false);
//				tvFrame.setLoaded(false);
//				tvFrame.setView(StringRes.view_Welcome);
//			}
		}
		
//		addMenuListeners();
	}
	
	/**
	 * Passes the resize call for the matrix to the DendroController.
	 * @param mode
	 */
	protected void setMatrixSize(String mode) {
		
		dendroController.setMatrixSize(mode);
	}
	
	
	public void loadData(FileSet fileSet) {
		
		fileMenuSet = (fileSet != null) ? fileSet : tvFrame.getFileSet(file);
		
		/* Setting loading screen */
		tvFrame.setView("LoadProgressView");

		/* Loading TVModel */
		TVModel tvModel = (TVModel) model;

		try {
			tvModel.resetState();
			tvModel.setSource(fileMenuSet);
			
			if(tvModel.getArrayHeaderInfo().getNumHeaders() == 0) {
			/* ------ Load Process -------- */
			ModelLoader loader = new ModelLoader(tvModel, this);
			loader.execute();
			} else {
				LogBuffer.println("ArrayHeaders not reset, aborted loading.");
			}

		} catch (OutOfMemoryError  e) {
			if(e instanceof OutOfMemoryError) {
				final String oomError = "The data file is too large. "
						+ "Increase the JVM's heap size. Error: " 
						+ e.getMessage();
				tvFrame.setLoadErrorMessage(oomError);
				
			} else {
				JOptionPane.showMessageDialog(JFrame.getFrames()[0], e);
			}
		}
	}
	
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
			
			LogBuffer.println("Setting DendroView.");
			dendroController.setNew(tvFrame.getDendroView(), (TVModel) model);
			
			LogBuffer.println("Successfully loaded: " + model.getSource());

		} else {
			String message = "No data matrix could be set.";
			JOptionPane.showMessageDialog(JFrame.getFrames()[0], 
					message, "Alert", JOptionPane.WARNING_MESSAGE);
			LogBuffer.println("Alert: " + message);
			
			tvFrame.setLoadErrorMessage("Data in file unusable.");
			
			/* Set model status, which will update the view. */
			((TVModel) model).setLoaded(false);
		}
		
		addViewListeners();
		addMenuListeners();
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
			if (file != null) loadData(tvFrame.getFileSet(file));

		} catch (final LoadException e) {
			LogBuffer.println("Loading the FileSet was interrupted. "
					+ "Cause: " + e.getCause());
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

			final GeneListMaker t = 
					new GeneListMaker((JFrame) JFrame.getFrames()[0],
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

			final GeneListMaker t = 
					new GeneListMaker((JFrame) JFrame.getFrames()[0],
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

			JOptionPane.showMessageDialog(JFrame.getFrames()[0],
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
			JOptionPane.showMessageDialog(JFrame.getFrames()[0],
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

			final int retVal = fileDialog.showSaveDialog(JFrame.getFrames()[0]);

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

			tvFrame.setView(StringRes.view_LoadProg); //change
			loadData(fileMenuSet);
//			new LoadWorker().execute();

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
					
					Double min = model.getDataMatrix().getMinVal();
					Double max = model.getDataMatrix().getMaxVal();
					
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
					
				}
				
				if(menu.equalsIgnoreCase(StringRes.menu_RowAndCol)) {
					preferences.setHeaderInfo(model.getGeneHeaderInfo(), 
							model.getArrayHeaderInfo());
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
	
	/*
	 * Gets the main view's config node.
	 */
	public Preferences getConfigNode() {
		
		return tvFrame.getConfigNode();
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
