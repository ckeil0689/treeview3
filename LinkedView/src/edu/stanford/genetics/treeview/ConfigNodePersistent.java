/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

import java.util.prefs.Preferences;

/**
 * Defines an interface by which objects can be bound to ConfigNodes.
 */
public interface ConfigNodePersistent {
	
	/**
	 * Retrieve a reference to a class's configNode object.
	 * @return the configNode instance.
	 */
	public Preferences getConfigNode();
	/**
	 *  An instantiated object of a class which implements ConfigNodePersistent
	 *  can restore its state (member variables) to what was saved in the
	 *  relevant configNode. This method ensures that all either the stored
	 *  state is fully reset or at least sensible default values are used. 
	 */
	public void requestStoredState();
	
	/**
	 * Stores the selected information of the current state of an object to
	 * a Preferences node. Keys are dependent on the class which implements
	 * ConfigNodePersistent.
	 */
	public void storeState();

	/**
	 * If a configuration node is needed to store preferences, use this method.
	 * It accepts a parent node as parameter so the new node can be added to the
	 * exisiting hierarchy of Preferences nodes. The node will be a child of the
	 * supplied parent node.
	 */
	public void setConfigNode(Preferences parentNode);
}
