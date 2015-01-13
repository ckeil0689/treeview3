package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Observable;

import javax.swing.JScrollBar;

import Utilities.GUIFactory;
import Utilities.StringRes;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.UrlExtractor;

public class RowLabelView extends LabelView implements LabelDisplay {

	private int col;
	
	public RowLabelView() {
		
		super(LabelView.ROW);
	}
	
	public void generateView(final UrlExtractor uExtractor) {

		this.urlExtractor = uExtractor;

		headerSummary.setIncluded(new int[] { 0 });
		headerSummary.addObserver(this);
	}
	
	@Override
	public void setJustifyOption(boolean isRightJustified) {
		
		super.setJustifyOption(isRightJustified);
		
		if(isRightJustified) {
			int scrollMax = scrollPane.getHorizontalScrollBar().getMaximum();
			scrollPane.getHorizontalScrollBar().setValue(scrollMax);
		} else {
			scrollPane.getHorizontalScrollBar().setValue(0);
		}
		
		repaint();
	}

	@Override
	public JScrollBar getMainScrollBar() {
		// TODO Auto-generated method stub
		return null;
	}

	public void generateView(final UrlExtractor uExtractor, final int col) {
		
		super.setUrlExtractor(uExtractor);
		this.col = col;

		headerSummary.setIncluded(new int[] { 0 });
		headerSummary.addObserver(this);
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

			if (out == null) continue;

			final int length = fontMetrics.stringWidth(out);
			if (maxlength < length) maxlength = length;

		}

		setPreferredSize(new Dimension(maxlength, map.getUsedPixels()));
		revalidate();
		repaint();
	}
	
	@Override
	public void updateBuffer(final Graphics g) {

		updateBuffer(g, offscreenSize);
	}

	public void updateBuffer(final Graphics g, final Dimension offscreenSize) {

		g.setColor(this.getBackground());
		g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
		g.setColor(Color.black);

		// clear the pallette...
		if (map.getScale() > 12.0) {

			zoomHint.setText("");

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

							// TODO move if outside of loop?
							if(isRightJustified) {
								g.drawString(out, offscreenSize.width
												- metrics.stringWidth(out),
										map.getMiddlePixel(j) + ascent / 2);
							} else {
								g.drawString(out, 0,
										map.getMiddlePixel(j) + ascent / 2);
							}

							if (fgColorIndex > 0) {
								g.setColor(fore);
							}
						} else {
							g.setColor(Color.black);
							if(isRightJustified) {
								g.drawString(out, offscreenSize.width
												- metrics.stringWidth(out),
										map.getMiddlePixel(j) + ascent / 2);
							} else {
								g.drawString(out, 0,
										map.getMiddlePixel(j) + ascent / 2);
							}
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
			zoomHint.setText(StringRes.lbl_ZoomRowLabels);
		}
	}

}
