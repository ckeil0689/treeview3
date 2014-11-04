/*
 * Created on Aug 1, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.core;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;

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
	public GeneFinderBox(final ViewFrame f, final HeaderInfo hI, 
			final HeaderSummary headerSummary, 
			final TreeSelectionI geneSelection, final MapContainer globalXmap, final MapContainer globalYmap, final TreeSelectionI arraySelection, final HeaderInfo arrayHI) {

		super(f, hI, headerSummary, geneSelection, "Row", globalXmap, globalYmap, arraySelection, arrayHI);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.genetics.treeview.HeaderFinder#scrollToIndex(int)
	 */
	@Override
	public void scrollToIndex(final int i) {}

}
