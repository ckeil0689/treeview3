package Cluster;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import Controllers.ClusterController;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;

/**
 * This class takes the original uploaded dataset and manipulates it 
 * according to mathematical principles of hierarchical clustering. 
 * It generates files to display dendrograms (.gtr and .atr) as well
 * as a reordered original data file (.cdt).
 * @author CKeil
 * 
 */
public class ClusterProcessor {

	private final DataModel tvModel;
	private int pBarCount;
	private DistanceWorker distTask;
	private ClusterTask clusterTask;

	/**
	 * Constructor for the ClusterProcessor. Sets the pBarCount to 0,
	 * which is the value that stores progress between multiple tasks, so that
	 * a single progress bar for the entire process can be displayed. 
	 * @param model
	 */
	public ClusterProcessor(final DataModel model) {

		this.tvModel = model;
		this.pBarCount = 0;
	}
	
	/**
	 * Starts a SwingWorker thread to do the clustering and waits for it
	 * to return a String array containing the reordered axis elements.
	 * @param distMatrix
	 * @param linkMethod
	 * @param spinnerInput
	 * @param hierarchical
	 * @param axis
	 * @return
	 */
	public String[] clusterAxis(double[][] distMatrix, String linkMethod, 
			final Integer[] spinnerInput, boolean hierarchical, 
			final int axis) {
		
		try {
			LogBuffer.println("Starting clusterAxis(): " + axis);
			this.clusterTask = new ClusterTask(distMatrix, linkMethod, 
					spinnerInput, hierarchical, axis);
			clusterTask.execute();
			
			/* 
			 * Get() blocks until this thread finishes, so the following
			 * code waits for this procedure to finish. 
		 	 */
			return clusterTask.get();
			
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			LogBuffer.logException(e);
			LogBuffer.println(e.getLocalizedMessage());
			return new String[]{"No clustered data."};
		}
	}
	
	/**
	 * Creates a SwingWorker to calculate the distance matrix for the loaded
	 * data.
	 * @param distMeasure
	 * @param axis
	 * @return
	 */
	public double[][] calcDistance(int distMeasure, int axis) {
		
		try {
			this.distTask = new DistanceWorker(distMeasure,axis);
			distTask.execute();
			
			/* 
			 * Get() blocks until this thread finishes, so the following
			 * code waits for this procedure to finish. 
		 	 */
			return distTask.get();
			
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			LogBuffer.logException(e);
			LogBuffer.println(e.getLocalizedMessage());
			return new double[][] {{0}, {0}};
		}
	}

	/**
	 * General cluster method that starts a dedicated SwingWorker method
	 * which runs the calculations in the background. This allows for 
	 * updates of the ClusterView GUI, e.g. the JProgressBar. If it 
	 * finishes after the calculations were cancelled by the user, 
	 * it let's the cluster dialog know so it can respond appropriately.
	 * Input data is translated into output data here.
	 */
	private class DistanceWorker extends SwingWorker<double[][], Integer> {

		private final int distMeasure;
		private final int axis;
		private final int axisSize;

		public DistanceWorker(final int distMeasure, final int axis) {

			this.distMeasure = distMeasure;
			this.axis = axis;
			
			if(axis == ClusterController.ROW) {
				this.axisSize = 
						((TVDataMatrix) tvModel.getDataMatrix()).getNumRow();
				
			} else {
				this.axisSize = 
						((TVDataMatrix) tvModel.getDataMatrix()).getNumCol();
			}
		}
		
		@Override
        protected void process(List<Integer> chunks) {
            
			int i = chunks.get(chunks.size()-1);
			int progress = (isCancelled()) ? 0 : pBarCount + i;
            ClusterView.updatePBar(progress);
            //ClusterView.updatePBar(pBarCount + i);
        }

		@Override
		public double[][] doInBackground() {
			
			/* Calculate distance matrix */
			double[][] data = null;
			if (axis == ClusterController.ROW) {
				data = ((TVDataMatrix)tvModel.getDataMatrix()).getExprData();

			} else {
				data = formatColData(((TVDataMatrix) tvModel.getDataMatrix())
						.getExprData());
			}

			if (data != null) {
				final DistMatrixCalculator dCalc = 
						new DistMatrixCalculator(data, distMeasure, axis);
				
				/* Ranking data if Spearman was chosen */
				if(distMeasure == DistMatrixCalculator.SPEARMAN) {
					
					double[][] rankMatrix = 
							new double[data.length][data[0].length];
					
					/* Iterate over every row of the matrix. */
					for (int i = 0; i < rankMatrix.length; i++) {
						
						if(isCancelled()) return new double[0][];
						publish(i);
						rankMatrix[i] = dCalc.spearman(data[i]);
					}
					
					dCalc.setTaskData(rankMatrix);
					
					/* Keep track of progress */
					pBarCount += axisSize;
					
					LogBuffer.println("Ranking has completed.");
				}

				try {
					/* Loop generates rows for distance matrix */
					/* take a row */
					for (int i = 0; i < data.length; i++) {
			 
						if(isCancelled()) {
							LogBuffer.println("DistTask cancelled.");
							return new double[0][];
						}
						publish(i);
						dCalc.calcRow(i);
					}
					
					return dCalc.getDistanceMatrix();
					
				} catch (NumberFormatException e) {
					String message = "Measuring the distances experienced "
							+ "an issue. Check log messages to see "
							+ "the cause.";
					
					JOptionPane.showMessageDialog(JFrame.getFrames()[0], 
							message, "Error", JOptionPane.ERROR_MESSAGE);
					LogBuffer.logException(e);
				}
			} else {
				String message = "Data could not be retrieved for "
						+ "distance calculation.";
				JOptionPane.showMessageDialog(JFrame.getFrames()[0], 
						message, "Alert", JOptionPane.WARNING_MESSAGE);
				LogBuffer.println("Alert: " + message);
			}
			
			LogBuffer.println("Distance matrix could not be calculated and"
					+ " was set to values of 0.");
			return new double[][] {{0}, {0}};
		}
		
		@Override
		public void done() {
			
			if(!isCancelled()) {
				/* keep track of overall progress */
				pBarCount += axisSize;
				LogBuffer.println("pBarCount: " + pBarCount);
			}
		}
		
		/**
		 * This method uses the unformatted matrix data list and splits 
		 * it up into the columns.
		 * 
		 * @param unformattedData The non-formatted, loaded data.
		 * @return
		 */
		public double[][] formatColData(final double[][] unformattedData) {

			final DataFormatter formattedData = new DataFormatter();

			return formattedData.splitColumns(unformattedData);
		}
	}
	
	/**
	 * General cluster method that starts a dedicated SwingWorker method
	 * which runs the calculations in the background. This allows for 
	 * updates of the ClusterView GUI, e.g. the JProgressBar. If it 
	 * finishes after the calculations were cancelled by the user, 
	 * it let's the cluster dialog know so it can respond appropriately.
	 * Input data is translated into output data here.
	 */
	private class ClusterTask extends SwingWorker<String[], Integer> {

		private final double[][] distMatrix;
		private final String linkMethod;
		private final Integer[] spinnerInput;
		private final int axis;
		private final int max;
		private boolean hierarchical;

		public ClusterTask(final double[][] distMatrix, String linkMethod, 
				final Integer[] spinnerInput, boolean hierarchical, 
				final int axis) {

			LogBuffer.println("Initializing ClusterTask.");
			this.distMatrix = distMatrix;
			this.linkMethod = linkMethod;
			this.spinnerInput = spinnerInput;
			this.hierarchical = hierarchical;
			this.axis = axis;
			this.max = distMatrix.length - 1; // 1 cluster remains
		}
		
		@Override
        protected void process(List<Integer> chunks) {
            
			int i = chunks.get(chunks.size()-1);
			int progress = (isCancelled()) ? 0 : pBarCount + i;
            ClusterView.updatePBar(progress);
        }

		@Override
		public String[] doInBackground() {
			
			/* Hierarchical */
			if (hierarchical) {
				LogBuffer.println("Starting cluster.");
				
				String fileName = tvModel.getSource().substring(0, 
						tvModel.getSource().length() - 4);
				
				HierCluster cGen = new HierCluster(fileName, linkMethod, 
								distMatrix, axis);
				
				/* 
				 * Continue process until distMatrix has a size of 1, 
				 * This array is the final cluster. Initially every row is 
				 * its own cluster (bottom-up clustering).
				 */
				int loopNum = 0;
				int distMatrixLength = distMatrix.length;
				
				while (distMatrixLength > 1 && !isCancelled()) {
					
					distMatrixLength = cGen.cluster();
					publish(loopNum);
					loopNum++;
				}
				
				LogBuffer.println("Clustering matrix done.");
				
				/* Return empty String[] if user cancels operation */
				if(isCancelled()) {
					LogBuffer.println("ClusterTask cancelled.");
					return new String[]{""};
				}
				
				/* Write the tree file */
				LogBuffer.println("Writing clustered data.");
				cGen.finish();

				return cGen.getReorderedList();
			} 
			/* K-Means */
			/*TODO Implement k-means correctly */
			else {
				KMeansCluster cGen = new KMeansCluster(tvModel, distMatrix, 
						axis, spinnerInput[0], spinnerInput[1], this);

				cGen.cluster();

				return cGen.getReorderedList();
			}
		}
		
		@Override
		public void done() {
			
			if(!isCancelled()) pBarCount += max;
			LogBuffer.println("pBarCount: " + pBarCount);
			LogBuffer.println("Clustering successful? " + !isCancelled());
		}
	}
	
	/**
	 * Cancels all currently running threads.
	 */
	public void cancelAll() {
		
		LogBuffer.println("ClusterProcessor canceling all.");
		if(distTask != null) distTask.cancel(true);
		if(clusterTask != null) clusterTask.cancel(true);
	}
}
