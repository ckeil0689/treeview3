/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: FileSet.java,v $
 * $Revision: 1.15 $
 * $Date: 2008-06-11 01:58:57 $
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

/**
 * Encapsulates a set of files corresponding to a typical hierarchical cluster
 * analysis. Such things are always based upon a cdt or pcl file.
 * <p>
 * The following attributes are meaningful to the FileSet:
 * <ul>
 * <li>dir: The directory of the fileset</li>
 * <li>root: The root of the fileset</li>
 * <li>cdt: The extension for the generalized cdt file</li>
 * <li>atr: The extension for the atr</li>
 * <li>jtv: The extension for the jtv</li>
 * </ul>
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.15 $ $Date: 2008-06-11 01:58:57 $
 * 
 *          Note: Should be flexible enough to have a url base instead of dir
 *          (1/16/2003)
 */
public class FileSet {

	// the following concerns types which you can open as...
	public static final int AUTO_STYLE = 0;
	public static final int CLASSIC_STYLE = 1;
	public static final int KMEANS_STYLE = 2;
	public static final int LINKED_STYLE = 3;
	private static final String validStyles = "auto|classic|kmeans|linked";
	private static final String[] validStylesArray = { "Auto", "Classic",
			"Kmeans", "Linked" };

	// the following concerns whether quoted strings are parsed
	public static final int PARSE_QUOTED = 1;

	private Preferences node = null;
	List<FileSetListener> fileSetListeners = new ArrayList<FileSetListener>();

	/**
	 * Checks if a file's location has changed.
	 * 
	 * @return
	 */
	public boolean hasMoved() {

		if (isUrl()) {
			return false;

		} else {
			try {
				final File f = new File(getCdt());
				return !(f.exists());

			} catch (final Exception e) {
				LogBuffer.println("Exception occurred when checking "
						+ "whether a FileSet has moved: " + e.getMessage());
			}

			return true;
		}
	}

	/**
	 * Constructor for the FileSet object
	 * 
	 * @param configNode
	 *            ConfigNode to base this fileset on.
	 */
	public FileSet(final Preferences configNode) {

		node = configNode;
	}

	/**
	 * Make fileset based upon unrooted DummyConfigNode with the specified
	 * values
	 * 
	 * @param string1
	 *            name of cdt which this fileset is based on
	 * @param string2
	 *            directory to find file in.
	 */
	public FileSet(final String cdt, final String dir) {

		node = Preferences.userRoot().node(this.getClass().getName());
		setCdt(cdt);
		setDir(dir);
	}

	public FileSet(final String dir) {

		node = Preferences.userRoot().node(this.getClass().getName());
		setDir(dir);
	}

	/**
	 * Gets the configNode attribute of the FileSet object
	 * 
	 * @return The configNode value
	 */
	public Preferences getConfigNode() {

		return node;
	}

	/**
	 * Copies state from another fileset
	 * 
	 * @param fileSet
	 *            FileSet to copy state from
	 */
	public void copyState(final FileSet fileSet) {

		setRoot(fileSet.getRoot());
		setDir(fileSet.getDir());
		setExt(fileSet.getExt());
		setName(fileSet.getName());
		setStyle(fileSet.getStyle());
	}

	/**
	 * @return String representation of fileset
	 */
	@Override
	public String toString() {

		return getCdt();
	}

	/**
	 * Determines equality by looking at the cdt base alone.
	 * 
	 * @param fileSet
	 *            FileSet to compare to
	 * @return true if equal
	 */
	public boolean equals(final FileSet fileSet) {

		return getCdt().equals(fileSet.getCdt());
	}

	/**
	 * @return The complete path of the atr file
	 */
	public String getAtr() {

		return getDir() + getRoot() + node.get("atr", ".atr");
	}

	/**
	 * @return The complete path of the cdt file
	 */
	public String getCdt() {

		return getDir() + getRoot() + getExt();
	}

	/**
	 * @return The directory in which the files of the fileset are found
	 */
	public String getDir() {

		return node.get("dir", "");
	}

	/**
	 * @return The complete path of the gtr file
	 */
	public String getGtr() {

		return getDir() + getRoot() + node.get("gtr", ".gtr");
	}

	/**
	 * @return The complete path of the jtv file
	 */
	public String getJtv() {

		return getDir() + getRoot() + node.get("jtv", ".jtv");
	}

	/**
	 * @return The root of the fileset, i.e. "test" if the fileset is based on
	 *         "test.cdt".
	 */
	public String getRoot() {

		return node.get("root", "");
	}

	/**
	 * @return The extension associated with the base of the fileset (i.e. "cdt"
	 *         for one based on "test.cdt", "pcl" for one based on "test.pcl"
	 */
	public String getExt() {

		return node.get("cdt", ".cdt");
	}

	/**
	 * @return The logical name of the fileset
	 */
	public String getName() {

		return node.get("name", "No name");
	}

	/**
	 * Sets the base of the FileSet object. Parses out extension, root
	 * 
	 * @param string1
	 *            Name of base of the FileSet
	 */
	public void setCdt(final String string1) {

		if (string1 != null) {
			setRoot(string1.substring(0, string1.length() - 4));
			setExt(string1.substring(string1.length() - 4, string1.length()));
		}
	}

	/**
	 * Sets the root of the FileSet object. i.e. the filename without
	 * extendsion.
	 * 
	 * @param string
	 *            The new root value
	 */
	public void setRoot(final String string) {

		node.put("root", string);
	}

	/**
	 * Sets the dir in which this fileset can be found
	 * 
	 * @param string
	 *            The new dir value
	 */
	public void setDir(final String string) {

		node.put("dir", string);
	}

	/**
	 * Sets the extension associated with the base of the fileset.
	 * 
	 * @param string
	 *            The new ext value
	 */
	public void setExt(final String string) {

		node.put("cdt", string);
	}

	/**
	 * @return The logical name of the fileset
	 */
	public void setName(final String string) {

		node.put("name", string);
	}

	/**
	 * Used to display gene clusters for knn clustering
	 */
	public String getKgg() {

		String filename = getRoot();
		final int postfix = filename.lastIndexOf("_K");

		if (filename.indexOf("_G", postfix) == -1) {
			return "";
		}

		final int arrayid = filename.indexOf("_A", postfix);
		if (arrayid != -1) {
			filename = filename.substring(0, arrayid);
		}

		return getDir() + filename + node.get("kgg", ".kgg");
	}

	/**
	 * Used to display array clusters for knn clustering
	 */
	public String getKag() {

		String filename = getRoot();
		final int postfix = filename.lastIndexOf("_K");
		final int arrayid = filename.indexOf("_A");
		if (arrayid == -1) {
			return "";
		}

		final int geneid = filename.indexOf("_G", postfix);
		if (geneid != -1) {
			filename = filename.substring(0, geneid)
					+ filename.substring(arrayid);
		}

		return getDir() + filename + node.get("kag", ".kag");
	}

	/**
	 * returns string array of valid style names
	 */
	public static String[] getStyles() {

		return validStylesArray;
	}

	/**
	 * 
	 * @param i
	 *            integer code for style
	 * @return string representing style, or null if code not found.
	 */
	public static String getStyleByIndex(final int i) {

		switch (i) {

		case AUTO_STYLE:
			return "auto";

		case CLASSIC_STYLE:
			return "classic";

		case KMEANS_STYLE:
			return "kmeans";

		case LINKED_STYLE:
			return "linked";

		default:
			return null;
		}
	}

	/**
	 * 
	 * @param name
	 *            string representation of style, one of validStyles
	 * @return int representing style, or -1 if style not found.
	 */
	public static int getStyleByName(final String name) {

		if (name.equalsIgnoreCase("auto")) {
			return AUTO_STYLE;

		} else if (name.equalsIgnoreCase("classic")) {
			return CLASSIC_STYLE;

		} else if (name.equalsIgnoreCase("kmeans")) {
			return KMEANS_STYLE;

		} else if (name.equalsIgnoreCase("linked")) {
			return LINKED_STYLE;

		} else {
			JOptionPane.showMessageDialog(null, "Error: Invalid Style " + name
					+ ". Valid styles " + validStyles);
			return -1;
		}
	}

	/**
	 * 
	 * @return does this fileset have a specified style?
	 */
	public boolean hasStyle() {

		boolean contains = false;

		String[] keys;
		try {
			keys = node.keys();

			for (int i = 0; i < keys.length; i++) {

				if (keys[i].equalsIgnoreCase("style")) {

					contains = true;
					break;
				}
			}

			return contains;

		} catch (final BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return contains;
		}
	}

	public int getStyle() {

		return node.getInt("style", AUTO_STYLE);
	}

	public void setStyle(final int newStyle) {

		node.putInt("style", newStyle);
	}

	public void setStyle(final String newStyle) {

		if (newStyle == null) {
			JOptionPane.showMessageDialog(null, "Error: Invalid Style null. "
					+ "Valid styles " + validStyles);

		} else {
			final int style = getStyleByName(newStyle);
			if (style != -1) {
				setStyle(style);
			}
		}
	}

	public void setParseQuotedStrings(final boolean parseQuote) {

		if (parseQuote) {
			node.putInt("quotes", 1);

		} else {
			node.putInt("quotes", 0);
		}
	}

	public boolean getParseQuotedStrings() {

		return (node.getInt("quotes", PARSE_QUOTED) == 1);
	}

	public void addFileSetListener(final FileSetListener listener) {

		fileSetListeners.add(listener);
	}

	public void clearFileSetListeners() {

		fileSetListeners.clear();
	}

	public void notifyMoved() {

		for (final FileSetListener listener : fileSetListeners) {
			listener.onFileSetMoved(this);
		}
	}

	public boolean isUrl() {

		return getDir().startsWith("http");
	}

}
