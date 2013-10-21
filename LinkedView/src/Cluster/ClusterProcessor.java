package Cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JPanel;
import javax.swing.JProgressBar;

//import Cluster.ClusterView.FinalOptionsPanel;

/**
 * This class takes the original uploaded dataArray passed 
 * in the constructor and manipulates it according to mathematical 
 * principles of hierarchical clustering. It generates files to display 
 * dendrograms (.gtr and .atr) as well as a reordered original data file (.cdt)
 * @author CKeil
 *
 */
public class ClusterProcessor {
	
	//Instance variables
	private ClusterModel model;
	private double[] currentArray;
	private final String rowString = "GENE"; 
	private final String colString = "ARRY";
	private String choice;
	private String choice2;
	private String similarityM;
	private int row_clusterN;
	private int row_iterations;
	private int col_clusterN;
	private int col_iterations;

	//GUI Components
	private FinalOptionsPanel finalPanel;
	private JPanel mainPanel;
	private JProgressBar pBar; 
	private JProgressBar pBar2;
	private JProgressBar pBar3; 
	private JProgressBar pBar4;
	
	/**
	 * Main constructor
	 * @param model
	 * @param viewFrame
	 * @param cView
	 * @param pBar
	 * @param pBar2
	 * @param pBar3
	 * @param pBar4
	 * @param currentArray
	 */
	public ClusterProcessor(ClusterView cView, JProgressBar pBar, 
			JProgressBar pBar2, JProgressBar pBar3, JProgressBar pBar4, 
			String similarityM, int row_clusterN, int row_iterations, 
			int col_clusterN, int col_iterations) {
		
		this.model = cView.getClusterModel();
		this.pBar = pBar;
		this.pBar2 = pBar2;
		this.pBar3 = pBar3;
		this.pBar4 = pBar4;
		this.currentArray = cView.getDataArray();
		this.finalPanel = cView.getFinalPanel();
		this.mainPanel = cView.getMainPanel();
		this.choice = (String)cView.getGeneCombo().getSelectedItem();
		this.choice2 = (String)cView.getArrayCombo().getSelectedItem();
		this.similarityM = similarityM;
		this.row_clusterN = row_clusterN;
		this.row_iterations = row_iterations;
		this.col_clusterN = col_clusterN;
		this.col_iterations = col_iterations;
	}
	
	/**
	 * Main method to iterate through the various processes of clustering
	 * which are done by other classes.
	 * @param similarityM
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void cluster(boolean hierarchical) 
			throws InterruptedException, ExecutionException {
		
		//declare variables needed for function
		List<Double> currentList = new ArrayList<Double>();
		List<List<Double>> sepRows = new ArrayList<List<Double>>();
		List<List<Double>> sepCols = new ArrayList<List<Double>>();
		List<List<Double>> rowDistances  = new ArrayList<List<Double>>();
		List<List<Double>> colDistances = new ArrayList<List<Double>>();
		
		//change data array into a list (more flexible, faster access for 
		//larger computations)
		for(double d : currentArray) {
			
			currentList.add(d);
		}
		
		List<String> orderedRows = new ArrayList<String>();
		List<String> orderedCols = new ArrayList<String>();
		
		DataFormatter formattedData = new DataFormatter(model, 
				currentList, pBar);
		
		//if user checked clustering for elements
		if(!choice.contentEquals("Do Not Cluster")) {
			
			mainPanel.revalidate();
			mainPanel.repaint();
			
			formattedData.splitRows();
			
			sepRows = formattedData.getRowList();
			
			DistanceMatrixCalculator dCalc = 
					new DistanceMatrixCalculator(sepRows, choice, pBar);
			
			dCalc.measureDistance();
			
			rowDistances  = dCalc.getDistanceMatrix();
			
//			HCluster cGen = 
//					new HCluster(model, rowDistances, pBar2, 
//					rowString, similarityM);
//			
//			cGen.cluster();
			
			if(hierarchical) {
				orderedRows = hCluster(rowDistances, rowString, pBar2);//cGen.getReorderedList();
				
			} else { 
				orderedCols = kmCluster(rowDistances, rowString, pBar2, 
						row_clusterN, row_iterations);
			}
			
			mainPanel.revalidate();
			mainPanel.repaint();
			
			//finalPanel.setPath(cGen.getFilePath());
		}
		
		//if user checked clustering for arrays
		if(!choice2.contentEquals("Do Not Cluster")) {
			
			mainPanel.revalidate();
			mainPanel.repaint();
			
			formattedData.splitColumns();
			sepCols = formattedData.getColList();
			
			DistanceMatrixCalculator dCalc2 = 
					new DistanceMatrixCalculator(sepCols, choice2, pBar3);
			
			dCalc2.measureDistance();
			
			colDistances  = dCalc2.getDistanceMatrix();
			
//			HCluster cGen2 = 
//					new HCluster(model, colDistances, pBar4, 
//					colString, similarityM);
//			
//			cGen2.cluster();
			
			if(hierarchical) {
				orderedCols = hCluster(colDistances, colString, pBar4);//cGen2.getReorderedList();
				
			} else {
				orderedCols = kmCluster(colDistances, colString, pBar4, 
						col_clusterN, col_iterations);
			}
			
			mainPanel.revalidate();
			mainPanel.repaint();;
			
//			finalPanel.setPath(cGen2.getFilePath());
		}
		
		//also takes list of row elements because only one list can easily 
		//be consistently transformed and fed into file writer 
		//to make a tab-delimited file
		mainPanel.revalidate();
		mainPanel.repaint();
		
		CDTGenerator cdtGen = new CDTGenerator(model, sepRows, 
				orderedRows, orderedCols, choice, choice2);
		cdtGen.generateCDT();
		
		mainPanel.revalidate();
		mainPanel.repaint();
		
		finalPanel.setPath(cdtGen.getFilePath());
		finalPanel.setFile(cdtGen.getFile());
	}
	
	/**
	 * Method to execute one of the Hierarchical Clustering algorithms.
	 * @param distances
	 * @param type
	 * @param pBar
	 * @return
	 */
	public List<String> hCluster(List<List<Double>> distances, String type,
			JProgressBar pBar) {
		
		HCluster cGen = 
				new HCluster(model, distances, pBar, 
				type, similarityM);
		
		cGen.cluster();
		
		finalPanel.setPath(cGen.getFilePath());
		
		return cGen.getReorderedList();
	}
	
	/**
	 * Method to execute the K-Means clustering algorithm.
	 * @param distances
	 * @param type
	 * @param pBar
	 * @param clusterN
	 * @param iterations
	 * @return
	 */
	public List<String> kmCluster(List<List<Double>> distances, String type,
			JProgressBar pBar, int clusterN, int iterations) {
		
		KMeansCluster cGen = 
				new KMeansCluster(model, distances, pBar, 
						clusterN, iterations, type);
		
		cGen.cluster();
		
		finalPanel.setPath(cGen.getFilePath());
		
		return cGen.getReorderedList();
	}
}
