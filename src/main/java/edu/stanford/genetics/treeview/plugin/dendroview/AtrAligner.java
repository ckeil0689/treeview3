/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */


package edu.stanford.genetics.treeview.plugin.dendroview;

/**
 * @author avsegal
 *
 * Aligns the array ordering to match a different array tree.
 * Used statically two align one fileset to another.
 */

import java.util.Hashtable;
import java.util.Vector;

import edu.stanford.genetics.treeview.HeaderInfo;

public class AtrAligner {

	/**
	 *
	 * @param atrHeader1
	 *            the atr header to be aligned
	 * @param arrayHeader1
	 *            the array header to be aligned
	 * @param atrHeader2
	 *            the atr header to align to
	 * @param arrayHeader2
	 *            the array header to align to
	 * @return a new ordering of arrayHeader1
	 * @throws DendroException
	 */
	public static int[] align(final HeaderInfo atrHeader1,
			final HeaderInfo arrayHeader1, final HeaderInfo atrHeader2,
			final HeaderInfo arrayHeader2) throws DendroException {

		final int numArrays = arrayHeader1.getNumHeaders();
		final int[] newOrder = new int[numArrays];
		AtrAnalysisNode root1;

		for (int i = 0; i < numArrays; i++) {
			newOrder[i] = i;
		}

		root1 = createAnalysisTree(atrHeader1, arrayHeader1);

		alignTree(root1, arrayHeader1, arrayHeader2, newOrder);

		return newOrder;
	}

	/**
	 * Creates an AtrAnalysis tree based on the atr and array headers.
	 *
	 * @param atrHeader
	 *            ATR header
	 * @param arrayHeader
	 *            array header
	 * @return the root node of the tree
	 * @throws DendroException
	 */
	private static AtrAnalysisNode createAnalysisTree(
			final HeaderInfo atrHeader, final HeaderInfo arrayHeader)
			throws DendroException {

		final int numArrays = arrayHeader.getNumHeaders();

		final AtrAnalysisNode[] leafNodes = new AtrAnalysisNode[numArrays];
		final Hashtable id2node = new Hashtable(
				((atrHeader.getNumHeaders() * 4) / 3) / 2, .75f);

		String newId, leftId, rightId;

		AtrAnalysisNode newN, leftN, rightN;

		for (int i = 0; i < atrHeader.getNumHeaders(); i++) {
			newId = atrHeader.getHeader(i, "NODEID");
			leftId = atrHeader.getHeader(i, "LEFT");
			rightId = atrHeader.getHeader(i, "RIGHT");

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
				val = arrayHeader.getHeaderIndex(leftId);

				if (val == -1)
					throw new DendroException("Identifier " + leftId
							+ " from tree file not found in CDT");

				leftN = new AtrAnalysisNode(leftId, newN);
				leftN.setIndex(val);
				leftN.setName(arrayHeader.getHeader(val, "GID"));

				leafNodes[val] = leftN;
				id2node.put(leftId, leftN);
			}

			if (rightN == null) {
				// this means that the identifier for rightn is a new leaf
				// System.out.println("Looking up " + rightId);
				int val; // stores index (y location)
				val = arrayHeader.getHeaderIndex(rightId);

				if (val == -1)
					throw new DendroException("Identifier " + rightId
							+ " from tree file not found in CDT.");

				rightN = new AtrAnalysisNode(rightId, newN);
				rightN.setIndex(val);
				rightN.setName(arrayHeader.getHeader(val, "GID"));

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
	 * @param arrayHeader1
	 *            array header of the tree to align
	 * @param arrayHeader2
	 *            array header of the tree to align to
	 * @param ordering
	 *            the ordering array which this method will fill
	 */
	private static void alignTree(final AtrAnalysisNode root1,
			final HeaderInfo arrayHeader1, final HeaderInfo arrayHeader2,
			final int[] ordering) {

		final Vector v1 = new Vector();
		final Hashtable gid2index = new Hashtable();
		final int gidIndex = arrayHeader2.getIndex("GID");

		for (int i = 0; i < arrayHeader2.getNumHeaders(); i++) {

			gid2index.put(arrayHeader2.getHeader(i)[gidIndex], new Integer(i));
		}

		root1.indexTree(arrayHeader2, gid2index);

		root1.enumerate(v1);

		for (int i = 0; i < v1.size(); i++) {

			ordering[i] = arrayHeader1.getHeaderIndex(((AtrAnalysisNode) v1
					.get(i)).getID());
		}
	}
}
