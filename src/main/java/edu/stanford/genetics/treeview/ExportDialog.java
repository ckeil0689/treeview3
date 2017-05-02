package edu.stanford.genetics.treeview;

import java.awt.Component;
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

import net.miginfocom.swing.MigLayout;

import org.freehep.graphicsio.PageConstants;

import Controllers.ExportHandler;
import Controllers.FormatType;
import Controllers.RegionType;
import Controllers.TreeExportOption;
import Utilities.CustomDialog;
import Utilities.GUIFactory;
import Controllers.ExportHandler.ExportWorker;
import Controllers.LabelExportOption;

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
	private ExportPreviewLabels rowPrevLabels;
	private ExportPreviewLabels colPrevLabels;
	private ExportPreviewMatrix matrix;

	private JComboBox<FormatType> formatBox;
	private JComboBox<PaperType> paperBox;
	private JComboBox<String> orientBox;
	private ButtonGroup regionRadioBtns;
	private ButtonGroup rowLabelBtns;
	private ButtonGroup colLabelBtns;
	private ButtonGroup aspectRadioBtns;
	private JCheckBox selectionsBox;
	private JButton exportBtn;
	private List<RegionType> bigRegs; //List of regions that are too big for image export (doc export is OK)
	private boolean rowLabelsTooBig; //True if including labels makes the image too big (doc export is OK)
	private boolean colLabelsTooBig; //True if including labels makes the image too big (doc export is OK)
	private boolean selectionsExist;
	private ExportHandler eh;

	public ExportDialog(final boolean selectionsExist, final ExportHandler eh)
		throws ExportException {

		super("Export");
		this.eh = eh;
		this.eh.getSetBestOptions();
		final boolean useMinimums = true;
		FormatType ft = getDefaultFormatType();
		this.selectionsExist = selectionsExist;
		this.bigRegs = eh.getOversizedRegions(useMinimums,
			ft.isDocumentFormat());

		//This interface interactively sets its own defaults based on user
		//selections and what turns out to be too big, so let's set a minimum
		//calculated size to start based on the defaults selected in this class
		RegionType rt = getDefaultRegion();
		if(rt == null) {
			throw new ExportException(this.eh,rt);
		}
		LogBuffer.println("Default region: " + rt);
		eh.setCalculatedDimensions(rt, getDefaultAspectType(ft,
			eh.getOversizedAspects(rt,ft.isDocumentFormat())));

		this.rowLabelsTooBig = eh.areRowLabelsTooBig(rt,ft.isDocumentFormat());
		this.colLabelsTooBig = eh.areColLabelsTooBig(rt,ft.isDocumentFormat());

		this.gapsize = eh.getGapSize(); //Minimum gap size

		this.bgWidth = PAPER_LONGSIDELEN;
		this.bgHeight = PAPER_LONGSIDELEN;

		setupLayout();

		//This will cause a cascade of updates
		updateFormatSelectList();
		//updateRegionRadioBtns(ft.isDocumentFormat());

		LogBuffer.println("ExportDialog ready.");
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
		JLabel orient = GUIFactory.createLabel("Orientation:",GUIFactory.FONTS);

		JLabel matrix = GUIFactory.createLabel("<HTML><U>Matrix</U></HTML>",GUIFactory.FONTS);
		JLabel region = GUIFactory.createLabel("Region:",GUIFactory.FONTS);
		JLabel aspect = GUIFactory.createLabel("Tile Aspect:",GUIFactory.FONTS);

		JLabel labels = GUIFactory.createLabel("<HTML><U>Labels</U></HTML>",GUIFactory.FONTS);
		JLabel rlabel = GUIFactory.createLabel("Row:",GUIFactory.FONTS);
		JLabel clabel = GUIFactory.createLabel("Column:",GUIFactory.FONTS);

		JLabel selecs = GUIFactory.createLabel("<HTML><U>Selections</U></HTML>",GUIFactory.FONTS);
		JLabel showsl = GUIFactory.createLabel("Show:",GUIFactory.FONTS);

		JLabel spacer1 = GUIFactory.createLabel(" ",GUIFactory.FONTS);
		JLabel spacer2 = GUIFactory.createLabel(" ",GUIFactory.FONTS);
		JLabel spacer3 = GUIFactory.createLabel(" ",GUIFactory.FONTS);

		FormatType selectedFormat = FormatType.getDefault();
		if(eh.isImageExportPossible()) {
			this.formatBox = new JComboBox<FormatType>(
				FormatType.getHiResFormats());

			formatBox.setSelectedItem(FormatType.getDefault());
		} else {
			selectedFormat = FormatType.getDefaultDocumentFormat();
			formatBox = new JComboBox<FormatType>(FormatType.getDocumentFormats());
			formatBox.setSelectedItem(FormatType.getDefaultDocumentFormat());
			formatBox.setToolTipText("All regions too big for " +
				(selectedFormat.isDocumentFormat() ? "document" : "image") +
				" export");
		}

		this.paperBox = new JComboBox<PaperType>(PaperType.values());
		paperBox.setSelectedItem(PaperType.getDefault());
		paperBox.setEnabled(selectedFormat.isDocumentFormat());
		
		this.regionRadioBtns = new ButtonGroup();
		this.aspectRadioBtns = new ButtonGroup();

		this.rowLabelBtns = new ButtonGroup();
		this.colLabelBtns = new ButtonGroup();

		this.selectionsBox = new JCheckBox("");
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

		optionsPanel.add(spacer1,"wrap");

		optionsPanel.add(matrix,"align right, wrap");
		optionsPanel.add(region, "label, aligny 0");
		RegionType selectedRegion = addRegionRadioButtons(optionsPanel,
			selectedFormat);

		optionsPanel.add(aspect, "label, aligny 0");
		addAspectRadioButtons(optionsPanel,selectedRegion,selectedFormat);

		optionsPanel.add(spacer2,"wrap");

		optionsPanel.add(labels,"align right, wrap");
		optionsPanel.add(rlabel, "label, aligny 0");
		addRowLabelButtons(optionsPanel,selectedFormat);
		optionsPanel.add(clabel, "label, aligny 0");
		addColLabelButtons(optionsPanel,selectedFormat);

		optionsPanel.add(spacer3,"wrap");

		optionsPanel.add(selecs,"align right, wrap");
		optionsPanel.add(showsl, "label, aligny 0");
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

	public FormatType getDefaultFormatType() {
		FormatType defaultFormat = FormatType.getDefault();
		if(!eh.isImageExportPossible()) {
			defaultFormat = FormatType.getDefaultDocumentFormat();
		}
		return(defaultFormat);
	}

	/**
	 * Adds radio buttons to regionRadioBtns and to the supplied rangePanel and
	 * also disables invalid regions with a tooltip explaining why based on
	 * bigRegs and selectionsExist.  The default region is pre-selected.
	 * 
	 * @param rangePanel
	 * @param selectedFormat
	 * @return
	 */
	public RegionType addRegionRadioButtons(final JPanel optionPanel,
		final FormatType selectedFormat) {

		RegionType selectedRegion = null;
		RegionType defReg = RegionType.getMinDefault(selectionsExist);
		
		// Switched to normal for loop to handle last button via index
		RegionType[] vals = RegionType.values(); 
		for (int i = 0; i < vals.length; i++) {
			RegionType reg = vals[i];
			JRadioButton option = new JRadioButton(reg.toString());
			
			if(bigRegs.contains(reg)) {
				option.setEnabled(false);
				option.setToolTipText("Too big for " +
					(selectedFormat.isDocumentFormat() ? "document" : "image") +
					" export");
			}
			//Default region pre-selected
			if(reg == defReg && !bigRegs.contains(reg)) {

				option.setSelected(true);
				selectedRegion = reg;
			}
			//If this is the selection region and it's valid
			if(reg == RegionType.SELECTION && !bigRegs.contains(reg)) {

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
	 * Returns a valid (likely minimum) default region
	 * 
	 * @return
	 */
	public RegionType getDefaultRegion() {

		RegionType selectedRegion = RegionType.getMinDefault(selectionsExist);

		if(!bigRegs.contains(selectedRegion)) {
			return(selectedRegion);
		}

		RegionType[] vals = RegionType.values(); 
		for (int i = 0; i < vals.length; i++) {

			selectedRegion = vals[i];
			if((selectionsExist || selectedRegion != RegionType.SELECTION) &&
				!bigRegs.contains(selectedRegion)) {

				return(selectedRegion);
			}
		}

		return(null);
	}

	/**
	 * Adds row label radio buttons to rowLabelBtns and to the supplied
	 * labelPanel and also disables invalid label inclusions with a tooltip
	 * explaining why based on rowLabelsTooBig.  The default label
	 * option is pre-selected.
	 * 
	 * @param optionPanel
	 * @param selectedFormat
	 * @return
	 */
	public LabelExportOption addRowLabelButtons(final JPanel optionPanel,
		final FormatType selectedFormat) {

		//This is a "safe" default which cannot result in an image that is "too
		//big"
		LabelExportOption selectedLabelOption = LabelExportOption.NO;
		//And this is an assumed valid default set by the LabelExportOption
		//class
		LabelExportOption defLO = LabelExportOption.getDefault();

		// Switched to normal for loop to handle last button via index
		LabelExportOption[] vals = LabelExportOption.values(); 
		for (int i = 0; i < vals.length; i++) {
			LabelExportOption lo = vals[i];
			JRadioButton option = new JRadioButton(lo.toString());

			if(lo != LabelExportOption.NO && rowLabelsTooBig) {
				option.setEnabled(false);
				option.setToolTipText("This will make the image too big for " +
					"export");
			}
			//Default label export option pre-selected
			if(lo == defLO && (selectedFormat.isDocumentFormat() ||
				!rowLabelsTooBig)) {

				option.setSelected(true);
				selectedLabelOption = lo;
			}
			//If this is the selection region and it's valid
			if(lo == LabelExportOption.SELECTION &&
				(selectedFormat.isDocumentFormat() || !rowLabelsTooBig)) {

				option.setEnabled(selectionsExist);
				if(!selectionsExist) {
					option.setToolTipText("No data selected");
				}
			}
			rowLabelBtns.add(option);
			
			// Wrap after last button	
			if(i == vals.length - 1) {
				optionPanel.add(option, "wrap");
			} else {
				optionPanel.add(option);
			}
		}
		return(selectedLabelOption);
	}

	/**
	 * Adds column label radio buttons to colLabelBtns and to the supplied
	 * labelPanel and also disables invalid label inclusions with a tooltip
	 * explaining why based on colLabelsTooBig.  The default label
	 * option is pre-selected.
	 * 
	 * @param optionPanel
	 * @param selectedFormat
	 * @return
	 */
	public LabelExportOption addColLabelButtons(final JPanel optionPanel,
		final FormatType selectedFormat) {

		//This is a "safe" default which cannot result in an image that is "too
		//big"
		LabelExportOption selectedLabelOption = LabelExportOption.NO;
		//And this is an assumed valid default set by the LabelExportOption
		//class
		LabelExportOption defLO = LabelExportOption.getDefault();

		// Switched to normal for loop to handle last button via index
		LabelExportOption[] vals = LabelExportOption.values(); 
		for (int i = 0; i < vals.length; i++) {
			LabelExportOption lo = vals[i];
			JRadioButton option = new JRadioButton(lo.toString());

			if(lo != LabelExportOption.NO && colLabelsTooBig) {
				option.setEnabled(false);
				option.setToolTipText("This will make the image too big for " +
					"export");
			}
			//Default label export option pre-selected
			if(lo == defLO && (selectedFormat.isDocumentFormat() ||
				!colLabelsTooBig)) {

				option.setSelected(true);
				selectedLabelOption = lo;
			}
			//If this is the selection region and it's valid
			if(lo == LabelExportOption.SELECTION &&
				(selectedFormat.isDocumentFormat() || !colLabelsTooBig)) {

				option.setEnabled(selectionsExist);
				if(!selectionsExist) {
					option.setToolTipText("No data selected");
				}
			}
			colLabelBtns.add(option);
			
			// Wrap after last button	
			if(i == vals.length - 1) {
				optionPanel.add(option, "wrap");
			} else {
				optionPanel.add(option);
			}
		}
		return(selectedLabelOption);
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

		//Default region pre-selected
		List<AspectType> tooBigs = eh.getOversizedAspects(selectedRegion,
			selectedFormat.isDocumentFormat());
		AspectType defAsp = getDefaultAspectType(selectedFormat,tooBigs);

		// Switched to normal for loop to handle last button via index
		AspectType[] vals = AspectType.values();
		for(int i = 0; i < vals.length; i++) {
			AspectType asp = vals[i];
			JRadioButton option = new JRadioButton(asp.toString());
			if(asp == defAsp) {
				option.setSelected(true);
			}
			if(tooBigs.contains(asp)) {
				option.setEnabled(false);
				option.setToolTipText("Too big for " +
					(selectedFormat.isDocumentFormat() ? "document" : "image") +
					" export");
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

	public AspectType getDefaultAspectType(FormatType ft,
		List<AspectType> tooBigs) {

		AspectType defAsp = (ft.isDocumentFormat() ?
			AspectType.getDefault() : AspectType.getDefault(tooBigs));
		return(defAsp);
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

	public void addAspectListener(final ActionListener l) {
		Enumeration<AbstractButton> rab = aspectRadioBtns.getElements();
		while(rab.hasMoreElements()) {
			AbstractButton btn = rab.nextElement();
			btn.addActionListener(l);
		}
	}

	public void addRowLabelListener(final ActionListener l) {
		Enumeration<AbstractButton> rlb = rowLabelBtns.getElements();
		while(rlb.hasMoreElements()) {
			AbstractButton btn = rlb.nextElement();
			btn.addActionListener(l);
		}
	}
	
	public void addColLabelListener(final ActionListener l) {
		Enumeration<AbstractButton> clb = colLabelBtns.getElements();
		while(clb.hasMoreElements()) {
			AbstractButton btn = clb.nextElement();
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
		
		Enumeration<AbstractButton> rlb = rowLabelBtns.getElements();
		while(rlb.hasMoreElements()) {
			AbstractButton btn = rlb.nextElement();
			btn.addItemListener(l);
		}
		
		Enumeration<AbstractButton> clb = colLabelBtns.getElements();
		while(clb.hasMoreElements()) {
			AbstractButton btn = clb.nextElement();
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
	 * @return the paperBox
	 */
	public JComboBox<PaperType> getPaperBox() {
		return(paperBox);
	}

	/**
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
		ExportPreviewTrees colTrees,ExportPreviewLabels rowLabels,
		ExportPreviewLabels colLabels,ExportPreviewMatrix matrix) {

		this.rowPrevTrees = rowTrees;
		this.colPrevTrees = colTrees;
		this.rowPrevLabels = rowLabels;
		this.colPrevLabels = colLabels;
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

		//Cannot use the factory here because the gap layout settings explicitly
		//set below aren't respected when the factory is used
		JPanel previews = new JPanel();
		previews.setOpaque(false);
		previews.setLayout(new MigLayout("ins 0, gap " + gapsize + "!"));

		/* Tree panels need to have the same size as the matrix */
		JPanel filler1 = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);
		if(rowPrevTrees != null && colPrevTrees != null) {
			previews.add(filler1, "w " + rowPrevTrees.getShortSide() + "!, "
				+ "h " + colPrevTrees.getShortSide() + "!");
		}
		JPanel filler2 = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);
		if(rowPrevLabels != null && colPrevTrees != null) {
			previews.add(filler2, "w " + rowPrevLabels.getShortSide() + "!, "
				+ "h " + colPrevTrees.getShortSide() + "!");
		}

		if(colPrevTrees != null) {
			colPrevTrees.setLongSide(matrix.getMatrixWidth());
			previews.add(colPrevTrees, "growx, pushx, "
				+ "h " + colPrevTrees.getShortSide() + "!,"
				+ " w " + colPrevTrees.getLongSide() + "!, wrap");
		}

		JPanel filler3 = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);
		if(rowPrevTrees != null && colPrevLabels != null) {
			previews.add(filler3, "w " + rowPrevTrees.getShortSide() + "!, "
				+ "h " + colPrevLabels.getShortSide() + "!");
		}
		JPanel filler4 = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);
		if(rowPrevLabels != null && colPrevLabels != null) {
			previews.add(filler4, "w " + rowPrevLabels.getShortSide() + "!, "
				+ "h " + colPrevLabels.getShortSide() + "!");
		}

		if(colPrevLabels != null) {
			colPrevLabels.setLongSide(matrix.getMatrixWidth());
			previews.add(colPrevLabels, "growx, pushx, "
				+ "h " + colPrevLabels.getShortSide() + "!,"
				+ " w " + colPrevLabels.getLongSide() + "!, wrap");
		}

		if(rowPrevTrees != null) {
			rowPrevTrees.setLongSide(matrix.getMatrixHeight());
			previews.add(rowPrevTrees, "growy, aligny 0, pushy, "
				+ "h " + rowPrevTrees.getLongSide() + "!, "
				+ "w " + rowPrevTrees.getShortSide() + "!");
		}

		if(rowPrevLabels != null) {
			rowPrevLabels.setLongSide(matrix.getMatrixHeight());
			previews.add(rowPrevLabels, "growx, pushx, "
				+ "h " + rowPrevLabels.getLongSide() + "!,"
				+ " w " + rowPrevLabels.getShortSide() + "!");
		}

		previews.add(matrix, "h " + matrix.getMatrixHeight() + "!, w " 
			+ matrix.getMatrixWidth() + "!, aligny 0, push, grow, gap 0!");

		background.add(previews, "push, align center, gap 0!");
		previewComp.add(background, "w " + bgWidth + "!, h " 
			+ bgHeight + "!, push, align center, gap 0!");
		
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
		final boolean isPaper =
			exportOptions.getFormatType().isDocumentFormat();

		if(rowPrevTrees != null) {
			rowPrevTrees.setPaperBackground(isPaper);
		}

		if(colPrevTrees != null) {
			colPrevTrees.setPaperBackground(isPaper);
		}

		if(rowPrevLabels != null) {
			rowPrevLabels.setPaperBackground(isPaper);
		}

		if(colPrevLabels != null) {
			colPrevLabels.setPaperBackground(isPaper);
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
		eh.setRowLabelsIncluded(exportOptions.getRowLabelOption());
		eh.setColLabelsIncluded(exportOptions.getColLabelOption());
		eh.setTileAspectRatio(exportOptions.getAspectType());
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
		this.gapsize = (int)(scaleWidth * scaleHeight * eh.getGapSize());
		int rowLabelSize = (int)(eh.getRowLabelPanelWidth(
			exportOptions.getRegionType(),
			exportOptions.getRowLabelOption()) * scaleWidth * scaleHeight);
		int colLabelSize = (int)(eh.getColLabelPanelHeight(
			exportOptions.getRegionType(),
			exportOptions.getColLabelOption()) * scaleWidth * scaleHeight);

		// adjust maximum matrix side length for tree thickness and gaps
		if(rowPrevTrees != null) {
			rowPrevTrees.setShortSide(treeSize);
			previewImgWidth -= treeSize;
			previewImgWidth -= gapsize;
		}

		if(rowPrevLabels != null) {
			rowPrevLabels.setShortSide(rowLabelSize);
		}

		if(exportOptions.getRowLabelOption() != LabelExportOption.NO) {
			previewImgWidth -= rowLabelSize;
			previewImgWidth -= gapsize;
		}

		if(colPrevTrees != null) {
			colPrevTrees.setShortSide(treeSize);
			previewImgHeight -= treeSize;
			previewImgHeight -= gapsize;
		}

		if(colPrevLabels != null) {
			colPrevLabels.setShortSide(colLabelSize);
		}

		if(exportOptions.getColLabelOption() != LabelExportOption.NO) {
			previewImgHeight -= colLabelSize;
			previewImgHeight -= gapsize;
		}

		matrix.setMatrixWidth(previewImgWidth);
		matrix.setMatrixHeight(previewImgHeight);

		//Now that we know the dimensions of the matrix, set the long side of
		//the trees and labels
		if(rowPrevTrees != null) {
			rowPrevTrees.setLongSide(previewImgHeight);
		}
		if(rowPrevLabels != null) {
			rowPrevLabels.setLongSide(previewImgHeight);
		}
		if(colPrevTrees != null) {
			colPrevTrees.setLongSide(previewImgWidth);
		}
		if(colPrevLabels != null) {
			colPrevLabels.setLongSide(previewImgWidth);
		}
	}

	/**
	 * Updates the availability and the selection of the format select list
	 * options based on the selected file format, whether a selection exists,
	 * and on whether the 1:1 size of the region is exportable (in an image
	 * format).  Calls updateRegionRadioBtns.
	 * @param isDocFormat
	 */
	public void updateFormatSelectList() throws ExportException {

		//Set minimum options in order to determine whether format options
		//should be disabled
		eh.setRowLabelsIncluded(LabelExportOption.NO);
		eh.setColLabelsIncluded(LabelExportOption.NO);
		eh.setRowTreeIncluded(TreeExportOption.AUTO);
		eh.setColTreeIncluded(TreeExportOption.AUTO);
		eh.setTileAspectRatio(AspectType.ONETOONE);

		//Determine whether image and document formats are too big
		RegionType minReg = RegionType.getMinDefault(selectionsExist);

		boolean docTooBig = false;
		eh.setFormat(FormatType.getDefaultDocumentFormat());
		eh.setCalculatedDimensions(minReg);
		docTooBig = eh.isOversized(minReg);
		boolean imageTooBig = false;
		eh.setFormat(FormatType.getDefaultImageFormat());
		eh.setCalculatedDimensions(minReg);
		imageTooBig = eh.isOversized(minReg);

		if(docTooBig && imageTooBig) {
			throw new ExportException(eh,minReg);
		}

		FormatType selectedFormat = (FormatType) formatBox.getSelectedItem();
		if(selectedFormat == null) {
			selectedFormat = FormatType.getDefault();
		}
		if(selectedFormat.isDocumentFormat() && docTooBig) {
			//This is technically impossible because document format is smaller
			//than image format, but I'm putting it here in case that changes in
			//the future.  Besides, ExportDialog shouldn't know this.
			selectedFormat = FormatType.getDefaultImageFormat();
		} else if(!selectedFormat.isDocumentFormat() && imageTooBig) {
			selectedFormat = FormatType.getDefaultDocumentFormat();
		}

		//If both image and document format types are not oversized
		if(!docTooBig && !imageTooBig) {
			if(formatBox.getItemCount() != FormatType.getHiResFormats().length) {
				formatBox.removeAllItems();
				FormatType[] fts = FormatType.getHiResFormats();
				for(int i = 0;i < fts.length;i++) {
					FormatType ft = fts[i];
					formatBox.addItem(ft);
				}
				formatBox.setToolTipText(null);
			}
		}
		//Else if image format is not oversized
		else if(!imageTooBig) {
			formatBox.removeAllItems();
			FormatType[] fts = FormatType.getImageFormats();
			for(int i = 0;i < fts.length;i++) {
				FormatType ft = fts[i];
				formatBox.addItem(ft);
			}
			formatBox.setToolTipText("Too big for document format export");
		}
		//Else if document format is not oversized
		else if(!docTooBig) {
			formatBox.removeAllItems();
			FormatType[] fts = FormatType.getDocumentFormats();
			for(int i = 0;i < fts.length;i++) {
				formatBox.addItem(fts[i]);
			}
			formatBox.setToolTipText("Too big for image format export");
		}
		formatBox.setSelectedItem(selectedFormat);

		//The aspect radio buttons should be updated based on the selected
		//region
		updateRegionRadioBtns(selectedFormat.isDocumentFormat());
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

		//Set minimum options in order to determine whether region options
		//should be disabled
		eh.setRowLabelsIncluded(LabelExportOption.NO);
		eh.setColLabelsIncluded(LabelExportOption.NO);
		eh.setRowTreeIncluded(TreeExportOption.AUTO);
		eh.setColTreeIncluded(TreeExportOption.AUTO);
		eh.setTileAspectRatio(AspectType.ONETOONE);
		//Which specific format does not matter here - only whether it's a
		//document format or image format because of the size limits imposed by
		//BufferedImage.  Doc format uses BufferedImage for the matrix only.
		//Image format uses it for the whole image.
		eh.setFormat(isDocFormat ?
			FormatType.getDefaultDocumentFormat() : FormatType.PNG);

		//Check if region radio buttons need to be disabled/enabled based on
		//selected region
		RegionType backupReg = null;
		while(rBtns.hasMoreElements()) {
			AbstractButton option = rBtns.nextElement();
			RegionType reg = RegionType.getRegion(option.getText());
			if(option.isSelected()) {
				selectedRegion = reg;
			}
			if(reg != RegionType.SELECTION || selectionsExist) {

				eh.setCalculatedDimensions(reg);
				final boolean isEnabled = !eh.isOversized(reg);
				option.setEnabled(isEnabled);
				if(isEnabled) {
					option.setToolTipText(null);
					backupReg = reg;
				} else {
					option.setToolTipText("Too big for " +
						(isDocFormat ? "document" : "image") + " export");
					if(option.isSelected()) {
						option.setSelected(false);
						selectedRegion = null;
						changeSelected = true;
					}
				}
			} else {
				option.setEnabled(false);
				option.setToolTipText("No selection has been made");
				if(option.isSelected()) {
					option.setSelected(false);
					selectedRegion = null;
					changeSelected = true;
				}
			}
		}

		//If the selected option was disabled, select a new default
		if(changeSelected && backupReg != null) {
			rBtns = regionRadioBtns.getElements();

			while(rBtns.hasMoreElements()) {
				AbstractButton option = rBtns.nextElement();
				if(RegionType.getRegion(option.getText()) == backupReg) {
					selectedRegion = RegionType.getRegion(option.getText());
					option.setSelected(true);
				}
			}
		} else if(changeSelected) {
			LogBuffer.println("ERROR: No regions suffice as a valid default " +
				"in the export dialog.");
		}

		//The aspect radio buttons should be updated based on the selected
		//region
		updateAspectRadioBtns(isDocFormat,selectedRegion);
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

		eh.setRowLabelsIncluded(LabelExportOption.NO);
		eh.setColLabelsIncluded(LabelExportOption.NO);
		eh.setRowTreeIncluded(TreeExportOption.AUTO);
		eh.setColTreeIncluded(TreeExportOption.AUTO);
		//Which specific format does not matter here - only whether it's a
		//document format or image format because of the size limits imposed by
		//BufferedImage.  Doc format uses BufferedImage for the matrix only.
		//Image format uses it for the whole image.
		eh.setFormat(isDocFormat ?
			FormatType.getDefaultDocumentFormat() : FormatType.PNG);

		Enumeration<AbstractButton> aBtns = aspectRadioBtns.getElements();
		boolean changeSelected = false;
		List<AspectType> bigAsps = new ArrayList<AspectType>();
		AspectType selectedAspect = AspectType.getDefault();

		//Check if aspect radio buttons need to be disabled/enabled based on
		//selected region
		AspectType backupAsp = null;
		while(aBtns.hasMoreElements()) {
			AbstractButton option = aBtns.nextElement();
			AspectType asp = AspectType.getAspect(option.getText());
			eh.setTileAspectRatio(asp);
			eh.setCalculatedDimensions(selectedRegion);
			final boolean enabled = !eh.isOversized(selectedRegion);
			if(!enabled) {
				bigAsps.add(asp);
			} else {
				backupAsp = asp;
			}
			if(option.isSelected()) {
				selectedAspect = asp;
			}
			if(!enabled && option.isSelected()) {
				option.setSelected(false);
				changeSelected = true;
			}
			option.setEnabled(enabled);
			if(enabled) {
				option.setToolTipText(null);
			} else {
				option.setToolTipText("Too big for " +
					(isDocFormat ? "document" : "image") + " export");
			}
		}

		//If the selected option was disabled, select a new default
		if(changeSelected && backupAsp != null) {
			aBtns = aspectRadioBtns.getElements();
			while(aBtns.hasMoreElements()) {
				AbstractButton option = aBtns.nextElement();
				AspectType asp = AspectType.getAspect(option.getText());
				if(asp == backupAsp) {
					option.setSelected(true);
					selectedAspect = asp;
				}
			}
		} else if(changeSelected) {
			LogBuffer.println("ERROR: No aspect ratios suffice as a valid " +
				"default in the export dialog.");
		}

		updateLabelBtns(isDocFormat,selectedRegion,selectedAspect);
	}

	public void updateLabelBtns(final boolean isDocFormat,
		final RegionType selectedRegion,final AspectType selectedAspect) {

		//Get the current selected column label option or set to NO to be used
		//in deciding whether the row label options should be enabled
		Enumeration<AbstractButton> cBtns = colLabelBtns.getElements();
		LabelExportOption selectedCLEO = LabelExportOption.NO;
		boolean oneIsSelected = false;
		while(cBtns.hasMoreElements()) {
			AbstractButton option = cBtns.nextElement();
			if(option.isSelected()) {
				selectedCLEO =
					LabelExportOption.getLabelExportOption(option.getText());
				oneIsSelected = true;
				break;
			}
		}
		/* TODO: This should actually be done in updateRowLabelBtns or
		 * updateColLabelBtns.  If there's code there to do it already, it
		 * should be fixed, because it's not working */
		if(!oneIsSelected) {
			cBtns = colLabelBtns.getElements();
			while(cBtns.hasMoreElements()) {
				AbstractButton option = cBtns.nextElement();
				LabelExportOption leo =
					LabelExportOption.getLabelExportOption(option.getText());
				if(leo == selectedCLEO) {
					option.setSelected(true);
					break;
				}
			}
		}

		//The label buttons should be updated based on the selected
		//format, region, and aspect (and column/row labels)
		updateRowLabelBtns(isDocFormat,selectedRegion,selectedAspect,
			selectedCLEO);

		//Get the current selected row label option or set to NO to be used
		//in deciding whether the column label options should be enabled
		Enumeration<AbstractButton> rBtns = rowLabelBtns.getElements();
		LabelExportOption selectedRLEO = LabelExportOption.NO;
		oneIsSelected = false;
		while(rBtns.hasMoreElements()) {
			AbstractButton option = rBtns.nextElement();
			if(option.isSelected()) {
				selectedRLEO =
					LabelExportOption.getLabelExportOption(option.getText());
				oneIsSelected = true;
				break;
			}
		}
		/* TODO: This should actually be done in updateRowLabelBtns or
		 * updateColLabelBtns.  If there's code there toi do it already, it
		 * should be fixed, because it's not working */
		if(!oneIsSelected) {
			rBtns = rowLabelBtns.getElements();
			while(cBtns.hasMoreElements()) {
				AbstractButton option = rBtns.nextElement();
				LabelExportOption leo =
					LabelExportOption.getLabelExportOption(option.getText());
				if(leo == selectedRLEO) {
					option.setSelected(true);
					break;
				}
			}
		}

		updateColLabelBtns(isDocFormat,selectedRegion,selectedAspect,
			selectedRLEO);
	}

	/* TODO: Make sure this takes inclusion of trees into account */
	/**
	 * Update row label buttons based on other selections
	 * 
	 * @param isDocFormat - Whether we're exporting in doc format or not 
	 * @param selectedRegion - the selected region
	 * @param selectedAsp - the selected aspect ratio
	 * @param selectedColLEO - We need to know whether col labels are being
	 *                         exported already
	 */
	public void updateRowLabelBtns(final boolean isDocFormat,
		final RegionType selectedRegion,final AspectType selectedAsp,
		final LabelExportOption selectedColLEO) {

		Enumeration<AbstractButton> rlBtns = rowLabelBtns.getElements();
		boolean changeSelected = false;

		eh.setTileAspectRatio(selectedAsp);
		eh.setColLabelsIncluded(selectedColLEO);
		eh.setRowTreeIncluded(TreeExportOption.AUTO);
		eh.setColTreeIncluded(TreeExportOption.AUTO);
		//Which specific format does not matter here - only whether it's a
		//document format or image format because of the size limits imposed by
		//BufferedImage.  Doc format uses BufferedImage for the matrix only.
		//Image format uses it for the whole image.
		eh.setFormat(isDocFormat ?
			FormatType.getDefaultDocumentFormat() : FormatType.PNG);

		//Check if row label buttons need to be disabled/enabled based on
		//selected region
		LabelExportOption backupLEO = LabelExportOption.NO;
		LabelExportOption selectedRLEO = LabelExportOption.NO;
		while(rlBtns.hasMoreElements()) {
			AbstractButton option = rlBtns.nextElement();
			LabelExportOption leo =
				LabelExportOption.getLabelExportOption(option.getText());
			boolean enabled = true;
			if(leo != LabelExportOption.SELECTION || selectionsExist) {
				eh.setRowLabelsIncluded(leo);
				eh.setCalculatedDimensions(selectedRegion);
				enabled = !eh.isOversized(selectedRegion);
			}
			if(!selectionsExist && leo == LabelExportOption.SELECTION) {
				enabled = false;
			}
			if(!enabled && option.isSelected()) {
				option.setSelected(false);
				changeSelected = true;
			} else {
				if(enabled) {
					backupLEO = leo;
				}
				if(option.isSelected()) {
					selectedRLEO = leo;
				}
			}
			option.setEnabled(leo == LabelExportOption.NO ? true : enabled);
			if(enabled) {
				option.setToolTipText(null);
			} else if(!selectionsExist && leo == LabelExportOption.SELECTION) {
				option.setToolTipText("No selection has been made");
			} else {
				option.setToolTipText("Too big for " +
					(isDocFormat ? "document" : "image") + " export");
			}
		}

		//If the selected option was disabled, select a new default
		if(changeSelected) {
			rlBtns = rowLabelBtns.getElements();
			while(rlBtns.hasMoreElements()) {
				AbstractButton option = rlBtns.nextElement();
				LabelExportOption leo =
					LabelExportOption.getLabelExportOption(option.getText());
				if(leo == backupLEO) {
					option.setSelected(true);
					eh.setRowLabelsIncluded(leo);
				}
			}
		} else {
			eh.setRowLabelsIncluded(selectedRLEO);
		}
	}

	/* TODO: Make sure this takes inclusion of trees into account */
	/**
	 * Update col label buttons based on other selections
	 * 
	 * @param isDocFormat - Whether we're exporting in doc format or not 
	 * @param selectedRegion - the selected region
	 * @param selectedAsp - the selected aspect ratio
	 * @param selectedRowLEO - We need to know whether row labels are being
	 *                         exported already
	 */
	public void updateColLabelBtns(final boolean isDocFormat,
		final RegionType selectedRegion,final AspectType selectedAsp,
		final LabelExportOption selectedRowLEO) {

		Enumeration<AbstractButton> clBtns = colLabelBtns.getElements();
		boolean changeSelected = false;

		eh.setTileAspectRatio(selectedAsp);
		eh.setRowLabelsIncluded(selectedRowLEO);
		eh.setRowTreeIncluded(TreeExportOption.AUTO);
		eh.setColTreeIncluded(TreeExportOption.AUTO);
		//Which specific format does not matter here - only whether it's a
		//document format or image format because of the size limits imposed by
		//BufferedImage.  Doc format uses BufferedImage for the matrix only.
		//Image format uses it for the whole image.
		eh.setFormat(isDocFormat ?
			FormatType.getDefaultDocumentFormat() : FormatType.PNG);

		//Check if col label buttons need to be disabled/enabled based on
		//selected region
		LabelExportOption backupLEO = LabelExportOption.NO;
		LabelExportOption selectedCLEO = LabelExportOption.NO;
		while(clBtns.hasMoreElements()) {
			AbstractButton option = clBtns.nextElement();
			LabelExportOption leo =
				LabelExportOption.getLabelExportOption(option.getText());
			boolean enabled = true;
			if(leo != LabelExportOption.SELECTION || selectionsExist) {
				eh.setColLabelsIncluded(leo);
				eh.setCalculatedDimensions(selectedRegion);
				enabled = !eh.isOversized(selectedRegion);
			}
			if(!selectionsExist && leo == LabelExportOption.SELECTION) {
				enabled = false;
			}
			//If the column label option was disabled, yet is selected, mark the
			//selection to be changed
			if(!enabled && option.isSelected()) {
				option.setSelected(false);
				changeSelected = true;
			} else {
				if(enabled) {
					backupLEO = leo;
				}
				if(option.isSelected()) {
					selectedCLEO = leo;
				}
			}
			option.setEnabled(leo == LabelExportOption.NO ? true : enabled);
			//Update the tool tip
			if(enabled) {
				option.setToolTipText(null);
			} else if(!selectionsExist && leo == LabelExportOption.SELECTION) {
				option.setToolTipText("No selection has been made");
			} else {
				option.setToolTipText("Too big for " +
					(isDocFormat ? "document" : "image") + " export");
			}
		}

		//If the selected option was disabled, select a new default
		if(changeSelected) {
			clBtns = colLabelBtns.getElements();
			while(clBtns.hasMoreElements()) {
				AbstractButton option = clBtns.nextElement();
				LabelExportOption leo =
						LabelExportOption.getLabelExportOption(option.getText());
				if(leo == backupLEO) {
					option.setSelected(true);
					eh.setColLabelsIncluded(leo);
				}
			}
		} else {
			eh.setColLabelsIncluded(selectedCLEO);
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
			cancelButton = GUIFactory.createBtn("cancel");
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
		int answer = JOptionPane.showOptionDialog(c,
			"Are your sure you want to cancel the export?",
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
	 * Sets the members of the passed ExportOptions reference according to the
	 * current state of the GUI.
	 * @param exportOptions - The ExportOptions object
	 */
	public void retrieveOptions(final ExportOptions exportOptions) {
		
		exportOptions.setFormatType((FormatType) formatBox.getSelectedItem());
		exportOptions.setPaperType((PaperType) paperBox.getSelectedItem());
		exportOptions.setOrientation((String) orientBox.getSelectedItem());

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

		/* Labels to be exported */
		LabelExportOption includeRowLabels = LabelExportOption.getDefault();
		buttonText = "default";
		for(Enumeration<AbstractButton> buttons = rowLabelBtns.getElements();
			buttons.hasMoreElements();) {
			AbstractButton button = buttons.nextElement();
			if(button.isSelected()) {
				buttonText = button.getText();
				break;
			}
		}
		for(LabelExportOption leo : LabelExportOption.values()) {
			if(leo.toString().equalsIgnoreCase(buttonText)) {
				includeRowLabels = leo;
				break;
			}
		}
		exportOptions.setRowLabelOption(includeRowLabels);

		LabelExportOption includeColLabels = LabelExportOption.getDefault();
		buttonText = "default";
		for(Enumeration<AbstractButton> buttons = colLabelBtns.getElements();
			buttons.hasMoreElements();) {
			AbstractButton button = buttons.nextElement();
			if(button.isSelected()) {
				buttonText = button.getText();
				break;
			}
		}
		for(LabelExportOption leo : LabelExportOption.values()) {
			if(leo.toString().equalsIgnoreCase(buttonText)) {
				includeColLabels = leo;
				break;
			}
		}
		exportOptions.setColLabelOption(includeColLabels);


		/* Show selections */
		exportOptions.setShowSelections(selectionsBox.isSelected());
	}

	/**
	 * Returns the export button object so we can tell if actions came from it.
	 * @return
	 */
	public Object getExportButton() {
		return(exportBtn);
	}
}
