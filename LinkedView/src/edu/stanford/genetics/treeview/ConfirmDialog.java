package edu.stanford.genetics.treeview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;

import Utilities.CustomDialog;
import Utilities.GUIFactory;

/**
 * Small dialog used to confirm the decision to exit the program.
 * @author CKeil
 *
 */
public class ConfirmDialog extends CustomDialog {
	
	private boolean confirmed;

	/**
	 * Constructs a dialog that provides the user with a second chance
	 * in case the closing operation for the application was accidentally 
	 * invoked.
	 * @param view The main view instance.
	 * @param title Title for this ConfirmDialog.
	 * @param intent The action to be confirmed by the user.
	 */
	public ConfirmDialog(final ViewFrame view, String intent) {

		super("Confirm");

		final JLabel prompt = GUIFactory.createLabel("Are you sure "
				+ "you want to continue the following task:", GUIFactory.FONTS);
		final JLabel task = GUIFactory.createLabel(intent, GUIFactory.FONTS);

		final JButton ok = GUIFactory.createBtn("OK");
		ok.addActionListener(new ConfirmListener());

		final JButton cancel = GUIFactory.createBtn("Cancel");
		cancel.addActionListener(new DenyListener());

		mainPanel.add(prompt, "push, alignx 50%, span, wrap");
		mainPanel.add(task, "push, alignx 50%, span, wrap");
		mainPanel.add(ok, "pushx, alignx 50%");
		mainPanel.add(cancel, "pushx, alignx 50%");

		dialog.getContentPane().add(mainPanel);
		dialog.pack();
		dialog.setLocationRelativeTo(view.getAppFrame());
	}
	
	/**
	 * Gets the result of the user choice about closing the application.
	 * @return boolean 
	 */
	public boolean getConfirmed() {

		return confirmed;
	}
	
	class ConfirmListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			confirmed = true;
			dialog.dispose();
		}
	}

	class DenyListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			confirmed = false;
			dialog.dispose();
		}
	}
}
