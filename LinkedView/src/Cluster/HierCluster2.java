package Cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
public class HierCluster2 {

	// Instance variables
	private final TVModel model;
	private final ClusterView clusterView;
	private String filePath;
	private final String type;
	private int wholeMSize;

	// Distance Matrix
	//private List<List<Double>> dMatrix = new ArrayList<List<Double>>();

	// Half of the Distance Matrix (symmetry)
	private List<List<Double>> halfDMatrix;
	
	// Half of the Distance Matrix (symmetry)
	private List<List<Double>> halfDMatrixCopy;

	// list to keep track of previously used minimum values in the dMatrix
	private List<Double> usedMins;

	// list to return ordered GENE numbers for .cdt creation
	private final List<String> reorderedList = new ArrayList<String>();
	
	private ClusterFileWriter2 bufferedWriter;

	private List<List<Integer>> geneGroups;
	private List<List<Integer>> geneIntegerTable;
	
	// needed for connectNodes??? better way?
	private List<List<String>> dataTable;

	/**
	 * Main constructor
	 * 
	 * @param model
	 * @param dMatrix
	 * @param pBar
	 * @param type
	 * @param method
	 */
	public HierCluster2(final DataModel model, final ClusterView clusterView, 
			final List<List<Double>> dMatrix, final String type) {

		this.model = (TVModel) model;
		this.clusterView = clusterView;
		this.halfDMatrix = dMatrix;
		this.type = type;
	}

	// method for clustering the distance matrix
	public void cluster() {
		
		try {
			setupFileWriter();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		wholeMSize = halfDMatrix.size();
		
		// ProgressBar maximum
		clusterView.setLoadText("Clustering data...");
		clusterView.setPBarMax(wholeMSize);

		// data to be written to file
		dataTable = new ArrayList<List<String>>();

		// integer representation of genes for references
		// in calculations (list of fusedGroups)
		geneIntegerTable = new ArrayList<List<Integer>>();

		usedMins = new ArrayList<Double>();

		// halving of the distance matrix
		halfDMatrix = splitMatrix(halfDMatrix);
		
		// deep copy of distance matrix to avoid mutation
		// needed to access values during generation of new row after joining
		// two elements
		halfDMatrixCopy = deepCopy(halfDMatrix);

		// genes in integer representation to keep track
		// of the clades and cluster formation
		geneGroups = new ArrayList<List<Integer>>();

		// fill list with integers corresponding to the genes
		// gets a bunch of "null" values added in the back?!    --- Investigate
		for (int i = 0; i < halfDMatrix.size(); i++) {

			final List<Integer> group = new ArrayList<Integer>();
			group.add(i);
			geneGroups.add(group);
		}

		// continue process until newDList has a size of 1,
		// which means that there is only 1 cluster
		// initially every gene is its own cluster
		int finalClusterN = 1;
		while (halfDMatrix.size() > finalClusterN) {

			double time = System.currentTimeMillis();
			// update ProgressBar
			clusterView.updatePBar(wholeMSize - halfDMatrix.size());

			// local variables
			double min = 0;
			int row = 0;
			int column = 0;
			double geneMin = 0;

			// list to store the minimum value of each gene
			// at each while-loop iteration
			final List<Double> geneMinList = new ArrayList<Double>();

			// list to store the Strings which represent
			// calculated data (such as gene pairs)
			// to be added to dataTable
			final List<String> pair = new ArrayList<String>();

			// the row value is just the position of
			// the corresponding column value in columnValues
			final List<Integer> colMinIndexList = new ArrayList<Integer>();

			// going through every gene (row) in newDList
			// takes ~150ms
			for (int j = 0; j < halfDMatrix.size(); j++) {

				// select current gene
				final List<Double> gene = halfDMatrix.get(j);

				// just avoid the first empty list in the half-distance matrix
				if (gene.size() > 0) {

					// make trial the minimum value of that gene
					geneMin = findRowMin(gene);

					// minimums of each gene in distance matrix
					// does not contain previously used minimums
					geneMinList.add(geneMin);

					// add the column of each gene's minimum to a list
					colMinIndexList.add(gene.indexOf(geneMin));
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
					final int last = halfDMatrix.size() - 1;
					final double substitute = Collections.max(halfDMatrix
							.get(last)) * 2;

					geneMinList.add(substitute);
					colMinIndexList.add(halfDMatrix.get(last).indexOf(
							Collections.max(halfDMatrix.get(last))));
				}
			}

			// finds the row (gene) which has the smallest value
			row = geneMinList.indexOf(Collections.min(geneMinList));

			// find the corresponding column using gene
			// with the minimum value (row)
			column = colMinIndexList.get(row);

			// row and column value of the minimum
			// distance value in matrix are now known
			min = halfDMatrix.get(row).get(column);

			// add used min value to record so the
			// next iterations finds the next higher min
			usedMins.add(min);

			// the replacement row for the two removed row elements 
			// (when joining clusters)
			List<Double> newRow = new ArrayList<Double>();

			// get the two clusters to be fused
			final List<Integer> rowGroup = geneGroups.get(row);
			final List<Integer> colGroup = geneGroups.get(column);

			// the new geneGroup containing the all genes (BG)
			final List<Integer> fusedGroup = new ArrayList<Integer>();
			fusedGroup.addAll(rowGroup);
			fusedGroup.addAll(colGroup);

			// The two connected clusters
			final List<String> genePair = connectNodes(fusedGroup, rowGroup,
					colGroup, row, column);

			// Construct String list to add to dataTable (current cluster)
			pair.add("NODE" + (wholeMSize - halfDMatrix.size()) + "X");
			pair.add(genePair.get(0));
			pair.add(genePair.get(1));
			pair.add(String.valueOf(1 - min));
			
			bufferedWriter.writeContent(pair);

			// add note of new cluster to dataTable
			dataTable.add(pair);

			// register clustering of the two elements
			geneIntegerTable.add(fusedGroup);

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

			if (rowGroup.contains(Collections.min(fusedGroup))) {
				geneGroups.add(row, fusedGroup);

			} else if (colGroup.contains(Collections.min(fusedGroup))) {
				geneGroups.add(column, fusedGroup);

			} else {
				System.out.println("Adding fusedGroup to geneGroups failed.");
			}
			
			// Update the distance matrix 

			// remove old elements first;
			if (column > row) {
				halfDMatrix.remove(column);
				halfDMatrix.remove(row);

				for (final List<Double> element : halfDMatrix) {

					if (element.size() > column) {
						element.remove(column);
					}
				}
			} else {
				halfDMatrix.remove(row);
				halfDMatrix.remove(column);

				// something more efficient possible??				--- Investigate
				for (final List<Double> element : halfDMatrix) {

					if (element.size() > row) {
						element.remove(row);
					}
				}
			}

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
			if (rowGroup.contains(Collections.min(fusedGroup))) {
				halfDMatrix.add(row, newRow.subList(0, row));

				for (final List<Double> element : halfDMatrix) {

					// add to element at index 'row' if the element
					// is bigger than the row value otherwise the element
					// is too small to add a column value
					if (element.size() > row) {
						element.set(row,
								newRow.get(halfDMatrix.indexOf(element)));
					}

				}
			} else if (colGroup.contains(Collections.min(fusedGroup))) {
				halfDMatrix.add(column, newRow.subList(0, column));

				for (final List<Double> element : halfDMatrix) {

					// value at column index of element replaced with 
					// value from newRow at index of the current element's
					// index in halfDMatrix
					if (element.size() > column) {
						element.set(column,
								newRow.get(halfDMatrix.indexOf(element)));
					}

				}
			} else {
				System.out.println("Weird error. Neither "
						+ "rowGroup nor colGroup have a minimum.");
			}
			
			time = System.currentTimeMillis() - time;
			System.out.println("Loop time Lists:" + time);
		}
		
		bufferedWriter.closeWriter();
		reorderGen(geneGroups.get(0));
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
	 * Method to half the distance matrix. This should be done because a
	 * distance matrix is symmetrical and it saves computational time to do
	 * operations on only half of the values.
	 * 
	 * @param distanceMatrix
	 * @return half distanceMatrix
	 */
	public List<List<Double>> splitMatrix(
			final List<List<Double>> distanceMatrix) {

		// distance matrices are symmetrical!
		for (int i = 0; i < distanceMatrix.size(); i++) {

			final int stop = distanceMatrix.get(i).size();

			distanceMatrix.get(i).subList(i, stop).clear();
		}

		return distanceMatrix;
	}

	/**
	 * Method to make deep copy of distance matrix
	 * 
	 * @return
	 */
	public List<List<Double>> deepCopy(final List<List<Double>> distanceMatrix) {

		final List<List<Double>> deepCopy = new ArrayList<List<Double>>();

		for (int i = 0; i < distanceMatrix.size(); i++) {

			final List<Double> oldList = distanceMatrix.get(i);
			final List<Double> newList = new ArrayList<Double>();

			for (int j = 0; j < oldList.size(); j++) {

				final Double e = new Double(oldList.get(j));
				newList.add(e);
			}
			
			deepCopy.add(newList);
		}

		return deepCopy;
	}

	/**
	 * make trial the minimum value of that gene check whether min was used
	 * before (in whole calculation process)
	 * 
	 * @return double trial
	 */
	public double findRowMin(final List<Double> gene) {

		double geneMin = 0.0;

		// standard collection copy constructor to make deep copy to protect
		// gene
		final List<Double> deepGene = new ArrayList<Double>(gene);

		for (int i = 0; i < deepGene.size(); i++) {

			final double min = Collections.min(deepGene);

			if (!usedMins.contains(min)) {
				geneMin = min;
				break;

			} else {
				deepGene.remove(deepGene.indexOf(min));
			}
		}

		return geneMin;
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
	public List<String> connectNodes(final List<Integer> fusedGroup,
			final List<Integer> rowGroup, final List<Integer> colGroup,
			final int row, final int column) {

		// make Strings for String list to be written to data file
		String geneRow = "";
		String geneCol = "";

		final List<String> dataList = new ArrayList<String>();

		// check the lists in dataMatrix whether the genePair you want
		// to add now is already in a list from before, if yes connect
		// to LATEST node by replacing the gene name with the node name
		if (fusedGroup.size() == 2) {
			geneRow = type + geneGroups.get(row).get(0) + "X";
			geneCol = type + geneGroups.get(column).get(0) + "X";

		}
		// if size of fusedGroup exceeds 2...
		else {
			// move from top down to find the last fusedGroup
			// (history of clusters) containing any gene from current
			// colGroup so that the correct NODE-connection can be found
			for (int j = geneIntegerTable.size() - 1; j >= 0; j--) {

				// this currently gets the last node that has a common element
				// if the 2 groups have elements in common...
				if (!Collections.disjoint(geneIntegerTable.get(j), colGroup)) {
					final List<Integer> intersect = new ArrayList<Integer>(
							geneIntegerTable.get(j));
					intersect.retainAll(colGroup);

					// assigns NODE # of last fusedGroup containing
					// a colGroup element
					geneCol = dataTable.get(j).get(0);
					break;

				}
				// if the current fusedGroup in geneIntegerTable
				// does not have any elements in common
				// with geneGroups.get(column)
				else {
					geneCol = type + geneGroups.get(column).get(0) + "X";
				}
			}

			// move from top down to find the last fusedGroup
			// (history of clusters) containing any gene from
			// current rowGroup so that the correct NODE-connection can be found
			for (int j = geneIntegerTable.size() - 1; j >= 0; j--) {

				// this currently gets the last node that has a common element
				// if the 2 groups have elements in common...
				if (!Collections.disjoint(geneIntegerTable.get(j), rowGroup)) {
					final List<Integer> intersect = new ArrayList<Integer>(
							geneIntegerTable.get(j));
					intersect.retainAll(rowGroup);

					// assigns NODE # of last fusedGroup containing
					// a rowGroup element
					geneRow = dataTable.get(j).get(0);
					break;

				} else {
					// random fix to see what happens
					geneRow = type + geneGroups.get(row).get(0) + "X";
				}
			}
		}

		dataList.add(geneCol);
		dataList.add(geneRow);

		return dataList;
	}

	public void reorderGen(final List<Integer> finalCluster) {

		String element = "";

		for (final int e : finalCluster) {

			element = type + e + "X";
			reorderedList.add(element);
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
	public List<Double> newRowGenSC(final List<Integer> fusedGroup) {

		String linkMethod = clusterView.getLinkageMethod();
		List<Double> newRow = new ArrayList<Double>();
		
		for (int i = 0; i < geneGroups.size(); i++) {
			
			if(i == 26)
			{
				System.out.println("Bug");
			}

			double newRowVal = 0;
			double distanceVal = 0;
			int selectedGene = 0;

			// check if fusedGroup contains the current checked gene
			// (then no mean should be calculated) no elements in common
			if (Collections.disjoint(geneGroups.get(i), fusedGroup)) {
			
				final List<Double> distances = new ArrayList<Double>();

				for (int j = 0; j < fusedGroup.size(); j++) {

					selectedGene = fusedGroup.get(j);

					// use halfDMatrix instead and just reverse the index
					// access if a list is too short for the requested
					// index since the distance matrix is symmetrical
					
					// halfDMatrix is getting mutated. Needs deepCopy? 			---Investigate
					
					final List<Double> currentRow = halfDMatrixCopy
							.get(selectedGene);

					// go through all clusters and their contained genes
					// finds the distances between a gene in geneGroup 
					// (remaining non-clustered) and all genes in 
					// fusedGroup (current gene cluster)
					for (final int gene : geneGroups.get(i)) {

						// if-else to allow for use of halfDMatrix and
						// discard the full matrix when halfDMatrix is 
						// created
						if(currentRow.size() > gene) { 
							distanceVal = currentRow.get(gene);
						
						} else {
							
							// reverse index access because of distance
							// matrix symmetry
							distanceVal = halfDMatrixCopy.get(gene)
									.get(selectedGene);
						}
						
						if(distanceVal != 1.0 )
						{
							System.out.println("Bug");
						}
						
						distances.add(distanceVal);
					}
				}
				
				// result is a list of all distances between genes in 
				// fusedGroup (current cluster) and every gene in every 
				// cluster contained in fusedGroup

				if (linkMethod.contentEquals("Single Linkage")) {
					newRowVal = Collections.min(distances);
					
				} else if(linkMethod.contentEquals("Complete Linkage")){
					newRowVal = Collections.max(distances);
				}
			
				newRow.add(newRowVal);
			}
			// all elements in common
			else if (geneGroups.get(i).containsAll(fusedGroup)) {
				newRowVal = 0.0;
				newRow.add(newRowVal);
			
			// is there a third case?
			} else {
	
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
	public List<Double> newRowGenAverage(final List<Integer> fusedGroup) {

		List<Double> newRow = new ArrayList<Double>();
		
		for (int i = 0; i < geneGroups.size(); i++) {

			double distanceSum = 0;
			double newRowVal = 0;
			double distanceVal = 0;
			int selectedGene = 0;

			// check if fusedGroup contains the current checked gene
			// (then no mean should be calculated) no elements in common
			if (Collections.disjoint(geneGroups.get(i), fusedGroup)) {

				// select members of the new clade (B & G)
				for (int j = 0; j < fusedGroup.size(); j++) {

					selectedGene = fusedGroup.get(j);
					
					// take a row (gene) from the matrix which also appears
					// in the fusedGroup (current cluster).
					
					// halfDMatrix is getting mutated. Needs deepCopy? 			---Investigate
					final List<Double> currentRow = halfDMatrixCopy
							.get(selectedGene);

					// go through all clusters and their contained genes
					// calculate the distance between each column (gene) 
					// and the current row (gene) from fusedGroup
					// sum the distance values up
					for (final int gene : geneGroups.get(i)) {

						if(currentRow.size() > gene) {
							distanceVal = currentRow.get(gene);
						
						} else {
							distanceVal = halfDMatrixCopy.get(gene)
									.get(selectedGene);
						}
						distanceSum += distanceVal;
					}
				}

				// calculate the new value to be added to newClade
				// as the average of distances
				newRowVal = distanceSum
						/ (fusedGroup.size() * geneGroups.get(i).size());
				
				newRow.add(newRowVal);
			}
			// all elements in common
			else if (geneGroups.get(i).containsAll(fusedGroup)) {
				newRowVal = 0.0;
				newRow.add(newRowVal);
			} else {

			}
		}

		return newRow;
	}
	
	
	/**
	 * Accessor for the reordered list
	 * 
	 * @return
	 */
	public List<String> getReorderedList() {

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
