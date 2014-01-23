package Cluster;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

import edu.stanford.genetics.treeview.model.TVModel;

/**
 * Class to be used as controller in the MVC design pattern for clustergrams.
 * It handles user interactions by implementing listeners which call methods
 * from the model or view to respond.
 * 
 * @author CKeil
 *
 */
public class ClusterViewController {

	private TVModel tvModel;
	private ClusterView clusterView;
	private SwingWorker<Void, Void> worker;
	
	public ClusterViewController(TVModel model, ClusterView view) {
		
		this.tvModel = model;
		this.clusterView = view;
		
		setupWorkerThread();
		
		// Add listeners after creating them
		clusterView.addClusterListener(new ClusterListener());
		clusterView.addVisualizeListener(new VisualizeListener());
		clusterView.addClusterMenuListener(new ClusterMenuSetupListener());
		clusterView.addCancelListener(new CancelListener());
	}
	
	/**
	 * Sets up the worker thread to start calculations without 
	 * affecting the GUI.
	 */
	public void setupWorkerThread() {
		
		worker = new SwingWorker<Void, Void>() {

			@Override
			public Void doInBackground() {

//				int row_clusterN = 0;
//				int col_clusterN = 0;
//				int row_iterations = 0;
//				int col_iterations = 0;
				
				try {
//					Integer[] spinnerValues = clusterView.getSpinnerValues();
//					
//					// Set integers only if KMeans options are shown
//					if (!isHierarchical()) {
//						row_clusterN = spinnerValues[0];
//						col_clusterN = spinnerValues[1];
//
//						row_iterations = spinnerValues[2];
//						col_iterations = spinnerValues[3];
//					}
//					
//					JProgressBar pBar = clusterView.getProgressBars()[0];
//					JProgressBar pBar2 = clusterView.getProgressBars()[1];
//					JProgressBar pBar3 = clusterView.getProgressBars()[2];
//					JProgressBar pBar4 = clusterView.getProgressBars()[3];

					// Setup a ClusterProcessor
					final ClusterProcessor clusterTarget = 
							new ClusterProcessor(clusterView, tvModel);
//						, pBar, pBar2, pBar3, pBar4, 
//									clusterView.getLinkageMethod(), 
//									row_clusterN, row_iterations, col_clusterN, 
//									col_iterations);

					// Begin the actual clustering, hierarchical or kmeans
					clusterTarget.cluster(isHierarchical());

				} catch (final InterruptedException e) {

				} catch (final ExecutionException e) {

				}

				return null;
			}

			@Override
			protected void done() {

				clusterView.displayCompleted();
			}
		};
	}
	
	// Define Listeners as inner classes
	/**
	 * Defines what happens if the user clicks the 'Cluster' button in
	 * DendroView2.
	 * @author CKeil
	 *
	 */
	class ClusterListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			
			// Declared inside because the choice is different every time 
			// the cluster_button is being clicked.
			String rowSimilarity = clusterView.getRowSimilarity();
			String colSimilarity = clusterView.getColSimilarity();
			String linkageMethod = clusterView.getLinkageMethod();
			
			// GUI changes when clustering is running
			if (checkSelections(rowSimilarity, colSimilarity)) {
				clusterView.displayClusterProcess(rowSimilarity, colSimilarity, 
						linkageMethod);
				
				// Start the worker
				worker.execute();
				
			} else {
				clusterView.showError(isHierarchical());
			}
		}
		
		/**
		 * Checks whether all needed options are selected to perform clustering.
		 * 
		 * @param choice
		 * @param choice2
		 * @return
		 */
		public boolean checkSelections(final String choice, 
				final String choice2) {

			if (isHierarchical()) {
				if (!choice.contentEquals("Do Not Cluster")
						|| !choice2.contentEquals("Do Not Cluster")) {
					return true;

				} else {
					return false;
				}
			} else {
				final Integer[] spinnerValues = clusterView.getSpinnerValues();
				
				final int clustersR = spinnerValues[0];
				final int itsR = spinnerValues[1];
				final int clustersC = spinnerValues[2];
				final int itsC = spinnerValues[3];
				
				if ((!choice.contentEquals("Do Not Cluster") 
						&& (clustersR > 0 && itsR > 0))
						|| (!choice2.contentEquals("Do Not Cluster") 
								&& (clustersC > 0 && itsC > 0))) {
					return true;

				} else {
					return false;
				}
			}
		}
	}
	
	/**
	 * Listener to interact with the viewer and switch the cluster panel 
	 * view when a different type is selected by the user.
	 * @author CKeil
	 *
	 */
	class ClusterMenuSetupListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {

			clusterView.setupClusterMenu(isHierarchical());	
		} 
	}
	
	/**
	 * A listener for dendro_button in ClusterView that sets up the new 
	 * DendroView2 with the recently clustered file by calling setLoaded() in 
	 * TreeViewFrame.
	 * @author CKeil
	 *
	 */
	class VisualizeListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			
			clusterView.visualizeData();
		}
		
	}
	
	/**
	 * Defines what happens if the user clicks the 'Cancel' button in
	 * DendroView2. Calls the cancel() method in the view.
	 * @author CKeil
	 *
	 */
	class CancelListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			
			worker.cancel(true);
			clusterView.cancel();
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
		hierarchical = choice.equalsIgnoreCase("Hierarchical Clustering");
		return hierarchical;
	}
}
