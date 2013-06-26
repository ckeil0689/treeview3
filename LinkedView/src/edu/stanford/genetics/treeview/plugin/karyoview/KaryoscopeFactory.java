/*
 * Created on Aug 15, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.plugin.karyoview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JFrame;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.PluginFactory;
import edu.stanford.genetics.treeview.SettingsPanelHolder;
import edu.stanford.genetics.treeview.TabbedSettingsPanel;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.core.PluginManager;

/**
 * @author aloksaldanha
 *
 */
public class KaryoscopeFactory extends PluginFactory {
	private static KaryoColorPresets colorPresets = new KaryoColorPresets();
	private static CoordinatesPresets coordPresets = new CoordinatesPresets();
	private static KaryoColorPresetEditor  cpresetEditor = null;
	private static CoordinatesPresetEditor  coordEditor = null;
	private JFrame cpresetFrame = null;
	private TabbedSettingsPanel tabbedPanel;
	static {
		PluginManager.registerPlugin(new KaryoscopeFactory());
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#getPluginName()
	 */
	public String getPluginName() {
		return "Karyoscope";
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#restorePlugin(edu.stanford.genetics.treeview.ConfigNode, edu.stanford.genetics.treeview.ViewFrame)
	 */
	public MainPanel restorePlugin(ConfigNode node, ViewFrame viewFrame) {
		KaryoPanel karyoPanel = new KaryoPanel(viewFrame.getDataModel(), 
				viewFrame.getGeneSelection(), viewFrame, node);
		karyoPanel.setName(getPluginName());
		return karyoPanel;
	}
	
	public KaryoscopeFactory() {
		super();
		cpresetEditor = new KaryoColorPresetEditor(colorPresets);
		cpresetEditor.setTitle("Karyoscope Color Presets");
		coordEditor  = new CoordinatesPresetEditor(coordPresets);
		coordEditor.setTitle("Karyoscope Coordinates Presets");
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#setGlobalNode(edu.stanford.genetics.treeview.ConfigNode)
	 */
	public void setGlobalNode(ConfigNode node) {
		super.setGlobalNode(node);
		
		colorPresets.bindConfig(node.fetchOrCreate("ColorPresets"));
		if (colorPresets.getNumPresets() == 0) {
		  colorPresets.addDefaultPresets();
		}
		cpresetEditor.synchronizeFrom();
		/*
		coordPresets.bindConfig(node.fetchOrCreate("CoordPresets"));
		if (coordPresets.getNumPresets() == 0) {
			coordPresets.addDefaultPresets();
		}
		coordEditor.synchronizeFrom();
		*/
	}

	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.PluginFactory#addPluginConfig(java.awt.Menu)
	 */
	public void addPluginConfig(TreeviewMenuBarI globalMenu, final ViewFrame frame) {
		super.addPluginConfig(globalMenu, frame);
		globalMenu.addMenuItem("Karyoscope Color...", new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				if (cpresetFrame == null) {
					setupPresetsFrame(frame.getApp().getGlobalConfig().getRoot());
				}
				tabbedPanel.setSelectedComponent(cpresetEditor);
				cpresetFrame.setVisible(true);
			}
		});
		globalMenu.setMnemonic(KeyEvent.VK_K);

		if (coordPresets.getNumPresets() == 0) {
			try {
				coordPresets.scanUrl(new URL(frame.getApp().getCodeBase().toString() +"/coordinates"));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			coordEditor.synchronizeFrom();
		}
		
		globalMenu.addMenuItem("Karyoscope Coordinates...", new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				if (cpresetFrame == null) {
					setupPresetsFrame(frame.getApp().getGlobalConfig().getRoot());
				}
				tabbedPanel.setSelectedComponent(coordEditor);
				cpresetFrame.setVisible(true);
			}
		});
		globalMenu.setMnemonic(KeyEvent.VK_O);		
	}
	/**
	 * 
	 * @param frame ViewFrame that contains relevant global config node
	 */
	private void setupPresetsFrame(ConfigNode node) {
		tabbedPanel = new TabbedSettingsPanel();
		tabbedPanel.addSettingsPanel("Color", cpresetEditor);
		tabbedPanel.addSettingsPanel("Coordinates", coordEditor);
		
		cpresetFrame = new JFrame("Karyoscope Presets");
		SettingsPanelHolder holder = 
			new SettingsPanelHolder(cpresetFrame, node);
		holder.addSettingsPanel(tabbedPanel);
		cpresetFrame.getContentPane().add(holder);
		cpresetFrame.pack();
	}
	
	/**
	 * mechanism by which KaryoPanel can access the presets.
	 * @return color presets for dendrogram view
	 */
	public static KaryoColorPresets getColorPresets() {
		return colorPresets;
	}
	/**
	 * mechanism by which KaryoPanel can access the presets.
	 * @return color presets for dendrogram view
	 */
	public static CoordinatesPresets getCoordinatesPresets() {
		return coordPresets;
	}
	/** returns JPanel that allowed editing of coordinates presets */
	public static CoordinatesPresetEditor getCoordinatesPresetsEditor() {
		return coordEditor;
	}

	public boolean configurePlugin(ConfigNode node, ViewFrame viewFrame) {
		return true;
	}
}
