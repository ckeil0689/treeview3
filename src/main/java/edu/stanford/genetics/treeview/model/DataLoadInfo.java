package edu.stanford.genetics.treeview.model;

import java.util.Arrays;
import java.util.prefs.Preferences;

import Utilities.Helper;
import Views.DataImportController;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;

public class DataLoadInfo {

	public final static String[] DEFAULT_LABEL_TYPES = {""};

	private Preferences oldNode;
	private FileSet oldFileSet;

	private boolean isClusteredFile = false;
	private int[] dataCoords;
	private String[] rowLabelTypes;
	private String[] colLabelTypes;
	private String delimiter;

	/** Derive a <code>DataLoadInfo</code> object from a <code>Preferences</code>
	 * node which contains stored settings for data loading.
	 * 
	 * @param node - The <code>Preferences</code> node with the stored data
	 *          settings. */
	public DataLoadInfo(final Preferences node) {

		this.dataCoords = new int[2];
		importDataFromNode(node);
	}

	public DataLoadInfo(int[] dataCoords, final String delimString) {

		this.dataCoords = dataCoords;
		this.delimiter = delimString;
	}

	public void setOldFileSet(final FileSet fs) {
		this.oldFileSet = fs;
	}

	public FileSet getOldFileSet() {
		return oldFileSet;
	}

	public void setOldNode(Preferences oldNode) {
		this.oldNode = oldNode;
	}

	public void setIsClusteredFile(boolean isClusteredFile) {
		this.isClusteredFile = isClusteredFile;
	}

	public boolean isClusteredFile() {
		return isClusteredFile;
	}

	public Preferences getOldNode() {
		return oldNode;
	}

	public int[] getDataCoords() {
		return dataCoords;
	}

	public int getDataStartRow() {

		if(dataCoords == null || dataCoords.length != 2) {
			LogBuffer.println("Could not get data start row index from DataLoadInfo.");
			return -1;
		}

		return dataCoords[0];
	}

	public void setDataStartRow(final int dataStartRow) {

		if(dataCoords == null || dataCoords.length != 2) {
			LogBuffer.println("Could not set data start row index in DataLoadInfo.");
			return;
		}

		dataCoords[0] = dataStartRow;
	}

	public int getDataStartCol() {

		if(dataCoords == null || dataCoords.length != 2) {
			LogBuffer.println("Could not get data start column index from DataLoadInfo.");
			return -1;
		}

		return dataCoords[1];
	}

	public void setDataStartCol(final int dataStartCol) {

		if(dataCoords == null || dataCoords.length != 2) {
			LogBuffer.println("Could not set data start column index in DataLoadInfo.");
			return;
		}

		dataCoords[1] = dataStartCol;
	}

	/** Update the data start coordinate information.
	 * 
	 * @param newDataCoords - The new data start coordinates. */
	public void setDataStartCoords(final int[] newDataCoords) {

		if(dataCoords == null || dataCoords.length != 2) {
			LogBuffer.println("Problem with the data coordinates array in " +
												"DataLoadInfo. Cannot update the array.");
		}

		this.dataCoords = newDataCoords;
	}

	public String[] getRowLabelTypes() {
		return rowLabelTypes;
	}

	public String[] getColLabelTypes() {
		return colLabelTypes;
	}

	public String getRowLabelTypesAsString() {
		return Arrays.toString(rowLabelTypes);
	}

	public String getColLabelTypesAsString() {
		return Arrays.toString(colLabelTypes);
	}

	public void setRowLabelTypes(String[] newRowLabelTypes) {
		this.rowLabelTypes = newRowLabelTypes;
	}

	public void setColLabelTypes(String[] newColLabelTypes) {
		this.colLabelTypes = newColLabelTypes;
	}

	public String getDelimiter() {
		return delimiter;
	}

	/** Based on the supplied coordinate array it tests whether an update to the
	 * data coordinate array might be necessary.
	 * 
	 * @param newCoords - The new coordinate array which is checked against the
	 *          existing coordinate array.
	 * @return Whether the coordinate array should be updated. */
	public boolean needsDataCoordsUpdate(final int[] newCoords) {

		if(newCoords == null || newCoords.length != 2) { return false; }

		if((newCoords[0] > dataCoords[0]) ||
				(newCoords[1] > dataCoords[1])) { return true; }

		return false;
	}

	/** Imports delimiter and data start coordinates from a supplied
	 * <code>Preferences</code> node.
	 * 
	 * @param node - The <code>Preferences</code> node which contains information
	 *          about a particular file's loading settings. */
	private void importDataFromNode(final Preferences node) {

		if(dataCoords == null) {
			LogBuffer.println("No array for data coordinates has been defined in " +
												getClass().getSimpleName() +
												". Cannot import old settings.");
			return;
		}
		
		if(node == null) {
			LogBuffer.println("Preferences node is null. Cannot import data.");
			return;
		}

		setOldNode(node);

		this.delimiter = node.get("delimiter", DataImportController.TAB_DELIM);

		this.dataCoords[0] = node.getInt("rowCoord", 0);
		this.dataCoords[1] = node.getInt("colCoord", 0);

		String dLabelTypes = Arrays.toString(DataLoadInfo.DEFAULT_LABEL_TYPES);
		String rowLabelTypes = node.get("rowLabelTypes", dLabelTypes);
		String colLabelTypes = node.get("colLabelTypes", dLabelTypes);

		this.rowLabelTypes = Helper.getStringValuesFromKeyString(rowLabelTypes);
		this.colLabelTypes = Helper.getStringValuesFromKeyString(colLabelTypes);
	}

	public void setDelimiter(final String newDelimiter) {
		this.delimiter = newDelimiter;
	}

	@Override
	public String toString() {
		return "DataLoadInfo [oldNode=" +	oldNode + ", oldFileSet=" + oldFileSet +
						", isClusteredFile=" + isClusteredFile + ", dataCoords=" + Arrays
																																							.toString(dataCoords) +
						", rowLabelTypes=" + Arrays.toString(rowLabelTypes) +
						", colLabelTypes=" + Arrays.toString(colLabelTypes) +
						", delimiter=" + delimiter + "]";
	}
}
