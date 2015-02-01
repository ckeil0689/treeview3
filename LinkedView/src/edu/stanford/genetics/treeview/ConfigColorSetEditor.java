/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: ConfigColorSetEditor.java,v $
 * $Revision: 1.4 $
 * $Date: 2004-12-21 03:28:14 $
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * This class allows editing of a color set.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.4 $ $Date: 2004-12-21 03:28:14 $
 */

public class ConfigColorSetEditor extends JPanel {

	private static final long serialVersionUID = 1L;

	private final ConfigColorSet colorSet;

	/**
	 * Constructor for the ConfigColorSetEditor object
	 *
	 * @param colorSet
	 *            A ConfigColorSet to be edited
	 */
	public ConfigColorSetEditor(final ConfigColorSet colorSet) {
		this.colorSet = colorSet;
		final String[] types = colorSet.getTypes();
		for (int i = 0; i < types.length; i++) {
			add(new ColorPanel(i));
		}
	}

	/**
	 * A simple test program. Allowed editing, and prints out the results.
	 *
	 * @param argv
	 *            none required.
	 */
	public final static void main(final String[] argv) {
		final String[] types = new String[] { "Up", "Down", "Left", "Right" };
		final String[] colors = new String[] { "#FF0000", "#FFFF00", "#FF00FF",
				"#00FFFF" };

		final ConfigColorSet temp = new ConfigColorSet("Bob", types, colors);
		final ConfigColorSetEditor cse = new ConfigColorSetEditor(temp);
		final JFrame frame = new JFrame("ColorSetEditor Test");
		frame.getContentPane().add(cse);
		frame.pack();
		frame.setVisible(true);
	}

	/** this has been superceded by the general ColorPanel class */
	class ColorPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		ColorIcon colorIcon;
		int type;

		ColorPanel(final int i) {
			type = i;
			redoComps();
		}

		public void redoComps() {
			removeAll();
			colorIcon = new ColorIcon(10, 10, getColor());
			final JButton pushButton = new JButton(getLabel(), colorIcon);
			pushButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					final Color trial = JColorChooser.showDialog(
							ConfigColorSetEditor.this, "Pick Color for "
									+ getLabel(), getColor());
					if (trial != null) {
						setColor(trial);
					}
				}
			});

			add(pushButton);
		}

		private void setColor(final Color c) {
			colorSet.setColor(type, c);
			colorIcon.setColor(getColor());
			repaint();
		}

		private String getLabel() {
			return colorSet.getType(type);
		}

		private Color getColor() {
			return colorSet.getColor(type);
		}
	}

}
