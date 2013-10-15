package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

public class ResizablePanel extends JPanel implements MouseListener, 
MouseMotionListener {

	private static final long serialVersionUID = 1L;
	private boolean drag = false;
	private Point dragLocation = new Point();
	
	public ResizablePanel(){
		
		setLayout(new MigLayout("ins 0"));
		setBackground(Color.white);
		setBorder(BorderFactory.createEmptyBorder());
		
	    addMouseListener(this);
	    addMouseMotionListener(this);
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		
		if(drag){
			
			if(dragLocation.getX() > (getWidth()-10) && dragLocation.getY() > (getHeight()-10)){
				
				setSize((int)(getWidth() + (e.getPoint().getX() - dragLocation.getX())), 
						(int)(getHeight() + (e.getPoint().getY() - dragLocation.getY())));
				
				dragLocation = e.getPoint();
			}
		}
	}
	
	@Override
	public void mouseMoved(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void mouseEntered(MouseEvent arg0) {
		
		
		
	}
	
	@Override
	public void mouseExited(MouseEvent arg0) {
		
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		
		drag = true;
		dragLocation = e.getPoint();	
	}
	
	@Override
	public void mouseReleased(MouseEvent arg0) {
		
		drag = false;	
	}
	
	

   

}
