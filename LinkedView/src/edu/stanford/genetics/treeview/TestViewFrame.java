///* BEGIN_HEADER                                              Java TreeView
// *
// * $Author: alokito $
// * $RCSfile: TestViewFrame.java,v $
// * $Revision: 1.15 $
// * $Date: 2009-08-26 11:48:27 $
// * $Name:  $
// *
// * This file is part of Java TreeView
// * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
// *
// * This software is provided under the GNU GPL Version 2. In particular,
// *
// * 1) If you modify a source file, make a comment in it containing your name and the date.
// * 2) If you distribute a modified version, you must do it under the GPL 2.
// * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
// *
// * A full copy of the license can be found in gpl.txt or online at
// * http://www.gnu.org/licenses/gpl.txt
// *
// * END_HEADER
// */
//package edu.stanford.genetics.treeview;
//
//import javax.swing.JLabel;
//
//import edu.stanford.genetics.treeview.core.HeaderFinder;
//
///**
// * Internal test class, used only by <code>main</code> test case.
// */
//public class TestViewFrame extends ViewFrame {
//
//	TestViewFrame() {
//		
//		super("Test Export Panel");
//		super.getAppFrame().getContentPane().add(
//				new JLabel("test        test"));
//	}
//
//	@Override
//	public void setLoaded(final boolean b) {
//	}
//
//	@Override
//	public void update(final java.util.Observable obs,
//			final java.lang.Object obj) {
//	}
//
//	@Override
//	public double noData() {
//		return 0.0;
//	}
//
//	@Override
//	public UrlPresets getGeneUrlPresets() {
//		return null;
//	}
//
//	@Override
//	public UrlPresets getArrayUrlPresets() {
//		return null;
//	}
//
//	// hmmm this is kind of an insane dependancy... should get rid of it,
//	// methinks.
//	public edu.stanford.genetics.treeview.plugin.dendroview.ColorPresets getColorPresets() {
//		return null;
//	}
//
//	@Override
//	public boolean getLoaded() {
//		return false;
//	}
//
//	@Override
//	public TreeViewApp getApp() {
//		return null;
//	}
//
//	@Override
//	public DataModel getDataModel() {
//		return null;
//	}
//
//	@Override
//	public void setDataModel(final DataModel model) {//, final boolean cluster,
//			//final boolean hierarchical) {
//	}
//
//	@Override
//	public HeaderFinder getGeneFinder() {
//		return null;
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see edu.stanford.genetics.treeview.ViewFrame#scrollToGene(int)
//	 */
//	@Override
//	public void scrollToGene(final int i) {
//		// nothing
//
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see edu.stanford.genetics.treeview.ViewFrame#scrollToArray(int)
//	 */
//	@Override
//	public void scrollToArray(final int i) {
//		// nothing
//
//	}
//
////	@Override
////	public MainPanel[] getMainPanelsByName(final String name) {
////		return null;
////	}
////
////	@Override
////	public MainPanel[] getMainPanels() {
////		return null;
////	}
//
//	@Override
//	public void setView(String name) {
//		// TODO Auto-generated method stub
//		
//	}
//}
