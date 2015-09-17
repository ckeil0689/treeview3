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

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * Maps integers (gene index) to pixels using a fixed scale
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.1 $ $Date: 2006-08-16 19:13:45 $
 */
public class FixedMap extends IntegerMap {

	private double default_scale;
	private double scale; //Pixels per data index
	private int debug = 0;

	/**
	 * constructs new FixedMap
	 */
	public FixedMap() {

		default_scale = 10.0;
		debug = 0;
		//1 = Debug getIndex to see if it's returning the correct value
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
	 * Gets the index for a particular pixel. If there are multiple data indexes
	 * per pixel, arbitrarily returns the one from the subset that corresponds
	 * to their overall matrix position. This is so that you can obtain the last
	 * data index via this method - which wasn't possible when there were
	 * multiple rows of data represented by a single row of pixels in dense/
	 * large data.
	 *
	 * @param i
	 *            the pixel value
	 * @return The index value
	 */
	@Override
	public int getIndex(final int i) {
		if(scale >= 1.0) {
			debug("Returning usual getIndex response because scale > 1.",1);
			return(getIndexHelper(i));
		} else {
			//When there is more than 1 data row or col per pixel, we will
			//arbitrarily return the index among the numerous indexes
			//represented by that pixel which is in the same relative position
			//as the pixel is (relative to the entire offscreen map). To get the
			//data index among the multiple represented by a single pixel which
			//is relative to what is visible/zoomed, you will have to roll your
			//own.

			//Assuming that you get the first data index represented by a pixel
			//from getIndexHelper (the previous version of this method), we will
			//grab the data index represented by the next pixel and subtract 1
			//to find the last data index represented by the current pixel.
			int firstindex = getIndexHelper(i);
			int lastindex  = getIndexHelper(i+1) - 1;
			if(firstindex == lastindex) {
				debug("Returning usual getIndex response because there " +
					"appears to be only 1 index mapped to this pixel.",1);
				return(firstindex);
			} else {
				//Number of data indexes under the pixel
				int numindexes = lastindex - firstindex + 1;
				//The middle index will be used to get relative matrix position
				int middleindex =
					firstindex + (int) Math.round(numindexes/2) - 1;
				//Total number of matrix indexes
				int totalindexes = maxindex - minindex + 1;
				//Relative position of the middle index
				double ratioposition =
					(double) middleindex / (double) totalindexes;
				//Select the index under the pixel that corresponds to their
				//overall matrix position
				int selectindex = firstindex +
					(int) Math.round(numindexes * ratioposition);
				//Just in case...
				if(selectindex > lastindex) {
					selectindex = lastindex;
				} else if(selectindex < firstindex) {
					selectindex = firstindex;
				}
				debug("Given the data index range of [" + firstindex +
					"] to [" + lastindex + "], including [" + numindexes +
					"] indexes returning index [" + selectindex +
					"] because of ratioposition [" + ratioposition +
					"]. Pixel index sent in: [" + i + "] out of [" +
					getAvailablePixels() + "] available pixels.",1);
				return(selectindex);
			}
		}
	}

	/**
	 * Gets the index for a particular pixel.
	 *
	 * @param i
	 *            the pixel value
	 * @return The index value
	 */
	public int getIndexHelper(final int i) {

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

		if ((Math.round(i / scale) + minindex) > 0 &&
			((i / scale) + minindex) /
				(Math.round(i / scale) + minindex) > (1 - 0.000001))
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

	public void debug(String msg,int level) {
		if(level == debug) {
			LogBuffer.println(msg);
		}
	}
}
