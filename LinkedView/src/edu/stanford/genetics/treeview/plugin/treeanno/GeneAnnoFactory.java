/*
 * Created on Dec 17, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.plugin.treeanno;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import edu.stanford.genetics.treeview.*;
import edu.stanford.genetics.treeview.core.PluginManager;

/**
 * Factory to create instances of the gene tree annotation
 * @author aloksaldanha
 *
 */
public class GeneAnnoFactory extends PluginFactory {
	static {
		PluginManager.registerPlugin(new GeneAnnoFactory());
	}
	public GeneAnnoFactory() {
		super();
	}
		
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#getName()
	 */
public String getPluginName() {
		return "GeneTreeAnno";
	}
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#createPlugin(edu.stanford.genetics.treeview.ConfigNode)
	 */
	public MainPanel restorePlugin(ConfigNode node, ViewFrame viewFrame) {
		if (viewFrame.getDataModel().gidFound() == false) {
			JOptionPane.showMessageDialog(viewFrame, new JTextArea("DataModel does not have a gene tree"));
			return null;
		} else {
			// make sure the annotation columns are there...
			HeaderInfo info = viewFrame.getDataModel().getGtrHeaderInfo();
			info.addName("NAME", info.getNumNames());
			info.addName("ANNOTATION", info.getNumNames());
			// restore and return panel
			TreeAnnoPanel panel = new TreeAnnoPanel(viewFrame, node);
			panel.setName(getPluginName());
			return panel;
		}
	}
	
	public boolean configurePlugin(ConfigNode node, ViewFrame viewFrame) {
		node.setAttribute("tree_type", TreeAnnoPanel.GENE_TREE, TreeAnnoPanel.DEFAULT_TYPE);
		return true;
	}

}
