package Cluster;

import java.awt.Frame;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import Controllers.ClusterDialogController;
import Views.ClusterView;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.IntLabelInfo;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;

/**
 * This class takes the original uploaded dataset and manipulates it according
 * to mathematical principles of hierarchical clustering. It generates files to
 * display dendrograms (.gtr and .atr) as well as a reordered original data file
 * (.cdt).
 */
public class ClusterProcessor {

	private final TVDataMatrix originalMatrix;
	private final IntLabelInfo rowLabelI;
	private final IntLabelInfo colLabelI;

	private int pBarCount;
	private DistanceWorker distTask;
	private Clusterer clusterer;

	/**
	 * Hierarchical Clustering constructor for the ClusterProcessor. 
	 * LabelInfo not needed for this version of clustering.
	 * Sets the pBarCount to 0, which is the value that stores progress 
	 * between multiple tasks, so that a single progress bar for the entire 
	 * process can be displayed.
	 *
	 * @param dataMatrix The original data matrix to be clustered.
	 */
	public ClusterProcessor(final TVDataMatrix dataMatrix) {

		this.originalMatrix = dataMatrix;
		this.rowLabelI = null;
		this.colLabelI = null;
		this.pBarCount = 0;
	}
	
	/**
	 * K-Means constructor for the ClusterProcessor. 
	 * LabelInfo not needed for this version of clustering.
	 * Sets the pBarCount to 0, which is the value that stores progress 
	 * between multiple tasks, so that a single progress bar for the entire 
	 * process can be displayed.
	 *
	 * @param dataMatrix The original data matrix to be clustered.
	 * @param fileName The name of the file to which the data matrix belongs.
	 * @param rowLabelI The row LabelInfo object.
	 * @param colLabelI The column LabelInfo object.
	 */
	public ClusterProcessor(final TVDataMatrix dataMatrix,
			final String fileName, final IntLabelInfo rowLabelI,
			final IntLabelInfo colLabelI) {

		this.originalMatrix = dataMatrix;
		this.rowLabelI = rowLabelI;
		this.colLabelI = colLabelI;
		this.pBarCount = 0;
	}

	/**
	 * Starts a SwingWorker thread to calculate the distance matrix for the
	 * loaded data.
	 *
	 * @param distMeasure
	 * @param axis
	 * @return m * m distance matrix where m is the clustered axis length of the
	 *         original data matrix.
	 */
	public double[][] calcDistance(final int distMeasure, final int axis) {

		try {
			this.distTask = new DistanceWorker(distMeasure, axis);
			distTask.execute();

			/*
			 * Get() blocks until this thread finishes, so the following code
			 * waits for this procedure to finish.
			 */
			return distTask.get();

		} catch (InterruptedException | ExecutionException e) {
			LogBuffer.logException(e);
			LogBuffer.println(e.getLocalizedMessage());
			return new double[][] { { 0 }, { 0 } };
		}
	}

	/**
	 * Starts a SwingWorker thread to do the clustering and waits for it to
	 * return a String array containing the reordered axis elements.
	 *
	 * @param distMatrix - The distance matrix calculated for clustering
	 * @param linkMethod - The cluster linkage method to be employed
	 * @param spinnerInput - The input values from the GUI spinners (Kmeans)
	 * @param hierarchical - Whether we perform hierarchical clustering or Kmeans
	 * @param axisID - The matrix axis to be clustered
	 * @return ClusteredAxisData object which will store clustering results
	 */
	public ClusteredAxisData clusterAxis(final DistanceMatrix distMatrix,
			final int linkMethod, final Integer[] spinnerInput,
			final boolean hierarchical, final int axisID) {

		try {
			this.clusterer = new Clusterer(distMatrix, linkMethod,
					spinnerInput, hierarchical, axisID);
			clusterer.execute();

			/*
			 * Get() blocks until this thread finishes, so the following code
			 * waits for this procedure to finish.
			 */
			return clusterer.get();

		} catch (InterruptedException | ExecutionException e) {
			LogBuffer.logException(e);
			LogBuffer.println("Interrupted when clustering.");
			ClusteredAxisData cad = new ClusteredAxisData(axisID);
			cad.shouldReorderAxis(false);
			cad.setAxisClustered(false);
			return cad;
		}
	}

	/**
	 * General cluster method that starts a dedicated SwingWorker method which
	 * runs the calculations in the background. This allows for updates of the
	 * ClusterView GUI, e.g. the JProgressBar. If it finishes after the
	 * calculations were cancelled by the user, it let's the cluster dialog know
	 * so it can respond appropriately. Input data is translated into output
	 * data here.
	 */
	private class DistanceWorker extends SwingWorker<double[][], Integer> {

		private final int distMeasure;
		private final int axis;
		private final int axisSize;

		public DistanceWorker(final int distMeasure, final int axis) {

			this.distMeasure = distMeasure;
			this.axis = axis;

			if (axis == ClusterDialogController.ROW_IDX) {
				this.axisSize = originalMatrix.getNumRow();

			} else {
				this.axisSize = originalMatrix.getNumCol();
			}
		}

		@Override
		protected void process(final List<Integer> chunks) {

			final int i = chunks.get(chunks.size() - 1);
			final int progress = (isCancelled()) ? 0 : pBarCount + i;
			ClusterView.updatePBar(progress);
		}

		@Override
		public double[][] doInBackground() {

			/* Calculate distance matrix */
			double[][] data = null;
			if (axis == ClusterDialogController.ROW_IDX) {
				data = originalMatrix.getExprData();

			} else {
				data = formatColData(originalMatrix.getExprData());
			}

			if (data != null && !isCancelled()) {
				final DistMatrixCalculator dCalc = new DistMatrixCalculator(
						data, distMeasure, axis);

				/* Ranking data if Spearman was chosen */
				if (distMeasure == DistMatrixCalculator.SPEARMAN) {
					final double[][] rankMatrix = 
							new double[data.length][data[0].length];

					/* Iterate over every row of the matrix. */
					for (int i = 0; i < rankMatrix.length; i++) {

						if (isCancelled()) {
							return new double[0][];
						}
						
						publish(i);
						rankMatrix[i] = dCalc.spearman(data[i]);
					}

					dCalc.setTaskData(rankMatrix);

					/* Keep track of progress */
					pBarCount += axisSize;
				}

				try {
					/* Loop generates rows for distance matrix */
					/* take a row */
					for (int i = 0; i < data.length; i++) {

						if (isCancelled()) {
							return new double[0][];
						}
						publish(i);
						dCalc.calcRow(i);
					}

					return dCalc.getDistanceMatrix();

				} catch (final NumberFormatException e) {
					final String message = "Measuring the distances experienced "
							+ "an issue. Check log messages to see "
							+ "the cause.";

					JOptionPane.showMessageDialog(Frame.getFrames()[0],
							message, "Error", JOptionPane.ERROR_MESSAGE);
					LogBuffer.logException(e);
				}
			} else {
				final String message = "Data could not be retrieved for "
						+ "distance calculation.";
				JOptionPane.showMessageDialog(Frame.getFrames()[0], message,
						"Alert", JOptionPane.WARNING_MESSAGE);
				LogBuffer.println("Alert: " + message);
			}

			LogBuffer.println("Distance matrix could not be calculated and"
					+ " was set to values of 0.");
			return new double[][] { { 0 }, { 0 } };
		}

		@Override
		public void done() {

			if (!isCancelled()) {
				/* keep track of overall progress */
				pBarCount += axisSize;
				LogBuffer.println("DistTask succeeded (" + axis + ")");
			} else {
				LogBuffer.println("DistTask cancelled (" + axis + ")");
			}
		}

		/**
		 * This method uses the unformatted matrix data list and splits it up
		 * into the columns.
		 *
		 * @param unformattedData
		 *            The non-formatted, loaded data.
		 * @return
		 */
		public double[][] formatColData(final double[][] unformattedData) {

			final DataFormatter formattedData = new DataFormatter();
			return formattedData.splitColumns(unformattedData);
		}
	}

	/**
	 * General cluster method that starts a dedicated SwingWorker method which
	 * runs the calculations in the background. This allows for updates of the
	 * ClusterView GUI, e.g. the JProgressBar. If it finishes after the
	 * calculations were cancelled by the user, it let's the cluster dialog know
	 * so it can respond appropriately. Input data is translated into output
	 * data here.
	 */
	private class Clusterer extends SwingWorker<ClusteredAxisData, Integer> {

		private final DistanceMatrix distMatrix;
		private final int linkMethod;
		private final Integer[] spinnerInput;
		private final int axisID;
		private final int max;
		private final boolean hier;
		private ClusteredAxisData cad;
		
		public Clusterer(final DistanceMatrix distMatrix,
				final int linkMethod, final Integer[] spinnerInput,
				final boolean hier, final int axisID) {

			// defaults
			this.cad = new ClusteredAxisData(axisID);
			this.distMatrix = distMatrix;
			this.linkMethod = linkMethod;
			this.spinnerInput = spinnerInput;
			this.hier = hier;
			this.axisID = axisID;

			// progress bar max dependent on selected clustering type
			this.max = (hier) ? (distMatrix.getSize() - 1) : spinnerInput[0];
		}

		@Override
		protected void process(final List<Integer> chunks) {

			if(isCancelled()) return;
			final int i = chunks.get(chunks.size() - 1);
			final int progress = (isCancelled()) ? 0 : pBarCount + i;
			ClusterView.updatePBar(progress);
		}

		@Override
		public ClusteredAxisData doInBackground() {

			if (hier) {
				return doHierarchicalCluster();
			}
			
			// Not using K-means clustering at the moment. Needs refactoring!
		  return doKMeansCluster();
		}

		@Override
		public void done() {
			
			if (!isCancelled()) {
				pBarCount += max;
				LogBuffer.println("ProcessorClusterTask succeeded (" + axisID + ")");
			} else {
				LogBuffer.println("ProcessorClusterTask cancelled (" + axisID + ")");
			}
		}
		
		/**
		 * Initializes the hierarchical clustering process, 
		 * starts and finishes the tree file writer, and keeps tracks of
		 * the GUI aspects such as updating the progress bar for ClusterView.
		 * @return a ClusteredAxisData object containing all results from clustering.
		 */
		private ClusteredAxisData doHierarchicalCluster() {
			
			
			final HierCluster hierCluster = new HierCluster(linkMethod, distMatrix, 
			                                              axisID);

			/*
			 * Continue process until distMatrix has a size of 1, This array
			 * is the final cluster. Initially every row is its own cluster
			 * (bottom-up clustering).
			 */
			int loopNum = 0;
			int distMatrixSize = distMatrix.getSize();

			while (distMatrixSize > 1 && !isCancelled()) {
				distMatrixSize = hierCluster.cluster();
				publish(loopNum++);
			}

			/* Distance matrix needs to be size 1 when cluster finishes! */
			if (distMatrixSize != 1) {
				this.cancel(true);
			}

			/* Return empty String[] if user cancels operation */
			if (isCancelled()) {
				cad.shouldReorderAxis(true);
				return cad;
			}

			hierCluster.finish();

			// Update ClusteredAxisData object to conveniently store 
			// clustering results
			cad.setReorderedIdxs(hierCluster.getReorderedIDs());
			cad.setTreeNodeData(hierCluster.getTreeNodeData());
			cad.shouldReorderAxis(true);
			cad.setAxisClustered(true);
			
			return cad;
		}
		
		/**
		 * Initializes the K-Means clustering process, and keeps tracks of
		 * the GUI aspects such as updating the progress bar for ClusterView.
		 * @deprecated - REWRITE
		 */
		@Deprecated
		private ClusteredAxisData doKMeansCluster() {
			
			int k;
			int iterations;
			if (axisID == ClusterDialogController.ROW_IDX) {
				k = spinnerInput[0];
				iterations = spinnerInput[1];

			} else {
				k = spinnerInput[2];
				iterations = spinnerInput[3];
			}
			
			final KMeansCluster clusterer = new KMeansCluster(distMatrix, axisID, k);

			/*
			 * Begin iteration of recalculating means and reassigning row
			 * distance means to clusters.
			 */
			for (int i = 0; i < iterations; i++) {
				clusterer.cluster();
				publish(i);
			}

			/* Get axis labels */
			String[][] labelArray;
			if (axisID == ClusterDialogController.ROW_IDX) {
				labelArray = rowLabelI.getLabelArray();

			} else {
				labelArray = colLabelI.getLabelArray();
			}

			if (labelArray.length != distMatrix.getSize()) {
				LogBuffer.println("Label array length does not match "
						+ "size of distance matrix.");
				LogBuffer.println("Length: " + labelArray.length);
				LogBuffer.println("Distance Matrix: " + distMatrix.getSize());
				cad.shouldReorderAxis(true);
				return cad;
			}

			clusterer.finish(labelArray);

			cad.setKmeansClusterNum(k);
			cad.setReorderedIdxs(clusterer.getReorderedList());
			cad.setAxisClustered(true);
			return cad;
		}
	}
	
	/**
	 * Cancels all currently running threads.
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public void cancelAll() {

		if (distTask != null && !distTask.isDone()) {
			distTask.cancel(true);
		}
		
		if (clusterer != null && !clusterer.isDone()) {
			clusterer.cancel(true);
		}
	}
}
