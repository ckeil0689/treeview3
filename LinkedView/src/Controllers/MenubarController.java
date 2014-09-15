package Controllers;

import Utilities.ErrorDialog;
import Utilities.StringRes;
import edu.stanford.genetics.treeview.TreeViewFrame;

/**
 * This class contains all the different actions which are being mapped to
 * JMenuItems using ActionListener.
 * 
 * @author CKeil
 * 
 */
public class MenubarController {

	private final TreeViewFrame tvFrame;
	private final TVFrameController controller;

	public MenubarController(final TreeViewFrame tvFrame,
			final TVFrameController controller) {

		this.tvFrame = tvFrame;
		this.controller = controller;
	}

	// TODO implement switch statement instead of elseifs.
	/**
	 * Executes a certain function based on which menuItem was clicked by the
	 * user. This is determined by using the name string of the MenuItem, 
	 * stored in StringRes.
	 * 
	 * @param name
	 */
	public void execute(final String name) {

//		if (name.equalsIgnoreCase(StringRes.menu_title_Open)) {
//			controller.openFile();
//
//		} else if (name.equalsIgnoreCase(StringRes.menu_title_Save)) {
//			controller.doModelSave(true);
//
//		} else if (name.equalsIgnoreCase(StringRes.menu_title_SaveAs)) {
//			controller.saveModelAs();
//
//		} else if (name.equalsIgnoreCase(StringRes.menu_title_EditRecent)) {
//			tvFrame.showRecentFileEditor();
//			
//		} else if (name.equalsIgnoreCase(StringRes.menu_title_ResetPrefs)) {
//			controller.resetPreferences();
//			
//		} else if (name.equalsIgnoreCase("Isolate Selected")) {
//			controller.showSubDataModel(
//					tvFrame.getGeneSelection().getSelectedIndexes(), 
//					tvFrame.getArraySelection().getSelectedIndexes(), 
//					null, null);
//
//		} else if (name.equalsIgnoreCase(StringRes.menu_title_QuitWindow)) {
//			try {
//				// tvFrame.getApp().closeAllWindows();
//				tvFrame.closeWindow();
//
//			} catch (final Exception e) {
//				System.out.println("While trying to exit, got error " + e);
//				System.exit(1);
//			}
//
//		} else if (name.equalsIgnoreCase(StringRes.menu_title_RowAndCol)
//				|| name.equalsIgnoreCase(StringRes.menu_title_Color)
//				|| name.equalsIgnoreCase(StringRes.menu_title_Font)
//				|| name.equalsIgnoreCase(StringRes.menu_title_URL)) {
//			controller.openPrefMenu(name);
//			
//		} else if(name.equalsIgnoreCase("Fill screen")) {
//			controller.setMatrixSize("fill");
//			
//		} else if(name.equalsIgnoreCase("Equal axes")) {
//			controller.setMatrixSize("equal");
//			
//		} else if(name.equalsIgnoreCase("Proportional axes")) {
//			controller.setMatrixSize("proportional");
//			
//		} else if (name.equalsIgnoreCase(StringRes.menu_title_Hier)) {
//			controller.setupClusterView(StringRes.menu_title_Hier);
//
//		} else if (name.equalsIgnoreCase(StringRes.menu_title_KMeans)) {
//			controller.setupClusterView(StringRes.menu_title_KMeans);
//
//		} else if (name.equalsIgnoreCase("Functional Enrichment")) {
//			tvFrame.displayWIP();
//
//		} else if (name.equalsIgnoreCase(StringRes.menu_title_Stats)) {
//			String source = controller.getDataModel().getSource();
//			int rowNum = controller.getDataModel().getGeneHeaderInfo()
//					.getNumHeaders();
//			int colNum = controller.getDataModel().getArrayHeaderInfo()
//					.getNumHeaders();
//			
//			tvFrame.openStatsView(source, rowNum, colNum);
//
//		} else if (name.equalsIgnoreCase("Save List")) {
//			controller.saveList();
//
//		} else if (name.equalsIgnoreCase("Save Data")) {
//			controller.saveData();
//
//		} else if (name.equalsIgnoreCase(StringRes.menu_title_NewWindow)) {
//			tvFrame.createNewFrame().getAppFrame().setVisible(true);
//
//			// } else if(name.equalsIgnoreCase("Close Window")) {
//			// tvFrame.closeWindow();
//
//		} else if (name.equalsIgnoreCase(StringRes.menu_title_About)) {
//			tvFrame.showAboutWindow();
//
//		} else if (name.equalsIgnoreCase(StringRes.menu_title_Docs)) {
//			tvFrame.showDocumentation();
//
//		} else if (name.equalsIgnoreCase(StringRes.menu_title_ShowLog)) {
//			tvFrame.showLogMessages();
//		}
		
		// Switch implementation
		switch (name) {
			case StringRes.menu_Open: 		controller.openFile();
											break;
			case StringRes.menu_Save: 		controller.doModelSave(true);
											break;
			case StringRes.menu_SaveAs: 	controller.saveModelAs();
											break;
			case StringRes.menu_EditRecent: tvFrame.showRecentFileEditor();
											break;
			case StringRes.menu_ResetPrefs:	controller.resetPreferences();
											break;
			case "Isolate Selected":		showSubData();
											break;
			case StringRes.menu_QuitWindow: tvFrame.closeWindow();
											break;
			case StringRes.menu_RowAndCol:	controller.openPrefMenu(name);
											break;
			case StringRes.menu_Color:		controller.openPrefMenu(name);
											break;
			case StringRes.menu_Font:		controller.openPrefMenu(name);
											break;
			case StringRes.menu_URL:		controller.openPrefMenu(name);
											break;	
			case "Fill screen":				controller.setMatrixSize("fill");
											break;
			case "Equal axes":				controller.setMatrixSize("equal");
											break;
			case "Proportional axes":	    controller.setMatrixSize(
											"proportional");
											break;
			case StringRes.menu_Hier:		controller.setupClusterView(name);
											break;
			case StringRes.menu_KMeans:    	controller.setupClusterView(name);
											break;
			case "Functional Enrichment":	tvFrame.displayWIP();
											break;
			case StringRes.menu_Stats:		openStats();
											break;
			case "Save List":				controller.saveList();
											break;
			case "Save Data":				controller.saveData();
											break;
			case StringRes.menu_NewWindow:	tvFrame.createNewFrame()
											.getAppFrame().setVisible(true);
											break;
			case StringRes.menu_About:		tvFrame.showAboutWindow();
											break;
			case StringRes.menu_Docs:		tvFrame.showDocumentation();
											break;
			case StringRes.menu_ShowLog:	tvFrame.showLogMessages();
											break;
			default: 						displayError();
											break;
		}
	}
	
	// Some helpers to keep the switch statement readable.
	/**
	 * Takes the currently selected row and column indexes and uses them
	 * to open a new view that only displays the selected data. 
	 */
	private void showSubData() {
		
		controller.showSubDataModel(
				tvFrame.getGeneSelection().getSelectedIndexes(), 
				tvFrame.getArraySelection().getSelectedIndexes(), 
				null, null);
	}
	
	/**
	 * Just opens a stats dialog.
	 */
	private void openStats() {
		
		String source = controller.getDataModel().getSource();
		int rowNum = controller.getDataModel().getGeneHeaderInfo()
				.getNumHeaders();
		int colNum = controller.getDataModel().getArrayHeaderInfo()
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
		ErrorDialog error = new ErrorDialog(tvFrame.getAppFrame(), message);
		error.setVisible(true);
	}
}
