package Cluster;

import java.util.ArrayList;
import java.util.Arrays;
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
	private List<double[]> geneList;
	
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
		geneList = splitArray(dataArray, model);
		
		String[][] geneNames = model.geneHeaderInfo.getHeaderArray();
		
		String[] title = model.geneHeaderInfo.getNames();
		
		//create trial table with demo data to add to JSplitPane
		//JTable can either accept raw string array data or Vectors
		//Vectors preferred because they can be resized and optimiz memory allocation
		JTable table = new JTable(new ClusterTableModel(geneList, model));
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
		//tableScroll.setPreferredSize(new Dimension(viewFrame.getSize()));
		//create a scrollPane with he table in it 
		table.setFillsViewportHeight(true);
		
		mainPanel.add(tableScroll, "grow, push");
		this.add(mainPanel);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}
	
	//function to split up a long array into smaller arrays
	public List <double[]> splitArray(double[] array, ClusterModel model){
		
		int lower = 0;
		int upper = 0;
		int max = model.nExpr();
		
		List<double[]> geneList = new ArrayList<double[]>();
		
		for(int i = 0; i < array.length/max; i++){
			
			upper+=max;
			
			geneList.add(Arrays.copyOfRange(array, lower, upper));
			
			lower = upper;
			
		}
		
		if(upper < array.length -1){
			
			lower = upper;
			upper = array.length;
			
			geneList.add(Arrays.copyOfRange(array, lower, upper));
		}
	
		return geneList;
	}

}
