/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package gui.matrix;

import model.data.Helper;
import model.data.matrix.DataMatrix;
import model.data.matrix.DataModel;
import util.LogBuffer;

import java.awt.*;

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
				if (Helper.nearlyEqual(val, DataModel.NAN)
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
	 * This updates the pixel array reference to contain new int values which
	 * represent colors. These are then used by ModelViewProduced.
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
			final Rectangle dest, final int scanSize) {


		if (dataMatrix == null) {
			LogBuffer.println("Data matrix wasn't set, "
					+ "can't be used in paint() in DoubleArrayDrawer.");
			return;
		}
		
		for(int row = 0; row < source.getHeight(); row++) {
			for(int col = 0; col < source.getWidth(); col++) {
				
				double val = dataMatrix.getValue(col, row);
				
				final int t_color = colorExtractor.getARGBColor(val);
				
				pixels[col + row * scanSize] = t_color;
			}
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

		double val = DataModel.NAN;
		if (dataMatrix == null) {
			LogBuffer.println("DataMatrix was not set in DoubleArrayDrawer,"
					+ "can't be used in getValue().");
			return val;
		}
		
		return dataMatrix.getValue(x, y);
	}

	@Override
	public String getSummary(final int x, final int y) {

		return "" + getValue(x, y);
	}

	@Override
	public boolean isMissing(final int x, final int y) {

		return Double.isNaN(getValue(x, y));
	}

	@Override
	public boolean isEmpty(final int x, final int y) {

		return Double.isInfinite(getValue(x, y));
	}

	/** how many rows are there to draw? */
	@Override
	public int getNumRow() {

		if (dataMatrix != null) {
			return dataMatrix.getNumRow();
		}
		
		return 0;
	}

	/** how many cols are there to draw? */
	@Override
	public int getNumCol() {

		if (dataMatrix != null) {
			return dataMatrix.getNumCol();
		}
		
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

		this.dataMatrix = null;
	}
}
