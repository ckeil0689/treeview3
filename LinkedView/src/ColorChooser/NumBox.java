package ColorChooser;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.LogBuffer;

public class NumBox {
	
	private final ColorPicker colorPicker;

	private final FontMetrics fm;

	private final Rectangle2D numRect = new Rectangle2D.Float();
	
	public NumBox(ColorPicker colorPicker) {
		
		this.colorPicker = colorPicker;
		
		/* Font details for text-alignment in numBox */
		this.fm = colorPicker.getContainerPanel()
				.getFontMetrics(GUIFactory.FONTS);
	}

	protected void drawNumBox(final Graphics2D g2) {

		g2.setColor(GUIFactory.DEFAULT_BG);
		g2.fill(numRect);

		g2.setColor(Color.black);
		g2.setFont(GUIFactory.FONTS);
		
		List<Thumb> thumbs = colorPicker.getThumbList();
		float[] fractions = colorPicker.getFractions();
		double range = colorPicker.getRange();
		double minVal = colorPicker.getMinVal();

		 if (thumbs.size() == fractions.length) {
			 int i = 0;
			 for (final Thumb t : thumbs) {
				 // Rounding to 3 decimals
				 final float fraction = fractions[i++];
				 double value = Math.abs((range) * fraction) + minVal;
				 value = (double) Math.round(value * 1000) / 1000;
				
				 /* Center string using font metrics to get string width */
				 String value_s = Double.toString(value);
				 int stringWidth = fm.stringWidth(value_s);
				 
				 g2.drawString(Double.toString(value), 
						 t.getX() - (stringWidth/2), 
						 (int) ((numRect.getHeight() / 2) + numRect.getMinY()));
			 }
		 } else {
			 LogBuffer.println("ThumbList size (" + thumbs.size()
			 + ") and fractions size (" + fractions.length
			 + ") are different in drawNumbBox!");
		 }

		/* Draw first number */
//		final double first = minVal;
//		int x = (int) numRect.getMinX();
//
//		g2.drawString(Double.toString(first), x,
//				(int) ((numRect.getHeight() / 2) + numRect.getMinY()));
//
//		final double last = maxVal;
//		x = (int) numRect.getMaxX();
//		final int stringWidth = fm.stringWidth(Double.toString(last));
//
//		g2.drawString(Double.toString(last), x - stringWidth,
//				(int) ((numRect.getHeight() / 2) + numRect.getMinY()));
	}
	
	protected void setRect(int start_x, int start_y, int width, int height) {
		
		numRect.setRect(start_x, start_y, width, height);
	}
	
	protected Rectangle2D getNumRect() {
		
		return numRect;
	}
}
