package edu.stanford.genetics.treeview.model;

import java.util.Hashtable;
import java.util.Observable;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * A generic headerinfo, backed by private arrays.
 * 
 * @author aloksaldanha
 * 
 */
public class IntHeaderInfo extends Observable implements HeaderInfo {

	private String[] prefixArray = new String[0];
	private String[][] labelArray = new String[0][];
	private Hashtable<String, Integer> id2row = new Hashtable<String, 
			Integer>();

	private boolean modified = false;

	public void hashIDs(final String header) {

		final int index = getIndex(header);
		id2row = TVModel.populateHash(this, index, id2row);
	}

	public void clear() {

		prefixArray = new String[0];
		setHeaderArray(new String[0][]);
		id2row.clear();
	}

	@Override
	public void setPrefixArray(final String[] newVal) {

		prefixArray = newVal;
	}

	public void setHeaderArray(final String[][] newVal) {

		labelArray = newVal;
	}

	@Override
	public String[] getNames() {

		return prefixArray;
	}

	@Override
	public int getNumNames() {

		return prefixArray.length;
	}

	@Override
	public int getNumHeaders() {

		return getHeaderArray().length;
	}

	/**
	 * Returns the header for a given gene and column heading.
	 */
	@Override
	public String[] getHeader(final int gene) {
		
		try {
			if (getHeaderArray()[gene] == null) {
				return new String[0];
				
			} else {
				return getHeaderArray()[gene];
			}
		} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
			LogBuffer.println("error: tried to retrieve header for  index "
					+ gene + " but max is " + getHeaderArray().length);
			e.printStackTrace();
			return new String[0];
		}
	}

	/**
	 * Returns the header for a given gene and column heading, or null if not
	 * present.
	 */
	@Override
	public String getHeader(final int gene, final String col) {

		final int index = getIndex(col);
		if (index == -1) {
			return null;
		}
		return getHeader(gene, index);
	}

	@Override
	public String getHeader(final int rowIndex, final int columnIndex) {

		return (getHeader(rowIndex))[columnIndex];
	}

	@Override
	public int getIndex(final String header) {

		for (int i = 0; i < prefixArray.length; i++) {
			if (header.equalsIgnoreCase(prefixArray[i]))
				return i;
		}
		return -1;
	}

	@Override
	public int getHeaderIndex(final String id) {

		final Object ind = id2row.get(id);
		if (ind == null) {
			return -1;

		} else {
			return ((Integer) ind).intValue();
		}
	}

	/**
	 * adds new header column of specified name at specified index.
	 * 
	 * @param name
	 * @param index
	 * @return
	 */
	@Override
	public boolean addName(final String name, final int index) {

		final int existing = getIndex(name);
		// already have this header
		if (existing != -1) {

			return false;
		}

		final int newNumNames = getNumNames() + 1;
		for (int row = 0; row < getNumHeaders(); row++) {

			final String[] from = getHeaderArray()[row];
			final String[] to = new String[newNumNames];

			// for (int col = 0; col < index; col++) {
			//
			// to[col] = from[col];
			// }

			System.arraycopy(from, 0, to, 0, index);

			// for (int col = index + 1; col < newNumNames; col++) {
			//
			// to[col] = from[col - 1];
			// }

			System.arraycopy(from, index, to, index + 1, newNumNames);

			getHeaderArray()[row] = to;
		}

		final String[] newPrefix = new String[newNumNames];
		// for (int col = 0; col < index; col++) {
		//
		// newPrefix[col] = prefixArray[col];
		// }

		System.arraycopy(prefixArray, 0, newPrefix, 0, index);

		newPrefix[index] = name;
		// for (int col = index + 1; col < newNumNames; col++) {
		//
		// newPrefix[col] = prefixArray[col - 1];
		// }
		System.arraycopy(prefixArray, index, newPrefix, index + 1, newNumNames);

		prefixArray = newPrefix;
		setModified(true);
		return true;
	}

	public boolean reorderHeaders(final int[] ordering) {

		if (ordering.length == getHeaderArray().length) {
			final String[][] temp2 = new String[getHeaderArray().length][];

			for (int i = 0; i < getHeaderArray().length; i++) {

				if (i < ordering.length) {
					temp2[i] = getHeaderArray()[ordering[i]];

				} else {
					temp2[i] = getHeaderArray()[i];
				}
			}

			setHeaderArray(temp2);
			return true;

		} else {
			return false;
		}
	}

	@Override
	public boolean setHeader(final int i, final String name, final String value) {

		if (getHeaderArray().length < i) {
			return false;
		}

		final int nameIndex = getIndex(name);
		if (nameIndex == -1) {
			return false;
		}

		if (getHeaderArray()[i][nameIndex].equalsIgnoreCase(value)) {
			return false;
		}

		getHeaderArray()[i][nameIndex] = value;
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
	public String[][] getHeaderArray() {

		return labelArray;
	}

	/*
	 * public void printHashKeys() { Enumeration e = id2row.keys(); while
	 * (e.hasMoreElements()) { System.err.println(e.nextElement()); } }
	 */
}