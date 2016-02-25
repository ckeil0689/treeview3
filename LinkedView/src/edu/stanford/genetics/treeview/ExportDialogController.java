package edu.stanford.genetics.treeview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
	}
	
	private class ExportListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			LogBuffer.println("Pressed export button!");
			/* For now, the selected indices of the 2 JComboBoxes */
			int[] selectedOptions = exportDialog.getSelectedOptions();

			if(selectedOptions.length < 5) {
				LogBuffer.println("Error: The number of options returned by " +
					"getSelectedOptions is [" + selectedOptions.length +
					"].  5 are required.  Unable to export.");
				return;
			}
			Format selFormat = Format.values()[selectedOptions[0]];
			PaperType selPaper  = PaperType.values()[selectedOptions[1]];
			Region selRegion = Region.values()[selectedOptions[2]];
			ExportAspect selAspect = ExportAspect.values()[selectedOptions[3]];
			boolean showSelections = (selectedOptions[4] == 1 ? true : false);

			// TODO call to event handler here, work with selected indices
			// and EXP_FORMATS / PAPER_TYPE enum classes

			ExportHandler eh = new ExportHandler(dendroView,interactiveXmap,
				interactiveYmap,colSelection,rowSelection);
			eh.setDefaultPageSize(selPaper);
			eh.setTileAspectRatio(selAspect);
			eh.export(selFormat,"Output." + selFormat.toString(),selRegion,
				showSelections);
		}
	}
	
}
