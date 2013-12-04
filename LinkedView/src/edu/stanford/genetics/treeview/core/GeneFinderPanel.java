/*
 * Created on Aug 1, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.core;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.ViewFrame;


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
	public GeneFinderPanel(ViewFrame f, HeaderInfo hI, 
			TreeSelectionI geneSelection) {
		
		super(f, hI, geneSelection, "Row");
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.HeaderFinder#scrollToIndex(int)
	 */
	@Override
	public void scrollToIndex(int i) {
		
		viewFrame.scrollToGene(i);
	}

}
