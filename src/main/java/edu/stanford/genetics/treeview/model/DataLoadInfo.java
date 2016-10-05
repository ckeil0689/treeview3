package edu.stanford.genetics.treeview.model;

import java.util.Arrays;
import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;

public class DataLoadInfo {

	public final static String DEFAULT_DELIM = "\\t";
	
	private Preferences oldNode;
	private FileSet oldFileSet;
	
	private boolean isClusteredFile = false;
	private int[] dataCoords;
	private String delimiter;

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

	public String getDelimiter() {
		return delimiter;
	}
	
	public void setDelimiter(final String newDelimiter) {
		this.delimiter = newDelimiter;
	}

	@Override
	public String toString() {
		return "DataLoadInfo [oldNode=" + oldNode + ", oldFileSet=" + oldFileSet + ", isClusteredFile="
				+ isClusteredFile + ", dataCoords=" + Arrays.toString(dataCoords) + ", delimiter=" + delimiter + "]";
	}
}
