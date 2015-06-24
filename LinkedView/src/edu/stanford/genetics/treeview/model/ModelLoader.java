package edu.stanford.genetics.treeview.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;

import Controllers.TVController;
import Utilities.Helper;
import Views.WelcomeView;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.ModelLoader.LoadStatus;

/**
 * The class responsible for loading data into the TVModel.
 *
 * @author CKeil
 *
 */
public class ModelLoader extends SwingWorker<Void, LoadStatus> {

	protected TVController controller;

	/* Reference to the main model which will hold the data */
	protected TVModel targetModel;
	private final FileSet fileSet;

	/* 2D array to hold numerical data */
	private double[][] doubleData;

	/* Holds pattern which recognizes data in a tab-delimited file */
	private String fpRegex;

	/* Total line number of file to be loaded */
	private int row_num;

	private int dataStartRow;
	private int dataStartCol;
	private int lastLabelCol;
	private int possibleLastLabelCol;

	private boolean hasData = false;
	private boolean hasGID = false;
	private boolean hasAID = false;
	private boolean hasEWeight = false;
	private boolean hasGWeight = false;

	public ModelLoader(final DataModel model, final TVController controller) {

		this.controller = controller;
		this.targetModel = (TVModel) model;
		this.fileSet = model.getFileSet();

		setupPattern();
	}

	@Override
	protected void process(final List<LoadStatus> chunks) {

		/* Update the GUI */
		final LoadStatus ls = chunks.get(chunks.size() - 1);
		WelcomeView.setLoadBarMax(ls.getMaxProgress());
		WelcomeView.updateLoadBar(ls.getProgress());
		WelcomeView.setLoadText(ls.getStatus());
	}

	@Override
	protected Void doInBackground() throws Exception {

		/*
		 * Count row numbers to be able to initialize stringLabels[][] and set
		 * progress bar maximum.
		 */
		row_num = Helper.countFileLines(new File(fileSet.getCdt()));

		final LoadStatus ls = new LoadStatus();
		ls.setProgress(0);
		ls.setMaxProgress(row_num);
		ls.setStatus("Preparing...");

		final File file = new File(fileSet.getCdt());
		final BufferedReader reader = new BufferedReader(new FileReader(file));

		/* Find data start */
		final String[][] stringLabels = new String[row_num][];

		String line;
		lastLabelCol         = -1;
		possibleLastLabelCol = -1;
		dataStartRow         = 0;
		dataStartCol         = 0;
		int current_row      = 0;

		/* Read all lines and parse the data */
		while ((line = reader.readLine()) != null) {

			if (hasData) {
				ls.setStatus("Loading...");
				stringLabels[current_row] = fillDoubles(line, current_row);

			} else {
				stringLabels[current_row] = findData(line, current_row);
				//Take care of the first row of data
				if(hasData)
					stringLabels[current_row] = fillDoubles(line, current_row);
			}

			ls.setProgress(current_row++);
			publish(ls);
		}

		reader.close();

		ls.setStatus("Getting ready...");
		publish(ls);

		/* Parse tree and config files */
		assignDataToModel(stringLabels);

		return null;
	}

	@Override
	protected void done() {

		doubleData = null;

		/* Update GUI, set new DendroView */
		controller.finishLoading();
	}

	/* ---- Loading methods -------- */
	// TODO turn this whole thing into a STATIC method which can ATTEMPT to 
	// find data and then display it in the preview. Let users adjust first.
	
	private String[] findData(final String line, final int current_row) {

		/* Flag for the current_row to avoid adding weights/labels as data. */
		boolean is_label_row    = false;

		// load line as String array
		final String[] lineAsStrings = line.split("\\t", -1);
		String[] labels;

		// loop over String array to convert applicable String to double
		// first find data start
		labels = new String[lineAsStrings.length];

		for (int i = 0; i < lineAsStrings.length; i++) {

			final String element = lineAsStrings[i];

			/* Check string if it is the label for GIDs or AIDs. */
			if (element.equalsIgnoreCase("GID")) {
				if(i > lastLabelCol)
					lastLabelCol = i;
				if(i > possibleLastLabelCol)
					possibleLastLabelCol = i;
				hasGID = true;
				is_label_row = true;
			}
			else if (element.equalsIgnoreCase("AID")) {
				if(i > lastLabelCol)
					lastLabelCol = i;
				if(i > possibleLastLabelCol)
					possibleLastLabelCol = i;
				hasAID = true;
				is_label_row = true;
			}
			else if (element.equalsIgnoreCase("GWEIGHT")) {
				if(i > lastLabelCol)
					lastLabelCol = i;
				if(i > possibleLastLabelCol)
					possibleLastLabelCol = i;
				hasGWeight = true;
				is_label_row = true;
			}
			else if (element.equalsIgnoreCase("EWEIGHT")) {
				if(i > lastLabelCol)
					lastLabelCol = i;
				if(i > possibleLastLabelCol)
					possibleLastLabelCol = i;
				hasEWeight = true;
				is_label_row = true;
			}
			else if (element.equalsIgnoreCase("ORF")   ||
					 element.equalsIgnoreCase("GNAME") ||
					 element.equalsIgnoreCase("ID")    ||
					 element.equalsIgnoreCase("NAME")  ||
					 element.equalsIgnoreCase("UID")) {
				if(i > lastLabelCol)
					lastLabelCol = i;
				if(i > possibleLastLabelCol)
					possibleLastLabelCol = i;
				is_label_row = true;
			}
			//Else if the value is all-caps and this has either already been
			//identified as a label row or data has not yet been identified and
			//all previous columns qualified as labels
			else if(Pattern.matches("^[A-Z]{2,}$", element) &&
					(is_label_row ||
					 (dataStartRow == 0 && (possibleLastLabelCol + 1) == i)) &&
					 current_row == 0) {
				possibleLastLabelCol = i;
				is_label_row = true;
			}
			//Else if the value is empty and this has either already been
			//identified as a label row or data has not yet been identified and
			//all previous columns qualified as labels
			else if(element.equalsIgnoreCase("") &&
					(is_label_row ||
					 (dataStartRow == 0 && (possibleLastLabelCol + 1) == i)) &&
					 current_row == 0) {
				possibleLastLabelCol = i;
//			} else if(!hasData) {
//				LogBuffer.println("Column [" + i + "] Row [" + current_row +
//						"] Value [" + element +
//						"] hasn't matched a label. dataStartRow [" +
//						dataStartRow + "] possibleLastLabelCol [" +
//						possibleLastLabelCol + "] Matches pattern [" +
//						(Pattern.matches("^[A-Z]{2,}$", element) ?
//						 "YES" : "NO") + "]");
			}

			/*
			 * If the current string matches the pattern and is not in a label
			 * row and the index is greater than the last label column, we found
			 * the data start!
			 */
			if (Pattern.matches(fpRegex, element) && !is_label_row
					&& i > lastLabelCol) {

				//If the current column is greater than what you would expect
				//using uncommon or empty label headers
				if((possibleLastLabelCol + 1) < i) {
					LogBuffer.println("WARNING: Assuming data starts in " +
							"column [" + (possibleLastLabelCol + 2) +
							"].");
					dataStartCol = possibleLastLabelCol + 1;
				}
				//
				else if(lastLabelCol != possibleLastLabelCol) {
					LogBuffer.println("WARNING: Assuming data starts in " +
							"column [" + (i + 1) + "].");
					dataStartCol = i;
				} else {
					dataStartCol = i;
				}
				dataStartRow = current_row;

				/* Initialize data matrix */
				doubleData = new double[row_num - dataStartRow][];

				hasData = true;
				break;

			}
			/*
			 * Concatenate empty String, otherwise this will store a
			 * reference to the entire line from reader even when loading
			 * method and SwingWorker are closed. Strings are immutable!
			 */
			labels[i] = element + "";
		}

		return labels;
	}

	private String[] fillDoubles(final String line, final int current_row) {

		// load line as String array
		final String[] lineAsStrings = line.split("\\t", -1);
		final String[] labels = new String[dataStartCol];
		final double[] dataValues = new double[lineAsStrings.length
		                                       - dataStartCol];
		
		System.arraycopy(lineAsStrings, 0, labels, 0, dataStartCol);

		/*
		 * This ensures that references to immutable String (whole line from
		 * readline()!) do not stay in memory. This can and will screw up RAM if
		 * not done like this.
		 */
		for (int i = 0; i < labels.length; i++) {
			labels[i] += "";
		}

		for (int i = 0; i < lineAsStrings.length - dataStartCol; i++) {

			String element = lineAsStrings[i + dataStartCol];

			// handle parseDouble error somehow?
			// using the Pattern.matches method screws up
			// loading time by a factor of 1000....
			if (element.endsWith("e") || element.endsWith("E")) {
				element += "+00";
			}

			/* Trying to parse the String. If not possible add 0. */
			try {
				final double val = Double.parseDouble(element);
				dataValues[i] = val;

			} catch (final Exception e) {
				final double val = DataModel.NODATA;
				dataValues[i] = val;
			}
		}

		// Issue with length of stringLabels
		doubleData[current_row - dataStartRow] = dataValues;

		return labels;
	}

	private void assignDataToModel(final String[][] stringLabels) {

		/* ----- Tree file and config stuff ---- */

		// Parse the CDT File
		parseCDT(stringLabels);

		// If present, parse ATR File
		if (hasAID) {
			parseATR();

		} else {
			LogBuffer.println("No ATR file found for this CDT file.");
			targetModel.aidFound(false);
		}

		// If present, parse GTR File
		if (hasGID) {
			parseGTR();

		} else {
			LogBuffer.println("No GTR file found for this CDT file.");
			targetModel.gidFound(false);
		}

		setConfigData();
	}

	/**
	 * Loads or sets up configuration data for the file.
	 */
	private void setConfigData() {

		try {
			final String fileName = targetModel.getFileSet().getRoot();
			final String fileExt = targetModel.getFileSet().getExt();

			final Preferences fileNode = controller.getConfigNode()
					.node("File");

			Preferences documentConfig = null;
			final String[] childrenNodes = fileNode.childrenNames();

			final String default_name = "No file.";
			final String default_ext = "nan";

			/* Look if there's already a node for the file */
			boolean fileFound = false;
			if (childrenNodes.length > 0) {
				for (final String childrenNode : childrenNodes) {

					final String childName = fileNode.node(childrenNode).get(
							"name", default_name);
					final String childExt = fileNode.node(childrenNode).get(
							"extension", default_ext);

					if (childName.equalsIgnoreCase(fileName)
							&& childExt.equalsIgnoreCase(fileExt)) {
						documentConfig = fileNode.node(childrenNode);
						fileFound = true;
						break;
					}
				}
			}

			/* If no node for the file has been found, add one. */
			if (!fileFound) {
				documentConfig = fileNode.node("Model "
						+ (childrenNodes.length + 1));
				documentConfig.put("name", fileName);
				documentConfig.put("extension", fileExt);
			}

			targetModel.setDocumentConfig(documentConfig);

		} catch (final Exception e) {
			LogBuffer.logException(e);
			targetModel.setDocumentConfig(null);
		}
	}

	/**
	 * Sets up regex patterns which will be used to differentiate between labels
	 * and numerical data in tab-delimited table entries.
	 */
	private void setupPattern() {

		final String Digits = "(\\p{Digit}+)";
		final String emptyDigits = "(\\p{Digit}*)";
		final String HexDigits = "(\\p{XDigit}+)";

		// an exponent is 'e' or 'E' followed by an optionally
		// signed decimal integer.
		final String exp = "[eE][+-]?" + emptyDigits;

		this.fpRegex = ("[\\x00-\\x20]*" + // Optional leading
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
				+ ")[pP][+-]?" + Digits + "))" + "[fFdD]?))" + "[\\x00-\\x20]*");
		// Optional trailing "whitespace"
	}

	/**
	 * Parses the prefixes for the labels from the header data collected until
	 * this point.
	 *
	 * @param stringLabels
	 */
	private void parsePrefixes(final String[][] stringLabels) {

		/* lengths of prefix arrays */
		final int nGenePrefix = dataStartCol;
		final int nExprPrefix = dataStartRow;

		final String[] readGPrefixes = new String[nGenePrefix];
		final String[] readAPrefixes = new String[nExprPrefix];

		/* read gene prefixes */
		System.arraycopy(stringLabels[0], 0, readGPrefixes, 0, nGenePrefix);

		/* read array prefixes */
		for (int i = 0; i < nExprPrefix; i++) {

			readAPrefixes[i] = stringLabels[i][0];
		}

		/*
		 * The regex assurance is needed because the CDT-format is completely
		 * inconsistent and we wanna keep backwards compatibility.
		 */
		if (readAPrefixes[0].equalsIgnoreCase("GID")) {

			readAPrefixes[0] = assurePrefixNames(readGPrefixes);
		}

		/* set the prefixes */
		targetModel.setGenePrefix(readGPrefixes);
		targetModel.setArrayPrefix(readAPrefixes);

		/* set weight status */
		targetModel.setEweightFound(hasEWeight);
		targetModel.setGweightFound(hasGWeight);
	}

	/**
	 * Switches out false axis labeling due to inconsistent CDT format.
	 *
	 * @param gPrefixes
	 *            The row prefixes contain the right label.
	 * @return The correct prefix
	 */
	private static String assurePrefixNames(final String[] gPrefixes) {

		String finalPrefix = "OTHER";

		for (int i = 0; i < gPrefixes.length; i++) {

			if (!gPrefixes[i].equalsIgnoreCase("YORF")
					&& !gPrefixes[i].equalsIgnoreCase("GID")
					&& !gPrefixes[i].equalsIgnoreCase("GWEIGHT")) {
				finalPrefix = gPrefixes[i];
				break;
			}
		}

		return finalPrefix;
	}

	/**
	 * Reads the label prefixes and headers from the data and stores the data in
	 * the TVModel.
	 *
	 * @param stringLabels
	 */
	private void parseCDT(final String[][] stringLabels) {

		parsePrefixes(stringLabels);

		/* # of headers */
		final int nGene = doubleData.length;
		final int nExpr = doubleData[0].length;

		/* fill row header array */
		final String[][] gHeaders = new String[nGene][dataStartCol];

		for (int i = 0; i < nGene; i++) {

			gHeaders[i] = stringLabels[i + dataStartRow];
		}
		targetModel.setGeneHeaders(gHeaders);

		/* fill column header array */
		final String[][] aHeaders = new String[nExpr][dataStartRow];

		for (int i = 0; i < dataStartRow; i++) {

			for (int j = 0; j < nExpr; j++) {

				aHeaders[j][i] = stringLabels[i][j + dataStartCol];
			}
		}
		targetModel.setArrayHeaders(aHeaders);

		/* set data in TVModel */
		targetModel.setExprData(doubleData);
		targetModel.getDataMatrix().calculateMinMax();
	}

	private void parseGTR() {

		// First, load the GTR File
		final List<String[]> gtrData = loadTreeSet(fileSet.getGtr());
		
		/* In case an gtr file exists but is empty */
		if(gtrData.isEmpty()) {
			LogBuffer.println("GTR file empty.");
			targetModel.gidFound(false);
			return;
		}

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

	private void parseATR() {

		// First, load the ATR File
		final List<String[]> atrData = loadTreeSet(fileSet.getAtr());

		/* In case an atr file exists but is empty */
		if(atrData.isEmpty()) {
			LogBuffer.println("ATR file empty.");
			targetModel.aidFound(false);
			return;
		}
		
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
	
	/**
	 * Small wrapper for <extractPreviewData> 
	 * @param filename The path/ name of the file to be loaded.
	 * @return String[][] The preview data in array format for use in JTable.
	 */
	public static String[][] loadPreviewData(final String filename, 
			final String delimiter) {

		String[][] previewData;
		
		try {
			final BufferedReader br = new BufferedReader(new FileReader(
					filename));

			previewData = extractPreviewData(br, delimiter);

			br.close();

		} catch (final IOException e) {
			LogBuffer.logException(e);
			return new String[0][];
		}

		return previewData;
	}
	
	/**
	 * TODO put this into a PreviewLoader subclass which extends SwingWorker
	 * Get a String array representation of the first <LIMIT> elements of 
	 * data. This is for use in the data preview dialog only. The data
	 * @param A BufferedReader object to read through the file.
	 * @return String[][] The preview data in array format for use in JTable.
	 * @author chris0689
	 */
	private static String[][] extractPreviewData(final BufferedReader reader,
			final String delimiter) {

		final int LIMIT = 20;
		final String[][] previewData = new String[LIMIT][];
		String line;
		int count = 0;

		try {
			while ((line = reader.readLine()) != null && count < LIMIT) {

				// load line as String array
				final String[] lineAsStrings = line.split(delimiter, -1);
				previewData[count++] = Arrays.copyOfRange(lineAsStrings, 1, 
						LIMIT);
			}
		} catch (final IOException e) {
			LogBuffer.logException(e);
			return new String[][]{{"N/A"}};
		}

		return previewData;
	}

	private static List<String[]> loadTreeSet(final String loadingSet) {

		List<String[]> treeData = new ArrayList<String[]>();

		try {
			// Read data from specified file location
			final BufferedReader br = new BufferedReader(new FileReader(
					loadingSet));

			// Get data from file into String and double arrays
			// Put the arrays in ArrayLists for later access.
			treeData = extractTreeData(br);

			br.close();

		} catch (final IOException e) {
			LogBuffer.logException(e);
			return new ArrayList<String[]>();
		}

		return treeData;
	}
	
	private static List<String[]> extractTreeData(final BufferedReader reader) {

		final List<String[]> treeData = new ArrayList<String[]>();
		String line;

		try {
			while ((line = reader.readLine()) != null) {

				// load line as String array
				final String[] lineAsStrings = line.split("\\t", -1);
				treeData.add(lineAsStrings);
			}
		} catch (final IOException e) {
			LogBuffer.println("IOException during the "
					+ "extraction of GTR file: " + e.getMessage());
		}

		return treeData;
	}

	/**
	 * Encapsulates loading progress, so both label and progress bar can be
	 * updated using publish() and only one SwingWorker.
	 */
	class LoadStatus {

		private int progress;
		private int max_progress;
		private String status;

		public LoadStatus() {

			this.progress = 0;
			this.status = "Ready.";
		}

		public void setStatus(final String status) {

			this.status = status;
		}

		public void setProgress(final int prog) {

			this.progress = prog;
		}

		public void setMaxProgress(final int max) {

			this.max_progress = max;
		}

		public int getMaxProgress() {

			return max_progress;
		}

		public String getStatus() {

			return status;
		}

		public int getProgress() {

			return progress;
		}
	}
}
