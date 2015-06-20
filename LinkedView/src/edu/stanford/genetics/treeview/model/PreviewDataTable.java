package edu.stanford.genetics.treeview.model;

import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

public class PreviewDataTable extends JTable {

	/** Keeping Eclipse happy...*/
	private static final long serialVersionUID = 1L;
	
	private static final int COLUMN_WIDTH = 100;
	private static final int ROW_HEIGHT = 30;
	
	public PreviewDataTable(String[][] previewData, Object[] headers) {
		
		super(previewData, headers);
		
		// TODO implement custom table model for data refreshing
		setTableHeader(null);
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setDefaultColumnWidth();
		setRowHeight(ROW_HEIGHT);
		setPreferredScrollableViewportSize(getPreferredSize());
	}
	
	private void setDefaultColumnWidth() {
		
		TableColumnModel tcm = getColumnModel();
		
		for(int i = 0; i < getColumnCount(); i++) {
			tcm.getColumn(i).setPreferredWidth(COLUMN_WIDTH);
		}
	}
}
