package Cluster;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

public class DataViewDialog extends JDialog{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ClusterModel model;
	JFrame viewFrame;
	ClusterModel.ClusterDataMatrix matrix;
	
	private double[] dataArray;
//	private List<List<Double>> geneList;
	private List<List<Double>> arraysList;
	
	public DataViewDialog(ClusterModel model, JFrame mainFrame){
		this.model = model;
		this.viewFrame = mainFrame;
		
		JFrame frame = new JFrame();
		this.setTitle("Data View");
		
		frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		
		matrix = model.getDataMatrix();
		dataArray = matrix.getExprData();
		
		List<Double> gList = new ArrayList<Double>();
		
		for(double d : dataArray){
			
			gList.add(d);
		}
		
		arraysList = splitArrays(gList, model);
		
		String[][] geneNames = model.geneHeaderInfo.getHeaderArray();
		
		String[] title = model.geneHeaderInfo.getNames();
		
		JTable table = new JTable(new ClusterTableModel(arraysList, model));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		//create a scrollPane with he table in it 
		JScrollPane tableScroll = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		table.setFillsViewportHeight(true);
		
		JTable rowTable = new RowNumberTable(table);
		
		JTable geneTable = new JTable(geneNames, title);

		geneTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		tableScroll.setRowHeaderView(geneTable);
		tableScroll.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowTable.getTableHeader());
		table.setFillsViewportHeight(true);
		
		mainPanel.add(tableScroll, "grow, push");
		this.add(mainPanel);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}
	
	//function to split up a long array into smaller arrays
	public List <List<Double>> splitGenes(List<Double> gList, ClusterModel model){
		
		int lower = 0;
		int upper = 0;
		int max = model.nExpr();
		
		List<List<Double>> geneList = new ArrayList<List<Double>>();
		
		for(int i = 0; i < gList.size()/max; i++){
			
			upper+=max;
			
			geneList.add(gList.subList(lower, upper));
			
			lower = upper;
			
		}
		
		if(upper < gList.size() -1){
			
			lower = upper;
			upper = gList.size();
			
			geneList.add(gList.subList(lower, upper));
		}
	
		return geneList;
	}
	
	public List <List<Double>> splitArrays(List<Double> gList, ClusterModel model){
		
		//number of rows/ columns
		int max = model.nExpr();
		int nGenes = model.nGene();
		
		arraysList = new ArrayList<List<Double>>();
		
		//iterate through columns ...max
		for(int j = 0; j < max; j++){
			
			List<Double> sArray = new ArrayList<Double>();
			
			for(int i = 0; i < nGenes; i++){
				
				int element = (i * max) + j;
				
				sArray.add(gList.get(element));
				
			}
			
			arraysList.add(sArray);
		}
	
		return arraysList;
	}

}
