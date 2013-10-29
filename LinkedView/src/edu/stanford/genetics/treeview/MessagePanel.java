/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: MessagePanel.java,v $
 * $Revision: 1.6 $
 * $Date: 2004-12-21 03:28:13 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular, 
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;


import java.awt.*;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;

public class MessagePanel extends JPanel{

	private static final long serialVersionUID = 1L;
	
	protected MessageCanvas messagecanvas;
    protected Vector<String> messages;

    private final Color bgColor;
    private final Color textColor = new Color(90, 90, 90, 255);
    private final Color labelColor = new Color(178, 129, 0, 255);
    
    private String title;
    
    /**
     * Chained constructor for empty class declaration 
     */
    public MessagePanel() {
		
		this(null, Color.white);
	}
    
    /**
     * Main constructor
     * @param t
     * @param bgColor
     */
	public MessagePanel(String t, Color bgColor) {
		
		super();
		this.setLayout(new MigLayout("ins 0"));
		this.setOpaque(false);
		this.bgColor = bgColor;
		title = t;
		
		JLabel header = new JLabel(t);
		header.setFont(new Font("Sans Serif", Font.BOLD, 16));
		header.setForeground(labelColor);
		
		messages = new Vector<String>(5,5);
		messagecanvas = new MessageCanvas();
		messagecanvas.setFont(new Font("Sans Serif", Font.PLAIN, 16));
		
		this.add(header, "pushx, span, wrap");
		this.add(messagecanvas, "pushx, growx");
	}
	
	/**
	 * MessageCanvas subclass
	 */
	public class MessageCanvas extends JPanel {

		private static final long serialVersionUID = 1L;

		public MessageCanvas() {
			
			super();
		}
		
		@Override
		public void paintComponent(Graphics g) {
			
			int xoff = 0;
			FontMetrics metrics = getFontMetrics(g.getFont());
			int ascent = metrics.getAscent();
			int height = 0;
			Enumeration<String> e = messages.elements();
			
			Dimension size = getSize();
			g.clearRect(0, 0, size.width, size.height);
			
			g.setColor(bgColor);
			g.fillRect(0, 0, size.width, size.height);
			
			g.setColor(textColor);

			while (e.hasMoreElements()) {
				String message = (String) e.nextElement();
				if (message == null) {
					continue;
				}
				height += ascent;
				g.drawString(message, -xoff, height);
			}
		}
		
		@Override
		public Dimension getPreferredSize() {
			
			FontMetrics metrics = getFontMetrics(getFont());
			int ascent = metrics.getAscent();
			
			// for title...
			int height = ascent;
			int width = metrics.stringWidth(title);
			Enumeration<String> e = messages.elements();
			while (e.hasMoreElements()) {
				String message = (String) e.nextElement();
				if (message == null) {
					continue;
				}
				height += ascent;
				int length = metrics.stringWidth(message);
				if (width < length) {
					width = length;
				}
			}
			return new Dimension(width, height);
		}
	}	
    
	public void setMessages(String [] m) {
		
		resetMessages();
		int i;
		for (i = 0; i < m.length; i++) {
			addMessage(m[i]);
		}
		layoutMessages();
	}

	public void resetMessages() {
		
		messages.removeAllElements();
	}
	
	public void addMessage(String message) {
		
		messages.addElement(message);
	}
	
	public void layoutMessages() {
		
		revalidate();
		repaint();
	}
}
