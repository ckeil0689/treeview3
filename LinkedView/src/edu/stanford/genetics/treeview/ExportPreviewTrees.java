package edu.stanford.genetics.treeview;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

public class ExportPreviewTrees extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final Image paintImage;
	public static final int WIDTH = 400;
	public static final int HEIGHT = 80;
	
	private int x; 
	private int y;
	
	public ExportPreviewTrees(final BufferedImage trees, final boolean isRows) {
		
		this.paintImage = trees;
		this.setLayout(new MigLayout());
		
		if(isRows) {
			x = HEIGHT;
			y = WIDTH;
			
		} else {
			x = WIDTH;
			y = HEIGHT;
		}
	}
	
	/*
	 * The double buffer in Swing doesn't seem to be persistent across draws.
	 * for instance, every time another window obscures one of our windows and
	 * then moves, a repaint is triggered by most VMs.
	 * 
	 * We apparently need to maintain our own persistent offscreen buffer for
	 * speed reasons...
	 */
	@Override
	public synchronized void paintComponent(final Graphics g) {

		super.paintComponent(g);
		
		Graphics2D g2d = (Graphics2D) g;

		// update offscreenBuffer if necessary
		g2d.setColor(this.getBackground());
		g2d.fillRect(0, 0, x, y);

		if(paintImage != null) {
			g2d.drawImage(paintImage, 0, 0, x, y, this);
			
		} else {
			BufferedImage img = new BufferedImage(x, y, 
					BufferedImage.TYPE_BYTE_GRAY);
			g2d.drawImage(img, 0, 0, x, y, this);
		}
	}
}
