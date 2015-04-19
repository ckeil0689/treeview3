package ColorChooser;

import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowListener;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;
import Utilities.CustomDialog;
import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorPresets;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorSet;
import edu.stanford.genetics.treeview.plugin.dendroview.DendrogramFactory;

public class ColorChooserUI extends CustomDialog {//implements ConfigNodePersistent {

//	/* Node for saved data */
//	private Preferences configNode;

	/* GUI components */
	private JPanel contentPanel;
	private JPanel mainPanel;
	
	private ColorPicker colorPicker;
	private JPanel gradientPanel;
//	private final GradientBox gradientBox;

	/* For custom ColorSet manipulation */
	private JButton addBtn;
	private JButton removeBtn;
	private JButton missingBtn;

	/* ColorSet choices */
	private JRadioButton redGreenBtn;
	private JRadioButton yellowBlueBtn;
	private JRadioButton customColorBtn;

//	/* Holds all preset color data */
//	private final ColorPresets colorPresets;

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
		
		dialog.add(mainPanel);
		
		dialog.pack();
		dialog.setLocationRelativeTo(JFrame.getFrames()[0]);
	}

	/**
	 * Sets up the GUI layout of the ColorChooser object.
	 */
	private void setLayout() {

		mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
		
		contentPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
		contentPanel.setLayout(new MigLayout("debug"));
		contentPanel.setBorder(BorderFactory.createEtchedBorder());

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

		contentPanel.add(radioButtonPanel, "span, pushx, wrap");
		contentPanel.add(hint, "span, wrap");
		contentPanel.add(gradientPanel, "h 150:150:, w 500:500:, pushx, "
				+ "alignx 50%, span, wrap");
		contentPanel.add(addBtn, "pushx, split 3, alignx 50%");
		contentPanel.add(removeBtn, "pushx");
		contentPanel.add(missingBtn, "wrap");
		
		mainPanel.add(contentPanel, "push, grow, wrap");
		
		mainPanel.add(closeBtn, "al right, pushx");
		
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

	protected JRadioButton getRGBtn() {

		return redGreenBtn;
	}

	protected JRadioButton getYBBtn() {

		return yellowBlueBtn;
	}

	protected JRadioButton getCustomColorBtn() {

		return customColorBtn;
	}

	public boolean isCustomSelected() {

		return isCustomSelected;
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

	protected void addColorSetListener(final ActionListener l) {

		redGreenBtn.addActionListener(l);
		yellowBlueBtn.addActionListener(l);
		customColorBtn.addActionListener(l);
	}

	protected void addMissingListener(final ActionListener l) {

		missingBtn.addActionListener(l);
	}
	
	protected void addDialogCloseListener(final WindowListener l) {
		
		dialog.addWindowListener(l);
	}
}
