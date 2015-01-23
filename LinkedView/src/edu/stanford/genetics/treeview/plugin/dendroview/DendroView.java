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

import java.awt.Event;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import Utilities.GUIFactory;
import Utilities.Helper;
import Utilities.StringRes;
import edu.stanford.genetics.treeview.DendroPanel;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.DataTicker;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.core.ArrayFinderBox;
import edu.stanford.genetics.treeview.core.GeneFinderBox;
import edu.stanford.genetics.treeview.core.HeaderFinderBox;

/**
 * TODO Refactor this JavaDoc. 
 * It's not applicable to the current program anymore.
 * 
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
public class DendroView implements Observer, DendroPanel {

	private int div_size;

	// Container JFrame
	protected TreeViewFrame tvFrame;

	private String name;

	// Main containers
	private final JPanel dendroPane;
	private JPanel firstPanel;

	protected ScrollPane panes[];

	// Matrix view
	private final GlobalView globalview;

	// Tree views
	protected final GTRView gtrview;
	protected final ATRView atrview;
	
	/* JSplitPanes containing trees & labels */
	private JSplitPane gtrPane;
	private JSplitPane atrPane;

	/* Gene and array label views */
	protected final GeneLabelView textview;
	protected final ArrayLabelView arraynameview;

	/* JScrollBars for GlobalView */
	/* TODO one glorious day, update GlobalView to a scrollpane... */
	protected JScrollBar globalXscrollbar;
	protected JScrollBar globalYscrollbar;
	
	protected DataTicker dataTicker;
	
	private HeaderFinderBox geneFinderBox;
	private HeaderFinderBox arrayFinderBox;

	/* Some important class-wide JMenuItems */
	private JMenuItem colorMenuItem;
	private JMenuItem annotationsMenuItem;
	private JMenuItem showTreesMenuItem;
	private JMenu matrixMenu;

	/* JButtons for scaling the matrix */
	/* TODO should be controlled in a GlobalViewController...when it exists */
	private JButton zoomBtn;
	private JButton scaleIncX;
	private JButton scaleIncY;
	private JButton scaleIncXY;
	private JButton scaleDecX;
	private JButton scaleDecY;
	private JButton scaleDecXY;
	private JButton scaleDefaultAll;
	
	// Buttons for interaction in dendroview.
	/* TODO depreciate when clickable searchbar is implemented */
	private JButton searchBtn;
	
	/* GlobalView default sizes */
	/* TODO needed? ... */
	private double gvWidth;
	private double gvHeight;
	
	private boolean showSearch;
	
	/* Maximum GlobalView dimensions in percent */
	public static final double MAX_GV_WIDTH = 75;
	public static final double MAX_GV_HEIGHT = 80;
	
	/* 
	 * MapContainers map tile size (scale) to selection rectangles 
	 * in GlobalView, label & tree positions 
	 */
	protected MapContainer globalXmap = null;
	protected MapContainer globalYmap = null;

	/**
	 * Chained constructor for the DendroView object without a name.
	 * 
	 * @param vFrame
	 *            parent ViewFrame of DendroView
	 */
	public DendroView(final TreeViewFrame tvFrame) {

		this(tvFrame, "Dendrogram");
	}

	/* TODO why does this even exist... */
	public DendroView(final Preferences root, final TreeViewFrame tvFrame) {

		this(tvFrame, "Dendrogram");
	}

	/**
	 * Constructor for the DendroView object
	 * 
	 * @param vFrame
	 *            parent ViewFrame of DendroView
	 * @param name
	 *            name of this view.
	 */
	public DendroView(final TreeViewFrame tvFrame, final String name) {

		this.tvFrame = tvFrame;
		this.name = "DendroView";

		/* main panel */
		dendroPane = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING_FILL, 
				null);
		
		/* data ticker panel */
		dataTicker = new DataTicker();
		
		/* Create the Global view (JPanel to display) */
		globalview = new GlobalView();

		/* scrollbars, mostly used by maps */
		globalXscrollbar = globalview.getXScroll();
		globalYscrollbar = globalview.getYScroll();

		/* Set up the column name display */
//		arraynameview = new ArrayNameView_deprec();
		arraynameview = new ArrayLabelView();
		// arraynameview.setUrlExtractor(viewFrame.getArrayUrlExtractor());

//		textview = new TextView_deprec();
		textview = new GeneLabelView();

		// Set up row dendrogram
		gtrview = new GTRView();
		gtrview.getHeaderSummary().setIncluded(new int[] { 0, 3 });

		// Set up column dendrogram
		atrview = new ATRView();
		atrview.getHeaderSummary().setIncluded(new int[] { 0, 3 });
		
		showSearch = false;
		
		setupScaleButtons();
	}
	
	public void setGlobalXMap(MapContainer xmap) {
		
		this.globalXmap = xmap;
	}
	
	public void setGlobalYMap(MapContainer ymap) {
		
		this.globalYmap = ymap;
	}
	
	public void prepareView(final HeaderInfo geneHI, final HeaderInfo arrayHI,
			MapContainer xmap, MapContainer ymap) {
		
		setupSearch(geneHI, arrayHI, xmap, ymap);
		setupLayout();
	}
	
	public void resetMatrixSize() {
		
		setupLayout();
	}
	
	public void setMatrixHome(boolean isHome) {
		
		globalview.resetHome(isHome);
	}
	
	public void setShowSearch() {
		
		showSearch = !showSearch;
		setupLayout();
	}

	/**
	 * Returns the dendroPane so it can be displayed in TVFrame.
	 * 
	 * @return JPanel dendroPane
	 */
	public JPanel makeDendro() {

		arraynameview.generateView(tvFrame.getUrlExtractor());
		textview.generateView(tvFrame.getUrlExtractor());
		
		globalview.setHeaderSummary(textview.getHeaderSummary(), 
				arraynameview.getHeaderSummary());
		
		// Register Views
		registerView(globalview);
		registerView(atrview);
		registerView(arraynameview);
		registerView(textview);
		registerView(gtrview);
		
		return dendroPane;
	}
	
	private JPanel makeSearchPanel() {
		
		if(geneFinderBox == null || arrayFinderBox == null) return null;
		
		JPanel searchPanel = GUIFactory.createJPanel(true, GUIFactory.DEFAULT, 
				GUIFactory.DARK_BG);
		
		String tooltip = "You can use wildcards to search (*, ?). E.g.: *complex* --> "
				+ "Rpd3s complex, ATP Synthase (complex V), etc...";
		searchPanel.setToolTipText(tooltip);
		
		Image img = GUIFactory.getIconImage("close_x.png");
		ImageIcon close_x = new ImageIcon(img);
		
		searchPanel.add(new JLabel(close_x), "push, al right");
		searchPanel.add(arrayFinderBox.getSearchTermBox());
		searchPanel.add(geneFinderBox.getSearchTermBox());
		searchPanel.add(searchBtn);
		
		return searchPanel;
	}
	
	private void setupSearch(final HeaderInfo geneHI, 
			final HeaderInfo arrayHI, MapContainer xmap, MapContainer ymap) {
		
		setSearchTermBoxes(geneHI, arrayHI, xmap, ymap);
//		setArrayFinderPanel(arrayHI, geneHI);
		
		searchBtn = GUIFactory.createNavBtn("searchIcon");
		//GUIFactory.createBtn(StringRes.btn_SearchLabels);
		searchBtn.setToolTipText(StringRes.tt_searchRowCol);
	}
	
	
	
	/**
	 * Manages the component layout in TreeViewFrame
	 */
	private void setupLayout() {

		LogBuffer.println("DendroPane layout called.");
		
		/* Clear dendroPane first */
		dendroPane.removeAll();

		/* Panels for layout setup */
		JPanel btnPanel;
		JPanel crossPanel;
		JPanel textpanel;
		JPanel arrayNamePanel;
		JPanel arrayContainer;
		JPanel geneContainer;
		JPanel globalViewContainer;
		JPanel navContainer;
		JPanel bottomPanel;
		
//		searchPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);

		/* Generate the sub-panels */
		btnPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
		
		crossPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
		
		globalViewContainer = GUIFactory.createJPanel(false, 
				GUIFactory.NO_PADDING_FILL, null); 
		
		navContainer = GUIFactory.createJPanel(false, 
				GUIFactory.NO_PADDING_FILL, null);

		bottomPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null); 
		
		arrayContainer = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING_X,
				null);
		
		geneContainer = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING_Y, 
				null);

		firstPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
		firstPanel.setBorder(null);

		textpanel = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING, null); 

		arrayNamePanel = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING, 
				null);

		div_size = 5;
		gtrPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gtrview,
				textpanel);
		gtrPane.setResizeWeight(0.5);
		gtrPane.setOpaque(false);
		gtrPane.setOneTouchExpandable(true); // does not work on Linux :(
		gtrPane.setDividerSize(div_size);
		
		colorDivider(gtrPane);
		gtrPane.setBorder(null);
		
		if(gtrview.isEnabled()) {
			gtrPane.setDividerLocation(tvFrame.getConfigNode()
					.getDouble("gtr_loc", 0.5));
		} else {
			gtrPane.setDividerLocation(0.0);
			gtrPane.setEnabled(false);
		}

		atrPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, atrview,
				arrayNamePanel);
		atrPane.setResizeWeight(0.5);
		atrPane.setOpaque(false);
		atrPane.setOneTouchExpandable(true);
		atrPane.setDividerSize(div_size);
		
		colorDivider(atrPane);
		atrPane.setBorder(null);
		
		if(atrview.isEnabled()) {
			atrPane.setDividerLocation(tvFrame.getConfigNode()
					.getDouble("atr_loc", 0.5));
		} else {
			atrPane.setDividerLocation(0.0);
			atrPane.setEnabled(false);
		}
		
		if(!treesEnabled() && showTreesMenuItem != null) {
			showTreesMenuItem.setEnabled(false);
		}
		
		if(getDivLoc(atrview) > 0.0 || getDivLoc(gtrview) > 0.0) {
			showTreesMenuItem.setText("Hide Trees...");
		}
			
		textpanel.add(textview.getComponent(), "push, grow");
		arrayNamePanel.add(arraynameview.getComponent(), "push, grow");
		
		globalViewContainer.add(globalview, "w 99%, h 99%, push, alignx 50%, "
				+ "aligny 50%");
		globalViewContainer.add(globalYscrollbar, "w 1%, h 100%, wrap");
		globalViewContainer.add(globalXscrollbar, "span, pushx, alignx 50%, "
				+ "w 100%, h 1%");

		crossPanel.add(scaleIncY, "span 2 1, alignx 100%, h 33%");
		crossPanel.add(scaleIncXY, "h 33%, wrap");
		crossPanel.add(scaleDecX, "h 33%");
		crossPanel.add(zoomBtn, "h 33%");
		crossPanel.add(scaleIncX, "h 33%, wrap");
		crossPanel.add(scaleDecXY, "h 33%");
		crossPanel.add(scaleDecY, "span 2 1, h 33%, alignx 0%");
		
		btnPanel.add(crossPanel, "pushx, alignx 50%, wrap");
		btnPanel.add(scaleDefaultAll, "push, alignx 50%, aligny 5%");
		
		navContainer.add(btnPanel, "push, alignx 50%, aligny 100%, wrap");
		navContainer.add(dataTicker.getTickerPanel(), "push, h 25%!, aligny 5%");
		
		arrayContainer.add(atrPane, "w 99%, h 100%");
		geneContainer.add(gtrPane, "w 100%, h 99%, wrap");
		
		/* Add the scrollbars (outside of LabelViews) */
		JScrollBar arrayScroll = arraynameview.getScrollBar();
		JScrollBar geneScroll = textview.getScrollBar();
		
		arrayContainer.add(arrayScroll, "w 1%, h 100%");
		geneContainer.add(geneScroll, "w 100%, h 1%");

		if(gvWidth == 0 && gvHeight == 0) {
			gvWidth = MAX_GV_WIDTH;
			gvHeight = MAX_GV_HEIGHT;
		}
		
		/* Column widths */
		double textViewCol = (100 - gvWidth - 1) / 2;
		
		/* Heights */
		double arrayRow = (100 - gvHeight - 2);
		double bottomRow = 2;
 		
		/* Adding all components to the dendroPane */
		if(showSearch) {
			gvHeight -= 2;
			dendroPane.add(this.makeSearchPanel(), "w 100%, h 2%, span, wrap");
		} else {
			gvHeight = MAX_GV_HEIGHT;
		}
		
		dendroPane.add(firstPanel, "w " + textViewCol + "%, "
				+ "h " + arrayRow + "%, pushx");
		
		dendroPane.add(arrayContainer, "w " + gvWidth + "%, "
				+ "h " + arrayRow + "%, growx");
		
		dendroPane.add(navContainer, "span 1 2, w " + (textViewCol - 1) 
				+ "%!, h 100%, wrap");
		
		dendroPane.add(geneContainer, "w " + textViewCol + "%, "
				+ "h " + gvHeight + "%, growy");
		
		dendroPane.add(globalViewContainer, "w " + gvWidth + "%, "
				+ "h " + gvHeight + "%, grow, wrap");
		
		dendroPane.add(bottomPanel, "span, h " + bottomRow + "%");
		
		dendroPane.revalidate();
		dendroPane.repaint();
	}
	
	/* Sets up the buttons which control scaling and zooming */
	private void setupScaleButtons() {
		
		scaleDefaultAll = GUIFactory.createNavBtn(StringRes.icon_home);
		scaleDefaultAll.setToolTipText("Reset the zoomed view");
		
		scaleIncX = GUIFactory.createNavBtn(StringRes.icon_zoomIn);
		scaleIncX.setToolTipText(StringRes.tt_xZoomIn);
		
		scaleIncXY = GUIFactory.createNavBtn(StringRes.icon_zoomIn);
		scaleIncXY.setToolTipText(StringRes.tt_xyZoomIn);
		
		scaleDecX = GUIFactory.createNavBtn(StringRes.icon_zoomOut);
		scaleDecX.setToolTipText(StringRes.tt_xZoomOut);
			
		scaleIncY = GUIFactory.createNavBtn(StringRes.icon_zoomIn);
		scaleIncY.setToolTipText(StringRes.tt_yZoomIn);
		
		scaleDecXY = GUIFactory.createNavBtn(StringRes.icon_zoomOut);
		scaleDecXY.setToolTipText(StringRes.tt_xyZoomOut);
		
		scaleDecY = GUIFactory.createNavBtn(StringRes.icon_zoomOut);
		scaleDecY.setToolTipText(StringRes.tt_yZoomOut);
		
		zoomBtn = GUIFactory.createNavBtn(StringRes.icon_zoomAll);
		zoomBtn.setToolTipText(StringRes.tt_home);
	}
	
	/* Colors the divider of a JSplitPane */
	private void colorDivider(JSplitPane sPane) {
		
		sPane.setUI(new BasicSplitPaneUI() {
			
	        public BasicSplitPaneDivider createDefaultDivider() {
	        	
		        return new BasicSplitPaneDivider(this) {
		        	
					private static final long serialVersionUID = 1L;
	
					public void setBorder(Border b) {}
	
		            @Override
	                public void paint(Graphics g) {
		               
		            	g.setColor(GUIFactory.DARK_BG);
		                g.fillRect(0, 0, getSize().width, getSize().height);
	                    super.paint(g);
	                }
		        };
	        }
	    });
	}
	
	/**
	 * Controls the display of the search and tree toggle buttons.
	 * @param hasTrees
	 */
	private void setOptionButtons() {
		
		firstPanel.removeAll();
		
		if(tvFrame.isLoaded()) firstPanel.add(searchBtn);
		
		firstPanel.revalidate();
		firstPanel.repaint();
	}
	
	public void searchLabels() {
		
		geneFinderBox.seekAll();
		arrayFinderBox.seekAll();
	}

	// Add Button Listeners
	/**
	 * Adds an ActionListener to the scale buttons in DendroView.
	 * 
	 * @param l
	 */
	public void addScaleListeners(final ActionListener l) {

		scaleIncX.addActionListener(l);
		scaleIncXY.addActionListener(l);
		scaleDecX.addActionListener(l);
		scaleIncY.addActionListener(l);
		scaleDecY.addActionListener(l);
		scaleDecXY.addActionListener(l);
		scaleDefaultAll.addActionListener(l);
	}

	/**
	 * Adds an ActionListener to the Zoom button in DendroView.
	 * 
	 * @param l
	 */
	public void addZoomListener(final ActionListener l) {

		zoomBtn.addActionListener(l);
	}

	/**
	 * Adds a component listener to the main panel of DendroView.
	 * 
	 * @param l
	 */
	public void addCompListener(final ComponentListener l) {

		getDendroPane().addComponentListener(l);
	}
	
	/**
	 * Adds a component listener to the main panel of DendroView.
	 * 
	 * @param l
	 */
	public void addContListener(final ContainerListener l) {

		getDendroPane().addContainerListener(l);
	}

	/**
	 * First removes all listeners from searchBtn, then adds one new listener.
	 * @param l
	 */
	public void addSearchBtnListener(final ActionListener l) {

		if(getSearchBtn().getActionListeners().length == 0) {
			getSearchBtn().addActionListener(l);
		}
	}

	// Methods
	/**
	 * 
	 * @param o
	 * @param arg
	 */
	@Override
	public void update(final Observable o, final Object arg) {
		
//		LogBuffer.println("Update in DendroView, Observable: " + o.getClass());

//		if (o == geneSelection) {
//			gtrview.scrollToNode(geneSelection.getSelectedNode());
//
//		} else if (o == arraySelection) {
//			atrview.scrollToNode(arraySelection.getSelectedNode());
//		}
	}

	/**
	 * registers a modelview with the viewFrame.
	 * 
	 * @param modelView
	 *            The ModelView to be added
	 */
	private void registerView(final ModelView modelView) {

		modelView.setViewFrame(tvFrame);
		modelView.setStatusPanel(dataTicker);
	}

	/**
	 * Changes the visibility of dendrograms and resets the layout.
	 * 
	 * @param visible
	 */
	public void setTreeVisibility(final double atr_loc, final double gtr_loc) {
		
		if(atrPane != null) atrPane.setDividerLocation(atr_loc);
		if(gtrPane != null) gtrPane.setDividerLocation(gtr_loc);
	
		if(Helper.nearlyEqual(atr_loc, 0.0) 
				&& Helper.nearlyEqual(gtr_loc, 0.0)) {
			showTreesMenuItem.setText("Show trees...");
			
		} else {
			showTreesMenuItem.setText("Hide trees...");
		}
		
		dendroPane.repaint();
	}
	
	/**
	 * Returns information about the current alignment of TextView and
	 * ArrayNameView
	 * @return [isRowRight, isColRight]
	 */
	public boolean[] getLabelAligns() {
		
		boolean[] alignments = {getTextview().getJustifyOption(), 
				getArraynameview().getJustifyOption()};
		
		return alignments;
	}
	
	/**
	 * Sets the label alignment for TextView and ArrayNameView.
	 * @param isRowRight TextView label justification.
	 * @param isColRight ArrayNameView label justification.
	 */
	public void setLabelAlignment(boolean isRowRight, boolean isColRight) {
		
		if(getTextview() == null || getArraynameview() == null) return;
		
		getTextview().setJustifyOption(isRowRight);
		getArraynameview().setJustifyOption(isColRight);
	}

	/**
	 * Opens a JWindow containing Swing components used to search data by name
	 * in the loaded TVModel.
	 */
	@Override
	public void openSearchDialog(final HeaderInfo geneHI, 
			final HeaderInfo arrayHI) {

//		final JDialog dialog = new JDialog();
//		dialog.setModalityType(JDialog.DEFAULT_MODALITY_TYPE);
//		dialog.setTitle(StringRes.dlg_search);
//		dialog.setResizable(false);
//
//		final JPanel container = GUIFactory.createJPanel(true, 
//				GUIFactory.DEFAULT, null);
//		
//		JLabel wildTip = GUIFactory.createLabel("You can use wildcards "
//				+ "to search (*, ?).", GUIFactory.FONTS);
//		
//		JLabel wildTip2 = GUIFactory.createLabel("E.g.: *complex* --> "
//				+ "Rpd3s complex, ATP Synthase (complex V), etc...", 
//				GUIFactory.FONTS);
//
//		container.add(wildTip, "span, wrap");
//		container.add(wildTip2, "span, wrap");
//		//Adding arrayHI in order to be able to determine where the selected cells are and if they are currently visible
//		container.add(setGeneFinderPanel(geneHI,arrayHI), "w 90%, h 40%, "
//				+ "alignx 50%, wrap");
//		//Adding geneHI in order to be able to determine where the selected cells are and if they are currently visible
//		container.add(setArrayFinderPanel(arrayHI,geneHI), "w 90%, h 40%, "
//				+ "alignx 50%");
//		
//		//Added this to de-select anything that was selected prior to
//		//clicking the search button so that the first search would not
//		//be restricted to what was selected prior to clicking the search
//		//button
//		tvFrame.getGeneSelection().deselectAllIndexes();
//		tvFrame.getGeneSelection().notifyObservers();
//		tvFrame.getArraySelection().deselectAllIndexes();
//		tvFrame.getArraySelection().notifyObservers();
//
//		dialog.getContentPane().add(container);
//		dialog.pack();
//		dialog.setLocationRelativeTo(tvFrame.getAppFrame());
//		dialog.setVisible(true);
	}

	// @Override
	// public void populateExportMenu(final TreeviewMenuBarI menu) {
	//
	// menu.addMenuItem("Export to Postscript...");
	// , new ActionListener() {
	//
	// @Override
	// public void actionPerformed(final ActionEvent actionEvent) {
	//
	// MapContainer initXmap, initYmap;
	//
	// // if ((getArraySelection().getNSelectedIndexes() != 0) ||
	// // (getGeneSelection().getNSelectedIndexes() != 0)) {
	// // initXmap = getZoomXmap();
	// // initYmap = getZoomYmap();
	// //
	// // } else {
	// initXmap = getGlobalXmap();
	// initYmap = getGlobalYmap();
	// // }
	//
	// final PostscriptExportPanel psePanel = setupPostscriptExport(
	// initXmap, initYmap);
	//
	// final JDialog popup = new CancelableSettingsDialog(viewFrame,
	// "Export to Postscript", psePanel);
	// popup.pack();
	// popup.setVisible(true);
	// }
	// });
	// menu.setAccelerator(KeyEvent.VK_X);
	// menu.setMnemonic(KeyEvent.VK_X);
	//
	// menu.addMenuItem("Export to Image...");
	// , new ActionListener() {
	//
	// @Override
	// public void actionPerformed(final ActionEvent actionEvent) {
	//
	// MapContainer initXmap, initYmap;
	// // if ((getArraySelection().getNSelectedIndexes() != 0) ||
	// // (getGeneSelection().getNSelectedIndexes() != 0)) {
	// // initXmap = getZoomXmap();
	// // initYmap = getZoomYmap();
	// //
	// // } else {
	// initXmap = getGlobalXmap();
	// initYmap = getGlobalYmap();
	// // }
	//
	// final BitmapExportPanel bitmapPanel = setupBitmapExport(
	// initXmap, initYmap);
	//
	// final JDialog popup = new CancelableSettingsDialog(viewFrame,
	// "Export to Image", bitmapPanel);
	// popup.pack();
	// popup.setVisible(true);
	// }
	// });
	// menu.setMnemonic(KeyEvent.VK_I);
	//
	// menu.addMenuItem("Export ColorBar to Postscript...");
	// , new ActionListener() {
	//
	// @Override
	// public void actionPerformed(final ActionEvent actionEvent) {
	//
	// final PostscriptColorBarExportPanel gcbPanel =
	// new PostscriptColorBarExportPanel(
	// ((DoubleArrayDrawer) arrayDrawer)
	// .getColorExtractor());
	//
	// gcbPanel.setSourceSet(getDataModel().getFileSet());
	//
	// final JDialog popup = new CancelableSettingsDialog(
	// viewFrame, "Export ColorBar to Postscript",
	// gcbPanel);
	// popup.pack();
	// popup.setVisible(true);
	// }
	// });
	// menu.setMnemonic(KeyEvent.VK_B);
	//
	// menu.addMenuItem("Export ColorBar to Image...");
	// , new ActionListener() {
	//
	// @Override
	// public void actionPerformed(final ActionEvent actionEvent) {
	//
	// final BitmapColorBarExportPanel gcbPanel =
	// new BitmapColorBarExportPanel(
	// ((DoubleArrayDrawer) arrayDrawer).getColorExtractor());
	//
	// gcbPanel.setSourceSet(getDataModel().getFileSet());
	//
	// final JDialog popup = new CancelableSettingsDialog(viewFrame,
	// "Export ColorBar to Image", gcbPanel);
	// popup.pack();
	// popup.setVisible(true);
	// }
	// });
	// menu.setMnemonic(KeyEvent.VK_M);
	//
	// menu.addSeparator();
	// addSimpleExportOptions(menu);
	// }

	private void addSimpleExportOptions(final TreeviewMenuBarI menu) {

		menu.addMenuItem("Save Tree Image");
		// , new ActionListener() {
		//
		// @Override
		// public void actionPerformed(final ActionEvent actionEvent) {
		//
		// MapContainer initXmap, initYmap;
		// initXmap = getGlobalXmap();
		// initYmap = getGlobalYmap();
		//
		// final BitmapExportPanel bitmapPanel = new BitmapExportPanel(
		// arraynameview.getHeaderInfo(), getDataModel()
		// .getGeneHeaderInfo(), getGeneSelection(),
		// getArraySelection(), invertedTreeDrawer,
		// leftTreeDrawer, arrayDrawer, initXmap, initYmap);
		//
		// bitmapPanel.setGeneFont(textview.getFont());
		// bitmapPanel.setArrayFont(arraynameview.getFont());
		// bitmapPanel.setSourceSet(getDataModel().getFileSet());
		// bitmapPanel.setDrawSelected(false);
		// bitmapPanel.includeData(false);
		// bitmapPanel.includeAtr(false);
		// bitmapPanel.deselectHeaders();
		//
		// final JDialog popup = new CancelableSettingsDialog(viewFrame,
		// "Export to Image", bitmapPanel);
		// popup.pack();
		// popup.setVisible(true);
		// }
		// });
		menu.setMnemonic(KeyEvent.VK_T);

		menu.addMenuItem("Save Thumbnail Image");
		// , new ActionListener() {
		//
		// @Override
		// public void actionPerformed(final ActionEvent actionEvent) {
		//
		// MapContainer initXmap, initYmap;
		// initXmap = getGlobalXmap();
		// initYmap = getGlobalYmap();
		//
		// final BitmapExportPanel bitmapPanel = new BitmapExportPanel(
		// arraynameview.getHeaderInfo(), getDataModel()
		// .getGeneHeaderInfo(), getGeneSelection(),
		// getArraySelection(), invertedTreeDrawer,
		// leftTreeDrawer, arrayDrawer, initXmap, initYmap);
		//
		// bitmapPanel.setSourceSet(getDataModel().getFileSet());
		// bitmapPanel.setGeneFont(textview.getFont());
		// bitmapPanel.setArrayFont(arraynameview.getFont());
		// bitmapPanel.setDrawSelected(false);
		// bitmapPanel.includeGtr(false);
		// bitmapPanel.includeAtr(false);
		// bitmapPanel.deselectHeaders();
		//
		// final JDialog popup = new CancelableSettingsDialog(viewFrame,
		// "Export To Image", bitmapPanel);
		// popup.pack();
		// popup.setVisible(true);
		// }
		// });
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

	// Populate Menus
	/**
	 * adds DendroView stuff to Analysis menu
	 * 
	 * @param menu
	 *            menu to add to
	 */
	// @Override
	// public void populateAnalysisMenu(final TreeviewMenuBarI menu) {
	//
	// menu.addMenuItem("Flip Array Tree Node", new ActionListener() {
	//
	// @Override
	// public void actionPerformed(final ActionEvent ae) {
	//
	// if (getGtrview().hasFocus()) {
	//
	// flipSelectedGTRNode();
	// } else {
	//
	// flipSelectedATRNode();
	// }
	// }
	// });
	// menu.setAccelerator(KeyEvent.VK_L);
	// menu.setMnemonic(KeyEvent.VK_A);
	//
	// menu.addMenuItem("Flip Gene Tree Node", new ActionListener() {
	//
	// @Override
	// public void actionPerformed(final ActionEvent ae) {
	//
	// flipSelectedGTRNode();
	// }
	// });
	// menu.setMnemonic(KeyEvent.VK_G);
	//
	// menu.addMenuItem("Align to Tree...", new ActionListener() {
	//
	// @Override
	// public void actionPerformed(final ActionEvent ae) {
	//
	// try {
	//
	// final FileSet fileSet = offerATRFileSelection();
	// final AtrTVModel atrModel = makeAtrModel(fileSet);
	//
	// alignAtrToModel(atrModel);
	// } catch (final LoadException e) {
	//
	// if ((e.getType() != LoadException.INTPARSE)
	// && (e.getType() != LoadException.NOFILE)) {
	// LogBuffer.println("Could not open file: "
	// + e.getMessage());
	// e.printStackTrace();
	// }
	// }
	// }
	// });
	// menu.setAccelerator(KeyEvent.VK_A);
	// menu.setMnemonic(KeyEvent.VK_G);
	//
	// menu.addMenuItem("Compare to...", new ActionListener() {
	//
	// @Override
	// public void actionPerformed(final ActionEvent ae) {
	//
	// try {
	//
	// final FileSet fileSet = offerATRFileSelection();
	// final TVModel tvModel = makeCdtModel(fileSet);
	// compareToModel(tvModel);
	// } catch (final LoadException e) {
	//
	// if ((e.getType() != LoadException.INTPARSE)
	// && (e.getType() != LoadException.NOFILE)) {
	// LogBuffer.println("Could not open file: "
	// + e.getMessage());
	// e.printStackTrace();
	// }
	// }
	// }
	// });
	// menu.setAccelerator(KeyEvent.VK_C);
	// menu.setMnemonic(KeyEvent.VK_C);
	//
	// menu.addMenuItem("Remove comparison", new ActionListener() {
	//
	// @Override
	// public void actionPerformed(final ActionEvent ae) {
	//
	// getDataModel().removeAppended();
	// globalXmap.setIndexRange(0, getDataModel().getDataMatrix()
	// .getNumCol() - 1);
	// globalXmap.notifyObservers();
	//
	// ((Observable) getDataModel()).notifyObservers();
	// }
	// });
	// menu.setAccelerator(KeyEvent.VK_R);
	// menu.setMnemonic(KeyEvent.VK_R);

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
	// }

	// /**
	// * adds DendroView stuff to Document menu
	// *
	// * @param menu
	// * menu to add to
	// */
	// @Override
	// public void populateSettingsMenu(final TreeviewMenuBarI menu) {
	//
	// annotationsMenuItem = (JMenuItem) menu.addMenuItem(
	// "Row and Column Labels", 0);
	// menu.setMnemonic(KeyEvent.VK_R);
	// tvFrame.addToMenuList(annotationsMenuItem);
	//
	// colorMenuItem = (JMenuItem) menu.addMenuItem("Color Settings", 1);
	// menu.setMnemonic(KeyEvent.VK_C);
	// tvFrame.addToMenuList(colorMenuItem);
	// }

	@Override
	public void addDendroMenus(final JMenu menu) {

		annotationsMenuItem = new JMenuItem(StringRes.menu_RowAndCol);
		menu.add(annotationsMenuItem);
		tvFrame.addToStackMenuList(annotationsMenuItem);

		colorMenuItem = new JMenuItem(StringRes.menu_Color);
		menu.add(colorMenuItem);
		tvFrame.addToStackMenuList(colorMenuItem);
		
		menu.addSeparator();
		
		matrixMenu = new JMenu("Matrix Size");
		menu.add(matrixMenu);
		
		JMenuItem fillScreenMenuItem = new JMenuItem("Fill screen");
		matrixMenu.add(fillScreenMenuItem);
		fillScreenMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, 
				Event.ALT_MASK));
		tvFrame.addToStackMenuList(fillScreenMenuItem);
		
		JMenuItem equalAxesMenuItem = new JMenuItem("Equal axes");
		matrixMenu.add(equalAxesMenuItem);
		equalAxesMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, 
				Event.ALT_MASK));
		tvFrame.addToStackMenuList(equalAxesMenuItem);
		
		JMenuItem proportMatrixMenuItem = new JMenuItem("Proportional axes");
		matrixMenu.add(proportMatrixMenuItem);
		proportMatrixMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, 
				Event.ALT_MASK));
		tvFrame.addToStackMenuList(proportMatrixMenuItem);
		
		menu.addSeparator();
		
		showTreesMenuItem = new JMenuItem("Show trees...");
		menu.add(showTreesMenuItem);
		showTreesMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, 
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		tvFrame.addToStackMenuList(showTreesMenuItem);
		
//		
//		isolateMenu = new JMenuItem("Isolate Selected");
//		menu.add(isolateMenu);
//		tvFrame.addToStackMenuList(isolateMenu);
	}

	@Override
	public void addClusterMenus(final JMenu menu) {

		// Cluster Menu
		final JMenuItem hierMenuItem = new JMenuItem(
				StringRes.menu_Hier);
		hierMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menu.add(hierMenuItem);
		tvFrame.addToStackMenuList(hierMenuItem);

//		final JMenuItem kMeansMenuItem = new JMenuItem(
//				StringRes.menu_KMeans);
//		menu.add(kMeansMenuItem);
//		tvFrame.addToStackMenuList(kMeansMenuItem);
		
	}
	
	@Override
	public void addSearchMenus(final JMenu menu) {
		
		final JMenuItem searchMenuItem = new JMenuItem("Labels");
		searchMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menu.add(searchMenuItem);
		tvFrame.addToStackMenuList(searchMenuItem);
	}

	// /**
	// * @param initXmap
	// * @param initYmap
	// * @return
	// */
	// private PostscriptExportPanel setupPostscriptExport(
	// final MapContainer initXmap, final MapContainer initYmap) {
	//
	// final PostscriptExportPanel psePanel = new PostscriptExportPanel(
	// arraynameview.getHeaderInfo(), getDataModel()
	// .getGeneHeaderInfo(), getGeneSelection(),
	// getArraySelection(), invertedTreeDrawer, leftTreeDrawer,
	// arrayDrawer, initXmap, initYmap);
	//
	// psePanel.setSourceSet(getDataModel().getFileSet());
	// psePanel.setGeneFont(textview.getFont());
	// psePanel.setArrayFont(arraynameview.getFont());
	// psePanel.setIncludedArrayHeaders(arraynameview.getHeaderSummary()
	// .getIncluded());
	// psePanel.setIncludedGeneHeaders(textview.getHeaderSummary()
	// .getIncluded());
	//
	// return psePanel;
	// }
	//
	// private BitmapExportPanel setupBitmapExport(final MapContainer initXmap,
	// final MapContainer initYmap) {
	//
	// final BitmapExportPanel bitmapPanel = new BitmapExportPanel(
	// arraynameview.getHeaderInfo(), getDataModel()
	// .getGeneHeaderInfo(), getGeneSelection(),
	// getArraySelection(), invertedTreeDrawer, leftTreeDrawer,
	// arrayDrawer, initXmap, initYmap);
	//
	// bitmapPanel.setSourceSet(getDataModel().getFileSet());
	// bitmapPanel.setGeneFont(textview.getFont());
	// bitmapPanel.setArrayFont(arraynameview.getFont());
	// bitmapPanel.setIncludedArrayHeaders(arraynameview.getHeaderSummary()
	// .getIncluded());
	// bitmapPanel.setIncludedGeneHeaders(textview.getHeaderSummary()
	// .getIncluded());
	//
	// return bitmapPanel;
	// }

	// @Override
	// public void export(final MainProgramArgs mainArgs) throws ExportException
	// {
	//
	// final DendroviewArgs args = new DendroviewArgs(mainArgs.remainingArgs());
	//
	// if (args.getFilePath() == null) {
	// System.err.println("Error, must specify an output file\n");
	// args.printUsage();
	//
	// return;
	// }
	//
	// final ExportPanel exporter;
	//
	// if ("ps".equalsIgnoreCase(args.getExportType())) {
	// exporter = setupPostscriptExport(getGlobalXmap(), getGlobalYmap());
	//
	// } else if ("png".equalsIgnoreCase(args.getExportType())
	// || "gif".equalsIgnoreCase(args.getExportType())) {
	// exporter = setupBitmapExport(getGlobalXmap(), getGlobalYmap());
	//
	// } else {
	// System.err.println("Error, unrecognized output format "
	// + args.getExportType() + " \n");
	//
	// args.printUsage();
	// exporter = null;
	// }
	//
	// if (exporter != null) {
	// exporter.setFilePath(args.getFilePath());
	// exporter.setIncludedArrayHeaders(args.getArrayHeaders());
	// exporter.setIncludedGeneHeaders(args.getGeneHeaders());
	//
	// if (args.getXScale() != null) {
	// exporter.setXscale(args.getXScale());
	// }
	//
	// if (args.getYScale() != null) {
	// exporter.setYscale(args.getYScale());
	// }
	//
	// if (args.getContrast() != null) {
	// colorExtractor.setContrast(args.getContrast());
	// }
	//
	// if (args.getGtrWidth() != null) {
	// exporter.setExplicitGtrWidth(args.getGtrWidth());
	// }
	//
	// if (args.getAtrHeight() != null) {
	// exporter.setExplicitAtrHeight(args.getAtrHeight());
	// }
	//
	// if (args.getLogcenter() != null) {
	// colorExtractor.setLogCenter(args.getLogcenter());
	// colorExtractor.setLogBase(2.0);
	// colorExtractor.setLogTransform(true);
	// }
	//
	// exporter.setArrayAnnoInside(args.getArrayAnnoInside());
	// exporter.save();
	// }
	// }
	
	// Set GlobalView sizes
	public void setGVWidth(double newWidth) {
		
		this.gvWidth = newWidth;
	}
	
	public void setGVHeight(double newHeight) {
		
		this.gvHeight = newHeight;
	}
	
	// Get GlobalView sizes
	public double getGVWidth() {
		
		return gvWidth;
	}
	
	public double getGVHeight() {
		
		return gvHeight;
	}

	/**
	 * Setter for viewFrame
	 */
	public void setViewFrame(final TreeViewFrame viewFrame) {

		this.tvFrame = viewFrame;
	}

	public void setSearchTermBoxes(HeaderInfo geneHI, HeaderInfo arrayHI,
			MapContainer xmap, MapContainer ymap) {

		this.geneFinderBox = new GeneFinderBox(tvFrame,
				geneHI, getTextview().getHeaderSummary(), 
				tvFrame.getGeneSelection(), ymap, xmap, 
				tvFrame.getArraySelection(), arrayHI);
		
		this.arrayFinderBox = new ArrayFinderBox(tvFrame,
				arrayHI, getArraynameview().getHeaderSummary(),
				tvFrame.getArraySelection(), xmap, ymap, 
				tvFrame.getGeneSelection(), geneHI);

//		final JPanel contentPanel = geneFinderBox.getContentPanel();
		
		//Added this as a test/kludge to de-select anything that was selected prior to clicking the search button
		//geneFinderBox.seekAll();

//		return contentPanel;
	}

//	/**
//	 * Getter for geneFinderPanel
//	 * 
//	 * @return HeaderFinderPanel arrayFinderPanel
//	 */
//	public void setArrayFinderPanel(HeaderInfo arrayHI, HeaderInfo geneHI,
//			MapContainer xmap, MapContainer ymap) {
//
//		//LogBuffer.println("Creating array finder box.  Current visible array start index is: [" + globalYmap.getFirstVisible() + "].  Num visible array indexes is: [" + globalYmap.getNumVisible() + "].");
//		arrayFinderBox = new ArrayFinderBox(tvFrame,
//				arrayHI, getArraynameview().getHeaderSummary(),
//				tvFrame.getArraySelection(), xmap, ymap, 
//				tvFrame.getGeneSelection(),geneHI);
//
////		final JPanel contentPanel = arrayFinderBox.getContentPanel();
//		
//		//Added this as a test/kludge to de-select anything that was selected prior to clicking the search button
//		//arrayFinderBox.seekAll();
//
////		return contentPanel;
//	}

	@Override
	public String getName() {

		return name;
	}

	public void setName(final String name) {

		this.name = name;
	}

	// Getters
	public JButton getXPlusButton() {

		return scaleIncX;
	}
	
	public JButton getXYPlusButton() {

		return scaleIncXY;
	}

	public JButton getXMinusButton() {

		return scaleDecX;
	}

	public JButton getYPlusButton() {

		return scaleIncY;
	}

	public JButton getYMinusButton() {

		return scaleDecY;
	}
	
	public JButton getXYMinusButton() {

		return scaleDecXY;
	}

	public JButton getHomeButton() {

		return scaleDefaultAll;
	}

	public JScrollBar getXScroll() {

		return globalXscrollbar;
	}

	public void setXScroll(final int i) {

		globalXscrollbar.setValue(i);
	}

	public JScrollBar getYScroll() {

		return globalYscrollbar;
	}

	public void setYScroll(final int i) {

		globalYscrollbar.setValue(i);
	}

	public GlobalView getGlobalView() {

		return globalview;
	}

	public JPanel getDendroPane() {

		return dendroPane;
	}
	
	public LabelView getArraynameview() {

		return arraynameview;
	}
	
	public ATRView getAtrview() {

		return atrview;
	}
	
	public GTRView getGtrview() {

		return gtrview;
	}
	
	public LabelView getTextview() {

		return textview;
	}
	
	/**
	 * Returns the current location of the divider in the supplied TRView's
	 * JSplitPane
	 * @param dendrogram
	 * @return Location of divider (max = 1.0)
	 */
	public double getDivLoc(TRView dendrogram) {
		
		/* Get value for correct dendrogram JSplitPane */
		JSplitPane treePane = (dendrogram == atrview) ? atrPane : gtrPane;
		
		/* returns imprecise position? -- no bug reports found */
		double abs_div_loc = (double)treePane.getDividerLocation();
		double max_div_loc = treePane.getMaximumDividerLocation();
		
		/* Round the value */
		int tmp = (int) ((abs_div_loc / max_div_loc) * 100);
		double rel_div_loc = tmp / 100.0;
		
		return (rel_div_loc > 1.0) ? 1.0 : rel_div_loc;
	}

	/**
	 * Getter for viewFrame
	 */
	public ViewFrame getViewFrame() {

		return tvFrame;
	}

	public JButton getSearchBtn() {

		return searchBtn;
	}

	/**
	 * Returns a boolean which indicates whether the dendrogram ModelViews are
	 * enabled.
	 * 
	 * @return
	 */
	public boolean treesEnabled() {
		
		boolean treesEnabled = gtrview.isEnabled() || atrview.isEnabled();
		return treesEnabled;
	}
}
