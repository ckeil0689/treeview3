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
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
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
import edu.stanford.genetics.treeview.plugin.dendroview.InteractiveMatrixView;
import edu.stanford.genetics.treeview.plugin.dendroview.LabelContextMenu;
import edu.stanford.genetics.treeview.plugin.dendroview.LabelContextMenuController;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;
import edu.stanford.genetics.treeview.plugin.dendroview.MatrixView;
import edu.stanford.genetics.treeview.plugin.dendroview.TreeColorer;
import edu.stanford.genetics.treeview.plugin.dendroview.TreePainter;

/* TODO separate some parts into dedicated GlobalView controller */
/**
 * Controller class handling UI input and calculations related to DendroView.
 *
 * @author chris0689
 *
 */
public class DendroController implements ConfigNodePersistent, Observer {

	private DendroView dendroView;
	private final TreeViewFrame tvFrame;
	private final TVController tvController;
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

	// Color Extractor
	private ColorExtractor colorExtractor;

	public DendroController(final TreeViewFrame tvFrame,
			final TVController tvController) {

		this.tvFrame = tvFrame;
		this.tvController = tvController;

		interactiveXmap = new MapContainer("Fixed", "GlobalXMap");
		interactiveYmap = new MapContainer("Fixed", "GlobalYMap");

		globalXmap = new MapContainer("Fixed", "OverviewXMap");
		globalYmap = new MapContainer("Fixed", "OverviewYMap");
	}

	public void setNewMatrix(final DendroView dendroView,
			final DataModel tvModel) {

		this.dendroView = dendroView;
		this.tvModel = tvModel;

		/* Get the saved settings */
		setConfigNode(tvFrame.getConfigNode());

		updateHeaderInfo();
		bindComponentFunctions();

		dendroView
				.setupSearch(tvModel.getRowHeaderInfo(),
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

		/*
		 * Needs to wait for repaint() called from resetMapContainer() and
		 * component listener. TODO implement resetMapContainer/ setSavedScale
		 * differently...
		 */
		resetMatrixViews();

		addKeyBindings();
		addDendroViewListeners();
	}

	/** Adds all keyboard shortcuts that can be used with DendroView open. */
	private void addKeyBindings() {

		final InputMap input_map = dendroView.getInputMap();
		final ActionMap action_map = dendroView.getActionMap();

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

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, modifier),
				"zoomSelection");
		action_map.put("zoomSelection", new ZoomAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, modifier),
				"resetZoom");
		action_map.put("resetZoom", new HomeAction());
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
	 * Recalculates proportions for the MapContainers, when the layout was
	 * changed by removing or adding components, or resizing the TVFrame. Only
	 * works if GlobalView is already resized (has availablePixels set to new
	 * value!).
	 */
	public void resetMatrixViews() {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				dendroView.setMatrixHome();
			}
		});
	}

	/**
	 * Adds listeners to DendroView's UI components.
	 */
	private void addDendroViewListeners() {

		dendroView.addScaleListeners(new ScaleListener());
		dendroView.addZoomListener(new ZoomListener());
		dendroView.addDividerListener(new DividerListener());
		dendroView.addSplitPaneListener(new SplitPaneListener());
		dendroView.addResizeListener(new AppFrameListener());
		dendroView.addDeselectClickListener(new PanelClickDeselector());
	}

	/* -------------- Listeners --------------------- */
	/**
	 * When mouse click happens on dendroPane in DendroView, everything will be
	 * deselected.
	 * 
	 * @author chris0689
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

	/* Action to deselect everything */
	private class SearchLabelAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			dendroView.setRowFinderBoxFocused();
		}
	}

	private class DividerListener implements PropertyChangeListener {

		@Override
		public void propertyChange(final PropertyChangeEvent evt) {

			dendroView.updateTreeMenuBtn((JSplitPane) evt.getSource());
		}
	}

	/* >>>>>>> Mapped Key Actions <<<<<<<<< */
	/* TODO make all this key-scroll code more compact... */
	/** Action to scroll the y-axis to top. */
	private class HomeKeyYAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			getInteractiveYMap().scrollToIndex(0);
		}
	}

	/** Action to scroll the y-axis to bottom. */
	private class EndKeyYAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int max = getInteractiveYMap().getMaxIndex();
			getInteractiveYMap().scrollToIndex(max);
		}
	}

	/** Action to scroll the y-axis to top. */
	private class HomeKeyXAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			getInteractiveXMap().scrollToIndex(0);
		}
	}

	/** Action to scroll the y-axis to bottom. */
	private class EndKeyXAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int max = getInteractiveXMap().getMaxIndex();
			getInteractiveXMap().scrollToIndex(max);
		}
	}

	private class PageUpYAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int scrollBy = getInteractiveYMap().getNumVisible();
			getInteractiveYMap().scrollBy(-scrollBy);
		}
	}

	private class PageDownYAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int scrollBy = getInteractiveYMap().getNumVisible();
			getInteractiveYMap().scrollBy(scrollBy);
		}
	}

	private class PageUpXAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int scrollBy = getInteractiveXMap().getNumVisible();
			getInteractiveXMap().scrollBy(-scrollBy);
		}
	}

	private class PageDownXAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int scrollBy = getInteractiveXMap().getNumVisible();
			getInteractiveXMap().scrollBy(scrollBy);
		}
	}

	private class ArrowLeftAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			getInteractiveXMap().scrollBy(-1);
		}
	}

	private class ArrowRightAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			getInteractiveXMap().scrollBy(1);
		}
	}

	private class ArrowUpAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			getInteractiveYMap().scrollBy(-1);
		}
	}

	private class ArrowDownAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			getInteractiveYMap().scrollBy(1);
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

			final boolean genesSelected = rowSelection.getNSelectedIndexes() > 0;
			final boolean arraysSelected = colSelection.getNSelectedIndexes() > 0;

			if (genesSelected || arraysSelected) {
				// Zoom in (or out)
				getInteractiveXMap().zoomToSelected(colSelection.getMinIndex(),
						colSelection.getMaxIndex());
				getInteractiveYMap().zoomToSelected(rowSelection.getMinIndex(),
						rowSelection.getMaxIndex());

				// Then scroll
				getInteractiveXMap().scrollToFirstIndex(
						colSelection.getMinIndex());
				getInteractiveYMap().scrollToFirstIndex(
						rowSelection.getMinIndex());
			}
			// zoomSelection();
		}
	}

	/** Zooms into GlobalView by 1 scale step (depends on previous scale). */
	private class ZoomInAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			getInteractiveXMap().zoomInBegin();
			getInteractiveYMap().zoomInBegin();

			notifyAllMapObservers();
		}
	}

	/** Zooms out of GlobalView by 1 scale step (depends on previous scale). */
	private class ZoomOutAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			getInteractiveXMap().zoomOutBegin();
			getInteractiveYMap().zoomOutBegin();

			notifyAllMapObservers();
		}
	}

	/** Resets the GlobalView to all zoomed-out state */
	private class HomeAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			resetMatrixViews();
			dendroView.getInteractiveMatrixView().setAspectRatio(
					interactiveXmap.getMaxIndex() + 1,
					interactiveYmap.getMaxIndex() + 1);
		}
	}

	/* >>>>>>> Component Listeners <<<<<<<<< */
	/**
	 * Listener for the setScale-buttons in DendroView. Changes the scale in
	 * xMap and yMap MapContainers, allowing the user to zoom in or out of each
	 * individual axis in GlobalView.
	 *
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
					resetMatrixViews();
					dendroView.getInteractiveMatrixView().setAspectRatio(
							interactiveXmap.getMaxIndex() + 1,
							interactiveYmap.getMaxIndex() + 1);
				} else if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
					interactiveXmap.zoomOutCenter("fast");
					interactiveYmap.zoomOutCenter("fast");
				} else if ((e.getModifiers() & InputEvent.ALT_MASK) != 0) {
					interactiveXmap.zoomOutCenter("slow");
					interactiveYmap.zoomOutCenter("slow");
				} else {
					interactiveXmap.zoomOutCenter("medium");
					interactiveYmap.zoomOutCenter("medium");
				}

			} else if (e.getSource() == dendroView.getXYPlusButton()) {
				if ((e.getModifiers() & InputEvent.META_MASK) != 0) {
					interactiveXmap.zoomInCenter("slam");
					interactiveYmap.zoomInCenter("slam");
				} else if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
					interactiveXmap.zoomInCenter("fast");
					interactiveYmap.zoomInCenter("fast");
				} else if ((e.getModifiers() & InputEvent.ALT_MASK) != 0) {
					interactiveXmap.zoomInCenter("slow");
					interactiveYmap.zoomInCenter("slow");
				} else {
					interactiveXmap.zoomInCenter("medium");
					interactiveYmap.zoomInCenter("medium");
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
					resetMatrixViews();
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
	 * @author chris0689
	 *
	 */
	private class SplitPaneListener extends ComponentAdapter {

		@Override
		public void componentResized(final ComponentEvent e) {

			/* TODO define JSplitPane behavior */
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
	 *
	 * @author CKeil
	 *
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

		case InteractiveMatrixView.FILL:
			setMatrixFill();
			break;

		case InteractiveMatrixView.EQUAL:
			setMatrixAxesEqual();
			break;

		case InteractiveMatrixView.PROPORT:
			setMatrixPropotional();
			break;

		default:
			setMatrixFill();
			break;
		}

		addDendroViewListeners();
		refocusViewPort();
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
		final double absGVWidth = dendroView.getInteractiveMatrixView()
				.getWidth();
		final double absGVHeight = dendroView.getInteractiveMatrixView()
				.getHeight();

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

	/**
	 * Resizes the matrix such that it fits all pixels as squares. If gvWidth or
	 * gvHeight would go below a certain size, the matrix is adjusted such that
	 * the content remains viewable in a meaningful manner.
	 */
	private void setMatrixPropotional() {

		// Condition: All pixels must be shown, but must have same scale.

		final double xScale = interactiveXmap.getScale();
		final double yScale = interactiveYmap.getScale();

		if (xScale >= yScale) {
			interactiveXmap.setScale(yScale);

			double newWidth = calcAxisDimensionFromMap(interactiveXmap,
					DendroView.MAX_GV_WIDTH);

			dendroView.setGVWidth(newWidth);

		} else {
			interactiveYmap.setScale(xScale);

			double newHeight = calcAxisDimensionFromMap(interactiveYmap,
					DendroView.MAX_GV_HEIGHT);

			dendroView.setGVHeight(newHeight);
		}
	}

	private static double calcAxisDimensionFromMap(final MapContainer map,
			final double max) {

		final double used = map.getUsedPixels();
		final double avail = map.getAvailablePixels();

		return calcAxisDimension(avail, used, max);
	}

	/**
	 * TODO just deprecate this method when this feature will be implemented...
	 * You can just take the smaller of WIDTH or HEIGHT of a maximized matrix.
	 * 
	 * @param big
	 * @param small
	 * @param max
	 * @return
	 */
	private static double calcAxisDimension(final double big,
			final double small, final double max) {

		final double percentDiff = small / big;

		double newAxis = percentDiff * max;

		// rounding
		newAxis *= 1000;
		newAxis = Math.round(newAxis);
		newAxis /= 1000;

		return newAxis;
	}

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
	 *
	 */
	private class ZoomListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			final boolean genesSelected = rowSelection.getNSelectedIndexes() > 0;
			final boolean arraysSelected = colSelection.getNSelectedIndexes() > 0;

			if (genesSelected || arraysSelected) {
				if ((arg0.getModifiers() & InputEvent.SHIFT_MASK) != 0
						|| (arg0.getModifiers() & InputEvent.META_MASK) != 0) {

					// Zoom in (or out)
					getInteractiveXMap().zoomToSelected(
							colSelection.getMinIndex(),
							colSelection.getMaxIndex());
					getInteractiveYMap().zoomToSelected(
							rowSelection.getMinIndex(),
							rowSelection.getMaxIndex());

					// Then scroll
					getInteractiveXMap().scrollToFirstIndex(
							colSelection.getMinIndex());
					getInteractiveYMap().scrollToFirstIndex(
							rowSelection.getMinIndex());

					// zoomSelection();
					// centerSelection();
				} else if ((arg0.getModifiers() & InputEvent.ALT_MASK) != 0) {
					dendroView.getInteractiveMatrixView()
							.smoothZoomTowardSelection(
									colSelection.getMinIndex(),
									(colSelection.getMaxIndex()
											- colSelection.getMinIndex() + 1),
									rowSelection.getMinIndex(),
									(rowSelection.getMaxIndex()
											- rowSelection.getMinIndex() + 1));
				} else {
					dendroView.getInteractiveMatrixView()
							.smoothAnimatedZoomTowardSelection(
									colSelection.getMinIndex(),
									(colSelection.getMaxIndex()
											- colSelection.getMinIndex() + 1),
									rowSelection.getMinIndex(),
									(rowSelection.getMaxIndex()
											- rowSelection.getMinIndex() + 1));
				}
			}
		}
	}

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

		final ColorPresets colorPresets = DendrogramFactory.getColorPresets();
		colorPresets.setConfigNode(configNode);
		colorExtractor = new ColorExtractor(
				tvModel.getDataMatrix().getMinVal(), tvModel.getDataMatrix()
						.getMaxVal());
		colorExtractor.setDefaultColorSet(colorPresets.getDefaultColorSet());
		colorExtractor.setMissing(DataModel.NAN, DataModel.EMPTY);

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
		setupMapContainers();

		interactiveXmap.setScrollbar(dendroView.getMatrixXScroll());
		interactiveYmap.setScrollbar(dendroView.getMatrixYScroll());

		// Drawers
		dendroView.getInteractiveMatrixView().setArrayDrawer(arrayDrawer);
		dendroView.getGlobalMatrixView().setArrayDrawer(arrayDrawer);

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
		colorExtractor.setConfigNode(configNode);

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

		dendroView.getInteractiveMatrixView().setXMap(interactiveXmap);
		dendroView.getInteractiveMatrixView().setYMap(interactiveYmap);

		dendroView.getGlobalMatrixView().setXMap(globalXmap);
		dendroView.getGlobalMatrixView().setYMap(globalYmap);

		dendroView.getGlobalMatrixView().setInteractiveXMap(interactiveXmap);
		dendroView.getGlobalMatrixView().setInteractiveYMap(interactiveYmap);
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
		dendroView.getInteractiveMatrixView().setHeaders(
				tvModel.getRowHeaderInfo(), tvModel.getColumnHeaderInfo());
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

		LogBuffer.println("Importing color settings...");

		colorExtractor.importPreferences(oldNode);

		/* Update GradientChooser node */
		String lastActive = oldNode.node("GradientChooser").get("activeColors",
				"RedGreen");
		configNode.node("GradientChooser").put("activeColors", lastActive);

		/* Store copied node in new ColorPresets node */
		final ColorPresets colorPresets = DendrogramFactory.getColorPresets();
		colorPresets.setConfigNode(configNode);
		colorPresets.addColorSet(colorExtractor.getActiveColorSet());
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

	/**
	 * Scrolls to index i in the Y-MapContainer
	 *
	 * @param i
	 */
	public void scrollToGene(final int i) {

		getInteractiveYMap().scrollToIndex(i);
		getInteractiveYMap().notifyObservers();
	}

	/**
	 * Scrolls to index i in the X-MapContainer.
	 *
	 * @param i
	 */
	public void scrollToArray(final int i) {

		getInteractiveXMap().scrollToIndex(i);
		getInteractiveXMap().notifyObservers();
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

		dendroView.getInteractiveMatrixView().setColSelection(colSelection);
		dendroView.getColumnTreeView().setTreeSelection(colSelection);
		dendroView.getRowLabelView().setColSelection(colSelection);
		dendroView.getColumnLabelView().setColSelection(colSelection);
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

		dendroView.getInteractiveMatrixView().setRowSelection(rowSelection);
		dendroView.getRowTreeView().setTreeSelection(rowSelection);
		dendroView.getRowLabelView().setRowSelection(rowSelection);
		dendroView.getColumnLabelView().setRowSelection(rowSelection);
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

	// Getters for fields
	public ArrayDrawer getArrayDrawer() {

		return arrayDrawer;
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

		dendroView
				.updateSearchTermBoxes(tvModel.getRowHeaderInfo(),
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

		/* Determine if either MapContainer is at minimum scale */
		boolean isXMin = Helper.nearlyEqual(interactiveXmap.getMinScale(),
				interactiveXmap.getScale());
		boolean isYMin = Helper.nearlyEqual(interactiveYmap.getMinScale(),
				interactiveYmap.getScale());
		boolean atRight = (interactiveXmap.getFirstVisible() + interactiveXmap
				.getNumVisible()) == (interactiveXmap.getMaxIndex() + 1);
		boolean atLeft = interactiveXmap.getFirstVisible() == 0;
		boolean atTop = interactiveYmap.getFirstVisible() == 0;
		boolean atBottom = (interactiveYmap.getFirstVisible() + interactiveYmap
				.getNumVisible()) == (interactiveYmap.getMaxIndex() + 1);

		int xTilesVisible = interactiveXmap.getNumVisible();
		int yTilesVisible = interactiveYmap.getNumVisible();

		final boolean genesSelected = this.rowSelection != null
				&& rowSelection.getNSelectedIndexes() > 0;
		final boolean arraysSelected = this.colSelection != null
				&& colSelection.getNSelectedIndexes() > 0;

		// Note: A selection is "fully zoomed" if there is no selection - this
		// will disable the zoom selection button
		boolean isSelectionZoomed = (!genesSelected && !arraysSelected)
				|| (genesSelected
						&& rowSelection.getMinIndex() == interactiveYmap
								.getFirstVisible()
						&& (rowSelection.getMaxIndex()
								- rowSelection.getMinIndex() + 1) == yTilesVisible
						&& arraysSelected
						&& colSelection.getMinIndex() == interactiveXmap
								.getFirstVisible() && (colSelection
						.getMaxIndex() - colSelection.getMinIndex() + 1) == xTilesVisible);

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
