/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import javax.swing.event.MenuListener;

public abstract class TreeviewMenuBarI {

	public static final String programMenu = "File";
	public static final String documentMenu = "Edit";
	public static final String viewsMenu = "Views";
	public static final String analysisMenu = "Analysis";
	public static final String exportMenu = "Export";
	public static final String windowMenu = "Window";
	public static final String helpMenu = "Help";
	public static final String mruSubMenu = "Recent Files";
	public static final String clusterSubMenu = "Cluster";
	public static final String vizSubMenu = "Visualize";
	public static final String prefSubMenu = "Preferences";

	/**
	 * this will make the newly added menu current
	 *
	 * @param name
	 *            of menu to add to menu bar
	 * @return menu added
	 */
	public abstract Object addMenu(String name);

	/**
	 * add sub menu to current menu and make it the current menu
	 *
	 * @param name
	 *            of new menu
	 * @return sub menu added
	 */
	public abstract Object addSubMenu(String name);

	/**
	 * add separator to current menu
	 */
	public abstract void addSeparator();

	/**
	 * remove all items from current menu.
	 */
	public abstract void removeAll();

	public abstract void removeMenuItems();

	/**
	 * get item count of current menu.
	 */
	public abstract int getItemCount();

	/**
	 * set enabled status of current menu.
	 */
	public abstract void setEnabled(boolean value);

	/**
	 * add item to current menu, and make current
	 */
	// public abstract Object addMenuItem(String name, ActionListener l);
	//
	// public abstract Object addMenuItem(String name, ActionListener l, int
	// pos);
	public abstract Object addMenuItem(String name);

	public abstract Object addMenuItem(String name, int pos);

	public abstract void addMenuListener(MenuListener listener);

	/**
	 * this adds a shortcut, i.e. Ctrl-key or Cmd-key on mac
	 *
	 * @param key
	 */
	public abstract void setAccelerator(int key);

	/**
	 * this adds a menu item on windows and mac.
	 *
	 * @param key
	 */
	public abstract void setMnemonic(int key);

	public abstract Object setMenu(String name);

	public abstract Object setSubMenu(String name);

	public void setMenuMnemonic(final int vkS) {
		// TODO Auto-generated method stub
	}

}