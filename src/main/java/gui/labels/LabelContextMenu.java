package gui.labels;

import javax.swing.*;
import java.awt.event.ActionListener;

public class LabelContextMenu extends JPopupMenu {

	/**
	 * Default serial version ID to keep Eclipse happy...
	 */
	private static final long serialVersionUID = 1L;
	
	private final JMenuItem labelStyles;
	private final JMenu clipboardMenu;
	private final JMenuItem copyAll;
	private final JMenuItem copySelected;
	private final JMenuItem copyVisibleMatrix;

	public LabelContextMenu() {

		super();
		this.labelStyles = new JMenuItem("Label settings...");
		this.clipboardMenu = new JMenu("Copy labels");
		this.copyAll = new JMenuItem("All");
		this.copySelected = new JMenuItem("Selected");
		this.copyVisibleMatrix = new JMenuItem("Zoomed");
		
		clipboardMenu.add(copyAll);
		clipboardMenu.add(copySelected);
		clipboardMenu.add(copyVisibleMatrix);
		
		this.add(labelStyles);
		this.addSeparator();
		this.add(clipboardMenu);
	}

	public void addStyleListener(ActionListener l) {

		labelStyles.addActionListener(l);
	}
	
	public void addCopyAllListener(ActionListener l) {

		copyAll.addActionListener(l);
	}
	
	public void addCopySelectedListener(ActionListener l) {

		copySelected.addActionListener(l);
	}
	
	public void addCopyVisibleListener(ActionListener l) {

		copyVisibleMatrix.addActionListener(l);
	}

}
