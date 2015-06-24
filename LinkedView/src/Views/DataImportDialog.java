package Views;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import Utilities.CustomDialog;
import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.model.PreviewDataTable;

public class DataImportDialog extends CustomDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private PreviewDataTable dataTable;
	
	private JButton proceedBtn;
	
	private JSpinner rowDataStart;
	private JSpinner columnDataStart;
	
	private JCheckBox tabDelimCheckBox;
	private JCheckBox commaDelimCheckBox;
	private JCheckBox semicolonDelimCheckBox;
	private JCheckBox spaceDelimCheckBox;
	
	// TODO add ability to select which rows/ cols are labels. highlight selection dynamically
	// add checkbox that confirms the correctness of the loaded data.
	public DataImportDialog(String filename) {
		
		super("Data Import - [" + filename + "]");
	}
	
	public void setupDialogComponents() {

		setupCheckBoxes();
		setupLayout();
		setInputDefaults();
	}
	
	/**
	 * Sets up the GUI for the data import dialog.
	 */
	private void setupLayout() {
		
		JPanel checkboxPanel;
		JPanel indexPanel;
		
		final String delimText = "Select delimiters for your dataset:";
		final JLabel preDelimiterLine = GUIFactory.createLabel(delimText, 
				GUIFactory.FONTS_B);
		
		/* Delimiter panel */
		checkboxPanel = GUIFactory.createJPanel(false, 
				GUIFactory.DEFAULT);
		checkboxPanel.add(tabDelimCheckBox, "push");
		checkboxPanel.add(commaDelimCheckBox, "push");
		checkboxPanel.add(semicolonDelimCheckBox, "push");
		checkboxPanel.add(spaceDelimCheckBox, "push");
		
		final String findDataStartText = "Select indices of first data cell:";
		JLabel findDataStartLabel = GUIFactory.createLabel(findDataStartText, 
				GUIFactory.FONTS_B);
		
		SpinnerNumberModel indexModel = 
				new SpinnerNumberModel(0, 0, 10, 1); // must be ints for Spinner listener
		SpinnerNumberModel indexModel2 = 
				new SpinnerNumberModel(0, 0, 10, 1);
		
		final String rowSpinnerText = "Row #:";
		JLabel rowSpinnerLabel = GUIFactory.createLabel(rowSpinnerText, 
				GUIFactory.FONTS);
		rowDataStart = new JSpinner(indexModel);
		
		final String columnSpinnerText = "Column #:";
		JLabel columnSpinnerLabel = GUIFactory.createLabel(columnSpinnerText, 
				GUIFactory.FONTS);
		columnDataStart = new JSpinner(indexModel2);
		
		indexPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);
		indexPanel.add(rowSpinnerLabel);
		indexPanel.add(rowDataStart);
		indexPanel.add(columnSpinnerLabel, "pushx");
		indexPanel.add(columnDataStart, "wrap");
		
		final String previewText = "Inspect this data preview (25x25):";
		final JLabel preTableLine = GUIFactory.createLabel(previewText, 
				GUIFactory.FONTS_B);
		
		final JScrollPane scrollPane = new JScrollPane(dataTable);
		
		proceedBtn = GUIFactory.createBtn("Proceed >");
		
		mainPanel.add(preDelimiterLine, "wrap");
		mainPanel.add(checkboxPanel, "push, wrap");
		
		mainPanel.add(findDataStartLabel, "push, wrap");
		mainPanel.add(indexPanel, "push, wrap");
		
		mainPanel.add(preTableLine, "wrap");
		mainPanel.add(scrollPane, "w :800:, h :600:, span, wrap");
		
		mainPanel.add(closeBtn, "align left");
		mainPanel.add(proceedBtn, "align right");
		
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
	
	private void setInputDefaults() {
		
		proceedBtn.setEnabled(false);
		
		tabDelimCheckBox.setSelected(true);
		commaDelimCheckBox.setSelected(false);
		semicolonDelimCheckBox.setSelected(false);
		spaceDelimCheckBox.setSelected(false);
		
		
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
	
	public void updateTableLabels(int maxRow, int maxCol) {
		
		dataTable.includeLabelsUpTo(maxRow, maxCol);
		dataTable.repaint();
	}
	
	public void addSpinnerListeners(ChangeListener l) {
		
		rowDataStart.addChangeListener(l);
		columnDataStart.addChangeListener(l);
	}
	
	public void addDelimCheckBoxesListener(ChangeListener l) {
		
		tabDelimCheckBox.addChangeListener(l);
		commaDelimCheckBox.addChangeListener(l);
		spaceDelimCheckBox.addChangeListener(l);
		semicolonDelimCheckBox.addChangeListener(l);
	}
	
	public JSpinner getRowStartSpinner() {
		
		return rowDataStart;
	}
	
	public JSpinner getColStartSpinner() {
		
		return columnDataStart;
	}
}
