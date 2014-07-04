package Cluster;

import javax.swing.table.AbstractTableModel;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.model.TVModel;

public class ClusterTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	// Instance variables
	private final double[][] tableList;
	private final DataModel currentModel;

	public ClusterTableModel(final double[][] tableList,
			final DataModel currentModel) {

		this.tableList = tableList;
		this.currentModel = (TVModel) currentModel;
	}

	// Depends on whether elements or arrays are used in DataViewDialog!
	@Override
	public int getColumnCount() {

		return tableList[0].length;
	}

	@Override
	public int getRowCount() {

		return tableList.length;
	}

	@Override
	public Object getValueAt(final int arg0, final int arg1) {

		return ((TVModel)currentModel).getValue(arg1, arg0);
	}

	@Override
	public String getColumnName(final int pCol) {

		return currentModel.getArrayHeaderInfo().getHeaderArray()[pCol][0];
	}
}
