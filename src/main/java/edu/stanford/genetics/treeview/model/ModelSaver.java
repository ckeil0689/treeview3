package edu.stanford.genetics.treeview.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import Cluster.TreeFileWriter;
import Controllers.TVController;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.HintDialog;
import edu.stanford.genetics.treeview.LogBuffer;

public class ModelSaver {

	private DataModel model;
	private final TVController tvController;
	
	/**
	 * Handles the process of saving a DataModel object to file.
	 * @param parent - The parent Component over which the Save-dialog will 
	 * appear.
	 */
	public ModelSaver(final TVController tvController) {
		
		this.tvController = tvController;
	}
	
	/**
	 * Modal dialog opened which will wait for save process to finish.
	 * Listens to completion of SwingWorker task and then closes the modal dialog.
	 */
	class SaveCompletionWaiter implements PropertyChangeListener {
		private JDialog dialog;

    public SaveCompletionWaiter(JDialog dialog) {
        this.dialog = dialog;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
    	if ("state".equals(event.getPropertyName()) && 
    		SwingWorker.StateValue.DONE == event.getNewValue()) {
    		dialog.setVisible(false);
    		dialog.dispose();
    	}
    }
  }
	
	/**
	 * Pops up a dialog to define path and filename for the model to be saved.
	 * It then initiates a SaveTask SwingWorker.
	 * @param model - the DataModel to be saved.
	 * @return An indicator whether saving finished successfully
	 */
	public boolean save(final DataModel model, final Path path) {
		
		this.model = model;
	  // This dialog just tells the user about saving being in progress
	  // This helps to avoid confusion for large files, where writing may
		// take a while.
		HintDialog saveHintDialog = new HintDialog("Saving...");
		saveHintDialog.setModal(true);
		
		SaveTask saveTask = new SaveTask(path);
		if(saveTask.shouldAbortSave()) {
			LogBuffer.println("Aborted saving due to user choice.");
			return false;
		}
		
		saveTask.addPropertyChangeListener(new SaveCompletionWaiter(saveHintDialog));
		saveTask.execute();
		saveHintDialog.setVisible(true);
		
		try {
			// get() used to let everything wait for saving to finish (GUI lock!)
			// hence the need for a modal dialog to prevent user confusion
			return saveTask.get();
		}
		catch(InterruptedException | ExecutionException e) {
			LogBuffer.logException(e);
			LogBuffer.println("Saving the file " + path + "has failed.");
			return false;
		}
	}
	
	/**
	 * Worker with the task to generate and write a new CDT file which contains
	 * the newly clustered matrix. Makes sure that the newly created file is
	 * visualized right after clustering, given that the process was not
	 * cancelled.
	 *
	 * @param path - The path where the new file is to be saved
	 */
	private class SaveTask extends SwingWorker<Boolean, Void> {
		
		private boolean hadProblem = false;
		private Path filePath;
		
		private File matrixFile;
		private File atrFile;
		private File gtrFile;

		public SaveTask(final Path path) {

			// TODO this will be false if model was not clustered in current session
			boolean isClustered = model.isRowClustered() || model.isColClustered();
			this.filePath = ModelFileCreator.fixFileExtension(path, isClustered);
			
			// Main file
			// Update file extension according to clustering status of model
			if(isClustered) {
				this.matrixFile = ModelFileCreator.retrieveMainFile(filePath, 
				                                                    model.isRowClustered(), 
				                                                    model.isColClustered(),
				                                                    model.isHierarchical(), 
				                                                    model.getKMeansClusterNum());
			} 
			else {
				this.matrixFile = ModelFileCreator.retrieveDefaultFile(filePath);
			}
			
			// Column tree file
			if(model.isHierarchical() && model.isColClustered()) {
				this.atrFile = ModelFileCreator.retrieveATRFile(filePath);
			}
			
			// Row tree file
			if(model.isHierarchical() && model.isRowClustered()) {
				this.gtrFile = ModelFileCreator.retrieveGTRFile(filePath);
			}
		}

		@Override
		protected Boolean doInBackground() throws Exception {
			
			try {
				// CDT
				matrixFile.createNewFile();
				boolean isCDTWriteOK = writeCDTFile();
				
				// Row tree file
				boolean isGTRWriteOK = true;
				if(model.isRowClustered()) {
					gtrFile.createNewFile();
					isGTRWriteOK = writeTreeFile(gtrFile, model.getGTRData());
				}
				
				// Column tree file
				boolean isATRWriteOK = true;
				if(model.isColClustered()) {
					atrFile.createNewFile();
					isATRWriteOK = writeTreeFile(atrFile, model.getATRData());
				}
				
				return (isCDTWriteOK && isGTRWriteOK && isATRWriteOK);
			} 
			catch(Exception e) {
				hadProblem = true;
				LogBuffer.logException(e);
				String msg = "There was a problem. Could not save the file.";
				JOptionPane.showMessageDialog(JFrame.getFrames()[0], msg);
				LogBuffer.println(msg);
				return Boolean.FALSE;
			}
		}

		@Override
		protected void done() {

			if (!isCancelled() && !hadProblem) {// && hasEnsuredTreeFilePresence()) {
				String filename = filePath.getFileName().toString();
				String dir = filePath.getParent().toString() + File.separator;
				
				FileSet newFS = new FileSet(filename, dir);
				LogBuffer.println("New FileSet created when saving: " + newFS);
				((TVModel) model).setSource(newFS);
				((TVModel) model).setLoaded(true);

				JOptionPane.showMessageDialog(JFrame.getFrames()[0], "Saving complete.");
				LogBuffer.println("Success. Saved file " + model.getFileName());
				tvController.finishModelSave(true);
			} 
			else {
				deleteAllFiles();
				tvController.finishModelSave(false);
			}
		}
		
		/**
		 * Write a CDT matrix file from the TVModel using ModelFileGenerator.
		 * @return An indicator whether the file writing concluded without 
		 * cancellation.
		 */
		private boolean writeCDTFile() {
			
			final ModelFileGenerator modelGen = 
				new ModelFileGenerator((TVModel) model);
			modelGen.setupWriter(matrixFile);
			modelGen.generateCDT();
			
			if(isCancelled()) {
				return Boolean.FALSE;
			}

			return Boolean.TRUE;
		}
		
		/**
		 * Writes a tree file which contains all tree node data for the given axis.
		 * @param treeFile - The file which to write the tree data to.
		 * @param treeNodeData - The node pair data to write to treeFile.
		 * @return A boolean indicating whether the write process was successful.
		 */
    private boolean writeTreeFile(final File treeFile, 
                                  final List<String[]> treeNodeData) {
    	
    	TreeFileWriter treeFileWriter = new TreeFileWriter(treeFile);
    	boolean wasWriteSuccessful = treeFileWriter.writeData(treeNodeData);
    	treeFileWriter.closeWriter();
    	
    	return wasWriteSuccessful;
    }
		
//		/**
//		 * Makes sure that a tree file exists for an axis that is supposed to
//		 * be clustered. If not, it attempts to take one from a previous 
//		 * cluster and if that does not exist either it will consider an axis
//		 * as not clustered. In that case, a tree file will not be present. Returns
//		 * true upon successful completion.
//		 */
//		private boolean hasEnsuredTreeFilePresence() {
//	
//			if(filePath == null || fileName == null) {
//				return false;
//			}
//			
//			final int fileRootNameSize = filePath.length() - CDT_EXT.length();
//			final String newFileRoot = filePath.substring(0, fileRootNameSize);
//			
//			ensureTreeFilePresence(fileName, newFileRoot, GTR_EXT, ROW_IDX);
//			ensureTreeFilePresence(fileName, newFileRoot, ATR_EXT, COL_IDX);
//			
//			return true;
//		}
//		
//		/**
//		 * For a given axis, this ensures that its tree file exists in case
//		 * it is considered to be clustered. This is useful especially if
//		 * an axis was already clustered before so that the old tree file can
//		 * be carried over to the new FileSet.
//		 * @param oldFileRoot The root name of the older cdt file.
//		 * @param newFileRoot The root name of the new cdt file.
//		 * @param treeFileSuffix The tree file suffix to be used if a new file
//		 * is created.
//		 * @param axisIdx An integer identifying the axis.
//		 */
//		private void ensureTreeFilePresence(final String oldFileRoot, 
//				final String newFileRoot, final String treeFileSuffix, 
//				final int axisIdx) { 
//			
//			String axis_id;
//			boolean axisNeedsTreeFileCheck;
//			
//			if(axisIdx == ROW_IDX) {
//				axis_id = "row";
//				axisNeedsTreeFileCheck = model.isRowClustered();
//			} else {
//				axis_id = "column";
//				axisNeedsTreeFileCheck = model.isColClustered();
//			}
//			
//			if(axisNeedsTreeFileCheck) {
//				String newTreeFilePath = newFileRoot + treeFileSuffix;
//				String oldTreeFilePath = oldFileRoot + treeFileSuffix;
//				
//				if(!doesFileExist(newTreeFilePath)) {
//					LogBuffer.println("No file found for " + axis_id 
//							+ " trees.");
//					if(doesFileExist(oldTreeFilePath)) {
//						LogBuffer.println("But old " + axis_id 
//								+ " tree file was found!");
//						copyFile(oldTreeFilePath, newTreeFilePath);
//					} else {
//						String message = "The tree file for the " + axis_id 
//								+ " axis could not be recovered. No trees "
//								+ "can be shown.";
//						JOptionPane.showMessageDialog(clusterDialog, message);
//					}
//				} else {
//					LogBuffer.println("Success! The " + axis_id 
//							+ " tree file was found.");
//				}
//			} else {
//				LogBuffer.println("The " + axis_id 
//						+ "s have not been clustered.");
//			}
//		}
//		
//		/**
//		 * Copies an old file to a new one with the correct file name. This 
//		 * is used for transferring old tree files to new clustered matrices.
//		 * @param oldTreeFilePath The path of the file to be copied.
//		 * @param newTreeFilePath The path to which the old file will be copied.
//		 */
//		private boolean copyFile(final String oldTreeFilePath, 
//				final String newTreeFilePath) {
//			
//			try(FileInputStream srcStream = 
//					new FileInputStream(oldTreeFilePath); 
//				FileOutputStream dstStream = 
//						new FileOutputStream(newTreeFilePath)) {
//				
//				dstStream.getChannel().transferFrom(srcStream.getChannel(), 
//						0, srcStream.getChannel().size());
//				return true;
//				
//			} catch (IOException e) {
//				LogBuffer.logException(e);
//				return false;
//			} 
//		}
//		
		/**
		 * Check for duplicate main file. If the user does not want to overwrite
		 * a duplicate file, then cancel the saving procedure.
		 * @return boolean whether the save process should be aborted.
		 */
		public boolean shouldAbortSave() {
			
			if (matrixFile.exists() && !matrixFile.isDirectory()) {
				int response = JOptionPane.showConfirmDialog(null,
				                              "A file with this name already exists. " +
				                              "Do you want to replace it?",
				                              "Confirm",
				                              JOptionPane.YES_NO_OPTION,
				                              JOptionPane.QUESTION_MESSAGE);
				return (response != JOptionPane.YES_OPTION);
			}
			return false;
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
			
			deleteFile(matrixFile);
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
