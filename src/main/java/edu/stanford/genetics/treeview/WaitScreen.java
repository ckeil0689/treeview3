/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
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
