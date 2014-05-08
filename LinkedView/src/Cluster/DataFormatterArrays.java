package Cluster;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.model.TVModel;

/**
 * This class is used to make an object which can take in the loaded data in its
 * format as originally coded in Java TreeView's first version and format it for
 * use in the clustering module.
 * 
 * @author CKeil
 */
public class DataFormatterArrays {

	// Instance variables
	private final TVModel model;
	private final ClusterView clusterView;
	private final double[][] rawData;

	private double[][] colList;

	// Constructor (building the object)
	public DataFormatterArrays(final DataModel model,
			final ClusterView clusterView, final double[][] rawData) {

		this.model = (TVModel) model;
		this.clusterView = clusterView;
		this.rawData = rawData;
	}

	// getting the columns from raw data array
	public void splitColumns() {

		// Number of arrays/ columns
		final int nCols = model.nExpr();
		final int nRows = model.nGene();

		colList = new double[nCols][nRows];

		// Setting up ProgressBar
		clusterView.setLoadText("Finding data columns...");
		clusterView.setPBarMax(nCols);

		// Iterate through all columns
		for (int j = 0; j < nCols; j++) {

			clusterView.updatePBar(j);

			final double[] sArray = new double[nRows];

			for (int i = 0; i < nRows; i++) {

				// final int element = (i * nCols) + j;

				sArray[i] = rawData[i][j];
			}

			colList[j] = sArray;
		}
	}

	public double[][] getColList() {

		return colList;
	}
}
