/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
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


import edu.stanford.genetics.treeview.plugin.dendroview.LabelView;
import net.miginfocom.swing.MigLayout;
import Utilities.GUIFactory;

/**
 * enables editing of a headerSummary object.
 */
public class HeaderSummaryPanel extends JPanel implements SettingsPanel,
		Observer {

	private static final long serialVersionUID = 1L;

	private LabelInfo headerInfo;
	private LabelSummary headerSummary;
	private final JList<String> headerList = new JList<String>(new String[0]);

	//Hook to be able to reset the label view scrollbar
	private final LabelView labelView;

	public HeaderSummaryPanel(final LabelInfo headerInfo,
		LabelView labelView) {

		this.headerInfo = headerInfo;
		this.headerSummary = labelView.getLabelSummary();
		this.labelView = labelView;

		setLayout(new MigLayout());
		setOpaque(false);

		final JLabel label = GUIFactory.createLabel("Select headers to "
				+ "display:", GUIFactory.FONTS);
		add(label, "span, wrap");

		setHeaderList(headerInfo.getPrefixes());
		headerList.setVisibleRowCount(5);
		headerList.setFont(GUIFactory.FONTS);
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
	public void setHeaderInfo(final LabelInfo headerInfo) {

		if (this.headerInfo != null) {
			this.headerInfo.deleteObserver(this);
		}
		this.headerInfo = headerInfo;
		headerInfo.addObserver(this);
		synchronizeFrom();
	}

	/** Getter for headerInfo */
	public LabelInfo getHeaderInfo() {

		return headerInfo;
	}

	/** Setter for headerSummary */
	public void setHeaderSummary(final LabelSummary headerSummary) {

		this.headerSummary = headerSummary;
		synchronizeFrom();
	}

	/** Getter for headerSummary */
	public LabelSummary getHeaderSummary() {

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
	public JList<String> getHeaderList() {

		return headerList;
	}

	/**
	 * Returns the smallest index from a selected range in the header list.
	 *
	 * @return The index of the selected list item.
	 */
	public int getSmallestSelectedIndex() {

		return getHeaderList().getSelectedIndex();
	}

	@Override
	public void synchronizeFrom() {

		final int[] included = getHeaderSummary().getIncluded();
		final JList<String> list = getHeaderList();

		if (list == null)
			return;

		list.clearSelection();
		for (final int index : included) {
			if ((index >= 0) && (index < list.getModel().getSize())) {
				list.addSelectionInterval(index, index);
			}
		}
	}

	@Override
	public void synchronizeTo() {

		getHeaderSummary().setIncluded(getHeaderList().getSelectedIndices());
		labelView.resetSecondaryScroll();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(final Observable o, final Object arg) {

		if (o == headerInfo) {
			setHeaderList(headerInfo.getPrefixes());
			synchronizeFrom();
			repaint();
		} else {
			LogBuffer.println("HeaderSummaryPanel got update from unexpected "
					+ "observable " + o);
		}
	}
}