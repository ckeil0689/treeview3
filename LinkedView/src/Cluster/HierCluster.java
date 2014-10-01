package Cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import Utilities.Helper;
import Controllers.ClusterController;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * Test class to assess whether the operation can work on half a matrix (due to
 * symmetry)
 * 
 * @author CKeil
 * 
 */
public class HierCluster {

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
	private final double EPSILON = 0.0000000001; // float comparison!
	private final String linkMethod;
	private final String axisPrefix;
	private final int distMatrixSize;
	private int loopNum;

	private final SwingWorker<String[], Integer> worker;

	/* Half of the Distance Matrix (symmetry) */
	private double[][] distMatrix;

	/* 
	 * Deep copy of distance matrix because original will be mutated. 
	 * The mutation happens when a new row is formed from to rows that
	 * are being clustered together.
	 */
	private double[][] distMatrixCopy;

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
	 * because this software does agglomoerative clustering.
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
	public HierCluster(String fileName, final String linkMethod, 
			final double[][] distMatrix, final int axis, 
			final SwingWorker<String[], Integer> worker) {

		LogBuffer.println("Initializing HierCluster.");
		this.linkMethod = linkMethod;
		this.distMatrix = distMatrix;
		this.distMatrixSize = distMatrix.length;
		this.worker = worker;
		
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
		setupFileWriter(fileName);
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
		 * Deep copy of distance matrix to avoid later mutation. 
		 * Needed to access values during generation of new row after 
		 * joining two rows. 
		 * TODO Can this be avoided? A smarter solution?
		 */
		distMatrixCopy = deepCopy(distMatrix);

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
			worker.cancel(true);
		}
		
		LogBuffer.println("Done preparing.");
	}

	/** 
	 * Hierarchically clusters the distance matrix and stores results in
	 * an array of reordered distance matrix row indices. 
	 * TODO Break up this monster method into more readable & eventually 
	 * testable modules.
	 */
	public int cluster() {

		loopNum = distMatrixSize - distMatrix.length;
		
//		LogBuffer.println("Loop: " + loopNum); // Debug
		
/* 
* STEP 1: Find current(!) matrix minimum. Should be larger than the last
* minimum that was found in the previous step! The matrix shrinks as rows are
* clustered together (hence the need for a deep copy!).
*/			
		min = findMatrixMin();
/* 
 * STEP 2: Link the two clusters, that are closest, in the cluster index list, 
 * which represents the clusters as row indexes of the distance matrix.
 * Use the index values of the minimum value in the current step's 
 * distance matrix. The pair of closest clusters is composed of the cluster
 * at rowMinIndex and the cluster at colMinIndex in currentClusters.
 */
		/* 
		 * The replacement row for the two removed row elements 
		 * (when joining clusters).
		 */
		double[] newRow = new double[loopNum];

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
		final int[] newCluster = concatArrays(targetRow, targetRow2);

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
		/* TODO These function are wrong for row/ col = 0 */
		final int groupMin = findGroupMin(newCluster);
		final boolean rowGroupHasMin = checkGroupForMin(groupMin, 
				targetRow);
		final boolean colGroupHasMin = checkGroupForMin(groupMin, 
				targetRow2);

		/* Transform newCluster array into a list */
		final List<Integer> newVals = new ArrayList<Integer>();
		for (int i = 0; i < newCluster.length; i++) {

			newVals.add(newCluster[i]);
		}

		final int newClusterListSize = currentClusters.size() + 1;
		if (rowGroupHasMin && rowMinIndex < newClusterListSize) {
			currentClusters.add(rowMinIndex, newVals);

		} else if (colGroupHasMin && colMinIndex < newClusterListSize) {
			currentClusters.add(colMinIndex, newVals);

		} else if ((rowGroupHasMin && rowMinIndex == newClusterListSize)
				|| (colGroupHasMin && colMinIndex == newClusterListSize)) {
			currentClusters.add(newVals);

		} else {
			LogBuffer.println("Problem adding newCluster to currentClusters.");
		}

/*
 * Step 5: Update the distance matrix to reflect the newly found node.
 * The two old rows that are clustered together must first be removed and 
 * the new row that contains the distance values of the new cluster towards all
 * other clusters (depending on linkage choice) must be generated.
 */

		/* 
		 * newRow contains corresponding values depending on 
		 * the cluster method 
		 */
		if (linkMethod.contentEquals("Single Linkage")
				|| linkMethod.contentEquals("Complete Linkage")) {
			newRow = newRowGenSC(newCluster);

		} else if (linkMethod.contentEquals("Average Linkage")) {
			newRow = newRowGenAverage(newCluster);
		}

		/* 
		 * first: check whether the row or column contains the 
		 * smallest gene by index of both (fusedGroup) then add a 
		 * newClade value to each element where newClade intersects 
		 * (basically adding the column)
		 */
		if (rowGroupHasMin) {
			/* replace element at row with newRow */
			replaceRow(rowMinIndex, newRow);

			/* remove the other row from distMatrix*/
			updateDistMatrix(colMinIndex, rowMinIndex);

			for (int j = rowMinIndex; j < distMatrix.length; j++) {

				final double[] element = distMatrix[j];
				 /* add to element at index 'row' if the element 
				  * is bigger than the row value otherwise the element
				  * is too small to add a column value
				  */
				if (element.length > rowMinIndex) {
					element[rowMinIndex] = newRow[j];
				}
			}
		} else if (colGroupHasMin) {
			/* replace element at row with newRow */
			replaceRow(colMinIndex, newRow);

			/* remove the other row from distMatrix */
			updateDistMatrix(rowMinIndex, colMinIndex);

			for (int j = colMinIndex; j < distMatrix.length; j++) {

				final double[] element = distMatrix[j];

				if (element.length > colMinIndex) {
					element[colMinIndex] = newRow[j];
				}
			}
		} else {
			LogBuffer.println("Weird error. Neither "
					+ "rowGroup nor colGroup have a minimum.");
		}
		
		return distMatrix.length;
	}
	
	public String[] writeData(String[] rowPair) {
		
		/*
		 *  list to store the Strings which represent calculated data 
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
	
	public void finish() {

		bufferedWriter.closeWriter();
		reorderGen(currentClusters.get(0));

		/* Ensure garbage collection for large objects */
		distMatrixCopy = null;
		rowPairs = null;
		currentClusters = null;
	}
	
	/**
	 * Finds and returns the current minimum value in the cluster matrix.
	 * The minimum value determines which rows are closest together and will
	 * be clustered. They will form a new row that replaces the other two and
	 * as a result a new minimum must be found at each step to determine the
	 * new row pair to be clustered.
	 * @return The minimum value in the current distance matrix.
	 */
	public double findMatrixMin() {
		
		/* New min must be bigger than previous matrix min */
		double newMin = Double.MAX_VALUE;
		
		for(int i = 0; i < distMatrix.length; i++) {
			
			for(int j = 0; j < distMatrix[i].length; j++) {
				
				double element = distMatrix[i][j];
				
				if((element > min || Helper.nearlyEqual(element, min, EPSILON)) 
						&& element < newMin) {
					newMin = element;
					rowMinIndex = i;
					colMinIndex = j;
				}
			}
		}
		
		return newMin;
	}

	/**
	 * Replaces a row in the distance matrix by first making a new matrix, 
	 * then making a deep copy of the newRow to be added, and finally
	 * of it
	 * @param repInd The index of the row in distMatrix that will be replaced.
	 * @param newRow The new row to replace the old row.
	 */
	public void replaceRow(final int repInd, final double[] newRow) {

		final double[][] newMatrix = new double[distMatrix.length][];

		final double[] replacementRow = new double[repInd];
		System.arraycopy(newRow, 0, replacementRow, 0, repInd);

		for (int i = 0; i < newMatrix.length; i++) {

			if (i != repInd) {
				newMatrix[i] = distMatrix[i];

			} else {
				newMatrix[i] = replacementRow;
			}
		}

		distMatrix = newMatrix;
	}

	/**
	 * Sets up a buffered writer to write & save the tree files (GTR & ATR).
	 * Also sets the filePath to the directory in which the resulting file was
	 * saved.
	 * Cancels the cluster worker and alerts the user if there's a problem
	 * with setting up the buffered writer since there wouldn't be a filePath
	 * where the cluster data could be saved anyways.
	 */
	public void setupFileWriter(String fileName) {

		LogBuffer.println("Setting up file writer in HierCluster.");
		
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
			worker.cancel(true);
		}
	}

	/**
	 * This method updates the old distance matrix by replacing the two rows 
	 * which are clustered together with the new row.
	 * 
	 * @param removeIndex Index of row to be removed from distance matrix.
	 * @param keepIndex Index of row that will be replaced with the newly
	 * generated row.
	 */
	public void updateDistMatrix(final int removeIndex, final int keepIndex) {

		final double[][] newMatrix = new double[distMatrix.length - 1][];

		/* This should shift the elements of distMatrix up by one 
		 * once it reaches the index which should be removed.
		 * A double of the last array in the element should remain at the end.
		 */
		for (int i = 0; i < newMatrix.length; i++) {

			if (i < removeIndex) {
				newMatrix[i] = distMatrix[i];

			} else if (i >= removeIndex) {
				final double[] newElement = distMatrix[i + 1];
				newMatrix[i] = newElement;
			}
		}

		/* This shrinks the current element after to a max size of '***Index' 
		 * after this the longest element in distMatrix has size '***Index'
		 */
		if (removeIndex > keepIndex) {
			for (int i = removeIndex; i < newMatrix.length; i++) {

				final double[] element = newMatrix[i];
				newMatrix[i] = removeCol(element, removeIndex);
			}
		} else {
			for (int i = keepIndex; i < newMatrix.length; i++) {

				final double[] element = newMatrix[i];
				newMatrix[i] = removeCol(element, keepIndex);
			}
		}

		distMatrix = newMatrix;
	}

	/**
	 * Removes one value from a double[] array by making a new array without
	 * this value and with a length of array.length - 1.
	 * 
	 * @param array Array from which a value should be removed.
	 * @param toDelete The index of the value to be removed.
	 * @return The supplied array with the value at toDelete removed.
	 */
	public double[] removeCol(final double[] array, final int toDelete) {

		final double[] newArray = new double[array.length - 1];

		for (int i = 0; i < newArray.length; i++) {

			if (i < toDelete) {
				newArray[i] = array[i];

			} else {
				newArray[i] = array[i + 1];
			}
		}

		return newArray;
	}

	/**
	 * Finds the index of a value in a double array.
	 * 
	 * @param array
	 * @param value
	 * @return
	 */
	public int findValue(final double[] array, final double value) {

		for (int i = 0; i < array.length; i++) {

			if (Helper.nearlyEqual(array[i], value, EPSILON)) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Finds the index of a value in a double array.
	 * 
	 * @param array
	 * @param value
	 * @return
	 */
	public int findArrayInDM(final double[] array) {

		for (int i = 0; i < array.length; i++) {

			if (!checkDisjoint(distMatrix[i], array)) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Fuses two arrays together.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public int[] concatArrays(final int[] a, final int[] b) {

		final int[] c = new int[a.length + b.length];

		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);

		return c;
	}

	/**
	 * Method to make deep copy of distance matrix
	 * 
	 * @return
	 */
	public double[][] deepCopy(final double[][] distanceMatrix) {

		final double[][] deepCopy = new double[distMatrixSize][];

		for (int i = 0; i < distanceMatrix.length; i++) {

			final double[] oldList = distanceMatrix[i];
			final double[] newList = new double[oldList.length];

			System.arraycopy(oldList, 0, newList, 0, oldList.length);

			deepCopy[i] = newList;
		}

		return deepCopy;
	}

	/**
	 * Make trial the minimum value of that gene check whether min was used
	 * before (in whole calculation process)<--- Why? TODO
	 * 
	 * @return double trial
	 */
	public int findGroupMin(final int[] row) {

		// standard collection copy constructor to make deep copy to protect
		// 'row' from mutation.
		final int[] rowCopy = row.clone();
		Arrays.sort(rowCopy);

		return rowCopy[0];
	}

	/**
	 * Checks usedMins if it already contains a given double value.
	 * 
	 * @param min
	 * @return
	 */
	public boolean checkGroupForMin(final double min, final int[] group) {

		boolean contains = false;

		for (final int gene : group) {

			if (min == gene) {
				contains = true;
				break;
			}
		}

		return contains;
	}

	/**
	 * Determines the String name of the current gene pair and checks 
	 * whether either of the two genes is already part of a previously 
	 * formed cluster. In this case the remaining gene will be connected 
	 * to that NODE.
	 * 
	 * @param fusedGroup
	 * @param rowGroup
	 * @param colGroup
	 * @param currentClusters
	 * @param row
	 * @param column
	 * @param rowIndexTable
	 * @return
	 */
	public String[] connectNodes(final int[] fusedGroup, final int[] rowGroup,
			final int[] colGroup, final int row, final int column) {

		// make Strings for String list to be written to data file
		String geneRow = "";
		String geneCol = "";

		final int groupSize = 2;

		final String[] dataList = new String[groupSize];

		// check the lists in dataMatrix whether the genePair you want
		// to add now is already in a list from before, if yes connect
		// to LATEST node by replacing the gene name with the node name
		if (fusedGroup.length == 2) {
			geneRow = axisPrefix + currentClusters.get(row).get(0) + "X";
			geneCol = axisPrefix + currentClusters.get(column).get(0) + "X";

		}
		// if size of fusedGroup exceeds 2...
		else {
			// move from top down to find the last fusedGroup
			// (history of clusters) containing any gene from current
			// colGroup so that the correct NODE-connection can be found
			for (int j = loopNum - 1; j >= 0; j--) {

				// this currently gets the last node that has a common element
				// if the 2 groups have elements in common...
				if (rowIndexTable[j] != null) {
					if (!checkDisjoint(rowIndexTable[j], colGroup)) {

						// assigns NODE # of last fusedGroup containing
						// a colGroup element
						geneCol = rowPairs[j][0];
						break;
					}
					// if the current fusedGroup in geneIntegerTable
					// does not have any elements in common
					// with geneGroups.get(column)
					else {
						geneCol = axisPrefix + currentClusters.get(column).get(0) 
								+ "X";
					}
				}
			}

			// move from top down to find the last fusedGroup
			// (history of clusters) containing any gene from
			// current rowGroup so that the correct NODE-connection can be found
			for (int j = loopNum - 1; j >= 0; j--) {

				// this currently gets the last node that has a common element
				// if the 2 groups have elements in common...
				if (rowIndexTable[j] != null) {
					if (!checkDisjoint(rowIndexTable[j], rowGroup)) {

						// assigns NODE # of last fusedGroup containing
						// a rowGroup element
						geneRow = rowPairs[j][0];
						break;

					} else {
						// random fix to see what happens
						geneRow = axisPrefix + currentClusters.get(row).get(0) + "X";
					}
				}
			}
		}

		// Check if only one of the two String has a "NODE" component.
		// If yes, position it at [1].
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
	 * Checks if two double[] have ALL elements in common.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public boolean checkDisjoint(final double[] a, final double[] b) {

		double[] toIterate;
		double[] toSearch;

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

				if (Helper.nearlyEqual(toIterate[i], toSearch[j], EPSILON)) {
					disjoint = false;
				} else {
					disjoint = true;
					break;
				}
			}
		}

		return disjoint;
	}

	/**
	 * Reorders the finalCluster and returns it as a String[]
	 * 
	 * @param finalCluster
	 */
	public void reorderGen(final List<Integer> finalCluster) {

		String element = "";

		reorderedRows = new String[finalCluster.size()];

		for (int i = 0; i < finalCluster.size(); i++) {

			element = axisPrefix + finalCluster.get(i) + "X";
			reorderedRows[i] = element;
		}
	}

	/**
	 * Method used to generate a new row/col for the distance matrix which is
	 * processed. The new row/col represents the joint gene pair which has been
	 * chosen as the one with the minimum distance in each iteration. The values
	 * of the new row/col are calculated as maximum (complete) or minimum
	 * (single) of all distance values.
	 * 
	 * @param fusedGroup
	 * @param currentClusters
	 * @param newRow
	 * @return newClade
	 */
	public double[] newRowGenSC(final int[] fusedGroup) {

		final double[] newRow = new double[currentClusters.size()];

		for (int i = 0; i < currentClusters.size(); i++) {

			double newRowVal = 0;
			double distanceVal = 0;
			int selectedRow = 0;

			final int[] currentGroup = new int[currentClusters.get(i).size()];

			for (int z = 0; z < currentGroup.length; z++) {

				currentGroup[z] = currentClusters.get(i).get(z);
			}

			// check if fusedGroup contains the current checked gene
			// (then no mean should be calculated) no elements in common
			if (checkDisjoint(currentGroup, fusedGroup)) {
				final double[] distances = new double[fusedGroup.length
						* currentGroup.length];

				int dInd = 0;
				for (int j = 0; j < fusedGroup.length; j++) {

					selectedRow = fusedGroup[j];

					// use halfDMatrix instead and just reverse the index
					// access if a list is too short for the requested
					// index since the distance matrix is symmetrical

					/* distMatrix is getting mutated here. */
					final double[] currentRow = distMatrixCopy[selectedRow];

					// go through all clusters and their contained genes
					// finds the distances between a gene in geneGroup
					// (remaining non-clustered) and all genes in
					// fusedGroup (current gene cluster)
					for (int k = 0; k < currentGroup.length; k++) {

						// if-else to allow for use of halfDMatrix
						if (currentRow.length > currentGroup[k]) {
							distanceVal = currentRow[currentGroup[k]];

						} else {
							// reverse index access because of distance
							// matrix symmetry
							distanceVal = distMatrixCopy[currentGroup[k]][selectedRow];
						}

						distances[dInd] = distanceVal;
						dInd++;
					}
				}

				// result is a list of all distances between genes in
				// fusedGroup (current cluster) and every gene in every
				// cluster contained in fusedGroup
				Arrays.sort(distances);
				if (linkMethod.contentEquals("Single Linkage")) {
					newRowVal = distances[0];

				} else if (linkMethod.contentEquals("Complete Linkage")) {
					newRowVal = distances[distances.length - 1];
				}

				newRow[i] = newRowVal;
			}
			// all elements in common
			else {
				newRowVal = 0.0;
				newRow[i] = newRowVal;

				// is there a third case?
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
	public double[] newRowGenAverage(final int[] fusedGroup) {

		final double[] newRow = new double[currentClusters.size()];

		for (int i = 0; i < currentClusters.size(); i++) {

			double distanceSum = 0;
			double newRowVal = 0;
			double distanceVal = 0;
			int selectedGene = 0;

			final int[] currentGroup = new int[currentClusters.get(i).size()];

			for (int z = 0; z < currentGroup.length; z++) {

				currentGroup[z] = currentClusters.get(i).get(z);
			}

			// check if fusedGroup contains the current checked gene
			// (then no mean should be calculated) no elements in common
			if (checkDisjoint(currentGroup, fusedGroup)) {

				// select members of the new clade (B & G)
				for (int j = 0; j < fusedGroup.length; j++) {

					selectedGene = fusedGroup[j];

					// take a row (gene) from the matrix which also appears
					// in the fusedGroup (current cluster).

					// halfDMatrix is getting mutated. Needs deepCopy?
					final double[] currentRow = distMatrixCopy[selectedGene];

					// go through all clusters and their contained genes
					// calculate the distance between each column (gene)
					// and the current row (gene) from fusedGroup
					// sum the distance values up
					for (final int gene : currentClusters.get(i)) {

						if (currentRow.length > gene) {
							distanceVal = currentRow[gene];

						} else {
							distanceVal = distMatrixCopy[gene][selectedGene];
						}
						distanceSum += distanceVal;
					}
				}

				// calculate the new value to be added to newClade
				// as the average of distances
				newRowVal = distanceSum
						/ (fusedGroup.length * currentGroup.length);

				newRow[i] = newRowVal;
			}
			// all elements in common
			else {
				newRowVal = 0.0;
				newRow[i] = newRowVal;
			}
		}

		return newRow;
	}

	/**
	 * Accessor for the reordered list
	 * 
	 * @return The reordered list of matrix elements after clustering. 
	 */
	public String[] getReorderedList() {

		return reorderedRows;
	}
}
