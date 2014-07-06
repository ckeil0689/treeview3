package Controllers;

import edu.stanford.genetics.treeview.StringRes;
import edu.stanford.genetics.treeview.TreeViewFrame;

/**
 * This class contains all the different actions which are being mapped to
 * JMenuItems using ActionListener. GUI related actions will be called from
 * TVFrame, other stuff in the Controller.
 * 
 * @author CKeil
 * 
 */
public class MenubarActions {

	private final TreeViewFrame tvFrame;
	private final TVFrameController controller;

	public MenubarActions(final TreeViewFrame tvFrame,
			final TVFrameController controller) {

		this.tvFrame = tvFrame;
		this.controller = controller;
	}

	/**
	 * Executes a certain function based on which menuItem was clicked by the
	 * user.
	 * 
	 * @param name
	 */
	public void execute(final String name) {

		if (name.equalsIgnoreCase(StringRes.menu_title_Open)) {
			controller.openFile();

		} else if (name.equalsIgnoreCase(StringRes.menu_title_Save)) {
			controller.doModelSave(true);

		} else if (name.equalsIgnoreCase(StringRes.menu_title_SaveAs)) {
			controller.saveModelAs();

		} else if (name.equalsIgnoreCase(StringRes.menu_title_EditRecent)) {
			tvFrame.showRecentFileEditor();
			
		} else if (name.equalsIgnoreCase("Isolate Selected")) {
			controller.showSubDataModel(
					tvFrame.getGeneSelection().getSelectedIndexes(), 
					tvFrame.getArraySelection().getSelectedIndexes(), 
					null, null);

		} else if (name.equalsIgnoreCase(StringRes.menu_title_QuitWindow)) {
			try {
				// tvFrame.getApp().closeAllWindows();
				tvFrame.closeWindow();

			} catch (final Exception e) {
				System.out.println("While trying to exit, got error " + e);
				System.exit(1);
			}

		} else if (name.equalsIgnoreCase(StringRes.menu_title_RowAndCol)
				|| name.equalsIgnoreCase(StringRes.menu_title_Color)
				|| name.equalsIgnoreCase(StringRes.menu_title_Font)
				|| name.equalsIgnoreCase(StringRes.menu_title_URL)) {
			controller.openPrefMenu(name);
			
		} else if(name.equalsIgnoreCase("Fill screen")) {
			tvFrame.setMatrixSize("fill");
			
		} else if(name.equalsIgnoreCase("Equal axes")) {
			tvFrame.setMatrixSize("equal");
			
		} else if(name.equalsIgnoreCase("Proportional axes")) {
			tvFrame.setMatrixSize("proportional");
			
		} else if (name.equalsIgnoreCase(StringRes.menu_title_Hier)) {
			controller.setupClusterView(StringRes.menu_title_Hier);

		} else if (name.equalsIgnoreCase(StringRes.menu_title_KMeans)) {
			controller.setupClusterView(StringRes.menu_title_KMeans);

		} else if (name.equalsIgnoreCase("Functional Enrichment")) {
			tvFrame.displayWIP();

		} else if (name.equalsIgnoreCase(StringRes.menu_title_Stats)) {
			tvFrame.openStatsView();

		} else if (name.equalsIgnoreCase("Save List")) {
			controller.saveList();

		} else if (name.equalsIgnoreCase("Save Data")) {
			controller.saveData();

		} else if (name.equalsIgnoreCase(StringRes.menu_title_NewWindow)) {
			tvFrame.createNewFrame().getAppFrame().setVisible(true);

			// } else if(name.equalsIgnoreCase("Close Window")) {
			// tvFrame.closeWindow();

		} else if (name.equalsIgnoreCase(StringRes.menu_title_About)) {
			tvFrame.showAboutWindow();

		} else if (name.equalsIgnoreCase(StringRes.menu_title_Docs)) {
			tvFrame.showDocumentation();

		} else if (name.equalsIgnoreCase(StringRes.menu_title_ShowLog)) {
			tvFrame.showLogMessages();
		}
	}
}
