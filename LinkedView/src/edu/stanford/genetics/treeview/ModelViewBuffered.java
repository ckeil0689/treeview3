/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.MemoryImageSource;

import utilities.GUIFactory;

/**
 * superclass, to hold info and code common to all model views
 *
 * This adds buffer management to the modelview. Interestingly, but necessarily,
 * it has no dependancy on any models.
 */
public abstract class ModelViewBuffered extends ModelView {

	private static final long serialVersionUID = 1L;

	protected int[] offscreenPixels = null;
	protected Image offscreenBuffer = null;
	protected Graphics offscreenGraphics = null;

	protected boolean rotateOffscreen = false;

	protected ModelViewBuffered() {

		super();
	}

	/**
	 * this method does no management of instance variables.
	 */
	public Image ensureCapacity(final Image i, final Dimension req) {

		if (i == null)
			return createImage(req.width, req.height);

		int w = i.getWidth(null);
		int h = i.getHeight(null);
		if ((w < req.width) || (h < req.height)) {
			if (w < req.width) {
				w = req.width;
			}
			if (h < req.height) {
				h = req.height;
			}
			// should I try to free something?
			final Image n = createImage(w, h);
			n.getGraphics().drawImage(i, 0, 0, null);
			return n;

		} else
			return i;
	}

	/**
	 * this method sets up all the instance variables. XXX - THIS FAILS ON MAC
	 * OS X since mac os x doesn't let you call getGraphics on the Image if it's
	 * generated from a pixels array... hmm...
	 */
	protected void ensureCapacity(final Dimension req) {

		if (offscreenBuffer == null) {
			createNewBuffer(req.width, req.height);
		}

		int w = offscreenBuffer.getWidth(null);
		int h = offscreenBuffer.getHeight(null);
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

	private synchronized void createNewBuffer(final int w, final int h) {

		offscreenPixels = new int[w * h];
		final MemoryImageSource source = new MemoryImageSource(w, h,
				offscreenPixels, 0, w);
		source.setAnimated(true);
		final Image n = createImage(source);

		if (offscreenBuffer != null) {
			// should I be copying over pixels instead?
			n.getGraphics().drawImage(offscreenBuffer, 0, 0, null);
		}
		offscreenBuffer = n;
		offscreenGraphics = offscreenBuffer.getGraphics();
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

		final Rectangle clip = g.getClipBounds();
		// System.out.println("Entering " + viewName() + " to clip " + clip );

		final Dimension newsize = getSize();
		if (newsize == null)
			return;

		Dimension reqSize;
		reqSize = newsize;

		// monitor size changes
		if ((offscreenBuffer == null) || (reqSize.width != offscreenSize.width)
				|| (reqSize.height != offscreenSize.height)) {

			offscreenSize = reqSize;
			offscreenBuffer = ensureCapacity(offscreenBuffer, offscreenSize);
			offscreenGraphics = offscreenBuffer.getGraphics();

			try {
				((Graphics2D) offscreenGraphics).setRenderingHint(
						RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_OFF);

			} catch (final java.lang.NoClassDefFoundError err) {
				// ignore if Graphics2D not found...
			}
			offscreenChanged = true;

		} else {
			offscreenChanged = false;
		}

		// update offscreenBuffer if necessary
		g.setColor(GUIFactory.DEFAULT_BG);
		g.fillRect(clip.x, clip.y, clip.width, clip.height);

		if (isEnabled()) {
			if ((offscreenSize.width > 0) && (offscreenSize.height > 0)) {
				updateBuffer(offscreenGraphics);
				offscreenValid = true;
			}
		} else {
			// System.out.println(viewName() + " not enabled");
			final Graphics tg = offscreenBuffer.getGraphics();
			tg.setColor(GUIFactory.DEFAULT_BG);
			tg.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
		}

		if (g != offscreenGraphics) { // sometimes paint directly
			g.drawImage(offscreenBuffer, 0, 0, this);
		}
		paintComposite(g);
		// System.out.println("Exiting " + viewName() + " to clip " + clip );
	}

}
