/*
 * Created on Mar 5, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.model;

import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.DataMatrix;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.FileSetListener;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * 
 * This class produces a reordered version of the parent DataModel It can a
 * subset, if the integer arrays passed in have fewer members, or a superset, if
 * the arrays passed in have more members. Gaps can be introduced between genes
 * or arrays by inserting "-1" at an index.
 * 
 * @author aloksaldanha
 * 
 */
public class ReorderedDataModel extends Observable implements DataModel {

	private IntHeaderInfo gtrHeaderInfo;
	private IntHeaderInfo geneHeaderInfo;
	private IntHeaderInfo atrHeaderInfo;
	private IntHeaderInfo arrayHeaderInfo;
	private final DataMatrix subDataMatrix = new SubDataMatrix();
	private final DataModel parent;
	private final int[] geneIndex;
	private final int[] arrayIndex;
	private Preferences documentConfig = Preferences.userRoot()
			.node("SubDataModel");

	/**
	 * Builds data model which corresponds to a reordered version of the source
	 * datamodel, as specified by geneIndex
	 * 
	 * @param source
	 * @param geneIndex
	 */
	public ReorderedDataModel(final DataModel source, final int[] geneIndex) {

		this(source, geneIndex, null);
	}

	/**
	 * Builds data model which corresponds to a reordered version of the source
	 * datamodel, as specified by geneIndex and arrayIndex.
	 * 
	 * @param source
	 * @param geneIndex
	 */
	public ReorderedDataModel(final DataModel source, final int[] geneIndex,
			final int[] arrayIndex) {

		this.geneIndex = geneIndex;
		this.arrayIndex = arrayIndex;
		if (geneIndex != null) {
			geneHeaderInfo = new ReorderedHeaderInfo(
					source.getGeneHeaderInfo(), geneIndex);
			gtrHeaderInfo = new ReorderedHeaderInfo(source.getGtrHeaderInfo(),
					geneIndex);
		}

		if (arrayIndex != null) {
			arrayHeaderInfo = new ReorderedHeaderInfo(
					source.getArrayHeaderInfo(), arrayIndex);
			atrHeaderInfo = new ReorderedHeaderInfo(source.getAtrHeaderInfo(),
					arrayIndex);
		}

		this.parent = source;
		this.source = "Subset " + parent.getSource();
		this.name = "Subset of " + parent.getName();
		// this should really be set to a clone of the parent's document config.
		documentConfig = source.getDocumentConfigRoot();
	}

	/**
	 * @author aloksaldanha
	 * 
	 */
	private class SubDataMatrix implements DataMatrix {

		@Override
		public double getValue(int col, int row) {

			if (geneIndex != null) {
				row = geneIndex[row];
			}

			if (arrayIndex != null) {
				col = arrayIndex[col];
			}

			if ((row == -1) || (col == -1)) {
				return DataModel.EMPTY;

			} else {
				return parent.getDataMatrix().getValue(col, row);
			}
		}

		@Override
		public void setValue(final double value, int col, int row) {

			if (geneIndex != null) {
				row = geneIndex[row];
			}

			if (arrayIndex != null) {
				col = arrayIndex[col];
			}

			if ((row == -1) || (col == -1)) {
				return;
			} else {
				LogBuffer.getSingleton().log(
						"Error: cannot modifiy " + "reordered data model");
				return;
			}
		}

		@Override
		public int getNumRow() {

			if (geneIndex != null) {
				return geneIndex.length;

			} else {
				return parent.getDataMatrix().getNumRow();
			}
		}

		@Override
		public int getNumCol() {

			if (arrayIndex != null) {
				return arrayIndex.length;

			} else {
				return parent.getDataMatrix().getNumCol();
			}
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
		public void setModified(final boolean b) {
		}

		@Override
		public int getNumUnappendedRow() {

			return 0;
		}

		@Override
		public void calculateMinMax() {
			
			parent.getDataMatrix().calculateMinMax();
		}

		@Override
		public double getMinVal() {
			
			return parent.getDataMatrix().getMinVal();
		}

		@Override
		public double getMaxVal() {
			
			return parent.getDataMatrix().getMaxVal();
		}
	}

	/**
	 * 
	 * Represents reordered HeaderInfo of parent.
	 */
	private class ReorderedHeaderInfo extends IntHeaderInfo {// implements
																// HeaderInfo {

		private final HeaderInfo parentHeaderInfo;
		private final int[] reorderedIndex;

		private ReorderedHeaderInfo(final HeaderInfo hi, final int[] ri) {

			parentHeaderInfo = hi;
			reorderedIndex = ri;
		}

		@Override
		public String[] getHeader(final int i) {

			final int index = reorderedIndex[i];
			if (index == -1) {
				return null;
			}

			return parentHeaderInfo.getHeader(index);
		}

		@Override
		public String getHeader(final int i, final String name) {

			final int index = reorderedIndex[i];
			if (index == -1) {
				return null;
			}

			return parentHeaderInfo.getHeader(index, name);
		}

		@Override
		public String getHeader(final int rowIndex, final int columnIndex) {

			final String[] header = getHeader(rowIndex);
			if (header != null) {
				return header[columnIndex];

			} else {
				return "";
			}
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
		public int getIndex(final String name) {

			return parentHeaderInfo.getIndex(name);
		}

		@Override
		public int getHeaderIndex(final String id) {

			final int parentIndex = parentHeaderInfo.getHeaderIndex(id);
			if (reorderedIndex[parentIndex] == parentIndex) {
				return parentIndex;

			} else {
				for (int i = 0; i < reorderedIndex.length; i++)

					if (reorderedIndex[i] == parentIndex) {
						return i;
					}
			}

			return -1;
		}

		@Override
		public void addObserver(final Observer o) {

			parentHeaderInfo.addObserver(o);
		}

		@Override
		public void deleteObserver(final Observer o) {

			parentHeaderInfo.deleteObserver(o);
		}

		@Override
		public boolean addName(final String name, final int location) {

			return false;
		}

		@Override
		public boolean setHeader(final int i, final String name,
				final String value) {

			return false;
		}

		@Override
		public boolean getModified() {

			return false;
		}

		@Override
		public void setModified(final boolean mod) {
		}

		@Override
		public String[][] getHeaderArray() {

			return parentHeaderInfo.getHeaderArray();// null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.DataModel#getDocumentConfig()
	 */
	@Override
	public Preferences getDocumentConfigRoot() {

		return documentConfig;
	}

	String source;

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.DataModel#getSource()
	 */
	@Override
	public String getSource() {

		return source;
	}

	@Override
	public String getFileName() {
		if (source == null) {
			return "No Data Loaded";

		} else {
			return source;
		}
	}

	public void setSource(final String string) {

		source = string;
	}

	String name;

	@Override
	public String getName() {

		return name;
	}

	public void setName(final String string) {

		name = string;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.DataModel
	 * #setModelForCompare(edu.stanford.genetics.treeview.DataModel)
	 */
	@Override
	public void setModelForCompare(final DataModel dm) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.DataModel#getFileSet()
	 */
	@Override
	public FileSet getFileSet() {

		return null;
	}

	@Override
	public void clearFileSetListeners() {
	}

	@Override
	public void addFileSetListener(final FileSetListener listener) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.DataModel#getGeneHeaderInfo()
	 */
	@Override
	public IntHeaderInfo getGeneHeaderInfo() { // IntHeaderInfo

		if (geneHeaderInfo == null) {
			return parent.getGeneHeaderInfo();

		} else {
			return geneHeaderInfo; // (IntHeaderInfo)
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.DataModel#getArrayHeaderInfo()
	 */
	@Override
	public IntHeaderInfo getArrayHeaderInfo() {

		if (arrayHeaderInfo == null) {
			return parent.getArrayHeaderInfo();

		} else {
			return arrayHeaderInfo;// (IntHeaderInfo)
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.DataModel#getGtrHeaderInfo()
	 */
	@Override
	public HeaderInfo getGtrHeaderInfo() {

		if (gtrHeaderInfo == null) {
			return parent.getGtrHeaderInfo();

		} else {
			return gtrHeaderInfo;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.DataModel#getAtrHeaderInfo()
	 */
	@Override
	public HeaderInfo getAtrHeaderInfo() {

		if (atrHeaderInfo == null) {
			return parent.getAtrHeaderInfo();

		} else {
			return atrHeaderInfo;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.DataModel#getType()
	 */
	@Override
	public String getType() {

		return "ReorderedDataModel";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.DataModel#getDataMatrix()
	 */
	@Override
	public DataMatrix getDataMatrix() {

		return subDataMatrix;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.DataModel
	 * #append(edu.stanford.genetics.treeview.DataModel)
	 */
	@Override
	public void append(final DataModel m) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.DataModel#removeAppended()
	 */
	@Override
	public void removeAppended() {
	}

	@Override
	public boolean aidFound() {

		if (atrHeaderInfo == null) {
			return parent.aidFound();

		} else {
			return false;
		}
	}

	@Override
	public boolean gidFound() {

		// the following causes a mismatch if not all genes were selected.
		if (gtrHeaderInfo == null) {
			return parent.gidFound();

		} else {
			return false;
		}
	}

	@Override
	public boolean getModified() {

		return false;
	}

	@Override
	public boolean isLoaded() {

		return true;
	}
}
