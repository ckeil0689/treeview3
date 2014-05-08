///* BEGIN_HEADER                                              Java TreeView
// *
// * $Author: alokito $
// * $RCSfile: DummyConfigNode.java,v $
// * $Revision: 1.9 $
// * $Date: 2005-12-05 05:27:53 $
// * $Name:  $
// *
// * This file is part of Java TreeView
// * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
// *
// * This software is provided under the GNU GPL Version 2. In particular, 
// *
// * 1) If you modify a source file, make a comment in it containing your name and the date.
// * 2) If you distribute a modified version, you must do it under the GPL 2.
// * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
// *
// * A full copy of the license can be found in gpl.txt or online at
// * http://www.gnu.org/licenses/gpl.txt
// *
// * END_HEADER 
// */
//package edu.stanford.genetics.treeview;
//
//import java.util.Hashtable;
//import java.util.Vector;
//
//// vector
//
///**
// * This interface defines a ConfigNode without persistence...
// * 
// * @author Alok Saldanha <alok@genome.stanford.edu>
// * @version $Revision: 1.9 $ $Date: 2005-12-05 05:27:53 $
// */
//public class DummyConfigNode implements ConfigNode {
//
//	private final Vector<ConfigNode> kids;
//	private final Hashtable<String, Object> attr;
//	private final String name;
//	private static final String NULLVALUE = "Null String";
//
//	/**
//	 * create and return a subnode which has the indicated name
//	 * 
//	 * @param tname
//	 *            name of subnode to create
//	 * @return newly created node
//	 */
//	@Override
//	public ConfigNode create(final String tname) {
//
//		final DummyConfigNode child = new DummyConfigNode(tname);
//
//		kids.addElement(child);
//		return child;
//	}
//
//	/**
//	 * Constructor for the DummyConfigNode object
//	 * 
//	 * @param tname
//	 *            name of the (parentless) node to create
//	 */
//	public DummyConfigNode(final String tname) {
//
//		super();
//		name = tname;
//		kids = new Vector<ConfigNode>();
//		attr = new Hashtable<String, Object>();
//	}
//
//	/**
//	 * fetch all nodes with the name
//	 * 
//	 * @param byname
//	 *            type of nodes to search for
//	 * @return array of matching nodes
//	 */
//	@Override
//	public ConfigNode[] fetch(final String byname) {
//
//		if (byname == null) {
//			return null;
//		}
//
//		int matching = 0;
//
//		for (int i = 0; i < kids.size(); i++) {
//			if (byname.equals(((DummyConfigNode) kids.elementAt(i)).name)) {
//				matching++;
//			}
//		}
//
//		final ConfigNode[] ret = new DummyConfigNode[matching];
//		matching = 0;
//
//		for (int i = 0; i < kids.size(); i++) {
//			if (byname.equals(((DummyConfigNode) kids.elementAt(i)).name)) {
//				ret[matching] = kids.elementAt(i);
//				matching++;
//			}
//		}
//		return ret;
//	}
//
//	/**
//	 * fetch first node by name
//	 * 
//	 * @param byname
//	 *            type of node to search for
//	 * @return first matching node
//	 */
//	@Override
//	public ConfigNode fetchFirst(final String byname) {
//		for (int i = 0; i < kids.size(); i++) {
//			if (byname.equals(((DummyConfigNode) kids.elementAt(i)).name)) {
//				return kids.elementAt(i);
//			}
//		}
//		return null;
//	}
//
//	/**
//	 * remove particular subnode
//	 * 
//	 * @param configNode
//	 *            node to remove
//	 */
//	@Override
//	public void remove(final ConfigNode configNode) {
//		kids.removeElement(configNode);
//	}
//
//	/**
//	 * remove all subnodes by name
//	 * 
//	 * @param byname
//	 *            type of node to remove
//	 */
//	@Override
//	public void removeAll(final String byname) {
//		for (int i = kids.size() - 1; i >= 0; i--) {
//			if (byname.equals(((DummyConfigNode) kids.elementAt(i)).name)) {
//				kids.removeElementAt(i);
//			}
//		}
//	}
//
//	/**
//	 * set attribute to be last in list
//	 * 
//	 * @param configNode
//	 *            The new last value
//	 */
//	@Override
//	public void setLast(final ConfigNode configNode) {
//		kids.removeElement(configNode);
//		kids.addElement(configNode);
//	}
//
//	/**
//	 * get double attribute
//	 * 
//	 * @param string
//	 *            name of attribude
//	 * @param d
//	 *            a default value to return
//	 * @return The attribute value
//	 */
//	@Override
//	public double getAttribute(final String string, final double d) {
//		final Object o = attr.get(string);
//		if ((o == null) || (o == NULLVALUE)) {
//			return d;
//		}
//		return ((Double) o).doubleValue();
//	}
//
//	/**
//	 * determine if a particular attribute is defined for this node.
//	 */
//	@Override
//	public boolean hasAttribute(final String string) {
//		final Object o = attr.get(string);
//		if (o == null) {
//			return false;
//		} else {
//			return true;
//		}
//	}
//
//	/**
//	 * get int attribute
//	 * 
//	 * @param string
//	 *            name of attribue
//	 * @param i
//	 *            default int value
//	 * @return The attribute value
//	 */
//	@Override
//	public int getAttribute(final String string, final int i) {
//		final Object o = attr.get(string);
//		if ((o == null) || (o == NULLVALUE)) {
//			return i;
//		}
//		return ((Integer) o).intValue();
//	}
//
//	/**
//	 * get String attribute
//	 * 
//	 * @param string1
//	 *            attribute to get
//	 * @param string2
//	 *            Default value
//	 * @return The attribute value
//	 */
//	@Override
//	public String getAttribute(final String string1, final String string2) {
//		final Object o = attr.get(string1);
//		if (o == null) {
//			return string2;
//		}
//		if (o == NULLVALUE) {
//			return null;
//		}
//		return (String) o;
//	}
//
//	/**
//	 * set double attribute
//	 * 
//	 * @param att
//	 *            name of attribute
//	 * @param val
//	 *            The new attribute value
//	 * @param dval
//	 *            The default value
//	 */
//	@Override
//	public void setAttribute(final String att, final double val,
//			final double dval) {
//		attr.put(att, new Double(val));
//	}
//
//	/**
//	 * set int attribute
//	 * 
//	 * @param att
//	 *            name of attribute
//	 * @param val
//	 *            The new attribute value
//	 * @param dval
//	 *            The default value
//	 */
//	@Override
//	public void setAttribute(final String att, final int val, final int dval) {
//		attr.put(att, new Integer(val));
//	}
//
//	/**
//	 * set String attribute
//	 * 
//	 * @param att
//	 *            name of attribute
//	 * @param val
//	 *            The new attribute value
//	 * @param dval
//	 *            The default value
//	 */
//	@Override
//	public void setAttribute(final String att, String val, final String dval) {
//		if (att == null)
//			LogBuffer.println("attibute to DummyConfig was null!");
//		if (val == null) {
//			val = NULLVALUE;
//		}
//		attr.put(att, val);
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see
//	 * edu.stanford.genetics.treeview.ConfigNode#fetchOrCreate(java.lang.String)
//	 */
//	@Override
//	public ConfigNode fetchOrCreate(final String string) {
//		final ConfigNode cand = fetchFirst(string);
//		if (cand == null)
//			return create(string);
//		else
//			return cand;
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see edu.stanford.genetics.treeview.ConfigNode#store()
//	 */
//	@Override
//	public void store() {
//		// null op, since dummy.
//		// System.err.println("Trying to save dummy config")
//	}
// }
