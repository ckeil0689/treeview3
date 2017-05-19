/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.model;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Observable;
import java.util.prefs.Preferences;

import Utilities.Helper;
import edu.stanford.genetics.treeview.DataMatrix;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.FileSetListener;
import edu.stanford.genetics.treeview.LabelInfo;
import edu.stanford.genetics.treeview.LogBuffer;

public class TVModel extends Observable implements DataModel {

	// protected TreeViewFrame tvFrame;
	protected FileSet source = null;
	protected String dir = null;
	protected String root;

	protected TVDataMatrix dataMatrix;

	protected IntLabelInfo colLabelInfo;
	protected RowLabelInfo rowLabelInfo;
	protected IntLabelInfo atrLabelInfo;
	protected IntLabelInfo gtrLabelInfo;

	protected boolean aidFound = false;
	protected boolean gidFound = false;

	protected boolean eweightFound = false;
	protected boolean gweightFound = false;
	protected Preferences documentConfig; // holds document config

	/** has model been successfully loaded? */
	private boolean loaded = false;
	private final int appendIndex = -1;

	/*
	 * For cases where we are comparing two models (this needs to be changed).
	 */
	private TVModel compareModel = null;
	private int extraCompareExpr = 0;

	public TVModel() {

		super();
		/* build TVModel, initially empty... */
		rowLabelInfo = new RowLabelInfo();
		colLabelInfo = new IntLabelInfo();
		atrLabelInfo = new IntLabelInfo();
		gtrLabelInfo = new IntLabelInfo();
		dataMatrix = new TVDataMatrix();
	}

	/*
	 * This not-so-object-oriented hack is in those rare instances where it is
	 * not enough to know that we've got a DataModel.
	 */
	@Override
	public String getType() {

		return "TVModel";
	}

	@Override
	public void setModelForCompare(final DataModel m) {

		if (m == null) {
			compareModel = null;
			extraCompareExpr = 0;

		} else {
			compareModel = (TVModel) m;
			extraCompareExpr = compareModel.nCols() + 2;
		}
		hasChanged();
	}

	// accessor methods
	@Override
	public IntLabelInfo getRowLabelInfo() {

		return rowLabelInfo;
	}

	@Override
	public IntLabelInfo getColLabelInfo() {

		return colLabelInfo;
	}

	@Override
	public DataMatrix getDataMatrix() {

		if (compareModel != null) {

		}
		return dataMatrix;
	}

	@Override
	public LabelInfo getAtrLabelInfo() {

		return atrLabelInfo;
	}

	@Override
	public LabelInfo getGtrLabelInfo() {

		return gtrLabelInfo;
	}

	public boolean gweightFound() {

		return gweightFound;
	}

	public int nRows() {

		return rowLabelInfo.getNumLabels();
	}

	public int nCols() {

		return colLabelInfo.getNumLabels() + extraCompareExpr;
	}

	public void setExprData(final double[][] newData) {

		LogBuffer.println("Adding data to model...");
		dataMatrix.setExprData(newData);
	}

	public double getValue(final int x, final int y) {

		final int nCols = nCols();
		final int nRows = nRows();
		
		if (x >= nCols + 2) {
			if (compareModel != null) {
				return compareModel.getValue(x - (nCols + 2), y); // check
			// offsets
			}

		} else if (x >= nCols && y < nRows) {
			return 0; // gray border
		}

		if ((x < nCols && y < nRows) && (x >= 0 && y >= 0)) {
			return dataMatrix.getValue(x, y);
		}

		return DataModel.NAN;
	}

	@Override
	public boolean aidFound() {

		return aidFound;
	}
	
	/**
	 * Static implementation to check data values for NaN while retaining
	 * the 'missing' name.
	 * @param val
	 * @return Whether the data is NaN or not
	 */
	public static boolean isMissing(final double val) {
		
		return Double.isNaN(val);
	}
	
	/**
	 * Static implementation to check data values for Infinity while retaining
	 * the 'empty' name.
	 * @param val
	 * @return Whether the data is considered empty or not
	 */
	public static boolean isEmpty(final double val) {
		
		return Double.isInfinite(val);
	}
	
	public void aidFound(final boolean newVal) {

		aidFound = newVal;
	}

	@Override
	public boolean gidFound() {

		return gidFound;
	}

	public void gidFound(final boolean newVal) {

		gidFound = newVal;
	}

	public void setSource(final FileSet source) {

		this.source = source;
		setChanged();
	}

	@Override
	public String getSource() {

		if (source == null) {
			return "No Data Loaded";
		}
		
		return source.getCdt();
	}

	@Override
	public String getName() {

		return getFileSet().getRoot();
	}

	@Override
	public FileSet getFileSet() {

		return source;
	}

	@Override
	public void clearFileSetListeners() {

		source.clearFileSetListeners();
	}

	@Override
	public void addFileSetListener(final FileSetListener listener) {

		source.addFileSetListener(listener);
	}

	public Preferences getDocumentConfig() {

		return documentConfig;
	}

	@Override
	public Preferences getDocumentConfigRoot() {

		return Preferences.userRoot();
	}

	public void setDocumentConfig(final Preferences newVal) {

		documentConfig = newVal;
	}

	protected void hashAIDs() {

		colLabelInfo.hashIDs("AID");
	}

	protected void hashGIDs() {

		rowLabelInfo.hashIDs("GID");
	}

	protected void hashATRs() {

		atrLabelInfo.hashIDs("NODEID");
	}

	protected void hashGTRs() {

		gtrLabelInfo.hashIDs("NODEID");
	}

	protected static Hashtable<String, Integer> populateHash(
		final LabelInfo source, final String labelType,
		final Hashtable<String, Integer> target) {

		final int indexCol = source.getIndex(labelType);

		return populateHash(source, indexCol, target);
	}

	protected static Hashtable<String, Integer> populateHash(
			final LabelInfo source, int indexCol,
			Hashtable<String, Integer> target) {

		if (target == null) {
			target = new Hashtable<String, Integer>(
				(source.getNumLabels() * 4) / 3, .75f);

		} else {
			target.clear();
		}

		if (indexCol < 0) {
			indexCol = 0;
		}

		for (int i = 0; i < source.getNumLabels(); i++) {
			target.put(source.getLabels(i)[indexCol], new Integer(i));
		}

		return target;
	}

	/**
	 * Reorders all the arrays in the new ordering.
	 *
	 * @param ordering
	 *            the new ordering of arrays, must have size equal to number of
	 *            arrays.
	 */
	public void reorderColumns(final int[] ordering) {
		if (ordering == null
				|| ordering.length != dataMatrix.getNumUnappendedCol())
			return;

		final DataMatrix data = getDataMatrix();

		final double[] temp = new double[data.getNumUnappendedCol()];
		for (int j = 0; j < data.getNumRow(); j++) {
			for (int i = 0; i < ordering.length; i++) {
				temp[i] = data.getValue(ordering[i], j);
			}

			for (int i = 0; i < ordering.length; i++) {
				data.setValue(temp[i], i, j);
			}
		}

		final String[][] colLabels = colLabelInfo.getLabelArray();
		final String[][] temp2 = new String[colLabels.length][];

		for (int i = 0; i < colLabels.length; i++) {
			if (i < ordering.length) {
				temp2[i] = colLabels[ordering[i]];

			} else {
				temp2[i] = colLabels[i];
			}
		}

		setColumnLabels(temp2);
		hashAIDs();
		setChanged();
	}

	/**
	 * Reorders all the arrays in the new ordering.
	 *
	 * @param ordering
	 *            the new ordering of arrays, must have size equal to number of
	 *            arrays
	 */
	public void reorderRows(final int[] ordering) {

		if (ordering == null || ordering.length != dataMatrix.getNumRow()) {
			return;
		}

		final DataMatrix data = getDataMatrix();
		final double[] temp = new double[data.getNumRow()];
		for (int j = 0; j < data.getNumUnappendedCol(); j++) {
			for (int i = 0; i < ordering.length; i++) {
				temp[i] = data.getValue(j, ordering[i]);
			}

			for (int i = 0; i < ordering.length; i++) {
				data.setValue(temp[i], j, i);
			}
		}
		rowLabelInfo.reorderLabels(ordering);
		hashGIDs();
		setChanged();
	}

	public void resetState() {

		LogBuffer.println("Resetting model.");

		// reset some state stuff.
		// if (documentConfig != null)
		// documentConfig.store();
		documentConfig = null;
		setLoaded(false);
		aidFound = false;
		gidFound = false;
		source = null;

		eweightFound = false;
		gweightFound = false;

		rowLabelInfo.clear();
		colLabelInfo.clear();
		atrLabelInfo.clear();
		gtrLabelInfo.clear();
		dataMatrix.clear();
	}

	@Override
	public String toString() {

		final String[] strings = toStrings();
		String msg = "";
		for (final String string : strings) {
			msg += string + "\n";
		}
		return msg;
	}

	public String[] toStrings() {

		final String[] msg = { "Selected TVModel Stats",
				"Source = " + getSource(), "NCols   = " + nCols(),
				"NRowLabelTypes = " + getRowLabelInfo().getNumLabelTypes(),
				"NRows   = " + nRows(), "eweight  = " + eweightFound,
				"gweight  = " + gweightFound, "aid  = " + aidFound,
				"gid  = " + gidFound };

		/*
		 * Enumeration e = genePrefix.elements(); msg += "GPREFIX: " +
		 * e.nextElement(); for (; e.hasMoreElements() ;) { msg += " " +
		 * e.nextElement(); }
		 * 
		 * e = aHeaders.elements(); msg += "\naHeaders: " + e.nextElement(); for
		 * (; e.hasMoreElements() ;) { msg += ":" + e.nextElement(); }
		 */

		return msg;
	}

	@Override
	public void removeAppended() {

		// if (appendIndex == -1) {
		//
		// return;
		// }
		//
		// final int ngene = nGene();
		// int nexpr = nExpr();
		// final double[] temp = new double[ngene * appendIndex];
		//
		// int i = 0;
		//
		// for (int g = 0; g < this.dataMatrix.getNumRow(); g++) {
		// for (int e = 0; e < nexpr; e++) {
		// if (e < appendIndex) {
		// temp[i++] = getValue(e, g);
		// }
		// }
		// }
		// dataMatrix.setExprData(temp);
		//
		// final String[][] tempS = new String[appendIndex][];
		//
		// for (int j = 0; j < appendIndex; j++) {
		// tempS[j] = arrayHeaderInfo.getHeader(j);
		// }
		//
		// arrayHeaderInfo.setHeaderArray(tempS);
		// nexpr = appendIndex;
		// appendIndex = -1;
		// setChanged();
	}

	/**
	 * Appends a second matrix to this one provided they have the same height.
	 * Used for comparison of two data sets where the data is displayed side by
	 * side.
	 *
	 */
	@Override
	public void append(final DataModel m) {

		// final int ngene = nGene();
		// int nexpr = nExpr();
		// if (m == null || m.getDataMatrix().getNumRow() != ngene
		// || appendIndex != -1) {
		// System.out.println("Could not compare.");
		// return;
		// }
		//
		// final double[] temp = new double[getDataMatrix().getNumRow()
		// * getDataMatrix().getNumCol() + m.getDataMatrix().getNumRow()
		// * (m.getDataMatrix().getNumCol() + 1)];
		//
		// int i = 0;
		//
		// for (int g = 0; g < m.getDataMatrix().getNumRow(); g++) {
		// for (int e = 0; e < nexpr + m.getDataMatrix().getNumCol() + 1; e++) {
		//
		// if (e < nexpr) {
		// temp[i++] = getValue(e, g);
		//
		// } else if (e < nexpr + 1) {
		// temp[i++] = DataModel.NODATA;
		//
		// } else {
		// temp[i++] = m.getDataMatrix().getValue(e - nexpr - 1, g);
		// }
		// }
		// }
		//
		// final String[][] tempS = new String[getArrayHeaderInfo()
		// .getNumHeaders() + m.getArrayHeaderInfo().getNumHeaders() + 1][];
		//
		// i = 0;
		// for (int j = 0; j < getArrayHeaderInfo().getNumHeaders(); j++) {
		// tempS[i++] = getArrayHeaderInfo().getHeader(j);
		// }
		//
		// tempS[i] = new String[getArrayHeaderInfo().getNumNames()];
		//
		// for (int j = 0; j < tempS[i].length; j++) {
		// tempS[i][j] = "-----------------------";
		// }
		//
		// i++;
		//
		// for (int j = 0; j < getArrayHeaderInfo().getNumHeaders(); j++) {
		// tempS[i++] = getArrayHeaderInfo().getHeader(j);
		// }
		//
		// arrayHeaderInfo.setHeaderArray(tempS);
		// appendIndex = nexpr;
		// nexpr += m.getDataMatrix().getNumCol() + 1;
		// dataMatrix.setExprData(temp);
		// setChanged();
	}

	/**
	 * Really just a thin wrapper around exprData array.
	 *
	 * @author aloksaldanha
	 *
	 */
	public class TVDataMatrix implements DataMatrix {

		private boolean modified = false;
		private double[][] exprData = null;
		private double minVal = Double.MAX_VALUE;
		private double maxVal = Double.MIN_VALUE;
		private double mean = Double.NaN;
		private double median = Double.NaN;

		/**
		 * Sets all member variables of the DataMatrix instance to their initial default values.
		 */
		public void clear() {

			modified = false;
			exprData = null;
			minVal = Double.MAX_VALUE;
			maxVal = Double.MIN_VALUE;
			mean = Double.NaN;
			median = Double.NaN;
		}

		/**
		 * Sets all zero values (0.0) in the data set to the defined very large
		 * double for missing data. Zeroes are therefore set to 'missing'
		 * status. They will be ignored during clustering and colored like
		 * missing values.
		 */
		public void setZeroesToMissing() {

			for (int i = 0; i < exprData.length; i++) {
				for (int j = 0; j < exprData[i].length; j++) {
					
					if (Helper.nearlyEqual(0.0, exprData[i][j])) {
						exprData[i][j] = DataModel.NAN;
					}
				}
			}
		}

		/** 
		 * Finds the maximum and minimum values in the data, as well as
		 * mean, median.
		 */
		@Override
		public void calculateBaseValues() {

			// Double.MAX_VALUE is multiplied by -1 because data can be negative and we want to start with the 
			// smallest value possible for finding a maximum in a data array. Double.MIN_VALUE is the smallest
			// positive value.
			double maxVal = -1.0 * Double.MAX_VALUE;
			double minVal = Double.MAX_VALUE;
			double roundedMean = Double.NaN;
			double roundedMedian = Double.NaN;
			
			if (exprData == null) {
			    LogBuffer.println("Could not calculate base values for data (min, max, median, mean). "
			    		+ "Data matrix was null.");
			    return;
			}
			
			final int nRows = nRows();
			final int nCols = nCols();
			
			double sum = 0;
			int skipped = 0;

			int i = 0, j = 0;
			for (i = 0; i < nRows; i++) {
				for (j = 0; j < nCols; j++) {
					
					final double dataPoint = exprData[i][j];
					
					if(!Double.isNaN(dataPoint) 
							&& !Double.isInfinite(dataPoint)) {
						sum += dataPoint;
						
						if (dataPoint > maxVal) {
							maxVal = dataPoint;
						}
						
						if (dataPoint < minVal) {
							minVal = dataPoint;
						}
					} else {
						skipped++;
					}
				}
			}
				
			roundedMean = calculateMean(sum, skipped);
			roundedMedian = calculateMedian(exprData);
			
			LogBuffer.println("Setting base values.");
			setMinVal(minVal);
			setMaxVal(maxVal);
			setMean(roundedMean);
			setMedian(roundedMedian);
		}
		
		/**
		 * return the mean of data points in a zoomed matrix
		 * @param startingRow included in the mean calculation
		 * @param endingRow included in the mean calculation
		 * @param startingCol included in the mean calculation
		 * @param endingRow included in the mean calculation
		 */
		public double getZoomedMean(int startingRow, int endingRow, int startingCol, int endingCol){
			final int nRows = nRows();
			final int nCols = nCols();
			if (exprData == null) {
		    LogBuffer.println("Could not calculate Zommed mean. "
		    		+ "Data matrix was null.");
		    return 0;
		  }
			if (startingRow<0 || endingRow>=nRows || startingCol<0 || endingCol>=nCols) {
		    LogBuffer.println("Could not calculate Zommed mean. "
		    		+ "Indexes are out of range.");
		    return 0;
		  }
			
			double roundedMean = Double.NaN;
			double sum = 0;
			int skipped = 0;

			for (int i = startingRow; i <= endingRow; i++) {
				for (int j = startingCol; j <= endingCol; j++) {
					
					final double dataPoint = exprData[i][j];
					
					if(!Double.isNaN(dataPoint) 
							&& !Double.isInfinite(dataPoint)) {
						sum += dataPoint;
						
						if (dataPoint > maxVal) {
							maxVal = dataPoint;
						}
						
						if (dataPoint < minVal) {
							minVal = dataPoint;
						}
					} else {
						skipped++;
					}
				}
			}
		  
      roundedMean = calculateZoomMean(startingRow, endingRow, startingCol, endingCol, sum, skipped);
			return roundedMean;
		}

		/** calculates the mean of a subset of the data matrix
		 * @param startingRow
		 * @param endingRow
		 * @param startingCol
		 * @param endingCol
		 * @param sum
		 * @param skipped
		 * @return
		 */
		private double calculateZoomMean(	int startingRow, int endingRow,
																			int startingCol, int endingCol,
																			double sum, int skipped) {
			double roundedMean;
			
			double mean;
			int numDataPoints = ((endingRow-startingRow+1) * (endingCol-startingCol+1)) - skipped;
			
			if(numDataPoints < 1) {
				LogBuffer.println("Not enough data points for calculation of mean: " + numDataPoints);
				return Double.NaN;
			}
			
			mean = sum / numDataPoints;
			
			roundedMean = Helper.roundDouble(mean, 4);
			return roundedMean;
		}
		
		/**
		 * Finds the median value of a 2D double array.
		 * @param data
		 * @return Median value.
		 */
		private double calculateMedian(double[][] data) {
			
			LogBuffer.println("Calculating median.");
			
			double result = Double.NaN;
			
			final int nRows = nRows();
			final int nCols = nCols();
			
			/* ROW ORDER 
			 * Although allocating a second array kills RAM for large
			 * matrices it needs to be done. Sorting the data is required
			 * for median and the original data cannot be disturbed...
			 */
			double[] newData = new double[nRows * nCols];
			
			for (int i = 0; i < nRows; i++) {
				for (int j = 0; j < nCols; j++) {
					newData[nCols * i + j] = exprData[i][j];
				}
			}
			Arrays.sort(newData); /* Good night cpu...*/
			
			newData = truncateSortedData(newData);
			
			/* Even length case */
			if(newData.length % 2 == 0) {
				int idxLeft = newData.length / 2;
				int idxRight = (newData.length / 2) + 1;
				result = (newData[idxLeft] + newData[idxRight]) / 2;
				
			} else {
				result = newData[newData.length / 2];
			}
			
			return Helper.roundDouble(result, 4);
		}
		
		/**
		 * This method truncates a sorted array at the first occurrence of
		 * NaN or Infinity as defined in the Double class.
		 * @param data
		 * @return A truncated data array.
		 */
		private double[] truncateSortedData(final double[] data) {
			
			LogBuffer.println("Truncating sorted data array.");
			
			int idx = data.length;
			
			// find first NaN or Infinity value
			for(int i = 0; i < data.length; i++) {
				
				double dataPoint = data[i];
				if(Double.isNaN(dataPoint) 
						|| Double.isInfinite(dataPoint)) {
					idx = i;
					break;
				}
			}
			
			double[] correctedData = Arrays.copyOfRange(data, 0, idx);
			
			return correctedData;
		}
		
		/**
		 * Helper for calculateBaseValues().
		 * Finds the mean value of a 2D double array given the sum of the data values and the amount of skipped values.
		 * This doesn't calculate the mean from the data itself because during search min and max values, the sum is
		 * already created. This way, another walk of the data arrays can be avoided.
		 * @param sum - Sum of all data values.
		 * @param skipped - Amount of skipped data array entries.
		 * @return Mean value.
		 */
		private double calculateMean(double sum, int skipped) {
			
			LogBuffer.println("Calculating mean.");
			
			double mean;
			int numDataPoints = (nRows() * nCols()) - skipped;
			
			if(numDataPoints < 1) {
				LogBuffer.println("Not enough data points for calculation of mean: " + numDataPoints);
				return Double.NaN;
			}
			
			mean = sum / numDataPoints;
			
			return Helper.roundDouble(mean, 4);
		}
		
		@Override
		public void setMinVal(final double newMinVal) {

			this.minVal = newMinVal;
		}

		@Override
		public void setMaxVal(final double newMaxVal) {

			this.maxVal = newMaxVal;
		}
		
		@Override
		public void setMean(final double newMeanVal) {

			this.mean = newMeanVal;
		}
		
		@Override
		public void setMedian(final double newMedianVal) {

			this.median = newMedianVal;
		}

		@Override
		public double getMinVal() {

			return minVal;
		}

		@Override
		public double getMaxVal() {

			return maxVal;
		}
		
		@Override
		public double getMean() {

			return mean;
		}
		
		@Override
		public double getMedian() {

			return median;
		}

		@Override
		public double getValue(final int x, final int y) {

			final int nexpr = nCols();
			final int ngene = nRows();
			
			if ((x < nexpr) && (y < ngene) && (x >= 0) && (y >= 0)) {
				return exprData[y][x];
			}
			
			return DataModel.NAN;
		}

		public void setExprData(final double[][] newData) {

			exprData = newData;
		}

		public double[][] getExprData() {

			return exprData;
		}

		@Override
		public void setValue(final double value, final int x, final int y) {

			exprData[x][y] = value;
			setModified(true);
			setChanged();
		}

		@Override
		public int getNumRow() {

			return nRows();
		}

		@Override
		public int getNumCol() {

			return nCols();
		}

		@Override
		public int getNumUnappendedCol() {

			return appendIndex == -1 ? getNumCol() : appendIndex;
		}

		@Override
		public int getNumUnappendedRow() {

			return appendIndex == -1 ? getNumRow() : appendIndex;
		}

		@Override
		public void setModified(final boolean modified) {

			this.modified = modified;
		}

		@Override
		public boolean getModified() {

			return modified;
		}
		
		/*
		 * returns the average of a set of Rows - fromRow to toRow(included)
		 * (non-Javadoc)
		 * @see edu.stanford.genetics.treeview.DataMatrix#getRowAverage(int, int)
		 */
		public double getRowAverage(int fromRowId, int toRowId){
			double sum = 0;
			int count = 0;
			final int numOfCols = nCols();
			for( int j=fromRowId ; j<=toRowId; j++ ){
				for(int i=0 ; i<numOfCols ; i++){
					double value = getValue(i, j);
					if(!Double.isNaN(value)){
						sum += value;
						count++;
					}
				}
			}
			if(count == 0)
				// TODO: if all the data is NaN, is this 0 or NaN?
				return Double.NaN;
			else{
				double avg = sum/count; 
				return Helper.roundDouble(avg, 4);
			}
		}
		
		/*
		 * returns the average of a set of Columns - fromcolumn to toColumn(included)
		 * (non-Javadoc)
		 * @see edu.stanford.genetics.treeview.DataMatrix#getColAverage(int, int)
		 */
		public double getColAverage(int fromColId, int toColId){
			double sum = 0;
			int count = 0;
			final int numOfRows = nRows();
			for( int j=fromColId ; j<=toColId; j++ ){
				for(int i=0 ; i<numOfRows ; i++){
					double value = getValue(j, i);
					if(!Double.isNaN(value)){
						sum += value;
						count++;
					}
				}	
			}
			if(count == 0)
				return Double.NaN;
			else{
				double avg = sum/(count); 
				return Helper.roundDouble(avg, 4);
			}
		}
	}

	/** holds actual node information for column tree */
	public void setAtrLabels(final String[][] atrLabels) {

		atrLabelInfo.setLabelArray(atrLabels);
	}

	/** holds label types from atr file */
	public void setAtrLabelTypes(final String[] atrLabelTypes) {

		atrLabelInfo.setLabelTypeArray(atrLabelTypes);
	}

	/** holds actual node information for row tree */
	public void setGtrLabels(final String[][] gtrLabels) {

		gtrLabelInfo.setLabelArray(gtrLabels);
	}

	public void setGtrLabelTypes(final String[] gtrLabelTypes) {

		gtrLabelInfo.setLabelTypeArray(gtrLabelTypes);
	}

	public void setColumnLabels(final String[][] newLabels) {

		colLabelInfo.setLabelArray(newLabels);
	}

	public void setColumnLabelTypes(final String[] newLabelTypes) {

		colLabelInfo.setLabelTypeArray(newLabelTypes);
	}

	class RowLabelInfo extends IntLabelInfo {

		/*
		TODO ... oh god this all just throws out fixed values.
		*/
		public int getYorfIndex() {

			if (getIndex("GID") == -1) {
				return 0;
			}

			return 1;
		}

		public int getNameIndex() {

			if (getIndex("GID") == -1) {
				return 1;
			}

				return 2;
		}

		/**
		 * There are two special indexes, YORF and NAME.
		 */
		@Override
		public int getIndex(final String labelType) {

			final int retval = super.getIndex(labelType);

			if (retval != -1) {
				return retval;
			}

			if (labelType.equals("YORF")) {
				return getYorfIndex();
			}

			if (labelType.equals("NAME")) {
				return getNameIndex();
			}

			return -1;
		}
	}

	public void setRowLabelTypes(final String[] newLabelTypes) {

		rowLabelInfo.setLabelTypeArray(newLabelTypes);
	}

	public void setRowLabels(final String[][] newLabels) {

		rowLabelInfo.setLabelArray(newLabels);
	}

	/**
	 * @param b
	 */
	public void setEweightFound(final boolean b) {

		eweightFound = b;
	}

	/**
	 * @param b
	 */
	public void setGweightFound(final boolean b) {

		gweightFound = b;
	}

	@Override
	public boolean getModified() {

		return getGtrLabelInfo().getModified() 
				|| getAtrLabelInfo().getModified();
	}

	@Override
	public boolean isLoaded() {

		return loaded;
	}

	public void setLoaded(final boolean loaded) {

		this.loaded = loaded;
		this.notifyObservers(loaded);
	}

	@Override
	public String getFileName() {
		
		if (source == null) {
			return "No Data Loaded";
		}

		return source.getRoot() + source.getExt();
	}
}
