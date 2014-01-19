package Cluster;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.TreeViewFrame;

public class ClusterViewFrame extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private ClusterView clusterView;

	public ClusterViewFrame(DataModel dataModel, TreeViewFrame viewFrame, 
			boolean hierarchical) {
		
		super("Hierarchical Clustering");
		
		this.clusterView = new ClusterView(dataModel, viewFrame, hierarchical);
		
		// Setting preferred size for the mainPanel
//		final Toolkit toolkit = Toolkit.getDefaultToolkit();
//		final Dimension mainDim = toolkit.getScreenSize();
//		final Rectangle rectangle = new Rectangle(mainDim);
//		mainDim.setSize(rectangle.height, rectangle.height * 3 / 4);

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
