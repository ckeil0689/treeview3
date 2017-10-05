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

/** The class responsible for loading data into the TVModel. */
public class ModelLoader extends SwingWorker<Void, LoadStatus> {

	protected TVController controller;

	// Reference to the main model which will hold the data
	protected TVModel targetModel;
	private final FileSet fileSet;

	// 2D array to hold numerical data
	private double[][] doubleData;

	private DataLoadInfo dataInfo;

	// total line number of file to be loaded
	private int nRows;

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
	}

	/** Set the delimiter which is used to define separate cells in
	 * the data file.
	 * 
	 * @param delimiter - The delimiter used for reading the file. */
	public void setDelimiter(final String delimiter) {

		dataInfo.setDelimiter(delimiter);
	}

	/** Define the start coordinate of the first data cell in the data table to be
	 * loaded (first non-label cell).
	 * 
	 * @param dataStartRow - The row index of the first data cell.
	 * @param dataStartCol - The column index of the first data cell. */
	public void setDataCoords(int dataStartRow, int dataStartCol) {

		dataInfo.setDataStartRow(dataStartRow);
		dataInfo.setDataStartCol(dataStartCol);
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
		this.nRows = Helper.countFileLines(new File(fileSet.getCdt()));
		this.doubleData = new double[nRows - dataInfo.getDataStartRow()][];

		final String[][] stringLabels = new String[nRows][];

		final LoadStatus ls = new LoadStatus();
		ls.setProgress(0);
		ls.setMaxProgress(nRows);
		ls.setStatus("Preparing...");

		final File file = new File(fileSet.getCdt());
		final BufferedReader reader = new BufferedReader(new FileReader(file));

		String line;
		int row_idx = 0;

		ls.setStatus("Loading...");

		// Read all lines and parse the data (creates a String matrix)
		while((line = reader.readLine()) != null) {

			String[] lineAsStrings = line.split(dataInfo.getDelimiter(), -1);

			if(row_idx < dataInfo.getDataStartRow()) {
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
		
		final Preferences fileNode = controller.getConfigNode().node("File");
		storeDataLoadInfo(fileNode, targetModel, dataInfo);

		// Update GUI, set new DendroView
		controller.finishLoading(dataInfo);
	}

	/** Check the labels for commonly used labels that are useful for TreeView. */
	private void analyzeLabels(String[][] stringLabels) {

		for(int i = 0; i < dataInfo.getDataStartRow(); i++) {

			String[] labels = stringLabels[i];
			for(int j = 0; j < dataInfo.getDataStartCol(); j++) {
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
		final String[] labels = new String[dataInfo.getDataStartCol()];
		final double[] dataValues = new double[lineAsStrings.length - dataInfo
																																					.getDataStartCol()];

		System.arraycopy(lineAsStrings, 0, labels, 0, dataInfo.getDataStartCol());

		/*
		 * This ensures that references to immutable String (whole line from
		 * readline()!) do not stay in memory. This can and will screw up RAM if
		 * not done like this.
		 */
		for(int i = 0; i < labels.length; i++) {
			labels[i] += "";
		}

		for(int i = 0; i < lineAsStrings.length - dataInfo.getDataStartCol(); i++) {
			String element = lineAsStrings[i + dataInfo.getDataStartCol()];

			/* no data value should ever be a word, so ending with e is
			 * considered as exponent value notation. If it indeed is not a data
			 * value, then the NumberFormatException will be triggered */
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

		doubleData[row_idx - dataInfo.getDataStartRow()] = dataValues;

		return labels;
	}

	private void assignDataToModel(final String[][] stringLabels) {

		// Parse the CDT File
		/* TODO wrap in try-catch */
		LogBuffer.println("Parsing CDT file.");
		parseCDT(stringLabels);

		// If present, parse ATR File
		if(hasAID) {
			LogBuffer.println("Parsing ATR file.");
			parseATR();
			LogBuffer.println("Done parsing ATR file.");
		}
		else {
			LogBuffer.println("No ATR file found for this CDT file.");
			targetModel.aidFound(false);
		}

		// If present, parse GTR File
		if(hasGID) {
			LogBuffer.println("Parsing GTR file.");
			parseGTR();
			LogBuffer.println("Done parsing GTR file.");
		}
		else {
			LogBuffer.println("No GTR file found for this CDT file.");
			targetModel.gidFound(false);
		}
	}

	/** Loads or sets up configuration data for the file. */
	public static Preferences getConfigData(final DataModel model, 
	                                 final Preferences fileNode) {

		LogBuffer.println("Loading Model entry from File node.");

		try {
			final String fileSetName = model.getFileSet().getName();

			Preferences documentConfig = null;
			final String[] childrenNodes = fileNode.childrenNames();

			// Look if there's already a node for the file
			boolean fileFound = false;
			if(childrenNodes.length > 0) {
				for(final String entry : childrenNodes) {
					Preferences childNode = fileNode.node(entry);
					final String connectedFS = childNode.get("connectedFileSet", "none");

					if(connectedFS.equalsIgnoreCase(fileSetName)) {
						documentConfig = childNode;
						fileFound = true;
						break;
					}
				}
			}

			// If no node for the file has been found, add one.
			if(!fileFound) {
				documentConfig = fileNode.node("Model-" + (childrenNodes.length + 1));
				documentConfig.put("connectedFileSet", fileSetName);
			}
			
			LogBuffer.println("Found Model entry: " + documentConfig);
			return documentConfig;
		}
		catch(final Exception e) {
			LogBuffer.logException(e);
			return null;
		}
	}

	/** Store load info such as coordinates of first data cell and used delimiter
	 * to the supplied node.
	 * 
	 * @param fileNode - The node at which the values will be stored. */
	public static void storeDataLoadInfo(final Preferences fileNode,
	                                     final DataModel model,
	                                     final DataLoadInfo dataLoadInfo) {

		if(fileNode == null) {
			LogBuffer.println("Cannot store any data load information. " +
				"The Preferences node for the model is not defined.");
			return;
		}
		
		Preferences modelNode = getConfigData(model, fileNode);
		
		modelNode.putBoolean("firstLoad", false);
		modelNode.putBoolean("isRowClustered", model.gidFound());
		modelNode.putBoolean("isColClustered", model.aidFound());
		modelNode.put("delimiter", dataLoadInfo.getDelimiter());
		modelNode.putInt("rowCoord", dataLoadInfo.getDataStartRow());
		modelNode.putInt("colCoord", dataLoadInfo.getDataStartCol());
		modelNode.put("rowLabelTypes", dataLoadInfo.getRowLabelTypesAsString());
		modelNode.put("colLabelTypes", dataLoadInfo.getColLabelTypesAsString());
		
		((TVModel) model).setDocumentConfig(modelNode);
		
	}

	/** Parses the label types from the label data collected until
	 * this point. It will set a default value if no labels are present and will
	 * also create default label types if labels are present in the data, but
	 * no label types could be found.
	 *
	 * @param stringLabels - the parsed data lines containing the labels */
	private void parseLabelTypes(final String[][] stringLabels) {

		// supposed lengths of label type arrays (by selection or detection)
		// this does NOT indicate whether the label types are present!
		final int nRowLabelType = dataInfo.getDataStartCol();
		final int nColLabelType = dataInfo.getDataStartRow();

		String[] readRowLabelTypes = DataLoadInfo.DEFAULT_LABEL_TYPES;
		String[] readColLabelTypes = DataLoadInfo.DEFAULT_LABEL_TYPES;

		// row labels exist, replace single default label type
		if(nRowLabelType > 0) {
			readRowLabelTypes = new String[nRowLabelType];

			// may only add row label types if they are present
			boolean areRowLabelTypesPresent = (dataInfo.getDataStartRow() > 0);
			if(areRowLabelTypesPresent) {
				String[] firstDataRow = stringLabels[0];
				System.arraycopy(firstDataRow, 0, readRowLabelTypes, 0, nRowLabelType);
			}
		}

		// column labels exist, replace single default label type
		if(nColLabelType > 0) {
			readColLabelTypes = new String[nColLabelType];

			// may only add column label types if they are present
			boolean areColLabelTypesPresent = (dataInfo.getDataStartCol() > 0);
			if(areColLabelTypesPresent) {
				for(int i = 0; i < nColLabelType; i++) {
					// do not add known row label types
					String colLabelType = stringLabels[i][0];
					if(PreviewLoader.isCommonLabel(colLabelType, PreviewLoader.COMMON_ROW_LABELS)) {
						continue;
					}
					readColLabelTypes[i] = stringLabels[i][0];
				}
			}
		}

		// Replacing empty or whitespace-only label types
		readRowLabelTypes = replaceEmptyLabelTypes(readRowLabelTypes, "ROW");
		readColLabelTypes = replaceEmptyLabelTypes(readColLabelTypes, "COLUMN");

		// set the label types
		targetModel.setRowLabelTypes(readRowLabelTypes);
		dataInfo.setRowLabelTypes(readRowLabelTypes);

		targetModel.setColumnLabelTypes(readColLabelTypes);
		dataInfo.setColLabelTypes(readColLabelTypes);

		// set weight status
		targetModel.setEweightFound(hasEWeight);
		targetModel.setGweightFound(hasGWeight);
	}

	/** Replaces empty or null values in the label type array with numbered
	 * defaults.
	 * 
	 * @param originalLabelTypes - the array of original (detected label types)
	 * @param axis - the axis type for the labels (row or column)
	 * @return an adjusted String array without empty or null values. */
	private String[] replaceEmptyLabelTypes(String[] originalLabelTypes,
																					final String axis) {

		String[] newLabelTypes = new String[originalLabelTypes.length];
		Pattern p = Pattern.compile("(^\\s*$)", Pattern.UNICODE_CHARACTER_CLASS);

		for(int i = 0; i < originalLabelTypes.length; i++) {
			String oldLabelType = originalLabelTypes[i];
			// java checks from left to right, so null is handled
			String newLabelType = oldLabelType;
			if(oldLabelType == null || p.matcher(oldLabelType).find()) {
				int idx = i + 1;
				newLabelType = axis + " LABELS " + idx;
			}

			newLabelTypes[i] = newLabelType;
		}

		return newLabelTypes;
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
		if(dataInfo.getDataStartCol() > 0) {
			rowLabels = new String[nRows][dataInfo.getDataStartCol()];
			for(int i = 0; i < nRows; i++) {
				rowLabels[i] = stringLabels[i + dataInfo.getDataStartRow()];
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
		if(dataInfo.getDataStartRow() > 0) {
			colLabels = new String[nCols][dataInfo.getDataStartRow()];
			for(int i = 0; i < dataInfo.getDataStartRow(); i++) {
				for(int j = 0; j < nCols; j++) {
					colLabels[j][i] = stringLabels[i][j + dataInfo.getDataStartCol()];
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

		targetModel.setExprData(doubleData);
		targetModel.getDataMatrix().calculateBaseValues();

		LogBuffer.println("Done parsing for CDT-format.");
	}

	//TODO replace with ModelTreeAdder to reduce code
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

	//TODO replace with ModelTreeAdder to reduce code
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
