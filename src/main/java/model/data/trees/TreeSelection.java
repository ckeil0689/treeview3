/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package model.data.trees;

import util.LogBuffer;

import java.util.Observable;

/**
 * A quasi-model independant selection model for leaf indexes as well as
 * internal nodes of trees.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.5 $ $Date: 2006-03-20 06:18:43 $
 */
public class TreeSelection extends Observable implements TreeSelectionI {

	private IntegerSelection integerSelection;
	private String selectedNode;

	/**
	 * Constructor for the TreeSelection object
	 *
	 * @param nIndex
	 *            number of indexes which can be selected
	 */
	public TreeSelection(final int nIndex) {

		this.integerSelection = new IntegerSelection(nIndex);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeSelectionI#resize(int)
	 */
	@Override
	public void resize(final int nIndex) {

		final IntegerSelection temp = new IntegerSelection(nIndex);

		for (int i = 0; i < nIndex; i++) {

			if (i < integerSelection.getNSelectable()) {
				temp.set(i, integerSelection.isSelected(i));

			} else {
				temp.set(i, false);
			}
		}

		this.integerSelection = temp;
		setChanged();
	}

	// index methods
	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeSelectionI#deselectAllIndexes()
	 */
	@Override
	public void deselectAllIndexes() {

		integerSelection.deselectAll();

		/* This should probably be here.... */
		setSelectedNode(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeSelectionI#selectAllIndexes()
	 */
	@Override
	public void selectAllIndexes() {

		integerSelection.selectAll();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeSelectionI#setIndex(int, boolean)
	 */
	@Override
	public void setIndexSelection(final int i, final boolean b) {

		integerSelection.set(i, b);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeSelectionI#isIndexSelected(int)
	 */
	@Override
	public boolean isIndexSelected(final int i) {

		return integerSelection.isSelected(i);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeSelectionI#getMinIndex()
	 */
	@Override
	public int getMinIndex() {

		return integerSelection.getMin();
	}

	/**
	 * Given a selected index, return the minimum selected index that is
	 * separated from the initial index be a contiguous series of 0 or more
	 * selected indexes
	 * @author rleach
	 * @param i
	 * @return int
	 */
	@Override
	public int getMinContiguousIndex(final int i) {

		//Error-check the input
		if(!isIndexSelected(i) || i == 0) {
			return(i);
		}

		int j = i;
		for(j = i;j > 0;j--){
			if(!isIndexSelected(j - 1)) {
				break;
			}
		}
		return(j);
	}

	/**
	 * Given a selected index, return the minimum selected index that is
	 * separated from the initial index be a contiguous series of 0 or more
	 * selected indexes
	 * @author rleach
	 * @param i
	 * @return int
	 */
	@Override
	public int getMaxContiguousIndex(final int i) {

		//Error-check the input
		if(!isIndexSelected(i) || i == getMaxIndex()) {
			if(!isIndexSelected(i)) {
				LogBuffer.println("ERROR: Invalid index [" + i + "].  " +
					"Not selected.");
			}
			return(i);
		}

		int j = i;
		for(j = i; j < getMaxIndex(); j++){
			if(!isIndexSelected(j + 1)) {
				break;
			}
		}
		return(j);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeSelectionI#getSelectedIndexes()
	 */
	@Override
	public int[] getSelectedIndexes() {

		return integerSelection.getSelectedIndexes();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeSelectionI#getMaxIndex()
	 */
	@Override
	public int getMaxIndex() {

		return integerSelection.getMax();
	}
	
	@Override
	public int getFullSelectionRange() {
		
		return getMaxIndex() - getMinIndex() + 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeSelectionI#getNumIndexes()
	 */
	@Override
	public int getNumIndexes() {

		return integerSelection.getNSelectable();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeSelectionI#selectNewIndexRange(
	 * int,int)
	 */
	@Override
	public void selectNewIndexRange(int min, int max) {

//		LogBuffer.println("Selection Min: " + min + " Max: " + max);

		deselectAllIndexes();

		selectIndexRange(min,max);
	}

	/**
	 * Selects a range of indexes. Indexes do not need to be sorted min/max.
	 * @param min
	 * @param max
	 */
	@Override
	public void selectIndexRange(int min, int max) {

		if(min > max) {
			final int swap = min;
			min = max;
			max = swap;
		}

		for(int i = min;i <= max;i++) {
			setIndexSelection(i,true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeSelectionI#getNSelectedIndexes()
	 */
	@Override
	public int getNSelectedIndexes() {

		return integerSelection.getNSelected();
	}

	// node methods
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * treeview.TreeSelectionI#setSelectedNode(java.lang
	 * .String)
	 */
	@Override
	public void setSelectedNode(final String n) {

		if (selectedNode == null || !selectedNode.equalsIgnoreCase(n)) {
			selectedNode = n;
			setChanged();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeSelectionI#getSelectedNode()
	 */
	@Override
	public String getSelectedNode() {

		return selectedNode;
	}

	/**
	 * a class to efficiently model a range of integers which can be selected.
	 */
	class IntegerSelection {

		boolean[] isSelected;

		IntegerSelection(final int size) {

			isSelected = new boolean[size];
			deselectAll();
		}

		public int getNSelectable() {

			return isSelected.length;
		}

		public int getNSelected() {
			int n = 0;

			for (final boolean element : isSelected) {

				if (element) {
					n++;
				}
			}

			return n;
		}

		public int[] getSelectedIndexes() {

			final int nSelected = getNSelected();
			final int[] indexes = new int[nSelected];
			int curr = 0;

			// LogBuffer.println("Num Selected: [" + nSelected + "].");

			for (int i = 0; i < isSelected.length; i++) {

				if (isSelected[i]) {
					// LogBuffer.println("Index: [" + i + "] is selected.");
					indexes[curr++] = i;
				}
			}

			return indexes;
		}

		public void deselectAll() {

			TreeSelection.this.setChanged();
			for (int i = 0; i < isSelected.length; i++) {

				isSelected[i] = false;
			}
		}

		public void selectAll() {

			TreeSelection.this.setChanged();
			for (int i = 0; i < isSelected.length; i++) {

				isSelected[i] = true;
			}
		}

		public void set(final int i, final boolean b) {

			if ((i >= 0) && (i < isSelected.length)) {
				TreeSelection.this.setChanged();
				// LogBuffer.println("Setting isSelected to [" + (b ? "true" :
				// "false") + "] for index: [" + i + "].");
				isSelected[i] = b;
			}
		}

		public boolean isSelected(final int i) {
			
			if ((i >= 0) && (i < isSelected.length)) {
				return isSelected[i];
			}
			
			return false;
		}

		public int getMin() {

			final int min = -1;
			for (int i = 0; i < isSelected.length; i++) {

				if (isSelected[i])
					return i;
			}

			return min;
		}

		public int getMax() {

			int max = -1;
			for (int i = 0; i < isSelected.length; i++) {

				if (isSelected[i]) {
					max = i;
				}
			}

			return max;
		}
	}

	@Override
	public boolean hasSelection() {
		
		return getNSelectedIndexes() > 0;
	}
}
