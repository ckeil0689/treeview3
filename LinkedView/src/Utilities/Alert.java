package Utilities;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;

import edu.stanford.genetics.treeview.ViewFrame;

public class Alert extends CustomDialog {
	
	public Alert(final ViewFrame view, String message) {
		
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
		dialog.pack();
		dialog.setLocationRelativeTo(view.getAppFrame());
	}
}
