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
import javax.swing.filechooser.FileNameExtensionFilter;

import net.miginfocom.swing.MigLayout;

import edu.stanford.genetics.treeview.*;
import edu.stanford.genetics.treeview.core.ArrayFinderPanel;
import edu.stanford.genetics.treeview.core.GeneFinderPanel;
import edu.stanford.genetics.treeview.core.HeaderFinderPanel;
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
 * The intention here is that you create this from a model, and never replace 
 * that model. If you want to show another file, make another dendroview. 
 * All views should of course still listen to the model, since that can still 
 * be changed ad libitum.
 *
 * @author     Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.7 $ $Date: 2009-03-23 02:46:51 $
 */
public class DendroView2 extends JPanel implements ConfigNodePersistent, 
MainPanel, Observer {
	
	//Static Variables
	private static final long serialVersionUID = 1L;
	
	private static ImageIcon treeviewIcon = null;
	
	//Instance Variables
	protected final int DIVIDER_SIZE = 5;
	protected final int DIV_LOC = 90;
	
	protected ViewFrame viewFrame;
	
	protected ScrollPane panes[];
	protected boolean loaded;

	//Map Views
    protected GlobalView globalview;
	protected ZoomView zoomview;
	
	//Trees
	protected GTRView gtrview;
	protected ATRView atrview;
//	protected ATRZoomView atrzview;
	
	//Row and Column names
	protected TextViewManager textview;
	protected ArrayNameView arraynameview;

    protected JScrollBar globalXscrollbar;
    protected JScrollBar globalYscrollbar;
    protected JScrollBar zoomXscrollbar;
    protected JScrollBar zoomYscrollbar;
    
    //Drawers
    protected ArrayDrawer arrayDrawer;
	protected InvertedTreeDrawer invertedTreeDrawer;
	protected LeftTreeDrawer leftTreeDrawer;
	
	//MapContainers
	protected MapContainer globalXmap;
	protected MapContainer globalYmap;
	protected MapContainer zoomXmap; 
	protected MapContainer zoomYmap;

	protected MessagePanel statuspanel;
	protected BrowserControl browserControl;
	protected ConfigNode root;
	
	//persistent popups
	protected JDialog settingsFrame;
	protected TabbedSettingsPanel settingsPanel;
	
	/*
	 * The following arrays allow translation to and from screen and DataMatrix 
	 * I had to add these in order to have gaps in the Dendroview of k-means
	 */
	private int [] arrayIndex  = null;
	private int [] geneIndex   = null;
	
	//The model
	private DataModel dataModel = null;
	
	//Selections
	private TreeSelectionI geneSelection = null;
	private TreeSelectionI arraySelection = null;
	
	
	private ColorExtractor colorExtractor;
	private boolean zoomed = false;
	
	//Buttons
	private JButton scaleIncX;
	private JButton scaleIncY;
	private JButton scaleDecX;
	private JButton scaleDecY;
	private JButton scaleDefaultAll;
	
	//JPanels for gene search
	private HeaderFinderPanel geneFinderPanel = null;
	private HeaderFinderPanel arrayFinderPanel = null;
	
	/**
	 * Chained constructor
	 * Calls setName of the JPanel class
	 * @param cols
	 * @param rows
	 * @param name
	 */
	protected DendroView2(int cols, int rows, String name) {
		
		super.setName(name);
	}
	
	/**
	 * Chained constructor for the DendroView object
	 * note this will reuse any existing MainView subnode of the documentconfig.
	 *
	 * @param  tVModel   model this DendroView is to represent
	 * @param  vFrame  parent ViewFrame of DendroView
	 */
	public DendroView2(DataModel tVModel, ViewFrame vFrame) {
		
		this(tVModel, null, vFrame, "Dendrogram");
	}
	
	public DendroView2(DataModel tVModel, ConfigNode root, ViewFrame vFrame) {
		
		this(tVModel, root, vFrame, "Dendrogram");
	}
	
	/**
	 *  Constructor for the DendroView object which 
	 *  binds to an explicit confignode.
	 *
	 * @param  dataModel   model this DendroView is to represent
	 * @param  root   Confignode to which to bind this DendroView
	 * @param  vFrame  parent ViewFrame of DendroView
	 * @param  name name of this view.
	 */
	public DendroView2(DataModel dataModel, ConfigNode root, ViewFrame vFrame, 
			String name) {
		
		super.setName(name);
		this.viewFrame = vFrame;
		this.dataModel = dataModel;

		this.setLayout(new MigLayout("ins 0"));
		
		if (root == null) {
			if (dataModel.getDocumentConfigRoot() != null ) {
				bindConfig(dataModel.getDocumentConfigRoot().fetchOrCreate(
						"MainView"));
				
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
		
		if ((arrayIndex != null) || (geneIndex != null)){
			dataModel = new ReorderedDataModel(
					dataModel, geneIndex, arrayIndex);
		}
		
		setDataModel(dataModel);
		
		setupViews();
		
		if (geneIndex != null) {
			setGeneSelection(new ReorderedTreeSelection(
					viewFrame.getGeneSelection(), geneIndex));
			
		} else {
			setGeneSelection(viewFrame.getGeneSelection());
		}

		if (arrayIndex != null){
			setArraySelection(new ReorderedTreeSelection(
					viewFrame.getArraySelection(), arrayIndex));
			
		} else {
			setArraySelection(viewFrame.getArraySelection());
		}
	}
	
	//Layout
	/**
	 *  This method should be called only during initial setup of the ModelView.
	 *  It sets up the views and binds them all to Config nodes.
	 *
	 */
	protected void setupViews() {
		
		//Reset layout of this frame
		this.removeAll();
		
		ColorPresets colorPresets = DendrogramFactory.getColorPresets();
		colorExtractor = new ColorExtractor();
		colorExtractor.setDefaultColorSet(colorPresets.getDefaultColorSet());
		colorExtractor.setMissing(DataModel.NODATA, DataModel.EMPTY);

		DoubleArrayDrawer dArrayDrawer = new DoubleArrayDrawer();
		dArrayDrawer.setColorExtractor(colorExtractor);
		arrayDrawer = dArrayDrawer;
		((Observable)getDataModel()).addObserver(arrayDrawer);
		
		//Set up status panel
		statuspanel = new MessagePanel("Status", GUIParams.PANEL_BG);
		
		// scrollbars, mostly used by maps
		globalXscrollbar = new JScrollBar(Adjustable.HORIZONTAL, 0,1,0,1);
		globalYscrollbar = new JScrollBar(Adjustable.VERTICAL,0,1,0,1);
		
		zoomXscrollbar = new JScrollBar(Adjustable.HORIZONTAL, 0, 1, 0, 1);
		zoomYscrollbar = new JScrollBar(Adjustable.VERTICAL, 0, 1, 0, 1);

		// globalmaps tell globalview, atrview, and gtrview
		// where to draw each data point.
		// the scrollbars "scroll" by communicating with the maps.
		globalXmap = new MapContainer("Fill");
		globalXmap.setScrollbar(globalXscrollbar);
		
		globalYmap = new MapContainer("Fill");
		globalYmap.setScrollbar(globalYscrollbar);
		
		//Zoom maps to set up data points (colored by arraydrawer)
		zoomXmap = new MapContainer();
		zoomXmap.setDefaultScale(12.0);
		zoomXmap.setScrollbar(zoomXscrollbar);
		
		zoomYmap = new MapContainer();
		zoomYmap.setDefaultScale(12.0);
		zoomYmap.setScrollbar(zoomYscrollbar);
		
		//Create the Global view (JPanel to display)
		globalview = new GlobalView();
		globalview.setXMap(globalXmap);
		globalview.setYMap(globalYmap);
		globalview.setHeaders(getDataModel().getGeneHeaderInfo(), 
				getDataModel().getArrayHeaderInfo());
		
		//Set the Zoom maps for GlobalView 
		globalview.setZoomXMap(zoomXmap);
		globalview.setZoomYMap(zoomYmap);
		
		//Set the drawer for GlobalView
		globalview.setArrayDrawer(arrayDrawer);
		
		//Set up ZoomView
		zoomview = new ZoomView();
		zoomview.setYMap(getZoomYmap());
		zoomview.setXMap(getZoomXmap());
		zoomview.setArrayDrawer(arrayDrawer);

		//Set up the column name display
		arraynameview = new ArrayNameView(getDataModel().getArrayHeaderInfo());
		arraynameview.setUrlExtractor(viewFrame.getArrayUrlExtractor());
		arraynameview.setDataModel(getDataModel());
		
		//Set up row dendrogram
		leftTreeDrawer = new LeftTreeDrawer();
		gtrview = new GTRView();
		gtrview.setOpaque(true);
		gtrview.setMap(globalYmap);
		gtrview.setLeftTreeDrawer(leftTreeDrawer);
		gtrview.getHeaderSummary().setIncluded(new int [] {0,3});
		
		//Set up column dendrogram
		invertedTreeDrawer = new InvertedTreeDrawer();
		atrview = new ATRView();
		atrview.setMap(globalXmap);
		atrview.setInvertedTreeDrawer(invertedTreeDrawer);
		atrview.getHeaderSummary().setIncluded(new int [] {0,3});

//		atrzview = new ATRZoomView();
//		atrzview.setZoomMap(getZoomXmap());
//		atrzview.setHeaderSummary(atrview.getHeaderSummary());
//		atrzview.setInvertedTreeDrawer(invertedTreeDrawer);
		
		scaleDefaultAll = setZoomButtonLayout("Reset");
		scaleDefaultAll.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				if(zoomed) {
					getZoomXmap().setHome();
					getZoomYmap().setHome();
					
				} else {
					getGlobalXmap().setHome();
					getGlobalYmap().setHome();
				}
			}
		});
		
		scaleIncX = setZoomButtonLayout("+");
		scaleIncX.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				if(zoomed) {
					getZoomXmap().zoomIn();
				
				} else {
					getGlobalXmap().zoomIn();
				}
			}
		});
		
		scaleDecX = setZoomButtonLayout("-");
		scaleDecX.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
		
				if(zoomed) {
					getZoomXmap().zoomOut();
					
				} else {
					getGlobalXmap().zoomOut();
				}
			
			}
		});
		
		scaleIncY = setZoomButtonLayout("+");
		scaleIncY.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				if(zoomed) {
					getZoomYmap().zoomIn();
					
				} else {
					getGlobalYmap().zoomIn();
				}
			}	
		});
		
		scaleDecY = setZoomButtonLayout("-");
		scaleDecY.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				if(zoomed) {
					getZoomYmap().zoomOut();
					
				} else {
					getGlobalYmap().zoomOut();
				}
			}
		});

		arraynameview.setMapping(getGlobalXmap());

		textview = new TextViewManager(getDataModel().getGeneHeaderInfo(), 
				viewFrame.getUrlExtractor(), getDataModel());
		
		//textview.setMap(getZoomYmap());
		textview.setMap(getGlobalYmap());
		
		//Layout Setup
		doDoubleLayout();

		//reset persistent popups
		settingsFrame = null;
		settingsPanel = null;

		// urls
		colorExtractor.bindConfig(getFirst("ColorExtractor"));
		
		// set data first to avoid adding auto-genereated 
		//contrast to documentConfig.
		dArrayDrawer.setDataMatrix(getDataModel().getDataMatrix());
		dArrayDrawer.bindConfig(getFirst("ArrayDrawer"));

		// this is here because my only subclass shares this code.
		bindTrees();
		
		//Changes Row/ Col number to gene names in Status panel
		zoomview.setHeaders(getDataModel().getGeneHeaderInfo(), 
				getDataModel().getArrayHeaderInfo());
		
		globalXmap.bindConfig(getFirst("GlobalXMap"));
		globalYmap.bindConfig(getFirst("GlobalYMap"));
		getZoomXmap().bindConfig(getFirst("ZoomXMap"));
		getZoomYmap().bindConfig(getFirst("ZoomYMap"));

		textview.bindConfig(getFirst("TextView"));			
		
		arraynameview.bindConfig(getFirst("ArrayNameView"));
		
		HeaderSummary atrSummary = atrview.getHeaderSummary();
		
//		atrzview.setHeaderSummary(atrSummary);
		
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
	 * Lays out components in two DragGridPanel separated by a
	 * JSplitPane, so that you can expand/contract with one click.
	 *
	 */
	protected void doDoubleLayout() {
		
		JPanel backgroundPanel;
		JPanel buttonPanel;
		JPanel crossPanel;
		final JPanel panel; 
		JPanel zoompanel;
		JPanel textpanel;
		JPanel navPanel;
		JSplitPane gtrPane;
		JSplitPane atrPane;
//		JButton saveButton;
		JButton closeButton;
		final JButton zoomButton;
//		JButton fullScreenButton;
		
		this.removeAll();
		
		backgroundPanel = new JPanel();
		backgroundPanel.setLayout(new MigLayout());
		backgroundPanel.setBackground(GUIParams.BG_COLOR);
		
		gtrview.setStatusPanel(statuspanel);
		gtrview.setViewFrame(viewFrame);
		
		//ButtonPanel
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new MigLayout());
		buttonPanel.setOpaque(false);
		
		crossPanel = new JPanel();
		crossPanel.setLayout(new MigLayout());
		crossPanel.setOpaque(false);
		
		//Global view
		panel = new JPanel();
		panel.setLayout(new MigLayout("ins 0"));
		panel.setOpaque(false);
		
//		saveButton = setButtonLayout("Save Zoomed Image", BLUE1);
//		saveButton.addActionListener(new ActionListener(){
//
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				
//				try {
//					saveImage(zoomview);
//					
//				} catch (IOException e) {
//					
//				}	
//			}
//		});
		
		closeButton = setButtonLayout("< Back");;
  		closeButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				viewFrame.setLoaded(false);
			}
		});
  		
		zoomButton = setButtonLayout("Zoom Selection");;
  		zoomButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				if(zoomed) {
					arraynameview.setMapping(getGlobalXmap());
					textview.setMap(getGlobalYmap());
					
					panel.removeAll();
					panel.add(globalview, "grow, push");	
					panel.add(globalYscrollbar, "pushy, growy, wrap");
					panel.add(globalXscrollbar, "pushx, growx, span");
					
					panel.revalidate();
					panel.repaint();
					
					zoomed = false;
					zoomButton.setText("Zoom Map");
					
				} else {
					arraynameview.setMapping(getZoomXmap());
					textview.setMap(getZoomYmap());
					
					panel.removeAll();
					panel.add(zoomview, "grow, push");	
					panel.add(zoomYscrollbar, "pushy, growy, wrap");
					panel.add(zoomXscrollbar, "pushx, growx, span");
					
					panel.revalidate();
					panel.repaint();
					
					zoomed = true;
					zoomButton.setText("Global Map");
				}
			}
		});
  		
		navPanel = new JPanel();
		navPanel.setLayout(new MigLayout());
		navPanel.setBackground(GUIParams.PANEL_BG);
		navPanel.setBorder(BorderFactory.createEtchedBorder());
		
		textpanel = new JPanel();
		textpanel.setLayout(new MigLayout("ins 0"));
		textpanel.setOpaque(false);
		
		gtrPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				gtrview, textpanel);
		gtrPane.setDividerSize(DIVIDER_SIZE);
		gtrPane.setDividerLocation(DIV_LOC);
		gtrPane.setOpaque(false);
		gtrPane.setBorder(null);
		
		atrPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				atrview, arraynameview);
		atrPane.setDividerSize(DIVIDER_SIZE);
		atrPane.setDividerLocation(DIV_LOC);
		atrPane.setOpaque(false);
		atrPane.setBorder(null);
		
		//Register Views
		registerView(globalview);
		registerView(atrview);
		registerView(arraynameview);
		registerView(zoomview);
		registerView(textview);
//		registerView(atrzview);
		
		panel.add(globalview, "grow, push");	
		panel.add(globalYscrollbar, "pushy, growy, wrap");
		panel.add(globalXscrollbar, "pushx, growx, span");
		
		zoompanel = new JPanel();
		zoompanel.setLayout(new MigLayout());
		zoompanel.add(zoomview, BorderLayout.CENTER);
		zoompanel.add(zoomXscrollbar, BorderLayout.SOUTH);	
		zoompanel.add(zoomYscrollbar, BorderLayout.EAST);
		
		textpanel.add(textview.getComponent(), "push, grow");
		
		JLabel adjScale = new JLabel("Adjust Scale:");
		adjScale.setFont(GUIParams.FONTS);
		adjScale.setForeground(GUIParams.TEXT);
		
		crossPanel.add(scaleIncY, "span, alignx 50%, wrap");
		crossPanel.add(scaleDecX);
		crossPanel.add(scaleDefaultAll);
		crossPanel.add(scaleIncX, "wrap");
		crossPanel.add(scaleDecY, "span, alignx 50%");
		
		buttonPanel.add(adjScale, "pushx, wrap");
		buttonPanel.add(crossPanel, "pushx, alignx 50%");
		
		navPanel.add(getGeneFinderPanel(), "pushx, alignx 50%, " +
				"height 10%::, wrap");
		navPanel.add(getArrayFinderPanel(), "pushx, alignx 50%, " +
				"height 10%::, wrap");
		navPanel.add(buttonPanel, "push, growx, alignx 50%, wrap");
		navPanel.add(zoomButton, "push, alignx 50%, wrap");
		navPanel.add(closeButton, "push, alignx 50%");
		
		backgroundPanel.add(statuspanel, "pushx, growx, height 20%::");
		backgroundPanel.add(atrPane, "grow, push, width 62%, height 20%::");
		backgroundPanel.add(navPanel, "span 1 3, grow, push, width 13%, wrap");
		backgroundPanel.add(gtrPane, "grow, width 20%");
		backgroundPanel.add(panel, "grow, push, width 62%, height 80%");

		this.add(backgroundPanel, "push, grow");
		
		this.revalidate();
		this.repaint();
	}
	
	@Override
	public void refresh() {
		
		doDoubleLayout();
	}
	
	//Methods
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
	
	/**
	 *  Gets the contrast attribute of the DendroView object
	 *
	 * @return    The contrast value
	public double getContrast() {
		return colorExtractor.getContrast();
	}
	 */


	/**
	 * Finds the currently selected genes, mirror image flips them,
	 * and then rebuilds all necessary trees and saved data to the .jtv file.
	 *
	 */
	private void flipSelectedGTRNode() {
		
		int leftIndex, rightIndex;
		String selectedID;
		TreeDrawerNode geneNode = leftTreeDrawer.getNodeById(
				getGeneSelection().getSelectedNode());
		
		if(geneNode == null || geneNode.isLeaf()) {
			
			return;
		}
		
		selectedID = geneNode.getId();
	
		//find the starting index of the left array tree, the ending 
		//index of the right array tree
		leftIndex = getDataModel().getGeneHeaderInfo().getHeaderIndex(
				geneNode.getLeft().getLeftLeaf().getId());
		rightIndex = getDataModel().getGeneHeaderInfo().getHeaderIndex(
				geneNode.getRight().getRightLeaf().getId());
		
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
		
		for(int i = 0; i < num; i++) {
			
			newOrder[i] = i;
		}
		
		for(int i = 0; i <= (rightIndex - leftIndex); i++) {
			
			newOrder[leftIndex + i] = rightIndex - i;
		}
		
		return newOrder;
	}

	/**
	 * Finds the currently selected arrays, mirror image flips them, 
	 * and then rebuilds all necessary trees and saved data to the .jtv file.
	 */
	private void flipSelectedATRNode() {
		
		int leftIndex, rightIndex;
		String selectedID;
		TreeDrawerNode arrayNode = invertedTreeDrawer.getNodeById(
				getArraySelection().getSelectedNode());
		
		if(arrayNode == null || arrayNode.isLeaf()) {
			
			return;
		}
		
		selectedID = arrayNode.getId();
	
		//find the starting index of the left array tree, 
		//the ending index of the right array tree
		leftIndex = getDataModel().getArrayHeaderInfo().getHeaderIndex(
				arrayNode.getLeft().getLeftLeaf().getId());
		rightIndex = getDataModel().getArrayHeaderInfo().getHeaderIndex(
				arrayNode.getRight().getRightLeaf().getId());
		
		int num = getDataModel().getDataMatrix().getNumUnappendedCol();
		
		int [] newOrder = new int[num];
		
		for(int i = 0; i < num; i++) {
			
			newOrder[i] = i;
		}
		
		for(int i = 0; i <= (rightIndex - leftIndex); i++) {
			
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
	public void compareToModel(TVModel model) {
		
		getDataModel().removeAppended();
		getDataModel().append(model);
		
		arraySelection.resize(getDataModel().getDataMatrix().getNumCol());
		arraySelection.notifyObservers();
		
		globalXmap.setIndexRange(0, 
				getDataModel().getDataMatrix().getNumCol() - 1);
		globalXmap.notifyObservers();
		
//		zoomXmap.setIndexRange(0, 
//				getDataModel().getDataMatrix().getNumCol() - 1);
//		zoomXmap.notifyObservers();
		
		((Observable)getDataModel()).notifyObservers();
	}
	
	/**
	 * Aligns the current ATR to the passed model as best as possible, 
	 * saves the new ordering to the .jtv file.
	 * @param model - AtrTVModel with which to align.
	 */
	public void alignAtrToModel(AtrTVModel model) {
		
		try {	
			String selectedID = null;
			
			try {
				selectedID = getArraySelection().getSelectedNode();
				
			} catch(NullPointerException npe) {
				npe.printStackTrace();
			}
			
			int [] ordering;
			ordering = AtrAligner.align(getDataModel().getAtrHeaderInfo(), 
					getDataModel().getArrayHeaderInfo(),
					model.getAtrHeaderInfo(), model.getArrayHeaderInfo());
								 
			/*System.out.print("New ordering: ");
			for(int i = 0; i < ordering.length; i++)
			{
				System.out.print(ordering[i] + " ");
			}			
			System.out.println();*/
			
			
			((TVModel)getDataModel()).reorderArrays(ordering);
			((Observable)getDataModel()).notifyObservers();
			
			if(selectedID != null) {
				
				updateATRDrawer(selectedID);
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println(e.toString());
		}
	}
	
	
	/**
	 * Updates the GTRDrawer to reflect changes in the DataModel gene order; 
	 * rebuilds the TreeDrawerNode tree.
	 * 
	 * @param selectedID ID of the node selected before a 
	 * change in tree structure was made. This node is then 
	 * found and reselected after the ATR tree is rebuilt.
	 */
	private void updateGTRDrawer(String selectedID) {
		
		try {
			TVModel tvmodel = (TVModel)getDataModel();
			leftTreeDrawer.setData(tvmodel.getGtrHeaderInfo(), 
					tvmodel.getGeneHeaderInfo());
			
			HeaderInfo trHeaderInfo = tvmodel.getGtrHeaderInfo();
			
			if (trHeaderInfo.getIndex("NODECOLOR") >= 0) {
				TreeColorer.colorUsingHeader (leftTreeDrawer.getRootNode(),
				trHeaderInfo,
				trHeaderInfo.getIndex("NODECOLOR"));
			}	
		} catch (DendroException e) {
			
			//LogPanel.println("Had problem setting up the array tree : " 
			//+ e.getMessage());
			//e.printStackTrace();
			Box mismatch = new Box(BoxLayout.Y_AXIS); mismatch.add(
					new JLabel(e.getMessage()));
			mismatch.add(new JLabel("Perhaps there is a mismatch " +
					"between your ATR and CDT files?"));
			mismatch.add(new JLabel("Ditching Gene Tree, since it's lame."));
			
			JOptionPane.showMessageDialog(viewFrame, mismatch, 
					"Tree Construction Error", JOptionPane.ERROR_MESSAGE);
			
			gtrview.setEnabled(false);
//			gtrzview.setEnabled(false);
			
			try{	
				leftTreeDrawer.setData(null, null);
				
			} catch (DendroException ex) {
				
			}
		}
		
		TreeDrawerNode arrayNode = 
				leftTreeDrawer.getRootNode().findNode(selectedID);
		
		geneSelection.setSelectedNode(arrayNode.getId());
		gtrview.setSelectedNode(arrayNode);
//		gtrzview.setSelectedNode(arrayNode);
		
		geneSelection.notifyObservers();
		leftTreeDrawer.notifyObservers();
	}

	/**
	 * Updates the ATRDrawer to reflect changes in the DataMode array order; 
	 * rebuilds the TreeDrawerNode tree.
	 * @param selectedID ID of the node selected before a change 
	 * in tree structure was made. This node is then found and 
	 * reselected after the ATR tree is rebuilt.
	 */
	private void updateATRDrawer(String selectedID) {
		
		try {
			TVModel tvmodel = (TVModel)getDataModel();
			invertedTreeDrawer.setData(tvmodel.getAtrHeaderInfo(), 
					tvmodel.getArrayHeaderInfo());
			HeaderInfo trHeaderInfo = tvmodel.getAtrHeaderInfo();
			
			if (trHeaderInfo.getIndex("NODECOLOR") >= 0) {
				
				TreeColorer.colorUsingHeader(invertedTreeDrawer.getRootNode(),
				trHeaderInfo,
				trHeaderInfo.getIndex("NODECOLOR"));
			}	
		} catch (DendroException e) {
			
			//LogPanel.println("Had problem setting up the array tree : " 
			//+ e.getMessage());
			//e.printStackTrace();
			Box mismatch = new Box(BoxLayout.Y_AXIS); mismatch.add(
					new JLabel(e.getMessage()));
			mismatch.add(new JLabel("Perhaps there is a mismatch " +
					"between your ATR and CDT files?"));
			mismatch.add(new JLabel("Ditching Array Tree, since it's lame."));
			
			JOptionPane.showMessageDialog(viewFrame, mismatch, 
					"Tree Construction Error", JOptionPane.ERROR_MESSAGE);
			
			atrview.setEnabled(false);
//			atrzview.setEnabled(false);
			
			try{
				invertedTreeDrawer.setData(null, null);
				
			} catch (DendroException ex) {
				
			}
		}
		
		TreeDrawerNode arrayNode =
				invertedTreeDrawer.getRootNode().findNode(selectedID);
		
		arraySelection.setSelectedNode(arrayNode.getId());
//		atrzview.setSelectedNode(arrayNode);
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
	* Open a dialog which allows the user to select 
	* a new CDT data file for tree alignment.
	*
	* @return The fileset corresponding to the dataset.
	*/
	/*
	 *  Unknow what actually happens if the file CDT does not 
	 *  have an associated ATR.
	 */
	protected FileSet offerATRFileSelection() throws LoadException {
		
		FileSet fileSet1; // will be chosen...
	 
		JFileChooser fileDialog = new JFileChooser();
		setupATRFileDialog(fileDialog);
		
		int retVal = fileDialog.showOpenDialog(this);
		
		if(retVal == JFileChooser.APPROVE_OPTION) {
			File chosen = fileDialog.getSelectedFile();
			fileSet1 = new FileSet(chosen.getName(), 
					chosen.getParent() + File.separator);
			
		} else {
			throw new LoadException("File Dialog closed without selection...", 
					LoadException.NOFILE);
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
			fileDialog.addChoosableFileFilter(
					new javax.swing.filechooser.FileFilter() {
				
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
//				atrzview.setEnabled(true);
				
				invertedTreeDrawer.setData(tvmodel.getAtrHeaderInfo(), 
						tvmodel.getArrayHeaderInfo());
				HeaderInfo trHeaderInfo = tvmodel.getAtrHeaderInfo();
				
				if (trHeaderInfo.getIndex("NODECOLOR") >= 0) {
					TreeColorer.colorUsingHeader (
							invertedTreeDrawer.getRootNode(),
					trHeaderInfo,
					trHeaderInfo.getIndex("NODECOLOR"));
				}	
				
			} catch (DendroException e) {
				//LogPanel.println("Had problem setting up the array tree : " 
				//+ e.getMessage());
				//e.printStackTrace();
				Box mismatch = new Box(BoxLayout.Y_AXIS); mismatch.add(
						new JLabel(e.getMessage()));
				mismatch.add(new JLabel("Perhaps there is a mismatch " +
						"between your ATR and CDT files?"));
				mismatch.add(new JLabel("Ditching Array Tree, " +
						"since it's lame."));
				
				JOptionPane.showMessageDialog(viewFrame, mismatch, 
						"Tree Construction Error", JOptionPane.ERROR_MESSAGE);
				
				atrview.setEnabled(false);
//				atrzview.setEnabled(false);
				
				try{
					invertedTreeDrawer.setData(null, null);
					
				} catch (DendroException ex) {
					
				}
			}
		} else {
			atrview.setEnabled(false);
//			atrzview.setEnabled(false);
		
			try{
				invertedTreeDrawer.setData(null, null);
				
			} catch (DendroException ex) {
				
			}
		}
		
		invertedTreeDrawer.notifyObservers();

		if ((tvmodel != null) && tvmodel.gidFound()) {
			try {
				gtrview.setEnabled(true);
//				gtrzview.setEnabled(true);
				
				leftTreeDrawer.setData(tvmodel.getGtrHeaderInfo(), 
						tvmodel.getGeneHeaderInfo());
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
				
			} catch (DendroException e) {
//				LogPanel.println("Had problem setting up the gene tree : 
//				" + e.getMessage());
//				e.printStackTrace();
				Box mismatch = new Box(BoxLayout.Y_AXIS); mismatch.add(
						new JLabel(e.getMessage()));
				mismatch.add(new JLabel("Perhaps there is a mismatch " +
						"between your GTR and CDT files?"));
				mismatch.add(new JLabel("Ditching Gene Tree, " +
						"since it's lame."));
				
				JOptionPane.showMessageDialog(viewFrame, mismatch, 
						"Tree Construction Error", JOptionPane.ERROR_MESSAGE);
				
				gtrview.setEnabled(false);
//				gtrzview.setEnabled(false);
				
				try{
					leftTreeDrawer.setData(null, null);
					
				} catch (DendroException ex) {
					
				}
			}
		} else {
			gtrview.setEnabled(false);
//			gtrzview.setEnabled(false);
			
			try{
				leftTreeDrawer.setData(null, null);
				
			} catch (DendroException ex) {
				
			}
		}
		
		leftTreeDrawer.notifyObservers();
	}

	/**
	 *  registers a modelview with the hint and status panels, 
	 *  and the viewFrame.
	 *
	 * @param  modelView  The ModelView to be added
	 */
	private void registerView(ModelView modelView) {
		
		modelView.setStatusPanel(statuspanel);
		modelView.setViewFrame(viewFrame);
	}

	// Menus
	@Override
	public void populateExportMenu(TreeviewMenuBarI menu) {
		
		menu.addMenuItem("Export to Postscript...", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				
				MapContainer initXmap, initYmap;
				
//				if ((getArraySelection().getNSelectedIndexes() != 0) || 
//						(getGeneSelection().getNSelectedIndexes() != 0)) {
//					 initXmap = getZoomXmap();
//					 initYmap = getZoomYmap();
//					 
//				} else {
					initXmap = getGlobalXmap();
					initYmap = getGlobalYmap();
//				}
				
				PostscriptExportPanel psePanel = 
						setupPostscriptExport(initXmap, initYmap);
				
				final JDialog popup = new CancelableSettingsDialog(viewFrame, 
						"Export to Postscript", psePanel);
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
//				if ((getArraySelection().getNSelectedIndexes() != 0) || 
//						(getGeneSelection().getNSelectedIndexes() != 0)) {
//					initXmap = getZoomXmap();
//					initYmap = getZoomYmap();
//					
//				} else {
					initXmap = getGlobalXmap();
					initYmap = getGlobalYmap();
//				}
				
				BitmapExportPanel bitmapPanel = setupBitmapExport(initXmap, 
						initYmap);

				final JDialog popup = new CancelableSettingsDialog(viewFrame, 
						"Export to Image", bitmapPanel);
				popup.pack();
				popup.setVisible(true);
		  }
		});
		menu.setMnemonic(KeyEvent.VK_I);

		menu.addMenuItem("Export ColorBar to Postscript...", 
				new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				
				PostscriptColorBarExportPanel gcbPanel = 
						new PostscriptColorBarExportPanel(
								((DoubleArrayDrawer) arrayDrawer)
								.getColorExtractor());
				
				gcbPanel.setSourceSet(getDataModel().getFileSet());

				final JDialog popup = new CancelableSettingsDialog(viewFrame, 
						"Export ColorBar to Postscript", gcbPanel);
				popup.pack();
				popup.setVisible(true);
			}
		});
		menu.setMnemonic(KeyEvent.VK_B);

		menu.addMenuItem("Export ColorBar to Image...",new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				
				BitmapColorBarExportPanel gcbPanel = 
						new BitmapColorBarExportPanel(
								((DoubleArrayDrawer) arrayDrawer)
								.getColorExtractor());
				
				gcbPanel.setSourceSet(getDataModel().getFileSet());

			final JDialog popup = new CancelableSettingsDialog(viewFrame, 
					"Export ColorBar to Image", gcbPanel);
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
					
					final JDialog popup = 
							new CancelableSettingsDialog(viewFrame, 
									"Export to Image", bitmapPanel);
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
					
					final JDialog popup = 
							new CancelableSettingsDialog(viewFrame, 
									"Export To Image", bitmapPanel);
					popup.pack();
					popup.setVisible(true);
				}
			});
			menu.setMnemonic(KeyEvent.VK_H);
			
//			menu.addMenuItem("Save Zoomed Image", new ActionListener() {
//				
//				@Override
//				public void actionPerformed(ActionEvent actionEvent) {
//					
//					MapContainer initXmap, initYmap;
//					initXmap = getZoomXmap();
//					initYmap = getZoomYmap();
//					
//					BitmapExportPanel bitmapPanel = new BitmapExportPanel
//					(arraynameview.getHeaderInfo(), 
//					getDataModel().getGeneHeaderInfo(),
//					getGeneSelection(), getArraySelection(),
//					invertedTreeDrawer,
//					leftTreeDrawer, arrayDrawer, initXmap, initYmap);
//					
//					bitmapPanel.setSourceSet(getDataModel().getFileSet());
//					bitmapPanel.setGeneFont(textview.getFont());
//					bitmapPanel.setArrayFont(arraynameview.getFont());
//					
//					bitmapPanel.includeGtr(false);
//					bitmapPanel.includeAtr(false);
//					bitmapPanel.deselectHeaders();
//					
//					final JDialog popup = 
//							new CancelableSettingsDialog(viewFrame, 
//									"Export To Image", bitmapPanel);
//					popup.pack();
//					popup.setVisible(true);
//				}
//			});
//			menu.setMnemonic(KeyEvent.VK_Z);
		}
	  
	  
	/**
	 * show summary of the specified indexes
	 */
	public void showSubDataModel(int [] indexes) {
		
		getViewFrame().showSubDataModel(indexes, null, null);
	}

	//Populate Menus
	/**
	 *  adds DendroView stuff to Analysis menu
	 *
	 * @param  menu  menu to add to
	 */
	@Override
	public void populateAnalysisMenu(TreeviewMenuBarI menu) {
		
		menu.addMenuItem("Flip Array Tree Node", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent ae) {
				
				if (getGtrview().hasFocus()) {
					
					flipSelectedGTRNode();
				}
				else {
					
					flipSelectedATRNode();
				}
			}
		});
		menu.setAccelerator(KeyEvent.VK_L);
		menu.setMnemonic(KeyEvent.VK_A);

		menu.addMenuItem("Flip Gene Tree Node", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent ae) {
				
				flipSelectedGTRNode();
			}
		});
		menu.setMnemonic(KeyEvent.VK_G);
		
		menu.addMenuItem("Align to Tree...", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent ae) {
				
				try {
					
					FileSet fileSet = offerATRFileSelection();
					AtrTVModel atrModel = makeAtrModel(fileSet);
					
					alignAtrToModel(atrModel);
				}
				catch (LoadException e) {
					
					if ((e.getType() != LoadException.INTPARSE) &&
						(e.getType() != LoadException.NOFILE)) {
						LogBuffer.println("Could not open file: " 
						+ e.getMessage());
						e.printStackTrace();
					}
				}
			}			
		});
		menu.setAccelerator(KeyEvent.VK_A);
		menu.setMnemonic(KeyEvent.VK_G);
		
		menu.addMenuItem("Compare to...", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent ae) {
				
				try {
					
					FileSet fileSet = offerATRFileSelection();
					TVModel tvModel = makeCdtModel(fileSet);
					compareToModel(tvModel);						
				}
				catch (LoadException e){
					
					if ((e.getType() != LoadException.INTPARSE) &&
						(e.getType() != LoadException.NOFILE)) {
						LogBuffer.println("Could not open file: " 
						+ e.getMessage());
						e.printStackTrace();
					}
				}
			}
		});
		menu.setAccelerator(KeyEvent.VK_C);
		menu.setMnemonic(KeyEvent.VK_C);
		
		menu.addMenuItem("Remove comparison", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent ae) {
				
				getDataModel().removeAppended();
				globalXmap.setIndexRange(0, 
						getDataModel().getDataMatrix().getNumCol() - 1);
				globalXmap.notifyObservers();
				
//				zoomXmap.setIndexRange(0, 
//						getDataModel().getDataMatrix().getNumCol() - 1);
//				zoomXmap.notifyObservers();
				
				((Observable)getDataModel()).notifyObservers();
			}
		});
		menu.setAccelerator(KeyEvent.VK_R);
		menu.setMnemonic(KeyEvent.VK_R);		

//		menu.addMenuItem("Summary Window...",new ActionListener() {
//		  public void actionPerformed(ActionEvent e) {
//			  SummaryViewWizard  wizard = 
//		new SummaryViewWizard(DendroView2.this);
//			  int retval = JOptionPane.showConfirmDialog(DendroView2.this, 
//		wizard, "Configure Summary", JOptionPane.OK_CANCEL_OPTION);
//			  if (retval == JOptionPane.OK_OPTION) {
//			  	showSubDataModel(wizard.getIndexes());
//			  }
//		  }
//		});
//		menu.setMnemonic(KeyEvent.VK_S);
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
	
//				PixelSettingsSelector pssSelector = 
//						new PixelSettingsSelector(globalXmap, globalYmap,
//							getZoomXmap(), getZoomYmap(), 
//							ce, DendrogramFactory.getColorPresets());
				
				PixelSettingsSelector pssSelector = 
						new PixelSettingsSelector(globalXmap, globalYmap, ce, 
								DendrogramFactory.getColorPresets());

				JDialog popup = new ModelessSettingsDialog(viewFrame, 
						"Pixel Settings", pssSelector);
			 
				System.out.println("showing popup...");
			 	popup.addWindowListener(XmlConfig.getStoreOnWindowClose(
			 			getDataModel().getDocumentConfigRoot()));	
			 	
				popup.pack();
				popup.setVisible(true);
			}
		}, 0);
		menu.setMnemonic(KeyEvent.VK_X);

		menu.addMenuItem("Url Settings...", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				
				// keep refs to settingsPanel, settingsFrame local, 
				//since will dispose of self when closed...
				TabbedSettingsPanel settingsPanel = new TabbedSettingsPanel();
				
				UrlSettingsPanel genePanel = new UrlSettingsPanel(
						viewFrame.getUrlExtractor(), 
						viewFrame.getGeneUrlPresets());
				settingsPanel.addSettingsPanel("Gene", genePanel);
				
				UrlSettingsPanel arrayPanel = new UrlSettingsPanel(
						viewFrame.getArrayUrlExtractor(), 
						viewFrame.getArrayUrlPresets());
				settingsPanel.addSettingsPanel("Array", arrayPanel);
				
	
				JDialog settingsFrame = new ModelessSettingsDialog(viewFrame, 
						"Url Settings", settingsPanel); 
	
				settingsFrame.addWindowListener(
						XmlConfig.getStoreOnWindowClose(getDataModel()
								.getDocumentConfigRoot()));
				settingsFrame.pack();
				settingsFrame.setVisible(true);
			}
		}, 0);
		menu.setMnemonic(KeyEvent.VK_U);
	
		menu.addMenuItem("Font Settings...", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				
				// keep ref to settingsFrame local, 
				//since will dispose of self when closed...
				TabbedSettingsPanel settingsPanel = new TabbedSettingsPanel();
				
				FontSettingsPanel genePanel = new FontSettingsPanel(textview);
				settingsPanel.addSettingsPanel("Gene", genePanel);
				
				FontSettingsPanel arrayPanel = 
						new FontSettingsPanel(arraynameview);
				settingsPanel.addSettingsPanel("Array", arrayPanel);
				
				JDialog settingsFrame = new ModelessSettingsDialog(viewFrame, 
						"Font Settings", settingsPanel); 
				settingsFrame.addWindowListener(
						XmlConfig.getStoreOnWindowClose(getDataModel()
								.getDocumentConfigRoot()));
				settingsFrame.pack();
				settingsFrame.setVisible(true);
			}
		}, 0);
		menu.setMnemonic(KeyEvent.VK_F);
	
		menu.addMenuItem("Annotations...", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				// keep refs to settingsPanel, settingsFrame local, 
				//since will dispose of self when closed...
				TabbedSettingsPanel settingsPanel = new TabbedSettingsPanel();
				
				HeaderSummaryPanel genePanel = new HeaderSummaryPanel(
						getDataModel().getGeneHeaderInfo(), 
						textview.getHeaderSummary());
				settingsPanel.addSettingsPanel("Gene", genePanel);
				
				HeaderSummaryPanel arrayPanel = new HeaderSummaryPanel(
						arraynameview.getHeaderInfo(), 
						arraynameview.getHeaderSummary());
				settingsPanel.addSettingsPanel("Array", arrayPanel);
				
				HeaderSummaryPanel atrPanel = new HeaderSummaryPanel(
						getDataModel().getAtrHeaderInfo(), 
						atrview.getHeaderSummary());
				settingsPanel.addSettingsPanel("Array Tree", atrPanel);
				
				HeaderSummaryPanel gtrPanel = new HeaderSummaryPanel(
						getDataModel().getGtrHeaderInfo(), 
						gtrview.getHeaderSummary());
				settingsPanel.addSettingsPanel("Gene Tree", gtrPanel);
				
	
				JDialog settingsFrame = new ModelessSettingsDialog(viewFrame, 
						"Annotation Settings", settingsPanel); 
	
				settingsFrame.addWindowListener(
						XmlConfig.getStoreOnWindowClose(getDataModel()
								.getDocumentConfigRoot()));
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
				  UrlEditor urlEditor = new UrlEditor(urlExtractor, 
				  viewFrame.getGeneUrlPresets(), dataModel.getGeneHeaderInfo()); 
				  urlEditor.showConfig(viewFrame);
				  dataModel.getDocumentConfig().store();
				}
			});
		menu.add(urlItem);
	*/
	}

	/**
	 * Alok: this function changes the info in the ConfigNode to match 
	 * the current panel sizes. this is a hack, since I don't know 
	 * how to intercept panel resizing.
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
				n.setAttribute("value", 
				(double) widths[i], 1.0 / widths.length);
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
				heights[i] = (float) heightNodes[i].getAttribute(
				"value", 1.0 / heights.length);
			}
			for (int j = 0; j < widths.length; j++) {
				widths[j] = (float) widthNodes[j].getAttribute(
				"value", 1.0 / widths.length);
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
		
		File saveFile = new File("savedImage.png");
		
		final JFileChooser fc = new JFileChooser();
		
		fc.setFileFilter(new FileNameExtensionFilter("PNG", "png"));
		fc.setSelectedFile(saveFile);
		int returnVal = fc.showSaveDialog(this);
		
		if(returnVal == JFileChooser.APPROVE_OPTION){
			saveFile = fc.getSelectedFile();
			
			String fileName = saveFile.toString();
			
			if(!fileName.endsWith(".png")){
				fileName += ".png";
				saveFile = new File(fileName);
			}
			
			BufferedImage im = new BufferedImage(panel.getWidth(), 
					panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
			
			panel.paint(im.getGraphics());
			ImageIO.write(im, "PNG", saveFile);
		}	
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
		psePanel.setIncludedArrayHeaders(
				arraynameview.getHeaderSummary().getIncluded());
		psePanel.setIncludedGeneHeaders(
				textview.getHeaderSummary().getIncluded());
		
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
		bitmapPanel.setIncludedArrayHeaders(
				arraynameview.getHeaderSummary().getIncluded());
		bitmapPanel.setIncludedGeneHeaders(
				textview.getHeaderSummary().getIncluded());
		
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
			
		} else if ("png".equalsIgnoreCase(args.getExportType()) || 
				"gif".equalsIgnoreCase(args.getExportType())) {
			exporter = setupBitmapExport(getGlobalXmap(), getGlobalYmap());
			
		} else {
			System.err.println("Error, unrecognized output format " 
			+ args.getExportType()+  " \n");
			
			args.printUsage();
			exporter = null;
		}
		
		if (exporter != null) {
			exporter.setFilePath(args.getFilePath());
			exporter.setIncludedArrayHeaders(args.getArrayHeaders());
			exporter.setIncludedGeneHeaders(args.getGeneHeaders());
			
			if (args.getXScale() != null) {
				exporter.setXscale(args.getXScale());
			}
			
			if (args.getYScale() != null) {
				exporter.setYscale(args.getYScale());
			}
			
			if (args.getContrast() != null){
				colorExtractor.setContrast(args.getContrast());
			}
			
			if (args.getGtrWidth() != null) {
				exporter.setExplicitGtrWidth(args.getGtrWidth());
			}
			
			if (args.getAtrHeight() != null){
				exporter.setExplicitAtrHeight(args.getAtrHeight());
			}
			
			if (args.getLogcenter() != null) {
				colorExtractor.setLogCenter(args.getLogcenter());
				colorExtractor.setLogBase(2.0);
				colorExtractor.setLogTransform(true);
			}
			
			exporter.setArrayAnnoInside(args.getArrayAnnoInside());
			exporter.save();
		}
	}
	
	
	//Getters
	/**
	 * Always returns an instance of the node, even if it has to create it.
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
	 * Getter for root 
	 */
	@Override
	public ConfigNode getConfigNode() {
		
		return root;
	}
	
	/**
	 * Icon for display in tabbed panel
	 */
	@Override
	public ImageIcon getIcon() {
		
		if (treeviewIcon == null)
			try {
				treeviewIcon = new ImageIcon("images/treeview.gif", 
						"TreeView Icon");
				
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
	 *  Gets the globalXmap attribute of the DendroView object
	 * @return globalXmap
	 */
	public MapContainer getGlobalXmap() {
		
		return globalXmap;
	}

	/**
	 *  Gets the globalYmap attribute of the DendroView object
	 * @return    The globalYmap
	 */
	public MapContainer getGlobalYmap() {
		
		return globalYmap;
	}

	/**
	 *  Gets the zoomXmap attribute of the DendroView object
	 * @return zoomXmap
	 */
	public MapContainer getZoomXmap() {
		
		return zoomXmap;
	}

	/**
	 *  Gets the zoomYmap attribute of the DendroView object
	 * @return zoomYmap
	 */
	public MapContainer getZoomYmap() {
		
		return zoomYmap;
	}
	
	/** 
	 * Getter for viewFrame 
	 */
	public ViewFrame getViewFrame() {
		
		return viewFrame;
	}
	
	/** 
	 * Gets the model this DendroView is based on
	 */
	protected DataModel getDataModel() {
		
		return this.dataModel;
	}
	
	
	//Setters
	/**
	 * This should be called after setDataModel has been set to the 
	 * appropriate model
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
//		atrzview.setArraySelection(arraySelection);
		arraynameview.setArraySelection(arraySelection);
	}

	/**
	 * This should be called after setDataModel has been set 
	 * to the appropriate model
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
	 * Setting up a general layout for a button object
	 * The method is used to make all buttons appear consistent in aesthetics
	 * @param button
	 * @return
	 */
	public JButton setButtonLayout(String title){
		
		Font buttonFont = new Font("Sans Serif", Font.PLAIN, 14);
		
		JButton button = new JButton(title);
  		Dimension d = button.getPreferredSize();
  		d.setSize(d.getWidth()*1.5, d.getHeight()*1.5);
  		button.setPreferredSize(d);
  		
  		button.setFont(buttonFont);
  		button.setOpaque(true);
  		button.setBackground(GUIParams.ELEMENT);
  		button.setForeground(GUIParams.BG_COLOR);
  		
  		return button;
	}
	
	/**
	 * Setting up a general layout for a button object
	 * The method is used to make all buttons appear consistent in aesthetics
	 * @param button
	 * @return
	 */
	public JButton setZoomButtonLayout(String title){

		int buttonHeight = 30;
		int buttonWidth = 30;
		
		Font buttonFont = new Font("Sans Serif", Font.BOLD, 16);
		
		JButton button = new JButton(title);
		
		Dimension d = button.getPreferredSize();
  		d.setSize(buttonWidth, buttonHeight);
  		button.setPreferredSize(d);
  	
  		button.setFont(buttonFont);
  		button.setOpaque(true);
  		button.setBackground(GUIParams.ELEMENT);
  		button.setForeground(GUIParams.BG_COLOR);
  		
  		return button;
	}
	
	/**
	 * Setting up a general layout for a ComboBox object
	 * The method is used to make all ComboBoxes appear consistent in aesthetics
	 * @param combo
	 * @return
	 */
	public static JComboBox setComboLayout(String[] combos){
		
		JComboBox comboBox = new JComboBox(combos);
		Dimension d = comboBox.getPreferredSize();
		d.setSize(d.getWidth() * 1.5, d.getHeight() * 1.5);
		comboBox.setPreferredSize(d);
		comboBox.setFont(GUIParams.FONTS);
		comboBox.setBackground(Color.white);
		
		return comboBox;
	}
	
	/** 
	 * Setter for viewFrame 
	 */
	public void setViewFrame(ViewFrame viewFrame) {
		
		this.viewFrame = viewFrame;
	}
	
	/** 
	 * Setter for dataModel 
	 */
	protected void setDataModel(DataModel dataModel) {
		
		this.dataModel = dataModel;
	}
	
	public HeaderFinderPanel getGeneFinderPanel() {
		
		if (geneFinderPanel == null) {
			geneFinderPanel = new GeneFinderPanel(viewFrame, 
					getDataModel().getGeneHeaderInfo(), 
					viewFrame.getGeneSelection());
		}
		
		return geneFinderPanel;
	}
	
	/** 
	 * Getter for geneFinderPanel 
	 * @return HeaderFinderPanel arrayFinderPanel
	 */
	public HeaderFinderPanel getArrayFinderPanel() {
		
		if (arrayFinderPanel == null) {
			
			arrayFinderPanel = new ArrayFinderPanel(viewFrame, 
					getDataModel().getArrayHeaderInfo(), 
					viewFrame.getArraySelection());
		}
		return arrayFinderPanel;
	}
	
	/** Setter for root  - may not work properly
	public void setConfigNode(ConfigNode root) {
		this.root = root;
	}*/
}
