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
	
	/* Even number please! It's the total for both sides of the string. */
	private final int TOTAL_MARGIN = 10;

	private final Rectangle2D numRect = new Rectangle2D.Float();
	
	public NumBox(ColorPicker colorPicker) {
		
		this.colorPicker = colorPicker;
		
		/* Font details for text-alignment in numBox */
		this.fm = colorPicker.getContainerPanel()
				.getFontMetrics(GUIFactory.FONTS);
	}

	protected void drawNumBox(final Graphics2D g2) {

		/* clear box before repainting thumb numbers */
		g2.setColor(GUIFactory.DEFAULT_BG);
		g2.fill(numRect);

		g2.setFont(GUIFactory.FONTS);
		
		List<Thumb> thumbs = colorPicker.getThumbList();
		float[] fractions = colorPicker.getFractions();
		double range = colorPicker.getRange();
		double minVal = colorPicker.getMinVal();
		
		/* store the selected thumb because it always has to be painted last */
		Thumb selected_thumb = null;
		int frac_index = -1;

		if (thumbs.size() == fractions.length) {
			 int i = 0;
			 for (final Thumb t : thumbs) {
				 
				 if(t.isSelected()) {
					 /* store a reference to t for later */
					 selected_thumb = t;
					 frac_index = i++;
					 continue;
				 }
				 
				 paintString(g2, fractions[i++], t, range, minVal);
			 }
			 
			 /* 
			  * selected thumb NEEDS to be painted last because it should always
			  * be on top over the other values.
			  */
			 if (selected_thumb != null) {
				 paintString(g2, fractions[frac_index], selected_thumb, 
						 range, minVal);
			 }
			 
		 } else {
			 LogBuffer.println("ThumbList size (" + thumbs.size()
			 + ") and fractions size (" + fractions.length
			 + ") are different in drawNumbBox!");
		 }
	}
	
	private void paintString(Graphics2D g2, float fraction, Thumb t, 
			double range, double minVal) {
		
		double value = Math.abs((range) * fraction) + minVal;
		value = (double) Math.round(value * 1000) / 1000;
		String value_s = Double.toString(value);
		int stringWidth = fm.stringWidth(value_s);
		
		g2.setColor(GUIFactory.DEFAULT_BG);
		g2.fillRect(t.getX() - (stringWidth / 2) - (TOTAL_MARGIN / 2), 
				0, stringWidth + TOTAL_MARGIN, (int)numRect.getHeight());
		g2.setColor(Color.black);
		
		g2.drawString(Double.toString(value),  t.getX() - (stringWidth/2), 
				(int) ((numRect.getHeight() / 2) + numRect.getMinY()));
	}
	
	protected void setRect(int start_x, int start_y, int width, int height) {
		
		numRect.setRect(start_x, start_y, width, height);
	}
	
	protected Rectangle2D getNumRect() {
		
		return numRect;
	}
}
