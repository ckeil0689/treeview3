package Cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.model.TVModel;

public class KMeansCluster {

	// Instance variables
	private final TVModel model;
	private final ClusterView clusterView;
	
	private String filePath;
	private String type = "";
	private final int pBarNum;
	private final int clusterN;
	private final int iterations;

	// Distance Matrix
	private List<List<Double>> dMatrix = new ArrayList<List<Double>>();

	// Half of the Distance Matrix (symmetry)
	private List<List<Double>> copyDMatrix;

	// list to return ordered GENE numbers for .cdt creation
	private List<Double> seedMeans = new ArrayList<Double>();

	// list to return ordered GENE numbers for .cdt creation
	private final List<String> reorderedList = new ArrayList<String>();

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
			final List<List<Double>> dMatrix, final String type, int clusterN, 
			int iterations, final int pBarNum) {

		this.model = (TVModel) model;
		this.clusterView = clusterView;
		this.dMatrix = dMatrix;
		this.type = type;
		this.clusterN = clusterN;
		this.iterations = iterations;
		this.pBarNum = pBarNum;
	}

	// method for clustering the distance matrix
	public void cluster() {

		List<Double> elementMeanList = new ArrayList<Double>();
		List<List<Integer>> clusters = new ArrayList<List<Integer>>();
		List<List<Double>> clusterMeans = new ArrayList<List<Double>>();

		// ProgressBar maximum
		clusterView.setPBarMax(dMatrix.size(), pBarNum);

		// deep copy of distance matrix to avoid mutation
		copyDMatrix = deepCopy(dMatrix);

		// Make a list of all means of distances for every gene
		elementMeanList = generateMeans(copyDMatrix);

		// List of seeds should be of clusterN-size
		setSeeds(clusterN, elementMeanList);

		// Use the seedClusters to define Voronoi spaces. This means, assign
		// the distance mean of each gene to the closest seed cluster.
		clusters = assignMeansVals(elementMeanList);
		clusterMeans = indexesToMeans(elementMeanList, clusters);

		// Begin iteration of recalculating means and reassigning gene
		// distance means to clusters
		for (int i = 0; i < iterations; i++) {

			findNewMeans(clusterMeans);

			clusters = assignMeansVals(elementMeanList);
			clusterMeans = indexesToMeans(elementMeanList, clusters);
		}

		// Next Step: Generate CDT and KGG Excel file
		List<List<String>> geneGroups = new ArrayList<List<String>>();

		geneGroups = indexToString(clusters);

		final List<List<String>> clusterData = new ArrayList<List<String>>();

		final List<String> initial = new ArrayList<String>();

		String fileEnd;

		if (type.equalsIgnoreCase("Gene")) {
			fileEnd = "_K_G" + clusterN + ".kgg";
			initial.add("ORF");

		} else {
			fileEnd = "_K_A" + clusterN + ".kag";
			initial.add("ARRAY");
		}

		initial.add("GROUP");
		clusterData.add(initial);

		// Prepare data for writing
		for (final List<String> group : geneGroups) {

			reorderedList.addAll(group);

			for (final String element : group) {

				final List<String> dataPair = new ArrayList<String>();
				final String index = Integer
						.toString(geneGroups.indexOf(group));

				dataPair.add(element);
				dataPair.add(index);

				clusterData.add(dataPair);
			}
		}

		final ClusterFileWriter fw = new ClusterFileWriter(model);

		fw.writeFile(clusterData, fileEnd);
	}

	// Cluster support methods
	/**
	 * Method to find the mean distances for every row element in the distance
	 * matrix.
	 * 
	 * @param matrix
	 * @return
	 */
	public List<Double> generateMeans(final List<List<Double>> matrix) {

		final List<Double> meanList = new ArrayList<Double>();

		for (final List<Double> row : matrix) {

			double sum = 0;
			double mean;

			for (final Double d : row) {

				sum += d;
			}

			mean = sum / row.size();

			meanList.add(mean);
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
	public void setSeeds(final int seedNumber, final List<Double> meanList) {

		final List<List<Integer>> seedClusters = new ArrayList<List<Integer>>();
		final List<Integer> indexList = new ArrayList<Integer>();

		for (int i = 0; i < seedNumber; i++) {

			final List<Integer> cluster = new ArrayList<Integer>();

			final int seedIndex = new Random().nextInt(meanList.size());

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
				indexList.add(seedIndex);
				cluster.add(seedIndex);
				seedClusters.add(cluster);
			}
		}

		setSeedMeans(seedClusters, meanList);
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
	public List<List<Integer>> assignMeansVals(final List<Double> meanList) {

		clusterView.updatePBar(0, pBarNum);

		final List<List<Integer>> clusters = new ArrayList<List<Integer>>();

		// fill clusters List with clusterN-amount of empty lists to
		// avoid duplicates of the initial means later
		for (int i = 0; i < clusterN; i++) {

			final List<Integer> group = new ArrayList<Integer>();

			clusters.add(group);
		}

		// seedCluster has clusterN-amount of Integers designating the
		// initial means
		final List<Double> initialMeans = getSeedMeans();

		// Compare each mean from meanList to all seed means from seedClusters
		// and assign the means to the according group.
		for (final Double mean : meanList) {

			final int geneID = meanList.indexOf(mean);

			clusterView.updatePBar(geneID, pBarNum);

			final List<Integer> meanIndexes = new ArrayList<Integer>();

			double bestD = Double.MAX_VALUE;
			double distance = 0;

			for (final Double seed : initialMeans) {

				distance = Math.abs(seed - mean);

				if (distance < bestD) {

					bestD = distance;
					meanIndexes.add(initialMeans.indexOf(seed));
				}
			}

			final int bestCluster = meanIndexes.get(meanIndexes.size() - 1);

			// Add the current gene to appropriate cluster group
			clusters.get(bestCluster).add(geneID);
		}

		// Check if sum of group sizes match the amount of overall means
		// for testing only
		int sum = 0;
		for (final List<Integer> group : clusters) {

			sum += group.size();
			System.out.println("Group Size " + clusters.indexOf(group) + ": "
					+ group.size());
		}

		if (sum == meanList.size()) {
			System.out.println("Success! sum and meanList size match up.");
			System.out.println("Seed Means: " + getSeedMeans());

		} else {
			System.out.println("Something's weird.");
			System.out.println("Sum: " + sum);
			System.out.println("MeanList Size: " + meanList.size());
		}

		return clusters;
	}

	/**
	 * Transforms the clusters of gene indexes to clusters of gene distance
	 * means in order to use the resulting list for calculation.
	 * 
	 * @param meanList
	 * @param clusters
	 * @return
	 */
	public List<List<Double>> indexesToMeans(final List<Double> meanList,
			final List<List<Integer>> clusters) {

		final List<List<Double>> clusterMeans = new ArrayList<List<Double>>();

		for (final List<Integer> group : clusters) {

			final List<Double> meanCluster = new ArrayList<Double>();

			for (final int gene : group) {

				final double mean = meanList.get(gene);
				meanCluster.add(mean);
			}

			clusterMeans.add(meanCluster);
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
	public void findNewMeans(final List<List<Double>> clusterMeans) {

		final List<Double> newSeedMeans = new ArrayList<Double>();

		// find new mean of each cluster
		for (final List<Double> group : clusterMeans) {

			double sum = 0;
			double newMean = 0;
			for (final Double mean : group) {

				sum += mean;
			}

			newMean = sum / group.size();

			newSeedMeans.add(newMean);
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
	public List<List<String>> indexToString(final List<List<Integer>> clusters) {

		final List<List<String>> stringClusters = new ArrayList<List<String>>();

		String[][] geneNameArray;

		final List<String> geneNameList = new ArrayList<String>();

		// Get the right header array
		if (getType().equalsIgnoreCase("Arry")) {
			geneNameArray = model.getArrayHeaderInfo().getHeaderArray();

		} else {
			geneNameArray = model.getGeneHeaderInfo().getHeaderArray();
		}

		for (final String[] element : geneNameArray) {

			geneNameList.add(element[0]);
		}

		for (final List<Integer> group : clusters) {

			final List<String> geneNames = new ArrayList<String>();

			for (final int mean : group) {

				final String gene = geneNameList.get(mean);
				geneNames.add(gene);
			}

			stringClusters.add(geneNames);
		}

		return stringClusters;
	}

	// Other support methods
	/**
	 * Method to make deep copy of distance matrix
	 * 
	 * @return
	 */
	public List<List<Double>> deepCopy(final List<List<Double>> distanceMatrix) {

		final List<List<Double>> deepCopy = new ArrayList<List<Double>>();

		for (int i = 0; i < distanceMatrix.size(); i++) {

			final List<Double> newList = new ArrayList<Double>();
			deepCopy.add(newList);

			for (int j = 0; j < distanceMatrix.size(); j++) {

				final Double e = new Double(distanceMatrix.get(i).get(j));
				newList.add(e);
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
	public void setSeedMeans(final List<Double> newSeedMeans) {

		seedMeans = newSeedMeans;
	}

	/**
	 * Setter for the seed means for every cluster.
	 * 
	 * @param clusters
	 * @param elementMeanList
	 */
	public void setSeedMeans(final List<List<Integer>> clusters,
			final List<Double> elementMeanList) {

		seedMeans.clear();

		for (final List<Integer> group : clusters) {

			final double seedMean = elementMeanList.get(group.get(0));
			seedMeans.add(seedMean);
		}
	}

	// Getters
	/**
	 * Accessor for the reordered list
	 * 
	 * @return
	 */
	public List<String> getReorderedList() {

		return reorderedList;
	}

	/**
	 * Accessor for the seed means
	 * 
	 * @return
	 */
	public List<Double> getSeedMeans() {

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
