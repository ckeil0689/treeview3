/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package gui.trees;

import model.data.matrix.LinearTransformation;
import model.data.trees.TreeDrawerNode;
import model.data.trees.TreeSelectionI;
import util.LogBuffer;

import java.awt.*;
import java.util.Stack;

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
		final int startIndex,final int endIndex,final boolean showSelections) {
	
		if ((getRootNode() == null) || (getRootNode().isLeaf())) {
			LogBuffer.println("Root node is null or leaf in paint() "
					+ "in InvertedTreeDrawer!");
		} else {
			this.isLeft = isLeft;
			// recursively drawtree...
			final NodeDrawer nd =
				new NodeDrawer(graphics,xScaleEq,yScaleEq,dest,null);
			nd.draw(getRootNode(),treeSelection,xIndent,yIndent,size,
				startIndex,endIndex,showSelections);
		}
	}

	/**
	 * Paints a subtree
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

		if ((root == null) || (root.isLeaf()) || (xScaleEq == null) ||
			(yScaleEq == null)) {

			return;
		}
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
			dest = d; // TODO if d is NULL this will crash & burn in else clause

			// GTRView
			if (isLeft && dest != null) {
				minInd = (int) yScaleEq.inverseTransform(dest.y);
				maxInd = (int) yScaleEq.inverseTransform(dest.y + dest.height) +
					1;
			// ATRView
			} else {
				minInd = (int) xScaleEq.inverseTransform(dest.x);
				maxInd = (int) xScaleEq.inverseTransform(dest.x + dest.width) +
					1;
			}
		}

		/**
		 * Wrapper for drawDFS, which has an additional parameter for recursion
		 * in order to propagate the hovered node state down its subtree and
		 * thus draw the hovered node's subtree red.  It also draws dots over
		 * top of the tree for all selected subtrees and leaves, as well as the
		 * hovered node after the tree is finished (so that the dots don't get
		 * drawn over).
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
		 * Export a portion of the tree
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
			final int endIndex,final boolean showSelections) {

			Stack<TreeDrawerNode> selectedNodeStack =
				exportDFS(node,treeSelection,xIndent,yIndent,size,
					node.getMinCorr(),node.getMaxCorr(),startIndex,endIndex,
					showSelections);

			if(showSelections) {
				graphics.setColor(new Color(197,181,66));//dark yellow
				exportNodeDots(selectedNodeStack,xIndent,yIndent,size,
					node.getMinCorr(),node.getMaxCorr(),startIndex,endIndex);
			}
		}

		/**
		 * Determines whether a given node is the hovered node
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
		 * @return boolean
		 */
		public boolean aNodeIsHovered() {
			return(isNodeHovered(getHoveredNode()));
		}

		/**
		 * The drawing method works recursively and returns a stack of the
		 * selected nodes
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
			else if(treeSelection != null && treeSelection.isIndexSelected(
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
			else if(treeSelection != null && treeSelection.isIndexSelected(
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
			final int endIndex,boolean showSelections) {
	
			Stack<TreeDrawerNode> returnStack = new Stack<TreeDrawerNode>();

			// just return if no subkids visible.
			if((node.getMaxIndex() < startIndex) ||
				(node.getMinIndex() > endIndex)) {
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
				leftDotNodeStack = exportDFS(node.getLeft(),treeSelection,
					xIndent,yIndent,size,minCorr,maxCorr,startIndex,endIndex,
					showSelections);
			}
			//We do not recurse down to the leaves, so add them to the stack
			//here
			else if(treeSelection.isIndexSelected(
				(int) node.getLeft().getIndex())) {
	
				leftDotNodeStack.push(node.getLeft());
			}
	
			//Do the right side
			if(!node.getRight().isLeaf()) {
				rightDotNodeStack = exportDFS(node.getRight(),treeSelection,
					xIndent,yIndent,size,minCorr,maxCorr,startIndex,endIndex,
					showSelections);
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
			exportSingle(node,thisNodeIsSelected,xIndent,yIndent,size,minCorr,
				maxCorr,startIndex,endIndex,showSelections);
	
			return(returnStack);
		}

		/**
		 * Determines whether all the leaves under a given node have indexes
		 * that are selected
		 * @param node - the tree node to check the leaves of
		 * @param treeSelection - contains the selected data indexes
		 * @return boolean
		 */
		private boolean isNodeSelected(TreeDrawerNode node,
			TreeSelectionI treeSelection) {
			
			if(treeSelection == null) {
				return false;
			}
			
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
		 * @return the hoveredNode
		 */
		public TreeDrawerNode getHoveredNode() {
			return(hoveredNode);
		}

		/**
		 * Setter
		 * @param hoveredNode the hoveredNode to set
		 */
		public void setHoveredNode(TreeDrawerNode hoveredNode) {
			this.hoveredNode = hoveredNode;
		}

		/**
		 * This method is intended to draw a dot over a hovered or selected top
		 * node after its parent branch has been drawn so that the dot ends up
		 * on top of the branch lines
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

		public void exportNodeDot(final TreeDrawerNode node,final int xIndent,
			final int yIndent,final int size,final double minCorr,
			final double maxCorr,final int startIndex,final int endIndex) {

			int x = 0;
			int y = 0;

			if(isLeft) {
				x = (int) Math.round((node.getCorr() - minCorr) /
					(maxCorr - minCorr) * (xIndent - 1)) - 2;
				y = yIndent + (int) Math.round(node.getIndex() * size +
					(size / 2)) - startIndex * size - 2;
			} else {
				y = (int) Math.round((node.getCorr() - minCorr) /
					(maxCorr - minCorr) * (yIndent - 1)) - 2;
				x = xIndent + (int) Math.round(node.getIndex() * size +
					(size / 2)) - startIndex * size - 2;
			}

			graphics.fillRect(x,y,5,5);
		}

		/**
		 * Draws all the tree node dots in a stack of nodes
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

		public void exportNodeDots(final Stack<TreeDrawerNode> nodeStack,
			final int xIndent,final int yIndent,final int size,
			final double minCorr,final double maxCorr,final int startIndex,
			final int endIndex) {

			if(nodeStack == null || nodeStack.isEmpty()) {
				return;
			}
			while(!nodeStack.isEmpty()) {
				TreeDrawerNode node = nodeStack.pop();
				exportNodeDot(node,xIndent,yIndent,size,minCorr,maxCorr,
					startIndex,endIndex);
			}
		}

		/**
		 * Draws 2 branches stemming from a given node
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
			final int startIndex,final int endIndex,
			final boolean showSelections) {

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
				maxCorr,startIndex,endIndex,showSelections);

			graphics.setColor(node.getColor());

			exportRightBranch(node,isSelected,xIndent,yIndent,size,minCorr,
				maxCorr,startIndex,endIndex,showSelections);
		}

		/**
		 * Draws the left branch stemming from a given node
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

			int leftChildXCoord = 0;
			int leftChildYCoord = 0;
			int parentXCoord = 0;
			int parentYCoord = 0;

			int pointerBaseOffset = (left.isLeaf() ? 2 : 1);

			// GTRView
			if (isLeft) {
				leftChildXCoord = (int) xT.transform(left.getCorr());
				parentXCoord = (int) xT.transform(node.getCorr());

				leftChildYCoord = (int) yT.transform(left.getIndex() + .5);
				parentYCoord = (int) yT.transform(node.getIndex() + .5);

				if(Math.abs(leftChildXCoord - parentXCoord) < 3) {
					pointerBaseOffset = 0;
				}
			// ATRView
			} else {
				leftChildYCoord = (int) yT.transform(left.getCorr());
				parentYCoord = (int) yT.transform(node.getCorr());

				leftChildXCoord = (int) xT.transform(left.getIndex() + .5);
				parentXCoord = (int) xT.transform(node.getIndex() + .5);

				if(Math.abs(leftChildYCoord - parentYCoord) < 3) {
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
						parentXCoord,
						parentXCoord,
						leftChildXCoord
					},
					new int[] {
						parentYCoord,
						leftChildYCoord,
						leftChildYCoord
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
							parentXCoord + 1,
							parentXCoord + 1,
							leftChildXCoord - pointerBaseOffset
						},
						new int[] {
							parentYCoord,
							leftChildYCoord + 1,
							leftChildYCoord + 1
						},
						3);
					graphics.drawPolyline(
						new int[] {
							parentXCoord - 1,
							parentXCoord - 1,
							leftChildXCoord - pointerBaseOffset
						},
						new int[] {
							parentYCoord,
							leftChildYCoord - 1,
							leftChildYCoord - 1
						},
						3);
				}
			} else {
				graphics.drawPolyline(
					new int[] {
						parentXCoord,
						leftChildXCoord,
						leftChildXCoord
					},
					new int[] {
						parentYCoord,
						parentYCoord,
						leftChildYCoord
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
							parentXCoord,
							leftChildXCoord + 1,
							leftChildXCoord + 1
						},
						new int[] {
							parentYCoord + 1,
							parentYCoord + 1,
							leftChildYCoord - pointerBaseOffset
						},
						3);
					graphics.drawPolyline(
						new int[] {
							parentXCoord,
							leftChildXCoord - 1,
							leftChildXCoord - 1
						},
						new int[] {
							parentYCoord - 1,
							parentYCoord - 1,
							leftChildYCoord - pointerBaseOffset
						},
						3);
				}
			}
		}

		/**
		 * Export the left branch of a node
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
			final int startIndex,final int endIndex,
			final boolean showSelections) {

			if (node == null) {
				LogBuffer.println("node in drawSingle in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			final TreeDrawerNode left = node.getLeft();

			if (left == null) {
				LogBuffer.println("left in drawSingle in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			int leftChildXCoord = 0;
			int leftChildYCoord = 0;
			int parentXCoord = 0;
			int parentYCoord = 0;

			int pointerBaseOffset = (left.isLeaf() ? 2 : 1);
			int minCoord = 0;
			int maxCoord = 0;
			boolean shoulderOnly = false;

			// GTRView
			if (isLeft) {
				leftChildXCoord = (int) Math.round((left.getCorr() - minCorr) /
					(maxCorr - minCorr) * (xIndent - 1));
				parentXCoord = (int) Math.round((node.getCorr() - minCorr) /
					(maxCorr - minCorr) * (xIndent - 1));

				leftChildYCoord =
					yIndent + (int) Math.round(left.getIndex() * size +
					(size / 2)) - startIndex * size;
				parentYCoord =
					yIndent + (int) Math.round(node.getIndex() * size +
					(size / 2)) - startIndex * size;

				//These values define the "horizontal" drawing area
				minCoord = yIndent;// + startIndex * size;
				maxCoord = yIndent + endIndex * size - startIndex * size + size;

				//This conditional adjusts coordinates to not overrun the min/
				//max boundaries and either sets a boolean that prevents
				//"vertical" lines which are completely outside the drawing area
				//from drawing or returns if both lines to be drawn are outside
				//the drawing area
				if((leftChildYCoord < minCoord && parentYCoord < minCoord) ||
					(leftChildYCoord > maxCoord && parentYCoord > maxCoord)) {

					return;
				} else if(leftChildYCoord >= minCoord &&
					parentYCoord >= minCoord &&
					leftChildYCoord <= maxCoord && parentYCoord <= maxCoord) {

					//Do nothing
				} else if(leftChildYCoord < minCoord &&
					parentYCoord > maxCoord) {

					shoulderOnly = true;
					parentYCoord = maxCoord;
					leftChildYCoord = minCoord;
				} else if(leftChildYCoord < minCoord) {
					shoulderOnly = true;
					leftChildYCoord = minCoord;
				} else if(parentYCoord > maxCoord) {
					parentYCoord = maxCoord;
				}

				if(Math.abs(leftChildXCoord - parentXCoord) < 3) {
					pointerBaseOffset = 0;
				}
			// ATRView
			} else {
				leftChildYCoord = (int) Math.round((left.getCorr() - minCorr) /
					(maxCorr - minCorr) * (yIndent - 1));
				parentYCoord = (int) Math.round((node.getCorr() - minCorr) /
					(maxCorr - minCorr) * (yIndent - 1));
	
				leftChildXCoord =
					xIndent + (int) Math.round(left.getIndex() * size +
					(size / 2)) - startIndex * size;
				parentXCoord =
					xIndent + (int) Math.round(node.getIndex() * size +
					(size / 2)) - startIndex * size;

				//These values define the "horizontal" drawing area
				minCoord = xIndent;// + startIndex * size;
				maxCoord = xIndent + endIndex * size - startIndex * size + size;

				//This conditional adjusts coordinates to not overrun the min/
				//max boundaries and either sets a boolean that prevents
				//"vertical" lines which are completely outside the drawing area
				//from drawing or returns if both lines to be drawn are outside
				//the drawing area
				if((leftChildXCoord < minCoord && parentXCoord < minCoord) ||
					(leftChildXCoord > maxCoord && parentXCoord > maxCoord)) {

					return;
				} else if(leftChildXCoord >= minCoord &&
					parentXCoord >= minCoord &&
					leftChildXCoord <= maxCoord && parentXCoord <= maxCoord) {

					//Do nothing
				} else if(leftChildXCoord < minCoord &&
					parentXCoord > maxCoord) {

					shoulderOnly = true;
					parentXCoord = maxCoord;
					leftChildXCoord = minCoord;
				} else if(leftChildXCoord < minCoord) {
					shoulderOnly = true;
					leftChildXCoord = minCoord;
				} else if(parentXCoord > maxCoord) {
					parentXCoord = maxCoord;
				}

				if(Math.abs(leftChildYCoord - parentYCoord) < 3) {
					pointerBaseOffset = 0;
				}
			}

			// draw our (flipped) polyline...
			if(isLeft) {
				//The VectorGraphics2D library messes up the drawPolyline call
				//sometimes and skips the center point, so drawing two-
				//separate lines (while less attractively drawn) is more
				//reliable
				graphics.drawLine(parentXCoord,parentYCoord,
					parentXCoord,leftChildYCoord);
				if(!shoulderOnly) {
					graphics.drawLine(parentXCoord,leftChildYCoord,
						leftChildXCoord,leftChildYCoord);
				}

				//Draw an outline around the line to highlight a selected branch
				if(showSelections && isSelected) {
					graphics.setColor(new Color(249,238,160));//yellow
					graphics.drawPolyline(
						new int[] {
							parentXCoord + 1,
							parentXCoord + 1,
							leftChildXCoord - pointerBaseOffset
						},
						new int[] {
							parentYCoord,
							leftChildYCoord + 1,
							leftChildYCoord + 1
						},
						3);
					graphics.drawPolyline(
						new int[] {
							parentXCoord - 1,
							parentXCoord - 1,
							leftChildXCoord - pointerBaseOffset
						},
						new int[] {
							parentYCoord,
							leftChildYCoord - 1,
							leftChildYCoord - 1
						},
						3);
				}
			} else {
				//The VectorGraphics2D library messes up the drawPolyline call
				//sometimes and skips the center point, so drawing two-
				//separate lines (while less attractively drawn) is more
				//reliable
				graphics.drawLine(parentXCoord,parentYCoord,
					leftChildXCoord,parentYCoord);
				if(!shoulderOnly) {
					graphics.drawLine(leftChildXCoord,parentYCoord,
						leftChildXCoord,leftChildYCoord);
				}

				//Draw an outline around the line to highlight a selected branch
				if(showSelections && isSelected) {
					graphics.setColor(new Color(249,238,160));//yellow
					graphics.drawPolyline(
						new int[] {
							parentXCoord,
							leftChildXCoord + 1,
							leftChildXCoord + 1
						},
						new int[] {
							parentYCoord + 1,
							parentYCoord + 1,
							leftChildYCoord - pointerBaseOffset
						},
						3);
					graphics.drawPolyline(
						new int[] {
							parentXCoord,
							leftChildXCoord - 1,
							leftChildXCoord - 1
						},
						new int[] {
							//Vertical coordinates
							parentYCoord - 1,
							parentYCoord - 1,
							leftChildYCoord - pointerBaseOffset
						},
						3);
				}
			}
		}

		/**
		 * Draws the right branch stemming from a given node
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

			int rightChildXCoord = 0;
			int rightChildYCoord = 0;
			int parentXCoord = 0;
			int parentYCoord = 0;

			int pointerBaseOffset = (right.isLeaf() ? 2 : 0);

			// GTRView
			if (isLeft) {
				rightChildXCoord = (int) xT.transform(right.getCorr());
				parentXCoord = (int) xT.transform(node.getCorr());

				rightChildYCoord = (int) yT.transform(right.getIndex() + .5);
				parentYCoord = (int) yT.transform(node.getIndex() + .5);

				if(Math.abs(rightChildXCoord - parentXCoord) < 3) {
					pointerBaseOffset = 0;
				}

			}
			// ATRView
			else {
				rightChildYCoord = (int) yT.transform(right.getCorr());
				parentYCoord = (int) yT.transform(node.getCorr());

				rightChildXCoord = (int) xT.transform(right.getIndex() + .5);
				parentXCoord = (int) xT.transform(node.getIndex() + .5);

				if(Math.abs(rightChildYCoord - parentYCoord) < 3) {
					pointerBaseOffset = 0;
				}
			}

			// draw our (flipped) polyline...
			if(hovered || isHovered) {
				graphics.setColor(Color.red);
			}

			if(isLeft) {
				graphics.drawPolyline(
					new int[] {
						rightChildXCoord,
						parentXCoord,
						parentXCoord
					},
					new int[] {
						rightChildYCoord,
						rightChildYCoord,
						parentYCoord
					},
					3);

				//If this is the hovered leaf, make it bold
				if(hovered || isSelected) {
					if(!hovered) {
						graphics.setColor(new Color(249,238,160));//yellow
					}
					graphics.drawPolyline(
						new int[] {
							rightChildXCoord - pointerBaseOffset,
							parentXCoord + 1,
							parentXCoord + 1
						},
						new int[] {
							rightChildYCoord - 1,
							rightChildYCoord - 1,
							parentYCoord
						},
						3);
					graphics.drawPolyline(
						new int[] {
							rightChildXCoord - pointerBaseOffset,
							parentXCoord - 1,
							parentXCoord - 1
						},
						new int[] {
							rightChildYCoord + 1,
							rightChildYCoord + 1,
							parentYCoord
						},
						3);
				}
			} else {
				graphics.drawPolyline(
					new int[] {
						rightChildXCoord,
						rightChildXCoord,
						parentXCoord
					},
					new int[] {
						rightChildYCoord,
						parentYCoord,
						parentYCoord
					},
					3);

				//If this is the hovered leaf, make it bold
				if(hovered || isSelected) {
					if(!hovered) {
						graphics.setColor(new Color(249,238,160));//yellow
					}
					graphics.drawPolyline(
						new int[] {
							rightChildXCoord + 1,
							rightChildXCoord + 1,
							parentXCoord
						},
						new int[] {
							rightChildYCoord - pointerBaseOffset,
							parentYCoord - 1,
							parentYCoord - 1
						},
						3);
					graphics.drawPolyline(
						new int[] {
							rightChildXCoord - 1,
							rightChildXCoord - 1,
							parentXCoord
						},
						new int[] {
							rightChildYCoord - pointerBaseOffset,
							parentYCoord + 1,
							parentYCoord + 1
						},
						3);
				}
			}
		}

		/**
		 * Export the right branch of a node
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
			final int startIndex,final int endIndex,
			final boolean showSelections) {

			if (node == null) {
				LogBuffer.println("node in drawSingle in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			final TreeDrawerNode right = node.getRight();

			if (right == null) {
				LogBuffer.println("right in drawSingle in InvertedTreeDrawer "
						+ "was null.");
				return;
			}

			int rightChildXCoord = 0;
			int rightChildYCoord = 0;
			int parentXCoord = 0;
			int parentYCoord = 0;

			int pointerBaseOffset = (right.isLeaf() ? 2 : 0);
			int minCoord = 0;
			int maxCoord = 0;
			boolean shoulderOnly = false;

			// GTRView
			if (isLeft) {
				rightChildXCoord =
					(int) Math.round((right.getCorr() - minCorr) /
					(maxCorr - minCorr) * (xIndent - 1));
				parentXCoord = (int) Math.round((node.getCorr() - minCorr) /
					(maxCorr - minCorr) * (xIndent - 1));

				rightChildYCoord =
					yIndent + (int) Math.round(right.getIndex() * size +
					(size / 2)) - startIndex * size;
				parentYCoord =
					yIndent + (int) Math.round(node.getIndex() * size +
					(size / 2)) - startIndex * size;

				//These values define the "horizontal" drawing area
				minCoord = yIndent;// + startIndex * size;
				maxCoord = yIndent + endIndex * size - startIndex * size + size;

				//This conditional adjusts coordinates to not overrun the min/
				//max boundaries and either sets a boolean that prevents
				//"vertical" lines which are completely outside the drawing area
				//from drawing or returns if both lines to be drawn are outside
				//the drawing area
				if((rightChildYCoord < minCoord && parentYCoord < minCoord) ||
					(rightChildYCoord > maxCoord && parentYCoord > maxCoord)) {

					return;
				} else if(rightChildYCoord >= minCoord &&
					parentYCoord >= minCoord &&
					rightChildYCoord <= maxCoord && parentYCoord <= maxCoord) {

					//Do nothing
				} else if(rightChildYCoord > maxCoord &&
					parentYCoord < minCoord) {

					shoulderOnly = true;
					parentYCoord = minCoord;
					rightChildYCoord = maxCoord;
				} else if(parentYCoord < minCoord) {
					parentYCoord = minCoord;
				} else if(rightChildYCoord > maxCoord) {
					shoulderOnly = true;
					rightChildYCoord = maxCoord;
				}

				if(Math.abs(rightChildXCoord - parentXCoord) < 3) {
					pointerBaseOffset = 0;
				}

			}
			// ATRView
			else {
				rightChildYCoord =
					(int) Math.round((right.getCorr() - minCorr) /
					(maxCorr - minCorr) * (yIndent - 1));
				parentYCoord = (int) Math.round((node.getCorr() - minCorr) /
					(maxCorr - minCorr) * (yIndent - 1));

				rightChildXCoord =
					xIndent + (int) Math.round(right.getIndex() * size +
					(size / 2)) - startIndex * size;
				parentXCoord =
					xIndent + (int) Math.round(node.getIndex() * size +
					(size / 2)) - startIndex * size;

				//These values define the "horizontal" drawing area
				minCoord = xIndent;// + startIndex * size;
				maxCoord = xIndent + endIndex * size - startIndex * size + size;

				//This conditional adjusts coordinates to not overrun the min/
				//max boundaries and either sets a boolean that prevents
				//"vertical" lines which are completely outside the drawing area
				//from drawing or returns if both lines to be drawn are outside
				//the drawing area
				if((rightChildXCoord < minCoord && parentXCoord < minCoord) ||
					(rightChildXCoord > maxCoord && parentXCoord > maxCoord)) {

					return;
				} else if(rightChildXCoord >= minCoord &&
					parentXCoord >= minCoord &&
					rightChildXCoord <= maxCoord && parentXCoord <= maxCoord) {

					//Do nothing
				} else if(rightChildXCoord > maxCoord &&
					parentXCoord < minCoord) {

					shoulderOnly = true;
					parentXCoord = minCoord;
					rightChildXCoord = maxCoord;
				} else if(parentXCoord < minCoord) {
					parentXCoord = minCoord;
				} else if(rightChildXCoord > maxCoord) {
					shoulderOnly = true;
					rightChildXCoord = maxCoord;
				}

				if(Math.abs(rightChildYCoord - parentYCoord) < 3) {
					pointerBaseOffset = 0;
				}
			}

			if (isLeft) {
				//The VectorGraphics2D library messes up the drawPolyline call
				//sometimes and skips the center point, so drawing two-
				//separate lines (while less attractively drawn) is more
				//reliable
				if(!shoulderOnly) {
					graphics.drawLine(rightChildXCoord,rightChildYCoord,
						parentXCoord,rightChildYCoord);
				}
				graphics.drawLine(parentXCoord,rightChildYCoord,
					parentXCoord,parentYCoord);

				//If this is selected, outline it
				if(showSelections && isSelected) {
					graphics.setColor(new Color(249,238,160));//yellow
					graphics.drawPolyline(
						new int[] {
							rightChildXCoord - pointerBaseOffset,
							parentXCoord + 1,
							parentXCoord + 1
						},
						new int[] {
							rightChildYCoord - 1,
							rightChildYCoord - 1,
							parentYCoord
						},
						3);
					graphics.drawPolyline(
						new int[] {
							rightChildXCoord - pointerBaseOffset,
							parentXCoord - 1,
							parentXCoord - 1
						},
						new int[] {
							rightChildYCoord + 1,
							rightChildYCoord + 1,
							parentYCoord
						},
						3);
				}
			} else {
				//The VectorGraphics2D library messes up the drawPolyline call
				//sometimes and skips the center point, so drawing two-
				//separate lines (while less attractiveleftChildYCoord drawn) is
				//more reliable
				if(!shoulderOnly) {
					graphics.drawLine(rightChildXCoord,rightChildYCoord,
						rightChildXCoord,parentYCoord);
				}
				graphics.drawLine(rightChildXCoord,parentYCoord,
					parentXCoord,parentYCoord);

				//If this is selected, outline it
				if(showSelections && isSelected) {
					graphics.setColor(new Color(249,238,160));//yellow
					graphics.drawPolyline(
						new int[] {
							rightChildXCoord + 1,
							rightChildXCoord + 1,
							parentXCoord
						},
						new int[] {
							rightChildYCoord - pointerBaseOffset,
							parentYCoord - 1,
							parentYCoord - 1
						},
						3);
					graphics.drawPolyline(
						new int[] {
							rightChildXCoord - 1,
							rightChildXCoord - 1,
							parentXCoord
						},
						new int[] {
							rightChildYCoord - pointerBaseOffset,
							parentYCoord + 1,
							parentYCoord + 1
						},
						3);
				}
			}
		}
	}
}
