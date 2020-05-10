/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

import java.io.File;
import java.util.prefs.Preferences;

import util.StringRes;

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

	private Preferences node = null;

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
	public CDTFileSet(final Preferences configNode) {

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
		// node = new DummyConfigNode("FileSet");
		node = Preferences.userRoot().node(StringRes.pnode_TVFrame)
				.node("CDTFileSet");
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

		return node.get("cdt", ".cdt");
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

		node.put("cdt", string);
	}

	// the following concerns types which you can open as...
	public static final int AUTO_STYLE = 0;
	public static final int CLASSIC_STYLE = 1;
	public static final int KMEANS_STYLE = 2;
	public static final int LINKED_STYLE = 3;

}
