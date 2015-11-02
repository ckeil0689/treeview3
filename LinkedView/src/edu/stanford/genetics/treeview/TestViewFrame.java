///* BEGIN_HEADER                                                   TreeView 3
// *
// * Please refer to our LICENSE file if you wish to make changes to this software
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
