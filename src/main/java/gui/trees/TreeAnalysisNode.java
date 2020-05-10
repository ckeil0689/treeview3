/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */


package gui.trees;

/**
 * @author avsegal
 *
 * Generic class for analysis of ATR and GTR trees.
 */

import java.util.Vector;

public abstract class TreeAnalysisNode {

	/**
	 * Create a new node with given id.
	 *
	 * @param pID
	 *            the id of the node
	 */
	TreeAnalysisNode(final String pID) {
		id = pID;
		left = null;
		right = null;
		parent = null;
	}

	/**
	 * Create a new node with a given id and parent.
	 *
	 * @param pID
	 *            the id of the node
	 * @param pParent
	 *            the parent of the node
	 */
	TreeAnalysisNode(final String pID, final TreeAnalysisNode pParent) {
		id = pID;
		left = null;
		right = null;
		parent = pParent;
	}

	/**
	 * Gets the node's id.
	 *
	 * @return the id
	 */
	public String getID() {
		return id;
	}

	/**
	 * Gets the index of this node.
	 *
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Sets the index of this node.
	 *
	 * @param ind
	 *            index to set
	 */
	public void setIndex(final int ind) {
		index = ind;
	}

	/**
	 * Is this a root node?
	 *
	 * @return true if this node is a root, false otherwise
	 */
	public boolean isRoot() {
		return (parent == null);
	}

	/**
	 * Is this a leaf node?
	 *
	 * @return true if this node is a leaf, false otherwise
	 */
	public boolean isLeaf() {
		return (left == null && right == null);
	}

	/**
	 * Gets the leftmost leaf of the subtree rooted at this node.
	 *
	 * @return the leftmost leaf of subtree.
	 */
	public TreeAnalysisNode getLeftLeaf() {
		if (isLeaf())
			return this;

		if (left != null)
			return left.getLeftLeaf();

		return right.getLeftLeaf(); // right cannot be null, otherwise this is a
		// leaf
	}

	/**
	 * Gets the rightmost leaf of the subtree rooted at this node.
	 *
	 * @return the rightmost leaf of subtree.
	 */
	public TreeAnalysisNode getRightLeaf() {
		if (isLeaf())
			return this;

		if (right != null)
			return right.getRightLeaf();

		return left.getRightLeaf(); // left cannot be null, otherwise this is a
		// leaf
	}

	/**
	 * Switched the left and right child.
	 *
	 */
	public void flip() {
		TreeAnalysisNode temp;

		temp = left;
		left = right;
		right = temp;
	}

	/**
	 * Gives a listing, in order, of all nodes in this tree.
	 *
	 * @param v
	 *            vector to be filled with the listing
	 */
	public void enumerate(final Vector v) {
		if (left != null) {
			left.enumerate(v);
		}

		v.add(this);

		if (right != null) {
			right.enumerate(v);
		}
	}

	/**
	 * Computes the root of this node's tree.
	 *
	 * @return the root node
	 */
	public TreeAnalysisNode findRoot() {
		if (isRoot())
			return this;

		return parent.findRoot();
	}

	/**
	 * Find a node with given id in this subtree.
	 *
	 * @param id
	 *            id to look for
	 * @return node found, or null if no such node exists
	 */
	public TreeAnalysisNode find(final String id) {
		TreeAnalysisNode temp;

		if (getID().equals(id))
			return this;

		if (left != null) {
			temp = left.find(id);
			if (temp != null)
				return temp;
		}

		if (right != null) {
			temp = right.find(id);
			if (temp != null)
				return temp;
		}

		return null;
	}

	/**
	 * Set the left child.
	 *
	 * @param node
	 *            left child
	 */
	public void setLeft(final TreeAnalysisNode node) {
		left = node;
	}

	/**
	 * Gets the left child.
	 *
	 * @return left child
	 */
	public TreeAnalysisNode getLeft() {
		return left;
	}

	/**
	 * Set the right child.
	 *
	 * @param node
	 *            right child
	 */
	public void setRight(final TreeAnalysisNode node) {
		right = node;
	}

	/**
	 * Gets the right child.
	 *
	 * @return right child
	 */
	public TreeAnalysisNode getRight() {
		return right;
	}

	/**
	 * Sets the parent.
	 *
	 * @param node
	 *            parent
	 */
	public void setParent(final TreeAnalysisNode node) {
		parent = node;
	}

	/**
	 * Gets the parent.
	 *
	 * @return parent
	 */
	public TreeAnalysisNode getParent() {
		return parent;
	}

	TreeAnalysisNode left;
	TreeAnalysisNode right;
	TreeAnalysisNode parent;
	String id;
	int index;

}