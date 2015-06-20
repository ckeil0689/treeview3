package Views;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeListener;

import edu.stanford.genetics.treeview.model.PreviewDataTable;
import Utilities.CustomDialog;
import Utilities.GUIFactory;

public class DataImportDialog extends CustomDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JTable dataTable;
	
	private JCheckBox tabDelimCheckBox;
	private JCheckBox commaDelimCheckBox;
	private JCheckBox semicolonDelimCheckBox;
	private JCheckBox spaceDelimCheckBox;
	
	
	public DataImportDialog(String filename) {
		
		super("Data Import - [" + filename + "]");
	}
	
	public void setupDialogComponents() {

		setupCheckBoxes();
		setupLayout();
	}
	
	/**
	 * Sets up the GUI for the data import dialog.
	 */
	private void setupLayout() {
		
		String delimText = "Select delimiters for your dataset:";
		JLabel preDelimiterLine = GUIFactory.createLabel(delimText, 
				GUIFactory.FONTS_B);
		
		String previewText = "Inspect this data preview:";
		JLabel preTableLine = GUIFactory.createLabel(previewText, 
				GUIFactory.FONTS_B);
		
		JScrollPane scrollPane = new JScrollPane(dataTable);
		
		/* Delimiter panel */
		JPanel checkboxPanel = GUIFactory.createJPanel(false, 
				GUIFactory.DEFAULT);
		checkboxPanel.add(tabDelimCheckBox, "push");
		checkboxPanel.add(commaDelimCheckBox, "push");
		checkboxPanel.add(semicolonDelimCheckBox, "push");
		checkboxPanel.add(spaceDelimCheckBox, "push");
		
		mainPanel.add(preDelimiterLine, "span, wrap");
		mainPanel.add(checkboxPanel, "push, span, wrap");
		mainPanel.add(preTableLine, "span, wrap");
		mainPanel.add(scrollPane, "w :800:, h :600:, wrap");
		mainPanel.add(closeBtn);
		
		mainPanel.revalidate();
		mainPanel.repaint();
	}
	
	/**
	 * Sets up the JCheckBoxes which are used to set delimiters for 
	 * file loading. 
	 */
	private void setupCheckBoxes() {
		
		tabDelimCheckBox = new JCheckBox("Tab");
		commaDelimCheckBox = new JCheckBox("Comma");
		semicolonDelimCheckBox = new JCheckBox("Semicolon");
		spaceDelimCheckBox = new JCheckBox("Space");
	}
	
	/**
	 * Set up the JTable which will contain the preview data.  
	 */
	public void setNewTable(String[][] previewData) {
		
		String[] columnNames = getColumnNames(previewData[0].length);
		this.dataTable = new PreviewDataTable(previewData, columnNames);
	}
	
	/**
	 * Generates an array of Strings which serve has headers for the JTable
	 * constructor.
	 * @param Number of columns.
	 * @return An array of numbers as Strings.
	 */
	private static String[] getColumnNames(int columnNum) {
		
		final int colNum = columnNum;
		String[] columnNames = new String[colNum];
		for(int i = 0; i < colNum; i++) {
			
			columnNames[i] = Integer.toString(i + 1);
		}
		
		return columnNames;
	}
	
	public void addDelimCheckBoxesListener(ChangeListener l) {
		
		tabDelimCheckBox.addChangeListener(l);
		commaDelimCheckBox.addChangeListener(l);
		spaceDelimCheckBox.addChangeListener(l);
		semicolonDelimCheckBox.addChangeListener(l);
	}
}
