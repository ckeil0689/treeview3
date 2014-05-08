package Cluster;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.WindowConstants;

import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.TreeViewFrame;

/**
 * Frame set up for ClusterView.
 * 
 * @author CKeil
 * 
 */
public class ClusterViewDialog {

	private final JDialog clusterDialog;
	private final ClusterView clusterView;

	public ClusterViewDialog(final TreeViewFrame tvFrame,
			final String clusterType) {

		clusterDialog = new JDialog();
		clusterDialog.setTitle("Clustering");
		clusterDialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		clusterDialog.setResizable(false);

		clusterView = new ClusterView(tvFrame, clusterType);

		// Setting preferred size for the ContentPane of this frame
		final Dimension mainDim = GUIParams.getScreenSize();
		clusterDialog.getContentPane().setPreferredSize(
				new Dimension(mainDim.width * 1 / 2, mainDim.height * 3 / 4));

		clusterDialog.setMinimumSize(new Dimension(800, 600));

		// setup frame options
		clusterDialog
				.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// Makes the frame invisible when the window is closed
		clusterDialog.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent we) {

				clusterDialog.dispose();
			}
		});

		clusterDialog.getContentPane().add(clusterView);

		clusterDialog.pack();
		clusterDialog.setLocationRelativeTo(tvFrame.getAppFrame());
	}

	/**
	 * Sets the visibility of clusterFrame.
	 * 
	 * @param visible
	 */
	public void setVisible(final boolean visible) {

		clusterDialog.setVisible(visible);
	}

	/**
	 * Returns the loaded instance of ClusterView
	 * 
	 * @return ClusterView clusterView
	 */
	public ClusterView getClusterView() {

		return clusterView;
	}

}
