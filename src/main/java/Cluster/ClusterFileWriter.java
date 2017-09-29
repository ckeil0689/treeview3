package Cluster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import edu.stanford.genetics.treeview.LogBuffer;

public class ClusterFileWriter {
	
	private final String SEPARATOR = "\t";
	private final String END_OF_ROW = "\n";
	
	private File file;
	protected BufferedWriter bw;
	
	public ClusterFileWriter(final File file) {

		this.file = file;
		setupWriter();
	}
	
	/**
	 * A method to parse a String array into one tab-delimited string.
	 *
	 * @param input
	 * @param lastLine
	 * @return One tab-delimited String.
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
	
	/**
	 * Creates a single string by calling doParse() and then writes the 
	 * supplied data using the BufferedWriter object.
	 * @param data - A String array of data to be written to a file.
	 * @return boolean indicating finishing of data writing without any Exception.
	 */
	public boolean writeData(final String[] data) {
		
		final String content = doParse(data);

		try {
			bw.write(content);
		} 
		catch (final IOException e) {
			LogBuffer.logException(e);
			return false;
		}
		return true;
	}
	
	/**
	 * Set up the BufferedWriter.
	 */
	public void setupWriter() {
		
		try {
			this.bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file.getAbsoluteFile()), "UTF-8"));
			
		} catch (UnsupportedEncodingException e) {
			LogBuffer.logException(e);
			
		} catch (FileNotFoundException e) {
			LogBuffer.logException(e);
		}
	}
	
	/**
	 * Closes this BufferedWriter and prints a notification to console.
	 */
	public void closeWriter() {

		try {
			bw.close();
			LogBuffer.println("Done." + file.getAbsolutePath());
			
		} catch (final IOException e) {
			LogBuffer.logException(e);
      bw = null;
		}
	}
	
	public String getFilePath() {
		return file.getAbsolutePath();
	}

	public File getFile() {
		return file;
	}
	
	public BufferedWriter getBufferedWriter() {
		return bw;
	}
	
}
