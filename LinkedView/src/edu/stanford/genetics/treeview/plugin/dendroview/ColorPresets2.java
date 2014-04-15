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
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * This class encapsulates a list of Color presets. This is the class to edit
 * the default presets in...
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.1 $ $Date: 2006-08-16 19:13:45 $
 */

public class ColorPresets2 implements ConfigNodePersistent {

	private final static int dIndex = 0;
	/**
	 * holds the default color sets, which can be added at any time to the
	 * extant set
	 */
	public static ColorSet2[] defaultColorSets;

	static {
		defaultColorSets = new ColorSet2[2];
		defaultColorSets[0] = new ColorSet2("RedGreen", "#FF0000", "#000000",
				"#00FF00", "#909090", "#FFFFFF");
		defaultColorSets[1] = new ColorSet2("YellowBlue", "#FEFF00", "#000000",
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
	public ColorPresets2(final Preferences parent) {

		super();
		setConfigNode(parent);
		final int nNames = getPresetNames().length;
		if (nNames == 0) {
			addDefaultPresets();
		}
	}

	/** Constructor for the ColorPresets object */
	public ColorPresets2() {

		super();
		
		// Get the global configNode
		configNode = Preferences.userRoot().node("TreeViewApp");
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
	public ColorSet2 getDefaultColorSet() {

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
	public void addDefaultPresets() {

		for (int i = 0; i < defaultColorSets.length; i++) {

			defaultColorSets[i].setConfigNode(configNode);
			addColorSet(defaultColorSets[i]);
		}
	}

	/**
	 * returns String [] of preset names for display
	 */
	public String[] getPresetNames() {

//		final ConfigNode aconfigNode[] = root.fetch("ColorSet");
		String[] childrenNodes = getRootChildrenNodes();
		return childrenNodes;
		
//		final String astring[] = new String[aconfigNode.length];
//		final ColorSet2 temp = new ColorSet2();

//		for (int i = 0; i < aconfigNode.length; i++) {

//			temp.setConfigNode(aconfigNode[i]);
//			astring[i] = temp.getName();
//		}
	}

	/**
	 * The current number of available presets.
	 */
	public int getNumPresets() {

//		final ConfigNode aconfigNode[] = root.fetch("ColorSet");
		String[] childrenNodes = getRootChildrenNodes();
		return childrenNodes.length;
	}

	/* inherit description */
	@Override
	public String toString() {

//		final ConfigNode aconfigNode[] = root.fetch("ColorSet");
		String[] childrenNodes = getRootChildrenNodes();
		final ColorSet2 tmp = new ColorSet2();
		final String[] names = getPresetNames();
		String ret = "No Presets";
		if (names.length > 0) {
			ret = "Default is " + names[getDefaultIndex()] + " index "
					+ getDefaultIndex() + "\n";
		}

		for (int index = 0; index < childrenNodes.length; index++) {

			tmp.setConfigNode(configNode.node(childrenNodes[index]));
			ret += tmp.toString() + "\n";
		}
		return ret;
	}

	/**
	 * returns the color set for the ith preset or null, if any exceptions are
	 * thrown.
	 */
	public ColorSet2 getColorSet(final int index) {

//		final ConfigNode aconfigNode[] = root.fetch("ColorSet");
		try {
			String[] childrenNodes = getRootChildrenNodes();
			final ColorSet2 ret = new ColorSet2();
			ret.setConfigNode(configNode.node(childrenNodes[index]));
			return ret;
			
		} catch (final Exception e) {
			return null;
		}
	}

	/**
	 * returns the color set for this name or null, if name not found in kids
	 */
	public ColorSet2 getColorSet(final String name) {

		String[] childrenNodes = getRootChildrenNodes();
		final ColorSet2 ret = new ColorSet2();
		
		// First check for the node with the supplied name.
		for (int i = 0; i < childrenNodes.length; i++) {

			ret.setConfigNode(configNode.node(childrenNodes[i]));

			if (name.equals(ret.getName())) {
				return ret;
			}
		}
		
		// Should always have a RedGreen and YellowBlue node.
		ret.setConfigNode(configNode.node(childrenNodes[0]));
		return ret;
	}

	/**
	 * constructs and adds a <code>ColorSet2</code> with the specified
	 * attributes.
	 */
	public void addColorSet(final String name, ArrayList<Color> colors, 
			ArrayList<Double> fractions, final String missing) {

		// Make the children of ColorSet here by adding an int to the name?
		final ColorSet2 preset = new ColorSet2();
		String[] childrenNodes = getRootChildrenNodes();
		boolean customFound = false;
		String customNode = "";
		
		for(String node : childrenNodes) {
			
			String default_name = "NoName";
			if(configNode.node(node).get("name", default_name)
					.equalsIgnoreCase("Custom")) {
				customFound = true;
				customNode = node;
			}
		}
		
		if(customFound) {
			preset.setConfigNode(configNode.node(customNode));
			preset.setColorList(colors);
			preset.setFractionList(fractions);
			preset.setMissing(missing);
			
		} else {
			int setNodeIndex = 0;
			setNodeIndex = getRootChildrenNodes().length + 1;
			preset.setConfigNode(configNode.node("ColorSet" + setNodeIndex));
			preset.setColorList(colors);
			preset.setFractionList(fractions);
			preset.setName(name);
			preset.setMissing(missing);
		}
	}

	/**
	 * actually copies state of colorset, does not add the colorset itself but a
	 * copy.
	 */
	public void addColorSet(final ColorSet2 set) {

		final ColorSet2 preset = new ColorSet2();
		// Make the children of ColorSet here by adding an int to the name?
		if (configNode != null) {
			int setNodeIndex = 0;
			setNodeIndex = getRootChildrenNodes().length + 1;
			preset.setConfigNode(configNode.node("ColorSet" + setNodeIndex));
		}
		preset.copyStateFrom(set);
	}

//	/* inherit description */
//	@Override
//	public void bindConfig(final Preferences configNode) {
//
//		root = configNode;
//		final int nNames = getPresetNames().length;
//		if (nNames == 0) {
//			addDefaultPresets();
//		}
//	}
	
	/* inherit description */
	@Override
	public void setConfigNode(Preferences parentNode) {

		if(parentNode != null) {
			this.configNode = parentNode.node("ColorPresets");
			
		} else {
			LogBuffer.println("Could not find or create ColorPresets " +
					"node because parentNode was null.");
		}
		
		final int nNames = getPresetNames().length;
		if (nNames == 0) {
			addDefaultPresets();
		}
	}

	/**
	 * Remove color set permanently from presets
	 * 
	 * @param i
	 *            index of color set
	 */
	public void removeColorSet(final int i) {

//		final ConfigNode aconfigNode[] = root.fetch("ColorSet");
		String[] childrenNames = getRootChildrenNodes();
		configNode.remove(childrenNames[i]);
	}

	/**
	 * Resets all the stored colorsets to default.
	 */
	public void reset() {

		String[] childrenNodes = getRootChildrenNodes();
		
		for(int i = 0; i < childrenNodes.length; i++) {
			
			if(childrenNodes[i].contains("ColorSet")) {
				configNode.remove(childrenNodes[i]);
			}
		}
		addDefaultPresets();
	}
	
	/**
	 * Returns the names of the current children of this class' root node.
	 * @return
	 */
	public String[] getRootChildrenNodes() {
		
		if(configNode != null) {
			String[] childrenNodes;
			try {
				childrenNodes = configNode.childrenNames();
				return childrenNodes;
				
			} catch (BackingStoreException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the configNode of ColorPresets
	 * @return
	 */
	public Preferences getConfigNode() {
		
		return configNode;
	}
}
