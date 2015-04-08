/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: GlobalView.java,v $
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;

import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeSelectionI;

public class InteractiveMatrixView extends MatrixView implements
MouseWheelListener {

	private static final long serialVersionUID = 1L;

	public static final int FILL = 0;
	public static final int EQUAL = 1;
	public static final int PROPORT = 2;

	private final String[] statustext = new String[] { "Mouseover Selection",
			"", "" };
	private HeaderInfo arrayHI;
	private HeaderInfo geneHI;

	private HeaderSummary geneSummary;
	private HeaderSummary arraySummary;

	private int overx;
	private int overy;
//<<<<<<< HEAD:LinkedView/src/edu/stanford/genetics/treeview/plugin/dendroview/GlobalView.java
//	private final JScrollPane scrollPane;
	
	private double aspectRatio = -1;
//=======
//>>>>>>> global_overview:LinkedView/src/edu/stanford/genetics/treeview/plugin/dendroview/InteractiveMatrixView.java

	/**
	 * Points to track candidate selected rows/cols should reflect where the
	 * mouse has actually been
	 */
	private final Point startPoint = new Point();
	private final Point endPoint = new Point();

	/**
	 * This rectangle keeps track of where the drag rect was drawn
	 */
	private final Rectangle dragRect = new Rectangle();

	/**
	 * Rectangle to track yellow selected rectangle (pixels)
	 */
	private List<Rectangle> selectionRectList = new ArrayList<Rectangle>();

	/**
	 * Circle to be used as indicator for selection
	 */
	private Ellipse2D.Double indicatorCircle = null;

	/**
	 * GlobalView also likes to have an globalxmap and globalymap (both of type
	 * MapContainer) to help it figure out where to draw things. It also tries
	 * to
	 */
	public InteractiveMatrixView() {

		super();

		/* Listeners for interactivity */
		addMouseListener(new MatrixMouseListener());
		addMouseMotionListener(new MatrixMouseListener());
		addMouseWheelListener(this);
	}

	/**
	 * 
	 * @return x-axis scroll bar (horizontal) for the InteractiveMatrixView.
	 */
	public JScrollBar getXScroll() {

		return scrollPane.getHorizontalScrollBar();
	}

	/**
	 * 
	 * @return y-axis scroll bar (vertical) for the InteractiveMatrixView.
	 */
	public JScrollBar getYScroll() {

		return scrollPane.getVerticalScrollBar();
	}

	@Override
	public String[] getStatus() {

		try {
			if (xmap.contains(overx) && ymap.contains(overy)) {
				statustext[0] = "";// "Row: ";

				if (geneHI != null) {
					final int realGene = overy;
					try {
						statustext[0] += geneSummary.getSummary(geneHI,
								realGene);

					} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
						LogBuffer.println("ArrayIndexOutOfBoundsException "
								+ "in getStatus() in GlobalView: "
								+ e.getMessage());
						statustext[0] += " (N/A)";
					}
				}
				statustext[1] = "";// "Column: ";
				if (arrayHI != null) {
					try {
						statustext[1] += arraySummary
								.getSummary(arrayHI, overx);

					} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
						LogBuffer.println("ArrayIndexOutOfBoundsException "
								+ "in getStatus() in GlobalView: "
								+ e.getMessage());
						statustext[1] += " (N/A)";
					}
				}

				if (drawer != null) {
					if (drawer.isMissing(overx, overy)) {
						statustext[2] = "No Data";

					} else if (drawer.isEmpty(overx, overy)) {
						statustext[2] = "";

					} else {
						statustext[2] = drawer.getSummary(overx, overy);
					}
				}
			}
		} catch (final ArrayIndexOutOfBoundsException e) {
			LogBuffer.println("ArrayIndexOutOfBoundsException "
					+ "in getStatus() in GlobalView: " + e.getMessage());
		}
		return statustext;
	}

	public void setHeaderSummary(final HeaderSummary gene,
			final HeaderSummary array) {

		this.geneSummary = gene;
		this.arraySummary = array;
	}

	/**
	 * Get ArrayDrawer
	 *
	 * @return The current ArrayDrawer
	 */
	public ArrayDrawer getArrayDrawer() {

		return drawer;
	}

	@Override
	public String viewName() {

		return "InteractiveMatrixView";
	}

	// Canvas Methods
	/**
	 * This method updates the graphics object directly by asking the
	 * ArrayDrawer to draw on it directly. The alternative is to have a pixel
	 * buffer which you update using updatePixels.
	 */
	@Override
	protected void updateBuffer(final Graphics g) {

		revalidateScreen();

		if (!offscreenValid) {
			// clear the pallette...
			g.setColor(GUIFactory.DEFAULT_BG);
			g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
			g.setColor(Color.black);

			final Rectangle destRect = new Rectangle(0, 0,
					xmap.getUsedPixels(), ymap.getUsedPixels());

			final Rectangle sourceRect = new Rectangle(xmap.getIndex(0),
					ymap.getIndex(0), xmap.getIndex(destRect.width)
							- xmap.getIndex(0), ymap.getIndex(destRect.height)
							- ymap.getIndex(0));

			if ((sourceRect.x >= 0) && (sourceRect.y >= 0) && drawer != null) {
				drawer.paint(g, sourceRect, destRect, null);
			}
		}
	}

	/**
	 * This method updates a pixel buffer. The alternative is to update the
	 * graphics object directly by calling updateBuffer.
	 */
	@Override
	protected void updatePixels() {

//<<<<<<< HEAD:LinkedView/src/edu/stanford/genetics/treeview/plugin/dendroview/GlobalView.java
//		if (offscreenChanged) {
//			// LogBuffer.println("OFFSCREEN CHANGED");
//			offscreenValid = false;
//			xmap.setAvailablePixels(offscreenSize.width);
//			ymap.setAvailablePixels(offscreenSize.height);
//
//			if (!hasDrawn) {
//				// total kludge, but addnotify isn't working correctly...
//				xmap.recalculateScale();
//				ymap.recalculateScale();
//				hasDrawn = true;
//			}
//		}
//
//		if (resetHome) {
//			LogBuffer.println("Resetting GV");
//			xmap.setHome();
//			ymap.setHome();
//			
//			updateAspectRatio();
//
//			resetHome(false);
//		}
//
//=======
//>>>>>>> global_overview:LinkedView/src/edu/stanford/genetics/treeview/plugin/dendroview/InteractiveMatrixView.java
		if (!offscreenValid) {
			
			revalidateScreen();
			
			// LogBuffer.println("OFFSCREEN INVALID");
			final Rectangle destRect = new Rectangle(0, 0,
					xmap.getUsedPixels(), ymap.getUsedPixels());

			final Rectangle sourceRect = new Rectangle(xmap.getIndex(0),
					ymap.getIndex(0), xmap.getIndex(destRect.width)
							- xmap.getIndex(0), ymap.getIndex(destRect.height)
							- ymap.getIndex(0));

			if ((sourceRect.x >= 0) && (sourceRect.y >= 0) && drawer != null) {
				/*
				 * In case selection dimming should be brought back, there is
				 * this code below.
				 */
				// int[] geneSelections = new int[]
				// {geneSelection.getMinIndex(),
				// geneSelection.getMaxIndex()};
				// int[] arraySelections = new int[]
				// {arraySelection.getMinIndex(),
				// arraySelection.getMaxIndex()};

				/* Set new offscreenPixels (pixel colors) */
				drawer.paint(offscreenPixels, sourceRect, destRect,
						offscreenScanSize);

				// , geneSelections, arraySelections);
			}

			offscreenSource.newPixels();
		}
		
		xmap.notifyObservers();
		ymap.notifyObservers();
	}

	@Override
	public synchronized void paintComposite(final Graphics g) {

		if (selectionRectList != null) {

			/* draw all selection rectangles in yellow */
			g.setColor(Color.yellow);

			for (final Rectangle rect : selectionRectList) {
				g.drawRect(rect.x, rect.y, rect.width, rect.height);
			}

			/*
			 * draw white selection circle if only 1 tile is selected and small
			 * enough.
			 */
			if (indicatorCircle != null) {
				final Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(Color.white);
				g2.setStroke(new BasicStroke(3));
				g2.draw(indicatorCircle);
			}
		}
	}

	/**
	 * Checks the selection of genes and arrays and calculates the appropriate
	 * selection rectangle.
	 */
	@Override
	protected void recalculateOverlay() {

		if ((geneSelection == null) || (arraySelection == null)) {
			selectionRectList = null;
			return;
		}

		selectionRectList = new ArrayList<Rectangle>();

		final int[] selectedArrayIndexes = arraySelection.getSelectedIndexes();
		final int[] selectedGeneIndexes = geneSelection.getSelectedIndexes();

		if (selectedArrayIndexes.length > 0) {

			// LogBuffer.println("Selected min array index: [" +
			// selectedArrayIndexes[0] + "] Selected min gene index: [" +
			// selectedGeneIndexes[0] + "].");

			List<List<Integer>> arrayBoundaryList;
			List<List<Integer>> geneBoundaryList;

			arrayBoundaryList = findRectangleBoundaries(selectedArrayIndexes,
					xmap);
			geneBoundaryList = findRectangleBoundaries(selectedGeneIndexes,
					ymap);

			// Make the rectangles
			if (selectionRectList != null) {
				for (final List<Integer> xBoundaries : arrayBoundaryList) {

					for (final List<Integer> yBoundaries : geneBoundaryList) {

						selectionRectList.add(new Rectangle(xBoundaries.get(0),
								yBoundaries.get(0), xBoundaries.get(1)
								- xBoundaries.get(0), yBoundaries
								.get(1) - yBoundaries.get(0)));
					}
				}
			}
		}
		
		setIndicatorCircleBounds();
	}

	/**
	 * Finds the boundaries needed to draw all selection rectangles
	 *
	 * @param selectedIndexes
	 * @param map
	 * @return
	 */
	protected List<List<Integer>> findRectangleBoundaries(
			final int[] selectedIndexes, final MapContainer map) {

		int sp = 0;
		int ep = 0;

		final List<List<Integer>> rangeList = new ArrayList<List<Integer>>();
		final List<List<Integer>> boundaryList = new ArrayList<List<Integer>>();
		List<Integer> rectangleRange = new ArrayList<Integer>();

		/*
		 * If array is bigger than 1, check how many consecutive labels are
		 * selected by finding out which elements are only 1 apart. Store
		 * consecutive indexes separately to make separate rectangles later.
		 */
		int loopStart = 0;
		while (loopStart < selectedIndexes.length) {

			for (int i = loopStart; i < selectedIndexes.length; i++) {

				final int current = selectedIndexes[i];

				if (rectangleRange.size() == 0
						|| rectangleRange.get(rectangleRange.size() - 1) == current - 1) {
					rectangleRange.add(current);
					loopStart = i + 1;

				} else {
					break;
				}
			}

			rangeList.add(rectangleRange);
			rectangleRange = new ArrayList<Integer>();
		}

		/*
		 * For every selection range produce map values (rectangle boundaries)
		 * for each rectangle to be drawn.
		 */
		for (final List<Integer> selectionRange : rangeList) {

			final List<Integer> boundaries = new ArrayList<Integer>(2);

			sp = map.getPixel(selectionRange.get(0));
			// last pixel of last block
			ep = map.getPixel(selectionRange.get(selectionRange.size() - 1) + 1) - 1;

			if (ep < sp) {
				ep = sp;
				// correct for roundoff error above
			}

			boundaries.add(sp);
			boundaries.add(ep);

			boundaryList.add(boundaries);
		}

		return boundaryList;
	}

	@Override
	public void update(final Observable o, final Object arg) {

		if (o == geneSelection) {
			updateSelection(geneSelection, arraySelection);
			recalculateOverlay();

			/* trigger updatePixels() */
			offscreenValid = false;

		} else if (o == arraySelection) {
			updateSelection(arraySelection, geneSelection);
			recalculateOverlay();
			
			offscreenValid = false;
			repaint();

		} else {
			super.update(o, arg);
		}
	}
	
	/**
	 * Adjusts one axis selection to changes in the other.
	 * @param origin The axis selection object that has previously been
	 * changed.
	 * @param adjusting The axis selection object that needs to be adjusted.
	 */
	private static void updateSelection(TreeSelectionI origin, 
			TreeSelectionI adjusting) {
		
		if (adjusting.getNSelectedIndexes() == 0) {
			if (origin.getNSelectedIndexes() != 0) {
				// select all genes if some arrays selected...
				adjusting.selectAllIndexes();
				// notifies self...
				adjusting.notifyObservers();
				return;
			}
			/*
			 * When deselecting a tree node with right click, this matters,
			 * because in the eventlistener you can only deselect indices
			 * for the local tree selection. other axis here!
			 */
		} else if (origin.getNSelectedIndexes() == 0) {
			adjusting.deselectAllIndexes();
			adjusting.notifyObservers();
		}
	}

	/* TODO move to a specified controller class */
	private class MatrixMouseListener extends MouseAdapter {

		@Override
		public void mouseMoved(final MouseEvent e) {

			setDataStatus(e);
		}

		private void setDataStatus(final MouseEvent e) {

			final int ooverx = overx;
			final int oovery = overy;
			overx = xmap.getIndex(e.getX());
			overy = ymap.getIndex(e.getY());

			/* Timed repaint to avoid constant unnecessary repainting. */

			if (oovery != overy || ooverx != overx) {
				if (status != null) {
					status.setMessages(getStatus());
				}
			}
		}

		@Override
		public void mouseDragged(final MouseEvent e) {

			// When left button is used
			if (SwingUtilities.isLeftMouseButton(e)) {
				// rubber band?
				drawBand(dragRect);
				endPoint.setLocation(xmap.getIndex(e.getX()),
						ymap.getIndex(e.getY()));

				/* Full gene selection */
				if (e.isShiftDown()) {
					dragRect.setLocation(xmap.getMinIndex(), startPoint.y);
					dragRect.setSize(0, 0);
					dragRect.add(xmap.getMaxIndex(), endPoint.y);

					/* Full array selection */
				} else if (e.isControlDown()) {
					dragRect.setLocation(startPoint.x, ymap.getMinIndex());
					dragRect.setSize(0, 0);
					dragRect.add(endPoint.x, ymap.getMaxIndex());

					/* Normal selection */
				} else {
					dragRect.setLocation(startPoint.x, startPoint.y);
					dragRect.setSize(0, 0);
					dragRect.add(endPoint.x, endPoint.y);
				}

				drawBand(dragRect);
			}
		}

		@Override
		public void mouseReleased(final MouseEvent e) {

			if (!enclosingWindow().isActive())
				return;

			// When left button is used
			if (SwingUtilities.isLeftMouseButton(e)) {
				mouseDragged(e);
				drawBand(dragRect);

				/* Full gene selection */
				if (e.isShiftDown()) {
					final Point start = new Point(xmap.getMinIndex(),
							startPoint.y);
					final Point end = new Point(xmap.getMaxIndex(), endPoint.y);
					selectRectangle(start, end);

					/* Full array selection */
				} else if (e.isControlDown()) {
					final Point start = new Point(startPoint.x,
							ymap.getMinIndex());
					final Point end = new Point(endPoint.x, ymap.getMaxIndex());
					selectRectangle(start, end);

					/* Normal selection */
				} else {
					selectRectangle(startPoint, endPoint);
				}

			} else {
				// do something else?
			}

			repaint();
		}

		// Mouse Listener
		@Override
		public void mousePressed(final MouseEvent e) {

			if (!enclosingWindow().isActive())
				return;

			// if left button is used
			if (SwingUtilities.isLeftMouseButton(e)) {
				startPoint.setLocation(xmap.getIndex(e.getX()),
						ymap.getIndex(e.getY()));
				endPoint.setLocation(startPoint.x, startPoint.y);
				dragRect.setLocation(startPoint.x, startPoint.y);
				dragRect.setSize(endPoint.x - dragRect.x, endPoint.y
						- dragRect.y);

				drawBand(dragRect);

			} else if (SwingUtilities.isRightMouseButton(e)) {
				geneSelection.setSelectedNode(null);
				geneSelection.deselectAllIndexes();

				arraySelection.setSelectedNode(null);
				arraySelection.deselectAllIndexes();

				geneSelection.notifyObservers();
				arraySelection.notifyObservers();
			}
		}

		@Override
		public void mouseEntered(final MouseEvent e) {

			hasMouse = true;
			requestFocus();
		}

		@Override
		public void mouseExited(final MouseEvent e) {

			hasMouse = false;

			// Display empty field
			statustext[0] = "";
			statustext[1] = "";
			statustext[2] = "";

			status.setMessages(statustext);
		}
	}

	/**
	 * Zooming when the mouse wheel is used in conjunction with the alt/option key.
	 * Vertical scrolling if the shift key is not pressed.
	 */
	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {

		final int notches = e.getWheelRotation();
		final int shift = (notches < 0) ? -3 : 3;

		// On macs' magic mouse, horizontal scroll comes in as if the shift was
		// down
		if (e.isAltDown()) {
			if (notches < 0) {
				//This ensures we only zoom toward the cursor when the cursor is over the map
				if (status != null) {
					smoothZoomTowardPixel(e.getX(),e.getY());
				}
				//This should happen when the mouse is not over the heatmap
				else {
					xmap.zoomIn();
					ymap.zoomIn();
				}
			} else {
				if (status != null) {
					smoothZoomAwayPixel(e.getX(),e.getY());
				} else {
					xmap.zoomOut();
					ymap.zoomOut();
				}
			}
		} else if (e.isShiftDown()) {
			xmap.scrollBy(shift);
		} else {
			ymap.scrollBy(shift);
		}

		revalidate();
		repaint();
	}

//<<<<<<< HEAD:LinkedView/src/edu/stanford/genetics/treeview/plugin/dendroview/GlobalView.java
//	// @Override
//	// public String getToolTipText(final MouseEvent e) {
//	// /*
//	// * Do we want to do mouseovers if value already visible? if
//	// * (getShowVal()) return null; // don't do tooltips and vals at same
//	// * time.
//	// */
//	// String ret = "";
//	// String row = "";
//	// String col = "";
//	//
//	// if (drawer != null) {
//	//
//	// final int geneRow = overy;
//	//
//	// if (xmap.contains(overx) && ymap.contains(overy)) {
//	//
//	// if (geneHI != null) {
//	// row = geneSummary.getSummary(geneHI, overy);
//	//
//	// } else {
//	// row = "N/A";
//	// }
//	//
//	// if (arrayHI != null) {
//	// col = arraySummary.getSummary(arrayHI, overx);
//	//
//	// } else {
//	// col = "N/A";
//	// }
//	//
//	// if (drawer.isMissing(overx, geneRow)) {
//	// ret = "No data";
//	//
//	// } else if (drawer.isEmpty(overx, geneRow)) {
//	// ret = null;
//	//
//	// } else {
//	// ret = "<html>Row: " + row + " <br>Column: " + col
//	// + " <br>Value: "
//	// + drawer.getSummary(overx, geneRow) + "</html>";
//	// }
//	// }
//	// }
//	// return ret;
//	// }

	public void smoothZoomTowardPixel(int xPxPos,int yPxPos) {
		int zoomXVal = 0;
		int zoomYVal = 0;
		int numXCells = xmap.getNumVisible();
		int numYCells = ymap.getNumVisible();
		int xPxNum = xmap.getAvailablePixels();
		int yPxNum = ymap.getAvailablePixels();

		//If the current aspect ratio has not been set, set it
		if(aspectRatio < 0) {
			updateAspectRatio();
		}

		double targetZoomFrac = 0.05; //This is the amount by which we incrementally zoom in.  It is hard-coded here and in separate unrelated zooming functions in MapContainer

		//This will tell us how out of sync the aspect ratio has gotten from the smooth-zooming trick and
		//how many cells on the x axis we must add or remove to correct the aspect ratio
		//(to what it was before we started zooming)
		double numXCellsShouldHave = aspectRatio * (double) numYCells;

		//Could check for possible div by zero here, but should be impossible
		double targetZoomFracCorrection = Math.abs(numXCellsShouldHave - (double) numXCells) / (double) numXCells;
		//If numXCellsShouldHave is basically an integer and is equal to numXCells
		if((numXCellsShouldHave % 1) == 0 && ((int) Math.round(numXCellsShouldHave)) == numXCells) {
			zoomXVal = xmap.getBestZoomInVal(xPxPos,xPxNum,numXCells,targetZoomFrac);
			zoomYVal = ymap.getBestZoomInVal(yPxPos,yPxNum,numYCells,targetZoomFrac);
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in both dimensions
				if(numYCells > 1 && numXCells > 1) {
					zoomXVal = 1;
					zoomYVal = 1;
				} else if(numYCells > 1) {
					zoomYVal = 1;
				} else if(numXCells > 1) {
					zoomXVal = 1;
				}
			}
		} else if(numXCellsShouldHave > numXCells) {
			//If the X axis should have more cells on it, so zoom in on the y axis more (resulting in fewer rows) to relatively make the x axis have more cells in comparison
			//The fraction calculation is different for the Y axis.  The following calculation is based on the merging of a few equations:
			//current aspectRatio           = numXCells/numYCells
			//numXCellsShouldHave           = aspectRatio*numYCells                                     //If we decide to make Y "correct", we can calculate what X should be
			//numXCellsShouldHave/numYCells = numXCells/(numYCells+_numYCellsShouldHave_) = aspectRatio //We do not know _numYCellsShouldHave_, solving for it, we have:
			//_numYCellsShouldHave_         = numXCells*numYCells/numXCellsShouldHave-numYCells         //Then, canceling out numYCells, we have:
			//_numYCellsShouldHave_         = numXCells/numXCellsShouldHave-1                           //_numYCellsShouldHave_ will be a ratio, i.e. the same as targetZoomFracCorrection
			targetZoomFracCorrection = Math.abs((double) numXCells / numXCellsShouldHave - 1);
			zoomXVal = xmap.getBestZoomInVal(xPxPos,xPxNum,numXCells,targetZoomFrac);
			zoomYVal = ymap.getBestZoomInVal(yPxPos,yPxNum,numYCells,targetZoomFrac + targetZoomFracCorrection);
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in the Y dimension
				if(numYCells > 1) {
					zoomYVal = 1;
				} else if(numXCells > 1) {
					zoomXVal = 1;
				}
			}
		}
		else {
			//This should theoretically be more accurate, but it is not for some reason:
			//targetZoomFracCorrection = numXCellsShouldHave / (double) numXCells;

			zoomXVal = xmap.getBestZoomInVal(xPxPos,xPxNum,numXCells,targetZoomFrac + targetZoomFracCorrection);
			zoomYVal = ymap.getBestZoomInVal(yPxPos,yPxNum,numYCells,targetZoomFrac);
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in the X dimension
				if(numXCells > 1) {
					zoomXVal = 1;
				} else if(numYCells > 1) {
					zoomYVal = 1;
				}
			}
		}
		//LogBuffer.println("Aspect ratio.  Current: [" + numXCells + " / " + numYCells + "] Target: [" + aspectRatio + "].  targetZoomFracCorrection: [" + targetZoomFracCorrection + "].  New zoomXVal: [" + zoomXVal + "] New zoomYVal: [" + zoomYVal + "] numXCellsShouldHave: [" + numXCellsShouldHave + "].");

		if(xmap.zoomTowardPixel(xPxPos,zoomXVal) == 1)
			updateAspectRatio();
		if(ymap.zoomTowardPixel(yPxPos,zoomYVal) == 1)
			updateAspectRatio();
	}

	public void smoothZoomAwayPixel(int xPxPos,int yPxPos) {
		int zoomXVal = 0;
		int zoomYVal = 0;
		int numXCells = xmap.getNumVisible();
		int numYCells = ymap.getNumVisible();
		int xPxNum = xmap.getAvailablePixels();
		int yPxNum = ymap.getAvailablePixels();

		//If the current aspect ratio has not been set, set it
		if(aspectRatio < 0) {
			updateAspectRatio();
		}

		double targetZoomFrac = 0.05; //This is the amount by which we incrementally zoom in.  It is hard-coded here and in separate unrelated zooming functions in MapContainer

		//This will tell us how out of sync the aspect ratio has gotten from the smooth-zooming trick and
		//how many cells on the x axis we must add or remove to correct the aspect ratio
		//(to what it was before we started zooming)
		double numXCellsShouldHave = aspectRatio * (double) numYCells;

		//Could check for possible div by zero here, but should be impossible
		double targetZoomFracCorrection = Math.abs(numXCellsShouldHave - (double) numXCells) / (double) numXCells;
		//If numXCellsShouldHave is basically an integer and equal to numXCells
		if((numXCellsShouldHave % 1) == 0 && ((int) Math.round(numXCellsShouldHave)) == numXCells) {
			zoomXVal = xmap.getBestZoomOutVal(xPxPos,xPxNum,numXCells,targetZoomFrac);
			zoomYVal = ymap.getBestZoomOutVal(yPxPos,yPxNum,numYCells,targetZoomFrac);
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in both dimensions, try to zoom out 2, then 1
				if(numYCells < ymap.getMaxIndex() && numXCells < xmap.getMaxIndex()) {
					zoomXVal = 2;
					zoomYVal = 2;
				} else if(numYCells < ymap.getMaxIndex()) {
					zoomYVal = 2;
				} else if(numXCells < xmap.getMaxIndex()) {
					zoomXVal = 2;
				} else if(numYCells < (ymap.getMaxIndex() + 1) && numXCells < (xmap.getMaxIndex() + 1)) {
					zoomXVal = 1;
					zoomYVal = 1;
				} else if(numYCells < (ymap.getMaxIndex() + 1)) {
					zoomYVal = 1;
				} else if(numXCells < (xmap.getMaxIndex() + 1)) {
					zoomXVal = 1;
				}
			}
		} else if(numXCellsShouldHave > numXCells) {
			//This should theoretically be more accurate, but it is not for some reason:
			//targetZoomFracCorrection = numXCellsShouldHave / (double) numXCells;

			//If the X axis should have more cells on it, zoom out on the x axis more
			zoomXVal = xmap.getBestZoomOutVal(xPxPos,xPxNum,numXCells,targetZoomFrac + targetZoomFracCorrection);
			zoomYVal = ymap.getBestZoomOutVal(yPxPos,yPxNum,numYCells,targetZoomFrac);
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom out in the X dimension
				if(numXCells < (xmap.getMaxIndex() + 1)) {
					zoomXVal = 1;
				} else if(numYCells < (ymap.getMaxIndex() + 1)) {
					zoomYVal = 1;
				}
			}
		}
		else {
			//Else, the X axis should have fewer cells on it, so zoom out on the y axis more
			//The fraction calculation is different for the Y axis.  The following calculation is based on the merging of a few equations:
			//current aspectRatio           = numXCells/numYCells
			//numXCellsShouldHave           = aspectRatio*numYCells                                     //If we decide to make Y "correct", we can calculate what X should be
			//numXCellsShouldHave/numYCells = numXCells/(numYCells+_numYCellsShouldHave_) = aspectRatio //We do not know _numYCellsShouldHave_, solving for it, we have:
			//_numYCellsShouldHave_         = numXCells*numYCells/numXCellsShouldHave-numYCells         //Then, canceling out numYCells, we have:
			//_numYCellsShouldHave_         = numXCells/numXCellsShouldHave-1                           //_numYCellsShouldHave_ will be a ratio, i.e. the same as targetZoomFracCorrection
			targetZoomFracCorrection = Math.abs((double) numXCells / numXCellsShouldHave - 1);
			zoomXVal = xmap.getBestZoomOutVal(xPxPos,xPxNum,numXCells,targetZoomFrac);
			zoomYVal = ymap.getBestZoomOutVal(yPxPos,yPxNum,numYCells,targetZoomFrac + targetZoomFracCorrection);
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom out in the Y dimension
				if(numYCells < (ymap.getMaxIndex() + 1)) {
					zoomYVal = 1;
				} else if(numXCells < (xmap.getMaxIndex() + 1)) {
					zoomXVal = 1;
				}
			}
		}
		//LogBuffer.println("Aspect ratio.  Current: [" + numXCells + " / " + numYCells + "] Target: [" + aspectRatio + "].  targetZoomFracCorrection: [" + targetZoomFracCorrection + "].  New zoomXVal: [" + zoomXVal + "] New zoomYVal: [" + zoomYVal + "] numXCellsShouldHave: [" + numXCellsShouldHave + "].");

		if(xmap.zoomAwayPixel(xPxPos,zoomXVal) == 1)
			updateAspectRatio();
		if(ymap.zoomAwayPixel(yPxPos,zoomYVal) == 1)
			updateAspectRatio();
	}

	/* TODO: Simplify/streamline this function and move a bunch of steps to independent functions in MapContainer - Rob */
	//Currently, this function assumes that the selected cells are smaller than or equal to the number of cells currently visible
	//I should rename it to smoothZoomToSelection and update it to be able to handle zooming out when the visible area is smaller
	//(or 1 dimension smaller, the other larger) and to be able to smoothly zoom/scroll to a selection outside of the visible area
	//Essentially, all this function does is it calls getZoomTowardPixelOfSelection to determine the pixel in the selection to zoom toward
	//  (so that the selection ends up filling the view) and then uses that pixel to call zoomTowardPixel.
	public void smoothZoomTowardSelection(int selecXStartIndex,int numXSelectedIndexes,int selecYStartIndex,int numYSelectedIndexes) {
		
		//Find the pixel inside the selected area to "zoom toward" such that the selected area essentially expands to fill the screen
		int startXPixel = xmap.getPixel(selecXStartIndex);
		double pixelsPerXIndex = xmap.getScale();
		int numSelectedXPixels = (int) Math.round((double) numXSelectedIndexes * pixelsPerXIndex);
		//int xPxPos = xmap.getZoomTowardPixelOfSelection(startXPixel,numSelectedXPixels);
		int xPxPos = xmap.getZoomTowardPixelOfSelectionUniversal(startXPixel,numSelectedXPixels);
		xmap.getZoomTowardPixelOfSelection(startXPixel,numSelectedXPixels);
		int startYPixel = ymap.getPixel(selecYStartIndex);
		double pixelsPerYIndex = ymap.getScale();
		int numSelectedYPixels = (int) Math.round((double) numYSelectedIndexes * pixelsPerYIndex);
		//int yPxPos = ymap.getZoomTowardPixelOfSelection(startYPixel,numSelectedYPixels);
		int yPxPos = ymap.getZoomTowardPixelOfSelectionUniversal(startYPixel,numSelectedYPixels);
		ymap.getZoomTowardPixelOfSelection(startYPixel,numSelectedYPixels);
		
		//LogBuffer.println("Going to zoom toward pixel at: [x" + xPxPos + ",y" + yPxPos + "] determined from data cell at: [x" + selecXStartIndex + ",y" + selecYStartIndex +
		//		"].  Selected pixel start/number: [x" + startXPixel + "/" + numSelectedXPixels + ",y" + startYPixel + "/" + numSelectedYPixels + "].");
		
		//The final aspect ratio of the selected area will be our target aspect ratio (used for smoothness corrections)
		double targetAspectRatio = (double) numXSelectedIndexes / (double) numYSelectedIndexes;
		
		int zoomXVal  = 0;
		int zoomYVal  = 0;
		int numXCells = xmap.getNumVisible();
		int numYCells = ymap.getNumVisible();
		int xPxNum    = xmap.getAvailablePixels();
		int yPxNum    = ymap.getAvailablePixels();
		int prevXFirstVisible = xmap.getFirstVisible();
		int prevXNumVisible = xmap.getNumVisible();
		int centerXIndex = xmap.getFirstVisible() + (int) Math.floor((double) xmap.getNumVisible() / 2.0);
		int prevYFirstVisible = ymap.getFirstVisible();
		int prevYNumVisible = ymap.getNumVisible();
		int centerYIndex = ymap.getFirstVisible() + (int) Math.floor((double) ymap.getNumVisible() / 2.0);

		double targetZoomFracX = 0.5;
		//The following loop cycles through zoom increments 0.05,0.1,0.15,... to 0.5
		//and stops when the zoom increment is large enough to overcome the long time it takes to draw huge matrices
		//It uses an equation that was solved using these 2 equations (forcing a logarithmic curve to pass through 2 points)
		//6000 = a * b^0.5 and 100 = a * b^0.05
		//Basically, when the number of visible spots is 6000, the zoom increment will be 50%.
		//When the number of visible spots is 100, the zoom increment will be 5%.  There's a log scale in between.
		int largerXSize = numXSelectedIndexes;
		if(numXCells > largerXSize) {
			largerXSize = numXCells;
		}
		for(int zf = 1;zf<=10;zf++) {
			double zmfc = (double) zf * 0.05;
			if(63.45*Math.pow(8942,zmfc) > largerXSize) {
				targetZoomFracX = zmfc;
				break;
			}
		}

		double targetZoomFracY = 0.5;
		//The following loop cycles through zoom increments 0.05,0.1,0.15,... to 0.5
		//and stops when the zoom increment is large enough to overcome the long time it takes to draw huge matrices
		//It uses an equation that was solved using these 2 equations (forcing a logarithmic curve to pass through 2 points)
		//6000 = a * b^0.5 and 100 = a * b^0.05
		//Basically, when the number of visible spots is 6000, the zoom increment will be 50%.
		//When the number of visible spots is 100, the zoom increment will be 5%.  There's a log scale in between.
		int largerYSize = numYSelectedIndexes;
		if(numYCells > largerYSize) {
			largerYSize = numYCells;
		}
		for(int zf = 1;zf<=10;zf++) {
			double zmfc = (double) zf * 0.05;
			if(63.45*Math.pow(8942,zmfc) > largerYSize) {
				targetZoomFracY = zmfc;
				break;
			}
		}

		//Select the larger zoom increment from the 2 dimensions
		double targetZoomFrac = targetZoomFracX;
		if(targetZoomFrac < targetZoomFracY) targetZoomFrac = targetZoomFracY;
		//LogBuffer.println("Zoom fraction: [" + targetZoomFrac + "] XtargetFrac: [" + targetZoomFracX + "] YtargetFrac: [" + targetZoomFracY + "].");

		double aspectRatioFracCorrection = 0.2; //This makes the change in aspect ratio *somewhat* gradual, though it's not synced up with the zoom fraction (because we don't know how many iterations it will take)

		//This will tell us how out of sync the aspect ratio has gotten from the smooth-zooming trick and
		//how many cells on the x axis we must add or remove to correct the aspect ratio
		//(to what it was before we started zooming)
		double numXCellsShouldHave = targetAspectRatio * (double) numYCells;

		double targetZoomFracCorrection = 0.0;
		
		//If numXCellsShouldHave is basically an integer and is equal to numXCells
		if((numXCellsShouldHave % 1) == 0 && ((int) Math.round(numXCellsShouldHave)) == numXCells) {
			zoomXVal = xmap.getBestZoomInVal(xPxPos,xPxNum,numXCells,targetZoomFrac);
			zoomYVal = ymap.getBestZoomInVal(yPxPos,yPxNum,numYCells,targetZoomFrac);
			
			LogBuffer.println("Jumping to [" + numXCellsShouldHave + "] because it's an integer equal to the target number of cells.");
			
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in both dimensions
				if(((numXSelectedIndexes <= xmap.getNumVisible() && numXCells > 1) ||
						(numXSelectedIndexes > xmap.getNumVisible() && numXCells < (xmap.getMaxIndex() + 1))) &&
					((numYSelectedIndexes <= ymap.getNumVisible() && numYCells > 1) ||
						(numYSelectedIndexes > ymap.getNumVisible() && numYCells < (ymap.getMaxIndex() + 1)))) {
					zoomXVal = 1;
					zoomYVal = 1;
				} else if((numXSelectedIndexes <= xmap.getNumVisible() && numXCells > 1) ||
						(numXSelectedIndexes > xmap.getNumVisible() && numXCells < (xmap.getMaxIndex() + 1))) {
					zoomXVal = 1;
				} else if((numYSelectedIndexes <= ymap.getNumVisible() && numYCells > 1) ||
						(numYSelectedIndexes > ymap.getNumVisible() && numYCells < (ymap.getMaxIndex() + 1))) {
					zoomYVal = 1;
				}
			}
		}
		//Else if we should have more X cells and we are zooming in on the Y axis
		else if((numXCellsShouldHave > numXCells && numYCells > numYSelectedIndexes) || (numXCellsShouldHave < numXCells && numYCells < numYSelectedIndexes)) {
			double numYCellsShouldHave = (double) numXCells / targetAspectRatio;
			if(numYCellsShouldHave < numYSelectedIndexes) {
				numYCellsShouldHave = numYSelectedIndexes;
			} else if(numYCellsShouldHave > (ymap.getMaxIndex() + 1)) {
				numYCellsShouldHave = (ymap.getMaxIndex() + 1);
			}

			//To cause the targetAspectRatio to be arrived at gradually, let's only correct by a percentage of the correction:
			numYCellsShouldHave = (double) numYCells - Math.abs(numYCellsShouldHave - (double) numYCells) * aspectRatioFracCorrection;
			//LogBuffer.println("Zooming in on Y axis more: numYCellsShouldHave = numYCells - Math.abs(numYCellsShouldHave - numYCells) * aspectRatioFracCorrection");
			//LogBuffer.println("Zooming in on Y axis more: " + numYCellsShouldHave + " = " + numYCells + " - Math.abs(" + numYCellsShouldHave + " - " + numYCells + ") * " + aspectRatioFracCorrection);
			
			//Could check for possible div by zero here, but should be impossible
			targetZoomFracCorrection = Math.abs(numYCellsShouldHave - (double) numYCells) / (double) numYCells;
			//LogBuffer.println("Zooming in on Y axis more: targetZoomFracCorrection = Math.abs(numYCellsShouldHave - numYCells) / numYCells");
			//LogBuffer.println("Zooming in on Y axis more: " + targetZoomFracCorrection + " = Math.abs(" + numYCellsShouldHave + " - " + numYCells + ") / " + numYCells);
			
			//If the X axis should have more cells on it, so zoom in on the y axis more (resulting in fewer rows) to relatively make the x axis have more cells in comparison
			//The fraction calculation is different for the Y axis.  The following calculation is based on the merging of a few equations:
			//current aspectRatio           = numXCells/numYCells
			//numXCellsShouldHave           = aspectRatio*numYCells                                     //If we decide to make Y "correct", we can calculate what X should be
			//numXCellsShouldHave/numYCells = numXCells/(numYCells+_numYCellsShouldHave_) = aspectRatio //We do not know _numYCellsShouldHave_, solving for it, we have:
			//_numYCellsShouldHave_         = numXCells*numYCells/numXCellsShouldHave-numYCells         //Then, canceling out numYCells, we have:
			//_numYCellsShouldHave_         = numXCells/numXCellsShouldHave-1                           //_numYCellsShouldHave_ will be a ratio, i.e. the same as targetZoomFracCorrection
			////////targetZoomFracCorrection = Math.abs((double) numXCells / numXCellsShouldHave - 1);
			if(numXCells >= numXSelectedIndexes) {
				zoomXVal = xmap.getBestZoomInVal(xPxPos,xPxNum,numXCells,targetZoomFrac);
			} else {
				zoomXVal = xmap.getBestZoomOutVal(xPxPos,xPxNum,numXCells,targetZoomFrac);
			}
			if(numYCells >= numYSelectedIndexes) {
				zoomYVal = ymap.getBestZoomInVal(yPxPos,yPxNum,numYCells,targetZoomFrac + targetZoomFracCorrection);
			} else {
				zoomYVal = ymap.getBestZoomOutVal(yPxPos,yPxNum,numYCells,targetZoomFrac + targetZoomFracCorrection);
			}

			//LogBuffer.println("Zooming in more on the Y axis because the number of X cells we should have [" + numXCellsShouldHave + "] is greater than the current number of cells [" + numXCells + "], so we calculated this many Y cells we should have given a gradual aspect ratio change: [" + numYCellsShouldHave + "].");

			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in the X dimension
				if((numXSelectedIndexes <= xmap.getNumVisible() && numXCells > 1) ||
						(numXSelectedIndexes > xmap.getNumVisible() && numXCells < (xmap.getMaxIndex() + 1))) {
					zoomXVal = 1;
				} else if((numYSelectedIndexes <= ymap.getNumVisible() && numYCells > 1) ||
						(numYSelectedIndexes > ymap.getNumVisible() && numYCells < (ymap.getMaxIndex() + 1))) {
					zoomYVal = 1;
				}
			}
		}
		else {
			//To cause the targetAspectRatio to be arrived at gradually, let's only correct by a percentage of the correction:
			numXCellsShouldHave = numXCells + (numXCells - numXCellsShouldHave) * aspectRatioFracCorrection;
			
			//Could check for possible div by zero here, but should be impossible
			targetZoomFracCorrection = Math.abs(numXCellsShouldHave - (double) numXCells) / (double) numXCells;
			
			//This should theoretically be more accurate, but it is not for some reason:
			//targetZoomFracCorrection = numXCellsShouldHave / (double) numXCells;

			if(numXCells >= numXSelectedIndexes) {
				zoomXVal = xmap.getBestZoomInVal(xPxPos,xPxNum,numXCells,targetZoomFrac + targetZoomFracCorrection);
			} else {
				zoomXVal = xmap.getBestZoomOutVal(xPxPos,xPxNum,numXCells,targetZoomFrac + targetZoomFracCorrection);
			}
			if(numYCells >= numYSelectedIndexes) {
				zoomYVal = ymap.getBestZoomInVal(yPxPos,yPxNum,numYCells,targetZoomFrac);
			} else {
				zoomYVal = ymap.getBestZoomOutVal(yPxPos,yPxNum,numYCells,targetZoomFrac);
			}

			//LogBuffer.println("Zooming in more on the X axis because the number of X cells we should have [" + numXCellsShouldHave + "] is less than or equal to the target number of X cells [" + numXCells + "].");

			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in the X dimension
				if((numXSelectedIndexes <= xmap.getNumVisible() && numXCells > 1) ||
						(numXSelectedIndexes > xmap.getNumVisible() && numXCells < (xmap.getMaxIndex() + 1))) {
					zoomXVal = 1;
				} else if((numYSelectedIndexes <= ymap.getNumVisible() && numYCells > 1) ||
						(numYSelectedIndexes > ymap.getNumVisible() && numYCells < (ymap.getMaxIndex() + 1))) {
					zoomYVal = 1;
				}
			}
		}
		//LogBuffer.println("Aspect ratio.  Current: [" + numXCells + " / " + numYCells + "] Target: [" + targetAspectRatio + "].  targetZoomFracCorrection: [" + targetZoomFracCorrection + "].  New zoomXVal: [" + zoomXVal + "] New zoomYVal: [" + zoomYVal + "] numXCellsShouldHave: [" + numXCellsShouldHave + "].");

		if(numXSelectedIndexes <= xmap.getNumVisible()) {
			//Let's make sure that we're not going to zoom past our target area
			if((xmap.getNumVisible() - zoomXVal) < numXSelectedIndexes) {
				zoomXVal = xmap.getNumVisible() - numXSelectedIndexes;
			}
			
			//Now double-check that a zoom will indeed happen
			if(zoomXVal == 0 && zoomYVal == 0 && numYSelectedIndexes != ymap.getNumVisible()) {
				zoomYVal = 1;
			}
			
			xmap.zoomTowardPixel(xPxPos,zoomXVal);
		} else {
			//Let's make sure that we're not going to zoom past our target area
			if((xmap.getNumVisible() + zoomXVal) > numXSelectedIndexes) {
				zoomXVal = Math.abs(xmap.getNumVisible() - numXSelectedIndexes);
			}
			
			//Now double-check that a zoom will indeed happen
			if(zoomXVal == 0 && zoomYVal == 0 && numYSelectedIndexes != ymap.getNumVisible()) {
				zoomYVal = 1;
			}
			
			xmap.zoomAwayPixel(xPxPos,zoomXVal);
		}
		if(numYSelectedIndexes <= ymap.getNumVisible()) {
			//Let's make sure that we're not going to zoom past our target area
			if((ymap.getNumVisible() - zoomYVal) < numYSelectedIndexes) {
				zoomYVal = ymap.getNumVisible() - numYSelectedIndexes;
			}

			//Now double-check that a zoom will indeed happen
			if(zoomXVal == 0 && zoomYVal == 0 && numXSelectedIndexes != xmap.getNumVisible()) {
				zoomXVal = 1;
				//We have to do an extra here because we didn't do any x zooming above
				if(numXSelectedIndexes <= xmap.getNumVisible()) {
					xmap.zoomTowardPixel(xPxPos,zoomXVal);
				} else {
					xmap.zoomAwayPixel(xPxPos,zoomXVal);
				}
			}
			
			ymap.zoomTowardPixel(yPxPos,zoomYVal);
		} else {
			//Let's make sure that we're not going to zoom past our target area
			if((ymap.getNumVisible() + zoomYVal) > numYSelectedIndexes) {
				zoomYVal = Math.abs(ymap.getNumVisible() - numYSelectedIndexes);
			}

			//Now double-check that a zoom will indeed happen
			if(zoomXVal == 0 && zoomYVal == 0 && numXSelectedIndexes != xmap.getNumVisible()) {
				zoomXVal = 1;
				//We have to do an extra here because we didn't do any x zooming above
				if(numXSelectedIndexes <= xmap.getNumVisible()) {
					xmap.zoomTowardPixel(xPxPos,zoomXVal);
				} else {
					xmap.zoomAwayPixel(xPxPos,zoomXVal);
				}
			}
			
			ymap.zoomAwayPixel(yPxPos,zoomYVal);
		}
			
		//Now that we have zoomed, we should scroll if the selection is off the screen
		//If zooming has finished, scroll all the way
		if(xmap.getNumVisible() == numXSelectedIndexes && ymap.getNumVisible() == numYSelectedIndexes) {
			xmap.scrollToFirstIndex(selecXStartIndex);
			ymap.scrollToFirstIndex(selecYStartIndex);
		} else {
			//Scroll toward by half of the same fraction as the zoom changed
			//We don't want to scroll all the way in 1 sudden jolt, so we'll scroll to be closer to the center of the selection by (half) the same fraction as the incremental zoom (unless we're at the zoom level of the selection)
			double scrollFrac =  ((double) xmap.getNumVisible() / (double) prevXNumVisible);
			if(scrollFrac > 1) {
				scrollFrac =  (double) prevXNumVisible / (double) xmap.getNumVisible();
			}
			scrollFrac /= 2.0;
			
			int scrollDistDirX = (selecXStartIndex + (int) Math.floor(numXSelectedIndexes / 2)) - centerXIndex;
			scrollDistDirX = (int) Math.round((double) scrollDistDirX * scrollFrac);
			
			/* TODO: Refactor the scrolling so that these checks do not have to be done. - Rob */
			//The following correction calculations are to prevent scrolling a visible selected edge out of view./
			//The calculations are not perfect and are only necessary because the zoomToward* functions only really work when
			//the selection is either completely in view or we're zoomed in inside the selected area.
			int prevFarEdge = prevXFirstVisible + prevXNumVisible;
			int selecFarEdge = selecXStartIndex + numXSelectedIndexes;
			int newNearEdge = (selecXStartIndex + (int) Math.floor(numXSelectedIndexes / 2)) - scrollDistDirX - (int) Math.floor((double) xmap.getNumVisible() / 2);
			int newFarEdge = (selecXStartIndex + (int) Math.floor(numXSelectedIndexes / 2)) - scrollDistDirX + (int) Math.ceil((double) xmap.getNumVisible() / 2);
			//Make sure we don't scroll an edge from either in view or before the view to out of view/past the view
			if(selecFarEdge <= prevFarEdge && selecFarEdge > newFarEdge) {
				scrollDistDirX -= (selecFarEdge - newFarEdge) + (int) Math.round((1.0 - scrollFrac) * ((double) selecFarEdge - (double) newFarEdge));
				newNearEdge = (selecXStartIndex + (int) Math.floor(numXSelectedIndexes / 2)) - scrollDistDirX - (int) Math.ceil((double) xmap.getNumVisible() / 2);
				if(selecFarEdge > newFarEdge || selecXStartIndex >= prevXFirstVisible && selecXStartIndex < newNearEdge) {
					scrollDistDirX = 0;
				}
			} else if(selecXStartIndex >= prevXFirstVisible && selecXStartIndex < newNearEdge) {
				scrollDistDirX += (newNearEdge - selecXStartIndex) + (int) Math.round((1.0 - scrollFrac) * ((double) newNearEdge - (double) selecXStartIndex));
				newFarEdge = (selecXStartIndex + (int) Math.floor(numXSelectedIndexes / 2)) - scrollDistDirX + (int) Math.ceil((double) xmap.getNumVisible() / 2);
				if(selecXStartIndex < newNearEdge || selecFarEdge <= prevFarEdge && selecFarEdge > newFarEdge) {
					scrollDistDirX = 0;
				}
			}
			
			xmap.scrollToIndex((selecXStartIndex + (int) Math.floor(numXSelectedIndexes / 2)) - scrollDistDirX);

			//Scroll toward by half of the same fraction as the zoom changed
			scrollFrac =  ((double) ymap.getNumVisible() / (double) prevYNumVisible); //We don't want to scroll all the way in 1 jolt, so we'll scroll to be closer to the center of the selection by this fraction (unless we're at the zoom level of the selection)
			if(scrollFrac > 1) {
				scrollFrac =  (double) prevYNumVisible / (double) ymap.getNumVisible();
			}
			scrollFrac /= 2.0;

			int scrollDistDirY = (selecYStartIndex + (int) Math.floor(numYSelectedIndexes / 2)) - centerYIndex;
			scrollDistDirY = (int) Math.round((double) scrollDistDirY * scrollFrac);

			/* TODO: Refactor the scrolling so that these checks do not have to be done. - Rob */
			//The following correction calculations are to prevent scrolling a visible selected edge out of view./
			//The calculations are not perfect and are only necessary because the zoomToward* functions only really work when
			//the selection is either completely in view or we're zoomed in inside the selected area.
			prevFarEdge = prevYFirstVisible + prevYNumVisible;
			selecFarEdge = selecYStartIndex + numYSelectedIndexes;
			newNearEdge = (selecYStartIndex + (int) Math.floor(numYSelectedIndexes / 2)) - scrollDistDirY - (int) Math.ceil((double) ymap.getNumVisible() / 2);
			newFarEdge = (selecYStartIndex + (int) Math.floor(numYSelectedIndexes / 2)) - scrollDistDirY + (int) Math.ceil((double) ymap.getNumVisible() / 2);
			//Make sure we don't scroll an edge from either in view or before the view to out of view/past the view
			if(selecFarEdge <= prevFarEdge && selecFarEdge > newFarEdge) {
				scrollDistDirY -= (selecFarEdge - newFarEdge) + (int) Math.round((1.0 - scrollFrac) * ((double) selecFarEdge - (double) newFarEdge));
				newNearEdge = (selecYStartIndex + (int) Math.floor(numYSelectedIndexes / 2)) - scrollDistDirY - (int) Math.ceil((double) ymap.getNumVisible() / 2);
				if(selecFarEdge > newFarEdge || selecYStartIndex >= prevYFirstVisible && selecYStartIndex < newNearEdge) {
					scrollDistDirY = 0;
				}
			} else if(selecYStartIndex >= prevYFirstVisible && selecYStartIndex < newNearEdge) {
				scrollDistDirY += (int) Math.round((1.0 - scrollFrac) * ((double) newNearEdge - (double) selecYStartIndex));
				newFarEdge = (selecYStartIndex + (int) Math.floor(numYSelectedIndexes / 2)) - scrollDistDirY + (int) Math.ceil((double) ymap.getNumVisible() / 2);
				if(selecYStartIndex < newNearEdge || selecFarEdge <= prevFarEdge && selecFarEdge > newFarEdge) {
					scrollDistDirY = 0;
				}
			}
			
			ymap.scrollToIndex((selecYStartIndex + (int) Math.floor(numYSelectedIndexes / 2)) - scrollDistDirY);
		}
		
		//Update the aspect ratio once we have arrived at our destination (as we are going to be using the starting aspect ratio to use it to gradually adjust the target aspect ratio)
		if(xmap.getNumVisible() == numXSelectedIndexes && ymap.getNumVisible() == numYSelectedIndexes) {
			updateAspectRatio();
		}
	}

	public double getAspectRatio(int numXDots,int numYDots) {
		return((double) numXDots / (double) numYDots);
	}
	
	public void updateAspectRatio() {
		aspectRatio = getAspectRatio(xmap.getNumVisible(),ymap.getNumVisible());
		//LogBuffer.println("Aspect Ratio updated to [" + aspectRatio + "].");
	}

//=======
//>>>>>>> global_overview:LinkedView/src/edu/stanford/genetics/treeview/plugin/dendroview/InteractiveMatrixView.java
	private void drawBand(final Rectangle l) {

		final Graphics g = getGraphics();
		g.setXORMode(getBackground());
		g.setColor(GUIFactory.MAIN);

		final int x = xmap.getPixel(l.x);
		final int y = ymap.getPixel(l.y);
		final int w = xmap.getPixel(l.x + l.width + 1) - x;
		final int h = ymap.getPixel(l.y + l.height + 1) - y;

		g.drawRect(x, y, w, h);
		g.setPaintMode();
	}

	/**
	 * Draws a circle if the user selects one rectangle in the clustergram to
	 * indicate the position of this rectangle.
	 */
	private void setIndicatorCircleBounds() {

		double x = 0;
		double y = 0;
		double w = 0;
		double h = 0;

		if (geneSelection == null || arraySelection == null)
			return;
		else if ((geneSelection.getNSelectedIndexes() == 1 && arraySelection
				.getNSelectedIndexes() == 1)
				&& (xmap.getScale() < 20.0 && ymap.getScale() < 20.0)) {

			// Width and height of rectangle which spans the Ellipse2D object
			w = xmap.getUsedPixels() * 0.05;
			h = w;

			// coords for center of circle
			x = xmap.getPixel(arraySelection.getSelectedIndexes()[0]) - (w / 2)
					+ (xmap.getScale() / 2);
			y = ymap.getPixel(geneSelection.getSelectedIndexes()[0]) - (h / 2)
					+ (ymap.getScale() / 2);

			if (indicatorCircle == null) {
				indicatorCircle = new Ellipse2D.Double(x, y, w, h);

			} else {
				indicatorCircle.setFrame(x, y, w, h);
			}
		} else if (indicatorCircle != null) {
			indicatorCircle.setFrame(x, y, w, h);
		}
	}

	/**
	 * Selecting a rectangular area in GlobalView
	 *
	 * @param start
	 * @param end
	 */
	private void selectRectangle(final Point start, final Point end) {

		// sort so that ep is upper left corner
		if (end.x < start.x) {
			final int x = end.x;
			end.x = start.x;
			start.x = x;
		}

		if (end.y < start.y) {
			final int y = end.y;
			end.y = start.y;
			start.y = y;
		}

		// nodes
		geneSelection.setSelectedNode(null);

		// genes...
		geneSelection.deselectAllIndexes();

		for (int i = start.y; i <= end.y; i++) {

			geneSelection.setIndexSelection(i, true);
		}

		// arrays...
		arraySelection.setSelectedNode(null);
		arraySelection.deselectAllIndexes();

		for (int i = start.x; i <= end.x; i++) {

			arraySelection.setIndexSelection(i, true);
		}

		geneSelection.notifyObservers();
		arraySelection.notifyObservers();
	}

	/**
	 * Scrolls to the center of the selected rectangle
	 */
	public void centerView(int scrollX, int scrollY) {

		scrollX = scrollX + (xmap.getScroll().getVisibleAmount() / 2);
		scrollY = scrollY + (ymap.getScroll().getVisibleAmount() / 2);

		xmap.scrollToIndex(scrollX);
		ymap.scrollToIndex(scrollY);
	}

	/**
	 * Sets the gene header instance variables of GlobalView.
	 *
	 * @param ghi
	 * @param ahi
	 */
	public void setHeaders(final HeaderInfo ghi, final HeaderInfo ahi) {

		geneHI = ghi;
		arrayHI = ahi;
	}
}
