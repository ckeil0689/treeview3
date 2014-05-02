/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: HeaderSummaryPanel.java,v $
 * $Revision: 1.9 $
 * $Date: 2005-12-05 05:27:53 $
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
package edu.stanford.genetics.treeview;

import java.util.Observable;
import java.util.Observer;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

/**
 * enables editing of a headerSummary object.
 */
public class HeaderSummaryPanel extends JPanel implements SettingsPanel,
		Observer {

	private static final long serialVersionUID = 1L;

	private HeaderInfo headerInfo;
	private HeaderSummary headerSummary;
	private final JList headerList = new JList(new String[0]);

	public HeaderSummaryPanel(final HeaderInfo headerInfo,
			final HeaderSummary headerSummary, TreeViewFrame frame) {

		this.headerInfo = headerInfo;
		this.headerSummary = headerSummary; 

		setLayout(new MigLayout());
		setOpaque(false);
		
		JLabel label = new JLabel("Select headers to display:");
		label.setFont(GUIParams.FONTS);
		label.setForeground(GUIParams.TEXT);
		add(label, "span, wrap");
		
		setHeaderList(headerInfo.getNames());
		headerList.setVisibleRowCount(5);
		headerList.setFont(GUIParams.FONTS);
		add(new JScrollPane(getHeaderList()), "push, grow, wrap");
		
		final ListSelectionListener tmp = new ListSelectionListener() {

			@Override
			public void valueChanged(final ListSelectionEvent e) {

				synchronizeTo();
			}
		};
		
		getHeaderList().addListSelectionListener(tmp);
		synchronizeFrom();
	}

	/** Setter for headerInfo */
	public void setHeaderInfo(final HeaderInfo headerInfo) {

		if (this.headerInfo != null) {
			this.headerInfo.deleteObserver(this);
		}
		this.headerInfo = headerInfo;
		headerInfo.addObserver(this);
		synchronizeFrom();
	}

	/** Getter for headerInfo */
	public HeaderInfo getHeaderInfo() {

		return headerInfo;
	}

	/** Setter for headerSummary */
	public void setHeaderSummary(final HeaderSummary headerSummary) {

		this.headerSummary = headerSummary;
		synchronizeFrom();
	}

	/** Getter for headerSummary */
	public HeaderSummary getHeaderSummary() {

		return headerSummary;
	}

	/** Setter for headerList */
	public void setHeaderList(final String[] headers) {

		if (headers == null) {
			headerList.setListData(new String[0]);
		} else {
			headerList.setListData(headers);
		}
	}

	/** Getter for headerList */
	public JList getHeaderList() {

		return headerList;
	}
	
	/**
	 * Returns the smalles index from a selected range in the header list.
	 * @return
	 */
	public int getSmallestSelectedIndex() {
		
		return getHeaderList().getSelectedIndex();
	}

	@Override
	public void synchronizeFrom() {

		final int[] included = getHeaderSummary().getIncluded();
		final JList list = getHeaderList();
		if (list == null) {
			return;
		}
		list.clearSelection();
		for (int i = 0; i < included.length; i++) {
			final int index = included[i];
			if ((index >= 0) && (index < list.getModel().getSize())) {
				list.addSelectionInterval(index, index);
			}
		}
	}

	@Override
	public void synchronizeTo() {

		getHeaderSummary().setIncluded(getHeaderList().getSelectedIndices());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(final Observable o, final Object arg) {
		
		if (o == headerInfo) {
			setHeaderList(headerInfo.getNames());
			synchronizeFrom();
			repaint();
		} else {
			LogBuffer.println("HeaderSummaryPanel got update from unexpected "
					+ "observable " + o);
		}
	}
}