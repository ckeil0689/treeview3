package Cluster;

import java.awt.Dialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import edu.stanford.genetics.treeview.StringRes;

/**
 * Frame set up for ClusterView.
 * 
 * @author CKeil
 * 
 */
public class ClusterViewDialog {

	private final JDialog clusterDialog;
	private final ClusterView clusterView;
	private final JFrame parentFrame;

	public ClusterViewDialog(final JFrame parentFrame,
			final String clusterType) {
		
		this.parentFrame = parentFrame;
		
		clusterDialog = new JDialog();
		clusterDialog.setTitle(StringRes.dialog_title_Cluster);
		clusterDialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		clusterDialog.setResizable(false);

		clusterView = new ClusterView(clusterType);

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

		clusterDialog.getContentPane().add(clusterView.makeClusterPanel());

		packDialog();
	}
	
	/**
	 * Resets the entire JDialog contentPane when the user chooses to change
	 * the clustering type. This avoids a continuously growing dialog when
	 * switches are performed and pack() is called.
	 */
	public void reset() {
		
		clusterDialog.getContentPane().removeAll();
		clusterDialog.getContentPane().add(clusterView.makeClusterPanel());
		packDialog();
	}
	
	/**
	 * Packs the JDialog layout and centers it in the application JFrame.
	 */
	public void packDialog() {
		
		clusterDialog.pack();
		clusterDialog.setLocationRelativeTo(parentFrame);
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
	
	/**
	 * Test method to check UI and UX.
	 * @param args
	 */
	public static void main(String[] args) {
		
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				
				ClusterViewDialog cd = new ClusterViewDialog(new JFrame(), 
						StringRes.menu_title_Hier);
				cd.setVisible(true);
			}
		});
	}

}
