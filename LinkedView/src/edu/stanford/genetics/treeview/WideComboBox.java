package edu.stanford.genetics.treeview;

import java.awt.Dimension;

import javax.swing.ComboBoxModel;

import edu.stanford.genetics.treeview.core.AutoComboBox;

// got this workaround from the following bug: 
//      http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4618607 
public class WideComboBox extends AutoComboBox {

	private static final long serialVersionUID = 1L;

	private boolean layingOut = false;

	public WideComboBox(final Object items[]) {

		super(items);
	}

	public WideComboBox(final ComboBoxModel aModel) {

		super(aModel);
	}

	@Override
	public void doLayout() {
		try {
			layingOut = true;
			super.doLayout();
		} finally {
			layingOut = false;
		}
	}

	@Override
	public Dimension getSize() {

		final Dimension dim = super.getSize();
		if (!layingOut)
			dim.width = Math.max(dim.width, getPreferredSize().width);
		return dim;
	}
}
