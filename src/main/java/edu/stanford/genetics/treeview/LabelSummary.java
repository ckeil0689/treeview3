/*
 * BEGIN_HEADER TreeView 3
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

import util.Helper;

/** this class generates a single string summary of a LabelInfo. */
public class LabelSummary extends Observable implements ConfigNodePersistent,
	ModelLoadReset {

	private final int[] d_included = new int[] {0};
	private final String[] d_labelTypes = new String[] {"default"};
	private final String type;

	private Preferences configNode;
	private String[] labelTypes;
	private int[] included;

	public LabelSummary(final String type) {

		super();
		this.type = type;
		this.included = d_included;
	}

	@Override
	public void resetDefaults() {

		this.included = d_included;
		this.labelTypes = d_labelTypes;
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if(parentNode == null) {
			LogBuffer.println("Could not find or create " + type +
				" node because parentNode was null.");
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

		if(configNode == null) {
			LogBuffer.println("Could not store state for " + type +
				" because configNode was null.");
			return;
		}

		final int[] vec = getIncluded();
		configNode.put("included", Arrays.toString(vec));

		if(labelTypes == null) {
			LogBuffer.println("Could not store label types. " +
				"No label types defined yet.");
			return;
		}

		final String[] names = new String[vec.length];
		for(int i = 0; i < names.length; i++) {
			int idx = vec[i];
			if(idx < labelTypes.length) {
				names[i] = labelTypes[idx];
			}
		}
		configNode.put("includedNames", Arrays.toString(names));
	}

	@Override
	public void importStateFrom(Preferences oldNode) {

		if(oldNode == null) {
			LogBuffer.println("Node to be imported was null.");
			return;
		}

		if(nodeHasAttribute("includedNames", oldNode)) {
			String importNames = oldNode.get("includedNames", "[0]");
			configNode.put("includedNames", importNames);
		}

		if(nodeHasAttribute("included", oldNode)) {
			final String incString = oldNode.get("included", "[0]");
			if(incString.equals("[]")) {
				LogBuffer.println("The included key has no values stored. " +
					"Including nothing. (" + type + ")");
				setIncluded(new int[0]);

			}
			else {
				String[] inclArray =
					Helper.getStringValuesFromKeyString(incString);
				int[] array = new int[inclArray.length];

				try {
					for(int i = 0; i < inclArray.length; i++) {
						String elem = inclArray[i];
						array[i] = Integer.parseInt(elem);
					}

				}
				catch(final NumberFormatException e) {
					LogBuffer.logException(e);
					LogBuffer.println("LabelSummary has trouble " +
						"restoring included list from " +
						Arrays.toString(inclArray));
					setIncluded(new int[0]);
					return;
				}

				array = adjustIncl(array);
				setIncluded(array);
			}
		}
		else if(labelTypes != null && labelTypes.length > 0) {
			LogBuffer.println("There are labels defined but no key stored " +
				"for included indices. Setting a default. (" +
				type + ")");
			setIncluded(new int[] {0});

		}
		else {
			LogBuffer.println("There are no labels defined. " +
				"Including nothing. (" + type + ")");
			setIncluded(new int[0]);
		}
	}

	/** Setter for the list of indices describing which labels should be
	 * included when returning a "summary" string. For LabelView this would
	 * mean that a summary of labels at a certain index will be returned based
	 * on which label types the user selected.
	 * 
	 * @param newIncluded - An array of indices describing the selected label
	 *          types. */
	public void setIncluded(final int[] newIncluded) {

		this.included = newIncluded;
		storeState();
		setChanged();
		notifyObservers();
	}

	/**
	 * Setter for the current available label types in a loaded file. Setting
	 * the label types is vital for ensuring label type selection consistency
	 * because a purely index-based approach does not account for label types
	 * that moved their position in the label type list, for example after
	 * clustering.
	 * 
	 * @param label types - The current label types of a loaded file. */
	public void setLabelTypes(final String[] labelTypes) {

		this.labelTypes = labelTypes;
		int[] adjustedIncluded = adjustIncl(getIncluded());
		setIncluded(adjustedIncluded);
		setChanged();
		notifyObservers();
	}

	/** @return included - A list of included label type indices. */
	public int[] getIncluded() {

		return included;
	}

	/** @return the best possible summary for the specified index.
	 *         If no label types are applicable, will return the empty string.*/
	public String getSummary(final LabelInfo labelInfo, final int index) {

		String[] strings = null;
		try {
			strings = labelInfo.getLabels(index);

		}
		catch(final java.lang.ArrayIndexOutOfBoundsException e) {
			LogBuffer.println("index: " + index +
				" out of bounds on labels, continuing");
			LogBuffer.println("ArrayIndexOutOfBoundsException in " +
				"getSummary() in LabelSummary: " + e.getMessage());
			return null;
		}

		if(strings == null) { return ""; }

		final StringBuffer out = new StringBuffer();
		int count = 0;
		if(included.length == 0) { return ""; }

		for(final int element : included) {
			try {
				final String test = strings[element];
				if(test != null) {
					if(count != 0) {
						out.append(", ");
					}
					out.append(test);
					count++;
				}
			}
			catch(final java.lang.ArrayIndexOutOfBoundsException e) {
				LogBuffer.logException(e);
				break;
			}
		}

		return (count == 0) ? "" : out.toString();
	}

	/** Retrieves a String array containing the labels for all included
	 * label types at a certain index.
	 * 
	 * @param labelInfo - The LabelInfo object from which to take the labels.
	 * @param idx - The axis index of the label to be returned.
	 * @return A String array of labels at a defined index. */
	public String[] getSummaryArray(final LabelInfo labelInfo, final int idx) {

		String[] strings = null;
		try {
			strings = labelInfo.getLabels(idx);

		}
		catch(final java.lang.ArrayIndexOutOfBoundsException e) {
			LogBuffer.logException(e);
			return null;
		}

		if(strings == null) { return null; }

		if(included.length == 0) { return null; }

		final String[] out = new String[included.length];
		int count = 0;
		for(final int element : included) {
			try {
				final String test = strings[element];
				out[count] = test;
				count++;
			}
			catch(final java.lang.ArrayIndexOutOfBoundsException e) {
				LogBuffer.logException(e);
				break;
			}
		}
		return out;
	}

	/** Checks if the included indices match with the stored label type strings.
	 * During clustering or reworking a file, actual label types may shift in
	 * their position and the stored index alone may not be representative
	 * for the last selected label type.
	 * 
	 * @param included The included indices. */
	private int[] adjustIncl(int[] included) {

		if(labelTypes == null) {
			LogBuffer.println("label types are null in " + type);
			return new int[] {};
		}

		if(labelTypes.length == 0) {
			LogBuffer.println("No label types set, cannot include any labels.");
			return new int[] {};
		}

		String[] inclNames;
		int[] newIncluded = included;

		if(nodeHasAttribute("includedNames", configNode)) {
			String names = configNode.get("includedNames", "[0]");
			inclNames = Helper.getStringValuesFromKeyString(names);

			int inclSize = inclNames.length;
			newIncluded = new int[inclSize];

			for(int i = 0; i < inclNames.length; i++) {
				String name = inclNames[i];
				for(int j = 0; j < labelTypes.length; j++) {
					if(name.equals(labelTypes[j])) {
						newIncluded[i] = j;
						break;
					}
				}
			}
			/* If no included label types were found, at least ensure 
			 * that included[] is not out of bounds */
		}
		else {
			inclNames = null;
			int maxSize = 0;
			// Shrink if necessary
			for(int i = 0; i < included.length; i++) {
				if(included[i] >= labelTypes.length) {
					maxSize = i + 1;
					break;
				}
			}

			// Create adjusted array
			newIncluded = new int[maxSize];

			// Fill with pre-included values that may be included again
			for(int i = 0; i < newIncluded.length; i++) {
				newIncluded[i] = included[i];
			}
		}

		// Ensure ascending order, just in case
		Arrays.sort(newIncluded);

		/* Make sure to display first label type as default. Note, this must
		 * still allow the user to explicitly included nothing. */
		if(labelTypes.length > 0 && newIncluded.length == 0 &&
			inclNames == null) {

			newIncluded = new int[] {0};
		}

		return newIncluded;
	}

	/** Checks if a Preferences node contains the key name.
	 * 
	 * @param name - The key to check for.
	 * @param node - The Preferences node to check.
	 * @return boolean Whether key exists or not. */
	public boolean nodeHasAttribute(final String name, final Preferences node) {

		if(node == null) { return false; }

		try {
			final String[] keys = node.keys();

			for(final String key : keys) {
				if(key.equalsIgnoreCase(name)) { return true; }
			}
			return false;

		}
		catch(final BackingStoreException e) {
			LogBuffer.logException(e);
			return false;
		}
	}

	@Override
	public String toString() {

		String str = super.toString();
		str += "// included_class (" +	Arrays.toString(included) + "/ " +
			"included_stored (" + configNode.get("included", "default") +
			")/ includedNames_stored (" +
			configNode.get("includedNames", "default") +
			")/ headers_class (" + Arrays.toString(labelTypes) + ")";
		return str;
	}
}
