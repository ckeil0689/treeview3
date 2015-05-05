/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: DoubleArrayDrawer.java,v $
 * $Revision: 1.1 $
 * $Date: 2006-08-16 19:13:46 $
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
import java.awt.Rectangle;

import Utilities.Helper;
import edu.stanford.genetics.treeview.DataMatrix;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * Class for Drawing A Colored Grid Representation of a Data Matrix.
 *
 * Each cell in the view corresponds to an element in the array. The color of
 * the pixels is determined by the ColorExtractor, which is passed in the value
 * to be converted.
 * <p>
 *
 * The ArrayDrawer is Observable. It setsChanged() itself when the data array is
 * changed, but you have to call notifyObservers() yourself. Notifications from
 * the ColorExtractor, however, are immediately passed on to listeners.
 * <p>
 *
 * Upon setting a data array, ArrayDrawer will set a reference to the data
 * array, and may refer to it when it asked to draw things. Of course, it may
 * form some kind of internal buffer- you're advised to call setData() if you
 * change the data, and not to change the data unless you call setData() too.
 * <p>
 *
 * The ArrayDrawer can draw on a Graphics object. It requires a source rectangle
 * in units of array indexes, to determine which array values to render, and a
 * destination rectangle to draw them to.
 * <p>
 *
 * At some point, we many want to allow arrays of ints to specify source rows
 * and columns to grab data from for non-contiguous views.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.1 $ $Date: 2006-08-16 19:13:46 $
 *
 */
public class DoubleArrayDrawer extends ArrayDrawer {

	/** Used to convert data values into colors */
	protected ColorExtractor colorExtractor;

	/** The array of data values to be rendered. */
	protected DataMatrix dataMatrix;

	/** Constructor does nothing but set defaults */
	public DoubleArrayDrawer() {

		super();
	}

	/**
	 * Set ColorExtractor for future draws
	 *
	 * @param colorExtractor
	 *            A ColorExtractor to draw required pixels
	 */
	public void setColorExtractor(final ColorExtractor colorExtractor) {

		if (this.colorExtractor != null) {
			this.colorExtractor.deleteObserver(this);
		}

		this.colorExtractor = colorExtractor;
		colorExtractor.addObserver(this);
		setChanged();
	}

	/**
	 * Gets the colorExtractor attribute of the ArrayDrawer object
	 *
	 * @return The colorExtractor value
	 */
	public ColorExtractor getColorExtractor() {

		return colorExtractor;
	}

	/**
	 * Set the source of the data.
	 *
	 * @param matrix
	 *            A DataMatrix of values to be rendered.
	 */
	public void setDataMatrix(final DataMatrix matrix) {

		if (dataMatrix != matrix) {
			dataMatrix = matrix;

			if (colorExtractor != null) {
				colorExtractor.setMin(dataMatrix.getMinVal());
				colorExtractor.setMax(dataMatrix.getMaxVal());
			} else {
				// Log null value for color extractor
			}

			setChanged();
		}
	}

	/** sets contrast to 4 times the mean. Works well in practice. */
	public void recalculateContrast() {

		double mean = 0.0;
		int count = 0;
		final int nRow = dataMatrix.getNumRow();
		final int nCol = dataMatrix.getNumCol();

		for (int row = 0; row < nRow; row++) {

			for (int col = 0; col < nCol; col++) {

				final double val = dataMatrix.getValue(row, col);

				/* don't skew calculations with missing data */
				if (Helper.nearlyEqual(val, DataModel.NODATA)
						|| Helper.nearlyEqual(val, DataModel.EMPTY)) {
					continue;
				}

				mean += Math.abs(val);
				count++;
			}
		}

		mean /= count;

		colorExtractor.setContrast(mean * 4);
		colorExtractor.notifyObservers();
	}

	/**
	 * Paint the array values onto pixels. This method will do averaging if
	 * multiple values map to the same pixel.
	 *
	 * @param pixels
	 *            The pixel buffer to draw to.
	 * @param source
	 *            Specifies Rectangle of values to draw from
	 * @param dest
	 *            Specifies Rectangle of pixels to draw to
	 * @param scanSize
	 *            The scansize for the pixels array (in other words, the width
	 *            of the image)
	 * @param geneOrder
	 *            the order of the genes. The source rect y values are taken to
	 *            mean indexes into this array. If the gene order is null, he
	 *            indexes from the source rect are used as indexes into the data
	 *            matrix.
	 */
	@Override
	public void paint(final int[] pixels, final Rectangle source,
			final Rectangle dest, final int scanSize, final int[] geneOrder) {

		/* Selection dimming code, selections passed from GlobalView */
		// , int[] geneSelections, int[] arraySelections) {

		if (dataMatrix == null) {
			LogBuffer.println("Data matrix wasn't set, "
					+ "can't be used in paint() in DoubleArrayDrawer.");
		}

		/*
		 * Selection Dimming Set the selection ranges for rows and columns. All
		 * values are -1 if no selection was made.
		 */
		// int g_min = geneSelections[0];
		// int g_max = geneSelections[1];
		//
		// int a_min = arraySelections[0];
		// int a_max = arraySelections[1];

		// ynext will hold the first pixel of the next block.
		int ynext = dest.y;

		// geneFirst holds first gene which contributes to this pixel.
		int geneFirst = 0;

		// gene will hold the last gene to contribute to this pixel.
		for (int gene = 0; gene < source.height; gene++) {

			final int ystart = ynext;
			ynext = dest.y + (dest.height + gene * dest.height) / source.height;

			// keep incrementing until block is at least one pixel high
			if (ynext == ystart) {
				continue;
			}

			// xnext will hold the first pixel of the next block.
			int xnext = dest.x;

			// arrayFirst holds first gene which contributes to this pixel.
			int arrayFirst = 0;

			for (int array = 0; array < source.width; array++) {

				final int xstart = xnext;
				xnext = dest.x + (dest.width + array * dest.width)
						/ source.width;

				if (xnext == xstart) {
					continue;
				}

				try {
					double val = 0;
					int count = 0;

					for (int i = geneFirst; i <= gene; i++) {

						for (int j = arrayFirst; j <= array; j++) {

							int actualGene = source.y + i;
							if (geneOrder != null) {
								actualGene = geneOrder[actualGene];
							}

							final double thisVal = dataMatrix.getValue(j
									+ source.x, actualGene);

							if (Helper.nearlyEqual(thisVal, DataModel.EMPTY)) {
								val = DataModel.EMPTY;
								count = 1;
								break;
							}

							if (!Helper.nearlyEqual(thisVal, DataModel.EMPTY)) {
								count++;
								val += thisVal;
							}
						}

						if (Helper.nearlyEqual(val, DataModel.EMPTY)) {
							break;
						}
					}

					if (count == 0) {
						val = DataModel.NODATA;

					} else {
						val /= count;
					}

					/* Darken non-selected rows/ cols if there's a selection */
					// boolean isBackground;
					// int geneInd = gene + source.y;
					// int arrayInd = array + source.x;

					/* Selection Dimming */
					// if(g_min == -1) {
					// isBackground = false;
					// } else {
					// isBackground = !(geneInd >= g_min && geneInd <= g_max)
					// || !(arrayInd >= a_min && arrayInd <= a_max);
					// }

					final int t_color = colorExtractor.getARGBColor(val);
					/* Selection dimming */
					// , isBackground);

					for (int x = xstart; x < xnext; x++) {

						for (int y = ystart; y < ynext; y++) {

							pixels[x + y * scanSize] = t_color;
						}
					}
				} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
					LogBuffer
							.println("ArrayIndexOutOfBoundsException in "
									+ "paint() in DoubleArrayDrawer: "
									+ e.getMessage());
				}
				arrayFirst = array + 1;
			}
			geneFirst = gene + 1;
		}
	}

	/**
	 * Get value for a given array element
	 *
	 * @param x
	 *            x coordinate of array element
	 * @param y
	 *            y coordinate of array element
	 * @return value of array element, or DataModel.NODATA if not found
	 */
	public double getValue(final int x, final int y) {

		if (dataMatrix == null) {
			LogBuffer.println("DataMatrix was not set in DoubleArrayDrawer,"
					+ "can't be used in getValue().");
		}
		return dataMatrix.getValue(x, y);
	}

	@Override
	public String getSummary(final int x, final int y) {

		return "" + getValue(x, y);
	}

	@Override
	public boolean isMissing(final int x, final int y) {

		return Helper.nearlyEqual(getValue(x, y), DataModel.NODATA);
	}

	@Override
	public boolean isEmpty(final int x, final int y) {

		return Helper.nearlyEqual(getValue(x, y), DataModel.EMPTY);
	}

	/** how many rows are there to draw? */
	@Override
	public int getNumRow() {

		if (dataMatrix != null)
			return dataMatrix.getNumRow();
		return 0;
	}

	/** how many cols are there to draw? */
	@Override
	public int getNumCol() {

		if (dataMatrix != null)
			return dataMatrix.getNumCol();
		return 0;
	}

	/**
	 * Get Color for a given array element
	 *
	 * @param x
	 *            x coordinate of array element
	 * @param y
	 *            y coordinate of array element
	 * @return color for array element, or DataModel.NODATA if not found
	 */
	@Override
	public Color getColor(final int x, final int y) {

		return colorExtractor.getColor(getValue(x, y));
	}

	/** resets the ArrayDrawer to a default state. */
	@Override
	protected void setDefaults() {

		dataMatrix = null;
	}
}
