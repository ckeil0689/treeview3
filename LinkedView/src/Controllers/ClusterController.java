package Controllers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

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
	private SwingWorker<String[], Integer> clusterWorker;
	private SwingWorker<Void, Void> saveWorker;

	private String[] reorderedRows;
	private String[] reorderedCols;

	private String finalFilePath;
	private FileSet fileSet;

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

			final ClusterProcessWorker clusterProcess = 
					new ClusterProcessWorker();
			
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
		
		@Override
        protected void process(List<String> chunks) {
            
			String s = chunks.get(chunks.size()-1);
            ClusterView.setLoadText(s);
        }
		
		@Override
		protected Void doInBackground() throws Exception {
			
			/* Get chosen similarity options. */
			String rowSimilarity = clusterView.getRowSimilarity();
			String colSimilarity = clusterView.getColSimilarity();

			/* If no options are selected, display error. */
			if (!checkSelections(rowSimilarity, ROW) 
					&& !checkSelections(colSimilarity, COL)) {
				clusterView.displayErrorLabel();
				return null;
			}
			
			clusterView.setClustering(true);
			
			/* ProgressBar maximum */
//			ClusterView.setPBarMax();
			
			/* When done, start distance measure */
			ClusterProcessor processor = new ClusterProcessor(tvModel);
			
			/* Row axis cluster */
			double[][] distMatrix;
			if (checkSelections(rowSimilarity, ROW)) {
				/* ProgressBar label */
				publish("Calculating row distances...");
				distMatrix = processor.calcDistance(rowSimilarity, ROW);
				
				if(distMatrix == null) return null;
				
				publish("Clustering row data...");
				
				reorderedRows = processor.clusterAxis(distMatrix, 
						clusterView.getLinkageMethod(), 
						clusterView.getSpinnerValues(), isHierarchical(), ROW);
			}
			
			/* Column axis cluster */
			if (checkSelections(colSimilarity, COL)) {
				/* ProgressBar label */
				publish("Calculating column distances...");
				distMatrix = processor.calcDistance(colSimilarity, COL);
				
				if(distMatrix == null) return null;
				
				publish("Clustering column data...");
				
				reorderedCols = processor.clusterAxis(distMatrix, 
						clusterView.getLinkageMethod(), 
						clusterView.getSpinnerValues(), isHierarchical(), COL);
			}
			
			return null;
		}
		
		@Override
		public void done() {
			
			saveClusterFile();
		}
	}

	/**
	 * Saves the clustering output (reordered axes) to a new CDT file, 
	 * so it can later be loaded and displayed.
	 */
	public void saveClusterFile() {

		if (reorderedRows != null || reorderedCols != null) {
			ClusterView.setLoadText("Saving...");
			saveWorker = new SaveCDTWorker();
			saveWorker.execute();
			LogBuffer.println("Saving started.");
			
		} else {
			String message = "Cannot save. No clustered data was created.";
			JOptionPane.showMessageDialog(
					clusterDialog.getParentFrame(), message, "Error", 
					JOptionPane.ERROR_MESSAGE);
			LogBuffer.println("Alert: " + message);
		}
	}
		
	/**
	 * Worker with the task to generate and write a new CDT file which 
	 * contains the newly clustered matrix. 
	 * Makes sure that the newly created file is visualized right after 
	 * clustering, given that the process was not cancelled.
	 * @author CKeil
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
				ClusterView.setLoadText("Done!");
				visualizeData();

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
	public boolean checkSelections(final String distMeasure, int type) {

		if (isHierarchical()) {
			return !distMeasure.contentEquals(StringRes.cluster_DoNot);

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

			return (!distMeasure.contentEquals(StringRes.cluster_DoNot) 
					&& (groups > 0 && iterations > 0));
		}
	}

	/**
	 * Worker thread to load data chosen by the user. If the loading is
	 * successful, it sets the dataModel in the TVFrameController and loads the
	 * DendroView. If not, it also ensures the appropriate response of 
	 * DendroView and TVFrame.
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
				String message = "No clustered data matrix could be set.";
				JOptionPane.showMessageDialog(clusterDialog.getParentFrame(), 
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
	 * @return boolean Whether the user selected hierarchical clustering (true)
	 * or k-means (false).
	 */
	public boolean isHierarchical() {

		return clusterView.getClusterMethod()
				.equalsIgnoreCase(StringRes.menu_Hier);
	}
}
