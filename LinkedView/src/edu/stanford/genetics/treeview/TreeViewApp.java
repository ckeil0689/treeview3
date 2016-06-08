/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.ToolTipManager;

import Controllers.TVController;
import Utilities.StringRes;
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

	private final UrlPresets rowUrlPresets;
	private final UrlPresets columnUrlPresets;
	// private boolean exitOnWindowsClosed = true;

	/* holds global configuration settings */
	private final Preferences globalConfig;

	/**
	 * Constructor for the TreeViewApp object. Opens up a globalConfig from the
	 * default location.
	 */
	public TreeViewApp() {

		this(null, false);
	}

	/**
	 * Constructor for the TreeViewApp object takes configuration from the
	 * passed in XmlConfig.
	 *
	 * @param Preferences
	 *            The saved preferences file.
	 * @param boolean checks if the current running instance is a browser applet
	 *        or desktop application.
	 */
	public TreeViewApp(final Preferences preferences, final boolean isApplet) {

		// windows = new java.util.Vector<Window>();
		if (preferences != null) {
			globalConfig = preferences;

		} else {
			globalConfig = getMainPreferencesNode();
		}
		
		handlePreferencesVersion();

		rowUrlPresets = new UrlPresets("GeneUrlPresets");
		rowUrlPresets.setConfigNode(getGlobalConfig());

		columnUrlPresets = new UrlPresets("ArrayUrlPresets");
		columnUrlPresets.setConfigNode(getGlobalConfig());

		if (columnUrlPresets.getPresetNames().length == 0) {
			columnUrlPresets.addPreset("Google",
					"http://www.google.com/search?hl=en&ie=ISO-8859-1"
							+ "&q=HEADER");
			columnUrlPresets.setDefaultPreset(-1);
		}

		// Generate an XML file of the Preferences at this point, so it
		// can be viewed and analyzed.
		try {
			FileOutputStream fos = new FileOutputStream("prefs.xml");
			globalConfig.exportSubtree(fos);
			fos.close();

			final ToolTipManager ttm = ToolTipManager.sharedInstance();
			ttm.setEnabled(true);

		} catch (final IOException | BackingStoreException e1) {
			LogBuffer.logException(e1);
			LogBuffer.println("Could not export preferences XML "
					+ "file for viewing.");
		}
	}

	/** URL of codebase */
	public abstract URL getCodeBase();

	/** Starts up the application's setup process */
	public abstract void start();

	/**
	 * The returned object contains presets for the URLs used when searching for
	 * row labels in online databases.
	 * 
	 * @return URLPresets for row labels.
	 */
	public UrlPresets getRowLabelUrlPresets() {

		return rowUrlPresets;
	}

	/**
	 * The returned object contains presets for the URLs used when searching for
	 * column labels in online databases.
	 * 
	 * @return URLPresets for column labels.
	 */
	public UrlPresets getColumnLabelUrlPresets() {

		return columnUrlPresets;
	}

	/**
	 * Loads the Preferences file stored on the users machine. If none is
	 * present with the root node 'TreeViewApp', it creates a new one.
	 *
	 * @return Preferences
	 */
	public Preferences getMainPreferencesNode() {

		return Preferences.userRoot().node("TreeViewApp");
	}
	
	/** 
	 * Versioning following MAJOR.MINOR.PATCH
	 * See http://semver.org/
	 * This routine now has the purpose of handling the preferences version.
	 * If needed, it will make a decision how old versions are adapted and if
	 * major restructuring of old preferences setups are needed.
	 */
	private void handlePreferencesVersion() {
		
		// Default 'none' to detect if Preferences are stored for the first time
		String currVersion = getNotedPreferencesVersion();
		
	  // An earlier version exists
		if(!"none".equals(currVersion)) {
			// Existing version does not match current version
			if(!StringRes.preferencesVersionTag.equals(currVersion)) {
				// TODO make sure old preferences are migrated well (BB Issue #407)
			}
		}
		
		// finally store the new version
		globalConfig.put("version", StringRes.preferencesVersionTag);
	}
	
	/**
	 * 
	 * @return The version String denoting the Preferences version stored in
	 * the node which was initially assigned to the configNode of this class
	 * upon application startup.
	 */
	public String getNotedPreferencesVersion() {
		
		String version;
		if(globalConfig == null) {
			return "none";
		}
		
		version = globalConfig.get("version", "none");
		
		return version;
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
		final DataModel model = new TVModel();
		final TVController tvController = new TVController(tvFrame, model);

		if (fileSet != null) {
			tvController.getDataInfoAndLoad(fileSet, false);
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
