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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import Utilities.GUIFactory;

/**
 * This class displays editable Url settings.
 * 
 * It requires a UrlExtractor, HeaderInfo and optionally a UrlPresets
 */
public class UrlSettingsPanel implements SettingsPanel {

	private final UrlExtractor urlExtractor;
	private UrlPresets urlPresets = null;
	private final HeaderInfo headerInfo;
//	private JDialog d;
	
	private JPanel mainPanel;

	private JButton[] buttons;
	private JTextField previewField;
	private final TemplateField templateField;
	private HeaderChoice headerChoice;

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
//		
//		mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
//		mainPanel.setBorder(BorderFactory.createTitledBorder(up.g));
//
//		redoLayout();
//		updatePreview();
//		mainPanel.setEnabled(urlExtractor.isEnabled());
	}
	
	public JPanel generate(String name) {
		
		mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
		mainPanel.setBorder(BorderFactory.createTitledBorder(name));

		redoLayout();
		updatePreview();
		mainPanel.setEnabled(urlExtractor.isEnabled());
		
		return mainPanel;
	}

	public static void main(final String[] argv) {

		final UrlPresets p = new UrlPresets("UrlSettingsPanel");
		p.setConfigNode(null);
		final HeaderInfo hi = new DummyHeaderInfo();
		final UrlExtractor ue = new UrlExtractor(hi);

		final UrlSettingsPanel e = new UrlSettingsPanel(ue, hi, p);
		final JFrame f = new JFrame("Url Settings Test");
		f.add(e.generate("Test"));
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
		mainPanel.setEnabled(urlExtractor.isEnabled());
	}

	@Override
	public void synchronizeTo() {
		// nothing to do...
	}
	public void redoLayout() {

		String[] preset;
		preset = urlPresets.getPresetNames();
		final int nPresets = preset.length;
		mainPanel.removeAll();
		
		final JCheckBox enableBox = new JCheckBox("Enable",
				urlExtractor.isEnabled());
		enableBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {

				urlExtractor.setEnabled(enableBox.isSelected());
				mainPanel.setEnabled(enableBox.isSelected());
			}
		});
		mainPanel.add(enableBox, "alignx 0%, w 10%, split 2");
		
		headerChoice = new HeaderChoice();
		mainPanel.add(headerChoice, "align 0%, pushx, wrap");

		mainPanel.add(templateField, "w 100%, wrap");

		previewField = new JTextField("Ex: " + urlExtractor.getUrl(0));
		// previewField = new JTextField(urlExtractor.substitute(tester));
		previewField.setEditable(false);
		mainPanel.add(previewField, "w 100%, wrap");

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

		// add(new JScrollPane(presetPanel,
		// JScrollPane.VERTICAL_SCROLLBAR_NEVER,
		// JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS), gbc);
		mainPanel.add(presetPanel, "wrap");

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

	private class HeaderChoice extends JComboBox<String> implements ItemListener {

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
}
