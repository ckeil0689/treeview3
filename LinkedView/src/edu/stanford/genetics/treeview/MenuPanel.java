package edu.stanford.genetics.treeview;

import java.awt.Cursor;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

public class MenuPanel {
	
	private JLabel label;
	private JPanel menuPanel;
	private boolean selected = false;
	
	public MenuPanel(String title) {
		
		menuPanel = new JPanel();
		menuPanel.setLayout(new MigLayout());
		menuPanel.setBackground(GUIParams.BG_COLOR);
		
		label = new JLabel(title);
		label.setFont(GUIParams.FONTS);
		label.setForeground(GUIParams.TEXT);
		
		menuPanel.add(label, "push");
	}
	
	public JPanel getMenuPanel() {
		
		return menuPanel;
	}
	
	public void setSelected(boolean selected) {
		
		this.selected = selected;
		
		if(selected) {
			label.setForeground(GUIParams.MAIN);
			
		} else {
			label.setForeground(GUIParams.TEXT);
		}
		
		menuPanel.repaint();
	}
	
	public String getLabelText() {
		
		return label.getText();
	}
	
	public void setHover(boolean hover) {
		
		if(hover) {
			label.setForeground(GUIParams.MAIN);
			menuPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
			
		} else {
			if(!selected) {
				label.setForeground(GUIParams.TEXT);
			}
			menuPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}
}
