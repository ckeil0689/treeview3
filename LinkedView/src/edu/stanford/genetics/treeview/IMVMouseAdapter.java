package edu.stanford.genetics.treeview;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import edu.stanford.genetics.treeview.plugin.dendroview.InteractiveMatrixView;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;

/**
 * This class handles all mouse input except from the mouse wheel. It needs to
 * be attached to InteractiveMatrixView in order to make the matrix interactive
 * for mouse gestures such as selection. 
 * @author chris0689
 *
 */
public class IMVMouseAdapter extends MouseAdapter {

	// Members
	private final MatrixViewController mvController;
	private final InteractiveMatrixView imView;
	
	private final TreeSelectionI rowSelection;
	private final TreeSelectionI colSelection;
	
	private final MapContainer xmap;
	private final MapContainer ymap;
	
	/**
	 * This rectangle keeps track of where the drag rect was drawn
	 */
	private final Rectangle dragRect = new Rectangle();
	
	/**
	 * Points to track candidate selected rows/cols should reflect where the
	 * mouse has actually been
	 */
	private final Point startPoint = new Point();
	private final Point endPoint = new Point();
	
	private boolean isMousePressed;
	private int clickCount;     //Need to determine single/double/etc clicks
	
	//Had to put these in the main class, because when they were in
	//MatrixMouseListener, setting them in mousePressed did not make them
	//available when mouseDragged ran
	private int pressedX;
	private int pressedY;
	
	private MouseEvent dragEvent;
	private MouseEvent pressedEvent;   //To process clicks initiated via timer
	
	public IMVMouseAdapter(final MatrixViewController mvController, 
			final InteractiveMatrixView imView, 
			final MapContainer interactiveXmap, 
			final MapContainer interactiveYmap) {
		
		super();
		
		this.mvController = mvController;
		this.imView = imView;
		
		this.rowSelection = imView.getRowSelection();
		this.colSelection = imView.getColSelection();
		
		this.xmap = interactiveXmap;
		this.ymap = interactiveYmap;
	}
	
	@Override
	public void mouseMoved(final MouseEvent e) {

		/* TODO passing index makes the most sense but an overloaded method 
		 * that takes a pixel works well too. 
		 * Reduces clutter in calling classes a little bit.
		 */
		xmap.setHoverIndex(xmap.getIndex(e.getX()));
		ymap.setHoverIndex(ymap.getIndex(e.getY()));
	}
	
	// Mouse Listener
	@Override
	public void mousePressed(final MouseEvent e) {

		LogBuffer.println("Mouse pressed");
		
		if(!imView.enclosingWindow().isActive()) {
			return;
		}

		isMousePressed = true;

		imView.debug("mousePressed: Setting pressedX [" + pressedX +
			"] and pressedY [" + pressedY + "].", 10);

		//If a previous 200ms timer is still running, restart the timer and
		//increment the clickCount
		if(multiClickActionTimer != null &&
			multiClickActionTimer.isRunning()) {

			multiClickActionTimer.setDelay(multiClickActionDelay);
			multiClickActionTimer.restart();
			clickCount++;
		}
		//Else this is a first click event - set clickCount to 1 and start
		//the timer to listen for more clicks
		else {
			Integer tmp =
				(Integer) Toolkit.getDefaultToolkit().
				getDesktopProperty("awt.multiClickInterval");
			if(tmp != null) {
				multiClickActionInitialDelay = tmp;
				multiClickActionDelay        = tmp;
			}

			clickCount = 1;

			//Always record where the first click happened
			pressedEvent = e;
			pressedX = e.getX();
			pressedY = e.getY();

			//Start the drag event over so that we don't base the
			//mouseWasDragged() calculation on a previous drag event
			dragEvent = null;

			if(multiClickActionTimer != null) {
				multiClickActionTimer.
					setDelay(multiClickActionInitialDelay);
				multiClickActionTimer.start();
				
			} else {
				multiClickActionTimer =
					new Timer(multiClickActionInitialDelay,
						multiClickActionListener);
				multiClickActionTimer.start();
			}
		}
	}
	
	@Override
	public void mouseDragged(final MouseEvent e) {

		LogBuffer.println("// mouse dragged");
		
		if (!imView.enclosingWindow().isActive()) {
			return;
		}
		
		dragEvent = e;

		//When left button is used
		if(SwingUtilities.isLeftMouseButton(e)) {
			//We are going to send both the start point and the current
			//point in case the timer has not yet started the drag
			imView.debug("mouseDragged: pressedEvent is [" +
				((pressedEvent == null) ? "null" : "defined") + "].",10);
			imView.debug("mouseDragged: pressedX [" + pressedX + "] pressedY [" +
				pressedY + "].",10);
			
			if(e.isShiftDown()) {
				processLeftShiftDragDuring(pressedX, pressedY, e.getX(), 
						e.getY());
				
			} else if(e.isControlDown()) {
				processLeftControlDragDuring(pressedX, pressedY, e.getX(),
						e.getY());
				
			} else {
				processLeftDragDuring(pressedX, pressedY, e.getX(), e.getY());
			}
		}
	}
		
	@Override
	public void mouseReleased(final MouseEvent e) {

		LogBuffer.println("mouse released");
		
		if(!imView.enclosingWindow().isActive() || !isMousePressed) {
			return;
		}

		isMousePressed = false;

		//If the timer is still running and the user has not dragged the
		//mouse
		if(multiClickActionTimer != null &&
			multiClickActionTimer.isRunning() && !mouseWasDragged()) {

			//Restart the timer to listen for the second click
			multiClickActionTimer.setDelay(multiClickActionDelay);
			multiClickActionTimer.restart();

			//Immediately process multiclicks in intervals of the maximum
			//number of supported clicks (in this case, a double-click).
			//We will allow multi-clicks to continue and restart the timer
			//so that they can continue to be detected as a single multi-
			//click event.
			if(clickCount > 2 && clickCount % 2 == 0) {

				processClickEvents();
			}

			//Do nothing & let the timer trigger the click action
			return;
		}
		//Else the mouse was dragged, so process the drag start (if
		//necessary) and stop the timer
		else if(multiClickActionTimer != null &&
			multiClickActionTimer.isRunning()) {

			//If we're not already in selecting mode, process the drag start
			if(!xmap.isSelecting()) {
				processLeftDragStart(pressedX,pressedY);
			}

			multiClickActionTimer.stop();
		}

		//This is a drag-selection event
		imView.debug("Mouse released.",4);

		//At this point, we are assured the timer has stopped, so we need to
		//know if this is a release from a drag or a click event (possibly
		//multi). xmap.isSelecting will be true if it is a drag.
		//If this is a drag-select
		if(xmap.isSelecting()) {
			imView.debug("Ending a drag-selection",10);
			/* Full gene selection */
			if(e.isShiftDown()) {
				processLeftShiftDragEnd(e.getX(), e.getY());
			}
			/* Full array selection */
			else if(e.isControlDown()) {
				processLeftControlDragEnd(e.getX(), e.getY());
			}
			/* Normal selection */
			else {
				processLeftDragEnd(e.getX(), e.getY());
			}
		}
		//Else, the user just had a long click in one spot, so select the
		//cell. It doesn't matter if they click again immediately after the
		//long click
		else {
			imView.debug("Ending a quick click that didn't move",10);
			processClickEvents();
		}
	}

	//Timer to let the label pane linger a bit (prevents flashing when
	//passing between panes which do not change the visibility of the label
	//panes)
	final private int delay = 250;
	private javax.swing.Timer turnOffLabelPortTimer;
	ActionListener turnOffLabelPort = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent evt) {
			
			if (evt.getSource() == turnOffLabelPortTimer) {
				/* Stop timer */
				turnOffLabelPortTimer.stop();
				turnOffLabelPortTimer = null;
			
				xmap.setOverInteractiveMatrix(false);
				ymap.setOverInteractiveMatrix(false);
				
				xmap.notifyObservers();
				ymap.notifyObservers();
				
				imView.repaint();
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
		
		xmap.setOverInteractiveMatrix(true);
		ymap.setOverInteractiveMatrix(true);
		
		if (!imView.enclosingWindow().isActive()) {
			return;
		}

		//Commented the following out because clicking in a search box and then
		//moving the cursor away before typing was making the focus go away.
		//The user should explicitly use tab or a click to change focus before
		//navigating with the keyboard.
//		imView.setHasMouse(true);
//		imView.requestFocus();
	}

	@Override
	public void mouseExited(final MouseEvent e) {

		imView.debug("mouseExited IMV",1);
		//Turn off the "over a label port view" boolean after a bit
		if(this.turnOffLabelPortTimer == null) {
			imView.debug("mouseExited IMV - starting timer",1);
			/* Start waiting for delay millis to elapse and then
			 * call actionPerformed of the ActionListener
			 * "turnOffLabelPort". */
			if(delay == 0) {
				imView.debug("mouseEvent in IMV - ACTED ON",1);
				xmap.setOverInteractiveMatrix(false);
				ymap.setOverInteractiveMatrix(false);
				xmap.notifyObservers();
				ymap.notifyObservers();
				
				imView.repaint();
			} else {
				this.turnOffLabelPortTimer = new Timer(this.delay,
						turnOffLabelPort);
				this.turnOffLabelPortTimer.start();
			}
		}

		imView.setHasMouse(false);

		xmap.setHoverIndex(-1);
		ymap.setHoverIndex(-1);
	}
	
	/**
	 * Only to be called by the multiClickDetectionTimer. This method
	 * returns true if the cursor position changed significantly between the
	 * first click and the last drag position by the time the timer goes off
	 * @author rleach
	 * @param 
	 * @return 
	 * @return
	 */
	public boolean mouseWasDragged() {

		double maxPixelDistance = 4.0;
		if(pressedEvent == null || dragEvent == null) {
			return(false);
		}
		
		double xdist = dragEvent.getX() - pressedEvent.getX();
		double ydist = dragEvent.getY() - pressedEvent.getY();
		double distance = Math.sqrt(xdist * xdist + ydist * ydist);
		return(distance > maxPixelDistance);
	}
	
	/**
	 * This timer initiates single-click, multi-click, & drag events only.
	 * It is interrupted by mouseDragged. Also, even-numbered double-click
	 * events during a 3+ click event (including a double-click) are
	 * initiated in mouseReleased. The fact that this timer is running is
	 * used to determine whether or not mouseReleased should do anything.
	 */
	private int multiClickActionInitialDelay = 350;
	private int multiClickActionDelay = 250;
	private javax.swing.Timer multiClickActionTimer;
	
	ActionListener multiClickActionListener = new ActionListener() {

		/**
		 * When a multi-click or a click & hold timer expires, this is
		 * called
		 */
		@Override
		public void actionPerformed(ActionEvent evt) {

			if(evt.getSource() == multiClickActionTimer) {

				/* Stop timer so it doesn't repeat */
				multiClickActionTimer.stop();
				multiClickActionTimer = null;

				//THIS FIRST IF CONDITIONAL MIGHT BE UNNECESSARY
				//If the left mouse button is pressed, we're not already in
				//drag-selecting mode, and it's a left-click, process the
				//drag start.
				if(isMousePressed && !xmap.isSelecting() &&
					SwingUtilities.isLeftMouseButton(pressedEvent)) {

					imView.debug("Correcting drag start",10);
					processLeftDragStart(pressedX,pressedY);
				}
				//Else if the mouse button is not pressed and the mouse has
				//not moved during the click event (possibly multi)
				else if(!isMousePressed && !mouseWasDragged()) {

					processClickEvents();
				}
				//Else if the mouse button is not pressed and the mouse
				//finished moving before this timer action was triggered,
				//then the drag end never got called to make the selection,
				//so call it
				else if(!isMousePressed && mouseWasDragged()) {

					if(!xmap.isSelecting()) {

						imView.debug("Correcting drag start",10);
						processLeftDragStart(pressedX,
							pressedY);
					}

					//Figure out the end of the drag event
					int dragX, dragY;
					if(dragEvent == null) {
						imView.debug("dragEvent is null",10);
						Point p =
							MouseInfo.getPointerInfo().getLocation();
						SwingUtilities.convertPointFromScreen(p,
							imView.getComponent());
						p.y += 1; //fudge factor bec conversion is off by 1
						dragX = p.x;
						dragY = p.y;
					} else {
						dragX = dragEvent.getX();
						dragY = dragEvent.getY();
					}

					/* Full gene selection */
					if(pressedEvent.isShiftDown()) {
						processLeftShiftDragEnd(dragX,dragY);
					}
					/* Full array selection */
					else if(pressedEvent.isControlDown()) {
						processLeftControlDragEnd(dragX, dragY);
					}
					/* Normal selection */
					else {
						processLeftDragEnd(dragX,dragY);
					}
				}
				//Note, double-clicks are not handled here because they can
				//be more efficiently detected in real time.  If we were to
				//introduce triple-click or double-click-drag functionality,
				//then double-clicks would have to be handled here.
			}
		}
	};
	
	/**
	 * This method should be called after an N-click event has completed,
	 * where 'N' is the number of clicks (e.g. single-click, double-click,
	 * triple-click, etc). It will perform 1 action from the click event.
	 * This method is not for use in mouse-overs, drags, click-and-hold
	 * events, etc..
	 * @author rleach
	 * @param none
	 * @return none
	 */
	public void processClickEvents() {

		//When left button is used
		if(SwingUtilities.isLeftMouseButton(pressedEvent)) {
			//If the click count is even, perform a double-click
			if(clickCount % 2 == 0) {
				//option/alt = Zooming out
				if(pressedEvent.isAltDown()) {
					//command-shift = slam zoom to target without animation
					if(pressedEvent.isMetaDown() &&
						pressedEvent.isShiftDown()) {
						processLeftDoubleOptionShiftCommandClick(
							pressedX,pressedY);
					}
					//shift = zoom faster
					else if(pressedEvent.isShiftDown()) {
						processLeftDoubleOptionShiftClick(
							pressedX,pressedY);
					}
					//command/windows = slam zoom to target
					else if(pressedEvent.isMetaDown()) {
						processLeftDoubleOptionCommandClick(
							pressedX,pressedY);
					}
					//control = zoom slower
					else if(pressedEvent.isControlDown()) {
						processLeftDoubleOptionControlClick(
							pressedX,pressedY);
					}
					//regular double-click = zoom medium
					else {
						processLeftDoubleOptionClick(
							pressedX,pressedY);
					}
				}
				//No option/alt = Zooming in
				else {
					//command-shift = slam zoom to target without animation
					if(pressedEvent.isMetaDown() &&
						pressedEvent.isShiftDown()) {
						processLeftDoubleShiftCommandClick(pressedX,
							pressedY);
					}
					//shift = zoom faster
					else if(pressedEvent.isShiftDown()) {
						processLeftDoubleShiftClick(
							pressedX,pressedY);
					}
					//command/windows = slam zoom to target
					else if(pressedEvent.isMetaDown()) {
						processLeftDoubleCommandClick(pressedX,
							pressedY);
					}
					//control = zoom slower
					else if(pressedEvent.isControlDown()) {
						processLeftDoubleControlClick(
							pressedX,pressedY);
					}
					//Regular double-click = zoom medium
					else {
						processLeftDoubleClick(pressedX,
							pressedY);
					}
				}
			} else if(clickCount == 1) {
				//shift = select entire row
				if(pressedEvent.isShiftDown()) {
					processLeftSingleShiftClick(
						pressedX,pressedY);
				}
				//control = select entire column
				else if(pressedEvent.isMetaDown()) {
					processLeftSingleControlClick(pressedX,
						pressedY);
				}
				//Regular click = select a spot
				else {
					processLeftSingleClick(pressedX,
						pressedY);
				}
			} else {
				imView.debug("Odd click count other than 1, so do nothing", 
						imView.debug);
			}
		} else if(SwingUtilities.isRightMouseButton(pressedEvent)) {
			processRightSingleClick(pressedX,
				pressedY);
		}
	}

	/**
	 * Processed a completed left single click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftSingleClick(int xPixel,int yPixel) {
	
		final Point start = new Point(xmap.getIndex(xPixel),
			ymap.getIndex(yPixel));
		final Point end   = new Point(xmap.getIndex(xPixel),
			ymap.getIndex(yPixel));
	
		mvController.selectRectangle(start, end);
	
		imView.repaint();
	}
	
	/**
	 * Processed a completed left single shift click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftSingleShiftClick(int xPixel, int yPixel) {
	
		final Point start = new Point(xmap.getMinIndex(),ymap.getIndex(yPixel));
		final Point end   = new Point(xmap.getMaxIndex(),ymap.getIndex(yPixel));
	
		mvController.selectRectangle(start, end);
	
		imView.repaint();
	}
	
	/**
	 * Processed a completed left control click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftSingleControlClick(int xPixel, int yPixel) {
	
		final Point start = new Point(xmap.getIndex(xPixel),ymap.getMinIndex());
		final Point end   = new Point(xmap.getIndex(xPixel),ymap.getMaxIndex());
	
		mvController.selectRectangle(start, end);
	
		imView.repaint();
	}
	
	/**
	 * This method encapsulates the logic of zooming when a double-click with
	 * any modifier happens, however it is abstracted from clicks and keyboard
	 * keys.  It takes the click pixel coordinates, whether we're zooming in or
	 * out, the speed of the zoom (slow/med/fast), and whether we are doing a
	 * stepwise zoom or not, all the way to the clicked target.
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 * @param zoomIn
	 * @param stepwiseZoom
	 * @param zoomSpeed
	 */
	public void processLeftDoubleClickZoomEvent(int xPixel, int yPixel,
		boolean zoomIn, boolean stepwiseZoom,
		int zoomSpeed /* 0=slow,1=med,2=fast */) {
	
		//zoomDegree is only used when stepwiseZoom is false
		double zoomDegree = 0.3; //Medium TODO use MapContainer static vars... 	
	
		if(zoomSpeed == 0) {
			zoomDegree = 0.05;   //Slow
			
		} else if(zoomSpeed == 2) {
			zoomDegree = 0.6;    //Fast
		}
	
		if(zoomIn) {
			//If click was inside a contiguous selection and the selection
			//is smaller than the current zoom level or if the
			//selection is the same size but isn't equal to what
			//is visible
			if(pixelIsInsideSelection(xPixel,yPixel) &&
				(subSelectionIsSmallerThanVisible(xmap.getIndex(xPixel),
					ymap.getIndex(yPixel)) ||
					(subSelectionIsEqualToVisible(xmap.getIndex(xPixel),
						ymap.getIndex(yPixel)) &&
					//Selected area is not scrolled to
					//visible area
					(rowSelection.getMinContiguousIndex(ymap.getIndex(yPixel))
						!= ymap.getFirstVisible() ||
					colSelection.getMinContiguousIndex(xmap.getIndex(xPixel))
					!= xmap.getFirstVisible())))) {
	
				if(stepwiseZoom) {
					if(zoomSpeed == 2) {
						imView.hardZoomToTarget(
							colSelection.getMinContiguousIndex(
								xmap.getIndex(xPixel)),
							(colSelection.getMaxContiguousIndex(
								xmap.getIndex(xPixel)) -
								colSelection.getMinContiguousIndex(
									xmap.getIndex(xPixel)) + 1),
							rowSelection.getMinContiguousIndex(
								ymap.getIndex(yPixel)),
							(rowSelection.getMaxContiguousIndex(
								ymap.getIndex(yPixel)) -
								rowSelection.getMinContiguousIndex(
									ymap.getIndex(yPixel)) + 1));
					} else {
						//Zoom to sub-selection
						imView.smoothAnimatedZoomToTarget(
							colSelection.getMinContiguousIndex(
								xmap.getIndex(xPixel)),
							(colSelection.getMaxContiguousIndex(
								xmap.getIndex(xPixel)) -
								colSelection.getMinContiguousIndex(
									xmap.getIndex(xPixel)) + 1),
							rowSelection.getMinContiguousIndex(
								ymap.getIndex(yPixel)),
							(rowSelection.getMaxContiguousIndex(
								ymap.getIndex(yPixel)) -
								rowSelection.getMinContiguousIndex(
									ymap.getIndex(yPixel)) + 1));
					}
				} else {
					//Zoom to sub-selection
					imView.smoothAnimatedZoomTowardTarget(
						colSelection.getMinContiguousIndex(
							xmap.getIndex(xPixel)),
	
						(colSelection.getMaxContiguousIndex(
							xmap.getIndex(xPixel)) -
							colSelection.getMinContiguousIndex(
								xmap.getIndex(xPixel)) + 1),
	
						rowSelection.getMinContiguousIndex(ymap.getIndex(yPixel)),
	
						(rowSelection.getMaxContiguousIndex(ymap.getIndex(yPixel)) -
						rowSelection.getMinContiguousIndex(ymap.getIndex(yPixel)) +
						1),
	
						zoomDegree);
				}
			}
			//If click was inside a selection or between selections and the
			//selection area is smaller than the current zoom level or if the
			//selection area is the same size but isn't equal to what
			//is visible
			else if(pixelIsAmidstSelection(xPixel,yPixel) &&
				(selectionIsSmallerThanVisible() ||
					(selectionIsEqualToVisible() &&
						//Selected area is not scrolled to
						//visible area
						(rowSelection.getMinIndex() !=
							ymap.getFirstVisible() ||
							colSelection.getMinIndex() !=
							xmap.getFirstVisible())))) {
	
				if(stepwiseZoom) {
					if(zoomSpeed == 2) {
						imView.hardZoomToTarget(
							colSelection.getMinIndex(),
							colSelection.getFullSelectionRange(),
							rowSelection.getMinIndex(),
							rowSelection.getFullSelectionRange());
						
					} else {
						//Zoom to selection
						imView.smoothAnimatedZoomToTarget(
							colSelection.getMinIndex(),
							colSelection.getFullSelectionRange(),
							rowSelection.getMinIndex(),
							rowSelection.getFullSelectionRange());
					}
				} else {
					//Zoom toward selection
					imView.smoothAnimatedZoomTowardTarget(
						colSelection.getMinIndex(),
						colSelection.getFullSelectionRange(),
						rowSelection.getMinIndex(),
						rowSelection.getFullSelectionRange(),
						zoomDegree);
				}
			}
			//Else full zoom in
			else {
				if(stepwiseZoom) {
					if(zoomSpeed == 2) {
						imView.hardZoomToTarget(xmap.getIndex(xPixel),1,
							ymap.getIndex(yPixel),1);
						
					} else {
						imView.smoothAnimatedZoomToTarget(xmap.getIndex(xPixel),1,
							ymap.getIndex(yPixel),1);
					}
					
				} else {
					imView.smoothAnimatedZoomTowardPixel(xPixel,yPixel,zoomDegree);
				}
			}
			
		} else {
			if(stepwiseZoom) {
				//If click was inside a contiguous selection and the selection
				//is larger than the current zoom level
				if(pixelIsInsideSelection(xPixel,yPixel) &&
					(subSelectionIsLargerThanVisible(xmap.getIndex(xPixel),
						ymap.getIndex(yPixel)))) {
	
					if(zoomSpeed == 2) {
						//Zoom to sub-selection
						imView.hardZoomToTarget(
							colSelection.getMinContiguousIndex(
								xmap.getIndex(xPixel)),
							(colSelection.getMaxContiguousIndex(
								xmap.getIndex(xPixel)) -
								colSelection.getMinContiguousIndex(
									xmap.getIndex(xPixel)) + 1),
							rowSelection.getMinContiguousIndex(
								ymap.getIndex(yPixel)),
							(rowSelection.getMaxContiguousIndex(
								ymap.getIndex(yPixel)) -
								rowSelection.getMinContiguousIndex(
									ymap.getIndex(yPixel)) + 1));
					} else {
						//Zoom to sub-selection
						imView.smoothAnimatedZoomToTarget(
							colSelection.getMinContiguousIndex(
								xmap.getIndex(xPixel)),
							(colSelection.getMaxContiguousIndex(
								xmap.getIndex(xPixel)) -
								colSelection.getMinContiguousIndex(
									xmap.getIndex(xPixel)) + 1),
							rowSelection.getMinContiguousIndex(
								ymap.getIndex(yPixel)),
							(rowSelection.getMaxContiguousIndex(
								ymap.getIndex(yPixel)) -
								rowSelection.getMinContiguousIndex(
									ymap.getIndex(yPixel)) + 1));
					}
				}
				//If click was inside a selection or between selections and the
				//selection area is larger than the current zoom level
				else if(pixelIsAmidstSelection(xPixel,yPixel) &&
					(selectionIsLargerThanVisible())) {
	
					if(zoomSpeed == 2) {
	
						//Zoom to selection
						imView.hardZoomToTarget(
							colSelection.getMinIndex(),
							(colSelection.getMaxIndex() -
								colSelection.getMinIndex() + 1),
							rowSelection.getMinIndex(),
							(rowSelection.getMaxIndex() -
								rowSelection.getMinIndex() + 1));
					} else {
						//Zoom to selection
						imView.smoothAnimatedZoomToTarget(
							colSelection.getMinIndex(),
							(colSelection.getMaxIndex() -
								colSelection.getMinIndex() + 1),
							rowSelection.getMinIndex(),
							(rowSelection.getMaxIndex() -
								rowSelection.getMinIndex() + 1));
					}
				}
				//Else full zoom out
				else {
					if(zoomSpeed == 2) {
						imView.hardZoomToTarget(xmap.getMinIndex(), 
								xmap.getMaxIndex() + 1, ymap.getMinIndex(), 
								ymap.getMaxIndex() + 1);
						
					} else {
	
						imView.smoothAnimatedZoomOut();
					}
				}
			} else {
	
				//All incomplete zoom-outs will perform a precise zoom-out from
				//the pixel clicked, partly because I haven't implemented a
				//zoom-away method which takes selection boundaries, but also to
				//provide more discrete control of where precisely to be zoomed
				imView.smoothAnimatedZoomFromPixel(xPixel,yPixel,zoomDegree);
			}
		}
	}
	
	/**
	 * Processed a completed left double click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDoubleClick(int xPixel,int yPixel) {
	
		processLeftDoubleClickZoomEvent(xPixel,yPixel,true,false,1);
	}
	
	/**
	 * Processed a completed left double control click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDoubleControlClick(int xPixel,int yPixel) {
	
		processLeftDoubleClickZoomEvent(xPixel,yPixel,true,false,0);
	}
	
	/**
	 * Processed a completed left double shift click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDoubleShiftClick(int xPixel,int yPixel) {
	
		processLeftDoubleClickZoomEvent(xPixel,yPixel,true,false,2);
	}
	
	/**
	 * Processed a completed left double command click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDoubleCommandClick(int xPixel,int yPixel) {
	
		processLeftDoubleClickZoomEvent(xPixel,yPixel,true,true,1);
	}
	
	/**
	 * Returns true if the pixel is either inside a selection or between
	 * multiple selections, false otherwise
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 * @return boolean
	 */
	public boolean pixelIsAmidstSelection(int xPixel,int yPixel) {
	
		return(//A selection exists
			rowSelection != null && colSelection != null &&
			//Click is amidst a selected area
			rowSelection.getMinIndex() <= ymap.getIndex(yPixel) &&
			rowSelection.getMaxIndex() >= ymap.getIndex(yPixel) &&
			colSelection.getMinIndex() <= xmap.getIndex(xPixel) &&
			colSelection.getMaxIndex() >= xmap.getIndex(xPixel));
	}
	
	/**
	 * Returns true if the pixel is either inside a selection or between
	 * multiple selections, false otherwise
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 * @return boolean
	 */
	public boolean pixelIsInsideSelection(int xPixel,int yPixel) {
	
		return(//A selection exists
			rowSelection != null && colSelection != null &&
			//Click is amidst a selected area
			rowSelection.isIndexSelected(ymap.getIndex(yPixel)) &&
			colSelection.isIndexSelected(xmap.getIndex(xPixel)));
	}
	
	/**
	 * Returns true if a selection exists and the extreme boundaries of either
	 * dimension of the selection is smaller than the amount visible in the
	 * respective dimension, false otherwise
	 * @author rleach
	 * @return boolean
	 */
	public boolean selectionIsSmallerThanVisible() {
	
		return(//A selection exists
			rowSelection != null && colSelection != null &&
			(rowSelection.getMaxIndex() - rowSelection.getMinIndex() + 1) <
			ymap.getNumVisible() ||
			(colSelection.getMaxIndex() - colSelection.getMinIndex() + 1) <
			xmap.getNumVisible());
	}
	
	/**
	 * Returns true if a selection exists and the contiguously selected
	 * boundaries surrounding the supplied selected index of either
	 * dimension of the selection is smaller than the amount visible in the
	 * respective dimension, false otherwise
	 * @author rleach
	 * @return boolean
	 */
	public boolean subSelectionIsSmallerThanVisible(final int xIndex,
		final int yIndex) {
	
		return(//A selection exists
			rowSelection != null && colSelection != null &&
			(rowSelection.getMaxContiguousIndex(yIndex) -
			rowSelection.getMinContiguousIndex(yIndex) + 1) <
			ymap.getNumVisible() ||
			(colSelection.getMaxContiguousIndex(xIndex) -
			colSelection.getMinContiguousIndex(xIndex) + 1) <
			xmap.getNumVisible());
	}
	
	/**
	 * Returns true if a selection exists and the extreme boundaries of either
	 * dimension of the selection is larger than the amount visible in the
	 * respective dimension, false otherwise
	 * @author rleach
	 * @return boolean
	 */
	public boolean selectionIsLargerThanVisible() {
	
		return(//A selection exists
			rowSelection != null && colSelection != null &&
			(rowSelection.getMaxIndex() -
			rowSelection.getMinIndex() + 1) >
			ymap.getNumVisible() ||
			(colSelection.getMaxIndex() -
			colSelection.getMinIndex() + 1) >
			xmap.getNumVisible());
	}
	
	/**
	 * Returns true if a selection exists and the contiguously selected
	 * boundaries surrounding the supplied selected index of either
	 * dimension of the selection is larger than the amount visible in the
	 * respective dimension, false otherwise
	 * @author rleach
	 * @return boolean
	 */
	public boolean subSelectionIsLargerThanVisible(final int xIndex,
		final int yIndex) {
	
		return(//A selection exists
			rowSelection != null && colSelection != null &&
			(rowSelection.getMaxContiguousIndex(yIndex) -
			rowSelection.getMinContiguousIndex(yIndex) + 1) >
			ymap.getNumVisible() ||
			(colSelection.getMaxContiguousIndex(xIndex) -
			colSelection.getMinContiguousIndex(xIndex) + 1) >
			xmap.getNumVisible());
	}
	
	/**
	 * Returns true if a selection exists and the extreme boundaries of both
	 * dimensions of the selection are equal to the amount visible in the
	 * respective dimensions, false otherwise
	 * @author rleach
	 * @return boolean
	 */
	public boolean selectionIsEqualToVisible() {
	
		return(//A selection exists
			rowSelection != null && colSelection != null &&
			(rowSelection.getMaxIndex() - rowSelection.getMinIndex() + 1) ==
			ymap.getNumVisible() &&
			(colSelection.getMaxIndex() - colSelection.getMinIndex() + 1) ==
			xmap.getNumVisible());
	}
	
	/**
	 * Returns true if a selection exists and the contiguously selected
	 * boundaries surrounding the supplied selected index of either
	 * dimension of the selection is larger than the amount visible in the
	 * respective dimension, false otherwise
	 * @author rleach
	 * @param xIndex
	 * @param yIndex
	 * @return boolean
	 */
	public boolean subSelectionIsEqualToVisible(final int xIndex,
		final int yIndex) {
	
		return(//A selection exists
			rowSelection != null && colSelection != null &&
			(rowSelection.getMaxContiguousIndex(yIndex) -
			rowSelection.getMinContiguousIndex(yIndex) + 1) ==
			ymap.getNumVisible() &&
			(colSelection.getMaxContiguousIndex(xIndex) -
			colSelection.getMinContiguousIndex(xIndex) + 1) ==
			xmap.getNumVisible());
	}
	
	/**
	 * Performs the actions related to a Left-Double-Shift-Command-Click
	 * @author rleach
	 * @return none
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDoubleShiftCommandClick(int xPixel,int yPixel) {
	
		processLeftDoubleClickZoomEvent(xPixel, yPixel, true, true, 2);
	}
	
	/**
	 * Processes a completed left double option control click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDoubleOptionControlClick(int xPixel,int yPixel) {
	
		processLeftDoubleClickZoomEvent(xPixel, yPixel, false, false, 0);
	}
	
	/**
	 * Processes a completed left double option click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDoubleOptionClick(int xPixel,int yPixel) {
	
		processLeftDoubleClickZoomEvent(xPixel, yPixel, false, false, 1);
	}
	
	/**
	 * Processes a completed left double option shift click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDoubleOptionShiftClick(int xPixel,int yPixel) {
	
		processLeftDoubleClickZoomEvent(xPixel, yPixel, false, false, 2);
	}
	
	/**
	 * Processes a completed left double option command click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDoubleOptionCommandClick(int xPixel,int yPixel) {
	
		processLeftDoubleClickZoomEvent(xPixel, yPixel, false, true, 1);
	}
	
	/**
	 * Processes a completed left double option shift command click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDoubleOptionShiftCommandClick(int xPixel,
		int yPixel) {
	
		processLeftDoubleClickZoomEvent(xPixel, yPixel, false, true, 2);
	}
	
	/**
	 * Processes left click drag start event
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDragStart(int xPixel, int yPixel) {
		
		//If this is a correction of the starting point, clear out the previous
		//dragRect
		if(xmap.isSelecting()) {
			imView.debug("processLeftDragStart: Correcting drag start", 10);
			endDragRect(endPoint.x, endPoint.y);
			imView.drawBand(getPixelRect(dragRect));
		}
		
		xmap.setSelecting(true);
		ymap.setSelecting(true);
		
		xmap.setSelectingStart(xmap.getIndex(xPixel));
		ymap.setSelectingStart(ymap.getIndex(yPixel));
		
		startPoint.setLocation(xmap.getIndex(xPixel), ymap.getIndex(yPixel));
		endPoint.setLocation(startPoint);
	
		dragRect.setLocation(startPoint);
		dragRect.setSize(endPoint.x - dragRect.x, endPoint.y - dragRect.y);
	
		imView.drawBand(getPixelRect(dragRect));
	}
	
	/**
	 * Processes left click temporary drag start event (called by
	 * mouseDragged and not the timer which detects multi-clicks so that double-
	 * clicks where the mouse is accidentally slightly dragged can perform a
	 * double-click instead of a drag event and potentially mess up a user's
	 * existing selection)
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processTemporaryLeftDragStart(int xPixelStart, int yPixelStart,
		int xPixel, int yPixel) {
		
		startPoint.setLocation(xmap.getIndex(xPixelStart),
			ymap.getIndex(yPixelStart));
		
		endPoint.setLocation(xmap.getIndex(xPixel), ymap.getIndex(yPixel));
	
		dragRect.setLocation(startPoint);
		dragRect.setSize(endPoint.x - dragRect.x, endPoint.y - dragRect.y);
	
		imView.drawBand(getPixelRect(dragRect));
		
		imView.repaint();
	}
	
	/**
	 * Processes left click drag end event
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDragEnd(int xPixel, int yPixel) {
		
		xmap.setSelecting(false);
		ymap.setSelecting(false);
		
		xmap.setSelectingStart(-1);
		ymap.setSelectingStart(-1);
		
		xmap.setHoverIndex(xmap.getIndex(xPixel));
		ymap.setHoverIndex(ymap.getIndex(yPixel));
	
		imView.drawBand(getPixelRect(dragRect));
		
		endPoint.setLocation(xmap.getIndex(xPixel), ymap.getIndex(yPixel));
	
		dragRect.setLocation(startPoint);
		dragRect.setSize(0,0);
		dragRect.add(endPoint);
	
		imView.drawBand(getPixelRect(dragRect));
		
		mvController.selectRectangle(startPoint, endPoint);
	
		imView.repaint();
	}
	
	/**
	 * Processes left shift click drag end event
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftShiftDragEnd(int xPixel, int yPixel) {
		
		xmap.setSelecting(false);
		ymap.setSelecting(false);
		
		xmap.setSelectingStart(-1);
		ymap.setSelectingStart(-1);
		
		xmap.setHoverIndex(xmap.getIndex(xPixel));
		ymap.setHoverIndex(ymap.getIndex(yPixel));
	
		imView.drawBand(getPixelRect(dragRect));
		
		startPoint.setLocation(xmap.getMinIndex(), startPoint.y);
		endPoint.setLocation(xmap.getMaxIndex(), ymap.getIndex(yPixel));
		
		dragRect.setLocation(startPoint);
		dragRect.setSize(0, 0);
		dragRect.add(endPoint);
	
		imView.drawBand(getPixelRect(dragRect));
		
		mvController.selectRectangle(startPoint, endPoint);
	
		imView.repaint();
	}
	
	/**
	 * Processes left control click drag end event
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftControlDragEnd(int xPixel, int yPixel) {
		
		xmap.setSelecting(false);
		ymap.setSelecting(false);
		xmap.setSelectingStart(-1);
		ymap.setSelectingStart(-1);
		
		imView.debug("Mouse dragged. Updating hover indexes to [" +
		      xmap.getIndex(xPixel) + "x" + ymap.getIndex(yPixel) +
		      "]",4);
		
		xmap.setHoverIndex(xmap.getIndex(xPixel));
		ymap.setHoverIndex(ymap.getIndex(yPixel));
	
		imView.drawBand(getPixelRect(dragRect));
		
		startPoint.setLocation(startPoint.x, ymap.getMinIndex());
		endPoint.setLocation(xmap.getIndex(xPixel), ymap.getMaxIndex());
		
		dragRect.setLocation(startPoint);
		dragRect.setSize(0, 0);
		dragRect.add(endPoint);
	
		imView.drawBand(getPixelRect(dragRect));
		
		mvController.selectRectangle(startPoint, endPoint);
	
		imView.repaint();
	}
	
	/**
	 * Processes left click drag movement event
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDragDuring(int xPixelStart, int yPixelStart,
		int xPixel, int yPixel) {
		
		//If the timer hasn't done this yet, we now have confirmation this is a
		//left-click drag event
		if(!xmap.isSelecting()) {
			processTemporaryLeftDragStart(xPixelStart, yPixelStart, xPixel,
				yPixel);
		}
	
//		imView.debug("Mouse dragged. Updating hover indexes to [" +
//		      xmap.getIndex(xPixel) + "x" + ymap.getIndex(yPixel) +
//		      "]", 4);
		
		imView.debug("Mouse dragged. Pixels: [" +
			      xPixelStart + ", " + yPixelStart + ", " + xPixel +
			      ", " + yPixel + "]", 4);
		
		xmap.setHoverIndex(xmap.getIndex(xPixel));
		ymap.setHoverIndex(ymap.getIndex(yPixel));
	
		imView.drawBand(getPixelRect(dragRect));
	
		startPoint.setLocation(xmap.getIndex(xPixelStart), 
				ymap.getIndex(yPixelStart));
		endPoint.setLocation(xmap.getIndex(xPixel), ymap.getIndex(yPixel));
		
		imView.debug("Indexes: [" +
				xmap.getIndex(xPixelStart) + ", " + ymap.getIndex(yPixelStart) + ", " + xmap.getIndex(xPixel) +
			      ", " + ymap.getIndex(yPixel) + "]", 4);
	
		/* Normal selection */
		dragRect.setLocation(startPoint);
		dragRect.setSize(0, 0);
		dragRect.add(endPoint);
	
		imView.drawBand(getPixelRect(dragRect));
	}
	
	/**
	 * Processes left shift click drag movement event
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftShiftDragDuring(int xPixelStart, int yPixelStart,
		int xPixel, int yPixel) {
	
		//If the timer hasn't done this yet, we now have confirmation this is a
		//left-click drag event
		if(!xmap.isSelecting()) {
			processTemporaryLeftDragStart(xPixelStart,yPixelStart,xPixel,
				yPixel);
		}
	
		imView.debug("Mouse dragged. Updating hover indexes to [" +
		      xmap.getIndex(xPixel) + "x" + ymap.getIndex(yPixel) +
		      "]",4);
		
		xmap.setHoverIndex(xmap.getIndex(xPixel));
		ymap.setHoverIndex(ymap.getIndex(yPixel));
	
		imView.drawBand(getPixelRect(dragRect));
	
		startPoint.setLocation(xmap.getMinIndex(), ymap.getIndex(yPixelStart));
		endPoint.setLocation(xmap.getMaxIndex(), ymap.getIndex(yPixel));
	
		/* Full gene selection */
		dragRect.setLocation(startPoint);
		dragRect.setSize(0, 0);
		dragRect.add(endPoint);
	
		imView.drawBand(getPixelRect(dragRect));
	}
	
	/**
	 * Processes left control click drag movement event
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftControlDragDuring(int xPixelStart, int yPixelStart,
		int xPixel, int yPixel) {

		//If the timer hasn't done this yet, we now have confirmation this is a
		//left-click drag event
		if(!xmap.isSelecting()) {
			processTemporaryLeftDragStart(xPixelStart,yPixelStart,xPixel,
				yPixel);
		}
	
		imView.debug("Mouse dragged. Updating hover indexes to [" +
		      xmap.getIndex(xPixel) + "x" + ymap.getIndex(yPixel) +
		      "]",4);
		
		xmap.setHoverIndex(xmap.getIndex(xPixel));
		ymap.setHoverIndex(ymap.getIndex(yPixel));
	
		imView.drawBand(getPixelRect(dragRect));
	
		startPoint.setLocation(xmap.getIndex(xPixelStart), ymap.getMinIndex());
		endPoint.setLocation(xmap.getIndex(xPixel), ymap.getMaxIndex());
	
		/* Full array selection */
		dragRect.setLocation(startPoint);
		dragRect.setSize(0, 0);
		dragRect.add(endPoint);
	
		imView.drawBand(getPixelRect(dragRect));
	}
	
	/**
	 * Processes a completed right single click.
	 * TODO Delete parameters from method signature UNLESS method will be used
	 * in a different way which requires them, since we talked about switching
	 * the deselect-action to another key combo.
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processRightSingleClick(int xPixel,int yPixel) {
		
		mvController.deselectAll();
	}
	
	// TODO use or delete.
	private void startDragRect(int xIndex, int yIndex) {
		
		startPoint.setLocation(xIndex, yIndex);
		endPoint.setLocation(startPoint);
		dragRect.setLocation(startPoint);
		dragRect.setSize(endPoint.x - dragRect.x,endPoint.y - dragRect.y);
		
		imView.drawBand(getPixelRect(dragRect));
	}
	
	/**
	 * TODO add JavaDoc
	 * @param xIndex
	 * @param yIndex
	 */
	private void updateDragRect(final int xIndex, final int yIndex) {
		
		imView.drawBand(getPixelRect(dragRect));
		endPoint.setLocation(xIndex, yIndex);
		
		dragRect.setLocation(startPoint);
		dragRect.setSize(0,0);
		dragRect.add(endPoint);
	}
	
	/**
	 * TODO add JavaDoc
	 * @param xIndex
	 * @param yIndex
	 */
	private void endDragRect(final int xIndex, final int yIndex) {
		
		updateDragRect(xIndex, yIndex);
		imView.drawBand(getPixelRect(dragRect));
	}
	
	/**
	 * Takes a rectangle defined by index ranges and returns another rectangle
	 * which describes the index range as a range of pixels according to the 
	 * MapContainers.
	 * @param l A rectangle described by row or column indexes.
	 * @return A rectangle describing pixel ranges in InteractiveMatrixView.
	 */
	private Rectangle getPixelRect(final Rectangle l) {
		
		int x = xmap.getPixel(l.x);
		int y = ymap.getPixel(l.y);
		int w = xmap.getPixel(l.x + l.width  + 1) - x;
		int h = ymap.getPixel(l.y + l.height + 1) - y;
		
		return new Rectangle(x, y, w, h);
	}
}
