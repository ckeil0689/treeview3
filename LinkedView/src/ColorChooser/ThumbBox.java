package ColorChooser;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
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
		newThumb.setDataValue(dataValue);

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
	 * @param inputX
	 *            New x-coordinate of the selected thumb.
	 */
	protected void moveThumbTo(int inputX) {

		if (selectedThumb == null) {
			return;
		}

		double dataVal;

		/* Max out on edges */
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

		updateThumbDataVal(dataVal);
	}

	/**
	 * TODO break up method into smaller parts
	 * 
	 * @param inputData
	 */
	protected void updateThumbDataVal(final double inputData) {

		/* Stay between boundaries */
		if (selectedThumb == null || inputData < colorPicker.getMinVal()
				|| inputData > colorPicker.getMaxVal()) {
			return;
		}

		/* Cannot move to boundary fractions */
		if (Helper.nearlyEqual(colorPicker.getMinVal(), inputData)
				|| Helper.nearlyEqual(colorPicker.getMaxVal(), inputData)) {
			return;
		}

		double[] thumbDataVals = new double[colorPicker.getThumbNumber()];

		for (int i = 0; i < thumbDataVals.length; i++) {
			thumbDataVals[i] = getThumbVal(i);
		}

		/* get position of previous thumb */
		List<Thumb> thumbs = colorPicker.getThumbList();
		int selectedIndex = thumbs.indexOf(selectedThumb);

		/* define the boundaries around active thumb's fraction */
		/* defined out of range in case no previous/ next thumb exists */
		double previousData = colorPicker.getMinVal();
		double nextData = colorPicker.getMaxVal();

		/* deal with boundary cases */
		if (selectedIndex == 0) {
			boolean isSafeShrink = inputData < thumbDataVals[selectedIndex + 1];
			int removed = 0;
			if (!isSafeShrink) {
				removed = shrinkThumbsToDataRange(inputData, true);
			}
			int nextIndex = selectedIndex + 1 + removed;
			previousData = -1.0 * Double.MAX_VALUE;
			nextData = thumbDataVals[nextIndex];
			colorPicker.setMinVal(inputData); // calls updateFractions()...maybe
												// theres a better way

		} else if (selectedIndex == colorPicker.getThumbNumber() - 1) {
			boolean isSafeShrink = (inputData > thumbDataVals[selectedIndex - 1]);
			int removed = 0;
			if (!isSafeShrink) {
				removed = shrinkThumbsToDataRange(inputData, false);
				selectedIndex -= removed;
			}
			int prevIndex = selectedIndex - 1;
			previousData = thumbDataVals[prevIndex];
			nextData = Double.MAX_VALUE;
			colorPicker.setMaxVal(inputData);

			/* Non-boundary */
		} else {
			previousData = thumbDataVals[selectedIndex - 1];
			nextData = thumbDataVals[selectedIndex + 1];
		}

		/* set new thumb position and check for boundaries/ other thumbs */
		if ((previousData < inputData && inputData < nextData)) {
			thumbs.get(selectedIndex).setDataValue(inputData);

		} else if (inputData < previousData
				&& !Helper.nearlyEqual(previousData, colorPicker.getMinVal())) {
			colorPicker.swapPositions(selectedIndex, selectedIndex - 1);

			thumbs.get(selectedIndex).setDataValue(previousData);
			thumbs.get(selectedIndex - 1).setDataValue(inputData);

		} else if (inputData > nextData && nextData < colorPicker.getMaxVal()) {
			colorPicker.swapPositions(selectedIndex, selectedIndex + 1);

			thumbs.get(selectedIndex).setDataValue(nextData);
			thumbs.get(selectedIndex + 1).setDataValue(inputData);
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

		for (int i = 0; i < fractions.length; i++) {

			final int pos = getPosFromFraction(fractions[i]);

			Thumb t = colorPicker.getThumb(i);
			t.setCoords(pos, t.getY());
		}
	}

	/**
	 * Turns a fraction into a data value.
	 * 
	 * @param frac
	 * @return
	 */
	protected int getPosFromFraction(float frac) {

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

		double dataVal = posInputDialog.showDialog(colorPicker
				.getContainerPanel());

		t.setDataValue(dataVal);

		alignThumbWithDataVal(dataVal);
	}

	/**
	 * Finds the correct x-position for a thumb for a given data value.
	 * 
	 * @param dataVal
	 *            A data value for which the corresponding thumb's x-position is
	 *            to be determined.
	 */
	protected void alignThumbWithDataVal(double dataVal) {

		double minVal = colorPicker.getMinVal();
		double maxVal = colorPicker.getMaxVal();

		/* TODO adapt range if values are outside */
		if (dataVal < minVal) {
			colorPicker.setMinVal(dataVal);

		} else if (dataVal > maxVal) {
			colorPicker.setMaxVal(dataVal);

		} else {
			updateThumbDataVal(dataVal);
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
	 * Gets the data value a thumb represents.
	 *
	 * @param t
	 * @return A double value between minimum and maximum of the currently
	 *         relevant data range for coloring.
	 */
	protected double getThumbVal(final int thumbIndex) {

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

			double tDataVal = getThumbVal(thumbs.indexOf(t));
			if (Helper.nearlyEqual(tDataVal, dataVal)) {
				hasThumb = true;
				break;
			}
		}

		return hasThumb;
	}
	
	/**
	 * Check if there is any thumb associated with the given data value. If yes,
	 * return its index.
	 * 
	 * @param dataVal
	 * @return Thumb index in data
	 */
	protected void removeThumbWithVal(double dataVal) {

		List<Thumb> thumbs = colorPicker.getThumbList();

		for (final Thumb t : thumbs) {
			double tDataVal = getThumbVal(thumbs.indexOf(t));
			if (Helper.nearlyEqual(tDataVal, dataVal)) {
				int idx = thumbs.indexOf(t);
				thumbs.remove(idx);
				colorPicker.getColorList().remove(idx);
			}
		}
		
		colorPicker.updateFractions();
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
