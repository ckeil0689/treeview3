/*
 * Created on Sep 21, 2006
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.PluginFactory;
import edu.stanford.genetics.treeview.SettingsPanel;
import edu.stanford.genetics.treeview.SettingsPanelHolder;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.core.PluginManager;
import edu.stanford.genetics.treeview.model.KnnModel;

public class KnnDendrogramFactory extends PluginFactory {
	// presets must be set before static initializer.
	private static ColorPresets colorPresets = new ColorPresets();
	private ColorPresetEditor  cpresetEditor;
	private JFrame cpresetFrame = null;
	static {
		PluginManager.registerPlugin(new KnnDendrogramFactory());
	}
	public String getPluginName() {
		return "KnnDendrogram";
	}


	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#createPlugin(edu.stanford.genetics.treeview.ConfigNode)
	 */
	public MainPanel restorePlugin(ConfigNode node, ViewFrame viewFrame) {
		DendroView dendroView = new KnnDendroView((KnnModel) viewFrame.getDataModel(), node, viewFrame);
		dendroView.setName(getPluginName());
		return dendroView;
	}
	public KnnDendrogramFactory() {
		super();
		cpresetEditor = new ColorPresetEditor(colorPresets);
	}
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#setGlobalNode(edu.stanford.genetics.treeview.ConfigNode)
	 */
	public void setGlobalNode(ConfigNode node) {
		super.setGlobalNode(node);
		colorPresets.bindConfig(node.fetchOrCreate("KnnColorPresets"));
		if (colorPresets.getNumPresets() == 0) {
		  colorPresets.addDefaultPresets();
		}
		cpresetEditor.synchronizeFrom();
	}
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#addPluginConfig(java.awt.Menu)
	 */
	public void addPluginConfig(TreeviewMenuBarI globalMenu, final ViewFrame frame) {
		super.addPluginConfig(globalMenu, frame);
		globalMenu.addMenuItem("KnnDendrogram Color Presets...", new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				if (cpresetFrame == null) {
					cpresetFrame = new JFrame("KnnDendrogram Color Presets");
					SettingsPanelHolder holder = new SettingsPanelHolder(cpresetFrame, 
							frame.getApp().getGlobalConfig().getRoot());
					holder.addSettingsPanel(cpresetEditor);
					cpresetFrame.getContentPane().add(holder);
				}
				cpresetFrame.pack();
				cpresetFrame.setVisible(true);
			}
		});
		globalMenu.setMnemonic(KeyEvent.VK_N);
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#setGlobalNode(edu.stanford.genetics.treeview.ConfigNode)
	 */
	public SettingsPanel getPresetEditor () {
		return cpresetEditor;
	}
	/**
	 * mechanism by which Dendroview can access the presets.
	 * @return color presets for dendrogram view
	 */
	public static ColorPresets getColorPresets() {
		return colorPresets;
	}


	public boolean configurePlugin(ConfigNode node, ViewFrame viewFrame) {
		return true;
	}
}
