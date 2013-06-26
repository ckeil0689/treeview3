package edu.stanford.genetics.treeview;

import Cluster.ClusterFileSet;


/**
 * Interface for things that want to know when a file set has moved.
 * @author alok
 *
 */
public interface FileSetListener {
	public void onFileSetMoved(FileSet fileset);
	public void onFileSetMoved(ClusterFileSet fileset);
}
