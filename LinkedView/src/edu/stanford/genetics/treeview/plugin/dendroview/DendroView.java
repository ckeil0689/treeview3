/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: DendroView.java,v $
 * $Revision: 1.7 $
 * $Date: 2009-03-23 02:46:51 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular, 
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import javax.imageio.ImageIO;
import javax.swing.*;

import edu.stanford.genetics.treeview.*;
import edu.stanford.genetics.treeview.model.*;

//Explicitly imported because error (unclear TVModel reference) was thrown
import edu.stanford.genetics.treeview.model.TVModel;

/**
 *  This class encapsulates a dendrogram view, which is the classic Eisen
 *  treeview. It uses a drag grid panel to lay out a bunch of linked
 *  visualizations of the data, a la Eisen. In addition to laying out
 *  components, it also manages the GlobalZoomMap. This is necessary since both
 *  the GTRView (gene tree) and GlobalView need to know where to lay out genes
 *  using the same map. The zoom map is managed by the ViewFrame- it represents
 *  the selected genes, and potentially forms a link between different views,
 *  only one of which is the DendroView.
 *
 * The intention here is that you create this from a model, and never replace that model. If you want to show another file, make another dendroview. All views should of course still listen to the model, since that can still be changed ad libitum.
 *
 * @author     Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.7 $ $Date: 2009-03-23 02:46:51 $
 */
public class DendroView extends JPanel implements ConfigNodePersistent, MainPanel, Observer {
	/**
	 * EDIT
	 * @author Chris Keil
	 * Added static long serial ID
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 *  Constructor for the DendroView object
	 * note this will reuse any existing MainView subnode of the documentconfig.
	 *
	 * @param  tVModel   model this DendroView is to represent
	 * @param  vFrame  parent ViewFrame of DendroView
	 */
	public DendroView(DataModel tVModel, ViewFrame vFrame) {
		this(tVModel, null, vFrame, "Dendrogram");
	}
	public DendroView(DataModel tVModel, ConfigNode root, ViewFrame vFrame) {
		this(tVModel, root, vFrame, "Dendrogram");
	}
	/**
	 *  Constructor for the DendroView object which binds to an explicit confignode
	 *
	 * @param  dataModel   model this DendroView is to represent
	 * @param  root   Confignode to which to bind this DendroView
	 * @param  vFrame  parent ViewFrame of DendroView
	 * @param  name name of this view.
	 */
	public DendroView(DataModel dataModel, ConfigNode root, ViewFrame vFrame, String name) {
		super.setName(name);
		viewFrame = vFrame;
		if (root == null) {
			if (dataModel.getDocumentConfigRoot() != null ) {
				  bindConfig(dataModel.getDocumentConfigRoot().fetchOrCreate("MainView"));
				} else {
				  bindConfig(new DummyConfigNode("MainView"));
				}
		} else {
			bindConfig(root);
		}
		if (dataModel.getArrayHeaderInfo().getIndex("GROUP") != -1) {
			HeaderInfo headerInfo = dataModel.getArrayHeaderInfo();
			int groupIndex = headerInfo.getIndex("GROUP");
			arrayIndex = getGroupVector(headerInfo, groupIndex);
		} else {
			arrayIndex = null;
		}
		if (dataModel.getGeneHeaderInfo().getIndex("GROUP") != -1) {
			System.err.println("got gene group header");
			HeaderInfo headerInfo = dataModel.getGeneHeaderInfo();
			int groupIndex = headerInfo.getIndex("GROUP");
			geneIndex = getGroupVector(headerInfo, groupIndex);
		} else {
			geneIndex = null;
		}
		if ((arrayIndex != null) ||(geneIndex != null)){
			dataModel = new ReorderedDataModel(dataModel, geneIndex, arrayIndex);
		}
		setDataModel(dataModel);
		
		setupViews();
		
		if (geneIndex != null) {
			setGeneSelection(new ReorderedTreeSelection(viewFrame.getGeneSelection(), geneIndex));
		} else {
			setGeneSelection(viewFrame.getGeneSelection());
		}

		if (arrayIndex != null){
			setArraySelection(new ReorderedTreeSelection(viewFrame.getArraySelection(), arrayIndex));
		} else {
			setArraySelection(viewFrame.getArraySelection());
		}
	}

	private int [] getGroupVector(HeaderInfo headerInfo, int groupIndex) {
		int ngroup = 0;
		String cur = headerInfo.getHeader(0, groupIndex);
		for (int i = 0; i < headerInfo.getNumHeaders(); i++) {
			String test = headerInfo.getHeader(i, groupIndex);
			if (cur.equals(test) == false) {
				cur = test;
				ngroup++;
			}
		}
		int [] groupVector = new int[ngroup + headerInfo.getNumHeaders()];
		ngroup = 0;
		cur = headerInfo.getHeader(0, groupIndex);
		for (int i = 0; i < headerInfo.getNumHeaders(); i++) {
			String test = headerInfo.getHeader(i, groupIndex);
			if (cur.equals(test) == false) {
				groupVector[i+ngroup] = -1;
				cur = test;
				ngroup++;
			}
			groupVector[i + ngroup] = i;
		}
		return groupVector;
	}

	protected DendroView(int cols, int rows, String name) {
		super.setName(name);
	}
	
	/**
	* always returns an instance of the node, even if it has to create it.
	*/
	protected ConfigNode getFirst(String name) {
		return getConfigNode().fetchOrCreate(name);
	}

	public TreeSelectionI getGeneSelection() {
		return geneSelection;
	}
	public TreeSelectionI getArraySelection() {
		return arraySelection;
	}
	/**
	 *  This should be called after setDataModel has been set to the appropriate model
	 * @param arraySelection
	 */
	protected void setArraySelection(TreeSelectionI arraySelection) {
		if (this.arraySelection != null) {
			this.arraySelection.deleteObserver(this);	
		}
		this.arraySelection = arraySelection;
		arraySelection.addObserver(this);
		globalview.setArraySelection(arraySelection);
		zoomview.setArraySelection(arraySelection);
		atrview.setArraySelection(arraySelection);
		atrzview.setArraySelection(arraySelection);
		
		arraynameview.setArraySelection(arraySelection);
	}

	/**
	 *  This should be called after setDataModel has been set to the appropriate model
	 * @param geneSelection
	 */
	protected void setGeneSelection(TreeSelectionI geneSelection) {
		if (this.geneSelection != null) {
			this.geneSelection.deleteObserver(this);	
		}
		this.geneSelection = geneSelection;
		geneSelection.addObserver(this);
		globalview.setGeneSelection(geneSelection);
		zoomview.setGeneSelection(geneSelection);
		gtrview.setGeneSelection(geneSelection);
		
		textview.setGeneSelection(geneSelection);
		
	}
	
	/**
	 *  Gets the contrast attribute of the DendroView object
	 *
	 * @return    The contrast value
	public double getContrast() {
		return colorExtractor.getContrast();
	}
	 */


	/**
	 * Finds the currently selected genes, mirror image flips them, and then rebuilds all necessary trees and saved data to the .jtv file.
	 *
	 */
	private void flipSelectedGTRNode()
	{
			int leftIndex, rightIndex;
			String selectedID;
			TreeDrawerNode geneNode = leftTreeDrawer.getNodeById(getGeneSelection().getSelectedNode());
			
			if(geneNode == null || geneNode.isLeaf())
			{
					return;
			}
			
			selectedID = geneNode.getId();
		
			//find the starting index of the left array tree, the ending index of the right array tree
			leftIndex = getDataModel().getGeneHeaderInfo().getHeaderIndex(geneNode.getLeft().getLeftLeaf().getId());
			rightIndex = getDataModel().getGeneHeaderInfo().getHeaderIndex(geneNode.getRight().getRightLeaf().getId());
			
			int num = getDataModel().getDataMatrix().getNumRow();
			
			int [] newOrder = SetupInvertedArray(num, leftIndex, rightIndex);
			
			/*System.out.print("Fliping to: ");
			for(int i = 0; i < newOrder.length; i++)
			{
				System.out.print(newOrder[i] + " ");
			}
			System.out.println("");*/
			
			((TVModel)getDataModel()).reorderGenes(newOrder);
//			((TVModel)getDataModel()).saveGeneOrder(newOrder);	
			((Observable)getDataModel()).notifyObservers();
			
			updateGTRDrawer(selectedID);
	}

private int [] SetupInvertedArray(int num, int leftIndex, int rightIndex) {
	int []  newOrder = new int[num];
	for(int i = 0; i < num; i++)
		newOrder[i] = i;
	for(int i = 0; i <= (rightIndex - leftIndex); i++)
		newOrder[leftIndex + i] = rightIndex - i;
	return newOrder;
}

	
	

	/**
	 * Finds the currently selected arrays, mirror image flips them, and then rebuilds all necessary trees and saved data to the .jtv file.
	 *
	 */
	private void flipSelectedATRNode()
	{
			int leftIndex, rightIndex;
			String selectedID;
			TreeDrawerNode arrayNode = invertedTreeDrawer.getNodeById(getArraySelection().getSelectedNode());
			
			if(arrayNode == null || arrayNode.isLeaf())
			{
					return;
			}
			
			selectedID = arrayNode.getId();
		
			//find the starting index of the left array tree, the ending index of the right array tree
			leftIndex = getDataModel().getArrayHeaderInfo().getHeaderIndex(arrayNode.getLeft().getLeftLeaf().getId());
			rightIndex = getDataModel().getArrayHeaderInfo().getHeaderIndex(arrayNode.getRight().getRightLeaf().getId());
			
			int num = getDataModel().getDataMatrix().getNumUnappendedCol();
			
			int [] newOrder = new int[num];
			
			for(int i = 0; i < num; i++)
			{
				newOrder[i] = i;
			}
			
			for(int i = 0; i <= (rightIndex - leftIndex); i++)
			{
				newOrder[leftIndex + i] = rightIndex - i;
			}
			
			/*System.out.print("Fliping to: ");
			for(int i = 0; i < newOrder.length; i++)
			{
				System.out.print(newOrder[i] + " ");
			}
			System.out.println("");*/
			
			((TVModel)getDataModel()).reorderArrays(newOrder);
			((Observable)getDataModel()).notifyObservers();
			
			updateATRDrawer(selectedID);
	}
	
	
	
	/**
	 * Displays a data set alongside the primary one for comparison.
	 * @param model - the model containing cdt data being added to the display.
	 */
	public void compareToModel(TVModel model)
	{
		
		getDataModel().removeAppended();
		getDataModel().append(model);
		arraySelection.resize(getDataModel().getDataMatrix().getNumCol());
		arraySelection.notifyObservers();
		globalXmap.setIndexRange(0, getDataModel().getDataMatrix().getNumCol() - 1);
		globalXmap.notifyObservers();
		
		zoomXmap.setIndexRange(0, getDataModel().getDataMatrix().getNumCol() - 1);
		zoomXmap.notifyObservers();
		
		
		((Observable)getDataModel()).notifyObservers();
		
	
	}
	
	
	/**
	 * Aligns the current ATR to the passed model as best as possible, saves the new ordering to the .jtv file.
	 * @param model - AtrTVModel with which to align.
	 */
	public void alignAtrToModel(AtrTVModel model)
	{
		try{
			String selectedID = null;
			
			try {
				selectedID = getArraySelection().getSelectedNode();
			}
			catch(NullPointerException npe)
			{
			}
			
			
			int [] ordering;
			ordering = AtrAligner.align(getDataModel().getAtrHeaderInfo(), getDataModel().getArrayHeaderInfo(),
								 model.getAtrHeaderInfo(), model.getArrayHeaderInfo());
								 
			/*System.out.print("New ordering: ");
			for(int i = 0; i < ordering.length; i++)
			{
				System.out.print(ordering[i] + " ");
			}			
			System.out.println();*/
			
			
			((TVModel)getDataModel()).reorderArrays(ordering);
			((Observable)getDataModel()).notifyObservers();
			
			if(selectedID != null)
			{
				updateATRDrawer(selectedID);
			}
		}
		catch(Exception e)
		{
			System.out.println(e.toString());
		}
	}
	
	
	/**
	 * Updates the GTRDrawer to reflect changes in the DataModel gene order; rebuilds the TreeDrawerNode tree.
	 * @param selectedID ID of the node selected before a change in tree structure was made. This node is then found and reselected after the ATR tree is rebuilt.
	 */
	private void updateGTRDrawer(String selectedID)
	{
		try {
			TVModel tvmodel = (TVModel)getDataModel();
			leftTreeDrawer.setData(tvmodel.getGtrHeaderInfo(), tvmodel.getGeneHeaderInfo());
			HeaderInfo trHeaderInfo = tvmodel.getGtrHeaderInfo();
			if (trHeaderInfo.getIndex("NODECOLOR") >= 0) {
				TreeColorer.colorUsingHeader (leftTreeDrawer.getRootNode(),
				trHeaderInfo,
				trHeaderInfo.getIndex("NODECOLOR"));
			}	
		}
		catch (DendroException e) {
			//				LogPanel.println("Had problem setting up the array tree : " + e.getMessage());
			//				e.printStackTrace();
			Box mismatch = new Box(BoxLayout.Y_AXIS); mismatch.add(new JLabel(e.getMessage()));
			mismatch.add(new JLabel("Perhaps there is a mismatch between your ATR and CDT files?"));
			mismatch.add(new JLabel("Ditching Gene Tree, since it's lame."));
			JOptionPane.showMessageDialog(viewFrame, mismatch, "Tree Construction Error", JOptionPane.ERROR_MESSAGE);
			gtrview.setEnabled(false);
			try{leftTreeDrawer.setData(null, null);} catch (DendroException ex) {}
		}
		
		TreeDrawerNode arrayNode = leftTreeDrawer.getRootNode().findNode(selectedID);
		geneSelection.setSelectedNode(arrayNode.getId());
		gtrview.setSelectedNode(arrayNode);		
		geneSelection.notifyObservers();
		
		
		leftTreeDrawer.notifyObservers();
	}

	
	/**
	 * Updates the ATRDrawer to reflect changes in the DataMode array order; rebuilds the TreeDrawerNode tree.
	 * @param selectedID ID of the node selected before a change in tree structure was made. This node is then found and reselected after the ATR tree is rebuilt.
	 */
	private void updateATRDrawer(String selectedID)
	{
		try {
			TVModel tvmodel = (TVModel)getDataModel();
			invertedTreeDrawer.setData(tvmodel.getAtrHeaderInfo(), tvmodel.getArrayHeaderInfo());
			HeaderInfo trHeaderInfo = tvmodel.getAtrHeaderInfo();
			if (trHeaderInfo.getIndex("NODECOLOR") >= 0) {
				TreeColorer.colorUsingHeader (invertedTreeDrawer.getRootNode(),
				trHeaderInfo,
				trHeaderInfo.getIndex("NODECOLOR"));
			
			}	
		}
		catch (DendroException e) {
			//				LogPanel.println("Had problem setting up the array tree : " + e.getMessage());
			//				e.printStackTrace();
			Box mismatch = new Box(BoxLayout.Y_AXIS); mismatch.add(new JLabel(e.getMessage()));
			mismatch.add(new JLabel("Perhaps there is a mismatch between your ATR and CDT files?"));
			mismatch.add(new JLabel("Ditching Array Tree, since it's lame."));
			JOptionPane.showMessageDialog(viewFrame, mismatch, "Tree Construction Error", JOptionPane.ERROR_MESSAGE);
			atrview.setEnabled(false);
			atrzview.setEnabled(false);
			try{invertedTreeDrawer.setData(null, null);} catch (DendroException ex) {}
		}
		
		TreeDrawerNode arrayNode = invertedTreeDrawer.getRootNode().findNode(selectedID);
		arraySelection.setSelectedNode(arrayNode.getId());
		atrzview.setSelectedNode(arrayNode);
		atrview.setSelectedNode(arrayNode);		
		arraySelection.notifyObservers();
		
		
		invertedTreeDrawer.notifyObservers();
	}
	/**
	 * Creates an AtrTVModel for use in tree alignment.
	 * @param fileSet
	 * @return a new AtrTVModel with the file set loaded into it.
	 * @throws LoadException
	 */
	protected AtrTVModel makeAtrModel(FileSet fileSet) throws LoadException {
		 AtrTVModel atrTVModel = new AtrTVModel();
		 
		 try {
			 atrTVModel.loadNew(fileSet);
		 } catch (LoadException e) {
			 JOptionPane.showMessageDialog(this, e);
			 throw e;
		 }
		 
		 return atrTVModel;
	}
	
	protected TVModel makeCdtModel(FileSet fileSet) throws LoadException {
		 TVModel tvModel = new TVModel();
		 
		 try {
			 tvModel.loadNew(fileSet);
		 } catch (LoadException e) {
			 JOptionPane.showMessageDialog(this, e);
			 throw e;
		 }
		 
		 return tvModel;
	}


	/**
	* Open a dialog which allows the user to select a new CDT data file for tree alignment.
	*
	* @return The fileset corresponding to the dataset.
	*/
	/*
	 *  Unknow what actually happens if the file CDT does not have an associated ATR.
	 */
	protected FileSet offerATRFileSelection() throws LoadException
	{
		FileSet fileSet1; // will be chosen...
	 
		JFileChooser fileDialog = new JFileChooser();
		setupATRFileDialog(fileDialog);
		int retVal = fileDialog.showOpenDialog(this);
		if(retVal == JFileChooser.APPROVE_OPTION)
		{
			File chosen = fileDialog.getSelectedFile();
		 
			fileSet1 = new FileSet(chosen.getName(), chosen.getParent() + File.separator);
		}
		else
		{
			throw new LoadException("File Dialog closed without selection...", LoadException.NOFILE);
		}
	 
		return fileSet1;
	}

	/**
	 * Sets up a dialog for loading ATR files for tree alignment.
	 * @param fileDialog the dialog to setup
	 */
	protected void setupATRFileDialog(JFileChooser fileDialog) {
		CdtFilter ff = new CdtFilter();
		try {
			fileDialog.addChoosableFileFilter(ff);
			// will fail on pre-1.3 swings
			fileDialog.setAcceptAllFileFilterUsed(true);
		} catch (Exception e) {
			// hmm... I'll just assume that there's no accept all.
			fileDialog.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
				@Override
				public boolean accept (File f) {
					return true;
				}
				@Override
				public String getDescription () {
					return "All Files";
				}
			});
		}
		fileDialog.setFileFilter(ff);
		fileDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
	 }

	// accessors
	/**
	 *  Gets the globalXmap attribute of the DendroView object
	 *
	 * @return    The globalXmap
	 */
	public MapContainer getGlobalXmap() {
		return globalXmap;
	}


	/**
	 *  Gets the globalYmap attribute of the DendroView object
	 *
	 * @return    The globalYmap
	 */
	public MapContainer getGlobalYmap() {
		return globalYmap;
	}


	/**
	 *  Gets the zoomXmap attribute of the DendroView object
	 *
	 * @return    The zoomXmap
	 */
	public MapContainer getZoomXmap() {
		return zoomXmap;
	}


	/**
	 *  Gets the zoomYmap attribute of the DendroView object
	 *
	 * @return    The zoomYmap
	 */
	public MapContainer getZoomYmap() {
		return zoomYmap;
	}

	@Override
	public void scrollToGene(int i) {
		getGlobalYmap().scrollToIndex(i);
		getGlobalYmap().notifyObservers();
	}
	@Override
	public void scrollToArray(int i) {
		getGlobalXmap().scrollToIndex(i);
		getGlobalXmap().notifyObservers();
	}
	
    @Override
	public void update(Observable o, Object arg) {
    		if (o == geneSelection) {
    			gtrview.scrollToNode(geneSelection.getSelectedNode());
    		}
	}
	/**
	 *  This method should be called only during initial setup of the modelview
	 *
	 *  It sets up the views and binds them all to config nodes.
	 *
	 */
	protected void setupViews() {
		this.removeAll();
		ColorPresets colorPresets = DendrogramFactory.getColorPresets();
		colorExtractor = new ColorExtractor();
		colorExtractor.setDefaultColorSet(colorPresets.getDefaultColorSet());
		colorExtractor.setMissing(DataModel.NODATA, DataModel.EMPTY);
		
		hintpanel = new MessagePanel("Usage Hints");
		statuspanel = new MessagePanel("View Status");
		buttonPanel = new JPanel();

		DoubleArrayDrawer dArrayDrawer = new DoubleArrayDrawer();
		dArrayDrawer.setColorExtractor(colorExtractor);
		arrayDrawer = dArrayDrawer;
		((Observable)getDataModel()).addObserver(arrayDrawer);
		
		


	globalview = new GlobalView();
	
	// scrollbars, mostly used by maps
	globalXscrollbar = new JScrollBar(Adjustable.HORIZONTAL, 0,1,0,1);
	globalYscrollbar = new JScrollBar(Adjustable.VERTICAL,0,1,0,1);
	zoomXscrollbar = new JScrollBar(Adjustable.HORIZONTAL, 0,1,0,1);
	zoomYscrollbar = new JScrollBar(Adjustable.VERTICAL,0,1,0,1);



		 zoomXmap = new MapContainer();
		 zoomXmap.setDefaultScale(12.0);
		 zoomXmap.setScrollbar(zoomXscrollbar);
		 zoomYmap = new MapContainer();
		 zoomYmap.setDefaultScale(12.0);
		 zoomYmap.setScrollbar(zoomYscrollbar);

		 // globalmaps tell globalview, atrview, and gtrview
		 // where to draw each data point.
	// the scrollbars "scroll" by communicating with the maps.
		globalXmap = new MapContainer();
		globalXmap.setDefaultScale(2.0);
		globalXmap.setScrollbar(globalXscrollbar);
		globalYmap = new MapContainer();
		globalYmap.setDefaultScale(2.0);
		globalYmap.setScrollbar(globalYscrollbar);

		globalview.setXMap(globalXmap);
		globalview.setYMap(globalYmap);
		
		globalview.setZoomYMap(getZoomYmap());
		globalview.setZoomXMap(getZoomXmap());
		
		globalview.setArrayDrawer(arrayDrawer);


		arraynameview = new ArrayNameView(getDataModel().getArrayHeaderInfo());
		arraynameview.setUrlExtractor(viewFrame.getArrayUrlExtractor());
		arraynameview.setDataModel(getDataModel());

		leftTreeDrawer = new LeftTreeDrawer();
		gtrview = new GTRView();
		gtrview.setMap(globalYmap);
		gtrview.setLeftTreeDrawer(leftTreeDrawer);
		gtrview.getHeaderSummary().setIncluded(new int [] {0,3});
		
		invertedTreeDrawer = new InvertedTreeDrawer();
		atrview = new ATRView();
		atrview.setMap(globalXmap);
		atrview.setInvertedTreeDrawer(invertedTreeDrawer);
		atrview.getHeaderSummary().setIncluded(new int [] {0,3});

		atrzview = new ATRZoomView();
		atrzview.setZoomMap(getZoomXmap());
		atrzview.setHeaderSummary(atrview.getHeaderSummary());
		atrzview.setInvertedTreeDrawer(invertedTreeDrawer);

		zoomview = new ZoomView();
		zoomview.setYMap(getZoomYmap());
		zoomview.setXMap(getZoomXmap());
		zoomview.setArrayDrawer(arrayDrawer);

		arraynameview.setMapping(getZoomXmap());

		
		
		textview = new TextViewManager(getDataModel().getGeneHeaderInfo(), viewFrame.getUrlExtractor());
		
		textview.setMap(getZoomYmap());
		

		doDoubleLayout();
		

		// reset persistent popups
		settingsFrame = null;
		settingsPanel = null;

		// urls
		colorExtractor.bindConfig(getFirst("ColorExtractor"));
		
		// set data first to avoid adding auto-genereated contrast to documentConfig.
		dArrayDrawer.setDataMatrix(getDataModel().getDataMatrix());
		dArrayDrawer.bindConfig(getFirst("ArrayDrawer"));

		// this is here because my only subclass shares this code.
		bindTrees();
		
		zoomview.setHeaders(getDataModel().getGeneHeaderInfo(), getDataModel().getArrayHeaderInfo());
		
		globalXmap.bindConfig(getFirst("GlobalXMap"));
		globalYmap.bindConfig(getFirst("GlobalYMap"));
		getZoomXmap().bindConfig(getFirst("ZoomXMap"));
		getZoomYmap().bindConfig(getFirst("ZoomYMap"));

		textview.bindConfig(getFirst("TextView"));			
		
		arraynameview.bindConfig(getFirst("ArrayNameView"));
		HeaderSummary atrSummary = atrview.getHeaderSummary();
		atrzview.setHeaderSummary(atrSummary);
		atrSummary.bindConfig(getFirst("AtrSummary"));
		gtrview.getHeaderSummary().bindConfig(getFirst("GtrSummary"));

		// perhaps I could remember this stuff in the MapContainer...
		globalXmap.setIndexRange(0, dataModel.getDataMatrix().getNumCol() - 1);
		globalYmap.setIndexRange(0, dataModel.getDataMatrix().getNumRow() - 1);
		getZoomXmap().setIndexRange(-1, -1);
		getZoomYmap().setIndexRange(-1, -1);

		globalXmap.notifyObservers();
		globalYmap.notifyObservers();
		getZoomXmap().notifyObservers();
		getZoomYmap().notifyObservers();
	}

	/**
	* this is meant to be called from setupViews.
	* It make sure that the trees are generated from the current model,
	* and enables/disables them as required.
	*
	* I factored it out because it is common betwen DendroView and KnnDendroView.
	*/
	protected void bindTrees() {
		DataModel tvmodel =  getDataModel();

		if ((tvmodel != null) && tvmodel.aidFound()) {
			try {
				atrview.setEnabled(true);
				atrzview.setEnabled(true);
				invertedTreeDrawer.setData(tvmodel.getAtrHeaderInfo(), tvmodel.getArrayHeaderInfo());
				HeaderInfo trHeaderInfo = tvmodel.getAtrHeaderInfo();
				if (trHeaderInfo.getIndex("NODECOLOR") >= 0) {
					TreeColorer.colorUsingHeader (invertedTreeDrawer.getRootNode(),
					trHeaderInfo,
					trHeaderInfo.getIndex("NODECOLOR"));
					
				}	
			} catch (DendroException e) {
				//				LogPanel.println("Had problem setting up the array tree : " + e.getMessage());
				//				e.printStackTrace();
				Box mismatch = new Box(BoxLayout.Y_AXIS); mismatch.add(new JLabel(e.getMessage()));
				mismatch.add(new JLabel("Perhaps there is a mismatch between your ATR and CDT files?"));
				mismatch.add(new JLabel("Ditching Array Tree, since it's lame."));
				JOptionPane.showMessageDialog(viewFrame, mismatch, "Tree Construction Error", JOptionPane.ERROR_MESSAGE);
				atrview.setEnabled(false);
				atrzview.setEnabled(false);
				try{invertedTreeDrawer.setData(null, null);} catch (DendroException ex) {}
			}
		} else {
			atrview.setEnabled(false);
			atrzview.setEnabled(false);
				try{invertedTreeDrawer.setData(null, null);} catch (DendroException ex) {}
		}
		invertedTreeDrawer.notifyObservers();

		if ((tvmodel != null) && tvmodel.gidFound()) {
			try {
				leftTreeDrawer.setData(tvmodel.getGtrHeaderInfo(), tvmodel.getGeneHeaderInfo());
				HeaderInfo gtrHeaderInfo = tvmodel.getGtrHeaderInfo();
				if (gtrHeaderInfo.getIndex("NODECOLOR") >= 0) {
					TreeColorer.colorUsingHeader (leftTreeDrawer.getRootNode(),
					tvmodel.getGtrHeaderInfo(),
					gtrHeaderInfo.getIndex("NODECOLOR"));
					
				} else {
					TreeColorer.colorUsingLeaf(leftTreeDrawer.getRootNode(),
							tvmodel.getGeneHeaderInfo(),
							tvmodel.getGeneHeaderInfo().getIndex("FGCOLOR")
							);
				}

				gtrview.setEnabled(true);
			} catch (DendroException e) {
//				LogPanel.println("Had problem setting up the gene tree : " + e.getMessage());
//				e.printStackTrace();
				Box mismatch = new Box(BoxLayout.Y_AXIS); mismatch.add(new JLabel(e.getMessage()));
				mismatch.add(new JLabel("Perhaps there is a mismatch between your GTR and CDT files?"));
				mismatch.add(new JLabel("Ditching Gene Tree, since it's lame."));
				JOptionPane.showMessageDialog(viewFrame, mismatch, "Tree Construction Error", JOptionPane.ERROR_MESSAGE);
				gtrview.setEnabled(false);
				try{leftTreeDrawer.setData(null, null);} catch (DendroException ex) {}
			}
		} else {
			gtrview.setEnabled(false);
			try{leftTreeDrawer.setData(null, null);} catch (DendroException ex) {}
		}
		leftTreeDrawer.notifyObservers();

	}
/**
 * Lays out components in a single DragGridPanel
 *
 */
	/*
	private void doSingleLayout() {
		Rectangle rectangle  = new Rectangle(0, 0, 1, 2);

		DragGridPanel innerPanel = new DragGridPanel(4, 3);
		innerPanel.setBorderWidth(2);
		innerPanel.setBorderHeight(2);
		innerPanel.setMinimumWidth(1);
		innerPanel.setMinimumHeight(1);
		innerPanel.setFocusWidth(1);
		innerPanel.setFocusHeight(1);

		innerPanel.addComponent(statuspanel, rectangle);
		rectangle.translate(1, 0);

		innerPanel.addComponent(atrview, rectangle);
		registerView(atrview);

		rectangle.translate(1, 0);
		rectangle.setSize(1, 1);
		innerPanel.addComponent(arraynameview, rectangle);
		registerView(arraynameview);
		rectangle.translate(0, 1);
		innerPanel.addComponent(atrzview, rectangle);
		registerView(atrzview);

		rectangle.setSize(1, 2);
		rectangle.translate(1, -1);
		innerPanel.addComponent(hintpanel, rectangle);

		rectangle = new Rectangle(0, 2, 1, 1);
		JPanel gtrPanel = new JPanel();
		gtrPanel.setLayout(new BorderLayout());
		gtrPanel.add(gtrview, BorderLayout.CENTER);
		gtrPanel.add(new JScrollBar(JScrollBar.HORIZONTAL, 0,1,0,0), BorderLayout.SOUTH);
		innerPanel.addComponent(gtrPanel, rectangle);
		gtrview.setHintPanel(hintpanel);
		gtrview.setStatusPanel(statuspanel);
		gtrview.setViewFrame(viewFrame);

		// global view
		rectangle.translate(1, 0);
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(globalview, BorderLayout.CENTER);
		panel.add(globalYscrollbar, BorderLayout.EAST);
		panel.add(globalXscrollbar, BorderLayout.SOUTH);	
		innerPanel.addComponent(panel, rectangle);
		globalview.setHintPanel(hintpanel);
		globalview.setStatusPanel(statuspanel);
		globalview.setViewFrame(viewFrame);

		// zoom view
		rectangle.translate(1, 0);
		JPanel zoompanel = new JPanel();
		zoompanel.setLayout(new BorderLayout());
		zoompanel.add(zoomview, BorderLayout.CENTER);
		zoompanel.add(zoomYscrollbar, BorderLayout.EAST);
		zoompanel.add(zoomXscrollbar, BorderLayout.SOUTH);	
		innerPanel.addComponent(zoompanel, rectangle);
		zoomview.setHintPanel(hintpanel);
		zoomview.setStatusPanel(statuspanel);
		zoomview.setViewFrame(viewFrame);



		rectangle.translate(1, 0);
		innerPanel.addComponent(textview, rectangle);
		registerView(textview);
		add(innerPanel);
		
	}
	*/
	
	/**
	 * Lays out components in two DragGridPanel separated by a
	 * JSplitPane, so that you can expand/contract with one click.
	 *
	 */

	protected void doDoubleLayout() {
	  DragGridPanel left = new DragGridPanel(2, 2);
	  left.setName("LeftDrag");
	  DragGridPanel right = new DragGridPanel(2,3);
	 right.setName("RightDrag");
	    left.setBorderWidth(2);
		left.setBorderHeight(2);
		left.setMinimumWidth(1);
		left.setMinimumHeight(1);
		left.setFocusWidth(1);
		left.setFocusHeight(1);

	    right.setBorderWidth(2);
		right.setBorderHeight(2);
		right.setMinimumWidth(1);
		right.setMinimumHeight(1);
		right.setFocusWidth(1);
		right.setFocusHeight(1);

		float lheights []  = new float[2];
		lheights [0] = (float) .15;
		lheights[1] = (float) .85;
		left.setHeights(lheights);

		float lwidths []  = new float[2];
		lwidths [0] = (float) .35;
		lwidths[1] = (float) .65;
		left.setWidths(lwidths);

		float rheights [] = new float[4];
		rheights[0] = (float).15;
		rheights[1] = (float).05;
		rheights[2] = (float).8;
		rheights[3] = (float).8;
		right.setHeights(rheights);

		
		Rectangle rectangle  = new Rectangle(0, 0, 1, 1);

		left.addComponent(statuspanel, rectangle);
		rectangle.translate(1, 0);

		left.addComponent(atrview.getComponent(), rectangle);
		registerView(atrview);

		rectangle.translate(-1, 0);
		right.addComponent(arraynameview.getComponent(), rectangle);
		registerView(arraynameview);
		
		rectangle.translate(0, 1);
		right.addComponent(atrzview.getComponent(), rectangle);
		registerView(atrzview);

		rectangle.setSize(1, 2);
		rectangle.translate(1, -1);
		right.addComponent(hintpanel, rectangle);

		rectangle = new Rectangle(0, 1, 1, 1);
		JPanel gtrPanel = new JPanel();
		gtrPanel.setLayout(new BorderLayout());
		gtrPanel.add(gtrview, BorderLayout.CENTER);
		gtrPanel.add(new JScrollBar(Adjustable.HORIZONTAL, 0,1,0,1), BorderLayout.SOUTH);
		left.addComponent(gtrPanel, rectangle);
		gtrview.setHintPanel(hintpanel);
		gtrview.setStatusPanel(statuspanel);
		gtrview.setViewFrame(viewFrame);

		// global view
		rectangle.translate(1, 0);
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(globalview, BorderLayout.CENTER);
		panel.add(globalYscrollbar, BorderLayout.EAST);
		panel.add(globalXscrollbar, BorderLayout.SOUTH);	
		left.addComponent(panel, rectangle);
		registerView(globalview);

		// zoom view
		rectangle.translate(-1, 1);
		JPanel zoompanel = new JPanel();
		zoompanel.setLayout(new BorderLayout());
		zoompanel.add(zoomview, BorderLayout.CENTER);
		zoompanel.add(zoomXscrollbar, BorderLayout.SOUTH);	
		zoompanel.add(zoomYscrollbar, BorderLayout.EAST);
		right.addComponent(zoompanel, rectangle);
		registerView(zoomview);
		
		//rectangle.translate(0, 1);
		//right.addComponent(buttonPanel, rectangle);
		JButton saveButton = new JButton("Save Image");
		saveButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				try {
					
					saveImage(zoomview);
					
				} catch (IOException e) {
					
					e.printStackTrace();
				}	
			}
		});
		
		buttonPanel.add(saveButton);
		
		rectangle.translate(1, 0);
		JPanel textpanel = new JPanel();
		textpanel.setLayout(new BorderLayout());
		textpanel.add(textview.getComponent(), BorderLayout.CENTER);
		
		//textpanel.add(saveButton);
		
		right.addComponent(textpanel, rectangle);
		registerView(textview);

		JSplitPane innerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				left, right);
		//adds expansion arrows to central divider
		innerPanel.setOneTouchExpandable(true);
		innerPanel.setDividerLocation(300);
		//CardLayout sets up the tabbed organization of plugin-windows
		setLayout(new CardLayout());
		add(innerPanel, "running");
	}



	/**
	 *  registers a modelview with the hint and status panels, and the viewFrame.
	 *
	 * @param  modelView  The ModelView to be added
	 */
	private void registerView(ModelView modelView) {
		modelView.setHintPanel(hintpanel);
		modelView.setStatusPanel(statuspanel);
		modelView.setViewFrame(viewFrame);
	}

	

	// Menus
	
		  @Override
		public void populateExportMenu(TreeviewMenuBarI menu)
	  {
		  	menu.addMenuItem("Export to Postscript...", new ActionListener() {
		  @Override
		public void actionPerformed(ActionEvent actionEvent) {

			  
			  MapContainer initXmap, initYmap;
			  if ((getArraySelection().getNSelectedIndexes() != 0) || (getGeneSelection().getNSelectedIndexes() != 0)){
				  initXmap = getZoomXmap();
				  initYmap = getZoomYmap();
			  } else {
				  initXmap = getGlobalXmap();
				  initYmap = getGlobalYmap();
			  }
			PostscriptExportPanel psePanel = setupPostscriptExport(initXmap, initYmap);
			final JDialog popup = new CancelableSettingsDialog(viewFrame, "Export to Postscript", psePanel);
			popup.pack();
			popup.setVisible(true);
		  }

		});
		menu.setAccelerator(KeyEvent.VK_X);
		menu.setMnemonic(KeyEvent.VK_X);
		
		menu.addMenuItem("Export to Image...", new ActionListener() {
		  @Override
		public void actionPerformed(ActionEvent actionEvent) {

			  MapContainer initXmap, initYmap;
			  if ((getArraySelection().getNSelectedIndexes() != 0) || (getGeneSelection().getNSelectedIndexes() != 0)){
				  initXmap = getZoomXmap();
				  initYmap = getZoomYmap();
			  } else {
				  initXmap = getGlobalXmap();
				  initYmap = getGlobalYmap();
			  }

			  BitmapExportPanel bitmapPanel = setupBitmapExport(initXmap, initYmap);

			final JDialog popup = new CancelableSettingsDialog(viewFrame, "Export to Image", bitmapPanel);
			popup.pack();
			popup.setVisible(true);
		  }

		});
		menu.setMnemonic(KeyEvent.VK_I);

		menu.addMenuItem("Export ColorBar to Postscript...", new ActionListener() {
		  @Override
		public void actionPerformed(ActionEvent actionEvent) {

			  PostscriptColorBarExportPanel gcbPanel = new PostscriptColorBarExportPanel(
			  ((DoubleArrayDrawer) arrayDrawer).getColorExtractor());
			  gcbPanel.setSourceSet(getDataModel().getFileSet());

			final JDialog popup = new CancelableSettingsDialog(viewFrame, "Export ColorBar to Postscript", gcbPanel);
			popup.pack();
			popup.setVisible(true);
		  }
		});
		menu.setMnemonic(KeyEvent.VK_B);

		menu.addMenuItem("Export ColorBar to Image...",new ActionListener() {
		  @Override
		public void actionPerformed(ActionEvent actionEvent) {

			  BitmapColorBarExportPanel gcbPanel = new BitmapColorBarExportPanel(
			  ((DoubleArrayDrawer) arrayDrawer).getColorExtractor());
			  gcbPanel.setSourceSet(getDataModel().getFileSet());

			final JDialog popup = new CancelableSettingsDialog(viewFrame, "Export ColorBar to Image", gcbPanel);
			popup.pack();
			popup.setVisible(true);
		  }
		});
		menu.setMnemonic(KeyEvent.VK_M);

		
		menu.addSeparator();
		addSimpleExportOptions(menu);
		
	  }
	  
	  
		private void addSimpleExportOptions(TreeviewMenuBarI menu) {
			menu.addMenuItem("Save Tree Image", new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					
					MapContainer initXmap, initYmap;
					initXmap = getGlobalXmap();
					initYmap = getGlobalYmap();
					
					BitmapExportPanel bitmapPanel = new BitmapExportPanel
					(arraynameview.getHeaderInfo(), 
					getDataModel().getGeneHeaderInfo(),
					getGeneSelection(), getArraySelection(),
					invertedTreeDrawer,
					leftTreeDrawer, arrayDrawer, initXmap, initYmap);
					bitmapPanel.setGeneFont(textview.getFont());
					bitmapPanel.setArrayFont(arraynameview.getFont());
					bitmapPanel.setSourceSet(getDataModel().getFileSet());
					bitmapPanel.setDrawSelected(false);
					bitmapPanel.includeData(false);
					bitmapPanel.includeAtr(false);
					bitmapPanel.deselectHeaders();
					
					final JDialog popup = new CancelableSettingsDialog(viewFrame, "Export to Image", bitmapPanel);
					popup.pack();
					popup.setVisible(true);
				}
			});
			menu.setMnemonic(KeyEvent.VK_T);
			
			menu.addMenuItem("Save Thumbnail Image", new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					
					MapContainer initXmap, initYmap;
					initXmap = getGlobalXmap();
					initYmap = getGlobalYmap();
					
					BitmapExportPanel bitmapPanel = new BitmapExportPanel
					(arraynameview.getHeaderInfo(), 
					getDataModel().getGeneHeaderInfo(),
					getGeneSelection(), getArraySelection(),
					invertedTreeDrawer,
					leftTreeDrawer, arrayDrawer, initXmap, initYmap);
					bitmapPanel.setSourceSet(getDataModel().getFileSet());
					bitmapPanel.setGeneFont(textview.getFont());
					bitmapPanel.setArrayFont(arraynameview.getFont());
					bitmapPanel.setDrawSelected(false);
					bitmapPanel.includeGtr(false);
					bitmapPanel.includeAtr(false);
					bitmapPanel.deselectHeaders();
					
					final JDialog popup = new CancelableSettingsDialog(viewFrame, "Export To Image", bitmapPanel);
					popup.pack();
					popup.setVisible(true);
				}
			});
			menu.setMnemonic(KeyEvent.VK_H);
			
			menu.addMenuItem("Save Zoomed Image", new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					
					MapContainer initXmap, initYmap;
					initXmap = getZoomXmap();
					initYmap = getZoomYmap();
					
					BitmapExportPanel bitmapPanel = new BitmapExportPanel
					(arraynameview.getHeaderInfo(), 
					getDataModel().getGeneHeaderInfo(),
					getGeneSelection(), getArraySelection(),
					invertedTreeDrawer,
					leftTreeDrawer, arrayDrawer, initXmap, initYmap);
					bitmapPanel.setSourceSet(getDataModel().getFileSet());
					bitmapPanel.setGeneFont(textview.getFont());
					bitmapPanel.setArrayFont(arraynameview.getFont());
					
					bitmapPanel.includeGtr(false);
					bitmapPanel.includeAtr(false);
					bitmapPanel.deselectHeaders();
					
					final JDialog popup = new CancelableSettingsDialog(viewFrame, "Export To Image", bitmapPanel);
					popup.pack();
					popup.setVisible(true);
				}
			});
			menu.setMnemonic(KeyEvent.VK_Z);
		}
	  
	  
	  /**
	   * show summary of the specified indexes
	   */
	  public void showSubDataModel(int [] indexes) {
	  	getViewFrame().showSubDataModel(indexes, null, null);
	  }


	/**
	 *  adds DendroView stuff to Analysis menu
	 *
	 * @param  menu  menu to add to
	 */
	@Override
	public void populateAnalysisMenu(TreeviewMenuBarI menu) {
		menu.addMenuItem("Flip Array Tree Node", new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent ae)
				{
					if (getGtrview().hasFocus())
						flipSelectedGTRNode();
					else
						flipSelectedATRNode();
				}
			}
		);
		menu.setAccelerator(KeyEvent.VK_L);
		menu.setMnemonic(KeyEvent.VK_A);

		menu.addMenuItem("Flip Gene Tree Node", new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent ae)
				{
					flipSelectedGTRNode();
				}
			}
		);
		menu.setMnemonic(KeyEvent.VK_G);
		
		menu.addMenuItem("Align to Tree...", new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent ae)
				{
					try
					{
						FileSet fileSet = offerATRFileSelection();
						AtrTVModel atrModel = makeAtrModel(fileSet);
						
						alignAtrToModel(atrModel);
						
					}
					catch (LoadException e)
					{
						if ((e.getType() != LoadException.INTPARSE) &&
							(e.getType() != LoadException.NOFILE)) {
							LogBuffer.println("Could not open file: " + e.getMessage());
							e.printStackTrace();
						}
					}
				}			
			}
		);
		menu.setAccelerator(KeyEvent.VK_A);
		menu.setMnemonic(KeyEvent.VK_G);
		
		menu.addMenuItem("Compare to...", new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent ae)
				{
					try
					{
						FileSet fileSet = offerATRFileSelection();
						TVModel tvModel = makeCdtModel(fileSet);
						compareToModel(tvModel);						
					}
					catch (LoadException e)
					{
						if ((e.getType() != LoadException.INTPARSE) &&
							(e.getType() != LoadException.NOFILE)) {
							LogBuffer.println("Could not open file: " + e.getMessage());
							e.printStackTrace();
						}
					}
				}
			}
		);
		menu.setAccelerator(KeyEvent.VK_C);
		menu.setMnemonic(KeyEvent.VK_C);
		
		menu.addMenuItem("Remove comparison", new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent ae)
				{
					getDataModel().removeAppended();
					globalXmap.setIndexRange(0, getDataModel().getDataMatrix().getNumCol() - 1);
					globalXmap.notifyObservers();
					zoomXmap.setIndexRange(0, getDataModel().getDataMatrix().getNumCol() - 1);
					zoomXmap.notifyObservers();
					((Observable)getDataModel()).notifyObservers();
				}
			}
		);
		menu.setAccelerator(KeyEvent.VK_R);
		menu.setMnemonic(KeyEvent.VK_R);		

		menu.addMenuItem("Summary Window...",new ActionListener() {
		  @Override
		public void actionPerformed(ActionEvent e) {
			  SummaryViewWizard  wizard = new SummaryViewWizard(DendroView.this);
			  int retval = JOptionPane.showConfirmDialog(DendroView.this, wizard, "Configure Summary", JOptionPane.OK_CANCEL_OPTION);
			  if (retval == JOptionPane.OK_OPTION) {
			  	showSubDataModel(wizard.getIndexes());
			  }
		  }
		});
		menu.setMnemonic(KeyEvent.VK_S);
	}

	/**
	 *  adds DendroView stuff to Document menu
	 *
	 * @param  menu  menu to add to
	 */
	@Override
	public void populateSettingsMenu(TreeviewMenuBarI menu) {
		menu.addMenuItem("Pixel Settings...", new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
				ColorExtractor ce = null;
				try {
					ce = ((DoubleArrayDrawer) arrayDrawer).getColorExtractor();
				} catch (Exception e) {
				}
				PixelSettingsSelector pssSelector
								 = new PixelSettingsSelector
								(globalXmap, globalYmap,
								getZoomXmap(), getZoomYmap(),
								ce, DendrogramFactory.getColorPresets());

				JDialog popup = new ModelessSettingsDialog(viewFrame, "Pixel Settings", pssSelector);
			 System.out.println("showing popup...");
			 	popup.addWindowListener(XmlConfig.getStoreOnWindowClose(getDataModel().getDocumentConfigRoot()));	
				popup.pack();
				popup.setVisible(true);
				}
			}, 0);
		menu.setMnemonic(KeyEvent.VK_X);

		menu.addMenuItem("Url Settings...", new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			// keep refs to settingsPanel, settingsFrame local, since will dispose of self when closed...
			TabbedSettingsPanel settingsPanel = new TabbedSettingsPanel();
			
			UrlSettingsPanel genePanel = new UrlSettingsPanel(viewFrame.getUrlExtractor(), viewFrame.getGeneUrlPresets());
			settingsPanel.addSettingsPanel("Gene", genePanel);
			
			UrlSettingsPanel arrayPanel = new UrlSettingsPanel(viewFrame.getArrayUrlExtractor(), viewFrame.getArrayUrlPresets());
			settingsPanel.addSettingsPanel("Array", arrayPanel);
			

			JDialog settingsFrame = new ModelessSettingsDialog(viewFrame, "Url Settings", settingsPanel); 

			settingsFrame.addWindowListener(XmlConfig.getStoreOnWindowClose(getDataModel().getDocumentConfigRoot()));
			settingsFrame.pack();
			settingsFrame.setVisible(true);
		}
	}, 0);
	menu.setMnemonic(KeyEvent.VK_U);

	menu.addMenuItem("Font Settings...", new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			// keep ref to settingsFrame local, since will dispose of self when closed...
			TabbedSettingsPanel settingsPanel = new TabbedSettingsPanel();
			
			FontSettingsPanel genePanel = new FontSettingsPanel(textview);
			settingsPanel.addSettingsPanel("Gene", genePanel);
			
			FontSettingsPanel arrayPanel = new FontSettingsPanel(arraynameview);
			settingsPanel.addSettingsPanel("Array", arrayPanel);
			
			JDialog settingsFrame = new ModelessSettingsDialog(viewFrame, "Font Settings", settingsPanel); 
			settingsFrame.addWindowListener(XmlConfig.getStoreOnWindowClose(getDataModel().getDocumentConfigRoot()));
			settingsFrame.pack();
			settingsFrame.setVisible(true);
		}
	}, 0);
	menu.setMnemonic(KeyEvent.VK_F);

	menu.addMenuItem("Annotations...", new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			// keep refs to settingsPanel, settingsFrame local, since will dispose of self when closed...
			TabbedSettingsPanel settingsPanel = new TabbedSettingsPanel();
			
			HeaderSummaryPanel genePanel = new HeaderSummaryPanel(getDataModel().getGeneHeaderInfo(), textview.getHeaderSummary());
			settingsPanel.addSettingsPanel("Gene", genePanel);
			
			HeaderSummaryPanel arrayPanel = new HeaderSummaryPanel(arraynameview.getHeaderInfo(), arraynameview.getHeaderSummary());
			settingsPanel.addSettingsPanel("Array", arrayPanel);
			
			HeaderSummaryPanel atrPanel = new HeaderSummaryPanel(getDataModel().getAtrHeaderInfo(), atrview.getHeaderSummary());
			settingsPanel.addSettingsPanel("Array Tree", atrPanel);
			
			HeaderSummaryPanel gtrPanel = new HeaderSummaryPanel(getDataModel().getGtrHeaderInfo(), gtrview.getHeaderSummary());
			settingsPanel.addSettingsPanel("Gene Tree", gtrPanel);
			

			JDialog settingsFrame = new ModelessSettingsDialog(viewFrame, "Annotation Settings", settingsPanel); 

			settingsFrame.addWindowListener(XmlConfig.getStoreOnWindowClose(getDataModel().getDocumentConfigRoot()));
			settingsFrame.addWindowListener(new WindowAdapter() {
				 @Override
				public void windowClosed(WindowEvent e) {
				 	textview.repaint();
				 	
					arraynameview.repaint();
				 }
			});
			settingsFrame.pack();
			settingsFrame.setVisible(true);
			
		}
	}, 0);
	menu.setMnemonic(KeyEvent.VK_A);

		/*
	MenuItem urlItem    = new MenuItem("Url Options...");
		urlItem.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
				  UrlEditor urlEditor = new UrlEditor(urlExtractor, viewFrame.getGeneUrlPresets(), dataModel.getGeneHeaderInfo()); 
				  urlEditor.showConfig(viewFrame);
				  dataModel.getDocumentConfig().store();
				}
			});
		menu.add(urlItem);
	*/
	}

	/**
	 *  this function changes the info in the confignode to match the current panel sizes. 
	 * this is a hack, since I don't know how to intercept panel resizing.
	 * Actually, in the current layout this isn't even used.
	 */
	@Override
	public void syncConfig() {
		/*
		DragGridPanel running   = this;
		floa	t[] heights         = running.getHeights();
		ConfigNode heightNodes[]  = root.fetch("Height");
		for (int i = 0; i < heights.length; i++) {
			if (i < heightNodes.length) {
				heightNodes[i].setAttribute("value", (double) heights[i],
						1.0 / heights.length);
			} else {
				ConfigNode n  = root.create("Height");
					n.setAttribute("value", (double) heights[i],
							1.0 / heights.length);
			}
		}

	float[] widths          = running.getWidths();
	ConfigNode widthNodes[]   = root.fetch("Width");
		for (int i = 0; i < widths.length; i++) {
			if (i < widthNodes.length) {
				widthNodes[i].setAttribute("value", (double) widths[i],
						1.0 / widths.length);
			} else {
			ConfigNode n  = root.create("Width");
				n.setAttribute("value", (double) widths[i], 1.0 / widths.length);
			}
		}
*/
	}


	/**
	 *  binds this dendroView to a particular confignode, resizing the panel sizes
	 *  appropriately.
	 *
	 * @param  configNode  ConfigNode to bind to
	 */

	@Override
	public void bindConfig(ConfigNode configNode) {
		root = configNode;
		/*
	ConfigNode heightNodes[]  = root.fetch("Height");
	ConfigNode widthNodes[]   = root.fetch("Width");

	float heights[];
	float widths[];
		if (heightNodes.length != 0) {
			heights = new float[heightNodes.length];
			widths = new float[widthNodes.length];
			for (int i = 0; i < heights.length; i++) {
				heights[i] = (float) heightNodes[i].getAttribute("value", 1.0 / heights.length);
			}
			for (int j = 0; j < widths.length; j++) {
				widths[j] = (float) widthNodes[j].getAttribute("value", 1.0 / widths.length);
			}
		} else {
			widths = new float[]{2 / 11f, 3 / 11f, 3 / 11f, 3 / 11f};
			heights = new float[]{3 / 16f, 1 / 16f, 3 / 4f};
		}
		setHeights(heights);
		setWidths(widths);
		*/
	}

	public void saveImage(JPanel panel) throws IOException{
		
		//Container c = viewFrame.getContentPane();
		BufferedImage im = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
		panel.paint(im.getGraphics());
		ImageIO.write(im, "PNG", new File("sampleImage.png"));
		
	}
	
	protected ViewFrame viewFrame;
	/** Setter for viewFrame */
	public void setViewFrame(ViewFrame viewFrame) {
		this.viewFrame = viewFrame;
	}
	/** Getter for viewFrame */
	public ViewFrame getViewFrame() {
		return viewFrame;
	}
	// holds the thumb and zoom panels
	protected ScrollPane panes[];
	protected boolean loaded;

	private DataModel dataModel = null;
	/** Setter for dataModel 
	 * 
	 * */
	protected void setDataModel(DataModel dataModel) {
		this.dataModel = dataModel;
	}
	/** 
	 * 	* gets the model this dendroview is based on
	 */
	protected DataModel getDataModel() {
		return this.dataModel;
	}
	/**
	 * The following arrays allow translation to and from screen and datamatrix 
	 * I had to add these in order to have gaps in the dendroview of k-means
	 */
	private int [] arrayIndex  = null;
	private int [] geneIndex   = null;
	
    protected JScrollBar globalXscrollbar, globalYscrollbar;
    protected GlobalView globalview;

    protected JScrollBar zoomXscrollbar, zoomYscrollbar;
	protected ZoomView zoomview;
	protected TextViewManager textview;
	protected ArrayNameView arraynameview;
	protected GTRView gtrview;
	protected ATRView atrview;
	protected ATRZoomView atrzview;
	protected InvertedTreeDrawer invertedTreeDrawer;
	protected LeftTreeDrawer leftTreeDrawer;

	private TreeSelectionI geneSelection = null;
	private TreeSelectionI arraySelection = null;

	protected MapContainer globalXmap, globalYmap;
	protected MapContainer zoomXmap,   zoomYmap;

	protected MessagePanel hintpanel;
	protected MessagePanel statuspanel;
	protected JPanel buttonPanel;
	protected BrowserControl browserControl;
	protected ArrayDrawer arrayDrawer;
	protected ConfigNode root;
	/** Setter for root  - may not work properly
	public void setConfigNode(ConfigNode root) {
		this.root = root;
	}
	/** Getter for root */
	@Override
	public ConfigNode getConfigNode() {
		return root;
	}
	
	// persistent popups
	protected JDialog settingsFrame;
	protected TabbedSettingsPanel settingsPanel;
	
	private static ImageIcon treeviewIcon = null;
	private ColorExtractor colorExtractor;
	/**
	 * icon for display in tabbed panel
	 */
	@Override
	public ImageIcon getIcon() {
		if (treeviewIcon == null)
			try {
				treeviewIcon = new ImageIcon("images/treeview.gif", "TreeView Icon");
			} catch (java.security.AccessControlException e) {
				// need form relative URL somehow...
			}
		return treeviewIcon;
	}
	public ArrayNameView getArraynameview() {
		return arraynameview;
	}
	public ATRView getAtrview() {
		return atrview;
	}
	public GTRView getGtrview() {
		return gtrview;
	}
	public TextViewManager getTextview() {
		return textview;
	}

	/**
	 * @param initXmap
	 * @param initYmap
	 * @return
	 */
	private PostscriptExportPanel setupPostscriptExport(
			MapContainer initXmap, MapContainer initYmap) {
		PostscriptExportPanel psePanel = new PostscriptExportPanel
		(arraynameview.getHeaderInfo(), 
		getDataModel().getGeneHeaderInfo(),
		getGeneSelection(), getArraySelection(),
		invertedTreeDrawer,
		leftTreeDrawer, arrayDrawer, initXmap, initYmap);
		psePanel.setSourceSet(getDataModel().getFileSet());
		psePanel.setGeneFont(textview.getFont());
		psePanel.setArrayFont(arraynameview.getFont());
		psePanel.setIncludedArrayHeaders(arraynameview.getHeaderSummary().getIncluded());
		psePanel.setIncludedGeneHeaders(textview.getHeaderSummary().getIncluded());
		return psePanel;
	}
	
	private BitmapExportPanel setupBitmapExport(MapContainer initXmap,
			MapContainer initYmap) {
		BitmapExportPanel bitmapPanel = new BitmapExportPanel
		(arraynameview.getHeaderInfo(), 
		getDataModel().getGeneHeaderInfo(),
		getGeneSelection(), getArraySelection(),
		invertedTreeDrawer,
		leftTreeDrawer, arrayDrawer, initXmap, initYmap);
		bitmapPanel.setSourceSet(getDataModel().getFileSet());
		bitmapPanel.setGeneFont(textview.getFont());
		bitmapPanel.setArrayFont(arraynameview.getFont());
		bitmapPanel.setIncludedArrayHeaders(arraynameview.getHeaderSummary().getIncluded());
		bitmapPanel.setIncludedGeneHeaders(textview.getHeaderSummary().getIncluded());
		return bitmapPanel;
	}
	@Override
	public void export(MainProgramArgs mainArgs) throws ExportException {
		DendroviewArgs args = new DendroviewArgs(mainArgs.remainingArgs());
		if (args.getFilePath() == null) {
			System.err.println("Error, must specify an output file\n");
			args.printUsage();
			return;
		}
		final ExportPanel exporter;
		if ("ps".equalsIgnoreCase(args.getExportType())) {
			exporter = setupPostscriptExport(getGlobalXmap(), getGlobalYmap());
		} else if ("png".equalsIgnoreCase(args.getExportType()) || "gif".equalsIgnoreCase(args.getExportType())) {
			exporter = setupBitmapExport(getGlobalXmap(), getGlobalYmap());
		} else {
			System.err.println("Error, unrecognized output format " + args.getExportType()+  " \n");
			args.printUsage();
			exporter = null;
		}
		if (exporter != null) {
			exporter.setFilePath(args.getFilePath());
			exporter.setIncludedArrayHeaders(args.getArrayHeaders());
			exporter.setIncludedGeneHeaders(args.getGeneHeaders());
			if (args.getXScale() != null)
				exporter.setXscale(args.getXScale());
			if (args.getYScale() != null)
				exporter.setYscale(args.getYScale());
			if (args.getContrast() != null)
				colorExtractor.setContrast(args.getContrast());
			if (args.getGtrWidth() != null)
				exporter.setExplicitGtrWidth(args.getGtrWidth());
			if (args.getAtrHeight() != null)
				exporter.setExplicitAtrHeight(args.getAtrHeight());
			if (args.getLogcenter() != null) {
				colorExtractor.setLogCenter(args.getLogcenter());
				colorExtractor.setLogBase(2.0);
				colorExtractor.setLogTransform(true);
			}
			exporter.setArrayAnnoInside(args.getArrayAnnoInside());
			exporter.save();
		}
	}

}
