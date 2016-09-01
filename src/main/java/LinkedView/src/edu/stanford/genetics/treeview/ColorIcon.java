/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 * A little icon with a changeable color.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.5 $ $Date: 2004-12-21 03:28:14 $
 */
public class ColorIcon implements Icon {
	private final int width, height;
	private Color color;

	/**
	 * @param x
	 *            width of icon
	 * @param y
	 *            height of icon
	 * @param c
	 *            Initial color of icon.
	 */
	public ColorIcon(final int x, final int y, final Color c) {
		width = x;
		height = y;
		color = c;
	}

	/**
	 * Sets the color, but doesn't redraw or anything.
	 *
	 * @param c
	 *            The new color
	 */
	public void setColor(final Color c) {
		color = c;
	}

	/* inherit description */
	@Override
	public int getIconHeight() {
		return height;
	}

	/* inherit description */
	@Override
	public int getIconWidth() {
		return width;
	}

	/* inherit description */
	@Override
	public void paintIcon(final Component c, final Graphics g, final int x,
			final int y) {
		final Color old = g.getColor();
		g.setColor(color);
		g.fillRect(x, y, width, height);
		g.setColor(Color.black);
		g.drawRect(x, y, width, height);
		g.setColor(old);
	}
}
