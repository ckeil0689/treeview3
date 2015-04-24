package ColorChooser;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import edu.stanford.genetics.treeview.LogBuffer;
import Utilities.CustomDialog;
import Utilities.GUIFactory;
import Utilities.Helper;

public class ThumbBox {

	private final ColorPicker colorPicker;
	
	private final Rectangle2D thumbRect = new Rectangle2D.Float();
	
	/* Holds the currently selected thumb */
	private Thumb selectedThumb = null;
	
	public ThumbBox(ColorPicker colorPicker) {
		
		this.colorPicker = colorPicker;
	}
	
	protected void drawThumbBox(Graphics g) {
		
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
	 * Checks if a thumb is located at a certain point.
	 *
	 * @param point The point to be checked.
	 * @return boolean - Whether a thumb is located at the point or not.
	 */
	protected boolean containsThumb(final Point point) {

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
	 * Selects a thumb if it is not yet selected, deselects it otherwise.
	 *
	 * @param point
	 */
	protected void selectThumb(final Point point) {

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
	private void verifyThumbs() {

		final int x = (int) thumbRect.getMinX();
		final int w = (int) thumbRect.getWidth();
		
		float[] fractions = colorPicker.getFractions();

		for (int i = 0; i < fractions.length; i++) {

			// Avoid rounding errors when casting to int
			final double widthFactor = Math.round(w * fractions[i]);
			final int pos = x + (int) (widthFactor);

			if (!checkThumbPresence(i)
					&& !((colorPicker.getThumbNumber() == colorPicker.getColorNumber()) 
							&& colorPicker.getThumbNumber() == fractions.length)) {
				insertThumbAt(pos, colorPicker.getColorList().get(i));
			}
		}
	}
	
	/**
	 * Checks if a thumb is present at the given x-position.
	 *
	 * @param pos
	 * @return
	 */
	private boolean checkThumbPresence(final int thumbIndex) {

		List<Thumb> thumbs = colorPicker.getThumbList();
		float[] fractions = colorPicker.getFractions();

		if (thumbs.size() > thumbIndex) {
			double fraction = thumbs.get(thumbIndex).getX() 
					/ thumbRect.getWidth();
			fraction = (double) Math.round(fraction * 10000) / 10000;

			final double fraction2 = (double) Math
					.round(fractions[thumbIndex] * 10000) / 10000;

			if (Helper.nearlyEqual(fraction, fraction2)
					|| thumbs.get(thumbIndex).isSelected()) {
				return true;
			}
		}

		return false;
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
	 * TODO fractions cannot be dependent on thumb x-coords because a full
	 * data range does not fit into 430 pixels which is the width of the color
	 * gradient box. (-> obv. can only represent 430 data points). Separate
	 * fractions from thumb x position and keep thumbList + fractions synced!!
	 * @param inputX
	 */
	protected void updateThumbPos(final int inputX) {

		/* stay within boundaries */
		if (selectedThumb == null
				|| (inputX < (int) thumbRect.getMinX() 
						|| inputX > (int) thumbRect.getMaxX()))
			return;

		/* get position of previous thumb */
		final int selectedIndex = colorPicker.getThumbList()
				.indexOf(selectedThumb);
		int previousPos = 0;
		int nextPos = (int) thumbRect.getMaxX();

		/* set positions around active thumb */
		if (selectedIndex == 0) {
			nextPos = colorPicker.getThumb(selectedIndex + 1).getX();

		} else if (selectedIndex == colorPicker.getThumbNumber() - 1) {
			previousPos = colorPicker.getThumb(selectedIndex - 1).getX();

		} else {
			previousPos = colorPicker.getThumb(selectedIndex - 1).getX();
			nextPos = colorPicker.getThumb(selectedIndex + 1).getX();
		}

		/* get updated x-values */
		final int deltaX = inputX - selectedThumb.getX();
		final int newX = selectedThumb.getX() + deltaX;

		/* set new thumb position and check for boundaries/ other thumbs */
		if (previousPos < newX && newX < nextPos) {
			selectedThumb.setCoords(newX, selectedThumb.getY());

		} else if (newX < previousPos && previousPos != thumbRect.getMinX()) {
			colorPicker.swapPositions(selectedIndex, selectedIndex - 1);
			selectedThumb.setCoords(newX, selectedThumb.getY());

		} else if (newX > nextPos && nextPos < thumbRect.getMaxX()) {
			colorPicker.swapPositions(selectedIndex, selectedIndex + 1);
			selectedThumb.setCoords(newX, selectedThumb.getY());
		}

		colorPicker.refreshLists();
	}

	/**
	 * Finds if an existing thumb contains the passed coordinate point.
	 * This is used to open an edit dialog for a thumb if it was double-clicked. 
	 * @param point Coordinate point of the clicked area.
	 */
	protected void setThumbPosition(final Point point) {

		int index = 0;
		for (final Thumb t : colorPicker.getThumbList()) {

			if (t.contains((int) point.getX(), (int) point.getY())) {

				openThumbEditDialog(t, index);
			}
			index++;
		}

		colorPicker.refreshLists();
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
		final double fraction = diff / (range);

		int thumbXValue = (int) Math.round((fraction * ColorPicker.WIDTH));
		thumbXValue += (int) thumbRect.getMinX(); // offset in the panel

		updateThumbPos(thumbXValue);
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
	 * Returns the fraction of the width of the gradientRect where a thumb is
	 * currently positioned.
	 *
	 * @param t
	 * @return a float value between 0.0 and 1.0
	 */
	protected float getThumbFraction(final Thumb t) {

		final double x = t.getX() - thumbRect.getMinX();
		return (float) (x / thumbRect.getWidth());
	}

	/**
	 * Gets a thumb's position in terms of the data range.
	 *
	 * @param t
	 * @return A double value between minimum and maximum of the currently
	 *         relevant data range for coloring.
	 */
	protected double getThumbPosition(final Thumb t) {

		double range = colorPicker.getRange();
		double minVal = colorPicker.getMinVal();
		
		final float fraction = getThumbFraction(t);
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
	 * @return The index of the selected thumb in thumbList.
	 */
	protected int getSelectedThumbIndex() {
		
		List<Thumb> thumbs = colorPicker.getThumbList();
		
		int selectedIndex = 0;
		if (selectedThumb != null) {
			selectedIndex = thumbs.indexOf(selectedThumb);
		}

		if (thumbs.get(selectedIndex).getX() == thumbs.get(thumbs.size() - 1)
				.getX()) {
			selectedIndex--;
		}
		
		return selectedIndex;
	}
	
	/**
	 * Set a new value for the selected thumb.
	 * @param t The new thumb to be set as the selected one.
	 */
	protected void setSelectedThumb(Thumb t) {
		
		this.selectedThumb = t;
	}
}


