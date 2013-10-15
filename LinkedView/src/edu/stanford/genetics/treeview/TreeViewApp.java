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

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *  This class defines the treeview application.
 *  In practice, it holds the common functionality of the LinkedViewApp and the
 *  AppletApp.
 *  
 *  The main responsibilities of this class are to 
 *  - hold static global configuration variables, such as version and URLs
 *  - hold link to global XmlConfig object
 *  - hold gene and array url preset objects
 *  - keep track of all open windows
 *  - at one point, it kept track of plugins, but now the PluginManager does that.
 *  
 *  The actual Gui handling of a given
 *  window is done by TreeViewFrame, which represents a single document. Because
 *  multiple documents can be open at a given time, there can be multiple TVFrame
 *  objects. however, there can be only one TreeView object. what this really means
 *  is that TreeView itself just manages two resources, the window list and the global
 *  config. Anything that effects these resources should bottleneck through TreeView.
 *  
 *  1/16/2003 by popular demand (with the exception of my advisor) I have decided
 *  to try and make an applet version. as a first cut, I'm just going to make this
 *  class extend applet and pop open a jframe.
 *
 * 9/24/2003 this has now been superceded by the applet.ButtonApplet class.
 *  
 * @author     Alok Saldanha <alok@genome.stanford.edu>
 * @version   $Revision: 1.16 $ $Date: 2010-05-11 13:31:51 $
 */
public abstract class TreeViewApp implements WindowListener {

	/**  Version of application */
	public final static String versionTag  = "1.1.6";

	private Preferences prefs;
	
	/**
	 * URL of codebase
	 */
	public abstract URL getCodeBase();
	
	/**
	 *  The release-level version tag for the whole application.
	 *
	 * @return    a string representing the versionTag
	 */
	public static String getVersionTag() {
		return versionTag;
	}

	// url of homepage to go for updates
	protected static String updateUrl = "http://jtreeview.sourceforge.net";
	// url of announcements mailing list
	protected static String announcementUrl = "http://lists.sourceforge.net/lists/listinfo/jtreeview-announce";
	/** 
	* Getter for updateUrl, a string representing a website where you can download newer versions.
	*/
	public static String getUpdateUrl() {
		return updateUrl;
	}
	public static String getAnnouncementUrl() {
		return announcementUrl;
	}
	
	
	/**  holds all open windows */
	protected java.util.Vector windows;
	/**  holds global config */
	private XmlConfig globalConfig;


	/**  Constructor for the TreeViewApp object.
	* Opens up a globalConfig from the default location.
	*/
	public TreeViewApp() {
		this (new XmlConfig(globalConfigName(), "ProgramConfig"), false);
	}
	/**  Constructor for the TreeViewApp object.
	* Opens up a globalConfig from the default location.
	*/
	public TreeViewApp(boolean isApplet) {
		this (new XmlConfig(globalConfigName(), "ProgramConfig"), isApplet);
	}
	/**
	* Constructor for the TreeViewApp object
	* takes configuration from the passed in XmlConfig.
	*/
	public TreeViewApp(XmlConfig xmlConfig, boolean isApplet) {
		windows = new java.util.Vector();
		globalConfig = xmlConfig;
		
		geneUrlPresets = new UrlPresets(getGlobalConfig().getNode("GeneUrlPresets"));
		arrayUrlPresets = new UrlPresets();
		arrayUrlPresets.bindConfig(getGlobalConfig().getNode("ArrayUrlPresets"));
		if(arrayUrlPresets.getPresetNames().length == 0) {
		  	arrayUrlPresets.addPreset("Google",
		  "http://www.google.com/search?hl=en&ie=ISO-8859-1&q=HEADER");
		  	arrayUrlPresets.setDefaultPreset(-1);
		}
		try {
			ToolTipManager ttm  = ToolTipManager.sharedInstance();
			ttm.setEnabled(true);
		} catch (Exception e) {
		}
		
		prefs = new Preferences();
		prefs.bindConfig(getGlobalConfig().getNode("Preferences"));
		//JOptionPane.showMessageDialog(null, System.getProperty( "os.name" ));
		if (!isApplet) {
		if ( System.getProperty( "os.name" ).startsWith( "Mac OS" )) {
			if (prefs.getMacStyleMenubar()) {

			// Mac Java 1.3
			System.setProperty( "com.apple.macos.useScreenMenuBar", "true" );
			System.setProperty( "com.apple.mrj.application.growbox.intrudes", "true" );
			System.setProperty( "com.apple.hwaccel", "true" ); // only needed for 1.3.1 on OS X 10.2
			System.setProperty( "com.apple.mrj.application.apple.menu.about.name", "TreeView" );
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException e) {
				LogBuffer.logException(e);
			} catch (InstantiationException e) {
				LogBuffer.logException(e);
			} catch (IllegalAccessException e) {
				LogBuffer.logException(e);
			} catch (UnsupportedLookAndFeelException e) {
				LogBuffer.logException(e);
			}
			
				// Mac Java 1.4
				System.setProperty( "apple.laf.useScreenMenuBar", "true" );
				System.setProperty( "apple.awt.showGrowBox", "true" );
					
			}
		}
		}
	}
	
	private UrlPresets geneUrlPresets, arrayUrlPresets;
	private boolean exitOnWindowsClosed = true;
	  public UrlPresets getGeneUrlPresets() {
		return geneUrlPresets;
	  }
	  public UrlPresets getArrayUrlPresets() {
		return arrayUrlPresets;
	  }



	
	/**  creates a ViewFrame window  
	 * @throws LoadException */
	public ViewFrame openNew() {
		
		try {
			return openNew(null);
		} catch (LoadException e) {
			// ignore, since not loading anything shouldn't throw.
			return null;
		}
	}

	
	/**
	 *  creates a new ViewFrame window displaying the specified <code>FileSet</code>
	 *
	 * @param  fileSet            FileSet to be displayed
	 * @exception  LoadException  If the fileset cannot be loaded, the window is closed and the exception rethrown.
	 * @throws LoadException 
	 */
	public ViewFrame openNew(FileSet fileSet) throws LoadException {
		
		// setup toplevel
		TreeViewFrame tvFrame  = new TreeViewFrame(this);
		if (fileSet != null) {
			try {
				tvFrame.loadFileSet(fileSet);
				tvFrame.setLoaded(true);
			} catch (LoadException e) {
				tvFrame.dispose();
				throw e;
			}
		}
		tvFrame.addWindowListener(this);
		return tvFrame;
	}
	/**
	* same as above, but doesn't open a loading window (damn deadlocks!)
	 * @throws LoadException
	*/
	public ViewFrame openNewNW(FileSet fileSet) throws LoadException {
		
		// setup toplevel
		TreeViewFrame tvFrame  = new TreeViewFrame(this);
		if (fileSet != null) {
			try {
				tvFrame.loadFileSetNW(fileSet);
				tvFrame.setLoaded(true);
			} catch (LoadException e) {
				tvFrame.dispose();
				throw e;
			}
		}
		tvFrame.addWindowListener(this);
		return tvFrame;
	}


	/**
	 *  returns an XmlConfig representing global configuration variables
	 *
	 * @return    The globalConfig value
	 */
	public XmlConfig getGlobalConfig() {
		return globalConfig;
	}

	/**
	 *  A WindowListener, which allows other windows to react to another window being opened.
	 *
	 * @param  e  A window opening event. Used to add the window to the windows list.
	 */
	@Override
	public void windowOpened(WindowEvent e) {
		windows.addElement(e.getWindow());
		rebuildWindowMenus();
	}


	/**
	 *  rebuilds all the window menus. Should be called whenever a <code>ViewFrame</code> is
	 *  created, destroyed, or changes its name. The first two cases are handled by
	 *  <code>TreeViewApp</code>, the <code>ViewFrame</code> itself should call this method when it changes its name.
	 */
	public void rebuildWindowMenus() {
		int max  = windows.size();
		for (int i = 0; i < max; i++) {
			ViewFrame source  = (ViewFrame) windows.elementAt(i);
//			rebuildWindowMenu( source.getWindowMenu());
			source.rebuildWindowMenu(windows);
		}
	}




	/**
	 *  A WindowListener, which allows other windows to react to another window being closed.
	 *
	 * @param  e  A window closing event. Used to remove the window from the windows list.
	 */
	@Override
	public void windowClosed(WindowEvent e) {
		windows.removeElement(e.getWindow());
		if (windows.isEmpty() && exitOnWindowsClosed) {
			endProgram();
		}
		rebuildWindowMenus();
	}


	/**
	 * loops over the list of windows the <code>TreeViewApp</code> has
	 * collected through the WindowListener interface. just closes the window;
	 * other bookkeeping stuff is done by the <code>windowClosed</code>
	 * method.
	 */
	public void closeAllWindows() {
		Enumeration e  = windows.elements();
		while (e.hasMoreElements()) {
			ViewFrame f  = (ViewFrame) e.nextElement();
			f.closeWindow();
		}
	}

	public ViewFrame [] getWindows() {
		ViewFrame [] frames = new ViewFrame[windows.size()];
		int i = 0;
		Enumeration e  = windows.elements();
		while (e.hasMoreElements()) {
			frames[i++] = (ViewFrame) e.nextElement();
		}
		return frames;
	}
	
	/** Stores the globalconfig, closes all windows, and then exits. */
	protected abstract void endProgram();


	@Override
	public void windowActivated(WindowEvent e) {
		//nothing
	}


	@Override
	public void windowClosing(WindowEvent e) {
		//nothing
	}


	@Override
	public void windowDeactivated(WindowEvent e) {
		//nothing
	}


	@Override
	public void windowDeiconified(WindowEvent e) {
		//nothing
	}


	@Override
	public void windowIconified(WindowEvent e) {
		//nothing
	}


	/**
	 *  Get a per-user file in which to store global config info.
	 *  Read the code to find out exactly how it guesses.
	 *
	 * @return    A system-specific guess at a global config file name.
	 */
	public static String globalConfigName() {
		// must find and construct the properties object...
		String dir   = System.getProperty("user.home");
		;
		String fsep  = System.getProperty("file.separator");
		;
		String os    = System.getProperty("os.name");
		;
		String file;
		if (os.indexOf("Mac") >= 0) {
			file = "JavaTreeView Config";
		} else if (fsep.equals("/")) {
			file = ".javaTreeViewXmlrc";
		} else if (fsep.equals("\\")) {
			file = "jtview.xml";
		} else {
			System.out.println("Could not determine sys type! using name jtview.cfg");
			file = "jtview.xml";
		}
		return dir + fsep + file;
	}

	/**
	 * @param exitOnWindowsClosed the exitOnWindowsClosed to set
	 */
	public void setExitOnWindowsClosed(boolean exitOnWindowsClosed) {
		this.exitOnWindowsClosed = exitOnWindowsClosed;
	}

	public Preferences getPrefs() {
		return prefs;
	}
}

