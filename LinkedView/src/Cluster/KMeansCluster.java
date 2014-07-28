package Cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.LogBuffer;

public class KMeansCluster {

	// Instance variables
	private final DataModel model;
	private final ClusterView clusterView;

	private ClusterFileWriter bufferedWriter;
	private String filePath;
	private String type = "";
	private final int clusterN;
	private final int iterations;

	// Distance Matrix
	private final double[][] dMatrix;

	// Half of the Distance Matrix (symmetry)
	private double[][] copyDMatrix;

	// list to return ordered GENE numbers for .cdt creation
	private double[] seedMeans;

	// list to return ordered GENE numbers for .cdt creation
	private String[] reorderedList;

	private final SwingWorker<String[], Void> worker;

	/**
	 * Main constructor
	 * 
	 * @param model
	 * @param dMatrix
	 * @param pBar
	 * @param type
	 * @param method
	 */
	public KMeansCluster(final DataModel model, final ClusterView clusterView,
			final double[][] dMatrix, final String type, final int clusterN,
			final int iterations, final SwingWorker<String[], Void> worker) {

		this.model = model;
		this.clusterView = clusterView;
		this.dMatrix = dMatrix;
		this.type = type;
		this.clusterN = clusterN;
		this.iterations = iterations;
		this.worker = worker;
	}

	// method for clustering the distance matrix
	public void cluster() {

		// Just checking for debugging
		LogBuffer.println("Is KMeansCluster.cluster() on EDT? " 
				+ SwingUtilities.isEventDispatchThread());
				
		double[] elementMeanList = new double[dMatrix.length];
		int[][] clusters = new int[clusterN][];
		double[][] clusterMeans = new double[dMatrix.length][];

		// ProgressBar maximum
		clusterView.setLoadText("Clustering data...");
		clusterView.setPBarMax(dMatrix.length);

		// deep copy of distance matrix to avoid mutation
		copyDMatrix = deepCopy(dMatrix);

		// The list containing the reordered gene names.
		reorderedList = new String[dMatrix.length];

		// Make a list of all means of distances for every gene
		elementMeanList = generateMeans(copyDMatrix);

		// List of seeds should be of clusterN-size
		seedMeans = new double[clusterN];
		setSeeds(clusterN, elementMeanList);

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

		if (type.equalsIgnoreCase("Gene")) {
			initial[addIndex] = "ORF";
			addIndex++;

		} else {
			initial[addIndex] = "ARRAY";
			addIndex++;
		}

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

		String fileEnd;

		if (type.equalsIgnoreCase("Gene")) {
			fileEnd = "_K_G" + clusterN + ".kgg";

		} else {
			fileEnd = "_K_A" + clusterN + ".kag";
		}

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

		clusterView.updatePBar(0);

		final List<List<Integer>> clusters = new ArrayList<List<Integer>>();

		// fill clusters List with clusterN-amount of empty lists to
		// avoid duplicates of the initial means later
		for (int i = 0; i < clusterN; i++) {

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

			clusterView.updatePBar(geneID);

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
		if (getType().equalsIgnoreCase("Arry")) {
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

	/**
	 * Accessor for the type of the current object instance
	 * 
	 * @return
	 */
	public String getType() {

		return type;
	}
}
