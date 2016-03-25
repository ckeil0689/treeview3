package edu.stanford.genetics.treeview;

import java.awt.event.ActionListener;
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.freehep.graphicsio.PageConstants;

import Controllers.ExportHandler;
import Controllers.FormatType;
import Controllers.RegionType;
import Utilities.CustomDialog;
import Utilities.GUIFactory;

public class ExportDialog extends CustomDialog {

	private static final long serialVersionUID = 1L;

	private JPanel previewComp;
	private JComboBox<FormatType> formatBox;
	private JComboBox<PaperType> paperBox;
	private JComboBox<Object> orientBox;
	private ButtonGroup regionRadioBtns;
	private ButtonGroup aspectRadioBtns;
	private JCheckBox selectionsBox;
	private JButton exportBtn;
	private List<RegionType> bigRegs; //List of regions that are too big for image export (doc export is OK)
	private boolean selectionsExist;
	private ExportHandler eh;

	public ExportDialog(final boolean selectionsExist,final ExportHandler eh) {
		super("Export");
		this.eh = eh;
		final boolean useMinimums = true;
		this.bigRegs = eh.getOversizedRegions(useMinimums);
		this.selectionsExist = selectionsExist;
		setupLayout(selectionsExist);
	}

	@Override
	protected void setupLayout() {
		setupLayout(false);
	}

	protected void setupLayout(final boolean selectionsExist) {

		this.mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);

		JPanel contentPanel = GUIFactory.createJPanel(false, 
				GUIFactory.DEFAULT);
		JPanel optionsPanel = GUIFactory.createJPanel(false, 
				GUIFactory.DEFAULT);
		JPanel rangePanel = GUIFactory.createJPanel(false, 
			GUIFactory.DEFAULT);
		JPanel aspectPanel = GUIFactory.createJPanel(false, 
			GUIFactory.DEFAULT);

		/* Shows the preview */
		JPanel previewPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_INSETS);
		previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));
		
		this.previewComp = GUIFactory.createJPanel(false, 
				GUIFactory.NO_INSETS);

		JLabel format = GUIFactory.createLabel("Format:",GUIFactory.FONTS);
		JLabel paper = GUIFactory.createLabel("Paper Size:",GUIFactory.FONTS);
		JLabel region = GUIFactory.createLabel("Export:",GUIFactory.FONTS);
		JLabel aspect = GUIFactory.createLabel("Aspect:",GUIFactory.FONTS);
		JLabel orient = GUIFactory.createLabel("Orientation:",GUIFactory.FONTS);

		FormatType selectedFormat = FormatType.getDefault();
		if(eh.isImageExportPossible()) {
			formatBox = new JComboBox<FormatType>(FormatType.values());
			formatBox.setSelectedItem(FormatType.getDefault());
			
		} else {
			selectedFormat = FormatType.getDefaultDocumentFormat();
			formatBox = new JComboBox<FormatType>(FormatType.getDocumentFormats());
			formatBox.setSelectedItem(FormatType.getDefaultDocumentFormat());
			formatBox.setToolTipText("All regions too big for PNG/JPG/PPM " +
				"export");
		}
		
		paperBox = new JComboBox<PaperType>(PaperType.values());
		paperBox.setSelectedItem(PaperType.getDefault());
		paperBox.setEnabled(selectedFormat.isDocumentFormat());
		
		regionRadioBtns = new ButtonGroup();
		aspectRadioBtns = new ButtonGroup();
		
		selectionsBox = new JCheckBox("Show Selections");
		selectionsBox.setEnabled(selectionsExist);
		if(!selectionsExist) {
			selectionsBox.setToolTipText("No data selected");
		}
		
		orientBox = new JComboBox<Object>(PageConstants.getOrientationList());
		orientBox.setSelectedItem(PageConstants.LANDSCAPE);
		orientBox.setEnabled(selectedFormat.isDocumentFormat());

		previewPanel.add(previewComp, "grow, push");

		optionsPanel.add(format, "label, aligny 0");
		optionsPanel.add(formatBox, "aligny 0, growx, wrap");

		optionsPanel.add(paper, "label");
		optionsPanel.add(paperBox, "growx, wrap");

		optionsPanel.add(orient, "label");
		optionsPanel.add(orientBox, "growx, wrap");

		optionsPanel.add(region,"label, aligny 0");
		RegionType selectedRegion = addRegionRadioButtons(rangePanel,
			selectedFormat);
		rangePanel.add(selectionsBox,"alignx 0, aligny 0");
		optionsPanel.add(rangePanel,"growx, aligny 0, alignx 0, wrap");

		optionsPanel.add(aspect, "label, aligny 0");
		addAspectRadioButtons(aspectPanel,selectedRegion,selectedFormat);
		optionsPanel.add(aspectPanel, "growx, aligny 0, alignx 0, wrap");

		contentPanel.add(previewPanel, "grow");//w 500!, h 500!");
		contentPanel.add(optionsPanel, "aligny 0%, growx, push");
		
		this.exportBtn = GUIFactory.createBtn("Export");
		closeBtn.setText("Cancel");
		
		JPanel btnPanel = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);
		btnPanel.add(closeBtn, "tag cancel, pushx, al right");
		btnPanel.add(exportBtn, "al right");

		mainPanel.add(contentPanel, "push, grow, w 800!, wrap");
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
	public RegionType addRegionRadioButtons(final JPanel rangePanel,
		final FormatType selectedFormat) {

		RegionType selectedRegion = null;
		RegionType defReg = (selectedFormat.isDocumentFormat() ?
			RegionType.getDefault(selectionsExist) :
			RegionType.getDefault(bigRegs,selectionsExist));
		for (RegionType reg : RegionType.values()) {
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
			rangePanel.add(option,"alignx 0, aligny 0, wrap");
		}
		return(selectedRegion);
	}

	/**
	 * Adds radio buttons to aspectRadioBtns and to the supplied aspectPanel and
	 * also disables invalid aspects with a tooltip explaining why based on
	 * bigRegs.  The default aspect is pre-selected.
	 * 
	 * @param aspectPanel
	 * @param selectedRegion
	 */
	public void addAspectRadioButtons(final JPanel aspectPanel,
		final RegionType selectedRegion,final FormatType selectedFormat) {

		for(ExportAspect asp : ExportAspect.values()) {
			JRadioButton option = new JRadioButton(asp.toString());
			List<ExportAspect> tooBigs =
				eh.getOversizedAspects(selectedRegion);
			//Default region pre-selected
			ExportAspect defAsp = (selectedFormat.isDocumentFormat() ?
				ExportAspect.getDefault() : ExportAspect.getDefault(tooBigs));
			if(asp == defAsp) {
				option.setSelected(true);
			}
			if(!selectedFormat.isDocumentFormat() && tooBigs.contains(asp)) {
				option.setEnabled(false);
				option.setToolTipText("Too big for PNG/JPG/PPM export");
			}
			aspectRadioBtns.add(option);
			aspectPanel.add(option, "alignx 0, aligny 0, wrap");
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
		
		selectionsBox.addItemListener(l);
		
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
	 * @return an int array which contains 5 values: 0. the selected index of
	 * the [Format] drop-down. 1. the selected index of the [PaperType] dropdown
	 * 2. the selected index of the [Region] radio buttons 3. the selected index
	 * of the [ExportAspect] radio buttons and 4. whether to [0] not show
	 * selections or [1] show selections.  If any option was not selected, the
	 * selected index returned is -1.  It is assumed that the loops used
	 * encounter enumeration options in the same order in which they are defined
	 * in the enum definitions.
	 */
	public int[] getSelectedOptions() {

		int[] options = new int[5];

		if(formatBox == null) {
			options[0] = -1;
		} else {
			options[0] = formatBox.getSelectedIndex();
		}

		if(paperBox == null) {
			options[1] = -1;
		} else {
			options[1] = paperBox.getSelectedIndex();
		}

		options[2] = -1;
		int cnt = 0;
		for(Enumeration<AbstractButton> buttons =
			regionRadioBtns.getElements();buttons.hasMoreElements();) {
			AbstractButton button = buttons.nextElement();
			if(button.isSelected()) {
				options[2] = cnt;
			}
			cnt++;
		}

		options[3] = -1;
		cnt = 0;
		for(Enumeration<AbstractButton> buttons =
			aspectRadioBtns.getElements();buttons.hasMoreElements();) {
			AbstractButton button = buttons.nextElement();
			if(button.isSelected()) {
				options[3] = cnt;
			}
			cnt++;
		}

		if(selectionsBox == null) {
			options[4] = -1;
		} else if(selectionsBox.isSelected()) {
			options[4] = 1;
		} else {
			options[4] = 0;
		}

		return(options);
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
	public JComboBox<Object> getOrientBox() {
		return(orientBox);
	}

	/**
	 * Arranges the trees and the matrix on the preview panel, depending on
	 * which trees are active.
	 * @param rowTrees - The panel containing the row trees drawing.
	 * @param colTrees - The panel containing the column trees drawing.
	 * @param matrix - The panel containing the matrix drawing.
	 */
	public void setPreview(JPanel rowTrees, JPanel colTrees, JPanel matrix) {

		if(previewComp == null) {
			LogBuffer.println("Cannot set preview for Export.");
			return;
		}

		JPanel filler = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);

		if(rowTrees != null && colTrees != null) {
			previewComp.add(filler, "w 80!, h 80!");
		}

		if(colTrees != null) {
			previewComp.add(colTrees, "growx, pushx, h 80!, w 400!, wrap");
		}

		if(rowTrees != null) {
			previewComp.add(rowTrees, "growy, pushy, h 400!, w 80!");
		}

		previewComp.add(matrix, "h 400!, w 400!, push, grow");
	}
	
	/**
	 * Add a JComponent (e.g. JPanel) to the preview panel.
	 * @param comp The component to be added.
	 */
	public void addToPreviewPanel(final JComponent comp) {

		previewComp.add(comp, "push, grow");
		
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
	 */
	public void updatePreviewComponents(final ExportOptions exportOptions) {
		
		LogBuffer.println("Updating preview components with following options:");
		LogBuffer.println("FormatType: " + exportOptions.getFormatType().toString());
		LogBuffer.println("PaperType: " + exportOptions.getPaperType().toString());
		LogBuffer.println("AspectType: " + exportOptions.getAspectType().toString());
		LogBuffer.println("RegionType: " + exportOptions.getRegionType().toString());
		LogBuffer.println("showSelections: " + exportOptions.isShowSelections());
		
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
		List<ExportAspect> bigAsps = new ArrayList<ExportAspect>();

		//Check if aspect radio buttons need to be disabled/enabled based on
		//selected region
		while(aBtns.hasMoreElements()) {
			AbstractButton option = aBtns.nextElement();
			ExportAspect asp = ExportAspect.getAspect(option.getText());
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
			ExportAspect defAsp;
			if(isDocFormat) {
				defAsp = ExportAspect.getDefault();
			} else {
				defAsp = ExportAspect.getDefault(bigAsps);
			}

			if(defAsp != null) {
				aBtns = aspectRadioBtns.getElements();
				while(aBtns.hasMoreElements()) {
					AbstractButton option = aBtns.nextElement();
					ExportAspect asp = ExportAspect.getAspect(option.getText());
					if(asp == defAsp) {
						option.setSelected(true);
					}
				}
			}
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
		
		ExportAspect aspectType = ExportAspect.getDefault();
		String buttonText = "default";
		for(Enumeration<AbstractButton> buttons = aspectRadioBtns.getElements();
				buttons.hasMoreElements();) {
			AbstractButton button = buttons.nextElement();
			if(button.isSelected()) {
				buttonText = button.getText();
				break;
			}
		}
		
		/* Find which enum member corresponds to the button text */
		for(ExportAspect eA : ExportAspect.values()) {
			if(eA.toString().equalsIgnoreCase(buttonText)) {
				aspectType = eA;
				break;
			}
		}
		
		exportOptions.setAspectType(aspectType);
		
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
		
		/* Find which enum member corresponds to the button text */
		for(RegionType rT : RegionType.values()) {
			if(rT.toString().equalsIgnoreCase(buttonText)) {
				regionType = rT;
				break;
			}
		}
		exportOptions.setRegionType(regionType);
		exportOptions.setShowSelections(selectionsBox.isSelected());
	}
}
