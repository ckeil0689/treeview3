package edu.stanford.genetics.treeview;

import java.awt.Frame;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import Utilities.CustomDialog;
import Utilities.GUIFactory;

public class ShortcutDialog extends CustomDialog {

	public ShortcutDialog() {

		super("Keyboard Shortcuts");

		setupData();

		dialog.add(mainPanel);
		dialog.pack();
		dialog.setLocationRelativeTo(Frame.getFrames()[0]);
	}

	private void setupData() {

		final String os = System.getProperty("os.name").toLowerCase();
		final boolean isMac = os.startsWith("mac os x");

		final String mod_key = (isMac) ? "CMD" : "CTRL";
		final String opt_key = (isMac) ? "Option" : "Alt";

		final String[] col_names = { "Shortcut", "Function" };
		
		/* General Table */
		final JLabel generalLabel = GUIFactory.createLabel("General Functions", 
				GUIFactory.FONTM);
		mainPanel.add(generalLabel, "pushx, span, wrap");
		
		final String[][] data = { { mod_key + " + O", "Open new file" },
				{ mod_key + " + W", "Close window" },
				{ mod_key + " + D", "Deselect all" },
				{ mod_key + " + T", "Toggle dendrograms" },
				{ mod_key + " + ARROW_UP", "Zoom selection" },
				{ mod_key + " + ARROW_DOWN", "Reset zoom" },
				{ mod_key + " + C", "Open cluster menu" },
				{ mod_key + " + F", "Open label search dialog" }
				/*TODO add back when feature works well */
//				{ opt_key + " + 1", "Set matrix to 'Fill'" },
//				{ opt_key + " + 2", "Set matrix to 'Equal axes'" },
//				{ opt_key + " + 3", "Set matrix to 'Proportional'" } 
				};

		setupTable(col_names, data);
		
		
		/* Scroll Table */
		final JLabel scrollLabel = GUIFactory.createLabel("Matrix Scrolling", 
				GUIFactory.FONTM);
		mainPanel.add(scrollLabel, "pushx, span, wrap");
		
		final String[][] scrollData = {
				{ mod_key + " + HOME", "X-axis to start" },
				{ mod_key + " + END", "X-axis to end" },
				{ mod_key + " + PAGE UP", "X-axis left" },
				{ mod_key + " + PAGE DOWN", "X-axis right" },
				{ "HOME", "Y-axis to start" },
				{ "END", "Y-axis to end" },
				{ "PAGE UP", "Y-axis up" },
				{ "PAGE DOWN", "Y-axis down" }
				};
		
		setupTable(col_names, scrollData);
	}

	private void setupTable(final String[] col_names, final String[][] data) {

		final JTable table = new JTable(data, col_names);
		table.setFocusable(false);
		table.setFont(GUIFactory.FONTS);
		table.setOpaque(false);
		table.setGridColor(GUIFactory.DEFAULT_BG);

		final JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);

		mainPanel.add(scrollPane, "pushx, growx, wrap");

	}
}
