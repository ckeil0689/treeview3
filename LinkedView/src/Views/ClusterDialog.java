package Views;

import java.awt.Frame;

import Utilities.CustomDialog;
import Utilities.StringRes;

/**
 * Dialog set up for ClusterView. Produces a modal JDialog that contains the
 * elements from ClusterView and overlays over the main application. Used to
 * focus the user on this specific task.
 *
 * @author CKeil
 *
 */
public class ClusterDialog extends CustomDialog {

	private final ClusterView clusterView;

	public ClusterDialog(final int clusterType) {

		super(StringRes.dlg_Cluster);

		clusterView = new ClusterView();

		mainPanel.add(clusterView.makeClusterPanel(clusterType));
		dialog.getContentPane().add(mainPanel);

		packDialog();
	}

	/**
	 * Resets the entire JDialog contentPane when the user chooses to change the
	 * clustering type. This avoids a continuously growing dialog when switches
	 * are performed and pack() is called.
	 */
	public void reset(final int clusterType) {

		dialog.getContentPane().removeAll();
		dialog.getContentPane().add(clusterView.makeClusterPanel(clusterType));
		packDialog();
	}

	/**
	 * Packs the JDialog layout and centers it in the application JFrame.
	 */
	private void packDialog() {

		dialog.pack();
		dialog.setLocationRelativeTo(Frame.getFrames()[0]);
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
	 * Closes the JDialog.
	 */
	public void dispose() {

		dialog.dispose();
	}

	/**
	 * Test method to check UI and UX.
	 *
	 * @param args
	 */
	public static void main(final String[] args) {

		// SwingUtilities.invokeLater(new Runnable() {
		//
		// @Override
		// public void run() {
		//
		// new JFrame(); // needs top level frame to center jDialog
		// new ClusterDialog(ClusterView.KMEANS).setVisible(true);
		// }
		// });
	}
}
