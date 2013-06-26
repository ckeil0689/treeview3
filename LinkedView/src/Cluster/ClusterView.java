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
package Cluster;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import edu.stanford.genetics.treeview.BrowserControl;
import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.DragGridPanel;
import edu.stanford.genetics.treeview.DummyConfigNode;
import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.MainProgramArgs;
import edu.stanford.genetics.treeview.MessagePanel;
import edu.stanford.genetics.treeview.ReorderedTreeSelection;
import edu.stanford.genetics.treeview.TabbedSettingsPanel;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.model.ReorderedDataModel;
import edu.stanford.genetics.treeview.plugin.dendroview.ArrayDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.DoubleArrayDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.LeftTreeDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;
//Explicitly imported because error (unclear TVModel reference) was thrown



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
public class ClusterView extends JPanel implements ConfigNodePersistent, MainPanel, Observer {

	private static final long serialVersionUID = 1L;
	
	
	ClusterModel outer;
	ClusterModel.ClusterDataMatrix matrix;
	List<double[]> geneList;
	
	//Instance variable in which the loaded data array is being stored
	private double[] dataArray;
	
	/**
	 *  Constructor for the DendroView object
	 * note this will reuse any existing MainView subnode of the documentconfig.
	 *
	 * @param  tVModel   model this DendroView is to represent
	 * @param  vFrame  parent ViewFrame of DendroView
	 */
	public ClusterView(DataModel cVModel, ViewFrame vFrame) {
		this(cVModel, null, vFrame, "Cluster View");
	}
	public ClusterView(DataModel cVModel, ConfigNode root, ViewFrame vFrame) {
		this(cVModel, root, vFrame, "Cluster View");
	}
	/**
	 *  Constructor for the DendroView object which binds to an explicit confignode
	 *
	 * @param  dataModel   model this DendroView is to represent
	 * @param  root   Confignode to which to bind this DendroView
	 * @param  vFrame  parent ViewFrame of DendroView
	 * @param  name name of this view.
	 */
	public ClusterView(DataModel dataModel, ConfigNode root, ViewFrame vFrame, String name) {
		super.setName(name);
		viewFrame = vFrame;
		this.dataModel = dataModel;
		
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
		
		//Setting up the ArrayList to display
		outer = (ClusterModel) dataModel;
		System.out.println("LOL WHAT? " + outer.nExpr());
		matrix = outer.getDataMatrix();
		dataArray = matrix.getExprData();
		
		geneList = splitArray(dataArray, outer);
		
		setupViews();
		
	}
	
	//function to split up a long array into smaller arrays
	public List <double[]> splitArray(double[] array, ClusterModel model){
		
		int lower = 0;
		int upper = 0;
		int max = model.nExpr();
		
		List<double[]> geneList = new ArrayList<double[]>();
		
		for(int i = 0; i < array.length/max; i++){
			
			upper+=max;
			
			geneList.add(Arrays.copyOfRange(array, lower, upper));
			
			lower = upper;
			
		}
		
		if(upper < array.length -1){
			
			lower = upper;
			upper = array.length;
			
			geneList.add(Arrays.copyOfRange(array, lower, upper));
		}
		
		return geneList;
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

	protected ClusterView(int cols, int rows, String name) {
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
		
	}

	
	/**
	 * Creates an AtrTVModel for use in tree alignment.
	 * @param fileSet
	 * @return a new AtrTVModel with the file set loaded into it.
	 * @throws LoadException
	 */
	
	protected ClusterModel makeTxtModel(ClusterFileSet fileSet) throws LoadException {
		 ClusterModel clusterModel = new ClusterModel();
		 
		 try {
			 clusterModel.loadNew(fileSet);
		 } catch (LoadException e) {
			 JOptionPane.showMessageDialog(this, e);
			 throw e;
		 }
		 
		 return clusterModel;
	}


	/**
	* Open a dialog which allows the user to select a new TXT data file for tree alignment.
	*
	* @return The fileset corresponding to the dataset.
	*/
	/*
	 *  Unknow what actually happens if the file CDT does not have an associated ATR.
	 */
	protected ClusterFileSet offerTXTFileSelection() throws LoadException
	{
		ClusterFileSet fileSet1; // will be chosen...
	 
		JFileChooser fileDialog = new JFileChooser();
		setupClusterFileDialog(fileDialog);
		int retVal = fileDialog.showOpenDialog(this);
		if(retVal == JFileChooser.APPROVE_OPTION)
		{
			File chosen = fileDialog.getSelectedFile();
		 
			fileSet1 = new ClusterFileSet(chosen.getName(), chosen.getParent() + File.separator);
		}
		else
		{
			throw new LoadException("File Dialog closed without selection...", LoadException.NOFILE);
		}
	 
		return fileSet1;
	}

	/**
	 * Sets up a dialog for loading TXT files for tree alignment.
	 * @param fileDialog the dialog to setup
	 */
	protected void setupClusterFileDialog(JFileChooser fileDialog) {
		TxtFilter ff = new TxtFilter();
		try {
			fileDialog.addChoosableFileFilter(ff);
			// will fail on pre-1.3 swings
			fileDialog.setAcceptAllFileFilterUsed(true);
		} catch (Exception e) {
			// hmm... I'll just assume that there's no accept all.
			fileDialog.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
				public boolean accept (File f) {
					return true;
				}
				public String getDescription () {
					return "All Files";
				}
			});
		}
		fileDialog.setFileFilter(ff);
		fileDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
	 }

	
	/**
	 *  This method should be called only during initial setup of the modelview
	 *
	 *  It sets up the views and binds them all to config nodes.
	 *
	 */
	protected void setupViews() {
		this.removeAll();
		
		clusterPanel = new ClusterPanel("Cluster Table here");
		statuspanel = new MessagePanel("View Status");

		DoubleArrayDrawer dArrayDrawer = new DoubleArrayDrawer();
		arrayDrawer = dArrayDrawer;
		((Observable)getDataModel()).addObserver(arrayDrawer);
		
	
		// scrollbars, mostly used by maps
		globalXscrollbar = new JScrollBar(JScrollBar.HORIZONTAL, 0,1,0,1);
		globalYscrollbar = new JScrollBar(JScrollBar.VERTICAL,0,1,0,1);
		zoomXscrollbar = new JScrollBar(JScrollBar.HORIZONTAL, 0,1,0,1);
		zoomYscrollbar = new JScrollBar(JScrollBar.VERTICAL,0,1,0,1);



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
		

		doDoubleLayout();
		

		// reset persistent popups
		settingsFrame = null;
		settingsPanel = null;
		
		// set data first to avoid adding auto-genereated contrast to documentConfig.
		dArrayDrawer.setDataMatrix(getDataModel().getDataMatrix());
		dArrayDrawer.bindConfig(getFirst("ArrayDrawer"));
	
		
		globalXmap.bindConfig(getFirst("GlobalXMap"));
		globalYmap.bindConfig(getFirst("GlobalYMap"));
		

		// perhaps I could remember this stuff in the MapContainer...
		globalXmap.setIndexRange(0, dataModel.getDataMatrix().getNumCol() - 1);
		globalYmap.setIndexRange(0, dataModel.getDataMatrix().getNumRow() - 1);

		globalXmap.notifyObservers();
		globalYmap.notifyObservers();
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
		//The following two variables simulate data from the .txt tab-delimited files loaded into Cluster
		//random two-dimensional string array to try out in table
		String[][] people = {{"Pierce", "Electrician", "43"},{"Monica","Nurse", "34"}, 
				{"Alfred", "Engineer", "24"}};
		
		//string array with column names
		String[] colNames = {"Name", " Occupation", "Age"};
		
		//create trial table with demo data to add to JSplitPane
		//JTable can either accept raw string array data or Vectors
		//Vectors preferred because they can be resized and optimiz memory allocation
		System.out.println("Attention: " + geneList.size());
		JTable table = new JTable(new ClusterTableModel(geneList, outer));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		//create a scrollPane with he table in it 
		JScrollPane tableScroll = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		table.setFillsViewportHeight(true);
		
		//creating the two GridPanels
		DragGridPanel left = new DragGridPanel(2,2);
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

		float rheights [] = new float[3];
		rheights[0] = (float).15;
		rheights[1] = (float).05;
		rheights[2] = (float).8;
		right.setHeights(rheights);

		//creating and adding the status panel to left GridPanel
		Rectangle rectangle  = new Rectangle(0, 0, 1, 1);

		left.addComponent(statuspanel, rectangle);
		rectangle.translate(1, 0);

		rectangle.translate(-1, 0);
		
		rectangle.translate(0, 1);

		rectangle.setSize(1, 2);
		rectangle.translate(1, -1);
		right.addComponent(clusterPanel, rectangle);   //clusterPanel added to the right GridPanel object (what is doDoubleLayout() used on?)
		
		//creating and adding the GTR Panel to the left GridPanel
		rectangle = new Rectangle(0, 1, 1, 1);
		JPanel gtrPanel = new JPanel();
		gtrPanel.setLayout(new BorderLayout());
		gtrPanel.add(new JScrollBar(JScrollBar.HORIZONTAL, 0,1,0,1), BorderLayout.SOUTH);
		left.addComponent(gtrPanel, rectangle);      


		// global view
		rectangle.translate(1, 0);
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(globalYscrollbar, BorderLayout.EAST);
		panel.add(globalXscrollbar, BorderLayout.SOUTH);	
		left.addComponent(panel, rectangle);


		// zoom view
		rectangle.translate(-1, 1);
		JPanel zoompanel = new JPanel();
		zoompanel.setLayout(new BorderLayout());
		zoompanel.add(zoomXscrollbar, BorderLayout.SOUTH);	
		zoompanel.add(zoomYscrollbar, BorderLayout.EAST);
		right.addComponent(zoompanel, rectangle);



		rectangle.translate(1, 0);
		JPanel textpanel = new JPanel();
		textpanel.setLayout(new BorderLayout());
		right.addComponent(textpanel, rectangle);

		//creating the split panel with 'left' and 'right' components
//		JSplitPane innerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
//				//left, 
//				tableScroll, right);
		JPanel innerPanel = new JPanel();
		innerPanel.setSize(viewFrame.getSize());
		innerPanel.add(tableScroll);
		//adds expansion arrows to central divider
//		innerPanel.setOneTouchExpandable(true);
//		innerPanel.setDividerLocation(300);
		//CardLayout sets up the tabbed organization of plugin-windows
		setLayout(new CardLayout());
		add(innerPanel, "running");

	}

	

	// Menus
	
	public void populateExportMenu(TreeviewMenuBarI menu){
		  	
		
		
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
	public void populateAnalysisMenu(TreeviewMenuBarI menu) {
	

	}

	/**
	 *  adds DendroView stuff to Document menu
	 *
	 * @param  menu  menu to add to
	 */
	public void populateSettingsMenu(TreeviewMenuBarI menu) {


	}

	/**
	 *  this function changes the info in the confignode to match the current panel sizes. 
	 * this is a hack, since I don't know how to intercept panel resizing.
	 * Actually, in the current layout this isn't even used.
	 */
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


    protected JScrollBar zoomXscrollbar, zoomYscrollbar;

	protected LeftTreeDrawer leftTreeDrawer;

	private TreeSelectionI geneSelection = null;
	private TreeSelectionI arraySelection = null;

	protected MapContainer globalXmap, globalYmap;
	protected MapContainer zoomXmap,   zoomYmap;

	protected ClusterPanel clusterPanel;
	protected MessagePanel statuspanel;
	protected BrowserControl browserControl;
	protected ArrayDrawer arrayDrawer;
	protected ConfigNode root;
	/** Setter for root  - may not work properly
	public void setConfigNode(ConfigNode root) {
		this.root = root;
	}
	/** Getter for root */
	public ConfigNode getConfigNode() {
		return root;
	}
	
	// persistent popups
	protected JDialog settingsFrame;
	protected TabbedSettingsPanel settingsPanel;
	
	private static ImageIcon treeviewIcon = null;
	/**
	 * icon for display in tabbed panel
	 */
	public ImageIcon getIcon() {
		if (treeviewIcon == null)
			try {
				treeviewIcon = new ImageIcon("images/treeview.gif", "TreeView Icon");
			} catch (java.security.AccessControlException e) {
				// need form relative URL somehow...
			}
		return treeviewIcon;
	}


	@Override
	public void update(Observable arg0, Object arg1) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void scrollToGene(int i) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void scrollToArray(int i) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void export(MainProgramArgs args) throws ExportException {
		// TODO Auto-generated method stub
		
	}

}
