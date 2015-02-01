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
//
//=======
//>>>>>>> bugFix
/**
 * @author aloksaldanha
 *
 */
public class ColumnFinderBox extends HeaderFinderBox {

	/**
	 * @param f
	 * @param hI
	 * @param geneSelection
	 */
//<<<<<<< HEAD
//	public ColumnFinderBox(final ViewFrame f, final HeaderInfo hI,
//			final HeaderSummary headerSummary,
//			final TreeSelectionI arraySelection, final MapContainer globalYmap,
//			final MapContainer globalXmap, final TreeSelectionI geneSelection,
//			final HeaderInfo geneHI) {
//
//		super(f, hI, headerSummary, arraySelection, "Column", globalYmap,
//				globalXmap, geneSelection, geneHI);
//=======
	public ColumnFinderBox() {

		super("Column");
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
