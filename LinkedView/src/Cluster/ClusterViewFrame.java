package Cluster;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.TreeViewFrame;

public class ClusterViewFrame extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private ClusterView clusterView;

	public ClusterViewFrame(DataModel dataModel, TreeViewFrame viewFrame, 
			boolean hierarchical) {
		
		super("Hierarchical Clustering");
		
		this.clusterView = new ClusterView(dataModel, viewFrame, hierarchical);
		
		// Setting preferred size for the ContentPane of this frame
		final Dimension mainDim = GUIParams.getScreenSize();
		getContentPane().setPreferredSize(new Dimension(mainDim.width * 1/2, 
				mainDim.height * 1/2));

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
		
		setLocationRelativeTo(viewFrame);
		pack();	
	}

	

}
