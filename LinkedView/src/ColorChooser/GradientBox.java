package ColorChooser;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;

import javax.swing.JColorChooser;
import javax.swing.JPanel;

import Utilities.Helper;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * A special JPanel that represents a gradient colored box. It has a
 * MouseListener attached (via the controller class) which handles user input
 * and allows for change of color in the clicked area.
 */
public class GradientBox {
	
	private ColorPicker colorPicker;

	private final Rectangle2D gradientRect = new Rectangle2D.Float();

	/**
	 * Constructs a GradientBox object.
	 */
	public GradientBox(ColorPicker colorPicker) {

		this.colorPicker = colorPicker;
	}
	
	protected void drawGradientBox(final Graphics2D g2) {

		// Dimensions
		final float startX = (float) gradientRect.getMinX();
		final float startY = (float) gradientRect.getMinY();

		final float endX = (float) gradientRect.getMaxX();

		final int height = (int) gradientRect.getHeight();
		final int width = (int) gradientRect.getWidth();
		
		float[] fractions = colorPicker.getFractions();
		Color[] colors = colorPicker.getColors();

		// Generating Gradient to fill the rectangle with
		final LinearGradientPaint gradient = new LinearGradientPaint(startX,
				startY, endX, startY, fractions, colors, CycleMethod.NO_CYCLE);

		/* Outline of gradient box */
		g2.setColor(Color.black);
		g2.drawRect((int) startX, (int) startY, width, height);

		/* fill gradient box with gradient */
		g2.setPaint(gradient);
		g2.fillRect((int) startX, (int) startY, width, height);
	}
	
	/**
	 * Checks if a passed Point is contained in the area of the gradient
	 * rectangle.
	 *
	 * @param point
	 * @return
	 */
	protected boolean isGradientArea(final Point point) {

		return gradientRect.contains(point);
	}
	
	protected void setRect(int start, int left, int width, int height) {
		
		gradientRect.setRect(start, left, width, height);
	}
	
	/**
	 * Adds a color to the gradient.
	 *
	 * @param newCol
	 */
	protected void addColor(final Color newCol) {
		
		List<Thumb> thumbs = colorPicker.getThumbList();
		float[] fractions = colorPicker.getFractions();
		
		/* find largest diff between fractions */
		int newColIndex = 0;
		float maxDiff = 0.0f;
		for(int i = 0; i < fractions.length - 1; i++) {
			
			float diff = fractions[i+1] - fractions[i];
			if(diff > maxDiff) {
				maxDiff = diff;
				newColIndex = i;
			}
		}
		
		final float halfDiff = (fractions[newColIndex + 1] 
				- fractions[newColIndex]) / 2;
		final float addFrac = fractions[newColIndex] + halfDiff;
		
		float[] newFractions = new float[fractions.length + 1];
		
		/** TODO avoid this.... 
		 * break method into smaller methods...*/
		newColIndex++;
		
		int j = 0;
		for(int i = 0; i < newFractions.length; i++) {
			
			if(i == newColIndex) {
				newFractions[i] = addFrac;
				continue;
			}
			newFractions[i] = fractions[j++];
		}
		
		LogBuffer.println("New fractions: " + Arrays.toString(newFractions));
		
		final int x = (int) (addFrac * gradientRect.getWidth());

		colorPicker.getColorList().add(newColIndex, newCol);
		colorPicker.getThumbBox().insertThumbAt(x, newCol);
		colorPicker.setFractions(newFractions);

		if (thumbs.size() != fractions.length) {
			System.out.println("ThumbList size (" + thumbs.size()
					+ ") and fractions size (" + fractions.length
					+ ") are different in drawNumbBox!");
		}

		colorPicker.updateColors();
	}
	
	/**
	 * Removes a color and its matching thumb from the gradient.
	 */
	protected void removeColor() {

		float[] fractions = colorPicker.getFractions();
		List<Thumb> thumbs = colorPicker.getThumbList();
		List<Color> colorList = colorPicker.getColorList();
		int removeIndex = 0;
		boolean updated = false;
		
		for (final Thumb t : thumbs) {

			if (t.isSelected()) {
				removeIndex = thumbs.indexOf(t);
				thumbs.remove(removeIndex);
				colorList.remove(removeIndex);
				colorPicker.getThumbBox().setSelectedThumb(null);
				updated = true;
				break;
			}
		}

		if(updated) {
			/* update fractions */
			float[] newFractions = new float[fractions.length - 1];
			
			int j = 0;
			for(int i = 0; i < fractions.length; i++) {
				
				if(i == removeIndex) {
					continue;
				}
				newFractions[j++] = fractions[i];
			}
	
			if (thumbs.size() != fractions.length) {
				System.out.println("ThumbList size (" + thumbs.size()
						+ ") and fractions size (" + fractions.length
						+ ") are different in drawNumbBox!");
			}
	
			colorPicker.setFractions(newFractions);
			colorPicker.updateColors();
		}
	}
	
	/**
	 * Changes the gradient color in the area the mouse was clicked on.
	 *
	 * @param newCol
	 * @param point
	 */
	protected void changeColor(final Point point) {

		List<Thumb> thumbs = colorPicker.getThumbList();
		List<Color> colorList = colorPicker.getColorList();
		
		Color newCol = null;

		final int clickPos = (int) point.getX();
		int index = 0;
		int distance = ColorPicker.WIDTH;

		for (final Thumb t : thumbs) {

			if (Math.abs(t.getX() - clickPos) < distance) {
				distance = Math.abs(t.getX() - clickPos);
				index = thumbs.indexOf(t);
			}
		}

		JPanel panel = colorPicker.getContainerPanel();
		newCol = JColorChooser.showDialog(panel, "Pick a Color",
				thumbs.get(index).getColor());

		if (newCol != null) {
			colorList.set(index, newCol);
			thumbs.get(index).setColor(newCol);
			
			colorPicker.updateColors();
		}
	}
}
