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
 * This is a generic class for maintaining a configuration registry
 * for documents. The root element is managed by this class, and
 * configuration should be stored in children of the root.
 *
 * The class is actually implemented as wrapper around XMLElement that
 * is associated with a file, and knows how to store itself.
 */
public class XmlConfig {
    /**
     * Construct new configuration information source
     * 
     * @param xmlFile xml file associated with configuration info
     */
	 public XmlConfig(String xmlFile, String tag) {
		 file = xmlFile;
		 XMLElement el = null;
		 boolean fileMissing = false;
		 try {
			 try{
				 try {
					 IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
					 // IXMLReader reader = StdXMLReader.fileReader(file); // fails on pc
					 BufferedReader breader = new BufferedReader(new FileReader(file));
					 IXMLReader reader = new StdXMLReader(breader);
					 parser.setReader(reader);
					 el = (XMLElement) parser.parse();
					 if (el == null) {
						 el = promptToOverwrite(xmlFile, tag);
					 } else {
						 LogBuffer.println("created xml config from file " + file);
					 }
				 } catch (java.io.FileNotFoundException e) {
					 LogBuffer.println("File not found, will try from URL");
					 fileMissing = true;
					 url = new URL(xmlFile);
					 el = getXMLElementFromURL();
				 } catch (java.security.AccessControlException e) {
					 LogBuffer.println("AccessControlException, will try from URL");
					 url = new URL(xmlFile);
					 el = getXMLElementFromURL();
				 }
			 } catch (java.net.MalformedURLException e) {
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
		 } catch (XMLException ex) {
			 try {
				 el = promptToOverwrite(xmlFile, tag);
			 } catch (Exception e) {
				 System.out.println("Problem opening window: " + e.toString());
				 System.out.println("Error parsing XML code in configuration file");
				 System.out.println(file);
				 System.out.println("Manually deleting file may fix, but you'll lose settings.");
				 System.out.println("error was: " + ex);
				 file = null;
				 el = new XMLElement(tag);
			 }
		 } catch (java.io.FileNotFoundException e) {
			 // well, let's make our own...
			 el = makeNewConfig(tag);
		 } catch (Exception ex) {
			 System.out.println("Unknown Exception");
			 System.out.println(ex);
		 }
		 root = new XmlConfigNode(el);
	 }
	private XMLElement promptToOverwrite(String xmlFile, String tag) {
		XMLElement el;
		int response = JOptionPane.showConfirmDialog (null, "Problem Parsing Settings " + xmlFile + ", Should I replace?", "Replace Faulty Settings File?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		 if (response == JOptionPane.OK_OPTION) {
			 el = makeNewConfig(tag);
		 } else {
			 LogBuffer.println("Using dummy config nodes");
			 file = null;
			 el = new XMLElement(tag);
		 }
		return el;
	}
	 private XMLElement makeNewConfig(String tag) {
			 LogBuffer.println("Making new configuration file " + file);
			 changed = true;
			 return  new XMLElement(tag);
	 }
	 
    /**
     * Construct new configuration information source
     * 
     * @param xmlUrl url from which the text came from, since nanoxml sucks so damn hard.
     */
	 public XmlConfig(java.net.URL xmlUrl, String tag) {
		 url = xmlUrl;
		 XMLElement el = null;
		 if (url != null) {
			 try {
					el = getXMLElementFromURL();
			 } catch (XMLException ex) {
				 LogBuffer.println("Error parsing XML code in configuration url");
				 LogBuffer.println(url.toString());
				 ex.printStackTrace();
				 url = null;
				 el = new XMLElement(ex.toString());
			 } catch (java.security.AccessControlException sec) {
				 sec.printStackTrace();
				 throw sec;
			 } catch (Exception ex) {
				 //	    el = new XMLElement(ex.toString());
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
	   * This factors out the common code to open an XMLElement from a URL
     * It is used by the URL constructor, and also as a fallback by the file 
     * constructor.
     * 
		 * @return an XMLElement representing the contents of the file
		 * @throws IOException
		 * @throws ClassNotFoundException
		 * @throws InstantiationException
		 * @throws IllegalAccessException
		 * @throws XMLException
		 */
		private XMLElement getXMLElementFromURL() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, XMLException {
			XMLElement el;
			String xmlText     = "";
			Reader st          = new InputStreamReader(url.openStream());
			int ch             = st.read();
			while (ch != -1) {
				char[] cbuf  = new char[1];
				cbuf[0] = (char) ch;
				xmlText = xmlText + new String(cbuf);
				ch = st.read();
			}
 
 IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
 Reader breader = new StringReader(xmlText);
 IXMLReader reader = new StdXMLReader(breader);
 parser.setReader(reader);
 el = (XMLElement) parser.parse();
			return el;
		}

    /**
     * returns node if it exists, otherwise makes a new one.
     */
    public ConfigNode getNode(String name) {
	ConfigNode t =root.fetchFirst(name);
	// just return if exists
	if (t != null) return t;
	//otherwise, create and return
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
			 LogBuffer.println("Not printing config to file, config is unchanged.");
			 return;
		 }
		 if (file == null) {
			 LogBuffer.println("Not printing config to file, file is null.");
			 return;
		 }
		 try {
			 OutputStream os = new FileOutputStream(file);
			 XMLWriter w = new XMLWriter(os);
			 w.write(root.root);
			 changed = false;
			 LogBuffer.println("Successfully stored config file " + file);
		 } catch (Exception e) {
			 LogBuffer.logException(e);
			 System.out.println("Caught exception " + e);
		 }
	 }
    
    /**
     * Unit test, tries to load arg[0] as an xml file
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
	XmlConfig c = new XmlConfig(args[0], "TestConfig");	
	System.out.println(c);
	c.store();
    }
    
    @Override
	public String toString() {
	return "XmlConfig object based on file " + file + "\n" 
	    + " url " + url + "\n" + root;
    }

    // inner class, used to implement ConfigNode
    private class XmlConfigNode implements ConfigNode{
	IXMLElement root;
	public XmlConfigNode(IXMLElement e) {
	    root = e;
	}
	@Override
	public void store() {
		XmlConfig.this.store();
	}
	@Override
	public ConfigNode create(String name) {
	  if (root == null) {
		  LogBuffer.println("Warning: root is null, creating dummy config node");
		  return new DummyConfigNode(name);
	  }
	    XMLElement kid = new XMLElement(name);
	    root.addChild(kid);
	    XmlConfig.this.changed = true;
	    return new XmlConfigNode(kid);
 	}
	@Override
	public ConfigNode[] fetch(String name) {
	    Vector kids = root.getChildrenNamed(name);
	    ConfigNode [] ret = new XmlConfigNode[kids.size()];
	    for (int i = 0; i < kids.size(); i++) {
		ret[i] = new XmlConfigNode((XMLElement) kids.elementAt(i));
	    }
	    return ret;
	}
	
	@Override
	public ConfigNode fetchFirst(String string) {
	  	if (root == null) return null;
	    IXMLElement kid = root.getFirstChildNamed(string);
	    if (kid == null) return null;
	    return new XmlConfigNode(kid);
	}
	@Override
	public ConfigNode fetchOrCreate(String string) {
		ConfigNode t = fetchFirst(string);
		// just return if exists
		if (t != null) return t;
		//otherwise, create and return
		return create(string);
	}

	@Override
	public boolean equals(Object cn) {
	    return (((XmlConfigNode)cn).root == root);
	}
    
	@Override
	public void remove(ConfigNode configNode) {
	    root.removeChild(((XmlConfigNode)configNode).root);
	    XmlConfig.this.changed = true;
	}

	@Override
	public void removeAll(String string) {
	    ConfigNode [] ret = fetch(string);
	    for (int i = 0; i < ret.length; i++) {
		remove(ret[i]);
	    }
	}

	@Override
	public void setLast(ConfigNode configNode) {
	    remove(configNode);
	    root.addChild(((XmlConfigNode) configNode).root);
	    XmlConfig.this.changed = true;
	}
	
	/**
	 * determine if a particular attribute is defined for this node.
	 */
	 @Override
	public boolean hasAttribute(String string) {
	   return root.hasAttribute(string);
	 }

	@Override
	public double getAttribute(String string, double d) {
	    Double val = Double.valueOf(root.getAttribute(string, Double.toString(d)));
	    return val.doubleValue();
	}
	@Override
	public int getAttribute(String string, int i) {
	    return root.getAttribute(string, i);
	}
	@Override
	public String getAttribute(String string, String dval) {
	    return root.getAttribute(string, dval);
	}

	@Override
	public void setAttribute(String att, double val, double dval) {
	    double cur = getAttribute(att, dval);
	    if (cur != val) {
		XmlConfig.this.changed = true;
		root.setAttribute(att, Double.toString(val));
	    }
	}

	@Override
	public void setAttribute(String att, int val, int dval) {
	    int cur = getAttribute(att, dval);
	    if (cur != val) {
		XmlConfig.this.changed = true;
		root.setAttribute(att, Integer.toString(val));
	    }
	}
	@Override
	public void setAttribute(String att, String val, String dval) {
	    String cur = getAttribute(att, dval);
	    if ((cur == null) || (!cur.equals(val))) {
		XmlConfig.this.changed = true;
		root.setAttribute(att, val);
	    }
	}
	@Override
	public String toString() {
	    String ret = "Root:" + root.getFullName() + "\n";
	    for (Enumeration e = root.enumerateChildren(); 
		 e.hasMoreElements(); ) {
		ret += " " + ((XMLElement) e.nextElement()).getFullName() + "\n";
	    }
	    return ret;
	}
    }
    
    private String file = null;
	private java.net.URL url = null;
    private XmlConfigNode root = null;
    private boolean changed = false;
    
    /**
     * This is a non-object-oriented general purpose static method 
     * to create a window listener that will call ConfigNode.store()
     * when the window it is listening to is closed. There is nothing
     * particular to the XmlConfigNode class about it, but I can't think
     * of a better place to put it. 
     * 
     * Wherenever a settings panel which affects the config is closed, we want those changes to be saved.
	 *
	 * returns a WindowListener which will store theconfig every time a window it listens on is closed.
	 * 
     * @param node node to store
     * @return window listener to attach to windows
     */
    public static WindowListener getStoreOnWindowClose(final ConfigNode node) {
		 // don't share, or you might end up listening to stale old windows...
		 // do window listeners keep a pointer to the things they listen to, or is it the other way around?
		 // it seems like it's probably the other way around.
		 // in which case, it's bad for observable things to stay around for longer than their observers.
		 // anyways, the overhead of making a new one is pretty small.
		 return new WindowListener() {
				 @Override
				public void windowActivated(WindowEvent e) {
					 // nothing...
				 }
				 @Override
				public void windowClosed(WindowEvent e) {
					 node.store();
				 }
				 @Override
				public void windowClosing(WindowEvent e) {
					 // nothing...
				 }
				 @Override
				public void windowDeactivated(WindowEvent e) {
					 // nothing...
				 }
				 @Override
				public void windowDeiconified(WindowEvent e) {
					 // nothing...
				 }
				 @Override
				public void windowIconified(WindowEvent e) {
					 // nothing...
				 }
				 @Override
				public void windowOpened(WindowEvent e) {
					 // nothing...
				 }
		 };
    }
    /**
     *  change the file that this xml config is backed by
     *  and store the config
     *  
     * @param file
     */
	public void setFile(String file) {
		this.file = file;
		changed = true;
		store();
	}
}
