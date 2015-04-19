package ColorChooser;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

import javax.swing.JColorChooser;
import javax.swing.Timer;

import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorPresets;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorSet;
import edu.stanford.genetics.treeview.plugin.dendroview.DendrogramFactory;

public class ColorChooserController implements ConfigNodePersistent {

	public static final Integer DEFAULT_MULTI_CLICK_INTERVAL = 300;
	private final ColorChooserUI colorChooserUI;
	
	/* Node for saved data */
	private Preferences configNode;
	
	/* Holds all preset color data */
	private final ColorPresets colorPresets;

	public ColorChooserController(final ColorChooserUI colorChooserUI) {

		this.colorChooserUI = colorChooserUI;
		this.colorPresets = DendrogramFactory.getColorPresets();
		
		setPresets();

		addAllListeners();
	}
	
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node("GradientChooser");

			final String colorSet = configNode.get("activeColors", "RedGreen");
			ColorSet activeSet = colorPresets.getColorSet(colorSet);
			
			colorChooserUI.getColorPicker().setActiveColorSet(activeSet);

		} else {
			LogBuffer.println("Could not find or create GradientChooser "
					+ "node because parentNode was null.");
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
			colorChooserUI.getCustomColorBtn().setSelected(true);
			colorChooserUI.setCustomSelected(true);
			selectedColorSet = colorPresets.getColorSet(colorScheme);

		} else if (colorScheme.equalsIgnoreCase(defaultColors)) {
			colorChooserUI.getRGBtn().setSelected(true);
			colorChooserUI.setCustomSelected(false);
			selectedColorSet = colorPresets.getColorSet(colorScheme);

		} else if (colorScheme.equalsIgnoreCase("YellowBlue")) {
			colorChooserUI.getYBBtn().setSelected(true);
			colorChooserUI.setCustomSelected(false);
			selectedColorSet = colorPresets.getColorSet("YellowBlue");

		} else {
			/* Should never get here */
			selectedColorSet = null;
			LogBuffer.println("No matching ColorSet found in "
					+ "ColorGradientChooser.setPresets()");
		}

		colorChooserUI.getColorPicker().loadPresets(selectedColorSet);
	}
	
	/**
	 * Saves the current colors and fractions as a ColorSet to the configNode.
	 */
	private void saveStatus() {

		if (colorChooserUI.getColorPicker() != null) {
			ColorSet colorSet = colorChooserUI.getColorPicker()
					.saveCustomPresets();
			colorPresets.addColorSet(colorSet);
		}
	}
	
	public void setActiveColorSet(final String name) {

		final ColorSet set = colorPresets.getColorSet(name);
		colorChooserUI.getColorPicker().setActiveColorSet(set);

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
		colorChooserUI.getColorPicker().setGradientColors();

		/* Update view! */
		colorChooserUI.getMainPanel().repaint();
	}

	/**
	 * Returns the system multi-click interval.
	 */
	public static int getMultiClickInterval() {
		Integer multiClickInterval = (Integer) Toolkit.getDefaultToolkit()
				.getDesktopProperty("awt.multiClickInterval");

		if (multiClickInterval == null) {
			multiClickInterval = DEFAULT_MULTI_CLICK_INTERVAL;
		}

		return multiClickInterval;
	}

	/**
	 * Adds all defined listeners to the ColorGradientChooser object.
	 */
	private void addAllListeners() {

		if (colorChooserUI != null) {
			colorChooserUI.addThumbSelectListener(new ThumbSelectListener());
			colorChooserUI.addThumbMotionListener(new ThumbMotionListener());
			colorChooserUI.addAddListener(new AddButtonListener());
			colorChooserUI.addRemoveListener(new RemoveButtonListener());
			colorChooserUI.addColorSetListener(new ColorSetListener());
			colorChooserUI.addMissingListener(new MissingBtnListener());
			colorChooserUI.addDialogCloseListener(new WindowCloseListener());
		}
	}
	
	private class WindowCloseListener extends WindowAdapter {
		
		@Override
		public void windowClosed(final WindowEvent e) {

			if (colorChooserUI != null && colorChooserUI.isCustomSelected()) {
				saveStatus();
			}
		}
	}

	/**
	 * This listener specifically defines what happens when a user clicks on a
	 * thumb.
	 *
	 * @author CKeil
	 *
	 */
	private class ThumbSelectListener extends MouseAdapter 
	implements ActionListener {

		private final Timer timer;
		private MouseEvent lastEvent;

		protected ThumbSelectListener() {
			timer = new Timer(ColorChooserController.getMultiClickInterval(),
					this);
		}

		/**
		 * Specifies what happens when a single click is performed by the user.
		 */
		private void clickOrPress() {

			if (colorChooserUI.isCustomSelected()) {
				if (gradientBox.isGradientArea(lastEvent.getPoint())) {
					gradientBox.changeColor(lastEvent.getPoint());
					setActiveColorSet("Custom");

				} else {
					gradientBox.deselectAllThumbs();
					gradientBox.selectThumb(lastEvent.getPoint());
				}
			}
		}

		/**
		 * Specifies what happens when a double click is performed by the user.
		 */
		private void doubleClick() {

			if (colorChooserUI.isCustomSelected()) {
				gradientBox.setThumbPosition(lastEvent.getPoint());
			}
		}

		@Override
		public void mouseClicked(final MouseEvent e) {

			lastEvent = e;

			if (timer.isRunning()) {
				timer.stop();

				if (e.getClickCount() >= 2) {
					doubleClick();
				}
			} else {
				timer.restart();
			}
		}

		@Override
		public void mousePressed(final MouseEvent e) {

			lastEvent = e;
			clickOrPress();
		}

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			timer.stop();
			clickOrPress();
		}
	}

	/**
	 * This listener defines the behavior of ColorGradientChooser when the user
	 * moves the mouse on top a thumb or drags it in a certain direction.
	 *
	 * @author CKeil
	 *
	 */
	private class ThumbMotionListener implements MouseMotionListener {

		@Override
		public void mouseDragged(final MouseEvent e) {

			if (colorChooserUI.isCustomSelected()) {
				gradientBox.updateThumbPos(e.getX());
			}
		}

		@Override
		public void mouseMoved(final MouseEvent e) {

			if (colorChooserUI.isCustomSelected()) {
				if (gradientBox.containsThumb(e.getX(), e.getY())) {
					gradientBox.setCursor(new Cursor(Cursor.HAND_CURSOR));

				} else {
					gradientBox.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			}
		}
	}

	/**
	 * Adds a user-selected color to the colorList in the gradientBox.
	 *
	 * @author chris0689
	 *
	 */
	private class AddButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			final Color newCol = JColorChooser.showDialog(gradientBox,
					"Pick a Color", Color.black);

			if (newCol != null) {
				gradientBox.addColor(newCol);
			}
		}
	}

	/**
	 * Removes a color from colorList in the gradientBox.
	 *
	 * @author chris0689
	 *
	 */
	private class RemoveButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			if (gradientBox.getColorListSize() > 2) {
				gradientBox.removeColor();
			}
		}
	}

	/**
	 * Radio-button controls over which ColorSet is active.
	 *
	 * @author chris0689
	 *
	 */
	private class ColorSetListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			boolean isCustom = colorChooserUI.isCustomSelected();
			String colorSetName = "";

			/* Save if switching from 'Custom' */
			if (isCustom) {
				saveStatus();
			}

			if (arg0.getSource() == colorChooserUI.getRGBtn()) {
				/* Switch to RedGreen */
				colorSetName = "RedGreen";
				isCustom = false;

			} else if (arg0.getSource() == colorChooserUI.getYBBtn()) {
				/* Switch to YellowBlue */
				colorSetName = "YellowBlue";
				isCustom = false;

			} else {
				/* Switch to Custom */
				colorSetName = "Custom";
				isCustom = true;
			}

			switchColorSet(colorSetName);
			colorChooserUI.setCustomSelected(isCustom);
		}
	}

	private class MissingBtnListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			if (colorChooserUI.isCustomSelected()) {
				final Color missing = JColorChooser.showDialog(
						colorChooserUI.getMainPanel(), "Pick Color for Missing",
						gradientBox.getMissing());

				if (missing != null) {
					gradientBox.setMissing(missing);
				}
			}
		}
	}
	
	protected Preferences getConfigNode() {

		return configNode;
	}
	
	protected ColorSet getColorSet(final String name) {

		return colorPresets.getColorSet(name);
	}
}
