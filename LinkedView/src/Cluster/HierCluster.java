package Cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import Utilities.Helper;
import Controllers.ClusterController;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * Class that performs hierarchical clustering on a supplied distance 
 * matrix. It implements multiple linkage methods and consists of one main
 * method (cluster()) that is used to create one new cluster at a time.
 * 
 * @author CKeil
 * 
 */
public class HierCluster {

	/* Correspond to the JCombobox positions in ClusterView */
	private final static int SINGLE = 0;
	private final static int COMPLETE = 1;
	private final static int AVG = 2;
	
	/*
	 * IMPORTANT NOTE: The variable prefixes row- and col- refer to the 
	 * current distance matrix. This means that if columns of the original
	 * matrix are clustered, they will be rows here. This is because the data
	 * is formatted such that the axis to be clustered will be represented by
	 * an array of arrays and column access doesn't always need to jump
	 * between arrays.
	 */
	private final int linkMethod;
	private String axisPrefix;
	private int initial_matrix_size;
	private int loopNum;

	/* Half of the Distance Matrix (symmetry) */
	private DistanceMatrix distMatrix;

	/* 
	 * Deep copy of distance matrix because original will be mutated. 
	 * The mutation happens when a new row is formed from to rows that
	 * are being clustered together.
	 */
	private DistanceMatrix distMatrixCopy;

	/* list to keep track of previously used minimum values from distMatrix */
	private double min;
	int min_row_index = 0;
	int min_col_index = 0;

	/* 
	 * Reordered list of distance matrix rows. This directly represents
	 * the reordered axis that was selected to be clustered. 
	 */
	private String[] reorderedRows;

	/* Writer that generates the ATR/ GTR files for trees */ 
	private ClusterFileWriter bufferedWriter;

	/* 
	 * List to keep track of all clusters during each iteration of the
	 * clustering loop. Each sublist in this list is a cluster. 
	 * As the loop proceeds, eventually only one sublist/ cluster will remain,
	 * because this program does agglomerative clustering.
	 */
	private List<List<Integer>> currentClusters;
	private int[][] rowIndexTable;

	private String[][] links; // needed for connectNodes??? better way?

	/**
	 * The class that is responsible for the hierarchical clustering of
	 * the supplied distance matrix. It uses input parameters such as the
	 * chosen linkage method and the axis of the original dataset to be 
	 * clustered to perform the clustering. 
	 * @param fileName The name of the original loaded file which will serve
	 * as the basis for the name of the newly generated files.
	 * @param linkMethod The cluster linkage method chosen by the user.
	 * @param distMatrix The distance matrix calculated in the previous step.
	 * @param axis The axis of the original dataset which will be clustered.
	 * @param worker The worker thread which performs the clustering. Needs to
	 * be interrupted in this class if the user cancels the operation.
	 */
	public HierCluster(String fileName, final int linkMethod, 
			final DistanceMatrix distMatrix, final int axis) {

		LogBuffer.println("Initializing HierCluster.");
		
		this.linkMethod = linkMethod;
		this.distMatrix = distMatrix;
		this.initial_matrix_size = distMatrix.getSize();
		
		prepareCluster();
	}
	
	/**
	 * Goes through a bunch of steps to prepare the object and data for
	 * clustering. Initializes important variables, sets up lists to be used
	 * and makes a deep copy of the distance matrix so there can be a
	 * reference to a non-mutated distance matrix.
	 */
	public void prepareCluster() {

		LogBuffer.println("Preparing for cluster.");
		
		/* 
		 * Deep copy of distance matrix to avoid mutation. Needs to access 
		 * original values during generation of the new row when joining 
		 * two rows. 
		 */
		distMatrixCopy = new DistanceMatrix(0);
		distMatrixCopy.cloneFrom(distMatrix);
		
		/* Data to be written to file */
		links = new String[initial_matrix_size][];

		/* 
		 * Integer representation of rows for references
		 * in calculations (list of fusedGroups). Keeps track of which cluster
		 * was formed at which step.
		 */
		rowIndexTable = new int[initial_matrix_size][];
		
		/* Initialize min with smallest double value */
		min = Double.MIN_VALUE;

		/* 
		 * Groups of row indices to keep track of the formed clusters at all
		 * steps.
		 */
		currentClusters = new ArrayList<List<Integer>>(initial_matrix_size);

		/* 
		 * Fill list with integers corresponding to the row indices. 
		 * Initially, every matrix row is its own little cluster.
		 */
		for (int i = 0; i < initial_matrix_size; i++) {

			final List<Integer> initialCluster = new ArrayList<Integer>(1);
			initialCluster.add(i);
			currentClusters.add(initialCluster);
		}
		
		/* Ensure all needed variables are set up and initialized */
		if(links == null || rowIndexTable == null || distMatrixCopy == null 
				|| currentClusters == null 
				|| (distMatrixCopy.getSize() != distMatrix.getSize())) {
			String message = "Looks like clustering was not properly "
					+ "set up by the software. Can't proceed with "
					+ "clustering.";
			JOptionPane.showMessageDialog(JFrame.getFrames()[0], message, 
					"Error", JOptionPane.ERROR_MESSAGE);
			LogBuffer.println("Cluster preparation failed.");
			return;
		}
		
		LogBuffer.println("Cluster preparation successful.");
	}

	/** 
	 * Hierarchically clusters the distance matrix and stores results in
	 * an array of reordered distance matrix row indices.
	 */
	public int cluster() {

		loopNum = initial_matrix_size - distMatrix.getSize();
		
		/* STEP 1: Find current(!) matrix minimum. */
		
		getMatrixMinimum();
		
		/* STEP 2: Link the two clusters with the minimum distance */
		
		final int[] newCluster = linkClosestClusters();
		
		/* STEP 3: Write info about the new connection to file. */
		
		final String[] link = 
				connectNodes(newCluster, min_row_index, min_col_index);
		/* 
		 * Add the new node to links, so the next clusters can be checked
		 * in connectNodes().
		 */
		links[loopNum] = writeData(link);	
		
		/* STEP 4: Update the lists that keep track of clusters. */		

		/* Register clustering of the two rows */
		rowIndexTable[loopNum] = newCluster;
		
		boolean rowClusHasMin = updateCurrentClusters(newCluster);
		
		/* STEP 5: Generate new row based on the chosen linkage method. */

		/* 
		 * The replacement row for the two removed row elements 
		 * (when joining clusters). Will contain corresponding values
		 * depending on the cluster method.
		 */
		double[] newRow;
		
		if (linkMethod == SINGLE || linkMethod == COMPLETE) {
			newRow = scLink(newCluster);

		} else if (linkMethod == AVG) {
			newRow = avgLink(newCluster);
			
		} else {
			LogBuffer.println("No matching link method found.");
			return -1;
		}
		
		/* STEP 6: Update the distance matrix to reflect the new cluster. */
		
		updateDistMatrix(rowClusHasMin, newRow);
		
		return distMatrix.getSize();
	}
	
	/* -------------------Cluster Methods -----------------------------*/
	/**
	 * Finds the minimum of the current distance matrix and notes its 
	 * coordinates in the matrix. Should be larger than the last minimum 
	 * that was found in the previous step! The matrix shrinks as rows are 
	 * clustered together (hence the need for a deep copy!).
	 */
	private void getMatrixMinimum() {
		
		this.min = distMatrix.findCurrentMin(min);
		this.min_row_index = distMatrix.getMinRowIndex();
		this.min_col_index = distMatrix.getMinColIndex();
	}
	
	/**
	 * Link the two clusters, that are closest in the cluster index list, 
	 * which represents the clusters as row indexes of the distance matrix.
	 * Use the index values of the minimum value in the current step's 
	 * distance matrix. The pair of closest clusters is composed of the cluster
	 * at rowMinIndex and the cluster at colMinIndex in currentClusters.
	 * @return The new cluster.
	 */
	private int[] linkClosestClusters() {
		
		/* Get the two clusters to be fused */
		/* Get the cluster at rowMinIndex */
		final int[] row_cluster = 
				new int[currentClusters.get(min_row_index).size()];
		for (int i = 0; i < row_cluster.length; i++) {								

			row_cluster[i] = currentClusters.get(min_row_index).get(i);
		}

		/* Get the cluster at colMinIndex */
		final int[] col_cluster = 
				new int[currentClusters.get(min_col_index).size()];
		for (int i = 0; i < col_cluster.length; i++) {								

			col_cluster[i] = currentClusters.get(min_col_index).get(i);
		}
		
		return Helper.concatIntArrays(row_cluster, col_cluster);
	}
	
	/**
	 * Takes information about a newly formed cluster and writes it to a file.
	 * @param link The pair of clustered rows.
	 * @return
	 */
	public String[] writeData(String[] link) {
		
		/*
		 *  List to store the Strings which represent calculated data 
		 *  (such as row pairs) to be added to dataTable.
		 */
		final int nodeInfoSize = 4;
		final String[] nodeInfo = new String[nodeInfoSize];
		
		/* 
		 * Create a list that stores the info of the current new node 
		 * to be formed. This will be written down in the corresponding 
		 * tree file. 
	     */
		nodeInfo[0] = "NODE" + (loopNum + 1) + "X";
		nodeInfo[1] = link[0];
		nodeInfo[2] = link[1];
		nodeInfo[3] = String.valueOf(1 - min);

		/* Write the node info to the tree output file */
		bufferedWriter.writeContent(nodeInfo);
		
		return nodeInfo;
	}
	
	/**
	 * Updates the list of all current clusters by removing the old clusters
	 * and adding the newly formed cluster at the appropriate index. 
	 * @param newCluster
	 * @return Whether the row cluster contains the minimum.
	 */
	private boolean updateCurrentClusters(int[] newCluster) {
		
		/* 
		 * Adding the newly formed cluster to the list of current cluster at
		 * the position of the old cluster that contains the minimum row index.
		 */
		int newClusterMin = findClusterMin(newCluster);						
		
		/* Either of the two rows to be clustered has the minimum */
		boolean rowClusHasMin = clusterHasMin(newClusterMin, min_row_index);
		
		/* 
		 * Remove the two old clusters from the currentClusters list. 
		 * Rows and columns with bigger list position must be first to avoid 
		 * list shifting issues when currentClusters is updated.
		 */
		if (min_row_index > min_col_index) {
			currentClusters.remove(min_row_index);
			currentClusters.remove(min_col_index);

		} else {
			currentClusters.remove(min_col_index);
			currentClusters.remove(min_row_index);
		}	

		/* 
		 * Transform newCluster array into a list so it can be added
		 * to currentClusters
		 */
		final List<Integer> newVals = new ArrayList<Integer>();
		for (int i = 0; i < newCluster.length; i++) {							

			newVals.add(newCluster[i]);
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
	 * @param rowClusHasMin
	 * @param newRow
	 */
	private void updateDistMatrix(boolean rowClusHasMin, double[] newRow) {
		
		/* 
		 * First: check whether the row or column contains the 
		 * smallest gene by index of both (fusedGroup) then add a 
		 * newClade value to each element where newClade intersects 
		 * (basically adding the column)
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
				  * Add to element at index 'row' if the element 
				  * is bigger than the row value otherwise the element
				  * is too small to add a column value
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
	 * Finishes up clustering. Closes the bufferedWriter and causes the list
	 * of reordered distance matrix rows to be generated.
	 * Also sets the variables that store the most data to null, to ensure 
	 * garbage collection.
	 */
	public void finish() {

		bufferedWriter.closeWriter();
		reorderRows(currentClusters.get(0));

		/* Ensure garbage collection for large objects */
		distMatrixCopy = null;
		links = null;
		currentClusters = null;
	}

	/**
	 * Sets up a buffered writer to write & save the tree files (GTR & ATR).
	 * Also sets the filePath to the directory in which the resulting file was
	 * saved.
	 * Cancels the cluster worker and alerts the user if there's a problem
	 * with setting up the buffered writer since there wouldn't be a filePath
	 * where the cluster data could be saved anyways.
	 */
	public void setupFileWriter(int axis, String fileName) {

		LogBuffer.println("Setting up file writer in HierCluster.");
		
		/* 
		 * Setting up labels for ATR and GTR tree files and 
		 * file name for writing. 
		 */
		String fileSuffix = ".notchanged";
		if(axis == ClusterController.ROW) {
			this.axisPrefix = "GENE";
			fileSuffix = ".gtr";
			
		} else {
			this.axisPrefix = "ARRY";
			fileSuffix = ".atr";
		}
		
		fileName += fileSuffix;
		
		final File file = new File(fileName);

		try {
			file.createNewFile();
			bufferedWriter = new ClusterFileWriter(file);
			
		} catch (IOException e) {
			LogBuffer.logException(e);
			String message = "There was trouble when trying to setup the"
					+ "buffered writer to save ATR or GTR files.";
			JOptionPane.showMessageDialog(JFrame.getFrames()[0], message, 
					"Error", JOptionPane.ERROR_MESSAGE);
			LogBuffer.println("Setting up buffered writer failed.");
			return;
		}
		
		LogBuffer.println("Tree file writer setup successful.");
	}

	/**
	 * Find minimum of an integer array that represents a cluster.
	 * O(n)
	 * 
	 * @return Minimum of the supplied int array.
	 */
	public int findClusterMin(final int[] cluster) {

		int min = Integer.MAX_VALUE;
		
		for(int row : cluster) {
			
			if(row < min) min = row;
		}

		return min;
	}

	/**
	 * Checks a cluster if it contains a certain value.
	 * @param min
	 * @return
	 */
	public boolean clusterHasMin(final int min, final int index) {

		List<Integer> cluster = currentClusters.get(index);
		for (final int row : cluster) {

			if (min == row) return true;
		}

		return false;
	}

	/**
	 * Determines the String name of the cluster pair to be linked and checks 
	 * whether either of the two rows is already part of a previously 
	 * formed cluster. In this case the remaining row will be connected 
	 * to that NODE.
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
		 * Check the record of clusters (rowIndexTable) whether a part 
		 * of the new cluster is already part of a previous cluster. 
		 * If yes connect to LAST node by replacing the gene name with 
		 * the node name for the tree file.
		 */
		if (newCluster.length == 2) {
			geneRow = axisPrefix + currentClusters.get(row).get(0) + "X";
			geneCol = axisPrefix + currentClusters.get(column).get(0) + "X";

		}
		/* If size of new cluster exceeds 2 */
		else {
			geneRow = findLastClusterMatch(row);
			geneCol = findLastClusterMatch(column);
		}

		/* 
		 * Check if only one of the two String has a "NODE" component. 
		 * If yes, position it at [1].
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
	 * Move from top down to find the last cluster containing 
	 * any row from current colGroup so that the correct 
	 * NODE-connection can be found.
	 */
	private String findLastClusterMatch(int index) {
		
		String name = "";
		
		int[] group = new int[currentClusters.get(index).size()];
		for (int i = 0; i < group.length; i++) {

			group[i] = currentClusters.get(index).get(i);
		}
		
		for (int j = loopNum - 1; j >= 0; j--) {						

			/* 
			 * This currently gets the last node that has a common 
			 * element if the 2 groups have elements in common...
			 */
			if (rowIndexTable[j] != null) {
				if (shareCommonElements(rowIndexTable[j], group)) {

					/* 
					 * Assigns NODE # of last fusedGroup containing 
					 * a colGroup element.
					 */
					name = links[j][0];
					break;
				}
				/* If the current fusedGroup in geneIntegerTable does 
				 * not have any elements in common with 
				 * geneGroups.get(column).
				 */
				else {
					name = axisPrefix + currentClusters.get(index).get(0) + "X";
				}
			}
		}
		
		return name;
	}

	/**
	 * Checks if two int[] have elements in common.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public boolean shareCommonElements(final int[] a, final int[] b) {

		int[] toIterate;
		int[] toSearch;

		boolean joint = false;

		if (a.length > b.length) {
			toIterate = b;
			toSearch = a;

		} else {
			toIterate = a;
			toSearch = b;
		}

		for (int i = 0; i < toIterate.length; i++) {

			for (int j = 0; j < toSearch.length; j++) {

				if (toIterate[i] == toSearch[j]) {
					joint = true;
					break;
				}
				
				if(joint) break;
			}
		}

		return joint;
	}

	/**
	 * Reorders the finalCluster and returns it as a String[].
	 * 
	 * @param finalCluster
	 */
	public void reorderRows(final List<Integer> finalCluster) {

		String element = "";

		reorderedRows = new String[finalCluster.size()];

		for (int i = 0; i < finalCluster.size(); i++) {

			element = axisPrefix + finalCluster.get(i) + "X";
			reorderedRows[i] = element;
		}
	}

	/**
	 * Method used to generate a new row/col for the distance matrix which is
	 * processed. The new row/col represents the joint row pair which has been
	 * chosen as the one with the minimum distance in the current iteration. 
	 * The values of the new row/col are calculated as maximum (complete) 
	 * or minimum (single) of all distance values.
	 * 
	 * Time complexity: ?
	 * 
	 * @param fusedClusters
	 * @return newRow
	 */
	public double[] scLink(final int[] fusedClusters) {

		/* Make a new row that has the same size as the number of clusters. */
		final double[] newRow = new double[currentClusters.size()];

		/* Iterate over all clusters */
		for (int i = 0; i < currentClusters.size(); i++) {

			double distanceVal = 0;
			int selectedRow = 0;

			final int[] currentCluster = new int[currentClusters.get(i).size()];

			/* Filling the array */
			for (int z = 0; z < currentCluster.length; z++) {

				currentCluster[z] = currentClusters.get(i).get(z);
			}

			/* 
			 * Only calculate distance if the current cluster is not part
			 * of the newly formed cluster. If it is, then the distance is 0.
			 */
			if (!shareCommonElements(currentCluster, fusedClusters)) {
				final double[] distances = new double[fusedClusters.length
						* currentCluster.length];
				
				double min = Double.MAX_VALUE;
				double max = Double.MIN_VALUE;

				int dInd = 0; // Iterator
				for (int j = 0; j < fusedClusters.length; j++) {

					/* select element from new cluster */
					selectedRow = fusedClusters[j];

					/* 
					 * Get the corresponding row in the original, 
					 * non-mutated matrix. 
					 */
					double[] currentRow = distMatrixCopy.getRow(selectedRow);

					/* 
					 * Go through all clusters and their elements.
					 * Finds the distances between a row in currentCluster
					 * (remaining non-clustered) and all rows in the new cluster
					 * (fusedClusters, current row cluster).
					 */
					for (int k = 0; k < currentCluster.length; k++) {

						// if-else to allow for use of halfDMatrix
						if (currentRow.length > currentCluster[k]) {
							/* 
							 * distance value in the current row at the indices
							 * which correspond to the elements of the new
							 * current cluster. 
							 */
							distanceVal = currentRow[currentCluster[k]];

						} else {
							/* 
							 * Reverse index access because of distance
							 * matrix symmetry.
							 */
							distanceVal = distMatrixCopy.getRow(
									currentCluster[k])[selectedRow];
						}
						
						/* Determine min and max */
						if(distanceVal < min) min = distanceVal;
						if(distanceVal > max) max = distanceVal;

						distances[dInd] = distanceVal;
						dInd++;
					}
				}
				
				/* 
				 * Single Link - Minimum distance between the new cluster
				 * and the other clusters.
				 */
				if (linkMethod == SINGLE) {
					newRow[i] = min;
				} 
				/* Complete Link - Maximum*/
				else { 
					newRow[i] = max;
				}
			}
			/* all elements in common */
			else {
				newRow[i] = 0.0;
			}
		}

		return newRow;
	}

	/**
	 * Method used to generate a new row/col for the distance matrix which is
	 * processed. The new row/col represents the joint gene pair which has been
	 * chosen as the one with the minimum distance in each iteration. The values
	 * of the new row/col are calculated as average of the sum of all distances.
	 * 
	 * @param fusedGroup 
	 * @param currentClusters
	 * @param newRow
	 * @return newRow
	 */
	public double[] avgLink(final int[] fusedGroup) {

		final double[] newRow = new double[currentClusters.size()];

		for (int i = 0; i < currentClusters.size(); i++) {

			double distanceSum = 0;
			double distanceVal = 0;
			int selectedGene = 0;

			final int[] currentGroup = new int[currentClusters.get(i).size()];

			for (int z = 0; z < currentGroup.length; z++) {

				currentGroup[z] = currentClusters.get(i).get(z);
			}

			/* 
			 * Check if fusedGroup contains the current checked gene 
			 * (then no mean should be calculated) no elements in common.
			 */
			if (!shareCommonElements(currentGroup, fusedGroup)) {

				/* select members of the new row (B & G) */
				for (int j = 0; j < fusedGroup.length; j++) {

					selectedGene = fusedGroup[j];

					/* 
					 * Take a row (gene) from the matrix which also appears 
					 * in the fusedGroup (current cluster).
					 */
					double[] currentRow = distMatrixCopy.getRow(selectedGene);

					/* 
					 * Go through all clusters and their contained genes
					 * calculate the distance between each column (gene) 
					 * and the current row (gene) from fusedGroup sum the 
					 * distance values up.
					 */
					for (final int gene : currentClusters.get(i)) {

						if (currentRow.length > gene) {
							distanceVal = currentRow[gene];

						} else {
							distanceVal = 
									distMatrixCopy.getRow(gene)[selectedGene];
						}
						distanceSum += distanceVal;
					}
				}

				/* 
				 * Calculate the new value to be added to newRow as the 
				 * average of distances.
				 */
				newRow[i] = distanceSum / (fusedGroup.length 
						* currentGroup.length);
			}
			/* all elements in common */
			else {
				newRow[i] = 0.0;
			}
		}

		return newRow;
	}

	/**
	 * Getter for the reordered list
	 * 
	 * @return The reordered list of matrix elements after clustering. 
	 */
	public String[] getReorderedList() {

		return reorderedRows;
	}
}
