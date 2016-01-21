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
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import Cluster.ClusterFileGenerator;
import Cluster.ClusterProcessor;
import Cluster.DistMatrixCalculator;
import Cluster.DistanceMatrix;
import Utilities.StringRes;
import Views.ClusterDialog;
import Views.ClusterView;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.IntHeaderInfo;
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
 * @author CKeil
 *
 */
public class ClusterController {

	/* Axes identifiers */
	public final static int ROW = 1;
	public final static int COL = 2;
	
	private final int ROW_IDX = 0;
	private final int COL_IDX = 1;

	private final DataModel tvModel;
	private final TVController tvController;
	private final ClusterView clusterView;
	private final ClusterDialog clusterDialog;

	/* Delegates the clustering process */
	private ClusterProcessor processor;
	
	private boolean[] overAllClusterStatus;

	/* Initialize with defaults for error checking parameters */
	private int rowSimilarity = DistMatrixCalculator.PEARSON_UN;
	private int colSimilarity = DistMatrixCalculator.PEARSON_UN;

	private SwingWorker<Void, String> clusterTask;
	private SwingWorker<Void, Void> saveTask;

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
	public ClusterController(final ClusterDialog dialog,
			final TVController controller) {

		this.clusterDialog = dialog;
		this.tvController = controller;
		this.tvModel = controller.getDataModel();
		this.clusterView = dialog.getClusterView();
		this.overAllClusterStatus = new boolean[]{false, false};

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
	private class ClusterTask extends SwingWorker<Void, String> {
		
		/* The finished reordered axes */
		private String[] reorderedRows = new String[] {};
		private String[] reorderedCols = new String[] {};

		private String fileName;

		/* Used to set upper limit of cluster progress bar in GUI */
		private int pBarMax = 0;

		@Override
		protected void process(final List<String> chunks) {

			final String s = chunks.get(chunks.size() - 1);
			ClusterView.setStatusText(s);
		}

		@Override
		protected Void doInBackground() throws Exception {

			// Get fileName for saving calculated data
			final int extlen = tvModel.getFileSet().getExt().length();
			
			fileName = tvModel.getSource().substring(0,
					tvModel.getSource().length() - extlen);

			// Initialize the clustering processor and pass the data
			final TVDataMatrix originalMatrix = (TVDataMatrix) tvModel
					.getDataMatrix();
			
			if(isHierarchical()) {
				processor = new ClusterProcessor(originalMatrix, fileName);
				
			} else {
				final IntHeaderInfo rowHeaderI = tvModel.getRowHeaderInfo();
				final IntHeaderInfo colHeaderI = tvModel.getColumnHeaderInfo();
				
				processor = new ClusterProcessor(originalMatrix, fileName,
						rowHeaderI, colHeaderI);
			}

			// Set zeroes invalid if they should be ignored.
			if (clusterView.isIgnoreZeroesChecked()) {
				originalMatrix.setZeroesToMissing();
			}

			final boolean isRowReady = isReady(rowSimilarity, ROW);
			final boolean isColReady = isReady(colSimilarity, COL);
			
			boolean[] clusterCheck = reaffirmClusterChoice(isRowReady, 
					isColReady);
			
			setupClusterViewProgressBar(clusterCheck[ROW_IDX], 
					clusterCheck[COL_IDX]);
			
			if (clusterCheck[ROW_IDX]) {
				reorderedRows = calculateAxis(rowSimilarity, ROW, fileName);
			}

			// Check for cancellation in between axis clustering
			if (isCancelled()) {
				reorderedRows = new String[] {};
				reorderedCols = new String[] {};
				return null;
			}
			
			if (clusterCheck[COL_IDX]) {
				reorderedCols = calculateAxis(colSimilarity, COL, fileName);
			}

			// finished setting reordered axis labels
			return null;
		}

		@Override
		public void done() {

			if (!isCancelled()) {
				saveClusterFile(fileName);

			} else {
				reorderedRows = new String[] {};
				reorderedCols = new String[] {};
				clusterView.setClustering(false);
				LogBuffer.println("Clustering has been cancelled.");
			}
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
			
			// only warn of axis was clustered before AND user wants to cluster
			boolean warnRowAxis = wasRowAxisClustered && rowReady;
			boolean warnColAxis = wasColAxisClustered && colReady;
			
			String message = "Something happened :(";
			if(warnRowAxis && warnColAxis) {
				message = "Both axes have been clustered before. "
						+ "Would you like to cluster your selected axes again?";
				
				if(!confirmChoice(message)) {
					clusterCheck[ROW_IDX] = false;
					clusterCheck[COL_IDX] = false;
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
			
			// final cluster status for later tree file integrity check
			overAllClusterStatus[ROW_IDX] = wasRowAxisClustered 
					|| clusterCheck[ROW_IDX];
			overAllClusterStatus[COL_IDX] = wasColAxisClustered 
					|| clusterCheck[COL_IDX];
			
			return clusterCheck;
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
				cancelAll();
				shouldProceed = false;
				break;
			default:
				shouldProceed = true;
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
			final int cols = tvModel.getColumnHeaderInfo().getNumHeaders();

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
				final String fileName) {
			
			boolean isRow = (axis == ROW);
			
			/* Row axis cluster */
			final DistanceMatrix distMatrix = new DistanceMatrix(0);

			final String axisPrefix = (isRow) ? "row" : "column";

			/* ProgressBar label */
			publish("Calculating " + axisPrefix + " distances...");

			/* Calculating the distance matrix */
			distMatrix.setMatrix(processor.calcDistance(similarity, axis));

			if (isCancelled()) {
				return new String[] {}; // TODO add something sensible to return here...
			}

			publish("Clustering " + axisPrefix + " data...");

			String[] reorderedAxisLabels =  processor.clusterAxis(distMatrix,
					clusterView.getLinkMethod(),
					clusterView.getSpinnerValues(), isHierarchical(), axis);
			
			return reorderedAxisLabels;
		}

		/**
		 * Saves the clustering output (reordered axes) to a new CDT file, so it
		 * can later be loaded and displayed.
		 */
		private void saveClusterFile(final String fileName) {

			if (reorderedRows != null || reorderedCols != null) {
				ClusterView.setStatusText("Saving...");
				saveTask = new SaveTask(reorderedRows, reorderedCols, fileName);
				saveTask.execute();

			} else {
				final String message = "Cannot save. No clustered data was created.";
				JOptionPane.showMessageDialog(Frame.getFrames()[0], message,
						"Error", JOptionPane.ERROR_MESSAGE);
				LogBuffer.println("Alert: " + message);
			}
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
	private class SaveTask extends SwingWorker<Void, Void> {

		/* The finished reordered axes */
		private final String[] reorderedRows;
		private final String[] reorderedCols;

		private final String fileName;
		private String filePath;

		public SaveTask(final String[] reorderedRows,
				final String[] reorderedCols, final String fileName) {

			this.reorderedRows = reorderedRows;
			this.reorderedCols = reorderedCols;
			this.fileName = fileName;
		}

		@Override
		protected Void doInBackground() throws Exception {

			final TVDataMatrix originalMatrix = (TVDataMatrix) tvModel
					.getDataMatrix();
			final double[][] data = originalMatrix.getExprData();

			final ClusterFileGenerator cdtGen = new ClusterFileGenerator(data, 
					reorderedRows, reorderedCols, rowSimilarity, colSimilarity,
					isHierarchical());

			cdtGen.setupWriter(fileName, clusterView.getLinkMethod(),
					clusterView.getSpinnerValues());

			final IntHeaderInfo rowHeaderI = tvModel.getRowHeaderInfo();
			final IntHeaderInfo colHeaderI = tvModel.getColumnHeaderInfo();

			cdtGen.prepare(rowHeaderI, colHeaderI);
			cdtGen.generateCDT();

			filePath = cdtGen.finish();

			return null;
		}

		@Override
		protected void done() {

			if (!isCancelled()) {
				checkTreeFileIntegrity();
				ClusterView.setStatusText("Done!");
				loadClusteredData(filePath);

			} else {
				clusterView.setClustering(false);
				LogBuffer.println("Clustering was cancelled.");
			}
		}
		
		/**
		 * Makes sure that a tree file exists for an axis that is supposed to
		 * be clustered. If not, it attempts to take one from a previous 
		 * cluster and if that does not exist either it will consider an axis
		 * as not clustered.
		 */
		private void checkTreeFileIntegrity() {
	
			LogBuffer.println("Checking tree files...");
			
			// those should really be handled centrally somewhere (final static)
			final String clusterFileSuffix = ".cdt";
			final String rowTreeSuffix = ".gtr";
			final String colTreeSuffix = ".atr";
			
			final int fileRootNameSize = filePath.length() 
					- clusterFileSuffix.length();
			final String newFileRoot = filePath.substring(0, fileRootNameSize);
			
			ensureTreeFilePresence(fileName, newFileRoot, rowTreeSuffix, 
					ROW_IDX);
			ensureTreeFilePresence(fileName, newFileRoot, colTreeSuffix, 
					COL_IDX);
		}
		
		/**
		 * For a given axis, this ensures that it has its tree file in case
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
			if(axisIdx == ROW_IDX) {
				axis_id = "row";
			} else {
				axis_id = "column";
			}
			
			if(overAllClusterStatus[axisIdx]) {
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
		 * Copies an old file to a new one with the correct file name.
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
	 * Sets a new DendroView with the new data loaded into TVModel, displaying
	 * an updated HeatMap. It should also close the ClusterViewFrame.
	 */
	private void loadClusteredData(final String filePath) {

		File file = null;

		if (filePath != null) {
			file = new File(filePath);

			final FileSet fileSet = new FileSet(file.getName(),
					file.getParent() + File.separator);

			clusterDialog.dispose();

			tvController.getDataInfoAndLoad(fileSet, true);
			// DataLoadInfo dataInfo = new DataLoadInfo(new int[]{0,0}, "\\t");
			// // TODO replace with actual values
			// tvController.loadData(fileSet, true, dataInfo);

		} else {
			final String alert = "When trying to load the clustered file, no "
					+ "file path could be found.";
			JOptionPane.showMessageDialog(Frame.getFrames()[0], alert, "Alert",
					JOptionPane.WARNING_MESSAGE);
			LogBuffer.println("Alert: " + alert);
			LogBuffer.println("File path: " + filePath);
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
	 * Listens to a change in selection for the linkChooser JComboBox in
	 * clusterView. Calls a new layout setup as a response.
	 *
	 * @author CKeil
	 *
	 */
	private class LinkChoiceListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			LogBuffer.println("Reprint link method tips.");
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

			LogBuffer.println("Trying to cancel.");
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
	 */
	private void cancelAll() {

		if (clusterTask != null) {
			clusterTask.cancel(true);
		}
		if (processor != null) {
			processor.cancelAll();
		}
		if (saveTask != null) {
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
