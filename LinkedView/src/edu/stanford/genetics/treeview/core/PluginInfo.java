/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.core;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.PluginFactory;

/**
 * Prints info about the installed modules Currently, this is a very boring
 * class.
 *
 * @author aloksaldanha
 *
 */
public class PluginInfo extends JPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = -1205246332280997485L;

	public PluginInfo(final PluginFactory[] plugins) {
		super();
		if (plugins == null || plugins.length == 0) {
			add(new JLabel("No Plugins Found"));
		} else {
			for (final PluginFactory plugin : plugins) {
				add(new JLabel(plugin.getPluginName()));
			}
		}
	}

}
