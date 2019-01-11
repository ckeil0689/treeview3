/*
 * BEGIN_HEADER TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;

import Controllers.RegionType;
import Utilities.GUIFactory;
import Utilities.Helper;
import Utilities.StringRes;
import edu.stanford.genetics.treeview.DataTicker;
import edu.stanford.genetics.treeview.DendroPanel;
import edu.stanford.genetics.treeview.DragGridPanel;
import edu.stanford.genetics.treeview.ExportPreviewMatrix;
import edu.stanford.genetics.treeview.ExportPreviewTrees;
import edu.stanford.genetics.treeview.LabelInfo;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.core.ColumnFinderBox;
import edu.stanford.genetics.treeview.core.LabelFinderBox;
import edu.stanford.genetics.treeview.core.RowFinderBox;
import edu.stanford.genetics.treeview.DragGridPanel;
import edu.stanford.genetics.treeview.ExportPreviewLabels;

/** TODO Refactor this JavaDoc. It's not applicable to the current program
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
 * @version $Revision: 1.7 $ $Date: 2009-03-23 02:46:51 $ */
public class DendroView implements Observer, DendroPanel {

	// Container JFrame
	protected TreeViewFrame tvFrame;

	private String name;

	// Main containers
	private final JPanel dendroPane;
	private final DragGridPanel dragGrid;
	private final JPanel searchPanel;
	private final DataTicker ticker;

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

	/* JScrollBars for InteractiveMatrixView */
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

	private JButton scaleIncXY;
	private JButton scaleDecXY;
	private JButton scaleDefaultAll;

	/* TODO: This needs to be better integrated into the interface */
//	private JButton exportBtn;

	private LabelFinderBox rowFinderBox;
	private LabelFinderBox colFinderBox;


	//Used to layout the matrix panel
	static int BORDER_THICKNESS = 3;
	static int MIN_GRID_CELL_SIZE = 10;
	static int FOCUS_THICKNESS = 0;
	static int LABEL_AREA_HEIGHT = 180;

	/** Chained constructor for the DendroView object without a name.
	 *
	 * @param vFrame
	 *          parent ViewFrame of DendroView */
	public DendroView(final TreeViewFrame tvFrame) {

		this(tvFrame, "Dendrogram");
	}

	/* TODO why does this even exist... */
	public DendroView(final Preferences root, final TreeViewFrame tvFrame) {

		this(tvFrame, "Dendrogram");
	}

	/** Constructor for the DendroView object
	 *
	 * @param tvFrame parent ViewFrame of DendroView
	 * @param name name of this view. */
	public DendroView(final TreeViewFrame tvFrame, final String name) {

		this.tvFrame = tvFrame;
		this.name = "DendroView";

		// Main panel to which all components are added
		dendroPane = GUIFactory.createJPanel(false, GUIFactory.TINY_GAPS_AND_INSETS);

		dragGrid = new DragGridPanel(2, 2);

		// Search panel containing the search bars
		searchPanel = GUIFactory.createJPanel(false, GUIFactory.NO_GAPS_OR_INSETS);

		// The two matrix views (big, interactive & small, overview)
		globalMatrixView = new GlobalMatrixView();
		interactiveMatrixView = new InteractiveMatrixView();

		// Main matrix JScrollbars
		matrixXscrollbar = interactiveMatrixView.getXMapScroll();
		matrixYscrollbar = interactiveMatrixView.getYMapScroll();

		// Label views
		rowLabelView = new RowLabelView();
		colLabelView = new ColumnLabelView();
		// arraynameview.setUrlExtractor(viewFrame.getArrayUrlExtractor());

		rowLabelScroll = rowLabelView.getSecondaryScrollBar();
		colLabelScroll = colLabelView.getSecondaryScrollBar();

		// Dendrograms
		rowTreeView = new RowTreeView();
		colTreeView = new ColumnTreeView();

		ticker = new DataTicker();

		setupScaleButtons();
	}

	/** Returns the dendroPane so it can be displayed in TVFrame.
	 *
	 * @return JPanel dendroPane */
	public JPanel makeDendro() {

		colLabelView.setUrlExtractor(tvFrame.getUrlExtractor());
		rowLabelView.setUrlExtractor(tvFrame.getUrlExtractor());

		registerViews();

		return dendroPane;
	}

	/** Registers all ModelViews with the ViewFrame. */
	private void registerViews() {

		registerView(globalMatrixView);
		registerView(interactiveMatrixView);
		registerView(colTreeView);
		registerView(colLabelView);
		registerView(rowLabelView);
		registerView(rowTreeView);
	}

	/** Setup the search panel which contains tow editable JComboBoxes containing
	 * all labels for each axis.
	 *
	 * @return JPanel */
	private void setupSearchPanel() {

		if((rowFinderBox == null) || (colFinderBox == null)) {
			LogBuffer.println("Could not set up search. A search bar has" +
				" not been set up.");
			return;
		}

		searchPanel.removeAll();

		final String tooltip = "You can use wildcards to search (*, ?). " +
			"E.g.: *complex* --> Rpd3s complex, ATP Synthase " +
			"(complex V), etc...";
		searchPanel.setToolTipText(tooltip);

		searchPanel.add(rowFinderBox.getSearchTermBox(), "w 80::, growx, " +
			"pushx, al right");
		searchPanel.add(colFinderBox.getSearchTermBox(), "w 80::, growx, " +
			"pushx, al right");

		searchPanel.revalidate();
		searchPanel.repaint();
	}

	/** Initializes the objects associated with label search. These are the
	 * JComboBoxes containing all the label names as well as the buttons on that
	 * panel.
	 *
	 * @param geneHI
	 * @param arrayHI
	 * @param xmap
	 * @param ymap */
	public void setupSearch(final LabelInfo geneHI, final LabelInfo arrayHI,
													final MapContainer xmap, final MapContainer ymap) {

		setSearchTermBoxes();
		updateSearchTermBoxes(geneHI, arrayHI, xmap, ymap);
		setupSearchPanel();
	}

	/** Organizes the main layout for DendroView. */
	public void setupLayout() {

		dendroPane.removeAll();

		final JPanel toolbarPanel = createToolbarPanel();

		setupRowDataPane();
		setupColDataPane();
		setDataPaneDividers();

		setupMatrixPanel();

		dendroPane.add(dragGrid, "grow, push, wrap");
		dendroPane.add(toolbarPanel, "growx, pushx, h 3%, wrap");

		dendroPane.revalidate();
		dendroPane.repaint();
	}

	/** Reset default state for all model views. */
	public void resetModelViewDefaults() {

		rowLabelView.resetDefaults();
		colLabelView.resetDefaults();

		rowTreeView.resetDefaults();
		colTreeView.resetDefaults();

		interactiveMatrixView.resetDefaults();
		globalMatrixView.resetDefaults();
	}

	/** Restore the state of RowLabelView and ColumnLabelView to the stored
	 * values in their respective Preferences nodes. */
	public void restoreLabelViewStates() {

		rowLabelView.requestStoredState();
		colLabelView.requestStoredState();
	}

	/** Creates a panel which contains the color-value indicator. This is
	 * used to display the data value of the currently hovered matrix pixel.
	 *
	 * @return JPanel containing the olor-value indicator. */
	private JPanel createColorValIndicatorPanel() {

		// TODO currently just a data ticker displaying a tile's data value
		return ticker.getTickerPanel();
	}

	/** Creates a JPanel to hold the main navigation buttons for the matrix.
	 *
	 * @return A JPanel with all navigation buttons. */
	private JPanel createNavBtnPanel() {

		final JPanel navBtnPanel = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);
		navBtnPanel.add(scaleIncXY);
		navBtnPanel.add(scaleDecXY);
		navBtnPanel.add(zoomBtn);
		navBtnPanel.add(scaleDefaultAll);

		return navBtnPanel;
	}

	/** Creates a button which contains search interface elements, such as
	 * search bars.
	 *
	 * @return A JPanel containing search bars. */
	private JPanel createSearchBarPanel() {

		final JPanel searchBarPanel = GUIFactory
																						.createJPanel(false, GUIFactory.NO_INSETS_FILL);
		searchBarPanel.add(searchPanel);

		return searchBarPanel;
	}

	/** A parent JPanel to organize main navigation + info UI elements in a
	 * toolbar like layout.
	 *
	 * @return A JPanel containing all major UI navigation + info elements. */
	private JPanel createToolbarPanel() {

		JPanel navBtnPanel;
		JPanel searchBarPanel;
		JPanel colorValIndicatorPanel;
		JPanel toolbarPanel;

		colorValIndicatorPanel = createColorValIndicatorPanel();
		navBtnPanel = createNavBtnPanel();
		searchBarPanel = createSearchBarPanel();

		// Toolbar
		toolbarPanel = GUIFactory.createJPanel(false, GUIFactory.NO_GAPS_OR_INSETS);
		toolbarPanel.add(colorValIndicatorPanel, "al left, w 33%, wmax 33%");
		toolbarPanel.add(navBtnPanel, "al center, pushx");
		toolbarPanel.add(searchBarPanel, "al right, w 33%");

		return toolbarPanel;
	}

	/** Creates a panel to hold the row dendrogram. This will be added to
	 * the left side of the corresponding JSplitPane. The dendrogram is only
	 * drawn if a model was clustered and a gtr-file exists.
	 *
	 * @return A JPanel with the row dendrogram. */
	private JPanel createRowTreePanel() {

		final JPanel rowTreePanel = GUIFactory
																					.createJPanel(false, GUIFactory.NO_GAPS_OR_INSETS);

		rowTreePanel.add(rowTreeView, "w 100%, pushy, growy, wrap");

		// hidemode is a MigLayout trick! Very important to keep trees aligned.
		rowTreePanel.add(rowTreeView.getHorizontalScrollBar(), "w 100%, " +
																														"hidemode 0");
		rowTreeView.getHorizontalScrollBar().setVisible(false);

		return rowTreePanel;
	}

	/** Creates a JPanel to hold the display of row labels.
	 *
	 * @return A JPanel holding the row LabelView. */
	private JPanel createRowLabelPanel() {

		final JPanel rowLabelPanel = GUIFactory.createJPanel(false, GUIFactory.NO_GAPS_OR_INSETS);

		rowLabelPanel.add(rowLabelView.getComponent(), "w 100%, pushy, growy, " +
																										"wrap");
		// Row-Label scrollbar in sync with colNavPanel via MigLayout!
		// Guaranteeing same size and alignment can only be done via manual add.
		rowLabelPanel.add(rowLabelView.getSecondaryScrollBar(), "w 100%");

		return rowLabelPanel;
	}

	/** Sets up the JSplitPane used to show row dendrogram and labels. */
	private void setupRowDataPane() {

		JPanel rowLabelPanel;
		JPanel rowTreePanel;

		rowLabelPanel = createRowLabelPanel();
		//Allow the user to drag the divider to completely hide the labels
		rowLabelPanel.setMinimumSize(new Dimension(0, 0));

		rowTreePanel = createRowTreePanel();
		//Allow the user to drag the divider to completely hide the trees
		rowTreePanel.setMinimumSize(new Dimension(0, 0));

		rowDataPane = new JSplitPane(	JSplitPane.HORIZONTAL_SPLIT, rowTreePanel,
																	rowLabelPanel);
		rowDataPane.setResizeWeight(0.0);
		rowDataPane.setOpaque(false);

		rowDataPane.setUI(new DragBarUI(StringRes.icon_dragbar_vert,
																		StringRes.icon_dragbar_vert_light));
		rowDataPane.setBorder(null);
		rowDataPane.setDividerSize(10);

		final double oldRowDiv = tvFrame.getConfigNode().getDouble("gtr_loc", 0.5d);
		if(rowTreeView.isEnabled()) {
			rowDataPane.setDividerLocation(oldRowDiv);
			setRowTRViewEnabled(true);

		}
		else {
			rowDataPane.setDividerLocation(0.0);
			setRowTRViewEnabled(false);
		}
	}

	/** Creates a panel to hold the column dendrogram. This will be added to
	 * the top side of the corresponding JSplitPane. The dendrogram is only
	 * drawn if a model was clustered and a atr-file exists.
	 *
	 * @return A JPanel with the column dendrogram. */
	private JPanel createColTreePanel() {

		final JPanel colTreePanel = GUIFactory
																					.createJPanel(false, GUIFactory.NO_GAPS_OR_INSETS);

		colTreePanel.add(colTreeView, "push, growx, h 100%");

		// hidemode is a MigLayout trick! Very important to keep trees aligned.
		colTreePanel.add(colTreeView.getVerticalScrollBar(), "h 100%, " +
																													"hidemode 0");
		colTreeView.getVerticalScrollBar().setVisible(false);

		return colTreePanel;
	}

	/** Creates a JPanel to hold the display of column labels.
	 *
	 * @return A JPanel holding the column LabelView. */
	private JPanel createColLabelPanel() {

		JPanel colLabelPanel;

		colLabelPanel = GUIFactory
															.createJPanel(false, GUIFactory.NO_GAPS_OR_INSETS);
		colLabelPanel.add(colLabelView.getComponent(), "h 100%, pushx, growx");
		// Col-Label scrollbar in sync with rowNavPanel via MigLayout!
		// Guaranteeing same size and alignment can only be done via manual add.
		colLabelPanel.add(colLabelView.getSecondaryScrollBar(), "h 100%");

		return colLabelPanel;
	}

	/** Sets up the JSplitPane used to show column dendrogram and labels. */
	private void setupColDataPane() {

		JPanel colTreePanel;
		JPanel colLabelPanel;

		colTreePanel = createColTreePanel();
		//Allow the user to drag the divider to completely hide the trees
		colTreePanel.setMinimumSize(new Dimension(0, 0));

		colLabelPanel = createColLabelPanel();
		//Allow the user to drag the divider to completely hide the labels
		colLabelPanel.setMinimumSize(new Dimension(0, 0));

		colDataPane = new JSplitPane(	JSplitPane.VERTICAL_SPLIT, colTreePanel,
																	colLabelPanel);
		colDataPane.setResizeWeight(0.0);
		colDataPane.setOpaque(false);

		colDataPane.setUI(new DragBarUI(StringRes.icon_dragbar_horiz,
																		StringRes.icon_dragbar_horiz_light));
		colDataPane.setBorder(null);
		colDataPane.setDividerSize(10);

		final double oldColDiv = tvFrame.getConfigNode().getDouble("atr_loc", 0.5d);
		if(colTreeView.isEnabled()) {
			colDataPane.setDividerLocation(oldColDiv);
			setColTRViewEnabled(true);

		}
		else {
			colDataPane.setDividerLocation(0.0);
			setColTRViewEnabled(false);
		}
	}

	/** Creates container for the GlobalOverViewMatrix.
	 *
	 * @return JPanel holding the GlobalOverviewMatrix instance. */
	private JPanel createGlobalOverviewPanel() {

		JPanel globalOverviewPanel;

		globalOverviewPanel = GUIFactory
																		.createJPanel(false, GUIFactory.NO_GAPS_OR_TOPLEFT_INSETS);
		globalOverviewPanel.add(globalMatrixView, "grow, push");

		return globalOverviewPanel;
	}

	/** Panel that holds the main components for the interactive matrix.
	 * This includes InteractiveMatrixView itself as well as row and column
	 * navigation panels.
	 *
	 * @return A JPanel with the interactive matrix view setup. */
	private JPanel createInteractiveMatrixPanel() {

		JPanel interactiveMatrixPanel;

		interactiveMatrixPanel = GUIFactory.createJPanel(false, GUIFactory.NO_GAPS_OR_INSETS);
		interactiveMatrixPanel.add(interactiveMatrixView, "push, grow");
		interactiveMatrixPanel.add(matrixYscrollbar, "growy, pushy, wrap");
		interactiveMatrixPanel.add(matrixXscrollbar, "growx, pushx");

		return interactiveMatrixPanel;
	}

	/** Creates the full main matrix panel which includes all components
	 * making up a full DendroView with the exception of the toolbar related
	 * elements such as buttons or search.
	 *
	 * @return A DragGridPanel with all main views arranged in it. */
	private void setupMatrixPanel() {

		dragGrid.removeAll();

		dragGrid.setName("MatrixPanel");
		dragGrid.setBorderWidth(DendroView.BORDER_THICKNESS);
		dragGrid.setBorderHeight(DendroView.BORDER_THICKNESS);
		dragGrid.setMinimumWidth(DendroView.MIN_GRID_CELL_SIZE);
		dragGrid.setMinimumHeight(DendroView.MIN_GRID_CELL_SIZE);
		dragGrid.setFocusWidth(DendroView.FOCUS_THICKNESS);   //This is a line in the
		dragGrid.setFocusHeight(DendroView.FOCUS_THICKNESS);  //middle of the border

		final int mheights[] = new int[1];   //1 less than the size of the grid
		mheights[0] = DendroView.LABEL_AREA_HEIGHT; //must be less than pane size!!!
		dragGrid.setHeights(mheights);

		final int mwidths[] = new int[1];   //1 less than the size of the grid
		mwidths[0] = DendroView.LABEL_AREA_HEIGHT; //must be less than pane size!!!
		dragGrid.setWidths(mwidths);

		JPanel globalOverviewPanel;
		JPanel interactiveMatrixPanel;

		globalOverviewPanel = createGlobalOverviewPanel();
		interactiveMatrixPanel = createInteractiveMatrixPanel();

		dragGrid.addComponent(globalOverviewPanel, 0, 0);
		dragGrid.addComponent(colDataPane, 1, 0);
		dragGrid.addComponent(rowDataPane, 0, 1);
		dragGrid.addComponent(interactiveMatrixPanel, 1, 1);
	}

	public DragGridPanel getDragGrid() {
		return(dragGrid);
	}

	/** Looks up the stored location values for the JSplitPane dividers.
	 * This is needed for "Show-Hide" trees. It determines how much of
	 * labels vs. tree panel is shown. */
	private void setDataPaneDividers() {

		final double oldRowDiv = tvFrame.getConfigNode().getDouble("gtr_loc", 0.5d);
		final double oldColDiv = tvFrame.getConfigNode().getDouble("atr_loc", 0.5d);

		/* If trees in general are disabled */
		if(!treesEnabled() && (showTreesMenuItem != null)) {
			showTreesMenuItem.setEnabled(false);

		}
		else {
			/* If trees are visible from the start */
			if((oldRowDiv > 0.0) || (oldColDiv > 0.0)) {
				showTreesMenuItem.setText(StringRes.menu_hideTrees);
			}
		}
	}

	/** Sets up the JButtons which control scaling and zooming. */
	private void setupScaleButtons() {

		// reset scale
		scaleDefaultAll = GUIFactory.createIconBtn(StringRes.icon_home);
		scaleDefaultAll.setToolTipText("Reset the zoomed view");

		// scale both axes (large buttons)
		scaleIncXY = GUIFactory.createIconBtn(StringRes.icon_fullZoomIn);
		scaleIncXY.setToolTipText(StringRes.tt_xyZoomIn);

		scaleDecXY = GUIFactory.createIconBtn(StringRes.icon_fullZoomOut);
		scaleDecXY.setToolTipText(StringRes.tt_xyZoomOut);

		// zoom
		zoomBtn = GUIFactory.createIconBtn(StringRes.icon_zoomAll);
		zoomBtn.setToolTipText(StringRes.tt_home);
	}

	/** Used to update the JMenuItem field "Show trees/ Hide trees" depending
	 * on the current status of the divider. */
	public void updateTreeMenuBtn() {

		/* Should always be "Show trees" if any tree panel is invisible */
		if((rowDataPane.getDividerLocation() == 0) || (colDataPane
																															.getDividerLocation() == 0)) {
			showTreesMenuItem.setText(StringRes.menu_showTrees);

		}
		else {
			showTreesMenuItem.setText(StringRes.menu_hideTrees);
		}
	}

	/** Adds an ActionListener to the scale buttons in DendroView.
	 *
	 * @param l */
	public void addScaleListeners(final ActionListener l) {

		scaleIncXY.addActionListener(l);
		scaleDecXY.addActionListener(l);
		scaleDefaultAll.addActionListener(l);
	}

	/** Adds an ActionListener to the Zoom button in DendroView.
	 *
	 * @param l */
	public void addZoomListener(final ActionListener l) {

		zoomBtn.addActionListener(l);
	}

	/** Adds a component listener to the main panel of DendroView.
	 *
	 * @param l */
	public void addContListener(final ContainerListener l) {

		dendroPane.addContainerListener(l);
	}

	/** Add listener for divider movement and position for the Tree/ Label
	 * JSplitPanes.
	 *
	 * @param l */
	public void addDividerListener(final PropertyChangeListener l) {

		rowDataPane.addPropertyChangeListener(l);
		colDataPane.addPropertyChangeListener(l);
	}

	public void addSplitPaneListener(final ComponentAdapter c) {

		rowDataPane.addComponentListener(c);
		colDataPane.addComponentListener(c);
	}

	/** A small listener for the main dendroPane and the searchPanel which
	 * causes deselection of all elements upon clicking within these panels.
	 *
	 * @param l */
	public void addDeselectClickListener(final MouseListener l) {

		dendroPane.addMouseListener(l);
		searchPanel.addMouseListener(l);
	}

	// Methods
	/** @param o
	 * @param arg */
	@Override
	public void update(final Observable o, final Object arg) {

		// if (o == geneSelection) {
		// gtrview.scrollToNode(geneSelection.getSelectedNode());
		//
		// } else if (o == arraySelection) {
		// atrview.scrollToNode(arraySelection.getSelectedNode());
		// }
	}

	/** Connects a ModelView to the viewFrame and sets it up with the DataTicker
	 * so it can post information.
	 *
	 * @param modelView
	 *          The ModelView to be added */
	private void registerView(final ModelView modelView) {
		modelView.setDataTicker(ticker);
		modelView.setViewFrame(tvFrame);
	}

	/** Changes the visibility of dendrograms and resets the layout.
	 *
	 * @param visible */
	public void setTreeVisibility(final double atr_loc, final double gtr_loc) {

		if(colDataPane != null) {
			colDataPane.setDividerLocation(atr_loc);
		}
		if(rowDataPane != null) {
			rowDataPane.setDividerLocation(gtr_loc);
		}

		if(Helper.nearlyEqual(atr_loc, 0.0) || Helper.nearlyEqual(gtr_loc, 0.0)) {
			showTreesMenuItem.setText(StringRes.menu_showTrees);

		}
		else {
			showTreesMenuItem.setText(StringRes.menu_hideTrees);
		}

		dendroPane.repaint();
	}

	/** Returns information about the current alignment of TextView and
	 * ArrayNameView
	 *
	 * @return [isRowRight, isColRight] */
	public boolean[] getLabelAligns() {

		final boolean[] alignments = {
			getRowLabelView().getLabelAttributes().isRightJustified(),
			getColLabelView().getLabelAttributes().isRightJustified()};

		return alignments;
	}

	/** Sets the label alignment for TextView and ArrayNameView.
	 *
	 * @param isRowRight
	 *          TextView label justification.
	 * @param isColRight
	 *          ArrayNameView label justification. */
	public void setLabelAlignment(final boolean isRowRight,
		final boolean isColRight) {

		if((getRowLabelView() == null) || (getColLabelView() == null)) {
			return;
		}

		getRowLabelView().getLabelAttributes().setRightJustified(isRowRight);
		getColLabelView().getLabelAttributes().setRightJustified(isColRight);
	}

	// Populate Menus
	/** adds DendroView stuff to Analysis menu
	 *
	 * @param menu
	 *          menu to add to */
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

	@Override
	public void addViewMenus(final JMenu menu) {

		annotationsMenuItem = new JMenuItem(StringRes.menu_RowAndCol);
		menu.add(annotationsMenuItem);
		tvFrame.addToStackMenuList(annotationsMenuItem);

		colorMenuItem = new JMenuItem(StringRes.menu_Color);
		menu.add(colorMenuItem);
		tvFrame.addToStackMenuList(colorMenuItem);

		menu.addSeparator();

		showTreesMenuItem = new JMenuItem("Show trees...");
		menu.add(showTreesMenuItem);
		showTreesMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit()
																																									.getMenuShortcutKeyMask()));
		tvFrame.addToStackMenuList(showTreesMenuItem);

		// TODO readd once copy-from-selection is available
		// isolateMenu = new JMenuItem("Isolate Selected");
		// menu.add(isolateMenu);
		// tvFrame.addToStackMenuList(isolateMenu);
	}

	@Override
	public void addClusterMenus(final JMenu menu) {

		// Cluster Menu
		final JMenuItem hierMenuItem = new JMenuItem(StringRes.menu_Hier);
		hierMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit	.getDefaultToolkit()
																																							.getMenuShortcutKeyMask()));
		menu.add(hierMenuItem);
		tvFrame.addToStackMenuList(hierMenuItem);

		// TODO re-add K-means once available
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

	/** Setter for viewFrame */
	public void setViewFrame(final TreeViewFrame viewFrame) {

		tvFrame = viewFrame;
	}

	public void setSearchTermBoxes() {

		rowFinderBox = new RowFinderBox();
		colFinderBox = new ColumnFinderBox();

		//This is so a search in either box can initiate the search in the
		//companion box as well (so that both search terms are respected for
		//every search).  See Bitbucket Issue #233.
		rowFinderBox.setCompanionBox(colFinderBox);
		colFinderBox.setCompanionBox(rowFinderBox);
	}

	public void setRowFinderBoxFocused() {

		rowFinderBox.getSearchTermBox().requestFocusInWindow();
	}

	public boolean isAFinderBoxFocussed() {
		return(rowFinderBox	.getSearchTermBox().getEditor().getEditorComponent()
			.isFocusOwner() || colFinderBox	.getSearchTermBox().getEditor()
			.getEditorComponent().isFocusOwner());
	}

	public void updateSearchTermBoxes(final LabelInfo rowHI,
		final LabelInfo columnHI,final MapContainer xmap,final MapContainer ymap) {

		rowFinderBox.setLabelInfo(rowHI);
		rowFinderBox.setLabelSummary(getRowLabelView().getLabelSummary());
		rowFinderBox.setMapContainers(ymap, xmap);
		rowFinderBox.setSelection(tvFrame.getRowSelection(), tvFrame.getColSelection());
		rowFinderBox.setNewSearchTermBox();

		colFinderBox.setLabelInfo(columnHI);
		colFinderBox.setLabelSummary(getColLabelView().getLabelSummary());
		colFinderBox.setMapContainers(xmap, ymap);
		colFinderBox.setSelection(tvFrame.getColSelection(), tvFrame.getRowSelection());
		colFinderBox.setNewSearchTermBox();

		setupSearchPanel();
	}

	/** Disables the ColumnTreeView and its JSplitPane to make divider
	 * movement / resizing impossible.
	 *
	 * @param enabled Enable / disable the column TreeView. */
	public void setColTRViewEnabled(final boolean enabled) {

		if((colTreeView == null) || (colDataPane == null)) {
			LogBuffer.println("colTreeView could not be enabled/ disabled " +
				"because it was null");
			return;
		}

		colTreeView.setEnabled(enabled);
		colDataPane.setEnabled(enabled);
	}

	/** Disables the RowTreeView and its JSplitPane to make divider
	 * movement / resizing impossible.
	 *
	 * @param enabled Enable / disable the row TreeView. */
	public void setRowTRViewEnabled(final boolean enabled) {

		if((rowTreeView == null) || (rowDataPane == null)) {
			LogBuffer.println("rowTreeView could not be enabled/ disabled " +
				"because it was null");
			return;
		}

		rowTreeView.setEnabled(enabled);
		rowDataPane.setEnabled(enabled);
	}

	/** Gets a snapshot of the matrix from InteractiveMatrixView depending on
	 * the selected region type. Selections can be drawn as well.
	 *
	 * @param withSelections - whether selections should be drawn onto
	 *          the matrix
	 * @param region - The RegionType defines which region of the matrix
	 *          will be shown.
	 * @return A new ExportPreviewMatrix panel containing the matrix. */
	public ExportPreviewMatrix getMatrixSnapshot(final boolean withSelections,
		RegionType region) {

		Image image;

		switch(region) {
			case ALL:
				image = getInteractiveMatrixView().getFullImage(withSelections);
				break;
			case VISIBLE:
				image = getInteractiveMatrixView().getVisibleImage(withSelections);
				break;
			case SELECTION:
				image = getInteractiveMatrixView().getSelectionImage();
				break;
			default:
				image = getInteractiveMatrixView().getFullImage(withSelections);
		}

		return new ExportPreviewMatrix(image);
	}

	/**
	 * Get an image of the row tree for the export preview
	 * 
	 * @param withSelections - Whether to include selection highlighting
	 * @param region - region of the matrix to export
	 * @return
	 */
	public ExportPreviewTrees getRowTreeSnapshot(final boolean withSelections,
		RegionType region) {

		return getTreeSnapshot(rowTreeView, region, withSelections, true);
	}

	/**
	 * Get an image of the row tree for the export preview
	 * 
	 * @param withSelections - Whether to include selection highlighting
	 * @param region - region of the matrix to export
	 * @param width - preview image width
	 * @param height - preview image height
	 * @param longMatrixEdge - Length of the matrix edge adjacent to the tree
	 * @return
	 */
	public ExportPreviewTrees getRowTreeSnapshot(final boolean withSelections,
		final RegionType region,final int width,final int height,
		final int longMatrixEdge) {

		return getTreeSnapshot(rowTreeView,region,withSelections,true,width,
			height,longMatrixEdge);
	}

	/**
	 * Get an image of the col tree for the export preview
	 * 
	 * @param withSelections - Whether to include selection highlighting
	 * @param region - region of the matrix to export
	 * @return
	 */
	public ExportPreviewTrees getColTreeSnapshot(final boolean withSelections,
		RegionType region) {

		return getTreeSnapshot(colTreeView, region, withSelections, false);
	}

	/**
	 * Get an image of the row labels for the export preview
	 * 
	 * @param withSelections - Whether to include selection highlighting
	 * @param region - region of the matrix to export
	 * @param width - preview image width
	 * @param height - preview image height
	 * @param drawSelectionOnly - Only draw labels that are selected
	 * @param tileHeight - The height of the tiles in the final exported image
	 * @param fontSize - The height of the font in the final exported image
	 * @param longMatrixEdge - Length of the matrix edge adjacent to the tree
	 * @return
	 */
	public ExportPreviewLabels getRowLabelsSnapshot(
		final boolean withSelections,final RegionType region,final int width,
		final int height,final boolean drawSelectionOnly,final int tileHeight,
		final int fontSize,final int longMatrixEdge) {

		return getLabelsSnapshot(rowLabelView,region,withSelections,true,width,
			height,drawSelectionOnly,tileHeight,fontSize,longMatrixEdge);
	}

	/**
	 * Get an image of the column labels for the export preview
	 * 
	 * @param withSelections - Whether to include selection highlighting
	 * @param region - region of the matrix to export
	 * @param width - preview image width
	 * @param height - preview image height
	 * @param drawSelectionOnly - Only draw labels that are selected
	 * @param tileHeight - The height of the tiles in the final exported image
	 * @param fontSize - The height of the font in the final exported image
	 * @param longMatrixEdge - Length of the matrix edge adjacent to the tree
	 * @return
	 */
	public ExportPreviewLabels getColLabelsSnapshot(
		final boolean withSelections,RegionType region,final int width,
		final int height,final boolean drawSelectionOnly,final int tileWidth,
		final int fontSize,final int longMatrixEdge) {

		return getLabelsSnapshot(colLabelView,region,withSelections,false,width,
			height,drawSelectionOnly,tileWidth,fontSize,longMatrixEdge);
	}

	/**
	 * Get an image of the col tree for the export preview
	 * 
	 * @param withSelections - Whether to include selection highlighting
	 * @param region - region of the matrix to export
	 * @param width - preview image width
	 * @param height - preview image height
	 * @param longMatrixEdge - Length of the matrix edge adjacent to the tree
	 * @return
	 */
	public ExportPreviewTrees getColTreeSnapshot(final boolean withSelections,
		final RegionType region,final int width,final int height,
		final int longMatrixEdge) {

		return getTreeSnapshot(colTreeView,region,withSelections,false,width,
			height,longMatrixEdge);
	}

	/**
	 * Get an image of a tree for the export preview
	 * 
	 * @param treeAxisView - TRView object
	 * @param region - region of the matrix to export
	 * @param withSelections - Whether to include selection highlighting
	 * @param isRows - Whether this is for a row tree or not
	 * @return
	 */
	private ExportPreviewTrees getTreeSnapshot(TRView treeAxisView,
		RegionType region,final boolean withSelections,final boolean isRows) {

		if(treeAxisView == null) {
			LogBuffer.println("Cannot generate tree snapshot. TRView object " +
				"is null.");
			return new ExportPreviewTrees(null, isRows); // empty panel
		}

		/* using defaults here. The actual image will be rescaled later
		 * in the ExportDialog. */
		int width;
		int height;
		if(isRows) {
			width = ExportPreviewTrees.SECONDARY_SIDE_LEN_DEFAULT;
			height = ExportPreviewTrees.PRIMARY_SIDE_LEN_DEFAULT;

		}
		else {
			width = ExportPreviewTrees.PRIMARY_SIDE_LEN_DEFAULT;
			height = ExportPreviewTrees.SECONDARY_SIDE_LEN_DEFAULT;
		}

		/* Set up column tree image */
		BufferedImage treeSnapshot = null;
		ExportPreviewTrees expTrees = null;
		if(treeAxisView.isEnabled()) {
			treeSnapshot = treeAxisView.getSnapshot(width, height, region,
				withSelections);
			expTrees = new ExportPreviewTrees(treeSnapshot, isRows);
		}

		return expTrees;
	}

	/**
	 * Get an image of a tree for the export preview
	 * 
	 * @param treeAxisView - TRView object
	 * @param region - region of the matrix to export
	 * @param withSelections - Whether to include selection highlighting
	 * @param isRows - Whether this is for a row tree or not
	 * @param width - preview image width
	 * @param height - preview image height
	 * @param longMatrixEdge - Length of the matrix edge adjacent to the tree
	 * @return
	 */
	private ExportPreviewTrees getTreeSnapshot(TRView treeAxisView,
		RegionType region,final boolean withSelections,final boolean isRows,
		int width,int height,final int longMatrixEdge) {

		if(treeAxisView == null) {
			LogBuffer.println("Cannot generate tree snapshot. TRView object " +
				"is null.");
			return new ExportPreviewTrees(null, isRows); // empty panel
		}

		//The max preview matrix edge length is PRIMARY_SIDE_LEN_DEFAULT. If
		//there's not the same number of cols & rows, the long edge length is
		//smaller than the max.  The fraction smaller that the real long tree
		//edge is than the longest matrix edge is the fraction we must reduce
		//the max for this long edge
		double shrinkby =
			(double) ExportPreviewTrees.PRIMARY_SIDE_LEN_DEFAULT /
			(double) longMatrixEdge;
		int longLen =
			(int) Math.floor((double) (isRows ? height : width) * shrinkby);
		int shortLen = calculatePrevSecondaryLen(shrinkby,width,height,isRows);

		height = (isRows ? longLen : shortLen);
		width = (isRows ? shortLen : longLen);

		/* Set up column tree image */
		BufferedImage treeSnapshot = null;
		ExportPreviewTrees expTrees = null;
		if(treeAxisView.isEnabled()) {
			treeSnapshot = treeAxisView.getSnapshot(width, height, region,
				withSelections);
			expTrees = new ExportPreviewTrees(treeSnapshot,isRows,shortLen,
				longLen);
		}

		return expTrees;
	}

	/**
	 * This method determines the short length of the tree based on actual tree
	 * area dimensions and using the fixed long length for the preview for
	 * scaling down the short length
	 * 
	 * @param shrinkby - The ratio by which to shrink the image
	 * @param realWidth - export image width
	 * @param realHeight - export image height
	 * @param isRows - Whether this is for a row tree or not
	 */
	private int calculatePrevSecondaryLen(final double shrinkby,
		final int realWidth,final int realHeight,final boolean isRows) {

		int realShortLen = (isRows ? realWidth  : realHeight);
		int prevShortLen = (int) Math.round((double) realShortLen * shrinkby);
		if(prevShortLen < ExportPreviewTrees.D_MIN) {
			prevShortLen = ExportPreviewTrees.D_MIN;
		}
		return(prevShortLen);
	}

	/**
	 * Get a labels pane snapshot for the export preview
	 * 
	 * @param labelsAxisView - A LabelView object
	 * @param region - region of the matrix to export
	 * @param withSelections - Whether to include selection highlighting
	 * @param isRows - Whether this is for a row tree or not
	 * @param width - preview image width
	 * @param height - preview image height
	 * @param drawSelectionOnly - Only draw labels that are selected
	 * @param tileHeight - The height of the tiles in the final exported image
	 * @param fontSize - The height of the font in the final exported image
	 * @param longMatrixEdge - Length of the matrix edge adjacent to the tree
	 * @return
	 */
	private ExportPreviewLabels getLabelsSnapshot(LabelView labelsAxisView,
		RegionType region,final boolean withSelections,final boolean isRows,
		int width,int height,final boolean drawSelectionOnly,
		final int tileHeight,int fontSize,final int longMatrixEdge) {

		if(labelsAxisView == null) {
			LogBuffer.println("Cannot generate labels snapshot. Label object " +
				"is null.");
			return new ExportPreviewLabels(null,isRows); // empty panel
		}

		//Determine how much the preview needs to be shrunk
		double shrinkby =
			(double) ExportPreviewTrees.PRIMARY_SIDE_LEN_DEFAULT /
			(double) longMatrixEdge;

		int primaryLen =
			(int) Math.floor((double) (isRows ? height : width) * shrinkby);
		if(primaryLen == 0) {
			primaryLen = 1;
		}
		int secondaryLen = calculatePrevSecondaryLen(shrinkby,width,height,
			isRows);
		if(secondaryLen == 0) {
			secondaryLen = 1;
		}

		//Scale down the dimensions for the preview, but do not scale down the
		//other components such as font size, as that is needed to measure
		//string length which can be subsequently scaled
		height = (isRows ? primaryLen : secondaryLen);
		width = (isRows ? secondaryLen : primaryLen);

		/* Set up column label image */
		BufferedImage labelsSnapshot = null;
		ExportPreviewLabels expLabels = null;

		labelsSnapshot = labelsAxisView.getSnapshot(width,height,region,
			withSelections,drawSelectionOnly,tileHeight,fontSize,shrinkby);
		expLabels =
			new ExportPreviewLabels(labelsSnapshot,isRows,secondaryLen,
				primaryLen);

		return expLabels;
	}

	/**
	 * Getter for name
	 * @return name
	 */
	@Override
	public String getName() {

		return name;
	}

	/**
	 * Setter for name
	 * 
	 * @param name - Name string
	 */
	public void setName(final String name) {

		this.name = name;
	}

	/**
	 * Getter for scaleIncXY
	 * 
	 * @return
	 */
	public JButton getXYPlusButton() {

		return scaleIncXY;
	}

	/**
	 * Getter for scaleDecXY
	 * 
	 * @return
	 */
	public JButton getXYMinusButton() {

		return scaleDecXY;
	}

	/**
	 * Getter for scaleDefaultAll
	 * 
	 * @return
	 */
	public JButton getHomeButton() {

		return scaleDefaultAll;
	}

	/**
	 * Getter for zoomBtn
	 * 
	 * @return
	 */
	public JButton getZoomButton() {

		return zoomBtn;
	}

	/**
	 * Getter for matrixXscrollbar
	 * 
	 * @return
	 */
	public JScrollBar getMatrixXScroll() {

		return matrixXscrollbar;
	}

	/**
	 * Getter for matrixYscrollbar
	 * 
	 * @return
	 */
	public JScrollBar getMatrixYScroll() {

		return matrixYscrollbar;
	}

	/**
	 * Getter for colLabelScroll
	 * 
	 * @return
	 */
	public JScrollBar getColLabelLengthScroll() {

		return colLabelScroll;
	}

	/**
	 * Getter for rowLabelScroll
	 * 
	 * @return
	 */
	public JScrollBar getRowLabelLengthScroll() {

		return rowLabelScroll;
	}

	/**
	 * Getter for interactiveMatrixView
	 * 
	 * @return
	 */
	public InteractiveMatrixView getInteractiveMatrixView() {

		return interactiveMatrixView;
	}

	/**
	 * Getter for globalMatrixView
	 * 
	 * @return
	 */
	public GlobalMatrixView getGlobalMatrixView() {

		return globalMatrixView;
	}

	/**
	 * Getter for colLabelView
	 * 
	 * @return
	 */
	public LabelView getColLabelView() {

		return colLabelView;
	}

	/**
	 * Getter for colTreeView
	 * 
	 * @return
	 */
	public ColumnTreeView getColumnTreeView() {

		return colTreeView;
	}

	/**
	 * Getter for rowTreeView
	 * 
	 * @return
	 */
	public RowTreeView getRowTreeView() {

		return rowTreeView;
	}

	/**
	 * Getter for rowLabelView
	 * 
	 * @return
	 */
	public LabelView getRowLabelView() {

		return rowLabelView;
	}

	/**
	 * Getter for rowDataPane
	 * 
	 * @return
	 */
	public JSplitPane getRowSplitPane() {

		return rowDataPane;
	}

	/**
	 * Getter for colDataPane
	 * 
	 * @return
	 */
	public JSplitPane getColSplitPane() {

		return colDataPane;
	}

	/** Get the InputMap for the DendroPane. This is important for key binding.
	 *
	 * @return An InputMap object belonging to DendroView's dendroPane. */
	public InputMap getInputMap() {

		return dendroPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	/** Get the ActionMap for the DendroPane. This is important for key binding.
	 *
	 * @return An ActionMap object belonging to DendroView's dendroPane. */
	public ActionMap getActionMap() {

		return dendroPane.getActionMap();
	}

	/** Get a reference to the data ticker object.
	 *
	 * @return The DataTicker object for the active DendroView. */
	public DataTicker getDataTicker() {

		return ticker;
	}

	/** Returns the current location of the divider in the supplied TRView's
	 * JSplitPane
	 *
	 * @param dendrogram
	 * @return Location of divider (max = 1.0) */
	public double getDivLoc(final TRView dendrogram) {

		/* Get value for correct dendrogram JSplitPane */
		final JSplitPane treePane = (dendrogram == colTreeView) ?
			colDataPane : rowDataPane;

		final double abs_div_loc = treePane.getDividerLocation();
		final double max_div_loc = treePane.getMaximumDividerLocation();

		double rel_div_loc = abs_div_loc / max_div_loc;
		rel_div_loc = Helper.roundDouble(rel_div_loc, 2);

		return (rel_div_loc > 1.0) ? 1.0 : rel_div_loc;
	}

	/** Returns a boolean which indicates whether any Dendrogram is enabled.
	 *
	 * @return boolean */
	public boolean treesEnabled() {

		final boolean treesEnabled =
			rowTreeView.isEnabled() || colTreeView.isEnabled();
		return treesEnabled;
	}

	/**
	 * 
	 * @return the bORDER_THICKNESS
	 */
	public static int getBORDER_THICKNESS() {
		return(BORDER_THICKNESS);
	}

	/**
	 * 
	 * @return the mIN_GRID_CELL_SIZE
	 */
	public static int getMIN_GRID_CELL_SIZE() {
		return(MIN_GRID_CELL_SIZE);
	}

	/**
	 * 
	 * @return the lABEL_AREA_HEIGHT
	 */
	public static int getLABEL_AREA_HEIGHT() {
		return(LABEL_AREA_HEIGHT);
	}
}
