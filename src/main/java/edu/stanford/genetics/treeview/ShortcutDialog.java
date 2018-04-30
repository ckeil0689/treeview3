package edu.stanford.genetics.treeview;

import java.awt.Dimension;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import Utilities.CustomDialog;
import Utilities.GUIFactory;

public class ShortcutDialog extends CustomDialog {

	private static final long serialVersionUID = 1L;

	private static final String mod_key =
		(System.getProperty("os.name").toLowerCase().startsWith("mac os x") ?
			"CMD" : "CTRL");
	private static final String[] col_names = {"Shortcut / Modifier-Action",
		"Function"};
	private boolean tableSetupDone = false;

	public enum RegionTable {
		ANY("Keyboard Shortcuts",new JTable(new String[][] {
			{mod_key + "-D",    "deselect all"},
			{"ESC",             "dismiss context menu"},
			{mod_key + "-T",    "toggle trees"},
			{mod_key + "-F",    "search"},
			{mod_key + "-O",    "open file"},
			{mod_key + "-I",    "import file"},
			{mod_key + "-C",    "cluster"},
			{mod_key + "-E",    "export"},
			{mod_key + "-W",    "close window"},
			{"ARROW",           "small scroll"},
			{"SHIFT-ARROW",     "large scroll"},
			{"HOME",            "scroll to top"},
			{"END",             "scroll to bottom"},
			{mod_key + "-HOME", "scroll to left end"},
			{mod_key + "-END",  "scroll to right end"},
			{"PAGE",            "stepwise vertical scroll"},
			{mod_key + "-PAGE", "stepwise horizontal scroll"},
			{"PLUS/EQUAL",      "zoom in"},
			{"MINUS/DASH",      "zoom out"}},
			col_names)),
		MAT("Matrix Mouse Modifiers",new JTable(new String[][] {
			{"SCROLL",                           "vertical scroll"},
			{"SHIFT-SCROLL",                     "horizontal scroll"},
			{"OPT-SCROLL",                       "medium zoom"},
			{"CLICK",                            "select a tile"},
			{"SHIFT-CLICK",                      "select row"},
			{mod_key + "-CLICK",                 "select column"},
			{"CLICK-DRAG",                       "select tiles"},
			{"SHIFT-CLICK-DRAG",                 "select rows"},
			{"CTRL-CLICK-DRAG",                  "select columns"},
			{"DOUBLE-CLICK",                     "medium zoom in"},
			{"CTRL-DOUBLE-CLICK",                "small zoom in"},
			{"SHIFT-DOUBLE-CLICK",               "large zoom in"},
			{mod_key + "-DOUBLE-CLICK",          "step-wise zoom in"},
			{mod_key + "-SHIFT-DOUBLE-CLICK",    "step-wise zoom in (fast)"},
			{"OPT-DOUBLE-CLICK",                 "medium zoom out"},
			{"OPT-CTRL-DOUBLE-CLICK",            "small zoom out"},
			{"OPT-SHIFT-DOUBLE-CLICK",           "large zoom out"},
			{"OPT-" + mod_key + "-DOUBLE-CLICK", "step-wise zoom out"},
			{"OPT-" + mod_key +
				"-SHIFT-DOUBLE-CLICK",           "step-wise zoom out (fast)"},
			{"HOVER",                            "no highlight"},
			{"SHIFT-HOVER",                      "highlight row"},
			{"CTRL-HOVER",                       "highlight column"}},
			col_names)),
		LAB("Label Panes Mouse Modifiers",new JTable(new String[][] {
			{"SCROLL",                    "scroll matrix or labels"},
			{"SHIFT-SCROLL",              "scroll matrix or labels"},
			{"CLICK",                     "select 1 column or row"},
			{"SHIFT-CLICK",               "select (from nearest)"},
			{mod_key + "-CLICK",          "toggle selection"},
			{"OPT-CLICK",                 "shrink selection (from nearest)"},
			{"OPT-" + mod_key + "-CLICK", "deselect all"},
			{"CTRL-CLICK",                "contextual menu"},
			{"CLICK-DRAG",                "select"},
			{"SHIFT-CLICK-DRAG",          "add to selection"},
			{mod_key + "-CLICK-DRAG",     "toggle selection"},
			{"OPT-CLICK-DRAG",            "deselect"},
			{"HOVER",                     "label tracking or highlight"}},
			col_names)),
		TREE("Tree Panes Mouse Modifiers",new JTable(new String[][] {
			{"SCROLL",       "medium matrix scroll"},
			{"SHIFT-SCROLL", "medium matrix scroll"},
			{"CLICK",        "select subtree"},
			{"HOVER",        "highlight"}},
			col_names)),
		ZOOM("Zoom Button Modifiers",new JTable(new String[][] {
			{"CLICK",            "medium zoom"},
			{"OPT-CLICK",        "small zoom"},
			{"SHIFT-CLICK",      "large zoom"},
			{mod_key + "-CLICK", "full zoom"}},
			col_names)),
		NAV("Selection & Home Button Modifiers",new JTable(new String[][] {
			{"CLICK",            "zoom to selection"},
			{mod_key + "-CLICK", "zoom to selection (with animation)"},
			{"SHIFT-CLICK",      "zoom to selection (with animation)"},
			{"OPT-SHIFT-CLICK",  "partial zoom to small selection"},
			{"OPT-" + mod_key +
				"-CLICK",        "partial zoom to small selection"}},
			col_names));

		private final String toString;
		private final JTable toTable;

		private RegionTable(String toString,JTable toTable) {
			this.toString = toString;
			this.toTable = toTable;
		}

		@Override
		public String toString() {
			return toString;
		}

		public JTable get() {
			return toTable;
		}
	}

	public ShortcutDialog() {

		super("Keyboard Shortcuts / Modifiers");

		setupData();
		setupLayout();
	}

	private void setupData() {
		setupTables();
	}

	private void setupTables() {
		tableSetupDone = true;
		for(RegionTable table : RegionTable.values()) {
			JTable thisTable = table.get();
			thisTable.setFocusable(false);
			thisTable.setFont(GUIFactory.FONTS);
			thisTable.setOpaque(false);
			thisTable.setGridColor(GUIFactory.DEFAULT_BG);
		}
	}

	@Override
	protected void setupLayout() {

		if(!tableSetupDone) {
			return;
		}

		//Establish the window & component sizes
		int wd = 500;
		int tht = 0;
		int rowht = getFontMetrics(getLayeredPane().getComponent(0).getFont()).
			getHeight();
		int num_head_rows = 1;
		int pad = 4;

		//All the tables (inside scrollpanes) will be put on the content pane
		JPanel contentPane = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);

		for(RegionTable table : RegionTable.values()) {

			//Create a table & disable pointless row selection ability
			JTable tableobj = table.get();
			tableobj.setRowSelectionAllowed(false);

			//Label the table for the region it has shortcuts for
			final JLabel tablelabel = GUIFactory.createLabel(
				table.toString(),GUIFactory.FONTM);
			contentPane.add(tablelabel, "pushx, span, wrap");

			//Establish this table's height & track total height
			int ht = rowht * (tableobj.getRowCount() + num_head_rows) + pad;
			tht += ht;

			//JTables can only be added to jscrollpanes
			final JScrollPane tablePane = new JScrollPane(tableobj);
			//Pass mouse wheel scroll events to parent
			tablePane.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseWheelMoved(MouseWheelEvent e) {
					tablePane.getParent().dispatchEvent(e);
				}
			});

			//Make sure that the table is big enough to not create a scrollbar,
			//assuming each row is 1 line of text
			tablePane.setPreferredSize(new Dimension(wd,ht));
			tableobj.setFillsViewportHeight(false);
			tableobj.setSize(wd,ht);

			contentPane.add(tablePane, "pushx, growy, wrap");
		}

		//Make sure there's enough room in the content pane for none of the
		//tables' scroll panes to need a scrollbar
		contentPane.setMinimumSize(new Dimension(wd,tht));
		contentPane.setVisible(true);

		//Create a master scroll bar to scroll over all the tables
		JScrollPane scrollPane = new JScrollPane(contentPane);
		//Leave some room for vertical scroll bars just in case
		scrollPane.setPreferredSize(new Dimension(wd + 60,600));
		scrollPane.setVisible(true);

		//Finally, add the main scroll pane to the main panel
		mainPanel.add(scrollPane);
		getContentPane().add(mainPanel);
	}
}
