/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

/**
 * this class encapuslates a linear equation. I probably could have named it
 * line equation or something...
 *
 */
public class LinearTransformation {
	// y = mx + b
	private double m = 0.0;
	private double b = 0.0;
	// for the inverse transformation
	private double mi = 0.0;
	private double bi = 0.0;

	public double getSlope() {
		return m;
	}

	public LinearTransformation(final double fromX, final double fromY,
			final double toX, final double toY) {
		setMapping(fromX, fromY, toX, toY);
		/*
		 * System.out.println("New line y = " + m + " x + " + b);
		 * System.out.println("from (" + fromX + ", " + fromY + "), (" + toX+
		 * ", " + toY + ")");
		 */
		System.out.println("from (x" + fromX + ", y" + fromY + "), to (x" + toX+
			", y" + toY + ")");
	}

	public LinearTransformation() { /* default, map everything to 0.0 */
	}

	public void setMapping(final double fromX, final double fromY,
			final double toX, final double toY) {
		m = (toY - fromY) / (toX - fromX);
		b = (fromY * toX - toY * fromX) / (toX - fromX);
		mi = (toX - fromX) / (toY - fromY);
		bi = (fromX * toY - toX * fromY) / (toY - fromY);
	}

	public double transform(final double y) {
		return (m * y + b);
	}

	public double inverseTransform(final double y) {
		return (mi * y + bi);
	}
}
