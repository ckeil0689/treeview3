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
	
	public static final int D_SIDE = 350;
	
	private final Image paintImage;
	private int matrixWidth;
	private int matrixHeight;
	
	public ExportPreviewMatrix(final Image matrix) {
		
		this(matrix, D_SIDE, D_SIDE);
	}
	
	public ExportPreviewMatrix(final Image matrix, final int matrixWidth, 
			final int matrixHeight) {
		
		this.paintImage = matrix;
		this.matrixWidth = matrixWidth;
		this.matrixHeight = matrixHeight;
		
		setLayout(new MigLayout());
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
		g2d.fillRect(0, 0, matrixWidth, matrixHeight);

		if(paintImage != null) {
			g2d.drawImage(paintImage, 0, 0, matrixWidth, matrixHeight, this);
			
		} else {
			BufferedImage img = new BufferedImage(matrixWidth, matrixHeight, 
					BufferedImage.TYPE_BYTE_GRAY);
			g2d.drawImage(img, 0, 0, null);
		}
	}

	/* Methods to manipulate image dimensions */
	public void setMatrixWidth(final int newWidth) {

		this.matrixWidth = newWidth;
	}

	public void setMatrixHeight(final int newHeight) {

		this.matrixHeight = newHeight;
	}

	public void setDefaultSideLength() {

		this.matrixWidth = D_SIDE;
		this.matrixHeight = D_SIDE;
	}

	public int getMatrixWidth() {

		return(matrixWidth);
	}

	public int getMatrixHeight() {
		
		return(matrixHeight);
	}
}
