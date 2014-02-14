/*
 * Created on Sep 21, 2006
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.event.KeyEvent;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.PluginFactory;
import edu.stanford.genetics.treeview.SettingsPanel;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.core.PluginManager;

public class KnnDendrogramFactory extends PluginFactory {

	// presets must be set before static initializer.
	private static ColorPresets colorPresets = new ColorPresets();
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
	public MainPanel restorePlugin(final ConfigNode node,
			final TreeViewFrame viewFrame) {

		// DendroView dendroView = new KnnDendroView(
		// (KnnModel) viewFrame.getDataModel(), node, viewFrame);
		final DendroView dendroView = new KnnDendroView2(node, viewFrame);
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
	public void setGlobalNode(final ConfigNode node) {

		super.setGlobalNode(node);
		colorPresets.bindConfig(node.fetchOrCreate("KnnColorPresets"));
		if (colorPresets.getNumPresets() == 0) {
			colorPresets.addDefaultPresets();
		}
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
	public static ColorPresets getColorPresets() {

		return colorPresets;
	}

	@Override
	public boolean configurePlugin(final ConfigNode node,
			final ViewFrame viewFrame) {

		return true;
	}
}
