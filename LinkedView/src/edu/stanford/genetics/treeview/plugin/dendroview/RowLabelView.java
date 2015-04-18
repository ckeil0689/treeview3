package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.UrlExtractor;

public class RowLabelView extends LabelView {

	private static final long serialVersionUID = 1L;

	public RowLabelView() {

		super(LabelView.ROW);
	}
	
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			super.setConfigNode(parentNode.node("RowLabelView"));

		} else {
			LogBuffer.println("Could not find or create ArrayameView"
					+ "node because parentNode was null.");
			return;
		}
	}

	public void generateView(final UrlExtractor uExtractor) {

		this.urlExtractor = uExtractor;

		headerSummary.setIncluded(new int[] { 0 });
		headerSummary.addObserver(this);
	}

	
	@Override
	protected void adjustScrollBar() {

		if (isRightJustified) {
			final int scrollMax = scrollPane.getHorizontalScrollBar()
					.getMaximum();
			scrollPane.getHorizontalScrollBar().setValue(scrollMax);
		} else {
			scrollPane.getHorizontalScrollBar().setValue(0);
		}

		repaint();
	}

	@Override
	public void update(final Observable o, final Object arg) {

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

		final int start = 0;
		final int end = headerInfo.getNumHeaders();
		
//		LogBuffer.println("NumHeaders: " + end);

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

	@Override
	public void mouseMoved(final MouseEvent e) {

		hoverIndex = map.getIndex(e.getY());
		repaint();
	}

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

		if (SwingUtilities.isLeftMouseButton(e)) {
			if (arraySelection.getNSelectedIndexes() == arraySelection
					.getNumIndexes() && geneSelection.isIndexSelected(index)) {
				arraySelection.deselectAllIndexes();
				geneSelection.deselectAllIndexes();

			} else if (arraySelection.getNSelectedIndexes() > 0) {
				if (!e.isShiftDown()) {
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
}
