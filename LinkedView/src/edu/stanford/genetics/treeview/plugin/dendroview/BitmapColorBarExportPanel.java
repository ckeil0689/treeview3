/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.prefs.Preferences;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import edu.stanford.genetics.treeview.BitmapWriter;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.SettingsPanel;

/**
 * Subclass of ColorBarExportPanel which outputs a bitmap version of color bar
 * scale
 *
 */
public class BitmapColorBarExportPanel extends ColorBarExportPanel implements
		SettingsPanel {

	JComboBox formatPulldown = new JComboBox(BitmapWriter.formats);

	// I wish I could just inherit this...
	public BitmapColorBarExportPanel(final ColorExtractor colorExtractor) {
		super(colorExtractor);
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
			BitmapWriter.writeBitmap(i, format, output, this);

			output.close();
		} catch (final Exception e) {
			JOptionPane.showMessageDialog(this, new JTextArea(
					"Colorbar Image export had problem " + e));
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
		return ("_colorbar.png");
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {
		// TODO Auto-generated method stub

	}

	@Override
	public Preferences getConfigNode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void requestStoredState() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void storeState() {
		// TODO Auto-generated method stub
		
	}
}
