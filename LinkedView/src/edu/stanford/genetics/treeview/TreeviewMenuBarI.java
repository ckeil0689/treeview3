/*
 * Created on May 17, 2008
 *
 * The purpose of this interface is to make it easy to switch underlying menu
 * toolkits (i.e. JMenu vs Menu). To use it, the caller should 
 * 1) grab the lock using a synchronized block
 * 2) call setMenu and setSubMenu to set the menus
 * 3) pass object to a popluate method. The populate method should assume that
 *   the caller has already positioned the TreeViewMenuBar to the correct menu.
 * 
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview;

import java.awt.event.ActionListener;

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
	
	/**
	 * this will make the newly added menu current
	 * 
	 * @param name of menu to add to menu bar
	 * @return menu added
	 */
	public abstract Object addMenu(String name);
	
	/**
	 * add sub menu to current menu and make it the current menu
	 * 
	 * @param name of new menu
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
	public abstract Object addMenuItem(String name, ActionListener l);
	public abstract Object addMenuItem(String name, ActionListener l, int pos);

	/**
	 * this adds a shortcut, i.e. Ctrl-key or Cmd-key on mac
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

	public void setMenuMnemonic(int vkS) {
		// TODO Auto-generated method stub
	}




}