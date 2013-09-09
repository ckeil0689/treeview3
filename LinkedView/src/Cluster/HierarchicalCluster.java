package Cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import edu.stanford.genetics.treeview.TreeViewFrame;

import Cluster.ClusterView.FinalOptionsPanel;

/**
 * This class takes the original uploaded dataArray passed 
 * in the constructor and manipulates it according to mathematical 
 * principles of hierarchical clustering. It generates files to display dendrograms (.gtr and .atr)
 * as well as a reordered original data file (.cdt)
 * @author CKeil
 *
 */
public class HierarchicalCluster {
	
	//Instance variables
	private ClusterModel model;
	private JFrame frame;
	private JProgressBar pBar;
	private JLabel loadingInfo;
	private double[] currentArray;
	private boolean isRows = true;
	private boolean isColumns = false;
	private String choice;
	private String choice2;
	
	private FinalOptionsPanel finalPanel;
	private JPanel mainPanel;
	
	//Constructor (building the object)
	public HierarchicalCluster(ClusterModel model, TreeViewFrame viewFrame, ClusterView cView, 
			JProgressBar pBar, double[] currentArray){
		
		this.model = model;
		this.pBar = pBar;
		this.currentArray = currentArray;
		this.frame = viewFrame;
		this.finalPanel = cView.getFinalPanel();
		this.mainPanel = cView.getMainPanel();
		this.choice = (String)cView.getGeneCombo().getSelectedItem();
		this.choice2 = (String)cView.getArrayCombo().getSelectedItem();
	}
	
	public void hCluster(String similarityM) 
			throws InterruptedException, ExecutionException{
		
		loadingInfo = new JLabel();
		
		//declare variables needed for function
		List<Double> currentList = new ArrayList<Double>();
		List<List<Double>> sepRows = new ArrayList<List<Double>>();
		List<List<Double>> sepCols = new ArrayList<List<Double>>();
		List<List<Double>> rowDistances  = new ArrayList<List<Double>>();
		List<List<Double>> colDistances = new ArrayList<List<Double>>();
		
		//change data array into a list (more flexible, faster access for larger computations)
		for(double d : currentArray){
			
			currentList.add(d);
		}
		
		List<String> orderedRows = new ArrayList<String>();
		List<String> orderedCols = new ArrayList<String>();
		
		DataFormatter formattedData = new DataFormatter(model, currentList, pBar);
		
		//if user checked clustering for elements
		if(!choice.contentEquals("Do Not Cluster")){
			
			finalPanel.add(loadingInfo, "alignx 50%, pushx, wrap");
			
			loadingInfo.setText("Operation Infos...");
			
			mainPanel.revalidate();
			mainPanel.repaint();
			
			loadingInfo.setText("Preparing Row Data...");
			
			formattedData.splitRows();
			sepRows = formattedData.getRowList();
			
			loadingInfo.setText("Creating Row Distance Matrix...");
			
			DistanceMatrixCalculator dCalc = new DistanceMatrixCalculator(sepRows, choice, pBar);
			
			dCalc.measureDistance();
			
			rowDistances  = dCalc.getDistanceMatrix();
			
			loadingInfo.setText("Clustering Row Elements...");
			
			ClusterGenerator cGen = new ClusterGenerator(model, frame, 
					rowDistances, pBar, isRows, similarityM);
			
			cGen.cluster();
			
			orderedRows = cGen.getReorderedList();
			
			finalPanel.remove(loadingInfo);
			mainPanel.revalidate();
			mainPanel.repaint();
			
			finalPanel.setPath(cGen.getFilePath());
			
		}
		
		//if user checked clustering for arrays
		if(!choice2.contentEquals("Do Not Cluster")){
			
			finalPanel.add(loadingInfo, "alignx 50%, pushx, wrap");
			
			loadingInfo.setText("Operation Infos");
			
			mainPanel.revalidate();
			mainPanel.repaint();
			
			loadingInfo.setText("Preparing Column Data...");
			
			formattedData.splitColumns();
			sepCols = formattedData.getColList();
			
			loadingInfo.setText("Creating Column Distance Matrix...");
			
			DistanceMatrixCalculator dCalc2 = new DistanceMatrixCalculator(sepCols, choice2, pBar);
			
			dCalc2.measureDistance();
			
			colDistances  = dCalc2.getDistanceMatrix();
			
			loadingInfo.setText("Clustering Column Elements...");
			
			ClusterGenerator cGen2 = new ClusterGenerator(model, frame, 
					colDistances, pBar, isColumns, similarityM);
			
			cGen2.cluster();
			
			orderedCols = cGen2.getReorderedList();
			
			finalPanel.remove(loadingInfo);
			mainPanel.revalidate();
			mainPanel.repaint();
			
			finalPanel.setPath(cGen2.getFilePath());
			
		}
		
		//also takes list of row elements because only one list can easily be consistently transformed and 
		//fed into file writer to make a tab-delimited file
		finalPanel.add(loadingInfo, "alignx 50%, pushx, wrap");
		
		loadingInfo.setText("Generating .CDT file...");
		
		mainPanel.revalidate();
		mainPanel.repaint();
		
		CDTGenerator cdtGen = new CDTGenerator(model, frame, sepRows, 
				orderedRows, orderedCols, choice, choice2);
		cdtGen.generateCDT();
		
		finalPanel.remove(loadingInfo);
		mainPanel.revalidate();
		mainPanel.repaint();
		
		finalPanel.setPath(cdtGen.getFilePath());
		
	}
}
