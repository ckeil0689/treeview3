/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: BitmapExportPanel.java,v $
 * $Revision: 1.1 $
 * $Date: 2006-08-16 19:13:45 $
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
import edu.stanford.genetics.treeview.HeaderInfo;
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
	public BitmapExportPanel(final HeaderInfo arrayHeaderInfo,
			final HeaderInfo geneHeaderInfo,
			final TreeSelectionI geneSelection,
			final TreeSelectionI arraySelection,
			final InvertedTreeDrawer arrayTreeDrawer,
			final LeftTreeDrawer geneTreeDrawer, final ArrayDrawer arrayDrawer,
			final MapContainer arrayMap, final MapContainer geneMap) {
		this(arrayHeaderInfo, geneHeaderInfo, geneSelection, arraySelection,
				arrayTreeDrawer, geneTreeDrawer, arrayDrawer, arrayMap,
				geneMap, false);
	}

	public BitmapExportPanel(final HeaderInfo arrayHeaderInfo,
			final HeaderInfo geneHeaderInfo,
			final TreeSelectionI geneSelection,
			final TreeSelectionI arraySelection,
			final InvertedTreeDrawer arrayTreeDrawer,
			final LeftTreeDrawer geneTreeDrawer, final ArrayDrawer arrayDrawer,
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
			@SuppressWarnings("unused")
			// ignore success, could keep window open on failure if save could
			// indicate success.
			final boolean success = BitmapWriter.writeBitmap(i, format, output,
					this);

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
