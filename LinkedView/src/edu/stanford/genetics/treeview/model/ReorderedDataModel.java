/*
 * Created on Mar 5, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.model;

import java.util.Observable;
import java.util.Observer;

import Cluster.ClusterFileSet;

import edu.stanford.genetics.treeview.*;

/**
 * 
 * This class produces a reordered version of the parent DataModel
 * It can a subset, if the integer arrays passed in have fewer members, or a 
 * superset, if the arrays passed in have more members.
 * Gaps can be introduced between genes or arrays by inserting "-1" at an index.
 * 
 * @author aloksaldanha
 *
 */
public class ReorderedDataModel extends Observable implements DataModel {
	/**
	 * @author aloksaldanha
	 *
	 */
	private class SubDataMatrix implements DataMatrix {
		
		@Override
		public double getValue(int col, int row) {
			if (geneIndex != null) row = geneIndex[row];
			if (arrayIndex != null) col = arrayIndex[col];
			if ((row == -1) || (col == -1)) {
				return DataModel.EMPTY;
			} else {
				return parent.getDataMatrix().getValue(col, row);
			}
		}

		@Override
		public void setValue(double value, int col, int row) {
			if (geneIndex != null) row = geneIndex[row];
			if (arrayIndex != null) col = arrayIndex[col];
			if ((row == -1) || (col == -1)) {
				return;
			} else {
				LogBuffer.getSingleton().log("Error: cannot modifiy " +
						"reordered data model");
				return;
			}
		}

		@Override
		public int getNumRow() {
			if (geneIndex != null) 
				return geneIndex.length;
			else
				return parent.getDataMatrix().getNumRow();
		}

		@Override
		public int getNumCol() {
			if (arrayIndex != null) 
				return arrayIndex.length;
			else
				return parent.getDataMatrix().getNumCol();
		}

		@Override
		public int getNumUnappendedCol() {
			return parent.getDataMatrix().getNumUnappendedCol();
		}

		@Override
		public boolean getModified() {
			return false;
		}

		@Override
		public void setModified(boolean b) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int getNumUnappendedRow() {
			// TODO Auto-generated method stub
			return 0;
		}

	}
	

	/**
	 * 
	 * Represents reordered HeaderInfo of parent.
	 */
	private class ReorderedHeaderInfo implements HeaderInfo {
		private HeaderInfo parentHeaderInfo;
		int [] reorderedIndex;
		private ReorderedHeaderInfo(HeaderInfo hi, int [] ri) {
			parentHeaderInfo = hi;
			reorderedIndex = ri;
		}
		@Override
		public String[] getHeader(int i) {
			int index = reorderedIndex[i];
			if (index == -1)
				return null;
			return parentHeaderInfo.getHeader(index);
		}

		@Override
		public String getHeader(int i, String name) {
			int index = reorderedIndex[i];
			if (index == -1)
				return null;
			return parentHeaderInfo.getHeader(index, name);
		}
		@Override
		public String getHeader(int rowIndex, int columnIndex) {
			String [] header = getHeader(rowIndex);
			if (header != null)
				return header[columnIndex];
			else 
				return "";
		}

		@Override
		public String[] getNames() {
			return parentHeaderInfo.getNames();
		}

		@Override
		public int getNumNames() {
			return parentHeaderInfo.getNumNames();
		}

		@Override
		public int getNumHeaders() {
			return reorderedIndex.length;
		}

		@Override
		public int getIndex(String name) {
			return parentHeaderInfo.getIndex(name);
		}

		@Override
		public int getHeaderIndex(String id) {
			int parentIndex = parentHeaderInfo.getHeaderIndex(id);
			if (reorderedIndex[parentIndex] == parentIndex) 
				return parentIndex;
			else {
				for (int i = 0; i < reorderedIndex.length; i++)
					if (reorderedIndex[i] == parentIndex)
						return i;
			}
			return -1;
		}
		@Override
		public void addObserver(Observer o) {
			parentHeaderInfo.addObserver(o);
		}
		@Override
		public void deleteObserver(Observer o) {
			parentHeaderInfo.deleteObserver(o);
		}
		@Override
		public boolean addName(String name, int location) {return false;}
		@Override
		public boolean setHeader(int i, String name, String value) {return false;}
		@Override
		public boolean getModified() {return false;}
		@Override
		public void setModified(boolean mod) {}		
	}

	/**
	 * Builds data model which corresponds to a reordered version of the source datamodel, 
	 * as specified by geneIndex
	 * 
	 * @param source
	 * @param geneIndex
	 */
	public ReorderedDataModel(DataModel source, int [] geneIndex) {
		this(source, geneIndex, null);
	}
	/**
	 * Builds data model which corresponds to a reordered version of the source datamodel, 
	 * as specified by geneIndex and arrayIndex.
	 * 
	 * @param source
	 * @param geneIndex
	 */
	public ReorderedDataModel(DataModel source, int [] geneIndex, int [] arrayIndex) {
		this.geneIndex = geneIndex;
		this.arrayIndex = arrayIndex;
		if (geneIndex != null) {
			GeneHeaderInfo = new ReorderedHeaderInfo(source.getGeneHeaderInfo(), geneIndex);
			GtrHeaderInfo = new ReorderedHeaderInfo(source.getGtrHeaderInfo(), geneIndex);
		}
		if (arrayIndex != null) {
			ArrayHeaderInfo = new ReorderedHeaderInfo(source.getArrayHeaderInfo(), arrayIndex);
			AtrHeaderInfo = new ReorderedHeaderInfo(source.getAtrHeaderInfo(), arrayIndex);
		}
		
		this.parent =source;
		this.source = "Subset " +parent.getSource();
		this.name = "Subset of " +parent.getName();
		// this should really be set to a clone of the parent's document config.
		documentConfig = source.getDocumentConfigRoot();
	}
	private HeaderInfo GtrHeaderInfo;
	private HeaderInfo GeneHeaderInfo;
	private HeaderInfo AtrHeaderInfo;
	private HeaderInfo ArrayHeaderInfo;
	private DataMatrix subDataMatrix = new SubDataMatrix();
	private DataModel parent;
	private int [] geneIndex;
	private int [] arrayIndex;
	private ConfigNode documentConfig = new DummyConfigNode("SubDataModel");
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.DataModel#getDocumentConfig()
	 */
	@Override
	public ConfigNode getDocumentConfigRoot() {
		return documentConfig;
	}

	String source;
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.DataModel#getSource()
	 */
	@Override
	public String getSource() {
		return source;
	}
	public void setSource(String string) {
		source = string;
	}
	String name;
	@Override
	public String getName() {
		return name;
	}
	public void setName(String string) {
		name = string;
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.DataModel#setModelForCompare(edu.stanford.genetics.treeview.DataModel)
	 */
	@Override
	public void setModelForCompare(DataModel dm) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.DataModel#getFileSet()
	 */
	@Override
	public FileSet getFileSet() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void clearFileSetListeners() {
	}
	@Override
	public void addFileSetListener(FileSetListener listener) {
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.DataModel#getGeneHeaderInfo()
	 */
	@Override
	public HeaderInfo getGeneHeaderInfo() {
		if (GeneHeaderInfo == null)
			return parent.getGeneHeaderInfo();
		else
			return GeneHeaderInfo;
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.DataModel#getArrayHeaderInfo()
	 */
	@Override
	public HeaderInfo getArrayHeaderInfo() {
		if (ArrayHeaderInfo == null)
			return parent.getArrayHeaderInfo();
		else
			return ArrayHeaderInfo;
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.DataModel#getGtrHeaderInfo()
	 */
	@Override
	public HeaderInfo getGtrHeaderInfo() {
		if (GtrHeaderInfo == null)
			return parent.getGtrHeaderInfo();
		else
			return GtrHeaderInfo;
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.DataModel#getAtrHeaderInfo()
	 */
	@Override
	public HeaderInfo getAtrHeaderInfo() {
		if (AtrHeaderInfo == null)
			return parent.getAtrHeaderInfo();
		else
			return AtrHeaderInfo;
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.DataModel#getType()
	 */
	@Override
	public String getType() {
		return "ReorderedDataModel";
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.DataModel#getDataMatrix()
	 */
	@Override
	public DataMatrix getDataMatrix() {
		return subDataMatrix;
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.DataModel#append(edu.stanford.genetics.treeview.DataModel)
	 */
	@Override
	public void append(DataModel m) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.DataModel#removeAppended()
	 */
	@Override
	public void removeAppended() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean aidFound() {
		if (AtrHeaderInfo == null)
			return parent.aidFound();
		else
			return false;
	}

	@Override
	public boolean gidFound() {
		// the following causes a mismatch if not all genes were selected.
		if (GtrHeaderInfo == null)
			return parent.gidFound();
		else
			return false;
	}
	@Override
	public boolean getModified() {
		return false;
	}
	@Override
	public boolean isLoaded() {
		return true;
	}
	@Override
	public ClusterFileSet getClusterFileSet() {
		// TODO Auto-generated method stub
		return null;
	}
}
