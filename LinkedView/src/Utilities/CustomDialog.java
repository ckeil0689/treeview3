package Utilities;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * Basic custom JDialog that can be extended for more specified behavior.
 *
 * @author CKeil
 *
 */
public class CustomDialog {

	protected JDialog dialog;
	protected JPanel mainPanel;
	protected JButton closeBtn;

	/**
	 * Constructs a basic JDialog with some custom behavior that differs from a
	 * raw JDialog. For example, CustomDialog objects are always modal by
	 * default (cannot be unfocused on Windows machines).
	 *
	 * @param String
	 *            Title to be given to this dialog.
	 */
	public CustomDialog(final String title) {

		this.dialog = new JDialog();
		dialog.setTitle(title);
		dialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setResizable(false);

		this.closeBtn = GUIFactory.createBtn("Close");
		closeBtn.addActionListener(new CloseListener());

		this.mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT,
				null);
	}

	private class CloseListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			dialog.dispose();
		}
	}

	/**
	 * Sets the visibility of exitFrame;
	 *
	 * @param ViewFrame
	 *            The main view, used to center the dialog on screen.
	 * @param boolean Sets the visibility status of the dialog.
	 */
	public void setVisible(final boolean visible) {

		dialog.pack();
		dialog.setLocationRelativeTo(Frame.getFrames()[0]);
		closeBtn.requestFocus();
		dialog.setVisible(visible);
	}
}
