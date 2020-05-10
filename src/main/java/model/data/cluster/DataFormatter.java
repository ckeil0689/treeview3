/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package model.data.cluster;

/**
 * This class is used to make an object which can take in the loaded data in its
 * format as originally coded in Java TreeView's first version and format it for
 * use in the clustering module.
 *
 * @author CKeil
 */
public class DataFormatter {

	// getting the columns from raw data array
	public double[][] splitColumns(final double[][] rawData) {

		// Number of arrays/ columns
		// Assumes all arrays are same length
		final int nCols = rawData[0].length;
		final int nRows = rawData.length;

		final double[][] colList = new double[nCols][nRows];

		// Iterate through all columns
		for (int j = 0; j < nCols; j++) {

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
