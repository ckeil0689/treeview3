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
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSplitPane;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.DendroPanel;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.ModelView;
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
	private JPanel dendroPane;

	protected ScrollPane panes[];
	protected boolean loaded;

	// Map Views
	private GlobalView globalview;

	// Trees
	protected GTRView gtrview;
	protected ATRView atrview;

	// Row and Column names
	protected TextViewManager textview;
	protected ArrayNameView arraynameview;

	protected JScrollBar globalXscrollbar;
	protected JScrollBar globalYscrollbar;

	// persistent popups
	protected JDialog settingsFrame;
	protected TabbedSettingsPanel settingsPanel;
	
	// JMenuItems
	private JMenuItem colorMenuItem;
	private JMenuItem annotationsMenuItem;
	
	// JButtons
	private JButton zoomButton;
	private JButton scaleIncX;
	private JButton scaleIncY;
	private JButton scaleDecX;
	private JButton scaleDecY;
	private JButton scaleDefaultAll;
	
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
	public DendroView2(TreeViewFrame tvFrame) {

		this(tvFrame, "Dendrogram");
	}

	public DendroView2(final ConfigNode root, final TreeViewFrame tvFrame) {

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

		dendroPane = new JPanel();
		dendroPane.setLayout(new MigLayout("ins 0"));
		dendroPane.setBackground(GUIParams.BG_COLOR);
		
		setupViews();
	}
	
	/**
	 *  Returns the dendroPane so it can be displayed in TVFrame.
	 * @return JPanel dendroPane
	 */
	public JPanel makeDendroPanel() {
		
		return dendroPane;
	}

	// Layout
	/**
	 * This method should be called only during initial setup of the ModelView.
	 * It sets up the views and binds them all to Config nodes.
	 * 
	 */
	protected void setupViews() {
		
		// Create the Global view (JPanel to display)
		globalview = new GlobalView();

		// scrollbars, mostly used by maps
		globalXscrollbar = globalview.getXScroll();
		globalYscrollbar = globalview.getYScroll();

		// Set up the column name display
		arraynameview = new ArrayNameView(tvFrame.getDataModel().getArrayHeaderInfo());
//		arraynameview.setUrlExtractor(viewFrame.getArrayUrlExtractor());

		textview = new TextViewManager(tvFrame.getDataModel().getGeneHeaderInfo(),
				tvFrame.getUrlExtractor(), tvFrame.getDataModel());

		// Set up row dendrogram
		gtrview = new GTRView();
		gtrview.getHeaderSummary().setIncluded(new int[] { 0, 3 });

		// Set up column dendrogram
		atrview = new ATRView();
		atrview.getHeaderSummary().setIncluded(new int[] { 0, 3 });
		
		// Register Views
		registerView(globalview);
		registerView(atrview);
		registerView(arraynameview);
		registerView(textview);
		registerView(gtrview);

		// reset persistent popups
		settingsFrame = null;
		settingsPanel = null;
	}

	/**
	 * Manages the component layout in TreeViewFrame
	 */
	public void setupLayout() {

		// Clear panel
		dendroPane.removeAll();
		
		// Components for layout setup
		JPanel buttonPanel;
		JPanel crossPanel;
		JPanel finderPanel;
		JPanel textpanel;
		JPanel navPanel;
		JSplitPane gtrPane;
		JSplitPane atrPane;
		
		JPanel fillPanel1;
		JPanel fillPanel2;
		JPanel fillPanel3;
		JPanel fillPanel4;
		
		//Buttons
		scaleDefaultAll = GUIParams.setButtonLayout(null, "homeIcon");
		scaleDefaultAll.setToolTipText("Resets the zoomed view.");
		scaleDefaultAll.setForeground(GUIParams.LIGHTGRAY);

		scaleIncX = GUIParams.setButtonLayout(null, "zoomInIcon");
		scaleIncX.setToolTipText("Zooms in on X-axis.");

		scaleDecX = GUIParams.setButtonLayout(null, "zoomOutIcon");
		scaleDecX .setToolTipText("Zooms out of X-axis.");

		scaleIncY = GUIParams.setButtonLayout(null, "zoomInIcon");
		scaleIncY.setToolTipText("Zooms in on Y-axis.");

		scaleDecY = GUIParams.setButtonLayout(null, "zoomOutIcon");
		scaleDecY.setToolTipText("Zooms out of Y-axis.");
		
		zoomButton = GUIParams.setButtonLayout(null, "fullscreenIcon");
		zoomButton.setToolTipText("Zooms into the selected area.");

		// Panels
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new MigLayout());
		buttonPanel.setOpaque(false);

		crossPanel = new JPanel();
		crossPanel.setLayout(new MigLayout());
		crossPanel.setOpaque(false);

		fillPanel1 = new JPanel();
		fillPanel1.setOpaque(false);

		fillPanel2 = new JPanel();
		fillPanel2.setOpaque(false);
		
		fillPanel3 = new JPanel();
		fillPanel3.setOpaque(false);
		
		fillPanel4 = new JPanel();
		fillPanel4.setOpaque(false);
		
		navPanel = new JPanel();
		navPanel.setLayout(new MigLayout());
		navPanel.setOpaque(false);
		navPanel.setBorder(null);

		textpanel = new JPanel();
		textpanel.setLayout(new MigLayout("ins 0"));
		textpanel.setOpaque(false);
		
		finderPanel = new JPanel();
		finderPanel.setLayout(new MigLayout());
		finderPanel.setOpaque(false);
		
		gtrPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gtrview,
				textpanel);
		gtrPane.setResizeWeight(0.5);
		gtrPane.setOpaque(false);
		gtrPane.setBorder(null);
		
		if(gtrview.isEnabled()) {
			gtr_div_size = 3;
		}
		gtrPane.setDividerSize(gtr_div_size);

		atrPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, atrview,
				arraynameview);
		atrPane.setResizeWeight(0.5);
		atrPane.setOpaque(false);
		atrPane.setBorder(null);
		
		if(atrview.isEnabled()) {
			atr_div_size = 3;
		}
		atrPane.setDividerSize(atr_div_size);
		
		// Adding Components onto each other
		textpanel.add(textview.getComponent(), "push, grow");

		crossPanel.add(scaleIncY, "span, alignx 50%, wrap");
		crossPanel.add(scaleDecX);
		crossPanel.add(zoomButton);
		crossPanel.add(scaleIncX, "wrap");
		crossPanel.add(scaleDecY, "span, alignx 50%");

		buttonPanel.add(crossPanel, "pushx, alignx 50%, wrap");
		buttonPanel.add(scaleDefaultAll, "pushx, alignx 50%");
		
		finderPanel.add(getGeneFinderPanel(), "w 90%, h 30%, " +
				"alignx 50%, wrap");
		finderPanel.add(getArrayFinderPanel(), "w 90%, h 30%, " +
				"alignx 50%");

		navPanel.add(buttonPanel, "h 20%, w 90%, alignx 50%, wrap");
		navPanel.add(finderPanel, "pushy, h 20%, w 70%, alignx 50%, " +
				"aligny 10%");
		
		dendroPane.add(fillPanel4, "w 18.5%::, h 20%");
		dendroPane.add(atrPane, "w 72%, h 20%");
		dendroPane.add(fillPanel1, "span 2, w ::19.5%, h 20%, wrap");
		dendroPane.add(gtrPane, "w 18.5%::, h 75%");
		dendroPane.add(globalview, "w 72%, h 75%");
		dendroPane.add(globalYscrollbar, "w 1%, h 75%");
		dendroPane.add(navPanel, "w ::18.5%, h 75%, wrap");
		dendroPane.add(fillPanel2, "w 18.5%::, h 5%");
		dendroPane.add(globalXscrollbar, "pushy, aligny 0%, w 72%, h 1%");
		dendroPane.add(fillPanel3, "span 2, w ::19.5%, h 5%");
	}
	
	// Add Button Listeners
	/**
	 * Adds an ActionListener to the scale buttons in DendroView.
	 * @param l
	 */
	public void addScaleListener(ActionListener l) {
		
		scaleIncX.addActionListener(l);
		scaleDecX.addActionListener(l);
		scaleIncY.addActionListener(l);
		scaleDecY.addActionListener(l);
		scaleDefaultAll.addActionListener(l);
	}
	
	/**
	 * Adds an ActionListener to the Zoom button in DendroView.
	 * @param l
	 */
	public void addZoomListener(ActionListener l) {
		
		zoomButton.addActionListener(l);
	}
	
	/**
	 * Adds a component listener to the main panel of DendroView.
	 * @param l
	 */
	public void addCompListener(ComponentListener l) {
		
		getDendroPane().addComponentListener(l);
	}

	// Methods
	/**
	 * Redoing all the layout if parameters changed.
	 */
	public void refresh() {
		
		dendroPane.removeAll();
		setupLayout();
		
		dendroPane.revalidate();
		dendroPane.repaint();
	}
	
	/**
	 * Zooms in on the globalview.
	 * Make a separate globalview controller later.
	 */
	public void zoomSelection() {
		
		globalview.zoomSelection();
		globalview.centerSelection();
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

	// Menus
	@Override
	public void populateExportMenu(final TreeviewMenuBarI menu) {

		menu.addMenuItem("Export to Postscript...");
//		, new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent actionEvent) {
//
//				MapContainer initXmap, initYmap;
//
//				// if ((getArraySelection().getNSelectedIndexes() != 0) ||
//				// (getGeneSelection().getNSelectedIndexes() != 0)) {
//				// initXmap = getZoomXmap();
//				// initYmap = getZoomYmap();
//				//
//				// } else {
//				initXmap = getGlobalXmap();
//				initYmap = getGlobalYmap();
//				// }
//
//				final PostscriptExportPanel psePanel = setupPostscriptExport(
//						initXmap, initYmap);
//
//				final JDialog popup = new CancelableSettingsDialog(viewFrame,
//						"Export to Postscript", psePanel);
//				popup.pack();
//				popup.setVisible(true);
//			}
//		});
		menu.setAccelerator(KeyEvent.VK_X);
		menu.setMnemonic(KeyEvent.VK_X);

		menu.addMenuItem("Export to Image...");
//		, new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent actionEvent) {
//
//				MapContainer initXmap, initYmap;
//				// if ((getArraySelection().getNSelectedIndexes() != 0) ||
//				// (getGeneSelection().getNSelectedIndexes() != 0)) {
//				// initXmap = getZoomXmap();
//				// initYmap = getZoomYmap();
//				//
//				// } else {
//				initXmap = getGlobalXmap();
//				initYmap = getGlobalYmap();
//				// }
//
//				final BitmapExportPanel bitmapPanel = setupBitmapExport(
//						initXmap, initYmap);
//
//				final JDialog popup = new CancelableSettingsDialog(viewFrame,
//						"Export to Image", bitmapPanel);
//				popup.pack();
//				popup.setVisible(true);
//			}
//		});
		menu.setMnemonic(KeyEvent.VK_I);

		menu.addMenuItem("Export ColorBar to Postscript...");
//		, new ActionListener() {
//
//					@Override
//					public void actionPerformed(final ActionEvent actionEvent) {
//
//						final PostscriptColorBarExportPanel gcbPanel = 
//								new PostscriptColorBarExportPanel(
//								((DoubleArrayDrawer) arrayDrawer)
//										.getColorExtractor());
//
//						gcbPanel.setSourceSet(getDataModel().getFileSet());
//
//						final JDialog popup = new CancelableSettingsDialog(
//								viewFrame, "Export ColorBar to Postscript",
//								gcbPanel);
//						popup.pack();
//						popup.setVisible(true);
//					}
//				});
		menu.setMnemonic(KeyEvent.VK_B);

		menu.addMenuItem("Export ColorBar to Image...");
//		, new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent actionEvent) {
//
//				final BitmapColorBarExportPanel gcbPanel = 
//						new BitmapColorBarExportPanel(
//						((DoubleArrayDrawer) arrayDrawer).getColorExtractor());
//
//				gcbPanel.setSourceSet(getDataModel().getFileSet());
//
//				final JDialog popup = new CancelableSettingsDialog(viewFrame,
//						"Export ColorBar to Image", gcbPanel);
//				popup.pack();
//				popup.setVisible(true);
//			}
//		});
		menu.setMnemonic(KeyEvent.VK_M);

		menu.addSeparator();
		addSimpleExportOptions(menu);
	}

	private void addSimpleExportOptions(final TreeviewMenuBarI menu) {

		menu.addMenuItem("Save Tree Image");
//		, new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent actionEvent) {
//
//				MapContainer initXmap, initYmap;
//				initXmap = getGlobalXmap();
//				initYmap = getGlobalYmap();
//
//				final BitmapExportPanel bitmapPanel = new BitmapExportPanel(
//						arraynameview.getHeaderInfo(), getDataModel()
//								.getGeneHeaderInfo(), getGeneSelection(),
//						getArraySelection(), invertedTreeDrawer,
//						leftTreeDrawer, arrayDrawer, initXmap, initYmap);
//
//				bitmapPanel.setGeneFont(textview.getFont());
//				bitmapPanel.setArrayFont(arraynameview.getFont());
//				bitmapPanel.setSourceSet(getDataModel().getFileSet());
//				bitmapPanel.setDrawSelected(false);
//				bitmapPanel.includeData(false);
//				bitmapPanel.includeAtr(false);
//				bitmapPanel.deselectHeaders();
//
//				final JDialog popup = new CancelableSettingsDialog(viewFrame,
//						"Export to Image", bitmapPanel);
//				popup.pack();
//				popup.setVisible(true);
//			}
//		});
		menu.setMnemonic(KeyEvent.VK_T);

		menu.addMenuItem("Save Thumbnail Image");
//		, new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent actionEvent) {
//
//				MapContainer initXmap, initYmap;
//				initXmap = getGlobalXmap();
//				initYmap = getGlobalYmap();
//
//				final BitmapExportPanel bitmapPanel = new BitmapExportPanel(
//						arraynameview.getHeaderInfo(), getDataModel()
//								.getGeneHeaderInfo(), getGeneSelection(),
//						getArraySelection(), invertedTreeDrawer,
//						leftTreeDrawer, arrayDrawer, initXmap, initYmap);
//
//				bitmapPanel.setSourceSet(getDataModel().getFileSet());
//				bitmapPanel.setGeneFont(textview.getFont());
//				bitmapPanel.setArrayFont(arraynameview.getFont());
//				bitmapPanel.setDrawSelected(false);
//				bitmapPanel.includeGtr(false);
//				bitmapPanel.includeAtr(false);
//				bitmapPanel.deselectHeaders();
//
//				final JDialog popup = new CancelableSettingsDialog(viewFrame,
//						"Export To Image", bitmapPanel);
//				popup.pack();
//				popup.setVisible(true);
//			}
//		});
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
	@Override
	public void populateAnalysisMenu(final TreeviewMenuBarI menu) {

//		menu.addMenuItem("Flip Array Tree Node", new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent ae) {
//
//				if (getGtrview().hasFocus()) {
//
//					flipSelectedGTRNode();
//				} else {
//
//					flipSelectedATRNode();
//				}
//			}
//		});
//		menu.setAccelerator(KeyEvent.VK_L);
//		menu.setMnemonic(KeyEvent.VK_A);
//
//		menu.addMenuItem("Flip Gene Tree Node", new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent ae) {
//
//				flipSelectedGTRNode();
//			}
//		});
//		menu.setMnemonic(KeyEvent.VK_G);
//
//		menu.addMenuItem("Align to Tree...", new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent ae) {
//
//				try {
//
//					final FileSet fileSet = offerATRFileSelection();
//					final AtrTVModel atrModel = makeAtrModel(fileSet);
//
//					alignAtrToModel(atrModel);
//				} catch (final LoadException e) {
//
//					if ((e.getType() != LoadException.INTPARSE)
//							&& (e.getType() != LoadException.NOFILE)) {
//						LogBuffer.println("Could not open file: "
//								+ e.getMessage());
//						e.printStackTrace();
//					}
//				}
//			}
//		});
//		menu.setAccelerator(KeyEvent.VK_A);
//		menu.setMnemonic(KeyEvent.VK_G);
//
//		menu.addMenuItem("Compare to...", new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent ae) {
//
//				try {
//
//					final FileSet fileSet = offerATRFileSelection();
//					final TVModel tvModel = makeCdtModel(fileSet);
//					compareToModel(tvModel);
//				} catch (final LoadException e) {
//
//					if ((e.getType() != LoadException.INTPARSE)
//							&& (e.getType() != LoadException.NOFILE)) {
//						LogBuffer.println("Could not open file: "
//								+ e.getMessage());
//						e.printStackTrace();
//					}
//				}
//			}
//		});
//		menu.setAccelerator(KeyEvent.VK_C);
//		menu.setMnemonic(KeyEvent.VK_C);
//
//		menu.addMenuItem("Remove comparison", new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent ae) {
//
//				getDataModel().removeAppended();
//				globalXmap.setIndexRange(0, getDataModel().getDataMatrix()
//						.getNumCol() - 1);
//				globalXmap.notifyObservers();
//
//				((Observable) getDataModel()).notifyObservers();
//			}
//		});
//		menu.setAccelerator(KeyEvent.VK_R);
//		menu.setMnemonic(KeyEvent.VK_R);

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
		
		annotationsMenuItem = (JMenuItem) menu.addMenuItem(
				"Row and Column Labels", 0);
		menu.setMnemonic(KeyEvent.VK_R);
		tvFrame.addToMenuList(annotationsMenuItem);
		
		colorMenuItem = (JMenuItem) menu.addMenuItem("Color Settings", 1); 
		menu.setMnemonic(KeyEvent.VK_C);
		tvFrame.addToMenuList(colorMenuItem);
	}

//	/**
//	 * @param initXmap
//	 * @param initYmap
//	 * @return
//	 */
//	private PostscriptExportPanel setupPostscriptExport(
//			final MapContainer initXmap, final MapContainer initYmap) {
//
//		final PostscriptExportPanel psePanel = new PostscriptExportPanel(
//				arraynameview.getHeaderInfo(), getDataModel()
//						.getGeneHeaderInfo(), getGeneSelection(),
//				getArraySelection(), invertedTreeDrawer, leftTreeDrawer,
//				arrayDrawer, initXmap, initYmap);
//
//		psePanel.setSourceSet(getDataModel().getFileSet());
//		psePanel.setGeneFont(textview.getFont());
//		psePanel.setArrayFont(arraynameview.getFont());
//		psePanel.setIncludedArrayHeaders(arraynameview.getHeaderSummary()
//				.getIncluded());
//		psePanel.setIncludedGeneHeaders(textview.getHeaderSummary()
//				.getIncluded());
//
//		return psePanel;
//	}
//
//	private BitmapExportPanel setupBitmapExport(final MapContainer initXmap,
//			final MapContainer initYmap) {
//
//		final BitmapExportPanel bitmapPanel = new BitmapExportPanel(
//				arraynameview.getHeaderInfo(), getDataModel()
//						.getGeneHeaderInfo(), getGeneSelection(),
//				getArraySelection(), invertedTreeDrawer, leftTreeDrawer,
//				arrayDrawer, initXmap, initYmap);
//
//		bitmapPanel.setSourceSet(getDataModel().getFileSet());
//		bitmapPanel.setGeneFont(textview.getFont());
//		bitmapPanel.setArrayFont(arraynameview.getFont());
//		bitmapPanel.setIncludedArrayHeaders(arraynameview.getHeaderSummary()
//				.getIncluded());
//		bitmapPanel.setIncludedGeneHeaders(textview.getHeaderSummary()
//				.getIncluded());
//
//		return bitmapPanel;
//	}

//	@Override
//	public void export(final MainProgramArgs mainArgs) throws ExportException {
//
//		final DendroviewArgs args = new DendroviewArgs(mainArgs.remainingArgs());
//
//		if (args.getFilePath() == null) {
//			System.err.println("Error, must specify an output file\n");
//			args.printUsage();
//
//			return;
//		}
//
//		final ExportPanel exporter;
//
//		if ("ps".equalsIgnoreCase(args.getExportType())) {
//			exporter = setupPostscriptExport(getGlobalXmap(), getGlobalYmap());
//
//		} else if ("png".equalsIgnoreCase(args.getExportType())
//				|| "gif".equalsIgnoreCase(args.getExportType())) {
//			exporter = setupBitmapExport(getGlobalXmap(), getGlobalYmap());
//
//		} else {
//			System.err.println("Error, unrecognized output format "
//					+ args.getExportType() + " \n");
//
//			args.printUsage();
//			exporter = null;
//		}
//
//		if (exporter != null) {
//			exporter.setFilePath(args.getFilePath());
//			exporter.setIncludedArrayHeaders(args.getArrayHeaders());
//			exporter.setIncludedGeneHeaders(args.getGeneHeaders());
//
//			if (args.getXScale() != null) {
//				exporter.setXscale(args.getXScale());
//			}
//
//			if (args.getYScale() != null) {
//				exporter.setYscale(args.getYScale());
//			}
//
//			if (args.getContrast() != null) {
//				colorExtractor.setContrast(args.getContrast());
//			}
//
//			if (args.getGtrWidth() != null) {
//				exporter.setExplicitGtrWidth(args.getGtrWidth());
//			}
//
//			if (args.getAtrHeight() != null) {
//				exporter.setExplicitAtrHeight(args.getAtrHeight());
//			}
//
//			if (args.getLogcenter() != null) {
//				colorExtractor.setLogCenter(args.getLogcenter());
//				colorExtractor.setLogBase(2.0);
//				colorExtractor.setLogTransform(true);
//			}
//
//			exporter.setArrayAnnoInside(args.getArrayAnnoInside());
//			exporter.save();
//		}
//	}

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

		HeaderFinderBox geneFinderBox = new GeneFinderBox(tvFrame, this,
					tvFrame.getDataModel().getGeneHeaderInfo(),
					tvFrame.getGeneSelection());

		JPanel contentPanel = geneFinderBox.getContentPanel();
		
		return contentPanel;
	}

	/**
	 * Getter for geneFinderPanel
	 * 
	 * @return HeaderFinderPanel arrayFinderPanel
	 */
	public JPanel getArrayFinderPanel() {

		HeaderFinderBox arrayFinderBox = new ArrayFinderBox(tvFrame, this,
				tvFrame.getDataModel().getArrayHeaderInfo(),
				tvFrame.getArraySelection());
		
		JPanel contentPanel = arrayFinderBox.getContentPanel();
		
		return contentPanel;
	}

	@Override
	public String getName() {

		return name;
	}
	
	public void setName(String name) {

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
	
	public JScrollBar getYScroll() {
		
		return globalYscrollbar;
	}
	
	public GlobalView getGlobalView() {
		
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

	public TextViewManager getTextview() {

		return textview;
	}

	/**
	 * Getter for viewFrame
	 */
	public ViewFrame getViewFrame() {

		return tvFrame;
	}
}
