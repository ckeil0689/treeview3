package Cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

	private String[][] rowNames;
	private String[][] colNames;

	private final List<List<Double>> sepList;
	private List<List<Double>> cdtDataDoubles;

	private List<List<String>> rowNameList;
	private List<List<String>> colNameList;
	private List<List<String>> rowNameListOrdered;
	private List<List<String>> colNameListOrdered;
	private List<List<String>> cdtDataStrings;

	private final List<String> orderedRows;
	private final List<String> orderedCols;
	
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
		cdtDataDoubles = new ArrayList<List<Double>>();

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
		for (final List<Double> element : cdtDataDoubles) {

			final List<String> newStringData = new ArrayList<String>();

			for (final Double element2 : element) {

				newStringData.add(element2.toString());
			}

			cdtDataStrings.add(newStringData);
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

				cdtDataDoubles.add(rowData);

				// Order the row names
				rowNameListOrdered.add(rowNameList.get(index));
			}
		} else {
			rowNameListOrdered.addAll(rowNameList);
		}
		
		// order column data and names
		if (!choice2.contentEquals("Do Not Cluster")) {

			if (cdtDataDoubles.size() == 0) {
				cdtDataDoubles.addAll(sepList);
			}

			for (int i = 0; i < orderedCols.size(); i++) {

				final String colElement = orderedCols.get(i);
				final String adjusted = colElement.replaceAll("[\\D]", "");

				// gets index from ordered list, e.g. ARRY45X --> 45;
				final int index = Integer.parseInt(adjusted);

				// going through every row
				for (int j = 0; j < cdtDataDoubles.size(); j++) {

					// swapping position in original column arrangement
					// according to new ordered list if Element 1 in orderedCols
					// is ARRY45X, then element 1 and element 45 will
					// be swapped in every row
					Collections.swap(cdtDataDoubles.get(j), i, index);
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

				cdtDataDoubles.add(rowData);

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

			if (cdtDataDoubles.size() == 0) {
				cdtDataDoubles.addAll(sepList);
			}

			//
			for (int i = 0; i < orderedCols.size(); i++) {

				final String colElement = orderedCols.get(i);

				final int index = geneNames.indexOf(colElement);

				// going through every row
				for (int j = 0; j < cdtDataDoubles.size(); j++) {

					// swapping position in original column arrangement
					// according to new ordered list
					Collections.swap(cdtDataDoubles.get(j), i, index);
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

		// The first row
		final List<String> cdtRow1 = new ArrayList<String>();
		
		if (!choice.contentEquals("Do Not Cluster")) {
			cdtRow1.add("GID");
		}

		String[] rowHeaders = model.getGeneHeaderInfo().getNames();
		
		for(String element : rowHeaders) {
			
			cdtRow1.add(element);
		}

		// Adding column names to first row
		for (int i = 0; i < colNameListOrdered.size(); i++) {

			cdtRow1.add(colNameListOrdered.get(i).get(0));
		}
		
		// write every row to buffered writer
		bufferedWriter.writeContent(cdtRow1);

		if (!choice2.contentEquals("Do Not Cluster")) {
			final List<String> cdtRow2 = new ArrayList<String>();

			cdtRow2.add("AID");
			
			for(int i = 0; i < rowHeaders.length; i++) {
				cdtRow2.add("");
			}

			// Fill second row with array element strings ("ARRY3X")
			for (int i = 0; i < orderedCols.size(); i++) {

				cdtRow2.add(orderedCols.get(i));
			}

			bufferedWriter.writeContent(cdtRow2);
		}

		final List<String> cdtRow3 = new ArrayList<String>();

		cdtRow3.add("EWEIGHT");
		
		for(int i = 0; i < rowHeaders.length; i++) {
			cdtRow3.add("");
		}

		for (int i = 0; i < colNameListOrdered.size(); i++) {

			cdtRow3.add(colNameListOrdered.get(i).get(1));
		}
		
		bufferedWriter.writeContent(cdtRow3);

		// if(!choice.contentEquals("Do Not Cluster")) {
		// Adding the values for ORF, NAME, GWEIGHT
		for (int i = 0; i < cdtDataStrings.size(); i++) {
			
			List<String> row = cdtDataStrings.get(i);
			
			for(int j = 0; j < rowHeaders.length; j++) {
				
				row.add(j, rowNameListOrdered.get(i).get(j));
			}

			// Adding GID names if rows were clustered
			if (!choice.contentEquals("Do Not Cluster")) {
				row.add(0, orderedRows.get(i));
			}
			
			bufferedWriter.writeContent(row);
		}
	}

	/**
	 * This method fills the String matrix with names for rows/ columns and
	 * other elements.
	 */
	public void fillKMeans() {

		final List<String> cdtRow1 = new ArrayList<String>();

		String[] rowHeaders = model.getGeneHeaderInfo().getNames();
		
		for(String element : rowHeaders) {
			
			cdtRow1.add(element);
		}

		// Adding column names to first row
		for (int i = 0; i < colNameListOrdered.size(); i++) {

			cdtRow1.add(colNameListOrdered.get(i).get(0));
		}
		
		bufferedWriter.writeContent(cdtRow1);

		// Fill and add second row
		final List<String> cdtRow2 = new ArrayList<String>();

		cdtRow2.add("EWEIGHT");
		for(int i = 0; i < rowHeaders.length - 1; i++) {
			
			cdtRow2.add("");
		}

		// Fill with weights
		for (int i = 0; i < colNameListOrdered.size(); i++) {

			cdtRow2.add(colNameListOrdered.get(i).get(1));
		}
		
		bufferedWriter.writeContent(cdtRow2);

		// Add gene names in ORF and NAME columns (0 & 1) and GWeights (2)
		// buffer is just the amount of rows before the data starts
		int dataLineN = orderedRows.size();
		for (int i = 0; i < dataLineN; i++) {

			List<String> row = cdtDataStrings.get(i);
			
			for(int j = 0; j < rowHeaders.length; j++) {
				
				row.add(j, rowNameListOrdered.get(i).get(j));
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
