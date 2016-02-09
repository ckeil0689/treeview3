package Cluster;

import java.io.File;
import java.io.IOException;

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * This class determines the file name structure (full path) for a file 
 * that will be written during clustering. The name will be dependent on 
 * the linkage type (avg, single, complete) and what kind of file will 
 * be created.
 * @author CKeil
 *
 */
public class ClusterFileStorage {

	private final String FILE_EXT = ".cdt";
	
	private final String KMEANS_DESIGNATOR = "_K";
	private final String KMEANS_ROW_SUFFIX = "_G";
	private final String KMEANS_COL_SUFFIX = "_A";
	
	public File createFile(final String fileDirectory, final String fileEnd,
			final int linkMethod) {
		
		String linkName = getLinkName(linkMethod);
		String fileName = getName(fileDirectory);
		String rootDir = getRootDir(fileDirectory, fileName);
		String subDir = setLinkDir(rootDir, fileName, linkName);
		
		return retrieveFile(subDir, fileName, linkName, fileEnd);
	}

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

	private static String getName(String fileDirectory) {

		char[] nameArray = fileDirectory.toCharArray();
		int startIndex = 0;
		for (int i = 0; i < nameArray.length; i++) {

			if (nameArray[i] == File.separatorChar) {
				startIndex = i;
			}
		}

		return getRootFileName(fileDirectory.substring(startIndex + 1,
				fileDirectory.length()));
	}

	private static String getRootDir(String fileDirectory, String fileName) {

		int first = fileDirectory.indexOf(fileName, 0);

		return fileDirectory.substring(0, first);
	}

	private static String getRootFileName(String name) {

		String single_suff = "_single";
		String complete_suff = "_complete";
		String average_suff = "_average";
		String kmeans_suff = "_kmeans";

		int start_s = name.indexOf(single_suff, 0);
		int end_s = start_s + single_suff.length();

		int start_c = name.indexOf(complete_suff, 0);
		int end_c = start_c + complete_suff.length();

		int start_a = name.indexOf(average_suff, 0);
		int end_a = start_a + average_suff.length();

		int start_k = name.indexOf(kmeans_suff, 0);
		;
		int end_k = start_k + kmeans_suff.length();

		int end;
		if (start_s > 0
				&& name.substring(start_s, end_s).equalsIgnoreCase(single_suff)) {
			end = start_s;
			
		} else if (start_c > 0
				&& name.substring(start_c, end_c).equalsIgnoreCase(
						complete_suff)) {
			end = start_c;
			
		} else if (start_a > 0
				&& name.substring(start_a, end_a)
						.equalsIgnoreCase(average_suff)) {
			end = start_a;
			
		} else if (start_k > 0
				&& name.substring(start_k, end_k).equalsIgnoreCase(kmeans_suff)) {
			end = start_k;
			
		} else {
			end = name.length();
		}

		return name.substring(0, end);
	}

	/**
	 * Creates a folder with the general file name to store all variations and
	 * subfiles of clustering in one folder.
	 * 
	 * @param fileDir
	 *            The directory plus file name without file type.
	 * @return The main file's directory.
	 */
	private static String setLinkDir(String rootDir, String fileName, 
			String linkName) {

		String linkSubDir = rootDir + fileName + File.separator + linkName;
		File file = new File(linkSubDir);

		/* Create folder if it does not exist */
		if (!(file.exists() && file.isDirectory())) {
			file.mkdirs();
		}

		return linkSubDir += File.separator;
	}

	private File retrieveFile(String dir, String fileName, String linkName,
			String fileEnd) {

		fileName += "_" + linkName;
		String fullFileID = dir + fileName + fileEnd;
		
		File tempFile = new File(fullFileID);
		
		try {
			/* Do not overwrite at the moment */
			tempFile = getNewFile(dir, fileName, fileEnd);
			tempFile.createNewFile();
			
		} catch (IOException e) {
			LogBuffer.logException(e);
		}

		return tempFile;
	}

	private static File getNewFile(String dir, String oldName, String fileEnd) {

		File file = new File(dir + oldName + fileEnd);

		/*
		 * Even for gtr and atr files, the cdt files are what should be counted
		 * ONLY. While single axes might be clustered, if the user hits cancel
		 * during clustering, no .cdt file will exist. That makes the rest
		 * useless so it can be overwritten and avoids number errors with tree
		 * files.
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
	
    public String getClusterFileExtension(final boolean isHier, 
    		Integer[] spinnerInput, ClusteredAxisData rowClusterData, 
    		ClusteredAxisData colClusterData) {
    	
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
}
