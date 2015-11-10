package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LinearTransformation;
import edu.stanford.genetics.treeview.ModelViewBuffered;
import edu.stanford.genetics.treeview.TreeDrawerNode;
import edu.stanford.genetics.treeview.TreeSelectionI;

public class TRView extends ModelViewBuffered implements KeyListener {

	private static final long serialVersionUID = 1L;

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

	public TRView(final boolean isGeneTree) {

		super();
		
		setLayout(new MigLayout());

		final String summary = (isGeneTree) ? "GtrSummary" : "AtrSummary";
		this.headerSummary = new HeaderSummary(summary);
		
//		panel = new JPanel();
		
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

//		panel.setLayout(new BorderLayout());
//		panel.add(this, BorderLayout.CENTER);
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
	 * make sure the selected array range reflects the selected node, if any.
	 */
	private void synchMap() {

		if ((selectedNode != null) && (treeSelection != null)) {// &&
			// treeSelection.getNSelectedIndexes() == 1) {
			final int start = (int) (selectedNode.getLeftLeaf().getIndex());
			final int end = (int) (selectedNode.getRightLeaf().getIndex());

			treeSelection.deselectAllIndexes();
			treeSelection.setSelectedNode(selectedNode.getId());
			treeSelection.selectIndexRange(start, end);
			treeSelection.notifyObservers();
		}

		// if ((status != null) && hasMouse) {
		// status.setMessages(getStatus());
		// }
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

		/* deselecting previously selected node -- painting it black again */
		if (selectedNode != null) {
			paintNode(selectedNode, false);
		}

		selectedNode = n;

		/* paint the selected node and its children */
		if (selectedNode != null) {
			paintNode(selectedNode, true);
		}

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

		if (hoveredNode == n)
			return;

		/* painting old hovered node black again */
		paintNode(hoveredNode, false);

		/* keep selected node colored while hovering */
		if (selectedNode != null) {
			paintNode(selectedNode, true);
		}

		hoveredNode = n;

		/* paint the hovered node and its children */
		paintNode(hoveredNode, true);

		synchMap();
		repaint();
	}

	public void repaintHoveredNode() {
		if(hoveredNode == null) {
			return;
		}

		/* paint the hovered node and its children */
		paintNode(hoveredNode, true);
//
//		synchMap();
//		repaint();
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
	private void paintNode(final TreeDrawerNode node, final boolean isColored) {

		if (xScaleEq != null) {
			treePainter.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
					destRect, node, isColored, isLeft);
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
				if (treeSelection.getMinIndex() == treeSelection.getMaxIndex()) {
					cand = treePainter.getLeaf(treeSelection.getMinIndex());
				}
				// this clause selects the root node if all arrays are selected.
				else if (treeSelection.getMinIndex() == map.getMinIndex()
						&& treeSelection.getMaxIndex() == map.getMaxIndex() &&
						// All the intervening rows/cols are selected
						(treeSelection.getMaxIndex()
								- treeSelection.getMinIndex() + 1) == treeSelection
								.getNSelectedIndexes()) {
					cand = treePainter.getRootNode();

				} else if (treeSelection.getMinIndex() >= map.getMinIndex()
						&& treeSelection.getMaxIndex() <= map.getMaxIndex() &&
						// All the intervening rows/cols are selected
						(treeSelection.getMaxIndex()
								- treeSelection.getMinIndex() + 1) == treeSelection
								.getNSelectedIndexes()) {
					cand = treePainter.getNearestNode(
							treeSelection.getMinIndex(),
							treeSelection.getMaxIndex());
					// If no candidate was found or the candidate is not an
					// exact match
					if (cand == null
							|| ((int) Math.round(cand.getMinIndex()) != treeSelection
									.getMinIndex() || (int) Math.round(cand
									.getMaxIndex()) != treeSelection
									.getMaxIndex())) {
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

		// if (offscreenChanged) {
		// offscreenValid = false;
		// }
		//
		// if ((!offscreenValid) && (drawer != null)) {
		// map.setAvailablePixels(offscreenSize.width);
		//
		// // clear the pallette...
		// g.setColor(this.getBackground());
		// g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
		// g.setColor(Color.black);
		//
		// // calculate Scaling
		// destRect.setBounds(0, 0, map.getUsedPixels(), offscreenSize.height);
		// xScaleEq = new LinearTransformation(map.getIndex(destRect.x),
		// destRect.x, map.getIndex(destRect.x + destRect.width),
		// destRect.x + destRect.width);
		// yScaleEq = new LinearTransformation(drawer.getCorrMin(),
		// destRect.y, drawer.getCorrMax(), destRect.y
		// + destRect.height);
		//
		// // draw
		// drawer.paint(g, xScaleEq, yScaleEq, destRect, selectedNode, isLeft);
		//
		// } else {
		// // System.out.println("didn't update buffer: valid =
		// // " + offscreenValid + " drawer = " + drawer);
		// }
	}

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
	 * Arrow keys are used to change the selected node.
	 *
	 * up selects parent of current. left selects left child. right selects
	 * right child down selects child with most descendants.
	 *
	 */
	@Override
	public void keyPressed(final KeyEvent e) {

		if (selectedNode == null)
			return;

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

		treePainter.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
				destRect, current, true, isLeft);
		treePainter.paintSingle(offscreenGraphics, xScaleEq, yScaleEq,
				destRect, selectedNode, true, isLeft);

		synchMap();
		repaint();
	}

	private void selectRight() {

		if (selectedNode.isLeaf())
			return;

		final TreeDrawerNode current = selectedNode;
		selectedNode = current.getRight();

		treePainter.paintSingle(offscreenGraphics, xScaleEq, yScaleEq,
				destRect, current, false, isLeft);
		treePainter.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
				destRect, current.getLeft(), false, isLeft);

		synchMap();
		repaint();
	}

	private void selectLeft() {

		if (selectedNode.isLeaf())
			return;

		final TreeDrawerNode current = selectedNode;
		selectedNode = current.getLeft();

		treePainter.paintSingle(offscreenGraphics, xScaleEq, yScaleEq,
				destRect, current, false, isLeft);
		treePainter.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
				destRect, current.getRight(), false, isLeft);

		synchMap();
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
}
