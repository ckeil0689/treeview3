package Controllers;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.PreferencesMenu;
import edu.stanford.genetics.treeview.TVFrameController;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.model.CustomLabelLoader;
import edu.stanford.genetics.treeview.model.TVModel;


public class PreferencesController {

	private TVFrameController controller;
	private TreeViewFrame tvFrame;
	private PreferencesMenu preferences;
	
	public PreferencesController(TreeViewFrame tvFrame, 
			PreferencesMenu preferences, TVFrameController controller) {
		
		this.controller = controller;
		this.tvFrame = tvFrame;
		this.preferences = preferences;
		
		preferences.addWindowListener(new WindowListener());
		preferences.addOKButtonListener(new ConfirmationListener());
		preferences.addThemeListener(new ThemeListener());
		preferences.addCustomLabelListener(new CustomLabelListener());
	}
	
	/**
	 * Listener for 'use custom labels' button.
	 * @author CKeil
	 *
	 */
	class CustomLabelListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			
			CustomLabelLoader clLoader = new CustomLabelLoader(tvFrame);
			String[][] loadedLabels = clLoader.load();
			clLoader.replaceLabels((TVModel)tvFrame.getDataModel(), 
					loadedLabels);
			
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
			
			updateCheck();
		}
		
		/**
		 * Switches the theme between day and night.
		 */
		public void updateCheck() {

			if (GUIParams.isDarkTheme()) {
				GUIParams.setDayLight();
				resetTheme();

			} else {
				GUIParams.setNight();
				resetTheme();
			}
		}
		
		/**
		 * Clears the TVFrame from the current view and loads the 
		 * new appropriate view with new color parameters.
		 */
		public void resetTheme() {
			
			preferences.setupLayout("Theme");
			
			if (tvFrame.getDataModel() != null 
					&& tvFrame.getRunning() != null) {
				if(tvFrame.getConfirmPanel() != null) {
					tvFrame.setView("LoadCheckView");
					controller.addViewListeners();
				}
				tvFrame.setView("DendroView");
				controller.addViewListeners();

			} else if (tvFrame.getDataModel() != null 
					&& tvFrame.getRunning() == null) {
				tvFrame.setView("LoadCheckView");
				controller.addViewListeners();

			} else {
				tvFrame.setView("WelcomeView");
				controller.addViewListeners();
			}
		}
	}
}
