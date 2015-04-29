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

import edu.stanford.genetics.treeview.LogBuffer;

public class GlobalMatrixView extends MatrixView {

	/**
	 * Default so warnings don't pop up...
	 */
	private static final long serialVersionUID = 1L;
	
	private final int MAP_SIZE_LIMIT = 300;
	private final int MIN_VIEWPORT_SIZE = 1;
	
	/* 
	 * Also needs reference to interactive MapContainers to get knowledge
	 * about which specific tiles are currently visible and update the 
	 * viewport rectangle through the observer pattern.
	 */
	private MapContainer interactiveXmap;
	private MapContainer interactiveYmap;
	
	private int xViewMin;
	private int yViewMin;
	
	private final Rectangle viewPortRect  = new Rectangle();

	/**
	 * Rectangle to track yellow selected rectangle (pixels)
	 */
	private List<Rectangle> selectionRectList = new ArrayList<Rectangle>();

	/**
	 * Circle to be used as indicator for Visible area in interactive matrix
	 */
	private Ellipse2D.Double indicatorVisibleCircle = null;

	/**
	 * Circle to be used as indicator for selection area in interactive matrix
	 */
	private List<Ellipse2D.Double> indicatorSelectionCircleList = null;

	private int[] selectedIMVArrayIndexes = null;
	private int[] selectedIMVGeneIndexes  = null;

	public GlobalMatrixView() {
		
		super();
	}
	
	@Override
	public synchronized void paintComposite(final Graphics g) {
		if (viewPortRect != null) {
			/* draw visible rectangle in white */
			g.setColor(Color.white);

			g.drawRect(viewPortRect.x, viewPortRect.y, viewPortRect.width, 
					viewPortRect.height);
		}
		if (selectionRectList != null) {

			/* draw all selection rectangles in yellow */
			g.setColor(Color.yellow);

			for (final Rectangle rect : selectionRectList) {
				g.drawRect(rect.x, rect.y, rect.width, rect.height);
			}
		}
		final Graphics2D g2 = (Graphics2D) g;
		if(viewPortRect != null || indicatorSelectionCircleList != null) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
								RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setStroke(new BasicStroke(2));
		}
		if (viewPortRect != null) {
			g2.setColor(Color.white);
			if (indicatorVisibleCircle != null) {
				g2.draw(indicatorVisibleCircle);
			}
		}
		if (indicatorSelectionCircleList != null) {
			g2.setColor(Color.yellow);
			for(Ellipse2D.Double indicatorCircle : indicatorSelectionCircleList) {
				//LogBuffer.println("Drawing ellipse.");
				g2.draw(indicatorCircle);
			}
		}
	}
	
	/**
	 * This method updates a pixel buffer. The alternative is to update the
	 * graphics object directly by calling updateBuffer.
	 */
	@Override
	protected void updatePixels() {

		if (!offscreenValid) {
			
			revalidateScreen();
			
			// LogBuffer.println("OFFSCREEN INVALID");
			final Rectangle destRect = new Rectangle(0, 0,
					xmap.getUsedPixels(), ymap.getUsedPixels());
			
			final Rectangle sourceRect = new Rectangle(xmap.getIndex(0),
					ymap.getIndex(0), xmap.getNumVisible(), 
					ymap.getNumVisible());

			if ((sourceRect.x >= 0) && (sourceRect.y >= 0) && drawer != null) {

				/* Set new offscreenPixels (pixel colors) */
				drawer.paint(offscreenPixels, sourceRect, destRect,
						offscreenScanSize);
			}

			offscreenSource.newPixels();
		}
	}
	
	@Override
	public void update(Observable o, Object arg) {
		
		if (o == interactiveXmap || o == interactiveYmap) {
			recalculateOverlay();
			offscreenValid = false;
			repaint();
			
		} else {
			super.update(o, arg);
		}
	}
	
	@Override
	public String viewName() {

		return "GlobalMatrixView";
	}
	
	/**
	 * Sets a reference to the interactive x-MapContainer to keep track of
	 * of currently visible tiles. 
	 * @param m
	 */
	public void setInteractiveXMap(final MapContainer m) {

		if (interactiveXmap != null) {
			interactiveXmap.deleteObserver(this);
		}

		interactiveXmap = m;
		interactiveXmap.addObserver(this);
	}

	/**
	 * Sets a reference to the interactive y-MapContainer to keep track of
	 * of currently visible tiles. 
	 * @param m
	 */
	public void setInteractiveYMap(final MapContainer m) {

		if (interactiveYmap != null) {
			interactiveYmap.deleteObserver(this);
		}

		interactiveYmap = m;
		interactiveYmap.addObserver(this);
	}
	
	/**
	 * DEPRECATE set the xmapping for this view
	 *
	 * @param m
	 *            the new mapping
	 */
	@Override
	public void setXMap(final MapContainer m) {

		super.setXMap(m);
		
		if(xmap.getMaxIndex() > MAP_SIZE_LIMIT) {
			this.xViewMin = (xmap.getMaxIndex() / 100) * 2;
			
		} else {
			this.xViewMin = MIN_VIEWPORT_SIZE;
		}
	}

	/**
	 * DEPRECATE set the ymapping for this view
	 *
	 * @param m
	 *            the new mapping
	 */
	@Override
	public void setYMap(final MapContainer m) {

		super.setYMap(m);
		
		if(ymap.getMaxIndex() > MAP_SIZE_LIMIT) {
			this.yViewMin = (ymap.getMaxIndex() / 100) * 2;
			
		} else {
			this.yViewMin = MIN_VIEWPORT_SIZE;
		}
	}
	
	/**
	 * Checks the current view of rows and columns and calculates 
	 * the appropriate viewport rectangle.
	 */
	@Override
	protected void recalculateOverlay() {
		
		/* Assure minimum draw size for viewport rectangle */
		int xRange; 
		if(interactiveXmap.getNumVisible() > xViewMin) {
			xRange = interactiveXmap.getNumVisible();
		} else {
			xRange = xViewMin;
		}
		
		int yRange; 
		if(interactiveYmap.getNumVisible() > yViewMin) {
			yRange = interactiveYmap.getNumVisible();
		} else {
			yRange = yViewMin;
		}
		
		int xFirst = interactiveXmap.getFirstVisible();
		int xLast = xFirst + (xRange - 1);
		
		int yFirst = interactiveYmap.getFirstVisible();
		int yLast = yFirst + (yRange - 1);
		
		int spx = xmap.getPixel(xFirst);
		int epx = xmap.getPixel(xLast + 1) - 1;
		int spy = ymap.getPixel(yFirst);
		int epy = ymap.getPixel(yLast + 1) - 1;

		if (epx < spx) {
			epx = spx;
		}

		if (epy < spy) {
			epy = spy;
		}
		
		viewPortRect.setBounds(spx, spy, epx - spx, epy - spy);

		setIndicatorVisibleCircleBounds();


		//if ((geneSelection == null) || (arraySelection == null)) {
		if ((selectedIMVGeneIndexes == null) ||
			(selectedIMVArrayIndexes == null)) {
			selectionRectList = null;
			indicatorSelectionCircleList = null;
			//LogBuffer.println("Nothing appears to be selected.");
			return;
		}

		selectionRectList = new ArrayList<Rectangle>();

		//final int[] selectedArrayIndexes = arraySelection.getSelectedIndexes();
		//final int[] selectedGeneIndexes = geneSelection.getSelectedIndexes();

		//LogBuffer.println("Num selected array indexes: [" +
		//		selectedIMVArrayIndexes.length + "].");
		//Could check either genes or arrays here - it's assumed if one's
		//populated, the other must be as well
		if (selectedIMVArrayIndexes.length > 0) {

			// LogBuffer.println("Selected min array index: [" +
			// selectedArrayIndexes[0] + "] Selected min gene index: [" +
			// selectedGeneIndexes[0] + "].");

			List<List<Integer>> arrayBoundaryList;
			List<List<Integer>> geneBoundaryList;

			arrayBoundaryList = findRectangleBoundaries(selectedIMVArrayIndexes,
					xmap);
			geneBoundaryList = findRectangleBoundaries(selectedIMVGeneIndexes,
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

		setIndicatorSelectionCircleBounds();
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
			// last pixel of last block
			ep = map.getPixel(selectionRange.get(selectionRange.size() - 1) + 1)
					- 1;

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

	/**
	 * Draws a circle if the user is viewing a small area to
	 * indicate the position of the view.
	 */
	private void setIndicatorVisibleCircleBounds() {

		double x = 0;
		double y = 0;
		double w = 0;
		double h = 0;

		// Width and height of rectangle which spans the Ellipse2D object
		w = interactiveXmap.getNumVisible() * xmap.getScale();
		h = interactiveYmap.getNumVisible() * ymap.getScale();

		if (w < 5 && h < 5) {

			// coords for top left of circle
			x = xmap.getPixel(interactiveXmap.getFirstVisible()) + w/2.0 - 5;
			y = ymap.getPixel(interactiveYmap.getFirstVisible()) + h/2.0 - 5;
			
			//LogBuffer.println("Circle coords: x y w h: " + x+" "+y+" "+w+" "+h);

			if (indicatorVisibleCircle == null) {
				indicatorVisibleCircle = new Ellipse2D.Double(x, y, 10, 10);

			} else {
				indicatorVisibleCircle.setFrame(x, y, 10, 10);
			}
		} else if (indicatorVisibleCircle != null) {
			indicatorVisibleCircle.setFrame(x, y, 0, 0);
		}
	}

	/**
	 * Draws a circle if the user selects one rectangle in the clustergram to
	 * indicate the position of this rectangle.
	 */
	private void setIndicatorSelectionCircleBounds() {

		double x = 0;
		double y = 0;
		double w = 0;
		double h = 0;

		//if (geneSelection == null || arraySelection == null)
		if (selectedIMVGeneIndexes == null || selectedIMVArrayIndexes == null ||
				selectedIMVGeneIndexes.length == 0 ||
				selectedIMVArrayIndexes.length == 0) {
			indicatorSelectionCircleList = null;
			return;
		} else {// if ((selectedIMVGeneIndexes.length * ymap.getScale()) < 5 &&
				// (selectedIMVArrayIndexes.length * xmap.getScale()) < 5) {

			//Empty the list of
			indicatorSelectionCircleList = new ArrayList<Ellipse2D.Double>();
			
			List<List<Integer>> arrayBoundaryList;
			List<List<Integer>> geneBoundaryList;

			arrayBoundaryList =
					findRectangleBoundaries(selectedIMVArrayIndexes,xmap);
			geneBoundaryList =
					findRectangleBoundaries(selectedIMVGeneIndexes,ymap);

			double lastxb = -1;
			double lastyb = -1;

			// Make the rectangles
			for (final List<Integer> xBoundaries : arrayBoundaryList) {

				w = (xBoundaries.get(1) - xBoundaries.get(0));

				for (final List<Integer> yBoundaries : geneBoundaryList) {

					//LogBuffer.println("Preparing to create ellipse.");

					// Width and height of rectangle which spans the Ellipse2D
					// object
					h = (yBoundaries.get(1) - yBoundaries.get(0));

					if(w < 5 && h < 5 &&
					   //This is not the first selection and the last selection
					   //is far away OR this is the first selection and the next
					   //selection either doesn't exists or is far away
					   ((lastxb >= 0 &&
					     Math.abs(xBoundaries.get(0) - lastxb) > 5) ||
					    (lastxb < 0 &&
					     (arrayBoundaryList.size() == 1 ||
					      Math.abs(arrayBoundaryList.get(1).get(0) -
					               xBoundaries.get(1)) > 5))) &&
					   ((lastyb >= 0 &&
					     Math.abs(yBoundaries.get(0) - lastyb) > 5) ||
					    (lastyb < 0 &&
					     (geneBoundaryList.size() == 1 ||
					      Math.abs(geneBoundaryList.get(1).get(0) -
					               yBoundaries.get(1)) > 5)))) {
						// coords for top left of circle
						x = xBoundaries.get(0) + (w / 2.0) - 5;
						y = yBoundaries.get(0) + (h / 2.0) - 5;

						//LogBuffer.println("Ellipse created at [" + x + "x" + y +
						//		"] and is dimensions [" + w + "x" + h + "].");

						indicatorSelectionCircleList.add(
								new Ellipse2D.Double(x, y, 10, 10));
					//} else {
					//	LogBuffer.println("Selection was too big [" + w + "x" +
					//			h + "] or [(" + xBoundaries.get(1) + " - " +
					//			xBoundaries.get(0) + ") x (" +
					//			yBoundaries.get(1) + " - " +
					//			yBoundaries.get(0) + ")].");
					}
					lastyb = yBoundaries.get(1);
				}
				lastxb = xBoundaries.get(1);
			}
		}
	}

	public void setIMVselectedIndexes(int[] selectedArrayIndexes,
									  int[] selectedGeneIndexes) {
		selectedIMVArrayIndexes = selectedArrayIndexes;
		selectedIMVGeneIndexes  = selectedGeneIndexes;

		recalculateOverlay();

		//If the selected index arrays have been defined, repaint the screen
		if(selectedIMVArrayIndexes != null && selectedIMVGeneIndexes != null) {
			repaint();
		}
	}
}
