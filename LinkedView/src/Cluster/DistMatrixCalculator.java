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

	/* Distance method indeces from JCombobox in ClusterView */
	public static final int NO_CLUSTER = 0;
	public static final int PEARSON_UN = 1;
	public static final int PEARSON_CEN = 2;
	public static final int ABSOLUTE_UN = 3;
	public static final int ABSOLUTE_CEN = 4;
	public static final int SPEARMAN = 5;
	public static final int EUCLIDEAN = 6;
	public static final int CITY_BLOCK = 7;
	
	/* 
	 * reference for the matrix to be clustered. Spearman Rank generates
	 * the rank matrix, which needs to be clustered. Without reference variable,
	 * the data matrix would be mutated.
	 */
	private double[][] taskData;

	/* The user selected distance measure */
	private final int distMeasure;
	
	/* The distance matrix object to be filled and returned */
	private final double[][] distMatrix;

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
		
		this.taskData = data;
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

			rowDist[j] = calcPearson(taskData[limit], taskData[j], centered, 
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
	 * This method runs the Spearman Ranked distance measure. It rewrites the
	 * data object with ranks for the values in the matrix rows.
	 * Later on, pearson correlation will be called on the rewritten data
	 * object. 
	 */
	public double[] spearman(double[] row) {
		
	/*
	 * Step 1: Go through each row.
	 * Step 2: Rank the values in the data array by increasing 
	 * numerical value.
	 * Step 3: For all identical values, make rank the average rank.
	 * 
	 * If the value is the same as the 'next' one in the sorted array 
	 * (e.g. 0.0 following 0.0), it doesn't matter to what index in the 
	 * array it corresponds to. All of the same values will be assigned
	 * the same rank, which only depends on the number of equal values.
	 * The assigned rank will be the average rank of all these values.
	 * If two 1.3s are in the data, and the first is rank 5, 
	 * the second rank 6, then their actual recorded rank will be 5.5.
	 */
		double[] rankRow = new double[row.length];
		
		/* Make a copy row to avoid mutation */
		final double[] copyRow = row.clone();

		/* Sort the copy row */
		Arrays.sort(copyRow);
		
		double temp;
		int countDuplicates = 1;
		int duplicateRankSum = 0;
		/* Iterate over sorted copy row to assign ranks */
		for (int j = 0; j < copyRow.length; j++) {
			
			temp = copyRow[j];
			
			/*
			 * Check if following values in array are identical.
			 */	
			while (j + 1 < copyRow.length 
					&& Helper.nearlyEqual(copyRow[j + 1], temp)) {
				countDuplicates++;
				duplicateRankSum += j;
				temp = copyRow[j];
				j++;
			}
			
			/* Fill duplicate values with average rank */
			if (countDuplicates > 1) {
				double avgRank = duplicateRankSum / (countDuplicates * 1.0);
				
				for(int k = 0; k < row.length; k++) {
					
					if(Helper.nearlyEqual(row[k], temp)) {
						rankRow[k] = avgRank; 	
					} 
				}
				
				/* Don't forget to set new temp */
				temp = copyRow[j];
			} 
			
			/* Assign rank if value is unique */
			else {
				/* Find index of current copy row element in original */
				final int index = find(row, copyRow[j]);

				/* Write rank over original value at found index. */
				if (index != -1) {
					rankRow[index] = j + 1;  // j + 1 is the rank! 
					temp = copyRow[j];
				} else {
					LogBuffer.println("Spearman rank failed due to a "
							+ "ranking issue.");
					break;
				}
			}
			
			/* reset duplicate params */
			countDuplicates = 1;
			duplicateRankSum = 0;
		}
		
		return rankRow;
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

				rowDist[j] = calcEuclid(taskData[limit], taskData[j]);
			}
		} else {
			for (int j = 0; j < limit; j++) {

				rowDist[j] = calcManhattan(taskData[limit], taskData[j]);
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
			sum += diff * diff; 
		}
		
		return sum/ taskData.length;
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

			if (Helper.nearlyEqual(array[i], value)) {
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
		 * Switches JCombobox item indeces.
		 */
		switch(distMeasure) {
		
		case PEARSON_UN: 	
			pearson(false, false, index);
			break;
		case PEARSON_CEN: 
			pearson(false, true, index);
			break;
		case ABSOLUTE_UN: 	
			pearson(true, false, index);
			break;
		case ABSOLUTE_CEN:	
			pearson(true, true, index);
			break;
		case SPEARMAN:	
			pearson(false, false, index); // pearson after ranking!
			break;
		case EUCLIDEAN: 	
			taxicab(true, index);
			break;
		case CITY_BLOCK:	
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
	
	public void setTaskData(double[][] data) {
		
		this.taskData = data;
	}

	/** 
	 * Getter to retrieve the distance matrix.
	 * @return The calculated distance matrix.
	 */
	public double[][] getDistanceMatrix() {

		return distMatrix;
	}
}
