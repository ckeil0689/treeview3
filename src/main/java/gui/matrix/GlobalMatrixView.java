package gui.matrix;

import model.data.trees.TreeSelection;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class GlobalMatrixView extends MatrixView {

	/**
	 * Default so warnings don't pop up...
	 */
	private static final long serialVersionUID = 1L;

	private final int MAP_SIZE_LIMIT = 300;
	private final int MIN_VIEWPORT_SIZE = 1;
	
	/**
	 * Keep track of IMV viewport. TODO change to direct rectangle
	 * pixel calcs should not be in this class.
	 */
	private int imv_firstXVisible;
	private int imv_firstYVisible;
	private int imv_numXVisible;
	private int imv_numYVisible;

	private int xViewMin;
	private int yViewMin;

	/**
	 * White rectangle to display IMV viewport.
	 */
	private final Rectangle viewPortRect = new Rectangle();

	/**
	 * List of all yellow selection rectangles.
	 */
	private List<Rectangle> selectionRectList = new ArrayList<Rectangle>();

	/**
	 * Circle to be used as indicator for small visible area 
	 * in interactive matrix.
	 */
	private Ellipse2D.Double indicatorVisibleCircle = null;

	/**
	 * List of circles to be used as indicator for small selection area 
	 * in interactive matrix.
	 */
	private List<Ellipse2D.Double> indicatorSelectionCircleList = null;

	public GlobalMatrixView() {

		super();
	}

	@Override
	public synchronized void paintComposite(final Graphics g) {
		
		final Graphics2D g2 = (Graphics2D) g;
		
		/* White viewport rectangle */
		if (viewPortRect != null) {
			g2.setColor(Color.white);

			g2.drawRect(viewPortRect.x, viewPortRect.y, viewPortRect.width,
				viewPortRect.height);
		}
		
		/* yellow selection rectangles */
		if (selectionRectList != null) {

			g2.setColor(Color.yellow);

			for (final Rectangle rect : selectionRectList) {
				g2.drawRect(rect.x, rect.y, rect.width, rect.height);
			}
		}
		
		/* indicator circles */
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(new BasicStroke(2));
		
		/* viewport circle */
		if (viewPortRect != null) {
			g2.setColor(Color.white);
			
			if (indicatorVisibleCircle != null) {
				g2.draw(indicatorVisibleCircle);
			}
		}
		
		/* selection circles */
		if (indicatorSelectionCircleList != null) {
			g2.setColor(Color.yellow);
			
			for(Ellipse2D.Double indicatorCircle : indicatorSelectionCircleList) {
				g2.draw(indicatorCircle);
			}
		}
	}

	@Override
	public void update(Observable o, Object arg) {

		if (o instanceof MapContainer) {
			recalculateOverlay();
			offscreenValid = false;
			repaint();
			
		} else if(o instanceof TreeSelection) {
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
	 * Checks the current view of rows and columns and calculates the
	 * appropriate viewport rectangle.
	 */
	@Override
	protected void recalculateOverlay() {
		
		setViewportRectBounds();
		setIndicatorVisibleCircleBounds();

		if ((rowSelection == null) || (colSelection == null)) {
			selectionRectList = null;
			indicatorSelectionCircleList = null;
			return;
		}

		setSelectionRectangles();
		setIndicatorSelectionCircleBounds();
	}
	
	/**
	 * Sets up all rectangles graphically indicating row/ column index 
	 * selection.
	 */
	private void setSelectionRectangles() {
		
		this.selectionRectList = new ArrayList<Rectangle>();
		
		if (colSelection.getSelectedIndexes().length > 0) {

			List<List<Integer>> arrayBoundaryList;
			List<List<Integer>> geneBoundaryList;

			arrayBoundaryList = findRectangleBoundaries(
					colSelection.getSelectedIndexes(), xmap);
			geneBoundaryList = findRectangleBoundaries(
					rowSelection.getSelectedIndexes(), ymap);

			// Make the rectangles
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
	
	/**
	 * Determine boundaries of the viewport indicator rectangle based on
	 * the visible tiles in the InteractiveMatrixView.
	 */
	private void setViewportRectBounds() {
		
		/* Assure minimum draw size for viewport rectangle */
		int xRange;
		if(imv_numXVisible > xViewMin) {
			xRange = imv_numXVisible;
		} else {
			xRange = xViewMin;
		}

		int yRange;
		if(imv_numYVisible > yViewMin) {
			yRange = imv_numYVisible;
		} else {
			yRange = yViewMin;
		}

		int xFirst = imv_firstXVisible;
		int xLast = xFirst + (xRange - 1);

		int yFirst = imv_firstYVisible;
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
		w = imv_numXVisible * xmap.getScale();
		h = imv_numYVisible * ymap.getScale();

		if (w < 5 && h < 5) {

			// coords for top left of circle
			x = xmap.getPixel(imv_firstXVisible) + w / 2.0 - 5;
			y = ymap.getPixel(imv_firstYVisible) + h / 2.0 - 5;

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

		if (rowSelection == null || colSelection == null) {
			indicatorSelectionCircleList = null;
			return;
		}
		
		//Empty the list of
		indicatorSelectionCircleList = new ArrayList<Ellipse2D.Double>();

		List<List<Integer>> arrayBoundaryList;
		List<List<Integer>> geneBoundaryList;

		arrayBoundaryList = findRectangleBoundaries(
				colSelection.getSelectedIndexes(), xmap);
		geneBoundaryList = findRectangleBoundaries(
				rowSelection.getSelectedIndexes(), ymap);

		/*
		 * TODO: Instead of just checking the last(/next) selection
		 * position, should group all small selections together too see if
		 * the components.cluster is smaller than our limits.
		 */
		double lastxb = -1;

		// Make the rectangles
		for (final List<Integer> xBoundaries : arrayBoundaryList) {

			/*
			 * TODO: Instead of just checking the last(/next) selection
			 * position, should group all small selections together too see
			 * if the components.cluster is smaller than our limits.
			 */
			double lastyb = -1;
			w = (xBoundaries.get(1) - xBoundaries.get(0));

			for (final List<Integer> yBoundaries : geneBoundaryList) {

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

					indicatorSelectionCircleList.add(
							new Ellipse2D.Double(x, y, 10, 10));
				}
				lastyb = yBoundaries.get(1);
			}
			lastxb = xBoundaries.get(1);
		}
	}
	
	/**
	 * Let GlobalMatrixView know the viewport boundaries of 
	 * InteractiveMatrixView without the need for full MapContainer dependence.
	 * Called via MatrixViewController update().
	 * 
	 * @param firstXVisible Index of first visible tile in IMV xmap. 
	 * @param firstYVisible Index of first visible tile in IMV ymap.
	 * @param numXVisible Number of visible tiles in IMV xmap.
	 * @param numYVisible Number of visible tiles in IMV ymap.
	 */
	public void setIMVViewportRange(final int firstXVisible, 
			final int firstYVisible, final int numXVisible, 
			final int numYVisible) {

		this.imv_firstXVisible = firstXVisible;
		this.imv_firstYVisible = firstYVisible;
		this.imv_numXVisible = numXVisible;
		this.imv_numYVisible = numYVisible;

		recalculateOverlay();
		offscreenValid = false;	
		repaint();
	}

	/**
	 * This is called when the underlying data has changed.  It updates the x
	 * and y view minimums for the viewport rectangle.  A larger minimum is
	 * needed for large datasets so that the rectangle is visible.
	 */
	@Override
	protected void updateMatrix() {
		super.updateMatrix();

		if(xmap.getMaxIndex() > MAP_SIZE_LIMIT) {
			this.xViewMin = (xmap.getMaxIndex() / 100) * 2;

		} else {
			this.xViewMin = MIN_VIEWPORT_SIZE;
		}

		if(ymap.getMaxIndex() > MAP_SIZE_LIMIT) {
			this.yViewMin = (ymap.getMaxIndex() / 100) * 2;

		} else {
			this.yViewMin = MIN_VIEWPORT_SIZE;
		}
	}
}
