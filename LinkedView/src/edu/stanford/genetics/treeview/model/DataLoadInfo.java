package edu.stanford.genetics.treeview.model;

public class DataLoadInfo {

	private boolean isClusteredFile = false;
	private int[] dataCoords;
	private String delimiter;

	public DataLoadInfo(int[] dataCoords, final String delimString) {

		this.dataCoords = dataCoords;
		this.delimiter = delimString;
	}
	
	public void setIsClusteredFile(boolean isClusteredFile) {
		
		this.isClusteredFile = isClusteredFile;
	}

	public boolean isClusteredFile() {
		
		return isClusteredFile;
	}
	
	public int[] getDataCoords() {

		return dataCoords;
	}

	public String getDelimiter() {

		return delimiter;
	}
}
