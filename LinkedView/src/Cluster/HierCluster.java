package Cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
	 * between arrays. If the matrix was represented by a 1D array, column 
	 * iteration would be done by calculating the iterator at every step which
	 * might be additional overhead during already intensive/ complex 
	 * clustering algorithms.
	 */
	private final int linkMethod;
	private String axisPrefix;
	private final int distMatrixSize;
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
	int rowMinIndex = 0;
	int colMinIndex = 0;

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
	 * because this software does agglomerative clustering.
	 */
	private List<List<Integer>> currentClusters;
	private int[][] rowIndexTable;

	private String[][] rowPairs; // needed for connectNodes??? better way?

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
		this.distMatrixSize = distMatrix.getSize();
		
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
		/* Data to be written to file */
		rowPairs = new String[distMatrixSize][];

		/* 
		 * Integer representation of rows for references
		 * in calculations (list of fusedGroups).
		 */
		rowIndexTable = new int[distMatrixSize][];
		
		/* Initialize min with smallest double value */
		min = Double.MIN_VALUE;

		/* 
		 * Deep copy of distance matrix to avoid mutation. Needs to access 
		 * original values during generation of the new row when joining 
		 * two rows. 
		 */
		distMatrixCopy = new DistanceMatrix(0);
		distMatrixCopy.cloneFrom(distMatrix);

		/* 
		 * Groups of row indices to keep track of the formed clusters at all
		 * steps.
		 */
		currentClusters = new ArrayList<List<Integer>>(distMatrixSize);

		/* 
		 * Fill list with integers corresponding to the row indices. 
		 * Initially, every matrix row is its own little cluster.
		 * TODO Investigate: gets a bunch of "null" values added in the back?!
		 */
		for (int i = 0; i < distMatrixSize; i++) {

			final List<Integer> initialCluster = new ArrayList<Integer>(1);
			initialCluster.add(i);
			currentClusters.add(initialCluster);
		}
		
		/* Ensure all needed variables are set up and initialized */
		if(rowPairs == null || rowIndexTable == null || distMatrixCopy == null 
				|| currentClusters == null) {
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
	 * TODO Break up this monster method into more readable & eventually 
	 * testable modules.
	 */
	public int cluster() {

		loopNum = distMatrixSize - distMatrix.getSize();
		
//		LogBuffer.println("Loop: " + loopNum); // Debug
		
/* 
* STEP 1: Find current(!) matrix minimum. Should be larger than the last
* minimum that was found in the previous step! The matrix shrinks as rows are
* clustered together (hence the need for a deep copy!).
*/
		min = distMatrix.findCurrentMin(min);
		rowMinIndex = distMatrix.getMinRowIndex();
		colMinIndex = distMatrix.getMinColIndex();
/* 
 * STEP 2: Link the two clusters, that are closest, in the cluster index list, 
 * which represents the clusters as row indexes of the distance matrix.
 * Use the index values of the minimum value in the current step's 
 * distance matrix. The pair of closest clusters is composed of the cluster
 * at rowMinIndex and the cluster at colMinIndex in currentClusters.
 */

		/* Get the two clusters to be fused */
		/* Get the cluster at rowMinIndex */
		final int[] targetRow = new int[currentClusters.get(rowMinIndex).size()];
		for (int i = 0; i < targetRow.length; i++) {								

			targetRow[i] = currentClusters.get(rowMinIndex).get(i);
		}

		/* Get the cluster at colMinIndex */
		final int[] targetRow2 = new int[currentClusters.get(colMinIndex).size()];
		for (int i = 0; i < targetRow2.length; i++) {								

			targetRow2[i] = currentClusters.get(colMinIndex).get(i);
		}

		/* Fuse the two clusters into a new one. */
		final int[] newCluster = Helper.concatIntArrays(targetRow, targetRow2);			
		
/*
 * Step 3: Match the newly created node with the others.
 */
		/* 
		 * Find out the actual names of the two old clusters and check
		 * if one was already part of a cluster with more than one element
		 * in it. In that case, connect the other cluster to that node.
		 */
		final String[] rowPair = connectNodes(newCluster, targetRow,			
				targetRow2, rowMinIndex, colMinIndex);
/* 
 * Step 4: Write the info about the new node with the two connected clusters
 * into a file. 
 * Update the lists that keep track of currently formed clusters.
 */
		/* 
		 * Add the new node to rowPairs, so the next clusters can be checked
		 * in connectNodes().
		 */
		rowPairs[loopNum] = writeData(rowPair);			

		/* Register clustering of the two rows */
		rowIndexTable[loopNum] = newCluster;

		/* 
		 * Remove the two old clusters from the currentClusters list. 
		 * Rows and columns with bigger list position must be first to avoid 
		 * list shifting issues when currentClusters is updated.
		 */
		if (rowMinIndex > colMinIndex) {
			currentClusters.remove(rowMinIndex);
			currentClusters.remove(colMinIndex);

		} else {
			currentClusters.remove(colMinIndex);
			currentClusters.remove(rowMinIndex);
		}
		
		/* 
		 * Adding the newly formed cluster to the list of current cluster at
		 * the position of the old cluster that contains the minimum row index.
		 */
		final int newClusterMin = findClusterMin(newCluster);						
		
		/* Either of the two rows to be clustered has the minimum */
		final boolean rowClusHasMin = clusterHasMin(newClusterMin, targetRow);	

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
		if (rowClusHasMin && rowMinIndex < newClusterListSize) {
			currentClusters.add(rowMinIndex, newVals);

		/* The node is in the col cluster */
		} else if (!rowClusHasMin && colMinIndex < newClusterListSize) {
			currentClusters.add(colMinIndex, newVals);

		} else {
			currentClusters.add(newVals);
		}

/*
 * Step 5: Update the distance matrix to reflect the newly found node.
 * The two old rows that are clustered together must first be removed and 
 * the new row that contains the distance values of the new cluster towards all
 * other clusters (depending on linkage choice) must be generated.
 */

		/* 
		 * The replacement row for the two removed row elements 
		 * (when joining clusters).
		 */
		double[] newRow;
		
		/* 
		 * newRow contains corresponding values depending on 
		 * the cluster method 
		 * TODO reduce crazy complexity of link methods...
		 */
		if (linkMethod == SINGLE || linkMethod == COMPLETE) {
			newRow = scLink(newCluster);

		} else if (linkMethod == AVG) {
			newRow = avgLink(newCluster);
			
		} else {
			LogBuffer.println("No matching link method found.");
			return -1;
		}

		/* 
		 * First: check whether the row or column contains the 
		 * smallest gene by index of both (fusedGroup) then add a 
		 * newClade value to each element where newClade intersects 
		 * (basically adding the column)
		 */
		if (rowClusHasMin) {
			/* replace element at row with newRow */
			distMatrix.replaceIndex(newRow, rowMinIndex);

			/* 
			 * Remove the other row from distMatrix and keep the previously
			 * replaced row.
			 */
			distMatrix.deleteIndex(colMinIndex);

			/* ? */
			for (int j = rowMinIndex; j < distMatrix.getSize(); j++) {

				final double[] element = distMatrix.getRow(j);
				 /* 
				  * Add to element at index 'row' if the element 
				  * is bigger than the row value otherwise the element
				  * is too small to add a column value
				  */
				if (element.length > rowMinIndex) {
					element[rowMinIndex] = newRow[j];
				}
			}
		} else {
			/* replace element at row with newRow */
			distMatrix.replaceIndex(newRow, colMinIndex);

			/* remove the other row from distMatrix */
			distMatrix.deleteIndex(rowMinIndex);

			/* ? */
			for (int j = colMinIndex; j < distMatrix.getSize(); j++) {

				final double[] element = distMatrix.getRow(j);

				if (element.length > colMinIndex) {
					element[colMinIndex] = newRow[j];
				}
			}
		}
		
		return distMatrix.getSize();
	}
	
	/**
	 * Takes information about a newly formed cluster and writes it to a file.
	 * @param rowPair The pair of clustered rows.
	 * @return
	 */
	public String[] writeData(String[] rowPair) {
		
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
		nodeInfo[1] = rowPair[0];
		nodeInfo[2] = rowPair[1];
		nodeInfo[3] = String.valueOf(1 - min);

		/* Write the node info to the tree output file */
		bufferedWriter.writeContent(nodeInfo);
		
		return nodeInfo;
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
		rowPairs = null;
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
	 * 
	 * @return int
	 */
	public int findClusterMin(final int[] cluster) {

		/* 
		 * Standard collection copy constructor to make deep copy to protect 
		 * 'cluster' from mutation. 
		 * Arrays.sort worst case is O(n^2) in Java 7, though mostly O(n log n)
		 */
		final int[] clusterCopy = cluster.clone();
		Arrays.sort(clusterCopy);

		return clusterCopy[0];
	}

	/**
	 * Checks usedMins if it already contains a given double value.
	 * 
	 * @param min
	 * @return
	 */
	public boolean clusterHasMin(final double min, final int[] rowCluster) {

		for (final int row : rowCluster) {

			if (Helper.nearlyEqual(min, row)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Determines the String name of the current gene pair and checks 
	 * whether either of the two genes is already part of a previously 
	 * formed cluster. In this case the remaining gene will be connected 
	 * to that NODE.
	 * 
	 * Complexity: O(n^2) (for loop + checkDisjoint)
	 * 
	 * @param newCluster
	 * @param rowGroup
	 * @param colGroup
	 * @param row
	 * @param column
	 * @return The pair of matrix rows that have been clustered.
	 */
	public String[] connectNodes(final int[] newCluster, final int[] rowGroup,
			final int[] colGroup, final int row, final int column) {

		/* make Strings for String list to be written to data file */
		String geneRow = "";
		String geneCol = "";

		final int groupSize = 2;

		final String[] dataList = new String[groupSize];

		/* 
		 * Check the lists in dataMatrix whether the genePair you want to 
		 * add now is already in a list from before, if yes connect to 
		 * LATEST node by replacing the gene name with the node name.
		 */
		if (newCluster.length == 2) {
			geneRow = axisPrefix + currentClusters.get(row).get(0) + "X";
			geneCol = axisPrefix + currentClusters.get(column).get(0) + "X";

		}
		/* if size of fusedGroup exceeds 2... */
		else {
			/* 
			 * Move from top down to find the last fusedGroup 
			 * (history of clusters) containing any gene from current colGroup 
			 * so that the correct NODE-connection can be found.
			 */
			for (int j = loopNum - 1; j >= 0; j--) {						

				/* 
				 * This currently gets the last node that has a common 
				 * element if the 2 groups have elements in common...
				 */
				if (rowIndexTable[j] != null) {
					if (!checkDisjoint(rowIndexTable[j], colGroup)) {

						/* 
						 * Assigns NODE # of last fusedGroup containing 
						 * a colGroup element.
						 */
						geneCol = rowPairs[j][0];
						break;
					}
					/* If the current fusedGroup in geneIntegerTable does 
					 * not have any elements in common with 
					 * geneGroups.get(column).
					 */
					else {
						geneCol = axisPrefix + currentClusters.get(column).get(0) 
								+ "X";
					}
				}
			}

			/* 
			 * Move from top down to find the last fusedGroup 
			 * (history of clusters) containing any gene from current 
			 * rowGroup so that the correct NODE-connection can be found.
			 */
			for (int j = loopNum - 1; j >= 0; j--) {							

				/* 
				 * This currently gets the last node that has a common element
				 * if the 2 groups have elements in common...
				 */
				if (rowIndexTable[j] != null) {
					if (!checkDisjoint(rowIndexTable[j], rowGroup)) {

						/* 
						 * Assigns NODE # of last fusedGroup containing
						 * a rowGroup element.
						 */
						geneRow = rowPairs[j][0];
						break;

					} else {
						geneRow = axisPrefix + currentClusters.get(row).get(0) 
								+ "X";
					}
				}
			}
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
	 * Checks if two int[] have elements in common.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public boolean checkDisjoint(final int[] a, final int[] b) {

		int[] toIterate;
		int[] toSearch;

		boolean disjoint = true;

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
					disjoint = false;
				}
			}
		}

		return disjoint;
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
	 * Complexity: O(n^3)?
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
			if (checkDisjoint(currentCluster, fusedClusters)) {
				final double[] distances = new double[fusedClusters.length
						* currentCluster.length];

				int dInd = 0; // Iterator
				for (int j = 0; j < fusedClusters.length; j++) {

					/* select element from new cluster */
					selectedRow = fusedClusters[j];

					/* 
					 * Get the corresponding row in the original, 
					 * non-mutated matrix. 
					 */
					final double[] currentRow = distMatrixCopy.getRow(selectedRow);

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

						distances[dInd] = distanceVal;
						dInd++;
					}
				}

				/* 
				 * Result is a list of all distances between rows in
				 * fusedClusters (the new cluster) and every row in every 
				 * cluster contained in fusedCluster
				 */
				Arrays.sort(distances);
				
				/* 
				 * Single Link - Minimum distance between the new cluster
				 * and the other clusters.
				 */
				if (linkMethod == SINGLE) {
					newRow[i] = distances[0];
				} 
				/* Complete Link - Maximum*/
				else { 
					newRow[i] = distances[distances.length - 1];
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
	 * @return newClade
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
			if (checkDisjoint(currentGroup, fusedGroup)) {

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
