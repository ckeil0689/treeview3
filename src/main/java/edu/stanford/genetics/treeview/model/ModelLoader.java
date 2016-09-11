package edu.stanford.genetics.treeview.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
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

	public final static String DEFAULT_DELIM = "\\t";

	protected TVController controller;

	/* Reference to the main model which will hold the data */
	protected TVModel targetModel;
	private final FileSet fileSet;

	/* 2D array to hold numerical data */
	private double[][] doubleData;

	private DataLoadInfo dataInfo;
	private String delimiter;

	/* Total line number of file to be loaded */
	private int row_num;

	private int dataStartRow;
	private int dataStartColumn;

	private boolean hasGID = false;
	private boolean hasAID = false;
	private boolean hasEWeight = false;
	private boolean hasGWeight = false;

	public ModelLoader(final DataModel model, final TVController controller,
			final DataLoadInfo dataInfo) {

		this.controller = controller;
		this.targetModel = (TVModel) model;
		this.fileSet = model.getFileSet();
		this.dataInfo = dataInfo;
		this.dataStartRow = dataInfo.getDataCoords()[0];
		this.dataStartColumn = dataInfo.getDataCoords()[1];
		this.delimiter = dataInfo.getDelimiter();
	}

	public void setDelimiter(final String delimiter) {

		this.delimiter = delimiter;
	}

	public void setDataCoords(int dataStartRow, int dataStartColumn) {

		this.dataStartRow = dataStartRow;
		this.dataStartColumn = dataStartColumn;
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
		doubleData = new double[row_num - dataStartRow][];

		final String[][] stringLabels = new String[row_num][];

		final LoadStatus ls = new LoadStatus();
		ls.setProgress(0);
		ls.setMaxProgress(row_num);
		ls.setStatus("Preparing...");

		final File file = new File(fileSet.getCdt());
		final BufferedReader reader = new BufferedReader(new FileReader(file));

		String line;
		int row_idx = 0;

		ls.setStatus("Loading...");

		/* Read all lines and parse the data */
		while ((line = reader.readLine()) != null) {

			String[] lineAsStrings = line.split(delimiter, -1);

			if (row_idx < dataStartRow) {
				stringLabels[row_idx] = lineAsStrings;

			} else {
				String[] labels = partitionRow(lineAsStrings, row_idx);
				stringLabels[row_idx] = labels;
			}

			ls.setProgress(row_idx++);
			publish(ls);
		}

		ls.setStatus("Getting ready...");
		publish(ls);

		analyzeLabels(stringLabels);

		/* Parse tree and config files */
		assignDataToModel(stringLabels);

		ls.setStatus("Done!");
		publish(ls);

		reader.close();
		return null;
	}

	@Override
	protected void done() {

		doubleData = null;

		/* Update GUI, set new DendroView */
		controller.finishLoading(dataInfo);
	}

	/**
	 * Check the labels for commonly used labels that are useful for TreeView.
	 */
	private void analyzeLabels(String[][] stringLabels) {

		for (int i = 0; i < dataStartRow; i++) {

			String[] labels = stringLabels[i];
			for (int j = 0; j < dataStartColumn; j++) {
				if ("GID".equalsIgnoreCase(labels[j])) {
					hasGID = true;
				} else if ("AID".equalsIgnoreCase(labels[j])) {
					hasAID = true;
				} else if ("GWEIGHT".equalsIgnoreCase(labels[j])) {
					hasGWeight = true;
				} else if ("EWEIGHT".equalsIgnoreCase(labels[j])) {
					hasEWeight = true;
				}
			}
		}
	}

	private String[] partitionRow(final String[] lineAsStrings,
			final int row_idx) {

		// load line as String array
		final String[] labels = new String[dataStartColumn];
		final double[] dataValues = new double[lineAsStrings.length
				- dataStartColumn];

		System.arraycopy(lineAsStrings, 0, labels, 0, dataStartColumn);

		/*
		 * This ensures that references to immutable String (whole line from
		 * readline()!) do not stay in memory. This can and will screw up RAM if
		 * not done like this.
		 */
		for (int i = 0; i < labels.length; i++) {
			labels[i] += "";
		}

		for (int i = 0; i < lineAsStrings.length - dataStartColumn; i++) {

			String element = lineAsStrings[i + dataStartColumn];

			/* no data value should ever be a word, so ending with e is
			 * considered as exponent value notation */
			if (element.endsWith("e") || element.endsWith("E")) {
				element += "+00";
			}

			/* Trying to parse the String. If not possible add 0. */
			try {
				double val = Double.parseDouble(element);
				
//				/* NaN and Infinite treated as non-data values */
//				if(Double.isNaN(val) || Double.isInfinite(val)) {
//					val = DataModel.NAN;
//				} 
//				
				dataValues[i] = val;

			} catch (final NumberFormatException e) {
				dataValues[i] = DataModel.NAN;
			}
		}

		doubleData[row_idx - dataStartRow] = dataValues;

		return labels;
	}

	private void assignDataToModel(final String[][] stringLabels) {

		/* ----- Tree file and config stuff ---- */

		// Parse the CDT File
		/* TODO wrap in try-catch */
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
				documentConfig = fileNode.node("Model"
						+ (childrenNodes.length + 1));
				documentConfig.put("name", fileName);
				documentConfig.put("extension", fileExt);
				storeDataLoadInfo(documentConfig);
			}

			targetModel.setDocumentConfig(documentConfig);

		} catch (final Exception e) {
			LogBuffer.logException(e);
			targetModel.setDocumentConfig(null);
		}
	}

	private void storeDataLoadInfo(Preferences node) {

		final int rowCoord = dataInfo.getDataCoords()[0];
		final int colCoord = dataInfo.getDataCoords()[1];

		node.putBoolean("firstLoad", false);
		node.put("delimiter", dataInfo.getDelimiter());
		node.putInt("rowCoord", rowCoord);
		node.putInt("colCoord", colCoord);
	}

	/**
	 * Parses the prefixes from the label data collected until
	 * this point.
	 *
	 * @param stringLabels
	 */
	private void parsePrefixes(final String[][] stringLabels) {

		/* lengths of prefix arrays */
		final int nRowPrefix = dataStartColumn;
		final int nColPrefix = dataStartRow;

		final String[] readRowPrefixes = new String[nRowPrefix];
		final String[] readColPrefixes = new String[nColPrefix];

		/* read gene prefixes */
		System.arraycopy(stringLabels[0], 0, readRowPrefixes, 0, nRowPrefix);

		/* read array prefixes */
		for (int i = 0; i < nColPrefix; i++) {
			readColPrefixes[i] = stringLabels[i][0];
		}

		/*
		 * The regex assurance is needed because the CDT-format is completely
		 * inconsistent and we wanna keep backwards compatibility.
		 */
		if (readColPrefixes[0].equalsIgnoreCase("GID")) {
			readColPrefixes[0] = assurePrefixNames(readRowPrefixes);
		}
		
		/* Replacing empty or whitespace-only labels */
		replaceEmptyLabels(readRowPrefixes, "ROW");
		replaceEmptyLabels(readColPrefixes, "COLUMN");
		
		/* set the prefixes */
		targetModel.setRowPrefixes(readRowPrefixes);
		targetModel.setColumnPrefixes(readColPrefixes);

		/* set weight status */
		targetModel.setEweightFound(hasEWeight);
		targetModel.setGweightFound(hasGWeight);
	}
	
	private String[] replaceEmptyLabels(String[] original, final String axis) {
		
		Pattern p = Pattern.compile("(^\\s*$)", 
				Pattern.UNICODE_CHARACTER_CLASS);
		
		for(int i = 0; i < original.length; i++) {
			Matcher m = p.matcher(original[i]);
			if(m.find()) {
				int idx = i + 1;
				original[i] = axis + " LABELS " + idx;
			}
		}
		
		return original;
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
	 * Reads the label prefixes and labels from the data and stores the data in
	 * the TVModel.
	 *
	 * @param stringLabels
	 */
	private void parseCDT(final String[][] stringLabels) {

		parsePrefixes(stringLabels);

		/* # of labels */
		final int nRows = doubleData.length;
		final int nCols = doubleData[0].length;

		/* fill row label array */
		final String[][] rowLabels = new String[nRows][dataStartColumn];

		for (int i = 0; i < nRows; i++) {
			rowLabels[i] = stringLabels[i + dataStartRow];
		}
		
		targetModel.setRowLabels(rowLabels);

		/* fill column label array */
		final String[][] colLabels = new String[nCols][dataStartRow];

		for (int i = 0; i < dataStartRow; i++) {
			for (int j = 0; j < nCols; j++) {
				colLabels[j][i] = stringLabels[i][j + dataStartColumn];
			}
		}
		
		targetModel.setColumnLabels(colLabels);

		/* set data in TVModel */
		targetModel.setExprData(doubleData);
		targetModel.getDataMatrix().calculateBaseValues();
	}

	private void parseGTR() {

		// First, load the GTR File
		final List<String[]> gtrData = loadTreeSet(fileSet.getGtr());

		/* In case an gtr file exists but is empty */
		if (gtrData.isEmpty()) {
			LogBuffer.println("GTR file empty.");
			targetModel.gidFound(false);
			return;
		}

		final String[] firstRow = gtrData.get(0);
		if ( // decide if this is not an extended file..
		(firstRow.length == 4)// is the length classic?
				&& !(firstRow[0].equalsIgnoreCase("NODEID"))) {
			// okay, need to assign prefixes...
			targetModel.setGtrPrefixes(new String[] { "NODEID", "LEFT", "RIGHT",
					"CORRELATION" });

			final String[][] gtrLabels = new String[gtrData.size()][];
			for (int i = 0; i < gtrLabels.length; i++) {
				gtrLabels[i] = gtrData.get(i);
			}
			targetModel.setGtrLabels(gtrLabels);

		} else {// first row of tempVector is actual prefix names...
			targetModel.setGtrPrefixes(firstRow);

			final String[][] gtrLabels = new String[gtrData.size() - 1][];
			for (int i = 0; i < gtrLabels.length; i++) {
				gtrLabels[i] = gtrData.get(i + 1);
			}
			targetModel.setGtrLabels(gtrLabels);
		}

		targetModel.hashGIDs();
		targetModel.hashGTRs();
		targetModel.gidFound(hasGID);
	}

	private void parseATR() {

		// First, load the ATR File
		final List<String[]> atrData = loadTreeSet(fileSet.getAtr());

		/* In case an atr file exists but is empty */
		if (atrData.isEmpty()) {
			LogBuffer.println("ATR file empty.");
			targetModel.aidFound(false);
			return;
		}

		final String[] firstRow = atrData.get(0);
		if ( // decide if this is not an extended file..
		(firstRow.length == 4)// is the length classic?
				&& !(firstRow[0].equalsIgnoreCase("NODEID"))) {

			// okay, need to assign prefixes...
			targetModel.setAtrPrefixes(new String[] { "NODEID", "LEFT", "RIGHT",
					"CORRELATION" });

			final String[][] atrLabels = new String[atrData.size()][];
			for (int i = 0; i < atrLabels.length; i++) {
				atrLabels[i] = atrData.get(i);
			}
			targetModel.setAtrLabels(atrLabels);
		} else {// first row of tempVector is actual prefix names...
			targetModel.setAtrPrefixes(firstRow);

			final String[][] atrLabels = new String[atrData.size() - 1][];
			for (int i = 0; i < atrLabels.length; i++) {
				atrLabels[i] = atrData.get(i + 1);
			}
			targetModel.setAtrLabels(atrLabels);
		}

		targetModel.hashAIDs();
		targetModel.hashATRs();
		targetModel.aidFound(hasAID);
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
	public static class LoadStatus {

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
