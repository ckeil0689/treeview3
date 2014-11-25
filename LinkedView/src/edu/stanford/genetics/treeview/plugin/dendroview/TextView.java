/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: TextView.java,v $
 * $Revision: 1.4 $
 * $Date: 2010-05-02 13:39:00 $
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Observable;
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
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.UrlExtractor;

public class TextView extends ModelView implements ConfigNodePersistent,
		FontSelectable, KeyListener, AdjustmentListener, MouseListener,
		MouseMotionListener {

	private static final long serialVersionUID = 1L;

	protected HeaderInfo headerInfo = null;
	protected HeaderSummary headerSummary = new HeaderSummary("GeneSummary");

	private final int scrollstep = 5;
	private final String d_face = "Dialog";
	private final int d_style = 0;
	private final int d_size = 12;

	private Preferences configNode = null;

	private String face;
	private int style;
	private int size;

	private TreeSelectionI geneSelection;
	private TreeSelectionI arraySelection;
	
	private MapContainer map;
	private UrlExtractor urlExtractor;
	private int maxlength = 0;
	private int col;
	private boolean dragging = false;
	private int hoverIndex;

	private final JScrollPane scrollPane;
	private final JLabel l1;

	/**
	 * should really take a HeaderSummary instead of HeaderInfo, since the
	 * mapping should be managed by Dendroview so that the SummaryView can use
	 * the same HeaderSummary.
	 */
//	public TextView(final HeaderInfo hI, final UrlExtractor uExtractor) {
//
//		this(hI, uExtractor, -1);
//	}

	public TextView() {

		super();
		this.setLayout(new MigLayout());
		this.setOpaque(false);

		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);

		l1 = GUIFactory.createLabel("", GUIFactory.FONTS);
		add(l1, "alignx 0%, aligny 50%, push, wrap");

		scrollPane = new JScrollPane(this,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);
		panel = scrollPane;
		
		hoverIndex = -1;
	}
	
	public JScrollBar getXScroll() {
		
		return scrollPane.getHorizontalScrollBar();
	}
	
	public void generateView(final UrlExtractor uExtractor, final int col) {
		
		this.urlExtractor = uExtractor;
		this.col = col;

		// could set up headerSummary...
//		final int GIDIndex = headerInfo.getIndex("GID");
//		if (GIDIndex == -1) {
//			headerSummary.setIncluded(new int[] { 1 });
//
//		} else {
//			headerSummary.setIncluded(new int[] { 2 });
//		}
		headerSummary.setIncluded(new int[] { 0 });
		headerSummary.addObserver(this);
	}

	@Override
	public String viewName() {

		return "TextView";
	}

	// Canvas methods
	@Override
	public void updateBuffer(final Graphics g) {

		updateBuffer(g, offscreenSize);
	}

	public void updateBuffer(final Image buf) {

		final Dimension offscreenSize = new Dimension(buf.getWidth(null),
				buf.getHeight(null));
		updateBuffer(buf.getGraphics(), offscreenSize);
	}

	public void updateBuffer(final Graphics g, final Dimension offscreenSize) {

		g.setColor(this.getBackground());
		g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
		g.setColor(Color.black);

		// clear the pallette...
		if (map.getScale() > 12.0) {

			l1.setText("");

			if ((map.getMinIndex() >= 0) && (offscreenSize.height > 0)) {

				final int start = map.getIndex(0);
				final int end = map.getIndex(map.getUsedPixels());
				g.setFont(new Font(face, style, size));
				final FontMetrics metrics = getFontMetrics(g.getFont());
				final int ascent = metrics.getAscent();

				// draw backgrounds first...
				final int bgColorIndex = headerInfo.getIndex("BGCOLOR");
				if (bgColorIndex > 0) {
					final Color back = g.getColor();
					for (int j = start; j < end; j++) {
						if ((geneSelection == null)
								|| geneSelection.isIndexSelected(j)) {
							final String[] strings = headerInfo.getHeader(j);

							try {
								g.setColor(TreeColorer
										.getColor(strings[bgColorIndex]));

							} catch (final Exception e) {
								// ignore
							}
							g.fillRect(0, map.getMiddlePixel(j) - ascent / 2,
									offscreenSize.width, ascent);
						}
					}
					g.setColor(back);
				}

				// now, foreground text
				final int fgColorIndex = headerInfo.getIndex("FGCOLOR");
				for (int j = start; j < end; j++) {

					String out = null;

					if (col == -1) {
						out = headerSummary.getSummary(headerInfo, j);

					} else {
						final String[] summaryArray = headerSummary
								.getSummaryArray(headerInfo, j);

						if ((summaryArray != null)
								&& (col < summaryArray.length)) {
							out = summaryArray[col];
						}
					}

					if (out != null) {
						final Color fore = GUIFactory.MAIN; // g.getColor();
						if ((geneSelection == null)
								|| geneSelection.isIndexSelected(j)
								|| j == hoverIndex) {
							final String[] strings = headerInfo.getHeader(j);

							if (fgColorIndex > 0) {
								g.setColor(TreeColorer
										.getColor(strings[fgColorIndex]));
							}

							g.setColor(fore);

							// left-aligned text
							g.drawString(
									out,
									0,
									//This is how you would do right-aligned text
									//offscreenSize.width
									//		- metrics.stringWidth(out),
									map.getMiddlePixel(j) + ascent / 2);

							if (fgColorIndex > 0) {
								g.setColor(fore);
							}
						} else {
							g.setColor(Color.black);
							// left-aligned text
							g.drawString(
									out,
									0,
									//This is how you would do right-aligned text
									//offscreenSize.width
									//		- metrics.stringWidth(out),// 0,
									map.getMiddlePixel(j) + ascent / 2);
							// g.setColor(fore);
						}

						// g2d.translate(offscreenSize.height, 0);
						// g2d.rotate(Math.PI / 2);
					}
				}
			} else {
				// some kind of blank default image?
				// backG.drawString("Select something already!", 0,
				// offscreenSize.height / 2 );
			}
		} else {
			l1.setText(StringRes.lbl_ZoomRowLabels);
		}
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

	public void setMap(final MapContainer im) {

		if (map != null) {
			map.deleteObserver(this);
		}

		map = im;
		if (map != null) {
			map.addObserver(this);
		}

		this.revalidate();
		this.repaint();
	}

	/**
	 * This method is called when the selection is changed. It causes the
	 * component to recalculate it's width, and call repaint.
	 */
	private void selectionChanged() {

		maxlength = 1;
		final FontMetrics fontMetrics = getFontMetrics(new Font(face, style,
				size));
		
		/* Why iterate over headers but use map to set the indices........? */
		/* TODO ensure this is fixed, won't remove old code in case it's not */
//		final int start = map.getIndex(0);
//		final int end = map.getIndex(map.getUsedPixels());
		
		final int start = 0;
		final int end = headerInfo.getNumHeaders();

		for (int j = start; j < end; j++) {

			final int actualGene = j;
			final String out = headerSummary.getSummary(headerInfo, actualGene);

			if (out == null) {
				continue;
			}

			final int length = fontMetrics.stringWidth(out);
			if (maxlength < length) {
				maxlength = length;
			}
		}

		setPreferredSize(new Dimension(maxlength, map.getUsedPixels()));
		revalidate();
		repaint();
	}

	public UrlExtractor getUrlExtractor() {

		return urlExtractor;
	}

	// Observer
	@Override
	public void update(final Observable o, final Object arg) {

		if (o == map) {
			selectionChanged(); // gene locations changed

		} else if (o == geneSelection || o == arraySelection) {
			selectionChanged(); // which genes are selected changed

		} else if (o == headerSummary) { // annotation selection changed
			selectionChanged();

		} else {
			System.out.println("Textview got funny update!");
		}
	}

	// MouseListener
	@Override
	public void mouseClicked(final MouseEvent e) {

		// if (urlExtractor == null) {
		// return;
		// }
		//
		// urlExtractor.setEnabled(true);
		//
		// if (urlExtractor.isEnabled() == false) {
		// return;
		// }
		//
		// // now, want mouse click to signal browser...
		// final int index = map.getIndex(e.getY());
		// if (map.contains(index)) {
		// if (col != -1) {
		// viewFrame.displayURL(urlExtractor.getUrl(index,
		// headerInfo.getNames()[col]));
		//
		// } else {
		// viewFrame.displayURL(urlExtractor.getUrl(index));
		// }
		// }
		final int index = map.getIndex(e.getY());

		if(SwingUtilities.isLeftMouseButton(e)) {
			if (arraySelection.getNSelectedIndexes() == arraySelection
					.getNumIndexes() && geneSelection.isIndexSelected(index)) {
				arraySelection.deselectAllIndexes();
				geneSelection.deselectAllIndexes();
	
			} else if (arraySelection.getNSelectedIndexes() > 0) {
				if(!e.isShiftDown()) {
					arraySelection.deselectAllIndexes();
					geneSelection.deselectAllIndexes();
				}
				geneSelection.setIndexSelection(index, true);
				arraySelection.selectAllIndexes();
	
			} else {
				geneSelection.setIndexSelection(index, true);
				arraySelection.selectAllIndexes();
			}
		} else {
			geneSelection.deselectAllIndexes();
			arraySelection.deselectAllIndexes();
		}

		arraySelection.notifyObservers();
		geneSelection.notifyObservers();
	}

	// MouseMotionListener
	@Override
	public void mousePressed(final MouseEvent e) {

		dragging = true;
	}

	@Override
	public void mouseDragged(final MouseEvent e) {

		if (dragging) {
			
		}
	}

	@Override
	public void mouseReleased(final MouseEvent e) {

		dragging = false;
	}
	
	@Override
	public void mouseMoved(final MouseEvent e) {
		
		hoverIndex = map.getIndex(e.getY());
		repaint();
	}
	
	@Override
	public void mouseExited(final MouseEvent e) {
		
		hoverIndex = -1;
		repaint();
	}

	// KeyListener
	@Override
	public void keyPressed(final KeyEvent e) {

		int xoff = 0;// scrollbar.getValue();
		final int c = e.getKeyCode();

		switch (c) {

		case KeyEvent.VK_UP:
			break;

		case KeyEvent.VK_DOWN:
			break;

		case KeyEvent.VK_LEFT:
			xoff -= scrollstep;
			break;

		case KeyEvent.VK_RIGHT:
			xoff += scrollstep;
			break;

		default:
			return;
		}

		adjustScrollbar(xoff);
	}

	// AdjustmentListener
	@Override
	public void adjustmentValueChanged(final AdjustmentEvent evt) {

		offscreenValid = false;
		revalidate();
		repaint();
	}

	private void adjustScrollbar(final int offset) {

		// scrollbar.setValue(offset);
		offscreenValid = false;
		revalidate();
		repaint();
	}

	// FontSelectable
	@Override
	public String getFace() {

		return face;
	}

	@Override
	public int getPoints() {

		return size;
	}

	@Override
	public int getStyle() {

		return style;
	}

	@Override
	public void setFace(final String string) {

		if ((face == null) || (!face.equals(string))) {
			face = string;

			if (configNode != null) {
				configNode.put("face", face);
			}

			setFont(new Font(face, style, size));
			revalidate();
			repaint();
		}
	}

	@Override
	public void setPoints(final int i) {

		if (size != i) {
			size = i;

			if (configNode != null) {
				configNode.putInt("size", size);
			}

			setFont(new Font(face, style, size));
			revalidate();
			repaint();
		}
	}

	@Override
	public void setStyle(final int i) {

		if (style != i) {
			style = i;

			if (configNode != null) {
				configNode.putInt("style", style);
			}

			setFont(new Font(face, style, size));
			revalidate();
			repaint();
		}
	}

	// public void bindConfig(final Preferences configNode) {
	//
	// root = configNode;
	//
	// setFace(root.get("face", d_face));
	// setStyle(root.getInt("style", d_style));
	// setPoints(root.getInt("size", d_size));
	//
	// // getHeaderSummary().bindConfig(root.fetchOrCreate("GeneSummary"));
	// getHeaderSummary().setConfigNode("GeneSummary");
	// }
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node("TextView");

		} else {
			LogBuffer.println("Could not find or create TextView "
					+ "node because parentNode was null.");
		}

		setFace(configNode.get("face", d_face));
		setStyle(configNode.getInt("style", d_style));
		setPoints(configNode.getInt("size", d_size));

		// getHeaderSummary().bindConfig(root.fetchOrCreate("GeneSummary"));
		getHeaderSummary().setConfigNode(configNode);
	}

	/** Setter for headerSummary */
	public void setHeaderSummary(final HeaderSummary headerSummary) {

		this.headerSummary = headerSummary;
	}

	/** Getter for headerSummary */
	public HeaderSummary getHeaderSummary() {

		return headerSummary;
	}
	
	public void setHeaderInfo(HeaderInfo geneHI) {
		
		this.headerInfo = geneHI;
	}
}
