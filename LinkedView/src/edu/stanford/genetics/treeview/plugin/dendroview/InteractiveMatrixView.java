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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.Timer;

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

	private double aspectRatio = -1;

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
	private List<Ellipse2D.Double> indicatorCircleList = null;

	GlobalMatrixView globalMatrixView = null;

	/**
	 * GlobalView also likes to have an globalxmap and globalymap (both of type
	 * MapContainer) to help it figure out where to draw things. It also tries
	 * to
	 */
	public InteractiveMatrixView() {

		super();

		setLabelPortMode(true);
		debug = 0;

		/* Listeners for interactivity */
		addMouseListener(new MatrixMouseListener());
		addMouseMotionListener(new MatrixMouseListener());
		addMouseWheelListener(this);
	}

	/**
	 * 
	 * @return x-axis scroll bar (horizontal) for the InteractiveMatrixView.
	 */
	public JScrollBar getXMapScroll() {

		return scrollPane.getHorizontalScrollBar();
	}

	/**
	 * 
	 * @return y-axis scroll bar (vertical) for the InteractiveMatrixView.
	 */
	public JScrollBar getYMapScroll() {

		return scrollPane.getVerticalScrollBar();
	}

	public void setGlobalMatrixView(GlobalMatrixView gmv) {
		globalMatrixView = gmv;
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

		if(xmap.hoverChanged() || ymap.hoverChanged()) {
			xmap.unsetHoverChanged();
			ymap.unsetHoverChanged();
			return;
		}
		Graphics2D g2d = (Graphics2D) g;

		revalidateScreen();

		if (!offscreenValid) {
			// clear the pallette...
			g2d.setColor(GUIFactory.DEFAULT_BG);
			g2d.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
			g2d.setColor(Color.black);

			final Rectangle destRect = new Rectangle(0, 0,
					xmap.getUsedPixels(), ymap.getUsedPixels());

			final Rectangle sourceRect = new Rectangle(xmap.getIndex(0),
					ymap.getIndex(0), xmap.getIndex(destRect.width)
							- xmap.getIndex(0), ymap.getIndex(destRect.height)
							- ymap.getIndex(0));

			if ((sourceRect.x >= 0) && (sourceRect.y >= 0) && drawer != null) {
				drawer.paint(g2d, sourceRect, destRect, null);
			}
		}
	}

	@Override
	public synchronized void paintComposite(final Graphics g) {

		if(xmap.hoverChanged() || ymap.hoverChanged()) {
			xmap.unsetHoverChanged();
			ymap.unsetHoverChanged();
			return;
		}
		Graphics2D g2 = (Graphics2D) g;

		if (selectionRectList != null) {
			//Reinitialize the graphics object
			g2 = (Graphics2D) g;

			/* draw all selection rectangles in yellow */
			g2.setColor(Color.yellow);

			for (final Rectangle rect : selectionRectList) {
				g2.drawRect(rect.x, rect.y, rect.width, rect.height);
			}

			//debug("Preparing to draw ellipses.");
			/*
			 * draw white selection circle if only 1 tile is selected and small
			 * enough.
			 */
			if (indicatorCircleList != null) {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(Color.yellow);
				g2.setStroke(new BasicStroke(3));
				for(Ellipse2D.Double indicatorCircle : indicatorCircleList) {
					//debug("Drawing ellipse.");
					g2.draw(indicatorCircle);
				}
			}
		}
	}

	/**
	 * Checks the selection of genes and arrays and calculates the appropriate
	 * selection rectangle.
	 */
	@Override
	protected void recalculateOverlay() {

		if(xmap.hoverChanged() || ymap.hoverChanged()) {
			xmap.unsetHoverChanged();
			ymap.unsetHoverChanged();
			return;
		}

		if((geneSelection == null) || (arraySelection == null)) {
			selectionRectList   = null;
			indicatorCircleList = null;
		} else {

			selectionRectList = new ArrayList<Rectangle>();
	
			final int[] selectedArrayIndexes =
				arraySelection.getSelectedIndexes();
			final int[] selectedGeneIndexes =
				geneSelection.getSelectedIndexes();
	
			globalMatrixView.setIMVselectedIndexes(selectedArrayIndexes,
												   selectedGeneIndexes);
	
			if (selectedArrayIndexes.length > 0) {
	
				// debug("Selected min array index: [" +
				// selectedArrayIndexes[0] + "] Selected min gene index: [" +
				// selectedGeneIndexes[0] + "].");
	
				List<List<Integer>> arrayBoundaryList;
				List<List<Integer>> geneBoundaryList;
	
				arrayBoundaryList =
					findRectangleBoundaries(selectedArrayIndexes,xmap);
				geneBoundaryList =
					findRectangleBoundaries(selectedGeneIndexes,ymap);
	
				// Make the rectangles
				if (selectionRectList != null) {
					for(final List<Integer> xBoundaries : arrayBoundaryList) {
	
						for(final List<Integer> yBoundaries : geneBoundaryList){
	
							selectionRectList
								.add(new Rectangle(xBoundaries.get(0),
									yBoundaries.get(0), xBoundaries.get(1)
									- xBoundaries.get(0), yBoundaries
									.get(1) - yBoundaries.get(0)));
						}
					}
				}
			}
			
			setIndicatorCircleBounds();
		}
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

				if (rectangleRange.size() == 0 ||
					rectangleRange.get(rectangleRange.size() - 1) ==
					current - 1) {
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
			ep = map.getPixel(selectionRange.get(selectionRange.size() - 1) + 1);
			// removed + 1 here due to new image drawing (otherwise selection was not accurate by 1 pixel)

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

		if(xmap.hoverChanged() || ymap.hoverChanged()) {
			xmap.unsetHoverChanged();
			ymap.unsetHoverChanged();
			return;
		}
		if (o == geneSelection) {
			updateSelection(geneSelection, arraySelection);
			recalculateOverlay();

			/* trigger updatePixels() */
			offscreenValid = false;
			repaint();

		} else if (o == arraySelection) {
			updateSelection(arraySelection, geneSelection);
			recalculateOverlay();

			offscreenValid = false;
			repaint();

		} else// if(!xmap.hoverChanged() && !ymap.hoverChanged()) {
			super.update(o, arg);
//		} else {
//			if(xmap.hoverChanged()) {
//				xmap.unsetHoverChanged();
//			}
//			if(ymap.hoverChanged()) {
//				ymap.unsetHoverChanged();
//			}
//		}
	}

	/**
	 * Adjusts one axis selection to changes in the other.
	 * 
	 * @param origin
	 *            The axis selection object that has previously been changed.
	 * @param adjusting
	 *            The axis selection object that needs to be adjusted.
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

		boolean isMousePressed;
		
		@Override
		public void mouseMoved(final MouseEvent e) {
			
			debug("mouseMoved inside IMV",9);
			
			xmap.setHoverIndex(xmap.getIndex(e.getX()));
			ymap.setHoverIndex(ymap.getIndex(e.getY()));
		}

		@Override
		public void mouseDragged(final MouseEvent e) {

			// TODO dont draw when out of focus... doesnt recognize isMousePressed...
			if (!enclosingWindow().isActive()) {
				return;
			}
			
			// When left button is used
			if (SwingUtilities.isLeftMouseButton(e)) {
				debug("Mouse dragged. Updating hover indexes to [" +
				      xmap.getIndex(e.getX()) + "x" + ymap.getIndex(e.getY()) +
				      "]",4);
				xmap.setHoverIndex(xmap.getIndex(e.getX()));
				ymap.setHoverIndex(ymap.getIndex(e.getY()));

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

			if (!enclosingWindow().isActive() || !isMousePressed) {
				return;
			}

			// When left button is used
			if (SwingUtilities.isLeftMouseButton(e)) {
				debug("Mouse released. No longer selecting",4);
				xmap.setSelecting(false);
				ymap.setSelecting(false);
				xmap.setSelectingStart(-1);
				ymap.setSelectingStart(-1);
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
			
			isMousePressed = false;
			repaint();
		}

		// Mouse Listener
		@Override
		public void mousePressed(final MouseEvent e) {

			if (!enclosingWindow().isActive()) {
				return;
			}

			isMousePressed = true;
			
			// if left button is used
			if (SwingUtilities.isLeftMouseButton(e)) {
				debug("Starting selecting, setting selection start to [" +
				      xmap.getIndex(e.getX()) + "x" + ymap.getIndex(e.getY()) +
				      "]",4);
				xmap.setSelecting(true);
				ymap.setSelecting(true);
				xmap.setSelectingStart(xmap.getIndex(e.getX()));
				ymap.setSelectingStart(ymap.getIndex(e.getY()));

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

				//debug("Deselecting.");

				geneSelection.notifyObservers();
				arraySelection.notifyObservers();
			}

			globalMatrixView.setIMVselectedIndexes(
					arraySelection.getSelectedIndexes(),
					geneSelection.getSelectedIndexes());
		}

		//Timer to let the label pane linger a bit (prevents flashing when
		//passing between panes which do not change the visibility of the label
		//panes)
		final private int delay = 250;
		private javax.swing.Timer turnOffLabelPortTimer;
		ActionListener turnOffLabelPort = new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent evt) {
				if (evt.getSource() == turnOffLabelPortTimer) {
					/* Stop timer */
					turnOffLabelPortTimer.stop();
					turnOffLabelPortTimer = null;
				
					debug("mouseEvent in IMV - ACTED ON",1);
					xmap.setOverInteractiveMatrix(false);
					ymap.setOverInteractiveMatrix(false);
					xmap.notifyObservers();
					ymap.notifyObservers();
					revalidate();
					repaint();
				}
			}
		};

		@Override
		public void mouseEntered(final MouseEvent e) {
			debug("mouseEntered IMV",9);
			if(this.turnOffLabelPortTimer != null) {
				/* Event came too soon, swallow it by resetting the timer.. */
				this.turnOffLabelPortTimer.stop();
				this.turnOffLabelPortTimer = null;
			}
			xmap.setOverInteractiveMatrix(true);
			ymap.setOverInteractiveMatrix(true);
			if (!enclosingWindow().isActive()) {
				return;
			}
			
			hasMouse = true;
			requestFocus();
		}

		@Override
		public void mouseExited(final MouseEvent e) {

			debug("mouseExited IMV",1);
			//Turn off the "over a label port view" boolean after a bit
			if(this.turnOffLabelPortTimer == null) {
				debug("mouseExited IMV - starting timer",1);
				/* Start waiting for delay millis to elapse and then
				 * call actionPerformed of the ActionListener
				 * "turnOffLabelPort". */
				if(delay == 0) {
					debug("mouseEvent in IMV - ACTED ON",1);
					xmap.setOverInteractiveMatrix(false);
					ymap.setOverInteractiveMatrix(false);
					xmap.notifyObservers();
					ymap.notifyObservers();
					revalidate();
					repaint();
				} else {
					this.turnOffLabelPortTimer = new Timer(this.delay,
							turnOffLabelPort);
					this.turnOffLabelPortTimer.start();
				}
			}

			//setOverInteractiveMatrix(false);
			hasMouse = false;

			xmap.setHoverIndex(-1);
			ymap.setHoverIndex(-1);
		}
	}

	public void startDragRect(int xIndex,int yIndex) {
		startPoint.setLocation(xIndex,yIndex);
		endPoint.setLocation(startPoint.x,startPoint.y);
		dragRect.setLocation(startPoint.x,startPoint.y);
		dragRect.setSize(endPoint.x - dragRect.x,endPoint.y - dragRect.y);
		drawBand(dragRect);
	}

	public void updateDragRect(int xIndex,int yIndex) {
		drawBand(dragRect);
		endPoint.setLocation(xIndex,yIndex);
		dragRect.setLocation(startPoint.x, startPoint.y);
		dragRect.setSize(0, 0);
		dragRect.add(endPoint.x, endPoint.y);
	}

	public void endDragRect(int xIndex,int yIndex) {
		updateDragRect(xIndex,yIndex);
		drawBand(dragRect);
	}

	/**
	 * Zooming when the mouse wheel is used in conjunction with the alt/option
	 * key.  Vertical scrolling if the shift key is not pressed.
	 */
	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {

		final int notches = e.getWheelRotation();
		final int shift = (notches < 0) ? -3 : 3;

		//On macs' magic mouse, horizontal scroll comes in as if the shift was
		//down
		if (e.isAltDown()) {
			if (notches < 0) {
				//This ensures we only zoom toward the cursor when the cursor is
				//over the map
				if (hasMouse) {
					smoothZoomTowardPixel(e.getX(),e.getY());
				}
				//This should happen when the mouse is not over the heatmap
				else {
					xmap.zoomInBegin();
					ymap.zoomInBegin();
				}
			} else {
				if (hasMouse) {
					smoothZoomFromPixel(e.getX(),e.getY());
				} else {
					xmap.zoomOutBegin();
					ymap.zoomOutBegin();
				}
			}
		} else if (e.isShiftDown()) {
			xmap.scrollBy(shift,false);
			//Now we are hovered over a new index
			xmap.setHoverIndex(xmap.getIndex(e.getX()));
		} else {
			ymap.scrollBy(shift,false);
			//Now we are hovered over a new index
			ymap.setHoverIndex(ymap.getIndex(e.getY()));
		}

		repaint();
	}

	/**
	 * This function, given a pixel location (e.g. where the cursor is), will
	 * intelligently choose a zoom amount to given the appearance of a smooth
	 * zoom that expands from the given pixel
	 * 
	 * @param xPxPos
	 * @param yPxPos
	 */
	public void smoothZoomTowardPixel(int xPxPos,int yPxPos) {
		int zoomXVal = 0;
		int zoomYVal = 0;
		int numXCells = xmap.getNumVisible();
		int numYCells = ymap.getNumVisible();

		//If the current aspect ratio has not been set, set it
		if(aspectRatio < 0) {
			updateAspectRatio();
		}

		//This is the amount by which we incrementally zoom in.  It is hard-
		//coded here in addition to inside MapContainer because we need to
		//adjust the value based on aspect ratio
		double targetZoomFrac = 0.05;

		//This will tell us how out of sync the aspect ratio has gotten from the
		//smooth-zooming trick and how many cells on the x axis we must add or
		//remove to correct the aspect ratio (to what it was before we started
		//zooming)
		double numXCellsShouldHave = aspectRatio * numYCells;

		//Could check for possible div by zero here, but should be impossible
		double targetZoomFracCorrection =
				Math.abs(numXCellsShouldHave - numXCells) /
				numXCells;

		//If numXCellsShouldHave is basically an integer and equals numXCells,
		//we will ensure there is a zoom on both axes (this really is only good
		//for equal numbers of visible cells on both axes -possibly unnecessary)
		if((numXCellsShouldHave % 1) == 0 &&
				((int) Math.round(numXCellsShouldHave)) == numXCells) {
			zoomXVal = xmap.getBestZoomInVal(xPxPos,targetZoomFrac);
			zoomYVal = ymap.getBestZoomInVal(yPxPos,targetZoomFrac);
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
			//If the X axis should have more cells on it, so zoom in on the y
			//axis more (resulting in fewer rows) to relatively make the x axis
			//have more cells in comparison.
			//The fraction calculation is different for the Y axis.  The
			//following calculation is based on the merging of a few equations:
			//current aspectRatio           = numXCells/numYCells
			//numXCellsShouldHave           = aspectRatio*numYCells                                     //If we decide to make Y "correct", we can calculate what X should be
			//numXCellsShouldHave/numYCells = numXCells/(numYCells+_numYCellsShouldHave_) = aspectRatio //We do not know _numYCellsShouldHave_, solving for it, we have:
			//_numYCellsShouldHave_         = numXCells*numYCells/numXCellsShouldHave-numYCells         //Then, canceling out numYCells, we have:
			//_numYCellsShouldHave_         = numXCells/numXCellsShouldHave-1                           //_numYCellsShouldHave_ will be a ratio, i.e. the same as targetZoomFracCorrection
			targetZoomFracCorrection =
					Math.abs(numXCells / numXCellsShouldHave - 1);
			zoomXVal = xmap.getBestZoomInVal(xPxPos,targetZoomFrac);
			zoomYVal = ymap.getBestZoomInVal(yPxPos,
					targetZoomFrac + targetZoomFracCorrection);
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in the Y dimension
				if(numYCells > 1) {
					zoomYVal = 1;
				} else if(numXCells > 1) {
					zoomXVal = 1;
				}
			}
		} else {
			zoomXVal = xmap.getBestZoomInVal(xPxPos,
					targetZoomFrac + targetZoomFracCorrection);
			zoomYVal = ymap.getBestZoomInVal(yPxPos,targetZoomFrac);
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

		if(xmap.zoomTowardPixel(xPxPos, zoomXVal) == 1)
			updateAspectRatio();
		if(ymap.zoomTowardPixel(yPxPos, zoomYVal) == 1)
			updateAspectRatio();
	}

	/**
	 * This function, given a pixel location (e.g. where the cursor is), will
	 * intelligently choose a zoom amount to given the appearance of a smooth
	 * zoom that contracts on the given pixel
	 * 
	 * @param xPxPos
	 * @param yPxPos
	 */
	public void smoothZoomFromPixel(int xPxPos,int yPxPos) {

		int zoomXVal = 0;
		int zoomYVal = 0;
		int numXCells = xmap.getNumVisible();
		int numYCells = ymap.getNumVisible();

		//If the current aspect ratio has not been set, set it
		if(aspectRatio < 0) {
			updateAspectRatio();
		}

		//This is the amount by which we incrementally zoom out.  It is hard-
		//coded here in addition to inside MapContainer because we need to
		//adjust the value based on aspect ratio
		double targetZoomFrac = 0.05;

		//This will tell us how out of sync the aspect ratio has gotten from the
		//smooth-zooming trick and how many cells on the x axis we must add or
		//remove to correct the aspect ratio (to what it was before we started
		//zooming)
		double numXCellsShouldHave = aspectRatio * numYCells;

		//Could check for possible div by zero here, but should be impossible
		double targetZoomFracCorrection = Math.abs(numXCellsShouldHave -
				numXCells) / numXCells;

		//If numXCellsShouldHave is basically an integer and equals numXCells,
		//we will ensure there is a zoom on both axes (this really is only good
		//for equal numbers of visible cells on both axes -possibly unnecessary)
		if((numXCellsShouldHave % 1) == 0 &&
				((int) Math.round(numXCellsShouldHave)) == numXCells) {
			zoomXVal = xmap.getBestZoomOutVal(xPxPos,targetZoomFrac);
			zoomYVal = ymap.getBestZoomOutVal(yPxPos,targetZoomFrac);
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in both dimensions, try to zoom out 2,
				//then 1 : Probably unnecessary
				if(numYCells < ymap.getMaxIndex() &&
						numXCells < xmap.getMaxIndex()) {
					zoomXVal = 2;
					zoomYVal = 2;
				} else if(numYCells < ymap.getMaxIndex()) {
					zoomYVal = 2;
				} else if(numXCells < xmap.getMaxIndex()) {
					zoomXVal = 2;
				} else if(numYCells < (ymap.getMaxIndex() + 1) &&
						numXCells < (xmap.getMaxIndex() + 1)) {
					zoomXVal = 1;
					zoomYVal = 1;
				} else if(numYCells < (ymap.getMaxIndex() + 1)) {
					zoomYVal = 1;
				} else if(numXCells < (xmap.getMaxIndex() + 1)) {
					zoomXVal = 1;
				}
			}
		} else if(numXCellsShouldHave > numXCells) {
			//If the X axis should have more cells on it, zoom out on the x axis
			//more
			zoomXVal = xmap.getBestZoomOutVal(xPxPos,
					targetZoomFrac + targetZoomFracCorrection);
			zoomYVal = ymap.getBestZoomOutVal(yPxPos,targetZoomFrac);
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom out in the X dimension
				if(numXCells < (xmap.getMaxIndex() + 1)) {
					zoomXVal = 1;
				} else if(numYCells < (ymap.getMaxIndex() + 1)) {
					zoomYVal = 1;
				}
			}
		} else {
			//Else, the X axis should have fewer cells on it, so zoom out on the
			//y axis more.
			//The fraction calculation is different for the Y axis.  The
			//following calculation is based on the merging of a few equations:
			//current aspectRatio           = numXCells/numYCells
			//numXCellsShouldHave           = aspectRatio*numYCells                                     //If we decide to make Y "correct", we can calculate what X should be
			//numXCellsShouldHave/numYCells = numXCells/(numYCells+_numYCellsShouldHave_) = aspectRatio //We do not know _numYCellsShouldHave_, solving for it, we have:
			//_numYCellsShouldHave_         = numXCells*numYCells/numXCellsShouldHave-numYCells         //Then, canceling out numYCells, we have:
			//_numYCellsShouldHave_         = numXCells/numXCellsShouldHave-1                           //_numYCellsShouldHave_ will be a ratio, i.e. the same as targetZoomFracCorrection
			targetZoomFracCorrection =
					Math.abs(numXCells / numXCellsShouldHave - 1);
			zoomXVal = xmap.getBestZoomOutVal(xPxPos,targetZoomFrac);
			zoomYVal = ymap.getBestZoomOutVal(yPxPos,
					targetZoomFrac + targetZoomFracCorrection);
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

		if(xmap.zoomAwayPixel(xPxPos, zoomXVal) == 1)
			updateAspectRatio();
		if(ymap.zoomAwayPixel(yPxPos, zoomYVal) == 1)
			updateAspectRatio();
	}

	/**
	 * This is a wrapper function for smoothZoomTowardSelection which allows one
	 * to call this function without providing any parameters. So it just fills
	 * in the target zoom level info.
	 */
	public void smoothIncrementalZoomOut() {
		//The function below has a bug that prevents full zoom out
		// - this works around that - though it may have already been fixed
		int prevXNumVisible = xmap.getNumVisible();
		int prevYNumVisible = ymap.getNumVisible();

		/*
		 * TODO: Fix the smoothZoomTowardSelection function below and then
		 * remove the work-around code - Rob
		 */
		smoothZoomTowardSelection(0,(xmap.getMaxIndex() + 1),
				0,(ymap.getMaxIndex() + 1));

		//The function above has a bug that prevents full zoom out sometimes
		// - this works around that, though it may have been fixed
		if(prevXNumVisible == xmap.getNumVisible() &&
		   prevYNumVisible == ymap.getNumVisible()) {

			//If you've never seen this message and you use the home button a
			//lot, you can probably remove this if-conditional and the prev*
			//variables
			//debug("smoothZoomTowardSelection bug still exists." +
			//					"  Working around it.");

			xmap.zoomOutCenter("fast");
			ymap.zoomOutCenter("fast");
		}

		//If we've reached full zoom-out, reset the aspect ratio
		if(xmap.getNumVisible() == (xmap.getMaxIndex() + 1) &&
				ymap.getNumVisible() == (ymap.getMaxIndex() + 1)) {
			setAspectRatio(xmap.getMaxIndex() + 1,ymap.getMaxIndex() + 1);
		}
	}

	/**
	 * This is just a wrapper for smoothAnimatedZoomTowardSelection which fills
	 * in the dimensions of the matrix
	 */
	public void smoothAnimatedZoomOut() {
		smoothAnimatedZoomTowardSelection(0,(xmap.getMaxIndex() + 1),
											0,(ymap.getMaxIndex() + 1));
	}

	/**
	 * This is a wrapper for smoothZoomTowardSelection which calls it repeatedly
	 * until a target location is reached to animate a full zoom to target
	 * 
	 * @param selecXStartIndex
	 * @param numXSelectedIndexes
	 * @param selecYStartIndex
	 * @param numYSelectedIndexes
	 */
	public void smoothAnimatedZoomTowardSelection(int selecXStartIndex,
												  int numXSelectedIndexes,
												  int selecYStartIndex,
												  int numYSelectedIndexes) {
		long startTime = System.currentTimeMillis();

		//Zooming out is slower on large matrices because it just takes longer
		//to draw large amounts of cells, so our cutoff wait time should be
		//smaller if we are zooming out
		int doneWaitingMillis = 500;
//		int selectionSizeCutoff = 500;
//		if((numXSelectedIndexes > selectionSizeCutoff &&
//				xmap.getNumVisible() < numXSelectedIndexes) ||
//				(numYSelectedIndexes > selectionSizeCutoff &&
//						ymap.getNumVisible() < numYSelectedIndexes)) {
//			doneWaitingMillis = 200;
//		}

		//Let's calculate the relative position of the center of the selection
		//and gradually zoom toward that spot in a loop
		while(xmap.getNumVisible() != numXSelectedIndexes ||
			  ymap.getNumVisible() != numYSelectedIndexes) {

			//If drawing the zoom increment levels is taking too long, snap out
			//of it.
			if((System.currentTimeMillis() - startTime) > doneWaitingMillis)
				break;

			smoothZoomTowardSelection(selecXStartIndex,
									  numXSelectedIndexes,
									  selecYStartIndex,
									  numYSelectedIndexes);

			//debug("numVisible versus num selected: [x" +
			//				  xmap.getNumVisible() + ":" + numXSelectedIndexes +
			//				  ",y" + ymap.getNumVisible() + ":" +
			//				  numYSelectedIndexes + "].");

			//Force an immediate repaint.  Found this in a thread here:
			//https://community.oracle.com/thread/1663771
			paintImmediately(0,0,getWidth(),getHeight());

			//Sleep a few milliseconds
			try {
				// Use this to debug, by slowing the zoom down to see what's
				// happening at each step
				// Thread.sleep(500);
				Thread.sleep(10);
			} catch(InterruptedException e) {
				LogBuffer.println("Error: Couldn't sleep.");
				e.printStackTrace();
			}
		}

		//Once the loop is done, we may not have gotten to the correct zoom
		//level or scroll position, so we should do a zoom & scroll here just to
		//be certain. Scroll could be off because of the way it is separated
		//from the zoom, so we will always do that
		if(xmap.getNumVisible() != numXSelectedIndexes) {
			//debug("Adjusting final X zoom");
			xmap.zoomToSelected(selecXStartIndex,
					(selecXStartIndex + numXSelectedIndexes - 1));
		}
		if(ymap.getNumVisible() != numYSelectedIndexes) {
			//debug("Adjusting final Y zoom");
			ymap.zoomToSelected(selecYStartIndex,
					(selecYStartIndex + numYSelectedIndexes - 1));
		}
		if(xmap.getFirstVisible() != selecXStartIndex) {
			//debug("Adjusting final X scroll");
			xmap.scrollToFirstIndex(selecXStartIndex);
		}
		if(ymap.getFirstVisible() != selecYStartIndex) {
			//debug("Adjusting final Y scroll");
			ymap.scrollToFirstIndex(selecYStartIndex);
		}

		//We will update the aspect ratio just in case it didn't happen
		//automatically
		updateAspectRatio();
	}

	/*
	 * TODO: Simplify/streamline this function, move a bunch of steps to
	 * independent functions in MapContainer, and make it able to handle pixel
	 * positions that are out of view (xPxPos and yPxPos) - Rob
	 */
	/*
	 * TODO: There appears to be a minor bug when zooming out.  The final zoom
	 * steps tends to be very very small, perhaps a single row/col.  That can't
	 * be coincidence. - Rob
	 */
	/**
	 * Essentially, all this function does is call getZoomTowardPixelOfSelection
	 * to determine the pixel in the selection to zoom toward (so that the
	 * selection ends up filling the view) and then uses that pixel to call
	 * zoomTowardPixel.
	 */
	public int[] smoothZoomTowardSelection(int selecXStartIndex,
										   int numXSelectedIndexes,
										   int selecYStartIndex,
										   int numYSelectedIndexes) {
		
		//Find the pixel inside the selected area to "zoom toward" such that the
		//selected area essentially expands to fill the screen
		int startXPixel = xmap.getPixel(selecXStartIndex);
		double pixelsPerXIndex = xmap.getScale();
		int numSelectedXPixels = (int) Math.round(numXSelectedIndexes *
				pixelsPerXIndex);
		int xPxPos = xmap.getZoomTowardPixelOfSelection(startXPixel,
														numSelectedXPixels);
		//debug("smoothZoomTowardSelection: Starting X index " +
		//"sent in: [" + selecXStartIndex + "] and pixel index obtained for " +
		//"that data index: [" + startXPixel + "] and then pixel selected to " +
		//"zoom toward is: [" + xPxPos + "].");
		int startYPixel = ymap.getPixel(selecYStartIndex);
		double pixelsPerYIndex = ymap.getScale();
		int numSelectedYPixels = (int) Math.round(numYSelectedIndexes *
				pixelsPerYIndex);
		int yPxPos = ymap.getZoomTowardPixelOfSelection(startYPixel,
														numSelectedYPixels);
		//debug("smoothZoomTowardSelection: Starting Y index " +
		//"sent in: [" + selecYStartIndex + "] and pixel index obtained for " +
		//"that data index: [" + startYPixel + "] and then pixel selected to " +
		//"zoom toward is: [" + yPxPos + "].");

		//debug("Going to zoom toward pixel at: [x" + xPxPos +
		//		",y" + yPxPos + "] determined from data cell at: [x" +
		//		selecXStartIndex + ",y" + selecYStartIndex +
		//		"].  Selected pixel start/number: [x" + startXPixel + "/" +
		//		numSelectedXPixels + ",y" + startYPixel + "/" +
		//		numSelectedYPixels + "].");
		
		//The final aspect ratio of the selected area will be our target aspect
		//ratio (used for smoothness corrections)
		double targetAspectRatio =
				(double) numXSelectedIndexes / (double) numYSelectedIndexes;
		
		int zoomXVal          = 0;
		int zoomYVal          = 0;
		int numXCells         = xmap.getNumVisible();
		int numYCells         = ymap.getNumVisible();
		int xPxNum            = xmap.getAvailablePixels();
		int yPxNum            = ymap.getAvailablePixels();
		int prevXFirstVisible = xmap.getFirstVisible();
		int prevXNumVisible   = xmap.getNumVisible();
		int centerXIndex      = xmap.getFirstVisible() +
				(int) Math.floor(xmap.getNumVisible() / 2.0);
		int prevYFirstVisible = ymap.getFirstVisible();
		int prevYNumVisible   = ymap.getNumVisible();
		int centerYIndex      = ymap.getFirstVisible() +
				(int) Math.floor(ymap.getNumVisible() / 2.0);

		//Since it takes a lot of time to draw large matrices, select a zoom
		//fraction based on the size of the matrix we are going to draw at each
		//increment so that we skip increments that take too much time to draw
		double targetZoomFracX =
				xmap.getOptimalZoomIncrement(numXSelectedIndexes,
						(prevXNumVisible < numXSelectedIndexes));
		double targetZoomFracY =
				ymap.getOptimalZoomIncrement(numYSelectedIndexes,
						(prevYNumVisible < numYSelectedIndexes));
		//Select the larger zoom increment from the 2 dimensions
		double targetZoomFrac = targetZoomFracX;
		if(targetZoomFrac < targetZoomFracY)
			targetZoomFrac = targetZoomFracY;

		//debug("targetZoomFrac: [" + targetZoomFrac + "].");

		//If the starting aspect ratio is different from the target aspect
		//ratio, we don't want to snap to that ratio on the first step - we want
		//to get there gradually, so we'll adjust based on the
		//targetZoomFraction.  Matching that value doesn't seem to be very
		//effective, so as long as the targetZoomFraction is less than 33%,
		//we'll multiply the value by 3.  That seems to end up with the gradual
		//changing of the aspect ratio to look much more appealing and natural.
		//We might end up reaching the target aspect ratio before or after we've
		//reached the target zoom level, so there's probably a better way, but
		//this works OK.
		double aspectRatioFracCorrection = targetZoomFrac;
			//1 - targetZoomFrac;
			//(targetZoomFrac < 0.33 ? targetZoomFrac * 3.0 : targetZoomFrac);
				
		//debug("Zoom fraction: [" + targetZoomFrac +
		//		"] XtargetFrac: [" + targetZoomFracX + "] YtargetFrac: [" +
		//		targetZoomFracY + "] Aspect Ratio fraction correction: [" +
		//		aspectRatioFracCorrection + "].");

		//This will tell us how out of sync the aspect ratio has gotten from the
		//smooth-zooming trick and how many cells on the x axis we must add or
		//remove to correct the aspect ratio (to what it was before we started
		//zooming)
		double numXCellsShouldHave = targetAspectRatio * numYCells;

		//debug("numXCellsShouldHave before fractioning the " +
		//"targetAspectRatio: [" + numXCellsShouldHave + "].");

		// To cause the targetAspectRatio to be arrived at gradually, let's
		// only correct by a percentage of the correction:
		numXCellsShouldHave = numXCells + (numXCellsShouldHave - numXCells)
				* aspectRatioFracCorrection;

		//debug("numXCellsShouldHave after fractioning the " +
		//"targetAspectRatio: [" + numXCellsShouldHave + "].");

		double targetZoomFracCorrection = 0.0;

		//If numXCellsShouldHave is basically an integer and is equal to
		//numXCells, we are at the exact aspect ratio and no correction to it is
		//necessary
		if((numXCellsShouldHave % 1) == 0 &&
				((int) Math.round(numXCellsShouldHave)) == numXCells) {

			//If we're zooming in on the X axis
			if(numXCells >= numXSelectedIndexes) {
				zoomXVal = xmap.getBestZoomInVal(xPxPos,targetZoomFrac);
			} else {
				zoomXVal = xmap.getBestZoomOutVal(xPxPos,targetZoomFrac);
			}
			// If we're zooming in on the Y axis
			if (numYCells >= numYSelectedIndexes) {
				zoomYVal = ymap.getBestZoomInVal(yPxPos,targetZoomFrac);
			} else {
				zoomYVal = ymap.getBestZoomOutVal(yPxPos,targetZoomFrac);
			}

			//debug("Jumping to [" + numXCellsShouldHave +
			//"] because it's an integer equal to the target number of cells.");
			//debug("Zooming by [x" + zoomXVal + ",y" + zoomYVal +
			//"] target aspect ratio: [" + targetAspectRatio +
			//"]. Target zoom frac: [" + targetZoomFrac + "]");
			
			//If no zoom has occurred (due to selection of 0 due to smoothing -
			//a possibility that helps to allow aspect ratio changes, but is not
			//relevant here) - force a zoom to happen
			if(zoomXVal == 0 && zoomYVal == 0) {

				//If there's room to zoom 2 in both dimensions
				if(((numXSelectedIndexes <= xmap.getNumVisible() &&
					 numXCells > 2) ||
					(numXSelectedIndexes > xmap.getNumVisible() &&
					 numXCells < (xmap.getMaxIndex() + 2))) &&
					((numYSelectedIndexes <= ymap.getNumVisible() &&
					  numYCells > 2) ||
					 (numYSelectedIndexes > ymap.getNumVisible() &&
					  numYCells < (ymap.getMaxIndex() + 2)))) {
					zoomXVal = 2;
					zoomYVal = 2;
				} else if((numXSelectedIndexes <= xmap.getNumVisible() &&
						   numXCells > 1) ||
						  (numXSelectedIndexes > xmap.getNumVisible() &&
						   numXCells < (xmap.getMaxIndex() + 1))) {
					zoomXVal = 1;
				} else if((numYSelectedIndexes <= ymap.getNumVisible() &&
						   numYCells > 1) ||
						  (numYSelectedIndexes > ymap.getNumVisible() &&
						   numYCells < (ymap.getMaxIndex() + 1))) {
					zoomYVal = 1;
				}
			}
		}
		//Else if we should have more X cells and we are zooming in on the Y
		//axis OR we should have fewer X cells and we are zooming out on the Y
		//axis
		else if((numXCellsShouldHave > numXCells &&
				 numYCells > numYSelectedIndexes) ||
				(numXCellsShouldHave < numXCells &&
				 numYCells < numYSelectedIndexes)) {

			double numYCellsShouldHave = numXCells / targetAspectRatio;

			//debug("Actual number of cells that should be on " +
			//"the Y axis: [" + numYCellsShouldHave + "]");
			//if(numYCellsShouldHave < numYSelectedIndexes) {
			//	numYCellsShouldHave = numYSelectedIndexes;
			//} else if(numYCellsShouldHave > (ymap.getMaxIndex() + 1)) {
			//	numYCellsShouldHave = (ymap.getMaxIndex() + 1);
			//}

			//debug("Zooming in on Y axis more: " +
			//"numYCellsShouldHave = numYCells - Math.abs(numYCellsShouldHave" +
			//" - numYCells) * aspectRatioFracCorrection");
			//debug("Zooming in on Y axis more: " +
			//"numYCellsShouldHave = " + numYCells + " - Math.abs(" +
			//numYCellsShouldHave + " - " + numYCells + ") * " +
			//aspectRatioFracCorrection);

			//To cause the targetAspectRatio to be arrived at gradually, let's
			//only correct by a percentage of the correction:
			numYCellsShouldHave = numYCells +
					(numYCellsShouldHave -
							numYCells) * aspectRatioFracCorrection;
			
			//Could check for possible div by zero here, but should be
			//impossible
			targetZoomFracCorrection =
					Math.abs(numYCellsShouldHave -
							 numYCells) / numYCells;
			if(targetZoomFracCorrection > 1.0) {
				targetZoomFracCorrection =
						Math.abs(numYCellsShouldHave - numYCells) /
						numYCellsShouldHave;
			}
			//debug("Zooming in on Y axis more: " +
			//"targetZoomFracCorrection = Math.abs(numYCellsShouldHave - " +
			//"numYCells) / numYCells");
			//debug("Zooming in on Y axis more: " +
			//targetZoomFracCorrection + " = Math.abs(" + numYCellsShouldHave +
			//" - " + numYCells + ") / " + numYCells);
			
			//If the X axis should have more cells on it, so zoom in on the y
			//axis more (resulting in fewer rows) to relatively make the x axis
			//have more cells in comparison
			//The fraction calculation is different for the Y axis.  The
			//following calculation is based on the merging of a few equations:
			//current aspectRatio           = numXCells/numYCells
			//numXCellsShouldHave           = aspectRatio*numYCells                                     //If we decide to make Y "correct", we can calculate what X should be
			//numXCellsShouldHave/numYCells = numXCells/(numYCells+_numYCellsShouldHave_) = aspectRatio //We do not know _numYCellsShouldHave_, solving for it, we have:
			//_numYCellsShouldHave_         = numXCells*numYCells/numXCellsShouldHave-numYCells         //Then, canceling out numYCells, we have:
			//_numYCellsShouldHave_         = numXCells/numXCellsShouldHave-1                           //_numYCellsShouldHave_ will be a ratio, i.e. the same as targetZoomFracCorrection
			////////targetZoomFracCorrection = Math.abs((double) numXCells / numXCellsShouldHave - 1);
			if(numXCells >= numXSelectedIndexes) {
				zoomXVal = xmap.getBestZoomInVal(xPxPos,targetZoomFrac);
			} else {
				//debug("Zooming out on the X axis 1.");
				zoomXVal = xmap.getBestZoomOutVal(xPxPos,targetZoomFrac);
			}
			if(numYCells >= numYSelectedIndexes) {
				zoomYVal = ymap.getBestZoomInVal(yPxPos,
						targetZoomFrac +
						(1 - targetZoomFrac) * targetZoomFracCorrection);
			} else {
				//debug("Zooming out on the Y axis 1.");
				zoomYVal = ymap.getBestZoomOutVal(yPxPos,
						targetZoomFrac +
						(1 - targetZoomFrac) * targetZoomFracCorrection);
			}

			//debug("targetZoomFracCorrection: [" +
			//		targetZoomFracCorrection + "].  Resulting zoomXVal: [" +
			//		zoomXVal + "].");

			//debug("Zooming more on the Y axis because the " +
			//"number of X cells we should have [" + numXCellsShouldHave +
			//"] is greater than the current number of cells [" + numXCells +
			//"] and we are zooming in on the Y axis or the number of X " +
			//"cells we should have is fewer than the current number of " +
			//"cells, so we calculated this many Y cells we should have " +
			//"given a gradual aspect ratio change: [" + numYCellsShouldHave +
			//"].");

			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in the X dimension
				if((numXSelectedIndexes <= xmap.getNumVisible() &&
					numXCells > 1) ||
				   (numXSelectedIndexes > xmap.getNumVisible() &&
					numXCells < (xmap.getMaxIndex() + 1))) {
					zoomXVal = 1;
				} else if((numYSelectedIndexes <= ymap.getNumVisible() &&
						   numYCells > 1) ||
						(numYSelectedIndexes > ymap.getNumVisible() &&
						 numYCells < (ymap.getMaxIndex() + 1))) {
					zoomYVal = 1;
				}
			}
		} else {
			//debug("Actual number of cells that should be on " +
			//"the X axis: [" + numXCellsShouldHave + "]");
			
			//Could check for possible div by zero here, but should be
			//impossible
			targetZoomFracCorrection =
					Math.abs(numXCellsShouldHave -
							 numXCells) / numXCells;
			if(targetZoomFracCorrection > 1.0) {
				targetZoomFracCorrection =
						Math.abs(numXCellsShouldHave - numXCells) /
						numXCellsShouldHave;
			}

			//This should theoretically be more accurate, but it is not for some
			//reason:
			//targetZoomFracCorrection =
			//		numXCellsShouldHave / (double) numXCells;

			if(numXCells >= numXSelectedIndexes) {
				//debug("Zooming in on the X axis.");
				zoomXVal = xmap.getBestZoomInVal(xPxPos,
						targetZoomFrac +
						(1 - targetZoomFrac) * targetZoomFracCorrection);
			} else {
				//debug("Zooming out on the X axis 2.");
				zoomXVal = xmap.getBestZoomOutVal(xPxPos,
						targetZoomFrac +
						(1 - targetZoomFrac) * targetZoomFracCorrection);
			}
			if(numYCells >= numYSelectedIndexes) {
				//debug("Zooming in on the Y axis.");
				zoomYVal = ymap.getBestZoomInVal(yPxPos,targetZoomFrac);
			} else {
				//debug("Zooming out on the Y axis 2.");
				zoomYVal = ymap.getBestZoomOutVal(yPxPos,targetZoomFrac);
			}

			//debug("targetZoomFracCorrection: [" +
			//		targetZoomFracCorrection + "].  Resulting zoomXVal: [" +
			//		zoomXVal + "].");

			//debug("Zooming more on the X axis because the " +
			//"number of X cells we should have [" + numXCellsShouldHave +
			//"] is less than or equal to the target number of X cells [" +
			//numXCells + "].");

			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in the X dimension
				if((numXSelectedIndexes <= xmap.getNumVisible() &&
					numXCells > 1) ||
				   (numXSelectedIndexes > xmap.getNumVisible() &&
					numXCells < (xmap.getMaxIndex() + 1))) {
					zoomXVal = 1;
				} else if((numYSelectedIndexes <= ymap.getNumVisible() &&
						   numYCells > 1) ||
						  (numYSelectedIndexes > ymap.getNumVisible() &&
						   numYCells < (ymap.getMaxIndex() + 1))) {
					zoomYVal = 1;
				}
			}
		}
		//debug("Aspect ratio.  Current: [" + numXCells + " / " +
		//numYCells + "] Target: [" + targetAspectRatio +
		//"].  targetZoomFracCorrection: [" + targetZoomFracCorrection +
		//"].  New zoomXVal: [" + zoomXVal + "] New zoomYVal: [" + zoomYVal +
		//"] numXCellsShouldHave: [" + numXCellsShouldHave + "].");

		//Double-check that a zoom will indeed happen - a fail-safe because
		//The above code does not guarantee a zoom in rare circumstances - a
		//side-effect of the way we're smoothing the zoom
		if(zoomXVal == 0 && zoomYVal == 0) {
			if(numXSelectedIndexes != numXCells) {
				zoomXVal = 1;
			}
			if(numYSelectedIndexes != numYCells) {
				zoomYVal = 1;
			}
		}

		//Zoom the x axis
		if(numXSelectedIndexes <= numXCells) {
			//Let's make sure that we're not going to zoom past our target area
			if((numXCells - zoomXVal) < numXSelectedIndexes) {
				zoomXVal = numXCells - numXSelectedIndexes;
			}

			//debug("Zooming in on the x axis");
			xmap.zoomTowardPixel(xPxPos,zoomXVal);
		} else {
			//Let's make sure that we're not going to zoom past our target area
			if((numXCells + zoomXVal) > numXSelectedIndexes) {
				zoomXVal = Math.abs(numXCells - numXSelectedIndexes);
			}

			//debug("Zooming out on the x axis with xPxPos [" +
			//xPxPos + "] and zoomXVal [" + zoomXVal + "].");
			xmap.zoomAwayPixel(xPxPos,zoomXVal);
		}

		//Zoom the y axis
		if(numYSelectedIndexes <= numYCells) {
			//Let's make sure that we're not going to zoom past our target area
			if((numYCells - zoomYVal) < numYSelectedIndexes) {
				zoomYVal = numYCells - numYSelectedIndexes;
			}

			//debug("Zooming in on the y axis");
			ymap.zoomTowardPixel(yPxPos,zoomYVal);
		}
		//Else we were supposed to be zooming out
		else {
			//Let's make sure that we're not going to zoom past our target area
			if((numYCells + zoomYVal) > numYSelectedIndexes) {
				zoomYVal = Math.abs(numYCells - numYSelectedIndexes);
			}

			//debug("Zooming out on the y axis");
			ymap.zoomAwayPixel(yPxPos,zoomYVal);
		}

		//debug("Should have zoomed by [x" + zoomXVal + ",y" +
		//		zoomYVal + "] from [" + prevXNumVisible + ":" +
		//		prevYNumVisible + "] to [" + xmap.getNumVisible() + ":" +
		//		ymap.getNumVisible() + "].");

		//Now that we have zoomed, we should scroll if the selection is off the
		//screen.
		//If zooming has finished, scroll all the way
		if(xmap.getNumVisible() == numXSelectedIndexes &&
				ymap.getNumVisible() == numYSelectedIndexes) {
			xmap.scrollToFirstIndex(selecXStartIndex);
			ymap.scrollToFirstIndex(selecYStartIndex);
		} else {
			//debug("Correcting the scroll because (xPxPos <= 0 " +
			//"|| yPxPos <= 0 || xPxPos >= (xPxNum - 1) || yPxPos >= (yPxNum " +
			//"- 1)) = (" + xPxPos + " <= 0 || " + yPxPos + " <= 0 || " +
			//xPxPos + " >= (" + xPxNum + " - 1) || " + yPxPos + " >= (" +
			//yPxNum + " - 1)).");

			//If a selected edge (toward which we are zooming) is outside the
			//previously visible corresponding boundary AND the selected area is
			//not surrounding the visible area AND the pixel we are zooming to
			//is on an edge, assume we need to scroll incrementally to bring the
			//pixel we are zooming to/from into view
			if((((//The target start is left of the view
				  selecXStartIndex < prevXFirstVisible ||
				  //The target stop is right of the view
				  (selecXStartIndex + numXSelectedIndexes - 1) >
				  (prevXFirstVisible + prevXNumVisible - 1)) &&
				//The view is not inside the target dimensions
				!(selecXStartIndex < prevXFirstVisible &&
				  (selecXStartIndex + numXSelectedIndexes - 1) >
				  (prevXFirstVisible + prevXNumVisible - 1)))) &&
			   //The zoom-toward-pixel reported is on an edge
			   (xPxPos <= 0 || xPxPos >= (xPxNum - 1))) {

				//Scroll toward by half of the same fraction as the zoom changed
				//We don't want to scroll all the way in 1 sudden jolt, so we'll
				//scroll to be closer to the center of the selection by (half)
				//the same fraction as the incremental zoom (unless we're at the
				//zoom level of the selection)
				double scrollFrac =  ((double) xmap.getNumVisible() /
						(double) numXSelectedIndexes);
				if(scrollFrac > 1) {
					scrollFrac =  (double) numXSelectedIndexes /
							(double) xmap.getNumVisible();
				}
				scrollFrac /= 2.0;

				int scrollDistDirX = (selecXStartIndex +
						(int) Math.floor(numXSelectedIndexes / 2)) -
						centerXIndex;

				//debug("ScrollX difference before fractioning: " +
				//scrollDistDirX + " = selecXStartIndex + " +
				//"numXSelectedIndexes / 2) - centerXIndex /// " +
				//selecXStartIndex + " + " + numXSelectedIndexes + " / 2) - " +
				//centerXIndex + ".");
				scrollDistDirX =
						(int) Math.round(scrollDistDirX * scrollFrac);
				//debug("ScrollX difference after fractioning: [" +
				//scrollDistDirX + "].");

				/*
				 * TODO: Refactor the scrolling so that these checks do not have
				 * to be done. - Rob
				 */
				//The following correction calculations are to prevent scrolling
				//a visible selected edge out of view.
				//The calculations are not perfect and are only necessary
				//because the zoomToward* functions only really work when the
				//selection is either completely in view or we're zoomed in
				//inside the selected area.
				int prevFarEdge = prevXFirstVisible + prevXNumVisible;
				int selecFarEdge = selecXStartIndex + numXSelectedIndexes;
				int newNearEdge = (selecXStartIndex +
						(int) Math.floor(numXSelectedIndexes / 2)) -
						scrollDistDirX -
						(int) Math.ceil((double) xmap.getNumVisible() / 2);
				int newFarEdge = (selecXStartIndex +
						(int) Math.floor(numXSelectedIndexes / 2)) -
						scrollDistDirX +
						(int) Math.ceil((double) xmap.getNumVisible() / 2);
				//Make sure we don't scroll an edge from either in view or
				//before the view to out of view/past the view
				if(selecFarEdge <= prevFarEdge && selecFarEdge > newFarEdge) {
					scrollDistDirX -= (selecFarEdge - newFarEdge) +
							(int) Math.round((1.0 - scrollFrac) *
									Math.abs((double) selecFarEdge -
											(double) prevFarEdge));
					//debug("ScrollX difference after correcting " +
					//"for far edge overscroll: [" + scrollDistDirX + "].");
					newNearEdge = (selecXStartIndex +
							(int) Math.floor(numXSelectedIndexes / 2)) -
							scrollDistDirX -
							(int) Math.ceil((double) xmap.getNumVisible() / 2);
					newFarEdge = (selecXStartIndex +
							(int) Math.floor(numXSelectedIndexes / 2)) -
							scrollDistDirX +
							(int) Math.ceil((double) xmap.getNumVisible() / 2);
					if(selecFarEdge > newFarEdge ||
					   (selecXStartIndex >= prevXFirstVisible &&
						selecXStartIndex < newNearEdge)) {
						scrollDistDirX = 0;
						//debug("ScrollX difference after " +
						//"correcting for far edge overscroll: [" +
						//scrollDistDirX + "].");
					}
				} else if(selecXStartIndex >= prevXFirstVisible &&
						  selecXStartIndex < newNearEdge) {
					scrollDistDirX += (newNearEdge - selecXStartIndex) +
							(int) Math.round((1.0 - scrollFrac) *
									((double) selecXStartIndex -
											(double) prevXFirstVisible));
					//debug("ScrollX difference after correcting " +
					//"for near edge overscroll: [" + scrollDistDirX + "].");
					newNearEdge = (selecXStartIndex +
							(int) Math.floor(numXSelectedIndexes / 2)) -
							scrollDistDirX -
							(int) Math.ceil((double) xmap.getNumVisible() / 2);
					newFarEdge = (selecXStartIndex +
							(int) Math.floor(numXSelectedIndexes / 2)) -
							scrollDistDirX +
							(int) Math.ceil((double) xmap.getNumVisible() / 2);
					if(selecXStartIndex < newNearEdge ||
					   (selecFarEdge <= prevFarEdge &&
					    selecFarEdge > newFarEdge)) {
						scrollDistDirX = 0;
						//debug("ScrollX difference after " +
						//"correcting for far edge overscroll: [" +
						//scrollDistDirX + "].");
					}
				}

				//debug("Correcting the X scroll because the " +
				//"pixel we are zooming to/from is on an edge.");
				//debug("(selecXStartIndex + numXSelectedIndexes " +
				//"/ 2) - scrollDistDirX");
				//debug(((selecXStartIndex + (int) Math.floor(" +
				//"numXSelectedIndexes / 2)) - scrollDistDirX) + " = (" +
				//selecXStartIndex + " + " + numXSelectedIndexes + " / 2) - " +
				//scrollDistDirX);
				xmap.scrollToIndex((selecXStartIndex +
						(int) Math.floor(numXSelectedIndexes / 2)) -
						scrollDistDirX);
			}

			//If a selected edge (toward which we are zooming) is outside the
			//previously visible corresponding boundary AND the selected area is
			//not surrounding the visible area AND the pixel we are zooming to
			//is on an edge, assume we need to scroll incrementally to bring the
			//pixel we are zooming to/from into view
			if((((selecYStartIndex < prevYFirstVisible ||
				  (selecYStartIndex + numYSelectedIndexes - 1) >
				  (prevYFirstVisible + prevYNumVisible - 1)) &&
				 !(selecYStartIndex < prevYFirstVisible &&
				   (selecYStartIndex + numYSelectedIndexes - 1) >
				   (prevYFirstVisible + prevYNumVisible - 1)))) &&
			   (yPxPos <= 0 || yPxPos >= (yPxNum - 1))) {

				//We don't want to scroll all the way in 1 jolt, so we'll scroll
				//to be closer to the center of the selection by this fraction
				//(unless we're at the zoom level of the selection)
				//Scroll toward by half of the same fraction as the zoom changed
				double scrollFrac = ((double) ymap.getNumVisible() /
						(double) numYSelectedIndexes);
				if(scrollFrac > 1) {
					scrollFrac =  (double) numYSelectedIndexes /
							(double) ymap.getNumVisible();
				}
				scrollFrac /= 2.0;

				int scrollDistDirY = (selecYStartIndex +
						(int) Math.floor(numYSelectedIndexes / 2)) -
						centerYIndex;
				//debug("ScrollY difference before fractioning: " +
				//scrollDistDirY + " = selecYStartIndex + " +
				//"numYSelectedIndexes / 2) - centerYIndex /// " +
				//selecYStartIndex + " + " + numYSelectedIndexes + " / 2) - " +
				//centerYIndex + ".");
				scrollDistDirY = (int) Math.round(scrollDistDirY *
						scrollFrac);
				//debug("ScrollY difference after fractioning: [" +
				//scrollDistDirY + "].");
	
				/*
				 * TODO: Refactor the scrolling so that these checks do not have
				 * to be done. - Rob
				 */
				//The following correction calculations are to prevent scrolling
				//a visible selected edge out of view.
				//The calculations are not perfect and are only necessary
				//because the zoomToward* functions only really work when the
				//selection is either completely in view or we're zoomed in
				//inside the selected area.
				int prevFarEdge = prevYFirstVisible + prevYNumVisible;
				int selecFarEdge = selecYStartIndex + numYSelectedIndexes;
				int newNearEdge = (selecYStartIndex +
						(int) Math.floor(numYSelectedIndexes / 2)) -
						scrollDistDirY -
						(int) Math.ceil((double) ymap.getNumVisible() / 2);
				int newFarEdge = (selecYStartIndex +
						(int) Math.floor(numYSelectedIndexes / 2)) -
						scrollDistDirY +
						(int) Math.ceil((double) ymap.getNumVisible() / 2);
				//Make sure we don't scroll an edge from either in view or
				//before the view to out of view/past the view
				if(selecFarEdge <= prevFarEdge && selecFarEdge > newFarEdge) {
					scrollDistDirY -= (selecFarEdge - newFarEdge) +
							(int) Math.round((1.0 - scrollFrac) *
									Math.abs((double) selecFarEdge -
											(double) prevFarEdge));
					//debug("ScrollY difference after correcting " +
					//"for far edge overscroll: [" + scrollDistDirY + "].");
					newNearEdge = (selecYStartIndex +
							(int) Math.floor(numYSelectedIndexes / 2)) -
							scrollDistDirY -
							(int) Math.ceil((double) ymap.getNumVisible() / 2);
					newFarEdge = (selecYStartIndex +
							(int) Math.floor(numYSelectedIndexes / 2)) -
							scrollDistDirY +
							(int) Math.ceil((double) ymap.getNumVisible() / 2);
					if(selecFarEdge > newFarEdge ||
					   (selecYStartIndex >= prevYFirstVisible &&
						selecYStartIndex < newNearEdge)) {
						scrollDistDirY = 0;
						//debug("ScrollY difference after " +
						//"correcting for far edge overscroll: [" +
						//scrollDistDirY + "].");
					}
				} else if(selecYStartIndex >= prevYFirstVisible &&
						  selecYStartIndex < newNearEdge) {
					scrollDistDirY += (newNearEdge - selecYStartIndex) +
							(int) Math.round((1.0 - scrollFrac) *
									((double) selecYStartIndex -
											(double) prevYFirstVisible));
					//debug("ScrollY difference after correcting " +
					//"for near edge overscroll: [" + scrollDistDirY +
					//"] because (selecYStartIndex >= prevYFirstVisible && " +
					//"selecYStartIndex < newNearEdge) = (" + selecYStartIndex +
					//" >= " + prevYFirstVisible + " && " + selecYStartIndex +
					//" < " + newNearEdge + ").");
					newNearEdge = (selecYStartIndex +
							(int) Math.floor(numYSelectedIndexes / 2)) -
							scrollDistDirY -
							(int) Math.ceil((double) ymap.getNumVisible() / 2);
					newFarEdge = (selecYStartIndex +
							(int) Math.floor(numYSelectedIndexes / 2)) -
							scrollDistDirY +
							(int) Math.ceil((double) ymap.getNumVisible() / 2);
					if(selecYStartIndex < newNearEdge ||
					   (selecFarEdge <= prevFarEdge &&
					    selecFarEdge > newFarEdge)) {
						//debug("ScrollY difference after " +
						//"correcting for far edge overscroll: [" +
						//scrollDistDirY + "].");
						scrollDistDirY = 0;
					}
				}

				//debug("Correcting the Y scroll because the " +
				//"pixel we are zooming to/from is on an edge.");
				//debug("(selecYStartIndex + numYSelectedIndexes " +
				//"/ 2) - scrollDistDirY");
				//debug(((selecYStartIndex +
				//(int) Math.floor((double) numYSelectedIndexes / 2.0)) -
				//scrollDistDirY) + " = (" + selecYStartIndex + " + " +
				//numYSelectedIndexes + " / 2) - " + scrollDistDirY);
				ymap.scrollToIndex((selecYStartIndex +
						(int) Math.floor(numYSelectedIndexes / 2.0)) -
						scrollDistDirY);
			}
		}

		//Update the aspect ratio once we have arrived at our destination (as we
		//are going to be using the starting aspect ratio to use it to gradually
		//adjust the target aspect ratio)
		if(xmap.getNumVisible() == numXSelectedIndexes &&
		   ymap.getNumVisible() == numYSelectedIndexes) {
			updateAspectRatio();
		}

		//debug("Zoom redraw bounds initial: [" + startXPixel + "," +
		//		startYPixel + "," + (startXPixel + numSelectedXPixels - 1) +
		//		"," + (startYPixel + numSelectedYPixels - 1) +
		//		"]. Panel dimensions: [" + getWidth() + "x" + getHeight() +
		//		"].");
		//Compute the area to redraw (only for zooming-out - see code at end)
		int[] redrawPixelBounds = new int[4];

		pixelsPerXIndex = xmap.getScale();
		if(prevXNumVisible > numXSelectedIndexes) {
			//debug("Zoom redraw bounds initial X: [" + startXPixel +
			//		"," + (startXPixel + numSelectedXPixels - 1) +
			//		"]. Panel dimensions: [" + getWidth() + "].");
			//Now let's return the pixel indexes of the selection post-zoom
			redrawPixelBounds[0] = xmap.getPixel(selecXStartIndex);
			redrawPixelBounds[2] =
				(int) Math.round(numXSelectedIndexes *
				pixelsPerXIndex) + 1; //Added 1 because sometimes inaccurate
		} else {
			redrawPixelBounds[0] = xmap.getPixel(prevXFirstVisible);
			redrawPixelBounds[2] = (int) Math.round(prevXNumVisible *
					pixelsPerXIndex) + 1; //Added 1 because sometimes inaccurate
		}
		pixelsPerYIndex = ymap.getScale();
		if(prevYNumVisible > numYSelectedIndexes) {
			//debug("Zoom redraw bounds initial: [" + startYPixel +
			//		"," + (startYPixel + numSelectedYPixels - 1) +
			//		"]. Panel dimensions: [" + getHeight() + "].");
			redrawPixelBounds[1] = ymap.getPixel(selecYStartIndex);
			redrawPixelBounds[3] =
				(int) Math.round(numYSelectedIndexes *
				pixelsPerYIndex) + 1; //Added 1 because sometimes inaccurate
			//debug("Zoom redraw bounds before fix: [" +
			//		redrawPixelBounds[0] + "," + redrawPixelBounds[1] + "," +
			//		redrawPixelBounds[2] + "," + redrawPixelBounds[3] + "].");
		} else {
			redrawPixelBounds[1] = ymap.getPixel(prevYFirstVisible);
			redrawPixelBounds[3] = (int) Math.round(prevYNumVisible *
					pixelsPerYIndex) + 1; //Added 1 because sometimes inaccurate
		}

		int maxWidth  = getWidth();
		int maxHeight = getHeight();

		if(redrawPixelBounds[0] < 0) redrawPixelBounds[0] = 0;
		if(redrawPixelBounds[0] > maxWidth) redrawPixelBounds[0] = maxWidth - 1;
		if(redrawPixelBounds[2] < 0) redrawPixelBounds[2] = 0;
		if((redrawPixelBounds[0] + redrawPixelBounds[2]) > maxWidth)
				redrawPixelBounds[2] = maxWidth - redrawPixelBounds[0];

		if(redrawPixelBounds[1] < 0) redrawPixelBounds[1] = 0;
		if(redrawPixelBounds[1] > maxHeight) redrawPixelBounds[1] =
				maxHeight - 1;
		if(redrawPixelBounds[3] < 0) redrawPixelBounds[3] = 0;
		if((redrawPixelBounds[1] + redrawPixelBounds[3]) > maxHeight)
				redrawPixelBounds[3] = maxHeight - redrawPixelBounds[1];
		//debug("Zoom redraw bounds after fix: [" +
		//		redrawPixelBounds[0] + "," + redrawPixelBounds[1] + "," +
		//		redrawPixelBounds[2] + "," + redrawPixelBounds[3] + "].");
		return(redrawPixelBounds);
	}

	public double getAspectRatio(int numXDots,int numYDots) {
		return((double) numXDots / (double) numYDots);
	}

	public void updateAspectRatio() {
		aspectRatio = getAspectRatio(xmap.getNumVisible(),ymap.getNumVisible());
		//debug("Aspect Ratio updated to [" + aspectRatio + " : " +
		//xmap.getNumVisible() + "/" + ymap.getNumVisible() + "].");
	}

	public void setAspectRatio(int numXDots,int numYDots) {
		aspectRatio = getAspectRatio(numXDots,numYDots);
		//debug("Aspect Ratio set to [" + aspectRatio + " : " +
		//xmap.getNumVisible() + "/" + ymap.getNumVisible() + "].");
	}

	private void drawBand(final Rectangle l) {

		final Graphics g = getGraphics();
		Graphics2D g2d = (Graphics2D) g;
		g2d.setXORMode(getBackground());
		g2d.setColor(GUIFactory.MAIN);

		final int x = xmap.getPixel(l.x);
		final int y = ymap.getPixel(l.y);
		final int w = xmap.getPixel(l.x + l.width + 1) - x;
		final int h = ymap.getPixel(l.y + l.height + 1) - y;

		g2d.drawRect(x, y, w, h);
		g2d.setPaintMode();
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

		if (geneSelection == null || arraySelection == null ||
			arraySelection.getNSelectedIndexes() == 0 ||
			geneSelection.getNSelectedIndexes()  == 0) {
			indicatorCircleList = null;
			return;
		}

		//Empty the list of
		indicatorCircleList = new ArrayList<Ellipse2D.Double>();

		final int[] selectedArrayIndexes = arraySelection.getSelectedIndexes();
		final int[] selectedGeneIndexes  = geneSelection.getSelectedIndexes();

		List<List<Integer>> arrayBoundaryList;
		List<List<Integer>> geneBoundaryList;

		arrayBoundaryList = findRectangleBoundaries(selectedArrayIndexes,
				xmap);
		geneBoundaryList  = findRectangleBoundaries(selectedGeneIndexes,
				ymap);

		/*
		 * TODO: Instead of just checking the last(/next) selection
		 * position, should group all small selections together too see
		 * if the cluster is smaller than our limits.
		 */
		double lastxb = -1;

		// Make the rectangles
		for (final List<Integer> xBoundaries : arrayBoundaryList) {

			/*
			 * TODO: Instead of just checking the last(/next) selection
			 * position, should group all small selections together too see
			 * if the cluster is smaller than our limits.
			 */
			double lastyb = -1;
			w = (xBoundaries.get(1) - xBoundaries.get(0));

			for (final List<Integer> yBoundaries : geneBoundaryList) {

				//Width and height of rectangle which spans the Ellipse2D
				//object
				h = (yBoundaries.get(1) - yBoundaries.get(0));

				if(w < 20 && h < 20 &&
				   //This is not the first selection and the last selection is
					//far away OR this is the first selection and the next
					//selection either doesn't exists or is far away
				   ((lastxb >= 0 &&
				     Math.abs(xBoundaries.get(0) - lastxb) > 20) ||
				    (lastxb < 0 &&
				     (arrayBoundaryList.size() == 1 ||
				      Math.abs(arrayBoundaryList.get(1).get(0) -
				               xBoundaries.get(1)) > 20))) &&
				   ((lastyb >= 0 &&
				     Math.abs(yBoundaries.get(0) - lastyb) > 20) ||
				    (lastyb < 0 &&
				     (geneBoundaryList.size() == 1 ||
				      Math.abs(geneBoundaryList.get(1).get(0) -
				               yBoundaries.get(1)) > 20)))) {
					// coords for top left of circle
					x = xBoundaries.get(0) + (w / 2.0) - 20;
					y = yBoundaries.get(0) + (h / 2.0) - 20;

					indicatorCircleList.add(new Ellipse2D.Double(x, y, 40, 40));
				}
				lastyb = yBoundaries.get(1);
			}
			lastxb = xBoundaries.get(1);
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

		//debug("Rectangle selected.");
		globalMatrixView.setIMVselectedIndexes(
				arraySelection.getSelectedIndexes(),
				geneSelection.getSelectedIndexes());

		geneSelection.notifyObservers();
		arraySelection.notifyObservers();
	}

	public TreeSelectionI getGeneSelection() {
		return(geneSelection);
	}

	public TreeSelectionI getArraySelection() {
		return(arraySelection);
	}
}
