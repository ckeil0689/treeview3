package Cluster;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

/**
 * This class is used to generate the data preview table in the Cluster section
 * @author CKeil
 *
 */
public class DataViewPanel extends JPanel{

	private static final long serialVersionUID = 1L;
	
	/**
	 * Instance Variables
	 */
	private ClusterModel model;
	private ClusterModel.ClusterDataMatrix matrix;
	private double[] dataArray;
	private List<List<Double>> arraysList;
	private List<Double> gList;
	
	private String[][] geneNames;
	private String[] title;
	
	/**
	 * Swing Components
	 */
	private JTable table, rowTable, geneTable;
	private JScrollPane tableScroll;
	
	/**
	 * Constructor
	 * @param model
	 * @param mainFrame
	 */
	public DataViewPanel(ClusterModel cModel){
		
		this.model = cModel;
		this.setLayout(new MigLayout("ins 0"));
		
		matrix = model.getDataMatrix();
		dataArray = matrix.getExprData();
		
		gList = fillDList(dataArray);
		arraysList = splitArrays(gList, model);
		
		geneNames = model.geneHeaderInfo.getHeaderArray();
		title = model.geneHeaderInfo.getNames();
		
		table = new JTable(new ClusterTableModel(arraysList, model));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		//create a scrollPane with the table in it 
		tableScroll = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		table.setFillsViewportHeight(true);
		
		rowTable = new RowNumberTable(table);
		geneTable = new JTable(geneNames, title);

		//geneTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		tableScroll.setRowHeaderView(geneTable);
		tableScroll.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowTable.getTableHeader());
		
		this.add(tableScroll, "grow, push");
		this.setVisible(true);
	}
	
	
	public List<Double> fillDList(double[] array){
		
		List<Double> doubleList = new ArrayList<Double>();
		
		for(double d : array){
			
			doubleList.add(d);
		} 
		
		return doubleList;
	}
	
	//functions to split up a long array into smaller arrays
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
		
		List <List<Double>> arraysList = new ArrayList<List<Double>>();
		
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
