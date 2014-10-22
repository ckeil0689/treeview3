package edu.stanford.genetics.treeview.app;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * Launcher class which wraps the creation of the GUI as well as the main
 * method in LinkedViewApp in a Swing thread and allows to modify some UIManager
 * configurations beforehand. This is useful for the activation of the native 
 * Mac OSX menubar, for example.
 * @author CKeil
 */
public class TreeViewLauncher {

	public static void main(final String[] args) {

		try {
			final boolean isApplet = false;
			
			/*
			 * Set the look and feel to the system look and feel of the 
			 * user's individual machine.
			 */
			UIManager.setLookAndFeel(UIManager
					.getSystemLookAndFeelClassName());
			
			/*
			 * Check for OS and use menubar for Mac if applicable, 
			 * for a more native feel.
			 */
			if (!isApplet && System.getProperty("os.name").contains("Mac")) {

				System.setProperty("com.apple.mrj.application"
						+ ".apple.menu.about.name", "TreeView 3");

				/* Activate OSX native menubar */
				System.setProperty("apple.laf.useScreenMenuBar", "true");
				System.setProperty("apple.awt.showGrowBox", "true");
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
	}
}
