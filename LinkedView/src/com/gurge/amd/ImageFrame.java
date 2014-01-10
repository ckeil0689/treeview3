/*
 * @(#)ImageFrame.java    0.90 9/19/00 Adam Doppelt
 */

package com.gurge.amd;

import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.IOException;

/**
 * A frame that displays an image. Create an ImageFrame, then use one of the
 * setImage() methods to show the image.
 * 
 * @version 0.90 19 Sep 2000
 * @author <a href="http://www.gurge.com/amd/">Adam Doppelt</a>
 */
public class ImageFrame extends Frame {

	private static final long serialVersionUID = 1L;

	private int left = -1;
	private int top;
	private Image image;

	ImageFrame() {

		setLayout(null);
		setSize(100, 100);
	}

	/**
	 * Set the image from a file.
	 */
	public void setImage(final File file) throws IOException {

		// load the image
		final Image image = getToolkit().getImage(file.getAbsolutePath());

		// wait for the image to entirely load
		final MediaTracker tracker = new MediaTracker(this);
		tracker.addImage(image, 0);

		try {
			tracker.waitForID(0);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		if (tracker.statusID(0, true) != MediaTracker.COMPLETE) {
			throw new IOException("Could not load: " + file + " "
					+ tracker.statusID(0, true));
		}

		setTitle(file.getName());
		setImage(image);
	}

	/**
	 * Set the image from an AWT image object.
	 */
	public void setImage(final Image image) {

		this.image = image;
		setVisible(true);
	}

	/**
	 * Set the image from an indexed color array.
	 */
	public void setImage(final int palette[], final int pixels[][]) {

		final int w = pixels.length;
		final int h = pixels[0].length;
		final int pix[] = new int[w * h];

		// convert to RGB
		for (int x = w; x-- > 0;) {
			for (int y = h; y-- > 0;) {
				pix[y * w + x] = palette[pixels[x][y]];
			}
		}

		setImage(w, h, pix);
	}

	/**
	 * Set the image from a 2D RGB pixel array.
	 */
	public void setImage(final int pixels[][]) {

		final int w = pixels.length;
		final int h = pixels[0].length;
		final int pix[] = new int[w * h];

		// convert to RGB
		for (int x = w; x-- > 0;) {
			for (int y = h; y-- > 0;) {
				pix[y * w + x] = pixels[x][y];
			}
		}

		setImage(w, h, pix);
	}

	/**
	 * Set the image from a 1D RGB pixel array.
	 */
	public void setImage(final int w, final int h, final int pix[]) {

		setImage(createImage(new MemoryImageSource(w, h, pix, 0, w)));
	}

	/**
	 * Get the image.
	 */
	public Image getImage() {

		return image;
	}

	/**
	 * Overridden for double buffering.
	 */
	@Override
	public void update(final Graphics g) {

		paint(g);
	}

	/**
	 * Paint the image.
	 */
	@Override
	public void paint(final Graphics g) {

		// the first time through, figure out where to draw the image
		if (left == -1) {

			final Insets insets = getInsets();
			left = insets.left;
			top = insets.top;

			setSize(image.getWidth(null) + left + insets.right,
					image.getHeight(null) + top + insets.bottom);
		}
		g.drawImage(image, left, top, this);
	}

	public static void main(final String args[]) throws IOException {

		final ImageFrame f = new ImageFrame();
		f.setImage(new File(args[0]));
	}
}
