package colorChooser;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import util.GUIFactory;

public class BoundaryBox extends ThumbBox {

	private boolean isMin;

	private final FontMetrics fm;

	private Thumb thumb;

	private final Rectangle2D boundaryRect = new Rectangle2D.Float();

	/**
	 * Constructs a GradientBox object.
	 */
	public BoundaryBox(ColorPicker cP, BoundaryThumb thumb, boolean isMin) {

		super(cP);

		this.isMin = isMin;
		this.fm = colorPicker.getContainerPanel().getFontMetrics(
				GUIFactory.FONTS);
		this.thumb = thumb;
	}

	protected void drawBoundaryBox(final Graphics2D g2) {

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		/* Clear thumbRect with background color */
		g2.setColor(GUIFactory.DEFAULT_BG);
		g2.fill(boundaryRect);

		double value;
		if (isMin) {
			value = colorPicker.getMinVal();
		} else {
			value = colorPicker.getMaxVal();
		}

		paintString(g2, value, thumb);
		thumb.paint(g2);
	}

	private void paintString(Graphics2D g2, double value, Thumb t) {

		NumberFormat formatter = new DecimalFormat("##0.0##");
		String value_s = formatter.format(value);
		int stringWidth = fm.stringWidth(value_s);
		final double nonSciLimit = 0.0001;
		
		// adapt to scientific notation if String still too long...
		if(stringWidth > boundaryRect.getWidth() 
				|| Math.abs(value) < nonSciLimit) {
			formatter = new DecimalFormat("##0.##E0");
			value_s = formatter.format(value);
			stringWidth = fm.stringWidth(value_s);
		}
		
		int x = (int) Math.round(t.getX() - (stringWidth/ 2.0));
		int y = (int) Math.round((boundaryRect.getHeight()/ 3.0) 
				+ boundaryRect.getMinY());

		g2.setColor(Color.black);
		g2.setFont(GUIFactory.FONTS);
		g2.drawString(value_s, x, y);
	}

	@Override
	protected void setRect(int start_x, int start_y, int width, int height) {

		boundaryRect.setRect(start_x, start_y, width, height);

		int thumb_x = start_x + (width / 2);
		int thumb_y = start_y + (height / 2);

		thumb.setCoords(thumb_x, thumb_y);
	}

	/**
	 * 
	 * @return The Rectangle object used to display the thumbs.
	 */
	protected Rectangle2D getBoundaryRect() {

		return boundaryRect;
	}
	
	/**
	 * Update the current thumb defined for this BoundaryBox.
	 * @param bT - The new BoundaryThumb object.
	 */
	protected void setNewThumb(final BoundaryThumb bT) {
		this.thumb = bT;
	}
}
