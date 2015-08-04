package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Adjustable;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Observable;
import java.util.prefs.Preferences;

import javax.swing.JScrollBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import Utilities.StringRes;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.UrlExtractor;

public class ColumnLabelView extends LabelView implements MouseWheelListener,
	AdjustmentListener {

	private static final long serialVersionUID = 1L;

	public ColumnLabelView() {

		super(LabelView.COL);
		d_justified = false;
		zoomHint = StringRes.lbl_ZoomColLabels;
		addMouseWheelListener(this);
		getSecondaryScrollBar().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent e) {
				setPrimaryHoverIndex(map.getMaxIndex());
				map.setOverColLabelsScrollbar(true);
				paintImmediately(0,0,getWidth(),colLabelPaneSize);
				debug("The mouse has entered a column label pane scrollbar",2);
				if(overScrollLabelPortOffTimer != null) {
					/* Event came too soon, swallow by resetting the timer.. */
					overScrollLabelPortOffTimer.stop();
					overScrollLabelPortOffTimer = null;
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				debug("The mouse has exited a column label pane scrollbar",2);
				//Turn off the "over a label port view" boolean after a bit
				if(overScrollLabelPortOffTimer == null) {
					if(labelPortOffDelay == 0) {
						map.setOverColLabelsScrollbar(false);
						map.notifyObservers();
						revalidate();
						repaint();
					} else {
						/* Start waiting for delay millis to elapse and then
						 * call actionPerformed of the ActionListener
						 * "paneLabelPortOffListener". */
						overScrollLabelPortOffTimer =
							new Timer(labelPortOffDelay,
							          scrollLabelPortOffListener);
						overScrollLabelPortOffTimer.start();
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				map.setColLabelsBeingScrolled(true);
				updateDragScrollTimer.start();
				paintImmediately(0,0,getWidth(),colLabelPaneSize);
				debug("The mouse has clicked a column label scrollbar",6);
				if(activeScrollLabelPortOffTimer != null) {
					/* Event came too soon, swallow by resetting the timer.. */
					activeScrollLabelPortOffTimer.stop();
					activeScrollLabelPortOffTimer = null;
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				debug("The mouse has released a column label scrollbar",9);
				updateDragScrollTimer.stop();
				map.setHoverIndex(-1);
				//Turn off the "over a label port view" boolean after a bit
				if(activeScrollLabelPortOffTimer == null) {
					if(labelPortOffDelay == 0) {
						map.setColLabelsBeingScrolled(false);
						map.notifyObservers();
						revalidate();
						repaint();
					} else {
						/* Start waiting for delay millis to elapse and then
						 * call actionPerformed of the ActionListener
						 * "paneLabelPortOffListener". */
						activeScrollLabelPortOffTimer =
							new Timer(labelPortOffDelay,
							          activeLabelPortOffListener);
						activeScrollLabelPortOffTimer.start();
					}
				}
			}

			//I had mouseDragged here, but is NOT EVEN CALLED during a scrollbar
			//drag!!! - Hence it's useless!
		});

		getSecondaryScrollBar().addAdjustmentListener(this);
	}

	public void generateView(final UrlExtractor uExtractor) {
		super.setUrlExtractor(uExtractor);
		headerSummary.setIncluded(new int[] { 0 });
		headerSummary.addObserver(this);
	}

	@Override
	protected void adjustScrollBar() {}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			super.setConfigNode(parentNode.node("ColLabelView"));

		} else {
			LogBuffer.println("Error: Could not find or create ArrayameView" +
			                  "node because parentNode was null.");
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
			LogBuffer.println("Warning: LabelView got funny update!");
		}
	}

	public boolean areLabelsBeingScrolled() {
		return(map.areColLabelsBeingScrolled());
	}

	public void updatePrimaryHoverIndexDuringScrollDrag() {
		//If the labels are being scrolled, you must manually retrieve the
		//cursor position
		if(areLabelsBeingScrolled()) {
			forceUpdatePrimaryHoverIndex();
		}
	}

	public void forceUpdatePrimaryHoverIndex() {
		Point p = MouseInfo.getPointerInfo().getLocation();
		SwingUtilities.convertPointFromScreen(p,getComponent());
		debug("Cursor x coordinate relative to column labels: [" + p.x + "]",8);
		int hDI = map.getIndex(p.x); //Hover Data Index
		if(hDI > map.getMaxIndex()) {
			hDI = map.getMaxIndex();
		} else if (hDI < 0) {
			hDI = 0;
		}
		hoverIndex = hDI;
	}

	/**
	 * This method is called when the selection is changed. It causes the
	 * component to recalculate it's width, and call repaint.
	 */
	protected void selectionChanged() {
		offscreenValid = false;
		revalidate();
		repaint();
	}

	//Timer to let the label pane linger a bit (prevents flashing when passing
	//between panes which do not change the visibility of the label panes)
	final private int labelPortOffDelay = 250;
	private javax.swing.Timer paneLabelPortOffTimer;
	ActionListener paneLabelPortOffListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent evt) {
			if (evt.getSource() == paneLabelPortOffTimer) {
				debug("You hovered off the col label pane 1s ago, so the " +
				      "label port might turn off unless you're over another " +
				      "pane that activates it",2);
				/* Stop timer */
				paneLabelPortOffTimer.stop();
				paneLabelPortOffTimer = null;
			
				map.setOverColLabels(false);
				map.notifyObservers();
				revalidate();
				repaint();
			}
		}
	};

	//And this listener is for hovers over the secondary scrollbar, since they
	//each are independent with regard to hovering on or off them
	private javax.swing.Timer overScrollLabelPortOffTimer;
	ActionListener scrollLabelPortOffListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent evt) {
			if (evt.getSource() == overScrollLabelPortOffTimer) {
				debug("You hovered off the secondary col scrollbar 1s ago, " +
				      "so the label port might turn off unless you're over " +
				      "another pane that activates it",2);
				/* Stop timer */
				overScrollLabelPortOffTimer.stop();
				overScrollLabelPortOffTimer = null;
			
				map.setOverColLabelsScrollbar(false);
				map.notifyObservers();
				revalidate();
				repaint();
			}
		}
	};

	//And this listener is for click releases off the secondary scrollbar,
	//because they can hover off the scrollbar and you don't want the knob and
	//labels to disappear while dragging the knob
	private javax.swing.Timer activeScrollLabelPortOffTimer;
	ActionListener activeLabelPortOffListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent evt) {
			if (evt.getSource() == activeScrollLabelPortOffTimer) {
				debug("You released the secondary col scrollbar 1s ago, so " +
				      "the label port might turn off unless you're over " +
				      "another pane that activates it",6);
				/* Stop timer */
				activeScrollLabelPortOffTimer.stop();
				activeScrollLabelPortOffTimer = null;
			
				map.setColLabelsBeingScrolled(false);
				map.notifyObservers();
				revalidate();
				repaint();
			}
		}
	};

	@Override
	public void mouseEntered(final MouseEvent e) {
		debug("You hovered over the col label pane",2);
		if(paneLabelPortOffTimer != null) {
			/* Event came too soon, swallow it by resetting the timer.. */
			paneLabelPortOffTimer.stop();
			paneLabelPortOffTimer = null;
		}
		map.setOverColLabels(true);
		super.mouseEntered(e);
	}

	public void setHoverPosition(final MouseEvent e) {
		hoverPixel = e.getX();
	}

	@Override
	public void mouseExited(final MouseEvent e) {
		debug("You hovered off the col label pane",2);
		//Turn off the "over a label port view" boolean after a bit
		if(paneLabelPortOffTimer == null) {
			if(labelPortOffDelay == 0) {
				map.setOverColLabels(false);
				map.notifyObservers();
				revalidate();
				repaint();
			} else {
				/* Start waiting for delay millis to elapse and then
				 * call actionPerformed of the ActionListener
				 * "paneLabelPortOffListener". */
				paneLabelPortOffTimer = new Timer(labelPortOffDelay,
						paneLabelPortOffListener);
				paneLabelPortOffTimer.start();
			}
		}

		super.mouseExited(e);
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
		final int index = getPrimaryHoverIndex(e);

		if (SwingUtilities.isLeftMouseButton(e)) {
			if (geneSelection.getNSelectedIndexes() > 0) {
				if(e.isMetaDown() && e.isAltDown()) {
					geneSelection.deselectAllIndexes();
					arraySelection.deselectAllIndexes();
				} else if(e.isShiftDown()) {
					toggleSelectFromClosestToIndex(arraySelection,index);
				} else if(e.isMetaDown()) {
					toggleSelect(arraySelection,index);
				} else if(e.isAltDown()) {
					arraySelection.setIndexSelection(index, false);
				} else {
					selectAnew(arraySelection,index);
				}
			} else {
				//Assumes there is no selection at all
				arraySelection.setIndexSelection(index, true);
				geneSelection.selectAllIndexes();
			}
		}
		//A right-click now brings up a contextual menu coded elsewhere
		// else {
		//	geneSelection.deselectAllIndexes();
		//	arraySelection.deselectAllIndexes();
		//}

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
		hoverPixel = e.getX();
		return(hoverPixel);
	}

	public void explicitSecondaryScrollTo(int pos,int endPos,int endGap) {
		if(pos < 0) pos = 0;
		if(pos > (getSecondaryScrollBar().getMaximum() -
		          getSecondaryScrollBar().getModel().getExtent())) {
			pos = getSecondaryScrollBar().getMaximum() -
			      getSecondaryScrollBar().getModel().getExtent();
		}
		if(endPos > 0) {
			endPos += (pos - getSecondaryScrollBar().getValue());
		} else {
			endPos = pos + getSecondaryScrollBar().getModel().getExtent();
		}
		if(endGap == -1) {
			endGap = getSecondaryScrollBar().getMaximum() - endPos;
		}
		getSecondaryScrollBar().setValue(pos);
		lastScrollColPos    = pos;
		lastScrollColEndPos = endPos;
		lastScrollColEndGap = endGap;
	}

	/* TODO: Eliminate this and use adjustmentValueChanged instead because it is
	 * more holistic */
	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {

		final int notches = e.getWheelRotation();
		int shift = (notches < 0) ? -3 : 3;

		debug("Detected [" + (e.isShiftDown() ? "horizontal" : "vertical") +
		      "] scroll event",1);
		// On macs' magic mouse, horizontal scroll comes in as if the shift was
		// down
		if(e.isShiftDown()) {
			map.scrollBy(shift, false);
			updatePrimaryHoverIndexDuringScrollWheel();
		} else {
			shift = (notches < 0) ? -6 : 6;
			final int j = getSecondaryScrollBar().getValue();
			if(j + shift < 0) {
				shift = -j;
			} else if(j + shift + getSecondaryScrollBar().getModel().getExtent()
			          > getSecondaryScrollBar().getMaximum()) {
				shift = getSecondaryScrollBar().getMaximum() -
					(j + getSecondaryScrollBar().getModel().getExtent());
			}
			if(shift == 0) return;
			debug("Scrolling vertically from [" + j + "] by [" + shift + "]",1);
			lastScrollColPos = j + shift;
			lastScrollColEndPos = lastScrollColPos +
			                      getSecondaryScrollBar().getModel()
			                      .getExtent();
			lastScrollColEndGap = getSecondaryScrollBar().getMaximum() -
			                      lastScrollColEndPos;
			if(lastScrollColEndGap < 0) {
				lastScrollColPos -= lastScrollColEndGap;
				lastScrollColEndPos -= lastScrollColEndGap;
				lastScrollColEndGap = 0;
			} else if(lastScrollColPos < 0) {
				lastScrollColEndPos += lastScrollColPos;
				lastScrollColEndGap += lastScrollColPos;
				lastScrollColPos = 0;
			}
			getSecondaryScrollBar().setValue(j + shift);
			debug("New secondary col scroll position [" + lastScrollColPos +
			      "] end pos: [" + lastScrollColEndPos + "] end gap: [" +
			      lastScrollColEndGap + "] out of [" +
			      getSecondaryScrollBar().getMaximum() + "]",1);
			//paintImmediately(0, 0, getWidth(), getHeight());
		}

		//revalidate();
		//repaint();
		//paintImmediately(0, 0, getWidth(), getHeight());
	}

	public void adjustmentValueChanged(AdjustmentEvent evt) {
		Adjustable source = evt.getAdjustable();
		int orient = source.getOrientation();
		if(orient == Adjustable.VERTICAL) {
			debug("scrollbar adjustment detected from vertical scrollbar",2); 
		}
		int oldvalue = getSecondaryScrollBar().getValue();
		boolean updateScroll = false;
		//This if conditional catches drags
		if(!evt.getValueIsAdjusting() && map.areColLabelsBeingScrolled()) {
			debug("The knob on the scrollbar is being dragged",7);
			updateScroll = true;
			explicitSecondaryScrollTo(oldvalue,-1,-1);
		}
		//This gets ANY other scroll event, even programmatic scrolls called
		//from the code, but we only want to do anything when the scrollbar is
		//clicked - everything else is either the scroll wheel or a coded re-
		//scroll that we don't want to change anything
		else {
			updateScroll = true;
			int newvalue = evt.getValue();
			if(oldvalue != newvalue) {
				int type = evt.getAdjustmentType();
				switch(type) {
    				case AdjustmentEvent.UNIT_INCREMENT:
    					debug("Scrollbar was increased by one unit",1);
    					break;
    				case AdjustmentEvent.UNIT_DECREMENT:
    					debug("Scrollbar was decreased by one unit",1);
    					break;
    				case AdjustmentEvent.BLOCK_INCREMENT:
    					debug("Scrollbar was increased by one block",1);
    					break;
    				case AdjustmentEvent.BLOCK_DECREMENT:
    					debug("Scrollbar was decreased by one block",1);
    					break;
    				case AdjustmentEvent.TRACK:
    					debug("A non-scrollbar scroll event was detected (a " +
    					      "call from code or a mouse wheel event)",1);
    					updateScroll = false;
    					break;
				}
				if(updateScroll) {
					debug("Scrolling from: [" + source.getValue() + " or (" +
					      oldvalue + ")" + "] to: [" + newvalue + "] via [" +
					      evt.getSource() + "]",7);
					explicitSecondaryScrollTo(newvalue,-1,-1);
				}
			}
		}
		if(updateScroll) {
			repaint();
			//paintImmediately(0, 0, getWidth(), getHeight());
		}
	}

	//This is an attempt to get the dragging of the scroll handle to correctly
	//redraw the labels in the correct positions
	private int updateDragScrollInterval = 10;  // update every X milliseconds
	private Timer updateDragScrollTimer =
		new Timer(updateDragScrollInterval,
		          new ActionListener() {
			@Override
			public void
			actionPerformed(ActionEvent e) {
				explicitSecondaryScrollTo(getSecondaryScrollBar().getValue(),
				                          -1,
				                          -1);
				repaint();
			}
		});
}
