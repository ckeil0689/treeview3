package edu.stanford.genetics.treeview.app;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
//import java.awt.EventQueue;
//import javax.swing.JFrame;
//import javax.swing.JMenuBar;

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * Launcher class which wraps the creation of the GUI as well as the main
 * method in LinkedViewApp in a Swing thread and allows to modify some UIManager
 * configurations beforehand. This is useful for the activation of the native 
 * Mac OSX menubar, for example.
 * @author CKeil
 */
public class TreeViewLauncher {

	private static void macSetup() {
		String os = System.getProperty("os.name").toLowerCase();
		boolean isMac = os.startsWith("mac os x");    

		if(!isMac)
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
			 * Set the look and feel to the system look and feel of the 
			 * user's individual machine.
			 */
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
//			EventQueue.invokeLater(new Runnable() {

				@Override
				public void run() {
//	                JFrame frame = new JFrame("Gabby");
//	                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//	                frame.setLocationByPlatform(true);
//	                JMenuBar menuBar = new JMenuBar();
//	                frame.setJMenuBar(menuBar);
//	                frame.setVisible(true);
					new LinkedViewApp();
//					Application app = new LinkedViewApp();
//					Main main = new Main();           
//					app.addApplicationListener(main.getApplicationListener());
//
//					app.addPreferencesMenuItem();
//					app.setEnabledPreferencesMenu(true);           
				}
			});
		} catch (final ClassNotFoundException | InstantiationException 
				|  IllegalAccessException | UnsupportedLookAndFeelException e) {
			LogBuffer.logException(e);
		} 
	}
}
