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
	
	/* Initial default sizes */
	public static final int PRIMARY_SIDE_LEN_DEFAULT = 300;
	public static final int SECONDARY_SIDE_LEN_DEFAULT = 85;
	//Minimum dimension of an element (e.g. tree height)
	public static final int D_MIN = 1;
	
	private final Image paintImage;
	private final boolean isRows;
	
	private Color backgroundColor;
	
	private int xSide; 
	private int ySide;
	
	public ExportPreviewTrees(final BufferedImage trees, final boolean isRows) {
		
		this.paintImage = trees;
		this.isRows = isRows;
		
		setLayout(new MigLayout());
		setShortSide(SECONDARY_SIDE_LEN_DEFAULT);
		setLongSide(PRIMARY_SIDE_LEN_DEFAULT);
		setPaperBackground(false);
	}

	public ExportPreviewTrees(final BufferedImage trees,final boolean isRows,
		final int shortLen,final int longLen) {

		this.paintImage = trees;
		this.isRows = isRows;

		setLayout(new MigLayout());
		setShortSide(shortLen);
		setLongSide(longLen);	
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
	 * The tree panels can vary in thickness. This method can be used to 
	 * adapt the length to represent the calculated tree thickness according 
	 * to calculated matrix-tree ratios in ExportHandler.
	 * @param shortSide - The thickness of the tree preview panel.
	 */
	public void setShortSide(final int shortSide) {
		
		if(isRows) {
			this.xSide = shortSide;
			
		} else {
			this.ySide = shortSide;
		}
	}
	
	/**
	 * The tree panels can vary in length depending on the matrix. This method
	 * can be used to adapt the length to retain the same size as the matrix
	 * they belong to.
	 * @param longSide - The size of the long side of the tree preview panel.
	 */
	public void setLongSide(final int longSide) {
		
		if(isRows) {
			this.ySide = longSide;
			
		} else {
			this.xSide = longSide;
		}
	}
	
	/**
	 * @return the longest side length for the preview trees.
	 */
	public int getLongSide() {
		
		return (isRows) ? ySide : xSide;
	}
	
	/**
	 * @return the shortest side length (thickness) for the preview trees.
	 */
	public int getShortSide() {
		
		return (isRows) ? xSide : ySide;
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
		g2d.fillRect(0, 0, xSide, ySide);

		if(paintImage != null) {
			g2d.drawImage(paintImage, 0, 0, xSide, ySide, this);
			
		} else {
			BufferedImage img = new BufferedImage(xSide, ySide, 
					BufferedImage.TYPE_BYTE_GRAY);
			g2d.drawImage(img, 0, 0, xSide, ySide, this);
		}
	}
}
