///* BEGIN_HEADER                                                   TreeView 3
// *
// * Please refer to our LICENSE file if you wish to make changes to this software
// *
// * END_HEADER 
// */

package edu.stanford.genetics.treeview.plugin.dendroview;

//package edu.stanford.genetics.treeview.plugin.dendroview;
//
//import java.awt.Adjustable;
//import java.awt.BorderLayout;
//import java.awt.Color;
//import java.awt.Graphics;
//import java.awt.Rectangle;
//import java.awt.event.AdjustmentEvent;
//import java.awt.event.KeyEvent;
//import java.awt.event.KeyListener;
//import java.awt.event.MouseEvent;
//import java.awt.event.MouseListener;
//import java.awt.event.MouseMotionListener;
//import java.util.Observable;
//
//import javax.swing.JPanel;
//import javax.swing.JScrollBar;
//import javax.swing.SwingUtilities;
//
//import edu.stanford.genetics.treeview.HeaderInfo;
//import edu.stanford.genetics.treeview.HeaderSummary;
//import edu.stanford.genetics.treeview.LinearTransformation;
//import edu.stanford.genetics.treeview.ModelViewBuffered;
//import edu.stanford.genetics.treeview.TreeDrawerNode;
//import edu.stanford.genetics.treeview.TreeSelectionI;
//
///**
// * Draws an array tree to show the relations between arrays. This object
// * requires a MapContainer to figure out the offsets for the arrays.
// * Furthermore, it sets up a scrollbar to scroll the tree, although there is
// * currently no way to specify how large you would like the scrollable area to
// * be.
// * 
// * @author Alok Saldanha <alok@genome.stanford.edu>
// * @version $Revision: 1.2 $ $Date: 2010-05-02 13:39:00 $
// */
//public class ATRView extends ModelViewBuffered implements MouseListener, 
//		MouseMotionListener, KeyListener {
//
//	private static final long serialVersionUID = 1L;
//
//	protected HeaderSummary headerSummary = new HeaderSummary("AtrSummary");
//
//	private HeaderInfo atrHI;
//	private TreeSelectionI arraySelection;
//	private LinearTransformation xScaleEq, yScaleEq;
//	private MapContainer map;
//	private final JScrollBar scrollbar;
//
//	private TreePainter drawer = null;
//	private TreeDrawerNode selectedNode = null;
//	private TreeDrawerNode hoveredNode = null;
//	private Rectangle destRect = null;
//
//	private final boolean ISLEFT = false;
//
//	/** Constructor, sets up AWT components */
//	public ATRView() {
//
//		super();
//
//		panel = new JPanel();
//		scrollbar = new JScrollBar(Adjustable.VERTICAL, 0, 1, 0, 1);
//		destRect = new Rectangle();
//
//		panel.setLayout(new BorderLayout());
//		panel.add(this, BorderLayout.CENTER);
//		// EDIT
//		panel.add(scrollbar, BorderLayout.NORTH);
//
//		addMouseListener(this);
//		addMouseMotionListener(this);
//		addKeyListener(this);
//	}
//
//	/**
//	 * Set the selected node and redraw
//	 * 
//	 * @param n
//	 *            The new node to be selected Does nothing if the node is
//	 *            already selected.
//	 */
//	public void setSelectedNode(final TreeDrawerNode n) {
//		
//		setHoveredNode(null);
//
//		if (selectedNode == n) {
//			return;
//		}
//		if (selectedNode != null) {
//			drawer.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
//					destRect, selectedNode, false, ISLEFT);
//		}
//
//		selectedNode = n;
//
//		if (selectedNode != null) {
//			if (xScaleEq != null) {
//				drawer.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
//						destRect, selectedNode, true, ISLEFT);
//			}
//		}
//
//		if ((status != null) && hasMouse) {
//			status.setMessages(getStatus());
//		}
//		
//		synchMap();
//		repaint();
//	}
//	
//	public void setHoveredNode(final TreeDrawerNode n) {
//
//		if (hoveredNode == n) {
//			return;
//		}
//		
//		if (hoveredNode != null) {
//			drawer.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
//					destRect, hoveredNode, false, ISLEFT);
//		}
//
//		hoveredNode = n;
//
//		if (hoveredNode != null) {
//			if (xScaleEq != null) {
//				drawer.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
//						destRect, hoveredNode, true, ISLEFT);
//			}
//		}
//		
//		synchMap();
//		repaint();
//	}
//
//	/**
//	 * make sure the selected array range reflects the selected node, if any.
//	 */
//	private void synchMap() {
//
//		if ((selectedNode != null) && (arraySelection != null)) {
//			final int start = (int) (selectedNode.getLeftLeaf().getIndex());
//			final int end = (int) (selectedNode.getRightLeaf().getIndex());
//
//			arraySelection.deselectAllIndexes();
//			arraySelection.setSelectedNode(selectedNode.getId());
//			arraySelection.selectIndexRange(start, end);
//			arraySelection.notifyObservers();
//			arraySelection.notifyObservers();
//		}
//
//		if ((status != null) && hasMouse) {
//			status.setMessages(getStatus());
//		}
//	}
//
//	/**
//	 * Set <code>TreeSelection</code> object which coordinates the shared
//	 * selection state.
//	 * 
//	 * @param arraySelection
//	 *            The <code>TreeSelection</code> which is set by selecting
//	 *            arrays in the </code>GlobalView</code>
//	 */
//	public void setArraySelection(final TreeSelectionI arraySelection) {
//
//		if (this.arraySelection != null) {
//			this.arraySelection.deleteObserver(this);
//		}
//
//		this.arraySelection = arraySelection;
//		this.arraySelection.addObserver(this);
//	}
//
//	/**
//	 * Set the drawer
//	 * 
//	 * @param d
//	 *            The new drawer
//	 */
//	public void setInvertedTreeDrawer(final TreePainter d) {
//
//		if (drawer != null) {
//			drawer.deleteObserver(this);
//		}
//
//		drawer = d;
//		drawer.addObserver(this);
//	}
//
//	/**
//	 * Set the map. For the ATRView, this determines where the leaves of the
//	 * tree will be.
//	 * 
//	 * @param m
//	 *            The new map to be used for determining the spacing between
//	 *            indexes.
//	 */
//	public void setMap(final MapContainer m) {
//
//		if (map != null) {
//			map.deleteObserver(this);
//		}
//
//		map = m;
//		map.addObserver(this);
//	}
//
//	/**
//	 * expect updates to come from map, arraySelection and drawer
//	 * 
//	 * @param o
//	 *            The observable which sent the update
//	 * @param arg
//	 *            Argument for this update, typically null.
//	 */
//	@Override
//	public void update(final Observable o, final Object arg) {
//
//		if (!isEnabled()) {
//			return;
//		}
//
//		if (o == map) {
//			// System.out.println("Got an update from map");
//			offscreenValid = false;
//			repaint();
//
//		} else if (o == drawer) {
//			// System.out.println("Got an update from drawer");
//			offscreenValid = false;
//			repaint();
//
//		} else if (o == arraySelection) {
//			// LogBuffer.println("got update from arraySelection "+o );
//			TreeDrawerNode cand = null;
//			if (arraySelection.getNSelectedIndexes() > 0) {
//				// This clause selects the array node if only a
//				// single array is selected.
//				if (arraySelection.getMinIndex() == arraySelection
//						.getMaxIndex()) {
//					cand = drawer.getLeaf(arraySelection.getMinIndex());
//				}
//				// this clause selects the root node if all arrays are selected.
//				else if (arraySelection.getMinIndex() == map.getMinIndex()
//						&& arraySelection.getMaxIndex() == map.getMaxIndex()) {
//					cand = drawer.getRootNode();
//
//				}
//				// // find node when multiple arrays are selected.
//				// } else if(arraySelection.getMinIndex() >= map.getMinIndex()
//				// && arraySelection.getMaxIndex() <= map.getMaxIndex()) {
//				// cand = drawer.getNearestNode(arraySelection.getMinIndex(),
//				// arraySelection.getMaxIndex());
//				// }
//			}
//			// Only notify observers if we're changing the selected node.
//			if ((cand != null)
//					&& !(cand.getId().equalsIgnoreCase(arraySelection
//							.getSelectedNode()))) {
//				arraySelection.setSelectedNode(cand.getId());
//				arraySelection.notifyObservers();
//
//			} else {
//				setSelectedNode(drawer.getNodeById(arraySelection
//						.getSelectedNode()));
//			}
//		} else {
//			System.out.println(viewName() + "Got an update from unknown " + o);
//		}
//
//		revalidate();
//		repaint();
//	}
//
//	/**
//	 * Need to blit another part of the buffer to the screen when the scrollbar
//	 * moves.
//	 * 
//	 * @param evt
//	 *            The adjustment event generated by the scrollbar
//	 */
//	public void adjustmentValueChanged(final AdjustmentEvent evt) {
//
//		repaint();
//	}
//
//	// method from ModelView
//	/**
//	 * Implements abstract method from ModelView. In this case, returns
//	 * "ATRView".
//	 * 
//	 * @return name of this subclass of modelview
//	 */
//	@Override
//	public String viewName() {
//
//		return "ATRView";
//	}
//
//	// method from ModelView
//	/**
//	 * Gets the status attribute of the ATRView object. The status is some
//	 * information which the user might find useful.
//	 * 
//	 * @return The status value
//	 */
//	@Override
//	public String[] getStatus() {
//
//		String[] status;
//		if (selectedNode != null) {
//			if (selectedNode.isLeaf()) {
//				status = new String[2];
//				status[0] = "Leaf Node " + selectedNode.getId();
//				status[1] = "Pos " + selectedNode.getCorr();
//
//			} else {
//				final int[] nameIndex = getHeaderSummary().getIncluded();
//				status = new String[nameIndex.length * 2];
//				final String[] names = atrHI.getNames();
//				for (int i = 0; i < nameIndex.length; i++) {
//					status[2 * i] = names[nameIndex[i]] + ":";
//					status[2 * i + 1] = " "
//							+ atrHI.getHeader(atrHI.getHeaderIndex(
//									selectedNode.getId()))[nameIndex[i]];
//				}
//			}
//		} else {
//			status = new String[2];
//			status[0] = "Select node to ";
//			status[1] = "view annotation.";
//		}
//
//		return status;
//	}
//
//	/** Setter for headerSummary */
//	public void setHeaderSummary(final HeaderSummary headerSummary) {
//
//		this.headerSummary = headerSummary;
//	}
//
//	/** Getter for headerSummary */
//	public HeaderSummary getHeaderSummary() {
//
//		return headerSummary;
//	}
//	
//	public void setATRHeaderInfo(final HeaderInfo atrHI) {
//		
//		this.atrHI = atrHI;
//	}
//
//	/* inherit description */
//	@Override
//	public void updateBuffer(final Graphics g) {
//
//		if (offscreenChanged) {
//			offscreenValid = false;
//		}
//
//		if ((!offscreenValid) && (drawer != null)) {
//			map.setAvailablePixels(offscreenSize.width);
//
//			// clear the pallette...
//			g.setColor(this.getBackground());
//			g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
//			g.setColor(Color.black);
//
//			// calculate Scaling
//			destRect.setBounds(0, 0, map.getUsedPixels(), offscreenSize.height);
//			xScaleEq = new LinearTransformation(map.getIndex(destRect.x),
//					destRect.x, map.getIndex(destRect.x + destRect.width),
//					destRect.x + destRect.width);
//			yScaleEq = new LinearTransformation(drawer.getCorrMin(),
//					destRect.y, drawer.getCorrMax(), destRect.y
//							+ destRect.height);
//
//			// draw
//			drawer.paint(g, xScaleEq, yScaleEq, destRect, selectedNode, ISLEFT);
//
//		} else {
//			// System.out.println("didn't update buffer: valid =
//			// " + offscreenValid + " drawer = " + drawer);
//		}
//	}
//
//	// Mouse Listener
//	/**
//	 * When a mouse is clicked, a node is selected.
//	 */
//	@Override
//	public void mouseClicked(final MouseEvent e) {
//
//		if (!isEnabled()) {
//			return;
//		}
//
//		if (!enclosingWindow().isActive()) {
//			return;
//		}
//
//		if (drawer != null) {
//			
//			if(SwingUtilities.isLeftMouseButton(e)) {
//				// the trick is translating back to the normalized space...
//				setSelectedNode(drawer.getClosest(
//						xScaleEq.inverseTransform(e.getX()),
//						yScaleEq.inverseTransform(e.getY()),
//						// weight must have correlation slope on top
//						yScaleEq.getSlope() / xScaleEq.getSlope()));
//			} else {
//				setSelectedNode(null);
//			}
//		}
//	}
//	
//	@Override
//	public void mouseMoved(final MouseEvent e) {
//
//		if (!isEnabled()) {
//			return;
//		}
//
//		if (!enclosingWindow().isActive()) {
//			return;
//		}
//
//		if (drawer != null && arraySelection.getNSelectedIndexes() == 0) {
//			// the trick is translating back to the normalized space...
//			setHoveredNode(drawer.getClosest(
//					xScaleEq.inverseTransform(e.getX()),
//					yScaleEq.inverseTransform(e.getY()),
//					// weight must have correlation slope on top
//					yScaleEq.getSlope() / xScaleEq.getSlope()));
//		}
//	}
//	
//	@Override
//	public void mouseExited(final MouseEvent e) {
//
//		if (!isEnabled()) {
//			return;
//		}
//
//		if (!enclosingWindow().isActive()) {
//			return;
//		}
//
//		setHoveredNode(null);
//	}
//	
//	
//
//	// method from KeyListener
//	/**
//	 * Arrow keys are used to change the selected node.
//	 * 
//	 * up selects parent of current. left selects left child. right selects
//	 * right child down selects child with most descendants.
//	 * 
//	 */
//	@Override
//	public void keyPressed(final KeyEvent e) {
//
//		if (selectedNode == null) {
//			return;
//		}
//
//		final int c = e.getKeyCode();
//
//		switch (c) {
//		case KeyEvent.VK_UP:
//			selectParent();
//			break;
//
//		case KeyEvent.VK_LEFT:
//			if (!selectedNode.isLeaf()) {
//				selectLeft();
//			}
//			break;
//
//		case KeyEvent.VK_RIGHT:
//			if (!selectedNode.isLeaf()) {
//				selectRight();
//			}
//			break;
//
//		case KeyEvent.VK_DOWN:
//			if (!selectedNode.isLeaf()) {
//				final TreeDrawerNode right = selectedNode.getRight();
//				final TreeDrawerNode left = selectedNode.getLeft();
//
//				if (right.getRange() > left.getRange()) {
//					selectRight();
//
//				} else {
//					selectLeft();
//				}
//			}
//			break;
//		}
//	}
//
//	private void selectParent() {
//
//		TreeDrawerNode current = selectedNode;
//		selectedNode = current.getParent();
//
//		if (selectedNode == null) {
//			selectedNode = current;
//			return;
//		}
//
//		if (current == selectedNode.getLeft()) {
//			current = selectedNode.getRight();
//
//		} else {
//			current = selectedNode.getLeft();
//		}
//
//		drawer.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq, destRect,
//				current, true, ISLEFT);
//		drawer.paintSingle(offscreenGraphics, xScaleEq, yScaleEq, destRect,
//				selectedNode, true, ISLEFT);
//
//		synchMap();
//		repaint();
//	}
//
//	private void selectRight() {
//
//		if (selectedNode.isLeaf()) {
//			return;
//		}
//
//		final TreeDrawerNode current = selectedNode;
//		selectedNode = current.getRight();
//
//		drawer.paintSingle(offscreenGraphics, xScaleEq, yScaleEq, destRect,
//				current, false, ISLEFT);
//		drawer.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq, destRect,
//				current.getLeft(), false, ISLEFT);
//
//		synchMap();
//		repaint();
//	}
//
//	private void selectLeft() {
//
//		if (selectedNode.isLeaf()) {
//			return;
//		}
//
//		final TreeDrawerNode current = selectedNode;
//		selectedNode = current.getLeft();
//
//		drawer.paintSingle(offscreenGraphics, xScaleEq, yScaleEq, destRect,
//				current, false, ISLEFT);
//		drawer.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq, destRect,
//				current.getRight(), false, ISLEFT);
//
//		synchMap();
//		repaint();
//	}
//
//	/**
//	 * @param nodeName
//	 */
//	public void scrollToNode(final String nodeName) {
//
//		final TreeDrawerNode node = drawer.getNodeById(nodeName);
//		if (node != null) {
//			final int index = (int) node.getIndex();
//			if (!map.isVisible(index)) {
//				map.scrollToIndex(index);
//				map.notifyObservers();
//			}
//		}
//	}
//
//	/**
//	 * Key releases are ignored.
//	 * 
//	 * @param e
//	 *            The keyevent
//	 */
//	@Override
//	public void keyReleased(final KeyEvent e) {
//	}
//
//	/**
//	 * Key types are ignored.
//	 * 
//	 * @param e
//	 *            the keypress.
//	 */
//	@Override
//	public void keyTyped(final KeyEvent e) {
//	}
//}
