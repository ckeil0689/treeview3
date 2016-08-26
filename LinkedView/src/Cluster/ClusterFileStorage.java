package Cluster;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import edu.stanford.genetics.treeview.LogBuffer;

/** This class determines the file name structure (full path) for a file
 * that will be written during clustering. The name will be dependent on
 * the linkage type (avg, single, complete) and what kind of file will
 * be created. */
public class ClusterFileStorage {

	private static final String FILE_EXT = ".cdt";

	private static final String KMEANS_DESIGNATOR = "_K";
	private static final String KMEANS_ROW_SUFFIX = "_G";
	private static final String KMEANS_COL_SUFFIX = "_A";

	/** Create a file obejct from the passed arguments.
	 * 
	 * @param filePath - The file path for the new file.
	 * @param fileEnd - The file type / file end for the new file.
	 * @param linkMethod - The linkage method used during clustering which will
	 *          be added to the file name for better distinction of files.
	 * @return A new file object */
	public static File createFile(final String filePath, final String fileEnd,
																final int linkMethod) {

		if((filePath == null) || (fileEnd == null)) {
			LogBuffer.println("Cannot create file. FilePath or fileEnd was null.");
			return null;
		}

		LogBuffer.println(">>>>> " + fileEnd);
		LogBuffer.println("filePath: " + filePath);
		final Path path = Paths.get(filePath);

		if(path == null) {
			LogBuffer.println("Cannot create file. Path could not be resolved.");
			return null;
		}

		final Path fileNameP = path.getFileName();
		final Path rootDirP = path.getParent();

		if((fileNameP == null) || (rootDirP == null)) {
			LogBuffer.println("Cannot create file. File name or root directory " +
												"could not be resolved.");
			return null;
		}

		final String fileName = ClusterFileStorage.getRootFileName(fileNameP.toString());
		final String rootDir = rootDirP.toString() + File.separatorChar;

		final String linkName = ClusterFileStorage.getLinkName(linkMethod);
		final String subDir = ClusterFileStorage.createSubDir(rootDir, fileName, linkName);

		LogBuffer.println("RootDir: " + rootDir);
		LogBuffer.println("Created subDir: " + subDir);
		LogBuffer.println("Found file name: " + fileName);

		return ClusterFileStorage.retrieveFile(subDir, fileName, linkName, fileEnd);
	}

	/** Returns a cluster linkage name String that matches the passed linkage
	 * type.
	 * 
	 * @param link - Integer representation of the cluster linkage type.
	 * @return A String name for the cluster linkage type. */
	private static String getLinkName(final int link) {

		String linkName;
		switch(link) {

			case Linker.SINGLE:
				linkName = "single";
				break;
			case Linker.AVG:
				linkName = "average";
				break;
			case Linker.COMPLETE:
				linkName = "complete";
				break;
			case KMeansCluster.KMEANS:
				linkName = "kmeans";
				break;
			default:
				linkName = "no_link";
				break;
		}

		return linkName;
	}

	/** Figure out the original name from the name of a TreeView3-clustered
	 * file by extracting cluster linkage type suffixes from the supplied
	 * file name.
	 * 
	 * @param fileName - Name of the file.
	 * @return If the file was clustered by TreeView3 and presents the resulting
	 *         name with a cluster linkage suffix, it will be stripped of that
	 *         suffix and
	 *         returned. Otherwise the original name is simply returned again. */
	private static String getRootFileName(final String fileName) {

		final String single_suff = "_single";
		final String complete_suff = "_complete";
		final String average_suff = "_average";
		final String kmeans_suff = "_kmeans";

		final int start_s = fileName.indexOf(single_suff, 0);
		final int start_c = fileName.indexOf(complete_suff, 0);
		final int start_a = fileName.indexOf(average_suff, 0);
		final int start_k = fileName.indexOf(kmeans_suff, 0);

		int end = fileName.length();
		if(start_s != -1) {
			end = start_s;

		}
		else if(start_c != -1) {
			end = start_c;

		}
		else if(start_a != -1) {
			end = start_a;

		}
		else if(start_k != -1) {
			end = start_k;
		}

		return fileName.substring(0, end);
	}

	/** Creates a folder with the general file name to store all variations and
	 * subfiles of clustering in one folder.
	 *
	 * @param fileDir
	 *          The directory plus file name without file type.
	 * @return The main file's directory. */
	private static String createSubDir(	final String rootDir,
																			final String fileName,
																			final String linkName) {

		final Path subdir = Paths.get(rootDir, fileName, linkName);
		LogBuffer.println("Checking for this directory: " + subdir.toString());
		final File file = subdir.toFile();

		// Create folder if it does not exist
		if(!(file.exists() && file.isDirectory())) {
			file.mkdirs();
		}

		return subdir.toString();
	}

	/** Creates a new file from based on the passed directory, file name, link
	 * name, and file end.
	 * 
	 * @param dir - The directory for the new file
	 * @param fileName - The name for the new file
	 * @param linkName - The linkage type component for the new file name
	 * @param fileEnd - The file ending
	 * @return A newly created File object. */
	private static File retrieveFile(	final String dir, String fileName,
																		final String linkName,
																		final String fileEnd) {

		fileName += "_" + linkName;
		File tempFile = Paths.get(dir, fileName, fileEnd).toFile();

		try {
			// Do not overwrite at the moment
			tempFile = ClusterFileStorage.getNewFile(dir, fileName, fileEnd);
			tempFile.createNewFile();

		}
		catch(final IOException e) {
			LogBuffer.logException(e);
		}

		return tempFile;
	}

	/** Sets up a new file object based on the passed components. Makes sure
	 * the file has a unique name by adding an integer to the name which
	 * increments when files with the same base name and linkage type string
	 * are encountered. For example, if "originFile_single.cdt" exists and a new
	 * file is created for single linkage clustering, it will be named
	 * "originFile_single_1.cdt"
	 * 
	 * @param dir - The directory in which the file will reside.
	 * @param oldName - The original name for the file which is used as a basis.
	 * @param fileEnd - The original file ending.
	 * @return A new file object. */
	private static File getNewFile(	final String dir, final String oldName,
																	final String fileEnd) {

		String fileDescr = oldName + fileEnd;
		File file = Paths.get(dir, fileDescr).toFile();

		/*
		 * Even for gtr and atr files, the cdt files are the ones that should
		 * be exclusively counted. While single axes might be clustered,
		 * if the user hits cancel during clustering, no .cdt file will exist.
		 */
		String cdtFileDescr = oldName + ".cdt";
		File cdtFile = Paths.get(dir, cdtFileDescr).toFile();

		int fileCount = 0;
		String cdtSuffix;
		String suffix;
		while(cdtFile.exists()) {
			fileCount++;

			cdtSuffix = "_" + fileCount + ".cdt";
			cdtFileDescr = oldName + cdtSuffix;
			cdtFile = Paths.get(dir, cdtFileDescr).toFile();

			suffix = "_" + fileCount + fileEnd;
			fileDescr = oldName + suffix;
			file = Paths.get(dir, fileDescr).toFile();
		}

		cdtFile = null;

		return file;
	}

	/** Figures out which file extension to use for the passed arguments. This
	 * is especially necessary to create k-means files using the Cluster 3.0
	 * naming convention where file names depended on chosen number of clusters
	 * and chosen number of iterations.
	 * 
	 * @param isHier - Whether clustering was hierarchical or not.
	 * @param spinnerInput - Input for K-Means clustering, such as number of
	 *          clusters.
	 * @param rowClusterData - The specific cluster data for the row axis.
	 * @param colClusterData - The specific cluster data for the column axis.
	 * @return A String representing a file extension. */
	public static String determineClusterFileExt(	final boolean isHier,
																								final Integer[] spinnerInput,
																								final ClusteredAxisData rowClusterData,
																								final ClusteredAxisData colClusterData) {
		String fileEnd = "";

		if(isHier) {
			fileEnd = ClusterFileStorage.FILE_EXT;

			// k-means file names have a few more details
		}
		else {
			String rowC = "";
			String colC = "";

			final int row_clusterN = spinnerInput[0];
			final int col_clusterN = spinnerInput[2];

			final String[] orderedGIDs = rowClusterData.getReorderedIDs();
			final String[] orderedAIDs = colClusterData.getReorderedIDs();

			if((orderedGIDs != null) && (orderedGIDs.length > 0)) {
				rowC = ClusterFileStorage.KMEANS_ROW_SUFFIX + row_clusterN;
			}

			if((orderedAIDs != null) && (orderedAIDs.length > 0)) {
				colC = ClusterFileStorage.KMEANS_COL_SUFFIX + col_clusterN;
			}

			fileEnd = ClusterFileStorage.KMEANS_DESIGNATOR +rowC + colC +
								ClusterFileStorage.FILE_EXT;
		}

		return fileEnd;
	}
}
