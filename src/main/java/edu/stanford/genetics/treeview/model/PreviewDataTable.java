package edu.stanford.genetics.treeview.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

public class PreviewDataTable extends JTable {

	/** Keeping Eclipse happy... */
	private static final long serialVersionUID = 1L;

	private static final Color CELL_HIGHLIGHT = new Color(67, 134, 245);

	private static final int COLUMN_WIDTH = 100;
	private static final int ROW_HEIGHT = 30;

	private final LabelCellRenderer renderer;

	public PreviewDataTable(String[][] previewData, Object[] labels) {

		super(getNewDefaultTableModel(labels, 0));

		this.renderer = new LabelCellRenderer();

		setData(previewData);
		setDefaultRenderer(String.class, renderer);
		setBorder(BorderFactory.createMatteBorder(1, 1, 2, 1, new Color(230,
				230, 230)));
		setTableHeader(null);
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setDefaultColumnWidth();
		setRowHeight(ROW_HEIGHT);
		setPreferredScrollableViewportSize(getPreferredSize());
		setFocusable(false);
		setRowSelectionAllowed(false);
		setColumnSelectionAllowed(false);
	}

	/**
	 * Delegates the parameters to the includeLabelsUpTo method of the
	 * LabelCellRenderer for this JTable.
	 * 
	 * @param maxRow
	 *            The number of rows included as labels.
	 * @param maxCol
	 *            The number of columns included as labels.
	 */
	public void includeLabelsUpTo(int maxRow, int maxCol) {

		renderer.includeLabelsUpTo(maxRow, maxCol);
	}

	public void setData(String[][] previewData) {

		DefaultTableModel model = (DefaultTableModel) getModel();
		model.setRowCount(0);

		for (String[] row : previewData) {
			model.addRow(row);
		}
	}

	private void setDefaultColumnWidth() {

		TableColumnModel tcm = getColumnModel();

		for (int i = 0; i < getColumnCount(); i++) {
			tcm.getColumn(i).setPreferredWidth(COLUMN_WIDTH);
		}
	}

	private static DefaultTableModel getNewDefaultTableModel(
			final Object[] labels, final int rowCount) {

		DefaultTableModel defaultModel = new DefaultTableModel(labels,
				rowCount) {

			/** Keeping Eclipse happy... */
			private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return String.class;
			}
		};

		return defaultModel;
	}

	/**
	 * Implements custom rendering for a JTable cell. The intent here is to
	 * allow the cell background color to indicate if it is considered to be
	 * part of a label row or column. The Lists of integer values are supposed
	 * to be updated by the JSpinners for including rows/ columns as labels. The
	 * values in these lists are then compared with specific cell indices and if
	 * there's a match, the cells will be colored.
	 * 
	 * @author chris0689
	 *
	 */
	private class LabelCellRenderer extends DefaultTableCellRenderer {

		/** Keeping Eclipse happy... */
		private static final long serialVersionUID = 1L;
		private List<Integer> label_rows;
		private List<Integer> label_cols;

		public LabelCellRenderer() {

			this.label_rows = new ArrayList<Integer>();
			this.label_cols = new ArrayList<Integer>();
		}

		@Override
		public JComponent getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int col) {

			/* get the DefaultCellRenderer to give you the basic component */
			JComponent c = (JComponent) super.getTableCellRendererComponent(
					table, value, isSelected, hasFocus, row, col);

			/* Conditions for cell highlighting */
			boolean adjustRowComp = !label_rows.isEmpty() &&
				label_rows.contains(Integer.valueOf(row));
			boolean adjustColumnComp = !label_cols.isEmpty() &&
				label_cols.contains(Integer.valueOf(col));
			boolean firstDataCell = row == label_rows.size() &&
				col == label_cols.size();

			/* Apply highlighting (or not) */
			if (adjustRowComp || adjustColumnComp) {
				c.setBackground(PreviewDataTable.CELL_HIGHLIGHT);
				c.setForeground(Color.WHITE);

			} else {
				c.setForeground(Color.BLACK);
				Color cellColor = Color.WHITE;
				if (row % 2 != 0) {
					cellColor = new Color(245, 245, 245);
				}
				c.setBackground(cellColor);

				if(firstDataCell) {
					c.setBorder(BorderFactory.createMatteBorder(2,2,2,2,
						PreviewDataTable.CELL_HIGHLIGHT));
				}
			}

			return c;
		}

		/**
		 * TODO divide by row/ column Specifies how many rows/ columns are
		 * considered to be labels. Starts at index 0 at all times because
		 * labels in a dataset should always be the very first rows/ columns.
		 * Should be called whenever JSpinners are updated in
		 * DataImportController.
		 * 
		 * @param maxRow
		 *            The number of continuous rows considered to be labels.
		 * @param maxCol
		 *            The number of continuous columns considered to be labels.
		 */
		public void includeLabelsUpTo(int maxRow, int maxCol) {

			/* Deal with rows */
			setNewLabelListVals(label_rows, maxRow);
			setNewLabelListVals(label_cols, maxCol);
		}

		/**
		 * Updates the Integer lists so that they include all the currently
		 * defined labels indices.
		 * 
		 * @param list
		 * @param maxVal
		 */
		private void setNewLabelListVals(List<Integer> list, int maxVal) {

			list.clear();

			int incl = 0;
			while (incl < maxVal) {
				list.add(incl++);
			}
		}
	}
}
