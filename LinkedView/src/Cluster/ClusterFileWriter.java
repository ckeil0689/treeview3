package Cluster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * This class is used to save data from clustering to the local storage by
 * converting it to a tab-delimited string and using a BufferedWriter.
 *
 * @author CKeil
 *
 */
public class ClusterFileWriter extends BufferedWriter {

	private final File file;
	private final String SEPARATOR = "\t";
	private final String END_OF_ROW = "\n";

	public ClusterFileWriter(final File file) throws IOException,
			FileNotFoundException {

		super(new OutputStreamWriter(new FileOutputStream(
				file.getAbsoluteFile()), "UTF-8"));
		this.file = file;
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
			write(content);

		} catch (final IOException e) {

		}
	}

	/**
	 * Closes this BufferedWriter and prints a notification to console.
	 */
	public void closeWriter() {

		try {
			close();

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
