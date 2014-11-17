package edu.stanford.genetics.treeview;

import java.awt.Dialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import Utilities.GUIFactory;

public class StatsPanel {

	private final TreeViewFrame viewFrame;
	private final JDialog statsDialog;

	/**
	 * Constructor
	 * 
	 * @param viewFrame
	 */
	public StatsPanel(final TreeViewFrame viewFrame) {

		this.viewFrame = viewFrame;

		statsDialog = new JDialog();
		statsDialog.setTitle("Stats");
		statsDialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		statsDialog.setResizable(false);

		statsDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		statsDialog.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent we) {

				statsDialog.dispose();
			}
		});
	}

	/**
	 * Sets up layout and content of this window.
	 */
	public void setupLayout(String source, int rowNum, int colNum) {

		final JPanel mainPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_PADDING, null);

		final JLabel header = GUIFactory.setupHeader("Data Stats");

		final JLabel srcLabel = GUIFactory.createLabel("Source: " 
				+ source, GUIFactory.FONTS);

		final JLabel cols = GUIFactory.createLabel("Columns: "
				+ colNum, GUIFactory.FONTS);

		final JLabel rows = GUIFactory.createLabel("Rows: " + rowNum, 
				GUIFactory.FONTS);

		final JLabel size = GUIFactory.createLabel("Matrix Size (includes "
				+ "N/A-values): " + (rowNum * colNum), GUIFactory.FONTS);

		mainPanel.add(header, "pushx, alignx 50%, wrap");
		mainPanel.add(srcLabel, "wrap");
		mainPanel.add(rows, "wrap");
		mainPanel.add(cols, "wrap");
		mainPanel.add(size, "wrap");


		statsDialog.getContentPane().add(mainPanel);
	}

	/**
	 * Sets the visibility of statsFrame.
	 * 
	 * @param visible
	 */
	public void setVisible(final boolean visible) {

		statsDialog.pack();
		statsDialog.setLocationRelativeTo(viewFrame.getAppFrame());
		statsDialog.setVisible(true);
	}
}
