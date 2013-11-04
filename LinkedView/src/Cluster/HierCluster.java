package Cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JProgressBar;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.model.TVModel;

/**
 * Test class to assess whether the operation can work on 
 * half a matrix (due to symmetry) 
 * @author CKeil
 *
 */
public class HierCluster {

	//Instance variables
	private TVModel model;
	private String filePath;
	private String type;
	private String method;
	private JProgressBar pBar;
	
	//Distance Matrix
	private List<List<Double>> dMatrix = new ArrayList<List<Double>>();
	
	//Half of the Distance Matrix (symmetry)
	private List<List<Double>> halfDMatrix;
	
	//list to keep track of previously used minimum values in the dMatrix
	private List<Double> usedMins;
	
	//list to return ordered GENE numbers for .cdt creation
	private List<String> reorderedList = new ArrayList<String>();
	
	private List<List<Integer>> geneGroups;
	private List<List<Integer>> geneIntegerTable;
	private List<List<String>> dataTable;
	
	/**
	 * Main constructor
	 * @param model
	 * @param dMatrix
	 * @param pBar
	 * @param type
	 * @param method
	 */
	public HierCluster(DataModel model, List<List<Double>> dMatrix, 
			JProgressBar pBar, String type, String method) {
		
		this.model = (TVModel)model;
		this.dMatrix = dMatrix;
		this.type = type;
		this.method = method;
		this.pBar = pBar;
	}
	
	//method for clustering the distance matrix
    public void cluster() {
    	
    	//ProgressBar maximum
    	pBar.setMaximum(dMatrix.size());
    	
    	//data to be written to file
    	dataTable = new ArrayList<List<String>>();
    	
    	//integer representation of genes for references 
    	//in calculations (list of fusedGroups)
    	geneIntegerTable = new ArrayList<List<Integer>>();
    	
		usedMins = new ArrayList<Double>();
		
		//deep copy of distance matrix to avoid mutation
		halfDMatrix = deepCopy(dMatrix);
		
		//halving of the distance matrix
		halfDMatrix = splitMatrix(halfDMatrix);
		
		//genes in integer representation to keep track 
		//of the clades and cluster formation
		geneGroups = new ArrayList<List<Integer>>();
		
		//fill list with integers corresponding to the genes
		for(int i = 0; i < halfDMatrix.size(); i++){
			
			List<Integer> group = new ArrayList<Integer>();
			group.add(i);
			geneGroups.add(group);
		}
		
		//continue process until newDList has a size of 1, 
		//which means that there is only 1 cluster
		//initially every gene is its own cluster
		while(halfDMatrix.size() > 1) {

    		//update ProgressBar
    		pBar.setValue(dMatrix.size() - halfDMatrix.size());
    		
    		//local variables
    		double min = 0;
    		int row = 0;
    		int column = 0;
    		double geneMin = 0;
    		
    		//list to store the minimum value of each gene 
    		//at each while-loop iteration
    		List<Double> geneMinList = new ArrayList<Double>();
    		
    		//list to store the Strings which represent 
    		//calculated data (such as gene pairs)
    		//to be added to dataTable
    		List<String> pair = new ArrayList<String>();

    		//the row value is just the position of 
    		//the corresponding column value in columnValues
    		List<Integer> colMinIndexList = new ArrayList<Integer>();
    		
    		//going through every gene (row) in newDList
    		//takes ~150ms
    		for(int j = 0; j < halfDMatrix.size(); j++) {
    			
    			//select current gene
    			List<Double> gene = halfDMatrix.get(j);
    			
    			//just avoid the first empty list in the half-distance matrix
    			if(gene.size() > 0) {
    				
	    			//make trial the minimum value of that gene
	    			geneMin = findRowMin(gene);
	    			
	    			//minimums of each gene in distance matrix
	    			//does not contain previously used minimums 
	    			geneMinList.add(geneMin);
	    			
	    			//add the column of each gene's minimum to a list
	    			colMinIndexList.add(gene.indexOf(geneMin));
    			
    			}
    			//for the first empty list in the half-distance matrix, 
    			//add the largest value of the last row so it 
    			//won't be mistaken as a minimum value
    			else {
    				
    				//there's no actual value for the empty top row. 
    				//Therefore a substitute is added. It is 2x the max size 
    				//of the greatest value of the last distance matrix 
    				//entry so it an never be a minimum and 
    				//is effectively ignored
    				int last = halfDMatrix.size() - 1;
    				double substitute = Collections.max(
    						halfDMatrix.get(last)) * 2;
    				
    				geneMinList.add(substitute);
    				colMinIndexList.add(halfDMatrix.get(last).indexOf(
    						Collections.max(halfDMatrix.get(last))));
    			}
    		}
    		
    		//finds the row (gene) which has the smallest value
    		row = geneMinList.indexOf(Collections.min(geneMinList));
    		
    		//find the corresponding column using gene 
    		//with the minimum value (row)
    		column = colMinIndexList.get(row);
			
    		//row and column value of the minimum 
    		//distance value in matrix are now known
    		min = halfDMatrix.get(row).get(column);
    		
    		//add used min value to record so the 
    		//next iterations finds the next higher min
    		usedMins.add(min);
			
    		//the replacement list for the two removed lists (joint clusters)
    		List<Double> newClade = new ArrayList<Double>();
    		
    		//get the two clusters to be fused 
    		List<Integer> rowGroup = geneGroups.get(row); 
    		List<Integer> colGroup = geneGroups.get(column); 
    		
    		//the new geneGroup containing the all genes (BG)
    		List<Integer> fusedGroup = new ArrayList<Integer>();
    		fusedGroup.addAll(rowGroup);
    		fusedGroup.addAll(colGroup); 
  
    		//The two connected clusters
    		List<String> genePair = connectNodes(
    				fusedGroup, rowGroup, colGroup, row, column);
			
			//Construct String list to add to dataTable (current cluster)
			pair.add("NODE" + (dMatrix.size() - halfDMatrix.size() + 1) + "X");
    		pair.add(genePair.get(0));
    		pair.add(genePair.get(1));
    		pair.add(String.valueOf(1 - min));
    		
    		//add note of new cluster to dataTable
    		dataTable.add(pair);
    		
    		//register clustering of the two elements
    		geneIntegerTable.add(fusedGroup);
    		
    		//remove element with bigger list position first 
    		//to avoid list shifting issues
    		if(row > column) {
    			geneGroups.remove(row);
	    		geneGroups.remove(column);
	    		
    		} else {
    			geneGroups.remove(column);
    			geneGroups.remove(row);
    		}

    		
			if(rowGroup.contains(Collections.min(fusedGroup))) {
				geneGroups.add(row, fusedGroup);
				
			} else if(colGroup.contains(Collections.min(fusedGroup))) {
				geneGroups.add(column, fusedGroup);
				
			} else {
				System.out.println("Adding fusedGroup to geneGroups failed.");
			}
			
    		//remove old elements first;
			if(column > row) {
				halfDMatrix.remove(column);
				halfDMatrix.remove(row);
				
	    		for(List<Double> element : halfDMatrix) {
	    			
	    			if(element.size() > column) {
	    				element.remove(column);
	    			}
	    		}
			} else {
				halfDMatrix.remove(row);
				halfDMatrix.remove(column);
				
	    		for(List<Double> element : halfDMatrix) {
	    			
	    			if(element.size() > row) {
	    				element.remove(row);
	    			}
	    		}	
			}
			
			//newClade is new row with corresponding values 
			//depending on the cluster method
			newClade = newCladeGen(fusedGroup, newClade);
    		
    		//first: check whether the row or column contains the 
			//smallest gene by index of both (fusedGroup)
    		//then add a newClade value to each element where 
			//newClade intersects (basically adding the column)
			if(rowGroup.contains(Collections.min(fusedGroup))) {
	    		halfDMatrix.add(row, newClade.subList(0, row));
	    		
	    		for(List<Double> element : halfDMatrix) {
	    			
	    			//add to element at index 'row' if the element 
	    			//is bigger than the row value otherwise the element 
	    			//is too small to add a column value
	    			if(element.size() > row) {
	    				element.set(row, newClade.get(
	    						halfDMatrix.indexOf(element)));
	    			}
	    			
	    		}
			} else if(colGroup.contains(Collections.min(fusedGroup))) {
				halfDMatrix.add(column, newClade.subList(0, column));
				
	    		for(List<Double> element : halfDMatrix) {
	    			
	    			if(element.size() > column) {
	    				element.set(column, newClade.get(
	    						halfDMatrix.indexOf(element))); 
	    			}
	    			
	    		}
			} else {
				System.out.println("Weird error. Neither " +
						"rowGroup nor colGroup have a minimum.");
			}
		}
    	
		reorderGen(geneGroups.get(0));
		
    	ClusterFileWriter dataFile = new ClusterFileWriter(model);
    	
    	//generate files for Dendrogram
    	if(type.equalsIgnoreCase("GENE")) {
    		dataFile.writeFile(dataTable, ".gtr");
    		
    	} else {
    		dataFile.writeFile(dataTable, ".atr");
    	}
    	
		filePath = dataFile.getFilePath();
    }
    
    /**
     * Method to half the distance matrix. This should be done because a 
     * distance matrix is symmetrical and it saves computational time 
     * to do operations on only half of the values.
     * @param distanceMatrix
     * @return half distanceMatrix
     */
    public List<List<Double>> splitMatrix(List<List<Double>> distanceMatrix) {
    	
    	//distance matrices are symmetrical!
    	for(int i = 0; i < distanceMatrix.size(); i++) {

    		int stop = distanceMatrix.get(i).size();
    		
    		distanceMatrix.get(i).subList(i, stop).clear();
    	}
    	
    	return distanceMatrix;
    }
    
    /**
     * Method to make deep copy of distance matrix
     * @return
     */
    public List<List<Double>> deepCopy(List<List<Double>> distanceMatrix) {
    	
    	List<List<Double>> deepCopy = new ArrayList<List<Double>>();
    	
		for(int i = 0; i < distanceMatrix.size(); i++) {
			
			List<Double> newList = new ArrayList<Double>();
			deepCopy.add(newList);
			
			for(int j = 0; j < distanceMatrix.size(); j++) {
				
				Double e = new Double(distanceMatrix.get(i).get(j));
				newList.add(e);
			}
		}
		
		return deepCopy;
    }
    
	/**
	 * make trial the minimum value of that gene
	 * check whether min was used before (in whole calculation process)
	 * @return double trial
	 */
    public double findRowMin(List<Double> gene) {
    	
		double geneMin = 0.0;
		
		//standard collection copy constructor to make deep copy to protect gene
		List<Double> deepGene = new ArrayList<Double>(gene);
		
		for(int i = 0; i < deepGene.size(); i++) {
			
			double min = Collections.min(deepGene);
			
			if(!usedMins.contains(min)) {
				geneMin = min;
				break;
				
			} else {
				deepGene.remove(deepGene.indexOf(min));
			}
		}
		
		return geneMin;
    }
    
    /**
     * This method exists to determine the String name of the 
     * current gene pair and to check whether either of the two genes is 
     * already part of a previously formed cluster. In this case the
     * remaining gene will be connected to that NODE.
     * @param fusedGroup
     * @param rowGroup
     * @param colGroup
     * @param geneGroups
     * @param row
     * @param column
     * @param geneIntegerTable
     * @return
     */
    public List<String> connectNodes(List<Integer> fusedGroup, 
    		List<Integer> rowGroup, List<Integer> colGroup, 
    		int row, int column) {
    	
		//make Strings for String list to be written to data file
		String geneRow = "";
		String geneCol = "";
		
		List<String> dataList = new ArrayList<String>();
		
		//check the lists in dataMatrix whether the genePair you want 
		//to add now is already in a list from before, if yes connect 
		//to LATEST node by replacing the gene name with the node name	
		if(fusedGroup.size() == 2) {
			geneRow = type + geneGroups.get(row).get(0) + "X"; 
			geneCol = type + geneGroups.get(column).get(0) + "X";
			
		}
		//if size of fusedGroup exceeds 2...
		else{
			//move from top down to find the last fusedGroup 
			//(history of clusters) containing any gene from current 
			//colGroup so that the correct NODE-connection can be found
			for(int j = geneIntegerTable.size() - 1; j >= 0; j--) {
				
				//this currently gets the last node that has a common element
				//if the 2 groups have elements in common...
				if(!Collections.disjoint(geneIntegerTable.get(j), colGroup)) {
					List<Integer> intersect = 
							new ArrayList<Integer>(geneIntegerTable.get(j));
					intersect.retainAll(colGroup);
					
					//assigns NODE # of last fusedGroup containing 
					//a colGroup element
					geneCol = dataTable.get(j).get(0);
					break;
					
				}
				//if the current fusedGroup in geneIntegerTable 
				//does not have any elements in common 
				//with geneGroups.get(column)
				else{
					geneCol = type + geneGroups.get(column).get(0) + "X";
				}
			}
			
			//move from top down to find the last fusedGroup 
			//(history of clusters) containing any gene from 
			//current rowGroup so that the correct NODE-connection can be found
			for(int j = geneIntegerTable.size() - 1; j >= 0; j--) {
				
				//this currently gets the last node that has a common element
				//if the 2 groups have elements in common...
				if(!Collections.disjoint(geneIntegerTable.get(j), rowGroup)) {
					List<Integer> intersect = 
							new ArrayList<Integer>(geneIntegerTable.get(j));
					intersect.retainAll(rowGroup);
					
					//assigns NODE # of last fusedGroup containing 
					//a rowGroup element
					geneRow = dataTable.get(j).get(0);
					break;
					
				} else {
					//random fix to see what happens
					geneRow = type + geneGroups.get(row).get(0) + "X";
				}
			}
		}
		
		dataList.add(geneCol);
		dataList.add(geneRow);
		
		return dataList;
    }
    
    public void reorderGen(List<Integer> finalCluster) {
    	
    	String element = "";
    	
    	for(int e : finalCluster) {
    		
    		element = type + e + "X";
    		reorderedList.add(element);
    	}
    }
    
    /**
     * Method used to generate a new row/col for the distance matrix 
     * which is processed. The new row/col represents the joint gene pair 
     * which has been chosen as the one with the minimum distance 
     * in each iteration. The values of the new row/col are calculated 
     * according to the chosen cluster method. 
     * @param fusedGroup
     * @param geneGroups
     * @param newClade
     * @return newClade
     */
    public List<Double> newCladeGen(List<Integer> fusedGroup, 
    		List<Double> newClade) {
    	
    	for(int i = 0; i < geneGroups.size(); i++) {
			
			double distanceSum = 0;
			double newVal = 0;
			double distanceVal = 0;
			int selectedGene = 0;
			
			//check if fusedGroup contains the current checked gene 
			//(then no mean should be calculated) no elements in common
			if(Collections.disjoint(geneGroups.get(i), fusedGroup)) {
				
				if(method.contentEquals("Average Linkage")) {
	    			//select members of the new clade (B & G)	
		    		for(int j = 0; j < fusedGroup.size(); j++) {
		    				
		    			selectedGene = fusedGroup.get(j);
		    			
		    			List<Double> currentRow = dMatrix.get(selectedGene);
		    			
		    			for(int gene : geneGroups.get(i)) {
		    				
			    			distanceVal = currentRow.get(gene);
			    			//dMatrix.get(selectedGene).get(gene);
			    			distanceSum += distanceVal;
			    			
		    			}
		    		}
		    		
		    		//newVal = mean
		    		newVal = distanceSum/(fusedGroup.size() 
		    				* geneGroups.get(i).size());
		    		
				} else if(method.contentEquals("Single Linkage") 
						|| method.contentEquals("Complete Linkage")){
					
					List<Double> distances = new ArrayList<Double>();
					
					for(int j = 0; j < fusedGroup.size(); j++) {
	    				
		    			selectedGene = fusedGroup.get(j);
		    			
		    			List<Double> currentRow = dMatrix.get(selectedGene);
		    			
		    			for(int gene : geneGroups.get(i)) {
		    				
			    			distanceVal = currentRow.get(gene);
			    			//dMatrix.get(selectedGene).get(gene);
			    			distances.add(distanceVal);
		    			}
		    			
		    		}
		    		
					if(method.contentEquals("Single Linkage")){
						//newVal = min
						newVal = Collections.min(distances);
						
					} else {
						//newVal = max
						newVal = Collections.max(distances);
					}
		    
				}
    			newClade.add(newVal);
    			
			}
			//all elements in common
			else if(geneGroups.get(i).containsAll(fusedGroup)) {
				newVal = 0.0;
				newClade.add(newVal);
			} else {

			}
		}
    
    	return newClade;
    }
    
    /**
     * Accessor for the reordered list
     * @return
     */
    public List<String> getReorderedList(){
    	
    	return reorderedList;
    }
    
    /**
     * Accessor for the file path
     * @return
     */
    public String getFilePath(){
    	
    	return filePath;
    }
}
