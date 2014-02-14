package Cluster;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.TreeViewFrame;

/**
 * Frame set up for ClusterView.
 * @author CKeil
 *
 */
public class ClusterViewFrame {
	
	private JFrame clusterFrame;
	private ClusterView clusterView;

	public ClusterViewFrame(TreeViewFrame tvFrame) {
		
		clusterFrame = new JFrame("Hierarchical Clustering");
		
		clusterView = new ClusterView(tvFrame);
		
		// Setting preferred size for the ContentPane of this frame
		final Dimension mainDim = GUIParams.getScreenSize();
		clusterFrame.getContentPane().setPreferredSize(
				new Dimension(mainDim.width * 1/2, mainDim.height * 3/4));
		
		clusterFrame.setMinimumSize(new Dimension(800, 600));

		// setup frame options
		clusterFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// Makes the frame invisible when the window is closed
		clusterFrame.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(final WindowEvent we) {
				
				clusterFrame.dispose();
			}
		});
		
		clusterFrame.getContentPane().add(clusterView);
		
		clusterFrame.pack();	
		clusterFrame.setLocationRelativeTo(tvFrame.getAppFrame());
	}
	
	/**
	 * Sets the visibility of clusterFrame.
	 * @param visible
	 */
	public void setVisible(boolean visible) {
		
		clusterFrame.setVisible(visible);
	}
	
	/**
	 * Returns the loaded instance of ClusterView
	 * @return ClusterView clusterView
	 */
	public ClusterView getClusterView() {
		
			return clusterView;
	}

	

}
