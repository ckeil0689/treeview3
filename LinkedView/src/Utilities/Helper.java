package Utilities;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * Class that is supposed to contain a bunch of static general helper methods,
 * which are usually operations that aren't necessarily unique to one class.
 * 
 * @author CKeil
 *
 */
public final class Helper {

	private static final double EPSILON = 0.0000000001;

	/**
	 * Compares to floating point numbers to find out whether they can be
	 * considered equal. '==' can never be used to compare floating point
	 * numbers!
	 * 
	 * @param double a First floating point number.
	 * @param double b Second floating point number.
	 * @return boolean Whether 2 floats can be considered equal.
	 */
	public static boolean nearlyEqual(final double a, final double b) {

		final double absA = Math.abs(a);
		final double absB = Math.abs(b);
		final double diff = Math.abs(a - b);

		if (a == b)
			return true;
		else if (a == 0 || b == 0 || diff < Float.MIN_NORMAL)
			// a or b is zero or both are extremely close to it
			// relative error is less meaningful here
			return diff < (EPSILON * Float.MIN_NORMAL);
		else
			return diff / (absA + absB) < EPSILON;
	}

	/**
	 * Fuses two int arrays into a new third array and returns it.
	 *
	 * @param int[] a First array.
	 * @param int[] b Second array.
	 * @return The merged array.
	 */
	public static int[] concatIntArrays(final int[] a, final int[] b) {

		final int[] c = new int[a.length + b.length];

		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);

		return c;
	}

	/**
	 * Count amount of lines in the file to be loaded so that the progressBar
	 * can get correct values for extractData(). Code from StackOverflow
	 * (https://stackoverflow.com/questions/1277880).
	 */
	public static int countFileLines(final File aFile) throws IOException {

		LineNumberReader reader = null;

		try {
			reader = new LineNumberReader(new FileReader(aFile));
			while ((reader.readLine()) != null) {
				;
			}
			return reader.getLineNumber();

		} catch (final Exception ex) {
			LogBuffer.println("Exception when trying to count lines: "
					+ ex.getMessage());
			return -1;

		} finally {
			if (reader != null) {
				reader.close();
				reader = null;
			}
		}
	}
}
