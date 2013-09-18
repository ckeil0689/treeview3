package Cluster;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
	private JProgressBar pBar, pBar2;
	private JLabel opLabel;
	private double[] currentArray;
	private final String rowString = "GENE"; 
	private final String colString = "ARRY";
	private String choice;
	private String choice2;
	
	private FinalOptionsPanel finalPanel;
	private JPanel mainPanel;
	
	private int maxProgress_both = 5;
	private int maxProgress_single = 3;
	
	private int prog_count = 0;
	
	//Constructor (building the object)
	public HierarchicalCluster(ClusterModel model, TreeViewFrame viewFrame, ClusterView cView, 
			JProgressBar pBar, JProgressBar pBar2, JLabel opLabel, double[] currentArray){
		
		this.model = model;
		this.pBar = pBar;
		this.pBar2 = pBar2;
		this.opLabel = opLabel;
		this.currentArray = currentArray;
		this.finalPanel = cView.getFinalPanel();
		this.mainPanel = cView.getMainPanel();
		this.choice = (String)cView.getGeneCombo().getSelectedItem();
		this.choice2 = (String)cView.getArrayCombo().getSelectedItem();
	}
	
	public void hCluster(String similarityM) 
			throws InterruptedException, ExecutionException{
		
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
		
		if(!choice.contentEquals("Do Not Cluster") && !choice2.contentEquals("Do Not Cluster")){
			
			pBar2.setMaximum(maxProgress_both);
		}
		else{
			
			pBar2.setMaximum(maxProgress_single);
		}
		
		//if user checked clustering for elements
		if(!choice.contentEquals("Do Not Cluster")){
			
			opLabel.setFont(new Font("Sans Serif", Font.BOLD, 16));
			opLabel.setForeground(Color.LIGHT_GRAY);
			opLabel.setText("Distance Matrix");
			
			mainPanel.revalidate();
			mainPanel.repaint();
			
			formattedData.splitRows();
			sepRows = formattedData.getRowList();
			
			opLabel.setForeground(new Color(240, 80, 50, 255));
			opLabel.setText("Calculating Distance Matrix...");
			
			DistanceMatrixCalculator dCalc = new DistanceMatrixCalculator(sepRows, choice, pBar);
			
			dCalc.measureDistance();
			
			rowDistances  = dCalc.getDistanceMatrix();
			
			prog_count++;
			pBar2.setValue(prog_count);
			
			opLabel.setText("Clustering Data...");
			
			ClusterGenerator cGen = new ClusterGenerator(model, rowDistances, pBar, 
					rowString, similarityM);
			
			cGen.cluster();
			
			orderedRows = cGen.getReorderedList();
			
			prog_count++;
			pBar2.setValue(prog_count);
			
			mainPanel.revalidate();
			mainPanel.repaint();
			
			finalPanel.setPath(cGen.getFilePath());
			
		}
		
		//if user checked clustering for arrays
		if(!choice2.contentEquals("Do Not Cluster")){
			
			opLabel.setFont(new Font("Sans Serif", Font.BOLD, 16));
			opLabel.setForeground(Color.LIGHT_GRAY);
			opLabel.setText("Distance Matrix");
			
			mainPanel.revalidate();
			mainPanel.repaint();
			
			formattedData.splitColumns();
			sepCols = formattedData.getColList();
			
			opLabel.setForeground(new Color(240, 80, 50, 255));
			opLabel.setText("Calculating Distance Matrix...");
			
			DistanceMatrixCalculator dCalc2 = new DistanceMatrixCalculator(sepCols, choice2, pBar);
			
			dCalc2.measureDistance();
			
			colDistances  = dCalc2.getDistanceMatrix();
			
			prog_count++;
			pBar2.setValue(prog_count);
			
			opLabel.setText("Clustering Data...");
			
			ClusterGenerator cGen2 = new ClusterGenerator(model, colDistances, pBar, 
					colString, similarityM);
			
			cGen2.cluster();
			
			orderedCols = cGen2.getReorderedList();
			
			prog_count++;
			pBar2.setValue(prog_count);
			
			mainPanel.revalidate();
			mainPanel.repaint();;
			
			finalPanel.setPath(cGen2.getFilePath());
			
		}
		
		//also takes list of row elements because only one list can easily be consistently transformed and 
		//fed into file writer to make a tab-delimited file
		
		opLabel.setText("Generating Data File...");
		
		mainPanel.revalidate();
		mainPanel.repaint();
		
		CDTGenerator cdtGen = new CDTGenerator(model, sepRows, 
				orderedRows, orderedCols, choice, choice2);
		cdtGen.generateCDT();
		
		mainPanel.revalidate();
		mainPanel.repaint();
		
		opLabel.setForeground(new Color(0, 200, 50));
		opLabel.setText("Data File Saved");
		
		prog_count++;
		pBar2.setValue(prog_count);
		
		finalPanel.setPath(cdtGen.getFilePath());
		finalPanel.setFile(cdtGen.getFile());
		
	}
}
