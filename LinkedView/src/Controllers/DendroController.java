package Controllers;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
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
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import Utilities.Helper;
import edu.stanford.genetics.treeview.CdtFilter;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
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

	private int[] arrayIndex = null;
	private int[] geneIndex = null;

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

		interactiveXmap = new MapContainer(IntegerMap.FIXED, "GlobalXMap");
		interactiveYmap = new MapContainer(IntegerMap.FIXED, "GlobalYMap");

		globalXmap = new MapContainer(IntegerMap.FIXED, "OverviewXMap");
		globalYmap = new MapContainer(IntegerMap.FIXED, "OverviewYMap");
	}
	
	/**
	 * Checks whether there is a configuration node for the current model and
	 * DendroView. If not it creates one.
	 * @param The parent Preferences node for the node to be created in this
	 * class.
	 */
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			if (tvModel.getDocumentConfigRoot() != null) {
				configNode = ((TVModel) tvModel).getDocumentConfig();

			} else {
				configNode = Preferences.userRoot().node("DendroView");
			}
		} else {
			// TODO handle null parentNode beyond logging
			LogBuffer.println("parentNode in " + this.getClass().getName() 
					+ " was null.");
		}
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

		/* Get the saved settings */
		setConfigNode(tvFrame.getConfigNode());

		setMatrixController();
		updateHeaderInfo();
		bindComponentFunctions();

		dendroView.setupSearch(tvModel.getRowHeaderInfo(),
						tvModel.getColumnHeaderInfo(), interactiveXmap,
						interactiveYmap);
		
		dendroView.setupLayout();
		setObservables();

		/*
		 * TODO Find solution... doesn't work because of resetMapContainers
		 * which needs to run to reset MapContainer scales in GlobalView for
		 * potentially changed AppFrame size between closing TreeView and
		 * loading a new matrix (changed availablePixels in GlobalView).
		 */
		// setSavedScale();

		/**
		 * make sure pixel colors are calculated after new model was loaded. 
		 */
		mvController.updateMatrixPixels();
		/*
		 * Needs to wait for repaint() called from resetMapContainer() and
		 * component listener. TODO implement resetMapContainer/ setSavedScale
		 * differently...
		 */
		mvController.resetMatrixViews();

		addKeyBindings();
		addListeners();
	}
	
	/**
	 * Sets up all necessary components for the MatrixView controller.
	 */
	private void setMatrixController() {
		
		this.mvController = new MatrixViewController(
				dendroView.getInteractiveMatrixView(), 
				dendroView.getGlobalMatrixView(), tvModel);
		
		mvController.setConfigNode(configNode);
		mvController.setup();
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
		dendroView.getRowLabelView().getHeaderSummary().addObserver(this);
		dendroView.getColumnLabelView().getHeaderSummary().addObserver(this);

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
		dendroView.addResizeListener(new AppFrameListener());
		dendroView.addDeselectClickListener(new PanelClickDeselector());
		
		mvController.addListeners();
	}

	/* -------------- Listeners --------------------- */
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

			notifyAllMapObservers();
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

		}

		@Override
		public void componentShown(final ComponentEvent e) {

			final double atr_loc = configNode.getDouble("atr_Loc", 0.5);
			final double gtr_loc = configNode.getDouble("gtr_Loc", 0.5);

			dendroView.setTreeVisibility(atr_loc, gtr_loc);
		}
	}

	/**
	 * Listens to the resizing of DendroView2 and makes changes to MapContainers
	 * as a result.
	 */
	private class AppFrameListener extends ComponentAdapter {

		// Timer to prevent repeatedly saving window dimensions upon resize
		private final int saveResizeDelay = 1000;
		private javax.swing.Timer saveResizeTimer;
		ActionListener saveWindowAttrs = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent evt) {
				if (evt.getSource() == saveResizeTimer) {
					/* Stop timer */
					saveResizeTimer.stop();
					saveResizeTimer = null;

					tvFrame.saveSettings();
				}
			}
		};

		@Override
		public void componentResized(final ComponentEvent arg0) {

			// Previously, resetMapContainers was called here, but that caused
			// the zoom level to change when the user resized the window, so I
			// added a way to track the currently visible area in mapContainer
			// and implemented these functions to make the necessary
			// adjustments to the image when that happens
			refocusViewPort();

			// Save the new dimensions/position if it's done changing
			if (this.saveResizeTimer == null) {
				/*
				 * Start waiting for saveResizeDelay millis to elapse and then
				 * call actionPerformed of the ActionListener "saveWindowAttrs".
				 */
				this.saveResizeTimer = new Timer(this.saveResizeDelay,
						saveWindowAttrs);
				this.saveResizeTimer.start();
			} else {
				/* Event came too soon, swallow it by resetting the timer.. */
				this.saveResizeTimer.restart();
			}
		}
	}

	/**
	 * Notifies all MapContainers' observers.
	 */
	private void notifyAllMapObservers() {

		globalXmap.notifyObservers();
		globalYmap.notifyObservers();

		interactiveXmap.notifyObservers();
		interactiveYmap.notifyObservers();
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
	 * Stores MapContainer settings (scale + scroll values) in Preferences
	 * nodes.
	 */
	public void saveSettings() {

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
	private void refocusViewPort() {

		interactiveXmap.adjustToScreenChange();
		interactiveYmap.adjustToScreenChange();

		globalXmap.setToMinScale();
		globalYmap.setToMinScale();

		saveSettings();
	}

	public void saveImage(final JPanel panel) throws IOException {

		// File saveFile = new File("savedImage.png");
		//
		// final JFileChooser fc = new JFileChooser();
		//
		// fc.setFileFilter(new FileNameExtensionFilter("PNG", "png"));
		// fc.setSelectedFile(saveFile);
		// final int returnVal = fc.showSaveDialog(dendroView.getDendroPane());
		//
		// if (returnVal == JFileChooser.APPROVE_OPTION) {
		// saveFile = fc.getSelectedFile();
		//
		// String fileName = saveFile.toString();
		//
		// if (!fileName.endsWith(".png")) {
		// fileName += ".png";
		// saveFile = new File(fileName);
		// }
		//
		// final BufferedImage im = new BufferedImage(panel.getWidth(),
		// panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
		//
		// panel.paint(im.getGraphics());
		// ImageIO.write(im, "PNG", saveFile);
		// }
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

		if (tvModel.getColumnHeaderInfo().getIndex("GROUP") != -1) {
			final HeaderInfo headerInfo = tvModel.getColumnHeaderInfo();
			final int groupIndex = headerInfo.getIndex("GROUP");

			arrayIndex = getGroupVector(headerInfo, groupIndex);

		} else {
			arrayIndex = null;
		}

		if (tvModel.getRowHeaderInfo().getIndex("GROUP") != -1) {
			System.err.println("got gene group header");
			final HeaderInfo headerInfo = tvModel.getRowHeaderInfo();
			final int groupIndex = headerInfo.getIndex("GROUP");
			geneIndex = getGroupVector(headerInfo, groupIndex);

		} else {
			geneIndex = null;
		}

		// ISSUE: Needs DataModel, not TVModel. Should dataModel be used
		// in this class rather than TVModel?
		if ((arrayIndex != null) || (geneIndex != null)) {
			tvModel = new ReorderedDataModel(tvModel, geneIndex, arrayIndex);
			LogBuffer.println("DataModel issue in DendroController.");
		}
	}

	/**
	 * Returns an array of indexes of K-Means groups.
	 *
	 * @param headerInfo
	 * @param groupIndex
	 * @return
	 */
	private static int[] getGroupVector(final HeaderInfo headerInfo,
			final int groupIndex) {

		int ngroup = 0;
		String cur = headerInfo.getHeader(0, groupIndex);

		for (int i = 0; i < headerInfo.getNumHeaders(); i++) {

			final String test = headerInfo.getHeader(i, groupIndex);
			if (!cur.equals(test)) {
				cur = test;
				ngroup++;
			}
		}

		final int[] groupVector = new int[ngroup + headerInfo.getNumHeaders()];
		ngroup = 0;
		cur = headerInfo.getHeader(0, groupIndex);

		for (int i = 0; i < headerInfo.getNumHeaders(); i++) {

			final String test = headerInfo.getHeader(i, groupIndex);
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

		// Handle selection
		if (geneIndex != null) {
			setRowSelection(new ReorderedTreeSelection(
					tvFrame.getRowSelection(), geneIndex));

		} else {
			setRowSelection(tvFrame.getRowSelection());
		}

		if (arrayIndex != null) {
			setColumnSelection(new ReorderedTreeSelection(
					tvFrame.getColumnSelection(), arrayIndex));

		} else {
			setColumnSelection(tvFrame.getColumnSelection());
		}
		
		setupMapContainers();

		// TODO replace with IMVController method
		interactiveXmap.setScrollbar(dendroView.getMatrixXScroll());
		interactiveYmap.setScrollbar(dendroView.getMatrixYScroll());

		leftTreeDrawer = new TreePainter();
		dendroView.getRowTreeView().setTreeDrawer(leftTreeDrawer);

		invertedTreeDrawer = new TreePainter();
		dendroView.getColumnTreeView().setTreeDrawer(invertedTreeDrawer);

		setPresets();

		// this is here because my only subclass shares this code.
		bindTrees();

		// Set context menu for LabelViews
		LabelContextMenu lCMenu = new LabelContextMenu();
		LabelContextMenuController lCMenuController = new LabelContextMenuController(
				lCMenu, tvController);
		dendroView.getRowLabelView().setComponentPopupMenu(lCMenu);
		dendroView.getColumnLabelView().setComponentPopupMenu(lCMenu);

		// perhaps I could remember this stuff in the MapContainer...
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
	private void setPresets() {

		interactiveXmap.setConfigNode(configNode);
		interactiveYmap.setConfigNode(configNode);

		globalXmap.setConfigNode(configNode);
		globalYmap.setConfigNode(configNode);

		// URLs
		mvController.getColorExtractor().setConfigNode(configNode);

		dendroView.getRowLabelView().setConfigNode(configNode);
		dendroView.getColumnLabelView().setConfigNode(configNode);
		dendroView.getColumnTreeView().getHeaderSummary()
				.setConfigNode(configNode);
		dendroView.getRowTreeView().getHeaderSummary()
				.setConfigNode(configNode);
	}

	/**
	 * Assigns the MapContainers to classes which need to know about them.
	 */
	private void setupMapContainers() {

		dendroView.getColumnTreeView().setMap(interactiveXmap);
		dendroView.getColumnLabelView().setMap(interactiveXmap);
		dendroView.getRowTreeView().setMap(interactiveYmap);
		dendroView.getRowLabelView().setMap(interactiveYmap);

		mvController.setInteractiveMapContainers(interactiveXmap, 
				interactiveYmap);
		mvController.setGlobalMapContainers(globalXmap, globalYmap);
	}

	/**
	 * Updates all headerInfo instances for all the view components.
	 */
	private void updateHeaderInfo() {

		dendroView.getColumnTreeView().setATRHeaderInfo(
				tvModel.getAtrHeaderInfo());
		dendroView.getRowTreeView()
				.setGTRHeaderInfo(tvModel.getGtrHeaderInfo());
		dendroView.getColumnLabelView().setHeaderInfo(
				tvModel.getColumnHeaderInfo());
		dendroView.getRowLabelView().setHeaderInfo(tvModel.getRowHeaderInfo());
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
			invertedTreeDrawer.setData(tvModel.getAtrHeaderInfo(),
					tvModel.getColumnHeaderInfo());
			final HeaderInfo trHeaderInfo = tvModel.getAtrHeaderInfo();

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
	protected FileSet offerATRFileSelection() throws LoadException {

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
	protected void setupATRFileDialog(final JFileChooser fileDialog) {

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
			ordering = AtrAligner.align(tvModel.getAtrHeaderInfo(),
					tvModel.getColumnHeaderInfo(), model.getAtrHeaderInfo(),
					model.getColumnHeaderInfo());

			/*
			 * System.out.print("New ordering: "); for(int i = 0; i <
			 * ordering.length; i++) { System.out.print(ordering[i] + " "); }
			 * System.out.println();
			 */

			((TVModel) tvModel).reorderArrays(ordering);
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

				invertedTreeDrawer.setData(tvModel.getAtrHeaderInfo(),
						tvModel.getColumnHeaderInfo());
				final HeaderInfo trHeaderInfo = tvModel.getAtrHeaderInfo();

				if (trHeaderInfo.getIndex("NODECOLOR") >= 0) {
					TreeColorer.colorUsingHeader(
							invertedTreeDrawer.getRootNode(), trHeaderInfo,
							trHeaderInfo.getIndex("NODECOLOR"));
				}

			} catch (final DendroException e) {
				String message = "Seems like there is a mismatch between your "
						+ "ATR and CDT files. Ditching Array Tree.";

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

				leftTreeDrawer.setData(tvModel.getGtrHeaderInfo(),
						tvModel.getRowHeaderInfo());
				final HeaderInfo gtrHeaderInfo = tvModel.getGtrHeaderInfo();

				if (gtrHeaderInfo.getIndex("NODECOLOR") >= 0) {
					TreeColorer.colorUsingHeader(leftTreeDrawer.getRootNode(),
							tvModel.getGtrHeaderInfo(),
							gtrHeaderInfo.getIndex("NODECOLOR"));

				} else {
					TreeColorer.colorUsingLeaf(leftTreeDrawer.getRootNode(),
							tvModel.getRowHeaderInfo(), tvModel
									.getRowHeaderInfo().getIndex("FGCOLOR"));
				}

			} catch (final DendroException e) {
				String message = "There seems to be a mismatch between your "
						+ "GTR and CDT files. Ditching Gene Tree, "
						+ "since it's lame.";

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
	public void importColorPreferences(Preferences oldNode)
			throws BackingStoreException {
		
		mvController.importColorPreferences(oldNode);
	}

	/**
	 * Update the state of label views to reflect settings from an imported
	 * node.
	 * 
	 * @param node
	 */
	public void importLabelPreferences(Preferences node) {

		LogBuffer.println("Importing labels...");
		
		dendroView.getRowLabelView().importSettingsFromNode(
				node.node("RowLabelView"));
		dendroView.getColumnLabelView().importSettingsFromNode(
				node.node("ColLabelView"));
	}

	// /**
	// * show summary of the specified indexes
	// */
	// public void showSubDataModel(final int[] indexes) {
	//
	// tvFrame.showSubDataModel(indexes, null, null);
	// }

	// Setters
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
		dendroView.getColumnLabelView().setDrawSelection(colSelection);
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
		dendroView.getColumnLabelView().setOtherSelection(rowSelection);
	}

	public void setNewIncluded(final int[] gIncluded, final int[] aIncluded) {

		dendroView.getRowLabelView().getHeaderSummary().setIncluded(gIncluded);
		dendroView.getColumnLabelView().getHeaderSummary()
				.setIncluded(aIncluded);
	}

	public int[] getColumnIncluded() {

		return dendroView.getColumnLabelView().getHeaderSummary().getIncluded();
	}

	public int[] getRowIncluded() {

		return dendroView.getRowLabelView().getHeaderSummary().getIncluded();
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

		if (o instanceof HeaderSummary) {
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

		dendroView.updateSearchTermBoxes(tvModel.getRowHeaderInfo(),
						tvModel.getColumnHeaderInfo(), interactiveXmap,
						interactiveYmap);
	}

	/**
	 * Requests the focus for the zoom button to draw attention to it, for
	 * example when the user makes a selection in the matrix.
	 */
	private void requestFocusForZoomBtn() {

		if (rowSelection.getNSelectedIndexes() > 0) {
			dendroView.getZoomButton().requestFocusInWindow();
		} else {
			dendroView.getXYPlusButton().requestFocusInWindow();
		}
		setAdaptiveButtonStatus();
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
}
