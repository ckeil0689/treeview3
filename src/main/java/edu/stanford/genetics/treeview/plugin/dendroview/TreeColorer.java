/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;

import edu.stanford.genetics.treeview.LabelInfo;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeDrawerNode;

/**
 * This class simply colors in trees. It's a pretty non-OO class.
 */
public class TreeColorer {
	private static int colorInd;
	private static String[][] headers; // used when inferring node colors from
	// gene colors
	private static LabelInfo labelInfo; // used when coloring using column

	// from GTR.

	/**
	 *
	 * @param rootNode
	 * @param rowLabelInfo
	 */
	public static void colorUsingHeader(final TreeDrawerNode rootNode,
			final LabelInfo rowLabelInfo) {
		final int index = rowLabelInfo.getIndex("FGCOLOR");
		if (index < 0)
			return;
		colorUsingPrefix(rootNode, rowLabelInfo, index);
	}

	/**
	 * colors using header stored in nodes of tree
	 *
	 * @param root
	 *            root node of tree
	 * @param h
	 *            headerInfo of tree
	 * @param ci
	 *            index into columns of tree's header info
	 */
	public static final synchronized void colorUsingPrefix(
			final TreeDrawerNode root, final LabelInfo h, final int ci) {
		colorInd = ci;
		labelInfo = h;
		if (labelInfo == null) {
			LogBuffer.println("TreeColorer: headers null");
			return;
		}
		if (colorInd < 0) {
			LogBuffer.println("TreeColorer: colorInd < 0");
			return;
		}
		if (root == null) {
			LogBuffer.println("TreeColorer: root null");
			return;
		}
		recursiveColorUsingLabel(root);
	}

	private static final void recursiveColorUsingLabel(
			final TreeDrawerNode node) {
		// wrong index
		// String [] headers = headerInfo.getHeader((int) node.getIndex());
		if (node.isLeaf())
			return;
		else {
			final int index = labelInfo.getLabelIndex(node.getId());
			if (index < 0) {
				LogBuffer.println("Problem finding node " + node.getId());
			}
			final String[] labels = labelInfo.getLabels(index);

			final String color = labels[colorInd];
			node.setColor(parseColor(color));

			recursiveColorUsingLabel(node.getLeft());
			recursiveColorUsingLabel(node.getRight());
		}
	}

	/**
	 * colors using leaf nodes
	 *
	 * @param root
	 * @param h
	 * @param ci
	 */
	public static final synchronized void colorUsingLeaf(
			final TreeDrawerNode root, final LabelInfo h, final int ci) {
		colorInd = ci;
		labelInfo = h;
		if (labelInfo == null) {
			LogBuffer.println("headers null");
			return;
		}
		if (colorInd < 0)
			// LogPanel.println("colorInd < 0");
			return;
		recursiveColorUsingLeaf(root);
	}

	public static final synchronized void colorize(final TreeDrawerNode root,
			final String[][] h, final int ci) {
		colorInd = ci;
		headers = h;
		if (headers == null)
			// System.out.println("headers null");
			return;
		if (colorInd < 0)
			// System.out.println("colorInd < 0");
			return;
		recursiveColor(root);
	}

	private static final void recursiveColorUsingLeaf(final TreeDrawerNode node) {
		if (node.isLeaf()) {
			// System.out.println("coloring leaf");
			node.setColor(parseColor(labelInfo.getLabel(
					(int) node.getIndex(), colorInd)));
		} else {
			recursiveColorUsingLeaf(node.getLeft());
			recursiveColorUsingLeaf(node.getRight());
			majorityColor(node);
			// node.setColor(synthesizeColor(node.getLeft(), node.getRight()));
		}
	}

	private static final void recursiveColor(final TreeDrawerNode node) {
		if (node.isLeaf()) {
			// System.out.println("coloring leaf");
			node.setColor(parseColor(headers[(int) node.getIndex()][colorInd]));
		} else {
			recursiveColor(node.getLeft());
			recursiveColor(node.getRight());
			majorityColor(node);
			// node.setColor(synthesizeColor(node.getLeft(), node.getRight()));
		}
	}

	private static String[] colornames = new String[100];
	private static Color[] colors = new Color[100];

	private static final void majorityColor(final TreeDrawerNode node) {
		final int[] count = new int[100];
		final int min = (int) node.getMinIndex();
		final int max = (int) node.getMaxIndex();
		for (int i = min; i < max; i++) {
			String color;
			if (headers == null) {
				color = labelInfo.getLabel(i, colorInd);
			} else {
				color = headers[i][colorInd];
			}
			final int index = getIndex(color);
			count[index]++;
		}
		// now, get max
		int maxI = 0;
		for (int i = 0; colornames[i] != null; i++) {
			// System.out.println("colornames[" + i +"] = "+ colornames[i]);
			if (count[i] > count[maxI]) {
				maxI = i;
			}
		}
		node.setColor(colors[maxI]);
	}

	public static Color getColor(final String color) {
		return colors[getIndex(color)];
	}

	private static int getIndex(final String color) {
		int i;
		for (i = 0; i < colornames.length; i++) {
			if (colornames[i] == null) {
				break;
			} else if (colornames[i].equals(color))
				return i;
		}
		// need to allocate new color
		colornames[i] = color;
		colors[i] = parseColor(colornames[i]);
		return i;
	}

	private static final Color parseColor(final String colorString) {
		try {
			return Color.decode(colorString); // will this work?
		} catch (final Exception e) {
			return Color.gray;
		}
	}

}
