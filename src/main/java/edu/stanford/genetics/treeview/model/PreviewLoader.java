package edu.stanford.genetics.treeview.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

import edu.stanford.genetics.treeview.LogBuffer;

/** Static class (java way...) to be used for loading read-only preview data for
 * the user. The information can then be used to specify parameters and option
 * for the real loading process.
 * 
 * @author chris0689 */
public final class PreviewLoader {

	/*
	 * For recognizing common labels in a file. Matches (non case-sensitive):
	 * YORF, ORF, EWEIGHT, GWEIGHT, WEIGHT, COMPLEX, NAME, GID, UID, AID, ID,
	 * ROWID, COLID
	 */
	public final static String COMMON_LABELS = "(?i)(COMPLEX|NAME|^Y?ORF$|" +
																							"^(GENE|G|ARRAY|A|U|ROW|COL)?ID$|^.*WEIGHT$)";

	public final static String COMMON_ROW_LABELS = "(?i)(COMPLEX|NAME|^Y?ORF$|" +
																									"^(GENE|G|U|ROW)?ID$|^.*WEIGHT$)";

	/*
	 * For recognizing supposed numeric data in a file. Matches (non
	 * case-sensitive): NA, N/A, NAN, NONE, EMPTY, MISS, MISSING
	 */
	private final static String MISSING = "(?i)(^N(/)?A.?$|EMPTY|NONE|^MISS.*$)";

	public final static int LIMIT = 20;  //The number of rows & cols to include

	private PreviewLoader() {

		/* prevent instantiation */
	}

	/** Reads the file defined by the <code>filename</code> line by line and
	 * searches for the starting coordinates of the numeric data. Elements
	 * are distinguished by the <code>delimiter</code>. If label types were
	 * already defined, for example in a previous load, they can assist in
	 * correctly identifying the data starting coordinates.
	 * 
	 * @param filename - The file to read line by line
	 * @param delimiter - Separator of elements in the file
	 * @param rowLabelTypes - If defined, row label types that were defined in
	 *          a previous load.
	 * @param colLabelTypes - If defined, column label types that were defined in
	 *          a previous load.
	 * @return an integer array with two elements which describe the coordinates
	 *         of the data starting point in file */
	public static int[] findDataStartCoords(final String filename,
																					final String delimiter,
																					final String[] rowLabelTypes,
																					final String[] colLabelTypes) {

		int[] dataStartCoords = new int[2];

		try {
			final BufferedReader br = new BufferedReader(new FileReader(filename));

			String line;
			int rowIdx = 0;
			int maxRowLabelTypeIdx = -1;

			// read all lines, at most up to LIMIT
			while((line = br.readLine()) != null && rowIdx < LIMIT) {

				final String[] lineAsStrings = line.split(delimiter, -1);

				// iterate over strings in a line
				for(int colIdx = 0; colIdx < lineAsStrings.length; colIdx++) {

					String element = lineAsStrings[colIdx];

					// correct trailing 'e' if it belongs to numeric element
					// REVIEW NOTE: this is replaced in another open PR but temporarily 
					// updated to avoid exceptions
					if(element.endsWith("e") || element.endsWith("E")) {
						int bound = (element.length() > 2)	? (element.length() - 2)
																								: element.length();
						String trimmedElem = element.substring(0, bound);
						// only do this if original is numeric with trailing 'e' 
						if(isDoubleParseable(trimmedElem)) {
							element += "+00";
						}
					}

					/*
					 * Data found if: 
					 * 1) Is numeric OR found a N/A (or equivalent) symbol) AND
					 * 2) current string index is bigger than last known 
					 * column label type index OR current line does not have a 
					 * common known label type (e.g. EWEIGHT) in the line, which may be 
					 * a label but contain numeric data.
					 */
					if(isDoubleParseable(element) || isNaN(element)) {
						// Numerics
						// skip to next line
						if(maxRowLabelTypeIdx < colIdx &&
								hasCommonLabel(lineAsStrings, PreviewLoader.COMMON_LABELS)) {
							break;
						}

						// skip to next element in line (data cannot be in this column)
						if(colIdx <= maxRowLabelTypeIdx) {
							continue;
						}

						dataStartCoords[0] = rowIdx;
						dataStartCoords[1] = colIdx;
						rowIdx = LIMIT;
						break;
					}
					else {
						// Non-numeric
						// if a known row label type is encountered, skip to next element
						if(Arrays.asList(rowLabelTypes).contains(element)) {
							LogBuffer.println("Found " + element + " from row label types.");
							if(maxRowLabelTypeIdx < colIdx) {
								maxRowLabelTypeIdx = colIdx;
							}
							continue;
						}

						// if a known column label type is encountered, skip to next line
						if(Arrays.asList(colLabelTypes).contains(element)) {
							LogBuffer.println("Found " + element + " from col label types.");
							break;
						}
					}

					if(isCommonLabel(element, PreviewLoader.COMMON_LABELS) &&
							maxRowLabelTypeIdx < colIdx) {
						maxRowLabelTypeIdx = colIdx;
					}
				}
				rowIdx++;
			}

			br.close();

		}
		catch(final IOException e) {
			LogBuffer.logException(e);
			LogBuffer.println("Could not find data start coordinates.");
			return new int[] {0, 0};
		}

		return dataStartCoords;
	}

	/** Check the current line for any sort of common label.
	 * 
	 * @param line - the String array to check
	 * @param pattern - the regex pattern to match
	 * @return Whether line includes a common label or not. */
	public static boolean hasCommonLabel(	final String[] line,
																				final String pattern) {

		for(String token : line) {
			if(Pattern.matches(pattern, token)) { return true; }
		}
		return false;
	}

	/** Check a string if it is part of the recognized common labels.
	 * 
	 * @param token - The token to check
	 * @param pattern - the regex pattern to match
	 * @return Whether token is a common label or not. */
	public static boolean isCommonLabel(final String token,
																			final String pattern) {

		if(Pattern.matches(pattern, token)) { return true; }
		return false;
	}

	/** Check if a string is a supposed numeric string that has been substituted
	 * due to missing/ non-available data etc.
	 * 
	 * @param token
	 * @return Whether token is a supposed-numeric string. */
	private static boolean isNaN(final String token) {

		if(Pattern.matches(PreviewLoader.MISSING, token)) { return true; }
		return false;
	}

	/** Check if the current string is numeric and can be parsed into a double.
	 * 
	 * @param token
	 * @return Whether token is parseable to double. */
	private static boolean isDoubleParseable(final String token) {

		try {
			Double.parseDouble(token);
			return true;

		}
		catch(NumberFormatException e) {
			return false;
		}
	}

	/** Small wrapper for <extractPreviewData>
	 * 
	 * @param filename
	 *          The path/ name of the file to be loaded.
	 * @return String[][] The preview data in array format for use in JTable. */
	public static String[][] loadPreviewData(	final String filename,
																						final String delimiter) {

		String[][] previewData;

		try {
			final BufferedReader br = new BufferedReader(new FileReader(filename));

			previewData = extractPreviewData(br, delimiter);

			br.close();

		}
		catch(final IOException e) {
			LogBuffer.logException(e);
			return new String[0][];
		}

		return previewData;
	}

	/** TODO put this into a PreviewLoader subclass which extends SwingWorker Get
	 * a String array representation of the first <LIMIT> elements of data. This
	 * is for use in the data preview dialog only. The data
	 * 
	 * @param A
	 *          BufferedReader object to read through the file.
	 * @return String[][] The preview data in array format for use in JTable.
	 * @author chris0689 */
	private static String[][] extractPreviewData(	final BufferedReader reader,
																								final String delimiter) {

		final String[][] previewData = new String[LIMIT][];
		String line;
		int count = 0;

		try {
			while((line = reader.readLine()) != null && count < LIMIT) {

				// load line as String array
				final String[] lineAsStrings = line.split(delimiter, -1);
				previewData[count++] = Arrays.copyOfRange(lineAsStrings, 0, LIMIT);
			}
		}
		catch(final IOException e) {
			LogBuffer.logException(e);
			return new String[][] {{"N/A"}};
		}

		return previewData;
	}
}
