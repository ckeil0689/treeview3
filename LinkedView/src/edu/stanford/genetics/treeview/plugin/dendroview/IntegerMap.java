/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: IntegerMap.java,v $
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

import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * This class is a contract for maps between indexes and pixels. It would be an
 * interface, except there are some common routines which are worth implementing
 * in the superclass.
 */
public abstract class IntegerMap implements ConfigNodePersistent {

	protected int availablepixels;
	protected int maxindex;
	protected int minindex;
	protected Preferences configNode;
	protected String type;

	public IntegerMap() {

		availablepixels = 0;
		maxindex = -1;
		minindex = -1;
		configNode = Preferences.userRoot().node(type());
	}
	
	@Override
	public void setConfigNode(Preferences parentNode) {

		if(parentNode != null) {
			this.configNode = parentNode.node(type);
			
		} else {
			LogBuffer.println("Could not find or create IntegerMap " +
					"node because parentNode was null.");
		}
		
		configNode.put("type", type());
	}
	
	/**
	 * Sets the String type to either Fixed or Full, so that 
	 * different configNode can be created.
	 * @param type
	 */
	public void setTypeString(String type) {
		
		this.type = type;
	}

	public IntegerMap createMap(final String string) {

		if (string.equals("Fixed")) {
			return new FixedMap();

		} else if (string.equals("Fill")) {
			return new FillMap();

		} else {
			System.out.println(string + " not found");
			return null;
		}
	}

	/**
	 * @return number of pixels available for display
	 */
	public int getAvailablePixels() {

		return availablepixels;
	}

	/**
	 * @param i
	 *            pixel for which to find index
	 * @return index into array for that pixel
	 */
	public abstract int getIndex(int i);

	/**
	 * @return maximum index mapped
	 */
	public int getMaxIndex() {

		return maxindex;
	}

	// simple accessors
	/**
	 * @return minimum index mapped
	 */
	public int getMinIndex() {

		return minindex;
	}

	public boolean contains(final int i) {

		if (i < getMinIndex()) {
			return false;

		} else if (i > getMaxIndex()) {
			return false;

		} else {
			return true;
		}
	}

	// subclasses implement actual mapping functions
	/**
	 * note: if i == maxindex + 1, return the first pixel beyond end of max
	 * 
	 * @param i
	 *            the index for which we want the first pixel of
	 * 
	 * @return first pixel corresponding to index
	 */
	public abstract int getPixel(int i);

	/**
	 * @param indval
	 *            the (fractional) index for which we want the pixel.
	 * 
	 *            This is determined by assuming that the actual index
	 *            corresponds to the middle of the block of pixels assigned to
	 *            that index, and then linearly interpolating the unit interval
	 *            onto the block.
	 * 
	 *            This means that 6.0 would map to the middle of the block, and
	 *            6.5 would map to the boundary of the 6 and 7 blocks. Values
	 *            between 6.0 and 6.5 would be linearly interpolated between
	 *            those points.
	 * 
	 *            The relation getPixel(i) == getPixel (i -0.5) should hold.
	 */
	public int getPixel(final double indval) {

		final double base = Math.rint(indval);
		// indicates how far into the block to go, from 0.0 - 1.0
		final double residual = indval - base + .5;
		final int ibase = (int) base;
		final int map = (int) (getPixel(ibase) * (1.0 - residual) + residual
				* getPixel(ibase + 1));
		// System.out.println("scale " + getScale() + "got base " + base +
		// " residual " + residual + " maps to " + map);

		return map;
	}

	public int getRequiredPixels() {

		return (int) ((maxindex - minindex + 1) * getScale());
	}

	// how many pixels per integer, on average?
	/**
	 * @return average number of pixels per index. Could be meaningless if
	 *         non-constant spacing.
	 */
	public abstract double getScale();

	/**
	 * @return how many of the avaiable pixels are actually used...
	 */
	public abstract int getUsedPixels();

	/**
	 * @return number of indexes viewable at once
	 */
	public abstract int getViewableIndexes();

	/**
	 * @param i
	 *            number of pixels which we can map to. The map will map the
	 *            index range to pixels 1 to n-1.
	 */
	public void setAvailablePixels(final int i) {

		availablepixels = i;
	}

	/**
	 * Set the range of pixels to map to
	 * 
	 * @param i
	 *            lower bound
	 * @param j
	 *            upper bound
	 */
	public void setIndexRange(final int i, final int j) {

		minindex = i;
		maxindex = j;
	}

	public abstract String type();
}
