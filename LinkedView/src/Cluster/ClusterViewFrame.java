package Cluster;

import java.io.File;
import java.util.Observer;

import javax.swing.JFileChooser;

import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.TreeViewApp;
import edu.stanford.genetics.treeview.TreeViewFrame;

public class ClusterViewFrame extends TreeViewFrame implements Observer
{

	private static final long serialVersionUID = 1L;

	public ClusterViewFrame(TreeViewApp treeview)
	{
		super(treeview);
		// TODO Auto-generated constructor stub
	}

	/**
	* Open a dialog which allows the user to select a new data file
	*
	* @return The fileset corresponding to the dataset.
	*/
	protected ClusterFileSet clusterSelection()
	throws LoadException
	{
		ClusterFileSet fileSet1; // will be chosen...
		JFileChooser fileDialog = new JFileChooser();
		setupFileDialog(fileDialog);
		int retVal = fileDialog.showOpenDialog(this);
		if (retVal == JFileChooser.APPROVE_OPTION) {
			File chosen = fileDialog.getSelectedFile();
			fileSet1 = new ClusterFileSet(chosen.getName(), chosen.getParent()
					+File.separator);
		} else {
			throw new LoadException("File Dialog closed without selection...", 
					LoadException.NOFILE);
		}

		return fileSet1;
	}
}
