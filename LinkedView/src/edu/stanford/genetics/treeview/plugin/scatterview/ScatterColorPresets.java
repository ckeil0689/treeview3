/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: ScatterColorPresets.java,v $
 * $Revision: 1.1 $
 * $Date: 2006-08-16 19:13:49 $
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
package edu.stanford.genetics.treeview.plugin.scatterview;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.DummyConfigNode;

/**
 * This class encapsulates a list of ScatterColorSet presets.
 */
public class ScatterColorPresets {
	private ConfigNode root;
	private final static int dIndex = 0; // which preset to use if not by
											// confignode?

	/**
	 * creates a new ColorPresets object and binds it to the node
	 * 
	 * adds default Presets if none are currently set.
	 */
	public ScatterColorPresets(final ConfigNode parent) {
		super();
		bindConfig(parent);
		final int nNames = getPresetNames().length;
		if (nNames == 0) {
			addDefaultPresets();
		}

	}

	public ScatterColorPresets() {
		this(new DummyConfigNode("ScatterColorPresets"));
	}

	/**
	 * returns default preset, for use when opening a new file which has no
	 * color settings
	 */
	public int getDefaultIndex() {
		return root.getAttribute("default", dIndex);
	}

	public boolean isDefaultEnabled() {
		return (getDefaultIndex() != -1);
	}

	public ScatterColorSet getDefaultColorSet() {
		final int defaultPreset = getDefaultIndex();
		try {
			return getColorSet(defaultPreset);
		} catch (final Exception e) {
			return getColorSet(0);
		}
	}

	public void setDefaultIndex(final int i) {
		root.setAttribute("default", i, dIndex);
	}

	public static ScatterColorSet[] defaultColorSets;

	static {
		defaultColorSets = new ScatterColorSet[2];
		defaultColorSets[0] = new ScatterColorSet("BlackBG", "#000000",
				"#00FF00", "#FFFF00", "#FFFFFF");
		defaultColorSets[1] = new ScatterColorSet("WhiteBG", "#FFFFFF",
				"#00FF00", "#999900", "#000000");
	}

	public void addDefaultPresets() {
		for (int i = 0; i < defaultColorSets.length; i++) {
			addColorSet(defaultColorSets[i]);
		}
	}

	/**
	 * returns String [] of preset names for display
	 */
	public String[] getPresetNames() {
		final ConfigNode aconfigNode[] = root.fetch("ScatterColorSet");
		final String astring[] = new String[aconfigNode.length];
		final ScatterColorSet temp = new ScatterColorSet("TempColorSet");
		for (int i = 0; i < aconfigNode.length; i++) {
			temp.bindConfig(aconfigNode[i]);
			astring[i] = temp.getName();
		}
		return astring;
	}

	public int getNumPresets() {
		final ConfigNode aconfigNode[] = root.fetch("ScatterColorSet");
		return aconfigNode.length;
	}

	@Override
	public String toString() {
		final ConfigNode aconfigNode[] = root.fetch("ScatterColorSet");
		final ScatterColorSet tmp = new ScatterColorSet();
		String ret = "Default is " + getPresetNames()[getDefaultIndex()]
				+ " index " + getDefaultIndex() + "\n";
		for (int index = 0; index < aconfigNode.length; index++) {
			tmp.bindConfig(aconfigNode[index]);
			ret += tmp.toString() + "\n";
		}
		return ret;
	}

	/**
	 * returns the color set for the ith preset or null, if any exceptions are
	 * thrown.
	 */
	public ScatterColorSet getColorSet(final int index) {
		final ConfigNode aconfigNode[] = root.fetch("ScatterColorSet");
		try {
			final ScatterColorSet ret = new ScatterColorSet();
			ret.bindConfig(aconfigNode[index]);
			return ret;
		} catch (final Exception e) {
			return null;
		}
	}

	/**
	 * returns the color set for this name or null, if name not found in kids
	 */
	public ScatterColorSet getColorSet(final String name) {
		final ConfigNode aconfigNode[] = root.fetch("ScatterColorSet");
		final ScatterColorSet ret = new ScatterColorSet();
		for (int i = 0; i < aconfigNode.length; i++) {
			ret.bindConfig(aconfigNode[i]);
			if (name.equals(ret.getName())) {
				return ret;
			}
		}
		return null;
	}

	/**
	 * actually copies state of colorset, does not add the colorset itself but a
	 * copy.
	 */
	public void addColorSet(final ScatterColorSet set) {
		final ScatterColorSet preset = new ScatterColorSet("AddingColorSet");
		if (root != null)
			preset.bindConfig(root.create("ScatterColorSet"));
		preset.copyStateFrom(set);
	}

	public void bindConfig(final ConfigNode configNode) {
		root = configNode;
		final int nNames = getPresetNames().length;
		if (nNames == 0) {
			addDefaultPresets();
		}

	}

	public void removeColorSet(final int i) {
		final ConfigNode aconfigNode[] = root.fetch("ScatterColorSet");
		root.remove(aconfigNode[i]);
	}

}
