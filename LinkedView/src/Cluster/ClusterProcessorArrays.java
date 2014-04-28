package Cluster;

import java.util.concurrent.ExecutionException;

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
	private SwingWorker<Void, Void> worker;
	
	/**
	 * Main constructor
	 * @param cView
	 * @param model
	 */
	public ClusterProcessorArrays(final ClusterView cView, 
			final TVModel model, SwingWorker<Void, Void> worker) {
		
		this.clusterView = cView;
		this.tvModel = model;
		this.worker = worker;
	}
	
	/**
	 * Function to generate a cdt-file that represents a clustered data set.
	 * The file is generated from the clustering results and subsequently
	 * stored into the same directory as the original file, together with
	 * ATR and GTR files for the dendrograms. 
	 * @param hierarchical
	 * @return
	 */
	public String saveCDT(boolean hierarchical, String[] orderedRows, 
			String[] orderedCols) {
		
		// also takes list of row elements because only one list can easily
		// be consistently transformed and fed into file writer
		// to make a tab-delimited file
		final double[][] dataArrays = ((TVDataMatrix) tvModel.getDataMatrix())
				.getExprData();
		
		final CDTGeneratorArrays cdtGen = new CDTGeneratorArrays(tvModel, 
				clusterView, dataArrays, orderedRows, orderedCols, 
				hierarchical);
				
		cdtGen.generateCDT();
		
		return cdtGen.getFilePath();
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
			 final String type) {

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
	public String[] kmCluster(final double[][] distances,
			final String type, final int clusterN, final int iterations) {

		final KMeansCluster cGen = new KMeansCluster(tvModel, clusterView, 
				distances, type, clusterN, iterations, worker);

		cGen.cluster();

		return cGen.getReorderedList();
	}
	
	/**
	 * This method uses the unformatted matrix data list and splits it up into
	 * the columns.
	 * @param unformattedData
	 * @return
	 */
	public double[][] formatColData(double[][] unformattedData) {
		
		final DataFormatterArrays formattedData = 
				new DataFormatterArrays(tvModel, clusterView, unformattedData);
		
		formattedData.splitColumns();

		double[][] sepCols = formattedData.getColList();
		
		return sepCols;
	}
	
	/**
	 * Calculates the distance matrix according to the chosen method.
	 * @param choice
	 * @return
	 */
	public double[][] calculateDistance(String choice, String type) {
		
		double[][] data = null;
		if(type.equalsIgnoreCase("Row")) {
			data = ((TVDataMatrix) tvModel.getDataMatrix())
					.getExprData();
			
		} else {
			data = formatColData(((TVDataMatrix)tvModel.getDataMatrix())
					.getExprData());
		}
		
		if(data != null) {
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
