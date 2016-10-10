package edu.stanford.genetics.treeview.model;

import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;

public class DataLoadInfo {

	private Preferences oldNode;
	private FileSet oldFileSet;
	
	private boolean isClusteredFile = false;
	private int[] dataCoords;
	private String delimiter;

	/**
	 * Derive a <code>DataLoadInfo</code> object from a <code>Preferences</code> 
	 * node which contains stored settings for data loading. 
	 * @param node - The <code>Preferences</code> node with the stored data 
	 * settings.
	 */
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

	public String getDelimiter() {
		return delimiter;
	}
	
	/**
	 * Imports delimiter and data start coordinates from a supplied 
	 * <code>Preferences</code> node.
	 * @param node - The <code>Preferences</code> node which contains information 
	 * about a particular file's loading settings.
	 */
	private void importDataFromNode(final Preferences node) {
		
		if(dataCoords == null) {
		   LogBuffer.println("No array for data coordinates has been defined in " +
		   	getClass().getSimpleName() + ". Cannot import old settings.");
		   return;
		}
		
		setOldNode(node);
		
		this.delimiter = node.get("delimiter", ModelLoader.DEFAULT_DELIM);
		
		this.dataCoords[0] = node.getInt("rowCoord", 0);
		this.dataCoords[1] = node.getInt("colCoord", 0);
	}
	
}
