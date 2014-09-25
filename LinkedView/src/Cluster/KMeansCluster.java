package Cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import Controllers.ClusterController;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * Class to perform the calculations of the K-Means algorithm.
 * @author CKeil
 *
 */
public class KMeansCluster {

	// Instance variables
	private final DataModel model;

	private ClusterFileWriter bufferedWriter;
	private String filePath;
	private int axis;
	private final int groupNum;
	private final int iterations;

	// Distance Matrix
	private final double[][] distMatrix;

	// Half of the Distance Matrix (symmetry)
	private double[][] copyDistMatrix;

	// list to return ordered GENE numbers for .cdt creation
	private double[] seedMeans;

	// list to return ordered GENE numbers for .cdt creation
	private String[] reorderedList;

	private final SwingWorker<String[], Integer> worker;

	/**
	 * The KMeansCluster object needs several parameters to be able to
	 * start its calculations. It will eventually return a String array of 
	 * reordered matrix elements. 
	 * @param model The model which holds the loaded data as well as info
	 * about this it.
	 * @param distMatrix The calculated distance matrix to be clustered.
	 * @param axis The matrix axis to be clustered.
	 * @param groupNum The number of groups (k) to be formed.
	 * @param iterations The number iterations the k-means algorithm is
	 * run. This impacts the clustering result.
	 * @param worker The clustering worker object. Used to check if the user
	 * cancelled the operation, so that calculations can be interrupted.
	 */
	public KMeansCluster(final DataModel model, final double[][] distMatrix, 
			final int axis, final int groupNum, final int iterations, 
			final SwingWorker<String[], Integer> worker) {

		this.model = model;
		this.distMatrix = distMatrix;
		this.axis = axis;
		this.groupNum = groupNum;
		this.iterations = iterations;
		this.worker = worker;
	}

	// method for clustering the distance matrix
	public void cluster() {

		// Just checking for debugging
		LogBuffer.println("Is KMeansCluster.cluster() on EDT? " 
				+ SwingUtilities.isEventDispatchThread());
				
		double[] elementMeanList = new double[distMatrix.length];
		int[][] clusters = new int[groupNum][];
		double[][] clusterMeans = new double[distMatrix.length][];

		// ProgressBar maximum
		ClusterView.setLoadText("Clustering data...");
		ClusterView.setPBarMax(distMatrix.length);

		// deep copy of distance matrix to avoid mutation
		copyDistMatrix = deepCopy(distMatrix);

		// The list containing the reordered gene names.
		reorderedList = new String[distMatrix.length];

		// Make a list of all means of distances for every gene
		elementMeanList = generateMeans(copyDistMatrix);

		// List of seeds should be of clusterN-size
		seedMeans = new double[groupNum];
		setSeeds(groupNum, elementMeanList);

		// Use the seedClusters to define Voronoi spaces. This means, assign
		// the distance mean of each gene to the closest seed cluster.
		clusters = assignMeansVals(elementMeanList);
		clusterMeans = indexesToMeans(elementMeanList, clusters);

		// Begin iteration of recalculating means and reassigning gene
		// distance means to clusters
		for (int i = 0; i < iterations; i++) {

			if (worker.isCancelled()) {
				break;
			}

			findNewMeans(clusterMeans);

			clusters = assignMeansVals(elementMeanList);
			clusterMeans = indexesToMeans(elementMeanList, clusters);
		}

		// Next Step: Generate CDT and KGG Excel file
		String[][] geneGroups = new String[clusters.length][];

		geneGroups = indexToString(clusters);

		final int pairSize = 2;
		final String[] initial = new String[pairSize];
		int addIndex = 0;

		// File Writer
		try {
			setupFileWriter();

		} catch (final IOException e) {
			System.out.println("FileWriter failed in KMeansCluster");
			e.printStackTrace();
		}

		initial[addIndex] = (axis == ClusterController.ROW) ? "ORF" : "ARRAY";
		addIndex++;

		initial[addIndex] = "GROUP";
		addIndex++;

		bufferedWriter.writeContent(initial);

		addIndex = 0;
		// Prepare data for writing
		for (int i = 0; i < geneGroups.length; i++) {

			final String[] group = geneGroups[i];

			int addIndexInner = 0;
			for (int j = 0; j < group.length; j++) {

				addIndexInner = 0;

				reorderedList[addIndex] = group[j];
				addIndex++;

				final String[] dataPair = new String[pairSize];
				final String index = Integer.toString(i);

				dataPair[addIndexInner] = group[j];
				addIndexInner++;

				dataPair[addIndexInner] = index;

				bufferedWriter.writeContent(dataPair);
			}
		}

		bufferedWriter.closeWriter();
	}

	public void setupFileWriter() throws IOException {

		String fileEnd = (axis == ClusterController.ROW) ? 
				"_K_G" + groupNum + ".kgg" : "_K_A" + groupNum + ".kag";

		final File file = new File(model.getSource().substring(0,
				model.getSource().length() - 4)
				+ fileEnd);

		file.createNewFile();

		bufferedWriter = new ClusterFileWriter(file);

		filePath = bufferedWriter.getFilePath();
	}

	// Cluster support methods
	/**
	 * Method to find the mean distances for every row element in the distance
	 * matrix.
	 * 
	 * @param matrix
	 * @return
	 */
	public double[] generateMeans(final double[][] matrix) {

		final double[] meanList = new double[matrix.length];

		int addIndex = 0;
		for (final double[] row : matrix) {

			double sum = 0;
			double mean;

			for (final Double d : row) {

				sum += d;
			}

			mean = sum / row.length;

			meanList[addIndex] = mean;
			addIndex++;
		}

		return meanList;
	}

	/**
	 * Find random seed clusters to begin assigning distance mean to. The number
	 * of seed clusters is user-specified and designated as "clusterN" in this
	 * class.
	 * 
	 * @param meanList
	 * @return
	 */
	public void setSeeds(final int seedNumber, final double[] meanList) {

		final int[][] seedClusters = new int[seedNumber][];
		final int[] indexList = new int[seedNumber];

		int addIndex = 0;
		for (int i = 0; i < seedNumber; i++) {

			final int[] cluster = new int[seedNumber];

			final int seedIndex = new Random().nextInt(meanList.length);

			// Making sure to exclude duplicates
			boolean same = false;
			for (final int e : indexList) {

				if (e == seedIndex) {

					i = i - 1;
					same = true;
					break;
				}
			}

			if (!same) {
				indexList[addIndex] = seedIndex;
				cluster[addIndex] = seedIndex;
				seedClusters[addIndex] = cluster;
				addIndex++;
			}
		}

		setSeedMeans(indexList, meanList);
	}

	/**
	 * This method assigns all the means from the distance matrix to the Voronoi
	 * space of a set of previously chosen means. Each mean is assigned to the
	 * closest 'seed mean'.
	 * 
	 * @param meanList
	 * @param seedClusters
	 * @return
	 */
	public int[][] assignMeansVals(final double[] meanList) {

		ClusterView.updatePBar(0);

		final List<List<Integer>> clusters = new ArrayList<List<Integer>>();

		// fill clusters List with clusterN-amount of empty lists to
		// avoid duplicates of the initial means later
		for (int i = 0; i < groupNum; i++) {

			final List<Integer> group = new ArrayList<Integer>();
			clusters.add(group);
		}

		// seedCluster has clusterN-amount of Integers designating the
		// initial means
		final double[] initialMeans = getSeedMeans();

		// Compare each mean from meanList to all seed means from seedClusters
		// and assign the means to the according group.
		for (int i = 0; i < meanList.length; i++) {

			final int geneID = i;
			final double mean = meanList[i];

			ClusterView.updatePBar(geneID);

			final int[] meanIndexes = new int[initialMeans.length];

			double bestD = Double.MAX_VALUE;
			double distance = 0;
			int bestInd = -1;

			int addIndexInner = 0;
			for (int j = 0; j < initialMeans.length; j++) {

				final double seed = initialMeans[j];

				distance = Math.abs(seed - mean);

				if (distance < bestD) {

					bestD = distance;
					meanIndexes[addIndexInner] = j;
					bestInd = j;
					addIndexInner++;
				} else {
					meanIndexes[addIndexInner] = bestInd;
					addIndexInner++;
				}
			}

			final int bestCluster = meanIndexes[meanIndexes.length - 1];

			// Add the current gene to appropriate cluster group
			clusters.get(bestCluster).add(geneID);
		}

		// Transform the list into arrays
		final int[][] clustersArray = new int[clusters.size()][];

		for (int i = 0; i < clusters.size(); i++) {

			final int[] cluster = new int[clusters.get(i).size()];

			for (int j = 0; j < cluster.length; j++) {

				cluster[j] = clusters.get(i).get(j);
			}

			clustersArray[i] = cluster;
		}

		// Check if sum of group sizes match the amount of overall means
		// for testing only
		// int sum = 0;
		// for (int i = 0; i < clustersArray.length; i++) {
		//
		// int[] group = clustersArray[i];
		//
		// sum += group.length;
		// System.out.println("Group Size " + i + ": " + group.length);
		// }
		//
		// if (sum == meanList.length) {
		// System.out.println("Success! sum and meanList size match up.");
		// System.out.println("Seed Means: " + Arrays.toString(getSeedMeans()));
		//
		// } else {
		// System.out.println("Something's weird.");
		// System.out.println("Sum: " + sum);
		// System.out.println("MeanList Size: " + meanList.length);
		// }

		return clustersArray;
	}

	/**
	 * Transforms the clusters of gene indexes to clusters of gene distance
	 * means in order to use the resulting list for calculation.
	 * 
	 * @param meanList
	 * @param clusters
	 * @return
	 */
	public double[][] indexesToMeans(final double[] meanList,
			final int[][] clusters) {

		final double[][] clusterMeans = new double[clusters.length][];

		int addIndex = 0;
		for (final int[] group : clusters) {

			final double[] meanCluster = new double[group.length];

			int addIndexInner = 0;
			for (final int gene : group) {

				final double mean = meanList[gene];
				meanCluster[addIndexInner] = mean;
				addIndexInner++;
			}

			clusterMeans[addIndex] = meanCluster;
			addIndex++;
		}

		return clusterMeans;
	}

	/**
	 * Finding new adjusted means for each cluster for later reassignment of all
	 * gene distance means which will create more accurate Voronoi spaces.
	 * 
	 * @param clusterMeans
	 * @return
	 */
	public void findNewMeans(final double[][] clusterMeans) {

		final double[] newSeedMeans = new double[clusterMeans.length];

		int addIndex = 0;
		// find new mean of each cluster
		for (final double[] group : clusterMeans) {

			double sum = 0;
			double newMean = 0;
			for (final Double mean : group) {

				sum += mean;
			}

			newMean = sum / group.length;

			newSeedMeans[addIndex] = newMean;
			addIndex++;
		}

		setSeedMeans(newSeedMeans);
	}

	/**
	 * This method uses the list of clusters composed of gene indexes to find
	 * the appropriate ORF names from the loaded model's headers.
	 * 
	 * @param clusters
	 * @return
	 */
	public String[][] indexToString(final int[][] clusters) {

		final String[][] stringClusters = new String[clusters.length][];

		String[][] headerArray;

		// Get the right header array
		if (axis == ClusterController.ROW) {
			headerArray = model.getArrayHeaderInfo().getHeaderArray();

		} else {
			headerArray = model.getGeneHeaderInfo().getHeaderArray();
		}

		final String[] geneNameArray = new String[headerArray.length];

		int addIndex = 0;
		for (final String[] element : headerArray) {

			geneNameArray[addIndex] = element[0];
			addIndex++;
		}

		addIndex = 0;
		for (final int[] group : clusters) {

			final String[] geneNames = new String[group.length];
			int addIndexInner = 0;
			for (final int mean : group) {

				final String gene = geneNameArray[mean];
				geneNames[addIndexInner] = gene;
				addIndexInner++;
			}

			stringClusters[addIndex] = geneNames;
			addIndex++;
		}

		return stringClusters;
	}

	// Other support methods
	/**
	 * Method to make deep copy of distance matrix
	 * 
	 * @return
	 */
	public double[][] deepCopy(final double[][] distanceMatrix) {

		final double[][] deepCopy = new double[distanceMatrix.length][];

		int addIndex = 0;
		for (int i = 0; i < distanceMatrix.length; i++) {

			final double[] newRow = new double[distanceMatrix[i].length];
			deepCopy[addIndex] = newRow;
			addIndex++;

			int addIndexInner = 0;
			for (int j = 0; j < distanceMatrix.length; j++) {

				final double e = distanceMatrix[i][j];
				newRow[addIndexInner] = e;
				addIndexInner++;
			}
		}

		return deepCopy;
	}

	// Setters
	/**
	 * Setter for the seed means for every cluster.
	 * 
	 * @param clusters
	 * @param elementMeanList
	 */
	public void setSeedMeans(final double[] newSeedMeans) {

		seedMeans = newSeedMeans;
	}

	/**
	 * Setter for the seed means for every cluster.
	 * 
	 * @param clusters
	 * @param elementMeanList
	 */
	public void setSeedMeans(final int[] clusters,
			final double[] elementMeanList) {

		// Clear seedMeans
		for (int i = 0; i < seedMeans.length; i++) {

			seedMeans[i] = -1;
		}

		// set new seed means
		int addIndex = 0;
		for (final int ind : clusters) {

			final double seedMean = elementMeanList[ind];
			seedMeans[addIndex] = seedMean;
			addIndex++;
		}
	}

	// Getters
	/**
	 * Accessor for the reordered list
	 * 
	 * @return
	 */
	public String[] getReorderedList() {

		return reorderedList;
	}

	/**
	 * Accessor for the seed means
	 * 
	 * @return
	 */
	public double[] getSeedMeans() {

		return seedMeans;
	}

	/**
	 * Accessor for the file path
	 * 
	 * @return
	 */
	public String getFilePath() {

		return filePath;
	}
}
