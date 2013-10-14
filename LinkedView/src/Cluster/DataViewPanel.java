package Cluster;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

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
	
	private String[] geneNames;
	private String[][] headerArray;
	
	/**
	 * Swing Components
	 */
	private JTable table, headerTable;
	private JScrollPane tableScroll;
	
	private final Color BLUE2 = new Color(210, 230, 240, 255);
	
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
		
		//headerArray = model.getArrayHeaderInfo().getHeaderArray();
		headerArray = model.getGeneHeaderInfo().getHeaderArray();
		
		gList = fillDList(dataArray);
		arraysList = splitArrays(gList, model);
		
		geneNames = new String[headerArray.length];
		
		for(int i = 0; i < headerArray.length; i++){
			
			geneNames[i] = headerArray[i][0];
		}
		
		table = new JTable(new ClusterTableModel(arraysList, model));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		//Table Header Options
		JTableHeader header = table.getTableHeader();
		header.setReorderingAllowed(false);
		header.setBackground(BLUE2);
		
		//create a scrollPane with the table in it 
		tableScroll = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		table.setFillsViewportHeight(true);
		
		DefaultTableModel model = new DefaultTableModel(){

            private static final long serialVersionUID = 1L;

            @Override
            public int getColumnCount() {
                return 1;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public int getRowCount() {
                return table.getRowCount();
            }

            @Override
            public Class<?> getColumnClass(int colNum) {
                switch (colNum) {
                    case 0:
                        return String.class;
                    default:
                        return super.getColumnClass(colNum);
                }
            }
        };;
		model.addColumn(geneNames);
		
		headerTable = new JTable(model);
		
		int max = table.getRowCount();
		
		for(int i = 0; i < max; i++){
			
			headerTable.setValueAt(geneNames[i], i, 0);
		}
		
		headerTable.setShowGrid(true);
		headerTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        headerTable.setPreferredScrollableViewportSize(new Dimension(100, 0));
        headerTable.getColumnModel().getColumn(0).setCellRenderer(new TableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable x, Object value, boolean isSelected, 
            		boolean hasFocus, int row, int column) {

                Component component = table.getTableHeader().getDefaultRenderer()
                		.getTableCellRendererComponent(table, value, false, false, -1, -2);
                ((JLabel) component).setHorizontalAlignment(SwingConstants.CENTER);
                return component;
            }
        });
		
		tableScroll.setRowHeaderView(headerTable);
		
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
