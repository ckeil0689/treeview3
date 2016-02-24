/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
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

	public static final int NULL = 0;
	public static final int FIXED = 1;
	public static final int FILL = 2;
	
	protected int availablepixels;
	protected int maxindex;
	protected int minindex;
	protected Preferences configNode;
	protected int type;
	protected String typeName = "NullMap";

	public IntegerMap() {

		this.availablepixels = 0;
		this.maxindex = -1;
		this.minindex = -1;
		setConfigNode(Preferences.userRoot());
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode == null) {
			LogBuffer.println("Could not find or create IntegerMap "
					+ "node because parentNode was null.");
			return;
		}
		
		this.configNode = parentNode.node(typeName);
		requestStoredState();
	}
	
	@Override
	public Preferences getConfigNode() {
		
		return configNode;
	}
	
	@Override
	public void requestStoredState() {
		
		this.type = configNode.getInt("type", IntegerMap.FIXED);
	}
	
	@Override
	public void storeState() {
		
		if(configNode == null) {
			LogBuffer.println("Could not store state of IntegerMap. ConfigNode"
					+ " was null.");
			return;
		}
		configNode.putInt("type", type);
	}

	/**
	 * Sets the String type to either Fixed or Full, so that different
	 * configNode can be created.
	 *
	 * @param type
	 */
	public void setType(final int type) {
		
		switch(type) {
		
		case IntegerMap.NULL:
			this.typeName = "NullMap";
			break;
			
		case IntegerMap.FIXED:
			this.typeName = "FixedMap";
			break;
			
		case IntegerMap.FILL:
			this.typeName = "FillMap";
			break;
			
		default:
			LogBuffer.println("Type cannot be set for IntegerMap: " + type);
			return;
		}
		
		this.type = type;
		storeState();
	}

	/**
	 * Creates an IntegerMap object based on the type int that was supplied.
	 * @param type Indicates the type of IntegerMap to create.
	 * @return A specific type IntegerMap. Null if type cannot be identified.
	 */
	public IntegerMap createMap(final int type) {

		switch(type) {
		
		case IntegerMap.NULL:
			return new NullMap();
			
		case IntegerMap.FIXED:
			return new FixedMap();
			
		case IntegerMap.FILL:
			return new FillMap();
			
		default:
			LogBuffer.println(type + " type not found for IntegerMap.");
			return null;
		}
	}

	/**
	 * @return Number of pixels available for display.
	 */
	public int getAvailablePixels() {

		return availablepixels;
	}

	/**
	 * @param i Pixel for which to find the corresponding tile index.
	 * @return Tile index for the given pixel.
	 */
	public abstract int getIndex(int i);

	/**
	 * @return Maximum index mapped.
	 */
	public int getMaxIndex() {

		return maxindex;
	}

	/**
	 * @return Minimum index mapped.
	 */
	public int getMinIndex() {

		return minindex;
	}

	/**
	 * Checks if a given index is part of this IntegerMap's index range.
	 * @param i An index to check.
	 * @return Whether the indicator is inside the IntegerMap's index 
	 * range.
	 */
	public boolean contains(final int i) {

		if (i < getMinIndex() || i > getMaxIndex()) {
			return false;
		}
		
		return true;
	}

	/**
	 * Note: if i == maxindex + 1, return the first pixel beyond end of max.
	 *
	 * @param i The index for which we want the first pixel of.
	 *
	 * @return First pixel corresponding to index.
	 */
	public abstract int getPixel(final int i);

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

		return map;
	}

	/**
	 * Calculates how many pixels are required for the full range of elements
	 * at the currently set tile scale.
	 * 
	 * @return Number of required pixels for the amount of elements at the 
	 * current scale.
	 */
	public int getRequiredPixels() {

		return (int) ((maxindex - minindex + 1) * getScale());
	}

	/**
	 * @return Average number of pixels per index. Could be meaningless if
	 *         non-constant spacing.
	 */
	public abstract double getScale();

	/**
	 * @return How many of the available pixels are actually used/ filled by
	 * tiles.
	 */
	public abstract int getUsedPixels();

	/**
	 * @return Number of indexes viewable at once.
	 */
	public abstract int getViewableIndexes();

	/**
	 * @param i Number of pixels which we can map to. 
	 * The map will map the index range to pixels 1 to (n - 1).
	 */
	public void setAvailablePixels(final int i) {

		this.availablepixels = i;
	}

	/**
	 * Set the range of pixels to map to.
	 *
	 * @param i lower bound (minimum index)
	 * @param j upper bound (maximum index)
	 */
	public void setIndexRange(final int i, final int j) {

		this.minindex = i;
		this.maxindex = j;
	}
	
	/**
	 * Check if the type of this IntegerMap is the same as the supplied type.
	 * @param otherType The type to be checked for equality.
	 * @return Whether the IntegerMap has the type of the given parameter.
	 */
	public boolean equalsType(final int otherType) {
		
		return type == otherType;
	}

	public abstract String typeName();
	public abstract int type();
}
