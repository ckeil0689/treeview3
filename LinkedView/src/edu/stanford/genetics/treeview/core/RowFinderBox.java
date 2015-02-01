/*
 * Created on Aug 1, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.core;

//<<<<<<< HEAD
//import edu.stanford.genetics.treeview.HeaderInfo;
//import edu.stanford.genetics.treeview.HeaderSummary;
//import edu.stanford.genetics.treeview.TreeSelectionI;
//import edu.stanford.genetics.treeview.ViewFrame;
//import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;
//=======
//>>>>>>> bugFix

/**
 * @author aloksaldanha
 *
 */
public class RowFinderBox extends HeaderFinderBox {

	/**
	 * @param f
	 * @param hI
	 * @param geneSelection
	 */
//<<<<<<< HEAD
//	public RowFinderBox(final ViewFrame f, final HeaderInfo hI,
//			final HeaderSummary headerSummary,
//			final TreeSelectionI geneSelection, final MapContainer globalXmap,
//			final MapContainer globalYmap, final TreeSelectionI arraySelection,
//			final HeaderInfo arrayHI) {
//
//		super(f, hI, headerSummary, geneSelection, "Row", globalXmap,
//				globalYmap, arraySelection, arrayHI);
//=======
	public RowFinderBox() {

		super("Row");
//>>>>>>> bugFix
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.stanford.genetics.treeview.HeaderFinder#scrollToIndex(int)
	 */
	@Override
	public void scrollToIndex(final int i) {
	}

}
