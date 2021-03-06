/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package gui.window.menubar;

import javax.swing.event.MenuListener;
import java.awt.*;

public class TreeViewMenuBar extends TreeviewMenuBarI {
	private MenuBar underlyingMenuBar;
	private Menu currentMenu;
	private MenuItem currentMenuItem;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * treeview.TreeviewMenuBarI#getUnderlyingMenuBar()
	 */
	public MenuBar getUnderlyingMenuBar() {
		return underlyingMenuBar;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * treeview.TreeviewMenuBarI#setUnderlyingMenuBar(
	 * java.awt.MenuBar)
	 */
	public void setUnderlyingMenuBar(final MenuBar underlyingMenuBar) {
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
		currentMenu = new Menu(name);
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
	// currentMenuItem = new MenuItem(name);
	// currentMenuItem.addActionListener(l);
	// currentMenu.add(currentMenuItem);
	// return currentMenuItem;
	// }
	//
	// @Override
	// public Object addMenuItem(final String name, final ActionListener l,
	// final int pos) {
	// currentMenuItem = new MenuItem(name);
	// currentMenuItem.addActionListener(l);
	// currentMenu.insert(currentMenuItem, pos);
	// return currentMenuItem;
	// }
	//
	@Override
	public Object addMenuItem(final String name) {

		currentMenuItem = new MenuItem(name);
		currentMenu.add(currentMenuItem);
		return currentMenuItem;
	}

	@Override
	public Object addMenuItem(final String name, final int pos) {

		currentMenuItem = new MenuItem(name);
		currentMenu.insert(currentMenuItem, pos);
		return currentMenuItem;
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
		final Menu newMenu = new Menu(name);
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
		currentMenuItem.setShortcut(new MenuShortcut(key));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.TreeviewMenuBarI#setMnemonic(int)
	 */
	@Override
	public void setMnemonic(final int key) {
		// currentMenuItem.setShortcut(new MenuShortcut(key));
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
			if (underlyingMenuBar.getMenu(i).getLabel() == name) {
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
			if (currentMenu.getItem(i).getLabel() == name) {
				currentMenu = (Menu) currentMenu.getItem(i);
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

	@Override
	public void addMenuListener(final MenuListener listener) {
		// TODO Auto-generated method stub

	}
}
