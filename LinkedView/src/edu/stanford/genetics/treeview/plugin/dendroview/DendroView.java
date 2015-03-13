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

import java.awt.Graphics;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
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
import edu.stanford.genetics.treeview.DataTicker;
import edu.stanford.genetics.treeview.DendroPanel;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.core.ColumnFinderBox;
import edu.stanford.genetics.treeview.core.HeaderFinderBox;
import edu.stanford.genetics.treeview.core.RowFinderBox;

/**
 * TODO Refactor this JavaDoc. It's not applicable to the current program
 * anymore.
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


	// Container JFrame
	protected TreeViewFrame tvFrame;

	private String name;

	// Main containers
	private final JPanel dendroPane;
//	private JPanel firstPanel;

	protected ScrollPane panes[];

	// Matrix view
	private final GlobalView globalview;

	// Tree views
	protected final RowTreeView rowTreeView;
	protected final ColumnTreeView colTreeView;

	/* JSplitPanes containing trees & labels */
	private JSplitPane rowDataPane;
	private JSplitPane colDataPane;

	/* Gene and array label views */
	protected final RowLabelView rowLabelView;
	protected final ColumnLabelView colLabelView;

	/* JScrollBars for GlobalView */
	/* TODO one glorious day, update GlobalView to a scrollpane... */
	protected JScrollBar matrixXscrollbar;
	protected JScrollBar matrixYscrollbar;

	protected final DataTicker dataTicker;

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

	/* Search related buttons */
	private JButton searchBtn;
	private JButton searchCloseBtn;

	private HeaderFinderBox rowFinderBox;
	private HeaderFinderBox colFinderBox;

	/* GlobalView default sizes */
	/* TODO needed? ... */
	private double gvWidth;
	private double gvHeight;

	private boolean isSearchVisible;

	/* Maximum GlobalView dimensions in percent */
	public static final double MAX_GV_WIDTH = 75;
	public static final double MAX_GV_HEIGHT = 80;

	/*
	 * MapContainers map tile size (scale) to selection rectangles in
	 * GlobalView, label & tree positions
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
		dendroPane = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING_FILL);
//		dendroPane.setLayout(new MigLayout("debug"));

		/* >>> Init all views --- they should be final <<< */
		/* data ticker panel */
		dataTicker = new DataTicker();

		/* Create the Global view (JPanel to display) */
		globalview = new GlobalView();

		/* scrollbars, mostly used by maps */
		matrixXscrollbar = globalview.getXScroll();
		matrixYscrollbar = globalview.getYScroll();

		/* Set up the gene label display */
		rowLabelView = new RowLabelView();

		/* Set up the array label display */
		colLabelView = new ColumnLabelView();
		// arraynameview.setUrlExtractor(viewFrame.getArrayUrlExtractor());

		// Set up row dendrogram
		rowTreeView = new RowTreeView();
		rowTreeView.getHeaderSummary().setIncluded(new int[] { 0, 3 });

		// Set up column dendrogram
		colTreeView = new ColumnTreeView();
		colTreeView.getHeaderSummary().setIncluded(new int[] { 0, 3 });

		isSearchVisible = false;

		setupScaleButtons();
	}

	public void setGlobalXMap(final MapContainer xmap) {

		this.globalXmap = xmap;
	}

	public void setGlobalYMap(final MapContainer ymap) {

		this.globalYmap = ymap;
	}

	public void setMatrixHome(final boolean isHome) {

		globalview.resetHome(isHome);
	}

	public void setSearchVisible(final boolean visible) {

		this.isSearchVisible = visible;
		setupLayout();
	}

	public boolean isSearchVisible() {

		return isSearchVisible;
	}

	/**
	 * Returns the dendroPane so it can be displayed in TVFrame.
	 *
	 * @return JPanel dendroPane
	 */
	public JPanel makeDendro() {

		colLabelView.generateView(tvFrame.getUrlExtractor());
		rowLabelView.generateView(tvFrame.getUrlExtractor());

		globalview.setHeaderSummary(rowLabelView.getHeaderSummary(),
				colLabelView.getHeaderSummary());

		// Register Views
		registerView(globalview);
		registerView(colTreeView);
		registerView(colLabelView);
		registerView(rowLabelView);
		registerView(rowTreeView);

		return dendroPane;
	}

	/**
	 * Setup the search panel which contains tow editable JComboBoxes containing
	 * all labels for each axis.
	 *
	 * @return JPanel
	 */
	private JPanel makeSearchPanel() {

		if (rowFinderBox == null || colFinderBox == null)
			return null;

		final JPanel bgPanel = GUIFactory.createJPanel(false,
				GUIFactory.NO_PADDING, null);

		final JPanel searchPanel = GUIFactory.createJPanel(true,
				GUIFactory.DEFAULT, GUIFactory.DARK_BG);

		final String tooltip = "You can use wildcards to search (*, ?). "
				+ "E.g.: *complex* --> Rpd3s complex, ATP Synthase "
				+ "(complex V), etc...";
		searchPanel.setToolTipText(tooltip);

		searchPanel.add(searchCloseBtn, "split 4, push, al right");
		searchPanel.add(rowFinderBox.getSearchTermBox(), "pushx");
		searchPanel.add(colFinderBox.getSearchTermBox(), "pushx");
		searchPanel.add(searchBtn);

		bgPanel.add(searchPanel, "shrink 100, push, al right");
		return bgPanel;
	}

	/**
	 * Initializes the objects associated with label search. These are the
	 * JComboBoxes containing all the label names as well as the buttons on that
	 * panel.
	 *
	 * @param geneHI
	 * @param arrayHI
	 * @param xmap
	 * @param ymap
	 */
	public void setupSearch(final HeaderInfo geneHI, final HeaderInfo arrayHI,
			final MapContainer xmap, final MapContainer ymap) {

		setSearchTermBoxes();
		updateSearchTermBoxes(geneHI, arrayHI, xmap, ymap);

		searchBtn = GUIFactory.createIconBtn("searchIcon");
		searchBtn.setBorder(null);
		searchBtn.setToolTipText(StringRes.tt_searchRowCol);

		/* Init here for listener addition in DendroController */
		searchCloseBtn = GUIFactory.createIconBtn("close_x.png");
		searchCloseBtn.setBorder(null);
		searchCloseBtn.setBackground(null);
	}

	/**
	 * Manages the component layout in TreeViewFrame
	 */
	public void setupLayout() {

		/* Clear dendroPane first */
		dendroPane.removeAll();

		/* Panels for layout setup */
		JPanel globalOverviewPanel;
		JPanel crossPanel;
		JPanel zoomXPanel;
		JPanel zoomYPanel;
		JPanel rowLabelPanel;
		JPanel colLabelPanel;
		JPanel columnNavPanel;
		JPanel rowNavPanel;
		JPanel navContainer;
		JPanel bottomPanel;

		globalOverviewPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);
		globalOverviewPanel.setBorder(
				BorderFactory.createTitledBorder("Overview"));
		
		crossPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);


		navContainer = GUIFactory.createJPanel(false,
				GUIFactory.NO_PADDING);
//		navContainer.setLayout(new MigLayout("debug"));

		bottomPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);

		columnNavPanel = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING);
		rowNavPanel = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING);
		zoomXPanel = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING);
		zoomYPanel = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING);

		rowLabelPanel = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING);
		colLabelPanel = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING);

		rowDataPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rowTreeView,
				rowLabelPanel);
		rowDataPane.setResizeWeight(0.5);
		rowDataPane.setOpaque(false);
		rowDataPane.setOneTouchExpandable(true); // does not work on Linux :(

		colorDivider(rowDataPane);
		rowDataPane.setBorder(null);

		final double oldRowDiv = tvFrame.getConfigNode().getDouble("gtr_loc",
				0.5);
		if (rowTreeView.isEnabled()) {
			rowDataPane.setDividerLocation(oldRowDiv);
		} else {
			rowDataPane.setDividerLocation(0.0);
			rowDataPane.setEnabled(false);
		}

		colDataPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, colTreeView,
				colLabelPanel);
		colDataPane.setResizeWeight(0.0);
		colDataPane.setOpaque(false);
		colDataPane.setOneTouchExpandable(true);

		colorDivider(colDataPane);
		colDataPane.setBorder(null);

		final double oldColDiv = tvFrame.getConfigNode().getDouble("atr_loc",
				0.5);
		if (colTreeView.isEnabled()) {
			colDataPane.setDividerLocation(oldColDiv);
		} else {
			colDataPane.setDividerLocation(0.0);
			colDataPane.setEnabled(false);
		}

		/* If trees in general are disabled */
		if (!treesEnabled() && showTreesMenuItem != null) {
			showTreesMenuItem.setEnabled(false);
		} else {
			/* If trees are visible from the start */
			if (oldRowDiv > 0.0 || oldColDiv > 0.0) {
				showTreesMenuItem.setText(StringRes.menu_hideTrees);
			}
		}

		rowLabelPanel.add(rowLabelView.getComponent(), "push, grow");
		colLabelPanel.add(colLabelView.getComponent(), "push, grow");
		
		zoomXPanel.add(scaleDecX);
		zoomXPanel.add(scaleIncX);
		
		zoomYPanel.add(scaleDecY, "wrap");
		zoomYPanel.add(scaleIncY);
				
		crossPanel.add(scaleDecXY);
		crossPanel.add(zoomBtn);
		crossPanel.add(scaleIncXY, "wrap");
		crossPanel.add(scaleDefaultAll, "span, pushx, alignx 50%");

		navContainer.add(crossPanel, "push, alignx 50%, wrap");
		navContainer.add(dataTicker.getTickerPanel(), "pushx, grow, w 95%, "
				+ "h 25%, wrap");
		
		/* Add the scrollbars (outside of LabelViews) */
		final JScrollBar colLabelScroll = colLabelView.getScrollBar();
		final JScrollBar rowLabelScroll = rowLabelView.getScrollBar();
		
		/* Panels for scrollbars and axis-zoom buttons */
		columnNavPanel.add(matrixXscrollbar, "growx, pushx");
		columnNavPanel.add(zoomXPanel);
		
		rowNavPanel.add(matrixYscrollbar, "growy, pushy, wrap");
		rowNavPanel.add(zoomYPanel);
		
		
		/* Adding elements to the main JPanel */
		dendroPane.add(globalOverviewPanel, "w 12.5%!, h 19%!");

		/* Column tree view */
		dendroPane.add(colDataPane, "w 74%, h 19%!");
		dendroPane.add(colLabelScroll, "h 19%!");

		/* Navigation panel */
		dendroPane.add(navContainer, "span 1 3, w 12.5%!, h 100%, wrap");

		/* Row tree view */
		dendroPane.add(rowDataPane, "w 12.5%!, h 79%");
		
		/* Matrix view */
		dendroPane.add(globalview, "w 74%, h 79%, grow, push");
		dendroPane.add(rowNavPanel, "growy, h 95%, wrap");
		
		dendroPane.add(rowLabelScroll, "w 12.5%!");
		
		dendroPane.add(columnNavPanel, "growx, wrap");

		/* Bottom panel for spacing */
		dendroPane.add(bottomPanel, "span, w 87.5%, h 1%");
				
		dendroPane.revalidate();
		dendroPane.repaint();
	}

	/* Sets up the buttons which control scaling and zooming */
	private void setupScaleButtons() {

		scaleDefaultAll = GUIFactory.createIconBtn(StringRes.icon_home);
		scaleDefaultAll.setToolTipText("Reset the zoomed view");

		scaleIncX = GUIFactory.createSquareBtn("+", 20);
				//GUIFactory.createIconBtn(StringRes.icon_zoomIn);
		scaleIncX.setToolTipText(StringRes.tt_xZoomIn);

		scaleIncXY = GUIFactory.createIconBtn(StringRes.icon_fullZoomIn);
		scaleIncXY.setToolTipText(StringRes.tt_xyZoomIn);

		scaleDecX = GUIFactory.createSquareBtn("-", 20);
				//GUIFactory.createIconBtn(StringRes.icon_zoomOut);
		scaleDecX.setToolTipText(StringRes.tt_xZoomOut);

		scaleIncY = GUIFactory.createSquareBtn("+", 20);
				//GUIFactory.createIconBtn(StringRes.icon_zoomIn);
		scaleIncY.setToolTipText(StringRes.tt_yZoomIn);

		scaleDecXY = GUIFactory.createIconBtn(StringRes.icon_fullZoomOut);
		scaleDecXY.setToolTipText(StringRes.tt_xyZoomOut);

		scaleDecY = GUIFactory.createSquareBtn("-", 20);
				//GUIFactory.createIconBtn(StringRes.icon_zoomOut);
		scaleDecY.setToolTipText(StringRes.tt_yZoomOut);

		zoomBtn = GUIFactory.createIconBtn(StringRes.icon_zoomAll);
		zoomBtn.setToolTipText(StringRes.tt_home);
	}

	/* Colors the divider of a JSplitPane */
	private void colorDivider(final JSplitPane sPane) {

		sPane.setUI(new BasicSplitPaneUI() {

			@Override
			public BasicSplitPaneDivider createDefaultDivider() {

				return new BasicSplitPaneDivider(this) {

					private static final long serialVersionUID = 1L;

					@Override
					public void setBorder(final Border b) {
					}

					@Override
					public void paint(final Graphics g) {

						g.setColor(GUIFactory.DARK_BG);
						g.fillRect(0, 0, getSize().width, getSize().height);
						super.paint(g);
					}
				};
			}
		});
	}

	/**
	 * Initiates a search of of labels for both axes.
	 */
	public void searchLabels() {

		rowFinderBox.seekAll();
		colFinderBox.seekAll();
	}

	public void updateTreeMenuBtn(final JSplitPane srcPane) {

		/* Should always be "Show trees" if any tree panel is invisible */
		if(rowDataPane.getDividerLocation() == 0 
				|| colDataPane.getDividerLocation() == 0) {
			showTreesMenuItem.setText(StringRes.menu_showTrees);
		} else {
			showTreesMenuItem.setText(StringRes.menu_hideTrees);
		}
	}

	/* >>>>>>>>>> UI component listeners <<<<<<<<<< */
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

		dendroPane.addComponentListener(l);
	}

	/**
	 * Adds a component listener to the main panel of DendroView.
	 *
	 * @param l
	 */
	public void addContListener(final ContainerListener l) {

		dendroPane.addContainerListener(l);
	}

	/**
	 * First removes all listeners from searchBtn, then adds one new listener.
	 *
	 * @param l
	 */
	public void addSearchBtnListener(final ActionListener l) {

		if (getSearchBtn().getActionListeners().length == 0) {
			getSearchBtn().addActionListener(l);
		}
	}

	/**
	 * Adds a MouseListener to the close-search JLabel so that the user can
	 * click it in order to close the search panel.
	 *
	 * @param l
	 */
	public void addSearchCloseListener(final ActionListener l) {

		if (searchCloseBtn.getActionListeners().length == 0) {
			searchCloseBtn.addActionListener(l);
		}
	}

	/**
	 * Add listener for divider movement and position for the Tree/ Label
	 * JSplitPanes.
	 *
	 * @param l
	 */
	public void addDividerListener(final PropertyChangeListener l) {

		rowDataPane.addPropertyChangeListener(l);
		colDataPane.addPropertyChangeListener(l);
	}
	
	public void addSplitPaneListener(final ComponentAdapter c) {
		
		rowDataPane.addComponentListener(c);
		colDataPane.addComponentListener(c);
	}

	// Methods
	/**
	 *
	 * @param o
	 * @param arg
	 */
	@Override
	public void update(final Observable o, final Object arg) {


		// if (o == geneSelection) {
		// gtrview.scrollToNode(geneSelection.getSelectedNode());
		//
		// } else if (o == arraySelection) {
		// atrview.scrollToNode(arraySelection.getSelectedNode());
		// }
	}

	/**
	 * Connects a ModelView to the viewFrame and sets it up with the DataTicker
	 * so it can post information.
	 *
	 * @param modelView
	 *            The ModelView to be added
	 */
	private void registerView(final ModelView modelView) {

		modelView.setViewFrame(tvFrame);
		modelView.setStatusPanel(dataTicker);
	}

	public JButton getCloseSearchBtn() {

		return searchCloseBtn;
	}

	/**
	 * Changes the visibility of dendrograms and resets the layout.
	 *
	 * @param visible
	 */
	public void setTreeVisibility(final double atr_loc, final double gtr_loc) {

		if (colDataPane != null) {
			colDataPane.setDividerLocation(atr_loc);
		}
		if (rowDataPane != null) {
			rowDataPane.setDividerLocation(gtr_loc);
		}

		if (Helper.nearlyEqual(atr_loc, 0.0)
				|| Helper.nearlyEqual(gtr_loc, 0.0)) {
			showTreesMenuItem.setText(StringRes.menu_showTrees);

		} else {
			showTreesMenuItem.setText(StringRes.menu_hideTrees);
		}

		LogBuffer.println("Repaint from treeVisibilty");
		dendroPane.repaint();
	}

	/**
	 * Returns information about the current alignment of TextView and
	 * ArrayNameView
	 *
	 * @return [isRowRight, isColRight]
	 */
	public boolean[] getLabelAligns() {

		final boolean[] alignments = { getRowLabelView().getJustifyOption(),
				getColumnLabelView().getJustifyOption() };

		return alignments;
	}

	/**
	 * Sets the label alignment for TextView and ArrayNameView.
	 *
	 * @param isRowRight
	 *            TextView label justification.
	 * @param isColRight
	 *            ArrayNameView label justification.
	 */
	public void setLabelAlignment(final boolean isRowRight,
			final boolean isColRight) {

		if (getRowLabelView() == null || getColumnLabelView() == null)
			return;

		getRowLabelView().setJustifyOption(isRowRight);
		getColumnLabelView().setJustifyOption(isColRight);
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
		
		/* TODO add back when feature works well */
//		matrixMenu = new JMenu("Matrix Size");
//		menu.add(matrixMenu);
//
//		final JMenuItem fillScreenMenuItem = new JMenuItem("Fill screen");
//		matrixMenu.add(fillScreenMenuItem);
//		fillScreenMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1,
//				Event.ALT_MASK));
//		tvFrame.addToStackMenuList(fillScreenMenuItem);
//
//		final JMenuItem equalAxesMenuItem = new JMenuItem("Equal axes");
//		matrixMenu.add(equalAxesMenuItem);
//		equalAxesMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2,
//				Event.ALT_MASK));
//		tvFrame.addToStackMenuList(equalAxesMenuItem);
//
//		final JMenuItem proportMatrixMenuItem = new JMenuItem(
//				"Proportional axes");
//		matrixMenu.add(proportMatrixMenuItem);
//		proportMatrixMenuItem.setAccelerator(KeyStroke.getKeyStroke(
//				KeyEvent.VK_3, Event.ALT_MASK));
//		tvFrame.addToStackMenuList(proportMatrixMenuItem);

		menu.addSeparator();

		showTreesMenuItem = new JMenuItem("Show trees...");
		menu.add(showTreesMenuItem);
		showTreesMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		tvFrame.addToStackMenuList(showTreesMenuItem);

		//
		// isolateMenu = new JMenuItem("Isolate Selected");
		// menu.add(isolateMenu);
		// tvFrame.addToStackMenuList(isolateMenu);
	}

	@Override
	public void addClusterMenus(final JMenu menu) {

		// Cluster Menu
		final JMenuItem hierMenuItem = new JMenuItem(StringRes.menu_Hier);
		hierMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menu.add(hierMenuItem);
		tvFrame.addToStackMenuList(hierMenuItem);

		// final JMenuItem kMeansMenuItem = new JMenuItem(
		// StringRes.menu_KMeans);
		// menu.add(kMeansMenuItem);
		// tvFrame.addToStackMenuList(kMeansMenuItem);

	}

	@Override
	public void addSearchMenus(final JMenu menu) {

		final JMenuItem searchMenuItem = new JMenuItem("Find Labels...");
		searchMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
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
	public void setGVWidth(final double newWidth) {

		this.gvWidth = newWidth;
	}

	public void setGVHeight(final double newHeight) {

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

	public void setSearchTermBoxes() {

		this.rowFinderBox = new RowFinderBox();
		this.colFinderBox = new ColumnFinderBox();
	}

	public void updateSearchTermBoxes(final HeaderInfo rowHI,
			final HeaderInfo columnHI, final MapContainer xmap,
			final MapContainer ymap) {

		rowFinderBox.setHeaderInfo(rowHI, columnHI);
		rowFinderBox.setHeaderSummary(getRowLabelView().getHeaderSummary());
		rowFinderBox.setMapContainers(ymap, xmap);
		rowFinderBox.setSelection(tvFrame.getRowSelection(),
				tvFrame.getColumnSelection());
		rowFinderBox.setNewSearchTermBox();

		colFinderBox.setHeaderInfo(columnHI, rowHI);
		colFinderBox.setHeaderSummary(getColumnLabelView().getHeaderSummary());
		colFinderBox.setMapContainers(xmap, ymap);
		colFinderBox.setSelection(tvFrame.getColumnSelection(),
				tvFrame.getRowSelection());
		colFinderBox.setNewSearchTermBox();
	}

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

		return matrixXscrollbar;
	}

	public void setXScroll(final int i) {

		matrixXscrollbar.setValue(i);
	}

	public JScrollBar getYScroll() {

		return matrixYscrollbar;
	}

	public void setYScroll(final int i) {

		matrixYscrollbar.setValue(i);
	}

	public GlobalView getGlobalView() {

		return globalview;
	}

	public LabelView getColumnLabelView() {

		return colLabelView;
	}

	public ColumnTreeView getColumnTreeView() {

		return colTreeView;
	}

	public RowTreeView getRowTreeView() {

		return rowTreeView;
	}

	public LabelView getRowLabelView() {

		return rowLabelView;
	}
	
	public JSplitPane getRowSplitPane() {
		
		return rowDataPane;
	}
	
	public JSplitPane getColSplitPane() {
		
		return colDataPane;
	}
	
	public InputMap getInputMap() {
		
		return dendroPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
	}
	
	public ActionMap getActionMap() {
		
		return dendroPane.getActionMap();
	}

	/**
	 * Returns the current location of the divider in the supplied TRView's
	 * JSplitPane
	 *
	 * @param dendrogram
	 * @return Location of divider (max = 1.0)
	 */
	public double getDivLoc(final TRView dendrogram) {

		/* Get value for correct dendrogram JSplitPane */
		final JSplitPane treePane = (dendrogram == colTreeView) ? colDataPane
				: rowDataPane;

		/* returns imprecise position? -- no bug reports found */
		final double abs_div_loc = treePane.getDividerLocation();
		final double max_div_loc = treePane.getMaximumDividerLocation();

		/* Round the value */
		final int tmp = (int) ((abs_div_loc / max_div_loc) * 100);
		final double rel_div_loc = tmp / 100.0;

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

		final boolean treesEnabled = rowTreeView.isEnabled()
				|| colTreeView.isEnabled();
		return treesEnabled;
	}
}
