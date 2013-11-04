package Cluster;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JProgressBar;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.model.TVModel;

/**
 * This class is used to make an object which can take in the loaded data 
 * in its format as originally coded in Java TreeView's first version and 
 * format it for use in the clustering module.
 * @author CKeil
 */
public class DataFormatter {
	
	//Instance variables
	private TVModel model;
	private List<Double> list;
	private JProgressBar pBar;
	
	private List<List<Double>> rowList = new ArrayList<List<Double>>();
	private List<List<Double>> colList = new ArrayList<List<Double>>();
	
	//Constructor (building the object)
	public DataFormatter(DataModel model, List<Double> list, 
			JProgressBar pBar) {
		
		this.model = (TVModel)model;
		this.list = list;
		this.pBar = pBar;
	}
	
	//extracting rows from raw data array
	public void splitRows() {
		
		int lower = 0;
		int upper = 0;
		
		//number of arrays
		int max = model.nExpr();
		
		pBar.setMaximum(list.size()/max);
		
		for(int i = 0; i < list.size()/max; i++) {
			
			pBar.setValue(i);
			
			upper+=max;
			
			rowList.add(list.subList(lower, upper));
			
			lower = upper;
		}
		
		if(upper < list.size() -1) {
			lower = upper;
			upper = list.size();
			
			rowList.add(list.subList(lower, upper));
		}
	}
	
	//getting the columns from raw data array
	public void splitColumns() {
		
		//Number of arrays/ columns
		int max = model.nExpr();
		int nGenes = model.nGene();
		
		//Setting up ProgressBar
		pBar.setMaximum(list.size()/max);
		
		//Iterate through all columns 
		for(int j = 0; j < max; j++) {
			
			pBar.setValue(j);
			
			List<Double> sArray = new ArrayList<Double>();
			
			for(int i = 0; i < nGenes; i++) {
				
				int element = (i * max) + j;
				
				sArray.add(list.get(element));
			}
			
			colList.add(sArray);
		}
	}
	
	//Accessor methods to return each data list
	public List<List<Double>> getRowList() {
		
		return rowList;
	}
	
	public List<List<Double>> getColList() {
		
		return colList;
	}
}
