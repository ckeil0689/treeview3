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
	private List<List<Double>> halfDMatrix;
	
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
    	
    	//ProgressBar maximum
    	pBar.setMaximum(dMatrix.size());
    	
		//deep copy of distance matrix to avoid mutation
		setHalfDMatrix(deepCopy(dMatrix));
		
		//find random clusterN amount of seed clusters
		List<List<Integer>> seedList = new ArrayList<List<Integer>>();
		
		seedList = initialize();
		
		System.out.println(seedList.size());
    }
    
    //Cluster support methods
    public List<List<Integer>> initialize() {
    	
    	List<List<Integer>> seedList = new ArrayList<List<Integer>>();
    	
    	int size = halfDMatrix.size();

    	for(int i = 0; i < clusterN; i++) {
    		
    		List<Integer> distancePair = new ArrayList<Integer>();
    		
    		int randomIndex1 = new Random().nextInt(size); 
    		int elementSize = halfDMatrix.get(randomIndex1).size();
    		int randomIndex2 = new Random().nextInt(elementSize); 
    		
    		distancePair.add(randomIndex1);
    		distancePair.add(randomIndex2);
    		
    		//Needs a check whether the same pair is already contained
    		//in the list
    		seedList.add(distancePair);
    	}
    	
    	return seedList;
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
    
    //Getters
    public List<List<Double>> getHalfDMatrix() {
    	
    	return halfDMatrix;
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
    
    //Setters
    public void setHalfDMatrix(List<List<Double>> matrix) {
    	
    	halfDMatrix = matrix;
    }
}
