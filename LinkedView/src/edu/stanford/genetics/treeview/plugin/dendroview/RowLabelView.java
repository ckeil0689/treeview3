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

public class RowLabelView extends LabelView implements MouseWheelListener {

	private static final long serialVersionUID = 1L;

	private int oldWidth = 0;

	public RowLabelView() {

		super(LabelView.ROW);
		d_justified = true;
		zoomHint = StringRes.lbl_ZoomRowLabels;
		addMouseWheelListener(this);
	}

	//public void generateView(final UrlExtractor uExtractor) {

	//	this.urlExtractor = uExtractor;

	//	headerSummary.setIncluded(new int[] { 0 });
	//	headerSummary.addObserver(this);
	//}
	public void generateView(final UrlExtractor uExtractor) {

		super.setUrlExtractor(uExtractor);

		headerSummary.setIncluded(new int[] { 0 });
		headerSummary.addObserver(this);
	}

	//public void generateView(final UrlExtractor uExtractor) {

	//	this.urlExtractor = uExtractor;

	//	headerSummary.setIncluded(new int[] { 0 });
	//	headerSummary.addObserver(this);
	//}

	
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
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			super.setConfigNode(parentNode.node("RowLabelView"));

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
	//protected void selectionChanged() {

	//	maxlength = 1;
	//	final FontMetrics fontMetrics = getFontMetrics(new Font(face, style,
	//			size));

	//	final int start = 0;
	//	final int end = headerInfo.getNumHeaders();
	//	
//		LogBuffer.println("NumHeaders: " + end);

	//	for (int j = start; j < end; j++) {

	//		final int actualGene = j;
	//		final String out = headerSummary.getSummary(headerInfo, actualGene);

	//		if (out == null) {
	//			continue;
	//		}

	//		final int length = fontMetrics.stringWidth(out);
	//		if (maxlength < length) {
	//			maxlength = length;
	//		}

	//	}

	//	setPreferredSize(new Dimension(maxlength, map.getUsedPixels()));
	//	revalidate();
	//	repaint();
	//}
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
		setPreferredSize(new Dimension(maxlength, map.getUsedPixels()));

		if (maxlength > oldWidth) {
			visible.y += maxlength - oldWidth;
			scrollRectToVisible(visible);
		}
		oldWidth = maxlength;
		
		revalidate();
		repaint();
	}

	public void setSecondaryScrollBarPolicyAlways() {
		scrollPane.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
	}

	public void setSecondaryScrollBarPolicyNever() {
		scrollPane.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
	}

	public void setSecondaryScrollBarPolicyAsNeeded() {
		scrollPane.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
	}

	public boolean isJustifiedToMatrixEdge() {
		return(isRightJustified);
	}

	public int getPrimaryHoverPosition(final MouseEvent e) {
		return(e.getY());
	}

//	protected int shiftForScrollbar = 0;
//	protected int shiftedForScrollbar = 0;
//
//	@Override
//	public void mouseMoved(final MouseEvent e) {
//
//		if(shiftForScrollbar > 0) {
//			//Shift the scroll position to accommodate the scrollbar that
//			//appeared (I think this may only be for Macs, according to what I
//			//read.  They draw the scrollbar on top of content when it is set
//			//"AS_NEEDED")
//			getSecondaryScrollBar().setValue(shiftForScrollbar);
//			if(getSecondaryScrollBar().getValue() == shiftForScrollbar) {
//				shiftedForScrollbar = shiftForScrollbar;
//				shiftForScrollbar = 0;
//			}
//		}
//		hoverIndex = map.getIndex(getPrimaryHoverPosition(e));
//		repaint();
//	}
//
//	//@Override
//	public void mouseEntered(final MouseEvent e) {
//		//This method call is why these mouse functions
//		setSecondaryScrollBarPolicyAlways();
//		//scrollPane.setVerticalScrollBarPolicy(
//		//		ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
//		int ts = getSecondaryScrollBar().getValue();
//		int tw = getSecondaryScrollBar().getModel().getExtent();
//		int tm = getSecondaryScrollBar().getMaximum();
//		
//		//We do not want to shift the scrollbar if the user has manually left-
//		//justified his/her labels, so only shift to accommodate the scrollbar
//		//when the scroll position is more than half way
//		boolean nearBeginning = (ts < ((tm - tw) / 2));
//		LogBuffer.println("ENTER Setting temp scroll value: [" + ts +
//				"] width [" + tw + "] max [" + tm + "]");
//		if(isJustifiedToMatrixEdge() && !nearBeginning) {
//			LogBuffer.println("Adjusting scrollbar that is positioned near " +
//					"end. Now position: [" + ts + " + 15] New max: [" +
//					getSecondaryScrollBar().getMaximum() + " + 15]");
//			//Width of the vertical scrollbar is 15
//			int newWidth = getSecondaryScrollBar().getMaximum() + 15;
//			getSecondaryScrollBar().setMaximum(newWidth);
//			shiftForScrollbar = ts + 15;
//			getSecondaryScrollBar().setValue(shiftForScrollbar);
//		}
//		LogBuffer.println("ENTER New scroll values: [" +
//				getSecondaryScrollBar().getValue() + "] width [" +
//				getSecondaryScrollBar().getModel().getExtent() + "] max [" +
//				getSecondaryScrollBar().getMaximum() + "]");
//	}
//
//	//@Override
//	public void mouseExited(final MouseEvent e) {
//		int ts = getSecondaryScrollBar().getValue();
//		setSecondaryScrollBarPolicyNever();
//		//scrollPane.setVerticalScrollBarPolicy(
//		//		ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
//		LogBuffer.println("EXIT Setting temp scroll value: [" + ts + "]");
//		if(shiftedForScrollbar > 0) {
//			LogBuffer.println("Adjusting scrollbar that is positioned near end.");
//			getSecondaryScrollBar().setValue(ts - 15);
//			shiftedForScrollbar = 0;
//		}
//		scrollPane.revalidate();
//		LogBuffer.println("EXIT New scroll values: [" + getSecondaryScrollBar().getValue() + "] width [" + getSecondaryScrollBar().getModel().getExtent() + "] max [" + getSecondaryScrollBar().getMaximum() + "]");
//		super.mouseExited(e);
//	}

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
			if (arraySelection.getNSelectedIndexes() > 0) {
				if(e.isShiftDown()) {
					toggleSelectFromClosestToIndex(geneSelection,index);
				} else if(e.isMetaDown()) {
					toggleSelect(geneSelection,index);
				} else {
					selectAnew(geneSelection,index);
				}
			} else {
				//Assumes there is no selection at all
				geneSelection.setIndexSelection(index, true);
				arraySelection.selectAllIndexes();
			}
		} else {
			arraySelection.deselectAllIndexes();
			geneSelection.deselectAllIndexes();
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
		return scrollPane.getVerticalScrollBar();
	}

	@Override
	public JScrollBar getSecondaryScrollBar() {
		return scrollPane.getHorizontalScrollBar();
	}

	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {

		final int notches = e.getWheelRotation();
		final int shift = (notches < 0) ? -3 : 3;

		LogBuffer.println("Detected [" + (e.isShiftDown() ? "horizontal" : "vertical") + "] scroll event");
		// On macs' magic mouse, horizontal scroll comes in as if the shift was
		// down
		if(e.isShiftDown()) {
			final int j = scrollPane.getHorizontalScrollBar().getValue();
			scrollPane.getHorizontalScrollBar().setValue(j + shift);
		} else {
			final int j = scrollPane.getVerticalScrollBar().getValue();
			scrollPane.getVerticalScrollBar().setValue(j + shift);

//			if (j != scrollPane.getVerticalScrollBar().getValue()) {
//				setChanged();
//			}
//
//			notifyObservers();
		}

		revalidate();
		repaint();
	}
}
