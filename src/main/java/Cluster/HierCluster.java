package Cluster;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import Controllers.ClusterDialogController;
import Utilities.Helper;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * Class that performs hierarchical clustering on a supplied distance matrix. It
 * implements multiple linkage methods and consists of one main method
 * (cluster()) that is used to create one new cluster at a time.
 */
public class HierCluster {

	/*
	 * IMPORTANT NOTE: The variable prefixes row- and col- refer to the current
	 * distance matrix. This means that if columns of the original matrix are
	 * clustered, they will be rows here. This is because the data is formatted
	 * such that the axis to be clustered will be represented by an array of
	 * arrays and column access doesn't always need to jump between arrays.
	 */
	private Linker linker;
	private final String axisLabelType;
	private final int initial_matrix_size;
	private int iterNum;

	/* Half of the complete distance matrix (symmetry!) */
	private final DistanceMatrix distMatrix;

	private double min;
	int min_row_index = 0;
	int min_col_index = 0;

	/*
	 * Reordered list of distance matrix rows. This directly represents the
	 * reordered axis that was selected to be clustered.
	 */
	private int[] reorderedRowIdxs;
	private List<Node> nodeList;
	private List<String[]> treeNodeData;

	/*
	 * List to keep track of all clusters during each iteration of the
	 * clustering loop. Each sublist in this list is a cluster. As the loop
	 * proceeds, eventually only one sublist/ cluster will remain, because this
	 * program does agglomerative clustering.
	 */
	private List<List<Integer>> currentClusters;
	private int[][] rowIndexTable;

	private String[] links; // needed for connectNodes??? better way?

	/**
	 * The class that is responsible for the hierarchical clustering of the
	 * supplied distance matrix. It uses input parameters such as the chosen
	 * linkage method and the axis of the original dataset to be clustered to
	 * perform the clustering.
	 *
	 * @param linkMethod
	 *            The cluster linkage method chosen by the user.
	 * @param distMatrix
	 *            The distance matrix calculated in the previous step.
	 * @param axis
	 *            The axis of the original dataset which will be clustered.
	 * @param worker
	 *            The worker thread which performs the clustering. Needs to be
	 *            interrupted in this class if the user cancels the operation.
	 */
	public HierCluster(final int linkMethod, final DistanceMatrix distMatrix,
			final int axis) {

		this.linker = new Linker(linkMethod);
		this.distMatrix = distMatrix;
		this.initial_matrix_size = distMatrix.getSize();
		this.axisLabelType = (axis == ClusterDialogController.ROW_IDX) ? 
						ClusterFileGenerator.ROW_AXIS_BASEID : 
							ClusterFileGenerator.COL_AXIS_BASEID;
		
		treeNodeData = new ArrayList<String[]>(initial_matrix_size);
		// Add header row right away
		treeNodeData.add(new String[] {	"NODEID", "LEFT", "RIGHT", "CORRELATION"});

		prepareCluster();
	}

	/**
	 * Goes through a bunch of steps to prepare the object and data for
	 * clustering. Initializes important variables, sets up lists to be used and
	 * makes a deep copy of the distance matrix so there can be a reference to a
	 * non-mutated distance matrix.
	 */
	public void prepareCluster() {

		/*
		 * Deep copy of distance matrix to avoid mutation. Needs to access
		 * original values during generation of the new row when joining two
		 * rows.
		 */
		linker.cloneDistmatrix(distMatrix);

		/* Data to be written to file */
		links = new String[initial_matrix_size];

		/*
		 * Integer representation of rows for references in calculations (list
		 * of fusedGroups). Keeps track of which cluster was formed at which
		 * step.
		 */
		rowIndexTable = new int[initial_matrix_size][];

		/* Initialize min with smallest double value */
		min = Double.MIN_VALUE;

		/*
		 * Groups of row indices to keep track of the formed clusters at all
		 * steps.
		 */
		currentClusters = new ArrayList<List<Integer>>(initial_matrix_size);

		nodeList = new ArrayList<Node>(initial_matrix_size);

		/*
		 * Fill list with integers corresponding to the row indices. Initially,
		 * every matrix row is its own little cluster.
		 */
		for (int i = 0; i < initial_matrix_size; i++) {
			final List<Integer> initialCluster = new ArrayList<Integer>(1);
			initialCluster.add(i);
			currentClusters.add(initialCluster);
		}

		/* Ensure all needed variables are set up and initialized */
		if (links == null || rowIndexTable == null | currentClusters == null) {
			final String message = "Looks like clustering was not properly "
					+ "set up by the software. Can't proceed with "
					+ "clustering.";
			JOptionPane.showMessageDialog(Frame.getFrames()[0], message,
					"Error", JOptionPane.ERROR_MESSAGE);
			LogBuffer.println("Cluster preparation failed.");
			return;
		}
	}

	/**
	 * Hierarchically clusters the distance matrix and stores results in an
	 * array of reordered distance matrix row indices.
	 */
	public int cluster() {

		/* Set current iteration number */
		iterNum = initial_matrix_size - distMatrix.getSize();

		/* STEP 1: Find and set current(!) matrix minimum. */
		setMatrixMinimum();

		/* STEP 2: Link the two old clusters with the minimum distance */
		final int[] newCluster = linkClosestClusters();

		/* STEP 3: Write info about the new connection to file. */

		/* First determine if new cluster is linked to a previous node. */
		final String[] link = connectNodes(newCluster, min_row_index,
				min_col_index);
		/*
		 * Then write data to buffer and add the new node to links, so the next
		 * clusters can be checked in connectNodes() during future iterations.
		 */
		String[] nodeData = getNodeData(link, iterNum, min);
		links[iterNum] = nodeData[0];
		treeNodeData.add(nodeData);

		/* STEP 4: Update the lists that keep track of clusters. */

		/* Register clustering of the two rows */
		rowIndexTable[iterNum] = newCluster;

		final boolean rowClusHasMin = updateCurrentClusters(newCluster);

		/* STEP 5: Generate new row/ col based on the chosen linkage method. */
		double[] newMatrixElement = linker.link(newCluster, currentClusters);

		/* STEP 6: Update the distance matrix to reflect the new cluster. */
		updateDistMatrix(rowClusHasMin, newMatrixElement);

		return distMatrix.getSize();
	}

	/* -------------------Cluster Methods ----------------------------- */
	/**
	 * Finds the minimum of the current distance matrix and notes its
	 * coordinates in the matrix. Should be larger than the last minimum that
	 * was found in the previous step! The matrix shrinks as rows are clustered
	 * together (hence the need for a deep copy!).
	 */
	private void setMatrixMinimum() {

		this.min = distMatrix.findCurrentMin(min);
		this.min_row_index = distMatrix.getMinRowIndex();
		this.min_col_index = distMatrix.getMinColIndex();
	}

	/**
	 * Link the two clusters, that are closest in the cluster index list, which
	 * represents the clusters as row indexes of the distance matrix. Use the
	 * index values of the minimum value in the current step's distance matrix.
	 * The pair of closest clusters is composed of the cluster at rowMinIndex
	 * and the cluster at colMinIndex in currentClusters.
	 *
	 * @return The new cluster.
	 */
	private int[] linkClosestClusters() {

		/* Get the two clusters to be fused */
		/* Get the cluster at rowMinIndex */
		final int[] row_cluster = new int[currentClusters.get(min_row_index)
		                                  .size()];
		for (int i = 0; i < row_cluster.length; i++) {
			row_cluster[i] = currentClusters.get(min_row_index).get(i);
		}

		/* Get the cluster at colMinIndex */
		final int[] col_cluster = new int[currentClusters.get(min_col_index)
		                                  .size()];
		for (int i = 0; i < col_cluster.length; i++) {
			col_cluster[i] = currentClusters.get(min_col_index).get(i);
		}

		return Helper.concatIntArrays(row_cluster, col_cluster);
	}

	// TODO method never used - remove?
	private void addNodeToList(String[] link) {

		/* Add new node object to list */
		int id = iterNum + 1;
		double dist_val = 1 - min;
		Node newNode = new Node(id, dist_val);

		/* Link to children */
		int maxChildNum = 2;
		List<Node> nodes = new ArrayList<Node>();

		for (int i = 0; i < maxChildNum; i++) {

			if (link[i].substring(0, "NODE".length()).equalsIgnoreCase("NODE")) {
				String index_s = link[i].replaceAll("[\\D]", "");
				int index_i = Integer.parseInt(index_s);
				nodes.add(nodeList.get(index_i - 1));
			}
		}

		nodes = orderNodes(nodes);

		/* Left child is the one with larger dist value */
		if (nodes.size() > 0 && nodes.get(0) != null) {
			newNode.setLeftChild(nodes.get(0));
		}

		if (nodes.size() > 1 && nodes.get(1) != null) {
			newNode.setRightChild(nodes.get(1));
		}

		nodeList.add(newNode);
	}

	/**
	 * Order 2 nodes decreasing by their dist_value. TODO replace with actual
	 * comparator later.
	 * 
	 * @param nodes
	 * @return Ordered list of nodes.
	 */
	private static List<Node> orderNodes(List<Node> nodes) {

		if (nodes.size() < 2)
			return nodes;

		double val_1 = -1;
		double val_2 = -1;

		if (nodes.get(0) != null) {
			val_1 = nodes.get(0).getDistValue();
		}

		if (nodes.get(1) != null) {
			val_2 = nodes.get(1).getDistValue();
		}

		if (val_2 > val_1) {
			Collections.swap(nodes, 0, 1);
		}

		return nodes;
	}

	// TODO method never used - remove?
	private Node extractOrderedNodes(Node root, List<Node> nodeList) {

		if (root == null) {
			return null; 
		}

		if (root.getLeftChild() == null && root.getRightChild() == null) {
			nodeList.add(root);
			return null;
		}

		extractOrderedNodes(root.getLeftChild(), nodeList);
		extractOrderedNodes(root.getRightChild(), nodeList);

		return root;
	}

	/**
	 * Updates the list of all current clusters by removing the old clusters and
	 * adding the newly formed cluster at the appropriate index.
	 *
	 * @param newCluster
	 *            The new cluster to be added.
	 * @return Whether the row cluster contains the minimum.
	 */
	private boolean updateCurrentClusters(final int[] newCluster) {

		/*
		 * Adding the newly formed cluster to the list of current cluster at the
		 * position of the old cluster that contains the minimum row index.
		 */
		final int newClusterMin = findClusterMin(newCluster);

		/* Either of the two rows to be clustered has the minimum */
		final boolean rowClusHasMin = clusterHasMin(newClusterMin,
				min_row_index);

		/*
		 * Remove the two old clusters from the currentClusters list. Rows and
		 * columns with bigger list position must be first to avoid list
		 * shifting issues when currentClusters is updated.
		 */
		if (min_row_index > min_col_index) {
			currentClusters.remove(min_row_index);
			currentClusters.remove(min_col_index);

		} else {
			currentClusters.remove(min_col_index);
			currentClusters.remove(min_row_index);
		}

		/*
		 * TODO Better method? Transform newCluster array into a list so it can
		 * be added to currentClusters
		 */
		final List<Integer> newVals = new ArrayList<Integer>();
		for (final int element : newCluster) {
			newVals.add(element);
		}

		final int newClusterListSize = currentClusters.size() + 1;

		/* The node is in the row cluster */
		if (rowClusHasMin && min_row_index < newClusterListSize) {
			currentClusters.add(min_row_index, newVals);

			/* The node is in the col cluster */
		} else if (!rowClusHasMin && min_col_index < newClusterListSize) {
			currentClusters.add(min_col_index, newVals);

		} else {
			currentClusters.add(newVals);
		}

		return rowClusHasMin;
	}

	/**
	 * Updates the distance matrix by removing old rows and columns and
	 * inserting the newly formed row and column at the appropriate indices.
	 *
	 * @param rowClusHasMin
	 * @param newRow
	 */
	private void updateDistMatrix(final boolean rowClusHasMin,
			final double[] newRow) {

		/*
		 * First: check whether the row or column contains the smallest gene by
		 * index of both (fusedGroup) then add a newClade value to each element
		 * where newClade intersects (basically adding the column)
		 */
		if (rowClusHasMin) {
			/* replace element at row with newRow */
			distMatrix.replaceIndex(newRow, min_row_index);

			/*
			 * Remove the other row from distMatrix and keep the previously
			 * replaced row.
			 */
			distMatrix.deleteIndex(min_col_index);

			/* ? */
			for (int j = min_row_index; j < distMatrix.getSize(); j++) {

				final double[] element = distMatrix.getRow(j);
				/*
				 * Add to element at index 'row' if the element is bigger than
				 * the row value otherwise the element is too small to add a
				 * column value
				 */
				if (element.length > min_row_index) {
					element[min_row_index] = newRow[j];
				}
			}
		} else {
			/* replace element at row with newRow */
			distMatrix.replaceIndex(newRow, min_col_index);

			/* remove the other row from distMatrix */
			distMatrix.deleteIndex(min_row_index);

			/* ? */
			for (int j = min_col_index; j < distMatrix.getSize(); j++) {

				final double[] element = distMatrix.getRow(j);

				if (element.length > min_col_index) {
					element[min_col_index] = newRow[j];
				}
			}
		}
	}

	/**
	 * Finishes up clustering. Orders the list of reordered distance matrix rows 
	 * to be generated. Sets the variables that store the most data to null, 
	 * to ensure their garbage collection.
	 */
	public void finish() {

		linker.close();
		reorderRows(currentClusters.get(0));

		/* Ensure garbage collection for large objects */
		links = null;
		currentClusters = null;
	}

	/**
	 * Find minimum of an integer array that represents a cluster. O(n)
	 *
	 * @return Minimum of the supplied int array.
	 */
	public int findClusterMin(final int[] cluster) {

		int min = Integer.MAX_VALUE;

		for (final int row : cluster) {

			if (row < min) {
				min = row;
			}
		}

		return min;
	}

	/**
	 * Checks a cluster if it contains a certain value.
	 *
	 * @param min
	 * @return
	 */
	public boolean clusterHasMin(final int min, final int index) {

		final List<Integer> cluster = currentClusters.get(index);
		for (final int row : cluster) {

			if (min == row)
				return true;
		}

		return false;
	}

	/**
	 * Determines the String name of the cluster pair to be linked and checks
	 * whether either of the two rows is already part of a previously formed
	 * cluster. In this case the remaining row will be connected to that NODE.
	 *
	 * Complexity: O(n^2) (for loop + shareCommonElements())
	 *
	 * @param newCluster
	 * @param rowGroup
	 * @param colGroup
	 * @param row
	 * @param column
	 * @return The pair of matrix rows that have been clustered.
	 */
	public String[] connectNodes(final int[] newCluster, final int row,
			final int column) {

		/* Make Strings for String list to be written to data file */
		String geneRow = "";
		String geneCol = "";

		final int groupSize = 2;

		final String[] dataList = new String[groupSize];

		/*
		 * Check the record of clusters (rowIndexTable) whether a part of the
		 * new cluster is already part of a previous cluster. If yes connect to
		 * LAST node by replacing the gene name with the node name for the tree
		 * file.
		 */
		if (newCluster.length == 2) {
			geneRow = axisLabelType + currentClusters.get(row).get(0) + "X";
			geneCol = axisLabelType + currentClusters.get(column).get(0) + "X";
		}
		/* If size of new cluster exceeds 2 */
		else {
			geneRow = findLastClusterMatch(row);
			geneCol = findLastClusterMatch(column);
		}

		/*
		 * Check if only one of the two String has a "NODE" component. If yes,
		 * position it at [1].
		 */
		if (!geneCol.contains("NODE") && geneRow.contains("NODE")) {
			dataList[0] = geneCol;
			dataList[1] = geneRow;

		} else {
			dataList[0] = geneRow;
			dataList[1] = geneCol;
		}

		return dataList;
	}

	/**
	 * Move from top down to find the last cluster containing any row from
	 * current colGroup so that the correct NODE-connection can be found.
	 */
	private String findLastClusterMatch(final int index) {

		String name = "";

		final int[] group = new int[currentClusters.get(index).size()];
		for (int i = 0; i < group.length; i++) {

			group[i] = currentClusters.get(index).get(i);
		}

		for (int j = iterNum - 1; j >= 0; j--) {

			/*
			 * This currently gets the last node that has a common element if
			 * the 2 groups have elements in common...
			 */
			if (rowIndexTable[j] != null) {
				if (Helper.shareCommonElements(rowIndexTable[j], group)) {

					/*
					 * Assigns NODE # of last fusedGroup containing a colGroup
					 * element.
					 */
					name = links[j];
					break;
				}
				/*
				 * If the current fusedGroup in geneIntegerTable does not have
				 * any elements in common with geneGroups.get(column).
				 */
				name = axisLabelType + currentClusters.get(index).get(0) + "X";
			}
		}

		return name;
	}
	
	/**
	 * Writes information about the newly clustered elements to a buffer.
	 * 
	 * @param link
	 *            The pair of newly linked elements.
	 * @param loopNum
	 *            The current iteration step of clustering.
	 * @param min
	 *            The minimum value from the distance matrix associated with the
	 *            current cluster pair.
	 * @return The ID of the newly formed tree node.
	 */
	public String[] getNodeData(final String[] link, int loopNum, double min) {

		/*
		 * List to store the Strings which represent calculated data (such as
		 * row pairs) to be added to dataTable.
		 */
		final int nodeInfoSize = 4;
		final String[] nodeInfo = new String[nodeInfoSize];

		/*
		 * Create a list that stores the info of the current new node to be
		 * formed. This will be written down in the corresponding tree file.
		 */
		nodeInfo[0] = "NODE" + (loopNum + 1) + "X";
		nodeInfo[1] = link[0];
		nodeInfo[2] = link[1];
		nodeInfo[3] = String.valueOf(1 - min);

		return nodeInfo;
	}

	/**
	 * Reorders the finalCluster and returns it as a String[].
	 *
	 * @param finalCluster
	 */
	public void reorderRows(final List<Integer> finalCluster) {

		int limit = finalCluster.size();
		this.reorderedRowIdxs = new int[limit];

		for (int i = 0; i < limit; i++) {
			reorderedRowIdxs[i] = finalCluster.get((limit - 1) - i);
		}
	}

	public List<String[]> getTreeNodeData() {
		
		return treeNodeData;
	}
	/**
	 * Getter for the reordered list
	 *
	 * @return The reordered matrix row indices after clustering.
	 */
	public int[] getReorderedIDs() {

		return reorderedRowIdxs;
	}
}
