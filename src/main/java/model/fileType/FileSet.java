/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package model.fileType;

import org.apache.commons.io.FilenameUtils;
import util.LogBuffer;
import util.StringRes;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Encapsulates a set of files corresponding to a typical hierarchical components.cluster
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
	public static final String TRV = ".trv";
	
	// Sensible defaults for FileSet properties
	public static final String DEFAULT_ROOT = "generic-tv-file";
	public static final String DEFAULT_EXT = ".txt";
	public static final String DEFAULT_DIR = System.getProperty("user.home") + File.separator;
	public static final String DEFAULT_JTV = ".jtv";
	public static final String DEFAULT_ATR = ".atr";
	public static final String DEFAULT_GTR = ".gtr";
	public static final String DEFAULT_KGG = ".kgg";
	public static final String DEFAULT_KAG = ".kag";
	
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
		}
		
		try {
			final File f = new File(getCdt());
			return !(f.exists());
		} 
		catch (final Exception e) {
			LogBuffer.println("Exception occurred when checking "
					+ "whether a FileSet has moved: " + e.getMessage());
		}

		return true;
	}

	/**
	 * Constructor for the FileSet object
	 *
	 * @param fs - another FileSet to be copied.
	 */
	public FileSet(FileSet fs) {
		
		this.node = Preferences.userRoot().node(StringRes.pnode_globalMain)
			.node("FileSet");
		copyState(fs);
	}
	
	/**
	 * Constructor for the FileSet object
	 *
	 * @param configNode
	 *            ConfigNode to base this fileset on.
	 */
	public FileSet(final Preferences configNode) {

		this.node = configNode;
	}

	/**
	 * Make FileSet based upon unrooted DummyConfigNode with the specified
	 * values
	 *
	 * @param fullFileName
	 *            name of file which this FileSet is based on, including extension.
	 *            For example "myfile.txt"
	 * @param dir
	 *            directory where file is located.
	 */
	public FileSet(final String fullFileName, final String dir) {

		this.node = Preferences.userRoot().node(StringRes.pnode_globalMain)
				.node("FileSet");
		setCdt(fullFileName);
		setDir(dir);
	}

	public FileSet(final String dir) {

		this.node = Preferences.userRoot().node(StringRes.pnode_globalMain)
				.node("FileSet");
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
	public boolean equalsFileSet(final FileSet fileSet) {

		return getCdt().equals(fileSet.getCdt());
	}

	/**
	 * @return The complete path of the atr file
	 */
	public String getAtr() {

		return getDir() + getRoot() + node.get("atr", DEFAULT_ATR);
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

		return node.get("dir", DEFAULT_DIR);
	}

	/**
	 * @return The complete path of the gtr file
	 */
	public String getGtr() {

		return getDir() + getRoot() + node.get("gtr", DEFAULT_GTR);
	}

	/**
	 * @return The complete path of the jtv file
	 */
	public String getJtv() {

		return getDir() + getRoot() + node.get("jtv", DEFAULT_JTV);
	}

	/**
	 * @return The root of the fileset, i.e. "test" if the fileset is based on
	 *         "test.cdt".
	 */
	public String getRoot() {

		return node.get("root", DEFAULT_ROOT);
	}

	/**
	 * @return The extension associated with the base of the fileset (i.e. ".cdt"
	 *         for one based on "test.cdt", ".pcl" for one based on "test.pcl"
	 */
	public String getExt() {
		
		String storedExt = node.get("ext", DEFAULT_EXT);
		String finalExt;
		
		// FileUtils removed dot from file extension - ensure it is present!
		if(!storedExt.substring(0, 1).equals(".")) {
			finalExt = "." + storedExt;
			setExt(finalExt);
		} else {
			finalExt = storedExt;
		}

		return finalExt;
	}

	/**
	 * @return The logical name of the fileset
	 */
	public String getName() {

		return getRoot() + getExt();
	}

	/**
	 * Sets the base of the FileSet object. Parses out extension, root
	 *
	 * @param fileSetBasename
	 *            Name of base of the FileSet, including the extension. 
	 *            For example "myfile.txt"
	 */
	public void setCdt(final String fileSetBasename) {

		if (fileSetBasename != null) {
			setRoot(FilenameUtils.removeExtension(fileSetBasename));
			setExt(FilenameUtils.getExtension(fileSetBasename));
		}
	}

	/**
	 * Sets the root of the FileSet object. i.e. the filename without
	 * extension.
	 *
	 * @param newRoot
	 *            The new root value
	 */
	public void setRoot(final String newRoot) {

		LogBuffer.println("Setting root " + newRoot + " on FileSet " 
		+ this.toString());
		node.put("root", newRoot);
	}

	/**
	 * Sets the dir in which this fileset can be found
	 *
	 * @param string
	 *            The new dir value
	 */
	public void setDir(final String newDir) {

		node.put("dir", newDir);
	}

	/**
	 * Sets the extension associated with the base of the FileSet.
	 *
	 * @param ext - the new extension value
	 */
	public void setExt(final String ext) {
		
		node.put("ext", ext);
	}

	/**
	 * Used to display gene clusters for knn clustering
	 */
	public String getKgg() {

		String filename = getRoot();
		final int postfix = filename.lastIndexOf("_K");

		if (filename.indexOf("_G", postfix) == -1)
			return "";

		final int arrayid = filename.indexOf("_A", postfix);
		if (arrayid != -1) {
			filename = filename.substring(0, arrayid);
		}

		return getDir() + filename + node.get("kgg", DEFAULT_KGG);
	}

	/**
	 * Used to display array clusters for knn clustering
	 */
	public String getKag() {

		String filename = getRoot();
		final int postfix = filename.lastIndexOf("_K");
		final int arrayid = filename.indexOf("_A");
		if (arrayid == -1)
			return "";

		final int geneid = filename.indexOf("_G", postfix);
		if (geneid != -1) {
			filename = filename.substring(0, geneid)
					+ filename.substring(arrayid);
		}

		return getDir() + filename + node.get("kag", DEFAULT_KAG);
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

		if (name.equalsIgnoreCase("auto"))
			return AUTO_STYLE;
		else if (name.equalsIgnoreCase("classic"))
			return CLASSIC_STYLE;
		else if (name.equalsIgnoreCase("kmeans"))
			return KMEANS_STYLE;
		else if (name.equalsIgnoreCase("linked"))
			return LINKED_STYLE;
		else {
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

			for (final String key : keys) {

				if (key.equalsIgnoreCase("style")) {

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

	public void setNode(Preferences node) {
		this.node = node;
	}

}
