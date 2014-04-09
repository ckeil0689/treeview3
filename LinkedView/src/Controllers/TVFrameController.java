package Controllers;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import Cluster.ClusterViewDialog;
import edu.stanford.genetics.treeview.CdtFilter;
import edu.stanford.genetics.treeview.DataMatrix;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.DataModelFileType;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.GeneListMaker;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.PreferencesMenu;
import edu.stanford.genetics.treeview.SSMouseListener;
import edu.stanford.genetics.treeview.TreeSelection;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.UrlExtractor;
import edu.stanford.genetics.treeview.UrlPresets;
import edu.stanford.genetics.treeview.model.DataModelWriter;
import edu.stanford.genetics.treeview.model.TVModel;

/**
 * This class controls user interaction with TVFrame and its views.
 * @author CKeil
 *
 */
public class TVFrameController {

	private TVModel model;
	private TreeViewFrame tvFrame;
	private JFrame applicationFrame;
	private MenubarActions menuActions;
	
	private SwingWorker<Void, Void> worker;
	private File file;
	private FileSet fileMenuSet;
	
	private boolean error = false;
	
	public TVFrameController(TreeViewFrame tvFrame, TVModel model) {
		
		this.model = model;
		this.tvFrame = tvFrame;
		this.applicationFrame = tvFrame.getAppFrame();
		
		setViewChoice();
	}
	
	/**
	 * Adds listeners to views that are instantiated in TVFrame.
	 */
	public void addViewListeners() {
		
		menuActions =  new MenubarActions(tvFrame, 
				TVFrameController.this);
		
		tvFrame.addStackButtonListener(new StackButtonListener());
		tvFrame.addMenuActionListeners(new StackMenuListener());
		tvFrame.addFileMenuListeners(new FileMenuListener());
		
		if(tvFrame.getWelcomeView() != null) {
			tvFrame.getWelcomeView().addLoadListener(new LoadPanelListener(
					tvFrame.getWelcomeView().getLoadIcon(), 
					tvFrame.getWelcomeView().getLoadLabel()));
		}
		
		if(tvFrame.getLoadErrorView() != null) {
			tvFrame.getLoadErrorView().addLoadNewListener(
					new LoadButtonListener());
		}
	}
	
	/**
	 * Handles the loading of data.
	 * @author CKeil
	 *
	 */
	class LoadPanelListener extends SSMouseListener {
		
		public LoadPanelListener(JPanel panel, JLabel label) {
			
			super(panel, label);
		}

		@Override
		public void mouseClicked(MouseEvent arg0) {

			// Setting screen for loading bar.
			openFile();
		}	
	}
	
	/**
	 * Handles the new loading of data.
	 * @author CKeil
	 *
	 */
	class LoadButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			// Setting screen for loading bar.
			openFile();
		}	
	}
	
//	/**
//	 * Sets TVFrame "loaded" to true which triggers the setup of DendroView.
//	 * @author CKeil
//	 *
//	 */
//	class ContinueListener implements ActionListener {
//
//		@Override
//		public void actionPerformed(ActionEvent arg0) {
//
//			setLoaded();
//		}	
//	}
	
	
	class StackButtonListener implements MouseListener {

		@Override
		public void mouseClicked(MouseEvent e) {
			
			tvFrame.getStackMenu().show(e.getComponent(), 0, 
					e.getComponent().getHeight());
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			
			e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
		}

		@Override
		public void mouseExited(MouseEvent e) {
			
			e.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseReleased(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}
	}
	
//	class SearchButtonListener implements MouseListener {
//
//		@Override
//		public void mouseClicked(MouseEvent e) {
//			
//			if(tvFrame.getRunning() != null) {
//				JWindow window = tvFrame.getRunning().openSearchPanel(); 
//				window.setLocationRelativeTo(e.getComponent());
//			    window.pack();
//			    window.setVisible(true);
//			}
//		}
//
//		@Override
//		public void mouseEntered(MouseEvent e) {
//			
//			e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
//		}
//
//		@Override
//		public void mouseExited(MouseEvent e) {
//			
//			e.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
//		}
//
//		@Override
//		public void mousePressed(MouseEvent e) {
//			// TODO Auto-generated method stub
//			
//		}
//
//		@Override
//		public void mouseReleased(MouseEvent arg0) {
//			// TODO Auto-generated method stub
//			
//		}
//	}
//	
//	class TreeButtonListener implements MouseListener {
//
//		@Override
//		public void mouseClicked(MouseEvent e) {
//			
//			if(tvFrame.getRunning() != null) {
//				if(tvFrame.getTreeButton().getText()
//						.equalsIgnoreCase("SHOW TREES")) {
//					tvFrame.getTreeButton().setText("HIDE TREES");
//					tvFrame.getRunning().setTreesVisible(true);
//					tvFrame.getRunning().refresh();
//					addViewListeners();
//					
//				} else {
//					tvFrame.getTreeButton().setText("SHOW TREES");
//					tvFrame.getRunning().setTreesVisible(false);
//					tvFrame.getRunning().refresh();
//					addViewListeners();
//				}
//			}
//		}
//
//		@Override
//		public void mouseEntered(MouseEvent e) {
//			
//			e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
//		}
//
//		@Override
//		public void mouseExited(MouseEvent e) {
//			
//			e.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
//		}
//
//		@Override
//		public void mousePressed(MouseEvent e) {
//			// TODO Auto-generated method stub
//			
//		}
//
//		@Override
//		public void mouseReleased(MouseEvent arg0) {
//			// TODO Auto-generated method stub
//			
//		}
//	}
	
	class StackMenuListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			
			ArrayList<JMenuItem> menuList = tvFrame.getStackMenus();
			
			for(int i = 0; i < menuList.size(); i++) {
				if(e.getSource() ==  menuList.get(i)) {
					
					menuActions.execute(menuList.get(i).getText());
				}
			}
		}
	}
	
	public void setViewChoice() {
		
		if(error) {
			tvFrame.setView("LoadErrorView");
			tvFrame.setLoaded(false);
			addViewListeners();
			
			LogBuffer.println("DataModel not properly loaded.");
			
		} else {
			if(tvFrame.getDataModel() != null) {
				tvFrame.setView("DendroView");
				tvFrame.setLoaded(true);
				addViewListeners();
				
			} else {
				tvFrame.setView("WelcomeView");
				tvFrame.setLoaded(false);
				addViewListeners();
			}
		}
	}
	
	/**
	 * Setting up a worker thread to do the CDT generation if a non-cdt file was
	 * selected by the user. This prevents the GUI from locking up and allows
	 * the ProgressBar to display progress.
	 */
	public void setupWorkerThread() {
		
		worker = new SwingWorker<Void, Void>() {

			@Override
			public Void doInBackground() {
				
				FileSet fileSet = null;
				
				if(fileMenuSet == null) {
//						final String fileName = file.getAbsolutePath();
//						final int dotIndex = fileName.indexOf(".");
//	
//						final int suffixLength = fileName.length() - dotIndex;
//	
//						fileType = file.getAbsolutePath().substring(
//								fileName.length() - suffixLength, 
//								fileName.length());
//					
//						if (!fileType.equalsIgnoreCase(".cdt")) {
//							
//							
//								final CDTCreator3 fileTransformer = 
//										new CDTCreator3(file, fileType, 
//												tvFrame);
//								
//								fileTransformer.createFile();
//				
//								file = new File(fileTransformer.getFilePath());
//								
//						}
					fileSet = tvFrame.getFileSet(file);
					
				} else {
					fileSet = fileMenuSet;
				}
			
				// Loading TVModel
				loadFileSet(fileSet);
				
				if(fileSet != null) {
					fileSet = tvFrame.getFileMRU().addUnique(fileSet);
					tvFrame.getFileMRU().setLast(fileSet);
					
				} else {
					System.out.println("FileSet is null.");
				}
					
				return null;
			}

			@Override
			protected void done() {
 
				if(model.isLoaded()) {
					fileMenuSet = null;
					setDataModel();
					error = false;
					
				} else {
					error = true;
				}
				
				setViewChoice();
			}
		};
	}
	
	/**
	 * This method opens a file dialog to open either the visualization view or
	 * the cluster view depending on which file type is chosen.
	 * @throws IOException 
	 * 
	 * @throws LoadException
	 */
	public void openFile() {
	
		setupWorkerThread();
		
		try {
			file = tvFrame.selectFile();
			worker.execute();
			
		} catch (LoadException e) {
			System.out.println("Loading the FileSet was interrupted. " +
					"Cause: " + e.getCause());
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

		// Make TVModel object
		model = new TVModel();
		model.setFrame(tvFrame);
		
		tvFrame.setView("LoadProgressView");

		try {
			// load instance variables of TVModel with data
			model.loadNew(fileSet);
			
		} catch (OutOfMemoryError e) {
			String oomError = "The data file is too large. " +
					"Increase the JVM's heap size. Error: " + e.getMessage();
			tvFrame.setLoadErrorMessage(oomError);
			e.printStackTrace();

		} catch (final LoadException e) {
			if (e.getType() != LoadException.INTPARSE) {
				JOptionPane.showMessageDialog(applicationFrame, e);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			
		} catch (ExecutionException e) {
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

		final DataMatrix matrix = tvFrame.getDataModel().getDataMatrix();
		final int ngene = matrix.getNumRow();
		final int nexpr = matrix.getNumCol();

		final Preferences documentConfig = tvFrame.getDataModel()
				.getDocumentConfigRoot();
		
		// extractors...
		final UrlPresets genePresets = tvFrame.getGeneUrlPresets();
		final UrlExtractor urlExtractor = new UrlExtractor(
				tvFrame.getDataModel().getGeneHeaderInfo(), genePresets);
		urlExtractor.bindConfig(documentConfig.node("UrlExtractor"));
		tvFrame.setUrlExtractor(urlExtractor);

		final UrlPresets arrayPresets = tvFrame.getArrayUrlPresets();
		final UrlExtractor arrayUrlExtractor = new UrlExtractor(
				tvFrame.getDataModel().getArrayHeaderInfo(), arrayPresets);
		arrayUrlExtractor.bindConfig(documentConfig
				.node("ArrayUrlExtractor"));
		tvFrame.setArrayUrlExtractor(arrayUrlExtractor);

		tvFrame.setGeneSelection(new TreeSelection(ngene));
		tvFrame.setArraySelection(new TreeSelection(nexpr));
	}
	
	/**
	 * Opens the ClusterViewFrame with either the options for hierarchical
	 * clustering or K-Means, depending on the boolean parameter.
	 * @param hierarchical
	 */
	public void setupClusterView() {
		
		// Making a new Window to display clustering components
		ClusterViewDialog clusterViewFrame = new ClusterViewDialog(tvFrame);
		
		// Creating the Controller for this view.
		ClusterViewController clusControl = 
				new ClusterViewController(clusterViewFrame.getClusterView()
						, tvFrame, this);
		
		// Make the clustering window visible.
		clusterViewFrame.setVisible(true);
	}
	
	/**
	 * Setter for dataModel for TVFrame, also sets extractors, running.
	 * 
	 * @param DataModel newModel
	 */
	public void setDataModel() {

		if(model != null) {
			if (tvFrame.getDataModel() != null) {
				tvFrame.getDataModel().clearFileSetListeners();
			}
	
			tvFrame.setDataModel(model);
	
			if (tvFrame.getDataModel() != null) {
				tvFrame.getDataModel().addFileSetListener(tvFrame);
			}
					
			setupExtractors();
			
		} else {
			tvFrame.setDataModel(null);
		}
	}
	
	/**
	 * Opens an instance of GeneListMaker used to save a list of genes.
	 */
	public void saveList() {
		
		if (warnSelectionEmpty()) {
			final FileSet source = tvFrame.getDataModel().getFileSet();
			String def = tvFrame.getDataModel().getName() + "_list.txt";
		
			if (source != null) {
		
				def = source.getDir() + source.getRoot() + "_list.txt";
			}
		
			final GeneListMaker t = new GeneListMaker(applicationFrame,
					tvFrame.getGeneSelection(), tvFrame.getDataModel()
							.getGeneHeaderInfo(), def);
		
			t.setDataMatrix(tvFrame.getDataModel().getDataMatrix(),
					tvFrame.getDataModel().getArrayHeaderInfo(),
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
			final FileSet source = tvFrame.getDataModel().getFileSet();

			final GeneListMaker t = new GeneListMaker(applicationFrame,
					tvFrame.getGeneSelection(), tvFrame.getDataModel()
							.getGeneHeaderInfo(), source.getDir()
							+ source.getRoot() + "_data.cdt");

			t.setDataMatrix(tvFrame.getDataModel().getDataMatrix(),
					tvFrame.getDataModel().getArrayHeaderInfo(),
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
	 * @param incremental
	 * @return
	 */
	public boolean doModelSave(final boolean incremental) {

		final DataModelWriter writer = 
				new DataModelWriter(tvFrame.getDataModel());
		final Set<DataModelFileType> written;

		if (incremental) {
			written = writer.writeIncremental(
					tvFrame.getDataModel().getFileSet());

		} else {
			written = writer.writeAll(tvFrame.getDataModel().getFileSet());
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
		
		if (tvFrame.getDataModel().getFileSet() == null) {
			JOptionPane.showMessageDialog(applicationFrame,
					"Saving of datamodels not backed by "
							+ "files is not yet supported.");
	
		} else {
			final JFileChooser fileDialog = new JFileChooser();
			final CdtFilter ff = new CdtFilter();
			fileDialog.setFileFilter(ff);
	
			final String string = tvFrame.getDataModel().getFileSet()
					.getDir();
	
			if (string != null) {
	
				fileDialog
						.setCurrentDirectory(new File(string));
			}
	
			final int retVal = fileDialog
					.showSaveDialog(applicationFrame);
	
			if (retVal == JFileChooser.APPROVE_OPTION) {
				final File chosen = fileDialog
						.getSelectedFile();
				String name = chosen.getName();
	
				if (!name.toLowerCase().endsWith(".cdt")
						&& !name.toLowerCase().endsWith(".pcl")) {
					name += ".cdt";
				}
	
				FileSet fileSet2 = new FileSet(name, chosen
						.getParent() + File.separator);
				fileSet2.copyState(tvFrame.getDataModel().getFileSet());
	
				final FileSet fileSet1 = new FileSet(name,
						chosen.getParent() + File.separator);
				fileSet1.setName(tvFrame.getDataModel().getFileSet()
						.getName());
	
				tvFrame.getDataModel().getFileSet().copyState(fileSet1);
				doModelSave(false);
	
				tvFrame.getDataModel().getFileSet().notifyMoved();
				tvFrame.getFileMRU().removeDuplicates(tvFrame.getDataModel()
						.getFileSet());
				fileSet2 = tvFrame.getFileMRU().addUnique(fileSet2);
				tvFrame.getFileMRU().setLast(
						tvFrame.getDataModel().getFileSet());
				tvFrame.rebuildProgramMenu();
				tvFrame.addFileMenuListeners(new FileMenuListener());
	
				if (tvFrame.getDataModel() instanceof TVModel) {
					((TVModel) tvFrame.getDataModel())
					.getDocumentConfig().put("jtv", tvFrame.getDataModel()
							.getFileSet().getJtv());
				}
			}
		}
	}
	
	/**
	 * This class is an ActionListener which overrides the run() function.
	 */
	class FileMenuListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent actionEvent) {

			tvFrame.getFileMRU().setLast(tvFrame.getFileMenuSet());
			tvFrame.getFileMRU().notifyObservers();

			fileMenuSet = tvFrame.getFileMenuSet();
			
			setupWorkerThread();
			tvFrame.setView("LoadProgressView");
			worker.execute();

//					} catch (final LoadException e) {
//						if (e.getType() == LoadException.INTPARSE) {
//							
//						} else {
//							final int result = FileMruEditor.offerSearch(
//									tvFrame.getFileMenuSet(), 
//									tvFrame, "Could not Load " 
//											+ tvFrame.getFileMenuSet().getCdt());
//
//							if (result == FileMruEditor.FIND) {
//								tvFrame.getFileMRU().notifyFileSetModified();
//								tvFrame.getFileMRU().notifyObservers();
//
//								actionPerformed(actionEvent); // REPROCESS...
//								return; // EARLY RETURN
//
//							} else if (result == FileMruEditor.REMOVE) {
//								tvFrame.getFileMRU().removeFileSet(
//										tvFrame.getFileMenuSet());
//								tvFrame.getFileMRU().notifyObservers();
//							}
//						}
//						tvFrame.setLoaded(false);
//					}
					// dataModel.notifyObservers();
//				}
//			};
//			SwingUtilities.invokeLater(update);
		}
	}
	
	/**
	 * Opens the preferences menu and sets the displayed menu to
	 * the specified option using a string as identification.
	 * @param menu
	 */
	public void openPrefMenu(String menu, String dialogTitle) {
		
		if(tvFrame.getDendroView() == null) {
			PreferencesMenu preferences = new PreferencesMenu(tvFrame, menu, 
					dialogTitle);
			
			PreferencesController pController = 
					new PreferencesController(tvFrame, preferences, this);
			
			preferences.setVisible(true);
		
		} else {
			PreferencesMenu preferences = new PreferencesMenu(
					tvFrame, tvFrame.getDendroView(), 
					tvFrame.getDendroController(), menu, dialogTitle);
			
			PreferencesController pController = 
					new PreferencesController(tvFrame, preferences, this);
			
			preferences.setVisible(true);
		}
	}
	
	/**
	 * Returns TVFrameController's model.
	 * @return
	 */
	public TVModel getTVControllerModel() {
		
		return model;
	}
}
