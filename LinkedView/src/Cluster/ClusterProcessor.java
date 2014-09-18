package Cluster;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import Controllers.ClusterController;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.LogBuffer;
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

	private final DataModel tvModel;
	private final SwingWorker<String[], Void> worker;

	/**
	 * Main constructor
	 * 
	 * @param cView
	 * @param model
	 */
	public ClusterProcessor(final DataModel model, 
			final SwingWorker<String[], Void> worker) {

		this.tvModel = model;
		this.worker = worker;
	}

	/**
	 * Method to execute one of the Hierarchical Clustering algorithms.
	 * 
	 * @param distMatrix The calculated distance matrix to be clustered.
	 * @param axis The matrix axis to be clustered.
	 * @param linkageMethod The linkage method to be used.
	 * @return String[] List of reordered matrix elements.
	 */
	public String[] hCluster(final double[][] distMatrix, 
			final int axis, final String linkageMethod) {

		String fileName = tvModel.getSource().substring(0, 
				tvModel.getSource().length() - 4);
		
		final HierarchicalCluster cGen = new HierarchicalCluster(fileName,
				linkageMethod, distMatrix, axis, worker);

		cGen.cluster();

		return cGen.getReorderedList();
	}

	/**
	 * Method to execute the K-Means clustering algorithm.
	 * 
	 * @param tvModel The data model. Needed for headerInfo arrays.
	 * @param distMatrix The calculated distance matrix. 
	 * @param axis The matrix axis on which operations will be performed.
	 * @param groupNum Number of groups to be formed (k).
	 * @param iterations Number of iterations to be done.
	 * @return
	 */
	public String[] kmCluster(final double[][] distMatrix, final int axis,
			final int groupNum, final int iterations) {

		final KMeansCluster cGen = new KMeansCluster(tvModel, distMatrix, 
				axis, groupNum, iterations, worker);

		cGen.cluster();

		return cGen.getReorderedList();
	}

	/**
	 * This method uses the unformatted matrix data list and splits it up into
	 * the columns.
	 * 
	 * @param unformattedData The non-formatted, loaded data.
	 * @return
	 */
	public double[][] formatColData(final double[][] unformattedData) {

		final DataFormatter formattedData = new DataFormatter();

		return formattedData.splitColumns(unformattedData);
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
					distMeasure, axis, worker);

			try {
				dCalc.measureDistance();
				
			} catch(NumberFormatException e) {
				String message = "Measuring the distances experienced "
						+ "an issue. Check log messages to see the cause.";
				
				JOptionPane.showMessageDialog(JFrame.getFrames()[0], message, 
						"Error", JOptionPane.ERROR_MESSAGE);
				LogBuffer.logException(e);
			}
			
			return dCalc.getDistanceMatrix();

		} else {
			String message = "Data could not be retrieved "
					+ "for distance calculation.";
			JOptionPane.showMessageDialog(JFrame.getFrames()[0], message, 
					"Alert", JOptionPane.WARNING_MESSAGE);
			LogBuffer.println("Alert: " + message);
			return null;
		}
	}
}
