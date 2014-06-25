package edu.stanford.genetics.treeview;

import java.awt.Cursor;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

public class MenuPanel {

	private final JLabel label;
	private final JPanel menuPanel;
	private boolean selected = false;

	public MenuPanel(final String title) {

		menuPanel = new JPanel();
		menuPanel.setLayout(new MigLayout());
		menuPanel.setBackground(GUIFactory.BG_COLOR);

		label = new JLabel(title);
		label.setFont(GUIFactory.FONTS);
		label.setForeground(GUIFactory.TEXT);

		menuPanel.add(label, "push");
	}

	public JPanel getMenuPanel() {

		return menuPanel;
	}

	public void setSelected(final boolean selected) {

		this.selected = selected;

		if (selected) {
			label.setForeground(GUIFactory.MAIN);

		} else {
			label.setForeground(GUIFactory.TEXT);
		}

		menuPanel.repaint();
	}

	public String getLabelText() {

		return label.getText();
	}

	public void setHover(final boolean hover) {

		if (hover) {
			label.setForeground(GUIFactory.MAIN);
			menuPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

		} else {
			if (!selected) {
				label.setForeground(GUIFactory.TEXT);
			}
			menuPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}
}
