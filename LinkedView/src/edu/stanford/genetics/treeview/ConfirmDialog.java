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

public class ConfirmDialog {

	private final JDialog confirmDialog;
	private boolean confirmed;

	public ConfirmDialog(final ViewFrame viewFrame, final String function) {

		confirmDialog = new JDialog();
		confirmDialog.setTitle("Confirm");
		confirmDialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		confirmDialog
				.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		confirmDialog.setResizable(false);

		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.setBackground(GUIUtils.BG_COLOR);

		final JLabel prompt = new JLabel("Are you sure you want to " + function
				+ "?");
		prompt.setForeground(GUIUtils.TEXT);
		prompt.setFont(GUIUtils.FONT_MENU);

		final JButton ok = GUIUtils.setButtonLayout("OK", null);
		ok.addActionListener(new ConfirmListener());

		final JButton cancel = GUIUtils.setButtonLayout("Cancel", null);
		cancel.addActionListener(new DenyListener());

		mainPanel.add(prompt, "push, alignx 50%, span, wrap");
		mainPanel.add(ok, "pushx, alignx 50%");
		mainPanel.add(cancel, "pushx, alignx 50%");

		confirmDialog.getContentPane().add(mainPanel);
		confirmDialog.pack();
		confirmDialog.setLocationRelativeTo(viewFrame.getAppFrame());
	}

	class ConfirmListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			confirmed = true;
			confirmDialog.dispose();
		}
	}

	class DenyListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			confirmed = false;
			confirmDialog.dispose();
		}
	}

	public boolean getConfirmed() {

		return confirmed;
	}

	/**
	 * Sets the visibility of exitFrame;
	 * 
	 * @param visible
	 */
	public void setVisible(final boolean visible) {

		confirmDialog.setVisible(visible);
	}
}
