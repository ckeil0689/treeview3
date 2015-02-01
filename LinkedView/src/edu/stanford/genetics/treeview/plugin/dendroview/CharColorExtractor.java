/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: CharColorExtractor.java,v $
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
import java.util.Observable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * The purpose of this class is to convert a character into a color.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.1 $ $Date: 2006-08-16 19:13:45 $
 */

public class CharColorExtractor extends Observable implements
		ConfigNodePersistent {

	private static CharColorSet defaultColorSet = new CharColorSet();
	private final CharColorSet colorSet;// Will be backed by confignode when we
	// get one...
	private Preferences configNode;

	/** Constructor for the CharColorExtractor object */
	public CharColorExtractor() {

		// set a default defaultColorSet... should be superceded by a user
		// setting...
		colorSet = new CharColorSet();
		colorSet.copyStateFrom(defaultColorSet);
	}

	/**
	 * Sets the default colors to be used if a config node if a config node is
	 * not bound to us. Also used in setDefaults() to figure out what the
	 * default colors are.
	 */
	public void setDefaultColorSet(final CharColorSet set) {

		defaultColorSet = set;
	}

	// /**
	// * binds this CharColorExtractor to a particular ConfigNode. This makes
	// * colors persistent
	// *
	// * @param configNode
	// * confignode to bind to
	// */
	// @Override
	// public void bindConfig(final Preferences configNode) {
	//
	// root = configNode;
	// // Preferences cand = root.fetchFirst("ColorSet");
	// try {
	// String[] childrenNodes = root.childrenNames();
	// boolean nodePresent = false;
	//
	// for(int i = 0; i < childrenNodes.length; i++) {
	//
	// if(childrenNodes[i].equalsIgnoreCase("ColorSet")) {
	// nodePresent = true;
	// }
	// }
	//
	// Preferences cand = null;
	// if (!nodePresent) {
	// cand = root.node("CharColorSet");
	// }
	// colorSet.bindConfig(cand);
	//
	// } catch (BackingStoreException e) {
	// e.printStackTrace();
	// LogBuffer.println("Error in CharColorExtractor/bindConfig(): "
	// + e.getMessage());
	// }
	// }

	/**
	 * binds this CharColorExtractor to a particular ConfigNode. This makes
	 * colors persistent
	 *
	 * @param configNode
	 *            confignode to bind to
	 */
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node("CharColorExtractor");

		} else {
			LogBuffer.println("Could not find or create CharColorExtractor"
					+ "node because parentNode was null.");
		}

		try {
			final String[] childrenNodes = configNode.childrenNames();
			for (final String childrenNode : childrenNodes) {

				if (childrenNode.contains("ColorSet")) {
				}
			}

			// Preferences cand = null;
			// if (!nodePresent) {
			// cand = root.node("CharColorSet");
			// }
			colorSet.setConfigNode(configNode);

		} catch (final BackingStoreException e) {
			e.printStackTrace();
			LogBuffer.println("Error in CharColorExtractor/bindConfig(): "
					+ e.getMessage());
		}
	}

	/**
	 * The color for missing data.
	 */
	public Color getMissing() {

		return colorSet.getMissing();
	}

	/**
	 * The empty is a color to be used for cells which do not correspond to
	 * data, like in the KnnView. These cells are just used for spacing.
	 */
	public Color getEmpty() {

		return colorSet.getEmpty();
	}

	/**
	 * The color for chars.
	 */
	public void setColor(final char c, final String newString) {

		if (ColorSet.encodeColor(colorSet.getColor(c)).equals(newString))
			return;
		colorSet.setColor(c, ColorSet.decodeColor(newString));
		setChanged();
	}

	/**
	 * The color for missing data.
	 */
	public void setMissingColor(final String newString) {

		if (ColorSet.encodeColor(colorSet.getMissing()).equals(newString))
			return;
		colorSet.setMissing(ColorSet.decodeColor(newString));
		setChanged();
	}

	/**
	 * The empty is a color to be used for cells which do not correspond to data
	 */
	public void setEmptyColor(final String newString) {

		if (newString == null)
			return;

		if (ColorSet.encodeColor(colorSet.getEmpty()).equals(newString))
			return;

		colorSet.setEmpty(ColorSet.decodeColor(newString));
		setChanged();
	}

	/**
	 * The color for chars.
	 */
	public void setColor(final char c, final Color newColor) {
		if (colorSet.getColor(c).equals(newColor))
			return;
		colorSet.setColor(c, newColor);
		setChanged();
	}

	/**
	 * The color for missing data.
	 */
	public void setMissingColor(final Color newColor) {
		if (colorSet.getMissing().equals(newColor))
			return;
		colorSet.setMissing(newColor);
		setChanged();
	}

	/**
	 * Set emptyColor value for future draws The empty is a color to be used for
	 * cells which do not correspond to data
	 */
	public void setEmptyColor(final Color newColor) {

		if (newColor == null)
			return;
		if (colorSet.getEmpty().equals(newColor))
			return;
		colorSet.setEmpty(newColor);
		setChanged();
	}

	/**
	 * Gets the color corresponding to a particular char.
	 *
	 * @param c
	 *            char representing value we want color for
	 * @return The color value
	 */
	public Color getColor(final char c) {

		return colorSet.getColor(c);
	}

	/**
	 * Gets the floatColor attribute of the ColorExtractor object
	 *
	 * @param c
	 *            char representing value we want color for
	 * @return The floatColor value
	 */
	public float[] getFloatColor(final char c) {

		return getColor(c).getComponents(null);
	}

	/** prints out a description of the state to standard out */
	public void printSelf() {

		System.out.println("missingColor " + getMissing());
		System.out.println("emptyColor " + getEmpty());
	}

	/**
	 * Gets the aRGBColor attribute of the ColorExtractor object
	 *
	 * @param c
	 *            Description of the Parameter
	 * @return The aRGBColor value
	 */
	public int getARGBColor(final char c) {

		return getColor(c).getRGB();
	}

	/** resets the ColorExtractor to a default state. */
	public void setDefaults() {

		setMissingColor(ColorSet.encodeColor(defaultColorSet.getMissing()));
		setEmptyColor(ColorSet.encodeColor(defaultColorSet.getEmpty()));
		setChanged();
	}
}
