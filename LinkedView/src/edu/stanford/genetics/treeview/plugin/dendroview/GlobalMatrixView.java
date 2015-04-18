package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
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
	
	private final Rectangle viewPortRect = new Rectangle();

	/**
	 * Circle to be used as indicator for selection
	 */
	private Ellipse2D.Double indicatorCircle = null;

	public GlobalMatrixView() {
		
		super();
	}
	
	@Override
	public synchronized void paintComposite(final Graphics g) {
		if (viewPortRect != null) {
			/* draw all selection rectangles in yellow */
			g.setColor(Color.white);

			g.drawRect(viewPortRect.x, viewPortRect.y, viewPortRect.width, 
					viewPortRect.height);

			/*
			 * draw white selection circle if only 1 tile is selected and small
			 * enough.
			 */
			if (indicatorCircle != null) {
				final Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(Color.white);
				g2.setStroke(new BasicStroke(2));
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

		setIndicatorCircleBounds();
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

		if (interactiveXmap.getNumVisible() < 5 &&
				interactiveYmap.getNumVisible() < 5) {

			// Width and height of rectangle which spans the Ellipse2D object
			w = interactiveXmap.getNumVisible() * xmap.getScale();
			h = interactiveYmap.getNumVisible() * ymap.getScale();

			// coords for top left of circle
			x = xmap.getPixel(interactiveXmap.getFirstVisible()) + w/2.0 - 5;
			y = ymap.getPixel(interactiveYmap.getFirstVisible()) + w/2.0 - 5;
			
			//LogBuffer.println("Circle coords: x y w h: " + x+" "+y+" "+w+" "+h);

			if (indicatorCircle == null) {
				indicatorCircle = new Ellipse2D.Double(x, y, 10, 10);

			} else {
				indicatorCircle.setFrame(x, y, 10, 10);
			}
		} else if (indicatorCircle != null) {
			indicatorCircle.setFrame(x, y, 0, 0);
		}
	}

}
