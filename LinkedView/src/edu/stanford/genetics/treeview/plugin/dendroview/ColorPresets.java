/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: ColorPresets.java,v $
 * $Revision: 1.1 $
 * $Date: 2006-08-16 19:13:45 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular,
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER
 */
package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorSet;

/**
 * This class encapsulates a list of Color presets. This is the class to edit
 * the default presets in...
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.1 $ $Date: 2006-08-16 19:13:45 $
 */

public class ColorPresets implements ConfigNodePersistent {

	private final static int dIndex = 0;
	/**
	 * holds the default color sets, which can be added at any time to the
	 * extant set
	 */
	public static ColorSet[] defaultColorSets;

	static {
		defaultColorSets = new ColorSet[2];
		defaultColorSets[0] = new ColorSet("RedGreen", "#FF0000", "#000000",
				"#00FF00", "#909090", "#FFFFFF");
		defaultColorSets[1] = new ColorSet("YellowBlue", "#FEFF00", "#000000",
				"#1BB7E5", "#909090", "#FFFFFF");
	}

	private Preferences configNode;
	

	// which preset to use if not by confignode?

	/**
	 * creates a new ColorPresets object and binds it to the node adds default
	 * Presets if none are currently set.
	 * 
	 * @param parent
	 *            node to bind to
	 */
	public ColorPresets(final Preferences parent) {
		super();
		setConfigNode(parent);
	}

	/** Constructor for the ColorPresets object */
	public ColorPresets() {

		super();

		// Get the global configNode
		configNode = Preferences.userRoot().node("TreeViewApp");
	}

	/* inherit description */
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node("ColorPresets");

		} else {
			LogBuffer.println("Could not find or create ColorPresets "
					+ "node because parentNode was null.");
		}
	}

	/**
	 * returns default preset, for use when opening a new file which has no
	 * color settings
	 */
	public int getDefaultIndex() {

		return configNode.getInt("default", dIndex);
	}

	/**
	 * True if there a particular preset which we are to default to.
	 * 
	 */
	public boolean isDefaultEnabled() {

		return (getDefaultIndex() != -1);
	}

	/**
	 * Gets the default <code>ColorSet</code>, according to this preset.
	 */
	public ColorSet getDefaultColorSet() {

		final int defaultPreset = getDefaultIndex();

		try {
			return getColorSet(defaultPreset);

		} catch (final Exception e) {
			return getColorSet(0);
		}
	}

	/**
	 * Sets the default to be the i'th color preset.
	 */
	public void setDefaultIndex(final int i) {

		configNode.putInt("default", i);
	}

	/** Adds the default color sets to the current presets */
	// public void addDefaultPresets() {
	//
	// for (int i = 0; i < defaultColorSets.length; i++) {
	//
	// defaultColorSets[i].setConfigNode(parentNode.node("DefaultColorSet"
	// + i));
	// addColorSet(defaultColorSets[i]);
	// }
	// }

	/**
	 * returns String [] of preset names for display
	 */
	public String[] getPresetNames() {

		final String[] childrenNodes = getRootChildrenNodes();
		return childrenNodes;
	}

	/**
	 * The current number of available presets.
	 */
	public int getNumPresets() {

		final String[] childrenNodes = getRootChildrenNodes();
		return childrenNodes.length;
	}

	/* inherit description */
	@Override
	public String toString() {

		final String[] childrenNodes = getRootChildrenNodes();
		// final ColorSet tmp = new ColorSet();
		final String[] names = getPresetNames();
		String ret = "No Presets";
		if (names.length > 0) {
			ret = "Default is " + names[getDefaultIndex()] + " index "
					+ getDefaultIndex() + "\n";
		}

		for (int index = 0; index < childrenNodes.length; index++) {

			// tmp.setConfigNode(configNode.node(childrenNodes[index]));
			// ret += tmp.toString() + "\n";
		}
		return ret;
	}

	/**
	 * returns the color set for the ith preset or null, if any exceptions are
	 * thrown.
	 */
	public ColorSet getColorSet(final int index) {

		if (index < defaultColorSets.length) {
			return defaultColorSets[index];
		} else {
			try {
				final String[] childrenNodes = getRootChildrenNodes();
				final ColorSet ret = new ColorSet(
						configNode.node(childrenNodes[index]));
				// ret.setConfigNode(configNode.node(childrenNodes[index]));
				return ret;

			} catch (final Exception e) {
				return null;
			}
		}
	}

	/**
	 * returns the color set for this name or null, if name not found in kids
	 */
	public ColorSet getColorSet(final String name) {

		for (int i = 0; i < defaultColorSets.length; i++) {
			if (defaultColorSets[i].getName() == name) {
				return defaultColorSets[i];
			}
		}
		final String[] childrenNodes = getRootChildrenNodes();

		// First check for the node with the supplied name.
		for (int i = 0; i < childrenNodes.length; i++) {

			final ColorSet ret = new ColorSet(configNode.node(childrenNodes[i]));
			if (name.equals(ret.getName())) {
				return ret;
			}
		}

		// Default to first defaultColorSet (RedGreen)
		return defaultColorSets[0];
	}

	/**
	 * constructs and adds a <code>ColorSet</code> with the specified
	 * attributes.
	 */
	public void addColorSet(final String name, final List<Color> colors,
			final List<Double> fractions, final String missing,
			final String empty) {
		final ColorSet newColorSet = new ColorSet(name, colors, fractions,
				missing, empty);
		addColorSet(newColorSet);
	}

	/**
	 * actually copies state of colorset, does not add the colorset itself but a
	 * copy.
	 */
	public void addColorSet(final ColorSet set) {

		// Make the children of ColorSet here by adding an int to the name?
		// final ColorSet preset = new ColorSet();
		final String[] childrenNodes = getRootChildrenNodes();
		boolean customFound = false;
		String customNode = "";

		for (final String node : childrenNodes) {

			final String default_name = "RedGreen";
			if (configNode.node(node).get("name", default_name)
					.equalsIgnoreCase("Custom")) {
				customFound = true;
				customNode = node;
			}
		}

		final ColorSet newColorSet = new ColorSet(set);
		if (customFound) {
			newColorSet.save(configNode.node(customNode));
		} else {
			int setNodeIndex = 0;
			setNodeIndex = getRootChildrenNodes().length + 1;
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
		configNode.remove(childrenNames[i]);
	}

	/**
	 * Resets all the stored colorsets to default.
	 */
	// public void reset() {
	//
	// final String[] childrenNodes = getRootChildrenNodes();
	//
	// for (int i = 0; i < childrenNodes.length; i++) {
	//
	// if (childrenNodes[i].contains("ColorSet")) {
	// configNode.remove(childrenNodes[i]);
	// }
	// }
	// addDefaultPresets();
	// }

	/**
	 * Returns the names of the current children of this class' root node.
	 * 
	 * @return
	 */
	public String[] getRootChildrenNodes() {

		if (configNode != null) {
			String[] childrenNodes;
			try {
				childrenNodes = configNode.childrenNames();
				return childrenNodes;

			} catch (final BackingStoreException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Returns the configNode of ColorPresets
	 * 
	 * @return
	 */
	public Preferences getConfigNode() {

		return configNode;
	}
}
