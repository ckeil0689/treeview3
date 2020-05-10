package model.save;

import model.fileImport.AllowedFilesFilter;
import org.apache.commons.io.FilenameUtils;
import util.LogBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/** This class determines the file name structure (full path) for a file
 * that will be written during clustering. The name will be dependent on
 * the linkage type (avg, single, complete) and what kind of file will
 * be created. */
public class ModelFileCreator {

	private final static String DEFAULT_EXT = ".txt";
	private final static String CDT_EXT = ".cdt";
	private final static String GTR_EXT = ".gtr";
	private final static String ATR_EXT = ".atr";
	
	private static final String KMEANS_DESIGNATOR = "_K";
	private static final String KMEANS_ROW_SUFFIX = "_G";
	private static final String KMEANS_COL_SUFFIX = "_A";

	public static File retrieveDefaultFile(final Path clusterPath) {
		
		return retrieveFile(clusterPath, ModelFileCreator.DEFAULT_EXT);
	}
	
	public static File retrieveMainFile(final Path clusterPath, 
	                                    final boolean isRowClustered, 
	                                    final boolean isColClustered, 
	                                    final boolean isHier, 
	                                    final int[] spinnerInput) {
		
		String fileExt = ModelFileCreator.determineClusterFileExt(isRowClustered, 
		                                                          isColClustered, 
		                                                          isHier,
		                                                          spinnerInput);
		return retrieveFile(clusterPath, fileExt);
	}
	
  public static File retrieveATRFile(final Path clusterPath) {
		
		return retrieveFile(clusterPath, ModelFileCreator.ATR_EXT);
	}
  
  public static File retrieveGTRFile(final Path clusterPath) {
		
		return retrieveFile(clusterPath, ModelFileCreator.GTR_EXT);
	}

	/** Creates a new file from based on the passed directory, file name, link
	 * name, and file end.
	 * 
	 * @param clusterPath - The Path object which represents the directory in
	 *          which the new file will reside.
	 * @param fileEnd - The file ending
	 * @return A newly created File object. */
	private static File retrieveFile(final Path clusterPath,
																	final String fileEnd) {

		final String fileBasename = FilenameUtils.removeExtension(clusterPath.toString());
		final String filename = fileBasename + fileEnd;
		File tempFile = Paths.get(filename).toFile();
		// Do not overwrite at the moment
		//tempFile = ModelFileCreator.getNewFile(clusterPath, fileEnd);
		return tempFile;
	}
	
	/** 
	 * Fix extension if user did not add one or added an invalid extension. 
	 * This is here and not in FileSet because knowledge about whether 
	 * the model is clustered is required.*/
	public static Path fixFileExtension(Path filePath, 
	                                    final boolean isClustered) {
		
		String sFilePath = filePath.toString();
		AllowedFilesFilter ff = new AllowedFilesFilter();
		String ext = FilenameUtils.getExtension(sFilePath);
		
		if(ext.equals("") || !ff.accept(null, sFilePath)) {
			sFilePath = FilenameUtils.removeExtension(sFilePath);
			ext = ModelFileCreator.DEFAULT_EXT;
			if(isClustered) {
				ext = ModelFileCreator.CDT_EXT;
			}
			sFilePath += ext;
		}
		
		return Paths.get(sFilePath);
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
	private static File getNewFile(	final Path clusterPath,
																	final String fileEnd) {

		String fileDescr = clusterPath.toString() + fileEnd;
		File file = Paths.get(fileDescr).toFile();

		/*
		 * Even for gtr and atr files, the cdt files are the ones that should
		 * be exclusively counted. While single axes might be clustered,
		 * if the user hits cancel during clustering, no .cdt file will exist.
		 */
		String cdtFileDescr = clusterPath.toString() + ModelFileCreator.CDT_EXT;
		File cdtFile = Paths.get(cdtFileDescr).toFile();

		int fileCount = 0;
		String cdtSuffix;
		String suffix;
		while(cdtFile.exists()) {
			fileCount++;

			cdtSuffix = "_" + fileCount + ModelFileCreator.CDT_EXT;
			cdtFileDescr = clusterPath.toString() + cdtSuffix;
			cdtFile = Paths.get(cdtFileDescr).toFile();

			suffix = "_" + fileCount + fileEnd;
			fileDescr = clusterPath.toString() + suffix;
			file = Paths.get(fileDescr).toFile();
		}

		try {
			file.createNewFile();
		}
		catch(IOException e) {
			LogBuffer.logException(e);
			// TODO add warning Dialog before proceeding
		}
		
		cdtFile = null;
		return file;
	}

	/** Figures out which file extension to use for the passed arguments. This
	 * is especially necessary to create k-means files using the Cluster 3.0
	 * naming convention where file names depended on chosen number of clusters
	 * and chosen number of iterations.
	 * 
	 * @param isClustered - whether the file to be written is clustered or not
	 * @param isHier - Whether clustering was hierarchical or not.
	 * @param spinnerInput - Input for K-Means clustering, such as number of
	 *          clusters.
	 * @param rowClusterData - The specific components.cluster data for the row axis.
	 * @param colClusterData - The specific components.cluster data for the column axis.
	 * @return A String representing a file extension. */
	public static String determineClusterFileExt(	final boolean isRowClustered,
	                                             	final boolean isColClustered,
	                                             	final boolean isHier,
																								final int[] spinnerInput) {
		String fileEnd = ".txt";
		
		if(!(isRowClustered || isColClustered)) {
			return fileEnd; 
		}

		if(isHier) {
			fileEnd = ModelFileCreator.CDT_EXT;

			// k-means file names have a few more details
		}
		else {
			String rowC = "";
			String colC = "";

			final int row_clusterN = spinnerInput[0];
			final int col_clusterN = spinnerInput[2];

			if(isRowClustered) {
				rowC = ModelFileCreator.KMEANS_ROW_SUFFIX + row_clusterN;
			}

			if(isColClustered) {
				colC = ModelFileCreator.KMEANS_COL_SUFFIX + col_clusterN;
			}

			fileEnd = ModelFileCreator.KMEANS_DESIGNATOR +	rowC + colC +
								ModelFileCreator.CDT_EXT;
		}

		return fileEnd;
	}
}
