package edu.stanford.genetics.treeview;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JScrollBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.plugin.dendroview.ArrayDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorPresets;
import edu.stanford.genetics.treeview.plugin.dendroview.DendrogramFactory;
import edu.stanford.genetics.treeview.plugin.dendroview.DoubleArrayDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.GlobalMatrixView;
import edu.stanford.genetics.treeview.plugin.dendroview.InteractiveMatrixView;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;

/**
 * This controller explicitly handles direct user interaction with the
 * InteractiveMatrixView. 
 * This can probably also be split into Parent + Interactive + Global 
 * @author chris0689
 *
 */
public class MatrixViewController implements Observer, 
ConfigNodePersistent {

	private InteractiveMatrixView imView;
	private GlobalMatrixView gmView;
	
	private TreeSelectionI rowSelection;
	private TreeSelectionI colSelection;
	
	private MapContainer interactiveXmap;
	private MapContainer interactiveYmap;

	private MapContainer globalXmap;
	private MapContainer globalYmap;
	
	protected ArrayDrawer arrayDrawer;
	private ColorExtractor colorExtractor;
	
	private DataModel model;
	protected Preferences configNode;
	
	public MatrixViewController(final InteractiveMatrixView imView,
			final GlobalMatrixView gmView, final DataModel model) {
		
		this.imView = imView;
		this.gmView = gmView;
		this.model = model;
		
	}
	
	/**
	 * Setting up the main components of the controller. Separate from 
	 * constructor in order to allow for configNode setting first. 
	 */
	public void setup() {
		
		if(configNode == null) {
			LogBuffer.println("Could not set up IMVController. No configNode"
					+ "was set!");
			return;
		}
		
		setupDrawingComponents();
		addKeyBindings();
	}
	
	/**
	 * Checks whether there is a configuration node for the current model and
	 * DendroView. If not it creates one.
	 */
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			configNode = parentNode.node("IMViewController");

		} else {
			LogBuffer.println("Could not find or create IMViewController "
					+ "node because parentNode was null.");
			return;
		}
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

	@Override // Observer code
	public void update(Observable o, Object arg) {
		
		
	}
	
	/**
	 * Sets a new ColorExtractor with different data 
	 * @param minVal - data minimum value.
	 * @param maxVal - data maximum value.
	 */
	public void setColorExtractorData(final double minVal, 
			final double maxVal) {
		
		colorExtractor.setMin(minVal);
		colorExtractor.setMax(maxVal);
	}
	
	private void setupDrawingComponents() {
		
		final ColorPresets colorPresets = DendrogramFactory.getColorPresets();
		colorPresets.setConfigNode(configNode);
		colorExtractor = new ColorExtractor(
				model.getDataMatrix().getMinVal(), model.getDataMatrix()
						.getMaxVal());
		colorExtractor.setDefaultColorSet(colorPresets.getDefaultColorSet());
		colorExtractor.setMissing(DataModel.NAN, DataModel.EMPTY);

		final DoubleArrayDrawer dArrayDrawer = new DoubleArrayDrawer();
		dArrayDrawer.setColorExtractor(colorExtractor);
		arrayDrawer = dArrayDrawer;
		((TVModel) model).addObserver(arrayDrawer);
		
		dArrayDrawer.setDataMatrix(model.getDataMatrix());
		dArrayDrawer.recalculateContrast();
		dArrayDrawer.setConfigNode("ArrayDrawer1");
		
		imView.setArrayDrawer(arrayDrawer);
		gmView.setArrayDrawer(arrayDrawer);
	}
	
	private void addKeyBindings() {
		
		final InputMap input_map = imView.getInputMap();
		final ActionMap action_map = imView.getActionMap();

		/* Gets the system's modifier key (Ctrl or Cmd) */
		final int modifier = Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask();
		final int shift_mask = InputEvent.SHIFT_MASK;
		
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
		action_map.put("zoomSelection", new ZoomSelectionAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, modifier),
				"resetZoom");
		action_map.put("resetZoom", new HomeAction());
	}
	
	/* -------------- Listeners --------------------- */
	private class HomeKeyYAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			interactiveYmap.scrollToIndex(0);
		}
	}

	/** Action to scroll the y-axis to bottom. */
	private class EndKeyYAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int max = interactiveYmap.getMaxIndex();
			interactiveYmap.scrollToIndex(max);
		}
	}

	/** Action to scroll the y-axis to top. */
	private class HomeKeyXAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			interactiveXmap.scrollToIndex(0);
		}
	}

	/** Action to scroll the y-axis to bottom. */
	private class EndKeyXAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int max = interactiveXmap.getMaxIndex();
			interactiveXmap.scrollToIndex(max);
		}
	}

	private class PageUpYAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int scrollBy = interactiveYmap.getNumVisible();
			interactiveYmap.scrollBy(-scrollBy,false);
		}
	}

	private class PageDownYAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int scrollBy = interactiveYmap.getNumVisible();
			interactiveYmap.scrollBy(scrollBy,false);
		}
	}

	private class PageUpXAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int scrollBy = interactiveXmap.getNumVisible();
			interactiveXmap.scrollBy(-scrollBy,false);
		}
	}

	private class PageDownXAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int scrollBy = interactiveXmap.getNumVisible();
			interactiveXmap.scrollBy(scrollBy,false);
		}
	}

	private class ArrowLeftAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			interactiveXmap.scrollBy(-1,false);
		}
	}

	private class ArrowRightAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			interactiveXmap.scrollBy(1,false);
		}
	}

	private class ArrowUpAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			interactiveYmap.scrollBy(-1,false);
		}
	}

	private class ArrowDownAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			interactiveYmap.scrollBy(1,false);
		}
	}

	/**
	 * Zooms into the selected area
	 */
	private class ZoomSelectionAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			final boolean genesSelected = rowSelection.getNSelectedIndexes() > 0;
			final boolean arraysSelected = colSelection.getNSelectedIndexes() > 0;

			if (genesSelected || arraysSelected) {
				// Zoom in (or out)
				interactiveXmap.zoomToSelected(colSelection.getMinIndex(),
						colSelection.getMaxIndex());
				interactiveYmap.zoomToSelected(rowSelection.getMinIndex(),
						rowSelection.getMaxIndex());

				// Then scroll
				interactiveXmap.scrollToFirstIndex(
						colSelection.getMinIndex());
				interactiveYmap.scrollToFirstIndex(
						rowSelection.getMinIndex());
			}
		}
	}

	/** Zooms into GlobalView by 1 scale step (depends on previous scale). */
	private class ZoomInAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			interactiveXmap.zoomInBegin();
			interactiveYmap.zoomInBegin();

			notifyAllMapObservers();
		}
	}

	/** Zooms out of GlobalView by 1 scale step (depends on previous scale). */
	private class ZoomOutAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			interactiveXmap.zoomOutBegin();
			interactiveYmap.zoomOutBegin();

			notifyAllMapObservers();
		}
	}

	
	/** Resets the GlobalView to all zoomed-out state */
	private class HomeAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			resetMatrixViews();
			imView.setAspectRatio(
					interactiveXmap.getMaxIndex() + 1,
					interactiveYmap.getMaxIndex() + 1);
		}
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

				imView.resetView();
				imView.repaint();

				gmView.resetView();
				gmView.repaint();
			}
		});
	}
	
	/**
	 * Zooms on a selection. The type of zooming is dependent on input 
	 * modifiers used when executing the zoom.
	 * @param modifiers Input modifiers when pressing a button.
	 */
	public void zoomOnSelection(final int modifiers) {
		
		final boolean rowsSelected = rowSelection.getNSelectedIndexes() > 0;
		final boolean colssSelected = colSelection.getNSelectedIndexes() > 0;

		if (rowsSelected || colssSelected) {
			if ((modifiers & InputEvent.SHIFT_MASK) != 0
					|| (modifiers & InputEvent.META_MASK) != 0) {
				// Zoom in (or out)
				interactiveXmap.zoomToSelected(
						colSelection.getMinIndex(),
						colSelection.getMaxIndex());
				interactiveYmap.zoomToSelected(
						rowSelection.getMinIndex(),
						rowSelection.getMaxIndex());

				// Then scroll
				interactiveXmap.scrollToFirstIndex(
						colSelection.getMinIndex());
				interactiveYmap.scrollToFirstIndex(
						rowSelection.getMinIndex());

			} else if ((modifiers & InputEvent.ALT_MASK) != 0) {
				imView.smoothZoomTowardSelection(
								colSelection.getMinIndex(),
								(colSelection.getMaxIndex()
										- colSelection.getMinIndex() + 1),
								rowSelection.getMinIndex(),
								(rowSelection.getMaxIndex()
										- rowSelection.getMinIndex() + 1));
			} else {
				imView.smoothAnimatedZoomTowardSelection(
								colSelection.getMinIndex(),
								(colSelection.getMaxIndex()
										- colSelection.getMinIndex() + 1),
								rowSelection.getMinIndex(),
								(rowSelection.getMaxIndex()
										- rowSelection.getMinIndex() + 1));
			}
		}
	}
	
	/**
	 * Scrolls to index i in the Y-MapContainer
	 *
	 * @param i
	 */
	public void scrollToGene(final int i) {

		interactiveYmap.scrollToIndex(i);
		interactiveYmap.notifyObservers();
	}

	/**
	 * Scrolls to index i in the X-MapContainer.
	 *
	 * @param i
	 */
	public void scrollToArray(final int i) {

		interactiveXmap.scrollToIndex(i);
		interactiveXmap.notifyObservers();
	}
	
	/**
	 * Notifies both MatrixViews that the data has updated, so it can
	 * recalculate pixel color values during the next update.
	 */
	public void updateMatrixPixels() {
		
		imView.setDataChanged();
		gmView.setDataChanged();
	}
	
	/**
	 * Assigns references of MapContainer instances to be used for 
	 * interactivity and information display in InteractiveMatrixView. 
	 * @param xmap
	 * @param ymap
	 */
	public void setInteractiveMapContainers(final MapContainer xmap, 
			final MapContainer ymap) {
		
		this.interactiveXmap = xmap;
		this.interactiveYmap = ymap;
		
		interactiveXmap.addObserver(this);
		interactiveYmap.addObserver(this);
		
		imView.setXMap(xmap);
		imView.setYMap(ymap);
	}
	
	/**
	 * Assigns references of MapContainer instances to be used for 
	 * information display in GlobalMatrixView. 
	 * @param xmap
	 * @param ymap
	 */
	public void setGlobalMapContainers(final MapContainer xmap, 
			final MapContainer ymap) {
		
		this.globalXmap = xmap;
		this.globalYmap = ymap;
		
		globalXmap.addObserver(this);
		globalYmap.addObserver(this);
		
		gmView.setXMap(xmap);
		gmView.setYMap(ymap);
	}
	
	/**
	 * Set rowSelection
	 *
	 * @param rowSelection
	 *            The TreeSelection which is set by selecting genes in the
	 *            GlobalView
	 */
	public void setRowSelection(final TreeSelectionI rowSelection) {

		if (this.rowSelection != null) {
			this.rowSelection.deleteObserver(this);
		}

		this.rowSelection = rowSelection;
		this.rowSelection.addObserver(this);
		
		imView.setRowSelection(rowSelection);
		gmView.setRowSelection(rowSelection);
	}

	/**
	 * Set colSelection
	 *
	 * @param colSelection
	 *            The TreeSelection which is set by selecting arrays in the
	 *            GlobalView
	 */
	public void setColSelection(final TreeSelectionI colSelection) {

		if (this.colSelection != null) {
			this.colSelection.deleteObserver(this);
		}

		this.colSelection = colSelection;
		this.colSelection.addObserver(this);
		
		imView.setColSelection(colSelection);
		gmView.setColSelection(colSelection);
	}
	
	/**
	 * Assigns scrollbars to MapContainers.
	 * @param rowScroll Scrollbar for scrolling through matrix rows.
	 * @param colScroll Scrollbar for scrolling through matrix columns.
	 */
	public void setScrollBars(final JScrollBar rowScroll, 
			final JScrollBar colScroll) {

		interactiveXmap.setScrollbar(colScroll);
		interactiveYmap.setScrollbar(rowScroll);
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
	 * Access a reference of the color extractor which determines how
	 * colors are displayed in relation to values.
	 * @return The MatrixView's color extractor.
	 */
	public ColorExtractor getColorExtractor() {
		
		return colorExtractor;
	}
	
	/**
	 * Evaluates current zoom status and returns an array of booleans, which
	 * can be used to test certain zoom conditions. This is for example relevant
	 * for the DendroView buttons. They will be enabled or disabled based 
	 * on the zoom status.
	 * @return Ordered boolean array of zoom statuses: [isXMin, isYMin, 
	 * atRight, atLeft, atTop, atBottom, isSelectionZoomed]
	 */
	public boolean[] getZoomStatusForButtons() {
		
		boolean[] zoomStatus;
		
		/* Determine if either MapContainer is at minimum scale */
		boolean isXMin = interactiveXmap.showsAllTiles();
		boolean isYMin = interactiveYmap.showsAllTiles();
		
		boolean atRight = (interactiveXmap.getFirstVisible() + interactiveXmap
				.getNumVisible()) == (interactiveXmap.getMaxIndex() + 1);
		boolean atLeft = interactiveXmap.getFirstVisible() == 0;
		boolean atTop = interactiveYmap.getFirstVisible() == 0;
		boolean atBottom = (interactiveYmap.getFirstVisible() + interactiveYmap
				.getNumVisible()) == (interactiveYmap.getMaxIndex() + 1);

		int xTilesVisible = interactiveXmap.getNumVisible();
		int yTilesVisible = interactiveYmap.getNumVisible();

		final boolean genesSelected = rowSelection.getNSelectedIndexes() > 0;
		final boolean arraysSelected = colSelection.getNSelectedIndexes() > 0;

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
		
		zoomStatus = new boolean[]{isXMin, isYMin, atRight, atLeft, atTop, 
				atBottom, isSelectionZoomed};
		
		return zoomStatus;
	}
}
