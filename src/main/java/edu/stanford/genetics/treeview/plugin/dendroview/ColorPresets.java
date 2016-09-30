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
	}
	
	/**
	 * @return the Preferences node of the ColorPresets class.
	 */
	public Preferences getConfigNode() {

		return configNode;
	}

	@Override
	public void requestStoredState() {
		LogBuffer.println("No state to restore in " + this.getClass().getName());
	}

	@Override
	public void storeState() {
		LogBuffer.println("No state to store in " + this.getClass().getName());
		
	}

	@Override
	public void importStateFrom(final Preferences oldNode) {
		LogBuffer.println("No state to import in " + this.getClass().getName());
	}

	/**
	 * @return default preset, for use when opening a new file which has no
	 * color settings
	 */
	public int getDefaultIndex() {

		return configNode.getInt("default", dIndex);
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
	public ColorSet getColorSet(final int index) {

		if (index < defaultColorSets.length) {
			return defaultColorSets[index];
		}

		try {
			final String[] childrenNodes = getRootChildrenNodes();
			final ColorSet ret = new ColorSet(configNode.node(childrenNodes[index]));
			return ret;

		} catch (final Exception e) {
			LogBuffer.println("Error retrieving ColorSet. Returned default Red-Green.");
			LogBuffer.logException(e);
			return defaultColorSets[0];
		}
	}

	/**
	 * @return the color set for this name or null, if name not found in kids
	 */
	public ColorSet getColorSet(final String name) {

		if(name == null) {
			LogBuffer.println("ColorSet could not be returned because 'name' was null. Returned default Red-Green.");
			return defaultColorSets[0];
		}
		
		// Checking the defaults
		for (final ColorSet defaultColorSet : defaultColorSets) {
			if (defaultColorSet.getName().equals(name)) {
				return defaultColorSet;
			}
		}
		
		final String[] childrenNodes = getRootChildrenNodes();
		for (final String childrenNode : childrenNodes) {
			final ColorSet ret = new ColorSet(configNode.node(childrenNode));
			if (name.equals(ret.getName())) {
				return ret;
			}
		}

		// Default to first defaultColorSet (Red-Green)
		LogBuffer.println("ColorSet (" + name + ") not found. Returned default Red-Green instead.");
		return defaultColorSets[0];
	}

	/**
	 * constructs and adds a <code>ColorSet</code> with the specified
	 * attributes.
	 */
	public void addColorSet(final String name, final List<Color> colors,
			final List<Float> fractions, final double min, final double max,
			final String missing, final String empty) {

		final ColorSet newColorSet = new ColorSet(name, colors, fractions, min, max, missing, empty);
		addColorSet(newColorSet);
	}

	/**
	 * Actually copies state of <code>ColorSet</code>, does not add the <code>ColorSet</code> itself.
	 * @param set - The new ColorSet to add to the Preferences node.
	 */
	public void addColorSet(final ColorSet set) {

		LogBuffer.println("Adding ColorSet: " + set.getName());
		// Make the children of ColorSet here by adding an int to the name?
		final String[] childrenNodes = getRootChildrenNodes();
		boolean isCustomFound = false;
		String customNode = "";

		// Seek existing 'Custom' node
		for (final String node : childrenNodes) {
			final String nodeName = configNode.node(node).get("name", ColorSchemeType.REDGREEN.toString());
			if (nodeName.equalsIgnoreCase(ColorSchemeType.CUSTOM.toString())) {
				isCustomFound = true;
				customNode = node;
			}
		}

		final ColorSet newColorSet = new ColorSet(set);
		if (isCustomFound) {
			newColorSet.save(configNode.node(customNode));

		} else {
			int setNodeIndex = getRootChildrenNodes().length + 1;
			newColorSet.save(configNode.node("ColorSet" + setNodeIndex));
		}
	}

	/**
	 * Remove color set permanently from presets
	 *
	 * @param i
	 *            index of color set
	 */
	public void removeColorSet(final int i) {

		final String[] childrenNames = getRootChildrenNodes();
		
		if(i < 0 || i >= childrenNames.length) {
			LogBuffer.println("Cannot remove child node from ColorPresets node because index is out of bounds.");
			return;
		}
		
		configNode.remove(childrenNames[i]);
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
