package Controllers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import Utilities.StringRes;
import Cluster.CDTGenerator;
import Cluster.ClusterProcessor;
import Cluster.ClusterView;
import Cluster.ClusterDialog;
import Cluster.DistMatrixCalculator;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.IntHeaderInfo;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;

/**
 * Controls user input from ClusterView. It handles user interactions 
 * by implementing listeners which in turn call appropriate methods to respond.
 * When the user starts clustering by clicking the appropriate button in 
 * ClusterView, a SwingWorker thread will be created which generates a cascade
 * of multiple threads that take over various background calculations. 
 * This prevents the GUI from locking up.
 * 
 * TODO Make use of OOP methods to create more lean controllers. Use
 * interfaces and inheritance and avoid passing of the TVFrameController
 * object here, just to use its loading methods. Also inherits instance
 * variables like tvModel..
 * 
 * @author CKeil
 * 
 */
public class ClusterController {
	
	/* Axes identifiers */
	public final static int ROW = 1;
	public final static int COL = 2;

	private final DataModel tvModel;
	private final TVController tvController;
	private final ClusterView clusterView;
	private final ClusterDialog clusterDialog;
	
	private ClusterProcessor processor;
	
	/* Initialize with defaults for error checking parameters */
	private int rowSimilarity = DistMatrixCalculator.PEARSON_UN;
	private int colSimilarity = DistMatrixCalculator.PEARSON_UN;

	private SwingWorker<Void, String> clusterTask;
	private SwingWorker<Void, Void> saveTask;

	/**
	 * Links the clustering functionality to the user interface. The object
	 * controls what happens in response to user actions. It makes sure
	 * that the right parameters are supplied to clustering methods and
	 * controls the UI response to user interaction.
	 * @param dialog The JDialog that contains the cluster UI.
	 * @param controller The TVFrameController, mostly used to enable file
	 * loading.
	 */
	public ClusterController(final ClusterDialog dialog, 
			final TVController controller) {

		this.clusterDialog = dialog;
		this.tvController = controller;
		this.tvModel = controller.getDataModel();
		this.clusterView = dialog.getClusterView();

		/* Create and add all view component listeners */
		addAllListeners();
	}
	
	public void addAllListeners() {
		
		if(clusterView != null) {
			clusterView.addClusterListener(new TaskStartListener());
			clusterView.addClusterTypeListener(new ClusterTypeListener());
			clusterView.addCancelListener(new CancelListener());
			clusterView.addLinkageListener(new LinkChoiceListener());
			clusterView.addRowDistListener(new RowDistListener());
			clusterView.addColDistListener(new ColDistListener());
			clusterView.addSpinnerListener(new SpinnerListener());
			
		} else {
			LogBuffer.println("Cannot add listeners, clusterView is null.");
		}
	}
	
	/**
	 * Begins cluster process if the user clicks the 'Cluster' button in
	 * DendroView and sufficient parameters are set.
	 * @author CKeil
	 * 
	 */
	private class TaskStartListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {
			
			/* Only starts with valid selections. */
			if (isReady(rowSimilarity, ROW) || isReady(colSimilarity, COL)) {
				/* Tell ClusterView that clustering begins */
				clusterView.setClustering(true);
				clusterTask = new ClusterTask();
				clusterTask.execute();
			}
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
	private class ClusterTask extends SwingWorker<Void, String> {
		
		/* The finished reordered axes */
		private String[] reorderedRows;
		private String[] reorderedCols;
		
		private String fileName;
		
		private int pBarMax = 0;
		
		@Override
        protected void process(List<String> chunks) {
            
			String s = chunks.get(chunks.size()-1);
            ClusterView.setLoadText(s);
        }
		
		@Override
		protected Void doInBackground() throws Exception {
			
			int rows = tvModel.getGeneHeaderInfo().getNumHeaders();
			int cols = tvModel.getArrayHeaderInfo().getNumHeaders();
			
			/* 
			 * Set maximum for JProgressBar before any clustering!
			 */
			if(rowSimilarity != 0) {
				
				/* Check if ranking first or not. */
				pBarMax += (rowSimilarity == 5) ? (3 * rows) : (2 * rows);
				
			}
			
			if(colSimilarity != 0) {
				
				/* Check if ranking first or not. */
				pBarMax += (colSimilarity == 5) ? (3 * cols) : (2 * cols);
				
			}
			
			ClusterView.setPBarMax(pBarMax);
			
			/* Get fileName for saving calculated data */
			fileName = tvModel.getSource().substring(0, 
					tvModel.getSource().length() - 4);
			
			IntHeaderInfo geneHeaderI = tvModel.getGeneHeaderInfo();
			IntHeaderInfo arrayHeaderI = tvModel.getArrayHeaderInfo();
			
			/* Initialize the clustering processor and pass the data */
			TVDataMatrix originalMatrix = (TVDataMatrix)tvModel.getDataMatrix();
			processor = new ClusterProcessor(originalMatrix, fileName, 
					geneHeaderI, arrayHeaderI);
			
			/* Row axis cluster */
			reorderedRows = calculateAxis(rowSimilarity, ROW, fileName);
			LogBuffer.println("ReorderedRows length: " + reorderedRows.length);
			
			if(isCancelled()) return null;
			
			/* Column axis cluster */
			reorderedCols = calculateAxis(colSimilarity, COL, fileName);
			LogBuffer.println("ReorderedCols length: " + reorderedCols.length);
			
			return null;
		}
		
		@Override
		public void done() {
			
			if(!isCancelled()) { 
				saveClusterFile(fileName);
				
			} else {
				clusterView.setClustering(false);
				LogBuffer.println("---------------------------------------");
				LogBuffer.println("Clustering has been cancelled.");
			}
		}
		
		/**
		 * Controls clustering procedures for one axis.
		 * @param similarity The chosen similarity measure.
		 * @param axis The chosen matrix axis.
		 * @return A list of reordered axis elements.
		 */
		private String[] calculateAxis(int similarity, int axis, 
				String fileName) {
			
			/* Row axis cluster */
			double[][] distMatrix = null;
			
			/* Check if this axis should be clustered */
			if (!isReady(similarity, axis)) return new String[] {};
				
			String axisPrefix = (axis == ROW) ? "row" : "column";
			
			/* ProgressBar label */
			publish("Calculating " + axisPrefix + " distances...");
			
			/* Calculating the distance matrix */
			distMatrix = processor.calcDistance(similarity, axis);
			
			if(distMatrix == null || isCancelled()) return null;
			
			publish("Clustering " + axisPrefix + " data...");
			
			return processor.clusterAxis(distMatrix, 
					clusterView.getLinkMethod(), 
					clusterView.getSpinnerValues(), 
					isHierarchical(), axis);
		}
		
		/**
		 * Saves the clustering output (reordered axes) to a new CDT file, 
		 * so it can later be loaded and displayed.
		 */
		private void saveClusterFile(String fileName) {

			if (reorderedRows != null || reorderedCols != null) {
				ClusterView.setLoadText("Saving...");
				saveTask = new SaveTask(reorderedRows, reorderedCols, fileName);
				saveTask.execute();
				
			} else {
				String message = "Cannot save. No clustered data was created.";
				JOptionPane.showMessageDialog(JFrame.getFrames()[0], message, 
						"Error", JOptionPane.ERROR_MESSAGE);
				LogBuffer.println("Alert: " + message);
			}
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
	private class SaveTask extends SwingWorker<Void, Void> {

		/* The finished reordered axes */
		private String[] reorderedRows;
		private String[] reorderedCols;
		
		private String fileName;
		private String filePath;
		
		public SaveTask(String[] reorderedRows, String[] reorderedCols, 
				String fileName) {
			
			this.reorderedRows = reorderedRows;
			this.reorderedCols = reorderedCols;
			this.fileName = fileName;
		}
		
		@Override
		protected Void doInBackground() throws Exception {
			
			LogBuffer.println("Begin saving...");
			
			TVDataMatrix originalMatrix = (TVDataMatrix)tvModel.getDataMatrix();
			double[][] data = originalMatrix.getExprData();
			
			final CDTGenerator cdtGen = new CDTGenerator(data, reorderedRows, 
					reorderedCols, rowSimilarity, colSimilarity,
					isHierarchical());
			
			LogBuffer.println("Setting up buffered writer...");
			cdtGen.setupWriter(fileName, clusterView.getSpinnerValues());

			IntHeaderInfo geneHeaderI = tvModel.getGeneHeaderInfo();
			IntHeaderInfo arrayHeaderI = tvModel.getArrayHeaderInfo();
			
			cdtGen.prepare(geneHeaderI, arrayHeaderI);
			cdtGen.generateCDT();
			
			filePath = cdtGen.finish();
			
			LogBuffer.println(".CDT saved at: " + filePath);
			
			return null;
		}

		@Override
		protected void done() {

			if (!isCancelled()) {
				ClusterView.setLoadText("Done!");
				LogBuffer.println("Done saving. Opening file.");
				visualizeData(filePath);

			} else {
				clusterView.setClustering(false);
				LogBuffer.println("Clustering was cancelled.");
			}
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
			tvController.setViewChoice(false);
			
			/* TODO can this be inherited from TVController? */
			LoadWorker loadWorker = new LoadWorker(fileSet);
			loadWorker.execute();

		} else {
			String alert = "When trying to load the clustered file, no "
					+ "file path could be found.";
			JOptionPane.showMessageDialog(JFrame.getFrames()[0], alert, 
					"Alert", JOptionPane.WARNING_MESSAGE);
			LogBuffer.println("Alert: " + alert);
		}
	}

	/**
	 * Worker thread to load data chosen by the user. If the loading is
	 * successful, it sets the dataModel in the TVFrameController and loads the
	 * DendroView. If not, it also ensures the appropriate response of 
	 * DendroView and TVFrame.
	 * TODO Implement inheritance from TVController... 
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
				tvController.setViewChoice(false);
				LogBuffer.println("Successfully loaded.");

			} else {
				String message = "No clustered data matrix could be set.";
				JOptionPane.showMessageDialog(JFrame.getFrames()[0], 
						message, "Alert", JOptionPane.WARNING_MESSAGE);
				LogBuffer.println("Alert: " + message);
				clusterView.setClustering(false);
				tvController.setViewChoice(true);
				tvController.addViewListeners();
			}
		}
	}
	
	/* ----Listeners----- */
	/**
	 * Listener listens to a change in selection for the clusterChoice
	 * JComboBox in clusterView. Calls a new layout setup as a response.
	 * @author CKeil
	 *
	 */
	private class ClusterTypeListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			LogBuffer.println("Reset cluster type.");
			clusterDialog.reset(((JComboBox<String>)arg0.getSource())
					.getSelectedIndex());
			addAllListeners();
		}
	}
	
	/**
	 * Listener listens to a change in selection for the linkChooser
	 * JComboBox in clusterView. Calls a new layout setup as a response.
	 * @author CKeil
	 *
	 */
	private class LinkChoiceListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			LogBuffer.println("Reprint link method tips.");
			clusterView.setupLayout();
		}
	}
	
	/**
	 * Listens to a change in selection in the JComboBox for row distance
	 * measure selection.
	 * @author CKeil
	 */
	private class RowDistListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent event) {
			
			if(event.getStateChange() == ItemEvent.SELECTED) {
				rowSimilarity = clusterView.getRowSimilarity();
				
				/* Ready indicator label */
				clusterView.displayReadyStatus(isReady(rowSimilarity, ROW) 
						|| isReady(colSimilarity, COL));
			}
		}
	}
	
	/**
	 * Listens to a change in selection in the JComboBox for col distance
	 * measure selection.
	 * @author CKeil
	 */
	private class ColDistListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent event) {
			
			if(event.getStateChange() == ItemEvent.SELECTED) {
				colSimilarity = clusterView.getColSimilarity();
				
				/* Ready indicator label */
				clusterView.displayReadyStatus(isReady(rowSimilarity, ROW) 
						|| isReady(colSimilarity, COL));
			}
		}
	}
	
	/**
	 * Listens to a change in selection in the JSpinners for k-means.
	 * @author CKeil
	 */
	private class SpinnerListener implements ChangeListener {

		@Override
		public void stateChanged(ChangeEvent arg0) {
			
			/* XOR controlled ready indicator label */
			clusterView.displayReadyStatus(
					(isReady(rowSimilarity, ROW) 
							&& !isReady(colSimilarity, COL)) 
					|| (!isReady(rowSimilarity, ROW) 
							&& isReady(colSimilarity, COL)));
		}
	}

	/**
	 * Defines what happens if the user clicks the 'Cancel' button in
	 * DendroView. Calls the cancel() method in the view.
	 * TODO add cancel functionality to distance worker and cluster worker.
	 * @author CKeil
	 * 
	 */
	private class CancelListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			LogBuffer.println("Trying to cancel.");
			cancelAll();
		}
	}
	
	/* --- Helper methods --- */
	/**
	 * Verifies whether all needed options are selected to 
	 * perform clustering.
	 * 
	 * TODO What if the user selects a number for 1 of the 2 spinner values
	 * for one axis, but both for the other? Cluster will still continue but
	 * only cluster the correctly set up axis. How can a warning be displayed,
	 * how can intent of the user be recognized?
	 * 
	 * @param distMeasure Selected distance measure.
	 * @return boolean Whether all needed selections have 
	 * appropriate values.
	 */
	private boolean isReady(final int distMeasure, int type) {

		if (isHierarchical()) {
			return distMeasure != DistMatrixCalculator.NO_CLUSTER;

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

			return (distMeasure != DistMatrixCalculator.NO_CLUSTER 
					&& (groups > 0 && iterations > 0));
		}
	}
	
	/**
	 * Cancels all active threads related to clustering.
	 */
	private void cancelAll() {
		
		if(clusterTask != null) clusterTask.cancel(true);
		if(processor != null) processor.cancelAll();
		if(saveTask!= null) saveTask.cancel(true);
	}

	/**
	 * Returns whether hierarchical clustering is currently selected or not.
	 * 
	 * @return boolean Whether the user selected hierarchical clustering (true)
	 * or k-means (false).
	 */
	private boolean isHierarchical() {

		return clusterView.getClusterMethod()
				.equalsIgnoreCase(StringRes.menu_Hier);
	}
}
