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

import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSplitPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.BrowserControl;
import edu.stanford.genetics.treeview.CancelableSettingsDialog;
import edu.stanford.genetics.treeview.CdtFilter;
import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.DummyConfigNode;
import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.HeaderSummaryPanel;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.MainProgramArgs;
import edu.stanford.genetics.treeview.MessagePanel;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.ModelessSettingsDialog;
import edu.stanford.genetics.treeview.ReorderedTreeSelection;
import edu.stanford.genetics.treeview.TabbedSettingsPanel;
import edu.stanford.genetics.treeview.TreeDrawerNode;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
import edu.stanford.genetics.treeview.UrlSettingsPanel;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.XmlConfig;
import edu.stanford.genetics.treeview.core.ArrayFinderPanel;
import edu.stanford.genetics.treeview.core.GeneFinderPanel;
import edu.stanford.genetics.treeview.core.HeaderFinderPanel;
import edu.stanford.genetics.treeview.model.AtrTVModel;
import edu.stanford.genetics.treeview.model.ReorderedDataModel;
import edu.stanford.genetics.treeview.model.TVModel;
//Explicitly imported because error (unclear TVModel reference) was thrown

/**
 * This class encapsulates a dendrogram view, which is the classic Eisen
 * treeview. It uses a drag grid panel to lay out a bunch of linked
 * visualizations of the data, a la Eisen. In addition to laying out components,
 * it also manages the GlobalZoomMap. This is necessary since both the GTRView
 * (gene tree) and GlobalView need to know where to lay out genes using the same
 * map. The zoom map is managed by the ViewFrame- it represents the selected
 * genes, and potentially forms a link between different views, only one of
 * which is the DendroView.
 * 
 * The intention here is that you create this from a model, and never replace
 * that model. If you want to show another file, make another dendroview. All
 * views should of course still listen to the model, since that can still be
 * changed ad libitum.
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.7 $ $Date: 2009-03-23 02:46:51 $
 */
public class DendroView2 extends JPanel implements ConfigNodePersistent, 
ComponentListener,MainPanel, Observer {

	// Static Variables
	private static final long serialVersionUID = 1L;

	private static ImageIcon treeviewIcon = null;

	// Instance Variables
	protected final int DIVIDER_SIZE = 1;

	protected ViewFrame viewFrame;

	protected ScrollPane panes[];
	protected boolean loaded;

	// Map Views
	protected GlobalView globalview;
	// protected ZoomView zoomview;

	// Trees
	protected GTRView gtrview;
	protected ATRView atrview;
	// protected ATRZoomView atrzview;

	// Row and Column names
	protected TextViewManager textview;
	protected ArrayNameView arraynameview;

	protected JScrollBar globalXscrollbar;
	protected JScrollBar globalYscrollbar;
	// protected JScrollBar zoomXscrollbar;
	// protected JScrollBar zoomYscrollbar;

	// Drawers
	protected ArrayDrawer arrayDrawer;
	protected InvertedTreeDrawer invertedTreeDrawer;
	protected LeftTreeDrawer leftTreeDrawer;

	// MapContainers
	protected MapContainer globalXmap;
	protected MapContainer globalYmap;
	// protected MapContainer zoomXmap;
	// protected MapContainer zoomYmap;

	protected MessagePanel statuspanel;
	protected BrowserControl browserControl;
	protected ConfigNode root;

	// persistent popups
	protected JDialog settingsFrame;
	protected TabbedSettingsPanel settingsPanel;

	/*
	 * The following arrays allow translation to and from screen and DataMatrix
	 * I had to add these in order to have gaps in the Dendroview of k-means
	 */
	private int[] arrayIndex = null;
	private int[] geneIndex = null;

	// The model
	private DataModel dataModel = null;

	// Selections
	private TreeSelectionI geneSelection = null;
	private TreeSelectionI arraySelection = null;

	private ColorExtractor colorExtractor;
	// private boolean zoomed = false;

	// Buttons
	private JButton scaleIncX;
	private JButton scaleIncY;
	private JButton scaleDecX;
	private JButton scaleDecY;
	private JButton scaleDefaultAll;

	// JPanels for gene search
	private HeaderFinderPanel geneFinderPanel = null;
	private HeaderFinderPanel arrayFinderPanel = null;

	// /**
	// * Chained constructor
	// * Calls setName of the JPanel class
	// * @param cols
	// * @param rows
	// * @param name
	// */
	// protected DendroView2(int cols, int rows, String name) {
	//
	// super.setName(name);
	// }

	/**
	 * Chained constructor for the DendroView object note this will reuse any
	 * existing MainView subnode of the documentconfig.
	 * 
	 * @param tVModel
	 *            model this DendroView is to represent
	 * @param vFrame
	 *            parent ViewFrame of DendroView
	 */
	public DendroView2(final DataModel tVModel, final ViewFrame vFrame) {

		this(tVModel, null, vFrame, "Dendrogram");
	}

	public DendroView2(final DataModel tVModel, final ConfigNode root,
			final ViewFrame vFrame) {

		this(tVModel, root, vFrame, "Dendrogram");
	}

	/**
	 * Constructor for the DendroView object which binds to an explicit
	 * confignode.
	 * 
	 * @param dataModel
	 *            model this DendroView is to represent
	 * @param root
	 *            Confignode to which to bind this DendroView
	 * @param vFrame
	 *            parent ViewFrame of DendroView
	 * @param name
	 *            name of this view.
	 */
	public DendroView2(DataModel dataModel, final ConfigNode root,
			final ViewFrame vFrame, final String name) {

		super.setName(name);
		this.viewFrame = vFrame;
		this.dataModel = dataModel;

		this.setLayout(new MigLayout("ins 0"));

		if (root == null) {
			if (dataModel.getDocumentConfigRoot() != null) {
				bindConfig(dataModel.getDocumentConfigRoot().fetchOrCreate(
						"MainView"));

			} else {
				bindConfig(new DummyConfigNode("MainView"));

			}
		} else {
			bindConfig(root);
		}

		// For K-Means
		if (dataModel.getArrayHeaderInfo().getIndex("GROUP") != -1) {
			final HeaderInfo headerInfo = dataModel.getArrayHeaderInfo();
			final int groupIndex = headerInfo.getIndex("GROUP");

			arrayIndex = getGroupVector(headerInfo, groupIndex);

		} else {
			arrayIndex = null;
		}

		if (dataModel.getGeneHeaderInfo().getIndex("GROUP") != -1) {
			System.err.println("got gene group header");
			final HeaderInfo headerInfo = dataModel.getGeneHeaderInfo();
			final int groupIndex = headerInfo.getIndex("GROUP");
			geneIndex = getGroupVector(headerInfo, groupIndex);

		} else {
			geneIndex = null;
		}

		if ((arrayIndex != null) || (geneIndex != null)) {
			dataModel = new ReorderedDataModel(dataModel, geneIndex, arrayIndex);
		}

		setDataModel(dataModel);

		setupViews();
		addComponentListener(this);

		if (geneIndex != null) {
			setGeneSelection(new ReorderedTreeSelection(
					viewFrame.getGeneSelection(), geneIndex));

		} else {
			setGeneSelection(viewFrame.getGeneSelection());
		}

		if (arrayIndex != null) {
			setArraySelection(new ReorderedTreeSelection(
					viewFrame.getArraySelection(), arrayIndex));

		} else {
			setArraySelection(viewFrame.getArraySelection());
		}
	}

	// Layout
	/**
	 * This method should be called only during initial setup of the ModelView.
	 * It sets up the views and binds them all to Config nodes.
	 * 
	 */
	protected void setupViews() {

		// Reset layout of this frame
		removeAll();

		final ColorPresets colorPresets = DendrogramFactory.getColorPresets();
		colorExtractor = new ColorExtractor();
		colorExtractor.setDefaultColorSet(colorPresets.getDefaultColorSet());
		colorExtractor.setMissing(DataModel.NODATA, DataModel.EMPTY);

		final DoubleArrayDrawer dArrayDrawer = new DoubleArrayDrawer();
		dArrayDrawer.setColorExtractor(colorExtractor);
		arrayDrawer = dArrayDrawer;
		((Observable) getDataModel()).addObserver(arrayDrawer);
		
		// set data first to avoid adding auto-generated
		// contrast to documentConfig.
		dArrayDrawer.setDataMatrix(getDataModel().getDataMatrix());
		dArrayDrawer.bindConfig(getFirst("ArrayDrawer"));

		// Set up status panel
		statuspanel = new MessagePanel("Status", GUIParams.BG_COLOR);
		//statuspanel.setMinimumSize(new Dimension(300, 200));

		// globalmaps tell globalview, atrview, and gtrview
		// where to draw each data point.
		// the scrollbars "scroll" by communicating with the maps.
		globalXmap = new MapContainer("Fixed");
		globalYmap = new MapContainer("Fixed");

		// Create the Global view (JPanel to display)
		globalview = new GlobalView();
		globalview.setXMap(globalXmap);
		globalview.setYMap(globalYmap);
		globalview.setHeaders(getDataModel().getGeneHeaderInfo(),
				getDataModel().getArrayHeaderInfo());
		globalview.setArrayDrawer(arrayDrawer);

		// scrollbars, mostly used by maps
		globalXscrollbar = globalview.getXScroll();
		globalXmap.setScrollbar(globalXscrollbar);

		globalYscrollbar = globalview.getYScroll();
		globalYmap.setScrollbar(globalYscrollbar);

		// Set up ZoomView
		// zoomview = new ZoomView();
		// zoomview.setXMap(getZoomXmap());
		// zoomview.setYMap(getZoomYmap());
		// zoomview.setArrayDrawer(arrayDrawer);
		
		// zoomXscrollbar = new JScrollBar(Adjustable.HORIZONTAL, 0, 1, 0, 1);
		// zoomYscrollbar = new JScrollBar(Adjustable.VERTICAL, 0, 1, 0, 1);

		// Zoom maps to set up data points (colored by arraydrawer)
		// zoomXmap = new MapContainer();
		// zoomXmap.setDefaultScale(12.0);
		// zoomXmap.setScrollbar(zoomXscrollbar);
		//
		// zoomYmap = new MapContainer();
		// zoomYmap.setDefaultScale(12.0);
		// zoomYmap.setScrollbar(zoomYscrollbar);
		
		// Set the Zoom maps for GlobalView
		// globalview.setZoomXMap(zoomXmap);
		// globalview.setZoomYMap(zoomYmap);
		
		// Changes Row/ Col number to gene names in Status panel
		// zoomview.setHeaders(getDataModel().getGeneHeaderInfo(),
		// getDataModel().getArrayHeaderInfo());
		
		// Set up ATRZoomView
		// atrzview = new ATRZoomView();
		// atrzview.setZoomMap(getZoomXmap());
		// atrzview.setHeaderSummary(atrview.getHeaderSummary());
		// atrzview.setInvertedTreeDrawer(invertedTreeDrawer);

		// Set up the column name display
		arraynameview = new ArrayNameView(getDataModel().getArrayHeaderInfo());
		arraynameview.setUrlExtractor(viewFrame.getArrayUrlExtractor());
		arraynameview.setDataModel(getDataModel());
		arraynameview.setMap(getGlobalXmap());

		textview = new TextViewManager(getDataModel().getGeneHeaderInfo(),
				viewFrame.getUrlExtractor(), getDataModel());
		textview.setMap(getGlobalYmap());

		// Set up row dendrogram
		leftTreeDrawer = new LeftTreeDrawer();
		gtrview = new GTRView();
		gtrview.setOpaque(true);
		gtrview.setMap(globalYmap);
		gtrview.setLeftTreeDrawer(leftTreeDrawer);
		gtrview.getHeaderSummary().setIncluded(new int[] { 0, 3 });

		// Set up column dendrogram
		invertedTreeDrawer = new InvertedTreeDrawer();
		atrview = new ATRView();
		atrview.setMap(globalXmap);
		atrview.setInvertedTreeDrawer(invertedTreeDrawer);
		atrview.getHeaderSummary().setIncluded(new int[] { 0, 3 });
		
		final HeaderSummary atrSummary = atrview.getHeaderSummary();
		
		// atrzview.setHeaderSummary(atrSummary);
		
		// Register Views
		registerView(globalview);
		registerView(atrview);
		registerView(arraynameview);
		registerView(textview);
		// registerView(zoomview);
		// registerView(atrzview);

		scaleDefaultAll = GUIParams.setButtonLayout("Reset");
		scaleDefaultAll.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				getGlobalXmap().setHome();
				getGlobalYmap().setHome();
			}
		});

		scaleIncX = GUIParams.setButtonLayout("+");
		scaleIncX.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				getGlobalXmap().zoomIn();
			}
		});

		scaleDecX = GUIParams.setButtonLayout("-");
		scaleDecX.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				getGlobalXmap().zoomOut();
			}
		});

		scaleIncY = GUIParams.setButtonLayout("+");
		scaleIncY.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				getGlobalYmap().zoomIn();
			}
		});

		scaleDecY = GUIParams.setButtonLayout("-");
		scaleDecY.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				getGlobalYmap().zoomOut();
			}
		});

		// reset persistent popups
		settingsFrame = null;
		settingsPanel = null;

		// urls
		colorExtractor.bindConfig(getFirst("ColorExtractor"));

		// this is here because my only subclass shares this code.
		bindTrees();

		globalXmap.bindConfig(getFirst("GlobalXMap"));
		globalYmap.bindConfig(getFirst("GlobalYMap"));
		textview.bindConfig(getFirst("TextView"));
		arraynameview.bindConfig(getFirst("ArrayNameView"));
		atrSummary.bindConfig(getFirst("AtrSummary"));
		gtrview.getHeaderSummary().bindConfig(getFirst("GtrSummary"));
		// getZoomXmap().bindConfig(getFirst("ZoomXMap"));
		// getZoomYmap().bindConfig(getFirst("ZoomYMap"));

		// perhaps I could remember this stuff in the MapContainer...
		globalXmap.setIndexRange(0, dataModel.getDataMatrix().getNumCol() - 1);
		globalYmap.setIndexRange(0, dataModel.getDataMatrix().getNumRow() - 1);
		// getZoomXmap().setIndexRange(-1, -1);
		// getZoomYmap().setIndexRange(-1, -1);

		// Ensuring window resizing works with GlobalView
		globalXmap.setHome();
		globalYmap.setHome();

		globalXmap.notifyObservers();
		globalYmap.notifyObservers();
		// getZoomXmap().notifyObservers();
		// getZoomYmap().notifyObservers();
		
		// Layout Setup
		doDoubleLayout();
	}

	/**
	 * Manages the component layout in TreeViewFrame
	 */
	protected void doDoubleLayout() {

		//Components for layout setup
		JPanel buttonPanel;
		JPanel crossPanel;
		JPanel fillPanel1;
		JPanel fillPanel2;
		JPanel fillPanel3;
		JPanel finderPanel;
		JPanel textpanel;
		JPanel navPanel;
		JSplitPane gtrPane;
		JSplitPane atrPane;
		// JButton saveButton;
		final JButton zoomButton;
		JButton deselectButton;
		JLabel nav;
		JLabel contrast;

		// Clear panel
		removeAll();
		
		setBackground(GUIParams.BG_COLOR);

		gtrview.setStatusPanel(statuspanel);
		gtrview.setViewFrame(viewFrame);

		// ButtonPanel
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new MigLayout());
		buttonPanel.setOpaque(false);

		crossPanel = new JPanel();
		crossPanel.setLayout(new MigLayout());
		crossPanel.setOpaque(false);

		// Filling panels to complement MigLayout
		fillPanel1 = new JPanel();
		fillPanel1.setOpaque(false);

		fillPanel2 = new JPanel();
		fillPanel2.setOpaque(false);
		
		fillPanel3 = new JPanel();
		fillPanel3.setOpaque(false);

		// saveButton = setButtonLayout("Save Zoomed Image", BLUE1);
		// saveButton.addActionListener(new ActionListener(){
		//
		// @Override
		// public void actionPerformed(ActionEvent arg0) {
		//
		// try {
		// saveImage(zoomview);
		//
		// } catch (IOException e) {
		//
		// }
		// }
		// });
		
		//Button for zooming selected area
		zoomButton = GUIParams.setButtonLayout("Zoom Selection");
		zoomButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				globalview.zoomSelection();
				globalview.centerSelection();
			}
		});

		//Button to deselect the current selection
		deselectButton = GUIParams.setButtonLayout("Deselect");
		deselectButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {

				geneSelection.setSelectedNode(null);
				geneSelection.deselectAllIndexes();

				arraySelection.setSelectedNode(null);
				arraySelection.deselectAllIndexes();

				geneSelection.notifyObservers();
				arraySelection.notifyObservers();
			}
		});

		//Panel to hold the navigation interface
		navPanel = new JPanel();
		navPanel.setLayout(new MigLayout());
		//navPanel.setBackground(GUIParams.PANEL_BG);
		navPanel.setOpaque(false);
		navPanel.setBorder(null);//BorderFactory.createEtchedBorder());

		nav = new JLabel("Navigation");
		nav.setFont(GUIParams.HEADER);
		nav.setForeground(GUIParams.ELEMENT);

		contrast = new JLabel("Contrast");
		contrast.setFont(GUIParams.HEADER);
		contrast.setForeground(GUIParams.ELEMENT);

		textpanel = new JPanel();
		textpanel.setLayout(new MigLayout("ins 0"));
		textpanel.setOpaque(false);
		
		finderPanel = new JPanel();
		finderPanel.setLayout(new MigLayout("ins 0"));
		finderPanel.setOpaque(false);
		
		gtrPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gtrview,
				textpanel);
		gtrPane.setDividerSize(DIVIDER_SIZE);
		gtrPane.setResizeWeight(0.5);
		gtrPane.setOpaque(false);
		gtrPane.setBorder(null);

		atrPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, atrview,
				arraynameview);
		atrPane.setDividerSize(DIVIDER_SIZE);
		atrPane.setResizeWeight(0.5);
		atrPane.setOpaque(false);
		atrPane.setBorder(null);

		// zoompanel = new JPanel();
		// zoompanel.setLayout(new MigLayout());
		// zoompanel.add(zoomview, BorderLayout.CENTER);
		// zoompanel.add(zoomXscrollbar, BorderLayout.SOUTH);
		// zoompanel.add(zoomYscrollbar, BorderLayout.EAST);

		textpanel.add(textview.getComponent(), "push, grow");

		final JLabel adjScale = new JLabel("Adjust Scale:");
		adjScale.setFont(GUIParams.FONTS);
		adjScale.setForeground(GUIParams.TEXT);

		crossPanel.add(scaleIncY, "span, alignx 50%, wrap");
		crossPanel.add(scaleDecX);
		crossPanel.add(scaleDefaultAll);
		crossPanel.add(scaleIncX, "wrap");
		crossPanel.add(scaleDecY, "span, alignx 50%");

		buttonPanel.add(adjScale, "pushx, wrap");
		buttonPanel.add(crossPanel, "pushx, alignx 50%");
		
		finderPanel.add(getGeneFinderPanel(), "width 100%, height 50%, wrap");
		finderPanel.add(getArrayFinderPanel(), "width 100%, height 50%");

		navPanel.add(nav, "span, wrap");
		navPanel.add(finderPanel, "push, height 30%, alignx 50%, wrap");
		navPanel.add(zoomButton, "pushx, alignx 50%, wrap");
		navPanel.add(deselectButton, "pushx, alignx 50%, wrap");
		navPanel.add(buttonPanel, "push, height 30%, alignx 50%");
		
		add(statuspanel, "pushx, width 20%, height 20%");
		add(atrPane, "grow, push, width 62%, height 20%");
		add(fillPanel3, "span 2, growx, pushx, width 18%, " +
				"height 20%, wrap");
		add(gtrPane, "grow, width 20%, height 79%");
		add(globalview, "push, grow, width 62%, height 79%");
		add(globalYscrollbar, "pushy, growy, width 1%, " +
				"height 79%");
		add(navPanel, "push, grow, width 17%, height 79%, " +
				"wrap");
		add(fillPanel1, "growx, pushx, width 20%, height 1%");
		add(globalXscrollbar, "growx, pushx, width 62%, " +
				"height 1%");
		add(fillPanel2, "span 2, growx, pushx, width 18%, " +
				"height 1%");
	}

	@Override
	public void refresh() {

		doDoubleLayout();

		this.revalidate();
		this.repaint();
	}

	// Methods
	private int[] getGroupVector(final HeaderInfo headerInfo,
			final int groupIndex) {

		int ngroup = 0;
		String cur = headerInfo.getHeader(0, groupIndex);

		for (int i = 0; i < headerInfo.getNumHeaders(); i++) {

			final String test = headerInfo.getHeader(i, groupIndex);
			if (cur.equals(test) == false) {
				cur = test;
				ngroup++;
			}
		}

		final int[] groupVector = new int[ngroup + headerInfo.getNumHeaders()];
		ngroup = 0;
		cur = headerInfo.getHeader(0, groupIndex);

		for (int i = 0; i < headerInfo.getNumHeaders(); i++) {

			final String test = headerInfo.getHeader(i, groupIndex);
			if (cur.equals(test) == false) {
				groupVector[i + ngroup] = -1;
				cur = test;
				ngroup++;
			}
			groupVector[i + ngroup] = i;
		}

		return groupVector;
	}

	/**
	 * Gets the contrast attribute of the DendroView object
	 * 
	 * @return The contrast value public double getContrast() { return
	 *         colorExtractor.getContrast(); }
	 */

	/**
	 * Finds the currently selected genes, mirror image flips them, and then
	 * rebuilds all necessary trees and saved data to the .jtv file.
	 * 
	 */
	private void flipSelectedGTRNode() {

		int leftIndex, rightIndex;
		String selectedID;
		final TreeDrawerNode geneNode = leftTreeDrawer
				.getNodeById(getGeneSelection().getSelectedNode());

		if (geneNode == null || geneNode.isLeaf()) {

			return;
		}

		selectedID = geneNode.getId();

		// find the starting index of the left array tree, the ending
		// index of the right array tree
		leftIndex = getDataModel().getGeneHeaderInfo().getHeaderIndex(
				geneNode.getLeft().getLeftLeaf().getId());
		rightIndex = getDataModel().getGeneHeaderInfo().getHeaderIndex(
				geneNode.getRight().getRightLeaf().getId());

		final int num = getDataModel().getDataMatrix().getNumRow();

		final int[] newOrder = SetupInvertedArray(num, leftIndex, rightIndex);

		/*
		 * System.out.print("Fliping to: "); for(int i = 0; i < newOrder.length;
		 * i++) { System.out.print(newOrder[i] + " "); } System.out.println("");
		 */

		((TVModel) getDataModel()).reorderGenes(newOrder);
		// ((TVModel)getDataModel()).saveGeneOrder(newOrder);
		((Observable) getDataModel()).notifyObservers();

		updateGTRDrawer(selectedID);
	}

	private int[] SetupInvertedArray(final int num, final int leftIndex,
			final int rightIndex) {

		final int[] newOrder = new int[num];

		for (int i = 0; i < num; i++) {

			newOrder[i] = i;
		}

		for (int i = 0; i <= (rightIndex - leftIndex); i++) {

			newOrder[leftIndex + i] = rightIndex - i;
		}

		return newOrder;
	}

	/**
	 * Finds the currently selected arrays, mirror image flips them, and then
	 * rebuilds all necessary trees and saved data to the .jtv file.
	 */
	private void flipSelectedATRNode() {

		int leftIndex, rightIndex;
		String selectedID;
		final TreeDrawerNode arrayNode = invertedTreeDrawer
				.getNodeById(getArraySelection().getSelectedNode());

		if (arrayNode == null || arrayNode.isLeaf()) {

			return;
		}

		selectedID = arrayNode.getId();

		// find the starting index of the left array tree,
		// the ending index of the right array tree
		leftIndex = getDataModel().getArrayHeaderInfo().getHeaderIndex(
				arrayNode.getLeft().getLeftLeaf().getId());
		rightIndex = getDataModel().getArrayHeaderInfo().getHeaderIndex(
				arrayNode.getRight().getRightLeaf().getId());

		final int num = getDataModel().getDataMatrix().getNumUnappendedCol();

		final int[] newOrder = new int[num];

		for (int i = 0; i < num; i++) {

			newOrder[i] = i;
		}

		for (int i = 0; i <= (rightIndex - leftIndex); i++) {

			newOrder[leftIndex + i] = rightIndex - i;
		}

		/*
		 * System.out.print("Fliping to: "); for(int i = 0; i < newOrder.length;
		 * i++) { System.out.print(newOrder[i] + " "); } System.out.println("");
		 */

		((TVModel) getDataModel()).reorderArrays(newOrder);
		((Observable) getDataModel()).notifyObservers();

		updateATRDrawer(selectedID);
	}

	/**
	 * Displays a data set alongside the primary one for comparison.
	 * 
	 * @param model
	 *            - the model containing cdt data being added to the display.
	 */
	public void compareToModel(final TVModel model) {

		getDataModel().removeAppended();
		getDataModel().append(model);

		arraySelection.resize(getDataModel().getDataMatrix().getNumCol());
		arraySelection.notifyObservers();

		globalXmap.setIndexRange(0,
				getDataModel().getDataMatrix().getNumCol() - 1);
		globalXmap.notifyObservers();

		// zoomXmap.setIndexRange(0,
		// getDataModel().getDataMatrix().getNumCol() - 1);
		// zoomXmap.notifyObservers();

		((Observable) getDataModel()).notifyObservers();
	}

	/**
	 * Aligns the current ATR to the passed model as best as possible, saves the
	 * new ordering to the .jtv file.
	 * 
	 * @param model
	 *            - AtrTVModel with which to align.
	 */
	public void alignAtrToModel(final AtrTVModel model) {

		try {
			String selectedID = null;

			try {
				selectedID = getArraySelection().getSelectedNode();

			} catch (final NullPointerException npe) {
				npe.printStackTrace();
			}

			int[] ordering;
			ordering = AtrAligner.align(getDataModel().getAtrHeaderInfo(),
					getDataModel().getArrayHeaderInfo(),
					model.getAtrHeaderInfo(), model.getArrayHeaderInfo());

			/*
			 * System.out.print("New ordering: "); for(int i = 0; i <
			 * ordering.length; i++) { System.out.print(ordering[i] + " "); }
			 * System.out.println();
			 */

			((TVModel) getDataModel()).reorderArrays(ordering);
			((Observable) getDataModel()).notifyObservers();

			if (selectedID != null) {

				updateATRDrawer(selectedID);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.out.println(e.toString());
		}
	}

	/**
	 * Updates the GTRDrawer to reflect changes in the DataModel gene order;
	 * rebuilds the TreeDrawerNode tree.
	 * 
	 * @param selectedID
	 *            ID of the node selected before a change in tree structure was
	 *            made. This node is then found and reselected after the ATR
	 *            tree is rebuilt.
	 */
	private void updateGTRDrawer(final String selectedID) {

		try {
			final TVModel tvmodel = (TVModel) getDataModel();
			leftTreeDrawer.setData(tvmodel.getGtrHeaderInfo(),
					tvmodel.getGeneHeaderInfo());

			final HeaderInfo trHeaderInfo = tvmodel.getGtrHeaderInfo();

			if (trHeaderInfo.getIndex("NODECOLOR") >= 0) {
				TreeColorer.colorUsingHeader(leftTreeDrawer.getRootNode(),
						trHeaderInfo, trHeaderInfo.getIndex("NODECOLOR"));
			}
		} catch (final DendroException e) {

			// LogPanel.println("Had problem setting up the array tree : "
			// + e.getMessage());
			// e.printStackTrace();
			final Box mismatch = new Box(BoxLayout.Y_AXIS);
			mismatch.add(new JLabel(e.getMessage()));
			mismatch.add(new JLabel("Perhaps there is a mismatch "
					+ "between your ATR and CDT files?"));
			mismatch.add(new JLabel("Ditching Gene Tree, since it's lame."));

			JOptionPane.showMessageDialog(viewFrame, mismatch,
					"Tree Construction Error", JOptionPane.ERROR_MESSAGE);

			gtrview.setEnabled(false);
			// gtrzview.setEnabled(false);

			try {
				leftTreeDrawer.setData(null, null);

			} catch (final DendroException ex) {

			}
		}

		final TreeDrawerNode arrayNode = leftTreeDrawer.getRootNode().findNode(
				selectedID);

		geneSelection.setSelectedNode(arrayNode.getId());
		gtrview.setSelectedNode(arrayNode);
		// gtrzview.setSelectedNode(arrayNode);

		geneSelection.notifyObservers();
		leftTreeDrawer.notifyObservers();
	}

	/**
	 * Updates the ATRDrawer to reflect changes in the DataMode array order;
	 * rebuilds the TreeDrawerNode tree.
	 * 
	 * @param selectedID
	 *            ID of the node selected before a change in tree structure was
	 *            made. This node is then found and reselected after the ATR
	 *            tree is rebuilt.
	 */
	private void updateATRDrawer(final String selectedID) {

		try {
			final TVModel tvmodel = (TVModel) getDataModel();
			invertedTreeDrawer.setData(tvmodel.getAtrHeaderInfo(),
					tvmodel.getArrayHeaderInfo());
			final HeaderInfo trHeaderInfo = tvmodel.getAtrHeaderInfo();

			if (trHeaderInfo.getIndex("NODECOLOR") >= 0) {

				TreeColorer.colorUsingHeader(invertedTreeDrawer.getRootNode(),
						trHeaderInfo, trHeaderInfo.getIndex("NODECOLOR"));
			}
		} catch (final DendroException e) {

			// LogPanel.println("Had problem setting up the array tree : "
			// + e.getMessage());
			// e.printStackTrace();
			final Box mismatch = new Box(BoxLayout.Y_AXIS);
			mismatch.add(new JLabel(e.getMessage()));
			mismatch.add(new JLabel("Perhaps there is a mismatch "
					+ "between your ATR and CDT files?"));
			mismatch.add(new JLabel("Ditching Array Tree, since it's lame."));

			JOptionPane.showMessageDialog(viewFrame, mismatch,
					"Tree Construction Error", JOptionPane.ERROR_MESSAGE);

			atrview.setEnabled(false);
			// atrzview.setEnabled(false);

			try {
				invertedTreeDrawer.setData(null, null);

			} catch (final DendroException ex) {

			}
		}

		final TreeDrawerNode arrayNode = invertedTreeDrawer.getRootNode()
				.findNode(selectedID);

		arraySelection.setSelectedNode(arrayNode.getId());
		// atrzview.setSelectedNode(arrayNode);
		atrview.setSelectedNode(arrayNode);

		arraySelection.notifyObservers();
		invertedTreeDrawer.notifyObservers();
	}

	/**
	 * Creates an AtrTVModel for use in tree alignment.
	 * 
	 * @param fileSet
	 * @return a new AtrTVModel with the file set loaded into it.
	 * @throws LoadException
	 */
	protected AtrTVModel makeAtrModel(final FileSet fileSet)
			throws LoadException {

		final AtrTVModel atrTVModel = new AtrTVModel();

		try {
			atrTVModel.loadNew(fileSet);

		} catch (final LoadException e) {
			JOptionPane.showMessageDialog(this, e);
			throw e;
		}

		return atrTVModel;
	}

	protected TVModel makeCdtModel(final FileSet fileSet) throws LoadException {

		final TVModel tvModel = new TVModel();

		try {
			tvModel.loadNew(fileSet);

		} catch (final LoadException e) {
			JOptionPane.showMessageDialog(this, e);
			throw e;
		}

		return tvModel;
	}

	/**
	 * Open a dialog which allows the user to select a new CDT data file for
	 * tree alignment.
	 * 
	 * @return The fileset corresponding to the dataset.
	 */
	/*
	 * Unknow what actually happens if the file CDT does not have an associated
	 * ATR.
	 */
	protected FileSet offerATRFileSelection() throws LoadException {

		FileSet fileSet1; // will be chosen...

		final JFileChooser fileDialog = new JFileChooser();
		setupATRFileDialog(fileDialog);

		final int retVal = fileDialog.showOpenDialog(this);

		if (retVal == JFileChooser.APPROVE_OPTION) {
			final File chosen = fileDialog.getSelectedFile();
			fileSet1 = new FileSet(chosen.getName(), chosen.getParent()
					+ File.separator);

		} else {
			throw new LoadException("File Dialog closed without selection...",
					LoadException.NOFILE);
		}

		return fileSet1;
	}

	/**
	 * Sets up a dialog for loading ATR files for tree alignment.
	 * 
	 * @param fileDialog
	 *            the dialog to setup
	 */
	protected void setupATRFileDialog(final JFileChooser fileDialog) {

		final CdtFilter ff = new CdtFilter();
		try {
			fileDialog.addChoosableFileFilter(ff);
			// will fail on pre-1.3 swings
			fileDialog.setAcceptAllFileFilterUsed(true);

		} catch (final Exception e) {
			// hmm... I'll just assume that there's no accept all.
			fileDialog
					.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

						@Override
						public boolean accept(final File f) {

							return true;
						}

						@Override
						public String getDescription() {

							return "All Files";
						}
					});
		}

		fileDialog.setFileFilter(ff);
		fileDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
	}

	@Override
	public void scrollToGene(final int i) {

		getGlobalYmap().scrollToIndex(i);
		getGlobalYmap().notifyObservers();
	}

	@Override
	public void scrollToArray(final int i) {

		getGlobalXmap().scrollToIndex(i);
		getGlobalXmap().notifyObservers();
	}

	@Override
	public void update(final Observable o, final Object arg) {

		if (o == geneSelection) {
			gtrview.scrollToNode(geneSelection.getSelectedNode());
		}
	}

	/**
	 * this is meant to be called from setupViews. It make sure that the trees
	 * are generated from the current model, and enables/disables them as
	 * required.
	 * 
	 * I factored it out because it is common betwen DendroView and
	 * KnnDendroView.
	 */
	protected void bindTrees() {

		final DataModel tvmodel = getDataModel();

		if ((tvmodel != null) && tvmodel.aidFound()) {
			try {
				atrview.setEnabled(true);
				// atrzview.setEnabled(true);

				invertedTreeDrawer.setData(tvmodel.getAtrHeaderInfo(),
						tvmodel.getArrayHeaderInfo());
				final HeaderInfo trHeaderInfo = tvmodel.getAtrHeaderInfo();

				if (trHeaderInfo.getIndex("NODECOLOR") >= 0) {
					TreeColorer.colorUsingHeader(
							invertedTreeDrawer.getRootNode(), trHeaderInfo,
							trHeaderInfo.getIndex("NODECOLOR"));
				}

			} catch (final DendroException e) {
				// LogPanel.println("Had problem setting up the array tree : "
				// + e.getMessage());
				// e.printStackTrace();
				final Box mismatch = new Box(BoxLayout.Y_AXIS);
				mismatch.add(new JLabel(e.getMessage()));
				mismatch.add(new JLabel("Perhaps there is a mismatch "
						+ "between your ATR and CDT files?"));
				mismatch.add(new JLabel("Ditching Array Tree, "
						+ "since it's lame."));

				JOptionPane.showMessageDialog(viewFrame, mismatch,
						"Tree Construction Error", JOptionPane.ERROR_MESSAGE);

				atrview.setEnabled(false);
				// atrzview.setEnabled(false);

				try {
					invertedTreeDrawer.setData(null, null);

				} catch (final DendroException ex) {

				}
			}
		} else {
			atrview.setEnabled(false);
			// atrzview.setEnabled(false);

			try {
				invertedTreeDrawer.setData(null, null);

			} catch (final DendroException ex) {

			}
		}

		invertedTreeDrawer.notifyObservers();

		if ((tvmodel != null) && tvmodel.gidFound()) {
			try {
				gtrview.setEnabled(true);
				// gtrzview.setEnabled(true);

				leftTreeDrawer.setData(tvmodel.getGtrHeaderInfo(),
						tvmodel.getGeneHeaderInfo());
				final HeaderInfo gtrHeaderInfo = tvmodel.getGtrHeaderInfo();

				if (gtrHeaderInfo.getIndex("NODECOLOR") >= 0) {
					TreeColorer.colorUsingHeader(leftTreeDrawer.getRootNode(),
							tvmodel.getGtrHeaderInfo(),
							gtrHeaderInfo.getIndex("NODECOLOR"));

				} else {
					TreeColorer.colorUsingLeaf(leftTreeDrawer.getRootNode(),
							tvmodel.getGeneHeaderInfo(), tvmodel
									.getGeneHeaderInfo().getIndex("FGCOLOR"));
				}

			} catch (final DendroException e) {
				// LogPanel.println("Had problem setting up the gene tree :
				// " + e.getMessage());
				// e.printStackTrace();
				final Box mismatch = new Box(BoxLayout.Y_AXIS);
				mismatch.add(new JLabel(e.getMessage()));
				mismatch.add(new JLabel("Perhaps there is a mismatch "
						+ "between your GTR and CDT files?"));
				mismatch.add(new JLabel("Ditching Gene Tree, "
						+ "since it's lame."));

				JOptionPane.showMessageDialog(viewFrame, mismatch,
						"Tree Construction Error", JOptionPane.ERROR_MESSAGE);

				gtrview.setEnabled(false);
				// gtrzview.setEnabled(false);

				try {
					leftTreeDrawer.setData(null, null);

				} catch (final DendroException ex) {

				}
			}
		} else {
			gtrview.setEnabled(false);
			// gtrzview.setEnabled(false);

			try {
				leftTreeDrawer.setData(null, null);

			} catch (final DendroException ex) {

			}
		}

		leftTreeDrawer.notifyObservers();
	}

	/**
	 * registers a modelview with the hint and status panels, and the viewFrame.
	 * 
	 * @param modelView
	 *            The ModelView to be added
	 */
	private void registerView(final ModelView modelView) {

		modelView.setStatusPanel(statuspanel);
		modelView.setViewFrame(viewFrame);
	}

	// Menus
	@Override
	public void populateExportMenu(final TreeviewMenuBarI menu) {

		menu.addMenuItem("Export to Postscript...", new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent actionEvent) {

				MapContainer initXmap, initYmap;

				// if ((getArraySelection().getNSelectedIndexes() != 0) ||
				// (getGeneSelection().getNSelectedIndexes() != 0)) {
				// initXmap = getZoomXmap();
				// initYmap = getZoomYmap();
				//
				// } else {
				initXmap = getGlobalXmap();
				initYmap = getGlobalYmap();
				// }

				final PostscriptExportPanel psePanel = setupPostscriptExport(
						initXmap, initYmap);

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
			public void actionPerformed(final ActionEvent actionEvent) {

				MapContainer initXmap, initYmap;
				// if ((getArraySelection().getNSelectedIndexes() != 0) ||
				// (getGeneSelection().getNSelectedIndexes() != 0)) {
				// initXmap = getZoomXmap();
				// initYmap = getZoomYmap();
				//
				// } else {
				initXmap = getGlobalXmap();
				initYmap = getGlobalYmap();
				// }

				final BitmapExportPanel bitmapPanel = setupBitmapExport(
						initXmap, initYmap);

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
					public void actionPerformed(final ActionEvent actionEvent) {

						final PostscriptColorBarExportPanel gcbPanel = new PostscriptColorBarExportPanel(
								((DoubleArrayDrawer) arrayDrawer)
										.getColorExtractor());

						gcbPanel.setSourceSet(getDataModel().getFileSet());

						final JDialog popup = new CancelableSettingsDialog(
								viewFrame, "Export ColorBar to Postscript",
								gcbPanel);
						popup.pack();
						popup.setVisible(true);
					}
				});
		menu.setMnemonic(KeyEvent.VK_B);

		menu.addMenuItem("Export ColorBar to Image...", new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent actionEvent) {

				final BitmapColorBarExportPanel gcbPanel = new BitmapColorBarExportPanel(
						((DoubleArrayDrawer) arrayDrawer).getColorExtractor());

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

	private void addSimpleExportOptions(final TreeviewMenuBarI menu) {

		menu.addMenuItem("Save Tree Image", new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent actionEvent) {

				MapContainer initXmap, initYmap;
				initXmap = getGlobalXmap();
				initYmap = getGlobalYmap();

				final BitmapExportPanel bitmapPanel = new BitmapExportPanel(
						arraynameview.getHeaderInfo(), getDataModel()
								.getGeneHeaderInfo(), getGeneSelection(),
						getArraySelection(), invertedTreeDrawer,
						leftTreeDrawer, arrayDrawer, initXmap, initYmap);

				bitmapPanel.setGeneFont(textview.getFont());
				bitmapPanel.setArrayFont(arraynameview.getFont());
				bitmapPanel.setSourceSet(getDataModel().getFileSet());
				bitmapPanel.setDrawSelected(false);
				bitmapPanel.includeData(false);
				bitmapPanel.includeAtr(false);
				bitmapPanel.deselectHeaders();

				final JDialog popup = new CancelableSettingsDialog(viewFrame,
						"Export to Image", bitmapPanel);
				popup.pack();
				popup.setVisible(true);
			}
		});
		menu.setMnemonic(KeyEvent.VK_T);

		menu.addMenuItem("Save Thumbnail Image", new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent actionEvent) {

				MapContainer initXmap, initYmap;
				initXmap = getGlobalXmap();
				initYmap = getGlobalYmap();

				final BitmapExportPanel bitmapPanel = new BitmapExportPanel(
						arraynameview.getHeaderInfo(), getDataModel()
								.getGeneHeaderInfo(), getGeneSelection(),
						getArraySelection(), invertedTreeDrawer,
						leftTreeDrawer, arrayDrawer, initXmap, initYmap);

				bitmapPanel.setSourceSet(getDataModel().getFileSet());
				bitmapPanel.setGeneFont(textview.getFont());
				bitmapPanel.setArrayFont(arraynameview.getFont());
				bitmapPanel.setDrawSelected(false);
				bitmapPanel.includeGtr(false);
				bitmapPanel.includeAtr(false);
				bitmapPanel.deselectHeaders();

				final JDialog popup = new CancelableSettingsDialog(viewFrame,
						"Export To Image", bitmapPanel);
				popup.pack();
				popup.setVisible(true);
			}
		});
		menu.setMnemonic(KeyEvent.VK_H);

		// menu.addMenuItem("Save Zoomed Image", new ActionListener() {
		//
		// @Override
		// public void actionPerformed(ActionEvent actionEvent) {
		//
		// MapContainer initXmap, initYmap;
		// initXmap = getZoomXmap();
		// initYmap = getZoomYmap();
		//
		// BitmapExportPanel bitmapPanel = new BitmapExportPanel
		// (arraynameview.getHeaderInfo(),
		// getDataModel().getGeneHeaderInfo(),
		// getGeneSelection(), getArraySelection(),
		// invertedTreeDrawer,
		// leftTreeDrawer, arrayDrawer, initXmap, initYmap);
		//
		// bitmapPanel.setSourceSet(getDataModel().getFileSet());
		// bitmapPanel.setGeneFont(textview.getFont());
		// bitmapPanel.setArrayFont(arraynameview.getFont());
		//
		// bitmapPanel.includeGtr(false);
		// bitmapPanel.includeAtr(false);
		// bitmapPanel.deselectHeaders();
		//
		// final JDialog popup =
		// new CancelableSettingsDialog(viewFrame,
		// "Export To Image", bitmapPanel);
		// popup.pack();
		// popup.setVisible(true);
		// }
		// });
		// menu.setMnemonic(KeyEvent.VK_Z);
	}

	/**
	 * show summary of the specified indexes
	 */
	public void showSubDataModel(final int[] indexes) {

		getViewFrame().showSubDataModel(indexes, null, null);
	}

	// Populate Menus
	/**
	 * adds DendroView stuff to Analysis menu
	 * 
	 * @param menu
	 *            menu to add to
	 */
	@Override
	public void populateAnalysisMenu(final TreeviewMenuBarI menu) {

		menu.addMenuItem("Flip Array Tree Node", new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent ae) {

				if (getGtrview().hasFocus()) {

					flipSelectedGTRNode();
				} else {

					flipSelectedATRNode();
				}
			}
		});
		menu.setAccelerator(KeyEvent.VK_L);
		menu.setMnemonic(KeyEvent.VK_A);

		menu.addMenuItem("Flip Gene Tree Node", new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent ae) {

				flipSelectedGTRNode();
			}
		});
		menu.setMnemonic(KeyEvent.VK_G);

		menu.addMenuItem("Align to Tree...", new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent ae) {

				try {

					final FileSet fileSet = offerATRFileSelection();
					final AtrTVModel atrModel = makeAtrModel(fileSet);

					alignAtrToModel(atrModel);
				} catch (final LoadException e) {

					if ((e.getType() != LoadException.INTPARSE)
							&& (e.getType() != LoadException.NOFILE)) {
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
			public void actionPerformed(final ActionEvent ae) {

				try {

					final FileSet fileSet = offerATRFileSelection();
					final TVModel tvModel = makeCdtModel(fileSet);
					compareToModel(tvModel);
				} catch (final LoadException e) {

					if ((e.getType() != LoadException.INTPARSE)
							&& (e.getType() != LoadException.NOFILE)) {
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
			public void actionPerformed(final ActionEvent ae) {

				getDataModel().removeAppended();
				globalXmap.setIndexRange(0, getDataModel().getDataMatrix()
						.getNumCol() - 1);
				globalXmap.notifyObservers();

				// zoomXmap.setIndexRange(0,
				// getDataModel().getDataMatrix().getNumCol() - 1);
				// zoomXmap.notifyObservers();

				((Observable) getDataModel()).notifyObservers();
			}
		});
		menu.setAccelerator(KeyEvent.VK_R);
		menu.setMnemonic(KeyEvent.VK_R);

		// menu.addMenuItem("Summary Window...",new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// SummaryViewWizard wizard =
		// new SummaryViewWizard(DendroView2.this);
		// int retval = JOptionPane.showConfirmDialog(DendroView2.this,
		// wizard, "Configure Summary", JOptionPane.OK_CANCEL_OPTION);
		// if (retval == JOptionPane.OK_OPTION) {
		// showSubDataModel(wizard.getIndexes());
		// }
		// }
		// });
		// menu.setMnemonic(KeyEvent.VK_S);
	}

	/**
	 * adds DendroView stuff to Document menu
	 * 
	 * @param menu
	 *            menu to add to
	 */
	@Override
	public void populateSettingsMenu(final TreeviewMenuBarI menu) {

		menu.addMenuItem("Pixel Settings...", new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent actionEvent) {

				ColorExtractor ce = null;

				try {
					ce = ((DoubleArrayDrawer) arrayDrawer).getColorExtractor();

				} catch (final Exception e) {

				}

				final PixelSettingsSelector pssSelector = new PixelSettingsSelector(
						globalXmap, globalYmap,
						// getZoomXmap(), getZoomYmap(),
						ce, DendrogramFactory.getColorPresets());

				final JDialog popup = new ModelessSettingsDialog(viewFrame,
						"Pixel Settings", pssSelector);

				System.out.println("showing popup...");
				popup.addWindowListener(XmlConfig
						.getStoreOnWindowClose(getDataModel()
								.getDocumentConfigRoot()));

				popup.pack();
				popup.setVisible(true);
			}
		}, 0);
		menu.setMnemonic(KeyEvent.VK_X);

		menu.addMenuItem("Url Settings...", new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent actionEvent) {

				// keep refs to settingsPanel, settingsFrame local,
				// since will dispose of self when closed...
				final TabbedSettingsPanel settingsPanel = new TabbedSettingsPanel();

				final UrlSettingsPanel genePanel = new UrlSettingsPanel(
						viewFrame.getUrlExtractor(), viewFrame
								.getGeneUrlPresets());
				settingsPanel.addSettingsPanel("Gene", genePanel);

				final UrlSettingsPanel arrayPanel = new UrlSettingsPanel(
						viewFrame.getArrayUrlExtractor(), viewFrame
								.getArrayUrlPresets());
				settingsPanel.addSettingsPanel("Array", arrayPanel);

				final JDialog settingsFrame = new ModelessSettingsDialog(
						viewFrame, "Url Settings", settingsPanel);

				settingsFrame.addWindowListener(XmlConfig
						.getStoreOnWindowClose(getDataModel()
								.getDocumentConfigRoot()));
				settingsFrame.pack();
				settingsFrame.setVisible(true);
			}
		}, 0);
		menu.setMnemonic(KeyEvent.VK_U);

		menu.addMenuItem("Font Settings...", new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent actionEvent) {

				// keep ref to settingsFrame local,
				// since will dispose of self when closed...
				final TabbedSettingsPanel settingsPanel = new TabbedSettingsPanel();

				final FontSettingsPanel genePanel = new FontSettingsPanel(
						textview);
				settingsPanel.addSettingsPanel("Gene", genePanel);

				final FontSettingsPanel arrayPanel = new FontSettingsPanel(
						arraynameview);
				settingsPanel.addSettingsPanel("Array", arrayPanel);

				final JDialog settingsFrame = new ModelessSettingsDialog(
						viewFrame, "Font Settings", settingsPanel);
				settingsFrame.addWindowListener(XmlConfig
						.getStoreOnWindowClose(getDataModel()
								.getDocumentConfigRoot()));
				settingsFrame.pack();
				settingsFrame.setVisible(true);
			}
		}, 0);
		menu.setMnemonic(KeyEvent.VK_F);

		menu.addMenuItem("Annotations...", new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent actionEvent) {
				// keep refs to settingsPanel, settingsFrame local,
				// since will dispose of self when closed...
				final TabbedSettingsPanel settingsPanel = new TabbedSettingsPanel();

				final HeaderSummaryPanel genePanel = new HeaderSummaryPanel(
						getDataModel().getGeneHeaderInfo(), textview
								.getHeaderSummary(), (TreeViewFrame)viewFrame);
				settingsPanel.addSettingsPanel("Gene", genePanel);

				final HeaderSummaryPanel arrayPanel = new HeaderSummaryPanel(
						arraynameview.getHeaderInfo(), arraynameview
								.getHeaderSummary(), (TreeViewFrame)viewFrame);
				settingsPanel.addSettingsPanel("Array", arrayPanel);

				final HeaderSummaryPanel atrPanel = new HeaderSummaryPanel(
						getDataModel().getAtrHeaderInfo(), atrview
								.getHeaderSummary(), (TreeViewFrame)viewFrame);
				settingsPanel.addSettingsPanel("Array Tree", atrPanel);

				final HeaderSummaryPanel gtrPanel = new HeaderSummaryPanel(
						getDataModel().getGtrHeaderInfo(), gtrview
								.getHeaderSummary(), (TreeViewFrame)viewFrame);
				settingsPanel.addSettingsPanel("Gene Tree", gtrPanel);

				final JDialog settingsFrame = new ModelessSettingsDialog(
						viewFrame, "Annotation Settings", settingsPanel);

				settingsFrame.addWindowListener(XmlConfig
						.getStoreOnWindowClose(getDataModel()
								.getDocumentConfigRoot()));
				settingsFrame.addWindowListener(new WindowAdapter() {

					@Override
					public void windowClosed(final WindowEvent e) {

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
		 * MenuItem urlItem = new MenuItem("Url Options...");
		 * urlItem.addActionListener( new ActionListener() { public void
		 * actionPerformed(ActionEvent actionEvent) { UrlEditor urlEditor = new
		 * UrlEditor(urlExtractor, viewFrame.getGeneUrlPresets(),
		 * dataModel.getGeneHeaderInfo()); urlEditor.showConfig(viewFrame);
		 * dataModel.getDocumentConfig().store(); } }); menu.add(urlItem);
		 */
	}

	/**
	 * Alok: this function changes the info in the ConfigNode to match the
	 * current panel sizes. this is a hack, since I don't know how to intercept
	 * panel resizing. Actually, in the current layout this isn't even used.
	 */
	@Override
	public void syncConfig() {
		/*
		 * DragGridPanel running = this; floa t[] heights =
		 * running.getHeights(); ConfigNode heightNodes[] =
		 * root.fetch("Height"); for (int i = 0; i < heights.length; i++) { if
		 * (i < heightNodes.length) { heightNodes[i].setAttribute("value",
		 * (double) heights[i], 1.0 / heights.length); } else { ConfigNode n =
		 * root.create("Height"); n.setAttribute("value", (double) heights[i],
		 * 1.0 / heights.length); } }
		 * 
		 * float[] widths = running.getWidths(); ConfigNode widthNodes[] =
		 * root.fetch("Width"); for (int i = 0; i < widths.length; i++) { if (i
		 * < widthNodes.length) { widthNodes[i].setAttribute("value", (double)
		 * widths[i], 1.0 / widths.length); } else { ConfigNode n =
		 * root.create("Width"); n.setAttribute("value", (double) widths[i], 1.0
		 * / widths.length); } }
		 */
	}

	/**
	 * binds this dendroView to a particular confignode, resizing the panel
	 * sizes appropriately.
	 * 
	 * @param configNode
	 *            ConfigNode to bind to
	 */

	@Override
	public void bindConfig(final ConfigNode configNode) {
		root = configNode;
		/*
		 * ConfigNode heightNodes[] = root.fetch("Height"); ConfigNode
		 * widthNodes[] = root.fetch("Width");
		 * 
		 * float heights[]; float widths[]; if (heightNodes.length != 0) {
		 * heights = new float[heightNodes.length]; widths = new
		 * float[widthNodes.length]; for (int i = 0; i < heights.length; i++) {
		 * heights[i] = (float) heightNodes[i].getAttribute( "value", 1.0 /
		 * heights.length); } for (int j = 0; j < widths.length; j++) {
		 * widths[j] = (float) widthNodes[j].getAttribute( "value", 1.0 /
		 * widths.length); } } else { widths = new float[]{2 / 11f, 3 / 11f, 3 /
		 * 11f, 3 / 11f}; heights = new float[]{3 / 16f, 1 / 16f, 3 / 4f}; }
		 * setHeights(heights); setWidths(widths);
		 */
	}

	public void saveImage(final JPanel panel) throws IOException {

		File saveFile = new File("savedImage.png");

		final JFileChooser fc = new JFileChooser();

		fc.setFileFilter(new FileNameExtensionFilter("PNG", "png"));
		fc.setSelectedFile(saveFile);
		final int returnVal = fc.showSaveDialog(this);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			saveFile = fc.getSelectedFile();

			String fileName = saveFile.toString();

			if (!fileName.endsWith(".png")) {
				fileName += ".png";
				saveFile = new File(fileName);
			}

			final BufferedImage im = new BufferedImage(panel.getWidth(),
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
			final MapContainer initXmap, final MapContainer initYmap) {

		final PostscriptExportPanel psePanel = new PostscriptExportPanel(
				arraynameview.getHeaderInfo(), getDataModel()
						.getGeneHeaderInfo(), getGeneSelection(),
				getArraySelection(), invertedTreeDrawer, leftTreeDrawer,
				arrayDrawer, initXmap, initYmap);

		psePanel.setSourceSet(getDataModel().getFileSet());
		psePanel.setGeneFont(textview.getFont());
		psePanel.setArrayFont(arraynameview.getFont());
		psePanel.setIncludedArrayHeaders(arraynameview.getHeaderSummary()
				.getIncluded());
		psePanel.setIncludedGeneHeaders(textview.getHeaderSummary()
				.getIncluded());

		return psePanel;
	}

	private BitmapExportPanel setupBitmapExport(final MapContainer initXmap,
			final MapContainer initYmap) {

		final BitmapExportPanel bitmapPanel = new BitmapExportPanel(
				arraynameview.getHeaderInfo(), getDataModel()
						.getGeneHeaderInfo(), getGeneSelection(),
				getArraySelection(), invertedTreeDrawer, leftTreeDrawer,
				arrayDrawer, initXmap, initYmap);

		bitmapPanel.setSourceSet(getDataModel().getFileSet());
		bitmapPanel.setGeneFont(textview.getFont());
		bitmapPanel.setArrayFont(arraynameview.getFont());
		bitmapPanel.setIncludedArrayHeaders(arraynameview.getHeaderSummary()
				.getIncluded());
		bitmapPanel.setIncludedGeneHeaders(textview.getHeaderSummary()
				.getIncluded());

		return bitmapPanel;
	}

	@Override
	public void export(final MainProgramArgs mainArgs) throws ExportException {

		final DendroviewArgs args = new DendroviewArgs(mainArgs.remainingArgs());

		if (args.getFilePath() == null) {
			System.err.println("Error, must specify an output file\n");
			args.printUsage();

			return;
		}

		final ExportPanel exporter;

		if ("ps".equalsIgnoreCase(args.getExportType())) {
			exporter = setupPostscriptExport(getGlobalXmap(), getGlobalYmap());

		} else if ("png".equalsIgnoreCase(args.getExportType())
				|| "gif".equalsIgnoreCase(args.getExportType())) {
			exporter = setupBitmapExport(getGlobalXmap(), getGlobalYmap());

		} else {
			System.err.println("Error, unrecognized output format "
					+ args.getExportType() + " \n");

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

			if (args.getContrast() != null) {
				colorExtractor.setContrast(args.getContrast());
			}

			if (args.getGtrWidth() != null) {
				exporter.setExplicitGtrWidth(args.getGtrWidth());
			}

			if (args.getAtrHeight() != null) {
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

	// Getters
	/**
	 * Always returns an instance of the node, even if it has to create it.
	 */
	protected ConfigNode getFirst(final String name) {

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

			} catch (final java.security.AccessControlException e) {
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
	 * Gets the globalXmap attribute of the DendroView object
	 * 
	 * @return globalXmap
	 */
	public MapContainer getGlobalXmap() {

		return globalXmap;
	}

	/**
	 * Gets the globalYmap attribute of the DendroView object
	 * 
	 * @return The globalYmap
	 */
	public MapContainer getGlobalYmap() {

		return globalYmap;
	}

	// /**
	// * Gets the zoomXmap attribute of the DendroView object
	// * @return zoomXmap
	// */
	// public MapContainer getZoomXmap() {
	//
	// return zoomXmap;
	// }
	//
	// /**
	// * Gets the zoomYmap attribute of the DendroView object
	// * @return zoomYmap
	// */
	// public MapContainer getZoomYmap() {
	//
	// return zoomYmap;
	// }

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

	// Setters
	/**
	 * This should be called after setDataModel has been set to the appropriate
	 * model
	 * 
	 * @param arraySelection
	 */
	public void setArraySelection(final TreeSelectionI arraySelection) {

		if (this.arraySelection != null) {

			this.arraySelection.deleteObserver(this);
		}

		this.arraySelection = arraySelection;
		arraySelection.addObserver(this);

		globalview.setArraySelection(arraySelection);
		// zoomview.setArraySelection(arraySelection);
		atrview.setArraySelection(arraySelection);
		// atrzview.setArraySelection(arraySelection);
		arraynameview.setArraySelection(arraySelection);
	}

	/**
	 * This should be called after setDataModel has been set to the appropriate
	 * model
	 * 
	 * @param geneSelection
	 */
	public void setGeneSelection(final TreeSelectionI geneSelection) {

		if (this.geneSelection != null) {

			this.geneSelection.deleteObserver(this);
		}

		this.geneSelection = geneSelection;
		geneSelection.addObserver(this);

		globalview.setGeneSelection(geneSelection);
		// zoomview.setGeneSelection(geneSelection);
		gtrview.setGeneSelection(geneSelection);
		textview.setGeneSelection(geneSelection);
	}

	/**
	 * Setter for viewFrame
	 */
	public void setViewFrame(final ViewFrame viewFrame) {

		this.viewFrame = viewFrame;
	}

	/**
	 * Setter for dataModel
	 */
	protected void setDataModel(final DataModel dataModel) {

		this.dataModel = dataModel;
	}

	public HeaderFinderPanel getGeneFinderPanel() {

		if (geneFinderPanel == null) {
			geneFinderPanel = new GeneFinderPanel(viewFrame, this,
					getDataModel().getGeneHeaderInfo(),
					viewFrame.getGeneSelection());
		}

		return geneFinderPanel;
	}

	/**
	 * Getter for geneFinderPanel
	 * 
	 * @return HeaderFinderPanel arrayFinderPanel
	 */
	public HeaderFinderPanel getArrayFinderPanel() {

		if (arrayFinderPanel == null) {

			arrayFinderPanel = new ArrayFinderPanel(viewFrame, this,
					getDataModel().getArrayHeaderInfo(),
					viewFrame.getArraySelection());
		}
		return arrayFinderPanel;
	}

	/**
	 * Setter for root - may not work properly public void
	 * setConfigNode(ConfigNode root) { this.root = root; }
	 */
	// Component Listeners
		@Override
		public void componentHidden(final ComponentEvent arg0) {
		}

		@Override
		public void componentMoved(final ComponentEvent arg0) {
		}

		// Keep view centered and zoomed on visible part, also refreshing the
		// MapContainer with setHome to always fill out the entire GlobalView
		// panel
		@Override
		public void componentResized(final ComponentEvent arg0) {

			final int scrollX = globalXmap.getScroll().getValue();
			final int scrollY = globalYmap.getScroll().getValue();

			if (globalXmap.getAvailablePixels() > globalXmap.getUsedPixels()
					&& globalXmap.getScale() == globalXmap.getMinScale()) {
				globalXmap.setHome();
			}

			if (globalYmap.getAvailablePixels() > globalYmap.getUsedPixels()
					&& globalYmap.getScale() == globalYmap.getMinScale()) {
				globalYmap.setHome();
			}

			globalview.centerView(scrollX, scrollY);

			this.repaint();
			this.revalidate();
		}

		@Override
		public void componentShown(final ComponentEvent arg0) {
		}
}
