package Utilities;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;

import edu.stanford.genetics.treeview.LogBuffer;

public class ErrorDialog extends CustomDialog {
	
	public ErrorDialog(final String message) {
		
		super("Oh oh!");
		
		final JLabel ants = GUIFactory.createLabel("There must be ants "
				+ "in the system.", GUIFactory.FONTS);
		final JLabel warning = GUIFactory.createLabel("Here is what happened: " 
				+ message, GUIFactory.FONTS);

		final JButton ok = GUIFactory.createBtn("Continue");
		ok.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				dialog.dispose();
			}
		});

		mainPanel.add(ants, "push, alignx 50%, span, wrap");
		mainPanel.add(warning, "push, alignx 50%, span, wrap");
		mainPanel.add(ok, "alignx 50%");

		dialog.getContentPane().add(mainPanel);
	}
	
	/**
	 * Opens a JDialog that displays a supplied error message. Furthermore,
	 * it prints the message from the exception to the log. 
	 * @param parentFrame The parent frame for the appearing JDialog.
	 * @param message The error message to be shown.
	 * @param e The exception that necessitates the popup dialog..
	 */
	public static void showError(final String message, final Exception e) {
		
		ErrorDialog error = new ErrorDialog(message);
		LogBuffer.println(e.getMessage());
		error.setVisible(true);		
	}
}
