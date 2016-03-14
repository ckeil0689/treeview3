package edu.stanford.genetics.treeview;

import java.awt.event.ActionListener;
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

import Controllers.Format;
import Utilities.CustomDialog;
import Utilities.GUIFactory;
import Controllers.Region;

public class ExportDialog extends CustomDialog {

	private static final long serialVersionUID = 1L;

	private JPanel previewComp;
	private JComboBox<Object> formatBox;
	private JComboBox<Object> paperBox;
	private JComboBox<Object> orientBox;
	private ButtonGroup regionRadioBtns;
	private ButtonGroup aspectRadioBtns;
	private JCheckBox selectionsBox;
	private JButton exportBtn;
	private List<Region> bigRegs; //List of regions that are too big for image export (doc export is OK)
	private boolean selectionsExist;

	public ExportDialog(final boolean selectionsExist,
		final List<Region> bigRegs) {
		
		super("Export");
		this.bigRegs = bigRegs;
		this.selectionsExist = selectionsExist;
		setupLayout(selectionsExist);
	}

	@Override
	protected void setupLayout() {
		setupLayout(false);
	}

	protected void setupLayout(final boolean selectionsExist) {

		mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);
		
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

		formatBox = new JComboBox<Object>(Format.values());
		formatBox.setSelectedItem(Format.getDefault());
		paperBox = new JComboBox<Object>(PaperType.values());
		paperBox.setSelectedItem(PaperType.getDefault());
		paperBox.setEnabled(Format.getDefault().isDocumentFormat());
		regionRadioBtns = new ButtonGroup();
		aspectRadioBtns = new ButtonGroup();
		selectionsBox = new JCheckBox("Show Selections");
		selectionsBox.setEnabled(selectionsExist);
		if(!selectionsExist) {
			selectionsBox.setToolTipText("No data selected");
		}
		orientBox = new JComboBox<Object>(PageConstants.getOrientationList());
		orientBox.setSelectedItem(PageConstants.LANDSCAPE);
		orientBox.setEnabled(Format.getDefault().isDocumentFormat());

		previewPanel.add(previewComp, "grow, push");

		optionsPanel.add(format, "label, aligny 0");
		optionsPanel.add(formatBox, "aligny 0, growx, wrap");

		optionsPanel.add(paper, "label");
		optionsPanel.add(paperBox, "growx, wrap");

		optionsPanel.add(orient, "label");
		optionsPanel.add(orientBox, "growx, wrap");

		optionsPanel.add(region,"label, aligny 0");
		Region defReg = Region.getDefault();
		if(defReg == Region.SELECTION && !selectionsExist) {
			defReg = Region.VISIBLE;
		}
		for (Region reg : Region.values()) {
			JRadioButton option = new JRadioButton(reg.toString());
			if(bigRegs.contains(reg)) {
				option.setEnabled(false);
				option.setToolTipText("Too big for PNG/JPG/PPM export");
			}
			//Default region pre-selected
			if(reg == defReg && !bigRegs.contains(reg)) {
				option.setSelected(true);
			}
			if(reg == Region.SELECTION && !bigRegs.contains(reg)) {
				option.setEnabled(selectionsExist);
				if(!selectionsExist) {
					option.setToolTipText("No data selected");
				}
			}
			regionRadioBtns.add(option);
			rangePanel.add(option,"alignx 0, aligny 0, wrap");
		}
		rangePanel.add(selectionsBox,"alignx 0, aligny 0");
		optionsPanel.add(rangePanel,"growx, aligny 0, alignx 0, wrap");

		optionsPanel.add(aspect, "label, aligny 0");
		for(ExportAspect asp : ExportAspect.values()) {
			JRadioButton option = new JRadioButton(asp.toString());
			//Default region pre-selected
			if(asp == ExportAspect.getDefault()) {
				option.setSelected(true);
			}
			aspectRadioBtns.add(option);
			aspectPanel.add(option, "alignx 0, aligny 0, wrap");
		}
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
	 * Add export action to export button. Determines what happens, when
	 * the export button is clicked.
	 * @param l The ActionListener
	 */
	public void addExportListener(final ActionListener l) {
		exportBtn.addActionListener(l);
	}

	public void addFormatListener(final ActionListener l) {
		formatBox.addActionListener(l);
	}

	public void addAspectListener(final ActionListener l) {
		Enumeration<AbstractButton> eab = aspectRadioBtns.getElements();
		while(eab.hasMoreElements()) {
			AbstractButton btn = eab.nextElement();
			btn.addActionListener(l);
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
	public JComboBox<Object> getPaperBox() {
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

	public List<Region> getBigRegs() {
		return bigRegs;
	}

	public void setBigRegs(List<Region> bigRegs) {
		this.bigRegs = bigRegs;
	}

	public void updateRegionRadioBtns(final boolean isDocFormat) {
		Enumeration<AbstractButton> rBtns = regionRadioBtns.getElements();
		while(rBtns.hasMoreElements()) {
			AbstractButton option = rBtns.nextElement();
			final boolean isEnabled = isDocFormat ||
					!bigRegs.contains(Region.getRegion(option.getText()));
			if(Region.getRegion(option.getText()) != Region.SELECTION ||
				selectionsExist) {
				option.setEnabled(isEnabled);
				if(isEnabled) {
					option.setToolTipText(null);
				} else {
					option.setToolTipText("Too big for PNG/JPG/PPM export");
				}
			}
		}
	}
}
