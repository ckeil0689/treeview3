package edu.stanford.genetics.treeview.core;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.PluginFactory;
import edu.stanford.genetics.treeview.TreeViewFrame;

public class MenuHelpPluginsFrame extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JTextField tf_dir = new JTextField();

	private final JLabel l_pluginlist = new JLabel("");

	/*
	 * EFFECTS: Sets <l_pluginslist> text to currently loaded plugins RETURNS: #
	 * of plugins loaded
	 */
	private int setLabelText() {

		final PluginFactory[] plugins = PluginManager.getPluginManager()
				.getPluginFactories();
		String s = null;
		int height = 0;
		if (plugins == null || plugins.length == 0) {
			s = "No Plugins Found";
			height = 1;

		} else {
			s = "<html><br><ol>";
			for (int i = 0; i < plugins.length; i++) {
				s += "<li>" + plugins[i].getPluginName();
			}
			s += "</ol><br></html>";
			height = plugins.length;
			LogBuffer.println("LabelHeight: " + height);
		}
		l_pluginlist.setText(s);
		return height;
	}

	/**
	 * @param url
	 */
	public void setSourceText(final String url) {

		tf_dir.setText(url);
		MenuHelpPluginsFrame.this.pack();
	}

	public MenuHelpPluginsFrame(final String string, final TreeViewFrame frame) {

		super(frame, string, false);
		final GridBagLayout gridbag = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		this.getContentPane().setLayout(gridbag);
		setLabelText();
		this.getContentPane().add(l_pluginlist, c);

		final JPanel dirPanel = new JPanel();

		dirPanel.add(tf_dir, BorderLayout.CENTER);
		final JButton b_browse = new JButton("Browse...");
		b_browse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {

				final JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				final int returnVal = chooser
						.showOpenDialog(MenuHelpPluginsFrame.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					final String url = chooser.getSelectedFile()
							.getAbsolutePath();
					setSourceText(url);
				}
			}
		});
		dirPanel.add(b_browse, BorderLayout.EAST);
		this.getContentPane().add(dirPanel, c);

		final JButton b_scan = new JButton("Scan new plugins");
		b_scan.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {
				final File[] files = PluginManager.getPluginManager().readdir(
						tf_dir.getText());
				if (files == null || files.length == 0) {
					JOptionPane.showMessageDialog(MenuHelpPluginsFrame.this,
							"Directory contains no plugins");
				} else {
					PluginManager.getPluginManager().loadPlugins(files, true);
				}
				PluginManager.getPluginManager().pluginAssignConfigNodes(
						frame.getApp().getGlobalConfig().getNode("Plugins"));
				setLabelText();
				MenuHelpPluginsFrame.this.validate();
				frame.rebuildMainPanelMenu();
			}
		});
		this.getContentPane().add(b_scan, c);
		this.pack();
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(frame);
	}
}