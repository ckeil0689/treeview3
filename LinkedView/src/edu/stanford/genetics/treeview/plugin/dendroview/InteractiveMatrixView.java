/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import javax.swing.JScrollBar;

import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeSelectionI;

/**
 * TODO completely remove dependencies on MapContainer + Selections. This is
 * all Controller stuff. The only thing this class ever needs to know is
 * GUI painting related. ALL GUI handling + logic code execution happens
 * in the controller.
 *
 */
public class InteractiveMatrixView extends MatrixView {

	private static final long serialVersionUID = 1L;

	public static final int FILL = 0;
	public static final int EQUAL = 1;
	public static final int PROPORT = 2;

	private double aspectRatio = -1;

	/**
	 * Rectangle to track yellow selected rectangle (pixels)
	 */
	private List<Rectangle> selectionRectList = new ArrayList<Rectangle>();
	private Rectangle tmpRect = new Rectangle();

	/**
	 * Circle to be used as indicator for selection
	 */
	private List<Ellipse2D.Double> indicatorCircleList = null;

	private boolean overlayTempChange = false;

	/**
	 * GlobalView also likes to have an globalxmap and globalymap (both of type
	 * MapContainer) to help it figure out where to draw things. It also tries
	 * to
	 */
	public InteractiveMatrixView() {

		super();

		setLabelPortMode(true);
		debug = 14;
		//1 = Debug double-click detection
		//10 = Debug click dragging
		//11 = Debug zoom animation "toward" selection/sub-selection
		//13 = Debug zooming to/toward a target
		//14 = Debug temporary band drawing upon mousePressed
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

		debug("Updating buffer.  Temporary band change? [" + (isOverlayTempChange() ? "yes" : "no") + "]",14);

		//Don't need to repaint on only a hover change - waste of effort - it's
		//only for other observers and other functionality here
		if(xmap.hoverChanged() || ymap.hoverChanged()) {
			xmap.unsetHoverChanged();
			ymap.unsetHoverChanged();
			return;
		}
		
		Graphics2D g2d = (Graphics2D) g;

		revalidateScreen();

		if (!offscreenValid || isOverlayTempChange()) {
			setOverlayTempChange(false);
			// clear the pallette...
			g2d.setColor(GUIFactory.DEFAULT_BG);
			g2d.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
			g2d.setColor(Color.black);

			// TODO rectangles not needed anymore, adapt ArrayDrawer methods.
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

		debug("paintComposite.  Temporary band change? [" + (isOverlayTempChange() ? "yes" : "no") + "]",14);

		if(xmap.hoverChanged() || ymap.hoverChanged()) {
			xmap.unsetHoverChanged();
			ymap.unsetHoverChanged();
			return;
		}

		Graphics2D g2 = (Graphics2D) g;

		if(isOverlayTempChange()) {
			setOverlayTempChange(false);
			g2.drawRect(tmpRect.x, tmpRect.y, tmpRect.width, tmpRect.height);
		}

		if (selectionRectList != null) {
			//Reinitialize the graphics object
			g2 = (Graphics2D) g;

			/* draw all selection rectangles in yellow */
			g2.setColor(Color.yellow);

			for (final Rectangle rect : selectionRectList) {
				g2.drawRect(rect.x, rect.y, rect.width, rect.height);
			}

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
					g2.draw(indicatorCircle);
				}
			}
		}
	}

	@Override
	protected void recalculateOverlay() {

		if(xmap.hoverChanged() || ymap.hoverChanged()) {
			xmap.unsetHoverChanged();
			ymap.unsetHoverChanged();
			return;
		}

		if((rowSelection == null) || (colSelection == null)) {
			selectionRectList   = null;
			indicatorCircleList = null;
			
		} else {
			selectionRectList = new ArrayList<Rectangle>();
	
			final int[] selectedArrayIndexes =
				colSelection.getSelectedIndexes();
			final int[] selectedGeneIndexes =
				rowSelection.getSelectedIndexes();
	
			if (selectedArrayIndexes.length > 0) {
				List<List<Integer>> arrayBoundaryList;
				List<List<Integer>> geneBoundaryList;
	
				arrayBoundaryList =
					findRectBoundaries(selectedArrayIndexes, xmap);
				geneBoundaryList =
					findRectBoundaries(selectedGeneIndexes, ymap);
	
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
	 * @author rleach
	 * @return the overlayTempChange
	 */
	public boolean isOverlayTempChange() {
		return(overlayTempChange);
	}

	/**
	 * @author rleach
	 * @param overlayTempChange the overlayTempChange to set
	 */
	public void setOverlayTempChange(boolean overlayTempChange) {
		this.overlayTempChange = overlayTempChange;
	}

	/**
	 * @author rleach
	 * @param tmpRect the tmpRect to set
	 */
	public void setTmpRect(Rectangle tmpRect) {
		this.tmpRect = tmpRect;
	}

	/**
	 * Finds the boundaries needed to draw all selection rectangles
	 *
	 * @param selectedIndexes
	 * @param map
	 * @return
	 */
	protected List<List<Integer>> findRectBoundaries(
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
			// removed + 1 here due to new image drawing (otherwise selection
			//was not accurate by 1 pixel)

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
		
		if (o == rowSelection) {
			updateSelection(rowSelection, colSelection);
			recalculateOverlay();

			/* trigger updatePixels() */
			offscreenValid = false;
			repaint();

		} else if (o == colSelection) {
			updateSelection(colSelection, rowSelection);
			recalculateOverlay();

			offscreenValid = false;
			repaint();

		} else {
			super.update(o, arg);
		}
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
	
	/**
	 * This function, given a pixel location (e.g. where the cursor is), will
	 * intelligently choose a zoom amount to give the appearance of a smooth
	 * zoom that expands from the given pixel
	 * 
	 * @param xPxPos
	 * @param yPxPos
	 */
	public void smoothZoomTowardPixel(int xPxPos,int yPxPos) {
		smoothZoomTowardPixel(xPxPos,yPxPos,xmap.getZoomIncrement());
	}

	/**
	 * This function, given a pixel location (e.g. where the cursor is), will
	 * intelligently choose a zoom amount to given the appearance of a smooth
	 * zoom that expands from the given pixel
	 * 
	 * @param xPxPos
	 * @param yPxPos
	 */
	public void smoothZoomTowardPixelFast(int xPxPos,int yPxPos) {
		smoothZoomTowardPixel(xPxPos,yPxPos,xmap.getZoomIncrementFast());
	}

	/**
	 * This method zooms the zoom increment given toward the supplied pixel
	 * coordinates in an animated fashion. Basically, it issues repeated calls
	 * to smoothZoomTowardPixel until the target zoom increment is reached.
	 * Functions similar to smoothAnimatedZoomToTarget.
	 * @author rleach
	 * @return none
	 * @param xPxPos
	 * @param yPxPos
	 * @param zoomInc
	 */
	public void smoothAnimatedZoomTowardPixel(int xPxPos,int yPxPos,
		double targetZoomInc) {

		//If there's nothing to zoom, just return
		if(xmap.getNumVisible() == 1 && ymap.getNumVisible() == 1) {
			return;
		}

		long startTime = System.currentTimeMillis();

		//Zooming out is slower on large matrices because it just takes longer
		//to draw large amounts of cells, so our cutoff wait time should be
		//smaller if we are zooming out
		int doneWaitingMillis = 500;
		double zoomInc = xmap.getZoomIncrement();
		double zoomTotal = 0.0;

		if(zoomInc > targetZoomInc) {
			smoothZoomTowardPixel(xPxPos,yPxPos,targetZoomInc);
			return;
		}

		//Let's calculate the relative position of the center of the selection
		//and gradually zoom toward that spot in a loop
		while((zoomInc + zoomTotal) < targetZoomInc &&
			(xmap.getNumVisible() > 1 || ymap.getNumVisible() > 1)) {

			//If drawing the zoom increment levels is taking too long, snap out
			//of it.
			if((System.currentTimeMillis() - startTime) > doneWaitingMillis)
				break;

			smoothZoomTowardPixel(xPxPos,yPxPos,zoomInc);
			zoomTotal += zoomInc;

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

		//If we have not zoomed the full target fraction (within a margin of
		//accuracy) and there are still cells to zoom
		if(zoomTotal < targetZoomInc && (targetZoomInc - zoomTotal) > 0.0005 &&
			(xmap.getNumVisible() > 1 || ymap.getNumVisible() > 1)) {
			//debug("Adjusting final Y scroll");
			smoothZoomTowardPixel(xPxPos,yPxPos,zoomTotal - zoomInc);
		}

		//We will update the aspect ratio just in case it didn't happen
		//automatically
		updateAspectRatio();
	}

	/**
	 * This method zooms the zoom increment given toward the supplied pixel
	 * coordinates in an animated fashion. Basically, it issues repeated calls
	 * to smoothZoomTowardPixel until the target zoom increment is reached.
	 * Functions similar to smoothAnimatedZoomToTarget.
	 * @author rleach
	 * @return none
	 * @param xPxPos
	 * @param yPxPos
	 * @param zoomInc
	 */
	public void smoothAnimatedZoomFromPixel(int xPxPos,int yPxPos,
		double targetZoomInc) {

		//If there's nothing to zoom, just return
		if(xmap.getNumVisible() == (xmap.getMaxIndex() + 1) &&
			ymap.getNumVisible() == (ymap.getMaxIndex() + 1)) {
			return;
		}

		long startTime = System.currentTimeMillis();

		//Zooming out is slower on large matrices because it just takes longer
		//to draw large amounts of cells, so our cutoff wait time should be
		//smaller if we are zooming out
		int doneWaitingMillis = 500;
		double zoomInc = xmap.getZoomIncrement();
		double zoomTotal = 0.0;

		if(zoomInc > targetZoomInc) {
			smoothZoomFromPixel(xPxPos,yPxPos,targetZoomInc);
			return;
		}

		//Let's calculate the relative position of the center of the selection
		//and gradually zoom toward that spot in a loop
		while((zoomInc + zoomTotal) < targetZoomInc &&
			(xmap.getNumVisible() <= xmap.getMaxIndex() ||
			ymap.getNumVisible() <= ymap.getMaxIndex())) {

			//If drawing the zoom increment levels is taking too long, snap out
			//of it.
			if((System.currentTimeMillis() - startTime) > doneWaitingMillis)
				break;

			smoothZoomFromPixel(xPxPos,yPxPos,zoomInc);
			zoomTotal += zoomInc;

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

		//If we have not zoomed the full
		if(zoomTotal < targetZoomInc && (targetZoomInc - zoomTotal) > 0.0005 &&
			(xmap.getNumVisible() <= xmap.getMaxIndex() ||
			ymap.getNumVisible() <= ymap.getMaxIndex())) {
			//debug("Adjusting final Y scroll");
			smoothZoomFromPixel(xPxPos,yPxPos,zoomTotal - zoomInc);
		}

		//We will update the aspect ratio just in case it didn't happen
		//automatically
		updateAspectRatio();
	}

	/**
	 * This function, given a pixel location (e.g. where the cursor is) and a
	 * fraction to zoom by, will intelligently choose a zoom amount to given the
	 * appearance of a smooth zoom that expands from the given pixel
	 * 
	 * @param xPxPos
	 * @param yPxPos
	 * @param zoomInc
	 */
	public void smoothZoomTowardPixel(int xPxPos,int yPxPos,double zoomInc) {

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
		double targetZoomFrac = zoomInc;

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

		smoothZoomFromPixel(xPxPos,yPxPos,xmap.getZoomIncrement());
	}

	/**
	 * This function, given a pixel location (e.g. where the cursor is), will
	 * intelligently choose a zoom amount to given the appearance of a smooth
	 * zoom that contracts on the given pixel
	 * 
	 * @param xPxPos
	 * @param yPxPos
	 */
	public void smoothZoomFromPixelFast(int xPxPos,int yPxPos) {

		smoothZoomFromPixel(xPxPos,yPxPos,xmap.getZoomIncrementFast());
	}

	/**
	 * This function, given a pixel location (e.g. where the cursor is) and zoom
	 * increment, will intelligently choose a zoom amount to given the
	 * appearance of a smooth zoom that contracts on the given pixel
	 * 
	 * @param xPxPos
	 * @param yPxPos
	 * @param zoomInc
	 */
	public void smoothZoomFromPixel(int xPxPos,int yPxPos,double zoomInc) {

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
		double targetZoomFrac = zoomInc;

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

			xmap.zoomOutCenter(MapContainer.ZOOM_FAST);
			ymap.zoomOutCenter(MapContainer.ZOOM_FAST);
		}

		//If we've reached full zoom-out, reset the aspect ratio
		if(xmap.getNumVisible() == (xmap.getMaxIndex() + 1) &&
				ymap.getNumVisible() == (ymap.getMaxIndex() + 1)) {
			setAspectRatio(xmap.getMaxIndex() + 1,ymap.getMaxIndex() + 1);
		}
	}

	/**
	 * This is just a wrapper for smoothAnimatedZoomToTarget which fills
	 * in the dimensions of the matrix
	 */
	public void smoothAnimatedZoomOut() {
	
		smoothAnimatedZoomToTarget(0, (xmap.getTotalTileNum()), 
				0, (ymap.getTotalTileNum()));
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
	public void smoothAnimatedZoomToTarget(int selecXStartIndex,
		int numXSelectedIndexes,int selecYStartIndex,int numYSelectedIndexes) {

		long startTime = System.currentTimeMillis();

		//Zooming out is slower on large matrices because it just takes longer
		//to draw large amounts of cells, so our cutoff wait time should be
		//smaller if we are zooming out
		int doneWaitingMillis = 500;

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
		hardZoomToTarget(selecXStartIndex,numXSelectedIndexes,selecYStartIndex,
			numYSelectedIndexes);

		//We will update the aspect ratio just in case it didn't happen
		//automatically
		updateAspectRatio();
	}

	/**
	 * This method is basically the same as smoothAnimatedZoomToTarget, except
	 * that it only zooms the amount indicated by zoomInc
	 * @author rleach
	 * @return none
	 * @param selecXStartIndex
	 * @param numXSelectedIndexes
	 * @param selecYStartIndex
	 * @param numYSelectedIndexes
	 * @param targetZoomInc
	 */
	public void smoothAnimatedZoomTowardTarget(int selecXStartIndex,
		int numXSelectedIndexes,int selecYStartIndex,int numYSelectedIndexes,
		double targetZoomInc) {

		double zoomInc = xmap.getZoomIncrement();
		int startVisX = xmap.getNumVisible();
		int startVisY = ymap.getNumVisible();
		int targetDiffX =
			(int) Math.round((double) xmap.getNumVisible() * targetZoomInc -
				(double) xmap.getNumVisible() * zoomInc);
		int targetDiffY =
			(int) Math.round((double) ymap.getNumVisible() * targetZoomInc -
				(double) ymap.getNumVisible() * zoomInc);

		if(targetZoomInc >= zoomInc &&
			targetDiffX > Math.abs(xmap.getNumVisible() - startVisX) &&
			targetDiffY > Math.abs(ymap.getNumVisible() - startVisY)) {

			long startTime = System.currentTimeMillis();
		
			//Zooming out is slower on large matrices because it just takes
			//longer to draw large amounts of cells, so our cutoff wait time
			//should be smaller if we are zooming out
			int doneWaitingMillis = 500;
		
			//Let's calculate the relative position of the center of the
			//selection and gradually zoom toward that spot in a loop
			while((xmap.getNumVisible() != numXSelectedIndexes ||
				ymap.getNumVisible() != numYSelectedIndexes) &&
				targetDiffX > Math.abs(xmap.getNumVisible() - startVisX) &&
				targetDiffY > Math.abs(ymap.getNumVisible() - startVisY)) {
		
				//If drawing the zoom increment levels is taking too long, snap
				//out of it.
				if((System.currentTimeMillis() - startTime) > doneWaitingMillis)
					break;

				//This zooms by zoomInc
				//We will assume a relatively small amount of zoom and forego
				//logMode
				smoothZoomTowardSelection(selecXStartIndex,numXSelectedIndexes,
					selecYStartIndex,numYSelectedIndexes,false,-1.0);

				debug("Zoomed by [" + targetDiffX + "," + targetDiffY +
					"] thus far",11);
		
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
		} else if(targetZoomInc > 0.0) {
			smoothZoomTowardSelection(selecXStartIndex,numXSelectedIndexes,
				selecYStartIndex,numYSelectedIndexes,false,targetZoomInc);
		}

		//If the target is less than zoomInc away in both dimensions, hard-zoom
		//to finish it off
		if(//We're not at the target
			(xmap.getNumVisible() != numXSelectedIndexes ||
			ymap.getNumVisible() != numYSelectedIndexes) &&
			//The matrix is too small (in a dimension that's not fully zoomed)
			//for the logic following to work
			(1.0 / (double) (xmap.getMaxIndex() + 1) > zoomInc &&
				xmap.getNumVisible() != numXSelectedIndexes) ||
			(1.0 / (double) (ymap.getMaxIndex() + 1) > zoomInc &&
				ymap.getNumVisible() != numYSelectedIndexes) ||
			//X Size difference is less than the zoom increment (times two
			//because this is for the outer edge)
			(double) Math.abs(xmap.getNumVisible() - numXSelectedIndexes) * 2 /
			(double) (xmap.getMaxIndex() + 1) < zoomInc &&
			//X Position difference is less than the zoom increment
			(double) Math.abs(xmap.getFirstVisible() - selecXStartIndex) /
			(double) (xmap.getMaxIndex() + 1) < zoomInc &&
			//Y Size difference is less than the zoom increment (times two
			//because this is for the outer edge)
			(double) Math.abs(ymap.getNumVisible() - numYSelectedIndexes) * 2 /
			(double) (ymap.getMaxIndex() + 1) < zoomInc &&
			//Y Position difference is less than the zoom increment
			(double) Math.abs(ymap.getFirstVisible() - selecYStartIndex) /
			(double) (ymap.getMaxIndex() + 1) < zoomInc) {

			debug("Hard zooming to target to finish off the zoom step.",11);

			hardZoomToTarget(selecXStartIndex,numXSelectedIndexes,
				selecYStartIndex,numYSelectedIndexes);
		}

		//We will update the aspect ratio just in case it didn't happen
		//automatically
		updateAspectRatio();
	}

	/**
	 * This method simply groups the xmap and ymap calls necessary for a single
	 * zoom event
	 * @author rleach
	 * @param selecXStartIndex
	 * @param numXSelectedIndexes
	 * @param selecYStartIndex
	 * @param numYSelectedIndexes
	 */
	public void hardZoomToTarget(int selecXStartIndex,
		int numXSelectedIndexes,int selecYStartIndex,int numYSelectedIndexes) {

		if(xmap.getNumVisible() != numXSelectedIndexes) {
			xmap.zoomToSelected(selecXStartIndex,
					(selecXStartIndex + numXSelectedIndexes - 1));
		}
		if(ymap.getNumVisible() != numYSelectedIndexes) {
			ymap.zoomToSelected(selecYStartIndex,
					(selecYStartIndex + numYSelectedIndexes - 1));
		}
		if(xmap.getFirstVisible() != selecXStartIndex) {
			xmap.scrollToFirstIndex(selecXStartIndex);
		}
		if(ymap.getFirstVisible() != selecYStartIndex) {
			ymap.scrollToFirstIndex(selecYStartIndex);
		}
	}

	/**
	 * This method is a wrapper for the modified smoothZoomTowardSelection
	 * which now takes a boolean indicating log zoom mode, provided for
	 * simplicity and backwards compatibility
	 * @author rleach
	 * @param selecXStartIndex
	 * @param numXSelectedIndexes
	 * @param selecYStartIndex
	 * @param numYSelectedIndexes
	 * @return
	 */
	public int[] smoothZoomTowardSelection(int selecXStartIndex,
										   int numXSelectedIndexes,
										   int selecYStartIndex,
										   int numYSelectedIndexes) {

		return(smoothZoomTowardSelection(selecXStartIndex,numXSelectedIndexes,
			selecYStartIndex,numYSelectedIndexes,true,-1.0));
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
	 * @author rleach
	 * @param selecXStartIndex
	 * @param numXSelectedIndexes
	 * @param selecYStartIndex
	 * @param numYSelectedIndexes
	 * @param logMode
	 * @param optionalTargetZoomFrac (only used if > 0.0)
	 */
	public int[] smoothZoomTowardSelection(int selecXStartIndex,
		int numXSelectedIndexes,int selecYStartIndex,int numYSelectedIndexes,
		boolean logMode,double optionalTargetZoomFrac) {

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
		double targetZoomFracX = (optionalTargetZoomFrac > 0.0 ?
			optionalTargetZoomFrac :
			(logMode ? xmap.getOptimalZoomIncrement(numXSelectedIndexes,
				(prevXNumVisible < numXSelectedIndexes)) :
					xmap.getZoomIncrement()));
		double targetZoomFracY = (optionalTargetZoomFrac > 0.0 ?
			optionalTargetZoomFrac :
			(logMode ? ymap.getOptimalZoomIncrement(numYSelectedIndexes,
				(prevYNumVisible < numYSelectedIndexes)) :
					ymap.getZoomIncrement()));
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
		
		aspectRatio = getAspectRatio(xmap.getNumVisible(), ymap.getNumVisible());
	}

	public void setAspectRatio(int numXDots,int numYDots) {
		aspectRatio = getAspectRatio(numXDots,numYDots);
	}

	public void drawBand(final Rectangle l) {

		Graphics2D g2d = (Graphics2D) getGraphics();
		g2d.setXORMode(getBackground());
		g2d.setColor(GUIFactory.MAIN);

		g2d.drawRect(l.x, l.y, l.width, l.height);
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

		if (rowSelection == null || colSelection == null ||
			colSelection.getNSelectedIndexes() == 0 ||
			rowSelection.getNSelectedIndexes()  == 0) {
			indicatorCircleList = null;
			return;
		}

		//Empty the list of
		indicatorCircleList = new ArrayList<Ellipse2D.Double>();

		final int[] selectedArrayIndexes = colSelection.getSelectedIndexes();
		final int[] selectedGeneIndexes  = rowSelection.getSelectedIndexes();

		List<List<Integer>> arrayBoundaryList;
		List<List<Integer>> geneBoundaryList;

		arrayBoundaryList = findRectBoundaries(selectedArrayIndexes,
				xmap);
		geneBoundaryList  = findRectBoundaries(selectedGeneIndexes,
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

	public TreeSelectionI getRowSelection() {
		
		return rowSelection;
	}

	public TreeSelectionI getColSelection() {
		
		return colSelection;
	}
}
