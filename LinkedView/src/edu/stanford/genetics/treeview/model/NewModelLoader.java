package edu.stanford.genetics.treeview.model;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;

import Views.WelcomeView;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeViewFrame;

public class NewModelLoader {

	protected TVModel targetModel;
	private final FileSet fileSet;
	protected TreeViewFrame tvFrame;
	protected WelcomeView loadProgView;

	// Instance variables for the actual data to be loaded.
//	protected String[][] stringLabels;
	private double[][] doubleData;

	private int lineNum;
	private int dataStartRow;
	private int dataStartCol;

	boolean hasData = false;
	private boolean hasGID = false;
	private boolean hasAID = false;
	private boolean hasEWeight = false;
	private boolean hasGWeight = false;

	public NewModelLoader(final TVModel model) {

		this.targetModel = model;
		this.tvFrame = model.getFrame();
		this.fileSet = model.getFileSet();
		this.loadProgView = tvFrame.getWelcomeView();
	}

	public TVModel load() throws OutOfMemoryError {
		
		String[][] stringLabels = null;
		try { 
			// Read data from specified file location
			loadProgView.resetLoadBar();
			loadProgView.setLoadLabel("Getting file ready to load...");
			lineNum = count(new File(fileSet.getCdt()));
			loadProgView.setLoadBarMax(lineNum);
			
			BufferedReader br = new BufferedReader(new FileReader(
				new File(fileSet.getCdt())));
		
			try {
				// Get data from file into String and double arrays
				// Put the arrays in ArrayLists for later access.
				loadProgView.setLoadLabel("Extracting data.");
				stringLabels = extractData(br);
	
				if (!hasData) {
					final String noData = "No data could be identified "
							+ "in the chosen file.";
					tvFrame.setLoadErrorMessage(noData);
					return null;
				}
			} catch (final FileNotFoundException e) {
				final String fnfError = "Chosen file could not be found: "
						+ e.getMessage();
				LogBuffer.println(fnfError);
				tvFrame.setLoadErrorMessage(fnfError);
				e.printStackTrace();
				
			}  finally {
				if(br != null) {
					br.close();
					br = null;
				}
			}
		} catch (final IOException e) {
			final String fnfError = "File could not be loaded: "
					+ e.getMessage();
			LogBuffer.println(fnfError);
			tvFrame.setLoadErrorMessage(fnfError);
			e.printStackTrace();
		}

		// Parse the CDT File
		loadProgView.setLoadLabel("Parsing main file.");
		parseCDT(stringLabels);

		// If present, parse ATR File
		if (hasAID) {
			loadProgView.setLoadLabel("Reading ATR file.");
			parseATR();

		} else {
			LogBuffer.println("No ATR file found for this CDT file.");
			targetModel.aidFound(false);
		}

		// If present, parse GTR File
		if (hasGID) {
			loadProgView.setLoadLabel("Reading GTR file.");
			parseGTR();

		} else {
			LogBuffer.println("No GTR file found for this CDT file.");
			targetModel.gidFound(false);
		}

		// Load Config File
		try {
			loadProgView.setLoadLabel("Getting configurations...");
			final String fileName = targetModel.getFileSet().getRoot();
			final String fileExt = targetModel.getFileSet().getExt();

			final Preferences fileNode = tvFrame.getConfigNode().node("File");

			Preferences documentConfig = null;
			final String[] childrenNodes = fileNode.childrenNames();

			final String default_name = "No file.";

			boolean fileFound = false;
			if (childrenNodes.length > 0) {

				for (int i = 0; i < childrenNodes.length; i++) {

					if (fileNode.node(childrenNodes[i])
							.get("name", default_name)
							.equalsIgnoreCase(fileName)) {
						documentConfig = fileNode.node(childrenNodes[i]);
						fileFound = true;
						break;
					}
				}
			}

			if (!fileFound) {
				documentConfig = fileNode.node("Model "
						+ (childrenNodes.length + 1));
				documentConfig.put("name", fileName);
				documentConfig.put("extension", fileExt);
			}
			
			targetModel.setDocumentConfig(documentConfig);

		} catch (final Exception e) {
			targetModel.setDocumentConfig(null);
			e.printStackTrace();
		}

		// Free the large objects for garbage collection
		stringLabels = null;
		doubleData = null;
				
		loadProgView.setLoadLabel("Done.");
		targetModel.setLoaded(true);

		return targetModel;
	}

	/**
	 * Count amount of lines in the file to be loaded so that the progressBar
	 * can get correct values for extractData(). Code from StackOverflow
	 * (https://stackoverflow.com/questions/1277880).
	 */
	public int count(final File aFile) throws IOException {

		LineNumberReader reader = null;

		try {
			int num = 0;
			reader = new LineNumberReader(new FileReader(aFile));
			while ((reader.readLine()) != null) num++;
			return reader.getLineNumber();

		} catch (final Exception ex) {
			LogBuffer.println("Exception when trying to count lines: "
					+ ex.getMessage());
			return -1;

		} finally {
			if (reader != null) {
				reader.close();
				reader = null;
			}
		}
	}

	/**
	 * Reading the data separated by tab delimited \t The regex check if a
	 * String can be parsed to double is taken from StackOverflow
	 * (https://stackoverflow.com/questions/8564896).
	 * 
	 * @param reader
	 * @param dataExtract
	 */
	public String[][] extractData(final BufferedReader reader) 
			throws IOException {

		final String Digits = "(\\p{Digit}+)";
		final String emptyDigits = "(\\p{Digit}*)";
		final String HexDigits = "(\\p{XDigit}+)";

		// an exponent is 'e' or 'E' followed by an optionally
		// signed decimal integer.
		final String exp = "[eE][+-]?" + emptyDigits;
		final String fpRegex = ("[\\x00-\\x20]*" + // Optional leading
													// "whitespace"
				"[+-]?(" + // Optional sign character
				"NaN|" + // "NaN" string
				"Infinity|" + // "Infinity" string
				// Digits ._opt Digits_opt ExponentPart_opt
				// FloatTypeSuffix_opt
				"(((" + Digits + "(\\.)?(" + Digits + "?)(" + exp + ")?)|"
				+
				// . Digits ExponentPart_opt FloatTypeSuffix_opt
				"(\\.(" + Digits + ")(" + exp + ")?)|"
				+
				// Hexadecimal strings
				"(("
				+
				// 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
				"(0[xX]" + HexDigits
				+ "(\\.)?)|"
				+
				// 0[xX] HexDigits_opt . HexDigits BinaryExponent
				// FloatTypeSuffix_opt
				"(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")"
				+ ")[pP][+-]?" + Digits + "))" + "[fFdD]?))" 
				+ "[\\x00-\\x20]*");
				// Optional trailing "whitespace"

		String[][] stringLabels = new String[lineNum][];

		String line;
		dataStartRow = 0;
		dataStartCol = 0;
		int gWeightCol = 0;
		int rowN = 0;

		while ((line = reader.readLine()) != null) {

			loadProgView.updateLoadBar(rowN);

			// load line as String array
			String[] lineAsStrings = line.split("\t");
			String[] labels;
			double[] dataValues;

			// loop over String array to convert applicable String to double
			// first find data start
			if (!hasData) {
				labels = new String[lineAsStrings.length];
				boolean containsEWeight = false;

				for (int i = 0; i < lineAsStrings.length; i++) {

					final String element = lineAsStrings[i];

					if (element.equalsIgnoreCase("GID")) {
						hasGID = true;
					}

					// Check for GWEIGHT to avoid the weight being
					// recognized as row start of actual data
					if (element.equalsIgnoreCase("GWEIGHT")) {
						gWeightCol = i;
						hasGWeight = true;
					}

					if (element.equalsIgnoreCase("AID")) {
						hasAID = true;
					}

					// Check for EWEIGHT to avoid the weight being
					// recognized as column start of actual data
					if (element.equalsIgnoreCase("EWEIGHT")) {
						containsEWeight = true;
						hasEWeight = true;
					}

					if (Pattern.matches(fpRegex, element)
							&& (!containsEWeight && i != gWeightCol)) {

						dataStartRow = rowN;
						dataStartCol = i;

						hasData = true;
						break;

					} else {
						labels[i] = element;
					}
				}

				// avoid first datarow to be added with null values
				if (hasData) {
					final String[] firstDataRow = new String[dataStartCol];

					for (int i = 0; i < labels.length; i++) {

						if (labels[i] != null) {
							firstDataRow[i] = labels[i];

						} else {
							break;
						}
					}
					stringLabels[rowN] = firstDataRow;

				} else {
					// handle line in which data has been found
					stringLabels[rowN] = labels;
				}

				if (hasData) {
					doubleData = new double[lineNum - dataStartRow][];
					dataValues = new double[lineAsStrings.length - dataStartCol];

					for (int i = 0; i < lineAsStrings.length - dataStartCol; i++) {

						String element = lineAsStrings[i + dataStartCol];

						// Check whether string can be double and is not
						// gweight
						if (Pattern.matches(fpRegex, element)
								&& i != gWeightCol) {

							// For empty exponents apparently
							// caused by Windows .txt
							if (element.endsWith("e") || element.endsWith("E")) {
								element = element + "+00";
							}

							final double val = Double.parseDouble(element);
							dataValues[i] = val;

						} else {
							dataValues[i] = 0;
						}
					}

					doubleData[rowN - dataStartRow] = dataValues;
				}

			} else {
				labels = new String[dataStartCol];
				dataValues = new double[lineAsStrings.length - dataStartCol];
				
				System.arraycopy(lineAsStrings, 0, labels, 0, dataStartCol);
				
				// This ensures that references to immutable string 
				// (whole line from readline()!) do not stay in memory.
				for (int i = 0; i < labels.length; i++){
					labels[i] += "";
				}
				
				stringLabels[rowN] = labels;

				for (int i = 0; i < lineAsStrings.length - dataStartCol; i++) {

					String element = lineAsStrings[i + dataStartCol];

					// handle parseDouble error somehow?
					// using the Pattern.matches method screws up
					// loading time by a factor of 1000....
					if (element.endsWith("e") || element.endsWith("E")) {
						element = element + "+00";
					}

					// Trying to parse the String, if not possible,
					// add 0.
					try {
						final double val = Double.parseDouble(element);
						dataValues[i] = val;

					} catch (final Exception e) {
						LogBuffer.println("Exception when trying to parse "
								+ "a double in extractData() in "
								+ "NewModelLoader: " + e.getMessage());
						final double val = Double.parseDouble("0.00E+00");
						dataValues[i] = val;
					}
				}

				// Issue with length of stringLabels
				stringLabels[rowN] = labels;
				doubleData[rowN - dataStartRow] = dataValues;
			}
			rowN++;
		}
		
		return stringLabels;
	}

	public ArrayList<String[]> extractGTR(final BufferedReader reader) {

		final ArrayList<String[]> gtrData = new ArrayList<String[]>();
		String line;

		try {
			while ((line = reader.readLine()) != null) {

				// load line as String array
				final String[] lineAsStrings = line.split("\t");
				gtrData.add(lineAsStrings);
			}
		} catch (final IOException e) {
			LogBuffer.println("IOException during the "
					+ "extraction of GTR file: " + e.getMessage());
		}

		return gtrData;
	}

	public void parseCDT(String[][] stringLabels) {

		// Tell model whether EWEIGHT and GWEIGHT where found
		targetModel.setEweightFound(hasEWeight);
		targetModel.setGweightFound(hasGWeight);

		final int nExpr = doubleData[0].length;
		final int nExprPrefix = dataStartRow;

		final int nGenePrefix = dataStartCol;
		final int nGene = doubleData.length;

		// Set Array Prefix and Headers
		final String[] arrayPrefix = new String[nExprPrefix];
		final String[][] aHeaders = new String[nExpr][nExprPrefix];

		// fill prefix array
		for (int i = 0; i < nExprPrefix; i++) {

			arrayPrefix[i] = stringLabels[i][0];

			final String[] labelRow = stringLabels[i];

			// fill column header array
			for (int j = 0; j < nExpr; j++) {

				aHeaders[j][i] = labelRow[j + nGenePrefix];
			}
		}

		targetModel.setArrayPrefix(arrayPrefix);
		targetModel.setArrayHeaders(aHeaders);

		final String[] genePrefix = new String[nGenePrefix];
		final String[][] gHeaders = new String[nGene][nGenePrefix];

		System.arraycopy(stringLabels[0], 0, genePrefix, 0, nGenePrefix);

		// Fill Header array
		for (int i = 0; i < nGene; i++) {

			gHeaders[i] = stringLabels[i + nExprPrefix];
		}

		targetModel.setGenePrefix(genePrefix);
		targetModel.setGeneHeaders(gHeaders);
		targetModel.setExprData(doubleData);
		targetModel.getDataMatrix().calculateMinMax();
	}

	public void parseGTR() {

		// First, load the GTR File
		final ArrayList<String[]> gtrData = loadSet(fileSet.getGtr());

		final String[] firstRow = gtrData.get(0);
		if ( // decide if this is not an extended file..
		(firstRow.length == 4)// is the length classic?
				&& !(firstRow[0].equalsIgnoreCase("NODEID"))) {
			// okay, need to assign headers...
			targetModel.setGtrPrefix(new String[] { "NODEID", "LEFT", "RIGHT",
					"CORRELATION" });

			final String[][] gtrHeaders = new String[gtrData.size()][];
			for (int i = 0; i < gtrHeaders.length; i++) {
				gtrHeaders[i] = gtrData.get(i);
			}
			targetModel.setGtrHeaders(gtrHeaders);

		} else {// first row of tempVector is actual header names...
			targetModel.setGtrPrefix(firstRow);

			final String[][] gtrHeaders = new String[gtrData.size() - 1][];
			for (int i = 0; i < gtrHeaders.length; i++) {
				gtrHeaders[i] = gtrData.get(i + 1);
			}
			targetModel.setGtrHeaders(gtrHeaders);
		}

		targetModel.hashGIDs();
		targetModel.hashGTRs();
		targetModel.gidFound(hasGID);
	}

	public void parseATR() {

		// First, load the ATR File
		final ArrayList<String[]> atrData = loadSet(fileSet.getAtr());

		final String[] firstRow = atrData.get(0);
		if ( // decide if this is not an extended file..
		(firstRow.length == 4)// is the length classic?
				&& !(firstRow[0].equalsIgnoreCase("NODEID"))) {

			// okay, need to assign headers...
			targetModel.setAtrPrefix(new String[] { "NODEID", "LEFT", "RIGHT",
					"CORRELATION" });

			final String[][] atrHeaders = new String[atrData.size()][];
			for (int i = 0; i < atrHeaders.length; i++) {
				atrHeaders[i] = atrData.get(i);
			}
			targetModel.setAtrHeaders(atrHeaders);
		} else {// first row of tempVector is actual header names...
			targetModel.setAtrPrefix(firstRow);

			final String[][] atrHeaders = new String[atrData.size() - 1][];
			for (int i = 0; i < atrHeaders.length; i++) {
				atrHeaders[i] = atrData.get(i + 1);
			}
			targetModel.setAtrHeaders(atrHeaders);
		}

		targetModel.hashAIDs();
		targetModel.hashATRs();
		targetModel.aidFound(hasAID);
	}

	public ArrayList<String[]> loadSet(final String loadingSet) {

		ArrayList<String[]> gtrData = new ArrayList<String[]>();

		try {
			LogBuffer.println("Starting GTR load.");
			// Read data from specified file location

//			final FileInputStream fis = new FileInputStream(loadingSet);
//			final DataInputStream in = new DataInputStream(fis);
			final BufferedReader br = new BufferedReader(
					new FileReader(loadingSet));

			// Get data from file into String and double arrays
			// Put the arrays in ArrayLists for later access.
			LogBuffer.println("Starting GTR extract.");
			gtrData = extractGTR(br);
			
			br.close();

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return gtrData;
	}

	class LoadWorker extends SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() throws Exception {

			load();
			return null;
		}

		@Override
		protected void done() {

			try {
				// Wait for worker to finish.
				get();

			} catch (final InterruptedException e) {
				e.printStackTrace();

			} catch (final ExecutionException e) {
				e.printStackTrace();
			}
		}

	}
}
