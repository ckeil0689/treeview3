package Utilities;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;

import edu.stanford.genetics.treeview.LogBuffer;

public class AlertDialog extends CustomDialog {
	
	public AlertDialog(String message) {
		
		super("Attention!");
		
		final JLabel alert = GUIFactory.createLabel(message, GUIFactory.FONTS);

		final JButton ok = GUIFactory.createBtn("Continue");
		ok.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				dialog.dispose();
			}
		});

		mainPanel.add(alert, "push, alignx 50%, span, wrap");
		mainPanel.add(ok, "alignx 50%");

		dialog.getContentPane().add(mainPanel);
		
		dialog.getContentPane().revalidate();
		dialog.getContentPane().repaint();
	}
	
	/**
	 * Shows an alert-dialog that displays a supplied message to the user.
	 * The only choice for the user is to dispose the dialog and continue.
	 * @param frame The parent frame for centering the dialog.
	 * @param message The alert to be displayed.
	 */
	public static void showAlert(final String message) {
		
		AlertDialog alert = new AlertDialog(message);
		LogBuffer.println("Alert: " + message);
		alert.setVisible(true);
	}
}
