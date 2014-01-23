package Cluster;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
	private File file;

	private String filePath;
	private int aidBuffer = 2;
	private final boolean hierarchical;
	private String choice;
	private String choice2;

	private String[][] rowNames;
	private String[][] colNames;

	private final List<List<Double>> sepList;
	private List<List<Double>> cdtDataList;

	private List<List<String>> finalcdtTable;
	private List<List<String>> rowNameList;
	private List<List<String>> colNameList;
	private List<List<String>> rowNameListOrdered;
	private List<List<String>> colNameListOrdered;
	private List<List<String>> cdtDataStrings;

	private final List<String> orderedRows;
	private final List<String> orderedCols;

	// Constructor (building the object)
	public CDTGenerator(final DataModel model, final ClusterView clusterView,
			final List<List<Double>> sepList, final List<String> orderedRows,
			final List<String> orderedCols, final boolean hierarchical) {

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

		// The list of String-lists to be generated for file-writing,
		// contains all data
		finalcdtTable = new ArrayList<List<String>>();

		// the list containing all the reorganized row-data
		cdtDataList = new ArrayList<List<Double>>();

		// retrieving names and weights of row elements
		// format: [[YAL063C, 1.0], ..., [...]]
		rowNames = model.getGeneHeaderInfo().getHeaderArray();

		// retrieving names and weights of column elements
		// format: [[YAL063C, 1.0], ..., [...]]
		colNames = model.getArrayHeaderInfo().getHeaderArray();

		// first transform the String[][] to lists
		rowNameList = new ArrayList<List<String>>();
		colNameList = new ArrayList<List<String>>();

		// Lists to be filled with reordered strings
		rowNameListOrdered = new ArrayList<List<String>>();
		colNameListOrdered = new ArrayList<List<String>>();

		if (rowNames.length > 0) {
			for (final String[] element : rowNames) {

				rowNameList.add(Arrays.asList(element));
			}
		}

		if (colNames.length > 0) {
			for (final String[] element : colNames) {

				colNameList.add(Arrays.asList(element));
			}
		}

		// Order Rows and/ or Columns
		if (hierarchical) {
			orderHierarchical();

		} else {
			orderKMeans();
		}

		// transform cdtDataFile from double lists to string lists
		cdtDataStrings = new ArrayList<List<String>>();

		// takes 3k ms...
		for (final List<Double> element : cdtDataList) {

			final List<String> newStringData = new ArrayList<String>();

			for (final Double element2 : element) {

				newStringData.add(element2.toString());
			}

			cdtDataStrings.add(newStringData);
		}

		// fuse them to create the final .CDT-write-ready List<List<String>>
		// This is the final table with just the data
		finalcdtTable.addAll(cdtDataStrings);

		// Add some string elements, as well as row/ column names
		if (hierarchical) {
			fillHierarchical();

		} else {
			fillKMeans();
		}

		// save file as excel tab-delimited file
		final ClusterFileWriter dataFile = new ClusterFileWriter(model);

		String fileEnd = "";

		if (hierarchical) {
			fileEnd = ".cdt";

		} else {

			String rowC = "";
			String colC = "";
			
			Integer[] spinnerInput = clusterView.getSpinnerValues();
			
			int row_clusterN = spinnerInput[0];
			int col_clusterN = spinnerInput[2];

			if (orderedRows.size() > 0 && orderedCols.size() > 0) {
				rowC = "_G" + row_clusterN;
				colC = "_A" + col_clusterN;

			} else if (orderedRows.size() > 0) {
				rowC = "_G" + row_clusterN;

			} else if (orderedCols.size() > 0) {
				colC = "_A" + col_clusterN;
			}

			fileEnd = "_K" + rowC + colC + ".CDT";
		}
		// change boolean type to String file ending?
		dataFile.writeFile(finalcdtTable, fileEnd);

		file = dataFile.getFile();
		filePath = dataFile.getFilePath();
	}

	/**
	 * This method orders the data if the user decided to use hierarchical
	 * clustering.
	 */
	public void orderHierarchical() {
		
		if (!choice.contentEquals("Do Not Cluster")) {
			for (int i = 0; i < orderedRows.size(); i++) {

				final String rowElement = orderedRows.get(i);

				// Regex: Non-digits ('\D') are replaced with "" (no space!)
				// This means: GENE456X -> 456
				final String adjusted = rowElement.replaceAll("[\\D]", "");

				// Adjusted spring is made into integer which can be used
				// as index
				final int index = Integer.parseInt(adjusted);

				final List<Double> rowData = sepList.get(index);

				cdtDataList.add(rowData);

				// Order the row names
				rowNameListOrdered.add(rowNameList.get(index));
			}
		} else {
			rowNameListOrdered.addAll(rowNameList);
		}
		
		// order column data and names
		if (!choice2.contentEquals("Do Not Cluster")) {

			if (cdtDataList.size() == 0) {
				cdtDataList.addAll(sepList);
			}

			for (int i = 0; i < orderedCols.size(); i++) {

				final String colElement = orderedCols.get(i);
				final String adjusted = colElement.replaceAll("[\\D]", "");

				// gets index from ordered list, e.g. ARRY45X --> 45;
				final int index = Integer.parseInt(adjusted);

				// going through every row
				for (int j = 0; j < cdtDataList.size(); j++) {

					// swapping position in original column arrangement
					// according to new ordered list if Element 1 in orderedCols
					// is ARRY45X, then element 1 and element 45 will
					// be swapped in every row
					Collections.swap(cdtDataList.get(j), i, index);
				}

				// reordering column names
				colNameListOrdered.add(colNameList.get(index));
				;
			}
		} else {
			colNameListOrdered.addAll(colNameList);
		}
	}

	/**
	 * This method orders the data table if the user decided to use K-Means
	 * clustering.
	 */
	public void orderKMeans() {
		
		if (!choice.contentEquals("Do Not Cluster")) {
			// Make list of gene names to quickly access indexes
			final List<String> geneNames = new ArrayList<String>();

			for (final List<String> geneHeaders : rowNameList) {

				geneNames.add(geneHeaders.get(0));
			}

			for (int i = 0; i < orderedRows.size(); i++) {

				final String rowElement = orderedRows.get(i);

				// Index of the gene in original data table
				final int index = geneNames.indexOf(rowElement);

				final List<Double> rowData = sepList.get(index);

				cdtDataList.add(rowData);

				rowNameListOrdered.add(rowNameList.get(index));
			}
		} else {
			rowNameListOrdered.addAll(rowNameList);
		}
		
		// order column data and names
		if (!choice2.contentEquals("Do Not Cluster")) {
			// Make list of gene names to quickly access indexes
			final List<String> geneNames = new ArrayList<String>();

			for (final List<String> geneWeight : colNameList) {

				geneNames.add(geneWeight.get(0));
			}

			if (cdtDataList.size() == 0) {
				cdtDataList.addAll(sepList);
			}

			//
			for (int i = 0; i < orderedCols.size(); i++) {

				final String colElement = orderedCols.get(i);

				final int index = geneNames.indexOf(colElement);

				// going through every row
				for (int j = 0; j < cdtDataList.size(); j++) {

					// swapping position in original column arrangement
					// according to new ordered list
					Collections.swap(cdtDataList.get(j), i, index);
				}

				// reordering names
				colNameListOrdered.add(colNameList.get(index));
				;
			}
		} else {
			colNameListOrdered.addAll(colNameList);
		}
	}

	/**
	 * This method fills the String matrix with names for rows/ columns and
	 * other elements.
	 */
	public void fillHierarchical() {

		final List<String> cdtRowElement1 = new ArrayList<String>();
		
		if (!choice.contentEquals("Do Not Cluster")) {
			cdtRowElement1.add("GID");
		}

		cdtRowElement1.add("ORF");
		cdtRowElement1.add("NAME");
		cdtRowElement1.add("GWEIGHT");

		// Adding column names to first row
		for (int i = 0; i < colNameListOrdered.size(); i++) {

			cdtRowElement1.add(colNameListOrdered.get(i).get(0));
		}

		// Add first row at index 0 of the final matrix
		finalcdtTable.add(0, cdtRowElement1);

		if (!choice2.contentEquals("Do Not Cluster")) {
			aidBuffer = 3;

			final List<String> cdtRowElement2 = new ArrayList<String>();

			cdtRowElement2.add("AID");
			cdtRowElement2.add("");
			cdtRowElement2.add("");

			// ????
			if (!choice.contentEquals("Do Not Cluster")) {
				cdtRowElement2.add("");
			}

			// Fill second row with array element strings ("ARRY3X")
			for (int i = 0; i < orderedCols.size(); i++) {

				cdtRowElement2.add(orderedCols.get(i));
			}

			finalcdtTable.add(1, cdtRowElement2);
		}

		final List<String> cdtRowElement3 = new ArrayList<String>();

		cdtRowElement3.add("EWEIGHT");
		cdtRowElement3.add("");
		cdtRowElement3.add("");

		if (!choice.contentEquals("Do Not Cluster")) {
			cdtRowElement3.add("");
		}

		for (int i = 0; i < colNameListOrdered.size(); i++) {

			cdtRowElement3.add(colNameListOrdered.get(i).get(1));
		}

		if (!choice2.contentEquals("Do Not Cluster")) {
			finalcdtTable.add(2, cdtRowElement3);

		} else {
			finalcdtTable.add(1, cdtRowElement3);
		}

		// if(!choice.contentEquals("Do Not Cluster")) {
		// Adding the values for ORF, NAME, GWEIGHT
		for (int i = 0; i < cdtDataStrings.size(); i++) {

			finalcdtTable.get(i + aidBuffer).add(0,
					rowNameListOrdered.get(i).get(0));
			finalcdtTable.get(i + aidBuffer).add(1,
					rowNameListOrdered.get(i).get(1));
			finalcdtTable.get(i + aidBuffer).add(2,
					rowNameListOrdered.get(i).get(2));

			// Adding GID names
			if (!choice.contentEquals("Do Not Cluster")) {
				finalcdtTable.get(i + aidBuffer).add(0, orderedRows.get(i));
			}
		}
	}

	/**
	 * This method fills the String matrix with names for rows/ columns and
	 * other elements.
	 */
	public void fillKMeans() {

		final List<String> cdtRowElement1 = new ArrayList<String>();

		cdtRowElement1.add("ORF");
		cdtRowElement1.add("NAME");
		cdtRowElement1.add("GWEIGHT");

		// Adding column names to first row
		for (int i = 0; i < colNameListOrdered.size(); i++) {

			cdtRowElement1.add(colNameListOrdered.get(i).get(0));
		}

		// Add first row at index 0 of the final matrix
		finalcdtTable.add(0, cdtRowElement1);

		// Fill and add second row
		final List<String> cdtRowElement2 = new ArrayList<String>();

		cdtRowElement2.add("EWEIGHT");
		cdtRowElement2.add("");
		cdtRowElement2.add("");

		// Fill with weights
		for (int i = 0; i < colNameListOrdered.size(); i++) {

			cdtRowElement2.add(colNameListOrdered.get(i).get(1));
		}

		finalcdtTable.add(1, cdtRowElement2);

		// Add gene names in ORF and NAME columns (0 & 1) and GWeights (2)
		// buffer is just the amount of rows before the data starts
		for (int i = 0; i < orderedRows.size(); i++) {

			finalcdtTable.get(i + aidBuffer).add(0,
					rowNameListOrdered.get(i).get(0));
			finalcdtTable.get(i + aidBuffer).add(1,
					rowNameListOrdered.get(i).get(1));
			finalcdtTable.get(i + aidBuffer).add(2,
					rowNameListOrdered.get(i).get(2));
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

	/**
	 * Getter for the current source file object.
	 * 
	 * @return
	 */
	public File getFile() {

		return file;
	}
}
