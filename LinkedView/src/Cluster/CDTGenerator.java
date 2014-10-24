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

	private IntHeaderInfo geneHeaderI;

	private String filePath;
	private final boolean hierarchical;
	private final boolean isRowMethodChosen;
	private final boolean isColMethodChosen;

	private final double[][] origMatrix;
	private double[][] cdtData_doubles;

	private String[][] rowNames;
	private String[][] colNames;
	private String[][] rowNameListOrdered;
	private String[][] colNameListOrdered;
	private String[][] cdtData_strings;

	private final String[] orderedRows;
	private final String[] orderedCols;

	private ClusterFileWriter bufferedWriter;

	/**
	 * The task for this class is to take supplied data that is the result
	 * of clustering and format it into a new CDT file that can be read and 
	 * interpreted by TreeView. 
	 * @param origMatrix The original data matrix.
	 * @param orderedRows Row labels after clustering.
	 * @param orderedCols Column labels after clustering.
	 * @param hierarchical Indicates type of clustering.
	 */
	public CDTGenerator(final double[][] origMatrix, 
			final String[] orderedRows, final String[] orderedCols, 
			final int rowSimilarity, final int colSimilarity,
			final boolean hierarchical) {

		LogBuffer.println("Initializing CDTGenerator.");
		
		this.origMatrix = origMatrix;
		this.orderedRows = orderedRows;
		this.orderedCols = orderedCols;
		this.isRowMethodChosen = (rowSimilarity != 0);
		this.isColMethodChosen = (colSimilarity != 0);
		this.hierarchical = hierarchical;
	}
	
	/**
	 * Sets up a buffered writer to generate the .cdt file from the previously
	 * clustered data.
	 * 
	 * @throws IOException
	 */
	public void setupWriter(String fileName, Integer[] spinnerInput) {

		String fileEnd = "";

		if (hierarchical) {
			fileEnd = ".cdt";

		} else {
			String rowC = "";
			String colC = "";

			final int row_clusterN = spinnerInput[0];
			final int col_clusterN = spinnerInput[2];

			if (orderedRows != null && orderedRows.length > 0) {
				rowC = "_G" + row_clusterN;
			}

			if (orderedCols != null && orderedCols.length > 0) {
				colC = "_A" + col_clusterN;
			}

			fileEnd = "_K" + rowC + colC + ".CDT";
		}

		final File file = new File(fileName + fileEnd);
		try {
			file.createNewFile();
			this.bufferedWriter = new ClusterFileWriter(file);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets up instance variables needed for writing.
	 * @param geneHeaderI
	 * @param arrayHeaderI
	 */
	public void prepare(IntHeaderInfo geneHeaderI, 
			IntHeaderInfo arrayHeaderI) {
		
		LogBuffer.println("Preparing.");
		
		this.geneHeaderI = geneHeaderI;

		/* The list containing all the reorganized row-data */
		this.cdtData_doubles = new double[origMatrix.length][];
		this.cdtData_strings = new String[origMatrix.length][];

		// retrieving names and weights of row elements
		// format: [[YAL063C, 1.0], ..., [...]]
		this.rowNames = geneHeaderI.getHeaderArray();

		// retrieving names and weights of column elements
		// format: [[YAL063C, 1.0], ..., [...]]
		this.colNames = arrayHeaderI.getHeaderArray();

		// Lists to be filled with reordered strings
		this.rowNameListOrdered = new String[rowNames.length][];
		this.colNameListOrdered = new String[colNames.length][];
	}

	public void generateCDT() {
		
		orderData();

		LogBuffer.println("Making cdtDataStrings.");
		
		/* Transform cdtDataFile from double lists to string lists */
		for (int i = 0; i < cdtData_doubles.length; i++) {

			final double[] element = cdtData_doubles[i];
			
			final String[] newStringData = new String[element.length];

			for (int j = 0; j < element.length; j++) {

				newStringData[j] = String.valueOf(element[j]);
			}
			
			cdtData_strings[i] = newStringData;
		}

		/* Add some string elements, as well as row/ column names */
		if (hierarchical) {
			LogBuffer.println("Writing hierarchical data.");
			fillHierarchical();

		} else {
			LogBuffer.println("Writing k-means data.");
			fillKMeans();
		}
	}

	/**
	 * This method orders the data if the user decided to use hierarchical
	 * clustering.
	 */
	private void orderData() {
		
		LogBuffer.println("Ordering data.");

		final int[] reorderedRowIndexes = new int[origMatrix.length];
		final int[] reorderedColIndexes = new int[colNames.length];

		cdtData_doubles = new double[reorderedRowIndexes.length]
				[reorderedColIndexes.length];

		if (isRowMethodChosen) {
			final String[] geneNames = new String[rowNames.length];
			if (!hierarchical) {
				for (int i = 0; i < geneNames.length; i++) {

					geneNames[i] = rowNames[i][0];
				}
			}

			int rowIndex = -1;
			for (int i = 0; i < orderedRows.length; i++) {

				final String rowElement = orderedRows[i];

				if (hierarchical) {
					// Regex: Non-digits ('\D') are replaced with "" (no space!)
					// This means: GENE456X -> 456
					final String adjusted = rowElement.replaceAll("[\\D]", "");

					// Adjusted spring is made into integer which can be used
					// as index
					rowIndex = Integer.parseInt(adjusted);

				} else {
					rowIndex = findIndex(geneNames, rowElement);
				}

				reorderedRowIndexes[i] = rowIndex;

				// Order the row names
				rowNameListOrdered[i] = rowNames[rowIndex];
			}
		} else {
			for (int i = 0; i < reorderedRowIndexes.length; i++) {
				reorderedRowIndexes[i] = i;
			}
			rowNameListOrdered = rowNames;
		}

		if (isColMethodChosen) {
			// Make list of gene names to quickly access indexes
			final String[] geneNames = new String[colNames.length];

			if (!hierarchical) {
				for (int i = 0; i < geneNames.length; i++) {

					geneNames[i] = colNames[i][0];
				}
			}

			int colIndex = -1;
			// Make an array of indexes from the ordered column list.
			for (int i = 0; i < reorderedColIndexes.length; i++) {

				final String colElement = orderedCols[i];
				if (hierarchical) {
					final String adjusted = colElement.replaceAll("[\\D]", "");

					// gets index from ordered list, e.g. ARRY45X --> 45;
					colIndex = Integer.parseInt(adjusted);

				} else {
					colIndex = findIndex(geneNames, colElement);
				}

				reorderedColIndexes[i] = colIndex;

				// reordering column names
				colNameListOrdered[i] = colNames[colIndex];
			}
		} else {
			for (int i = 0; i < reorderedColIndexes.length; i++) {
				reorderedColIndexes[i] = i;
			}

			colNameListOrdered = colNames;
		}

		// Order the data.
		int row = -1;
		int col = -1;

		for (int i = 0; i < reorderedRowIndexes.length; i++) {

			row = reorderedRowIndexes[i];

			for (int j = 0; j < reorderedColIndexes.length; j++) {

				col = reorderedColIndexes[j];
				cdtData_doubles[i][j] = origMatrix[row][col];
			}
		}
	}
	
	/**
	 * Finishes up CDTGenerator by closing the buffered writer.
	 * Returns the file path where the cdt file was saved.
	 */
	public String finish() {

		LogBuffer.println("Finishing up.");
		
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
	 * This method fills the String matrix with names for rows/ columns and
	 * other elements.
	 */
	private void fillHierarchical() {

		// The first row
		int rowLength = geneHeaderI.getNumNames()
				+ colNames.length;
		int addIndex = 0;

		if (isRowMethodChosen) {
			rowLength++;
		}

		String[] cdtRow1 = new String[rowLength];

		if (isRowMethodChosen) {
			cdtRow1[addIndex] = "GID";
			addIndex++;
		}

		final String[] rowHeaders = geneHeaderI.getNames();

		for (final String element : rowHeaders) {

			cdtRow1[addIndex] = element;
			addIndex++;
		}

		// Adding column names to first row
		for (int i = 0; i < colNameListOrdered.length; i++) {

			cdtRow1[addIndex] = colNameListOrdered[i][0];
			addIndex++;
		}

		// write every row to buffered writer
		bufferedWriter.writeContent(cdtRow1);
		cdtRow1 = null;

		if (isColMethodChosen) {
			addIndex = 0;
			String[] cdtRow2 = new String[rowLength];

			cdtRow2[addIndex] = "AID";
			addIndex++;

			// Check if rows have been clustered
			if (isRowMethodChosen) {
				for (int i = 0; i < rowHeaders.length; i++) {
					cdtRow2[addIndex] = "";
					addIndex++;
				}
				
			} else {
				for (int i = 0; i < rowHeaders.length - 1; i++) {
					cdtRow2[addIndex] = "";
					addIndex++;
				}
			}

			// Fill second row with array element strings ("ARRY3X")
			for (int i = 0; i < orderedCols.length; i++) {

				cdtRow2[addIndex] = orderedCols[i];
				addIndex++;
			}

			bufferedWriter.writeContent(cdtRow2);
			cdtRow2 = null;
		}
		
		final String[] cdtRow3 = new String[rowLength];

		addIndex = 0;
		cdtRow3[addIndex] = "EWEIGHT";
		addIndex++;

		if (isRowMethodChosen) {
			for (int i = 0; i < rowHeaders.length; i++) {

				cdtRow3[addIndex] = "";
				addIndex++;
			}
		} else {
			for (int i = 0; i < rowHeaders.length - 1; i++) {

				cdtRow3[addIndex] = "";
				addIndex++;
			}
		}

		for (int i = 0; i < colNameListOrdered.length; i++) {

			cdtRow3[addIndex] = colNameListOrdered[i][1];
			addIndex++;
		}

		bufferedWriter.writeContent(cdtRow3);

		// if(!choice.contentEquals("Do Not Cluster")) {
		// Adding the values for ORF, NAME, GWEIGHT
		for (int i = 0; i < cdtData_strings.length; i++) {

			addIndex = 0;
			final String[] row = new String[rowLength];

			// Adding GID names if rows were clustered
			if (isRowMethodChosen) {
				row[addIndex] = orderedRows[i];
				addIndex++;
			}

			// Adding row headers
			for (int j = 0; j < rowNameListOrdered[i].length; j++) {

				row[addIndex] = rowNameListOrdered[i][j];
				addIndex++;
			}

			// Adding data
			for (int j = 0; j < cdtData_strings[i].length; j++) {

				row[addIndex] = cdtData_strings[i][j];
				addIndex++;
			}

			bufferedWriter.writeContent(row);
		}
	}

	/**
	 * This method fills the String matrix with names for rows/ columns and
	 * other elements.
	 */
	private void fillKMeans() {

		final int rowLength = geneHeaderI.getNumNames() + colNames.length;

		final String[] cdtRow1 = new String[rowLength];
		final String[] rowHeaders = geneHeaderI.getNames();

		int addIndex = 0;
		for (final String element : rowHeaders) {

			cdtRow1[addIndex] = element;
			addIndex++;
		}

		// Adding column names to first row
		for (int i = 0; i < colNameListOrdered.length; i++) {

			cdtRow1[addIndex] = colNameListOrdered[i][0];
			addIndex++;
		}

		bufferedWriter.writeContent(cdtRow1);

		// Fill and add second row
		addIndex = 0;
		final String[] cdtRow2 = new String[rowLength];

		cdtRow2[addIndex] = "EWEIGHT";
		addIndex++;

		for (int i = 0; i < rowHeaders.length - 1; i++) {

			cdtRow2[addIndex] = "";
			addIndex++;
		}

		// Fill with weights
		for (int i = 0; i < colNameListOrdered.length; i++) {

			cdtRow2[addIndex] = colNameListOrdered[i][1];
			addIndex++;
		}

		bufferedWriter.writeContent(cdtRow2);

		// Add gene names in ORF and NAME columns (0 & 1) and GWeights (2)
		// buffer is just the amount of rows before the data starts
		for (int i = 0; i < cdtData_strings.length; i++) {

			addIndex = 0;
			final String[] row = new String[rowLength];

			for (int j = 0; j < rowHeaders.length; j++) {

				row[addIndex] = rowNameListOrdered[i][j];
				addIndex++;
			}

			for (int j = 0; j < cdtData_strings[i].length; j++) {

				row[addIndex] = cdtData_strings[i][j];
				addIndex++;
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
