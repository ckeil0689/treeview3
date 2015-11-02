/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
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
	public GifColorBarExportPanel(final ColorExtractor colorExtractor) {

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
