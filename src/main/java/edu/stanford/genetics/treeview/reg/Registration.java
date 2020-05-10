/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.reg;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.TreeViewApp;

/**
 * @author aloksaldanha
 * 
 *         This class keeps track of the registration information for Java
 *         Treeview It should be bound to the Registration config node of the
 *         global xml config file.
 * 
 */
public class Registration implements ConfigNodePersistent {
	private ConfigNode configNode = null;

	/**
	 * @param node
	 */
	public Registration(final ConfigNode node) {
		bindConfig(node);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.stanford.genetics.treeview.ConfigNodePersistent#bindConfig(edu.stanford
	 * .genetics.treeview.ConfigNode)
	 */
	@Override
	public void bindConfig(final ConfigNode configNode) {
		this.configNode = configNode;
	}

	/**
	 * @param versionTag
	 * @return returns entry corresponding to version tag. Returns null if no
	 *         corresponding Entry.
	 */
	public Entry getEntry(final String versionTag) {
		for (int i = 0; i < getNumEntries(); i++) {
			final Entry entry = getEntry(i);
			if (entry.getVersionTag().equals(versionTag))
				return entry;
		}
		return null;
	}

	/**
	 * @param versionTag
	 * @return creates entry corresponding to version tag.
	 */
	public Entry createEntry(final String versionTag) {
		final Entry oldEntry = getLastEntry();
		final ConfigNode newNode = configNode.create("Entry");
		final Entry newEntry = new Entry(newNode);
		newEntry.initialize(oldEntry);
		return newEntry;
	}

	/**
	 * @return Number of entries in Registration confignode.
	 */
	public int getNumEntries() {
		final ConfigNode[] entries = configNode.fetch("Entry");
		return entries.length;
	}

	/**
	 * @param i
	 * @return i'th entry in ConfigNode.
	 */
	public Entry getEntry(final int i) {
		final ConfigNode[] entries = configNode.fetch("Entry");
		return new Entry(entries[i]);
	}

	/**
	 * @return current entry
	 */
	public Entry getCurrentEntry() {
		final String versionTag = TreeViewApp.getVersionTag();
		return getEntry(versionTag);
	}

	/**
	 * @return
	 */
	public Entry getLastEntry() {
		final int numEntries = getNumEntries();
		if (numEntries > 0) {
			return getEntry(numEntries - 1);
		} else {
			return null;
		}
	}
}
