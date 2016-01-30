package Cluster;

import java.io.IOException;

import edu.stanford.genetics.treeview.model.IntHeaderInfo;

/**
 * This class is used to generate the .CDT tab delimited file which Java
 * TreeView will use for visualization. It takes in the previously calculated
 * data and forms String lists to make them writable.
 *
 * @author CKeil
 *
 */
public class ClusterFileGenerator {

	// Important cluster strings for the files
	public final static String ROW_AXIS_ID = "ROW";
	public final static String COL_AXIS_ID = "COL";
	
	public final static String ROW_ID_HEADER = "GID";
	public final static String COL_ID_HEADER = "AID";
	
	public final static String ROW_WEIGHT_ID = "GWEIGHT";
	public final static String COL_WEIGHT_ID = "EWEIGHT";
	
	private final String FILE_EXT = ".cdt";
	
	private final String KMEANS_DESIGNATOR = "_K";
	private final String KMEANS_ROW_SUFFIX = "_G";
	private final String KMEANS_COL_SUFFIX = "_A";
	
	private String[] rowHeaders;
	private String[] colHeaders;

	private final boolean isHier;
	private final boolean isRowClustered;
	private final boolean shouldReorderRows;
	private final boolean isColClustered;
	private final boolean shouldReorderCols;

	private final double[][] origMatrix;

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
	 * @param origMatrix The original data matrix.
	 * @param orderedRows Row labels after clustering.
	 * @param orderedCols Column labels after clustering.
	 * @param isRowClustered Indicates whether the row axis is considered to
	 * be clustered.
	 * @param shouldReorderRows Indicates, whether row labels should be reordered. 
	 * This is different from <isRowClustered> because a row can be considered
	 * clustered and should not undergo reordering, e.g. tree file transfer
	 * when an axis was already clustered earlier.
	 * @param isColClustered Indicates whether the row axis is considered to
	 * be clustered.
	 * @param shouldReorderCols Indicates, whether row labels should be reordered. 
	 * This is different from <isColClustered> because a row can be considered
	 * clustered and should not undergo reordering, e.g. tree file transfer
	 * when an axis was already clustered earlier.
	 * @param isHier Indicates type of clustering. If true, clustering is
	 * hierarchical. If false, clustering is k-means.
	 */
	public ClusterFileGenerator(final double[][] origMatrix,
			final String[] orderedRows, final boolean isRowClustered, 
			final boolean shouldReorderRows, 
			final String[] orderedCols, final boolean isColClustered, 
			final boolean shouldReorderCols,
			final boolean isHier) {

		this.origMatrix = origMatrix;
		this.orderedGIDs = orderedRows;
		this.orderedAIDs = orderedCols;
		this.isRowClustered = isRowClustered;
		this.shouldReorderRows = shouldReorderRows;
		this.isColClustered = isColClustered;
		this.shouldReorderCols = shouldReorderCols;
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
			fileEnd = FILE_EXT;

		// k-means file names have a few more details	
		} else {
			String rowC = "";
			String colC = "";

			final int row_clusterN = spinnerInput[0];
			final int col_clusterN = spinnerInput[2];

			
			if (orderedGIDs != null && orderedGIDs.length > 0) {
				rowC = KMEANS_ROW_SUFFIX + row_clusterN;
			}

			if (orderedAIDs != null && orderedAIDs.length > 0) {
				colC = KMEANS_COL_SUFFIX + col_clusterN;
			}

			fileEnd = KMEANS_DESIGNATOR + rowC + colC + FILE_EXT;
		}

		this.bufferedWriter = new ClusterFileWriter(fileName, fileEnd, link);
	}

	/**
	 * Sets up instance variables needed for writing.
	 *
	 * @param rowHeaderI <IntHeaderInfo> object for the row labels.
	 * @param colHeaderI <IntHeaderInfo> object for the column labels.
	 */
	public void prepare(final IntHeaderInfo rowHeaderI,
			final IntHeaderInfo colHeaderI) {

		this.rowHeaders = rowHeaderI.getNames();
		this.colHeaders = colHeaderI.getNames();

		/* The list containing all the reorganized row-data */
		this.cdtData_s = new String[origMatrix.length][];

		/* 
		 * retrieving names and weights of row elements
		 * format: [[YAL063C, 1.0], ..., [...]]
		 */
		this.rowNames = rowHeaderI.getHeaderArray();

		/* 
		 * retrieving names and weights of column elements
		 * format: [[YAL063C, 1.0], ..., [...]]
		 */
		this.colNames = colHeaderI.getHeaderArray();

		/* Lists to be filled with reordered strings */
		this.rowNamesOrdered = new String[rowNames.length][];
		this.colNamesOrdered = new String[colNames.length][];
	}

	/**
	 * Manages the generation of a clustered data table (CDT) file.
	 */
	public void generateCDT() {

		/* First order the data according to clustering */
		double[][] reorderedMatrixData = orderData();

		/* Transform cdtDataFile from double lists to string lists */
		for (int i = 0; i < reorderedMatrixData.length; i++) {

			final double[] element = reorderedMatrixData[i];
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
	 * This method reorders the matrix data for an axis if it was newly 
	 * clustered. Transferred cluster files (from old data sets) are considered
	 * clustered, but do not have to be reordered.
	 */
	private double[][] orderData() {
		
		int[] reorderedRowIndices = new int[origMatrix.length];
		int[] reorderedColIndices = new int[colNames.length];

		// TODO implement axis settings object to store axis features 
		//such as row names, orderedIDs etc. and just retrieve them by axisID.
		if (shouldReorderRows && isRowClustered && orderedGIDs.length != 0) {
			reorderedRowIndices = orderElements(rowNames, orderedGIDs, 
					ROW_AXIS_ID);
			
		} else {
			/* old order simply remains */
			for (int i = 0; i < reorderedRowIndices.length; i++) {
				reorderedRowIndices[i] = i;
			}
			rowNamesOrdered = rowNames;
		}

		if (shouldReorderCols && isColClustered && orderedAIDs.length != 0) {
			reorderedColIndices = orderElements(colNames, orderedAIDs, 
					COL_AXIS_ID);
			
		} else {
			/* old order simply remains */
			for (int i = 0; i < reorderedColIndices.length; i++) {
				reorderedColIndices[i] = i;
			}
			colNamesOrdered = colNames;
		}

		/* Order the numerical data. */
		return reorderMatrixData(reorderedRowIndices, 
				reorderedColIndices, origMatrix);
	}
	
	/**
	 * Uses lists of reordered axis indices to reorder the data matrix.
	 * @param reorderedRowIndices List of reordered row indices.
	 * @param reorderedColIndices List of reordered column indices 
	 * @param origMatrix The data matrix to be reordered.
	 */
	private static double[][] reorderMatrixData(final int[] reorderedRowIndices, 
			final int[] reorderedColIndices, final double[][] origMatrix) {
		
		int rows = reorderedRowIndices.length;
		int cols = reorderedColIndices.length;

		double[][] reorderedMatrixData = new double[rows][cols];
		
		/* Order the numerical data. */
		int row = -1;
		int col = -1;

		for (int i = 0; i < rows; i++) {
			row = reorderedRowIndices[i];

			for (int j = 0; j < cols; j++) {
				col = reorderedColIndices[j];
				reorderedMatrixData[i][j] = origMatrix[row][col];
			}
		}
		
		return reorderedMatrixData;
	}

	/**
	 * Orders the labels for the CDT data based on the ordered ID String arrays.
	 * 
	 * @param origNames
	 *            The original axis labels before clustering.
	 * @param orderedIDs
	 *            The newly ordered axis IDs.
	 * @param axisPrefix
	 *            The axis prefix to identify which axis is being reordered.
	 * @return List of new element order indices that can be used to rearrange
	 *         the matrix data consistent with the new element ordering.
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
				// extract numerical part of element ID
				final String adjusted = id.replaceAll("[\\D]", "");

				// gets index from ordered list, e.g. COL45X --> 45;
				index = Integer.parseInt(adjusted);

			} else {
				index = findIndex(geneNames, id);
			}

			reorderedIndices[i] = index;

			// reordering column names
			orderedNames[i] = origNames[index];
		}

		setReorderedNames(orderedNames, axisPrefix);

		return reorderedIndices;
	}

	/**
	 * Setting the ordered names depending on the axis to be ordered.
	 * 
	 * @param orderedNames
	 * @param axisPrefix
	 */
	private void setReorderedNames(String[][] orderedNames, String axisPrefix) {

		if (axisPrefix.equals(ROW_AXIS_ID)) {
			this.rowNamesOrdered = orderedNames;
			
		} else if (axisPrefix.equals(COL_AXIS_ID)) {
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
	private static int findIndex(final String[] array, final String element) {

		int index = -1;
		for (int i = 0; i < array.length; i++) {

			if (array[i].equalsIgnoreCase(element)) {
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

		final boolean hasGID = findIndex(rowHeaders, ROW_ID_HEADER) != -1;

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
			cdtRow[addIndex++] = ROW_ID_HEADER;
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
			cdtRow[0] = COL_ID_HEADER;

			/* Fill with AIDs ("COL3X") */
			System.arraycopy(orderedAIDs, 0, cdtRow, dataStart,
					orderedAIDs.length);

			bufferedWriter.writeContent(cdtRow);
		}

		/* remaining label rows */
		for (int i = 1; i < colHeaders.length; i++) {

			if (colHeaders[i].equals(COL_ID_HEADER)) {
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
			String[] names = rowNamesOrdered[i];
			
			/* Adding GIDs ("ROW130X") */
			if (isRowClustered && !hasGID) {
				row[addIndex++] = orderedGIDs[i];
			
			/* 
			 * Ensure the labels are consistent with what was created for 
			 * orderedGIDs. For example, an old file might already contain 
			 * GENE23X etc. but the naming was ditched for ROW23X. If this
			 * isn't corrected, then tree files will not match up with the
			 * cdt.	
			 */
			} else if (isRowClustered && hasGID){
				names[0] = orderedGIDs[i];
			}

			/* Adding row names */
			System.arraycopy(names, 0, row, addIndex, names.length);
			addIndex += names.length;

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

		cdtRow2[addIndex++] = COL_WEIGHT_ID;

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
}
