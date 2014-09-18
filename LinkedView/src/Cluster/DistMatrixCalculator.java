package Cluster;

import java.math.BigDecimal;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import Utilities.StringRes;
import Controllers.ClusterController;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * This class is used to calculate a distance matrix based on input data. It
 * contains several different methods and will return a matrix of distances
 * between row or column elements, using the parameters which the user has
 * chosen.
 * 
 * @author CKeil
 * 
 */
public class DistMatrixCalculator {

	private final double PRECISION_LEVEL = 0.0001;
	private final double[][] data;

	private final String distMeasure;
	private final String axisPrefix;

	private final SwingWorker<String[], Void> worker;
	
	private double[][] distMatrix;

	// Constructor
	public DistMatrixCalculator(final double[][] fullList, 
			final String distMeasure, final int axis, 
			final SwingWorker<String[], Void> worker) {

		this.data = fullList;
		this.distMeasure = distMeasure;
		this.worker = worker;
		this.axisPrefix = (axis == ClusterController.ROW) ? "Row" : "Column";
	}

	// Methods to calculate distance matrix
	/**
	 * This method generates a symmetric distance list based on 
	 * the Pearson correlation. Distances of elements on the selected axis
	 * will be compared to each other.
	 * The parameters allow for selection of different versions of the Pearson
	 * correlation (absolute Pearson correlation, centered vs. uncentered).
	 * 
	 * @param data
	 * @param absolute
	 * @param centered
	 * @return List<List<Double> distance matrix
	 */
	public void pearson(final boolean absolute, final boolean centered) {
				
		// Reset in case Spearman Rank was used
		ClusterView.setLoadText("Calculating " + axisPrefix 
				+ " Distance Matrix...");

		ClusterView.setPBarMax(data.length);

		// making sure distanceList is clear
		distMatrix = new double[data.length][];

		// take an element
		for (int i = 0; i < data.length; i++) {

			if (worker.isCancelled()) {
				break;
			}
 
			// for half matrix! full distMatrix is symmetric...
			int limit = i;
			
			// update progressbar
			ClusterView.updatePBar(i);

			// pearson values of one element compared to all others
			final double[] elementDist = new double[limit];

			// second element for comparison
			for (int j = 0; j < limit; j++) {

				elementDist[j] = calcPearson(data[i], data[j], 
						centered, absolute);
			}

			distMatrix[i] = elementDist;
		}
	}
	
	/**
	 * Calculates the pearson correlation coefficient of two elements.
	 * @param element First matrix element.
	 * @param otherElement Matrix element to be compared.
	 * @param centered Whether the correlation should be centered.
	 * @param absolute Whether the coefficient will be in absolute terms.
	 * @return double The Pearson correlation coefficient.
	 */
	public double calcPearson(double[] element, double[] otherElement, 
			boolean centered, boolean absolute) {
		
		// local variables
		double xi = 0;
		double yi = 0;
		double mean_x = 0;
		double mean_y = 0;
		double mean_sumX = 0;
		double mean_sumY = 0;
		double sumX = 0;
		double sumY = 0;
		double sumXY = 0;
		double pearson = 0;
		double rootProduct = 0;

		if (centered) {
			for(int k = 0; k < element.length; k++) {
				
				mean_sumX += element[k];
				mean_sumY += otherElement[k];
			}

			mean_x = mean_sumX/ element.length;
			mean_y = mean_sumY/ otherElement.length;

		} else {// works great in cluster
			mean_x = 0;
			mean_y = 0;
		}

		// compare each value of both genes
		for (int k = 0; k < element.length; k++) {

			// part x
			xi = element[k];
			sumX += (xi - mean_x) * (xi - mean_x);

			// part y
			yi = otherElement[k];
			sumY += (yi - mean_y) * (yi - mean_y);

			// part xy
			sumXY += (xi - mean_x) * (yi - mean_y);
		}

		// calculate pearson value for current element pair
		rootProduct = (Math.sqrt(sumX) * Math.sqrt(sumY));
		
		if(rootProduct != 0) {
			double sumOverRoot = sumXY/ rootProduct;
			pearson = (absolute) ? Math.abs(sumOverRoot) :  sumOverRoot;
			pearson = 1.0 - pearson;
			
		} else {
			pearson = 0.0;
		}
		
		// using BigDecimal to correct for rounding errors caused by
		// floating point arithmetic (0.0 would be
		// -1.113274672357E-16 for example)
		return new BigDecimal(String.valueOf(pearson)).setScale(10, 
				BigDecimal.ROUND_DOWN).doubleValue();
	}
	

	/**
	 * This method runs the Spearman Ranked distance measure. It ranks the data
	 * values by their magnitude and then calls an uncentered Pearson
	 * correlation distance measure on the data.
	 */
	public void spearman() {
				
		ClusterView.setLoadText("Getting " + axisPrefix + " Spearman Ranks...");

		for (int i = 0; i < data.length; i++) {

			if (worker.isCancelled()) {
				break;
			}

			ClusterView.updatePBar(i);

			// Make a copy row to avoid mutation
			final double[] copyRow = data[i].clone();

			// Sort the copy row
			Arrays.sort(copyRow);

			// Iterate over sorted copy row to
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
	 * @param euclid Whether Euclidean distance is used or not.
	 */
	public void taxicab(boolean euclid) {

		ClusterView.setLoadText("Calculating " + axisPrefix 
				+ " Distance Matrix...");

		ClusterView.setPBarMax(data.length);

		// making sure distanceList is empty
		distMatrix = new double[data.length][];

		// iterate through all elements
		for (int i = 0; i < data.length; i++) {

			if (worker.isCancelled()) {
				break;
			}

			// update progressbar
			ClusterView.updatePBar(i);
			
			// limit size of distMatrix element due to symmetry 
			int limit = i;

			// euclidean distances of one element to all others
			final double[] elementDist = new double[limit];
			
			if(euclid) {
				for (int j = 0; j < limit; j++) {
	
					elementDist[j] = calcEuclid(data[i], data[j]);
				}
			} else {
				for (int j = 0; j < limit; j++) {

					elementDist[j] = calcManhattan(data[i], data[j]);
				}
			}

			// list with all genes and their distances to the other genes
			distMatrix[i] = elementDist;
		}
	}
	
	/**
	 * Calculates the Euclidean distance between two elements.
	 * @param element One matrix element.
	 * @param otherElement Another matrix element.
	 * @return double The Euclidean distance between the two elements.
	 */
	public double calcEuclid(double[] element, double[] otherElement) {
		
		double sum = 0;
		// compare each value of both elements
		for (int k = 0; k < element.length; k++) {
			
			double diff = element[k] - otherElement[k];
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
		// compare each value of both elements
		for (int k = 0; k < element.length; k++) {
			
			sum += Math.abs(element[k] - otherElement[k]);
		}
		
		return sum;
	}

	/**
	 * Finds the index of a value in a double array. Used for Spearman Rank.
	 * 
	 * @param array
	 * @param value
	 * @return
	 */
	public int find(final double[] array, final double value) {

		for (int i = 0; i < array.length; i++) {

			if (Math.abs(array[i] - value) < PRECISION_LEVEL) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Chooses appropriate distance measure according to user selection.
	 */
	public void measureDistance() {
		
		ClusterView.setLoadText("Calculating " + axisPrefix 
				+ " Distance Matrix...");
		ClusterView.setPBarMax(data.length);
		
		switch(distMeasure) {
		
			case StringRes.cluster_pearsonUn: 	pearson(false, false);
												break;
			case StringRes.cluster_pearsonCtrd: pearson(false, true);
												break;
			case StringRes.cluster_absCorrUn: 	pearson(true, false);
												break;
			case StringRes.cluster_absCorrCtrd:	pearson(true, true);
												break;
			case StringRes.cluster_spearman:	spearman();
												break;
			case StringRes.cluster_euclidean: 	taxicab(true);
												break;
			case StringRes.cluster_cityBlock:	taxicab(false);
												break;
			default:							showDistAlert();
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

	// Accessor method to retrieve the distance matrix
	public double[][] getDistanceMatrix() {

		return distMatrix;
	}
}
