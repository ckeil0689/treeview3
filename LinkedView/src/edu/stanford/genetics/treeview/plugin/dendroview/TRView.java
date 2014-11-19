package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Observable;

import javax.swing.JPanel;

import edu.stanford.genetics.treeview.LinearTransformation;
import edu.stanford.genetics.treeview.ModelViewBuffered;
import edu.stanford.genetics.treeview.TreeDrawerNode;
import edu.stanford.genetics.treeview.TreeSelectionI;

public class TRView extends ModelViewBuffered implements KeyListener {
	
	private static final long serialVersionUID = 1L;

	protected TreeSelectionI treeSelection;
	protected LinearTransformation xScaleEq, yScaleEq;
	protected MapContainer map;

	protected TreePainter treePainter = null;
	protected TreeDrawerNode selectedNode = null;
	protected TreeDrawerNode hoveredNode = null;
	protected Rectangle destRect = null;

	protected boolean isLeft;
	
	/** Constructor, sets up AWT components */
	public TRView() {

		super();

		panel = new JPanel();
		destRect = new Rectangle();

		panel.setLayout(new BorderLayout());
		panel.add(this, BorderLayout.CENTER);
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

		if ((selectedNode != null) && (treeSelection != null)) {
			final int start = (int) (selectedNode.getLeftLeaf().getIndex());
			final int end = (int) (selectedNode.getRightLeaf().getIndex());

			treeSelection.deselectAllIndexes();
			treeSelection.setSelectedNode(selectedNode.getId());
			treeSelection.selectIndexRange(start, end);
			treeSelection.notifyObservers();
		}

//		if ((status != null) && hasMouse) {
//			status.setMessages(getStatus());
//		}
	}
	
	/**
	 * Set <code>TreeSelection</code> object which coordinates the shared
	 * selection state.
	 * 
	 * @param treeSelection
	 *            The <code>TreeSelection</code> which is set by selecting
	 *            rows or columns in the </code>GlobalView</code>
	 */
	public void setTreeSelection(final TreeSelectionI treeSelection) {

		if (this.treeSelection != null) {
			this.treeSelection.deleteObserver(this);
		}

		this.treeSelection = treeSelection;
		this.treeSelection.addObserver(this);
	}
	
	/**
	 * Set the selected node and redraw
	 * 
	 * @param n
	 *            The new node to be selected Does nothing if the node is
	 *            already selected.
	 */
	public void setSelectedNode(final TreeDrawerNode n) {
		
		setHoveredNode(null);

		if (selectedNode == n) {
			return;
		}
		if (selectedNode != null) {
			treePainter.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
					destRect, selectedNode, false, isLeft);
		}

		selectedNode = n;

		if (selectedNode != null) {
			if (xScaleEq != null) {
				treePainter.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
						destRect, selectedNode, true, isLeft);
			}
		}

//		if ((status != null) && hasMouse) {
//			status.setMessages(getStatus());
//		}
		
		synchMap();
		repaint();
	}
	
	public void setHoveredNode(final TreeDrawerNode n) {

		if (hoveredNode == n) {
			return;
		}
		
		if (hoveredNode != null) {
			treePainter.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
					destRect, hoveredNode, false, isLeft);
		}

		hoveredNode = n;

		if (hoveredNode != null) {
			if (xScaleEq != null) {
				treePainter.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
						destRect, hoveredNode, true, isLeft);
			}
		}
		
		synchMap();
		repaint();
	}

	@Override
	public void update(Observable o, Object arg) {
		
		if (!isEnabled()) {
			return;
		}

		if (o == map) {
			// System.out.println("Got an update from map");
			offscreenValid = false;
			repaint();

		} else if (o == treePainter) {
			// System.out.println("Got an update from drawer");
			offscreenValid = false;
			repaint();

		} else if (o == treeSelection) {
			// LogBuffer.println("got update from arraySelection "+o );
			TreeDrawerNode cand = null;
			if (treeSelection.getNSelectedIndexes() > 0) {
				// This clause selects the array node if only a
				// single array is selected.
				if (treeSelection.getMinIndex() == treeSelection
						.getMaxIndex()) {
					cand = treePainter.getLeaf(treeSelection.getMinIndex());
				}
				// this clause selects the root node if all arrays are selected.
				else if (treeSelection.getMinIndex() == map.getMinIndex()
						&& treeSelection.getMaxIndex() == map.getMaxIndex()) {
					cand = treePainter.getRootNode();

				}
				// // find node when multiple arrays are selected.
//				 else if(treeSelection.getMinIndex() >= map.getMinIndex()
//				 && treeSelection.getMaxIndex() <= map.getMaxIndex()) {
//					 cand = treePainter.getNearestNode(treeSelection.getMinIndex(),
//							 treeSelection.getMaxIndex());
//				 }
			}
			// Only notify observers if we're changing the selected node.
			if ((cand != null)
					&& !(cand.getId().equalsIgnoreCase(treeSelection
							.getSelectedNode()))) {
				treeSelection.setSelectedNode(cand.getId());
				treeSelection.notifyObservers();

			} else {
				setSelectedNode(treePainter.getNodeById(treeSelection
						.getSelectedNode()));
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
//			drawer.paint(g, xScaleEq, yScaleEq, destRect, selectedNode, isLeft);
//
//		} else {
//			// System.out.println("didn't update buffer: valid =
//			// " + offscreenValid + " drawer = " + drawer);
//		}
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

	// method from KeyListener
	/**
	 * Arrow keys are used to change the selected node.
	 * 
	 * up selects parent of current. left selects left child. right selects
	 * right child down selects child with most descendants.
	 * 
	 */
	@Override
	public void keyPressed(final KeyEvent e) {

		if (selectedNode == null) {
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
		}
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

		if (selectedNode.isLeaf()) {
			return;
		}

		final TreeDrawerNode current = selectedNode;
		selectedNode = current.getRight();

		treePainter.paintSingle(offscreenGraphics, xScaleEq, yScaleEq, destRect,
				current, false, isLeft);
		treePainter.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq, destRect,
				current.getLeft(), false, isLeft);

		synchMap();
		repaint();
	}
	
	

	private void selectLeft() {

		if (selectedNode.isLeaf()) {
			return;
		}

		final TreeDrawerNode current = selectedNode;
		selectedNode = current.getLeft();

		treePainter.paintSingle(offscreenGraphics, xScaleEq, yScaleEq, destRect,
				current, false, isLeft);
		treePainter.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq, destRect,
				current.getRight(), false, isLeft);

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
