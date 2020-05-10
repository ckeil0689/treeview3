package app;

import util.LogBuffer;

import javax.swing.*;

/**
 * Launcher class which wraps the creation of the GUI as well as the main method
 * in app.LinkedViewApp in a Swing thread and allows to modify some UIManager
 * configurations beforehand. This is useful for the activation of the native
 * Mac OSX menubar, for example.
 */
public class TreeView3 {

	/**
	 * Tests if the current system is running on a version of OSX
	 * @return true if the operating system on which the JVM runs is OSX
	 */
	public static boolean isMac() {

		final String os = System.getProperty("os.name").toLowerCase();
		final boolean isMac = os.startsWith("mac os x");

		return isMac;
	}

	/**
	 * Sets up important system properties for Apple OS X systems. This includes
	 * mostly properties related to the OS X menu bar.
	 */
	private static void osSetup() {

		//This enhances graphics performance
		//System.setProperty("sun.java2d.opengl", "true");

		if(isMac()) {

			LogBuffer.println("Running on a Mac.");

			System.setProperty("apple.laf.useScreenMenuBar", "true");
			//This does not appear to do anything (on mac)
			//System.setProperty("com.apple.mrj.application.apple.menu.about.name",
			//	"app.TreeView3");
			//The latest Mac OS X doesn't have growboxes, so this is here primarily
			//for older versions of OS X
			System.setProperty("apple.awt.showGrowBox", "true");
			//Capture quit events to trigger close window so we can save the
			//clustering, etc..  Lots to do when quitting...
			System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
		}
	}

	public static void main(final String[] args) {

		try {
			osSetup();

			/*
			 * Set the look and feel to the system look and feel of the user's
			 * individual machine.
			 */
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

			/* Application start-up after setting System and UIManager stuff. */
			javax.swing.SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					TreeViewApp lvApp = new LinkedViewApp();
					lvApp.start();
				}
			});
		} catch (final ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e) {
			LogBuffer.logException(e);
			LogBuffer.println("Start up failed.");
		}
	}
}
