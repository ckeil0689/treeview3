/*
 * Created on May 16, 2008
 *
 * This class is meant to wrap the menu entirely, so I can
 * easily change between heavyweight menus and lightweight JMenus.
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.core;

import java.awt.*;
import java.awt.event.ActionListener;

import edu.stanford.genetics.treeview.TreeviewMenuBarI;

public class TreeViewMenuBar extends TreeviewMenuBarI {
	private MenuBar underlyingMenuBar;
	private Menu currentMenu;
	private MenuItem currentMenuItem;

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#getUnderlyingMenuBar()
	 */
	public MenuBar getUnderlyingMenuBar() {
		return underlyingMenuBar;
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#setUnderlyingMenuBar(java.awt.MenuBar)
	 */
	public void setUnderlyingMenuBar(MenuBar underlyingMenuBar) {
		this.underlyingMenuBar = underlyingMenuBar;
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#addMenu(java.lang.String)
	 */
	public Object addMenu(String name) {
		currentMenu = new Menu(name);
		underlyingMenuBar.add(currentMenu);
		return currentMenu;
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#addMenuItem(java.lang.String, java.awt.event.ActionListener)
	 */
	public Object addMenuItem(String name, ActionListener l) {
		currentMenuItem = new MenuItem(name);
		currentMenuItem.addActionListener(l);
		currentMenu.add(currentMenuItem);
		return currentMenuItem;
	}

	public Object addMenuItem(String name, ActionListener l, int pos) {
		currentMenuItem = new MenuItem(name);
		currentMenuItem.addActionListener(l);
		currentMenu.insert(currentMenuItem, pos);
		return currentMenuItem;
	}
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#addSubMenu(java.lang.String)
	 */
	public Object addSubMenu(String name) {
		Menu newMenu = new Menu(name);
		currentMenu.add(newMenu);
		currentMenu = newMenu;
		return currentMenu;
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#setAccelerator(int)
	 */
	public void setAccelerator(int key) {
		currentMenuItem.setShortcut(new MenuShortcut(key));
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#setMnemonic(int)
	 */
	public void setMnemonic(int key) {
//		currentMenuItem.setShortcut(new MenuShortcut(key));
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#setMenu(java.lang.String)
	 */
	public Object setMenu(String name) {
		int i;
		for (i = 0; i < underlyingMenuBar.getMenuCount(); i++) {
			if (underlyingMenuBar.getMenu(i).getLabel() == name) {
				currentMenu = underlyingMenuBar.getMenu(i);
				return currentMenu;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#setSubMenu(java.lang.String)
	 */
	public Object setSubMenu(String name) {
		int i;
		for (i = 0; i < currentMenu.getItemCount(); i++) {
			if (currentMenu.getItem(i).getLabel() == name) {
				currentMenu = (Menu) currentMenu.getItem(i);
				return currentMenu;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#clearMenu()
	 */
	public void clearMenu() {
		currentMenu.removeAll();
	}

	public void addSeparator() {
		currentMenu.addSeparator();
	}

	public int getItemCount() {
		return currentMenu.getItemCount();
	}

	public void removeAll() {
		currentMenu.removeAll();
	}

	public void removeMenuItems() {
		int i;
		@SuppressWarnings("unused") // used to throw an exception
		Menu testItem;
		for (i = currentMenu.getItemCount()-1; i >=0; i--) {
			try {
				testItem = (Menu) currentMenu.getItem(i);
			} catch (Exception e) {
				currentMenu.remove(i);				
			}
		}
	}

	public void setEnabled(boolean value) {
		currentMenu.setEnabled(value);		
	}
}
