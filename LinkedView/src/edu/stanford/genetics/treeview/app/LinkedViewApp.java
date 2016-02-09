/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.app;

import java.io.File;
//import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import Controllers.TVController;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.MainProgramArgs;
import edu.stanford.genetics.treeview.TreeViewApp;
import edu.stanford.genetics.treeview.TreeViewFrame;
//import edu.stanford.genetics.treeview.Util;
//import edu.stanford.genetics.treeview.core.PluginManager;
import edu.stanford.genetics.treeview.model.TVModel;

/**
 * Main class of LinkedView application. Mostly manages windows, and
 * communication between windows, as well as communication between them.
 *
 * There are two differences between this class and the TreeViewApp - which
 * <code>ViewFrame</code> they use. <code>LinkedViewApp</code> uses
 * <code>LinkedViewFrame</code>.
 *
 * - LinkedViewApp scans for plugins explicitly, TreeViewApp doesn't anymore
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.34 $ $Date: 2010-05-02 13:55:11 $
 */
public class LinkedViewApp extends TreeViewApp {

	private MainProgramArgs args;
	private URL codeBase = null;

	/** Constructor for the LinkedViewApp object */
	// "edu.stanford.genetics.treeview.plugin.scatterview.ScatterplotFactory"
	// "edu.stanford.genetics.treeview.plugin.treeanno.GeneAnnoFactory",
	// "edu.stanford.genetics.treeview.plugin.treeanno.ArrayAnnoFactory"
	// "edu.stanford.genetics.treeview.plugin.karyoview.KaryoscopeFactory"
	public LinkedViewApp() {

		/* load configurations in parent class */
		super();
	}

	/**
	 * Creates a new TreeViewFrame object and makes its JFrame visible.
	 */
	@Override
	public void start() {

		openNew().getAppFrame().setVisible(true);
	}

	/**
	 * Constructor for the TreeViewApp object takes configuration from the
	 * passed in XmlConfig.
	 *
	 * @param Preferences
	 *            The preferences data for this instance of TreeView.
	 */
	public LinkedViewApp(final Preferences configurations) {

		super(configurations, false);
	}

	/**
	 * @deprecated No features are used as plugins anymore. DendroView is now a
	 *             native package rather than a plugin.
	 */
	private void scanForPlugins() {

		// final URL fileURL = getCodeBase();
		// String dir = Util.URLtoFilePath(fileURL.getPath() + "/plugins");
		// File[] files = PluginManager.getPluginManager().readdir(dir);
		// if (files == null) {
		// LogBuffer.println("Directory " + dir + " returned null");
		// final File f_currdir = new File(".");
		// try {
		// dir = f_currdir.getCanonicalPath() + File.separator + "plugins"
		// + File.separator;
		// LogBuffer.println("failing over to " + dir);
		// files = PluginManager.getPluginManager().readdir(dir);
		// if (files != null) {
		// setCodeBase(f_currdir.toURI().toURL());
		// }
		// } catch (final IOException e1) {
		// // this might happen when the dir is bad.
		// LogBuffer.logException(e1);
		// LogBuffer.println("Directory for plugin probably bad: " + dir);
		// }
		// }
		// if (files == null || files.length == 0) {
		// LogBuffer.println("Directory " + dir + " contains no plugins");
		//
		// } else {
		// PluginManager.getPluginManager().loadPlugins(files, false);
		// }
		// PluginManager.getPluginManager().pluginAssignConfigNodes(
		// getGlobalConfig().node("Plugins"));
	}

	/* inherit description */
	@Override
	public TreeViewFrame openNew() {

		/* Setup MVC (Model, View, and Controller) */
		// final LinkedViewFrame tvFrame = new LinkedViewFrame(this);
		final DataModel model = new TVModel();
		final TreeViewFrame tvFrame = new TreeViewFrame(this);
		// tvFrame.addWindowListener(this);
		new TVController(tvFrame, model);

		return tvFrame;
	}

	/* inherit description */
	@Override
	public TreeViewFrame openNew(final FileSet fileSet) throws LoadException {

		/* Setup Model, View, and Controller */
		// final LinkedViewFrame tvFrame = new LinkedViewFrame(this);
		final TreeViewFrame tvFrame = new TreeViewFrame(this);
		final DataModel model = new TVModel();
		// new TVController(tvFrame, model).loadFileSet(fileSet);
		final TVController tvController = new TVController(tvFrame, model);
		tvController.getDataInfoAndLoad(fileSet, false);

		tvFrame.setLoaded(true);

		// tvFrame.addWindowListener(this);
		return tvFrame;
	}

	/**
	 * Method that sets up the initial View
	 *
	 * @param astring
	 */
	protected void standardStartup(final String astring[]) {

		args = new MainProgramArgs(astring);
		final String sFilePath = args.getFilePath();

		// setup toplevel
		if (sFilePath != null) {
			final String frameType = args.getFrameType();
			final String exportType = args.getExportType();

			FileSet fileSet;
			if (sFilePath.startsWith("http://")) {
				fileSet = new FileSet(sFilePath, "");

			} else {
				final File file = new File(sFilePath);
				fileSet = new FileSet(file.getName(), file.getParent()
						+ File.separator);
			}
			fileSet.setStyle(frameType);

			try {
				System.out.println("StandardStartup LOAD attempted.");
				final TreeViewFrame tvFrame = openNew(fileSet);
				// tvFrame.setVisible(true);
				// tvFrame.load(fileSet);
				if (exportType != null) {
					try {
						attemptExport(exportType, tvFrame);

					} catch (final ExportException e) {
						System.err.println(e.getMessage());
						e.printStackTrace(System.err);
					}
					tvFrame.getAppFrame().setVisible(false);
					endProgram();
					return;
				}
			} catch (final LoadException e) {
				e.printStackTrace();
			}
		} else {
			if (args.getExportType() == null) {
				start();
			} else {
				System.err.println("Must specify file/url to load "
						+ "(using -r) when specifying export with -x");
			}
		}
	}

	/**
	 * @deprecated
	 * @param exportType
	 * @param tvFrame
	 * @return
	 * @throws ExportException
	 */
	private boolean attemptExport(final String exportType,
			final TreeViewFrame tvFrame) throws ExportException {

		// for (final MainPanel mainPanel : tvFrame.getMainPanels()) {
		// if (exportType.equalsIgnoreCase(mainPanel.getName())) {
		// mainPanel.export(args);
		// return true;
		// }
		// if (exportType.equalsIgnoreCase(tvFrame.getRunning().getName())) {
		// tvFrame.getRunning().export(args);
		// return true;
		// }
		// }
		System.err.println("Error exporting, could not find plugin of type "
				+ exportType);
		return false;
	}

	/**
	 * Main method for TreeView application.
	 *
	 * Usage: java -jar treeview.jar -r <my cdt> -t
	 * [auto|classic|kmeans|linked].
	 *
	 * uses auto by default.
	 *
	 * @param astring
	 *            Standard argument string.
	 */
	public static void main(final String astring[]) {

		final LinkedViewApp statView = new LinkedViewApp();
		// statView.dealWithRegistration();

		// setup toplevel
		statView.standardStartup(astring);
	}

	private void setCodeBase(final URL url) {

		codeBase = url;
	}

	/**
	 * sometimes the location of the jar is not the location where the plugins
	 * and coordiates can be found. This is particularly the case with mac os
	 * X.I have added detection code in scanForPlugins that detects this and
	 * updates the codebase so that the coordinates settings will be done
	 * correctly.
	 */
	@Override
	public URL getCodeBase() {

		if (codeBase != null)
			return codeBase;
		try {
			// from http://weblogs.java.net/blog/ljnelson/archive/2004/09/
			// cheap_hack_i_re.html
			URL location;
			final String classLocation = LinkedViewApp.class.getName().replace(
					'.', '/')
					+ ".class";

			final ClassLoader loader = LinkedViewApp.class.getClassLoader();
			if (loader == null) {
				location = ClassLoader.getSystemResource(classLocation);

			} else {
				location = loader.getResource(classLocation);
			}

			String token = null;
			if (location != null && "jar".equals(location.getProtocol())) {
				String urlString = location.toString();
				if (urlString != null) {
					final int lastBangIndex = urlString.lastIndexOf("!");
					if (lastBangIndex >= 0) {
						urlString = urlString.substring("jar:".length(),
								lastBangIndex);
						if (urlString != null) {
							final int lastSlashIndex = urlString
									.lastIndexOf("/");
							if (lastSlashIndex >= 0) {
								token = urlString.substring(0, lastSlashIndex);
							}
						}
					}
				}
			}

			if (token == null) {
				return (new File(".")).toURI().toURL();
			}
			return new URL(token);

		} catch (final MalformedURLException e) {
			LogBuffer.logException(e);
			JOptionPane.showMessageDialog(null, e);
			return null;
		}
	}

	@Override
	protected void endProgram() {

		if (getGlobalConfig() != null) {
			try {
				getGlobalConfig().flush();

			} catch (final BackingStoreException e) {
				e.printStackTrace();
			}
		}
		// closeAllWindows();
		System.exit(0);
	}
}
