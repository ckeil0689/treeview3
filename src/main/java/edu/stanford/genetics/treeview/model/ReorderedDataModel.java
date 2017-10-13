/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.model;

import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.DataMatrix;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.FileSetListener;
import edu.stanford.genetics.treeview.LabelInfo;
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

	private IntLabelInfo gtrLabelInfo;
	private IntLabelInfo rowLabelInfo;
	private IntLabelInfo atrLabelInfo;
	private IntLabelInfo colLabelInfo;
	private final DataMatrix subDataMatrix = new SubDataMatrix();
	private final DataModel parent;
	private final int[] rowIdxArray;
	private final int[] colIdxArray;
	private Preferences documentConfig = Preferences.userRoot().node(
			"SubDataModel");

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
	 * @param rowIdxArray
	 */
	public ReorderedDataModel(final DataModel source, final int[] rowIdxArray,
			final int[] colIdxArray) {

		this.rowIdxArray = rowIdxArray;
		this.colIdxArray = colIdxArray;
		if (rowIdxArray != null) {
			rowLabelInfo = new ReorderedLabelInfo(source.getRowLabelInfo(),
					rowIdxArray);
			gtrLabelInfo = new ReorderedLabelInfo(source.getGtrLabelInfo(),
					rowIdxArray);
		}

		if (colIdxArray != null) {
			colLabelInfo = new ReorderedLabelInfo(
					source.getColLabelInfo(), colIdxArray);
			atrLabelInfo = new ReorderedLabelInfo(source.getAtrLabelInfo(),
					colIdxArray);
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

			if (rowIdxArray != null) {
				row = rowIdxArray[row];
			}

			if (colIdxArray != null) {
				col = colIdxArray[col];
			}

			if ((row == -1) || (col == -1)) {
				return DataModel.EMPTY;
			}
			
			return parent.getDataMatrix().getValue(col, row);
		}

		@Override
		public void setValue(final double value, int col, int row) {

			if (rowIdxArray != null) {
				row = rowIdxArray[row];
			}

			if (colIdxArray != null) {
				col = colIdxArray[col];
			}

			if ((row == -1) || (col == -1)) {
				return;
			}
			
			LogBuffer.getSingleton().log(
					"Error: cannot modifiy " + "reordered data model");
			return;
		}

		@Override
		public int getNumRow() {

			if (rowIdxArray != null) {
				return rowIdxArray.length;
			}
			
			return parent.getDataMatrix().getNumRow();
		}

		@Override
		public int getNumCol() {

			if (colIdxArray != null) {
				return colIdxArray.length;
			}
			
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
		public void setModified(final boolean b) {
		}

		@Override
		public int getNumUnappendedRow() {

			return 0;
		}

		@Override
		public void calculateBaseValues() {

			parent.getDataMatrix().calculateBaseValues();
		}

		@Override
		public double getMinVal() {

			return parent.getDataMatrix().getMinVal();
		}

		@Override
		public double getMaxVal() {

			return parent.getDataMatrix().getMaxVal();
		}

		@Override
		public double getMean() {

			return parent.getDataMatrix().getMean();
		}

		@Override
		public double getZoomedMean(int startingRow, int endingRow,
			int startingCol, int endingCol) {

			return parent.getDataMatrix().getZoomedMean(startingRow, endingRow,
				startingCol, endingCol);
		}

		@Override
		public double getMedian() {
			
			return parent.getDataMatrix().getMedian();
		}

		@Override
		public double getRowAverage(int fromRowId, int toRowId) {
			return parent.getDataMatrix().getRowAverage(fromRowId, toRowId);
		}

		@Override
		public double getColAverage(int fromColId, int toColId) {
			return parent.getDataMatrix().getColAverage(fromColId, toColId);
		}

		@Override
		public void setMinVal(double newMinVal) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setMaxVal(double newMaxVal) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setMean(double newMeanVal) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setMedian(double newMedianVal) {
			// TODO Auto-generated method stub
			
		}
	}

	/**
	 *
	 * Represents reordered LabelInfo of parent.
	 */
	private class ReorderedLabelInfo extends IntLabelInfo {

		private final LabelInfo parentLabelInfo;
		private final int[] reorderedIndex;

		private ReorderedLabelInfo(final LabelInfo labelInfo, final int[] ri) {

			parentLabelInfo = labelInfo;
			reorderedIndex = ri;
		}

		@Override
		public String[] getLabels(final int i) {

			final int index = reorderedIndex[i];
			if (index == -1)
				return null;

			return parentLabelInfo.getLabels(index);
		}

		@Override
		public String getLabel(final int i, final String name) {

			final int index = reorderedIndex[i];
			if (index == -1)
				return null;

			return parentLabelInfo.getLabel(index, name);
		}

		@Override
		public String getLabel(final int rowIndex, final int columnIndex) {

			final String[] labels = getLabels(rowIndex);
			if (labels != null) {
				return labels[columnIndex];
			}
			
			return "";
		}

		@Override
		public String[] getLabelTypes() {

			return parentLabelInfo.getLabelTypes();
		}

		@Override
		public int getNumLabelTypes() {

			return parentLabelInfo.getNumLabelTypes();
		}

		@Override
		public int getNumLabels() {

			return reorderedIndex.length;
		}

		@Override
		public int getIndex(final String name) {

			return parentLabelInfo.getIndex(name);
		}

		@Override
		public int getLabelIndex(final String id) {

			final int parentIndex = parentLabelInfo.getLabelIndex(id);
			if (reorderedIndex[parentIndex] == parentIndex)
				return parentIndex;
			for (int i = 0; i < reorderedIndex.length; i++) {

				if (reorderedIndex[i] == parentIndex) {
					return i;
				}
			}

			return -1;
		}

		@Override
		public void addObserver(final Observer o) {

			parentLabelInfo.addObserver(o);
		}

		@Override
		public void deleteObserver(final Observer o) {

			parentLabelInfo.deleteObserver(o);
		}

		@Override
		public boolean addLabelType(final String name, final int location) {

			return false;
		}

		@Override
		public boolean setLabel(final int i, final String name,
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
		public String[][] getLabelArray() {

			return parentLabelInfo.getLabelArray();// null;
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
		}
		return source;
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
	public IntLabelInfo getRowLabelInfo() {

		if (rowLabelInfo == null) {
			return parent.getRowLabelInfo();
		}
		
		return rowLabelInfo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.DataModel#getArrayHeaderInfo()
	 */
	@Override
	public IntLabelInfo getColLabelInfo() {

		if (colLabelInfo == null) {
			return parent.getColLabelInfo();
		}
		
		return colLabelInfo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.DataModel#getGtrHeaderInfo()
	 */
	@Override
	public LabelInfo getGtrLabelInfo() {

		if (gtrLabelInfo == null) {
			return parent.getGtrLabelInfo();
		}
		
		return gtrLabelInfo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.DataModel#getAtrHeaderInfo()
	 */
	@Override
	public LabelInfo getAtrLabelInfo() {

		if (atrLabelInfo == null) {
			return parent.getAtrLabelInfo();
		}
		
		return atrLabelInfo;
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

		if (atrLabelInfo == null) {
			return parent.aidFound();
		}
		
		return false;
	}

	@Override
	public boolean gidFound() {

		// the following causes a mismatch if not all genes were selected.
		if (gtrLabelInfo == null) {
			return parent.gidFound();
		}
		
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
}
