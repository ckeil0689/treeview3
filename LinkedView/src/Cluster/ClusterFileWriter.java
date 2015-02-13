package Cluster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

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
		String dir = setFolder(fileDirectory, linkName);
		setFile(dir, fileName, linkName, fileEnd);
		setupWriter();
	}
	
	private String getLinkName(int link) {
		
		String linkName;
		switch(link) {
		
		case HierCluster.SINGLE:
			linkName = "single";
			break;
		case HierCluster.AVG: 
			linkName = "average";
			break;
		case HierCluster.COMPLETE:
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
	
	private String getName(String fileDirectory) {
		
		char[] nameArray = fileDirectory.toCharArray();
		int startIndex = 0;
		for(int i = 0; i < nameArray.length; i++) {
			
			if(nameArray[i] == File.separatorChar) {
				startIndex = i;
			}				
		}
		
		return fileDirectory.substring(startIndex + 1, fileDirectory.length());
	}
	
	/**
	 * Creates a folder with the general file name to store all
	 * variations and subfiles of clustering in one folder.
	 * @param fileDir The directory plus file name without file type.
	 * @return The main file's directory.
	 */
	private String setFolder(String fileDir, String linkName) {
		
		String newDir = fileDir + File.separator + linkName;
		File file = new File(newDir);
		
		/* Create folder if it does not exist */
		if(!(file.exists() && file.isDirectory())) {
			file.mkdirs();
		}
		
		return newDir += File.separator;
	}
	
	private void setFile(String dir, String fileName, String linkName, 
			String fileEnd) {
		
		String fullFileID = dir + fileName + linkName + fileEnd;
		File file = new File(fullFileID);
		
		try {
			if(file.exists()) {
				int n = JOptionPane.showConfirmDialog(
					    JFrame.getFrames()[0],
					    "File already exists (" + fullFileID + "). Overwrite?",
					    "Confirm File Storage",
					    JOptionPane.YES_NO_OPTION);
				
				switch(n) {
				
				case JOptionPane.YES_OPTION:
					file.createNewFile();
					break;
				case JOptionPane.NO_OPTION:
				default: 
					file = getNewFile(dir, fileName, fileEnd);
					file.createNewFile();
					break;
				}
			}
		} catch (IOException e) {
			LogBuffer.logException(e);
		}
		
		this.file = file;
	}
	
	private File getNewFile(String dir, String oldName, String fileEnd) {
		
		File file = new File(oldName + fileEnd);
		int fileCount = 0;
		
		while(file.exists()) {
			fileCount++;
			file = new File(dir + oldName + "_" + fileCount + fileEnd);
		}
		
		return file;
	}
	
	private void setupWriter() {
		
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
					file.getAbsoluteFile()), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
