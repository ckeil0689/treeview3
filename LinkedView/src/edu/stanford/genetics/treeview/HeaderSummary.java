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

		if (parentNode != null) {
			this.configNode = parentNode.node(type);
			LogBuffer.println("Set up configNode for " + type);

		} else {
			LogBuffer.println("Could not find or create " + type
					+ " node because parentNode was null.");
			return;
		}
		synchronizeFrom();
	}

	public void setIncluded(final int[] newIncluded) {

		LogBuffer.println("Setting new included member.(" + type + ")");
		this.included = newIncluded;
		synchronizeTo();
		setChanged();
		notifyObservers();
	}
	
	public void setHeaders(final String[] headers) {
		
		LogBuffer.println("Setting new headers.(" + type + ")");
		this.headers = headers;
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
			final String incString = configNode.get("included", "[0]");
			if (incString.equals("[]")) {
				LogBuffer.println("The included key has no values stored. Including nothing. (" + type + ")");
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
					LogBuffer.println("HeaderSummary has trouble "
							+ "restoring included list from " 
							+ Arrays.toString(inclArray));
					LogBuffer.println("NumberFormatException in "
							+ "synchronizeFrom() in " + "HeaderSummary: "
							+ e.getMessage());
					return;
				}
				
				array = adjustIncludedHeaders(array);
				setIncluded(array);
			}
		} else if(headers != null && headers.length > 0) {
			LogBuffer.println("There are headers defined but no key stored for included indices. Setting a default. (" + type + ")");
			setIncluded(new int[]{0});
		} else {
			LogBuffer.println("There are no headers defined. Including nothing. (" + type + ")");
			setIncluded(new int[0]);
		}
	}
	
	private String[] getValuesFromStoredString(String storedVal) {
		
		String[] storedVals = storedVal
				.replaceAll(" ", "")
				.replaceAll("\\[", "")
				.replaceAll("\\]", "")
				.split(",");
		
		return storedVals;
	}
	
	/**
	 * Checks if the included indices match with the stored header Strings.
	 * During clustering or reworking a file, actual header names may shift in
	 * their position and the stored index alone may not be representative
	 * for the last selected header.
	 * @param included The included indices.
	 */
	private int[] adjustIncludedHeaders(int[] included) {
		
		if(headers == null) {
			LogBuffer.println("headers are null in " + type);
			return included;
		}
		
		int[] newIncluded = included;
		
		if (nodeHasAttribute("includedNames")) {
			String names = configNode.get("includedNames", "");
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
		
		return newIncluded;
	}

	/**
	 * Stores included indices as a String in the Preferences node. 
	 */
	private void synchronizeTo() {

		if (configNode == null) {
			return;
		}

		final int[] vec = getIncluded();
		
		final String[] names = new String[vec.length];
		for(int i = 0; i < names.length; i++) {
			int idx = vec[i];
			if(idx < headers.length) {
				names[i] = headers[idx];
			}
		}
		
		LogBuffer.println("Storing new included labels indices: (" + type + ")" + Arrays.toString(vec));
		LogBuffer.println("Storing new included labels names: (" + type + ")" + Arrays.toString(names));
		
		configNode.put("included", Arrays.toString(vec));//temp.toString());
		configNode.put("includedNames", Arrays.toString(names));
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
