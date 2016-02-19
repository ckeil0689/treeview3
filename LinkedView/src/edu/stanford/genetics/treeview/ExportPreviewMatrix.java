package edu.stanford.genetics.treeview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

public class ExportPreviewMatrix extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final Image paintImage;
	public static final int SIDE_LEN = 400;
	
	public ExportPreviewMatrix(final Image matrix) {
		
		this.paintImage = matrix;
		this.setLayout(new MigLayout());
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
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, SIDE_LEN, SIDE_LEN);

		if(paintImage != null) {
			g2d.drawImage(paintImage, 0, 0, SIDE_LEN, SIDE_LEN, this);
			
		} else {
			BufferedImage img = new BufferedImage(SIDE_LEN, SIDE_LEN, 
					BufferedImage.TYPE_BYTE_GRAY);
			g2d.drawImage(img, 0, 0, null);
		}
	}
}
