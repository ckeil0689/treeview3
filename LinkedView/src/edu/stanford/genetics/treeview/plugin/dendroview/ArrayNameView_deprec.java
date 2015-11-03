/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.util.Observable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import Utilities.GUIFactory;
import Utilities.StringRes;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.RotateImageFilter;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.UrlExtractor;

/**
 * Renders the names of the arrays.
 * 
 * Actually, renders the first element in a HeaderInfo as vertical text. Could
 * easily be generalized.
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.4 $ $Date: 2010-05-02 13:39:00 $
 */
public class ArrayNameView_deprec extends ModelView implements MouseListener, 
		MouseMotionListener, FontSelectable, ConfigNodePersistent {

	private static final long serialVersionUID = 1L;

	/**
	 * HeaderInfo containing the names of the arrays.
	 */
	protected HeaderInfo headerInfo = null;
	protected HeaderSummary headerSummary = new HeaderSummary("ArraySummary");
	protected DataModel dataModel = null;

	private String face;
	private int style;
	private int size;
	private boolean isRightJustified;

	private Image backBuffer;
	private final JScrollPane scrollPane;
	private MapContainer map;

	private int maxlength = 0;
	private boolean backBufferValid = false;
	private Preferences configNode = null;
	private UrlExtractor urlExtractor = null;
	
	private TreeSelectionI arraySelection;
	private TreeSelectionI geneSelection;
	private int hoverIndex = -1;

	private final String d_face = "Dialog";
	private final int d_style = 0;
	private final int d_size = 12;
	private final boolean d_justified = false;

	private final JLabel l1;

	/**
	 * Constructs an <code>ArrayNameView</code> with the given
	 * <code>HeaderInfo</code> as a source of array names.
	 * 
	 * @param hInfo
	 *            Header containing array names as first row.
	 */
	public ArrayNameView_deprec() {

		super();
		this.setLayout(new MigLayout());
		
//		this.urlExtractor = uExtractor;
		
		addMouseMotionListener(this);
		addMouseListener(this);

		l1 = GUIFactory.createLabel("", GUIFactory.FONTS);
		add(l1, "alignx 50%, aligny 100%, push");

		scrollPane = new JScrollPane(this,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);

		panel = scrollPane;
	}
	
//	public JScrollBar getYScroll() {
//		
//		return scrollPane.getVerticalScrollBar();
//	}
	
	public void generateView(final UrlExtractor uExtractor) {

		this.urlExtractor = uExtractor;

		headerSummary.setIncluded(new int[] { 0 });
		headerSummary.addObserver(this);
	}

	public HeaderInfo getHeaderInfo() {

		return headerInfo;
	}

//	public DataModel getDataModel() {
//
//		return dataModel;
//	}

	public void setHeaderInfo(final HeaderInfo headerInfo) {

		this.headerInfo = headerInfo;
	}

	public void setDataModel(final DataModel dataModel) {

		if (dataModel != null) {
			((Observable) dataModel).deleteObserver(this);
		}

		this.dataModel = dataModel;
		((Observable) dataModel).addObserver(this);
	}

	/* inherit description */
	@Override
	public String viewName() {

		return "ArrayNameView";
	}

	// Canvas methods
	/**
	 * updates a horizontally oriented test buffer, which will later be rotated
	 * to make vertical text. This is only used in the absence of Graphics2D.
	 */
	public void updateBackBuffer() {

		final Graphics g = backBuffer.getGraphics();
		final int start = map.getIndex(0);
		final int end = map.getIndex(map.getUsedPixels()) - 1;
		final int colorIndex = headerInfo.getIndex("FGCOLOR");
		final int bgColorIndex = headerInfo.getIndex("BGCOLOR");
		int gidRow = headerInfo.getIndex("GID");

		g.setColor(this.getBackground());
		g.fillRect(0, 0, maxlength, offscreenSize.width);
		g.setColor(Color.black);

		if (gidRow == -1) {
			gidRow = 0;
		}

		g.setFont(new Font(face, style, size));
		final FontMetrics metrics = getFontMetrics(g.getFont());
		final int ascent = metrics.getAscent();

		// draw backgrounds first...
		if (bgColorIndex > 0) {
			final Color back = g.getColor();
			for (int j = start; j < end; j++) {
				final String[] strings = headerInfo.getHeader(j);
				try {
					g.setColor(TreeColorer.getColor(strings[bgColorIndex]));
				} catch (final Exception e) {
				}
				g.fillRect(0, map.getMiddlePixel(j) - ascent / 2, maxlength,
						ascent);
			}
			g.setColor(back);
		}

		final Color back = g.getColor();
		for (int j = start; j <= end; j++) {

			try {
				final String out = headerSummary.getSummary(headerInfo, j);
				final String[] headers = headerInfo.getHeader(j);

				// System.out.println("Got row " + gidRow + " value " + out);
				if (out == null) {
					continue;
				}

				if ((arraySelection == null)
						|| arraySelection.isIndexSelected(j)) {

					if (colorIndex > 0) {
						g.setColor(TreeColorer.getColor(headers[colorIndex]));
					}

					g.drawString(out, 0, map.getMiddlePixel(j) + ascent / 2);
					if (colorIndex > 0) {
						g.setColor(back);
					}
				} else {
					g.setColor(Color.gray);
					g.drawString(out, 0, map.getMiddlePixel(j) + ascent / 2);
					g.setColor(back);
				}
			} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
			}
		}
		backBuffer = RotateImageFilter.rotate(this, backBuffer);
	}

	/* inherit description */
	@Override
	public void updateBuffer(final Graphics g) {

		updateBuffer(g, offscreenSize);
	}

	public void updateBuffer(final Image buf) {

		updateBuffer(buf.getGraphics(),
				new Dimension(buf.getWidth(null), buf.getHeight(null)));
	}

	public void updateBuffer(final Graphics g, final Dimension offscreenSize) {

		g.setColor(this.getBackground());
		g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
		g.setColor(Color.black);

		if (map.getScale() > 12.0) {
			l1.setText("");

			/* This code is for java2.it's worth supporting two ways. */
			try {
				final Graphics2D g2d = (Graphics2D) g;
				final AffineTransform orig = g2d.getTransform();

				g2d.rotate(Math.PI * 3 / 2);
				g2d.translate(-offscreenSize.height, 0);

				final int start = map.getIndex(0);
				final int end = map.getIndex(map.getUsedPixels()) - 1;
				int gidRow = headerInfo.getIndex("GID");
				if (gidRow == -1) {
					gidRow = 0;
				}
				final int colorIndex = headerInfo.getIndex("FGCOLOR");
				g.setFont(new Font(face, style, size));
				final FontMetrics metrics = getFontMetrics(g.getFont());
				final int ascent = metrics.getAscent();

				// draw backgrounds first...
				final int bgColorIndex = headerInfo.getIndex("BGCOLOR");
				if (bgColorIndex > 0) {
					final Color back = g.getColor();
					for (int j = start; j <= end; j++) {
						final String[] strings = headerInfo.getHeader(j);
						try {
							g.setColor(TreeColorer
									.getColor(strings[bgColorIndex]));

						} catch (final Exception e) {
							// ingore...
						}
						g.fillRect(0, map.getMiddlePixel(j) - ascent / 2,
								offscreenSize.height, ascent);
					}
					g.setColor(back);
				}

				// Foreground Text
				final Color fore = GUIFactory.MAIN;// g.getColor();
				for (int j = start; j <= end; j++) {

					try {
						final String out = headerSummary.getSummary(headerInfo,
								j);
						final String[] headers = headerInfo.getHeader(j);
						/*
						 * String out = headers[gidRow];
						 */
						if (out != null) {
							if ((arraySelection == null)
									|| arraySelection.isIndexSelected(j)
									|| j == hoverIndex) {
								if (colorIndex > 0) {
									g.setColor(TreeColorer
											.getColor(headers[colorIndex]));
								}

								g2d.setColor(fore);
								if(isRightJustified) {
									g2d.drawString(out, offscreenSize.height
													- metrics.stringWidth(out),
											map.getMiddlePixel(j) + ascent / 2);
								} else {
									g2d.drawString(out, 0, map.getMiddlePixel(j)
											+ ascent / 2);
								}

								if (colorIndex > 0) g.setColor(fore);
								
							} else {
								g2d.setColor(Color.black);
								if(isRightJustified) {
									g2d.drawString(out, offscreenSize.height
													- metrics.stringWidth(out),
											map.getMiddlePixel(j) + ascent / 2);
								} else {
									g2d.drawString(out, 0, map.getMiddlePixel(j)
											+ ascent / 2);
								}
							}

						}
					} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
					}
				}

				g2d.setTransform(orig);

			} catch (final java.lang.NoClassDefFoundError e) {

				if (backBufferValid == false) {
					final int tstart = map.getIndex(0);
					final int tend = map.getIndex(map.getUsedPixels());
					if ((tstart >= 0 && tend > tstart)
							&& (offscreenSize.width > 0)) {

						/*
						 * Should have been done by selectionChanged() String
						 * [][] aHeaders = model.getArrayHeaders(); int gidRow =
						 * model.getGIDIndex(); int colorIndex =
						 * model.getRowIndex("FGCOLOR");
						 * 
						 * g.setFont(new Font(face, style, size)); FontMetrics
						 * metrics = getFontMetrics(g.getFont()); int ascent =
						 * metrics.getAscent();
						 * 
						 * // calculate maxlength maxlength = 1; // for some
						 * reason, stop at end -1? int start =
						 * map.getMinIndex(); int end = map.getMaxIndex(); for
						 * (int j = start;j < end;j++) { String out =
						 * aHeaders[j][gidRow]; if (out == null) continue; int
						 * length = metrics.stringWidth(out); if (maxlength <
						 * length) { maxlength = length; } }
						 */

						backBuffer = createImage(maxlength, offscreenSize.width);
						// updateBackBuffer();// this flips the backbuffer...

					} else {
						// some kind of blank default image?
					}
					backBufferValid = true;
				}

				if (offscreenSize.height < maxlength) {
					g.drawImage(backBuffer, 0, 0, null);
				} else {
					if ((g != null) && (backBuffer != null)) {
						g.drawImage(backBuffer, 0, offscreenSize.height
								- maxlength, null);
					}
				}
			}
		} else {
			l1.setText(StringRes.lbl_ZoomColLabels);
		}
	}

	// /**
	// * Sets the urlExtractor to be used when an array name is clicked on.
	// *
	// * @param ue
	// * Will be fed array indexes.
	// */
	// public void setUrlExtractor(final UrlExtractor ue) {
	//
	// urlExtractor = ue;
	// }

	/**
	 * Used to space the array names.
	 * 
	 * @param im
	 *            A new mapcontainer.
	 */
	public void setMap(final MapContainer im) {

		if (map != null) {
			map.deleteObserver(this);
		}
		map = im;
		map.addObserver(this);

		this.revalidate();
		this.repaint();
	}

	private int oldHeight = 0;

	/**
	 * This method is called when the selection is changed. It causes the
	 * component to recalculate it's width, and call repaint.
	 */
	private void selectionChanged() {

		offscreenValid = false;
		backBufferValid = false;
		
		/* Why iterate over headers but use map to set the indices........? */
		/* TODO ensure this is fixed, won't remove old code in case it's not */
//		final int start = map.getMinIndex();
//		final int end = map.getMaxIndex();
		
		final int start = 0;
		final int end = headerInfo.getNumHeaders();
		
		int gidRow = headerInfo.getIndex("GID");
		if (gidRow == -1) {
			gidRow = 0;
		}

		final FontMetrics fontMetrics = getFontMetrics(new Font(face, style,
				size));
		maxlength = 1;
		for (int j = start; j < end; j++) {

			final String out = headerSummary.getSummary(headerInfo, j);
			/*
			 * String[] headers = headerInfo.getHeader(j); String out =
			 * headers[gidRow];
			 */
			if (out == null) continue;

			final int length = fontMetrics.stringWidth(out);
			if (maxlength < length) {
				maxlength = length;
			}
		}

		final Rectangle visible = getVisibleRect();
		setPreferredSize(new Dimension(map.getUsedPixels(), maxlength));

		revalidate();
		repaint();

		if (maxlength > oldHeight) {
			// System.out.println("old height " + oldHeight
			// +" new height " + maxlength + ", visible " + visible);
			visible.y += maxlength - oldHeight;
			// System.out.println("new visible " + visible);
			scrollRectToVisible(visible);
		}
		oldHeight = maxlength;

		/*
		 * The rest is done inside paintComponent... // calculate maxlength int
		 * start = map.getIndex(0); int end = map.getIndex(map.getUsedPixels());
		 * repaint(); if (maxlength > oldHeight) { //
		 * System.out.println("old height " + oldHeight +" new height " // * +
		 * maxlength + ", visible " + visible); visible.y += maxlength -
		 * oldHeight; // System.out.println("new visible " + visible);
		 * scrollRectToVisible(visible); } oldHeight = maxlength;
		 */
	}

	// Observer
	/**
	 * Expects to see updates only from the map, when the array name spacing
	 * changes.
	 * 
	 */
	@Override
	public void update(final Observable o, final Object arg) {

		if (o == map || o == dataModel) {
			selectionChanged();

		} else if (o == arraySelection || o == geneSelection) {
			selectionChanged(); // which genes are selected changed

		} else if (o == headerSummary) { // annotation selection changed
			selectionChanged();

		} else {
			LogBuffer.println("ArrayNameView got funny update!");
		}
	}

	// MouseListener
	/**
	 * Starts external browser if the urlExtractor is enabled.
	 */
	@Override
	public void mouseClicked(final MouseEvent e) {
		// if (urlExtractor == null) {
		// return;
		// }
		//
		// if (urlExtractor.isEnabled() == false) {
		// return;
		// }
		//
		// // now, want mouse click to signal browser...
		// final int index = map.getIndex(e.getX());
		// if (map.contains(index)) {
		// viewFrame.displayURL(urlExtractor.getUrl(index));
		// }
		final int index = map.getIndex(e.getX()); 
		
		if(SwingUtilities.isLeftMouseButton(e)) {
			if (geneSelection.getNSelectedIndexes() == geneSelection
					.getNumIndexes() && arraySelection.isIndexSelected(index)) {
				geneSelection.deselectAllIndexes();
				arraySelection.deselectAllIndexes();
	
			} else if (geneSelection.getNSelectedIndexes() > 0) {
				if(!e.isShiftDown()) {
					geneSelection.deselectAllIndexes();
					arraySelection.deselectAllIndexes();
				}
				arraySelection.setIndexSelection(index, true);
				geneSelection.selectAllIndexes();
	
			} else {
				arraySelection.setIndexSelection(index, true);
				geneSelection.selectAllIndexes();
			}
		}else {
			geneSelection.deselectAllIndexes();
			arraySelection.deselectAllIndexes();
		}

		geneSelection.notifyObservers();
		arraySelection.notifyObservers();
	}
	
	@Override
	public void mouseMoved(final MouseEvent e) {
		
		hoverIndex = map.getIndex(e.getX());
		repaint();
	}
	
	@Override
	public void mouseExited(final MouseEvent e) {
		
		hoverIndex = -1;
		repaint();
	}

	// FontSelectable
	/* inherit description */
	@Override
	public String getFace() {

		return face;
	}

	/* inherit description */
	@Override
	public int getPoints() {

		return size;
	}

	/* inherit description */
	@Override
	public int getStyle() {

		return style;
	}
	
	@Override
	public boolean getJustifyOption() {
		
		return isRightJustified;
	}

	/* inherit description */
	@Override
	public void setFace(final String string) {

		if ((face == null) || (!face.equals(string))) {
			face = string;
			if (configNode != null) {
				configNode.put("face", face);
			}
			setFont(new Font(face, style, size));
			backBufferValid = false;
			repaint();
		}
	}

	/* inherit description */
	@Override
	public void setPoints(final int i) {

		if (size != i) {
			size = i;
			if (configNode != null) {
				configNode.putInt("size", size);
			}
			setFont(new Font(face, style, size));
			backBufferValid = false;
			repaint();
		}
	}

	/* inherit description */
	@Override
	public void setStyle(final int i) {

		if (style != i) {
			style = i;
			backBufferValid = false;
			if (configNode != null) {
				configNode.putInt("style", style);
			}
			setFont(new Font(face, style, size));
			repaint();
		}
	}

	@Override
	public void setJustifyOption(boolean isRightJustified) {
		
		this.isRightJustified = isRightJustified;
		
		LogBuffer.println("Setting ArrayNameView Scroll: " + isRightJustified);
		
		if (configNode != null) {
			configNode.putBoolean("colRightJustified", isRightJustified);
		}
		
		if(isRightJustified) {
			scrollPane.getVerticalScrollBar().setValue(0);
		} else {
			int scrollMax = scrollPane.getVerticalScrollBar().getMaximum();
			scrollPane.getVerticalScrollBar().setValue(scrollMax);
		}
		
		repaint();
	}
	
	public void resetJustify() {
		
		boolean isRight = false;
		if (configNode != null) {
			isRight = configNode.getBoolean("colRightJustified", isRightJustified);
		}
		
		if(isRight) {
			scrollPane.getVerticalScrollBar().setValue(0);
		} else {
			int scrollMax = scrollPane.getVerticalScrollBar().getMaximum();
			scrollPane.getVerticalScrollBar().setValue(scrollMax);
		}
		
		repaint();
	}

	/** Setter for headerSummary */
	public void setHeaderSummary(final HeaderSummary headerSummary) {

		this.headerSummary = headerSummary;
	}

	/** Getter for headerSummary */
	public HeaderSummary getHeaderSummary() {

		return headerSummary;
	}

	/* inherit description */
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node("ArrayNameView");

		} else {
			LogBuffer.println("Could not find or create ArrayameView"
					+ "node because parentNode was null.");
		}

		final String[] childrenNodes = getRootChildrenNodes();
		boolean nodePresent = false;
		for (int i = 0; i < childrenNodes.length; i++) {

			// Actually looking for children nodes...
			if (childrenNodes[i].equalsIgnoreCase("ArraySummary")) {
				nodePresent = true;
			}
		}

		if (!nodePresent) {
			getHeaderSummary().setConfigNode(configNode);
			getHeaderSummary().setIncluded(new int[] { 0 });

		} else {
			// Actually get first subNode here...
			getHeaderSummary().setConfigNode(configNode);
		}

		setFace(configNode.get("face", d_face));
		setStyle(configNode.getInt("style", d_style));
		setPoints(configNode.getInt("size", d_size));
		setJustifyOption(configNode.getBoolean("colRightJustified", d_justified));
	}

	/**
	 * Set geneSelection
	 * 
	 * @param geneSelection
	 *            The TreeSelection which is set by selecting genes in the
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
	 * Set geneSelection
	 * 
	 * @param geneSelection
	 *            The TreeSelection which is set by selecting genes in the
	 *            GlobalView
	 */
	public void setGeneSelection(final TreeSelectionI arraySelection) {

		if (this.geneSelection != null) {
			this.geneSelection.deleteObserver(this);
		}
		this.geneSelection = arraySelection;
		this.geneSelection.addObserver(this);
	}

	/**
	 * Returns the names of the current children of this class' root node.
	 * 
	 * @return
	 */
	public String[] getRootChildrenNodes() {

		if (configNode != null) {
			String[] childrenNodes;
			try {
				childrenNodes = configNode.childrenNames();
				return childrenNodes;

			} catch (final BackingStoreException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}
}
