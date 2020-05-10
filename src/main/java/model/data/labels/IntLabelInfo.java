package model.data.labels;

import model.data.matrix.TVModel;
import util.LogBuffer;

import java.util.Hashtable;
import java.util.Observable;

/**
 * A generic LabelInfo, backed by private arrays.
 *
 */
public class IntLabelInfo extends Observable implements LabelInfo {

	private String[] labelTypeArray = new String[0];
	private String[][] labelArray = new String[0][];
	private Hashtable<String, Integer> id2row = new Hashtable<>();

	private boolean modified = false;

	public void hashIDs(final String labelType) {

		final int index = getIndex(labelType);
		id2row = TVModel.populateHash(this, index, id2row);
	}

	public void clear() {

		setLabelTypeArray(new String[0]);
		setLabelArray(new String[0][]);
		id2row.clear();
	}

	@Override
	public void setLabelTypeArray(final String[] newVal) {

		this.labelTypeArray = newVal;
	}

	public void setLabelArray(final String[][] newVal) {

		this.labelArray = newVal;
	}

	@Override
	public String[] getLabelTypes() {

		return labelTypeArray;
	}

	@Override
	public int getNumLabelTypes() {

		return labelTypeArray.length;
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
	 * Returns the label for a given row index and label type, or null if not
	 * present.
	 * @param rowIdx - The index of the row for which to retrieve the label.
	 * @param labelType - The name of the label type column for which to retrieve the label.
	 * @return A label.
	 */
	@Override
	public String getLabel(final int rowIdx, final String labelType) {

		final int index = getIndex(labelType);
		if (index == -1)
			return null;
		return getLabel(rowIdx, index);
	}

	@Override
	public String getLabel(final int rowIdx, final int colIdx) {

		return (getLabels(rowIdx))[colIdx];
	}

	@Override
	public int getIndex(final String labelType) {

		if(labelType == null) {
			return -1;
		}

		for (int i = 0; i < labelTypeArray.length; i++) {
			if (labelType.equalsIgnoreCase(labelTypeArray[i]))
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
	 * @param labelType
	 * @param idx
	 * @return
	 */
	@Override
	public boolean addLabelType(final String labelType, final int idx) {

		final int existing = getIndex(labelType);
		// already have this labelType
		if (existing != -1)
			return false;

		final int newNumLabelTypes = getNumLabelTypes() + 1;
		final String[] newLabelType = new String[newNumLabelTypes];
		System.arraycopy(labelTypeArray, 0, newLabelType, 0, idx);
		newLabelType[idx] = labelType;

		labelTypeArray = newLabelType;
		setModified(true);
		
		return true;
	}
	
	@Override
	public boolean addLabels(final String[] newLabels) {
		
		if(newLabels.length != labelArray.length) {
			LogBuffer.println("Could not extend labels because the array of labels" +
				" to be added does not match the size of the existing label arrays.");
			return false;
		}
		
		String[][] oldLabelArray = labelArray;
		String[][] newLabelArray = new String[labelArray.length][];
		
		for(int i = 0; i < oldLabelArray.length; i++) {
			String[] oldEntry = oldLabelArray[i];
			String[] newEntry = new String[oldEntry.length + 1];
			System.arraycopy(oldEntry, 0, newEntry, 0, oldEntry.length);
			newEntry[newEntry.length - 1] = newLabels[i];
			newLabelArray[i] = newEntry;
		}
		
		labelArray = newLabelArray;
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
	public boolean setLabel(final int i, final String labelType, final String newLabel) {

		if (getLabelArray().length < i)
			return false;

		final int labelTypeIdx = getIndex(labelType);
		if (labelTypeIdx == -1)
			return false;

		if (getLabelArray()[i][labelTypeIdx].equalsIgnoreCase(newLabel))
			return false;

		getLabelArray()[i][labelTypeIdx] = newLabel;
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