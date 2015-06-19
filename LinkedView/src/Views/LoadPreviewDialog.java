package Views;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.ModelLoader;
import Utilities.CustomDialog;
import Utilities.GUIFactory;

public class LoadPreviewDialog extends CustomDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final FileSet fileSet;
	private JTable dataTable;
	private String[][] previewData;
	
	public LoadPreviewDialog(FileSet fileSet) {
		
		super("Data Import - [" + fileSet.getCdt() + "]");
		
		this.fileSet = fileSet;
		
		loadPreviewData();
		setupLayout();
	}
	
	private void loadPreviewData() {
		
		if(fileSet == null) {
			LogBuffer.println("No fileSet specified to load preview data.");
			return;
		}
		
		String filename = fileSet.getCdt(); 
		previewData = ModelLoader.loadPreviewData(filename);
	}
	
	private void setupLayout() {
		
		String previewText = "Inspect this data preview:";
		JLabel firstLine = GUIFactory.createLabel(previewText, 
				GUIFactory.FONTS);
		
		setupTable();
		
		JScrollPane scrollPane = new JScrollPane(dataTable);
		
		mainPanel.add(firstLine, "span, wrap");
		mainPanel.add(scrollPane, "w :800:, h :600:, wrap");
		mainPanel.add(closeBtn);
		
		mainPanel.revalidate();
		mainPanel.repaint();
	}
	
	/* TODO create new Table class that handles all this */
	private void setupTable() {
		
		final int COL_WIDTH = 150;
		
		String[] columnNames = getColumnNames();
		this.dataTable = new JTable(previewData, columnNames);
		dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		TableColumnModel tcm = dataTable.getColumnModel();
		
		for(int i = 0; i < dataTable.getColumnCount(); i++) {
			tcm.getColumn(i).setPreferredWidth(COL_WIDTH);
		}
		
		dataTable.setPreferredScrollableViewportSize(dataTable
				.getPreferredSize());
	}
	
	private String[] getColumnNames() {
		
		final int colNum = previewData[0].length;
		String[] columnNames = new String[colNum];
		for(int i = 0; i < colNum; i++) {
			
			columnNames[i] = Integer.toString(i + 1);
		}
		
		return columnNames;
	}
	
}
