/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: GifColorBarExportPanel.java,v $
 * $Revision: 1.1 $
 * $Date: 2006-08-16 19:13:46 $
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
package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Image;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.gurge.amd.GIFEncoder;
import com.gurge.amd.Quantize;
import com.gurge.amd.TestQuantize;

import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.SettingsPanel;

/**
 * Subclass of ColorBarExportPanel which outputs a gif version of color bar
 * scale
 * 
 */
public class GifColorBarExportPanel extends ColorBarExportPanel implements
		SettingsPanel {

	// I wish I could just inherit this...
	public GifColorBarExportPanel(final ColorExtractor2 colorExtractor) {

		super(colorExtractor);
	}

	@Override
	public void synchronizeTo() {
		save();
	}

	@Override
	public void synchronizeFrom() {
		// do nothing...
	}

	public void save() {

		try {
			final OutputStream output = new BufferedOutputStream(
					new FileOutputStream(getFile()));

			final ColorBarGifWriter gw = new ColorBarGifWriter();
			gw.write(output);

			output.close();
		} catch (final Exception e) {
			LogBuffer
					.println("GIF ColorBar Export Panel caught exception " + e);
		}
	}

	/**
	 * indicate to superclass that this type does not have bbox
	 */
	@Override
	protected boolean hasBbox() {
		return false;
	}

	@Override
	protected String getInitialExtension() {
		return ("_colorbar.gif");
	}

	/**
	 * Inner class which outputs a gif version of Dendroview like things
	 * 
	 * It is "loosely coupled" in that it only calls protected methods in the
	 * ExportPanel superclass.
	 */

	class ColorBarGifWriter {

		/**
		 * write a gif image corresponding to the colorbar export panel preview
		 * to the OutputStream output.
		 */
		public void write(final OutputStream output) {
			final Image i = generateImage();
			try {
				final int pixels[][] = TestQuantize.getPixels(i);
				// quant
				final int palette[] = Quantize.quantizeImage(pixels, 256);
				final GIFEncoder enc = new GIFEncoder(
						createImage(TestQuantize.makeImage(palette, pixels)));
				enc.Write(output);
			} catch (final Exception e) {
				LogBuffer
						.println("In GifExportPanel.DendroGifWriter() got exception "
								+ e);
			}
		}
	}
}
