package Controllers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

import javax.swing.SwingWorker;

import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.LabelLoadDialog;
import edu.stanford.genetics.treeview.MenuPanel;
import edu.stanford.genetics.treeview.PreferencesMenu;
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
	private SwingWorker<Void, Void> labelWorker;
//	private Preferences configNode;
	
	public PreferencesController(TreeViewFrame tvFrame, 
			PreferencesMenu preferences, TVFrameController controller) {
		
		this.controller = controller;
		this.tvFrame = tvFrame;
		this.preferences = preferences;
//		this.configNode = preferences.getConfigNode();
		
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
			
			setupLabelWorker();
			
			if(tvFrame.getLoaded()){
				labelWorker.execute();
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
			
			if(preferences.getGradientPick() != null) {
				preferences.getGradientPick().saveStatus();
			}
			
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
			
			if(preferences.getGradientPick() != null) {
				preferences.getGradientPick().saveStatus();
			}
			
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
//			boolean light = false;
//			
//			if(e.getSource().equals(preferences.getLightButton())) {
//				preferences.getLightButton().setEnabled(false);
//				preferences.getDarkButton().setEnabled(true);
//				light = true;
//				
//			} else {
//				preferences.getLightButton().setEnabled(true);
//				preferences.getDarkButton().setEnabled(false);
//			}
//			
//			updateCheck(light);
		}
		
//		/**
//		 * Switches the theme between day and night.
//		 */
//		public void updateCheck(boolean light) {
//
//			if (light) {
//				GUIParams.setDayLight();
//				resetTheme();
//
//			} else {
//				GUIParams.setNight();
//				resetTheme();
//			}
//		}
//		
//		/**
//		 * Clears the TVFrame from the current view and loads the 
//		 * new appropriate view with new color parameters.
//		 */
//		public void resetTheme() {
//			
//			preferences.setupLayout("Theme");
//			addListeners();
//			
//			if (tvFrame.getDataModel() != null 
//					&& tvFrame.getRunning() != null) {
//				tvFrame.setView("DendroView");
//				controller.addViewListeners();
//
//			} else {
//				tvFrame.setView("WelcomeView");
//				controller.addViewListeners();
//			}
//		}
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
				resetTheme();
				tvFrame.getConfigNode().put("theme", "light");

			} else {
				GUIParams.setNight();
				resetTheme();
				tvFrame.getConfigNode().put("theme", "dark");
			}
		}
		
		/**
		 * Clears the TVFrame from the current view and loads the 
		 * new appropriate view with new color parameters.
		 */
		public void resetTheme() {
			
			preferences.setupLayout("Theme");
			addListeners();
			
			if (tvFrame.getDataModel() != null 
					&& tvFrame.getRunning() != null) {
				tvFrame.setView("DendroView");
				controller.addViewListeners();

			} else {
				tvFrame.setView("WelcomeView");
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
	 * Sets up a SwingWorker to run a background thread while loading the 
	 * custom labels.
	 */
	public void setupLabelWorker() {
		
		final LabelLoadDialog dialog = new LabelLoadDialog(tvFrame);
		
		labelWorker = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				
				// Load new labels
				CustomLabelLoader clLoader = new CustomLabelLoader(tvFrame);
				String[][] loadedLabels = clLoader.load();
				
				// Open small dialog
				dialog.setVisible(true);
				
				clLoader.addNewLabels((TVModel)tvFrame.getDataModel(), 
						loadedLabels);
				return null;
			}
			
			@Override
			protected void done() {
				
				// Refresh labels
				preferences.synchronizeAnnotation();
				
				// Close dialog
				dialog.dispose();
			}
		};
	}
}
