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
public class ClusterViewFrame extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private ClusterView clusterView;

	public ClusterViewFrame(TreeViewFrame tvFrame) {
		
		super("Hierarchical Clustering");
		
		clusterView = new ClusterView(tvFrame);
		
		// Setting preferred size for the ContentPane of this frame
		final Dimension mainDim = GUIParams.getScreenSize();
		getContentPane().setPreferredSize(new Dimension(mainDim.width * 1/2, 
				mainDim.height * 3/4));
		
		setMinimumSize(new Dimension(800, 600));

		// setup frame options
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// Makes the frame invisible when the window is closed
		this.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(final WindowEvent we) {
				
				ClusterViewFrame.this.dispose();
			}
		});
		
		getContentPane().add(clusterView);
		
		pack();	
		setLocationRelativeTo(tvFrame);
	}
	
	/**
	 * Returns the loaded instance of ClusterView
	 * @return ClusterView clusterView
	 */
	public ClusterView getClusterView() {
		
			return clusterView;
	}

	

}
