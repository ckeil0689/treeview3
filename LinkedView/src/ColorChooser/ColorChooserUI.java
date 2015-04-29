package ColorChooser;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import Utilities.CustomDialog;
import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;

/**
 * Constructs the GUI for color selection and manipulation.
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
	
	private ColorIcon missingColorIcon;

	/* ColorSet choices */
	private JComboBox<String> presetChoice;
	private final String[] presets = {"RedGreen", "YellowBlue", 
			"Custom Colors"};

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
			final double maxVal) {

		super("Choose matrix colors");
		this.colorPicker = new ColorPicker(drawer, minVal,maxVal);
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

		mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
		
		contentPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
		contentPanel.setBorder(BorderFactory.createEtchedBorder());

		addBtn = GUIFactory.createBtn("Add New Color");
		editBtn = GUIFactory.createBtn("Edit Selected Thumb");
		removeBtn = GUIFactory.createBtn("Remove Selected Color");
		
		setSelectionDependentBtnStatus(false);
		
		missingColorIcon = new ColorIcon();
		missingBtn = GUIFactory.createColorIconBtn("Missing Data", 
				missingColorIcon);

		presetChoice = new JComboBox<String>(presets);

		/* Preset choice panel */
		final JPanel presetChoicePanel = GUIFactory.createJPanel(false,
				GUIFactory.DEFAULT, null);

		final JLabel colorHint = GUIFactory.createLabel("Choose a Color "
				+ "Scheme: ", GUIFactory.FONTS);

		presetChoicePanel.add(colorHint, "span, wrap");
		presetChoicePanel.add(presetChoice, "pushx");
		presetChoicePanel.add(missingBtn, "pushx");
		
		final JLabel hint = GUIFactory.createLabel("Move, add or edit sliders "
				+ "to adjust color scheme.", GUIFactory.FONTS);
		
		contentPanel.add(presetChoicePanel, "span, pushx, wrap");
		contentPanel.add(hint, "span, wrap");
		contentPanel.add(gradientPanel, "h 150:150:, w 500:500:, pushx, "
				+ "alignx 50%, span, wrap");
		contentPanel.add(addBtn, "pushx, split 3, alignx 50%");
		contentPanel.add(removeBtn, "pushx");
		contentPanel.add(editBtn, "pushx");
		
		mainPanel.add(contentPanel, "push, grow, wrap");
		
		mainPanel.add(closeBtn, "al right, pushx");
	}
	
	/**
	 * Sets the icon of the missing color button to the new color.
	 * @param newColor The new color to be displayed.
	 */
	protected void updateMissingColorIcon(Color newColor) {
		
		missingColorIcon.setColor(newColor);
	}
	
	/**
	 * Updates the status of buttons which are dependent on whether there
	 * is any thumb selected or not.
	 * @param enabled
	 */
	protected void setSelectionDependentBtnStatus(boolean enabled) {
		
		editBtn.setEnabled(enabled);
		removeBtn.setEnabled(enabled);
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

	protected boolean isCustomSelected() {

		return isCustomSelected;
	}

//	/**
//	 * Sets button status and remembers if custom ColorSet is selected or not.
//	 * If not, it disables the add-, remove- and missing color buttons.
//	 *
//	 * @param selected
//	 *            Whether the custom ColorSet is selected or not.
//	 */
//	protected void setCustomSelected(final boolean selected) {
//
//		this.isCustomSelected = selected;
//
//		addBtn.setEnabled(selected);
//		removeBtn.setEnabled(selected);
//		missingBtn.setEnabled(selected);
//	}

	/* ------- GUI component listeners ------------ */

	protected void addThumbSelectListener(final MouseListener l) {

		colorPicker.getContainerPanel().addMouseListener(l);
	}

	protected void addThumbMotionListener(final MouseMotionListener l) {

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
	
	protected void addDialogCloseListener(final WindowListener l) {
		
		this.addWindowListener(l);
	}
}
