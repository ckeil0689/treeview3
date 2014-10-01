package Cluster;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import Utilities.Helper;
import Utilities.StringRes;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * This class is used to calculate a distance matrix based on input data. It
 * contains several different methods and will return a matrix of distances
 * between rows or columns, using the parameters which the user has
 * chosen.
 * 
 * @author CKeil
 * 
 */
public class DistMatrixCalculator {

	private final double EPSILON = 0.0001;
	private final double[][] data;

	private final String distMeasure;

//	private final SwingWorker<double[][], Integer> worker;
	
	private double[][] distMatrix;
	
//	private ClusterFileWriter bufferedWriter;

	/**
	 * Constructs a DistMatrixCalculator which can be used to obtain
	 * distance matrices for supplied datasets.
	 * @param data The original data from which to construct a distance matrix.
	 * @param distMeasure The user choice for the measure of distance 
	 * to be used.
	 * @param axis The axis of the original matrix which should be compared.
	 * @param worker The SwingWorker thread which calculates this operation.
	 * Used to allow cancellation of the process.
	 */
	public DistMatrixCalculator(final double[][] data, 
			final String distMeasure, final int axis, 
			final SwingWorker<double[][], Integer> worker) {

		LogBuffer.println("Initializing DistMatrixCalculator.");
		
		this.data = data;
		this.distMeasure = distMeasure;
//		this.worker = worker;
		
//		setupWriter();
	}

	/* Distance measure functions */
	/**
	 * This method generates a symmetric distance list based on 
	 * the Pearson correlation. Distances of rows on the selected axis
	 * will be compared to each other.
	 * The parameters allow for selection of different versions of the Pearson
	 * correlation (absolute Pearson correlation, centered vs. uncentered).
	 * 
	 * @param data The original dataset. 
	 * @param absolute Whether pearson should be absolute or not.
	 * @param centered Whether pearson should be centered or not.
	 */
	public void pearson(final boolean absolute, final boolean centered) {

		/* making sure distanceList is clear */
		distMatrix = new double[data.length][];

		/* take a row */
		for (int i = 0; i < data.length; i++) {
 
			/* for half matrix! full distMatrix is symmetric... */
			int limit = i;
			
			/* update progressbar */
			ClusterView.updatePBar(i); // take out of this class to swingworker

			/* pearson values of one row compared to all others */
			final double[] rowDist = new double[limit];

			/* second row for comparison */
			for (int j = 0; j < limit; j++) {

				rowDist[j] = calcPearson(data[i], data[j], centered, absolute);
			}

			distMatrix[i] = rowDist;
		}
		
//		writeMatrix();
	}
	
	/**
	 * Calculates the pearson correlation coefficient of two rows.
	 * @param row A matrix row.
	 * @param otherRow Another matrix row to be compared to the first one.
	 * @param centered Whether the correlation should be centered.
	 * @param absolute Whether the coefficient will be in absolute terms.
	 * @return The Pearson correlation coefficient.
	 */
	public double calcPearson(double[] row, double[] otherRow, 
			boolean centered, boolean absolute) {
		
		double xi = 0;
		double yi = 0;
		double mean_x = 0;
		double mean_y = 0;
		double mean_sumX = 0;
		double mean_sumY = 0;
		double sumX = 0;
		double sumY = 0;
		double sumXY = 0;
		double rootProduct = 0;

		if (centered) {
			for(int k = 0; k < row.length; k++) {
				
				mean_sumX += row[k];
				mean_sumY += otherRow[k];
			}

			mean_x = mean_sumX/ row.length;
			mean_y = mean_sumY/ otherRow.length;

		} else {// works great in cluster
			mean_x = 0;
			mean_y = 0;
		}

		/* compare each value of both genes */
		for (int k = 0; k < row.length; k++) {

			// part x
			xi = row[k];
			sumX += (xi - mean_x) * (xi - mean_x);

			// part y
			yi = otherRow[k];
			sumY += (yi - mean_y) * (yi - mean_y);

			// part xy
			sumXY += (xi - mean_x) * (yi - mean_y);
		}

		/* Calculate pearson value for current row pair. Stays 0.0 if
		 * the rootProduct is 0 */
		rootProduct = (Math.sqrt(sumX) * Math.sqrt(sumY));
		
		double pearson = 0.0;
		if(rootProduct != 0) {
			double sumOverRoot = sumXY/ rootProduct;
			pearson = (absolute) ? Math.abs(sumOverRoot) : sumOverRoot;
			pearson = 1.0 - pearson;	
		}
		
		 /* using BigDecimal to correct for rounding errors caused by 
		  * floating point arithmetic (e.g. 0.0 would be -1.113274672357E-16)
		  */
		return new BigDecimal(String.valueOf(pearson)).setScale(10, 
				BigDecimal.ROUND_DOWN).doubleValue();
	}
	

	/**
	 * TODO Some massive errors in here, correct them!
	 * This method runs the Spearman Ranked distance measure. It ranks the data
	 * values by their magnitude and then calls an uncentered Pearson
	 * correlation distance measure on the data.
	 */
	public void spearman() {

		for (int i = 0; i < data.length; i++) {

			ClusterView.updatePBar(i);

			/* Make a copy row to avoid mutation */
			final double[] copyRow = data[i].clone();

			/* Sort the copy row */
			Arrays.sort(copyRow);

			/* Iterate over sorted copy row to */
			for (int j = 0; j < copyRow.length; j++) {

				final Double rank = (double) j;
				final int index = find(data[i], copyRow[j]);

				if (index != -1) {
					data[i][index] = rank;

				} else {
					LogBuffer.println("Spearman rank distance calc "
							+ "went wrong.");
				}
			}
		}

		pearson(false, true);
	}

	/**
	 * Uses one of the two 'taxicab' distance measures: 
	 * Euclidean or Manhattan.
	 * @param isEuclid Whether Euclidean distance is used or not.
	 */
	public void taxicab(boolean isEuclid) {

		/* making sure distanceList is empty */
		distMatrix = new double[data.length][];

		/* iterate through all elements */
		for (int i = 0; i < data.length; i++) {

//			if (worker.isCancelled()) {
//				break;
//			}

			/* update progressbar */
			ClusterView.updatePBar(i);
			
			/* limit size of distMatrix element due to symmetry */
			int limit = i;

			/* Euclidean distances of one element to all others */
			final double[] rowDist = new double[limit];
			
			if(isEuclid) {
				for (int j = 0; j < limit; j++) {
	
					rowDist[j] = calcEuclid(data[i], data[j]);
				}
			} else {
				for (int j = 0; j < limit; j++) {

					rowDist[j] = calcManhattan(data[i], data[j]);
				}
			}

			/* list with all rows and their distances to the other rows */
			distMatrix[i] = rowDist;
		}
	}
	
	/**
	 * Calculates the Euclidean distance between two rows.
	 * @param row One matrix row.
	 * @param otherRow Another matrix row.
	 * @return double The Euclidean distance between the two rows.
	 */
	public double calcEuclid(double[] row, double[] otherRow) {
		
		double sum = 0;
		/* compare each value of both elements */
		for (int k = 0; k < row.length; k++) {
			
			double diff = row[k] - otherRow[k];
			sum += diff * diff; // using Math.pow is massive slow down...
		}
		
		return sum/ data.length;
	}
	
	/**
	 * Calculates the Manhattan distance between two elements.
	 * @param element One matrix element.
	 * @param otherElement Another matrix element.
	 * @return double The Manhattan distance between the two elements.
	 */
	public double calcManhattan(double[] element, double[] otherElement) {
		
		double sum = 0;
		/* compare each value of both elements */
		for (int k = 0; k < element.length; k++) {
			
			sum += Math.abs(element[k] - otherElement[k]);
		}
		
		return sum;
	}

	/**
	 * Finds the index of a value in a double array. Used for Spearman Rank.
	 * 
	 * @param array The array to query.
	 * @param value The value to be found in the array.
	 * @return The index of the target value.
	 */
	public int find(final double[] array, final double value) {

		for (int i = 0; i < array.length; i++) {

			if (Helper.nearlyEqual(array[i], value, EPSILON)) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Chooses appropriate distance measure according to user selection.
	 */
	public void measureDistance() {
		
		switch(distMeasure) {
		
		case StringRes.cluster_pearsonUn: 	
			pearson(false, false);
			break;
		case StringRes.cluster_pearsonCtrd: 
			pearson(false, true);
			break;
		case StringRes.cluster_absCorrUn: 	
			pearson(true, false);
			break;
		case StringRes.cluster_absCorrCtrd:	
			pearson(true, true);
			break;
		case StringRes.cluster_spearman:	
			spearman();
			break;
		case StringRes.cluster_euclidean: 	
			taxicab(true);
			break;
		case StringRes.cluster_cityBlock:	
			taxicab(false);
			break;
		default:							
			showDistAlert();
			break;
		}
	}
	
//	public void setupWriter() {
//		
//		LogBuffer.println("Setting up DistMatrix writer.");
//		
//		final File file = new File("C:/Users/CKeil/Programming/Princeton/"
//				+ "TreeView Related Files/test_dist_matrix.txt");
//
//		try {
//			file.createNewFile();
//			bufferedWriter = new ClusterFileWriter(file);
//			
//		} catch (IOException e) {
//			LogBuffer.logException(e);
//			String message = "There was trouble when trying to setup the"
//					+ "buffered writer to save ATR or GTR files.";
//			JOptionPane.showMessageDialog(JFrame.getFrames()[0], message, 
//					"Error", JOptionPane.ERROR_MESSAGE);
//		}
//	}
	
//	public void writeMatrix() {
//
//		LogBuffer.println("Writing distance matrix...");
//		/* Transform distMatrix to Strings for writing */ 
//		String[][] dataStrings = new String[distMatrix.length][];
//		
//		for (int i = 0; i < distMatrix.length; i++) {
//			
//			final double[] element = distMatrix[i];
//			final String[] newStringData = new String[element.length];
//
//			for (int j = 0; j < element.length; j++) {
//
//				newStringData[j] = String.valueOf(element[j]);
//			}
//
//			dataStrings[i] = newStringData;
//		}
//		
//		for(String[] row : dataStrings) {
//			bufferedWriter.writeContent(row);
//		}
//		
//		bufferedWriter.closeWriter();
//		
//		LogBuffer.println("Finished writing distMatrix.");
//	}
	
	/**
	 * Shows a pop-up alert if the selected distance measure could not
	 * be matched to an defined method to execute the calculations.
	 */
	public void showDistAlert() {
		
		String message = "Could not start measuring distance, "
				+ "because no match for the selected distance measure "
				+ "was found.";
		JOptionPane.showMessageDialog(JFrame.getFrames()[0], 
				message, "Alert", JOptionPane.WARNING_MESSAGE);
		LogBuffer.println("Alert: " + message);
	}

	/** 
	 * Getter to retrieve the distance matrix.
	 * @return The calculated distance matrix.
	 */
	public double[][] getDistanceMatrix() {

		return distMatrix;
	}
}
