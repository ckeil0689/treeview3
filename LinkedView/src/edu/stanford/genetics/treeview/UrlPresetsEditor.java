/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * This class extracts Urls from HeaderInfo. Also included is a class to pop up
 * a configuration window.
 */
public class UrlPresetsEditor extends JPanel implements SettingsPanel {

	private static final long serialVersionUID = 1L;

	private final UrlPresets presets;
	private Window window;
	private String title = "Url Preset Editor";
	private PresetEditPanel presetEditPanel;

	/**
	 * This class is to enable editing of a UrlPresets object. HACK I botched
	 * the design pretty badly here, but I'm too busy to clean it up now.
	 */
	public UrlPresetsEditor(final UrlPresets up) {

		super();
		presets = up;
		presetEditPanel = new PresetEditPanel();
		add(presetEditPanel);
	}

	/**
	 * pops up a configuration dialog.
	 */
	public void showConfig(final Frame f) {

		if (window == null) {
			final Dialog d = new Dialog(f, getTitle(), false);
			d.setLayout(new BorderLayout());
			presetEditPanel = new PresetEditPanel();
			d.add(presetEditPanel);
			d.add(new JLabel(getTitle()), BorderLayout.NORTH);
			d.add(new ButtonPanel(), BorderLayout.SOUTH);
			d.addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosing(final WindowEvent we) {

					we.getWindow().setVisible(false);
				}
			});
			d.pack();
			window = d;
		}
		window.setVisible(true);
	}

	public static void main(final String[] argv) {

		final UrlPresets p = new UrlPresets("UrlPresetsEditor");
		p.setConfigNode(null);

		final UrlPresetsEditor e = new UrlPresetsEditor(p);
		final Frame f = new Frame(e.getTitle());
		e.addToFrame(f);

		f.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent we) {

				System.exit(0);
			}
		});

		f.pack();
		f.setVisible(true);
	}

	public void addToFrame(final Frame f) {

		f.setLayout(new BorderLayout());
		presetEditPanel = new PresetEditPanel();
		f.add(presetEditPanel);
		// f.add(new Label(getTitle(),Label.CENTER), BorderLayout.NORTH);
		f.add(new ButtonPanel(), BorderLayout.SOUTH);
		window = f;
	}

	public String getTitle() {

		return title;
	}

	public void setTitle(final String s) {

		title = s;
	}

	@Override
	public void synchronizeTo() {

		presetEditPanel.saveAll();
	}

	@Override
	public void synchronizeFrom() {

		presetEditPanel.redoLayout();
	}

	// Inner classes
	private class ButtonPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		public ButtonPanel() {

			final JButton save_button = new JButton("Save");
			save_button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					synchronizeTo();
					window.setVisible(false);
				}
			});
			add(save_button);

			final JButton cancel_button = new JButton("Cancel");
			cancel_button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					synchronizeFrom();
					window.setVisible(false);
				}
			});
			add(cancel_button);
		}
	}

	private class PresetEditPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		private GridBagConstraints gbc;
		private JCheckBox[] presetEnablings;
		private JRadioButton[] defaultButtons;
		private JTextField[] presetNames;
		private JTextField[] presetHeaders;
		private JTextField[] presetTemplates;

		public PresetEditPanel() {

			redoLayout();
		}

		public void redoLayout() {

			String[] preset;
			preset = presets.getPresetNames();
			final int nPresets = preset.length;
			removeAll();
			setLayout(new GridBagLayout());

			gbc = new GridBagConstraints();
			gbc.weighty = 100;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 4;
			gbc.weighty = 100;
			add(new JLabel("Modify Url Presets", SwingConstants.CENTER), gbc);

			gbc.gridwidth = 1;
			gbc.weighty = 0;
			gbc.gridy = 1;
			gbc.gridx = 0;
			gbc.ipadx = 15;
			add(new JLabel("Enabled"), gbc);

			gbc.gridx = 1;
			add(new JLabel("Header"), gbc);

			gbc.gridx = 2;
			add(new JLabel("Name"), gbc);

			gbc.gridx = 3;
			add(new JLabel("Template"), gbc);

			gbc.gridx = 4;
			add(new JLabel("Default?"), gbc);

			defaultButtons = new JRadioButton[nPresets + 1];
			presetEnablings = new JCheckBox[nPresets + 1];
			presetNames = new JTextField[nPresets + 1];
			presetHeaders = new JTextField[nPresets + 1];
			presetTemplates = new JTextField[nPresets + 1];

			final ButtonGroup bob = new ButtonGroup();
			for (int i = 0; i < nPresets; i++) {
				gbc.gridy++;
				addPreset(i);
				bob.add(defaultButtons[i]);
			}

			gbc.gridy++;
			addNonePreset(nPresets);
			bob.add(defaultButtons[nPresets]);
			if (presets.getDefaultPreset() == -1) {
				defaultButtons[nPresets].setSelected(true);

			} else {
				defaultButtons[presets.getDefaultPreset()].setSelected(true);
			}
		}

		private void saveAll() {

			final int n = presetNames.length - 1; // for null...
			for (int i = 0; i < n; i++) {
				presets.setPresetHeader(i, presetHeaders[i].getText());
			}

			// for (int i = 0; i < n; i++) {
			// presets.setPresetName(i, presetNames[i].getText());
			// }

			for (int i = 0; i < n; i++) {
				presets.setPresetTemplate(i, presetTemplates[i].getText());
			}

			for (int i = 0; i < n; i++) {
				presets.setPresetEnabled(i, presetEnablings[i].isSelected());
			}

		}

		private void addPreset(final int i) {

			final int index = i;
			final JTextField templateField = new JTextField(50);
			final JTextField nameField = new JTextField();
			final JTextField headerField = new JTextField();
			final JCheckBox enabledField = new JCheckBox();

			gbc.gridx = 0;
			enabledField.setSelected((presets.getPresetEnablings())[index]);
			presetEnablings[index] = enabledField;
			add(enabledField, gbc);

			gbc.gridx = 1;
			gbc.weightx = 100;
			headerField.setText((presets.getPresetHeaders())[index]);
			presetHeaders[index] = headerField;
			add(headerField, gbc);

			gbc.gridx = 2;
			nameField.setText((presets.getPresetNames())[index]);
			presetNames[index] = nameField;
			add(nameField, gbc);

			gbc.gridx = 3;
			gbc.weightx = 100;
			templateField.setText(presets.getTemplate(index));
			presetTemplates[index] = templateField;
			add(templateField, gbc);

			gbc.gridx = 4;
			gbc.weightx = 0;
			final JRadioButton set = new JRadioButton();
			defaultButtons[index] = set;
			set.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					presets.setDefaultPreset(index);
				}
			});
			add(set, gbc);
		}

		private void addNonePreset(final int i) {

			final int index = i;
			// final JTextField templateField = new JTextField();
			final JTextField nameField = new JTextField();

			gbc.gridx = 2;
			nameField.setText("None");
			nameField.setEditable(false);
			presetNames[index] = nameField;
			add(nameField, gbc);

			gbc.gridx = 3;
			gbc.weightx = 100;
			// templateField.setText(presets.getTemplate(index));
			presetTemplates[index] = null;
			// add(templateField, gbc);

			gbc.gridx = 4;
			gbc.weightx = 0;
			final JRadioButton set = new JRadioButton();
			defaultButtons[index] = set;
			set.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					presets.setDefaultPreset(-1);
				}
			});
			add(set, gbc);
		}
	}
}
