package Cluster;

import java.math.BigDecimal;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import Utilities.Helper;
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

	/* The user selected distance measure */
	private final int distMeasure;
	
	/* The distance matrix object to be filled and returned */
	private double[][] distMatrix;

	/**
	 * Constructs a DistMatrixCalculator which is used to obtain
	 * distance matrices for supplied datasets.
	 * @param data The original data from which to construct a distance matrix.
	 * @param distMeasure The user choice for the measure of distance 
	 * to be used.
	 * @param axis The axis of the original matrix which should be compared.
	 * @param worker The SwingWorker thread which calculates this operation.
	 * Used to allow cancellation of the process.
	 */
	public DistMatrixCalculator(final double[][] data, 
			final int distMeasure, final int axis) {

		LogBuffer.println("Initializing DistMatrixCalculator.");
		
		this.data = data;
		this.distMatrix = new double[data.length][];
		this.distMeasure = distMeasure;
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
	public void pearson(final boolean absolute, final boolean centered, 
			final int limit) {

		/* pearson values of one row compared to all others */
		final double[] rowDist = new double[limit];

		/* second row for comparison */
		for (int j = 0; j < limit; j++) {

			rowDist[j] = calcPearson(data[limit], data[j], centered, 
					absolute);
		}

		distMatrix[limit] = rowDist;
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

			/* Make a copy row to avoid mutation */
			final double[] copyRow = data[i].clone();

			/* Sort the copy row */
			Arrays.sort(copyRow);

			/* Iterate over sorted copy row to */
			for (int j = 0; j < copyRow.length; j++) {

				final Double rank = (double) j;
				final int index = find(data[i], copyRow[j]);

				/* Rewrite the data object. This will be used by pearson(). */
				if (index != -1) {
					data[i][index] = rank;

				} else {
					LogBuffer.println("Spearman rank distance calc "
							+ "went wrong.");
				}
			}
		}

//		pearson(false, true);
	}

	/**
	 * Uses one of the two 'taxicab' distance measures: 
	 * Euclidean or Manhattan.
	 * @param isEuclid Whether Euclidean distance is used or not.
	 */
	public void taxicab(boolean isEuclid, int limit) {

		/* Euclidean distances of one element to all others */
		final double[] rowDist = new double[limit];
		
		if(isEuclid) {
			for (int j = 0; j < limit; j++) {

				rowDist[j] = calcEuclid(data[limit], data[j]);
			}
		} else {
			for (int j = 0; j < limit; j++) {

				rowDist[j] = calcManhattan(data[limit], data[j]);
			}
		}

		/* list with all rows and their distances to the other rows */
		distMatrix[limit] = rowDist;
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
	public void calcRow(int index) {
		
		/*
		 * The switch condition is an int instead of the actual method String
		 * name because until I figured out how to pass methods as variables
		 * (or a better way to switch the methods) this function will be called
		 * from a loop. Switching ints is much less expensive.
		 * The ints follow the index position of the methods in the JComboBox
		 * in ClusterView.
		 * For reference here is a table that describes what each int 
		 * stands for:
		 * - Pearson Uncentered: 1
		 * - Pearson Centered: 2
		 * - Absolute Uncentered: 3
		 * - Absolute Centered: 4
		 * - Spearman: 5
		 * - Euclidean: 6
		 * - City Block: 7
		 */
		switch(distMeasure) {
		
		case 1: 	
			pearson(false, false, index);
			break;
		case 2: 
			pearson(false, true, index);
			break;
		case 3: 	
			pearson(true, false, index);
			break;
		case 4:	
			pearson(true, true, index);
			break;
		case 5:	
			spearman();
			break;
		case 6: 	
			taxicab(true, index);
			break;
		case 7:	
			taxicab(false, index);
			break;
		default:							
			showDistAlert();
			break;
		}
	}
	
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
