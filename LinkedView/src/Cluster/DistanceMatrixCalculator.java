package Cluster;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JProgressBar;

/**
 * This class is used to calculate a distance matrix based on input data.
 * It contains several different methods and will return a matrix of distances beween row or column elements,
 * using the parameters which the user has chosen.
 * @author CKeil
 *
 */
public class DistanceMatrixCalculator {
	
	//Instance variables
	//list with all genes and their distances to all other genes (1455x1455 for sample data)
	private List <List<Double>> distanceList = new ArrayList<List<Double>>();
	private List<List<Double>> fullList;
	private JProgressBar pBar;
	private String choice;
	
	//Constructor
	public DistanceMatrixCalculator (List<List<Double>> fullList, String choice, JProgressBar pBar){
		
		this.fullList = fullList;
		this.pBar = pBar;
		this.choice = choice;
	}
	
	//Methods to calculate distance matrix
	/**
	 * This method generates a distance list based on the Pearson correlation. The parameters allow for selection of different
	 * versions of the Pearson correlation (absolute Pearson correlation, centered vs. uncentered). 
	 * @param fullList
	 * @param absolute
	 * @param centered
	 * @return List<List<Double> distance matrix
	 */
	public void pearson(boolean absolute, boolean centered){
    	
    	pBar.setMaximum(fullList.size());
    	
    	//clearing the distanceList in case the function is somehow called after using another
    	//distance measure and the object wasn't dumped before
    	distanceList.clear();
    	
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
    			
    			//local variables
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
    			double pearson1 = 0;
    			double rootProduct = 0;
    			double finalVal = 0;
    			BigDecimal pearson2;
    			
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
    }

	//Euclidean Distance
    public void euclid(){
		
    	//local variables
    	double sDist = 0;
    	double g1 = 0;
    	double g2 = 0;
    	double gDiff = 0;
    	
    	pBar.setMaximum(fullList.size());
    	
    	//clearing the distanceList in case the function is somehow called after using another
    	//distance measure and the object wasn't dumped before
    	distanceList.clear();
    	
    	//take a gene
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
    			
//	    			double rootedSum = 0;
//	    			rootedSum = Math.sqrt(sum);
//	    			
//	    			//Mathematically RIGHT? Not used in Cluster 3.0 but Euclidean Distance is caalculated this way
//	    			geneDistance[j] = rootedSum;
    			
    			double divSum = 0;
    			divSum = sum/fullList.size();
    			
    			geneDistance.add(divSum);
    		}
    		
    		//System.out.println("#1 Loop Time: " + (System.currentTimeMillis()-ms));

    		//list with all genes and their distances to the other genes
    		distanceList.add(geneDistance);
    	}
    }
    
    public void cityBlock(){

    	//local variables
    	double g1 = 0;
    	double g2 = 0;
    	double gDiff = 0;
    	
    	pBar.setMaximum(fullList.size());
    	
    	//clearing the distanceList in case the function is somehow called after using another
    	//distance measure and the object wasn't dumped before
    	distanceList.clear();
    	
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
    		
    		//System.out.println("#1 Loop Time: " + (System.currentTimeMillis()- ms));

    		//list with all genes and their distances to the other genes
    		distanceList.add(dataDistance);
    	}
    }
    
	public void measureDistance(){
		
		switch(choice){
		
			case "Pearson Correlation (uncentered)": pearson(false, false);
			break;
			
			case "Pearson Correlation (centered)": pearson(false, true);
			break;
			
			case "Absolute Correlation (uncentered)": pearson(true, false);
			break;
			
			case "Absolute Correlation (centered)": pearson(true, true);
			break;
			
			case "Euclidean Distance": euclid();
			break;
			
			case "City Block Distance": cityBlock();
			break;
			
			default: break;
		}
	}
    
    //Accessor method to retrieve the distance matrix
    public List<List<Double>> getDistanceMatrix(){
    	
    	return distanceList;
    }
}
