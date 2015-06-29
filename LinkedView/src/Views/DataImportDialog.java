package Views;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

import Utilities.CustomDialog;
import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.model.DataLoadInfo;
import edu.stanford.genetics.treeview.model.PreviewDataTable;

public class DataImportDialog extends CustomDialog {

	/** Keep Eclipse happy... */
	private static final long serialVersionUID = 1L;
	
	public static final int LABELS_READY = 1;
	public static final int LABELS_HINT = 2;
	public static final int LABELS_WARNING = 3;

	private PreviewDataTable dataTable;
	
	private DataLoadInfo result;
	
	private JButton proceedBtn;
	private JButton findDataBtn;
	private JLabel statusIndicator;
	
	private JSpinner rowDataStart;
	private JSpinner columnDataStart;
	
	private JCheckBox noLabelCheckBox;
	
	private final List<JCheckBox> delimiters;
	
	private JCheckBox tabDelimCheckBox;
	private JCheckBox commaDelimCheckBox;
	private JCheckBox semicolonDelimCheckBox;
	private JCheckBox spaceDelimCheckBox;
	
	public DataImportDialog(String filename) {
		
		super("Data Import - [" + filename + "]");
		
		this.delimiters = new ArrayList<JCheckBox>();
		
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
		
		JPanel delimPanel;
		JPanel checkboxPanel;
		JPanel indexPanel;
		JPanel buttonPanel;
		
		final String delimText = "1) Select delimiters for your dataset:";
		final JLabel preDelimiterLine = GUIFactory.createLabel(delimText, 
				GUIFactory.FONTM_B);
		
		/* Delimiter panel */
		delimPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);
		checkboxPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);
		
		checkboxPanel.add(tabDelimCheckBox, "push");
		checkboxPanel.add(commaDelimCheckBox, "push");
		checkboxPanel.add(semicolonDelimCheckBox, "push");
		checkboxPanel.add(spaceDelimCheckBox, "push");
		
		delimPanel.add(preDelimiterLine, "wrap");
		delimPanel.add(checkboxPanel);
		
		final String findDataStartText = "2) Select indices of first data cell:";
		JLabel findDataStartLabel = GUIFactory.createLabel(findDataStartText, 
				GUIFactory.FONTM_B);
		
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
		
		String note = "(Note: This may sometimes be inaccurate, "
				+ "especially if labels are numeric!)";
		JLabel noteLabel = GUIFactory.createLabel(note, GUIFactory.FONTS);
		
		findDataBtn = GUIFactory.createBtn("Auto-find labels!");
		
		indexPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);
		indexPanel.add(rowSpinnerLabel);
		indexPanel.add(rowDataStart);
		indexPanel.add(columnSpinnerLabel, "pushx");
		indexPanel.add(columnDataStart);
		indexPanel.add(noLabelCheckBox, "pushx, wrap");
		indexPanel.add(findDataBtn, "span 2 1, align left, pushx");
		indexPanel.add(noteLabel, "pushx, span 3 1");
		
		final String previewText = "Preview (25x25):";
		final JLabel preTableLine = GUIFactory.createLabel(previewText, 
				GUIFactory.FONTS_B);
		
		final JScrollPane scrollPane = new JScrollPane(dataTable);
		
		statusIndicator = new JLabel();
		setStatus(DataImportDialog.LABELS_HINT);
		
		proceedBtn = GUIFactory.createBtn("Proceed >");
		
		buttonPanel = GUIFactory.createJPanel(false, 
				GUIFactory.DEFAULT);
		buttonPanel.add(closeBtn, "align left, pushx");
		buttonPanel.add(statusIndicator);
		buttonPanel.add(proceedBtn, "align right");
		
		getRootPane().setDefaultButton(findDataBtn);
		
		mainPanel.add(delimPanel, "h :80:, push, wrap");
		
		mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL), "push, growx,"
				+ " wrap");
		
		mainPanel.add(findDataStartLabel, "push, wrap");
		mainPanel.add(indexPanel, "push, wrap");
		
		mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL), "push, growx,"
				+ " wrap");
		
		mainPanel.add(preTableLine, "wrap");
		mainPanel.add(scrollPane, "w :800:, h :600:, span, wrap");
		
		mainPanel.add(buttonPanel, "growx, push");
		
		mainPanel.revalidate();
		mainPanel.repaint();
	}
	
	public DataLoadInfo showDialog() {
		
		setVisible(true);
		return result;
	}
	
	/**
	 * Sets up the JCheckBoxes which are used to set delimiters for 
	 * file loading. 
	 */
	private void setupCheckBoxes() {
		
		noLabelCheckBox = new JCheckBox("No Labels");
		
		tabDelimCheckBox = new JCheckBox("Tab");
		delimiters.add(tabDelimCheckBox);
		
		commaDelimCheckBox = new JCheckBox("Comma");
		delimiters.add(commaDelimCheckBox);
		
		semicolonDelimCheckBox = new JCheckBox("Semicolon");
		delimiters.add(semicolonDelimCheckBox);
		
		spaceDelimCheckBox = new JCheckBox("Space");
		delimiters.add(spaceDelimCheckBox);
	}
	
	private void setInputDefaults() {
		
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
	
	public void updateTableData(String[][] previewData) {
		
		dataTable.setData(previewData);
		dataTable.repaint();
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
	
	public void addDataDetectListener(ActionListener l) {
		
		findDataBtn.addActionListener(l);
	}
	
	public void updateTableLabels(int maxRow, int maxCol) {
		
		dataTable.includeLabelsUpTo(maxRow, maxCol);
		dataTable.repaint();
	}
	
	public void addProceedBtnListener(ActionListener l) {
		
		proceedBtn.addActionListener(l);
	}
	
	public void addSpinnerListeners(ChangeListener l) {
		
		rowDataStart.addChangeListener(l);
		columnDataStart.addChangeListener(l);
	}
	
	public void addNoLabelListener(ItemListener l) {
		
		noLabelCheckBox.addItemListener(l);
	}
	
	public void addDelimCheckBoxesListener(ItemListener l) {
		
		tabDelimCheckBox.addItemListener(l);
		commaDelimCheckBox.addItemListener(l);
		spaceDelimCheckBox.addItemListener(l);
		semicolonDelimCheckBox.addItemListener(l);
	}
	
	public void setNoLabelSpinnerStatus() {
		
		boolean allowSpinners = !noLabelCheckBox.isSelected();
		
		if(allowSpinners) {
			rowDataStart.setEnabled(true);
			columnDataStart.setEnabled(true);
			int rowCount = (int) rowDataStart.getValue();
			int colCount = (int) columnDataStart.getValue();
			updateTableLabels(rowCount, colCount);
			
		} else {
			rowDataStart.setEnabled(false);
			columnDataStart.setEnabled(false);
			updateTableLabels(0, 0);
		}
	}
	
	public boolean allowsProceed() {
		
		boolean hasIdentifiedLabels = (int) rowDataStart.getValue() > 0 
				|| (int) columnDataStart.getValue() > 0 ;
		boolean hasLabels = !noLabelCheckBox.isSelected();
		
		return (hasIdentifiedLabels && hasLabels) || !hasLabels;
	}
	
	public void setStatus(final int status) {
		
		String message;
		Color color;
		
		switch(status) {
		case DataImportDialog.LABELS_READY:
			message = "Ready.";
			color = new Color(65, 173, 73); // dark green
			break;
		case DataImportDialog.LABELS_WARNING:
			message = "Please identify labels first!";
			color = Color.RED;
			break;
		default:
			message = "Identify your data's labels before proceeding.";
			color = Color.BLACK;
		}
		
		statusIndicator.setText(message);
		statusIndicator.setForeground(color);
	}
	
	public void setPreviewStatus() {
		
		int message;
		JButton defaultBtn;
		
		if(allowsProceed()) {
			message = DataImportDialog.LABELS_READY;
			defaultBtn = proceedBtn;
		} else {
			message = DataImportDialog.LABELS_HINT;
			defaultBtn = findDataBtn;
		}
		
		setStatus(message);
		getRootPane().setDefaultButton(defaultBtn);
	}
	
	public void setSpinnerValues(final int rowCount, final int columnCount) {
		
		rowDataStart.setValue(Integer.valueOf(rowCount));
		columnDataStart.setValue(Integer.valueOf(columnCount));
	}
	
	public void setResult(final String delimiter) {
		
		int rowNum = (Integer) rowDataStart.getValue();
		int colNum = (Integer) columnDataStart.getValue();
		
		this.result = new DataLoadInfo(new int[]{rowNum, colNum}, delimiter);
	}
	
	public JSpinner getRowStartSpinner() {
		
		return rowDataStart;
	}
	
	public JSpinner getColStartSpinner() {
		
		return columnDataStart;
	}
	
	public List<JCheckBox> getDelimiterList() {
		
		return delimiters;
	}
}
