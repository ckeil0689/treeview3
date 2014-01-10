package Cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JPanel;
import javax.swing.JProgressBar;

import edu.stanford.genetics.treeview.DataModel;

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

	// Instance variables
	private final ClusterView cView;
	private final DataModel model;
	private final double[] currentArray;
	private final String rowString = "GENE";
	private final String colString = "ARRY";
	private final String choice;
	private final String choice2;
	private final String similarityM;
	private final int row_clusterN;
	private final int row_iterations;
	private final int col_clusterN;
	private final int col_iterations;

	// GUI Components
	private final JPanel mainPanel;
	private final JProgressBar pBar;
	private final JProgressBar pBar2;
	private final JProgressBar pBar3;
	private final JProgressBar pBar4;

	/**
	 * Main constructor
	 * 
	 * @param model
	 * @param viewFrame
	 * @param cView
	 * @param pBar
	 * @param pBar2
	 * @param pBar3
	 * @param pBar4
	 * @param currentArray
	 */
	public ClusterProcessor(final ClusterView cView, final JProgressBar pBar,
			final JProgressBar pBar2, final JProgressBar pBar3,
			final JProgressBar pBar4, final String similarityM,
			final int row_clusterN, final int row_iterations,
			final int col_clusterN, final int col_iterations) {

		this.cView = cView;
		this.model = cView.getDataModel();
		this.pBar = pBar;
		this.pBar2 = pBar2;
		this.pBar3 = pBar3;
		this.pBar4 = pBar4;
		this.currentArray = cView.getDataArray();
		this.mainPanel = cView.getMainPanel();
		this.choice = (String) cView.getGeneCombo().getSelectedItem();
		this.choice2 = (String) cView.getArrayCombo().getSelectedItem();
		this.similarityM = similarityM;
		this.row_clusterN = row_clusterN;
		this.row_iterations = row_iterations;
		this.col_clusterN = col_clusterN;
		this.col_iterations = col_iterations;
	}

	/**
	 * Main method to iterate through the various processes of clustering which
	 * are done by other classes.
	 * 
	 * @param similarityM
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void cluster(final boolean hierarchical)
			throws InterruptedException, ExecutionException {

		// List variables needed for process
		final List<Double> currentList = new ArrayList<Double>();

		List<List<Double>> sepRows = new ArrayList<List<Double>>();
		List<List<Double>> sepCols = new ArrayList<List<Double>>();
		List<List<Double>> rowDistances = new ArrayList<List<Double>>();
		List<List<Double>> colDistances = new ArrayList<List<Double>>();

		List<String> orderedRows = new ArrayList<String>();
		List<String> orderedCols = new ArrayList<String>();

		// change data array into a list (more flexible, faster access for
		// larger computations)
		for (final double d : currentArray) {

			currentList.add(d);
		}

		final DataFormatter formattedData = new DataFormatter(model,
				currentList, pBar);

		formattedData.splitRows();
		sepRows = formattedData.getRowList();

		// if user checked clustering for elements
		if (!choice.contentEquals("Do Not Cluster")) {

			final DistanceMatrixCalculator dCalc = new DistanceMatrixCalculator(
					sepRows, choice, pBar);

			dCalc.measureDistance();

			rowDistances = dCalc.getDistanceMatrix();

			if (hierarchical) {
				orderedRows = hCluster(rowDistances, rowString, pBar2);

			} else {
				orderedRows = kmCluster(rowDistances, rowString, pBar2,
						row_clusterN, row_iterations);
			}

			mainPanel.revalidate();
			mainPanel.repaint();
		}

		// if user checked clustering for arrays
		if (!choice2.contentEquals("Do Not Cluster")) {

			formattedData.splitColumns();
			sepCols = formattedData.getColList();

			final DistanceMatrixCalculator dCalc2 = new DistanceMatrixCalculator(
					sepCols, choice2, pBar3);

			dCalc2.measureDistance();

			colDistances = dCalc2.getDistanceMatrix();

			if (hierarchical) {
				orderedCols = hCluster(colDistances, colString, pBar4);

			} else {
				orderedCols = kmCluster(colDistances, colString, pBar4,
						col_clusterN, col_iterations);
			}

			mainPanel.revalidate();
			mainPanel.repaint();
			;
		}

		// also takes list of row elements because only one list can easily
		// be consistently transformed and fed into file writer
		// to make a tab-delimited file
		final CDTGenerator cdtGen = new CDTGenerator(model, sepRows,
				orderedRows, orderedCols, choice, choice2, hierarchical,
				row_clusterN, col_clusterN);

		cdtGen.generateCDT();

		cView.setPath(cdtGen.getFilePath());
		cView.setFile(cdtGen.getFile());

		mainPanel.revalidate();
		mainPanel.repaint();
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
			final String type, final JProgressBar pBar) {

		final HierCluster cGen = new HierCluster(model, distances, pBar, type,
				similarityM);

		cGen.cluster();

		cView.setPath(cGen.getFilePath());

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
			final String type, final JProgressBar pBar, final int clusterN,
			final int iterations) {

		final KMeansCluster cGen = new KMeansCluster(model, distances, pBar,
				clusterN, iterations, type);

		cGen.cluster();

		cView.setPath(cGen.getFilePath());

		return cGen.getReorderedList();
	}
}
