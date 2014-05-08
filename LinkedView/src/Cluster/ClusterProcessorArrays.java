package Cluster;

import javax.swing.SwingWorker;

import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;

/**
 * This class takes the original uploaded dataArray passed in the constructor
 * and manipulates it according to mathematical principles of hierarchical
 * clustering. It generates files to display dendrograms (.gtr and .atr) as well
 * as a reordered original data file (.cdt)
 * 
 * @author CKeil
 * 
 */
public class ClusterProcessorArrays {

	private final ClusterView clusterView;
	private final TVModel tvModel;
	private final SwingWorker<String[], Void> worker;

	/**
	 * Main constructor
	 * 
	 * @param cView
	 * @param model
	 */
	public ClusterProcessorArrays(final ClusterView cView, final TVModel model,
			final SwingWorker<String[], Void> worker) {

		this.clusterView = cView;
		this.tvModel = model;
		this.worker = worker;
	}

	/**
	 * Method to execute one of the Hierarchical Clustering algorithms.
	 * 
	 * @param distances
	 * @param type
	 * @param pBar
	 * @return
	 */
	public String[] hCluster(final double[][] distanceMatrix, final String type) {

		final HierClusterArrays cGen = new HierClusterArrays(tvModel,
				clusterView, distanceMatrix, type, worker);

		cGen.cluster();

		return cGen.getReorderedList();
	}

	/**
	 * Method to execute the K-Means clustering algorithm.
	 * 
	 * @param distances
	 * @param type
	 * @param pBar
	 * @param clusterN
	 * @param iterations
	 * @return
	 */
	public String[] kmCluster(final double[][] distances, final String type,
			final int clusterN, final int iterations) {

		final KMeansCluster cGen = new KMeansCluster(tvModel, clusterView,
				distances, type, clusterN, iterations, worker);

		cGen.cluster();

		return cGen.getReorderedList();
	}

	/**
	 * This method uses the unformatted matrix data list and splits it up into
	 * the columns.
	 * 
	 * @param unformattedData
	 * @return
	 */
	public double[][] formatColData(final double[][] unformattedData) {

		final DataFormatterArrays formattedData = new DataFormatterArrays(
				tvModel, clusterView, unformattedData);

		formattedData.splitColumns();

		final double[][] sepCols = formattedData.getColList();

		return sepCols;
	}

	/**
	 * Calculates the distance matrix according to the chosen method.
	 * 
	 * @param choice
	 * @return
	 */
	public double[][] calculateDistance(final String choice, final String type) {

		double[][] data = null;
		if (type.equalsIgnoreCase("Row")) {
			data = ((TVDataMatrix) tvModel.getDataMatrix()).getExprData();

		} else {
			data = formatColData(((TVDataMatrix) tvModel.getDataMatrix())
					.getExprData());
		}

		if (data != null) {
			final DMCalculatorArrays dCalc = new DMCalculatorArrays(data,
					choice, type, clusterView, worker);

			dCalc.measureDistance();
			return dCalc.getDistanceMatrix();

		} else {
			LogBuffer.println(type + " Data was not extracted.");
			return null;
		}
	}
}
