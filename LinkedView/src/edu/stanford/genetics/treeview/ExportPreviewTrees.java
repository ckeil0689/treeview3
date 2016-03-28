package edu.stanford.genetics.treeview;

import java.awt.Color;
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
	
	public static final int D_LONG = 400;
	public static final int SHORT = 80;
	
	private final Image paintImage;
	private final boolean isRows;
	
	private Color backgroundColor;
	
	private int x; 
	private int y;
	
	public ExportPreviewTrees(final BufferedImage trees, final boolean isRows) {
		
		this.paintImage = trees;
		this.isRows = isRows;
		
		setLayout(new MigLayout());
		setLongSideSize(D_LONG);	
		setPaperBackground(false);
	}
	
	/**
	 * Define the background color of the tree drawing. For paper type 
	 * export formats this will turn the background white. For non-paper type
	 * backgrounds the color will be the default of the application, making it
	 * look transparent.
	 * @param isPaper - Whether the export format type is belongs in the paper
	 * category (for example A4)
	 */
	public void setPaperBackground(boolean isPaper) {
		
		if(isPaper) {
			this.backgroundColor = Color.WHITE;
			
		} else {
			this.backgroundColor = this.getBackground();
		}
	}
	
	/**
	 * The tree panels can very in length but not in thickness. This method
	 * can be used to adapt the length to retain the same size as the matrix
	 * they belong to.
	 * @param longSide - The size of the long side of the tree preview panel.
	 */
	public void setLongSideSize(final int longSide) {
		
		if(isRows) {
			this.x = SHORT;
			this.y = longSide;
			
		} else {
			this.x = longSide;
			this.y = SHORT;
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

		g2d.setColor(backgroundColor);
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
