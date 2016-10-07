/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.UIManager;

import ColorChooser.ColorSchemeType;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * This class encapsulates a list of Color presets. This is the class to edit
 * the default presets in...
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.1 $ $Date: 2006-08-16 19:13:45 $
 */

public class ColorPresets implements ConfigNodePersistent {

	private final static int dIndex = 0;
	
	/*
	 * holds the default color sets, which can be added at any time to the
	 * extant set
	 */
	public static ColorSet[] defaultColorSets;

	static {
		defaultColorSets = new ColorSet[2];
		defaultColorSets[0] = new ColorSet(ColorSchemeType.REDGREEN.toString(), 
				new String[] {"#FF0000", "#000000", "#00FF00" }, "#8E8E8E", "#FFFFFF");
		defaultColorSets[1] = new ColorSet(ColorSchemeType.YELLOWBLUE.toString(), 
				new String[] {"#FEFF00", "#000000", "#1BB7E5" }, "#8E8E8E", "#FFFFFF");
	}

	private Preferences configNode;
	private String lastActiveColorScheme;

	/**
	 * creates a new ColorPresets object and binds it to the node adds default
	 * Presets if none are currently set.
	 *
	 * @param parent
	 *            node to bind to
	 */
	public ColorPresets(final Preferences parent) {

		super();

		Color sysBackground = UIManager.getColor("Panel.background");
		String sysBack = Integer.toHexString(sysBackground.getRGB());
		sysBack = sysBack.substring(2, sysBack.length());
		
		defaultColorSets[0].setMissing(sysBackground);
		defaultColorSets[1].setMissing(sysBackground);
		
		this.lastActiveColorScheme = ColorSchemeType.REDGREEN.toString();
	}

	/** Constructor for the ColorPresets object */
	public ColorPresets() {

		super();
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode == null) {
			LogBuffer.println("Could not find or create ColorPresets "
					+ "node because parentNode was null.");
			return;
		}
		
		this.configNode = parentNode.node(this.getClass().getSimpleName());
		requestStoredState();
	}
	
	/**
	 * @return the Preferences node of the ColorPresets class.
	 */
	public Preferences getConfigNode() {

		return configNode;
	}

	@Override
	public void requestStoredState() {
		
		if(configNode == null) {
			LogBuffer.println("Could not get stored state of " + getClass().getSimpleName() 
					+ " because Preferences node was null.");
			return;
		}
		
		this.lastActiveColorScheme = configNode.get("lastActiveColorScheme", ColorSchemeType.REDGREEN.toString());
	}

	@Override
	public void storeState() {
		
		if(configNode == null) {
			LogBuffer.println("Could not store state of " + getClass().getSimpleName() 
					+ " because Preferences node was null.");
			return;
		}
		
		configNode.put("lastActiveColorScheme", lastActiveColorScheme);
	}

	@Override
	public void importStateFrom(final Preferences oldNode) {
		
		if(oldNode == null) {
			LogBuffer.println("Could not import state to " + getClass().getSimpleName() 
					+ " because the old Preferences node was null.");
			return;
		}
		
		String oldLastActiveScheme = oldNode.get("lastActiveColorScheme", ColorSchemeType.REDGREEN.toString());
		configNode.put("lastActiveColorScheme", oldLastActiveScheme);
		this.lastActiveColorScheme = oldLastActiveScheme;
	}

	/**
	 * @return default preset, for use when opening a new file which has no
	 * color settings
	 */
	public int getDefaultIndex() {

		if(configNode == null) {
			LogBuffer.println("No Preferences node in ColorPresets defined.");
		}
		return configNode.getInt("default", dIndex);
	}
	
	/**
	 * @return The <code>ColorSchemeType</code> stored as the last active one. Default Red-Green if none wass stored.
	 */
	public ColorSchemeType getLastActiveColorScheme() {
		
		ColorSchemeType lastActive = ColorSchemeType.getMemberFromKey(lastActiveColorScheme);
		
		if(lastActive == null) {
			return ColorSchemeType.REDGREEN;
		}
		
		return lastActive;
	}
	
	public void setLastActiveColorScheme(ColorSchemeType scheme) {
		
		this.lastActiveColorScheme = scheme.toString();
	}

	/**
	 * @return true if there a particular preset which we are to default to.
	 */
	public boolean isDefaultEnabled() {

		return (getDefaultIndex() != -1);
	}

	/**
	 * @return the default <code>ColorSet</code>, according to this preset.
	 */
	public ColorSet getDefaultColorSet() {

		final int defaultPreset = getDefaultIndex();

		try {
			return getColorSet(defaultPreset);

		} catch (final Exception e) {
			LogBuffer.println("Could not get default ColorSet.");
			LogBuffer.logException(e);
			return getColorSet(0);
		}
	}

	/**
	 * Sets the default to be the i'th color preset.
	 */
	public void setDefaultIndex(final int i) {

		if(configNode == null) {
			LogBuffer.println("Cannot store default index. ColorPresets Preferences node is undefined.");
			return;
		}
		
		configNode.putInt("default", i);
	}

	/**
	 * @return String[] of preset names for display
	 */
	public String[] getPresetNames() {

		final String[] childrenNodes = getRootChildrenNodes();
		return childrenNodes;
	}

	/**
	 * @return the current number of available presets.
	 */
	public int getNumPresets() {

		return getPresetNames().length;
	}
	
	@Override
	public String toString() {

		final String[] names = getPresetNames();
		String ret = "No Presets";
		if (names.length > 0) {
			ret = "Default is " + names[getDefaultIndex()] + " index " + getDefaultIndex() + "\n";
		}
		
		return ret;
	}

	/**
	 * @return the color set for the i'th preset or null, if any exceptions are
	 * thrown.
	 */
	private ColorSet getColorSet(final int index) {

		// Init to Red-Green ColorSet
		ColorSet ret = defaultColorSets[0];
		
		// In case of a bad index
		if(index < 0) {
			LogBuffer.println("Cannot return ColorSet at " + index + " Returning default Red-Green.");
			return ret;
		}
		
		// Getting any of the default ColorSets
		if (index < defaultColorSets.length) {
			LogBuffer.println("Returning default ColorSet at " + index);
			return defaultColorSets[index];
		}

		// Getting a stored ColorSet
		try {
			final String[] childrenNodes = getRootChildrenNodes();
			if(index < childrenNodes.length) {
				ret = new ColorSet(configNode.node(childrenNodes[index]));
			}
			
			LogBuffer.println("Returning default Red-Green.");
			return ret;

		} catch (final Exception e) {
			LogBuffer.println("Error retrieving ColorSet. Returned default Red-Green.");
			LogBuffer.logException(e);
			return defaultColorSets[0];
		}
	}

	/**
	 * @param colorSchemeName - The name of the ColorSchemeType which a ColorSet represents.
	 * @return the color set for this name or null, if name not found in kids
	 */
	public ColorSet getColorSet(final String colorSchemeName) {

		if(colorSchemeName == null) {
			LogBuffer.println("ColorSet could not be returned because 'name' was null. Returned default Red-Green.");
			return defaultColorSets[0];
		}
		
		if(configNode == null) {
			LogBuffer.println("ColorSet could not be returned because no Preferences node was defined. "
					+ "Returned default Red-Green.");
			return defaultColorSets[0];
		}
		
		// Checking the defaults
		for (final ColorSet defaultColorSet : defaultColorSets) {
			if (defaultColorSet.getColorSchemeName().equals(colorSchemeName)) {
				LogBuffer.println("Found default ColorSet with name: " + colorSchemeName);
				return defaultColorSet;
			}
		}
		
		// Checking existing nodes
		final String[] childrenNodes = getRootChildrenNodes();
		for (final String childrenNode : childrenNodes) {
			final ColorSet ret = new ColorSet(configNode.node(childrenNode));
			if (colorSchemeName.equals(ret.getColorSchemeName())) {
				LogBuffer.println("Found child ColorSet with name: " + colorSchemeName);
				return ret;
			}
		}

		// Default to first defaultColorSet (Red-Green)
		LogBuffer.println("ColorSet (" + colorSchemeName + ") not found. Returned default Red-Green instead.");
		return defaultColorSets[0];	
	}
	
	/**
	 * @return the last active <code>ColorSet</code> or the default Red-Green, if none is defined.
	 */
	public ColorSet getLastActiveColorSet() {
		
		String lastActiveScheme = getLastActiveColorScheme().toString();
		return getColorSet(lastActiveScheme);
	}

    /**
     * Constructs and adds a <code>ColorSet</code> with the specified
	 * attributes.
     * @param colorSchemeName - The name of the ColorSchemeType this node represents.
     * @param colors - The array of colors for this ColorSet
     * @param fractions - The array of thumb fractions for this ColorSet
     * @param min - The minimum value of the ColorSet
     * @param max - The maximum value of the ColorSet
     * @param missing - The hex string which represents the missing data color
     * @param empty - The hex string which represents the empty data color
     */
	public void addColorSet(final String colorSchemeName, final List<Color> colors,
			final List<Float> fractions, final double min, final double max,
			final String missing, final String empty) {

		final ColorSet newColorSet = new ColorSet(colorSchemeName, colors, fractions, min, max, missing, empty);
		addColorSet(newColorSet);
	}

	/**
	 * Actually copies state of <code>ColorSet</code>, does not add the <code>ColorSet</code> itself.
	 * @param set - The new ColorSet to add to the Preferences node.
	 */
	public void addColorSet(final ColorSet set) {

		// Make the children of ColorSet here by adding an int to the name?
		final String[] childrenNodes = getRootChildrenNodes();

		// Seek existing 'Custom' node first
		int customIdx = checkNodeExists(ColorSchemeType.CUSTOM.toString());
		final ColorSet newColorSet = new ColorSet(set);
		String addNodeName = newColorSet.getColorSchemeName();
		
		if (customIdx > -1) {
			addNodeName = childrenNodes[customIdx];
		} 
		
		LogBuffer.println("Saving to ColorSet " + addNodeName);
		
		newColorSet.saveTo(configNode.node(addNodeName));
	}

	/**
	 * Remove color set permanently from presets
	 *
	 * @param i
	 *            index of color set
	 */
	public void removeColorSet(final int i) {

		final String[] childrenNodeNames = getRootChildrenNodes();
		
		if(i < 0 || i >= childrenNodeNames.length) {
			LogBuffer.println("Cannot remove child node from ColorPresets node because index is out of bounds.");
			return;
		}
		
		try {
			configNode.node(childrenNodeNames[i]).removeNode();
			
		} catch (BackingStoreException e) {
			LogBuffer.println("Something happened when trying to remove node: " + childrenNodeNames[i] + " Aborting.");
			LogBuffer.logException(e);
			return;
		}
	}

//	/**
//	 * Resets all the stored colorsets to default.
//	 */
//	 public void reset() {
//	
//		 final String[] childrenNodes = getRootChildrenNodes();
//	
//		 for (int i = 0; i < childrenNodes.length; i++) {
//	
//			 if (childrenNodes[i].contains("ColorSet")) {
//				 configNode.remove(childrenNodes[i]);
//			 }
//		 }
//		 addDefaultPresets();
//	 }
	
	/**
	 * Searches the children nodes of the <code>ColorPresets</code> <code>Preferences</code> node for a node 
	 * with the given scheme name.
	 * @param scheme - The scheme name to search for.
	 * @return If a node exists with the given scheme name, return its index. If it doesn't -1 will be returned.
	 */
	public int checkNodeExists(final String scheme) {
		
		String[] childrenNodes = getRootChildrenNodes();
		int nodeIdx = -1;
		
		for (int i = 0; i < childrenNodes.length; i++) {
			String nodeName = childrenNodes[i];
			final String nodeScheme = configNode.node(nodeName).get("colorSchemeType", 
					ColorSchemeType.REDGREEN.toString());
			if (nodeScheme.equalsIgnoreCase(scheme)) {
				nodeIdx = i;
			}
		}
		
		return nodeIdx;
	}

	/**
	 * Returns the names of the current children of this class' root node.
	 *
	 * @return A String list of children nodes or an empty String list 
	 * if no children nodes can be found.
	 */
	public String[] getRootChildrenNodes() {

		if (configNode != null) {
			String[] childrenNodes;
			try {
				childrenNodes = configNode.childrenNames();
				return childrenNodes;

			} catch (final BackingStoreException e) {
				LogBuffer.logException(e);
				LogBuffer.println("Issue when retrieving children nodes for Preferences.");
				return new String[0];
			}
		}

		return new String[0];
	}
}
