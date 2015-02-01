package edu.stanford.genetics.treeview.app;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * Launcher class which wraps the creation of the GUI as well as the main method
 * in LinkedViewApp in a Swing thread and allows to modify some UIManager
 * configurations beforehand. This is useful for the activation of the native
 * Mac OSX menubar, for example.
 *
 * @author CKeil
 */
// public class TreeViewLauncher {
public class TreeView3 {

	private static void macSetup() {
		final String os = System.getProperty("os.name").toLowerCase();
		final boolean isMac = os.startsWith("mac os x");

		if (!isMac)
			return;

		LogBuffer.println("Running on a mac.");

		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name",
				"TreeView3");
		System.setProperty("apple.awt.showGrowBox", "true");
	}

	public static void main(final String[] args) {

		try {
			macSetup();

			/*
			 * Set the look and feel to the system look and feel of the user's
			 * individual machine.
			 */
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

			javax.swing.SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					new LinkedViewApp();
				}
			});
		} catch (final ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e) {
			LogBuffer.logException(e);
		}
	}
}
