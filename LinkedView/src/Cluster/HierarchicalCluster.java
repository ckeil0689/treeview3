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
	
	IntHeaderInfoCluster headerInfo = new IntHeaderInfoCluster();
	
	//Constructor (building the object)
	public HierarchicalCluster(ClusterModel model, double[] dataArray, int method, String similarity, JFrame currentFrame){
		
		this.model = model;
		this.dataArray = dataArray;
		this.similarity = similarity;
		this.method = method;
		this.frame = currentFrame;
	}
	
	//Step 1: Create Similarity matrices (one for genes, one for samples) according to all 
	//the different measurement options (correlation, Euclidean distance etc.)
	//differentiate between gene and array cluster!
	public List <List<Double>> splitList(List<Double> list, JProgressBar pBar){
		
		int lower = 0;
		int upper = 0;
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
	
	
	
	
//	//gene similarity matrix, euclidean distance
//	//first get an array of arrays for all genes (rows)
//	public double[] getRow(int rowN){
//		
//		return dataArray;
//	}
//	
//	
//	//get a certain value from the datamatrix
//    public double getValue(int x, int y) {
//		int nexpr = model.nExpr();//columns
//		int ngene = model.nGene();//rows
//		if ((x < nexpr) && (y < ngene) && (x >= 0) && (y >= 0)) {
//			return dataArray[x + y * nexpr];
//		} else {
//			return DataModel.NODATA;
//		}
//	}
    
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
    	
    	System.out.println("FullList Length: " + fullList.size());
    	
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
    	
    	
    	System.out.println("GeneDL Length: " + geneDistanceList.size());
    	System.out.println("GeneList Element Length: " + geneDistanceList.get(0).size());
    	
    	//Fusing all genes and their distances together to one array
    	//double[] fusedArray = concatAll(geneDistanceList.get(0),geneDistanceList.subList(1, geneDistanceList.size()));
   
    	//System.out.println("Fused Array Length: " + fusedArray.length);
    	
    	return geneDistanceList;
    }
	
    public void cluster(List<List<Double>> geneDList, JProgressBar pBar){
		
    	//CDT file from Cluster 3.0 adds one column with gene tags according to following rule: 
    	//(GENE) + (gene number in original dataset starting at 0) + (X) --> Example: First Gene: GENE0X
    	//Goal of clustering: agglomerative (top-to-bottom)
    	
    	List<String[]> genePairs = new ArrayList<String[]>();
    	List<Double> minValues = new ArrayList<Double>();
    	List<List<String>> gtrMatrix = new ArrayList<List<String>>();
    	
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
    		
    		//find first none-zero value in sorted array = shortest distance to other gene
    		//set min to this value
    		min = sortedGene.get(1);
    		
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
    		distPair[0] = "GENE" + i + "X";
    		distPair[1] = "GENE" + pos + "X";
    		
    		//add the pair to a list
    		genePairs.add(distPair);
    		
    	}
    	
    	System.out.println("GenePairs Length Pre: " + genePairs.size());
    	System.out.println("MinValues Length Pre: " + minValues.size());
    	
    	List<Double> orderedMin = new ArrayList<Double>(minValues);
    	Collections.sort(orderedMin);
    	
//    	System.out.println("OrderedMin: " + orderedMin);
//    	System.out.println("MinValues: " + minValues);
    	
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
			
			//Check all elements already added to gtrList
			for(List<String> element : gtrMatrix){
				
				//if any element contains a gene of the current checked pair (smallGene)
				//add the none-duplicate gene to geneLink
				//in case it contains both genes already
				if(element.contains(smallGene[0])){
					
					//Gene is replaced with node name of the element it contains
					smallGene[0] = element.get(0);
				}
				else if (element.contains(smallGene[1])){
					
					smallGene[1] = element.get(0);
				}
				//in case it contains none dont change smallGene
				else{
					
				}
			}
			
			//Add names to geneLink
			geneLink.add(smallGene[0]);
			geneLink.add(smallGene[1]);
			
			//add connection to gtrMatrix
			gtrMatrix.add(geneLink);
		
    	}
    	
    	
		//Test Output
		//System.out.println("Closest Gene Pair: " + Arrays.toString(smallGene));
    	System.out.println("Gene Connections: " + genePairs.size());
    	
    	System.out.println("GTR Matrix Size: " + gtrMatrix.size());
    	
    	ClusterFileWriter gtrFile = new ClusterFileWriter(frame);
    	
    	gtrFile.writeGTRFile(gtrMatrix);
    	
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
