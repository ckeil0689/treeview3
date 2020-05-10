/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package gui.settings;

import gui.GUIFactory;
import gui.labels.url.UrlExtractor;
import gui.labels.url.UrlPresets;
import model.data.labels.IntLabelInfo;
import model.data.labels.LabelInfo;
import util.LogBuffer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * This class displays editable Url settings.
 *
 * It requires a UrlExtractor, HeaderInfo and optionally a UrlPresets
 */
public class UrlSettingsPanel implements SettingsPanel {

	private final UrlExtractor urlExtractor;
	private UrlPresets urlPresets = null;
	private final LabelInfo labelInfo;

	private JPanel mainPanel;

	private JLabel previewLabel;
	private final TemplateField templateField;
	private HeaderChoice headerChoice;

	public UrlSettingsPanel(final UrlExtractor ue, final UrlPresets up) {

		this(ue, ue.getLabelInfo(), up);
	}

	public UrlSettingsPanel(final UrlExtractor ue, final LabelInfo hi,
			final UrlPresets up) {

		super();
		urlExtractor = ue;
		urlPresets = up;
		labelInfo = hi;
		templateField = new TemplateField();
		templateField.setText(urlExtractor.getUrlTemplate());
		templateField.setMinimumSize(new Dimension(300, templateField
				.getHeight()));
	}

	public JPanel generate(final String name) {

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
		final LabelInfo hi = new IntLabelInfo();
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

		mainPanel.add(templateField, "w 90%, pushx, wrap");

		previewLabel = new JLabel("Ex: " + urlExtractor.getUrl(0));
		// previewField = new JTextField(urlExtractor.substitute(tester));
		// previewLabel.setEditable(false);
		// mainPanel.add(previewLabel, "w 100%, wrap");

		final JComboBox<String> options = new JComboBox<String>(
				urlPresets.getPresetLabelTypes());

		options.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {

				templateField.setText(urlPresets.getTemplate(options
						.getSelectedIndex()));
				updatePreview();
			}
		});

		mainPanel.add(options, "alignx 0%, w 20%, pushx");

		try {
			headerChoice.setSelectedIndex(urlExtractor.getIndex());
		} catch (final IllegalArgumentException e) {
			LogBuffer.logException(e);
			headerChoice.setSelectedIndex(0);
		}
	}

	private void updatePreview() {

		urlExtractor.setUrlTemplate(templateField.getText());
		urlExtractor.setIndex(headerChoice.getSelectedIndex());
		previewLabel.setText("Ex: " + urlExtractor.getUrl(0));
	}

	private class HeaderChoice extends JComboBox<String> implements
			ItemListener {

		private static final long serialVersionUID = 1L;

		public HeaderChoice() {

			super();
			String[] headers;
			int lastI;
			if (labelInfo != null) {
				headers = labelInfo.getLabelTypes();
				lastI = headers.length;
				if (labelInfo.getIndex("GWEIGHT") != -1) {
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
