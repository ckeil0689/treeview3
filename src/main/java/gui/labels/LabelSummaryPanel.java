/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package gui.labels;

import gui.GUIFactory;
import gui.settings.SettingsPanel;
import model.data.labels.LabelInfo;
import model.data.labels.LabelSummary;
import net.miginfocom.swing.MigLayout;
import util.LogBuffer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.Observable;
import java.util.Observer;

/**
 * enables editing of a headerSummary object.
 */
public class LabelSummaryPanel extends JPanel implements SettingsPanel,
		Observer {

	private static final long serialVersionUID = 1L;

	private LabelInfo labelInfo;
	private LabelSummary labelSummary;
	private final JList<String> labelTypeList = new JList<String>(new String[0]);

	//Hook to be able to reset the label view scrollbar
	private final LabelView labelView;

	public LabelSummaryPanel(final LabelInfo labelInfo,
		LabelView labelView) {

		this.labelInfo = labelInfo;
		this.labelSummary = labelView.getLabelSummary();
		this.labelView = labelView;

		setLayout(new MigLayout());
		setOpaque(false);

//		final JLabel label = GUIFactory.createLabel("Label type(s) to " +
//			"display:", GUIFactory.FONTS);
//		add(label, "span, wrap");

		setLabelTypeList(labelInfo.getLabelTypes());
		labelTypeList.setVisibleRowCount(5);
		labelTypeList.setFont(GUIFactory.FONTS);
		add(new JScrollPane(getLabelTypeList()), "push, grow, wrap");

		final ListSelectionListener tmp = new ListSelectionListener() {

			@Override
			public void valueChanged(final ListSelectionEvent e) {

				synchronizeTo();
			}
		};

		getLabelTypeList().addListSelectionListener(tmp);
		synchronizeFrom();
	}

	/** Setter for headerInfo */
	public void setLabelInfo(final LabelInfo labelInfo) {

		if (this.labelInfo != null) {
			this.labelInfo.deleteObserver(this);
		}
		this.labelInfo = labelInfo;
		labelInfo.addObserver(this);
		synchronizeFrom();
	}

	/** Getter for headerInfo */
	public LabelInfo getLabelInfo() {

		return labelInfo;
	}

	/** Setter for headerSummary */
	public void setLabelSummary(final LabelSummary labelSummary) {

		this.labelSummary = labelSummary;
		synchronizeFrom();
	}

	/** Getter for headerSummary */
	public LabelSummary getLabelSummary() {

		return labelSummary;
	}

	/** Setter for labelTypeList */
	public void setLabelTypeList(final String[] labelTypes) {

		if (labelTypes == null) {
			labelTypeList.setListData(new String[0]);
		} else {
			labelTypeList.setListData(labelTypes);
		}
	}

	/** Getter for labelTypeList */
	public JList<String> getLabelTypeList() {

		return labelTypeList;
	}

	/**
	 * Returns the smallest index from a selected range in the header list.
	 *
	 * @return The index of the selected list item.
	 */
	public int getSmallestSelectedIndex() {

		return getLabelTypeList().getSelectedIndex();
	}

	@Override
	public void synchronizeFrom() {

		final int[] included = getLabelSummary().getIncluded();
		final JList<String> list = getLabelTypeList();

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

		getLabelSummary().setIncluded(getLabelTypeList().getSelectedIndices());
		labelView.resetSecondaryScroll();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(final Observable o, final Object arg) {

		if (o == labelInfo) {
			setLabelTypeList(labelInfo.getLabelTypes());
			synchronizeFrom();
			repaint();
		} else {
			LogBuffer.println("LabelSummaryPanel got update from unexpected "
					+ "observable " + o);
		}
	}
}