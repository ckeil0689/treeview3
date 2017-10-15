package edu.stanford.genetics.treeview.model;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.LogBuffer;

/** This class is used to generate the .CDT tab delimited file which Java
 * TreeView will use for visualization. It takes in the previously calculated
 * data and forms String lists to make them writable.
 */
public class ModelFileGenerator {

	// Important cluster strings for the files
	public final static String ROW_AXIS_BASEID = "ROW";
	public final static String COL_AXIS_BASEID = "COL";

	public final static String ROW_ID_LABELTYPE = "GID";
	public final static String COL_ID_LABELTYPE = "AID";

	public final static String ROW_WEIGHT_ID = "GWEIGHT";
	public final static String COL_WEIGHT_ID = "EWEIGHT";

	private double[][] origMatrix;
	private final IntLabelInfo rowLI;
	private final IntLabelInfo colLI;
	private final boolean isRowClustered;
	private final boolean isColClustered;
	private final boolean isHier;

	private ModelFileWriter modelFileWriter;

	/** The task for this class is to take supplied data that is the result of
	 * clustering and format it into a new CDT file that can be read and
	 * interpreted by TreeView.
	 *
	 * @param origMatrix The original data matrix.
	 * @param rowClusterData - contains information necessary to reorder
	 * the rows of the original matrix. 
	 * @param colClusterData - contains information necessary to reorder
	 * the columns of the original matrix. 
	 * @param isHier Indicates type of clustering. If true, clustering is
	 *          hierarchical. If false, clustering is k-means. */
	public ModelFileGenerator(final DataModel model) {

		this.origMatrix = model.getDataMatrix().getExprData();
		this.rowLI = model.getRowLabelInfo();
		this.colLI = model.getColLabelInfo();
		this.isHier = model.isHierarchical();
		this.isRowClustered = model.isRowClustered();
		this.isColClustered = model.isColClustered();
	}

	/** Sets up a buffered writer to generate the .cdt file from the previously
	 * clustered data.
	 *
	 * @throws IOException */
	public void setupWriter(final File file) {

		this.modelFileWriter = new ModelFileWriter(file);
	}

	/** Manages the generation of the main file which describes a data model. */
	public void generateMainFile() {

		if(isRowClustered || isColClustered) {
		  // Add some string elements, as well as row/ column names
		  if(isHier) {
		  	LogBuffer.println("Writing CDT file for hierarchical clustered data.");
			  createHierCDT();
		  }
		  else {
		  	LogBuffer.println("Writing CDT file for k-means clustered data.");
			  createKMeansCDT();
		  }
		} 
		else {
			LogBuffer.println("Writing TXT file for unclustered data.");
			createHierCDT();
		}
		
		modelFileWriter.closeWriter();
	}

	/** Generates a Clustered Data Table (CDT) file formatted for
	 * hierarchical clustering. Each line of the table is first created
	 * as String array and then passed to the BufferedWriter. The process
	 * moves through the labels and data line by line. */
	private void createHierCDT() {

		final String[] rowLabelTypes = rowLI.getLabelTypes();
		final String[] colLabelTypes = colLI.getLabelTypes();

		// Define how long a single matrix row needs to be
		int rowLength = rowLabelTypes.length + colLI.getNumLabels();

		// Row length finalized, calculate column index where data starts
		final int dataStartCol = rowLabelTypes.length;
		// The String array to be written as a single row
		final String[] fullRow = new String[rowLength];
		// Moving through cdtRow array, idxTracker keeps track of the position.
		int idxTracker = 0;
		System.arraycopy(rowLabelTypes, 0, fullRow, idxTracker, rowLabelTypes.length);
		idxTracker += rowLabelTypes.length;

		// Adding column names to first row
		for(final String[] labels : colLI.getLabelArray()) {
			fullRow[idxTracker] = labels[0];
			idxTracker++;
		}

		writeRowAndClear(fullRow);

		// remaining label rows
		for(int i = 1; i < colLabelTypes.length; i++) {

			idxTracker = 0;

			fullRow[idxTracker] = colLabelTypes[i];
			idxTracker++;

			while(idxTracker < dataStartCol) {
				fullRow[idxTracker] = "";
				idxTracker++;
			}

			for(final String[] names : colLI.getLabelArray()) {
				fullRow[idxTracker] = names[i];
				idxTracker++;
			}

			writeRowAndClear(fullRow);
		}

		// Filling the data rows
		for(int i = 0; i < origMatrix.length; i++) {
			// 1) adding row IDs ("ROW130X")...
			idxTracker = 0;
			final String[] row = new String[rowLength];
			String[] labels = rowLI.getLabelArray()[i];

			// 2) adding remaining row labels
			System.arraycopy(labels, 0, row, idxTracker, labels.length);
			idxTracker += labels.length;

			// 3) adding data values
			String[] rowData = getStringArray(origMatrix[i]);
			System.arraycopy(rowData, 0, row, idxTracker, rowData.length);

			writeRowAndClear(row);
		}
	}

	/** Writes the Strings in from an array to a <code>BufferedWriter</code>
	 * and clears the array afterwards.
	 * 
	 * @param row - The array of Strings to be written */
	private void writeRowAndClear(String[] row) {
		modelFileWriter.writeData(row);
		Arrays.fill(row, ""); // clears the final array
	}

	/** Smaller helper routine. Transforms a double array to a String array.
	 * 
	 * @param dArray The double array.
	 * @return An array of Strings which represent the values of the input
	 *         double array. */
	private static String[] getStringArray(double[] dArray) {

		String[] sArray = new String[dArray.length];

		for(int i = 0; i < dArray.length; i++) {
			sArray[i] = String.valueOf(dArray[i]);
		}

		return sArray;
	}

	/** Generates a Clustered Data Table (CDT) file formatted for
	 * k-means clustering. Each line of the table is first created
	 * as String array and then passed to the BufferedWriter. The process
	 * moves through the labels and data line by line. */
	private void createKMeansCDT() {

		final String[] rowLabels = rowLI.getLabelTypes();
		final int rowLength = rowLabels.length + colLI.getNumLabels();
		final String[] cdtRow1 = new String[rowLength];
		int addIndex = 0;

		for(final String element : rowLabels) {
			cdtRow1[addIndex] = element;
			addIndex++;
		}

		/* Adding column names to first row */
		for(final String[] element : colLI.getLabelArray()) {
			cdtRow1[addIndex] = element[0];
			addIndex++;
		}

		modelFileWriter.writeData(cdtRow1);

		/* Fill and add second row */
		addIndex = 0;
		final String[] cdtRow2 = new String[rowLength];

		cdtRow2[addIndex] = COL_WEIGHT_ID;
		addIndex++;

		for(int i = 0; i < rowLabels.length - 1; i++) {
			cdtRow2[addIndex] = "";
			addIndex++;
		}

		/* Fill with weights */
		for(final String[] element : colLI.getLabelArray()) {
			cdtRow2[addIndex] = element[1];
			addIndex++;
		}

		modelFileWriter.writeData(cdtRow2);

		/* 
		 * Add gene names in ORF and NAME columns (0 & 1) and GWeights (2)
		 * buffer is just the amount of rows before the data starts
		 */
		for(int i = 0; i < origMatrix.length; i++) {

			addIndex = 0;
			final String[] row = new String[rowLength];

			for(int j = 0; j < rowLabels.length; j++) {
				row[addIndex] = rowLI.getLabelArray()[i][j];
				addIndex++;
			}

			for(int j = 0; j < origMatrix[i].length; j++) {
				row[addIndex] = String.valueOf(origMatrix[i][j]);
				addIndex++;
			}

			// Check whether it's the last line
			modelFileWriter.writeData(row);
		}
	}
}
