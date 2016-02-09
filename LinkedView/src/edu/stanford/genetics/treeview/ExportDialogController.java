package edu.stanford.genetics.treeview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;

public class ExportDialogController {

	private final ExportDialog exportDialog;
	private final DendroView dendroView;
	
	public ExportDialogController(final ExportDialog expD, 
			final DendroView dendroView) {
		
		this.exportDialog = expD;
		this.dendroView = dendroView;
		
		addListeners();
	}
	
	private void addListeners() {
		
		exportDialog.addExportListener(new ExportListener());
	}
	
	private class ExportListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			LogBuffer.println("Pressed export button!");
			/* For now, the selected indices of the 2 JComboBoxes */
			int[] selectedOptions = exportDialog.getSelectedOptions();
			
			// TODO call to event handler here, work with selected indices
			// and EXP_FORMATS / PAPER_TYPE enum classes
			
		}
		
		
	}
	
}
