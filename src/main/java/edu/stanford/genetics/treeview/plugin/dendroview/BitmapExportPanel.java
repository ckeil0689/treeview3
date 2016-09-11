/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import edu.stanford.genetics.treeview.BitmapWriter;
import edu.stanford.genetics.treeview.LabelInfo;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.SettingsPanel;
import edu.stanford.genetics.treeview.TreeSelectionI;

/**
 * Subclass of ExportPanel which outputs a bitmap version of a DendroView
 * Supports JPEG, PNG and PPM
 */
public class BitmapExportPanel extends ExportPanel implements SettingsPanel {
	JComboBox formatPulldown = new JComboBox(BitmapWriter.formats);

	/**
	 * Default is no char data.
	 */
	public BitmapExportPanel(final LabelInfo arrayHeaderInfo,
			final LabelInfo geneHeaderInfo,
			final TreeSelectionI geneSelection,
			final TreeSelectionI arraySelection,
			final TreePainter arrayTreeDrawer,
			final TreePainter geneTreeDrawer, final ArrayDrawer arrayDrawer,
			final MapContainer arrayMap, final MapContainer geneMap) {
		this(arrayHeaderInfo, geneHeaderInfo, geneSelection, arraySelection,
				arrayTreeDrawer, geneTreeDrawer, arrayDrawer, arrayMap,
				geneMap, false);
	}

	public BitmapExportPanel(final LabelInfo arrayHeaderInfo,
			final LabelInfo geneHeaderInfo,
			final TreeSelectionI geneSelection,
			final TreeSelectionI arraySelection,
			final TreePainter arrayTreeDrawer,
			final TreePainter geneTreeDrawer, final ArrayDrawer arrayDrawer,
			final MapContainer arrayMap, final MapContainer geneMap,
			final boolean hasChar) {
		super(arrayHeaderInfo, geneHeaderInfo, geneSelection, arraySelection,
				arrayTreeDrawer, geneTreeDrawer, arrayDrawer, arrayMap,
				geneMap, hasChar);
		final JPanel holder = new JPanel();
		final JCheckBox appendExt = new JCheckBox("Append Extension?", true);
		formatPulldown.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (appendExt.isSelected()) {
					appendExtension();
				}
			}
		});
		holder.add(new JLabel("Image Format:"));
		holder.add(formatPulldown);
		holder.add(appendExt);
		add(holder);
	}

	private void appendExtension() {
		final String fileName = getFilePath();
		final int extIndex = fileName.lastIndexOf('.');
		final int dirIndex = fileName.lastIndexOf(File.separatorChar);
		if (extIndex > dirIndex) {
			setFilePath(fileName.substring(0, extIndex) + "."
					+ formatPulldown.getSelectedItem());
		} else {
			setFilePath(fileName + "." + formatPulldown.getSelectedItem());
		}
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

			final int extraWidth = getBorderPixels();
			final int extraHeight = getBorderPixels();
			final Rectangle destRect = new Rectangle(0, 0, estimateWidth(),
					estimateHeight());

			final BufferedImage i = new BufferedImage(destRect.width
					+ extraWidth, destRect.height + extraHeight,
					BufferedImage.TYPE_INT_ARGB);
			final Graphics g = i.getGraphics();
			g.setColor(Color.white);
			g.fillRect(0, 0, destRect.width + 1 + extraWidth, destRect.height
					+ 1 + extraHeight);
			g.setColor(Color.black);
			g.translate(extraHeight / 2, extraWidth / 2);
			drawAll(g, 1.0);

			final String format = (String) formatPulldown.getSelectedItem();
			BitmapWriter.writeBitmap(i, format, output, this);

			output.close();
		} catch (final Exception e) {
			JOptionPane.showMessageDialog(this, new JTextArea(
					"Dendrogram export had problem " + e));
			LogBuffer.println("Exception " + e);
			e.printStackTrace();
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
		return (".png");
	}

	/**
	 * Inner class which outputs a png version of Dendroview like things
	 *
	 * It is "loosely coupled" in that it only calls protected methods in the
	 * ExportPanel superclass.
	 */

	class DendroPngWriter {

		/**
		 * write a png image corresponding to the export panel preview to the
		 * OutputStream output.
		 */
		public void write(final OutputStream output) {

		}
	}
}
