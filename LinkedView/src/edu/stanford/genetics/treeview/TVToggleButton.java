package edu.stanford.genetics.treeview;

import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;

import javax.swing.JToggleButton;

import Utilities.GUIFactory;

/**
 * Custom JButton with overridden paintComponent method. 
 * @author CKeil
 *
 */
public class TVToggleButton extends JToggleButton {

	private static final long serialVersionUID = 1L;
	private final int BORDERSIZE = 3;
	
	public TVToggleButton(String title) {
		
		super(title);
        super.setContentAreaFilled(false);
		super.setBorder(null);
		super.setFocusPainted(false);
		
	}
	
	@Override
	public void paintComponent(Graphics g) {
		
		Graphics2D g2 = (Graphics2D) g.create();
		
		if(getModel().isRollover()) {
			g2.setPaint(new GradientPaint(new Point(0, 0), 
					GUIFactory.BUTTON_DARK, new Point(0, getHeight()), 
					GUIFactory.MAIN));
			
		} else if (getModel().isSelected()) {
			g2.setPaint(new GradientPaint(new Point(0, 0), 
					GUIFactory.BUTTON_DARKEST, new Point(0, getHeight()), 
					GUIFactory.BUTTON_DARKEST));
			
		} else { 
			g2.setPaint(new GradientPaint(new Point(0, 0), GUIFactory.MAIN, 
					new Point(0, getHeight()), GUIFactory.BUTTON_DARK));
		}
		g2.fillRect(0, 0, getWidth(), getHeight());
		g2.fillRect(0, 0, getWidth(), getHeight());

		super.paintComponent(g2);
		
		g2.dispose();
	}
	
	@Override
    public void paintBorder(Graphics g) {
        
		Graphics2D g2 = (Graphics2D) g.create();
		
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
        		RenderingHints.VALUE_ANTIALIAS_ON);
		
		if(getModel().isPressed()) {
	        g2.setColor(GUIFactory.BUTTON_BORDER_PRESS);
	       
		} else if(getModel().isRollover()) {
			g2.setColor(GUIFactory.BUTTON_BORDER_ROLL);
			
		} else {
			g2.setColor(GUIFactory.MAIN);
		}
		
	    g2.setStroke(new BasicStroke(BORDERSIZE, BasicStroke.CAP_ROUND, 
	    		BasicStroke.JOIN_ROUND));
		
		g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
    }
}
