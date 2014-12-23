package ColorChooser;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

/**
 * A class which describes a small triangular object used to define colors
 * and color positions along the gradient box. This is what the user
 * interacts with to define the color scheme for DendroView.
 * 
 * @author CKeil
 * 
 */
public class Thumb {

	private int x;
	private int y;

	private final int width = 10;
	private final int height = 15;

	private GeneralPath innerthumbPath;
	private GeneralPath outerthumbPath;
	private Color thumbColor;
	private boolean selected = false;

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

		innerthumbPath = new GeneralPath();
		innerthumbPath.moveTo(x, y + height / 2);
		innerthumbPath.lineTo(x + width / 4, y - height);
		innerthumbPath.lineTo(x - width / 4, y - height);
		innerthumbPath.closePath();

		outerthumbPath = new GeneralPath();
		outerthumbPath.moveTo(x, y);
		outerthumbPath.lineTo(x + width / 2, y - height);
		outerthumbPath.lineTo(x - width / 2, y - height);
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

		if (isSelected()) {
			g2d.setColor(Color.red);

		} else {
			g2d.setColor(Color.black);
		}

		g2d.fill(outerthumbPath);

		g2d.setColor(thumbColor);
		g2d.fill(innerthumbPath);
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
	 * gradientBox. Should equal the height of thumbBox because it sits on
	 * top of gradientBox and they directly touch.
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
	 * Checks if this Thumb's GeneralPath object contains the specified x-
	 * and y-variable.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean contains(final int x, final int y) {

		return outerthumbPath.contains(x, y);
	}
}
