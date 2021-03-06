package gui.export;

import gui.DendroView;
import gui.matrix.AspectType;
import gui.matrix.MapContainer;
import gui.window.TreeViewFrame;
import model.data.matrix.DataModel;
import model.data.trees.TreeSelectionI;
import model.export.ExportException;
import model.export.ExportHandler;
import model.export.ExportOptions;
import model.export.FormatType;
import model.export.RegionType;
import model.export.labels.ExportPreviewLabels;
import model.export.labels.LabelExportOption;
import model.export.matrix.ExportPreviewMatrix;
import model.export.trees.ExportPreviewTrees;
import util.LogBuffer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExportDialogController {

	private final ExportDialog exportDialog;
	private ExportOptions exportOptions;
	private final TreeViewFrame tvFrame;
	private final DendroView dendroView;
	private final MapContainer interactiveXmap;
	private final MapContainer interactiveYmap;
	private final TreeSelectionI colSelection;
	private final TreeSelectionI rowSelection;
	private DataModel model;
	private ExportHandler eh;

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
		this.eh = new ExportHandler(dendroView,
			interactiveXmap,interactiveYmap,colSelection,rowSelection);

		try {
			exportOptions = eh.getSetBestOptions();
		} catch(ExportException ee) {
			exportOptions = eh.getDefaultOptions();
			LogBuffer.println(ee.getLocalizedMessage());
			ee.printStackTrace();
			showWarning(ee.getLocalizedMessage());
			return;
		}

		//If the exported image is too large for BufferedImage to handle,
		//getSetBestOptions has already popped up a warning to the user and
		//returned null.  We just need to make sure the result is not null.
		if(exportOptions != null) {
			addListeners();

			//Make sure the radio buttons are properly enabled/disabled
			exportDialog.updateRegionRadioBtns(
				exportOptions.getFormatType().isDocumentFormat());

			setNewPreviewComponents(exportOptions);
			exportDialog.arrangePreviewPanel();
			updatePreview();
			try {
				exportDialog.setVisible(true);
			} catch(Exception e) {
				LogBuffer.println("Possible memory exception during " +
					"exportDialog.setVisible(true).");
				e.printStackTrace();
				throw e;
			}
	
			LogBuffer.println("ExportDialogController ready");
		}
	}

	/**
	 * Adds listeners to the GUI components in ExportDialog.
	 */
	private void addListeners() {

		exportDialog.addExportListener(new ExportListener());
		exportDialog.addFormatListener(new FormatListener());
		exportDialog.addRegionListener(new RegionListener());
		exportDialog.addAspectListener(new AspectListener());
		exportDialog.addItemStateListener(new RadioItemStateListener());
		exportDialog.addCheckBoxItemStateListener(new CheckItemStateListener());
		exportDialog.addRowLabelListener(new RowLabelListener());
		exportDialog.addColLabelListener(new ColLabelListener());
	}

	/**
	 * Set new components for displaying previews of trees and matrix in the
	 * model.export dialog.
	 * 
	 * @param withSelections - whether to show selections or not
	 */
	private void setNewPreviewComponents(final ExportOptions options) {

		//Obtain the dimensions of the exported image components
		eh.setOptions(options);
		eh.setCalculatedDimensions(exportOptions.getRegionType());

		//We need to get the model.export dimensions to update the model.export preview
		int height = eh.getYDim(exportOptions.getRegionType());
		int width = eh.getXDim(exportOptions.getRegionType());
		int treesHeight = eh.getTreesHeight();
		int rowLabelsLen =
			eh.getRowLabelPanelWidth(exportOptions.getRegionType(),
				exportOptions.getRowLabelOption());
		int colLabelsLen =
			eh.getColLabelPanelHeight(exportOptions.getRegionType(),
				exportOptions.getColLabelOption());
		int matrixHeight = height - eh.getColTreeAndGapLen() -
			eh.getColLabelAndGapLen();
		int matrixWidth = width - eh.getRowTreeAndGapLen() -
			eh.getRowLabelAndGapLen();

		ExportPreviewTrees expRowTrees = null;
		if(eh.isRowTreeIncluded()) {
			expRowTrees =
				tvFrame.getDendroView().getRowTreeSnapshot(
					options.isShowSelections(),options.getRegionType(),
					treesHeight,matrixHeight,
					(matrixWidth > matrixHeight ? matrixWidth : matrixHeight));
		}
		ExportPreviewTrees expColTrees = null;
		if(eh.isColTreeIncluded()) {
			expColTrees =
				tvFrame.getDendroView().getColTreeSnapshot(
					options.isShowSelections(),options.getRegionType(),
					matrixWidth,treesHeight,
					(matrixWidth > matrixHeight ? matrixWidth : matrixHeight));
		}
		ExportPreviewLabels expRowLabels = null;
		if(options.getRowLabelOption() != LabelExportOption.NO) {
			expRowLabels =
				tvFrame.getDendroView().getRowLabelsSnapshot(
					options.isShowSelections(),options.getRegionType(),
					rowLabelsLen,matrixHeight,
					options.getRowLabelOption() == LabelExportOption.SELECTION,
					eh.getTileHeight(),
					eh.getLabelAreaHeight() - ExportHandler.SQUEEZE,
					(matrixWidth > matrixHeight ? matrixWidth : matrixHeight));
		}
		ExportPreviewLabels expColLabels = null;
		if(options.getColLabelOption() != LabelExportOption.NO) {
			expColLabels =
				tvFrame.getDendroView().getColLabelsSnapshot(
					options.isShowSelections(),options.getRegionType(),
					matrixWidth,colLabelsLen,
					options.getColLabelOption() == LabelExportOption.SELECTION,
					eh.getTileWidth(),
					eh.getLabelAreaHeight() - ExportHandler.SQUEEZE,
					(matrixWidth > matrixHeight ? matrixWidth : matrixHeight));
		}
		ExportPreviewMatrix expMatrix =
			tvFrame.getDendroView().getMatrixSnapshot(
				options.isShowSelections(),options.getRegionType());

		exportDialog.setPreviewComponents(expRowTrees,expColTrees,expRowLabels,
			expColLabels,expMatrix);
	}

	/**
	 * Describes the actions used to produce an model.export.
	 */
	private class ExportListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			if(arg0.getSource() != exportDialog.getExportButton()) {
				return;
			}

			//This will open a file chooser dialog
			String exportFilename =
				chooseSaveFile(exportOptions.getFormatType());

			//If the returned string is null or empty, they either canceled or
			//there was an error
			if((exportFilename == null) || exportFilename.isEmpty()) {
				LogBuffer.println("Could not model.export. A file name could " +
					"not be created.");
				return;
			} else {
				LogBuffer.println("Exporting to file: [" + exportFilename +
					"]");
			}

			//Now model.export the file
			try {
				ExportHandler eh = new ExportHandler(dendroView,
					interactiveXmap,interactiveYmap,colSelection,rowSelection);
				eh.setOptions(exportOptions);
				eh.setCalculatedDimensions(exportOptions.getRegionType());
				boolean exportSucess = eh.export(exportOptions.getFormatType(),
					exportFilename,
					exportOptions.getRegionType(),
					exportOptions.isShowSelections(),
					exportOptions.getRowLabelOption(),
					exportOptions.getColLabelOption());

				if(exportSucess) {
					String msg = "Exported file: [" + exportFilename + "].";
					LogBuffer.println(msg);

					exportDialog.dispose();

					//Open the file in the default system app
					Desktop.getDesktop().open(new File(exportFilename));
				} else {
					String msg = "Could not model.export the file: [" +
						exportFilename + "].";
					LogBuffer.println(msg);
				}
			}
			catch(Exception iae) {
				LogBuffer.logException(iae);
				showWarning(iae.getLocalizedMessage());
			}
		}
	}

	public String chooseSaveFile(FormatType selFormat) {

		String chosen = null;

		final FileDialog fileDialog = new FileDialog(exportDialog,
			"Save Exported File",
			FileDialog.SAVE);

		//Set the default initial output file name and location to that of the
		//input file
		try {
			File inFile = new File(model.getSource());
			File outFile = new File(getInitialExportFileString(selFormat));
			fileDialog.setDirectory(inFile.getParentFile().getCanonicalPath());
			fileDialog.setFile(outFile.getName());

		}
		catch(Exception e) {
			e.printStackTrace();
		}

		fileDialog.setVisible(true);

		//Retrieve the chosen output file
		final String outdir = fileDialog.getDirectory();
		final String filename = fileDialog.getFile();
		if((outdir != null) && (filename != null)) {
			chosen = outdir + filename;
		}

		return(chosen);
	}

	public String getInitialExportFileString(FormatType selFormat) {
		String exportFilename = model.getSource();
		if((exportFilename == null) || exportFilename.isEmpty()) {
			exportFilename = "TreeView3_exported_file";
		}
		exportFilename += "." + selFormat.toString();
		return(exportFilename);
	}

	private void showWarning(final String message) {

		JOptionPane.showMessageDialog(tvFrame.getAppFrame(),
			message,"Warning",JOptionPane.WARNING_MESSAGE);
		LogBuffer.println(message);
	}

	/**
	 *
	 *
	 */
	private class FormatListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			FormatType selFormat = exportOptions.getFormatType();

			exportDialog.getPaperBox().setEnabled(selFormat.isDocumentFormat());
			exportDialog.getOrientBox().setEnabled(
				selFormat.isDocumentFormat());

			ExportHandler eh = new ExportHandler(tvFrame.getDendroView(),
				interactiveXmap,interactiveYmap,tvFrame.getColSelection(),
				tvFrame.getRowSelection());

			List<RegionType> tooBigs = new ArrayList<RegionType>();
			tooBigs = eh.getOversizedRegions(true,selFormat.isDocumentFormat());

			exportDialog.setBigRegs(tooBigs);
			exportDialog.updateRegionRadioBtns(selFormat.isDocumentFormat());
		}
	}

	private class RegionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			FormatType selFormat = exportOptions.getFormatType();
			RegionType selRegion = exportOptions.getRegionType();

			exportDialog.updateAspectRadioBtns(selFormat.isDocumentFormat(),
				selRegion);
		}
	}

	private class AspectListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			FormatType selFormat = exportOptions.getFormatType();
			RegionType selRegion = exportOptions.getRegionType();
			AspectType selAspect = exportOptions.getAspectType();

			exportDialog.updateLabelBtns(selFormat.isDocumentFormat(),
				selRegion,selAspect);
		}
	}

	private class RowLabelListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			FormatType selFormat = exportOptions.getFormatType();
			RegionType selRegion = exportOptions.getRegionType();
			AspectType selAspect = exportOptions.getAspectType();
			LabelExportOption selRows = exportOptions.getRowLabelOption();

			//We might need to enable/disable column label buttons upon change
			//of the row label button selection
			exportDialog.updateColLabelBtns(selFormat.isDocumentFormat(),
				selRegion,selAspect,selRows);
		}
	}
 
	private class ColLabelListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			FormatType selFormat = exportOptions.getFormatType();
			RegionType selRegion = exportOptions.getRegionType();
			AspectType selAspect = exportOptions.getAspectType();
			LabelExportOption selCols = exportOptions.getColLabelOption();

			//We might need to enable/disable row label buttons upon change
			//of the column label button selection
			exportDialog.updateRowLabelBtns(selFormat.isDocumentFormat(),
				selRegion,selAspect,selCols);
		}
	}

	/**
	 * Whenever the selection state of any GUI component which has this listener
	 * attached will be changed, it causes the model.export preview to update.
	 * For example, if the user checks the 'Show selections' JCheckBox,
	 * the update will invoke the recreation of the preview components so
	 * that selections will be drawn as well.
	 */
	private class RadioItemStateListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent event) {

			// Don't update when deselected or highlighted...
			if(event.getStateChange() == ItemEvent.SELECTED) {
				updatePreview();
			}
		}

	}

	/**
	 * Whenever the selection state of any GUI component which has this listener
	 * attached will be changed, it causes the model.export preview to update.
	 * For example, if the user checks the 'Show selections' JCheckBox,
	 * the update will invoke the recreation of the preview components so
	 * that selections will be drawn as well.
	 */
	private class CheckItemStateListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent event) {

			// Don't update when highlighted...
			if((event.getStateChange() == ItemEvent.SELECTED)
				|| (event.getStateChange() == ItemEvent.DESELECTED)) {
				updatePreview();
			}
		}

	}

	/**
	 * Update the preview panel of the model.export dialog according to selected
	 * options.
	 */
	private void updatePreview() {

		if(exportDialog == null) {
			LogBuffer.println("No exportDialog object defined. Could not " +
				"update preview components.");
			return;
		}

		exportDialog.retrieveOptions(exportOptions);
		setNewPreviewComponents(exportOptions);

		exportDialog.updatePreviewComponents(exportOptions);
	}
}
