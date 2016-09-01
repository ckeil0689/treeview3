/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.DendroPanel;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.PluginFactory;
import edu.stanford.genetics.treeview.SettingsPanel;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.core.PluginManager;

public class KnnDendrogramFactory extends PluginFactory {

	// presets must be set before static initializer.
	private static ColorPresets2 colorPresets = new ColorPresets2();
	private final ColorPresetEditor cpresetEditor;
//	private JFrame cpresetFrame = null;

	static {

		PluginManager.registerPlugin(new KnnDendrogramFactory());
	}

	@Override
	public String getPluginName() {

		return "KnnDendrogram";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview
	 * .PluginFactory#createPlugin(edu.stanford.genetics.treeview.ConfigNode)
	 */
	@Override
	public DendroPanel restorePlugin(final Preferences node,
			final TreeViewFrame viewFrame) {

		// DendroView dendroView = new KnnDendroView(
		// (KnnModel) viewFrame.getDataModel(), node, viewFrame);
		final DendroView2 dendroView = new KnnDendroView2(node, viewFrame);
		dendroView.setName(getPluginName());
		return dendroView;
	}

	public KnnDendrogramFactory() {

		super();
		cpresetEditor = new ColorPresetEditor(colorPresets);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview
	 * .PluginFactory#setGlobalNode(edu.stanford.genetics.treeview.ConfigNode)
	 */
	@Override
	public void setGlobalNode(final Preferences node) {

		super.setGlobalNode(node);
//		colorPresets.setConfigNode(node);
//		if (colorPresets.getNumPresets() == 0) {
//			colorPresets.addDefaultPresets();
//		}
		cpresetEditor.synchronizeFrom();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview
	 * .PluginFactory#addPluginConfig(java.awt.Menu)
	 */
	@Override
	public void addPluginConfig(final TreeviewMenuBarI globalMenu,
			final ViewFrame frame) {

		super.addPluginConfig(globalMenu, frame);
		globalMenu.addMenuItem("KnnDendrogram Color Presets...");
//		, new ActionListener() {
//
//					@Override
//					public void actionPerformed(final ActionEvent actionEvent) {
//
//						if (cpresetFrame == null) {
//							cpresetFrame = new JFrame(
//									"KnnDendrogram Color Presets");
//							final SettingsPanelHolder holder = new SettingsPanelHolder(
//									cpresetFrame, frame.getApp()
//											.getGlobalConfig().getRoot());
//							holder.addSettingsPanel(cpresetEditor);
//							cpresetFrame.getContentPane().add(holder);
//						}
//						cpresetFrame.pack();
//						cpresetFrame.setVisible(true);
//					}
//				});
		globalMenu.setMnemonic(KeyEvent.VK_N);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview
	 * .PluginFactory#setGlobalNode(edu.stanford.genetics.treeview.ConfigNode)
	 */
	public SettingsPanel getPresetEditor() {

		return cpresetEditor;
	}

	/**
	 * mechanism by which Dendroview can access the presets.
	 * 
	 * @return color presets for dendrogram view
	 */
	public static ColorPresets2 getColorPresets() {

		return colorPresets;
	}

	@Override
	public boolean configurePlugin(final Preferences node,
			final ViewFrame viewFrame) {

		return true;
	}
}
