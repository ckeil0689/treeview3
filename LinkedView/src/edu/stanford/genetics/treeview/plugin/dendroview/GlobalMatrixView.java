package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Observable;

import edu.stanford.genetics.treeview.LogBuffer;

public class GlobalMatrixView extends MatrixView {

	/**
	 * Default so warings don't pop up...
	 */
	private static final long serialVersionUID = 1L;
	
	private final Rectangle viewPortRect = new Rectangle();

	public GlobalMatrixView() {
		
		super();
		
	}
	
//	@Override
//	public synchronized void paintComposite(final Graphics g) {
/* TODO draw viewport rectangle here so user will know where in the 
 * matrix he is.
 */
//		if (selectionRectList != null) {
//
//			/* draw all selection rectangles in yellow */
//			g.setColor(Color.yellow);
//
//			for (final Rectangle rect : selectionRectList) {
//				g.drawRect(rect.x, rect.y, rect.width, rect.height);
//			}
//		}
//	}
	
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
		
		LogBuffer.println("GlobalMatrixView got update : " + o);
		
		if ((o == xmap) || o == ymap) {
			recalculateOverlay();
			offscreenValid = false;

		} else if (o == drawer && drawer != null) {
			/*
			 * signal from drawer means that it need to draw something
			 * different.
			 */
			offscreenValid = false;

		} else {
			LogBuffer.println("GlobalMatrixView got weird update : " + o);
		}

		revalidate();
		repaint();
	}
	
	@Override
	public String viewName() {

		return "GlobalMatrixView";
	}
	
	/**
	 * Checks the current view of rows and columns and calculates 
	 * the appropriate viewport rectangle.
	 */
	protected void recalculateOverlay() {
		
		int xFirst = xmap.getFirstVisible();
		int xLast = xFirst + xmap.getNumVisible();
		
		LogBuffer.println("xFirst: " + xFirst);
		LogBuffer.println("xLast: " + xLast);
	}
}
