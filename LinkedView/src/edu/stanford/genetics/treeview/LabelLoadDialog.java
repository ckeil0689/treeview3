package edu.stanford.genetics.treeview;

import java.awt.Dialog;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

public class LabelLoadDialog {

	private final JDialog labelLoadFrame;
	private final JLabel prompt;
	private final JPanel mainPanel;
	private final JProgressBar pBar;

	public LabelLoadDialog(final ViewFrame view, String type) {

		labelLoadFrame = new JDialog();
		labelLoadFrame.setTitle("Loading Labels");
		labelLoadFrame
				.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		labelLoadFrame.setResizable(false);
		labelLoadFrame.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);

		mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.setBackground(GUIUtils.BG_COLOR);

		prompt = new JLabel("Loading new " + type + " labels...");
		prompt.setForeground(GUIUtils.TEXT);
		prompt.setFont(GUIUtils.FONTL);

		pBar = GUIUtils.setPBarLayout();

		mainPanel.add(prompt, "pushx, alignx 50%, span, wrap");
		mainPanel.add(pBar, "pushx, alignx 50%, span");

		labelLoadFrame.getContentPane().add(mainPanel);
		labelLoadFrame.pack();
		labelLoadFrame.setLocationRelativeTo(view.getAppFrame());
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
		prompt.setForeground(GUIUtils.RED1);

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
