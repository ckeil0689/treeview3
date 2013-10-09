package Cluster;

import java.util.List;

import javax.swing.table.AbstractTableModel;

public class ClusterTableModel extends AbstractTableModel{
	

	private static final long serialVersionUID = 1L;
	
	//Instance variables
	private List<List<Double>> tableList;
	private ClusterModel currentModel;
	
	public ClusterTableModel(List<List<Double>> tableList, ClusterModel currentModel){
		this.tableList = tableList; 
		this.currentModel = currentModel;

	}
	
	//Depends on whether elements or arrays are used in DataViewDialog!
	@Override
	public int getColumnCount() {
		
		return tableList.size();
	}

	@Override
	public int getRowCount() {
		
		return tableList.get(0).size();
	}

	@Override
	public Object getValueAt(int arg0, int arg1) {

		return currentModel.getValue(arg1, arg0);
	}
	
	@Override
	public String getColumnName(int pCol) {
		
	    return currentModel.arrayHeaderInfo.getHeaderArray()[pCol][0];
	}
}
