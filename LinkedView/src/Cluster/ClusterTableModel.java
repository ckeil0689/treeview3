package Cluster;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.model.TVModel;

public class ClusterTableModel extends AbstractTableModel{
	

	private static final long serialVersionUID = 1L;
	
	//Instance variables
	private List<List<Double>> tableList;
	private TVModel currentModel;
	
	public ClusterTableModel(List<List<Double>> tableList, 
			DataModel currentModel){
		
		this.tableList = tableList; 
		this.currentModel = (TVModel)currentModel;
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
		
	    return currentModel.getArrayHeaderInfo().getHeaderArray()[pCol][0];
	}
}
