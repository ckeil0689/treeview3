package gui.matrix;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.PropertyListParser;
import gui.Controller;
import gui.DendrogramFactory;
import gui.info.DataTicker;
import gui.labels.LabelView;
import model.data.matrix.DataModel;
import model.data.matrix.TVModel;
import model.data.trees.TreeSelectionI;
import model.export.matrix.IMVMouseAdapter;
import preferences.ColorPresets;
import preferences.ConfigNodePersistent;
import util.LogBuffer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** This controller explicitly handles direct user interaction with the
 * InteractiveMatrixView.
 * This can probably also be split into Parent + Interactive + Global */
public class MatrixViewController implements Observer, ConfigNodePersistent,
        Controller {

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

	protected LabelView rowLabelView;
	protected LabelView colLabelView;

	// Data ticker reference, so it can be updated by the MouseAdapter
	private DataTicker ticker;

	private final boolean swap_animation_modifiers = true;

	private static int SCROLL_REPAINT_INTERVAL_MS = 250;

	private static int SPACE_PRESS_INIT_DELAY = 350;
	private static int SPACE_PRESS_REPEAT_DELAY = 50;

	public MatrixViewController(final InteractiveMatrixView imView,
		final GlobalMatrixView gmView,final DataModel model,
		final LabelView rowLabelView,final LabelView colLabelView) {

		this.imView = imView;
		this.gmView = gmView;
		this.model = model;
		this.rowLabelView = rowLabelView;
		this.colLabelView = colLabelView;
	}

	/** Setting up the main components of the controller. Separate from
	 * constructor in order to allow for configNode setting first. */
	public void setup() {

		setupDrawingComponents();
		addKeyBindings();
	}


	@Override
	public void addListeners() {

		removeAllMouseListeners();

		IMVMouseAdapter mmListener = new IMVMouseAdapter(this, imView,
			interactiveXmap,interactiveYmap);

		imView.addMouseListener(mmListener);
		imView.addMouseMotionListener(mmListener);

		imView.addMouseWheelListener(new MatrixMouseWheelListener());
	}

	/** Simply ensures that no listeners are added on top of others. This is
	 * actually needed due to how InteractiveMatrixView is currently handled
	 * when loading a new file... */
	private void removeAllMouseListeners() {

		for(MouseListener l : imView.getMouseListeners()) {

			imView.removeMouseListener(l);
		}

		for(MouseMotionListener l : imView.getMouseMotionListeners()) {

			imView.removeMouseMotionListener(l);
		}

		for(MouseWheelListener l : imView.getMouseWheelListeners()) {

			imView.removeMouseWheelListener(l);
		}
	}

	/** Checks whether there is a configuration node for the current model and
	 * DendroView. If not it creates one. */
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if(parentNode == null) {
			LogBuffer.println("Could not find or create MatrixViewController " +
				"node because parentNode was null.");
			return;
		}

		this.configNode = parentNode;
		getColorExtractor().setConfigNode(configNode);
	}

	@Override
	public Preferences getConfigNode() {

		return configNode;
	}

	@Override
	public void requestStoredState() {

		return; // nothing stored yet.
	}

	@Override
	public void storeState() {

		return; // nothing to store yet
	}

	@Override
	public void importStateFrom(final Preferences oldNode) {

		return; // nothing to import yet
	}

	/** Update the state of color extractor to reflect settings from an imported
	 * node.
	 * 
	 * @param node
	 * @throws BackingStoreException */
	public void importColorPreferences(final Preferences oldNode)
		throws BackingStoreException {

		LogBuffer.println("Importing color settings...");

		final ColorPresets colorPresets = DendrogramFactory.getColorPresets();
		String colorPresetsNode = colorPresets.getClass().getSimpleName();

		if(!oldNode.nodeExists(colorPresetsNode)) {
			LogBuffer.println(colorPresetsNode +
				" node not found when trying to import previous color " +
				"settings. Aborting import attempt.");
			return;
		}

		// Store copied node in new ColorPresets node
		colorPresets.setConfigNode(configNode);
		colorPresets.importStateFrom(oldNode.node(colorPresetsNode));
		colorExtractor.importStateFrom(oldNode.node(colorPresetsNode));
	}

	@Override // Observer code
	public void update(Observable o, Object arg) {

		if(o instanceof MapContainer) {
			updateGMVViewportRect();
		}
	}

	/** Lets the GlobalMatrixView know about the current InteractiveMatrixView
	 * viewport.
	 * TODO calculate pixels from maps here and send a rectangle instead. */
	private void updateGMVViewportRect() {

		int firstXVisible = interactiveXmap.getFirstVisible();
		int firstYVisible = interactiveYmap.getFirstVisible();
		int numXVisible = interactiveXmap.getNumVisible();
		int numYVisible = interactiveYmap.getNumVisible();

		gmView.setIMVViewportRange(firstXVisible, firstYVisible, numXVisible,
			numYVisible);
	}

	/** Sets a new ColorExtractor with different data
	 * 
	 * @param minVal - data minimum value.
	 * @param maxVal - data maximum value. */
	public void setColorExtractorData(final double minVal,final double maxVal) {

		colorExtractor.setMin(minVal);
		colorExtractor.setMax(maxVal);
	}

	private void setupDrawingComponents() {

		this.colorExtractor = new ColorExtractor(
			model.getDataMatrix().getMinVal(),
			model.getDataMatrix().getMaxVal());
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

	@Override
	public void addKeyBindings() {

		final InputMap input_map =
			imView.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap action_map = imView.getActionMap();

		/* Gets the system's modifier key (CTRL for Windows, CMD for OS X) */
		final int modifier =
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
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

		//Disable spacebar clicks focussed button behavior.  See:
		//http://stackoverflow.com/questions/4472530/jbutton-referring-to-space-
		//bar-the-same-as-a-click
		InputMap btn_input_map =
			(InputMap) UIManager.get("Button.focusInputMap");
		btn_input_map.put(KeyStroke.getKeyStroke("pressed SPACE"),"none");
		btn_input_map.put(KeyStroke.getKeyStroke("released SPACE"),"none");
		//Now let's swap out the spacebar default behavior with the enter key
		btn_input_map.put(KeyStroke.getKeyStroke("ENTER"),"pressed");
		btn_input_map.put(KeyStroke.getKeyStroke("released ENTER"),"released");

		/* Show more/fewer labels */
		input_map.put(KeyStroke.getKeyStroke("SPACE"), "labelToggle");
		action_map.put("labelToggle", new LabelToggleAction());

		//Holding control and/or shift controls a col/row highlight that follows
		//the hover position
		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 1),
			"rowHoverStart");
		action_map.put("rowHoverStart", new RowHoverStartAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0, true),
			"rowHoverStop");
		action_map.put("rowHoverStop", new RowHoverStopAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, 2),
			"columnHoverStart");
		action_map.put("columnHoverStart", new ColumnHoverStartAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, 0, true),
			"columnHoverStop");
		action_map.put("columnHoverStop", new ColumnHoverStopAction());

		//The following cases handle various combinations of row/col highlights
		//occurring in different orders of both the shift and control modifiers
		//being pressed/released
		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT,
			InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK),
			"bothHoverStart");
		action_map.put("bothHoverStart", new BothHoverStartAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 2, true),
			"rowHoverStop");
		action_map.put("rowHoverStop", new RowHoverStopAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL,
			InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK),
			"bothHoverStart");
		action_map.put("bothHoverStart", new BothHoverStartAction());

		input_map.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, 1, true),
			"columnHoverStop");
		action_map.put("columnHoverStop", new ColumnHoverStopAction());
	}

	/* -------------- Listeners --------------------- */
	private class HomeKeyYAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int min = interactiveYmap.getMinIndex();
			interactiveYmap.scrollToIndex(min);
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

			final int min = interactiveXmap.getMinIndex();
			interactiveXmap.scrollToIndex(min);
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
			interactiveYmap.scrollBy(-scrollBy);
		}
	}

	private class PageDownYAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int scrollBy = interactiveYmap.getNumVisible();
			interactiveYmap.scrollBy(scrollBy);
		}
	}

	private class PageUpXAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int scrollBy = interactiveXmap.getNumVisible();
			interactiveXmap.scrollBy(-scrollBy);
		}
	}

	private class PageDownXAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final int scrollBy = interactiveXmap.getNumVisible();
			interactiveXmap.scrollBy(scrollBy);
		}
	}

	private class ArrowLeftAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			interactiveXmap.scrollBy(-1);
		}
	}

	private class ArrowRightAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			interactiveXmap.scrollBy(1);
		}
	}

	private class ArrowUpAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			interactiveYmap.scrollBy(-1);
		}
	}

	private class ArrowDownAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			interactiveYmap.scrollBy(1);
		}
	}

	/** Zooms into the selected area */
	private class ZoomSelectionAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			final boolean genesSelected =
				rowSelection.getNSelectedIndexes() > 0;
			final boolean arraysSelected =
				colSelection.getNSelectedIndexes() > 0;

			if(genesSelected || arraysSelected) {
				// Zoom in (or out)
				interactiveXmap.zoomToSelected(colSelection.getMinIndex(),
					colSelection.getMaxIndex());
				interactiveYmap.zoomToSelected(rowSelection.getMinIndex(),
					rowSelection.getMaxIndex());

				// Then scroll
				interactiveXmap.scrollToFirstIndex(colSelection.getMinIndex());
				interactiveYmap.scrollToFirstIndex(rowSelection.getMinIndex());
			}
		}
	}

	/** Zooms into GlobalView by 1 scale step (depends on previous scale). */
	private class ZoomInAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			interactiveXmap.zoomInCenter(MapContainer.ZOOM_DEFAULT);
			interactiveYmap.zoomInCenter(MapContainer.ZOOM_DEFAULT);

			notifyAllMapObservers();
		}
	}

	/** Zooms out of GlobalView by 1 scale step (depends on previous scale). */
	private class ZoomOutAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			interactiveXmap.zoomOutCenter(MapContainer.ZOOM_DEFAULT);
			interactiveYmap.zoomOutCenter(MapContainer.ZOOM_DEFAULT);

			notifyAllMapObservers();
		}
	}

	/** Resets the GlobalView to all zoomed-out state */
	private class HomeAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			resetMatrixViews();
			imView.setAspectRatio( //TODO move into matrixview.resetView
				interactiveXmap.getTotalTileNum(),
				interactiveYmap.getTotalTileNum());
		}
	}

	/**
	 * This timer prevents the holding of the spacebar from making the label
	 * toggle flicker.  The fact that this timer is running prevents the toggle
	 * action from happening.  Once the timer expires, hitting the spacebar
	 * again will allow the toggle to happen.  The timer restarts itself when
	 * the spacebar is "pressed" again during the running timer, which is what
	 * results repeatedly from holding down the spacebar.
	 */
	private javax.swing.Timer spacePressTimer;
	private ActionListener spacePressTimerListener = new ActionListener() {

		/**
		 * When a space press timer expires, this is called.  Since all we need
		 * to know is whether the timer is running or not in order to do the
		 * right thing (e.g. there's nothing to do upon release - only press is
		 * what toggles the labels - holding the spacebar down does nothing -
		 * this timer only prevents repeated rapid toggles)
		 */
		@Override
		public void actionPerformed(ActionEvent evt) {
			if(evt.getSource() == spacePressTimer) {
				//Stop timer so it doesn't repeat
				spacePressTimer.stop();
				spacePressTimer = null;
			}
		}
	};


	/**
	 * Toggles whizzing labels from as many as is possible to either no or some
	 * labels (depending on the "showBaseIsNone" variable in LabelSettings).
	 */
	private class LabelToggleAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {
			if(spacePressTimer != null && spacePressTimer.isRunning()) {
				spacePressTimer.setDelay(SPACE_PRESS_REPEAT_DELAY);
				spacePressTimer.restart();
			} else {
				toggleLabels();
				if(spacePressTimer != null) {
					spacePressTimer.setDelay(SPACE_PRESS_INIT_DELAY);
					spacePressTimer.start();
					
				} else {
					spacePressTimer = new Timer(SPACE_PRESS_INIT_DELAY,
						spacePressTimerListener);
					spacePressTimer.start();
				}
			}
		}
	}

	public void toggleLabels() {
		if(!rowLabelView.inLabelPortMode() ||
			rowLabelView.isLabelPortFlankMode()) {

			rowLabelView.setLabelPortMode(true);
			rowLabelView.setLabelPortFlankMode(false);
			colLabelView.setLabelPortMode(true);
			colLabelView.setLabelPortFlankMode(false);
		} else if(rowLabelView.isLabelPortDefaultNone()) {
			rowLabelView.setLabelPortMode(false);
			colLabelView.setLabelPortMode(false);
		} else {
			rowLabelView.setLabelPortFlankMode(true);
			colLabelView.setLabelPortFlankMode(true);
		}

		if(rowLabelView.hasMouse) {
			//Will more than 1 label be drawn in port mode?
			boolean whizOverOne = !rowLabelView.isLabelPortFlankMode() ||
				rowLabelView.getMaxLabelPortFlankSize() > 0;
			//If whizeOverOne is true, we want setKeepTreeGlobal to be false so
			//that the tree links to the labels
			interactiveYmap.setKeepTreeGlobal(!whizOverOne);
		} else if(colLabelView.hasMouse) {
			//Will more than 1 label be drawn in port mode?
			boolean whizOverOne = !colLabelView.isLabelPortFlankMode() ||
				colLabelView.getMaxLabelPortFlankSize() > 0;
			//If whizeOverOne is true, we want setKeepTreeGlobal to be false so
			//that the tree links to the labels
			interactiveXmap.setKeepTreeGlobal(!whizOverOne);
		}

		rowLabelView.storeState();
		colLabelView.storeState();
	}

	/** Starts highlighting the hovered row and column */
	private class BothHoverStartAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			setRowColumnHoverHighlight(true);
		}
	}

	/** Starts highlighting the hovered row */
	private class RowHoverStartAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			if(!imView.getYMap().isSelecting()) {
				setRowHoverHighlight(true);
			}
		}
	}

	public void setRowHoverHighlight(boolean hc) {
		imView.setRowHoverHighlight(hc);
		imView.repaint();
	}

	/** Starts highlighting the hovered row */
	private class RowHoverStopAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			setRowHoverHighlight(false);
		}
	}

	/** Starts highlighting the hovered column */
	private class ColumnHoverStartAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			if(!imView.getXMap().isSelecting()) {
				setColumnHoverHighlight(true);
			}
		}
	}

	public void setColumnHoverHighlight(boolean hc) {
		imView.setColumnHoverHighlight(hc);
		imView.repaint();
	}

	public void setRowColumnHoverHighlight(boolean hc) {
		imView.setRowHoverHighlight(hc);
		imView.setColumnHoverHighlight(hc);
		imView.repaint();
	}

	/** Starts highlighting the hovered column */
	private class ColumnHoverStopAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			setColumnHoverHighlight(false);
		}
	}

	/** Recalculates proportions for the MapContainers, when the layout was
	 * changed by removing or adding components, or resizing the TVFrame. Only
	 * works if GlobalView is already resized (has availablePixels set to new
	 * value!). */
	public void resetMatrixViews() {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				imView.resetView();
				gmView.resetView();
			}
		});
	}

	private class MatrixMouseWheelListener implements MouseWheelListener {

		private boolean reverseZoomDirection;

		public MatrixMouseWheelListener() {
			super();
			/* TODO: Doing the system preferences check here is efficient
			 * because we're not reading a file on every scroll event, however
			 * if the user edits the system preference for natural scroll while
			 * TreeView is running, scroll-zooming will get reversed.  Figure
			 * out an efficient way to capture scroll-direction behavior changes
			 * while running. */
			setReverseZoomDirection(isNaturalScroll());
		}

		public void setReverseZoomDirection(final boolean reverse) {
			this.reverseZoomDirection = reverse;
		}

		public boolean onAMac() {
			boolean onamac = false;
			try {
				/* TODO: Figure out a way to get the OS from where it was
				 * determined in app.TreeView3.java instead of replicating that code
				 * here. */
				onamac = System	.getProperty("os.name").toLowerCase()
					.startsWith("mac os x");
			}
			catch(Exception ex) {
				LogBuffer.println("Failed to determine os: " + ex.getMessage());
			}
			return(onamac);
		}

		public boolean isNaturalScroll() {

			boolean natural = false;
			try {
				if(onAMac()) {
					File globalPref = new File(System.getProperty("user.home") +
						"/Library/Preferences/.GlobalPreferences.plist");

					NSDictionary dict =
						(NSDictionary) PropertyListParser.parse(globalPref);

					NSNumber pref = (NSNumber) dict.objectForKey(
						"com.apple.swipescrolldirection");

					if(pref.boolValue()) {
						natural = true;
					}
				}
			}
			catch(Exception ex) {
				LogBuffer.println("Failed to parse plist: " + ex.getMessage());
			}

			return(natural);
		}

		/**
		 * Zooming when the mouse wheel is used in conjunction with the alt/
		 * option key. Vertical scrolling if the shift key is not pressed.
		 */
		@Override
		public void mouseWheelMoved(final MouseWheelEvent e) {

			if(!imView.hasMouse()) { return; }

			final int notches = e.getWheelRotation();
			final int shift = notches;
			final double scroll_ratio = 0.01;

			//On macs' magic mouse, horizontal scroll comes in as if the shift
			//was down
			if(e.isAltDown()) {
				if((!reverseZoomDirection && notches < 0) ||
					(reverseZoomDirection && notches > 0)) {

					//This ensures we only zoom toward the cursor when the
					//cursor is over the map
					if(imView.hasMouse()) {
						imView.smoothZoomTowardPixel(e.getX(), e.getY());
					}
					//This should happen when the mouse is not over the heatmap
					else {
						interactiveXmap.zoomInBegin();
						interactiveYmap.zoomInBegin();
					}
				}
				else {
					if(imView.hasMouse()) {
						imView.smoothZoomFromPixel(e.getX(), e.getY());
					}
					else {
						interactiveXmap.zoomOutBegin();
						interactiveYmap.zoomOutBegin();
					}
				}
			}
			else if(e.isShiftDown()) {
				interactiveXmap.scrollBy(shift * (int) Math.ceil(scroll_ratio *
					interactiveXmap.getNumVisible()));
				//Now we are hovered over a new index
				interactiveXmap.setHoverIndex(interactiveXmap.getIndex(
					e.getX()));

			}
			else {
				interactiveYmap.scrollBy(shift * (int) Math.ceil(scroll_ratio *
					interactiveYmap.getNumVisible()));
				//Now we are hovered over a new index
				interactiveYmap.setHoverIndex(interactiveYmap.getIndex(
					e.getY()));
			}

			imView.repaint(SCROLL_REPAINT_INTERVAL_MS);
		}
	}


	/** Selecting a rectangular area in InteractiveMatrixView
	 *
	 * @param start A Point representing the first index of each axis to be
	 *          selected.
	 * @param end A Point representing the last index of each axis to be
	 *          selected. */
	public void selectRectangle(final Point start, final Point end) {

		// sort so that ep is upper left corner
		if(end.x < start.x) {
			final int x = end.x;
			end.x = start.x;
			start.x = x;
		}

		if(end.y < start.y) {
			final int y = end.y;
			end.y = start.y;
			start.y = y;
		}

		rowSelection.selectNewIndexRange(start.y, end.y);
		colSelection.selectNewIndexRange(start.x, end.x);

		rowSelection.notifyObservers();
		colSelection.notifyObservers();
	}

	/** Empties the selection for both axis selection objects. */
	public void deselectAll() {

		rowSelection.deselectAllIndexes();
		colSelection.deselectAllIndexes();

		rowSelection.notifyObservers();
		colSelection.notifyObservers();
	}

	/** Zooms on a selection. The type of zooming is dependent on input
	 * modifiers used when executing the zoom.
	 * 
	 * @param modifiers Input modifiers when pressing a button. */
	public void zoomOnSelection(final int modifiers) {

		final boolean rowsSelected = rowSelection.getNSelectedIndexes() > 0;

		if(rowsSelected) {
			if((swap_animation_modifiers &&
				(modifiers & InputEvent.SHIFT_MASK) == 0 &&
				(modifiers & InputEvent.META_MASK) == 0) ||
				(!swap_animation_modifiers &&
				((modifiers & InputEvent.SHIFT_MASK) != 0 ||
				(modifiers & InputEvent.META_MASK) != 0))) {

				// Zoom in (or out)
				interactiveXmap.zoomToSelected(colSelection.getMinIndex(),
					colSelection.getMaxIndex());
				interactiveYmap.zoomToSelected(rowSelection.getMinIndex(),
					rowSelection.getMaxIndex());

				// Then scroll
				interactiveXmap.scrollToFirstIndex(colSelection.getMinIndex());
				interactiveYmap.scrollToFirstIndex(rowSelection.getMinIndex());

			}
			else if((modifiers & InputEvent.ALT_MASK) != 0) {
				imView.smoothZoomTowardSelection(colSelection.getMinIndex(),
					(colSelection.getMaxIndex() - colSelection.getMinIndex() +
						1), rowSelection.getMinIndex(),
						(rowSelection.getMaxIndex() -
							rowSelection.getMinIndex() + 1));
			}
			else {
				imView.smoothAnimatedZoomToTarget(colSelection.getMinIndex(),
					(colSelection.getMaxIndex() - colSelection.getMinIndex() +
						1), rowSelection.getMinIndex(),
						(rowSelection.getMaxIndex() -
							rowSelection.getMinIndex() + 1));
			}
		}
	}

	public void setDataTicker(final DataTicker ticker) {

		this.ticker = ticker;
	}

	/** A wrapper for retrieving a data value from the data model. Used by
	 * IMVMouseAdapter to update the DataTicker in DendroView when hovering
	 * over the InteractiveMatrixView.
	 * 
	 * @param rowIdx Row index of the data value in the model's data matrix.
	 * @param colIdx Col index of the data value in the model's data matrix.
	 * @return The data value at the specified indices, if it exists.
	 *         DataModel.NAN otherwise. */
	public void setDataValueAt(final int rowIdx, final int colIdx) {
		ticker.setText("Data Value:");
		ticker.setValue(model.getDataMatrix().getValue(colIdx, rowIdx));
	}
	
	/**
	 * Scrolls to index i in the y-MapContainer
	 *
	 * @param i */
	public void scrollToGene(final int i) {

		interactiveYmap.scrollToIndex(i);
		interactiveYmap.notifyObservers();
	}

	/** Scrolls to index i in the x-MapContainer.
	 *
	 * @param i */
	public void scrollToArray(final int i) {

		interactiveXmap.scrollToIndex(i);
		interactiveXmap.notifyObservers();
	}

	/** Notifies both MatrixViews that the data has updated, so it can
	 * recalculate pixel color values during the next update. */
	public void updateMatrixPixels() {

		imView.setDataChanged();
		gmView.setDataChanged();
	}

	/** Assigns references of MapContainer instances to be used for
	 * interactivity and information display in InteractiveMatrixView.
	 * 
	 * @param interactiveXmap
	 * @param interactiveYmap */
	public void setInteractiveMapContainers(final MapContainer interactiveXmap,
		final MapContainer interactiveYmap) {

		this.interactiveXmap = interactiveXmap;
		this.interactiveYmap = interactiveYmap;

		interactiveXmap.addObserver(this);
		interactiveYmap.addObserver(this);

		imView.setXMap(interactiveXmap);
		imView.setYMap(interactiveYmap);
	}

	/** Assigns references of MapContainer instances to be used for
	 * information display in GlobalMatrixView.
	 * 
	 * @param xmap
	 * @param ymap */
	public void setGlobalMapContainers(final MapContainer xmap,
		final MapContainer ymap) {

		this.globalXmap = xmap;
		this.globalYmap = ymap;

		globalXmap.addObserver(this);
		globalYmap.addObserver(this);

		gmView.setXMap(xmap);
		gmView.setYMap(ymap);
	}

	/** Set rowSelection
	 *
	 * @param rowSelection
	 *          The TreeSelection which is set by selecting genes in the
	 *          GlobalView */
	public void setRowSelection(final TreeSelectionI rowSelection) {

		if(this.rowSelection != null) {
			this.rowSelection.deleteObserver(this);
		}

		this.rowSelection = rowSelection;
		this.rowSelection.addObserver(this);

		imView.setRowSelection(rowSelection);
		gmView.setRowSelection(rowSelection);
	}

	/** Set colSelection
	 *
	 * @param colSelection
	 *          The TreeSelection which is set by selecting arrays in the
	 *          GlobalView */
	public void setColSelection(final TreeSelectionI colSelection) {

		if(this.colSelection != null) {
			this.colSelection.deleteObserver(this);
		}

		this.colSelection = colSelection;
		this.colSelection.addObserver(this);

		imView.setColSelection(colSelection);
		gmView.setColSelection(colSelection);
	}

	/** Assigns scrollbars to MapContainers.
	 * 
	 * @param rowScroll Scrollbar for scrolling through matrix rows.
	 * @param colScroll Scrollbar for scrolling through matrix columns. */
	public void setScrollBars(final JScrollBar rowScroll,
		final JScrollBar colScroll) {

		interactiveXmap.setScrollbar(colScroll);
		interactiveYmap.setScrollbar(rowScroll);
	}

	/** Notifies all MapContainers' observers. */
	public void notifyAllMapObservers() {

		globalXmap.notifyObservers();
		globalYmap.notifyObservers();

		interactiveXmap.notifyObservers();
		interactiveYmap.notifyObservers();
	}

	/** Access a reference of the color extractor which determines how
	 * colors are displayed in relation to values.
	 * 
	 * @return The MatrixView's color extractor. */
	public ColorExtractor getColorExtractor() {

		return colorExtractor;
	}

	/** Evaluates current zoom status and returns an array of booleans, which
	 * can be used to test certain zoom conditions. This is for example relevant
	 * for the DendroView buttons. They will be enabled or disabled based
	 * on the zoom status.
	 * 
	 * @return Ordered boolean array of zoom statuses: [isXMin, isYMin,
	 *         atRight, atLeft, atTop, atBottom, isSelectionZoomed] */
	public boolean[] getZoomStatusForButtons() {

		boolean[] zoomStatus;

		/* Determine if either MapContainer is at minimum scale */
		boolean isXMin = interactiveXmap.showsAllTiles();
		boolean isYMin = interactiveYmap.showsAllTiles();

		int xTilesVisible = interactiveXmap.getNumVisible();
		int yTilesVisible = interactiveYmap.getNumVisible();

		final boolean rowsSelected = rowSelection.hasSelection();
		final boolean colsSelected = colSelection.hasSelection();

		// Note: A selection is "fully zoomed" if there is no selection - this
		// will disable the zoom selection button
		boolean isSelectionZoomed = (!rowsSelected && !colsSelected) ||
			(rowsSelected && rowSelection.getMinIndex() ==
			interactiveYmap.getFirstVisible() &&
			(rowSelection.getFullSelectionRange()) == yTilesVisible &&
			colsSelected &&
			colSelection.getMinIndex() == interactiveXmap.getFirstVisible() &&
			(colSelection.getFullSelectionRange()) == xTilesVisible);

		zoomStatus = new boolean[] {isXMin, isYMin, isSelectionZoomed};

		return zoomStatus;
	}
}
