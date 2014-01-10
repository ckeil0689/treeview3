/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: WaitScreen.java,v $
 * $Revision: 1.7 $
 * $Date: 2010-05-02 13:35:38 $
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

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;

/* This WaitScreen was originally designed to do the About message, but now
 it's pretty generic. */

public class WaitScreen extends Canvas {

	private static final long serialVersionUID = 1L;

	private int line_height;
	private final int line_widths[];
	private int max_width;
	private int total_height;
	private final int margin_width = 10;
	private final int margin_height = 10;
	private final String message[];

	public WaitScreen(final String[] m) {

		message = m;
		line_widths = new int[message.length];
	}

	private void measure() {

		final FontMetrics fm = this.getFontMetrics(this.getFont());

		if (fm == null) {

			return;
		}

		line_height = fm.getHeight();
		max_width = 0;
		for (int i = 0; i < message.length; i++) {
			line_widths[i] = fm.stringWidth(message[i]);

			if (line_widths[i] > max_width) {
				max_width = line_widths[i];
			}
		}

		// total_height = message.length * line_height;
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

		g.setColor(Color.black);
		int height = margin_height / 2 + line_height;

		for (int i = 0; i < message.length; i++) {
			g.drawString(message[i],
					(margin_width + max_width - line_widths[i]) / 2, height);
			height += line_height;
		}
	}

	public int getTotal_height() {

		return total_height;
	}

	public void setTotal_height(final int total_height) {

		this.total_height = total_height;
	}
}
