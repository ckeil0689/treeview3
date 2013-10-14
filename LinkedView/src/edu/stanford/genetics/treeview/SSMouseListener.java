package edu.stanford.genetics.treeview;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;

public class SSMouseListener implements MouseListener{
	
	private final Color BLUE1 = new Color(60, 180, 220, 255);
	private final Color RED1 = new Color(240, 80, 50, 255);
	
	private JLabel label;
	
	public SSMouseListener(JLabel label){
		
		this.label = label;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		
		// to be overridden
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		
		label.setForeground(RED1);
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		
		label.setForeground(BLUE1);
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
