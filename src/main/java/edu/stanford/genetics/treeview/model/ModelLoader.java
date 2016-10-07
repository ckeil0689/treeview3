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

/** The class responsible for loading data into the TVModel. */
public class ModelLoader extends SwingWorker<Void, LoadStatus> {

	public final static String DEFAULT_DELIM = "\\t";

	protected TVController controller;

	// Reference to the main model which will hold the data
	protected TVModel targetModel;
	private final FileSet fileSet;

	// 2D array to hold numerical data
	private double[][] doubleData;

	private DataLoadInfo dataInfo;
	private String delimiter;

	// Total line number of file to be loaded
	private int row_num;

	private int dataStartRow;
	private int dataStartColumn;

	private boolean hasGID = false;
	private boolean hasAID = false;
	private boolean hasEWeight = false;
	private boolean hasGWeight = false;

	public ModelLoader(	final DataModel model, final TVController controller,
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

		// Update the GUI
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

		// Read all lines and parse the data (creates a String matrix)
		while((line = reader.readLine()) != null) {

			String[] lineAsStrings = line.split(delimiter, -1);

			if(row_idx < dataStartRow) {
				stringLabels[row_idx] = lineAsStrings;

			}
			else {
				String[] labels = partitionRow(lineAsStrings, row_idx);
				stringLabels[row_idx] = labels;
			}

			ls.setProgress(row_idx++);
			publish(ls);
		}

		ls.setStatus("Getting ready...");
		publish(ls);

		analyzeLabels(stringLabels);

		// Parse tree and config files
		assignDataToModel(stringLabels);

		ls.setStatus("Done!");
		publish(ls);

		reader.close();
		return null;
	}

	@Override
	protected void done() {

		doubleData = null;

		// Update GUI, set new DendroView
		controller.finishLoading(dataInfo);
	}

	/** Check the labels for commonly used labels that are useful for TreeView. */
	private void analyzeLabels(String[][] stringLabels) {

		for(int i = 0; i < dataStartRow; i++) {

			String[] labels = stringLabels[i];
			for(int j = 0; j < dataStartColumn; j++) {
				if("GID".equalsIgnoreCase(labels[j])) {
					hasGID = true;
				}
				else if("AID".equalsIgnoreCase(labels[j])) {
					hasAID = true;
				}
				else if("GWEIGHT".equalsIgnoreCase(labels[j])) {
					hasGWeight = true;
				}
				else if("EWEIGHT".equalsIgnoreCase(labels[j])) {
					hasEWeight = true;
				}
			}
		}
	}

	/** Splits a row into labels and data.
	 * 
	 * @param lineAsStrings - The line in the file after it has been split into a
	 *          String array.
	 * @param row_idx - The index of the current row.
	 * @return An array of labels of the current row at index row_idx */
	private String[] partitionRow(final String[] lineAsStrings,
																final int row_idx) {

		// load line as String array
		final String[] labels = new String[dataStartColumn];
		final double[] dataValues = new double[lineAsStrings.length -
																						dataStartColumn];

		System.arraycopy(lineAsStrings, 0, labels, 0, dataStartColumn);

		/*
		 * This ensures that references to immutable String (whole line from
		 * readline()!) do not stay in memory. This can and will screw up RAM if
		 * not done like this.
		 */
		for(int i = 0; i < labels.length; i++) {
			labels[i] += "";
		}

		// Iterate over row data values (after labels)
		for(int i = 0; i < lineAsStrings.length - dataStartColumn; i++) {

			String element = lineAsStrings[i + dataStartColumn];

			/* no data value should ever be a word, so ending with e is
			 * considered as exponent value notation */
			if(element.endsWith("e") || element.endsWith("E")) {
				element += "+00";
			}

			// Trying to parse the String. If not possible add defined NAN.
			try {
				double val = Double.parseDouble(element);
				dataValues[i] = val;

			}
			catch(final NumberFormatException e) {
				dataValues[i] = DataModel.NAN;
			}
		}

		doubleData[row_idx - dataStartRow] = dataValues;

		return labels;
	}

	private void assignDataToModel(final String[][] stringLabels) {

		// Parse the CDT File
		/* TODO wrap in try-catch */
		parseCDT(stringLabels);

		// If present, parse ATR File
		if(hasAID) {
			parseATR();

		}
		else {
			LogBuffer.println("No ATR file found for this CDT file.");
			targetModel.aidFound(false);
		}

		// If present, parse GTR File
		if(hasGID) {
			parseGTR();

		}
		else {
			LogBuffer.println("No GTR file found for this CDT file.");
			targetModel.gidFound(false);
		}

		setConfigData();
	}

	/** Loads or sets up configuration data for the file. */
	private void setConfigData() {

		try {
			final String fileName = targetModel.getFileSet().getRoot();
			final String fileExt = targetModel.getFileSet().getExt();

			final Preferences fileNode = controller.getConfigNode().node("File");

			Preferences documentConfig = null;
			final String[] childrenNodes = fileNode.childrenNames();

			final String default_name = "No file.";
			final String default_ext = "nan";

			// Look if there's already a node for the file
			boolean fileFound = false;
			if(childrenNodes.length > 0) {
				for(final String childrenNode : childrenNodes) {

					final String childName = fileNode	.node(childrenNode)
																						.get("name", default_name);
					final String childExt = fileNode.node(childrenNode)
																					.get("extension", default_ext);

					if(childName.equalsIgnoreCase(fileName) && childExt
																															.equalsIgnoreCase(fileExt)) {
						documentConfig = fileNode.node(childrenNode);
						fileFound = true;
						break;
					}
				}
			}

			// If no node for the file has been found, add one.
			if(!fileFound) {
				documentConfig = fileNode.node("Model" + (childrenNodes.length + 1));
				documentConfig.put("name", fileName);
				documentConfig.put("extension", fileExt);
				storeDataLoadInfo(documentConfig);
			}

			targetModel.setDocumentConfig(documentConfig);

		}
		catch(final Exception e) {
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

	/** Parses the label types from the label data collected until
	 * this point.
	 *
	 * @param stringLabels */
	private void parseLabelTypes(final String[][] stringLabels) {

		// lengths of label type arrays
		final int nRowLabelType = dataStartColumn;
		final int nColLabelType = dataStartRow;

		String[] readRowLabelTypes = new String[nRowLabelType];
		String[] readColLabelTypes = new String[nColLabelType];

		if(nRowLabelType > 0) {
			// read row label types
			System.arraycopy(stringLabels[0], 0, readRowLabelTypes, 0, nRowLabelType);

		}
		else {
			// set a default empty label type
			readRowLabelTypes = new String[] {""};
		}

		if(nColLabelType > 0) {
			// read column label types
			for(int i = 0; i < nColLabelType; i++) {
				readColLabelTypes[i] = stringLabels[i][0];
			}

			/*
			 * The regex assurance is needed because the CDT-format is completely
			 * inconsistent and we wanna keep backwards compatibility.
			 */
			if(readColLabelTypes[0].equalsIgnoreCase("GID")) {
				readColLabelTypes[0] = assureLabelTypeNames(readRowLabelTypes);
			}
		}
		else {
			// set a default empty label type
			readColLabelTypes = new String[] {""};
		}

		// Replacing empty or whitespace-only labels
		replaceEmptyLabels(readRowLabelTypes, "ROW");
		replaceEmptyLabels(readColLabelTypes, "COLUMN");

		// set the label types
		targetModel.setRowLabelTypes(readRowLabelTypes);
		targetModel.setColumnLabelTypes(readColLabelTypes);

		// set weight status
		targetModel.setEweightFound(hasEWeight);
		targetModel.setGweightFound(hasGWeight);
	}

	private String[] replaceEmptyLabels(String[] original, final String axis) {

		Pattern p = Pattern.compile("(^\\s*$)", Pattern.UNICODE_CHARACTER_CLASS);

		for(int i = 0; i < original.length; i++) {
			Matcher m = p.matcher(original[i]);
			if(m.find()) {
				int idx = i + 1;
				original[i] = axis + " LABELS " + idx;
			}
		}

		return original;
	}

	/** Switches out false axis labeling due to inconsistent CDT format.
	 *
	 * @param rowLabelTypes
	 *          The row label types contain the right label.
	 * @return The correct label type */
	private static String assureLabelTypeNames(final String[] rowLabelTypes) {

		String finalLabelType = "OTHER";

		for(int i = 0; i < rowLabelTypes.length; i++) {
			if(!rowLabelTypes[i].equalsIgnoreCase("YORF") &&	!rowLabelTypes[i]
																																				.equalsIgnoreCase("GID") &&
					!rowLabelTypes[i].equalsIgnoreCase("GWEIGHT")) {
				finalLabelType = rowLabelTypes[i];
				break;
			}
		}

		return finalLabelType;
	}

	/** Reads the label types and labels from the data and stores the data in
	 * the TVModel.
	 *
	 * @param stringLabels */
	private void parseCDT(final String[][] stringLabels) {

		parseLabelTypes(stringLabels);

		// # of labels
		final int nRows = doubleData.length;
		final int nCols = doubleData[0].length;

		// fill row label array
		String[][] rowLabels;
		if(dataStartColumn > 0) {
			rowLabels = new String[nRows][dataStartColumn];
			for(int i = 0; i < nRows; i++) {
				rowLabels[i] = stringLabels[i + dataStartRow];
			}
		}
		else {
			// default row labels
			rowLabels = new String[nRows][1];
			for(int i = 0; i < nRows; i++) {
				rowLabels[i] = new String[] {"Row " + (i + 1)};
			}
		}

		targetModel.setRowLabels(rowLabels);

		// fill column label array
		final String[][] colLabels;
		if(dataStartRow > 0) {
			colLabels = new String[nCols][dataStartRow];
			for(int i = 0; i < dataStartRow; i++) {
				for(int j = 0; j < nCols; j++) {
					colLabels[j][i] = stringLabels[i][j + dataStartColumn];
				}
			}
		}
		else {
			// default column labels
			colLabels = new String[nCols][1];
			for(int i = 0; i < nCols; i++) {
				colLabels[i] = new String[] {"Column " + (i + 1)};
			}
		}

		targetModel.setColumnLabels(colLabels);

		// set data in TVModel
		targetModel.setExprData(doubleData);
		targetModel.getDataMatrix().calculateBaseValues();
	}

	private void parseGTR() {

		// First, load the GTR File
		final List<String[]> gtrData = loadTreeSet(fileSet.getGtr());

		// In case an gtr file exists but is empty
		if(gtrData.isEmpty()) {
			LogBuffer.println("GTR file empty.");
			targetModel.gidFound(false);
			return;
		}

		final String[] firstRow = gtrData.get(0);
		if( // decide if this is not an extended file..
		(firstRow.length == 4)// is the length classic?
				&& !(firstRow[0].equalsIgnoreCase("NODEID"))) {
			// okay, need to assign label types...
			targetModel.setGtrLabelTypes(new String[] {	"NODEID", "LEFT", "RIGHT",
																									"CORRELATION"});

			final String[][] gtrLabels = new String[gtrData.size()][];
			for(int i = 0; i < gtrLabels.length; i++) {
				gtrLabels[i] = gtrData.get(i);
			}
			targetModel.setGtrLabels(gtrLabels);

		}
		else {// first row of tempVector is actual label type names...
			targetModel.setGtrLabelTypes(firstRow);

			final String[][] gtrLabels = new String[gtrData.size() - 1][];
			for(int i = 0; i < gtrLabels.length; i++) {
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
		if(atrData.isEmpty()) {
			LogBuffer.println("ATR file empty.");
			targetModel.aidFound(false);
			return;
		}

		final String[] firstRow = atrData.get(0);
		if( // decide if this is not an extended file..
		(firstRow.length == 4)// is the length classic?
				&& !(firstRow[0].equalsIgnoreCase("NODEID"))) {

			// okay, need to assign label types...
			targetModel.setAtrLabelTypes(new String[] {	"NODEID", "LEFT", "RIGHT",
																									"CORRELATION"});

			final String[][] atrLabels = new String[atrData.size()][];
			for(int i = 0; i < atrLabels.length; i++) {
				atrLabels[i] = atrData.get(i);
			}
			targetModel.setAtrLabels(atrLabels);
		}
		else {// first row of tempVector is actual label type names...
			targetModel.setAtrLabelTypes(firstRow);

			final String[][] atrLabels = new String[atrData.size() - 1][];
			for(int i = 0; i < atrLabels.length; i++) {
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
			final BufferedReader br = new BufferedReader(new FileReader(loadingSet));

			// Get data from file into String and double arrays
			// Put the arrays in ArrayLists for later access.
			treeData = extractTreeData(br);

			br.close();

		}
		catch(final IOException e) {
			LogBuffer.logException(e);
			return new ArrayList<String[]>();
		}

		return treeData;
	}

	private static List<String[]> extractTreeData(final BufferedReader reader) {

		final List<String[]> treeData = new ArrayList<String[]>();
		String line;

		try {
			while((line = reader.readLine()) != null) {

				// load line as String array
				final String[] lineAsStrings = line.split("\\t", -1);
				treeData.add(lineAsStrings);
			}
		}
		catch(final IOException e) {
			LogBuffer.println("IOException during the " +	"extraction of GTR file: " +
												e.getMessage());
		}

		return treeData;
	}

	/** Encapsulates loading progress, so both label and progress bar can be
	 * updated using publish() and only one SwingWorker. */
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
