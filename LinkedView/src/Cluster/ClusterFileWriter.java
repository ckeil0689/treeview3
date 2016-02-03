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
	
	private BufferedWriter bw;
	private final File file;
	
	public ClusterFileWriter(final File file) {

		this.file = file;
		setupWriter();
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
			LogBuffer.logException(e);
		}
	}

	/**
	 * Closes this BufferedWriter and prints a notification to console.
	 */
	public void closeWriter() {

		try {
			bw.close();

		} catch (final IOException e) {
			LogBuffer.logException(e);
		}

		LogBuffer.println("Done." + file.getAbsolutePath());
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

	public String getFilePath() {

		return file.getAbsolutePath();
	}

	public File getFile() {

		return file;
	}
}
