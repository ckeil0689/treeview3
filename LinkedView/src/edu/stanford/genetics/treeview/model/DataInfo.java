package edu.stanford.genetics.treeview.model;

public class DataInfo {

	private int[] dataCoords;
	private String delimiter;
	
	public DataInfo(int[] dataCoords, final String delimString) {
		
		this.dataCoords = dataCoords;
		this.delimiter = delimString;
	}
	
	public int[] getDataCoords() {
		
		return dataCoords;
	}
	
	public String getDelimiter() {
		
		return delimiter;
	}
}
