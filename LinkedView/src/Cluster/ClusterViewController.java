//package Cluster;
//
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.io.File;
//
//import javax.swing.JDialog;
//import javax.swing.SwingUtilities;
//import javax.swing.SwingWorker;
//
//import edu.stanford.genetics.treeview.FileSet;
//import edu.stanford.genetics.treeview.TVFrameController;
//import edu.stanford.genetics.treeview.TreeViewFrame;
//import edu.stanford.genetics.treeview.model.TVModel;
//
///**
// * Class to be used as controller in the MVC design pattern for clustergrams.
// * It handles user interactions by implementing listeners which call methods
// * from the model or view to respond.
// * 
// * @author CKeil
// *
// */
//public class ClusterViewController {
//
//	private TVModel tvModel;
//	private TreeViewFrame tvFrame;
//	private TVFrameController controller;
//	private ClusterView clusterView;
//	
//	private SwingWorker<Void, Void> clusterWorker;
//	private SwingWorker<Void, Void> loadWorker;
//	
//	private String finalFilePath;
//	private FileSet fileSet;
//	
//	public ClusterViewController(ClusterView view, TreeViewFrame tvFrame, 
//			TVFrameController controller) {
//		
//		this.tvFrame = tvFrame;
//		this.controller = controller;
//		this.tvModel = controller.getTVControllerModel();
//		this.clusterView = view;
//		
//		setupWorkerThread();
//		
//		// Add listeners after creating them
//		clusterView.addClusterListener(new ClusterListener());
//		clusterView.addClusterMenuListener(new ClusterMenuSetupListener());
//		clusterView.addCancelListener(new CancelListener());
//	}
//	
//	/**
//	 * Sets up the worker thread to start calculations without 
//	 * affecting the GUI.
//	 */
//	public void setupWorkerThread() {
//		
//		clusterWorker = new SwingWorker<Void, Void>() {
//
//			@Override
//			public Void doInBackground() {
//				
//				// Setup a ClusterProcessor
//				// final ClusterProcessor clusterTarget = 
//				// new ClusterProcessor(clusterView, tvModel);
//					
//				final ClusterProcessorArrays clusterTarget = 
//						new ClusterProcessorArrays(clusterView, tvModel);
//				
//				// Begin the actual clustering, hierarchical or kmeans
//				finalFilePath = clusterTarget.cluster(
//						isHierarchical());
//				
//				clusterView.refresh();
//				
//				return null;	
//			}
//
//			@Override
//			protected void done() {
//
////				clusterView.displayCompleted(finalFilePath);
//				visualizeData();
//			}
//		};
//	}
//	
//	public void setupLoadWorkerThread() {
//		
//		loadWorker = new SwingWorker<Void, Void>() {
//
//			@Override
//			protected Void doInBackground() throws Exception {
//				
//				controller.loadFileSet(fileSet);
//				return null;
//			}
//			
//			@Override
//			protected void done() {
//				
//				if(controller.getTVControllerModel().
//						getDataMatrix().getNumRow() > 0) {
//					controller.setDataModel();
//					controller.setViewChoice();
//					
//				} else {
//					System.out.println("No datamatrix set by worker thread.");
//					tvFrame.setView("WelcomeView");
//					controller.addViewListeners();
//				}
//			}
//		};
//	}
//	
//	// Define Listeners as inner classes
//	/**
//	 * Defines what happens if the user clicks the 'Cluster' button in
//	 * DendroView2.
//	 * @author CKeil
//	 *
//	 */
//	class ClusterListener implements ActionListener {
//		
//		public void actionPerformed(ActionEvent e) {
//			
//			// Declared inside because the choice is different every time 
//			// the cluster_button is being clicked.
//			String rowSimilarity = clusterView.getRowSimilarity();
//			String colSimilarity = clusterView.getColSimilarity();
//			String linkageMethod = clusterView.getLinkageMethod();
//			
//			// GUI changes when clustering is running
//			if (checkSelections(rowSimilarity, colSimilarity)) {
//				clusterView.displayClusterProcess(rowSimilarity, colSimilarity, 
//						linkageMethod);
//				
//				// Start the worker
//				clusterWorker.execute();
//				
//			} else {
//				clusterView.showError(isHierarchical());
//			}
//		}
//		
//		/**
//		 * Checks whether all needed options are selected to perform clustering.
//		 * 
//		 * @param choice
//		 * @param choice2
//		 * @return
//		 */
//		public boolean checkSelections(final String choice, 
//				final String choice2) {
//
//			if (isHierarchical()) {
//				if (!choice.contentEquals("Do Not Cluster")
//						|| !choice2.contentEquals("Do Not Cluster")) {
//					return true;
//
//				} else {
//					return false;
//				}
//			} else {
//				final Integer[] spinnerValues = clusterView.getSpinnerValues();
//				
//				final int clustersR = spinnerValues[0];
//				final int itsR = spinnerValues[1];
//				final int clustersC = spinnerValues[2];
//				final int itsC = spinnerValues[3];
//				
//				if ((!choice.contentEquals("Do Not Cluster") 
//						&& (clustersR > 0 && itsR > 0))
//						|| (!choice2.contentEquals("Do Not Cluster") 
//								&& (clustersC > 0 && itsC > 0))) {
//					return true;
//
//				} else {
//					return false;
//				}
//			}
//		}
//	}
//	
//	/**
//	 * Listener to interact with the viewer and switch the cluster panel 
//	 * view when a different type is selected by the user.
//	 * @author CKeil
//	 *
//	 */
//	class ClusterMenuSetupListener implements ActionListener {
//		
//		public void actionPerformed(ActionEvent e) {
//
//			clusterView.setupClusterMenu(isHierarchical());	
//		} 
//	}
//	
//	/**
//	 * Sets a new DendroView with the new data loaded into TVModel, displaying
//	 * an updated HeatMap. It should also close the ClusterViewFrame.
//	 */
//	public void visualizeData() {
//		
//		JDialog topFrame = (JDialog) SwingUtilities
//				.getWindowAncestor(clusterView);
//		
//		File file = null;
//		
//		if(finalFilePath != null) {
//			file = new File(finalFilePath);
//		
//			fileSet = new FileSet(file.getName(), file.getParent() 
//					+ File.separator);
//
//			tvFrame.setView("LoadProgressView");
//			setupLoadWorkerThread();
//			loadWorker.execute();
//			
//			topFrame.dispose();
//		
//		} else {
//			// Make Warning Dialog
//			
//		}
//	}
//	
//	/**
//	 * Defines what happens if the user clicks the 'Cancel' button in
//	 * DendroView2. Calls the cancel() method in the view.
//	 * @author CKeil
//	 *
//	 */
//	class CancelListener implements ActionListener {
//		
//		public void actionPerformed(ActionEvent e) {
//			
//			clusterWorker.cancel(true);
//			clusterView.cancel();
//		}
//	}
//	
//	/**
//	 * Returns the choice of the cluster drop down menu as a string
//	 * 
//	 * @return
//	 */
//	public boolean isHierarchical() {
//
//		boolean hierarchical = false;
//		final String choice = clusterView.getClusterMethod();
//		hierarchical = choice.equalsIgnoreCase("Hierarchical Clustering");
//		return hierarchical;
//	}
//}
