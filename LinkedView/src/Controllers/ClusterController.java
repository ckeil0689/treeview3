package Controllers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import Utilities.ErrorDialog;
import Utilities.StringRes;
import Cluster.CDTGenerator;
import Cluster.ClusterProcessor;
import Cluster.ClusterView;
import Cluster.ClusterViewDialog;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;

/**
 * Controls clustering according to user choices in ClusterView. It
 * handles user interactions by implementing listeners which in turn call 
 * methods from the model or view to respond.
 * 
 * @author CKeil
 * 
 */
public class ClusterController {

	private final DataModel tvModel;
	private final TVFrameController controller;
	private final ClusterView clusterView;
	private final ClusterViewDialog clusterDialog;

	private SwingWorker<Void, Void> loadWorker;
	private SwingWorker<String[], Void> clusterWorker;
	private SwingWorker<Void, Void> saveWorker;

	private String[] orderedRows;
	private String[] orderedCols;

	private String finalFilePath;
	private FileSet fileSet;

	public ClusterController(final ClusterViewDialog dialog, 
			final TVFrameController controller) {

		this.clusterDialog = dialog;
		this.controller = controller;
		this.tvModel = controller.getDataModel();
		this.clusterView = dialog.getClusterView();

		// Add listeners after creating them
		clusterView.addClusterListener(new ClusterListener());
		clusterView.addClusterTypeListener(new ClusterTypeListener());
		clusterView.addCancelListener(new CancelListener());
		clusterView.addClusterChoiceListener(new ClusterChoiceListener());
	}

	class ClusterProcessWorker extends SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() throws Exception {

			// Declared inside because the choice is different every time
			// the cluster_button is being clicked.
			final String rowSimilarity = clusterView.getRowSimilarity();
			final String colSimilarity = clusterView.getColSimilarity();
//			final String linkageMethod = clusterView.getLinkageMethod();

			// GUI changes when clustering is running
			if (checkSelections(rowSimilarity, colSimilarity)) {
//				clusterView.displayClusterProcess(rowSimilarity, colSimilarity,
//						linkageMethod);
				clusterView.setClustering(true);

				// Start the workers
				if (!rowSimilarity.equalsIgnoreCase(StringRes.cluster_DoNot)) {
					orderedRows = cluster(rowSimilarity, "Row");
				}

				if (!colSimilarity.equalsIgnoreCase(StringRes.cluster_DoNot)) {
					orderedCols = cluster(colSimilarity, "Column");
				}
			} else {
				clusterView.displayErrorLabel();
			}
			return null;
		}
		
		@Override
		public void done() {
			
			save();
		}

		/**
		 * General cluster method that starts a dedicated SwingWorker method
		 * which runs the calculations in the background. This allows for 
		 * updates of the ClusterView GUI, e.g. the JProgressBar.
		 * @param choice The selected clustering method.
		 * @param type The currently active clustering type.
		 * @return
		 */
		public String[] cluster(final String choice, final String type) {

			clusterWorker = new MyClusterWorker(choice, type);
			clusterWorker.execute();
			LogBuffer.println(type + " Clustering started.");

			try {
				return clusterWorker.get();

			} catch (final InterruptedException e) {
				ErrorDialog error = new ErrorDialog(
						clusterDialog.getParentFrame(), 
						"Clustering was interrupted.");
				error.setVisible(true);
				LogBuffer.println(e.getMessage());
				return null;

			} catch (final ExecutionException e) {
				ErrorDialog error = new ErrorDialog(
						clusterDialog.getParentFrame(), 
						"Clustering experienced an issue with code"
						+ "execution.");
				error.setVisible(true);
				LogBuffer.println(e.getMessage());
				return null;
			}
		}

		public void save() {

			if (orderedRows != null || orderedCols != null) {
				saveWorker = new SaveCDTWorker();
				saveWorker.execute();
				LogBuffer.println("Saving started.");
			}
		}

		/**
		 * Verifies whether all needed options are selected 
		 * to perform clustering.
		 * 
		 * @param choiceRow Selected method for row clustering.
		 * @param choiceCol Selected method for column clustering.
		 * @return
		 */
		public boolean checkSelections(final String choiceRow, 
				final String choiceCol) {

			if (isHierarchical()) {
				return (!choiceRow.contentEquals(StringRes.cluster_DoNot) 
						|| !choiceCol.contentEquals(StringRes.cluster_DoNot));

			} else {
				final Integer[] spinnerValues = clusterView.getSpinnerValues();

				final int clustersR = spinnerValues[0];
				final int itsR = spinnerValues[1];
				final int clustersC = spinnerValues[2];
				final int itsC = spinnerValues[3];

				return (!choiceRow.contentEquals(StringRes.cluster_DoNot) 
						&& (clustersR > 0 && itsR > 0))
						|| (!choiceCol.contentEquals(StringRes.cluster_DoNot) 
								&& (clustersC > 0 && itsC > 0));
			}
		}
	}

	/**
	 * Sets up the worker thread to start calculations without affecting the
	 * GUI.
	 */
	private class MyClusterWorker extends SwingWorker<String[], Void> {

		private final String choice;
		private final String type;

		public MyClusterWorker(final String choice, final String type) {

			this.choice = choice;
			this.type = type;
		}

		@Override
		public String[] doInBackground() {

			final ClusterProcessor clusterTarget = 
					new ClusterProcessor(clusterView, tvModel, this);

			String[] reorderedElements = null;

			String typeHeader = "";
			if (type.equalsIgnoreCase("Row")) {
				typeHeader = "GENE";

			} else if (type.equalsIgnoreCase("Column")) {
				typeHeader = "ARRY";

			} else {
				LogBuffer.println("Invalid type name for clustering.");
			}

			final double[][] distances = clusterTarget.calculateDistance(
					choice, type);

			if (!isCancelled() || distances != null) {
				if (isHierarchical()) {
					reorderedElements = clusterTarget.hCluster(distances,
							typeHeader);

				} else {
					final Integer[] spinnerInput = clusterView
							.getSpinnerValues();

					reorderedElements = clusterTarget.kmCluster(distances,
							typeHeader, spinnerInput[0], spinnerInput[1]);
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
			} catch (InterruptedException e) {
				ErrorDialog error = new ErrorDialog(
						clusterDialog.getParentFrame(), 
						"Cancelling cluster was interrupted.");
				error.setVisible(true);		
				LogBuffer.println(e.getMessage());
				clusterView.cancel();
				
			} catch (ExecutionException e) {
				ErrorDialog error = new ErrorDialog(
						clusterDialog.getParentFrame(), 
						"Cancelling cluster experienced an issue with"
						+ "code execution.");
				error.setVisible(true);
				LogBuffer.println(e.getMessage());
				clusterView.cancel();
			}
		}
	}

	class SaveCDTWorker extends SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() throws Exception {

			if (!isCancelled()) {
				final double[][] dataArrays = ((TVDataMatrix) tvModel
						.getDataMatrix()).getExprData();

				final CDTGenerator cdtGen = new CDTGenerator(
						tvModel, clusterView, dataArrays, orderedRows,
						orderedCols, isHierarchical());

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

			controller.loadFileSet(fileSet);
			return null;
		}

		@Override
		protected void done() {

			if (controller.getDataModel().getDataMatrix()
					.getNumRow() > 0) {
				controller.setDataModel();
				controller.setViewChoice();

			} else {
				LogBuffer.println("No datamatrix set by worker thread.");
				controller.setViewChoice();
				controller.addViewListeners();
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
//			clusterDialog.packDialog();
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

		final JDialog topFrame = (JDialog) SwingUtilities
				.getWindowAncestor(clusterView.getMainPanel());

		File file = null;

		if (finalFilePath != null) {
			file = new File(finalFilePath);

			fileSet = new FileSet(file.getName(), file.getParent()
					+ File.separator);

			controller.setViewChoice();
			loadWorker = new LoadWorker();
			loadWorker.execute();

			topFrame.dispose();

		} else {
			// Make Warning Dialog

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
	 * Returns the choice of the cluster drop down menu as a string
	 * 
	 * @return
	 */
	public boolean isHierarchical() {

		boolean hierarchical = false;
		final String choice = clusterView.getClusterMethod();
		hierarchical = choice.equalsIgnoreCase(StringRes.menu_Hier);
		return hierarchical;
	}
}
