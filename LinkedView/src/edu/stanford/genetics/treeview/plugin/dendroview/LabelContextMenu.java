package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class LabelContextMenu extends JPopupMenu {

	/**
	 * Default serial version ID to keep Eclipse happy...
	 */
	private static final long serialVersionUID = 1L;

	private final boolean isRowMenu;
	
	private final JMenuItem labelStyles;
	private final JMenu clipboardMenu;
	private final JMenuItem copyAll;

	public LabelContextMenu(final boolean isRows) {

		super();
		this.isRowMenu = isRows;
		this.labelStyles = new JMenuItem("Label settings...");
		this.clipboardMenu = new JMenu("Copy labels");
		this.copyAll = new JMenuItem("All");
		
		clipboardMenu.add(copyAll);
		this.add(labelStyles);
		this.addSeparator();
		this.add(clipboardMenu);
	}

	protected void addStyleListener(ActionListener l) {

		labelStyles.addActionListener(l);
	}
	
	protected void addCopyAllListener(ActionListener l) {

		copyAll.addActionListener(l);
	}

}
