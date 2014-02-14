/*
 * Created on Aug 18, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.plugin.scatterview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.PluginFactory;
import edu.stanford.genetics.treeview.SettingsPanelHolder;
import edu.stanford.genetics.treeview.TabbedSettingsPanel;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.core.PluginManager;

/**
 * @author aloksaldanha
 * 
 */
public class ScatterplotFactory extends PluginFactory {
	
	private static ScatterColorPresets colorPresets = new ScatterColorPresets();
	private ScatterColorPresetEditor cpresetEditor = null;
	private JFrame cpresetFrame = null;
	private TabbedSettingsPanel tabbedPanel;
	static {
		PluginManager.registerPlugin(new ScatterplotFactory());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.PluginFactory#getPluginName()
	 */
	@Override
	public String getPluginName() {
		return "Scatterplot";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.stanford.genetics.treeview.PluginFactory#restorePlugin(edu.stanford
	 * .genetics.treeview.ConfigNode, edu.stanford.genetics.treeview.ViewFrame)
	 */
	@Override
	public MainPanel restorePlugin(final ConfigNode node,
			final TreeViewFrame viewFrame) {
		final ScatterPanel gsp = new ScatterPanel(viewFrame, node);
		gsp.setSelection(viewFrame.getGeneSelection());
		gsp.setName(getPluginName());
		return gsp;
	}

	public ScatterplotFactory() {
		super();
		cpresetEditor = new ScatterColorPresetEditor(colorPresets);
		cpresetEditor.setTitle("Scatterplot Color Presets");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.stanford.genetics.treeview.PluginFactory#setGlobalNode(edu.stanford
	 * .genetics.treeview.ConfigNode)
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
	 * @see
	 * edu.stanford.genetics.treeview.PluginFactory#addPluginConfig(java.awt
	 * .Menu)
	 */
	@Override
	public void addPluginConfig(final TreeviewMenuBarI globalMenu,
			final ViewFrame frame) {
		super.addPluginConfig(globalMenu, frame);
		globalMenu.addMenuItem("Scatterplot Color...");
//		, new ActionListener() {
//			@Override
//			public void actionPerformed(final ActionEvent actionEvent) {
//				if (cpresetFrame == null) {
//					setupPresetsFrame(frame.getApp().getGlobalConfig()
//							.getRoot());
//				}
//				tabbedPanel.setSelectedComponent(cpresetEditor);
//				cpresetFrame.setVisible(true);
//			}
//		});
		globalMenu.setMnemonic(KeyEvent.VK_S);
	}

	/**
	 * 
	 * @param tvFrame
	 *            ViewFrame that contains relevant global config node
	 */
	private void setupPresetsFrame(final ConfigNode node) {
		cpresetFrame = new JFrame("Scatterplot Color");
		final SettingsPanelHolder holder = new SettingsPanelHolder(
				cpresetFrame, node);
		holder.addSettingsPanel(cpresetEditor);
		cpresetFrame.getContentPane().add(holder);
		cpresetFrame.pack();
	}

	/**
	 * mechanism by which ScatterPanel can access the presets.
	 * 
	 * @return color presets for scatterplot view
	 */
	public static ScatterColorPresets getColorPresets() {
		return colorPresets;
	}

	@Override
	public boolean configurePlugin(final ConfigNode node, final ViewFrame frame) {
		final GraphDialog gd = new GraphDialog(node, frame);
		try {
			gd.setLocationRelativeTo(frame);
		} catch (final java.lang.NoSuchMethodError err) {
			// god damn MRJ for os9.
		}
		gd.pack();
		gd.setVisible(true);
		return true;
	}

	/**
	 * this class pops up a dialog window that allows one to make a graph.
	 */

	private class GraphDialog extends JDialog {
		StatPanel xPanel, yPanel;
		ViewFrame frame;
		ConfigNode node;
		int npre, nexpr;

		GraphDialog(final ConfigNode node, final ViewFrame frame) {
			super(frame, "Create Graph...", true);
			this.frame = frame;
			this.node = node;
			final JPanel box = new JPanel();
			box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
			yPanel = new StatPanel("Y Axis:");
			xPanel = new StatPanel("X Axis:");
			box.add(new JLabel("Create Graph:"));
			box.add(yPanel);
			box.add(xPanel);
			box.add(new ButtonPanel());
			setContentPane(box);
		}

		class StatPanel extends JPanel {
			JComboBox statPulldown;

			StatPanel(final String title) {
				super(false);

				final DataModel dataModel = frame.getDataModel();
				final HeaderInfo arrayInfo = dataModel.getArrayHeaderInfo();
				final HeaderInfo geneInfo = dataModel.getGeneHeaderInfo();
				// String [][] aHeaders = dataModel.getArrayHeaders();
				// int gidRow = dataModel.getGIDIndex();
				final int gidRow = 0;
				npre = geneInfo.getNumNames();
				nexpr = arrayInfo.getNumHeaders();
				final String[] statNames = new String[npre + nexpr + 1];
				// Index
				statNames[0] = "INDEX";
				// stat columns
				final String[] pre = geneInfo.getNames();
				for (int i = 0; i < npre; i++) {
					statNames[i + 1] = pre[i];
				}
				// experiment ratios
				for (int i = 0; i < nexpr; i++) {
					statNames[i + 1 + npre] = arrayInfo.getHeader(i)[gidRow];
				}
				add(new JLabel(title));
				statPulldown = new JComboBox(statNames);
				add(statPulldown);
			}

			int getType() {
				if (statPulldown.getSelectedIndex() == 0)
					return ScatterPanel.INDEX;
				if (statPulldown.getSelectedIndex() <= npre)
					return ScatterPanel.PREFIX;
				return ScatterPanel.RATIO;
			}

			// will return either an index into aHeaders or into
			// genePrefix depending...
			int getIndex() {
				if (getType() == ScatterPanel.PREFIX)
					return statPulldown.getSelectedIndex() - 1;
				if (getType() == ScatterPanel.RATIO)
					return statPulldown.getSelectedIndex() - 1 - npre;
				return -1;
			}
		}

		class ButtonPanel extends JPanel {
			private final JButton closeButton, goButton;

			ButtonPanel() {
				super();

				goButton = new JButton("Go!");
				goButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						final int xtype = xPanel.getType();
						final int xindex = xPanel.getIndex();
						final int ytype = yPanel.getType();
						final int yindex = yPanel.getIndex();
						node.setAttribute("type", "Scatterplot", null);
						node.setAttribute("xtype", xtype, 0);
						node.setAttribute("ytype", ytype, 0);
						node.setAttribute("xindex", xindex, 0);
						node.setAttribute("yindex", yindex, 0);
						GraphDialog.this.dispose();
					}
				});
				add(goButton);
				/*
				 * Why did I comment this?
				 */
				closeButton = new JButton("Cancel");
				closeButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						GraphDialog.this.dispose();
					}
				});
				add(closeButton);
			}
		}

	}
}
