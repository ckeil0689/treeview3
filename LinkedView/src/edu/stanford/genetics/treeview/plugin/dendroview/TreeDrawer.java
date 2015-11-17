/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.Stack;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LinearTransformation;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeDrawerNode;

/**
 * Class for Drawing and Manipulating Trees
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version Alpha
 *
 *          Stores a representation of the tree in a normalized space.
 *          <p>
 *
 *          The class will draw a tree on a graphics object, given some source
 *          rectangle in the normalized space, and a target rectangle in the
 *          caller's space.
 *          <p>
 *
 *          The dimensions of the normalized space can be found by calling an
 *          accessor method. In general, it has a number of units in the major
 *          direction(the index direction) equal to the number of leaf nodes.
 *          For a sideways tree, this means that the height of the sideways tree
 *          is equal to the number of leafs. The minor dimension (or correlation
 *          direction) is determined by the range of the values stored in the
 *          nodes. If the nodes represent correlations, this is at most from -1
 *          to 1.
 *          <p>
 *
 *          The tree can also be queried to find the closest node given a
 *          (index,corr) pair.
 *          <p>
 *
 *          I may later extend this class to support rotations about an
 *          arbitrary node.
 */
abstract class TreeDrawer extends Observable implements Observer {

	/** type of header which can be used to set branch heights */
	public static final int CORRELATION = 0;
	/** type of header which can be used to set branch heights */
	public static final int TIME = 1;

	/**
	 * used to keep track of the HeaderInfo we're observing, so we can stop
	 * observing if someone calls setData to a new HeaderInfo.
	 */
	private HeaderInfo nodeInfo = null;

	private double corrMin;
	private double corrMax;
	private TreeDrawerNode[] leafList;
	private TreeDrawerNode rootNode;
	private Hashtable<String, TreeDrawerNode> id2node;

	/**
	 * Constructor does nothing but set defaults
	 */
	public TreeDrawer() {

		setDefaults();
	}

	private void setDefaults() {

		id2node = null;
		rootNode = null;
		leafList = null;
		if (nodeInfo != null) {
			nodeInfo.deleteObserver(this);
		}
		nodeInfo = null;
		setChanged();
	}

	@Override
	public void update(final Observable o, final Object arg) {

		if (o == nodeInfo) {
			setChanged();
			notifyObservers();

		} else {
			LogBuffer.println("TreeDrawer got update from unexpected "
					+ "observable " + o);
		}
	}

	/**
	 * Accessor for the root node
	 *
	 * @return root node
	 */
	public TreeDrawerNode getRootNode() {

		return rootNode;
	}

	/**
	 * this somewhat misnamed method returns the minimum branch value
	 */
	public double getCorrMin() {

		return corrMin;
	}

	public void setCorrMin(final double corrMin) {

		this.corrMin = corrMin;
	}

	/**
	 * this somewhat misnamed method returns the maximum branch value
	 */
	public double getCorrMax() {

		return corrMax;
	}

	public void setCorrMax(final double corrMax) {

		this.corrMax = corrMax;
	}

	public TreeDrawerNode getLeaf(final int i) {

		if (leafList != null && i > -1 && i < leafList.length) {
			try {
				return leafList[i];

			} catch (final Exception e) {
				LogBuffer.println("Got exception: " + e.getMessage());
				e.printStackTrace();
			}
		}
		return null;
	}

	public TreeDrawerNode getNearestNode(final int minSelectionIndex,
			final int maxSelectionIndex) {

		if (rootNode == null)
			return null;

		TreeDrawerNode minLeaf = getLeaf(minSelectionIndex);
		TreeDrawerNode maxLeaf = getLeaf(maxSelectionIndex);

		final TreeDrawerNode[] minParentArray = new TreeDrawerNode[leafList.length];
		final TreeDrawerNode[] maxParentArray = new TreeDrawerNode[leafList.length];

		boolean isRoot = false;
		int addIndex = 0;
		while (!isRoot) {

			final TreeDrawerNode parent = minLeaf.getParent();
			if (parent.getId().equalsIgnoreCase(rootNode.getId())) {
				isRoot = true;

			} else {
				minParentArray[addIndex] = parent;
				minLeaf = parent;
				addIndex++;
			}
		}

		isRoot = false;
		addIndex = 0;
		while (!isRoot) {

			final TreeDrawerNode parent = maxLeaf.getParent();
			if (parent.getId().equalsIgnoreCase(rootNode.getId())) {
				isRoot = true;

			} else {
				maxParentArray[addIndex] = parent;
				maxLeaf = parent;
				addIndex++;
			}
		}

		TreeDrawerNode nearestNode = null;
		for (final TreeDrawerNode element : minParentArray) {

			if (element == null) {
				break;
			}

			for (final TreeDrawerNode element2 : maxParentArray) {

				if (element2 == null) {
					break;
				}

				if (element.getId().equalsIgnoreCase(element2.getId())) {
					nearestNode = element;
					break;
				}
			}

			if (nearestNode != null) {
				break;
			}
		}

		if (nearestNode == null) {
			LogBuffer.println("Error finding nearest node!");
		}
		return nearestNode;
	}

	/**
	 * Set the data from which to draw the tree
	 *
	 * @param nodeInfo
	 *            The headers from the node file. There should be one header row
	 *            per node. There should be a column named "NODEID", "RIGHT",
	 *            "LEFT" and one of either "CORRELATION" or "TIME".
	 * @param rowInfo
	 *            This is the header info for the rows which the ends of the
	 *            tree are supposed to line up with
	 *
	 */
	public void setData(final HeaderInfo nodeInfo, final HeaderInfo rowInfo)
			throws DendroException {

		if (nodeInfo == null) {
			setDefaults();
			return;
		}

		if (this.nodeInfo != null) {
			this.nodeInfo.deleteObserver(this);
		}

		this.nodeInfo = nodeInfo;
		nodeInfo.addObserver(this);

		leafList = new TreeDrawerNode[rowInfo.getNumHeaders()];
		id2node = new Hashtable<String, TreeDrawerNode>(
				((nodeInfo.getNumHeaders() * 4) / 3) / 2, .75f);

		final int nodeIndex = nodeInfo.getIndex("NODEID");
		if (nodeIndex == -1)
			throw new DendroException("Could not find header NODEID "
					+ "in tree header info");

		for (int j = 0; j < nodeInfo.getNumHeaders(); j++) {

			// extract the things we need from the enumeration
			final String newId = nodeInfo.getHeader(j, nodeIndex);
			final String leftId = nodeInfo.getHeader(j, "LEFT");
			final String rightId = nodeInfo.getHeader(j, "RIGHT");

			// setup the kids
			final TreeDrawerNode newn = id2node.get(newId);
			TreeDrawerNode leftn = id2node.get(leftId);
			TreeDrawerNode rightn = id2node.get(rightId);

			if (newn != null) {
				LogBuffer.println("Symbol '" + newn
						+ "' appeared twice, building weird tree");
			}

			if (leftn == null) {
				// this means that the identifier for leftn is a new leaf
				int val; // stores index (y location)
				val = rowInfo.getHeaderIndex(leftId);

				if (val == -1)
					throw new DendroException("Identifier " + leftId
							+ " from tree file not found in CDT.");

				leftn = new TreeDrawerNode(leftId, 1.0, val);
				leafList[val] = leftn;
				id2node.put(leftId, leftn);
			}

			if (rightn == null) {
				// this means that the identifier for rightn is a new leaf
				// System.out.println("Looking up " + rightId);
				int val; // stores index (y location)
				val = rowInfo.getHeaderIndex(rightId);
				if (val == -1)
					throw new DendroException("Identifier " + rightId
							+ " from tree file not found in CDT!");

				rightn = new TreeDrawerNode(rightId, 1.0, val);
				leafList[val] = rightn;
				id2node.put(rightId, rightn);
			}

			if (leftn.getIndex() > rightn.getIndex()) {
				final TreeDrawerNode swap = leftn;
				leftn = rightn;
				rightn = swap;
			}

			rootNode = new TreeDrawerNode(newId, 0.0, leftn, rightn);
			leftn.setParent(rootNode);
			rightn.setParent(rootNode);
			// finally, insert in tree
			id2node.put(newId, rootNode);
		}

		setBranchHeights(nodeInfo, rowInfo);
		setChanged();
	}

	public void setBranchHeights(final HeaderInfo nodeInfo,
			final HeaderInfo rowInfo) {

		if (rootNode == null)
			return;

		int nameIndex = nodeInfo.getIndex("TIME");
		int type = TIME;
		if (nameIndex == -1) {
			nameIndex = nodeInfo.getIndex("CORRELATION");
			type = CORRELATION;
		}
		setBranchHeightsIter(nodeInfo, nameIndex, type, rootNode);

		// set branch heights for leaf nodes...
		if (type == CORRELATION) {
			setCorrMin(rootNode.getMinCorr());
			setCorrMax(1.0);
			for (final TreeDrawerNode element : leafList) {

				if (element != null) {
					element.setCorr(getCorrMax());
				}
			}
		} else {
			for (final TreeDrawerNode element : leafList) {

				double leaf = rootNode.getCorr();
				try {
					leaf = parseDouble(rowInfo.getHeader(
							(int) element.getIndex(), "LEAF"));

				} catch (final Exception e) {

				}
				element.setCorr(leaf);
			}
			setCorrMin(rootNode.getMinCorr());
			setCorrMax(rootNode.getMaxCorr());

			for (final TreeDrawerNode element : leafList) {

				// similar to the correlation case, makes the leaves extend
				// all the way to the end.
				double leaf = getCorrMax();
				try {
					leaf = parseDouble(rowInfo.getHeader(
							(int) element.getIndex(), "LEAF"));
				} catch (final Exception e) {
				}

				element.setCorr(leaf);

				// This would set the leaf's branch length to be the same
				// as it's parent...
				// leafList[i].setCorr(leafList[i].getParent().getCorr());

				// this makes the leaf end at the midpoint of the previous two.
				// leafList[i].setCorr((getCorrMax() +
				// leafList[i].getParent().getCorr()) / 2);
			}
		}
	}

	public void setBranchHeightsIter(final HeaderInfo nodeInfo,
			final int nameIndex, final int type, final TreeDrawerNode start) {

		final Stack<TreeDrawerNode> remaining = new Stack<TreeDrawerNode>();
		remaining.push(start);

		while (remaining.empty() == false) {

			final TreeDrawerNode current = remaining.pop();
			if (current.isLeaf()) {
				// will get handled in a linear-time routine...

			} else {
				final int j = nodeInfo.getHeaderIndex(current.getId());
				final Double d = new Double(nodeInfo.getHeader(j)[nameIndex]);
				final double corr = d.doubleValue();
				if (type == CORRELATION) {
					if ((corr < -1.0) || (corr > 1.0)) {
						System.out.println("Got illegal correlation " + corr
								+ " at line j");
					}
					current.setCorr(corr);

				} else {
					current.setCorr(corr);
				}

				remaining.push(current.getLeft());
				remaining.push(current.getRight());
			}
		}
	}

	/**
	 * Draw the tree on a given graphics object, given a particular source and
	 * destination. Can specify a node to drawn as selected, or not.
	 *
	 * @param graphics
	 *            The graphics object to draw on
	 * @param xScaleEq
	 *            Equation describing mapping from pixels to index
	 * @param yScaleEq
	 *            Equation describing mapping from pixels to index
	 * @param dest
	 *            Specifies Rectangle of pixels to draw to
	 * @param selected
	 *            A selected node
	 *
	 *            the actual implementation of this depends, of course, on the
	 *            orientation of the tree.
	 */
	abstract public void paint(Graphics graphics,
			LinearTransformation xScaleEq, LinearTransformation yScaleEq,
			Rectangle dest, TreeDrawerNode selected, boolean isLeft,
			int hoverIndex,final int minVisLabelIndex,
			final int maxVisLabelIndex);

	/**
	 * Get the closest node to the given (index, correlation) pair.
	 *
	 */
	public TreeDrawerNode getClosest(final double index, final double corr,
			final double weight) {

		if (rootNode == null)
			return null;

		final IterativeClosestFinder rcf = new IterativeClosestFinder(index,
				corr, weight);

		return rcf.find(rootNode);
	}

	public TreeDrawerNode getClosestParent(TreeDrawerNode leaf,
		final double corr) {

		if (rootNode == null || leaf == null) {
			return null;
		}

		TreeDrawerNode currentNode = leaf;

		//Going to search up the tree from the leaf
		while(currentNode != rootNode) {
			currentNode = currentNode.getParent();
			//If the relative correlation of the cursor position (corr) is
			//greater than the correlation at the point where the parent node
			//splits
			if(currentNode.getCorr() < corr) {
				return(currentNode);
			}
		}
		return(currentNode);
	}

	/**
	 * Get node by Id returns null if no matching id
	 */
	public TreeDrawerNode getNodeById(final String id) {

		if (id == null)
			return null;

		return id2node.get(id);
	}

	/**
	 * this is an internal helper class which does a sort of recursive search,
	 * but doesn't blow stack quite as much as a recursive function
	 *
	 * @author Alok Saldanha <alok@genome.stanford.edu>
	 * @version Alpha
	 */
	class IterativeClosestFinder {

		private final double index;
		private final double correlation;
		private final double weight;

		/**
		 * The constructor sets the variables
		 *
		 * @param ind
		 *            The index for which to search
		 * @param corr
		 *            The correlation for which to search
		 * @param wei
		 *            The relative weight to assign to correlation. The distance
		 *            function will be sqrt((delta(corr) * weight) ^2 +
		 *            (delta(index))^2)
		 */
		public IterativeClosestFinder(final double ind, final double corr,
				final double wei) {

			index = ind;
			correlation = corr;
			weight = wei;
		}

		/**
		 * the find method actually finds the node
		 */
		public TreeDrawerNode find(final TreeDrawerNode startNode) {

			if (startNode.isLeaf())
				return startNode;
			TreeDrawerNode closest = startNode;

			// some stack allocation...
			final Stack<TreeDrawerNode> remaining = new Stack<TreeDrawerNode>();
			remaining.push(startNode);

			while (!remaining.empty()) {

				final TreeDrawerNode testN = remaining.pop();

				if (testN.getDist(index, correlation, weight) < closest
						.getDist(index, correlation, weight)) {
					closest = testN;
				}

				// lots of stack allocation...
				final TreeDrawerNode left = testN.getLeft();
				final TreeDrawerNode right = testN.getRight();
				if (!left.isLeaf()) {
					remaining.push(left);
				}

				if (!right.isLeaf()) {
					remaining.push(right);
				}
			}

			return closest;
		}
	}

	public static double parseDouble(final String string) {
		final Double val = Double.valueOf(string);
		return val.doubleValue();
	}
}
