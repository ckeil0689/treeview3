package gui.colorPicker;

import util.LogBuffer;

import javax.swing.*;
import java.awt.*;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.Rectangle2D;
import java.util.List;

/** A special JPanel that represents a gradient colored box. It has a
 * MouseListener attached (via the controller class) which handles user input
 * and allows for change of color in the clicked area. */
public class GradientBox {

	private ColorPicker colorPicker;

	private final Rectangle2D gradientRect = new Rectangle2D.Float();

	/** Constructs a GradientBox object. */
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

		try {
			// Generating Gradient to fill the rectangle with
			final LinearGradientPaint gradient = new LinearGradientPaint(	startX,
																																		startY,
																																		endX,
																																		startY,
																																		fractions,
																																		colors,
																																		CycleMethod.NO_CYCLE);

			/* Outline of gradient box */
			g2.setColor(Color.black);
			g2.drawRect((int) startX, (int) startY, width, height);

			/* fill gradient box with gradient */
			g2.setPaint(gradient);
			g2.fillRect((int) startX, (int) startY, width, height);

		}
		catch(IllegalArgumentException | NullPointerException e) {
			/* 
			 * The LinearGradientPaint class needs an ascending list of 
			 * fractions to generate the gradient.
			 * If the list gets corrupted and values are not in a strictly
			 * ascending order, an IllegalArgumentException will be thrown.
			 * Rather than crashing the paint code, a black box with a small
			 * hint will be displayed and the user can still move sliders to 
			 * bring the fractions back into a working order.
			 */
			LogBuffer.logException(e);
			fractions = new float[] {0.0f, 0.5f, 1.0f};
			colors = new Color[] {Color.BLACK, Color.BLACK, Color.BLACK};

			final LinearGradientPaint gradient = new LinearGradientPaint(	startX,
																																		startY,
																																		endX,
																																		startY,
																																		fractions,
																																		colors,
																																		CycleMethod.NO_CYCLE);

			/* Outline of gradient box */
			g2.setColor(Color.black);
			g2.drawRect((int) startX, (int) startY, width, height);

			/* fill gradient box with gradient */
			g2.setPaint(gradient);
			g2.fillRect((int) startX, (int) startY, width, height);

			String problem = "Problem! Try to move sliders.";
			g2.setPaint(Color.white);
			g2.drawString(problem, startX + (endX / 4), startY + (height / 2));
		}
	}

	/** Checks if a passed Point is contained in the area of the gradient
	 * rectangle.
	 *
	 * @param point
	 * @return */
	public boolean isGradientArea(final Point point) {

		return gradientRect.contains(point);
	}

	protected void setRect(int start, int left, int width, int height) {

		gradientRect.setRect(start, left, width, height);
	}

	/** Adds a color to the gradient.
	 *
	 * @param newCol */
	public void addColor(final Color newCol) {

		float[] fractions = colorPicker.getFractions();

		/* find largest diff between fractions and set index there */
		int newColorIndex = findAddIndex(fractions);

		final float halfDiff = (fractions[newColorIndex] - fractions[newColorIndex -
																																	1]) / 2;
		final float addFrac = fractions[newColorIndex - 1] + halfDiff;

		colorPicker.getColorList().add(newColorIndex, newCol);
		colorPicker	.getThumbBox()
								.insertThumbForFrac(addFrac, newColorIndex, newCol);

		colorPicker.updateFractions();
		colorPicker.updateColors();
	}

	/** Serves to find the largest space between fractions where a thumb can be
	 * inserted.
	 * 
	 * @param fractions */
	private static int findAddIndex(float[] fractions) {

		int newColIndex = 0;
		float maxDiff = 0.0f;

		for(int i = 0; i < fractions.length - 1; i++) {

			float diff = fractions[i + 1] - fractions[i];
			if(diff > maxDiff) {
				maxDiff = diff;
				newColIndex = i;
			}
		}

		return newColIndex + 1;
	}

	/** Removes a color and its matching thumb from the gradient. */
	public void removeColor() {

		List<Thumb> thumbs = colorPicker.getThumbList();
		List<Color> colorList = colorPicker.getColorList();
		int removeIndex = 0;

		for(final Thumb t : thumbs) {

			if(t.isSelected()) {
				removeIndex = thumbs.indexOf(t);
				thumbs.remove(removeIndex);
				colorList.remove(removeIndex);
				colorPicker.getThumbBox().setSelectedThumb(null);
				break;
			}
		}

		colorPicker.updateFractions();
		colorPicker.updateColors();
	}

	/** Changes the gradient color in the area the mouse was clicked on.
	 *
	 * @param newCol
	 * @param point
	 * @return */
	public boolean changeColor(final Point point) {

		List<Thumb> thumbs = colorPicker.getThumbList();
		List<Color> colorList = colorPicker.getColorList();

		Color newCol = null;

		final int clickPos = (int) point.getX();
		int index = 0;
		int distance = ColorPicker.WIDTH;

		for(final Thumb t : thumbs) {
			if(Math.abs(t.getX() - clickPos) < distance) {
				distance = Math.abs(t.getX() - clickPos);
				index = thumbs.indexOf(t);
			}
		}

		if(!(index < colorList.size())) {
			LogBuffer.println("Tried to remove color outside of color list bounds.");
			return false;
		}

		JPanel panel = colorPicker.getContainerPanel();
		Color oldColor = thumbs.get(index).getColor();
		newCol = JColorChooser.showDialog(panel, "Pick a Color", oldColor);

		boolean changed = false;
		if(newCol != null && !newCol.equals(oldColor)) {
			colorList.set(index, newCol);
			thumbs.get(index).setColor(newCol);
			colorPicker.updateColors();
			changed = true;
		}

		return changed;
	}
}
