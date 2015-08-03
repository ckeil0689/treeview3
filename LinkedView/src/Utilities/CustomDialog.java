package Utilities;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * Basic custom JDialog that can be extended for more specified behavior.
 *
 * @author CKeil
 *
 */
public class CustomDialog extends JDialog {

	/**
	 * Default serial version ID to keep Eclipse happy...
	 */
	private static final long serialVersionUID = 1L;

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

		super();
		setTitle(title);
		setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setResizable(false);

		this.closeBtn = GUIFactory.createBtn("Close");
		closeBtn.addActionListener(new CloseListener());

		this.mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT,
				null);

		getContentPane().add(mainPanel);
	}

	private class CloseListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			dispose();
		}
	}

	/**
	 * Sets the visibility of exitFrame;
	 *
	 * @param ViewFrame
	 *            The main view, used to center the dialog on screen.
	 * @param boolean Sets the visibility status of the dialog.
	 */
	@Override
	public void setVisible(final boolean visible) {

		pack();
		setLocationRelativeTo(JFrame.getFrames()[0]);
		closeBtn.requestFocus();

		super.setVisible(visible);
	}
	
	/**
	 * Allows to set a text to be displayed as indicator dialog.
	 * The passed String is everything that will be displayed.
	 * @param message The message to be displayed.
	 */
	public void setIndicatorPanel(final String message) {
		
		mainPanel.removeAll();
		JLabel indicator = GUIFactory.createLabel(message, GUIFactory.FONTM);
		mainPanel.add(indicator, "push");
		
		mainPanel.revalidate();
		mainPanel.repaint();
	}
}
