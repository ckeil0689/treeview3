///* BEGIN_HEADER                                                   TreeView 3
// *
// * Please refer to our LICENSE file if you wish to make changes to this software
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
