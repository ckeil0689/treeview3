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
	public final static String ROW_AXIS_BASEID = "ROW";
	public final static String COL_AXIS_BASEID = "COL";
	
	public final static String ROW_ID_HEADER = "GID";
	public final static String COL_ID_HEADER = "AID";
	
	public final static String ROW_WEIGHT_ID = "GWEIGHT";
	public final static String COL_WEIGHT_ID = "EWEIGHT";
	
	private final String FILE_EXT = ".cdt";
	
	private final String KMEANS_DESIGNATOR = "_K";
	private final String KMEANS_ROW_SUFFIX = "_G";
	private final String KMEANS_COL_SUFFIX = "_A";

	private double[][] origMatrix;
	private final ClusteredAxisData rowClusterData;
	private final ClusteredAxisData colClusterData;
	private final boolean isHier;
	
	//private String[][] cdtData_s;

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
			final ClusteredAxisData rowClusterData, 
			final ClusteredAxisData colClusterData,
			final boolean isHier) {

		this.origMatrix = origMatrix;
		this.rowClusterData = rowClusterData;
		this.colClusterData = colClusterData;
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

			final String[] orderedGIDs = rowClusterData.getReorderedIDs();
			final String[] orderedAIDs = colClusterData.getReorderedIDs();
			
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

		this.rowClusterData.setAxisHeaders(rowHeaderI.getNames());
		this.colClusterData.setAxisHeaders(colHeaderI.getNames());

		/* The list containing all the reorganized row-data */
	//	this.cdtData_s = new String[origMatrix.length][];

		/* 
		 * retrieving names and weights of row elements
		 * format: [[YAL063C, 1.0], ..., [...]]
		 */
		this.rowClusterData.setAxisLabels(rowHeaderI.getHeaderArray());
		this.colClusterData.setAxisLabels(colHeaderI.getHeaderArray());
		
        int rowLabelNum = rowClusterData.getAxisLabelNum();
        int colLabelNum = colClusterData.getAxisLabelNum();
        
		/* Lists to be filled with reordered strings */
		this.rowClusterData.setOrderedAxisLabels(new String[rowLabelNum][]);
		this.colClusterData.setOrderedAxisLabels(new String[colLabelNum][]);
	}

	/**
	 * Manages the generation of a clustered data table (CDT) file.
	 */
	public void generateCDT() {

		/* First reorder the data according to clustering */
		this.origMatrix = orderData(origMatrix);

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
	 * @param matrixData A double[][] which contains the numerical data for
	 * the unclustered file.
	 * @return A double[][] which represents the data in the new ordering
	 * determined by clustering.
	 */
	private double[][] orderData(double[][] matrixData) {
		
		int[] reorderedRowIndices = getReorderedIndices(rowClusterData);
		int[] reorderedColIndices = getReorderedIndices(colClusterData);

		/* Order the numerical data. */
		return reorderMatrixData(reorderedRowIndices, reorderedColIndices, 
				matrixData);
	}
	
	/**
	 * Creates a list of the post-clustering axis index order.
	 * @param cd The ClusteredAxisData object for the axis for which indices
	 * are to be retrieved.
	 * @return An integer array of new axis indices, useful for reordering.
	 */
	private int[] getReorderedIndices(ClusteredAxisData cd) {
		
		int[] reorderedIndices = new int[cd.getAxisLabelNum()];
		int orderedIDNum = cd.getReorderedIDs().length;

		if (cd.shouldReorderAxis() && cd.isAxisClustered() 
				&& orderedIDNum != 0) {
			reorderedIndices = orderElements(cd);
		
	    /* old order simply remains */
		} else {
			for (int i = 0; i < reorderedIndices.length; i++) {
				reorderedIndices[i] = i;
			}
			cd.setOrderedAxisLabels(cd.getAxisLabels());
		}
		
		return reorderedIndices;
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
	 * @param cd The ClusteredAxisData objects containing all relevant info
	 * for label reordering.
	 * @return List of new element order indices that can be used to rearrange
	 *         the matrix data consistent with the new element ordering.
	 */
	private int[] orderElements(ClusteredAxisData cd) {

		String[][] orderedNames = new String[cd.getAxisLabelNum()][];
		int[] reorderedIndices = new int[cd.getAxisLabelNum()];

		// Make list of gene names to quickly access indexes
		final String[] geneNames = new String[cd.getAxisLabelNum()];

		if (!isHier) {
			for (int i = 0; i < geneNames.length; i++) {
				geneNames[i] = cd.getAxisLabels()[i][0];
			}
		}

		int index = -1;
		// Make an array of indexes from the ordered column list.
		for (int i = 0; i < reorderedIndices.length; i++) {
			final String id = cd.getReorderedIDs()[i];
			
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
			orderedNames[i] = cd.getAxisLabels()[index];
		}

		setReorderedNames(orderedNames, cd.getAxisBaseID());

		return reorderedIndices;
	}

	/**
	 * Setting the ordered names depending on the axis to be ordered.
	 * 
	 * @param orderedNames
	 * @param axisPrefix
	 */
	private void setReorderedNames(String[][] orderedNames, String axisPrefix) {

		if (axisPrefix.equals(rowClusterData.getAxisBaseID())) {
			this.rowClusterData.setOrderedAxisLabels(orderedNames);
			
		} else if (axisPrefix.equals(colClusterData.getAxisBaseID())) {
			this.colClusterData.setOrderedAxisLabels(orderedNames);
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

		String[] rowHeaders = rowClusterData.getAxisHeaders();
		String[] colHeaders = colClusterData.getAxisHeaders();
		
		final boolean hasGID = findIndex(rowHeaders, ROW_ID_HEADER) != -1;
		
		String[] orderedGIDs = rowClusterData.getReorderedIDs();
		String[] orderedAIDs = colClusterData.getReorderedIDs();

		int rowLength = rowHeaders.length + colClusterData.getAxisLabelNum();
		if (rowClusterData.isAxisClustered() && !hasGID) {
			rowLength++;
		}

		final int dataStart = rowLength - colClusterData.getAxisLabelNum();

		final String[] cdtRow = new String[rowLength];
		int addIndex;

		/* The first row */
		addIndex = 0;
		if (rowClusterData.isAxisClustered() && !hasGID) {
			cdtRow[addIndex] = ROW_ID_HEADER;
			addIndex++;
		}

		System.arraycopy(rowHeaders, 0, cdtRow, addIndex, rowHeaders.length);
		addIndex += rowHeaders.length;

		/* Adding column names to first row */
		for (final String[] names : colClusterData.getOrderedAxisLabels()) {
			cdtRow[addIndex] = names[0];
			addIndex++;
		}

		/* write finished row */
		bufferedWriter.writeContent(cdtRow);

		/* next row */
		/* if columns were clustered, make AID row */
		if (colClusterData.isAxisClustered()) {
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
			
			cdtRow[addIndex] = colHeaders[i];
			addIndex++;
			
			while (addIndex < dataStart) {
				cdtRow[addIndex] = "";
				addIndex++;
			}

			for (final String[] names : colClusterData.getOrderedAxisLabels()) {
				cdtRow[addIndex] = names[i];
				addIndex++;
			}

			bufferedWriter.writeContent(cdtRow);
		}

		/* Filling the data rows */
//		for (int i = 0; i < cdtData_s.length; i++) {
		for (int i = 0; i < origMatrix.length; i++) {

			addIndex = 0;
			final String[] row = new String[rowLength];
			String[] names = rowClusterData.getOrderedAxisLabels()[i];
			
			/* Adding GIDs ("ROW130X") */
			if (rowClusterData.isAxisClustered() && !hasGID) {
				row[addIndex] = orderedGIDs[i];
				addIndex++;
			
			/* 
			 * Ensure the labels are consistent with what was created for 
			 * orderedGIDs. For example, an old file might already contain 
			 * GENE23X etc. but the naming was ditched for ROW23X. If this
			 * isn't corrected, then tree files will not match up with the
			 * cdt.	
			 */
			} else if (rowClusterData.isAxisClustered() && hasGID){
				names[0] = orderedGIDs[i];
			}

			/* Adding row names */
			System.arraycopy(names, 0, row, addIndex, names.length);
			addIndex += names.length;

			/* Adding data values */
			String[] rowData = getStringArray(origMatrix[i]);
			System.arraycopy(rowData, 0, row, addIndex, rowData.length);

			bufferedWriter.writeContent(row);
		}
	}
	
	/**
	 * Smaller helper routine. Transforms a double array to a String array.
	 * @param dArray The double array.
	 * @return An array of Strings which represent the values of the input 
	 * double array.
	 */
	private static String[] getStringArray(double[] dArray) {
		
		String[] sArray = new String[dArray.length];
		
		for(int i = 0; i < dArray.length; i++) {
			sArray[i] = String.valueOf(dArray[i]);
		}
		
		return sArray;
	}

	/**
	 * This method fills the String matrix with names for rows/ columns and
	 * other elements.
	 */
	private void fillKMeans() {

		final String[] rowHeaders = rowClusterData.getAxisHeaders();
		final int rowLength = rowHeaders.length 
				+ colClusterData.getAxisLabelNum();
		final String[] cdtRow1 = new String[rowLength];
		int addIndex = 0;
		
		for (final String element : rowHeaders) {
			cdtRow1[addIndex] = element;
			addIndex++;
		}

		/* Adding column names to first row */
		for (final String[] element : colClusterData.getOrderedAxisLabels()) {
			cdtRow1[addIndex] = element[0];
			addIndex++;
		}

		bufferedWriter.writeContent(cdtRow1);

		/* Fill and add second row */
		addIndex = 0;
		final String[] cdtRow2 = new String[rowLength];

		cdtRow2[addIndex] = COL_WEIGHT_ID;
		addIndex++;

		for (int i = 0; i < rowHeaders.length - 1; i++) {
			cdtRow2[addIndex] = "";
			addIndex++;
		}

		/* Fill with weights */
		for (final String[] element : colClusterData.getOrderedAxisLabels()) {
			cdtRow2[addIndex] = element[1];
			addIndex++;
		}

		bufferedWriter.writeContent(cdtRow2);

		/* 
		 * Add gene names in ORF and NAME columns (0 & 1) and GWeights (2)
		 * buffer is just the amount of rows before the data starts
		 */
		for (int i = 0; i < origMatrix.length; i++) {

			addIndex = 0;
			final String[] row = new String[rowLength];

			for (int j = 0; j < rowHeaders.length; j++) {
				row[addIndex] = rowClusterData.getOrderedAxisLabels()[i][j];
				addIndex++;
			}

			for (int j = 0; j < origMatrix[i].length; j++) {
				row[addIndex] = String.valueOf(origMatrix[i][j]);
				addIndex++;
			}

			// Check whether it's the last line
			bufferedWriter.writeContent(row);
		}
	}
}
