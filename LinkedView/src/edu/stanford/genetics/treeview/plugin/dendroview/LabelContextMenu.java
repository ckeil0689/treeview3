package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class LabelContextMenu extends JPopupMenu {

	/**
	 * Default serial version ID to keep Eclipse happy...
	 */
	private static final long serialVersionUID = 1L;
	
	private final JMenuItem labelStyles;
	
	public LabelContextMenu() {
		
		super();
		labelStyles = new JMenuItem("Label settings...");
		add(labelStyles);
	}
	
	protected void addStyleListener(ActionListener l) {
		
		labelStyles.addActionListener(l);
	}

}
