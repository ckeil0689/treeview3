package edu.stanford.genetics.treeview.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * Static class (java way...) to be used for loading read-only preview data for
 * the user. The information can then be used to specify parameters and option
 * for the real loading process.
 * 
 * @author chris0689
 *
 */
public final class PreviewLoader {

	/*
	 * For recognizing common labels in a file. Matches (non case-sensitive):
	 * YORF, ORF, EWEIGHT, GWEIGHT, WEIGHT, COMPLEX, NAME, GID, UID, AID, ID,
	 * ROWID, COLID
	 */
	private final static String COMMON_LABELS = "(?i)(COMPLEX|NAME|^Y?ORF$|"
			+ "^(GENE|G|ARRAY|A|U|ROW|COL)?ID$|^.*WEIGHT$)";

	/*
	 * For recognizing supposed numeric data in a file. Matches (non
	 * case-sensitive): NA, N/A, NAN, NONE, EMPTY, MISS, MISSING
	 */
	private final static String MISSING = "(?i)(^N(/)?A.?$|EMPTY|NONE|^MISS.*$)";

	public final static int LIMIT = 20;  //The number of rows & cols to include

	private PreviewLoader() {

		/* prevent instantiation */
	}

	public static int[] findDataStartCoords(final String filename,
			final String delimiter) {

		int[] dataStartCoords = new int[2];

		try {
			final BufferedReader br = new BufferedReader(new FileReader(
					filename));

			String line;
			int count = 0;
			int lastCommonLabelColumn = -1;

			/* read all lines */
			while ((line = br.readLine()) != null && count < LIMIT) {

				final String[] lineAsStrings = line.split(delimiter, -1);

				/* read all strings in a line */
				for (int i = 0; i < lineAsStrings.length; i++) {

					String element = lineAsStrings[i];

					if (element.endsWith("e") || element.endsWith("E")) {
						element += "+00";
					}

					/*
					 * Data found if: 1) Is numeric 2) OR found a N/A (or
					 * equivalent) symbol) 3) current string index is bigger
					 * than last known column header index 4) Current line does
					 * NOT have a common known header (e.g. EWEIGHT) in the
					 * line, which may be a label but contain numeric data.
					 */
					if (isDoubleParseable(element) || isNaN(element)) {
						if (lastCommonLabelColumn < i
								&& hasCommonLabel(lineAsStrings)) {
							break;
						}

						if (i <= lastCommonLabelColumn) {
							continue;
						}

						dataStartCoords[0] = count;
						dataStartCoords[1] = i;
						count = LIMIT;
						break;
					}

					if (isCommonLabel(element) && lastCommonLabelColumn < i) {
						lastCommonLabelColumn = i;
					}
				}
				count++;
			}

			br.close();

		} catch (final IOException e) {
			LogBuffer.logException(e);
			LogBuffer.println("Could not find data start coordinates.");
			return new int[] { 0, 0 };
		}

		return dataStartCoords;
	}

	/**
	 * Check the current line for any sort of common label.
	 * 
	 * @param line
	 * @return Whether line includes a common label or not.
	 */
	private static boolean hasCommonLabel(final String[] line) {

		for (String token : line) {
			if (Pattern.matches(PreviewLoader.COMMON_LABELS, token)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check a string if it is part of the recognized common labels.
	 * 
	 * @param token
	 * @return Whether token is a common label or not.
	 */
	private static boolean isCommonLabel(final String token) {

		if (Pattern.matches(PreviewLoader.COMMON_LABELS, token)) {
			return true;
		}
		return false;
	}

	/**
	 * Check if a string is a supposed numeric string that has been substituted
	 * due to missing/ non-available data etc.
	 * 
	 * @param token
	 * @return Whether token is a supposed-numeric string.
	 */
	private static boolean isNaN(final String token) {

		if (Pattern.matches(PreviewLoader.MISSING, token)) {
			return true;
		}
		return false;
	}

	/**
	 * Check if the current string is numeric and can be parsed into a double.
	 * 
	 * @param token
	 * @return Whether token is parseable to double.
	 */
	private static boolean isDoubleParseable(final String token) {

		try {
			Double.parseDouble(token);
			return true;

		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Small wrapper for <extractPreviewData>
	 * 
	 * @param filename
	 *            The path/ name of the file to be loaded.
	 * @return String[][] The preview data in array format for use in JTable.
	 */
	public static String[][] loadPreviewData(final String filename,
			final String delimiter) {

		String[][] previewData;

		try {
			final BufferedReader br = new BufferedReader(new FileReader(
					filename));

			previewData = extractPreviewData(br, delimiter);

			br.close();

		} catch (final IOException e) {
			LogBuffer.logException(e);
			return new String[0][];
		}

		return previewData;
	}

	/**
	 * TODO put this into a PreviewLoader subclass which extends SwingWorker Get
	 * a String array representation of the first <LIMIT> elements of data. This
	 * is for use in the data preview dialog only. The data
	 * 
	 * @param A
	 *            BufferedReader object to read through the file.
	 * @return String[][] The preview data in array format for use in JTable.
	 * @author chris0689
	 */
	private static String[][] extractPreviewData(final BufferedReader reader,
			final String delimiter) {

		final String[][] previewData = new String[LIMIT][];
		String line;
		int count = 0;

		try {
			while ((line = reader.readLine()) != null && count < LIMIT) {

				// load line as String array
				final String[] lineAsStrings = line.split(delimiter, -1);
				previewData[count++] = Arrays.copyOfRange(lineAsStrings, 0,
						LIMIT);
			}
		} catch (final IOException e) {
			LogBuffer.logException(e);
			return new String[][] { { "N/A" } };
		}

		return previewData;
	}
}
