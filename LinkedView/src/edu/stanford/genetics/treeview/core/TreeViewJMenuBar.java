/*
 * Created on Jun 3, 2008
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.core;

import java.awt.*;
import java.awt.event.ActionListener;

import javax.swing.*;

import edu.stanford.genetics.treeview.TreeviewMenuBarI;

public class TreeViewJMenuBar extends TreeviewMenuBarI {
	private JMenuBar underlyingMenuBar;
	private JMenu currentMenu;
	private JMenuItem currentMenuItem;

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#getUnderlyingMenuBar()
	 */
	public JMenuBar getUnderlyingMenuBar() {
		return underlyingMenuBar;
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#setUnderlyingMenuBar(java.awt.MenuBar)
	 */
	public void setUnderlyingMenuBar(JMenuBar underlyingMenuBar) {
		this.underlyingMenuBar = underlyingMenuBar;
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#addMenu(java.lang.String)
	 */
	public Object addMenu(String name) {
		currentMenu = new JMenu(name);
		underlyingMenuBar.add(currentMenu);
		return currentMenu;
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#addMenuItem(java.lang.String, java.awt.event.ActionListener)
	 */
	public Object addMenuItem(String name, ActionListener l) {
		currentMenuItem = new JMenuItem(name);
		currentMenuItem.addActionListener(l);
		currentMenu.add(currentMenuItem);
		return currentMenuItem;
	}

	public Object addMenuItem(String name, ActionListener l, int pos) {
		currentMenuItem = new JMenuItem(name);
		currentMenuItem.addActionListener(l);
		currentMenu.insert(currentMenuItem, pos);
		return currentMenuItem;
	}
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#addSubMenu(java.lang.String)
	 */
	public Object addSubMenu(String name) {
		JMenu newMenu = new JMenu( name);
		currentMenu.add(newMenu);
		currentMenu = newMenu;
		return currentMenu;
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#setAccelerator(int)
	 */
	public void setAccelerator(int key) {
		currentMenuItem.setAccelerator(KeyStroke.getKeyStroke((char) key,
			    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#setMnemonic(int)
	 */
	public void setMnemonic(int key) {
		currentMenuItem.setMnemonic(key);
	}
	
	public void setMenuMnemonic(int key) {
		currentMenu.setMnemonic(key);
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.TreeviewMenuBarI#setMenu(java.lang.String)
	 */
	public Object setMenu(String name) {
		int i;
		for (i = 0; i < underlyingMenuBar.getMenuCount(); i++) {
			String testName = underlyingMenuBar.getMenu(i).getText();
			if (testName != null && testName.equals(name)) {
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
			if (currentMenu.getItem(i).getText() == name) {
				currentMenu = (JMenu) currentMenu.getItem(i);
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
		@SuppressWarnings("unused")
		JMenu testItem;
		for (i = currentMenu.getItemCount()-1; i >=0; i--) {
			try {
				testItem = (JMenu) currentMenu.getItem(i);
			} catch (Exception e) {
				currentMenu.remove(i);				
			}
		}
	}

	public void setEnabled(boolean value) {
		currentMenu.setEnabled(value);		
	}

}
