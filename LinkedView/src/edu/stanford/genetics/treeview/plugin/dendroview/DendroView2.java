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

import java.awt.Color;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;

import edu.stanford.genetics.treeview.DendroPanel;
import edu.stanford.genetics.treeview.GUIUtils;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.StringRes;
import edu.stanford.genetics.treeview.TVScrollBarUI;
import edu.stanford.genetics.treeview.TabbedSettingsPanel;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.core.ArrayFinderBox;
import edu.stanford.genetics.treeview.core.GeneFinderBox;
import edu.stanford.genetics.treeview.core.HeaderFinderBox;

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
public class DendroView2 implements Observer, DendroPanel {

	// Instance Variables
	protected int gtr_div_size = 0;
	protected int atr_div_size = 0;

	// Container JFrame
	protected TreeViewFrame tvFrame;

	// Name
	private String name;

	// Main container JPanel
	private final JPanel dendroPane;

	protected ScrollPane panes[];
	private boolean loaded;

	// Map Views
	private final GlobalView2 globalview;

	// Trees
	protected final GTRView gtrview;
	protected final ATRView atrview;

	// Row and column names
	protected final TextView textview;
	protected final ArrayNameView arraynameview;

	protected JScrollBar globalXscrollbar;
	protected JScrollBar globalYscrollbar;

	// Persistent popups
	protected JDialog settingsFrame;
	protected TabbedSettingsPanel settingsPanel;

	// JMenuItems
	private JMenuItem colorMenuItem;
	private JMenuItem annotationsMenuItem;
	private JMenuItem isolateMenu;

	// JButtons
	private JButton zoomButton;
	private JButton scaleIncX;
	private JButton scaleIncY;
	private JButton scaleDecX;
	private JButton scaleDecY;
	private JButton scaleDefaultAll;
	
	private JToggleButton toggleScale;
	
	// GlobalView default sizes as ints to keep track.
	private double gvWidth;
	private double gvHeight;
	
	private double maxGVWidth;
	private double maxGVHeight;
	
	private double widthChange = 0;
	private double heightChange = 0;


	// Booleans
	private boolean showTrees = false;

	// Selections
	private TreeSelectionI geneSelection = null;
	private TreeSelectionI arraySelection = null;

	/**
	 * Chained constructor for the DendroView object note this will reuse any
	 * existing MainView subnode of the documentconfig.
	 * 
	 * @param tVModel
	 *            model this DendroView is to represent
	 * @param vFrame
	 *            parent ViewFrame of DendroView
	 */
	public DendroView2(final TreeViewFrame tvFrame) {

		this(tvFrame, "Dendrogram");
	}

	public DendroView2(final Preferences root, final TreeViewFrame tvFrame) {

		this(tvFrame, "Dendrogram");
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
	public DendroView2(final TreeViewFrame tvFrame, final String name) {

		this.tvFrame = tvFrame;
		this.name = "DendroView";
		this.loaded = false;

		dendroPane = GUIUtils.createJPanel(false, false, null);
		
		// Create the Global view (JPanel to display)
		globalview = new GlobalView2();

		// scrollbars, mostly used by maps
		globalXscrollbar = globalview.getXScroll();
		globalXscrollbar.setUI(new TVScrollBarUI());
		globalYscrollbar = globalview.getYScroll();
		globalYscrollbar.setUI(new TVScrollBarUI());
		
		toggleScale = GUIUtils.setToggleButtonLayout("Change Matrix Size");

		// Set up the column name display
//		arraynameview = new ArrayNameViewManager();
		arraynameview = new ArrayNameView();
		// new ArrayNameView(
		// tvFrame.getDataModel().getArrayHeaderInfo());
		// arraynameview.setUrlExtractor(viewFrame.getArrayUrlExtractor());

		textview = new TextView();

		// Set up row dendrogram
		gtrview = new GTRView();
		gtrview.getHeaderSummary().setIncluded(new int[] { 0, 3 });

		// Set up column dendrogram
		atrview = new ATRView();
		atrview.getHeaderSummary().setIncluded(new int[] { 0, 3 });
	}

	/**
	 * Returns the dendroPane so it can be displayed in TVFrame.
	 * 
	 * @return JPanel dendroPane
	 */
	public JPanel makeDendroPanel() {

//		arraynameview.generateView(tvFrame.getDataModel()
//				.getArrayHeaderInfo(), tvFrame.getUrlExtractor(),
//				tvFrame.getDataModel());
		arraynameview.generateView(tvFrame.getDataModel()
				.getArrayHeaderInfo(), tvFrame.getUrlExtractor());
		
		textview.generateView(tvFrame.getDataModel()
				.getGeneHeaderInfo(), tvFrame.getUrlExtractor(), -1);
		
		globalview.setHeaderSummary(textview.getHeaderSummary(), 
				arraynameview.getHeaderSummary());
		
		// Register Views
		registerView(globalview);
		registerView(atrview);
		registerView(arraynameview);
		registerView(textview);
		registerView(gtrview);

		// reset persistent popups
		settingsFrame = null;
		settingsPanel = null;
		
		return dendroPane;
	}

	/**
	 * Manages the component layout in TreeViewFrame
	 */
	public void setupLayout() {

		// Clear panel
		dendroPane.removeAll();
		
		// Set color scheme
		setComponentColors();

		// Components for layout setup
		JPanel buttonPanel;
		JPanel crossPanel;
		JPanel textpanel;
		JPanel arrayNamePanel;
		JPanel arrayContainer;
		JPanel geneContainer;
		JPanel navPanel;
		JPanel globalViewContainer;
		JPanel navContainer;
		JSplitPane gtrPane = null;
		JSplitPane atrPane = null;
		
		JPanel firstPanel;
		JPanel bottomPanel;

		// Buttons
		scaleDefaultAll = GUIUtils.setButtonLayout(null, "homeIcon");
		scaleDefaultAll.setToolTipText("Reset the zoomed view");

		scaleIncX = GUIUtils.setButtonLayout(null, "zoomInIcon");
		scaleIncX.setToolTipText(StringRes.tooltip_xZoomIn);

		scaleDecX = GUIUtils.setButtonLayout(null, "zoomOutIcon");
		scaleDecX.setToolTipText(StringRes.tooltip_xZoomOut);

		scaleIncY = GUIUtils.setButtonLayout(null, "zoomInIcon");
		scaleIncY.setToolTipText(StringRes.tooltip_yZoomIn);

		scaleDecY = GUIUtils.setButtonLayout(null, "zoomOutIcon");
		scaleDecY.setToolTipText(StringRes.tooltip_yZoomOut);

		zoomButton = GUIUtils.setButtonLayout(null, "fullscreenIcon");
		zoomButton.setToolTipText(StringRes.tooltip_home);

		// Panels
		buttonPanel = GUIUtils.createJPanel(false, true, null);
		
		crossPanel = GUIUtils.createJPanel(false, true, null);
		
		globalViewContainer = GUIUtils.createJPanel(false, false, null); 
		
		navContainer = GUIUtils.createJPanel(true, false, null);

		bottomPanel = GUIUtils.createJPanel(false, true, null); 
		
		arrayContainer = GUIUtils.createJPanel(false, false, null);
		
		geneContainer = GUIUtils.createJPanel(false, false, null);

		firstPanel = GUIUtils.createJPanel(true, true, null);
		firstPanel.setBorder(null);

		navPanel = GUIUtils.createJPanel(false, true, null);
		navPanel.setBorder(null);

		textpanel = GUIUtils.createJPanel(false, false, null); 

		arrayNamePanel = GUIUtils.createJPanel(false, false, null);

		if (gtrview.isEnabled() || atrview.isEnabled()) {
			tvFrame.getTreeButton().setEnabled(true);

		} else {
			tvFrame.getTreeButton().setEnabled(false);
			tvFrame.getTreeButton().setText("");
		}

		if (showTrees) {
			gtrPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gtrview,
					textpanel);
			gtrPane.setResizeWeight(0.5);
			gtrPane.setOpaque(false);
			gtrPane.setBorder(null);
			gtr_div_size = 3;
			gtrPane.setDividerSize(gtr_div_size);

			atrPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, atrview,
					arrayNamePanel);
			atrPane.setResizeWeight(0.5);
			atrPane.setOpaque(false);
			atrPane.setBorder(null);
			atr_div_size = 3;
			atrPane.setDividerSize(atr_div_size);
		}

		// Adding Components onto each other
		textpanel.add(textview.getComponent(), "push, grow");
		arrayNamePanel.add(arraynameview.getComponent(), "push, grow");
		
		globalViewContainer.add(globalview, "w 99%, h 99%, push, alignx 50%, "
				+ "aligny 50%");
		globalViewContainer.add(globalYscrollbar, "w 1%, h 100%, wrap");
		globalViewContainer.add(globalXscrollbar, "span, pushx, alignx 50%, "
				+ "w 100%, h 1%");

		crossPanel.add(scaleIncY, "span, alignx 50%, wrap");
		crossPanel.add(scaleDecX);
		crossPanel.add(zoomButton);
		crossPanel.add(scaleIncX, "wrap");
		crossPanel.add(scaleDecY, "span, alignx 50%");

		buttonPanel.add(crossPanel, "pushx, alignx 50%, wrap");

		navPanel.add(buttonPanel, "pushx, h 20%, w 90%, alignx 50%, wrap");
		navPanel.add(scaleDefaultAll, "push, alignx 50%, aligny 5%, wrap");
		navPanel.add(toggleScale, "push, alignx 50%, aligny 5%");
		
		navContainer.add(navPanel, "push, h 50%, alignx 100%, aligny 50%");
		
		JScrollBar arrayScroll = arraynameview.getYScroll();
		JScrollBar geneScroll = textview.getXScroll();
		
		// Widths
		double textViewCol = 0;
		double firstPanelCol = 0;
		double navCol = 10.5;
		double halfWidthChange = 0.0;
		
		// Heights
		double arrayRow = 0;
		double bottomRow = 5;
		double halfHeightChange = 0.0;

		// Layout parameters depend on tree visibility and atr/gtr availability
		if (showTrees && (atrview.isEnabled() && gtrview.isEnabled())) {
			maxGVWidth = 72;
			maxGVHeight = 75;
			
			firstPanelCol = 18.5;
			textViewCol = 18.5;
			
			arrayRow = 20;
			
			arrayContainer.add(atrPane, "w 99%, h 100%");
			geneContainer.add(gtrPane, "w 100%, h 99%, wrap");

		} else if (showTrees && (atrview.isEnabled() && !gtrview.isEnabled())) {
			maxGVWidth = 80;
			maxGVHeight = 75;
			
			firstPanelCol = 18.5;
			textViewCol = 10.5;
			
			arrayRow = 20;
			
			arrayContainer.add(atrPane, "w 99%, h 100%");
			geneContainer.add(textpanel, "w 100%, h 99%, wrap");

		} else if (showTrees && (!atrview.isEnabled() && gtrview.isEnabled())) {
			maxGVWidth = 72;
			maxGVHeight = 85;
			
			firstPanelCol = 18.5;
			textViewCol = 18.5;
			
			arrayRow = 10;
			
			arrayContainer.add(arrayNamePanel, "w 99%, h 100%");
			geneContainer.add(gtrPane, "w 100%, h 99%, wrap");

		} else {
			maxGVWidth = 80;
			maxGVHeight = 85;
			
			firstPanelCol = 10.5;
			textViewCol = 10.5;
			
			arrayRow = 10;
			
			arrayContainer.add(arrayNamePanel, "w 99%, h 100%");
			geneContainer.add(textpanel, "w 100%, h 99%, wrap");
		}
				
		arrayContainer.add(arrayScroll, "w 1%, h 100%");
		geneContainer.add(geneScroll, "w 100%, h 1%");
		
		gvWidth = maxGVWidth + widthChange;
		gvHeight = maxGVHeight + heightChange;
		
		halfWidthChange = widthChange/ 2.0;
		halfHeightChange = heightChange/ 2.0;
		
		// Adjusting widths and heights
		textViewCol = textViewCol - halfWidthChange;
		firstPanelCol = firstPanelCol - halfWidthChange;
		navCol = navCol - halfWidthChange;
		
		arrayRow = arrayRow - halfHeightChange;
		bottomRow = bottomRow - halfHeightChange;
		
		// Adding all components to the dendroPane
		dendroPane.add(firstPanel, "w " + firstPanelCol + "%, "
				+ "h " + arrayRow + "%");
		
		dendroPane.add(arrayContainer, "w " + gvWidth + "%, "
				+ "h " + arrayRow + "%");
		
		dendroPane.add(navContainer, "span 1 3, w " + navCol + "%, h 100%, "
				+ "wrap");
		
		dendroPane.add(geneContainer, "w " + textViewCol + "%, "
				+ "h " + gvHeight + "%");
		
		dendroPane.add(globalViewContainer, "w " + gvWidth + "%, "
				+ "h " + gvHeight + "%, wrap");
		
		dendroPane.add(bottomPanel, "span, h " + bottomRow + "%");
		
		dendroPane.revalidate();
		dendroPane.repaint();
	}
	
	public void setComponentColors() {
		
		dendroPane.setBackground(GUIUtils.BG_COLOR);
		arraynameview.setColors();
		textview.setColors();
	}

	// Add Button Listeners
	/**
	 * Adds an ActionListener to the scale buttons in DendroView.
	 * 
	 * @param l
	 */
	public void addScaleListener(final ActionListener l) {

		scaleIncX.addActionListener(l);
		scaleDecX.addActionListener(l);
		scaleIncY.addActionListener(l);
		scaleDecY.addActionListener(l);
		scaleDefaultAll.addActionListener(l);
	}

	/**
	 * Adds an ActionListener to the Zoom button in DendroView.
	 * 
	 * @param l
	 */
	public void addZoomListener(final ActionListener l) {

		zoomButton.addActionListener(l);
	}

	/**
	 * Adds a component listener to the main panel of DendroView.
	 * 
	 * @param l
	 */
	public void addCompListener(final ComponentListener l) {

		// tvFrame.getAppFrame().getContentPane().addComponentListener(l);
		getDendroPane().addComponentListener(l);
	}

	public void addSearchButtonListener(final MouseListener l) {

		tvFrame.getSearchButton().addMouseListener(l);
	}

	public void addTreeButtonListener(final MouseListener l) {

		tvFrame.getTreeButton().addMouseListener(l);
	}

	public void addSearchButtonClickListener(final ActionListener l) {

		tvFrame.getSearchButton().addActionListener(l);
	}

	public void addTreeButtonClickListener(final ActionListener l) {

		tvFrame.getTreeButton().addActionListener(l);
	}

	// Methods
	/**
	 * Redoing all the layout if parameters changed.
	 */
	@Override
	public void refresh() {

		dendroPane.revalidate();
		dendroPane.repaint();
	}

	/**
	 * 
	 * @param o
	 * @param arg
	 */
	@Override
	public void update(final Observable o, final Object arg) {

		if (o == geneSelection) {
			gtrview.scrollToNode(geneSelection.getSelectedNode());

		} else if (o == arraySelection) {
			atrview.scrollToNode(arraySelection.getSelectedNode());
		}
	}

	/**
	 * registers a modelview with the viewFrame.
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
	@Override
	public void setTreesVisible(final boolean visible) {

		this.showTrees = visible;
	}
	
	/**
	 * Sets the status of the JCheckBox that controls whether GlobalView
	 * will be resized or not.
	 * 
	 * @param visible
	 */
	public void setGVLocked(final boolean locked) {

		toggleScale.setSelected(locked);
	}
	
	/**
	 * Returns the status of the JCheckBox that controls whether GlobalView
	 * will be resized or not.
	 * @return
	 */
	public boolean getGVLocked() {
		
		return toggleScale.isSelected();
	}

	/**
	 * Opens a JWindow containing Swing components used to search data by name
	 * in the loaded TVModel.
	 */
	@Override
	public void openSearchDialog() {

		final JDialog dialog = new JDialog();
		dialog.setModalityType(JDialog.DEFAULT_MODALITY_TYPE);
		dialog.setTitle(StringRes.dialog_title_search);
		dialog.setResizable(false);

		final JPanel container = GUIUtils.createJPanel(true, true, null); //new JPanel();
//		container.setLayout(new MigLayout());
//		container.setBackground(GUIUtils.BG_COLOR);
		container.setBorder(BorderFactory.createEtchedBorder(GUIUtils.BORDERS,
				GUIUtils.BG_COLOR));
		
		JLabel wildTip = GUIUtils.createSmallLabel("Tip: Edit the search "
				+ "fields to search for terms.");
//		wildTip.setFont(GUIUtils.FONTS);
//		wildTip.setForeground(GUIUtils.TEXT);
		
		JLabel wildTip2 = GUIUtils.createSmallLabel("Wildcards ('*', '?') "
				+ "are supported.");
//		wildTip2.setFont(GUIUtils.FONTS);
//		wildTip2.setForeground(GUIUtils.TEXT);
		
		final JButton closeButton = GUIUtils.setButtonLayout("Close", null);
		closeButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				dialog.dispose();
			}
		});

		container.add(getGeneFinderPanel(), "w 90%, h 40%, "
				+ "alignx 50%, wrap");
		container.add(getArrayFinderPanel(), "w 90%, h 40%, "
				+ "alignx 50%, wrap");
		container.add(wildTip, "span, wrap");
		container.add(wildTip2, "span, wrap");
		container.add(closeButton, "push, alignx 100%");

		dialog.getContentPane().add(container);
		dialog.pack();
		dialog.setLocationRelativeTo(tvFrame.getAppFrame());
		dialog.setVisible(true);
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

		annotationsMenuItem = new JMenuItem(StringRes.menu_title_RowAndCol);
		menu.add(annotationsMenuItem);
		tvFrame.addToStackMenuList(annotationsMenuItem);

		colorMenuItem = new JMenuItem("Color Settings");
		menu.add(colorMenuItem);
		tvFrame.addToStackMenuList(colorMenuItem);
		
//		menu.addSeparator();
//		
//		isolateMenu = new JMenuItem("Isolate Selected");
//		menu.add(isolateMenu);
//		tvFrame.addToStackMenuList(isolateMenu);
	}

	@Override
	public void addClusterMenus(final JMenu menu) {

		// Cluster Menu
		final JMenuItem hierMenuItem = new JMenuItem(
				StringRes.menu_title_Hier);
		menu.add(hierMenuItem);
		tvFrame.addToStackMenuList(hierMenuItem);

		final JMenuItem kMeansMenuItem = new JMenuItem(
				StringRes.menu_title_KMeans);
		menu.add(kMeansMenuItem);
		tvFrame.addToStackMenuList(kMeansMenuItem);
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

	// Setters
	/**
	 * Sets the boolean which identifies the loaded status of DendroView.
	 * Will be true if a file has been loaded into DendroView.
	 * @param loaded
	 */
	public void setLoaded(boolean loaded) {
		
		this.loaded = loaded;
	}
	
	
	/**
	 * Shows whether the status of this DendroView is loaded or not.
	 * @return
	 */
	public boolean isLoaded() {
		
		return loaded;
	}
	
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
	
	public double getWidthChange() {
		
		return widthChange;
	}
	
	public void setWidthChange(double change) {
		
		this.widthChange = widthChange + change;
	}
	
	public double getHeightChange() {
		
		return heightChange;
	}
	
	public void setHeightChange(double change) {
		
		this.heightChange = heightChange + change;
	}
	
	public double getMaxGVWidth() {
		
		return maxGVWidth;
	}
	
	public double getMaxGVHeight() {
		
		return maxGVHeight;
	}
	
	// Selection methods
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
		atrview.setArraySelection(arraySelection);
		textview.setArraySelection(arraySelection);
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
		gtrview.setGeneSelection(geneSelection);
		textview.setGeneSelection(geneSelection);
		arraynameview.setGeneSelection(geneSelection);
	}

	/**
	 * Setter for viewFrame
	 */
	public void setViewFrame(final TreeViewFrame viewFrame) {

		this.tvFrame = viewFrame;
	}

	public JPanel getGeneFinderPanel() {

		final HeaderFinderBox geneFinderBox = new GeneFinderBox(tvFrame, this,
				tvFrame.getDataModel().getGeneHeaderInfo(), 
				getTextview().getHeaderSummary(),
				tvFrame.getGeneSelection());

		final JPanel contentPanel = geneFinderBox.getContentPanel();

		return contentPanel;
	}

	/**
	 * Getter for geneFinderPanel
	 * 
	 * @return HeaderFinderPanel arrayFinderPanel
	 */
	public JPanel getArrayFinderPanel() {

		final HeaderFinderBox arrayFinderBox = new ArrayFinderBox(tvFrame,
				this, tvFrame.getDataModel().getArrayHeaderInfo(), 
				getArraynameview().getHeaderSummary(),
				tvFrame.getArraySelection());

		final JPanel contentPanel = arrayFinderBox.getContentPanel();

		return contentPanel;
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

	public JButton getXMinusButton() {

		return scaleDecX;
	}

	public JButton getYPlusButton() {

		return scaleIncY;
	}

	public JButton getYMinusButton() {

		return scaleDecY;
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

	public GlobalView2 getGlobalView() {

		return globalview;
	}

	public JPanel getDendroPane() {

		return dendroPane;
	}

	public TreeSelectionI getGeneSelection() {

		return geneSelection;
	}

	public TreeSelectionI getArraySelection() {

		return arraySelection;
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

	public TextView getTextview() {

		return textview;
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

		boolean enabled = false;

		if (gtrview.isEnabled() || atrview.isEnabled()) {
			enabled = true;

		}

		return enabled;
	}
}
