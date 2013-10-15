/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: BitmapColorBarExportPanel.java,v $
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.swing.*;

import edu.stanford.genetics.treeview.*;

/**
*  Subclass of ColorBarExportPanel which outputs a bitmap version of color bar scale
*
*/
public class BitmapColorBarExportPanel extends ColorBarExportPanel implements SettingsPanel {
	JComboBox formatPulldown = new JComboBox(BitmapWriter.formats);
	// I wish I could just inherit this...
	public BitmapColorBarExportPanel(ColorExtractor colorExtractor) {
		super(colorExtractor);
		JPanel holder = new JPanel();
		final JCheckBox appendExt= new JCheckBox("Append Extension?", true);
		formatPulldown.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
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
		String fileName = getFilePath();
		int extIndex = fileName.lastIndexOf('.');
		int dirIndex = fileName.lastIndexOf(File.separatorChar);
		if  (extIndex > dirIndex) {
			setFilePath(fileName.substring(0, extIndex) + "." + formatPulldown.getSelectedItem());
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
			OutputStream output = new BufferedOutputStream
			(new FileOutputStream(getFile()));
			BufferedImage i = generateImage();
			String format = (String) formatPulldown.getSelectedItem();
			@SuppressWarnings("unused") // ignore success, could keep window open on failure if save could indicate success.
			boolean success = BitmapWriter.writeBitmap(i, format, output, this);
			
			output.close();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, 
			new JTextArea("Colorbar Image export had problem " +  e ));
			LogBuffer.println("Exception " + e);
			e.printStackTrace();
		}
  }
  /**
  * indicate to superclass that this type does not have bbox
  */
  @Override
protected boolean hasBbox() { return false;}
  
	@Override
	protected String getInitialExtension() {
		return("_colorbar.png");
  }
}


