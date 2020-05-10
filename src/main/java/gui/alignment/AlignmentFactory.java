/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package gui.alignment;

import gui.PluginFactory;
import gui.general.PluginManager;
import gui.matrix.DendroPanel;
import gui.window.TreeViewFrame;
import gui.window.ViewFrame;
import model.data.labels.LabelInfo;

import javax.swing.*;
import java.util.prefs.Preferences;

public class AlignmentFactory extends PluginFactory {

	private LabelInfo rowLabelI;

	static {
		PluginManager.registerPlugin(new AlignmentFactory());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.PluginFactory#getName()
	 */
	@Override
	public String getPluginName() {

		return "Alignment";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * treeview.PluginFactory#createPlugin(edu.stanford
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
	 * treeview.PluginFactory#configurePlugin(edu.stanford
	 * .genetics.treeview.ConfigNode, treeview.ViewFrame)
	 */
	@Override
	public boolean configurePlugin(final Preferences node,
			final ViewFrame viewFrame) {

		if (rowLabelI.getIndex("ALN") < 0) {
			JOptionPane.showMessageDialog(viewFrame.getAppFrame(),
					new JTextArea("Cannot find aligned sequence.\n"
							+ "Please put aligned sequence in column "
							+ "titled \"ALN\"."));
			return false;
		}

		node.put("headerName", "ALN");
		return true;
	}

	public void setRowLabelInfo(final LabelInfo rowLabelI) {

		this.rowLabelI = rowLabelI;
	}

}
