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

	// Node for saved data
	private Preferences configNode;
	private ApplyChangeListener applyChangeListener;

	// Holds all preset color data
	private final ColorPresets colorPresets;
	private final ColorSchemeType d_colorScheme = ColorSchemeType.REDGREEN;
	private ColorSchemeType colorScheme;

	public ColorChooserController(final ColorChooserUI colorChooserUI) {

		this.colorChooserUI = colorChooserUI;
		this.colorPicker = colorChooserUI.getColorPicker();
		this.colorPresets = DendrogramFactory.getColorPresets();
		this.colorScheme = d_colorScheme;

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
			colorChooserUI.addPresetChoiceListener(new PresetChoiceListener());
			colorChooserUI.addMissingListener(new MissingBtnListener());
			colorChooserUI.addEditListener(new EditButtonListener());
			colorChooserUI.addDialogCloseListener(new DialogCloseAdapter());
			
			this.applyChangeListener = new ApplyChangeListener();
			colorChooserUI.addApplyChangeListener(applyChangeListener);
		}
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode == null) {
			LogBuffer.println("Could not find or create GradientChooser node because parentNode was null.");
			return;
		}
		
		this.configNode = parentNode.node("GradientChooser");
		requestStoredState();
		switchColorSet(colorScheme);
		colorChooserUI.getColorPicker().loadPresets();
	}
	
	@Override
	public Preferences getConfigNode() {

		return configNode;
	}
	
	@Override
	public void requestStoredState() {
		
		importStateFrom(configNode);
	}

	@Override
	public void storeState() {
		
		if(configNode == null || colorPresets == null) {
			LogBuffer.println("Could not store state in ColorChooserController.");
			return;
		}
		
		configNode.put("activeColors", colorScheme.toString());
		colorPresets.addColorSet(getCurrentCustomColorSet());
	}
	
	@Override
	public void importStateFrom(Preferences oldNode) {
		
		if(oldNode == null) {
			LogBuffer.println("Could not restore saved state. " + this.getClass().toString());
			return;
		}
		
		String colorSchemeKey = oldNode.get("activeColors", d_colorScheme.toString());
		this.colorScheme = ColorSchemeType.getMemberFromKey(colorSchemeKey);
	}
	
	private ColorSet getCurrentCustomColorSet() {
		
		LogBuffer.println("Generating custom ColorSet...");
		return colorChooserUI.getColorPicker().generateCustomColorSet();
	}
	
	private void setColorScheme(final ColorSchemeType scheme) {
		
		this.colorScheme = scheme;
	}


	/**
	 * Switched the currently used ColorSet to the one that matches the
	 * specified entered name key in its 'ColorSet' configNode.
	 */
	private void switchColorSet(final ColorSchemeType scheme) {
		
		LogBuffer.println("Updating ColorPicker to: " + scheme.toString());
		
		final ColorSet set = colorPresets.getColorSet(scheme.toString());
		colorChooserUI.getColorPicker().setActiveColorSet(set);
		
		setColorScheme(scheme);
//		colorChooserUI.getPresetChoices().setSelectedItem(colorScheme);
		
		// Load and set data accordingly
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
	
	/**
	 * This listener is fired when the Apply-button is clicked. It translates the chosen colors in the ColorPicker to
	 * the matrix and stores associated values, such as thumb positions and colors.
	 */
	private class ApplyChangeListener implements ActionListener {

		private boolean wasApplied = false;
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			colorPicker.setGradientColors();
			wasApplied = true;
			setChanged();
			notifyObservers();
		}

		public boolean wasApplied() {
			
			return wasApplied;
		}
		
		public void reset() {
			
			this.wasApplied = false;
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
			timer = new Timer(ColorChooserController.getMultiClickInterval(), this);
		}

		/**
		 * Specifies what happens when a single click is performed by the user.
		 */
		private void clickOrPress() {

			GradientBox gBox = colorPicker.getGradientBox();
			LogBuffer.println("clickOrPress()");
			if (gBox.isGradientArea(lastEvent.getPoint())) {
				/* loading new presets erases selected thumb... */
				LogBuffer.println("in gradient area");
				int selectedThumb = colorPicker.getThumbBox().getSelectedThumbIndex();
				gBox.changeColor(lastEvent.getPoint());
				colorChooserUI.setCustomSelected(true);

				if (selectedThumb > -1) {
					Thumb t_selected = colorPicker.getThumb(selectedThumb);
					colorPicker.getThumbBox().setSelectedThumb(t_selected);
				}

			} else {
				LogBuffer.println("Outside gradient box");
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

			LogBuffer.println("doubleClick()");
			colorPicker.getThumbBox().editClickedThumb(lastEvent.getPoint());
			colorChooserUI.setCustomSelected(true);
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
			LogBuffer.println("mousePressed");
			clickOrPress();
		}

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			timer.stop();
			LogBuffer.println("actionPerformed");
			clickOrPress();
		}
	}

	/**
	 * This listener defines the behavior of ColorGradientChooser when the user
	 * moves the mouse on top a thumb or drags it in a certain direction.
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
				LogBuffer.println("ThumbMoion.mouseReleased()");
				colorPicker.getThumbBox().dragInnerThumbTo(e.getX());
				colorChooserUI.setCustomSelected(true);
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

			final Color newCol = JColorChooser.showDialog(colorPicker.getContainerPanel(), "Pick a Color", Color.black);

			if (newCol != null) {
				LogBuffer.println("Added button");
				colorPicker.getGradientBox().addColor(newCol);
				updateSelectionBtnStatus();
				colorChooserUI.setCustomSelected(true);
				
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

			LogBuffer.println("Editing button");
			colorPicker.getThumbBox().editSelectedThumb();
			colorChooserUI.setCustomSelected(true);
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
				LogBuffer.println("Removed button");
				colorPicker.getGradientBox().removeColor();
				colorChooserUI.setSelectionDependentBtnStatus(false, false);
				colorChooserUI.setCustomSelected(true);
			}
		}
	}

	private void updateSelectionBtnStatus() {

		ThumbBox tb = colorPicker.getThumbBox();

		boolean isBoundary = tb.isSelectedBoundaryThumb();
		boolean isEditAllowed = tb.hasSelectedThumb();
		boolean isRemoveAllowed = !isBoundary && isEditAllowed && colorPicker.getThumbNumber() > 2;

		colorChooserUI.setSelectionDependentBtnStatus(isEditAllowed, isRemoveAllowed);
	}

	/**
	 * Drop-down menu listener checks if the custom color scheme is selected and initiates the switch of the 
	 * active ColorSet.
	 */
	private class PresetChoiceListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			
			if(colorChooserUI.isCustomSelected()) {
				LogBuffer.println("Storing custom color set before switching.");
				colorPresets.addColorSet(getCurrentCustomColorSet());
			}
			
			@SuppressWarnings("unchecked")
			Object selectedItem = ((JComboBox<ColorSchemeType>) arg0.getSource()).getSelectedItem();
			ColorSchemeType selectedScheme = (ColorSchemeType) selectedItem;

			boolean customSelected = (selectedScheme == ColorSchemeType.CUSTOM);
			colorChooserUI.setCustomSelected(customSelected);
			LogBuffer.println("Changed presetChoice");
			switchColorSet(selectedScheme);
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
				LogBuffer.println("Updated missing color");
				colorPicker.setMissing(missing);
				colorChooserUI.updateMissingColorIcon(missing);
				colorChooserUI.setCustomSelected(true);
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
			
			LogBuffer.println("Window closed event.");
			if(applyChangeListener.wasApplied()) {
				LogBuffer.println("Saving applied changes.");
				storeState();
				applyChangeListener.reset();
				return;
			}
			
			LogBuffer.println("Not storing color changes since they were not applied.");
			colorPresets.removeColorSet(ColorSchemeType.CUSTOM.ordinal());
		}
	}
}
