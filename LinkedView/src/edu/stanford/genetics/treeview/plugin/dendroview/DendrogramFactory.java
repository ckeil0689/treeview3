/*
 * Created on Jul 1, 2005
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

/**
 * @author aloksaldanha
 * 
 * 
 */
public class DendrogramFactory extends PluginFactory {

	// presets must be set before static initializer.
	private static ColorPresets colorPresets = new ColorPresets();
	private final ColorPresetEditor cpresetEditor;
//	private JFrame cpresetFrame = null;

	static {
		PluginManager.registerPlugin(new DendrogramFactory());
	}

	public DendrogramFactory() {

		super();
		cpresetEditor = new ColorPresetEditor(colorPresets);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.PluginFactory#getName()
	 */
	@Override
	public String getPluginName() {

		return "Dendrogram";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.PluginFactory
	 * #createPlugin(edu.stanford.genetics.treeview.ConfigNode)
	 */
	@Override
	public MainPanel restorePlugin(final ConfigNode node,
			final TreeViewFrame viewFrame) {

		final DendroView dendroView = new DendroView(node, viewFrame);
		dendroView.setName("Dendrogram");
		return dendroView;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.PluginFactory
	 * #setGlobalNode(edu.stanford.genetics.treeview.ConfigNode)
	 */
	@Override
	public void setGlobalNode(final ConfigNode node) {

		super.setGlobalNode(node);
		colorPresets.bindConfig(node.fetchOrCreate("ColorPresets"));

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
		globalMenu.addMenuItem("Dendrogram Color Presets...");
//		, new ActionListener() {
//
//					@Override
//					public void actionPerformed(final ActionEvent actionEvent) {
//
//						if (cpresetFrame == null) {
//							cpresetFrame = new JFrame(
//									"Dendrogram Color Presets");
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
		globalMenu.setMnemonic(KeyEvent.VK_D);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.PluginFactory#setGlobalNode(
	 * edu.stanford.genetics.treeview.ConfigNode)
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
