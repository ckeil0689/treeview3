/*
 * Created on Aug 1, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.core;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;

/**
 * @author aloksaldanha
 * 
 */
public class GeneFinderPanel extends HeaderFinderPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * @param f
	 * @param hI
	 * @param geneSelection
	 */
	public GeneFinderPanel(final ViewFrame f, final DendroView dv,
			final HeaderInfo hI, final TreeSelectionI geneSelection) {

		super(f, hI, geneSelection, "Row");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.HeaderFinder#scrollToIndex(int)
	 */
	@Override
	public void scrollToIndex(final int i) {

		viewFrame.scrollToGene(i);
	}

}
