package Controllers;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.ConfirmDialog;
import edu.stanford.genetics.treeview.LogBuffer;
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

	private enum CLEARNODES {

	}

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

		} else if (name.equalsIgnoreCase(StringRes.menu_title_QuitWindow)) {
			try {
				// tvFrame.getApp().closeAllWindows();
				tvFrame.closeWindow();

			} catch (final Exception e) {
				System.out.println("While trying to exit, got error " + e);
				System.exit(1);
			}

		} else if (name.equalsIgnoreCase(StringRes.menu_title_Theme)
				|| name.equalsIgnoreCase(StringRes.menu_title_Font)
				|| name.equalsIgnoreCase(StringRes.menu_title_URL)) {
			controller.openPrefMenu(name, "Aesthetics");

		} else if (name.equalsIgnoreCase(StringRes.menu_title_RowAndCol)
				|| name.equalsIgnoreCase(StringRes.menu_title_Color)) {
			controller.openPrefMenu(name, "Options");

		} else if (name.equalsIgnoreCase(StringRes.menubar_clearPrefs)) {
			final ConfirmDialog confirmClear = new ConfirmDialog(tvFrame,
					"clear preferences");
			confirmClear.setVisible(true);

			if (confirmClear.getConfirmed()) {
				removeAllKeys(tvFrame.getConfigNode());
			}

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

	/**
	 * Recursive method to remove all keys from all nodes, starting from the
	 * original parent node on which this method is called.
	 * 
	 * @param parentNode
	 */
	public void removeAllKeys(final Preferences parentNode) {

		try {
			if (!tvFrame.getLoaded()) {
				parentNode.clear();
				final Preferences fileNode = parentNode
						.node(StringRes.pref_node_File);
				final String[] models = fileNode.childrenNames();

				for (int i = 0; i < models.length; i++) {

					fileNode.node(models[i]).removeNode();
				}

			} else {
				// parentNode.clear();
				//
				// String[] childrenNodes = parentNode.childrenNames();
				//
				// if(childrenNodes.length > 0)
				// for(int i = 0; i < childrenNodes.length; i++) {
				//
				// removeAllKeys(parentNode.node(childrenNodes[i]));
				// }
			}

		} catch (final BackingStoreException e) {
			LogBuffer.println("Error when removing Preferences " + "keys: "
					+ e.getMessage());
			e.printStackTrace();
		}

	}
}
