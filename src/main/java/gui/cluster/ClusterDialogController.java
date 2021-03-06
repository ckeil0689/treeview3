package gui.cluster;

import gui.window.TVController;
import model.data.cluster.ClusterModelTransformator;
import model.data.cluster.ClusterProcessor;
import model.data.cluster.ClusteredAxisData;
import model.data.cluster.DistMatrixCalculator;
import model.data.cluster.DistanceMatrix;
import model.data.labels.IntLabelInfo;
import model.data.matrix.DataModel;
import model.data.matrix.TVModel;
import model.data.matrix.TVModel.TVDataMatrix;
import org.apache.commons.io.FilenameUtils;
import util.LogBuffer;
import util.StringRes;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.List;

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

	// Axes identifiers
	public final static String ROW_ID_LABELTYPE = "GID";
	public final static String COL_ID_LABELTYPE = "AID";
	
	public final static int ROW_IDX = 0;
	public final static int COL_IDX = 1;

	private final DataModel tvModel;
	private final TVController tvController;
	private final ClusterView clusterView;
	private final ClusterDialog clusterDialog;

	// Delegates the clustering process
	private ClusterProcessor processor;

	/* Initialize with defaults for error checking parameters */
	private int rowSimilarity = DistMatrixCalculator.PEARSON_UN;
	private int colSimilarity = DistMatrixCalculator.PEARSON_UN;

	private SwingWorker<Boolean, String> clusterTask;

	/**
	 * Links the clustering functionality to the user interface. The object
	 * controls what happens in response to user actions. It makes sure that the
	 * right parameters are supplied to clustering methods and controls the UI
	 * response to user interaction.
	 *
	 * @param dialog
	 *            The JDialog that contains the components.cluster UI.
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
	 * Begins components.cluster process if the user clicks the 'Cluster' button in
	 * DendroView and sufficient parameters are set.
	 *
	 */
	private class TaskStartListener implements ActionListener {

		// To avoid synthetic compiler creation of a constructor
		protected TaskStartListener(){}
		
		@Override
		public void actionPerformed(final ActionEvent e) {

			/* Only starts with valid selections. */
			if (isReady(rowSimilarity, ROW_IDX) || isReady(colSimilarity, COL_IDX)) {
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
		private ClusteredAxisData rowCAD;
		private ClusteredAxisData colCAD;
		
		private boolean[] clusterCheck;

		private String oldFileName;

		/* Used to set upper limit of components.cluster progress bar in GUI */
		private int pBarMax = 0;
		
		public ClusterTask() {
			
			// defaults
			this.rowCAD = new ClusteredAxisData(ROW_IDX);
			this.colCAD = new ClusteredAxisData(COL_IDX);
		}

		@Override
		protected void process(final List<String> chunks) {

			if(isCancelled()) return;
			final String s = chunks.get(chunks.size() - 1);
			ClusterView.setStatusText(s);
		}

		@Override
		protected Boolean doInBackground() throws Exception {

			// Get fileName for saving calculated data
			this.oldFileName = FilenameUtils.removeExtension(tvModel.getSource());

			// Initialize the clustering processor and pass the data
			final TVDataMatrix originalMatrix = (TVDataMatrix) tvModel.getDataMatrix();
			
			// Initialize the components.cluster processor
			if(isHierarchical()) {
				// Hierarchical
				processor = new ClusterProcessor(originalMatrix);
			} 
			else {
				// K-means
				final IntLabelInfo rowLabelI = tvModel.getRowLabelInfo();
				final IntLabelInfo colLabelI = tvModel.getColLabelInfo();
				
				processor = new ClusterProcessor(originalMatrix, oldFileName, 
				                                 rowLabelI, colLabelI);
			}

			// Set zeroes invalid if they should be ignored.
			if (clusterView.isIgnoreZeroesChecked()) {
				originalMatrix.treatZeroesAsMissing();
			}

			final boolean isRowReady = isReady(rowSimilarity, ROW_IDX);
			final boolean isColReady = isReady(colSimilarity, COL_IDX);
			this.clusterCheck = reaffirmClusterChoice(isRowReady, isColReady);

			if(!clusterCheck[ROW_IDX] && !clusterCheck[COL_IDX]) {
				this.cancel(true);
				return Boolean.FALSE;
			}

			setupClusterViewProgressBar(clusterCheck[ROW_IDX], clusterCheck[COL_IDX]);
			
			if(clusterCheck[ROW_IDX]) {
				rowCAD = calculateAxis(rowSimilarity, ROW_IDX);
			}

			// Check for cancellation in between axis clustering
			if(isCancelled()) {
				return Boolean.FALSE;
			}
			
			if (clusterCheck[COL_IDX]) {
				colCAD = calculateAxis(colSimilarity, COL_IDX);
			}

			if(!isReorderingValid(clusterCheck)) {
				this.cancel(true);
				return Boolean.FALSE;
			}
			
			return Boolean.TRUE;
		}

		@Override
		public void done() {

			/* 
			 * Checked again here in case doInBackground() terminates before
			 * first check (not via cancel).
			 */
			if(!isReorderingValid(clusterCheck) || isCancelled()) {
				ClusterView.setPBarEmpty();
				LogBuffer.println("Something occurred during reordering.");
				// TODO make sure tvController doesnt crash the app here!
				return;
			}
			
			ClusterView.setPBarFull();
			ClusterModelTransformator cmt = 
				new ClusterModelTransformator(rowCAD, colCAD, (TVModel) tvModel);
			TVModel clusteredModel = cmt.applyClusterChanges(isHierarchical());
			tvController.loadClusteredModel(clusteredModel); //updates TVModel
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
			
			int numReorderedRowIDs = rowCAD.getReorderedIdxs().length;
			int numReorderedColIDs = colCAD.getReorderedIdxs().length;
			
			if(shouldClusterAxis[ROW_IDX] || tvModel.gidFound()) {
				rowsValid = (numReorderedRowIDs == numRowLabels); 
			} 
			else {
				rowsValid = (numReorderedRowIDs == 0);
			}
			
			if(shouldClusterAxis[COL_IDX] || tvModel.aidFound()) {
				colsValid = (numReorderedColIDs == numColLabels); 
			} 
			else {
				colsValid = (numReorderedColIDs == 0);
			}
			
			return rowsValid && colsValid;
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

			boolean wasRowAxisClustered = isAxisClustered(
					tvModel.getFileSet().getGtr(), tvModel.gidFound());
			boolean wasColAxisClustered = isAxisClustered(
					tvModel.getFileSet().getAtr(), tvModel.aidFound());

			// only warn if axis was clustered before AND user wants to components.cluster
			boolean warnRowAxis = wasRowAxisClustered && shouldClusterRow;
			boolean warnColAxis = wasColAxisClustered && shouldClusterCol;

			String message = "Something happened :(";
			if(warnRowAxis && warnColAxis) {
				message = "Both axes have been clustered before.\nPreviously " +
					"clustered files are located in a directory of the same " +
					"name as the original matrix file.\nWould you like to " +
					"components.cluster your selected axes again?";

				if(!confirmChoice(message)) {
					shouldClusterAxis[ROW_IDX] = false;
					shouldClusterAxis[COL_IDX] = false;
					this.cancel(true);
					return shouldClusterAxis;
				}

			} else if(warnRowAxis && !warnColAxis) {
				message = "The row axis has been clustered before.\n" +
					"Previously clustered files are located in a directory " +
					"of the same name as the original matrix file.\nWould " +
					"you like to components.cluster the rows again?";

				shouldClusterAxis[ROW_IDX]= confirmChoice(message);

			} else if(!warnRowAxis && warnColAxis){
				message = "The column axis has been clustered before.\n" +
					"Previously clustered files are located in a directory " +
					"of the same name as the original matrix file.\nWould " +
					"you like to components.cluster the columns again?";

				shouldClusterAxis[COL_IDX]= confirmChoice(message);
			}

			/* 
			 * Keeping track of components.cluster status for both axes here which is
			 * later used to ensure tree file presence if an axis is 
			 * considered to be clustered. 
			 */
			final boolean checkForRowTreeFile = wasRowAxisClustered 
					|| shouldClusterAxis[ROW_IDX];
			final boolean checkForColTreeFile = wasColAxisClustered 
					|| shouldClusterAxis[COL_IDX];
			
			rowCAD.setAxisClustered(checkForRowTreeFile);
			colCAD.setAxisClustered(checkForColTreeFile);
			
			return shouldClusterAxis;
		}
		
		/**
		 * Checks if axis was clustered using its tree file if available and 
		 * the axis specific ID if available. 
		 * If both are present, it will assume that the axis was clustered.
		 * @param treeFilePath Path of the axis tree file, if it exists.
		 * @param treeFileSuffix Axis associated tree file suffix (GTR, ATR).
		 * @param hasAxisID When loading a file, a check is performed for the
		 * axis ID label (GID, AID). This can be queried from the TVModel.
		 * @return Whether an axis is considered to have been clustered before.
		 */
		private boolean isAxisClustered(final String treeFilePath, 
				final boolean hasAxisID) {

			boolean hasTreeFile = false;

			File f = new File(treeFilePath);
			if(f.exists() && !f.isDirectory()) { 
				hasTreeFile = true;
			}

			return hasAxisID && hasTreeFile;
		}
		
		/**
		 * Opens a dialog to confirm user choice about clustering.
		 * @param message The text to be displayed in the dialog (depending on
		 * which axes have been clustered before).
		 * @return Boolean confirming whether to components.cluster or not.
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
		 * @param axisID
		 *            The chosen matrix axis.
		 * @return A list of reordered axis elements.
		 */
		private ClusteredAxisData calculateAxis(final int similarity, 
		                                        final int axisID) {
			
			final DistanceMatrix distMatrix = new DistanceMatrix(0);
			final String axisType = (axisID == ROW_IDX) ? "row" : "column";

			// progressBar label
			publish("Calculating " + axisType + " distances...");

			// calculating the distance matrix
			distMatrix.setMatrix(processor.calcDistance(similarity, axisID));

			if (isCancelled()) {
				ClusteredAxisData cad = new ClusteredAxisData(axisID);
				cad.shouldReorderAxis(false);
				return cad;
			}

			publish("Clustering " + axisType + " data...");

			return (processor.clusterAxis(distMatrix, clusterView.getLinkMethod(),
					clusterView.getSpinnerValues(), isHierarchical(), axisID));
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

			LogBuffer.println("Reset components.cluster type.");
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
				clusterView.displayReadyStatus(isReady(rowSimilarity, ROW_IDX)
						|| isReady(colSimilarity, COL_IDX));
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
				final boolean rowReady = isReady(rowSimilarity, ROW_IDX);
				final boolean colReady = isReady(colSimilarity, COL_IDX);

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
			final boolean rowReady = isReady(rowSimilarity, ROW_IDX);
			final boolean colReady = isReady(colSimilarity, COL_IDX);

			clusterView.displayReadyStatus(rowReady || colReady);
		}
	}

	/**
	 * Defines what happens if the user clicks the 'Cancel' button in
	 * DendroView. Calls the cancel() method in the view. TODO add cancel
	 * functionality to distance worker and components.cluster worker.
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

		case ROW_IDX:
			groups = (spinnerValues[0]).intValue();
			iterations = (spinnerValues[1]).intValue();
			break;
		case COL_IDX:
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
			LogBuffer.println("Cancelling components.cluster task...");
			clusterTask.cancel(true);
		}
	}

	/**
	 * Returns whether hierarchical clustering is currently selected or not.
	 *
	 * @return boolean Whether the user selected hierarchical clustering (true)
	 *         or k-means (false).
	 */
	private boolean isHierarchical() {

		return clusterView.getClusterMethod().equalsIgnoreCase(StringRes.menu_Hier);
	}
}
