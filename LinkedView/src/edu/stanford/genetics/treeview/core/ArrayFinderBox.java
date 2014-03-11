/*
 * Created on Aug 1, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.core;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView2;

/**
 * @author aloksaldanha
 * 
 */
public class ArrayFinderBox extends HeaderFinderBox {

	/**
	 * @param f
	 * @param hI
	 * @param geneSelection
	 */
	public ArrayFinderBox(final ViewFrame f, final DendroView2 dv,
			final HeaderInfo hI, final TreeSelectionI geneSelection) {

		super(f, hI, geneSelection, "Column");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.HeaderFinder#scrollToIndex(int)
	 */
	@Override
	public void scrollToIndex(final int i) {

		if (viewFrame != null) {
			viewFrame.scrollToArray(i);
		}
	}
}
