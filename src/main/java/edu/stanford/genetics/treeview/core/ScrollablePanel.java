package edu.stanford.genetics.treeview.core;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * Class to make JPanel work well with ScrollPane, advice taken from
 * http://stackoverflow.com/questions/15783014/jtextarea-on-jpanel-inside-
 * jscrollpane-does-not-resize-properly
 *
 * @author CKeil
 *
 */
public class ScrollablePanel extends JPanel implements Scrollable {

	private static final long serialVersionUID = 1L;

	@Override
	public Dimension getPreferredScrollableViewportSize() {

		return super.getPreferredSize();
		// tell the JScrollPane that we want to be our 'preferredSize' -
		// but later, we'll say that vertically, it should scroll.
	}

	@Override
	public int getScrollableUnitIncrement(final Rectangle visibleRect,
			final int orientation, final int direction) {

		switch (orientation) {
		case SwingConstants.VERTICAL:
			return visibleRect.height / 10;
		case SwingConstants.HORIZONTAL:
			return visibleRect.width / 10;
		default:
			throw new IllegalArgumentException("Invalid orientation: "
					+ orientation);
		}
	}

	@Override
	public int getScrollableBlockIncrement(final Rectangle visibleRect,
			final int orientation, final int direction) {

		switch (orientation) {
		case SwingConstants.VERTICAL:
			return visibleRect.height;
		case SwingConstants.HORIZONTAL:
			return visibleRect.width;
		default:
			throw new IllegalArgumentException("Invalid orientation: "
					+ orientation);
		}
		// set to 16 because that's what you had set in your code.
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {

		return true;
		// track the width, and re-size as needed.
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {

		return false;
		// we don't want to track the height,
		// because we want to scroll vertically.
	}
}
