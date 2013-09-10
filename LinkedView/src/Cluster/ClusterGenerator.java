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
 * This class uses the previously calculated distance matrices and detects gene clusters
 * according to the chosen method. It returns a reordered list of the original data set and 
 * generates a .ATR and/ or .GTR file (depending on what the user chooses to cluster)
 * @author CKeil
 *
 */
public class ClusterGenerator {

	//Instance variables
	private ClusterModel model;
	private JFrame frame;
	private String filePath;
	private String type;
	private String method;
	private JProgressBar pBar;
	
	//Distance Matrix
	private List<List<Double>> dMatrix;
	
	//Deep copy of dMatrix
	private List<List<Double>> newDList;
	
	//list to keep track of previously used minimum values in the dMatrix
	private List<Double> usedMins;
	
	//list to return ordered GENE numbers for .cdt creation
	private List<String> reorderedList = new ArrayList<String>();
	
	//Constructor (building the object)
	public ClusterGenerator(ClusterModel model, JFrame currentFrame, List<List<Double>> dMatrix, 
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
		newDList = deepCopy();
		
		//list of genes in integer representation to keep track of the clades and cluster formation
		//corresponds to gene names (123 = GENE123X)and is mutated along with newDList
		//e.g. clustering of GENE0X and GENE 233X: {{0}, ...,{233},...} --> {{0, 233},...}
		List<List<Integer>> geneGroups = new ArrayList<List<Integer>>();
		
		//fill list with integers corresponding to the genes
		for(int i = 0; i < newDList.size(); i++){
			
			List<Integer> group = new ArrayList<Integer>();
			group.add(i);
			geneGroups.add(group);
		}
		
		//continue process until newDList has a size of 1, which means that there is only 1 cluster
		//initially every gene is its own cluster
		while(newDList.size() > 1){
			
    		//update ProgressBar
    		pBar.setValue(dMatrix.size() - newDList.size());
    		
    		//local variables
    		double min = 0;
    		int row = 0;
    		int column = 0;
    		double trial = 0;
    		
    		//list to store the minimum value of each gene at each while-loop iteration
    		List<Double> geneMins = new ArrayList<Double>();
    		
    		//list to store the Strings which represent calculated data (such as gene pairs)
    		//to be added to dataTable
    		List<String> pair = new ArrayList<String>();

    		//the row value is just the position of the corresponding column value in columnValues
    		List<Integer> columnValues = new ArrayList<Integer>();
    		
    		//going through every gene (row) in newDList
    		//takes 300-400ms
    		for(int j = 0; j < newDList.size(); j++){
    			
    			//select current gene
    			List<Double> gene = newDList.get(j);
    			
    			//take first non-zero value in gene
    			for(Double element : gene){
        			
    				if(element != 0.0){
        				
        				trial = element;
        				break;
        			}
    			}
    			
    			//make trial the minimum value of that gene
    			trial = findRowMin(trial, gene);
    			
    			//minimums of each gene in distance matrix
    			//does not contain previously used minimums 
    			geneMins.add(trial);
    			
    			//add the column of each gene's minimum to a list
    			columnValues.add(gene.indexOf(trial));

    		}
    		
    		//finds the row (gene) which has the minValue
    		row = geneMins.indexOf(Collections.min(geneMins));
    		
    		//finds the column by referring back to values added for each gene
    		//knowing the gene with the minimum value (row) allows to find the corresponding column
    		column = columnValues.get(row);
    		
    		//row and column value of the minimum distance value in matrix are now known
    		min = newDList.get(row).get(column);

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
    		pair.add("NODE" + (dMatrix.size() - newDList.size() + 1) + "X");
    		
    		Random random = new Random();
    		
    		//check the lists in dataMatrix whether the genePair you want to add now is already
    		//in a list from before, if yes connect to LATEST node by replacing the gene name with the node name	
			if(fusedGroup.size() == 2){
					
				geneRow = type + geneGroups.get(row).get(0) + "X"; 
				geneCol = type + geneGroups.get(column).get(0) + "X";

				fillRList(geneRow, geneCol);
//				if(reorderedList.contains(geneRow) && reorderedList.contains(geneCol)){
//					
//				}
//				else if(reorderedList.contains(geneRow)){
//					
//					if(random.nextBoolean()){
//						
//						reorderedList.add(0, geneCol);
//					}
//					else{
//					
//						reorderedList.add(geneCol);
//					}
//					
//				}
//				else if(reorderedList.contains(geneCol)){
//					
//					if(random.nextBoolean()){
//						
//						reorderedList.add(0, geneRow);
//					}
//					else{
//						
//						reorderedList.add(geneRow);
//					}
//				}
//				else{
//					
//					if(random.nextBoolean()){
//						
//						reorderedList.add(0, geneRow);
//						reorderedList.add(1, geneCol);
//					}
//					else{
//						
//						reorderedList.add(geneRow);
//						reorderedList.add(reorderedList.size() - 1, geneCol);
//					}
//				}
				
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
//				if(reorderedList.contains(geneRow2) && reorderedList.contains(geneCol2)){
//					
//				}
//				else if(reorderedList.contains(geneRow2)){
//					
//					if(random.nextBoolean()){
//						
//						reorderedList.add(0, geneCol2);
//					}
//					else{
//					
//						reorderedList.add(geneCol2);
//					}
//					
//				}
//				else if(reorderedList.contains(geneCol2)){
//					
//					if(random.nextBoolean()){
//						
//						reorderedList.add(0, geneRow2);
//					}
//					else{
//						
//						reorderedList.add(geneRow2);
//					}
//				}
//				else{
//					
//					if(random.nextBoolean()){
//						
//						reorderedList.add(0, geneRow2);
//						reorderedList.add(1, geneCol2);
//					}
//					else{
//						
//						reorderedList.add(geneRow2);
//						reorderedList.add(reorderedList.size() - 1, geneCol2);
//					}
//				}
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
				
	    		newDList.remove(column);
	    		newDList.remove(row);
	    		
	    		for(List<Double> element : newDList){
	    			
	    			element.remove(column);
	    			element.remove(row);
	    		}
			}
			else{
				
	    		newDList.remove(row);
	    		newDList.remove(column);
	    		
	    		for(List<Double> element : newDList){
	    			
	    			element.remove(row);
	    			element.remove(column);
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
				
	    		for(List<Double> element : newDList){
	    			
	    			element.add(row, newClade.get(newDList.indexOf(element)));
	    			
	    		}
	    		
	    		//needs to come after filling all other elements with a row value, 
	    		//since newClade already has it
	    		newDList.add(row, newClade);
	    		
			}
			else if(colGroup.contains(Collections.min(fusedGroup))){
				
	    		for(List<Double> element : newDList){
	    			
	    			element.add(column, newClade.get(newDList.indexOf(element)));
	    			
	    		}
	    		
	    		newDList.add(column, newClade);
	    		
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
     * Method to make deep copy of distance matrix
     * @return
     */
    public List<List<Double>> deepCopy(){
    	
    	List<List<Double>> deepCopy = new ArrayList<List<Double>>();
    	
		for(int i = 0; i < dMatrix.size(); i++){
			
			List<Double> newList = new ArrayList<Double>();
			deepCopy.add(newList);
			
			for(int j = 0; j < dMatrix.size(); j++){
				
				Double e = new Double(dMatrix.get(i).get(j));
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
    public double findRowMin(double trial, List<Double> gene){
    	
		for(int k = 0; k < gene.size(); k++){ 
			
			if(gene.get(k) != 0.0){
	
				if(trial > gene.get(k) && !usedMins.contains(gene.get(k))){
					
					trial = gene.get(k);
				}
			}	
		}
		
		return trial;
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
