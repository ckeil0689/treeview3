/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: TreeViewApp.java,v $
 * $Revision: 1.16 $
 * $Date: 2010-05-11 13:31:51 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular,
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER */
package edu.stanford.genetics.treeview;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.ToolTipManager;

import Controllers.TVFrameController;
import edu.stanford.genetics.treeview.model.TVModel;

/**
 * This class defines the treeview application. In practice, it holds the common
 * functionality of the LinkedViewApp and the AppletApp.
 * 
 * The main responsibilities of this class are to - hold static global
 * configuration variables, such as version and URLs - hold link to global
 * XmlConfig object - hold gene and array url preset objects - keep track of all
 * open windows - at one point, it kept track of plugins, but now the
 * PluginManager does that.
 * 
 * The actual Gui handling of a given window is done by TreeViewFrame, which
 * represents a single document. Because multiple documents can be open at a
 * given time, there can be multiple TVFrame objects. However, there can be only
 * one TreeView object. What this really means is that TreeView itself just
 * manages two resources, the window list and the global config. Anything that
 * effects these resources should bottleneck through TreeView.
 * 
 * 1/16/2003 by popular demand (with the exception of my advisor) I have decided
 * to try and make an applet version. As a first cut, I'm just going to make
 * this class extend applet and pop open a JFrame.
 * 
 * 9/24/2003 this has now been superceded by the applet.ButtonApplet class.
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.16 $ $Date: 2010-05-11 13:31:51 $
 */
public abstract class TreeViewApp {// implements WindowListener {

	/** holds all open windows */
	// protected java.util.Vector<Window> windows;

	private final UrlPresets geneUrlPresets;
	private final UrlPresets arrayUrlPresets;
	// private boolean exitOnWindowsClosed = true;

	/** holds global config */
	private final Preferences globalConfig;

	/**
	 * Constructor for the TreeViewApp object. Opens up a globalConfig from the
	 * default location.
	 */
	public TreeViewApp() {

		this(null, false);
	}

	// /**
	// * Constructor for the TreeViewApp object. Opens up a globalConfig from
	// the
	// * default location.
	// */
	// public TreeViewApp(final boolean isApplet) {
	//
	// this(isApplet);
	// }

	/**
	 * Constructor for the TreeViewApp object takes configuration from the
	 * passed in XmlConfig.
	 */
	public TreeViewApp(final Preferences preferences, final boolean isApplet) {

		// windows = new java.util.Vector<Window>();
		if (preferences != null) {
			globalConfig = preferences;

		} else {
			globalConfig = setPreferences();
		}

		geneUrlPresets = new UrlPresets("GeneUrlPresets");
		geneUrlPresets.setConfigNode(getGlobalConfig());

		arrayUrlPresets = new UrlPresets("ArrayUrlPresets");
		arrayUrlPresets.setConfigNode(getGlobalConfig());

		if (arrayUrlPresets.getPresetNames().length == 0) {
			arrayUrlPresets.addPreset("Google",
					"http://www.google.com/search?hl=en&ie=ISO-8859-1"
							+ "&q=HEADER");
			arrayUrlPresets.setDefaultPreset(-1);
		}

		// Generate an XML file of the Preferences at this point, so it
		// can be viewed and analyzed.
		FileOutputStream fos;
		try {
			fos = new FileOutputStream("prefs.xml");
			globalConfig.exportSubtree(fos);
			fos.close();

		} catch (final IOException e1) {
			e1.printStackTrace();

		} catch (final BackingStoreException e1) {
			e1.printStackTrace();
		}

		try {
			final ToolTipManager ttm = ToolTipManager.sharedInstance();
			ttm.setEnabled(true);

		} catch (final Exception e) {
		}

		// The whole UIManager Jazz
		// try {
		//
		// UIManager.setLookAndFeel(UIManager
		// .getCrossPlatformLookAndFeelClassName());
		//
		// UIManager.put("MenuItem.selectionBackground",
		// GUIParams.ELEMENT_HOV);
		// UIManager.put("MenuItem.font", GUIParams.FONT_MENU);
		// UIManager.put("MenuItem.background", GUIParams.MENU);
		//
		// UIManager.put("Menu.selectionBackground",
		// GUIParams.ELEMENT_HOV);
		// UIManager.put("Menu.font", GUIParams.FONT_MENU);
		// UIManager.put("Menu.background", GUIParams.MENU);
		//
		// } catch (final ClassNotFoundException e) {
		// LogBuffer.logException(e);
		//
		// } catch (final InstantiationException e) {
		// LogBuffer.logException(e);
		//
		// } catch (final IllegalAccessException e) {
		// LogBuffer.logException(e);
		//
		// } catch (final UnsupportedLookAndFeelException e) {
		// LogBuffer.logException(e);
		// }
		//
		// // JOptionPane.showMessageDialog(null, System.getProperty( "os.name"
		// ));
		//
		// if (!isApplet) {
		// if (System.getProperty("os.name").startsWith("Mac OS")) {
		// // Mac Java 1.3
		// System.setProperty("com.apple.macos.useScreenMenuBar",
		// "true");
		// System.setProperty("com.apple.mrj.application"
		// + ".growbox.intrudes", "true");
		//
		// //only needed for 1.3.1 on OSX 10.2
		// System.setProperty("com.apple.hwaccel", "true");
		//
		// System.setProperty("com.apple.mrj.application"
		// + ".apple.menu.about.name", "TreeView 3");
		//
		// // Mac Java 1.4
		// System.setProperty("apple.laf.useScreenMenuBar", "true");
		// System.setProperty("apple.awt.showGrowBox", "true");
		// }
		// }
	}

	/**
	 * URL of codebase
	 */
	public abstract URL getCodeBase();

	public UrlPresets getGeneUrlPresets() {

		return geneUrlPresets;
	}

	public UrlPresets getArrayUrlPresets() {

		return arrayUrlPresets;
	}

	/**
	 * Sets up user-specified preferences.
	 * 
	 * @return
	 */
	public Preferences setPreferences() {

		final Preferences configurations = Preferences.userRoot().node(
				"TreeViewApp");

		return configurations;
	}

	/**
	 * returns an XmlConfig representing global configuration variables
	 * 
	 * @return The globalConfig value
	 */
	public Preferences getGlobalConfig() {

		return globalConfig;
	}

	/**
	 * creates a ViewFrame window
	 * 
	 * @throws LoadException
	 */
	public TreeViewFrame openNew() {

		try {
			return openNew(null);

		} catch (final LoadException e) {
			// ignore, since not loading anything shouldn't throw.
			return null;
		}
	}

	/**
	 * creates a new ViewFrame window displaying the specified
	 * <code>FileSet</code>
	 * 
	 * @param fileSet
	 *            FileSet to be displayed
	 * @exception LoadException
	 *                If the fileset cannot be loaded, the window is closed and
	 *                the exception rethrown.
	 * @throws LoadException
	 */
	public TreeViewFrame openNew(final FileSet fileSet) throws LoadException {

		// setup toplevel
		final TreeViewFrame tvFrame = new TreeViewFrame(this);
		final TVModel model = new TVModel();
		final TVFrameController tvController = new TVFrameController(tvFrame,
				model);
		if (fileSet != null) {
			tvController.loadFileSet(fileSet);
			tvFrame.setLoaded(true);
		}
		// tvFrame.addWindowListener(this);
		return tvFrame;
	}

	// /**
	// * A WindowListener, which allows other windows to react to another window
	// * being opened.
	// *
	// * @param e
	// * A window opening event. Used to add the window to the windows
	// * list.
	// */
	// @Override
	// public void windowOpened(final WindowEvent e) {
	//
	// windows.addElement(e.getWindow());
	// rebuildWindowMenus();
	// }

	// /**
	// * rebuilds all the window menus. Should be called whenever a
	// * <code>ViewFrame</code> is created, destroyed, or changes its name. The
	// * first two cases are handled by <code>TreeViewApp</code>, the
	// * <code>ViewFrame</code> itself should call this method when it changes
	// its
	// * name.
	// */
	// public void rebuildWindowMenus() {
	//
	// final int max = windows.size();
	// for (int i = 0; i < max; i++) {
	// final ViewFrame source = (ViewFrame) windows.elementAt(i);
	// // rebuildWindowMenu( source.getWindowMenu());
	// source.rebuildWindowMenu(windows);
	// }
	// }

	// /**
	// * A WindowListener, which allows other windows to react to another window
	// * being closed.
	// *
	// * @param e
	// * A window closing event. Used to remove the window from the
	// * windows list.
	// */
	// @Override
	// public void windowClosed(final WindowEvent e) {
	//
	// windows.removeElement(e.getWindow());
	//
	// if (windows.isEmpty() && exitOnWindowsClosed) {
	// endProgram();
	// }
	// rebuildWindowMenus();
	// }

	// /**
	// * loops over the list of windows the <code>TreeViewApp</code> has
	// collected
	// * through the WindowListener interface. just closes the window; other
	// * bookkeeping stuff is done by the <code>windowClosed</code> method.
	// */
	// public void closeAllWindows() {
	//
	// final Enumeration<Window> e = windows.elements();
	// while (e.hasMoreElements()) {
	// final ViewFrame f = (ViewFrame) e.nextElement();
	// f.closeWindow();
	// }
	// }
	//
	// public ViewFrame[] getWindows() {
	//
	// final ViewFrame[] frames = new ViewFrame[windows.size()];
	// int i = 0;
	//
	// final Enumeration<Window> e = windows.elements();
	// while (e.hasMoreElements()) {
	// frames[i++] = (ViewFrame) e.nextElement();
	// }
	//
	// return frames;
	// }

	/** Stores the globalconfig, closes all windows, and then exits. */
	protected abstract void endProgram();

	// @Override
	// public void windowActivated(final WindowEvent e) {
	// // nothing
	// }
	//
	// @Override
	// public void windowClosing(final WindowEvent e) {
	// // nothing
	// }
	//
	// @Override
	// public void windowDeactivated(final WindowEvent e) {
	// // nothing
	// }
	//
	// @Override
	// public void windowDeiconified(final WindowEvent e) {
	// // nothing
	// }
	//
	// @Override
	// public void windowIconified(final WindowEvent e) {
	// // nothing
	// }

	// /**
	// * @param exitOnWindowsClosed
	// * the exitOnWindowsClosed to set
	// */
	// public void setExitOnWindowsClosed(final boolean exitOnWindowsClosed) {
	//
	// this.exitOnWindowsClosed = exitOnWindowsClosed;
	// }
}
