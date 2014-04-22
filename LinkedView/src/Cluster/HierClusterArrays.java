package Cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.model.TVModel;

/**
 * Test class to assess whether the operation can work on half a matrix (due to
 * symmetry)
 * 
 * @author CKeil
 * 
 */
public class HierClusterArrays {

	// Instance variables
	private final double PRECISION_LEVEL = 0.001;
	private final TVModel model;
	private final ClusterView clusterView;
	private String filePath;
	private final String type;
	private int wholeMSize;
	private int loopN;

	// Distance Matrix
	//private List<List<Double>> dMatrix = new ArrayList<List<Double>>();

	// Half of the Distance Matrix (symmetry)
	private double[][] halfDMatrix;
	
	// Half of the Distance Matrix (symmetry)
	private double[][] halfDMatrixCopy;

	// list to keep track of previously used minimum values in the dMatrix
	private double[] usedMins;

	// list to return ordered GENE numbers for .cdt creation
	private String[] reorderedList;
	
	private ClusterFileWriter2 bufferedWriter;

	private ArrayList<List<Integer>> geneGroups;
	private int[][] geneIntegerTable;
	
	// needed for connectNodes??? better way?
	private String[][] dataTable;

	/**
	 * Main constructor
	 * 
	 * @param model
	 * @param dMatrix
	 * @param pBar
	 * @param type
	 * @param method
	 */
	public HierClusterArrays(final DataModel model, final ClusterView clusterView, 
			final double[][] dMatrix, final String type) {

		this.model = (TVModel) model;
		this.clusterView = clusterView;
		this.halfDMatrix = dMatrix;
		this.type = type;
		this.wholeMSize = dMatrix.length;
	}

	// method for clustering the distance matrix
	public void cluster() {
		
		try {
			setupFileWriter();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// ProgressBar maximum
		String side = "";
		if(type.equalsIgnoreCase("GENE")) {
			side = "Row";
			
		} else if(type.equalsIgnoreCase("ARRY")){
			side = "Column";
			
		} else {
			side = "???";
		}
		clusterView.setLoadText("Clustering " + side + " Data...");
		clusterView.setPBarMax(wholeMSize);

		// data to be written to file
		dataTable = new String[wholeMSize][];

		// integer representation of genes for references
		// in calculations (list of fusedGroups)
		geneIntegerTable = new int[wholeMSize][];

		// Create array to check whether a min val was already used.
		// Fill with -1 values so 0 won't be mistaken as having been used.
		usedMins = new double[wholeMSize];
		
		for(int i = 0; i < usedMins.length; i++) {
			
			usedMins[i] = -1;
		}

		// halving of the distance matrix
		halfDMatrix = splitMatrix(halfDMatrix);
		
		// deep copy of distance matrix to avoid mutation
		// needed to access values during generation of new row after joining
		// two elements
		halfDMatrixCopy = deepCopy(halfDMatrix);

		// genes in integer representation to keep track
		// of the clades and cluster formation
		geneGroups = new ArrayList<List<Integer>>(wholeMSize);

		// fill list with integers corresponding to the genes
		// gets a bunch of "null" values added in the back?!    --- Investigate
		for (int i = 0; i < wholeMSize; i++) {

			final List<Integer> group = new ArrayList<Integer>(1);
			group.add(i);
			geneGroups.add(group);
		}

		// continue process until newDList has a size of 1,
		// which means that there is only 1 cluster
		// initially every gene is its own cluster
		int finalClusterN = 1;
		double time = System.currentTimeMillis();
		while (halfDMatrix.length > finalClusterN) {
			
			loopN = wholeMSize - halfDMatrix.length;
			
			// update ProgressBar
			clusterView.updatePBar(loopN);

			// local variables
			double min = 0;
			int row = 0;
			int column = 0;
			double geneMin = 0;

			// list to store the minimum value of each gene
			// at each while-loop iteration
			final double[] geneMinList = new double[halfDMatrix.length];

			// list to store the Strings which represent
			// calculated data (such as gene pairs)
			// to be added to dataTable
			int pairSize = 4;
			final String[] pair = new String[pairSize];

			// the row value is just the position of
			// the corresponding column value in columnValues
			final int[] colMinIndexList = new int[halfDMatrix.length];

			// going through every gene (row) in newDList
			// takes ~150ms
			for (int j = 0; j < halfDMatrix.length; j++) {
				
				// select current gene
				final double[] gene = halfDMatrix[j];

				// just avoid the first empty list in the half-distance matrix
				if (gene.length > 0) {
					// make trial the minimum value of that gene
					geneMin = findRowMin(gene);

					// minimums of each gene in distance matrix
					// does not contain previously used minimums
					geneMinList[j] = geneMin;

					// add the column of each gene's minimum to a list
					colMinIndexList[j] = findValue(gene, geneMin);
				}
				// for the first empty list in the half-distance matrix,
				// add the largest value of the last row so it
				// won't be mistaken as a minimum value
				else {
					// there's no actual value for the empty top row.
					// Therefore a substitute is added. It is 2x the max size
					// of the greatest value of the last distance matrix
					// entry so it can never be a minimum and
					// is effectively ignored
					double[] last = halfDMatrix[halfDMatrix.length - 1].clone();
					Arrays.sort(last);
					final double substitute = last[last.length - 1] * 2;

					geneMinList[j] = substitute;
					colMinIndexList[j] = findValue(last, last[last.length - 1]);
				}
			}

			// finds the row (gene) which has the smallest value
			double[] geneMinListCopy = geneMinList.clone();
			Arrays.sort(geneMinListCopy);
			row = findValue(geneMinList, geneMinListCopy[0]);
			
			// find the corresponding column using gene
			// with the minimum value (row)
			column = colMinIndexList[row];

			// row and column value of the minimum
			// distance value in matrix are now known
			min = halfDMatrix[row][column];

			// add used min value to record so the
			// next iterations finds the next higher min
			usedMins[loopN] = min;

			// the replacement row for the two removed row elements 
			// (when joining clusters)
			double[] newRow = new double[loopN];

			// get the two clusters to be fused
			final int[] rowGroup = new int[geneGroups.get(row).size()]; 
			for(int i = 0; i < rowGroup.length; i++) {
				
				rowGroup[i] = geneGroups.get(row).get(i);
			}
			
			final int[] colGroup = new int[geneGroups.get(column).size()];
			for(int i = 0; i < colGroup.length; i++) {
				
				colGroup[i] = geneGroups.get(column).get(i);
			}

			// the new geneGroup containing the all genes (BG)
			final int[] fusedGroup = concatArrays(rowGroup, colGroup);

			// The two connected clusters
			final String[] genePair = connectNodes(fusedGroup, rowGroup,
					colGroup, row, column);

			// Construct String list to add to dataTable (current cluster)
			pair[0] = "NODE" + (loopN + 1) + "X";
			pair[1] = genePair[0];
			pair[2] = genePair[1];
			pair[3] = String.valueOf(1 - min);
			
			bufferedWriter.writeContent(pair);

			// add note of new cluster to dataTable
			dataTable[loopN] = pair;

			// register clustering of the two elements
			geneIntegerTable[loopN] = fusedGroup;

			// Update gene list to reflect newly formed cluster
			
			// remove element with bigger list position first
			// to avoid list shifting issues
			if (row > column) {
				geneGroups.remove(row);
				geneGroups.remove(column);

			} else {
				geneGroups.remove(column);
				geneGroups.remove(row);
			}
			
			// These function are wrong for row/ col = 0
			int groupMin = findGroupMin(fusedGroup);
			boolean rowGHasMin = checkGroupForMin(groupMin, rowGroup);
			boolean colGHasMin = checkGroupForMin(groupMin, colGroup);
			
			List<Integer> newVals = new ArrayList<Integer>();
			
			for(int i = 0; i < fusedGroup.length; i++) {
				
				newVals.add(fusedGroup[i]);
			}
					
			int targetGGSize = geneGroups.size() + 1;
			if (rowGHasMin && row < targetGGSize) {
				geneGroups.add(row, newVals);

			} else if (colGHasMin && column < targetGGSize) {
				geneGroups.add(column, newVals);

			} else if((rowGHasMin && row == targetGGSize)
					|| (colGHasMin && column == targetGGSize)){
				geneGroups.add(newVals);
			
			} else {
				System.out.println("Problem adding fusedGroup to geneGroups.");
			} 
			
			// Update the distance matrix 

			// newRow contains corresponding values depending on the 
			// cluster method
			String linkMethod = clusterView.getLinkageMethod();
			
			if (linkMethod.contentEquals("Single Linkage")
					|| linkMethod.contentEquals("Complete Linkage")) {
				newRow = newRowGenSC(fusedGroup);
				
			} else if(linkMethod.contentEquals("Average Linkage")) {
				newRow = newRowGenAverage(fusedGroup);
			}

			// first: check whether the row or column contains the
			// smallest gene by index of both (fusedGroup)
			// then add a newClade value to each element where
			// newClade intersects (basically adding the column)
			if (rowGHasMin) {
				// replace element at row with newRow
				replaceRow(row, newRow);
				
				// remove the other row from halfDMatrix
				updateDM(column, row);

				for (int j = row; j < halfDMatrix.length; j++) {

					double[] element = halfDMatrix[j];
					// add to element at index 'row' if the element
					// is bigger than the row value otherwise the element
					// is too small to add a column value
					if (element.length > row) {
						element[row] = newRow[j];
					}

				}
			} else if (colGHasMin) {
				// replace element at row with newRow
				replaceRow(column, newRow);
				
				// remove the other row from halfDMatrix
				updateDM(row, column);

				for (int j = column; j < halfDMatrix.length; j++) {

					double[] element = halfDMatrix[j];
					
					if (element.length > column) {
						element[column] = newRow[j];
					}
				}
				
			} else {
				System.out.println("Weird error. Neither "
						+ "rowGroup nor colGroup have a minimum.");
			}
		}
		
		time = System.currentTimeMillis() - time;
		System.out.println("Total Cluster Arrays:" + time);
		
		bufferedWriter.closeWriter();
		reorderGen(geneGroups.get(0));
		
		// Ensure garbage collection for large objects
		halfDMatrixCopy = null;
		dataTable = null;
		geneGroups = null;
	}
	
	public void replaceRow(int repInd, double[] newRow) {
		
		double[][] newMatrix = new double[halfDMatrix.length][];
		
		double[] replacementRow = new double[repInd];
		System.arraycopy(newRow, 0, replacementRow, 0, repInd);
		
		for(int i = 0; i < newMatrix.length; i++) {
			
			if(i < repInd || i > repInd) {
				newMatrix[i] = halfDMatrix[i];
				
			} else if(i == repInd) {
				newMatrix[i] = replacementRow;
			}
		}
		
		halfDMatrix = newMatrix;
	}
	
	public void setupFileWriter() throws IOException {
		
		String fileEnd; 
		
		if (type.equalsIgnoreCase("GENE")) {
			fileEnd = ".gtr";

		} else {
			fileEnd = ".atr";
		}
		
		File file = new File(model.getSource().substring(0,
				model.getSource().length() - 4)
				+ fileEnd);

		file.createNewFile();

		bufferedWriter = new ClusterFileWriter2(file);

		filePath = bufferedWriter.getFilePath();
		
	}
	
	/**
	 * This method updates the old distance matrix by replacing the two target
	 * rows with the new row.
	 * @param row
	 * @param column
	 */
	public void updateDM(int removeIndex, int keepIndex) {
		
		double[][] newMatrix = new double[halfDMatrix.length - 1][];
		
		// this should shift the elements of halfDMatrix up by one 
		// once it reaches the index which should be removed.
		// A double of the last array in the element should remain at the end.
		for(int i = 0; i < newMatrix.length; i++) {
				
			if(i < removeIndex) {
				newMatrix[i] = halfDMatrix[i];
				
			} else if(i >= removeIndex) {
				double[] newElement = halfDMatrix[i + 1];
				newMatrix[i] = newElement;
			}
		}
		
		// this shrinks the current element after to a max size of '***Index'
		// after this the longest element in halfDMatrix has size '***Index'
		if (removeIndex > keepIndex) {
			for (int i = removeIndex; i < newMatrix.length; i++) {

				double[] element = newMatrix[i];
				newMatrix[i] = removeCol(element, removeIndex);
			}
		} else {
			for (int i = keepIndex; i < newMatrix.length; i++) {

				double[] element = newMatrix[i];
				newMatrix[i] = removeCol(element, keepIndex);
			}
		}

		halfDMatrix = newMatrix;
	}
	
	/**
	 * Removes one value from a double[] array by making a new 
	 * array without this value and with a length of array.length - 1.
	 * @param array
	 * @param toDelete
	 * @return
	 */
	public double[] removeCol(double[] array, int toDelete) {
		
		double[] newArray = new double[array.length - 1];
		
		for(int i = 0; i < newArray.length; i++) {
			
			if(i < toDelete) {
				newArray[i] = array[i];
				
			} else if(i >= toDelete) {
				newArray[i] = array[i+1];
			}
		}
		
		return newArray;
	}
	
	/**
	 * Finds the index of a value in a double array.
	 * @param array
	 * @param value
	 * @return
	 */
	public int findValue(double[] array, double value) {
	    
		for(int i = 0; i < array.length; i++) {
			
			if(Math.abs(array[i] - value) < PRECISION_LEVEL) {
				return i;
			}
		} 
		
		return -1;
	}
	
	/**
	 * Finds the index of a value in a double array.
	 * @param array
	 * @param value
	 * @return
	 */
	public int findArrayInDM(double[] array) {
	    
		for(int i = 0; i < array.length; i++) {
			
			if(!checkDisjoint(halfDMatrix[i], array)) {
				return i;
			}
		} 
		
		return -1;
	}
	
	/**
	 * Fuses two arrays together.
	 * @param a
	 * @param b
	 * @return
	 */
	public int[] concatArrays(int[] a, int[] b) {
		
		int[] c = new int[a.length + b.length];
		
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		
		return c;
	}

	/**
	 * Method to half the distance matrix. This should be done because a
	 * distance matrix is symmetrical and it saves computational time to do
	 * operations on only half of the values.
	 * 
	 * @param distanceMatrix
	 * @return half distanceMatrix
	 */
	public double[][] splitMatrix(final double[][] distanceMatrix) {

		double[][] halfMatrix = new double[wholeMSize][];
		
		// distance matrices are symmetrical!
		for (int i = 0; i < distanceMatrix.length; i++) {

			double[] newRow = Arrays.copyOfRange(distanceMatrix[i], 0, i);
			
			halfMatrix[i] = newRow;
		}

		return halfMatrix;
	}

	/**
	 * Method to make deep copy of distance matrix
	 * 
	 * @return
	 */
	public double[][] deepCopy(final double[][] distanceMatrix) {

		final double[][] deepCopy = new double[wholeMSize][];

		for (int i = 0; i < distanceMatrix.length; i++) {

			final double[] oldList = distanceMatrix[i];
			final double[] newList = new double[oldList.length];

//			for (int j = 0; j < oldList.length; j++) {
//				
//				newList[j] = oldList[j];
//			}
			
			System.arraycopy(oldList, 0, newList, 0, oldList.length);
			
			deepCopy[i] = newList;
		}

		return deepCopy;
	}
	
	/**
	 * make trial the minimum value of that gene check whether min was used
	 * before (in whole calculation process)
	 * 
	 * @return double trial
	 */
	public int findGroupMin(final int[] group) {

		int groupMin = -1;

		// standard collection copy constructor to make deep copy to protect
		// gene
		final int[] groupCopy = group.clone();
		
		Arrays.sort(groupCopy);
		
		int minIndex = 0;

		for (int i = 0; i < groupCopy.length; i++) {
			
			final int min = groupCopy[minIndex];

			if (!checkForUsedMin(min)) {
				groupMin = min;
				break;

			} else {
				minIndex++;
			}
		}

		return groupMin;
	}

	/**
	 * make trial the minimum value of that gene check whether min was used
	 * before (in whole calculation process)
	 * 
	 * @return double trial
	 */
	public double findRowMin(final double[] gene) {

		double geneMin = -1.0;

//		// standard collection copy constructor to make deep copy to protect
//		// gene
//		final double[] deepGene = gene.clone();
//		
//		Arrays.sort(deepGene);
//		
//		int minIndex = 0;
//
//		for (int i = 0; i < deepGene.length; i++) {
//			
//			final double min = deepGene[minIndex];
//
//			if (!checkForUsedMin(min)) {
//				geneMin = min;
//				break;
//
//			} else {
//				minIndex++;
//			}
//		}

		// standard collection copy constructor to make deep copy to protect
		// gene
		final List<Double> deepGene = new ArrayList<Double>();
		
		for(double element : gene) {
			
			deepGene.add(element);
		}

		for (int i = 0; i < deepGene.size(); i++) {

			final double min = Collections.min(deepGene);

			if (!checkForUsedMin(min)) {
				geneMin = min;
				break;

			} else {
				deepGene.remove(deepGene.indexOf(min));
			}
		}

		return geneMin;
	}
	
	/**
	 * Checks usedMins if it already contains a given double value.
	 * @param min
	 * @return
	 */
	public boolean checkGroupForMin(double min, int[] group) {
		
		boolean contains = false;
		
		for(int gene : group) {
			
			if(min == gene) {
				contains = true;
				break;
			}
		}
		
		return contains;
	}
	
	/**
	 * Checks usedMins if it already contains a given double value.
	 * @param min
	 * @return
	 */
	public boolean checkForUsedMin(double min) {
		
		boolean contains = false;
		
		for(int i = 0; i < usedMins.length; i++) {
			
			if(Math.abs(min - usedMins[i]) < PRECISION_LEVEL) {
				contains = true;
				break;
			}
		}
		
		return contains;
	}

	/**
	 * This method exists to determine the String name of the current gene pair
	 * and to check whether either of the two genes is already part of a
	 * previously formed cluster. In this case the remaining gene will be
	 * connected to that NODE.
	 * 
	 * @param fusedGroup
	 * @param rowGroup
	 * @param colGroup
	 * @param geneGroups
	 * @param row
	 * @param column
	 * @param geneIntegerTable
	 * @return
	 */
	public String[] connectNodes(final int[] fusedGroup,
			final int[]rowGroup, final int[] colGroup,
			final int row, final int column) {

		// make Strings for String list to be written to data file
		String geneRow = "";
		String geneCol = "";
		
		int groupSize = 2;

		final String[] dataList = new String[groupSize];

		// check the lists in dataMatrix whether the genePair you want
		// to add now is already in a list from before, if yes connect
		// to LATEST node by replacing the gene name with the node name
		if (fusedGroup.length == 2) {
			geneRow = type + geneGroups.get(row).get(0) + "X";
			geneCol = type + geneGroups.get(column).get(0) + "X";

		}
		// if size of fusedGroup exceeds 2...
		else {
			// move from top down to find the last fusedGroup
			// (history of clusters) containing any gene from current
			// colGroup so that the correct NODE-connection can be found
			for (int j = loopN - 1; j >= 0; j--) {

				// this currently gets the last node that has a common element
				// if the 2 groups have elements in common...
				if(geneIntegerTable[j] != null) {
					if (!checkDisjoint(geneIntegerTable[j], colGroup)) {
	
						// assigns NODE # of last fusedGroup containing
						// a colGroup element
						geneCol = dataTable[j][0];
						break;
					}
					// if the current fusedGroup in geneIntegerTable
					// does not have any elements in common
					// with geneGroups.get(column)
					else {
						geneCol = type + geneGroups.get(column).get(0) + "X";
					}
				}
			}

			// move from top down to find the last fusedGroup
			// (history of clusters) containing any gene from
			// current rowGroup so that the correct NODE-connection can be found
			for (int j = loopN - 1; j >= 0; j--) {

				// this currently gets the last node that has a common element
				// if the 2 groups have elements in common...
				if(geneIntegerTable[j] != null) {
					if (!checkDisjoint(geneIntegerTable[j], rowGroup)) {
	
						// assigns NODE # of last fusedGroup containing
						// a rowGroup element
						geneRow = dataTable[j][0];
						break;
	
					} else {
						// random fix to see what happens
						geneRow = type + geneGroups.get(row).get(0) + "X";
					}
				}
			}
		}

		
		// Check if only one of the two String has a "NODE" component.
		// If yes, position it at [1].	
		if(!geneCol.contains("NODE") && geneRow.contains("NODE")) {
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
	 * @param a
	 * @param b
	 * @return
	 */
	public boolean checkDisjoint(int[] a, int[] b) {
		
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
		       
		    	if(toIterate[i] == toSearch[j]){
		            disjoint = false;
		        }
		    }
		}
		
		return disjoint;
	}
	
	/**
	 * Checks if two double[] have ALL elements in common.
	 * @param a
	 * @param b
	 * @return
	 */
	public boolean checkDisjoint(double[] a, double[] b) {
		
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
		       
		    	if(Math.abs(toIterate[i] - toSearch[j]) < PRECISION_LEVEL){
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
	 * @param finalCluster
	 */
	public void reorderGen(final List<Integer> finalCluster) {

		String element = "";
		
		reorderedList = new String[finalCluster.size()];

		for (int i = 0; i < finalCluster.size(); i++) {

			element = type + finalCluster.get(i) + "X";
			reorderedList[i] = element;
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
	 * @param geneGroups
	 * @param newRow
	 * @return newClade
	 */
	public double[] newRowGenSC(final int[] fusedGroup) {

		String linkMethod = clusterView.getLinkageMethod();
		double[] newRow = new double[geneGroups.size()];
		
		for (int i = 0; i < geneGroups.size(); i++) {

			double newRowVal = 0;
			double distanceVal = 0;
			int selectedGene = 0;
			
			int[] currentGroup = new int[geneGroups.get(i).size()];
			
			for(int z = 0; z < currentGroup.length; z++) {
				
				currentGroup[z] = geneGroups.get(i).get(z);
			}

			// check if fusedGroup contains the current checked gene
			// (then no mean should be calculated) no elements in common
			if (checkDisjoint(currentGroup, fusedGroup)) {
				final double[] distances = new double[fusedGroup.length 
				                                      * currentGroup.length];

				int dInd = 0;
				for (int j = 0; j < fusedGroup.length; j++) {

					selectedGene = fusedGroup[j];

					// use halfDMatrix instead and just reverse the index
					// access if a list is too short for the requested
					// index since the distance matrix is symmetrical
					
					// halfDMatrix is getting mutated. Needs deepCopy? 			
					final double[] currentRow = halfDMatrixCopy[selectedGene];

					// go through all clusters and their contained genes
					// finds the distances between a gene in geneGroup 
					// (remaining non-clustered) and all genes in 
					// fusedGroup (current gene cluster)
					for (int k = 0; k < currentGroup.length; k++) {

						// if-else to allow for use of halfDMatrix 
						if(currentRow.length > currentGroup[k]) { 
							distanceVal = currentRow[currentGroup[k]];
						
						} else {
							// reverse index access because of distance
							// matrix symmetry
							distanceVal = halfDMatrixCopy[currentGroup[k]]
									[selectedGene];
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
					
				} else if(linkMethod.contentEquals("Complete Linkage")){
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
	 * @param geneGroups
	 * @param newRow
	 * @return newClade
	 */
	public double[] newRowGenAverage(final int[] fusedGroup) {

		double[] newRow = new double[geneGroups.size()];
		
		for (int i = 0; i < geneGroups.size(); i++) {

			double distanceSum = 0;
			double newRowVal = 0;
			double distanceVal = 0;
			int selectedGene = 0;
			
			int[] currentGroup = new int[geneGroups.get(i).size()];
			
			for(int z = 0; z < currentGroup.length; z++) {
				
				currentGroup[z] = geneGroups.get(i).get(z);
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
					final double[] currentRow = halfDMatrixCopy[selectedGene];

					// go through all clusters and their contained genes
					// calculate the distance between each column (gene) 
					// and the current row (gene) from fusedGroup
					// sum the distance values up
					for (final int gene : geneGroups.get(i)) {

						if(currentRow.length > gene) {
							distanceVal = currentRow[gene];
						
						} else {
							distanceVal = halfDMatrixCopy[gene][selectedGene];
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
			else  {
				newRowVal = 0.0;
				newRow[i] = newRowVal;
			} 
		}

		return newRow;
	}
	
	
	/**
	 * Accessor for the reordered list
	 * 
	 * @return
	 */
	public String[] getReorderedList() {

		return reorderedList;
	}

	/**
	 * Accessor for the file path
	 * 
	 * @return
	 */
	public String getFilePath() {

		return filePath;
	}
}
