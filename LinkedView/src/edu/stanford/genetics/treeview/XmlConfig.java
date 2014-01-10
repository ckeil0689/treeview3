/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: XmlConfig.java,v $
 * $Revision: 1.20 $
 * $Date: 2010-05-02 13:57:28 $
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
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JOptionPane;

import net.n3.nanoxml.IXMLElement;
import net.n3.nanoxml.IXMLParser;
import net.n3.nanoxml.IXMLReader;
import net.n3.nanoxml.StdXMLReader;
import net.n3.nanoxml.XMLElement;
import net.n3.nanoxml.XMLException;
import net.n3.nanoxml.XMLParserFactory;
import net.n3.nanoxml.XMLWriter;

/**
 * This is a generic class for maintaining a configuration registry for
 * documents. The root element is managed by this class, and configuration
 * should be stored in children of the root.
 * 
 * The class is actually implemented as wrapper around XMLElement that is
 * associated with a file, and knows how to store itself.
 */
public class XmlConfig {

	private String file = null;
	private java.net.URL url = null;
	private XmlConfigNode root = null;
	private boolean changed = false;

	/**
	 * Construct new configuration information source
	 * 
	 * @param xmlFile
	 *            xml file associated with configuration info
	 */
	public XmlConfig(final String xmlFile, final String tag) {

		file = xmlFile;
		XMLElement el = null;
		boolean fileMissing = false;
		try {
			try {
				try {
					final IXMLParser parser = XMLParserFactory
							.createDefaultXMLParser();
					// IXMLReader reader = StdXMLReader.fileReader(file);
					// fails on pc
					final BufferedReader breader = new BufferedReader(
							new FileReader(file));
					final IXMLReader reader = new StdXMLReader(breader);
					parser.setReader(reader);

					el = (XMLElement) parser.parse();
					if (el == null) {
						el = promptToOverwrite(xmlFile, tag);

					} else {
						LogBuffer.println("created xml config from file "
								+ file);
					}
				} catch (final java.io.FileNotFoundException e) {
					LogBuffer.println("File not found, will try from URL");
					fileMissing = true;
					url = new URL(xmlFile);
					el = getXMLElementFromURL();

				} catch (final java.security.AccessControlException e) {
					LogBuffer.println("AccessControlException, "
							+ "will try from URL");
					url = new URL(xmlFile);
					el = getXMLElementFromURL();
				}
			} catch (final java.net.MalformedURLException e) {
				if (!file.contains(":")) {
					url = new URL("file://" + xmlFile);
					el = getXMLElementFromURL();

				} else if (fileMissing) {
					// well, let's make our own...
					el = makeNewConfig(tag);

				} else {
					System.out.println("Invalid URL");
					System.out.println(e);
				}
			}
		} catch (final XMLException ex) {
			try {
				el = promptToOverwrite(xmlFile, tag);

			} catch (final Exception e) {
				System.out.println("Problem opening window: " + e.toString());
				System.out.println("Error parsing XML code in "
						+ "configuration file");
				System.out.println(file);
				System.out.println("Manually deleting file may fix, "
						+ "but you'll lose settings.");
				System.out.println("error was: " + ex);
				file = null;
				el = new XMLElement(tag);
			}
		} catch (final java.io.FileNotFoundException e) {
			// well, let's make our own...
			el = makeNewConfig(tag);

		} catch (final Exception ex) {
			System.out.println("Unknown Exception");
			System.out.println(ex);
		}
		root = new XmlConfigNode(el);
	}

	private XMLElement promptToOverwrite(final String xmlFile, final String tag) {

		XMLElement el;
		final int response = JOptionPane.showConfirmDialog(null,
				"Problem Parsing Settings " + xmlFile + ", Should I replace?",
				"Replace Faulty Settings File?", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE);

		if (response == JOptionPane.OK_OPTION) {
			el = makeNewConfig(tag);

		} else {
			LogBuffer.println("Using dummy config nodes");
			file = null;
			el = new XMLElement(tag);
		}
		return el;
	}

	private XMLElement makeNewConfig(final String tag) {

		LogBuffer.println("Making new configuration file " + file);
		changed = true;
		return new XMLElement(tag);
	}

	/**
	 * Construct new configuration information source
	 * 
	 * @param xmlUrl
	 *            url from which the text came from, since nanoxml sucks so damn
	 *            hard.
	 */
	public XmlConfig(final java.net.URL xmlUrl, final String tag) {

		url = xmlUrl;
		XMLElement el = null;
		if (url != null) {
			try {
				el = getXMLElementFromURL();

			} catch (final XMLException ex) {
				LogBuffer
						.println("Error parsing XML code in configuration url");
				LogBuffer.println(url.toString());
				ex.printStackTrace();
				url = null;
				el = new XMLElement(ex.toString());

			} catch (final java.security.AccessControlException sec) {
				sec.printStackTrace();
				throw sec;

			} catch (final Exception ex) {
				// el = new XMLElement(ex.toString());
				LogBuffer.println(ex.toString());
				ex.printStackTrace();
			}
		}

		if (el == null) {
			el = new XMLElement(tag);
		}

		root = new XmlConfigNode(el);
	}

	/**
	 * This factors out the common code to open an XMLElement from a URL It is
	 * used by the URL constructor, and also as a fallback by the file
	 * constructor.
	 * 
	 * @return an XMLElement representing the contents of the file
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws XMLException
	 */
	private XMLElement getXMLElementFromURL() throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, XMLException {

		XMLElement el;
		String xmlText = "";
		final Reader st = new InputStreamReader(url.openStream());
		int ch = st.read();

		while (ch != -1) {
			final char[] cbuf = new char[1];
			cbuf[0] = (char) ch;
			xmlText = xmlText + new String(cbuf);
			ch = st.read();
		}

		final IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
		final Reader breader = new StringReader(xmlText);
		final IXMLReader reader = new StdXMLReader(breader);
		parser.setReader(reader);

		el = (XMLElement) parser.parse();
		return el;
	}

	/**
	 * returns node if it exists, otherwise makes a new one.
	 */
	public ConfigNode getNode(final String name) {

		final ConfigNode t = root.fetchFirst(name);

		// just return if exists
		if (t != null) {
			return t;
		}

		// otherwise, create and return
		return root.create(name);
	}

	/**
	 * returns node if it exists, otherwise makes a new one.
	 */
	public ConfigNode getRoot() {

		return root;
	}

	/**
	 * Store current configuration data structure in XML file
	 * 
	 */
	public void store() {

		if (changed == false) {
			LogBuffer.println("Not printing config to file, "
					+ "config is unchanged.");
			return;
		}

		if (file == null) {
			LogBuffer.println("Not printing config to file, file is null.");
			return;
		}

		try {
			final OutputStream os = new FileOutputStream(file);
			final XMLWriter w = new XMLWriter(os);
			w.write(root.root);
			changed = false;
			LogBuffer.println("Successfully stored config file " + file);

		} catch (final Exception e) {
			LogBuffer.logException(e);
			System.out.println("Caught exception " + e);
		}
	}

	/**
	 * Unit test, tries to load arg[0] as an xml file
	 * 
	 * @param args
	 *            Command line arguments
	 */
	public static void main(final String[] args) {

		final XmlConfig c = new XmlConfig(args[0], "TestConfig");
		System.out.println(c);
		c.store();
	}

	@Override
	public String toString() {

		return "XmlConfig object based on file " + file + "\n" + " url " + url
				+ "\n" + root;
	}

	// inner class, used to implement ConfigNode
	private class XmlConfigNode implements ConfigNode {

		IXMLElement root;

		public XmlConfigNode(final IXMLElement e) {

			root = e;
		}

		@Override
		public void store() {

			XmlConfig.this.store();
		}

		@Override
		public ConfigNode create(final String name) {

			if (root == null) {
				LogBuffer.println("Warning: root is null, "
						+ "creating dummy config node");
				return new DummyConfigNode(name);
			}

			final XMLElement kid = new XMLElement(name);
			root.addChild(kid);
			XmlConfig.this.changed = true;
			return new XmlConfigNode(kid);
		}

		@Override
		public ConfigNode[] fetch(final String name) {

			final Vector kids = root.getChildrenNamed(name);
			final ConfigNode[] ret = new XmlConfigNode[kids.size()];
			for (int i = 0; i < kids.size(); i++) {

				ret[i] = new XmlConfigNode((XMLElement) kids.elementAt(i));
			}
			return ret;
		}

		@Override
		public ConfigNode fetchFirst(final String string) {

			if (root == null) {
				return null;
			}

			final IXMLElement kid = root.getFirstChildNamed(string);
			if (kid == null) {
				return null;
			}

			return new XmlConfigNode(kid);
		}

		@Override
		public ConfigNode fetchOrCreate(final String string) {

			final ConfigNode t = fetchFirst(string);

			// just return if exists
			if (t != null) {
				return t;
			}

			// otherwise, create and return
			return create(string);
		}

		@Override
		public boolean equals(final Object cn) {

			return (((XmlConfigNode) cn).root == root);
		}

		@Override
		public void remove(final ConfigNode configNode) {

			root.removeChild(((XmlConfigNode) configNode).root);
			XmlConfig.this.changed = true;
		}

		@Override
		public void removeAll(final String string) {

			final ConfigNode[] ret = fetch(string);
			for (int i = 0; i < ret.length; i++) {

				remove(ret[i]);
			}
		}

		@Override
		public void setLast(final ConfigNode configNode) {

			remove(configNode);
			root.addChild(((XmlConfigNode) configNode).root);
			XmlConfig.this.changed = true;
		}

		/**
		 * determine if a particular attribute is defined for this node.
		 */
		@Override
		public boolean hasAttribute(final String string) {

			return root.hasAttribute(string);
		}

		@Override
		public double getAttribute(final String string, final double d) {

			final Double val = Double.valueOf(root.getAttribute(string,
					Double.toString(d)));
			return val.doubleValue();
		}

		@Override
		public int getAttribute(final String string, final int i) {

			return root.getAttribute(string, i);
		}

		@Override
		public String getAttribute(final String string, final String dval) {

			return root.getAttribute(string, dval);
		}

		@Override
		public void setAttribute(final String att, final double val,
				final double dval) {

			final double cur = getAttribute(att, dval);
			if (cur != val) {
				XmlConfig.this.changed = true;
				root.setAttribute(att, Double.toString(val));
			}
		}

		@Override
		public void setAttribute(final String att, final int val, final int dval) {

			final int cur = getAttribute(att, dval);
			if (cur != val) {
				XmlConfig.this.changed = true;
				root.setAttribute(att, Integer.toString(val));
			}
		}

		@Override
		public void setAttribute(final String att, final String val,
				final String dval) {

			final String cur = getAttribute(att, dval);
			if ((cur == null) || (!cur.equals(val))) {
				XmlConfig.this.changed = true;
				root.setAttribute(att, val);
			}
		}

		@Override
		public String toString() {

			String ret = "Root:" + root.getFullName() + "\n";
			for (final Enumeration e = root.enumerateChildren(); e
					.hasMoreElements();) {
				ret += " " + ((XMLElement) e.nextElement()).getFullName()
						+ "\n";
			}
			return ret;
		}
	}

	/**
	 * This is a non-object-oriented general purpose static method to create a
	 * window listener that will call ConfigNode.store() when the window it is
	 * listening to is closed. There is nothing particular to the XmlConfigNode
	 * class about it, but I can't think of a better place to put it.
	 * 
	 * Wherenever a settings panel which affects the config is closed, we want
	 * those changes to be saved.
	 * 
	 * returns a WindowListener which will store theconfig every time a window
	 * it listens on is closed.
	 * 
	 * @param node
	 *            node to store
	 * @return window listener to attach to windows
	 */
	public static WindowListener getStoreOnWindowClose(final ConfigNode node) {

		// don't share, or you might end up listening to stale old windows...
		// do window listeners keep a pointer to the things they listen to,
		// or is it the other way around?
		// it seems like it's probably the other way around.
		// in which case, it's bad for observable things to stay around
		// for longer than their observers.
		// anyways, the overhead of making a new one is pretty small.
		return new WindowListener() {
			@Override
			public void windowActivated(final WindowEvent e) {
				// nothing...
			}

			@Override
			public void windowClosed(final WindowEvent e) {

				node.store();
			}

			@Override
			public void windowClosing(final WindowEvent e) {
				// nothing...
			}

			@Override
			public void windowDeactivated(final WindowEvent e) {
				// nothing...
			}

			@Override
			public void windowDeiconified(final WindowEvent e) {
				// nothing...
			}

			@Override
			public void windowIconified(final WindowEvent e) {
				// nothing...
			}

			@Override
			public void windowOpened(final WindowEvent e) {
				// nothing...
			}
		};
	}

	/**
	 * change the file that this xml config is backed by and store the config
	 * 
	 * @param file
	 */
	public void setFile(final String file) {

		this.file = file;
		changed = true;
		store();
	}
}
