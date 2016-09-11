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
 * this class generates a single string summary of a LabelInfo.
 */
public class LabelSummary extends Observable 
implements ConfigNodePersistent, ModelLoadReset {

	private final int[] d_included = new int[] { 0 };
	private final String[] d_prefixes = new String[]{"default"};
	private final String type;
	
	private Preferences configNode;
	private String[] prefixes;
	private int[] included;

	public LabelSummary(final String type) {

		super();
		this.type = type;
		this.included = d_included;
	}
	
	@Override
	public void resetDefaults() {
		
		this.included = d_included;
		this.prefixes = d_prefixes;
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
		
		if(prefixes == null) {
			LogBuffer.println("Could not store label prefixes. "
					+ "No label prefixes defined yet.");
			return;
		}
		
		final String[] names = new String[vec.length];
		for(int i = 0; i < names.length; i++) {
			int idx = vec[i];
			if(idx < prefixes.length) {
				names[i] = prefixes[idx];
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
			String importNames = oldNode.get("includedNames", "[0]");
			configNode.put("includedNames", importNames);
		}

		if (nodeHasAttribute("included", oldNode)) {
			final String incString = oldNode.get("included", "[0]");
			if (incString.equals("[]")) {
				LogBuffer.println("The included key has no values stored. "
						+ "Including nothing. (" + type + ")");
				setIncluded(new int[0]);

			} else {
				String[] inclArray = getStringValuesFromKeyString(incString);
				int[] array = new int[inclArray.length];
				
				try {
					for(int i = 0; i < inclArray.length; i++) {
						String elem = inclArray[i];
						array[i] = Integer.parseInt(elem);
					}

				} catch (final NumberFormatException e) {
					LogBuffer.logException(e);
					LogBuffer.println("LabelSummary has trouble "
							+ "restoring included list from " 
							+ Arrays.toString(inclArray));
					setIncluded(new int[0]);
					return;
				}

				array = adjustIncl(array);
				setIncluded(array);
			}
		} else if(prefixes != null && prefixes.length > 0) {
			LogBuffer.println("There are labels defined but no key stored "
					+ "for included indices. Setting a default. "
					+ "(" + type + ")");
			setIncluded(new int[]{0});
			
		} else {
			LogBuffer.println("There are no labels defined. "
					+ "Including nothing. (" + type + ")");
			setIncluded(new int[0]);
		}
	}

	/**
	 * Setter for the list of indices describing which labels should be 
	 * included when returning a "summary" string. For LabelView this would
	 * mean that a summary of labels at a certain index will be returned based
	 * on which prefixes the user selected.
	 * @param newIncluded - An array of indices describing the selected prefixes.
	 */
	public void setIncluded(final int[] newIncluded) {

		this.included = newIncluded;
		storeState();
		setChanged();
		notifyObservers();
	}
	
	/**
	 * Setter for the current available prefixes in a loaded file. Setting the
	 * prefixes is vital for ensuring prefix selection consistency because
	 * a purely index-based approach does not account for prefixes that moved
	 * their position in the prefix list, for example after clustering.
	 * @param prefixes - The current prefixes of a loaded file.
	 */
	public void setPrefixes(final String[] prefixes) {
		
		this.prefixes = prefixes;
		int[] adjustedIncluded = adjustIncl(getIncluded());
		setIncluded(adjustedIncluded);
		storeState();
		setChanged();
		notifyObservers();
	}

	/**
	 * @return included - A list of included prefix indices.
	 */
	public int[] getIncluded() {

		return included;
	}

	/**
	 * @return the best possible summary for the specified index.
	 * If no prefixes are applicable, will return the empty string.
	 */
	public String getSummary(final LabelInfo labelInfo, final int index) {

		String[] strings = null;
		try {
			strings = labelInfo.getLabels(index);

		} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
			LogBuffer.println("index " + index
					+ " out of bounds on labels, continuing");
			LogBuffer.println("ArrayIndexOutOfBoundsException in "
					+ "getSummary() in LabelSummary: " + e.getMessage());
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
	 * prefixes at a certain index.
	 * @param labelInfo The LabelInfo object from which to take the labels.
	 * @param idx The axis index of the label to be returned. 
	 * @return A String array of labels at a defined index.
	 */
	public String[] getSummaryArray(final LabelInfo labelInfo, final int idx) {

		String[] strings = null;
		try {
			strings = labelInfo.getLabels(idx);

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
	 * Preferences nodes can only store String arrays as a single string.
	 * This method is a helper used to convert such strings back to an array
	 * by removing brackets, spaces, and splitting the String up at commas.
	 * @param keyString - A String object, should be a single String 
	 * representing comma-separated String array.
	 * @return An array of Strings
	 */
	private String[] getStringValuesFromKeyString(final String keyString) {
		
		String[] storedVals = keyString
				.replaceAll("\\[", "")
				.replaceAll("\\]", "")
				.split(",");
		
		/* No stored values case. Previous code will create a String array of
		 * size 1 (empty string) if "[]" was stored in Preferences node, but
		 * we need an empty String array */
		if(storedVals.length == 1 && storedVals[0].equals("")) {
			return new String[0];
		}
		
		/* Strings may have spaces within them, so extra trimming here. */
		for(int i = 0; i < storedVals.length; i++) {
			String s = storedVals[i];
			storedVals[i] = s.trim();
		}
		
		return storedVals;
	}
	
	/**
	 * Checks if the included indices match with the stored prefix strings.
	 * During clustering or reworking a file, actual prefixes may shift in
	 * their position and the stored index alone may not be representative
	 * for the last selected prefix.
	 * @param included The included indices.
	 */
	private int[] adjustIncl(int[] included) {
		
		if(prefixes == null) {
			LogBuffer.println("prefixes are null in " + type);
			return new int[0];
		}
		
		String[] inclNames;
		int[] newIncluded = included;
		
		if (nodeHasAttribute("includedNames", configNode)) {
			String names = configNode.get("includedNames", "[0]");
			inclNames = getStringValuesFromKeyString(names);
			
			int inclSize = inclNames.length;
			newIncluded = new int[inclSize];
			
			for(int i = 0; i < inclNames.length; i++) {
				String name = inclNames[i];
				for(int j = 0; j < prefixes.length; j++) {
					if(name.equals(prefixes[j])) {
						newIncluded[i] = j;
						break;
					}
				}
			}
		/* If no included prefixes were found, at least ensure 
		 * that included[] is not out of bounds */
		} else {
			inclNames = null;
	        int maxSize = 0;
	        /* Shrink if necessary */
			for(int i = 0; i < included.length; i++) {
				if(included[i] >= prefixes.length) {
					maxSize = i + 1;
					break;
				}
			}
			
			/* Create adjusted array */
			newIncluded = new int[maxSize];
			
			/* Fill with pre-included values that may be included again */
			for(int i = 0; i < newIncluded.length; i++) {
				newIncluded[i] = included[i];
			}
		}
		
		/* Ensure ascending order, just in case */
		Arrays.sort(newIncluded);
		
		/* Make sure to display first prefix as default. Note, this must
		 * still allow the user to explicitly included nothing. */
		if(prefixes.length > 0 && newIncluded.length == 0 && inclNames == null) {
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
	
	@Override
	public String toString() {
		
		String str = super.toString();
		str += "// included_class (" + Arrays.toString(included) + "/ "
				+ "included_stored (" + configNode.get("included", "default") 
				+ ")/ includedNames_stored (" + configNode.get("includedNames", 
						"default") + ")/ headers_class (" 
				+ Arrays.toString(prefixes) + ")";
		return str;
	}
}
