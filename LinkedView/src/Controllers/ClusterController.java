package Controllers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import Utilities.AlertDialog;
import Utilities.ErrorDialog;
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
	
	// Axes identifiers.
	public final static int ROW = 1;
	public final static int COL = 2;

	private final DataModel tvModel;
	private final TVFrameController tvController;
	private final ClusterView clusterView;
	private final ClusterViewDialog clusterDialog;

	private SwingWorker<Void, Void> loadWorker;
	private SwingWorker<String[], Void> clusterWorker;
	private SwingWorker<Void, Void> saveWorker;

	private String[] reorderedRows;
	private String[] reorderedCols;

	private String finalFilePath;
	private FileSet fileSet;

	public ClusterController(final ClusterViewDialog dialog, 
			final TVFrameController controller) {

		this.clusterDialog = dialog;
		this.tvController = controller;
		this.tvModel = controller.getDataModel();
		this.clusterView = dialog.getClusterView();

		// Add listeners after creating them
		clusterView.addClusterListener(new ClusterListener());
		clusterView.addClusterTypeListener(new ClusterTypeListener());
		clusterView.addCancelListener(new CancelListener());
		clusterView.addClusterChoiceListener(new ClusterChoiceListener());
	}

	/**
	 * Processes clustering in the background according to user input. Allows
	 * for the UI to remain responsive during these tasks. This is
	 * useful for updating the progress bar and progress label.
	 * @author CKeil
	 *
	 */
	class ClusterProcessWorker extends SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() throws Exception {

			// Get chosen similarity options.
			final String rowSimilarity = clusterView.getRowSimilarity();
			final String colSimilarity = clusterView.getColSimilarity();

			if (!checkSelections(rowSimilarity, ROW) 
					&& !checkSelections(colSimilarity, COL)) {
				clusterView.displayErrorLabel();
				return null;
			}
			
			if (checkSelections(rowSimilarity, ROW)) {
				reorderedRows = cluster(rowSimilarity, ROW);
			}

			if (checkSelections(colSimilarity, COL)) {
				reorderedCols = cluster(colSimilarity, COL);
			}
			
			return null;
		}
		
		@Override
		public void done() {
			
			saveClusterFile();
		}

		/**
		 * Tells the ClusterWorker to begin executing. Waits for the return
		 * of reordered matrix elements for the chosen axis. 
		 * @param distMeasure The selected clustering method.
		 * @param axis The currently active clustering type.
		 * @return
		 */
		public String[] cluster(final String distMeasure, final int axis) {
			
			clusterWorker = new MyClusterWorker(distMeasure, axis);
			clusterWorker.execute();
			LogBuffer.println("Clustering started.");

			try {
				return clusterWorker.get();

			} catch (InterruptedException | ExecutionException e) {
				ErrorDialog.showError("Clustering was interrupted.", e);
				return null;
			}
		}

		/**
		 * Saves the clustered matrix axes to a new CDT file, so it can be
		 * loaded and displayed.
		 */
		public void saveClusterFile() {

			if (reorderedRows != null || reorderedCols != null) {
				saveWorker = new SaveCDTWorker();
				saveWorker.execute();
				LogBuffer.println("Saving started.");
				
			} else {
				AlertDialog.showAlert("Cannot save the file, because neither"
						+ "rows nor columns were properly clustered.");
			}
		}

		/**
		 * Verifies whether all needed options are selected to 
		 * perform clustering.
		 * 
		 * @param dMeasure Selected distance measure.
		 * @return boolean 
		 */
		public boolean checkSelections(final String dMeasure, int type) {

			if (isHierarchical()) {
				return !dMeasure.contentEquals(StringRes.cluster_DoNot);

			} else {
				final Integer[] spinnerValues = clusterView.getSpinnerValues();

				int groups;
				int iterations;
				
				switch(type) {
				
				case ROW: 	groups = spinnerValues[0]; 
							iterations = spinnerValues[1];
							break;
				case COL: 	groups = spinnerValues[2]; 
							iterations = spinnerValues[3];
							break;
				default:	groups = 0;
							iterations = 0;
							break;
				}

				return (!dMeasure.contentEquals(StringRes.cluster_DoNot) 
						&& (groups > 0 && iterations > 0));
			}
		}
	}

	/**
	 * General cluster method that starts a dedicated SwingWorker method
	 * which runs the calculations in the background. This allows for 
	 * updates of the ClusterView GUI, e.g. the JProgressBar.
	 */
	private class MyClusterWorker extends SwingWorker<String[], Void> {

		private final String distMeasure;
		private final int axis;

		public MyClusterWorker(final String distMeasure, final int axis) {

			this.distMeasure = distMeasure;
			this.axis = axis;
		}

		@Override
		public String[] doInBackground() {

			final ClusterProcessor processor = 
					new ClusterProcessor(clusterView, tvModel, this);

			String[] reorderedElements = null;

			final double[][] distances = processor.calculateDistance(
					distMeasure, axis);

			if (!isCancelled() || distances != null) {
				if (isHierarchical()) {
					reorderedElements = processor.hCluster(distances,
							axis);

				} else {
					final Integer[] spinnerInput = clusterView
							.getSpinnerValues();

					reorderedElements = processor.kmCluster(distances,
							axis, spinnerInput[0], spinnerInput[1]);
				}
			}

			return reorderedElements;
		}
		
		@Override
		public void done() {
			
			try {
				if(isCancelled() || get() == null) {
					clusterView.cancel();
				}
			} catch (InterruptedException | ExecutionException e) {
				ErrorDialog.showError("Cancelling cluster was interrupted.", e);
				clusterView.cancel();
			}
		}
	}

	/**
	 * Worker with the task to generate and write a new CDT file which contains
	 * the newly clustered matrix.  
	 * @author CKeil
	 *
	 */
	class SaveCDTWorker extends SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() throws Exception {

			if (!isCancelled()) {
				final CDTGenerator cdtGen = new CDTGenerator(
						tvModel, clusterView, reorderedRows, reorderedCols, 
						isHierarchical());

				cdtGen.generateCDT();

				finalFilePath = cdtGen.getFilePath();
			}
			return null;
		}

		@Override
		protected void done() {

			if (!isCancelled()) {
				visualizeData();

			} else {
				clusterView.cancel();
				LogBuffer.println("Clustering has been cancelled.");
			}
		}
	}

	/**
	 * Worker thread to load data chosen by the user. If the loading is
	 * successful, it sets the dataModel in the TVFrameController and loads the
	 * DendroView.
	 */
	private class LoadWorker extends SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() throws Exception {

			tvController.loadFileSet(fileSet);
			return null;
		}

		@Override
		protected void done() {

			if (tvController.getDataModel().getDataMatrix()
					.getNumRow() > 0) {
				tvController.setDataModel();
				tvController.setViewChoice();
				clusterDialog.dispose();

			} else {
				AlertDialog.showAlert("No clustered data matrix could be set.");
				tvController.setViewChoice();
				tvController.addViewListeners();
			}
		}
	}

	// Define Listeners as inner classes
	/**
	 * Defines what happens if the user clicks the 'Cluster' button in
	 * DendroView2.
	 * 
	 * @author CKeil
	 * 
	 */
	class ClusterListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			final ClusterProcessWorker clusterProcess = 
					new ClusterProcessWorker();
			clusterProcess.execute();
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
			
			clusterView.setupLayout();
		}
	}

	/**
	 * Sets a new DendroView with the new data loaded into TVModel, displaying
	 * an updated HeatMap. It should also close the ClusterViewFrame.
	 */
	public void visualizeData() {

		File file = null;

		if (finalFilePath != null) {
			file = new File(finalFilePath);

			fileSet = new FileSet(file.getName(), file.getParent()
					+ File.separator);

			tvController.setViewChoice();
			loadWorker = new LoadWorker();
			loadWorker.execute();

		} else {
			String alert = "When trying to load the clustered file, no"
					+ "no file path could be found.";
			AlertDialog.showAlert(alert);
		}
	}

	/**
	 * Defines what happens if the user clicks the 'Cancel' button in
	 * DendroView2. Calls the cancel() method in the view.
	 * 
	 * @author CKeil
	 * 
	 */
	class CancelListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			if(clusterWorker != null) {
				clusterWorker.cancel(true);
				clusterWorker = null;
			}
		}
	}

	/**
	 * Returns whether hierarchical clustering is currently selected or not.
	 * 
	 * @return boolean
	 */
	public boolean isHierarchical() {

		return clusterView.getClusterMethod()
				.equalsIgnoreCase(StringRes.menu_Hier);
	}
}
