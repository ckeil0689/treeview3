package edu.stanford.genetics.treeview.model;

public class DataLoadInfo {

	private int[] dataCoords;
	private String delimiter;

	public DataLoadInfo(int[] dataCoords, final String delimString) {

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
