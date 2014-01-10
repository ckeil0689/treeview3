/*
 * Created on Mar 6, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.plugin.treeanno;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.util.Observable;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.TreeSelectionI;

/**
 * class to allow editing of TreeViewNodes, altough could easily be generalized
 * to all HeaderInfo types.
 */
public class TableNodeView extends ModelView implements ListSelectionListener {

	private final NodeTableModel tableModel;
	private TreeSelectionI selection;
	private final JTable nodeTable;
	private final HeaderInfo headerInfo;

	public void setSelection(final TreeSelectionI sel) {
		if (selection != null) {
			selection.deleteObserver(this);
		}
		selection = sel;
		selection.addObserver(this);
		if (selection != null) {
			update(selection, null);
		}
	}

	/**
	 * display table representing headerinfo contents.
	 */
	private class NodeTableModel extends AbstractTableModel {

		@Override
		public int getRowCount() {
			return headerInfo.getNumHeaders();
		}

		@Override
		public String getColumnName(final int i) {
			return headerInfo.getNames()[i];
		}

		@Override
		public int getColumnCount() {
			return headerInfo.getNumNames();
		}

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex) {
			return headerInfo.getHeader(rowIndex, columnIndex);
		}

		@Override
		public void setValueAt(final Object val, final int row, final int col) {
			headerInfo.setHeader(row, headerInfo.getNames()[col], (String) val);
		}

		@Override
		public boolean isCellEditable(final int row, final int col) {
			final String[] names = headerInfo.getNames();
			if (names[col].equals("NODEID"))
				return false;
			if (names[col].equals("LEFT"))
				return false;
			if (names[col].equals("RIGHT"))
				return false;
			if (names[col].equals("CORRELATION"))
				return false;
			return true;
		}
	}

	/**
	 * @param nodeInfo
	 */
	public TableNodeView(final HeaderInfo nodeInfo) {
		headerInfo = nodeInfo;
		headerInfo.addObserver(this);
		tableModel = new NodeTableModel();
		nodeTable = new JTable(tableModel);
		nodeTable.getSelectionModel().addListSelectionListener(this);
		nodeTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setLayout(new BorderLayout());
		add(new JScrollPane(nodeTable), BorderLayout.CENTER);
	}

	@Override
	public String viewName() {
		return "Tree Node Editor";
	}

	@Override
	protected void updateBuffer(final Graphics g) {
		// no buffer here.
	}

	@Override
	public void update(final Observable o, final Object arg) {
		update((Object) o, arg);
	}

	public void update(final Object o, final Object arg) {

		if (o == selection) {
			nodeTable.clearSelection();
			final String nodeName = selection.getSelectedNode();
			if (nodeName != null) {
				final int index = headerInfo.getHeaderIndex(nodeName);
				if (index >= 0) {
					nodeTable.changeSelection(index, 0, false, false);
					nodeTable.changeSelection(index, headerInfo.getNumNames(),
							false, true);
				}
			}
		} else if (o == headerInfo) {
			// dumb table model, doesn't keep things selected.
			final int index = nodeTable.getSelectedRow();
			tableModel.fireTableStructureChanged();
			nodeTable.changeSelection(index, 0, false, false);
		}
	}

	@Override
	public void valueChanged(final ListSelectionEvent e) {
		final int row = nodeTable.getSelectedRow();
		if (row >= 0) {
			final String name = headerInfo.getHeader(row, 0);
			selection.setSelectedNode(name);
			selection.notifyObservers();
		}
	}
}
