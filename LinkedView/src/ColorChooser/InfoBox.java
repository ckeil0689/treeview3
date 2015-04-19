package ColorChooser;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.JPanel;

import edu.stanford.genetics.treeview.LogBuffer;
import Utilities.GUIFactory;

public class InfoBox {//extends JPanel{
	
//	private static final long serialVersionUID = 1L;
	
	private final ColorPicker colorPicker;

	private final FontMetrics fm;

	private final Rectangle2D rulerRect = new Rectangle2D.Float();
	private final Rectangle2D numRect = new Rectangle2D.Float();
	
	/* Data boundaries */
//	private final double minVal;
//	private final double maxVal;
	
	public InfoBox(ColorPicker colorPicker) {
		
		this.colorPicker = colorPicker;
		
		/* Font details for text-alignment in numBox */
		this.fm = colorPicker.getContainerPanel()
				.getFontMetrics(GUIFactory.FONTS);
	}
	
//	@Override
//	public void paintComponent(final Graphics g) {
//
//		super.paintComponent(g);
//
//		final Graphics2D g2 = (Graphics2D) g;
//		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//				RenderingHints.VALUE_ANTIALIAS_ON);
//		
//		drawRulerBox(g2);
//		drawNumBox(g2);
//	}
	
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

	protected void drawNumBox(final Graphics2D g2) {

		g2.setColor(GUIFactory.DEFAULT_BG);
		g2.fill(numRect);

		g2.setColor(Color.black);
		g2.setFont(GUIFactory.FONTS);
		
		List<Thumb> thumbs = colorPicker.getThumbList();
		float[] fractions = colorPicker.getFractions();
		double range = colorPicker.getRange();
		double minVal = colorPicker.getMinVal();

//		 Paint the thumb values
		 if (thumbs.size() == fractions.length) {
			 int i = 0;
			 for (final Thumb t : thumbs) {
				 // Rounding to 3 decimals
				 final float fraction = fractions[i++];
				 double value = Math.abs((range) * fraction) + minVal;
				 value = (double) Math.round(value * 1000) / 1000;
				
				 g2.drawString(Double.toString(value), t.getX(), 
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
	
	protected void setRect(int start, int left, int width, int height) {
		
		rulerRect.setRect(start, left - 10, width, height);
		numRect.setRect(start, left, width, height);
		
	}
	
	protected Rectangle2D getNumRect() {
		
		return numRect;
	}
	
	protected Rectangle2D getRulerRect() {
		
		return rulerRect;
	}
}
