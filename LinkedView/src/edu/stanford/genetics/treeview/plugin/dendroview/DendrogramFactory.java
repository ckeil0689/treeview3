/*
 * Created on Jul 1, 2005
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

/**
 * @author aloksaldanha
 *
 *
 */
public class DendrogramFactory extends PluginFactory {
	// presets must be set before static initializer.
	private static ColorPresets colorPresets = new ColorPresets();
	private ColorPresetEditor  cpresetEditor;
	private JFrame cpresetFrame = null;
	static {
		PluginManager.registerPlugin(new DendrogramFactory());
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#getName()
	 */
	@Override
	public String getPluginName() {
		return "Dendrogram";
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#createPlugin(edu.stanford.genetics.treeview.ConfigNode)
	 */
	@Override
	public MainPanel restorePlugin(ConfigNode node, ViewFrame viewFrame) {
//		DendroView dendroView = new DendroView(viewFrame.getDataModel(), node, viewFrame);
		DendroView2 dendroView = new DendroView2(viewFrame.getDataModel(), node, viewFrame);
		dendroView.setName("Dendrogram");
		return dendroView;
	}
	
	
	
	public DendrogramFactory() {
		super();
		cpresetEditor = new ColorPresetEditor(colorPresets);
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#setGlobalNode(edu.stanford.genetics.treeview.ConfigNode)
	 */
	@Override
	public void setGlobalNode(ConfigNode node) {
		super.setGlobalNode(node);
		colorPresets.bindConfig(node.fetchOrCreate("ColorPresets"));
		if (colorPresets.getNumPresets() == 0) {
		  colorPresets.addDefaultPresets();
		}
		cpresetEditor.synchronizeFrom();
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#addPluginConfig(java.awt.Menu)
	 */
	@Override
	public void addPluginConfig(TreeviewMenuBarI globalMenu, final ViewFrame frame) {
		super.addPluginConfig(globalMenu, frame);
		globalMenu.addMenuItem("Dendrogram Color Presets...", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (cpresetFrame == null) {
					cpresetFrame = new JFrame("Dendrogram Color Presets");
					SettingsPanelHolder holder = new SettingsPanelHolder(cpresetFrame, 
							frame.getApp().getGlobalConfig().getRoot());
					holder.addSettingsPanel(cpresetEditor);
					cpresetFrame.getContentPane().add(holder);
				}
				cpresetFrame.pack();
				cpresetFrame.setVisible(true);
			}
		});
		globalMenu.setMnemonic(KeyEvent.VK_D);
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

	@Override
	public boolean configurePlugin(ConfigNode node, ViewFrame viewFrame) {
		return true;
	}


}
