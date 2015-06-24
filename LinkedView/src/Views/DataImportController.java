package Views;

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
import edu.stanford.genetics.treeview.model.ModelLoader;

public class DataImportController {

	private final static String TAB_DELIM = "\\t"; 
	private final static String COMMA_DELIM = ",";
	private final static String SEMICOLON_DELIM = ";";
	private final static String SPACE_DELIM = "\\s";
	
	private final DataImportDialog previewDialog;
	private FileSet fileSet;
	private String selectedDelimiter;
	
	public DataImportController(DataImportDialog dialog) {
		
		this.previewDialog = dialog;
		this.selectedDelimiter = DataImportController.TAB_DELIM;
	}
	
	public void setDialogVisible() {
		
		previewDialog.setupDialogComponents();
		addAllListeners();
		previewDialog.setVisible(true);
	}
	
	private void addAllListeners() {
		
		previewDialog.addProceedBtnListener(new ProceedListener());
		previewDialog.addDelimCheckBoxesListener(new DelimiterListener());
		previewDialog.addSpinnerListeners(new LabelIncludeListener());
		previewDialog.addNoLabelListener(new NoLabelListener());
	}
	
	public void setFileSet(FileSet fs) {
		
		this.fileSet = fs;
	}
	
	public String[][] loadPreviewData() {
		
		String[][] previewData;
		
		if(fileSet == null) {
			LogBuffer.println("No fileSet specified to load preview data.");
			return new String[][]{{"N/ A"}};
		}
		
		String filename = fileSet.getCdt(); 
		previewData = ModelLoader.loadPreviewData(filename, selectedDelimiter);
		
		if(previewData == null || previewData.length == 0) {
			LogBuffer.println("No preview data could be loaded.");
			return new String[][]{{"N/ A"}};
		}
		
		return previewData;
	}
	
	private void setPreviewStatus() {
		
		if(previewDialog.allowsProceed()) {
			previewDialog.setStatus(DataImportDialog.LABELS_READY);
		} else {
			previewDialog.setStatus(DataImportDialog.LABELS_HINT);
		}
	}
	
	private void updateSelectedDelimiter() {
		
		selectedDelimiter = "";
		
		List<JCheckBox> delimiters = previewDialog.getDelimiterList();
		
		for(JCheckBox cb : delimiters) {
			
			if(cb.isSelected()) {
				if(delimiters.indexOf(cb) > 0) {
					selectedDelimiter += "|" + getDelimiter(delimiters.indexOf(cb));
				} else {
					selectedDelimiter += getDelimiter(delimiters.indexOf(cb));
				}
			}
		}
		
		LogBuffer.println("New selectedDelimiter: " + selectedDelimiter);
	}
	
	private static String getDelimiter(final int idx) {
		
		String delim;
		switch(idx) {
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
			
			if(previewDialog.allowsProceed()) {
				LogBuffer.println("Proceed with load...");
			} 
			
			previewDialog.setStatus(DataImportDialog.LABELS_WARNING);
		}
	}
	
	/**
	 * Listens to changes in selection and initiates a new data loading
	 * process.  
	 * @author chris0689
	 *
	 */
	private class DelimiterListener implements ItemListener{

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
			setPreviewStatus();
		}
	}
	
	private class NoLabelListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent e) {
			
			previewDialog.setNoLabelSpinnerStatus();
			setPreviewStatus();
		}
	}
}
