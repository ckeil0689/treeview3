package edu.stanford.genetics.treeview.model;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.LabelInfo;
import edu.stanford.genetics.treeview.LogBuffer;

public class CustomLabelLoader {

	private final LabelInfo labelInfo;
	private String[][] labels;
	private String[] newLabelTypes;
	private boolean[] labelMatches;
	private final int[] selectedIndeces;
	private int misses;
	private int lineNum;

	boolean labelTypesFound = false;

	public CustomLabelLoader(final LabelInfo labelInfo,
			final int[] selectedIndeces) {

		this.labelInfo = labelInfo;
		this.selectedIndeces = selectedIndeces;
	}

	/**
	 * Loads the file with an InputStream and a BufferedReader and then assigns
	 * the file content to a String[][] which contains the new labels. The
	 * method then calls addNewLabels() to add the newly loaded labels to the
	 * currently loaded TVModel object.
	 *
	 * @param customFile
	 * @param int geneNum
	 */
	public void load(final File customFile, final int geneNum) {

		try {
			final String fileName = customFile.getAbsolutePath();

			lineNum = count(fileName);

			// Next: read file, return string arrays with new names
			// Then: update currently loaded model.
			final FileInputStream fis = new FileInputStream(fileName);
			final DataInputStream in = new DataInputStream(fis);
			final BufferedReader br = new BufferedReader(new InputStreamReader(
					in));

			// int to count the current row in while loop
			int rowN = 0;

			// Number of row labels without GID
			// int geneLabelNum = tvFrame.getDataModel().getGeneHeaderInfo()
			// .getNumNames();
			//
			// if (tvFrame.getDataModel().gidFound()) {
			// geneLabelNum--;
			// }

			labels = new String[lineNum][geneNum];

			String line;
			// iterate reader through each line
			while ((line = br.readLine()) != null) {

				final String[] lineAsStrings = line.split("\t");

				labels[rowN] = lineAsStrings;

				rowN++;
			}

			br.close();

		} catch (final FileNotFoundException e1) {
			e1.printStackTrace();

		} catch (final IOException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Searches the loaded labels for label types such as ORF. If they are
	 * present, the newly loaded labels will later replace the old labels.
	 */
	public int checkForLabelTypes(final DataModel model) {

		int checkLimit = labelInfo.getNumLabels() / 100;

		newLabelTypes = new String[labels[0].length];

		if (checkLimit > 5) {
			checkLimit = 5;
		}

		// YORF regex pattern
		// Pattern pattern = Pattern.compile("(\\D{3}\\d{3}\\D{1})");
		//
		// for(int i = 0; i < checkRowLimit; i++) {
		//
		// for(int j = 0; j < labels[i].length; j++) {
		//
		// String yorf = labels[i][j];
		// Matcher matcher = pattern.matcher(yorf);
		//
		// if (!(matcher.find() || yorf.equalsIgnoreCase(""))) {
		// // LogBuffer.println("Label: " + yorf);
		// newNames[j] = yorf;
		// namesFound = true;
		//
		// } else {
		// // LogBuffer.println(matcher.group(0));
		// }
		// }
		// }

		for (int i = 0; i < checkLimit; i++) {

			for (int j = 0; j < labels[i].length; j++) {

				final String yorf = labels[i][j];

				if (!yorf.equalsIgnoreCase("")) {
					newLabelTypes[j] = yorf;
					labelTypesFound = true;
				}
			}
		}

		// Check if old model already contains labels from the new list.
		final List<String> existingLabels = Arrays
				.asList(labelInfo.getLabelTypes());

		labelMatches = new boolean[newLabelTypes.length];

		misses = 0;
		if (existingLabels != null) {
			for (int i = 0; i < newLabelTypes.length; i++) {

				if (existingLabels.contains(newLabelTypes[i])) {
					labelMatches[i] = true;

				} else {
					labelMatches[i] = false;
					misses++;
				}
			}
		} else {
			LogBuffer.println("No Label names could be loaded.");
			return 0;
		}

		int addIndex = 0;
		final String[] finalNames = new String[misses];
		for (int i = 0; i < labelMatches.length; i++) {

			if (!labelMatches[i]) {
				finalNames[addIndex] = newLabelTypes[i];
				addIndex++;
			}
		}

		newLabelTypes = finalNames;

		LogBuffer.println("Old Labels: " + existingLabels.toString());
		LogBuffer.println("New Labels: " + Arrays.toString(newLabelTypes));
		LogBuffer.println("Selected Indeces: "
				+ Arrays.toString(selectedIndeces));
		LogBuffer.println("Match List: " + Arrays.toString(labelMatches));

		return labels[0].length;
	}

	/**
	 * Fuses two arrays together.
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	public String[] concatArrays(final String[] a, final String[] b) {

		final String[] c = new String[a.length + b.length];

		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);

		return c;
	}

	/**
	 * Replacing matching values in the old labelArray with loaded labels.
	 *
	 * @param model
	 * @param loadedLabels
	 */
	public String[] replaceLabel(final String[] oldLabels,
			final String oldLabelTypes[]) {

		String[] newLabels = null;
		String[] labelsToAdd;

		newLabels = findNewLabel(oldLabels, oldLabelTypes, labels);
		labelsToAdd = concatArrays(oldLabels, newLabels);

		return labelsToAdd;
	}

	/**
	 * Checks the loadedLabels array whether it contains a label from the old
	 * labelArray and then replaces it accordingly with the newly loaded
	 * version. Returns the new
	 *
	 * @param oldLabels
	 * @param loadedLabels
	 * @return
	 */
	public String[] findNewLabel(final String[] oldGene,
			final String[] oldNames, final String[][] loadedLabels) {

		final List<String> newLabels = new ArrayList<String>();
		boolean match = false;

		// Find a match
		int matchIndex = -1;
		final String oldLabel = oldGene[selectedIndeces[0]];
		for (int i = 0; i < loadedLabels.length; i++) {

			final String[] loadedLabelElement = loadedLabels[i];

			for (final String newLabel : loadedLabelElement) {

				if (newLabel.toLowerCase().contains(oldLabel.toLowerCase())) {
					match = true;
					matchIndex = i;
					break;
				}
			}

			if (match) {
				break;
			}
		}

		// Replace matched element.
		if (match) {
			final String[] matchedGene = loadedLabels[matchIndex];

			for (int i = 0; i < matchedGene.length; i++) {

				if (!labelMatches[i]) {
					newLabels.add(matchedGene[i]);
				}
			}

		} else {
			for (int i = 0; i < misses; i++) {

				newLabels.add("No match");
			}
		}

		// Change list to array for return
		return newLabels.toArray(new String[newLabels.size()]);
	}

	/**
	 * Sets the new names for the labels in the TVModel object.
	 *
	 * @param model
	 */
	public void setLabels(final DataModel model, final String type,
			final String[][] labelsToAdd) {

		final TVModel tvModel = (TVModel) model;
		if (type.equalsIgnoreCase("Row")) {
			tvModel.setRowLabels(labelsToAdd);

		} else if (type.equalsIgnoreCase("Column")) {
			tvModel.setColumnLabels(labelsToAdd);
		}

		tvModel.notifyObservers();

		// Set the new labels for the headers
		final String[] oldLabelTypes = labelInfo.getLabelTypes();
		String[] labelTypesToAdd = null;
		// Change model label type array
		if (labelTypesFound) {
			labelTypesToAdd = concatArrays(oldLabelTypes, newLabelTypes);

			// Check for empty or null value
			for (int i = 0; i < labelTypesToAdd.length; i++) {
				if (labelTypesToAdd[i] == null || "".equalsIgnoreCase(labelTypesToAdd[i])) {
					labelTypesToAdd[i] = "CUSTOM " + (i + 1);
				}
			}

		} else {
			// Make headers for custom labels
			for (int i = 0; i < newLabelTypes.length; i++) {

				newLabelTypes[i] = "CUSTOM " + (i + 1);
			}

			labelTypesToAdd = concatArrays(oldLabelTypes, newLabelTypes);
		}

		labelInfo.setLabelTypeArray(labelTypesToAdd);
	}

	/**
	 * Count amount of lines in the file to be loaded so that the progressBar
	 * can get correct values for extractData(). Code from StackOverflow
	 * (https://stackoverflow.com/questions/453018).
	 *
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public int count(final String filename) throws IOException {

		final InputStream is = new BufferedInputStream(new FileInputStream(
				filename));

		try {
			final byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			boolean empty = true;

			while ((readChars = is.read(c)) != -1) {
				empty = false;

				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
			}
			return (count == 0 && !empty) ? 1 : count;

		} finally {
			is.close();
		}
	}
}
