/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.awt.Image;
import java.awt.image.PixelGrabber;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This is a completely generic and self sufficient static class that writes
 * images to PPM format files.
 */
public class PpmWriter {

	public static void writePpm(final Image image, final OutputStream out)
			throws java.io.IOException {
		final int pixels[][] = getPixels(image);
		final int width = pixels.length;
		final int height = pixels[0].length;
		// # Each PPM image consists of the following:
		// # 1) A "magic number" for identifying the file type. A ppm image's
		// magic number is the two characters "P6".
		out.write(("P6").getBytes());
		// # 2) Whitespace (blanks, TABs, CRs, LFs).
		out.write(("\n").getBytes());
		// # 3) A width, formatted as ASCII characters in decimal.
		out.write(("" + width).getBytes());
		// # 4) Whitespace.
		out.write(("\n").getBytes());
		// # 5) A height, again in ASCII decimal.
		out.write(("" + height).getBytes());
		// # 6) Whitespace.
		out.write(("\n").getBytes());
		// # 7) The maximum color value (Maxval), again in ASCII decimal. Must
		// be less than 65536.
		out.write(("255").getBytes());
		// # 8) Newline or other single whitespace character.
		out.write(("\n").getBytes());
		// # 9) A raster of Height rows, in order from top to bottom. Each row
		// consists of Width pixels, in order from left # to right. Each pixel
		// is a triplet of red, green, and blue samples, in that order. Each
		// sample is represented in pure binary by either 1 or 2 bytes. If the
		// Maxval is less than 256, it is 1 byte. Otherwise, it is 2 bytes. The
		// most significant byte is first.
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				printColor(out, pixels[col][row]);
			}
		}
	}

	public static void printColor(final OutputStream out, final int val)
			throws java.io.IOException {
		final int blue = val;
		final int green = val >> 8;
		final int red = val >> 16;

		out.write(red);
		out.write(green);
		out.write(blue);
	}

	/**
	 * Snag the pixels from an image.
	 *
	 * Copied verbatim from com.gurge.amd.TestQuantize by <a
	 * href="http://www.gurge.com/amd/">Adam Doppelt</a>
	 */
	public static int[][] getPixels(final Image image) throws IOException {
		final int w = image.getWidth(null);
		final int h = image.getHeight(null);
		final int pix[] = new int[w * h];
		final PixelGrabber grabber = new PixelGrabber(image, 0, 0, w, h, pix,
				0, w);

		try {
			if (grabber.grabPixels() != true)
				throw new IOException("Grabber returned false: "
						+ grabber.status());
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		final int pixels[][] = new int[w][h];
		for (int x = w; x-- > 0;) {
			for (int y = h; y-- > 0;) {
				pixels[x][y] = pix[y * w + x];
			}
		}

		return pixels;
	}
}
