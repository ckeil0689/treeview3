package Views;

import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.ModelLoader;

public class DataImportController {

	
	private final DataImportDialog previewDialog;
	private List<Integer> activeDelimiters; 
	private FileSet fileSet;
	
	public DataImportController(DataImportDialog dialog) {
		
		this.previewDialog = dialog;
	
	}
	
	public void setDialogVisible() {
		
		previewDialog.setupDialogComponents();
		addAllListeners();
		previewDialog.setVisible(true);
	}
	
	private void addAllListeners() {
		
		previewDialog.addDelimCheckBoxesListener(new DelimiterListener());
		previewDialog.addSpinnerListeners(new LabelIncludeListener());
	}
	
	public void setFileSet(FileSet fs) {
		
		this.fileSet = fs;
	}
	
	public void loadPreviewData() {
		
		String[][] previewData;
		
		if(fileSet == null) {
			LogBuffer.println("No fileSet specified to load preview data.");
			return;
		}
		
		String filename = fileSet.getCdt(); 
		previewData = ModelLoader.loadPreviewData(filename); // TODO add delimiters here
		
		if(previewData == null || previewData.length == 0) {
			LogBuffer.println("No preview data could be loaded.");
			return;
		}
		
		previewDialog.setNewTable(previewData);
	}
	
	/**
	 * Listens to changes in selection and initiates a new data loading
	 * process.  
	 * @author chris0689
	 *
	 */
	private class DelimiterListener implements ChangeListener{

		@Override
		public void stateChanged(ChangeEvent e) {
			
			JCheckBox checkbox = (JCheckBox)e.getSource();
			
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
		}
	}
}
