package ColorChooser;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.List;

import edu.stanford.genetics.treeview.LogBuffer;
import Utilities.GUIFactory;
import Utilities.Helper;

public class ThumbBox {

	protected final ColorPicker colorPicker;
	
	private final Rectangle2D thumbRect = new Rectangle2D.Float();
	
	/* Holds the currently selected thumb */
	private Thumb selectedThumb;
	
	public ThumbBox(ColorPicker colorPicker) {
		
		this.colorPicker = colorPicker;
	}
	
	protected void drawThumbBox(Graphics g) {
		
		/* TODO Replace this with observer in other methods... */
		verifyThumbs();

		final Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		/* Clear thumbRect with background color */
		g2.setColor(GUIFactory.DEFAULT_BG);
		g2.fill(thumbRect);

		Thumb selected_thumb = null;
		
		/* Paint the thumbs */
		for (final Thumb t : colorPicker.getThumbList()) {
			
			/* skip selected thumb to draw it last (keeps it on top) */
			if(t.isSelected()) {
				selected_thumb = t;
				continue;
			}
			t.paint(g2);
		}
		
		/* finally paint selected thumb */
		if(selected_thumb != null) {
			selected_thumb.paint(g2);
		}
	}
	
	/**
	 * Selects a thumb if it is not yet selected, deselects it otherwise.
	 *
	 * @param point
	 */
	protected void selectThumbAtPoint(final Point point) {
		
		for (final Thumb t : colorPicker.getThumbList()) {

			if (t.contains((int) point.getX(), (int) point.getY())) {
				t.setSelected(!t.isSelected());

				selectedThumb = (t.isSelected()) ? t : null;
				break;
			} 
				
			t.setSelected(false);
		}

		colorPicker.getContainerPanel().repaint();
	}
	
	/**
	 * First checks if fraction array is in ascending order, then verifies thumb
	 * positions to match fractions. Returns a boolean so that the boxes won't
	 * be drawn in the paintComponent method, if the fractions aren't ascending.
	 * This would cause an exception with LinearGradientPaint.
	 */
	protected void verifyThumbs() {

		final int x = (int) thumbRect.getMinX();
		final int w = (int) thumbRect.getWidth();
		
		float[] fractions = colorPicker.getFractions();

		for (int i = 1; i < fractions.length - 1; i++) {

			// Avoid rounding errors when casting to int
			final double widthFactor = Math.round(w * fractions[i]);
			final int pos = x + (int) (widthFactor);

			if (!hasThumbForFraction(i)
					&& !((colorPicker.getThumbNumber() == colorPicker.getColorNumber()) 
							&& colorPicker.getThumbNumber() == fractions.length)) {
				insertThumbAt(pos, colorPicker.getColorList().get(i));
			}
		}
	}
	
	/**
	 * Inserts a thumb at a specific x-value and passes a color to the Thumb
	 * constructor.
	 *
	 * @param x
	 * @param color
	 */
	protected void insertThumbAt(final int x, final Color color) {

		List<Thumb> thumbs = colorPicker.getThumbList();
		int index = 0;
		for (final Thumb t : thumbs) {

			if (x > t.getX()) {
				index++;
			}
		}

		final int y = (int) (thumbRect.getHeight());
		thumbs.add(index, new Thumb(x, y, color));
		
		colorPicker.setThumbList(thumbs);
	}

	/**
	 * Sets all thumbs' selection status to 'false'.
	 */
	protected void deselectAllThumbs() {

		List<Thumb> thumbs = colorPicker.getThumbList();
		for (final Thumb t : thumbs) {

			t.setSelected(false);
			selectedThumb = null;
		}
	}

	/**
	 * Updates a thumb position based on a supplied x-coordinate. This is
	 * mainly useful to implement the drag-to-position feature to allow
	 * sliding thumbs.
	 * @param inputX New x-coordinate of the selected thumb.
	 */
	protected void setThumbPosition(int inputX) {

		/* adjust offset */
		inputX -= (int) thumbRect.getMinX();
		
		if (selectedThumb == null) {
			return;
		}
		
		float newFrac = inputX / (float) thumbRect.getWidth();
		
		updateFracsForThumbPos(newFrac);
	}
	
	/**
	 * TODO fractions cannot be dependent on thumb x-coords because a full
	 * data range does not fit into 430 pixels which is the width of the color
	 * gradient box. (-> obv. can only represent 430 data points). Separate
	 * fractions from thumb x-coords and keep thumbList + fractions synced!!
	 * @param inputX
	 */
	protected void updateFracsForThumbPos(final float inputFrac) {

		/* Stay between boundaries */
		if (selectedThumb == null
				|| (inputFrac < 0.0 || inputFrac > 1.0)) {
			return;
		}
		
		/* Cannot move to boundary fractions */
		if(Helper.nearlyEqual(0.0, inputFrac)
				|| Helper.nearlyEqual(1.0, inputFrac)) {
			return;
		}
		
		float[] fractions = colorPicker.getFractions();

		/* get position of previous thumb */
		final int selectedIndex = colorPicker.getThumbList()
				.indexOf(selectedThumb);
		
		/* define the boundaries around active thumb's fraction */
		float selectedFrac = fractions[selectedIndex];
		
		/* defined out of range in case no previous/ next thumb exists */
		float previousFrac = -1.0f;
		float nextFrac = 2.0f;
		
		if (selectedIndex == 0) {
			nextFrac = fractions[selectedIndex + 1];

		} else if (selectedIndex == colorPicker.getThumbNumber() - 1) {
			previousFrac = fractions[selectedIndex - 1];

		} else {
			previousFrac = fractions[selectedIndex - 1];
			nextFrac = fractions[selectedIndex + 1];
		}

		/* get updated x-values */
		final float deltaFrac = inputFrac - selectedFrac;
		final float newFrac = selectedFrac + deltaFrac;

		/* set new thumb position and check for boundaries/ other thumbs */
		if ((previousFrac < newFrac && newFrac < nextFrac)) {
			fractions[selectedIndex] = newFrac;

		} else if (newFrac < previousFrac && !Helper.nearlyEqual(previousFrac, 
				0.0)) {
			colorPicker.swapPositions(selectedIndex, selectedIndex - 1);
			
			fractions[selectedIndex] = fractions[selectedIndex - 1];
			fractions[selectedIndex - 1] = newFrac;

		} else if (newFrac > nextFrac && nextFrac < 1.0) {
			colorPicker.swapPositions(selectedIndex, selectedIndex + 1);
			
			fractions[selectedIndex] = fractions[selectedIndex + 1];
			fractions[selectedIndex + 1] = newFrac;
		}

		colorPicker.setFractions(fractions);
		colorPicker.updateColors();
	}

	/**
	 * Finds if an existing thumb contains the passed coordinate point.
	 * This is used to open an edit dialog for a thumb if it was double-clicked. 
	 * @param point Coordinate point of the clicked area.
	 */
	protected void editClickedThumb(final Point point) {

		int index = 0;
		for (final Thumb t : colorPicker.getThumbList()) {

			if (t.contains((int) point.getX(), (int) point.getY())) {

				openThumbEditDialog(t, index);
				break;
			}
			index++;
		}

		colorPicker.updateColors();
	}
	
	/**
	 * Opens the edit dialog for the currently selected thumb.
	 */
	protected void editSelectedThumb() {
		
		if(selectedThumb != null) {
			openThumbEditDialog(selectedThumb, this.getSelectedThumbIndex());
		}
	}
	
	/**
	 * Uses the currently defined fractions to adjust the thumb positions.
	 */
	protected void adjustThumbsToFractions() {
		
		final int x = (int) thumbRect.getMinX();
		final int w = (int) thumbRect.getWidth();
		
		float[] fractions = colorPicker.getFractions();

		for (int i = 0; i < fractions.length; i++) {

			// Avoid rounding errors when casting to int
			final double widthFactor = Math.round(w * fractions[i]);
			final int pos = x + (int) (widthFactor);
			
			Thumb t = colorPicker.getThumb(i);
			t.setCoords(pos, t.getY());

		}
	}
	
	/**
	 * Opens a JDialog for editing a thumb. 
	 * @param t The thumb which is to be edited.
	 */
	private void openThumbEditDialog(Thumb t, int thumbIndex) {
		
		final EditThumbDialog posInputDialog = new EditThumbDialog(t, 
				thumbIndex, this, colorPicker.getColorList());
		
		double dataVal = posInputDialog.showDialog(
				colorPicker.getContainerPanel());
		
		alignThumbWithDataVal(dataVal);
	}
	
	/**
	 * Finds the correct x-position for a thumb for a given data value.
	 * @param dataVal A data value for which the corresponding thumb's 
	 * x-position is to be determined.
	 */
	protected void alignThumbWithDataVal(double dataVal) {
		
		double minVal = colorPicker.getMinVal();
		double maxVal = colorPicker.getMaxVal();
		double range = colorPicker.getRange();
		
		/* TODO adapt range if values are outside */
		if (dataVal < minVal || dataVal > maxVal) {
			
			/* adapt gradient range if number outside boundaries 
			 * don forget to update local min/max/range variables
			 * */
			
			LogBuffer.println("Entered!");
		}

		double diff = Math.abs(dataVal - minVal);
		final float fraction = (float) (diff / (range));
		
		updateFracsForThumbPos(fraction);
	}
	
	/**
	 * Set the size of the ThumbBox rectangle.
	 * @param start_x X-coordinate of left corner point of the rectangle.
	 * @param start_y Y-coordinate of left corner point of the rectangle.
	 * @param width Width of the rectangle.
	 * @param height Height of the rectangle.
	 */
	protected void setRect(int start_x, int start_y, int width, int height) {
		
		thumbRect.setRect(start_x, start_y, width, height);
	}
	
	/**
	 * Set a new value for the selected thumb.
	 * @param t The new thumb to be set as the selected one.
	 */
	protected void setSelectedThumb(Thumb t) {
		
		this.selectedThumb = t;
	}

	/**
	 * Gets the data value a thumb represents.
	 *
	 * @param t
	 * @return A double value between minimum and maximum of the currently
	 *         relevant data range for coloring.
	 */
	protected double getThumbDataVal(final int thumbIndex) {

		double range = colorPicker.getRange();
		double minVal = colorPicker.getMinVal();
		
		final float fraction = colorPicker.getFractions()[thumbIndex];
		final double value = Math.abs((range) * fraction) + minVal;

		return (double) Math.round(value * 1000) / 1000;
	}
	
	/**
	 * 
	 * @return The Rectangle object used to display the thumbs.
	 */
	protected Rectangle2D getThumbRect() {
		
		return thumbRect;
	}
	
	/**
	 * Finds out which thumb in thumbList is currently selected and returns it.
	 * @return The index of the selected thumb in thumbList. If no thumb is 
	 * selected, the function returns -1.
	 */
	protected int getSelectedThumbIndex() {
		
		int selectedIndex = -1;
		
		if (selectedThumb == null) {
			return selectedIndex;
		}
		
		List<Thumb> thumbs = colorPicker.getThumbList();
		
		selectedIndex = thumbs.indexOf(selectedThumb);

		if (thumbs.get(selectedIndex).getX() == thumbs.get(thumbs.size() - 1)
				.getX()) {
			selectedIndex--;
		}
		
		return selectedIndex;
	}
	
	/**
	 * Checks if a thumb is present for a given fraction.
	 *
	 * @param fracIndex
	 * @return boolean
	 */
	private boolean hasThumbForFraction(final int fracIndex) {

		List<Thumb> thumbs = colorPicker.getThumbList();
		float[] fractions = colorPicker.getFractions();

		if (thumbs.size() > fracIndex) {
			double frac = thumbs.get(fracIndex).getX() / thumbRect.getWidth();
			double frac2 = fractions[fracIndex];

			if (Helper.nearlyEqual(frac, frac2)
					|| thumbs.get(fracIndex).isSelected()) {
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Checks if a thumb is located at a certain point.
	 *
	 * @param point The point to be checked.
	 * @return boolean - Whether a thumb is located at the point or not.
	 */
	protected boolean isPointInThumb(final Point point) {

		boolean containsThumb = false;

		for (final Thumb t : colorPicker.getThumbList()) {

			if (t.contains(point.x, point.y)) {
				containsThumb = true;
				break;
			}
		}

		return containsThumb;
	}
	
	/**
	 * Check if there is any thumb associated with the given data value.
	 * @param dataVal
	 * @return
	 */
	protected boolean hasThumbForDataVal(double dataVal) {
		
		boolean hasThumb = false;
		List<Thumb> thumbs = colorPicker.getThumbList();

		for (final Thumb t : thumbs) {
			
			double tDataVal = getThumbDataVal(thumbs.indexOf(t));
			if (Helper.nearlyEqual(tDataVal, dataVal)) {
				hasThumb = true;
				break;
			}
		}

		return hasThumb;
	}
	
	/**
	 * Requests if selected thumb is currently set or not. If yes, that means
	 * there generally is a thumb in selected status.
	 * @return boolean 
	 */
	protected boolean hasSelectedThumb() {
		
		return (selectedThumb != null);
	}
}


