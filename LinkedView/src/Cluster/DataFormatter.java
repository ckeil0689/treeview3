package Cluster;

import javax.swing.SwingUtilities;

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * This class is used to make an object which can take in the loaded data in its
 * format as originally coded in Java TreeView's first version and format it for
 * use in the clustering module.
 * 
 * @author CKeil
 */
public class DataFormatter {

	public DataFormatter() {

	}

	// getting the columns from raw data array
	public double[][] splitColumns(final double[][] rawData) {

		// Just checking for debugging
		LogBuffer.println("Is DataFormatterArrays.splitColumns() on EDT? " 
				+ SwingUtilities.isEventDispatchThread());
				
		// Number of arrays/ columns
		// Assumes all arrays are same length
		final int nCols = rawData[0].length;
		final int nRows = rawData.length;

		final double[][] colList = new double[nCols][nRows];

		// Setting up ProgressBar
		ClusterView.setLoadText("Finding data columns...");
		ClusterView.setPBarMax(nCols);

		// Iterate through all columns
		for (int j = 0; j < nCols; j++) {

			ClusterView.updatePBar(j);

			final double[] sArray = new double[nRows];

			for (int i = 0; i < nRows; i++) {

				// use if data is 1D array
				// final int element = (i * nCols) + j;

				sArray[i] = rawData[i][j];
			}

			colList[j] = sArray;
		}
		
		return colList;
	}
}
