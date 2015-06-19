package edu.stanford.genetics.treeview.plugin.dendroview;

//import java.awt.Adjustable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
//import java.awt.event.AdjustmentEvent;
//import java.awt.event.AdjustmentListener;
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

public class RowLabelView extends LabelView implements MouseWheelListener
    /*, AdjustmentListener*/ {

	private static final long serialVersionUID = 1L;

	public RowLabelView() {

		super(LabelView.ROW);
		d_justified = true;
		zoomHint = StringRes.lbl_ZoomRowLabels;
		addMouseWheelListener(this);

		getSecondaryScrollBar().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent e) {
				debug("The mouse has entered a row label pane scrollbar",2);
				if(overScrollLabelPortOffTimer != null) {
					/* Event came too soon, swallow by resetting the timer.. */
					overScrollLabelPortOffTimer.stop();
					overScrollLabelPortOffTimer = null;
				}
				map.setOverRowLabelsScrollbar(true);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				debug("The mouse has exited a row label pane scrollbar",2);
				//Turn off the "over a label port view" boolean after a bit
				if(overScrollLabelPortOffTimer == null) {
					if(labelPortOffDelay == 0) {
						map.setOverRowLabelsScrollbar(false);
						map.notifyObservers();
						revalidate();
						repaint();
					} else {
						/* Start waiting for delay millis to elapse and then
						 * call actionPerformed of the ActionListener
						 * "paneLabelPortOffListener". */
						overScrollLabelPortOffTimer = new Timer(labelPortOffDelay,
								scrollLabelPortOffListener);
						overScrollLabelPortOffTimer.start();
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				debug("The mouse has clicked a row label scrollbar",2);
				if(activeScrollLabelPortOffTimer != null) {
					/* Event came too soon, swallow by resetting the timer.. */
					activeScrollLabelPortOffTimer.stop();
					activeScrollLabelPortOffTimer = null;
				}
				map.setRowLabelsBeingScrolled(true);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				debug("The mouse has released a row label scrollbar",2);
				//Turn off the "over a label port view" boolean after a bit
				if(activeScrollLabelPortOffTimer == null) {
					if(labelPortOffDelay == 0) {
						map.setRowLabelsBeingScrolled(false);
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
		});

		debug = 2;
		//Listen for value changes in the scroll pane's scrollbars
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
	protected void adjustScrollBar() {}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			super.setConfigNode(parentNode.node("RowLabelView"));

		} else {
			LogBuffer.println("Error: Could not find or create ArrayameView"
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
			LogBuffer.println("Warning: LabelView got funny update!");
		}
	}

	protected void selectionChanged() {
		offscreenValid = false;
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

	//Timer to let the label pane linger a bit (prevents flashing when passing
	//between panes which do not change the visibility of the label panes)
	final private int labelPortOffDelay = 250;
	private javax.swing.Timer paneLabelPortOffTimer;
	ActionListener paneLabelPortOffListener = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent evt) {
			if (evt.getSource() == paneLabelPortOffTimer) {
				/* Stop timer */
				paneLabelPortOffTimer.stop();
				paneLabelPortOffTimer = null;
			
				map.setOverRowLabels(false);
				map.notifyObservers();
				revalidate();
				repaint();
			}
		}
	};

	//And this listener is for hovers over the secondary scrollbar, since they each are independent with regard to hovering on or off them
	private javax.swing.Timer overScrollLabelPortOffTimer;
	ActionListener scrollLabelPortOffListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent evt) {
			if (evt.getSource() == overScrollLabelPortOffTimer) {
				debug("You hovered off the secondary row scrollbar 1s ago, so the label port might turn off unless you're over another pane that activates it",2);
				/* Stop timer */
				overScrollLabelPortOffTimer.stop();
				overScrollLabelPortOffTimer = null;
			
				map.setOverRowLabelsScrollbar(false);
				map.notifyObservers();
				revalidate();
				repaint();
			}
		}
	};

	//And this listener is for click releases off the secondary scrollbar, because they can hover off the scrollbar and you don't want the knob and labels to disappear while dragging the knob
	private javax.swing.Timer activeScrollLabelPortOffTimer;
	ActionListener activeLabelPortOffListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent evt) {
			if (evt.getSource() == activeScrollLabelPortOffTimer) {
				debug("You released the secondary row scrollbar 1s ago, so the label port might turn off unless you're over another pane that activates it",2);
				/* Stop timer */
				activeScrollLabelPortOffTimer.stop();
				activeScrollLabelPortOffTimer = null;
			
				map.setRowLabelsBeingScrolled(false);
				map.notifyObservers();
				revalidate();
				repaint();
			}
		}
	};

	@Override
	public void mouseEntered(final MouseEvent e) {
		if(paneLabelPortOffTimer != null) {
			/* Event came too soon, swallow it by resetting the timer.. */
			paneLabelPortOffTimer.stop();
			paneLabelPortOffTimer = null;
		}
		map.setOverRowLabels(true);
		super.mouseEntered(e);
	}

	@Override
	public void mouseExited(final MouseEvent e) {
		//Turn off the "over a label port view" boolean after a bit
		if(paneLabelPortOffTimer == null) {
			if(labelPortOffDelay == 0) {
				map.setOverRowLabels(false);
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
		debug("Scroll wheel event detected",1);

		final int j = scrollPane.getHorizontalScrollBar().getValue();
		//LogBuffer.println("Detected [" + (e.isShiftDown() ? "horizontal" : "vertical") + "] scroll event");
		// On macs' magic mouse, horizontal scroll comes in as if the shift was
		// down
		if(e.isShiftDown()) {
			debug("Scrolling horizontally from [" + j + "] by [" + shift + "]",
			      1);
			scrollPane.getHorizontalScrollBar().setValue(j + shift);
			lastScrollRowPos = j + shift;
			lastScrollRowEndPos = lastScrollRowPos +
			                      getSecondaryScrollBar().getModel()
			                      .getExtent();
			lastScrollRowEndGap = getSecondaryScrollBar().getMaximum() -
			                      lastScrollRowEndPos;
			if(lastScrollRowEndGap < 0) {
				lastScrollRowPos -= lastScrollRowEndGap;
				lastScrollRowEndPos -= lastScrollRowEndGap;
				lastScrollRowEndGap = 0;
			} else if(lastScrollRowPos < 0) {
				lastScrollRowEndPos += lastScrollRowPos;
				lastScrollRowEndGap += lastScrollRowPos;
				lastScrollRowPos = 0;
			}
			debug("New scroll position [" + lastScrollRowPos + "] end pos: [" +
			      lastScrollRowEndPos + "] end gap: [" + lastScrollRowEndGap +
			      "] out of [" + getSecondaryScrollBar().getMaximum() + "]",1);
		} else {
			//Value of label length scrollbar
			map.scrollBy(shift, false);
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
