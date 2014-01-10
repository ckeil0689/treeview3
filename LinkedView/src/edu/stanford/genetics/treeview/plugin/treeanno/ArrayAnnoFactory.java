/*
 * Created on Dec 17, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.plugin.treeanno;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.PluginFactory;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.core.PluginManager;

public class ArrayAnnoFactory extends PluginFactory {
	static {
		PluginManager.registerPlugin(new ArrayAnnoFactory());
	}

	public ArrayAnnoFactory() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.PluginFactory#getName()
	 */
	@Override
	public String getPluginName() {
		return "ArrayTreeAnno";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.stanford.genetics.treeview.PluginFactory#createPlugin(edu.stanford
	 * .genetics.treeview.ConfigNode)
	 */
	@Override
	public MainPanel restorePlugin(final ConfigNode node,
			final ViewFrame viewFrame) {
		if (viewFrame.getDataModel().aidFound() == false) {
			JOptionPane.showMessageDialog(viewFrame, new JTextArea(
					"DataModel does not have array tree"));
			return null;
		} else {
			// make sure the annotation columns are there...
			final HeaderInfo info = viewFrame.getDataModel().getAtrHeaderInfo();
			info.addName("NAME", info.getNumNames());
			info.addName("ANNOTATION", info.getNumNames());

			// restore and return panel
			final TreeAnnoPanel panel = new TreeAnnoPanel(viewFrame, node);
			panel.setName(getPluginName());
			return panel;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.stanford.genetics.treeview.PluginFactory#configurePlugin(edu.stanford
	 * .genetics.treeview.ConfigNode, edu.stanford.genetics.treeview.ViewFrame)
	 */
	@Override
	public boolean configurePlugin(final ConfigNode node,
			final ViewFrame viewFrame) {
		node.setAttribute("tree_type", TreeAnnoPanel.ARRAY_TREE,
				TreeAnnoPanel.DEFAULT_TYPE);
		return true;
	}
}
