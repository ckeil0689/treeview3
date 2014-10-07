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

	/**
	 * 
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
			ClusterWorker clusterWorker = new ClusterWorker(distMatrix, 
					linkMethod, spinnerInput, hierarchical, axis);
			clusterWorker.execute();
			
			return clusterWorker.get();
			
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
			DistanceWorker distWorker = new DistanceWorker(distMeasure,axis);
			distWorker.execute();
			
			return distWorker.get();
			
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
	 * TODO Take switch statement outside the loop.
	 */
	class DistanceWorker extends SwingWorker<double[][], Integer> {

		private final int distMeasure;
		private final int axis;
		private final int max;

		public DistanceWorker(final int distMeasure, final int axis) {

			this.distMeasure = distMeasure;
			this.axis = axis;
			
			String axisPrefix = "N/A";
			if(axis == ClusterController.ROW) {
				this.max = ((TVDataMatrix) tvModel.getDataMatrix()).getNumRow();
				axisPrefix = "row";
				
			} else {
				this.max = ((TVDataMatrix) tvModel.getDataMatrix()).getNumCol();
				axisPrefix = "column";
			}
			
			LogBuffer.println("Max of " + axisPrefix + ": " + max);
			
			ClusterView.setLoadText("Calculating " + axisPrefix 
					+ " Distance Matrix...");
		}
		
		@Override
        protected void process(List<Integer> chunks) {
            
			int i = chunks.get(chunks.size()-1);
            ClusterView.updatePBar(pBarCount + i);
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

				try {
					String axisPrefix = 
							(axis == ClusterController.ROW) ? "Row" : "Column";
					ClusterView.setLoadText("Calculating " + axisPrefix 
							+ " Distance Matrix...");
					
					// Choice of method here instead of in loop?
					
					/* Loop generates rows for distance matrix */
					/* take a row */
					for (int i = 0; i < data.length; i++) {
			 
						if(isCancelled()) break;
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
			
			/* keep track of overall progress */
			pBarCount += max;
			LogBuffer.println("pBarCount: " + pBarCount);
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
	class ClusterWorker extends SwingWorker<String[], Integer> {

		private final double[][] distMatrix;
		private final String linkMethod;
		private final Integer[] spinnerInput;
		private final int axis;
		private final int max;
		private boolean hierarchical;

		public ClusterWorker(final double[][] distMatrix, String linkMethod, 
				final Integer[] spinnerInput, boolean hierarchical, 
				final int axis) {

			LogBuffer.println("Initializing ClusterWorker");
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
            ClusterView.updatePBar(pBarCount + i);
        }

		@Override
		public String[] doInBackground() {
			
			/* Hierarchical */
			if (hierarchical) {
				LogBuffer.println("Getting source file name.");
				
				String fileName = tvModel.getSource().substring(0, 
						tvModel.getSource().length() - 4);
				
				HierCluster cGen = new HierCluster(fileName, linkMethod, 
								distMatrix, axis);
				
				LogBuffer.println("Starting cluster.");
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
				
				/* Return empty String[] if user cancels operation */
				if(isCancelled()) return new String[]{""};
				
				/* Write the tree file */
				LogBuffer.println("Writing clustered data.");
				cGen.finish();

				return cGen.getReorderedList();
			} 
			/* K-Means */
			else {
				KMeansCluster cGen = new KMeansCluster(tvModel, distMatrix, 
						axis, spinnerInput[0], spinnerInput[1], this);

				cGen.cluster();

				return cGen.getReorderedList();
			}
		}
		
		@Override
		public void done() {
			
			pBarCount += max;
			LogBuffer.println("pBarCount: " + pBarCount);
			if (isCancelled()) {
				LogBuffer.println("Clustering has been cancelled.");

			} else {
				LogBuffer.println("Cluster successfully completed.");
			}
		}
	}
}
