package Cluster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * This class is used to save data from clustering to the local storage by
 * converting it to a tab-delimited string and using a BufferedWriter.
 *
 * @author CKeil
 *
 */
public class ClusterFileWriter {

	private final String SEPARATOR = "\t";
	private final String END_OF_ROW = "\n";

	private File file;
	private BufferedWriter bw;

	public ClusterFileWriter(final String fileDirectory, final String fileEnd,
			final int linkMethod) {

		String linkName = getLinkName(linkMethod);
		String fileName = getName(fileDirectory);
		String rootDir = getRootDir(fileDirectory, fileName);
		String subDir = setLinkDir(rootDir, fileName, linkName);
		setFile(subDir, fileName, linkName, fileEnd);
		setupWriter();
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

	private void setFile(String dir, String fileName, String linkName,
			String fileEnd) {

		fileName += "_" + linkName;
		String fullFileID = dir + fileName + fileEnd;
		
		File file = new File(fullFileID);
		
		try {
			/* Do not overwrite at the moment */
			file = getNewFile(dir, fileName, fileEnd);
			file.createNewFile();
			
		} catch (IOException e) {
			LogBuffer.logException(e);
		}

		this.file = file;
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

	private void setupWriter() {

		try {
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file.getAbsoluteFile()), "UTF-8"));
			
		} catch (UnsupportedEncodingException e) {
			LogBuffer.logException(e);
			
		} catch (FileNotFoundException e) {
			LogBuffer.logException(e);
		}
	}

	/**
	 * This methods writes the string from the doParse() method to local storage
	 * using the original name of the file and the specified file extension.
	 *
	 * @param input
	 * @param fileEnd
	 */
	public void writeContent(final String[] input) {

		final String content = doParse(input);

		try {
			bw.write(content);

		} catch (final IOException e) {

		}
	}

	/**
	 * Closes this BufferedWriter and prints a notification to console.
	 */
	public void closeWriter() {

		try {
			bw.close();

		} catch (final IOException e) {

		}

		LogBuffer.println("Done." + file.getAbsolutePath());
	}

	/**
	 * A method to parse the String matrix into a tab-delimited string.
	 *
	 * @param input
	 * @param lastLine
	 * @return
	 */
	public String doParse(final String[] input) {

		final StringBuilder sb = new StringBuilder();

		final int lastIndex = input.length - 1;
		for (int i = 0; i < input.length; i++) {

			sb.append(input[i]);

			if (i != lastIndex) {
				sb.append(SEPARATOR);
			}
		}

		sb.append(END_OF_ROW);

		return sb.toString();
	}

	public String getFilePath() {

		return file.getAbsolutePath();
	}

	public File getFile() {

		return file;
	}

}
