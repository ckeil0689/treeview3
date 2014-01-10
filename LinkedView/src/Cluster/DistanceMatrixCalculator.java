package Cluster;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JProgressBar;

/**
 * This class is used to calculate a distance matrix based on input data. It
 * contains several different methods and will return a matrix of distances
 * between row or column elements, using the parameters which the user has
 * chosen.
 * 
 * @author CKeil
 * 
 */
public class DistanceMatrixCalculator {

	// Instance variables
	// list with all genes and their distances to all other genes
	private final List<List<Double>> distanceList = new ArrayList<List<Double>>();
	private final List<List<Double>> fullList;
	private final JProgressBar pBar;
	private final String choice;

	// Constructor
	public DistanceMatrixCalculator(final List<List<Double>> fullList,
			final String choice, final JProgressBar pBar) {

		this.fullList = fullList;
		this.pBar = pBar;
		this.choice = choice;
	}

	// Methods to calculate distance matrix
	/**
	 * This method generates a distance list based on the Pearson correlation.
	 * The parameters allow for selection of different versions of the Pearson
	 * correlation (absolute Pearson correlation, centered vs. uncentered).
	 * 
	 * @param fullList
	 * @param absolute
	 * @param centered
	 * @return List<List<Double> distance matrix
	 */
	public void pearson(final boolean absolute, final boolean centered) {

		pBar.setMaximum(fullList.size());

		// making sure distanceList is clear
		distanceList.clear();

		// take a gene
		for (int i = 0; i < fullList.size(); i++) {

			// update progressbar
			pBar.setValue(i);

			// refers to one gene with all it's data
			final List<Double> data = fullList.get(i);

			// pearson values of one gene compared to all others
			final List<Double> pearsonList = new ArrayList<Double>();

			// second gene for comparison
			for (int j = 0; j < fullList.size(); j++) {

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

				final List<Double> data2 = fullList.get(j);

				if (centered) {// causes issues in cluster(????)
					for (final double x : data) {

						mean_sumX += x;
					}

					mean_x = mean_sumX / data.size(); // casted int to double

					for (final double y : data2) {

						mean_sumY += y;
					}

					mean_y = mean_sumY / data2.size();

				} else {// works great in cluster

					mean_x = 0;
					mean_y = 0;
				}

				// compare each value of both genes
				for (int k = 0; k < data.size(); k++) {

					// part x
					xi = data.get(k);
					sumX += (xi - mean_x) * (xi - mean_x);

					// part y
					yi = data2.get(k);
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

				pearsonList.add(pearson2.doubleValue());
			}

			distanceList.add(pearsonList);
		}
	}

	/**
	 * This method runs the Spearman Ranked distance measure. It ranks the data
	 * values by their magnitude and then calls an uncentered Pearson
	 * correlation distance measure on the data.
	 */
	public void spearman() {

		// making sure distanceList is clear
		distanceList.clear();

		System.out.println("FullList E 1000 Sample: "
				+ fullList.get(1000).subList(0, 20));

		for (final List<Double> row : fullList) {

			final List<Double> copyRow = new ArrayList<Double>(row);

			Collections.sort(copyRow);

			for (int i = 0; i < copyRow.size(); i++) {

				final Double rank = (double) i;
				row.set(row.indexOf(copyRow.get(i)), rank);
			}
		}

		System.out.println("FullList E 1000 Sample POST: "
				+ fullList.get(1000).subList(0, 20));

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

		pBar.setMaximum(fullList.size());

		// making sure distanceList is empty
		distanceList.clear();

		// take a gene
		for (int i = 0; i < fullList.size(); i++) {

			// update progressbar
			pBar.setValue(i);

			// refers to one gene with all it's data
			final List<Double> gene = fullList.get(i);

			// distances of one gene to all others
			final List<Double> geneDistance = new ArrayList<Double>();

			// choose a gene for distance comparison
			for (int j = 0; j < fullList.size(); j++) {

				final List<Double> gene2 = fullList.get(j);

				// squared differences between elements of 2 genes
				final List<Double> sDiff = new ArrayList<Double>();

				// compare each value of both genes
				for (int k = 0; k < gene.size(); k++) {

					g1 = gene.get(k);
					g2 = gene2.get(k);
					gDiff = g1 - g2;
					sDist = gDiff * gDiff;
					sDiff.add(sDist);
				}

				// sum all the squared value distances up
				// --> get distance between gene and gene2
				double sum = 0;
				for (final double element : sDiff) {
					sum += element;
				}

				double divSum = 0;
				divSum = sum / fullList.size();

				geneDistance.add(divSum);
			}

			// list with all genes and their distances to the other genes
			distanceList.add(geneDistance);
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

		pBar.setMaximum(fullList.size());

		// making sure distanceList is clear
		distanceList.clear();

		// take a gene
		for (int i = 0; i < fullList.size(); i++) {

			// update progressbar
			pBar.setValue(i);

			// refers to one gene with all it's data
			final List<Double> data = fullList.get(i);

			// distances of one gene to all others
			final List<Double> dataDistance = new ArrayList<Double>();

			// choose a gene for distance comparison
			for (int j = 0; j < fullList.size(); j++) {

				final List<Double> data2 = fullList.get(j);

				// differences between elements of 2 genes
				final List<Double> sDiff = new ArrayList<Double>();

				// compare each value of both genes
				for (int k = 0; k < data.size(); k++) {

					g1 = data.get(k);
					g2 = data2.get(k);
					gDiff = g1 - g2;
					gDiff = Math.abs(gDiff);
					sDiff.add(gDiff);
				}

				// sum all the squared value distances up
				// --> get distance between gene and gene2
				double sum = 0;
				for (final double element : sDiff) {

					sum += element;
				}

				dataDistance.add(sum);
			}

			// list with all genes and their distances to the other genes
			distanceList.add(dataDistance);
		}
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
	public List<List<Double>> getDistanceMatrix() {

		return distanceList;
	}
}
