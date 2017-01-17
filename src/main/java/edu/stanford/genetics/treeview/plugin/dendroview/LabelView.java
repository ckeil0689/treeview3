package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.util.Observable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.BoundedRangeModel;
import javax.swing.JDialog;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import Controllers.RegionType;
import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.LabelInfo;
import edu.stanford.genetics.treeview.LabelSummary;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.UrlExtractor;

public abstract class LabelView extends ModelView implements MouseListener,
	MouseMotionListener,ConfigNodePersistent,MouseWheelListener,
	AdjustmentListener {

	private static final long serialVersionUID = 1L;

	protected final static int HINTFONTSIZE = 14;

	// This is the space between labels in pixels
	protected final static int SQUEEZE = 1;

	/* Selection of row/ column indices */
	protected final Color textFGColor = Color.black;
	protected Color textBGColor;
	protected final Color selectionBorderColor = Color.yellow;
	protected final Color selectionTextBGColor = new Color(249,238,160);// yel
	protected final Color selectionTextFGColor = Color.black;
	protected final Color hoverTextFGColor = Color.red;
	protected final Color labelPortColor = new Color(30,144,251); // blu

	/* DataModel is an observer */
	protected DataModel dataModel;

	/* Required label data */
	protected LabelInfo labelInfo;
	protected LabelSummary labelSummary;

	/* Maps label position to GlobalView */
	protected MapContainer map;

	/* Use labels to generate URLs for look-up */
	protected UrlExtractor urlExtractor;

	protected LabelAttributes labelAttr;

	/*
	 * Stores a reference to the TreeSelection relevant for drawing and the
	 * opposing selection axis (for de-selecting purposes)
	 */
	protected TreeSelectionI drawSelection; // e.g. geneSelection
	protected TreeSelectionI otherSelection; // e.g. arraySelection

	/* Contains all the saved information */
	protected Preferences configNode;

	protected JScrollPane scrollPane;
	protected String zoomHint;

	/* "Position indicator" settings for when the label port is active */
	// matrixBarThickness - the thickness of the matrix position bar. Must be
	// an odd number
	// labelIndicatorProtrusion - the distance the label hover indicator
	// protrudes from matrix bar, i.e. the tip of the
	// indicator that points at a label
	// matrixIndicatorIntrusion - the distance the label hover indicator
	// intrudes into the matrix bar, i.e. the tip of
	// the indicator that points at a matrix row/
	// column
	// labelIndent - the distance label starts after end of the tip
	// of the label indicator
	// indicatorThickness - the entire area that the matrix/position
	// indicator takes up. It's sort of like a scroll
	// bar always drawn against the edge of the matrix
	int matrixBarThickness = 3;
	int labelIndicatorProtrusion = 0;
	int labelIndent = 3;
	int matrixIndicatorIntrusion =
		((labelIndicatorProtrusion > matrixBarThickness) ||
			(labelIndicatorProtrusion == 0) ?
				(int) Math.ceil(matrixBarThickness /
					2) : labelIndicatorProtrusion);
	int indicatorThickness = matrixBarThickness + labelIndicatorProtrusion +
		labelIndent;

	// The following variables help keep track of where the labels have been
	// scrolled to (along the length of the label strings). Since the label view
	// changes on hover in/out and upon zoom and possibly upon window resize, we
	// track scroll position to make updates intelligently to prevent user
	// annoyance/disorientation. -1 = unset.
	int lastScrollEndGap = -1;
	int lastScrollPos = -1;
	int lastScrollEndPos = -1;
	int secondaryPaneSize = -1;
	int secondaryViewportSize = -1;

	/*
	 * When mouse is moved on the label, this method is called to update data
	 * ticker value
	 */
	protected abstract void setDataTickerValue(final MouseEvent e);

	/*
	 * TODO: Instead of resetting the justification position whenever the font
	 * size changes, we should calculate and remember the relative position of
	 * the scrollbar
	 */

	public LabelView() {

		super();

		setLayout(new MigLayout());

		setLabelSummary(new LabelSummary(getSummaryName()));
		labelAttr = new LabelAttributes(this);
		// this.urlExtractor = uExtractor;

		addMouseMotionListener(this);
		addMouseListener(this);

		setLabelPortMode(true);

		/*
		 * This needs to be NEVER because the label scrollbars have to be
		 * manually added rather than automatically via JScrollPane. The main
		 * matrix scrollbars have also been manually added using MigLayout.
		 * Otherwise alignment cannot be guaranteed without setting explicit
		 * pixel sizes which should rarely if ever be done. - Chris
		 */
		scrollPane = new JScrollPane(this,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		scrollPane.setBorder(null);

		debug = 0;
		// Debug modes:
		// 5 = Debug bold font rendering issues
		// 9 = Debug repaint timer intervals and updates to label panels
		// 10 = Debug label drawing issues when split pane divider adjusted
		// 12 = Debug issues where strings and indicator bar are starting in the
		// wrong place
		// 13 = Debug negative label start positions when right-justified and
		// split pane divider position's resultant label viewport size is
		// slightly smaller than the longest label(s)
		// 14 = Debug position indicator not always drawn when data is large
		// (number of rows per pixel > 1).
		// 15 = Debug overrun arrow position in non-label-port mode
		// 16 = Debug issue where short top-justified column labels are not
		// visible after dragging the split pane divider to make the label
		// area larger
		// 17 = Debug row label highlight drawing position
		// 18 = Debug the lastScrollRowEndPos value
		// 19 = Debug the blanking out of scrolled labels
		// 20 = Debug whether the label offset calculations yield negatives
		// 21 = Test whether the pixel index determined by 2 different methods
		// is
		// the same
		// 22 = Debug the ChangeListener attached to the secondary scrollbar
		// 23 = Debug tree-hover label coloring & option-click deselect when
		// nothing is selected
		// 24 = Debug the lightening of the non-label-port labels

		panel = scrollPane;

		addMouseWheelListener(this);

		getSecondaryScrollBar().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent e) {
				debug("The mouse has entered a row label pane scrollbar",2);
				if(overScrollLabelPortOffTimer != null) {
					/* Event came too soon, swallow by resetting the timer.. */
					overScrollLabelPortOffTimer.stop();
					overScrollLabelPortOffTimer = null;
				}
				if(map.wasLastTreeModeGlobal() && map.shouldKeepTreeGlobal()) {
					map.setKeepTreeGlobal(true);
				}
				setPrimaryHoverIndex(map.getMaxIndex());
				map.setOverLabelsScrollbar(true);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				debug("The mouse has exited a row label pane scrollbar",2);
				// Turn off the "over a label port view" boolean after a bit
				if(overScrollLabelPortOffTimer == null) {
					if(labelPortOffDelay == 0) {
						map.setOverLabelsScrollbar(false);
						map.notifyObservers();
						repaint();
					}
					else {
						/*
						 * Start waiting for delay millis to elapse and then
						 * call actionPerformed of the ActionListener
						 * "paneLabelPortOffListener".
						 */
						overScrollLabelPortOffTimer = new Timer(
							labelPortOffDelay,
							scrollLabelPortOffListener);
						overScrollLabelPortOffTimer.start();
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if(map.wasLastTreeModeGlobal() && map.shouldKeepTreeGlobal()) {
					map.setKeepTreeGlobal(true);
				}
				map.setLabelsBeingScrolled(true);
				debug("The mouse has clicked a row label scrollbar",2);
				if(activeScrollLabelPortOffTimer != null) {
					/* Event came too soon, swallow by resetting the timer.. */
					activeScrollLabelPortOffTimer.stop();
					activeScrollLabelPortOffTimer = null;
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				debug("The mouse has released a row label scrollbar",2);
				// Turn off the "over a label port view" boolean after a bit
				if(activeScrollLabelPortOffTimer == null) {
					if(labelPortOffDelay == 0) {
						map.setLabelsBeingScrolled(false);
						map.notifyObservers();
						repaint();
					}
					else {
						/*
						 * Start waiting for delay millis to elapse and then
						 * call actionPerformed of the ActionListener
						 * "paneLabelPortOffListener".
						 */
						activeScrollLabelPortOffTimer = new Timer(
							labelPortOffDelay,
							activeLabelPortOffListener);
						activeScrollLabelPortOffTimer.start();
					}
				}
			}
		});

		// Listen for value changes in the scroll pane's scrollbars
		getSecondaryScrollBar().addAdjustmentListener(this);

		// The following live-updates the labels during a scroll handle drag
		class ScrollChangeListener implements ChangeListener {

			@Override
			public void stateChanged(ChangeEvent e) {
				Object source = e.getSource();
				if(source instanceof BoundedRangeModel) {
					BoundedRangeModel aModel = (BoundedRangeModel) source;
					if(aModel.getValueIsAdjusting()) {
						debug("Scroll position changed.",22);
						explicitSecondaryScrollTo(aModel.getValue(),-1,-1);
						repaint();
					}
				}
			}
		}
		getSecondaryScrollBar().getModel().addChangeListener(
			new ScrollChangeListener());
	}

	// TODO add javaDoc to all methods.
	protected abstract void setLabelPaneSize(int offscreenPrimarySize,
		int offscreenSecondarySize);

	protected abstract int getSecondaryPaneSize(final Dimension dims);

	protected abstract int getPrimaryPaneSize(final Dimension dims);

	protected abstract void setSecondaryPaneSize(final Dimension dims,int Size);

	@Override
	public void resetDefaults() {

		super.resetDefaults();

		lastScrollEndGap = -1;
		lastScrollPos = -1;
		lastScrollEndPos = -1;
		secondaryPaneSize = -1;
		secondaryViewportSize = -1;

		if(labelAttr != null) {
			labelAttr.resetDefaults();
		}

		if(labelSummary != null) {
			labelSummary.resetDefaults();
		}
	}

	/**
	 * This method returns false if the start of the label coordinates and the
	 * start of the scroll coordinates are both 0. It returns true if the label
	 * start coord is 0 and the scroll start coord is the end of the longest
	 * label (plus the labelShift). It depends on how the content of the label
	 * pane is rotated: if the start of the label is at the bottom of the pane
	 * and the label reads vertically upward, then the coordinates of the label
	 * versus the scroll will be opposite.
	 *
	 * @return
	 */
	protected abstract boolean labelAndScrollCoordsAreOpposite();

	/**
	 * Returns a summary string of what the labels represent (e.g. a row or
	 * column summary)
	 *
	 * @return String
	 */
	protected abstract String getSummaryName();

	/**
	 * Used to space the array names.
	 *
	 * @param im
	 *            A new mapcontainer.
	 * @param m2 The other orthogonal map, used only once while calculating
	 *            Zoomed Averages
	 */
	public void setMap(final MapContainer im) {
		if(map != null) {
			map.deleteObserver(this);
		}

		map = im;
		map.addObserver(this);

		revalidate();
		repaint();
	}

	public void setLabelInfo(final LabelInfo labelInfo) {

		this.labelInfo = labelInfo;

		if(labelSummary == null) {
			LogBuffer.println("Could not update labels for labelSummary in " +
				getName());
			return;
		}

		labelSummary.setLabelTypes(labelInfo.getLabelTypes());
	}

	public LabelInfo getLabelInfo() {
		return labelInfo;
	}

	public void setLabelSummary(final LabelSummary newLabelSummary) {
		labelSummary = newLabelSummary;
		labelSummary.addObserver(this);
	}

	public LabelSummary getLabelSummary() {
		return labelSummary;
	}

	public void setUrlExtractor(final UrlExtractor urlExtractor) {
		this.urlExtractor = urlExtractor;
	}

	public UrlExtractor getUrlExtractor() {
		return urlExtractor;
	}

	public void setDrawSelection(final TreeSelectionI drawSelection) {
		if(this.drawSelection != null) {
			this.drawSelection.deleteObserver(this);
		}

		this.drawSelection = drawSelection;
		this.drawSelection.addObserver(this);
	}

	public void setOtherSelection(final TreeSelectionI otherSelection) {
		if(this.otherSelection != null) {
			this.otherSelection.deleteObserver(this);
		}
		this.otherSelection = otherSelection;
		this.otherSelection.addObserver(this);
	}

	@Override
	public void update(final Observable o,final Object arg) {
		// update method not used, no updates from observing another object...
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if(parentNode == null) {
			LogBuffer.println("parentNode for LabelView was null.");
			return;
		}

		configNode = parentNode;
		getLabelSummary().setConfigNode(configNode);
		getLabelAttributes().setConfigNode(configNode);
	}

	@Override
	public Preferences getConfigNode() {

		return configNode;
	}

	@Override
	public void requestStoredState() {

		if(configNode == null) {
			LogBuffer.println("Could not synchronize state for " + getName() +
				" because configNode was null.");
			return;
		}

		if(labelAttr == null) {
			LogBuffer.println("Could not restore labelAttributes state.");
			return;
		}

		labelAttr.requestStoredState();

		resetSecondaryScroll();

		if(labelSummary == null) {
			LogBuffer.println("Could not restore labelSummary state.");
			return;
		}

		labelSummary.requestStoredState();
	}

	@Override
	public void storeState() {

		labelAttr.storeState();
		resetSecondaryScroll();
	}

	/**
	 * Imports a state from a given Preferences node.
	 *
	 * @param node
	 *            - the Preferences node from which to import the current state.
	 */
	@Override
	public void importStateFrom(final Preferences node) {

		labelAttr.importStateFrom(node);

		// Signal that the secondary scroll position should be reset the next
		// time it is needed
		resetSecondaryScroll();

		try {
			Preferences summaryNode;
			if(node.nodeExists("RowSummary")) {
				summaryNode = node.node("RowSummary");

			}
			else if(node.nodeExists("ColSummary")) {
				summaryNode = node.node("ColSummary");

			}
			else {
				summaryNode = null;
			}

			getLabelSummary().importStateFrom(summaryNode);

		}
		catch(BackingStoreException e) {
			LogBuffer.logException(e);
			LogBuffer.println("Skipping import of selected labels.");
			return;
		}
	}

	/** @return The LabelView's instance of LabelAttributes. */
	public LabelAttributes getLabelAttributes() {

		return labelAttr;
	}

	/**
	 * Rotates the pane content as necessary
	 *
	 * @return void
	 * @param g2d
	 */
	abstract public void orientLabelPane(Graphics2D g2d);

	abstract public void orientLabelsForExport(Graphics2D g2d,
		final int xIndent,final int yIndent,final int longestXLabelLen);

	abstract public void orientHintPane(Graphics2D g2d);

	/**
	 * Access a LabelView's main scrollBar.
	 *
	 * @return The horizontal scrollbar for gene labels, the vertical scrollbar
	 *         for array labels.
	 */
	protected abstract JScrollBar getSecondaryScrollBar();

	protected abstract JScrollBar getPrimaryScrollBar();

	protected abstract void setHoverPosition(final MouseEvent e);

	protected abstract int getPrimaryViewportSize();

	protected abstract int getSecondaryViewportSize();

	abstract public int determineCursorPixelIndex(Point p);

	/**
	 * This is for updating the hover index when the mouse has not moved but the
	 * data under it has (like when using the scroll wheel)
	 */
	public void updatePrimaryHoverIndexDuringScrollDrag() {
		// If the labels are being scrolled, you must manually retrieve the
		// cursor position
		if(map.areLabelsBeingScrolled()) {
			forceUpdatePrimaryHoverIndex();
		}
	}

	public void setTemporaryPoints(final int i) {
		debug("Setting temporary points",2);
		setFont(new Font(labelAttr.getFace(),labelAttr.getStyle(),i));
	}

	@Override
	public String viewName() {
		return "LabelView";
	}

	// This is an attempt to get the hovering of the mouse over the matrix to
	// get
	// the label panes to update more quickly and regularly, as the
	// notifyObservers method called from MapContainer was resulting in sluggish
	// updates
	private int repaintInterval = 50; // update every 50 milliseconds
	/*
	 * TODO: If anastasia doesn't like trees linked to whizzing labels,
	 * uncomment the following commented code. If she likes it, delete. This
	 * code saves compute cycles, but makes the trees sometimes move out of sync
	 * with the labels.
	 */
	// private int slowRepaintInterval = 1000;//update every 1s if mouse not
	// moving
	private int lastHoverIndex = -1;
	private Timer repaintTimer = new Timer(repaintInterval,
		new ActionListener() {

		/**
		 * The timer "ticks" by calling this method every _timeslice
		 * milliseconds
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			// This shouldn't be necessary, but when I change setPoints() to
			// setTemporaryPoints in the drawing of the HINT, the timer never
			// stops despite stop being continually called, so I'm going to
			// call stop in here if the map says that the animation is
			// supposed to have been stopped...
			if(!map.isLabelAnimeRunning()) {
				repaintTimer.stop();
			}
			repaint();
		}
	});

	/*
	 * TODO: If anastasia doesn't like trees linked to whizzing labels,
	 * uncomment the following commented code. If she likes it, delete. This
	 * code saves compute cycles, but makes the trees sometimes move out of sync
	 * with the labels.
	 */
	// //Timer to wait a bit before slowing down the slice _timer for painting.
	// //This conserves processor cycles in the interests of performance. Note
	// //that there is a pair of timers for each axis.
	// final private int delay = 1000;
	// private javax.swing.Timer slowDownRepaintTimer;
	// ActionListener slowDownRepaintListener = new ActionListener() {
	//
	// @Override
	// public void actionPerformed(ActionEvent evt) {
	// if(evt.getSource() == slowDownRepaintTimer) {
	// /* Stop timer */
	// slowDownRepaintTimer.stop();
	// slowDownRepaintTimer = null;
	//
	// //If we are still over a label port view panel, just slow the
	// //repaint timer, because this was triggered by the mouse not
	// //moving
	// if(map.overALabelPortLinkedView()) {
	//// debug("Slowing the repaint interval presumably because " +
	//// "of lack of mouse movement",9);
	//// repaintTimer.setDelay(slowRepaintInterval);
	// } else {
	// repaintTimer.stop();
	// map.setLabelAnimeRunning(false);
	// }
	// }
	// }
	// };

	/* inherit description */
	@Override
	public void updateBuffer(final Graphics g) {

		updateBuffer(g,offscreenSize);
	}

	public void updateBuffer(final Image buf) {

		updateBuffer(buf.getGraphics(),new Dimension(buf.getWidth(null),
			buf.getHeight(null)));
	}

	public void updateLabelRepaintTimers() {
		// If the mouse is not hovering over the IMV, stop both timers, set the
		// last hover index, and tell mapcontainer that the animation has
		// stopped
		if(!map.overALabelLinkedView()) {
			if((repaintTimer != null) && repaintTimer.isRunning()) {
				debug("Not hovering over a label port linked view - stopping " +
					"animation",9);
				repaintTimer.stop();
				lastHoverIndex = -1;
				map.setLabelAnimeRunning(false);
				/*
				 * TODO: If anastasia doesn't like trees linked to whizzing
				 * labels, uncomment the following commented code. If she likes
				 * it, delete. This code saves compute cycles, but makes the
				 * trees sometimes move out of sync with the labels.
				 */
				// //Disable the turnOffRepaintTimer if it is running, because
				// we've
				// //already stopped repaints
				// if(slowDownRepaintTimer != null) {
				// slowDownRepaintTimer.stop();
				// slowDownRepaintTimer = null;
				// }
			}
			else {
				debug("The repaint timer is not running. This updateBuffer " +
					"call was initiated by something else.",9);
			}
		}
		// Else, assume the mouse is hovering, and if the animation is not
		// running, start it up
		else if(!map.isLabelAnimeRunning()) {
			if((repaintTimer == null) || !repaintTimer.isRunning()) {
				debug("Hovering across matrix - starting up animation",9);
				repaintTimer.start();
				map.setLabelAnimeRunning(true);
				lastHoverIndex = getPrimaryHoverIndex();
				/*
				 * TODO: If anastasia doesn't like trees linked to whizzing
				 * labels, uncomment the following commented code. If she likes
				 * it, delete. This code saves compute cycles, but makes the
				 * trees sometimes move out of sync with the labels.
				 */
				// //Disable any slowDownRepaintTimer that might have been left
				// over
				// if(slowDownRepaintTimer != null) {
				// slowDownRepaintTimer.stop();
				// slowDownRepaintTimer = null;
				// }
			}
			else {
				debug("The repaint timer was in fact running even though " +
					"map.isLabelAnimeRunning() said it wasn't.",9);
			}
		}
		// Else if the mouse hasn't moved, start the second timer to slow down
		// the first after 1 second (this mitigates delays upon mouse motion
		// after a brief period of no motion)
		else if(map.overALabelLinkedView() &&
			(getPrimaryHoverIndex() == lastHoverIndex)) {
			/*
			 * TODO: If anastasia doesn't like trees linked to whizzing labels,
			 * uncomment the following commented code. If she likes it, delete.
			 * This code saves compute cycles, but makes the trees sometimes
			 * move out of sync with the labels.
			 */
			// if(repaintTimer.getDelay() == repaintInterval) {
			// debug("Hovering on one spot [" + lastHoverIndex +
			// "] - slowing animation",9);
			// if(slowDownRepaintTimer == null) {
			// slowDownRepaintTimer = new Timer(delay,slowDownRepaintListener);
			// slowDownRepaintTimer.start();
			// }
			// } else {
			// debug("Animation already slowed down to [" +
			// repaintTimer.getDelay() + "ms].",9);
			// }
		}
		// Else, disable the slowDownRepaintTimer, update the hover index, and
		// set the repaint interval to normal speed
		else {
			debug("Hovering across matrix - keeping animation going",9);
			debug("Last hover Index: [" + lastHoverIndex +
				"] current hover index [" +
				getPrimaryHoverIndex() + "]",9);
			/*
			 * TODO: If anastasia doesn't like trees linked to whizzing labels,
			 * uncomment the following commented code. If she likes it, delete.
			 * This code saves compute cycles, but makes the trees sometimes
			 * move out of sync with the labels.
			 */
			// if(repaintTimer != null && !repaintTimer.isRunning()) {
			// repaintTimer.start();
			// } else if(repaintTimer.getDelay() == slowRepaintInterval) {
			// debug("Speeding up the repaint interval because mouse " +
			// "movement detected",9);
			// repaintTimer.setDelay(repaintInterval);
			// repaintTimer.restart();
			// }
			// //Disable the slowDownRepaintTimer because we have detected
			// //continued mouse motion
			// if(slowDownRepaintTimer != null) {
			// slowDownRepaintTimer.stop();
			// slowDownRepaintTimer = null;
			// }
			lastHoverIndex = getPrimaryHoverIndex();
			map.setLabelAnimeRunning(true);
		}
	}

	protected abstract String getPaneType(); // returns "Row" or "Column" string

	public void updateBuffer(final Graphics g,
		final Dimension localOffscreenSize) {

		/*
		 * Manage future repaints with a pair of timers, because when
		 * MapContainer calls notifyObservers, repaints are choppy and
		 * sluggish...
		 *
		 * NOTE: Only call notifyObservers from MapContainer when
		 * map.labelAnimeRunning is false - otherwise, the repaints will be
		 * choppy & slow. The timers below will handle the smooth updates from
		 * here on out
		 */


		if(SwingUtilities.getWindowAncestor(this).isActive()) {
			forceUpdatePrimaryHoverIndex();
		}
		/*
		 * Gets the Current active Window (eg: export or treeview frame). It
		 * will return null if we are active in other application.
		 */
		Window activeWindow =
			javax.swing.FocusManager.getCurrentManager().getActiveWindow();
		if((activeWindow == null) && !isDialogonTop()) {
			forceUpdatePrimaryHoverIndex();
		}
		debug(getPaneType() + " forced hover index: [" +
			getPrimaryHoverIndex() +
			"] isOverIMV? [" + (map.isOverIMV() ? "yes" : "no") + "]",9);

		if(getSecondaryViewportSize() != getSavedSecondaryViewportSize()) {
			debug("Current visible " + getPaneType() + " label pane size: [" +
				getSecondaryViewportSize() + "] Stored: [" +
				getSavedSecondaryViewportSize() +
				"] secondary Content size: [" +
				getSecondaryPaneSize(localOffscreenSize) + "] Stored: [" +
				getSavedSecondaryPaneSize() + "]",10);
		}

		updateLabelRepaintTimers();

		debug("Updating the label pane graphics",1);
		debug("Hover index is [" + getPrimaryHoverIndex() + "]",9);

		/* Shouldn't draw if there's no TreeSelection defined */
		if(drawSelection == null) {
			LogBuffer.println("Error: drawSelection not defined. Can't draw " +
				"labels.");
			return;
		}

		final Graphics2D g2d = (Graphics2D) g;
		// Added this anti-aliasing setting to address an issue on Mac where
		// bold
		// labels were not being rendered as bold when certain fonts were at
		// certain sizes, e.g. Courier sizes 12, 18, and 24.
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
		final AffineTransform orig = g2d.getTransform();

		debug("Resetting justification for " + getPaneType() + "s!!!!",1);

		// If the scale can accommodate the minimum font size and can
		// accommodate a fixed-font-size (if the font has a fixed size) or the
		// label port is enabled
		if(doDrawLabels()) {
			// This is Changed in getMaxStringLength
			// Must set before either of those methods is called and use this
			// saved value to test if the font has changed since it was last
			// drawn
			int lastFontSize = labelAttr.getLastDrawnSize();
			updateFontSize(g);

			boolean drawLabelPort = doDrawLabelPort();
			final FontMetrics metrics = getFontMetrics(g.getFont());
			int realMaxStrLen = getMaxStringLength(metrics);
			int paneSizeShouldBe = getLabelPaneContentSize(metrics);
			int offscreenMatrixSize = map.getPixel(map.getTotalTileNum()) - 1 -
				map.getPixel(0);

			map.setWhizMode(drawLabelPort);

			// If the label pane's secondary dimension changed sizes or if the
			// font size has changed
			if(secondaryViewportSizeChanged() ||
				(lastFontSize != labelAttr.getPoints())) {
				debug("Viewport size change detected. Previous scroll " +
					"positions: lastScrollPos [" + lastScrollPos +
					"] lastScrollEndPos [" + lastScrollEndPos +
					"] lastScrollEndGap [" + lastScrollEndGap + "]",10);

				adjustSecondaryScroll(realMaxStrLen + getLabelShiftSize());

				debug("Viewport size change detected. New scroll positions: " +
					"lastScrollPos [" + lastScrollPos + "] lastScrollEndPos [" +
					lastScrollEndPos + "] lastScrollEndGap [" +
					lastScrollEndGap + "]",10);
			}

			debug("Pane size should be [" + paneSizeShouldBe + "]",17);
			trackSecondaryPaneSize(paneSizeShouldBe);

			/* Rotate plane for array axis (not for zoomHint) */
			orientLabelPane(g2d);

			textBGColor = g.getColor();

			/* Draw the labels */
			if(drawLabelPort) {
				debug("Drawing a label port",1);
				drawPortLabels(g,g2d);
			}
			else {
				drawFittedLabels(g,g2d);
			}
			labelAttr.saveLastDrawnFontDetails(realMaxStrLen);

			g2d.setTransform(orig);

			// Both the primary & secondary pane size change dynamically based
			// on
			// zoom level and whether or not the hint is displayed, so always
			// update it.
			setLabelPaneSize(offscreenMatrixSize,paneSizeShouldBe);

			// Scroll to the position equivalent to the previous position
			debug("calling secondaryReScroll with secondary pane size [" +
				paneSizeShouldBe + "] realMaxStrLen: [" + realMaxStrLen + "]",
				18);
			secondaryReScroll(paneSizeShouldBe);
		}
		else {
			debug("Label port NOT drawn",2);
			drawLabelHint(g,g2d);

			// Set the first & last visible label for the tree drawing positions
			map.setFirstVisibleLabel(-1);
			map.setLastVisibleLabel(-1);
			map.setWhizMode(false);
		}
	}

	/**
	 * return true if a Modal Dialog is on top/visible for this instance for
	 * this
	 * instance of treeview
	 *
	 * @return boolean whether a Modal dialog is top
	 */
	private static boolean isDialogonTop() {
		Window[] wins = Window.getWindows();
		boolean dialogOntop = false;
		for(Window windowInstance : wins) {
			if(windowInstance instanceof JDialog) {
				if(windowInstance.isVisible() &&
					((JDialog) windowInstance).isModal()) {
					dialogOntop = true;
					break;
				}
			}
		}
		return dialogOntop;
	}

	private int getSavedSecondaryPaneSize() {
		return(secondaryPaneSize);
	}

	private void drawPortLabels(final Graphics g,final Graphics2D g2d) {
		final int start = map.getFirstVisible();
		final int end = map.getLastVisible();
		final int colorIndex = labelInfo.getIndex("FGCOLOR");
		final Color fore = GUIFactory.MAIN;
		final FontMetrics metrics = getFontMetrics(g2d.getFont());
		final int ascent = metrics.getAscent();
		int offscreenMatrixSize = map.getPixel(map.getMaxIndex() + 1) - 1 - map
			.getPixel(0);
		Color bgColor;
		int minPortLabel = getPrimaryHoverIndex();
		int maxPortLabel = getPrimaryHoverIndex();
		int minPortLabelOffset = 0;
		int maxPortLabelOffset = 0;
		int hoverStyle = Font.BOLD | labelAttr.getStyle();

		// See if the labels are going to be offset because they are near
		// an edge
		int hoverYPos = map.getMiddlePixel(getPrimaryHoverIndex()) +
			(ascent / 2);
		int edgeOffset = 0;
		if((map.getMiddlePixel(getPrimaryHoverIndex()) + ascent) >
			getPrimaryViewportSize()) {
			edgeOffset = (map.getMiddlePixel(getPrimaryHoverIndex()) + ascent) -
				getPrimaryViewportSize();
		}
		else if((map.getMiddlePixel(getPrimaryHoverIndex()) - (int) Math
			.ceil(ascent /
				2)) < 0) {
			edgeOffset = map.getMiddlePixel(getPrimaryHoverIndex()) -
				(int) Math
				.ceil(ascent /
					2);
		}

		Color labelColor;
		// Draw labels from the hovered index backward to the start index
		// so that we show as many as there is room for, centered on
		// where the mouse is
		for(int j = getPrimaryHoverIndex();j >= start;j--) {
			try {
				String out = labelSummary.getSummary(labelInfo,j);
				final String[] labels = labelInfo.getLabels(j);

				if(out == null) {
					out = "No Label";
				}

				/* Set label color */
				if((j == getPrimaryHoverIndex()) ||
					((map.getHoverTreeMinIndex() > -1) &&
						(j >= map.getHoverTreeMinIndex()) &&
						(j <= map.getHoverTreeMaxIndex()))) {
					labelColor = hoverTextFGColor;
					debug("Label at index [" + j + "] is tree-hovered",23);
				}
				else {
					debug("Label at index [" + j + "] is NOT tree-hovered",23);
					if(drawSelection.isIndexSelected(j)) {
						labelColor = textFGColor;
						if(colorIndex > 0) {
							g.setColor(TreeColorer.getColor(labels[colorIndex]));
						}
						else {
							labelColor = selectionTextFGColor;
						}

					}
					else {
						labelColor = textFGColor;
					}
				}
				g2d.setColor(labelColor);

				/* Finally draw label (alignment-dependent) */
				int labelStrStart =
					getLabelStartOffset(metrics.stringWidth(out));
				debug("Label offset 1: [" + getLabelStartOffset(metrics
					.stringWidth(out)) +
					"]",11);

				int indexDiff = j - getPrimaryHoverIndex();
				int yPos = hoverYPos +
					(indexDiff * (labelAttr.getPoints() + SQUEEZE));
				// Account for offsets from being near an edge
				debug("edgeOffset: [" + edgeOffset + "]",3);
				yPos -= edgeOffset;
				if(yPos > (-ascent / 2)) {
					bgColor = drawLabelBackground(g,j,yPos - ascent);
					if(yPos >= ascent) {
						minPortLabel = j;
						minPortLabelOffset = yPos - ascent;
						/*
						 * TODO: If anastasia doesn't like labels being colored
						 * red on tree node hover, uncomment, otherwise delete.
						 */
						// if(j != getPrimaryHoverIndex() &&
						// (map.getHoverTreeMinIndex() == -1 ||
						// j < map.getHoverTreeMinIndex() ||
						// j > map.getHoverTreeMaxIndex())) {
						// labelColor = labelPortColor;
						// }
					}
					else if(indexDiff != 0) {
						debug("Lightening edge-labels",24);
						labelColor = lightenColor(labelColor);
					}
					g2d.setColor(labelColor);
					if(j == getPrimaryHoverIndex()) {
						// Just in case the hovered label is the right-most
						// label
						maxPortLabelOffset = getPrimaryViewportSize() -
							((yPos - ascent) + labelAttr.getPoints());
						debug("Drawing " + getPaneType() +
							" hover font BOLD [" +
							hoverStyle + "].",5);
						g2d.setFont(new Font(labelAttr.getFace(),hoverStyle,
							labelAttr
							.getPoints()));
					}
					else {
						g2d.setFont(labelAttr.getFont());
					}

					debug("Printing label [" + out +
						"] starting at labelStrStart [" +
						labelStrStart + "] (originally [" + labelStrStart +
						"] before offset for right-just.) and length [" +
						metrics
						.stringWidth(out) +
						"], which has been offset by [" + (isMatrixJustified()
							? getLabelShift()
								: 0) +
								"] and is inside the offscreen pane size [" +
								getLabelPaneContentSize(metrics) +
								"] Saved secondaryPaneSize: [" +
								getSavedSecondaryPaneSize() +
								"]",13);

					g2d.drawString(out,labelStrStart,yPos);

					drawOverrunArrows(metrics.stringWidth(out),g,yPos -
						ascent,labelAttr.getPoints(),g2d.getColor(),bgColor,
						labelStrStart);
				}
			}
			catch(final java.lang.ArrayIndexOutOfBoundsException e) {
				debug("There was a problem setting the font to bold",4);
				LogBuffer.logException(e);
				break;
			}
		}
		// For hovering over the left-most index, we must do this:
		g2d.setFont(labelAttr.getFont());
		for(int j = getPrimaryHoverIndex() + 1;j <= end;j++) {
			try {
				String out = labelSummary.getSummary(labelInfo,j);
				final String[] labels = labelInfo.getLabels(j);

				if(out == null) {
					out = "No Label";
				}

				/* Set label color */
				if((map.getHoverTreeMinIndex() > -1) &&
					(j >= map.getHoverTreeMinIndex()) &&
					(j <= map.getHoverTreeMaxIndex())) {

					labelColor = hoverTextFGColor;
					debug("Label at index [" + j + "] is tree-hovered",23);
				}
				else if(drawSelection.isIndexSelected(j)) {
					debug("Label at index [" + j + "] is NOT tree-hovered",23);
					if(colorIndex > 0) {
						g.setColor(TreeColorer.getColor(labels[colorIndex]));
					}

					labelColor = selectionTextFGColor;

					if(colorIndex > 0) {
						g.setColor(fore);
					}

				}
				else {
					debug("Label at index [" + j + "] is NOT tree-hovered",23);
					labelColor = Color.black;
				}

				/* Finally draw label (alignment-dependent) */
				int labelStrStart =
					getLabelStartOffset(metrics.stringWidth(out));
				debug("Label offset 2: [" + getLabelStartOffset(metrics
					.stringWidth(out)) +
					"]",11);

				int indexDiff = j - getPrimaryHoverIndex();
				int yPos = hoverYPos +
					(indexDiff * (labelAttr.getPoints() + SQUEEZE));
				debug("edgeOffset: [" + edgeOffset + "]",3);
				yPos -= edgeOffset;
				if(yPos < (offscreenMatrixSize + (ascent / 2))) {
					bgColor = drawLabelBackground(g,j,yPos - ascent);
					if(yPos <= getPrimaryViewportSize()) {
						maxPortLabel = j;
						maxPortLabelOffset = getPrimaryViewportSize() -
							((yPos - ascent) +
								labelAttr.getPoints());
						/*
						 * TODO: If anastasia doesn't like labels being colored
						 * red on tree node hover, uncomment, otherwise delete.
						 */
						// if(map.getHoverTreeMinIndex() == -1 ||
						// j < map.getHoverTreeMinIndex() ||
						// j > map.getHoverTreeMaxIndex()) {
						// labelColor = labelPortColor;
						// }
					}
					else {
						debug("Lightening edge-labels",24);
						labelColor = lightenColor(labelColor);
					}
					g2d.setColor(labelColor);

					g2d.drawString(out,labelStrStart,yPos);

					drawOverrunArrows(metrics.stringWidth(out),g,yPos -
						ascent,labelAttr.getPoints(),labelColor,bgColor,
						labelStrStart);
				}
			}
			catch(final java.lang.ArrayIndexOutOfBoundsException e) {
				LogBuffer.logException(e);
				break;
			}
		}

		// Set the first and last visible label for the tree drawing positions
		map.setFirstVisibleLabel(minPortLabel);
		map.setLastVisibleLabel(maxPortLabel);
		map.setFirstVisibleLabelOffset(minPortLabelOffset);
		map.setLastVisibleLabelOffset(maxPortLabelOffset);

		// Switch to the background color
		g2d.setColor(textBGColor);

		/* Draw the "position indicator" since the label port is active */
		// See variables initialized above the string drawing section
		if(indicatorThickness > 0) {
			// Draw a background box to blank-out any partially scrolled labels
			debug("Blanking out the scrolled " + getPaneType() +
				" labels from xpos: [" + (labelAndScrollCoordsAreOpposite()
					? lastScrollEndGap : lastScrollEndPos -
						indicatorThickness) + "] by thickness [" +
				indicatorThickness + "] and from 0 to [" +
				getSecondaryPaneSize(offscreenSize) + "]",19);
			g.fillRect((labelAndScrollCoordsAreOpposite() ? lastScrollEndGap
				: lastScrollEndPos -
				indicatorThickness),0,indicatorThickness,
				getPrimaryPaneSize(offscreenSize));
			// Switch to the label port color
			g2d.setColor(labelPortColor);

			if(matrixBarThickness > 0) {
				debug("Drawing " + getPaneType() +
					" matrix bar from index start [" +
					minPortLabel + "] to stop [" + maxPortLabel +
					"], pixel start [" +
					map.getPixel(minPortLabel) + "] to stop [" + (map
						.getPixel(maxPortLabel +
							1)) +
							"]",4);

				debug("Drawing " + getPaneType() + " matrix bar from xpos: [" +
					(labelAndScrollCoordsAreOpposite() ? lastScrollEndGap
						: lastScrollEndPos -
						indicatorThickness) +
						"] by thickness [" + matrixBarThickness + "]",19);
				// Draw the matrix breadth bar
				g.fillRect((labelAndScrollCoordsAreOpposite()
					? lastScrollEndGap
						: lastScrollEndPos -
						matrixBarThickness),map.getPixel(minPortLabel),
						matrixBarThickness,map.getPixel(maxPortLabel +
							1) -
							map.getPixel(minPortLabel));
			}

			if(getPrimaryHoverIndex() > -1) {
				// Change to the hover color
				g2d.setColor(hoverTextFGColor);
				// Draw the hover matrix position indicator
				int matrixPixel = map.getPixel(getPrimaryHoverIndex());

				int indwidth = map.getPixel(matrixPixel + 1) - map
					.getPixel(matrixPixel);
				if(indwidth < 1) {
					indwidth = 1;
				}
				debug("Drawing position indicator 1",14);
				g.fillRect((labelAndScrollCoordsAreOpposite()
					? lastScrollEndGap
						: lastScrollEndPos -
						matrixBarThickness - 1),matrixPixel,matrixBarThickness +
						1,indwidth);
			}
		}
		else {
			// Draw a background box to blank-out any partially scrolled
			// labels
			debug("Blanking out the " + getPaneType() +
				" indent at position [" +
				(labelAndScrollCoordsAreOpposite() ? lastScrollEndGap
					: lastScrollEndPos -
					labelIndent) + "]",15);
			g.fillRect((labelAndScrollCoordsAreOpposite() ? lastScrollEndGap
				: lastScrollEndPos -
				labelIndent),0,labelIndent,
				getPrimaryPaneSize(offscreenSize));
		}
	}

	/**
	 * Lightens a color by simply making it transparent (with an arbitrary alpha
	 * value)
	 *
	 * @param aColor
	 * @return newColor
	 */
	private Color lightenColor(final Color aColor) {
		Color newColor = new Color(aColor.getRed(),aColor.getGreen(),aColor
			.getBlue(),
			100);
		return(newColor);
	}

	private void drawFittedLabels(final Graphics g,final Graphics2D g2d) {
		final int start = map.getFirstVisible();
		final int end = map.getLastVisible();
		final int colorIndex = labelInfo.getIndex("FGCOLOR");
		final Color fore = GUIFactory.MAIN;
		final FontMetrics metrics = getFontMetrics(g2d.getFont());
		final int ascent = metrics.getAscent();
		int hoverStyle = Font.BOLD | labelAttr.getStyle();

		// Set the first and last visible label for the tree drawing positions
		map.setFirstVisibleLabel(start);
		map.setLastVisibleLabel(end);

		for(int j = start;j <= end;j++) {

			debug("Getting data index [" + j + "]",1);

			try {
				String out = labelSummary.getSummary(labelInfo,j);
				final String[] labels = labelInfo.getLabels(j);

				if(out == null) {
					out = "No Label";
				}

				Color bgColor = drawLabelBackground(g,j,map.getPixel(j));

				/* Set label color */
				if((drawSelection.isIndexSelected(j) && doDrawLabelPort()) ||
					(j == getAbsolutePrimaryHoverIndex()) || (((map
						.getHoverTreeMinIndex() > -1) &&
						(j >= map.getHoverTreeMinIndex()) &&
						(j <= map.getHoverTreeMaxIndex())))) {
					if(colorIndex > 0) {
						g.setColor(TreeColorer.getColor(labels[colorIndex]));
					}

					if((j == getPrimaryHoverIndex()) ||
						((map.getHoverTreeMinIndex() > -1) &&
							(j >= map.getHoverTreeMinIndex()) &&
							(j <= map.getHoverTreeMaxIndex()))) {
						g2d.setColor(hoverTextFGColor);
						if(j == getPrimaryHoverIndex()) {
							g2d.setFont(labelAttr.getTempStyleFont(hoverStyle));
						}
						else {
							g2d.setFont(labelAttr.getFont());
						}
					}
					else {
						g2d.setColor(fore);
						g2d.setFont(labelAttr.getFont());
					}

					if(colorIndex > 0) {
						g.setColor(fore);
					}

				}
				else {
					g2d.setColor(Color.black);
					g2d.setFont(labelAttr.getFont());
				}

				/* Finally draw label (alignment-dependent) */
				int labelStrStart =
					getLabelStartOffset(metrics.stringWidth(out));
				debug("Label offset 3: [" +
					getLabelStartOffset(metrics.stringWidth(out)) + "]",11);

				g2d.drawString(out,labelStrStart,map.getMiddlePixel(j) +
					(ascent / 2));

				drawOverrunArrows(metrics.stringWidth(out),g,
					map.getMiddlePixel(j) -
					(ascent / 2),labelAttr.getPoints(),g2d.getColor(),
					bgColor,labelStrStart);
			}
			catch(final java.lang.ArrayIndexOutOfBoundsException e) {
				LogBuffer.logException(e);
				break;
			}
		}

		// Switch to the background color
		g2d.setColor(textBGColor);
		// Draw a background box to blank-out any partially scrolled labels
		debug("Blanking out the " + getPaneType() + " indent at position [" +
			(labelAndScrollCoordsAreOpposite() ? lastScrollEndGap
				: lastScrollEndPos -
				labelIndent) + "]",15);
		g.fillRect((labelAndScrollCoordsAreOpposite() ? lastScrollEndGap
			: lastScrollEndPos -
			labelIndent),0,labelIndent,getPrimaryPaneSize(offscreenSize));
	}

	private void setHintPaneSize() {
		setPreferredSize(new Dimension(
			scrollPane.getViewport().getSize().width,
			scrollPane.getViewport().getSize().height));
	}

	private void drawLabelHint(final Graphics g,final Graphics2D g2d) {
		// Set a temporary font size - we don't need to save this size in the
		// prefs file. Besides, this font size would be saved as a new font
		// size and the scrollbars would be re-justified and not remember
		// their position
		g.setFont(labelAttr.getTempSizeFont(HINTFONTSIZE));
		g2d.setColor(Color.black);

		final FontMetrics metrics = getFontMetrics(g2d.getFont());
		int xPos =
			(getPrimaryViewportSize() - metrics.stringWidth(zoomHint)) / 2;
		int yPos = getSecondaryViewportSize() / 2;

		// Reduce the size of the scrollpane to just what is visible
		setHintPaneSize();

		orientHintPane(g2d);

		debug(getPaneType() + " Hint position: [" + xPos + "/" + yPos +
			"] zoomHint: [" + zoomHint + "] height [" + offscreenSize.height +
			"] width [" + offscreenSize.width + "] + HintStrLen [" + metrics
			.stringWidth(zoomHint) +
			"]",3);

		// Adding this useless scroll causes the pane size and scrollbar
		// attributes to update correctly - and reflect the settings made
		// above. Without it, the scrollbar still implies that the panel is
		// its previous size and the hint is off center. However, there is an
		// unfortunate side-effect: the scroll position of the labels upon
		// hover after this line has executed is briefly set to 0 instead of
		// the rembered value
		getSecondaryScrollBar().setValues(0,getSecondaryViewportSize(),0,
			getSecondaryViewportSize());

		g2d.drawString(zoomHint,xPos,yPos);
	}

	/**
	 * This method draws the backgrounds of selected and "selecting" (those who
	 * have been dragged over) labels. It takes into account whether the label
	 * is currently selected and whether the drag of the mouse is selecting
	 * anew, selecting additional, toggling, or deselecting. "Selecting anew"
	 * leaves the pre-existing highlights in place even though those not dragged
	 * over will be resultingly deselected. This is on purpose to give the user
	 * a visual context during dragging. Note this method is not intended to
	 * draw the default background in any circumstance.
	 *
	 * @param g
	 *            - Graphics object
	 * @param j
	 *            - Data index of the label whose background to draw
	 * @param yPos
	 *            - Pixel index of where the label is relative to the viewport
	 * @return
	 */
	public Color drawLabelBackground(final Graphics g,int j,int yPos) {

		if(j > (labelInfo.getNumLabels() - 1)) {
			return Color.black;
		}

		final int bgColorIndex = labelInfo.getIndex("BGCOLOR");
		final String[] strings = labelInfo.getLabels(j);
		Color bgColor = textBGColor;
		boolean isSelecting = (map.isSelecting() && (((j >= map
			.getSelectingStart()) &&
			(j <= getPrimaryHoverIndex())) ||
			((j <= map.getSelectingStart()) &&
				(j >= getPrimaryHoverIndex()))));
		boolean drawingSelectColor = false;
		try {
			if((drawSelection.isIndexSelected(j) && !isSelecting) ||
				(isSelecting &&
					((!map.isDeSelecting() &&
						!map.isToggling()) ||
						(map.isToggling() &&
							!drawSelection.isIndexSelected(j))))) {

				debug("Drawing yellow background for selected index [" + j +
					"] because isSelecting is [" +
					(isSelecting ? "true" : "false") +
					"] isDeSelecting is [" + (map.isDeSelecting() ? "true"
						: "false") +
						"] and isToggling is [" +
						(map.isToggling() ? "true" : "false") +
						"]",7);

				g.setColor(selectionTextBGColor);
				bgColor = selectionTextBGColor;
				drawingSelectColor = true;
			}
			else if(bgColorIndex > 0) {
				debug("Background color index is [" + bgColorIndex + "]",8);
				g.setColor(TreeColorer.getColor(strings[bgColorIndex]));
				bgColor = TreeColorer.getColor(strings[bgColorIndex]);
			}
		}
		catch(final Exception e) {
			// ignore...
		}

		if(drawingSelectColor) {
			g.fillRect(0,yPos,getSavedSecondaryPaneSize(),(doDrawLabelPort()
				? labelAttr.getPoints() +
					SQUEEZE
					: map.getPixel(j +
						1) -
						map.getPixel(j)));

			debug("Drawing select color from x|y [0|" + yPos +
				"] to x|y (secondaryPaneSize|(doDrawLabelPort() ? size + " +
				"SQUEEZE : map.getPixel(j + 1) - map.getPixel(j))) [" +
				getSavedSecondaryPaneSize() + "|" +
				(doDrawLabelPort() ? labelAttr
					.getPoints() + SQUEEZE : map.getPixel(j +
						1) -
						map.getPixel(j)) +
						"]",17);
		}

		return(bgColor);
	}

	/**
	 * This method ensures that the secondary label pane scrollbar is positioned
	 * to where it was before the labels were hidden behind the zoom/hover/font-
	 * size "hint". It performs the scroll based on 6 saved data members
	 * [lastScrollRowPos, lastScrollRowEndPos, lastScrollRowEndGap,
	 * lastScrollColPos, lastScrollColEndPos, and lastScrollColEndGap]. If any
	 * of those data members are not set (i.e. have a value of -1), it
	 * initializes them based on the saved justification.
	 *
	 * @param newSecondaryPaneSize
	 */
	private void secondaryReScroll(int newSecondaryPaneSize) {
		if(map.areLabelsBeingScrolled()) {
			debug("Not rescrolling because label knob is being dragged.",6);
			return;
		}

		int curPos = getSecondaryScrollBar().getValue();
		int curExtent = getSecondaryScrollBar().getModel().getExtent();
		int curMin = getSecondaryScrollBar().getMinimum();
		int curMax = getSecondaryScrollBar().getMaximum();

		/*
		 * Scroll to the position that is equivalent to the previous position
		 */
		if(labelAttr.isRightJustified()) {
			// See if we need to initialize the scroll positions
			if(lastScrollPos == -1) {
				if(labelAndScrollCoordsAreOpposite()) {
					debug("Scrolling to [0] after drawing - first time " +
						getPaneType() +
						"s right justified",6);

					// It seems that the scrollbar max and extent are not
					// updated in this scenario when the app first opens a
					// file, so we will calculate the initial scroll position
					// thusly
					lastScrollPos = 0;
					lastScrollEndPos = getSecondaryViewportSize();
					lastScrollEndGap = newSecondaryPaneSize -
						getSecondaryViewportSize();
				}
				else {
					debug("Scrolling to [" +
						(getSecondaryScrollBar().getMaximum() -
							getSecondaryScrollBar().getModel().getExtent()) +
							"] max - extent [" +
							getSecondaryScrollBar().getMaximum() + " - " +
							getSecondaryScrollBar().getModel().getExtent() +
							"] after drawing - first time " + getPaneType() +
							"s right justified",6);

					// It seems that the scrollbar max and extent are not
					// updated in this scenario when the app first opens a
					// file, so we will calculate the initial scroll position
					// thusly
					int tmpscrollpos = newSecondaryPaneSize -
						getSecondaryViewportSize();

					lastScrollPos = tmpscrollpos;
					debug("Setting lastScrollEndPos from [" + lastScrollEndPos +
						"] to [" + newSecondaryPaneSize +
						"] newSecondaryPaneSize",18);
					lastScrollEndPos = newSecondaryPaneSize;
					lastScrollEndGap = 0;
				}
			}
			else {
				debug("Scrolling to [" + lastScrollPos +
					"] after drawing - rememberred " + getPaneType() +
					"s right justified",6);
			}

			// Now perform the scroll...
			// Only change the other values if something about them has
			// changed (because it triggers an updateBuffer call and
			// updateBuffer calls this method, which would create an
			// endless loop). Setting just the scroll value does not
			// call updateBuffer
			if((curExtent != (lastScrollEndPos - lastScrollPos)) ||
				(curMin != 0) ||
				(curMax != newSecondaryPaneSize)) {
				getSecondaryScrollBar().setValues(lastScrollPos,
					lastScrollEndPos -
					lastScrollPos,0,newSecondaryPaneSize);

				debug("ReScroll " + getPaneType() + "s values, pane size: [" +
					newSecondaryPaneSize + "] pos [" + lastScrollPos +
					"] extent [" + (lastScrollEndPos - lastScrollPos) +
					"] min [0] max [" +
					(newSecondaryPaneSize -
						(lastScrollEndPos - lastScrollPos)) + "]",6);
			}
			else if(curPos != lastScrollPos) {
				getSecondaryScrollBar().setValue(lastScrollPos);

				debug("ReScroll " + getPaneType() + "s to pos, pane size: [" +
					newSecondaryPaneSize + "] pos [" + lastScrollPos +
					"] extent [" + (lastScrollEndPos - lastScrollPos) +
					"] min [0] max [" +
					(newSecondaryPaneSize -
						(lastScrollEndPos - lastScrollPos)) +"]",6);
			}
			else {
				debug("Already there",6);
			}
		}
		else {
			// See if we need to initialize the scroll positions
			if(lastScrollPos == -1) {
				if(labelAndScrollCoordsAreOpposite()) {
					// It seems that the scrollbar max and extent are not
					// updated in this scenario when the app first opens a
					// file, so we will calculate the initial scroll
					// position thusly
					int tmpscrollpos = newSecondaryPaneSize -
						getSecondaryViewportSize();

					lastScrollPos = tmpscrollpos;
					lastScrollEndPos = newSecondaryPaneSize;
					lastScrollEndGap = 0;

					debug("Set lastScrollPos [" + lastScrollPos +
						"] lastScrollEndPos [" +
						lastScrollEndPos + "] lastScrollEndGap [" +
						lastScrollEndGap +
						"] after drawing - first time " + getPaneType() +
						"s left justified",6);
				}
				else {
					debug("Scrolling to [0] after drawing - first time " +
						getPaneType() +
						"s left justified",6);

					// It seems that the scrollbar max and extent are not
					// updated in this scenario when the app first opens a
					// file, so we will calculate the initial scroll position
					// thusly
					lastScrollPos = 0;
					lastScrollEndPos = getSecondaryViewportSize();
					lastScrollEndGap = newSecondaryPaneSize -
						getSecondaryViewportSize();
				}
			}
			else {
				debug("Scrolling to [" + lastScrollPos +
					"] after drawing - rememberred " + getPaneType() +
					"s left justified",6);
			}

			// Only change the other values if something about them has
			// changed (because it triggers an updateBuffer call and
			// updateBuffer calls this method, which would create an
			// endless loop). Setting just the scroll value does not
			// call updateBuffer
			if((curExtent != (lastScrollEndPos - lastScrollPos)) ||
				(curMin != 0) ||
				(curMax != newSecondaryPaneSize)) {
				getSecondaryScrollBar().setValues(lastScrollPos,
					lastScrollEndPos -
					lastScrollPos,0,newSecondaryPaneSize);

				debug("ReScroll " + getPaneType() +
					"s left just. values, pane size: [" + newSecondaryPaneSize +
					"] pos [" + lastScrollPos + "] extent [" +
					(lastScrollEndPos -
						lastScrollPos) +
						"] min [0] max [" +
						(newSecondaryPaneSize - (lastScrollEndPos -
							lastScrollPos)) +
							"]",6);
			}
			else if(curPos != lastScrollPos) {
				getSecondaryScrollBar().setValue(lastScrollPos);

				debug("ReScroll " + getPaneType() +
					"s left just. to pos, pane size: [" + newSecondaryPaneSize +
					"] pos [" + lastScrollPos + "] extent [" +
					(lastScrollEndPos -
						lastScrollPos) +
						"] min [0] max [" +
						(newSecondaryPaneSize - (lastScrollEndPos -
							lastScrollPos)) +
							"]",6);
			}
			else {
				debug("Already there",6);
			}
		}
	}

	/**
	 * Returns either the number of pixels in length the longest string is, or
	 * the portion of the panel the strings will be drawn in (not including the
	 * indicator thickness or indent) - whichever is larger.
	 *
	 * @param metrics
	 *            - font details
	 */
	public int getLabelAreaLength(FontMetrics metrics) {
		int min = getSecondaryViewportSize() - getLabelShiftSize();
		int len = getMaxStringLength(metrics);
		return len > min ? len : min;
	}

	/**
	 * This returns the vertical size of the column label pane or horizontal
	 * size of the row label pane, including the space allotted for indent
	 * and/or the indicator bar. It calculates the size based on the longest
	 * label length using the current font settings (metrics param) via a call
	 * to getMaxStringLength. If the viewport size is larger than the longest
	 * label plus indent/indicator, it returns the viewport size.
	 *
	 * @param metrics
	 * @return
	 */
	public int getLabelPaneContentSize(FontMetrics metrics) {
		debug("Max string length for font size [" + labelAttr.getPoints() +
			"]: [" +
			getMaxStringLength(metrics) + "] and the viewport dimension: [" +
			getSecondaryViewportSize() + "]",18);
		int len = getMaxStringLength(metrics) + getLabelShiftSize();
		return(len > getSecondaryViewportSize() ? len
			: getSecondaryViewportSize());
	}

	/**
	 * This function returns the length in pixels of the longest label and
	 * tracks the font settings using data members [lastDrawnFace versus current
	 * 'face', lastDrawnStyle versus current 'style', lastDrawnSize versus
	 * current 'size', longest_str_length, and longest_str_index]. If nothing
	 * about the font has changed, it returns longest_str_length. If the face or
	 * style have changed, it re-measures the string at longest_str_index.
	 * Otherwise, it searches all the strings again to get the longest.
	 *
	 * @param FontMetrics
	 *            metrics
	 * @return maxStrLen
	 */
	public int getMaxStringLength(FontMetrics metrics) {
		int end = map.getMaxIndex();
		int maxStrLen = 0;

		// No labels have been loaded for this LabelView's axis
		if(labelInfo.getNumLabelTypes() == 0) {
			labelAttr.setLongestStrIdx(-1);
			labelAttr.setLongestStr("");
			return maxStrLen;
		}

		// TODO - ideally this code would not be necessary because a data change
		// should prompt a state reset of the entire class.
		// If we detect that the data has changed, re-initialize the last drawn
		// longest string variables. Note, this can be thwarted in the rare case
		// that 2 different data files happen to have the same label in the same
		// position and there exists a longer label elsewhere
		if(labelAttr.isLongestStrIdxDefined() &&
			((labelAttr.getLongestStrIdx() > map.getMaxIndex()) ||
				(labelAttr.getLongestStr() == null) ||
				"".equals(labelAttr.getLongestStr()) ||
				(labelAttr.getLongestStrIdx() == -1) ||
				!labelAttr.isLongestStrEqualTo(labelSummary.getSummary(labelInfo,
					labelAttr.getLongestStrIdx())))) {
			LogBuffer.println("Data changed. Prompting recalculation of max " +
				"string length.");
			labelAttr.setLongestStrIdx(-1);
			labelAttr.setLongestStr("");
		}

		boolean isLongestStrEqual = false;
		String tempLongest = null;
		if(labelAttr.isLongestStrIdxDefined()) {
			tempLongest = labelSummary.getSummary(labelInfo,
				labelAttr.getLongestStrIdx());
			isLongestStrEqual = labelAttr.isLongestStrEqualTo(tempLongest);
		}

		// If nothing about the font has changed, calculate the length of the
		// longest string
		if(isLongestStrEqual && !labelAttr.hasFontChanged()) {

			debug("Regurgitating maxstrlen",1);
			labelAttr.setLongestStr(tempLongest);
			maxStrLen = labelAttr.getLongestStrLen();
			debug("Everything fontwise is the same, including size [" +
				labelAttr
				.getPoints() +
				"]. returning saved maxStrLen [" + maxStrLen + "]",18);
		}
		// Else if the font attributes have changed, recalculate the longest
		// string's length
		else if(isLongestStrEqual && labelAttr.hasFontChanged()) {

			debug("Refining maxstrlen",1);
			labelAttr.setLongestStr(tempLongest);
			maxStrLen = metrics.stringWidth(tempLongest);
			debug("Font size only changed. Recalculating length of longest " +
				"string [" + labelAttr.getLongestStr() + "] & returning " +
				"maxStrLen [" + maxStrLen + "]",18);
		}
		// Else find the longest string and return its length
		else {
			debug("Calculating maxstrlen because not [lastDrawnFace == face " +
				"&& lastDrawnStyle == style && longest_str_index > -1 && " +
				"lastDrawnSize != size && longest_str.equals(labelSummary." +
				"getSummary(labelInfo,longest_str_index))]",18);
			debug("Calculating maxstrlen because not [" +
				labelAttr.getLastDrawnFace() + " == " + labelAttr.getFace() +
				" && " + labelAttr.getLastDrawnStyle() + " == " +
				labelAttr.getStyle() + " && " + labelAttr.getLastDrawnStyle() +
				" > -1 && " + labelAttr.getLastDrawnSize() + " != " +
				labelAttr.getPoints() + " && " + labelAttr.getLongestStr() +
				".equals(labelSummary.getSummary(labelInfo," +
				labelAttr.getLongestStrIdx() + "))]",18);
			for(int j = 0;j <= end;j++) {
				try {
					String out = labelSummary.getSummary(labelInfo,j);

					if(out == null) {
						out = "No Label";
					}

					if(maxStrLen < metrics.stringWidth(out)) {
						maxStrLen = metrics.stringWidth(out);
						labelAttr.setLongestStrIdx(j);
						labelAttr.setLongestStr(out);
					}

				}
				catch(final java.lang.ArrayIndexOutOfBoundsException e) {
					LogBuffer.logException(e);
					break;
				}
			}

			debug(
				"Full-on recalculating [" + labelAttr.getLongestStr() +
				"] maxStrLen [" + maxStrLen + "] at face [" +
				labelAttr.getFace() +
				"] style [" + labelAttr.getStyle() + "] size [" + labelAttr
				.getPoints() +
				"]",12);
		}

		debug(getPaneType() + ": MaxStrLen: [" + maxStrLen + "] MaxStr: [" +
			labelAttr.getLongestStr() + "] Start Index: [" + 0 +
			"] End Index: [" + end + "] height [" + offscreenSize.height +
			"] width [" + offscreenSize.width + "]",1);

		return(maxStrLen);
	}

	public int getMaxExportStringLength(final boolean selected) {
		int fontSize = getMinLabelAreaHeight() - SQUEEZE;
		Font tmpFont = new Font(labelAttr.getFace(),labelAttr.getStyle(),
			fontSize);
		FontMetrics fm = getFontMetrics(tmpFont);
		//Calling this because it updates longest_str (which may not have been calculated if labels were never drawn on the screen)
		getMaxStringLength(fm);
		int maxStrLen = fm.stringWidth(labelAttr.getLongestStr());

		if(selected) {
			maxStrLen = 0;
			int[] selectedIndexes = drawSelection.getSelectedIndexes();
			for(int j = 0;j < drawSelection.getNSelectedIndexes();j++) {
				try {
					String out =
						labelSummary.getSummary(labelInfo,selectedIndexes[j]);

					if(out == null) {
						out = "No Label";
					}

					if(maxStrLen < fm.stringWidth(out)) {
						maxStrLen = fm.stringWidth(out);
					}
				}
				catch(final java.lang.ArrayIndexOutOfBoundsException e) {
					LogBuffer.logException(e);
					break;
				}
			}
		}

		return(maxStrLen);
	}

	/**
	 * Dynamic update of font size based on size setting or map scale
	 *
	 * @param g - <code>Graphics</code> object for which to update the font.
	 * @return
	 */
	private void updateFontSize(final Graphics g) {
		if(!labelAttr.isFixed()) {
			adaptFontSizeToMapScale();
			g.setFont(labelAttr.getFont());
		}
	}

	/**
	 * FIXME - Consider moving to LabelAttributes Sets a dynamic font size based
	 * on current scale of the dependent axis map.
	 */
	private void adaptFontSizeToMapScale() {
		int newPoints = (int) map.getScale() - SQUEEZE;
		if(newPoints > labelAttr.getMaxSize()) {
			newPoints = labelAttr.getMaxSize();
		}
		else if(newPoints < labelAttr.getMinSize()) {
			newPoints = labelAttr.getMinSize();
		}

		if(!labelAttr.isFixed() && (newPoints != labelAttr.getPoints())) {
			debug("Adapting to new font size from [" + labelAttr.getPoints() +
				"] to [" + newPoints + "] for [" + getPaneType() + "s]",1);
			labelAttr.setLastSize(newPoints);
		}
	}

	/**
	 * Keeps track of the hover index and starts up the repaint timer for
	 * shifting labels based on that hover position. The starting of the timer
	 * is added only for responsiveness since it is started up in the
	 * updateBuffer method.
	 */
	@Override
	public void mouseDragged(final MouseEvent e) {
		// Note that the hover index is updated by forceUpdatePrimaryHoverIndex
		// which is called from updateBuffer

		debug("Selecting from " + map.getSelectingStart() + " to " + map
			.getIndex(getPrimaryHoverPosition(e)),8);

		/*
		 * TODO: If anastasia doesn't like trees linked to whizzing labels,
		 * uncomment the following commented code. If she likes it, delete. This
		 * code saves compute cycles, but makes the trees sometimes move out of
		 * sync with the labels.
		 */
		// //This is not necessary, but makes things a tad more responsive
		// if(repaintTimer != null &&
		// repaintTimer.getDelay() == slowRepaintInterval) {
		// repaintTimer.setDelay(repaintInterval);
		// repaintTimer.restart();
		// }

		repaint();

		/*
		 * TODO: Handle the temporary outlining of the matrix NOT SURE HOW TO DO
		 * THIS YET - CANNOT TALK TO IMV
		 */
	}

	/** Keeps track of whether a selection is being made and what kind. */
	@Override
	public void mousePressed(final MouseEvent e) {

		if(!enclosingWindow().isActive() ||
			(SwingUtilities.isLeftMouseButton(e) &&
				e.isControlDown())) {
			debug("Control is down - do nothing on pressed",23);
			return;
		}

		map.setHoverHighlight(false);

		// if left button is used
		if(SwingUtilities.isLeftMouseButton(e)) {
			// Handle the temporary yellow highlighting of the labels
			map.setSelecting(true);

			debug(
				"Selecting start: " + map.getIndex(getPrimaryHoverPosition(e)),
				8);

			map.setSelectingStart(map.getIndex(getPrimaryHoverPosition(e)));
			// Note that the hover index is updated by
			// forceUpdatePrimaryHoverIndex which is called from updateBuffer

			if(e.isMetaDown()) {
				map.setToggling(true);
			}
			else if(e.isAltDown()) {
				map.setDeSelecting(true);
			}

			/*
			 * TODO: Handle the temporary outlining of the matrix NOT SURE HOW
			 * TO DO THIS YET - CANNOT TALK TO IMV
			 */
		}

		drawSelection.notifyObservers();
		otherSelection.notifyObservers();
	}

	/**
	 * Keeps track of whether a selection is being made and performs the
	 * selection.
	 */
	@Override
	public void mouseReleased(final MouseEvent e) {

		/*
		 * NOTE: Right click is captured elsewhere application-wide and
		 * deselects everything
		 */

		if(!enclosingWindow().isActive() ||
			(SwingUtilities.isLeftMouseButton(e) &&
				e.isControlDown()) || !SwingUtilities
				.isLeftMouseButton(e)) {
			debug("Window inactive, control is down, or not left mouse " +
				"button - do nothing on release",23);
			return;
		}

		int hDI = map.getIndex(getPrimaryHoverPosition(e));

		// If the user dragged off an edge, the index will be out of bounds,
		// so fix it
		if(hDI < 0) {
			hDI = 0;
		}
		else if(hDI > map.getMaxIndex()) {
			hDI = map.getMaxIndex();
		}

		// If this was a drag event
		if(map.getSelectingStart() != getPrimaryHoverIndex(e)) {

			/*
			 * TODO: Handle the temporary outlining of the matrix NOT SURE HOW
			 * TO DO THIS YET - CANNOT TALK TO IMV
			 */

			// If a selection already exists
			if(otherSelection.getNSelectedIndexes() > 0) {
				if(e.isShiftDown()) {
					drawSelection.selectIndexRange(map.getSelectingStart(),hDI);
				}
				else if(e.isMetaDown()) {
					debug("Toggling selection of index range [" + map
						.getSelectingStart() +
						"] to [" + hDI + "]",23);
					toggleSelectRange(map.getSelectingStart(),hDI);
				}
				else if(e.isAltDown()) {
					debug("Toggling selection of index range [" + map
						.getSelectingStart() +
						"] to [" + hDI + "]",23);
					deSelectRange(map.getSelectingStart(),hDI);
				}
				else {
					drawSelection.selectNewIndexRange(map.getSelectingStart(),
						hDI);
				}
			}
			// Else if there's no current selection and we're not deselecting
			else if(!e.isAltDown()) {
				otherSelection.selectAllIndexes();
				drawSelection.selectNewIndexRange(map.getSelectingStart(),hDI);
			}
		}
		// Else this was not a drag
		else {

			// Do a deselect of everything if command-option are down
			if(e.isMetaDown() && e.isAltDown()) {
				drawSelection.deselectAllIndexes();
				otherSelection.deselectAllIndexes();
			}
			// If a selection already exists
			else if(otherSelection.getNSelectedIndexes() > 0) {
				if(e.isShiftDown()) {
					if(!drawSelection.isIndexSelected(hDI)) {
						toggleSelectFromClosestToIndex(drawSelection,hDI);
					}
				}
				else if(e.isMetaDown()) {
					debug("Toggling selection of index [" + hDI + "]",23);
					toggleSelect(drawSelection,hDI);
				}
				else if(e.isAltDown()) {
					if(drawSelection.isIndexSelected(hDI)) {
						debug("Setting selection of index [" + hDI +
							"] to false",23);
						toggleSelectFromClosestToIndex(drawSelection,hDI);
					}
				}
				else {
					selectAnew(drawSelection,hDI);
				}
			}
			// Else nothing is selected and if we're not deselecting
			else if(!e.isAltDown()) {
				drawSelection.setIndexSelection(hDI,true);
				otherSelection.selectAllIndexes();
			}
		}

		// Handle the temporary yellow highlighting of the labels
		map.setSelecting(false);
		map.setDeSelecting(false);
		map.setToggling(false);
		unsetPrimaryHoverIndex();
		map.setSelectingStart(-1);

		drawSelection.notifyObservers();
		otherSelection.notifyObservers();
	}

	/**
	 * This method toggles the selection booleans over a range of data indexes
	 *
	 * @param start
	 * @param end
	 */
	public void toggleSelectRange(int start,int end) {
		debug("Toggling start index [" + start + "] to end index [" + end + "]",
			8);
		if(start > end) {
			int tmp = start;
			start = end;
			end = tmp;
		}

		// If the user dragged off an edge, the index will be out of bounds, so
		// fix it
		if(start < 0) {
			start = 0;
		}
		if(end > map.getMaxIndex()) {
			end = map.getMaxIndex();
		}

		for(int i = start;i <= end;i++) {
			if(drawSelection.isIndexSelected(i)) {
				drawSelection.setIndexSelection(i,false);
			} else {
				drawSelection.setIndexSelection(i,true);
			}
		}
	}

	/**
	 * This method sets the selection booleans to false over a range of data
	 * indexes
	 *
	 * @param start
	 * @param end
	 */
	public void deSelectRange(int start,int end) {
		// Make sure start is less than stop
		if(start > end) {
			int tmp = start;
			start = end;
			end = tmp;
		}

		// If the user dragged off an edge, the index will be out of bounds, so
		// fix it
		if(start < 0) {
			start = 0;
		}
		if(end > map.getMaxIndex()) {
			end = map.getMaxIndex();
		}

		for(int i = start;i <= end;i++) {
			debug("Setting selection of index [" + i + "] to false in loop",23);
			drawSelection.setIndexSelection(i,false);
		}
	}

	/**
	 * Returns the pixel index of the cursor and updates the pixelIndex data
	 * member for use during scroll wheel events (to update the data index
	 * currently hovered over).
	 *
	 * @param e
	 * @return
	 */
	protected abstract int getPrimaryHoverPosition(final MouseEvent e);

	/**
	 * Keeps track of the hover data index and starts up the repaint timer for
	 * shifting labels based on that hover position. The starting of the timer
	 * is added only for responsiveness since it is started up in the
	 * updateBuffer method.
	 */
	@Override
	public void mouseMoved(final MouseEvent e) {
		// if(!hasFocus()) return;

		setHoverPosition(e);
		// Note that the hover index is updated by forceUpdatePrimaryHoverIndex
		// which is called from updateBuffer

		/*
		 * TODO: If anastasia doesn't like trees linked to whizzing labels,
		 * uncomment the following commented code. If she likes it, delete. This
		 * code saves compute cycles, but makes the trees sometimes move out of
		 * sync with the labels.
		 */
		// //This is not necessary, but makes things a tad more responsive
		// if(repaintTimer != null &&
		// repaintTimer.getDelay() == slowRepaintInterval) {
		// repaintTimer.setDelay(repaintInterval);
		// repaintTimer.restart();
		// }

		/*
		 * Set the Data ticker value to average value of the current row/column
		 * Rounding off to 4 decimals
		 */
		setDataTickerValue(e);

		repaint();
	}

	/**
	 * Returns the data index corresponding to the cursor position relative to
	 * the visible portion of the matrix.
	 *
	 * @param e
	 * @return
	 */
	public int getPrimaryHoverIndex(final MouseEvent e) {
		return(map.getIndex(getPrimaryHoverPosition(e)));
	}

	public int getPrimaryHoverIndex() {
		return(map.getHoverIndex());
	}

	/**
	 * Gets the data index that is hovered over, or -1 if not a valid hover
	 * position
	 *
	 * @return data index
	 */
	public int getAbsolutePrimaryHoverIndex() {
		return(map.overALabelLinkedView() ? map.getHoverIndex() : -1);
	}

	public void updatePrimaryHoverIndexDuringScrollWheel() {
		if(map.getHoverPixel() == -1) {
			unsetPrimaryHoverIndex();
		}
		else {
			setPrimaryHoverIndex(map.getIndex(map.getHoverPixel()));
		}
	}

	// There used to be a custom case for hover index updates during
	// scrollbar drag, but it has been replaced by this more universal
	// method. Note, this method returns 0 or the max index if the
	// cursor is
	// hovered off that nearest edge.
	public void forceUpdatePrimaryHoverIndex() {
		Point p = MouseInfo.getPointerInfo().getLocation();
		SwingUtilities.convertPointFromScreen(p,getComponent());
		p.y += 1; // +1 is a fudge factor because the conversion seems to
		// be off by 1 in the y direction for whatever reason
		int hPI = determineCursorPixelIndex(p); // Hover Pixel Index
		if(debug > 0) {
			debug("Hover Pixel as determined by: MouseInfo.getPointerInfo(). " +
				"getLocation(): [" + hPI + "] map.getHoverPixel(): [" +
				map.getHoverPixel() + "]",21);
		}
		int hDI = map.getIndex(hPI); // Hover Data Index
		if(hDI > map.getLastVisible()) {
			hDI = map.getLastVisible();
		}
		else if(hDI < map.getFirstVisible()) {
			hDI = map.getFirstVisible();
		}
		setPrimaryHoverIndex(hDI);
	}

	/**
	 * This setter allows a hover index to be manually set in cases where the
	 * cursor is for example, over the secondary scroll bar, drag-selecting off
	 * the edge of the matrix, or dragging the opposing labels' secondary
	 * scrollbar
	 *
	 * @param i
	 */
	public void setPrimaryHoverIndex(final int i) {
		map.setHoverIndex(i);
	}

	public void unsetPrimaryHoverIndex() {
		map.unsetHoverIndex();
	}

	/**
	 * This method returns a boolean indicating whether or not to draw the
	 * components of the label port. It is not just a getter like
	 * inLabelPortMode. It additionally checks whether the conditions of the
	 * display necessitate a label port (e.g. whether the labels' minimum font
	 * height is too big for the current scale of the matrix)
	 *
	 * @return
	 */
	public boolean doDrawLabelPort() {
		return(inLabelPortMode() && map.overALabelLinkedView() &&
			map.getScale() < getMinLabelAreaHeight());
	}

	public boolean doDrawLabels() {
		return(doDrawLabelPort() ||
			(map.getScale() >= getMinLabelAreaHeight()));
	}

	/**
	 * Returns the font height plus the SQUEEZE (i.e. all the height needed for
	 * a single label)
	 * 
	 * @return
	 */
	public int getMinLabelAreaHeight() {
		return((labelAttr.isFixed() ?
			labelAttr.getLastSize() : labelAttr.getMinSize()) + SQUEEZE);
	}

	/**
	 * 
	 * @return the squeeze
	 */
	public static int getSqueeze() {
		return(SQUEEZE);
	}

	/**
	 * This method sets the scroll data members all to -1 (i.e. an unset state).
	 * This is necessary for when for example, the font changes or when a new
	 * file is opened.
	 */
	public void resetSecondaryScroll() {
		debug("Resetting last secondary scroll position values",2);
		lastScrollPos = -1;
		lastScrollEndGap = -1;
		lastScrollEndPos = -1;
	}

	/**
	 * This method corrects the stored scroll positions for when the viewport
	 * size changes because the size of the knob will have changed and either
	 * the end gap (when left justified) or the position (when right justified)
	 * has changed
	 */
	public void adjustSecondaryScroll(int maxContentLen) {
		debug("Resetting last secondary scroll position values",12);
		// getSecondaryScrollBar().getModel().getExtent() and
		// getSecondaryScrollBar().getMaximum() cannot be trusted because it
		// does not appear to be updated after a split pane divider move, so
		// we will determine the scroll position based on what we know the
		// viewport and max str length to be
		int extent = getSavedSecondaryViewportSize();
		int scrollMax = (maxContentLen > getSavedSecondaryViewportSize()
			? maxContentLen
				: getSavedSecondaryViewportSize());
		// Do not adjust if the scrollbars are not properly set
		if((lastScrollPos == -1) || (lastScrollEndPos == -1) ||
			(lastScrollEndGap == -1)) { return; }

		if(labelAndScrollCoordsAreOpposite()) {
			if(labelAttr.isRightJustified()) {
				// Top Justified, but note that scroll position 0 is at the top
				// and that's the end/right-side of the string
				// lastScrollPos should not change unless the viewport is now
				// larger than the content minus the previous position
				if((lastScrollPos > 0) &&
					(extent > (maxContentLen - lastScrollPos))) {
					lastScrollPos = maxContentLen - extent;
					if(lastScrollPos < 0) {
						lastScrollPos = 0;
					}
				}
				debug("Adjusting columns for bottom justification",1);
				lastScrollEndPos = lastScrollPos + extent;
				lastScrollEndGap = scrollMax - lastScrollEndPos;
			}
			else {
				// Bottom justified, but note that the end scroll position is at
				// the bottom and that's the beginning of the string
				// lastScrollEndGap should not change unless the viewport is
				// now larger than the content minus the previous gap size
				if((lastScrollEndGap > 0) && (extent > (maxContentLen -
					lastScrollEndGap))) {
					lastScrollEndGap = maxContentLen - extent;
					if(lastScrollEndGap < 0) {
						lastScrollEndGap = 0;
					}
				}
				debug("Adjusting " + getPaneType() + "s for top justification",
					1);
				lastScrollEndPos = scrollMax - lastScrollEndGap;
				lastScrollPos = lastScrollEndPos - extent;
			}
		}
		else {
			if(labelAttr.isRightJustified()) {
				// lastScrollEndGap should not change unless the viewport is
				// now larger than the content minus the previous gap size
				if((lastScrollEndGap > 0) && (extent > (maxContentLen -
					lastScrollEndGap))) {
					lastScrollEndGap = maxContentLen - extent;
					if(lastScrollEndGap < 0) {
						lastScrollEndGap = 0;
					}
				}
				debug("Setting lastScrollEndPos from [" + lastScrollEndPos +
					"] to [" +
					(scrollMax - lastScrollEndGap) +
					"] scrollMax - lastScrollEndGap [" + scrollMax + " - " +
					lastScrollEndGap + "]",18);
				lastScrollEndPos = scrollMax - lastScrollEndGap;
				lastScrollPos = lastScrollEndPos - extent;
			}
			else {
				// lastScrollPos should not change unless the viewport is now
				// larger than the content minus the previous position
				if((lastScrollPos > 0) &&
					(extent > (maxContentLen - lastScrollPos))) {
					lastScrollPos = maxContentLen - extent;
					if(lastScrollPos < 0) {
						lastScrollPos = 0;
					}
				}
				debug("Setting lastScrollEndPos from [" + lastScrollEndPos +
					"] to [" +
					(lastScrollPos + extent) + "] lastScrollPos + extent [" +
					lastScrollPos + " + " + extent + "]",18);
				lastScrollEndPos = lastScrollPos + extent;
				lastScrollEndGap = scrollMax - lastScrollEndPos;
			}
		}
	}

	/**
	 * Keeps track of the label pane's viewport size in the secondary dimension
	 * (i.e. the length of the labels) and returns whether a change has occurred
	 * since the last time it was called.
	 *
	 * @return
	 */
	public boolean secondaryViewportSizeChanged() {
		boolean changed = false;
		if(getSecondaryViewportSize() != getSavedSecondaryViewportSize()) {
			changed = true;
			secondaryViewportSize = getSecondaryViewportSize();
		}
		return(changed);
	}

	protected int getSavedSecondaryViewportSize() {
		return(secondaryViewportSize);
	}

	public void trackSecondaryPaneSize(int paneSize) {
		debug("trackSecondaryPaneSize: Pane size saved as [" +
			getSavedSecondaryPaneSize() + "]",17);
		if((getSecondaryPaneSize(offscreenSize) != getSavedSecondaryPaneSize()) ||
			(paneSize != getSavedSecondaryPaneSize())) {
			setSecondaryPaneSize(offscreenSize,paneSize);
			debug("trackSecondaryPaneSize: Changing " + getPaneType() +
				" pane size to [" + paneSize + "]",17);
		}
	}

	/**
	 * This method computes the x coordinate of where the label should be drawn
	 * in order to keep it at least partially in view at any scroll position. It
	 * takes into account current scroll position, row/col axis, label length,
	 * and label justification (right/left or top/bottom). It computes the start
	 * relative to the full pane size, including the offscreen portion. Note
	 * that the 0 position for drawing row labels is the same as the 0 position
	 * for scrolling row labels, but for column labels, the 0 position for
	 * scrolling is at the end of the labels because labels are drawn from the
	 * bottom (0) up and scrolling in java swing is from the top (0) down.
	 *
	 * @param labelLen
	 * @return offset
	 */
	public int getLabelStartOffset(int labelLen) {
		int offset = 0;
		int indent = getLabelShiftSize();
		if(labelAndScrollCoordsAreOpposite()) {
			if(labelAttr.isRightJustified()) {
				debug("Top justified columns. Extent: [" +
					getSecondaryScrollBar().getModel().getExtent() +
					"] Pane Size: [" + getSavedSecondaryPaneSize() + "]",11);
				if((lastScrollEndPos != -1) && (lastScrollPos != -1) &&
					((labelLen +
						indent) > (/* Extent */lastScrollEndPos -
							lastScrollPos))) {
					if((labelLen + indent) >= lastScrollEndPos) {
						offset = getSavedSecondaryPaneSize() - labelLen;
						debug("Setting offset to (paneSize - labelLen) [" +
							getSavedSecondaryPaneSize() + " - " + labelLen +
							"] = [" +
							offset + "]",16);
						if(offset < 0) {
							debug("Case 1.",20);
						}
					} else if(lastScrollEndGap != -1) {
						offset = lastScrollEndGap + indent;
						debug(
							"Setting offset to (lastScrollEndGap + indent) [" +
								lastScrollEndGap + " + " + indent + "] = [" +
								offset +
								"]",16);
						if(offset < 0) {
							debug("Case 2.",20);
						}
					}
				} else if((lastScrollEndPos != -1) && (lastScrollPos != -1) &&
					(lastScrollEndGap != -1)) {
					offset = (lastScrollEndGap + (/* Extent */lastScrollEndPos -
						lastScrollPos)) -
						labelLen;
					debug(
						"Setting offset to (lastScrollEndGap + " +
						"(lastScrollEndPos - lastScrollPos) - labelLen) [" +
						lastScrollEndGap + " + (" + lastScrollEndPos + " - " +
						lastScrollPos + ") - " + labelLen + "] = [" + offset +
						"]",16);
					if(offset < 0) {
						debug("Case 2.",20);
					}
				}
			} else {
				debug("Bottom justified columns. Extent: [" +
					getSecondaryScrollBar()
					.getModel()
					.getExtent() +
					"] Pane Size: [" + getSavedSecondaryPaneSize() + "]",11);
				if((lastScrollEndPos != -1) && (lastScrollPos != -1) &&
					((labelLen +
						indent) > (/* Extent */lastScrollEndPos -
							lastScrollPos))) {
					if(lastScrollPos < (getSavedSecondaryPaneSize() -
						(labelLen + indent))) {
						offset = (lastScrollEndGap +
							(/* Extent */lastScrollEndPos - lastScrollPos)) -
							labelLen;
						if(offset < 0) {
							debug("Case 3.",20);
						}
					} else {
						offset += indent;
						if(offset < 0) {
							debug("Case 4.",20);
						}
					}
				} else if(lastScrollEndGap != -1) {
					offset = lastScrollEndGap + indent;
					if(offset < 0) {
						debug("Case 5.",20);
					}
				} else {
					offset = indent;
					if(offset < 0) {
						debug("Case 5.1.",20);
					}
				}
			}
		} else {
			if(labelAttr.isRightJustified()) {
				debug("Right justified rows. Extent: [" +
					getSecondaryScrollBar().getModel().getExtent() +
					"] Pane Size: [" + getSavedSecondaryPaneSize() + "]",11);
				if((lastScrollEndPos != -1) && (lastScrollPos != -1) &&
					((labelLen +
						indent) > (/* Extent */lastScrollEndPos -
							lastScrollPos))) {
					if((lastScrollPos != -1) &&
						(lastScrollPos < (getSavedSecondaryPaneSize() -
							(labelLen + indent)))) {
						offset = lastScrollPos + indent;
						debug("A: offset = lastScrollPos + indent [" + offset +
							" = " +
							lastScrollPos + " + " + indent + "]",11);
						if((offset - indent) < 0) {
							debug("Case 6.",20);
						}
					} else {
						offset = getSavedSecondaryPaneSize() - labelLen;
						debug("B: offset = paneSize - labelLen [" + offset +
							" = " +
							getSavedSecondaryPaneSize() + " - " + labelLen +
							"]",11);
						if((offset - indent) < 0) {
							debug("Case 7.",20);
						}
					}
					offset -= indent;
				} else if(lastScrollEndPos != -1) {
					offset = lastScrollEndPos - labelLen;
					debug("C: offset = lastScrollEndPos - labelLen [" + offset +
						" = " +
						lastScrollEndPos + " - " + labelLen + "]",11);
					offset -= indent;
					if(offset < 0) {
						debug("Case 8.",20);
					}
				}
			} else {
				debug("Left justified rows. Extent: [" +
					getSecondaryScrollBar().getModel().getExtent() +
					"] Pane Size: [" + getSavedSecondaryPaneSize() + "]",11);
				if((lastScrollEndPos != -1) && (lastScrollPos != -1) &&
					((labelLen +
						indent) > (/* Extent */lastScrollEndPos -
							lastScrollPos))) {
					if((lastScrollEndPos != -1) &&
						(lastScrollEndPos > (labelLen + indent))) {
						offset = lastScrollEndPos - (labelLen + indent);
						if(offset < 0) {
							debug("Case 9.",20);
						}
					}
				} else if(lastScrollPos != -1) {
					offset = lastScrollPos;
					if(offset < 0) {
						debug("Case 10.",20);
					}
				}
			}
		}

		if(offset < 0) {
			// The bugs that required this have been fixed, but I am leaving the
			// check here as a fail-safe. -Rob
			debug("Negative offset encountered [" + offset +
				"]. This should not have happened.",20);
			offset = 0;
		}

		return(offset);
	}

	/**
	 * This method decides whether to draw arrows when labels run out of the
	 * viewport view and calls the appropriate arrow drawing function
	 *
	 * @param int
	 *        labelLen
	 * @param Graphics
	 *            g
	 * @param int
	 *        yPos
	 * @param int
	 *        height
	 * @param Color
	 *            fgColor
	 * @param Color
	 *            bgColor
	 * @param int
	 *        xPos
	 * @return void
	 */
	public void drawOverrunArrows(int labelLen,final Graphics g,int yPos,
		int height,Color fgColor,Color bgColor,
		int xPos) {
		int indent = getLabelShift();
		int indentsize = Math.abs(indent);
		if(labelAndScrollCoordsAreOpposite()) {
			debug("Columns. Extent: [" + getSecondaryScrollBar().getModel()
				.getExtent() +
				"] Pane Size: [" + getSavedSecondaryPaneSize() + "]",7);
			if((lastScrollEndPos != -1) && (lastScrollPos != -1) && ((labelLen +
				indentsize) > (/* Extent */lastScrollEndPos -
					lastScrollPos))) {
				if(lastScrollPos > ((lastScrollEndPos + lastScrollEndGap) -
					(xPos + labelLen))) {
					debug("Drawing left/down arrow at lastScrollEndPos[" +
						lastScrollEndPos + "]",7);

					// Draw arrow on top
					drawRightArrow(g,yPos,getSavedSecondaryPaneSize() -
						lastScrollPos,height,fgColor,bgColor);
				}

				// if(lastScrollPos > xPos || lastScrollEndGap == 0) {
				// If the starting position of the string (from the bottom) is
				// larger than the scroll end gap (which is also at the bottom)
				// Note, the indent covers up what would otherwise be visible
				// label, so we must add the indent size to the non-visible
				// portion of the pane content
				if((lastScrollEndGap + indentsize) > xPos) {
					// Draw arrow on bottom
					drawLeftArrow(g,yPos,getSavedSecondaryPaneSize() -
						(lastScrollEndPos - indentsize -
							1),height,fgColor,bgColor);

					debug("Drawing column left overrun arrow at position (" +
						"paneSize - (lastScrollEndPos - indentsize - 1)) [" +
						getSavedSecondaryPaneSize() +
						" - (" +
						lastScrollEndPos +
						" - " +
						indentsize +
						" - 1)] = [" +
						(getSavedSecondaryPaneSize() - (lastScrollEndPos -
							indentsize -
							1)) + "]",15);
				}
				else {
					debug(
						"No overrun arrow drawn because " +
						"(lastScrollEndGap != 0 && lastScrollEndGap > " +
						"xPos) [" + lastScrollEndGap + " != 0 && " +
						lastScrollEndGap + " > " + xPos +
						"] lastScrollEndPos [" + lastScrollEndPos +
						"] lastScrollPos [" + lastScrollPos + "]",10);
				}
			}
		}
		else {
			debug("Rows. Extent: [" +
				getSecondaryScrollBar().getModel().getExtent() +
				"] Pane Size: [" + getSavedSecondaryPaneSize() + "]",7);
			if((lastScrollEndPos != -1) && (lastScrollPos != -1) && ((labelLen +
				indentsize) > (/* Extent */lastScrollEndPos -
					lastScrollPos))) {
				if((lastScrollEndPos - indentsize) < (xPos + labelLen)) {
					// Draw arrow on right side
					drawRightArrow(g,yPos,lastScrollEndPos - indentsize -
						1,height,fgColor,bgColor);

					debug("Right overrun " + getPaneType() +
						" arrow drawn at [" +
						lastScrollEndPos + " + " + indent +
						" - 1] because  (lastScrollEndPos < (xPos + " +
						"labelLen + indent) || lastScrollPos == 0) [" +
						lastScrollEndPos + " < (" + xPos + " + " + labelLen +
						" + " +
						indent + ") || " + lastScrollPos +
						" == 0] lastScrollEndGap [" +
						lastScrollEndGap + "]",10);
				}
				else {
					debug(
						"No overrun right " + getPaneType() +
						" arrow drawn because NOT (lastScrollEndPos < " +
						"(xPos + labelLen + indent) || lastScrollPos == 0) [" +
						lastScrollEndPos + " < (" + xPos + " + " +
						labelLen + " + " + indent + ") || " + lastScrollPos +
						" == 0] lastScrollEndGap [" +
						lastScrollEndGap + "]",10);
				}
				if(lastScrollPos > xPos) {
					// Draw arrow on left side
					drawLeftArrow(g,yPos,lastScrollPos,height,fgColor,bgColor);
				}
			}
		}
	}

	/**
	 * This method draws arrows for labels that run out of the viewport view.
	 * Call BEFORE rotating the column labels
	 *
	 * @return
	 */
	public void drawLeftArrow(final Graphics g,int yTop,int xLeft,int height,
		Color fgColor,Color bgColor) {
		// Drag a background box to cover up text that we're going to draw over
		g.setColor(bgColor);
		g.fillRect(xLeft - (isAColumnPane() ? 1 : 0
			// 1 is a fudge factor for the x axis - not sure why I need it
			),yTop - 2, /* Fudge factor to cover up the tops of | chars */
			(int) Math.floor(height / 2) + 1,height + 3 /* Fudge factor to cover up underscores */);

		// Make the arrow a little smaller
		height -= 2;

		// Make sure there's an odd number of pixels
		if((height % 2) == 0) {
			height--;
		}
		if(height < 1) { return; }

		// We can do a better job of drawing an equilateral triangle than the
		// polygon method does by drawing smaller and smaller lines in a loop
		g.setColor(fgColor);
		int curheight = height;
		int indent = 0;
		for(int xcoord = xLeft + (int) Math.floor(height / 2);xcoord >= xLeft;
			xcoord--) {
			g.drawLine(xcoord,yTop + indent,xcoord,yTop + indent + curheight);
			curheight -= 2;
			indent += 1;
			if(height < 1)
			{
				break; // Just in case
			}
		}
	}

	public int getLabelShiftSize() {
		return(doDrawLabelPort() ? indicatorThickness : labelIndent);
	}

	public int getLabelShift() {
		return(getLabelShiftSize() * (isLabelStartNearMatrix() ? 1 : -1));
	}

	protected abstract boolean isLabelStartNearMatrix();

	protected abstract boolean isMatrixJustified();

	protected abstract boolean isAColumnPane();

	/**
	 * This method draws arrows for labels that run out of the viewport view.
	 * Call BEFORE rotating the column labels
	 *
	 * @return
	 */
	public void drawRightArrow(final Graphics g,int yTop,int xRight,int height,
		Color fgColor,Color bgColor) {
		// Drag a background box to cover up text that we're going to draw over
		g.setColor(bgColor);
		g.fillRect(
			xRight -
			(int) Math.floor(height / 2) -
			(isAColumnPane() ? 1 : 0 /* Fudge factor for x axis - don't know why */),
			yTop - 2, /* Fudge factor to cover up the tops of | chars */
			(int) Math.floor(height / 2) + 3,height +
			3 /* Fudge factor to cover up underscores */);

		// Make the arrow a little smaller
		height -= 2;

		// Make sure there's an odd number of pixels
		if((height % 2) == 0) {
			height--;
		}

		// We can do a better job of drawing an equilateral triangle than the
		// polygon method does by drawing smaller and smaller lines in a loop
		g.setColor(fgColor);
		int curheight = height;
		int indent = 0;
		for(int xcoord = xRight - (int) Math.floor(height / 2);xcoord <= xRight;
			xcoord++) {
			debug("Drawing RIGHT ARROW",7);
			g.drawLine(xcoord,yTop + indent,xcoord,yTop + indent + curheight);
			curheight -= 2;
			indent += 1;
			if(height < 1)
			{
				break; // Just in case
			}
		}
	}

	// Timer to let the label pane linger a bit (prevents flashing when passing
	// between panes which do not change the visibility of the label panes)
	final private int labelPortOffDelay = 250;
	private javax.swing.Timer paneLabelPortOffTimer;
	ActionListener paneLabelPortOffListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent evt) {
			if(evt.getSource() == paneLabelPortOffTimer) {
				/* Stop timer */
				paneLabelPortOffTimer.stop();
				paneLabelPortOffTimer = null;

				map.setOverLabels(false);
				map.notifyObservers();
				repaint();
			}
		}
	};

	// And this listener is for hovers over the secondary scrollbar, since they
	// each are independent with regard to hovering on or off them
	private javax.swing.Timer overScrollLabelPortOffTimer;
	ActionListener scrollLabelPortOffListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent evt) {
			if(evt.getSource() == overScrollLabelPortOffTimer) {
				debug("You hovered off the secondary row scrollbar 1s ago, " +
					"so the label port might turn off unless you're over " +
					"another pane that activates it",2);
				/* Stop timer */
				overScrollLabelPortOffTimer.stop();
				overScrollLabelPortOffTimer = null;

				map.setOverLabelsScrollbar(false);
				map.notifyObservers();
				repaint();
			}
		}
	};

	// And this listener is for click releases off the secondary scrollbar,
	// because they can hover off the scrollbar and you don't want the knob and
	// labels to disappear while dragging the knob
	private javax.swing.Timer activeScrollLabelPortOffTimer;
	ActionListener activeLabelPortOffListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent evt) {
			if(evt.getSource() == activeScrollLabelPortOffTimer) {
				debug("You released the secondary scrollbar 1s ago, so " +
					"the label port might turn off unless you're over " +
					"another pane that activates it",2);
				/* Stop timer */
				activeScrollLabelPortOffTimer.stop();
				activeScrollLabelPortOffTimer = null;

				map.setLabelsBeingScrolled(false);
				map.notifyObservers();
				repaint();
			}
		}
	};

	@Override
	public void mouseEntered(final MouseEvent e) {
		if(paneLabelPortOffTimer != null) {
			/* Event came too soon, swallow it by resetting the timer.. */
			paneLabelPortOffTimer.stop();
			paneLabelPortOffTimer = null;
		}
		/*
		 * TODO: If anastasia doesn't like trees linked to whizzing labels,
		 * uncomment the following commented code. If she likes it, delete. This
		 * code saves compute cycles, but makes the trees sometimes move out of
		 * sync with the labels.
		 */
		// if(map.wasLastTreeModeGlobal() && map.shouldKeepTreeGlobal()) {
		// map.setKeepTreeGlobal(true);
		// } else {
		map.setKeepTreeGlobal(false);
		// }
		map.setOverLabels(true);

		super.mouseEntered(e);
	}

	@Override
	public void mouseExited(final MouseEvent e) {
		// Turn off the "over a label port view" boolean after a bit
		if(paneLabelPortOffTimer == null) {
			if(labelPortOffDelay == 0) {
				map.setOverLabels(false);
				map.notifyObservers();
				repaint();
			}
			else {
				/*
				 * Start waiting for delay millis to elapse and then call
				 * actionPerformed of the ActionListener
				 * "paneLabelPortOffListener".
				 */
				paneLabelPortOffTimer = new Timer(labelPortOffDelay,
					paneLabelPortOffListener);
				paneLabelPortOffTimer.start();
			}
		}

		map.setKeepTreeGlobal(true);
		map.unsetHoverPixel();
		unsetPrimaryHoverIndex();

		super.mouseExited(e);
		repaint();
	}

	/**
	 * TODO: This needs to go into a generic selection class and then the
	 * TreeSelectionI param can be removed
	 */
	public void toggleSelect(TreeSelectionI selection,int index) {
		if(selection.isIndexSelected(index)) {
			selection.setIndexSelection(index,false);
		} else {
			selection.setIndexSelection(index,true);
		}
	}

	/**
	 * TODO: This needs to go into a generic selection class and then the
	 * TreeSelectionI param can be removed
	 */
	public void toggleSelectFromClosestToIndex(TreeSelectionI selection,
		int index) {
		// If this index is selected (implying other selections may exist),
		// deselect from closest deselected to sent index
		if(selection.isIndexSelected(index)) {

			int closest = -1;
			for(int i = 0;i < selection.getNumIndexes();i++) {
				if(!selection.isIndexSelected(i) &&
					(((closest == -1) && (Math.abs(i - index) < Math.abs(0 -
						index))) ||
						((closest > -1) && (Math.abs(i -
							index) < Math.abs(closest -
								index))))) {
					closest = i;
					// LogBuffer.println("Closest index updated to [" +
					// closest + "] because index [" + index +
					// "] is closer [distance: " +
					// Math.abs(i - index) + "] to it.");
				}
				else if((i == (selection.getNumIndexes() - 1)) &&
					selection.isIndexSelected(i) &&
					(((closest == -1) && (Math.abs(i - index) <
					Math.abs(0 - index))) ||
					((closest > -1) && (Math.abs(i - index) < Math.abs(closest -
						index))))) {
					closest = i + 1;
				}
			}
			// LogBuffer.println("Closest index: [" + closest + "].");
			if(closest < index) {
				for(int i = closest + 1;i <= index;i++) {
					selection.setIndexSelection(i,false);
				}
			}
			else {
				for(int i = index;i < closest;i++) {
					selection.setIndexSelection(i,false);
				}
			}
		}
		// Else if other selections exist (implied that current index is not
		// selected), select from sent index to closest selected
		else if(selection.getNSelectedIndexes() > 0) {
			int[] selArrays = selection.getSelectedIndexes();
			int closest = selArrays[0];
			for(int i = 0;i < selArrays.length;i++) {
				if(Math.abs(selArrays[i] - index) < Math.abs(closest - index)) {
					closest = selArrays[i];
					// LogBuffer.println("Closest index updated to [" +
					// closest + "] because index [" + index +
					// "] is closer [distance: " +
					// Math.abs(selArrays[i] - index) + "] to it.");
				}
			}
			if(closest < index) {
				for(int i = closest + 1;i <= index;i++) {
					selection.setIndexSelection(i,true);
				}
			}
			else {
				for(int i = index;i < closest;i++) {
					selection.setIndexSelection(i,true);
				}
			}
		}
		// Else when no selections exist, just select this index
		else {
			selection.deselectAllIndexes();
			selection.setIndexSelection(index,true);
		}
	}

	/**
	 * TODO: This needs to go into a generic selection class and then the
	 * TreeSelectionI param can be removed
	 */
	public void selectAnew(TreeSelectionI selection,int index) {
		selection.deselectAllIndexes();
		selection.setIndexSelection(index,true);
	}

	abstract public int getLabelOrientation();

	@Override
	public void adjustmentValueChanged(AdjustmentEvent evt) {

		Adjustable source = evt.getAdjustable();
		int orient = source.getOrientation();
		if(orient == getLabelOrientation()) {
			debug("scrollbar adjustment detected from secondary scrollbar",2);
		}
		int oldvalue = getSecondaryScrollBar().getValue();
		boolean updateScroll = false;
		// This if conditional catches drags
		if(!evt.getValueIsAdjusting() && map.areLabelsBeingScrolled()) {
			debug("The knob on the scrollbar is being dragged",7);
			updateScroll = true;
			explicitSecondaryScrollTo(oldvalue,-1,-1);
		}
		// This gets ANY other scroll event, even programmatic scrolls called
		// from the code, but we only want to do anything when the scrollbar is
		// clicked - everything else is either the scroll wheel or a coded re-
		// scroll that we don't want to change anything
		else {
			updateScroll = true;
			int newvalue = evt.getValue();
			if(oldvalue != newvalue) {
				int type = evt.getAdjustmentType();
				switch(type) {
					case AdjustmentEvent.UNIT_INCREMENT:
						debug("Scrollbar was increased by one unit",1);
						break;
					case AdjustmentEvent.UNIT_DECREMENT:
						debug("Scrollbar was decreased by one unit",1);
						break;
					case AdjustmentEvent.BLOCK_INCREMENT:
						debug("Scrollbar was increased by one block",1);
						break;
					case AdjustmentEvent.BLOCK_DECREMENT:
						debug("Scrollbar was decreased by one block",1);
						break;
					case AdjustmentEvent.TRACK:
						debug("A non-scrollbar scroll event was detected (a " +
							"call from code or a mouse wheel event)",1);
						updateScroll = false;
						break;
				}
				debug("Scrolling from: [" + source.getValue() + " or (" +
					oldvalue + ")] to: [" + newvalue + "] via [" +
					evt.getSource() + "]",7);
				if(updateScroll) {
					explicitSecondaryScrollTo(newvalue,-1,-1);
				}
			}
		}
		if(updateScroll) {
			repaint();
		}
	}

	/*
	 * TODO: Eliminate this and use adjustmentValueChanged instead because it is
	 * more holistic
	 */
	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {

		final int notches = e.getWheelRotation();
		int shift = (notches < 0) ? -6 : 6;
		debug("Scroll wheel event detected",1);

		// On macs' magic mouse, horizontal scroll comes in as if the shift was
		// down
		if(isASecondaryScroll(e)) {
			final int j = getSecondaryScrollBar().getValue();
			if((j + shift) < 0) {
				shift = -j;
			}
			else if((j + shift +
				getSecondaryScrollBar().getModel().getExtent()) >
				getSecondaryScrollBar().getMaximum()) {

				shift = getSecondaryScrollBar().getMaximum() - (j +
					getSecondaryScrollBar().getModel().getExtent());
			}
			if(shift == 0) { return; }
			debug("Scrolling horizontally from [" + j + "] by [" + shift + "]",
				2);
			lastScrollPos = j + shift;
			lastScrollEndPos = lastScrollPos +
				getSecondaryScrollBar().getModel().getExtent();
			lastScrollEndGap = getSecondaryScrollBar().getMaximum() -
				lastScrollEndPos;
			if(lastScrollEndGap < 0) {
				lastScrollPos -= lastScrollEndGap;
				lastScrollEndPos -= lastScrollEndGap;
				lastScrollEndGap = 0;
			}
			else if(lastScrollPos < 0) {
				lastScrollEndPos += lastScrollPos;
				lastScrollEndGap += lastScrollPos;
				lastScrollPos = 0;
			}
			debug("New scroll position [" + lastScrollPos + "] end pos: [" +
				lastScrollEndPos + "] end gap: [" + lastScrollEndGap +
				"] out of [" + getSecondaryScrollBar().getMaximum() +
				"]",12);
			repaint();
			// Moved the setValue call here, after the repaint, because putting
			// it before caused the scroll to look really bouncy and choppy
			getSecondaryScrollBar().setValue(j + shift);
		}
		else {
			// Value of label length scrollbar
			map.scrollBy(shift);
			updatePrimaryHoverIndexDuringScrollWheel();
		}
	}

	protected abstract boolean isASecondaryScroll(final MouseWheelEvent e);

	/**
	 * This method was initially created to perform scrolling of labels (along
	 * their length) specifically by scroll-handle drag events initiated by the
	 * user. It is necessary for a few reasons: 1. Individual label positions
	 * are manipulated to always keep at least part of the label visible and the
	 * system's scroll-control would not do that. 2. We need to update the
	 * scroll position data members so that we can return to them after
	 * systematic interface changes (e.g. drawing a hint on hover-out and window
	 * or split-pane resizes). It is abstract because row/label panes are
	 * rotated into different orientations.
	 *
	 * @param pos
	 *            - scroll position
	 * @param endPos
	 *            - scroll position + extent
	 * @param endGap
	 *            - max scroll (i.e. offscreen pane size) - endPos
	 */
	public void explicitSecondaryScrollTo(int pos,int endPos,int endGap) {
		debug("Explicitly scrolling to [" + pos + "]",12);
		if(pos < 0) {
			pos = 0;
		}
		if(pos > (getSecondaryScrollBar().getMaximum() -
			getSecondaryScrollBar().getModel().getExtent())) {
			pos = getSecondaryScrollBar().getMaximum() -
				getSecondaryScrollBar().getModel().getExtent();
		}
		if(endPos > 0) {
			endPos += (pos - getSecondaryScrollBar().getValue());
		}
		else {
			endPos = pos + getSecondaryScrollBar().getModel().getExtent();
		}
		if(endGap == -1) {
			endGap = getSecondaryScrollBar().getMaximum() - endPos;
		}
		lastScrollPos = pos;
		lastScrollEndPos = endPos;
		lastScrollEndGap = endGap;
		getSecondaryScrollBar().setValue(pos);
	}

	protected void selectionChanged() {
		offscreenValid = false;
		revalidate();
		repaint();
	}

	public void setDataModel(final DataModel dataModel) {

		this.dataModel = dataModel;
	}

	/**
	 * Exports an image with a set of labels
	 *
	 * @param g - graphics object
	 * @param xIndent - size of the indent where to start drawing the labels
	 * @param yIndent - size of the indent where to start drawing the labels
	 * @param size - size of a matrix tile or rather, font height area
	 * @param region - what portion of the labels to export
	 */
	public void export(final Graphics g,final int xIndent,final int yIndent,
		final int size,final RegionType region,final boolean showSelections,
		final boolean drawSelectedOnly,final int fontSize) {

		if(region == RegionType.ALL) {
			exportAll(g,xIndent,yIndent,size,showSelections,drawSelectedOnly,
				fontSize);
		} else if(region == RegionType.VISIBLE) {
			exportVisible(g,xIndent,yIndent,size,showSelections);
		} else if(region == RegionType.SELECTION) {
			exportSelection(g,xIndent,yIndent,size,showSelections);
		} else {
			LogBuffer.println("ERROR: Invalid export region: [" + region +
				"].");
		}
	}

	/**
	 * Exports all the labels
	 *
	 * @param g - graphics object
	 * @param xIndent - size of the indent where to start drawing the labels
	 * @param yIndent - size of the indent where to start drawing the labels
	 * @param size - size of a matrix tile or rather, font height area
	 */
	public void exportAll(final Graphics g,final int xIndent,
		final int yIndent,final int size,final boolean showSelections,
		final boolean drawSelectedOnly,final int fontSize) {

		final Graphics2D g2d = (Graphics2D) g;

		//Turn on anti-aliasing so the text looks better
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);

		Font exportFont = new Font(labelAttr.getFace(),
				labelAttr.getStyle(),fontSize);
		final FontMetrics metrics = getFontMetrics(exportFont);
		int xSize = getMaxStringLength(metrics);
		final int ascent = metrics.getAscent();
 
		/* Rotate plane for array axis (not for zoomHint) */
		orientLabelsForExport(g2d,xIndent,yIndent,xSize);

		final int start = map.getMinIndex();
		final int end = map.getMaxIndex();

		//Labels are always drawn horizontally.  orientLabelPane does its magic
		//to rotate the whole thing, so we don't have to worry about it.  Thus
		//yPos is the "pixel" (i.e. "point") position referring to lines of
		//labels.  yOffset is to center the label on a tile.
		//yPos and xPos are assumed to start at 0.
		int yPos = yIndent;
		int yOffset = (int) Math.floor((double) size / 2.0);
		int xPos = xIndent;

		LogBuffer.println("Drawing [" + (isAColumnPane() ? "Column" : "Row") + "] labels" +
			"Longest Str Len: [" + xSize + "] font size: [" + fontSize + "] tile size: [" + size + "] ascent: [" + ascent + "] yIndent: [" + yIndent + "] xIndent: [" + xIndent + "] yOffset: [" + yOffset + "]");

		//This really just defines the size of the drawing area
		//g.clearRect(0,0,xSize,ySize);

		for(int j = start;j <= end;j++) {

			debug("Getting data index [" + j + "]",1);

			try {
				String out = labelSummary.getSummary(labelInfo,j);

				if(out == null) {
					out = "No Label";
				}

				/*
				 * This will draw the label background if selected
				 */
				if(drawSelection.isIndexSelected(j) && showSelections) {

					debug("Drawing yellow background for selected index [" +
						j + "] because shpwSelections is [" +
						(showSelections ? "true" : "false") + "]",7);

					g.setColor(selectionTextBGColor);

					g.fillRect(xIndent,yPos,xSize,size);
				} else if(!drawSelection.isIndexSelected(j) &&
					drawSelectedOnly) {

					//Skip this label if it is not selected and we're only
					//drawing selected labels
					yPos += size;
					continue;
				}

				/* Set label color */
				g2d.setColor(Color.black);
				g2d.setFont(exportFont);
				

				/* Finally draw label (alignment-dependent) */
				xPos = xIndent;
//					getLabelStartOffset(metrics.stringWidth(out));
				if(labelAttr.isRightJustified()) {
					xPos += (xSize - metrics.stringWidth(out));
				}

				if(isAColumnPane()) {
					LogBuffer.println("Drawing column label [" + out + "] at x[" + xPos + "] y[" + (yPos + yOffset + (ascent / 2)) + "]");
				}
				g2d.drawString(out,xPos,yPos + yOffset + (ascent / 2));

//				drawOverrunArrows(metrics.stringWidth(out),g,
//					map.getMiddlePixel(j) -
//					(ascent / 2),labelAttr.getPoints(),g2d.getColor(),
//					bgColor,labelStrStart);
			}
			catch(final java.lang.ArrayIndexOutOfBoundsException e) {
				LogBuffer.logException(e);
				break;
			}

			yPos += size;
		}
	}

	/**
	 * Exports the portion of the labels corresponding to the visible portion of
	 * the matrix
	 *
	 * @param g - graphics object
	 * @param xIndent - size of the indent where to start drawing the labels
	 * @param yIndent - size of the indent where to start drawing the labels
	 * @param size - size of a matrix tile or rather, font height area
	 */
	public void exportVisible(final Graphics g,final int xIndent,
		final int yIndent,final int size,final boolean showSelections) {

	}

	/**
	 * Exports the portion of the labels corresponding to the minimum & maximum
	 * selected portions of the matrix
	 *
	 * @param g - graphics object
	 * @param xIndent - size of the indent where to start drawing the labels
	 * @param yIndent - size of the indent where to start drawing the labels
	 * @param size - size of a matrix tile or rather, font height area
	 */
	public void exportSelection(final Graphics g,final int xIndent,
		final int yIndent,final int size,final boolean showSelections) {

	}
}
