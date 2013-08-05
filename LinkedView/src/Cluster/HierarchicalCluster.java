package Cluster;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
	public HierarchicalCluster(ClusterModel model, double[] dataArray, int method, String similarity, JFrame currentFrame){
		
		this.model = model;
		this.dataArray = dataArray;
		this.similarity = similarity;
		this.method = method;
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
    			double pearson = 0;
    			double finalVal = 0;
    			double rootProduct;
    			
    			List<Double> data2 = fullList.get(j);
    			
    			if(centered){
    				
        			for(double x : data){
        				
        				mean_sumX += x;
        			}
        			
        			mean_x = mean_sumX/data.size();
        			
        			for(double y : data2){
        				
        				mean_sumY += y;
        			}
        			
        			mean_y = mean_sumY/data2.size();
    			}
    			else{
    				
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
    			
    			pearson = 1 - finalVal;
    			
	    		pearsonList.add(pearson);
	    		
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
	
    public void cluster(List<List<Double>> geneDList, JProgressBar pBar, boolean type){
		
    	//list of genes and their closest partner gene (according to distance measure)
    	//Structure: {{g1, g2},...,{gX, gY}}
    	List<String[]> genePairs = new ArrayList<String[]>();
    	
    	//list of all minimum distances
    	//Structure: {d1, d2, d3,...,dX}
    	List<Double> minValues = new ArrayList<Double>();
    	
    	//
    	List<List<String>> dataMatrix = new ArrayList<List<String>>();
    	
    	//progressbar maximum = amount of genes
    	pBar.setMaximum(geneDList.size());
    	
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
    		if(minValues.contains(sortedGene.get(1))){
    			
    			min = sortedGene.get(2);
    		}
    		else{
    			
    			min = sortedGene.get(1);
    		}
    		
    		//find position of min in the current gene to figure out which the closest gene is
    		for(int j = 0; j < gene.size(); j++){
    			
    			if(gene.get(j) == min){
    				
    				pos = j;
    				break;
    			}
    		}
    		
    		//add the minimum distance value of each gene to minValues
    		minValues.add(min);
    		
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
    	
    	System.out.println("GenePair: " + Arrays.toString(genePairs.get(1165)));
//    	System.out.println("MinValues Length Pre: " + minValues.size());
    	
    	List<Double> orderedMin = new ArrayList<Double>(minValues);
    	Collections.sort(orderedMin);
    	
    	System.out.println("Ordered Min: " + orderedMin.toString());
    	
//----->//NEED TO CORRECT FOR INCREASING i and SHRINKING minValues (they meet midway and so only half the data gets processed)
    	for(int i = 0; i < orderedMin.size() ; i++){  		
    		
	    	//find least smallest distance in minValues
	    	//then associate the smallest distance with the appropriate gene pair
	    	String[] smallGene = new String[1];
	    	
	    	int k = minValues.indexOf(orderedMin.get(i));
	    	smallGene = genePairs.get(k);

			//Give a Node name, add the node name as well as the gene pair (or cluster!) to array
			//if gene pair is such that one gene is already in a node, add node name
			List<String> geneLink = new ArrayList<String>();
			String newNode = "NODE" + (i+1) + "X";
			geneLink.add(newNode);
			 
			//Check all elements previously added (loop) to dataMatrix
//			for(List<String> element : dataMatrix){
//				
//				//if any element contains a gene of the current checked pair (smallGene)
//				//add the none-duplicate gene to geneLink
//				if(element.contains(smallGene[0])){
//					
//					//Gene is replaced with node name of the element it contains
//					smallGene[0] = element.get(0);
//				}
//				else if (element.contains(smallGene[1])){
//					
//					smallGene[1] = element.get(0);
//				}
//				//in case it contains none dont change smallGene
//				else if(element.contains(smallGene[0]) && element.contains(smallGene[1])){
//					
//					
//				}
//			}
			
			//Add gene names to geneLink
			//structure: {NODE1X, GENE2X, GENE3X}
			geneLink.add(smallGene[0]);
			geneLink.add(smallGene[1]);
			
			//add connection to dataMatrix 
			//structure: {{NODE1X, GENE2X, GENE3X}, ..., {NODE1X, GENE2X, GENE3X}}
			dataMatrix.add(geneLink);
		
    	}
    	
		//Test Output
		//System.out.println("Closest Gene Pair: " + Arrays.toString(smallGene));
//    	System.out.println("Gene Connections: " + genePairs.size());
//    	
//    	System.out.println("GTR Matrix Size: " + dataMatrix.size());
    	
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
