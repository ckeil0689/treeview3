package Controllers;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import Utilities.StringRes;
import Views.ClusterView;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.plugin.dendroview.GlobalView;

/**
 * This class contains all the different actions which are being mapped to
 * JMenuItems using ActionListeners.
 * 
 * @author CKeil
 * 
 */
public class MenubarController {

	/* References to main view and main controller */
	private final TreeViewFrame tvFrame;
	private final TVController controller;

	/**
	 * A controller to handle user interaction with JMenuItems. It defines
	 * what methods are executed when the JMenuItems are clicked.
	 * @param tvFrame The main JFrame of the application.
	 * @param controller The controller for TVFrame.
	 */
	public MenubarController(final TreeViewFrame tvFrame,
			final TVController controller) {

		this.tvFrame = tvFrame;
		this.controller = controller;
	}

	/**
	 * Executes a certain function based on which menuItem was clicked by the
	 * user. This is determined by using the name string of the MenuItem, 
	 * stored in StringRes.
	 * 
	 * @param name The clicked menu's name.
	 */
	public void execute(final String name) {
		
		switch (name) {
		
		case StringRes.menu_Open: 		
			controller.openFile();
			break;
		case StringRes.menu_Save: 		
			controller.doModelSave(true);
			break;
		case StringRes.menu_SaveAs: 	
			controller.saveModelAs();
			break;
		case StringRes.menu_EditRecent: 
			tvFrame.showRecentFileEditor();
			break;
		case StringRes.menu_ResetPrefs:	
			controller.resetPreferences();
			break;
		case "Isolate Selected":		
			showSubData();
			break;
		case StringRes.menu_QuitWindow: 
			tvFrame.closeWindow();
			break;
		case StringRes.menu_RowAndCol:	
			controller.openPrefMenu(name);
			break;
		case StringRes.menu_Color:		
			controller.openPrefMenu(name);
			break;
		case StringRes.menu_Font:		
			controller.openPrefMenu(name);
			break;
		case StringRes.menu_URL:		
			controller.openPrefMenu(name);
			break;	
		case "Fill screen":				
			controller.setMatrixSize(GlobalView.FILL);
			break;
		case "Equal axes":				
			controller.setMatrixSize(GlobalView.EQUAL);
			break;
		case "Proportional axes":	    
			controller.setMatrixSize(GlobalView.PROPORT);
			break;
		case "Show trees...":	
		case "Hide trees...":
			controller.toggleTrees();
			break;
		case StringRes.menu_Hier:		
			controller.setupClusterView(ClusterView.HIER);
			break;
//		case StringRes.menu_KMeans:    	
//			controller.setupClusterView(ClusterView.KMEANS);
//			break;
		case "Find Labels...":		
			controller.setSearchVisible();
			break;
		case "Functional Enrichment":	
			tvFrame.displayWIP();
			break;
		case StringRes.menu_Stats:		
			openStats();
			break;
		case "Save List":				
			controller.saveList();
			break;
		case "Save Data":				
			controller.saveData();
			break;
		case StringRes.menu_NewWindow:	
			tvFrame.createNewFrame().getAppFrame().setVisible(true);
			break;
		case StringRes.menu_About:		
			tvFrame.showAboutWindow();
			break;
		case StringRes.menu_Shortcuts:		
			tvFrame.showShortcuts();
			break;
//There's currently no documentation page - don't need it for the first release
//		case StringRes.menu_Docs:		
//			tvFrame.showDocumentation();
//			break;
		case StringRes.menu_ShowLog:	
			tvFrame.showLogMessages();
			break;
		default: 						
			displayError();
			break;
		}
	}
	
	/* Some helpers to keep the switch statement readable. */
	/**
	 * Takes the currently selected row and column indexes and uses them
	 * to open a new view that only displays the selected data. 
	 */
	private void showSubData() {
		
		controller.showSubDataModel(
				tvFrame.getRowSelection().getSelectedIndexes(), 
				tvFrame.getColumnSelection().getSelectedIndexes(), 
				null, null);
	}
	
	/**
	 * Just opens a stats dialog.
	 */
	private void openStats() {
		
		String source = controller.getDataModel().getSource();
		int rowNum = controller.getDataModel().getRowHeaderInfo()
				.getNumHeaders();
		int colNum = controller.getDataModel().getColumnHeaderInfo()
				.getNumHeaders();
		
		tvFrame.openStatsView(source, rowNum, colNum);
	}
	
	/**
	 * Displays an error message. This should happened when the switch statement
	 * defaults due to not finding a matching function for the supplied MenuItem
	 * name.
	 */
	private void displayError() {
		
		String message = "A menu button could not be matched with a function.";
		JOptionPane.showMessageDialog(JFrame.getFrames()[0], message, 
				"Alert", JOptionPane.WARNING_MESSAGE);
		LogBuffer.println("Alert: " + message);
	}
}
