package edu.stanford.genetics.treeview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import Views.LoadProgressView;
import Views.WelcomeView;

import Cluster.ClusterProcessor;
import Cluster.ClusterViewController;
import Cluster.ClusterViewFrame;
import Controllers.MenubarActions;

import edu.stanford.genetics.treeview.model.CDTCreator3;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;

/**
 * This class controls user interaction with TVFrame and its views.
 * @author CKeil
 *
 */
public class TVFrameController {

	private TreeViewFrame tvFrame;
	private TVModel model;
	private SwingWorker<Void, Void> worker;
	private File file;
	private String fileType;
	
	public TVFrameController(TreeViewFrame tvFrame, TVModel model) {
		
		this.tvFrame = tvFrame;
		this.model = model;
		
//		setupWorkerThread();
		
		tvFrame.addMenuActionListeners(new TVMenuListener());
		
		// Get the Views from TVFrame
		WelcomeView welcomeView = tvFrame.getWelcomeView();
		
		// add listeners to Views
		welcomeView.addLoadListener(new LoadPanelListener(
				welcomeView.getLoadIcon(), 
				welcomeView.getLoadIcon().getLabel()));
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
			if(tvFrame.getLoaded()) {
				tvFrame.setLoaded(false);
			}
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
			
			tvFrame.setDataModel(null);
			
			// Setting screen for loading bar.
			if(tvFrame.getLoaded()) {
				tvFrame.setLoaded(false);
			}
			openFile();
		}	
	}
	
	/**
	 * Sets TVFrame "loaded" to true which triggers the setup of DendroView.
	 * @author CKeil
	 *
	 */
	class ContinueListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			if(tvFrame.getDataModel() != null) {
				tvFrame.setView("DendroView");
//				tvFrame.setLoaded(true);
				tvFrame.addMenuActionListeners(new TVMenuListener());
				
			} else {
				System.out.println("Couldn't continue, dataModel is null.");
			}
		}	
	}
	
	class TVMenuListener implements ActionListener {

		private MenubarActions menuActions;
		
		public TVMenuListener() {
			
			menuActions =  new MenubarActions(tvFrame, 
					TVFrameController.this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			
			ArrayList<JMenuItem> menuList = tvFrame.getMenus();
			
			for(JMenuItem menuItem : menuList) {
				if(e.getSource() ==  menuItem) {
					
					menuActions.execute(menuItem.getText());
				}
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
				
				try {
					final String fileName = file.getAbsolutePath();
					final int dotIndex = fileName.indexOf(".");

					final int suffixLength = fileName.length() - dotIndex;

					fileType = file.getAbsolutePath().substring(
							fileName.length() - suffixLength, 
							fileName.length());
				
					if (!fileType.equalsIgnoreCase(".cdt")) {
						
						
							final CDTCreator3 fileTransformer = 
									new CDTCreator3(file, fileType, tvFrame);
							
							fileTransformer.createFile();
			
							file = new File(fileTransformer.getFilePath());
							
					}
					FileSet fileSet = tvFrame.getFileSet(file);
				
					// Loading TVModel
					loadFileSet(fileSet);
					
					if(fileSet != null) {
						fileSet = tvFrame.getFileMRU().addUnique(fileSet);
						tvFrame.getFileMRU().setLast(fileSet);
						
					} else {
						System.out.println("FileSet is null.");
					}
					
				} catch (IOException e) {
					System.out.println("Could not generate CDT file. Cause: " +
							e.getCause());
					e.printStackTrace();
					
				} catch (LoadException e) {
					System.out.println("Loading the FileSet was interrupted. " +
							"Cause: " + e.getCause());
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void done() {

				if(model.getDataMatrix().getNumRow() > 0) {
					setDataModel(model);
					
				} else {
					System.out.println("No datamatrix set by worker thread.");
				}
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
			tvFrame.setView("LoadProgressView");
			tvFrame.setLoaded(false);
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

		tvFrame.setLoaded(true);
	}
	
	/**
	 * r * This is the workhorse. It creates a new DataModel of the file, and
	 * then sets the Datamodel. A side effect of setting the datamodel is to
	 * update the running window.
	 */
	public void loadFileSet(final FileSet fileSet) throws LoadException {

		// Make TVModel object
//		model = new TVModel();
		model.setFrame(tvFrame);

		try {
			// load instance variables of TVModel with data
			// use worker thread
			
			model.loadNew(fileSet);

		} catch (final LoadException e) {
			if (e.getType() != LoadException.INTPARSE) {
				JOptionPane.showMessageDialog(tvFrame, e);
				throw e;
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
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
	 * To load any fileset without using the event queue thread
	 */
	public void loadNW(FileSet fileSet) throws LoadException {

		loadFileSetNW(fileSet);

		fileSet = tvFrame.getFileMRU().addUnique(fileSet);
		tvFrame.getFileMRU().setLast(fileSet);
		tvFrame.getFileMRU().notifyObservers();

		tvFrame.setLoaded(true);
	}

	/**
	 * To load any FileSet without using the event queue thread
	 */
	public void loadFileSetNW(final FileSet fileSet) throws LoadException {

		model.setFrame(tvFrame);

		try {
			model.loadNewNW(fileSet);
			setDataModel(model);
		} catch (final LoadException e) {
			if (e.getType() != LoadException.INTPARSE) {
				JOptionPane.showMessageDialog(tvFrame, e);
				throw e;
			}
		}
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

		final ConfigNode documentConfig = tvFrame.getDataModel()
				.getDocumentConfigRoot();
		
		// extractors...
		final UrlPresets genePresets = tvFrame.getGeneUrlPresets();
		final UrlExtractor urlExtractor = new UrlExtractor(
				tvFrame.getDataModel().getGeneHeaderInfo(), genePresets);
		urlExtractor.bindConfig(documentConfig.fetchOrCreate("UrlExtractor"));
		tvFrame.setUrlExtractor(urlExtractor);

		final UrlPresets arrayPresets = tvFrame.getArrayUrlPresets();
		final UrlExtractor arrayUrlExtractor = new UrlExtractor(
				tvFrame.getDataModel().getArrayHeaderInfo(), arrayPresets);
		arrayUrlExtractor.bindConfig(documentConfig
				.fetchOrCreate("ArrayUrlExtractor"));
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
		ClusterViewFrame clusterViewFrame = new ClusterViewFrame(tvFrame);
		
		// Creating the Controller for this view.
		ClusterViewController clusControl = 
				new ClusterViewController(clusterViewFrame.getClusterView(), 
						tvFrame);
		
		// Make the clustering window visible.
		clusterViewFrame.setVisible(true);
	}
	
	/**
	 * Setter for dataModel for TVFrame, also sets extractors, running.
	 * 
	 * @param DataModel newModel
	 */
	public void setDataModel(DataModel newModel) {

		if(newModel != null) {
			if (tvFrame.getDataModel() != null) {
				tvFrame.getDataModel().clearFileSetListeners();
			}
	
			tvFrame.setDataModel(newModel);
			newModel = null;
	
			if (tvFrame.getDataModel() != null) {
				tvFrame.getDataModel().addFileSetListener(tvFrame);
			}
	
			tvFrame.setView("LoadCheckView");
			tvFrame.getLoadCheckView().addLoadListener(
					new LoadButtonListener());
			tvFrame.getLoadCheckView().addContinueListener(
					new ContinueListener());;
					
			setupExtractors();
			
		} else {
			tvFrame.setDataModel(null);
			tvFrame.setLoaded(false);
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
		
			final GeneListMaker t = new GeneListMaker(tvFrame,
					tvFrame.getGeneSelection(), tvFrame.getDataModel()
							.getGeneHeaderInfo(), def);
		
			t.setDataMatrix(tvFrame.getDataModel().getDataMatrix(),
					tvFrame.getDataModel().getArrayHeaderInfo(),
					DataModel.NODATA);
		
			t.bindConfig(tvFrame.getDataModel().getDocumentConfigRoot()
					.fetchOrCreate("GeneListMaker"));
		
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

			final GeneListMaker t = new GeneListMaker(tvFrame,
					tvFrame.getGeneSelection(), tvFrame.getDataModel()
							.getGeneHeaderInfo(), source.getDir()
							+ source.getRoot() + "_data.cdt");

			t.setDataMatrix(tvFrame.getDataModel().getDataMatrix(),
					tvFrame.getDataModel().getArrayHeaderInfo(),
					DataModel.NODATA);

			t.bindConfig(tvFrame.getDataModel().getDocumentConfigRoot()
					.fetchOrCreate("GeneListMaker"));

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

			JOptionPane.showMessageDialog(tvFrame,
					"Cannot generate gene list, no gene selected");
			return false;
		}
		return true;
	}
}
