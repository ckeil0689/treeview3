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

import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LabelLoadDialog;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.MenuPanel;
import edu.stanford.genetics.treeview.PreferencesMenu;
import edu.stanford.genetics.treeview.StringRes;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.model.CustomLabelLoader;
import edu.stanford.genetics.treeview.model.TVModel;

/**
 * Controller for the PreferencesMenu class. Handles user interaction with
 * Swing components such as buttons.
 * @author CKeil
 *
 */
public class PreferencesController {

	private TVFrameController controller;
	private TreeViewFrame tvFrame;
	private PreferencesMenu preferences;
	private SwingWorker<Void, Integer> labelWorker;
	private LabelLoadDialog dialog;
	private File customFile;
	
	public PreferencesController(TreeViewFrame tvFrame, 
			PreferencesMenu preferences, TVFrameController controller) {
		
		this.controller = controller;
		this.tvFrame = tvFrame;
		this.preferences = preferences;
		
		addListeners();
	}
	
	/**
	 * Adds all necessary listeners to the preferences instance.
	 */
	public void addListeners() {
		
		preferences.addWindowListener(new WindowListener());
		preferences.addOKButtonListener(new ConfirmationListener());
		preferences.addThemeListener(new ThemeListener());
		preferences.addCustomLabelListener(new CustomLabelListener());
		preferences.addMenuListeners(new MenuPanelListener());
		preferences.addComponentListener(new PreferencesComponentListener());
	}
	
	class MenuPanelListener implements MouseListener {
		
		@Override
		public void mouseClicked(MouseEvent arg0) {
			
			checkForColorSave();
			
			for(MenuPanel panel : preferences.getMenuPanelList()) {
				
				if(arg0.getSource().equals(panel.getMenuPanel())) {
					preferences.addMenu(panel.getLabelText());
					panel.setSelected(true);
					
				} else {
					panel.setSelected(false);
				}
			}
		}

		@Override
		public void mouseEntered(MouseEvent arg0) {
			
			for(MenuPanel panel : preferences.getMenuPanelList()) {
				
				if(arg0.getSource().equals(panel.getMenuPanel())) {
					panel.setHover(true);
				}
			}
		}

		@Override
		public void mouseExited(MouseEvent arg0) {
			
			for(MenuPanel panel : preferences.getMenuPanelList()) {
				
				if(arg0.getSource().equals(panel.getMenuPanel())) {
					panel.setHover(false);
				} 
			}
		}

		@Override
		public void mousePressed(MouseEvent arg0) {}

		@Override
		public void mouseReleased(MouseEvent arg0) {}
	}
	/**
	 * Listener for 'use custom labels' button.
	 * @author CKeil
	 *
	 */
	class CustomLabelListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			
			if(tvFrame.getLoaded() && (preferences.getAnnotationChoices()[0] 
					|| preferences.getAnnotationChoices()[1])){
				
				try {
					customFile = tvFrame.selectFile();
					
				} catch (LoadException e1) {
					e1.printStackTrace();
				}
				
				if(customFile != null) {
					
					if(preferences.getAnnotationChoices()[0]) {
						labelWorker = new LabelWorker("Row");
					} 
					
					dialog = new LabelLoadDialog(tvFrame);
					
					// A property listener used to update the progress bar
				    PropertyChangeListener listener = 
				    		new PropertyChangeListener(){
				    	
				      public void propertyChange(PropertyChangeEvent event) {
				        
				    	  if ("progress".equals(event.getPropertyName())) {
				    		  dialog.updateProgress(
				    				  ((Integer)event.getNewValue()));
				    	  }
				      }
				    };
				    labelWorker.addPropertyChangeListener(listener);
				    
					labelWorker.execute();
					
					// After executing SwingWorker to prevent the dialog
					// from blocking the background task.
					dialog.setVisible(true);
				}
			} else {
				LogBuffer.println("Nothing selected.");
			}
		}
	}
	
	/**
	 * Listener for the "Ok" button in the preferences frame.
	 * @author CKeil
	 *
	 */
	class ConfirmationListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
		
			checkForColorSave();
			preferences.getPreferencesFrame().dispose();
		}
		
	}
	
	/**
	 * WindowAdapter for the Preferences menu.
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
	
	/**
	 * Listener for the theme switch button.
	 * @author CKeil
	 *
	 */
	class ThemeListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			
			new ThemeResetter(e).run();
		}
	}
	
	class ThemeResetter extends SwingWorker<Void, Void> {

		private ActionEvent event;
		
		ThemeResetter(ActionEvent e) {
			
			this.event = e;
		}
		
		@Override
		protected Void doInBackground() throws Exception {
			
			boolean light = false;
			
			if(event.getSource().equals(preferences.getLightButton())) {
				light = true;	
			}
			
			updateCheck(light);
			return null;
		}
		
		/**
		 * Switches the theme between day and night.
		 */
		public void updateCheck(boolean light) {

			if (light) {
				GUIParams.setDayLight();
				tvFrame.getConfigNode().put("theme", StringRes.rButton_light);
				resetTheme();

			} else {
				GUIParams.setNight();
				tvFrame.getConfigNode().put("theme", StringRes.rButton_dark);
				resetTheme();
			}
		}
		
		/**
		 * Clears the TVFrame from the current view and loads the 
		 * new appropriate view with new color parameters.
		 */
		public void resetTheme() {
			
			preferences.setupLayout(StringRes.menu_title_Theme);
			addListeners();
			
			if (tvFrame.getDataModel() != null 
					&& tvFrame.getRunning() != null) {
				tvFrame.setView(StringRes.view_Dendro);
				controller.addViewListeners();

			} else {
				tvFrame.setView(StringRes.view_Welcome);
				controller.addViewListeners();
			}
		}
	}
	
	class PreferencesComponentListener implements ComponentListener {

		@Override
		public void componentHidden(ComponentEvent arg0) {}

		@Override
		public void componentMoved(ComponentEvent arg0) {}

		@Override
		public void componentResized(ComponentEvent arg0) {
			
			preferences.getPreferencesFrame().getContentPane().repaint();
		}

		@Override
		public void componentShown(ComponentEvent arg0) {}
		
		
	}
	
	/**
	 * Saves color presets if the currently shown menu is Color Settings
	 * and the 'Custom' JRadioButton is selected.
	 */
	public void checkForColorSave() {
		
		if(preferences.getActiveMenu().equalsIgnoreCase(
				StringRes.menu_title_Color) 
				&& preferences.getGradientPick().isCustomSelected()) {
			preferences.getGradientPick().saveStatus();
		}
	}
	
	/**
	 * Sets up a SwingWorker to run a background thread while loading the 
	 * custom labels.
	 */
	class LabelWorker extends SwingWorker<Void, Integer> {

		private String type;
		
		public LabelWorker(String type) {
			
			this.type = type;
		}
		
		@Override
		protected Void doInBackground() throws Exception {
			
			TVModel model = (TVModel)tvFrame.getDataModel();
			
			HeaderInfo headerInfo = null;
			if(type.equalsIgnoreCase("Row")) {
				headerInfo = model.getGeneHeaderInfo(); 
				
			} else if(type.equalsIgnoreCase("Column")){
				headerInfo = model.getArrayHeaderInfo();
			}
			
			// Load new labels
			CustomLabelLoader clLoader = new CustomLabelLoader(tvFrame, 
					headerInfo, preferences.getSelectedLabelIndexes());
			
			clLoader.load(customFile);
			
			int headerNum = clLoader.checkForHeaders(model);
			
			// Change headerArrays (without matching actual names first)
			String[][] oldHeaders = headerInfo.getHeaderArray();
			String[] oldNames = headerInfo.getNames();
			
			String[][] headersToAdd = 
					new String[oldHeaders.length + headerNum][];
			
			// Iterate over loadedLabels
			for(int i = 0; i < oldHeaders.length; i++) {
				
				headersToAdd[i] = clLoader.replaceLabel(oldHeaders[i], 
						oldNames);
				
				setProgress((i + 1) * 100 /oldHeaders.length);
			}
			
			clLoader.setHeaders(model, type, headersToAdd);
			
			return null;
		}
		
		@Override
		protected void process(List<Integer> chunks) {
			
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
