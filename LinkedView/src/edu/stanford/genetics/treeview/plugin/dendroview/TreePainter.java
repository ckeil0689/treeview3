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

	@Override
	public void paint(final Graphics graphics,
		final LinearTransformation xScaleEq,
		final LinearTransformation yScaleEq,final Rectangle dest,
		final boolean isLeft,final int hoverIndex,final int minVisLabelIndex,
		final int maxVisLabelIndex,final TreeSelectionI treeSelection,
		final TreeDrawerNode hoveredNode) {

		if ((getRootNode() == null) || (getRootNode().isLeaf())) {
			LogBuffer.println("Root node is null or leaf in paint() "
					+ "in InvertedTreeDrawer!");
		} else {
			this.isLeft = isLeft;
			// recursively drawtree...
			final NodeDrawer nd =
				new NodeDrawer(graphics,xScaleEq,yScaleEq,dest,hoveredNode);
			nd.draw(getRootNode(),hoverIndex,false,minVisLabelIndex,
				maxVisLabelIndex,true,false,treeSelection);
		}
	}

	public void paintSubtree(final Graphics graphics,
		final LinearTransformation xScaleEq,
		final LinearTransformation yScaleEq, final Rectangle dest,
		final TreeDrawerNode root, final boolean isSelected,
		final boolean isLeft,final int hoverIndex,
		final boolean isNodeHovered,final int minVisLabelIndex,
		final int maxVisLabelIndex,final TreeSelectionI treeSelection,
		final TreeDrawerNode hoveredNode) {

		if ((root == null) || (root.isLeaf()) || (xScaleEq == null)
				|| (yScaleEq == null))
			return;
		this.isLeft = isLeft;
		
		// recursively drawtree...
		final NodeDrawer nd =
			new NodeDrawer(graphics,xScaleEq,yScaleEq,dest,hoveredNode);
		nd.draw(root,hoverIndex,isNodeHovered,minVisLabelIndex,
			maxVisLabelIndex,true,false,treeSelection);
	}

	/**
	 * This class is used to store where to draw selected node dots while the
	 * tree is drawn in DFS order.  These objects are passed back to parent
	 * nodes in a stack.  They store whether they are selected or not.
	 * @author rleach
	 *
	 */
	class NodeStackItem {
		TreeDrawerNode node;
		boolean isSelected;
		public NodeStackItem(final TreeDrawerNode node,
			final boolean isSelected) {

			this.node = node;
			this.isSelected = isSelected;
		}
		/**
		 * @author rleach
		 * @return the node
		 */
		public TreeDrawerNode getNode() {
			return(node);
		}
		/**
		 * @author rleach
		 * @param node the node to set
		 */
		public void setNode(TreeDrawerNode node) {
			this.node = node;
		}
		/**
		 * @author rleach
		 * @return the isSelected
		 */
		public boolean isSelected() {
			return(isSelected);
		}
		/**
		 * @author rleach
		 * @param isSelected the isSelected to set
		 */
		public void setSelected(boolean isSelected) {
			this.isSelected = isSelected;
		}
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
		 */
		public NodeDrawer(final Graphics g,
			final LinearTransformation xScaleEq,
			final LinearTransformation yScaleEq,final Rectangle d,final TreeDrawerNode hoveredNode) {

			graphics = g;
			xT = xScaleEq;
			yT = yScaleEq;
			this.hoveredNode = hoveredNode;
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

		public void draw(final TreeDrawerNode node,final int hoverIndex,
 			final boolean isNodeHovered,final int minVisLabelIndex,
 			final int maxVisLabelIndex,final boolean isTop,
 			boolean isSelected,final TreeSelectionI treeSelection) {
			Stack<NodeStackItem> dotNodeStack =
				drawDFS(node,hoverIndex,isNodeHovered,minVisLabelIndex,
					maxVisLabelIndex,isTop,isSelected,treeSelection);
			drawNodeDots(dotNodeStack);
		}

		/**
		 * the draw method actually does the drawing
		 */
		public Stack<NodeStackItem> drawDFS(final TreeDrawerNode node,
			final int hoverIndex,final boolean isNodeHovered,
			final int minVisLabelIndex,
			final int maxVisLabelIndex,final boolean isTop,
			boolean isSelected,final TreeSelectionI treeSelection) {

			Stack<NodeStackItem> returnStack = new Stack<NodeStackItem>();

			// just return if no subkids visible.
			if((node.getMaxIndex() < minInd) ||
				(node.getMinIndex() > maxInd)) {
				LogBuffer.println("Returning because kids aren't visible");
				if(isNodeSelected(node,treeSelection)) {
					LogBuffer.println("Adding this node to the stack.");
					returnStack.push(new NodeStackItem(node,true));
				}
				return(returnStack);
			}

			//These will keep track of the selected nodes below us
			Stack<NodeStackItem> leftDotNodeStack  = new Stack<NodeStackItem>();
			Stack<NodeStackItem> rightDotNodeStack = new Stack<NodeStackItem>();

			//Recursive calls (leaves will be drawn first)
			if(!node.getLeft().isLeaf()) {
				leftDotNodeStack = drawDFS(node.getLeft(),hoverIndex,
					isNodeHovered,minVisLabelIndex,maxVisLabelIndex,false,
					isSelected,treeSelection);
			}
			//We do not recurse down to the leaves, so add them to the stack
			//here
			else if(treeSelection.isIndexSelected(
				(int) node.getLeft().getIndex())) {

				leftDotNodeStack.push(
					new NodeStackItem(node.getLeft(),true));
			}

			if(!node.getRight().isLeaf()) {
				rightDotNodeStack = drawDFS(node.getRight(),hoverIndex,
					isNodeHovered,minVisLabelIndex,maxVisLabelIndex,false,
					isSelected,treeSelection);
			}
			//We do not recurse down to the leaves, so add them to the stack
			//here
			else if(treeSelection.isIndexSelected(
				(int) node.getRight().getIndex())) {

				rightDotNodeStack.push(
					new NodeStackItem(node.getRight(),true));
			}

			boolean thisNodeIsSelected = false;

			//If the stack returned from each child contains 1 selected
			//node and that node is the child of this node, then we're not going
			//to draw dots for those nodes and just draw a dot for this node.
			//Ignore the child nodes and push this node onto a new stack to
			//return.  Otherwise, all all the selected subtrees & leaves on top
			//of which to draw dots.
			if(!leftDotNodeStack.isEmpty() && !rightDotNodeStack.isEmpty() &&
				leftDotNodeStack.size() == 1 && rightDotNodeStack.size() == 1 &&
				leftDotNodeStack.peek().getNode().getId() ==
				node.getLeft().getId() &&
				rightDotNodeStack.peek().getNode().getId() ==
				node.getRight().getId()) {

				thisNodeIsSelected = (leftDotNodeStack.peek().isSelected() &&
					rightDotNodeStack.peek().isSelected());
				returnStack.push(new NodeStackItem(node,thisNodeIsSelected));
			} else {
				if(!leftDotNodeStack.isEmpty()) {
					returnStack.addAll(leftDotNodeStack);
				}
				if(!rightDotNodeStack.isEmpty()) {
					returnStack.addAll(rightDotNodeStack);
				}
			}

			// finally draw
			drawSingle(node,hoverIndex,isNodeHovered,isTop,
				minVisLabelIndex,maxVisLabelIndex,thisNodeIsSelected);

			return(returnStack);
		}

		/**
		 * @author rleach
		 * @param 
		 * @return 
		 * @param node
		 * @param treeSelection
		 * @return
		 */
		private boolean isNodeSelected(TreeDrawerNode node,
			TreeSelectionI treeSelection) {
			boolean selected = true;
			for(int i = (int) node.getLeftLeaf().getIndex();
				i <= (int) node.getRightLeaf().getIndex();i++) {
				if(!treeSelection.isIndexSelected(i)) {
					LogBuffer.println("Index [" + i + "] is not selected");
					selected = false;
					break;
				}
			}
			return(selected);
		}

		/**
		 * @author rleach
		 * @return the hoveredNode
		 */
		public TreeDrawerNode getHoveredNode() {
			return(hoveredNode);
		}

		/**
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
		 * @param isTop
		 */
		public void drawNodeDot(final TreeDrawerNode node,
			final boolean isSelected) {

			int x = 0;
			int y = 0;

			if(isLeft) {
				x = (int) xT.transform(node.getCorr()) - 2;
				y = (int) yT.transform(node.getIndex() + .5) - 2;
			} else {
				y = (int) yT.transform(node.getCorr()) - 2;
				x = (int) xT.transform(node.getIndex() + .5) - 2;
			}

			//If this is the top selected/hovered node, draw a dot at its root
			if(!isSelected) {
				return;
			}
			graphics.setColor(new Color(197,181,66));//dark yellow
			graphics.fillRect(x,y,5,5);
		}

		public void drawNodeDots(final Stack<NodeStackItem> nodeStack) {
			if(nodeStack == null || nodeStack.isEmpty()) {
				return;
			}
			while(!nodeStack.isEmpty()) {
				NodeStackItem nodeItem = nodeStack.pop();
				drawNodeDot(nodeItem.getNode(),nodeItem.isSelected());
			}
		}

		/*
		 * // just return if no subkids visible. if ((node.getMaxIndex() <
		 * minInd) || (node.getMinIndex() > maxInd)) return;
		 * 
		 * // lots of stack allocation... TreeDrawerNode left = node.getLeft();
		 * TreeDrawerNode right = node.getRight();
		 * 
		 * int ry = (int) yT.transform(right.getCorr()); int ly = (int)
		 * yT.transform(left.getCorr()); int ty = (int)
		 * yT.transform(node.getCorr());
		 * 
		 * int rx = (int) xT.transform(right.getIndex() + .5); int lx = (int)
		 * xT.transform(left.getIndex() + .5); int tx = (int)
		 * xT.transform(node.getIndex() + .5); Color t = graphics.getColor();
		 * 
		 * isSelected = (node == selected); // System.out.println("rx = " + rx +
		 * ", ry = " + ry + ", lx = " + lx + ", ly = " + ly);
		 * 
		 * // oval first?... // graphics.setColor(node_color); //
		 * graphics.drawOval(tx - 1,ty - 1,2,2);
		 * 
		 * //draw our (flipped) polyline... if (isSelected)
		 * graphics.setColor(whiz_color); else graphics.setColor(t);
		 * 
		 * graphics.drawPolyline(new int[] {rx, rx, lx, lx}, new int[] {ry, ty,
		 * ty, ly}, 4); if (left.isLeaf() == false) draw(left); if
		 * (right.isLeaf() == false) draw(right); if (isSelected)
		 * graphics.setColor(t); }
		 */

		private void drawSingle(final TreeDrawerNode node,
			final int hoverIndex,final boolean isNodeHovered,
			final boolean isTop,final int minVisLabelIndex,
			final int maxVisLabelIndex,final boolean isSelected) {

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

			drawLeftBranch(node,hoverIndex,minVisLabelIndex,maxVisLabelIndex,
				isNodeHovered,isSelected);

			if(isNodeHovered) {
				graphics.setColor(Color.red);
			} else {
				graphics.setColor(node.getColor());
			}

			drawRightBranch(node,hoverIndex,minVisLabelIndex,maxVisLabelIndex,
				isNodeHovered,isSelected);
		}

		public void drawLeftBranch(final TreeDrawerNode node,
			final int hoverIndex,final int minVisLabelIndex,
			final int maxVisLabelIndex,final boolean isHovered,
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

			// draw our (flipped) polyline...
			if(hovered || isHovered) {
				graphics.setColor(Color.red);
			}

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
						c,        //center
						lx,       //left leaf corner
						lx        //left leaf end
					},
					new int[] {
						//Vertical coordinates
						ty,       //center
						ty,       //left leaf corner
						ly        //left leaf end
					},
					3);

				if(hovered || isSelected) {
					if(!hovered) {
						graphics.setColor(new Color(249,238,160));//yellow
					}
					graphics.drawPolyline(
						new int[] {
							//Horizontal coordinates
							c,            //center
							lx + 1,       //left leaf corner
							lx + 1        //left leaf end
						},
						new int[] {
							//Vertical coordinates
							ty + 1,       //center
							ty + 1,       //left leaf corner
							ly - pointerBaseOffset        //left leaf end
						},
						3);
					graphics.drawPolyline(
						new int[] {
							//Horizontal coordinates
							c,            //center
							lx - 1,       //left leaf corner
							lx - 1        //left leaf end
						},
						new int[] {
							//Vertical coordinates
							ty - 1,       //center
							ty - 1,       //left leaf corner
							ly - pointerBaseOffset        //left leaf end
						},
						3);
				}
			}
		}

		private void drawRightBranch(final TreeDrawerNode node,
			final int hoverIndex,final int minVisLabelIndex,
			final int maxVisLabelIndex,final boolean isHovered,
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

				// ATRView
			} else {
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
							tx + 1,       //right leaf corner
							tx + 1        //center
						},
						new int[] {
							//Vertical coordinates
							ry - 1,       //right leaf end
							ry - 1,       //right leaf corner
							c             //center
						},
						3);
					graphics.drawPolyline(
						new int[] {
							//Horizontal coordinates
							rx - pointerBaseOffset,       //right leaf end
							tx - 1,       //right leaf corner
							tx - 1        //center
						},
						new int[] {
							//Vertical coordinates
							ry + 1,       //right leaf end
							ry + 1,       //right leaf corner
							c             //center
						},
						3);
				}
			} else {
				graphics.drawPolyline(
					new int[] {
						//Horizontal coordinates
						rx,       //right leaf end
						rx,       //right leaf corner
						c         //center
					},
					new int[] {
						//Vertical coordinates
						ry,       //right leaf end
						ty,       //right leaf corner
						ty        //center
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
							rx + 1,       //right leaf end
							rx + 1,       //right leaf corner
							c             //center
						},
						new int[] {
							//Vertical coordinates
							ry - pointerBaseOffset,       //right leaf end
							ty - 1,       //right leaf corner
							ty - 1        //center
						},
						3);
					graphics.drawPolyline(
						new int[] {
							//Horizontal coordinates
							rx - 1,       //right leaf end
							rx - 1,       //right leaf corner
							c             //center
						},
						new int[] {
							//Vertical coordinates
							ry - pointerBaseOffset,       //right leaf end
							ty + 1,       //right leaf corner
							ty + 1        //center
						},
						3);
				}
			}
		}
	}
}
