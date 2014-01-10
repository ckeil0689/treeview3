/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: Popup.java,v $
 * $Revision: 1.4 $
 * $Date: 2010-05-02 13:33:30 $
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
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * This popup was originally designed to do the About message, but now it's a
 * generic way to display a dismissable array of strings
 */

class Popup extends Canvas {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Frame parent; // parent frame associated with popup
	Dialog d; // Holds ref to dialog window
	int line_height;
	int line_widths[];
	int max_width;
	int total_height;
	int margin_width = 10;
	int margin_height = 10;

	private final String message[];
	private final String title;

	private void measure() {
		final FontMetrics fm = this.getFontMetrics(this.getFont());
		if (fm == null)
			return;
		line_height = fm.getHeight();
		max_width = 0;
		for (int i = 0; i < message.length; i++) {
			line_widths[i] = fm.stringWidth(message[i]);
			if (line_widths[i] > max_width) {
				max_width = line_widths[i];
			}
		}
		total_height = message.length * line_height;
	}

	public void center(final Rectangle vb) {
		final Dimension d = getSize();
		setLocation((vb.width - d.width) / 2 + vb.x, (vb.height - d.height) / 2
				+ vb.y);
	}

	public Popup(final Frame f, final String t, final String m[]) {
		title = t;
		message = m;

		d = new Dialog(f, title);
		parent = f;
		d.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				d.dispose();
			}
		});
		line_widths = new int[message.length];
		d.setLayout(new BorderLayout());
		d.add(this, BorderLayout.CENTER);

		d.add(new DisposePanel(d), BorderLayout.SOUTH);

		d.pack();
		final Point p = f.getLocation();
		d.setLocation(p.x + 10, p.y + 10);
		d.setVisible(true);
	}

	@Override
	public void addNotify() {
		super.addNotify();
		measure();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(max_width + 2 * margin_width, message.length
				* line_height + 2 * margin_height);
	}

	@Override
	public void paint(final Graphics g) {
		final Dimension dim = d.getSize();
		g.setColor(Color.black);
		// int height = (dim.height - total_height) / 2;
		int height = line_height;
		for (int i = 0; i < message.length; i++) {
			g.drawString(message[i], (dim.width - line_widths[i]) / 2, height);
			height += line_height;
		}
	}

	/**
	 * This class simply displays a close button centered in a panel. Clicking
	 * the close button disposes of the window.
	 */
	class DisposePanel extends Panel {
		/**
	 * 
	 */
		private static final long serialVersionUID = 1L;
		private final Window m_window;

		public DisposePanel(final Window window) {
			m_window = window;
			final Button dispose_button = new Button("Close");
			dispose_button.addActionListener(new ActionListener() {
				// called when close button hit
				@Override
				public void actionPerformed(final ActionEvent evt) {
					if (evt.getSource() == dispose_button) {
						m_window.dispose();
					}
				}
			});
			add(dispose_button);
		}
	}

}
