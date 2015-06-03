package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Observable;
import java.util.prefs.Preferences;

import javax.swing.JScrollBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import Utilities.StringRes;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.UrlExtractor;

public class ColumnLabelView extends LabelView implements MouseWheelListener {

	private static final long serialVersionUID = 1L;

	private int oldHeight = 0;

	public ColumnLabelView() {

		super(LabelView.COL);
		d_justified = false;
		zoomHint = StringRes.lbl_ZoomColLabels;
		addMouseWheelListener(this);
	}

	public void generateView(final UrlExtractor uExtractor) {

		super.setUrlExtractor(uExtractor);

		headerSummary.setIncluded(new int[] { 0 });
		headerSummary.addObserver(this);
	}
	
	@Override
	protected void adjustScrollBar() {

		if (isRightJustified) {
			scrollPane.getVerticalScrollBar().setValue(0);
		} else {
			final int scrollMax = scrollPane.getVerticalScrollBar()
					.getMaximum();
			scrollPane.getVerticalScrollBar().setValue(scrollMax);
		}

		repaint();
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			super.setConfigNode(parentNode.node("ColLabelView"));

		} else {
			LogBuffer.println("Could not find or create ArrayameView"
					+ "node because parentNode was null.");
			return;
		}
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

		offscreenValid = false;

		final int start = 0;
		final int end = headerInfo.getNumHeaders();

		final FontMetrics fontMetrics = getFontMetrics(new Font(face, style,
				size));
		maxlength = 1;
		for (int j = start; j < end; j++) {

			final String out = headerSummary.getSummary(headerInfo, j);

			if (out == null) {
				continue;
			}

			final int length = fontMetrics.stringWidth(out);
			if (maxlength < length) {
				maxlength = length;
			}
		}

		final Rectangle visible = getVisibleRect();
		setPreferredSize(new Dimension(map.getUsedPixels(), maxlength));

		if (maxlength > oldHeight) {
			visible.y += maxlength - oldHeight;
			scrollRectToVisible(visible);
		}
		oldHeight = maxlength;
		
		revalidate();
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

		if (SwingUtilities.isLeftMouseButton(e)) {
			if (geneSelection.getNSelectedIndexes() > 0) {
				if(e.isShiftDown()) {
					toggleSelectFromClosestToIndex(arraySelection,index);
				} else if(e.isMetaDown()) {
					toggleSelect(arraySelection,index);
				} else {
					selectAnew(arraySelection,index);
				}
			} else {
				//Assumes there is no selection at all
				arraySelection.setIndexSelection(index, true);
				geneSelection.selectAllIndexes();
			}
		} else {
			geneSelection.deselectAllIndexes();
			arraySelection.deselectAllIndexes();
		}

		geneSelection.notifyObservers();
		arraySelection.notifyObservers();
	}

	/** TODO: This needs to go into a generic selection class and then the
	 * TreeSelectionI param can be removed */
	public void toggleSelectFromClosestToIndex(TreeSelectionI selection,
											   int index) {
		//If this index is selected (implying other selections may exist),
		//deselect from closest deselected to sent index
		if(selection.isIndexSelected(index)) {

			int closest = -1;
			for(int i = 0;i < selection.getNumIndexes();i++) {
				if(!selection.isIndexSelected(i) &&
				   ((closest == -1 &&
				     Math.abs(i - index) <
				     Math.abs(0 - index)) ||
				    (closest > -1 &&
				     Math.abs(i - index) <
				     Math.abs(closest - index)))) {
					closest = i;
					//LogBuffer.println("Closest index updated to [" +
					//		closest + "] because index [" + index +
					//		"] is closer [distance: " +
					//		Math.abs(i - index) + "] to it.");
				} else if(i == (selection.getNumIndexes() - 1) &&
						  selection.isIndexSelected(i) &&
						  ((closest == -1 &&
						    Math.abs(i - index) <
						    Math.abs(0 - index)) ||
						   (closest > -1 &&
						    Math.abs(i - index) <
						    Math.abs(closest - index)))) {
					closest = i + 1;
				}
			}
			//LogBuffer.println("Closest index: [" + closest + "].");
			if(closest < index) {
				for(int i = closest + 1;i <= index;i++)
					selection.setIndexSelection(i,false);
			} else {
				for(int i = index;i < closest;i++)
					selection.setIndexSelection(i,false);
			}
		}
		//Else if other selections exist (implied that current index is not
		//selected), select from sent index to closest selected
		else if(selection.getNSelectedIndexes() > 0) {
			int[] selArrays = selection.getSelectedIndexes();
			int closest = selArrays[0];
			for(int i = 0;i < selArrays.length;i++) {
				if(Math.abs(selArrays[i] - index) <
				   Math.abs(closest - index)) {
					closest = selArrays[i];
					//LogBuffer.println("Closest index updated to [" +
					//		closest + "] because index [" + index +
					//		"] is closer [distance: " +
					//		Math.abs(selArrays[i] - index) + "] to it.");
				}
			}
			if(closest < index) {
				for(int i = closest + 1;i <= index;i++) {
					selection.setIndexSelection(i, true);
				}
			} else {
				for(int i = index;i < closest;i++) {
					selection.setIndexSelection(i, true);
				}
			}
		}
		//Else when no selections exist, just select this index
		else {
			selection.deselectAllIndexes();
			selection.setIndexSelection(index, true);
		}
	}

	/** TODO: This needs to go into a generic selection class and then the
	 * TreeSelectionI param can be removed */
	public void toggleSelect(TreeSelectionI selection,int index) {
		if(selection.isIndexSelected(index))
			selection.setIndexSelection(index, false);
		else
			selection.setIndexSelection(index, true);
	}

	/** TODO: This needs to go into a generic selection class and then the
	 * TreeSelectionI param can be removed */
	public void selectAnew(TreeSelectionI selection,int index) {
		selection.deselectAllIndexes();
		selection.setIndexSelection(index, true);
	}

	@Override
	public JScrollBar getPrimaryScrollBar() {
		return scrollPane.getHorizontalScrollBar();
	}

	@Override
	public JScrollBar getSecondaryScrollBar() {
		return scrollPane.getVerticalScrollBar();
	}

	public void setSecondaryScrollBarPolicyAlways() {
		scrollPane.setHorizontalScrollBarPolicy(
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
	}

	public void setSecondaryScrollBarPolicyNever() {
		scrollPane.setHorizontalScrollBarPolicy(
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	}

	public void setSecondaryScrollBarPolicyAsNeeded() {
		scrollPane.setHorizontalScrollBarPolicy(
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}

	public boolean isJustifiedToMatrixEdge() {
		return(!isRightJustified);
	}

	public int getPrimaryHoverPosition(final MouseEvent e) {
		return(e.getX());
	}

	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {

		final int notches = e.getWheelRotation();
		final int shift = (notches < 0) ? -3 : 3;

		LogBuffer.println("Detected [" + (e.isShiftDown() ? "horizontal" : "vertical") + "] scroll event");
		// On macs' magic mouse, horizontal scroll comes in as if the shift was
		// down
		if(e.isShiftDown()) {
			map.scrollBy(shift, false);
		} else {
			final int j = scrollPane.getVerticalScrollBar().getValue();
			scrollPane.getVerticalScrollBar().setValue(j + shift);
		}

		revalidate();
		repaint();
	}
}
