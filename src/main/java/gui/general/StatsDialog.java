package gui.general;

import gui.CustomDialog;
import gui.GUIFactory;

import javax.swing.*;

public class StatsDialog extends CustomDialog {

	/**
	 * Default serial version ID to keep Eclipse happy...
	 */
	private static final long serialVersionUID = 1L;

	private final String source;
	private final int rowNum;
	private final int colNum;
	
	/**
	 * Constructor
	 *
	 * @param viewFrame
	 */
	public StatsDialog(final String source, final int rowNum, 
			final int colNum) {

		super("Stats");
		
		this.source = source;
		this.rowNum = rowNum;
		this.colNum = colNum;
		
		setupLayout();
	}

	/**
	 * Sets up layout and content of this window.
	 */
	@Override
	protected void setupLayout() {

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
		
		closeBtn.requestFocusInWindow();
	}
}
