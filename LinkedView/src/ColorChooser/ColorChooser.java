package ColorChooser;

import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorPresets;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorSet;
import edu.stanford.genetics.treeview.plugin.dendroview.DendrogramFactory;

public class ColorChooser implements ConfigNodePersistent {

	/* Saved data */
	private Preferences configNode;

	/* GUI components */
	private JPanel mainPanel;
	private final GradientBox gradientBox;

	/* For custom ColorSet manipulation */
	private JButton addBtn;
	private JButton removeBtn;
	private JButton missingBtn;

	/* ColorSet choices */
	private JRadioButton redGreenBtn;
	private JRadioButton yellowBlueBtn;
	private JRadioButton customColorBtn;

	/* Holds all preset color data */
	private final ColorPresets colorPresets;

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
	public ColorChooser(final ColorExtractor drawer, final double minVal,
			final double maxVal) {

		this.colorPresets = DendrogramFactory.getColorPresets();
		this.gradientBox = new GradientBox(drawer, minVal, maxVal);

		setLayout();
		setPresets();
	}

	/**
	 * Sets up the GUI layout of the ColorChooser object.
	 */
	private void setLayout() {

		mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
		mainPanel.setBorder(BorderFactory.createEtchedBorder());

		final JLabel hint = GUIFactory.createLabel("Move or add sliders "
				+ "to adjust color scheme.", GUIFactory.FONTS);

		addBtn = GUIFactory.createBtn("Add Color");
		removeBtn = GUIFactory.createBtn("Remove Selected");

		/* Collection of ColorSet choice buttons */
		final ButtonGroup colorBtnGroup = new ButtonGroup();

		redGreenBtn = GUIFactory.createRadioBtn("Red-Green");
		yellowBlueBtn = GUIFactory.createRadioBtn("Yellow-Blue");
		customColorBtn = GUIFactory.createRadioBtn("Custom Colors");
		missingBtn = GUIFactory.createBtn("Missing Data");

		colorBtnGroup.add(redGreenBtn);
		colorBtnGroup.add(yellowBlueBtn);
		colorBtnGroup.add(customColorBtn);

		final JPanel radioButtonPanel = GUIFactory.createJPanel(false,
				GUIFactory.DEFAULT, null);

		final JLabel colorHint = GUIFactory.createLabel("Choose a Color "
				+ "Scheme: ", GUIFactory.FONTS);

		radioButtonPanel.add(colorHint, "span, wrap");
		radioButtonPanel.add(redGreenBtn, "span, wrap");
		radioButtonPanel.add(yellowBlueBtn, "span, wrap");
		radioButtonPanel.add(customColorBtn, "span");

		mainPanel.add(radioButtonPanel, "span, pushx, wrap");
		mainPanel.add(hint, "span, wrap");
		mainPanel.add(gradientBox, "h 150:150:, w 500:500:, pushx, alignx 50%,"
				+ "span, wrap");
		mainPanel.add(addBtn, "pushx, split 3, alignx 50%");
		mainPanel.add(removeBtn, "pushx");
		mainPanel.add(missingBtn, "wrap");
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node("GradientChooser");

			final String colorSet = configNode.get("activeColors", "RedGreen");
			gradientBox.setActiveColorSet(colorPresets.getColorSet(colorSet));

		} else {
			LogBuffer.println("Could not find or create GradientChooser "
					+ "node because parentNode was null.");
		}
	}

	/**
	 * Returns the mainPanel which contains all the GUI components for the
	 * ColorGradientChooser.
	 *
	 * @return
	 */
	public JPanel makeGradientPanel() {

		setPresets();
		return mainPanel;
	}

	/**
	 * Gives access to the GradientBox object which is the JPanel containing the
	 * actual color gradient and thumbs.
	 *
	 * @return
	 */
	protected GradientBox getGradientBox() {

		return gradientBox;
	}

	/* ------------ Accessors ------------- */
	protected Preferences getConfigNode() {

		return configNode;
	}

	protected JPanel getMainPanel() {

		return mainPanel;
	}

	protected JRadioButton getRGButton() {

		return redGreenBtn;
	}

	protected JRadioButton getYBButton() {

		return yellowBlueBtn;
	}

	protected JRadioButton getCustomColorButton() {

		return customColorBtn;
	}

	public boolean isCustomSelected() {

		return isCustomSelected;
	}

	/**
	 * Saves the current colors and fractions as a ColorSet to the configNode.
	 */
	public void saveStatus() {

		if (gradientBox != null) {
			colorPresets.addColorSet(gradientBox.saveCustomPresets());
		}
	}

	/**
	 * Set all default color values.
	 */
	protected void setPresets() {

		final String defaultColors = "RedGreen";

		/* Get the active ColorSet name */
		String colorScheme = defaultColors;
		if (configNode != null) {
			colorScheme = configNode.get("activeColors", defaultColors);
		}

		/* Choose ColorSet according to name */
		final ColorSet selectedColorSet;
		if (colorScheme.equalsIgnoreCase("Custom")) {
			customColorBtn.setSelected(true);
			setCustomSelected(true);
			selectedColorSet = colorPresets.getColorSet(colorScheme);

		} else if (colorScheme.equalsIgnoreCase(defaultColors)) {
			redGreenBtn.setSelected(true);
			setCustomSelected(false);
			selectedColorSet = colorPresets.getColorSet(colorScheme);

		} else if (colorScheme.equalsIgnoreCase("YellowBlue")) {
			yellowBlueBtn.setSelected(true);
			setCustomSelected(false);
			selectedColorSet = colorPresets.getColorSet("YellowBlue");

		} else {
			/* Should never get here */
			selectedColorSet = null;
			LogBuffer.println("No matching ColorSet found in "
					+ "ColorGradientChooser.setPresets()");
		}

		gradientBox.loadPresets(selectedColorSet);
	}

	/**
	 * Sets button status and remembers if custom ColorSet is selected or not.
	 * If not, it disables the add-, remove- and missing color buttons.
	 * 
	 * @param selected
	 *            Whether the custom ColorSet is selected or not.
	 */
	public void setCustomSelected(final boolean selected) {

		this.isCustomSelected = selected;

		addBtn.setEnabled(selected);
		removeBtn.setEnabled(selected);
		missingBtn.setEnabled(selected);
	}

	public void setActiveColorSet(final String name) {

		final ColorSet set = colorPresets.getColorSet(name);
		gradientBox.setActiveColorSet(set);

		configNode.put("activeColors", name);
	}

	/**
	 * Switched the currently used ColorSet to the one that matches the
	 * specified entered name key in its 'ColorSet' configNode.
	 */
	public void switchColorSet(final String name) {

		setActiveColorSet(name);

		/* Load and set data accordingly */
		setPresets();
		gradientBox.setGradientColors();

		/* Update view! */
		mainPanel.repaint();
	}

	protected ColorSet getColorSet(final String name) {

		return colorPresets.getColorSet(name);
	}

	/* ------- GUI component listeners ------------ */

	protected void addThumbSelectListener(final MouseListener l) {

		gradientBox.addMouseListener(l);
	}

	protected void addThumbMotionListener(final MouseMotionListener l) {

		gradientBox.addMouseMotionListener(l);
	}

	protected void addAddListener(final ActionListener l) {

		addBtn.addActionListener(l);
	}

	protected void addRemoveListener(final ActionListener l) {

		removeBtn.addActionListener(l);
	}

	protected void addColorSetListener(final ActionListener l) {

		redGreenBtn.addActionListener(l);
		yellowBlueBtn.addActionListener(l);
		customColorBtn.addActionListener(l);
	}

	protected void addMissingListener(final ActionListener l) {

		missingBtn.addActionListener(l);
	}
}
