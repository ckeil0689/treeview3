/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: FontSettingsPanel.java,v $
 * $Revision: 1.2 $B
 * $Date: 2008-03-09 21:06:34 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved.
 * Modified by Alex Segal 2004/08/13. Modifications Copyright (C)
 * Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular,
 *
 * 1) If you modify a source file, make a comment in it containing your name
 * and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView
 * maintainers at alok@genome.stanford.edu when they make a useful addition.
 * It would be nice if significant contributions could be merged into the
 * main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER
 */
package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.SettingsPanel;

/**
 * This class allows selection of Fonts for a FontSelectable.
 */
public class FontSettings implements SettingsPanel {

	private final String[] preferredFonts = {

	"Arial", "Tahoma", "Verdana", "Times New Roman", "Helvetica", "Calibri",
			"Courier", "Dialog", "Myriad" };

	private Font[] fonts;

	private final FontSelectable client;
	private final FontSelectable client2;
	
	private JPanel fontPanel;
	private JComboBox<String> font_choice;
	private JComboBox<String> style_choice;
	private JCheckBox fixedBox;
	
	private JSpinner size_field;
	private JSpinner min_field;
	private JSpinner max_field;

	public FontSettings(final FontSelectable fs, final FontSelectable fs2) {

		client = fs;
		client2 = fs2;
	}

	/**
	 * Makes a JPanel that includes the font selection options. Then returns
	 * this JPanel.
	 *
	 * @return JPanel
	 */
	public JPanel makeFontPanel() {

		fontPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
		fontPanel.setBorder(BorderFactory.createTitledBorder("Set Label Font"));
		setupFonts();
		setupWidgets();

		return fontPanel;
	}

	public static void main(final String[] argv) {

		// final HeaderInfo hi = new DummyHeaderInfo();
		// final UrlExtractor ue = new UrlExtractor(hi);
		//
		// final FontSelectable fs = new TextView(hi, ue);
		// final FontSelectable fs2 = new ArrayNameView(hi, ue);
		// fs.setPoints(10);
		// final FontSettings e = new FontSettings(fs, fs2);
		// final JFrame f = new JFrame(StringRes.test_title_FontSelector);
		// f.add(e.makeFontPanel());
		// f.addWindowListener(new WindowAdapter() {
		// @Override
		// public void windowClosing(final WindowEvent we) {
		// System.exit(0);
		// }
		// });
		// f.pack();
		// f.setVisible(true);
	}

	@Override
	public void synchronizeFrom() {

		setupWidgets();
	}

	@Override
	public void synchronizeTo() {
		// nothing to do...
	}

	// the allowed font styles
	/**
	 * Description of the Field
	 */
	public final static String[] styles = {

	"Plain", "Italic", "Bold", "Bold Italic" };

	/**
	 * turn a style number from class java.awt.Font into a string
	 *
	 * @param style
	 *            style index
	 * @return string description
	 */
	public final static String decode_style(final int style) {

		switch (style) {

		case Font.PLAIN:
			return styles[0];

		case Font.ITALIC:
			return styles[1];

		case Font.BOLD:
			return styles[2];

		default:
			return styles[3];
		}
	}

	/**
	 * turn a string into a style number
	 *
	 * @param style
	 *            string description
	 * @return integer encoded representation
	 */
	public final static int encode_style(final String style) {

		return style.equalsIgnoreCase(styles[0]) ? Font.PLAIN : style
				.equalsIgnoreCase(styles[1]) ? Font.ITALIC : style
				.equalsIgnoreCase(styles[2]) ? Font.BOLD : Font.BOLD
				+ Font.ITALIC;
	}

	/**
	 * Gets all the preferred fonts from the system.
	 */
	public void setupFonts() {

		// Getting only preferred fonts from the system.
		final Font[] allFonts = GraphicsEnvironment
				.getLocalGraphicsEnvironment().getAllFonts();

		final List<Font> fontList = new ArrayList<Font>();

		for (final String fontName : preferredFonts) {

			for (final Font font : allFonts) {

				if (font.getName().contains(fontName)) {
					fontList.add(font);
				}
			}
		}

		fonts = new Font[fontList.size()];
		for (int i = 0; i < fontList.size(); i++) {

			fonts[i] = fontList.get(i);
		}
	}

	/**
	 * Create a blocking dialog containing this component
	 *
	 * @param f
	 *            frame to block
	 */
	public void showDialog(final Frame f, final String title) {

		final JDialog d = new JDialog(f, title);
		d.setLayout(new BorderLayout());
		d.add(fontPanel, BorderLayout.CENTER);
		d.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent we) {
				we.getWindow().dispose();
			}
		});
		d.pack();
		d.setVisible(true);
	}

	private void setupFontChoice() {

		final String[] fontNames = new String[fonts.length];
		for (final Font f : fonts) {

			fontNames[Arrays.asList(fonts).indexOf(f)] = f.getName();
		}

		font_choice = GUIFactory.createComboBox(fontNames);
		font_choice.setEditable(true);
		AutoCompleteDecorator.decorate(font_choice);
		font_choice.setSelectedItem(client.getFace());
		font_choice.addActionListener(new SelectionListener());
	}

	private void setupStyleChoice() {

		style_choice = GUIFactory.createComboBox(styles);
		style_choice.setSelectedItem(decode_style(client.getStyle()));
		style_choice.addActionListener(new SelectionListener());
	}

	private void synchronizeClient() {

		final String string = (String) font_choice.getSelectedItem();
		final int i = encode_style((String) style_choice.getSelectedItem());
		final int size = (Integer) size_field.getValue();
		final int min = (Integer) min_field.getValue();
		final int max = (Integer) max_field.getValue();
		final boolean isFixed = fixedBox.isSelected();

		client.setFace(string);
		client.setStyle(i);
		client.setSavedPoints(size);
		client.setMin(min);
		client.setMax(max);
		client.setFixed(isFixed);

		client2.setFace(string);
		client2.setStyle(i);
		client2.setSavedPoints(size);
		client2.setMin(min);
		client2.setMax(max);
		client2.setFixed(isFixed);
	}

	/**
	 * Sets up widgets
	 */
	private void setupWidgets() {

		fontPanel.removeAll();

		setupFontChoice();
		fontPanel.add(font_choice, "span, wrap");

		setupStyleChoice();
		fontPanel.add(style_choice, "span, wrap");

		SpinnerModel size_model = new SpinnerNumberModel(client.getPoints(), 0, 
				50, 1);
		SpinnerModel min_model = new SpinnerNumberModel(client.getMin(), 0, 
				50, 1);
		SpinnerModel max_model = new SpinnerNumberModel(client.getMax(), 0, 
				50, 1);
		
		/* Font size */
		// getLastSize() to avoid issues with hint label font size.
		size_field = new JSpinner(size_model);
		size_field.addChangeListener(new FontSizeChangeListener());
		fontPanel.add(size_field);

		fixedBox = new JCheckBox("Keep fixed");
		fixedBox.setSelected(client.getFixed());
		fixedBox.addActionListener(new SelectionListener());
		fontPanel.add(fixedBox, "wrap");

		/* Minimum font size */
		JLabel minLabel = new JLabel("Min:");
		minLabel.setFont(GUIFactory.FONTS);
		min_field = new JSpinner(min_model);
		min_field.addChangeListener(new FontSizeChangeListener());
		fontPanel.add(minLabel);
		fontPanel.add(min_field);

		/* Maximum font size */
		JLabel maxLabel = new JLabel("Max:");
		maxLabel.setFont(GUIFactory.FONTS);
		max_field = new JSpinner(max_model);
		max_field.addChangeListener(new FontSizeChangeListener());
		fontPanel.add(maxLabel);
		fontPanel.add(max_field);
	}

	/**
	 * Listener to remove need for a button. When the user chooses a value for a
	 * Swing component with this listener, the font is automatically updated.
	 *
	 * @author CKeil
	 *
	 */
	class SelectionListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			synchronizeClient();
		}
	}

	class FontSizeChangeListener implements ChangeListener {

		@Override
		public void stateChanged(ChangeEvent e) {
			
			synchronizeClient();
		}
	}
}
