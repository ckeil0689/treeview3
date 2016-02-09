/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

/**
 * Class for Drawing A Colored Grid Representation of a Matrix.
 *
 * Each cell in the view corresponds to an element in an array. The color of the
 * pixels is determined by subclasses.
 * <p>
 *
 * The ArrayDrawer is Observable. It setsChanged() itself when the data array is
 * changed, but you have to call notifyObservers() yourself.
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
public abstract class ArrayDrawer extends Observable implements Observer {

	private Preferences root;

	/**
	 * Get Color for a given array element
	 *
	 * @param x
	 *            x coordinate of array element
	 * @param y
	 *            y coordinate of array element
	 * @return color for array element, or nodata if not found
	 */
	public abstract Color getColor(int x, int y);

	/** resets the ArrayDrawer to a default state. */
	protected abstract void setDefaults();

	/** is the element missing? */
	public abstract boolean isMissing(int x, int y);

	/** is the element empty? */
	public abstract boolean isEmpty(int x, int y);

	/** String representing value of element */
	public abstract String getSummary(int x, int y);

	/** how many rows are there to draw? */
	public abstract int getNumRow();

	/** how many cols are there to draw? */
	public abstract int getNumCol();

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
	 *            mean indexes into this array. If the gene order is null, the
	 *            indexes from the source rect are used as indexes into the data
	 *            matrix.
	 */
	public abstract void paint(int[] pixels, Rectangle source, Rectangle dest,
			int scanSize, int[] geneOrder);

	/* Code for selection dimming */
	// , int[] geneSelections, int[] arraySelections);

	/** Constructor does nothing but set defaults */
	public ArrayDrawer() {

		setDefaults();
	}

	// /**
	// * binds this arraydrawer to a particular ConfigNode.
	// *
	// * @param configNode
	// * confignode to bind to
	// */
	// public void bindConfig(final Preferences configNode) {
	//
	// root = configNode;
	// }

	/**
	 * binds this arraydrawer to a particular ConfigNode.
	 *
	 * @param configNode
	 *            confignode to bind to
	 */
	public void setConfigNode(final String key) {

		if (key == null) {
			this.root = Preferences.userRoot().node(this.getClass().getName());

		} else {
			this.root = Preferences.userRoot().node(key);
		}
	}

	/**
	 * Paint the view of the Pixels
	 *
	 * @param g
	 *            The Graphics element to draw on
	 * @param source
	 *            Specifies Rectangle of values to draw from
	 * @param dest
	 *            Specifies Rectangle of pixels to draw to
	 * @param geneOrder
	 *            a desired reordered subset of the genes, or null if you want
	 *            order from cdt.
	 */
	public void paint(final Graphics g, final Rectangle source,
			final Rectangle dest, final int[] geneOrder) {

		int ynext = dest.y;
		for (int j = 0; j < source.height; j++) {
			final int ystart = ynext;
			ynext = dest.y + (dest.height + j * dest.height) / source.height;

			int xnext = dest.x;
			for (int i = 0; i < source.width; i++) {
				final int xstart = xnext;
				xnext = dest.x + (dest.width + i * dest.width) / source.width;
				final int width = xnext - xstart;
				final int height = ynext - ystart;
				if ((width > 0) && (height > 0)) {
					try {
						int actualGene = source.y + j;
						if (geneOrder != null) {
							actualGene = geneOrder[actualGene];
						}
						final Color t_color = getColor(i + source.x, actualGene);
						g.setColor(t_color);
						g.fillRect(xstart, ystart, width, height);

					} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
						// System.out.println("out of bounds, " + (i + source.x)
						// + ", " + (j + source.y));
					}
				}
			}
		}
	}

	/**
	 * This method is intended to be used for drawing the matrix using vector
	 * graphics (freehep's VectorGraphics2D object), thus there are no resizing
	 * calculations.
	 * @author rleach
	 * @param g - Graphics or Graphics2D or VectorGraphics2D object
	 * @param width - This should be the size of xmap
	 * @param height - This should be the size of ymap
	 */
	/* TODO: This needs to take a start end end index for each dimension fining
	 * the export region */
	public void paint(final Graphics g,final int width,final int height) {
		for (int j = height - 1; j >= 0; j--) {
			for (int i = width - 1; i >= 0; i--) {
				//setPaintMode seems to help color overlapping (affecting the
				//colors) a little, but doesn't fix it altogether. Probably
				//useless. Note there appears to be an alpha channel in the
				//output PDF.
				g.setPaintMode();
				g.setColor(getColor(i,j));
				//drawRect is better than fillRect because there're no gaps, but
				//still some color bleed for some reason at some zoom levels - I
				//think that's due to the reader's poor rendering
				g.drawRect(i,j,1,1);
			}
		}
	}

	/* TODO: This needs to take a start end end index for each dimension fining
	 * the export region */
	public void paint(final Graphics g,final int width,final int height,
		final int xIndent,final int yIndent,final int size) {

//		/* TODO: Remove this temporary code that is used only to ensure the tree
//		 * is aligned in the correct region */
//		//Temporary code to force the regions for the trees to be included
//		g.setPaintMode();
//		g.setColor(Color.GRAY);
//		g.fillRect(0,yIndent,xIndent,height * size);
//		g.setPaintMode();
//		g.setColor(Color.GRAY);
//		g.fillRect(xIndent,0,width * size,yIndent);
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				//setPaintMode seems to help color overlapping (affecting the
				//colors) a little, but doesn't fix it altogether. Probably
				//useless. Note there appears to be an alpha channel in the
				//output PDF.
				g.setPaintMode();
				g.setColor(getColor(i,j));
				//drawRect is better than fillRect because there're no gaps, but
				//still some color bleed for some reason at some zoom levels - I
				//think that's due to the reader's poor rendering
				g.drawRect(xIndent + i*size,yIndent + j*size,size,size);
				g.fillRect(xIndent + i*size,yIndent + j*size,size,size);
			}
		}
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
	 */
	public void paint(final int[] pixels, final Rectangle source,
			final Rectangle dest, final int scanSize) {

		paint(pixels, source, dest, scanSize, null);
	}

	/**
	 * Method to draw a single point (x,y) on grapics g using xmap and ymap
	 *
	 * @param g
	 *            Graphics to draw to
	 * @param xmap
	 *            Mapping from indexes to pixels
	 * @param ymap
	 *            Mapping from indexes to pixels
	 * @param x
	 *            x coordinate of data in array
	 * @param y
	 *            y coordinate of data in array
	 * @param geneOrder
	 *            a desired reordered subset of the genes, or null if you want
	 *            order from cdt.
	 */
	public void paintPixel(final Graphics g, final MapContainer xmap,
			final MapContainer ymap, final int x, final int y,
			final int[] geneOrder) {

		try {
			int actualGene = ymap.getIndex(y);
			if (geneOrder != null) {
				actualGene = geneOrder[actualGene];
			}

			final Color t_color = getColor(xmap.getIndex(x), actualGene);
			g.setColor(t_color);
			g.fillRect(x, y, 1, 1);

		} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
		}
	}

	/**
	 * This drawer can only draw from a single, unchanging model. This method
	 * may not be necessary. Neither may the observer/observable stuff.
	 *
	 * @param o
	 *            Object sending update
	 * @param arg
	 *            Argument, usually null
	 */
	@Override
	public void update(final Observable o, final Object arg) {

		setChanged();
		notifyObservers();
	}

	public Preferences getRoot() {

		return root;
	}

	public void setRoot(final Preferences root) {

		this.root = root;
	}
}
