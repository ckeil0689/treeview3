/*
 * Created on Dec 17, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.plugin.dendroview;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.DendroPanel;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.PluginFactory;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.core.PluginManager;

public class AlignmentFactory extends PluginFactory {
	static {
		PluginManager.registerPlugin(new AlignmentFactory());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.PluginFactory#getName()
	 */
	@Override
	public String getPluginName() {
		return "Alignment";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.stanford.genetics.treeview.PluginFactory#createPlugin(edu.stanford
	 * .genetics.treeview.ConfigNode)
	 */
	@Override
	public DendroPanel restorePlugin(final ConfigNode node,
			final TreeViewFrame viewFrame) {
//		if (node.getAttribute("headerName", null) == null) {
//			return null;
//		} else {
//			final CharDendroView charPanel = new CharDendroView(node, viewFrame);
//			charPanel.setName(getPluginName());
//			return charPanel;
//		}
		
		return null;
	}

	public AlignmentFactory() {
		super();
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
		
		if (viewFrame.getDataModel().getGeneHeaderInfo().getIndex("ALN") >= 0) {
			node.setAttribute("headerName", "ALN", null);
			return true;
			
		} else {
			JOptionPane.showMessageDialog(viewFrame.getAppFrame(),
							new JTextArea("Cannot find aligned sequence.\n" +
									"Please put aligned sequence in column " +
									"titled \"ALN\"."));
			return false;
		}
	}

}
