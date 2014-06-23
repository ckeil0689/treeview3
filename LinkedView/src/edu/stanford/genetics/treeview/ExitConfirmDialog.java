package edu.stanford.genetics.treeview;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

public class ExitConfirmDialog {
	
	private JDialog exitFrame;

	public ExitConfirmDialog(final ViewFrame view) {

		exitFrame = new JDialog();
		exitFrame.setTitle("Confirm Exit");
		exitFrame.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		exitFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		exitFrame.setResizable(false);

		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.setBackground(GUIUtils.BG_COLOR);

		final JLabel prompt = new JLabel(
				"Are you sure you want to close TreeView?");
		prompt.setForeground(GUIUtils.TEXT);
		prompt.setFont(GUIUtils.FONTS);

		final JButton ok = GUIUtils.setButtonLayout("Yes", null);
		ok.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				exitFrame.dispose();
				view.getAppFrame().dispose();
			}
		});

		mainPanel.add(prompt, "push, alignx 50%, span, wrap");
		mainPanel.add(ok, "alignx 50%");

		exitFrame.getContentPane().add(mainPanel);
		exitFrame.pack();
		exitFrame.setLocationRelativeTo(view.getAppFrame());
	}
	
	/**
	 * Sets the visibility of exitFrame;
	 * @param visible
	 */
	public void setVisible(boolean visible) {
		
		exitFrame.setVisible(visible);
	}
}
