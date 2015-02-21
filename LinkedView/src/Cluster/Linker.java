package Cluster;

import java.util.List;

import Utilities.Helper;

public class Linker {

	/* Correspond to the JCombobox positions in ClusterView */
	protected final static int AVG = 0;
	protected final static int SINGLE = 1;
	protected final static int COMPLETE = 2;
	
	private int linkMethod;
	
	/*
	 * Deep copy of distance matrix because original will be mutated. The
	 * mutation happens when a new row is formed from to rows that are being
	 * clustered together.
	 */
	private DistanceMatrix distMatrixClone;
	
	public Linker(int linkMethod) {
		
		this.linkMethod = linkMethod;
		this.distMatrixClone = new DistanceMatrix(0);
	}
	
	public void cloneDistmatrix(DistanceMatrix origMatrix) {
		
		distMatrixClone.cloneFrom(origMatrix);
	}
	
	public double[] link(final int[] fusedClusters, 
			List<List<Integer>> currentClusters) {
		
		if (linkMethod == SINGLE || linkMethod == COMPLETE) {
			return singleCompleteLink(fusedClusters, currentClusters);
		} else {
			return averageLink(fusedClusters, currentClusters);
		}
	}
	
	/**
	 * Method used to generate a new row/col for the distance matrix which is
	 * processed. The new row/col represents the joint row pair which has been
	 * chosen as the one with the minimum distance in the current iteration. The
	 * values of the new row/col are calculated as maximum (complete) or minimum
	 * (single) of all distance values.
	 *
	 * Time complexity: ?
	 *
	 * @param fusedClusters
	 * @return newRow
	 */
	private double[] singleCompleteLink(final int[] fusedClusters, 
			List<List<Integer>> currentClusters) {

		/* Make a new row that has the same size as the number of clusters. */
		final double[] newRow = new double[currentClusters.size()];

		/* Iterate over all clusters */
		for (int i = 0; i < currentClusters.size(); i++) {

			double distanceVal = 0;
			int selectedRow = 0;

			final int[] currentCluster = new int[currentClusters.get(i).size()];

			/* Filling the array */
			for (int z = 0; z < currentCluster.length; z++) {

				currentCluster[z] = currentClusters.get(i).get(z);
			}

			/*
			 * Only calculate distance if the current cluster is not part of the
			 * newly formed cluster. If it is, then the distance is 0.
			 */
			if (!Helper.shareCommonElements(currentCluster, fusedClusters)) {
				final double[] distances = new double[fusedClusters.length
						* currentCluster.length];

				double min = Double.MAX_VALUE;
				double max = Double.MIN_VALUE;

				int dInd = 0; // Iterator
				for (final int fusedCluster : fusedClusters) {

					/* select element from new cluster */
					selectedRow = fusedCluster;

					/*
					 * Get the corresponding row in the original, non-mutated
					 * matrix.
					 */
					final double[] currentRow = distMatrixClone
							.getRow(selectedRow);

					/*
					 * Go through all clusters and their elements. Finds the
					 * distances between a row in currentCluster (remaining
					 * non-clustered) and all rows in the new cluster
					 * (fusedClusters, current row cluster).
					 */
					for (final int element : currentCluster) {

						// if-else to allow for use of halfDMatrix
						if (currentRow.length > element) {
							/*
							 * distance value in the current row at the indices
							 * which correspond to the elements of the new
							 * current cluster.
							 */
							distanceVal = currentRow[element];

						} else {
							/*
							 * Reverse index access because of distance matrix
							 * symmetry.
							 */
							distanceVal = distMatrixClone.getRow(element)[selectedRow];
						}

						/* Determine min and max */
						if (distanceVal < min) {
							min = distanceVal;
						}
						if (distanceVal > max) {
							max = distanceVal;
						}

						distances[dInd] = distanceVal;
						dInd++;
					}
				}

				/*
				 * Single Link - Minimum distance between the new cluster and
				 * the other clusters.
				 */
				if (linkMethod == SINGLE) {
					newRow[i] = min;
				}
				/* Complete Link - Maximum */
				else {
					newRow[i] = max;
				}
			}
			/* all elements in common */
			else {
				newRow[i] = 0.0;
			}
		}

		return newRow;
	}

	/**
	 * Method used to generate a new row/col for the distance matrix which is
	 * processed. The new row/col represents the joint gene pair which has been
	 * chosen as the one with the minimum distance in each iteration. The values
	 * of the new row/col are calculated as average of the sum of all distances.
	 *
	 * @param fusedGroup
	 * @param currentClusters
	 * @param newRow
	 * @return newRow
	 */
	private double[] averageLink(final int[] fusedGroup, 
			List<List<Integer>> currentClusters) {

		final double[] newRow = new double[currentClusters.size()];

		for (int i = 0; i < currentClusters.size(); i++) {

			double distanceSum = 0;
			double distanceVal = 0;
			int selectedGene = 0;

			final int[] currentGroup = new int[currentClusters.get(i).size()];

			for (int z = 0; z < currentGroup.length; z++) {

				currentGroup[z] = currentClusters.get(i).get(z);
			}

			/*
			 * Check if fusedGroup contains the current checked gene (then no
			 * mean should be calculated) no elements in common.
			 */
			if (!Helper.shareCommonElements(currentGroup, fusedGroup)) {

				/* select members of the new row (B & G) */
				for (final int element : fusedGroup) {

					selectedGene = element;

					/*
					 * Take a row (gene) from the matrix which also appears in
					 * the fusedGroup (current cluster).
					 */
					final double[] currentRow = distMatrixClone
							.getRow(selectedGene);

					/*
					 * Go through all clusters and their contained genes
					 * calculate the distance between each column (gene) and the
					 * current row (gene) from fusedGroup sum the distance
					 * values up.
					 */
					for (final int gene : currentClusters.get(i)) {

						if (currentRow.length > gene) {
							distanceVal = currentRow[gene];

						} else {
							distanceVal = distMatrixClone.getRow(gene)[selectedGene];
						}
						distanceSum += distanceVal;
					}
				}

				/*
				 * Calculate the new value to be added to newRow as the average
				 * of distances.
				 */
				newRow[i] = distanceSum
						/ (fusedGroup.length * currentGroup.length);
			}
			/* all elements in common */
			else {
				newRow[i] = 0.0;
			}
		}

		return newRow;
	}
	
	public void close() {
		
		distMatrixClone = null;
	}
}
