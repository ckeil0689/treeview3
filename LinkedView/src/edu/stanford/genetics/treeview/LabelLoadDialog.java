package edu.stanford.genetics.treeview;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

public class LabelLoadDialog {

	private JDialog labelLoadFrame;
	private final JLabel prompt;
	private final JPanel mainPanel;

	public LabelLoadDialog(final ViewFrame view) {

		labelLoadFrame = new JDialog();
		labelLoadFrame.setTitle("Loading Labels");
		labelLoadFrame.setDefaultCloseOperation(
				WindowConstants.DISPOSE_ON_CLOSE);
		labelLoadFrame.setResizable(false);
		labelLoadFrame.setModalityType(JDialog.DEFAULT_MODALITY_TYPE);

		mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.setBackground(GUIParams.BG_COLOR);

		prompt = new JLabel("Loading new labels...");
		prompt.setForeground(GUIParams.TEXT);
		prompt.setFont(GUIParams.FONTL);

		mainPanel.add(prompt, "push, alignx 50%, span");

		labelLoadFrame.getContentPane().add(mainPanel);
		labelLoadFrame.pack();
		labelLoadFrame.setLocationRelativeTo(view.getAppFrame());
	}
	
	/**
	 * Sets the visibility of exitFrame;
	 * @param visible
	 */
	public void setVisible(boolean visible) {
		
		labelLoadFrame.setVisible(visible);
	}
	
	/**
	 * Shows a warning if loading labels failed.
	 */
	public void setWarning() {
		
		prompt.setText("Loading failed.");
		prompt.setForeground(GUIParams.RED1);
		
		mainPanel.revalidate();
		mainPanel.repaint();
	}
	
	/**
	 * Used to close the dialog's frame.
	 */
	public void dispose() {
		
		labelLoadFrame.dispose();
	}
}
