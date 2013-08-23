package Cluster;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JProgressBar;

/**
 * This class takes the original uploaded dataArray passed 
 * in the constructor and manipulates it according to mathematical 
 * principles of hierarchical clustering. It generates files to display dendrograms (.gtr and .atr)
 * as well as a reordered original data file (.cdt)
 * @author CKeil
 *
 */
public class HierarchicalCluster {
	
	//Instance variables
	ClusterModel model;
	private JFrame frame;
	private String filePath;
	
	IntHeaderInfoCluster headerInfo = new IntHeaderInfoCluster();
	
	//Constructor (building the object)
	public HierarchicalCluster(ClusterModel model, JFrame currentFrame){
		
		this.model = model;
		this.frame = currentFrame;
	}
	
	//getting rows from raw data array
	public List <List<Double>> splitRows(List<Double> list, JProgressBar pBar){
		
		int lower = 0;
		int upper = 0;
		
		//number of arrays
		int max = model.nExpr();
		
		pBar.setMaximum(list.size()/max);
		
		List<List<Double>> rowList = new ArrayList<List<Double>>();
		
		
		for(int i = 0; i < list.size()/max; i++){
			
			pBar.setValue(i);
			
			upper+=max;
			
			rowList.add(list.subList(lower, upper));
			
			lower = upper;
			
		}
		
		if(upper < list.size() -1){
			
			lower = upper;
			upper = list.size();
			
			rowList.add(list.subList(lower, upper));
		}
		
		return rowList;
	}
	
	//getting the columns from raw data array
	public List <List<Double>> splitColumns(List<Double> list, JProgressBar pBar){
		
		//number of arrays/ columns (3277 for test)
		int max = model.nExpr();
		int nGenes = model.nGene();
		
		//setting up progressbar
		pBar.setMaximum(list.size()/max);
		
		List<List<Double>> columnsList = new ArrayList<List<Double>>();
		
		//iterate through columns ...max
		for(int j = 0; j < max; j++){
			
			pBar.setValue(j);
			
			List<Double> sArray = new ArrayList<Double>();
			
			for(int i = 0; i < nGenes; i++){
				
				int element = (i * max) + j;
				
				sArray.add(list.get(element));
				
			}
			
			columnsList.add(sArray);
		}
	
		return columnsList;
	}
    
	//Uncentered Pearson (mean values = 0)
	public List <List<Double>> pearson(List<List<Double>> fullList, JProgressBar pBar, boolean absolute, boolean centered){
		
    	//list with all genes and their distances to all other genes (1455x1455 for sample data)
    	List <List<Double>> distanceList = new ArrayList<List<Double>>();
    	
    	pBar.setMaximum(fullList.size());
    	
    	//take a gene
    	for(int i = 0; i < fullList.size(); i++){
    		
    		//long ms = System.currentTimeMillis();
    		
    		//update progressbar
    		pBar.setValue(i);
    		
    		//refers to one gene with all it's data
    		List<Double> data = fullList.get(i);
    		
    		//pearson values of one gene compared to all others
    		List<Double> pearsonList = new ArrayList<Double>();
			
			//second gene for comparison
    		for(int j = 0; j < fullList.size(); j++){
    			
    			
    			//initialize needed instance variables
            	double xi = 0;
            	double yi = 0;
            	double mean_x = 0;
            	double mean_y = 0;
            	double mean_sumX = 0;
            	double mean_sumY = 0;
            	double sumX = 0;
            	double sumY = 0;
            	double sumXY = 0;
            	double sumX_root = 0;
            	double sumY_root = 0;
    			BigDecimal pearson2;
    			double pearson1 = 0;
    			double rootProduct;
    			double finalVal;
    			
    			List<Double> data2 = fullList.get(j);
    			
    			if(centered){//causes issues in cluster(????)
    				
        			for(double x : data){
        				
        				mean_sumX += x;
        			}
        			
        			mean_x = mean_sumX/(double)data.size(); //casted int to double
        			
        			for(double y : data2){
        				
        				mean_sumY += y;
        			}
        			
        			mean_y = mean_sumY/(double)data2.size();
        			
    			}
    			else{//works great in cluster
    				
    				mean_x = 0;
    				mean_y = 0;
    			}
    			
    			//compare each value of both genes
    			for(int k = 0; k < data.size(); k++){
    				
    				//part x
    				xi = data.get(k);
    				sumX += (xi - mean_x) * (xi - mean_x);
    				
    				//part y
    				yi = data2.get(k);
    				sumY += (yi - mean_y) * (yi - mean_y);
    				
    				//part xy
    				sumXY += (xi - mean_x) * (yi - mean_y);
    			}
    			
    			
    			sumX_root = Math.sqrt(sumX);
    			sumY_root = Math.sqrt(sumY);
    			
	    		
    			//calculate pearson value for current gene pair
    			rootProduct = (sumX_root * sumY_root);
    			
    			if(absolute){
    				
    				finalVal = Math.abs(sumXY/rootProduct);
    			}
    			else{
    				
    				finalVal = sumXY/rootProduct;
    			}
    			
    			pearson1 = 1.0 - finalVal;
    			
    			//using BigDecimal to correct for rounding errors caused by floating point arithmetic 
    			//(0.0 would be -1.113274672357E-16 for example)
    			pearson2 = new BigDecimal(String.valueOf(pearson1));
    			pearson2 = pearson2.setScale(6, BigDecimal.ROUND_DOWN);
    			
	    		pearsonList.add(pearson2.doubleValue());
	    		
    		}
    		
    		distanceList.add(pearsonList);
    	}
    	
    	return distanceList;
    }

	//Euclidean Distance
    //returns Euclidean PROXIMITY MATRIX, N x N;
    public List <List<Double>> euclid(List<List<Double>> fullList, JProgressBar pBar){
		
    	//list with all genes and their distances to all other genes (1455x1455 for sample data)
    	List <List<Double>> geneDistanceList = new ArrayList<List<Double>>();
		
    	double sDist = 0;
    	double g1 = 0;
    	double g2 = 0;
    	double gDiff = 0;
    	
    	pBar.setMaximum(fullList.size());
    	
    	//take a gene
    	//300ms per loop
    	for(int i = 0; i < fullList.size(); i++){
    		
    		//long ms = System.currentTimeMillis();
    		
    		//update progressbar
    		pBar.setValue(i);
    		
    		//refers to one gene with all it's data
    		List<Double> gene = fullList.get(i);
    		
    		//distances of one gene to all others
    		List<Double> geneDistance = new ArrayList<Double>();
    		
    		//choose a gene for distance comparison
    		//0.15ms per loop
    		for(int j = 0; j < fullList.size(); j++){
    			
    			List<Double> gene2 = fullList.get(j);
    			
    	    	//squared differences between elements of 2 genes
    			List<Double> sDiff= new ArrayList<Double>();
    			
    			//compare each value of both genes
    			//fixed: now runs at ~40 -60k ns = 0.05ms
    			for(int k = 0; k < gene.size(); k++){
    				
    				g1 = gene.get(k);
    				g2 = gene2.get(k);
    				gDiff = g1 - g2;
    				sDist = gDiff * gDiff;
    				sDiff.add(sDist);
    			}
    			
    			//sum all the squared value distances up
    			//--> get distance between gene and gene2
    			double sum = 0;
    			
    			//runs at ~5000ns or 0.005ms --> irrelevant
    			for(double element : sDiff){
    				sum += element;
    			}
    			
//    			double rootedSum = 0;
//    			rootedSum = Math.sqrt(sum);
//    			
//    			//Mathematically RIGHT? Not used in Cluster 3.0 but Euclidean Distance is caalculated this way
//    			geneDistance[j] = rootedSum;
    			
    			double divSum = 0;
    			divSum = sum/fullList.size();
    			
    			geneDistance.add(divSum);
    		}
    		
    		//System.out.println("#1 Loop Time: " + (System.currentTimeMillis()-ms));

    		//list with all genes and their distances to the other genes
    		geneDistanceList.add(geneDistance);
    		
    	}
    	
    	return geneDistanceList;
    }
    
    public List <List<Double>> cityBlock(List<List<Double>> fullList, JProgressBar pBar){
		
    	//list with all genes and their distances to all other genes (1455x1455 for sample data)
    	List <List<Double>> distanceList = new ArrayList<List<Double>>();

    	double g1 = 0;
    	double g2 = 0;
    	double gDiff = 0;
    	
    	pBar.setMaximum(fullList.size());
    	
    	//take a gene
    	for(int i = 0; i < fullList.size(); i++){
    		
    		//long ms = System.currentTimeMillis();
    		
    		//update progressbar
    		pBar.setValue(i);
    		
    		//refers to one gene with all it's data
    		List<Double> data = fullList.get(i);
    		
    		//distances of one gene to all others
    		List<Double> dataDistance = new ArrayList<Double>();
    		
    		//choose a gene for distance comparison
    		for(int j = 0; j < fullList.size(); j++){
    			
    			List<Double> data2 = fullList.get(j);
    			
    	    	//differences between elements of 2 genes
    			List<Double> sDiff= new ArrayList<Double>();
    			
    			//compare each value of both genes
    			//fixed: now runs at ~40 -60k ns = 0.05ms
    			for(int k = 0; k < data.size(); k++){
    				
    				g1 = data.get(k);
    				g2 = data2.get(k);
    				gDiff = g1 - g2;
    				gDiff = Math.abs(gDiff);
    				sDiff.add(gDiff);
    			}
    			
    			//sum all the squared value distances up
    			//--> get distance between gene and gene2
    			double sum = 0;
    			
    			for(double element : sDiff){
    				sum += element;
    			}
    			
    			dataDistance.add(sum);
    		}
    		
    		//System.out.println("#1 Loop Time: " + (System.currentTimeMillis()-ms));

    		//list with all genes and their distances to the other genes
    		distanceList.add(dataDistance);
    		
    	}
    	
    	return distanceList;
    }
	
    //method to do the actual clustering of data using the distance matrix previously calculated
    public void cluster(List<List<Double>> geneDList, JProgressBar pBar, boolean type, String method){
    	
    	//list which stores data to be written to file
    	List<List<String>> dataTable = new ArrayList<List<String>>();
    	
    	//list with integer representation of genes for references in calculations
    	List<List<Integer>> geneIntegerTable = new ArrayList<List<Integer>>();
    	
    	//ProgressBar maximum = amount of genes
    	pBar.setMaximum(geneDList.size());
    		
		//list to make sure every loop step uses the next highest min value from geneDList
		List<Double> usedMins = new ArrayList<Double>();
		
		//copy list of geneDList used as starting point for clustering algorithm
		//this distance list will be mutated as the genes are clustered 
		//(shrinks by 1 each step as a gene is added to a cluster)
		List<List<Double>> newDList = new ArrayList<List<Double>>();
		
		//make deep copy of original geneDList (so that geneDList won't be mutated)
		for(int i = 0; i < geneDList.size(); i++){
			
			List<Double> newList = new ArrayList<Double>();
			newDList.add(newList);
			
			for(int j = 0; j < geneDList.size(); j++){
				
				Double e = new Double(geneDList.get(i).get(j));
				newList.add(e);
			}
		}
		
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
    		pBar.setValue(geneDList.size() - newDList.size());
    		
    		//define some local instance variables
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
    			//check whether min was used before (in whole calculation process)
    			for(int k = 0; k < gene.size(); k++){ 
    				
    				if(gene.get(k) != 0.0){
    	
    					if(trial > gene.get(k) && !usedMins.contains(gene.get(k))){
	    					
	    					trial = gene.get(k);
	    				}
    				}	
    			}
    			
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

    		//now join the 2 genes and add the new joint clade as a row 
    		//REMEMBER: length of tree branch would be Min/2 for each gene
    		//first, create new List
    		List<Double> newClade = new ArrayList<Double>();
    		
    		//suspected issue: some genes in newDList are 1 elemtn too large and as a result 
    		//the column value is too large for geneGroups
    		//just tested: previously fused Genes are all 1 size larger than non-fused (??)
    		// issue only occurs if one of the 2 current genes is already present in a fused group
    		List<Integer> rowGroup = geneGroups.get(row); 
    		List<Integer> colGroup = geneGroups.get(column); 
    		
//	    		System.out.println("------------------------------------------");
//	    		System.out.println("RowGroup: " + rowGroup.toString());
//	    		System.out.println("ColumnGroup: " + colGroup.toString());
    		
    		//the new geneGroup containing the all genes (BG)
    		List<Integer> fusedGroup = new ArrayList<Integer>();
    		fusedGroup.addAll(rowGroup);
    		fusedGroup.addAll(colGroup); 
    		
    		//make Strings for String list to be written to data file
    		String geneRow = "";
    		String geneCol = "";
    		pair.add("NODE" + (geneDList.size() - newDList.size() + 1) + "X");
    		
    		//check the lists in dataMatrix whether the genePair you want to add now is already
    		//in a list from before, if yes connect to LATEST node by replacing the gene name with the node name	
			if(fusedGroup.size() == 2){
				
				geneRow = "GENE" + geneGroups.get(row).get(0) + "X"; 
				geneCol = "GENE" + geneGroups.get(column).get(0) + "X";
			}
			else{
				
				for(int j = geneIntegerTable.size() - 1; j >= 0; j--){
					
					//this currently gets the first node that has a common gene
					//but it should be the LAST
					if(!Collections.disjoint(geneIntegerTable.get(j), geneGroups.get(column))){
						
						geneCol = dataTable.get(j).get(0);
						break;
					}
					else{
						
						geneCol = "GENE" + geneGroups.get(column).get(0) + "X";
    					}
    				}
    				for(int j = geneIntegerTable.size() - 1; j >= 0; j--){
    					
    					if(!Collections.disjoint(geneIntegerTable.get(j),geneGroups.get(row))){
   
    						geneRow = dataTable.get(j).get(0);
    						break;
    					}
    					else{
    						geneRow = "GENE" + geneGroups.get(row).get(0) + "X";
					}
					
				}
				
			}
    			
    		pair.add(geneCol);
    		pair.add(geneRow);
    		pair.add(String.valueOf(1 - min));
    		
	    		System.out.println("------------------------------------------");
	    		System.out.println("Data to save: " + pair.toString());
    		
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
			    				
				    			distanceVal = geneDList.get(selectedGene).get(gene);
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
			    				
				    			distanceVal = geneDList.get(selectedGene).get(gene);
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
    	
//    	else if(method.contentEquals("Centroid Linkage")){
//    		
//    		
//    	}
    
		
    	System.out.println("dataTable Size: " + dataTable.size());
    	
    	ClusterFileWriter dataFile = new ClusterFileWriter(frame, model);

    		dataFile.writeFile(dataTable, type);
    		filePath = dataFile.getFilePath();
    	
    }
    
    public String getFilePath(){
    	
    	return filePath;
    }
    
    
    //method to join all gene arrays into one double[] 
    public static double[] concatAll(double[] first, List<double[]> rest) {
    	  int totalLength = first.length;
    	  for (double[] array : rest) {
    	    totalLength += array.length;
    	  }
    	  double[] result = Arrays.copyOf(first, totalLength);
    	  int offset = first.length;
    	  for (double[] array : rest) {
    	    System.arraycopy(array, 0, result, offset, array.length);
    	    offset += array.length;
    	  }
    	  return result;
    	}
    
    //erase later
    public static Double[] convert(double[] chars) {
        Double[] copy = new Double[chars.length];
        for(int i = 0; i < copy.length; i++) {
            copy[i] = Double.valueOf(chars[i]);
        }
        return copy;
    }
}
