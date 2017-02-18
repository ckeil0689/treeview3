package Controllers;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import Cluster.ClusterFileGenerator;
import Cluster.ClusterFileStorage;
import Cluster.ClusterModelTransformator;
import Cluster.ClusterProcessor;
import Cluster.ClusteredAxisData;
import Cluster.DistMatrixCalculator;
import Cluster.DistanceMatrix;
import Utilities.StringRes;
import Views.ClusterDialog;
import Views.ClusterView;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.IntLabelInfo;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;

/**
 * Controls user input from ClusterView. It handles user interactions by
 * implementing listeners which in turn call appropriate methods to respond.
 * When the user starts clustering by clicking the appropriate button in
 * ClusterView, a SwingWorker thread will be created which generates a cascade
 * of multiple threads that take over various background calculations. This
 * prevents the GUI from locking up.
 *
 * TODO Make use of OOP methods to create more lean controllers. Use interfaces
 * and inheritance and avoid passing of the TVFrameController object here, just
 * to use its loading methods. Also inherits instance variables like tvModel..
 *
 */
public class ClusterDialogController {

	/* Axes identifiers */
	public final static String ROW_ID_LABELTYPE = "GID";
	public final static String COL_ID_LABELTYPE = "AID";
	public final static String CDT_END = ".cdt";
	public final static String GTR_END = ".gtr";
	public final static String ATR_END = ".atr";
	
	public final static int ROW = 1;
	public final static int COL = 2;
	
	private final int ROW_IDX = 0;
	private final int COL_IDX = 1;

	private final DataModel tvModel;
	private final TVController tvController;
	private final ClusterView clusterView;
	private final ClusterDialog clusterDialog;
	
	private File cdtFile;
	private File atrFile;
	private File gtrFile;

	/* Delegates the clustering process */
	private ClusterProcessor processor;

	/* Initialize with defaults for error checking parameters */
	private int rowSimilarity = DistMatrixCalculator.PEARSON_UN;
	private int colSimilarity = DistMatrixCalculator.PEARSON_UN;

	private SwingWorker<Boolean, String> clusterTask;
	private SwingWorker<Boolean, Void> saveTask;

	/**
	 * Links the clustering functionality to the user interface. The object
	 * controls what happens in response to user actions. It makes sure that the
	 * right parameters are supplied to clustering methods and controls the UI
	 * response to user interaction.
	 *
	 * @param dialog
	 *            The JDialog that contains the cluster UI.
	 * @param controller
	 *            The TVFrameController, mostly used to enable file loading.
	 */
	public ClusterDialogController(final ClusterDialog dialog,
			final TVController controller) {

		this.clusterDialog = dialog;
		this.tvController = controller;
		this.tvModel = controller.getDataModel();
		this.clusterView = dialog.getClusterView();

		/* Create and add all view component listeners */
		addAllListeners();
	}

	/**
	 * Adds all GUI listeners defined in this controller to ClusterView.
	 */
	private void addAllListeners() {

		if (clusterView != null) {
			clusterView.addClusterListener(new TaskStartListener());
			clusterView.addClusterTypeListener(new ClusterTypeListener());
			clusterView.addCancelListener(new CancelListener());
			clusterView.addLinkageListener(new LinkChoiceListener());
			clusterView.addRowDistListener(new RowDistListener());
			clusterView.addColDistListener(new ColDistListener());
			clusterView.addSpinnerListener(new SpinnerListener());

		} else {
			LogBuffer.println("Cannot add listeners, clusterView is null.");
		}
	}

	public void displayView() {

		clusterDialog.setVisible(true);
	}

	/**
	 * Begins cluster process if the user clicks the 'Cluster' button in
	 * DendroView and sufficient parameters are set.
	 *
	 */
	private class TaskStartListener implements ActionListener {

		// To avoid synthetic compiler creation of a constructor
		protected TaskStartListener(){}
		
		@Override
		public void actionPerformed(final ActionEvent e) {

			/* Only starts with valid selections. */
			if (isReady(rowSimilarity, ROW) || isReady(colSimilarity, COL)) {
				/* Tell ClusterView that clustering begins */
				clusterView.setClustering(true);
				clusterTask = new ClusterTask();
				clusterTask.execute();
			}
		}
	}

	/**
	 * Processes clustering in the background according to user input. Allows
	 * for the UI to remain responsive during these tasks. This is useful for
	 * updating the progress bar and progress label. When the thread is done it
	 * calls a function to save the results to a CDT file.
	 *
	 */
	private class ClusterTask extends SwingWorker<Boolean, String> {
		
		/* The finished reordered axes */
		private ClusteredAxisData rowClusterData;
		private ClusteredAxisData colClusterData;
		
		private boolean[] clusterCheck;

		private String oldFileName;

		/* Used to set upper limit of cluster progress bar in GUI */
		private int pBarMax = 0;
		
		public ClusterTask() {
			
			this.rowClusterData = new ClusteredAxisData(ROW_IDX);
			this.colClusterData = new ClusteredAxisData(COL_IDX);
			
			rowClusterData.setReorderedIDs(getOldIDs(ROW_IDX));
			colClusterData.setReorderedIDs(getOldIDs(COL_IDX));
		}

		@Override
		protected void process(final List<String> chunks) {

			if(isCancelled()) return;
			final String s = chunks.get(chunks.size() - 1);
			ClusterView.setStatusText(s);
		}

		@Override
		protected Boolean doInBackground() throws Exception {

			/* Get fileName for saving calculated data */
			final int extlen = tvModel.getFileSet().getExt().length();
			this.oldFileName = tvModel.getSource().substring(0, tvModel.getSource().length() - extlen);

			/* Initialize the clustering processor and pass the data */
			final TVDataMatrix originalMatrix = (TVDataMatrix) tvModel.getDataMatrix();
			
			/* Initialize the cluster processor */
			if(isHierarchical()) {
				processor = new ClusterProcessor(originalMatrix);
				
			} else {
				final IntLabelInfo rowLabelI = tvModel.getRowLabelInfo();
				final IntLabelInfo colLabelI = tvModel.getColLabelInfo();
				
				processor = new ClusterProcessor(originalMatrix, oldFileName, rowLabelI, colLabelI);
			}

			// Set zeroes invalid if they should be ignored.
			if (clusterView.isIgnoreZeroesChecked()) {
				originalMatrix.setZeroesToMissing();
			}

			final boolean isRowReady = isReady(rowSimilarity, ROW);
			final boolean isColReady = isReady(colSimilarity, COL);
			
			this.clusterCheck = reaffirmClusterChoice(isRowReady, isColReady);
			
			if(!clusterCheck[ROW_IDX] && !clusterCheck[COL_IDX]) {
				this.cancel(true);
				return Boolean.FALSE;
			}
			
			setupClusterViewProgressBar(clusterCheck[ROW_IDX], clusterCheck[COL_IDX]);
			
			// TODO need to add drive partition part to the path when subpath is created
//			final Path clusterFilePath = ClusterFileStorage.createDirectoryStruc(oldFileName, 
//					clusterView.getLinkMethod());
			
			// Cluster rows if user selected option
			if (clusterCheck[ROW_IDX]) {
//				gtrFile = ClusterFileStorage.retrieveFile(clusterFilePath, GTR_END);
				calculateAxis(rowClusterData, rowSimilarity, ROW, gtrFile);
				rowClusterData.shouldReorderAxis(true);
			}

			// Check for cancellation in between axis clustering
			if (isCancelled()) {
				return Boolean.FALSE;
			}
			
			// Cluster columns if user selected option
			if (clusterCheck[COL_IDX]) {
//				atrFile = ClusterFileStorage.retrieveFile(clusterFilePath, ATR_END);
				calculateAxis(colClusterData, colSimilarity, COL, atrFile);
				colClusterData.shouldReorderAxis(true);
			}
			
			if(!isReorderingValid(clusterCheck)) {
				this.cancel(true);
				return Boolean.FALSE;
			}
			
			// TODO move to FileSaver class
			// Determine file extensions for CDT file (varies between hierarchical and k-means)
//			String fileEnd = ClusterFileStorage.determineClusterFileExt(
//					isHierarchical(), clusterView.getSpinnerValues(), 
//					rowClusterData, colClusterData);
//			
//			cdtFile = ClusterFileStorage.retrieveFile(clusterFilePath, fileEnd);
//
//			if(cdtFile == null) {
//				this.cancel(true);
//				return Boolean.FALSE;
//			}
			// finished setting reordered axis labels
			return Boolean.TRUE;
		}

		@Override
		public void done() {

			/* 
			 * Checked again here in case doInBackground() terminates before
			 * first check (not via cancel).
			 */
			if(!isReorderingValid(clusterCheck)) {
				LogBuffer.println("Something occurred during reordering.");
				return;
			}

			ClusterModelTransformator cmt = 
				new ClusterModelTransformator(rowClusterData, colClusterData, 
				                              (TVModel) tvModel);
			tvController.updateModel(cmt.applyClusterChanges(isHierarchical()), true);
			clusterDialog.dispose();
		}
		
		/**
		 * Checks if the arrays of reordered labels are the same size as
		 * the label arrays for each axis.
		 * @return True if reordered arrays are the same size as the axis 
		 * label arrays and the specific axis is supposed to be clustered.
		 */
		private boolean isReorderingValid(boolean[] shouldClusterAxis) {
			
			boolean rowsValid;
			boolean colsValid;
			
			int numRowLabels = tvModel.getRowLabelInfo().getNumLabels();
			int numColLabels = tvModel.getColLabelInfo().getNumLabels();
			
			int numReorderedRowIDs = rowClusterData.getReorderedIDs().length;
			int numReorderedColIDs = colClusterData.getReorderedIDs().length;
			
			if(shouldClusterAxis[ROW_IDX] || tvModel.gidFound()) {
				rowsValid = (numReorderedRowIDs == numRowLabels); 
			} else {
				rowsValid = (numReorderedRowIDs == 0);
			}
			
			if(shouldClusterAxis[COL_IDX] || tvModel.aidFound()) {
				colsValid = (numReorderedColIDs == numColLabels); 
			} else {
				colsValid = (numReorderedColIDs == 0);
			}
			
			return rowsValid && colsValid;
		}
		
		/**
		 * Extracts the old IDs
		 * @param axisID
		 * @return
		 */
		private String[] getOldIDs(final int axisID) {
			
			String[][] labelArray;
			String[] oldIDs; 
			Pattern p;
			int pos = 0;
			
            if(axisID == ROW_IDX) {
            	labelArray = tvModel.getRowLabelInfo().getLabelArray();
            	
            	if(!tvModel.gidFound()) {
            		return new String[]{};
            	}
            	/* Find ID index */
            	p = Pattern.compile("ROW\\d+X");
            	
            } else {
            	labelArray = tvModel.getColLabelInfo().getLabelArray();
            	
            	if(!tvModel.aidFound()) {
            		return new String[]{};
            	}
            	p = Pattern.compile("COL\\d+X");
            }
			
            /* Find ID index */
        	for(int i = 0; i < labelArray[0].length; i++) {
        		Matcher m = p.matcher(labelArray[0][i]);
        		if(m.find()) {
        			pos = i;
        			break;
        		}
        	}
        	
			oldIDs = new String[labelArray.length];
			
			for(int i = 0; i < labelArray.length; i++) {
				oldIDs[i] = labelArray[i][pos];
			}
			
			return oldIDs;
		}
		
		/** 
		 * Determines if both axes should be clustered based on available info 
		 * as well as user input.  
		 * @param shouldClusterRow - Whether all GUI input for row clustering allows for
		 * the row axis to be clustered.
		 * @param shouldClusterCol - Whether all GUI input for column clustering allows 
		 * for the column axis to be clustered.
		 * @return An array of 2 boolean values, each representing whether 
		 * the respective axis should be clustered.
		 */
		private boolean[] reaffirmClusterChoice(final boolean shouldClusterRow, 
				final boolean shouldClusterCol) {
			
			// default: depends on ready status
			boolean[] shouldClusterAxis = new boolean[] {shouldClusterRow, shouldClusterCol};
			
			boolean wasRowAxisClustered = wasAxisClustered(
					tvModel.getFileSet().getGtr(), tvModel.gidFound());
			boolean wasColAxisClustered = wasAxisClustered(
					tvModel.getFileSet().getAtr(), tvModel.aidFound());
			
			// only warn if axis was clustered before AND user wants to cluster
			boolean warnRowAxis = wasRowAxisClustered && shouldClusterRow;
			boolean warnColAxis = wasColAxisClustered && shouldClusterCol;
			
			String message = "Something happened :(";
			if(warnRowAxis && warnColAxis) {
				message = "Both axes have been clustered before. "
						+ "Would you like to cluster your selected axes again?";
				
				if(!confirmChoice(message)) {
					shouldClusterAxis[ROW_IDX] = false;
					shouldClusterAxis[COL_IDX] = false;
					this.cancel(true);
					return shouldClusterAxis;
				}
				
			} else if(warnRowAxis && !warnColAxis) {
				message = "The row axis has been clustered before. "
						+ "Would you like to cluster the rows again?";
				
				shouldClusterAxis[ROW_IDX]= confirmChoice(message);
					
			} else if(!warnRowAxis && warnColAxis){
				message = "The column axis has been clustered before. "
						+ "Would you like to cluster the columns again?";
				
				shouldClusterAxis[COL_IDX]= confirmChoice(message);
			}
			
			/* 
			 * Keeping track of cluster status for both axes here which is
			 * later used to ensure tree file presence if an axis is 
			 * considered to be clustered. 
			 */
			final boolean checkForRowTreeFile = wasRowAxisClustered 
					|| shouldClusterAxis[ROW_IDX];
			final boolean checkForColTreeFile = wasColAxisClustered 
					|| shouldClusterAxis[COL_IDX];
			
			rowClusterData.setAxisClustered(checkForRowTreeFile);
			colClusterData.setAxisClustered(checkForColTreeFile);
			
			return shouldClusterAxis;
		}
		
		/**
		 * Checks if axis was clustered using its tree file if available and 
		 * the axis specific ID if available. 
		 * If neither is present, it will assume that the axis was NOT 
		 * clustered.
		 * @param treeFilePath Path of the axis tree file, if it exists.
		 * @param treeFileSuffix Axis associated tree file suffix (GTR, ATR).
		 * @param hasAxisID When loading a file, a check is performed for the
		 * axis ID label (GID, AID). This can be queried from the TVModel.
		 * @return Whether an axis is considered to have been clustered before.
		 */
		private boolean wasAxisClustered(final String treeFilePath, 
				final boolean hasAxisID) {
			
			boolean hasTreeFile = false;
			
			File f = new File(treeFilePath);
			if(f.exists() && !f.isDirectory()) { 
			    hasTreeFile = true;
			}
			
			return hasAxisID || hasTreeFile;
		}
		
		/**
		 * Opens a dialog to confirm user choice about clustering.
		 * @param message The text to be displayed in the dialog (depending on
		 * which axes have been clustered before).
		 * @return Boolean confirming whether to cluster or not.
		 */
		private boolean confirmChoice(final String message) {
			
			boolean shouldProceed = false;
			
			final int choice = JOptionPane.showConfirmDialog(
					clusterDialog, message, "Select an Option", JOptionPane.YES_NO_OPTION);
			
			switch(choice) {
			case JOptionPane.YES_OPTION:
				shouldProceed = true;
				break;
			case JOptionPane.NO_OPTION:
				shouldProceed = false;
				break;
			case JOptionPane.CANCEL_OPTION:
				this.cancel(true);
				shouldProceed = false;
				break;
			default:
				shouldProceed = false;
			}
			
			return shouldProceed;
		}
		
		/**
		 * In order to show accurate progress information, the JProgressBar in
		 * ClusterView needs to know some information about the data. This
		 * method calculates how much data has to be processed based on which 
		 * axes are clustered as well as the number of labels on each axis.
		 */
		private void setupClusterViewProgressBar(final boolean clusterRows, 
				final boolean clusterCols) {
			
			final int rows = tvModel.getRowLabelInfo().getNumLabels();
			final int cols = tvModel.getColLabelInfo().getNumLabels();

			/*
			 * Set maximum for JProgressBar before any clustering!
			 */
			if (clusterRows) {
				if (isHierarchical()) {
					/* Check if should be ranked first or not. */
					pBarMax += (rowSimilarity == 5) ? (3 * rows) : (2 * rows);

				} else {
					final int cycles = (clusterView.getSpinnerValues()[1]).intValue();
					if (rowSimilarity == 5) {
						pBarMax += 2 * rows + cycles;

					} else {
						pBarMax += rows + cycles;
					}
				}
			}

			if (clusterCols) {
				if (isHierarchical()) {
					/* Check if should be ranked first or not. */
					pBarMax += (colSimilarity == 5) ? (3 * cols) : (2 * cols);

				} else {
					final int cycles = (clusterView.getSpinnerValues()[3]).intValue();

					if (colSimilarity == 5) {
						pBarMax += 2 * cols + cycles;

					} else {
						pBarMax += cols + cycles;
					}
				}
			}

			ClusterView.setPBarMax(pBarMax);
		}

		/**
		 * Controls clustering procedures for one axis.
		 *
		 * @param similarity
		 *            The chosen similarity measure.
		 * @param axis
		 *            The chosen matrix axis.
		 * @return A list of reordered axis elements.
		 */
		private void calculateAxis(final ClusteredAxisData cad, 
		                               final int similarity, final int axis,
		                               final File treeFile) {
			
			boolean isRow = (axis == ROW);
			
			/* Row axis cluster */
			final DistanceMatrix distMatrix = new DistanceMatrix(0);
			final String axisType = (isRow) ? "row" : "column";

			/* ProgressBar label */
			publish("Calculating " + axisType + " distances...");

			/* Calculating the distance matrix */
			distMatrix.setMatrix(processor.calcDistance(similarity, axis));

			if (isCancelled()) {
				return;
			}

			publish("Clustering " + axisType + " data...");

			processor.clusterAxis(distMatrix, cad, clusterView.getLinkMethod(),
					clusterView.getSpinnerValues(), isHierarchical(), axis, treeFile);
		}
	}
	
	/**
	 * Saves the clustering output (reordered axes) to a new CDT file, so it
	 * can later be loaded and displayed.
	 */
	private void saveClusterFile(final String fileName, 
			final ClusteredAxisData rowClusterData, 
			final ClusteredAxisData colClusterData) {

		if (rowClusterData.getReorderedIDs() != null 
				|| colClusterData.getReorderedIDs() != null) {
			ClusterView.setStatusText("Saving...");
			
			saveTask = new SaveTask(rowClusterData, colClusterData, 
					fileName);
			saveTask.execute();

		} else {
			final String message = "Cannot save. No clustered data "
					+ "was created.";
			JOptionPane.showMessageDialog(Frame.getFrames()[0], message,
					"Error", JOptionPane.ERROR_MESSAGE);
			LogBuffer.println("Alert: " + message);
		}
	}

	/**
	 * Worker with the task to generate and write a new CDT file which contains
	 * the newly clustered matrix. Makes sure that the newly created file is
	 * visualized right after clustering, given that the process was not
	 * cancelled.
	 *
	 * @param rowClusterData - row clustering data relevant for CDT save file
	 * @param colClusterData - column clustering data relevant for CDT save file
	 * @param fileName - name of the CDT file to be written
	 */
	private class SaveTask extends SwingWorker<Boolean, Void> {

		/* The finished reordered axes */
		private final ClusteredAxisData rowClusterData;
		private final ClusteredAxisData colClusterData;

		private final String fileName;
		private String filePath;

		public SaveTask(final ClusteredAxisData rowClusterData, 
				final ClusteredAxisData colClusterData, 
				final String fileName) {

			this.rowClusterData = rowClusterData;
			this.colClusterData = colClusterData;
			
			this.fileName = fileName;
		}

		@Override
		protected Boolean doInBackground() throws Exception {

			final TVDataMatrix originalMatrix = (TVDataMatrix) tvModel
					.getDataMatrix();
			final double[][] data = originalMatrix.getExprData();

			final ClusterFileGenerator cdtGen = new ClusterFileGenerator(data, 
					rowClusterData, colClusterData, isHierarchical());

			cdtGen.setupWriter(cdtFile);
			cdtGen.generateCDT();

			filePath = cdtGen.finish();
			
			if(isCancelled()) {
				return Boolean.FALSE;
			}
			
			if(filePath == null) {
				LogBuffer.println("Generating a CDT failed. Cancelling...");
				this.cancel(true);
				return Boolean.FALSE;
			}

			return Boolean.TRUE;
		}

		@Override
		protected void done() {

			if (!isCancelled() && hasEnsuredTreeFilePresence()) {
				ClusterView.setStatusText("Saving done!");
				loadClusteredData(filePath);
				LogBuffer.println("SaveTask is done: success.");
				
			} else {
				clusterView.setClustering(false);
				LogBuffer.println("Saving did not finish successfully.");
				deleteAllFiles();
			}
		}
		
		/**
		 * Makes sure that a tree file exists for an axis that is supposed to
		 * be clustered. If not, it attempts to take one from a previous 
		 * cluster and if that does not exist either it will consider an axis
		 * as not clustered. In that case, a tree file will not be present. Returns
		 * true upon successful completion.
		 */
		private boolean hasEnsuredTreeFilePresence() {
	
			if(filePath == null || fileName == null) {
				return false;
			}
			
			final int fileRootNameSize = filePath.length() - CDT_END.length();
			final String newFileRoot = filePath.substring(0, fileRootNameSize);
			
			ensureTreeFilePresence(fileName, newFileRoot, GTR_END, ROW_IDX);
			ensureTreeFilePresence(fileName, newFileRoot, ATR_END, COL_IDX);
			
			return true;
		}
		
		/**
		 * For a given axis, this ensures that its tree file exists in case
		 * it is considered to be clustered. This is useful especially if
		 * an axis was already clustered before so that the old tree file can
		 * be carried over to the new FileSet.
		 * @param oldFileRoot The root name of the older cdt file.
		 * @param newFileRoot The root name of the new cdt file.
		 * @param treeFileSuffix The tree file suffix to be used if a new file
		 * is created.
		 * @param axisIdx An integer identifying the axis.
		 */
		private void ensureTreeFilePresence(final String oldFileRoot, 
				final String newFileRoot, final String treeFileSuffix, 
				final int axisIdx) { 
			
			String axis_id;
			boolean axisNeedsTreeFileCheck;
			
			if(axisIdx == ROW_IDX) {
				axis_id = "row";
				axisNeedsTreeFileCheck = rowClusterData.isAxisClustered();
			} else {
				axis_id = "column";
				axisNeedsTreeFileCheck = colClusterData.isAxisClustered();
			}
			
			if(axisNeedsTreeFileCheck) {
				String newTreeFilePath = newFileRoot + treeFileSuffix;
				String oldTreeFilePath = oldFileRoot + treeFileSuffix;
				
				if(!doesFileExist(newTreeFilePath)) {
					LogBuffer.println("No file found for " + axis_id 
							+ " trees.");
					if(doesFileExist(oldTreeFilePath)) {
						LogBuffer.println("But old " + axis_id 
								+ " tree file was found!");
						copyFile(oldTreeFilePath, newTreeFilePath);
					} else {
						String message = "The tree file for the " + axis_id 
								+ " axis could not be recovered. No trees "
								+ "can be shown.";
						JOptionPane.showMessageDialog(clusterDialog, message);
					}
				} else {
					LogBuffer.println("Success! The " + axis_id 
							+ " tree file was found.");
				}
			} else {
				LogBuffer.println("The " + axis_id 
						+ "s have not been clustered.");
			}
		}
		
		/**
		 * Copies an old file to a new one with the correct file name. This 
		 * is used for transferring old tree files to new clustered matrices.
		 * @param oldTreeFilePath The path of the file to be copied.
		 * @param newTreeFilePath The path to which the old file will be copied.
		 */
		private boolean copyFile(final String oldTreeFilePath, 
				final String newTreeFilePath) {
			
			try(FileInputStream srcStream = 
					new FileInputStream(oldTreeFilePath); 
				FileOutputStream dstStream = 
						new FileOutputStream(newTreeFilePath)) {
				
				dstStream.getChannel().transferFrom(srcStream.getChannel(), 
						0, srcStream.getChannel().size());
				return true;
				
			} catch (IOException e) {
				LogBuffer.logException(e);
				return false;
			} 
		}
		
		/**
		 * Checks if a file at a given path exists or not.
		 * @param path - The complete file path which to check.
		 * @return Whether the checked file exists or not.
		 */
		private boolean doesFileExist(final String path) {
			
			File f = new File(path);
			return (f.exists() && !f.isDirectory());
		}
	}
	
	/**
	 * Deletes all files associated with the last clustering step. Also
	 * deletes the directory of the files if it is empty.
	 */
	public void deleteAllFiles() {
		
		File dir = getClusterDir(cdtFile, gtrFile, atrFile);
		
		deleteFile(cdtFile);
		deleteFile(atrFile);
		deleteFile(gtrFile);
		
		deleteEmptyDir(dir);
	}
	
	/**
	 * TODO move to ClusterFileStorage
	 * Attempt to extract the directory from any of the cluster files, if 
	 * they exist.
	 * @param cdtF The CDT file of the current cluster operation.
	 * @param gtrF The GTR file of the current cluster operation.
	 * @param atrF The ATR file of the current cluster operation.
	 * @return File The directory where the files are stored or null if neither
	 * of the files exists. 
	 */
	private static File getClusterDir(File cdtF, File gtrF, File atrF) {
		
		File dir;
		if(cdtF != null && cdtF.exists()) {
			dir = cdtF.getParentFile();
			
		} else if(gtrF != null && gtrF.exists()) {
			dir = gtrF.getParentFile();
			
		} else if(atrF != null && atrF.exists()) {
			dir = atrF.getParentFile();
			
		} else {
			dir = null;
		}
		
		if(dir != null) {
			LogBuffer.println("Determined dir: " + dir.getAbsolutePath());
		}
		
		return dir;
	}
	
	/**
	 * TODO move to ClusterFileStorage
	 * If the passed File object exists and is indeed a normal file, it deletion
	 * will be attempted. The passed object will also be set to null to avoid
	 * lingering of object data.
	 * @param file - The File to be deleted.
	 */
	private static void deleteFile(File file) {
		
		if(file == null) {
			return;
		}
		
		boolean success = false;
		String name = file.getName();
		
		if(file.isFile() && file.exists()) {
			success = file.delete();
			LogBuffer.println("Attempted delete of " + name);
			
		} else {
			LogBuffer.println(name + " is not a file or file does not exist.");
		}
		
		if(success) {
			LogBuffer.println(name + " was successfully deleted.");
			//file = null;
			// got a warning for this assignment, not sure what effects of deletion would be. can be deleted if
			// process is not affected
			
		} else {
			LogBuffer.println(name + " could not be deleted.");
		}
	}
	
	/**
	 * TODO Move to ClusterFileStorage
	 * Checks if the passed File object is a directory, if it is empty, and if
	 * that is true it attempts to delete the directory.
	 * @param dir - The File object to be deleted. It should represent 
	 * an empty directory.
	 */
	private static void deleteEmptyDir(File dir) {
		
		if(dir == null) {
			return;
		}
		
		boolean success = false;
		String name = dir.getName();
		
		if(dir.isDirectory()) {
			File[] files = dir.listFiles();
			if(files.length == 0) {
				success = dir.delete();
				
			} else {
				LogBuffer.println("Directory " + name + " still has " 
						+ files.length + " files.");
			}
		} else {
			LogBuffer.println(name + " is not a directory.");
		}
		
		if(success) {
			LogBuffer.println(name + " was successfully deleted.");
			//dir = null;
			// got a warning for this assignment, not sure what effects of deletion would be. can be deleted if
			// process is not affected
		} else {
			LogBuffer.println(name + " could not be deleted.");
		}
	}

	/**
	 * @deprecated
	 * Sets a new <code>DendroView</code> with the new data loaded into 
	 * <code>TVModel</code>, displaying an updated heat map. 
	 * It should also close the <code>ClusterDialog</code>.
	 * @param clusteredFilePath - The path to the clustered file which should
	 * be loaded
	 */
	private void loadClusteredData(final String clusteredFilePath) {

		File file = null;
		
		if (clusteredFilePath != null) {
			file = new File(clusteredFilePath);
			
			// Later used to import preferences
			final String oldRoot = tvModel.getFileSet().getRoot();
			final String oldExt = tvModel.getFileSet().getExt();
			
			final FileSet clusteredFileSet = new FileSet(file.getName(),
					file.getParent() + File.separator);
			
			clusterDialog.dispose();
			tvController.getDataInfoAndLoad(clusteredFileSet, oldRoot, oldExt, true, false);

		} else {
			final String alert = "When trying to load the clustered file, no "
					+ "file path could be found.";
			JOptionPane.showMessageDialog(Frame.getFrames()[0], alert, "Alert",
					JOptionPane.WARNING_MESSAGE);
			LogBuffer.println("Alert: " + alert);
		}
	}

	/* -------------------- Listeners ------------------------------ */
	/**
	 * Listens to a change in selection for the clusterChoice JComboBox in
	 * clusterView. Calls a new layout setup as a response.
	 */
	private class ClusterTypeListener implements ActionListener {

		// To avoid synthetic compiler creation of a constructor
		protected ClusterTypeListener(){}
		
		// source of arg0 is a JComboBox<String> in ClusterView
		@SuppressWarnings("unchecked")
		@Override
		public void actionPerformed(final ActionEvent arg0) {

			LogBuffer.println("Reset cluster type.");
			clusterDialog.reset(((JComboBox<String>) arg0.getSource())
					.getSelectedIndex());
			addAllListeners();
		}
	}

	/**
	 * Listens to a change in selection for the JComboBox linkChooser in
	 * the ClusterDialog. Calls a new layout setup as a response.
	 *
	 */
	private class LinkChoiceListener implements ActionListener {

		// To avoid synthetic compiler creation of a constructor
		protected LinkChoiceListener(){}
		
		@Override
		public void actionPerformed(final ActionEvent arg0) {

			clusterView.setupLayout();
		}
	}

	/**
	 * Listens to a change in selection in the JComboBox for row distance
	 * measure selection.
	 */
	private class RowDistListener implements ItemListener {

		// To avoid synthetic compiler creation of a constructor
		protected RowDistListener(){}
		
		@Override
		public void itemStateChanged(final ItemEvent event) {

			if (event.getStateChange() == ItemEvent.SELECTED) {
				rowSimilarity = clusterView.getRowSimilarity();

				/* Ready indicator label */
				clusterView.displayReadyStatus(isReady(rowSimilarity, ROW)
						|| isReady(colSimilarity, COL));
			}
		}
	}

	/**
	 * Listens to a change in selection in the JComboBox for col distance
	 * measure selection.
	 */
	private class ColDistListener implements ItemListener {

		// To avoid synthetic compiler creation of a constructor
		protected ColDistListener(){}
		
		@Override
		public void itemStateChanged(final ItemEvent event) {

			if (event.getStateChange() == ItemEvent.SELECTED) {
				colSimilarity = clusterView.getColSimilarity();

				/* OR controlled ready indicator label */
				final boolean rowReady = isReady(rowSimilarity, ROW);
				final boolean colReady = isReady(colSimilarity, COL);

				clusterView.displayReadyStatus(rowReady || colReady);
			}
		}
	}

	/**
	 * Listens to a change in selection in the JSpinners for k-means.
	 */
	private class SpinnerListener implements ChangeListener {

		// To avoid synthetic compiler creation of a constructor
		protected SpinnerListener(){}
		
		@Override
		public void stateChanged(final ChangeEvent arg0) {

			/* OR controlled ready indicator label */
			final boolean rowReady = isReady(rowSimilarity, ROW);
			final boolean colReady = isReady(colSimilarity, COL);

			clusterView.displayReadyStatus(rowReady || colReady);
		}
	}

	/**
	 * Defines what happens if the user clicks the 'Cancel' button in
	 * DendroView. Calls the cancel() method in the view. TODO add cancel
	 * functionality to distance worker and cluster worker.
	 */
	private class CancelListener implements ActionListener {

		protected CancelListener(){}
		
		@Override
		public void actionPerformed(final ActionEvent e) {

			LogBuffer.println("Cancelling...");
			cancelAll();
		}
	}

	/* ------------------ Helper methods -------------------- */
	/**
	 * Verifies whether all needed options are selected to perform clustering.
	 *
	 * @param distMeasure
	 *            Selected distance measure.
	 * @return boolean Whether all needed selections have appropriate values.
	 */
	private boolean isReady(final int distMeasure, final int type) {

		if (isHierarchical()) {
			return distMeasure != DistMatrixCalculator.NO_CLUSTER;
		}

		final Integer[] spinnerValues = clusterView.getSpinnerValues();

		int groups;
		int iterations;

		switch (type) {

		case ROW:
			groups = (spinnerValues[0]).intValue();
			iterations = (spinnerValues[1]).intValue();
			break;
		case COL:
			groups = (spinnerValues[2]).intValue();
			iterations = (spinnerValues[3]).intValue();
			break;
		default:
			groups = 0;
			iterations = 0;
			break;
		}

		return (distMeasure != DistMatrixCalculator.NO_CLUSTER 
				&& (groups > 0 && iterations > 0));

	}

	/**
	 * Cancels all active threads related to clustering.
	 * Attention: Swingworker.cancel(true/false) immediately calls done(). 
	 * This causes isDone() to be true before doInBackground() actually 
	 * finishes.
	 */
	private void cancelAll() {
		
		if (processor != null) {
			LogBuffer.println("Cancelling processor tasks...");
			processor.cancelAll();
		}

		if (clusterTask != null) {
			LogBuffer.println("Cancelling cluster task...");
			clusterTask.cancel(true);
		}
		
		if (saveTask != null) {
			LogBuffer.println("Cancelling save task...");
			saveTask.cancel(true);
		}
	}

	/**
	 * Returns whether hierarchical clustering is currently selected or not.
	 *
	 * @return boolean Whether the user selected hierarchical clustering (true)
	 *         or k-means (false).
	 */
	private boolean isHierarchical() {

		return clusterView.getClusterMethod().equalsIgnoreCase(
				StringRes.menu_Hier);
	}
}
