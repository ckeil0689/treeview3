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
import Views.HintDialog;
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
    		
				JOptionPane.showMessageDialog(JFrame.getFrames()[0], 
				                              "Saving complete.");
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

			// actual copy needed, otherwise the reference causes a race condition
			String oldFSName = model.getFileSet().getName(); 
			LogBuffer.println("Old FileSetName in ModelSaver.done(): " + oldFSName);
			if (!isCancelled() && !hadProblem) {// && hasEnsuredTreeFilePresence()) {
				String filename = filePath.getFileName().toString();
				String dir = filePath.getParent().toString() + File.separator;
				
				LogBuffer.println("Creating new FileSet from saved file.");
				// TODO issue is in next statement - changes the main FileSet node
				FileSet newFS = new FileSet(filename, dir);
				LogBuffer.println("New FileSet created when saving: " + newFS);
				((TVModel) model).setSource(newFS);
				((TVModel) model).setLoaded(true);

				LogBuffer.println("Success. Saved file " + model.getFileName());
				tvController.finishModelSave(true, oldFSName);
			} 
			else {
				deleteAllFiles();
				tvController.finishModelSave(false, oldFSName);
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
			modelGen.generateMainFile();
			
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
			} 
			else {
				LogBuffer.println(name + " is not a file or file does not exist.");
			}
			
			if(success) {
				LogBuffer.println(name + " was successfully deleted.");
			} 
			else {
				LogBuffer.println(name + " could not be deleted.");
			}
		}
	}

}
