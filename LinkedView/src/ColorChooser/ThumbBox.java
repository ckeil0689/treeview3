package ColorChooser;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import Utilities.GUIFactory;
import Utilities.Helper;
import edu.stanford.genetics.treeview.LogBuffer;

public class ThumbBox {

	/*
	 * When mouse is quickly moved outside thumbBox we need to max out the thumb
	 * on the edge. However, it cannot have the same data value as the max or
	 * min thumb because then its position cannot be updated. Thumbs cannot be
	 * stacked. So a small correction is needed to put the non-boundary thumbs
	 * on the edges of thumb box.
	 */
	private final static double CORRECTION = 0.001;

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
			if (t.isSelected()) {
				selected_thumb = t;
				continue;
			}
			t.paint(g2);
		}

		/* finally paint selected thumb */
		if (selected_thumb != null) {
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

		float[] fractions = colorPicker.getFractions();

		for (int i = 1; i < fractions.length - 1; i++) {

			if (!hasThumbForFraction(i) && !colorPicker.isSynced()) {
				insertThumbAt(fractions[i], i, colorPicker.getColorList()
						.get(i));
			}
		}
	}

	/**
	 * Inserts a thumb at a specific x-value and passes a color to the Thumb
	 * constructor.
	 *
	 * @param frac
	 *            The fraction for which to insert a thumb.
	 * @param color
	 *            The color of the thumb to be inserted.
	 */
	protected void insertThumbAt(final float frac, final int index,
			final Color color) {

		final int offset = ColorPicker.OFFSET;
		final int x = offset + (int) (frac * ColorPicker.WIDTH);

		List<Thumb> thumbs = colorPicker.getThumbList();

		final int y = ColorPicker.THUMB_HEIGHT;
		double dataValue = colorPicker.getDataFromFraction(frac);

		Thumb newThumb = new Thumb(x, y, color);
		newThumb.setValue(dataValue);

		thumbs.add(index, newThumb);
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
	 * Updates a thumb position based on a supplied x-coordinate. This is mainly
	 * useful to implement the drag-to-position feature to allow sliding thumbs.
	 * 
	 * @param inputX - New x-coordinate of the selected thumb.
	 */
	protected void moveInnerThumbTo(int inputX) {

		if (selectedThumb == null || selectedThumb instanceof BoundaryThumb) {
			return;
		}

		double dataVal;

		/* Max out on edges for inner thumbs */
		if (inputX < (int) thumbRect.getMinX()) {
			dataVal = colorPicker.getMinVal() + CORRECTION;

		} else if (inputX > (int) thumbRect.getMaxX()) {
			dataVal = colorPicker.getMaxVal() - CORRECTION;

		} else {
			/* adjust offset */
			inputX -= (int) thumbRect.getMinX();

			float newFrac = inputX / (float) thumbRect.getWidth();
			dataVal = colorPicker.getDataFromFraction(newFrac);
		}

		dataVal = Helper.roundDouble(dataVal, 3);
		updateSelectedThumbToVal(dataVal);
	}

	/**
	 * Ensures that the supplied data value is represented by a thumb at the
	 * correct index of the thumb list and color list and that the data value
	 * is assigned to a thumb.
	 * 
	 * @param dataVal - The data value to handle.
	 */
	protected void updateSelectedThumbToVal(final double dataVal) {

		/* Stay between boundaries */
		if (selectedThumb == null || dataVal < colorPicker.getMinVal()
				|| dataVal > colorPicker.getMaxVal()) {
			return;
		}

		/* Cannot move to boundary fractions */
		if (Helper.nearlyEqual(colorPicker.getMinVal(), dataVal)
				|| Helper.nearlyEqual(colorPicker.getMaxVal(), dataVal)) {
			return;
		}

		List<Thumb> thumbs = colorPicker.getThumbList();
		double[] thumbDataVals = new double[colorPicker.getThumbNumber()];

		for (int i = 0; i < thumbDataVals.length; i++) {
			//thumbDataVals[i] = calcThumbVal(i);
			thumbDataVals[i] = thumbs.get(i).getDataValue();
		}

		/* get position of previous thumb */
//		List<Thumb> thumbs = colorPicker.getThumbList();
		int selectedIndex = thumbs.indexOf(selectedThumb);

		/* define the boundaries around active thumb's fraction */
		/* defined out of range in case no previous/ next thumb exists */
		double previousData = colorPicker.getMinVal();
		double nextData = colorPicker.getMaxVal();

		/* deal with boundary cases */
		if (selectedIndex == 0) {
			boolean isSafeShrink = dataVal < thumbDataVals[selectedIndex + 1];
			int removed = 0;
			if (!isSafeShrink) {
				removed = shrinkThumbsToDataRange(dataVal, true);
			}
			int nextIndex = selectedIndex + 1 + removed;
			previousData = -1.0 * Double.MAX_VALUE;
			nextData = thumbDataVals[nextIndex];
			colorPicker.setMinVal(dataVal); // calls updateFractions()...maybe
												// theres a better way

		} else if (selectedIndex == colorPicker.getThumbNumber() - 1) {
			boolean isSafeShrink = (dataVal > thumbDataVals[selectedIndex - 1]);
			int removed = 0;
			if (!isSafeShrink) {
				removed = shrinkThumbsToDataRange(dataVal, false);
				selectedIndex -= removed;
			}
			int prevIndex = selectedIndex - 1;
			previousData = thumbDataVals[prevIndex];
			nextData = Double.MAX_VALUE;
			colorPicker.setMaxVal(dataVal);

			/* Non-boundary */
		} else {
			previousData = thumbDataVals[selectedIndex - 1];
			nextData = thumbDataVals[selectedIndex + 1];
		}

		/* set new thumb position and check for boundaries/ other thumbs */
		if ((previousData < dataVal && dataVal < nextData)) {
			thumbs.get(selectedIndex).setValue(dataVal);

		} else if (dataVal < previousData
				&& !Helper.nearlyEqual(previousData, colorPicker.getMinVal())) {
			colorPicker.swapPositions(selectedIndex, selectedIndex - 1);

			thumbs.get(selectedIndex).setValue(previousData);
			thumbs.get(selectedIndex - 1).setValue(dataVal);

		} else if (dataVal > nextData && nextData < colorPicker.getMaxVal()) {
			colorPicker.swapPositions(selectedIndex, selectedIndex + 1);

			thumbs.get(selectedIndex).setValue(nextData);
			thumbs.get(selectedIndex + 1).setValue(dataVal);
		}

		colorPicker.updateFractions();
		colorPicker.updateColors();
	}

	/**
	 * Shrinks the thumb and color lists so that new min/max can be set.
	 * 
	 * @param boundaryData
	 *            The new min or max data value.
	 * @param isMin
	 *            Whether the affected boundary is the minimum.
	 * @return Number of removed thumbs.
	 */
	private int shrinkThumbsToDataRange(double boundaryData, boolean isMin) {

		List<Thumb> thumbs = colorPicker.getThumbList();
		Iterator<Thumb> iter = thumbs.iterator();

		int removed = 0;

		while (iter.hasNext()) {

			Thumb t = iter.next();

			/* Never remove boundary thumbs */
			if (thumbs.indexOf(t) == 0
					|| thumbs.indexOf(t) == thumbs.size() - 1) {
				continue;
			}

			double tData = t.getDataValue();
			if (isMin
					&& (tData < boundaryData || Helper.nearlyEqual(tData,
							boundaryData))) {
				colorPicker.getColorList().remove(thumbs.indexOf(t));
				iter.remove();
				removed++;
			}

			if (!isMin
					&& (tData > boundaryData || Helper.nearlyEqual(tData,
							boundaryData))) {
				colorPicker.getColorList().remove(thumbs.indexOf(t));
				iter.remove();
				removed++;
			}
		}

		colorPicker.updateFractions();

		return removed;
	}

	/**
	 * Finds if an existing thumb contains the passed coordinate point. This is
	 * used to open an edit dialog for a thumb if it was double-clicked.
	 * 
	 * @param point
	 *            Coordinate point of the clicked area.
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

		int selected = getSelectedThumbIndex();

		if (selected > -1) {
			openThumbEditDialog(selectedThumb, selected);
		}
	}

	/**
	 * Uses the currently defined fractions to adjust the thumb positions.
	 */
	protected void adjustThumbsToFractions() {

		float[] fractions = colorPicker.getFractions();

		if (fractions.length != colorPicker.getThumbNumber()) {
			LogBuffer.println("Could not adjust thumb fractions. "
					+ "Unequal thumb list and fraction list sizes.");
			return;
		}

		/* Update every thumb */
		for (int i = 0; i < fractions.length; i++) {
			final int pos = getXPosFromFraction(fractions[i]);

			Thumb t = colorPicker.getThumb(i);
			t.setCoords(pos, t.getY());
		}
	}

	/**
	 * Determines the x-position for a thumb using the given fraction. The
	 * width of the ColorPicker is a constant. The x-position of a thumb is the
	 * fraction of the the ColorPicker width while considering an offset. 
	 * 
	 * @param frac - The fraction which will be used to determine the x-position.
	 * @return The x-position of the thumb which represents the given fraction.
	 */
	protected int getXPosFromFraction(float frac) {

		int x = ColorPicker.OFFSET;
		int w = ColorPicker.WIDTH;

		// Avoid rounding errors when casting to int
		final double widthFactor = Math.round(w * frac);
		final int pos = x + (int) (widthFactor);

		return pos;
	}

	/**
	 * Opens a JDialog for editing a thumb.
	 * 
	 * @param t
	 *            The thumb which is to be edited.
	 */
	private void openThumbEditDialog(Thumb t, int thumbIndex) {

		final EditThumbDialog posInputDialog = new EditThumbDialog(t,
			thumbIndex,this,colorPicker.getColorList(),colorPicker.getMean(),
			colorPicker.getMedian(),colorPicker.getDataCenter(),
			colorPicker.getDataMin(),colorPicker.getDataMax());
		
		double initVal = t.getDataValue();
		double dataVal = posInputDialog.showDialog(colorPicker
				.getContainerPanel());
		
		// No position change occurred
		if(dataVal == initVal) {
			return;
		}

		if(t instanceof BoundaryThumb) {
			moveBoundaryThumbTo((BoundaryThumb)t, dataVal);
			
		} else {
			t.setValue(dataVal);
			alignThumbWithVal(dataVal);
		}
	}

	/**
	 * Decides how to handle a data value and whether to extend the minimum
	 * or maximum of the ColorPicker range.
	 * @param dataVal - The data value to handle.
	 */
	protected void alignThumbWithVal(final double dataVal) {

		double minVal = colorPicker.getMinVal();
		double maxVal = colorPicker.getMaxVal();

		if (dataVal < minVal) {
			colorPicker.setMinVal(dataVal);

		} else if (dataVal > maxVal) {
			colorPicker.setMaxVal(dataVal);

		} else {
			updateSelectedThumbToVal(dataVal);
		}
	}

	/**
	 * Set the size of the ThumbBox rectangle.
	 * 
	 * @param start_x
	 *            X-coordinate of left corner point of the rectangle.
	 * @param start_y
	 *            Y-coordinate of left corner point of the rectangle.
	 * @param width
	 *            Width of the rectangle.
	 * @param height
	 *            Height of the rectangle.
	 */
	protected void setRect(int start_x, int start_y, int width, int height) {

		thumbRect.setRect(start_x, start_y, width, height);
	}

	/**
	 * Set a new value for the selected thumb.
	 * 
	 * @param t
	 *            The new thumb to be set as the selected one.
	 */
	protected void setSelectedThumb(Thumb t) {

		this.selectedThumb = t;

		if (t != null) {
			t.setSelected(true);
		}
	}

	/**
	 * Calculates the data value which the thumb at the given index represents.
	 *
	 * @param t
	 * @return A double value between minimum and maximum of the currently
	 *         relevant data range for coloring.
	 */
	protected double calcThumbVal(final int thumbIndex) {

		double range = colorPicker.getRange();
		double minVal = colorPicker.getMinVal();

		final float fraction = colorPicker.getFractions()[thumbIndex];
		final double value = Math.abs((range) * fraction) + minVal;

		return Helper.roundDouble(value, 3);
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
	 * 
	 * @return The index of the selected thumb in thumbList. If no thumb is
	 *         selected, the function returns -1.
	 */
	protected int getSelectedThumbIndex() {

		if (selectedThumb == null) {
			return -1;
		}

		return colorPicker.getThumbList().indexOf(selectedThumb);
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

			if (Helper.nearlyEqual(frac, frac2)) {
				// || thumbs.get(fracIndex).isSelected()) { /* TODO bug */
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if a thumb is located at a certain point.
	 *
	 * @param point
	 *            The point to be checked.
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
	 * 
	 * @param dataVal
	 * @return
	 */
	protected boolean hasThumbForVal(double dataVal) {

		boolean hasThumb = false;
		List<Thumb> thumbs = colorPicker.getThumbList();

		for (final Thumb t : thumbs) {

			double tDataVal = calcThumbVal(thumbs.indexOf(t));
			if (Helper.nearlyEqual(tDataVal, dataVal)) {
				hasThumb = true;
				break;
			}
		}

		return hasThumb;
	}
	
	/**
	 * Check if there is any thumb associated with the given data value. If yes,
	 * delete it.
	 * 
	 * @param dataVal - The data value for which to remove a thumb.
	 * @return Thumb index in data
	 */
	protected void removeThumbWithVal(double dataVal) {

		List<Thumb> thumbs = colorPicker.getThumbList();

		for (final Thumb t : thumbs) {
			int idx = thumbs.indexOf(t);
			double tDataVal = calcThumbVal(idx);
			if (Helper.nearlyEqual(tDataVal, dataVal)) {
				thumbs.remove(idx);
				colorPicker.getColorList().remove(idx);
				break;
			}
		}
		
		colorPicker.updateFractions();
		colorPicker.updateColors();
	}
	
	/**
	 * Replaces a thumb and color at the specified index.
	 * 
	 * @param origin - The index from which to move the thumb.
	 * @param destination - The index to which the thumb should be moved.
	 * @param t - The thumb to replace the old thumb with
	 */
	protected void moveBoundaryToInnerThumb(final int origin,
	                                        final Thumb t) {

		if(colorPicker == null) {
			LogBuffer.println("ColorPicker was null. Could not replace thumb.");
			return;
		}
		
		List<Thumb> thumbs = colorPicker.getThumbList();
		List<Color> colors = colorPicker.getColorList();

		thumbs.set(origin, t);
		colors.set(origin, t.getColor());
		
		setSelectedThumb(t);
		updateSelectedThumbToVal(t.getDataValue());
		
		colorPicker.updateFractions();
		colorPicker.updateColors();
	}
	
	/**
	 * Replaces a boundary thumb and the color at the specified index.
	 * 
	 * @param bT - The boundary thumb to replace the old boundary thumb with
	 */
	protected void replaceBoundaryThumb(final BoundaryThumb bT) {

		if(colorPicker == null) {
			LogBuffer.println("ColorPicker was null. Could not replace thumb.");
			return;
		}
		
		List<Thumb> thumbs = colorPicker.getThumbList();
		List<Color> colors = colorPicker.getColorList();

		int replaceIdx = (bT.isMin()) ? 0 : thumbs.size() - 1;
		
		// replace
		thumbs.set(replaceIdx, bT);
		colors.set(replaceIdx, bT.getColor());
		
		colorPicker.updateFractions();
		colorPicker.updateColors();
	}
	
	/**
	 * This method takes care of appropriately moving boundary thumbs to the
	 * specified user input. Boundary thumbs can be any value, even outside of 
	 * the currently loaded data set. They can also pass inner thumbs in either
	 * direction. In that case, the outer-most inner thumb will become a new 
	 * boundary. The previous boundary thumb will become an inner thumb at the
	 * specified location.
	 * @param bT - The boundary thumb to be moved.
	 * @param dataVal - The data value at which to place the boundary thumb.
	 */
	protected void moveBoundaryThumbTo(final BoundaryThumb bT, 
	                                   final double dataVal) {
		
		if(colorPicker == null) {
			LogBuffer.println("ColorPicker was null. Could not move BoundaryThumb.");
			return;
		}
		
		List<Thumb> thumbs = colorPicker.getThumbList();
		
		int outermostInnerIdx;
		int insertionIdx = 0;
		if(bT.isMin()) {
			/* Always set outer-most inner thumb to new boundary, if the old 
			 * boundary thumb "passes over".
			 */
			outermostInnerIdx = 1;
			
			/* Determine the thumb list index the new position corresponds to */
			for(Thumb t : thumbs) {
				if(t.getDataValue() > dataVal && !t.equals(bT)) {
					insertionIdx = thumbs.indexOf(t) - 1;
					break;
				}
			}
			
			/* Do not continue if no thumb to swap (-1 or 0) */
			if(insertionIdx < outermostInnerIdx) {
				LogBuffer.println("No need to swap min boundary thumb.");
				alignThumbWithVal(dataVal);
				return;
			}
		} else {
			outermostInnerIdx = thumbs.size() - 1;
			
			/* Determine the thumb list index the new position corresponds to */
			for(Thumb t : thumbs) {
				if(t.getDataValue() > dataVal && !t.equals(bT)) {
					insertionIdx = thumbs.indexOf(t);
					break;
				}
			}
			
			/* Do not continue if no thumb to swap (-1 or 0) */
			if(insertionIdx > outermostInnerIdx) {
				LogBuffer.println("No need to swap max boundary thumb.");
				alignThumbWithVal(dataVal);
				return;
			}
		}
		
		LogBuffer.println("Fractions before: " + Arrays.toString(colorPicker.getFractions()));
		swapBoundaryThumbAt(outermostInnerIdx, bT, dataVal);
		LogBuffer.println("Fractions after: " + Arrays.toString(colorPicker.getFractions()));
		LogBuffer.println("--------");
	}
	
	/**
	 * 
	 * @param outermostInnerIdx
	 * @param bT
	 * @param dataVal
	 */
	private void swapBoundaryThumbAt(final int outermostInnerIdx, 
	                                 final BoundaryThumb bT, 
	                                 final double dataVal) {
		
		List<Thumb> thumbs = colorPicker.getThumbList();
		
		LogBuffer.println("Swapping boundary thumb...");
		Thumb oldInnerThumb = thumbs.get(outermostInnerIdx);
		
		/* Set up new boundary */
		BoundaryThumb newBoundary = new BoundaryThumb(bT.isMin());
		newBoundary.setValue(oldInnerThumb.getDataValue());
		newBoundary.setColor(oldInnerThumb.getColor());
		
		Thumb newInnerThumb = new Thumb(bT.getColor());
		newInnerThumb.setValue(dataVal);
	
		colorPicker.setMinVal(oldInnerThumb.getDataValue());
		
		replaceBoundaryThumb(newBoundary);
		moveBoundaryToInnerThumb(outermostInnerIdx, newInnerThumb);
	}

	/**
	 * Requests if selected thumb is currently set or not. If yes, that means
	 * there generally is a thumb in selected status.
	 * 
	 * @return boolean
	 */
	protected boolean hasSelectedThumb() {

		return (selectedThumb != null);
	}

	/**
	 * Gives information whether the currently selected thumb is a boundary
	 * thumb or not.
	 * 
	 * @return
	 */
	protected boolean isSelectedBoundaryThumb() {

		/* could be done with instanceof but rather not... */
		int thumbNum = colorPicker.getThumbNumber();
		int selectedIndex = getSelectedThumbIndex();

		return selectedIndex == 0 || selectedIndex == thumbNum - 1;
	}
}
