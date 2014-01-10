/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: BitmapKaryoViewExportPanel.java,v $
 * $Revision: 1.1 $
 * $Date: 2006-08-16 19:13:49 $
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
package edu.stanford.genetics.treeview.plugin.karyoview;

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
import edu.stanford.genetics.treeview.LogBuffer;

class BitmapKaryoViewExportPanel extends KaryoViewExportPanel {
	JComboBox formatPulldown = new JComboBox(BitmapWriter.formats);

	BitmapKaryoViewExportPanel(final KaryoView scatterView) {
		super(scatterView);
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

	public void save() {
		try {
			final OutputStream output = new BufferedOutputStream(
					new FileOutputStream(getFile()));
			final BufferedImage i = generateImage();
			final String format = (String) formatPulldown.getSelectedItem();
			@SuppressWarnings("unused")
			// ignore success, could keep window open on failure if save could
			// indicate success.
			final boolean success = BitmapWriter.writeBitmap(i, format, output,
					this);

			output.close();
		} catch (final Exception e) {
			JOptionPane.showMessageDialog(this, new JTextArea(
					"Karyoscope image export had problem " + e));
			LogBuffer.println("Exception " + e);
			e.printStackTrace();
		}

	}

}
