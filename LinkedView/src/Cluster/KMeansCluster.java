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
	private List<Double> seedMeans = new ArrayList<Double>();
	
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
		List<List<Double>> clusterMeans = new ArrayList<List<Double>>();
		
    	//ProgressBar maximum
    	pBar.setMaximum(dMatrix.size());
    	
		//deep copy of distance matrix to avoid mutation
		copyDMatrix = deepCopy(dMatrix);
		
		//Make a list of all means of distances for every gene
		elementMeanList = generateMeans(copyDMatrix);
		
		//List of seeds should be of clusterN-size
		setSeeds(clusterN, elementMeanList);
		
		//Use the seedClusters to define Voronoi spaces. This means, assign
		//the distance mean of each gene to the closest seed cluster.
		clusters = assignMeansVals(elementMeanList);
		clusterMeans = indexesToMeans(elementMeanList, clusters);
		
		//Begin iteration of recalculating means and reassigning gene
		//distance means to clusters
		for(int i = 0; i < iterations; i++) {
			
			findNewMeans(clusterMeans);
			
			clusters = assignMeansVals(elementMeanList);
			clusterMeans = indexesToMeans(elementMeanList, clusters);
		}
		
		//Next Step: Generate CDT and KGG Excel file
		List<List<String>> geneGroups = new ArrayList<List<String>>();
		
		geneGroups = indexToString(clusters);
		
		List<List<String>> clusterData = new ArrayList<List<String>>();
		
		List<String> initial = new ArrayList<String>();
		
		String fileEnd;
		
		if(type.equalsIgnoreCase("Gene")) {
			fileEnd = "_K_G" + clusterN + ".kgg";
			initial.add("ORF");
			
		} else {
			fileEnd = "_K_A" + clusterN + ".kag";
			initial.add("ARRAY");
		}
		
		initial.add("GROUP");
		clusterData.add(initial);
		
		//Prepare data for writing
		for(List<String> group : geneGroups) {
			
			reorderedList.addAll(group);
			
			for(String element : group) {
				
				List<String> dataPair = new ArrayList<String>();
				String index = Integer.toString(geneGroups.indexOf(group));
				
				dataPair.add(element);
				dataPair.add(index);
				
				clusterData.add(dataPair);
			}
		}
		
		ClusterFileWriter fw = new ClusterFileWriter(model);
		
		fw.writeFile(clusterData, fileEnd);
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
    public void setSeeds(int seedNumber, List<Double> meanList) {
    	
    	List<List<Integer>> seedClusters = new ArrayList<List<Integer>>();
    	List<Integer> indexList = new ArrayList<Integer>();
    	
    	for(int i = 0; i < seedNumber; i++) {
    		
    		List<Integer> cluster = new ArrayList<Integer>();
    		
    		int seedIndex = new Random().nextInt(meanList.size());
    		
    		//Making sure to exclude duplicates
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
    	
    	setSeedMeans(seedClusters, meanList);
    }
    
    /**
     * This method assigns all the means from the distance matrix to 
     * the Voronoi space of a set of previously chosen means.
     * Each mean is assigned to the closest 'seed mean'.
     * 
     * @param meanList
     * @param seedClusters
     * @return
     */
    public List<List<Integer>> assignMeansVals(List<Double> meanList) {
    	
    	pBar.setValue(0);
    	
    	List<List<Integer>> clusters = new ArrayList<List<Integer>>();
    	
    	//fill clusters List with clusterN-amount of empty lists to 
    	//avoid duplicates of the initial means later
    	for(int i = 0; i < clusterN; i++) {
    		
    		List<Integer> group = new ArrayList<Integer>();
    		
    		clusters.add(group);
    	}
    	
    	//seedCluster has clusterN-amount of Integers designating the
    	//initial means
    	List<Double> initialMeans = getSeedMeans();
    	
    	//Compare each mean from meanList to all seed means from seedClusters
    	//and assign the means to the according group.
    	for(Double mean : meanList) {
    		
    		int geneID = meanList.indexOf(mean);

    		pBar.setValue(geneID);
    		
    		List<Integer> meanIndexes = new ArrayList<Integer>();
    		
    		double bestD = Double.MAX_VALUE;
    		double distance = 0;
    		
    		for(Double seed : initialMeans) {
    		
    			distance = Math.abs(seed - mean);
    			
    			if(distance < bestD) {
    				
    				bestD = distance;
    				meanIndexes.add(initialMeans.indexOf(seed));
    			}
    		}
    		
    		int bestCluster = meanIndexes.get(meanIndexes.size() - 1);
    		
    		//Add the current gene to appropriate cluster group
    		clusters.get(bestCluster).add(geneID);
    	}
    	
    	//Check if sum of group sizes match the amount of overall means
    	//for testing only
    	int sum = 0;
    	for(List<Integer> group : clusters) {
    		
    		sum += group.size();
    		System.out.println("Group Size " + clusters.indexOf(group) 
    				+ ": " + group.size());
    	}
    	
    	if(sum == meanList.size()) {
    		System.out.println("Success! sum and meanList size match up.");
    		System.out.println("Seed Means: " + getSeedMeans());
    		
    	} else {
    		System.out.println("Something's weird.");
    		System.out.println("Sum: " + sum);
    		System.out.println("MeanList Size: " + meanList.size());
    	}
    	
    	return clusters;
    }
    
    /**
     * Transforms the clusters of gene indexes to clusters of gene distance
     * means in order to use the resulting list for calculation.
     * @param meanList
     * @param clusters
     * @return
     */
    public List<List<Double>> indexesToMeans(List<Double> meanList, 
    		List<List<Integer>> clusters) {
    	
    	List<List<Double>> clusterMeans = new ArrayList<List<Double>>();
    	
    	for(List<Integer> group : clusters) {
    		
    		List<Double> meanCluster = new ArrayList<Double>();
    		
    		for(int gene : group) {
    			
    			double mean = meanList.get(gene);
    			meanCluster.add(mean);
    		}
    		
    		clusterMeans.add(meanCluster);
    	}
    	
    	return clusterMeans;
    }
    
    /**
     * Finding new adjusted means for each cluster for later reassignment of
     * all gene distance means which will create more accurate Voronoi spaces.
     * @param clusterMeans
     * @return
     */
    public void findNewMeans(List<List<Double>> clusterMeans) {
    	
    	List<Double> newSeedMeans = new ArrayList<Double>();
    	
    	//find new mean of each cluster
    	for(List<Double> group : clusterMeans) {
    		
    		double sum = 0;
    		double newMean = 0;
    		for(Double mean : group) {
    			
    			sum += mean;
    		}
    		
    		newMean = sum/group.size();
    		
    		newSeedMeans.add(newMean);    		
    	}
    	
    	setSeedMeans(newSeedMeans);
    }
    
    /**
     * This method uses the list of clusters composed of gene indexes to find
     * the appropriate ORF names from the loaded model's headers.
     * @param clusters
     * @return
     */
    public List<List<String>> indexToString(List<List<Integer>> clusters) {
    	
    	List<List<String>> stringClusters = new ArrayList<List<String>>();
    	
    	String[][] geneNameArray;
    	
    	List<String> geneNameList = new ArrayList<String>();
    	
    	//Get the right header array
    	if(getType().equalsIgnoreCase("Arry")) {	
    		geneNameArray = model.getArrayHeaderInfo().getHeaderArray();
    		
    	} else {
    		geneNameArray = model.getGeneHeaderInfo().getHeaderArray();
    	}
    	
    	for(String[] element : geneNameArray) {
    		
    		geneNameList.add(element[0]);
    	}
    	
    	for(List<Integer> group : clusters) {
    		
    		List<String> geneNames = new ArrayList<String>();
    		
    		for(int mean : group) {
    			
    			String gene = geneNameList.get(mean);
    			geneNames.add(gene);
    		}
    		
    		stringClusters.add(geneNames);
    	}
    	
    	return stringClusters;
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
    

    //Setters
    /**
     * Setter for the seed means for every cluster.
     * @param clusters
     * @param elementMeanList
     */
    public void setSeedMeans(List<Double> newSeedMeans) {

    	seedMeans = newSeedMeans;
    }
    
    /**
     * Setter for the seed means for every cluster.
     * @param clusters
     * @param elementMeanList
     */
    public void setSeedMeans(List<List<Integer>> clusters, 
    		List<Double> elementMeanList) {
    	
    	seedMeans.clear();
    	
    	for(List<Integer> group : clusters) {
    		
    		double seedMean = elementMeanList.get(group.get(0));
    		seedMeans.add(seedMean);
    	}
    }
    
    
    //Getters
    /**
     * Accessor for the reordered list
     * @return
     */
    public List<String> getReorderedList(){
    	
    	return reorderedList;
    }
    
    /**
     * Accessor for the seed means
     * @return
     */
    public List<Double> getSeedMeans(){
    	
    	return seedMeans;
    }
    
    /**
     * Accessor for the file path
     * @return
     */
    public String getFilePath(){
    	
    	return filePath;
    }
    
    /**
     * Accessor for the type of the current object instance
     * @return
     */
    public String getType() {
    	
    	return type;
    }
}
