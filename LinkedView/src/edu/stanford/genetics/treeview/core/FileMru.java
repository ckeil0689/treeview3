/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.core;

import java.util.Observable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import Utilities.StringRes;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;

/*
 *  Decompiled by Mocha from FileMru.class
 */
/*
 *  Originally compiled from FileMru.java
 */

/**
 * This class encapsulates an xml-based most recently used list of files
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.1 $ $Date: 2006-09-25 22:02:02 $
 */
public class FileMru extends Observable implements ConfigNodePersistent {

	private Preferences configNode;

	/**
	 * Binds FileMru to a ConfigNode
	 *
	 * @param configNode
	 *            Node to be bound to
	 */
	@Override
	public synchronized void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node(StringRes.pnode_FileMRU);

		} else {
			LogBuffer.println("Could not find or create FileMRU Preferences"
					+ "node because parentNode was null.");
		}
		setChanged();
	}

	/**
	 * Create subnode of current confignode
	 *
	 * @return Newly created subnode
	 */
	public synchronized Preferences createSubNode() {

		setChanged();
		final int subNodeIndex = getRootChildrenNodes().length + 1;
		LogBuffer.println("Creating subNode File" + subNodeIndex);
		return configNode.node("File" + subNodeIndex);
	}

	/**
	 * Gets the ConfigNode of the ith file
	 *
	 * @param i
	 *            Index of file to get node for
	 * @return The corresponding ConfigNdoe
	 */
	public Preferences getConfig(final int i) {

		final String[] childrenNodes = getRootChildrenNodes();

		if ((i < childrenNodes.length) && (i >= 0)) {
			return configNode.node(childrenNodes[i]);
		}
		
		return null;
	}

	/**
	 * Gets the configs of all files
	 *
	 * @return Array of all ConfigNodes
	 */
	public Preferences[] getConfigs() {

		final String[] childrenNodes = getRootChildrenNodes();
		final Preferences[] children = new Preferences[childrenNodes.length];

		for (int i = 0; i < children.length; i++) {

			children[i] = configNode.node(childrenNodes[i]);
		}
		return children;
	}

	/**
	 * Gets names of all recently used files
	 *
	 * @return String [] of file names for display
	 */
	public String[] getFileNames() {

		final String[] childrenNodes = getRootChildrenNodes();

		final String astring[] = new String[childrenNodes.length];

		for (int i = 0; i < childrenNodes.length; i++) {
			astring[i] = configNode.node(childrenNodes[i]).get("root", "")
					+ configNode.node(childrenNodes[i]).get("cdt", "");
		}
		return astring;
	}

	/**
	 * returns dir of most recently used file or null
	 *
	 * @return The Most Recent Dir or null
	 */
	public String getMostRecentDir() {

		// final ConfigNode aconfigNode[] = root.fetch("File");
		final String[] childrenNodes = getRootChildrenNodes();

		if (childrenNodes.length == 0)
			return null;

		final Preferences childNode = configNode
				.node(childrenNodes[childrenNodes.length - 1]);
		return childNode.get("dir", null);
	}

	public boolean getParseQuotedStrings() {

		return (configNode.getInt("quotes", FileSet.PARSE_QUOTED) == 1);
	}

	public void setParseQuotedStrings(final boolean parse) {

		if (parse) {
			configNode.putInt("quotes", 1);
		} else {
			configNode.putInt("quotes", 0);
		}
	}

	public int getStyle() {

		return configNode.getInt("style", FileSet.LINKED_STYLE);
	}

	public void setStyle(final int style) {

		configNode.putInt("style", style);
	}

	/**
	 * Delete the nth file in the list
	 *
	 * @param i
	 *            The the index of the file to delete.
	 */
	public synchronized void removeFile(final int i) {

		final String[] childrenNodes = getRootChildrenNodes();
		try {
			// final Preferences node = configNode.node(childrenNodes[i]);

			configNode.node(childrenNodes[i]).removeNode();

		} catch (final BackingStoreException e) {
			e.printStackTrace();
			LogBuffer.println("BackingStoreException when trying to remove"
					+ " a file in FileMRU: " + e.getMessage());
		}
		setChanged();
	}

	/**
	 * Removes particular FileSet from Mru
	 *
	 * @param fileSet
	 *            FileSet to remove
	 */
	public synchronized void removeFileSet(final FileSet fileSet) {

		configNode.remove(fileSet.getConfigNode().name());
		setChanged();
	}

	/**
	 * Sets configNode to be last in list
	 *
	 * @param configNode
	 *            Node to move to end
	 */
	public synchronized void setLast(final Preferences fileSetNode) {

		configNode.put("last_node", fileSetNode.name());
		setChanged();
	}

	/**
	 * Returns the last FileSet that was opened by the user.
	 *
	 * @return The last open FileSet
	 */
	public synchronized FileSet getLast() {

		final String lastNode = configNode.get("last_node", "no_last");

		if (lastNode.equalsIgnoreCase("no_last")) {
			return null;
		}
		
		try {
			if(configNode.node(lastNode).keys().length == 0) {
				return null;
			}
		} catch (BackingStoreException e) {
			LogBuffer.logException(e);
			return null;
		}

		return new FileSet(configNode.node(lastNode));
	}

	/**
	 * Must notify explicitly when a managed fileset is modified (perhaps should
	 * pass modifications through Mru?
	 */
	public void notifyFileSetModified() {

		setChanged();
	}

	/**
	 * Move FileSet to end of list
	 *
	 * @param fileSet
	 *            FileSet to move
	 */
	public synchronized void setLast(final FileSet fileSet) {

		setLast(fileSet.getConfigNode());
	}

	/**
	 * add a fileset if it's not already in the list. Or, if it is in the list,
	 * create a fileset with the correct confignode.
	 *
	 * @return the fileset corresponding to the correct config node
	 */
	public synchronized FileSet addUnique(final FileSet inSet) {

		// check existing file nodes...
		final Preferences[] aconfigNode = getConfigs();
		for (final Preferences element : aconfigNode) {
			final FileSet fileSet2 = new FileSet(element);
			if (fileSet2.equalsFileSet(inSet)) {
				LogBuffer.println("Found Existing node in MRU list for "
						+ inSet);
				fileSet2.copyState(inSet);
				return fileSet2;
			}
		}

		final Preferences configNode = createSubNode();
		final FileSet fileSet3 = new FileSet(configNode);
		fileSet3.copyState(inSet);
		LogBuffer.println("Creating new fileset " + fileSet3);
		return fileSet3;
	}

	/**
	 * Delete all but the last i files from the list
	 *
	 * @param i
	 *            The number of files to delete
	 */
	public synchronized void trimToLength(final int i) {

		// final ConfigNode aconfigNode[] = root.fetch("File");
		final String[] childrenNodes = getRootChildrenNodes();

		final int j = childrenNodes.length - i;
		for (int k = 0; k < j; k++) {
			configNode.remove(childrenNodes[k]);
		}
		setChanged();
	}

	public synchronized void removeMoved() {

		final Preferences[] nodes = getConfigs();

		for (int i = nodes.length; i > 0; i--) {

			final FileSet fileSet = new FileSet(nodes[i - 1]);
			if (fileSet.hasMoved()) {
				LogBuffer.println("Could not find " + fileSet.getCdt() + ", "
						+ "removing from mru...");
				removeFile(i - 1);
				setChanged();
			}
		}
	}

	/**
	 * removes any duplicates of this fileset in the mru list. it will keep the
	 * _last_ in the list.
	 *
	 * @param fileSet
	 */
	public void removeDuplicates(final FileSet inSet) {

		final Preferences[] nodes = getConfigs();
		int keeper = -1;
		for (int i = nodes.length; i > 0; i--) {
			final FileSet fileSet = new FileSet(nodes[i - 1]);
			if (fileSet.equalsFileSet(inSet)) {
				if (keeper != -1) {
					// delete node, keep the keeper
					LogBuffer.println("Found duplicate of " + fileSet.getCdt()
							+ ", removing from mru...");
					removeFile(i - 1);
					setChanged();
				} else {
					keeper = i;
				}
			}
		}
	}

	/**
	 * Returns the names of the current children of this class' root node.
	 *
	 * @return
	 */
	public String[] getRootChildrenNodes() {

		String[] childrenNodes;
		try {
			childrenNodes = configNode.childrenNames();
			return childrenNodes;

		} catch (final BackingStoreException e) {
			e.printStackTrace();
			return new String[0];
		}
	}

}
