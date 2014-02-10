/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: LeftTreeDrawer.java,v $
 * $Revision: 1.2 $
 * $Date: 2008-03-09 21:06:33 $
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

import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.LinearTransformation;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeDrawerNode;

/**
 * Class for drawing GTR-style trees rooted on the left
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version Alpha
 */

public class LeftTreeDrawer extends TreeDrawer {

	@Override
	public void paint(final Graphics graphics,
			final LinearTransformation xScaleEq,
			final LinearTransformation yScaleEq, final Rectangle dest,
			final TreeDrawerNode selected) {

		if ((getRootNode() == null) || (getRootNode().isLeaf() == true)) {
			System.out.println("Root node is null or leaf!");

		} else {
			// recursively drawtree...
			final NodeDrawer nd = new NodeDrawer(graphics, xScaleEq, yScaleEq,
					selected, dest);
			nd.draw(getRootNode());
		}
	}

	public void paintSubtree(final Graphics graphics,
			final LinearTransformation xScaleEq,
			final LinearTransformation yScaleEq, final Rectangle dest,
			final TreeDrawerNode root, final boolean isSelected) {

		if ((root == null) || (root.isLeaf() == true)) {
			return;

		} else {
			if (yScaleEq == null) {
				LogBuffer.println("yScaleEq was null in "
						+ "LeftTreeDrawer.paintSubTree!");
				final Exception e = new Exception();
				e.printStackTrace();
			}

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
			final TreeDrawerNode root, final TreeDrawerNode selected) {

		if ((root == null) || (root.isLeaf() == true)) {
			return;

		} else {
			// recursively drawtree...
			final NodeDrawer nd = new NodeDrawer(graphics, xScaleEq, yScaleEq,
					selected, dest);
			nd.draw(root);
		}
	}

	public void paintSingle(final Graphics graphics,
			final LinearTransformation xScaleEq,
			final LinearTransformation yScaleEq, final Rectangle dest,
			final TreeDrawerNode root, final boolean isSelected) {

		if ((root == null) || (root.isLeaf() == true)) {
			return;

		} else {
			// just draw single..
			final NodeDrawer nd = new NodeDrawer(graphics, xScaleEq, yScaleEq,
					null, dest);
			nd.isSelected = isSelected;
			if (root.isLeaf() == false) {
				nd.drawSingle(root);

			} else {
				System.err.println("Root was leaf?");
			}
		}
	}

	/**
	 * this is an internal helper class which does a sort of recursive drawing
	 * that's actually implemented with iteration.
	 * 
	 * @author Alok Saldanha <alok@genome.stanford.edu>
	 * @version Alpha
	 */
	class NodeDrawer {

		private final Color sel_color = GUIParams.MAIN;
		private Graphics graphics;
		private TreeDrawerNode selected;
		private LinearTransformation xT;
		private LinearTransformation yT;

		private double minInd;
		private double maxInd;
		private Rectangle dest;
		boolean isSelected = false;

		/**
		 * The constructor sets the variables
		 * 
		 * @param g
		 *            The graphics object to print to
		 * @param xScaleEq
		 *            The equation to be applied to scale the index of the nodes
		 *            to graphics object.
		 * @param yScaleEq
		 *            The equation to be applied to scale the correlation of the
		 *            nodes to the graphics object. maybe foreground color,
		 *            selection color and node color should be options?
		 */
		public NodeDrawer(final Graphics g,
				final LinearTransformation xScaleEq,
				final LinearTransformation yScaleEq, final TreeDrawerNode sel,
				final Rectangle d) {

			if (yScaleEq == null) {
				LogBuffer.println("yScaleEq was null!");
				return;
			}

			graphics = g;
			selected = sel;
			xT = xScaleEq;
			yT = yScaleEq;
			dest = d;

			if (dest != null) {
				minInd = (int) yScaleEq.inverseTransform(dest.y);
				maxInd = (int) yScaleEq.inverseTransform(dest.y + dest.height) 
						+ 1;
			}
		}

		/**
		 * the draw method actually does the drawing
		 */
		public void draw(final TreeDrawerNode startNode) {

			final Stack<TreeDrawerNode> remaining = new Stack<TreeDrawerNode>();
			remaining.push(startNode);

			while (remaining.empty() == false) {

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
						// node
						// off the second time.
						isSelected = false;
						continue;
					}
				}

				// lots of stack allocation...
				final TreeDrawerNode left = node.getLeft();
				final TreeDrawerNode right = node.getRight();
				if (left.isLeaf() == false) {
					remaining.push(left);
				}

				if (right.isLeaf() == false) {
					remaining.push(right);
				}
				// finally draw
				drawSingle(node);
			}
		}

		private void drawSingle(final TreeDrawerNode node) {

			final TreeDrawerNode left = node.getLeft();
			final TreeDrawerNode right = node.getRight();

			if (xT == null) {
				System.err.println("xt was null");
			}

			if (right == null) {
				System.err.println("right was null");
			}

			final int rx = (int) xT.transform(right.getCorr());
			final int lx = (int) xT.transform(left.getCorr());
			final int tx = (int) xT.transform(node.getCorr());

			final int ry = (int) yT.transform(right.getIndex() + .5);
			final int ly = (int) yT.transform(left.getIndex() + .5);
			// int ty = (int) yT.transform(node.getIndex() + .5);

			// draw our (flipped) polyline...
			if (isSelected) {
				graphics.setColor(sel_color);

			} else {
				graphics.setColor(node.getColor());
			}

			graphics.drawPolyline(new int[] { rx, tx, tx, lx }, new int[] { ry,
					ry, ly, ly }, 4);

		}
	}
}
