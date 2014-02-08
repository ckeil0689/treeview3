package edu.stanford.genetics.treeview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import Cluster.ClusterProcessor;
import Cluster.ClusterViewController;
import Cluster.ClusterViewFrame;

import edu.stanford.genetics.treeview.model.CDTCreator3;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;

public class TVFrameController {

	private TreeViewFrame tvFrame;
	private TVModel model;
	private SwingWorker<Void, Void> worker;
	private String fileType;
	
	public TVFrameController(TreeViewFrame tvFrame) {
		
		this.tvFrame = tvFrame;
		
		setupWorkerThread();
		
		// add listeners to TVFrame
		tvFrame.addLoadListener(new LoadPanelListener(tvFrame.getLoadIcon(), 
				tvFrame.getLoadIcon().getLabel()));
		tvFrame.addLoadListener(new LoadListener());
		tvFrame.addContinueListener(new ContinueListener());
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
			tvFrame.setLoading();
			worker.execute();
		}	
	}
	
	/**
	 * Handles the new loading of data.
	 * @author CKeil
	 *
	 */
	class LoadListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			tvFrame.setDataModel(null);
			
			// Setting screen for loading bar.
			tvFrame.setLoading();
			worker.execute();
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
				tvFrame.setLoaded(true);
				
			} else {
				System.out.println("Couldn't continue, dataModel is null.");
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
				
				openFile();
				return null;
			}

			@Override
			protected void done() {

				setDataModel(model);
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
		
		File file;
		try {
			file = tvFrame.selectFile();
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
		model = new TVModel();
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
	
			tvFrame.confirmLoaded();
			setupExtractors();
			setupRunning();
			
		} else {
			tvFrame.setLoaded(false);
		}
	}
	
	/**
	 * Generates a DendroView object and sets the current running MainPanel to
	 * it. As a result the View is displayed in the TreeViewFrame
	 */
	protected void setupRunning() {

		final DendroView dv2 = new DendroView(tvFrame.getDataModel(), tvFrame);
		tvFrame.setRunning(dv2);
	}
}
