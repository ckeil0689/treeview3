package ColorChooser;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Observable;
import java.util.prefs.Preferences;

import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.Timer;

import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorPresets;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorSet;
import edu.stanford.genetics.treeview.plugin.dendroview.DendrogramFactory;

public class ColorChooserController extends Observable 
implements ConfigNodePersistent {

	public static final Integer DEFAULT_MULTI_CLICK_INTERVAL = 300;
	private final ColorChooserUI colorChooserUI;
	private final ColorPicker colorPicker;
	
//	private CustomDialog drawColorsDialog;

	/* Node for saved data */
	private Preferences configNode;

	/* Holds all preset color data */
	private final ColorPresets colorPresets;

	public ColorChooserController(final ColorChooserUI colorChooserUI) {

		this.colorChooserUI = colorChooserUI;
		this.colorPicker = colorChooserUI.getColorPicker();
		this.colorPresets = DendrogramFactory.getColorPresets();

		addAllListeners();
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
			colorChooserUI.addPresetChoiceListener(new ColorSetListener());
			colorChooserUI.addMissingListener(new MissingBtnListener());
			colorChooserUI.addEditListener(new EditButtonListener());
			colorChooserUI.addApplyChangeListener(new ApplyChangeListener());
			colorChooserUI.addDialogCloseListener(new WindowCloseListener());
		}
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node("GradientChooser");
			setPresets();
			colorChooserUI.getColorPicker().loadPresets();

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
			colorChooserUI.getPresetChoices().setSelectedItem("Custom");
			selectedColorSet = colorPresets.getColorSet(colorScheme);

		} else if (colorScheme.equalsIgnoreCase(defaultColors)) {
			colorChooserUI.getPresetChoices().setSelectedItem("RedGreen");
			selectedColorSet = colorPresets.getColorSet(colorScheme);

		} else if (colorScheme.equalsIgnoreCase("YellowBlue")) {
			colorChooserUI.getPresetChoices().setSelectedItem("YellowBlue");
			selectedColorSet = colorPresets.getColorSet("YellowBlue");

		} else {
			/* Should never get here */
			selectedColorSet = null;
			LogBuffer.println("No matching ColorSet found in "
					+ "ColorGradientChooser.setPresets()");
		}

		colorChooserUI.getColorPicker().setActiveColorSet(selectedColorSet);
	}

	/**
	 * Saves the current colors and fractions as a ColorSet to the configNode.
	 */
	private void saveStatus() {

		ColorSet colorSet = colorChooserUI.getColorPicker().saveCustomPresets();
		colorPresets.addColorSet(colorSet);
	}

	public void setActiveColorSet(final String name) {

		final ColorSet set = colorPresets.getColorSet(name);
		colorChooserUI.getColorPicker().setActiveColorSet(set);

		if ("Custom".equals(name)) {
			colorChooserUI.getPresetChoices().setSelectedItem("Custom");
		}

		configNode.put("activeColors", name);
	}

	/**
	 * Switched the currently used ColorSet to the one that matches the
	 * specified entered name key in its 'ColorSet' configNode.
	 */
	private void switchColorSet(final String name) {

		setActiveColorSet(name);

		/* Load and set data accordingly */
		colorChooserUI.getColorPicker().loadPresets();
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

	private class WindowCloseListener extends WindowAdapter {

		@Override
		public void windowClosed(final WindowEvent e) {
			
			if (colorChooserUI.isCustomSelected()) {
				saveStatus();
			}
		}
	}
	
	private class ApplyChangeListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			
			colorPicker.setGradientColors();
			setChanged();
			notifyObservers();
		}
	}

	/**
	 * This listener specifically defines what happens when a user clicks on a
	 * thumb.
	 *
	 * @author CKeil
	 *
	 */
	private class ThumbSelectListener extends MouseAdapter implements
			ActionListener {

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

			GradientBox gBox = colorPicker.getGradientBox();

			if (gBox.isGradientArea(lastEvent.getPoint())) {
				/* loading new presets erases selected thumb... */
				int selectedThumb = colorPicker.getThumbBox()
						.getSelectedThumbIndex();
				gBox.changeColor(lastEvent.getPoint());
				colorChooserUI.setCustomSelected(true);
				setActiveColorSet("Custom");

				if (selectedThumb > -1) {
					Thumb t_selected = colorPicker.getThumb(selectedThumb);
					colorPicker.getThumbBox().setSelectedThumb(t_selected);
				}

			} else {
				ThumbBox tb = colorPicker.getThumbBox();
				tb.deselectAllThumbs();
				tb.selectThumbAtPoint(lastEvent.getPoint());
				updateSelectionBtnStatus();
			}
		}

		/**
		 * Specifies what happens when a double click is performed by the user.
		 */
		private void doubleClick() {

			colorPicker.getThumbBox().editClickedThumb(lastEvent.getPoint());
			colorChooserUI.setCustomSelected(true);
			setActiveColorSet("Custom");
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
	private class ThumbMotionListener extends MouseAdapter {

		private boolean dragged = false;

		@Override
		public void mouseDragged(final MouseEvent e) {

			dragged = true;
			colorPicker.getThumbBox().moveThumbTo(e.getX());
		}

		@Override
		public void mouseMoved(final MouseEvent e) {

			Cursor cursor;
			if (colorPicker.getThumbBox().isPointInThumb(e.getPoint())) {
				cursor = new Cursor(Cursor.HAND_CURSOR);

			} else {
				cursor = new Cursor(Cursor.DEFAULT_CURSOR);
			}

			colorPicker.getContainerPanel().setCursor(cursor);
		}

		@Override
		public void mouseReleased(final MouseEvent e) {

			if (!colorChooserUI.isCustomSelected() && dragged) {
				colorPicker.getThumbBox().moveThumbTo(e.getX());
				colorChooserUI.setCustomSelected(true);
				setActiveColorSet("Custom");
				dragged = false;
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

			final Color newCol = JColorChooser.showDialog(
					colorPicker.getContainerPanel(), "Pick a Color",
					Color.black);

			if (newCol != null) {
				colorPicker.getGradientBox().addColor(newCol);
				updateSelectionBtnStatus();
			}
		}
	}

	/**
	 * Removes a color from colorList in the gradientBox.
	 *
	 * @author chris0689
	 *
	 */
	private class EditButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			colorPicker.getThumbBox().editSelectedThumb();
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

			if (colorPicker.getColorList().size() > 2) {
				colorPicker.getGradientBox().removeColor();
				colorChooserUI.setSelectionDependentBtnStatus(false, false);
			}
		}
	}

	private void updateSelectionBtnStatus() {

		ThumbBox tb = colorPicker.getThumbBox();

		boolean isBoundary = tb.isSelectedBoundaryThumb();
		boolean isEditAllowed = tb.hasSelectedThumb();
		boolean isRemoveAllowed = !isBoundary && isEditAllowed
				&& colorPicker.getThumbNumber() > 2;

		colorChooserUI.setSelectionDependentBtnStatus(isEditAllowed,
				isRemoveAllowed);
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

			@SuppressWarnings("unchecked")
			String selected = (String) ((JComboBox<String>) arg0.getSource())
					.getSelectedItem();

			if (selected.equalsIgnoreCase("RedGreen")) {
				/* Switch to RedGreen */
				colorSetName = "RedGreen";
				colorChooserUI.setCustomSelected(false);
				//TODO reset values!

			} else if (selected.equalsIgnoreCase("YellowBlue")) {
				/* Switch to YellowBlue */
				colorSetName = "YellowBlue";
				colorChooserUI.setCustomSelected(false);

			} else {
				/* Switch to Custom */
				colorSetName = "Custom";
				colorChooserUI.setCustomSelected(true);
			}

			switchColorSet(colorSetName);
		}
	}

	private class MissingBtnListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			final Color missing = JColorChooser.showDialog(
					colorChooserUI.getMainPanel(), "Pick Color for Missing",
					colorPicker.getMissing());

			if (missing != null) {
				colorPicker.setMissing(missing);
				/* update the color icon */
				colorChooserUI.updateMissingColorIcon(missing);
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
