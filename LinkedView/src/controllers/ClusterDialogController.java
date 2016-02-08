package controllers;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cluster.ClusterFileGenerator;
import cluster.ClusterFileStorage;
import cluster.ClusterProcessor;
import cluster.ClusteredAxisData;
import cluster.DistMatrixCalculator;
import cluster.DistanceMatrix;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.IntHeaderInfo;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;
import utilities.StringRes;
import views.ClusterDialog;
import views.ClusterView;

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
 * @author CKeil
 *
 */
public class ClusterDialogController {

	/* Axes identifiers */
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
	 * @author CKeil
	 *
	 */
	private class TaskStartListener implements ActionListener {

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
	 * @author CKeil
	 *
	 */
	private class ClusterTask extends SwingWorker<Boolean, String> {
		
		/* The finished reordered axes */
		private ClusteredAxisData rowClusterData;
		private ClusteredAxisData colClusterData;

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
			final ClusterFileStorage clusStore = new ClusterFileStorage();
			final int extlen = tvModel.getFileSet().getExt().length();
			
			this.oldFileName = tvModel.getSource().substring(0,
					tvModel.getSource().length() - extlen);

			/* Initialize the clustering processor and pass the data */
			final TVDataMatrix originalMatrix = (TVDataMatrix) tvModel
					.getDataMatrix();
			
			/* Initialize the cluster processor */
			if(isHierarchical()) {
				processor = new ClusterProcessor(originalMatrix);
				
			} else {
				final IntHeaderInfo rowHeaderI = tvModel.getRowHeaderInfo();
				final IntHeaderInfo colHeaderI = tvModel.getColHeaderInfo();
				
				processor = new ClusterProcessor(originalMatrix, oldFileName,
						rowHeaderI, colHeaderI);
			}

			/* Set zeroes invalid if they should be ignored. */
			if (clusterView.isIgnoreZeroesChecked()) {
				originalMatrix.setZeroesToMissing();
			}

			final boolean isRowReady = isReady(rowSimilarity, ROW);
			final boolean isColReady = isReady(colSimilarity, COL);
			
			boolean[] clusterCheck = reaffirmClusterChoice(isRowReady, 
					isColReady);
			
			if(!clusterCheck[ROW_IDX] && !clusterCheck[COL_IDX]) {
				this.cancel(true);
				return false;
			}
			
			setupClusterViewProgressBar(clusterCheck[ROW_IDX], 
					clusterCheck[COL_IDX]);
			
			/* Cluster rows */
			if (clusterCheck[ROW_IDX]) {
				gtrFile = clusStore.createFile(oldFileName, GTR_END, 
						clusterView.getLinkMethod());
				rowClusterData.setReorderedIDs(
						calculateAxis(rowSimilarity, ROW, gtrFile));
				rowClusterData.shouldReorderAxis(true);
			}

			// Check for cancellation in between axis clustering
			if (isCancelled()) {
				return false;
			}
			
			/* Cluster columns */
			if (clusterCheck[COL_IDX]) {
				atrFile = clusStore.createFile(oldFileName, ATR_END, 
						clusterView.getLinkMethod());
				colClusterData.setReorderedIDs(
						calculateAxis(colSimilarity, COL, atrFile));
				colClusterData.shouldReorderAxis(true);
			}
			
			if(!isReorderingValid(clusterCheck)) {
				this.cancel(true);
				return false;
			}
			
			/* If all went smooth, create File object for CDT file */
			String fileEnd = clusStore.getClusterFileExtension(
					isHierarchical(), clusterView.getSpinnerValues(), 
					rowClusterData, colClusterData);
			
			cdtFile = clusStore.createFile(oldFileName, fileEnd, 
					clusterView.getLinkMethod());

			if(cdtFile == null) {
				this.cancel(true);
				return false;
			}
			// finished setting reordered axis labels
			return true;
		}

		@Override
		public void done() {

			boolean shouldSave = true;
			if(rowClusterData.getReorderedIDs().length == 0 
					|| colClusterData.getReorderedIDs().length == 0) {
				LogBuffer.println("Something occurred during reordering.");
				shouldSave = false;
			}
			
			if (!isCancelled() && shouldSave) {
				saveClusterFile(oldFileName, rowClusterData, colClusterData);
				LogBuffer.println("ClusterTask is done: success.");

			} else {
				rowClusterData.setReorderedIDs(new String[] {});
				colClusterData.setReorderedIDs(new String[] {});
				clusterView.setClustering(false);
				LogBuffer.println("ClusterTask is done: cancelled.");
				deleteAllFiles();
			}
		}
		
		/**
		 * Checks if the arrays of reordered labels are the same size as
		 * the header arrays for each axis.
		 * @return True if reordered arrays are the same size as the axis 
		 * header arrays and the specific axis is supposed to be clustered.
		 */
		private boolean isReorderingValid(boolean[] clusterCheck) {
			
			boolean rowsValid;
			boolean colsValid;
			
			int numRowHeaders = tvModel.getRowHeaderInfo().getNumHeaders();
			int numColHeaders = tvModel.getColHeaderInfo().getNumHeaders();
			
			int numReorderedRowIDs = rowClusterData.getReorderedIDs().length;
			int numReorderedColIDs = colClusterData.getReorderedIDs().length;
			
			if(clusterCheck[ROW_IDX] || tvModel.gidFound()) {
				rowsValid = (numReorderedRowIDs == numRowHeaders); 
			} else {
				rowsValid = (numReorderedRowIDs == 0);
			}
			
			if(clusterCheck[COL_IDX] || tvModel.aidFound()) {
				colsValid = (numReorderedColIDs == numColHeaders); 
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
			
			String[][] headerArray;
			String[] oldIDs; 
			Pattern p;
			int pos = 0;
			
            if(axisID == ROW_IDX) {
            	headerArray = tvModel.getRowHeaderInfo().getHeaderArray();
            	
            	if(!tvModel.gidFound()) {
            		return new String[]{};
            	}
            	/* Find ID index */
            	p = Pattern.compile("ROW\\d+X");
            	
            } else {
            	headerArray = tvModel.getColHeaderInfo().getHeaderArray();
            	
            	if(!tvModel.aidFound()) {
            		return new String[]{};
            	}
            	p = Pattern.compile("COL\\d+X");
            }
			
            /* Find ID index */
        	for(int i = 0; i < headerArray[0].length; i++) {
        		Matcher m = p.matcher(headerArray[0][i]);
        		if(m.find()) {
        			pos = i;
        			break;
        		}
        	}
        	
			oldIDs = new String[headerArray.length];
			
			for(int i = 0; i < headerArray.length; i++) {
				oldIDs[i] = headerArray[i][pos];
			}
			
			return oldIDs;
		}
		
		/** 
		 * Determines if both axes should be clustered based on available info 
		 * as well as user input.  
		 * @param rowReady Whether all GUI input for row clustering allows for
		 * the row axis to be clustered.
		 * @param colReady Whether all GUI input for column clustering allows 
		 * for the column axis to be clustered.
		 * @return An array of 2 boolean values, each representing whether 
		 * the respective axis should be clustered.
		 */
		private boolean[] reaffirmClusterChoice(final boolean rowReady, 
				final boolean colReady) {
			
			// default: depends on ready status
			boolean[] clusterCheck = new boolean[] {rowReady, colReady};
			
			boolean wasRowAxisClustered = wasAxisClustered(
					tvModel.getFileSet().getGtr(), tvModel.gidFound());
			boolean wasColAxisClustered = wasAxisClustered(
					tvModel.getFileSet().getAtr(), tvModel.aidFound());
			
			// only warn if axis was clustered before AND user wants to cluster
			boolean warnRowAxis = wasRowAxisClustered && rowReady;
			boolean warnColAxis = wasColAxisClustered && colReady;
			
			String message = "Something happened :(";
			if(warnRowAxis && warnColAxis) {
				message = "Both axes have been clustered before. "
						+ "Would you like to cluster your selected axes again?";
				
				if(!confirmChoice(message)) {
					clusterCheck[ROW_IDX] = false;
					clusterCheck[COL_IDX] = false;
					this.cancel(true);
					return clusterCheck;
				}
				
			} else if(warnRowAxis && !warnColAxis) {
				message = "The row axis has been clustered before. "
						+ "Would you like to cluster the rows again?";
				
				clusterCheck[ROW_IDX]= confirmChoice(message);
					
			} else if(!warnRowAxis && warnColAxis){
				message = "The column axis has been clustered before. "
						+ "Would you like to cluster the columns again?";
				
				clusterCheck[COL_IDX]= confirmChoice(message);
			}
			
			/* 
			 * Keeping track of cluster status for both axes here which is
			 * later used to ensure tree file presence if an axis is 
			 * considered to be clustered. 
			 */
			final boolean checkForRowTreeFile = wasRowAxisClustered 
					|| clusterCheck[ROW_IDX];
			final boolean checkForColTreeFile = wasColAxisClustered 
					|| clusterCheck[COL_IDX];
			
			rowClusterData.setAxisClustered(checkForRowTreeFile);
			colClusterData.setAxisClustered(checkForColTreeFile);
			
			return clusterCheck;
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
					clusterDialog, message);
			
			switch(choice) {
			case JOptionPane.OK_OPTION:
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
			
			final int rows = tvModel.getRowHeaderInfo().getNumHeaders();
			final int cols = tvModel.getColHeaderInfo().getNumHeaders();

			/*
			 * Set maximum for JProgressBar before any clustering!
			 */
			if (clusterRows) {
				if (isHierarchical()) {
					/* Check if should be ranked first or not. */
					pBarMax += (rowSimilarity == 5) ? (3 * rows) : (2 * rows);

				} else {
					final int cycles = clusterView.getSpinnerValues()[1];
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
					final int cycles = clusterView.getSpinnerValues()[3];

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
		private String[] calculateAxis(final int similarity, final int axis,
				final File treeFile) {
			
			boolean isRow = (axis == ROW);
			
			/* Row axis cluster */
			final DistanceMatrix distMatrix = new DistanceMatrix(0);
			final String axisPrefix = (isRow) ? "row" : "column";

			/* ProgressBar label */
			publish("Calculating " + axisPrefix + " distances...");

			/* Calculating the distance matrix */
			distMatrix.setMatrix(processor.calcDistance(similarity, axis));

			if (isCancelled()) {
				return new String[] {};
			}

			publish("Clustering " + axisPrefix + " data...");

			String[] reorderedAxisLabels =  processor.clusterAxis(distMatrix,
					clusterView.getLinkMethod(),
					clusterView.getSpinnerValues(), isHierarchical(), axis, 
					treeFile);
			
			return reorderedAxisLabels;
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
	 * @param reorderedRows
	 *            Reordered row axis.
	 * @author CKeil
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

			final IntHeaderInfo rowHeaderI = tvModel.getRowHeaderInfo();
			final IntHeaderInfo colHeaderI = tvModel.getColHeaderInfo();

			cdtGen.prepare(rowHeaderI, colHeaderI);
			cdtGen.generateCDT();

			filePath = cdtGen.finish();
			
			if(isCancelled()) {
				return false;
			}
			
			if(filePath == null) {
				LogBuffer.println("Generating a CDT failed. Cancelling...");
				this.cancel(true);
				return false;
			}

			return true;
		}

		@Override
		protected void done() {

			if (!isCancelled() && checkTreeFileIntegrity()) {
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
		 * as not clustered.
		 */
		private boolean checkTreeFileIntegrity() {
	
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
		 * @param filePath The complete file path which to check.
		 * @return Whether the checked file exists or not.
		 */
		private boolean doesFileExist(final String filePath) {
			
			File f = new File(filePath);
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
		
		deleteDir(dir);
	}
	
	/**
	 * Attempt to extract the directory from any of the cluster files, if 
	 * they exist.
	 * @param cdtF The CDT file of the current cluster operation.
	 * @param gtrF The GTR file of the current cluster operation.
	 * @param atrF The ATR file of the current cluster operation.
	 * @return File The directory where the files are stored or null if neither
	 * of the files exists. 
	 */
	private File getClusterDir(File cdtF, File gtrF, File atrF) {
		
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
	
	private void deleteFile(File file) {
		
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
		} else {
			LogBuffer.println(name + " could not be deleted.");
		}
	}
	
	private void deleteDir(File dir) {
		
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
		} else {
			LogBuffer.println(name + " could not be deleted.");
		}
	}

	/**
	 * Sets a new DendroView with the new data loaded into TVModel, displaying
	 * an updated HeatMap. It should also close the ClusterViewFrame.
	 */
	private void loadClusteredData(final String newFilePath) {

		File file = null;
		
		if (newFilePath != null) {
			file = new File(newFilePath);

			LogBuffer.println("TVModel in loadClustered 2"  + tvModel.getFileSet());
			
			// TODO instantiating a new local fileSet soemhow changes the fileset of TVModel... (???)
			final FileSet newFileSet = new FileSet(file.getName(),
					file.getParent() + File.separator);

			LogBuffer.println("TVModel in loadClustered 3"  + tvModel.getFileSet());
			
			clusterDialog.dispose();
			tvController.getDataInfoAndLoad(newFileSet, true);

		} else {
			final String alert = "When trying to load the clustered file, no "
					+ "file path could be found.";
			JOptionPane.showMessageDialog(Frame.getFrames()[0], alert, "Alert",
					JOptionPane.WARNING_MESSAGE);
			LogBuffer.println("Alert: " + alert);
			LogBuffer.println("File path: " + newFilePath);
		}
	}

	/* -------------------- Listeners ------------------------------ */
	/**
	 * Listens to a change in selection for the clusterChoice JComboBox in
	 * clusterView. Calls a new layout setup as a response.
	 *
	 * @author CKeil
	 *
	 */
	private class ClusterTypeListener implements ActionListener {

		/* source of arg0 is a JComboBox<String> in ClusterView */
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
	 * @author CKeil
	 *
	 */
	private class LinkChoiceListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			clusterView.setupLayout();
		}
	}

	/**
	 * Listens to a change in selection in the JComboBox for row distance
	 * measure selection.
	 *
	 * @author CKeil
	 */
	private class RowDistListener implements ItemListener {

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
	 *
	 * @author CKeil
	 */
	private class ColDistListener implements ItemListener {

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
	 *
	 * @author CKeil
	 */
	private class SpinnerListener implements ChangeListener {

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
	 *
	 * @author CKeil
	 *
	 */
	private class CancelListener implements ActionListener {

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
			groups = spinnerValues[0];
			iterations = spinnerValues[1];
			break;
		case COL:
			groups = spinnerValues[2];
			iterations = spinnerValues[3];
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

//		deleteAllFiles();
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
