package Cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import Controllers.ClusterController;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * Performs the calculations of the k-means algorithm.
 * @author CKeil
 *
 */
public class KMeansCluster {

	private ClusterFileWriter bufferedWriter;		/* Writer to save data */
	
	/* Parameters */
	private int axis;
	private final int k;                     		/* Amount of clusters */

	/* Distance Matrix */
	private final DistanceMatrix distMatrix;

	/* Object to store deep copy of distance matrix; avoids mutation */
	private DistanceMatrix copyDistMatrix;

	/* Array which stores the seed means */
	private double[] seedMeans;
	
	private double[] rowCentroidList;
	private int[][] kClusters;
	private double[][] clusterMeans;

	/* Ordered axis element numbers to be returned for .cdt creation */
	private String[] reorderedList;

	/**
	 * Constructor for KMeansCluster. 
	 * @param headerArray Array of labels to be written in the cluster file.
	 * @param distMatrix The calculated distance matrix to be clustered.
	 * @param axis The matrix axis to be clustered.
	 * @param k The number of groups (k) to be formed.
	 * @param iterations The number iterations the k-means algorithm is
	 * run. This impacts the clustering result.
	 */
	public KMeansCluster(final DistanceMatrix distMatrix, final int axis, 
			final int k, final String fileName) {

		this.distMatrix = distMatrix;
		this.axis = axis;
		this.k = k;
		
		setupFileWriter(fileName);
		prepare();
	}
	
	/**
	 * Sets up a buffered writer used to save the data created during the
	 * process of k-means clustering.
	 * @throws IOException
	 */
	public void setupFileWriter(String fileName) {

		String fileEnd = (axis == ClusterController.ROW) ? 
				"_K_G" + k + ".kgg" : "_K_A" + k + ".kag";

		final File file = new File(fileName + fileEnd);

		try {
			file.createNewFile();
			bufferedWriter = new ClusterFileWriter(file);
			
		} catch (IOException e) {
			LogBuffer.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	/** 
	 * Sets up important instance variables so that k-means calculation can
	 * begin.
	 */
	public void prepare() {
		
		rowCentroidList = new double[distMatrix.getSize()];
		kClusters = new int[k][];
		clusterMeans = new double[distMatrix.getSize()][];

		/* deep copy of distance matrix to avoid mutation */
		copyDistMatrix = new DistanceMatrix(0);
		copyDistMatrix.cloneFrom(distMatrix);

		/* Make a list of all means of distances for every gene */
		rowCentroidList = generateCentroids(copyDistMatrix);

		/* Seeds array should be of clusterN-size */
		setSeeds(k, rowCentroidList);

		/* Assign the centroids of each row to their closest seed cluster.*/
		kClusters = assignToNearestCluster(rowCentroidList);
		clusterMeans = indicesToMeans(rowCentroidList, kClusters);
	}
	
	/**
	 * Runs operations for k-means clustering.
	 */
	public void cluster() {

		computeNewCentroids(clusterMeans);

		kClusters = assignToNearestCluster(rowCentroidList);
		clusterMeans = indicesToMeans(rowCentroidList, kClusters);
	}
	
	/**
	 * Generates the KAG or KGG file (depends on chosen axis) 
	 * from the calculated data. 
	 * @param kClusters The k-clusters formed by the calculation.
	 * @param headerArray Matrix labels from the tvModel.
	 */
	public void writeData(int[][] kClusters, String[][] headerArray) {
		
		/* The list containing the reordered gene names. */
		reorderedList = new String[distMatrix.getSize()];
		
		String[][] kClusters_string = new String[kClusters.length][];

		kClusters_string = indexToString(kClusters, headerArray);

		final int pairSize = 2;
		final String[] initial = new String[pairSize];
		
		int addIndex = 0;

		/* Setting up header line */
		initial[addIndex] = (axis == ClusterController.ROW) ? "ORF" : "ARRAY";
		addIndex++;

		initial[addIndex] = "GROUP";
		addIndex++;

		/* Writing the header line */
		bufferedWriter.writeContent(initial);

		/* Write the calculated data */
		addIndex = 0;
		for (int i = 0; i < kClusters_string.length; i++) {

			final String[] cluster = kClusters_string[i];

			int addIndexInner = 0;
			for (int j = 0; j < cluster.length; j++) {

				addIndexInner = 0;

				reorderedList[addIndex] = cluster[j];
				addIndex++;

				final String[] dataPair = new String[pairSize];
				final String index = Integer.toString(i);

				dataPair[addIndexInner] = cluster[j];
				addIndexInner++;

				dataPair[addIndexInner] = index;

				bufferedWriter.writeContent(dataPair);
			}
		}
	}
	
	public void finish(String[][] headerArray) {
		
		writeData(kClusters, headerArray);
		bufferedWriter.closeWriter();
	}

	/**
	 * Finds the mean of all distance values in every row element of 
	 * the distance matrix.
	 * TODO Adapt to half-matrix....
	 * @param matrix
	 * @return Array of means/ centroids.
	 */
	private double[] generateCentroids(final DistanceMatrix matrix) {

		final double[] centroidList = new double[matrix.getSize()];

		/* 
		 * This algorithm gets the mean of each row for the half-matrix!
		 * Hence, the row can't just be iterated, since values are missing.
		 */
		int addIndex = 0;
		for (int i = 0; i < matrix.getSize(); i++) {

			double[] row = matrix.getRow(i);
			double sum = 0;
			double mean;

			/* Add the row values to sum */
			for (final double d : row) {

				sum += d;
			}
			
			/* 
			 * Add the rest, which is the column values of each row at the
			 * index of the current.
			 */
			for(int j = addIndex; j < matrix.getSize() - 1; j++) {
				
				sum += matrix.getRow(j + 1)[addIndex];
			}

			mean = sum / matrix.getSize();

			centroidList[addIndex] = mean;
			addIndex++;
		}

		return centroidList;
	}

	/**
	 * Find random seed clusters to begin assigning distance mean to. The number
	 * of seed clusters is user-specified and designated as "clusterN" in this
	 * class.
	 * 
	 * @param meanList
	 * @return
	 */
	private void setSeeds(final int seedNumber, final double[] meanList) {

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
	 * Assigns all the means from the distance matrix to the Voronoi space 
	 * of a set of previously chosen means. Each mean is assigned to the
	 * closest 'seed mean'. Partitions the data into k clusters.
	 * 
	 * @param meanList
	 * @param seedClusters
	 * @return
	 */
	private int[][] assignToNearestCluster(final double[] meanList) {

		final List<List<Integer>> clusters = new ArrayList<List<Integer>>();

		/* 
		 * Fill clusters list with k-amount of empty lists to 
		 * avoid duplicates of the initial means later.
		 */
		for (int i = 0; i < k; i++) {

			final List<Integer> group = new ArrayList<Integer>();
			clusters.add(group);
		}

		/* 
		 * seedCluster has k-amount of Integers designating 
		 * the initial means
		 */
		final double[] initialMeans = seedMeans;

		/* 
		 * Compare each mean from meanList to all seed means from 
		 * seedClusters and assign the means to the according group.
		 */
		for (int i = 0; i < meanList.length; i++) {

			final int geneID = i;
			final double mean = meanList[i];

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

			/* Add the current gene to appropriate cluster group */
			clusters.get(bestCluster).add(geneID);
		}

		/* Transform the list into arrays */
		final int[][] clustersArray = new int[clusters.size()][];

		for (int i = 0; i < clusters.size(); i++) {

			final int[] cluster = new int[clusters.get(i).size()];

			for (int j = 0; j < cluster.length; j++) {

				cluster[j] = clusters.get(i).get(j);
			}

			clustersArray[i] = cluster;
		}

		return clustersArray;
	}

	/**
	 * Uses centroidList and kClusters to build another array of the clusters
	 * which contains the centroids of the distance matrix rows in place of
	 * the row indices. This list is needed to calculate the new centroids
	 * during the next iteration, if there is one.
	 * 
	 * @param centroidList
	 * @param kClusters
	 * @return
	 */
	private double[][] indicesToMeans(final double[] centroidList, 
			final int[][] kClusters) {

		final double[][] clusterMeans = new double[kClusters.length][];

		int addIndex = 0;
		for (final int[] cluster : kClusters) {

			final double[] meanCluster = new double[cluster.length];

			int addIndexInner = 0;
			for (final int row : cluster) {

				meanCluster[addIndexInner] = centroidList[row];
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
	private void computeNewCentroids(final double[][] clusterMeans) {

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
	 * @param kClusters
	 * @return
	 */
	private String[][] indexToString(final int[][] kClusters, 
			String[][] headerArray) {

		final String[][] kClusters_string = new String[kClusters.length][];
		final String[] axisLabelArray = new String[headerArray.length];

		/* Get the name label only for each row */
		int addIndex = 0;
		for (final String[] rowHeader : headerArray) {

			axisLabelArray[addIndex] = rowHeader[0];
			addIndex++;
		}

		/* Replace the integers in kClusters with the name label strings */
		addIndex = 0;
		for (final int[] cluster : kClusters) {

			final String[] geneNames = new String[cluster.length];
			
			int addIndexInner = 0;
			for (final int mean : cluster) {

				final String label = axisLabelArray[mean];
				geneNames[addIndexInner] = label;
				addIndexInner++;
			}

			kClusters_string[addIndex] = geneNames;
			addIndex++;
		}

		return kClusters_string;
	}

	/* Setters */
	/**
	 * Setter for the seed means for every cluster.
	 * 
	 * @param clusters
	 * @param elementMeanList
	 */
	private void setSeedMeans(final double[] newSeedMeans) {

		this.seedMeans = newSeedMeans;
	}

	/**
	 * Setter for the seed means for every cluster.
	 * 
	 * @param clusters
	 * @param elementMeanList
	 */
	private void setSeedMeans(final int[] clusters,
			final double[] elementMeanList) {

		this.seedMeans = new double[k];

		/* Set new seed means */
		int addIndex = 0;
		for (final int ind : clusters) {

			final double seedMean = elementMeanList[ind];
			seedMeans[addIndex] = seedMean;
			addIndex++;
		}
	}

	/* Getters */
	/**
	 * Getter for the reordered list
	 * 
	 * @return
	 */
	public String[] getReorderedList() {

		return reorderedList;
	}
}
