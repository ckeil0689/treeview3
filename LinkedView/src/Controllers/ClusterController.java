package Controllers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import Utilities.StringRes;
import Cluster.CDTGenerator;
import Cluster.ClusterProcessor;
import Cluster.ClusterView;
import Cluster.ClusterViewDialog;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * Controls user input from ClusterView. It handles user interactions 
 * by implementing listeners which in turn call appropriate methods to respond.
 * 
 * @author CKeil
 * 
 */
public class ClusterController {
	
	/* Axes identifiers */
	public final static int ROW = 1;
	public final static int COL = 2;

	private final DataModel tvModel;
	private final TVFrameController tvController;
	private final ClusterView clusterView;
	private final ClusterViewDialog clusterDialog;

	private SwingWorker<Void, Void> loadWorker;
	private SwingWorker<Void, String> clusterProcess;
	private SwingWorker<Void, Void> saveWorker;

	/**
	 * Links the clustering functionality to the user interface. The object
	 * controls what happens in response to user actions. It makes sure
	 * that the right parameters are supplied to clustering methods and
	 * controls the UI response to user interaction.
	 * @param dialog The JDialog that contains the cluster UI.
	 * @param controller The TVFrameController, mostly used to enable file
	 * loading. 
	 * TODO Make use of OOP methods to create more lean controllers. Use
	 * interfaces and inheritance and avoid passing of the TVFrameController
	 * object here, just to use its loading methods. Also inherits instance
	 * variables like tvModel..
	 * TODO implement multi-core clustering.
	 * TODO Measure time for all processes.
	 */
	public ClusterController(final ClusterViewDialog dialog, 
			final TVFrameController controller) {

		this.clusterDialog = dialog;
		this.tvController = controller;
		this.tvModel = controller.getDataModel();
		this.clusterView = dialog.getClusterView();

		/* Add listeners after creating them */
		clusterView.addClusterListener(new ClusterListener());
		clusterView.addClusterTypeListener(new ClusterTypeListener());
		clusterView.addCancelListener(new CancelListener());
		clusterView.addLinkageListener(new ClusterChoiceListener());
	}
	
	/**
	 * Begins cluster process if the user clicks the 'Cluster' button in
	 * DendroView.
	 * @author CKeil
	 * 
	 */
	class ClusterListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			//final ClusterProcessWorker 
			clusterProcess = new ClusterProcessWorker();
			
			clusterProcess.execute();
		}
	}

	/**
	 * Processes clustering in the background according to user input. Allows
	 * for the UI to remain responsive during these tasks. This is
	 * useful for updating the progress bar and progress label. When the thread
	 * is done it calls a function to save the results to a CDT file.
	 * @author CKeil
	 *
	 */
	class ClusterProcessWorker extends SwingWorker<Void, String> {
		
		/* The finished reordered axes */
		private String[] reorderedRows;
		private String[] reorderedCols;
		
		private int pBarMax = 0;
		
		@Override
        protected void process(List<String> chunks) {
            
			String s = chunks.get(chunks.size()-1);
            ClusterView.setLoadText(s);
        }
		
		@Override
		protected Void doInBackground() throws Exception {
			
			/* Tell ClusterView that clustering begins */
			clusterView.setClustering(true);
			
			/* Get chosen similarity options. */
			int rowSimilarity = clusterView.getRowSimilarity();
			int colSimilarity = clusterView.getColSimilarity();
			
			/* Set maximum for progressbar */
			if(rowSimilarity != 0) {
				pBarMax += (2 * tvModel.getGeneHeaderInfo().getNumHeaders());
			}
			
			if(colSimilarity != 0) {
				pBarMax += (2 * tvModel.getArrayHeaderInfo().getNumHeaders());
			}
			
			ClusterView.setPBarMax(pBarMax);

			/* If no options are selected, display error and return null. */
			if (!checkSelections(rowSimilarity, ROW) 
					&& !checkSelections(colSimilarity, COL)) {
				clusterView.displayErrorLabel();
				return null;
			}
			
			/* When done, start distance measure */
			ClusterProcessor processor = new ClusterProcessor(tvModel);
			
			/* Row axis cluster */
			double[][] distMatrix = null;
			if (checkSelections(rowSimilarity, ROW)) {
				/* ProgressBar label */
				publish("Calculating row distances...");
				
				if(!isCancelled()) {
					distMatrix = processor.calcDistance(rowSimilarity, ROW);
				}
				
				if(distMatrix == null) return null;
				
				publish("Clustering row data...");
				
				if(!isCancelled()) {
					reorderedRows = processor.clusterAxis(distMatrix, 
						clusterView.getLinkageMethod(), 
						clusterView.getSpinnerValues(), isHierarchical(), ROW);
					LogBuffer.println("ReorderedRows length: " 
						+ reorderedRows.length);
				}
			}
			
			/* Column axis cluster */
			if (checkSelections(colSimilarity, COL)) {
				/* ProgressBar label */
				publish("Calculating column distances...");
				
				if(!isCancelled()) {
					distMatrix = processor.calcDistance(colSimilarity, COL);
				}
				
				if(distMatrix == null) return null;
				
				publish("Clustering column data...");
				
				if(!isCancelled()) {
					reorderedCols = processor.clusterAxis(distMatrix, 
						clusterView.getLinkageMethod(), 
						clusterView.getSpinnerValues(), isHierarchical(), COL);
					LogBuffer.println("ReorderedCols length: " 
						+ reorderedCols.length);
				}
			}
			
			return null;
		}
		
		@Override
		public void done() {
			
			saveClusterFile(reorderedRows, reorderedCols);
		}
	}

	/**
	 * Saves the clustering output (reordered axes) to a new CDT file, 
	 * so it can later be loaded and displayed.
	 */
	public void saveClusterFile(String[] reorderedRows, 
			String[] reorderedCols) {

		if (reorderedRows != null || reorderedCols != null) {
			ClusterView.setLoadText("Saving...");
			saveWorker = new SaveWorker(reorderedRows, reorderedCols);
			saveWorker.execute();
			LogBuffer.println("Saving started.");
			
		} else {
			String message = "Cannot save. No clustered data was created.";
			JOptionPane.showMessageDialog(JFrame.getFrames()[0], message, 
					"Error", JOptionPane.ERROR_MESSAGE);
			LogBuffer.println("Alert: " + message);
		}
	}
		
	/**
	 * Worker with the task to generate and write a new CDT file which 
	 * contains the newly clustered matrix. 
	 * Makes sure that the newly created file is visualized right after 
	 * clustering, given that the process was not cancelled.
	 * @param reorderedRows Reordered row axis.
	 * @author CKeil
	 */
	class SaveWorker extends SwingWorker<Void, Void> {

		/* The finished reordered axes */
		private String[] reorderedRows;
		private String[] reorderedCols;
		private String filePath;
		
		public SaveWorker(String[] reorderedRows, String[] reorderedCols) {
			
			this.reorderedRows = reorderedRows;
			this.reorderedCols = reorderedCols;
		}
		
		@Override
		protected Void doInBackground() throws Exception {
			
			if (!isCancelled()) {
				LogBuffer.println("Initializing CDT writer.");
				final CDTGenerator cdtGen = new CDTGenerator(
						tvModel, clusterView, reorderedRows, reorderedCols, 
						isHierarchical());

				LogBuffer.println("Generating CDT.");
				cdtGen.generateCDT();
				
				LogBuffer.println("Setting file path.");
				filePath = cdtGen.getFilePath();
				LogBuffer.println("Path: " + filePath);
			}
			return null;
		}

		@Override
		protected void done() {

			if (!isCancelled()) {
				ClusterView.setLoadText("Done!");
				LogBuffer.println("Done saving. Opening file now.");
				visualizeData(filePath);

			} else {
				clusterView.setClustering(false);
				LogBuffer.println("Clustering has been cancelled.");
			}
		}
	}

	/**
	 * Verifies whether all needed options are selected to 
	 * perform clustering.
	 * 
	 * @param distMeasure Selected distance measure.
	 * @return boolean Whether all needed selections have 
	 * appropriate values.
	 */
	public boolean checkSelections(final int distMeasure, int type) {

		if (isHierarchical()) {
			return distMeasure != 0;

		} else {
			final Integer[] spinnerValues = clusterView.getSpinnerValues();

			int groups;
			int iterations;
			
			switch(type) {
			
			case ROW: 	
				groups = spinnerValues[0]; 
				iterations = spinnerValues[1];
				break;
			case COL: 	
				groups = spinnerValues[2]; 
				iterations = spinnerValues[3];
				break;
			default:	
				groups = 0;
				iterations = 0;
				break;
			}

			return (distMeasure!= 0 && (groups > 0 && iterations > 0));
		}
	}

	/**
	 * Worker thread to load data chosen by the user. If the loading is
	 * successful, it sets the dataModel in the TVFrameController and loads the
	 * DendroView. If not, it also ensures the appropriate response of 
	 * DendroView and TVFrame.
	 */
	private class LoadWorker extends SwingWorker<Void, Void> {

		private FileSet fileSet;
		
		public LoadWorker(FileSet fileSet) {
			
			LogBuffer.println("Initializing LoadWorker.");
			this.fileSet = fileSet;
		}
		
		@Override
		protected Void doInBackground() throws Exception {

			LogBuffer.println("Loading data...");
			clusterDialog.dispose();
			
			tvController.loadFileSet(fileSet);
			return null;
		}

		@Override
		protected void done() {

			if (tvController.getDataModel().getDataMatrix()
					.getNumRow() > 0) {
				tvController.setDataModel();
				tvController.setViewChoice();
				LogBuffer.println("Successfully loaded.");

			} else {
				String message = "No clustered data matrix could be set.";
				JOptionPane.showMessageDialog(JFrame.getFrames()[0], 
						message, "Alert", JOptionPane.WARNING_MESSAGE);
				LogBuffer.println("Alert: " + message);
				clusterView.setClustering(false);
				tvController.setViewChoice();
				tvController.addViewListeners();
			}
		}
	}
	
	/**
	 * Listener listens to a change in selection for the clusterChoice
	 * JComboBox in clusterView. Calls a new layout setup as a response.
	 * @author CKeil
	 *
	 */
	class ClusterTypeListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			LogBuffer.println("Reset cluster type.");
			clusterDialog.reset();
		}
	}
	
	/**
	 * Listener listens to a change in selection for the clusterChoice
	 * JComboBox in clusterView. Calls a new layout setup as a response.
	 * @author CKeil
	 *
	 */
	class ClusterChoiceListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			LogBuffer.println("Reset ClusterView layout.");
			clusterView.setupLayout();
		}
	}

	/**
	 * Sets a new DendroView with the new data loaded into TVModel, displaying
	 * an updated HeatMap. It should also close the ClusterViewFrame.
	 */
	public void visualizeData(String filePath) {

		LogBuffer.println("Getting files for loading clustered data.");
		
		File file = null;

		if (filePath != null) {
			file = new File(filePath);

			FileSet fileSet = new FileSet(file.getName(), file.getParent()
					+ File.separator);

			LogBuffer.println("Setting view choice to begin loading.");
			tvController.setViewChoice();
			loadWorker = new LoadWorker(fileSet);
			loadWorker.execute();

		} else {
			String alert = "When trying to load the clustered file, no "
					+ "file path could be found.";
			JOptionPane.showMessageDialog(clusterDialog.getParentFrame(), 
					alert, "Alert", JOptionPane.WARNING_MESSAGE);
			LogBuffer.println("Alert: " + alert);
		}
	}

	/**
	 * Defines what happens if the user clicks the 'Cancel' button in
	 * DendroView. Calls the cancel() method in the view.
	 * TODO add cancel functionality to distance worker and cluster worker.
	 * @author CKeil
	 * 
	 */
	class CancelListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			LogBuffer.println("Trying to cancel.");
			
			if(clusterProcess != null) {
				clusterProcess.cancel(true);
				clusterProcess = null;
			}
		}
	}

	/**
	 * Returns whether hierarchical clustering is currently selected or not.
	 * 
	 * @return boolean Whether the user selected hierarchical clustering (true)
	 * or k-means (false).
	 */
	public boolean isHierarchical() {

		return clusterView.getClusterMethod()
				.equalsIgnoreCase(StringRes.menu_Hier);
	}
}
