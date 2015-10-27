/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: GlobalView.java,v $
 * $Revision: 1.1 $
 * $Date: 2006-08-16 19:13:45 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular,
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER
 */
package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeSelectionI;

public class InteractiveMatrixView extends MatrixView implements
		MouseWheelListener {

	private static final long serialVersionUID = 1L;

	public static final int FILL = 0;
	public static final int EQUAL = 1;
	public static final int PROPORT = 2;

	private final String[] statustext = new String[] { "Mouseover Selection",
			"", "" };
	private HeaderInfo arrayHI;
	private HeaderInfo geneHI;

	private HeaderSummary geneSummary;
	private HeaderSummary arraySummary;

	private int overx;
	private int overy;

	private double aspectRatio = -1;

	/**
	 * Points to track candidate selected rows/cols should reflect where the
	 * mouse has actually been
	 */
	private final Point startPoint = new Point();
	private final Point endPoint = new Point();

	/**
	 * This rectangle keeps track of where the drag rect was drawn
	 */
	private final Rectangle dragRect = new Rectangle();

	/**
	 * Rectangle to track yellow selected rectangle (pixels)
	 */
	private List<Rectangle> selectionRectList = new ArrayList<Rectangle>();

	/**
	 * Circle to be used as indicator for selection
	 */
	private List<Ellipse2D.Double> indicatorCircleList = null;

	GlobalMatrixView globalMatrixView = null;

	/**
	 * GlobalView also likes to have an globalxmap and globalymap (both of type
	 * MapContainer) to help it figure out where to draw things. It also tries
	 * to
	 */
	public InteractiveMatrixView() {

		super();

		setLabelPortMode(true);
		debug = 0;
		//1 = Debug double-click detection
		//10 = Debug click dragging
		//11 = Debug zoom animation "toward" selection/sub-selection
		//12 = Debug double-click zooming

		/* Listeners for interactivity */
		addMouseListener(new MatrixMouseListener());
		addMouseMotionListener(new MatrixMouseListener());
		addMouseWheelListener(this);
	}

	/**
	 * 
	 * @return x-axis scroll bar (horizontal) for the InteractiveMatrixView.
	 */
	public JScrollBar getXMapScroll() {

		return scrollPane.getHorizontalScrollBar();
	}

	/**
	 * 
	 * @return y-axis scroll bar (vertical) for the InteractiveMatrixView.
	 */
	public JScrollBar getYMapScroll() {

		return scrollPane.getVerticalScrollBar();
	}

	public void setGlobalMatrixView(GlobalMatrixView gmv) {
		globalMatrixView = gmv;
	}

	@Override
	public String[] getStatus() {

		try {
			if (xmap.contains(overx) && ymap.contains(overy)) {
				statustext[0] = "";

				if (geneHI != null) {
					final int realGene = overy;
					try {
						statustext[0] += geneSummary.getSummary(geneHI,
								realGene);

					} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
						LogBuffer.println("ArrayIndexOutOfBoundsException "
								+ "in getStatus() in GlobalView: "
								+ e.getMessage());
						statustext[0] += " (N/A)";
					}
				}
				statustext[1] = "";
				if (arrayHI != null) {
					try {
						statustext[1] += arraySummary
								.getSummary(arrayHI, overx);

					} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
						LogBuffer.println("ArrayIndexOutOfBoundsException "
								+ "in getStatus() in GlobalView: "
								+ e.getMessage());
						statustext[1] += " (N/A)";
					}
				}

				if (drawer != null) {
					if (drawer.isMissing(overx, overy)) {
						statustext[2] = "No Data";

					} else if (drawer.isEmpty(overx, overy)) {
						statustext[2] = "";

					} else {
						statustext[2] = drawer.getSummary(overx, overy);
					}
				}
			}
		} catch (final ArrayIndexOutOfBoundsException e) {
			LogBuffer.println("ArrayIndexOutOfBoundsException "
					+ "in getStatus() in GlobalView: " + e.getMessage());
		}
		return statustext;
	}

	public void setHeaderSummary(final HeaderSummary gene,
			final HeaderSummary array) {

		this.geneSummary = gene;
		this.arraySummary = array;
	}

	/**
	 * Get ArrayDrawer
	 *
	 * @return The current ArrayDrawer
	 */
	public ArrayDrawer getArrayDrawer() {

		return drawer;
	}

	@Override
	public String viewName() {

		return "InteractiveMatrixView";
	}

	// Canvas Methods
	/**
	 * This method updates the graphics object directly by asking the
	 * ArrayDrawer to draw on it directly. The alternative is to have a pixel
	 * buffer which you update using updatePixels.
	 */
	@Override
	protected void updateBuffer(final Graphics g) {

		if(xmap.hoverChanged() || ymap.hoverChanged()) {
			xmap.unsetHoverChanged();
			ymap.unsetHoverChanged();
			return;
		}
		Graphics2D g2d = (Graphics2D) g;

		revalidateScreen();

		if (!offscreenValid) {
			// clear the pallette...
			g2d.setColor(GUIFactory.DEFAULT_BG);
			g2d.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
			g2d.setColor(Color.black);

			final Rectangle destRect = new Rectangle(0, 0,
					xmap.getUsedPixels(), ymap.getUsedPixels());

			final Rectangle sourceRect = new Rectangle(xmap.getIndex(0),
					ymap.getIndex(0), xmap.getIndex(destRect.width)
							- xmap.getIndex(0), ymap.getIndex(destRect.height)
							- ymap.getIndex(0));

			if ((sourceRect.x >= 0) && (sourceRect.y >= 0) && drawer != null) {
				drawer.paint(g2d, sourceRect, destRect, null);
			}
		}
	}

	@Override
	public synchronized void paintComposite(final Graphics g) {

		if(xmap.hoverChanged() || ymap.hoverChanged()) {
			xmap.unsetHoverChanged();
			ymap.unsetHoverChanged();
			return;
		}
		Graphics2D g2 = (Graphics2D) g;

		if (selectionRectList != null) {
			//Reinitialize the graphics object
			g2 = (Graphics2D) g;

			/* draw all selection rectangles in yellow */
			g2.setColor(Color.yellow);

			for (final Rectangle rect : selectionRectList) {
				g2.drawRect(rect.x, rect.y, rect.width, rect.height);
			}

			//debug("Preparing to draw ellipses.");
			/*
			 * draw white selection circle if only 1 tile is selected and small
			 * enough.
			 */
			if (indicatorCircleList != null) {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(Color.yellow);
				g2.setStroke(new BasicStroke(3));
				for(Ellipse2D.Double indicatorCircle : indicatorCircleList) {
					//debug("Drawing ellipse.");
					g2.draw(indicatorCircle);
				}
			}
		}
	}

	/**
	 * Checks the selection of genes and arrays and calculates the appropriate
	 * selection rectangle.
	 */
	@Override
	protected void recalculateOverlay() {

		if(xmap.hoverChanged() || ymap.hoverChanged()) {
			xmap.unsetHoverChanged();
			ymap.unsetHoverChanged();
			return;
		}

		if((geneSelection == null) || (arraySelection == null)) {
			selectionRectList   = null;
			indicatorCircleList = null;
		} else {

			selectionRectList = new ArrayList<Rectangle>();
	
			final int[] selectedArrayIndexes =
				arraySelection.getSelectedIndexes();
			final int[] selectedGeneIndexes =
				geneSelection.getSelectedIndexes();
	
			globalMatrixView.setIMVselectedIndexes(selectedArrayIndexes,
												   selectedGeneIndexes);
	
			if (selectedArrayIndexes.length > 0) {
	
				// debug("Selected min array index: [" +
				// selectedArrayIndexes[0] + "] Selected min gene index: [" +
				// selectedGeneIndexes[0] + "].");
	
				List<List<Integer>> arrayBoundaryList;
				List<List<Integer>> geneBoundaryList;
	
				arrayBoundaryList =
					findRectangleBoundaries(selectedArrayIndexes,xmap);
				geneBoundaryList =
					findRectangleBoundaries(selectedGeneIndexes,ymap);
	
				// Make the rectangles
				if (selectionRectList != null) {
					for(final List<Integer> xBoundaries : arrayBoundaryList) {
	
						for(final List<Integer> yBoundaries : geneBoundaryList){
	
							selectionRectList
								.add(new Rectangle(xBoundaries.get(0),
									yBoundaries.get(0), xBoundaries.get(1)
									- xBoundaries.get(0), yBoundaries
									.get(1) - yBoundaries.get(0)));
						}
					}
				}
			}
			
			setIndicatorCircleBounds();
		}
	}

	/**
	 * Finds the boundaries needed to draw all selection rectangles
	 *
	 * @param selectedIndexes
	 * @param map
	 * @return
	 */
	protected List<List<Integer>> findRectangleBoundaries(
			final int[] selectedIndexes, final MapContainer map) {

		int sp = 0;
		int ep = 0;

		final List<List<Integer>> rangeList = new ArrayList<List<Integer>>();
		final List<List<Integer>> boundaryList = new ArrayList<List<Integer>>();
		List<Integer> rectangleRange = new ArrayList<Integer>();

		/*
		 * If array is bigger than 1, check how many consecutive labels are
		 * selected by finding out which elements are only 1 apart. Store
		 * consecutive indexes separately to make separate rectangles later.
		 */
		int loopStart = 0;
		while (loopStart < selectedIndexes.length) {

			for (int i = loopStart; i < selectedIndexes.length; i++) {

				final int current = selectedIndexes[i];

				if (rectangleRange.size() == 0 ||
					rectangleRange.get(rectangleRange.size() - 1) ==
					current - 1) {
					rectangleRange.add(current);
					loopStart = i + 1;

				} else {
					break;
				}
			}

			rangeList.add(rectangleRange);
			rectangleRange = new ArrayList<Integer>();
		}

		/*
		 * For every selection range produce map values (rectangle boundaries)
		 * for each rectangle to be drawn.
		 */
		for (final List<Integer> selectionRange : rangeList) {

			final List<Integer> boundaries = new ArrayList<Integer>(2);

			sp = map.getPixel(selectionRange.get(0));
			ep = map.getPixel(selectionRange.get(selectionRange.size() - 1) + 1);
			// removed + 1 here due to new image drawing (otherwise selection
			//was not accurate by 1 pixel)

			if (ep < sp) {
				ep = sp;
				// correct for roundoff error above
			}

			boundaries.add(sp);
			boundaries.add(ep);

			boundaryList.add(boundaries);
		}

		return boundaryList;
	}

	@Override
	public void update(final Observable o, final Object arg) {

		if(xmap.hoverChanged() || ymap.hoverChanged()) {
			xmap.unsetHoverChanged();
			ymap.unsetHoverChanged();
			return;
		}
		if (o == geneSelection) {
			updateSelection(geneSelection, arraySelection);
			recalculateOverlay();

			/* trigger updatePixels() */
			offscreenValid = false;
			repaint();

		} else if (o == arraySelection) {
			updateSelection(arraySelection, geneSelection);
			recalculateOverlay();

			offscreenValid = false;
			repaint();

		} else// if(!xmap.hoverChanged() && !ymap.hoverChanged()) {
			super.update(o, arg);
//		} else {
//			if(xmap.hoverChanged()) {
//				xmap.unsetHoverChanged();
//			}
//			if(ymap.hoverChanged()) {
//				ymap.unsetHoverChanged();
//			}
//		}
	}

	/**
	 * Adjusts one axis selection to changes in the other.
	 * 
	 * @param origin
	 *            The axis selection object that has previously been changed.
	 * @param adjusting
	 *            The axis selection object that needs to be adjusted.
	 */
	private static void updateSelection(TreeSelectionI origin,
			TreeSelectionI adjusting) {

		if (adjusting.getNSelectedIndexes() == 0) {
			if (origin.getNSelectedIndexes() != 0) {
				// select all genes if some arrays selected...
				adjusting.selectAllIndexes();
				// notifies self...
				adjusting.notifyObservers();
				return;
			}
			/*
			 * When deselecting a tree node with right click, this matters,
			 * because in the eventlistener you can only deselect indices
			 * for the local tree selection. other axis here!
			 */
		} else if (origin.getNSelectedIndexes() == 0) {
			adjusting.deselectAllIndexes();
			adjusting.notifyObservers();
		}
	}

	//Had to put these in the main class, because when they were in
	//MatrixMouseListener, setting them in mousePressed did not make them
	//available when mouseDragged ran
	int pressedX;
	int pressedY;
	MouseEvent dragEvent;
	MouseEvent pressedEvent;   //To process clicks initiated via timer

	/* TODO move to a specified controller class */
	private class MatrixMouseListener extends MouseAdapter {

		boolean    isMousePressed;
		int        clickCount;     //Need to determine single/double/etc clicks

		@Override
		public void mouseMoved(final MouseEvent e) {
			debug("mouseMoved inside IMV",9);
			xmap.setHoverIndex(xmap.getIndex(e.getX()));
			ymap.setHoverIndex(ymap.getIndex(e.getY()));

			setDataStatus(e);
		}

		private void setDataStatus(final MouseEvent e) {

			final int ooverx = overx;
			final int oovery = overy;
			overx = xmap.getIndex(e.getX());
			overy = ymap.getIndex(e.getY());

			/* Timed repaint to avoid constant unnecessary repainting. */

			if (oovery != overy || ooverx != overx) {
				if (status != null) {
					status.setMessages(getStatus());
				}
			}
		}

		@Override
		public void mouseDragged(final MouseEvent e) {

			//TODO don't draw when out of focus... doesn't recognize
			//isMousePressed...
			if(!enclosingWindow().isActive()) {
				return;
			}

			dragEvent = e;

			//When left button is used
			if(SwingUtilities.isLeftMouseButton(e)) {
				//We are going to send both the start point and the current
				//point in case the timer has not yet started the drag
				debug("mouseDragged: pressedEvent is [" +
					((pressedEvent == null) ? "null" : "defined") + "].",10);
				debug("mouseDragged: pressedX [" + pressedX + "] pressedY [" +
					pressedY + "].",10);
				if(e.isShiftDown()) {
					processLeftShiftDragDuring(pressedX,pressedY,e.getX(),
						e.getY());
				} else if(e.isControlDown()) {
					processLeftControlDragDuring(pressedX,pressedY,e.getX(),
						e.getY());
				} else {
					processLeftDragDuring(pressedX,pressedY,e.getX(),e.getY());
				}
			}
		}

		@Override
		public void mouseReleased(final MouseEvent e) {

			if(!enclosingWindow().isActive() || !isMousePressed) {
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
			debug("Mouse released.",4);

			//At this point, we are assured the timer has stopped, so we need to
			//know if this is a release from a drag or a click event (possibly
			//multi). xmap.isSelecting will be true if it is a drag.
			//If this is a drag-select
			if(xmap.isSelecting()) {
				debug("Ending a drag-selection",10);
				/* Full gene selection */
				if(e.isShiftDown()) {
					processLeftShiftDragEnd(e.getX(),e.getY());
				}
				/* Full array selection */
				else if(e.isControlDown()) {
					processLeftControlDragEnd(e.getX(),e.getY());
				}
				/* Normal selection */
				else {
					processLeftDragEnd(e.getX(),e.getY());
				}
			}
			//Else, the user just had a long click in one spot, so select the
			//cell. It doesn't matter if they click again immediately after the
			//long click
			else {
				debug("Ending a quick click that didn't move",10);
				processClickEvents();
			}
		}

		@Override
		public void mousePressed(final MouseEvent e) {

			if(!enclosingWindow().isActive()) {
				return;
			}

			isMousePressed = true;

			debug("mousePressed: Setting pressedX [" + pressedX +
				"] and pressedY [" + pressedY + "].",10);

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
				
					debug("mouseEvent in IMV - ACTED ON",1);
					xmap.setOverInteractiveMatrix(false);
					ymap.setOverInteractiveMatrix(false);
					xmap.notifyObservers();
					ymap.notifyObservers();
					revalidate();
					repaint();
				}
			}
		};

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

						debug("Correcting drag start",10);
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

							debug("Correcting drag start",10);
							processLeftDragStart(pressedX,
								pressedY);
						}

						//Figure out the end of the drag event
						int dragX,dragY;
						if(dragEvent == null) {
							debug("dragEvent is null",10);
							Point p =
								MouseInfo.getPointerInfo().getLocation();
							SwingUtilities.convertPointFromScreen(p,
								getComponent());
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
							processLeftControlDragEnd(dragX,dragY);
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

		@Override
		public void mouseEntered(final MouseEvent e) {

			debug("mouseEntered IMV",9);
			if(this.turnOffLabelPortTimer != null) {
				/* Event came too soon, swallow it by resetting the timer.. */
				this.turnOffLabelPortTimer.stop();
				this.turnOffLabelPortTimer = null;
			}
			xmap.setOverInteractiveMatrix(true);
			ymap.setOverInteractiveMatrix(true);
			if (!enclosingWindow().isActive()) {
				return;
			}
			
			hasMouse = true;
			requestFocus();
		}

		@Override
		public void mouseExited(final MouseEvent e) {

			debug("mouseExited IMV",1);
			//Turn off the "over a label port view" boolean after a bit
			if(this.turnOffLabelPortTimer == null) {
				debug("mouseExited IMV - starting timer",1);
				/* Start waiting for delay millis to elapse and then
				 * call actionPerformed of the ActionListener
				 * "turnOffLabelPort". */
				if(delay == 0) {
					debug("mouseEvent in IMV - ACTED ON",1);
					xmap.setOverInteractiveMatrix(false);
					ymap.setOverInteractiveMatrix(false);
					xmap.notifyObservers();
					ymap.notifyObservers();
					revalidate();
					repaint();
				} else {
					this.turnOffLabelPortTimer = new Timer(this.delay,
							turnOffLabelPort);
					this.turnOffLabelPortTimer.start();
				}
			}

			//setOverInteractiveMatrix(false);
			hasMouse = false;

			xmap.setHoverIndex(-1);
			ymap.setHoverIndex(-1);
			// Display empty field
			statustext[0] = "";
			statustext[1] = "";
			statustext[2] = "";

			status.setMessages(statustext);
		}

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
					debug("Odd click count other than 1, so do nothing",debug);
				}
			} else if(SwingUtilities.isRightMouseButton(pressedEvent)) {
				processRightSingleClick(pressedX,
					pressedY);
			}
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
			} else {
				double xdist = dragEvent.getX() - pressedEvent.getX();
				double ydist = dragEvent.getY() - pressedEvent.getY();
				double distance = Math.sqrt(xdist*xdist + ydist*ydist);
				return(distance > maxPixelDistance);
			}
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
	
		selectRectangle(start, end);

		repaint();
	}

	/**
	 * Processed a completed left single shift click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftSingleShiftClick(int xPixel,int yPixel) {

		final Point start = new Point(xmap.getMinIndex(),ymap.getIndex(yPixel));
		final Point end   = new Point(xmap.getMaxIndex(),ymap.getIndex(yPixel));
	
		selectRectangle(start, end);

		repaint();
	}

	/**
	 * Processed a completed left control click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftSingleControlClick(int xPixel,int yPixel) {

		final Point start = new Point(xmap.getIndex(xPixel),ymap.getMinIndex());
		final Point end   = new Point(xmap.getIndex(xPixel),ymap.getMaxIndex());
	
		selectRectangle(start, end);

		repaint();
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
	public void processLeftDoubleClickZoomEvent(int xPixel,int yPixel,
		boolean zoomIn,boolean stepwiseZoom,
		int zoomSpeed /* 0=slow,1=med,2=fast */) {

		//zoomDegree is only used when stepwiseZoom is false
		double zoomDegree = 0.3; //Medium

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
					(geneSelection.getMinContiguousIndex(ymap.getIndex(yPixel))
						!= ymap.getFirstVisible() ||
					arraySelection.getMinContiguousIndex(xmap.getIndex(xPixel))
					!= xmap.getFirstVisible())))) {

				if(stepwiseZoom) {

					debug("Contiguous selection bounds: [array start:" +
						arraySelection.getMinContiguousIndex(xmap.getIndex(xPixel)) +
						", array stop:" +
						"(" + arraySelection.getMaxContiguousIndex(xmap.getIndex(xPixel)) + " - " + arraySelection.getMinContiguousIndex(xmap.getIndex(xPixel)) + " + 1)" +
						", gene start:" +
						geneSelection.getMinContiguousIndex(ymap.getIndex(yPixel)) +
						", gene stop:" + "(" + geneSelection.getMaxContiguousIndex(ymap.getIndex(yPixel)) + " - " + geneSelection.getMinContiguousIndex(ymap.getIndex(yPixel)) + " + 1)" +
						"]",12);
					if(zoomSpeed == 2) {

						debug("Hard zoom event to a contiguous selection",12);
						hardZoomToTarget(
							arraySelection.getMinContiguousIndex(
								xmap.getIndex(xPixel)),
							(arraySelection.getMaxContiguousIndex(
								xmap.getIndex(xPixel)) -
								arraySelection.getMinContiguousIndex(
									xmap.getIndex(xPixel)) + 1),
							geneSelection.getMinContiguousIndex(
								ymap.getIndex(yPixel)),
							(geneSelection.getMaxContiguousIndex(
								ymap.getIndex(yPixel)) -
								geneSelection.getMinContiguousIndex(
									ymap.getIndex(yPixel)) + 1));
					} else {

						debug("Smooth zoom event to a contiguous selection",12);
						//Zoom to sub-selection
						smoothAnimatedZoomToTarget(
							arraySelection.getMinContiguousIndex(
								xmap.getIndex(xPixel)),
							(arraySelection.getMaxContiguousIndex(
								xmap.getIndex(xPixel)) -
								arraySelection.getMinContiguousIndex(
									xmap.getIndex(xPixel)) + 1),
							geneSelection.getMinContiguousIndex(
								ymap.getIndex(yPixel)),
							(geneSelection.getMaxContiguousIndex(
								ymap.getIndex(yPixel)) -
								geneSelection.getMinContiguousIndex(
									ymap.getIndex(yPixel)) + 1));
					}
				} else {

					debug("Smooth zoom event toward a contiguous selection",12);
					//Zoom to sub-selection
					smoothAnimatedZoomTowardTarget(
						arraySelection.getMinContiguousIndex(
							xmap.getIndex(xPixel)),
	
						(arraySelection.getMaxContiguousIndex(
							xmap.getIndex(xPixel)) -
							arraySelection.getMinContiguousIndex(
								xmap.getIndex(xPixel)) + 1),
	
						geneSelection.getMinContiguousIndex(ymap.getIndex(yPixel)),
	
						(geneSelection.getMaxContiguousIndex(ymap.getIndex(yPixel)) -
						geneSelection.getMinContiguousIndex(ymap.getIndex(yPixel)) +
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
						(geneSelection.getMinIndex() !=
							ymap.getFirstVisible() ||
							arraySelection.getMinIndex() !=
							xmap.getFirstVisible())))) {

				if(stepwiseZoom) {

					if(zoomSpeed == 2) {

						debug("Hard zoom event to a disjoint selection",12);
						hardZoomToTarget(
							arraySelection.getMinIndex(),
							(arraySelection.getMaxIndex() -
								arraySelection.getMinIndex() + 1),
							geneSelection.getMinIndex(),
							(geneSelection.getMaxIndex() -
								geneSelection.getMinIndex() + 1));
					} else {

						debug("Smooth zoom event to a disjoint selection",12);
						//Zoom to selection
						smoothAnimatedZoomToTarget(
							arraySelection.getMinIndex(),
							(arraySelection.getMaxIndex() -
								arraySelection.getMinIndex() + 1),
							geneSelection.getMinIndex(),
							(geneSelection.getMaxIndex() -
								geneSelection.getMinIndex() + 1));
					}
				} else {

					debug("Smooth zoom event toward a disjoint selection",12);
					//Zoom toward selection
					smoothAnimatedZoomTowardTarget(
						arraySelection.getMinIndex(),
						(arraySelection.getMaxIndex() -
							arraySelection.getMinIndex() + 1),
						geneSelection.getMinIndex(),
						(geneSelection.getMaxIndex() -
							geneSelection.getMinIndex() + 1),
						zoomDegree);
				}
			}
			//Else full zoom in
			else {

				if(stepwiseZoom) {

					if(zoomSpeed == 2) {

						debug("Hard zoom event to a tile",12);
						hardZoomToTarget(xmap.getIndex(xPixel),1,
							ymap.getIndex(yPixel),1);
					} else {

						debug("Smooth zoom event to a tile",12);
						smoothAnimatedZoomToTarget(xmap.getIndex(xPixel),1,
							ymap.getIndex(yPixel),1);
					}
				} else {

					debug("Smooth zoom event toward a tile",12);
					smoothAnimatedZoomTowardPixel(xPixel,yPixel,zoomDegree);
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
						hardZoomToTarget(
							arraySelection.getMinContiguousIndex(
								xmap.getIndex(xPixel)),
							(arraySelection.getMaxContiguousIndex(
								xmap.getIndex(xPixel)) -
								arraySelection.getMinContiguousIndex(
									xmap.getIndex(xPixel)) + 1),
							geneSelection.getMinContiguousIndex(
								ymap.getIndex(yPixel)),
							(geneSelection.getMaxContiguousIndex(
								ymap.getIndex(yPixel)) -
								geneSelection.getMinContiguousIndex(
									ymap.getIndex(yPixel)) + 1));
					} else {

						//Zoom to sub-selection
						smoothAnimatedZoomToTarget(
							arraySelection.getMinContiguousIndex(
								xmap.getIndex(xPixel)),
							(arraySelection.getMaxContiguousIndex(
								xmap.getIndex(xPixel)) -
								arraySelection.getMinContiguousIndex(
									xmap.getIndex(xPixel)) + 1),
							geneSelection.getMinContiguousIndex(
								ymap.getIndex(yPixel)),
							(geneSelection.getMaxContiguousIndex(
								ymap.getIndex(yPixel)) -
								geneSelection.getMinContiguousIndex(
									ymap.getIndex(yPixel)) + 1));
					}
				}
				//If click was inside a selection or between selections and the
				//selection area is larger than the current zoom level
				else if(pixelIsAmidstSelection(xPixel,yPixel) &&
					(selectionIsLargerThanVisible())) {

					if(zoomSpeed == 2) {

						//Zoom to selection
						hardZoomToTarget(
							arraySelection.getMinIndex(),
							(arraySelection.getMaxIndex() -
								arraySelection.getMinIndex() + 1),
							geneSelection.getMinIndex(),
							(geneSelection.getMaxIndex() -
								geneSelection.getMinIndex() + 1));
					} else {

						//Zoom to selection
						smoothAnimatedZoomToTarget(
							arraySelection.getMinIndex(),
							(arraySelection.getMaxIndex() -
								arraySelection.getMinIndex() + 1),
							geneSelection.getMinIndex(),
							(geneSelection.getMaxIndex() -
								geneSelection.getMinIndex() + 1));
					}
				}
				//Else full zoom out
				else {

					if(zoomSpeed == 2) {

						hardZoomToTarget(xmap.getMinIndex(),xmap.getMaxIndex() +
							1,ymap.getMinIndex(),ymap.getMaxIndex() + 1);
					} else {

						smoothAnimatedZoomOut();
					}
				}
			} else {

				//All incomplete zoom-outs will perform a precise zoom-out from
				//the pixel clicked, partly because I haven't implemented a
				//zoom-away method which takes selection boundaries, but also to
				//provide more discrete control of where precisely to be zoomed
				smoothAnimatedZoomFromPixel(xPixel,yPixel,zoomDegree);
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
			geneSelection != null && arraySelection != null &&
			//Click is amidst a selected area
			geneSelection.getMinIndex()  <= ymap.getIndex(yPixel) &&
			geneSelection.getMaxIndex()  >= ymap.getIndex(yPixel) &&
			arraySelection.getMinIndex() <= xmap.getIndex(xPixel) &&
			arraySelection.getMaxIndex() >= xmap.getIndex(xPixel));
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
			geneSelection != null && arraySelection != null &&
			//Click is amidst a selected area
			geneSelection.isIndexSelected(ymap.getIndex(yPixel)) &&
			arraySelection.isIndexSelected(xmap.getIndex(xPixel)));
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
			geneSelection != null && arraySelection != null &&
			(geneSelection.getMaxIndex() - geneSelection.getMinIndex() + 1) <
			ymap.getNumVisible() ||
			(arraySelection.getMaxIndex() - arraySelection.getMinIndex() + 1) <
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
			geneSelection != null && arraySelection != null &&
			(geneSelection.getMaxContiguousIndex(yIndex) -
			geneSelection.getMinContiguousIndex(yIndex) + 1) <
			ymap.getNumVisible() ||
			(arraySelection.getMaxContiguousIndex(xIndex) -
			arraySelection.getMinContiguousIndex(xIndex) + 1) <
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
			geneSelection != null && arraySelection != null &&
			(geneSelection.getMaxIndex() -
			geneSelection.getMinIndex() + 1) >
			ymap.getNumVisible() ||
			(arraySelection.getMaxIndex() -
			arraySelection.getMinIndex() + 1) >
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
			geneSelection != null && arraySelection != null &&
			(geneSelection.getMaxContiguousIndex(yIndex) -
			geneSelection.getMinContiguousIndex(yIndex) + 1) >
			ymap.getNumVisible() ||
			(arraySelection.getMaxContiguousIndex(xIndex) -
			arraySelection.getMinContiguousIndex(xIndex) + 1) >
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
			geneSelection != null && arraySelection != null &&
			(geneSelection.getMaxIndex() - geneSelection.getMinIndex() + 1) ==
			ymap.getNumVisible() &&
			(arraySelection.getMaxIndex() - arraySelection.getMinIndex() + 1) ==
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
			geneSelection != null && arraySelection != null &&
			(geneSelection.getMaxContiguousIndex(yIndex) -
			geneSelection.getMinContiguousIndex(yIndex) + 1) ==
			ymap.getNumVisible() &&
			(arraySelection.getMaxContiguousIndex(xIndex) -
			arraySelection.getMinContiguousIndex(xIndex) + 1) ==
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

		processLeftDoubleClickZoomEvent(xPixel,yPixel,true,true,2);
	}

	/**
	 * Processes a completed left double option control click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDoubleOptionControlClick(int xPixel,int yPixel) {

		processLeftDoubleClickZoomEvent(xPixel,yPixel,false,false,0);
	}

	/**
	 * Processes a completed left double option click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDoubleOptionClick(int xPixel,int yPixel) {

		processLeftDoubleClickZoomEvent(xPixel,yPixel,false,false,1);
	}

	/**
	 * Processes a completed left double option shift click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDoubleOptionShiftClick(int xPixel,int yPixel) {

		processLeftDoubleClickZoomEvent(xPixel,yPixel,false,false,2);
	}

	/**
	 * Processes a completed left double option command click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDoubleOptionCommandClick(int xPixel,int yPixel) {

		processLeftDoubleClickZoomEvent(xPixel,yPixel,false,true,1);
	}

	/**
	 * Processes a completed left double option shift command click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDoubleOptionShiftCommandClick(int xPixel,
		int yPixel) {

		processLeftDoubleClickZoomEvent(xPixel,yPixel,false,true,2);
	}

	/**
	 * Processes left click drag start event
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDragStart(int xPixel,int yPixel) {

		//If this is a correction of the starting point, clear out the previous
		//dragRect
		if(xmap.isSelecting()) {
			debug("processLeftDragStart: Correcting drag start",10);
			endDragRect(endPoint.x,endPoint.y);
			drawBand(dragRect);
		}
		xmap.setSelecting(true);
		ymap.setSelecting(true);
		xmap.setSelectingStart(xmap.getIndex(xPixel));
		ymap.setSelectingStart(ymap.getIndex(yPixel));

		startPoint.setLocation(xmap.getIndex(xPixel),ymap.getIndex(yPixel));
		endPoint.setLocation(startPoint.x, startPoint.y);

		dragRect.setLocation(startPoint.x,startPoint.y);
		dragRect.setSize(endPoint.x - dragRect.x,endPoint.y - dragRect.y);

		drawBand(dragRect);
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
	public void processTemporaryLeftDragStart(int xPixelStart,int yPixelStart,
		int xPixel,int yPixel) {

		startPoint.setLocation(xmap.getIndex(xPixelStart),
			ymap.getIndex(yPixelStart));
		endPoint.setLocation(xmap.getIndex(xPixel),
			ymap.getIndex(yPixel));

		dragRect.setLocation(startPoint.x,startPoint.y);
		dragRect.setSize(endPoint.x - dragRect.x,endPoint.y - dragRect.y);

		drawBand(dragRect);
		repaint();
	}

	/**
	 * Processes left click drag end event
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDragEnd(int xPixel,int yPixel) {

		xmap.setSelecting(false);
		ymap.setSelecting(false);
		xmap.setSelectingStart(-1);
		ymap.setSelectingStart(-1);
		debug("Mouse dragged. Updating hover indexes to [" +
		      xmap.getIndex(xPixel) + "x" + ymap.getIndex(yPixel) +
		      "]",4);
		xmap.setHoverIndex(xmap.getIndex(xPixel));
		ymap.setHoverIndex(ymap.getIndex(yPixel));

		drawBand(dragRect);

		endPoint.setLocation(xmap.getIndex(xPixel),
				ymap.getIndex(yPixel));

		dragRect.setLocation(startPoint.x,startPoint.y);
		dragRect.setSize(0,0);
		dragRect.add(endPoint.x,endPoint.y);

		drawBand(dragRect);

		debug("Selecting startpoint: [" + startPoint.x + "," + startPoint.y +
			"] to endPoint: [" + endPoint.x + "," + endPoint.y + "].",10);
		selectRectangle(startPoint,endPoint);

		repaint();
	}

	/**
	 * Processes left shift click drag end event
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftShiftDragEnd(int xPixel,int yPixel) {

		xmap.setSelecting(false);
		ymap.setSelecting(false);
		xmap.setSelectingStart(-1);
		ymap.setSelectingStart(-1);
		debug("Mouse dragged. Updating hover indexes to [" +
		      xmap.getIndex(xPixel) + "x" + ymap.getIndex(yPixel) +
		      "]",4);
		xmap.setHoverIndex(xmap.getIndex(xPixel));
		ymap.setHoverIndex(ymap.getIndex(yPixel));

		drawBand(dragRect);

		endPoint.setLocation(xmap.getIndex(xPixel),
				ymap.getIndex(yPixel));

		dragRect.setLocation(xmap.getMinIndex(),startPoint.y);
		dragRect.setSize(0, 0);
		dragRect.add(xmap.getMaxIndex(),endPoint.y);

		drawBand(dragRect);

		final Point start = new Point(xmap.getMinIndex(),startPoint.y);
		final Point end   = new Point(xmap.getMaxIndex(),endPoint.y);

		selectRectangle(start, end);

		repaint();
	}

	/**
	 * Processes left control click drag end event
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftControlDragEnd(int xPixel,int yPixel) {
		xmap.setSelecting(false);
		ymap.setSelecting(false);
		xmap.setSelectingStart(-1);
		ymap.setSelectingStart(-1);
		debug("Mouse dragged. Updating hover indexes to [" +
		      xmap.getIndex(xPixel) + "x" + ymap.getIndex(yPixel) +
		      "]",4);
		xmap.setHoverIndex(xmap.getIndex(xPixel));
		ymap.setHoverIndex(ymap.getIndex(yPixel));

		drawBand(dragRect);

		endPoint.setLocation(xmap.getIndex(xPixel),
				ymap.getIndex(yPixel));

		dragRect.setLocation(startPoint.x, ymap.getMinIndex());
		dragRect.setSize(0, 0);
		dragRect.add(endPoint.x, ymap.getMaxIndex());

		drawBand(dragRect);

		final Point start = new Point(startPoint.x,ymap.getMinIndex());
		final Point end   = new Point(endPoint.x,  ymap.getMaxIndex());

		selectRectangle(start, end);

		repaint();
	}

	/**
	 * Processes left click drag movement event
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftDragDuring(int xPixelStart,int yPixelStart,
		int xPixel,int yPixel) {

		//If the timer hasn't done this yet, we now have confirmation this is a
		//left-click drag event
		if(!xmap.isSelecting()) {
			processTemporaryLeftDragStart(xPixelStart,yPixelStart,xPixel,
				yPixel);
		}

		debug("Mouse dragged. Updating hover indexes to [" +
		      xmap.getIndex(xPixel) + "x" + ymap.getIndex(yPixel) +
		      "]",4);
		xmap.setHoverIndex(xmap.getIndex(xPixel));
		ymap.setHoverIndex(ymap.getIndex(yPixel));

		drawBand(dragRect);

		endPoint.setLocation(xmap.getIndex(xPixel),
				ymap.getIndex(yPixel));

		/* Normal selection */
		dragRect.setLocation(startPoint.x,startPoint.y);
		dragRect.setSize(0,0);
		dragRect.add(endPoint.x,endPoint.y);

		drawBand(dragRect);
	}

	/**
	 * Processes left shift click drag movement event
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftShiftDragDuring(int xPixelStart,int yPixelStart,
		int xPixel,int yPixel) {

		//If the timer hasn't done this yet, we now have confirmation this is a
		//left-click drag event
		if(!xmap.isSelecting()) {
			processTemporaryLeftDragStart(xPixelStart,yPixelStart,xPixel,
				yPixel);
		}

		debug("Mouse dragged. Updating hover indexes to [" +
		      xmap.getIndex(xPixel) + "x" + ymap.getIndex(yPixel) +
		      "]",4);
		xmap.setHoverIndex(xmap.getIndex(xPixel));
		ymap.setHoverIndex(ymap.getIndex(yPixel));

		drawBand(dragRect);

		endPoint.setLocation(xmap.getIndex(xPixel),
				ymap.getIndex(yPixel));

		/* Full gene selection */
		dragRect.setLocation(xmap.getMinIndex(),startPoint.y);
		dragRect.setSize(0,0);
		dragRect.add(xmap.getMaxIndex(),endPoint.y);

		drawBand(dragRect);
	}

	/**
	 * Processes left control click drag movement event
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processLeftControlDragDuring(int xPixelStart,int yPixelStart,
		int xPixel,int yPixel) {

		//If the timer hasn't done this yet, we now have confirmation this is a
		//left-click drag event
		if(!xmap.isSelecting()) {
			processTemporaryLeftDragStart(xPixelStart,yPixelStart,xPixel,
				yPixel);
		}

		debug("Mouse dragged. Updating hover indexes to [" +
		      xmap.getIndex(xPixel) + "x" + ymap.getIndex(yPixel) +
		      "]",4);
		xmap.setHoverIndex(xmap.getIndex(xPixel));
		ymap.setHoverIndex(ymap.getIndex(yPixel));

		drawBand(dragRect);

		endPoint.setLocation(xmap.getIndex(xPixel),
				ymap.getIndex(yPixel));

		/* Full array selection */
		dragRect.setLocation(startPoint.x, ymap.getMinIndex());
		dragRect.setSize(0, 0);
		dragRect.add(endPoint.x, ymap.getMaxIndex());

		drawBand(dragRect);
	}

	/**
	 * Processes a completed right single click
	 * @author rleach
	 * @param xPixel
	 * @param yPixel
	 */
	public void processRightSingleClick(int xPixel,int yPixel) {
		geneSelection.setSelectedNode(null);
		geneSelection.deselectAllIndexes();

		arraySelection.setSelectedNode(null);
		arraySelection.deselectAllIndexes();

		debug("Deselecting.",1);

		geneSelection.notifyObservers();
		arraySelection.notifyObservers();
	}

	public void startDragRect(int xIndex,int yIndex) {
		startPoint.setLocation(xIndex,yIndex);
		endPoint.setLocation(startPoint.x,startPoint.y);
		dragRect.setLocation(startPoint.x,startPoint.y);
		dragRect.setSize(endPoint.x - dragRect.x,endPoint.y - dragRect.y);
		drawBand(dragRect);
	}

	public void updateDragRect(int xIndex,int yIndex) {
		drawBand(dragRect);
		endPoint.setLocation(xIndex,yIndex);
		dragRect.setLocation(startPoint.x,startPoint.y);
		dragRect.setSize(0,0);
		dragRect.add(endPoint.x,endPoint.y);
	}

	public void endDragRect(int xIndex,int yIndex) {
		updateDragRect(xIndex,yIndex);
		drawBand(dragRect);
	}

	/**
	 * Zooming when the mouse wheel is used in conjunction with the alt/option
	 * key.  Vertical scrolling if the shift key is not pressed.
	 */
	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {

		final int notches = e.getWheelRotation();
		final int shift = (notches < 0) ? -3 : 3;

		//On macs' magic mouse, horizontal scroll comes in as if the shift was
		//down
		if(e.isAltDown()) {
			if (notches < 0) {
				//This ensures we only zoom toward the cursor when the cursor is
				//over the map
				if (status != null) {
					smoothZoomTowardPixel(e.getX(),e.getY());
				}
				//This should happen when the mouse is not over the heatmap
				else {
					xmap.zoomInBegin();
					ymap.zoomInBegin();
				}
			} else {
				if (status != null) {
					smoothZoomFromPixel(e.getX(),e.getY());
				} else {
					xmap.zoomOutBegin();
					ymap.zoomOutBegin();
				}
			}
		} else if(e.isShiftDown()) {
			xmap.scrollBy(shift,false);
			//Now we are hovered over a new index
			xmap.setHoverIndex(xmap.getIndex(e.getX()));
		} else {
			ymap.scrollBy(shift,false);
			//Now we are hovered over a new index
			ymap.setHoverIndex(ymap.getIndex(e.getY()));
		}

		repaint();
	}

	/**
	 * This function, given a pixel location (e.g. where the cursor is), will
	 * intelligently choose a zoom amount to give the appearance of a smooth
	 * zoom that expands from the given pixel
	 * 
	 * @param xPxPos
	 * @param yPxPos
	 */
	public void smoothZoomTowardPixel(int xPxPos,int yPxPos) {
		smoothZoomTowardPixel(xPxPos,yPxPos,xmap.getZoomIncrement());
	}

	/**
	 * This function, given a pixel location (e.g. where the cursor is), will
	 * intelligently choose a zoom amount to given the appearance of a smooth
	 * zoom that expands from the given pixel
	 * 
	 * @param xPxPos
	 * @param yPxPos
	 */
	public void smoothZoomTowardPixelFast(int xPxPos,int yPxPos) {
		smoothZoomTowardPixel(xPxPos,yPxPos,xmap.getZoomIncrementFast());
	}

	/**
	 * This method zooms the zoom increment given toward the supplied pixel
	 * coordinates in an animated fashion. Basically, it issues repeated calls
	 * to smoothZoomTowardPixel until the target zoom increment is reached.
	 * Functions similar to smoothAnimatedZoomToTarget.
	 * @author rleach
	 * @return none
	 * @param xPxPos
	 * @param yPxPos
	 * @param zoomInc
	 */
	public void smoothAnimatedZoomTowardPixel(int xPxPos,int yPxPos,
		double targetZoomInc) {

		//If there's nothing to zoom, just return
		if(xmap.getNumVisible() == 1 && ymap.getNumVisible() == 1) {
			return;
		}

		long startTime = System.currentTimeMillis();

		//Zooming out is slower on large matrices because it just takes longer
		//to draw large amounts of cells, so our cutoff wait time should be
		//smaller if we are zooming out
		int doneWaitingMillis = 500;
		double zoomInc = xmap.getZoomIncrement();
		double zoomTotal = 0.0;

		if(zoomInc > targetZoomInc) {
			smoothZoomTowardPixel(xPxPos,yPxPos,targetZoomInc);
			return;
		}

		//Let's calculate the relative position of the center of the selection
		//and gradually zoom toward that spot in a loop
		while((zoomInc + zoomTotal) < targetZoomInc &&
			(xmap.getNumVisible() > 1 || ymap.getNumVisible() > 1)) {

			//If drawing the zoom increment levels is taking too long, snap out
			//of it.
			if((System.currentTimeMillis() - startTime) > doneWaitingMillis)
				break;

			smoothZoomTowardPixel(xPxPos,yPxPos,zoomInc);
			zoomTotal += zoomInc;

			//Force an immediate repaint.  Found this in a thread here:
			//https://community.oracle.com/thread/1663771
			paintImmediately(0,0,getWidth(),getHeight());

			//Sleep a few milliseconds
			try {
				// Use this to debug, by slowing the zoom down to see what's
				// happening at each step
				// Thread.sleep(500);
				Thread.sleep(10);
			} catch(InterruptedException e) {
				LogBuffer.println("Error: Couldn't sleep.");
				e.printStackTrace();
			}
		}

		//If we have not zoomed the full target fraction (within a margin of
		//accuracy) and there are still cells to zoom
		if(zoomTotal < targetZoomInc && (targetZoomInc - zoomTotal) > 0.0005 &&
			(xmap.getNumVisible() > 1 || ymap.getNumVisible() > 1)) {
			//debug("Adjusting final Y scroll");
			smoothZoomTowardPixel(xPxPos,yPxPos,zoomTotal - zoomInc);
		}

		//We will update the aspect ratio just in case it didn't happen
		//automatically
		updateAspectRatio();
	}

	/**
	 * This method zooms the zoom increment given toward the supplied pixel
	 * coordinates in an animated fashion. Basically, it issues repeated calls
	 * to smoothZoomTowardPixel until the target zoom increment is reached.
	 * Functions similar to smoothAnimatedZoomToTarget.
	 * @author rleach
	 * @return none
	 * @param xPxPos
	 * @param yPxPos
	 * @param zoomInc
	 */
	public void smoothAnimatedZoomFromPixel(int xPxPos,int yPxPos,
		double targetZoomInc) {

		//If there's nothing to zoom, just return
		if(xmap.getNumVisible() == (xmap.getMaxIndex() + 1) &&
			ymap.getNumVisible() == (ymap.getMaxIndex() + 1)) {
			return;
		}

		long startTime = System.currentTimeMillis();

		//Zooming out is slower on large matrices because it just takes longer
		//to draw large amounts of cells, so our cutoff wait time should be
		//smaller if we are zooming out
		int doneWaitingMillis = 500;
		double zoomInc = xmap.getZoomIncrement();
		double zoomTotal = 0.0;

		if(zoomInc > targetZoomInc) {
			smoothZoomFromPixel(xPxPos,yPxPos,targetZoomInc);
			return;
		}

		//Let's calculate the relative position of the center of the selection
		//and gradually zoom toward that spot in a loop
		while((zoomInc + zoomTotal) < targetZoomInc &&
			(xmap.getNumVisible() <= xmap.getMaxIndex() ||
			ymap.getNumVisible() <= ymap.getMaxIndex())) {

			//If drawing the zoom increment levels is taking too long, snap out
			//of it.
			if((System.currentTimeMillis() - startTime) > doneWaitingMillis)
				break;

			smoothZoomFromPixel(xPxPos,yPxPos,zoomInc);
			zoomTotal += zoomInc;

			//Force an immediate repaint.  Found this in a thread here:
			//https://community.oracle.com/thread/1663771
			paintImmediately(0,0,getWidth(),getHeight());

			//Sleep a few milliseconds
			try {
				// Use this to debug, by slowing the zoom down to see what's
				// happening at each step
				// Thread.sleep(500);
				Thread.sleep(10);
			} catch(InterruptedException e) {
				LogBuffer.println("Error: Couldn't sleep.");
				e.printStackTrace();
			}
		}

		//If we have not zoomed the full
		if(zoomTotal < targetZoomInc && (targetZoomInc - zoomTotal) > 0.0005 &&
			(xmap.getNumVisible() <= xmap.getMaxIndex() ||
			ymap.getNumVisible() <= ymap.getMaxIndex())) {
			//debug("Adjusting final Y scroll");
			smoothZoomFromPixel(xPxPos,yPxPos,zoomTotal - zoomInc);
		}

		//We will update the aspect ratio just in case it didn't happen
		//automatically
		updateAspectRatio();
	}

	/**
	 * This function, given a pixel location (e.g. where the cursor is) and a
	 * fraction to zoom by, will intelligently choose a zoom amount to given the
	 * appearance of a smooth zoom that expands from the given pixel
	 * 
	 * @param xPxPos
	 * @param yPxPos
	 * @param zoomInc
	 */
	public void smoothZoomTowardPixel(int xPxPos,int yPxPos,double zoomInc) {

		int zoomXVal = 0;
		int zoomYVal = 0;
		int numXCells = xmap.getNumVisible();
		int numYCells = ymap.getNumVisible();

		//If the current aspect ratio has not been set, set it
		if(aspectRatio < 0) {
			updateAspectRatio();
		}

		//This is the amount by which we incrementally zoom in.  It is hard-
		//coded here in addition to inside MapContainer because we need to
		//adjust the value based on aspect ratio
		double targetZoomFrac = zoomInc;

		//This will tell us how out of sync the aspect ratio has gotten from the
		//smooth-zooming trick and how many cells on the x axis we must add or
		//remove to correct the aspect ratio (to what it was before we started
		//zooming)
		double numXCellsShouldHave = aspectRatio * numYCells;

		//Could check for possible div by zero here, but should be impossible
		double targetZoomFracCorrection =
				Math.abs(numXCellsShouldHave - numXCells) /
				numXCells;

		//If numXCellsShouldHave is basically an integer and equals numXCells,
		//we will ensure there is a zoom on both axes (this really is only good
		//for equal numbers of visible cells on both axes -possibly unnecessary)
		if((numXCellsShouldHave % 1) == 0 &&
				((int) Math.round(numXCellsShouldHave)) == numXCells) {
			zoomXVal = xmap.getBestZoomInVal(xPxPos,targetZoomFrac);
			zoomYVal = ymap.getBestZoomInVal(yPxPos,targetZoomFrac);
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in both dimensions
				if(numYCells > 1 && numXCells > 1) {
					zoomXVal = 1;
					zoomYVal = 1;
				} else if(numYCells > 1) {
					zoomYVal = 1;
				} else if(numXCells > 1) {
					zoomXVal = 1;
				}
			}
		} else if(numXCellsShouldHave > numXCells) {
			//If the X axis should have more cells on it, so zoom in on the y
			//axis more (resulting in fewer rows) to relatively make the x axis
			//have more cells in comparison.
			//The fraction calculation is different for the Y axis.  The
			//following calculation is based on the merging of a few equations:
			//current aspectRatio           = numXCells/numYCells
			//numXCellsShouldHave           = aspectRatio*numYCells                                     //If we decide to make Y "correct", we can calculate what X should be
			//numXCellsShouldHave/numYCells = numXCells/(numYCells+_numYCellsShouldHave_) = aspectRatio //We do not know _numYCellsShouldHave_, solving for it, we have:
			//_numYCellsShouldHave_         = numXCells*numYCells/numXCellsShouldHave-numYCells         //Then, canceling out numYCells, we have:
			//_numYCellsShouldHave_         = numXCells/numXCellsShouldHave-1                           //_numYCellsShouldHave_ will be a ratio, i.e. the same as targetZoomFracCorrection
			targetZoomFracCorrection =
					Math.abs(numXCells / numXCellsShouldHave - 1);
			zoomXVal = xmap.getBestZoomInVal(xPxPos,targetZoomFrac);
			zoomYVal = ymap.getBestZoomInVal(yPxPos,
					targetZoomFrac + targetZoomFracCorrection);
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in the Y dimension
				if(numYCells > 1) {
					zoomYVal = 1;
				} else if(numXCells > 1) {
					zoomXVal = 1;
				}
			}
		} else {
			zoomXVal = xmap.getBestZoomInVal(xPxPos,
					targetZoomFrac + targetZoomFracCorrection);
			zoomYVal = ymap.getBestZoomInVal(yPxPos,targetZoomFrac);
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in the X dimension
				if(numXCells > 1) {
					zoomXVal = 1;
				} else if(numYCells > 1) {
					zoomYVal = 1;
				}
			}
		}

		if(xmap.zoomTowardPixel(xPxPos, zoomXVal) == 1)
			updateAspectRatio();
		if(ymap.zoomTowardPixel(yPxPos, zoomYVal) == 1)
			updateAspectRatio();
	}

	/**
	 * This function, given a pixel location (e.g. where the cursor is), will
	 * intelligently choose a zoom amount to given the appearance of a smooth
	 * zoom that contracts on the given pixel
	 * 
	 * @param xPxPos
	 * @param yPxPos
	 */
	public void smoothZoomFromPixel(int xPxPos,int yPxPos) {

		smoothZoomFromPixel(xPxPos,yPxPos,xmap.getZoomIncrement());
	}

	/**
	 * This function, given a pixel location (e.g. where the cursor is), will
	 * intelligently choose a zoom amount to given the appearance of a smooth
	 * zoom that contracts on the given pixel
	 * 
	 * @param xPxPos
	 * @param yPxPos
	 */
	public void smoothZoomFromPixelFast(int xPxPos,int yPxPos) {

		smoothZoomFromPixel(xPxPos,yPxPos,xmap.getZoomIncrementFast());
	}

	/**
	 * This function, given a pixel location (e.g. where the cursor is) and zoom
	 * increment, will intelligently choose a zoom amount to given the
	 * appearance of a smooth zoom that contracts on the given pixel
	 * 
	 * @param xPxPos
	 * @param yPxPos
	 * @param zoomInc
	 */
	public void smoothZoomFromPixel(int xPxPos,int yPxPos,double zoomInc) {

		int zoomXVal = 0;
		int zoomYVal = 0;
		int numXCells = xmap.getNumVisible();
		int numYCells = ymap.getNumVisible();

		//If the current aspect ratio has not been set, set it
		if(aspectRatio < 0) {
			updateAspectRatio();
		}

		//This is the amount by which we incrementally zoom out.  It is hard-
		//coded here in addition to inside MapContainer because we need to
		//adjust the value based on aspect ratio
		double targetZoomFrac = zoomInc;

		//This will tell us how out of sync the aspect ratio has gotten from the
		//smooth-zooming trick and how many cells on the x axis we must add or
		//remove to correct the aspect ratio (to what it was before we started
		//zooming)
		double numXCellsShouldHave = aspectRatio * numYCells;

		//Could check for possible div by zero here, but should be impossible
		double targetZoomFracCorrection = Math.abs(numXCellsShouldHave -
				numXCells) / numXCells;

		//If numXCellsShouldHave is basically an integer and equals numXCells,
		//we will ensure there is a zoom on both axes (this really is only good
		//for equal numbers of visible cells on both axes -possibly unnecessary)
		if((numXCellsShouldHave % 1) == 0 &&
				((int) Math.round(numXCellsShouldHave)) == numXCells) {
			zoomXVal = xmap.getBestZoomOutVal(xPxPos,targetZoomFrac);
			zoomYVal = ymap.getBestZoomOutVal(yPxPos,targetZoomFrac);
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in both dimensions, try to zoom out 2,
				//then 1 : Probably unnecessary
				if(numYCells < ymap.getMaxIndex() &&
						numXCells < xmap.getMaxIndex()) {
					zoomXVal = 2;
					zoomYVal = 2;
				} else if(numYCells < ymap.getMaxIndex()) {
					zoomYVal = 2;
				} else if(numXCells < xmap.getMaxIndex()) {
					zoomXVal = 2;
				} else if(numYCells < (ymap.getMaxIndex() + 1) &&
						numXCells < (xmap.getMaxIndex() + 1)) {
					zoomXVal = 1;
					zoomYVal = 1;
				} else if(numYCells < (ymap.getMaxIndex() + 1)) {
					zoomYVal = 1;
				} else if(numXCells < (xmap.getMaxIndex() + 1)) {
					zoomXVal = 1;
				}
			}
		} else if(numXCellsShouldHave > numXCells) {
			//If the X axis should have more cells on it, zoom out on the x axis
			//more
			zoomXVal = xmap.getBestZoomOutVal(xPxPos,
					targetZoomFrac + targetZoomFracCorrection);
			zoomYVal = ymap.getBestZoomOutVal(yPxPos,targetZoomFrac);
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom out in the X dimension
				if(numXCells < (xmap.getMaxIndex() + 1)) {
					zoomXVal = 1;
				} else if(numYCells < (ymap.getMaxIndex() + 1)) {
					zoomYVal = 1;
				}
			}
		} else {
			//Else, the X axis should have fewer cells on it, so zoom out on the
			//y axis more.
			//The fraction calculation is different for the Y axis.  The
			//following calculation is based on the merging of a few equations:
			//current aspectRatio           = numXCells/numYCells
			//numXCellsShouldHave           = aspectRatio*numYCells                                     //If we decide to make Y "correct", we can calculate what X should be
			//numXCellsShouldHave/numYCells = numXCells/(numYCells+_numYCellsShouldHave_) = aspectRatio //We do not know _numYCellsShouldHave_, solving for it, we have:
			//_numYCellsShouldHave_         = numXCells*numYCells/numXCellsShouldHave-numYCells         //Then, canceling out numYCells, we have:
			//_numYCellsShouldHave_         = numXCells/numXCellsShouldHave-1                           //_numYCellsShouldHave_ will be a ratio, i.e. the same as targetZoomFracCorrection
			targetZoomFracCorrection =
					Math.abs(numXCells / numXCellsShouldHave - 1);
			zoomXVal = xmap.getBestZoomOutVal(xPxPos,targetZoomFrac);
			zoomYVal = ymap.getBestZoomOutVal(yPxPos,
					targetZoomFrac + targetZoomFracCorrection);
			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom out in the Y dimension
				if(numYCells < (ymap.getMaxIndex() + 1)) {
					zoomYVal = 1;
				} else if(numXCells < (xmap.getMaxIndex() + 1)) {
					zoomXVal = 1;
				}
			}
		}

		if(xmap.zoomAwayPixel(xPxPos, zoomXVal) == 1)
			updateAspectRatio();
		if(ymap.zoomAwayPixel(yPxPos, zoomYVal) == 1)
			updateAspectRatio();
	}

	/**
	 * This is a wrapper function for smoothZoomTowardSelection which allows one
	 * to call this function without providing any parameters. So it just fills
	 * in the target zoom level info.
	 */
	public void smoothIncrementalZoomOut() {

		//The function below has a bug that prevents full zoom out
		// - this works around that - though it may have already been fixed
		int prevXNumVisible = xmap.getNumVisible();
		int prevYNumVisible = ymap.getNumVisible();

		/*
		 * TODO: Fix the smoothZoomTowardSelection function below and then
		 * remove the work-around code - Rob
		 */
		smoothZoomTowardSelection(0,(xmap.getMaxIndex() + 1),
				0,(ymap.getMaxIndex() + 1));

		//The function above has a bug that prevents full zoom out sometimes
		// - this works around that, though it may have been fixed
		if(prevXNumVisible == xmap.getNumVisible() &&
		   prevYNumVisible == ymap.getNumVisible()) {

			//If you've never seen this message and you use the home button a
			//lot, you can probably remove this if-conditional and the prev*
			//variables
			//debug("smoothZoomTowardSelection bug still exists." +
			//					"  Working around it.");

			xmap.zoomOutCenter("fast");
			ymap.zoomOutCenter("fast");
		}

		//If we've reached full zoom-out, reset the aspect ratio
		if(xmap.getNumVisible() == (xmap.getMaxIndex() + 1) &&
				ymap.getNumVisible() == (ymap.getMaxIndex() + 1)) {
			setAspectRatio(xmap.getMaxIndex() + 1,ymap.getMaxIndex() + 1);
		}
	}

	/**
	 * This is just a wrapper for smoothAnimatedZoomToTarget which fills
	 * in the dimensions of the matrix
	 */
	public void smoothAnimatedZoomOut() {

		smoothAnimatedZoomToTarget(0,(xmap.getMaxIndex() + 1),
			0,(ymap.getMaxIndex() + 1));
	}

	/**
	 * This is a wrapper for smoothZoomTowardSelection which calls it repeatedly
	 * until a target location is reached to animate a full zoom to target
	 * 
	 * @param selecXStartIndex
	 * @param numXSelectedIndexes
	 * @param selecYStartIndex
	 * @param numYSelectedIndexes
	 */
	public void smoothAnimatedZoomToTarget(int selecXStartIndex,
		int numXSelectedIndexes,int selecYStartIndex,int numYSelectedIndexes) {

		long startTime = System.currentTimeMillis();

		//Zooming out is slower on large matrices because it just takes longer
		//to draw large amounts of cells, so our cutoff wait time should be
		//smaller if we are zooming out
		int doneWaitingMillis = 500;

		//Let's calculate the relative position of the center of the selection
		//and gradually zoom toward that spot in a loop
		while(xmap.getNumVisible() != numXSelectedIndexes ||
			  ymap.getNumVisible() != numYSelectedIndexes) {

			//If drawing the zoom increment levels is taking too long, snap out
			//of it.
			if((System.currentTimeMillis() - startTime) > doneWaitingMillis)
				break;

			smoothZoomTowardSelection(selecXStartIndex,
									  numXSelectedIndexes,
									  selecYStartIndex,
									  numYSelectedIndexes);

			//debug("numVisible versus num selected: [x" +
			//				  xmap.getNumVisible() + ":" + numXSelectedIndexes +
			//				  ",y" + ymap.getNumVisible() + ":" +
			//				  numYSelectedIndexes + "].");

			//Force an immediate repaint.  Found this in a thread here:
			//https://community.oracle.com/thread/1663771
			paintImmediately(0,0,getWidth(),getHeight());

			//Sleep a few milliseconds
			try {
				// Use this to debug, by slowing the zoom down to see what's
				// happening at each step
				// Thread.sleep(500);
				Thread.sleep(10);
			} catch(InterruptedException e) {
				LogBuffer.println("Error: Couldn't sleep.");
				e.printStackTrace();
			}
		}

		//Once the loop is done, we may not have gotten to the correct zoom
		//level or scroll position, so we should do a zoom & scroll here just to
		//be certain. Scroll could be off because of the way it is separated
		//from the zoom, so we will always do that
		hardZoomToTarget(selecXStartIndex,numXSelectedIndexes,selecYStartIndex,
			numYSelectedIndexes);

		//We will update the aspect ratio just in case it didn't happen
		//automatically
		updateAspectRatio();
	}

	/**
	 * This method is basically the same as smoothAnimatedZoomToTarget, except
	 * that it only zooms the amount indicated by zoomInc
	 * @author rleach
	 * @return none
	 * @param selecXStartIndex
	 * @param numXSelectedIndexes
	 * @param selecYStartIndex
	 * @param numYSelectedIndexes
	 * @param targetZoomInc
	 */
	public void smoothAnimatedZoomTowardTarget(int selecXStartIndex,
		int numXSelectedIndexes,int selecYStartIndex,int numYSelectedIndexes,
		double targetZoomInc) {

		double zoomInc = xmap.getZoomIncrement();
		int startVisX = xmap.getNumVisible();
		int startVisY = ymap.getNumVisible();
		int targetDiffX =
			(int) Math.round((double) xmap.getNumVisible() * targetZoomInc -
				(double) xmap.getNumVisible() * zoomInc);
		int targetDiffY =
			(int) Math.round((double) ymap.getNumVisible() * targetZoomInc -
				(double) ymap.getNumVisible() * zoomInc);

		if(targetZoomInc >= zoomInc &&
			targetDiffX > Math.abs(xmap.getNumVisible() - startVisX) &&
			targetDiffY > Math.abs(ymap.getNumVisible() - startVisY)) {

			long startTime = System.currentTimeMillis();
		
			//Zooming out is slower on large matrices because it just takes
			//longer to draw large amounts of cells, so our cutoff wait time
			//should be smaller if we are zooming out
			int doneWaitingMillis = 500;
		
			//Let's calculate the relative position of the center of the
			//selection and gradually zoom toward that spot in a loop
			while((xmap.getNumVisible() != numXSelectedIndexes ||
				ymap.getNumVisible() != numYSelectedIndexes) &&
				targetDiffX > Math.abs(xmap.getNumVisible() - startVisX) &&
				targetDiffY > Math.abs(ymap.getNumVisible() - startVisY)) {
		
				//If drawing the zoom increment levels is taking too long, snap
				//out of it.
				if((System.currentTimeMillis() - startTime) > doneWaitingMillis)
					break;

				//This zooms by zoomInc
				//We will assume a relatively small amount of zoom and forego
				//logMode
				smoothZoomTowardSelection(selecXStartIndex,numXSelectedIndexes,
					selecYStartIndex,numYSelectedIndexes,false,-1.0);

				debug("Zoomed by [" + targetDiffX + "," + targetDiffY +
					"] thus far",11);
		
				//Force an immediate repaint.  Found this in a thread here:
				//https://community.oracle.com/thread/1663771
				paintImmediately(0,0,getWidth(),getHeight());
		
				//Sleep a few milliseconds
				try {
					// Use this to debug, by slowing the zoom down to see what's
					// happening at each step
					// Thread.sleep(500);
					Thread.sleep(10);
				} catch(InterruptedException e) {
					LogBuffer.println("Error: Couldn't sleep.");
					e.printStackTrace();
				}
			}
		} else if(targetZoomInc > 0.0) {
			smoothZoomTowardSelection(selecXStartIndex,numXSelectedIndexes,
				selecYStartIndex,numYSelectedIndexes,false,targetZoomInc);
		}

		//If the target is less than zoomInc away in both dimensions, hard-zoom
		//to finish it off
		if(//We're not at the target
			(xmap.getNumVisible() != numXSelectedIndexes ||
			ymap.getNumVisible() != numYSelectedIndexes) &&
			//The matrix is too small (in a dimension that's not fully zoomed)
			//for the logic following to work
			(1.0 / (double) (xmap.getMaxIndex() + 1) > zoomInc &&
				xmap.getNumVisible() != numXSelectedIndexes) ||
			(1.0 / (double) (ymap.getMaxIndex() + 1) > zoomInc &&
				ymap.getNumVisible() != numYSelectedIndexes) ||
			//X Size difference is less than the zoom increment (times two
			//because this is for the outer edge)
			(double) Math.abs(xmap.getNumVisible() - numXSelectedIndexes) * 2 /
			(double) (xmap.getMaxIndex() + 1) < zoomInc &&
			//X Position difference is less than the zoom increment
			(double) Math.abs(xmap.getFirstVisible() - selecXStartIndex) /
			(double) (xmap.getMaxIndex() + 1) < zoomInc &&
			//Y Size difference is less than the zoom increment (times two
			//because this is for the outer edge)
			(double) Math.abs(ymap.getNumVisible() - numYSelectedIndexes) * 2 /
			(double) (ymap.getMaxIndex() + 1) < zoomInc &&
			//Y Position difference is less than the zoom increment
			(double) Math.abs(ymap.getFirstVisible() - selecYStartIndex) /
			(double) (ymap.getMaxIndex() + 1) < zoomInc) {

			debug("Hard zooming to target to finish off the zoom step.",11);

			hardZoomToTarget(selecXStartIndex,numXSelectedIndexes,
				selecYStartIndex,numYSelectedIndexes);
		}

		//We will update the aspect ratio just in case it didn't happen
		//automatically
		updateAspectRatio();
	}

	/**
	 * This method simply groups the xmap and ymap calls necessary for a single
	 * zoom event
	 * @author rleach
	 * @param selecXStartIndex
	 * @param numXSelectedIndexes
	 * @param selecYStartIndex
	 * @param numYSelectedIndexes
	 */
	public void hardZoomToTarget(int selecXStartIndex,
		int numXSelectedIndexes,int selecYStartIndex,int numYSelectedIndexes) {

		if(xmap.getNumVisible() != numXSelectedIndexes) {
			xmap.zoomToSelected(selecXStartIndex,
					(selecXStartIndex + numXSelectedIndexes - 1));
		}
		if(ymap.getNumVisible() != numYSelectedIndexes) {
			ymap.zoomToSelected(selecYStartIndex,
					(selecYStartIndex + numYSelectedIndexes - 1));
		}
		if(xmap.getFirstVisible() != selecXStartIndex) {
			xmap.scrollToFirstIndex(selecXStartIndex);
		}
		if(ymap.getFirstVisible() != selecYStartIndex) {
			ymap.scrollToFirstIndex(selecYStartIndex);
		}
	}

	/**
	 * This method is a wrapper for the modified smoothZoomTowardSelection
	 * which now takes a boolean indicating log zoom mode, provided for
	 * simplicity and backwards compatibility
	 * @author rleach
	 * @param selecXStartIndex
	 * @param numXSelectedIndexes
	 * @param selecYStartIndex
	 * @param numYSelectedIndexes
	 * @return
	 */
	public int[] smoothZoomTowardSelection(int selecXStartIndex,
										   int numXSelectedIndexes,
										   int selecYStartIndex,
										   int numYSelectedIndexes) {

		return(smoothZoomTowardSelection(selecXStartIndex,numXSelectedIndexes,
			selecYStartIndex,numYSelectedIndexes,true,-1.0));
	}

	/*
	 * TODO: Simplify/streamline this function, move a bunch of steps to
	 * independent functions in MapContainer, and make it able to handle pixel
	 * positions that are out of view (xPxPos and yPxPos) - Rob
	 */
	/*
	 * TODO: There appears to be a minor bug when zooming out.  The final zoom
	 * steps tends to be very very small, perhaps a single row/col.  That can't
	 * be coincidence. - Rob
	 */
	/**
	 * Essentially, all this function does is call getZoomTowardPixelOfSelection
	 * to determine the pixel in the selection to zoom toward (so that the
	 * selection ends up filling the view) and then uses that pixel to call
	 * zoomTowardPixel.
	 * @author rleach
	 * @param selecXStartIndex
	 * @param numXSelectedIndexes
	 * @param selecYStartIndex
	 * @param numYSelectedIndexes
	 * @param logMode
	 * @param optionalTargetZoomFrac (only used if > 0.0)
	 */
	public int[] smoothZoomTowardSelection(int selecXStartIndex,
		int numXSelectedIndexes,int selecYStartIndex,int numYSelectedIndexes,
		boolean logMode,double optionalTargetZoomFrac) {

		//Find the pixel inside the selected area to "zoom toward" such that the
		//selected area essentially expands to fill the screen
		int startXPixel = xmap.getPixel(selecXStartIndex);
		double pixelsPerXIndex = xmap.getScale();
		int numSelectedXPixels = (int) Math.round(numXSelectedIndexes *
				pixelsPerXIndex);
		int xPxPos = xmap.getZoomTowardPixelOfSelection(startXPixel,
														numSelectedXPixels);
		//debug("smoothZoomTowardSelection: Starting X index " +
		//"sent in: [" + selecXStartIndex + "] and pixel index obtained for " +
		//"that data index: [" + startXPixel + "] and then pixel selected to " +
		//"zoom toward is: [" + xPxPos + "].");
		int startYPixel = ymap.getPixel(selecYStartIndex);
		double pixelsPerYIndex = ymap.getScale();
		int numSelectedYPixels = (int) Math.round(numYSelectedIndexes *
				pixelsPerYIndex);
		int yPxPos = ymap.getZoomTowardPixelOfSelection(startYPixel,
														numSelectedYPixels);
		//debug("smoothZoomTowardSelection: Starting Y index " +
		//"sent in: [" + selecYStartIndex + "] and pixel index obtained for " +
		//"that data index: [" + startYPixel + "] and then pixel selected to " +
		//"zoom toward is: [" + yPxPos + "].");

		//debug("Going to zoom toward pixel at: [x" + xPxPos +
		//		",y" + yPxPos + "] determined from data cell at: [x" +
		//		selecXStartIndex + ",y" + selecYStartIndex +
		//		"].  Selected pixel start/number: [x" + startXPixel + "/" +
		//		numSelectedXPixels + ",y" + startYPixel + "/" +
		//		numSelectedYPixels + "].");
		
		//The final aspect ratio of the selected area will be our target aspect
		//ratio (used for smoothness corrections)
		double targetAspectRatio =
				(double) numXSelectedIndexes / (double) numYSelectedIndexes;
		
		int zoomXVal          = 0;
		int zoomYVal          = 0;
		int numXCells         = xmap.getNumVisible();
		int numYCells         = ymap.getNumVisible();
		int xPxNum            = xmap.getAvailablePixels();
		int yPxNum            = ymap.getAvailablePixels();
		int prevXFirstVisible = xmap.getFirstVisible();
		int prevXNumVisible   = xmap.getNumVisible();
		int centerXIndex      = xmap.getFirstVisible() +
				(int) Math.floor(xmap.getNumVisible() / 2.0);
		int prevYFirstVisible = ymap.getFirstVisible();
		int prevYNumVisible   = ymap.getNumVisible();
		int centerYIndex      = ymap.getFirstVisible() +
				(int) Math.floor(ymap.getNumVisible() / 2.0);

		//Since it takes a lot of time to draw large matrices, select a zoom
		//fraction based on the size of the matrix we are going to draw at each
		//increment so that we skip increments that take too much time to draw
		double targetZoomFracX = (optionalTargetZoomFrac > 0.0 ?
			optionalTargetZoomFrac :
			(logMode ? xmap.getOptimalZoomIncrement(numXSelectedIndexes,
				(prevXNumVisible < numXSelectedIndexes)) :
					xmap.getZoomIncrement()));
		double targetZoomFracY = (optionalTargetZoomFrac > 0.0 ?
			optionalTargetZoomFrac :
			(logMode ? ymap.getOptimalZoomIncrement(numYSelectedIndexes,
				(prevYNumVisible < numYSelectedIndexes)) :
					ymap.getZoomIncrement()));
		//Select the larger zoom increment from the 2 dimensions
		double targetZoomFrac = targetZoomFracX;
		if(targetZoomFrac < targetZoomFracY)
			targetZoomFrac = targetZoomFracY;

		//debug("targetZoomFrac: [" + targetZoomFrac + "].");

		//If the starting aspect ratio is different from the target aspect
		//ratio, we don't want to snap to that ratio on the first step - we want
		//to get there gradually, so we'll adjust based on the
		//targetZoomFraction.  Matching that value doesn't seem to be very
		//effective, so as long as the targetZoomFraction is less than 33%,
		//we'll multiply the value by 3.  That seems to end up with the gradual
		//changing of the aspect ratio to look much more appealing and natural.
		//We might end up reaching the target aspect ratio before or after we've
		//reached the target zoom level, so there's probably a better way, but
		//this works OK.
		double aspectRatioFracCorrection = targetZoomFrac;
			//1 - targetZoomFrac;
			//(targetZoomFrac < 0.33 ? targetZoomFrac * 3.0 : targetZoomFrac);
				
		//debug("Zoom fraction: [" + targetZoomFrac +
		//		"] XtargetFrac: [" + targetZoomFracX + "] YtargetFrac: [" +
		//		targetZoomFracY + "] Aspect Ratio fraction correction: [" +
		//		aspectRatioFracCorrection + "].");

		//This will tell us how out of sync the aspect ratio has gotten from the
		//smooth-zooming trick and how many cells on the x axis we must add or
		//remove to correct the aspect ratio (to what it was before we started
		//zooming)
		double numXCellsShouldHave = targetAspectRatio * numYCells;

		//debug("numXCellsShouldHave before fractioning the " +
		//"targetAspectRatio: [" + numXCellsShouldHave + "].");

		// To cause the targetAspectRatio to be arrived at gradually, let's
		// only correct by a percentage of the correction:
		numXCellsShouldHave = numXCells + (numXCellsShouldHave - numXCells)
				* aspectRatioFracCorrection;

		//debug("numXCellsShouldHave after fractioning the " +
		//"targetAspectRatio: [" + numXCellsShouldHave + "].");

		double targetZoomFracCorrection = 0.0;

		//If numXCellsShouldHave is basically an integer and is equal to
		//numXCells, we are at the exact aspect ratio and no correction to it is
		//necessary
		if((numXCellsShouldHave % 1) == 0 &&
				((int) Math.round(numXCellsShouldHave)) == numXCells) {

			//If we're zooming in on the X axis
			if(numXCells >= numXSelectedIndexes) {
				zoomXVal = xmap.getBestZoomInVal(xPxPos,targetZoomFrac);
			} else {
				zoomXVal = xmap.getBestZoomOutVal(xPxPos,targetZoomFrac);
			}
			// If we're zooming in on the Y axis
			if (numYCells >= numYSelectedIndexes) {
				zoomYVal = ymap.getBestZoomInVal(yPxPos,targetZoomFrac);
			} else {
				zoomYVal = ymap.getBestZoomOutVal(yPxPos,targetZoomFrac);
			}

			//debug("Jumping to [" + numXCellsShouldHave +
			//"] because it's an integer equal to the target number of cells.");
			//debug("Zooming by [x" + zoomXVal + ",y" + zoomYVal +
			//"] target aspect ratio: [" + targetAspectRatio +
			//"]. Target zoom frac: [" + targetZoomFrac + "]");
			
			//If no zoom has occurred (due to selection of 0 due to smoothing -
			//a possibility that helps to allow aspect ratio changes, but is not
			//relevant here) - force a zoom to happen
			if(zoomXVal == 0 && zoomYVal == 0) {

				//If there's room to zoom 2 in both dimensions
				if(((numXSelectedIndexes <= xmap.getNumVisible() &&
					 numXCells > 2) ||
					(numXSelectedIndexes > xmap.getNumVisible() &&
					 numXCells < (xmap.getMaxIndex() + 2))) &&
					((numYSelectedIndexes <= ymap.getNumVisible() &&
					  numYCells > 2) ||
					 (numYSelectedIndexes > ymap.getNumVisible() &&
					  numYCells < (ymap.getMaxIndex() + 2)))) {
					zoomXVal = 2;
					zoomYVal = 2;
				} else if((numXSelectedIndexes <= xmap.getNumVisible() &&
						   numXCells > 1) ||
						  (numXSelectedIndexes > xmap.getNumVisible() &&
						   numXCells < (xmap.getMaxIndex() + 1))) {
					zoomXVal = 1;
				} else if((numYSelectedIndexes <= ymap.getNumVisible() &&
						   numYCells > 1) ||
						  (numYSelectedIndexes > ymap.getNumVisible() &&
						   numYCells < (ymap.getMaxIndex() + 1))) {
					zoomYVal = 1;
				}
			}
		}
		//Else if we should have more X cells and we are zooming in on the Y
		//axis OR we should have fewer X cells and we are zooming out on the Y
		//axis
		else if((numXCellsShouldHave > numXCells &&
				 numYCells > numYSelectedIndexes) ||
				(numXCellsShouldHave < numXCells &&
				 numYCells < numYSelectedIndexes)) {

			double numYCellsShouldHave = numXCells / targetAspectRatio;

			//debug("Actual number of cells that should be on " +
			//"the Y axis: [" + numYCellsShouldHave + "]");
			//if(numYCellsShouldHave < numYSelectedIndexes) {
			//	numYCellsShouldHave = numYSelectedIndexes;
			//} else if(numYCellsShouldHave > (ymap.getMaxIndex() + 1)) {
			//	numYCellsShouldHave = (ymap.getMaxIndex() + 1);
			//}

			//debug("Zooming in on Y axis more: " +
			//"numYCellsShouldHave = numYCells - Math.abs(numYCellsShouldHave" +
			//" - numYCells) * aspectRatioFracCorrection");
			//debug("Zooming in on Y axis more: " +
			//"numYCellsShouldHave = " + numYCells + " - Math.abs(" +
			//numYCellsShouldHave + " - " + numYCells + ") * " +
			//aspectRatioFracCorrection);

			//To cause the targetAspectRatio to be arrived at gradually, let's
			//only correct by a percentage of the correction:
			numYCellsShouldHave = numYCells +
					(numYCellsShouldHave -
							numYCells) * aspectRatioFracCorrection;
			
			//Could check for possible div by zero here, but should be
			//impossible
			targetZoomFracCorrection =
					Math.abs(numYCellsShouldHave -
							 numYCells) / numYCells;
			if(targetZoomFracCorrection > 1.0) {
				targetZoomFracCorrection =
						Math.abs(numYCellsShouldHave - numYCells) /
						numYCellsShouldHave;
			}
			//debug("Zooming in on Y axis more: " +
			//"targetZoomFracCorrection = Math.abs(numYCellsShouldHave - " +
			//"numYCells) / numYCells");
			//debug("Zooming in on Y axis more: " +
			//targetZoomFracCorrection + " = Math.abs(" + numYCellsShouldHave +
			//" - " + numYCells + ") / " + numYCells);
			
			//If the X axis should have more cells on it, so zoom in on the y
			//axis more (resulting in fewer rows) to relatively make the x axis
			//have more cells in comparison
			//The fraction calculation is different for the Y axis.  The
			//following calculation is based on the merging of a few equations:
			//current aspectRatio           = numXCells/numYCells
			//numXCellsShouldHave           = aspectRatio*numYCells                                     //If we decide to make Y "correct", we can calculate what X should be
			//numXCellsShouldHave/numYCells = numXCells/(numYCells+_numYCellsShouldHave_) = aspectRatio //We do not know _numYCellsShouldHave_, solving for it, we have:
			//_numYCellsShouldHave_         = numXCells*numYCells/numXCellsShouldHave-numYCells         //Then, canceling out numYCells, we have:
			//_numYCellsShouldHave_         = numXCells/numXCellsShouldHave-1                           //_numYCellsShouldHave_ will be a ratio, i.e. the same as targetZoomFracCorrection
			////////targetZoomFracCorrection = Math.abs((double) numXCells / numXCellsShouldHave - 1);
			if(numXCells >= numXSelectedIndexes) {
				zoomXVal = xmap.getBestZoomInVal(xPxPos,targetZoomFrac);
			} else {
				//debug("Zooming out on the X axis 1.");
				zoomXVal = xmap.getBestZoomOutVal(xPxPos,targetZoomFrac);
			}
			if(numYCells >= numYSelectedIndexes) {
				zoomYVal = ymap.getBestZoomInVal(yPxPos,
						targetZoomFrac +
						(1 - targetZoomFrac) * targetZoomFracCorrection);
			} else {
				//debug("Zooming out on the Y axis 1.");
				zoomYVal = ymap.getBestZoomOutVal(yPxPos,
						targetZoomFrac +
						(1 - targetZoomFrac) * targetZoomFracCorrection);
			}

			//debug("targetZoomFracCorrection: [" +
			//		targetZoomFracCorrection + "].  Resulting zoomXVal: [" +
			//		zoomXVal + "].");

			//debug("Zooming more on the Y axis because the " +
			//"number of X cells we should have [" + numXCellsShouldHave +
			//"] is greater than the current number of cells [" + numXCells +
			//"] and we are zooming in on the Y axis or the number of X " +
			//"cells we should have is fewer than the current number of " +
			//"cells, so we calculated this many Y cells we should have " +
			//"given a gradual aspect ratio change: [" + numYCellsShouldHave +
			//"].");

			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in the X dimension
				if((numXSelectedIndexes <= xmap.getNumVisible() &&
					numXCells > 1) ||
				   (numXSelectedIndexes > xmap.getNumVisible() &&
					numXCells < (xmap.getMaxIndex() + 1))) {
					zoomXVal = 1;
				} else if((numYSelectedIndexes <= ymap.getNumVisible() &&
						   numYCells > 1) ||
						(numYSelectedIndexes > ymap.getNumVisible() &&
						 numYCells < (ymap.getMaxIndex() + 1))) {
					zoomYVal = 1;
				}
			}
		} else {
			//debug("Actual number of cells that should be on " +
			//"the X axis: [" + numXCellsShouldHave + "]");
			
			//Could check for possible div by zero here, but should be
			//impossible
			targetZoomFracCorrection =
					Math.abs(numXCellsShouldHave -
							 numXCells) / numXCells;
			if(targetZoomFracCorrection > 1.0) {
				targetZoomFracCorrection =
						Math.abs(numXCellsShouldHave - numXCells) /
						numXCellsShouldHave;
			}

			//This should theoretically be more accurate, but it is not for some
			//reason:
			//targetZoomFracCorrection =
			//		numXCellsShouldHave / (double) numXCells;

			if(numXCells >= numXSelectedIndexes) {
				//debug("Zooming in on the X axis.");
				zoomXVal = xmap.getBestZoomInVal(xPxPos,
						targetZoomFrac +
						(1 - targetZoomFrac) * targetZoomFracCorrection);
			} else {
				//debug("Zooming out on the X axis 2.");
				zoomXVal = xmap.getBestZoomOutVal(xPxPos,
						targetZoomFrac +
						(1 - targetZoomFrac) * targetZoomFracCorrection);
			}
			if(numYCells >= numYSelectedIndexes) {
				//debug("Zooming in on the Y axis.");
				zoomYVal = ymap.getBestZoomInVal(yPxPos,targetZoomFrac);
			} else {
				//debug("Zooming out on the Y axis 2.");
				zoomYVal = ymap.getBestZoomOutVal(yPxPos,targetZoomFrac);
			}

			//debug("targetZoomFracCorrection: [" +
			//		targetZoomFracCorrection + "].  Resulting zoomXVal: [" +
			//		zoomXVal + "].");

			//debug("Zooming more on the X axis because the " +
			//"number of X cells we should have [" + numXCellsShouldHave +
			//"] is less than or equal to the target number of X cells [" +
			//numXCells + "].");

			//If no zoom has occurred
			if(zoomXVal == 0 && zoomYVal == 0) {
				//If there's room to zoom in the X dimension
				if((numXSelectedIndexes <= xmap.getNumVisible() &&
					numXCells > 1) ||
				   (numXSelectedIndexes > xmap.getNumVisible() &&
					numXCells < (xmap.getMaxIndex() + 1))) {
					zoomXVal = 1;
				} else if((numYSelectedIndexes <= ymap.getNumVisible() &&
						   numYCells > 1) ||
						  (numYSelectedIndexes > ymap.getNumVisible() &&
						   numYCells < (ymap.getMaxIndex() + 1))) {
					zoomYVal = 1;
				}
			}
		}
		//debug("Aspect ratio.  Current: [" + numXCells + " / " +
		//numYCells + "] Target: [" + targetAspectRatio +
		//"].  targetZoomFracCorrection: [" + targetZoomFracCorrection +
		//"].  New zoomXVal: [" + zoomXVal + "] New zoomYVal: [" + zoomYVal +
		//"] numXCellsShouldHave: [" + numXCellsShouldHave + "].");

		//Double-check that a zoom will indeed happen - a fail-safe because
		//The above code does not guarantee a zoom in rare circumstances - a
		//side-effect of the way we're smoothing the zoom
		if(zoomXVal == 0 && zoomYVal == 0) {
			if(numXSelectedIndexes != numXCells) {
				zoomXVal = 1;
			}
			if(numYSelectedIndexes != numYCells) {
				zoomYVal = 1;
			}
		}

		//Zoom the x axis
		if(numXSelectedIndexes <= numXCells) {
			//Let's make sure that we're not going to zoom past our target area
			if((numXCells - zoomXVal) < numXSelectedIndexes) {
				zoomXVal = numXCells - numXSelectedIndexes;
			}

			//debug("Zooming in on the x axis");
			xmap.zoomTowardPixel(xPxPos,zoomXVal);
		} else {
			//Let's make sure that we're not going to zoom past our target area
			if((numXCells + zoomXVal) > numXSelectedIndexes) {
				zoomXVal = Math.abs(numXCells - numXSelectedIndexes);
			}

			//debug("Zooming out on the x axis with xPxPos [" +
			//xPxPos + "] and zoomXVal [" + zoomXVal + "].");
			xmap.zoomAwayPixel(xPxPos,zoomXVal);
		}

		//Zoom the y axis
		if(numYSelectedIndexes <= numYCells) {
			//Let's make sure that we're not going to zoom past our target area
			if((numYCells - zoomYVal) < numYSelectedIndexes) {
				zoomYVal = numYCells - numYSelectedIndexes;
			}

			//debug("Zooming in on the y axis");
			ymap.zoomTowardPixel(yPxPos,zoomYVal);
		}
		//Else we were supposed to be zooming out
		else {
			//Let's make sure that we're not going to zoom past our target area
			if((numYCells + zoomYVal) > numYSelectedIndexes) {
				zoomYVal = Math.abs(numYCells - numYSelectedIndexes);
			}

			//debug("Zooming out on the y axis");
			ymap.zoomAwayPixel(yPxPos,zoomYVal);
		}

		//debug("Should have zoomed by [x" + zoomXVal + ",y" +
		//		zoomYVal + "] from [" + prevXNumVisible + ":" +
		//		prevYNumVisible + "] to [" + xmap.getNumVisible() + ":" +
		//		ymap.getNumVisible() + "].");

		//Now that we have zoomed, we should scroll if the selection is off the
		//screen.
		//If zooming has finished, scroll all the way
		if(xmap.getNumVisible() == numXSelectedIndexes &&
				ymap.getNumVisible() == numYSelectedIndexes) {
			xmap.scrollToFirstIndex(selecXStartIndex);
			ymap.scrollToFirstIndex(selecYStartIndex);
		} else {
			//debug("Correcting the scroll because (xPxPos <= 0 " +
			//"|| yPxPos <= 0 || xPxPos >= (xPxNum - 1) || yPxPos >= (yPxNum " +
			//"- 1)) = (" + xPxPos + " <= 0 || " + yPxPos + " <= 0 || " +
			//xPxPos + " >= (" + xPxNum + " - 1) || " + yPxPos + " >= (" +
			//yPxNum + " - 1)).");

			//If a selected edge (toward which we are zooming) is outside the
			//previously visible corresponding boundary AND the selected area is
			//not surrounding the visible area AND the pixel we are zooming to
			//is on an edge, assume we need to scroll incrementally to bring the
			//pixel we are zooming to/from into view
			if((((//The target start is left of the view
				  selecXStartIndex < prevXFirstVisible ||
				  //The target stop is right of the view
				  (selecXStartIndex + numXSelectedIndexes - 1) >
				  (prevXFirstVisible + prevXNumVisible - 1)) &&
				//The view is not inside the target dimensions
				!(selecXStartIndex < prevXFirstVisible &&
				  (selecXStartIndex + numXSelectedIndexes - 1) >
				  (prevXFirstVisible + prevXNumVisible - 1)))) &&
			   //The zoom-toward-pixel reported is on an edge
			   (xPxPos <= 0 || xPxPos >= (xPxNum - 1))) {

				//Scroll toward by half of the same fraction as the zoom changed
				//We don't want to scroll all the way in 1 sudden jolt, so we'll
				//scroll to be closer to the center of the selection by (half)
				//the same fraction as the incremental zoom (unless we're at the
				//zoom level of the selection)
				double scrollFrac =  ((double) xmap.getNumVisible() /
						(double) numXSelectedIndexes);
				if(scrollFrac > 1) {
					scrollFrac =  (double) numXSelectedIndexes /
							(double) xmap.getNumVisible();
				}
				scrollFrac /= 2.0;

				int scrollDistDirX = (selecXStartIndex +
						(int) Math.floor(numXSelectedIndexes / 2)) -
						centerXIndex;

				//debug("ScrollX difference before fractioning: " +
				//scrollDistDirX + " = selecXStartIndex + " +
				//"numXSelectedIndexes / 2) - centerXIndex /// " +
				//selecXStartIndex + " + " + numXSelectedIndexes + " / 2) - " +
				//centerXIndex + ".");
				scrollDistDirX =
						(int) Math.round(scrollDistDirX * scrollFrac);
				//debug("ScrollX difference after fractioning: [" +
				//scrollDistDirX + "].");

				/*
				 * TODO: Refactor the scrolling so that these checks do not have
				 * to be done. - Rob
				 */
				//The following correction calculations are to prevent scrolling
				//a visible selected edge out of view.
				//The calculations are not perfect and are only necessary
				//because the zoomToward* functions only really work when the
				//selection is either completely in view or we're zoomed in
				//inside the selected area.
				int prevFarEdge = prevXFirstVisible + prevXNumVisible;
				int selecFarEdge = selecXStartIndex + numXSelectedIndexes;
				int newNearEdge = (selecXStartIndex +
						(int) Math.floor(numXSelectedIndexes / 2)) -
						scrollDistDirX -
						(int) Math.ceil((double) xmap.getNumVisible() / 2);
				int newFarEdge = (selecXStartIndex +
						(int) Math.floor(numXSelectedIndexes / 2)) -
						scrollDistDirX +
						(int) Math.ceil((double) xmap.getNumVisible() / 2);
				//Make sure we don't scroll an edge from either in view or
				//before the view to out of view/past the view
				if(selecFarEdge <= prevFarEdge && selecFarEdge > newFarEdge) {
					scrollDistDirX -= (selecFarEdge - newFarEdge) +
							(int) Math.round((1.0 - scrollFrac) *
									Math.abs((double) selecFarEdge -
											(double) prevFarEdge));
					//debug("ScrollX difference after correcting " +
					//"for far edge overscroll: [" + scrollDistDirX + "].");
					newNearEdge = (selecXStartIndex +
							(int) Math.floor(numXSelectedIndexes / 2)) -
							scrollDistDirX -
							(int) Math.ceil((double) xmap.getNumVisible() / 2);
					newFarEdge = (selecXStartIndex +
							(int) Math.floor(numXSelectedIndexes / 2)) -
							scrollDistDirX +
							(int) Math.ceil((double) xmap.getNumVisible() / 2);
					if(selecFarEdge > newFarEdge ||
					   (selecXStartIndex >= prevXFirstVisible &&
						selecXStartIndex < newNearEdge)) {
						scrollDistDirX = 0;
						//debug("ScrollX difference after " +
						//"correcting for far edge overscroll: [" +
						//scrollDistDirX + "].");
					}
				} else if(selecXStartIndex >= prevXFirstVisible &&
						  selecXStartIndex < newNearEdge) {
					scrollDistDirX += (newNearEdge - selecXStartIndex) +
							(int) Math.round((1.0 - scrollFrac) *
									((double) selecXStartIndex -
											(double) prevXFirstVisible));
					//debug("ScrollX difference after correcting " +
					//"for near edge overscroll: [" + scrollDistDirX + "].");
					newNearEdge = (selecXStartIndex +
							(int) Math.floor(numXSelectedIndexes / 2)) -
							scrollDistDirX -
							(int) Math.ceil((double) xmap.getNumVisible() / 2);
					newFarEdge = (selecXStartIndex +
							(int) Math.floor(numXSelectedIndexes / 2)) -
							scrollDistDirX +
							(int) Math.ceil((double) xmap.getNumVisible() / 2);
					if(selecXStartIndex < newNearEdge ||
					   (selecFarEdge <= prevFarEdge &&
					    selecFarEdge > newFarEdge)) {
						scrollDistDirX = 0;
						//debug("ScrollX difference after " +
						//"correcting for far edge overscroll: [" +
						//scrollDistDirX + "].");
					}
				}

				//debug("Correcting the X scroll because the " +
				//"pixel we are zooming to/from is on an edge.");
				//debug("(selecXStartIndex + numXSelectedIndexes " +
				//"/ 2) - scrollDistDirX");
				//debug(((selecXStartIndex + (int) Math.floor(" +
				//"numXSelectedIndexes / 2)) - scrollDistDirX) + " = (" +
				//selecXStartIndex + " + " + numXSelectedIndexes + " / 2) - " +
				//scrollDistDirX);
				xmap.scrollToIndex((selecXStartIndex +
						(int) Math.floor(numXSelectedIndexes / 2)) -
						scrollDistDirX);
			}

			//If a selected edge (toward which we are zooming) is outside the
			//previously visible corresponding boundary AND the selected area is
			//not surrounding the visible area AND the pixel we are zooming to
			//is on an edge, assume we need to scroll incrementally to bring the
			//pixel we are zooming to/from into view
			if((((selecYStartIndex < prevYFirstVisible ||
				  (selecYStartIndex + numYSelectedIndexes - 1) >
				  (prevYFirstVisible + prevYNumVisible - 1)) &&
				 !(selecYStartIndex < prevYFirstVisible &&
				   (selecYStartIndex + numYSelectedIndexes - 1) >
				   (prevYFirstVisible + prevYNumVisible - 1)))) &&
			   (yPxPos <= 0 || yPxPos >= (yPxNum - 1))) {

				//We don't want to scroll all the way in 1 jolt, so we'll scroll
				//to be closer to the center of the selection by this fraction
				//(unless we're at the zoom level of the selection)
				//Scroll toward by half of the same fraction as the zoom changed
				double scrollFrac = ((double) ymap.getNumVisible() /
						(double) numYSelectedIndexes);
				if(scrollFrac > 1) {
					scrollFrac =  (double) numYSelectedIndexes /
							(double) ymap.getNumVisible();
				}
				scrollFrac /= 2.0;

				int scrollDistDirY = (selecYStartIndex +
						(int) Math.floor(numYSelectedIndexes / 2)) -
						centerYIndex;
				//debug("ScrollY difference before fractioning: " +
				//scrollDistDirY + " = selecYStartIndex + " +
				//"numYSelectedIndexes / 2) - centerYIndex /// " +
				//selecYStartIndex + " + " + numYSelectedIndexes + " / 2) - " +
				//centerYIndex + ".");
				scrollDistDirY = (int) Math.round(scrollDistDirY *
						scrollFrac);
				//debug("ScrollY difference after fractioning: [" +
				//scrollDistDirY + "].");
	
				/*
				 * TODO: Refactor the scrolling so that these checks do not have
				 * to be done. - Rob
				 */
				//The following correction calculations are to prevent scrolling
				//a visible selected edge out of view.
				//The calculations are not perfect and are only necessary
				//because the zoomToward* functions only really work when the
				//selection is either completely in view or we're zoomed in
				//inside the selected area.
				int prevFarEdge = prevYFirstVisible + prevYNumVisible;
				int selecFarEdge = selecYStartIndex + numYSelectedIndexes;
				int newNearEdge = (selecYStartIndex +
						(int) Math.floor(numYSelectedIndexes / 2)) -
						scrollDistDirY -
						(int) Math.ceil((double) ymap.getNumVisible() / 2);
				int newFarEdge = (selecYStartIndex +
						(int) Math.floor(numYSelectedIndexes / 2)) -
						scrollDistDirY +
						(int) Math.ceil((double) ymap.getNumVisible() / 2);
				//Make sure we don't scroll an edge from either in view or
				//before the view to out of view/past the view
				if(selecFarEdge <= prevFarEdge && selecFarEdge > newFarEdge) {
					scrollDistDirY -= (selecFarEdge - newFarEdge) +
							(int) Math.round((1.0 - scrollFrac) *
									Math.abs((double) selecFarEdge -
											(double) prevFarEdge));
					//debug("ScrollY difference after correcting " +
					//"for far edge overscroll: [" + scrollDistDirY + "].");
					newNearEdge = (selecYStartIndex +
							(int) Math.floor(numYSelectedIndexes / 2)) -
							scrollDistDirY -
							(int) Math.ceil((double) ymap.getNumVisible() / 2);
					newFarEdge = (selecYStartIndex +
							(int) Math.floor(numYSelectedIndexes / 2)) -
							scrollDistDirY +
							(int) Math.ceil((double) ymap.getNumVisible() / 2);
					if(selecFarEdge > newFarEdge ||
					   (selecYStartIndex >= prevYFirstVisible &&
						selecYStartIndex < newNearEdge)) {
						scrollDistDirY = 0;
						//debug("ScrollY difference after " +
						//"correcting for far edge overscroll: [" +
						//scrollDistDirY + "].");
					}
				} else if(selecYStartIndex >= prevYFirstVisible &&
						  selecYStartIndex < newNearEdge) {
					scrollDistDirY += (newNearEdge - selecYStartIndex) +
							(int) Math.round((1.0 - scrollFrac) *
									((double) selecYStartIndex -
											(double) prevYFirstVisible));
					//debug("ScrollY difference after correcting " +
					//"for near edge overscroll: [" + scrollDistDirY +
					//"] because (selecYStartIndex >= prevYFirstVisible && " +
					//"selecYStartIndex < newNearEdge) = (" + selecYStartIndex +
					//" >= " + prevYFirstVisible + " && " + selecYStartIndex +
					//" < " + newNearEdge + ").");
					newNearEdge = (selecYStartIndex +
							(int) Math.floor(numYSelectedIndexes / 2)) -
							scrollDistDirY -
							(int) Math.ceil((double) ymap.getNumVisible() / 2);
					newFarEdge = (selecYStartIndex +
							(int) Math.floor(numYSelectedIndexes / 2)) -
							scrollDistDirY +
							(int) Math.ceil((double) ymap.getNumVisible() / 2);
					if(selecYStartIndex < newNearEdge ||
					   (selecFarEdge <= prevFarEdge &&
					    selecFarEdge > newFarEdge)) {
						//debug("ScrollY difference after " +
						//"correcting for far edge overscroll: [" +
						//scrollDistDirY + "].");
						scrollDistDirY = 0;
					}
				}

				//debug("Correcting the Y scroll because the " +
				//"pixel we are zooming to/from is on an edge.");
				//debug("(selecYStartIndex + numYSelectedIndexes " +
				//"/ 2) - scrollDistDirY");
				//debug(((selecYStartIndex +
				//(int) Math.floor((double) numYSelectedIndexes / 2.0)) -
				//scrollDistDirY) + " = (" + selecYStartIndex + " + " +
				//numYSelectedIndexes + " / 2) - " + scrollDistDirY);
				ymap.scrollToIndex((selecYStartIndex +
						(int) Math.floor(numYSelectedIndexes / 2.0)) -
						scrollDistDirY);
			}
		}

		//Update the aspect ratio once we have arrived at our destination (as we
		//are going to be using the starting aspect ratio to use it to gradually
		//adjust the target aspect ratio)
		if(xmap.getNumVisible() == numXSelectedIndexes &&
		   ymap.getNumVisible() == numYSelectedIndexes) {
			updateAspectRatio();
		}

		//debug("Zoom redraw bounds initial: [" + startXPixel + "," +
		//		startYPixel + "," + (startXPixel + numSelectedXPixels - 1) +
		//		"," + (startYPixel + numSelectedYPixels - 1) +
		//		"]. Panel dimensions: [" + getWidth() + "x" + getHeight() +
		//		"].");
		//Compute the area to redraw (only for zooming-out - see code at end)
		int[] redrawPixelBounds = new int[4];

		pixelsPerXIndex = xmap.getScale();
		if(prevXNumVisible > numXSelectedIndexes) {
			//debug("Zoom redraw bounds initial X: [" + startXPixel +
			//		"," + (startXPixel + numSelectedXPixels - 1) +
			//		"]. Panel dimensions: [" + getWidth() + "].");
			//Now let's return the pixel indexes of the selection post-zoom
			redrawPixelBounds[0] = xmap.getPixel(selecXStartIndex);
			redrawPixelBounds[2] =
				(int) Math.round((double) numXSelectedIndexes *
				pixelsPerXIndex) + 1; //Added 1 because sometimes inaccurate
		} else {
			redrawPixelBounds[0] = xmap.getPixel(prevXFirstVisible);
			redrawPixelBounds[2] = (int) Math.round((double) prevXNumVisible *
					pixelsPerXIndex) + 1; //Added 1 because sometimes inaccurate
		}
		pixelsPerYIndex = ymap.getScale();
		if(prevYNumVisible > numYSelectedIndexes) {
			//debug("Zoom redraw bounds initial: [" + startYPixel +
			//		"," + (startYPixel + numSelectedYPixels - 1) +
			//		"]. Panel dimensions: [" + getHeight() + "].");
			redrawPixelBounds[1] = ymap.getPixel(selecYStartIndex);
			redrawPixelBounds[3] =
				(int) Math.round((double) numYSelectedIndexes *
				pixelsPerYIndex) + 1; //Added 1 because sometimes inaccurate
			//debug("Zoom redraw bounds before fix: [" +
			//		redrawPixelBounds[0] + "," + redrawPixelBounds[1] + "," +
			//		redrawPixelBounds[2] + "," + redrawPixelBounds[3] + "].");
		} else {
			redrawPixelBounds[1] = ymap.getPixel(prevYFirstVisible);
			redrawPixelBounds[3] = (int) Math.round((double) prevYNumVisible *
					pixelsPerYIndex) + 1; //Added 1 because sometimes inaccurate
		}

		int maxWidth  = getWidth();
		int maxHeight = getHeight();

		if(redrawPixelBounds[0] < 0) redrawPixelBounds[0] = 0;
		if(redrawPixelBounds[0] > maxWidth) redrawPixelBounds[0] = maxWidth - 1;
		if(redrawPixelBounds[2] < 0) redrawPixelBounds[2] = 0;
		if((redrawPixelBounds[0] + redrawPixelBounds[2]) > maxWidth)
				redrawPixelBounds[2] = maxWidth - redrawPixelBounds[0];

		if(redrawPixelBounds[1] < 0) redrawPixelBounds[1] = 0;
		if(redrawPixelBounds[1] > maxHeight) redrawPixelBounds[1] =
				maxHeight - 1;
		if(redrawPixelBounds[3] < 0) redrawPixelBounds[3] = 0;
		if((redrawPixelBounds[1] + redrawPixelBounds[3]) > maxHeight)
				redrawPixelBounds[3] = maxHeight - redrawPixelBounds[1];
		//debug("Zoom redraw bounds after fix: [" +
		//		redrawPixelBounds[0] + "," + redrawPixelBounds[1] + "," +
		//		redrawPixelBounds[2] + "," + redrawPixelBounds[3] + "].");
		return(redrawPixelBounds);
	}

	public double getAspectRatio(int numXDots,int numYDots) {
		return((double) numXDots / (double) numYDots);
	}

	public void updateAspectRatio() {
		aspectRatio = getAspectRatio(xmap.getNumVisible(),ymap.getNumVisible());
		//debug("Aspect Ratio updated to [" + aspectRatio + " : " +
		//xmap.getNumVisible() + "/" + ymap.getNumVisible() + "].");
	}

	public void setAspectRatio(int numXDots,int numYDots) {
		aspectRatio = getAspectRatio(numXDots,numYDots);
		//debug("Aspect Ratio set to [" + aspectRatio + " : " +
		//xmap.getNumVisible() + "/" + ymap.getNumVisible() + "].");
	}

	private void drawBand(final Rectangle l) {

		final Graphics g = getGraphics();
		Graphics2D g2d = (Graphics2D) g;
		g2d.setXORMode(getBackground());
		g2d.setColor(GUIFactory.MAIN);

		final int x = xmap.getPixel(l.x);
		final int y = ymap.getPixel(l.y);
		final int w = xmap.getPixel(l.x + l.width + 1) - x;
		final int h = ymap.getPixel(l.y + l.height + 1) - y;

		g2d.drawRect(x, y, w, h);
		g2d.setPaintMode();
	}

	/**
	 * Draws a circle if the user selects one rectangle in the clustergram to
	 * indicate the position of this rectangle.
	 */
	private void setIndicatorCircleBounds() {

		double x = 0;
		double y = 0;
		double w = 0;
		double h = 0;

		if (geneSelection == null || arraySelection == null ||
			arraySelection.getNSelectedIndexes() == 0 ||
			geneSelection.getNSelectedIndexes()  == 0) {
			indicatorCircleList = null;
			return;
		}

		//Empty the list of
		indicatorCircleList = new ArrayList<Ellipse2D.Double>();

		final int[] selectedArrayIndexes = arraySelection.getSelectedIndexes();
		final int[] selectedGeneIndexes  = geneSelection.getSelectedIndexes();

		List<List<Integer>> arrayBoundaryList;
		List<List<Integer>> geneBoundaryList;

		arrayBoundaryList = findRectangleBoundaries(selectedArrayIndexes,
				xmap);
		geneBoundaryList  = findRectangleBoundaries(selectedGeneIndexes,
				ymap);

		/*
		 * TODO: Instead of just checking the last(/next) selection
		 * position, should group all small selections together too see
		 * if the cluster is smaller than our limits.
		 */
		double lastxb = -1;

		// Make the rectangles
		for (final List<Integer> xBoundaries : arrayBoundaryList) {

			/*
			 * TODO: Instead of just checking the last(/next) selection
			 * position, should group all small selections together too see
			 * if the cluster is smaller than our limits.
			 */
			double lastyb = -1;
			w = (xBoundaries.get(1) - xBoundaries.get(0));

			for (final List<Integer> yBoundaries : geneBoundaryList) {

				//Width and height of rectangle which spans the Ellipse2D
				//object
				h = (yBoundaries.get(1) - yBoundaries.get(0));

				if(w < 20 && h < 20 &&
				   //This is not the first selection and the last selection is
					//far away OR this is the first selection and the next
					//selection either doesn't exists or is far away
				   ((lastxb >= 0 &&
				     Math.abs(xBoundaries.get(0) - lastxb) > 20) ||
				    (lastxb < 0 &&
				     (arrayBoundaryList.size() == 1 ||
				      Math.abs(arrayBoundaryList.get(1).get(0) -
				               xBoundaries.get(1)) > 20))) &&
				   ((lastyb >= 0 &&
				     Math.abs(yBoundaries.get(0) - lastyb) > 20) ||
				    (lastyb < 0 &&
				     (geneBoundaryList.size() == 1 ||
				      Math.abs(geneBoundaryList.get(1).get(0) -
				               yBoundaries.get(1)) > 20)))) {
					// coords for top left of circle
					x = xBoundaries.get(0) + (w / 2.0) - 20;
					y = yBoundaries.get(0) + (h / 2.0) - 20;

					indicatorCircleList.add(new Ellipse2D.Double(x, y, 40, 40));
				}
				lastyb = yBoundaries.get(1);
			}
			lastxb = xBoundaries.get(1);
		}
	}

	/**
	 * Selecting a rectangular area in GlobalView
	 *
	 * @param start
	 * @param end
	 */
	private void selectRectangle(final Point start, final Point end) {

		// sort so that ep is upper left corner
		if (end.x < start.x) {
			final int x = end.x;
			end.x = start.x;
			start.x = x;
		}

		if (end.y < start.y) {
			final int y = end.y;
			end.y = start.y;
			start.y = y;
		}

		// nodes
		geneSelection.setSelectedNode(null);

		// genes...
		geneSelection.deselectAllIndexes();

		for (int i = start.y; i <= end.y; i++) {

			geneSelection.setIndexSelection(i, true);
		}

		// arrays...
		arraySelection.setSelectedNode(null);
		arraySelection.deselectAllIndexes();

		for (int i = start.x; i <= end.x; i++) {

			arraySelection.setIndexSelection(i, true);
		}

		//debug("Rectangle selected.");
		globalMatrixView.setIMVselectedIndexes(
				arraySelection.getSelectedIndexes(),
				geneSelection.getSelectedIndexes());

		geneSelection.notifyObservers();
		arraySelection.notifyObservers();
	}

	/**
	 * Sets the gene header instance variables of GlobalView.
	 *
	 * @param ghi
	 * @param ahi
	 */
	public void setHeaders(final HeaderInfo ghi, final HeaderInfo ahi) {

		geneHI = ghi;
		arrayHI = ahi;
	}

	public TreeSelectionI getGeneSelection() {

		return(geneSelection);
	}

	public TreeSelectionI getArraySelection() {

		return(arraySelection);
	}
}
