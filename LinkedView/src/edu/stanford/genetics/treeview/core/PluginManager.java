/*
 * Created on Sep 20, 2006
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;

import javax.swing.JOptionPane;

import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.PluginFactory;

public class PluginManager {

	private static String s_pluginclassfile = "tv_plugins.cd";

	/**
	 * holds global plugin manager I hate static variables, but this one looks
	 * necessary.
	 * */
	private static PluginManager pluginManager = new PluginManager();

	/**
	 * holds list of all plugin factories
	 */
	private final Vector<PluginFactory> pluginFactories = 
			new Vector<PluginFactory>();

	public static PluginManager getPluginManager() {

		return pluginManager;
	}

	public File[] readdir(final String s_dir) {

		final File f_dir = new File(s_dir);
		final FileFilter fileFilter = new FileFilter() {

			@Override
			public boolean accept(final File file) {

				return file.getName().endsWith("jar");
			}
		};

		return f_dir.listFiles(fileFilter);
	}

	/*
	 * EFFECTS: Reads the file <s_pluginclassfile> from a jar RETURNS: string
	 * array of all classes to be declared NOTE: This method should probably be
	 * moved into a PluginManager class
	 */
	private static List<String> getClassDeclarations(final JarFile jf)
			throws NullPointerException, IOException {

		ZipEntry ze = null;
		try {
			/*
			 * See if class declarations file exists
			 */
			final Enumeration<JarEntry> e = jf.entries();
			JarEntry je = null;
			String classfile = null;

			for (; e.hasMoreElements();) {

				je = e.nextElement();
				if (je.toString().indexOf(s_pluginclassfile) >= 0) {
					classfile = je.toString();
				}
			}
			ze = jf.getEntry(classfile);

		} catch (final NullPointerException e) {
			LogBuffer.println("JarFile has no tv_plugins.cd" + jf.getName());
			throw e;

		} catch (final RuntimeException e) {
			LogBuffer.println("RunTimeException in PluginManager: "
					+ e.getMessage());
			e.printStackTrace();
		}

		/*
		 * Classfile exists (otherwise exception thrown) Read classes into array
		 * list
		 */
		final BufferedReader br = new BufferedReader(new InputStreamReader(
				jf.getInputStream(ze)));

		final List<String> al = new ArrayList<String>();
		String s = null;

		while ((s = br.readLine()) != null) {
			al.add(s);
		}
		return al;
	}

	public void loadPlugins(final File[] f_jars, final boolean showPopup) {

		String s_loadedPlugins = "";
		String s_notloadedPlugins = "";

		for (final File f_jar : f_jars) {

			try {
				final URL jarURL = new URL("jar:"
						+ f_jar.toURI().toURL().toString() + "!/");

				final boolean b_loadedPlugin = loadPlugin(jarURL);

				if (b_loadedPlugin) {
					s_loadedPlugins += "<li>" + f_jar.getName() + "</li>";
				} else {
					s_loadedPlugins += "<li>" + f_jar.getName() + "*</li>";
				}
			} catch (final NullPointerException e) {
				e.printStackTrace();
				s_notloadedPlugins += "<li>" + f_jar.getName() + "</li>";
				Debug.print(e);
			} catch (final MalformedURLException e) {
				Debug.print(e);
			}
		}
		if (showPopup) {
			JOptionPane.showMessageDialog(null, "<html> Found jar files: <ol>"
					+ s_loadedPlugins
					+ "<br>* Already loaded</ol> <p>Unable to load: <ol>"
					+ s_notloadedPlugins + "</ol></html>");
		}
	}

	public boolean loadPlugin(final URL jarURL) {

		boolean b_loadedPlugin = false;
		try {
			LogBuffer.println("Plugin Jar " + jarURL);
			final JarURLConnection conn = (JarURLConnection) jarURL
					.openConnection();
			final JarFile jarFile = conn.getJarFile();
			final List<String> al_classnames = getClassDeclarations(jarFile);
			for (int j = 0; j < al_classnames.size(); j++) {
				final Class thisClass = getClass();
				final URLClassLoader urlcl = new URLClassLoader(
						new URL[] { jarURL }, thisClass.getClassLoader());
				if (!pluginExists(al_classnames.get(j))) {
					final Class c = urlcl.loadClass(al_classnames.get(j));
					c.newInstance();
					b_loadedPlugin |= true;

				} else {
					b_loadedPlugin |= false;
				}
				urlcl.close();
			}
			jarFile.close();
			
		} catch (final ClassNotFoundException e) {
			LogBuffer.logException(e);

		} catch (final InstantiationException e) {
			LogBuffer.logException(e);
			Debug.print(e);

		} catch (final IllegalAccessException e) {
			LogBuffer.logException(e);
			Debug.print(e);

		} catch (final IOException e) {
			LogBuffer.logException(e);
		}
		return b_loadedPlugin;
	}

	/**
	 *
	 * remember, static methods cannot be overridden.
	 *
	 * @param pf
	 */
	public static void registerPlugin(final PluginFactory pf) {
		LogBuffer.println("Registering Plugin " + pf.getPluginName());
		getPluginManager().pluginFactories.add(pf);
	}

	/**
	 * Assigns corresponding nodes from the global config to the appropriate
	 * plugin factories.
	 *
	 * needs to be called whenever a plugin is loaded or the global confignode
	 * changes.
	 *
	 */
	public void pluginAssignConfigNodes(final Preferences node) {

		final PluginFactory[] plugins = getPluginFactories();

		for (final PluginFactory plugin : plugins) {

			Preferences pluginNode = null;
			// final Preferences pluginPresetsNode = node.node("PluginGlobal");
			String[] pluginPresetsChildrenNodes;
			try {
				pluginPresetsChildrenNodes = node.childrenNames();

				for (final String pluginPresetsChildrenNode : pluginPresetsChildrenNodes) {

					// scan existing pluginPresets for correct name
					if (node.node(pluginPresetsChildrenNode).get("name", "")
							.equals(plugin.getPluginName())) {

						pluginNode = node.node(pluginPresetsChildrenNode);
						break;
					}
				}

				if (pluginNode == null) {
					// no existing presets node for plugin, must create
					pluginNode = node.node(plugin.getPluginName());
					pluginNode.put("name", plugin.getPluginName());
				}

				plugin.setGlobalNode(pluginNode);

			} catch (final BackingStoreException e) {
				e.printStackTrace();
			}
		}
	}

	public PluginFactory[] getPluginFactories() {
		
		final PluginFactory[] ret = new PluginFactory[pluginFactories.size()];
		final Enumeration<PluginFactory> e = pluginFactories.elements();
		int i = 0;
		while (e.hasMoreElements()) {
			ret[i++] = e.nextElement();
		}
		return ret;
	}

	public PluginFactory getPluginFactoryByName(final String name) {
		
		final Enumeration<PluginFactory> e = pluginFactories.elements();
		while (e.hasMoreElements()) {
			final PluginFactory factory = e.nextElement();
			if (name.equals(factory.getPluginName()))
				return factory;
		}
		return null;
	}

	public String[] getPluginNames() {
		final String[] names = new String[pluginFactories.size()];
		for (int i = 0; i < names.length; i++) {
			names[i] = (pluginFactories.elementAt(i))
					.getPluginName();
			LogBuffer.println("Pluginclassname ---- " + names[i]);
		}
		return names;
	}

	/**
	 *
	 * @param i
	 * @return ith plugin, or null if no such plugin.
	 */
	public PluginFactory getPluginFactory(final int i) {
		if ((i < pluginFactories.size()) && (i >= 0))
			return (PluginFactory) pluginFactories.elementAt(i);
		else {
			System.out.println("returns null");
			return null;
		}
	}

	public boolean pluginExists(final String qualified_name) {
		final Enumeration e = pluginFactories.elements();
		String s = null;
		while (e.hasMoreElements()) {
			s = ((PluginFactory) e.nextElement()).toString();
			/*
			 * The toString function on an (Object) always adds some funny
			 * strings to the end of the string, so we have to use "contains".
			 */
			if (s.indexOf(qualified_name) != -1)
				return true;
		}
		return false;
	}
}
