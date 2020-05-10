/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */


package gui.alignment;

/**
 * @author avsegal
 *
 * Aligns the array ordering to match a different array tree.
 * Used statically two align one fileset to another.
 */

import gui.general.DendroException;
import gui.trees.AtrAnalysisNode;
import model.data.labels.LabelInfo;

import java.util.Hashtable;
import java.util.Vector;

public class AtrAligner {

	/**
	 *
	 * @param atrLabelInfo1
	 *            the atr labelInfo to be aligned
	 * @param colLabelInfo1
	 *            the array LabelInfo to be aligned
	 * @param atrLabelInfo2
	 *            the atr LabelInfo to align to
	 * @param colLabelInfo2
	 *            the array LabelInfo to align to
	 * @return a new ordering of colLabelInfo1
	 * @throws DendroException
	 */
	public static int[] align(final LabelInfo atrLabelInfo1,
			final LabelInfo colLabelInfo1, final LabelInfo atrLabelInfo2,
			final LabelInfo colLabelInfo2) throws DendroException {

		final int numColLabels = colLabelInfo1.getNumLabels();
		final int[] newOrder = new int[numColLabels];
		AtrAnalysisNode root1;

		for (int i = 0; i < numColLabels; i++) {
			newOrder[i] = i;
		}

		root1 = createAnalysisTree(atrLabelInfo1, colLabelInfo1);

		alignTree(root1, colLabelInfo1, colLabelInfo2, newOrder);

		return newOrder;
	}

	/**
	 * Creates an AtrAnalysis tree based on the atr and column labels.
	 *
	 * @param atrLabelInfo
	 *            ATR labelInfo
	 * @param colLabelInfo
	 *            array labelInfo
	 * @return the root node of the tree
	 * @throws DendroException
	 */
	private static AtrAnalysisNode createAnalysisTree(
			final LabelInfo atrLabelInfo, final LabelInfo colLabelInfo)
			throws DendroException {

		final int numColLabels = colLabelInfo.getNumLabels();

		final AtrAnalysisNode[] leafNodes = new AtrAnalysisNode[numColLabels];
		final Hashtable id2node = new Hashtable(
				((atrLabelInfo.getNumLabels() * 4) / 3) / 2, .75f);

		String newId, leftId, rightId;

		AtrAnalysisNode newN, leftN, rightN;

		for (int i = 0; i < atrLabelInfo.getNumLabels(); i++) {
			newId = atrLabelInfo.getLabel(i, "NODEID");
			leftId = atrLabelInfo.getLabel(i, "LEFT");
			rightId = atrLabelInfo.getLabel(i, "RIGHT");

			newN = (AtrAnalysisNode) id2node.get(newId);
			leftN = (AtrAnalysisNode) id2node.get(leftId);
			rightN = (AtrAnalysisNode) id2node.get(rightId);

			if (newN != null) {
				System.out.println("Symbol '" + newId
						+ "' appeared twice, building weird tree");
			} else {
				newN = new AtrAnalysisNode(newId, null);
				id2node.put(newId, newN);
			}

			if (leftN == null) {
				// this means that the identifier for leftn is a new leaf
				int val; // stores index (y location)
				val = colLabelInfo.getLabelIndex(leftId);

				if (val == -1)
					throw new DendroException("Identifier " + leftId
							+ " from tree file not found in CDT");

				leftN = new AtrAnalysisNode(leftId, newN);
				leftN.setIndex(val);
				leftN.setName(colLabelInfo.getLabel(val, "GID"));

				leafNodes[val] = leftN;
				id2node.put(leftId, leftN);
			}

			if (rightN == null) {
				// this means that the identifier for rightn is a new leaf
				// System.out.println("Looking up " + rightId);
				int val; // stores index (y location)
				val = colLabelInfo.getLabelIndex(rightId);

				if (val == -1)
					throw new DendroException("Identifier " + rightId
							+ " from tree file not found in CDT.");

				rightN = new AtrAnalysisNode(rightId, newN);
				rightN.setIndex(val);
				rightN.setName(colLabelInfo.getLabel(val, "GID"));

				leafNodes[val] = rightN;
				id2node.put(rightId, rightN);
			}

			if (leftN.getIndex() > rightN.getIndex()) {
				final AtrAnalysisNode temp = leftN;
				leftN = rightN;
				rightN = temp;
			}

			rightN.setParent(newN);
			leftN.setParent(newN);

			newN.setLeft(leftN);
			newN.setRight(rightN);
		}

		return (AtrAnalysisNode) leafNodes[0].findRoot();
	}

	/**
	 * Aligns tree rooted at root1 to a different atr tree as best as possible.
	 *
	 * @param root1
	 *            root of the tree to align
	 * @param colLabelInfo1
	 *            column labelInfo of the tree to align
	 * @param colLabelInfo2
	 *            array header of the tree to align to
	 * @param ordering
	 *            the ordering array which this method will fill
	 */
	private static void alignTree(final AtrAnalysisNode root1,
			final LabelInfo colLabelInfo1, final LabelInfo colLabelInfo2,
			final int[] ordering) {

		final Vector v1 = new Vector();
		final Hashtable gid2index = new Hashtable();
		final int gidIndex = colLabelInfo2.getIndex("GID");

		for (int i = 0; i < colLabelInfo2.getNumLabels(); i++) {

			gid2index.put(colLabelInfo2.getLabels(i)[gidIndex], new Integer(i));
		}

		root1.indexTree(colLabelInfo2, gid2index);

		root1.enumerate(v1);

		for (int i = 0; i < v1.size(); i++) {

			ordering[i] = colLabelInfo1.getLabelIndex(((AtrAnalysisNode) v1
					.get(i)).getID());
		}
	}
}
