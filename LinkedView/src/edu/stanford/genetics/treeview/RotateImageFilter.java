/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: RotateImageFilter.java,v $
 * $Revision: 1.3 $
 * $Date: 2004-12-21 03:28:14 $
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

import java.awt.Component;
import java.awt.Image;
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
