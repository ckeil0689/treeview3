package edu.stanford.genetics.treeview;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This class implements MouseListener and overrides its methods
 * to produce custom clickable JPanels in the TV 3.0 GUI.
 * @author CKeil
 *
 */
public class SSMouseListener implements MouseListener{
	
	/**
	 * Instance Variables
	 */
	private final Color BLUE1 = new Color(118, 193, 228, 255);
	private final Color BLUE2 = new Color(6, 180, 250, 255);
	private final Color GRAY1 = new Color(160, 161, 162, 255);
	
	private JLabel label;
	private JPanel panel;
	
	/**
	 * Chained Constructor
	 * @param label
	 */
	public SSMouseListener(JLabel label){
		
		this(null, label);	
	}
	
	/**
	 * Main Constructor
	 * @param panel
	 * @param label
	 */
	public SSMouseListener(JPanel panel, JLabel label){
		
		this.label = label;
		this.panel = panel;
	}
	
	/**
	 * This method will be overriden at implementation.
	 * The class can be used for multiple purposes (aka different methods
	 * can be called when different JPanels are clicked).
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		
		// to be overridden
	}
	
	/**
	 * Aesthetic change to the JPanel when the user 
	 * enters the mouse. Mouse Cursor changes appearance as well.
	 */
	@Override
	public void mouseEntered(MouseEvent arg0) {
		
		label.setForeground(BLUE2);
		label.setCursor(new Cursor(Cursor.HAND_CURSOR));
		
		if(panel != null) {
			
			panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		}
	}
	
	/**
	 * Aesthetic change to the JPanel when the user 
	 * exits the mouse. Mouse Cursor changes appearance as well.
	 */
	@Override
	public void mouseExited(MouseEvent arg0) {
		
		label.setForeground(BLUE1);
		label.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		
		if(panel != null) {
			
			panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	/**
	 * Aesthetic change to the JPanel when the user 
	 * presses the mouse (left click).
	 */
	@Override
	public void mousePressed(MouseEvent arg0) {
		
		label.setForeground(GRAY1);
	}
	
	/**
	 * Aesthetic change to the JPanel when the user 
	 * releases a pressed mouse button.
	 */
	@Override
	public void mouseReleased(MouseEvent arg0) {
		
		label.setForeground(BLUE1);
	}

}
