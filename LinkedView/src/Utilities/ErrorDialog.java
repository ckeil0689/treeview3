package Utilities;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class ErrorDialog extends CustomDialog {
	
	public ErrorDialog(final JFrame parentFrame, final String message) {
		
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
		dialog.pack();
		dialog.setLocationRelativeTo(parentFrame);
	}
}
