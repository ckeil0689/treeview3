/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package gui.matrix;

import model.data.trees.TreeSelectionI;
import model.export.ExportHandler.ExportWorker;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
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
			int scanSize);

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
			final Rectangle dest) {

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
	 * A method to model.export a portion of the matrix
	 * @author rleach
	 * @param e - Swingworker class which is used to run the model.export
	 * @param g - A graphics2D-compatible object
	 * @param xend - The index of the last column of data to be included
	 * @param yend - The index of the last row of data to be included
	 * @param xIndent - The x start position of the image
	 * @param yIndent - The y start position of the image
	 * @param size - The number of "pixels" high/wide each tile is to be drawn
	 * @param xstart - The index of the first column of data to be included
	 * @param ystart - The index of the first row of data to be included
	 */
	public void paint(ExportWorker e, final Graphics g,
		final int xDataStart,final int yDataStart,
		final int xDataEnd,final int yDataEnd,
		final int xImageStart,final int yImageStart,
		final int xTileSize,final int yTileSize,
		final boolean showSelections,
		final TreeSelectionI colSelection,final TreeSelectionI rowSelection) {
	
		int k=0;
		for (int j = yDataStart; j <= yDataEnd; j++) {
			for (int i = xDataStart; i <= xDataEnd; i++) {
				//setPaintMode seems to help color overlapping (affecting the
				//colors) a little, but doesn't fix it altogether. Probably
				//useless. Note there appears to be an alpha channel in the
				//output PDF.
				g.setPaintMode();
				g.setColor(getColor(i,j));
				//drawRect is better than fillRect because there're no gaps, but
				//still some color bleed for some reason at some zoom levels - I
				//think that's due to the reader's poor rendering
				g.drawRect(xImageStart + (i - xDataStart) * xTileSize,
					yImageStart + (j - yDataStart) * yTileSize,xTileSize,yTileSize);
				g.fillRect(xImageStart + (i - xDataStart) * xTileSize,
					yImageStart + (j - yDataStart) * yTileSize,xTileSize,yTileSize);
				setChanged();
				notifyObservers(++k);
				if(e.isCancelled()){
		    		e.setExportSuccessful(false);
					return;
		    	}
			}
		}

		//Draw the selection rectangles
		if(showSelections && (rowSelection != null) && (colSelection != null) &&
			rowSelection.getNSelectedIndexes() > 0 &&
			colSelection.getNSelectedIndexes() > 0) {

			g.setColor(Color.yellow);

			final int[] selectedArrayIndexes =
				colSelection.getSelectedIndexes();
			final int[] selectedGeneIndexes =
				rowSelection.getSelectedIndexes();
	
			if (selectedArrayIndexes.length > 0) {
				List<List<Integer>> arrayBoundaryList;
				List<List<Integer>> geneBoundaryList;
	
				arrayBoundaryList =
					findRectBoundaries(selectedArrayIndexes);
				geneBoundaryList =
					findRectBoundaries(selectedGeneIndexes);
	
				for(final List<Integer> xBoundaries : arrayBoundaryList) {
					for(final List<Integer> yBoundaries : geneBoundaryList){

						int xSelectionPixel = xImageStart +
							(xBoundaries.get(0) - xDataStart) * xTileSize;
						int xPixelSize =
							(xBoundaries.get(1) - xBoundaries.get(0) + 1) *
							xTileSize - 1;
						int yStartPixel = yImageStart +
							(yBoundaries.get(0) - yDataStart) * yTileSize;
						int yPixelSize =
							(yBoundaries.get(1) - yBoundaries.get(0) + 1) *
							yTileSize - 1;
						g.drawRect(xSelectionPixel,yStartPixel,xPixelSize,
							yPixelSize);
					}
				}
			}
		}
	}

	protected List<List<Integer>> findRectBoundaries(
		final int[] selectedIndexes) {

		final List<List<Integer>> boundaryList = new ArrayList<List<Integer>>();
	
		/*
		 * If array is bigger than 1, check how many consecutive labels are
		 * selected by finding out which elements are only 1 apart. Store
		 * consecutive indexes separately to make separate rectangles later.
		 */
		int firstindex = 0;
		int lastval;
		while(firstindex < selectedIndexes.length) {

			lastval = selectedIndexes[firstindex];
			List<Integer> boundaries = new ArrayList<Integer>();
			boundaries.add(lastval);

			firstindex++;

			for(int i = firstindex; i < selectedIndexes.length; i++) {
	
				final int currentval = selectedIndexes[i];

				if(lastval == currentval - 1) {
					lastval = currentval;
					firstindex = i + 1;
				} else {
					break;
				}
			}

			boundaries.add(lastval);
			boundaryList.add(boundaries);
		}

		return(boundaryList);
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
