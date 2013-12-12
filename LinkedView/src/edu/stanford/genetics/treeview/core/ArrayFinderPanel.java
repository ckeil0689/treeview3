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
public class ArrayFinderPanel extends HeaderFinderPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * @param f
	 * @param hI
	 * @param geneSelection
	 */
	public ArrayFinderPanel(ViewFrame f, DendroView2 dv, HeaderInfo hI, 
			TreeSelectionI geneSelection) {
		
		super(f, hI, geneSelection, "Column");
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.HeaderFinder#scrollToIndex(int)
	 */
	@Override
	public void scrollToIndex(int i) {
		
		if (viewFrame != null) {
			viewFrame.scrollToArray(i);
		}
	}
}
