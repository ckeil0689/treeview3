/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * This class extracts Urls from LabelInfo. Also included is a class to pop up
 * a configuration window.
 */
public class UrlEditor {

	private final UrlExtractor extractor;
	private final UrlPresets presets;
	private Window window;
	private final LabelInfo labelInfo;

	/**
	 * This class must be constructed around a LabelInfo
	 */
	public UrlEditor(final UrlExtractor ue, final UrlPresets up,
			final LabelInfo hI) {

		super();
		extractor = ue;
		presets = up;
		labelInfo = hI;
	}

	/**
	 * pops up a configuration dialog.
	 */
	public void showConfig(final Frame f) {

		if (window == null) {
			final Dialog d = new Dialog(f, getTitle(), false);
			d.setLayout(new BorderLayout());
			d.add(new UrlEditPanel());
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

		final UrlPresets p = new UrlPresets("UrlEditor");
		p.setConfigNode(null);
		final UrlEditor e = new UrlEditor(new UrlExtractor(null), p, null);
		final Frame f = new Frame(getTitle());
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
		f.add(new UrlEditPanel());
		// f.add(new Label(getTitle(),Label.CENTER), BorderLayout.NORTH);
		f.add(new ButtonPanel(), BorderLayout.SOUTH);
		window = f;
	}

	private static String getTitle() {

		return "Url Link Editor";
	}

	// Inner classes
	private class ButtonPanel extends Panel {

		private static final long serialVersionUID = 1L;

		public ButtonPanel() {

			final JButton close_button = new JButton("Close");
			close_button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					window.dispose();
				}
			});
			add(close_button);
		}
	}

	private class UrlEditPanel extends Panel {

		private static final long serialVersionUID = 1L;

		private GridBagConstraints gbc;
		private LabelChoice labelChoice;
		private TemplateField templateField;

		public UrlEditPanel() {

			redoLayout();
			templateField.setText(extractor.getUrlTemplate());
			labelChoice.select(extractor.getIndex());
			updatePreview();
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
			gbc.weighty = 0;
			addTemplate();
			gbc.gridy = 1;
			addHeader();
			gbc.gridy = 2;
			addPreview();
			gbc.gridy = 3;
			gbc.gridx = 0;
			gbc.gridwidth = 3;
			gbc.weighty = 100;
			add(new JLabel("Url Presets (Can edit under Program Menu)",
					SwingConstants.CENTER), gbc);
			gbc.gridwidth = 1;

			for (int i = 0; i < nPresets; i++) {
				gbc.gridy++;
				addPreset(i);
			}
		}

		String tester = "YAL039W";
		JTextField previewField;

		private void addPreview() {

			gbc.gridx = 0;
			gbc.weightx = 0;
			add(new JLabel("Preview:"), gbc);
			gbc.gridx = 1;
			gbc.weightx = 100;
			previewField = new JTextField(extractor.substitute(tester));
			previewField.setEditable(false);
			add(previewField, gbc);
			final JButton update = new JButton("Update");
			update.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					updatePreview();
				}
			});
			gbc.gridx = 2;
			gbc.weightx = 0;
			// add(update, gbc);
		}

		private void updatePreview() {

			extractor.setUrlTemplate(templateField.getText());
			extractor.setIndex(labelChoice.getSelectedIndex());
			previewField.setText(extractor.getUrl(0));
		}

		private void addHeader() {

			gbc.gridx = 0;
			gbc.weightx = 0;
			add(new JLabel("Header:"), gbc);
			gbc.gridx = 1;
			gbc.weightx = 100;
			labelChoice = new LabelChoice();
			add(labelChoice, gbc);
		}

		private class LabelChoice extends Choice implements ItemListener {

			private static final long serialVersionUID = 1L;

			public LabelChoice() {

				super();
				String[] prefixes;
				int lastI;

				if (labelInfo != null) {
					prefixes = labelInfo.getPrefixes();
					lastI = prefixes.length;

					if (labelInfo.getIndex("GWEIGHT") != -1) {
						lastI--;
					}
				} else {
					prefixes = new String[] { "Dummy1", "Dummy2", "Dummy3" };
					lastI = prefixes.length;
				}

				for (int i = 0; i < lastI; i++) {

					add(prefixes[i]);
				}
				addItemListener(this);
			}

			@Override
			public void itemStateChanged(final ItemEvent e) {

				updatePreview();
			}
		}

		private void addTemplate() {

			gbc.gridx = 0;
			gbc.weightx = 0;
			add(new JLabel("Template:"), gbc);
			gbc.gridx = 1;
			gbc.weightx = 100;
			templateField = new TemplateField();
			add(templateField, gbc);
			final JButton updateButton = new JButton("Update");
			updateButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					updatePreview();
				}
			});

			gbc.gridx = 2;
			gbc.weightx = 0;
			add(updateButton, gbc);
		}

		private class TemplateField extends TextField {

			private static final long serialVersionUID = 1L;

			public TemplateField() {

				super("enter url template");
				addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(final ActionEvent e) {

						updatePreview();
					}
				});
			}
		}

		private void addPreset(final int i) {

			final int index = i;
			gbc.gridx = 0;
			add(new JLabel((presets.getPresetNames())[index]), gbc);
			gbc.gridx = 1;
			gbc.weightx = 100;
			add(new JTextField(presets.getTemplate(index)), gbc);
			gbc.gridx = 2;
			gbc.weightx = 0;
			final JButton set = new JButton("Set");
			set.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					templateField.setText(presets.getTemplate(index));
					updatePreview();
				}
			});
			add(set, gbc);
		}
	}
}
