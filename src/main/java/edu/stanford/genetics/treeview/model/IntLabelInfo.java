package edu.stanford.genetics.treeview.model;

import java.util.Hashtable;
import java.util.Observable;

import edu.stanford.genetics.treeview.LabelInfo;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * A generic LabelInfo, backed by private arrays.
 *
 */
public class IntLabelInfo extends Observable implements LabelInfo {

	private String[] prefixArray = new String[0];
	private String[][] labelArray = new String[0][];
	private Hashtable<String, Integer> id2row = new Hashtable<String, Integer>();

	private boolean modified = false;

	public void hashIDs(final String prefix) {

		final int index = getIndex(prefix);
		id2row = TVModel.populateHash(this, index, id2row);
	}

	public void clear() {

		prefixArray = new String[0];
		setLabelArray(new String[0][]);
		id2row.clear();
	}

	@Override
	public void setPrefixArray(final String[] newVal) {

		prefixArray = newVal;
	}

	public void setLabelArray(final String[][] newVal) {

		labelArray = newVal;
	}

	@Override
	public String[] getPrefixes() {

		return prefixArray;
	}

	@Override
	public int getNumPrefixes() {

		return prefixArray.length;
	}

	@Override
	public int getNumLabels() {

		return getLabelArray().length;
	}

	/**
	 * Returns a label array for a given index.
	 * @param idx - The index of the label array to be returned.
	 * @return The array of labels for the specified row or column index. 
	 */
	@Override
	public String[] getLabels(final int idx) {

		try {
			if (getLabelArray()[idx] == null) {
				return new String[0];
			}
			return getLabelArray()[idx];
			
		} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
			LogBuffer.println("error: tried to retrieve label for index "
					+ idx + " but max is " + getLabelArray().length);
			LogBuffer.logException(e);
			return new String[0];
		}
	}

	/**
	 * Returns the label for a given row index and prefix, or null if not
	 * present.
	 * @param rowIdx - The index of the row for which to retrieve the label.
	 * @param prefix - The name of the prefix column for which to retrieve the label.
	 * @return A label.
	 */
	@Override
	public String getLabel(final int rowIdx, final String prefix) {

		final int index = getIndex(prefix);
		if (index == -1)
			return null;
		return getLabel(rowIdx, index);
	}

	@Override
	public String getLabel(final int rowIdx, final int colIdx) {

		return (getLabels(rowIdx))[colIdx];
	}

	@Override
	public int getIndex(final String prefix) {

		for (int i = 0; i < prefixArray.length; i++) {
			if (prefix.equalsIgnoreCase(prefixArray[i]))
				return i;
		}
		return -1;
	}

	@Override
	public int getLabelIndex(final String id) {

		final Object ind = id2row.get(id);
		if (ind == null)
			return -1;
		else
			return ((Integer) ind).intValue();
	}

	/**
	 * adds new label column of specified name at specified index.
	 *
	 * @param prefix
	 * @param idx
	 * @return
	 */
	@Override
	public boolean addPrefix(final String prefix, final int idx) {

		final int existing = getIndex(prefix);
		// already have this prefix
		if (existing != -1)
			return false;

		final int newNumPrefixes = getNumPrefixes() + 1;
		for (int row = 0; row < getNumLabels(); row++) {

			final String[] from = getLabelArray()[row];
			final String[] to = new String[newNumPrefixes];

			System.arraycopy(from, 0, to, 0, idx);
			System.arraycopy(from, idx, to, idx + 1, newNumPrefixes);

			getLabelArray()[row] = to;
		}

		final String[] newPrefix = new String[newNumPrefixes];
		System.arraycopy(prefixArray, 0, newPrefix, 0, idx);

		newPrefix[idx] = prefix;
		System.arraycopy(prefixArray, idx, newPrefix, idx + 1, newNumPrefixes);

		prefixArray = newPrefix;
		setModified(true);
		
		return true;
	}

	public boolean reorderLabels(final int[] ordering) {

		if (ordering.length == getLabelArray().length) {
			final String[][] temp2 = new String[getLabelArray().length][];

			for (int i = 0; i < getLabelArray().length; i++) {

				if (i < ordering.length) {
					temp2[i] = getLabelArray()[ordering[i]];

				} else {
					temp2[i] = getLabelArray()[i];
				}
			}

			setLabelArray(temp2);
			return true;

		}
		
		return false;
	}

	@Override
	public boolean setLabel(final int i, final String prefix, final String newLabel) {

		if (getLabelArray().length < i)
			return false;

		final int prefixIdx = getIndex(prefix);
		if (prefixIdx == -1)
			return false;

		if (getLabelArray()[i][prefixIdx].equalsIgnoreCase(newLabel))
			return false;

		getLabelArray()[i][prefixIdx] = newLabel;
		setModified(true);
		return true;
	}

	@Override
	public boolean getModified() {

		return modified;
	}

	@Override
	public void setModified(final boolean mod) {

		setChanged();
		notifyObservers();
		modified = mod;
	}

	@Override
	public String[][] getLabelArray() {

		return labelArray;
	}
}