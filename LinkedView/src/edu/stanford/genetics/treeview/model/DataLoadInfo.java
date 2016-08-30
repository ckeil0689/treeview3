package edu.stanford.genetics.treeview.model;

import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.FileSet;

public class DataLoadInfo {

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

	public String getDelimiter() {
		return delimiter;
	}
}
