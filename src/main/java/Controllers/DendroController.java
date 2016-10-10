package Controllers;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import Utilities.Helper;
import edu.stanford.genetics.treeview.CdtFilter;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.CopyType;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LabelInfo;
import edu.stanford.genetics.treeview.LabelSummary;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.MatrixViewController;
import edu.stanford.genetics.treeview.ReorderedTreeSelection;
import edu.stanford.genetics.treeview.TreeDrawerNode;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.model.AtrTVModel;
import edu.stanford.genetics.treeview.model.ReorderedDataModel;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.plugin.dendroview.ArrayDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.AtrAligner;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroException;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;
import edu.stanford.genetics.treeview.plugin.dendroview.DendrogramFactory;
import edu.stanford.genetics.treeview.plugin.dendroview.IntegerMap;
import edu.stanford.genetics.treeview.plugin.dendroview.LabelContextMenu;
import edu.stanford.genetics.treeview.plugin.dendroview.LabelContextMenuController;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;
import edu.stanford.genetics.treeview.plugin.dendroview.MatrixView;
import edu.stanford.genetics.treeview.plugin.dendroview.TreeColorer;
import edu.stanford.genetics.treeview.plugin.dendroview.TreePainter;



/* 
 * NOTES: 
 * DendroController needs to listen to selection objects in order to
 * update button statuses (focus, enabled, etc.)
 * It needs to know nothing else about the data and user-matrix interactions.
 * In the future this will be handled by InteractiveMatrixViewController 
 * in order to separate concerns. */
/**
 * Controller class handling UI input and calculations related to the main
 * TreeView interface (DendroView).
 *
 */
public class DendroController implements ConfigNodePersistent, Observer, 
	Controller {

	private DendroView dendroView;
	private final TreeViewFrame tvFrame;
	private final TVController tvController;
	private MatrixViewController mvController;
	private DataModel tvModel;

	protected Preferences configNode;

	private int[] colIndex = null;
	private int[] rowIndex = null;

	// Drawers
	protected ArrayDrawer arrayDrawer;
	protected TreePainter invertedTreeDrawer;
	protected TreePainter leftTreeDrawer;

	// MapContainers
	protected final MapContainer interactiveXmap;
	protected final MapContainer interactiveYmap;

	protected final MapContainer globalXmap;
	protected final MapContainer globalYmap;

	// Selections
	private TreeSelectionI rowSelection = null;
	private TreeSelectionI colSelection = null;

	public DendroController(final TreeViewFrame tvFrame,
			final TVController tvController) {

		this.tvFrame = tvFrame;
		this.tvController = tvController;

		this.interactiveXmap = new MapContainer(IntegerMap.FIXED, "GlobalXMap");
		this.interactiveYmap = new MapContainer(IntegerMap.FIXED, "GlobalYMap");

		this.globalXmap = new MapContainer(IntegerMap.FIXED, "OverviewXMap");
		this.globalYmap = new MapContainer(IntegerMap.FIXED, "OverviewYMap");
	}
	
	/**
	 * Checks whether there is a configuration node for the current model and
	 * DendroView. If not it creates one.
	 * @param The parent Preferences node for the node to be created in this
	 * class.
	 */
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode == null) {
			LogBuffer.println("parentNode in " + this.getClass().getName() 
					+ " was null.");
			return;
		}
		
		if (tvModel.getDocumentConfigRoot() != null) {
			this.configNode = ((TVModel) tvModel).getDocumentConfig();

		} else {
			this.configNode = Preferences.userRoot().node("DendroView");
		}
	}
	
	@Override
	public void requestStoredState() {
		
		importStateFrom(configNode);
	}

	@Override
	public void storeState() {
		return; // nothing to store yet
	}
	
	@Override
	public void importStateFrom(Preferences oldNode) {
		return; // no stored state, so nothing to import yet
	}

	/**
	 * Initiates the controller for a new data matrix by setting up all
	 * necessary components.
	 * TODO - Layout reset necessary?
	 * @param dendroView Instance of the main UI class for viewing matrices.
	 * @param tvModel Instance of the underling data model.
	 */
	public void setNewMatrix(final DendroView dendroView,
			final DataModel tvModel) {

		this.dendroView = dendroView;
		this.tvModel = tvModel;
		this.mvController = new MatrixViewController(
				dendroView.getInteractiveMatrixView(), 
				dendroView.getGlobalMatrixView(), tvModel);
		
		resetComponentDefaults();
		
		mvController.setup();
		
		setComponentPreferences();
		updateLabelInfo();
		bindComponentFunctions();
		
		dendroView.setupSearch(tvModel.getRowLabelInfo(),
						tvModel.getColLabelInfo(), interactiveXmap,
						interactiveYmap);
		
		dendroView.setupLayout();
		setModelAndTicker(dendroView,tvModel);
		setObservables();

		/**
		 * make sure pixel colors are calculated after new model was loaded. 
		 */
		mvController.updateMatrixPixels();
		mvController.resetMatrixViews();
		mvController.setDataTicker(dendroView.getDataTicker());

		addKeyBindings();
		addListeners();
	}

	/** sets data model and ticker in Label View and TreeView classes
	 * @param dendroView
	 * @param tvModel
	 */
	private static void setModelAndTicker(final DendroView dendroView,
								final DataModel tvModel) {
		dendroView.getRowLabelView().setDataTicker(dendroView.getDataTicker());
		dendroView.getColLabelView().setDataTicker(dendroView.getDataTicker());
		dendroView.getRowLabelView().setDataModel(tvModel);
		dendroView.getColLabelView().setDataModel(tvModel);
		dendroView.getRowTreeView().setDataTicker(dendroView.getDataTicker());
		dendroView.getColumnTreeView().setDataTicker(dendroView.getDataTicker());
		dendroView.getRowTreeView().setDataModel(tvModel);
		dendroView.getColumnTreeView().setDataModel(tvModel);
	}
	
	/**
	 * Makes all relevant components reset to a default state in order to
	 * create a clean slate for loading a new model.
	 */
	private void resetComponentDefaults() {
		
		LogBuffer.println("Resetting MapContainers and DendroView components.");
		
		resetMapContainerDefaults();
		dendroView.resetModelViewDefaults();
	}
	
	/**
	 * Delegates a reset for all used MapContainers.
	 */
	private void resetMapContainerDefaults() {
		
		interactiveXmap.resetDefaultState();
		interactiveYmap.resetDefaultState();
		
		globalXmap.resetDefaultState();
		globalYmap.resetDefaultState();
	}

	@Override
	public void addKeyBindings() {

		final InputMap input_map = dendroView.getInputMap();
		final ActionMap action_map = dendroView.getActionMap();

		/* Gets the system's modifier key (Ctrl or Cmd) */
		final int modifier = Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask();

		/* Toggle the trees */
		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, modifier),
				"toggleTrees");
		action_map.put("toggleTrees", new TreeToggleAction());

		/* Select/ deselect */
		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, modifier),
				"deselect");
		action_map.put("deselect", new DeselectAction());

		/* Open search dialog */
		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, modifier),
				"searchLabels");
		action_map.put("searchLabels", new SearchLabelAction());
	}

	/**
	 * Adds DendroController as an observer to various classes so that the GUI
	 * can update as a response to updates in these classes.
	 */
	private void setObservables() {

		/* Label views */
		dendroView.getRowLabelView().getLabelSummary().addObserver(this);
		dendroView.getColLabelView().getLabelSummary().addObserver(this);

		/* MapContainers */
		interactiveXmap.addObserver(this);
		interactiveYmap.addObserver(this);

		globalXmap.addObserver(this);
		globalYmap.addObserver(this);

		/* Selections */
		rowSelection.addObserver(this);
		colSelection.addObserver(this);
	}

	/**
	 * Adds listeners to DendroView's UI components.
	 */
	@Override
	public void addListeners() {

		dendroView.addScaleListeners(new ScaleListener());
		dendroView.addZoomListener(new ZoomSelectionListener());
		dendroView.addDividerListener(new DividerListener());
		dendroView.addSplitPaneListener(new SplitPaneListener());
		dendroView.addDeselectClickListener(new PanelClickDeselector());
		addDividerHoverListeners();
		addDividerMouseWheelListeners();

		mvController.addListeners();
	}

	/* -------------- Listeners --------------------- */
	/**
	 * This adds mouse listeners to the split pane divider so that when the
	 * cursor is over a divider, we can detect it and keep the whizzing labels
	 * visible.
	 * @author rleach
	 */
	private void addDividerHoverListeners() {

		BasicSplitPaneUI rbspUI =
			(BasicSplitPaneUI) dendroView.getRowSplitPane().getUI();
		BasicSplitPaneDivider rbspDivider = rbspUI.getDivider();
		rbspDivider.addMouseListener(new DividerAdapter(interactiveYmap));

		BasicSplitPaneUI cbspUI =
			(BasicSplitPaneUI) dendroView.getColSplitPane().getUI();
		BasicSplitPaneDivider cbspDivider = cbspUI.getDivider();
		cbspDivider.addMouseListener(new DividerAdapter(interactiveXmap));
	}

	/**
	 * This adds mouse listeners to the split pane divider so that when the
	 * cursor is over a divider, we can detect the mouse wheel actions and
	 * scroll the matrix.
	 * @author rleach
	 */
	private void addDividerMouseWheelListeners() {

		boolean isColumnPane = false;
		BasicSplitPaneUI rbspUI =
			(BasicSplitPaneUI) dendroView.getRowSplitPane().getUI();
		BasicSplitPaneDivider rbspDivider = rbspUI.getDivider();
		rbspDivider.addMouseWheelListener(
			new DividerMouseWheelListener(interactiveYmap,isColumnPane));

		isColumnPane = true;
		BasicSplitPaneUI cbspUI =
			(BasicSplitPaneUI) dendroView.getColSplitPane().getUI();
		BasicSplitPaneDivider cbspDivider = cbspUI.getDivider();
		cbspDivider.addMouseWheelListener(
			new DividerMouseWheelListener(interactiveXmap,isColumnPane));
	}

	/**
	 * This mouse adapter extension allows one to control the visibility of the
	 * labels by setting values in mapcontainer objects.
	 * @author rleach
	 */
	private class DividerAdapter extends MouseAdapter {
		private MapContainer map;
		public DividerAdapter(MapContainer map) {
			super();
			this.map = map;
		}
		public void mouseEntered(MouseEvent e) {
			if(map.wasLastTreeModeGlobal() && map.shouldKeepTreeGlobal()) {
				map.setKeepTreeGlobal(true);
			}
			map.setOverDivider(true);
		}
		public void mouseExited(MouseEvent e) {
			map.setOverDivider(false);
		}
		public void mousePressed(MouseEvent e) {
			map.setDraggingDivider(true);
		}
		public void mouseReleased(MouseEvent e) {
			map.setDraggingDivider(false);
		}
	}

	/**
	 * This mouse wheel listener allows one to control the matrix scroll using
	 * the mouse while hovered over the divider.
	 * @author rleach
	 */
	private class DividerMouseWheelListener implements MouseWheelListener {
		MapContainer map;
		final boolean isColPane;

		public DividerMouseWheelListener(MapContainer map,
			final boolean isColPane) {
			super();
			this.map = map;
			this.isColPane = isColPane;
		}

		public void mouseWheelMoved(final MouseWheelEvent e) {

			final int notches = e.getWheelRotation();
			int shift = (notches < 0) ? -6 : 6;

			// On macs' magic mouse, horizontal scroll comes in as if the shift was
			// down
			if(e.isShiftDown() == isColPane) {
				//Value of label length scrollbar
				map.scrollBy(shift);
				updatePrimaryHoverIndexDuringScrollWheel();
			}
		}

		public void updatePrimaryHoverIndexDuringScrollWheel() {
			if(map.getHoverPixel() == -1) {
				unsetPrimaryHoverIndex();
			} else {
				setPrimaryHoverIndex(map.getIndex(map.getHoverPixel()));
			}
		}

		public void setPrimaryHoverIndex(final int i) {
			map.setHoverIndex(i);
		}

		public void unsetPrimaryHoverIndex() {
			map.unsetHoverIndex();
		}
	}

	/**
	 * When mouse click happens on dendroPane in DendroView, everything will be
	 * deselected.
	 *
	 */
	private class PanelClickDeselector extends MouseAdapter {

		/* 
		 * mousePressed instead of mouseClicked because mousePressing is
		 * apparently what makes a Window active. mouseClicked therefore
		 * always returns true for isActive() and deselection happens even
		 * though the window might be in the background. 
		 */
		@Override
		public void mousePressed(MouseEvent e) {

			if(tvFrame.getAppFrame().isActive()) {
				deselectAll();
			}
		}
	}

	/* >>>>>>> Keyboard Shortcut Actions <<<<<<<<< */
	/**
	 * This AbstractAction is used to toggle both dendrograms when the user uses
	 * the associated keyboard shortcut. It stores the location of the
	 * JSplitPane dividers when the user wants to hide the trees and retrieves
	 * this information when the user decides to show them again.
	 */
	private class TreeToggleAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			if (dendroView.treesEnabled()) {
				toggleTrees();
			}
		}
	}

	/**
	 * Toggles trees. If visible, the divider position will be saved and set to
	 * 0.0. If invisible, the divider location will be reset to the last saved
	 * value or the default value.
	 */
	public void toggleTrees() {

		double atr_loc = dendroView.getDivLoc(dendroView.getColumnTreeView());
		double gtr_loc = dendroView.getDivLoc(dendroView.getRowTreeView());

		if (atr_loc > 0.0 && gtr_loc > 0.0) {

			/* First save current setup */
			configNode.putDouble("atr_Loc", atr_loc);
			configNode.putDouble("gtr_Loc", gtr_loc);

			/* Shrink tree panel to 0 to make it invisible */
			atr_loc = 0.0;
			gtr_loc = 0.0;

		} else {
			/* Only update trees which are currently at 0.0 */
			if (Helper.nearlyEqual(0.0, atr_loc)) {
				atr_loc = configNode.getDouble("atr_Loc", 0.5);
			}

			if (Helper.nearlyEqual(0.0, gtr_loc)) {
				gtr_loc = configNode.getDouble("gtr_Loc", 0.5);
			}
		}

		dendroView.setTreeVisibility(atr_loc, gtr_loc);
	}

	/**
	 * Puts the row search box into focus when called.
	 */
	private class SearchLabelAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			dendroView.setRowFinderBoxFocused();
		}
	}

	/**
	 * When this PropertyChangeListener is triggered, the JMenuItem for
	 * showing/ hiding trees will be updated accordingly.
	 */
	private class DividerListener implements PropertyChangeListener {

		@Override
		public void propertyChange(final PropertyChangeEvent evt) {

			dendroView.updateTreeMenuBtn();
		}
	}

	/** 
	 * Causes all selections on both axes to be set to zero when called. 
	 */
	private class DeselectAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			deselectAll();
		}
	}

	/* >>>>>>> Component Listeners <<<<<<<<< */
	/**
	 * Listener for the setScale-buttons in DendroView. Changes the scale in
	 * xMap and yMap MapContainers, allowing the user to zoom in or out of each
	 * individual axis in GlobalView.
	 * In this class (and not the matrixController) because DendroViews 
	 * buttons are being used.
	 * 
	 * TODO Keep in this controller. 
	 * Split to single button listeners (one per button). Create methods
	 * in MCController to be called by the single button listeners.
	 */
	class ScaleListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			/*
			 * TODO Adapt zoom methods in MapContainer to differentiate between
			 * adding/removing tiles on the opposite axis sides. Can reduce this
			 * button madness to something more compact later.
			 */
			if (e.getSource() == dendroView.getXRightPlusButton()) {
				// Adds column on right side
				interactiveXmap.zoomInEnd();
				// Doing this here because many times zoomIn is called
				// successively for each dimension
				dendroView.getInteractiveMatrixView().updateAspectRatio();

			} else if (e.getSource() == dendroView.getXYMinusButton()) {
				if ((e.getModifiers() & InputEvent.META_MASK) != 0) {
					mvController.resetMatrixViews();
					dendroView.getInteractiveMatrixView().setAspectRatio(
							interactiveXmap.getTotalTileNum(),
							interactiveYmap.getTotalTileNum());
					
				} else if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
					interactiveXmap.zoomOutCenter(MapContainer.ZOOM_FAST);
					interactiveYmap.zoomOutCenter(MapContainer.ZOOM_FAST);
					
				} else if ((e.getModifiers() & InputEvent.ALT_MASK) != 0) {
					interactiveXmap.zoomOutCenter(MapContainer.ZOOM_SLOW);
					interactiveYmap.zoomOutCenter(MapContainer.ZOOM_SLOW);
					
				} else {
					interactiveXmap.zoomOutCenter(MapContainer.ZOOM_DEFAULT);
					interactiveYmap.zoomOutCenter(MapContainer.ZOOM_DEFAULT);
				}

			} else if (e.getSource() == dendroView.getXYPlusButton()) {
				if ((e.getModifiers() & InputEvent.META_MASK) != 0) {
					interactiveXmap.zoomInCenter(MapContainer.ZOOM_SLAM);
					interactiveYmap.zoomInCenter(MapContainer.ZOOM_SLAM);
					
				} else if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
					interactiveXmap.zoomInCenter(MapContainer.ZOOM_FAST);
					interactiveYmap.zoomInCenter(MapContainer.ZOOM_FAST);
					
				} else if ((e.getModifiers() & InputEvent.ALT_MASK) != 0) {
					interactiveXmap.zoomInCenter(MapContainer.ZOOM_SLOW);
					interactiveYmap.zoomInCenter(MapContainer.ZOOM_SLOW);
					
				} else {
					interactiveXmap.zoomInCenter(MapContainer.ZOOM_DEFAULT);
					interactiveYmap.zoomInCenter(MapContainer.ZOOM_DEFAULT);
				}

			} else if (e.getSource() == dendroView.getXLeftPlusButton()) {
				// Add a column on the left side
				interactiveXmap.zoomInBegin();
				// Doing this here because many times zoomIn is called
				// successively for each dimension
				dendroView.getInteractiveMatrixView().updateAspectRatio();
			} else if (e.getSource() == dendroView.getXMinusRightButton()) {
				// Removes column on right side
				getInteractiveXMap().zoomOutEnd();
				// Doing this here because many times zoomIn is called
				// successively for each dimension
				dendroView.getInteractiveMatrixView().updateAspectRatio();

			} else if (e.getSource() == dendroView.getXMinusLeftButton()) {
				// Remove column on left side
				interactiveXmap.zoomOutBegin();
				// Doing this here because many times zoomIn is called
				// successively for each dimension
				dendroView.getInteractiveMatrixView().updateAspectRatio();
			} else if (e.getSource() == dendroView.getYPlusBottomButton()) {
				// Adds a row to the bottom.
				getInteractiveYMap().zoomInEnd();
				// Doing this here because many times zoomIn is called
				// successively for each dimension
				dendroView.getInteractiveMatrixView().updateAspectRatio();
			} else if (e.getSource() == dendroView.getYPlusTopButton()) {
				// Add row to top here
				getInteractiveYMap().zoomInBegin();
				// Doing this here because many times zoomIn is called
				// successively for each dimension
				dendroView.getInteractiveMatrixView().updateAspectRatio();
			} else if (e.getSource() == dendroView.getYMinusBottomButton()) {
				// Removes row from bottom
				getInteractiveYMap().zoomOutEnd();
				// Doing this here because many times zoomIn is called
				// successively for each dimension
				dendroView.getInteractiveMatrixView().updateAspectRatio();

			} else if (e.getSource() == dendroView.getYMinusTopButton()) {
				// Remove row from top here
				getInteractiveYMap().zoomOutBegin();
				// Doing this here because many times zoomIn is called
				// successively for each dimension
				dendroView.getInteractiveMatrixView().updateAspectRatio();
			} else if (e.getSource() == dendroView.getHomeButton()) {

				if ((e.getModifiers() & InputEvent.META_MASK) != 0
						|| (e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
					mvController.resetMatrixViews();
					dendroView.getInteractiveMatrixView().setAspectRatio(
							interactiveXmap.getMaxIndex() + 1,
							interactiveYmap.getMaxIndex() + 1);
				} else if ((e.getModifiers() & InputEvent.ALT_MASK) != 0) {
					dendroView.getInteractiveMatrixView()
							.smoothIncrementalZoomOut();
				} else {
					dendroView.getInteractiveMatrixView()
							.smoothAnimatedZoomOut();
				}
			} else {
				LogBuffer.println("Got weird source for actionPerformed() "
						+ "in DendroController ScaleListener.");
			}

			mvController.notifyAllMapObservers();
		}
	}

	/**
	 * Defines what happens when component properties of the two JSplitPanes
	 * which contain labels and trees are changed by the system or the user.
	 *
	 */
	private class SplitPaneListener extends ComponentAdapter {

		@Override
		public void componentResized(final ComponentEvent e) {
			// no change on resize
		}

		@Override
		public void componentShown(final ComponentEvent e) {

			final double atr_loc = configNode.getDouble("atr_Loc", 0.5);
			final double gtr_loc = configNode.getDouble("gtr_Loc", 0.5);

			dendroView.setTreeVisibility(atr_loc, gtr_loc);
		}
	}

	/**
	 * Deselects all of both axes' current selections.
	 */
	public void deselectAll() {

		colSelection.deselectAllIndexes();
		rowSelection.deselectAllIndexes();

		colSelection.notifyObservers();
		rowSelection.notifyObservers();

		setAdaptiveButtonStatus();
	}

	/**
	 * TODO Should be in MatrixViewController
	 * Stores MapContainer settings (scale + scroll values) in Preferences
	 * nodes.
	 */
	public void saveSettings() {
    
		if(configNode == null) {
			return;
		}
		
		try {
			if (configNode.nodeExists("GlobalXMap") && interactiveXmap != null) {
				configNode.node("GlobalXMap").putInt("scale",
						interactiveXmap.getNumVisible());
				configNode.node("GlobalXMap").putInt("XScrollValue",
						dendroView.getMatrixXScroll().getValue());
			}

			if (configNode.nodeExists("GlobalYMap") && interactiveYmap != null) {
				configNode.node("GlobalYMap").putInt("scale",
						interactiveYmap.getNumVisible());
				configNode.node("GlobalYMap").putInt("YScrollValue",
						dendroView.getMatrixYScroll().getValue());
			}
		} catch (final BackingStoreException e) {
			LogBuffer.logException(e);
		}
	}

	/**
	 * The Zoom listener which allows the user to zoom into a selection.
	 */
	private class ZoomSelectionListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			mvController.zoomOnSelection(arg0.getModifiers());
		}
	}

	// TODO move to MVController
	protected void refocusViewPort() {

		interactiveXmap.adjustToScreenChange();
		interactiveYmap.adjustToScreenChange();

		globalXmap.setMinScale();
		globalYmap.setMinScale();

		saveSettings();
	}

	/**
	 * Getter for root
	 */
	public Preferences getConfigNode() {

		return configNode;
	}

	/**
	 * Sets arrayIndex and geneIndex if a K-Means clustered file has been
	 * loaded. These indexes are used to set gaps to visualize the distinct
	 * groups in a K-Means clustered file.
	 */
	public void setKMeansIndexes() {

		if (tvModel.getColLabelInfo().getIndex("GROUP") != -1) {
			final LabelInfo labelInfo = tvModel.getColLabelInfo();
			final int groupIndex = labelInfo.getIndex("GROUP");

			colIndex = getGroupVector(labelInfo, groupIndex);

		} else {
			colIndex = null;
		}

		if (tvModel.getRowLabelInfo().getIndex("GROUP") != -1) {
			System.err.println("got gene group prefix");
			final LabelInfo labelInfo = tvModel.getRowLabelInfo();
			final int groupIndex = labelInfo.getIndex("GROUP");
			rowIndex = getGroupVector(labelInfo, groupIndex);

		} else {
			rowIndex = null;
		}

		// ISSUE: Needs DataModel, not TVModel. Should dataModel be used
		// in this class rather than TVModel?
		if ((colIndex != null) || (rowIndex != null)) {
			tvModel = new ReorderedDataModel(tvModel, rowIndex, colIndex);
			LogBuffer.println("DataModel issue in DendroController.");
		}
	}

	/**
	 * Returns an array of indexes of K-Means groups.
	 *
	 * @param labelInfo
	 * @param groupIndex
	 * @return
	 */
	private static int[] getGroupVector(final LabelInfo labelInfo,
			final int groupIndex) {

		int ngroup = 0;
		String cur = labelInfo.getLabel(0, groupIndex);

		for (int i = 0; i < labelInfo.getNumLabels(); i++) {

			final String test = labelInfo.getLabel(i, groupIndex);
			if (!cur.equals(test)) {
				cur = test;
				ngroup++;
			}
		}

		final int[] groupVector = new int[ngroup + labelInfo.getNumLabels()];
		ngroup = 0;
		cur = labelInfo.getLabel(0, groupIndex);

		for (int i = 0; i < labelInfo.getNumLabels(); i++) {

			final String test = labelInfo.getLabel(i, groupIndex);
			if (!cur.equals(test)) {
				groupVector[i + ngroup] = -1;
				cur = test;
				ngroup++;
			}
			groupVector[i + ngroup] = i;
		}

		return groupVector;
	}

	/**
	 * Binds functionality to Swing components in DendroView.
	 */
	private void bindComponentFunctions() {

		setupSelectionHandlers();
		setupMapContainers();

		// TODO replace with IMVController method
		interactiveXmap.setScrollbar(dendroView.getMatrixXScroll());
		interactiveYmap.setScrollbar(dendroView.getMatrixYScroll());

		createTreePainters();
		bindTrees();
		addLabelViewContextMenu();
		defineMapContainerRanges();
	}
	
	/**
	 * Sets up the selection handlers which track details of selected indices
	 * in LabelView, the matrices, and the trees. 
	 */
	private void setupSelectionHandlers() {
		
		if (rowIndex != null) {
			setRowSelection(new ReorderedTreeSelection(
					tvFrame.getRowSelection(), rowIndex));

		} else {
			setRowSelection(tvFrame.getRowSelection());
		}

		if (colIndex != null) {
			setColumnSelection(new ReorderedTreeSelection(
					tvFrame.getColSelection(), colIndex));

		} else {
			setColumnSelection(tvFrame.getColSelection());
		}
	}
	
	/**
	 * Create tree painter objects and link them to the TRView objects for each
	 * axis.
	 */
	private void createTreePainters() {
		
		this.leftTreeDrawer = new TreePainter();
		dendroView.getRowTreeView().setTreeDrawer(leftTreeDrawer);

		this.invertedTreeDrawer = new TreePainter();
		dendroView.getColumnTreeView().setTreeDrawer(invertedTreeDrawer);
	}
	
	/**
	 * Adds a separate right-click context menu object to each LabelView.
	 */
	@SuppressWarnings("unused") // LabelContextMenuController don't need to be stored in a variable
	private void addLabelViewContextMenu() {
		
		// Set context menu for LabelViews
		LabelContextMenu rowLabelContext = new LabelContextMenu();
		LabelContextMenu colLabelContext = new LabelContextMenu();
		
		new LabelContextMenuController(rowLabelContext, tvController, true);
		new LabelContextMenuController(colLabelContext, tvController, false);
		
		dendroView.getRowLabelView().setComponentPopupMenu(rowLabelContext);
		dendroView.getColLabelView().setComponentPopupMenu(colLabelContext);
	}
	
	/**
	 * Let the MapContainers know their index range to work with. 
	 */
	private void defineMapContainerRanges() {
		
		interactiveXmap.setIndexRange(0,
				tvModel.getDataMatrix().getNumCol() - 1);
		interactiveYmap.setIndexRange(0,
				tvModel.getDataMatrix().getNumRow() - 1);

		globalXmap.setIndexRange(0, tvModel.getDataMatrix().getNumCol() - 1);
		globalYmap.setIndexRange(0, tvModel.getDataMatrix().getNumRow() - 1);
	}

	/**
	 * Connects all sub components with DendroView's configuration node, so that
	 * the hierarchical structure of Java's Preferences API can be followed.
	 */
	private void setComponentPreferences() {

		setConfigNode(tvFrame.getConfigNode());
		
		interactiveXmap.setConfigNode(configNode);
		interactiveYmap.setConfigNode(configNode);

		globalXmap.setConfigNode(configNode);
		globalYmap.setConfigNode(configNode);

		DendrogramFactory.getColorPresets().setConfigNode(configNode);
		mvController.setConfigNode(configNode);

		dendroView.getRowLabelView().setConfigNode(configNode);
		dendroView.getColLabelView().setConfigNode(configNode);
	}
	
	/**
	 * Requests all main components of the GUI to restore their states to
	 * saved preferences stored in their active Preferences node (configNode).
	 * This is especially useful after performing a reset to default state for
	 * those components to get a clean slate, for example when a new model is 
	 * loaded.
	 */
	public void restoreComponentStates() {
		
		LogBuffer.println("Restoring components states.");
		
		interactiveXmap.requestStoredState();
		interactiveYmap.requestStoredState();
		
		globalXmap.requestStoredState();
		globalYmap.requestStoredState();
		
		mvController.requestStoredState();
		
		dendroView.restoreLabelViewStates();
		
		DendrogramFactory.getColorPresets().requestStoredState();
	}

	/**
	 * Assigns the MapContainers to classes which need to know about them.
	 */
	private void setupMapContainers() {

		dendroView.getColumnTreeView().setMap(interactiveXmap);
		dendroView.getColLabelView().setMap(interactiveXmap);
		dendroView.getRowTreeView().setMap(interactiveYmap);
		dendroView.getRowLabelView().setMap(interactiveYmap);

		mvController.setInteractiveMapContainers(interactiveXmap, 
				interactiveYmap);
		mvController.setGlobalMapContainers(globalXmap, globalYmap);
	}

	/**
	 * Updates all LabelInfo instances for all the label views.
	 */
	private void updateLabelInfo() {

		dendroView.getRowLabelView().setLabelInfo(tvModel.getRowLabelInfo());
		dendroView.getColLabelView().setLabelInfo(tvModel.getColLabelInfo());
	}

	/**
	 * Displays a data set alongside the primary one for comparison.
	 *
	 * @param model
	 *            - the model containing cdt data being added to the display.
	 */
	public void compareToModel(final TVModel model) {

		tvModel.removeAppended();
		tvModel.append(model);

		colSelection.resize(tvModel.getDataMatrix().getNumCol());
		colSelection.notifyObservers();

		interactiveXmap.setIndexRange(0,
				tvModel.getDataMatrix().getNumCol() - 1);
		interactiveXmap.notifyObservers();

		((TVModel) tvModel).notifyObservers();
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
			invertedTreeDrawer.setData(tvModel.getAtrLabelInfo(),
					tvModel.getColLabelInfo());
			final LabelInfo trLabelInfo = tvModel.getAtrLabelInfo();

			if (trLabelInfo.getIndex("NODECOLOR") >= 0) {

				TreeColorer.colorUsingLabelType(invertedTreeDrawer.getRootNode(),
						trLabelInfo, trLabelInfo.getIndex("NODECOLOR"));
			}
		} catch (final DendroException e) {

			// LogPanel.println("Had problem setting up the array tree : "
			// + e.getMessage());
			// e.printStackTrace();
			final Box mismatch = new Box(BoxLayout.Y_AXIS);
			mismatch.add(new JLabel(e.getMessage()));
			mismatch.add(new JLabel("The ATR and CDT files appear to be " +
				"corrupted/out-of-sync."));
			mismatch.add(new JLabel("Rebuilding the ATR file's tree " +
				"structure."));

			JOptionPane.showMessageDialog(tvFrame.getAppFrame(), mismatch,
					"Tree Construction Error", JOptionPane.ERROR_MESSAGE);

			dendroView.getColumnTreeView().setEnabled(false);

			try {
				invertedTreeDrawer.setData(null, null);

			} catch (final DendroException ex) {
				LogBuffer.println("Got DendroException when trying to "
						+ "setData() on invertedTreeDrawer: " + e.getMessage());
			}
		}

		final TreeDrawerNode arrayNode = invertedTreeDrawer.getRootNode()
				.findNode(selectedID);

		colSelection.setSelectedNode(arrayNode.getId());
		dendroView.getColumnTreeView().setSelectedNode(arrayNode);

		colSelection.notifyObservers();
		invertedTreeDrawer.notifyObservers();
	}

	// ATR Methods
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
	protected static FileSet offerATRFileSelection() throws LoadException {

		// FileSet fileSet1; // will be chosen...
		//
		// final JFileChooser fileDialog = new JFileChooser();
		// setupATRFileDialog(fileDialog);
		//
		// final int retVal = fileDialog
		// .showOpenDialog(dendroView.getDendroPane());
		//
		// if (retVal == JFileChooser.APPROVE_OPTION) {
		// final File chosen = fileDialog.getSelectedFile();
		// fileSet1 = new FileSet(chosen.getName(), chosen.getParent()
		// + File.separator);
		//
		// } else
		// throw new LoadException("File Dialog closed without selection...",
		// LoadException.NOFILE);
		//
		// return fileSet1;
		return null;
	}

	/**
	 * Sets up a dialog for loading ATR files for tree alignment.
	 *
	 * @param fileDialog
	 *            the dialog to setup
	 */
	protected static void setupATRFileDialog(final JFileChooser fileDialog) {

		final CdtFilter ff = new CdtFilter();
		try {
			fileDialog.addChoosableFileFilter(ff);
			// will fail on pre-1.3 swings
			fileDialog.setAcceptAllFileFilterUsed(true);

		} catch (final Exception e) {
			LogBuffer.println("Exception when adding a ChoosableFileFilter "
					+ "and setAcceptAllFileFilterUsed(): " + e.getMessage());
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
				selectedID = colSelection.getSelectedNode();

			} catch (final NullPointerException npe) {
				npe.printStackTrace();
			}

			int[] ordering;
			ordering = AtrAligner.align(tvModel.getAtrLabelInfo(),
					tvModel.getColLabelInfo(), model.getAtrLabelInfo(),
					model.getColLabelInfo());

			/*
			 * System.out.print("New ordering: "); for(int i = 0; i <
			 * ordering.length; i++) { System.out.print(ordering[i] + " "); }
			 * System.out.println();
			 */

			((TVModel) tvModel).reorderColumns(ordering);
			((TVModel) tvModel).notifyObservers();

			if (selectedID != null) {

				updateATRDrawer(selectedID);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.out.println(e.toString());
		}
	}

	// /**
	// * Creates an AtrTVModel for use in tree alignment.
	// *
	// * @param fileSet
	// * @return a new AtrTVModel with the file set loaded into it.
	// * @throws LoadException
	// */
	// protected AtrTVModel makeAtrModel(final FileSet fileSet)
	// throws LoadException {
	//
	// final AtrTVModel atrTVModel = new AtrTVModel();
	//
	// try {
	// atrTVModel.loadNew(fileSet);
	//
	// } catch (final LoadException e) {
	// String message = "Loading Atr model was interrupted.";
	// JOptionPane.showMessageDialog(tvFrame.getAppFrame(), message,
	// "Error", JOptionPane.ERROR_MESSAGE);
	// LogBuffer.logException(e);
	// }
	//
	// return atrTVModel;
	// }

	// Flipping the trees
	// /**
	// * Finds the currently selected genes, mirror image flips them, and then
	// * rebuilds all necessary trees and saved data to the .jtv file.
	// *
	// */
	// private void flipSelectedGTRNode() {
	//
	// int leftIndex, rightIndex;
	// String selectedID;
	// final TreeDrawerNode geneNode = leftTreeDrawer
	// .getNodeById(getGeneSelection().getSelectedNode());
	//
	// if (geneNode == null || geneNode.isLeaf()) {
	//
	// return;
	// }
	//
	// selectedID = geneNode.getId();
	//
	// // find the starting index of the left array tree, the ending
	// // index of the right array tree
	// leftIndex = getDataModel().getGeneHeaderInfo().getHeaderIndex(
	// geneNode.getLeft().getLeftLeaf().getId());
	// rightIndex = getDataModel().getGeneHeaderInfo().getHeaderIndex(
	// geneNode.getRight().getRightLeaf().getId());
	//
	// final int num = getDataModel().getDataMatrix().getNumRow();
	//
	// final int[] newOrder = SetupInvertedArray(num, leftIndex, rightIndex);
	//
	// /*
	// * System.out.print("Fliping to: "); for(int i = 0; i < newOrder.length;
	// * i++) { System.out.print(newOrder[i] + " "); } System.out.println("");
	// */
	//
	// ((TVModel) getDataModel()).reorderGenes(newOrder);
	// // ((TVModel)getDataModel()).saveGeneOrder(newOrder);
	// ((Observable) getDataModel()).notifyObservers();
	//
	// updateGTRDrawer(selectedID);
	// }

	// private int[] SetupInvertedArray(final int num, final int leftIndex,
	// final int rightIndex) {
	//
	// final int[] newOrder = new int[num];
	//
	// for (int i = 0; i < num; i++) {
	//
	// newOrder[i] = i;
	// }
	//
	// for (int i = 0; i <= (rightIndex - leftIndex); i++) {
	//
	// newOrder[leftIndex + i] = rightIndex - i;
	// }
	//
	// return newOrder;
	// }

	// /**
	// * Finds the currently selected arrays, mirror image flips them, and then
	// * rebuilds all necessary trees and saved data to the .jtv file.
	// */
	// private void flipSelectedATRNode() {
	//
	// int leftIndex, rightIndex;
	// String selectedID;
	// final TreeDrawerNode arrayNode = invertedTreeDrawer
	// .getNodeById(getArraySelection().getSelectedNode());
	//
	// if (arrayNode == null || arrayNode.isLeaf()) {
	//
	// return;
	// }
	//
	// selectedID = arrayNode.getId();
	//
	// // find the starting index of the left array tree,
	// // the ending index of the right array tree
	// leftIndex = getDataModel().getArrayHeaderInfo().getHeaderIndex(
	// arrayNode.getLeft().getLeftLeaf().getId());
	// rightIndex = getDataModel().getArrayHeaderInfo().getHeaderIndex(
	// arrayNode.getRight().getRightLeaf().getId());
	//
	// final int num = getDataModel().getDataMatrix().getNumUnappendedCol();
	//
	// final int[] newOrder = new int[num];
	//
	// for (int i = 0; i < num; i++) {
	//
	// newOrder[i] = i;
	// }
	//
	// for (int i = 0; i <= (rightIndex - leftIndex); i++) {
	//
	// newOrder[leftIndex + i] = rightIndex - i;
	// }
	//
	// /*
	// * System.out.print("Fliping to: "); for(int i = 0; i < newOrder.length;
	// * i++) { System.out.print(newOrder[i] + " "); } System.out.println("");
	// */
	//
	// ((TVModel) getDataModel()).reorderArrays(newOrder);
	// ((Observable) getDataModel()).notifyObservers();
	//
	// updateATRDrawer(selectedID);
	// }

	/**
	 * this is meant to be called from setupViews. It make sure that the trees
	 * are generated from the current model, and enables/disables them as
	 * required.
	 *
	 * I factored it out because it is common between DendroView and
	 * KnnDendroView.
	 */
	protected void bindTrees() {

		if ((tvModel != null) && tvModel.aidFound()) {
			try {
				dendroView.getColumnTreeView().setEnabled(true);

				invertedTreeDrawer.setData(tvModel.getAtrLabelInfo(),
						tvModel.getColLabelInfo());
				final LabelInfo trLabelInfo = tvModel.getAtrLabelInfo();

				if (trLabelInfo.getIndex("NODECOLOR") >= 0) {
					TreeColorer.colorUsingLabelType(
							invertedTreeDrawer.getRootNode(), trLabelInfo,
							trLabelInfo.getIndex("NODECOLOR"));
				}

			} catch (final DendroException e) {
				String message = "The ATR and CDT files appear to be " +
					"corrupted/out-of-sync.  Rebuilding the ATR file's tree " +
					"structure.";

				JOptionPane.showMessageDialog(tvFrame.getAppFrame(), message,
						"Error", JOptionPane.ERROR_MESSAGE);
				LogBuffer.logException(e);

				dendroView.getColumnTreeView().setEnabled(false);

				try {
					invertedTreeDrawer.setData(null, null);

				} catch (final DendroException ex) {
					message = "Got DendroException in setData() for "
							+ "invertedTreeDrawer in bindTrees(): "
							+ e.getMessage();

					JOptionPane.showMessageDialog(tvFrame.getAppFrame(),
							message, "Error", JOptionPane.ERROR_MESSAGE);
					LogBuffer.logException(ex);
				}
			}
		} else {
			dendroView.getColumnTreeView().setEnabled(false);

			try {
				invertedTreeDrawer.setData(null, null);

			} catch (final DendroException e) {
				final String message = "Got DendroException in setData() "
						+ "for invertedTreeDrawer in bindTrees(): "
						+ e.getMessage();

				JOptionPane.showMessageDialog(tvFrame.getAppFrame(), message,
						"Error", JOptionPane.ERROR_MESSAGE);
				LogBuffer.logException(e);
			}
		}

		invertedTreeDrawer.notifyObservers();

		if ((tvModel != null) && tvModel.gidFound()) {
			try {
				dendroView.getRowTreeView().setEnabled(true);

				leftTreeDrawer.setData(tvModel.getGtrLabelInfo(),
						tvModel.getRowLabelInfo());
				final LabelInfo gtrLabelInfo = tvModel.getGtrLabelInfo();

				if (gtrLabelInfo.getIndex("NODECOLOR") >= 0) {
					TreeColorer.colorUsingLabelType(leftTreeDrawer.getRootNode(),
							tvModel.getGtrLabelInfo(),
							gtrLabelInfo.getIndex("NODECOLOR"));

				} else {
					TreeColorer.colorUsingLeaf(leftTreeDrawer.getRootNode(),
							tvModel.getRowLabelInfo(), tvModel
									.getRowLabelInfo().getIndex("FGCOLOR"));
				}

			} catch (final DendroException e) {
				String message = "The GTR and CDT files appear to be " +
					"corrupted/out-of-sync.  Rebuilding the GTR file's tree " +
					"structure.";

				JOptionPane.showMessageDialog(tvFrame.getAppFrame(), message,
						"Error", JOptionPane.ERROR_MESSAGE);
				LogBuffer.logException(e);

				dendroView.getRowTreeView().setEnabled(false);

				try {
					leftTreeDrawer.setData(null, null);

				} catch (final DendroException ex) {
					message = "Got DendroException in setData() "
							+ "for leftTreeDrawer in bindTrees(): "
							+ ex.getMessage();

					JOptionPane.showMessageDialog(tvFrame.getAppFrame(),
							message, "Error", JOptionPane.ERROR_MESSAGE);
					LogBuffer.logException(ex);
				}
			}
		} else {
			dendroView.getRowTreeView().setEnabled(false);

			try {
				leftTreeDrawer.setData(null, null);

			} catch (final DendroException e) {
				final String message = "Got DendroException in setData() "
						+ "for leftTreeDrawer in bindTrees(): "
						+ e.getMessage();

				JOptionPane.showMessageDialog(tvFrame.getAppFrame(), message,
						"Error", JOptionPane.ERROR_MESSAGE);
				LogBuffer.logException(e);
			}
		}

		leftTreeDrawer.notifyObservers();
	}

	/**
	 * Update the state of color extractor to reflect settings from an imported
	 * node.
	 * 
	 * @param node
	 * @throws BackingStoreException
	 */
	public void importColorPreferences(final Preferences oldNode)
			throws BackingStoreException {
		
		mvController.importColorPreferences(oldNode);
	}

	/**
	 * Update the state of label views to reflect settings from an imported
	 * node.
	 * 
	 * @param oldNode - The old Preferences node from which to import label
	 * preferences for both LabelViews (rows & columns).
	 */
	public void importLabelPreferences(final Preferences oldNode) {

		LogBuffer.println("Importing labels...");
		
		try {
			Preferences labelViewNode;
			if(!oldNode.nodeExists("RowLabelView")) {
				LogBuffer.println("Missing node in parent. Could not import "
						+ "row label settings.");
			} else {
				labelViewNode = oldNode.node("RowLabelView");
				dendroView.getRowLabelView().importStateFrom(labelViewNode);
			}
		
			if(!oldNode.nodeExists("ColLabelView")) {
				LogBuffer.println("Missing node in parent. Could not import "
						+ "column label settings.");
			} else {
				labelViewNode = oldNode.node("ColLabelView");
				dendroView.getColLabelView().importStateFrom(labelViewNode);
			}
		} catch (BackingStoreException e) {
			LogBuffer.logException(e);
			LogBuffer.println("Problem when trying to import label settings."
					+ " Could not complete import.");
			return;
		}
	}

	// /**
	// * show summary of the specified indexes
	// */
	// public void showSubDataModel(final int[] indexes) {
	//
	// tvFrame.showSubDataModel(indexes, null, null);
	// }

	/**
	 * This should be called after setDataModel has been set to the appropriate
	 * model
	 *
	 * @param colSelection
	 */
	public void setColumnSelection(final TreeSelectionI colSelection) {

		if (this.colSelection != null) {
			this.colSelection.deleteObserver(dendroView);
		}

		this.colSelection = colSelection;
		colSelection.addObserver(dendroView);
		
		mvController.setColSelection(colSelection);
		
		dendroView.getColumnTreeView().setTreeSelection(colSelection);
		dendroView.getRowLabelView().setOtherSelection(colSelection);
		dendroView.getColLabelView().setDrawSelection(colSelection);
	}

	/**
	 * General setup for the row selection object. A reference of it is passed
	 * to all classes that need to know about the row selection state.
	 *
	 * @param rowSelection
	 */
	public void setRowSelection(final TreeSelectionI rowSelection) {

		if (this.rowSelection != null) {
			this.rowSelection.deleteObserver(dendroView);
		}

		this.rowSelection = rowSelection;
		rowSelection.addObserver(dendroView);

		mvController.setRowSelection(rowSelection);
		
		dendroView.getRowTreeView().setTreeSelection(rowSelection);
		dendroView.getRowLabelView().setDrawSelection(rowSelection);
		dendroView.getColLabelView().setOtherSelection(rowSelection);
	}

	public void setNewIncluded(final int[] gIncluded, final int[] aIncluded) {

		dendroView.getRowLabelView().getLabelSummary().setIncluded(gIncluded);
		dendroView.getColLabelView().getLabelSummary()
				.setIncluded(aIncluded);
	}

	public int[] getColumnIncluded() {

		return dendroView.getColLabelView().getLabelSummary().getIncluded();
	}

	public int[] getRowIncluded() {

		return dendroView.getRowLabelView().getLabelSummary().getIncluded();
	}

	public boolean hasDendroView() {

		return dendroView != null;
	}
	
	/**
	 * Returns a reference to the ColorExtractor instance assigned to the 
	 * current MatrixViews (Global & Interactive).
	 * @return A ColorExtractor instance.
	 */
	public ColorExtractor getColorExtractor() {
		
		return mvController.getColorExtractor();
	}

	public MapContainer getInteractiveXMap() {

		return interactiveXmap;
	}

	public MapContainer getInteractiveYMap() {

		return interactiveYmap;
	}
	
	public MatrixView getInteractiveMatrixView() {
		
		return dendroView.getInteractiveMatrixView();
	}
	
	public MatrixView getGlobalMatrixView() {
		
		return dendroView.getGlobalMatrixView();
	}

	@Override
	public void update(final Observable o, final Object arg) {

		if (o instanceof LabelSummary) {
			updateSearchBoxes();

		} else if (o instanceof MapContainer) {
			setAdaptiveButtonStatus();

		} else if (o instanceof TreeSelectionI) {
			requestFocusForZoomBtn();
		}
	}

	/**
	 * Updates the content of the search boxes to the up-to-date headers.
	 */
	private void updateSearchBoxes() {

		dendroView.updateSearchTermBoxes(tvModel.getRowLabelInfo(),
						tvModel.getColLabelInfo(), interactiveXmap,
						interactiveYmap);
	}

	/**
	 * Requests the focus for the zoom button to draw attention to it, for
	 * example when the user makes a selection in the matrix.
	 */
	private void requestFocusForZoomBtn() {

		setAdaptiveButtonStatus();

		if(dendroView.isAFinderBoxFocussed()) {
			return;
		}

		if (rowSelection.getNSelectedIndexes() > 0) {
			dendroView.getZoomButton().requestFocusInWindow();
		} else {
			dendroView.getXYPlusButton().requestFocusInWindow();
		}
	}

	/**
	 * Enables or disables button based on the current zoom status of the two
	 * different axis maps. If they are set to minimum scale, then the relevant
	 * buttons should be disabled as they become useless. This provides
	 * intuitive visual clues to the user.
	 */
	private void setAdaptiveButtonStatus() {

		// TODO create an object that holds these statuses instead of an array
		boolean[] zoomStatusList = mvController.getZoomStatusForButtons();
		
		boolean isXMin = zoomStatusList[0];
		boolean isYMin = zoomStatusList[1];
		boolean atRight = zoomStatusList[2];
		boolean atLeft = zoomStatusList[3];
		boolean atTop = zoomStatusList[4];
		boolean atBottom = zoomStatusList[5];
		boolean isSelectionZoomed = zoomStatusList[6];
		
		int xTilesVisible = interactiveXmap.getNumVisible();
		int yTilesVisible = interactiveYmap.getNumVisible();

		/* Zoom-out buttons disabled if min scale for axis is reached. */
		dendroView.getHomeButton().setEnabled(!(isXMin && isYMin));
		dendroView.getYMinusBottomButton().setEnabled(!atTop);
		dendroView.getYMinusTopButton().setEnabled(!atBottom);
		dendroView.getXMinusRightButton().setEnabled(!atLeft);
		dendroView.getXMinusLeftButton().setEnabled(!atRight);
		dendroView.getXYMinusButton().setEnabled(!(isXMin && isYMin));

		/*
		 * Zoom-in buttons disabled if visible tile number for axis is 1 or if
		 * the zoom target is already fully zoomed
		 */
		dendroView.getXRightPlusButton().setEnabled((xTilesVisible != 1));
		dendroView.getYPlusBottomButton().setEnabled((yTilesVisible != 1));
		dendroView.getXLeftPlusButton().setEnabled((xTilesVisible != 1));
		dendroView.getYPlusTopButton().setEnabled((yTilesVisible != 1));
		dendroView.getXYPlusButton().setEnabled(
				(xTilesVisible != 1) || (yTilesVisible != 1));
		dendroView.getZoomButton().setEnabled(!isSelectionZoomed);
	}
	
	/**
	 * Copies labels to clipboard.
	 * @param copyType - The CopyType chosen by the user.
	 * @param isRows - A flag identifying the axis.
	 */
	public void copyLabels(final CopyType copyType, final boolean isRows) {
		
		if(!tvFrame.isLoaded() || tvFrame.getRunning() == null) {
			return;
		}
		
		String labels = "";
		LabelSummary axisSummary;
		LabelInfo axisInfo;
		TreeSelectionI treeSelection;
		MapContainer map;
		
		if(isRows) {
			axisSummary = tvFrame.getDendroView().getRowLabelView()
					.getLabelSummary();
			axisInfo = tvModel.getRowLabelInfo();
			treeSelection = tvFrame.getRowSelection();
			map = interactiveYmap;
			
		} else {
			axisSummary = tvFrame.getDendroView().getColLabelView()
					.getLabelSummary();
			axisInfo = tvModel.getColLabelInfo();
			treeSelection = tvFrame.getColSelection();
			map = interactiveXmap;
		}
		
		labels = constructLabelString(axisSummary, axisInfo, 
				treeSelection, map, copyType, isRows);
		
		StringSelection stringSelection = new StringSelection(labels);
		Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		clpbrd.setContents(stringSelection, null);
	}
	
	/**
	 * Construct a clipboard string from the labels according to the 
	 * CopyType and axis.
	 * @param axisSummary - Has the included labels.
	 * @param axisInfo - Needed to retrieve labels from LabelSummary.
	 * @param treeSelection - Needed for copying selected labels.
	 * @param map - The IMV map for the specific axis. Needed to find out
	 * which range of indices is visible.
	 * @param copyType - The type of clipboard copying chosen by the user.
	 * @param isRows - A flag which indicates the target axis.
	 * @return A constructed string for the clipboard.
	 */
	private static String constructLabelString(final LabelSummary axisSummary, 
			final LabelInfo axisInfo, final TreeSelectionI treeSelection, 
			final MapContainer map, final CopyType copyType, 
			final boolean isRows) {
		
		String seperator = "\t";
		int labelNum = axisInfo.getNumLabels();
		final StringBuilder sb = new StringBuilder();
		
		if(isRows) {
			seperator = "\n";
		}

		if(copyType == CopyType.ALL) {
			for (int i = 0; i < labelNum; i++) {
				sb.append(axisSummary.getSummary(axisInfo, i));
				sb.append(seperator);
			}
			
		} else if(copyType == CopyType.SELECTION && treeSelection != null) {
			for (int i = 0; i < labelNum; i++) {
				if(treeSelection.isIndexSelected(i)) {
					sb.append(axisSummary.getSummary(axisInfo, i));
					sb.append(seperator);
				}
			}
			
		} else if(copyType == CopyType.VISIBLE_MATRIX) {
			int start = map.getFirstVisible();
			int end = start + map.getNumVisible();
			
			for (int i = start; i < end; i++) {
				sb.append(axisSummary.getSummary(axisInfo, i));
				sb.append(seperator);
			}
		}

		return sb.toString();
	}
}
