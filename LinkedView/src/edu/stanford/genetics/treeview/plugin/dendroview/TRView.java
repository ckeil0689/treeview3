package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.Observable;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LinearTransformation;
import edu.stanford.genetics.treeview.ModelViewBuffered;
import edu.stanford.genetics.treeview.TreeDrawerNode;
import edu.stanford.genetics.treeview.TreeSelectionI;
import net.miginfocom.swing.MigLayout;

public abstract class TRView extends ModelViewBuffered implements KeyListener,
	MouseListener,MouseMotionListener,MouseWheelListener {

	private static final long serialVersionUID = 1L;

	private final Color whiz_bg_color = new Color(215,234,251); //light pale blue
	protected TreeSelectionI treeSelection;
	protected HeaderSummary headerSummary;
	protected LinearTransformation xScaleEq, yScaleEq;
	protected MapContainer map;

	protected final JScrollPane scrollPane;

	protected TreePainter treePainter = null;
	protected TreeDrawerNode selectedNode = null;
	protected TreeDrawerNode hoveredNode = null;
	protected Rectangle destRect = null;

	//isLeft is true if the tree being drawn is for the rows (i.e. oriented to
	//the left of the matrix)
	protected boolean isLeft;

	private final static int REPAINT_INTERVAL = 50;  //update every 50 millisecs
	/* TODO: If anastasia doesn't like trees linked to whizzing labels,
	 * uncomment the following commented code. If she likes it, delete. This
	 * code saves compute cycles, but makes the trees sometimes move out of sync
	 * with the labels. */
//	private int slowRepaintInterval = 1000;//update every 1s if mouse not moving
	private int lastHoverIndex = -1;

	public TRView(final boolean isGeneTree) {

		super();

		setLayout(new MigLayout());

		final String summary = (isGeneTree) ? "GtrSummary" : "AtrSummary";
		this.headerSummary = new HeaderSummary(summary);

		addMouseWheelListener(this);

		if(isGeneTree) {
			scrollPane = new JScrollPane(this,
					ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		} else {
			scrollPane = new JScrollPane(this,
					ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		}

		panel = scrollPane;
		
		destRect = new Rectangle();

		debug = 0;
		//17 = debug whizzing tree mode
	}
	
	public JScrollBar getVerticalScrollBar() {
		
		return scrollPane.getVerticalScrollBar();
	}
	
	public JScrollBar getHorizontalScrollBar() {
		
		return scrollPane.getHorizontalScrollBar();
	}

	/**
	 * Set the drawer
	 *
	 * @param d
	 *            The new drawer
	 */
	public void setTreeDrawer(final TreePainter d) {

		if (treePainter != null) {
			treePainter.deleteObserver(this);
		}

		treePainter = d;
		treePainter.addObserver(this);
	}

	/**
	 * Set the map. For the ATRView, this determines where the leaves of the
	 * tree will be.
	 *
	 * @param m
	 *            The new map to be used for determining the spacing between
	 *            indexes.
	 */
	public void setMap(final MapContainer m) {

		if (map != null) {
			map.deleteObserver(this);
		}

		map = m;
		map.addObserver(this);

		offscreenValid = false;
		repaint();
	}

	/**
	 * @param nodeName
	 */
	public void scrollToNode(final String nodeName) {

		final TreeDrawerNode node = treePainter.getNodeById(nodeName);
		if (node != null) {
			final int index = (int) node.getIndex();
			if (!map.isVisible(index)) {
				map.scrollToIndex(index);
				map.notifyObservers();
			}
		}
	}

	/**
	 * Make sure the selected array range reflects the selected node, if any,
	 * and that the hover tree indexes are up to date.
	 */
	private void synchMap() {

		if((selectedNode != null) && (treeSelection != null)) {
			final int start = (int) (selectedNode.getLeftLeaf().getIndex());
			final int end = (int) (selectedNode.getRightLeaf().getIndex());

			treeSelection.deselectAllIndexes();
			treeSelection.setSelectedNode(selectedNode.getId());
			treeSelection.selectNewIndexRange(start,end);
			treeSelection.notifyObservers();
		}

		//Keep the min and max updated so that the labels will color correctly
		//based on tree hover
		if(hoveredNode != null) {
			map.setHoverTreeMinIndex(
				(int) hoveredNode.getLeftLeaf().getIndex());
			map.setHoverTreeMaxIndex(
				(int) hoveredNode.getRightLeaf().getIndex());
		}
	}

	/**
	 * Set <code>TreeSelection</code> object which coordinates the shared
	 * selection state.
	 *
	 * @param treeSelection
	 *            The <code>TreeSelection</code> which is set by selecting rows
	 *            or columns in the </code>GlobalView</code>
	 */
	public void setTreeSelection(final TreeSelectionI treeSelection) {

		if (this.treeSelection != null) {
			this.treeSelection.deleteObserver(this);
		}

		this.treeSelection = treeSelection;
		this.treeSelection.addObserver(this);
	}

	public void setHeaderSummary(final HeaderSummary headerSummary) {

		this.headerSummary = headerSummary;
	}

	public HeaderSummary getHeaderSummary() {

		return headerSummary;
	}

	/**
	 * Set the selected node and redraw
	 *
	 * @param n
	 *            The new node to be selected Does nothing if the node is
	 *            already selected.
	 */
	public void setSelectedNode(final TreeDrawerNode n) {

		if (selectedNode == n)
			return;

		selectedNode = n;

		synchMap();
		repaint();
	}

	/**
	 * Sets the node which currently is closest to the mouse cursor.
	 *
	 * @param n
	 *            The node close to the mouse cursor.
	 */
	public void setHoveredNode(final TreeDrawerNode n) {

		if(hoveredNode == n) {
			return;
		}

		hoveredNode = n;

		synchMap();
		repaint();
	}

	/**
	 * Sets the hoveredNode to null
	 * @author rleach
	 */
	public void unsetHoveredNode() {
		hoveredNode = null;
	}

	/**
	 * Paint a node and its children.
	 *
	 * @param node
	 *            The node which is supposed to be painted.
	 * @param isColored
	 *            Whether the node and its children should be black or colored.
	 *            Colored is used for hovering and selected nodes.
	 */
	private void paintNode(final TreeDrawerNode node,final boolean isSelected) {

		if (xScaleEq != null) {
			treePainter.paintSubtree(offscreenGraphics,xScaleEq,yScaleEq,
				destRect,node,isLeft,getPrimaryHoverIndex(),treeSelection,
				hoveredNode);
		}
	}

	@Override
	public void update(final Observable o, final Object arg) {

		if (!isEnabled())
			return;

		if (o == map) {
			offscreenValid = false;
			repaint();

		} else if (o == treePainter) {
			offscreenValid = false;
			repaint();

		} else if (o == treeSelection) {
			TreeDrawerNode cand = null;
			boolean oddSelection = false;
			if (treeSelection.getNSelectedIndexes() > 0) {
				// This clause selects the array node if only a
				// single array is selected.
				if(treeSelection.getMinIndex() == treeSelection.getMaxIndex()) {
					cand = treePainter.getLeaf(treeSelection.getMinIndex());
				}
				// this clause selects the root node if all arrays are selected.
				else if (treeSelection.getMinIndex() == map.getMinIndex()
					&& treeSelection.getMaxIndex() == map.getMaxIndex() &&
					// All the intervening rows/cols are selected
					(treeSelection.getMaxIndex()
							- treeSelection.getMinIndex() + 1) ==
							treeSelection.getNSelectedIndexes()) {

					cand = treePainter.getRootNode();

				} else if (treeSelection.getMinIndex() >= map.getMinIndex()
					&& treeSelection.getMaxIndex() <= map.getMaxIndex() &&
					// All the intervening rows/cols are selected
					(treeSelection.getMaxIndex()
							- treeSelection.getMinIndex() + 1) ==
							treeSelection.getNSelectedIndexes()) {

					cand = treePainter.getNearestNode(
							treeSelection.getMinIndex(),
							treeSelection.getMaxIndex());
					// If no candidate was found or the candidate is not an
					// exact match
					if (cand == null
							|| ((int) Math.round(cand.getMinIndex()) !=
							treeSelection.getMinIndex() ||
							(int) Math.round(cand.getMaxIndex()) !=
							treeSelection.getMaxIndex())) {
						oddSelection = true;
					}
				}
				// Otherwise, it's an odd selection that doesn't match a tree
				// node
				else {
					oddSelection = true;
				}
			} else if (treeSelection.getMinIndex() >= map.getMinIndex()
				&& treeSelection.getMaxIndex() <= map.getMaxIndex()) {
				setSelectedNode(null);
			}
			// Only notify observers if we're changing the selected node.
			if ((cand != null)
					&& !(cand.getId().equalsIgnoreCase(treeSelection
							.getSelectedNode()))) {
				treeSelection.setSelectedNode(cand.getId());
				treeSelection.notifyObservers();

			} else if (oddSelection) {
				setSelectedNode(null);
			} else {
				setSelectedNode(cand);
			}

		} else {
			System.out.println(viewName() + "Got an update from unknown " + o);
		}

		revalidate();
		repaint();
	}

	/* inherit description */
	@Override
	public void updateBuffer(final Graphics g) {

		if (treePainter == null) {
			return;
		}

		if (offscreenChanged) {
			offscreenValid = false;
		}

		debug("updateBuffer called for column trees",14);

		updateTreeRepaintTimers();

		if(!offscreenValid || map.isLabelAnimeRunning()) {
			map.setAvailablePixels(getPrimaryPaneSize(offscreenSize));

			/* clear the panel */
			g.setColor(this.getBackground());
			g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);

			int firstVisIndex;
			int lastVisIndex;

			//If we're in label port/whizzing label mode
			if(map.isLabelAnimeRunning() && map.overALabelPortLinkedView() &&
				!map.shouldKeepTreeGlobal() && map.isWhizMode() &&
				map.getFirstVisibleLabel() > -1 &&
				map.getLastVisibleLabel() > -1) {

				g.setColor(Color.black);

				debug("Whizzing Tree mode",17);

				/* calculate scaling */
				setWhizzingDestRectBounds();
				
				debug("pixels used [" + map.getUsedPixels() +
					"] first label offset [" +
					map.getFirstVisibleLabelOffset() +
					"] last label offset [" +
					map.getLastVisibleLabelOffset() + "]",
					16);

				firstVisIndex = map.getFirstVisibleLabel();
				lastVisIndex  = map.getLastVisibleLabel();

				debug("first visible label index [" + firstVisIndex +
					"] last visible label index [" + lastVisIndex + "]",16);

				debug("destRect.x [" + getFittedDestRectStart() +
					"] map.getFirstVisibleLabelOffset() [" +
					map.getFirstVisibleLabelOffset() + "] destRect length [" +
					getFittedDestRectLength() + "]",16);

				setPrimaryScaleEq(new LinearTransformation(
					firstVisIndex,
					getWhizzingDestRectStart(),
					lastVisIndex + 1,
					getWhizzingDestRectEnd()));

				map.setLastTreeModeGlobal(false);
			} else {
				//If we are looking at a global/data-linked tree with whizzing
				//labels
				if(map.isWhizMode()) {

					g.setColor(whiz_bg_color);

					drawFittedWhizBackground(g,getPrimaryScaleEq());
				}

				g.setColor(Color.black);

				/* calculate scaling */
				setFittedDestRectBounds();

				firstVisIndex = map.getIndex(getFittedDestRectStart());
				lastVisIndex  = map.getIndex(getFittedDestRectEnd());

				setPrimaryScaleEq(new LinearTransformation(
					firstVisIndex,
					getFittedDestRectStart(),
					lastVisIndex,
					getFittedDestRectEnd()));

				map.setLastTreeModeGlobal(true);
			}

			setSecondaryScaleEq(new LinearTransformation(
				treePainter.getCorrMin(),
				getSecondaryDestRectStart(),
				treePainter.getCorrMax(),
				getSecondaryDestRectEnd()));

			/* draw trees */
			treePainter.paint(g,xScaleEq,yScaleEq,destRect,isLeft,
				getPrimaryHoverIndex(),treeSelection,hoveredNode);
		}
	}

	/* Abstract methods */
	protected abstract int  getSecondaryPaneSize(final Dimension dims);
	protected abstract int  getPrimaryPaneSize(final Dimension dims);
	protected abstract int  getUsedWhizzingLength();
	protected abstract void setWhizzingDestRectBounds();
	protected abstract int  getWhizzingDestRectStart();
	protected abstract int  getWhizzingDestRectEnd();
	protected abstract void setFittedDestRectBounds();
	protected abstract int  getFittedDestRectStart();
	protected abstract int  getFittedDestRectEnd();
	protected abstract int  getFittedDestRectLength();
	protected abstract int  getSecondaryDestRectStart();
	protected abstract int  getSecondaryDestRectEnd();
	protected abstract void setPrimaryScaleEq(final LinearTransformation
		scaleEq);
	protected abstract LinearTransformation getPrimaryScaleEq();
	protected abstract void setSecondaryScaleEq(final LinearTransformation
		scaleEq);
	protected abstract TreeDrawerNode getClosestNode(final MouseEvent e);
	protected abstract int getPrimaryPixelIndex(final MouseEvent e);
	protected abstract TreeDrawerNode getClosestParentNode(final MouseEvent e);
	protected abstract void drawWhizBackground(final Graphics g);
	protected abstract void drawFittedWhizBackground(final Graphics g,
		LinearTransformation scaleEq);
	protected abstract void setExportScale(final Rectangle dest);

	/**
	 * Need to blit another part of the buffer to the screen when the scrollbar
	 * moves.
	 *
	 * @param evt
	 *            The adjustment event generated by the scrollbar
	 */
	public void adjustmentValueChanged(final AdjustmentEvent evt) {

		repaint();
	}

	/**
	 * 
	 * @author rleach
	 * @return the length of the area the whizzing labels represent in data
	 * pixels
	 */
	protected int getWhizzingDestRectLength() {
		if(destRect == null) {
			return(-1);
		}
		return(getWhizzingDestRectEnd() - getWhizzingDestRectStart() + 1);
	}

	/**
	 * Arrow keys are used to change the selected node.
	 *
	 * up selects parent of current. left selects left child. right selects
	 * right child down selects child with most descendants.
	 *
	 */
	@Override
	public void keyPressed(final KeyEvent e) {

		if(selectedNode == null) {
			return;
		}

		final int c = e.getKeyCode();

		switch (c) {
		case KeyEvent.VK_UP:
			selectParent();
			break;

		case KeyEvent.VK_LEFT:
			if (!selectedNode.isLeaf()) {
				selectLeft();
			}
			break;

		case KeyEvent.VK_RIGHT:
			if (!selectedNode.isLeaf()) {
				selectRight();
			}
			break;

		case KeyEvent.VK_DOWN:
			if (!selectedNode.isLeaf()) {
				final TreeDrawerNode right = selectedNode.getRight();
				final TreeDrawerNode left = selectedNode.getLeft();

				if (right.getRange() > left.getRange()) {
					selectRight();

				} else {
					selectLeft();
				}
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void keyTyped(final KeyEvent e) {

	}

	@Override
	public void keyReleased(final KeyEvent e) {

	}

	private void selectParent() {

		TreeDrawerNode current = selectedNode;
		selectedNode = current.getParent();

		if (selectedNode == null) {
			selectedNode = current;
			return;
		}

		if (current == selectedNode.getLeft()) {
			current = selectedNode.getRight();

		} else {
			current = selectedNode.getLeft();
		}

		//Make the selection
		synchMap();

		//Paint from the parent down based on new selection
		treePainter.paintSubtree(offscreenGraphics,xScaleEq,yScaleEq,
			destRect,selectedNode,isLeft,getPrimaryHoverIndex(),treeSelection,
			hoveredNode);

		repaint();
	}

	private void selectRight() {

		if (selectedNode.isLeaf())
			return;

		final TreeDrawerNode current = selectedNode;
		selectedNode = current.getRight();

		synchMap();

		treePainter.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
			destRect,current,isLeft,getPrimaryHoverIndex(),treeSelection,
			hoveredNode);

		repaint();
	}

	private void selectLeft() {

		if (selectedNode.isLeaf())
			return;

		final TreeDrawerNode current = selectedNode;
		selectedNode = current.getLeft();

		synchMap();

		treePainter.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
			destRect,current,isLeft,getPrimaryHoverIndex(),treeSelection,
			hoveredNode);

		repaint();
	}

	@Override
	public String viewName() {

		return "TRView";
	}

	/** Setter for xScaleEq */
	public void setXScaleEq(final LinearTransformation xScaleEq) {

		this.xScaleEq = xScaleEq;
	}

	/** Getter for xScaleEq */
	public LinearTransformation getXScaleEq() {

		return xScaleEq;
	}

	/** Setter for yScaleEq */
	public void setYScaleEq(final LinearTransformation yScaleEq) {

		this.yScaleEq = yScaleEq;
	}

	/** Getter for yScaleEq */
	public LinearTransformation getYScaleEq() {

		return yScaleEq;
	}

	//This is an attempt to get the hovering of the mouse over the matrix to get
	//the tree panes to update more quickly and regularly, as the
	//notifyObservers method called from MapContainer was resulting in sluggish
	//updates
	private Timer repaintTimer =
		new Timer(REPAINT_INTERVAL,
		          new ActionListener() {
			/**
			 * The timer "ticks" by calling
			 * this method every _timeslice
			 * milliseconds
			 */
			@Override
			public void
			actionPerformed(ActionEvent e) {
				//This shouldn't be necessary, but when I change setPoints() to
				//setTemporaryPoints in the drawing of the HINT, the timer never
				//stops despite stop being continually called, so I'm going to
				//call stop in here if the map says that the animation is
				//supposed to have been stopped...
				if(!map.isLabelAnimeRunning()) {
					repaintTimer.stop();
				}
				debug("Repainting column tree",14);
				repaint();
			}
		});

	/* TODO: If anastasia doesn't like trees linked to whizzing labels,
	 * uncomment the following commented code. If she likes it, delete. This
	 * code saves compute cycles, but makes the trees sometimes move out of sync
	 * with the labels. */
//	//Timer to wait a bit before slowing down the slice _timer for painting.
//	//This conserves processor cycles in the interests of performance.  Note
//	//that there is a pair of timers for each axis.
//	final private int delay = 1000;
//	private javax.swing.Timer slowDownRepaintTimer;
//	ActionListener slowDownRepaintListener = new ActionListener() {
//
//		@Override
//		public void actionPerformed(ActionEvent evt) {
//			if(evt.getSource() == slowDownRepaintTimer) {
//				/* Stop timer */
//				slowDownRepaintTimer.stop();
//				slowDownRepaintTimer = null;
//
//				//If we are still over a label port view panel, just slow the
//				//repaint timer, because this was triggered by the mouse not
//				//moving
//				if(map.overALabelPortLinkedView()) {
//					debug("Slowing the repaint interval presumably because " +
//					      "of lack of mouse movement",9);
//					repaintTimer.setDelay(slowRepaintInterval);
//				} else {
//					repaintTimer.stop();
//					map.setLabelAnimeRunning(false);
//				}
//			}
//		}
//	};

	public void updateTreeRepaintTimers() {
		//If the mouse is not hovering over the IMV, stop both timers, set the
		//last hover index, and tell mapcontainer that the animation has stopped
		if(!map.overALabelPortLinkedView()) {
			if(repaintTimer != null && repaintTimer.isRunning()) {
				debug("Not hovering over a label port linked view - stopping " +
					"animation",9);
				repaintTimer.stop();
				lastHoverIndex = -1;
				/* TODO: If anastasia doesn't like trees linked to whizzing labels,
				 * uncomment the following commented code. If she likes it, delete. This
				 * code saves compute cycles, but makes the trees sometimes move out of sync
				 * with the labels. */
//				//Disable the turnOffRepaintTimer if it is running, because
//				//we've already stopped repaints
//				if(slowDownRepaintTimer != null) {
//					slowDownRepaintTimer.stop();
//					slowDownRepaintTimer = null;
//				}
			} else {
				debug("The repaint timer is not running. This updateBuffer " +
					"call was initiated by something else.",9);
			}
		}
		//Else, assume the mouse is hovering, and if the animation is not
		//running, start it up
		else if(!map.isLabelAnimeRunning()) {
			if(repaintTimer == null || !repaintTimer.isRunning()) {
				debug("Hovering across matrix - starting up animation",9);
				repaintTimer.start();
				lastHoverIndex = getPrimaryHoverIndex();
				/* TODO: If anastasia doesn't like trees linked to whizzing labels,
				 * uncomment the following commented code. If she likes it, delete. This
				 * code saves compute cycles, but makes the trees sometimes move out of sync
				 * with the labels. */
//				//Disable any slowDownRepaintTimer that might've been left over
//				if(slowDownRepaintTimer != null) {
//					slowDownRepaintTimer.stop();
//					slowDownRepaintTimer = null;
//				}
			} else {
				debug("The repaint timer was in fact running even though " +
					"map.isLabelAnimeRunning() said it wasn't.",9);
			}
		}
		//Else if the mouse hasn't moved, start the second timer to slow down
		//the first after 1 second (this mitigates delays upon mouse motion
		//after a brief period of no motion)
		else if(map.overALabelPortLinkedView() &&
			getPrimaryHoverIndex() == lastHoverIndex) {
			if(repaintTimer.getDelay() == REPAINT_INTERVAL) {
				debug("Hovering on one spot [" + lastHoverIndex +
				      "] - slowing animation",9);
				/* TODO: If anastasia doesn't like trees linked to whizzing labels,
				 * uncomment the following commented code. If she likes it, delete. This
				 * code saves compute cycles, but makes the trees sometimes move out of sync
				 * with the labels. */
//				if(slowDownRepaintTimer == null) {
//					slowDownRepaintTimer =
//						new Timer(delay,slowDownRepaintListener);
//					slowDownRepaintTimer.start();
//				}
			} else {
				debug("Animation already slowed down to [" +
					repaintTimer.getDelay() + "ms].",9);
			}
		}
		//Else, disable the slowDownRepaintTimer, update the hover index, and
		//set the repaint interval to normal speed
		else {
			debug("Hovering across matrix - keeping animation going",9);
			debug("Last hover Index: [" + lastHoverIndex +
				"] current hover index [" + getPrimaryHoverIndex() + "]",9);
			if(repaintTimer != null && !repaintTimer.isRunning()) {
				repaintTimer.start();
				/* TODO: If anastasia doesn't like trees linked to whizzing labels,
				 * uncomment the following commented code. If she likes it, delete. This
				 * code saves compute cycles, but makes the trees sometimes move out of sync
				 * with the labels. */
//			} else if(repaintTimer.getDelay() == slowRepaintInterval) {
//				debug("Speeding up the repaint interval because mouse " +
//				      "movement detected",9);
//				repaintTimer.setDelay(REPAINT_INTERVAL);
//				repaintTimer.restart();
			}
//			//Disable the slowDownRepaintTimer because we have detected
//			//continued mouse motion
//			if(slowDownRepaintTimer != null) {
//				slowDownRepaintTimer.stop();
//				slowDownRepaintTimer = null;
//			}
			lastHoverIndex = getPrimaryHoverIndex();
		}
	}

	/**
	 * Gets the data index that is hovered over
	 * @author rleach
	 * @return data index
	 */
	public int getPrimaryHoverIndex() {
		return(map.getHoverIndex());
	}

	/**
	 * When a mouse is clicked, a node is selected.
	 */
	@Override
	public void mouseClicked(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive())
			return;
		if (treePainter == null)
			return;

		if (SwingUtilities.isLeftMouseButton(e)) {
			setSelectedNode(getClosestParentNode(e));
		} else {
			treeSelection.deselectAllIndexes();
			treeSelection.notifyObservers();
		}
	}

	@Override
	public void mouseMoved(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive())
			return;
		if (treePainter == null)
			return;

		setHoveredNode(getClosestParentNode(e));
		map.setHoverPixel(getPrimaryPixelIndex(e));
		map.setHoverIndex(map.getIndex(getPrimaryPixelIndex(e)));
		synchMap();
	}

	@Override
	public void mouseDragged(final MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseEntered(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive())
			return;

		if(map.wasLastTreeModeGlobal()) {
			map.setKeepTreeGlobal(true);
		}
		map.setOverTree(true);
	}

	@Override
	public void mouseExited(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive())
			return;

		map.setOverTree(false);
		unsetHoveredNode();
	}

	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {

		final int notches = e.getWheelRotation();
		int shift = (notches < 0) ? -6 : 6;

		// On macs' magic mouse, horizontal scroll comes in as if the shift was
		// down
		if(isAPrimaryScroll(e)) {
			//Value of label length scrollbar
			map.scrollBy(shift);
			updatePrimaryHoverIndexDuringScrollWheel();
		}
	}

	protected abstract boolean isAPrimaryScroll(final MouseWheelEvent e);

	public void updatePrimaryHoverIndexDuringScrollWheel() {
		if(map.getHoverPixel() == -1) {
			unsetPrimaryHoverIndex();
		} else {
			setPrimaryHoverIndex(map.getIndex(map.getHoverPixel()));
		}
	}

	/**
	 * This setter allows a hover index to be manually set in cases where the
	 * cursor is for example, over the secondary scroll bar, drag-selecting off
	 * the edge of the matrix, or dragging the opposing labels' secondary
	 * scrollbar
	 * @param i
	 */
	public void setPrimaryHoverIndex(final int i) {
		map.setHoverIndex(i);
	}

	/**
	 * Sets the hover index to -1
	 * @author rleach
	 */
	public void unsetPrimaryHoverIndex() {
		map.unsetHoverIndex();
	}
	
	public BufferedImage getSnapshot(final int width, final int height) {
	
		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		Rectangle dest = new Rectangle(width, height);
		
		BufferedImage scaled = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		
		setExportScale(dest);
		
		/* Draw trees to first image, original size */
		treePainter.paint(img.getGraphics(), xScaleEq, yScaleEq, dest, 
				isLeft, -1, null, null);
		
		/* Draw a scaled version of the old image to a new image */
		Graphics g = scaled.getGraphics();
		g.drawImage(img, 0, 0, width, height, null);
		
		return scaled;
	}
}
