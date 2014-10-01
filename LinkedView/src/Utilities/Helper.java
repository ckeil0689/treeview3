package Utilities;

public final class Helper {

	/**
	 * Compares to floating point numbers to find out whether they can
	 * be considered equal.
	 * @param a
	 * @param b
	 * @param epsilon
	 * @return
	 */
	public static boolean nearlyEqual(double a, double b, double epsilon) {
		
		final double absA = Math.abs(a);
		final double absB = Math.abs(b);
		final double diff = Math.abs(a - b);

		if (a == b) { // shortcut, handles infinities
			return true;
		} else if (a == 0 || b == 0 || diff < Float.MIN_NORMAL) {
			// a or b is zero or both are extremely close to it
			// relative error is less meaningful here
			return diff < (epsilon * Float.MIN_NORMAL);
		} else { // use relative error
			return diff / (absA + absB) < epsilon;
		}
	}
}
