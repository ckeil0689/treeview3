package colorChooser;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import util.CustomDialog;
import util.GUIFactory;
import util.StringRes;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;

/** Constructs the GUI for color selection and manipulation.
 * 
 * @author chris0689 */
public class ColorChooserUI extends CustomDialog {

	/** Default serial version ID to keep Eclipse happy... */
	private static final long serialVersionUID = 1L;

	// GUI components
	private JPanel contentPanel;
	private JPanel mainPanel;

	private ColorPicker colorPicker;
	private JPanel gradientPanel;

	// For custom ColorSet manipulation
	private JButton addBtn;
	private JButton editBtn;
	private JButton removeBtn;
	private JButton missingBtn;
	private JButton saveBtn;

	private ColorIcon missingColorIcon;

	// ColorSet choices
	private JComboBox<ColorSchemeType> presetChoice;

	/** Constructs a ColorChooser object.
	 *
	 * @param drawer
	 *          The CoorExtractor which defines how colors are mapped to data.
	 * @param minVal
	 *          Minimum boundary of the data.
	 * @param maxVal
	 *          Maximum boundary of the data. */
	public ColorChooserUI(final ColorExtractor drawer, final double minVal,
												final double maxVal, final double mean,
												final double median) {

		super(StringRes.dlg_Colors);
		this.colorPicker = new ColorPicker(drawer, minVal, maxVal, mean, median);
		this.gradientPanel = colorPicker.getContainerPanel();

		setupLayout();
	}

	/** Sets up the GUI layout of the ColorChooser object. */
	@Override
	protected void setupLayout() {

		this.mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);
		this.contentPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);

		this.addBtn = GUIFactory.createBtn("Add New Color");
		this.editBtn = GUIFactory.createBtn("Edit Selected Color");
		this.removeBtn = GUIFactory.createBtn("Remove Selected Color");
		this.saveBtn = GUIFactory.createBtn("Save");
		getRootPane().setDefaultButton(saveBtn);

		this.closeBtn.setText("Cancel");

		setSelectionDependentBtnStatus(false, false);

		this.missingColorIcon = new ColorIcon();
		this.missingBtn = GUIFactory
																.createColorIconBtn("Missing Data", missingColorIcon);

		this.presetChoice = new JComboBox<ColorSchemeType>(ColorSchemeType
																																			.values());

		// Preset choice panel
		final JPanel presetChoicePanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);

		presetChoicePanel.add(presetChoice, "pushx");
		presetChoicePanel.add(missingBtn, "pushx");

		final JLabel colorHint = GUIFactory.createLabel("Choose a color " +
																										"scheme: ", GUIFactory.FONTS);
		final JLabel hint = GUIFactory.createLabel("Move, add or edit sliders " +
																								"to adjust color scheme.", GUIFactory.FONTS);

		contentPanel.add(colorHint, "span, wrap");
		contentPanel.add(presetChoicePanel, "span, pushx, wrap");
		contentPanel.add(hint, "span, wrap");
		contentPanel.add(gradientPanel, "h 150:150:, w 650:650:, pushx, " +
																		"alignx 50%, span, wrap");
		contentPanel.add(addBtn, "pushx, split 3, alignx 50%");
		contentPanel.add(removeBtn, "pushx");
		contentPanel.add(editBtn, "pushx");

		mainPanel.add(contentPanel, "push, grow, span, wrap");

		mainPanel.add(closeBtn, "span, split 2, tag cancel, sizegroup bttn");
		mainPanel.add(saveBtn, "tag ok, sizegroup bttn");

		getContentPane().add(mainPanel);

		pack();
		setLocationRelativeTo(JFrame.getFrames()[0]);

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	/** Sets the icon of the missing color button to the new color.
	 * 
	 * @param newColor
	 *          The new color to be displayed. */
	public void updateMissingColorIcon(Color newColor) {

		if(missingColorIcon == null || missingBtn == null) {
			LogBuffer.println("Could not update the color icon of the 'Missing Color' button.");
			return;
		}

		missingColorIcon.setColor(newColor);
		missingBtn.repaint();
	}

	/** Updates the status of buttons which are dependent on whether there is any
	 * thumb selected or not.
	 * 
	 * @param enabled */
	public void setSelectionDependentBtnStatus(boolean editEnabled,
																								boolean removeEnabled) {

		editBtn.setEnabled(editEnabled);
		removeBtn.setEnabled(removeEnabled);
	}

	/** Gives access to the GradientBox object which is the JPanel containing the
	 * actual color gradient and thumbs.
	 *
	 * @return */
	public ColorPicker getColorPicker() {

		return colorPicker;
	}

	/* ------------ Accessors ------------- */
	public JPanel getMainPanel() {

		return contentPanel;
	}

	public JComboBox<ColorSchemeType> getPresetChoices() {

		return presetChoice;
	}

	public boolean isCustomSelected() {

		return(getPresetChoices().getSelectedItem() == ColorSchemeType.CUSTOM);
	}

	/* ------- GUI component listeners ------------ */

	public void addThumbSelectListener(final MouseListener l) {

		colorPicker.getContainerPanel().addMouseListener(l);
	}

	public void addThumbMotionListener(final MouseAdapter l) {

		colorPicker.getContainerPanel().addMouseListener(l);
		colorPicker.getContainerPanel().addMouseMotionListener(l);
	}

	public void addAddListener(final ActionListener l) {

		addBtn.addActionListener(l);
	}

	public void addRemoveListener(final ActionListener l) {

		removeBtn.addActionListener(l);
	}

	public void addPresetChoiceListener(final ItemListener l) {

		presetChoice.addItemListener(l);
	}

	public void addMissingListener(final ActionListener l) {

		missingBtn.addActionListener(l);
	}

	public void addEditListener(final ActionListener l) {

		editBtn.addActionListener(l);
	}

	public void addSaveChangesListener(final ActionListener l) {

		saveBtn.addActionListener(l);
	}

	public void addDialogCloseListener(final WindowListener l) {

		this.addWindowListener(l);
	}
}
