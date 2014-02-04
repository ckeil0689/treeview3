package Cluster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * This class is used to save data from clustering to the local storage by
 * converting it to a tab-delimited string and using a BufferedWriter.
 * 
 * @author CKeil
 * 
 */
public class ClusterFileWriter2 extends BufferedWriter {

	private File file;
	private final String SEPARATOR = "\t";
	private final String END_OF_ROW = "\n";

	public ClusterFileWriter2(final File file) 
			throws IOException, FileNotFoundException {
		
		super(new OutputStreamWriter(
				new FileOutputStream(file.getAbsoluteFile()), "UTF-8"));
		this.file = file;
	}

	/**
	 * This methods writes the string from the doParse() method to local storage
	 * using the original name of the file and the specified file extension.
	 * 
	 * @param input
	 * @param fileEnd
	 */
	public void writeContent(final List<String> input) {

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
		
		System.out.println("Done." + file.getAbsolutePath());
	}

	/**
	 * A method to parse the String matrix into a tab-delimited string.
	 * 
	 * @param input
	 * @param lastLine
	 * @return
	 */
	public String doParse(final List<String> input) {

		final StringBuilder sb = new StringBuilder();

		int lastIndex = input.size() - 1;
		for (final String element : input) {
	
			sb.append(element);
			
			if(input.indexOf(element) != lastIndex) {
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
