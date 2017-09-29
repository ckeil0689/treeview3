package Cluster;

import java.io.File;
import java.util.List;

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * This class is specifically concerned with writing data to tree files.
 */
public class TreeFileWriter extends ClusterFileWriter {

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
	public TreeFileWriter(final File file) {
		
		super(file);
	}

	/**
	 * Writes information about the newly clustered elements to a buffer.
	 * 
	 * @param treeNodeData - list of all node pairs which define the Dendrogram.
	 * @return boolean indicating success of writing the node data to file.
	 */
	public boolean writeData(final List<String[]> treeNodeData) {

		if (bw == null) {
			LogBuffer.println("Cannot write cluster data because "
					+ "BufferedWriter object is null.");
			return false;
		}

		/*
		 * List to store the Strings which represent calculated data (such as
		 * row pairs) to be added to dataTable.
		 */
		final int nodeInfoSize = 4;
		String[] nodeInfo = new String[nodeInfoSize];
    boolean wasWriteSuccessful = false;
    
		for(int i = 0; i < treeNodeData.size(); i++) {
			nodeInfo = treeNodeData.get(i);
			wasWriteSuccessful = writeData(nodeInfo);
			
			if(!wasWriteSuccessful) {
				LogBuffer.println("Could not write node info " +
					"to file (" + nodeInfo + ")");
				return false;
			}
		}

		return true;
	}
}
