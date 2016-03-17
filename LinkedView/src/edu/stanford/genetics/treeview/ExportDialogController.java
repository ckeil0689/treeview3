package edu.stanford.genetics.treeview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.freehep.graphicsio.PageConstants;

import Controllers.ExportHandler;
import Controllers.Format;
import Controllers.Region;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;

public class ExportDialogController {

	private final ExportDialog exportDialog;
	private final TreeViewFrame tvFrame;
	private final DendroView dendroView;
	private final MapContainer interactiveXmap;
	private final MapContainer interactiveYmap;
	private final TreeSelectionI colSelection;
	private final TreeSelectionI rowSelection;
	private DataModel model;

	public ExportDialogController(final ExportDialog expD, 
			final TreeViewFrame tvFrame,final MapContainer interactiveXmap,
			final MapContainer interactiveYmap,final DataModel model) {
		
		this.exportDialog = expD;
		this.tvFrame = tvFrame;
		this.dendroView = tvFrame.getDendroView();
		this.interactiveXmap = interactiveXmap;
		this.interactiveYmap = interactiveYmap;
		this.colSelection = tvFrame.getColSelection();
		this.rowSelection = tvFrame.getRowSelection();
		this.model = model;

		addListeners();
	}

	private void addListeners() {
		
		exportDialog.addExportListener(new ExportListener());
		exportDialog.addFormatListener(new FormatListener());
		exportDialog.addRegionListener(new RegionListener());
	}
	
	private class ExportListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

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

			String selOrient;
			if(selectedOptions.length < 6 || selectedOptions[5] < 0 ||
				selectedOptions[5] >=
				PageConstants.getOrientationList().length) {

				selOrient = PageConstants.LANDSCAPE;
			} else {
				selOrient =
					PageConstants.getOrientationList()[selectedOptions[5]];
			}

			String exportFilename = model.getSource();
			if(exportFilename == null || exportFilename.isEmpty()) {
				exportFilename = "TreeView3_exported_file";
			}
			exportFilename += "." + selFormat.toString();

			try {
				ExportHandler eh = new ExportHandler(dendroView,interactiveXmap,
					interactiveYmap,colSelection,rowSelection);
				eh.setDefaultPageSize(selPaper);
				eh.setDefaultPageOrientation(selOrient);
				eh.setTileAspectRatio(selAspect);
				eh.export(selFormat,exportFilename,selRegion,showSelections);

				String msg = "Exported file: [" + exportFilename + "].";
				LogBuffer.println(msg);
				showDialog(msg);

				exportDialog.dispose();
			} catch(OutOfMemoryError oome) {
				showWarning("ERROR: Out of memory.  Note, you may be able to " +
					"export a smaller portion of the matrix.");
			} catch(Exception iae) {
				showWarning(iae.getLocalizedMessage());
			}
		}
	}

	private void showWarning(final String message) {

		JOptionPane.showMessageDialog(tvFrame.getAppFrame(), 
				message, "Warning", JOptionPane.WARNING_MESSAGE);
		LogBuffer.println(message);
	}

	private void showDialog(final String message) {

		JOptionPane.showMessageDialog(tvFrame.getAppFrame(), 
				message, "Note", JOptionPane.INFORMATION_MESSAGE);
		LogBuffer.println(message);
	}

	private class FormatListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

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
			exportDialog.getOrientBox().setEnabled(
				selFormat.isDocumentFormat());

			ExportHandler eh = new ExportHandler(tvFrame.getDendroView(),
				interactiveXmap,interactiveYmap,tvFrame.getColSelection(),
				tvFrame.getRowSelection());
			List<Region> tooBigs = new ArrayList<Region>();
			if(!selFormat.isDocumentFormat()) {
				final boolean useMinimums = true;
				tooBigs = eh.getRegionsThatAreTooBig(useMinimums);
			}
			exportDialog.setBigRegs(tooBigs);
			exportDialog.updateRegionRadioBtns(selFormat.isDocumentFormat());
		}
	}

	private class RegionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			/* For now, the selected indices of the 2 JComboBoxes */
			int[] selectedOptions = exportDialog.getSelectedOptions();

			Format selFormat;
			if(selectedOptions.length < 1 || selectedOptions[0] < 0 ||
				selectedOptions[0] >= Format.values().length) {
				selFormat = Format.getDefault();
			} else {
				selFormat = Format.values()[selectedOptions[0]];
			}

			Region selRegion;
			if(selectedOptions.length < 3 || selectedOptions[2] < 0 ||
				selectedOptions[2] >= Region.values().length) {

				selRegion = Region.getDefault();
			} else {
				selRegion = Region.values()[selectedOptions[2]];
			}

			exportDialog.updateAspectRadioBtns(selFormat.isDocumentFormat(),
				selRegion);
		}
	}
}
