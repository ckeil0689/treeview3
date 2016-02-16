package ColorChooser;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import Utilities.CustomDialog;
import Utilities.GUIFactory;
import Utilities.StringRes;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;

/**
 * Constructs the GUI for color selection and manipulation.
 * 
 * @author chris0689
 *
 */
public class ColorChooserUI extends CustomDialog {

	/**
	 * Default serial version ID to keep Eclipse happy...
	 */
	private static final long serialVersionUID = 1L;

	/* GUI components */
	private JPanel contentPanel;
	private JPanel mainPanel;

	private ColorPicker colorPicker;
	private JPanel gradientPanel;

	/* For custom ColorSet manipulation */
	private JButton addBtn;
	private JButton editBtn;
	private JButton removeBtn;
	private JButton missingBtn;
	private JButton applyBtn;

	private ColorIcon missingColorIcon;

	/* ColorSet choices */
	private JComboBox<String> presetChoice;
	private final String[] presets = { "RedGreen", "YellowBlue", "Custom" };

	/* Stores whether custom ColorSet is selected or not */
	private boolean isCustomSelected;

	/**
	 * Constructs a ColorChooser object.
	 *
	 * @param drawer
	 *            The CoorExtractor which defines how colors are mapped to data.
	 * @param minVal
	 *            Minimum boundary of the data.
	 * @param maxVal
	 *            Maximum boundary of the data.
	 */
	public ColorChooserUI(final ColorExtractor drawer, final double minVal,
			final double maxVal, final double mean, final double median) {

		super(StringRes.dlg_Colors);
		this.colorPicker = new ColorPicker(drawer, minVal, maxVal, 
				mean, median);
		this.gradientPanel = colorPicker.getContainerPanel();

		setLayout();
		add(mainPanel);

		pack();
		setLocationRelativeTo(JFrame.getFrames()[0]);
	}

	/**
	 * Sets up the GUI layout of the ColorChooser object.
	 */
	private void setLayout() {

		mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);
		contentPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);

		addBtn = GUIFactory.createBtn("Add New Color");
		editBtn = GUIFactory.createBtn("Edit Selected Color");
		removeBtn = GUIFactory.createBtn("Remove Selected Color");
		applyBtn = GUIFactory.createBtn("Apply");
		getRootPane().setDefaultButton(applyBtn);

		setSelectionDependentBtnStatus(false, false);

		missingColorIcon = new ColorIcon();
		missingBtn = GUIFactory.createColorIconBtn("Missing Data",
				missingColorIcon);

		presetChoice = new JComboBox<String>(presets);

		/* Preset choice panel */
		final JPanel presetChoicePanel = GUIFactory.createJPanel(false,
				GUIFactory.DEFAULT, null);

		presetChoicePanel.add(presetChoice, "pushx");
		presetChoicePanel.add(missingBtn, "pushx");

		final JLabel colorHint = GUIFactory.createLabel("Choose a color "
				+ "scheme: ", GUIFactory.FONTS);
		final JLabel hint = GUIFactory.createLabel("Move, add or edit sliders "
				+ "to adjust color scheme.", GUIFactory.FONTS);

		contentPanel.add(colorHint, "span, wrap");
		contentPanel.add(presetChoicePanel, "span, pushx, wrap");
		contentPanel.add(hint, "span, wrap");
		contentPanel.add(gradientPanel, "h 150:150:, w 650:650:, pushx, "
				+ "alignx 50%, span, wrap");
		contentPanel.add(addBtn, "pushx, split 3, alignx 50%");
		contentPanel.add(removeBtn, "pushx");
		contentPanel.add(editBtn, "pushx");

		mainPanel.add(contentPanel, "push, grow, span, wrap");

		mainPanel.add(applyBtn, "al right, pushx");
		mainPanel.add(closeBtn, "al right");
	}

	/**
	 * Sets the icon of the missing color button to the new color.
	 * 
	 * @param newColor
	 *            The new color to be displayed.
	 */
	protected void updateMissingColorIcon(Color newColor) {

		missingColorIcon.setColor(newColor);
	}

	/**
	 * Updates the status of buttons which are dependent on whether there is any
	 * thumb selected or not.
	 * 
	 * @param enabled
	 */
	protected void setSelectionDependentBtnStatus(boolean editEnabled,
			boolean removeEnabled) {

		editBtn.setEnabled(editEnabled);
		removeBtn.setEnabled(removeEnabled);
	}

	/**
	 * Gives access to the GradientBox object which is the JPanel containing the
	 * actual color gradient and thumbs.
	 *
	 * @return
	 */
	protected ColorPicker getColorPicker() {

		return colorPicker;
	}

	/* ------------ Accessors ------------- */
	protected JPanel getMainPanel() {

		return contentPanel;
	}

	protected JComboBox<String> getPresetChoices() {

		return presetChoice;
	}

	protected void setCustomSelected(boolean isCustom) {

		this.isCustomSelected = isCustom;
	}

	protected boolean isCustomSelected() {

		return isCustomSelected;
	}

	/* ------- GUI component listeners ------------ */

	protected void addThumbSelectListener(final MouseListener l) {

		colorPicker.getContainerPanel().addMouseListener(l);
	}

	protected void addThumbMotionListener(final MouseAdapter l) {

		colorPicker.getContainerPanel().addMouseListener(l);
		colorPicker.getContainerPanel().addMouseMotionListener(l);
	}

	protected void addAddListener(final ActionListener l) {

		addBtn.addActionListener(l);
	}

	protected void addRemoveListener(final ActionListener l) {

		removeBtn.addActionListener(l);
	}

	protected void addPresetChoiceListener(final ActionListener l) {

		presetChoice.addActionListener(l);
	}

	protected void addMissingListener(final ActionListener l) {

		missingBtn.addActionListener(l);
	}

	protected void addEditListener(final ActionListener l) {

		editBtn.addActionListener(l);
	}
	
	protected void addApplyChangeListener(final ActionListener l) {
		
		applyBtn.addActionListener(l);
	}

	protected void addDialogCloseListener(final WindowListener l) {

		this.addWindowListener(l);
	}
}
