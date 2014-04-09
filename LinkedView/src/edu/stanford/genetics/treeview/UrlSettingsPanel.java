/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: UrlSettingsPanel.java,v $
 * $Revision: 1.6 $B
 * $Date: 2010-05-11 13:30:43 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular, 
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * This class displays editable Url settings.
 * 
 * It requires a UrlExtractor, HeaderInfo and optionally a UrlPresets
 */
public class UrlSettingsPanel extends JPanel implements SettingsPanel {

	private static final long serialVersionUID = 1L;

	private final UrlExtractor urlExtractor;
	private UrlPresets urlPresets = null;
	private final HeaderInfo headerInfo;
	private JDialog d;
	private Window window;

	private JButton[] buttons;
	private JTextField previewField;
	private final TemplateField templateField;
	private HeaderChoice headerChoice;
	private GridBagConstraints gbc;

	public UrlSettingsPanel(final UrlExtractor ue, final UrlPresets up) {

		this(ue, ue.getHeaderInfo(), up);
	}

	public UrlSettingsPanel(final UrlExtractor ue, final HeaderInfo hi,
			final UrlPresets up) {

		super();
		urlExtractor = ue;
		urlPresets = up;
		headerInfo = hi;
		templateField = new TemplateField();
		templateField.setText(urlExtractor.getUrlTemplate());

		redoLayout();
		updatePreview();
		UrlSettingsPanel.this.setEnabled(urlExtractor.isEnabled());
	}

	public static void main(final String[] argv) {

		final UrlPresets p = new UrlPresets("UrlSettingsPanel");
		p.setConfigNode(null);
		final HeaderInfo hi = new DummyHeaderInfo();
		final UrlExtractor ue = new UrlExtractor(hi);

		final UrlSettingsPanel e = new UrlSettingsPanel(ue, hi, p);
		final Frame f = new Frame("Url Settings Test");
		f.add(e);
		f.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent we) {

				System.exit(0);
			}
		});
		f.pack();
		f.setVisible(true);
	}

	@Override
	public void synchronizeFrom() {

		redoLayout();
		UrlSettingsPanel.this.setEnabled(urlExtractor.isEnabled());
	}

	@Override
	public void synchronizeTo() {
		// nothing to do...
	}

	class EnablePanel extends JPanel {

		private static final long serialVersionUID = 1L;

		JCheckBox enableBox;

		public EnablePanel() {

			setLayout(new BorderLayout());

			add(new JLabel("Web Link:", SwingConstants.LEFT),
					BorderLayout.NORTH);

			enableBox = new JCheckBox("Enable", urlExtractor.isEnabled());
			enableBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {
					urlExtractor.setEnabled(enableBox.isSelected());
					UrlSettingsPanel.this.setEnabled(enableBox.isSelected());
				}
			});
			add(enableBox, BorderLayout.CENTER);
		}

		public boolean isSelected() {

			return enableBox.isSelected();
		}
	}

	/**
	 * Create a blocking dialog containing this component
	 * 
	 * @param f
	 *            frame to block
	 */
	public void showDialog(final Frame f, final String title) {

		d = new JDialog(f, title);
		window = d;
		d.setLayout(new BorderLayout());
		d.add(this, BorderLayout.CENTER);
		d.add(new ButtonPanel(), BorderLayout.SOUTH);
		d.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent we) {

				we.getWindow().dispose();
			}
		});
		d.pack();
		d.setVisible(true);
	}

	public void showDialog(final Frame f) {

		showDialog(f, "Url Settings Test");
	}

	@Override
	public void setEnabled(final boolean b) {

		templateField.setEnabled(b);
		headerChoice.setEnabled(b);
		previewField.setEnabled(b);
		for (int i = 0; i < buttons.length; i++) {
			if (buttons[i] != null) {
				buttons[i].setEnabled(b);
			}
		}
	}

	public void redoLayout() {

		String[] preset;
		preset = urlPresets.getPresetNames();
		final int nPresets = preset.length;
		removeAll();
		setLayout(new GridBagLayout());

		gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.gridy = 0;
		gbc.gridx = 0;
		gbc.weightx = 100;
		final JCheckBox enableBox = new JCheckBox("Enable",
				urlExtractor.isEnabled());
		enableBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {

				urlExtractor.setEnabled(enableBox.isSelected());
				UrlSettingsPanel.this.setEnabled(enableBox.isSelected());
			}
		});
		add(enableBox, gbc);

		gbc.gridx = 1;
		add(templateField, gbc);

		gbc.gridx = 2;
		headerChoice = new HeaderChoice();
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		add(headerChoice, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		previewField = new JTextField("Ex: " + urlExtractor.getUrl(0));
		// previewField = new JTextField(urlExtractor.substitute(tester));
		previewField.setEditable(false);
		add(previewField, gbc);

		final JPanel presetPanel = new JPanel();
		buttons = new JButton[nPresets];
		for (int i = 0; i < nPresets; i++) {
			final JButton presetButton = new JButton(
					(urlPresets.getPresetNames())[i]);
			final int index = i;
			presetButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					templateField.setText(urlPresets.getTemplate(index));
					updatePreview();
				}
			});
			presetPanel.add(presetButton);
			buttons[index] = presetButton;
		}

		gbc.gridy = 2;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 100;
		gbc.weightx = 100;
		// add(new JScrollPane(presetPanel,
		// JScrollPane.VERTICAL_SCROLLBAR_NEVER,
		// JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS), gbc);
		add(presetPanel, gbc);

		try {
			headerChoice.setSelectedIndex(urlExtractor.getIndex());

		} catch (final java.lang.IllegalArgumentException e) {
		}
	}

	private void updatePreview() {

		urlExtractor.setUrlTemplate(templateField.getText());
		urlExtractor.setIndex(headerChoice.getSelectedIndex());
		previewField.setText("Ex: " + urlExtractor.getUrl(0));
	}

	private class HeaderChoice extends JComboBox implements ItemListener {

		private static final long serialVersionUID = 1L;

		public HeaderChoice() {

			super();
			String[] headers;
			int lastI;
			if (headerInfo != null) {
				headers = headerInfo.getNames();
				lastI = headers.length;
				if (headerInfo.getIndex("GWEIGHT") != -1) {
					lastI--;
				}
			} else {
				headers = new String[] { "Dummy1", "Dummy2", "Dummy3" };
				lastI = headers.length;
			}

			for (int i = 0; i < lastI; i++) {
				if (headers[i] == null) {
					addItem("-- NULL --");

				} else {
					addItem(headers[i]);
				}
			}
			addItemListener(this);
		}

		@Override
		public void itemStateChanged(final ItemEvent e) {

			updatePreview();
		}
	}

	private class TemplateField extends JTextField {

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

	private class ButtonPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		public ButtonPanel() {

			final JButton save_button = new JButton("Close");
			save_button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					window.setVisible(false);
				}
			});
			add(save_button);
		}
	}
}
