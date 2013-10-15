/*
 * Created on Dec 17, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.plugin.dendroview;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import edu.stanford.genetics.treeview.*;
import edu.stanford.genetics.treeview.core.PluginManager;

public class AlignmentFactory extends PluginFactory {
	static {
		PluginManager.registerPlugin(new AlignmentFactory());
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#getName()
	 */
	@Override
	public String getPluginName() {
		return "Alignment";
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#createPlugin(edu.stanford.genetics.treeview.ConfigNode)
	 */
	@Override
	public MainPanel restorePlugin(ConfigNode node, ViewFrame viewFrame) {
		if (node.getAttribute("headerName", null) == null) {
			return null;
		} else {
			CharDendroView charPanel = new CharDendroView(viewFrame, node);
			charPanel.setName(getPluginName());
			return charPanel;
		}
	}
	
	public AlignmentFactory() {
		super();
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#configurePlugin(edu.stanford.genetics.treeview.ConfigNode, edu.stanford.genetics.treeview.ViewFrame)
	 */
	@Override
	public boolean configurePlugin(ConfigNode node, ViewFrame viewFrame) {
		if (viewFrame.getDataModel().getGeneHeaderInfo().getIndex("ALN") >= 0) {
			node.setAttribute("headerName", "ALN", null);
			return true;
		} else {
			JOptionPane.showMessageDialog(viewFrame, new JTextArea("Cannot find aligned sequence.\nPlease put aligned sequence in column titled \"ALN\"."));
			return false;
		}
	}
	
}
