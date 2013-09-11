package Cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JProgressBar;

/**
 * Test class to assess whether the operation can work on half a matrix (due to symmetry) 
 * @author CKeil
 *
 */
public class ClusterGenerator2 {

	//Instance variables
	private ClusterModel model;
	private JFrame frame;
	private String filePath;
	private String type;
	private String method;
	private JProgressBar pBar;
	
	//Distance Matrix
	private List<List<Double>> dMatrix;
	
	//Half of the Distance Matrix (since it's symmetrical)
	private List<List<Double>> halfDMatrix;
	
	//list to keep track of previously used minimum values in the dMatrix
	private List<Double> usedMins;
	
	//list to return ordered GENE numbers for .cdt creation
	private List<String> reorderedList = new ArrayList<String>();
	
	//Constructor (building the object)
	public ClusterGenerator2(ClusterModel model, JFrame currentFrame, List<List<Double>> dMatrix, 
			JProgressBar pBar, String type, String method){
		
		this.model = model;
		this.frame = currentFrame;
		this.dMatrix = dMatrix;
		this.type = type;
		this.method = method;
		this.pBar = pBar;
	}
	
	//method to do the actual clustering of data using the distance matrix previously calculated
    public void cluster(){
    	
    	//ProgressBar maximum = amount of genes
    	pBar.setMaximum(dMatrix.size());
    	
    	//list which stores data to be written to file
    	List<List<String>> dataTable = new ArrayList<List<String>>();
    	
    	//list with integer representation of genes for references in calculations (list of fusedGroups)
    	List<List<Integer>> geneIntegerTable = new ArrayList<List<Integer>>();
    		
		usedMins = new ArrayList<Double>();
		
		//this distance list will be mutated as the genes are clustered 
		//(shrinks by 1 each step as a gene is added to a cluster)
		//newDList = deepCopy(dMatrix);
		
		halfDMatrix = deepCopy(dMatrix);
		
		//trying to work with half a distance matrix
		halfDMatrix = splitMatrix(halfDMatrix);
		
		//list of genes in integer representation to keep track of the clades and cluster formation
		//corresponds to gene names (123 = GENE123X)and is mutated along with newDList
		//e.g. clustering of GENE0X and GENE 233X: {{0}, ...,{233},...} --> {{0, 233},...}
		List<List<Integer>> geneGroups = new ArrayList<List<Integer>>();
		
		//fill list with integers corresponding to the genes
		for(int i = 0; i < halfDMatrix.size(); i++){
			
			List<Integer> group = new ArrayList<Integer>();
			group.add(i);
			geneGroups.add(group);
		}

		System.out.println("dMatrix 872 S " + dMatrix.get(872).size());
		System.out.println("halfdMatrix 872 S " + halfDMatrix.get(872).size());
		
		//continue process until newDList has a size of 1, which means that there is only 1 cluster
		//initially every gene is its own cluster
		while(halfDMatrix.size() > 1452){
			
			System.out.println("~~~~~~~~~~~~~~");
    		//update ProgressBar
    		pBar.setValue(dMatrix.size() - halfDMatrix.size());
    		
    		//local variables
    		double min = 0;
    		int row = 0;
    		int column = 0;
    		double geneMin = 0;
    		
    		//list to store the minimum value of each gene at each while-loop iteration
    		List<Double> geneMinList = new ArrayList<Double>();
    		
    		//list to store the Strings which represent calculated data (such as gene pairs)
    		//to be added to dataTable
    		List<String> pair = new ArrayList<String>();

    		//the row value is just the position of the corresponding column value in columnValues
    		List<Integer> colMinIndexList = new ArrayList<Integer>();
    		
    		//going through every gene (row) in newDList
    		//takes 300-400ms
    		for(int j = 0; j < halfDMatrix.size(); j++){
    			
    			//select current gene
    			List<Double> gene = halfDMatrix.get(j);
    			
    			//just avoid the first empty list in the half-distance matrix
    			if(gene.size() > 0){
    			
	    			//make trial the minimum value of that gene
	    			geneMin = findRowMin(gene);
	    			
	    			//minimums of each gene in distance matrix
	    			//does not contain previously used minimums 
	    			geneMinList.add(geneMin);
	    			
	    			//add the column of each gene's minimum to a list
	    			colMinIndexList.add(gene.indexOf(geneMin));
    			
    			}
    			//for the first empty list in the half-distance matrix, add the largest value
    			//of the last row so it won't be mistaken as a minimum value
    			else{
    				
    				int last = halfDMatrix.size() - 1;
    				
    				geneMinList.add(Collections.max(halfDMatrix.get(last)));
    				colMinIndexList.add(halfDMatrix.get(last).indexOf(Collections.max(halfDMatrix.get(last))));
    			}

    		}
    		
    		//NOTE: GeneMin List is created correctly
    		
    		System.out.println("GeneMinList: " + geneMinList);
    		
    		//finds the row (gene) which has the minValue
    		row = geneMinList.indexOf(Collections.min(geneMinList));
    		
    		//finds the column by referring back to values added for each gene
    		//knowing the gene with the minimum value (row) allows to find the corresponding column
    		column = colMinIndexList.get(row);
    		
    		System.out.println("Row: " + row);
    		System.out.println("Col: " + column);
    		System.out.println("halfDMatrix.get(row) " + halfDMatrix.get(row));
    		System.out.println("halfDMatrix.get(row) S " + halfDMatrix.get(row).size());
    		
    		//row and column value of the minimum distance value in matrix are now known
    		min = halfDMatrix.get(row).get(column);
    		
    		System.out.println("Pearson: " + (1 - min));
    		
    		usedMins.add(min);
    		
    		//the replacement list for the two removed lists (joint clusters)
    		List<Double> newClade = new ArrayList<Double>();
    		
    		//suspected issue: some genes in newDList are 1 elemtn too large and as a result 
    		//the column value is too large for geneGroups
    		//just tested: previously fused Genes are all 1 size larger than non-fused (??)
    		// issue only occurs if one of the 2 current genes is already present in a fused group
    		List<Integer> rowGroup = geneGroups.get(row); 
    		List<Integer> colGroup = geneGroups.get(column); 
    		
    		//the new geneGroup containing the all genes (BG)
    		List<Integer> fusedGroup = new ArrayList<Integer>();
    		fusedGroup.addAll(rowGroup);
    		fusedGroup.addAll(colGroup); 
    		
    		//make Strings for String list to be written to data file
    		String geneRow = "";
    		String geneCol = "";
    		pair.add("NODE" + (dMatrix.size() - halfDMatrix.size() + 1) + "X");
    		
    		//check the lists in dataMatrix whether the genePair you want to add now is already
    		//in a list from before, if yes connect to LATEST node by replacing the gene name with the node name	
			if(fusedGroup.size() == 2){
					
				geneRow = type + geneGroups.get(row).get(0) + "X"; 
				geneCol = type + geneGroups.get(column).get(0) + "X";

				fillRList(geneRow, geneCol);
			}
			
			//if size of fusedGroup exceeds 2...
			else{
				
				//variables for?
				String geneCol2 = "";
				String geneRow2 = "";
				
				//move from top down to find the last fusedGroup (history of clusters) containing any gene
				//from current colGroup so that the correct NODE-connection can be found
				for(int j = geneIntegerTable.size() - 1; j >= 0; j--){
					
					//this currently gets the last node that has a common element
					//if the 2 groups have elements in common...
					if(!Collections.disjoint(geneIntegerTable.get(j), colGroup)){
						
						List<Integer> intersect = new ArrayList<Integer>(geneIntegerTable.get(j));
						intersect.retainAll(colGroup);
						
						//assigns NODE # of last fusedGroup containing a colGroup element
						geneCol = dataTable.get(j).get(0);
							
						geneCol2 =  type + intersect.get(0) + "X";

						break;
					}
					//if the current fusedGroup in geneIntegerTable does not have any elements
					//in common with geneGroups.get(column)
					else{
							
						geneCol = type + geneGroups.get(column).get(0) + "X";
						geneCol2 = type + geneGroups.get(column).get(0) + "X";

    				}
    			}
				
				//move from top down to find the last fusedGroup (history of clusters) containing any gene
				//from current rowGroup so that the correct NODE-connection can be found
    			for(int j = geneIntegerTable.size() - 1; j >= 0; j--){
    				
					//this currently gets the last node that has a common element
					//if the 2 groups have elements in common...
    				if(!Collections.disjoint(geneIntegerTable.get(j), rowGroup)){
    					
						List<Integer> intersect = new ArrayList<Integer>(geneIntegerTable.get(j));
						intersect.retainAll(rowGroup);
						
						//assigns NODE # of last fusedGroup containing a rowGroup element
    					geneRow = dataTable.get(j).get(0);
    					
    					geneRow2 = type + intersect.get(0) + "X";

    					break;
    				}
    				else{
    						
    					geneRow = type + geneGroups.get(row).get(0) + "X";
    					geneRow2 = type + geneGroups.get(row).get(0) + "X";
    				}
				}
    			
    			fillRList(geneRow2, geneCol2);
			}
			
    		pair.add(geneCol);
    		pair.add(geneRow);
    		pair.add(String.valueOf(1 - min));
    		
//    		System.out.println("------------------------------------------");
//    		System.out.println("Data to save: " + pair.toString());
    		
    		dataTable.add(pair);
    		geneIntegerTable.add(fusedGroup);

    		//remove element with bigger list position first to avoid list shifting issues
    		if(row > column){
    			
    			geneGroups.remove(row);
	    		geneGroups.remove(column);
	    		
    		}
    		else{
    			
    			geneGroups.remove(column);
    			geneGroups.remove(row);
    		}

    		//might need adjustment for different cluster methods
			if(rowGroup.contains(Collections.min(fusedGroup))){
				
				geneGroups.add(row, fusedGroup);
			}
			else if(colGroup.contains(Collections.min(fusedGroup))){
				
				geneGroups.add(column, fusedGroup);
			}
			else{
				
				System.out.println("Weird error.");
			}
			
    		//remove old elements first;
			if(column > row){
				
				halfDMatrix.remove(column);
				halfDMatrix.remove(row);
				
	    		for(List<Double> element : halfDMatrix){
	    			
	    			if(element.size() > column){
	    				
	    				element.remove(column);
	    			}
	    			
	    			if(element.size() > row){
	    			
	    				element.remove(row);
	    			}
	    		}
			}
			else{
				
				halfDMatrix.remove(row);
				halfDMatrix.remove(column);
				
	    		for(List<Double> element : halfDMatrix){
	    			
	    			if(element.size() > row){
	    				
	    				element.remove(row);
	    			}
	    			
	    			if(element.size() > column){
	    				
	    				element.remove(column);
	    			}
	    		}
	    		
			}
	
    		//fill newClade with new values
    		//mean = 0.0 with itself
    		//move through geneGroups (the gene to compare the new fused Group with
    		for(int i = 0; i < geneGroups.size(); i++){
    			
    			double distanceSum = 0;
    			double newVal = 0;
    			double distanceVal = 0;
    			int selectedGene = 0;
    		
    			//check if fusedGroup contains the current checked gene (then no mean should be calculated)
    			//no elements in common
    			if(Collections.disjoint(geneGroups.get(i), fusedGroup)){
    				
    				if(method.contentEquals("Average Linkage")){
		    			//select members of the new clade (B & G)	
			    		for(int j = 0; j < fusedGroup.size(); j++){
			    				
			    			selectedGene = fusedGroup.get(j);
			    			
			    			for(int gene : geneGroups.get(i)){
			    				
				    			distanceVal = dMatrix.get(selectedGene).get(gene);
				    			distanceSum += distanceVal;
				    			
			    			}

			    		}
			    		
			    		//newVal = mean
			    		newVal = distanceSum/(fusedGroup.size() * geneGroups.get(i).size()) ;
    				}
    				else if(method.contentEquals("Single Linkage") || method.contentEquals("Complete Linkage")){
    					
    					List<Double> distances = new ArrayList<Double>();
    					
    					for(int j = 0; j < fusedGroup.size(); j++){
		    				
			    			selectedGene = fusedGroup.get(j);
			    			
			    			for(int gene : geneGroups.get(i)){
			    				
				    			distanceVal = dMatrix.get(selectedGene).get(gene);
				    			distances.add(distanceVal);
			    			}

			    		}
			    		
    					if(method.contentEquals("Single Linkage")){
    						
    						//newVal = min
    						newVal = Collections.min(distances);
    					}
    					else{
    						
    						//newVal = max
    						newVal = Collections.max(distances);
    					}
			    
    				}
	    			
	    			newClade.add(newVal);
    			}
    			
    			//all elements in common
    			else if(geneGroups.get(i).containsAll(fusedGroup)){
    				
    				newVal = 0.0;
    				newClade.add(newVal);
    			}
    			
    			else{
    				
    				System.out.println("(i): " + i);
    				System.out.println("geneGroups.get(i): " + geneGroups.get(i).toString());
    				System.out.println("FusedGroup: " + fusedGroup.toString());
    				
    			}
    			
    		}
    		
			if(rowGroup.contains(Collections.min(fusedGroup))){
				
	    		for(List<Double> element : halfDMatrix){
	    			
	    			//add to element at index 'row'....what if the element is not as big?
	    			if(element.size() >= row){
	    			
	    				element.add(row, newClade.get(halfDMatrix.indexOf(element)));
	    			}

	    		}
	    		
	    		//needs to come after filling all other elements with a row value, 
	    		//since newClade already has it
	    		halfDMatrix.add(row, newClade);
	    		
			}
			else if(colGroup.contains(Collections.min(fusedGroup))){
				
	    		for(List<Double> element : halfDMatrix){
	    			
	    			if(element.size() >= column){
	    				
	    				element.add(column, newClade.get(halfDMatrix.indexOf(element)));
	    			}
	    			
	    		}
	    		
	    		halfDMatrix.add(column, newClade);
	    		
			}
			else{
				
				System.out.println("Weird error.");
			}
			
		}
    
		
    	System.out.println("FINAL reorderedList Size: " + reorderedList.size());
    	
    	Set<String> set = new HashSet<String>(reorderedList);
    	
    	if(set.size() < reorderedList.size()){
    		System.out.println("Duplicates!");
    	}
    	
    	//System.out.println("FINAL reorderedList: " + reorderedList.toString());
    	
    	ClusterFileWriter dataFile = new ClusterFileWriter(frame, model);
    	
    	//generate files for Dendrogram
    	if(type.equalsIgnoreCase("GENE")){
    		
    		dataFile.writeFile(dataTable, ".gtr");
    	}
    	else{
    		
    		dataFile.writeFile(dataTable, ".atr");
    	}
    	
		filePath = dataFile.getFilePath();
    }
    
    /**
     * Method to half the distance matrix. This should be done because a distance matrix is symmetrical and
     * it saves computational time to do operations on only half of the values.
     * @param distanceMatrix
     * @return half distanceMatrix
     */
    public List<List<Double>> splitMatrix(List<List<Double>> distanceMatrix){
    	
    	//distance matrices are symmetrical!
    	for(int i = 0; i < distanceMatrix.size(); i++){

    		int stop = distanceMatrix.get(i).size();
    		
    		distanceMatrix.get(i).subList(i, stop).clear();
    	}
    	
    	return distanceMatrix;
    }
    
    /**
     * Method to make deep copy of distance matrix
     * @return
     */
    public List<List<Double>> deepCopy(List<List<Double>> distanceMatrix){
    	
    	List<List<Double>> deepCopy = new ArrayList<List<Double>>();
    	
		for(int i = 0; i < distanceMatrix.size(); i++){
			
			List<Double> newList = new ArrayList<Double>();
			deepCopy.add(newList);
			
			for(int j = 0; j < distanceMatrix.size(); j++){
				
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
    public double findRowMin(List<Double> gene){
    	
		double geneMin = 0.0;
		
		//standard collection copy constructor to make deep copy to protect gene
		List<Double> deepGene = new ArrayList<Double>(gene);
		
		//Can't sort gene because it needs to remain unchanged
		//need deep copy
		Collections.sort(deepGene);
		
		for(int i = 0; i < deepGene.size(); i++){
			
			if(!usedMins.contains(deepGene.get(i))){
				
				geneMin = deepGene.get(i);
				break;
			}
		}
		
		return geneMin;
    }
    
    /**
     * method to fill reorderedList with the needed data 
     */
    public void fillRList(String geneRow, String geneCol){
    	
    	Random random = new Random();
    	
    	if(reorderedList.contains(geneRow) && reorderedList.contains(geneCol)){
			
		}
		else if(reorderedList.contains(geneRow)){
			
			if(random.nextBoolean()){
				
				reorderedList.add(0, geneCol);
			}
			else{
			
				reorderedList.add(geneCol);
			}
			
		}
		else if(reorderedList.contains(geneCol)){
			
			if(random.nextBoolean()){
				
				reorderedList.add(0, geneRow);
			}
			else{
				
				reorderedList.add(geneRow);
			}
		}
		else{
			
			if(random.nextBoolean()){
				
				reorderedList.add(0, geneRow);
				reorderedList.add(1, geneCol);
			}
			else{
				
				reorderedList.add(geneRow);
				reorderedList.add(reorderedList.size() - 1, geneCol);
			}
		}
    }
    
    /**
     * Accessor for the reordered list
     * @return
     */
    public List<String> getReorderedList(){
    	
    	return reorderedList;
    }
    
    //Access the file path generated by the clustering function
    public String getFilePath(){
    	
    	return filePath;
    }
}
