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
import java.awt.Dimension;
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

import javax.swing.BorderFactory;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ModelViewProduced;
import edu.stanford.genetics.treeview.TreeSelectionI;

public class GlobalView extends ModelViewProduced implements
MouseWheelListener {

	private static final long serialVersionUID = 1L;

	public static final int FILL = 0;
	public static final int EQUAL = 1;
	public static final int PROPORT = 2;

	protected boolean hasDrawn = false;
	private boolean resetHome = false;
	protected TreeSelectionI geneSelection;
	protected TreeSelectionI arraySelection;
	protected MapContainer xmap;
	protected MapContainer ymap;
	private final String[] statustext = new String[] { "Mouseover Selection",
			"", "" };
	private HeaderInfo arrayHI;
	private HeaderInfo geneHI;

	private HeaderSummary geneSummary;
	private HeaderSummary arraySummary;

	private ArrayDrawer drawer;
	private int overx;
	private int overy;
	private final JScrollPane scrollPane;

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
	public GlobalView() {

		super();

		setLayout(new MigLayout());

		scrollPane = new JScrollPane(this,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

		panel = scrollPane;
		panel.setBorder(BorderFactory.createEtchedBorder());
		panel.setBackground(GUIFactory.ELEMENT_HOV);

		// setToolTipText("This Turns Tooltips On");

		addMouseListener(new MatrixMouseListener());
		addMouseMotionListener(new MatrixMouseListener());
		addMouseWheelListener(this);
	}

	public JScrollBar getXScroll() {

		return scrollPane.getHorizontalScrollBar();
	}

	public JScrollBar getYScroll() {

		return scrollPane.getVerticalScrollBar();
	}

	@Override
	public Dimension getPreferredSize() {

		final Dimension p = new Dimension(xmap.getRequiredPixels(),
				ymap.getRequiredPixels());
		return p;
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
	
	public void setObserver() {
		
	}

	/**
	 * Set geneSelection
	 *
	 * @param geneSelection
	 *            The TreeSelection which is set by selecting genes in the
	 *            GlobalView
	 */
	public void setGeneSelection(final TreeSelectionI geneSelection) {

		if (this.geneSelection != null) {
			this.geneSelection.deleteObserver(this);
		}

		this.geneSelection = geneSelection;
		this.geneSelection.addObserver(this);
	}

	/**
	 * Set arraySelection
	 *
	 * @param arraySelection
	 *            The TreeSelection which is set by selecting arrays in the
	 *            GlobalView
	 */
	public void setArraySelection(final TreeSelectionI arraySelection) {

		if (this.arraySelection != null) {
			this.arraySelection.deleteObserver(this);
		}

		this.arraySelection = arraySelection;
		this.arraySelection.addObserver(this);
	}

	/**
	 * Set ArrayDrawer
	 *
	 * @param arrayDrawer
	 *            The ArrayDrawer to be used as a source
	 */
	public void setArrayDrawer(final ArrayDrawer arrayDrawer) {

		if (drawer != null) {
			drawer.deleteObserver(this);
		}

		drawer = arrayDrawer;
		drawer.addObserver(this);
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

	/**
	 * DEPRECATE set the xmapping for this view
	 *
	 * @param m
	 *            the new mapping
	 */
	public void setXMap(final MapContainer m) {

		if (xmap != null) {
			xmap.deleteObserver(this);
		}

		xmap = m;
		xmap.addObserver(this);
	}

	/**
	 * DEPRECATE set the ymapping for this view
	 *
	 * @param m
	 *            the new mapping
	 */
	public void setYMap(final MapContainer m) {

		if (ymap != null) {
			ymap.deleteObserver(this);
		}

		ymap = m;
		ymap.addObserver(this);
	}

	@Override
	public String viewName() {

		return "GlobalView";
	}

	// Canvas Methods
	/**
	 * This method updates the graphics object directly by asking the
	 * ArrayDrawer to draw on it directly. The alternative is to have a pixel
	 * buffer which you update using updatePixels.
	 */
	@Override
	protected void updateBuffer(final Graphics g) {

		if (offscreenChanged) {
			xmap.setAvailablePixels(offscreenSize.width);
			ymap.setAvailablePixels(offscreenSize.height);

			if (!hasDrawn) {
				// total kludge, but addnotify isn't working correctly...
				xmap.recalculateScale();
				ymap.recalculateScale();
				hasDrawn = true;
			}

			xmap.notifyObservers();
			ymap.notifyObservers();
		}

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

		if (offscreenChanged) {
			// LogBuffer.println("OFFSCREEN CHANGED");
			offscreenValid = false;
			xmap.setAvailablePixels(offscreenSize.width);
			ymap.setAvailablePixels(offscreenSize.height);

			if (!hasDrawn) {
				// total kludge, but addnotify isn't working correctly...
				xmap.recalculateScale();
				ymap.recalculateScale();
				hasDrawn = true;
			}
		}

		if (resetHome) {
			LogBuffer.println("Resetting GV");
			xmap.setHome();
			ymap.setHome();

			resetHome(false);
		}

		if (!offscreenValid) {
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

			repaint();
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

	// Observer Methods
	@Override
	public void update(final Observable o, final Object arg) {

		if (o == geneSelection) {
			if (arraySelection.getNSelectedIndexes() == 0) {
				if (geneSelection.getNSelectedIndexes() != 0) {
					// select all arrays if some genes selected...
					arraySelection.selectAllIndexes();
					// notifies self...
					arraySelection.notifyObservers();
					return;
				}
				/*
				 * When deselecting a tree node with right click, this matters,
				 * because in the eventlistener you can only deselect indices
				 * for the local tree selection. other axis here!
				 */
			} else if (geneSelection.getNSelectedIndexes() == 0) {
				arraySelection.deselectAllIndexes();
				arraySelection.notifyObservers();
			}
			recalculateOverlay();
			drawIndicatorCircle();

			/* trigger updatePixels() */
			offscreenValid = false;

		} else if (o == arraySelection) {
			if (geneSelection.getNSelectedIndexes() == 0) {
				if (arraySelection.getNSelectedIndexes() != 0) {
					// select all genes if some arrays selected...
					geneSelection.selectAllIndexes();
					// notifies self...
					geneSelection.notifyObservers();
					return;
				}
				/*
				 * When deselecting a tree node with right click, this matters,
				 * because in the eventlistener you can only deselect indices
				 * for the local tree selection. other axis here!
				 */
			} else if (arraySelection.getNSelectedIndexes() == 0) {
				geneSelection.deselectAllIndexes();
				geneSelection.notifyObservers();
			}
			recalculateOverlay();
			drawIndicatorCircle();

			offscreenValid = false;

		} else if ((o == xmap) || o == ymap) {
			recalculateOverlay();
			drawIndicatorCircle();
			offscreenValid = false;

		} else if (o == drawer && drawer != null) {
			/*
			 * signal from drawer means that it need to draw something
			 * different.
			 */
			offscreenValid = false;

		} else {
			LogBuffer.println("GlobalView got weird update : " + o);
		}

		revalidate();
		repaint();
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
	 * Zooming when the mouse wheel is used in conjunction with the shift key.
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
				xmap.zoomIn();
				ymap.zoomIn();
			} else {
				xmap.zoomOut();
				ymap.zoomOut();
			}
		} else if (e.isShiftDown()) {
			xmap.scrollBy(shift);
		} else {
			ymap.scrollBy(shift);
		}

		revalidate();
		repaint();
	}

	// @Override
	// public String getToolTipText(final MouseEvent e) {
	// /*
	// * Do we want to do mouseovers if value already visible? if
	// * (getShowVal()) return null; // don't do tooltips and vals at same
	// * time.
	// */
	// String ret = "";
	// String row = "";
	// String col = "";
	//
	// if (drawer != null) {
	//
	// final int geneRow = overy;
	//
	// if (xmap.contains(overx) && ymap.contains(overy)) {
	//
	// if (geneHI != null) {
	// row = geneSummary.getSummary(geneHI, overy);
	//
	// } else {
	// row = "N/A";
	// }
	//
	// if (arrayHI != null) {
	// col = arraySummary.getSummary(arrayHI, overx);
	//
	// } else {
	// col = "N/A";
	// }
	//
	// if (drawer.isMissing(overx, geneRow)) {
	// ret = "No data";
	//
	// } else if (drawer.isEmpty(overx, geneRow)) {
	// ret = null;
	//
	// } else {
	// ret = "<html>Row: " + row + " <br>Column: " + col
	// + " <br>Value: "
	// + drawer.getSummary(overx, geneRow) + "</html>";
	// }
	// }
	// }
	// return ret;
	// }

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
	private void drawIndicatorCircle() {

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

			// Get coords for center of circle
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

	public void resetHome(final boolean resized) {

		this.resetHome = resized;
	}
}
