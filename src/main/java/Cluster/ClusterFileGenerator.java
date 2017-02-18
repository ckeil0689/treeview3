package Cluster;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import edu.stanford.genetics.treeview.model.IntLabelInfo;

/** This class is used to generate the .CDT tab delimited file which Java
 * TreeView will use for visualization. It takes in the previously calculated
 * data and forms String lists to make them writable.
 *
 * @author CKeil */
public class ClusterFileGenerator {

	// Important cluster strings for the files
	public final static String ROW_AXIS_BASEID = "ROW";
	public final static String COL_AXIS_BASEID = "COL";

	public final static String ROW_ID_LABELTYPE = "GID";
	public final static String COL_ID_LABELTYPE = "AID";

	public final static String ROW_WEIGHT_ID = "GWEIGHT";
	public final static String COL_WEIGHT_ID = "EWEIGHT";

	private double[][] origMatrix;
	private final ClusteredAxisData rowClusterData;
	private final ClusteredAxisData colClusterData;
	private final boolean isHier;

	private ClusterFileWriter clusterFileWriter;

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
	public ClusterFileGenerator(final double[][] origMatrix,
															final ClusteredAxisData rowClusterData,
															final ClusteredAxisData colClusterData,
															final boolean isHier) {

		this.origMatrix = origMatrix;
		this.rowClusterData = rowClusterData;
		this.colClusterData = colClusterData;
		this.isHier = isHier;
	}

	/** Sets up a buffered writer to generate the .cdt file from the previously
	 * clustered data.
	 *
	 * @throws IOException */
	public void setupWriter(final File file) {

		this.clusterFileWriter = new ClusterFileWriter(file);
	}

	/** Manages the generation of a clustered data table (CDT) file. */
	public void generateCDT() {

		// Add some string elements, as well as row/ column names
		if(isHier) {
			createHierCDT();

		}
		else {
			createKMeansCDT();
		}
	}

	/** Finishes up <code>ClusterFileGenerator</code> by closing the buffered
	 * writer. Returns the file path where the cdt file was saved. */
	public String finish() {

		final String filePath = clusterFileWriter.getFilePath();

		clusterFileWriter.closeWriter();

		return filePath;
	}

	/** Finds the last index of an element match in a String array.
	 *
	 * @param array
	 * @param element
	 * @return */
	private static int findIndex(final String[] array, final String element) {

		int index = -1;
		for(int i = 0; i < array.length; i++) {

			if(array[i].equalsIgnoreCase(element)) {
				index = i;
			}
		}

		return index;
	}

	/** Generates a Clustered Data Table (CDT) file formatted for
	 * hierarchical clustering. Each line of the table is first created
	 * as String array and then passed to the BufferedWriter. The process
	 * moves through the labels and data line by line. */
	private void createHierCDT() {

		final String[] rowLabelTypes = rowClusterData.getAxisLabelTypes();
		final String[] colLabelTypes = colClusterData.getAxisLabelTypes();

		final boolean foundGIDs = findIndex(rowLabelTypes, ROW_ID_LABELTYPE) != -1;

		final String[] orderedGIDs = rowClusterData.getReorderedIDs();
		final String[] orderedAIDs = colClusterData.getReorderedIDs();

		// Define how long a single matrix row needs to be
		int rowLength = rowLabelTypes.length + colClusterData.getNumLabels();

		if(rowClusterData.isAxisClustered() && !foundGIDs) {
			// a clustered row axis without GID column needs one added, so increment
			rowLength++;
		}

		// Row length finalized, calculate column index where data starts
		final int dataStartCol = rowLength - colClusterData.getNumLabels();
		// The String array to be written as a single row
		final String[] cdtRow = new String[rowLength];
		// Moving through cdtRow array, idxTracker keeps track of the position.
		int idxTracker = 0;

		// >>> Adding data to String arrays representing rows starts here <<<
		if(rowClusterData.isAxisClustered() && !foundGIDs) {
			cdtRow[idxTracker] = ROW_ID_LABELTYPE;
			idxTracker++;
		}

		System.arraycopy(rowLabelTypes, 0, cdtRow, idxTracker, rowLabelTypes.length);
		idxTracker += rowLabelTypes.length;

		// Adding column names to first row
		for(final String[] labels : colClusterData.getAxisLabels()) {
			cdtRow[idxTracker] = labels[0];
			idxTracker++;
		}

		writeRowAndClear(cdtRow);

		// next row
		// if columns were clustered, make AID row
		if(colClusterData.isAxisClustered()) {
			cdtRow[0] = COL_ID_LABELTYPE;

			// Fill with AIDs ("COL3X")
			System.arraycopy(orderedAIDs, 0, cdtRow, dataStartCol, orderedAIDs.length);

			writeRowAndClear(cdtRow);
		}

		// remaining label rows
		for(int i = 1; i < colLabelTypes.length; i++) {

			if(colLabelTypes[i].equals(COL_ID_LABELTYPE)) {
				continue;
			}

			idxTracker = 0;

			cdtRow[idxTracker] = colLabelTypes[i];
			idxTracker++;

			while(idxTracker < dataStartCol) {
				cdtRow[idxTracker] = "";
				idxTracker++;
			}

			for(final String[] names : colClusterData.getAxisLabels()) {
				cdtRow[idxTracker] = names[i];
				idxTracker++;
			}

			writeRowAndClear(cdtRow);
		}

		// Filling the data rows
		for(int i = 0; i < origMatrix.length; i++) {
			// 1) adding row IDs ("ROW130X")...
			idxTracker = 0;
			final String[] row = new String[rowLength];
			String[] labels = rowClusterData.getAxisLabels()[i];

			if(rowClusterData.isAxisClustered() && !foundGIDs) {
				row[idxTracker] = orderedGIDs[i];
				idxTracker++;

				/* 
				 * Ensure the labels are consistent with what was created for 
				 * orderedGIDs. For example, an old file might already contain 
				 * GENE23X etc. but the naming was ditched for ROW23X. If this
				 * isn't corrected, then tree files will not match up with the
				 * cdt.	
				 */
			}
			else if(rowClusterData.isAxisClustered() && foundGIDs) {
				labels[0] = orderedGIDs[i];
			}

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
		clusterFileWriter.writeData(row);
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

		final String[] rowLabels = rowClusterData.getAxisLabelTypes();
		final int rowLength = rowLabels.length + colClusterData.getNumLabels();
		final String[] cdtRow1 = new String[rowLength];
		int addIndex = 0;

		for(final String element : rowLabels) {
			cdtRow1[addIndex] = element;
			addIndex++;
		}

		/* Adding column names to first row */
		for(final String[] element : colClusterData.getAxisLabels()) {
			cdtRow1[addIndex] = element[0];
			addIndex++;
		}

		clusterFileWriter.writeData(cdtRow1);

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
		for(final String[] element : colClusterData.getAxisLabels()) {
			cdtRow2[addIndex] = element[1];
			addIndex++;
		}

		clusterFileWriter.writeData(cdtRow2);

		/* 
		 * Add gene names in ORF and NAME columns (0 & 1) and GWeights (2)
		 * buffer is just the amount of rows before the data starts
		 */
		for(int i = 0; i < origMatrix.length; i++) {

			addIndex = 0;
			final String[] row = new String[rowLength];

			for(int j = 0; j < rowLabels.length; j++) {
				row[addIndex] = rowClusterData.getAxisLabels()[i][j];
				addIndex++;
			}

			for(int j = 0; j < origMatrix[i].length; j++) {
				row[addIndex] = String.valueOf(origMatrix[i][j]);
				addIndex++;
			}

			// Check whether it's the last line
			clusterFileWriter.writeData(row);
		}
	}
}
