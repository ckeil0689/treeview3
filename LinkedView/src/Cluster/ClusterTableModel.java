package Cluster;

import java.util.List;

import javax.swing.table.AbstractTableModel;

public class ClusterTableModel extends AbstractTableModel{
	

	private static final long serialVersionUID = 1L;
	
	//Instance variables
	List<double[]> arrayList;
	ClusterModel currentModel;
	
	public ClusterTableModel(List<double[]> arrayList, ClusterModel currentModel){
		this.arrayList = arrayList; 
		this.currentModel = currentModel;
	}
	@Override
	public int getColumnCount() {
		
		return arrayList.get(0).length;
	}

	@Override
	public int getRowCount() {
		
		return arrayList.size();
	}

	@Override
	public Object getValueAt(int arg0, int arg1) {

		return currentModel.getValue(arg0, arg1);
	}

}
