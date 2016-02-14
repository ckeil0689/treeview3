package Views;

import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
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

	private JSpinner rowDataStart;
	private JSpinner columnDataStart;

	private final List<JCheckBox> delimiters;

	private JCheckBox tabDelimCheckBox;
	private JCheckBox commaDelimCheckBox;
	private JCheckBox semicolonDelimCheckBox;
	private JCheckBox spaceDelimCheckBox;

	public DataImportDialog(String filename) {

		super("Data Import - [" + filename + "]");
		
		this.delimiters = new ArrayList<JCheckBox>();
		
		// "Cancel" makes more sense here than "Close" - functionality remains
		closeBtn.setText("Cancel");
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

		final String delimText = "Step 1. Select delimiters for your dataset:";
		final JLabel preDelimiterLine = GUIFactory.createLabel(delimText,
				GUIFactory.FONTM_B);

		/* Delimiter panel */
		delimPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);
		checkboxPanel = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);

		checkboxPanel.add(tabDelimCheckBox, "push");
		checkboxPanel.add(commaDelimCheckBox, "push");
		checkboxPanel.add(semicolonDelimCheckBox, "push");
		checkboxPanel.add(spaceDelimCheckBox, "push");

		delimPanel.add(checkboxPanel);

		final String findDataStartText = "Step 2. Select indices of first "
				+ "data cell:";
		JLabel findDataStartLabel = GUIFactory.createLabel(findDataStartText,
				GUIFactory.FONTM_B);

		// must be ints for spinner listener
		SpinnerNumberModel indexModel = new SpinnerNumberModel(0, 0, 10, 1);
		SpinnerNumberModel indexModel2 = new SpinnerNumberModel(0, 0, 10, 1);

		final String rowSpinnerText = "Row #:";
		JLabel rowSpinnerLabel = GUIFactory.createLabel(rowSpinnerText,
				GUIFactory.FONTS);
		rowDataStart = new JSpinner(indexModel);

		final String columnSpinnerText = "Column #:";
		JLabel columnSpinnerLabel = GUIFactory.createLabel(columnSpinnerText,
				GUIFactory.FONTS);
		columnDataStart = new JSpinner(indexModel2);

		findDataBtn = GUIFactory.createBtn("Auto-detect labels");

		indexPanel = GUIFactory.createJPanel(false, GUIFactory.DEBUG);
		indexPanel.add(rowSpinnerLabel);
		indexPanel.add(rowDataStart, "al left, pushx");
		indexPanel.add(columnSpinnerLabel, "al right");
		indexPanel.add(columnDataStart, "wrap");
		indexPanel.add(findDataBtn, "span 2 1, align left, push");

		final String previewText = "Preview (25x25):";
		final JLabel preTableLine = GUIFactory.createLabel(previewText,
				GUIFactory.FONTS_B);

		final JScrollPane scrollPane = new JScrollPane(dataTable);

		proceedBtn = GUIFactory.createBtn("Proceed >");
		proceedBtn.requestFocus();

		buttonPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);
		buttonPanel.add(closeBtn, "pushx, align right");
		buttonPanel.add(proceedBtn, "align right");

		getRootPane().setDefaultButton(findDataBtn);

		mainPanel.add(preDelimiterLine, "push, wrap");
		mainPanel.add(delimPanel, "grow, push, wrap");

		mainPanel.add(findDataStartLabel, "push, wrap");
		mainPanel.add(indexPanel, "push, wrap");

		mainPanel.add(preTableLine, "wrap");
		mainPanel.add(scrollPane, "w :800:800, h :400:400, span, wrap");

		mainPanel.add(buttonPanel, "growx, push");

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	public DataLoadInfo showDialog() {

		setVisible(true);
		return result;
	}

	/**
	 * Sets up the JCheckBoxes which are used to set delimiters for file
	 * loading.
	 */
	private void setupCheckBoxes() {

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
	 * 
	 * @param Number
	 *            of columns.
	 * @return An array of numbers as Strings.
	 */
	private static String[] getColumnNames(int columnNum) {

		final int colNum = columnNum;
		String[] columnNames = new String[colNum];
		for (int i = 0; i < colNum; i++) {

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

	public void addDelimCheckBoxesListener(ItemListener l) {

		tabDelimCheckBox.addItemListener(l);
		commaDelimCheckBox.addItemListener(l);
		spaceDelimCheckBox.addItemListener(l);
		semicolonDelimCheckBox.addItemListener(l);
	}

	public void setPreviewStatus() {

		getRootPane().setDefaultButton(proceedBtn);
	}

	public void setSpinnerValues(final int rowCount, final int columnCount) {

		rowDataStart.setValue(Integer.valueOf(rowCount));
		columnDataStart.setValue(Integer.valueOf(columnCount));
	}

	public void setResult(final String delimiter) {

		int rowNum = (Integer) rowDataStart.getValue();
		int colNum = (Integer) columnDataStart.getValue();

		this.result = new DataLoadInfo(new int[] { rowNum, colNum }, delimiter);
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
