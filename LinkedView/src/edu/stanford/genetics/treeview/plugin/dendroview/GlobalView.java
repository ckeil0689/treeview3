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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import java.util.Observable;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ModelViewProduced;
import edu.stanford.genetics.treeview.TreeSelectionI;

class GlobalView extends ModelViewProduced implements MouseMotionListener,
		MouseListener, MouseWheelListener, KeyListener {

	private static final long serialVersionUID = 1L;

	protected boolean hasDrawn = false;
	protected TreeSelectionI geneSelection;
	protected TreeSelectionI arraySelection;
	protected MapContainer xmap;
	protected MapContainer ymap;
	protected MapContainer zoomXmap;
	protected MapContainer zoomYmap;
	private final String[] statustext = new String[] { "Mouseover Selection",
			"", "" };
	private HeaderInfo arrayHI;
	private HeaderInfo geneHI;
	
	private ArrayDrawer drawer;
	private int overx;
	private int overy;
	private int rowLabelCol;
	private int colLabelCol;
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
	private Rectangle selectionRect = null;

	/**
	 * Circle to be used as indicator for selection
	 */
	private Ellipse2D.Double indicatorCircle = null;

	/**
	 * Rectangle to track blue zoom rectangle (pixels)
	 */
	// private Rectangle zoomRect = null;

	/**
	 * GlobalView also likes to have an globalxmap and globalymap (both of type
	 * MapContainer) to help it figure out where to draw things. It also tries
	 * to
	 */
	public GlobalView() {

		super();
		// panel = this;

		setLayout(new MigLayout());
		
		// Column indexes for statuspanel display
		rowLabelCol = 1;
		colLabelCol = 0;

		scrollPane = new JScrollPane(panel,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		setToolTipText("This Turns Tooltips On");

		//addComponentListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
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

	// @Override
	// public String[] getStatus() {
	//
	// String [] status = new String[4];
	// if ((geneSelection == null) || (arraySelection == null)) {
	// status[0] = "ERROR: GlobalView improperly configured";
	// status[1] = " geneSelection is null";
	// status[2] = " thus, gene selection will not work.";
	// status[3] = "";
	//
	// } else {
	// int sx = arraySelection.getMinIndex();
	// int ex = arraySelection.getMaxIndex();
	//
	// int sy = geneSelection.getMinIndex();
	// int ey = geneSelection.getMaxIndex();
	//
	// status[0] = (ey - sy + 1) + " genes selected";
	// status[1] = (ex - sx + 1) + " arrays selected";
	// status[2] = "Genes from " + sy + " to " + ey;
	// status[3] = "Arrays from " + sx + " to " + ex;
	// }
	//
	// return status;
	// }

	@Override
	public String[] getStatus() {

		try {
			if (xmap.contains(overx) && ymap.contains(overy)) {
				statustext[0] = "Row: ";// + (overy + 1);

				if (geneHI != null) {
					final int realGene = overy;
					try {
						statustext[0] += geneHI.getHeader(realGene, 
								rowLabelCol);

					} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
						statustext[0] += " (N/A)";
					}
				}
				statustext[1] = "Column: ";// + (overx + 1);
				if (arrayHI != null) {
					try {
						statustext[1] += arrayHI.getHeader(overx, colLabelCol);

					} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
						statustext[1] += " (N/A)";
					}
				}

				if (drawer.isMissing(overx, overy)) {
					statustext[2] = "Value:  No Data";

				} else if (drawer.isEmpty(overx, overy)) {
					statustext[2] = "";

				} else {
					statustext[2] = "Value:  "
							+ drawer.getSummary(overx, overy);
				}
			}
		} catch (final ArrayIndexOutOfBoundsException ex) {
			// ignore silently?
		}
		return statustext;
	}
	
	/**
	 * Sets the two integers which define the labels that are being
	 * shown by the status panel.
	 * @param row
	 * @param col
	 */
	public void setStatusLabelInds(int row, int col) {
		
		rowLabelCol = row;
		colLabelCol = col;
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

	// /**
	// * get the xmapping for this view
	// *
	// * @return the current mapping
	// */
	// public MapContainer getXMap() {
	//
	// return xmap;
	// }
	//
	// /**
	// * get the ymapping for this view
	// *
	// * @return the current mapping
	// */
	// public MapContainer getYMap() {
	//
	// return ymap;
	// }

	// /** DEPRECATE
	// * set the xmapping for this view
	// *
	// * @param m the new mapping
	// */
	// public void setZoomXMap(MapContainer m) {
	//
	// if (zoomXmap != null) {
	// zoomXmap.deleteObserver(this);
	// }
	//
	// zoomXmap = m;
	// zoomXmap.addObserver(this);
	// }
	//
	// /** DEPRECATE
	// * set the ymapping for this view
	// *
	// * @param m the new mapping
	// */
	// public void setZoomYMap(MapContainer m) {
	//
	// if (zoomYmap != null) {
	// zoomYmap.deleteObserver(this);
	// }
	//
	// zoomYmap = m;
	// zoomYmap.addObserver(this);
	// }

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

			if (hasDrawn == false) {
				// total kludge, but addnotify isn't working correctly...
				xmap.recalculateScale();
				ymap.recalculateScale();
				hasDrawn = true;
			}

			xmap.notifyObservers();
			ymap.notifyObservers();
		}

		if (offscreenValid == false) {
			// clear the pallette...
			g.setColor(Color.white);
			g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
			g.setColor(Color.black);

			final Rectangle destRect = new Rectangle(0, 0,
					xmap.getUsedPixels(), ymap.getUsedPixels());

			final Rectangle sourceRect = new Rectangle(xmap.getIndex(0),
					ymap.getIndex(0), xmap.getIndex(destRect.width)
							- xmap.getIndex(0), ymap.getIndex(destRect.height)
							- ymap.getIndex(0));

			if ((sourceRect.x >= 0) && (sourceRect.y >= 0)) {
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
			offscreenValid = false;
			xmap.setAvailablePixels(offscreenSize.width);
			ymap.setAvailablePixels(offscreenSize.height);

			if (hasDrawn == false) {
				// total kludge, but addnotify isn't working correctly...
				xmap.recalculateScale();
				ymap.recalculateScale();
				hasDrawn = true;
			}

			xmap.notifyObservers();
			ymap.notifyObservers();
		}

		if (offscreenValid == false) {
			final Rectangle destRect = new Rectangle(0, 0,
					xmap.getUsedPixels(), ymap.getUsedPixels());

			final Rectangle sourceRect = new Rectangle(xmap.getIndex(0),
					ymap.getIndex(0), xmap.getIndex(destRect.width)
							- xmap.getIndex(0), ymap.getIndex(destRect.height)
							- ymap.getIndex(0));

			if ((sourceRect.x >= 0) && (sourceRect.y >= 0)) {
				drawer.paint(offscreenPixels, sourceRect, destRect,
						offscreenScanSize);
			}

			offscreenSource.newPixels();
		}
	}

	@Override
	public synchronized void paintComposite(final Graphics g) {

		// composite the rectangles...
		if (selectionRect != null) {
			// if (zoomRect != null) {
			// g.setColor(Color.cyan);
			// g.drawRect(zoomRect.x, zoomRect.y,
			// zoomRect.width, zoomRect.height);
			// }

			g.setColor(Color.yellow);
			g.drawRect(selectionRect.x, selectionRect.y, selectionRect.width,
					selectionRect.height);

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

	protected void recalculateOverlay() {

		if ((geneSelection == null) || (arraySelection == null)) {
			return;
		}

		int spx, spy, epx, epy;
		spx = xmap.getPixel(arraySelection.getMinIndex());
		// last pixel of last block
		epx = xmap.getPixel(arraySelection.getMaxIndex() + 1) - 1;

		spy = ymap.getPixel(geneSelection.getMinIndex());
		epy = ymap.getPixel(geneSelection.getMaxIndex() + 1) - 1;

		if (epy < spy) {
			epy = spy;
			// correct for roundoff error above
		}

		if (selectionRect == null) {
			selectionRect = new Rectangle(spx, spy, epx - spx, epy - spy);

		} else {
			selectionRect.setBounds(spx, spy, epx - spx, epy - spy);
		}

		this.revalidate();
		this.repaint();
	}

	protected void recalculateZoom() {

		if (selectionRect == null) {
			return;
		}

		// int spx, epx, spy, epy;
		// try {
		// spx = xmap.getPixel(zoomXmap.getIndex(0));
		// epx = xmap.getPixel(zoomXmap.getIndex(
		// zoomXmap.getUsedPixels())) - 1;
		//
		// spy = ymap.getPixel(zoomYmap.getIndex(0));
		// epy = ymap.getPixel(zoomYmap.getIndex(
		// zoomYmap.getUsedPixels())) - 1;
		//
		// } catch (java.lang.ArithmeticException e) {
		// // silently ignore div zero exceptions, which arise when
		// // some dimension is zero and fillmap is selected...
		// return;
		// }

		// if (zoomRect == null) {
		// zoomRect = new Rectangle(spx, spy, epx - spx, epy - spy);
		//
		// } else {
		// zoomRect.setBounds(spx, spy, epx - spx, epy - spy);
		// }
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
			}
			recalculateOverlay();
			drawIndicatorCircle();

		} else if (o == arraySelection) {
			if (geneSelection.getNSelectedIndexes() == 0) {
				if (arraySelection.getNSelectedIndexes() != 0) {
					// select all genes if some arrays selected...
					geneSelection.selectAllIndexes();
					// notifies self...
					geneSelection.notifyObservers();
					return;
				}
			}
			recalculateOverlay();
			drawIndicatorCircle();

		} else if ((o == xmap) || o == ymap) {
			recalculateZoom(); // it moves around, you see...
			recalculateOverlay();
			drawIndicatorCircle();
			offscreenValid = false;

		} else if ((o == zoomYmap) || (o == zoomXmap)) {
			recalculateZoom();
			/*
			 * if (o == zoomXmap) { if ((zoomYmap.getUsedPixels() == 0) &&
			 * (zoomXmap.getUsedPixels() != 0)) {
			 * zoomYmap.setIndexRange(ymap.getMinIndex(), ymap.getMaxIndex());
			 * zoomYmap.notifyObservers(); } } else if (o == zoomYmap) { if
			 * ((zoomXmap.getUsedPixels() == 0) && (zoomYmap.getUsedPixels() !=
			 * 0)) { zoomXmap.setIndexRange(xmap.getMinIndex(),
			 * xmap.getMaxIndex()); zoomXmap.notifyObservers(); } }
			 */
			if ((status != null) && hasMouse) {
				status.setMessages(getStatus());
			}

		} else if (o == drawer) {
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

	// Mouse Listener
	@Override
	public void mousePressed(final MouseEvent e) {

		if (enclosingWindow().isActive() == false) {
			return;
		}

		// if left button is used
		if (SwingUtilities.isLeftMouseButton(e)) {
			startPoint.setLocation(xmap.getIndex(e.getX()),
					ymap.getIndex(e.getY()));
			endPoint.setLocation(startPoint.x, startPoint.y);
			dragRect.setLocation(startPoint.x, startPoint.y);
			dragRect.setSize(endPoint.x - dragRect.x, endPoint.y - dragRect.y);

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
	public void mouseExited(final MouseEvent e) {
		
		hasMouse = false;
		
		// Display empty field
		statustext[0] = "";
		statustext[1] = "";
		statustext[2] = "";
		
		status.setMessages(statustext);
	}

	@Override
	public void mouseReleased(final MouseEvent e) {

		if (enclosingWindow().isActive() == false) {
			return;
		}

		// When left button is used
		if (SwingUtilities.isLeftMouseButton(e)) {
			mouseDragged(e);
			drawBand(dragRect);

			if (e.isShiftDown()) {
				final Point start = new Point(xmap.getMinIndex(), startPoint.y);
				final Point end = new Point(xmap.getMaxIndex(), endPoint.y);
				selectRectangle(start, end);

			} else if (e.isControlDown()) {
				final Point start = new Point(startPoint.x, ymap.getMinIndex());
				final Point end = new Point(endPoint.x, ymap.getMaxIndex());
				selectRectangle(start, end);
				
			} else {
				selectRectangle(startPoint, endPoint);
			}
			
		} else {
			// do something else?
		}

		revalidate();
		repaint();
	}

	// MouseMotionListener
	@Override
	public void mouseDragged(final MouseEvent e) {

		// When left button is used
		if (SwingUtilities.isLeftMouseButton(e)) {
			// rubber band?
			drawBand(dragRect);
			endPoint.setLocation(xmap.getIndex(e.getX()),
					ymap.getIndex(e.getY()));

			if (e.isShiftDown()) {
				dragRect.setLocation(xmap.getMinIndex(), startPoint.y);
				dragRect.setSize(0, 0);
				dragRect.add(xmap.getMaxIndex(), endPoint.y);

			} else {
				dragRect.setLocation(startPoint.x, startPoint.y);
				dragRect.setSize(0, 0);
				dragRect.add(endPoint.x, endPoint.y);
			}

			drawBand(dragRect);
		}
	}

	@Override
	public void mouseMoved(final MouseEvent e) {

		final int ooverx = overx;
		final int oovery = overy;
		overx = xmap.getIndex(e.getX());
		overy = ymap.getIndex(e.getY());
		if (oovery != overy || ooverx != overx) {
			if (status != null) {
				status.setMessages(getStatus());
			}
		}
	}

	@Override
	public String getToolTipText(final MouseEvent e) {
		/*
		 * Do we want to do mouseovers if value already visible? if
		 * (getShowVal()) return null; // don't do tooltips and vals at same
		 * time.
		 */
		String ret = "";
		String row = "";
		String col = "";

		if (drawer != null) {

			final int geneRow = overy;
			final int geneCol = overx;

			if (xmap.contains(overx) && ymap.contains(overy)) {

				if (geneHI != null) {
					row = geneHI.getHeader(geneRow, 1);

				} else {
					row = "N/A";
				}

				if (arrayHI != null) {
					col = arrayHI.getHeader(geneCol, 0);

				} else {
					col = "N/A";
				}

				if (drawer.isMissing(overx, geneRow)) {
					ret = "No data";

				} else if (drawer.isEmpty(overx, geneRow)) {
					ret = null;

				} else {
					ret = "Row: " + row + " Column: " + col + " Value: "
							+ drawer.getSummary(overx, geneRow);
				}
			}
		}
		return ret;
	}

	private void drawBand(final Rectangle l) {

		final Graphics g = getGraphics();
		g.setXORMode(getBackground());
		g.setColor(GUIParams.MAIN);

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

		if (geneSelection == null || arraySelection == null) {
			return;

		} else if ((geneSelection.getNSelectedIndexes() == 1 && arraySelection
				.getNSelectedIndexes() == 1)
				&& (xmap.getScale() < 10.0 && ymap.getScale() < 10.0)) {

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

	// KeyListener
	@Override
	public void keyPressed(final KeyEvent e) {

		final int c = e.getKeyCode();
		int shift;

		if (e.isShiftDown()) {
			shift = 10;

		} else {
			shift = 1;
		}

		switch (c) {
		case KeyEvent.VK_LEFT:
			xmap.scrollBy(-shift);
			break;
		case KeyEvent.VK_RIGHT:
			xmap.scrollBy(shift);
			break;
		case KeyEvent.VK_UP:
			ymap.scrollBy(-shift);
			break;
		case KeyEvent.VK_DOWN:
			ymap.scrollBy(shift);
			break;
		case KeyEvent.VK_MINUS:
			xmap.zoomOut();
			ymap.zoomOut();
			break;
		case KeyEvent.VK_EQUALS:
			xmap.zoomIn();
			ymap.zoomIn();
			break;
		}

		this.revalidate();
		this.repaint();
	}

	/**
	 * Zooming when the mouse wheel is used in conjunction with the shift key.
	 * Vertical scrolling if the shift key is not pressed.
	 */
	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {

		final int notches = e.getWheelRotation();
		final int shift = 3;

		if (!e.isShiftDown()) {
			if (notches < 0) {
				ymap.scrollBy(-shift);

			} else {
				ymap.scrollBy(shift);
			}
		} else {
			if (notches < 0) {
				xmap.zoomIn();
				ymap.zoomIn();

			} else {
				xmap.zoomOut();
				ymap.zoomOut();
			}
		}

		this.revalidate();
		this.repaint();
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

			geneSelection.setIndex(i, true);
		}

		// arrays...
		arraySelection.setSelectedNode(null);
		arraySelection.deselectAllIndexes();

		for (int i = start.x; i <= end.x; i++) {

			arraySelection.setIndex(i, true);
		}

		geneSelection.notifyObservers();
		arraySelection.notifyObservers();
	}

	/**
	 * Uses the array- and geneSelection and currently available pixels on
	 * screen retrieved from the MapContainer objects to calculate a new scale
	 * and zoom in on it by working in conjunction with centerSelection().
	 */
	public void zoomSelection() {

		double newScale = 0.0;
		double newScale2 = 0.0;

		final int arrayIndexes = arraySelection.getNSelectedIndexes();
		final int geneIndexes = geneSelection.getNSelectedIndexes();

		if (arrayIndexes > 0 && geneIndexes > 0) {
			newScale = xmap.getAvailablePixels() / arrayIndexes;
			
			if (newScale < xmap.getMinScale()) {
				newScale = xmap.getMinScale();
			}
			xmap.setScale(newScale);

			newScale2 = ymap.getAvailablePixels() / geneIndexes;
			
			if (newScale2 < ymap.getMinScale()) {
				newScale2 = ymap.getMinScale();
			}
			ymap.setScale(newScale2);
		}
	}

	/**
	 * Scrolls to the center of the selected rectangle
	 */
	public void centerSelection() {

		int scrollX;
		int scrollY;

		if (startPoint != null && endPoint != null) {
			scrollX = (endPoint.x + startPoint.x) / 2;
			scrollY = (endPoint.y + startPoint.y) / 2;

			xmap.scrollToIndex(scrollX);
			ymap.scrollToIndex(scrollY);
		}
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
