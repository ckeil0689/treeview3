package Cluster;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.model.TVModel;

public class ClusterTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	// Instance variables
	private final List<double[]> tableList;
	private final TVModel currentModel;

	public ClusterTableModel(final List<double[]> tableList,
			final DataModel currentModel) {

		this.tableList = tableList;
		this.currentModel = (TVModel) currentModel;
	}

	// Depends on whether elements or arrays are used in DataViewDialog!
	@Override
	public int getColumnCount() {

		return tableList.get(0).length;
	}

	@Override
	public int getRowCount() {

		return tableList.size();
	}

	@Override
	public Object getValueAt(final int arg0, final int arg1) {

		return currentModel.getValue(arg1, arg0);
	}

	@Override
	public String getColumnName(final int pCol) {

		return currentModel.getArrayHeaderInfo().getHeaderArray()[pCol][0];
	}
}
