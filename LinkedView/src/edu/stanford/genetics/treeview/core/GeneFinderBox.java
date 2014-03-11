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
public class GeneFinderBox extends HeaderFinderBox {

	/**
	 * @param f
	 * @param hI
	 * @param geneSelection
	 */
	public GeneFinderBox(final ViewFrame f, final DendroView2 dv,
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
