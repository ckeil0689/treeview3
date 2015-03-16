package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Observable;

import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeSelectionI;

public class GlobalMatrixView extends MatrixView {

	/**
	 * Default so warings don't pop up...
	 */
	private static final long serialVersionUID = 1L;
	
	/* 
	 * Also needs reference to interactive MapContainers to get knowledge
	 * about which specific tiles are currently visible and update the 
	 * viewport rectangle through the observer pattern.
	 */
	private MapContainer interactiveXmap;
	private MapContainer interactiveYmap;
	
	private final Rectangle viewPortRect = new Rectangle();

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
		}
	}
	
	/**
	 * This method updates a pixel buffer. The alternative is to update the
	 * graphics object directly by calling updateBuffer.
	 */
	@Override
	protected void updatePixels() {

		revalidateScreen();

		if (!offscreenValid) {
			// LogBuffer.println("OFFSCREEN INVALID");
			final Rectangle destRect = new Rectangle(0, 0,
					xmap.getUsedPixels(), ymap.getUsedPixels());

			final Rectangle sourceRect = new Rectangle(xmap.getIndex(0),
					ymap.getIndex(0), xmap.getMaxIndex(), ymap.getMaxIndex());

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

		} else if (o == drawer && drawer != null) {
			/*
			 * signal from drawer means that it need to draw something
			 * different.
			 */
			offscreenValid = false;
			
		} else if(o == xmap || o == ymap) {
			revalidateScreen();
			
		} else if(o instanceof TreeSelectionI) {
			return;
			
		} else {
			LogBuffer.println("GlobalMatrixView got weird update : " + o);
			return;
		}

		revalidate();
		repaint();
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
	 * Checks the current view of rows and columns and calculates 
	 * the appropriate viewport rectangle.
	 */
	protected void recalculateOverlay() {
		
		int xFirst = interactiveXmap.getFirstVisible();
		int xLast = xFirst + interactiveXmap.getNumVisible();
		
		int yFirst = interactiveYmap.getFirstVisible();
		int yLast = yFirst + interactiveYmap.getNumVisible();
		
		int spx = xmap.getPixel(xFirst);
		int epx = xmap.getPixel(xLast + 1) - 1;
		int spy = ymap.getPixel(yFirst);
		int epy = ymap.getPixel(yLast + 1) - 1;

		if (epx < spx) {
			epx = spx;
			// correct for roundoff error above
		}

		if (epy < spy) {
			epy = spy;
			// correct for roundoff error above
		}
		
		viewPortRect.setBounds(spx, spy, epx - spx, epy - spy);
	}
}
