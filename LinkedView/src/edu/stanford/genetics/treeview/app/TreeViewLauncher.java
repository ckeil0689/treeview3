package edu.stanford.genetics.treeview.app;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.LogBuffer;

public class TreeViewLauncher {

	public static void main(String[] args) {
		
		try {
			boolean isApplet = false;
			
			UIManager.setLookAndFeel(UIManager
					.getCrossPlatformLookAndFeelClassName());
			
			UIManager.put("MenuItem.selectionBackground", 
					GUIParams.ELEMENT_HOV);
			UIManager.put("MenuItem.font", GUIParams.FONT_MENU);
			UIManager.put("MenuItem.background", GUIParams.MENU);
			
			UIManager.put("Menu.selectionBackground", 
					GUIParams.ELEMENT_HOV);
			UIManager.put("Menu.font", GUIParams.FONT_MENU);
			UIManager.put("Menu.background", GUIParams.MENU);
			
			if (!isApplet 
					&& System.getProperty("os.name").startsWith("Mac OS")) {
				// Mac Java 1.3
				System.setProperty("com.apple.macos.useScreenMenuBar", 
						"true");
				System.setProperty("com.apple.mrj.application"
						+ ".growbox.intrudes", "true");
				
				//only needed for 1.3.1 on OSX 10.2
				System.setProperty("com.apple.hwaccel", "true"); 
				
				System.setProperty("com.apple.mrj.application"
						+ ".apple.menu.about.name", "TreeView 3");

				// Mac Java 1.4
				System.setProperty("apple.laf.useScreenMenuBar", "true");
				System.setProperty("apple.awt.showGrowBox", "true");
			}
			
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
			
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
