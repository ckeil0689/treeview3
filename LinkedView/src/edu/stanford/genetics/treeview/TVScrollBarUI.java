package edu.stanford.genetics.treeview;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

import Utilities.GUIFactory;

/**
 * Custom ScrollBar UI to be used with any JScrollBars. GLobal implementation
 * is achieved by setting the JScrollBar UI to this class in the UI manager in
 * the launcher class.
 * @author CKeil
 *
 */
public class TVScrollBarUI extends BasicScrollBarUI {

	public static ComponentUI createUI(JComponent c) {
		
		return new TVScrollBarUI();
	}
	
    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        
    	g.setColor(GUIFactory.SCROLL_BG);
    	g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, 
    			trackBounds.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
    	
    	Graphics2D g2d = (Graphics2D)g;
    	g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
    	g2d.setColor(GUIFactory.MAIN);
    	g2d.fillRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, 
    			thumbBounds.height);
    }
}
