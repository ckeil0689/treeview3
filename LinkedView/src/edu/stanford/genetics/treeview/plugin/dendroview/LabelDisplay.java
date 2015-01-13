package edu.stanford.genetics.treeview.plugin.dendroview;

import javax.swing.JScrollBar;

/**
 * Rules for classes displaying labels.
 * @author chris0689
 *
 */
public interface LabelDisplay {

	/**
	 * Provides access to the relevant JScrollbar for an axis' label display.
	 * @return Horizontal scrollbar for row labels, vertical scrollbar for
	 * column labels.
	 */
	public abstract JScrollBar getMainScrollBar();
}
