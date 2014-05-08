///* BEGIN_HEADER                                              Java TreeView
// *
// * $Author: alokito $
// * $RCSfile: MapSelector.java,v $
// * $Revision: 1.2 $
// * $Date: 2008-03-09 21:06:34 $
// * $Name:  $
// *
// * This file is part of Java TreeView
// * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
// *
// * This software is provided under the GNU GPL Version 2. In particular, 
// *
// * 1) If you modify a source file, make a comment in it containing your name and the date.
// * 2) If you distribute a modified version, you must do it under the GPL 2.
// * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
// *
// * A full copy of the license can be found in gpl.txt or online at
// * http://www.gnu.org/licenses/gpl.txt
// *
// * END_HEADER 
// */
//package edu.stanford.genetics.treeview.plugin.dendroview;
//
//import java.awt.Component;
//import java.awt.Dialog;
//import java.awt.Frame;
//import java.awt.GridBagConstraints;
//import java.awt.GridBagLayout;
//import java.awt.Panel;
//import java.awt.event.ActionListener;
//import java.awt.event.WindowAdapter;
//import java.awt.event.WindowEvent;
//
//abstract class MapSelector extends Panel implements ActionListener {
//
//	private Dialog d;
//
//	public MapSelector(final MapContainer mapContainer) {
//		setupWidgets();
//	}
//
//	abstract protected void setupWidgets();
//
//	abstract protected String getTitle();
//
//	public void showDialog(final Frame f) {
//		d = new Dialog(f, getTitle());
//		d.add(this);
//		d.addWindowListener(new WindowAdapter() {
//			@Override
//			public void windowClosing(final WindowEvent we) {
//				d.dispose();
//			}
//		});
//		d.pack();
//		d.setVisible(true);
//	}
//
//	GridBagConstraints place(final GridBagLayout gbl, final Component comp,
//			final int x, final int y, final int width, final int anchor) {
//
//		final GridBagConstraints gbc = new GridBagConstraints();
//		gbc.gridx = x;
//		gbc.gridy = y;
//		gbc.gridwidth = width;
//		gbc.anchor = anchor;
//		gbc.fill = GridBagConstraints.BOTH;
//		gbl.setConstraints(comp, gbc);
//		return gbc;
//	}
// }
