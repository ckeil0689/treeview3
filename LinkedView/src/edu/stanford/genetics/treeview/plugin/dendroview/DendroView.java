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

import java.awt.Insets;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;

import Utilities.GUIFactory;
import Utilities.Helper;
import Utilities.StringRes;
import edu.stanford.genetics.treeview.DendroPanel;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.core.ColumnFinderBox;
import edu.stanford.genetics.treeview.core.HeaderFinderBox;
import edu.stanford.genetics.treeview.core.RowFinderBox;
//import com.sun.glass.events.MouseEvent;

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
	private final JPanel searchPanel;

	protected ScrollPane panes[];

	// Matrix views
	private final GlobalMatrixView globalMatrixView;
	private final InteractiveMatrixView interactiveMatrixView;

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

	final JScrollBar colLabelScroll;
	final JScrollBar rowLabelScroll;

	/* Some important class-wide JMenuItems */
	private JMenuItem colorMenuItem;
	private JMenuItem annotationsMenuItem;
	private JMenuItem showTreesMenuItem;

	/* JButtons for scaling the matrix */
	/* TODO should be controlled in a GlobalViewController...when it exists */
	private JButton zoomBtn;

	private JButton scaleAddRightX;
	private JButton scaleAddBottomY;
	private JButton scaleAddLeftX;
	private JButton scaleAddTopY;

	private JButton scaleRemoveRightX;
	private JButton scaleRemoveBottomY;
	private JButton scaleRemoveLeftX;
	private JButton scaleRemoveTopY;

	private JButton scaleIncXY;
	private JButton scaleDecXY;
	private JButton scaleDefaultAll;

	private HeaderFinderBox rowFinderBox;
	private HeaderFinderBox colFinderBox;

	/* GlobalView default sizes */
	/* TODO needed? ... */
	private double gvWidth;
	private double gvHeight;

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
	 * @param tvFrame
	 *            parent ViewFrame of DendroView
	 * @param name
	 *            name of this view.
	 */
	public DendroView(final TreeViewFrame tvFrame, final String name) {

		this.tvFrame = tvFrame;
		this.name = "DendroView";

		/* main panel */

		searchPanel = GUIFactory.createJPanel(false, GUIFactory.NO_GAPS_NO_INS);
		dendroPane = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);

		/* Create the Global view (JPanel to display) */
		globalMatrixView = new GlobalMatrixView();
		interactiveMatrixView = new InteractiveMatrixView();

		//Register the global matrix view with the interactive matrix view so
		//that it can notify it when a selection changes
		interactiveMatrixView.setGlobalMatrixView(globalMatrixView);

		/* scrollbars, mostly used by maps */
		matrixXscrollbar = interactiveMatrixView.getXMapScroll();
		matrixYscrollbar = interactiveMatrixView.getYMapScroll();

		/* Set up the gene label display */
		rowLabelView = new RowLabelView();

		/* Set up the array label display */
		colLabelView = new ColumnLabelView();
		// arraynameview.setUrlExtractor(viewFrame.getArrayUrlExtractor());

		colLabelScroll = colLabelView.getSecondaryScrollBar();
		rowLabelScroll = rowLabelView.getSecondaryScrollBar();

		// Set up row dendrogram
		rowTreeView = new RowTreeView();
		rowTreeView.getHeaderSummary().setIncluded(new int[] { 0, 3 });

		// Set up column dendrogram
		colTreeView = new ColumnTreeView();
		colTreeView.getHeaderSummary().setIncluded(new int[] { 0, 3 });

		setupScaleButtons();
	}

	public void setMatrixHome() {

		interactiveMatrixView.resetView();
		interactiveMatrixView.repaint();

		globalMatrixView.resetView();
		globalMatrixView.repaint();
	}
	
	public void updateMatrixPixels() {
		
		interactiveMatrixView.setPixelsChanged();
		globalMatrixView.setPixelsChanged();
	}

	/**
	 * Returns the dendroPane so it can be displayed in TVFrame.
	 *
	 * @return JPanel dendroPane
	 */
	public JPanel makeDendro() {

		colLabelView.generateView(tvFrame.getUrlExtractor());
		rowLabelView.generateView(tvFrame.getUrlExtractor());

		// Register Views
		registerView(globalMatrixView);
		registerView(interactiveMatrixView);
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
	private void setSearchPanel() {

		if (rowFinderBox == null || colFinderBox == null) {
			return;
		}

		searchPanel.removeAll();

		final String tooltip = "You can use wildcards to search (*, ?). "
				+ "E.g.: *complex* --> Rpd3s complex, ATP Synthase "
				+ "(complex V), etc...";
		searchPanel.setToolTipText(tooltip);

		searchPanel.add(rowFinderBox.getSearchTermBox(), "w 80::, growx");
		searchPanel.add(colFinderBox.getSearchTermBox(), "w 80::, growx");

		searchPanel.revalidate();
		searchPanel.repaint();
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
		setSearchPanel();
	}

	/**
	 * Manages the component layout in TreeViewFrame
	 */
	public void setupLayout() {

		/* Clear dendroPane first */
		dendroPane.removeAll();

		/* Panels for layout setup */
		JPanel toolbarPanel;
		JPanel matrixPanel;
				
		toolbarPanel = createToolbarPanel();
		
		setupRowDataPane();
		setupColDataPane();
		setDataPaneDividers();

		matrixPanel = createMatrixPanel();
		
		dendroPane.add(matrixPanel, "grow, push, wrap");
		dendroPane.add(toolbarPanel, "growx, pushx, h 45!, wrap");
		
		dendroPane.revalidate();
		dendroPane.repaint();
	}
	
	private static JPanel createColorValIndicatorPanel() {
		
		JPanel indicatorPanel;
		String hint = ">>>> Placeholder for Color-Value Indicator <<<<";
		JLabel indicatorPlaceHolder = GUIFactory.createLabel(hint, 
				GUIFactory.FONTM);
		
		indicatorPanel = GUIFactory.createJPanel(false, 
				GUIFactory.DEFAULT);
		indicatorPanel.add(indicatorPlaceHolder);
		
		return indicatorPanel;
	}
	
	private JPanel createNavBtnPanel() {
		
		JPanel navBtnPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);
		navBtnPanel.add(scaleIncXY);
		navBtnPanel.add(scaleDecXY);
		navBtnPanel.add(zoomBtn);
		navBtnPanel.add(scaleDefaultAll);
		
		return navBtnPanel;
	}
	
	private JPanel createSearchBarPanel() {
		
		JPanel searchBarPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_PADDING_FILL);
		searchBarPanel.add(searchPanel);
		
		return searchBarPanel;
	}
	
	private JPanel createToolbarPanel() {
		
		JPanel navBtnPanel;
		JPanel searchBarPanel;
		JPanel colorValIndicatorPanel;
		JPanel toolbarPanel;
		
		// Comp 1
		colorValIndicatorPanel = createColorValIndicatorPanel();
		
		// Comp 2
		navBtnPanel = createNavBtnPanel();
		
		// Comp 3
		searchBarPanel = createSearchBarPanel();
		
		// Toolbar
		toolbarPanel = GUIFactory.createJPanel(false, GUIFactory.NO_GAPS_NO_INS);
		toolbarPanel.add(colorValIndicatorPanel, "w 33%, pushx");
		toolbarPanel.add(navBtnPanel, "al center, pushx");
		toolbarPanel.add(searchBarPanel, "al right, pushx, w 160px:33%:");
		
		return toolbarPanel;
	}
	
	private JPanel createRowTreePanel() {
		
		JPanel rowTreeEmptyPanel;
		JPanel rowTreePanel;
		
		rowTreePanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_GAPS);
		rowTreeEmptyPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_GAPS);
		rowTreePanel.add(rowTreeView, "w 100%, h 99%, wrap");
		rowTreePanel.add(rowTreeEmptyPanel, "w 100%, h 1%");
		
		return rowTreePanel;
	}
	
	private JPanel createRowLabelPanel() {
		
		JPanel rowLabelPanel;
		
		rowLabelPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_GAPS_NO_INS_FILL);
		rowLabelPanel.add(rowLabelView.getComponent(), "w 100%, h 99%, wrap");
		rowLabelPanel.add(rowLabelScroll, "h 1%, w 100%, aligny center");
		
		return rowLabelPanel;
	}
	
	private void setupRowDataPane() {
		
		JPanel rowLabelPanel;
		JPanel rowTreePanel;
		
		rowTreePanel = createRowTreePanel();
		rowLabelPanel = createRowLabelPanel();
		
		rowDataPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rowTreePanel,
				rowLabelPanel);
		rowDataPane.setResizeWeight(0.0);
		rowDataPane.setOpaque(false);

		rowDataPane.setUI(new DragBarUI(StringRes.icon_dragbar_vert,
				StringRes.icon_dragbar_vert_light));
		rowDataPane.setBorder(null);
		rowDataPane.setDividerSize(10);

		final double oldRowDiv = tvFrame.getConfigNode().getDouble("gtr_loc",
				0.5d);
		if (rowTreeView.isEnabled()) {
			rowDataPane.setDividerLocation(oldRowDiv);
		} else {
			rowDataPane.setDividerLocation(0.0);
		}
	}
	
	private JPanel createColTreePanel() {
		
		JPanel colTreePanel;
		JPanel colTreeEmptyPanel;
		
		colTreePanel = GUIFactory.createJPanel(false, GUIFactory.NO_GAPS);
		colTreeEmptyPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_GAPS);
		colTreePanel.add(colTreeView, "w 99%, h 100%");
		colTreePanel.add(colTreeEmptyPanel, "w 1%, growy, pushy, alignx center");
		
		return colTreePanel;
	}
	
	private JPanel createColLabelPanel() {
		
		JPanel colLabelPanel;
		
		colLabelPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_GAPS_NO_INS_FILL);
		colLabelPanel.add(colLabelView.getComponent(), "w 99%, h 100%");
		colLabelPanel.add(colLabelScroll, "growy, pushy, w 1%, alignx center");
		
		return colLabelPanel;
	}
	
	private void setupColDataPane() {
		
		JPanel colTreePanel;
		JPanel colLabelPanel;
		
		colTreePanel = createColTreePanel();
		colLabelPanel = createColLabelPanel();
		
		colDataPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, colTreePanel,
				colLabelPanel);
		colDataPane.setResizeWeight(0.0);
		colDataPane.setOpaque(false);

		colDataPane.setUI(new DragBarUI(StringRes.icon_dragbar_horiz,
				StringRes.icon_dragbar_horiz_light));
		colDataPane.setBorder(null);
		colDataPane.setDividerSize(10);

		final double oldColDiv = tvFrame.getConfigNode().getDouble("atr_loc",
				0.5d);
		if (colTreeView.isEnabled()) {
			colDataPane.setDividerLocation(oldColDiv);
		} else {
			colDataPane.setDividerLocation(0.0);
		}
	}
	
	private JPanel createGlobalOverviewPanel() {
		
		JPanel globalOverviewPanel;
		
		globalOverviewPanel = GUIFactory.createJPanel(false,
				GUIFactory.NO_GAPS_NO_INS_FILL);
		globalOverviewPanel.add(globalMatrixView, "h 180!, w 180!");
		
		return globalOverviewPanel;
	}
	
	private JPanel createColNavPanel() {
		
		JPanel colNavPanel;
		
		colNavPanel = GUIFactory.createJPanel(true, GUIFactory.NO_GAPS_NO_INS);
		colNavPanel.add(scaleAddLeftX);
		colNavPanel.add(scaleRemoveLeftX);

		colNavPanel.add(matrixXscrollbar, "w 100%, growx, pushx");
		
		colNavPanel.add(scaleRemoveRightX);
		colNavPanel.add(scaleAddRightX);
		
		return colNavPanel;
	}
	
	private JPanel createRowNavPanel() {
		
		JPanel rowNavPanel;
		
		rowNavPanel = GUIFactory.createJPanel(false, GUIFactory.NO_GAPS_NO_INS);
		
		rowNavPanel.add(scaleAddTopY,"wrap");
		rowNavPanel.add(scaleRemoveTopY,"wrap");
		
		rowNavPanel.add(matrixYscrollbar, "growy, pushy, wrap");
		
		rowNavPanel.add(scaleRemoveBottomY, "wrap");
		rowNavPanel.add(scaleAddBottomY);
		
		return rowNavPanel;
	}
	
	private JPanel createInteractiveMatrixPanel() {
		
		JPanel interactiveMatrixPanel;
		JPanel colNavPanel;
		JPanel rowNavPanel;
		
		colNavPanel = createColNavPanel();
		rowNavPanel = createRowNavPanel();
		
		interactiveMatrixPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_GAPS_NO_INS);
		interactiveMatrixPanel.add(interactiveMatrixView, "push, grow");
		interactiveMatrixPanel.add(rowNavPanel, "w 20!, h 100%, pushy, wrap");
		interactiveMatrixPanel.add(colNavPanel, "h 20!, w 100%, pushx");
		
		return interactiveMatrixPanel;
	}
	
	private JPanel createMatrixPanel() {
		
		JPanel matrixPanel;
		
		JPanel globalOverviewPanel;
		JPanel interactiveMatrixPanel;
		
		globalOverviewPanel = createGlobalOverviewPanel();
		interactiveMatrixPanel = createInteractiveMatrixPanel();
		
		matrixPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_GAPS_NO_INS_FILL);
		matrixPanel.add(globalOverviewPanel);
		matrixPanel.add(colDataPane, "h 180!, w 100%, pushx, growx, wrap");
		matrixPanel.add(rowDataPane, "w 180!, h 100%, pushy, growy");
		matrixPanel.add(interactiveMatrixPanel, "push, grow");
		
		return matrixPanel;
	}
	
	private void setDataPaneDividers() {
		
		final double oldRowDiv = tvFrame.getConfigNode().getDouble("gtr_loc",
				0.5d);
		final double oldColDiv = tvFrame.getConfigNode().getDouble("atr_loc",
				0.5d);
		
		/* If trees in general are disabled */
		if (!treesEnabled() && showTreesMenuItem != null) {
			showTreesMenuItem.setEnabled(false);
			
		} else {
			/* If trees are visible from the start */
			if (oldRowDiv > 0.0 || oldColDiv > 0.0) {
				showTreesMenuItem.setText(StringRes.menu_hideTrees);
			}
		}
	}

	/**
	 * Sets up the buttons which control scaling and zooming
	 */
	private void setupScaleButtons() {

		scaleDefaultAll = GUIFactory.createIconBtn(StringRes.icon_home);
		scaleDefaultAll.setToolTipText("Reset the zoomed view");

		int btnSize = 20;
		
		/* Scale x-axis */
		scaleAddRightX = GUIFactory.createSquareBtn("+", btnSize);
		scaleAddRightX.setToolTipText(StringRes.tt_xZoomIn_right);
		scaleAddRightX.setMargin(new Insets(0,0,0,0));

		scaleRemoveRightX = GUIFactory.createSquareBtn("-", btnSize);
		scaleRemoveRightX.setToolTipText(StringRes.tt_xZoomOut_right);
		scaleRemoveRightX.setMargin(new Insets(0,0,0,0));

		scaleAddLeftX = GUIFactory.createSquareBtn("+", btnSize);
		scaleAddLeftX.setToolTipText(StringRes.tt_xZoomIn_left);
		scaleAddLeftX.setMargin(new Insets(0,0,0,0));

		scaleRemoveLeftX = GUIFactory.createSquareBtn("-", btnSize);
		scaleRemoveLeftX.setToolTipText(StringRes.tt_xZoomOut_left);
		scaleRemoveLeftX.setMargin(new Insets(0,0,0,0));

		/* Scale y-axis */
		scaleAddBottomY = GUIFactory.createSquareBtn("+", btnSize);
		scaleAddBottomY.setToolTipText(StringRes.tt_yZoomIn_bottom);
		scaleAddBottomY.setMargin(new Insets(0,0,0,0));
		
		scaleRemoveBottomY = GUIFactory.createSquareBtn("-", btnSize);
		scaleRemoveBottomY.setToolTipText(StringRes.tt_yZoomOut_bottom);
		scaleRemoveBottomY.setMargin(new Insets(0,0,0,0));
		
		scaleAddTopY = GUIFactory.createSquareBtn("+", btnSize);
		scaleAddTopY.setToolTipText(StringRes.tt_yZoomIn_top);
		scaleAddTopY.setMargin(new Insets(0,0,0,0));
		
		scaleRemoveTopY = GUIFactory.createSquareBtn("-", btnSize);
		scaleRemoveTopY.setToolTipText(StringRes.tt_yZoomOut_top);
		scaleRemoveTopY.setMargin(new Insets(0,0,0,0));

		/* Scale both axes */
		scaleIncXY = GUIFactory.createIconBtn(StringRes.icon_fullZoomIn);
		scaleIncXY.setToolTipText(StringRes.tt_xyZoomIn);

		scaleDecXY = GUIFactory.createIconBtn(StringRes.icon_fullZoomOut);
		scaleDecXY.setToolTipText(StringRes.tt_xyZoomOut);

		/* Reset zoom */
		zoomBtn = GUIFactory.createIconBtn(StringRes.icon_zoomAll);
		zoomBtn.setToolTipText(StringRes.tt_home);
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
		if (rowDataPane.getDividerLocation() == 0
				|| colDataPane.getDividerLocation() == 0) {
			showTreesMenuItem.setText(StringRes.menu_showTrees);
		} else {
			showTreesMenuItem.setText(StringRes.menu_hideTrees);
		}
	}

	/* >>>>>>>>>> UI component listeners <<<<<<<<<< */
	public void addResizeListener(final ComponentListener l) {

		dendroPane.addComponentListener(l);
	}

	/**
	 * Adds an ActionListener to the scale buttons in DendroView.
	 *
	 * @param l
	 */
	public void addScaleListeners(final ActionListener l) {

		scaleAddRightX.addActionListener(l);
		scaleRemoveRightX.addActionListener(l);
		scaleAddBottomY.addActionListener(l);
		scaleRemoveBottomY.addActionListener(l);

		scaleAddLeftX.addActionListener(l);
		scaleRemoveLeftX.addActionListener(l);
		scaleAddTopY.addActionListener(l);
		scaleRemoveTopY.addActionListener(l);

		scaleIncXY.addActionListener(l);
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
	public void addContListener(final ContainerListener l) {

		dendroPane.addContainerListener(l);
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

	/**
	 * A small listener for the main dendroPane and the searchPanel which
	 * causes deselection of all elements upon clicking within these panels.
	 * 
	 * @param l
	 */
	public void addDeselectClickListener(MouseListener l) {

		dendroPane.addMouseListener(l);
		searchPanel.addMouseListener(l);
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
		// matrixMenu = new JMenu("Matrix Size");
		// menu.add(matrixMenu);
		//
		// final JMenuItem fillScreenMenuItem = new JMenuItem("Fill screen");
		// matrixMenu.add(fillScreenMenuItem);
		// fillScreenMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1,
		// Event.ALT_MASK));
		// tvFrame.addToStackMenuList(fillScreenMenuItem);
		//
		// final JMenuItem equalAxesMenuItem = new JMenuItem("Equal axes");
		// matrixMenu.add(equalAxesMenuItem);
		// equalAxesMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2,
		// Event.ALT_MASK));
		// tvFrame.addToStackMenuList(equalAxesMenuItem);
		//
		// final JMenuItem proportMatrixMenuItem = new JMenuItem(
		// "Proportional axes");
		// matrixMenu.add(proportMatrixMenuItem);
		// proportMatrixMenuItem.setAccelerator(KeyStroke.getKeyStroke(
		// KeyEvent.VK_3, Event.ALT_MASK));
		// tvFrame.addToStackMenuList(proportMatrixMenuItem);

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

	// @Override
	// public void addSearchMenus(final JMenu menu) {
	//
	// final JMenuItem searchMenuItem = new JMenuItem("Find Labels...");
	// searchMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
	// Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	// menu.add(searchMenuItem);
	// tvFrame.addToStackMenuList(searchMenuItem);
	// }

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

	public void setRowFinderBoxFocused() {

		rowFinderBox.getSearchTermBox().requestFocusInWindow();
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

		setSearchPanel();
	}

	@Override
	public String getName() {

		return name;
	}

	public void setName(final String name) {

		this.name = name;
	}

	// Getters
	public JButton getXLeftPlusButton() {

		return scaleAddLeftX;
	}

	public JButton getXRightPlusButton() {

		return scaleAddRightX;
	}

	public JButton getXYPlusButton() {

		return scaleIncXY;
	}

	public JButton getXMinusLeftButton() {

		return scaleRemoveLeftX;
	}

	public JButton getXMinusRightButton() {

		return scaleRemoveRightX;
	}

	public JButton getYPlusTopButton() {

		return scaleAddTopY;
	}

	public JButton getYPlusBottomButton() {

		return scaleAddBottomY;
	}

	public JButton getYMinusTopButton() {

		return scaleRemoveTopY;
	}

	public JButton getYMinusBottomButton() {

		return scaleRemoveBottomY;
	}

	public JButton getXYMinusButton() {

		return scaleDecXY;
	}

	public JButton getHomeButton() {

		return scaleDefaultAll;
	}

	public JButton getZoomButton() {

		return zoomBtn;
	}

	public JScrollBar getMatrixXScroll() {

		return matrixXscrollbar;
	}

	public JScrollBar getMatrixYScroll() {

		return matrixYscrollbar;
	}

	public JScrollBar getColLabelLengthScroll() {

		return colLabelScroll;
	}

	public JScrollBar getRowLabelLengthScroll() {

		return rowLabelScroll;
	}

	public InteractiveMatrixView getInteractiveMatrixView() {

		return interactiveMatrixView;
	}

	public GlobalMatrixView getGlobalMatrixView() {

		return globalMatrixView;
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
