/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.util.Observer;

/**
 * Interface to access label info about genes or arrays or treenodes This
 * interface is used many ways. The basic idea is that the "label" refers to
 * which row, column, or node you want information about, whereas the "prefix" is
 * which header you want. Thus, getNumLabels() is the number of rows, whereas
 * getNumPrefixes() is the number of headers for each row.
 *
 * Conceptually, the objects that are annotated (rows, columns, nodes) can be
 * thought of as rows, and the various names as the headers of columns of
 * information about them.
 */
public interface LabelInfo {
	/**
	 * Gets the label info for row/column/node i
	 *
	 * @param i
	 *            index of the row/column/node for which to get labels
	 * @return The array of label values
	 */
	public String[] getLabels(int i);

	/**
	 * Gets the label info for row/column/node i, col name
	 *
	 * @param i
	 *            index of the row/column/node for which to get labels
	 * @param name
	 *            name of the label to get
	 * @return label value
	 */
	public String getLabel(int i, String name);

	/**
	 * Gets the names of the labels
	 *
	 * @return The array of label prefixes
	 */
	public String[] getLabelTypes();

	/**
	 * The number of prefixes.
	 */
	public int getNumLabelTypes();

	/**
	 * Gets the number of sets of labels. This will generally be the number
	 * things which have labels, i.e. number of row/column/node.
	 */
	public int getNumLabels();

	/**
	 * Gets the index associated with a particular label name.
	 *
	 * usually, getIndex(getNames() [i]) == i.
	 *
	 * Note that some label info classes may have special ways of mapping names
	 * to indexes, so that the getNames() array at the returned index may not
	 * actually match the name argument. This is particularly true for fields
	 * like YORF, which may also be UID, etc...
	 *
	 * Should have been called "getNameIndex".
	 *
	 * @param prefix
	 *            A name to find the index of
	 * @return The index value
	 */
	public int getIndex(String prefix);

	/**
	 * gets the index of a row/column/node given a value from the first column
	 * (the id column). Should have been called "getIndexById" or something.
	 *
	 * @param label
	 *            a particular id for a row or column or node
	 * @return The index value, for use with getLabel() or similar thing.
	 *         Returns -1 if no label matching "id" can be found.
	 */
	public int getLabelIndex(String label);

	/**
	 * This is used by LabelInfo objects that may change over time. If your
	 * LabelInfo is static, you can just make this a noop.
	 *
	 * @param o
	 */
	public void addObserver(Observer o);

	/**
	 * This is used by LabelInfo objects that may change over time. If your
	 * LabelInfo is static, you can just make this a noop.
	 *
	 * @param o
	 */
	public void deleteObserver(Observer o);

	/**
	 * Adds a new named "column" of labels to this object Just return false if
	 * your label info is read only.
	 *
	 * @param prefix
	 *            name of column to add
	 * @param location
	 *            0 means make it first, getNumPrefixes() means make it last
	 * @return true if successfully added, false if not.
	 */
	public boolean addLabelType(String prefix, int location);

	/**
	 * Sets indicated label to specified value Just return false if your label
	 * info is read only.
	 *
	 * @param prefix
	 *            name of column to change
	 * @param newLabel
	 *            new value for label.
	 *
	 * @return true if successfully modified, false if not.
	 */
	public boolean setLabel(int i, String prefix, String newLabel);

	public void setLabelTypeArray(String[] newPrefixArray);

	/**
	 * @return true if the LabelInfo has been modified since last save
	 */
	public boolean getModified();

	/**
	 * should only be called externally after LabelInfo has been saved to disk.
	 *
	 * @param mod
	 *            false if no longer out of synch with disk.
	 */
	public void setModified(boolean mod);

	/**
	 * lookup by row and column, which should correspond to position in the
	 * prefixes array.
	 *
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public String getLabel(int rowIndex, int columnIndex);

	public String[][] getLabelArray();
}
