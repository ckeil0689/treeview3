package edu.stanford.genetics.treeview.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import Cluster.ClusterFileGenerator;
import Cluster.ClusteredAxisData;
import Views.ClusterView;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;

public class ModelSaver {

	private DataModel model;
	
	public ModelSaver() {
		
	}
	
	public void save(final DataModel model) {
		
		// get file name
		JFileChooser chooser=new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.showSaveDialog(null);

		String path=chooser.getSelectedFile().getAbsolutePath();
		String filename=chooser.getSelectedFile().getName();
		
		this.model = model;
		
		new SaveTask(filename).execute();
	}
	
	/**
	 * Worker with the task to generate and write a new CDT file which contains
	 * the newly clustered matrix. Makes sure that the newly created file is
	 * visualized right after clustering, given that the process was not
	 * cancelled.
	 *
	 * @param rowCAD - row clustering data relevant for CDT save file
	 * @param colCAD - column clustering data relevant for CDT save file
	 * @param fileName - name of the CDT file to be written
	 */
	private class SaveTask extends SwingWorker<Boolean, Void> {

		private final String fileName;
		private String filePath;

		public SaveTask(final String fileName) {

			this.fileName = fileName;
		}

		@Override
		protected Boolean doInBackground() throws Exception {

			final TVDataMatrix originalMatrix = (TVDataMatrix) model.getDataMatrix();
			final double[][] data = originalMatrix.getExprData();

			final ClusterFileGenerator cdtGen = new ClusterFileGenerator(data, 
					rowClusterData, colClusterData, isHierarchical());

			cdtGen.setupWriter(cdtFile);
			cdtGen.generateCDT();

			filePath = cdtGen.finish();
			
			if(isCancelled()) {
				return Boolean.FALSE;
			}
			
			if(filePath == null) {
				LogBuffer.println("Generating a CDT failed. Cancelling...");
				this.cancel(true);
				return Boolean.FALSE;
			}

			return Boolean.TRUE;
		}

		@Override
		protected void done() {

			if (!isCancelled() && hasEnsuredTreeFilePresence()) {
				ClusterView.setStatusText("Saving done!");
				loadClusteredData(filePath);
				LogBuffer.println("SaveTask is done: success.");
				
			} else {
				clusterView.setClustering(false);
				LogBuffer.println("Saving did not finish successfully.");
				deleteAllFiles();
			}
		}
		
		/**
		 * Makes sure that a tree file exists for an axis that is supposed to
		 * be clustered. If not, it attempts to take one from a previous 
		 * cluster and if that does not exist either it will consider an axis
		 * as not clustered. In that case, a tree file will not be present. Returns
		 * true upon successful completion.
		 */
		private boolean hasEnsuredTreeFilePresence() {
	
			if(filePath == null || fileName == null) {
				return false;
			}
			
			final int fileRootNameSize = filePath.length() - CDT_END.length();
			final String newFileRoot = filePath.substring(0, fileRootNameSize);
			
			ensureTreeFilePresence(fileName, newFileRoot, GTR_END, ROW_IDX);
			ensureTreeFilePresence(fileName, newFileRoot, ATR_END, COL_IDX);
			
			return true;
		}
		
		/**
		 * For a given axis, this ensures that its tree file exists in case
		 * it is considered to be clustered. This is useful especially if
		 * an axis was already clustered before so that the old tree file can
		 * be carried over to the new FileSet.
		 * @param oldFileRoot The root name of the older cdt file.
		 * @param newFileRoot The root name of the new cdt file.
		 * @param treeFileSuffix The tree file suffix to be used if a new file
		 * is created.
		 * @param axisIdx An integer identifying the axis.
		 */
		private void ensureTreeFilePresence(final String oldFileRoot, 
				final String newFileRoot, final String treeFileSuffix, 
				final int axisIdx) { 
			
			String axis_id;
			boolean axisNeedsTreeFileCheck;
			
			if(axisIdx == ROW_IDX) {
				axis_id = "row";
				axisNeedsTreeFileCheck = rowClusterData.isAxisClustered();
			} else {
				axis_id = "column";
				axisNeedsTreeFileCheck = colClusterData.isAxisClustered();
			}
			
			if(axisNeedsTreeFileCheck) {
				String newTreeFilePath = newFileRoot + treeFileSuffix;
				String oldTreeFilePath = oldFileRoot + treeFileSuffix;
				
				if(!doesFileExist(newTreeFilePath)) {
					LogBuffer.println("No file found for " + axis_id 
							+ " trees.");
					if(doesFileExist(oldTreeFilePath)) {
						LogBuffer.println("But old " + axis_id 
								+ " tree file was found!");
						copyFile(oldTreeFilePath, newTreeFilePath);
					} else {
						String message = "The tree file for the " + axis_id 
								+ " axis could not be recovered. No trees "
								+ "can be shown.";
						JOptionPane.showMessageDialog(clusterDialog, message);
					}
				} else {
					LogBuffer.println("Success! The " + axis_id 
							+ " tree file was found.");
				}
			} else {
				LogBuffer.println("The " + axis_id 
						+ "s have not been clustered.");
			}
		}
		
		/**
		 * Copies an old file to a new one with the correct file name. This 
		 * is used for transferring old tree files to new clustered matrices.
		 * @param oldTreeFilePath The path of the file to be copied.
		 * @param newTreeFilePath The path to which the old file will be copied.
		 */
		private boolean copyFile(final String oldTreeFilePath, 
				final String newTreeFilePath) {
			
			try(FileInputStream srcStream = 
					new FileInputStream(oldTreeFilePath); 
				FileOutputStream dstStream = 
						new FileOutputStream(newTreeFilePath)) {
				
				dstStream.getChannel().transferFrom(srcStream.getChannel(), 
						0, srcStream.getChannel().size());
				return true;
				
			} catch (IOException e) {
				LogBuffer.logException(e);
				return false;
			} 
		}
		
		/**
		 * Checks if a file at a given path exists or not.
		 * @param path - The complete file path which to check.
		 * @return Whether the checked file exists or not.
		 */
		private boolean doesFileExist(final String path) {
			
			File f = new File(path);
			return (f.exists() && !f.isDirectory());
		}
	}
	
}
