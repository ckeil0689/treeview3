package Cluster;

import java.io.File;
import java.io.IOException;

import Utilities.StringRes;
import edu.stanford.genetics.treeview.DataModel;

/**
 * This class is used to generate the .CDT tab delimited file which Java
 * TreeView will use for visualization. It takes in the previously calculated
 * data and forms String lists to make them writable.
 * 
 * @author CKeil
 * 
 */
public class CDTGenerator {

	// Instance variables
	private final DataModel model;
	private final ClusterView clusterView;

	private String filePath;
	private final boolean hierarchical;
	private final boolean isRowMethodChosen;
	private final boolean isColMethodChosen;

	private final double[][] sepList;
	private double[][] cdtDataDoubles;

	private String[][] rowNames;
	private String[][] colNames;
	private String[][] rowNameListOrdered;
	private String[][] colNameListOrdered;
	private String[][] cdtDataStrings;

	private final String[] orderedRows;
	private final String[] orderedCols;

	private ClusterFileWriter bufferedWriter;

	// Constructor (building the object)
	public CDTGenerator(final DataModel model,
			final ClusterView clusterView, final double[][] sepList,
			final String[] orderedRows, final String[] orderedCols,
			final boolean hierarchical) {

		this.model = model;
		this.clusterView = clusterView;
		this.sepList = sepList;
		this.orderedRows = orderedRows;
		this.orderedCols = orderedCols;
		this.hierarchical = hierarchical;
		this.isRowMethodChosen = clusterView.getRowSimilarity()
				.contentEquals(StringRes.cluster_DoNot);
		this.isColMethodChosen = clusterView.getColSimilarity()
				.contentEquals(StringRes.cluster_DoNot);
	}

	public void generateCDT() {

		try {
			setupWriter();

		} catch (final IOException e) {
			e.printStackTrace();
		}

		// the list containing all the reorganized row-data
		cdtDataDoubles = new double[sepList.length][];

		// retrieving names and weights of row elements
		// format: [[YAL063C, 1.0], ..., [...]]
		rowNames = model.getGeneHeaderInfo().getHeaderArray();

		// retrieving names and weights of column elements
		// format: [[YAL063C, 1.0], ..., [...]]
		colNames = model.getArrayHeaderInfo().getHeaderArray();

		// Lists to be filled with reordered strings
		rowNameListOrdered = new String[rowNames.length][];
		colNameListOrdered = new String[colNames.length][];

		// Order Rows and/ or Columns
		orderData();

		// transform cdtDataFile from double lists to string lists
		cdtDataStrings = new String[cdtDataDoubles.length][];

		for (int i = 0; i < cdtDataDoubles.length; i++) {

			final double[] element = cdtDataDoubles[i];
			final String[] newStringData = new String[element.length];

			for (int j = 0; j < element.length; j++) {

				newStringData[j] = String.valueOf(element[j]);
			}

			cdtDataStrings[i] = newStringData;
		}

		// Add some string elements, as well as row/ column names
		if (hierarchical) {
			fillHierarchical();

		} else {
			fillKMeans();
		}

		bufferedWriter.closeWriter();
	}

	/**
	 * Sets up a buffered writer to generate the .cdt file from the previously
	 * clustered data.
	 * 
	 * @throws IOException
	 */
	public void setupWriter() throws IOException {

		String fileEnd = "";

		if (hierarchical) {
			fileEnd = ".cdt";

		} else {
			String rowC = "";
			String colC = "";

			final Integer[] spinnerInput = clusterView.getSpinnerValues();

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

		final File file = new File(model.getSource().substring(0,
				model.getSource().length() - 4)
				+ fileEnd);

		file.createNewFile();

		// save file as excel tab-delimited file
		bufferedWriter = new ClusterFileWriter(file);

		filePath = bufferedWriter.getFilePath();
	}

	/**
	 * This method orders the data if the user decided to use hierarchical
	 * clustering.
	 */
	public void orderData() {

		final int[] reorderedRowIndexes = new int[sepList.length];
		final int[] reorderedColIndexes = new int[colNames.length];

		cdtDataDoubles = new double[reorderedRowIndexes.length]
				[reorderedColIndexes.length];

		if (!isRowMethodChosen) {
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

		if (!isColMethodChosen) {
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
				cdtDataDoubles[i][j] = sepList[row][col];
			}
		}
	}

	/**
	 * Finds the index of an element in a String array.
	 * 
	 * @param array
	 * @param element
	 * @return
	 */
	public int findIndex(final String[] array, final String element) {

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
	public void fillHierarchical() {

		// The first row
		int rowLength = model.getGeneHeaderInfo().getNumNames()
				+ colNames.length;
		int addIndex = 0;

		if (!isRowMethodChosen) {
			rowLength++;
		}

		String[] cdtRow1 = new String[rowLength];

		if (!isRowMethodChosen) {
			cdtRow1[addIndex] = "GID";
			addIndex++;
		}

		final String[] rowHeaders = model.getGeneHeaderInfo().getNames();

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

		if (!isColMethodChosen) {
			addIndex = 0;
			String[] cdtRow2 = new String[rowLength];

			cdtRow2[addIndex] = "AID";
			addIndex++;

			// Check if rows have been clustered
			if (!isRowMethodChosen) {
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

		if (!isRowMethodChosen) {
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
		for (int i = 0; i < cdtDataStrings.length; i++) {

			addIndex = 0;
			final String[] row = new String[rowLength];

			// Adding GID names if rows were clustered
			if (!isRowMethodChosen) {
				row[addIndex] = orderedRows[i];
				addIndex++;
			}

			// Adding row headers
			for (int j = 0; j < rowNameListOrdered[i].length; j++) {

				row[addIndex] = rowNameListOrdered[i][j];
				addIndex++;
			}

			// Adding data
			for (int j = 0; j < cdtDataStrings[i].length; j++) {

				row[addIndex] = cdtDataStrings[i][j];
				addIndex++;
			}

			bufferedWriter.writeContent(row);
		}
	}

	/**
	 * This method fills the String matrix with names for rows/ columns and
	 * other elements.
	 */
	public void fillKMeans() {

		final int rowLength = model.getGeneHeaderInfo().getNumNames()
				+ colNames.length;

		final String[] cdtRow1 = new String[rowLength];
		final String[] rowHeaders = model.getGeneHeaderInfo().getNames();

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
		for (int i = 0; i < cdtDataStrings.length; i++) {

			addIndex = 0;
			final String[] row = new String[rowLength];

			for (int j = 0; j < rowHeaders.length; j++) {

				row[addIndex] = rowNameListOrdered[i][j];
				addIndex++;
			}

			for (int j = 0; j < cdtDataStrings[i].length; j++) {

				row[addIndex] = cdtDataStrings[i][j];
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
