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

public class CDTFileSet extends FileSet {
	private ConfigNode node = null;

	@Override
	public boolean hasMoved() {
		if (isUrl())
			return false;
		try {
			final File f = new File(getCdt());
			return !(f.exists());
		} catch (final Exception e) {
		}
		return true;
	}

	/**
	 * Constructor for the FileSet object
	 * 
	 * @param configNode
	 *            ConfigNode to base this fileset on.
	 */
	public CDTFileSet(final ConfigNode configNode) {

		super(configNode);
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
	public CDTFileSet(final String cdt, final String dir) {

		super(dir);
		node = new DummyConfigNode("FileSet");
		setCdt(cdt);
	}

	/**
	 * Copies state from another fileset
	 * 
	 * @param fileSet
	 *            FileSet to copy state from
	 */
	public void copyState(final CDTFileSet fileSet) {
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
	public boolean equals(final CDTFileSet fileSet) {
		return getCdt().equals(fileSet.getCdt());
	}

	/**
	 * @return The complete path of the cdt file
	 */
	@Override
	public String getCdt() {
		return getDir() + getRoot() + getExt();
	}

	/**
	 * @return The extension associated with the base of the fileset (i.e. "cdt"
	 *         for one based on "test.cdt", "pcl" for one based on "test.pcl"
	 */
	@Override
	public String getExt() {
		return node.getAttribute("cdt", ".cdt");
	}

	/**
	 * Sets the base of the FileSet object. Parses out extension, root
	 * 
	 * @param string1
	 *            Name of base of the FileSet
	 */
	@Override
	public void setCdt(final String string1) {
		if (string1 != null) {
			setRoot(string1.substring(0, string1.length() - 4));
			setExt(string1.substring(string1.length() - 4, string1.length()));
		}
	}

	/**
	 * Sets the extension associated with the base of the fileset.
	 * 
	 * @param string
	 *            The new ext value
	 */
	@Override
	public void setExt(final String string) {
		node.setAttribute("cdt", string, ".cdt");
	}

	// the following concerns types which you can open as...
	public static final int AUTO_STYLE = 0;
	public static final int CLASSIC_STYLE = 1;
	public static final int KMEANS_STYLE = 2;
	public static final int LINKED_STYLE = 3;

}
