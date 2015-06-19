/*
 * Created on Dec 17, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.plugin.dendroview;

import java.util.prefs.Preferences;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import edu.stanford.genetics.treeview.DendroPanel;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.PluginFactory;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.core.PluginManager;

public class AlignmentFactory extends PluginFactory {

	private HeaderInfo rowHI;

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
	public DendroPanel restorePlugin(final Preferences node,
			final TreeViewFrame viewFrame) {

		// if (node.getAttribute("headerName", null) == null) {
		// return null;
		// } else {
		// final CharDendroView charPanel = new CharDendroView(node, viewFrame);
		// charPanel.setName(getPluginName());
		// return charPanel;
		// }

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
	public boolean configurePlugin(final Preferences node,
			final ViewFrame viewFrame) {

		if (rowHI.getIndex("ALN") < 0) {
			JOptionPane.showMessageDialog(viewFrame.getAppFrame(),
					new JTextArea("Cannot find aligned sequence.\n"
							+ "Please put aligned sequence in column "
							+ "titled \"ALN\"."));
			return false;
		}
		
		node.put("headerName", "ALN");
		return true;
	}

	public void setRowHeaderInfo(final HeaderInfo rowHI) {

		this.rowHI = rowHI;
	}

}
