package Cluster;

import javax.swing.SwingWorker;

import Utilities.AlertDialog;
import Utilities.ErrorDialog;
import Controllers.ClusterController;
import edu.stanford.genetics.treeview.DataModel;
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
public class ClusterProcessor {

	private final ClusterView clusterView;
	private final DataModel tvModel;
	private final SwingWorker<String[], Void> worker;

	/**
	 * Main constructor
	 * 
	 * @param cView
	 * @param model
	 */
	public ClusterProcessor(final ClusterView cView, 
			final DataModel model, final SwingWorker<String[], Void> worker) {

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
	public String[] hCluster(final double[][] distanceMatrix, 
			final int type) {

		final HierarchicalCluster cGen = new HierarchicalCluster(tvModel,
				clusterView, distanceMatrix, type, worker);

		cGen.cluster();

		return cGen.getReorderedList();
	}

	/**
	 * Method to execute the K-Means clustering algorithm.
	 * 
	 * @param distMatrix The calculated distance matrix. 
	 * @param axis The matrix axis on which operations will be performed.
	 * @param pBar The progressBar.
	 * @param groupNum Number of groups to be formed (k).
	 * @param iterations Number of iterations to be done.
	 * @return
	 */
	public String[] kmCluster(final double[][] distMatrix, final int axis,
			final int groupNum, final int iterations) {

		final KMeansCluster cGen = new KMeansCluster(tvModel, clusterView,
				distMatrix, axis, groupNum, iterations, worker);

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

		final DataFormatter formattedData = new DataFormatter(
				tvModel, clusterView, unformattedData);

		formattedData.splitColumns();

		final double[][] sepCols = formattedData.getColList();

		return sepCols;
	}

	/**
	 * Calculates the distance matrix according to the chosen method.
	 * 
	 * @param distMeasure
	 * @return
	 */
	public double[][] calculateDistance(final String distMeasure, 
			final int axis) {

		double[][] data = null;
		if (axis == ClusterController.ROW) {
			data = ((TVDataMatrix) tvModel.getDataMatrix()).getExprData();

		} else {
			data = formatColData(((TVDataMatrix) tvModel.getDataMatrix())
					.getExprData());
		}

		if (data != null) {
			final DistMatrixCalculator dCalc = new DistMatrixCalculator(data, 
					distMeasure, axis, clusterView, worker);

			try {
				dCalc.measureDistance();
				
			} catch(NumberFormatException e) {
				String message = "Measuring the distances experienced "
						+ "an issue. Check log messages to see the cause.";
				ErrorDialog.showError(message, e);;
			}
			
			return dCalc.getDistanceMatrix();

		} else {
			String message = "Data could not be retrieved "
					+ "for distance calculation.";
			AlertDialog.showAlert(message);
			return null;
		}
	}
}
