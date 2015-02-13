package edu.stanford.genetics.treeview.model;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import Cluster.ClusterFileWriter;
import Views.WelcomeView;
import edu.stanford.genetics.treeview.TreeViewFrame;

/**
 * This class transforms any loaded file of non-cdt format to a cdt file in
 * order to use existing infrastructure in the JTV source code and expanding the
 * compatibility of loaded files to other delimited file types without rewriting
 * and adjusting a lot of the source code. The cdt file type used by Java
 * TreeView assumes a certain format, so any loaded file will be scanned for the
 * required data and a formatted cdt file is generated.
 *
 * @author CKeil
 *
 */
public class CDTCreator {

	private BufferedReader reader = null;
	private final File file;
	private final File customFile;
	private String[][] dataSet;

	// booleans
	boolean hasGID = false;
	boolean hasAID = false;
	boolean hasORF = false;
	boolean hasName = false;
	boolean hasGWeight = false;
	boolean hasEWeight = false;

	// label positions
	private final int[] gidInd = new int[2];
	private final int[] aidInd = new int[2];
	private final int[] orfInd = new int[2];
	private final int[] gweightInd = new int[2];
	private final int[] eweightInd = new int[2];
	private final int[] dataStart = new int[2];
	private final int[] nameInd = new int[2];

	private final boolean gid = false;
	private int rowSize;

	private String fileType = "";
	private String filePath = "";

	// Buffered writer
	private ClusterFileWriter bw;

	/**
	 * Constructor
	 *
	 * @param file
	 */
	public CDTCreator(final File file, final String fileType,
			final TreeViewFrame tvFrame) {

		this(file, null, fileType, tvFrame);
	}

	/**
	 * Constructor for custom ORF/ NAME lists
	 *
	 * @param file
	 */
	public CDTCreator(final File file, final File file2, final String fileType,
			final TreeViewFrame tvFrame) {

		this.file = file;
		this.customFile = file2;
		this.fileType = fileType;
	}

	public void createFile() throws IOException {

		try {
			// Loading screen
			WelcomeView.setLoadText("Transforming file to CDT format...");
			WelcomeView.resetLoadBar();

			// Count file lines for loadBar
			final int pBarMax = count(file.getAbsolutePath());
			WelcomeView.setLoadBarMax(pBarMax);

			reader = new BufferedReader(new FileReader(file));

			// final String[][] dataExtract = extractData(reader, pBarMax);

			// Arrays to ArrayLists
			WelcomeView.setLoadText("Preparing dataset.");
			// dataSet = transformArray(dataExtract);
			dataSet = extractData(reader, pBarMax);
			rowSize = dataSet[0].length;

			// Find positions of labels in the data set
			WelcomeView.setLoadText("Checking for labels.");
			findLabel(gidInd, "GID");
			findLabel(aidInd, "AID");
			findLabel(orfInd, "ORF");
			findLabel(nameInd, "NAME");
			findLabel(eweightInd, "EWEIGHT");
			findLabel(gweightInd, "GWEIGHT");

			// currently making an assumption rather than actually finding
			// the beginning of data values...
			dataStart[0] = eweightInd[0] + 1;
			dataStart[1] = gweightInd[1] + 1;

			WelcomeView.setLoadText("Setting up file details.");
			setupFile();

			WelcomeView.setLoadText("Writing CDT file...");
			generateCDT();

			bw.closeWriter();

		} catch (final FileNotFoundException e) {
			e.printStackTrace();

		} finally {
			try {
				reader.close();

			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Finds the positions of given labels in the data set and assigns the
	 * values to instance variables for further use.
	 *
	 * @param labelPos
	 * @param dataSet
	 * @param label
	 */
	private void findLabel(final int[] labelPos, final String label) {

		// check for labels in the first 20 rows
		final int threshold = 20;

		// find GWEIGHT and EWEIGHT
		for (int i = 0; i < dataSet.length; i++) {

			final String[] row = dataSet[i];

			int addIndex = 0;
			for (int j = 0; j < row.length; j++) {

				final String element = row[j];

				if (element.equalsIgnoreCase(label)) {
					labelPos[addIndex] = i;
					addIndex++;

					labelPos[addIndex] = j;
					addIndex++;

					if (label.equalsIgnoreCase("GID")) {
						hasGID = true;

					} else if (label.equalsIgnoreCase("AID")) {
						hasAID = true;

					} else if (label.equalsIgnoreCase("ORF")) {
						hasORF = true;

					} else if (label.equalsIgnoreCase("NAME")) {
						hasName = true;

					} else if (label.equalsIgnoreCase("GWEIGHT")) {
						hasGWeight = true;

					} else if (label.equalsIgnoreCase("EWEIGHT")) {
						hasEWeight = true;
					}
					break;
				}
			}

			if (labelPos.length > 0 || i > threshold) {
				if (labelPos.length == 0) {
					labelPos[addIndex] = -1;
					addIndex++;

					labelPos[addIndex] = -1;
					addIndex++;
				}
				break;
			}
		}
	}

	/**
	 * Composes a .cdt file to further be used by TreeView (currently tab-delim
	 * only). TreeView uses the specific .cdt-format with very defined row and
	 * column elements to display certain data in its various views. This
	 * function assembles such a .cdt file from the scavenged data of the loaded
	 * input file.
	 *
	 * @throws IOException
	 */
	private void generateCDT() throws IOException {

		final int orfRow = orfInd[0];
		final int orfCol = orfInd[1];
		final int eweightRow = eweightInd[0];
		final int eweightCol = eweightInd[1];
		final int gweightCol = gweightInd[1];
		final int dataCol = dataStart[1];
		int eweightGap = dataStart[1];
		final int dataRow = dataStart[0];
		final int gidCol = 0;
		int line = 0;

		String[] rowElement = new String[rowSize];

		int addIndex = 0;
		if (hasGID) {
			rowElement[addIndex] = "GID";
		}

		if (hasORF == false) {
			eweightGap++;
		}
		rowElement[addIndex] = "ORF";
		addIndex++;

		if (hasName == false) {
			eweightGap++;
		}
		rowElement[addIndex] = "NAME";
		addIndex++;

		if (hasGWeight == false) {
			eweightGap++;
		}
		rowElement[addIndex] = "GWEIGHT";
		addIndex++;

		// add array name row
		final String[] element = dataSet[orfRow];
		for (int i = dataCol; i < element.length; i++) {

			rowElement[addIndex] = element[i];
			addIndex++;
		}

		bw.writeContent(rowElement);
		line++;

		rowElement = new String[rowSize];
		addIndex = 0;

		// add array id row
		if (hasAID) {
			rowElement[addIndex] = "AID";
			addIndex++;

			for (int i = 0; i < dataCol; i++) {

				rowElement[addIndex] = "";
				addIndex++;
			}

			final int aidRow = aidInd[0];

			final String[] aidList = dataSet[aidRow];
			for (int i = dataCol; i < aidList.length; i++) {

				rowElement[addIndex] = aidList[i];
				addIndex++;
			}

			bw.writeContent(rowElement);
			line++;
		}

		// add EWEIGHT row
		rowElement = new String[rowSize];
		addIndex = 0;

		rowElement[addIndex] = "EWEIGHT";
		addIndex++;

		// start at 1 because EWEIGHT takes position 0
		for (int i = eweightCol + 1; i < eweightGap; i++) {

			rowElement[addIndex] = "";
			addIndex++;
		}

		final String[] eweightList = dataSet[eweightRow];
		for (int i = dataCol; i < eweightList.length; i++) {

			rowElement[addIndex] = eweightList[i];
			addIndex++;
		}

		bw.writeContent(rowElement);
		line++;

		// continue with each data row, just each element + data sublist values
		// for the size of the dataSet - dataCol(amount of rows already filled)
		final int dataLineN = dataSet.length;
		for (int i = dataRow; i < dataLineN; i++) {

			final String[] fullRow = dataSet[i];
			rowElement = new String[rowSize];
			addIndex = 0;

			if (gid) {
				rowElement[addIndex] = fullRow[gidCol];
				addIndex++;
			}

			if (orfInd != null) {
				rowElement[addIndex] = fullRow[orfCol];
				addIndex++;

			} else {
				rowElement[addIndex] = "ORF N/A";
				addIndex++;
			}

			if (nameInd[0] == -1) {
				rowElement[addIndex] = fullRow[nameInd[1]];
				addIndex++;

			} else if (orfInd != null) {
				rowElement[addIndex] = fullRow[orfCol];
				addIndex++;

			} else {
				rowElement[addIndex] = "ORF & NAME N/A";
				addIndex++;
			}

			rowElement[addIndex] = fullRow[gweightCol];
			addIndex++;

			for (int j = dataCol; j < rowSize; j++) {
				rowElement[addIndex] = fullRow[i];
				addIndex++;
			}

			// Check whether it's the last line
			bw.writeContent(rowElement);
			line++;
			WelcomeView.updateLoadBar(line);
		}

		bw.closeWriter();
	}

	/**
	 * Saves a cdt-file, built from the loaded file, to the same directory as
	 * the loaded file. This file will then be used in JTV!
	 */
	public void setupFile() {

		final String fileEnd;

		if (customFile == null) {
			fileEnd = "_adjusted.cdt";

		} else {
			fileEnd = "_custom.cdt";
		}

		final String fileName = file.getAbsolutePath().substring(0,
				file.getAbsolutePath().length() - fileType.length());

		bw = new ClusterFileWriter(fileName, fileEnd, -1);
			
		filePath = bw.getFilePath();
	}

	/**
	 * Reading the data separated by tab delimited \t
	 *
	 * @param reader
	 * @param dataExtract
	 * @return
	 */
	public String[][] extractData(final BufferedReader reader,
			final int numLines) {

		final String[][] dataExtract = new String[numLines][];
		String line;

		try {
			int addIndex = 0;
			while ((line = reader.readLine()) != null) {

				final String[] row = line.split("\t");
				dataExtract[addIndex] = row;
				addIndex++;
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return dataExtract;
	}

	// /**
	// * Transforms Array matrix to ArrayList matrix
	// * @param dataExtract
	// * @return
	// */
	// public String[][] transformArray(String[][] dataExtract) {
	//
	// String[][] dataSet = new String[][];
	// int addIndexOuter = 0;
	// for (final String[] row : dataExtract) {
	//
	// final String[] newRow = new String[row.length];
	// int addIndexInner = 0;
	//
	// for (final String element : row) {
	//
	// newRow[addIndexInner] = element;
	// addIndexInner++;
	// }
	//
	// dataSet[addIndexOuter] = newRow;
	// }
	//
	// return dataSet;
	// }

	// /**
	// * Replaces original ORF/ NAME labels with custom labels
	// */
	// public void replaceLabels() {
	//
	// final int dataRow = dataStart.get(0);
	// final int orfCol = orfInd.get(1);
	// final int nameCol = nameInd.get(1);
	// final int customOrfCol = customOrfInd.get(1);
	// final int customNameCol = customNameInd.get(1);
	//
	// for (int i = dataRow; i < dataSet.size(); i++) {
	//
	// final List<String> row = dataSet.get(i);
	//
	// for(List<String> labels : customDataSet) {
	//
	// if(checkSubString(row.get(orfCol),
	// labels.get(customOrfCol))) {
	// row.set(orfCol, labels.get(customOrfCol));
	// }
	//
	// if(checkSubString(row.get(nameCol),
	// labels.get(customNameCol))) {
	// row.set(nameCol, labels.get(customNameCol));
	// }
	// }
	// }
	// }

	/**
	 * Checking a custom string whether it contains
	 *
	 * @param original
	 * @param custom
	 * @return
	 */
	public boolean checkSubString(final String original, final String custom) {

		boolean match = false;
		final int nameLength = 7;

		if (custom != null || original != null) {
			if (custom.contains(original.substring(0, nameLength))) {
				match = true;
			}
		}

		return match;
	}

	/**
	 * Count amount of lines in the file to be loaded so that the progressBar
	 * can get correct values for extractData(). Code from StackOverflow
	 * (https://stackoverflow.com/questions/453018).
	 *
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public int count(final String filename) throws IOException {

		final InputStream is = new BufferedInputStream(new FileInputStream(
				filename));

		try {
			final byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			boolean empty = true;

			while ((readChars = is.read(c)) != -1) {
				empty = false;

				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
			}
			return (count == 0 && !empty) ? 1 : count;

		} finally {
			is.close();
		}
	}

	/**
	 * Getter for file path
	 *
	 * @return
	 */
	public String getFilePath() {

		return filePath;
	}

}
