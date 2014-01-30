package edu.stanford.genetics.treeview.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

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
public class CDTCreator3 {

	private BufferedReader reader = null;
	private BufferedReader customReader = null;
	private File file = null;
	private File customFile = null;
	private ArrayList<List<String>> dataSet;
	private ArrayList<List<String>> customDataSet;

	// label positions
	private final List<Integer> gidInd = new ArrayList<Integer>();
	private final List<Integer> aidInd = new ArrayList<Integer>();
	private final List<Integer> orfInd = new ArrayList<Integer>();
	private final List<Integer> gweightInd = new ArrayList<Integer>();
	private final List<Integer> eweightInd = new ArrayList<Integer>();
	private final List<Integer> dataStart = new ArrayList<Integer>();
	private final List<Integer> customDataStart = new ArrayList<Integer>();
	private final List<Integer> nameInd = new ArrayList<Integer>();
	
	//For custom
	private final List<Integer> customOrfInd = new ArrayList<Integer>();
	private final List<Integer> customNameInd = new ArrayList<Integer>();

	private boolean gid = false;

	private String fileType = "";
	private String filePath = "";
	private final String SEPARATOR = "\t";
	private final String END_OF_ROW = "\n";
	
	private BufferedWriter bw;

	/**
	 * Constructor
	 * 
	 * @param file
	 */
	public CDTCreator3(final File file, final String fileType) {

		this.file = file;
		this.fileType = fileType;
	}
	
	/**
	 * Constructor for custom ORF/ NAME lists
	 * 
	 * @param file
	 */
	public CDTCreator3(final File file, final File file2, 
			final String fileType) {

		this.file = file;
		this.customFile = file2;
		this.fileType = fileType;
	}

	public void createFile() throws IOException {

		try {
			reader = new BufferedReader(new FileReader(file));

			final ArrayList<String[]> dataExtract = extractData(reader);

			//Arrays to ArrayLists
			dataSet = transformArray(dataExtract);

			//Find positions of labels in the data set
			findLabel(gidInd, dataSet, "GID");
			findLabel(aidInd, dataSet, "AID");
			findLabel(orfInd, dataSet, "ORF");
			findLabel(nameInd, dataSet, "NAME");
			findLabel(eweightInd, dataSet, "EWEIGHT");
			findLabel(gweightInd, dataSet, "GWEIGHT");

			// currently making an assumption rather than actually finding
			// the beginning of data values...
			dataStart.add(eweightInd.get(0) + 1);
			dataStart.add(gweightInd.get(1) + 1);
			
			if(customFile != null) {
				customReader = new BufferedReader(new FileReader(customFile));
				
				final ArrayList<String[]> customDataExtract = 
						extractData(customReader);
				
				customDataSet = transformArray(customDataExtract);
				
				findLabel(customOrfInd, customDataSet, "ORF");
				findLabel(customNameInd, customDataSet, "NAME");
				
				if(customOrfInd.get(0) == null) {
					customOrfInd.set(0, 0);
					customOrfInd.set(1, 0);
				}
				
				if(customNameInd.get(0) == null) {
					customNameInd.set(0, 0);
					customNameInd.set(1, 1);
				}
				
				replaceLabels();
			}

			setupCDT();
			generateCDT();
			
			bw.close();
			
			System.out.println("Done." + filePath);

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
	 * @param labelPos
	 * @param dataSet
	 * @param label
	 */
	private void findLabel(final List<Integer> labelPos, 
			ArrayList<List<String>> dataSet, final String label) {

		// check for labels in the first 20 rows
		final int threshold = 20;

		// find GWEIGHT and EWEIGHT
		for (final List<String> row : dataSet) {
			for (final String element : row) {

				if (element.equalsIgnoreCase(label)) {
					labelPos.add(dataSet.indexOf(row));
					labelPos.add(row.indexOf(element));
					break;
				}
			}

			if (labelPos.size() > 0 || dataSet.indexOf(row) > threshold) {
				if (labelPos.size() == 0) {
					labelPos.add(null);
					labelPos.add(null);
				}
				break;
			}
		}
	}

	/**
	 * Composes a .cdt file to further be used by JTV from any tab-delimited
	 * file. JTV uses the specific .cdt-format with very defined row and column
	 * elements to display certain data in its various views. This function
	 * assembles such a .cdt file from the scavenged data of the loaded input
	 * file.
	 * @throws IOException 
	 */
	private void generateCDT() throws IOException {

		final int orfRow = orfInd.get(0);
		final int orfCol = orfInd.get(1);
		final int eweightRow = eweightInd.get(0);
		final int gweightCol = gweightInd.get(1);
		final int dataCol = dataStart.get(1);
		final int dataRow = dataStart.get(0);
		int gidCol = 0;

		if (gidInd.get(0) != null) {
			gidCol = gidInd.get(1);
			gid = true;
		}

//		String finalCDT = "";

		if (gid) {
			bw.write("GID");
			bw.write(SEPARATOR);
		}

		bw.write("ORF");
		bw.write(SEPARATOR);
		bw.write("NAME");
		bw.write(SEPARATOR);
		bw.write("GWEIGHT");
		bw.write(SEPARATOR);

		// add array name row
		final List<String> arrayNames = dataSet.get(orfRow).subList(dataCol,
				dataSet.get(orfRow).size());

		for (int i = 0; i < arrayNames.size(); i++) {

			bw.write(arrayNames.get(i));

			if (i == arrayNames.size() - 1) {
				bw.write(END_OF_ROW);

			} else {
				bw.write(SEPARATOR);
			}
		}

		// add array id row
		if (aidInd.get(1) != null) {
			bw.write("AID");
			bw.write(SEPARATOR);

			for (int i = 0; i < dataCol; i++) {

				bw.write("");
				bw.write(SEPARATOR);
			}

			final int aidRow = aidInd.get(0);

			List<String> aidList = dataSet.get(aidRow).subList(dataCol,
					dataSet.get(aidRow).size());
			
			for(String aidString : aidList) {
				bw.write(aidString);
			}
			
			bw.write(END_OF_ROW);
		}

		// add EWEIGHT row
		bw.write("EWEIGHT");
		bw.write(SEPARATOR);

		// start at 1 because EWEIGHT takes position 0
		for (int i = 0; i < dataCol; i++) {

			bw.write("");
			bw.write(SEPARATOR);
		}

		for (int i = dataCol; i < dataSet.get(0).size(); i++) {

			bw.write(dataSet.get(eweightRow).get(i));

			if (i == dataSet.get(0).size() - 1) {
				bw.write(END_OF_ROW);

			} else {
				bw.write(SEPARATOR);
			}
		}

		// continue with each data row, just each element + data sublist values
		// for the size of the dataSet - 3 (amount of rows already filled)
		for (int i = dataRow; i < dataSet.size(); i++) {

			final List<String> row = dataSet.get(i);

			if (gid) {
				bw.write(row.get(gidCol));
				bw.write(SEPARATOR);

			}

			bw.write(row.get(orfCol));
			bw.write(SEPARATOR);

			if (nameInd.get(0) != null) {
				bw.write(row.get(nameInd.get(1)));
				bw.write(SEPARATOR);

			} else {
				bw.write(row.get(orfCol));
				bw.write(SEPARATOR);
			}

			bw.write(row.get(gweightCol));
			bw.write(SEPARATOR);

			for (int j = dataCol; j < row.size(); j++) {

				bw.write(row.get(j));
				if (j == row.size() - 1) {
					bw.write(END_OF_ROW);

				} else {
					bw.write(SEPARATOR);
				}
			}
		}

		//finalCDT = sb.toString();

		// System.out.println(finalCDT);

		//return finalCDT;
	}

	/**
	 * Saves a cdt-file, built from the loaded file, to the same directory as
	 * the loaded file. This file will then be used in JTV!
	 */
	public void setupCDT() {

		final String fileEnd;
		
		if(customFile == null) {
			fileEnd = "_adjusted.cdt";
			
		} else {
			fileEnd = "_custom.cdt";
		}
		
		final String fileName = file.getAbsolutePath().substring(0,
				file.getAbsolutePath().length() - fileType.length());

		try {
			final File file2 = new File(fileName + fileEnd);

			file2.createNewFile();

			final FileOutputStream fos = 
					new FileOutputStream(file2.getAbsoluteFile(), true);
			final OutputStreamWriter fw = new OutputStreamWriter(fos, "UTF-8");
			bw = new BufferedWriter(fw);

			filePath = file2.getAbsolutePath();
			
//			System.out.println("Done." + file2.getAbsolutePath());

		} catch (final IOException e) {

		}
	}
	
	/**
	 * Reading the data separated by tab delimited \t
	 * @param reader
	 * @param dataExtract
	 * @return
	 */
	public ArrayList<String[]> extractData(BufferedReader reader) {
		
		ArrayList<String[]> dataExtract = new ArrayList<String[]>();
		String line;
		
		try {
			while ((line = reader.readLine()) != null) {
	
				final String[] row = line.split("\t");
				dataExtract.add(row);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return dataExtract;
	}
	
	/**
	 * Transforms Array matrix to ArrayList matrix
	 * @param dataExtract
	 * @return
	 */
	public ArrayList<List<String>> transformArray(
			ArrayList<String[]> dataExtract) {
		
		ArrayList<List<String>> dataSet = new ArrayList<List<String>>();
		
		for (final String[] row : dataExtract) {

			final ArrayList<String> newRow = new ArrayList<String>();

			for (final String element : row) {

				newRow.add(element);
			}

			dataSet.add(newRow);
		}
		
		return dataSet;
	}
	
	/**
	 * Replaces original ORF/ NAME labels with custom labels
	 */
	public void replaceLabels() {
		
		final int dataRow = dataStart.get(0);
		final int orfCol = orfInd.get(1);
		final int nameCol = nameInd.get(1);
		final int customOrfCol = customOrfInd.get(1);
		final int customNameCol = customNameInd.get(1);
		
		for (int i = dataRow; i < dataSet.size(); i++) {

			final List<String> row = dataSet.get(i);
			
			for(List<String> labels : customDataSet) {
				
				if(checkSubString(row.get(orfCol), 
						labels.get(customOrfCol))) {
					row.set(orfCol, labels.get(customOrfCol));
				}
				
				if(checkSubString(row.get(nameCol), 
						labels.get(customNameCol))) {
					row.set(nameCol, labels.get(customNameCol));
				}
			}
		}
	}
	
	/**
	 * Checking a custom string whether it contains 
	 * @param original
	 * @param custom
	 * @return
	 */
	public boolean checkSubString(String original, String custom) {
		
		boolean match = false;
		int nameLength = 7;
		
		if(custom != null || original != null) {
			if(custom.contains(original.substring(0, nameLength))){
				match = true;
			}
		}
		
		return match;
	}

	/**
	 * Getter for file path
	 * @return
	 */
	public String getFilePath() {

		return filePath;
	}

}
