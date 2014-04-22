/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: ATRView.java,v $
 * $Revision: 1.2 $
 * $Date: 2010-05-02 13:39:00 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by 
 * Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular,
 *
 * 1) If you modify a source file, make a comment in it containing your name 
 * and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the 
 * Java TreeView maintainers at alok@genome.stanford.edu when they make a 
 * useful addition. It would be nice if significant contributions could be 
 * merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER
 */
package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Observable;

import javax.swing.JPanel;
import javax.swing.JScrollBar;

import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LinearTransformation;
import edu.stanford.genetics.treeview.ModelViewBuffered;
import edu.stanford.genetics.treeview.TreeDrawerNode;
import edu.stanford.genetics.treeview.TreeSelectionI;

/**
 * Draws an array tree to show the relations between arrays. This object
 * requires a MapContainer to figure out the offsets for the arrays.
 * Furthermore, it sets up a scrollbar to scroll the tree, although there is
 * currently no way to specify how large you would like the scrollable area to
 * be.
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.2 $ $Date: 2010-05-02 13:39:00 $
 */
public class ATRView extends ModelViewBuffered implements MouseListener,
		KeyListener {

	private static final long serialVersionUID = 1L;

	protected HeaderSummary headerSummary = new HeaderSummary("AtrSummary");

	private TreeSelectionI arraySelection;
	private LinearTransformation xScaleEq, yScaleEq;
	private MapContainer map;
	private final JScrollBar scrollbar;

	private InvertedTreeDrawer drawer = null;
	private TreeDrawerNode selectedNode = null;
	private Rectangle destRect = null;

	/** Constructor, sets up AWT components */
	public ATRView() {

		super();

		panel = new JPanel();
		scrollbar = new JScrollBar(Adjustable.VERTICAL, 0, 1, 0, 1);
		destRect = new Rectangle();

		panel.setLayout(new BorderLayout());
		panel.add(this, BorderLayout.CENTER);
		// EDIT
		panel.add(scrollbar, BorderLayout.NORTH);

		addMouseListener(this);
		addKeyListener(this);
	}

	/**
	 * Set the selected node and redraw
	 * 
	 * @param n
	 *            The new node to be selected Does nothing if the node is
	 *            already selected.
	 */
	public void setSelectedNode(final TreeDrawerNode n) {

		if (selectedNode == n) {
			return;
		}
		if (selectedNode != null) {
			drawer.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
					destRect, selectedNode, false);
		}
		selectedNode = n;
		if (selectedNode != null) {
			if (xScaleEq != null) {
				drawer.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq,
						destRect, selectedNode, true);
			}
		}

		if ((status != null) && hasMouse) {
			status.setMessages(getStatus());
		}
		synchMap();
		repaint();
	}

	/**
	 * make sure the selected array range reflects the selected node, if any.
	 */
	private void synchMap() {

		if ((selectedNode != null) && (arraySelection != null)) {
			final int start = (int) (selectedNode.getLeftLeaf().getIndex());
			final int end = (int) (selectedNode.getRightLeaf().getIndex());

			arraySelection.deselectAllIndexes();
			arraySelection.setSelectedNode(selectedNode.getId());
			arraySelection.selectIndexRange(start, end);
			arraySelection.notifyObservers();
		}

		if ((status != null) && hasMouse) {
			status.setMessages(getStatus());
		}
	}

	/**
	 * Set <code>TreeSelection</code> object which coordinates the shared
	 * selection state.
	 * 
	 * @param arraySelection
	 *            The <code>TreeSelection</code> which is set by selecting
	 *            arrays in the </code>GlobalView</code>
	 */
	public void setArraySelection(final TreeSelectionI arraySelection) {

		if (this.arraySelection != null) {
			this.arraySelection.deleteObserver(this);
		}

		this.arraySelection = arraySelection;
		this.arraySelection.addObserver(this);
	}

	/**
	 * Set the drawer
	 * 
	 * @param d
	 *            The new drawer
	 */
	public void setInvertedTreeDrawer(final InvertedTreeDrawer d) {

		if (drawer != null) {
			drawer.deleteObserver(this);
		}

		drawer = d;
		drawer.addObserver(this);
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
	}

	/**
	 * expect updates to come from map, arraySelection and drawer
	 * 
	 * @param o
	 *            The observable which sent the update
	 * @param arg
	 *            Argument for this update, typically null.
	 */
	@Override
	public void update(final Observable o, final Object arg) {

		if (!isEnabled()) {
			return;
		}

		if (o == map) {
			// System.out.println("Got an update from map");
			offscreenValid = false;
			repaint();

		} else if (o == drawer) {
			// System.out.println("Got an update from drawer");
			offscreenValid = false;
			repaint();

		} else if (o == arraySelection) {
			// LogBuffer.println("got update from arraySelection "+o );
			TreeDrawerNode cand = null;
			if (arraySelection.getNSelectedIndexes() > 0) {
				// This clause selects the array node if only a
				// single array is selected.
				if (arraySelection.getMinIndex() == arraySelection
						.getMaxIndex()) {
					cand = drawer.getLeaf(arraySelection.getMinIndex());
				}
				// this clause selects the root node if all arrays are selected.
				if (arraySelection.getMinIndex() == map.getMinIndex()) {
					if (arraySelection.getMaxIndex() == map.getMaxIndex()) {
						cand = drawer.getRootNode();
					}
				}
			}
			// Only notify observers if we're changing the selected node.
			if ((cand != null)
					&& !(cand.getId().equalsIgnoreCase(
							arraySelection.getSelectedNode()))) {
				arraySelection.setSelectedNode(cand.getId());
				arraySelection.notifyObservers();

			} else {
				setSelectedNode(drawer.getNodeById(arraySelection
						.getSelectedNode()));
			}
		} else {
			System.out.println(viewName() + "Got an update from unknown " + o);
		}

		this.revalidate();
		this.repaint();
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

	// method from ModelView
	/**
	 * Implements abstract method from ModelView. In this case, returns
	 * "ATRView".
	 * 
	 * @return name of this subclass of modelview
	 */
	@Override
	public String viewName() {

		return "ATRView";
	}

	// method from ModelView
	/**
	 * Gets the status attribute of the ATRView object. The status is some
	 * information which the user might find useful.
	 * 
	 * @return The status value
	 */
	@Override
	public String[] getStatus() {

		String[] status;
		if (selectedNode != null) {
			if (selectedNode.isLeaf()) {
				status = new String[2];
				status[0] = "Leaf Node " + selectedNode.getId();
				status[1] = "Pos " + selectedNode.getCorr();

			} else {
				final int[] nameIndex = getHeaderSummary().getIncluded();
				status = new String[nameIndex.length * 2];
				final HeaderInfo atrInfo = getViewFrame().getDataModel()
						.getAtrHeaderInfo();
				final String[] names = atrInfo.getNames();
				for (int i = 0; i < nameIndex.length; i++) {
					status[2 * i] = names[nameIndex[i]] + ":";
					status[2 * i + 1] = " "
							+ atrInfo.getHeader(atrInfo
									.getHeaderIndex(selectedNode.getId()))[nameIndex[i]];
				}
			}
		} else {
			status = new String[2];
			status[0] = "Select node to ";
			status[1] = "view annotation.";
		}

		return status;
	}

	/** Setter for headerSummary */
	public void setHeaderSummary(final HeaderSummary headerSummary) {

		this.headerSummary = headerSummary;
	}

	/** Getter for headerSummary */
	public HeaderSummary getHeaderSummary() {

		return headerSummary;
	}

	/* inherit description */
	@Override
	public void updateBuffer(final Graphics g) {

		if (offscreenChanged) {
			offscreenValid = false;
		}

		if ((!offscreenValid) && (drawer != null)) {
			map.setAvailablePixels(offscreenSize.width);

			// clear the pallette...
			g.setColor(GUIParams.BG_COLOR);
			g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
			g.setColor(Color.black);

			// calculate Scaling
			destRect.setBounds(0, 0, map.getUsedPixels(), offscreenSize.height);
			xScaleEq = new LinearTransformation(map.getIndex(destRect.x),
					destRect.x, map.getIndex(destRect.x + destRect.width),
					destRect.x + destRect.width);
			yScaleEq = new LinearTransformation(drawer.getCorrMin(),
					destRect.y, drawer.getCorrMax(), destRect.y
							+ destRect.height);

			// draw
			drawer.paint(g, xScaleEq, yScaleEq, destRect, selectedNode);

		} else {
			// System.out.println("didn't update buffer: valid =
			// " + offscreenValid + " drawer = " + drawer);
		}
	}

	// Mouse Listener
	/**
	 * When a mouse is clicked, a node is selected.
	 */
	@Override
	public void mouseClicked(final MouseEvent e) {

		if (!isEnabled()) {
			return;
		}

		if (this == null) {
			return;
		}

		if (!enclosingWindow().isActive()) {
			return;
		}

		if (drawer != null) {
			// the trick is translating back to the normalized space...
			setSelectedNode(drawer.getClosest(
					xScaleEq.inverseTransform(e.getX()),
					yScaleEq.inverseTransform(e.getY()),
					// weight must have correlation slope on top
					yScaleEq.getSlope() / xScaleEq.getSlope()));
		}
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

		drawer.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq, destRect,
				current, true);
		drawer.paintSingle(offscreenGraphics, xScaleEq, yScaleEq, destRect,
				selectedNode, true);

		synchMap();
		repaint();
	}

	private void selectRight() {

		if (selectedNode.isLeaf()) {
			return;
		}

		final TreeDrawerNode current = selectedNode;
		selectedNode = current.getRight();

		drawer.paintSingle(offscreenGraphics, xScaleEq, yScaleEq, destRect,
				current, false);
		drawer.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq, destRect,
				current.getLeft(), false);

		synchMap();
		repaint();
	}

	private void selectLeft() {

		if (selectedNode.isLeaf()) {
			return;
		}

		final TreeDrawerNode current = selectedNode;
		selectedNode = current.getLeft();

		drawer.paintSingle(offscreenGraphics, xScaleEq, yScaleEq, destRect,
				current, false);
		drawer.paintSubtree(offscreenGraphics, xScaleEq, yScaleEq, destRect,
				current.getRight(), false);

		synchMap();
		repaint();
	}
	
	/**
	 * @param nodeName
	 */
	public void scrollToNode(final String nodeName) {

		final TreeDrawerNode node = drawer.getNodeById(nodeName);
		if (node != null) {
			final int index = (int) node.getIndex();
			if (!map.isVisible(index)) {
				map.scrollToIndex(index);
				map.notifyObservers();
			}
		}
	}

	/**
	 * Key releases are ignored.
	 * 
	 * @param e
	 *            The keyevent
	 */
	@Override
	public void keyReleased(final KeyEvent e) {
	}

	/**
	 * Key types are ignored.
	 * 
	 * @param e
	 *            the keypress.
	 */
	@Override
	public void keyTyped(final KeyEvent e) {
	}
}
