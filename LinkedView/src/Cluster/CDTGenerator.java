package Cluster;

import java.io.File;
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
	 * The task for this class is to take supplied data that is the result
	 * of clustering and format it into a new CDT file that can be read and 
	 * interpreted by TreeView. 
	 * @param origMatrix The original data matrix.
	 * @param orderedRows Row labels after clustering.
	 * @param orderedCols Column labels after clustering.
	 * @param isHier Indicates type of clustering.
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
	public void setupWriter(String fileName, Integer[] spinnerInput) {

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

		final File file = new File(fileName + fileEnd);
		try {
			file.createNewFile();
			this.bufferedWriter = new ClusterFileWriter(file);
			
		} catch (IOException e) {
			LogBuffer.logException(e);
		}
	}
	
	/**
	 * Sets up instance variables needed for writing.
	 * @param geneHeaderI
	 * @param arrayHeaderI
	 */
	public void prepare(IntHeaderInfo geneHeaderI, IntHeaderInfo arrayHeaderI) {
		
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
			LogBuffer.println("Writing hierarchical data.");
			fillHierarchical();

		} else {
			LogBuffer.println("Writing k-means data.");
			fillKMeans();
		}
	}

	/**
	 * This method reorders the double data if the axis was clustered.
	 */
	private void orderData() {

		final int[] reorderedRowIndices = new int[origMatrix.length];
		final int[] reorderedColIndices = new int[colNames.length];

		cdtData_doubles = new double[reorderedRowIndices.length]
				[reorderedColIndices.length];

		if (isRowClustered) {
			final String[] geneNames = new String[rowNames.length];
			if (!isHier) {
				for (int i = 0; i < geneNames.length; i++) {

					geneNames[i] = rowNames[i][0];
				}
			}

			int rowIndex = -1;
			for (int i = 0; i < orderedGIDs.length; i++) {

				final String rowElement = orderedGIDs[i];

				if (isHier) {
					// Regex: Non-digits ('\D') are replaced with "" (no space!)
					// This means: GENE456X -> 456
					final String adjusted = rowElement.replaceAll("[\\D]", "");

					// Adjusted spring is made into integer which can be used
					// as index
					rowIndex = Integer.parseInt(adjusted);

				} else {
					rowIndex = findIndex(geneNames, rowElement);
				}

//				reorderedRowIndices[i] = rowIndex;

				// Order the row names
				String target = "GENE" + rowIndex + "X";
				int newRowIndex = findID(rowNames, target, 0);
				
				int finalIndex = (newRowIndex == -1) ? rowIndex : newRowIndex;
				
				reorderedRowIndices[i] = finalIndex;
				
				rowNamesOrdered[i] = rowNames[finalIndex];
			}
		} else {
			for (int i = 0; i < reorderedRowIndices.length; i++) {
				reorderedRowIndices[i] = i;
			}
			rowNamesOrdered = rowNames;
		}

		if (isColClustered) {
			// Make list of gene names to quickly access indexes
			final String[] geneNames = new String[colNames.length];

			if (!isHier) {
				for (int i = 0; i < geneNames.length; i++) {

					geneNames[i] = colNames[i][0];
				}
			}

			int colIndex = -1;
			// Make an array of indexes from the ordered column list.
			for (int i = 0; i < reorderedColIndices.length; i++) {

				final String colElement = orderedAIDs[i];
				if (isHier) {
					final String adjusted = colElement.replaceAll("[\\D]", "");

					// gets index from ordered list, e.g. ARRY45X --> 45;
					colIndex = Integer.parseInt(adjusted);

				} else {
					colIndex = findIndex(geneNames, colElement);
				}

//				reorderedColIndices[i] = colIndex;
				
				String target = "ARRY" + colIndex + "X";
				int newColIndex = findID(colNames, target, 1);
				
				int finalIndex = (newColIndex == -1) ? colIndex : newColIndex;
				
				reorderedColIndices[i] = finalIndex;

				// reordering column names
				colNamesOrdered[i] = colNames[finalIndex];
			}
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
	 * Finishes up CDTGenerator by closing the buffered writer.
	 * Returns the file path where the cdt file was saved.
	 */
	public String finish() {
		
		String filePath = bufferedWriter.getFilePath();
		
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
	private int findID(final String[][] array, final String element, int axis) {

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
		
		boolean hasGID = findIndex(rowHeaders, "GID") != - 1;
		
		int rowLength = rowHeaders.length + colNames.length;
		if (isRowClustered && !hasGID) rowLength++;
		
		int dataStart = rowLength - colNames.length;
		
		final String[] cdtRow = new String[rowLength];
		int addIndex;
		
		/* The first row */
		addIndex = 0;
		if (isRowClustered && !hasGID) cdtRow[addIndex++] = "GID";
		
		System.arraycopy(rowHeaders, 0, cdtRow, addIndex, rowHeaders.length);
		addIndex += rowHeaders.length;

		/* Adding column names to first row */
		for (String[] names : colNamesOrdered) {

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
		for(int i = 1; i < colHeaders.length; i++) {
			
			if(colHeaders[i].equalsIgnoreCase("AID")) continue;
			
			addIndex = 0;
			cdtRow[addIndex++] = colHeaders[i];
			
			while(addIndex < dataStart) {
				cdtRow[addIndex++] = "";
			}

			for (String[] names : colNamesOrdered) {

				cdtRow[addIndex++] = names[i];
			}

			bufferedWriter.writeContent(cdtRow);
		}

		/* Filling the data rows */
		for (int i = 0; i < cdtData_s.length; i++) {
			
			addIndex = 0;
			final String[] row = new String[rowLength];

			/* Adding GIDs ("GENE130X") */
			if (isRowClustered && !hasGID) row[addIndex++] = orderedGIDs[i];

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
		for (int i = 0; i < colNamesOrdered.length; i++) {

			cdtRow1[addIndex++] = colNamesOrdered[i][0];
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
		for (int i = 0; i < colNamesOrdered.length; i++) {

			cdtRow2[addIndex++] = colNamesOrdered[i][1];
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
