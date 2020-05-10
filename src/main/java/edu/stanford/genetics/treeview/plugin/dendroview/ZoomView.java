/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Observable;

import javax.swing.JOptionPane;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ModelViewProduced;
import edu.stanford.genetics.treeview.TreeSelectionI;

/**
 * Implements zoomed in view of the data array
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version Alpha
 * 
 * 
 *          The zoom view listens for mouse motion so that it can report status
 *          and usage appropriately.
 */
class ZoomView extends ModelViewProduced implements MouseMotionListener,
		MouseWheelListener, KeyListener, ComponentListener {

	private static final long serialVersionUID = 1L;

	protected TreeSelectionI geneSelection, arraySelection;
	private int overx;
	private int overy;
	private ArrayDrawer drawer;
	private final String[] statustext = new String[] { "Mouseover Selection",
			"", "", "Active Map: Zoom" };
	private final Rectangle sourceRect = new Rectangle();
	private final Rectangle destRect = new Rectangle();
	private MapContainer xmap;
	private MapContainer ymap;
	private HeaderInfo arrayHI;
	private HeaderInfo geneHI; // to get gene and array names...

	/**
	 * Allocate a new ZoomView
	 */
	public ZoomView() {

		super();
		this.setLayout(new MigLayout());
		panel = this;

		setToolTipText("This Turns Tooltips On");
		addComponentListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
	}

	/**
	 * showVal indicates whether the zoom should draw the value of each cell on
	 * the canvas on top of the corresponding square. Used to display IUPAC
	 * symbols for alignment view.
	 */
	boolean showVal = false;

	/**
	 * showVal indicates whether the zoom should draw the value of each cell on
	 * the canvas on top of the corresponding square. Used to display IUPAC
	 * symbols for alignment view.
	 */
	public boolean getShowVal() {

		return showVal;
	}

	/**
	 * showVal indicates whether the zoom should draw the value of each cell on
	 * the canvas on top of the corresponding square. Used to display IUPAC
	 * symbols for alignment view.
	 */
	public void setShowVal(final boolean showVal) {

		this.showVal = showVal;
	}

	@Override
	public Dimension getPreferredSize() {

		// return super.getPreferredSize();
		return new Dimension(xmap.getRequiredPixels(), ymap.getRequiredPixels());
	}

	/**
	 * Set geneSelection
	 * 
	 * @param geneSelection
	 *            The TreeSelection which is set by selecting genes in the
	 *            ZoomView
	 */
	public void setGeneSelection(final TreeSelectionI geneSelection) {

		if (this.geneSelection != null) {
			this.geneSelection.deleteObserver(this);
		}

		this.geneSelection = geneSelection;
		if (this.geneSelection != null) {
			this.geneSelection.addObserver(this);
		}
	}

	/**
	 * Set arraySelection
	 * 
	 * @param arraySelection
	 *            The TreeSelection which is set by selecting genes in the
	 *            ZoomView
	 */
	public void setArraySelection(final TreeSelectionI arraySelection) {

		if (this.arraySelection != null) {
			this.arraySelection.deleteObserver(this);
		}

		this.arraySelection = arraySelection;
		if (this.arraySelection != null) {
			this.arraySelection.addObserver(this);
		}
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
		if (drawer != null) {
			drawer.addObserver(this);
		}
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
	 * set the xmapping for this view
	 * 
	 * @param m
	 *            the new mapping
	 */
	public void setXMap(final MapContainer m) {

		if (xmap != null) {
			xmap.deleteObserver(this);
		}

		xmap = m;
		if (xmap != null) {
			xmap.addObserver(this);
		}
	}

	/**
	 * set the ymapping for this view
	 * 
	 * @param m
	 *            the new mapping
	 */
	public void setYMap(final MapContainer m) {

		if (ymap != null) {
			ymap.deleteObserver(this);
		}

		ymap = m;
		if (ymap != null) {
			ymap.addObserver(this);
		}
	}

	/**
	 * get the xmapping for this view
	 * 
	 * @return the current mapping
	 */
	public MapContainer getXMap() {

		return xmap;
	}

	public MapContainer getZoomXMap() {

		return xmap;
	}

	/**
	 * get the ymapping for this view
	 * 
	 * @return the current mapping
	 */
	public MapContainer getYMap() {

		return ymap;
	}

	public MapContainer getZoomYMap() {

		return ymap;
	}

	// method from ModelView
	@Override
	public String viewName() {

		return "ZoomView";
	}

	// method from ModelView
	@Override
	public String[] getStatus() {

		try {
			if (xmap.contains(overx) && ymap.contains(overy)) {
				statustext[0] = "Row:    " + (overy + 1);

				if (geneHI != null) {
					final int realGene = overy;
					try {
						statustext[0] += " (" + geneHI.getHeader(realGene, 1) // WRONG
																				// FOR
																				// .TXT
								+ ")";

					} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
						statustext[0] += " (N/A)";
					}
				}
				statustext[1] = "Column: " + (overx + 1);
				if (arrayHI != null) {
					try {
						statustext[1] += " (" + arrayHI.getHeader(overx, 0)
								+ ")";

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

	// method from ModelView
	@Override
	public void updateBuffer(final Graphics g) {

		if (offscreenChanged) {
			xmap.setAvailablePixels(offscreenSize.width);
			ymap.setAvailablePixels(offscreenSize.height);
			xmap.notifyObservers();
			ymap.notifyObservers();
		}

		if (offscreenValid == false) {

			// clear the pallette...
			g.setColor(Color.white);
			g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
			g.setColor(Color.black);

			destRect.setBounds(0, 0, xmap.getUsedPixels(), ymap.getUsedPixels());

			sourceRect.setBounds(xmap.getIndex(0), ymap.getIndex(0),
					xmap.getIndex(destRect.width) - xmap.getIndex(0),
					ymap.getIndex(destRect.height) - ymap.getIndex(0));

			if ((sourceRect.x >= 0) && (sourceRect.y >= 0)) {
				drawer.paint(g, sourceRect, destRect, null);
			}
		}
	}

	@Override
	public void paintComposite(final Graphics g) {

		if (getShowVal()) {
			// need to draw values on screen!
			try {
				((CharArrayDrawer) drawer).paintChars(g, xmap, ymap, destRect);

			} catch (final Exception e) {
				JOptionPane.showMessageDialog(this,
						"ZoomView had trouble compositing:" + e);
				setShowVal(false);
			}
		}
	}

	@Override
	public void updatePixels() {


		if (offscreenChanged) {
			xmap.setAvailablePixels(offscreenSize.width);
			ymap.setAvailablePixels(offscreenSize.height);
			xmap.notifyObservers();
			ymap.notifyObservers();
		}

		if (offscreenValid == false) {

			destRect.setBounds(0, 0, xmap.getUsedPixels(), ymap.getUsedPixels());

			sourceRect.setBounds(xmap.getIndex(0), ymap.getIndex(0),
					xmap.getIndex(destRect.width) - xmap.getIndex(0),
					ymap.getIndex(destRect.height) - ymap.getIndex(0));

			if ((sourceRect.x >= 0) && (sourceRect.y >= 0)) {
				drawer.paint(offscreenPixels, sourceRect, destRect,
						offscreenScanSize);
			}
			offscreenSource.newPixels();
		}
	}

	/**
	 * Watch for updates from ArrayDrawer and the two maps The appropriate
	 * response for both is to trigger a redraw.
	 */
	@Override
	public void update(final Observable o, final Object arg) {

		if (o == drawer) {
			// System.out.println("got drawer update");
			offscreenValid = false;

		} else if ((o == xmap) || (o == ymap)) {
			offscreenValid = false;

		} else if ((o == geneSelection) || (o == arraySelection)) {
			/*
			 * if (cdtSelection.getNSelectedArrays() == 0) { if
			 * (cdtSelection.getNSelectedGenes() != 0) {
			 * cdtSelection.selectAllArrays(); cdtSelection.notifyObservers(); }
			 * } else {
			 */
			// Hmm... it almost seems like you could get rid of the
			// zoom map as a mechanism of communication... but not quite,
			// because the globalview, textview and atrzview depend on it
			// to know what is visible in the zoom window.
			final MapContainer zoomXmap = getZoomXMap();
			final MapContainer zoomYmap = getZoomYMap();
			zoomYmap.setIndexRange(geneSelection.getMinIndex(),
					geneSelection.getMaxIndex());
			zoomXmap.setIndexRange(arraySelection.getMinIndex(),
					arraySelection.getMaxIndex());

			zoomXmap.notifyObservers();
			zoomYmap.notifyObservers();

		} else {
			LogBuffer.println("ZoomView got weird update : " + o);
		}

		if (offscreenValid == false) {
			repaint();
		}
	}

	// MouseMotionListener
	@Override
	public void mouseMoved(final MouseEvent e) {

		final int ooverx = overx;
		final int oovery = overy;
		overx = xmap.getIndex(e.getX());
		overy = ymap.getIndex(e.getY());
		if (oovery != overy || ooverx != overx)
			if (status != null)
				status.setMessages(getStatus());
	}

	Point mousePt;

	@Override
	public void mousePressed(final MouseEvent e) {

		mousePt = e.getPoint();
		this.setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	@Override
	public void mouseReleased(final MouseEvent e) {

		this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	@Override
	public String getToolTipText(final MouseEvent e) {
		/*
		 * Do we want to do mouseovers if value already visible? if
		 * (getShowVal()) return null; // don't do tooltips and vals at same
		 * time.
		 */
		String ret = "";
		if (drawer != null) {
			final int geneRow = overy;
			if (xmap.contains(overx) && ymap.contains(overy)) {
				if (drawer.isMissing(overx, geneRow)) {
					ret = "No data";
				} else if (drawer.isEmpty(overx, geneRow)) {
					ret = null;
				} else {
					final int row = geneRow + 1;
					final int col = overx + 1;
					ret = "Row: " + row + " Column: " + col + " Value: "
							+ drawer.getSummary(overx, geneRow);
				}
			}
		}
		return ret;
	}

	public void setHeaders(final HeaderInfo ghi, final HeaderInfo ahi) {

		geneHI = ghi;
		arrayHI = ahi;
	}

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
			getXMap().scrollBy(-shift);
			break;
		case KeyEvent.VK_RIGHT:
			getXMap().scrollBy(shift);
			break;
		case KeyEvent.VK_UP:
			getYMap().scrollBy(-shift);
			break;
		case KeyEvent.VK_DOWN:
			getYMap().scrollBy(shift);
			break;
		case KeyEvent.VK_MINUS:
			// getXMap().zoomOut(shift);
			// getYMap().zoomOut(shift);
			break;
		case KeyEvent.VK_EQUALS:
			// getXMap().zoomIn(shift);
			// getYMap().zoomIn(shift);
			break;
		}

		this.revalidate();
		this.repaint();
	}

	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {

		final int notches = e.getWheelRotation();
		final int shift = 3;

		if (!e.isShiftDown()) {
			if (notches < 0) {
				getZoomYMap().scrollBy(-shift);

			} else {
				getZoomYMap().scrollBy(shift);
			}
		} else {
			if (notches < 0) {
				// getZoomXMap().zoomIn(shift);
				// getZoomYMap().zoomIn(shift);

			} else {
				// getZoomXMap().zoomOut(shift);
				// getZoomYMap().zoomOut(shift);
			}
		}

		this.revalidate();
		this.repaint();
	}

	// Component Listeners
	@Override
	public void componentHidden(final ComponentEvent e) {
	}

	@Override
	public void componentMoved(final ComponentEvent e) {
	}

	@Override
	public void componentResized(final ComponentEvent e) {

		double scaleFactorX = 1.0;
		double scaleFactorY = 1.0;

		if (xmap.getMinScale() > 0.0) {
			scaleFactorX = xmap.getScale() / xmap.getMinScale();
		}
		xmap.setHome();
		xmap.setScale(xmap.getMinScale() * scaleFactorX);

		if (ymap.getMinScale() > 0.0) {
			scaleFactorY = ymap.getScale() / ymap.getMinScale();
		}
		ymap.setHome();
		ymap.setScale(ymap.getMinScale() * scaleFactorY);

	}

	@Override
	public void componentShown(final ComponentEvent e) {
	}
}
