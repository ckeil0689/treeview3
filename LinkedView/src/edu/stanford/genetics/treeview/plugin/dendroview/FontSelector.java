/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: FontSelector.java,v $
 * $Revision: 1.2 $
 * $Date: 2008-06-11 01:58:57 $
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
package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import edu.stanford.genetics.treeview.NatField;

/**
 * Allows selection of fonts for a FontSelectable
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.2 $ $Date: 2008-06-11 01:58:57 $
 */
public class FontSelector extends Panel {

	private static final long serialVersionUID = 1L;

	public static final Font[] fonts = GraphicsEnvironment
			.getLocalGraphicsEnvironment().getAllFonts();

	private final String title;
	private Choice font_choice;
	private Choice style_choice;
	private NatField size_field;
	private Button display_button;
	private Frame top;
	private Dialog d;
	private final FontSelectable client;

	String size_prop, face_prop, style_prop;

	/**
	 * Constructor for the FontSelector object
	 * 
	 * @param fs
	 *            FontSelectable to modify
	 * @param name
	 *            Title for the titlebar
	 */
	public FontSelector(final FontSelectable fs, final String name) {

		title = name;
		client = fs;
		setupWidgets();
	}

	/**
	 * Place component using gridbaglayout
	 * 
	 * @param gbl
	 *            Layout to use
	 * @param comp
	 *            Compnent to layout
	 * @param x
	 *            x coordinate in layout
	 * @param y
	 *            y coordinate in layout
	 * @param width
	 *            width in layout
	 * @param anchor
	 *            anchor direction
	 * @return GridBagConstraints used
	 */
	private GridBagConstraints place(final GridBagLayout gbl,
			final Component comp, final int x, final int y, final int width,
			final int anchor) {

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = width;
		gbc.anchor = anchor;
		gbc.fill = GridBagConstraints.BOTH;
		gbl.setConstraints(comp, gbc);
		return gbc;
	}

	/**
	 * Sets up widgets
	 */
	private void setupWidgets() {

		final GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);

		final Label font_label = new Label("Font:", Label.LEFT);
		add(font_label);

		font_choice = new Choice();
		for (int i = 0; i < fonts.length; ++i) {

			font_choice.addItem(fonts[i].getFontName());
		}

		font_choice.select(client.getFace());
		add(font_choice);

		final Label style_label = new Label("Style:", Label.LEFT);
		add(style_label);

		style_choice = new Choice();
		for (int i = 0; i < styles.length; ++i) {

			style_choice.addItem(styles[i]);
		}
		style_choice.select(decode_style(client.getStyle()));
		add(style_choice);

		final Label size_label = new Label("Size:", Label.LEFT);
		add(size_label);

		size_field = new NatField(client.getPoints(), 3);
		add(size_field);

		display_button = new Button("Display");
		display_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent actionEvent) {

				final String string = font_choice.getSelectedItem();
				final int i = encode_style(style_choice.getSelectedItem());
				final int size = size_field.getNat();

				client.setFace(string);
				client.setStyle(i);
				client.setPoints(size);
			}
		});
		add(display_button);

		place(gbl, font_label, 0, 0, 1, GridBagConstraints.WEST);
		place(gbl, font_choice, 1, 0, 1, GridBagConstraints.EAST);
		place(gbl, style_label, 0, 1, 1, GridBagConstraints.WEST);
		place(gbl, style_choice, 1, 1, 1, GridBagConstraints.EAST);
		place(gbl, size_label, 0, 2, 1, GridBagConstraints.WEST);
		place(gbl, size_field, 1, 2, 1, GridBagConstraints.EAST);
		place(gbl, display_button, 0, 3, 2, GridBagConstraints.WEST);
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

		return style.equalsIgnoreCase(styles[0]) ? Font.PLAIN
				: style.equalsIgnoreCase(styles[1]) ? Font.ITALIC
						: style.equalsIgnoreCase(styles[2]) ? Font.BOLD : 
							Font.BOLD + Font.ITALIC;
	}

	/**
	 * Create a toplevel font selecting frame
	 */
	public void makeTop() {

		top = new Frame(getTitle());
		top.add(this);
		top.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent we) {

				we.getWindow().dispose();
			}
		});
		top.pack();
		top.setVisible(true);
	}

	/**
	 * Create a blocking font selecting dialog
	 * 
	 * @param f
	 *            frame to block
	 */
	public void showDialog(final Frame f) {

		d = new Dialog(f, getTitle());
		d.add(this);
		d.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent we) {
				we.getWindow().dispose();
			}
		});
		d.pack();
		d.setVisible(true);
	}

	/**
	 * @return The title of this FontSelector
	 */
	protected String getTitle() {

		return title;
	}
}
