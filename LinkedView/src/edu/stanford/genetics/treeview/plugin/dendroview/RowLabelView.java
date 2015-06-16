package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Adjustable;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Observable;
import java.util.prefs.Preferences;

import javax.swing.JScrollBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.sun.xml.internal.ws.util.StringUtils;

import Utilities.StringRes;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.UrlExtractor;

public class RowLabelView extends LabelView implements MouseWheelListener/*, AdjustmentListener*/ {

	private static final long serialVersionUID = 1L;

	private int oldWidth = 0;

	public RowLabelView() {

		super(LabelView.ROW);
		d_justified = true;
		zoomHint = StringRes.lbl_ZoomRowLabels;
		addMouseWheelListener(this);

		// Listen for value changes in the scroll pane's scrollbars
//		scrollPane.getHorizontalScrollBar().addAdjustmentListener(this);
//		scrollPane.getVerticalScrollBar().addAdjustmentListener(this);
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
//
//		if (isRightJustified) {
//			final int scrollMax = scrollPane.getHorizontalScrollBar()
//					.getMaximum();
//			scrollPane.getHorizontalScrollBar().setValue(scrollMax);
//		} else {
//			scrollPane.getHorizontalScrollBar().setValue(0);
//		}
//
//		repaint();
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

//		final int start = 0;
//		final int end = headerInfo.getNumHeaders();
//
//		final FontMetrics fontMetrics = getFontMetrics(new Font(face, style,
//				size));
//		maxlength = 1;
//		for (int j = start; j < end; j++) {
//
//			final String out = headerSummary.getSummary(headerInfo, j);
//
//			if (out == null) {
//				continue;
//			}
//
//			final int length = fontMetrics.stringWidth(out);
//			if (maxlength < length) {
//				maxlength = length;
//			}
//		}

		//final Rectangle visible = getVisibleRect();
		//setPreferredSize(new Dimension(maxlength, map.getUsedPixels()));

		//if (maxlength > oldWidth) {
		//	visible.y += maxlength - oldWidth;
//Commenting this out because the scrollbar keeps jumping around
//			scrollRectToVisible(visible);
		//}
		//oldWidth = maxlength;
		
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

	//Timer to let the label pane linger a bit
	final private int delay = 0;
	private javax.swing.Timer turnOffLabelPortTimer;
	ActionListener turnOffLabelPort = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent evt) {
			if (evt.getSource() == turnOffLabelPortTimer) {
				/* Stop timer */
				turnOffLabelPortTimer.stop();
				turnOffLabelPortTimer = null;
			
				map.setOverRowLabels(false);
				map.notifyObservers();
				revalidate();
				repaint();
			}
		}
	};

	@Override
	public void mouseEntered(final MouseEvent e) {
		if(this.turnOffLabelPortTimer != null) {
			/* Event came too soon, swallow it by resetting the timer.. */
			this.turnOffLabelPortTimer.stop();
			this.turnOffLabelPortTimer = null;
		}
		map.setOverRowLabels(true);
		super.mouseEntered(e);
	}

	@Override
	public void mouseExited(final MouseEvent e) {
		//Turn off the "over a label port view" boolean after a bit
		if(this.turnOffLabelPortTimer == null) {
			if(delay == 0) {
				map.setOverRowLabels(false);
				map.notifyObservers();
				revalidate();
				repaint();
			} else {
				/* Start waiting for delay millis to elapse and then
				 * call actionPerformed of the ActionListener
				 * "turnOffLabelPort". */
				this.turnOffLabelPortTimer = new Timer(this.delay,
						turnOffLabelPort);
				this.turnOffLabelPortTimer.start();
			}
		}

		//setOverRowLabels(false);
		super.mouseExited(e);
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
		final int index = getPrimaryHoverIndex(e);

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
		if(debug)
			LogBuffer.println("Scroll wheel event detected");

		final int j = scrollPane.getHorizontalScrollBar().getValue();
		//LogBuffer.println("Detected [" + (e.isShiftDown() ? "horizontal" : "vertical") + "] scroll event");
		// On macs' magic mouse, horizontal scroll comes in as if the shift was
		// down
		if(e.isShiftDown()) {
			//if(debug)
				LogBuffer.println("Scrolling horizontally from [" + j + "] by [" + shift + "]");
			scrollPane.getHorizontalScrollBar().setValue(j + shift);
			lastScrollRowPos = j + shift;
			lastScrollRowEndPos = lastScrollRowPos + getSecondaryScrollBar().getModel().getExtent();
			lastScrollRowEndGap =
					getSecondaryScrollBar().getMaximum() - lastScrollRowEndPos;
			if(lastScrollRowEndGap < 0) {
				lastScrollRowPos -= lastScrollRowEndGap;
				lastScrollRowEndPos -= lastScrollRowEndGap;
				lastScrollRowEndGap = 0;
			} else if(lastScrollRowPos < 0) {
				lastScrollRowEndPos += lastScrollRowPos;
				lastScrollRowEndGap += lastScrollRowPos;
				lastScrollRowPos = 0;
			}
			//if(debug)
			LogBuffer.println("New scroll position [" + lastScrollRowPos + "] end pos: [" + lastScrollRowEndPos + "] end gap: [" + lastScrollRowEndGap + "] out of [" + getSecondaryScrollBar().getMaximum() + "]");
		} else {
			//Value of label length scrollbar
			//int doesItChange = scrollPane.getHorizontalScrollBar().getValue();
			//scrollPane.getVerticalScrollBar().setValue(j + shift);
			map.scrollBy(shift, false);

			//if(scrollPane.getHorizontalScrollBar().getValue() != doesItChange) {
			//	LogBuffer.println("The fucking horizontal scroll changed from [" + doesItChange + "] to [" + scrollPane.getHorizontalScrollBar().getValue() + "] during a fucking vertical scroll!");
			//}
//			if (j != scrollPane.getVerticalScrollBar().getValue()) {
//				setChanged();
//			}
//
//			notifyObservers();
		}

		revalidate();
		//repaint();
		//repaint wasn't always updating the last paint step, but paintImmediately (and an invokdeLater also) seems to work
		//I think it's because the updateBuffer finds out if it needs to change anything from the map object
		paintImmediately(0, 0, getWidth(), getHeight());
	}

//	public void adjustmentValueChanged(AdjustmentEvent evt) {
//		Adjustable source = evt.getAdjustable();
//		if (evt.getValueIsAdjusting()) {
//			return;
//		}
//		int orient = source.getOrientation();
//		if (orient == Adjustable.HORIZONTAL) {
//			System.out.println("from horizontal scrollbar"); 
////			StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
////			LogBuffer.println("Horizontal scroll came from: [" + Thread.currentThread().getStackTrace() +
//////				stackTraceElements[8].getClassName() + ":" + stackTraceElements[8].getMethodName() + ":" + stackTraceElements[8].getLineNumber() + "<-" +
//////				stackTraceElements[9].getClassName() + ":" + stackTraceElements[9].getMethodName() + ":" + stackTraceElements[9].getLineNumber() + "<-" +
//////				stackTraceElements[10].getClassName() + ":" + stackTraceElements[10].getMethodName() + ":" + stackTraceElements[10].getLineNumber() + "<-" +
//////				stackTraceElements[11].getClassName() + ":" + stackTraceElements[11].getMethodName() + ":" + stackTraceElements[11].getLineNumber() + "<-" +
//////				stackTraceElements[12].getClassName() + ":" + stackTraceElements[12].getMethodName() + ":" + stackTraceElements[12].getLineNumber() + "<-" +
//////				stackTraceElements[13].getClassName() + ":" + stackTraceElements[13].getMethodName() + ":" + stackTraceElements[13].getLineNumber() +
////				"].");
////			Thread.dumpStack();
//		} else {
//			System.out.println("from vertical scrollbar");
//		}
//		int type = evt.getAdjustmentType();
//		switch (type) {
//			case AdjustmentEvent.UNIT_INCREMENT:
//				System.out.println("Scrollbar was increased by one unit");
//				break;
//			case AdjustmentEvent.UNIT_DECREMENT:
//				System.out.println("Scrollbar was decreased by one unit");
//				break;
//			case AdjustmentEvent.BLOCK_INCREMENT:
//				System.out.println("Scrollbar was increased by one block");
//				break;
//			case AdjustmentEvent.BLOCK_DECREMENT:
//				System.out.println("Scrollbar was decreased by one block");
//				break;
//			case AdjustmentEvent.TRACK:
//				System.out.println("The knob on the scrollbar was dragged");
//				break;
//		}
//		int value = evt.getValue();
//	}
}
