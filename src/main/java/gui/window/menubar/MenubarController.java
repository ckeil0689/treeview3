package gui.window.menubar;

import gui.cluster.ClusterView;
import gui.window.TVController;
import gui.window.TreeViewFrame;
import util.LogBuffer;
import util.StringRes;

import javax.swing.*;
import java.awt.*;

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
	 * A controller to handle user interaction with JMenuItems. It defines what
	 * methods are executed when the JMenuItems are clicked.
	 *
	 * @param tvFrame
	 *            The main JFrame of the application.
	 * @param controller
	 *            The controller for TVFrame.
	 */
	public MenubarController(final TreeViewFrame tvFrame,
			final TVController controller) {

		this.tvFrame = tvFrame;
		this.controller = controller;
	}

	/**
	 * TODO Enumerate menu items (own enum class and remove strings)
	 * Executes a certain function based on which menuItem was clicked by the
	 * user. This is determined by using the name string of the MenuItem, stored
	 * in StringRes.
	 *
	 * @param name
	 *            The clicked menu's name.
	 */
	public void execute(final String name) {

		switch (name) {

		case StringRes.menu_Open:
			controller.openFile(null,false,false);
			break;
		case StringRes.menu_Import:
			controller.openFile(null,true,false);
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
		case StringRes.menu_QuitWindow:
			controller.closeWindow();
			break;
		case StringRes.menu_Export:
			try {
				controller.openExportMenu();
			} catch(Exception oome) {
				LogBuffer.println("Out of memory during " +
					"controller.openExportMenu().");
				oome.printStackTrace();
			}
			break;
		case StringRes.menu_RowAndCol:
			controller.openLabelMenu(name);
			break;
		case StringRes.menu_Color:
			controller.openColorMenu();
			break;
		case StringRes.menu_URL:
			controller.openLabelMenu(name);
			break;
		case StringRes.menu_showTrees:
		case StringRes.menu_hideTrees:
			controller.toggleTrees();
			break;
		case StringRes.menu_Hier:
			controller.setupClusterView(ClusterView.HIER);
			break;
		// case StringRes.menu_KMeans:
		// controller.setupClusterView(ClusterView.KMEANS);
		// break;
		case "Find Labels...":
			// controller.setSearchVisible();
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
			TreeViewFrame.showAboutWindow();
			break;
		case StringRes.menu_Shortcuts:
			TreeViewFrame.showShortcuts();
			break;
		// There's currently no documentation page - don't need it for the first
		// release
		// case StringRes.menu_Docs:
		// tvFrame.showDocumentation();
		// break;
		case StringRes.menu_ShowLog:
			tvFrame.showLogMessages();
			break;
		default:
			displayError();
			break;
		}
	}

	/**
	 * Just opens a stats dialog.
	 */
	private void openStats() {

		final String source = controller.getDataModel().getSource();
		final int rowNum = controller.getDataModel().getRowLabelInfo()
				.getNumLabels();
		final int colNum = controller.getDataModel().getColLabelInfo()
				.getNumLabels();

		TreeViewFrame.openStatsView(source, rowNum, colNum);
	}

	/**
	 * Displays an error message. This should happened when the switch statement
	 * defaults due to not finding a matching function for the supplied MenuItem
	 * name.
	 */
	private static void displayError() {

		final String message = "A menu button could not be matched with a function.";
		JOptionPane.showMessageDialog(Frame.getFrames()[0], message, "Alert",
				JOptionPane.WARNING_MESSAGE);
		LogBuffer.println("Alert: " + message);
	}
}
