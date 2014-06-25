package edu.stanford.genetics.treeview.app;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import edu.stanford.genetics.treeview.GUIFactory;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TVScrollBarUI;

/**
 * Launcher class which wraps the creation of the GUI as well as the main
 * method in LinkedViewApp in a Swing thread and allows to modify some UIManager
 * configurations beforehand (OSX menubar etc.).
 * @author CKeil
 *
 */
public class TreeViewLauncher {

	public static void main(final String[] args) {

		try {
			final boolean isApplet = false;
			
			UIManager.setLookAndFeel(UIManager
					.getSystemLookAndFeelClassName());
			
			/*
			 * This String will store the menuBarUI key from 
			 * Apple's Aqua LookAndFeel so it can be implemented
			 * in CrossPlatform LAF afterwards which cannot use 
			 * OSX native menuBar otherwise...
			 */
			String menuBarUI = "";
			if (!isApplet && System.getProperty("os.name").contains("Mac")) {

				System.setProperty("com.apple.mrj.application"
						+ ".apple.menu.about.name", "TreeView 3");

				// Activate OSX native menubar
				System.setProperty("apple.laf.useScreenMenuBar", "true");
				System.setProperty("apple.awt.showGrowBox", "true");
				
//				UIManager.setLookAndFeel(UIManager
//						.getSystemLookAndFeelClassName());
				
				/* 
				 * To keep custom look and feel AND OSX menubar, store variable
				 * and then switch back to CrossPlatformLookAndFeel. Otherwise
				 * the menubar can't be displayed.
				 */
				menuBarUI = UIManager.getString("MenuBarUI");
				
			}
			
//			UIManager.setLookAndFeel(UIManager
//					.getCrossPlatformLookAndFeelClassName());
			
//			UIManager.put("ScrollBarUI", TVScrollBarUI.class.getName());

//			if (!isApplet && System.getProperty("os.name").contains("Mac")) {
//				UIManager.put("MenuBarUI", menuBarUI);
//				
//			} else {
//				// Custom menu components for non-OSX systems.
//				UIManager.put("MenuItem.selectionBackground", 
//								GUIFactory.ELEMENT_HOV);
//				UIManager.put("MenuItem.font", GUIFactory.FONT_MENU);
//				UIManager.put("MenuItem.background", GUIFactory.MENU);
//	
//				UIManager.put("Menu.selectionBackground", 
//						GUIFactory.ELEMENT_HOV);
//				UIManager.put("Menu.font", GUIFactory.FONT_MENU);
//				UIManager.put("Menu.background", GUIFactory.MENU);
//			}

			javax.swing.SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					new LinkedViewApp();
				}
			});

		} catch (final ClassNotFoundException e) {
			LogBuffer.logException(e);

		} catch (final InstantiationException e) {
			LogBuffer.logException(e);

		} catch (final IllegalAccessException e) {
			LogBuffer.logException(e);

		} catch (final UnsupportedLookAndFeelException e) {
			LogBuffer.logException(e);
		}
	}
}
