/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.util.Observer;

/**
 * This class is a dummy which can be used for testing in place of LabelInfo.
 * @deprecated - unused remnant of Java TreeView 
 * @TODO - check if removal is beneficial
 *
 */
public class DummyLabelInfo implements LabelInfo {
	String[] header1 = new String[] { "Bob1", "Alice1" };
	String[] header2 = new String[] { "Bob2", "Alice2" };
	String[] header3 = new String[] { "Bob3", "Alice3" };

	@Override
	public String[] getLabels(final int i) {
		if (i == 1)
			return header1;
		if (i == 2)
			return header2;
		return header3;
	}

	/**
	 * Gets the label info for gene/array i, col name
	 *
	 * @param i
	 *            index of the label to get
	 * @return The array of label values
	 */
	@Override
	public String getLabel(final int i, final String name) {
		return (getLabels(i))[getIndex(name)];
	}

	String[] names = new String[] { "Bob", "Alice" };

	/**
	 * Gets the names of the headers
	 *
	 * @return The list of names
	 */
	@Override
	public String[] getLabelTypes() {
		return names;
	}

	@Override
	public int getNumLabelTypes() {
		return names.length;
	}

	@Override
	public int getNumLabels() {
		return 3;
	}

	@Override
	public int getIndex(final String name) {
		if (name.equals("Bob"))
			return 0;
		return 1;
	}

	@Override
	public int getLabelIndex(final String id) {
		for (int i = 0; i < getNumLabels(); i++) {
			if ((getLabels(i)[0]).equals(id))
				return i;
		}
		return -1;
	}

	/**
	 * noop, since this object is static.
	 */
	@Override
	public void addObserver(final Observer o) {
	}

	@Override
	public void deleteObserver(final Observer o) {
	}

	@Override
	public boolean addLabelType(final String name, final int location) {
		return false;
	}

	@Override
	public boolean setLabel(final int i, final String name, final String value) {
		return false;
	}

	@Override
	public void setLabelTypeArray(final String[] newPrefixArray) {

	}

	@Override
	public boolean getModified() {
		return false;
	}

	@Override
	public void setModified(final boolean mod) {
	}

	@Override
	public String getLabel(final int rowIndex, final int columnIndex) {
		return (getLabels(rowIndex))[columnIndex];
	}

	@Override
	public String[][] getLabelArray() {
		// TODO Auto-generated method stub
		return null;
	}

}
