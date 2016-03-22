/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.util.Arrays;
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
	private String[] headers;

	public HeaderSummary(final String type) {

		super();
		this.type = type;
	}
	
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode == null) {
			LogBuffer.println("Could not find or create " + type
					+ " node because parentNode was null.");
			return;
		}

		this.configNode = parentNode.node(type);
	}
	
	@Override
	public Preferences getConfigNode() {

		return configNode;
	}

	@Override
	public void requestStoredState() {
		
		importStateFrom(configNode);
	}

	@Override
	public void storeState() {
		
		if (configNode == null) {
			LogBuffer.println("Could not store state for " + type 
					+ " because configNode was null.");
			return;
		}

		final int[] vec = getIncluded();
		configNode.put("included", Arrays.toString(vec));
		
		if(headers == null) {
			LogBuffer.println("Could not store headers. "
					+ "No headers defined yet.");
			return;
		}
		
		final String[] names = new String[vec.length];
		for(int i = 0; i < names.length; i++) {
			int idx = vec[i];
			if(idx < headers.length) {
				names[i] = headers[idx];
			}
		}
		configNode.put("includedNames", Arrays.toString(names));
	}
	
	@Override
	public void importStateFrom(Preferences oldNode) {
		
		if (oldNode == null) {
			LogBuffer.println("Node to be imported was null.");
			return;
		}
		
		if(nodeHasAttribute("includedNames", oldNode)) {
			String importNames = oldNode.get("includedNames", "[]");
			configNode.put("includedNames", importNames);
		}

		if (nodeHasAttribute("included", oldNode)) {
			final String incString = oldNode.get("included", "[]");
			if (incString.equals("[]")) {
				LogBuffer.println("The included key has no values stored. "
						+ "Including nothing. (" + type + ")");
				setIncluded(new int[0]);

			} else {
				String[] inclArray = getValuesFromStoredString(incString);
				int[] array = new int[inclArray.length];
				
				try {
					for(int i = 0; i < inclArray.length; i++) {
						String elem = inclArray[i];
						array[i] = Integer.parseInt(elem);
					}

				} catch (final NumberFormatException e) {
					LogBuffer.logException(e);
					LogBuffer.println("HeaderSummary has trouble "
							+ "restoring included list from " 
							+ Arrays.toString(inclArray));
					setIncluded(new int[0]);
					return;
				}
			
				array = adjustIncl(array);
				setIncluded(array);
			}
		} else if(headers != null && headers.length > 0) {
			LogBuffer.println("There are headers defined but no key stored "
					+ "for included indices. Setting a default. "
					+ "(" + type + ")");
			setIncluded(new int[]{0});
			
		} else {
			LogBuffer.println("There are no headers defined. "
					+ "Including nothing. (" + type + ")");
			setIncluded(new int[0]);
		}
	}

	/**
	 * Setter for the list of indices describing which labels should be 
	 * included when returning a "summary" string. For LabelView this would
	 * mean that a summary of labels at a certain index will be returned based
	 * on which label headers the user selected.
	 * @param newIncluded - An array of indices describing the selected label 
	 * headers.
	 */
	public void setIncluded(final int[] newIncluded) {

		this.included = newIncluded;
		storeState();
		setChanged();
		notifyObservers();
	}
	
	/**
	 * Setter for the current available headers in a loaded file. Setting the
	 * headers is vital for ensuring label header selection consistency because
	 * a purely index-based approach does not account for headers that moved
	 * their position in the header list, for example after clustering.
	 * @param headers - The current headers of a loaded file.
	 */
	public void setHeaders(final String[] headers) {
		
		this.headers = headers;
		int[] adjustedIncluded = adjustIncl(getIncluded());
		setIncluded(adjustedIncluded);
		storeState();
		setChanged();
		notifyObservers();
	}

	/**
	 * @return included - A list of included label header indices.
	 */
	public int[] getIncluded() {

		return included;
	}

	/**
	 * @return the best possible summary for the specified index.
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
	 * @param hI The HeaderInfo object from which to take the labels.
	 * @param idx The axis index of the label to be returned. 
	 * @return A String array of labels at a defined index.
	 */
	public String[] getSummaryArray(final HeaderInfo hI, final int idx) {

		String[] strings = null;
		try {
			strings = hI.getHeader(idx);

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
	 * Preferences nodes store can only store String arrays as a single string.
	 * This method is a helper used to convert such strings back to an array
	 * by removing brackets, spaces, and splitting the String up at commas.
	 * @param storedVal - A String object, should be a single String 
	 * representing comma-separated String array.
	 * @return An array of Strings
	 */
	private String[] getValuesFromStoredString(final String storedVal) {
		
		String[] storedVals = storedVal
				.replaceAll("\\[", "")
				.replaceAll("\\]", "")
				.split(",");
		
		/* Strings may have spaces within them, so extra trimming here. */
		for(int i = 0; i < storedVals.length; i++) {
			String s = storedVals[i];
			storedVals[i] = s.trim();
		}
		
		return storedVals;
	}
	
	/**
	 * Checks if the included indices match with the stored header Strings.
	 * During clustering or reworking a file, actual header names may shift in
	 * their position and the stored index alone may not be representative
	 * for the last selected header.
	 * @param included The included indices.
	 */
	private int[] adjustIncl(int[] included) {
		
		if(headers == null) {
			LogBuffer.println("headers are null in " + type);
			return new int[0];
		}
		
		int[] newIncluded = included;
		
		if (nodeHasAttribute("includedNames", configNode)) {
			String names = configNode.get("includedNames", "");
			LogBuffer.println("Included header names: " + names);
			String[] nameArray = getValuesFromStoredString(names);
			
			newIncluded = new int[nameArray.length];
			
			for(int i = 0; i < nameArray.length; i++) {
				String name = nameArray[i];
				for(int j = 0; j < headers.length; j++) {
					if(name.equals(headers[j])) {
						newIncluded[i] = j;
						break;
					}
				}
			}
		/* At least ensure that included[] is not out of bounds */
		} else {
	        int maxSize = 0;
	        /* Shrink if necessary */
			for(int i = 0; i < included.length; i++) {
				if(included[i] >= headers.length) {
					maxSize = i + 1;
					break;
				}
			}
			
			/* Create adjusted array */
			newIncluded = new int[maxSize];
			
			for(int i = 0; i < newIncluded.length; i++) {
				newIncluded[i] = included[i];
			}
		}
		
		/* Ensure ordering */
		Arrays.sort(newIncluded);
		
		/* Make sure to display first header if nothing was restored. */
		if(headers.length > 0 && newIncluded.length == 0) {
			newIncluded = new int[]{0};
		}
		
		return newIncluded;
	}

	/**
	 * Checks if a Preferences node contains the key name.
	 * @param name The key to check for.
	 * @return boolean Whether key exists or not.
	 */
	public boolean nodeHasAttribute(final String name, final Preferences node) {

		if (node == null) {
			return false;
		}
		
		try {
			final String[] keys = node.keys();

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
