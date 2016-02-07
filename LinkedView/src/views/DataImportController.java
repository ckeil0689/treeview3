package views;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.PreviewLoader;

public class DataImportController {

	private final static String TAB_DELIM = "\\t";
	private final static String COMMA_DELIM = ",";
	private final static String SEMICOLON_DELIM = ";";
	private final static String SPACE_DELIM = "\\s";

	private final DataImportDialog previewDialog;
	private FileSet fileSet;
	private String selectedDelimiter;

	public DataImportController(final String selectedDelimiter) {

		this.previewDialog = null;
		this.selectedDelimiter = selectedDelimiter;
	}
	
	public DataImportController(DataImportDialog dialog) {

		this.previewDialog = dialog;
		this.selectedDelimiter = DataImportController.TAB_DELIM;
	}

	public void initDialog() {

		previewDialog.setupDialogComponents();
		addAllListeners();
	}

	private void addAllListeners() {

		previewDialog.addProceedBtnListener(new ProceedListener());
		previewDialog.addDelimCheckBoxesListener(new DelimiterListener());
		previewDialog.addSpinnerListeners(new LabelIncludeListener());
		previewDialog.addNoLabelListener(new NoLabelListener());
		previewDialog.addDataDetectListener(new DataDetectionListener());
	}

	public void setFileSet(FileSet fs) {

		this.fileSet = fs;
	}

	public String[][] loadPreviewData() {

		String[][] previewData;

		if (fileSet == null) {
			LogBuffer.println("No fileSet specified to load preview data.");
			return new String[][] { { "N/ A" } };
		}

		String filename = fileSet.getCdt();
		previewData = PreviewLoader
				.loadPreviewData(filename, selectedDelimiter);

		if (previewData == null || previewData.length == 0) {
			LogBuffer.println("No preview data could be loaded.");
			return new String[][] { { "N/ A" } };
		}

		return previewData;
	}

	private void updateSelectedDelimiter() {

		selectedDelimiter = "";
		int addCount = 0;

		List<JCheckBox> delimiters = previewDialog.getDelimiterList();

		for (JCheckBox cb : delimiters) {

			if (cb.isSelected()) {
				final int idx = delimiters.indexOf(cb);
				if (addCount++ > 0) {
					selectedDelimiter += "|" + getDelimiter(idx);
				} else {
					selectedDelimiter += getDelimiter(idx);
				}
			}
		}
	}

	private static String getDelimiter(final int idx) {

		String delim;
		switch (idx) {
		case 1:
			delim = DataImportController.COMMA_DELIM;
			break;
		case 2:
			delim = DataImportController.SEMICOLON_DELIM;
			break;
		case 3:
			delim = DataImportController.SPACE_DELIM;
			break;
		default:
			delim = DataImportController.TAB_DELIM;
		}

		return delim;
	}

	private class ProceedListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {

			if (previewDialog.allowsProceed()) {
				previewDialog.setResult(selectedDelimiter);
				previewDialog.setVisible(false);
				previewDialog.dispose();
			}

			previewDialog.setStatus(DataImportDialog.LABELS_WARNING);
		}
	}

	private class DataDetectionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			
			detectDataBoundaries();
		}
	}
	
	/**
	 * Initiates data boundary detection with currently set fileSet object
	 * and the selectedDelimiter. Then it updates the JSpinner values which
	 * describe the data boundaries in the dialog.
	 */
	public int[] detectDataBoundaries() {
		
		String filename = fileSet.getCdt();
		int[] dataStartCoords = PreviewLoader.findDataStartCoords(filename,
				selectedDelimiter);

		/* This method can be called without the actual dialog being open! */
		if(previewDialog != null) {
			int rowCount = dataStartCoords[0];
			int columnCount = dataStartCoords[1];
			previewDialog.setSpinnerValues(rowCount, columnCount);
		}
		
		return dataStartCoords;
	}

	/**
	 * Listens to changes in selection and initiates a new data loading process.
	 * 
	 * @author chris0689
	 *
	 */
	private class DelimiterListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent e) {

			updateSelectedDelimiter();
			String[][] newData = loadPreviewData();
			previewDialog.updateTableData(newData);
		}
	}

	private class LabelIncludeListener implements ChangeListener {

		@Override
		public void stateChanged(final ChangeEvent e) {

			int maxRow = (Integer) previewDialog.getRowStartSpinner()
					.getValue();
			int maxCol = (Integer) previewDialog.getColStartSpinner()
					.getValue();

			previewDialog.updateTableLabels(maxRow, maxCol);
			previewDialog.setPreviewStatus();
		}
	}

	private class NoLabelListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent e) {

			previewDialog.setNoLabelSpinnerStatus();
			previewDialog.setPreviewStatus();
		}
	}
}
