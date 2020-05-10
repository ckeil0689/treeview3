/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

import javax.swing.JMenu;

/**
 * implementing objects are expected to be subclasses of component. The purpose
 * of this class is to provide an interface for LinkedView, whereby different
 * views can be added to a tabbed panel. This is meant to eventually become a
 * plugin interface.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.12 $ $Date: 2010-05-02 13:33:30 $
 */
public interface DendroPanel {

	public void addViewMenus(JMenu menu);

	public void addClusterMenus(JMenu menu);

	// public void addSearchMenus(JMenu menu);

	/**
	 *
	 * @return name suitable for displaying in tab
	 */
	public String getName();

}
