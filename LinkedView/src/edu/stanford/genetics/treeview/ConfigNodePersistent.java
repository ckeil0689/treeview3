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
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.5 $ $Date: 2004-12-21 03:28:13 $
 */
public interface ConfigNodePersistent {
	// /**
	// * Should bind implementing object to suppled confignode. As it is bound,
	// * the object should change its state information to match that in the
	// * confignode. Furthermore, once bound it should store all its state
	// * information in the confignode, so as to maintain persistence across
	// runs.
	// *
	// * @param configNode
	// * config node to bind to.
	// */
	// public void bindConfig(Preferences configNode);

	/**
	 * If a configuration node is needed to store preferences, use this method.
	 * It accepts a parent node as parameter so the new node can be added to the
	 * exisiting hierarchy of Preferences nodes. The node will be a child of the
	 * supplied parent node.
	 */
	public void setConfigNode(Preferences parentNode);
}
