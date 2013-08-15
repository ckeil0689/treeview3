package Cluster;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import Cluster.ClusterLoader.TimerListener;

import net.miginfocom.swing.MigLayout;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LoadProgress2;

/**
 * This class takes the original uploaded dataArray passed 
 * in the constructor and manipulates it according to mathematical 
 * principles of hierarchical clustering.
 * @author CKeil
 *
 */
public class HierarchicalCluster {
	
	//Instance variables
	ClusterModel model;
	private double[] dataArray;
	private int method;
	private String similarity;
	private JFrame frame;
	private String filePath;
	
	IntHeaderInfoCluster headerInfo = new IntHeaderInfoCluster();
	
	//Constructor (building the object)
	public HierarchicalCluster(ClusterModel model, double[] dataArray, String similarity, JFrame currentFrame){
		
		this.model = model;
		this.dataArray = dataArray;
		this.similarity = similarity;
		this.frame = currentFrame;
	}
	
	//getting elements from raw data array
	public List <List<Double>> splitElements(List<Double> list, JProgressBar pBar){
		
		int lower = 0;
		int upper = 0;
		
		//number of arrays
		int max = model.nExpr();
		
		pBar.setMaximum(list.size()/max);
		
		List<List<Double>> geneList = new ArrayList<List<Double>>();
		
		
		for(int i = 0; i < list.size()/max; i++){
			
			pBar.setValue(i);
			
			upper+=max;
			
			geneList.add(list.subList(lower, upper));
			
			lower = upper;
			
		}
		
		if(upper < list.size() -1){
			
			lower = upper;
			upper = list.size();
			
			geneList.add(list.subList(lower, upper));
		}
		
		return geneList;
	}
	
	//getting the arrays from raw data array
	public List <List<Double>> splitArrays(List<Double> list, JProgressBar pBar){
		
		//number of arrays/ columns (3277 for test)
		int max = model.nExpr();
		int nGenes = model.nGene();
		
		//setting up progressbar
		pBar.setMaximum(list.size()/max);
		
		List<List<Double>> arraysList = new ArrayList<List<Double>>();
		
		//iterate through columns ...max
		for(int j = 0; j < max; j++){
			
			pBar.setValue(j);
			
			List<Double> sArray = new ArrayList<Double>();
			
			for(int i = 0; i < nGenes; i++){
				
				int element = (i * max) + j;
				
				sArray.add(list.get(element));
				
			}
			
			arraysList.add(sArray);
		}
	
		return arraysList;
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
    			
    			//using BigDecimal to correct for rounding errors caused by floating point arithmetic (0.0 would be -1.113274672357E-16 for example)
    			pearson2 = new BigDecimal(String.valueOf(pearson1));
    			pearson2 = pearson2.setScale(6, BigDecimal.ROUND_DOWN);
    			
	    		pearsonList.add(pearson2.doubleValue());
	    		
    		}
    		
    		distanceList.add(pearsonList);
    	}
    	
//    	System.out.println("GDL Size (1455?): " + distanceList.size());
//    	System.out.println("GENE725 vs GENE872: " + distanceList.get(724).get(871));
//    	System.out.println("GENE872 vs GENE725: " + distanceList.get(871).get(724));
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
    	
    	
//    	System.out.println("DL Length: " + geneDistanceList.size());
//    	System.out.println("DL Element Length: " + geneDistanceList.get(0).size());
    	
    	//Fusing all genes and their distances together to one array
    	//double[] fusedArray = concatAll(geneDistanceList.get(0),geneDistanceList.subList(1, geneDistanceList.size()));
   
    	//System.out.println("Fused Array Length: " + fusedArray.length);
    	
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
    	
    	
//    	System.out.println(" CB DL Length: " + distanceList.size());
//    	System.out.println("CB DL Element Length: " + distanceList.get(0).size());
    	
    	//Fusing all genes and their distances together to one array
    	//double[] fusedArray = concatAll(geneDistanceList.get(0),geneDistanceList.subList(1, geneDistanceList.size()));
   
    	//System.out.println("Fused Array Length: " + fusedArray.length);
    	
    	return distanceList;
    }
	
    public void cluster(List<List<Double>> geneDList, JProgressBar pBar, boolean type, String method){
		
    	//list of genes and their closest partner gene (according to distance measure)
    	//Structure: {{g1, g2},...,{gX, gY}}
    	List<String[]> genePairs = new ArrayList<String[]>();
    	
    	//e.g. for single linkage it's the list of all minimum distances
    	//Structure: {d1, d2, d3,...,dX}
    	List<Double> linkValues = new ArrayList<Double>();
    	
    	//
    	List<List<String>> dataMatrix = new ArrayList<List<String>>();
    	
    	//progressbar maximum = amount of genes
    	pBar.setMaximum(geneDList.size());
    	
    	
    	//differentiate between methods
    	if(method.contentEquals("Single Linkage")){
    		
	    	//go through all genes
	    	for(int i = 0; i < geneDList.size(); i++){
	    		
	    		//update progressbar
	    		pBar.setValue(i);
	    		
	    		//Local variables, initialized
	    		double min = 0;	
	    		int pos = 0;
	    		String[] distPair = new String[2];
	    		
	    		//getting the current gene
	    		List<Double> gene = geneDList.get(i);
	    		
	    		//set up a sorted array for the current gene
	    		List<Double>sortedGene = new ArrayList<Double>();
	    		
	    		//copy gene to sorted array
	    		sortedGene.addAll(gene);
	    		
	    		//sort the gene (min to max)
	    		Collections.sort(sortedGene);
	    		
	    		//find first none-zero value in sorted array = shortest distance to other gene (first value is always 0.0)
	    		//if Gene A is closest to Gene B and Gene B is closest to Gene A (rare!) and the exact minimum value
	    		//already exists in minValues, for the second pair {B, A} the next closest gene/ node is taken.
	    		//set min to this value
	    		//start at k = 1 because sortedGene.get(0) will always be 0.0 
	    		//(in the matrix, this is the distance between a gene and itself, row vs. column)
	    		for(int k = 1; k < sortedGene.size(); k++ ){
	    			
	    			if(!linkValues.contains(sortedGene.get(k))){
	        			
	        			min = sortedGene.get(k);
	        			break;
	        		}
	    			
	    		}
	    		
	    		//find position of min in the current gene to figure out which the closest gene is
	    		for(int j = 0; j < gene.size(); j++){
	    			
	    			if(gene.get(j) == min){
	    				
	    				pos = j;
	    				break;
	    			}
	    		}
	    		
	    		//add the minimum distance value of each gene to minValues
	    		linkValues.add(min);
	    		
	    		//generate names and the pair of the current gene (i) and the gene closest to it (pos)
	    		if(type){
	    			
		    		distPair[0] = "GENE" + i + "X";
		    		distPair[1] = "GENE" + pos + "X";
		    		
	    		}
	    		else{
	    			
	    			distPair[0] = "ARRY" + i + "X";
		    		distPair[1] = "ARRY" + pos + "X";
	    		}
	    		
	    		//add the pair to a list
	    		genePairs.add(distPair);
	    		
	    	}
	    	
	//    	System.out.println("MinValues Length Pre: " + minValues.size());
	    	
	    	List<Double> orderedMin = new ArrayList<Double>(linkValues);
	    	Collections.sort(orderedMin);
	    	
	    	System.out.println("Ordered Min: " + orderedMin.toString());
	    	
	//----->//NEED TO CORRECT FOR INCREASING i and SHRINKING minValues (they meet midway and so only half the data gets processed)
	    	for(int i = 0; i < orderedMin.size() ; i++){  		
	    		
		    	//find least smallest distance in minValues
		    	//then associate the smallest distance with the appropriate gene pair
		    	String[] smallGene = new String[2];
		    	
		    	int k = linkValues.indexOf(orderedMin.get(i));
		    	smallGene = genePairs.get(k);
	
				//Give a Node name, add the node name as well as the gene pair (or cluster!) to array
				//if gene pair is such that one gene is already in a node, add node name
				List<String> geneLink = new ArrayList<String>();
				String newNode = "NODE" + (i+1) + "X";
				geneLink.add(newNode);
				 
				//Check all elements previously added (loop) to dataMatrix
				for(List<String> element : dataMatrix){
					
					//if any element contains a gene of the current checked pair (smallGene)
					//add the none-duplicate gene to geneLink
					if(element.contains(smallGene[0])){
						
						//Gene is replaced with node name of the element it contains
						smallGene[0] = element.get(0);
					}
					else if (element.contains(smallGene[1])){
						
						smallGene[1] = element.get(0);
					}
					
				}
				
				//Add gene names to geneLink
				//structure: {NODE1X, GENE2X, GENE3X}
				geneLink.add(smallGene[0]);
				geneLink.add(smallGene[1]);
				geneLink.add(Double.toString(orderedMin.get(i)));
				
				//add connection to dataMatrix 
				//structure: {{NODE1X, GENE2X, GENE3X}, ..., {NODE1X, GENE2X, GENE3X}}
				dataMatrix.add(geneLink);
			
	    	}
	    	
    	}
    	//Complete linkage: maximum pairwise distances
    	else if(method.contentEquals("Complete Linkage")){
    		
    		//go through all genes
	    	for(int i = 0; i < geneDList.size(); i++){
	    		
	    		//update progressbar
	    		pBar.setValue(i);
	    		
	    		//Local variables, initialized
	    		double max = 0;	
	    		int pos = 0;
	    		String[] distPair = new String[2];
	    		
	    		//getting the current gene
	    		List<Double> gene = geneDList.get(i);
	    		
	    		//set up a sorted array for the current gene
	    		List<Double>sortedGene = new ArrayList<Double>();
	    		
	    		//copy gene to sorted array
	    		sortedGene.addAll(gene);
	    		
	    		//sort the gene (min to max!)
	    		Collections.sort(sortedGene);
	    		
	    		//find maximum value in sorted array = longest distance to other gene (last value is always 0.0)
	    		//start at k = sortedGene.size() an move downwards k-- because sortedGene.get(k) needs to return the maximum value
	    		for(int k = sortedGene.size() - 1; k >= 0; k--){
	    			
	    			if(!linkValues.contains(sortedGene.get(k))){
	        			
	        			max = sortedGene.get(k);
	        			break;
	        		}
	    			
	    		}
	    		
	    		
	    		//find position of max in the current gene to figure out which the farthest gene is
	    		for(int j = 0; j < gene.size(); j++){
	    			
	    			if(gene.get(j) == max){
	    				
	    				pos = j;
	    				break;
	    			}
	    		}
	    		
	    		
	    		//add the minimum distance value of each gene to minValues
	    		linkValues.add(max);
	    		
	 
	    		
	    		//generate names and the pair of the current gene (i) and the gene closest to it (pos)
	    		if(type){
	    			
		    		distPair[0] = "GENE" + i + "X";
		    		distPair[1] = "GENE" + pos + "X";
		    		
	    		}
	    		else{
	    			
	    			distPair[0] = "ARRY" + i + "X";
		    		distPair[1] = "ARRY" + pos + "X";
	    		}
	    		
	    		//add the pair to a list
	    		genePairs.add(distPair);
	    		
	    	}
	    	
	//    	System.out.println("MaxValues Length Pre: " + linkValues.size());
	    	
	    	List<Double> orderedMax = new ArrayList<Double>(linkValues);
	    	Collections.sort(orderedMax);
	    	
	//----->//NEED TO CORRECT FOR INCREASING i and SHRINKING maxValues (they meet midway and so only half the data gets processed)
	    	for(int i = orderedMax.size() - 1; i >= 0 ; i--){  		
	    		
		    	//find least smallest distance in minValues
		    	//then associate the smallest distance with the appropriate gene pair
		    	String[] smallGene = new String[2];
		    	
		    	int k = linkValues.indexOf(orderedMax.get(i));
		    	smallGene = genePairs.get(k);
	
				//Give a Node name, add the node name as well as the gene pair (or cluster!) to array
				//if gene pair is such that one gene is already in a node, add node name
				List<String> geneLink = new ArrayList<String>();
				String newNode = "NODE" + (orderedMax.size()- i + 1) + "X";
				geneLink.add(newNode);
				 
				//Check all elements previously added (loop) to dataMatrix
				for(List<String> element : dataMatrix){
					
					//if any element contains a gene of the current checked pair (smallGene)
					//add the none-duplicate gene to geneLink
					if(element.contains(smallGene[0])){
						
						//Gene is replaced with node name of the element it contains
						smallGene[0] = element.get(0);
					}
					else if (element.contains(smallGene[1])){
						
						smallGene[1] = element.get(0);
					}
					
				}
				
				//Add gene names to geneLink
				//structure: {NODE1X, GENE2X, GENE3X}
				geneLink.add(smallGene[0]);
				geneLink.add(smallGene[1]);
				geneLink.add(Double.toString(orderedMax.get(i)));
				
				//add connection to dataMatrix 
				//structure: {{NODE1X, GENE2X, GENE3X}, ..., {NODE1X, GENE2X, GENE3X}}
				dataMatrix.add(geneLink);
			
	    	}
	    	
	    	System.out.println("Works!");
	    	
    	}
//---------------------------------------------------------------------------------------------------------------------------------------    	
    	//Average linkage uses the mean of pairwise distances 
    	else if(method.contentEquals("Average Linkage")){
    		
    		//variables for use:
    		//distance matrix (every gene and its distances to other genes): geneDList
    		//Progressbar: pBar
    		//row or column: type
    		//List for String arrays: genePairs
    		//List of String lists containing pair and node info: dataMatrix
    		
    		//list to make sure every loop step uses the next highest min value from geneDList
    		List<Double> usedMins = new ArrayList<Double>();
    		
    		//1 find shortest distance in entire original matrix
    		
    		//make deep copy of original geneDList
    		List<List<Double>> newDList = new ArrayList<List<Double>>();
    		
    		for(int i = 0; i < geneDList.size(); i++){
    			
    			List<Double> newList = new ArrayList<Double>();
    			newDList.add(newList);
    			
    			for(int j = 0; j < geneDList.size(); j++){
    				
    				Double e = new Double(geneDList.get(i).get(j));
    				newList.add(e);
    			}
    		}
    		
    		//to keep track of the clades and how genes are clustered, make a list of numbers, corresponding to gene names
    		//(123 = GENE123X) in which genes can be fused if they are being joint
    		List<List<Integer>> geneGroups = new ArrayList<List<Integer>>();
    		
    		//fill list
    		for(int i = 0; i < newDList.size(); i++){
    			
    			List<Integer> group = new ArrayList<Integer>();
    			group.add(i);
    			geneGroups.add(group);
    		}
    		
    		//geneGroups should now be as big as newDList, format {{0},{1},{2}...{x}}
    		System.out.println("Gene Groups: " + geneGroups.size());
    		
    		//CHANGE LIMIT!!
    		while(newDList.size() > 1435){
    		
	    		double min = 0;
	    		int row = 0;
	    		int column = 0;
	    		List<Double> geneMins = new ArrayList<Double>();
	    		String[] pair = new String[2];
	    		
	    		
	    		//the row value is just the position of the corresponding column value in columnValues
	    		List<Integer> columnValues = new ArrayList<Integer>();
	    		
	    		double trial = 0;
	    		
	    		//for every gene
	    		for(int j = 0; j < newDList.size(); j++){
	    			
	    			List<Double> gene = newDList.get(j);
	    			
	    			//take first non-zero value in gene
	    			for(Double element : gene){
	        			
	    				if(element != 0.0){
	        				
	        				trial = element;
	        				break;
	        			}
	    			}
	    			
	    			//make trial the minimum value of that gene
	    			//check whether min was used before (in whole matrix)
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
	    			
//		    		System.out.println("Trial Index: " + gene.indexOf(trial));
//		    		System.out.println("Gene: " + j);
	
	    		}
	    		
	    		//finds the row (gene) which has the minValue
	    		row = geneMins.indexOf(Collections.min(geneMins));
	    		
//	    		System.out.println("geneMins Minimum: " + Collections.min(geneMins));
	    		
	    		//finds the column by referring back to values added for each gene
	    		//knowing the gene with the minimum value (row) allows to find the corresponding column
	    		column = columnValues.get(row);
	    		
//	    		System.out.println("Row: " + row);
//	    		System.out.println("Col: " + column);
	    		
	    		//row and column value of the minimum distance value in matrix are now known
	    		min = newDList.get(row).get(column);
	    		
//	    		System.out.println("newDList: " + newDList.size());
	    		System.out.println("1 - Min: " + (1 - min));
	    		System.out.println("NewDList Element: " + newDList.get(10).size());
	    		
	    		usedMins.add(min);
	    		
	    		//now both genes must be joined, a new "matrix" must be created (actually new geneDList)
	    		
	    		//if min is in row A and column C, both row A and row C as well as column A and column C
	    		//are removed and fused into row AC and column AC later
	    		
	    		//check if any of the two genes (column, row) are already part of a joint clade in geneGroups
	    		//then remove THAT clade from newDList
	    		//but only after taking it to calculate means
	    		
	    		
	    		//now join the 2 genes and add the new joint clade as a row 
	    		//REMEMBER: length of tree branch would be Min/2 for each gene
	    		//first, create new List
	    		List<Double> newClade = new ArrayList<Double>();
	    		
	    		//fill list with the appropriate values (Average linkage = mean). 
	    		
	    		//get the 2 groups of genes
	    		List<Integer> rowGroup = geneGroups.get(row); //G
	    		List<Integer> colGroup = geneGroups.get(column); //B
	    		
	    		//the new geneGroup containing the all genes (BG)
	    		List<Integer> fusedGroup = new ArrayList<Integer>();
	    		fusedGroup.addAll(rowGroup);
	    		fusedGroup.addAll(colGroup); 
	    		
	    		System.out.println("Gene 1: " + geneGroups.get(row).toString());
	    		System.out.println("Gene 2: " + geneGroups.get(column).toString());
	    		
	    		geneGroups.remove(row);
	    		geneGroups.remove(column);
	    		
	    		geneGroups.add(Collections.min(fusedGroup), fusedGroup);
	    		
	    		//next compare the rowGroup to geneGroup column values for each gene in the rowGroup
	    		//find mean values to add to newClade (which is a new rowGroup)
	    		//place newClade in newDList, at the same index as fusedGroup in geneGroups
	    		//BG -->
	    		
	    		//remove old elements first
	    		newDList.remove(row);
	    		newDList.remove(column);
	    		
	    		for(List<Double> element : newDList){
	    			
	    			element.remove(column);
	    			element.remove(row);
	    		}
	    		
	    		
	    		//fill newClade with means
	    		//mean = 0.0 with itself
	    		//Error in here------------------------------------------------------------
	    		for(int i = 0; i < geneDList.get(0).size(); i++){
	    			
	    			double distanceSum = 0;
	    			double mean = 0;
	    			
	    			if(!fusedGroup.contains(i)){
	    			
		    			//select members of the new clade (B & G)	
			    		for(int j = 0; j < fusedGroup.size(); j++){
			    				
			    			int selectedGene = fusedGroup.get(j);
			    			double distanceVal = geneDList.get(selectedGene).get(i);
			    			distanceSum += distanceVal;
			    				
			    		}
			    		
			    		mean = distanceSum/fusedGroup.size();
		    			
		    			newClade.add(mean);
	    			}
	    		}
	    		
//	    		System.out.println("newClade: " + newClade.size());
	    	
	    		newDList.add(Collections.min(fusedGroup), newClade);
	    		
	    		System.out.println("newDList: " + newDList.size());
	    		
	    		for(List<Double> element : newDList){
	    			
	    			element.add(Collections.min(fusedGroup), newClade.get(newDList.indexOf(element)));
	    		}
	    		
    		}
    	}
    	
    	
    	
    	
    	else if(method.contentEquals("Centroid Linkage")){
    		
    		
    	}
    	
		//Test Output
		//System.out.println("Closest Gene Pair: " + Arrays.toString(smallGene));
//    	System.out.println("Gene Connections: " + genePairs.size());
//    	
    	System.out.println("dataMatrix Size: " + dataMatrix.size());
    	
    	ClusterFileWriter dataFile = new ClusterFileWriter(frame, model);

    		dataFile.writeFile(dataMatrix, type);
    		filePath = dataFile.getFilePath();
    	
    }
    
    public String getFilePath(){
    	
    	return filePath;
    }
    
    
    //method to join all gene arrays into one double[] 
    public static <Double> double[] concatAll(double[] first, List<double[]> rest) {
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
