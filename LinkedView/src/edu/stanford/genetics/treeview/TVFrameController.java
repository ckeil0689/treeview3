package edu.stanford.genetics.treeview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import Cluster.ClusterProcessor;

import edu.stanford.genetics.treeview.model.CDTCreator3;

public class TVFrameController {

	private TreeViewFrame tvFrame;
	private SwingWorker<File, Void> worker;
	private File file;
	private String fileType;
	
	public TVFrameController(TreeViewFrame tvFrame) {
		
		this.tvFrame = tvFrame;
		
		setupWorkerThread();
		
		// add listeners to TVFrame
		tvFrame.addLoadListener(new LoadPanelListener(tvFrame.getLoadIcon(), 
				tvFrame.getLoadIcon().getLabel()));
		tvFrame.addLoadListener(new LoadListener());
		tvFrame.addContinueListener(new ContinueListener());
	}
	
	/**
	 * Handles the loading of data.
	 * @author CKeil
	 *
	 */
	class LoadPanelListener extends SSMouseListener {
		
		public LoadPanelListener(JPanel panel, JLabel label) {
			
			super(panel, label);
		}

		@Override
		public void mouseClicked(MouseEvent arg0) {

			openFile();
		}	
	}
	
	/**
	 * Handles the new loading of data.
	 * @author CKeil
	 *
	 */
	class LoadListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			tvFrame.setDataModel(null);
			openFile();
		}	
	}
	
	/**
	 * Sets TVFrame "loaded" to true which triggers the setup of DendroView.
	 * @author CKeil
	 *
	 */
	class ContinueListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			if(tvFrame.getDataModel() != null) {
				tvFrame.setLoaded(true);
				
			} else {
				System.out.println("Couldn't continue, dataModel is null.");
			}
		}	
	}
	
	/**
	 * Setting up a worker thread to do the CDT generation if a non-cdt file was
	 * selected by the user. This prevents the GUI from locking up and allows
	 * the ProgressBar to display progress.
	 */
	public void setupWorkerThread() {
		
		worker = new SwingWorker<File, Void>() {

			@Override
			public File doInBackground() {
				
				try {
					final CDTCreator3 fileTransformer = 
							new CDTCreator3(file, fileType, tvFrame);
					
					fileTransformer.createFile();
	
					file = new File(fileTransformer.getFilePath());
					
				} catch (IOException e) {
					System.out.println("Could not generate CDT file. Cause: " +
							e.getCause());
					e.printStackTrace();
				}
				
				return file;
			}

			@Override
			protected void done() {

				System.out.println("Tranform worker done.");
			}
		};
	}
	
	/**
	 * This method opens a file dialog to open either the visualization view or
	 * the cluster view depending on which file type is chosen.
	 * @throws IOException 
	 * 
	 * @throws LoadException
	 */
	public void openFile() {
		
		try {
			file = tvFrame.selectFile();
			tvFrame.setLoading();
			final String fileName = file.getAbsolutePath();
			final int dotIndex = fileName.indexOf(".");

			final int suffixLength = fileName.length() - dotIndex;

			fileType = file.getAbsolutePath().substring(
					fileName.length() - suffixLength, 
					fileName.length());
			
		} catch (LoadException e1) {
			System.out.println("File could not be retrieved. Cause:" +
					e1.getCause());
			e1.printStackTrace();
		}
		
		if (!fileType.equalsIgnoreCase(".cdt")) {
			try {
				worker.execute();
				file = worker.get();
				
			} catch (InterruptedException e) {
				System.out.println("Getting or loading the FileSet " +
						"was interrupted. Cause: " + e.getCause());
				e.printStackTrace();
				
			} catch (ExecutionException e) {
				System.out.println("Task was aborted due to " +
						"another exception in worker thread. Cause: " 
						+ e.getCause());
				e.printStackTrace();
			}
		}
		
		try {
			FileSet fileSet = tvFrame.getFileSet(file);
		
			// Loading TVModel
			tvFrame.loadFileSet(fileSet);
			
			if(fileSet != null) {
				fileSet = tvFrame.getFileMRU().addUnique(fileSet);
				tvFrame.getFileMRU().setLast(fileSet);
				
			} else {
				System.out.println("FileSet is null.");
			}
		} catch (LoadException e) {
			System.out.println("Loading the FileSet was interrupted. " +
					"Cause: " + e.getCause());
			e.printStackTrace();
		}
	}
	
}
