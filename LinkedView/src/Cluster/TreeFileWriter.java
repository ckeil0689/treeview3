package Cluster;

import Controllers.ClusterDialogController;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * This class is specifically concerned with writing data to tree files.
 * 
 * @author chris0689
 *
 */
public class TreeFileWriter {

	/* Writer that generates the ATR/ GTR files for trees */
	private ClusterFileWriter bufferedWriter;

	/**
	 * Sets up a writer for tree files which keep track of the clustered matrix
	 * elements.
	 * 
	 * @param axis
	 *            The original matrix' axis which is clustered.
	 * @param fileName
	 *            The name of the file to be clustered.
	 * @param linkMethod
	 *            Identifier for link method used for clustering.
	 */
	public TreeFileWriter(final int axis, String fileName, int linkMethod) {

		String fileSuffix = (axis == ClusterDialogController.ROW) ? ".gtr" : ".atr";

		this.bufferedWriter = new ClusterFileWriter(fileName, fileSuffix,
				linkMethod);

	}

	/**
	 * Writes information about the newly clustered elements to a buffer.
	 * 
	 * @param link
	 *            The pair of newly linked elements.
	 * @param loopNum
	 *            The current iteration step of clustering.
	 * @param min
	 *            The minimum value from the distance matrix associated with the
	 *            current cluster pair.
	 * @return The ID of the newly formed tree node.
	 */
	public String writeData(final String[] link, int loopNum, double min) {

		if (bufferedWriter == null) {
			LogBuffer.println("Cannot write cluster data because "
					+ "BufferedWriter object is null.");
			return "NA";
		}

		/*
		 * List to store the Strings which represent calculated data (such as
		 * row pairs) to be added to dataTable.
		 */
		final int nodeInfoSize = 4;
		final String[] nodeInfo = new String[nodeInfoSize];

		/*
		 * Create a list that stores the info of the current new node to be
		 * formed. This will be written down in the corresponding tree file.
		 */
		nodeInfo[0] = "NODE" + (loopNum + 1) + "X";
		nodeInfo[1] = link[0];
		nodeInfo[2] = link[1];
		nodeInfo[3] = String.valueOf(1 - min);

		/* Write the node info to the tree output file */
		bufferedWriter.writeContent(nodeInfo);

		return nodeInfo[0];
	}

	/**
	 * Closes the file buffer and writes the tree file to disk.
	 */
	public void close() {

		bufferedWriter.closeWriter();
	}
}
