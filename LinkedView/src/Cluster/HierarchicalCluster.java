package Cluster;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import Cluster.ClusterLoader.TimerListener;

import net.miginfocom.swing.MigLayout;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LoadProgress2;
import edu.stanford.genetics.treeview.SwingWorker;

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
	int method;
	String similarity;
	
	IntHeaderInfoCluster headerInfo = new IntHeaderInfoCluster();
	
	//Constructor (building the object)
	public HierarchicalCluster(ClusterModel model, double[] dataArray, int method, String similarity){
		
		this.model = model;
		this.dataArray = dataArray;
		this.similarity = similarity;
		this.method = method;
	}
	
	//Step 1: Create Similarity matrices (one for genes, one for samples) according to all 
	//the different measurement options (correlation, Euclidean distance etc.)
	//differentiate between gene and array cluster!
	public List <double[]> splitArray(double[] array){
		
		int lower = 0;
		int upper = 0;
		int max = model.nExpr();
		
		List<double[]> geneList = new ArrayList<double[]>();
		
		System.out.println("EventDispatch SplitArray: " + javax.swing.SwingUtilities.isEventDispatchThread());
		
		for(int i = 0; i < array.length/max; i++){
			
			upper+=max;
			
			geneList.add(Arrays.copyOfRange(array, lower, upper));
			
			lower = upper;
			
		}
		
		if(upper < array.length -1){
			
			lower = upper;
			upper = array.length;
			
			geneList.add(Arrays.copyOfRange(array, lower, upper));
		}
		
		return geneList;
	}
	
	
	
	
	//gene similarity matrix, euclidean distance
	//first get an array of arrays for all genes (rows)
	public double[] getRow(int rowN){
		return dataArray;
//		dataArray
	}
	
	//Accessors (get...)
	
	//get a certain value from the datamatrix
    public double getValue(int x, int y) {
		int nexpr = model.nExpr();//columns
		int ngene = model.nGene();//rows
		if ((x < nexpr) && (y < ngene) && (x >= 0) && (y >= 0)) {
			return dataArray[x + y * nexpr];
		} else {
			return DataModel.NODATA;
		}
	}
    
	//Euclidean Distance
    public List <double[]> euclid(List<double []> fullArray, JProgressBar pBar){
    	
		System.out.println("EventDispatch Euclid: " + javax.swing.SwingUtilities.isEventDispatchThread());
    	//Local variables
    	//list with all genes and their distances to all other genes
    	List <double[]> geneDistanceList = new ArrayList<double[]>();
    	
    	pBar.setMaximum(fullArray.size());
    	
    	//take a gene
    	for(int i = 0; i < fullArray.size(); i++){//fullArray.size()
    		
    		pBar.setValue(i);
    		//distances of one gene to all others
    		double[] geneDistance = new double[fullArray.size()];
    		double[] gene = fullArray.get(i);
    		
    		//choose a gene for distance comparison
    		for(int j = 0; j < fullArray.size(); j++){
    			
    			double[] gene2 = fullArray.get(j);
    			
    			//squared differences between elements of 2 genes
    			List<Double> sDiff= new ArrayList<Double>();
    			
    			//compare each value of both genes
    			for(int k = 0; k < gene.length; k++){
    				
    				double sDist = Math.pow((gene[k] - gene2[k]),2);
    				sDiff.add(sDist);
    			}
    			
    			
    			//sum all the squared value distances up
    			//--> get distance between gene and gene2
    			double sum = 0;
    			
    			for(double element : sDiff){
    				sum += element;
    			}
    			
    			double rootedSum = 0;
    			rootedSum = Math.sqrt(sum);
    			
    			//NOT RIGHT
    			geneDistance[j] = rootedSum;
    			
//    			double divSum = 0;
//    			divSum = sum/fullArray.size();
//    			
//    			geneDistance[j] = divSum;
    			
    		}
    		
//    		System.out.println("GeneDist Size: " + geneDistance.length);
//    		System.out.println("GeneDist Gene 0: " + geneDistance[0]);
//    		System.out.println("GeneDist Gene 1: " + geneDistance[1]);
//    		System.out.println("GeneDist Gene 2: " + geneDistance[2]);
//    		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    		
    		//Now add the gene with all its distances to other genes to the geneDistanceList so that we get
    		//a list with all genes and their distances to the other genes
    		
    		
    		geneDistanceList.add(geneDistance);
    		
    	}
    	
    	System.out.println("GeneDL Length: " + geneDistanceList.size());
    	System.out.println("GeneDL Element 0: " + Arrays.toString(geneDistanceList.get(0)));
    	System.out.println("GeneList Element Length: " + geneDistanceList.get(0).length);
    	
    	double[] fusedArray = concatAll(geneDistanceList.get(0),geneDistanceList);
    	
    	System.out.println("Fused Array Length: " + fusedArray.length);
    	
    	return geneDistanceList;
    }
	
    
    public void cluster(List<double[]> geneDList, JProgressBar pBar){
    	
		System.out.println("EventDispatch Cluster: " + javax.swing.SwingUtilities.isEventDispatchThread());
    	//CDT file from Cluster 3.0 adds one column with gene tags according to following rule: 
    	//(GENE) + (gene number in original dataset starting at 0) + (X) --> Example: First Gene: GENE0X
    	//Goal of clustering: agglomerative (top-to-bottom)
    	//first: find closest gene of each gene
    	List<String[]> genePairs = new ArrayList<String[]>();
    	List<Double> minValues = new ArrayList<Double>();

    	pBar.setMaximum(geneDList.size());
    	
    	for(int i = 0; i < geneDList.size(); i++){
    		
    		pBar.setValue(i);
    		double min = 0;	
    		int pos = 0;
    		String[] distPair = new String[2];
    		
    		double[] gene = geneDList.get(i);
    		double[]sortedGene = new double[gene.length];
    		
    		Double[] gene2 = convert(gene);
    		
    		System.arraycopy(gene, 0, sortedGene, 0, gene.length);
    		Arrays.sort(sortedGene);
    		
    		for(int k = 0; k < sortedGene.length; k++){
    			
    			if(sortedGene[k] > 0.0){
    				
    				min = sortedGene[k];
    				break;
    			}
    		}
    		
    		for(int j = 0; j < gene.length; j++){
    			
    			if(gene[j] == min){
    				
    				pos = j;
//    				System.out.println("Distance: " + gene[j]);
//    				System.out.println("Min: " + min);
//    				System.out.println("Position: " + j);
    				break;
    			}
    		}
    		
    		minValues.add(min);
    		distPair[0] = "GENE" + i + "X";
    		distPair[1] = "GENE" + pos + "X";
    		
    		genePairs.add(distPair);
    		
    	}
    	
    	//find least distant genes
    	String[] smallGene = new String[1];
		for(int k = 0; k < minValues.size(); k++){
			
			if(minValues.get(k) == Collections.min(minValues)){
				
				smallGene = genePairs.get(k);
				break;
			}	
		}
		
		
		
		System.out.println("Closest Gene Pair: " + smallGene);
    	System.out.println("Gene Connections: "+ genePairs.size());
    	System.out.println("Gene Pair Sample: "+ Arrays.toString(genePairs.get(0)));
    	System.out.println("Gene Pair Sample: "+ Arrays.toString(genePairs.get(1)));
    	System.out.println("Gene Pair Sample: "+ Arrays.toString(genePairs.get(2)));
    	System.out.println("Gene Pair Sample: "+ Arrays.toString(genePairs.get(3)));
    	System.out.println("MinVal Sample: "+ minValues);
    	
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
    
	class TimerListener implements ActionListener { // manages the FileLoader
		// this method is invoked every few hundred ms
		public void actionPerformed(ActionEvent evt) {
		
			//Toolkit.getDefaultToolkit().beep();

		}
	}
}
