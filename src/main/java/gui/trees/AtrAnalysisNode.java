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
 * Binary tree node for analysis of array trees. The tree is parent-linked.
 */

import model.data.labels.LabelInfo;

import java.util.Hashtable;
import java.util.Vector;

public class AtrAnalysisNode extends TreeAnalysisNode {

	/**
	 * Creates a new node.
	 *
	 * @param pID
	 *            ID of the node in the ATR file
	 */
	public AtrAnalysisNode(final String pID) {
		super(pID);
		leafCount = -1;
		averageIndex = -1;
		name = "";
	}

	/**
	 * Creates a new node with a given parent.
	 *
	 * @param pID
	 *            ID of the node in the ATR file
	 * @param pParent
	 *            parent of this node
	 */
	public AtrAnalysisNode(final String pID, final TreeAnalysisNode pParent) {
		super(pID, pParent);
		leafCount = -1;
		averageIndex = -1;
		name = "";
	}

	/**
	 * Sets the name of this node.
	 *
	 * @param name
	 *            the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Returns the node's name.
	 *
	 * @return the node's name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the average of all leaf weights in this subtree. This is used in
	 * the alignment algorithm.
	 *
	 * @return the average index of leaves
	 */
	public double getAverageSubtreeIndex() {
		double sum = 0;
		double num = 0;

		final Vector v = new Vector();

		enumerate(v);

		for (int i = 0; i < v.size(); i++) {
			if (((TreeAnalysisNode) v.elementAt(i)).isLeaf()) {
				sum += ((TreeAnalysisNode) v.elementAt(i)).getIndex();
				num++;
			}
		}

		return sum / num;
	}

	/**
	 * Returns a vector of all leaves in this subtree, in order.
	 *
	 * @param v
	 *            the vector to fill with leaves
	 */

	@Override
	public void enumerate(final Vector v) {
		if (left != null) {
			left.enumerate(v);
		}

		if (isLeaf()) {
			v.add(this);
		}

		if (right != null) {
			right.enumerate(v);
		}
	}

	/**
	 * Gets the number of leaves.
	 *
	 * @return the number of leaves in this subtree
	 */
	public int getLeafCount() {
		if (leafCount == -1) {
			if (isLeaf()) {
				leafCount = 1;
			} else {
				leafCount = 0;
				if (left != null) {
					leafCount += ((AtrAnalysisNode) left).getLeafCount();
				} else if (right != null) {
					leafCount += ((AtrAnalysisNode) right).getLeafCount();
				}
			}

		}

		return leafCount;
	}

	/**
	 * Calculates the average index of all nodes.
	 *
	 * @param colLabelInfo
	 *            the arrayHeader to use for index look up
	 * @param gid2index
	 *            hashtable for reverse index look up (by array name)
	 * @return the average index for this subtree
	 */
	private double computeAverageIndexTree(final LabelInfo colLabelInfo,
			final Hashtable gid2index) {

		double leftSum = 0, rightSum = 0;
		if (isLeaf()) {
			int val = 0;
			try {
				val = ((Integer) gid2index.get(getName())).intValue();
			} catch (final java.lang.NullPointerException ex) {
				leafCount = 0;

				// do nothing, since we want to ignore non-matched aspects of
				// mostly equivelent trees.
			}
			setIndex(val);
			averageIndex = val;
		} else {
			leftSum = ((AtrAnalysisNode) left).computeAverageIndexTree(
					colLabelInfo, gid2index);
			rightSum = ((AtrAnalysisNode) right).computeAverageIndexTree(
					colLabelInfo, gid2index);

			leftSum *= ((AtrAnalysisNode) left).getLeafCount();
			rightSum *= ((AtrAnalysisNode) right).getLeafCount();

			averageIndex = (leftSum + rightSum)
					/ (((AtrAnalysisNode) left).getLeafCount() + ((AtrAnalysisNode) right)
							.getLeafCount());

		}

		return averageIndex;
	}

	/**
	 * Rearranged the tree by average index.
	 *
	 */
	private void arrangeByAverageIndex() {
		if (left == null || right == null)
			return;

		AtrAnalysisNode temp;

		if (((AtrAnalysisNode) left).getAverageIndex() > ((AtrAnalysisNode) right)
				.getAverageIndex()) {
			temp = (AtrAnalysisNode) left;
			left = right;
			right = temp;
		}

		((AtrAnalysisNode) left).arrangeByAverageIndex();
		((AtrAnalysisNode) right).arrangeByAverageIndex();
	}

	/**
	 * Calculates all the indecies.
	 *
	 * @param colLabelInfo
	 *            the arrayHeader to use for index look up
	 * @param gid2index
	 *            hashtable for reverse index look up (by array name)
	 */
	public void indexTree(final LabelInfo colLabelInfo,
			final Hashtable gid2index) {
		computeAverageIndexTree(colLabelInfo, gid2index);
		arrangeByAverageIndex();
	}

	/**
	 * Gets the average index of this subtree.
	 *
	 * @return the average index of this subtree.
	 */
	public double getAverageIndex() {
		return averageIndex;
	}

	int leafCount;
	double averageIndex;
	String name;
}