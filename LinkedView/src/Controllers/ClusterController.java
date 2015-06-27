package Controllers;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import Cluster.CDTGenerator;
import Cluster.ClusterProcessor;
import Cluster.DistMatrixCalculator;
import Cluster.DistanceMatrix;
import Utilities.StringRes;
import Views.ClusterDialog;
import Views.ClusterView;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.DataInfo;
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

	private final DataModel tvModel;
	private final TVController tvController;
	private final ClusterView clusterView;
	private final ClusterDialog clusterDialog;

	/* Delegates the clustering process */
	private ClusterProcessor processor;

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
		private String[] reorderedRows;
		private String[] reorderedCols;

		private String fileName;

		private int pBarMax = 0;

		@Override
		protected void process(final List<String> chunks) {

			final String s = chunks.get(chunks.size() - 1);
			ClusterView.setLoadText(s);
		}

		@Override
		protected Void doInBackground() throws Exception {

			final int rows = tvModel.getRowHeaderInfo().getNumHeaders();
			final int cols = tvModel.getColumnHeaderInfo().getNumHeaders();

			/*
			 * Set maximum for JProgressBar before any clustering!
			 */
			if (rowSimilarity != 0) {

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

			if (colSimilarity != 0) {
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

			/* Get fileName for saving calculated data */
			fileName = tvModel.getSource().substring(0,
					tvModel.getSource().length() - 4);

			final IntHeaderInfo geneHeaderI = tvModel.getRowHeaderInfo();
			final IntHeaderInfo arrayHeaderI = tvModel.getColumnHeaderInfo();

			/* Initialize the clustering processor and pass the data */
			final TVDataMatrix originalMatrix = (TVDataMatrix) tvModel
					.getDataMatrix();
			processor = new ClusterProcessor(originalMatrix, fileName,
					geneHeaderI, arrayHeaderI);
			
			/* Set zeroes invalid if they should be ignored. */
			if(clusterView.isIgnoreZeroesChecked()) {
				originalMatrix.setZeroesToMissing();
			}

			/* Row axis cluster */
			reorderedRows = calculateAxis(rowSimilarity, ROW, fileName);

			if (isCancelled())
				return null;

			/* Column axis cluster */
			reorderedCols = calculateAxis(colSimilarity, COL, fileName);

			return null;
		}

		@Override
		public void done() {

			if (!isCancelled()) {
				saveClusterFile(fileName);

			} else {
				clusterView.setClustering(false);
				LogBuffer.println("---------------------------------------");
				LogBuffer.println("Clustering has been cancelled.");
			}
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

			/* Row axis cluster */
			final DistanceMatrix distMatrix = new DistanceMatrix(0);

			/* Check if this axis should be clustered */
			if (!isReady(similarity, axis))
				return new String[] {};

			final String axisPrefix = (axis == ROW) ? "row" : "column";

			/* ProgressBar label */
			publish("Calculating " + axisPrefix + " distances...");

			/* Calculating the distance matrix */
			distMatrix.setMatrix(processor.calcDistance(similarity, axis));

			if (isCancelled()) {
				return null;
			}
			
//			distMatrix.writeMatrix(axisPrefix);

			publish("Clustering " + axisPrefix + " data...");

			return processor.clusterAxis(distMatrix,
					clusterView.getLinkMethod(),
					clusterView.getSpinnerValues(), isHierarchical(), axis);
		}

		/**
		 * Saves the clustering output (reordered axes) to a new CDT file, so it
		 * can later be loaded and displayed.
		 */
		private void saveClusterFile(final String fileName) {

			if (reorderedRows != null || reorderedCols != null) {
				ClusterView.setLoadText("Saving...");
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

			final CDTGenerator cdtGen = new CDTGenerator(data, reorderedRows,
					reorderedCols, rowSimilarity, colSimilarity,
					isHierarchical());

			cdtGen.setupWriter(fileName, clusterView.getLinkMethod(), 
					clusterView.getSpinnerValues());

			final IntHeaderInfo geneHeaderI = tvModel.getRowHeaderInfo();
			final IntHeaderInfo arrayHeaderI = tvModel.getColumnHeaderInfo();

			cdtGen.prepare(geneHeaderI, arrayHeaderI);
			cdtGen.generateCDT();

			filePath = cdtGen.finish();

			return null;
		}

		@Override
		protected void done() {

			if (!isCancelled()) {
				ClusterView.setLoadText("Done!");
				visualizeData(filePath);

			} else {
				clusterView.setClustering(false);
				LogBuffer.println("Clustering was cancelled.");
			}
		}
	}

	/**
	 * Sets a new DendroView with the new data loaded into TVModel, displaying
	 * an updated HeatMap. It should also close the ClusterViewFrame.
	 */
	private void visualizeData(final String filePath) {

		File file = null;

		if (filePath != null) {
			file = new File(filePath);

			final FileSet fileSet = new FileSet(file.getName(),
					file.getParent() + File.separator);

			clusterDialog.dispose();

			DataInfo dataInfo = new DataInfo(new int[]{0,0}, "\\t"); // TODO replace with actual values
			tvController.loadData(fileSet, true, dataInfo);

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
