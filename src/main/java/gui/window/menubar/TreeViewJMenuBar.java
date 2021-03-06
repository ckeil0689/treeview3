/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package gui.window.menubar;

import javax.swing.*;
import javax.swing.event.MenuListener;
import java.awt.*;

public class TreeViewJMenuBar extends TreeviewMenuBarI {

	private JMenuBar underlyingMenuBar;
	private JMenu currentMenu;
	private JMenuItem currentMenuItem;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * treeview.TreeviewMenuBarI#getUnderlyingMenuBar()
	 */
	public JMenuBar getUnderlyingMenuBar() {

		return underlyingMenuBar;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * treeview.TreeviewMenuBarI#setUnderlyingMenuBar(
	 * java.awt.MenuBar)
	 */
	public void setUnderlyingMenuBar(final JMenuBar underlyingMenuBar) {

		this.underlyingMenuBar = underlyingMenuBar;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * treeview.TreeviewMenuBarI#addMenu(java.lang.String)
	 */
	@Override
	public Object addMenu(final String name) {

		currentMenu = new JMenu(name);
		underlyingMenuBar.add(currentMenu);
		return currentMenu;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * treeview.TreeviewMenuBarI#addMenuItem(java.lang
	 * .String, java.awt.event.ActionListener)
	 */
	// @Override
	// public Object addMenuItem(final String name, final ActionListener l) {
	//
	// currentMenuItem = new JMenuItem(name);
	// currentMenuItem.addActionListener(l);
	// currentMenu.add(currentMenuItem);
	// return currentMenuItem;
	// }
	//
	// @Override
	// public Object addMenuItem(final String name, final ActionListener l,
	// final int pos) {
	//
	// currentMenuItem = new JMenuItem(name);
	// currentMenuItem.addActionListener(l);
	// currentMenu.insert(currentMenuItem, pos);
	// return currentMenuItem;
	// }

	@Override
	public Object addMenuItem(final String name) {

		currentMenuItem = new JMenuItem(name);
		currentMenu.add(currentMenuItem);
		return currentMenuItem;
	}

	@Override
	public Object addMenuItem(final String name, final int pos) {

		currentMenuItem = new JMenuItem(name);
		currentMenu.insert(currentMenuItem, pos);
		return currentMenuItem;
	}

	/**
	 * Adds a MenuListener to the currently active menu.
	 *
	 * @param listener
	 */
	@Override
	public void addMenuListener(final MenuListener listener) {

		currentMenu.addMenuListener(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * treeview.TreeviewMenuBarI#addSubMenu(java.lang.
	 * String)
	 */
	@Override
	public Object addSubMenu(final String name) {

		final JMenu newMenu = new JMenu(name);
		currentMenu.add(newMenu);
		currentMenu = newMenu;
		return currentMenu;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeviewMenuBarI#setAccelerator(int)
	 */
	@Override
	public void setAccelerator(final int key) {

		currentMenuItem.setAccelerator(KeyStroke.getKeyStroke((char) key,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeviewMenuBarI#setMnemonic(int)
	 */
	@Override
	public void setMnemonic(final int key) {

		currentMenuItem.setMnemonic(key);
	}

	@Override
	public void setMenuMnemonic(final int key) {

		currentMenu.setMnemonic(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * treeview.TreeviewMenuBarI#setMenu(java.lang.String)
	 */
	@Override
	public Object setMenu(final String name) {

		int i;
		for (i = 0; i < underlyingMenuBar.getMenuCount(); i++) {
			final String testName = underlyingMenuBar.getMenu(i).getText();
			if (testName != null && testName.equals(name)) {
				currentMenu = underlyingMenuBar.getMenu(i);
				return currentMenu;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * treeview.TreeviewMenuBarI#setSubMenu(java.lang.
	 * String)
	 */
	@Override
	public Object setSubMenu(final String name) {

		int i;
		for (i = 0; i < currentMenu.getItemCount(); i++) {

			if (currentMenu.getItem(i) != null
					&& currentMenu.getItem(i).getText() == name) {
				currentMenu = (JMenu) currentMenu.getItem(i);
				return currentMenu;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeviewMenuBarI#clearMenu()
	 */
	public void clearMenu() {
		currentMenu.removeAll();
	}

	@Override
	public void addSeparator() {
		currentMenu.addSeparator();
	}

	@Override
	public int getItemCount() {
		return currentMenu.getItemCount();
	}

	@Override
	public void removeAll() {
		currentMenu.removeAll();
	}

	@Override
	public void removeMenuItems() {

		int i;
		for (i = currentMenu.getItemCount() - 1; i >= 0; i--) {
			try {
				currentMenu.getItem(i);
			} catch (final Exception e) {
				currentMenu.remove(i);
			}
		}
	}

	@Override
	public void setEnabled(final boolean value) {
		currentMenu.setEnabled(value);
	}

}
