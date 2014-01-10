/*
 * Created on Sep 22, 2006
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.applet;

import java.applet.Applet;
import java.net.URL;

import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LinkedViewFrame;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.TreeViewApp;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.XmlConfig;

public class AppletApp extends TreeViewApp {
	private final Applet applet;

	public AppletApp(final Applet ap) {
		super(true);
		applet = ap;
	}

	public AppletApp(final Applet ap, final XmlConfig xmlConfig) {
		super(xmlConfig, true);
		applet = ap;
	}

	/* inherit description */
	@Override
	public ViewFrame openNew() {
		// setup toplevel
		final LinkedViewFrame tvFrame = new AppletViewFrame(this, applet);
		tvFrame.addWindowListener(this);
		return tvFrame;
	}

	/* inherit description */
	@Override
	public ViewFrame openNew(final FileSet fileSet) throws LoadException {
		// setup toplevel
		final LinkedViewFrame tvFrame = new AppletViewFrame(this, applet);
		try {
			tvFrame.loadFileSet(fileSet);
			tvFrame.setLoaded(true);
		} catch (final LoadException e) {
			tvFrame.dispose();
			throw e;
		}

		tvFrame.addWindowListener(this);
		return tvFrame;
	}

	/**
	 * same as above, but doesn't open a loading window (damn deadlocks!)
	 */
	@Override
	public ViewFrame openNewNW(final FileSet fileSet) throws LoadException {
		// setup toplevel
		final LinkedViewFrame tvFrame = new AppletViewFrame(this, applet);
		if (fileSet != null) {
			try {
				tvFrame.loadFileSetNW(fileSet);
				tvFrame.setLoaded(true);
			} catch (final LoadException e) {
				tvFrame.dispose();
				throw e;
			}
		}
		tvFrame.addWindowListener(this);
		return tvFrame;
	}

	@Override
	public URL getCodeBase() {
		return applet.getCodeBase();
	}

	@Override
	protected void endProgram() {
		try {
			if (getGlobalConfig() != null) {
				getGlobalConfig().store();
			}
			closeAllWindows();
		} catch (final java.security.AccessControlException e) {
			// we're probably running in a applet here...
		}
	}
}
