package Cluster;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * This class is used to calculate a distance matrix based on input data. It
 * contains several different methods and will return a matrix of distances
 * between row or column elements, using the parameters which the user has
 * chosen.
 * 
 * @author CKeil
 * 
 */
public class DMCalculatorArrays {

	// Instance variables
	// list with all genes and their distances to all other genes
	private double[][] distanceMatrix;
	private final double[][] dataArrays;
	
	private final ClusterView clusterView;
	private final String choice;

	// Constructor
	public DMCalculatorArrays(final double[][] fullList,
			final String choice, final ClusterView clusterView) {

		this.dataArrays = fullList;
		this.choice = choice;
		this.clusterView = clusterView;
		
		clusterView.setLoadText("Calculating distance matrix...");
	}

	// Methods to calculate distance matrix
	/**
	 * This method generates a distance list based on the Pearson correlation.
	 * The parameters allow for selection of different versions of the Pearson
	 * correlation (absolute Pearson correlation, centered vs. uncentered).
	 * 
	 * @param dataArrays
	 * @param absolute
	 * @param centered
	 * @return List<List<Double> distance matrix
	 */
	public void pearson(final boolean absolute, final boolean centered) {

		// Reset in case Spearman Rank was used
		clusterView.setLoadText("Calculating Distance Matrix...");
		
		clusterView.setPBarMax(dataArrays.length);

		// making sure distanceList is clear
		distanceMatrix = new double[dataArrays.length][];

		// take a gene
		for (int i = 0; i < dataArrays.length; i++) {

			// update progressbar
			clusterView.updatePBar(i);

			// refers to one gene with all it's data
			final double[] data = dataArrays[i];

			// pearson values of one gene compared to all others
			final double[] pearsonList = new double[dataArrays.length];

			// second gene for comparison
			for (int j = 0; j < dataArrays.length; j++) {

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
				double sumX_root = 0;
				double sumY_root = 0;
				double pearson1 = 0;
				double rootProduct = 0;
				double finalVal = 0;
				BigDecimal pearson2;

				final double[] data2 = dataArrays[j];

				if (centered) {// causes issues in cluster(????)
					for (final double x : data) {

						mean_sumX += x;
					}

					mean_x = mean_sumX / data.length; // casted int to double

					for (final double y : data2) {

						mean_sumY += y;
					}

					mean_y = mean_sumY / data2.length;

				} else {// works great in cluster

					mean_x = 0;
					mean_y = 0;
				}

				// compare each value of both genes
				for (int k = 0; k < data.length; k++) {

					// part x
					xi = data[k];
					sumX += (xi - mean_x) * (xi - mean_x);

					// part y
					yi = data2[k];
					sumY += (yi - mean_y) * (yi - mean_y);

					// part xy
					sumXY += (xi - mean_x) * (yi - mean_y);
				}

				sumX_root = Math.sqrt(sumX);
				sumY_root = Math.sqrt(sumY);

				// calculate pearson value for current gene pair
				rootProduct = (sumX_root * sumY_root);

				if (absolute) {
					finalVal = Math.abs(sumXY / rootProduct);

				} else {
					finalVal = sumXY / rootProduct;
				}

				pearson1 = 1.0 - finalVal;

				// using BigDecimal to correct for rounding errors caused by
				// floating point arithmetic (0.0 would be
				// -1.113274672357E-16 for example)
				pearson2 = new BigDecimal(String.valueOf(pearson1));
				pearson2 = pearson2.setScale(10, BigDecimal.ROUND_DOWN);

				pearsonList[j] = pearson2.doubleValue();
			}

			distanceMatrix[i] = pearsonList;
		}
	}

	/**
	 * This method runs the Spearman Ranked distance measure. It ranks the data
	 * values by their magnitude and then calls an uncentered Pearson
	 * correlation distance measure on the data.
	 */
	public void spearman() {
		
		clusterView.setLoadText("Getting Spearman Ranks...");

		for (int i = 0; i < dataArrays.length; i++) {
			
			clusterView.updatePBar(i);

			// Make a copy row to avoid mutation
			final double[] copyRow = dataArrays[i].clone();

			// Sort the copy row
			Arrays.sort(copyRow);

			// Iterate over sorted copy row to 
			for (int j = 0; j < copyRow.length; j++) {

				final Double rank = (double) j;
				int index = find(dataArrays[i], copyRow[j]);
				
				if(index != -1) {
					dataArrays[i][index] = rank;
				
				} else {
					System.out.println("Spearman rank distance calc " +
							"went wrong.");
				}
			}
		}

		pearson(false, true);
	}

	// Euclidean Distance
	/**
	 * Euclidean distance measure applied to the data matrix.
	 */
	public void euclid() {

		// local variables
		double sDist = 0;
		double g1 = 0;
		double g2 = 0;
		double gDiff = 0;

		clusterView.setPBarMax(dataArrays.length);

		// making sure distanceList is empty
		distanceMatrix = new double[dataArrays.length][];

		// take a gene
		for (int i = 0; i < dataArrays.length; i++) {

			// update progressbar
			clusterView.updatePBar(i);

			// refers to one gene with all it's data
			final double[] gene = dataArrays[i];

			// distances of one gene to all others
			final double[] geneDistance = new double[dataArrays.length];

			// choose a gene for distance comparison
			for (int j = 0; j < dataArrays.length; j++) {

				final double[] gene2 = dataArrays[j];

				// squared differences between elements of 2 genes
				final double[] sDiff = new double[gene.length];

				// compare each value of both genes
				for (int k = 0; k < gene.length; k++) {

					g1 = gene[k];
					g2 = gene2[k];
					gDiff = g1 - g2;
					sDist = gDiff * gDiff;
					sDiff[k] = sDist;
				}

				// sum all the squared value distances up
				// --> get distance between gene and gene2
				double sum = 0;
				for (final double element : sDiff) {
					sum += element;
				}

				double divSum = 0;
				divSum = sum / dataArrays.length;

				geneDistance[j] = divSum;
			}

			// list with all genes and their distances to the other genes
			distanceMatrix[i] = geneDistance;
		}
	}

	/**
	 * Manhattan Distance measure applied to data matrix
	 */
	public void cityBlock() {

		// local variables
		double g1 = 0;
		double g2 = 0;
		double gDiff = 0;

		clusterView.setPBarMax(dataArrays.length);

		// making sure distanceList is clear
		distanceMatrix = new double[dataArrays.length][];

		// take a gene
		for (int i = 0; i < dataArrays.length; i++) {
			
			// update progressbar
			clusterView.updatePBar(i);

			// refers to one gene with all it's data
			final double[] data = dataArrays[i];

			// distances of one gene to all others
			final double[] dataDistance = new double[dataArrays.length];

			// choose a gene for distance comparison
			for (int j = 0; j < dataArrays.length; j++) {

				final double[] data2 = dataArrays[j];

				// differences between elements of 2 genes
				final double[] sDiff = new double[data.length];

				// compare each value of both genes
				for (int k = 0; k < data.length; k++) {

					g1 = data[k];
					g2 = data2[k];
					gDiff = g1 - g2;
					gDiff = Math.abs(gDiff);
					sDiff[k] = gDiff;
				}

				// sum all the squared value distances up
				// --> get distance between gene and gene2
				double sum = 0;
				for (final double element : sDiff) {

					sum += element;
				}

				dataDistance[j] = sum;
			}

			// list with all genes and their distances to the other genes
			distanceMatrix[i] = dataDistance;
		}
	}
	
	/**
	 * Finds the index of a value in a double array.
	 * @param array
	 * @param value
	 * @return
	 */
	public int find(double[] array, double value) {
	    
		for(int i = 0; i < array.length; i++) {
			
			if(array[i] == value) {
				return i;
			}
		} 
		
		return -1;
	}

	/**
	 * Chooses appropriate distance measure according to user GUI selection.
	 */
	public void measureDistance() {
		
		if (choice.equalsIgnoreCase("Pearson Correlation (uncentered)")) {
			pearson(false, false);

		} else if (choice.equalsIgnoreCase("Pearson Correlation "
				+ "(centered)")) {
			pearson(false, true);

		} else if (choice.equalsIgnoreCase("Absolute Correlation "
				+ "(uncentered)")) {
			pearson(true, false);

		} else if (choice.equalsIgnoreCase("Absolute Correlation "
				+ "(centered)")) {
			pearson(true, true);

		} else if (choice.equalsIgnoreCase("Spearman Ranked Correlation")) {
			spearman();

		} else if (choice.equalsIgnoreCase("Euclidean Distance")) {
			euclid();

		} else if (choice.equalsIgnoreCase("City Block Distance")) {
			cityBlock();

		} else {

		}
	}

	// Accessor method to retrieve the distance matrix
	public double[][] getDistanceMatrix() {

		return distanceMatrix;
	}
}
