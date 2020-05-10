/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package model.data.trees;

/**
 * Represents nodes in ATRView and GTRView.
 *
 * HACK Should really retrofit this so that it's a subclass of
 * DefaultMutableTreeNode.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version Alpha
 */

import java.awt.*;
import java.util.Stack;

public class TreeDrawerNode {

	// instance variables
	private double corr = 0.0;
	private double ind = -1;
	private final double minInd, maxInd; // store max and min ind from this one.
	private TreeDrawerNode parent = null;
	private TreeDrawerNode left = null;
	private TreeDrawerNode right = null;
	private String id = null;
	private Color color = Color.black;

	/**
	 * Constructor for leaves.
	 *
	 * @param i
	 * @param correlation
	 * @param index
	 */
	public TreeDrawerNode(final String i, final double correlation,
			final double index) {

		id = i;
		corr = correlation;
		ind = index;
		minInd = index;
		maxInd = index;
	}

	/**
	 * Constructor for internal nodes.
	 *
	 * @param i
	 * @param correlation
	 * @param l
	 * @param r
	 */
	public TreeDrawerNode(final String i, final double correlation,
			final TreeDrawerNode l, final TreeDrawerNode r) {

		id = i;
		corr = correlation;
		right = r;
		left = l;

		ind = (right.getIndex() + left.getIndex()) / 2;
		minInd = Math.min(right.getMinIndex(), left.getMinIndex());
		maxInd = Math.max(right.getMaxIndex(), left.getMaxIndex());

		if (minInd > maxInd)
			throw new RuntimeException(
					"min was less than max! this should not happen.");
	}

	/**
	 * returns maximum correlation value (really branch height) for this subtree
	 */
	public double getMaxCorr() {

		double curCorr = getCorr();
		if (isLeaf())
			return curCorr;

		final Stack<TreeDrawerNode> remaining = new Stack<TreeDrawerNode>();
		remaining.push(this);

		while (remaining.empty() == false) {
			final TreeDrawerNode node = remaining.pop();
			if (node.getCorr() > curCorr) {
				curCorr = node.getCorr();
			}

			final TreeDrawerNode leftNode = node.getLeft();
			if (leftNode != null) {
				remaining.push(leftNode);
			}

			final TreeDrawerNode rightNode = node.getRight();
			if (rightNode != null) {
				remaining.push(rightNode);
			}
		}

		return curCorr;
	}

	/**
	 * returns minimum correlation value (really branch height) for this subtree
	 */
	public double getMinCorr() {

		double curCorr = getCorr();

		if (isLeaf())
			return curCorr;

		final Stack<TreeDrawerNode> remaining = new Stack<TreeDrawerNode>();
		remaining.push(this);

		while (remaining.empty() == false) {
			final TreeDrawerNode node = remaining.pop();
			if (node.getCorr() < curCorr) {
				curCorr = node.getCorr();
			}

			final TreeDrawerNode leftNode = node.getLeft();
			if (leftNode != null) {
				remaining.push(leftNode);
			}

			final TreeDrawerNode rightNode = node.getRight();
			if (rightNode != null) {
				remaining.push(rightNode);
			}
		}
		return curCorr;
	}

	/**
	 * This method interatively finds the node with the given id.
	 *
	 * @param nodeid
	 *            ID of the node to be found.
	 * @return the node found, or null if no such node exists
	 */
	public TreeDrawerNode findNode(final String nodeid) {

		final Stack<TreeDrawerNode> remaining = new Stack<TreeDrawerNode>();
		remaining.push(this);

		while (remaining.empty() == false) {
			final TreeDrawerNode node = remaining.pop();

			if (node.getId().equals(nodeid))
				return node;

			final TreeDrawerNode leftNode = node.getLeft();
			if (leftNode != null) {
				remaining.push(leftNode);
			}

			final TreeDrawerNode rightNode = node.getRight();
			if (rightNode != null) {
				remaining.push(rightNode);
			}
		}
		return null;
	}

	public double getDist(final double index, final double correlation,
			final double weight) {

		final double dx = ind - index;
		double dy = corr - correlation;

		dy *= weight;

		return dx * dx + dy * dy;
	}

	// Accessors
	/**
	 * @return index of node, i.e. where to draw node across width of tree
	 */
	public double getIndex() {

		return ind;
	}

	/**
	 * @return correlation of node, i.e. where to draw node across height of
	 *         tree
	 */
	public double getCorr() {

		return corr;
	}

	public String getId() {

		return id;
	}

	public TreeDrawerNode getParent() {

		return parent;
	}

	public TreeDrawerNode getLeft() {

		return left;
	}

	public TreeDrawerNode getRight() {

		return right;
	}

	public double getMaxIndex() {

		return maxInd;
	}

	public double getMinIndex() {

		return minInd;
	}

	public boolean isLeaf() {

		return ((left == null) && (right == null));
	}

	public TreeDrawerNode getLeftLeaf() {

		TreeDrawerNode cand = this;
		while (!cand.isLeaf()) {
			cand = cand.getLeft();
		}

		return cand;
	}

	public TreeDrawerNode getRightLeaf() {

		TreeDrawerNode cand = this;
		while (!cand.isLeaf()) {
			cand = cand.getRight();
		}

		return cand;
	}

	public double getRange() {

		return maxInd - minInd;
	}

	public Color getColor() {

		return color;
	}

	// Setters
	public void setParent(final TreeDrawerNode n) {

		parent = n;
	}

	public void setCorr(final double newCorr) {

		corr = newCorr;
	}

	public void setColor(final Color c) {

		color = c;
	}

	// Printing
	public void printSubtree() {

		printRecursive("");
	}

	private void printRecursive(final String pre) {

		if (getLeft() != null) {
			System.out.println(pre + getId() + ", corr " + getCorr()
					+ ", index " + getIndex());
			System.out.println(pre + "Left:");
			getLeft().printRecursive(pre + " ");
			System.out.println(pre + "Right:");
			getRight().printRecursive(pre + " ");

		} else {
			System.out.println(pre + getId() + " LEAF, corr " + getCorr()
					+ ", index " + getIndex());
		}
	}
}
