/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package gui;

import gui.general.PluginManager;
import gui.matrix.DendroPanel;
import gui.window.TreeViewFrame;
import gui.window.ViewFrame;
import gui.window.menubar.TreeviewMenuBarI;
import preferences.ColorPresets;

import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

/**
 * @author aloksaldanha
 *
 *
 */
public class DendrogramFactory extends PluginFactory {

	// presets must be set before static initializer.
	private static ColorPresets colorPresets = new ColorPresets();
	// private JFrame cpresetFrame = null;

	static {
		PluginManager.registerPlugin(new DendrogramFactory());
	}

	public DendrogramFactory() {

		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.PluginFactory#getName()
	 */
	@Override
	public String getPluginName() {

		return "Dendrogram";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.PluginFactory
	 * #createPlugin(treeview.ConfigNode)
	 */
	@Override
	public DendroPanel restorePlugin(final Preferences node,
			final TreeViewFrame viewFrame) {

		final DendroView dendroView = new DendroView(node, viewFrame);
		dendroView.setName("Dendrogram");
		return dendroView;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treeview.PluginFactory
	 * #setGlobalNode(treeview.ConfigNode)
	 */
	@Override
	public void setGlobalNode(final Preferences node) {
		super.setGlobalNode(node);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * 
	 * @see treeview
	 * .PluginFactory#addPluginConfig(java.awt.Menu)
	 */
	@Override
	public void addPluginConfig(final TreeviewMenuBarI globalMenu,
			final ViewFrame frame) {

		super.addPluginConfig(globalMenu, frame);
		globalMenu.addMenuItem("Dendrogram Color Presets...");
		// , new ActionListener() {
		//
		// @Override
		// public void actionPerformed(final ActionEvent actionEvent) {
		//
		// if (cpresetFrame == null) {
		// cpresetFrame = new JFrame(
		// "Dendrogram Color Presets");
		// final SettingsPanelHolder holder = new SettingsPanelHolder(
		// cpresetFrame, frame.getApp()
		// .getGlobalConfig().getRoot());
		// holder.addSettingsPanel(cpresetEditor);
		// cpresetFrame.getContentPane().add(holder);
		// }
		// cpresetFrame.pack();
		// cpresetFrame.setVisible(true);
		// }
		// });
		globalMenu.setMnemonic(KeyEvent.VK_D);
	}

	/**
	 * mechanism by which Dendroview can access the presets.
	 *
	 * @return color presets for dendrogram view
	 */
	public static ColorPresets getColorPresets() {

		return colorPresets;
	}

	@Override
	public boolean configurePlugin(final Preferences node,
			final ViewFrame viewFrame) {

		return true;
	}

}
