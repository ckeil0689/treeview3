package edu.stanford.genetics.treeview.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
	private int gWeightCol;

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
		gWeightCol = 0;
		dataStartRow = 0;
		dataStartCol = 0;
		int current_row = 0;

		/* Read all lines and parse the data */
		while ((line = reader.readLine()) != null) {

			if (hasData) {
				ls.setStatus("Loading...");
				stringLabels[current_row] = fillDoubles(line, current_row);

			} else {
				stringLabels[current_row] = findData(line, current_row);
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
	private String[] findData(final String line, final int current_row) {

		/* Flag for the current_row to avoid adding weights as data. */
		boolean containsEWeight = false;

		// load line as String array
		final String[] lineAsStrings = line.split("\\t", -1);
		String[] labels;
		double[] dataValues;

		// loop over String array to convert applicable String to double
		// first find data start
		labels = new String[lineAsStrings.length];

		for (int i = 0; i < lineAsStrings.length; i++) {

			final String element = lineAsStrings[i];

			/* Check string if it is the label for GIDs or AIDs. */
			if (element.equalsIgnoreCase("GID")) {
				hasGID = true;
			}
			if (element.equalsIgnoreCase("AID")) {
				hasAID = true;
			}

			/*
			 * Check for GWEIGHT to avoid the weight being recognized as row
			 * start of actual data.
			 */
			if (element.equalsIgnoreCase("GWEIGHT")) {
				gWeightCol = i;
				hasGWeight = true;
			}

			/*
			 * Check for EWEIGHT to avoid the weight being recognized as column
			 * start of actual data.
			 */
			if (element.equalsIgnoreCase("EWEIGHT")) {
				hasEWeight = true;
				containsEWeight = true;
			}

			/*
			 * If the current string matches the pattern and is not in either
			 * the GWEIGHT column or EWEIGHT row, we found the data start!
			 */
			if (Pattern.matches(fpRegex, element)
					&& (!containsEWeight && i != gWeightCol)) {

				dataStartRow = current_row;
				dataStartCol = i;

				/* Initialize data matrix */
				doubleData = new double[row_num - dataStartRow][];

				hasData = true;
				break;

			} else {
				/*
				 * Concatenate empty String, otherwise this will store a
				 * reference to the entire line from reader even when loading
				 * method and SwingWorker are closed. Strings are immutable!
				 */
				labels[i] = element + "";
			}
		}

		/*
		 * Rest of first line with data has to be handled here, because
		 * reader.nextLine() will move on and cannot be set back.
		 */
		if (hasData) {
			doubleData = new double[row_num - dataStartRow][];
			dataValues = new double[lineAsStrings.length - dataStartCol];

			for (int i = 0; i < lineAsStrings.length - dataStartCol; i++) {

				String element = lineAsStrings[i + dataStartCol];

				// Check whether string can be double and is not
				// gweight
				if (Pattern.matches(fpRegex, element) && i != gWeightCol) {

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

			doubleData[current_row - dataStartRow] = dataValues;
		}

		// avoid first datarow to be added with null values
		if (hasData) {
			final String[] firstDataRow = new String[dataStartCol];

			for (int i = 0; i < labels.length; i++) {

				if (labels[i] == null) {
					break;
				}
				firstDataRow[i] = labels[i];
			}

			return firstDataRow;

		} else
			// handle line in which data has been found
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
		LogBuffer.println("Parsing main file.");
		parseCDT(stringLabels);

		// If present, parse ATR File
		if (hasAID) {
			LogBuffer.println("Reading ATR file.");
			parseATR();

		} else {
			LogBuffer.println("No ATR file found for this CDT file.");
			targetModel.aidFound(false);
		}

		// If present, parse GTR File
		if (hasGID) {
			LogBuffer.println("Reading GTR file.");
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
			LogBuffer.println("Getting configurations...");
			final String fileName = targetModel.getFileSet().getRoot();
			final String fileExt = targetModel.getFileSet().getExt();

			final Preferences fileNode = controller.getConfigNode()
					.node("File");

			Preferences documentConfig = null;
			final String[] childrenNodes = fileNode.childrenNames();

			final String default_name = "No file.";
			final String default_ext = "nan";

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

	private List<String[]> extractTreeData(final BufferedReader reader) {

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
	private String assurePrefixNames(final String[] gPrefixes) {

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

	private List<String[]> loadTreeSet(final String loadingSet) {

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
			e.printStackTrace();
		}

		return treeData;
	}

	/*
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
