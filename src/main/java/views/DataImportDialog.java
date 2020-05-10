package views;

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

import util.CustomDialog;
import util.GUIFactory;
import edu.stanford.genetics.treeview.model.DataLoadInfo;
import edu.stanford.genetics.treeview.model.PreviewDataTable;
import edu.stanford.genetics.treeview.model.PreviewLoader;

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

	/** Sets up the GUI for the data import dialog. */
	@Override
	protected void setupLayout() {

		JPanel delimPanel;
		JPanel checkboxPanel;
		JPanel indexPanel;
		JPanel buttonPanel;

		final String delimText = "Column delimiter:";
		final JLabel preDelimiterLine = GUIFactory.createBoldLabel(delimText);

		/* Delimiter panel */
		delimPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);
		checkboxPanel = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);

		checkboxPanel.add(tabDelimCheckBox, "push");
		checkboxPanel.add(commaDelimCheckBox, "push");
		checkboxPanel.add(semicolonDelimCheckBox, "push");
		checkboxPanel.add(spaceDelimCheckBox, "push");

		delimPanel.add(checkboxPanel);

		final String findDataStartText = "First data cell:";
		JLabel findDataStartLabel = GUIFactory.createBoldLabel(findDataStartText);

		// must be ints for spinner listener
		SpinnerNumberModel indexModel = new SpinnerNumberModel(1, 1,
			PreviewLoader.PREVIEW_LIMIT,1);
		SpinnerNumberModel indexModel2 = new SpinnerNumberModel(1, 1,
			PreviewLoader.PREVIEW_LIMIT,1);

		final String rowSpinnerText = "Row:";
		JLabel rowSpinnerLabel = GUIFactory.createLabel(rowSpinnerText);
		rowDataStart = new JSpinner(indexModel);

		final String columnSpinnerText = "Column:";
		JLabel columnSpinnerLabel = GUIFactory.createLabel(columnSpinnerText);
		columnDataStart = new JSpinner(indexModel2);

		findDataBtn = GUIFactory.createBtn("Auto-detect labels");

		indexPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);
		indexPanel.add(rowSpinnerLabel);
		indexPanel.add(rowDataStart, "al left, pushx 5");
		indexPanel.add(columnSpinnerLabel, "al right");
		indexPanel.add(columnDataStart, "pushx 5");
		indexPanel.add(findDataBtn, "pushx");

		final String previewText = "Preview (" + PreviewLoader.PREVIEW_LIMIT +
			"x" + PreviewLoader.PREVIEW_LIMIT + "):";
		final JLabel preTableLine = GUIFactory.createBoldLabel(previewText);

		final JScrollPane scrollPane = new JScrollPane(dataTable);

		proceedBtn = GUIFactory.createBtn("Continue");
		proceedBtn.requestFocus();

		buttonPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);
		buttonPanel.add(closeBtn, "pushx, align right");
		buttonPanel.add(proceedBtn, "align right");

		getRootPane().setDefaultButton(findDataBtn);

		mainPanel.add(preDelimiterLine);
		mainPanel.add(delimPanel, "al left, w 80%, wrap");

		mainPanel.add(findDataStartLabel);
		mainPanel.add(indexPanel, "al left, w 80%, wrap");

		mainPanel.add(preTableLine, "wrap");
		mainPanel.add(scrollPane, "w :800:800, h :400:400, span, wrap");

		mainPanel.add(buttonPanel, "al right, span, push");

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	public DataLoadInfo showDialog() {

		setVisible(true);
		return result;
	}

	/** Sets up the JCheckBoxes which are used to set delimiters for file
	 * loading. */
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

	/** Set up the JTable which will contain the preview data. */
	public void setNewTable(String[][] previewData) {

		String[] columnNames = getColumnNames(previewData[0].length);
		this.dataTable = new PreviewDataTable(previewData, columnNames);
	}

	public void updateTableData(String[][] previewData) {

		dataTable.setData(previewData);
		dataTable.repaint();
	}

	/** Generates an array of Strings which serve as headers for the JTable
	 * constructor.
	 * 
	 * @param Number
	 *          of columns.
	 * @return An array of numbers as Strings. */
	private static String[] getColumnNames(final int colNum) {

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
	
	/**
	 * Adds an action to the Cancel (or Close) button. 
	 * @param l The ActionListener to be added.
	 */
	public void addCancelBtnListener(ActionListener l) {

		closeBtn.addActionListener(l);
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

	/** Takes the number of rows & columns that are label rows/columns and sets
	 * the column & row number (numbered from 1) which represents the first data
	 * cell (i.e. non-label row/col). Note that the spinners reflect the "First
	 * Data Cell".
	 * 
	 * @param rowCount - number of rows (from the top) containing labels. Also
	 *          can be interpreted as the index of the first row of
	 *          data
	 * @param columnCount - number of columns (from the left) containing labels
	 *          Also can be interpreted as the index of the first
	 *          column of data **/
	public void setSpinnerValues(final int rowCount, final int columnCount) {

		rowDataStart.setValue(Integer.valueOf(rowCount) + 1);
		columnDataStart.setValue(Integer.valueOf(columnCount) + 1);
	}

	public void setDelimiterForResult(final String delimiter) {

		int rowNum = getRowSpinnerDataIndex();
		int colNum = getColSpinnerDataIndex();

		this.result = new DataLoadInfo(new int[] {rowNum, colNum}, delimiter);
	}

	/** Sets the result object to null. This is useful for returning 
	 * a null object in case the user cancelled the import dialog.
	 */
	public void clearResult() {
		
		this.result = null;
	}
	
	/** Calculates and returns the data index of the first row containing data
	 * 
	 * @return int */
	public int getRowSpinnerDataIndex() {

		return((Integer) rowDataStart.getValue() - 1);
	}

	/** Calculates and returns the data index of the first column containing data
	 * 
	 * @return int */
	public int getColSpinnerDataIndex() {

		return((Integer) columnDataStart.getValue() - 1);
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
