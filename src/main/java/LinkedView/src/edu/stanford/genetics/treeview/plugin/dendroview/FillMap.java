/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

/**
 * maps integers (gene index) to pixels, filling available pixels
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.1 $ $Date: 2006-08-16 19:13:46 $
 */
public class FillMap extends IntegerMap {

	/**
	 * Gets the index for a particular pixel.
	 *
	 * @param i
	 *            the pixel value
	 * @return The index value
	 */
	@Override
	public int getIndex(final int i) {

		if (availablepixels == 0)
			return 0;

		// Rob 10/14/2014
		// Added Math.floor before casting as int instead of having that happen
		// implicitly
		return (int) Math.floor(i * (maxindex - minindex + 1) / availablepixels
				+ minindex);
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

		return (i - minindex) * availablepixels / (maxindex - minindex + 1);
	}

	/**
	 * @return The effective scale for the current FillMap
	 */
	@Override
	public double getScale() {

		return (double) availablepixels / (maxindex - minindex + 1);
	}

	/**
	 * @return The number of pixels currently being used
	 */
	@Override
	public int getUsedPixels() {

		if (minindex == -1) {
			return 0;
		}
		
		return availablepixels;
	}

	/**
	 * @return The number of indexes currently visible
	 */
	@Override
	public int getViewableIndexes() {

		return maxindex - minindex + 1;
	}

	/**
	 * @return A short word desribing this type of map
	 */
	@Override
	public String typeName() {

		return "FillMap";
	}

	@Override
	public int type() {
		
		return IntegerMap.FILL;
	}
}
