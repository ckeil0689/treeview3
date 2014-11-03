/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: ColorSetEditor.java,v $
 * $Revision: 1.1 $
 * $Date: 2006-08-16 19:13:46 $
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.ColorIcon;

/**
 * This class allows editing of a color set...
 * 
 * NOTE: This is superceded by the ConfigColorSet stuff in
 * edu.stanford.genetics.treeview, although this code is still used within the
 * dendroview package.
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.1 $ $Date: 2006-08-16 19:13:46 $
 */

public class ColorSetEditor extends JPanel {

	private static final long serialVersionUID = 1L;

	private final static int UP = 0;
	private final static int ZERO = 1;
	private final static int DOWN = 2;
	private final static int MISSING = 3;
	private final ColorSet colorSet;

	/**
	 * Constructor for the ColorSetEditor object
	 * 
	 * @param colorSet
	 *            <code>ColorSet</code> to be edited
	 */
	public ColorSetEditor(final ColorSet colorSet) {

		this.colorSet = colorSet;
		add(new ColorPanel(UP));
		add(new ColorPanel(ZERO));
		add(new ColorPanel(DOWN));
		add(new ColorPanel(MISSING));
	}

//	/**
//	 * A simple test program
//	 * 
//	 * @param argv
//	 *            ignored
//	 */
//	public final static void main(final String[] argv) {
//
//		final ColorSet temp = new ColorSet();
//		final ColorSetEditor cse = new ColorSetEditor(temp);
//		final JFrame frame = new JFrame("ColorSetEditor Test");
//		frame.getContentPane().add(cse);
//		frame.pack();
//		frame.setVisible(true);
//	}

	class ColorPanel extends JPanel {
		/**
		 * 
		 */
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
							ColorSetEditor.this, "Pick Color for "
									+ getLabel(), getColor());
					if (trial != null) {
						setColor(trial);
					}
				}
			});

			add(pushButton);
		}

		private void setColor(final Color c) {
			switch (type) {
			// case UP:
			// colorSet.setUp(c);
			// break;
			// case ZERO:
			// colorSet.setZero(c);
			// break;
			// case DOWN:
			// colorSet.setDown(c);
			// break;
			case MISSING:
				colorSet.setMissing(c);
				break;
			}
			colorIcon.setColor(getColor());
			// redoComps();
			repaint();
		}

		private String getLabel() {
			switch (type) {
			case UP:
				return "Positive";
			case ZERO:
				return "Zero";
			case DOWN:
				return "Negative";
			case MISSING:
				return "Missing";
			}
			return null;
		}

		private Color getColor() {
			switch (type) {
			// case UP:
			// return colorSet.getUp();
			// case ZERO:
			// return colorSet.getZero();
			// case DOWN:
			// return colorSet.getDown();
			case MISSING:
				return colorSet.getMissing();
			}
			return null;
		}
	}

}
