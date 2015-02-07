/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: FixedMap.java,v $
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

import java.util.prefs.Preferences;

/**
 * Maps integers (gene index) to pixels using a fixed scale
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.1 $ $Date: 2006-08-16 19:13:45 $
 */
public class FixedMap extends IntegerMap {

	private double default_scale;
	private double scale;

	/**
	 * constructs new FixedMap
	 */
	public FixedMap() {

		default_scale = 10.0;
	}

	// /**
	// * For persistance of scale, bind to a ConfigNode
	// *
	// * @param configNode
	// * ConfigNode to bind to
	// */
	// @Override
	// public void bindConfig(final Preferences configNode) {
	//
	// super.bindConfig(configNode);
	// scale = root.getDouble("scale", default_scale);
	// }

	/**
	 * For persistance of scale, bind to a ConfigNode
	 *
	 * @param configNode
	 *            ConfigNode to bind to
	 */
	@Override
	public void setConfigNode(final Preferences parentNode) {

		super.setConfigNode(parentNode);
		scale = configNode.getDouble("scale", default_scale);
	}

	/**
	 * Gets the index for a particular pixel.
	 *
	 * @param i
	 *            the pixel value
	 * @return The index value
	 */
	@Override
	public int getIndex(final int i) {

		// Rob 10/14/2014
		// Explanation: When scale has a trailing long decimal value that is cut
		// off, sometimes, the value being floored is just under the integer
		// value that it *SHOULD* be,
		// causing drawing problems, because this function is called with the
		// last of the visible pixels to decide how many data columns to draw.
		// It ends up drawing one too few.
		// To resolve this, I selected a "precision" value. If the fraction of
		// the calculated value over the rounded value is greater than 0.999999,
		// then return the rounded value,
		// otherwise, return the floored value. Here's what was happening in
		// some cases when only one of these methods was universally applied:
		// If this is Math.round, the visible grid is fine, but the highlight is
		// wrong.
		// If this is Math.floor, the visible grid is wrong, and the highlight
		// is correct.
		// LogBuffer.println("Min index = [" + minindex + "].  Pixel index = ["
		// + i + "].  Scale = [" + scale + "].  Data index = [floor(" + ((i /
		// scale) + minindex) + ") = " + ((int) Math.floor((i / scale) +
		// minindex)) + "].");

		if ((Math.round(i / scale) + minindex) > 0
				&& ((i / scale) + minindex)
						/ (Math.round(i / scale) + minindex) > (1 - 0.000001))
			// LogBuffer.println("Returning round [" + ((int) Math.round(i /
			// scale) + minindex) + "]");
			return (int) Math.round(i / scale) + minindex;
		// LogBuffer.println("Returning floor [" + ((int) Math.floor(i / scale)
		// + minindex) + "]");
		return (int) Math.floor(i / scale) + minindex;
	}

	/**
	 * Gets the pixel for a particular index
	 *
	 * @param i
	 *            The index value
	 * @return The pixel value
	 */
	@Override
	public int getPixel(final int i) {

		// Rob 10/14/2014 - Added Math.floor to be explicit
		return (int) Math.floor((i - minindex) * scale);
	}

	/**
	 * @return The effective scale for the current FillMap
	 */
	@Override
	public double getScale() {

		return scale;
	}

	/**
	 * @return The number of pixels currently being used
	 */
	@Override
	public int getUsedPixels() {

		if (minindex == -1)
			return 0;

		final int i = (int) Math.round((maxindex - minindex + 1) * scale);
		// Rob 10/14/2014 - Changed the redundant calculation into a call to
		// getViewableIndexes
		final int j = (int) Math.round((scale * getViewableIndexes()));
		if (i > j)
			return j;
		else
			return i;
	}

	/**
	 * @return The number of indexes currently visible
	 */
	@Override
	public int getViewableIndexes() {

		// Rob 10/14/2014 - Added Math.round because due to precision issues,
		// sometimes the value ended in .99999999...
		final int i = (int) Math.round(availablepixels / scale);
		return i;
	}

	/**
	 * Sets the defaultScale attribute of the FixedMap object
	 *
	 * @param d
	 *            The new defaultScale value
	 */
	public void setDefaultScale(final double d) {

		default_scale = d;
	}

	/**
	 * set scaling value
	 *
	 * @param d
	 *            The new scale value
	 */
	public void setScale(final double d) {

		scale = d;
		configNode.putDouble("scale", scale);
	}

	/**
	 * @return A short word desribing this type of map
	 */
	@Override
	public String type() {

		return "Fixed";
	}
}
