package ColorChooser;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

/**
 * A class which describes a small triangular object used to define colors and
 * color positions along the gradient box. This is what the user interacts with
 * to define the color scheme for DendroView.
 *
 * @author CKeil
 *
 */
public class Thumb {

	protected int x;
	protected int y;
	
	protected double dataVal;

	/* Thumb dimensions */
	protected int width = 12;
	protected int height = 8;

	protected GeneralPath innerthumbPath;
	protected GeneralPath outerthumbPath;
	
	protected Color thumbColor;
	protected boolean selected = false;

	/**
	 * Constructs a thumb object if given the x/y-coordinates and a color.
	 *
	 * @param x
	 * @param y
	 * @param color
	 */
	public Thumb(final int x, final int y) {

		this(x, y,Color.GRAY);
	}
	
	/**
	 * Constructs a thumb object if given the x/y-coordinates and a color.
	 *
	 * @param x
	 * @param y
	 * @param color
	 */
	public Thumb(final int x, final int y, final Color color) {

		this.thumbColor = color;
		setCoords(x, y);
	}
	
	/**
	 * Sets the data value which the thumb represents on the gradient. 
	 * @param dataVal
	 */
	public void setDataValue(final double dataVal) {
		
		this.dataVal = dataVal;
	}
	
	/**
	 * Accessor for the thumb's associated data value.
	 * @return
	 */
	public double getDataValue() {
		
		return dataVal;
	}

	/**
	 * Sets the base x/y-coordinates for the thumb object. This is where it
	 * touches the gradientBox.
	 *
	 * @param x
	 * @param y
	 */
	public void setCoords(final int x, final int y) {

		this.x = x;
		this.y = y;

		createThumbPath();
	}

	/**
	 * Uses the GeneralPath class and x/y-coordinates to generate a small
	 * triangular object which will represent an interactive 'thumb'.
	 */
	public void createThumbPath() {

		/* magic numbers in here are pixel offsets */
		innerthumbPath = new GeneralPath();
		innerthumbPath.moveTo(x, y);
		innerthumbPath.lineTo(x + width / 2, y - (height + 3));
		innerthumbPath.lineTo(x - width / 2, y - (height + 3));
		innerthumbPath.closePath();

		outerthumbPath = new GeneralPath();
		outerthumbPath.moveTo(x, y);
		outerthumbPath.lineTo(x + (width + 4) / 2, y - (height + 4));
		outerthumbPath.lineTo(x - (width + 4) / 2, y - (height + 4));
		outerthumbPath.closePath();
	}

	public void setSelected(final boolean selected) {

		this.selected = selected;
	}

	/**
	 * Paints the GeneralPath object with the set color and makes the thumb
	 * visible to the user.
	 *
	 * @param g2d
	 */
	public void paint(final Graphics2D g2d) {

		g2d.setColor(Color.GRAY);
		g2d.fill(outerthumbPath);

		g2d.setColor(thumbColor);

		/* draw circle above thumb if selected */
		if (isSelected()) {
			final int yPos = y - height - 5;
			drawCenteredCircle(g2d, x, yPos, 4);
		}

		g2d.fill(innerthumbPath);
	}

	/**
	 * Draw a small circle to be positioned above the selected thumb.
	 *
	 * @param g
	 *            Graphics object
	 * @param x
	 *            x-Position
	 * @param y
	 *            y-Position
	 * @param r
	 *            Circle radius
	 */
	public void drawCenteredCircle(final Graphics2D g, int x, int y, final int r) {

		final int r1 = r + 2;
		final int x1 = x - (r1 / 2);
		final int y1 = y - (r1 / 2);

		g.setColor(Color.GRAY);
		g.fillOval(x1, y1, r1, r1);

		x = x - (r / 2);
		y = y - (r / 2);

		g.setColor(thumbColor);
		g.fillOval(x, y, r, r);
	}

	/**
	 * Returns the base x-coordinate for the thumb where it contacts the
	 * gradientBox.
	 *
	 * @return int
	 */
	public int getX() {

		return x;
	}

	/**
	 * Returns the base y-coordinate for the thumb where it contacts the
	 * gradientBox. Should equal the height of thumbBox because it sits on top
	 * of gradientBox and they directly touch.
	 *
	 * @return
	 */
	public int getY() {

		return y;
	}

	/**
	 * Shows if the current thumb's selected status is true or not.
	 *
	 * @return boolean
	 */
	public boolean isSelected() {

		return selected;
	}

	/**
	 * Provides the currently set color for its thumb object.
	 *
	 * @return
	 */
	public Color getColor() {

		return thumbColor;
	}

	/**
	 * Provides the currently set color for its thumb object.
	 *
	 * @return
	 */
	public void setColor(final Color newCol) {

		thumbColor = newCol;
	}

	/**
	 * Checks if this Thumb's GeneralPath object contains the specified x- and
	 * y-variable.
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean contains(final int x, final int y) {

		return outerthumbPath.contains(x, y);
	}
}
