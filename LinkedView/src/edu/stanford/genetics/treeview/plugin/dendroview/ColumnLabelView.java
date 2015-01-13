package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Observable;
import java.util.prefs.Preferences;

import javax.swing.JScrollBar;

import Utilities.GUIFactory;
import Utilities.StringRes;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.UrlExtractor;

public class ColumnLabelView extends LabelView implements LabelDisplay {

	private static final long serialVersionUID = 1L;
	
	private int oldHeight = 0;

	public ColumnLabelView() {
		
		super(LabelView.COL);
	}

	@Override
	public JScrollBar getMainScrollBar() {
		// TODO Auto-generated method stub
		return null;
	}

	public void generateView(final UrlExtractor uExtractor) {

		super.setUrlExtractor(uExtractor);

		headerSummary.setIncluded(new int[] { 0 });
		headerSummary.addObserver(this);
	}
	
	@Override
	public void setJustifyOption(boolean isRightJustified) {
		
		super.setJustifyOption(isRightJustified);
		
		if(isRightJustified) {
			scrollPane.getVerticalScrollBar().setValue(0);
		} else {
			int scrollMax = scrollPane.getVerticalScrollBar().getMaximum();
			scrollPane.getVerticalScrollBar().setValue(scrollMax);
		}
		
		repaint();
	}
	
	@Override
	public void setConfigNode(Preferences parentNode) {
		
		if (parentNode != null) {
			super.setConfigNode(parentNode.node("ColLabelView"));

		} else {
			LogBuffer.println("Could not find or create ArrayameView"
					+ "node because parentNode was null.");
		}
	}
	
	@Override
	public void update(Observable o, Object arg) {
		
		if (o == map) {
			selectionChanged(); // gene locations changed

		} else if (o == geneSelection || o == arraySelection) {
			selectionChanged(); // which genes are selected changed

		} else if (o == headerSummary) { // annotation selection changed
			selectionChanged();

		} else {
			LogBuffer.println("LabelView got funny update!");
		}
	}
	
	/**
	 * This method is called when the selection is changed. It causes the
	 * component to recalculate it's width, and call repaint.
	 */
	protected void selectionChanged() {

		offscreenValid = false;
//		backBufferValid = false;
		
		/* Why iterate over headers but use map to set the indices........? */
		/* TODO ensure this is fixed, won't remove old code in case it's not */
//		final int start = map.getMinIndex();
//		final int end = map.getMaxIndex();
		
		final int start = 0;
		final int end = headerInfo.getNumHeaders();
		
		int gidRow = headerInfo.getIndex("GID");
		if (gidRow == -1) gidRow = 0;


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
			if (maxlength < length) maxlength = length;
		}

		final Rectangle visible = getVisibleRect();
		setPreferredSize(new Dimension(map.getUsedPixels(), maxlength));

		revalidate();
		repaint();

		if (maxlength > oldHeight) {
			visible.y += maxlength - oldHeight;
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
	
	/* inherit description */
	@Override
	public void updateBuffer(final Graphics g) {

		updateBuffer(g, offscreenSize);
	}

	public void updateBuffer(final Graphics g, final Dimension offscreenSize) {

		g.setColor(this.getBackground());
		g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
		g.setColor(Color.black);

		if (map.getScale() > 12.0) {
			zoomHint.setText("");

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

		} else {
			zoomHint.setText(StringRes.lbl_ZoomColLabels);
		}
	}
}
