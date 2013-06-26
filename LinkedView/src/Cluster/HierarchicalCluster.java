package Cluster;

import java.util.ArrayList;
import java.util.Arrays;
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
    
	//
}
