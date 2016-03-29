/* BEGIN_HEADER                                                   TreeView 3
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
import java.awt.event.ComponentListener;
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

import Utilities.GUIFactory;
import Utilities.Helper;
import Utilities.StringRes;
import edu.stanford.genetics.treeview.DataTicker;
import edu.stanford.genetics.treeview.DendroPanel;
import edu.stanford.genetics.treeview.ExportPreviewMatrix;
import edu.stanford.genetics.treeview.ExportPreviewTrees;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.TreeViewFrame;
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

	/* JScrollBars for GlobalView */
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

	/* TODO: This needs to be better integrated into the interface */
//	private JButton exportBtn;

	private HeaderFinderBox rowFinderBox;
	private HeaderFinderBox colFinderBox;

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

		searchPanel = GUIFactory.createJPanel(false, GUIFactory.NO_GAPS_OR_INSETS);
		dendroPane = GUIFactory.createJPanel(false, GUIFactory.TINY_GAPS_AND_INSETS);

		/* Create the Global view (JPanel to display) */
		globalMatrixView = new GlobalMatrixView();
		interactiveMatrixView = new InteractiveMatrixView();

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
		
		ticker = new DataTicker();

		setupScaleButtons();
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

		searchPanel.add(rowFinderBox.getSearchTermBox(), "w 80::, growx, "
				+ "pushx, al right");
		searchPanel.add(colFinderBox.getSearchTermBox(), "w 80::, growx, "
				+ "pushx, al right");

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
	 * Organizes the main layout for DendroView.
	 */
	public void setupLayout() {

		dendroPane.removeAll();

		JPanel toolbarPanel;
		JPanel matrixPanel;
				
		toolbarPanel = createToolbarPanel();
		
		setupRowDataPane();
		setupColDataPane();
		setDataPaneDividers();

		matrixPanel = createMatrixPanel();
		
		dendroPane.add(matrixPanel, "grow, push, wrap");
		dendroPane.add(toolbarPanel, "growx, pushx, h 3%, wrap");
		
		dendroPane.revalidate();
		dendroPane.repaint();
	}
	
	/**
	 * TODO currently only a DataTicker placeholder is realized.
	 * Creates a panel which contains the color-value indicator. This is
	 * used to display the data value of the currently hovered matrix pixel.
	 * @return JPanel containing the olor-value indicator.
	 */
	private JPanel createColorValIndicatorPanel() {
		
		// TODO currently just a data ticker displaying a tile's data value
		return ticker.getTickerPanel();
	}
	
	/**
	 * Creates a JPanel to hold the main navigation buttons for the matrix.
	 * @return A JPanel with all navigation buttons.
	 */
	private JPanel createNavBtnPanel() {
		
		JPanel navBtnPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_INSETS);
		navBtnPanel.add(scaleIncXY);
		navBtnPanel.add(scaleDecXY);
		navBtnPanel.add(zoomBtn);
		navBtnPanel.add(scaleDefaultAll);
		/* TODO: This needs to be better integrated into the interface */
//		navBtnPanel.add(exportBtn);
		
		return navBtnPanel;
	}
	
	/**
	 * Creates a button which contains search interface elements, such as 
	 * search bars. 
	 * @return A JPanel containing search bars.
	 */
	private JPanel createSearchBarPanel() {
		
		JPanel searchBarPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_INSETS_FILL);
		searchBarPanel.add(searchPanel);
		
		return searchBarPanel;
	}
	
	/**
	 * A parent JPanel to organize main navigation + info UI elements in a 
	 * toolbar like layout.
	 * @return A JPanel containing all major UI navigation + info elements.
	 */
	private JPanel createToolbarPanel() {
		
		JPanel navBtnPanel;
		JPanel searchBarPanel;
		JPanel colorValIndicatorPanel;
		JPanel toolbarPanel;
	
		colorValIndicatorPanel = createColorValIndicatorPanel();
		navBtnPanel = createNavBtnPanel();
		searchBarPanel = createSearchBarPanel();
		
		// Toolbar
		toolbarPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_GAPS_OR_INSETS);
		toolbarPanel.add(colorValIndicatorPanel, "al left, w 33%");
		toolbarPanel.add(navBtnPanel, "al center, pushx");
		toolbarPanel.add(searchBarPanel, "al right, w 33%");
		
		return toolbarPanel;
	}
	
	/**
	 * Creates a panel to hold the row dendrogram. This will be added to
	 * the left side of the corresponding JSplitPane. The dendrogram is only
	 * drawn if a model was clustered and a gtr-file exists.
	 * @return A JPanel with the row dendrogram.
	 */
	private JPanel createRowTreePanel() {
	
		JPanel rowTreePanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_GAPS_OR_INSETS);
		
		rowTreePanel.add(rowTreeView, "w 100%, pushy, growy, wrap");

		// hidemode is a MigLayout trick! Very important to keep trees aligned.
		rowTreePanel.add(rowTreeView.getHorizontalScrollBar(), "w 100%, "
				+ "hidemode 0"); 
		rowTreeView.getHorizontalScrollBar().setVisible(false);
		
		return rowTreePanel;
	}
	
	/**
	 * Creates a JPanel to hold the display of row labels.
	 * @return A JPanel holding the row LabelView.
	 */
	private JPanel createRowLabelPanel() {
		
		JPanel rowLabelPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_GAPS_OR_INSETS);

		rowLabelPanel.add(rowLabelView.getComponent(), "w 100%, pushy, growy, "
				+ "wrap");
		// Row-Label scrollbar in sync with colNavPanel via MigLayout!
		// Guaranteeing same size and alignment can only be done via manual add.
		rowLabelPanel.add(rowLabelView.getSecondaryScrollBar(), "w 100%");
		
		return rowLabelPanel;
	}
	
	/**
	 * Sets up the JSplitPane used to show row dendrogram and labels.
	 */
	private void setupRowDataPane() {

		JPanel rowLabelPanel;
		JPanel rowTreePanel;

		rowLabelPanel = createRowLabelPanel();
		//Allow the user to drag the divider to completely hide the labels
		rowLabelPanel.setMinimumSize(new Dimension(0,0));

		rowTreePanel = createRowTreePanel();
		//Allow the user to drag the divider to completely hide the trees
		rowTreePanel.setMinimumSize(new Dimension(0,0));

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
	
	/**
	 * Creates a panel to hold the column dendrogram. This will be added to
	 * the top side of the corresponding JSplitPane. The dendrogram is only
	 * drawn if a model was clustered and a atr-file exists.
	 * @return A JPanel with the column dendrogram.
	 */
	private JPanel createColTreePanel() {
		
		JPanel colTreePanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_GAPS_OR_INSETS);
		
		colTreePanel.add(colTreeView, "push, growx, h 100%");
		
		// hidemode is a MigLayout trick! Very important to keep trees aligned.
		colTreePanel.add(colTreeView.getVerticalScrollBar(), "h 100%, "
				+ "hidemode 0");
		colTreeView.getVerticalScrollBar().setVisible(false);
		
		return colTreePanel;
	}
	
	/**
	 * Creates a JPanel to hold the display of column labels.
	 * @return A JPanel holding the column LabelView.
	 */
	private JPanel createColLabelPanel() {
		
		JPanel colLabelPanel;
		
		colLabelPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_GAPS_OR_INSETS);
		colLabelPanel.add(colLabelView.getComponent(), "h 100%, pushx, growx");
		// Col-Label scrollbar in sync with rowNavPanel via MigLayout!
		// Guaranteeing same size and alignment can only be done via manual add.
		colLabelPanel.add(colLabelView.getSecondaryScrollBar(), "h 100%");
		
		return colLabelPanel;
	}
	
	/**
	 * Sets up the JSplitPane used to show column dendrogram and labels.
	 */
	private void setupColDataPane() {

		JPanel colTreePanel;
		JPanel colLabelPanel;

		colTreePanel = createColTreePanel();
		//Allow the user to drag the divider to completely hide the trees
		colTreePanel.setMinimumSize(new Dimension(0,0));

		colLabelPanel = createColLabelPanel();
		//Allow the user to drag the divider to completely hide the labels
		colLabelPanel.setMinimumSize(new Dimension(0,0));

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
	
	/**
	 * Creates container for the GlobalOverViewMatrix.
	 * @return JPanel holding the GlobalOverviewMatrix instance.
	 */
	private JPanel createGlobalOverviewPanel() {
		
		JPanel globalOverviewPanel;
		
		globalOverviewPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_GAPS_OR_TOPLEFT_INSETS);
		globalOverviewPanel.add(globalMatrixView, "grow, push");
		
		return globalOverviewPanel;
	}
	
	/**
	 * Creates the InteractiveMatrixView column navigation panel. This includes
	 * the column scrollbar and 4 scaling buttons (2 on each side).
	 * @return JPanel holding column navigation components.
	 */
	private JPanel createColNavPanel() {
		
		JPanel colNavPanel;
		
		colNavPanel = GUIFactory.createJPanel(false, GUIFactory.NO_GAPS_OR_INSETS);
		colNavPanel.add(scaleAddLeftX);
		colNavPanel.add(scaleRemoveLeftX);

		colNavPanel.add(matrixXscrollbar, "w 100%, growx, pushx");
		
		colNavPanel.add(scaleRemoveRightX);
		colNavPanel.add(scaleAddRightX);
		
		return colNavPanel;
	}
	
	/**
	 * Creates the InteractiveMatrixView row navigation panel. This includes
	 * the row scrollbar and 4 scaling buttons (2 on each side).
	 * @return JPanel holding row navigation components.
	 */
	private JPanel createRowNavPanel() {
		
		JPanel rowNavPanel;
		
		rowNavPanel = GUIFactory.createJPanel(false, GUIFactory.NO_GAPS_OR_INSETS);
		
		rowNavPanel.add(scaleAddTopY, "wrap");
		rowNavPanel.add(scaleRemoveTopY, "wrap");
		
		rowNavPanel.add(matrixYscrollbar, "growy, push, wrap");
		
		rowNavPanel.add(scaleRemoveBottomY, "wrap");
		rowNavPanel.add(scaleAddBottomY, "");
		
		return rowNavPanel;
	}
	
	/**
	 * Panel that holds the main components for the interactive matrix.
	 * This includes InteractiveMatrixView itself as well as row and column
	 * navigation panels.
	 * @return A JPanel with the interactive matrix view setup.
	 */
	private JPanel createInteractiveMatrixPanel() {
		
		JPanel interactiveMatrixPanel;
		JPanel colNavPanel;
		JPanel rowNavPanel;
		
		colNavPanel = createColNavPanel();
		rowNavPanel = createRowNavPanel();
		
		interactiveMatrixPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_GAPS_OR_INSETS);
		interactiveMatrixPanel.add(interactiveMatrixView, "push, grow");
		interactiveMatrixPanel.add(rowNavPanel, "h 100%, wrap");
		interactiveMatrixPanel.add(colNavPanel, "w 100%");
		
		return interactiveMatrixPanel;
	}
	
	/**
	 * Creates the full main matrix panel which includes all components
	 * making up a full DendroView with the exception of the toolbar related
	 * elements such as buttons or search.
	 * @return A JPanel with all main views arranged in it.
	 */
	private JPanel createMatrixPanel() {
		
		JPanel matrixPanel;
		
		JPanel globalOverviewPanel;
		JPanel interactiveMatrixPanel;
		
		globalOverviewPanel = createGlobalOverviewPanel();
		interactiveMatrixPanel = createInteractiveMatrixPanel();
		
		matrixPanel = GUIFactory.createJPanel(false, GUIFactory.TINY_GAPS_AND_INSETS);
		matrixPanel.add(globalOverviewPanel, "h 180!, w 180!, grow 0");
		matrixPanel.add(colDataPane, "h 180!, pushx, "
				+ "growx, growy 0, wrap");
		matrixPanel.add(rowDataPane, "w 180!, pushy, growy, "
				+ "growx 0");
		matrixPanel.add(interactiveMatrixPanel, "grow");
		
		return matrixPanel;
	}
	
	/**
	 * Looks up the stored location values for the JSplitPane dividers.
	 * This is needed for "Show-Hide" trees. It determines how much of
	 * labels vs. tree panel is shown.
	 */
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
	 * Sets up the JButtons which control scaling and zooming.
	 */
	private void setupScaleButtons() {

		scaleDefaultAll = GUIFactory.createIconBtn(StringRes.icon_home);
		scaleDefaultAll.setToolTipText("Reset the zoomed view");

		int btnSize = 15;
		
		/* Scale x-axis */
		scaleAddRightX = GUIFactory.createSquareBtn("+", btnSize);
		scaleAddRightX.setToolTipText(StringRes.tt_xZoomIn_right);

		scaleRemoveRightX = GUIFactory.createSquareBtn("-", btnSize);
		scaleRemoveRightX.setToolTipText(StringRes.tt_xZoomOut_right);

		scaleAddLeftX = GUIFactory.createSquareBtn("+", btnSize);
		scaleAddLeftX.setToolTipText(StringRes.tt_xZoomIn_left);

		scaleRemoveLeftX = GUIFactory.createSquareBtn("-", btnSize);
		scaleRemoveLeftX.setToolTipText(StringRes.tt_xZoomOut_left);

		/* Scale y-axis */
		scaleAddBottomY = GUIFactory.createSquareBtn("+", btnSize);
		scaleAddBottomY.setToolTipText(StringRes.tt_yZoomIn_bottom);
		
		scaleRemoveBottomY = GUIFactory.createSquareBtn("-", btnSize);
		scaleRemoveBottomY.setToolTipText(StringRes.tt_yZoomOut_bottom);
		
		scaleAddTopY = GUIFactory.createSquareBtn("+", btnSize);
		scaleAddTopY.setToolTipText(StringRes.tt_yZoomIn_top);
		
		scaleRemoveTopY = GUIFactory.createSquareBtn("-", btnSize);
		scaleRemoveTopY.setToolTipText(StringRes.tt_yZoomOut_top);

		/* Scale both axes */
		scaleIncXY = GUIFactory.createIconBtn(StringRes.icon_fullZoomIn);
		scaleIncXY.setToolTipText(StringRes.tt_xyZoomIn);

		scaleDecXY = GUIFactory.createIconBtn(StringRes.icon_fullZoomOut);
		scaleDecXY.setToolTipText(StringRes.tt_xyZoomOut);

		/* Reset zoom */
		zoomBtn = GUIFactory.createIconBtn(StringRes.icon_zoomAll);
		zoomBtn.setToolTipText(StringRes.tt_home);

		/* TODO: This needs to be better integrated into the interface */
//		exportBtn = GUIFactory.createSquareBtn("X",39);
//		exportBtn.setToolTipText("Export image to file");
	}

	/**
	 * Used to update the JMenuItem field "Show trees/ Hide trees" depending
	 * on the current status of the divider.
	 */
	public void updateTreeMenuBtn() {

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

		/* TODO: This needs to be better integrated into the interface */
//		exportBtn.addActionListener(l);
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

		// TODO readd once copy-from-selection is available
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

	/**
	 * Setter for viewFrame
	 */
	public void setViewFrame(final TreeViewFrame viewFrame) {

		this.tvFrame = viewFrame;
	}

	public void setSearchTermBoxes() {

		this.rowFinderBox = new RowFinderBox();
		this.colFinderBox = new ColumnFinderBox();

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
		return(rowFinderBox.getSearchTermBox().getEditor()
			.getEditorComponent().isFocusOwner() ||
			colFinderBox.getSearchTermBox().getEditor()
			.getEditorComponent().isFocusOwner());
	}

	public void updateSearchTermBoxes(final HeaderInfo rowHI,
			final HeaderInfo columnHI, final MapContainer xmap,
			final MapContainer ymap) {

		rowFinderBox.setHeaderInfo(rowHI);
		rowFinderBox.setHeaderSummary(getRowLabelView().getHeaderSummary());
		rowFinderBox.setMapContainers(ymap, xmap);
		rowFinderBox.setSelection(tvFrame.getRowSelection(),
				tvFrame.getColSelection());
		rowFinderBox.setNewSearchTermBox();

		colFinderBox.setHeaderInfo(columnHI);
		colFinderBox.setHeaderSummary(getColumnLabelView().getHeaderSummary());
		colFinderBox.setMapContainers(xmap, ymap);
		colFinderBox.setSelection(tvFrame.getColSelection(),
				tvFrame.getRowSelection());
		colFinderBox.setNewSearchTermBox();

		setSearchPanel();
	}
	
	
	public ExportPreviewMatrix getMatrixSnapshot(final boolean withSelections) {
		
		Image image = getInteractiveMatrixView()
				.getVisibleImage(withSelections);
		return new ExportPreviewMatrix(image);
	}
	
	public ExportPreviewTrees getRowTreeSnapshot(final boolean withSelections) {
		
		return getTreeSnapshot(rowTreeView, withSelections, true);
	}
	
	public ExportPreviewTrees getColTreeSnapshot(final boolean withSelections) {
		
		return getTreeSnapshot(colTreeView, withSelections, false);
	}
	
	private ExportPreviewTrees getTreeSnapshot(TRView treeAxisView, 
			final boolean withSelections, final boolean isRows) {
		
		if(treeAxisView == null) {
			LogBuffer.println("Cannot generate tree snapshot. "
					+ "TRView object is null.");
			return new ExportPreviewTrees(null, isRows); // empty panel
		}
		
		int width;
		int height;
		if(isRows) {
			width = ExportPreviewTrees.SHORT;
			height = ExportPreviewTrees.D_LONG;
		} else {
			width = ExportPreviewTrees.D_LONG;
			height = ExportPreviewTrees.SHORT;
		}
		
		/* Set up column tree image */
		BufferedImage treeSnapshot = null;
		ExportPreviewTrees expTrees = null;
		if(treeAxisView.isEnabled()) {
			treeSnapshot = treeAxisView.getSnapshot(width, height, 
					withSelections);
			expTrees = new ExportPreviewTrees(treeSnapshot, isRows);
		}
		
		return expTrees;
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

	/* TODO: This needs to be better integrated into the interface */
//	public JButton getExportButton() {
//
//		return exportBtn;
//	}

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

	/**
	 * Get the InputMap for the DendroPane. This is important for key binding.
	 * @return An InputMap object belonging to DendroView's dendroPane.
	 */
	public InputMap getInputMap() {

		return dendroPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	/**
	 * Get the ActionMap for the DendroPane. This is important for key binding.
	 * @return An ActionMap object belonging to DendroView's dendroPane.
	 */
	public ActionMap getActionMap() {

		return dendroPane.getActionMap();
	}
	
	/**
	 * Get a reference to the data ticker object. 
	 * @return The DataTicker object for the active DendroView.
	 */
	public DataTicker getDataTicker() {
		
		return ticker;
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

		final double abs_div_loc = treePane.getDividerLocation();
		final double max_div_loc = treePane.getMaximumDividerLocation();

		double rel_div_loc = abs_div_loc / max_div_loc;
		rel_div_loc = Helper.roundDouble(rel_div_loc, 2);

		return (rel_div_loc > 1.0) ? 1.0 : rel_div_loc;
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
