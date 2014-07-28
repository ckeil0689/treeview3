package Controllers;

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

import javax.swing.SwingWorker;

import Utilities.StringRes;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LabelLoadDialog;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.PreferencesMenu;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.model.CustomLabelLoader;

/**
 * Controller for the PreferencesMenu class. Handles user interaction with Swing
 * components such as buttons.
 * 
 * @author CKeil
 * 
 */
public class PreferencesController {

	private final TreeViewFrame tvFrame;
	private final PreferencesMenu preferences;
	private final DataModel model;
	private SwingWorker<Void, Integer> labelWorker;
	private LabelLoadDialog dialog;
	private File customFile;

	public PreferencesController(final TreeViewFrame tvFrame, DataModel model,
			final PreferencesMenu preferences) {

		this.tvFrame = tvFrame;
		this.model = model;
		this.preferences = preferences;

		addListeners();
	}

	/**
	 * Adds all necessary listeners to the preferences instance.
	 */
	public void addListeners() {

		preferences.addWindowListener(new WindowListener());
		preferences.addOKButtonListener(new ConfirmationListener());
		preferences.addCustomLabelListener(new CustomLabelListener());
		preferences.addComponentListener(new PreferencesComponentListener());
	}

	class MenuPanelListener implements MouseListener {

		@Override
		public void mouseClicked(final MouseEvent arg0) {

			checkForColorSave();
		}

		@Override
		public void mouseEntered(final MouseEvent arg0) {}

		@Override
		public void mouseExited(final MouseEvent arg0) {}

		@Override
		public void mousePressed(final MouseEvent arg0) {}

		@Override
		public void mouseReleased(final MouseEvent arg0) {}
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

		dialog = new LabelLoadDialog(tvFrame, type);

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
	 * 
	 * @author CKeil
	 * 
	 */
	class ConfirmationListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			checkForColorSave();
			preferences.getPreferencesFrame().dispose();
		}
	}

	/**
	 * WindowAdapter for the Preferences menu.
	 * 
	 * @author CKeil
	 * 
	 */
	class WindowListener extends WindowAdapter {

		@Override
		public void windowClosing(final WindowEvent we) {

			checkForColorSave();
			preferences.getPreferencesFrame().dispose();
		}
	}

	class PreferencesComponentListener implements ComponentListener {

		@Override
		public void componentHidden(final ComponentEvent arg0) {}

		@Override
		public void componentMoved(final ComponentEvent arg0) {}

		@Override
		public void componentResized(final ComponentEvent arg0) {

			preferences.getPreferencesFrame().getContentPane().repaint();
		}

		@Override
		public void componentShown(final ComponentEvent arg0) {}

	}

	/**
	 * Saves color presets if the currently shown menu is Color Settings and the
	 * 'Custom' JRadioButton is selected.
	 */
	public void checkForColorSave() {

//		if (preferences.getActiveMenu().equalsIgnoreCase(
//				StringRes.menu_title_Color)
//				&& preferences.getGradientPick().isCustomSelected()) {
//			preferences.getGradientPick().saveStatus();
//		}
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

			HeaderInfo headerInfo = null;
			if (type.equalsIgnoreCase("Row")) {
				headerInfo = model.getGeneHeaderInfo();

			} else if (type.equalsIgnoreCase("Column")) {
				headerInfo = model.getArrayHeaderInfo();
			}

			// Load new labels
			final CustomLabelLoader clLoader = new CustomLabelLoader(tvFrame,
					headerInfo, preferences.getSelectedLabelIndexes());

			clLoader.load(customFile);

			final int headerNum = clLoader.checkForHeaders(model);

			// Change headerArrays (without matching actual names first)
			final String[][] oldHeaders = headerInfo.getHeaderArray();
			final String[] oldNames = headerInfo.getNames();

			final String[][] headersToAdd = new String[oldHeaders.length
					+ headerNum][];

			// Iterate over loadedLabels
			for (int i = 0; i < oldHeaders.length; i++) {

				headersToAdd[i] = clLoader
						.replaceLabel(oldHeaders[i], oldNames);

				setProgress((i + 1) * 100 / oldHeaders.length);
			}

			clLoader.setHeaders(model, type, headersToAdd);

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

			preferences.setupLayout(StringRes.menu_title_RowAndCol);
			addListeners();
		}
	}
}
