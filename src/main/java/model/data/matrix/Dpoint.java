/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package model.data.matrix;

/**
 * Class to represent pair of doubles
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.4 $ $Date: 2004-12-21 03:28:14 $
 */
public class Dpoint {
	// point who's location is stored in doubles...
	/**
	 * actual pair of doubles
	 */
	protected double x, y;

	/**
	 * Constructor for the Dpoint object
	 *
	 * @param dx
	 *            first val in pair
	 * @param dy
	 *            second val in pair
	 */
	public Dpoint(final double dx, final double dy) {
		x = dx;
		y = dy;
	}

	/**
	 * Gets the x attribute of the Dpoint object
	 *
	 * @return The x value
	 */
	public double getX() {
		return x;
	}

	/**
	 * Gets the y attribute of the Dpoint object
	 *
	 * @return The y value
	 */
	public double getY() {
		return y;
	}

	/**
	 * Sometimes we want to scale the x and take the int part
	 *
	 * @param s
	 *            multiplicative scaling factor
	 * @return int part of product
	 */
	public int scaledX(final double s) {
		return (int) (x * s);
	}

	/**
	 * Sometimes we want to scale the y and take the int part
	 *
	 * @param s
	 *            multiplicative scaling factor
	 * @return int part of product
	 */
	public int scaledY(final double s) {
		return (int) (y * s);
	}

	/**
	 * Gets the dist to another point
	 *
	 * @param dp
	 *            the other point
	 * @return The distance to dp
	 */
	public double getDist(final Dpoint dp) {
		final double dx = x - dp.getX();
		final double dy = y - dp.getY();
		return dx * dx + dy * dy;
	}
}
