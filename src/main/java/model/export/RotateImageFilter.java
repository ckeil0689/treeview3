/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package model.export;

import java.awt.*;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;

public class RotateImageFilter {
	// this could probably be generalized if necessary...
	public static Image rotate(final Component c, final Image in) {
		final int width = in.getWidth(null);
		final int height = in.getHeight(null);
		if (width < 0)
			return null;
		final int imgpixels[] = new int[width * height];
		final int npixels[] = new int[width * height];
		try {
			final PixelGrabber pg = new PixelGrabber(in, 0, 0, width, height,
					imgpixels, 0, width);
			pg.grabPixels();
		} catch (final java.lang.InterruptedException e) {
			System.out.println("Intterrupted exception caught...");
		}
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				npixels[j + (width - i - 1) * height] = imgpixels[i + j * width];
			}
		}

		return c.createImage(new MemoryImageSource(height, width, npixels, 0,
				height));
	}
}
