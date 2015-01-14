package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.prefs.Preferences;

import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;

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
		
		final int start = 0;
		final int end = headerInfo.getNumHeaders();

		final FontMetrics fontMetrics = getFontMetrics(new Font(face, style,
				size));
		maxlength = 1;
		for (int j = start; j < end; j++) {

			final String out = headerSummary.getSummary(headerInfo, j);
			
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
	}
	
	@Override
	public void mouseMoved(final MouseEvent e) {
		
		hoverIndex = map.getIndex(e.getX());
		repaint();
	}
	
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
}
