/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.gurge.amd.GIFEncoder;
import com.gurge.amd.Quantize;
import com.gurge.amd.TestQuantize;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.SettingsPanel;
import edu.stanford.genetics.treeview.TreeSelectionI;

/**
 * Subclass of ExportPanel which outputs a postscript version of a DendroView.
 *
 */
public class GifExportPanel extends ExportPanel implements SettingsPanel {

	// I wish I could just inherit this...
	public GifExportPanel(final HeaderInfo arrayHeaderInfo,
			final HeaderInfo geneHeaderInfo,
			final TreeSelectionI geneSelection,
			final TreeSelectionI arraySelection,
			final TreePainter arrayTreeDrawer,
			final TreePainter geneTreeDrawer, final ArrayDrawer arrayDrawer,
			final MapContainer arrayMap, final MapContainer geneMap) {
		super(arrayHeaderInfo, geneHeaderInfo, geneSelection, arraySelection,
				arrayTreeDrawer, geneTreeDrawer, arrayDrawer, arrayMap,
				geneMap, false);
	}

	@Override
	public void synchronizeTo() {
		save();
	}

	@Override
	public void synchronizeFrom() {
		// do nothing...
	}

	@Override
	public void save() {
		try {
			final OutputStream output = new BufferedOutputStream(
					new FileOutputStream(getFile()));

			final DendroGifWriter gw = new DendroGifWriter();
			gw.write(output);

			output.close();
		} catch (final Exception e) {
			LogBuffer.println("GIF Export Panel caught exception " + e);
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
		return (".gif");
	}

	/**
	 * Inner class which outputs a gif version of Dendroview like things
	 *
	 * It is "loosely coupled" in that it only calls protected methods in the
	 * ExportPanel superclass.
	 */

	class DendroGifWriter {
		int extraWidth = getBorderPixels();
		int extraHeight = getBorderPixels();

		/**
		 * write a gif image corresponding to the export panel preview to the
		 * OutputStream output.
		 */
		public void write(final OutputStream output) {

			final Rectangle destRect = new Rectangle(0, 0, estimateWidth(),
					estimateHeight());
			final Image i = createImage(destRect.width + extraWidth,
					destRect.height + extraHeight);
			final Graphics g = i.getGraphics();
			g.setColor(Color.white);
			g.fillRect(0, 0, destRect.width + 1 + extraWidth, destRect.height
					+ 1 + extraHeight);
			g.setColor(Color.black);
			g.translate(extraHeight / 2, extraWidth / 2);
			drawAll(g, 1.0);
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
