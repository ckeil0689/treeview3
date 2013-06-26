/*
 * Created on Sep 27, 2006
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package jtvexample.plugin;

import java.awt.BorderLayout;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.MainProgramArgs;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
import edu.stanford.genetics.treeview.ViewFrame;

public class DataTablePanel extends JPanel implements MainPanel {
	private ConfigNode configNode = null;
	private ViewFrame viewFrame = null;
	private JTable jtable  = null;
	
	class GeneSelectionObserver implements Observer {
		public void update(Observable o, Object arg) {
			TreeSelectionI geneSelection = viewFrame.getGeneSelection();
			jtable.setRowSelectionInterval(geneSelection.getMinIndex(), 
							geneSelection.getMaxIndex());
		}
	}
	
	class RowSelectionListener implements ListSelectionListener {

		public void valueChanged(ListSelectionEvent arg0) {
			TreeSelectionI geneSelection = viewFrame.getGeneSelection();
			@SuppressWarnings("unused")
			int rows[] = jtable.getSelectedRows();
			for (int i = 0; i < geneSelection.getNumIndexes(); i++) {
				geneSelection.setIndex(i, jtable.isRowSelected(i));
			}
			geneSelection.notifyObservers();
		}
		
	}
	
	class ThinTableModel extends AbstractTableModel {

		
		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		@Override
		public String getColumnName(int col) {
			if (viewFrame == null) 
				return super.getColumnName(col);
			DataModel model = viewFrame.getDataModel();
			if (model == null) 
				return super.getColumnName(col);
			if (col == 0)
				return "Gene";
			else
				return concat(model.getArrayHeaderInfo().getHeader(col-1));
		}

		private String concat(String[] header) {
			String ret = header[0];
			for (int i= 1 ; i < header.length; i++)
				ret += "\n" + header[i];
			return ret;
		}

		public int getColumnCount() {
			if (viewFrame == null) 
				return 0;
			DataModel model = viewFrame.getDataModel();
			if (model == null) 
				return 0;
			return model.getArrayHeaderInfo().getNumHeaders() + 1;
		}

		public int getRowCount() {
			if (viewFrame == null) 
				return 0;
			DataModel model = viewFrame.getDataModel();
			if (model == null) 
				return 0;
			return model.getGeneHeaderInfo().getNumHeaders();
		}

		public Object getValueAt(int row, int col) {
			if (viewFrame == null) 
				return new Integer(0);
			DataModel model = viewFrame.getDataModel();
			if (model == null) 
				return new Integer(0);
			if (col == 0) {
				return concat(model.getGeneHeaderInfo().getHeader(row));
			} else {
				return model.getDataMatrix().getValue
				(col-1, row);
			}
		}
		
	}
	
	
	public DataTablePanel(ConfigNode configNode2, ViewFrame viewFrame) {
		configNode = configNode2;
		this.viewFrame = viewFrame;
		
		
		
		
	      jtable = new JTable(new ThinTableModel());
	      jtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	      jtable.setAutoscrolls(true);
	      JScrollPane scrollPane = new JScrollPane(jtable);
	      scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
	      scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
	      setLayout(new BorderLayout());
	      add(scrollPane, BorderLayout.CENTER);
	      
	      
	      viewFrame.getGeneSelection().addObserver(new GeneSelectionObserver());
	      
	      jtable.getSelectionModel().addListSelectionListener(new RowSelectionListener());
	}

	public ConfigNode getConfigNode() {
		return configNode;
	}

	public ImageIcon getIcon() {
		return null;
	}

	public String getName() {
		return "DataTable";
	}

	public void populateAnalysisMenu(Menu arg0) {
		// could put additional analysis options here
	}

	public void populateExportMenu(Menu arg0) {
		// could put export menu items here
	}

	public void populateSettingsMenu(Menu arg0) {
		// If there are any setting to be configured, add them here
		// for now, I'm just going to store a value that's never 
		// used for anything...
		MenuItem bob = new MenuItem("Edit Persistent Value...");
		bob.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				JTextField value = new JTextField(configNode.getAttribute("bob", "Edit me!"));
				JOptionPane.showMessageDialog(DataTablePanel.this, value, 
						"Edit the persistent value", JOptionPane.INFORMATION_MESSAGE);
				configNode.setAttribute("bob", value.getText(), "Edit me!");
			}
		});
		arg0.add(bob);
	}

	public void scrollToArray(int arg0) {
		// TODO Auto-generated method stub

	}

	public void scrollToGene(int arg0) {
		jtable.setRowSelectionInterval(arg0, arg0);
	}

	public void syncConfig() {
		// This will be called before the display is closed, so 
		// values can be saved to the ConfigNode
	}

	@Override
	public void populateSettingsMenu(TreeviewMenuBarI menubar) {
	}

	@Override
	public void populateAnalysisMenu(TreeviewMenuBarI menubar) {
	}

	@Override
	public void populateExportMenu(TreeviewMenuBarI menubar) {
	}

	@Override
	public void export(MainProgramArgs args) throws ExportException {
		LogBuffer.getSingleton().log("Error: cannot export DataTable.");
	}

}
