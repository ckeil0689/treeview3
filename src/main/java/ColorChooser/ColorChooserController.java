package ColorChooser;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Observable;

import javax.swing.JColorChooser;
import javax.swing.Timer;

import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorPresets;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorSet;
import edu.stanford.genetics.treeview.plugin.dendroview.DendrogramFactory;

/**
 * Controls user actions for the ColorChooser dialog. It is responsible to initiate the actions conferred by the user.
 * Additionally, it delegates methods necessary to created user expected behavior.
 */
public class ColorChooserController extends Observable {

	public static final Integer DEFAULT_MULTI_CLICK_INTERVAL = 300;
	private final ColorChooserUI colorChooserUI;
	private final ColorPicker colorPicker;

	// Node for saved data
	private ApplyChangeListener applyChangeListener;
	// Holds all preset color data
	private final ColorPresets colorPresets;

	public ColorChooserController(final ColorChooserUI colorChooserUI) {

		this.colorChooserUI = colorChooserUI;
		this.colorPicker = colorChooserUI.getColorPicker();
		this.colorPresets = DendrogramFactory.getColorPresets();

		addAllListeners();
		restoreStateFromColorPresets();
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
			colorChooserUI.addPresetChoiceListener(new PresetChoiceListener());
			colorChooserUI.addMissingListener(new MissingBtnListener());
			colorChooserUI.addEditListener(new EditButtonListener());
			colorChooserUI.addDialogCloseListener(new DialogCloseAdapter());
			
			this.applyChangeListener = new ApplyChangeListener();
			colorChooserUI.addApplyChangeListener(applyChangeListener);
		}
	}
	
	/**
	 * Asks the ColorPresets for the last active ColorSet and adapts the GUI to it.
	 */
	private void restoreStateFromColorPresets() {
		
		if(colorPresets == null) {
			LogBuffer.println("Could not restore state in " + this.getClass().getSimpleName() 
					+ " because ColorPresets were null.");
			return;
		}
		
		ColorSchemeType colorSchemeKey = colorPresets.getLastActiveColorScheme();
		setActiveColorScheme(colorSchemeKey);
		switchColorSet();
	}

	/**
	 * Stores the active <code>ColorSet</code> in the <code>ColorPresets</code> node. Generates a <code>ColorSet</code> 
	 * from custom settings, if currently selected. Calls storeState() in <code>ColorPresets</code>.
	 */
	public void saveColorSettings() {
		
		if(colorPresets == null) {
			LogBuffer.println("Could not save color settings.");
			return;
		}
		
		ColorSchemeType activeColorScheme = (ColorSchemeType) colorChooserUI.getPresetChoices().getSelectedItem();
		colorPresets.setLastActiveColorScheme(activeColorScheme);
		
		if(colorChooserUI.isCustomSelected()) {
			colorPresets.addColorSet(getCurrentCustomColorSet());
		}
		
		colorPresets.storeState();
	}
	
	/**
	 * @return a ColorSet from current values and colors, tagged as Custom.
	 */
	private ColorSet getCurrentCustomColorSet() {
		
		return colorChooserUI.getColorPicker().generateCustomColorSet();
	}
	
	/**
	 * Define which ColorSchemeType is currently active.
	 * @param scheme - The new active scheme
	 */
	private void setActiveColorScheme(final ColorSchemeType scheme) {
		
		colorChooserUI.getPresetChoices().setSelectedItem(scheme);
	}
	
	/**
	 * Define which ColorSchemeType is currently active.
	 * @param scheme
	 */
	private ColorSchemeType getSelectedColorScheme() {
		
		return ((ColorSchemeType) colorChooserUI.getPresetChoices().getSelectedItem());
	}


	/**
	 * Switched the currently used ColorSet to the one that matches the
	 * specified entered name key in its 'ColorSet' configNode.
	 */
	private void switchColorSet() {
		
		ColorSchemeType scheme = getSelectedColorScheme();
		
		final ColorSet set = colorPresets.getColorSet(scheme.toString());
		colorChooserUI.getColorPicker().setActiveColorSet(set);
		
		// Load and set data accordingly
		colorChooserUI.getColorPicker().loadActiveColorSetValues();
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
	 * This listener is fired when the Apply-button is clicked. It translates the chosen colors in the ColorPicker to
	 * the matrix and stores associated values, such as thumb positions and colors.
	 */
	private class ApplyChangeListener implements ActionListener {

		private boolean wasCustomInitiallyPresent = false;
		private boolean wasApplied = false;
		
		public ApplyChangeListener() {
			
			if(colorPresets == null) {
				LogBuffer.println("Cannot check ColorPreset nodes for existing Custom node "
						+ "because the member is null.");
				return;
			}
			
			if(colorPresets.checkNodeExists(ColorSchemeType.CUSTOM.toString()) > -1) {
				wasCustomInitiallyPresent = true;
			}
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			if(!colorChooserUI.isCustomSelected() && hasChanged()) {
				addCurrentSettingsTo(ColorSchemeType.CUSTOM);
				colorChooserUI.getPresetChoices().setSelectedItem(ColorSchemeType.CUSTOM);
			}
			
			colorPicker.setGradientColors();
			saveColorSettings();
			wasApplied = true;
			setChanged();
			notifyObservers();
		}

		public boolean wasApplied() {
			
			return wasApplied;
		}
		
		public boolean wasCustomInitiallyPresent() {
			
			return wasCustomInitiallyPresent;
		}
		
		public void reset() {
			
			this.wasApplied = false;
		}
	}
	
	protected void addCurrentSettingsTo(final ColorSchemeType scheme) {
		
		String name = scheme.toString();
		ColorSet currentCustom = getCurrentCustomColorSet();
		currentCustom.setColorSchemeType(name);
		
		colorPresets.addColorSet(currentCustom);
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
			timer = new Timer(ColorChooserController.getMultiClickInterval(), this);
		}

		/**
		 * Specifies what happens when a single click is performed by the user.
		 */
		private void clickOrPress() {

			GradientBox gBox = colorPicker.getGradientBox();
			if (gBox.isGradientArea(lastEvent.getPoint())) {
				/* loading new presets erases selected thumb... */
				int selectedThumb = colorPicker.getThumbBox().getSelectedThumbIndex();
				gBox.changeColor(lastEvent.getPoint());
				setChanged();

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
			setChanged();
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
	 * This listener defines the behavior of <code>ColorPicker</code> when the user moves the mouse on top a 
	 * thumb or drags it in a certain direction.
	 */
	private class ThumbMotionListener extends MouseAdapter {

		private boolean dragged = false;

		@Override
		public void mouseDragged(final MouseEvent e) {

			dragged = true;
			colorPicker.getThumbBox().dragInnerThumbTo(e.getX());
		}

		@Override
		public void mouseMoved(final MouseEvent e) {

			Cursor cursor = new Cursor(Cursor.DEFAULT_CURSOR);;
			if (colorPicker.getThumbBox().isPointInThumb(e.getPoint())) {
				cursor = new Cursor(Cursor.HAND_CURSOR);
			}

			colorPicker.getContainerPanel().setCursor(cursor);
		}

		@Override
		public void mouseReleased(final MouseEvent e) {

			if (dragged) {
				colorPicker.getThumbBox().dragInnerThumbTo(e.getX());
				dragged = false;
				setChanged();
			}
		}
	}

	/**
	 * Adds a user-selected color to the color list in the <code>GradientBox</code>.
	 */
	private class AddButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			final Color newCol = JColorChooser.showDialog(colorPicker.getContainerPanel(), "Pick a Color", Color.black);

			if (newCol != null) {
				colorPicker.getGradientBox().addColor(newCol);
				updateSelectionBtnStatus();
				setChanged();
			}
		}
	}

	/**
	 * Opens an edit dialog for the selected thumb.
	 */
	private class EditButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			colorPicker.getThumbBox().editSelectedThumb();
			setChanged();
		}
	}

	/**
	 * Removes a color from colorList in the gradientBox.
	 */
	private class RemoveButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			if (colorPicker.isRemovalAllowed()) {
				colorPicker.getGradientBox().removeColor();
				colorChooserUI.setSelectionDependentBtnStatus(false, false);
				setChanged();
			}
		}
	}

	/**
	 * The buttons for editing or removal can be grayed out if the specific action is not allowed. This method
	 * delegates the status update to these buttons.
	 */
	private void updateSelectionBtnStatus() {

		boolean isEditAllowed = colorPicker.getThumbBox().hasSelectedThumb();
		boolean isRemovalAllowed = colorPicker.isRemovalAllowed();

		colorChooserUI.setSelectionDependentBtnStatus(isEditAllowed, isRemovalAllowed);
	}

	/**
	 * Drop-down menu listener checks if the custom color scheme is selected and initiates the switch of the 
	 * active ColorSet.
	 */
	private class PresetChoiceListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent e) {
			switchColorSet();
		}	
	}

	/**
	 * Defines what happens, when the missing color button is clicked. A dialog for choosing a new color will pop up.
	 * If a color was chosen, then it will be updated.
	 */
	private class MissingBtnListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			final Color missing = JColorChooser.showDialog(
					colorChooserUI.getMainPanel(), "Pick Color for Missing", colorPicker.getMissing());

			if (missing != null) {
				colorPicker.setMissing(missing);
				colorChooserUI.updateMissingColorIcon(missing);
				setChanged();
			}
		}
	}
	
	/**
	 * This WindowAdapter executes an action when the ColorChooserUI dialog is closed. It ensures that, if the
	 * apply button was clicked, changes made to the color settings are stored in Preferences nodes so they 
	 * can be reused later for the current file. 
	 * If the the apply button was not clicked, changes made to color settings are erased.
	 */
	private class DialogCloseAdapter extends WindowAdapter {

		@Override
		public void windowClosed(WindowEvent e) {
			
			if(applyChangeListener.wasCustomInitiallyPresent() || applyChangeListener.wasApplied()) {
				LogBuffer.println("Custom was already present or changes were applied.");
				applyChangeListener.reset();
				return;
			}
			
			LogBuffer.println("Not storing color changes since they were not applied.");
			colorPresets.removeColorSet(ColorSchemeType.CUSTOM.ordinal());
		}
	}
}
