package edu.stanford.genetics.treeview;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

public class MenuPanel implements MouseListener {
	
	private JLabel label;
	private JPanel menuPanel;
	private PreferencesMenu frame;
	
	public MenuPanel(String title, PreferencesMenu frame) {

		this.frame = frame;
		
		menuPanel = new JPanel();
		menuPanel.setLayout(new MigLayout());
		menuPanel.setBackground(GUIParams.BG_COLOR);
		menuPanel.addMouseListener(this);
		
		label = new JLabel(title);
		label.setFont(GUIParams.FONTS);
		label.setForeground(GUIParams.TEXT);
		
		menuPanel.add(label, "push");
	}
	
	public JPanel makeMenuPanel() {
		
		return menuPanel;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		
		frame.addMenu(label.getText());
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		
		menuPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		label.setForeground(GUIParams.MAIN);
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		
		menuPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		label.setForeground(GUIParams.TEXT);
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
