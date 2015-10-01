package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.util.Observable;
import java.util.prefs.Preferences;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;
import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.UrlExtractor;

public abstract class LabelView extends ModelView implements MouseListener,
	MouseMotionListener,FontSelectable,ConfigNodePersistent {

	private static final long serialVersionUID = 1L;

	/* Axis IDs */
	protected final static int ROW = 0;
	protected final static int COL = 1;

	protected final static int HINTFONTSIZE = 14;

	//This is the space between labels in pixels
	protected final static int SQUEEZE = 1;


	/* DataModel is an observer */
	protected DataModel dataModel;

	/* Required label data */
	protected HeaderInfo headerInfo;
	protected HeaderSummary headerSummary;

	/* Maps label position to GlobalView */
	protected MapContainer map;

	/* Use labels to generate URLs for look-up */
	protected UrlExtractor urlExtractor;

	/* Default label settings */
	protected final String d_face = "Courier";
	protected final int d_style = 0;
	protected final int d_size = 14;
	protected final int d_min = 8;
	protected final int d_max = 30;
	protected boolean d_justified;
	protected boolean d_fixed = false;
	protected int longest_str_index;
	protected int longest_str_length;
	protected String longest_str;

	/* Custom label settings */
	protected String  face;
	protected int     style;
	protected int     size;
	protected String  lastDrawnFace;  //used only by getMaxStringLength
	protected int     lastDrawnStyle; //used only by getMaxStringLength
	protected int     lastDrawnSize;  //used only by getMaxStringLength
	protected int     min;
	protected int     max;
	protected int     last_size;
	protected boolean isFixed;

	/* Panel sizing */
	protected int maxlength = 0;

	/* Keeps track of label index with mouse cursor on top */
	protected int hoverIndex = -1;

	//Keep track of where the mouse is so that the hover index can be updated
	//during scrollwheel motion
	protected int hoverPixel = -1;

	/* Alignment status */
	protected boolean isRightJustified;

	/* Selection of row/ column indices */
	protected TreeSelectionI arraySelection;
	protected TreeSelectionI geneSelection;
	protected Color textFGColor          = Color.black;
	protected Color textBGColor;
	protected Color selectionBorderColor = Color.yellow;
	protected Color selectionTextBGColor = new Color(249,238,160); //soft yellow
	protected Color selectionTextFGColor = Color.black;
	protected Color hoverTextFGColor     = Color.red;
	protected Color labelPortColor       = new Color(30,144,251);  //soft blue

	/*
	 * Stores a reference to the TreeSelection relevant for drawing -- allows to
	 * keep one paint method updateBuffer() for gene- and arraySelection
	 */
	protected TreeSelectionI drawSelection;
	protected TreeSelectionI otherSelection;

	/* Contains all the saved information */
	protected Preferences configNode;

	private final boolean isGeneAxis;

	protected JScrollPane scrollPane;
	protected String zoomHint;

	/* "Position indicator" settings for when the label port is active */
	int matrixBarThickness = 3; //Thickness of matrix position bar - Must be odd
	int labelBarThickness = 0; //Must be odd! (so red hover line is centered)
	int labelIndicatorProtrusion = 0;
	//^^Distance label hover indicator protrudes from label bar - point on
	//indicator
	int matrixIndicatorIntrusion =                          //Point on indicator
		(labelIndicatorProtrusion > matrixBarThickness ||
		 labelIndicatorProtrusion == 0 ?
		 (int) Math.ceil(matrixBarThickness / 2) :
		 labelIndicatorProtrusion);
	int labelIndent = 3; //Distance label starts after end of label indicator
	int indicatorThickness = matrixBarThickness + labelBarThickness +
		labelIndicatorProtrusion + labelIndent;
	//^^Should be assigned a value summing the following values if drawing a
	//label port
	int lastScrollRowEndGap  = -1;
	int lastScrollRowPos     = -1;
	int lastScrollRowEndPos  = -1;
	int lastScrollColEndGap  = -1;
	int lastScrollColPos     = -1;
	int lastScrollColEndPos  = -1;
	int rowLabelPaneSize     = -1;
	int colLabelPaneSize     = -1;
	int rowLabelViewportSize = -1;
	int colLabelViewportSize = -1;
	/* TODO: Instead of resetting the justification position whenever the font
	 * size changes, we should calculate and remember the relative position of
	 * the scrollbar */

	public LabelView(final int axis_id) {

		super();

		isGeneAxis = axis_id == ROW;
		setLayout(new MigLayout());

		final String summary = isGeneAxis ? "GeneSummary" : "ArraySummary";
		headerSummary = new HeaderSummary(summary);

		//this.urlExtractor = uExtractor;

		addMouseMotionListener(this);
		addMouseListener(this);

		setLabelPortMode(true);

		if(isGeneAxis) {
			scrollPane = new JScrollPane(this,
				            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
				            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		} else {
			scrollPane = new JScrollPane(this,
			                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
			                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		}
		
		scrollPane.setBorder(null);

		debug = 0;
		//Debug modes:
		//9 = debug repaint timer intervals and updates to label panels
		//10 = Debug label drawing issues when split pane divider adjusted
		//12 = Debug issues where strings and indicator bar are starting in the
		//     wrong place
		//13 = Debug negative label start positions when right-justified and
		//     split pane divider position's resultant label viewport size is
		//     slightly smaller than the longest label(s)
		//14 = Debug position indicator not always drawn when data is large
		//     (number of rows per pixel > 1).
		//15 = Debug overrun arrow position in non-label-port mode
		//16 = Debug issue where short top-justified column labels are not
		//     visible after dragging the split pane divider to make the label
		//     area larger
		//17 = Debug row label highlight drawing position

		panel = scrollPane;
	}

	protected abstract void adjustScrollBar();

	/**
	 * Used to space the array names.
	 *
	 * @param im
	 *            A new mapcontainer.
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

	public void setHeaderInfo(final HeaderInfo headerInfo) {
		this.headerInfo = headerInfo;
	}

	public HeaderInfo getHeaderInfo() {
		return headerInfo;
	}

	public void setHeaderSummary(final HeaderSummary headerSummary) {
		this.headerSummary = headerSummary;
	}

	public HeaderSummary getHeaderSummary() {
		return headerSummary;
	}

	public void setUrlExtractor(final UrlExtractor urlExtractor) {
		this.urlExtractor = urlExtractor;
	}

	public UrlExtractor getUrlExtractor() {
		return urlExtractor;
	}

	/**
	 * Set geneSelection
	 *
	 * @param geneSelection
	 *            The TreeSelection which is set by selecting genes in the
	 *            GlobalView
	 */
	public void setRowSelection(final TreeSelectionI geneSelection) {
		if(this.geneSelection != null) {
			this.geneSelection.deleteObserver(this);
		}

		this.geneSelection = geneSelection;
		this.geneSelection.addObserver(this);

		if(isGeneAxis) {
			drawSelection = geneSelection;
		} else {
			otherSelection = geneSelection;
		}
	}

	/**
	 * Set geneSelection
	 *
	 * @param geneSelection
	 *            The TreeSelection which is set by selecting genes in the
	 *            GlobalView
	 */
	public void setColSelection(final TreeSelectionI arraySelection) {
		if(this.arraySelection != null) {
			this.arraySelection.deleteObserver(this);
		}
		this.arraySelection = arraySelection;
		this.arraySelection.addObserver(this);

		if(!isGeneAxis) {
			drawSelection = arraySelection;
		} else {
			otherSelection = arraySelection;
		}
	}

	@Override
	public void update(final Observable o,final Object arg) {}

	@Override
	public void setConfigNode(final Preferences parentNode) {
		if(parentNode != null) {
			configNode = parentNode;

		} else {
			LogBuffer.println("parentNode for LabelView was null.");
			return;
		}

		LogBuffer.println("Setting new label configNode");
		importSettingsFromNode(configNode);
	}

	public void importSettingsFromNode(Preferences node) {
		setMin(node.getInt("min",d_min));
		setMax(node.getInt("max",d_max));
		setFace(node.get("face",d_face));
		setStyle(node.getInt("style",d_style));
		setSavedPoints(node.getInt("size",d_size));
		setJustifyOption(node.getBoolean("isRightJustified",d_justified));
		setFixed(node.getBoolean("isFixed",d_fixed));

		//Signal that the secondary scroll position should be reset the next
		//time it is needed
		resetSecondaryScroll();

		getHeaderSummary().setConfigNode(node);
	}

	@Override
	public String getFace() {
		return face;
	}

	@Override
	public int getPoints() {
		return size;
	}

	@Override
	public int getLastSize() {
		return last_size;
	};

	@Override
	public int getMin() {
		return min;
	}

	@Override
	public int getMax() {
		return max;
	}

	@Override
	public int getStyle() {
		return style;
	}

	@Override
	public boolean getFixed() {
		return isFixed;
	}

	@Override
	public boolean getJustifyOption() {
		return isRightJustified;
	}

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

	public int getHoverPixel() {
		return(hoverPixel);
	}

	public void setHoverPixel(int pixelIndex) {
		hoverPixel = pixelIndex;
	}

	/**
	 * This is for updating the hover index when the mouse has not moved but the
	 * data under it has (like when using the scroll wheel)
	 */
	abstract public void updatePrimaryHoverIndexDuringScrollDrag();
	abstract public void forceUpdatePrimaryHoverIndex();
	abstract public boolean areLabelsBeingScrolled();

	public void updatePrimaryHoverIndexDuringScrollWheel() {
		if(hoverPixel == -1) {
			hoverIndex = -1;
		} else {
			hoverIndex = map.getIndex(hoverPixel);
		}
	}

	@Override
	public void setFace(final String string) {
		if(face == null || !face.equals(string)) {
			face = string;
			if(configNode != null) {
				configNode.put("face",face);
			}
			setFont(new Font(face,style,size));
			resetSecondaryScroll();
			adjustScrollBar();
			repaint();
		}
	}

	/**
	 * Wrapper for setPoints which allows to save the newly set points to an
	 * instance variable.
	 * 
	 * @param i
	 */
	@Override
	public void setSavedPoints(final int i) {
		/* Stay within boundaries */
		int new_i = i;

		last_size = new_i;
		setPoints(new_i);
	}

	@Override
	public void setPoints(final int i) {
		if(size != i) {
			size = i;
			if(configNode != null) {
				configNode.putInt("size",size);
			}
			setFont(new Font(face,style,size));
			resetSecondaryScroll();
			adjustScrollBar();
			repaint();
		}
	}

	public void setTemporaryPoints(final int i) {
		debug("Setting temporary points",2);
		setFont(new Font(face,style,i));
		//repaint();
	}

	@Override
	public void setStyle(final int i) {
		if(style != i) {
			style = i;
			if(configNode != null) {
				configNode.putInt("style",style);
			}
			setFont(new Font(face,style,size));
			resetSecondaryScroll();
			adjustScrollBar();
			repaint();
		}
	}

	@Override
	public void setMin(final int i) {
		if(i < 1 || max > 0 && i > max) { return; }

		if(min != i) {
			min = i;
			if(configNode != null) {
				configNode.putInt("min",min);
			}
			setFont(new Font(face,style,size));
			resetSecondaryScroll();
			adjustScrollBar();
			repaint();
		}
	}

	@Override
	public void setMax(final int i) {
		if(i < 1 || min > 0 && i < min) { return; }

		if(max != i) {
			max = i;
			if(configNode != null) {
				configNode.putInt("max",max);
			}
			setFont(new Font(face,style,size));
			resetSecondaryScroll();
			adjustScrollBar();
			repaint();
		}
	}

	@Override
	public void setJustifyOption(final boolean isRightJustified) {
		this.isRightJustified = isRightJustified;

		if(configNode != null) {
			configNode.putBoolean("isRightJustified",isRightJustified);
		}

		resetSecondaryScroll();

		/**
		 * Queue up this action so it will be run after LabelView is set up.
		 */
		javax.swing.SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				adjustScrollBar();
			}
		});
	}

	@Override
	public void setFixed(boolean fixed) {
		isFixed = fixed;

		resetSecondaryScroll();

		if(configNode != null) {
			configNode.putBoolean("isFixed",fixed);
		}
	};

	@Override
	public String viewName() {
		return "LabelView";
	}

	//This is an attempt to get the hovering of the mouse over the matrix to get
	//the label panes to update more quickly and regularly, as the
	//notifyObservers method called from MapContainer was resulting in sluggish
	//updates
	private int repaintInterval = 50;      //update every 50 milliseconds
	private int slowRepaintInterval = 1000;//update every 1s if mouse not moving
	private int lastHoverIndex = -1;
	private Timer repaintTimer =
		new Timer(repaintInterval,
		          new ActionListener() {
			/**
			 * The timer "ticks" by calling
			 * this method every _timeslice
			 * milliseconds
			 */
			@Override
			public void
			actionPerformed(ActionEvent e) {
				//This shouldn't be necessary, but when I change setPoints() to
				//setTemporaryPoints in the drawing of the HINT, the timer never
				//stops despite stop being continually called, so I'm going to
				//call stop in here if the map says that the animation is
				//supposed to have been stopped...
				if(!map.isLabelAnimeRunning()) {
					repaintTimer.stop();
				}
				repaint();
				//paintImmediately(0,0,getWidth(),getHeight());
			}
		});

	//Timer to wait a bit before stopping the slice _timer for painting
	final private int delay = 1000;
	private javax.swing.Timer turnOffRepaintTimer;
	ActionListener turnOffRepaintListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent evt) {
			if(evt.getSource() == turnOffRepaintTimer) {
				/* Stop timer */
				turnOffRepaintTimer.stop();
				turnOffRepaintTimer = null;

				//If we are still over a label port view panel, just slow the
				//repaint timer, because this was triggered by the mouse not
				//moving
				if(map.overALabelPortLinkedView()) {
					debug("Slowing the repaint interval presumably because " +
					      "of lack of mouse movement",9);
					repaintTimer.setDelay(slowRepaintInterval);
				} else {
					repaintTimer.stop();
					map.setLabelAnimeRunning(false);
				}
			}
		}
	};

	/* inherit description */
	@Override
	public void updateBuffer(final Graphics g) {
		
		updateBuffer(g, offscreenSize);
	}

	public void updateBuffer(final Image buf) {
		
		updateBuffer(buf.getGraphics(),
		             new Dimension(buf.getWidth(null),buf.getHeight(null)));
	}

	public void updateBuffer(final Graphics g,final Dimension offscreenSize) {

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

		//Update the hover position before repainting in case the user is
		//dragging the scrollbar (cursor can still hover over data during a
		//scroll drag)
		updatePrimaryHoverIndexDuringScrollDrag();

		int matrixHoverIndex = map.getHoverIndex();

		//We want to set active hover data index to the one stored for the
		//IMV when the mouse is hovered over the IMV or to the hover index
		//*here* when the cursor is hovered over this panel... HOWEVER, we
		//are going to base this on the value of the IMV hover index being
		//-1 because while drag-selecting, the user can hover off the edge,
		//causing the index to be less than 0 or greater than the max.  In
		//those cases, we want to set the hover index to whichever end they
		//hovered off of.
		int activeHoverDataIndex;

		if(matrixHoverIndex > -1 && map.isOverIMV()) {
			activeHoverDataIndex = matrixHoverIndex;
		} else if(hoverIndex > -1 || map.isSelecting() ||
		          areLabelsBeingScrolled()) {
			activeHoverDataIndex = hoverIndex;
		} else {
			forceUpdatePrimaryHoverIndex();
			activeHoverDataIndex = hoverIndex;
		}

		//The code above is overridden by this, which is a test to see if always
		//updating cursor location is better here or not instead of getting the
		//complexities of which hover index to use (which has proven difficult
		//to get just right in every scenario).
		/** TODO: Clean out usage of hoverIndex in deference to this method */
		forceUpdatePrimaryHoverIndex();
		activeHoverDataIndex = hoverIndex;
		if(isGeneAxis) {
			debug("Gene axis forced hover index: [" + activeHoverDataIndex +
			      "] isOverIMV? [" + (map.isOverIMV() ? "yes" : "no") + "]",9);
			if(scrollPane.getViewport().getSize().width != rowLabelViewportSize)
				debug("Current visible row label pane width: [" +
				      scrollPane.getViewport().getSize().width + "] Stored: [" +
				      rowLabelViewportSize + "] Content width: [" +
				      offscreenSize.width + "] Stored: [" + rowLabelPaneSize +
				      "]",10);
		} else if(scrollPane.getViewport().getSize().height !=
		          colLabelViewportSize) {
			debug("Current visible col label pane height: [" +
			      scrollPane.getViewport().getSize().height + "] Stored: [" +
			      colLabelViewportSize + "] Content height: [" +
			      offscreenSize.height + "] Stored: [" + colLabelPaneSize + "]",
			      10);
		}

		//Correct out of bounds situations, which can happen either when the
		//user is selecting and drags the cursor off the edge, the delay
		//hasn't expired for drawing labels upon hover off a label port view
		//panel, or the user is dragging the scrollbar and hovers off a
		//label port view pane.
		if(activeHoverDataIndex < 0 ||
		   activeHoverDataIndex > map.getMaxIndex()) {
			if(map.isSelecting()) {
				if(activeHoverDataIndex < 0) {
					activeHoverDataIndex = 0;
				} else {
					activeHoverDataIndex = map.getMaxIndex();
				}
			} else {
				forceUpdatePrimaryHoverIndex();
				activeHoverDataIndex = hoverIndex;
			}
		}

		//If the mouse is not hovering over the IMV, stop both timers, set the
		//last hover index, and tell mapcontainer that the animation has stopped
		if(activeHoverDataIndex < 0 ||
		   activeHoverDataIndex > map.getMaxIndex()) {
			debug("Not hovering over matrix - stopping animation",9);
			repaintTimer.stop();
			lastHoverIndex = -1;
			map.setLabelAnimeRunning(false);
			//Disable the turnOffRepaintTimer if it is running, because we've
			//already stopped repaints
			if(turnOffRepaintTimer != null) {
				turnOffRepaintTimer.stop();
				turnOffRepaintTimer = null;
			}
		}
		//Else, assume the mouse is hovering, and if the animation is not
		//running, start it up
		else if(!map.isLabelAnimeRunning()) {
			debug("Hovering across matrix - starting up animation",9);
			repaintTimer.start();
			map.setLabelAnimeRunning(true);
			lastHoverIndex = activeHoverDataIndex;
			//Disable any turnOffRepaintTimer that might have been left over
			if(turnOffRepaintTimer != null) {
				turnOffRepaintTimer.stop();
				turnOffRepaintTimer = null;
			}
		}
		//Else if the mouse hasn't moved, start the second timer to turn off the
		//first after 1 second (this mitigates delays upon mouse motion after a
		//brief period of no motion)
		else if(activeHoverDataIndex == lastHoverIndex) {
			debug("Hovering on one spot [" + lastHoverIndex +
			      "] - stopping animation",9);
			if(turnOffRepaintTimer == null) {
				turnOffRepaintTimer = new Timer(delay,turnOffRepaintListener);
				turnOffRepaintTimer.start();
			}
		}
		//Else, disable the turnOffRepaintTimer, update the hover index, and set
		//the repaint interval to normal speed
		else {
			debug("Hovering across matrix - keeping animation going",9);
			if(repaintTimer != null && !repaintTimer.isRunning()) {
				repaintTimer.start();
			} else if(repaintTimer.getDelay() == slowRepaintInterval) {
				debug("Speeding up the repaint interval because mouse " +
				      "movement detected",9);
				repaintTimer.setDelay(repaintInterval);
				repaintTimer.restart();
			}
			//Disable the turnOffRepaintTimer because we have detected continued
			//mouse motion
			if(turnOffRepaintTimer != null) {
				turnOffRepaintTimer.stop();
				turnOffRepaintTimer = null;
			}
			lastHoverIndex = activeHoverDataIndex;
			map.setLabelAnimeRunning(true);
		}

		debug("Updating the label pane graphics",1);
		debug("Hover indexes are (local) [" + hoverIndex + "] and (matrix) [" +
		      map.getHoverIndex() + "]",9);

		/* Shouldn't draw if there's no TreeSelection defined */
		if(drawSelection == null) {
			LogBuffer.println("Error: drawSelection not defined. Can't draw " +
			                  "labels.");
			return;
		}

		final Graphics2D g2d = (Graphics2D) g;
		final AffineTransform orig = g2d.getTransform();

		/* Draw labels if zoom level allows it */
		final boolean hasFixedOverlap = isFixed && map.getScale() < last_size;
		debug("Resetting justification for " + (isGeneAxis ? "ROW" : "COL") +
		      "s!!!!",1);

		//If the scale can accommodate the minimum font size and and can
		//accommodate a fixed-font-size (if the font has a fixed size) or the
		//label port is enabled
		if(map.getScale() >= getMin() + SQUEEZE && !hasFixedOverlap ||
		   doDrawLabelPort()) {

			int lastFontSize          = lastDrawnSize;
			//  ^^Changed in getMaxStringLength (and via getLabelAreaSize())

			final FontMetrics metrics = getFontMetrics(g.getFont());
			final int ascent          = metrics.getAscent();
			int maxStrLen             = getLabelAreaSize(metrics);
			int realMaxStrLen         = getMaxStringLength(metrics);
			int paneSizeShouldBe      = getLabelPaneContentSize(metrics);
			boolean drawLabelPort     = doDrawLabelPort();
			if(drawLabelPort) {
				debug("Drawing a label port",1);
			}

			//If the label pane's secondary dimension changed sizes or if the
			//font size has changed
			if(secondaryViewportSizeChanged() || lastFontSize != size) {
				if(isGeneAxis)
					debug("Viewport size change detected. Previous scroll " +
					      "positions: lastScrollRowPos [" + lastScrollRowPos +
					      "] lastScrollRowEndPos [" + lastScrollRowEndPos +
					      "] lastScrollRowEndGap [" + lastScrollRowEndGap + "]",
						10);
				else
					debug("Viewport size change detected. Previous scroll " +
					      "positions: lastScrollColPos [" + lastScrollColPos +
					      "] lastScrollColEndPos [" + lastScrollColEndPos +
					      "] lastScrollColEndGap [" + lastScrollColEndGap + "]",
					      10);

				adjustSecondaryScroll(realMaxStrLen +
				                      (drawLabelPort ?
				                       indicatorThickness : labelIndent));

				if(isGeneAxis)
					debug("Viewport size change detected. New scroll " +
					      "positions: lastScrollRowPos [" + lastScrollRowPos +
					      "] lastScrollRowEndPos [" + lastScrollRowEndPos +
					      "] lastScrollRowEndGap [" + lastScrollRowEndGap + "]",
					      10);
				else
					debug("Viewport size change detected. New scroll " +
					      "positions: lastScrollColPos [" + lastScrollColPos +
					      "] lastScrollColEndPos [" + lastScrollColEndPos +
					      "] lastScrollColEndGap [" + lastScrollColEndGap + "]",
					      10);
			}

			debug("Pane size should be [" + paneSizeShouldBe + "]",17);
			trackSecondaryPaneSize(paneSizeShouldBe);

			if(isFixed) {
				setSavedPoints(last_size);
			} else {
				adaptFontSizeToMapScale();
			}

			int curFontSize = size;
			int fittedFontSize = (int) Math.floor(map.getScale()) - SQUEEZE;
			if(fittedFontSize < 1) {
				fittedFontSize = 1;
			}

			/* Get indices range */
			//final int start = map.getIndex(0);
			//final int end   = map.getIndex(map.getAvailablePixels()) - 1;
			final int start = map.getFirstVisible();
			final int end   = start + map.getNumVisible() - 1;

			debug("Set end data index to [" + end + "] as first [" +
			      map.getFirstVisible() + "] plus num [" + map.getNumVisible() +
			      "] visible - 1",1);

			/* Rotate plane for array axis (not for zoomHint) */
			if(!isGeneAxis) {
				g2d.rotate(Math.PI * 3 / 2);
				g2d.translate(-offscreenSize.height,0);
			}

			final int colorIndex = headerInfo.getIndex("FGCOLOR");
			g.setFont(new Font(face,style,size));

			int matrixSize   = map.getPixel(map.getMaxIndex() + 1) - 1 -
			                   map.getPixel(0);
			int fullPaneSize = matrixSize;
			int viewportPrimarySize = (isGeneAxis ?
			                    rowLabelViewportSize : colLabelViewportSize);
			int labelStart   = activeHoverDataIndex;
			int labelEnd     = activeHoverDataIndex;

			debug("Label port mode is " + (drawLabelPort ? "" : "in") +
			      "active.  Map square size: [" + map.getScale() +
			      "] Font size: [" + curFontSize + "] SQUEEZE: [" + SQUEEZE +
			      "] ascent: [" + ascent + "]",1);

			textBGColor = g.getColor();

			/* Draw the labels */
			final Color fore = GUIFactory.MAIN;
			Color bgColor;
			int hoverStyle = Font.BOLD | style;
			if(drawLabelPort) {
				//See if the labels are going to be offset because they are near
				//an edge
				int hoverYPos  = map.getMiddlePixel(activeHoverDataIndex) +
				                 ascent / 2;
				int edgeOffset = 0;
				if((map.getMiddlePixel(activeHoverDataIndex) + ascent) >
				   fullPaneSize) {
					edgeOffset = map.getMiddlePixel(activeHoverDataIndex) +
					             ascent - fullPaneSize;
				} else if(map.getMiddlePixel(activeHoverDataIndex) -
				          (int) Math.ceil(ascent / 2) < 0) {
					edgeOffset = map.getMiddlePixel(activeHoverDataIndex) -
					             (int) Math.ceil(ascent / 2);
				}

				Color labelColor;
				//Draw labels from the hovered index backward to the start index
				//so that we show as many as there is room for, centered on
				//where the mouse is
				for(int j = activeHoverDataIndex;j >= start;j--) {
					try {
						String out = headerSummary.getSummary(headerInfo,j);
						final String[] headers = headerInfo.getHeader(j);

						if(out == null) {
							out = "No Label";
						}

						/* Set label color */
						if(j == activeHoverDataIndex) {
							labelColor = hoverTextFGColor;
						} else {
							if(drawSelection.isIndexSelected(j)) {
								labelColor = textFGColor;
								if(colorIndex > 0) {
									g.setColor(TreeColorer
									           .getColor(headers[colorIndex]));
								} else {
									labelColor = selectionTextFGColor;
								}

							} else {
								labelColor = textFGColor;
							}
						}
						g2d.setColor(labelColor);

						/* Finally draw label (alignment-dependent) */
						int xPos =
							getLabelStartOffset(metrics.stringWidth(out));
						debug("Label offset 1: [" +
						      getLabelStartOffset(metrics.stringWidth(out)) +
						      "]",11);

						int indexDiff = j - activeHoverDataIndex;
						int yPos = hoverYPos + indexDiff *
						           (curFontSize + SQUEEZE);
						//Account for offsets from being near an edge
						debug("edgeOffset: [" + edgeOffset + "]",3);
						yPos -= edgeOffset;
						if(yPos > -ascent / 2) {
							bgColor = drawLabelBackground(g,j,yPos - ascent);
							if(yPos >= ascent){
								labelStart = j;
								if(j != activeHoverDataIndex) {
									labelColor = labelPortColor;
								}
							}
							g2d.setColor(labelColor);
							if(j == activeHoverDataIndex) {
								debug("Drawing " +
					                  (isGeneAxis ? "ROW" : "COL") +
					                  " hover font BOLD [" + hoverStyle + "].",
					                  5);
								g2d.setFont(new Font(face,hoverStyle,size));
							} else {
								g2d.setFont(new Font(face,style,size));
							}

							int labelStrStart = xPos +
								((isGeneAxis == isRightJustified) ?
								 getLabelAreaOffset() : 0);
							if(labelStrStart < 0) {
								//This should not be necessary, but there's a
								//miscalculation somewhere and this fixes it
								/** TODO: Fix the bug that this code works
								 * around - use debug mode 13 */
								labelStrStart = 0;
							}

							debug("Printing label [" + out +
							      "] starting at labelStrStart [" +
							      labelStrStart +
							      "] (originally [" + xPos + "] before " +
							      "offset for right-just.) and length [" +
							      metrics.stringWidth(out) +
							      "], which has been offset by [" +
							      ((isGeneAxis == isRightJustified) ?
							       getLabelAreaOffset() : 0) + "]" +
							      " and is inside the offscreen pane size [" +
							      paneSizeShouldBe + "] Actual rowPaneSize: [" +
							      rowLabelPaneSize + "] colPaneSize: [" +
							      rowLabelPaneSize + "]",13);

							g2d.drawString(out,
							               labelStrStart,
							               yPos);

							drawOverrunArrows(metrics.stringWidth(out),
							                  g,
							                  yPos - ascent,
							                  curFontSize,
							                  g2d.getColor(),
							                  bgColor,
							                  labelStrStart);
						}

					}
					catch(final java.lang.ArrayIndexOutOfBoundsException e) {
						debug("There was a problem setting the font to bold",4);
						LogBuffer.logException(e);
						break;
					}
				}
				//For hovering over the left-most index, we must do this:
				g2d.setFont(new Font(face,style,size));
				for(int j = activeHoverDataIndex + 1;j <= end;j++) {
					try {
						String out = headerSummary.getSummary(headerInfo,j);
						final String[] headers = headerInfo.getHeader(j);

						if(out == null) {
							out = "No Label";
						}

						/* Set label color */
						if(drawSelection.isIndexSelected(j)) {
							if(colorIndex > 0) {
								g.setColor(TreeColorer
								           .getColor(headers[colorIndex]));
							}

							labelColor = selectionTextFGColor;

							if(colorIndex > 0) {
								g.setColor(fore);
							}

						} else {
							labelColor = Color.black;
						}

						/* Finally draw label (alignment-dependent) */
						int xPos =
							getLabelStartOffset(metrics.stringWidth(out));
						debug("Label offset 2: [" +
						      getLabelStartOffset(metrics.stringWidth(out)) +
						      "]",11);

						int indexDiff = j - activeHoverDataIndex;
						int yPos = hoverYPos + indexDiff *
						           (curFontSize + SQUEEZE);
						debug("edgeOffset: [" + edgeOffset + "]",3);
						yPos -= edgeOffset;
						if(yPos < fullPaneSize + ascent / 2) {
							bgColor = drawLabelBackground(g,j,yPos - ascent);
							if(yPos <= getPrimaryViewportSize()) {
								labelEnd = j;
								labelColor = labelPortColor;
							}
							g2d.setColor(labelColor);
							int labelStrStart = xPos +
								((isGeneAxis == isRightJustified) ?
								 getLabelAreaOffset() : 0);
							if(labelStrStart < 0) {
								//This should not be necessary, but there's a
								//miscalculation somewhere and this fixes it
								/** TODO: Fix the bug that this code works
								 * around - use debug mode 13 */
								labelStrStart = 0;
							}

							g2d.drawString(out,
							               labelStrStart,
							               yPos);

							drawOverrunArrows(metrics.stringWidth(out),
							                  g,
							                  yPos - ascent,
							                  curFontSize,
							                  labelColor,
							                  bgColor,
							                  labelStrStart);
						}

					}
					catch(final java.lang.ArrayIndexOutOfBoundsException e) {
						LogBuffer.logException(e);
						break;
					}
				}
			} else {
				for(int j = start;j <= end;j++) {

					debug("Getting data index [" + j + "]",1);

					try {
						String out = headerSummary.getSummary(headerInfo,j);
						final String[] headers = headerInfo.getHeader(j);

						if(out == null) {
							out = "No Label";
						}

						bgColor = drawLabelBackground(g,j,map.getPixel(j));

						/* Set label color */
						if((drawSelection.isIndexSelected(j) &&
							doDrawLabelPort()) ||
						   j == activeHoverDataIndex) {
							if(colorIndex > 0) {
								g.setColor(TreeColorer
								           .getColor(headers[colorIndex]));
							}

							if(j == activeHoverDataIndex) {
								g2d.setColor(hoverTextFGColor);
							} else {
								g2d.setColor(fore);
							}

							if(colorIndex > 0) {
								g.setColor(fore);
							}

						} else {
							g2d.setColor(Color.black);
						}

						/* Finally draw label (alignment-dependent) */
						int xPos =
							getLabelStartOffset(metrics.stringWidth(out));
						debug("Label offset 3: [" +
						      getLabelStartOffset(metrics.stringWidth(out)) +
						      "]",11);

						if(isGeneAxis) {
							xPos -= labelIndent;
						}
						/** TODO: Not sure why I need to not adjust for the
						 *  indent when top-justified. Move all this logic into
						 *   getLabelStartOffset */
						else if(!isRightJustified) {
							xPos += labelIndent;
						}

						if(xPos < 0) {
							xPos = 0;
						}



						//I MIGHT resurrect some or all of the preceding
						//commented code depending on feedback from the next
						//meeting (after 6/18/2015).  Other associated commented
						//code exists elsewhere too
//						int xPos = labelIndent;
//						if(isRightJustified) {
//							xPos = stringX - metrics.stringWidth(out) - labelIndent;
//						}


						g2d.drawString(out,xPos,map.getMiddlePixel(j) + ascent /
						               2);

						drawOverrunArrows(metrics.stringWidth(out),
						                  g,
						                  map.getMiddlePixel(j) - ascent / 2,
						                  curFontSize,
						                  g2d.getColor(),
						                  bgColor,
						                  xPos);
					}
					catch(final java.lang.ArrayIndexOutOfBoundsException e) {
						LogBuffer.logException(e);
						break;
					}
				}
			}
			debug((isGeneAxis ? "ROW" : "COL") + ": MaxStrLen: [" + maxStrLen +
			      "]Start Index: [" + start + "] End Index: [" + end +
			      "] height [" + offscreenSize.height + "] width [" +
			      offscreenSize.width + "]",1);

			/* Draw the "position indicator" if the label port is active */
			//See variables initialized above the string drawing section
			if(drawLabelPort && indicatorThickness > 0) {
				if(!isGeneAxis) {


					//I MIGHT resurrect some or all of the preceding
					//commented code depending on feedback from the next
					//meeting (after 6/18/2015).  Other associated commented
					//code exists elsewhere too
//					if(isRightJustified) {
//						LogBuffer.println("Resetting justification via drawing for rows!  Will draw at maxstrlen + indicatorthickness - lastendgap [" + maxStrLen + " + " + indicatorThickness + " - " + lastScrollRowEndGap + " = " + (maxStrLen + indicatorThickness - lastScrollRowEndGap) + "] - indicatorThickness [" + indicatorThickness + "] instead of current [" + secondaryScrollPos + "] - indicatorThickness [" + indicatorThickness + "]. MaxStrLen: [" + maxStrLen + "]");
//						secondaryScrollEndPos = maxStrLen + indicatorThickness - lastScrollRowEndGap;
//					} else {
//						LogBuffer.println("Resetting justification via drawing for rows!  Will draw at lastScrollRowEndPos [" + lastScrollRowEndPos + "] instead of current [" + secondaryScrollEndPos + "] - indicatorThickness [" + indicatorThickness + "]. MaxStrLen: [" + maxStrLen + "]");
//						secondaryScrollEndPos = (lastScrollRowEndPos == -1 ? scrollPane.getViewport().getSize().width : lastScrollRowEndPos);
//					}


					//Switch to the background color
					g2d.setColor(textBGColor);
					//Draw a background box to blank-out any partially scrolled
					//labels
					g.fillRect(
							lastScrollColEndGap,
							0,//start * (curFontSize + SQUEEZE) - 1,
							indicatorThickness,
							offscreenSize.width);//(end+1)*(curFontSize+SQUEEZE));
					//Switch to the label port color
					g2d.setColor(labelPortColor);


					//I MIGHT resurrect some or all of the preceding commented code
					//depending on feedback from the next meeting (after 6/18/2015)
					//Other associated commented code exists elsewhere too
//					//Draw the first border line
//					g2d.drawLine(
//							secondaryScrollEndGap + indicatorThickness -
//								(labelIndent + labelIndicatorProtrusion),
//							start * (curFontSize + SQUEEZE),
//							indicatorThickness + maxStrLen,
//							start * (curFontSize + SQUEEZE));
//					//Draw the second border line
//					g2d.drawLine(
//							secondaryScrollEndGap + indicatorThickness -
//								(labelIndent + labelIndicatorProtrusion),
//							(end + 1) * (curFontSize + SQUEEZE) - 1,
//							indicatorThickness + maxStrLen,
//							(end + 1) * (curFontSize + SQUEEZE) - 1);
//					if(labelBarThickness > 0) {
//    					//Draw the label breadth bar
//    					g.fillRect(
//    							lastScrollColEndGap + matrixBarThickness,
//    							start * (curFontSize + SQUEEZE),
//    							labelBarThickness,
//    							(end + 1) * (curFontSize + SQUEEZE) - start * (curFontSize + SQUEEZE));
//					}


					if(matrixBarThickness > 0) {
						debug("Drawing col matrix bar from index start [" +
						      labelStart + "] to stop [" + labelEnd +
						      "], pixel start [" + map.getPixel(labelStart) +
						      "] to stop [" + (map.getPixel(labelEnd + 1)) +
						      "]",4);

						//Draw the matrix breadth bar
						g.fillRect(
								lastScrollColEndGap,
								/* start * (curFontSize + SQUEEZE) + */
								map.getPixel(labelStart),
								matrixBarThickness,
								map.getPixel(labelEnd + 1) -
								map.getPixel(labelStart));
					}


					//I MIGHT resurrect some or all of the preceding
					//commented code depending on feedback from the next
					//meeting (after 6/18/2015).  Other associated commented
					//code exists elsewhere too
//					//If there is a data index that is hovered over
//					int activeHoverDataIndex =
//							(hoverIndex == -1 ? matrixHoverIndex : hoverIndex);


					if(activeHoverDataIndex > -1) {
						//Change to the hover color
						g2d.setColor(hoverTextFGColor);
						//Draw the hover matrix position indicator
						int matrixPixel = /* start * (curFontSize + SQUEEZE) +*/
							map.getPixel(activeHoverDataIndex);


						//I MIGHT resurrect some or all of the preceding
						//commented code depending on feedback from the next
						//meeting (after 6/18/2015).  Other associated commented
						//code exists elsewhere too
//						LogBuffer.println("Hover matrix start: [" + start + "] startpixel: [" + start * (curFontSize + SQUEEZE) + "] activeHoverIndex: [" + activeHoverDataIndex + "] data cell pixel [" + map.getPixel(activeHoverDataIndex) + "] matrixPixel: [" + matrixPixel + "]");
//						int labelPixel = activeHoverDataIndex * (curFontSize + SQUEEZE);
//						boolean outOfBounds = activeHoverDataIndex > end || activeHoverDataIndex < start;
//						boolean drawAMatrixPoint = ((int) Math.round(map.getScale()) > 2);// && !outOfBounds && matrixBarThickness > 4);
//						if(drawAMatrixPoint) {
//							debug("Drawing point on matrix hover indicator",4);
//							//Draw the base of the indicator
//							g.fillRect(
//									lastScrollColEndGap + matrixIndicatorIntrusion,
//									matrixPixel,
//									matrixBarThickness - matrixIndicatorIntrusion + 1,
//									(int) Math.round(map.getScale()));
//							//Draw the point of the indicator
//							//Do a 4 point polygon if there's an even number of pixels in order to have a stubby but symmetrical point
//							int baseWidth = (int) Math.floor(map.getScale());
//							int center = matrixPixel + (int) Math.ceil((int) Math.floor(map.getScale()) / 2);
//							if(baseWidth % 2 == 1) {
//								int[] exes = {lastScrollColEndGap + matrixIndicatorIntrusion,lastScrollColEndGap + matrixIndicatorIntrusion,lastScrollColEndGap};
//								int[] whys = {matrixPixel,matrixPixel + (int) Math.ceil(map.getScale()) - 1,center};
//								//g2d.setColor(Color.green);
//								g.fillPolygon(exes, whys, 3);
//							} else {
//								int[] exes = {lastScrollColEndGap + matrixIndicatorIntrusion,lastScrollColEndGap + matrixIndicatorIntrusion,lastScrollColEndGap,lastScrollColEndGap};
//								int[] whys = {matrixPixel,matrixPixel + (int) Math.ceil(map.getScale()) - 1,center - 1,center};
//								//g2d.setColor(Color.black);
//								g.fillPolygon(exes, whys, 4);
//							}
//							//g2d.setColor(hoverTextFGColor);
//						} else {


						int indwidth = map.getPixel(matrixPixel + 1) -
								map.getPixel(matrixPixel);
							if(indwidth < 1) {
								indwidth = 1;
							}
							debug("Drawing position indicator 1",14);
							g.fillRect(
									lastScrollColEndGap,
									matrixPixel,
									/* (outOfBounds ?
									(int)Math.ceil(matrixBarThickness/2)+1:*/
									matrixBarThickness + 1 /* ) */,
									indwidth);


							//I MIGHT resurrect some or all of the preceding
							//commented code depending on feedback from the next
							//meeting (after 6/18/2015).  Other associated
							//commented code exists elsewhere too
									//(int) Math.round(map.getScale()));
//						}
//						if(!outOfBounds) {
//							//Draw the hover label position indicator
////							g.fillRect(
////									secondaryScrollEndGap + matrixBarThickness + (int) Math.floor(labelBarThickness / 2),
////									labelPixel,
////									(int) Math.ceil(labelBarThickness / 2) + labelIndicatorProtrusion + 1,
////									curFontSize + SQUEEZE);
//							if(labelBarThickness > 0) {
//    							//Draw the base of the indicator
//    							g.fillRect(
//    									lastScrollColEndGap + matrixBarThickness,
//    									labelPixel,
//    									labelBarThickness + 1,
//    									curFontSize + SQUEEZE);
//							}
//							if(labelIndicatorProtrusion > 0) {
//    							//Draw the point of the indicator
//    							//Do a 4 point polygon if there's an even number of pixels in order to have a stubby but symmetrical point
//    							if((curFontSize + SQUEEZE) % 2 == 1) {
//    								//g2d.setColor(Color.gray);
//    								int[] exes = {lastScrollColEndGap + matrixBarThickness + labelBarThickness,lastScrollColEndGap + matrixBarThickness + labelBarThickness,lastScrollColEndGap + matrixBarThickness + labelBarThickness + labelIndicatorProtrusion + 1};
//    								int[] whys = {labelPixel - 1,labelPixel + curFontSize + SQUEEZE,labelPixel + (int) Math.ceil((curFontSize + SQUEEZE) / 2)};
//    								g.fillPolygon(exes, whys, 3);
//    							} else {
//    								//g2d.setColor(Color.green);
//    								int[] exes = {lastScrollColEndGap + matrixBarThickness + labelBarThickness,lastScrollColEndGap + matrixBarThickness + labelBarThickness,lastScrollColEndGap + matrixBarThickness + labelBarThickness + labelIndicatorProtrusion,lastScrollColEndGap + matrixBarThickness + labelBarThickness + labelIndicatorProtrusion};
//    								int[] whys = {labelPixel - 1,labelPixel + curFontSize + SQUEEZE,labelPixel + (int) (curFontSize + SQUEEZE) / 2 - 1,labelPixel + (int) (curFontSize + SQUEEZE) / 2};
//    								g.fillPolygon(exes, whys, 4);
//    							}
//							}
//							//g2d.setColor(hoverTextFGColor);
//						}
//						//Draw the connection btwn hvr label & matrix pos indicators
//						g2d.drawLine(
//								lastScrollColEndGap + (outOfBounds ? (int) Math.floor(matrixBarThickness / 2) : matrixBarThickness) + 1,
//								matrixPixel,
//								lastScrollColEndGap + (outOfBounds ? (int) Math.floor(matrixBarThickness / 2) : matrixBarThickness) + 1,
//								(outOfBounds ? (activeHoverDataIndex > end ? (end + 1) * (curFontSize + SQUEEZE) - 1 : 0) : labelPixel));
//						if(activeHoverDataIndex > end) {
//							//Draw an arrow off the right side
//							g2d.drawLine(secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2),
//										 (end + 1) * (curFontSize + SQUEEZE) - 2,
//										 secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2) + 2,
//										 (end + 1) * (curFontSize + SQUEEZE) - 2);
//							g2d.drawLine(secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2) - 1,
//									 (end + 1) * (curFontSize + SQUEEZE) - 3,
//									 secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2) + 3,
//									 (end + 1) * (curFontSize + SQUEEZE) - 3);
//						} else if(activeHoverDataIndex < start) {
//							//Draw an arrow off the left side
//							g2d.drawLine(secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2),
//										 start * (curFontSize + SQUEEZE) + 1,
//										 secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2) + 2,
//										 start * (curFontSize + SQUEEZE) + 1);
//							g2d.drawLine(secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2) - 1,
//									 start * (curFontSize + SQUEEZE) + 2,
//									 secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2) + 3,
//									 start * (curFontSize + SQUEEZE) + 2);
//						}


					}
				} else {


					//I MIGHT resurrect some or all of the preceding
					//commented code depending on feedback from the next
					//meeting (after 6/18/2015).  Other associated
					//commented code exists elsewhere too
					//int secondaryScrollEndPos = secondaryScrollPos + getSecondaryScrollBar().getModel().getExtent();
//					if(isRightJustified) {
//						lastScrollRowEndPos = maxStrLen + indicatorThickness - lastScrollRowEndGap;
//					} else {
//						lastScrollRowEndPos = (lastScrollRowEndPos == -1 ? scrollPane.getViewport().getSize().width : lastScrollRowEndPos);
//					}


					//Switch to the background color
					g2d.setColor(textBGColor);


					//I MIGHT resurrect some or all of the preceding
					//commented code depending on feedback from the next
					//meeting (after 6/18/2015).  Other associated
					//commented code exists elsewhere too
					//resetJustification = (secondaryScrollPos == 0 && secondaryScrollEndGap == 0);


					//Draw a background box to blank-out any partially scrolled labels
					g.fillRect(
							lastScrollRowEndPos - indicatorThickness,
							0,//start * (curFontSize + SQUEEZE) - 1,
							indicatorThickness,
							offscreenSize.height);//(end+1)*(curFontSize+SQUEEZE));
					//Switch to the label port color
					g2d.setColor(labelPortColor);


					//I MIGHT resurrect some or all of the preceding
					//commented code depending on feedback from the next
					//meeting (after 6/18/2015).  Other associated
					//commented code exists elsewhere too
//					//Draw the first border line
//					g2d.drawLine(
//							secondaryScrollEndPos - indicatorThickness +
//								(labelIndent + labelIndicatorProtrusion),
//							start * (curFontSize + SQUEEZE),
//							0,
//							start * (curFontSize + SQUEEZE));
//					//Draw the second border line
//					g2d.drawLine(
//							secondaryScrollEndPos - indicatorThickness +
//								(labelIndent + labelIndicatorProtrusion),
//							(end + 1) * (curFontSize + SQUEEZE) - 1,
//							0,
//							(end + 1) * (curFontSize + SQUEEZE) - 1);
//					//Draw the label breadth bar
//					g.fillRect(
//							secondaryScrollEndPos - matrixBarThickness - labelBarThickness,
//							start * (curFontSize + SQUEEZE),
//							labelBarThickness,
//							(end + 1) * (curFontSize + SQUEEZE) - start * (curFontSize + SQUEEZE));


					if(matrixBarThickness > 0) {
						//Draw the matrix breadth bar
						debug("Drawing row matrix bar from index start [" +
						      labelStart + "] to stop [" + labelEnd +
						      "], pixel start [" + map.getPixel(labelStart) +
						      "] to stop [" + (map.getPixel(labelEnd + 1)) +
						      "]",4);

						g.fillRect(
								lastScrollRowEndPos - matrixBarThickness,
								/* start * (curFontSize + SQUEEZE) + */
								map.getPixel(labelStart),
								matrixBarThickness,
								map.getPixel(labelEnd + 1) -
								map.getPixel(labelStart));
					}


					//I MIGHT resurrect some or all of the preceding
					//commented code depending on feedback from the next
					//meeting (after 6/18/2015).  Other associated
					//commented code exists elsewhere too
//					//If there is a data index that is hovered over
//					int activeHoverDataIndex =
//							(hoverIndex == -1 ? matrixHoverIndex : hoverIndex);


					if(activeHoverDataIndex > -1) {
						//Change to the hover color
						g2d.setColor(hoverTextFGColor);
						//Draw the hover matrix position indicator
						int matrixPixel = /* start * (curFontSize + SQUEEZE) +*/
							map.getPixel(activeHoverDataIndex);


						//I MIGHT resurrect some or all of the preceding
						//commented code depending on feedback from the next
						//meeting (after 6/18/2015).  Other associated
						//commented code exists elsewhere too
//						int labelPixel = activeHoverDataIndex * (curFontSize + SQUEEZE);
//						boolean outOfBounds = activeHoverDataIndex > end || activeHoverDataIndex < start;


						int indwidth = map.getPixel(matrixPixel + 1) -
							map.getPixel(matrixPixel);
						if(indwidth < 1) {
							indwidth = 1;
						}
						debug("Drawing position indicator",14);
						g.fillRect(
								lastScrollRowEndPos - /* (outOfBounds ?
									(int) Math.ceil(matrixBarThickness / 2) : */
								matrixBarThickness /* ) */ - 1,
								matrixPixel,
								/* (outOfBounds ?
									(int) Math.ceil(matrixBarThickness / 2) : */
								matrixBarThickness /* ) */ + 1,
								indwidth);
								//(int) Math.round(map.getScale()));


						//I MIGHT resurrect some or all of the preceding
						//commented code depending on feedback from the next
						//meeting (after 6/18/2015).  Other associated
						//commented code exists elsewhere too
//						if(!outOfBounds) {
//							//Draw the hover label position indicator
//							g.fillRect(
//									lastScrollRowEndPos - indicatorThickness + labelIndent,
//									//secondaryScrollPos + matrixBarThickness + (int) Math.floor(labelBarThickness / 2),
//									labelPixel,
//									labelBarThickness + labelIndicatorProtrusion,
//									curFontSize + SQUEEZE);
//						}
//						//Draw the connection btwn hvr label & matrix pos indicators
//						g2d.drawLine(
//								secondaryScrollEndPos - (outOfBounds ? (int) Math.floor(matrixBarThickness / 2) : matrixBarThickness) - 1,
//								matrixPixel,
//								secondaryScrollEndPos - (outOfBounds ? (int) Math.floor(matrixBarThickness / 2) : matrixBarThickness) - 1,
//								(outOfBounds ? (activeHoverDataIndex > end ? (end + 1) * (curFontSize + SQUEEZE) - 1 : 0) : labelPixel));
//						if(activeHoverDataIndex > end) {
//							//Draw an arrow off the right side
//							g2d.drawLine(secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2),
//										 (end + 1) * (curFontSize + SQUEEZE) - 2,
//										 secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2) - 2,
//										 (end + 1) * (curFontSize + SQUEEZE) - 2);
//							g2d.drawLine(secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2) + 1,
//									 (end + 1) * (curFontSize + SQUEEZE) - 3,
//									 secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2) - 3,
//									 (end + 1) * (curFontSize + SQUEEZE) - 3);
//						} else if(activeHoverDataIndex < start) {
//							//Draw an arrow off the left side
//							g2d.drawLine(secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2),
//										 start * (curFontSize + SQUEEZE) + 1,
//										 secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2) - 2,
//										 start * (curFontSize + SQUEEZE) + 1);
//							g2d.drawLine(secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2) + 1,
//									 start * (curFontSize + SQUEEZE) + 2,
//									 secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2) - 3,
//									 start * (curFontSize + SQUEEZE) + 2);
//						}
					}
				}
			} else {
				//Switch to the background color
				g2d.setColor(textBGColor);
				//Draw a background box to blank-out any partially scrolled
				//labels
				if(isGeneAxis) {
					debug("Blanking out the indent at position [" +
					      (lastScrollRowEndPos - labelIndent) + "]",15);
					g.fillRect(
							lastScrollRowEndPos - labelIndent,
							0,
							labelIndent,
							offscreenSize.height);
				} else {
					debug("Blanking out the indent at position [" +
					      lastScrollColEndGap + "]",15);
					g.fillRect(
							lastScrollColEndGap,
							0,
							labelIndent,
							offscreenSize.width);
				}
			}

			g2d.setTransform(orig);

			//Set the size of the scrollpane to match the longest string
			if(isGeneAxis) {
				if(offscreenSize.height != fullPaneSize ||
				   offscreenSize.width  != maxStrLen) {
					setPreferredSize(new Dimension(maxStrLen +
					                                (drawLabelPort ?
					                                 indicatorThickness :
					                                 labelIndent),
					                               fullPaneSize));
				}
				debug("Resizing row labels panel to [" + maxStrLen + "x" +
				      fullPaneSize + "].",2);
			} else {
				if(offscreenSize.height != maxStrLen ||
				   offscreenSize.width  != fullPaneSize) {
					debug("Setting col pane height to [" +
					      (maxStrLen +
					       (drawLabelPort ? indicatorThickness : labelIndent)) +
					      "]",6);
					setPreferredSize(new Dimension(fullPaneSize,
					                               maxStrLen +
					                               (drawLabelPort ?
					                                indicatorThickness :
					                                labelIndent)
					                               ));
				}
				debug("Resizing col labels panel to [" + fullPaneSize + "x" +
				      maxStrLen + "].",1);
			}

			/*
			 * Scroll to the position that is equivalent to the previous
			 * position
			 */
			secondaryReScroll(maxStrLen +
			                  (drawLabelPort ?
			                   indicatorThickness : labelIndent));
		} else {
			debug("Label port NOT drawn",2);
			//Set a temporary font size - we don't need to save this size in the
			//prefs file. Besides, this font size would be saved as a new font
			//size and the scrollbars would be re-justified and not remember
			//their position
			g.setFont(new Font(face,style,HINTFONTSIZE));
			g2d.setColor(Color.black);

			int xPos;
			int yPos;
			final FontMetrics metrics = getFontMetrics(g2d.getFont());

			//Reduce the size of the scrollpane to just what is visible
			setPreferredSize(new Dimension(scrollPane.getViewport()
			                                         .getSize().width,
			                               scrollPane.getViewport()
			                                         .getSize().height));

			if(isGeneAxis) {
				//After rotate, the current width will be the "height" and the
				//zero point we need to calculate from is the bottom left of the
				//panel
				yPos = scrollPane.getViewport().getSize().width / 2;
				//After rotate, this is the distance up from the bottom where to
				//start the hint string
				xPos = (offscreenSize.height -
				        metrics.stringWidth(zoomHint)) / 2;
				g2d.rotate(Math.PI * 3 / 2);
				//Need to translate the coordinates to/from? the rotated state
				g2d.translate(-offscreenSize.height,0);

				debug("Row Hint position: [" + xPos + "/" + yPos +
				      "] zoomHint: [" + zoomHint +
				      "] height [" + offscreenSize.height + "] width [" +
				      offscreenSize.width + "] + HintStrLen [" +
				      metrics.stringWidth(zoomHint) + "]",3);
			} else {
				xPos = (offscreenSize.width -
				        metrics.stringWidth(zoomHint)) / 2;
				yPos = scrollPane.getViewport().getSize().height / 2;

				debug("Col Hint position: [" + xPos + "/" + yPos +
				      "] zoomHint: [" + zoomHint +
				      "] height [" + offscreenSize.height +
				      "] width [" + offscreenSize.width +
				      "] + HintStrLen [" + metrics.stringWidth(zoomHint) + "]",
				      3);
			}

			//Adding this useless scroll causes the pane size and scrollbar
			//attributes to update correctly - and reflect the settings made
			//above.  Without it, the scrollbar still implies that the panel is
			//its previous size and the hint is off center. However, there is an
			//unfortunate side-effect: the scroll position of the labels upon
			//hover after this line has executed is briefly set to 0 instead of
			//the rembered value
			int secondaryLength =
				(isGeneAxis ? scrollPane.getViewport().getSize().width :
				 scrollPane.getViewport().getSize().height);
			getSecondaryScrollBar().setValues(0,
			                                  secondaryLength,
			                                  0,
			                                  secondaryLength);

			g2d.drawString(zoomHint,xPos,yPos);
		}
	}

	/**
	 * This method draws the backgrounds of selected and "selecting" (those who
	 * have been dragged over) labels.  It takes into account whether the label
	 * is currently selected and whether the drag of the mouse is selecting
	 * anew, selecting additional, toggling, or deselecting.  "Selecting anew"
	 * leaves the pre-existing highlights in place even though those not dragged
	 * over will be resultingly deselected. This is on purpose to give the user
	 * a visual context during dragging.  Note this method is not intended to
	 * draw the default background in any circumstance.
	 * @param g - Graphics object
	 * @param j - Data index of the label whose background to draw
	 * @param yPos - Pixel index of where the label is relative to the viewport
	 * @return
	 */
	public Color drawLabelBackground(final Graphics g,int j,int yPos) {
		
		if(j > headerInfo.getNumHeaders() - 1) {
			return Color.black;
		}
		
		final int bgColorIndex = headerInfo.getIndex("BGCOLOR");
		final String[] strings = headerInfo.getHeader(j);
		Color bgColor = textBGColor;
		boolean isSelecting =
			(map.isSelecting() &&
		     ((j >= map.getSelectingStart() && j <= map.getHoverIndex()) ||
		      (j <= map.getSelectingStart() && j >= map.getHoverIndex())));
		boolean drawingSelectColor = false;
		try {
			if((drawSelection.isIndexSelected(j) && !isSelecting) ||
				(isSelecting &&
				 ((!map.isDeSelecting() && !map.isToggling()) ||
				  (map.isToggling() && !drawSelection.isIndexSelected(j))))) {

				debug("Drawing yellow background for selected index [" + j +
				      "] because isSelecting is [" +
				      (isSelecting ? "true" : "false") +
				      "] isDeSelecting is [" +
				      (map.isDeSelecting() ? "true" : "false") +
				      "] and isToggling is [" +
				      (map.isToggling() ? "true" : "false") + "]",7);

				g.setColor(selectionTextBGColor);
				bgColor = selectionTextBGColor;
				drawingSelectColor = true;
			} else if(bgColorIndex > 0) {
				debug("Background color index is [" + bgColorIndex + "]",8);
				g.setColor(TreeColorer.getColor(strings[bgColorIndex]));
				bgColor = TreeColorer.getColor(strings[bgColorIndex]);
			}
		}
		catch(final Exception e) {
			// ignore...
		}

		if(drawingSelectColor) {
			g.fillRect(0,
			           yPos,
			           (isGeneAxis ? rowLabelPaneSize : colLabelPaneSize),
			           (doDrawLabelPort() ?
			            size + SQUEEZE :
			            map.getPixel(j + 1) - map.getPixel(j)));

			debug("Drawing select color from x|y [0|" + yPos +
			      "] to x|y (LabelPaneSize|(doDrawLabelPort() ? size + " +
			      "SQUEEZE : map.getPixel(j + 1) - map.getPixel(j))) [" +
			      (isGeneAxis ? rowLabelPaneSize : colLabelPaneSize) + "|" +
			      (doDrawLabelPort() ? size + SQUEEZE :
			       map.getPixel(j + 1) - map.getPixel(j)) + "]",17);
		}

		return(bgColor);
	}

	/**
	 * This method ensures that the secondary label pane scrollbar is positioned
	 * to where it was before the labels were hidden behind the zoom/hover/font-
	 * size "hint". It performs the scroll based on 6 saved data members
	 * [lastScrollRowPos, lastScrollRowEndPos, lastScrollRowEndGap,
	 * lastScrollColPos, lastScrollColEndPos, and lastScrollColEndGap].  If any
	 * of those data members are not set (i.e. have a value of -1), it
	 * initializes them based on the saved justification.
	 * @param secondaryPaneSize
	 */
	private void secondaryReScroll(int secondaryPaneSize) {
		if((isGeneAxis  && (map.areRowLabelsBeingScrolled())) ||
		   (!isGeneAxis && (map.areColLabelsBeingScrolled()))) {
			debug("Not rescrolling because label knob is being dragged.",6);
			return;
		}

		int curPos    = getSecondaryScrollBar().getValue();
		int curExtent = getSecondaryScrollBar().getModel().getExtent();
		int curMin    = getSecondaryScrollBar().getMinimum();
		int curMax    = getSecondaryScrollBar().getMaximum();

		/*
		 * Scroll to the position that is equivalent to the previous
		 * position
		 */
		if(isRightJustified) {
			if(isGeneAxis) {
				//See if we need to initialize the scroll positions
				if(lastScrollRowPos == -1) {
					debug("Scrolling to [" +
					      (getSecondaryScrollBar().getMaximum() -
					       getSecondaryScrollBar().getModel().getExtent()) +
					      "] max - extent [" +
					       getSecondaryScrollBar().getMaximum() + " - " +
					       getSecondaryScrollBar().getModel().getExtent() +
					       "] after drawing - first time rows right " +
					       "justified",6);

					//It seems that the scrollbar max and extent are not
					//updated in this scenario when the app first opens a
					//file, so we will calculate the initial scroll position
					//thusly
					int tmpscrollpos =
						secondaryPaneSize -
					    scrollPane.getViewport().getSize().width;

					lastScrollRowPos    = tmpscrollpos;
					lastScrollRowEndPos = secondaryPaneSize;
					lastScrollRowEndGap = 0;
				} else {
					debug("Scrolling to [" + lastScrollRowPos +
					      "] after drawing - rememberred rows right " +
					      "justified",6);
				}

				//Now perform the scroll...
				//Only change the other values if something about them has
				//changed (because it triggers an updateBuffer call and
				//updateBuffer calls this method, which would create an
				//endless loop).  Setting just the scroll value does not
				//call updateBuffer
				if(curExtent != (lastScrollRowEndPos - lastScrollRowPos) ||
				   curMin != 0 || curMax != secondaryPaneSize) {
					getSecondaryScrollBar().setValues(lastScrollRowPos,
					                                  lastScrollRowEndPos -
					                                  lastScrollRowPos,
					                                  0,
					                                  secondaryPaneSize);

					debug("ReScroll rows values, pane size: [" +
					      secondaryPaneSize + "] pos [" + lastScrollRowPos +
					      "] extent [" +
					      (lastScrollRowEndPos - lastScrollRowPos) +
					      "] min [" + "0" + "] max [" +
					      (secondaryPaneSize -
					       (lastScrollRowEndPos - lastScrollRowPos)) + "]",6);
				} else if(curPos != lastScrollRowPos) {
					getSecondaryScrollBar().setValue(lastScrollRowPos);

					debug("ReScroll rows to pos, pane size: [" +
					      secondaryPaneSize + "] pos [" + lastScrollRowPos +
					      "] extent [" +
					      (lastScrollRowEndPos - lastScrollRowPos) +
					      "] min [0] max [" +
					      (secondaryPaneSize -
					       (lastScrollRowEndPos - lastScrollRowPos)) + "]",6);
				} else {
					debug("Already there",6);
				}
			} else {
				//See if we need to initialize the scroll positions
				if(lastScrollColPos == -1) {
					debug("Scrolling to [0] after drawing - first time " +
					      "cols right justified",6);

					//It seems that the scrollbar max and extent are not
					//updated in this scenario when the app first opens a
					//file, so we will calculate the initial scroll position
					//thusly
					lastScrollColPos = 0;
					lastScrollColEndPos =
						scrollPane.getViewport().getSize().height;
					lastScrollColEndGap = secondaryPaneSize -
						scrollPane.getViewport().getSize().height;
				} else {
					debug("Scrolling to [" + lastScrollColPos +
					      "] after drawing - rememberred " +
					      "cols right justified",6);
				}

				//Only change the other values if something about them has
				//changed (because it triggers an updateBuffer call and
				//updateBuffer calls this method, which would create an
				//endless loop).  Setting just the scroll value does not
				//call updateBuffer
				if(curExtent != (lastScrollColEndPos - lastScrollColPos) ||
				   curMin != 0 || curMax != secondaryPaneSize) {
					getSecondaryScrollBar().setValues(lastScrollColPos,
					                                  lastScrollColEndPos -
					                                  lastScrollColPos,
					                                  0,
					                                  secondaryPaneSize);

					debug("ReScroll col right just. values, pane size: [" +
					      secondaryPaneSize + "] pos [" + lastScrollColPos +
					      "] extent [" +
					      (lastScrollColEndPos - lastScrollColPos) +
					      "] min [0] max [" + secondaryPaneSize + "]",6);
				} else if(curPos != lastScrollColPos) {
					getSecondaryScrollBar().setValue(lastScrollColPos);

					debug("ReScroll col right just. to pos, pane size: [" +
					      secondaryPaneSize + "] pos [" + lastScrollColPos +
					      "] extent [" +
					      (lastScrollColEndPos - lastScrollColPos) +
					      "] min [0] max [" + secondaryPaneSize + "]",6);
				} else {
					debug("Already there",6);
				}
			}
		} else {
			if(isGeneAxis) {
				//See if we need to initialize the scroll positions
				if(lastScrollRowPos == -1) {
					debug("Scrolling to [0] after drawing - first time " +
					      "rows left justified",6);

					//It seems that the scrollbar max and extent are not
					//updated in this scenario when the app first opens a
					//file, so we will calculate the initial scroll position
					//thusly
					lastScrollRowPos = 0;
					lastScrollRowEndPos =
						scrollPane.getViewport().getSize().width;
					lastScrollRowEndGap = secondaryPaneSize -
						scrollPane.getViewport().getSize().width;
				} else {
					debug("Scrolling to [" + lastScrollRowPos +
					      "] after drawing - rememberred " +
					      "rows left justified",6);
				}

				//Only change the other values if something about them has
				//changed (because it triggers an updateBuffer call and
				//updateBuffer calls this method, which would create an
				//endless loop).  Setting just the scroll value does not
				//call updateBuffer
				if(curExtent != (lastScrollRowEndPos - lastScrollRowPos) ||
				   curMin != 0 || curMax != secondaryPaneSize) {
					getSecondaryScrollBar().setValues(lastScrollRowPos,
					                                  lastScrollRowEndPos -
					                                  lastScrollRowPos,
					                                  0,
					                                  secondaryPaneSize);

					debug("ReScroll rows left just. values, pane size: [" +
					      secondaryPaneSize + "] pos [" + lastScrollRowPos +
					      "] extent [" +
					      (lastScrollRowEndPos - lastScrollRowPos) +
					      "] min [0] max [" +
					      (secondaryPaneSize -
					       (lastScrollRowEndPos - lastScrollRowPos)) + "]",6);
				} else if(curPos != lastScrollRowPos) {
					getSecondaryScrollBar().setValue(lastScrollRowPos);

					debug("ReScroll rows left just. to pos, pane size: [" +
					      secondaryPaneSize + "] pos [" + lastScrollRowPos +
					      "] extent [" +
					      (lastScrollRowEndPos - lastScrollRowPos) +
					      "] min [0] max [" +
					      (secondaryPaneSize -
					       (lastScrollRowEndPos - lastScrollRowPos)) + "]",6);
				} else {
					debug("Already there",6);
				}
			} else {
				//See if we need to initialize the scroll positions
				if(lastScrollColPos == -1) {
					//It seems that the scrollbar max and extent are not
					//updated in this scenario when the app first opens a
					//file, so we will calculate the initial scroll
					//position thusly
					int tmpscrollpos = secondaryPaneSize -
						scrollPane.getViewport().getSize().height;

					lastScrollColPos    = tmpscrollpos;
					lastScrollColEndPos = secondaryPaneSize;
					lastScrollColEndGap = 0;

					debug("Set lastScrollColPos [" +
						lastScrollColPos +
					      "] lastScrollColEndPos [" +
					      lastScrollColEndPos + "] lastScrollColEndGap [" +
					      lastScrollColEndGap +
					      "] after drawing - first time " +
					      "cols left justified",6);
				} else {
					debug("Scrolling to [" + lastScrollColPos +
					      "] after drawing - rememberred " +
					      "cols left justified",6);
				}

				//Only change the other values if something about them has
				//changed (because it triggers an updateBuffer call and
				//updateBuffer calls this method, which would create an
				//endless loop).  Setting just the scroll value does not
				//call updateBuffer
				if(curExtent != (lastScrollColEndPos - lastScrollColPos) ||
				   curMin != 0 || curMax != secondaryPaneSize) {
					getSecondaryScrollBar().setValues(lastScrollColPos,
					                                  lastScrollColEndPos -
					                                  lastScrollColPos,
					                                  0,
					                                  secondaryPaneSize);

					debug("ReScroll col left just. values, pane size: [" +
					      secondaryPaneSize + "] pos [" + lastScrollColPos +
					      "] extent [" +
					      (lastScrollColEndPos - lastScrollColPos) +
					      "] min [0] max [" + secondaryPaneSize + "]",6);
				} else if(curPos != lastScrollColPos) {
					getSecondaryScrollBar().setValue(lastScrollColPos);

					debug("ReScroll col left just. to pos, pane size: [" +
					      secondaryPaneSize + "] pos [" + lastScrollColPos +
					      "] extent [" +
					      (lastScrollColEndPos - lastScrollColPos) +
					      "] min [0] max [" + secondaryPaneSize + "]",6);
				} else {
					debug("Already there",6);
				}
			}
		}
	}

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
	 * @param pos - scroll position
	 * @param endPos - scroll position + extent
	 * @param endGap - max scroll (i.e. offscreen pane size) - endPos
	 */
	protected abstract void explicitSecondaryScrollTo(int pos,
	                                                  int endPos,
	                                                  int endGap);

	/**
	 * Returns either the number of pixels in length the longest string is, or
	 * the portion of the panel the strings will be drawn in (not including the
	 * indicator thickness or indent) - whichever is larger.
	 * @param metrics - font details
	 */
	public int getLabelAreaSize(FontMetrics metrics) {
		int min =
			(isGeneAxis ? scrollPane.getViewport().getSize().width :
						  scrollPane.getViewport().getSize().height)
					- (doDrawLabelPort() ? indicatorThickness : labelIndent);
		int len = getMaxStringLength(metrics);
		return len > min ? len : min;
	}

	/**
	 * This returns the vertical size of the column label pane or horizontal
	 * size of the row label pane, including the space allotted for indent
	 * and/or the indicator bar.  It calculates the size based on the longest
	 * label length using the current font settings (metrics param) via a call
	 * to getMaxStringLength. If the viewport size is larger than the longest
	 * label plus indent/indicator, it returns the viewport size.
	 * @param metrics
	 * @return
	 */
	public int getLabelPaneContentSize(FontMetrics metrics) {
		int min =
			(isGeneAxis ? scrollPane.getViewport().getSize().width :
						  scrollPane.getViewport().getSize().height);
		int len = getMaxStringLength(metrics)
			+ (doDrawLabelPort() ? indicatorThickness : labelIndent);
		return len > min ? len : min;
	}

	/**
	 * This function returns the length in pixels of the longest label and
	 * tracks the font settings using data members [lastDrawnFace versus current
	 * 'face', lastDrawnStyle versus current 'style', lastDrawnSize versus
	 * current 'size', longest_str_length, and longest_str_index]. If nothing
	 * about the font has changed, it returns longest_str_length. If the face or
	 * style have changed, it re-measures the string at longest_str_index.
	 * Otherwise, it searches all the strings again to get the longest.
	 * @param FontMetrics metrics
	 * @return maxStrLen
	 */
	public int getMaxStringLength(FontMetrics metrics) {
		int end       = map.getMaxIndex();
		int maxStrLen = 0;
		String maxStr = "";

		/* TODO: Check whether any of the string data has changed if the
		 * user changes labels OR when such a change is made, set the
		 * lastDrawn* etc. variables to initial values again. */

		//If nothing about the font has changed, calculate the length of the
		//longest string
		if(lastDrawnFace == face  && lastDrawnStyle == style &&
		   longest_str_index > -1 && lastDrawnSize  == size &&
			longest_str.equals(headerSummary.getSummary(headerInfo,
			                                            longest_str_index))) {
			debug("Regurgitating maxstrlen",1);
			maxStr = headerSummary.getSummary(headerInfo,longest_str_index);
			maxStrLen = longest_str_length;
			debug("Everything fontwise is the same, including size [" + size +
			      "]. returning saved maxStrLen [" + maxStrLen + "]",12);
		}
		//Else if the font size only has changed, recalculate the longest
		//string's length
		else if(lastDrawnFace == face && lastDrawnStyle == style &&
		        longest_str_index > -1 &&
		        lastDrawnSize != size &&
		        longest_str.equals(headerSummary.getSummary(headerInfo,
		                                                    longest_str_index))
		       ) {
			debug("Refining maxstrlen",1);
			maxStr = headerSummary.getSummary(headerInfo,longest_str_index);
			maxStrLen = metrics.stringWidth(maxStr);
			debug("Font size only changed. Recalculating length of longest " +
			      "string [" + maxStr + "] & returning maxStrLen [" +
			      maxStrLen + "]",12);
		}
		//Else find the longest string and return its length
		else {
			debug("Calculating maxstrlen because not [lastDrawnFace == face " +
			      "&& lastDrawnStyle == style && longest_str_index > -1 && " +
			      "lastDrawnSize != size && longest_str.equals(headerSummary." +
			      "getSummary(headerInfo,longest_str_index))]",1);
			debug("Calculating maxstrlen because not [" + lastDrawnFace +
			      " == " + face + " && " + lastDrawnStyle + " == " + style +
			      " && " + longest_str_index + " > -1 && " + lastDrawnSize +
			      " != " + size + " && " + longest_str +
			      ".equals(headerSummary.getSummary(headerInfo," +
			      longest_str_index + "))]",1);
			for(int j = 0;j <= end;j++) {
				try {
					String out = headerSummary.getSummary(headerInfo,j);

					if(out == null) {
						out = "No Label";
					}

					if(maxStrLen < metrics.stringWidth(out)) {
						maxStrLen = metrics.stringWidth(out);
						longest_str_index = j;
						if(debug != 0) {
							maxStr = out;
						}
					}

				} catch(final java.lang.ArrayIndexOutOfBoundsException e) {
					LogBuffer.logException(e);
					break;
				}
			}
			longest_str = maxStr;

			debug("Full-on recalculating [" + maxStr + "] maxStrLen [" +
			      maxStrLen + "] at face [" + face + "] style [" + style +
			      "] size [" + size + "]",12);
		}

		//Save the state to detect changes upon the next call of this method
		lastDrawnFace      = face;
		lastDrawnStyle     = style;
		lastDrawnSize      = size;
		longest_str_length = maxStrLen;

		debug((isGeneAxis ? "ROW" : "COL") + ": MaxStrLen: [" + maxStrLen +
		      "] MaxStr: [" + maxStr + "] Start Index: [" + 0 +
		      "] End Index: [" + end + "] height [" + offscreenSize.height +
		      "] width [" + offscreenSize.width + "]",1);

		return maxStrLen;
	}

	/**
	 * Sets a dynamic font size based on current scale of the dependent axis
	 * map.
	 */
	private void adaptFontSizeToMapScale() {
		int newPoints = (int) map.getScale() - SQUEEZE;
		if(newPoints > getMax()) {
			newPoints = getMax();
		} else if(newPoints < getMin()) {
			newPoints = getMin();
		}

		if(!isFixed && newPoints != getPoints()) {
			debug("Adapting to new font size from [" + getPoints() + "] to [" +
			      newPoints + "] for [" + (isGeneAxis ? "rows" : "cols") + "]",
			      1);
			setSavedPoints(newPoints);
		}
	}

	/**
	 * Keeps track of the hover index and starts up the repaint timer for
	 * shifting labels based on that hover position.  The starting of the timer
	 * is added only for responsiveness since it is started up in the
	 * updateBuffer method.
	 */
	@Override
	public void mouseDragged(final MouseEvent e) {
		setHoverPosition(e);
		hoverIndex = map.getIndex(getPrimaryHoverPosition(e));
		map.setHoverIndex(hoverIndex);

		debug("Selecting from " + map.getSelectingStart() + " to " +
		      map.getIndex(getPrimaryHoverPosition(e)),8);

		//This is not necessary, but makes things a tad more responsive
		if(repaintTimer != null &&
		   repaintTimer.getDelay() == slowRepaintInterval) {
			repaintTimer.setDelay(repaintInterval);
			repaintTimer.restart();
		}

		repaint();

		/* TODO: Handle the temporary outlining of the matrix
		 * NOT SURE HOW TO DO THIS YET - CANNOT TALK TO IMV */
	}

	/**
	 * Keeps track of whether a selection is being made and what kind.
	 */
	@Override
	public void mousePressed(final MouseEvent e) {

		if(!enclosingWindow().isActive())
			return;

		// if left button is used
		if(SwingUtilities.isLeftMouseButton(e)) {
			//Handle the temporary yellow highlighting of the labels
			map.setSelecting(true);

			debug("Selecting start: " +
			      map.getIndex(getPrimaryHoverPosition(e)),8);

			map.setSelectingStart(map.getIndex(getPrimaryHoverPosition(e)));
			map.setHoverIndex(map.getIndex(getPrimaryHoverPosition(e)));

			if(e.isMetaDown()) {
				map.setToggling(true);
			} else if(e.isAltDown()) {
				map.setDeSelecting(true);
			}

			/* TODO: Handle the temporary outlining of the matrix
			 * NOT SURE HOW TO DO THIS YET - CANNOT TALK TO IMV */
		} else if (SwingUtilities.isRightMouseButton(e)) {
			geneSelection.deselectAllIndexes();
			arraySelection.deselectAllIndexes();
		}

		geneSelection.notifyObservers();
		arraySelection.notifyObservers();
	}

	/**
	 * Keeps track of whether a selection is being made and performs the
	 * selection.
	 */
	@Override
	public void mouseReleased(final MouseEvent e) {

		if (!enclosingWindow().isActive())
			return;

		//When left button is used
		//This might interfere with mouseClicked - That should be checked...
		if(SwingUtilities.isLeftMouseButton(e) &&
		   map.getSelectingStart() !=
		   map.getIndex(getPrimaryHoverPosition(e))) {

			/* TODO: Handle the temporary outlining of the matrix
			 * NOT SURE HOW TO DO THIS YET - CANNOT TALK TO IMV */

			int hDI = map.getIndex(getPrimaryHoverPosition(e));
			//If the user dragged off an edge, the index will be out of bounds,
			//so fix it
			if(hDI < 0) {
				hDI = 0;
			} else if(hDI > map.getMaxIndex()) {
				hDI = map.getMaxIndex();
			}

			//Make the selection upon release
			if(otherSelection.getNSelectedIndexes() > 0) {
				if(e.isShiftDown()) {
					drawSelection.selectIndexRange(map.getSelectingStart(),
					                               hDI);
				} else if(e.isMetaDown()) {
					toggleSelectRange(map.getSelectingStart(),
					                  hDI);
				} else if(e.isAltDown()) {
					deSelectRange(map.getSelectingStart(),
					              hDI);
				} else {
					//Deselect everything
					geneSelection.deselectAllIndexes();
					arraySelection.deselectAllIndexes();

					//Select what was dragged over
					otherSelection.selectAllIndexes();
					drawSelection.selectIndexRange(map.getSelectingStart(),
					                               hDI);
				}
			} else {
				//Assumes there is no selection at all & selects what was
				//dragged over
				otherSelection.selectAllIndexes();
				drawSelection
				.selectIndexRange(map.getSelectingStart(),
				                  map.getIndex(getPrimaryHoverPosition(e)));
			}
		}

		//Handle the temporary yellow highlighting of the labels
		map.setSelecting(false);
		map.setDeSelecting(false);
		map.setToggling(false);
		map.setHoverIndex(-1);
		map.setSelectingStart(-1);

		geneSelection.notifyObservers();
		arraySelection.notifyObservers();
	}

	/**
	 * This method toggles the selection booleans over a range of data indexes
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

		//If the user dragged off an edge, the index will be out of bounds, so
		//fix it
		if(start < 0) {
			start = 0;
		}
		if(end > map.getMaxIndex()) {
			end = map.getMaxIndex();
		}

		for(int i = start;i <= end;i++) {
			if(drawSelection.isIndexSelected(i))
				drawSelection.setIndexSelection(i,false);
			else
				drawSelection.setIndexSelection(i,true);
		}
	}

	/**
	 * This method sets the selection booleans to false over a range of data
	 * indexes
	 * @param start
	 * @param end
	 */
	public void deSelectRange(int start,int end) {
		//Make sure start is less than stop
		if(start > end) {
			int tmp = start;
			start = end;
			end = tmp;
		}

		//If the user dragged off an edge, the index will be out of bounds, so
		//fix it
		if(start < 0) {
			start = 0;
		}
		if(end > map.getMaxIndex()) {
			end = map.getMaxIndex();
		}

		for(int i = start;i <= end;i++) {
			drawSelection.setIndexSelection(i,false);
		}
	}

	/* TODO: Deprecated. Remove these functions if they won't be used in the
	 * future. */
	protected abstract void setSecondaryScrollBarPolicyAsNeeded();
	protected abstract void setSecondaryScrollBarPolicyAlways();
	protected abstract void setSecondaryScrollBarPolicyNever();

	/**
	 * Returns the pixel index of the cursor and updates the pixelIndex data
	 * member for use during scroll wheel events (to update the data index
	 * currently hovered over).
	 * @param e
	 * @return
	 */
	protected abstract int getPrimaryHoverPosition(final MouseEvent e);

	/* TODO: Find a purpose for this function. */
	protected abstract boolean isJustifiedToMatrixEdge();

	/**
	 * Keeps track of the hover data index and starts up the repaint timer for
	 * shifting labels based on that hover position.  The starting of the timer
	 * is added only for responsiveness since it is started up in the
	 * updateBuffer method.
	 */
	@Override
	public void mouseMoved(final MouseEvent e) {
		setHoverPosition(e);
		hoverIndex = map.getIndex(getPrimaryHoverPosition(e));
		debug("mouseMoved - updating hoverIndex to [" + hoverIndex + "]",9);

		//This is not necessary, but makes things a tad more responsive
		if(repaintTimer != null &&
		   repaintTimer.getDelay() == slowRepaintInterval) {
			repaintTimer.setDelay(repaintInterval);
			repaintTimer.restart();
		}

		repaint();
	}

	/**
	 * Returns the data index corresponding to the cursor position relative to
	 * the visible portion of the matrix.
	 * @param e
	 * @return
	 */
	public int getPrimaryHoverIndex(final MouseEvent e) {
		return(map.getIndex(getPrimaryHoverPosition(e)));
	}

	/**
	 * This setter allows a hover index to be manually set in cases where the
	 * cursor is for example, over the secondary scroll bar, drag-selecting off
	 * the edge of the matrix, or dragging the opposing labels' secondary
	 * scrollbar
	 * @param i
	 */
	public void setPrimaryHoverIndex(final int i) {
		hoverIndex = i;
	}

	/**
	 * This method computes the hover index based on the last saved pixel index
	 * maintained by getPrimaryHoverPosition.
	 * @return
	 */
	public int getUpdatedPrimaryHoverIndex() {
		if(hoverPixel == -1) {
			return(-1);
		}
		return(map.getIndex(hoverPixel));
	}

	/**
	 * This method returns a boolean indicating whether or not to draw the
	 * components of the label port. It is not just a getter like
	 * inLabelPortMode. It additionally checks whether the conditions of the
	 * display necessitate a label port (e.g. whether the labels' minimum font
	 * height is too big for the current scale of the matrix
	 * @return
	 */
	public boolean doDrawLabelPort() {
		return(inLabelPortMode() && map.overALabelPortLinkedView() &&
		       ((!isFixed && map.getScale() < getMin() + SQUEEZE) ||
		        (isFixed && map.getScale() < last_size)));
	}

	//For debugging purposes only
	@Override
	public void mouseEntered(final MouseEvent e) {
		debug("Mouse entered a label view",9);
	}

	/**
	 * Keeps track of the hover data index by simply setting it to an invalid
	 * state when not in a position over the label area.
	 */
	@Override
	public void mouseExited(final MouseEvent e) {
		setHoverPixel(-1);
		hoverIndex = -1;
		repaint();
	}

	/**
	 * This method sets the scroll data members all to -1 (i.e. an unset state).
	 * This is necessary for when for example, the font changes or when a new
	 * file is opened.
	 */
	public void resetSecondaryScroll() {
		debug("Resetting last secondary scroll position values",2);
		lastScrollRowPos    = -1;
		lastScrollRowEndGap = -1;
		lastScrollRowEndPos = -1;
		lastScrollColPos    = -1;
		lastScrollColEndGap = -1;
		lastScrollColEndPos = -1;
	}

	/**
	 * This method corrects the stored scroll positions for when the viewport
	 * size changes because the size of the knob will have changed and either
	 * the end gap (when left justified) or the position (when right justified)
	 * has changed
	 */
	public void adjustSecondaryScroll(int maxStrLen) {
		debug("Resetting last secondary scroll position values",12);
		if(isGeneAxis) {
			//getSecondaryScrollBar().getModel().getExtent() and
			//getSecondaryScrollBar().getMaximum() cannot be trusted because it
			//does not appear to be updated after a split pane divider move, so
			//we will determine the scroll position based on what we know the
			//viewport and max str length to be
			int extent    = rowLabelViewportSize;
			int scrollMax = (maxStrLen > rowLabelViewportSize ?
			                 maxStrLen : rowLabelViewportSize);
			//Do not adjust if the scrollbars are not properly set
			if(lastScrollRowPos    == -1 ||
			   lastScrollRowEndPos == -1 ||
			   lastScrollRowEndGap == -1) {
				return;
			}
			if(isRightJustified) {
				//lastScrollRowEndGap should not change unless the viewport is
				//now larger than the content minus the previous gap size
				if(lastScrollRowEndGap > 0 &&
				   extent > (maxStrLen - lastScrollRowEndGap)) {
					lastScrollRowEndGap = maxStrLen - extent;
					if(lastScrollRowEndGap < 0) {
						lastScrollRowEndGap = 0;
					}
				}
				lastScrollRowEndPos = scrollMax - lastScrollRowEndGap;
				lastScrollRowPos    = lastScrollRowEndPos - extent;
			} else {
				//lastScrollRowPos should not change unless the viewport is now
				//larger than the content minus the previous position
				if(lastScrollRowPos > 0 &&
				   extent > (maxStrLen - lastScrollRowPos)) {
					lastScrollRowPos = maxStrLen - extent;
					if(lastScrollRowPos < 0) {
						lastScrollRowPos = 0;
					}
				}
				lastScrollRowEndPos = lastScrollRowPos + extent;
				lastScrollRowEndGap = scrollMax - lastScrollRowEndPos;
			}
		} else {
			//getSecondaryScrollBar().getModel().getExtent() and
			//getSecondaryScrollBar().getMaximum() cannot be trusted because it
			//does not appear to be updated after a split pane divider move, so
			//we will determine the scroll position based on what we know the
			//viewport and max str length to be
			int extent    = colLabelViewportSize;
			int scrollMax = (maxStrLen > colLabelViewportSize ?
			                 maxStrLen : colLabelViewportSize);
			//Do not adjust if the scrollbars are not properly set
			if(lastScrollColPos    == -1 ||
			   lastScrollColEndPos == -1 ||
			   lastScrollColEndGap == -1) {
				return;
			}
			if(isRightJustified) {
				//Top Justified, but note that scroll position 0 is at the top
				//and that's the end/right-side of the string
				//lastScrollColPos should not change unless the viewport is now
				//larger than the content minus the previous position
				if(lastScrollColPos > 0 &&
				   extent > (maxStrLen - lastScrollColPos)) {
					lastScrollColPos = maxStrLen - extent;
					if(lastScrollColPos < 0) {
						lastScrollColPos = 0;
					}
				}
				debug("Adjusting columns for bottom justification",1);
				lastScrollColEndPos = lastScrollColPos + extent;
				lastScrollColEndGap = scrollMax - lastScrollColEndPos;
			} else {
				//Bottom justified, but note that the end scroll position is at
				//the bottom and that's the beginning of the string
				//lastScrollColEndGap should not change unless the viewport is
				//now larger than the content minus the previous gap size
				if(lastScrollColEndGap > 0 &&
				   extent > (maxStrLen - lastScrollColEndGap)) {
					lastScrollColEndGap = maxStrLen - extent;
					if(lastScrollColEndGap < 0) {
						lastScrollColEndGap = 0;
					}
				}
				debug("Adjusting columns for top justification",1);
				lastScrollColEndPos = scrollMax - lastScrollColEndGap;
				lastScrollColPos    = lastScrollColEndPos - extent;
			}
		}
	}

	/**
	 * Keeps track of the label pane's viewport size in the secondary dimension
	 * (i.e. the length of the labels) and returns whether a change has occurred
	 * since the last time it was called.
	 * @return
	 */
	public boolean secondaryViewportSizeChanged() {
		boolean changed = false;
		if(isGeneAxis) {
			if(scrollPane.getViewport().getSize().width !=
			   rowLabelViewportSize) {
				changed = true;
				rowLabelViewportSize =
					scrollPane.getViewport().getSize().width;
			}
		} else {
			if(scrollPane.getViewport().getSize().height !=
			   colLabelViewportSize) {
				changed = true;
				colLabelViewportSize =
					scrollPane.getViewport().getSize().height;
			}
		}
		return changed;
	}

	public void trackSecondaryPaneSize(int paneSize) {
		debug("trackSecondaryPaneSize: Pane size saved as [" +
		      (isGeneAxis ? rowLabelPaneSize : colLabelPaneSize) + "]",17);
		if(isGeneAxis) {
			if(offscreenSize.width != rowLabelPaneSize ||
			   paneSize != rowLabelPaneSize) {
			   rowLabelPaneSize    = paneSize;
			   offscreenSize.width = paneSize;
				debug("trackSecondaryPaneSize: Changing row pane size to [" +
				      paneSize + "]",17);
			}
		} else {
			if(offscreenSize.height != colLabelPaneSize ||
			   paneSize != colLabelPaneSize) {
				colLabelPaneSize     = paneSize;
				offscreenSize.height = paneSize;
				debug("trackSecondaryPaneSize: Changing col pane size to [" +
				      paneSize + "]",17);
			}
		}
	}

	/**
	 * This method computes the x coordinate of where the label should be drawn
	 * in order to keep it at least partially in view at any scroll position.
	 * It takes into account current scroll position, row/col axis, label
	 * length, and label justification (right/left or top/bottom).
	 * It computes the start relative to the full pane size, including the
	 * offscreen portion.  Note that the 0 position for drawing row labels is
	 * the same as the 0 position for scrolling row labels, but for column
	 * labels, the 0 position for scrolling is at the end of the labels because
	 * labels are drawn from the bottom (0) up and scrolling in java swing is
	 * from the top (0) down.
	 * @param labelLen
	 * @return offset
	 */
	public int getLabelStartOffset(int labelLen) {
		int offset = 0;
		int paneSize = (isGeneAxis ? rowLabelPaneSize : colLabelPaneSize);
		int indent = (doDrawLabelPort() ? indicatorThickness : labelIndent);
		if(isGeneAxis) {
			if(isRightJustified) {
				debug("Right justified rows. Viewport size: [" +
				      getSecondaryScrollBar().getModel().getExtent() +
				      "] Pane Size: [" + paneSize + "]",11);
				if(lastScrollRowEndPos != -1 && lastScrollRowPos != -1 &&
				   labelLen >
				   (/* Extent */ lastScrollRowEndPos - lastScrollRowPos)) {
    				if(lastScrollRowPos != -1 &&
    					lastScrollRowPos < (paneSize - labelLen)) {
    					offset = lastScrollRowPos + indent;
    				} else {
    					offset = paneSize - labelLen;
    				}
				} else {
					offset = lastScrollRowEndPos - labelLen;
				}
			} else {
				labelLen += indent;
				debug("Left justified rows. Viewport size: [" +
				      getSecondaryScrollBar().getModel().getExtent() +
				      "] Pane Size: [" + paneSize + "]",11);
				if(lastScrollRowEndPos != -1 && lastScrollRowPos != -1 &&
				   labelLen >
				   (/* Extent */ lastScrollRowEndPos - lastScrollRowPos)) {
					if(lastScrollRowEndPos != -1 &&
					   lastScrollRowEndPos > labelLen) {
						offset = lastScrollRowEndPos - labelLen;
					}
				} else {
					offset = lastScrollRowPos;
				}
			}
		} else {
			if(isRightJustified) {
				debug("Top justified columns. Viewport size: [" +
				      getSecondaryScrollBar().getModel().getExtent() +
				      "] Pane Size: [" + paneSize + "]",11);
				if(lastScrollColEndPos != -1 && lastScrollColPos != -1 &&
				   (labelLen + indent) >
				   (/* Extent */ lastScrollColEndPos - lastScrollColPos)) {
					if(lastScrollColEndPos != -1 &&
					   (labelLen + indent) >= lastScrollColEndPos) {
						offset = paneSize - labelLen;
						debug("Setting offset to (paneSize - labelLen) [" +
						      paneSize + " - " + labelLen + "] = [" + offset +
						      "]",16);
					} else {
						offset = lastScrollColEndGap + indent;
						debug("Setting offset to (lastScrollColEndGap + " +
						      "indent) [" + lastScrollColEndGap + " + " +
						      indent + "] = [" + offset + "]",16);
					}
				} else {
					offset = lastScrollColEndGap +
						(/* Extent */ lastScrollColEndPos - lastScrollColPos) -
						labelLen;
					debug("Setting offset to (lastScrollColEndGap + (" +
					      "lastScrollColEndPos - lastScrollColPos) - " +
					      "labelLen) [" + lastScrollColEndGap + " + (" +
					      lastScrollColEndPos + " - " + lastScrollColPos +
					      ") - " + labelLen + "] = [" + offset + "]",16);
				}
				if(labelLen >= 390) debug("Got it! [" + labelLen + "]",7);
			} else {
				labelLen += indent;
				debug("Bottom justified columns. Viewport size: [" +
				      getSecondaryScrollBar().getModel().getExtent() +
				      "] Pane Size: [" + paneSize + "]",11);
				if(lastScrollColEndPos != -1 && lastScrollColPos != -1 &&
				   labelLen >
				   (/* Extent */ lastScrollColEndPos - lastScrollColPos)) {
					if(lastScrollColPos < (paneSize - labelLen)) {
						offset = lastScrollColEndGap +
							(/* Extent */ lastScrollColEndPos -
							 lastScrollColPos) - labelLen;
					}
				} else {
					offset = lastScrollColEndGap;
				}
			}
		}
		return(offset);
	}

	/**
	 * This method decides whether to draw arrows when labels run out of the
	 * viewport view and calls the appropriate arrow drawing function
	 * @param int labelLen
	 * @param Graphics g
	 * @param int yPos
	 * @param int height
	 * @param Color fgColor
	 * @param Color bgColor
	 * @param int xPos
	 * @return void
	 */
	public void drawOverrunArrows(int labelLen,
	                              final Graphics g,
	                              int yPos,
	                              int height,
	                              Color fgColor,
	                              Color bgColor,
	                              int xPos) {
		int paneSize = (isGeneAxis ? rowLabelPaneSize : colLabelPaneSize);
		int indent = getLabelAreaOffset();
		int indentsize = Math.abs(indent);
		if(isGeneAxis) {
			debug("Rows. Viewport size: [" +
			      getSecondaryScrollBar().getModel().getExtent() +
			      "] Pane Size: [" + paneSize + "]",7);
			if(lastScrollRowEndPos != -1 && lastScrollRowPos != -1 &&
			   (labelLen + indentsize) >
			    (/* Extent */ lastScrollRowEndPos - lastScrollRowPos)) {
				if((lastScrollRowEndPos - indentsize) < (xPos + labelLen)) {
					//Draw arrow on right side
					drawRightArrow(g,
					               yPos,
					               lastScrollRowEndPos - indentsize - 1,
					               height,
					               fgColor,
					               bgColor);

					debug("Right overrun row arrow drawn at [" +
					      lastScrollRowEndPos + " + " + indent +
					      " - 1] because  (lastScrollRowEndPos < (xPos + " +
					      "labelLen + indent) || lastScrollRowPos == 0) [" +
					      lastScrollRowEndPos + " < (" + xPos + " + " +
					      labelLen + " + " + indent + ") || " +
					      lastScrollRowPos + " == 0] lastScrollRowEndGap [" +
					      lastScrollRowEndGap + "]",10);
				} else {
					debug("No overrun right row arrow drawn because NOT (" +
					      "lastScrollRowEndPos < (xPos + labelLen + indent) " +
					      "|| lastScrollRowPos == 0) [" + lastScrollRowEndPos +
					      " < (" + xPos + " + " + labelLen + " + " + indent +
					      ") || " + lastScrollRowPos +
					      " == 0] lastScrollRowEndGap [" + lastScrollRowEndGap +
					      "]",10);
				}
				if(lastScrollRowPos > xPos) {
					//Draw arrow on left side
					drawLeftArrow(g,
					              yPos,
					              lastScrollRowPos,
					              height,
					              fgColor,
					              bgColor);
				}
			}
		} else {
			debug("Columns. Viewport size: [" +
			      getSecondaryScrollBar().getModel().getExtent() +
			      "] Pane Size: [" + paneSize + "]",7);
			if(lastScrollColEndPos != -1 && lastScrollColPos != -1 &&
			   (labelLen + indentsize) >
			    (/* Extent */ lastScrollColEndPos - lastScrollColPos)) {
				if(lastScrollColPos >
				   (lastScrollColEndPos + lastScrollColEndGap -
				    (xPos + labelLen))) {
					debug("Drawing left/down arrow at lastScrollColEndPos[" +
					      lastScrollColEndPos + "]",7);

					//Draw arrow on top
					drawRightArrow(g,
					               yPos,
					               paneSize - lastScrollColPos,
					               height,
					               fgColor,
					               bgColor);
				}

				//if(lastScrollColPos > xPos || lastScrollColEndGap == 0) {
				//If the starting position of the string (from the bottom) is
				//larger than the scroll end gap (which is also at the bottom)
				//Note, the indent covers up what would otherwise be visible
				//label, so we must add the indent size to the non-visible
				//portion of the pane content
				if((lastScrollColEndGap + indentsize) > xPos) {
					//Draw arrow on bottom
					drawLeftArrow(g,
					              yPos,
					              paneSize -
					              (lastScrollColEndPos - indentsize - 1),
					              height,
					              fgColor,
					              bgColor);

					debug("Drawing column left overrun arrow at position (" +
					      "paneSize - (lastScrollColEndPos - indentsize - 1)" +
					      ") [" + paneSize + " - (" + lastScrollColEndPos +
					      " - " +indentsize + " - 1)] = [" +
					      (paneSize - (lastScrollColEndPos - indentsize - 1)) +
					      "]",15);
				} else {
					debug("No overrun arrow drawn because (" +
					      "lastScrollColEndGap != 0 && lastScrollColEndGap > " +
					      "xPos) [" + lastScrollColEndGap + " != 0 && " +
					      lastScrollColEndGap + " > " + xPos +
					      "] lastScrollColEndPos [" + lastScrollColEndPos +
					      "] lastScrollColPos [" + lastScrollColPos + "]",10);
				}
			}
		}
	}

	/**
	 * Returns the number of pixels by which labels must be shifted to account
	 * for the label indent and the label port mode indicator bar that is drawn
	 * adjacent to the labels. This value is negative for rows because the
	 * indent/indicator is located at the end of the labels whereas the indent/
	 * indicator offset is positive for columns because it is adjacent to the
	 * beginning of the labels.
	 * @return
	 */
	public int getLabelAreaOffset() {
		return((doDrawLabelPort() ? indicatorThickness : labelIndent) *
		        (isGeneAxis ? -1 : 1));
	}

	/**
	 * This method draws arrows for labels that run out of the
	 * viewport view.  Call BEFORE rotating the column labels
	 * @return
	 */
	public void drawLeftArrow(final Graphics g,int yTop,int xLeft,int height,
	                          Color fgColor,Color bgColor) {
		//Drag a background box to cover up text that we're going to draw over
		g.setColor(bgColor);
		g.fillRect(xLeft - (isGeneAxis ? 0 : 1), //1 is a fudge factor for the x
		                                         //axis - not sure why I need it
		           yTop,
		           (int) Math.floor(height / 2) + 1,
		           height + 1 /*Fudge factor to cover up underscores*/);

		//Make the arrow a little smaller
		height -= 2;

		//Make sure there's an odd number of pixels
		if(height % 2 == 0) {
			height--;
		}
		if(height < 1) {
			return;
		}

		//We can do a better job of drawing an equilateral triangle than the
		//polygon method does by drawing smaller and smaller lines in a loop
		g.setColor(fgColor);
		int curheight = height;
		int indent = 0;
		for(int xcoord = xLeft + (int) Math.floor(height / 2);
		    xcoord >= xLeft;
		    xcoord--) {
			g.drawLine(xcoord,yTop + indent,xcoord,yTop + indent + curheight);
			curheight -= 2;
			indent += 1;
			if(height < 1) break; //Just in case
		}
	}

	/**
	 * This method draws arrows for labels that run out of the
	 * viewport view.  Call BEFORE rotating the column labels
	 * @return
	 */
	public void drawRightArrow(final Graphics g,int yTop,int xRight,int height,
	                           Color fgColor,Color bgColor) {
		//Drag a background box to cover up text that we're going to draw over
		g.setColor(bgColor);
		g.fillRect(xRight - (int) Math.floor(height / 2) -
		           (isGeneAxis ? 0 : 1
		                       /* Fudge factor for x axis - don't know why */),
		           yTop,
		           (int) Math.floor(height / 2) + 3,
		           height + 1 /*Fudge factor to cover up underscores*/);

		//Make the arrow a little smaller
		height -= 2;

		//Make sure there's an odd number of pixels
		if(height % 2 == 0) {
			height--;
		}

		//We can do a better job of drawing an equilateral triangle than the
		//polygon method does by drawing smaller and smaller lines in a loop
		g.setColor(fgColor);
		int curheight = height;
		int indent = 0;
		for(int xcoord = xRight - (int) Math.floor(height / 2);
			xcoord <= xRight;
			xcoord++) {
			debug("Drawing RIGHT ARROW",7);
			g.drawLine(xcoord,yTop + indent,xcoord,yTop + indent + curheight);
			curheight -= 2;
			indent += 1;
			if(height < 1) break; //Just in case
		}
	}
}
