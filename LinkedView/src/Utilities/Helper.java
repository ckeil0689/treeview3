package Utilities;

public final class Helper {

	private static final double EPSILON = 0.0000000001;
	
	/**
	 * Compares to floating point numbers to find out whether they can
	 * be considered equal.
	 * @param a
	 * @param b
	 * @param epsilon
	 * @return
	 */
	public static boolean nearlyEqual(double a, double b) {
		
		final double absA = Math.abs(a);
		final double absB = Math.abs(b);
		final double diff = Math.abs(a - b);

		if (a == b) { // shortcut, handles infinities
			return true;
		} else if (a == 0 || b == 0 || diff < Float.MIN_NORMAL) {
			// a or b is zero or both are extremely close to it
			// relative error is less meaningful here
			return diff < (EPSILON * Float.MIN_NORMAL);
		} else { // use relative error
			return diff / (absA + absB) < EPSILON;
		}
	}
	
	/**
	 * Method to make deep copy of distance matrix
	 * 
	 * @return A two-dimensional double array that is a copy of the distance
	 * matrix used in both hierarchical and k-means clustering.
	 */
	public static double[][] cloneMatrix(final double[][] distMatrix) {

		final double[][] deepCopy = new double[distMatrix.length][];

		for (int i = 0; i < distMatrix.length; i++) {

			final double[] oldList = distMatrix[i];
			final double[] newList = new double[oldList.length];

			System.arraycopy(oldList, 0, newList, 0, oldList.length);

			deepCopy[i] = newList;
		}

		return deepCopy;
	}
}
