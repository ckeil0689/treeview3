package views;

import java.awt.Frame;

import utilities.CustomDialog;
import utilities.StringRes;

/**
 * Dialog set up for ClusterView. Produces a modal JDialog that contains the
 * elements from ClusterView and overlays over the main application. Used to
 * focus the user on this specific task.
 *
 * @author CKeil
 *
 */
public class ClusterDialog extends CustomDialog {

	/**
	 * Default serial version ID to keep Eclipse happy...
	 */
	private static final long serialVersionUID = 1L;

	private final ClusterView clusterView;

	public ClusterDialog(final int clusterType) {

		super(StringRes.dlg_Cluster);

		clusterView = new ClusterView();

		mainPanel.add(clusterView.makeClusterPanel(clusterType));
		getContentPane().add(mainPanel);
	}

	/**
	 * Resets the entire JDialog contentPane when the user chooses to change the
	 * clustering type. This avoids a continuously growing dialog when switches
	 * are performed and pack() is called.
	 */
	public void reset(final int clusterType) {

		getContentPane().removeAll();
		getContentPane().add(clusterView.makeClusterPanel(clusterType));
		packDialog();
	}

	/**
	 * Packs the JDialog layout and centers it in the application JFrame.
	 */
	private void packDialog() {

		pack();
		setLocationRelativeTo(Frame.getFrames()[0]);
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
