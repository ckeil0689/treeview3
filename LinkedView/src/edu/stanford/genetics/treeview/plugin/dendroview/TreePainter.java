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

import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.LinearTransformation;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeDrawerNode;

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
			final LinearTransformation yScaleEq, final Rectangle dest,
			final TreeDrawerNode selected, final boolean isLeft,
			final int hoverIndex,final int minVisLabelIndex,
			final int maxVisLabelIndex) {

		if ((getRootNode() == null) || (getRootNode().isLeaf())) {
			LogBuffer.println("Root node is null or leaf in paint() "
					+ "in InvertedTreeDrawer!");
		} else {
			this.isLeft = isLeft;
			// recursively drawtree...
			final NodeDrawer nd = new NodeDrawer(graphics, xScaleEq, yScaleEq,
					selected, dest);
			nd.draw(getRootNode(),hoverIndex,false,minVisLabelIndex,
				maxVisLabelIndex);
		}
	}

	// Used
	public void paintSubtree(final Graphics graphics,
			final LinearTransformation xScaleEq,
			final LinearTransformation yScaleEq, final Rectangle dest,
			final TreeDrawerNode root, final boolean isSelected,
			final boolean isLeft,final int hoverIndex,
			final boolean isNodeHovered,final int minVisLabelIndex,
			final int maxVisLabelIndex) {

		if ((root == null) || (root.isLeaf()) || (xScaleEq == null)
				|| (yScaleEq == null))
			return;
		this.isLeft = isLeft;
		
		// recursively drawtree...
		final NodeDrawer nd = new NodeDrawer(graphics, xScaleEq, yScaleEq,
				null, dest);
		nd.isSelected = isSelected;
		nd.draw(root,hoverIndex,isNodeHovered,minVisLabelIndex,
			maxVisLabelIndex);
	}

	public void paintSubtree(final Graphics graphics,
			final LinearTransformation xScaleEq,
			final LinearTransformation yScaleEq, final Rectangle dest,
			final TreeDrawerNode root, final TreeDrawerNode selected,
			final boolean isLeft,final int hoverIndex,
			final boolean isNodeHovered,final int minVisLabelIndex,
			final int maxVisLabelIndex) {

		if ((root == null) || (root.isLeaf()))
			return;
		
		this.isLeft = isLeft;
		// recursively drawtree...
		final NodeDrawer nd = new NodeDrawer(graphics, xScaleEq, yScaleEq,
				selected, dest);
		nd.draw(root,hoverIndex,isNodeHovered,minVisLabelIndex,
			maxVisLabelIndex);
	}

	public void paintSingle(final Graphics graphics,
			final LinearTransformation xScaleEq,
			final LinearTransformation yScaleEq, final Rectangle dest,
			final TreeDrawerNode root, final boolean isSelected,
			final boolean isLeft, int hoverIndex,final boolean isNodeHovered,
			final int minVisLabelIndex,final int maxVisLabelIndex) {

		if ((root == null) || (root.isLeaf()))
			return;
		this.isLeft = isLeft;
		// just draw single..
		final NodeDrawer nd = new NodeDrawer(graphics, xScaleEq, yScaleEq,
				null, dest);
		nd.isSelected = isSelected;
		if (!root.isLeaf()) {
			nd.drawSingle(root,hoverIndex,isNodeHovered,false,minVisLabelIndex,
				maxVisLabelIndex);

		} else {
			LogBuffer.println("Root was leaf?");
		}
	}

	/**
	 * this is an internal helper class which does a sort of recursive drawing
	 *
	 * @author Alok Saldanha <alok@genome.stanford.edu>
	 * @version Alpha
	 */
	class NodeDrawer {

		private final Color whiz_color = GUIFactory.MAIN;
		private final Graphics graphics;
		private final TreeDrawerNode selected;
		private final LinearTransformation xT, yT;

		private final double minInd;
		private final double maxInd;
		private final Rectangle dest;
		private boolean isSelected = false;

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
				final LinearTransformation yScaleEq, final TreeDrawerNode sel,
				final Rectangle d) {

			graphics = g;
			selected = sel;
			xT = xScaleEq;
			yT = yScaleEq;
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
		 * the draw method actually does the drawing
		 */
		public void draw(final TreeDrawerNode startNode,final int hoverIndex,
			final boolean isNodeHovered,final int minVisLabelIndex,
			final int maxVisLabelIndex) {

			final Stack<TreeDrawerNode> remaining = new Stack<TreeDrawerNode>();
			remaining.push(startNode);

			while (!remaining.empty()) {

				final TreeDrawerNode node = remaining.pop();

				// just return if no subkids visible.
				if ((node.getMaxIndex() < minInd)
						|| (node.getMinIndex() > maxInd)) {
					continue;
				}

				// handle selection...
				if (node == selected) {
					if (isSelected == false) {
						isSelected = true;

						// push onto stack, so we know when we're finished
						// with the selected subtree..
						remaining.push(selected);

					} else {
						// isSelected is true, so we're pulling the selected
						// node off the second time.
						isSelected = false;
						continue;
					}
				}

				// lots of stack allocation...
				final TreeDrawerNode left = node.getLeft();
				final TreeDrawerNode right = node.getRight();
				if (!left.isLeaf()) {
					remaining.push(left);
				}

				if (!right.isLeaf()) {
					remaining.push(right);
				}

				// finally draw
				drawSingle(node,hoverIndex,isNodeHovered,(node == startNode),
					minVisLabelIndex,maxVisLabelIndex);
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
			final int maxVisLabelIndex) {

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
				isNodeHovered);

			if(isNodeHovered) {
				graphics.setColor(Color.red);
			} else {
				graphics.setColor(node.getColor());
			}

			drawRightBranch(node,hoverIndex,minVisLabelIndex,maxVisLabelIndex,
				isNodeHovered);

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
			if((isTop && (isSelected || isNodeHovered)) || node == selected) {
				if(!isNodeHovered) {
					graphics.setColor(new Color(197,181,66));//dark yellow
				}
				graphics.fillRect(x,y,5,5);
			}
		}

		public void drawLeftBranch(final TreeDrawerNode node,
			final int hoverIndex,final int minVisLabelIndex,
			final int maxVisLabelIndex,final boolean isHovered) {

			final TreeDrawerNode left = node.getLeft();
			final boolean hovered = (node.getLeft().getIndex() == hoverIndex);
			final TreeDrawerNode leftLeaf = node.getLeftLeaf();
			final TreeDrawerNode rightLeaf = node.getRightLeaf();
//			boolean allLabelsVisible = false;
//			boolean leftLabelVisible = false;
//			if((int) leftLeaf.getIndex() >= minVisLabelIndex &&
//				(int) rightLeaf.getIndex() <= maxVisLabelIndex) {
//				allLabelsVisible = true;
//			} else if(left.isLeaf() &&
//				(int) left.getIndex() >= minVisLabelIndex &&
//				(int) left.getIndex() <= maxVisLabelIndex) {
//				leftLabelVisible = true;
//			}

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

			int pointerBaseOffset = (left.isLeaf() ? 2 : 0);

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
//			} else if(allLabelsVisible || leftLabelVisible) {
//				graphics.setColor(whiz_color);
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
			final int maxVisLabelIndex,final boolean isHovered) {

			final TreeDrawerNode right = node.getRight();
			final boolean hovered = (node.getRight().getIndex() == hoverIndex);
			final TreeDrawerNode leftLeaf = node.getLeftLeaf();
			final TreeDrawerNode rightLeaf = node.getRightLeaf();
//			boolean allLabelsVisible  = false;
//			boolean rightLabelVisible = false;
//			if((int) leftLeaf.getIndex() >= minVisLabelIndex &&
//				(int) rightLeaf.getIndex() <= maxVisLabelIndex) {
//				allLabelsVisible  = true;
//				rightLabelVisible = true;
//			} else if(right.isLeaf() &&
//				(int) right.getIndex() >= minVisLabelIndex &&
//				(int) right.getIndex() <= maxVisLabelIndex) {
//				rightLabelVisible = true;
//			}

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
//			} else if(allLabelsVisible || rightLabelVisible) {
//				graphics.setColor(whiz_color);
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
