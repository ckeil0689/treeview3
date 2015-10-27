///* BEGIN_HEADER                                                   TreeView 3
//*
//* Please refer to our LICENSE file if you wish to make changes to this software
//*
//* END_HEADER 
//*/

package edu.stanford.genetics.treeview.plugin.dendroview;

//package edu.stanford.genetics.treeview.plugin.dendroview;
//
//import java.awt.Color;
//import java.awt.Graphics;
//import java.awt.Rectangle;
//import java.awt.event.KeyEvent;
//import java.awt.event.KeyListener;
//import java.awt.event.MouseEvent;
//import java.awt.event.MouseListener;
//import java.awt.event.MouseMotionListener;
//import java.util.Observable;
//
//import javax.swing.SwingUtilities;
//
//import edu.stanford.genetics.treeview.HeaderInfo;
//import edu.stanford.genetics.treeview.HeaderSummary;
//import edu.stanford.genetics.treeview.LinearTransformation;
//import edu.stanford.genetics.treeview.LogBuffer;
//import edu.stanford.genetics.treeview.ModelViewBuffered;
//import edu.stanford.genetics.treeview.TreeDrawerNode;
//import edu.stanford.genetics.treeview.TreeSelectionI;
//
///**
// * Draws a gene tree to show the relations between genes
// * 
// * This object requires a MapContainer to figure out the offsets for the genes.
// */
//
//public class GTRView extends ModelViewBuffered implements MouseListener, 
//		MouseMotionListener, KeyListener {
//
//	private static final long serialVersionUID = 1L;;
//
//	protected HeaderSummary headerSummary = new HeaderSummary("GtrSummary");
//
//	private HeaderInfo gtrHI;
//	private MapContainer map;
//	private TreePainter drawer = null;
//	private TreeDrawerNode selectedNode = null;
//	private TreeDrawerNode hoveredNode = null;
//	private Rectangle destRect = null;
//
//	private TreeSelectionI geneSelection;
//	private LinearTransformation xScaleEq;
//	private LinearTransformation yScaleEq;
//
//	private final boolean ISLEFT = true;
//
//	/**
//	 * Constructor. You still need to specify a map to have this thing draw.
//	 */
//	public GTRView() {
//
//		super();
//		panel = this;
//		destRect = new Rectangle();
//
//		addMouseListener(this);
//		addMouseMotionListener(this);
//		addKeyListener(this);
//	}
//
//	/**
//	 * Set the drawer
//	 * 
//	 * @param d
//	 *            The new drawer
//	 */
//	public void setLeftTreeDrawer(final TreePainter d) {
//
//		if (drawer != null) {
//			drawer.deleteObserver(this);
//		}
//		drawer = d;
//		drawer.addObserver(this);
//	}
//
//	/**
//	 * Set geneSelection
//	 * 
//	 * @param geneSelection
//	 *            The TreeSelection which is set by selecting genes in the
//	 *            GlobalView
//	 */
//	public void setGeneSelection(final TreeSelectionI geneSelection) {
//
//		if (this.geneSelection != null) {
//			this.geneSelection.deleteObserver(this);
//		}
//		this.geneSelection = geneSelection;
//		this.geneSelection.addObserver(this);
//	}
//
//	/**
//	 * Set the map
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
//		map = m;
//		map.addObserver(this);
//		offscreenValid = false;
//		repaint();
//	}
//
//	/**
//	 * Synchronizes TreeSelection with selectedNode.
//	 * 
//	 * sets the TreeSelection to reflect the span of the selected node. sets the
//	 * selected node of the TreeSelection to this node. Notifies observers.
//	 * Should be called whenever the internal pointer to selected node is
//	 * changed.
//	 */
//	private void synchMap() {
//
//		if ((selectedNode != null) && (geneSelection != null)) {
//
//			final int start = (int) (selectedNode.getLeftLeaf().getIndex());
//			final int end = (int) (selectedNode.getRightLeaf().getIndex());
//
//			geneSelection.deselectAllIndexes();
//			geneSelection.selectIndexRange(start, end);
//			geneSelection.setSelectedNode(selectedNode.getId());
//			
//		} else {
//			geneSelection.deselectAllIndexes();
//		}
//		
//		geneSelection.notifyObservers();
//		
//		if ((status != null) && hasMouse) {
//			status.setMessages(getStatus());
//		}
//	}
//
//	/**
//	 * Setter for headerSummary
//	 * 
//	 */
//	public void setHeaderSummary(final HeaderSummary headerSummary) {
//
//		this.headerSummary = headerSummary;
//	}
//
//	/**
//	 * Getter for headerSummary
//	 */
//	public HeaderSummary getHeaderSummary() {
//
//		return headerSummary;
//	}
//	
//	public void setGTRHeaderInfo(HeaderInfo gtrHI) {
//		
//		this.gtrHI = gtrHI;
//	}
//
//	public void setSelectedNode(final TreeDrawerNode n) {
//
//		setHoveredNode(null);
//		
//		if (selectedNode == n) {
//			return;
//		}
//
//		if (getYScaleEq() != null) {
//
//			if (selectedNode != null) {
//				drawer.paintSubtree(offscreenGraphics, getXScaleEq(),
//						getYScaleEq(), destRect, selectedNode, false, ISLEFT);
//			}
//
//			selectedNode = n;
//
//			if (selectedNode != null) {
//				drawer.paintSubtree(offscreenGraphics, getXScaleEq(),
//						getYScaleEq(), destRect, selectedNode, true, ISLEFT);
//			}
//		} else {
//			selectedNode = n;
//		}
//
//		synchMap();
//		// offscreenValid = false;
//		repaint();
//	}
//	
//	public void setHoveredNode(final TreeDrawerNode n) {
//
//		if (hoveredNode == n) {
//			return;
//		}
//
//		if (getYScaleEq() != null) {
//
//			if (hoveredNode != null) {
//				drawer.paintSubtree(offscreenGraphics, getXScaleEq(),
//						getYScaleEq(), destRect, hoveredNode, false, ISLEFT);
//			}
//
//			hoveredNode = n;
//
//			if (hoveredNode != null) {
//				drawer.paintSubtree(offscreenGraphics, getXScaleEq(),
//						getYScaleEq(), destRect, hoveredNode, true, ISLEFT);
//			}
//		} else {
//			hoveredNode = n;
//		}
//
//		synchMap();
//		// offscreenValid = false;
//		repaint();
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
//		drawer.paintSubtree(offscreenGraphics, getXScaleEq(), getYScaleEq(),
//				destRect, current, true, ISLEFT);
//		drawer.paintSingle(offscreenGraphics, getXScaleEq(), getYScaleEq(),
//				destRect, selectedNode, true, ISLEFT);
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
//		drawer.paintSingle(offscreenGraphics, getXScaleEq(), getYScaleEq(),
//				destRect, current, false, ISLEFT);
//		drawer.paintSubtree(offscreenGraphics, getXScaleEq(), getYScaleEq(),
//				destRect, current.getLeft(), false, ISLEFT);
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
//		drawer.paintSingle(offscreenGraphics, getXScaleEq(), getYScaleEq(),
//				destRect, current, false, ISLEFT);
//		drawer.paintSubtree(offscreenGraphics, getXScaleEq(), getYScaleEq(),
//				destRect, current.getRight(), false, ISLEFT);
//
//		synchMap();
//		repaint();
//	}
//
//	/**
//	 * expect updates to come from map, geneSelection and drawer
//	 */
//	@Override
//	public void update(final Observable o, final Object arg) {
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
//		} else if (o == geneSelection) {
//			TreeDrawerNode cand = null;
//			if (geneSelection.getNSelectedIndexes() > 0) {
//				// Select the array node if only a single array is selected.
//				if (geneSelection.getMinIndex() == geneSelection.getMaxIndex()) {
//					cand = drawer.getLeaf(geneSelection.getMinIndex());
//				}
//				// select the root node if all genes are selected.
//				else if ((geneSelection.getMinIndex() == map.getMinIndex())
//						&& (geneSelection.getMaxIndex() == map.getMaxIndex())) {
//					cand = drawer.getRootNode();
//
//					// find node when multiple arrays are selected.
//				}
//				// } else if(geneSelection.getMinIndex() >= map.getMinIndex()
//				// && geneSelection.getMaxIndex() <= map.getMaxIndex()) {
//				// cand = drawer.getNearestNode(geneSelection.getMinIndex(),
//				// geneSelection.getMaxIndex());
//				// }
//			}
//
//			if ((cand != null)
//					&& !(cand.getId().equalsIgnoreCase(geneSelection
//							.getSelectedNode()))) {
//				geneSelection.setSelectedNode(cand.getId());
//				geneSelection.notifyObservers();
//
//			} else {
//				setSelectedNode(drawer.getNodeById(geneSelection
//						.getSelectedNode()));
//			}
//		} else {
//			LogBuffer.println(viewName() + "Got an update from unknown " + o);
//		}
//
//		revalidate();
//		repaint();
//	}
//
//	// method from ModelView
//	@Override
//	public String viewName() {
//
//		return "GTRView";
//	}
//
//	// method from ModelView
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
//				final String[] names = gtrHI.getNames();
//
//				for (int i = 0; i < nameIndex.length; i++) {
//					status[2 * i] = names[nameIndex[i]] + ":";
//					status[2 * i + 1] = " "
//							+ gtrHI.getHeader(gtrHI.getHeaderIndex(
//									selectedNode.getId()))[nameIndex[i]];
//				}
//			}
//		} else {
//			status = new String[2];
//			status[0] = "Select Node to ";
//			status[1] = "view annotation.";
//		}
//		return status;
//	}
//
//	// method from ModelView
//	@Override
//	public void updateBuffer(final Graphics g) {
//
//		// System.out.println("GTRView updateBuffer() called offscreenChanged "
//		// + offscreenChanged + " valid " + offscreenValid + " yScaleEq "
//		// + getYScaleEq());
//		if (offscreenChanged == true) {
//			offscreenValid = false;
//		}
//
//		if ((offscreenValid == false) && (drawer != null)) {
//			map.setAvailablePixels(offscreenSize.height);
//
//			// clear the pallette...
//			g.setColor(this.getBackground());
//			g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
//			g.setColor(Color.black);
//
//			// calculate Scaling
//			destRect.setBounds(0, 0, offscreenSize.width, map.getUsedPixels());
//			setXScaleEq(new LinearTransformation(drawer.getCorrMin(),
//					destRect.x, drawer.getCorrMax(), destRect.x
//							+ destRect.width));
//
//			setYScaleEq(new LinearTransformation(map.getIndex(destRect.y),
//					destRect.y, map.getIndex(destRect.y + destRect.height),
//					destRect.y + destRect.height));
//
//			// System.out.println("yScaleEq " + getYScaleEq());
//			// draw
//			drawer.paint(g, getXScaleEq(), getYScaleEq(), destRect,
//					selectedNode, ISLEFT);
//
//		} else {
//			// System.out.println("didn't update buffer: valid = "
//			// + offscreenValid + " drawer = " + drawer);
//		}
//	}
//
//	// Mouse Listener
//	@Override
//	public void mouseClicked(final MouseEvent e) {
//
//		if (!enclosingWindow().isActive()) {
//			return;
//		}
//
//		if ((drawer != null) && (getXScaleEq() != null)) {
//			if(SwingUtilities.isLeftMouseButton(e)) {
//				// the trick is translating back to the normalized space...
//				setSelectedNode(drawer.getClosest(
//						getYScaleEq().inverseTransform(e.getY()), getXScaleEq()
//								.inverseTransform(e.getX()), getXScaleEq()
//								.getSlope() / getYScaleEq().getSlope()));
//			} else {
//				setSelectedNode(null);
//			}
//		} else {
//			if (drawer == null) {
//				LogBuffer.println("GTRView.mouseClicked() : drawer is null");
//			}
//
//			if (getXScaleEq() == null) {
//				LogBuffer.println("GTRView.mouseClicked() : xscaleEq is null");
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
//		if (drawer != null && geneSelection.getNSelectedIndexes() == 0) {
//			// the trick is translating back to the normalized space...
//			setHoveredNode(drawer.getClosest(
//					getYScaleEq().inverseTransform(e.getY()), getXScaleEq()
//							.inverseTransform(e.getX()), getXScaleEq()
//							.getSlope() / getYScaleEq().getSlope()));
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
//	// method from KeyListener
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
//
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
//
//	}
//
//	@Override
//	public void keyReleased(final KeyEvent e) {
//	}
//
//	@Override
//	public void keyTyped(final KeyEvent e) {
//	}
//
//	/** Setter for xScaleEq */
//	public void setXScaleEq(final LinearTransformation xScaleEq) {
//
//		this.xScaleEq = xScaleEq;
//	}
//
//	/** Getter for xScaleEq */
//	public LinearTransformation getXScaleEq() {
//
//		return xScaleEq;
//	}
//
//	/** Setter for yScaleEq */
//	public void setYScaleEq(final LinearTransformation yScaleEq) {
//
//		this.yScaleEq = yScaleEq;
//	}
//
//	/** Getter for yScaleEq */
//	public LinearTransformation getYScaleEq() {
//
//		return yScaleEq;
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
//			if (map.isVisible(index) == false) {
//				map.scrollToIndex(index);
//				map.notifyObservers();
//			}
//		}
//	}
//}
