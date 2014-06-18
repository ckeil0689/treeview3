package edu.stanford.genetics.treeview.app;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TVScrollBarUI;

public class TreeViewLauncher {

	public static void main(final String[] args) {

		try {
			final boolean isApplet = false;
			
			// This String will store the menuBarUI key from 
			// Apple's Aqua LookAndFeel so it can be implemented
			// in CrossPlatform LAF afterwards which cannot use 
			// OSX native menuBar otherwise...
			String menuBarUI = "";
			if (!isApplet && System.getProperty("os.name").contains("Mac")) {

				System.setProperty("com.apple.mrj.application"
						+ ".apple.menu.about.name", "TreeView 3");

				// Mac Java 1.4
				System.setProperty("apple.laf.useScreenMenuBar", "true");
				System.setProperty("apple.awt.showGrowBox", "true");
				
				UIManager.setLookAndFeel(UIManager
						.getSystemLookAndFeelClassName());
				
				menuBarUI = UIManager.getString("MenuBarUI");
				
			}
			
			UIManager.setLookAndFeel(UIManager
					.getCrossPlatformLookAndFeelClassName());
			
			UIManager.put("ScrollBarUI", TVScrollBarUI.class.getName());

			if (!isApplet && System.getProperty("os.name").contains("Mac")) {
				UIManager.put("MenuBarUI", menuBarUI);
				
			} else {
				UIManager.put("MenuItem.selectionBackground", 
								GUIParams.ELEMENT_HOV);
				UIManager.put("MenuItem.font", GUIParams.FONT_MENU);
				UIManager.put("MenuItem.background", GUIParams.MENU);
	
				UIManager.put("Menu.selectionBackground", 
						GUIParams.ELEMENT_HOV);
				UIManager.put("Menu.font", GUIParams.FONT_MENU);
				UIManager.put("Menu.background", GUIParams.MENU);
			}

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

		// JOptionPane.showMessageDialog(null, System.getProperty( "os.name" ));
	}
}
