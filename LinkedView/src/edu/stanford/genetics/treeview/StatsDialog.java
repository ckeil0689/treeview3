package edu.stanford.genetics.treeview;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import utilities.CustomDialog;
import utilities.GUIFactory;

public class StatsDialog extends CustomDialog {

	/**
	 * Default serial version ID to keep Eclipse happy...
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 *
	 * @param viewFrame
	 */
	public StatsDialog(final String source, final int rowNum, final int colNum) {

		super("Stats");
		setupLayout(source, rowNum, colNum);

		closeBtn.requestFocusInWindow();
	}

	/**
	 * Sets up layout and content of this window.
	 */
	public void setupLayout(final String source, final int rowNum,
			final int colNum) {

		final JPanel contentPanel = GUIFactory.createJPanel(false,
				GUIFactory.DEFAULT, null);

		final JLabel srcLabel = GUIFactory.createLabel("Source: ",
				GUIFactory.FONTS);

		final JLabel srcTxt = GUIFactory.createLabel(source, GUIFactory.FONTS);

		final JLabel colLabel = GUIFactory.createLabel("Columns: ",
				GUIFactory.FONTS);

		final JLabel cols = GUIFactory.createLabel("" + colNum,
				GUIFactory.FONTS);

		final JLabel rowLabel = GUIFactory.createLabel("Rows: ",
				GUIFactory.FONTS);

		final JLabel rows = GUIFactory.createLabel("" + rowNum,
				GUIFactory.FONTS);

		final JLabel sizeLabel = GUIFactory.createLabel(
				"Matrix Size (includes " + "N/A-values): ", GUIFactory.FONTS);

		final JLabel size = GUIFactory.createLabel("" + (rowNum * colNum),
				GUIFactory.FONTS);

		contentPanel.setBorder(BorderFactory
				.createTitledBorder("Data Statistics"));

		contentPanel.add(srcLabel, "tag label");
		contentPanel.add(srcTxt, "wrap");
		contentPanel.add(rowLabel, "tag label");
		contentPanel.add(rows, "wrap");
		contentPanel.add(colLabel, "tag label");
		contentPanel.add(cols, "wrap");
		contentPanel.add(sizeLabel, "tag label");
		contentPanel.add(size);

		mainPanel.add(contentPanel, "push, grow, wrap");
		mainPanel.add(closeBtn, "span, al right");
	}
}
