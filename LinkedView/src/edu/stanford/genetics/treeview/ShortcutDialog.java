package edu.stanford.genetics.treeview;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import Utilities.CustomDialog;
import Utilities.GUIFactory;

public class ShortcutDialog extends CustomDialog {

	public ShortcutDialog() {
		
		super("Keyboard Shortcuts");
		
		setupData();

		dialog.pack();
		dialog.setLocationRelativeTo(JFrame.getFrames()[0]);
	}
	
	private void setupData() {
		
		String os_name = System.getProperty("os.name");
		String mod_key = (os_name.contains("Mac")) ? "CMD" : "CTRL";
		
		String[] col_names = {"Shortcut", "Function"};
		
		String[][] data = {
				{mod_key + " + O", "Open new file"},
				{mod_key + " + W", "Close window"},
				{mod_key + " + D", "Deselect all"},
				{mod_key + " + T", "Toggle dendrograms"},
				{mod_key + " + Z", "Zoom selection"},
				{mod_key + " + X", "Reset zoom"},
				{mod_key + " + C", "Open cluster menu"},
				{mod_key + " + F", "Open label search dialog"},
				{"Alt + 1", "Set matrix to 'Fill'"},
				{"Alt + 2", "Set matrix to 'Equal axes'"},
				{"Alt + 3", "Set matrix to 'Proportional'"}
				};
		
		setupLayout(col_names, data);
	}
	
	private void setupLayout(String[] col_names, String[][] data) {
		
		JTable table = new JTable(data, col_names);
		table.setFocusable(false);
		table.setFont(GUIFactory.FONTS);
		table.setOpaque(false);
		table.setGridColor(GUIFactory.DEFAULT_BG);
		
		JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);
		
		mainPanel.add(scrollPane, "push, grow");
		
		dialog.add(mainPanel);
	}

}
