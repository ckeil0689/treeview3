/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: InvertedTreeDrawer.java,v $
 * $Revision: 1.2 $
 * $Date: 2008-03-09 21:06:34 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular, 
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
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
			final TreeDrawerNode selected, final boolean isLeft) {

		if ((getRootNode() == null) || (getRootNode().isLeaf()))
			LogBuffer.println("Root node is null or leaf in paint() "
					+ "in InvertedTreeDrawer!");

		else {
			this.isLeft = isLeft;
			// recursively drawtree...
			final NodeDrawer nd = new NodeDrawer(graphics, xScaleEq, yScaleEq,
					selected, dest);
			nd.draw(getRootNode());
		}
	}

	// Used
	public void paintSubtree(final Graphics graphics,
			final LinearTransformation xScaleEq,
			final LinearTransformation yScaleEq, final Rectangle dest,
			final TreeDrawerNode root, final boolean isSelected,
			final boolean isLeft) {

		if ((root == null) || (root.isLeaf()) || (xScaleEq == null)
				|| (yScaleEq == null)) {
			return;

		} else {
			this.isLeft = isLeft;
			// recursively drawtree...
			final NodeDrawer nd = new NodeDrawer(graphics, xScaleEq, yScaleEq,
					null, dest);
			nd.isSelected = isSelected;
			nd.draw(root);
		}
	}

	public void paintSubtree(final Graphics graphics,
			final LinearTransformation xScaleEq,
			final LinearTransformation yScaleEq, final Rectangle dest,
			final TreeDrawerNode root, final TreeDrawerNode selected,
			final boolean isLeft) {

		if ((root == null) || (root.isLeaf())) {
			return;

		} else {
			this.isLeft = isLeft;
			// recursively drawtree...
			final NodeDrawer nd = new NodeDrawer(graphics, xScaleEq, yScaleEq,
					selected, dest);
			nd.draw(root);
		}
	}

	public void paintSingle(final Graphics graphics,
			final LinearTransformation xScaleEq,
			final LinearTransformation yScaleEq, final Rectangle dest,
			final TreeDrawerNode root, final boolean isSelected,
			final boolean isLeft) {

		if ((root == null) || (root.isLeaf())) {
			return;

		} else {
			this.isLeft = isLeft;
			// just draw single..
			final NodeDrawer nd = new NodeDrawer(graphics, xScaleEq, yScaleEq,
					null, dest);
			nd.isSelected = isSelected;
			if (!root.isLeaf()) {
				nd.drawSingle(root);

			} else {
				LogBuffer.println("Root was leaf?");
			}
		}
	}

	/**
	 * this is an internal helper class which does a sort of recursive drawing
	 * 
	 * @author Alok Saldanha <alok@genome.stanford.edu>
	 * @version Alpha
	 */
	class NodeDrawer {

		private final Color sel_color = GUIFactory.MAIN;
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
			dest = d;

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
		public void draw(final TreeDrawerNode startNode) {

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
				drawSingle(node);
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
		 * graphics.setColor(sel_color); else graphics.setColor(t);
		 * 
		 * graphics.drawPolyline(new int[] {rx, rx, lx, lx}, new int[] {ry, ty,
		 * ty, ly}, 4); if (left.isLeaf() == false) draw(left); if
		 * (right.isLeaf() == false) draw(right); if (isSelected)
		 * graphics.setColor(t); }
		 */

		private void drawSingle(final TreeDrawerNode node) {

			final TreeDrawerNode left = node.getLeft();
			final TreeDrawerNode right = node.getRight();

			if (xT == null) {
				LogBuffer.println("xt in drawSingle in InvertedTreeDrawer "
						+ "was null.");
			}

			if (right == null) {
				LogBuffer.println("right in drawSingle in InvertedTreeDrawer "
						+ "was null.");
			}

			int rx = 0;
			int lx = 0;
			int tx = 0;

			int ry = 0;
			int ly = 0;
			int ty = 0;

			// GTRView
			if (isLeft) {
				rx = (int) xT.transform(right.getCorr());
				lx = (int) xT.transform(left.getCorr());
				tx = (int) xT.transform(node.getCorr());

				ry = (int) yT.transform(right.getIndex() + .5);
				ly = (int) yT.transform(left.getIndex() + .5);

			// ATRView
			} else {
				ry = (int) yT.transform(right.getCorr());
				ly = (int) yT.transform(left.getCorr());
				ty = (int) yT.transform(node.getCorr());

				rx = (int) xT.transform(right.getIndex() + .5);
				lx = (int) xT.transform(left.getIndex() + .5);
				// int tx = (int) xT.transform(node.getIndex() + .5);
			}

			// draw our (flipped) polyline...
			if (isSelected) {
				graphics.setColor(sel_color);

			} else {
				graphics.setColor(node.getColor());
			}

			if (isLeft) {
				graphics.drawPolyline(new int[] { rx, tx, tx, lx }, new int[] {
						ry, ry, ly, ly }, 4);

			} else {
				graphics.drawPolyline(new int[] { rx, rx, lx, lx }, new int[] {
						ry, ty, ty, ly }, 4);
			}

			// graphics.setColor(t);
		}
	}
}
