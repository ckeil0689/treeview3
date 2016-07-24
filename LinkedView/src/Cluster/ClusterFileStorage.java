package Cluster;

import java.io.File;
import java.io.IOException;

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * This class determines the file name structure (full path) for a file 
 * that will be written during clustering. The name will be dependent on 
 * the linkage type (avg, single, complete) and what kind of file will 
 * be created.
 */
public class ClusterFileStorage {

	private final String FILE_EXT = ".cdt";
	
	private final String KMEANS_DESIGNATOR = "_K";
	private final String KMEANS_ROW_SUFFIX = "_G";
	private final String KMEANS_COL_SUFFIX = "_A";
	
	/**
	 * Create a file obejct from the passed arguments.
	 * @param filePath - The file path for the new file.
	 * @param fileEnd - The file type / file end for the new file.
	 * @param linkMethod - The linkage method used during clustering which will
	 * be added to the file name for better distinction of files.
	 * @return A new file object
	 */
	public File createFile(final String filePath, final String fileEnd,
			final int linkMethod) {
		
		String linkName = getLinkName(linkMethod);
		String fileName = getFileName(filePath);
		String rootDir = getRootDir(filePath, fileName);
		String subDir = setLinkDir(rootDir, fileName, linkName);
		
		return retrieveFile(subDir, fileName, linkName, fileEnd);
	}

	/**
	 * Returns a cluster linkage name String that matches the passed linkage type. 
	 * @param link - Integer representation of the cluster linkage type.
	 * @return A String name for the cluster linkage type.
	 */
	private static String getLinkName(int link) {

		String linkName;
		switch (link) {

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

	/**
	 * Gets the name of a file from a file path String by finding the last
	 * occurrence of a File.separatorChar and using it as a startIndex to
	 * extract a substring of the file name + extension which is passed to
	 * getRootFileName() where the file name will be determined.
	 * @param filePath - The path of a file for which to extract the name.
	 * @return The name of a file.
	 */
	private static String getFileName(final String filePath) {

		// Substring starts one right from last separator char
		int startIdx = getLastFileSeparatorPos(filePath) + 1;

		return getRootFileName(filePath.substring(startIdx, filePath.length()));
	}

	/**
	 * Extracts the path to a directory in which the file with the supplied name 
	 * is present. 
	 * @param filePath - The full path to the file.
	 * @param fileName - The name of the file.
	 * @return The original filePath String shortened by the name of the file.
	 */
	private static String getRootDir(final String filePath, 
	                                 final String fileName) {

		int first = filePath.indexOf(fileName, 0);

		return filePath.substring(0, first);
	}

	/**
	 * Figure out the original name from the name of a TreeView3-clustered
	 * file by extracting cluster linkage type suffixes from the supplied
	 * file name.
	 * @param fileName - Name of the file.
	 * @return If the file was clustered by TreeView3 and presents the resulting
	 * name with a cluster linkage suffix, it will be stripped of that suffix and
	 * returned. Otherwise the original name is simply returned again.
	 */
	private static String getRootFileName(final String fileName) {

		String single_suff = "_single";
		String complete_suff = "_complete";
		String average_suff = "_average";
		String kmeans_suff = "_kmeans";

		int start_s = fileName.indexOf(single_suff, 0);
		int start_c = fileName.indexOf(complete_suff, 0);
		int start_a = fileName.indexOf(average_suff, 0);
		int start_k = fileName.indexOf(kmeans_suff, 0);

		int end = fileName.length();
		if (start_s != -1) {
			end = start_s;
			
		} else if (start_c != -1) {
			end = start_c;
			
		} else if (start_a != -1) {
			end = start_a;
			
		} else if (start_k != -1) {
			end = start_k;
		}

		return fileName.substring(0, end);
	}

	/**
	 * Creates a folder with the general file name to store all variations and
	 * subfiles of clustering in one folder.
	 * 
	 * @param fileDir
	 *            The directory plus file name without file type.
	 * @return The main file's directory.
	 */
	private static String setLinkDir(final String rootDir, final String fileName, 
			final String linkName) {

		String linkSubDir = rootDir + fileName + File.separator + linkName;
		File file = new File(linkSubDir);

		// Create folder if it does not exist
		if (!(file.exists() && file.isDirectory())) {
			file.mkdirs();
		}

		return linkSubDir += File.separator;
	}

	/**
	 * Creates a new file from based on the passed directory, file name, link
	 * name, and file end.  
	 * @param dir - The directory for the new file
	 * @param fileName - The name for the new file
	 * @param linkName - The linkage type component for the new file name
	 * @param fileEnd - The file ending
	 * @return A newly created File object.
	 */
	private File retrieveFile(final String dir, String fileName, 
	                          final String linkName, final String fileEnd) {

		fileName += "_" + linkName;
		String fullFileID = dir + fileName + fileEnd;
		
		File tempFile = new File(fullFileID);
		
		try {
			// Do not overwrite at the moment
			tempFile = getNewFile(dir, fileName, fileEnd);
			tempFile.createNewFile();
			
		} catch (IOException e) {
			LogBuffer.logException(e);
		}

		return tempFile;
	}

	/**
	 * Sets up a new file object based on the passed components. Makes sure
	 * the file has a unique name by adding an integer to the name which 
	 * increments when files with the same base name and linkage type string
	 * are encountered. For example, if "originFile_single.cdt" exists and a new
	 * file is created for single linkage clustering, it will be named 
	 * "originFile_single_1.cdt"
	 * @param dir - The directory in which the file will reside.
	 * @param oldName - The original name for the file which is used as a basis.
	 * @param fileEnd - The original file ending.
	 * @return A new file object.
	 */
	private static File getNewFile(String dir, String oldName, String fileEnd) {

		File file = new File(dir + oldName + fileEnd);

		/*
		 * Even for gtr and atr files, the cdt files are the ones that should 
		 * be exclusively counted. While single axes might be clustered, 
		 * if the user hits cancel during clustering, no .cdt file will exist. 
		 */
		File cdtFile = new File(dir + oldName + ".cdt");

		int fileCount = 0;

		while (cdtFile.exists()) {
			fileCount++;
			cdtFile = new File(dir + oldName + "_" + fileCount + ".cdt");
			file = new File(dir + oldName + "_" + fileCount + fileEnd);
		}

		cdtFile = null;

		return file;
	}
	
	/**
	 * Figures out which file extension to use for the passed arguments.
	 * @param isHier - Whether clustering was hierarchical or not.
	 * @param spinnerInput - Input for K-Means clustering, such as number of
	 * clusters.
	 * @param rowClusterData - The specific cluster data for the row axis. 
	 * @param colClusterData - The specific cluster data for the column axis.
	 * @return A String representing a file extension.
	 */
  public String getClusterFileExtension(final boolean isHier, 
                                        final Integer[] spinnerInput, 
                                        final ClusteredAxisData rowClusterData, 
                                        final ClusteredAxisData colClusterData) 
  {
    String fileEnd = "";

		if (isHier) {
			fileEnd = FILE_EXT;

		// k-means file names have a few more details	
		} else {
			String rowC = "";
			String colC = "";

			final int row_clusterN = spinnerInput[0];
			final int col_clusterN = spinnerInput[2];

			final String[] orderedGIDs = rowClusterData.getReorderedIDs();
			final String[] orderedAIDs = colClusterData.getReorderedIDs();
			
			if (orderedGIDs != null && orderedGIDs.length > 0) {
				rowC = KMEANS_ROW_SUFFIX + row_clusterN;
			}

			if (orderedAIDs != null && orderedAIDs.length > 0) {
				colC = KMEANS_COL_SUFFIX + col_clusterN;
			}

			fileEnd = KMEANS_DESIGNATOR + rowC + colC + FILE_EXT;
		}
		
		return fileEnd;
   }
   
  /**
   * Finds the last position of a File.separatorChar in a filePath String.  
   * @param filePath - The filePath String.
   * @return The index of the last position of a File.separatorChar.
   */
  private static int getLastFileSeparatorPos(final String filePath) {
  	
		char[] nameArray = filePath.toCharArray();
		int startIndex = 0;
		for (int i = 0; i < nameArray.length; i++) {
			if (nameArray[i] == File.separatorChar) {
				startIndex = i;
			}
		}
		
		return startIndex;
  }
}
