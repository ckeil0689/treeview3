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

import edu.stanford.genetics.treeview.*;

import java.awt.*;
import java.util.Stack;
/**
 * Class for drawing ATR-style inverted trees
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version Alpha
 */

class InvertedTreeDrawer extends TreeDrawer {
    public void paint(Graphics graphics, LinearTransformation xScaleEq, 
		      LinearTransformation yScaleEq, Rectangle dest, 
		      TreeDrawerNode selected) {	
	if ((getRootNode() == null) || (getRootNode().isLeaf() == true))
	    System.out.println("Root node is null or leaf!");
	else {
	    // recursively drawtree...
	    NodeDrawer nd = new NodeDrawer(graphics, xScaleEq, yScaleEq, 
					   selected, dest);
	    nd.draw(getRootNode());
	}
    }

	public void paintSubtree(Graphics graphics, LinearTransformation xScaleEq, 
	LinearTransformation yScaleEq, Rectangle dest, 
	TreeDrawerNode root, boolean isSelected) {	
	  if ((root == null) || (root.isLeaf() == true) || 
			  (xScaleEq == null) || (yScaleEq == null))
		return;
		else {
		  // recursively drawtree...
		  NodeDrawer nd = new NodeDrawer(
		  graphics, xScaleEq, yScaleEq, 
		  null, dest);
		  nd.isSelected = isSelected;
		  nd.draw(root);
		}
	}

	public void paintSubtree(Graphics graphics, LinearTransformation xScaleEq, 
		      LinearTransformation yScaleEq, Rectangle dest, 
		      TreeDrawerNode root, 
			  TreeDrawerNode selected) {	
		if ((root == null) || (root.isLeaf() == true))
		  return;
		else {
		  // recursively drawtree...
		  NodeDrawer nd = new NodeDrawer(
			graphics, xScaleEq, yScaleEq, 
			selected, dest);
			nd.draw(root);
		}
    }

    public void paintSingle(Graphics graphics, LinearTransformation xScaleEq, 
		      LinearTransformation yScaleEq, Rectangle dest, 
		      TreeDrawerNode root, boolean isSelected) {	
		if ((root == null) || (root.isLeaf() == true))
		  return;
		else {
		  // just draw single..
		  NodeDrawer nd = new NodeDrawer(
			graphics, xScaleEq, yScaleEq, 
			null, dest);
			nd.isSelected = isSelected;
			if (root.isLeaf() == false)
			  nd.drawSingle(root);
			else
			  System.err.println("Root was leaf?");
		}
    }

	
	
    /**
     * this is an internal helper class which does a sort of recursive drawing
     * @author Alok Saldanha <alok@genome.stanford.edu>
     * @version Alpha
     */
    class NodeDrawer {
	/**
	 * The constructor sets the variables
	 *
	 * @param g         The graphics object to print to

	 * @param xScaleEq The equation to be applied to scale the
	 * index of the nodes to graphics object

	 * @param yScaleEq The equation to be applied to scale the
	 * correlation of the nodes to the graphics object

	 * maybe foreground color, selection color and node color should be options?
	 */
	public NodeDrawer(Graphics g, LinearTransformation xScaleEq, 
			  LinearTransformation yScaleEq, TreeDrawerNode sel,
			  Rectangle d) {
	    graphics = g;
	    selected = sel;
	    xT = xScaleEq;
	    yT = yScaleEq;
	    dest = d;
	    minInd = (int) xScaleEq.inverseTransform(dest.x);
	    maxInd = (int) xScaleEq.inverseTransform(dest.x + dest.width) + 
		1;
	}

	/** 
	 * the draw method actually does the drawing
	 */
	public void draw(TreeDrawerNode startNode) {
		Stack remaining = new Stack();
		remaining.push(startNode);
		while (remaining.empty() == false) {
			TreeDrawerNode node = (TreeDrawerNode) remaining.pop();

			// just return if no subkids visible.
			if ((node.getMaxIndex() < minInd) ||
					(node.getMinIndex() > maxInd))
				continue;
			// handle selection...
			if (node == selected) {
				if (isSelected == false) {
					isSelected = true;
					// push onto stack, so we know when we're finished with the selected subtree..
					remaining.push(selected);
				} else {
					// isSelected is true, so we're pulling the selected node off the second time.
					isSelected = false;
					continue;
				}
			}
			// lots of stack allocation...
			TreeDrawerNode left = node.getLeft();
			TreeDrawerNode right = node.getRight();
			if (left.isLeaf() == false) remaining.push(left);
			if (right.isLeaf() == false) remaining.push(right);
			// finally draw
			drawSingle(node);
		}
	}
/*
	    // just return if no subkids visible.
	    if ((node.getMaxIndex() < minInd) ||
		(node.getMinIndex() > maxInd))
		return;

	    // lots of stack allocation...
	    TreeDrawerNode left = node.getLeft();
	    TreeDrawerNode right = node.getRight();
	    
	    int ry = (int) yT.transform(right.getCorr());
	    int ly = (int) yT.transform(left.getCorr());
	    int ty = (int) yT.transform(node.getCorr());
	    
	    int rx = (int) xT.transform(right.getIndex() + .5);
	    int lx = (int) xT.transform(left.getIndex() + .5);
	    int tx = (int) xT.transform(node.getIndex() + .5);
	    Color t = graphics.getColor();
	    
	    isSelected = (node == selected);
	    //	System.out.println("rx = " + rx + ", ry = " + ry + ", lx = " + lx + ", ly = " + ly);
	    
	    // oval first?...
//	    graphics.setColor(node_color);
//	    graphics.drawOval(tx - 1,ty - 1,2,2);

	    //draw our (flipped) polyline...
	    if (isSelected) 
		graphics.setColor(sel_color);
	    else
		graphics.setColor(t);

	    graphics.drawPolyline(new int[] {rx, rx, lx, lx},
			   new int[] {ry, ty, ty, ly}, 4);
	    if (left.isLeaf() == false) draw(left); 
	    if (right.isLeaf() == false) draw(right);	    
	    if (isSelected)  graphics.setColor(t);
	} */
	private void drawSingle(TreeDrawerNode node) {
	    TreeDrawerNode left = node.getLeft();
	    TreeDrawerNode right = node.getRight();
		if (xT == null) 
		  System.err.println("xt was null");
		if (right == null)
		  System.err.println("right was null");

		int ry = (int) yT.transform(right.getCorr());
	    int ly = (int) yT.transform(left.getCorr());
	    int ty = (int) yT.transform(node.getCorr());
	    
	    int rx = (int) xT.transform(right.getIndex() + .5);
	    int lx = (int) xT.transform(left.getIndex() + .5);
	    //int tx = (int) xT.transform(node.getIndex() + .5);


	    //draw our (flipped) polyline...
	    if (isSelected) 
		graphics.setColor(sel_color);
	    else
		graphics.setColor(node.getColor());
	    graphics.drawPolyline(new int[] {rx, rx, lx, lx},
				  new int[] {ry, ty, ty, ly}, 4);

//	    graphics.setColor(t);
	}

	boolean isSelected = false;
	private Color sel_color = Color.red;
	private Graphics graphics;
	private TreeDrawerNode selected;
	private LinearTransformation xT, yT;

	private double minInd, maxInd;
	private Rectangle dest;
    }
}

