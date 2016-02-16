/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.util.Observable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * this class generates a single string summary of a HeaderInfo.
 */
public class HeaderSummary extends Observable implements ConfigNodePersistent {

	private Preferences configNode;
	private int[] included = new int[] { 0 };
	private final String type;

	public HeaderSummary(final String type) {

		super();
		this.type = type;
	}
	
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node(type);

		} else {
			LogBuffer.println("Could not find or create " + type
					+ " node because parentNode was null.");
		}
		synchronizeFrom();
	}

	public void setIncluded(final int[] newIncluded) {

		included = newIncluded;
		synchronizeTo();
		setChanged();
		notifyObservers();
	}

	public int[] getIncluded() {

		return included;
	}

	/**
	 * returns the best possible summary for the specified index.
	 *
	 * If no headers are applicable, will return the empty string.
	 */
	public String getSummary(final HeaderInfo headerInfo, final int index) {

		String[] strings = null;
		try {
			strings = headerInfo.getHeader(index);

		} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
			LogBuffer.println("index " + index
					+ " out of bounds on headers, continuing");
			LogBuffer.println("ArrayIndexOutOfBoundsException in "
					+ "getSummary() in HeaderSummary: " + e.getMessage());
			return null;
		}

		if (strings == null) {
			return "";
		}

		final StringBuffer out = new StringBuffer();
		int count = 0;
		if (included.length == 0) {
			return "";
		}

		for (final int element : included) {
			try {
				final String test = strings[element];
				if (test != null) {
					if (count != 0) {
						out.append(", ");
					}
					out.append(test);
					count++;
				}
			} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
				LogBuffer.logException(e);
				break;
			}
		}

		return (count == 0) ? "" : out.toString();
	}

	/**
	 * Retrieves a String array containing the labels for all included 
	 * headers at a certain index.
	 * @param headerInfo The HeaderInfo object from which to take the labels.
	 * @param index The axis index of the label to be returned. 
	 * @return A String array of labels at a defined index.
	 */
	public String[] getSummaryArray(final HeaderInfo headerInfo, 
			final int index) {

		String[] strings = null;
		try {
			strings = headerInfo.getHeader(index);

		} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
			LogBuffer.logException(e);
			return null;
		}

		if (strings == null) {
			return null;
		}

		if (included.length == 0) {
			return null;
		}

		final String[] out = new String[included.length];
		int count = 0;
		for (final int element : included) {
			try {
				final String test = strings[element];
				out[count] = test;
				count++;
			} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
				LogBuffer.logException(e);
				break;
			}
		}
		return out;
	}

	/**
	 * Checks the included-key of the Preferences node and sets the stored
	 * values as the new included index array.
	 */
	private void synchronizeFrom() {

		if (configNode == null) {
			return;
		}

		if (nodeHasAttribute("included")) {
			final String incString = configNode.get("included", "0");
			if (incString.equals("")) {
				setIncluded(new int[0]);

			} else {
				int numComma = 0;
				for (int i = 0; i < incString.length(); i++) {
					if (incString.charAt(i) == ',') {
						numComma++;
					}
				}

				final int[] array = new int[numComma + 1];
				numComma = 0;
				int last = 0;
				for (int i = 0; i < incString.length(); i++) {
					if (incString.charAt(i) == ',') {
						final Integer x = new Integer(incString.substring(last,
								i));
						array[numComma++] = x.intValue();
						last = i + 1;
					}
				}
				try {
					array[numComma] = Integer.parseInt(incString
							.substring(last));

				} catch (final NumberFormatException e) {
					LogBuffer.println("HeaderSummary has trouble "
							+ "restoring included list from " + incString);
					LogBuffer.println("NumberFormatException in "
							+ "synchronizeFrom() in " + "HeaderSummary: "
							+ e.getMessage());
					return;
				}
				setIncluded(array);
			}
		}
	}

	/**
	 * Stores included indices as a String in the Preferences node. 
	 */
	private void synchronizeTo() {

		if (configNode == null) {
			return;
		}

		final int[] vec = getIncluded();
		final StringBuffer temp = new StringBuffer();
		if (vec.length > 0) {
			temp.append(vec[0]);
		}

		for (int i = 1; i < vec.length; i++) {
			temp.append(',');
			temp.append(vec[i]);
		}
		
		configNode.put("included", temp.toString());
	}

	/**
	 * Checks if a Preferences node contains the key name.
	 * @param name The key to check for.
	 * @return boolean Whether key exists or not.
	 */
	public boolean nodeHasAttribute(final String name) {

		try {
			final String[] keys = configNode.keys();

			for (final String key : keys) {
				if (key.equalsIgnoreCase(name)) {
					return true;
				}
			}
			return false;

		} catch (final BackingStoreException e) {
			LogBuffer.logException(e);
			return false;
		}
	}
}
