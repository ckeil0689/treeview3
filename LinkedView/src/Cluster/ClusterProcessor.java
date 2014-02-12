package Cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import edu.stanford.genetics.treeview.DataModel;
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
public class ClusterProcessor {

	// View
	private final ClusterView clusterView;
	
	// Model
	private final DataModel tvModel;
	
	// Lists
	private List<List<Double>> rowDistances = new ArrayList<List<Double>>();
	private List<List<Double>> colDistances = new ArrayList<List<Double>>();

	private List<String> orderedRows = new ArrayList<String>();
	private List<String> orderedCols = new ArrayList<String>();
	
	/**
	 * Main constructor
	 * @param cView
	 * @param model
	 */
	public ClusterProcessor(final ClusterView cView, final TVModel model) {
		
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
		
		// List variables needed for process
		final List<Double> unformattedDataList = matrixToList();
		
		// Getting formatted row data
		List<List<Double>> sepRows = formatRowData(unformattedDataList);
		
		// Getting user choices from the View
		final String choice = clusterView.getRowSimilarity();
		final String choice2 = clusterView.getColSimilarity();
		
		// if user checked clustering for elements
		if (!choice.contentEquals("Do Not Cluster")) {
			
			clusterRows(sepRows, hierarchical);
		}

		// if user checked clustering for arrays
		if (!choice2.contentEquals("Do Not Cluster")) {
			
			List<List<Double>> sepCols = formatColData(unformattedDataList);
			
			clusterCols(sepCols, hierarchical);
		}

		// also takes list of row elements because only one list can easily
		// be consistently transformed and fed into file writer
		// to make a tab-delimited file
		
		//works until here (but check for correctness!)
		final CDTGenerator2 cdtGen = new CDTGenerator2(tvModel, clusterView, 
				sepRows, orderedRows, orderedCols, hierarchical);

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
	public List<String> hCluster(final List<List<Double>> distances,
			final String type) {

		final HierCluster2 cGen = new HierCluster2(tvModel, clusterView, 
				distances, type);

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
	 * This method takes the matrix data that is provided as double[] and
	 * transforms it to a List<Double> object for further procedures.
	 * @return List<Double>
	 */
	public List<Double> matrixToList() {
		
		final List<Double> dataList = new ArrayList<Double>();
		
		TVDataMatrix matrix = (TVDataMatrix) tvModel.getDataMatrix();
		final double[][] dataArrays = matrix.getExprData();
		
		for(double[] array : dataArrays) {
			for (final double d : array) {
	
				dataList.add(d);
			}
		}
		
		return dataList;
	}
	
	/**
	 * This method uses the unformatted matrix data list and splits it up into
	 * the rows.
	 * @param unformattedData
	 * @return
	 */
	public List<List<Double>> formatRowData(List<Double> unformattedData) {
		
		final DataFormatter formattedData = 
				new DataFormatter(tvModel, clusterView, unformattedData);

		List<List<Double>> sepRows = new ArrayList<List<Double>>();
		
		formattedData.splitRows();
		sepRows = formattedData.getRowList();
		
		return sepRows;
	}
	
	/**
	 * This method uses the unformatted matrix data list and splits it up into
	 * the columns.
	 * @param unformattedData
	 * @return
	 */
	public List<List<Double>> formatColData(List<Double> unformattedData) {
		
		final DataFormatter formattedData = 
				new DataFormatter(tvModel, clusterView,unformattedData);

		List<List<Double>> sepCols = new ArrayList<List<Double>>();
		
		formattedData.splitColumns();
		sepCols = formattedData.getColList();
		
		return sepCols;
	}
	
	/**
	 * Clusters the row data based on user input.
	 * @param sepRows
	 */
	public void clusterRows(List<List<Double>> sepRows, boolean hierarchical) {
		
		final String choice = clusterView.getRowSimilarity();
		final String rowString = "GENE";
		
		final DistanceMatrixCalculator dCalc = 
				new DistanceMatrixCalculator(sepRows, choice, clusterView);

		dCalc.measureDistance();

		rowDistances = dCalc.getDistanceMatrix();
				
		if (hierarchical) {
			orderedRows = hCluster(rowDistances, rowString);

		} else {
			Integer[] spinnerInput = clusterView.getSpinnerValues();
			
			orderedRows = kmCluster(rowDistances, rowString, spinnerInput[0], 
					spinnerInput[1]);
		}

		clusterView.refresh();
	}
	
	/**
	 * Clusters the column data based on user input.
	 * @param sepRows
	 */
	public void clusterCols(List<List<Double>> sepCols, boolean hierarchical) {
		
		final String choice2 = clusterView.getColSimilarity();
		final String colString = "ARRY";
		
		final DistanceMatrixCalculator dCalc = 
				new DistanceMatrixCalculator(sepCols, choice2, clusterView);

		dCalc.measureDistance();

		colDistances = dCalc.getDistanceMatrix();
		
		Integer[] spinnerInput = clusterView.getSpinnerValues();
		
		if (hierarchical) {
			orderedCols = hCluster(colDistances, colString);

		} else {
			orderedCols = kmCluster(colDistances, colString, spinnerInput[2], 
					spinnerInput[3]);
		}

		clusterView.refresh();
	}
}
