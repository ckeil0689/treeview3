package gui.settings;

import gui.fileImport.labels.LabelLoadDialog;
import gui.window.TreeViewFrame;
import model.data.labels.LabelInfo;
import model.data.matrix.DataModel;
import model.fileImport.LoadException;
import model.fileImport.labels.CustomLabelLoader;
import util.LogBuffer;
import util.StringRes;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

/**
 * Controller for the PreferencesMenu class. Handles user interaction with Swing
 * components such as buttons.
 *
 * @author CKeil
 *
 */
public class LabelSettingsController {

	private final TreeViewFrame tvFrame;
	private final LabelSettingsDialog preferences;
	private final DataModel model;
	private SwingWorker<Void, Integer> labelWorker;
	private LabelLoadDialog dialog;
	private File customFile;

	public LabelSettingsController(final TreeViewFrame tvFrame,
			final DataModel model, final LabelSettingsDialog preferences) {

		this.tvFrame = tvFrame;
		this.model = model;
		this.preferences = preferences;

		addListeners();
	}

	/**
	 * Adds all necessary listeners to the preferences instance.
	 */
	public void addListeners() {

		preferences.addSaveAndCloseListener(new SaveAndCloseListener());
		preferences.addOKButtonListener(new ConfirmationListener());
		preferences.addResizeDialogListener(new PreferencesComponentListener());
		preferences.addJustifyListener(new LabelJustifyListener());
		preferences.addShowListener(new LabelShowListener());
		preferences.addFlankSizeListener(new SpinnerListener(preferences));
	}

	class MenuPanelListener implements MouseListener {

		@Override
		public void mouseClicked(final MouseEvent arg0) {

			checkForColorSave();
		}

		@Override
		public void mouseEntered(final MouseEvent arg0) {
		}

		@Override
		public void mouseExited(final MouseEvent arg0) {
		}

		@Override
		public void mousePressed(final MouseEvent arg0) {
		}

		@Override
		public void mouseReleased(final MouseEvent arg0) {
		}
	}

	/**
	 * Listener for 'use custom labels' button.
	 *
	 * @author CKeil
	 *
	 */
	class CustomLabelListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			if (tvFrame.isLoaded()) {
				try {
					customFile = tvFrame.selectFile();

				} catch (final LoadException e1) {
					e1.printStackTrace();
				}

				if (customFile != null) {
					loadNewLabels(StringRes.main_rows);
					loadNewLabels(StringRes.main_cols);
				}
			} else {
				LogBuffer.println("Model not loaded in tvFrame.");
			}
		}
	}

	public void loadNewLabels(final String type) {

		labelWorker = new LabelWorker(type);

		dialog = new LabelLoadDialog(type);

		// A property listener used to update the progress bar
		final PropertyChangeListener listener = new PropertyChangeListener() {

			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				if ("progress".equals(event.getPropertyName())) {
					dialog.updateProgress(((Integer) event.getNewValue()));
				}
			}
		};
		labelWorker.addPropertyChangeListener(listener);

		labelWorker.execute();

		// After executing SwingWorker to prevent the dialog
		// from blocking the background task.
		dialog.setVisible(true);
	}

	/**
	 * Listener for the "Ok" button in the preferences frame.
	 */
	class ConfirmationListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			checkForColorSave();
			preferences.getPreferencesFrame().dispose();
			preferences.synchronizeAnnotation();
		}
	}

	/**
	 * WindowAdapter for the Preferences menu.
	 */
	class SaveAndCloseListener extends WindowAdapter {

		@Override
		public void windowClosing(final WindowEvent we) {

			checkForColorSave();
			preferences.getPreferencesFrame().dispose();
		}
	}

	/**
	 * Listens to changing radio buttons in the AnnotationSettings and sets the
	 * justify-flag in TextView and ArrayNameView respectively.
	 */
	class LabelJustifyListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			final boolean[] labelAligns =
				tvFrame.getDendroView().getLabelAligns();

			boolean isRowRight = labelAligns[0];
			boolean isColRight = labelAligns[1];

			/* counter to recognize the selected JRadioButton */
			switch (((JRadioButton) e.getSource()).getText()) {
			case "Left":
				isRowRight = false;
				break;
			case "Right":
				isRowRight = true;
				break;
			case "Bottom":
				isColRight = false;
				break;
			case "Top":
				isColRight = true;
				break;
			default:
				break;
			}

			tvFrame.getDendroView().setLabelAlignment(isRowRight, isColRight);
		}
	}

	/**
	 * Listens to changing radio buttons in the AnnotationSettings and sets the
	 * various label port settings.
	 *
	 * @author chris0689
	 *
	 */
	class LabelShowListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			/* counter to recognize the selected JRadioButton */
			switch (((JRadioButton) e.getSource()).getText()) {
				case "As many as possible":
					LogBuffer.println("Setting as many as possible.");
					tvFrame.getDendroView().getRowLabelView()
						.setLabelPortMode(true);
					tvFrame.getDendroView().getRowLabelView()
						.setLabelPortFlankMode(false);
					tvFrame.getDendroView().getColLabelView()
						.setLabelPortMode(true);
					tvFrame.getDendroView().getColLabelView()
						.setLabelPortFlankMode(false);
					break;
				case "Hovered label and ":
					LogBuffer.println("Setting some.");
					tvFrame.getDendroView().getRowLabelView()
						.setLabelPortMode(true);
					tvFrame.getDendroView().getRowLabelView()
						.setLabelPortFlankMode(true);
					tvFrame.getDendroView().getColLabelView()
						.setLabelPortMode(true);
					tvFrame.getDendroView().getColLabelView()
						.setLabelPortFlankMode(true);
					//New behavior for spacebar toggling
					preferences.setShowBaseIsNone(false);
					break;
				case "None":
					LogBuffer.println("Setting none.");
					tvFrame.getDendroView().getRowLabelView()
						.setLabelPortMode(false);
					tvFrame.getDendroView().getColLabelView()
						.setLabelPortMode(false);
					//New behavior for spacebar toggling
					preferences.setShowBaseIsNone(true);
					break;
				default:
					break;
			}
		}
	}

	class PreferencesComponentListener implements ComponentListener {

		@Override
		public void componentHidden(final ComponentEvent arg0) {
		}

		@Override
		public void componentMoved(final ComponentEvent arg0) {
		}

		@Override
		public void componentResized(final ComponentEvent arg0) {

			preferences.getPreferencesFrame().getContentPane().repaint();
		}

		@Override
		public void componentShown(final ComponentEvent arg0) {
		}

	}

	/**
	 * Saves color presets if the currently shown menu is Color Settings and the
	 * 'Custom' JRadioButton is selected.
	 */
	public void checkForColorSave() {

		// if (preferences.getActiveMenu().equalsIgnoreCase(
		// StringRes.menu_title_Color)
		// && preferences.getGradientPick().isCustomSelected()) {
		// preferences.getGradientPick().saveStatus();
		// }
	}

	/**
	 * Sets up a SwingWorker to run a background thread while loading the custom
	 * labels.
	 */
	class LabelWorker extends SwingWorker<Void, Integer> {

		private final String type;

		public LabelWorker(final String type) {

			this.type = type;
		}

		@Override
		protected Void doInBackground() throws Exception {

			LabelInfo labelInfo;
			if (type.equalsIgnoreCase(StringRes.main_rows)) {
				labelInfo = model.getRowLabelInfo();

			} else if (type.equalsIgnoreCase(StringRes.main_cols)) {
				labelInfo = model.getColLabelInfo();
				
			} else {
				LogBuffer.println("Could not set LabelInfo"
						+ " when trying to load new labels.");
				return null;
			}

			/*
			 * Get number of rows without GID row. Done here to avoid passing
			 * model.
			 */
			int rowNum = model.getRowLabelInfo().getNumLabelTypes();

			if (model.gidFound()) {
				rowNum--;
			}

			// Load new labels
			final CustomLabelLoader clLoader = new CustomLabelLoader(
					labelInfo, preferences.getSelectedLabelIndexes());

			clLoader.load(customFile, rowNum);

			final int labelTypeNum = clLoader.checkForLabelTypes(model);

			// Change labelArrays (without matching actual names first)
			final String[][] oldLabels = labelInfo.getLabelArray();
			final String[] oldLabelTypes = labelInfo.getLabelTypes();

			final String[][] labelsToAdd = new String[oldLabels.length
					+ labelTypeNum][];

			// Iterate over loadedLabels
			for (int i = 0; i < oldLabels.length; i++) {
				labelsToAdd[i] = clLoader.replaceLabel(oldLabels[i], 
													   oldLabelTypes);
				setProgress((i + 1) * 100 / oldLabels.length);
			}

			clLoader.setLabels(model, type, labelsToAdd);

			return null;
		}

		@Override
		protected void process(final List<Integer> chunks) {

			dialog.setPBarMax(chunks.get(0));
		}

		@Override
		protected void done() {

			// Close dialog
			dialog.dispose();

			// Refresh labels
			preferences.synchronizeAnnotation();

			/* TODO commented out due to issue 354 which changed the way the
			 * layout creation for the label menu works. The entire preference
			 * menu controller needs some refactoring (mostly naming) because
			 * it is now label specific. The GUI components for LabelWorker are
			 * not active atm due to the feature being excluded until it is
			 * specifically tackled again and properly implemented.
			 */
//			preferences.setupLayout(StringRes.menu_RowAndCol);
			addListeners();
		}
	}

	/**
	 * Listens to a change in selection in the JSpinner for label neighbors.
	 */
	private class SpinnerListener implements ChangeListener {

		LabelSettingsDialog labelSettings;

		// To avoid synthetic compiler creation of a constructor
		protected SpinnerListener(){}

		public SpinnerListener(final LabelSettingsDialog labelSettings) {

			this.labelSettings = labelSettings;
		}

		@Override
		public void stateChanged(final ChangeEvent arg0) {

			labelSettings.setFlankSize(labelSettings.getNumNeighbors());
		}
	}
}
