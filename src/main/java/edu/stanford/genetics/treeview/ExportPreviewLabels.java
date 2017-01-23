package edu.stanford.genetics.treeview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

public class ExportPreviewLabels extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/* Initial default sizes */
	public static final int D_LONG = 400;
	public static final int D_SHORT = 80;
	
	private final Image paintImage;
	private final boolean isRows;
	
	private Color backgroundColor;
	
	private int xSide; 
	private int ySide;
	
	public ExportPreviewLabels(final BufferedImage labels, final boolean isRows) {
		
		this.paintImage = labels;
		this.isRows = isRows;
		
		setLayout(new MigLayout());
		setShortSide(D_SHORT);
		setLongSide(D_LONG);
		setPaperBackground(false);
	}

	public ExportPreviewLabels(final BufferedImage labels,final boolean isRows,
		final int shortLen,final int longLen) {
		
		this.paintImage = labels;
		this.isRows = isRows;
		
		setLayout(new MigLayout());
		setShortSide(shortLen);
		setLongSide(longLen);	
		setPaperBackground(false);
	}

	/**
	 * Define the background color of the label drawing. For paper type 
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
	 * The label panels can vary in thickness. This method can be used to 
	 * adapt the length to represent the calculated label thickness according 
	 * to calculated matrix-label ratios in ExportHandler.
	 * @param shortSide - The thickness of the label preview panel.
	 */
	public void setShortSide(final int shortSide) {
		
		if(isRows) {
			this.xSide = shortSide;
			
		} else {
			this.ySide = shortSide;
		}
	}
	
	/**
	 * The label panels can vary in length depending on the matrix. This method
	 * can be used to adapt the length to retain the same size as the matrix
	 * they belong to.
	 * @param longSide - The size of the long side of the label preview panel.
	 */
	public void setLongSide(final int longSide) {
		
		if(isRows) {
			this.ySide = longSide;
			
		} else {
			this.xSide = longSide;
		}
	}
	
	/**
	 * @return the longest side length for the preview labels.
	 */
	public int getLongSide() {
		
		return (isRows) ? ySide : xSide;
	}
	
	/**
	 * @return the shortest side length (thickness) for the preview labels.
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

			LogBuffer.println("Drawing preview image for labels at size WxH: [" + xSide + "x" + ySide + "].");
		} else {
			BufferedImage img = new BufferedImage(xSide, ySide, 
				BufferedImage.TYPE_BYTE_GRAY);
			g2d.drawImage(img, 0, 0, xSide, ySide, this);
		}
	}
}
