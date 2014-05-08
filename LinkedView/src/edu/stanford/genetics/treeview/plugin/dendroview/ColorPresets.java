///* BEGIN_HEADER                                              Java TreeView
// *
// * $Author: rqluk $
// * $RCSfile: ColorPresets.java,v $
// * $Revision: 1.1 $
// * $Date: 2006-08-16 19:13:45 $
// * $Name:  $
// *
// * This file is part of Java TreeView
// * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
// *
// * This software is provided under the GNU GPL Version 2. In particular,
// *
// * 1) If you modify a source file, make a comment in it containing your name and the date.
// * 2) If you distribute a modified version, you must do it under the GPL 2.
// * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
// *
// * A full copy of the license can be found in gpl.txt or online at
// * http://www.gnu.org/licenses/gpl.txt
// *
// * END_HEADER
// */
//package edu.stanford.genetics.treeview.plugin.dendroview;
//
//import java.util.prefs.Preferences;
//
//import edu.stanford.genetics.treeview.ConfigNodePersistent;
//
///**
// * This class encapsulates a list of Color presets. This is the class to edit
// * the default presets in...
// * 
// * @author Alok Saldanha <alok@genome.stanford.edu>
// * @version @version $Revision: 1.1 $ $Date: 2006-08-16 19:13:45 $
// */
//
//public class ColorPresets implements ConfigNodePersistent {
//
//	private final static int dIndex = 0;
//	/**
//	 * holds the default color sets, which can be added at any time to the
//	 * extant set
//	 */
//	public static ColorSet[] defaultColorSets;
//
//	static {
//		defaultColorSets = new ColorSet[2];
//		defaultColorSets[0] = new ColorSet("RedGreen", "#FF0000", "#000000",
//				"#00FF00", "#909090", "#FFFFFF");
//		defaultColorSets[1] = new ColorSet("YellowBlue", "#FEFF00", "#000000",
//				"#1BB7E5", "#909090", "#FFFFFF");
//	}
//
//	private Preferences root;
//
//	// which preset to use if not by confignode?
//
//	/**
//	 * creates a new ColorPresets object and binds it to the node adds default
//	 * Presets if none are currently set.
//	 * 
//	 * @param parent
//	 *            node to bind to
//	 */
//	public ColorPresets(final Preferences parent) {
//
//		super();
//		bindConfig(parent);
//		final int nNames = getPresetNames().length;
//		if (nNames == 0) {
//			addDefaultPresets();
//		}
//	}
//
//	/** Constructor for the ColorPresets object */
//	public ColorPresets() {
//
//		super();
////		root = new DummyConfigNode("ColorPresets");
//		root = Preferences.userRoot().node(this.getClass().getName());
//	}
//
//	/**
//	 * returns default preset, for use when opening a new file which has no
//	 * color settings
//	 */
//	public int getDefaultIndex() {
//
//		return root.getInt("default", dIndex);
//	}
//
//	/**
//	 * True if there a particular preset which we are to default to.
//	 * 
//	 */
//	public boolean isDefaultEnabled() {
//
//		return (getDefaultIndex() != -1);
//	}
//
//	/**
//	 * Gets the default <code>ColorSet</code>, according to this preset.
//	 */
//	public ColorSet getDefaultColorSet() {
//
//		final int defaultPreset = getDefaultIndex();
//
//		try {
//			return getColorSet(defaultPreset);
//
//		} catch (final Exception e) {
//			return getColorSet(0);
//		}
//	}
//
//	/**
//	 * Sets the default to be the i'th color preset.
//	 */
//	public void setDefaultIndex(final int i) {
//
//		root.putInt("default", i);
//	}
//
//	/** Adds the default color sets to the current presets */
//	public void addDefaultPresets() {
//
//		for (int i = 0; i < defaultColorSets.length; i++) {
//
//			addColorSet(defaultColorSets[i]);
//		}
//	}
//
//	/**
//	 * returns String [] of preset names for display
//	 */
//	public String[] getPresetNames() {
//
//		final Preferences aconfigNode[] = root.fetch("ColorSet");
//		final String astring[] = new String[aconfigNode.length];
//		final ColorSet temp = new ColorSet();
//
//		for (int i = 0; i < aconfigNode.length; i++) {
//
//			temp.bindConfig(aconfigNode[i]);
//			astring[i] = temp.getName();
//		}
//		return astring;
//	}
//
//	/**
//	 * The current number of available presets.
//	 */
//	public int getNumPresets() {
//
//		final ConfigNode aconfigNode[] = root.fetch("ColorSet");
//		return aconfigNode.length;
//	}
//
//	/* inherit description */
//	@Override
//	public String toString() {
//
//		final ConfigNode aconfigNode[] = root.fetch("ColorSet");
//		final ColorSet tmp = new ColorSet();
//		final String[] names = getPresetNames();
//		String ret = "No Presets";
//		if (names.length > 0) {
//			ret = "Default is " + names[getDefaultIndex()] + " index "
//					+ getDefaultIndex() + "\n";
//		}
//
//		for (int index = 0; index < aconfigNode.length; index++) {
//
//			tmp.bindConfig(aconfigNode[index]);
//			ret += tmp.toString() + "\n";
//		}
//		return ret;
//	}
//
//	/**
//	 * returns the color set for the ith preset or null, if any exceptions are
//	 * thrown.
//	 */
//	public ColorSet getColorSet(final int index) {
//
//		final ConfigNode aconfigNode[] = root.fetch("ColorSet");
//		try {
//			final ColorSet ret = new ColorSet();
//			ret.bindConfig(aconfigNode[index]);
//			return ret;
//
//		} catch (final Exception e) {
//			return null;
//		}
//	}
//
//	/**
//	 * returns the color set for this name or null, if name not found in kids
//	 */
//	public ColorSet getColorSet(final String name) {
//
//		final ConfigNode aconfigNode[] = root.fetch("ColorSet");
//		final ColorSet ret = new ColorSet();
//		for (int i = 0; i < aconfigNode.length; i++) {
//
//			ret.bindConfig(aconfigNode[i]);
//			if (name.equals(ret.getName())) {
//				return ret;
//			}
//		}
//		return null;
//	}
//
//	/**
//	 * constructs and adds a <code>ColorSet</code> with the specified
//	 * attributes.
//	 */
//	public void addColorSet(final String name, final String up,
//			final String zero, final String down, final String missing) {
//
//		final ColorSet preset = new ColorSet();
//		preset.bindConfig(root.node("ColorSet"));
//		preset.setName(name);
//		preset.setUp(up);
//		preset.setZero(zero);
//		preset.setDown(down);
//		preset.setMissing(missing);
//	}
//
//	/**
//	 * actually copies state of colorset, does not add the colorset itself but a
//	 * copy.
//	 */
//	public void addColorSet(final ColorSet set) {
//
//		final ColorSet preset = new ColorSet();
//		if (root != null) {
//			preset.bindConfig(root.node("ColorSet"));
//		}
//		preset.copyStateFrom(set);
//	}
//
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
//
//	/**
//	 * Remove color set permanently from presets
//	 * 
//	 * @param i
//	 *            index of color set
//	 */
//	public void removeColorSet(final int i) {
//
//		final Preferences aconfigNode[] = root.fetch("ColorSet");
//		root.remove(aconfigNode[i]);
//	}
//
//	public void reset() {
//
//		root.remove("ColorSet");
//		addDefaultPresets();
//	}
//
// }
