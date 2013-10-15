package edu.stanford.genetics.treeview;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class SSMouseListener implements MouseListener{
	
	private final Color BLUE1 = new Color(60, 180, 220, 255);
	private final Color RED1 = new Color(240, 80, 50, 255);
	
	private JLabel label;
	private JPanel panel;
	
	public SSMouseListener(JPanel panel, JLabel label){
		
		this.label = label;
		this.panel = panel;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		
		// to be overridden
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		
		label.setForeground(RED1);
		panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		
		label.setForeground(BLUE1);
		panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		
		label.setForeground(Color.LIGHT_GRAY);
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		
		label.setForeground(BLUE1);
	}

}
