package colorChooser;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import util.GUIFactory;

public class NumBox {

	private final ColorPicker colorPicker;

	private final FontMetrics fm;

	/* Even number please! It's the total for both sides of the string. */
	private final int TOTAL_MARGIN = 10;

	private final Rectangle2D numRect = new Rectangle2D.Float();

	public NumBox(ColorPicker colorPicker) {

		this.colorPicker = colorPicker;

		/* Font details for text-alignment in numBox */
		this.fm = colorPicker.getContainerPanel().getFontMetrics(
				GUIFactory.FONTS);
	}

	protected void drawNumBox(final Graphics2D g2) {

		/* clear box before repainting thumb numbers */
		g2.setColor(GUIFactory.DEFAULT_BG);
		g2.fill(numRect);

		g2.setFont(GUIFactory.FONTS);

		List<Thumb> thumbs = colorPicker.getThumbList();

		// store the selected thumb because it always has to be painted last
		Thumb selected_thumb = null;
		for (final Thumb t : thumbs) {
			// do not paint min/ max thumb data values here
			if (thumbs.indexOf(t) == 0
					|| thumbs.indexOf(t) == thumbs.size() - 1) {
				continue;
			}

			if (t.isSelected()) {
				// store a reference to t for later
				selected_thumb = t;
				continue;
			}

			paintString(g2, t);
		}

		/*
		 * selected thumb NEEDS to be painted last because it should always be
		 * on top over the other values.
		 */
		if (selected_thumb != null) {
			paintString(g2, selected_thumb);
		}
	}

	/**
	 * Paints a background rectangle and a number string which represents
	 * the assigned data value of the passed thumb.
	 * @param g2 - The Graphics object to paint to.
	 * @param t - The thumb for which the data value will be painted.
	 */
	private void paintString(final Graphics2D g2, final Thumb t) {

		String value_s = Double.toString(t.getDataValue());
		int stringWidth = fm.stringWidth(value_s);

		// painting background rectangle for overlap case
		g2.setColor(GUIFactory.DEFAULT_BG);
		g2.fillRect(t.getX() - (stringWidth / 2) - (TOTAL_MARGIN / 2), 0,
				stringWidth + TOTAL_MARGIN, (int) numRect.getHeight());
		
		// paint the number string
		g2.setColor(Color.black);
		g2.drawString(value_s, t.getX() - (stringWidth / 2),
				(int) ((numRect.getHeight() / 2) + numRect.getMinY()));
	}

	protected void setRect(int start_x, int start_y, int width, int height) {

		numRect.setRect(start_x, start_y, width, height);
	}

	protected Rectangle2D getNumRect() {

		return numRect;
	}
}
