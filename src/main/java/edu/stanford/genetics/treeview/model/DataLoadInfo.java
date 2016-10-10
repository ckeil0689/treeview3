package edu.stanford.genetics.treeview.model;

import java.util.Arrays;
import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;

public class DataLoadInfo {

	public final static String DEFAULT_DELIM = "\\t";
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

		if(oldNode == null) {
			LogBuffer.println("Could not update new data start coordinates " +
												"information in the Preferences because no node was defined in " +
												"the DataLoadInfo object for this file.");
			return;
		}

		oldNode.putInt("rowCoord", dataCoords[0]);
		oldNode.putInt("colCoord", dataCoords[1]);
	}

	public String[] getRowLabelTypes() {
		return rowLabelTypes;
	}

	public String[] getColLabelTypes() {
		return colLabelTypes;
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

		setOldNode(node);

		this.delimiter = node.get("delimiter", DataLoadInfo.DEFAULT_DELIM);

		this.dataCoords[0] = node.getInt("rowCoord", 0);
		this.dataCoords[1] = node.getInt("colCoord", 0);
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
