/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

/* Decompiled by Mocha from NullMap.class */
/* Originally compiled from NullMap.java */

public class NullMap extends IntegerMap {
	@Override
	public int getIndex(final int i) {
		return 0;
	}

	@Override
	public int getPixel(final int i) {
		return 0;
	}

	@Override
	public double getScale() {
		return 0.0;
	}

	@Override
	public int getUsedPixels() {
		return 0;
	}

	@Override
	public int getViewableIndexes() {
		return 0;
	}

	@Override
	public String type() {
		return "Null";
	}
}
