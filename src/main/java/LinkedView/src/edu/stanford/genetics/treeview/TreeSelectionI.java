package edu.stanford.genetics.treeview;

import java.util.Observer;

public interface TreeSelectionI {

	/**
	 * Resizes the size of the TreeSelection to accommodate more elements.
	 *
	 * @param nIndex
	 *            - The new size.
	 */
	public abstract void resize(int nIndex);

	// index methods
	/**
	 * calls deselectall on the <code>IntegerSelection</code> for indexes Much
	 * faster than looping over indexes.
	 */
	public abstract void deselectAllIndexes();

	/**
	 * calls selectall on the <code>IntegerSelection</code> for indexes. Much
	 * faster than looping over genes.
	 */
	public abstract void selectAllIndexes();

	/**
	 * sets the selection status for a particular index.
	 *
	 * @param i
	 *            The gene index
	 * @param b
	 *            The new selection status
	 */
	public abstract void setIndexSelection(int i, boolean b);

	/**
	 * gets the selection status for a particular index.
	 *
	 * @param i
	 *            The gene index
	 * @return The current selection status
	 */
	public abstract boolean isIndexSelected(int i);
	
	/**
	 * Encapsulates a test for the existence of any selection.
	 * @return Whether a selection exists or not.
	 */
	public abstract boolean hasSelection();

	/**
	 *
	 * @return The minimum selected index
	 */
	public abstract int getMinIndex();

	public abstract int[] getSelectedIndexes();

	/**
	 * The maximum selected index contiguous with given index or intervening
	 * @author rleach
	 * @param i
	 * @return
	 */
	public abstract int getMinContiguousIndex(final int i);

	/**
	 *
	 * @return The maximum selected index.
	 */
	public abstract int getMaxIndex();
	
	/**
	 * 
	 * @return The number of indexes between the maximum und minimum selection
	 * index regardless of whether there are multiple selection rectangles 
	 * with interruptions or not.
	 */
	public abstract int getFullSelectionRange();

	/**
	 * The maximum selected index contiguous with given index or intervening
	 * @author rleach
	 * @param i
	 * @return
	 */
	public abstract int getMaxContiguousIndex(final int i);

	/**
	 * Nice for find boxes which are curious.
	 *
	 * @return The number of indexes which could be selected.
	 */
	public abstract int getNumIndexes();

	/**
	 * Deselects existing selection and selects a range of indexes.
	 *
	 * @param min
	 *            the minimum index to select
	 * @param max
	 *            the maximum index to select
	 */
	public abstract void selectNewIndexRange(int min, int max);

	/**
	 * Selects a range of indexes.
	 *
	 * @param min
	 *            the minimum index to select
	 * @param max
	 *            the maximum index to select
	 */
	public abstract void selectIndexRange(int min, int max);

	/**
	 * @return The number of selected indexes.
	 */
	public abstract int getNSelectedIndexes();

	// node methods
	/**
	 * Selects a tree node
	 *
	 * @param n
	 *            Id of node to select
	 */
	public abstract void setSelectedNode(String n);

	/**
	 * Gets the selected tree node
	 *
	 * @return Index of selected Node
	 */
	public abstract String getSelectedNode();

	public abstract void addObserver(Observer view);

	public abstract void notifyObservers();

	public abstract void deleteObserver(Observer view);

}