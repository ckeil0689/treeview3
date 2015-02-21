package Cluster;

import java.io.IOException;

import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.IntHeaderInfo;

/**
 * This class is used to generate the .CDT tab delimited file which Java
 * TreeView will use for visualization. It takes in the previously calculated
 * data and forms String lists to make them writable.
 *
 * @author CKeil
 *
 */
public class CDTGenerator {

	private String[] rowHeaders;
	private String[] colHeaders;

	private String filePath;
	private final boolean isHier;
	private final boolean isRowClustered;
	private final boolean isColClustered;

	private final double[][] origMatrix;
	private double[][] cdtData_doubles;

	private String[][] rowNames;
	private String[][] colNames;
	private String[][] rowNamesOrdered;
	private String[][] colNamesOrdered;
	private String[][] cdtData_s;

	private final String[] orderedGIDs;
	private final String[] orderedAIDs;

	private ClusterFileWriter bufferedWriter;

	/**
	 * The task for this class is to take supplied data that is the result of
	 * clustering and format it into a new CDT file that can be read and
	 * interpreted by TreeView.
	 *
	 * @param origMatrix
	 *            The original data matrix.
	 * @param orderedRows
	 *            Row labels after clustering.
	 * @param orderedCols
	 *            Column labels after clustering.
	 * @param isHier
	 *            Indicates type of clustering.
	 */
	public CDTGenerator(final double[][] origMatrix,
			final String[] orderedRows, final String[] orderedCols,
			final int rowSimilarity, final int colSimilarity,
			final boolean isHier) {

		this.origMatrix = origMatrix;
		this.orderedGIDs = orderedRows;
		this.orderedAIDs = orderedCols;
		this.isRowClustered = (rowSimilarity != 0);
		this.isColClustered = (colSimilarity != 0);
		this.isHier = isHier;
	}

	/**
	 * Sets up a buffered writer to generate the .cdt file from the previously
	 * clustered data.
	 *
	 * @throws IOException
	 */
	public void setupWriter(final String fileName, final int link, 
			final Integer[] spinnerInput) {

		String fileEnd = "";

		if (isHier) {
			fileEnd = ".cdt";

		} else {
			String rowC = "";
			String colC = "";

			final int row_clusterN = spinnerInput[0];
			final int col_clusterN = spinnerInput[2];

			if (orderedGIDs != null && orderedGIDs.length > 0) {
				rowC = "_G" + row_clusterN;
			}

			if (orderedAIDs != null && orderedAIDs.length > 0) {
				colC = "_A" + col_clusterN;
			}

			fileEnd = "_K" + rowC + colC + ".cdt";
		}
		
		this.bufferedWriter = new ClusterFileWriter(fileName, fileEnd, link);
	}

	/**
	 * Sets up instance variables needed for writing.
	 *
	 * @param geneHeaderI
	 * @param arrayHeaderI
	 */
	public void prepare(final IntHeaderInfo geneHeaderI,
			final IntHeaderInfo arrayHeaderI) {

		this.rowHeaders = geneHeaderI.getNames();
		this.colHeaders = arrayHeaderI.getNames();

		/* The list containing all the reorganized row-data */
		this.cdtData_doubles = new double[origMatrix.length][];
		this.cdtData_s = new String[origMatrix.length][];

		// retrieving names and weights of row elements
		// format: [[YAL063C, 1.0], ..., [...]]
		this.rowNames = geneHeaderI.getHeaderArray();

		// retrieving names and weights of column elements
		// format: [[YAL063C, 1.0], ..., [...]]
		this.colNames = arrayHeaderI.getHeaderArray();

		// Lists to be filled with reordered strings
		this.rowNamesOrdered = new String[rowNames.length][];
		this.colNamesOrdered = new String[colNames.length][];
	}

	public void generateCDT() {

		/* First order the data according to clustering */
		orderData();

		/* Transform cdtDataFile from double lists to string lists */
		for (int i = 0; i < cdtData_doubles.length; i++) {

			final double[] element = cdtData_doubles[i];

			final String[] newStringData = new String[element.length];

			for (int j = 0; j < element.length; j++) {

				newStringData[j] = String.valueOf(element[j]);
			}

			cdtData_s[i] = newStringData;
		}

		/* Add some string elements, as well as row/ column names */
		if (isHier) {
			fillHierarchical();

		} else {
			fillKMeans();
		}
	}

	/**
	 * This method reorders the double data if the axis was clustered.
	 */
	private void orderData() {

		int[] reorderedRowIndices = new int[origMatrix.length];
		int[] reorderedColIndices = new int[colNames.length];

		cdtData_doubles = new double[reorderedRowIndices.length][reorderedColIndices.length];

		if (isRowClustered) {
			reorderedRowIndices = orderElements(rowNames, orderedGIDs, "GENE");
//			final String[] geneNames = new String[rowNames.length];
//			if (!isHier) {
//				for (int i = 0; i < geneNames.length; i++) {
//
//					geneNames[i] = rowNames[i][0];
//				}
//			}
//
//			int rowIndex = -1;
//			for (int i = 0; i < orderedGIDs.length; i++) {
//
//				final String rowElement = orderedGIDs[i];
//
//				if (isHier) {
//					// Regex: Non-digits ('\D') are replaced with "" (no space!)
//					// This means: GENE456X -> 456
//					final String adjusted = rowElement.replaceAll("[\\D]", "");
//
//					// Adjusted spring is made into integer which can be used
//					// as index
//					rowIndex = Integer.parseInt(adjusted);
//
//				} else {
//					rowIndex = findIndex(geneNames, rowElement);
//				}
//
//				 reorderedRowIndices[i] = rowIndex;
//
//				// Order the row names
//				final String target = "GENE" + rowIndex + "X";
//				final int newRowIndex = findID(rowNames, target, 0);
//
//				final int finalIndex = (newRowIndex == -1) ? rowIndex
//						: newRowIndex;
//
//				rowNamesOrdered[i] = rowNames[finalIndex];
//			}
		} else {
			for (int i = 0; i < reorderedRowIndices.length; i++) {
				reorderedRowIndices[i] = i;
			}
			rowNamesOrdered = rowNames;
		}

		if (isColClustered) {
			reorderedColIndices = orderElements(colNames, orderedAIDs, "ARRY");
//			// Make list of gene names to quickly access indexes
//			final String[] geneNames = new String[colNames.length];
//
//			if (!isHier) {
//				for (int i = 0; i < geneNames.length; i++) {
//
//					geneNames[i] = colNames[i][0];
//				}
//			}
//
//			int colIndex = -1;
//			// Make an array of indexes from the ordered column list.
//			for (int i = 0; i < reorderedColIndices.length; i++) {
//
//				final String colElement = orderedAIDs[i];
//				if (isHier) {
//					final String adjusted = colElement.replaceAll("[\\D]", "");
//
//					// gets index from ordered list, e.g. ARRY45X --> 45;
//					colIndex = Integer.parseInt(adjusted);
//
//				} else {
//					colIndex = findIndex(geneNames, colElement);
//				}
//
//				 reorderedColIndices[i] = colIndex;
//
//				final String target = "ARRY" + colIndex + "X";
//				final int newColIndex = findID(colNames, target, 1);
//
//				final int finalIndex = (newColIndex == -1) ? colIndex
//						: newColIndex;
//
//				// reordering column names
//				colNamesOrdered[i] = colNames[finalIndex];
//			}
		} else {
			/* TODO change to use index from orderedAIDs instead */
			for (int i = 0; i < reorderedColIndices.length; i++) {
				reorderedColIndices[i] = i;
			}

			colNamesOrdered = colNames;
		}

		/* Order the numerical data. */
		int row = -1;
		int col = -1;

		for (int i = 0; i < reorderedRowIndices.length; i++) {

			row = reorderedRowIndices[i];

			for (int j = 0; j < reorderedColIndices.length; j++) {

				col = reorderedColIndices[j];
				cdtData_doubles[i][j] = origMatrix[row][col];
			}
		}
	}
	
	/**
	 * Orders the labels for the CDT data based on the ordered ID String arrays.
	 * @param origNames The original axis labels before clustering.
	 * @param orderedIDs The newly ordered axis IDs.
	 * @param axisPrefix The axis prefix to identify which axis is being
	 * reordered.
	 * @return List of new element order indices that can be used to
	 * rearrange the matrix data consistent with the new element ordering.
	 */
	private int[] orderElements(String[][] origNames, String[] orderedIDs, 
			String axisPrefix) {
		
		String[][] orderedNames = new String[origNames.length][];
		int[] reorderedIndices = new int[origNames.length];
		
		// Make list of gene names to quickly access indexes
		final String[] geneNames = new String[origNames.length];

		if (!isHier) {
			for (int i = 0; i < geneNames.length; i++) {

				geneNames[i] = origNames[i][0];
			}
		}

		int index = -1;
		// Make an array of indexes from the ordered column list.
		for (int i = 0; i < reorderedIndices.length; i++) {

			final String id = orderedIDs[i];
			if (isHier) {
				final String adjusted = id.replaceAll("[\\D]", "");

				// gets index from ordered list, e.g. ARRY45X --> 45;
				index = Integer.parseInt(adjusted);

			} else {
				index = findIndex(geneNames, id);
			}

			reorderedIndices[i] = index;

//			final String target = axisPrefix + index + "X";
			int axisID; 
			if(axisPrefix.equalsIgnoreCase("GENE")) {
				axisID = 0;
			} else {
				axisID = 1;
			}
			
			final int newColIndex = findID(origNames, id, axisID);

			final int finalIndex = (newColIndex == -1) ? index : newColIndex;

			// reordering column names
			orderedNames[i] = origNames[finalIndex];
		}
		
		setReorderedNames(orderedNames, axisPrefix);
		
		return reorderedIndices;
	}
	
	/**
	 * Setting the ordered names depending on the axis to be ordered.
	 * @param orderedNames
	 * @param axisPrefix
	 */
	private void setReorderedNames(String[][] orderedNames, 
			String axisPrefix) {
		
		if(axisPrefix.equalsIgnoreCase("GENE")) {
			this.rowNamesOrdered = orderedNames;
		} else if(axisPrefix.equalsIgnoreCase("ARRY")) {
			this.colNamesOrdered = orderedNames;
		}
	}

	/**
	 * Finishes up CDTGenerator by closing the buffered writer. Returns the file
	 * path where the cdt file was saved.
	 */
	public String finish() {

		final String filePath = bufferedWriter.getFilePath();

		bufferedWriter.closeWriter();

		return filePath;
	}

	/**
	 * Finds the index of an element in a String array.
	 *
	 * @param array
	 * @param element
	 * @return
	 */
	private int findIndex(final String[] array, final String element) {

		int index = -1;
		for (int i = 0; i < array.length; i++) {

			if (array[i].equalsIgnoreCase(element)) {
				index = i;
			}
		}

		return index;
	}

	/**
	 * Finds the index of an element in a String array.
	 *
	 * @param array
	 * @param element
	 * @return
	 */
	private int findID(final String[][] array, final String element,
			final int axis) {

		int index = -1;
		for (int i = 0; i < array.length; i++) {

			if (array[i][axis].equalsIgnoreCase(element)) {
				index = i;
			}
		}

		return index;
	}

	/**
	 * This method fills the String matrix with names for rows/ columns and
	 * other elements.
	 */
	private void fillHierarchical() {

		final boolean hasGID = findIndex(rowHeaders, "GID") != -1;

		int rowLength = rowHeaders.length + colNames.length;
		if (isRowClustered && !hasGID) {
			rowLength++;
		}

		final int dataStart = rowLength - colNames.length;

		final String[] cdtRow = new String[rowLength];
		int addIndex;

		/* The first row */
		addIndex = 0;
		if (isRowClustered && !hasGID) {
			cdtRow[addIndex++] = "GID";
		}

		System.arraycopy(rowHeaders, 0, cdtRow, addIndex, rowHeaders.length);
		addIndex += rowHeaders.length;

		/* Adding column names to first row */
		for (final String[] names : colNamesOrdered) {

			cdtRow[addIndex++] = names[0];
		}

		/* write finished row */
		bufferedWriter.writeContent(cdtRow);

		/* next row */
		/* if columns were clustered, make AID row */
		if (isColClustered) {

			cdtRow[0] = "AID";

			/* Fill with AIDs ("ARRY3X") */
			System.arraycopy(orderedAIDs, 0, cdtRow, dataStart,
					orderedAIDs.length);

			bufferedWriter.writeContent(cdtRow);
		}

		/* remaining label rows */
		for (int i = 1; i < colHeaders.length; i++) {

			if (colHeaders[i].equalsIgnoreCase("AID")) {
				continue;
			}

			addIndex = 0;
			cdtRow[addIndex++] = colHeaders[i];

			while (addIndex < dataStart) {
				cdtRow[addIndex++] = "";
			}

			for (final String[] names : colNamesOrdered) {

				cdtRow[addIndex++] = names[i];
			}

			bufferedWriter.writeContent(cdtRow);
		}

		/* Filling the data rows */
		for (int i = 0; i < cdtData_s.length; i++) {

			addIndex = 0;
			final String[] row = new String[rowLength];

			/* Adding GIDs ("GENE130X") */
			if (isRowClustered && !hasGID) {
				row[addIndex++] = orderedGIDs[i];
			}

			/* Adding row names */
			System.arraycopy(rowNamesOrdered[i], 0, row, addIndex,
					rowNamesOrdered[i].length);
			addIndex += rowNamesOrdered[i].length;

			/* Adding data values */
			System.arraycopy(cdtData_s[i], 0, row, addIndex,
					cdtData_s[i].length);

			bufferedWriter.writeContent(row);
		}
	}

	/**
	 * This method fills the String matrix with names for rows/ columns and
	 * other elements.
	 */
	private void fillKMeans() {

		final int rowLength = rowHeaders.length + colNames.length;

		final String[] cdtRow1 = new String[rowLength];

		int addIndex = 0;
		for (final String element : rowHeaders) {

			cdtRow1[addIndex++] = element;
		}

		// Adding column names to first row
		for (final String[] element : colNamesOrdered) {

			cdtRow1[addIndex++] = element[0];
		}

		bufferedWriter.writeContent(cdtRow1);

		// Fill and add second row
		addIndex = 0;
		final String[] cdtRow2 = new String[rowLength];

		cdtRow2[addIndex++] = "EWEIGHT";

		for (int i = 0; i < rowHeaders.length - 1; i++) {

			cdtRow2[addIndex++] = "";
		}

		// Fill with weights
		for (final String[] element : colNamesOrdered) {

			cdtRow2[addIndex++] = element[1];
		}

		bufferedWriter.writeContent(cdtRow2);

		// Add gene names in ORF and NAME columns (0 & 1) and GWeights (2)
		// buffer is just the amount of rows before the data starts
		for (int i = 0; i < cdtData_s.length; i++) {

			addIndex = 0;
			final String[] row = new String[rowLength];

			for (int j = 0; j < rowHeaders.length; j++) {

				row[addIndex++] = rowNamesOrdered[i][j];
			}

			for (int j = 0; j < cdtData_s[i].length; j++) {

				row[addIndex++] = cdtData_s[i][j];
			}

			// Check whether it's the last line
			bufferedWriter.writeContent(row);
		}
	}

	/**
	 * Gets the filePath for the current source file.
	 *
	 * @return
	 */
	public String getFilePath() {

		return filePath;
	}
}
