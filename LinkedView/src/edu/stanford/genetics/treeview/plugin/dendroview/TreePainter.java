/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Stack;

import edu.stanford.genetics.treeview.LinearTransformation;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeDrawerNode;
import edu.stanford.genetics.treeview.TreeSelectionI;

/**
 * Class for drawing ATR-style inverted trees
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version Alpha
 */

public class TreePainter extends TreeDrawer {

	private boolean isLeft;

	/**
	 * Paints the entire tree
	 * @param graphics - the graphics object with which to paint
	 * @param xScaleEq - X coord calculations object
	 * @param yScaleEq - Y coord calculations object
	 * @param dest - The coords where to draw the tree
	 * @param isLeft - left tree or top tree
	 * @param hoverIndex - the data index hovered over
	 * @param treeSelection - contains the selected indexes
	 * @param hoveredNode - the node the cursor is closest to
	 */
	@Override
	public void paint(final Graphics graphics,
		final LinearTransformation xScaleEq,
		final LinearTransformation yScaleEq,final Rectangle dest,
		final boolean isLeft,final int hoverIndex,
		final TreeSelectionI treeSelection,final TreeDrawerNode hoveredNode) {

		if ((getRootNode() == null) || (getRootNode().isLeaf())) {
			LogBuffer.println("Root node is null or leaf in paint() "
					+ "in InvertedTreeDrawer!");
		} else {
			this.isLeft = isLeft;
			// recursively drawtree...
			final NodeDrawer nd =
				new NodeDrawer(graphics,xScaleEq,yScaleEq,dest,hoveredNode);
			nd.draw(getRootNode(),hoverIndex,treeSelection);
		}
	}

	/**
	 * Export to file method which paints a portion of a tree in a position
	 * aligned with the matrix and the other tree.
	 * @author rleach
	 * @param graphics - the graphics object with which to paint
	 * @param xScaleEq - X coord calculations object
	 * @param yScaleEq - Y coord calculations object
	 * @param dest - The coords where to draw the tree
	 * @param isLeft - left tree or top tree
	 * @param treeSelection - contains the selected indexes
	 * @param xIndent - where to start drawing the (entire) tree
	 * @param yIndent - where to start drawing the (entire) tree
	 * @param size - The size of an edge of a tile in the matrix
	 * @param startIndex - The data index where the drawing starts
	 * @param endIndex - The data index where the drawing ends
	 */
	public void paint(final Graphics graphics,
		final LinearTransformation xScaleEq,
		final LinearTransformation yScaleEq,final Rectangle dest,
		final boolean isLeft,final TreeSelectionI treeSelection,
		final int xIndent,final int yIndent,final int size,
		final int startIndex,final int endIndex) {
	
		if ((getRootNode() == null) || (getRootNode().isLeaf())) {
			LogBuffer.println("Root node is null or leaf in paint() "
					+ "in InvertedTreeDrawer!");
		} else {
			this.isLeft = isLeft;
			// recursively drawtree...
			final NodeDrawer nd =
				new NodeDrawer(graphics,xScaleEq,yScaleEq,dest,null);
			nd.draw(getRootNode(),treeSelection,xIndent,yIndent,size,
				startIndex,endIndex);
		}
	}

	/**
	 * Paints a subtree
	 * @author rleach
	 * @param graphics - the graphics object with which to paint
	 * @param xScaleEq - X coord calculations object
	 * @param yScaleEq - Y coord calculations object
	 * @param dest - The coords where to draw the tree
	 * @param root - the top node to draw
	 * @param isLeft - left tree or top tree
	 * @param hoverIndex - the data index hovered over
	 * @param treeSelection - contains the selected indexes
	 * @param hoveredNode - the node the cursor is closest to
	 */
	public void paintSubtree(final Graphics graphics,
		final LinearTransformation xScaleEq,
		final LinearTransformation yScaleEq, final Rectangle dest,
		final TreeDrawerNode root,final boolean isLeft,final int hoverIndex,
		final TreeSelectionI treeSelection,final TreeDrawerNode hoveredNode) {

		if ((root == null) || (root.isLeaf()) || (xScaleEq == null)
				|| (yScaleEq == null))
			return;
		this.isLeft = isLeft;
		
		// recursively drawtree...
		final NodeDrawer nd =
			new NodeDrawer(graphics,xScaleEq,yScaleEq,dest,hoveredNode);
		nd.draw(root,hoverIndex,treeSelection);
	}

	/**
	 * this is an internal helper class which does a sort of recursive drawing
	 *
	 * @author Alok Saldanha <alok@genome.stanford.edu>
	 * @version Alpha
	 */
	class NodeDrawer {

		private final Graphics graphics;
		private final LinearTransformation xT, yT;
		private TreeDrawerNode hoveredNode;

		private final double minInd;
		private final double maxInd;
		private final Rectangle dest;

		/**
		 * The constructor sets the variables
		 *
		 * @param g
		 *            The graphics object to print to
		 *
		 * @param xScaleEq
		 *            The equation to be applied to scale the index of the nodes
		 *            to graphics object
		 *
		 * @param yScaleEq
		 *            The equation to be applied to scale the correlation of the
		 *            nodes to the graphics object
		 *
		 *            maybe foreground color, selection color and node color
		 *            should be options?
		 *
		 * @param d - where to draw
		 * @param hoveredNode - the currently hovered tree node
		 */
		public NodeDrawer(final Graphics g,final LinearTransformation xScaleEq,
			final LinearTransformation yScaleEq,final Rectangle d,
			final TreeDrawerNode hoveredNode) {

			graphics = g;
			xT = xScaleEq;
			yT = yScaleEq;
			setHoveredNode(hoveredNode);
			dest = d; // TODO if d is NULL this will crash and burn in else clause

			// GTRView
			if (isLeft && dest != null) {
				minInd = (int) yScaleEq.inverseTransform(dest.y);
				maxInd = (int) yScaleEq.inverseTransform(dest.y + dest.height) + 1;
				// ATRView
			} else {
				minInd = (int) xScaleEq.inverseTransform(dest.x);
				maxInd = (int) xScaleEq.inverseTransform(dest.x + dest.width) + 1;
			}
		}

		/**
		 * Wrapper for drawDFS, which has an additional parameter for recursion
		 * in order to propagate the hovered node state down its subtree and
		 * thus draw the hovered node's subtree red.  It also draws dots over
		 * top of the tree for all selected subtrees and leaves, as well as the
		 * hovered node after the tree is finished (so that the dots don't get
		 * drawn over).
		 * @author rleach
		 * @param node - The top node whose subtree to draw.
		 * @param hoverIndex - The data index the cursor is in alignment with
		 *                     over the matrix
		 * @param treeSelection - contains the selected data indexes
		 */
		public void draw(final TreeDrawerNode node,final int hoverIndex,
			final TreeSelectionI treeSelection) {

			Stack<TreeDrawerNode> selectedNodeStack =
				drawDFS(node,hoverIndex,false,treeSelection);

			graphics.setColor(new Color(197,181,66));//dark yellow
			drawNodeDots(selectedNodeStack);
			if(aNodeIsHovered()) {
				graphics.setColor(Color.RED);
				drawNodeDot(getHoveredNode());
			}
		}

		/**
		 * 
		 * @author rleach
		 * @param node - root node of the tree
		 * @param treeSelection - selection object
		 * @param xIndent - where to start drawing the (entire) tree
		 * @param yIndent - where to start drawing the (entire) tree
		 * @param size - The size of an edge of a tile in the matrix
		 * @param startIndex - The data index where the drawing starts
		 * @param endIndex - The data index where the drawing ends
		 */
		public void draw(final TreeDrawerNode node,
			final TreeSelectionI treeSelection,final int xIndent,
			final int yIndent,final int size,final int startIndex,
			final int endIndex) {

			exportDFS(node,treeSelection,xIndent,yIndent,size,node.getMinCorr(),
				node.getMaxCorr(),startIndex,endIndex);
		}

		/**
		 * Determines whether a given node is the hovered node
		 * @author rleach
		 * @param node - the node to test
		 * @return boolean
		 */
		public boolean isNodeHovered(TreeDrawerNode node) {
			if(getHoveredNode() != null && node != null &&
				node == getHoveredNode()) {

				return(true);
			}
			return(false);
		}

		/**
		 * Returns whether a node is currently hovered over
		 * @author rleach
		 * @return boolean
		 */
		public boolean aNodeIsHovered() {
			return(isNodeHovered(getHoveredNode()));
		}

		/**
		 * The drawing method works recursively and returns a stack of the
		 * selected nodes
		 * @author rleach
		 * @param node - The top node to draw
		 * @param hoverIndex - The data index the cursor is in alignment with
		 *                     over the matrix
		 * @param isNodeHovered - whether we're under a hovered node
		 * @param treeSelection - contains the selected data indexes
		 * @return a stack of selected nodes (either only the top ones or a
		 *         group of disjoint selected nodes, including leaves)
		 */
		public Stack<TreeDrawerNode> drawDFS(final TreeDrawerNode node,
			final int hoverIndex,boolean isNodeHovered,
			final TreeSelectionI treeSelection) {

			Stack<TreeDrawerNode> returnStack = new Stack<TreeDrawerNode>();

			//If we've hit the hovered node, set it to true for this and all
			//child nodes
			if(isNodeHovered(node)) {
				isNodeHovered = true;
			}

			// just return if no subkids visible.
			if((node.getMaxIndex() < minInd) ||
				(node.getMinIndex() > maxInd)) {
				if(isNodeSelected(node,treeSelection)) {
					returnStack.push(node);
				}
				return(returnStack);
			}

			//These will keep track of the selected nodes so that the dots can
			//be drawn on top and the branch colors can be determined
			Stack<TreeDrawerNode> leftDotNodeStack  =
				new Stack<TreeDrawerNode>();
			Stack<TreeDrawerNode> rightDotNodeStack =
				new Stack<TreeDrawerNode>();

			/* Recursive calls (leaves will be drawn first) */

			//Do the left side
			if(!node.getLeft().isLeaf()) {
				leftDotNodeStack = drawDFS(node.getLeft(),hoverIndex,
					isNodeHovered,treeSelection);
			}
			//We do not recurse down to the leaves, so add them to the stack
			//here
			else if(treeSelection.isIndexSelected(
				(int) node.getLeft().getIndex())) {

				leftDotNodeStack.push(node.getLeft());
			}

			//Do the right side
			if(!node.getRight().isLeaf()) {
				rightDotNodeStack = drawDFS(node.getRight(),hoverIndex,
					isNodeHovered,treeSelection);
			}
			//We do not recurse down to the leaves, so add them to the stack
			//here
			else if(treeSelection.isIndexSelected(
				(int) node.getRight().getIndex())) {

				rightDotNodeStack.push(node.getRight());
			}

			boolean thisNodeIsSelected = false;

			//If the stack returned from each child contains 1 selected
			//node and that node is the child of this node, then we're not going
			//to draw dots for those nodes and just draw a dot for this node.
			//Ignore the child nodes and push this node onto a new stack to
			//return.  Otherwise, all all the selected subtrees & leaves on top
			//of which to draw dots.
			if(!leftDotNodeStack.isEmpty()   && !rightDotNodeStack.isEmpty()  &&
				leftDotNodeStack.size() == 1 && rightDotNodeStack.size() == 1 &&
				leftDotNodeStack.peek().getId()  == node.getLeft().getId()    &&
				rightDotNodeStack.peek().getId() == node.getRight().getId()) {

				thisNodeIsSelected = true;
				returnStack.push(node);
			} else {
				if(!leftDotNodeStack.isEmpty()) {
					returnStack.addAll(leftDotNodeStack);
				}
				if(!rightDotNodeStack.isEmpty()) {
					returnStack.addAll(rightDotNodeStack);
				}
			}

			// finally draw
			drawSingle(node,hoverIndex,isNodeHovered,thisNodeIsSelected);

			return(returnStack);
		}

		/**
		 * Exports a portion of a tree to a file in a position aligned with the
		 * matrix and other tree
		 * @author rleach
		 * @param node - root node of the tree
		 * @param hoverIndex - 
		 * @param isNodeHovered
		 * @param treeSelection - selection object
		 * @param xIndent - where to start drawing the (entire) tree
		 * @param yIndent - where to start drawing the (entire) tree
		 * @param size - The size of an edge of a tile in the matrix
		 * @param minCorr - minimum correlation value
		 * @param maxCorr - maximum correlation value
		 * @param startIndex - The data index where the drawing starts
		 * @param endIndex - The data index where the drawing ends
		 * @return
		 */
		public Stack<TreeDrawerNode> exportDFS(final TreeDrawerNode node,
			final TreeSelectionI treeSelection,
			final int xIndent,final int yIndent,final int size,
			final double minCorr,final double maxCorr,final int startIndex,
			final int endIndex) {
	
			Stack<TreeDrawerNode> returnStack = new Stack<TreeDrawerNode>();

/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//			// just return if no subkids visible.
//			if((node.getMaxIndex() < minInd) ||
//				(node.getMinIndex() > maxInd)) {
//				if(isNodeSelected(node,treeSelection)) {
//					returnStack.push(node);
//				}
//				return(returnStack);
//			}
	
			//These will keep track of the selected nodes so that the dots can
			//be drawn on top and the branch colors can be determined
			Stack<TreeDrawerNode> leftDotNodeStack  =
				new Stack<TreeDrawerNode>();
			Stack<TreeDrawerNode> rightDotNodeStack =
				new Stack<TreeDrawerNode>();
	
			/* Recursive calls (leaves will be drawn first) */
	
			//Do the left side
			if(!node.getLeft().isLeaf()) {
				leftDotNodeStack = exportDFS(node.getLeft(),treeSelection,
					xIndent,yIndent,size,minCorr,maxCorr,startIndex,endIndex);
			}
/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//			//We do not recurse down to the leaves, so add them to the stack
//			//here
//			else if(treeSelection.isIndexSelected(
//				(int) node.getLeft().getIndex())) {
//	
//				leftDotNodeStack.push(node.getLeft());
//			}
	
			//Do the right side
			if(!node.getRight().isLeaf()) {
				rightDotNodeStack = exportDFS(node.getRight(),treeSelection,
					xIndent,yIndent,size,minCorr,maxCorr,startIndex,endIndex);
			}
/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//			//We do not recurse down to the leaves, so add them to the stack
//			//here
//			else if(treeSelection.isIndexSelected(
//				(int) node.getRight().getIndex())) {
//	
//				rightDotNodeStack.push(node.getRight());
//			}
	
			boolean thisNodeIsSelected = false;
	
/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//			//If the stack returned from each child contains 1 selected
//			//node and that node is the child of this node, then we're not going
//			//to draw dots for those nodes and just draw a dot for this node.
//			//Ignore the child nodes and push this node onto a new stack to
//			//return.  Otherwise, all all the selected subtrees & leaves on top
//			//of which to draw dots.
//			if(!leftDotNodeStack.isEmpty()   && !rightDotNodeStack.isEmpty()  &&
//				leftDotNodeStack.size() == 1 && rightDotNodeStack.size() == 1 &&
//				leftDotNodeStack.peek().getId()  == node.getLeft().getId()    &&
//				rightDotNodeStack.peek().getId() == node.getRight().getId()) {
//	
//				thisNodeIsSelected = true;
//				returnStack.push(node);
//			} else {
//				if(!leftDotNodeStack.isEmpty()) {
//					returnStack.addAll(leftDotNodeStack);
//				}
//				if(!rightDotNodeStack.isEmpty()) {
//					returnStack.addAll(rightDotNodeStack);
//				}
//			}
	
			// finally draw
			exportSingle(node,thisNodeIsSelected,xIndent,yIndent,size,minCorr,
				maxCorr,startIndex,endIndex);
	
			return(returnStack);
		}

		/**
		 * Determines whether all the leaves under a given node have indexes
		 * that are selected
		 * @author rleach
		 * @param node - the tree node to check the leaves of
		 * @param treeSelection - contains the selected data indexes
		 * @return boolean
		 */
		private boolean isNodeSelected(TreeDrawerNode node,
			TreeSelectionI treeSelection) {
			boolean selected = true;
			for(int i = (int) node.getLeftLeaf().getIndex();
				i <= (int) node.getRightLeaf().getIndex();i++) {
				if(!treeSelection.isIndexSelected(i)) {
					selected = false;
					break;
				}
			}
			return(selected);
		}

		/**
		 * Accessor
		 * @author rleach
		 * @return the hoveredNode
		 */
		public TreeDrawerNode getHoveredNode() {
			return(hoveredNode);
		}

		/**
		 * Setter
		 * @author rleach
		 * @param hoveredNode the hoveredNode to set
		 */
		public void setHoveredNode(TreeDrawerNode hoveredNode) {
			this.hoveredNode = hoveredNode;
		}

		/**
		 * This method is intended to draw a dot over a hovered or selected top
		 * node after its parent branch has been drawn so that the dot ends up
		 * on top of the branch lines
		 * @author rleach
		 * @param node
		 */
		public void drawNodeDot(final TreeDrawerNode node) {

			int x = 0;
			int y = 0;

			if(isLeft) {
				x = (int) xT.transform(node.getCorr()) - 2;
				y = (int) yT.transform(node.getIndex() + .5) - 2;
			} else {
				y = (int) yT.transform(node.getCorr()) - 2;
				x = (int) xT.transform(node.getIndex() + .5) - 2;
			}

			graphics.fillRect(x,y,5,5);
		}

		/**
		 * Draws all the tree node dots in a stack of nodes
		 * @author rleach
		 * @param nodeStack
		 */
		public void drawNodeDots(final Stack<TreeDrawerNode> nodeStack) {
			if(nodeStack == null || nodeStack.isEmpty()) {
				return;
			}
			while(!nodeStack.isEmpty()) {
				TreeDrawerNode node = nodeStack.pop();
				drawNodeDot(node);
			}
		}

		/**
		 * Draws 2 branches stemming from a given node
		 * @author rleach
		 * @param node - the node from which to draw the branches
		 * @param hoverIndex - the data index corresponding to the cursor
		 * @param isNodeHovered - if we are under a hovered node
		 * @param isSelected - if this node is selected
		 */
		private void drawSingle(final TreeDrawerNode node,
			final int hoverIndex,final boolean isNodeHovered,
			final boolean isSelected) {

			if (xT == null) {
				LogBuffer.println("xt in drawSingle in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			if (node.getRight() == null) {
				LogBuffer.println("right in drawSingle in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			// draw our (flipped) polyline...
			if(isNodeHovered) {
				graphics.setColor(Color.red);
			} else {
				graphics.setColor(node.getColor());
			}

			drawLeftBranch(node,hoverIndex,isNodeHovered,isSelected);

			if(isNodeHovered) {
				graphics.setColor(Color.red);
			} else {
				graphics.setColor(node.getColor());
			}

			drawRightBranch(node,hoverIndex,isNodeHovered,isSelected);
		}

		/**
		 * Export a single node and it's extending branches
		 * @author rleach
		 * @param node - node being exported
		 * @param isSelected - Whether the node is selected
		 * @param xIndent - where to start drawing the (entire) tree
		 * @param yIndent - where to start drawing the (entire) tree
		 * @param size - The size of an edge of a tile in the matrix
		 * @param minCorr - minimum correlation value
		 * @param maxCorr - maximum correlation value
		 * @param startIndex - The data index where the drawing starts
		 * @param endIndex - The data index where the drawing ends
		 */
		private void exportSingle(final TreeDrawerNode node,
			final boolean isSelected,final int xIndent,final int yIndent,
			final int size,final double minCorr,final double maxCorr,
			final int startIndex,final int endIndex) {
	
/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//			if (xT == null) {
//				LogBuffer.println("xt in drawSingle in InvertedTreeDrawer "
//						+ "was null.");
//				return;
//			}

			if (node == null) {
				LogBuffer.println("node in drawSingle in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			if (node.getRight() == null) {
				LogBuffer.println("right in drawSingle in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			// draw our (flipped) polyline...
			graphics.setColor(node.getColor());

			exportLeftBranch(node,isSelected,xIndent,yIndent,size,minCorr,
				maxCorr,startIndex,endIndex);

			graphics.setColor(node.getColor());

			exportRightBranch(node,isSelected,xIndent,yIndent,size,minCorr,
				maxCorr,startIndex,endIndex);
		}

		/**
		 * Draws the left branch stemming from a given node
		 * @author rleach
		 * @param node - the node from which to draw the branches
		 * @param hoverIndex - the data index corresponding to the cursor
		 * @param isNodeHovered - if we are under a hovered node
		 * @param isSelected - if this node is selected
		 */
		public void drawLeftBranch(final TreeDrawerNode node,
			final int hoverIndex,final boolean isHovered,
			final boolean isSelected) {

			final TreeDrawerNode left = node.getLeft();
			final boolean hovered = (node.getLeft().getIndex() == hoverIndex);

			if (xT == null) {
				LogBuffer.println("xt in drawLeftBranch in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			if (left == null) {
				LogBuffer.println("left in drawSingle in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			int lx = 0;
			int tx = 0;

			int ly = 0;
			int ty = 0;

			int c = 0;

			int pointerBaseOffset = (left.isLeaf() ? 2 : 1);

			// GTRView
			if (isLeft) {
				lx = (int) xT.transform(left.getCorr());
				tx = (int) xT.transform(node.getCorr());

				ly = (int) yT.transform(left.getIndex() + .5);
				c = (int) yT.transform(node.getIndex() + .5);

				if(Math.abs(lx - tx) < 3) {
					pointerBaseOffset = 0;
				}
				// ATRView
			} else {
				ly = (int) yT.transform(left.getCorr());
				ty = (int) yT.transform(node.getCorr());

				lx = (int) xT.transform(left.getIndex() + .5);
				c = (int) xT.transform(node.getIndex() + .5);
				// int tx = (int) xT.transform(node.getIndex() + .5);

				if(Math.abs(ly - ty) < 3) {
					pointerBaseOffset = 0;
				}
			}

			if(hovered || isHovered) {
				graphics.setColor(Color.red);
			}

			// draw our (flipped) polyline...
			if(isLeft) {
				graphics.drawPolyline(
					new int[] {
						tx,
						tx,
						lx
					},
					new int[] {
						c,
						ly,
						ly
					},
					3);

				//Draw an outline around the line to either bold a hovered
				//branch or highlight a selected branch
				if(hovered || isSelected) {
					if(!hovered) {
						graphics.setColor(new Color(249,238,160));//yellow
					}
					graphics.drawPolyline(
						new int[] {
							//Horizontal coordinates
							tx + 1,
							tx + 1,
							lx - pointerBaseOffset
						},
						new int[] {
							//Vertical coordinates
							c,
							ly + 1,
							ly + 1
						},
						3);
					graphics.drawPolyline(
						new int[] {
							//Horizontal coordinates
							tx - 1,
							tx - 1,
							lx - pointerBaseOffset
						},
						new int[] {
							//Vertical coordinates
							c,
							ly - 1,
							ly - 1
						},
						3);
				}
			} else {
				graphics.drawPolyline(
					new int[] {
						//Horizontal coordinates
						c,                                //center
						lx,                               //left leaf corner
						lx                                //left leaf end
					},
					new int[] {
						//Vertical coordinates
						ty,                               //center
						ty,                               //left leaf corner
						ly                                //left leaf end
					},
					3);

				//Draw an outline around the line to either bold a hovered
				//branch or highlight a selected branch
				if(hovered || isSelected) {
					if(!hovered) {
						graphics.setColor(new Color(249,238,160));//yellow
					}
					graphics.drawPolyline(
						new int[] {
							//Horizontal coordinates
							c,                            //center
							lx + 1,                       //left leaf corner
							lx + 1                        //left leaf end
						},
						new int[] {
							//Vertical coordinates
							ty + 1,                       //center
							ty + 1,                       //left leaf corner
							ly - pointerBaseOffset        //left leaf end
						},
						3);
					graphics.drawPolyline(
						new int[] {
							//Horizontal coordinates
							c,                            //center
							lx - 1,                       //left leaf corner
							lx - 1                        //left leaf end
						},
						new int[] {
							//Vertical coordinates
							ty - 1,                       //center
							ty - 1,                       //left leaf corner
							ly - pointerBaseOffset        //left leaf end
						},
						3);
				}
			}
		}

		/**
		 * Export the left branch of a node
		 * @author rleach
		 * @param node - node being exported
		 * @param isSelected - Whether the node is selected
		 * @param xIndent - where to start drawing the (entire) tree
		 * @param yIndent - where to start drawing the (entire) tree
		 * @param size - The size of an edge of a tile in the matrix
		 * @param minCorr - minimum correlation value
		 * @param maxCorr - maximum correlation value
		 * @param startIndex - The data index where the drawing starts
		 * @param endIndex - The data index where the drawing ends
		 */
		public void exportLeftBranch(final TreeDrawerNode node,
			final boolean isSelected,final int xIndent,final int yIndent,
			final int size,final double minCorr,final double maxCorr,
			final int startIndex,final int endIndex) {

			if (node == null) {
				LogBuffer.println("node in drawSingle in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			final TreeDrawerNode left = node.getLeft();

/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//			if (xT == null) {
//				LogBuffer.println("xt in drawLeftBranch in InvertedTreeDrawer "
//						+ "was null.");
//				return;
//			}

			if (left == null) {
				LogBuffer.println("left in drawSingle in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			int lx = 0;
			int tx = 0;
	
			int ly = 0;
			int ty = 0;
	
			int c = 0;
	
/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//			int pointerBaseOffset = (left.isLeaf() ? 2 : 1);
			int minCoord = 0;
			int maxCoord = 0;
			boolean shoulderOnly = false;

			// GTRView
			if (isLeft) {
				lx = (int) Math.round((left.getCorr() - minCorr) /
					(maxCorr - minCorr) * (xIndent - 1));
				tx = (int) Math.round((node.getCorr() - minCorr) /
					(maxCorr - minCorr) * (xIndent - 1));

				ly = yIndent + (int) Math.round(left.getIndex() * size +
					(size / 2)) - startIndex * size;
				c = yIndent + (int) Math.round(node.getIndex() * size +
					(size / 2)) - startIndex * size;
	
				//These values define the "horizontal" drawing area
				minCoord = yIndent;// + startIndex * size;
				maxCoord = yIndent + endIndex * size - startIndex * size + size;

				//This conditional adjusts coordinates to not overrun the min/
				//max boundaries and either sets a boolean that prevents
				//"vertical" lines which are completely outside the drawing area
				//from drawing or returns if both lines to be drawn are outside
				//the drawing area
				if((ly < minCoord && c < minCoord) ||
					(ly > maxCoord && c > maxCoord)) {
					return;
				} else if(ly >= minCoord && c >= minCoord &&
					ly <= maxCoord && c <= maxCoord) {
					//Do nothing
				} else if(ly < minCoord && c > maxCoord) {
					shoulderOnly = true;
					c = maxCoord;
					ly = minCoord;
				} else if(ly < minCoord) {
					shoulderOnly = true;
					ly = minCoord;
				} else if(c > maxCoord) {
					c = maxCoord;
				}

/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//				if(Math.abs(lx - tx) < 3) {
//					pointerBaseOffset = 0;
//				}
				// ATRView
			} else {
				ly = (int) Math.round((left.getCorr() - minCorr) /
					(maxCorr - minCorr) * (yIndent - 1));
				ty = (int) Math.round((node.getCorr() - minCorr) /
					(maxCorr - minCorr) * (yIndent - 1));
	
				lx = xIndent + (int) Math.round(left.getIndex() * size +
					(size / 2)) - startIndex * size;
				c = xIndent + (int) Math.round(node.getIndex() * size +
					(size / 2)) - startIndex * size;

				//These values define the "horizontal" drawing area
				minCoord = xIndent;// + startIndex * size;
				maxCoord = xIndent + endIndex * size - startIndex * size + size;

				//This conditional adjusts coordinates to not overrun the min/
				//max boundaries and either sets a boolean that prevents
				//"vertical" lines which are completely outside the drawing area
				//from drawing or returns if both lines to be drawn are outside
				//the drawing area
				if((lx < minCoord && c < minCoord) ||
					(lx > maxCoord && c > maxCoord)) {
					return;
				} else if(lx >= minCoord && c >= minCoord &&
					lx <= maxCoord && c <= maxCoord) {
					//Do nothing
				} else if(lx < minCoord && c > maxCoord) {
					shoulderOnly = true;
					c = maxCoord;
					lx = minCoord;
				} else if(lx < minCoord) {
					shoulderOnly = true;
					lx = minCoord;
				} else if(c > maxCoord) {
					c = maxCoord;
				}

/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//				if(Math.abs(ly - ty) < 3) {
//					pointerBaseOffset = 0;
//				}
			}
	
/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//			if(hovered || isHovered) {
//				graphics.setColor(Color.red);
//			}
	
			// draw our (flipped) polyline...
			if(isLeft) {
				//The VectorGraphics2D library messes up the drawPolyline call
				//sometimes and skips the center point, so drawing two-
				//separate lines (while less attractively drawn) is more
				//reliable
				graphics.drawLine(tx,c,tx,ly);
				if(!shoulderOnly) {
					graphics.drawLine(tx,ly,lx,ly);
				}

/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//				//Draw an outline around the line to either bold a hovered
//				//branch or highlight a selected branch
//				if(hovered || isSelected) {
//					if(!hovered) {
//						graphics.setColor(new Color(249,238,160));//yellow
//					}
//					graphics.drawPolyline(
//						new int[] {
//							//Horizontal coordinates
//							tx + 1,
//							tx + 1,
//							lx - pointerBaseOffset
//						},
//						new int[] {
//							//Vertical coordinates
//							c,
//							ly + 1,
//							ly + 1
//						},
//						3);
//					graphics.drawPolyline(
//						new int[] {
//							//Horizontal coordinates
//							tx - 1,
//							tx - 1,
//							lx - pointerBaseOffset
//						},
//						new int[] {
//							//Vertical coordinates
//							c,
//							ly - 1,
//							ly - 1
//						},
//						3);
//				}
			} else {
				//The VectorGraphics2D library messes up the drawPolyline call
				//sometimes and skips the center point, so drawing two-
				//separate lines (while less attractively drawn) is more
				//reliable
				graphics.drawLine(c,ty,lx,ty);
				if(!shoulderOnly) {
					graphics.drawLine(lx,ty,lx,ly);
				}

/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//				//Draw an outline around the line to either bold a hovered
//				//branch or highlight a selected branch
//				if(hovered || isSelected) {
//					if(!hovered) {
//						graphics.setColor(new Color(249,238,160));//yellow
//					}
//					graphics.drawPolyline(
//						new int[] {
//							//Horizontal coordinates
//							c,                            //center
//							lx + 1,                       //left leaf corner
//							lx + 1                        //left leaf end
//						},
//						new int[] {
//							//Vertical coordinates
//							ty + 1,                       //center
//							ty + 1,                       //left leaf corner
//							ly - pointerBaseOffset        //left leaf end
//						},
//						3);
//					graphics.drawPolyline(
//						new int[] {
//							//Horizontal coordinates
//							c,                            //center
//							lx - 1,                       //left leaf corner
//							lx - 1                        //left leaf end
//						},
//						new int[] {
//							//Vertical coordinates
//							ty - 1,                       //center
//							ty - 1,                       //left leaf corner
//							ly - pointerBaseOffset        //left leaf end
//						},
//						3);
//				}
			}
		}

		/**
		 * Draws the right branch stemming from a given node
		 * @author rleach
		 * @param node - the node from which to draw the branches
		 * @param hoverIndex - the data index corresponding to the cursor
		 * @param isNodeHovered - if we are under a hovered node
		 * @param isSelected - if this node is selected
		 */
		private void drawRightBranch(final TreeDrawerNode node,
			final int hoverIndex,final boolean isHovered,
			final boolean isSelected) {

			final TreeDrawerNode right = node.getRight();
			final boolean hovered = (node.getRight().getIndex() == hoverIndex);

			if (xT == null) {
				LogBuffer.println("xt in drawRightBranch in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			if (right == null) {
				LogBuffer.println("right in drawSingle in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			int rx = 0;
			int tx = 0;

			int ry = 0;
			int ty = 0;

			int c = 0;

			int pointerBaseOffset = (right.isLeaf() ? 2 : 0);

			// GTRView
			if (isLeft) {
				rx = (int) xT.transform(right.getCorr());
				tx = (int) xT.transform(node.getCorr());

				ry = (int) yT.transform(right.getIndex() + .5);
				c = (int) yT.transform(node.getIndex() + .5);

				if(Math.abs(rx - tx) < 3) {
					pointerBaseOffset = 0;
				}

			}
			// ATRView
			else {
				ry = (int) yT.transform(right.getCorr());
				ty = (int) yT.transform(node.getCorr());

				rx = (int) xT.transform(right.getIndex() + .5);
				c = (int) xT.transform(node.getIndex() + .5);
				// int tx = (int) xT.transform(node.getIndex() + .5);

				if(Math.abs(ry - ty) < 3) {
					pointerBaseOffset = 0;
				}
			}

			// draw our (flipped) polyline...
			if(hovered || isHovered) {
				graphics.setColor(Color.red);
			}

			if (isLeft) {
				graphics.drawPolyline(
					new int[] {
						rx,
						tx,
						tx
					},
					new int[] {
						ry,
						ry,
						c
					},
					3);

				//If this is the hovered leaf, make it bold
				if(hovered || isSelected) {
					if(!hovered) {
						graphics.setColor(new Color(249,238,160));//yellow
					}
					graphics.drawPolyline(
						new int[] {
							//Horizontal coordinates
							rx - pointerBaseOffset,       //right leaf end
							tx + 1,                       //right leaf corner
							tx + 1                        //center
						},
						new int[] {
							//Vertical coordinates
							ry - 1,                       //right leaf end
							ry - 1,                       //right leaf corner
							c                             //center
						},
						3);
					graphics.drawPolyline(
						new int[] {
							//Horizontal coordinates
							rx - pointerBaseOffset,       //right leaf end
							tx - 1,                       //right leaf corner
							tx - 1                        //center
						},
						new int[] {
							//Vertical coordinates
							ry + 1,                       //right leaf end
							ry + 1,                       //right leaf corner
							c                             //center
						},
						3);
				}
			} else {
				graphics.drawPolyline(
					new int[] {
						//Horizontal coordinates
						rx,                               //right leaf end
						rx,                               //right leaf corner
						c                                 //center
					},
					new int[] {
						//Vertical coordinates
						ry,                               //right leaf end
						ty,                               //right leaf corner
						ty                                //center
					},
					3);

				//If this is the hovered leaf, make it bold
				if(hovered || isSelected) {
					if(!hovered) {
						graphics.setColor(new Color(249,238,160));//yellow
					}
					graphics.drawPolyline(
						new int[] {
							//Horizontal coordinates
							rx + 1,                       //right leaf end
							rx + 1,                       //right leaf corner
							c                             //center
						},
						new int[] {
							//Vertical coordinates
							ry - pointerBaseOffset,       //right leaf end
							ty - 1,                       //right leaf corner
							ty - 1                        //center
						},
						3);
					graphics.drawPolyline(
						new int[] {
							//Horizontal coordinates
							rx - 1,                       //right leaf end
							rx - 1,                       //right leaf corner
							c                             //center
						},
						new int[] {
							//Vertical coordinates
							ry - pointerBaseOffset,       //right leaf end
							ty + 1,                       //right leaf corner
							ty + 1                        //center
						},
						3);
				}
			}
		}

		/**
		 * Export the right branch of a node
		 * @author rleach
		 * @param node - node being exported
		 * @param isSelected - Whether the node is selected
		 * @param xIndent - where to start drawing the (entire) tree
		 * @param yIndent - where to start drawing the (entire) tree
		 * @param size - The size of an edge of a tile in the matrix
		 * @param minCorr - minimum correlation value
		 * @param maxCorr - maximum correlation value
		 * @param startIndex - The data index where the drawing starts
		 * @param endIndex - The data index where the drawing ends
		 */
		public void exportRightBranch(final TreeDrawerNode node,
			final boolean isSelected,final int xIndent,final int yIndent,
			final int size,final double minCorr,final double maxCorr,
			final int startIndex,final int endIndex) {

			if (node == null) {
				LogBuffer.println("node in drawSingle in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			final TreeDrawerNode right = node.getRight();

/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//			if (xT == null) {
//				LogBuffer.println("xt in drawRightBranch in InvertedTreeDrawer "
//						+ "was null.");
//				return;
//			}

			if (right == null) {
				LogBuffer.println("right in drawSingle in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			int rx = 0;
			int tx = 0;

			int ry = 0;
			int ty = 0;

			int c = 0;

/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//			int pointerBaseOffset = (right.isLeaf() ? 2 : 0);
			int minCoord = 0;
			int maxCoord = 0;
			boolean shoulderOnly = false;

			// GTRView
			if (isLeft) {
				rx = (int) Math.round((right.getCorr() - minCorr) /
					(maxCorr - minCorr) * (xIndent - 1));
				tx = (int) Math.round((node.getCorr() - minCorr) /
					(maxCorr - minCorr) * (xIndent - 1));

				ry = yIndent + (int) Math.round(right.getIndex() * size +
					(size / 2)) - startIndex * size;
				c = yIndent + (int) Math.round(node.getIndex() * size +
					(size / 2)) - startIndex * size;

				//These values define the "horizontal" drawing area
				minCoord = yIndent;// + startIndex * size;
				maxCoord = yIndent + endIndex * size - startIndex * size + size;

				//This conditional adjusts coordinates to not overrun the min/
				//max boundaries and either sets a boolean that prevents
				//"vertical" lines which are completely outside the drawing area
				//from drawing or returns if both lines to be drawn are outside
				//the drawing area
				if((ry < minCoord && c < minCoord) ||
					(ry > maxCoord && c > maxCoord)) {
					return;
				} else if(ry >= minCoord && c >= minCoord &&
					ry <= maxCoord && c <= maxCoord) {
					//Do nothing
				} else if(ry > maxCoord && c < minCoord) {
					shoulderOnly = true;
					c = minCoord;
					ry = maxCoord;
				} else if(c < minCoord) {
					c = minCoord;
				} else if(ry > maxCoord) {
					shoulderOnly = true;
					ry = maxCoord;
				}

/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//				if(Math.abs(rx - tx) < 3) {
//					pointerBaseOffset = 0;
//				}

			}
			// ATRView
			else {
				ry = (int) Math.round((right.getCorr() - minCorr) /
					(maxCorr - minCorr) * (yIndent - 1));
				ty = (int) Math.round((node.getCorr() - minCorr) /
					(maxCorr - minCorr) * (yIndent - 1));

				rx = xIndent + (int) Math.round(right.getIndex() * size +
					(size / 2)) - startIndex * size;
				c = xIndent + (int) Math.round(node.getIndex() * size +
					(size / 2)) - startIndex * size;

				//These values define the "horizontal" drawing area
				minCoord = xIndent;// + startIndex * size;
				maxCoord = xIndent + endIndex * size - startIndex * size + size;

				//This conditional adjusts coordinates to not overrun the min/
				//max boundaries and either sets a boolean that prevents
				//"vertical" lines which are completely outside the drawing area
				//from drawing or returns if both lines to be drawn are outside
				//the drawing area
				if((rx < minCoord && c < minCoord) ||
					(rx > maxCoord && c > maxCoord)) {
					return;
				} else if(rx >= minCoord && c >= minCoord &&
					rx <= maxCoord && c <= maxCoord) {
					//Do nothing
				} else if(rx > maxCoord && c < minCoord) {
					shoulderOnly = true;
					c = minCoord;
					rx = maxCoord;
				} else if(c < minCoord) {
					c = minCoord;
				} else if(rx > maxCoord) {
					shoulderOnly = true;
					rx = maxCoord;
				}

/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//				if(Math.abs(ry - ty) < 3) {
//					pointerBaseOffset = 0;
//				}
			}

/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//			// draw our (flipped) polyline...
//			if(hovered || isHovered) {
//				graphics.setColor(Color.red);
//			}

			if (isLeft) {
				//The VectorGraphics2D library messes up the drawPolyline call
				//sometimes and skips the center point, so drawing two-
				//separate lines (while less attractively drawn) is more
				//reliable
				if(!shoulderOnly) {
					graphics.drawLine(rx,ry,tx,ry);
				}
				graphics.drawLine(tx,ry,tx,c);

/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//				//If this is the hovered leaf, make it bold
//				if(hovered || isSelected) {
//					if(!hovered) {
//						graphics.setColor(new Color(249,238,160));//yellow
//					}
//					graphics.drawPolyline(
//						new int[] {
//							//Horizontal coordinates
//							rx - pointerBaseOffset,       //right leaf end
//							tx + 1,                       //right leaf corner
//							tx + 1                        //center
//						},
//						new int[] {
//							//Vertical coordinates
//							ry - 1,                       //right leaf end
//							ry - 1,                       //right leaf corner
//							c                             //center
//						},
//						3);
//					graphics.drawPolyline(
//						new int[] {
//							//Horizontal coordinates
//							rx - pointerBaseOffset,       //right leaf end
//							tx - 1,                       //right leaf corner
//							tx - 1                        //center
//						},
//						new int[] {
//							//Vertical coordinates
//							ry + 1,                       //right leaf end
//							ry + 1,                       //right leaf corner
//							c                             //center
//						},
//						3);
//				}
			} else {
				//The VectorGraphics2D library messes up the drawPolyline call
				//sometimes and skips the center point, so drawing two-
				//separate lines (while less attractively drawn) is more
				//reliable
				if(!shoulderOnly) {
					graphics.drawLine(rx,ry,rx,ty);
				}
				graphics.drawLine(rx,ty,c,ty);

/* Commented temporarily until all optional export features are fully
 * implemented (e.g. drawing selections) */
//				//If this is the hovered leaf, make it bold
//				if(hovered || isSelected) {
//					if(!hovered) {
//						graphics.setColor(new Color(249,238,160));//yellow
//					}
//					graphics.drawPolyline(
//						new int[] {
//							//Horizontal coordinates
//							rx + 1,                       //right leaf end
//							rx + 1,                       //right leaf corner
//							c                             //center
//						},
//						new int[] {
//							//Vertical coordinates
//							ry - pointerBaseOffset,       //right leaf end
//							ty - 1,                       //right leaf corner
//							ty - 1                        //center
//						},
//						3);
//					graphics.drawPolyline(
//						new int[] {
//							//Horizontal coordinates
//							rx - 1,                       //right leaf end
//							rx - 1,                       //right leaf corner
//							c                             //center
//						},
//						new int[] {
//							//Vertical coordinates
//							ry - pointerBaseOffset,       //right leaf end
//							ty + 1,                       //right leaf corner
//							ty + 1                        //center
//						},
//						3);
//				}
			}
		}
	}
}
