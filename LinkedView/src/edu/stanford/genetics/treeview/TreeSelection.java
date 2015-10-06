/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: TreeSelection.java,v $
 * $Revision: 1.5 $
 * $Date: 2006-03-20 06:18:43 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular,
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER
 */
package edu.stanford.genetics.treeview;

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

		integerSelection = new IntegerSelection(nIndex);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.TreeSelectionI#resize(int)
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

		integerSelection = temp;
		setChanged();
	}

	// index methods
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.TreeSelectionI#deselectAllIndexes()
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
	 * @see edu.stanford.genetics.treeview.TreeSelectionI#selectAllIndexes()
	 */
	@Override
	public void selectAllIndexes() {

		integerSelection.selectAll();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.TreeSelectionI#setIndex(int, boolean)
	 */
	@Override
	public void setIndexSelection(final int i, final boolean b) {

		integerSelection.set(i, b);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.TreeSelectionI#isIndexSelected(int)
	 */
	@Override
	public boolean isIndexSelected(final int i) {

		return integerSelection.isSelected(i);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.TreeSelectionI#getMinIndex()
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
	public int getMinContiguousIndex(final int i) {

		//Error-check the input
		if(!isIndexSelected(i) || i == 0) {
			return(i);
		}

		int j = i;
		for(j = i;j > 0 && isIndexSelected(j - 1);j--){}
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
	public int getMaxContiguousIndex(final int i) {

		//Error-check the input
		if(!isIndexSelected(i) || i == getMaxIndex()) {
			return(i);
		}

		int j = i;
		for(j = i;j < getMaxIndex() && isIndexSelected(j + 1);j++){}
		return(j);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.TreeSelectionI#getSelectedIndexes()
	 */
	@Override
	public int[] getSelectedIndexes() {

		return integerSelection.getSelectedIndexes();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.TreeSelectionI#getMaxIndex()
	 */
	@Override
	public int getMaxIndex() {

		return integerSelection.getMax();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.TreeSelectionI#getNumIndexes()
	 */
	@Override
	public int getNumIndexes() {

		return integerSelection.getNSelectable();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.TreeSelectionI#selectIndexRange(int,
	 * int)
	 */
	@Override
	public void selectIndexRange(int min, int max) {

		if (min > max) {
			final int swap = min;
			min = max;
			max = swap;
		}

		for (int i = min; i <= max; i++) {

			integerSelection.set(i, true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.TreeSelectionI#getNSelectedIndexes()
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
	 * edu.stanford.genetics.treeview.TreeSelectionI#setSelectedNode(java.lang
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
	 * @see edu.stanford.genetics.treeview.TreeSelectionI#getSelectedNode()
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

			return isSelected[i];
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
}
