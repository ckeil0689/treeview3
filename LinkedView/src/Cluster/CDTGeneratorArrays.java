package Cluster;

import java.io.File;
import java.io.IOException;

import edu.stanford.genetics.treeview.model.TVModel;

/**
 * This class is used to generate the .CDT tab delimited file which Java
 * TreeView will use for visualization. It takes in the previously calculated
 * data and forms String lists to make them writable.
 * 
 * @author CKeil
 * 
 */
public class CDTGeneratorArrays {

	// Instance variables
	private final TVModel model;
	private final ClusterView clusterView;

	private String filePath;
	private final boolean hierarchical;
	private String choice;
	private String choice2;

	private final double[][] sepList;
	private double[][] cdtDataDoubles;

	private String[][] rowNames;
	private String[][] colNames;
	private String[][] rowNameListOrdered;
	private String[][] colNameListOrdered;
	private String[][] cdtDataStrings;

	private final String[] orderedRows;
	private final String[]orderedCols;
	
	private ClusterFileWriter2 bufferedWriter;

	// Constructor (building the object)
	public CDTGeneratorArrays(final TVModel model, final ClusterView clusterView,
			final double[][] sepList, final String[] orderedRows,
			final String[] orderedCols, final boolean hierarchical) {

		this.model = model;
		this.clusterView = clusterView;
		this.sepList = sepList;
		this.orderedRows = orderedRows;
		this.orderedCols = orderedCols;
		this.hierarchical = hierarchical;
		this.choice = clusterView.getRowSimilarity();
		this.choice2 = clusterView.getColSimilarity();
	}

	public void generateCDT() {
		
		try {
			setupWriter();
			
		} catch (IOException e) {
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
		if (hierarchical) {
			orderHierarchical();

		} else {
			orderKMeans();
		}

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
	 * Sets up a buffered writer to generate the .cdt file from the 
	 * previously clustered data.
	 * @throws IOException 
	 */
	public void setupWriter() throws IOException {

		String fileEnd = "";

		if (hierarchical) {
			fileEnd = ".cdt";

		} else {
			String rowC = "";
			String colC = "";
			
			Integer[] spinnerInput = clusterView.getSpinnerValues();
			
			int row_clusterN = spinnerInput[0];
			int col_clusterN = spinnerInput[2];

			if (orderedRows != null && orderedRows.length > 0) {
				rowC = "_G" + row_clusterN;
			} 
			
			if (orderedCols != null && orderedCols.length > 0) {
				colC = "_A" + col_clusterN;
			}

			fileEnd = "_K" + rowC + colC + ".CDT";
		}
				
		File file = new File(model.getSource().substring(0,
				model.getSource().length() - 4) + fileEnd);
		
		file.createNewFile();
		
		// save file as excel tab-delimited file
		bufferedWriter = new ClusterFileWriter2(file);
		
		filePath = bufferedWriter.getFilePath();
	}

	/**
	 * This method orders the data if the user decided to use hierarchical
	 * clustering.
	 */
	public void orderHierarchical() {
		
		int[] reorderedRowIndexes = new int[sepList.length];
		int[] reorderedColIndexes = new int[colNames.length];
		
		cdtDataDoubles = new double[reorderedRowIndexes.length]
				[reorderedColIndexes.length];
		
		if (!choice.contentEquals("Do Not Cluster")) {
			for (int i = 0; i < orderedRows.length; i++) {

				final String rowElement = orderedRows[i];

				// Regex: Non-digits ('\D') are replaced with "" (no space!)
				// This means: GENE456X -> 456
				final String adjusted = rowElement.replaceAll("[\\D]", "");

				// Adjusted spring is made into integer which can be used
				// as index
				final int index = Integer.parseInt(adjusted);
				
				reorderedRowIndexes[i] = index;

				// Order the row names
				rowNameListOrdered[i] = rowNames[index];
			}
		} else {
			for(int i = 0; i < reorderedRowIndexes.length; i++) {
				reorderedRowIndexes[i] = i;
			}
			
			rowNameListOrdered = rowNames;
		}
		
		if (!choice2.contentEquals("Do Not Cluster")) {
			int colIndex = -1;
			// Make an array of indexes from the ordered column list.
			for(int i = 0; i < reorderedColIndexes.length; i++) {
				
				final String colElement = orderedCols[i];
				final String adjusted = colElement.replaceAll("[\\D]", "");

				// gets index from ordered list, e.g. ARRY45X --> 45;
				colIndex = Integer.parseInt(adjusted);
				
				reorderedColIndexes[i] = colIndex;
				
				// reordering column names
				colNameListOrdered[i] = colNames[colIndex];
			}
		} else {
			for(int i = 0; i < reorderedColIndexes.length; i++) {
				reorderedColIndexes[i] = i;
			}
			
			colNameListOrdered = colNames;
		}
		
		// Order the data.
		int row = -1;
		int col = -1;
		
		for(int i = 0; i < reorderedRowIndexes.length; i ++) {
			
			row = reorderedRowIndexes[i];
			
			for(int j = 0; j < reorderedColIndexes.length; j ++) {
				
				col = reorderedColIndexes[j];
				cdtDataDoubles[i][j] = sepList[row][col];
			}
		}
		System.out.println("Check.");
	}

	/**
	 * This method orders the data table if the user decided to use K-Means
	 * clustering.
	 */
	public void orderKMeans() {
		
		if (!choice.contentEquals("Do Not Cluster")) {
			// Make list of gene names to quickly access indexes
			final String[] geneNames = new String[rowNames.length];

			for (int i = 0; i < geneNames.length; i++) {

				geneNames[i] = rowNames[i][0];
			}

			for (int i = 0; i < orderedRows.length; i++) {

				final String rowElement = orderedRows[i];

				// Index of the gene in original data table
				final int index = findIndex(geneNames, rowElement);;

				final double[] rowData = sepList[index];

				cdtDataDoubles[i] = rowData;

				rowNameListOrdered[i] = rowNames[index];
			}
		} else {
			rowNameListOrdered = rowNames;
		}
		
		// order column data and names
		if (!choice2.contentEquals("Do Not Cluster")) {
			// Make list of gene names to quickly access indexes
			final String[] geneNames = new String[colNames.length];

			for (int i = 0; i < geneNames.length; i++) {

				geneNames[i] = colNames[i][0];
			}

			if (cdtDataDoubles[0] == null) {
				cdtDataDoubles = sepList;
			}

			//
			for (int i = 0; i < orderedCols.length; i++) {

				final String colElement = orderedCols[i];

				final int index = findIndex(geneNames, colElement);

				// going through every row
				for (int j = 0; j < cdtDataDoubles.length; j++) {

					// swapping position in original column arrangement
					// according to new ordered list
					double first = cdtDataDoubles[j][i];
					double second = cdtDataDoubles[j][index];
				
					cdtDataDoubles[j][i] = second;
					cdtDataDoubles[j][index] = first;
				}

				// reordering names
				colNameListOrdered[i] = colNames[index];
			}
		} else {
			colNameListOrdered = colNames;
		}
	}
	
	/**
	 * Finds the index of an element in a String array.
	 * @param array
	 * @param element
	 * @return
	 */
	public int findIndex(String[] array, String element) {
		
		int index = -1;
		for(int i = 0; i < array.length; i++) {
			
			if(array[i].equalsIgnoreCase(element)) {
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
		
		if (!choice.contentEquals("Do Not Cluster")) {
			rowLength++;
		}
		
		String[] cdtRow1 = new String[rowLength];
		
		if (!choice.contentEquals("Do Not Cluster")) {
			cdtRow1[addIndex] = "GID";
			addIndex++;
		}

		String[] rowHeaders = model.getGeneHeaderInfo().getNames();
		
		for(String element : rowHeaders) {
			
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

		if (!choice2.contentEquals("Do Not Cluster")) {
			
			addIndex = 0;
			String[] cdtRow2 = new String[rowLength];

			cdtRow2[addIndex] = "AID";
			addIndex++;
			
			// Check if rows have been clustered
			if(!choice.contentEquals("Do Not Cluster")) {
				for(int i = 0; i < rowHeaders.length; i++) {
					cdtRow2[addIndex] = "";
					addIndex++;
				}
			} else {
				for(int i = 0; i < rowHeaders.length - 1; i++) {
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
		
		if(!choice.contentEquals("Do Not Cluster")) {
			for(int i = 0; i < rowHeaders.length; i++) {
				
				cdtRow3[addIndex] = "";
				addIndex++;
			}
		} else {
			for(int i = 0; i < rowHeaders.length - 1; i++) {
				
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
			String[] row = new String[rowLength];
			
			// Adding GID names if rows were clustered
			if (!choice.contentEquals("Do Not Cluster")) {
				row[addIndex] = orderedRows[i];
				addIndex++;
			}
			
			// Adding row headers
			for(int j = 0; j < rowNameListOrdered[i].length; j++) {
				
				row[addIndex] = rowNameListOrdered[i][j];
				addIndex++;
			}
			
			// Adding data
			for(int j = 0; j < cdtDataStrings[i].length; j++) {
				
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

		int rowLength = model.getGeneHeaderInfo().getNumNames() 
				+ colNames.length;
		
		final String[] cdtRow1 = new String[rowLength];
		String[] rowHeaders = model.getGeneHeaderInfo().getNames();
		
		int addIndex = 0;
		for(String element : rowHeaders) {
			
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
		
		for(int i = 0; i < rowHeaders.length - 1; i++) {
			
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
		int dataLineN = colNames.length;
		for (int i = 0; i < dataLineN; i++) {

			addIndex = 0;
			String[] row = new String[rowLength];
			
			for(int j = 0; j < rowHeaders.length; j++) {
				
				row[addIndex] = rowNameListOrdered[i][j];
				addIndex++;
			}
			
			for(int j = 0; j < cdtDataStrings[i].length; j++) {
				
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
