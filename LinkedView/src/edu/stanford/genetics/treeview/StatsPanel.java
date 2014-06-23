package edu.stanford.genetics.treeview;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

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

		final Dimension mainDim = GUIUtils.getScreenSize();

		statsDialog.getContentPane().setSize(mainDim.width * 1 / 2,
				mainDim.height * 1 / 2);

		statsDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		statsDialog.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent we) {

				statsDialog.dispose();
			}
		});

		setupLayout();

		statsDialog.pack();
		statsDialog.setLocationRelativeTo(viewFrame.getAppFrame());
	}

	/**
	 * Sets up layout and content of this window.
	 */
	public void setupLayout() {

		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.setBackground(GUIUtils.BG_COLOR);

		final JLabel header = GUIUtils.setupHeader("Data Stats");

		if (viewFrame.getDataModel() != null) {
			final JLabel source = new JLabel("Source: "
					+ viewFrame.getDataModel().getSource());
			source.setForeground(GUIUtils.TEXT);
			source.setFont(GUIUtils.FONTS);

			final JLabel cols = new JLabel("Columns: "
					+ viewFrame.getDataModel().getArrayHeaderInfo()
							.getNumHeaders());
			cols.setForeground(GUIUtils.TEXT);
			cols.setFont(GUIUtils.FONTS);

			final int rowN = viewFrame.getDataModel().getGeneHeaderInfo()
					.getNumHeaders();
			final int colN = viewFrame.getDataModel().getArrayHeaderInfo()
					.getNumHeaders();

			final JLabel rows = new JLabel("Rows: "
					+ viewFrame.getDataModel().getGeneHeaderInfo()
							.getNumHeaders());
			rows.setForeground(GUIUtils.TEXT);
			rows.setFont(GUIUtils.FONTS);

			final JLabel size = new JLabel(
					"Matrix Size (includes N/A-values): " + (rowN * colN));
			size.setForeground(GUIUtils.TEXT);
			size.setFont(GUIUtils.FONTS);

			mainPanel.add(header, "pushx, alignx 50%, wrap");
			mainPanel.add(source, "wrap");
			mainPanel.add(rows, "wrap");
			mainPanel.add(cols, "wrap");
			mainPanel.add(size, "wrap");

		} else {
			final JLabel nLoad = new JLabel(
					"It appears, the Model was not loaded.");
			nLoad.setForeground(GUIUtils.TEXT);
			nLoad.setFont(GUIUtils.FONTS);

			mainPanel.add(nLoad, "push, alignx 50%");
		}

		statsDialog.getContentPane().add(mainPanel);
	}

	/**
	 * Sets the visibility of statsFrame.
	 * 
	 * @param visible
	 */
	public void setVisible(final boolean visible) {

		statsDialog.setVisible(true);
	}
}
