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
		
		String os = System.getProperty("os.name").toLowerCase();
		boolean isMac = os.startsWith("mac os x"); 
		
		String mod_key = (isMac) ? "CMD" : "CTRL";
		String opt_key = (isMac) ? "Option" : "Alt";
		
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
				{"HOME", "Scroll matrix y-axis to start"},
				{"END", "Scroll matrix y-axis to end"},
				{"PAGE UP", "Scroll matrix y-axis up"},
				{"PAGE DOWN", "Scroll matrix y-axis down"},
				{mod_key + " + HOME", "Scroll matrix x-axis to start"},
				{mod_key + " + END", "Scroll matrix x-axis to end"},
				{mod_key + " + PAGE UP", "Scroll matrix x-axis left"},
				{mod_key + " + PAGE DOWN", "Scroll matrix x-axis right"},
				{opt_key + " + 1", "Set matrix to 'Fill'"},
				{opt_key + " + 2", "Set matrix to 'Equal axes'"},
				{opt_key + " + 3", "Set matrix to 'Proportional'"}
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
