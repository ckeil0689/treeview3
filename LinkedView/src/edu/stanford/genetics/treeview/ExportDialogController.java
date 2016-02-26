package edu.stanford.genetics.treeview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;

import Controllers.ExportHandler;
import Controllers.Format;
import Controllers.Region;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;

public class ExportDialogController {

	private final ExportDialog exportDialog;
	private final DendroView dendroView;
	private final MapContainer interactiveXmap;
	private final MapContainer interactiveYmap;
	private final TreeSelectionI colSelection;
	private final TreeSelectionI rowSelection;

	public ExportDialogController(final ExportDialog expD, 
			final DendroView dendroView,final MapContainer interactiveXmap,
			final MapContainer interactiveYmap,
			final TreeSelectionI colSelection,
			final TreeSelectionI rowSelection) {
		
		this.exportDialog = expD;
		this.dendroView = dendroView;
		this.interactiveXmap = interactiveXmap;
		this.interactiveYmap = interactiveYmap;
		this.colSelection = colSelection;
		this.rowSelection = rowSelection;

		addListeners();
	}

	private void addListeners() {
		
		exportDialog.addExportListener(new ExportListener());
		exportDialog.addFormatListener(new FormatListener());
	}
	
	private class ExportListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			LogBuffer.println("Pressed export button!");
			/* For now, the selected indices of the 2 JComboBoxes */
			int[] selectedOptions = exportDialog.getSelectedOptions();

			Format selFormat;
			if(selectedOptions.length < 1 || selectedOptions[0] < 0 ||
				selectedOptions[0] >= Format.values().length) {
				selFormat = Format.getDefault();
			} else {
				selFormat = Format.values()[selectedOptions[0]];
			}

			PaperType selPaper;
			if(selectedOptions.length < 2 || selectedOptions[1] < 0 ||
				selectedOptions[1] >= PaperType.values().length) {
				selPaper = PaperType.getDefault();
			} else {
				selPaper = PaperType.values()[selectedOptions[1]];
			}

			Region selRegion;
			if(selectedOptions.length < 3 || selectedOptions[2] < 0 ||
				selectedOptions[2] >= Region.values().length) {
				selRegion = Region.getDefault();
			} else {
				selRegion = Region.values()[selectedOptions[2]];
			}

			ExportAspect selAspect;
			if(selectedOptions.length < 4 || selectedOptions[3] < 0 ||
				selectedOptions[3] >= ExportAspect.values().length) {
				selAspect = ExportAspect.getDefault();
			} else {
				selAspect = ExportAspect.values()[selectedOptions[3]];
			}

			boolean showSelections;
			if(selectedOptions.length < 5) {
				showSelections = false;
			} else {
				showSelections = (selectedOptions[4] == 1 ? true : false);
			}

			// TODO call to event handler here, work with selected indices
			// and EXP_FORMATS / PAPER_TYPE enum classes

			ExportHandler eh = new ExportHandler(dendroView,interactiveXmap,
				interactiveYmap,colSelection,rowSelection);
			eh.setDefaultPageSize(selPaper);
			eh.setTileAspectRatio(selAspect);
			eh.export(selFormat,"Output." + selFormat.toString(),selRegion,
				showSelections);

			exportDialog.dispose();
		}
	}

	private class FormatListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			LogBuffer.println("Format changed!");
			/* For now, the selected indices of the 2 JComboBoxes */
			int[] selectedOptions = exportDialog.getSelectedOptions();

			Format selFormat;
			if(selectedOptions.length < 1 || selectedOptions[0] < 0 ||
				selectedOptions[0] >= Format.values().length) {
				selFormat = Format.getDefault();
			} else {
				selFormat = Format.values()[selectedOptions[0]];
			}

			exportDialog.getPaperBox().setEnabled(selFormat.isDocumentFormat());
		}
	}
}
