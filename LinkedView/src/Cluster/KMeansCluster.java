package Cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JProgressBar;

public class KMeansCluster {

	//Instance variables
	private ClusterModel model;
	private String filePath;
	private JProgressBar pBar;
	private int clusterN = 0;
	private int iterations = 0;
	private String type = "";
	
	//Distance Matrix
	private List<List<Double>> dMatrix = new ArrayList<List<Double>>();
	
	//Half of the Distance Matrix (symmetry)
	private List<List<Double>> copyDMatrix;
	
	//list to return ordered GENE numbers for .cdt creation
	private List<Integer> clusterIndexList = new ArrayList<Integer>();
	
	//list to return ordered GENE numbers for .cdt creation
	private List<String> reorderedList = new ArrayList<String>();
	
	/**
	 * Main constructor
	 * @param model
	 * @param dMatrix
	 * @param pBar
	 * @param type
	 * @param method
	 */
	public KMeansCluster(ClusterModel model, List<List<Double>> dMatrix, 
			JProgressBar pBar, int clusterN, int iterations, String type) {
		
		this.model = model;
		this.dMatrix = dMatrix;
		this.pBar = pBar;
		this.clusterN = clusterN;
		this.iterations = iterations;
		this.type = type;
	}
	
	//method for clustering the distance matrix
    public void cluster() {
    	
    	List<Double> elementMeanList = new ArrayList<Double>();
		List<List<Integer>> clusters = new ArrayList<List<Integer>>();
		
    	//ProgressBar maximum
    	pBar.setMaximum(dMatrix.size());
    	
		//deep copy of distance matrix to avoid mutation
		copyDMatrix = deepCopy(dMatrix);
		
		//Make a list of all means of distances for every gene
		elementMeanList = generateMeans(copyDMatrix);
		
		clusters = setSeeds(elementMeanList, clusterN);

    }
    
    //Cluster support methods
    /**
     * Method to find the mean distances for every row element in 
     * the distance matrix.
     * @param matrix
     * @return
     */
    public List<Double> generateMeans(List<List<Double>> matrix) {
    	
    	List<Double> meanList = new ArrayList<Double>();
    	
    	for(List<Double> row : matrix) {
    		
    		double sum = 0;
    		double mean;
    		
    		for(Double d : row) {
    			
    			sum += d;
    		}
    		
    		mean = sum/row.size();
    		
    		meanList.add(mean);
    	}
    	
    	return meanList;
    }
    
    /**
     * Find random seed clusters to begin assigning distance mean to.
     * The number of seed clusters is user-specified and designated as 
     * "clusterN" in this class.
     * @param meanList
     * @return
     */
    public List<List<Integer>> setSeeds(List<Double> meanList, 
    		int seedNumber) {
    	
    	List<List<Integer>> seedClusters = new ArrayList<List<Integer>>();
    	List<Integer> indexList = new ArrayList<Integer>();
    	
    	for(int i = 0; i < seedNumber; i++) {
    		
    		List<Integer> cluster = new ArrayList<Integer>();
    		
    		int seedIndex = new Random().nextInt(meanList.size());
    		
    		boolean same = false;
    		for(int e : indexList) {
    			
    			if(e == seedIndex) {
    				
    				i = i - 1;
    				same = true;
    				break;
    			}
    		}
    		
    		if(!same) {	
        		indexList.add(seedIndex);
        		cluster.add(seedIndex);
            	seedClusters.add(cluster);
    		}
    	}
    	
    	return seedClusters;
    }
    
    //Other support methods
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
