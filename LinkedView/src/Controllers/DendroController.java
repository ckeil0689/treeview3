package Controllers;

import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import Utilities.Helper;
import edu.stanford.genetics.treeview.CdtFilter;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
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
import edu.stanford.genetics.treeview.plugin.dendroview.ColorPresets;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroException;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;
import edu.stanford.genetics.treeview.plugin.dendroview.DendrogramFactory;
import edu.stanford.genetics.treeview.plugin.dendroview.DoubleArrayDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.GlobalView;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;
import edu.stanford.genetics.treeview.plugin.dendroview.TreeColorer;
import edu.stanford.genetics.treeview.plugin.dendroview.TreePainter;

/* TODO separate some parts into dedicated GlobalView controller */
/**
 * Controller class handling UI input and calculations related to DendroView.
 * 
 * @author chris0689
 *
 */
public class DendroController implements ConfigNodePersistent {

	private DendroView dendroView;
	private final TreeViewFrame tvFrame;
	private DataModel tvModel;

	protected Preferences configNode;

	private int[] arrayIndex = null;
	private int[] geneIndex = null;

	// Drawers
	protected ArrayDrawer arrayDrawer;
	protected TreePainter invertedTreeDrawer;
	protected TreePainter leftTreeDrawer;

	// MapContainers
	protected final MapContainer globalXmap;
	protected final MapContainer globalYmap;

	// Selections
	private TreeSelectionI geneSelection = null;
	private TreeSelectionI arraySelection = null;

	// Color Extractor
	private ColorExtractor colorExtractor;

	public DendroController(final TreeViewFrame tvFrame) {

		this.tvFrame = tvFrame;

		globalXmap = new MapContainer("Fixed", "GlobalXMap");
		globalYmap = new MapContainer("Fixed", "GlobalYMap");
	}

	public void setNew(final DendroView dendroView, final DataModel tvModel) {

		this.dendroView = dendroView;
		this.tvModel = tvModel;

		/* Get the saved settings */
		setConfigNode(tvFrame.getConfigNode());

		updateHeaderInfo();
		bindComponentFunctions();

		dendroView.prepareView(tvModel.getGeneHeaderInfo(),
				tvModel.getArrayHeaderInfo(), globalXmap, globalYmap);

		setSavedScale();

		addKeyBindings();
		addViewListeners();
		addMenuBtnListeners();
	}

	/** Adds all keyboard shortcuts that can be used with DendroView open. */
	private void addKeyBindings() {

		final JPanel dendroPane = dendroView.getDendroPane();

		final InputMap input_map = dendroPane
				.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap action_map = dendroPane.getActionMap();

		/* Gets the system's modifier key (Ctrl or Cmd) */
		final int modifier = Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask();
		final int shift_mask = InputEvent.SHIFT_MASK;

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

		/* Scroll through GlobalView with HOME, END, PgUP, PgDOWN */
		input_map.put(KeyStroke.getKeyStroke("HOME"), "pageYToStart");
		action_map.put("pageYToStart", new HomeKeyYAction());

		input_map.put(KeyStroke.getKeyStroke("END"), "pageYToEnd");
		action_map.put("pageYToEnd", new EndKeyYAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, modifier),
				"pageXToStart");
		action_map.put("pageXToStart", new HomeKeyXAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, modifier),
				"pageXToEnd");
		action_map.put("pageXToEnd", new EndKeyXAction());

		input_map.put(KeyStroke.getKeyStroke("PAGE_UP"), "pageYUp");
		action_map.put("pageYUp", new PageUpYAction());

		input_map.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "pageYDown");
		action_map.put("pageYDown", new PageDownYAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, modifier),
				"pageXUp");
		action_map.put("pageXUp", new PageUpXAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, modifier),
				"pageXDown");
		action_map.put("pageXDown", new PageDownXAction());

		/* Scroll through GlobalView with arrow keys */
		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, modifier),
				"arrowYToStart");
		action_map.put("arrowYToStart", new HomeKeyYAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, modifier),
				"arrowYToEnd");
		action_map.put("arrowYToEnd", new EndKeyYAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, modifier),
				"arrowXToStart");
		action_map.put("arrowXToStart", new HomeKeyXAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, modifier),
				"arrowXToEnd");
		action_map.put("arrowXToEnd", new EndKeyXAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, shift_mask),
				"arrowYUp");
		action_map.put("arrowYUp", new PageUpYAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, shift_mask),
				"arrowYDown");
		action_map.put("arrowYDown", new PageDownYAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, shift_mask),
				"arrowXUp");
		action_map.put("arrowXUp", new PageUpXAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, shift_mask),
				"arrowXDown");
		action_map.put("arrowXDown", new PageDownXAction());

		/* arrow 1-step */
		input_map.put(KeyStroke.getKeyStroke("UP"), "arrowUp");
		action_map.put("arrowUp", new ArrowUpAction());

		input_map.put(KeyStroke.getKeyStroke("DOWN"), "arrowDown");
		action_map.put("arrowDown", new ArrowDownAction());

		input_map.put(KeyStroke.getKeyStroke("LEFT"), "arrowLeft");
		action_map.put("arrowLeft", new ArrowLeftAction());

		input_map.put(KeyStroke.getKeyStroke("RIGHT"), "arrowRight");
		action_map.put("arrowRight", new ArrowRightAction());

		/* zoom actions */
		input_map.put(KeyStroke.getKeyStroke("MINUS"), "zoomOut");
		action_map.put("zoomOut", new ZoomOutAction());

		input_map.put(KeyStroke.getKeyStroke("EQUALS"), "zoomIn");
		action_map.put("zoomIn", new ZoomInAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, modifier),
				"zoomSelection");
		action_map.put("zoomSelection", new ZoomAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, modifier),
				"resetZoom");
		action_map.put("resetZoom", new HomeAction());
	}

	/**
	 * Recalculates proportions for the MapContainers, when the layout was
	 * changed by removing or adding components, or resizing the TVFrame. Only
	 * works if GlobalView is already resized (has availablePixels set to new
	 * value!).
	 */
	public void resetMapContainers() {

		dendroView.setMatrixHome(true);

		updateGlobalView();
	}

	private void updateGlobalView() {

		globalXmap.notifyObservers();
		globalYmap.notifyObservers();

		dendroView.getGlobalView().repaint();
	}

	/**
	 * Adds listeners to DendroView's UI components.
	 */
	private void addViewListeners() {

		dendroView.addScaleListeners(new ScaleListener());
		dendroView.addZoomListener(new ZoomListener());
		dendroView.addCompListener(new ResizeListener());
		dendroView.addSearchCloseListener(new CloseSearchAction());
	}

	/**
	 * Add listener to the search button.
	 */
	private void addMenuBtnListeners() {

		dendroView.addSearchBtnListener(new SearchBtnListener());
	}

	/* -------------- Listeners --------------------- */
	/**
	 * Listener for the search button. Opens a dialog when the button is
	 * clicked.
	 * 
	 * @author CKeil
	 *
	 */
	private class SearchBtnListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			/*
			 * Putting the mapContainer objects in DendroView so that I can
			 * control zoom-out of the found genes/arrays are outside the
			 * visible area.
			 */
			dendroView.setGlobalXMap(globalXmap);
			dendroView.setGlobalYMap(globalYmap);
			/*
			 * Adding the mapContainer objects here so that the search dialog
			 * can determine if results are visible in order to be able to
			 * determine whether to zoom out.
			 */
			deselectAll();
			dendroView.searchLabels();
		}
	}

	private void resetDendroView() {

		resetMapContainers();

		reZoomVisible();
		reCenterVisible();
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

		if (atr_loc > 0.0 || gtr_loc > 0.0) {

			/* First save current setup */
			configNode.putDouble("atr_Loc", atr_loc);
			configNode.putDouble("gtr_Loc", gtr_loc);

			/* Shrink tree panel to 0 to make it invisible */
			atr_loc = 0.0;
			gtr_loc = 0.0;

		} else {
			atr_loc = configNode.getDouble("atr_Loc", 0.5);
			gtr_loc = configNode.getDouble("gtr_Loc", 0.5);
		}

		dendroView.setTreeVisibility(atr_loc, gtr_loc);
	}

	/* Action to deselect everything */
	private class SearchLabelAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			toggleSearch();
		}
	}

	/**
	 * Determines mouse behavior over the close-x icon of search panel.
	 * 
	 * @author chris0689
	 *
	 */
	private class CloseSearchAction extends MouseAdapter implements
			ActionListener {

		@Override
		public void mouseEntered(final MouseEvent e) {

			dendroView.getCloseSearchBtn().setCursor(
					new Cursor(Cursor.HAND_CURSOR));
			dendroView
					.getCloseSearchBtn()
					.setBorder(
							BorderFactory
									.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

		}

		@Override
		public void mouseExited(final MouseEvent e) {

			dendroView.getCloseSearchBtn().setCursor(
					new Cursor(Cursor.DEFAULT_CURSOR));
			dendroView.getCloseSearchBtn().setBorder(null);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {

			toggleSearch();
		}

	}

	/* >>>>>>> Mapped Key Actions <<<<<<<<< */
	/* TODO make all this key-scroll code more compact... */
	/** Action to scroll the y-axis to top. */
	private class HomeKeyYAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			getGlobalYMap().scrollToIndex(0);
			getGlobalYMap().notifyObservers();
		}
	}

	/** Action to scroll the y-axis to bottom. */
	private class EndKeyYAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int max = getGlobalYMap().getMaxIndex();
			getGlobalYMap().scrollToIndex(max);
			getGlobalYMap().notifyObservers();
		}
	}

	/** Action to scroll the y-axis to top. */
	private class HomeKeyXAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			getGlobalXMap().scrollToIndex(0);
			getGlobalXMap().notifyObservers();
		}
	}

	/** Action to scroll the y-axis to bottom. */
	private class EndKeyXAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int max = getGlobalXMap().getMaxIndex();
			getGlobalXMap().scrollToIndex(max);
			getGlobalXMap().notifyObservers();
		}
	}

	private class PageUpYAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int scrollBy = getGlobalYMap().getNumVisible();
			getGlobalYMap().scrollBy(-scrollBy);
			getGlobalYMap().notifyObservers();
		}
	}

	private class PageDownYAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int scrollBy = getGlobalYMap().getNumVisible();
			getGlobalYMap().scrollBy(scrollBy);
			getGlobalYMap().notifyObservers();
		}
	}

	private class PageUpXAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int scrollBy = getGlobalXMap().getNumVisible();
			getGlobalXMap().scrollBy(-scrollBy);
			getGlobalXMap().notifyObservers();
		}
	}

	private class PageDownXAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int scrollBy = getGlobalXMap().getNumVisible();
			getGlobalXMap().scrollBy(scrollBy);
			getGlobalXMap().notifyObservers();
		}
	}

	private class ArrowLeftAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			getGlobalXMap().scrollBy(-1);
			getGlobalXMap().notifyObservers();
		}
	}

	private class ArrowRightAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			getGlobalXMap().scrollBy(1);
			getGlobalXMap().notifyObservers();
		}
	}

	private class ArrowUpAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			getGlobalYMap().scrollBy(-1);
			getGlobalYMap().notifyObservers();
		}
	}

	private class ArrowDownAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			getGlobalYMap().scrollBy(1);
			getGlobalYMap().notifyObservers();
		}
	}

	/** Action to deselect everything */
	private class DeselectAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			deselectAll();
		}
	}

	/**
	 * Zooms into the selected area
	 */
	private class ZoomAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			zoomSelection();
			centerSelection();
		}
	}

	/** Zooms into GlobalView by 1 scale step (depends on previous scale). */
	private class ZoomInAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			getGlobalXMap().zoomIn();
			getGlobalYMap().zoomIn();

			getGlobalXMap().notifyObservers();
			getGlobalYMap().notifyObservers();
		}
	}

	/** Zooms out of GlobalView by 1 scale step (depends on previous scale). */
	private class ZoomOutAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			getGlobalXMap().zoomOut();
			getGlobalYMap().zoomOut();

			getGlobalXMap().notifyObservers();
			getGlobalYMap().notifyObservers();
		}
	}

	/** Resets the GlobalView to all zoomed-out state */
	private class HomeAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			resetMapContainers();
		}
	}

	/* >>>>>>> Component Listeners <<<<<<<<< */
	/**
	 * Listener for the setScale-buttons in DendroView. Changes the scale in
	 * xMap and yMap MapContainers, allowing the user to zoom in or out of each
	 * individual axis in GlobalView.
	 *
	 * @author CKeil
	 *
	 */
	class ScaleListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			if (e.getSource() == dendroView.getXPlusButton()) {
				getGlobalXMap().zoomIn();

			} else if (e.getSource() == dendroView.getXMinusButton()) {
				getGlobalXMap().zoomOut();

			} else if (e.getSource() == dendroView.getXYMinusButton()) {
				getGlobalXMap().zoomOut();
				getGlobalYMap().zoomOut();

			} else if (e.getSource() == dendroView.getXYPlusButton()) {
				getGlobalXMap().zoomIn();
				getGlobalYMap().zoomIn();

			} else if (e.getSource() == dendroView.getYPlusButton()) {
				getGlobalYMap().zoomIn();

			} else if (e.getSource() == dendroView.getYMinusButton()) {
				getGlobalYMap().zoomOut();

			} else if (e.getSource() == dendroView.getHomeButton()) {
				resetMapContainers();

			} else {
				LogBuffer.println("Got weird source for actionPerformed() "
						+ "in DendroController ScaleListener.");
			}

			getGlobalXMap().notifyObservers();
			getGlobalYMap().notifyObservers();
		}
	}

	/**
	 * Deselects all of both axes' current selections.
	 */
	public void deselectAll() {

		if (arraySelection.getNSelectedIndexes() > 0) {
			arraySelection.deselectAllIndexes();
			geneSelection.deselectAllIndexes();

			arraySelection.notifyObservers();
			geneSelection.notifyObservers();
		}
	}

	/**
	 * Sets the dimensions of the GlobalView axes. There are three options,
	 * passed from the MenuBar when the user selects it. Fill: This fills all of
	 * the available space on the screen with the matrix. Equal: Both axes are
	 * equally sized, forming a square matrix. Proportional: Axes are sized in
	 * proportion to how many elements they show.
	 * 
	 * @param mode
	 */
	public void setMatrixSize(final int mode) {

		switch (mode) {

		case GlobalView.FILL:
			setMatrixFill();
			break;

		case GlobalView.EQUAL:
			setMatrixAxesEqual();
			break;

		case GlobalView.PROPORT:
			setMatrixPropotional();
			break;

		default:
			setMatrixFill();
			break;
		}

		dendroView.resetMatrixSize();
		;
		addViewListeners();
		resetMapContainers();
	}

	/**
	 * Sets axis dimensions of heat map to their maximum values.
	 */
	private void setMatrixFill() {

		dendroView.setGVWidth(DendroView.MAX_GV_WIDTH);
		dendroView.setGVHeight(DendroView.MAX_GV_HEIGHT);
	}

	/**
	 * Sets the size of each GlobalView axes to the smallest of both to form a
	 * square.
	 */
	private void setMatrixAxesEqual() {

		/* Get GlobalView dimensions */
		final double absGVWidth = dendroView.getGlobalView().getWidth();
		final double absGVHeight = dendroView.getGlobalView().getHeight();

		if (!Helper.nearlyEqual(absGVWidth, absGVHeight)) {

			/*
			 * Depends on app frame size (what if app used on a screen with
			 * larger height than width etc...)
			 */
			final int screen_width = tvFrame.getAppFrame().getWidth();
			final int screen_height = tvFrame.getAppFrame().getHeight();

			/* Make sure the axis with the smallest screen side is maximized */
			if (screen_height < screen_width) {
				final double newWidth = calcAxisDimension(absGVWidth,
						absGVHeight, DendroView.MAX_GV_WIDTH);

				dendroView.setGVWidth(newWidth);

			} else {
				final double newHeight = calcAxisDimension(absGVHeight,
						absGVWidth, DendroView.MAX_GV_HEIGHT);

				dendroView.setGVHeight(newHeight);
			}
		}
	}

	private double calcAxisDimension(final double big, final double small,
			final double max) {

		final double percentDiff = small / big;

		double newAxis = percentDiff * max;

		// rounding
		newAxis *= 1000;
		newAxis = Math.round(newAxis);
		newAxis /= 1000;

		return newAxis;
	}

	/**
	 * Resizes the matrix such that it fits all pixels as squares. If gvWidth or
	 * gvHeight would go below a certain size, the matrix is adjusted such that
	 * the content remains viewable in a meaningful manner.
	 */
	private void setMatrixPropotional() {

		// Condition: All pixels must be shown, but must have same scale.

		final double xScale = globalXmap.getScale();
		final double yScale = globalYmap.getScale();

		if (xScale >= yScale) {
			globalXmap.setScale(yScale);

			final double used = globalXmap.getUsedPixels();
			final double avail = globalXmap.getAvailablePixels();

			final double percentDiff = used / avail;

			double newWidth = DendroView.MAX_GV_WIDTH * percentDiff;
			// rounding
			newWidth = newWidth * 1000;
			newWidth = Math.round(newWidth);
			newWidth = newWidth / 1000;

			dendroView.setGVWidth(newWidth);

		} else {
			globalYmap.setScale(xScale);

			final double used = globalYmap.getUsedPixels();
			final double avail = globalYmap.getAvailablePixels();

			final double percentDiff = used / avail;

			double newHeight = DendroView.MAX_GV_HEIGHT * percentDiff;
			// rounding
			newHeight = newHeight * 1000;
			newHeight = Math.round(newHeight);
			newHeight = newHeight / 1000;

			dendroView.setGVHeight(newHeight);
		}
	}

	/**
	 * Toggles the search bars in DendroView.
	 */
	public void toggleSearch() {

		dendroView.setShowSearch();
		resetDendroView();
	}

	public void saveSettings() {

		try {
			if (configNode.nodeExists("GlobalXMap") && globalXmap != null) {
				configNode.node("GlobalXMap").putDouble("scale",
						globalXmap.getScale());
				configNode.node("GlobalXMap").putInt("XScrollValue",
						dendroView.getXScroll().getValue());
			}

			if (configNode.nodeExists("GlobalYMap") && globalYmap != null) {
				configNode.node("GlobalYMap").putDouble("scale",
						globalYmap.getScale());
				configNode.node("GlobalYMap").putInt("YScrollValue",
						dendroView.getYScroll().getValue());
			}
		} catch (final BackingStoreException e) {
			e.printStackTrace();
		}
	}

	public void setSavedScale() {

		try {
			if (configNode.nodeExists("GlobalXMap") && globalXmap != null) {
				globalXmap.setLastScale();
				dendroView.setXScroll(configNode.node("GlobalXMap").getInt(
						"XScrollValue", 0));
			}

			if (configNode.nodeExists("GlobalYMap") && globalYmap != null) {
				globalYmap.setLastScale();
				dendroView.setYScroll(configNode.node("GlobalYMap").getInt(
						"YScrollValue", 0));
			}

		} catch (final BackingStoreException e) {
			LogBuffer.logException(e);
		}
	}

	/**
	 * The Zoom listener which allows the user to zoom into a selection.
	 *
	 * @author CKeil
	 *
	 */
	private class ZoomListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			zoomSelection();
			centerSelection();

			// LogBuffer.println("globalXmap.getFirstVisible(): [" +
			// globalXmap.getFirstVisible() + "] " +
			// "globalXmap.getNumVisible(): [" + globalXmap.getNumVisible() +
			// "].  " +
			// "globalXmap.getScroll().getValue(): [" +
			// globalXmap.getScroll().getValue() + "] " +
			// "globalXmap.getScroll().getVisibleAmount(): [" +
			// globalXmap.getScroll().getVisibleAmount() + "].");
			// LogBuffer.println("globalYmap.getFirstVisible(): [" +
			// globalYmap.getFirstVisible() + "] " +
			// "globalYmap.getNumVisible(): [" + globalYmap.getNumVisible() +
			// "].  " +
			// "globalYmap.getScroll().getValue(): [" +
			// globalYmap.getScroll().getValue() + "] " +
			// "globalYmap.getScroll().getVisibleAmount(): [" +
			// globalYmap.getScroll().getVisibleAmount() + "].");
		}
	}

	/**
	 * Uses the array- and geneSelection and currently available pixels on
	 * screen retrieved from the MapContainer objects to calculate a new scale
	 * and zoom in on it by working in conjunction with centerSelection().
	 */
	public void zoomSelection() {

		double newScale = 0.0;
		double newScale2 = 0.0;

		// Declare the min number of spots to zoom in on for each dimension.
		// We should set this as a preference in the future. -Rob
		final double minZoomIndex = 20;
		double minArrayZoomIndex = minZoomIndex;
		double minGeneZoomIndex = minZoomIndex;

		// Determine the boundaries of the data (so that we do not exceed them)
		final double maxArrayIndex = globalXmap.getMaxIndex();
		final double maxGeneIndex = globalYmap.getMaxIndex();

		// Make sure our zoom limits have not exceeded the boundaries of the
		// data (if the data matrix is really small)
		if (maxArrayIndex < minArrayZoomIndex) {
			minArrayZoomIndex = maxArrayIndex;
		}
		if (maxGeneIndex < minGeneZoomIndex) {
			minGeneZoomIndex = maxGeneIndex;
		}

		// We'll allow the user to surpass the min zoom index when they are near
		// the edge, so that
		// their selection is centered on the screen, so let's get the edges of
		// the selection
		final double minSelectedArrayIndex = arraySelection.getMinIndex();
		final double minSelectedGeneIndex = geneSelection.getMinIndex();

		// Obtain the selection size of each dimension
		double arrayIndexes = arraySelection.getMaxIndex()
				- minSelectedArrayIndex + 1;
		double geneIndexes = geneSelection.getMaxIndex() - minSelectedGeneIndex
				+ 1;

		// If the array selection is smaller than the minimum zoom level
		if (arrayIndexes < minArrayZoomIndex) {

			// If the center of the selection is less than half the distance to
			// the near edge
			if ((minSelectedArrayIndex + arrayIndexes / 2) < (minArrayZoomIndex / 2)) {
				arrayIndexes = (minSelectedArrayIndex + arrayIndexes / 2) * 2;
			}
			// Else if the center of the selection is less than half the
			// distance to the far edge
			else if ((minSelectedArrayIndex + arrayIndexes / 2) > (maxArrayIndex - (minArrayZoomIndex / 2))) {
				arrayIndexes = (maxArrayIndex - (minSelectedArrayIndex
						+ arrayIndexes / 2 - 1)) * 2;
			}
			// Otherwise, set the standard minimum zoom
			else {
				arrayIndexes = minArrayZoomIndex;
			}
		}

		// If the gene selection is smaller than the minimum zoom level
		if (geneIndexes < minGeneZoomIndex) {

			// If the center of the selection is less than half the distance to
			// the near edge
			if ((minSelectedGeneIndex + geneIndexes / 2) < (minGeneZoomIndex / 2)) {
				geneIndexes = (minSelectedGeneIndex + geneIndexes / 2) * 2;
			}
			// Else if the center of the selection is less than half the
			// distance to the far edge
			else if ((minSelectedGeneIndex + geneIndexes / 2) > (maxGeneIndex - (minGeneZoomIndex / 2))) {
				geneIndexes = (maxGeneIndex - (minSelectedGeneIndex
						+ geneIndexes / 2 - 1)) * 2;
			}
			// Otherwise, set the standard minimum zoom
			else {
				geneIndexes = minGeneZoomIndex;
			}
		}

		if (arrayIndexes > 0 && geneIndexes > 0) {
			newScale = (globalXmap.getAvailablePixels()) / arrayIndexes;

			// if (newScale < globalXmap.getMinScale()) {
			// newScale = globalXmap.getMinScale();
			// }
			// Changed setScale to use numVisible, so I moved the call below
			// where numVisible was changed
			// globalXmap.setScale(newScale);

			// LogBuffer.println("Setting numVisible for arrays to round of double ["
			// + arrayIndexes + "].");

			// Track explicitly manipulated visible area (instead of the visible
			// area) as
			// is manipulated via indirect actions (such as resizing the window)
			final int numArrayIndexes = (int) Math.round(arrayIndexes);
			globalXmap.setNumVisible(numArrayIndexes);

			globalXmap.setScale(newScale);

			globalXmap.notifyObservers();

			newScale2 = (globalYmap.getAvailablePixels()) / geneIndexes;

			// LogBuffer.println("Zooming. MinSelectedArrayIndex: [" +
			// minSelectedArrayIndex + "] " +
			// "MinSelectedGeneIndex: [" + minSelectedGeneIndex + "] " +
			// "ArrayIndexesSelected: [" + arrayIndexes + "] " +
			// "GeneIndexesSelected: [" + geneIndexes + "] " +
			// "xscale: [" + newScale + "] " +
			// "yscale: [" + newScale2 + "].");

			// if (newScale2 < globalYmap.getMinScale()) {
			// newScale2 = globalYmap.getMinScale();
			// }
			// Changed setScale to use numVisible, so I moved the call below
			// where numVisible was changed
			// globalYmap.setScale(newScale2);

			// LogBuffer.println("Setting numVisible for genes to round of double ["
			// + geneIndexes + "].");

			// Track explicitly manipulated visible area (instead of the visible
			// area) as
			// is manipulated via indirect actions (such as resizing the window)
			final int numGeneIndexes = (int) Math.round(geneIndexes);
			globalYmap.setNumVisible(numGeneIndexes);

			globalYmap.setScale(newScale2);

			globalYmap.notifyObservers();
		}

		saveSettings();
	}

	/**
	 * Scrolls to the center of the selected rectangle
	 */
	public void centerSelection() {

		int scrollX;
		int scrollY;

		final int[] selectedGenes = geneSelection.getSelectedIndexes();
		final int[] selectedArrays = arraySelection.getSelectedIndexes();

		if (selectedGenes.length > 0 && selectedArrays.length > 0) {

			final double endX = selectedArrays[selectedArrays.length - 1];
			final double endY = selectedGenes[selectedGenes.length - 1];

			final double startX = selectedArrays[0];
			final double startY = selectedGenes[0];

			scrollX = (int) Math.round((endX + startX) / 2);
			scrollY = (int) Math.round((endY + startY) / 2);

			// LogBuffer.println("Scrolling to selected indexes: [" + startX +
			// "-" + endX + "," + startY + "-" + endY + "] with centers [" +
			// scrollX + "," + scrollY + "].");
			globalXmap.scrollToIndex(scrollX);
			globalYmap.scrollToIndex(scrollY);

			// Calculate the first visible data index in both dimensions
			int firstX = 0;
			while (firstX < globalXmap.getMaxIndex()
					&& !globalXmap.isVisible(firstX)) {
				firstX++;
			}

			int firstY = 0;
			while (firstY < globalYmap.getMaxIndex()
					&& !globalYmap.isVisible(firstY)) {
				firstY++;
			}

			// Track explicitly manipulated visible area (instead of the visible
			// area) as
			// is manipulated via indirect actions (such as resizing the window)
			globalXmap.setFirstVisible(firstX);
			globalYmap.setFirstVisible(firstY);

			globalXmap.notifyObservers();
			globalYmap.notifyObservers();
		}
	}

	/**
	 * Uses the currently visible data indexes on the screen to update the scale
	 * and zoom in conjunction with centerSelection() to handle window resize
	 * events. Based on zoomSelection(). Does not change the number of visible
	 * data indexes.
	 */
	public void reZoomVisible() {

		double newScale = 0.0;
		double newScale2 = 0.0;

		// Obtain the selection size of each dimension
		// double arrayIndexes = globalXmap.getTileNumVisible();
		// double geneIndexes = globalYmap.getTileNumVisible();
		double arrayIndexes = globalXmap.getNumVisible();
		double geneIndexes = globalYmap.getNumVisible();

		if (arrayIndexes == 0
				|| geneIndexes == 0
				|| (arrayIndexes == globalXmap.getMaxIndex() && geneIndexes == globalYmap
						.getMaxIndex())) {
			// LogBuffer.println("No spots are visible. Resetting view.");
			arrayIndexes = globalXmap.getMaxIndex() + 1;
			geneIndexes = globalYmap.getMaxIndex() + 1;
			resetMapContainers();
		} else {
			// LogBuffer.println("pixels / array indexes visible: [" +
			// globalXmap.getAvailablePixels() + "/" + arrayIndexes +
			// "] gene indexes visible: [" + globalYmap.getAvailablePixels() +
			// "/" + geneIndexes + "].");
			newScale = (globalXmap.getAvailablePixels()) / arrayIndexes;
			// LogBuffer.println("reZoomVisible: numVisible: [" + arrayIndexes +
			// "] is being used in calculations for new scale values: [" +
			// newScale + "].  They cannot be less than the minscale: [" +
			// globalXmap.getMinScale() + "]");

			// if (newScale < globalXmap.getMinScale()) {
			// newScale = globalXmap.getMinScale();
			// }
			globalXmap.setScale(newScale);
			globalXmap.notifyObservers();

			newScale2 = (globalYmap.getAvailablePixels()) / geneIndexes;
			// LogBuffer.println("reZoomVisible: numVisible: [" + geneIndexes +
			// "] is being used in calculations for new scale values: [" +
			// newScale2 + "].  They cannot be less than the minscale: [" +
			// globalYmap.getMinScale() + "]");

			// if (newScale2 < globalYmap.getMinScale()) {
			// newScale2 = globalYmap.getMinScale();
			// }
			globalYmap.setScale(newScale2);
			globalYmap.notifyObservers();
			dendroView.getGlobalView().repaint();
		}

		saveSettings();
	}

	/**
	 * Scrolls to the center of the visible rectangle. Used when the window or
	 * the image area is resized in order to keep the same data displayed.
	 */
	public void reCenterVisible() {

		// final int visibleGenes = globalYmap.getTileNumVisible();
		// final int visibleArrays = globalXmap.getTileNumVisible();
		final int visibleGenes = globalYmap.getNumVisible();
		final int visibleArrays = globalXmap.getNumVisible();

		if (visibleGenes > 0 && visibleArrays > 0) {

			final int startX = globalXmap.getFirstVisible();
			final int startY = globalYmap.getFirstVisible();

			// LogBuffer.println("Firstx visible: [" + startX +
			// "] Firsty visible: [" + startY + "].");

			globalXmap.scrollToFirstIndex(startX);
			globalXmap.notifyObservers();

			globalYmap.scrollToFirstIndex(startY);
			globalYmap.notifyObservers();
		}

		saveSettings();
	}

	/**
	 * Listens to the resizing of DendroView2 and makes changes to MapContainers
	 * as a result.
	 *
	 * @author CKeil
	 *
	 */
	class ResizeListener implements ComponentListener {

		// Component Listeners
		@Override
		public void componentHidden(final ComponentEvent arg0) {
		}

		@Override
		public void componentMoved(final ComponentEvent arg0) {
		}

		@Override
		public void componentResized(final ComponentEvent arg0) {
			// LogBuffer.println("componentResized: globalYmap.getTileNumVisible: ["
			// + globalYmap.getTileNumVisible() +
			// "] globalXmap.getTileNumVisible: [" +
			// globalXmap.getTileNumVisible() +
			// "] dendroView.getXScroll().getValue(): [" +
			// dendroView.getXScroll().getValue() +
			// "] dendroView.getYScroll().getValue(): [" +
			// dendroView.getYScroll().getValue() + "].");

			// Previously, resetMapContainers was called here, but that caused
			// the zoom level to change when the user resized the window, so I
			// added a way to track the currently visible area in mapContainer
			// and implemented these functions to make the necessary
			// adjustments to the image when that happens
			reZoomVisible();
			reCenterVisible();
		}

		@Override
		public void componentShown(final ComponentEvent arg0) {
		}
	}

	public void saveImage(final JPanel panel) throws IOException {

		File saveFile = new File("savedImage.png");

		final JFileChooser fc = new JFileChooser();

		fc.setFileFilter(new FileNameExtensionFilter("PNG", "png"));
		fc.setSelectedFile(saveFile);
		final int returnVal = fc.showSaveDialog(dendroView.getDendroPane());

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
	 * Checks whether there is a configuration node for the current model and
	 * DendroView. If not it creates one.
	 */
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			if (tvModel.getDocumentConfigRoot() != null) {
				configNode = ((TVModel) tvModel).getDocumentConfig();

			} else {
				configNode = Preferences.userRoot().node("DendroView");
			}
		}
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

		if (tvModel.getArrayHeaderInfo().getIndex("GROUP") != -1) {
			final HeaderInfo headerInfo = tvModel.getArrayHeaderInfo();
			final int groupIndex = headerInfo.getIndex("GROUP");

			arrayIndex = getGroupVector(headerInfo, groupIndex);

		} else {
			arrayIndex = null;
		}

		if (tvModel.getGeneHeaderInfo().getIndex("GROUP") != -1) {
			System.err.println("got gene group header");
			final HeaderInfo headerInfo = tvModel.getGeneHeaderInfo();
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
	private int[] getGroupVector(final HeaderInfo headerInfo,
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
	public void bindComponentFunctions() {

		// Handle selection
		if (geneIndex != null) {
			setGeneSelection(new ReorderedTreeSelection(
					tvFrame.getGeneSelection(), geneIndex));

		} else {
			setGeneSelection(tvFrame.getGeneSelection());
		}

		if (arrayIndex != null) {
			setArraySelection(new ReorderedTreeSelection(
					tvFrame.getArraySelection(), arrayIndex));

		} else {
			setArraySelection(tvFrame.getArraySelection());
		}

		final ColorPresets colorPresets = DendrogramFactory.getColorPresets();
		colorPresets.setConfigNode(configNode);
		colorExtractor = new ColorExtractor();
		colorExtractor.setDefaultColorSet(colorPresets.getDefaultColorSet());
		colorExtractor.setMissing(DataModel.NODATA, DataModel.EMPTY);

		final DoubleArrayDrawer dArrayDrawer = new DoubleArrayDrawer();
		dArrayDrawer.setColorExtractor(colorExtractor);
		arrayDrawer = dArrayDrawer;
		((TVModel) tvModel).addObserver(arrayDrawer);

		// set data first to avoid adding auto-generated
		// contrast to documentConfig.
		dArrayDrawer.setDataMatrix(tvModel.getDataMatrix());
		dArrayDrawer.recalculateContrast();
		dArrayDrawer.setConfigNode("ArrayDrawer1");

		// globalmaps tell globalview, atrview, and gtrview
		// where to draw each data point.
		// the scrollbars "scroll" by communicating with the maps.
		setMapContainers();

		globalXmap.setScrollbar(dendroView.getXScroll());
		globalYmap.setScrollbar(dendroView.getYScroll());

		// Drawers
		dendroView.getGlobalView().setArrayDrawer(arrayDrawer);

		leftTreeDrawer = new TreePainter();
		dendroView.getRowTreeView().setTreeDrawer(leftTreeDrawer);

		invertedTreeDrawer = new TreePainter();
		dendroView.getColumnTreeView().setTreeDrawer(invertedTreeDrawer);

		setPresets();

		// this is here because my only subclass shares this code.
		bindTrees();

		// perhaps I could remember this stuff in the MapContainer...
		globalXmap.setIndexRange(0, tvModel.getDataMatrix().getNumCol() - 1);
		globalYmap.setIndexRange(0, tvModel.getDataMatrix().getNumRow() - 1);
	}

	/**
	 * Connects all sub components with DendroView's configuration node, so that
	 * the hierarchical structure of Java's Preferences API can be followed.
	 */
	public void setPresets() {

		globalXmap.setConfigNode(configNode);// getFirst("GlobalXMap"));
		globalYmap.setConfigNode(configNode);// getFirst("GlobalYMap"));

		// URLs
		colorExtractor.setConfigNode(configNode);// getFirst("ColorExtractor"));

		dendroView.getRowLabelView().setConfigNode(configNode);// getFirst("TextView"));
		dendroView.getColumnLabelView().setConfigNode(configNode);// getFirst("ArrayNameView"));
		dendroView.getColumnTreeView().getHeaderSummary()
				.setConfigNode(configNode);
		dendroView.getRowTreeView().getHeaderSummary()
				.setConfigNode(configNode);
	}

	/**
	 * Sets up the views with the MapContainers.
	 */
	public void setMapContainers() {

		dendroView.getColumnTreeView().setMap(globalXmap);
		dendroView.getColumnLabelView().setMap(globalXmap);
		dendroView.getRowTreeView().setMap(globalYmap);
		dendroView.getRowLabelView().setMap(globalYmap);

		dendroView.getGlobalView().setXMap(globalXmap);
		dendroView.getGlobalView().setYMap(globalYmap);
	}

	/**
	 * Updates all headerInfo instances for all the view components.
	 */
	public void updateHeaderInfo() {

		dendroView.getColumnTreeView().setATRHeaderInfo(
				tvModel.getAtrHeaderInfo());
		dendroView.getRowTreeView()
				.setGTRHeaderInfo(tvModel.getGtrHeaderInfo());
		dendroView.getColumnLabelView().setHeaderInfo(
				tvModel.getArrayHeaderInfo());
		dendroView.getRowLabelView().setHeaderInfo(tvModel.getGeneHeaderInfo());
		dendroView.getGlobalView().setHeaders(tvModel.getGeneHeaderInfo(),
				tvModel.getArrayHeaderInfo());
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

		arraySelection.resize(tvModel.getDataMatrix().getNumCol());
		arraySelection.notifyObservers();

		globalXmap.setIndexRange(0, tvModel.getDataMatrix().getNumCol() - 1);
		globalXmap.notifyObservers();

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
					tvModel.getArrayHeaderInfo());
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

		arraySelection.setSelectedNode(arrayNode.getId());
		dendroView.getColumnTreeView().setSelectedNode(arrayNode);

		arraySelection.notifyObservers();
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

		FileSet fileSet1; // will be chosen...

		final JFileChooser fileDialog = new JFileChooser();
		setupATRFileDialog(fileDialog);

		final int retVal = fileDialog
				.showOpenDialog(dendroView.getDendroPane());

		if (retVal == JFileChooser.APPROVE_OPTION) {
			final File chosen = fileDialog.getSelectedFile();
			fileSet1 = new FileSet(chosen.getName(), chosen.getParent()
					+ File.separator);

		} else
			throw new LoadException("File Dialog closed without selection...",
					LoadException.NOFILE);

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
				selectedID = arraySelection.getSelectedNode();

			} catch (final NullPointerException npe) {
				npe.printStackTrace();
			}

			int[] ordering;
			ordering = AtrAligner.align(tvModel.getAtrHeaderInfo(),
					tvModel.getArrayHeaderInfo(), model.getAtrHeaderInfo(),
					model.getArrayHeaderInfo());

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
	 * I factored it out because it is common betwen DendroView and
	 * KnnDendroView.
	 */
	protected void bindTrees() {

		if ((tvModel != null) && tvModel.aidFound()) {
			try {
				dendroView.getColumnTreeView().setEnabled(true);

				invertedTreeDrawer.setData(tvModel.getAtrHeaderInfo(),
						tvModel.getArrayHeaderInfo());
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
						tvModel.getGeneHeaderInfo());
				final HeaderInfo gtrHeaderInfo = tvModel.getGtrHeaderInfo();

				if (gtrHeaderInfo.getIndex("NODECOLOR") >= 0) {
					TreeColorer.colorUsingHeader(leftTreeDrawer.getRootNode(),
							tvModel.getGtrHeaderInfo(),
							gtrHeaderInfo.getIndex("NODECOLOR"));

				} else {
					TreeColorer.colorUsingLeaf(leftTreeDrawer.getRootNode(),
							tvModel.getGeneHeaderInfo(), tvModel
							.getGeneHeaderInfo().getIndex("FGCOLOR"));
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
	 * Scrolls to index i in the Y-MapContainer
	 *
	 * @param i
	 */
	public void scrollToGene(final int i) {

		getGlobalYMap().scrollToIndex(i);
		getGlobalYMap().notifyObservers();
	}

	/**
	 * Scrolls to index i in the X-MapContainer.
	 *
	 * @param i
	 */
	public void scrollToArray(final int i) {

		getGlobalXMap().scrollToIndex(i);
		getGlobalXMap().notifyObservers();
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
	 * @param arraySelection
	 */
	public void setArraySelection(final TreeSelectionI arraySelection) {

		if (this.arraySelection != null) {
			this.arraySelection.deleteObserver(dendroView);
		}

		this.arraySelection = arraySelection;
		arraySelection.addObserver(dendroView);

		dendroView.getGlobalView().setArraySelection(arraySelection);
		dendroView.getColumnTreeView().setTreeSelection(arraySelection);
		dendroView.getRowLabelView().setArraySelection(arraySelection);
		dendroView.getColumnLabelView().setArraySelection(arraySelection);
	}

	/**
	 * This should be called after setDataModel has been set to the appropriate
	 * model
	 *
	 * @param geneSelection
	 */
	public void setGeneSelection(final TreeSelectionI geneSelection) {

		if (this.geneSelection != null) {
			this.geneSelection.deleteObserver(dendroView);
		}

		this.geneSelection = geneSelection;
		geneSelection.addObserver(dendroView);

		dendroView.getGlobalView().setGeneSelection(geneSelection);
		dendroView.getRowTreeView().setTreeSelection(geneSelection);
		dendroView.getRowLabelView().setGeneSelection(geneSelection);
		dendroView.getColumnLabelView().setGeneSelection(geneSelection);
	}

	public void setNewIncluded(final int[] gIncluded, final int[] aIncluded) {

		dendroView.getRowLabelView().getHeaderSummary().setIncluded(gIncluded);
		dendroView.getColumnLabelView().getHeaderSummary()
				.setIncluded(aIncluded);
	}

	public int[] getArrayIncluded() {

		return dendroView.getColumnLabelView().getHeaderSummary().getIncluded();
	}

	public int[] getGeneIncluded() {

		return dendroView.getRowLabelView().getHeaderSummary().getIncluded();
	}

	public boolean hasDendroView() {

		return dendroView != null;
	}

	// Getters for fields
	public ArrayDrawer getArrayDrawer() {

		return arrayDrawer;
	}

	public MapContainer getGlobalXMap() {

		return globalXmap;
	}

	public MapContainer getGlobalYMap() {

		return globalYmap;
	}
}
