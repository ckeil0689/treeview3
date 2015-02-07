package Cluster;

import java.awt.Frame;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import Controllers.ClusterController;
import Views.ClusterView;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.IntHeaderInfo;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;

/**
 * This class takes the original uploaded dataset and manipulates it according
 * to mathematical principles of hierarchical clustering. It generates files to
 * display dendrograms (.gtr and .atr) as well as a reordered original data file
 * (.cdt).
 *
 * @author CKeil
 *
 */
public class ClusterProcessor {

	private final TVDataMatrix originalMatrix;
	private final IntHeaderInfo geneHeaderI;
	private final IntHeaderInfo arrayHeaderI;

	private final String fileName;
	private int pBarCount;
	private DistanceWorker distTask;
	private ClusterTask clusterTask;

	/**
	 * Constructor for the ClusterProcessor. Sets the pBarCount to 0, which is
	 * the value that stores progress between multiple tasks, so that a single
	 * progress bar for the entire process can be displayed.
	 *
	 * @param model
	 */
	public ClusterProcessor(final TVDataMatrix dataMatrix,
			final String fileName, final IntHeaderInfo geneHeaderI,
			final IntHeaderInfo arrayHeaderI) {

		this.originalMatrix = dataMatrix;
		this.fileName = fileName;
		this.geneHeaderI = geneHeaderI;
		this.arrayHeaderI = arrayHeaderI;
		this.pBarCount = 0;
	}

	/**
	 * Starts a SwingWorker thread to do the clustering and waits for it to
	 * return a String array containing the reordered axis elements.
	 *
	 * @param distMatrix
	 * @param linkMethod
	 * @param spinnerInput
	 * @param hierarchical
	 * @param axis
	 * @return
	 */
	public String[] clusterAxis(final DistanceMatrix distMatrix,
			final int linkMethod, final Integer[] spinnerInput,
			final boolean hierarchical, final int axis) {

		try {
			LogBuffer.println("Starting clusterAxis(): " + axis);
			this.clusterTask = new ClusterTask(distMatrix, linkMethod,
					spinnerInput, hierarchical, axis);
			clusterTask.execute();

			/*
			 * Get() blocks until this thread finishes, so the following code
			 * waits for this procedure to finish.
			 */
			return clusterTask.get();

		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			LogBuffer.logException(e);
			LogBuffer.println(e.getLocalizedMessage());
			return new String[] { "No clustered data." };
		}
	}

	/**
	 * Creates a SwingWorker to calculate the distance matrix for the loaded
	 * data.
	 *
	 * @param distMeasure
	 * @param axis
	 * @return
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
			e.printStackTrace();
			LogBuffer.logException(e);
			LogBuffer.println(e.getLocalizedMessage());
			return new double[][] { { 0 }, { 0 } };
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

			if (axis == ClusterController.ROW) {
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
			if (axis == ClusterController.ROW) {
				data = originalMatrix.getExprData();

			} else {
				data = formatColData(originalMatrix.getExprData());
			}

			if (data != null) {
				final DistMatrixCalculator dCalc = new DistMatrixCalculator(
						data, distMeasure, axis);

				/* Ranking data if Spearman was chosen */
				if (distMeasure == DistMatrixCalculator.SPEARMAN) {

					final double[][] rankMatrix = new double[data.length][data[0].length];

					/* Iterate over every row of the matrix. */
					for (int i = 0; i < rankMatrix.length; i++) {

						if (isCancelled())
							return new double[0][];
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

						if (isCancelled()) {
							LogBuffer.println("DistTask cancelled.");
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
				LogBuffer.println("pBarCount: " + pBarCount);
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
	private class ClusterTask extends SwingWorker<String[], Integer> {

		private final DistanceMatrix distMatrix;
		private final int linkMethod;
		private final Integer[] spinnerInput;
		private final int axis;
		private final int max;
		private final boolean hier;

		public ClusterTask(final DistanceMatrix distMatrix,
				final int linkMethod, final Integer[] spinnerInput,
				final boolean hier, final int axis) {

			LogBuffer.println("Initializing ClusterTask.");
			this.distMatrix = distMatrix;
			this.linkMethod = linkMethod;
			this.spinnerInput = spinnerInput;
			this.hier = hier;
			this.axis = axis;

			/* Progress bar max dependent on selected clustering type */
			this.max = (hier) ? distMatrix.getSize() - 1 : spinnerInput[0];
		}

		@Override
		protected void process(final List<Integer> chunks) {

			final int i = chunks.get(chunks.size() - 1);
			final int progress = (isCancelled()) ? 0 : pBarCount + i;
			ClusterView.updatePBar(progress);
		}

		@Override
		public String[] doInBackground() {

			LogBuffer.println("Starting cluster.");

			/* Hierarchical */
			if (hier) {
				final HierCluster cGen = new HierCluster(fileName, linkMethod,
						distMatrix, axis);

				cGen.setupFileWriter(axis, fileName);

				/*
				 * Continue process until distMatrix has a size of 1, This array
				 * is the final cluster. Initially every row is its own cluster
				 * (bottom-up clustering).
				 */
				int loopNum = 0;
				int distMatrixSize = distMatrix.getSize();

				while (distMatrixSize > 1 && !isCancelled()) {

					distMatrixSize = cGen.cluster();
					publish(loopNum);
					loopNum++;
				}

				/* Distance matrix needs to be size 1 when cluster finishes! */
				if (distMatrixSize != 1) {
					this.cancel(true);
				}

				/* Return empty String[] if user cancels operation */
				if (isCancelled()) {
					LogBuffer.println("ClusterTask cancelled.");
					return new String[] {};
				}

				LogBuffer.println("Clustering matrix done.");

				/* Write the tree file */
				LogBuffer.println("Writing clustered data.");
				cGen.finish();

				return cGen.getReorderedList();
			}
			/* K-Means */
			else {
				int k;
				int iterations;
				if (axis == ClusterController.ROW) {
					k = spinnerInput[0];
					iterations = spinnerInput[1];

				} else {
					k = spinnerInput[2];
					iterations = spinnerInput[3];
				}

				final KMeansCluster cGen = new KMeansCluster(distMatrix, axis,
						k, fileName);

				/*
				 * Begin iteration of recalculating means and reassigning row
				 * distance means to clusters.
				 */
				for (int i = 0; i < iterations; i++) {

					cGen.cluster();
					publish(i);
				}

				/* Get axis labels */
				String[][] headerArray;
				if (axis == ClusterController.ROW) {
					headerArray = geneHeaderI.getHeaderArray();

				} else {
					headerArray = arrayHeaderI.getHeaderArray();
				}

				if (headerArray.length != distMatrix.getSize()) {
					LogBuffer.println("Label array length does not match "
							+ "size of distance matrix.");
					LogBuffer.println("HeaderArray: " + headerArray.length);
					LogBuffer.println("Distance Matrix: "
							+ distMatrix.getSize());
					return new String[] {};
				}

				/* Write data and close writer */
				cGen.finish(headerArray);

				return cGen.getReorderedList();
			}
		}

		@Override
		public void done() {

			if (!isCancelled()) {
				pBarCount += max;
			}
			LogBuffer.println("Clustering canceled? " + !isCancelled());
		}
	}

	/**
	 * Cancels all currently running threads.
	 */
	public void cancelAll() {

		LogBuffer.println("ClusterProcessor canceling all.");
		if (distTask != null) {
			distTask.cancel(true);
		}
		if (clusterTask != null) {
			clusterTask.cancel(true);
		}
	}
}
