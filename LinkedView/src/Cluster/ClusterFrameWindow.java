/*
 * Created on May 31st, 2013
 *
 */
package Cluster;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.TreeViewFrame;


/**
 * @author ChrisK
 *
 */
public class ClusterFrameWindow extends ClusterFrame {

	private static final long serialVersionUID = 1L;

	/**
	 * @param f
	 * @param dataModel2 
	 */
	public ClusterFrameWindow(TreeViewFrame f, DataModel dataModel2) 
	{
		super(f, "Prepare Data for Clustering", dataModel2);
	}
}
