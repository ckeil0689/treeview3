/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: ModelViewProduced.java,v $
 * $Revision: 1.8 $
 * $Date: 2010-05-02 13:33:30 $
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;

/**
 * superclass, to hold info and code common to all model views
 *
 * This adds buffer management to the modelview. Interestingly, but necessarily,
 * it has no dependency on any models.
 */
public abstract class ModelViewProduced extends ModelView {

	private static final long serialVersionUID = 1L;

	/* ARGB color integers for all pixels */
	protected int[] offscreenPixels = null;
	
	protected Image offscreenImage = null;
	protected Image paintImage = null;
	protected Graphics offscreenGraphics = null;
	protected int offscreenScanSize = 0;
	protected boolean rotateOffscreen = false;
	
	protected boolean pixelsChanged;

	protected ModelViewProduced() {

		super();
	}

	/**
	 * this method sets up all the instance variables. XXX - THIS FAILS ON MAC
	 * OS X since mac os x doesn't let you call getGraphics on the Image if it's
	 * generated from a pixels array... hmm...
	 */
	protected void ensureCapacity() {//final Dimension req) {

		LogBuffer.println("Ensuring capacity");
		Dimension req = offscreenSize;
		
		if (offscreenImage == null) {
			createNewBuffer(req.width, req.height);

		} else {
			int w = offscreenImage.getWidth(null);
			int h = offscreenImage.getHeight(null);
			if ((w < req.width) || (h < req.height)) {
				if (w < req.width) {
					w = req.width;
				}

				if (h < req.height) {
					h = req.height;
				}

				// should I try to free something?
				createNewBuffer(w, h);
			}
		}
	}

	protected synchronized void createNewBuffer(final int w, final int h) {

		// should I be copy over pixels instead?
		LogBuffer.println("Creating new image buffer: " + viewName());
		LogBuffer.println("Pixels: " + (w * h));
		
		offscreenScanSize = w;
		offscreenImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		WritableRaster raster = ((BufferedImage)offscreenImage).getRaster();
		offscreenPixels = ((DataBufferInt)raster.getDataBuffer()).getData();
		pixelsChanged = true;
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

		Graphics2D g2d = (Graphics2D) g;

		final Dimension newsize = getSize();
		if (newsize == null) {
			return;
		}

		Dimension reqSize;
		reqSize = newsize;
		// monitor size changes
		if ((offscreenImage == null) || (reqSize.width != offscreenSize.width)
				|| (reqSize.height != offscreenSize.height)) {
			offscreenSize = reqSize;
			ensureCapacity();//offscreenSize); 
			offscreenChanged = true;
			offscreenValid = false;

		} else {
			offscreenChanged = false;
		}

		// update offscreenBuffer if necessary
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
		
		if (isEnabled()) {
			if ((offscreenSize.width > 0) && (offscreenSize.height > 0)) {
				updateMatrix();
				offscreenValid = true;
			}
		}

		if(paintImage != null) {
			g2d.drawImage(paintImage, 0, 0, offscreenSize.width, 
					offscreenSize.height, this);
			
		} else {
			g2d.drawImage(offscreenImage, 0, 0, null);
		}
		
		paintComposite(g2d);
	}

	/**
	 * method to update the offscreenPixels. don't forget to call
	 * offscreenSource.newPixels(); !
	 */
	abstract protected void updatePixels();
	
	/**
	 * Separate method than updatePixels which opens a hint dialog during
	 * pixel updating. This requires some extra Swing EDT threading code.
	 */
	abstract protected void updatePixelsWithHint();
	
	/**
	 * Method to adjust some matrix parameters regarding screen fit and mapping.
	 */
	abstract protected void updateMatrix();
}
