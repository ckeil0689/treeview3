package edu.stanford.genetics.treeview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

public class ExitConfirmDialog extends JFrame {

	private static final long serialVersionUID = 1L;

	public ExitConfirmDialog(final ViewFrame view) {

		super("Confirm Exit");
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setResizable(false);

		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.setBackground(GUIParams.BG_COLOR);

		final JLabel prompt = new JLabel(
				"Are you sure you want to close TreeView?");
		prompt.setForeground(GUIParams.TEXT);
		prompt.setFont(GUIParams.FONTS);

		final JButton ok = GUIParams.setButtonLayout("Yes", null);
		ok.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				view.dispose();
			}
		});

		mainPanel.add(prompt, "push, alignx 50%, span, wrap");
		mainPanel.add(ok, "alignx 50%");

		getContentPane().add(mainPanel);
		pack();
		setLocationRelativeTo(view);
	}
}
