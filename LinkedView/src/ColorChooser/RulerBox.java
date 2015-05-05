package ColorChooser;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public class RulerBox {

	private final Rectangle2D rulerRect = new Rectangle2D.Float();
	
	/**
	 * Draws a ruler with markings.
	 * @param g2
	 */
	protected void drawRulerBox(final Graphics2D g2) {

		g2.setColor(Color.black);

		final int minY = (int) rulerRect.getMinY();
		final int minX = (int) rulerRect.getMinX();
		final int maxX = (int) rulerRect.getMaxX();

		for (int x = minX; x < maxX + 1; x++) {

			if (x == minX || x == maxX) {
				g2.drawLine(x, minY, x, minY + 10);

			} else if ((x - minX) % 50 == 0) {
				g2.drawLine(x, minY, x, minY + 5);
			}
		}
	}
	
	protected void setRect(int start_x, int start_y, int width, int height) {
		
		rulerRect.setRect(start_x, start_y, width, height);
	}
	
	protected Rectangle2D getRulerRect() {
		
		return rulerRect;
	}
}
