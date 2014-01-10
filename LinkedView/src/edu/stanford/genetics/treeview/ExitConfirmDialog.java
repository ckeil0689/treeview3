package edu.stanford.genetics.treeview;

import java.awt.Dimension;
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

		final JButton ok = setButtonLayout(new JButton("Yes"));
		ok.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				view.closeWindow();
			}
		});

		mainPanel.add(prompt, "push, alignx 50%, span, wrap");
		mainPanel.add(ok, "alignx 50%");

		getContentPane().add(mainPanel);
		pack();
		setLocationRelativeTo(null);
	}

	public JButton setButtonLayout(final JButton button) {

		final Dimension d = button.getPreferredSize();
		d.setSize(d.getWidth() * 1.5, d.getHeight() * 1.5);
		button.setPreferredSize(d);

		button.setFont(GUIParams.FONTS);
		button.setBackground(GUIParams.ELEMENT);
		button.setForeground(GUIParams.BG_COLOR);

		return button;
	}
}
