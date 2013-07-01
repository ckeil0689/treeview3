package Cluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import edu.stanford.genetics.treeview.DataModel;

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
	//Mutators (set...)
    
	//Euclidean Distance
    public List <double[]> euclid(List<double []> fullArray){
    	
    	//Local variables
    	//list with all genes and their distances to all other genes
    	List <double[]> geneDistanceList = new ArrayList<double[]>();
    	
    	//take a gene
    	for(int i = 0; i < 3; i++){//fullArray.size()
    		
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
    			
    		}
    		
    		System.out.println("GeneDist Size: " + geneDistance.length);
    		System.out.println("GeneDist Gene 0: " + geneDistance[0]);
    		System.out.println("GeneDist Gene 1: " + geneDistance[1]);
    		System.out.println("GeneDist Gene 2: " + geneDistance[2]);
    		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    		
    		//Now add the gene with all its distances to other genes to the geneDistanceList so that we get
    		//a list with all genes and their distances to the other genes
    		
    		
    		geneDistanceList.add(geneDistance);
    		
    	}
    	
    	System.out.println("GeneDL Length: " + geneDistanceList.size());
    	System.out.println("GeneDL Element 0: " + Arrays.toString(geneDistanceList.get(0)));
    	
    	return geneDistanceList;
    }
}
