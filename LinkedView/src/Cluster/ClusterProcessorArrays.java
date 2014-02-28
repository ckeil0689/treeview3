package Cluster;

import java.util.List;
import java.util.concurrent.ExecutionException;

import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;

//import Cluster.ClusterView.FinalOptionsPanel;

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

	// View
	private final ClusterView clusterView;
	
	// Model
	private final TVModel tvModel;
	
	// Lists
	private double[][] rowDistances;
	private double[][] colDistances;

	private String[] orderedRows;
	private String[] orderedCols;
	
	/**
	 * Main constructor
	 * @param cView
	 * @param model
	 */
	public ClusterProcessorArrays(final ClusterView cView, final TVModel model) {
		
		this.clusterView = cView;
		this.tvModel = model;
	}

	/**
	 * Main method to iterate through the various processes of clustering which
	 * are done by other classes.
	 * 
	 * Returns the filePath of the newly clustered file.
	 * 
	 * @param similarityM
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public String cluster(final boolean hierarchical) {
		
		// Get the data for clustering
		final double[][] dataArrays = ((TVDataMatrix) tvModel.getDataMatrix())
				.getExprData();
		
		// Getting user choices from the View
		final String choice = clusterView.getRowSimilarity();
		final String choice2 = clusterView.getColSimilarity();
		
		// if user checked clustering for elements
		if (!choice.contentEquals("Do Not Cluster")) {
			
			clusterRows(dataArrays, hierarchical);
		}

		// if user checked clustering for arrays
		if (!choice2.contentEquals("Do Not Cluster")) {
			
			double[][] sepCols = formatColData(dataArrays);
			
			clusterCols(sepCols, hierarchical);
		}

		// also takes list of row elements because only one list can easily
		// be consistently transformed and fed into file writer
		// to make a tab-delimited file
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
				clusterView, distanceMatrix, type);

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
	public List<String> kmCluster(final List<List<Double>> distances,
			final String type, final int clusterN, final int iterations) {

		final KMeansCluster cGen = new KMeansCluster(tvModel, clusterView, 
				distances, type, clusterN, iterations);

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
	 * Clusters the row data based on user input.
	 * @param sepRows
	 */
	public void clusterRows(double[][] dataArrays, boolean hierarchical) {
		
		final String choice = clusterView.getRowSimilarity();
		final String rowString = "GENE";
		
		final DMCalculatorArrays dCalc = 
				new DMCalculatorArrays(dataArrays, choice, clusterView);

		dCalc.measureDistance();

		rowDistances = dCalc.getDistanceMatrix();
				
		if (hierarchical) {
			orderedRows = hCluster(rowDistances, rowString);

		} else {
			Integer[] spinnerInput = clusterView.getSpinnerValues();
			
//			orderedRows = kmCluster(rowDistances, rowString, spinnerInput[0], 
//					spinnerInput[1]);
		}

		clusterView.refresh();
	}
	
	/**
	 * Clusters the column data based on user input.
	 * @param sepRows
	 */
	public void clusterCols(double[][] sepCols, boolean hierarchical) {
		
		final String choice2 = clusterView.getColSimilarity();
		final String colString = "ARRY";
		
		final DMCalculatorArrays dCalc = 
				new DMCalculatorArrays(sepCols, choice2, clusterView);

		dCalc.measureDistance();

		colDistances = dCalc.getDistanceMatrix();
		
		if (hierarchical) {
			orderedCols = hCluster(colDistances, colString);

		} else {
			Integer[] spinnerInput = clusterView.getSpinnerValues();
//			orderedCols = kmCluster(colDistances, colString, spinnerInput[2], 
//					spinnerInput[3]);
		}

		clusterView.refresh();
	}
}
