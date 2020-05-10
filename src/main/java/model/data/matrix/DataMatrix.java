/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package model.data.matrix;

/**
 * Description of the Interface
 *
 * Provides a simple interface to the actual gene expression data, using the
 * same indexes as the corresponding LabelInfo objects.
 *
 *
 * @author aloksaldanha
 */
public interface DataMatrix {
	
	/**
	 * Gets the 2D double array representing the underlying matrix data.
	 * 
	 * @return A 2D double array.
	 */
	public double[][] getExprData();
	
	/**
	 * Gets the value attribute of the DataMatrix object
	 *
	 * @param row
	 *            row (gene) of interest
	 * @param col
	 *            column (array) of interest
	 * @return The value at the row/col, or possibly some special "missing data"
	 *         value, as defined by the constants in DataModel.
	 */
	double getValue(int col, int row);

	/**
	 * Find the minimum and maximum value in the loaded data set.
	 */
	void calculateBaseValues();

	/**
	 * Return the minimum value of the data set. Used to calculate colors to draw
	 * the pixels with DoubleArrayDrawer in DendroView.
	 *
	 * @return
	 */
	double getMinVal();

	/**
	 * Return the maximum value of the data set. Used to calculate colors to draw
	 * the pixels with DoubleArrayDrawer in DendroView.
	 *
	 * @return
	 */
	double getMaxVal();
	
	/**
	 * Return the mean value of the data set.
	 *
	 * @return A double representing the mean value of the data set.
	 */
	double getMean();
	
	/**
	 * Return the mean value of a subset of the data set.
	 *
	 * @return A double representing the mean value of a subset of the data set.
	 */
	double getZoomedMean(int startingRow, int endingRow, int startingCol, int endingCol);
	
	/**
	 * Return the median value of the data set.
	 *
	 * @return A double representing the mean value of the data set.
	 */
	double getMedian();

	/**
	 * Sets the value attribute of an element in the DataMatrix object
	 *
	 * @param value
	 *            value to be set
	 * @param row
	 *            row (gene) of interest
	 * @param col
	 *            column (array) of interest
	 */
	void setValue(double value, int col, int row);

	/**
	 * Gets the numRow attribute of the DataMatrix object
	 *
	 * @return The number of rows (genes) in this data matrix.
	 */
	int getNumRow();

	/**
	 * Appends a data matrix to the right of this one. Used for comparison of
	 * two data sets.
	 *
	 * @param m
	 *            The DataMatrix being appended.
	 */

	/**
	 * Gets the numCol attribute of the DataMatrix object
	 *
	 * @return The number of columns (arrays) in this data matrix.
	 */
	int getNumCol();

	/**
	 * Gets the numCol attribute of the DataMatrix object before anything was
	 * appended to it.
	 *
	 * @return The number of columns (arrays) in this data matrix before
	 *         anything was appended.
	 */
	int getNumUnappendedCol();

	int getNumUnappendedRow();

	/**
	 * return true if data matrix has been modified and should be written to
	 * file.
	 *
	 * @return
	 */
	boolean getModified();

	void setModified(boolean b);
	
	public double getRowAverage(int fromRowId, int toRowId);
	
	public double getColAverage(int fromColId, int toColId);

	void setMinVal(double newMinVal);

	void setMaxVal(double newMaxVal);

	void setMean(double newMeanVal);

	void setMedian(double newMedianVal);

}
