package edu.stanford.genetics.treeview.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import Views.ClusterDialog;
import Views.ClusterView;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.LogBuffer;

public class ModelSaver {

	private final int ROW_IDX = 0;
	private final int COL_IDX = 1;
	
	public final static String CDT_EXT = ".cdt";
	public final static String GTR_EXT = ".gtr";
	public final static String ATR_EXT = ".atr";
	
	private final JDialog clusterDialog;
	private DataModel model;
	
	public ModelSaver(final ClusterDialog cd) {
		
		this.clusterDialog = cd;
	}
	
	public void save(final DataModel model) {
		
		// get file name
		JFileChooser chooser=new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.showSaveDialog(null);

		String path = chooser.getSelectedFile().getAbsolutePath();
		String filename = chooser.getSelectedFile().getName();
		
		this.model = model;
		
		new SaveTask(path, filename).execute();
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
		
		private File cdtFile;
		private File atrFile;
		private File gtrFile;

		public SaveTask(final String path, final String fileName) {

			this.filePath = path;
			this.fileName = fileName;
		}

		@Override
		protected Boolean doInBackground() throws Exception {

			final ModelFileGenerator modelGen = new ModelFileGenerator((TVModel) model);

			modelGen.setupWriter(cdtFile);
			modelGen.generateCDT();

			filePath = modelGen.finish();
			
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
				LogBuffer.println("SaveTask is done: success.");
				
			} else {
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
			
			final int fileRootNameSize = filePath.length() - CDT_EXT.length();
			final String newFileRoot = filePath.substring(0, fileRootNameSize);
			
			ensureTreeFilePresence(fileName, newFileRoot, GTR_EXT, ROW_IDX);
			ensureTreeFilePresence(fileName, newFileRoot, ATR_EXT, COL_IDX);
			
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
				axisNeedsTreeFileCheck = model.isRowClustered();
			} else {
				axis_id = "column";
				axisNeedsTreeFileCheck = model.isColClustered();
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
		
		/**
		 * Deletes all files associated with the last clustering step. Also
		 * deletes the directory of the files if it is empty.
		 */
		public void deleteAllFiles() {
			
			deleteFile(cdtFile);
			deleteFile(atrFile);
			deleteFile(gtrFile);
		}
		
		/**
		 * TODO move to ClusterFileStorage
		 * If the passed File object exists and is indeed a normal file, it deletion
		 * will be attempted. The passed object will also be set to null to avoid
		 * lingering of object data.
		 * @param file - The File to be deleted.
		 */
		private void deleteFile(File file) {
			
			if(file == null) {
				return;
			}
			
			boolean success = false;
			String name = file.getName();
			
			if(file.isFile() && file.exists()) {
				success = file.delete();
				LogBuffer.println("Attempted delete of " + name);
				
			} else {
				LogBuffer.println(name + " is not a file or file does not exist.");
			}
			
			if(success) {
				LogBuffer.println(name + " was successfully deleted.");
				//file = null;
				// got a warning for this assignment, not sure what effects of deletion would be. can be deleted if
				// process is not affected
				
			} else {
				LogBuffer.println(name + " could not be deleted.");
			}
		}
	}

}
