package edu.stanford.genetics.treeview;

import java.awt.Dialog;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;

import Utilities.GUIFactory;
import Utilities.StringRes;

public class LabelLoadDialog {

	private final JDialog labelLoadFrame;
	private final JLabel prompt;
	private final JPanel mainPanel;
	private final JProgressBar pBar;

	public LabelLoadDialog(String type) {

		labelLoadFrame = new JDialog();
		labelLoadFrame.setTitle(StringRes.dlg_loadLabels);
		labelLoadFrame
				.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		labelLoadFrame.setResizable(false);
		labelLoadFrame.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);

		mainPanel = GUIFactory.createJPanel(false, true, null);

		prompt = GUIFactory.createLabel("Loading new " + type + " labels...", 
				GUIFactory.FONTM);

		pBar = GUIFactory.createPBar();

		mainPanel.add(prompt, "pushx, alignx 50%, span, wrap");
		mainPanel.add(pBar, "pushx, alignx 50%, span");

		labelLoadFrame.getContentPane().add(mainPanel);
		labelLoadFrame.pack();
		labelLoadFrame.setLocationRelativeTo(JFrame.getFrames()[0]);
	}

	/**
	 * Sets the visibility of exitFrame;
	 * 
	 * @param visible
	 */
	public void setVisible(final boolean visible) {

		labelLoadFrame.setVisible(visible);
	}

	/**
	 * Shows a warning if loading labels failed.
	 */
	public void setWarning() {

		prompt.setText("Loading failed.");
		prompt.setForeground(GUIFactory.RED1);

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	public void setPBarMax(final int max) {

		pBar.setMaximum(max);
	}

	public void updateProgress(final int value) {

		pBar.setValue(value);
	}

	/**
	 * Used to close the dialog's frame.
	 */
	public void dispose() {

		labelLoadFrame.dispose();
	}
}
