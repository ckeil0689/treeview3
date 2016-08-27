package edu.stanford.genetics.treeview;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;

import org.freehep.graphicsio.PageConstants;

import Controllers.ExportHandler;
import Controllers.FormatType;
import Controllers.RegionType;
import Utilities.CustomDialog;
import Utilities.GUIFactory;
import Controllers.ExportHandler.ExportWorker;

public class ExportDialog extends CustomDialog {

	private static final long serialVersionUID = 1L;
	
	private static final int PAPER_LONGSIDELEN = 450;

	private JPanel previewComp;
	private JPanel background;
	
	private int bgWidth;
	private int bgHeight;
	private int gapsize;
	
	private ExportPreviewTrees rowPrevTrees;
	private ExportPreviewTrees colPrevTrees;
	private ExportPreviewMatrix matrix;
	
	private JComboBox<FormatType> formatBox;
	private JComboBox<PaperType> paperBox;
	private JComboBox<String> orientBox;
	private ButtonGroup regionRadioBtns;
	private ButtonGroup aspectRadioBtns;
	private JCheckBox selectionsBox;
	private JButton exportBtn;
	private List<RegionType> bigRegs; //List of regions that are too big for image export (doc export is OK)
	private boolean selectionsExist;
	private ExportHandler eh;

	public ExportDialog(final boolean selectionsExist, final ExportHandler eh) {
		
		super("Export");
		this.eh = eh;
		final boolean useMinimums = true;
		this.bigRegs = eh.getOversizedRegions(useMinimums);
		this.selectionsExist = selectionsExist;
		
		this.gapsize = 5; // default
		
		this.bgWidth = PAPER_LONGSIDELEN;
		this.bgHeight = PAPER_LONGSIDELEN;
		
		setupLayout();
	}

	@Override
	protected void setupLayout() {
		
		this.mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);

		JPanel contentPanel = GUIFactory.createJPanel(false, 
				GUIFactory.DEFAULT);
		JPanel optionsPanel = GUIFactory.createJPanel(false, 
				GUIFactory.DEFAULT);

		/* Shows the preview */
		JPanel previewPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_INSETS);
		previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));
		
		this.previewComp = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);
		this.background = GUIFactory.createJPanel(true, GUIFactory.NO_INSETS);
		
		JLabel format = GUIFactory.createLabel("Format:",GUIFactory.FONTS);
		JLabel paper = GUIFactory.createLabel("Paper Size:",GUIFactory.FONTS);
		JLabel matrix = GUIFactory.createLabel("Matrix:",GUIFactory.FONTS);
		JLabel aspect = GUIFactory.createLabel("Tile Aspect:",GUIFactory.FONTS);
		JLabel orient = GUIFactory.createLabel("Orientation:",GUIFactory.FONTS);

		FormatType selectedFormat = FormatType.getDefault();
		if(eh.isImageExportPossible()) {
			this.formatBox = new JComboBox<FormatType>(FormatType
					.getHiResFormats());
			formatBox.setSelectedItem(FormatType.getDefault());
			
		} else {
			selectedFormat = FormatType.getDefaultDocumentFormat();
			formatBox = new JComboBox<FormatType>(FormatType.getDocumentFormats());
			formatBox.setSelectedItem(FormatType.getDefaultDocumentFormat());
			formatBox.setToolTipText("All regions too big for PNG/JPG/PPM " +
				"export");
		}
		
		this.paperBox = new JComboBox<PaperType>(PaperType.values());
		paperBox.setSelectedItem(PaperType.getDefault());
		paperBox.setEnabled(selectedFormat.isDocumentFormat());
		
		this.regionRadioBtns = new ButtonGroup();
		this.aspectRadioBtns = new ButtonGroup();
		
		this.selectionsBox = new JCheckBox("Show Selections");
		selectionsBox.setEnabled(selectionsExist);
		if(!selectionsExist) {
			selectionsBox.setToolTipText("No data selected");
		}
		
		this.orientBox = new JComboBox<String>(PageConstants.getOrientationList());
		orientBox.setSelectedItem(PageConstants.LANDSCAPE);
		orientBox.setEnabled(selectedFormat.isDocumentFormat());

		previewPanel.add(previewComp, "grow, push");

		optionsPanel.add(format, "label, aligny 0");
		optionsPanel.add(formatBox, "aligny 0, growx, span, wrap");

		optionsPanel.add(paper, "label");
		optionsPanel.add(paperBox, "growx, span, wrap");

		optionsPanel.add(orient, "label");
		optionsPanel.add(orientBox, "growx, span, wrap");

		optionsPanel.add(matrix, "label, aligny 0");
		RegionType selectedRegion = addRegionRadioButtons(optionsPanel,
			selectedFormat);
		
		optionsPanel.add(aspect, "label, aligny 0");
		addAspectRadioButtons(optionsPanel,selectedRegion,selectedFormat);
		
		optionsPanel.add(selectionsBox,"span");

		contentPanel.add(previewPanel, "grow, w 500!, h 500!");
		contentPanel.add(optionsPanel, "aligny 0%, growx, push");
		
		this.exportBtn = GUIFactory.createBtn("Export");
		closeBtn.setText("Cancel");
		
		JPanel btnPanel = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);
		btnPanel.add(closeBtn, "tag cancel, pushx, al right");
		btnPanel.add(exportBtn, "al right");

		mainPanel.add(contentPanel, "push, grow, wrap");
		mainPanel.add(btnPanel, "bottom, pushx, growx, span");

		getContentPane().add(mainPanel);

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	/**
	 * Adds radio buttons to regionRadioBtns and to the supplied rangePanel and
	 * also disables invalid regions with a tooltip explaining why based on
	 * bigRegs and selectionsExist.  The default region is pre-selected.
	 * 
	 * @param rangePanel
	 * @return
	 */
	public RegionType addRegionRadioButtons(final JPanel optionPanel,
		final FormatType selectedFormat) {

		RegionType selectedRegion = null;
		RegionType defReg = (selectedFormat.isDocumentFormat() ?
			RegionType.getDefault(selectionsExist) :
			RegionType.getDefault(bigRegs,selectionsExist));
		
		// Switched to normal for loop to handle last button via index
		RegionType[] vals = RegionType.values(); 
		for (int i = 0; i < vals.length; i++) {
			RegionType reg = vals[i];
			JRadioButton option = new JRadioButton(reg.toString());
			
			if(!selectedFormat.isDocumentFormat() && bigRegs.contains(reg)) {
				option.setEnabled(false);
				option.setToolTipText("Too big for PNG/JPG/PPM export");
			}
			//Default region pre-selected
			if(reg == defReg && (selectedFormat.isDocumentFormat() ||
				!bigRegs.contains(reg))) {

				option.setSelected(true);
				selectedRegion = reg;
			}
			//If this is the selection region and it's valid
			if(reg == RegionType.SELECTION && (selectedFormat.isDocumentFormat() ||
				!bigRegs.contains(reg))) {

				option.setEnabled(selectionsExist);
				if(!selectionsExist) {
					option.setToolTipText("No data selected");
				}
			}
			regionRadioBtns.add(option);
			
			// Wrap after last button	
			if(i == vals.length - 1) {
				optionPanel.add(option, "wrap");
				
			} else {
				optionPanel.add(option);
			}
		}
		return(selectedRegion);
	}

	/**
	 * Adds radio buttons to aspectRadioBtns and to the supplied aspectPanel and
	 * also disables invalid aspects with a tooltip explaining why based on
	 * bigRegs.  The default aspect is pre-selected.
	 * 
	 * @param optionsPanel
	 * @param selectedRegion
	 */
	public void addAspectRadioButtons(final JPanel optionsPanel,
		final RegionType selectedRegion,final FormatType selectedFormat) {

		// Switched to normal for loop to handle last button via index
		AspectType[] vals = AspectType.values();
		for(int i = 0; i < vals.length; i++) {
			AspectType asp = vals[i];
			JRadioButton option = new JRadioButton(asp.toString());
			List<AspectType> tooBigs =
				eh.getOversizedAspects(selectedRegion);
			//Default region pre-selected
			AspectType defAsp = (selectedFormat.isDocumentFormat() ?
				AspectType.getDefault() : AspectType.getDefault(tooBigs));
			if(asp == defAsp) {
				option.setSelected(true);
			}
			if(!selectedFormat.isDocumentFormat() && tooBigs.contains(asp)) {
				option.setEnabled(false);
				option.setToolTipText("Too big for PNG/JPG/PPM export");
			}
			aspectRadioBtns.add(option);
			
			// Wrap after last button, also span because of long name	
			if(i == vals.length - 1) {
				optionsPanel.add(option, "span, wrap");
				
			} else {
				optionsPanel.add(option);
			}
		}
	}

	/**
	 * Add export action to export button. Determines what happens, when
	 * the export button is clicked.
	 * @param l The ActionListener
	 */
	public void addExportListener(final ActionListener l) {
		exportBtn.addActionListener(l);
	}

	/**
	 * Add format action to format dropdown. Determines what region radio
	 * buttons are valid when a format is selected.
	 * @param l The ActionListener
	 */
	public void addFormatListener(final ActionListener l) {
		formatBox.addActionListener(l);
	}

	/**
	 * Add region action to region radio buttons. Determines what aspect radio
	 * buttons are valid when a region is clicked.
	 * @param l The ActionListener
	 */
	public void addRegionListener(final ActionListener l) {
		Enumeration<AbstractButton> rab = regionRadioBtns.getElements();
		while(rab.hasMoreElements()) {
			AbstractButton btn = rab.nextElement();
			btn.addActionListener(l);
		}
	}
	
	/**
	 * An item listener that allows us to fire events upon selection changes
	 * in the components that it has been added to.
	 * @param l - The item listener.
	 */
	public void addItemStateListener(final ItemListener l) {
		
		formatBox.addItemListener(l);
		paperBox.addItemListener(l);
		orientBox.addItemListener(l);
		
		Enumeration<AbstractButton> rab = regionRadioBtns.getElements();
		while(rab.hasMoreElements()) {
			AbstractButton btn = rab.nextElement();
			btn.addItemListener(l);
		}
		
		Enumeration<AbstractButton> asp = aspectRadioBtns.getElements();
		while(asp.hasMoreElements()) {
			AbstractButton btn = asp.nextElement();
			btn.addItemListener(l);
		}
	}
	
	/**
	 * An item listener that allows us to fire events upon selection changes
	 * in the components that it has been added to.
	 * @param l - The item listener.
	 */
	public void addCheckBoxItemStateListener(final ItemListener l) {
		
		selectionsBox.addItemListener(l);
	}

	/**
	 * @author rleach
	 * @return the paperBox
	 */
	public JComboBox<PaperType> getPaperBox() {
		return(paperBox);
	}

	/**
	 * @author rleach
	 * @return the orientBox
	 */
	public JComboBox<String> getOrientBox() {
		return(orientBox);
	}

	/**
	 * Sets the trees and the matrix for the preview panel.
	 * @param rowTrees - The panel containing the row trees drawing.
	 * @param colTrees - The panel containing the column trees drawing.
	 * @param matrix - The panel containing the matrix drawing.
	 */
	public void setPreviewComponents(ExportPreviewTrees rowTrees, 
			ExportPreviewTrees colTrees, ExportPreviewMatrix matrix) {

		this.rowPrevTrees = rowTrees;
		this.colPrevTrees = colTrees;
		this.matrix = matrix;
	}
	
	/**
	 * Arranges the trees and the matrix on the preview panel, depending on
	 * which trees are active. Previously calculated component sizes are used
	 * here to dynamically create a layout.
	 **/
	public void arrangePreviewPanel() {
		
		if(previewComp == null || background == null) {
			LogBuffer.println("Cannot set preview for Export.");
			return;
		}

		background.removeAll();
		
		JPanel previews = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);
		
		/* Tree panels need to have the same size as the matrix */
		JPanel filler = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);
		if(rowPrevTrees != null && colPrevTrees != null) {
			previews.add(filler, "w " + rowPrevTrees.getShortSide() + "!, "
					+ "h " + colPrevTrees.getShortSide() + "!,"
							+ "gap " + gapsize + "! " + gapsize);
		}

		if(colPrevTrees != null) {
			colPrevTrees.setLongSide(matrix.getMatrixWidth());
			previews.add(colPrevTrees, "growx, pushx, "
					+ "h " + colPrevTrees.getShortSide() + "!,"
					+ " w " + colPrevTrees.getLongSide() + "!, "
					+ "gapy " + gapsize + "!, wrap");
		}

		if(rowPrevTrees != null) {
			rowPrevTrees.setLongSide(matrix.getMatrixHeight());
			previews.add(rowPrevTrees, "growy, aligny 0, pushy, "
					+ "h " + rowPrevTrees.getLongSide() + "!, "
					+ "w " + rowPrevTrees.getShortSide() + "!, "
					+ "gapx " + gapsize + "!");
		}

		
		previews.add(matrix, "h " + matrix.getMatrixHeight() + "!, w " 
				+ matrix.getMatrixWidth() + "!, aligny 0, push, grow");
		
		background.add(previews, "push, align center");
		previewComp.add(background, "w " + bgWidth + "!, h " 
				+ bgHeight + "!, push, align center");
		
		mainPanel.revalidate();
		mainPanel.repaint();
	}

	public List<RegionType> getBigRegs() {
		return bigRegs;
	}

	public void setBigRegs(List<RegionType> bigRegs) {
		this.bigRegs = bigRegs;
	}

	/**
	 * Updates the preview components (matrix and trees) according to the
	 * selected options in the dialog.
	 * @param exportOptions - The user selected export options which are used
	 * to adjust the preview components
	 * @param dataMatrixSize - The size of the dataMatrix (rows x columns)
	 */
	public void updatePreviewComponents(final ExportOptions exportOptions) {
		
		updateBackground(exportOptions);
		Dimension bgSize = new Dimension(bgWidth, bgHeight);
		updatePreviewMatrix(exportOptions, bgSize);
		
		arrangePreviewPanel();
		
		mainPanel.revalidate();
		mainPanel.repaint();
	}
	
	/**
	 * Updates the background of the preview components.There are document
	 * and image formats for export. The background can thus be paper like or
	 * transparent. It also adjusts its size and orientation.
	 * @param exportOptions - The user selected export options
	 */
	private void updateBackground(final ExportOptions exportOptions) {
		
		/* First, set the background color depending on document format */
		final boolean isPaper = exportOptions.getFormatType().isDocumentFormat();
		
		if(rowPrevTrees != null) {
			rowPrevTrees.setPaperBackground(isPaper);
		}
		
		if(colPrevTrees != null) {
			colPrevTrees.setPaperBackground(isPaper);
		}
		
		if(isPaper) {
			// set paper background
			background.setBackground(Color.WHITE);
			background.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			
			// default is portrait orientation
			Dimension bgSize = 
					PaperType.getDimFromLongSide(exportOptions.getPaperType(), 
					                             PAPER_LONGSIDELEN);
			
			// adjust background panel to paper orientation and size
			String orientation = exportOptions.getOrientation();
			if((PageConstants.LANDSCAPE).equalsIgnoreCase(orientation)) {
				bgWidth = (int)bgSize.getHeight();
				bgHeight = (int)bgSize.getWidth();
				
			} else {
				bgWidth = (int)bgSize.getWidth();
				bgHeight = (int)bgSize.getHeight();
			}
			
		} else {
			// set normal / transparent background
			bgWidth = PAPER_LONGSIDELEN;
			bgHeight = PAPER_LONGSIDELEN;
			
			background.setBackground(mainPanel.getBackground());
			background.setBorder(null);
		}
	}
	
	/**
	 * Updates the size of the preview matrix based on available trees and 
	 * the size of the background panel. The goal is to maximize the matrix 
	 * on the page while respecting user selected export options. 
	 * @param exportOptions - The user selected options
	 * @param bgSize - The dimension of the background panel
	 * @param imvSize - Size of the InteractiveMatrixView panel for adjusting
	 * to 'as seen on screen' 
	 * @param dataMatrixSize - The dimension of the data matrix 
	 * (1 element = 1px)
	 */
	private void updatePreviewMatrix(final ExportOptions exportOptions, 
			final Dimension bgSize) {
		
		/* (!)
		 * Using int casts throughout this method instead of Math methods,
		 * because plain truncating is fine. 
		 */
		int availBgWidth = (int) bgSize.getWidth();
		int availBgHeight = (int) bgSize.getHeight();
		
		// Shrink available background area to realize an arbitrary 10px margin
		int margin = 20;
		availBgWidth -= margin;
		availBgHeight -= margin;
		
		/* issue #6/ #6.1 implementation */
		eh.setCalculatedDimensions(exportOptions.getRegionType(), 
				exportOptions.getAspectType());
		
		/* Dimensions of full exported image before scaling */
		int exportWidth = eh.getXDim(exportOptions.getRegionType());
		int exportHeight = eh.getYDim(exportOptions.getRegionType());

		/* 
		 * Define scales to fit ExportHandler-calculated sizes of preview 
		 * components to the available background area.
		 */
		double scaleWidth = ((double) availBgWidth) / exportWidth;
		double scaleHeight = 1;
		
		/* Apply width scale */
		int previewImgWidth = (int)(exportWidth * scaleWidth);
		int previewImgHeight = (int)(exportHeight * scaleWidth);
		
		/* If height is still too great, rescale */
		if(availBgHeight < previewImgHeight) {
			scaleHeight = ((double)availBgHeight) / previewImgHeight;
		}
		
		/* Apply height scale */
		previewImgWidth *= scaleHeight;
		previewImgHeight *= scaleHeight;
		
		/* Get scaled sizes for trees and gaps */
		int treeSize = (int)(scaleWidth * scaleHeight * eh.getTreesHeight());
		this.gapsize = (int)(scaleWidth * scaleHeight * eh.getTreeMatrixGapSize());
		
		// adjust maximum matrix side length for tree thickness and gaps
		if(rowPrevTrees != null) {
			rowPrevTrees.setShortSide(treeSize);
			previewImgWidth -= treeSize;
			previewImgWidth -= gapsize;
		}
		
		if(colPrevTrees != null) {
			colPrevTrees.setShortSide(treeSize);
			previewImgHeight -= treeSize;
			previewImgHeight -= gapsize;
		}
		
		matrix.setMatrixWidth(previewImgWidth);
		matrix.setMatrixHeight(previewImgHeight);
	}
	
	/**
	 * Updates the availability and the selection of the region radio buttons
	 * based on the selected file format, whether a selection exists, and on
	 * whether the 1:1 size of the region is exportable (in an image format).
	 * Calls updateAspectRadioBtns.
	 * @param isDocFormat
	 */
	public void updateRegionRadioBtns(final boolean isDocFormat) {

		Enumeration<AbstractButton> rBtns = regionRadioBtns.getElements();
		boolean changeSelected = false;
		RegionType selectedRegion = null;

		//Check if region radio buttons need to be disabled/enabled based on
		//selected region
		while(rBtns.hasMoreElements()) {
			AbstractButton option = rBtns.nextElement();
			final boolean isEnabled = isDocFormat ||
					!bigRegs.contains(RegionType.getRegion(option.getText()));
			if(option.isSelected()) {
				selectedRegion = RegionType.getRegion(option.getText());
			}
			if(RegionType.getRegion(option.getText()) != RegionType.SELECTION ||
				selectionsExist) {

				option.setEnabled(isEnabled);
				if(isEnabled) {
					option.setToolTipText(null);
				} else {
					option.setToolTipText("Too big for PNG/JPG/PPM export");
					if(option.isSelected()) {
						option.setSelected(false);
						selectedRegion = null;
						changeSelected = true;
					}
				}
			} else {
				option.setEnabled(false);
				option.setToolTipText("No selection has been made");
			}
		}

		//If the selected option was disabled, select a new default
		if(changeSelected) {
			rBtns = regionRadioBtns.getElements();

			RegionType defReg;
			if(isDocFormat) {
				defReg = RegionType.getDefault();
			} else {
				defReg = RegionType.getDefault(bigRegs,selectionsExist);
			}

			if(defReg != null) {
				while(rBtns.hasMoreElements()) {
					AbstractButton option = rBtns.nextElement();
					if(RegionType.getRegion(option.getText()) == defReg) {
						selectedRegion = RegionType.getRegion(option.getText());
						option.setSelected(true);
					}
				}
			}
		}

		//The aspect radio buttons should be updated based on the selected
		//region
		updateAspectRadioBtns(isDocFormat, selectedRegion);
	}

	/**
	 * Updates the availability and the selection of the aspect radio buttons
	 * based on the selected file format, and on whether the size of the
	 * selected region is exportable (in an image format).
	 * @param isDocFormat
	 * @param selectedRegion
	 */
	public void updateAspectRadioBtns(final boolean isDocFormat,
		final RegionType selectedRegion) {

		Enumeration<AbstractButton> aBtns = aspectRadioBtns.getElements();
		boolean changeSelected = false;
		List<AspectType> bigAsps = new ArrayList<AspectType>();

		//Check if aspect radio buttons need to be disabled/enabled based on
		//selected region
		while(aBtns.hasMoreElements()) {
			AbstractButton option = aBtns.nextElement();
			AspectType asp = AspectType.getAspect(option.getText());
			eh.setTileAspectRatio(asp);
			eh.setCalculatedDimensions(selectedRegion);
			final boolean tooBig = eh.isOversized(selectedRegion);
			if(tooBig) {
				bigAsps.add(asp);
			}
			final boolean enabled = isDocFormat || !tooBig;
			if(!enabled && option.isSelected()) {
				option.setSelected(false);
				changeSelected = true;
			}
			option.setEnabled(enabled);
			if(enabled) {
				option.setToolTipText(null);
			} else {
				option.setToolTipText("Too big for PNG/JPG/PPM export");
			}
		}

		//If the selected option was disabled, select a new default
		if(changeSelected) {
			AspectType defAsp;
			if(isDocFormat) {
				defAsp = AspectType.getDefault();
			} else {
				defAsp = AspectType.getDefault(bigAsps);
			}

			if(defAsp != null) {
				aBtns = aspectRadioBtns.getElements();
				while(aBtns.hasMoreElements()) {
					AbstractButton option = aBtns.nextElement();
					AspectType asp = AspectType.getAspect(option.getText());
					if(asp == defAsp) {
						option.setSelected(true);
					}
				}
			}
		}
	}
	
	/* A Dialog box with progress bar used to show the export progress
	 * 
	 */
	public static class ExportBarDialog extends CustomDialog{
		private static final long serialVersionUID = 1L;
		/*
		 *  Swing worker which is handling the progress of the progress bar jpb
		 */
		private ExportWorker worker;
		private JProgressBar jpb = null;
		private JLabel label = null;
		private JButton cancelButton = null;
		private JTextArea exportFileName = null;
		
		public ExportBarDialog(String title, ExportWorker worker) {
			super(title);
			this.worker = worker;
		}
		
		public void setupDialogComponents(){
			setupLayout();
		}

		@Override
		protected void setupLayout() {
			this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			mainPanel = GUIFactory.createJPanel(false, GUIFactory.FILL);
			label = GUIFactory.createLabel("Loading...");
			exportFileName = GUIFactory.createWrappableTextArea();
			exportFileName.setFont(GUIFactory.FONTXS);
			exportFileName.setFocusable(true);
			cancelButton = GUIFactory.createSquareBtn("cancel", 15);
			jpb = new JProgressBar();
	        mainPanel.add(label,"push, grow, w 375, wrap");
	        mainPanel.add(jpb, "push, grow, split 2, gapright 15,  w 300!,  h 25!");
	        mainPanel.add(cancelButton, "push, grow, w 75!, h 25!, wrap");
			mainPanel.add(exportFileName,"push, grow, w 390!, wrap");
			getContentPane().add(mainPanel);
			this.setResizable(false);
			mainPanel.revalidate();
			mainPanel.repaint();
			addWindowListener(new ExportBarWindowListener(worker));
			cancelButton.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e) {
					onPressCancelButton(worker,cancelButton.getParent());
				}
			});
		}

		/**
		 * @return the jpb
		 */
		public JProgressBar getProgressBar() {
			return jpb;
		}

		/**
		 * @param jpb the jpb to set
		 */
		public void setProgressBar(JProgressBar jpb) {
			this.jpb = jpb;
		}
		
		/**
		 * @param jpb the jpb value to set
		 */
		public void setProgressBarValue(int value) {
			this.jpb.setValue(value);
		}
		
		/**
		 * @param text the status to set
		 */
		public void setText(String text) {
			this.label.setText(text);
		}

		public void setExportFileName(String exportFileName) {
			this.exportFileName.setText(exportFileName);
		}

	}
	
	private static class ExportBarWindowListener extends WindowAdapter{
		ExportWorker worker;
		public ExportBarWindowListener(ExportWorker worker) {
			this.worker = worker;
		}
		
		// This event gets fired when the user closes the Export bar
		public void windowClosing(WindowEvent e) {
			onPressCancelButton(worker,e.getComponent());
		}
	}
	
	private static void onPressCancelButton(ExportWorker worker, Component c){
		Object[] options = {"Yes", "No"};
		/* returns -1 if window is closed,
         * change the window to DO_NOTHING_ON_CLOSE
         */
        int answer = JOptionPane.showOptionDialog(c, "Are your sure you want to cancel the export?",
                                                  "Cancelling Export!",
                                                  JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                                                  null, options, options[0]);
        
        if(answer != 0)
        	return;
  
        worker.ebd.dispose();
        LogBuffer.println("User closed the Export Progress bar");
		//set the boolean
		worker.setExportSuccessful(false);
		/* Now gracefully try to end the SwingWorker. The call worker.cancel(true) will
		 * always throws java.util.concurrent.CancellationException when swingworker is
		 * running. we need to handle it and use isCancelled method while executing 
		 * doInBackground() to gracefully end the Export.
		 */
		try {
			worker.cancel(true);
		}
		catch(java.util.concurrent.CancellationException ex){
			LogBuffer.println("Cancelled the Export process");
		}
	}
	
	/**
	 * Sets the members of the passed ExportOption reference according to the
	 * current state of the GUI.
	 * @param exportOptions - The ExportOptions object
	 */
	public void retrieveOptions(final ExportOptions exportOptions) {
		
		exportOptions.setFormatType((FormatType) formatBox.getSelectedItem());
		exportOptions.setPaperType((PaperType) paperBox.getSelectedItem());
		exportOptions.setOrientation((String)orientBox.getSelectedItem());
		
		/* Aspect ratio */
		AspectType aspectType = AspectType.getDefault();
		String buttonText = "default";
		for(Enumeration<AbstractButton> buttons = aspectRadioBtns.getElements();
				buttons.hasMoreElements();) {
			AbstractButton button = buttons.nextElement();
			if(button.isSelected()) {
				buttonText = button.getText();
				break;
			}
		}
		
		for(AspectType eA : AspectType.values()) {
			if(eA.toString().equalsIgnoreCase(buttonText)) {
				aspectType = eA;
				break;
			}
		}
		exportOptions.setAspectType(aspectType);
		
		/* Region to be exported */
		RegionType regionType = RegionType.getDefault();
		buttonText = "default";
		for(Enumeration<AbstractButton> buttons = regionRadioBtns.getElements();
				buttons.hasMoreElements();) {
			AbstractButton button = buttons.nextElement();
			if(button.isSelected()) {
				buttonText = button.getText();
				break;
			}
		}
		
		for(RegionType rT : RegionType.values()) {
			if(rT.toString().equalsIgnoreCase(buttonText)) {
				regionType = rT;
				break;
			}
		}
		exportOptions.setRegionType(regionType);
		
		/* Show selections */
		exportOptions.setShowSelections(selectionsBox.isSelected());
	}
}
