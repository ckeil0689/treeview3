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
 * @author CKeil
 *
 */
public class MenubarActions {

	private TreeViewFrame tvFrame;
	private TVFrameController controller;
	
	private enum CLEARNODES {
		
		
	}
	
	public MenubarActions(TreeViewFrame tvFrame, 
			TVFrameController controller) {
		
		this.tvFrame = tvFrame;
		this.controller = controller;
	}
	
	/**
	 * Executes a certain function based on which menuItem was clicked
	 * by the user.
	 * @param name
	 */
	public void execute(String name) {
		
		if(name.equalsIgnoreCase("Open")) {
			controller.openFile();
			
		} else if(name.equalsIgnoreCase("Save")) {
			controller.doModelSave(true);
			
		} else if(name.equalsIgnoreCase("Save As")) {
			controller.saveModelAs();
			
		} else if(name.equalsIgnoreCase("Edit Recent Files")) {
			tvFrame.showRecentFileEditor();
			
		} else if(name.equalsIgnoreCase("Quit Program")) {
			try {
//				tvFrame.getApp().closeAllWindows();
				tvFrame.closeWindow();
				
			} catch (final Exception e) {
				System.out.println("While trying to exit, got error " + e);
				System.exit(1);
			}
			
		} else if(name.equalsIgnoreCase("Theme")
				|| name.equalsIgnoreCase("Font")
				|| name.equalsIgnoreCase("URL")){
			controller.openPrefMenu(name, "Aesthetics");
			
		} else if(name.equalsIgnoreCase("Row and Column Labels")
				|| name.equalsIgnoreCase("Color Settings")) {
			controller.openPrefMenu(name, "Options");
			
		} else if(name.equalsIgnoreCase(StringRes.menubar_clearPrefs)) {
			ConfirmDialog confirmClear = new ConfirmDialog(tvFrame, 
					"clear preferences");
			confirmClear.setVisible(true);
			
			if(confirmClear.getConfirmed()) {
				removeAllKeys(tvFrame.getConfigNode());
			}
				
		} else if(name.equalsIgnoreCase("Cluster")) {
			controller.setupClusterView();
			
		} else if(name.equalsIgnoreCase("Functional Enrichment")) {
			tvFrame.displayWIP();
			
		} else if(name.equalsIgnoreCase("Stats")) {
			tvFrame.openStatsView();	
			
		} else if(name.equalsIgnoreCase("Save List")) {
			controller.saveList();
			
		} else if(name.equalsIgnoreCase("Save Data")) {
			controller.saveData();
			
		} else if(name.equalsIgnoreCase("New Window")) {
			tvFrame.createNewFrame().getAppFrame().setVisible(true);
			
//		} else if(name.equalsIgnoreCase("Close Window")) {
//			tvFrame.closeWindow();
			
		} else if(name.equalsIgnoreCase("About...")) {
			tvFrame.showAboutWindow();
			
		} else if(name.equalsIgnoreCase("Documentation...")) {
			tvFrame.showDocumentation();
			
		} else if(name.equalsIgnoreCase("Show Log")) {
			tvFrame.showLogMessages();
		}
	}
	
	/**
	 * Recursive method to remove all keys from all nodes, starting from the
	 * original parent node on which this method is called.
	 * @param parentNode
	 */
	public void removeAllKeys(Preferences parentNode) {
		
		try {
			if(!tvFrame.getLoaded()) {
				parentNode.clear();
				Preferences fileNode = parentNode.node(
						StringRes.pref_node_File);
				String[] models = fileNode.childrenNames();
				
				for(int i = 0; i < models.length; i++) {
					
					fileNode.node(models[i]).removeNode();
				}
				
			} else {
//				parentNode.clear();
//				
//				String[] childrenNodes = parentNode.childrenNames();
//				
//				if(childrenNodes.length > 0)
//				for(int i = 0; i < childrenNodes.length; i++) {
//					
//					removeAllKeys(parentNode.node(childrenNodes[i]));
//				}
			}
			
		} catch (BackingStoreException e) {
			LogBuffer.println("Error when removing Preferences " +
					"keys: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
}
