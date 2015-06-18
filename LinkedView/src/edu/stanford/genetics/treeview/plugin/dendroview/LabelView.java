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
	protected final int d_min = 5;
	protected final int d_max = 18;
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

	/* Alignment status */
	protected boolean isRightJustified;

	/* Selection of row/ column indices */
	protected TreeSelectionI arraySelection;
	protected TreeSelectionI geneSelection;

	/*
	 * Stores a reference to the TreeSelection relevant for drawing -- allows to
	 * keep one paint method updateBuffer() for gene- and arraySelection
	 */
	protected TreeSelectionI drawSelection;

	/* Contains all the saved information */
	protected Preferences configNode;

	private final boolean isGeneAxis;

	protected JScrollPane scrollPane;
	protected String zoomHint;
	private boolean hasMouse;

	/* "Position indicator" settings for when the label port is active */
//	int matrixBarThickness = 7; //Thickness of the matrix position bar - Must be an odd number! (so red hover line is centered)
//	int labelBarThickness = 7; //Must be an odd number! (so red hover line is centered)
//	int labelIndicatorProtrusion = 3; //Distance label hover indicator protrudes from label bar - point on indicator
//	int matrixIndicatorIntrusion = (labelIndicatorProtrusion > matrixBarThickness ? (int) Math.ceil(matrixBarThickness / 2) : labelIndicatorProtrusion); //Point on indicator
//	int labelIndent = 3; //Distance label text starts after the end of the label indicator
//	int indicatorThickness = matrixBarThickness + labelBarThickness + labelIndicatorProtrusion + labelIndent; //Should be assigned a value summing the following values if drawing a label port
	int lastScrollRowEndGap = -1;
	int lastScrollRowPos    = -1;
	int lastScrollRowEndPos = -1;
	int lastScrollColEndGap = -1;
	int lastScrollColPos    = -1;
	int lastScrollColEndPos = -1;
	int rowLabelPaneSize    = -1;
	int colLabelPaneSize    = -1;

	protected boolean debug = true;

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

		scrollPane =
			new JScrollPane(this,
			                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
			                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);

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
		LogBuffer.println("Setting min font size from preferences to [" + min +
		                  "]");
		setFace(node.get("face",d_face));
		setStyle(node.getInt("style",d_style));
		setSavedPoints(node.getInt("size",d_size));
		setJustifyOption(node.getBoolean("isRightJustified",d_justified));
		setFixed(node.getBoolean("isFixed",d_fixed));

		/*
		 * TODO: I need to catch when the remembered scroll info has been reset
		 * when deciding where to draw the indicator - then I can uncomment the
		 * line below. This will cause the indicator to be drawn in the correct
		 * place when the font settings are changed and it will allow the scroll
		 * position to be reset properly.
		 */
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
		if(debug) {
			LogBuffer.println("setMin(" + i + ") called");
		}
		if(i < 1 || max > 0 && i > max) { return; }

		if(min != i) {
			min = i;
			if(configNode != null) {
				LogBuffer.println("Saving new min font size [" + min + "]");
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
		if(debug) {
			LogBuffer.println("setMax(" + i + ") called");
		}
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
	private int repaintInterval = 50;  // update every 50 milliseconds
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

				repaintTimer.stop();
				map.setLabelAnimeRunning(false);
			}
		}
	};

	/* inherit description */
	@Override
	public void updateBuffer(final Graphics g) {
		updateBuffer(g,offscreenSize);
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

		//If the mouse is not hovering over the IMV, stop both timers, set the
		//last hover index, and tell mapcontainer that the animation has stopped
		if(map.getHoverIndex() == -1) {
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
			repaintTimer.start();
			map.setLabelAnimeRunning(true);
			lastHoverIndex = map.getHoverIndex();
			//Disable any turnOffRepaintTimer that might have been left over
			if(turnOffRepaintTimer != null) {
				turnOffRepaintTimer.stop();
				turnOffRepaintTimer = null;
			}
		}
		//Else if the mouse hasn't moved, start the second timer to turn off the
		//first after 1 second (this mitigates delays upon mouse motion after a
		//brief period of no motion)
		else if(map.getHoverIndex() == lastHoverIndex) {
			if(turnOffRepaintTimer == null) {
				turnOffRepaintTimer = new Timer(delay,turnOffRepaintListener);
				turnOffRepaintTimer.start();
			}
		}
		//Else, disable the turnOffRepaintTimer and update the hover index
		else {
			//Disable the turnOffRepaintTimer because we have detected continued
			//mouse motion
			if(turnOffRepaintTimer != null) {
				turnOffRepaintTimer.stop();
				turnOffRepaintTimer = null;
			}
			lastHoverIndex = map.getHoverIndex();
			map.setLabelAnimeRunning(true);
		}

		if(debug) {
			LogBuffer.println("Updating the label pane graphics");
		}
		/** TODO: Make sure that the number of visible labels is up to date */

		/* Shouldn't draw if there's no TreeSelection defined */
		if(drawSelection == null) {
			LogBuffer.println("drawSelection not defined. Can't draw labels.");
			return;
		}

		int matrixHoverIndex = map.getHoverIndex();

		final int stringX = isGeneAxis ? offscreenSize.width
		                              : offscreenSize.height;

		final Graphics2D g2d = (Graphics2D) g;
		final AffineTransform orig = g2d.getTransform();

		/* Draw labels if zoom level allows it */
		final boolean hasFixedOverlap = isFixed && map.getScale() < last_size;
		boolean resetJustification =
			isGeneAxis ?
			scrollPane.getViewport().getSize().width == offscreenSize.width :
			scrollPane.getViewport().getSize().height == offscreenSize.height;
		if(resetJustification && debug) {
			LogBuffer.println("Resetting justification for " +
			                  (isGeneAxis ? "ROW" : "COL") + "s!!!!");
		}

		if(map.getScale() >= getMin() + SQUEEZE && !hasFixedOverlap ||
		   doDrawLabelPort()) {

			if(secondaryPaneSizeChanged()) {
				adjustSecondaryScroll();
			}

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

			boolean drawLabelPort = doDrawLabelPort();
			if(drawLabelPort && debug) {
				LogBuffer.println("Drawing a label port");
			}

			/* Get indices range */
			final int start = map.getIndex(0);
			final int end   = map.getIndex(map.getAvailablePixels()) - 1;
			LogBuffer.println("Set end data index to [" + end +
			                  "] as first [" +
			                  map.getFirstVisible() +
			                  "] plus num [" +
			                  map.getNumVisible() +
			                  "] visible - 1");

			/* Rotate plane for array axis (not for zoomHint) */
			if(!isGeneAxis) {
				g2d.rotate(Math.PI * 3 / 2);
				g2d.translate(-offscreenSize.height,0);
			}

			final int colorIndex = headerInfo.getIndex("FGCOLOR");
			g.setFont(new Font(face,style,size));

			final FontMetrics metrics = getFontMetrics(g.getFont());
			final int ascent = metrics.getAscent();

			int matrixSize   = map.getPixel(map.getMaxIndex() + 1) - 1 -
			                   map.getPixel(0);
			int fullPaneSize = matrixSize;

			int activeHoverDataIndex =
				hoverIndex == -1 ? matrixHoverIndex : hoverIndex;

			if(debug) {
				LogBuffer.println("Label port mode is " +
				                  (drawLabelPort ? "" : "in") +
				                  "active.  Map square size: [" +
				                  map.getScale() + "] Font size: [" +
				                  curFontSize + "] SQUEEZE: [" + SQUEEZE +
				                  "] ascent: [" + ascent + "]");
			}

			/* Draw label backgrounds first if color is defined */
			final int bgColorIndex = headerInfo.getIndex("BGCOLOR");
			Color bgColor = g.getColor();
			if(bgColorIndex > 0) {
				if(debug) {
					LogBuffer.println("BgLabel");
				}
				final Color back = g.getColor();
				for(int j = start;j <= end;j++) {
					final String[] strings = headerInfo.getHeader(j);
					try {
						g.setColor(TreeColorer.getColor(strings[bgColorIndex]));

					}
					catch(final Exception e) {
						// ignore...
					}
					g.fillRect(0,
					           map.getMiddlePixel(j) - ascent / 2,
					           stringX,
					           ascent);
				}
				g.setColor(back);
			}

			/* Draw the labels */
			int maxStrLen = getJustifiedPosition(metrics);
			final Color fore = GUIFactory.MAIN;
			final Color labelPortColor = new Color(30,144,251);
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

				for(int j = activeHoverDataIndex;j >= start;j--) {
					try {
						String out = headerSummary.getSummary(headerInfo,j);
						final String[] headers = headerInfo.getHeader(j);

						if(out == null) {
							out = "No Label";
						}

						/* Set label color */
						if(drawSelection.isIndexSelected(j) ||
						   j == activeHoverDataIndex) {
							if(colorIndex > 0) {
								g.setColor(TreeColorer
								           .getColor(headers[colorIndex]));
							}

							if(j == activeHoverDataIndex) {
								g2d.setColor(Color.red);
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
						int xPos = 0;
						if(isRightJustified) {
							xPos = stringX - metrics.stringWidth(out);
						}

						int indexDiff = j - activeHoverDataIndex;
						int yPos = hoverYPos + indexDiff *
						           (curFontSize + SQUEEZE);
						//Account for offsets from being near an edge
						yPos -= edgeOffset;
						if(yPos > -ascent / 2) {
							g2d.drawString(out,xPos,yPos);
						}

					}
					catch(final java.lang.ArrayIndexOutOfBoundsException e) {
						LogBuffer.logException(e);
						break;
					}
				}
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

							g2d.setColor(fore);

							if(colorIndex > 0) {
								g.setColor(fore);
							}

						} else {
							g2d.setColor(Color.black);
						}

						/* Finally draw label (alignment-dependent) */
						int xPos = 0;
						if(isRightJustified) {
							xPos = stringX - metrics.stringWidth(out);
						}

						int indexDiff = j - activeHoverDataIndex;
						int yPos = hoverYPos + indexDiff *
						           (curFontSize + SQUEEZE);
						yPos -= edgeOffset;
						if(yPos < fullPaneSize + ascent / 2) {
							g2d.drawString(out,xPos,yPos);
						}

					}
					catch(final java.lang.ArrayIndexOutOfBoundsException e) {
						LogBuffer.logException(e);
						break;
					}
				}
			} else {
				for(int j = start;j <= end;j++) {

					if(debug) {
						LogBuffer.println("Getting data index [" + j + "]");
					}
					try {
						String out = headerSummary.getSummary(headerInfo,j);
						final String[] headers = headerInfo.getHeader(j);

						if(out == null) {
							out = "No Label";
						}

						/* Set label color */
						if(drawSelection.isIndexSelected(j) ||
						   j == activeHoverDataIndex) {
							if(colorIndex > 0) {
								g.setColor(TreeColorer
								           .getColor(headers[colorIndex]));
							}

							if(j == activeHoverDataIndex) {
								g2d.setColor(Color.red);
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
						int xPos = 0;
						if(isRightJustified) {
							xPos = stringX - metrics.stringWidth(out);
						}

//						if(debug) {
//							LogBuffer.println("Drawing " +
//							                  (isGeneAxis ? "ROW" : "COL") +
//							                  " label at X pixel [" +
//							                  (xPos +
//							                   (drawLabelPort ?
//							                    indicatorThickness :
//							                    labelIndent) *
//							                   (isGeneAxis ? -1 : 1)) +
//							                  "] xPos [" + xPos +
//							                  "] indicatorThickness [" +
//							                  (indicatorThickness *
//							                   (isGeneAxis ? -1 : 1)) + "].");
//						}
						g2d.drawString(out,xPos,map.getMiddlePixel(j) + ascent /
						                        2);

					}
					catch(final java.lang.ArrayIndexOutOfBoundsException e) {
						LogBuffer.logException(e);
						break;
					}
				}
			}
			if(debug) {
				LogBuffer.println((isGeneAxis ? "ROW" : "COL") +
				                  ": MaxStrLen: [" + maxStrLen +
				                  "]Start Index: [" + start + "] End Index: [" +
				                  end + "] height [" + offscreenSize.height +
				                  "] width [" + offscreenSize.width + "]");
			}

			//I MIGHT resurect some or all of the following commented code
			//depending on feedback from the next meeting (after 6/18/2015)
			//Other associated commented code exists elsewhere too

//			/* Draw the "position indicator" if the label port is active */
//			//See variables initialized above the string drawing section
//			if(drawLabelPort) {
//				if(!isGeneAxis) {
//					if(isRightJustified) {
//						LogBuffer.println("Resetting justification via drawing for rows!  Will draw at maxstrlen + indicatorthickness - lastendgap [" + maxStrLen + " + " + indicatorThickness + " - " + lastScrollRowEndGap + " = " + (maxStrLen + indicatorThickness - lastScrollRowEndGap) + "] - indicatorThickness [" + indicatorThickness + "] instead of current [" + secondaryScrollPos + "] - indicatorThickness [" + indicatorThickness + "]. MaxStrLen: [" + maxStrLen + "]");
//						secondaryScrollEndPos = maxStrLen + indicatorThickness - lastScrollRowEndGap;
//					} else {
//						LogBuffer.println("Resetting justification via drawing for rows!  Will draw at lastScrollRowEndPos [" + lastScrollRowEndPos + "] instead of current [" + secondaryScrollEndPos + "] - indicatorThickness [" + indicatorThickness + "]. MaxStrLen: [" + maxStrLen + "]");
//						secondaryScrollEndPos = (lastScrollRowEndPos == -1 ? scrollPane.getViewport().getSize().width : lastScrollRowEndPos);
//					}
//
//					//Switch to the background color
//					g2d.setColor(bgColor);
//					//Draw a background box to blank-out and partially scrolled labels
//					g.fillRect(
//							secondaryScrollEndGap,
//							start * (curFontSize + SQUEEZE) - 1,
//							indicatorThickness,
//							(end + 1) * (curFontSize + SQUEEZE));
//					//Switch to the label port color
//					g2d.setColor(labelPortColor);
////					//Draw the first border line
////					g2d.drawLine(
////							secondaryScrollEndGap + indicatorThickness -
////								(labelIndent + labelIndicatorProtrusion),
////							start * (curFontSize + SQUEEZE),
////							indicatorThickness + maxStrLen,
////							start * (curFontSize + SQUEEZE));
////					//Draw the second border line
////					g2d.drawLine(
////							secondaryScrollEndGap + indicatorThickness -
////								(labelIndent + labelIndicatorProtrusion),
////							(end + 1) * (curFontSize + SQUEEZE) - 1,
////							indicatorThickness + maxStrLen,
////							(end + 1) * (curFontSize + SQUEEZE) - 1);
////					//Draw the label breadth bar
////					g.fillRect(
////							secondaryScrollEndGap + matrixBarThickness,
////							start * (curFontSize + SQUEEZE),
////							labelBarThickness,
////							(end + 1) * (curFontSize + SQUEEZE) - start * (curFontSize + SQUEEZE));
////					//Draw the matrix breadth bar
////					g.fillRect(
////							secondaryScrollEndGap,
////							start * (curFontSize + SQUEEZE) + map.getPixel(start),
////							matrixBarThickness,
////							map.getPixel(end + 1) - map.getPixel(start) + 1);
//					//If there is a data index that is hovered over
//					int activeHoverDataIndex =
//							(hoverIndex == -1 ? matrixHoverIndex : hoverIndex);
//					if(activeHoverDataIndex > -1) {
//						//Change to the hover color
//						g2d.setColor(Color.red);
//						//Draw the hover matrix position indicator
//						int matrixPixel = start * (curFontSize + SQUEEZE) + map.getPixel(activeHoverDataIndex);
//						LogBuffer.println("Hover matrix start: [" + start + "] startpixel: [" + start * (curFontSize + SQUEEZE) + "] activeHoverIndex: [" + activeHoverDataIndex + "] data cell pixel [" + map.getPixel(activeHoverDataIndex) + "] matrixPixel: [" + matrixPixel + "]");
//						int labelPixel = activeHoverDataIndex * (curFontSize + SQUEEZE);
//						boolean outOfBounds = activeHoverDataIndex > end || activeHoverDataIndex < start;
//						boolean drawAMatrixPoint = ((int) Math.round(map.getScale()) > 2 && !outOfBounds);
//						if(drawAMatrixPoint) {
//							//Draw the base of the indicator
//							g.fillRect(
//									secondaryScrollEndGap + matrixIndicatorIntrusion,
//									matrixPixel,
//									matrixBarThickness - matrixIndicatorIntrusion + 1,
//									(int) Math.round(map.getScale()));
//							//Draw the point of the indicator
//							//Do a 4 point polygon if there's an even number of pixels in order to have a stubby but symmetrical point
//							int baseWidth = (int) Math.floor(map.getScale());
//							int center = matrixPixel + (int) Math.ceil((int) Math.floor(map.getScale()) / 2);
//							if(baseWidth % 2 == 1) {
//								int[] exes = {secondaryScrollEndGap + matrixIndicatorIntrusion,secondaryScrollEndGap + matrixIndicatorIntrusion,secondaryScrollEndGap};
//								int[] whys = {matrixPixel,matrixPixel + (int) Math.ceil(map.getScale()) - 1,center};
//								//g2d.setColor(Color.green);
//								g.fillPolygon(exes, whys, 3);
//							} else {
//								int[] exes = {secondaryScrollEndGap + matrixIndicatorIntrusion,secondaryScrollEndGap + matrixIndicatorIntrusion,secondaryScrollEndGap,secondaryScrollEndGap};
//								int[] whys = {matrixPixel,matrixPixel + (int) Math.ceil(map.getScale()) - 1,center - 1,center};
//								//g2d.setColor(Color.black);
//								g.fillPolygon(exes, whys, 4);
//							}
//							//g2d.setColor(Color.red);
//						} else {
//							g.fillRect(
//									secondaryScrollEndGap,
//									matrixPixel,
//									(outOfBounds ?
//											(int) Math.ceil(matrixBarThickness / 2) + 1 : matrixBarThickness + 1),
//									(int) Math.round(map.getScale()));
//						}
//						if(!outOfBounds) {
//							//Draw the hover label position indicator
////							g.fillRect(
////									secondaryScrollEndGap + matrixBarThickness + (int) Math.floor(labelBarThickness / 2),
////									labelPixel,
////									(int) Math.ceil(labelBarThickness / 2) + labelIndicatorProtrusion + 1,
////									curFontSize + SQUEEZE);
//							//Draw the base of the indicator
//							g.fillRect(
//									secondaryScrollEndGap + matrixBarThickness,
//									labelPixel,
//									labelBarThickness + 1,
//									curFontSize + SQUEEZE);
//							//Draw the point of the indicator
//							//Do a 4 point polygon if there's an even number of pixels in order to have a stubby but symmetrical point
//							if((curFontSize + SQUEEZE) % 2 == 1) {
//								//g2d.setColor(Color.gray);
//								int[] exes = {secondaryScrollEndGap + matrixBarThickness + labelBarThickness,secondaryScrollEndGap + matrixBarThickness + labelBarThickness,secondaryScrollEndGap + matrixBarThickness + labelBarThickness + labelIndicatorProtrusion + 1};
//								int[] whys = {labelPixel - 1,labelPixel + curFontSize + SQUEEZE,labelPixel + (int) Math.ceil((curFontSize + SQUEEZE) / 2)};
//								g.fillPolygon(exes, whys, 3);
//							} else {
//								//g2d.setColor(Color.green);
//								int[] exes = {secondaryScrollEndGap + matrixBarThickness + labelBarThickness,secondaryScrollEndGap + matrixBarThickness + labelBarThickness,secondaryScrollEndGap + matrixBarThickness + labelBarThickness + labelIndicatorProtrusion,secondaryScrollEndGap + matrixBarThickness + labelBarThickness + labelIndicatorProtrusion};
//								int[] whys = {labelPixel - 1,labelPixel + curFontSize + SQUEEZE,labelPixel + (int) (curFontSize + SQUEEZE) / 2 - 1,labelPixel + (int) (curFontSize + SQUEEZE) / 2};
//								g.fillPolygon(exes, whys, 4);
//							}
//							//g2d.setColor(Color.red);
//						}
//						//Draw the connection btwn hvr label & matrix pos indicators
//						g2d.drawLine(
//								secondaryScrollEndGap + (outOfBounds ? (int) Math.floor(matrixBarThickness / 2) : matrixBarThickness) + 1,
//								matrixPixel,
//								secondaryScrollEndGap + (outOfBounds ? (int) Math.floor(matrixBarThickness / 2) : matrixBarThickness) + 1,
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
//					}
//				} else {
//					//int secondaryScrollEndPos = secondaryScrollPos + getSecondaryScrollBar().getModel().getExtent();
//					if(isRightJustified) {
//						LogBuffer.println("Resetting justification via drawing for rows!  Will draw at maxstrlen + indicatorthickness - lastendgap [" + maxStrLen + " + " + indicatorThickness + " - " + lastScrollRowEndGap + " = " + (maxStrLen + indicatorThickness - lastScrollRowEndGap) + "] - indicatorThickness [" + indicatorThickness + "] instead of current [" + secondaryScrollPos + "] - indicatorThickness [" + indicatorThickness + "]. MaxStrLen: [" + maxStrLen + "]");
//						secondaryScrollEndPos = maxStrLen + indicatorThickness - lastScrollRowEndGap;
//					} else {
//						LogBuffer.println("Resetting justification via drawing for rows!  Will draw at lastScrollRowEndPos [" + (lastScrollRowEndPos == -1 ? scrollPane.getViewport().getSize().width : lastScrollRowEndPos) + "] instead of current [" + secondaryScrollEndPos + "] - indicatorThickness [" + indicatorThickness + "]. MaxStrLen: [" + maxStrLen + "]");
//						secondaryScrollEndPos = (lastScrollRowEndPos == -1 ? scrollPane.getViewport().getSize().width : lastScrollRowEndPos);
//					}
//
//					//Switch to the background color
//					g2d.setColor(bgColor);
//					//resetJustification = (secondaryScrollPos == 0 && secondaryScrollEndGap == 0);
//					//Draw a background box to blank-out and partially scrolled labels
//					LogBuffer.println("Starting indicator drawing at position [" + (secondaryScrollEndPos - indicatorThickness) + "]");
//					g.fillRect(
//							secondaryScrollEndPos - indicatorThickness,
//							start * (curFontSize + SQUEEZE) - 1,
//							indicatorThickness,
//							(end + 1) * (curFontSize + SQUEEZE));
//					//Switch to the label port color
//					g2d.setColor(labelPortColor);
////					//Draw the first border line
////					g2d.drawLine(
////							secondaryScrollEndPos - indicatorThickness +
////								(labelIndent + labelIndicatorProtrusion),
////							start * (curFontSize + SQUEEZE),
////							0,
////							start * (curFontSize + SQUEEZE));
////					//Draw the second border line
////					g2d.drawLine(
////							secondaryScrollEndPos - indicatorThickness +
////								(labelIndent + labelIndicatorProtrusion),
////							(end + 1) * (curFontSize + SQUEEZE) - 1,
////							0,
////							(end + 1) * (curFontSize + SQUEEZE) - 1);
////					//Draw the label breadth bar
////					g.fillRect(
////							secondaryScrollEndPos - matrixBarThickness - labelBarThickness,
////							start * (curFontSize + SQUEEZE),
////							labelBarThickness,
////							(end + 1) * (curFontSize + SQUEEZE) - start * (curFontSize + SQUEEZE));
////					//Draw the matrix breadth bar
////					g.fillRect(
////							secondaryScrollEndPos - matrixBarThickness,
////							start * (curFontSize + SQUEEZE) + map.getPixel(start),
////							matrixBarThickness,
////							map.getPixel(end + 1) - map.getPixel(start) + 1);
//					//If there is a data index that is hovered over
//					int activeHoverDataIndex =
//							(hoverIndex == -1 ? matrixHoverIndex : hoverIndex);
//					if(activeHoverDataIndex > -1) {
//						//Change to the hover color
//						g2d.setColor(Color.red);
//						//Draw the hover matrix position indicator
//						int matrixPixel = start * (curFontSize + SQUEEZE) + map.getPixel(activeHoverDataIndex);
//						int labelPixel = activeHoverDataIndex * (curFontSize + SQUEEZE);
//						boolean outOfBounds = activeHoverDataIndex > end || activeHoverDataIndex < start;
//						g.fillRect(
//								secondaryScrollEndPos - (outOfBounds ?
//										(int) Math.ceil(matrixBarThickness / 2) : matrixBarThickness) - 1,
//								matrixPixel,
//								(outOfBounds ?
//										(int) Math.ceil(matrixBarThickness / 2) : matrixBarThickness) + 1,
//								(int) Math.round(map.getScale()));
//						if(!outOfBounds) {
//							//Draw the hover label position indicator
//							g.fillRect(
//									secondaryScrollEndPos - indicatorThickness + labelIndent,
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
//					}
//				}
//			}

			//I MIGHT resurect some or all of the preceding commented code
			//depending on feedback from the next meeting (after 6/18/2015)
			//Other associated commented code exists elsewhere too

			g2d.setTransform(orig);

			//Set the size of the scrollpane to match the longest string
			if(isGeneAxis) {
				if(offscreenSize.height != fullPaneSize || offscreenSize.width != maxStrLen) {
					setPreferredSize(new Dimension(maxStrLen
					                               /* + (drawLabelPort ?
					                                *    indicatorThickness : 0)
					                                */,
					                               fullPaneSize));
				}
				if(debug) {
					LogBuffer.println("Resizing row labels panel to [" +
					                  maxStrLen + "x" + fullPaneSize + "].");
				}
			} else {
				if(offscreenSize.height != maxStrLen || offscreenSize.width != fullPaneSize) {
					setPreferredSize(new Dimension(fullPaneSize,
					                               maxStrLen
					                               /* + (drawLabelPort ?
					                                * indicatorThickness : 0) */
					                               ));
				}
				if(debug) {
					LogBuffer.println("Resizing col labels panel to [" +
					                  fullPaneSize + "x" + maxStrLen + "].");
				}
			}

			/*
			 * Scroll to the position that is equivalent to the previous
			 * position
			 */
			if(isRightJustified) {
				if(isGeneAxis) {
					if(lastScrollRowPos == -1) {
						LogBuffer.println("Scrolling to [" +
						                  (getSecondaryScrollBar().getMaximum() -
						                   getSecondaryScrollBar().getModel()
						                   .getExtent()) +
						                  "] max - extent [" +
						                  getSecondaryScrollBar().getMaximum() +
						                  " - " +
						                  getSecondaryScrollBar().getModel()
						                  .getExtent() +
						                  "] after drawing - first time rows " +
						                  "right justified");
						//It seems that the scrollbar max and extent are not
						//updated in this scenario when the app first opens a
						//file, so we will calculate the initial scroll position
						//thusly
						int tmpscrollpos =
							maxStrLen -
						    scrollPane.getViewport().getSize().width;
						getSecondaryScrollBar().setValue(tmpscrollpos);
						lastScrollRowPos = tmpscrollpos;
						lastScrollRowEndPos = maxStrLen;
						lastScrollRowEndGap = 0;
					} else {
						LogBuffer.println("Scrolling to [" + lastScrollRowPos +
						                  "] after drawing - rememberred " +
						                  "rows right justified");
						getSecondaryScrollBar().setValue(lastScrollRowPos);
					}
				} else {
					/*
					 * TODO: Save column scroll position - below is copied from
					 * row logic above - edit it!
					 */
					if(lastScrollColPos == -1) {
						LogBuffer.println("Scrolling to [0] after drawing - " +
						                  "first time cols left justified");
						getSecondaryScrollBar().setValue(0);
						//It seems that the scrollbar max and extent are not
						//updated in this scenario when the app first opens a
						//file, so we will calculate the initial scroll position
						//thusly
						lastScrollColPos = 0;
						lastScrollColEndPos =
							scrollPane.getViewport().getSize().width;
						lastScrollColEndGap = maxStrLen -
							scrollPane.getViewport().getSize().width;
					} else {
						LogBuffer.println("Scrolling to [" + lastScrollColPos +
						                  "] after drawing - rememberred " +
						                  "cols right justified");
						getSecondaryScrollBar().setValue(lastScrollColPos);
					}
				}
			} else {
				if(isGeneAxis) {
					if(lastScrollRowPos == -1) {
						LogBuffer.println("Scrolling to [0] after drawing - " +
						                  "first time rows left justified");
						getSecondaryScrollBar().setValue(0);
						//It seems that the scrollbar max and extent are not
						//updated in this scenario when the app first opens a
						//file, so we will calculate the initial scroll position
						//thusly
						lastScrollRowPos = 0;
						lastScrollRowEndPos =
							scrollPane.getViewport().getSize().width;
						lastScrollRowEndGap = maxStrLen -
							scrollPane.getViewport().getSize().width;
					} else {
						LogBuffer.println("Scrolling to [" + lastScrollRowPos +
						                  "] after drawing - rememberred " +
						                  "rows left justified");
						getSecondaryScrollBar().setValue(lastScrollRowPos);
					}
				} else {
					/*
					 * TODO: Save column scroll position - below is copied from
					 * row logic above - edit it!
					 */
					if(lastScrollColPos == -1) {
						LogBuffer.println("Scrolling to [" +
						                  (getSecondaryScrollBar()
						                   .getMaximum() -
						                   getSecondaryScrollBar().getModel()
						                   .getExtent()) +
						                  "] max - extent [" +
						                  getSecondaryScrollBar().getMaximum() +
						                  " - " +
						                  getSecondaryScrollBar().getModel()
						                  .getExtent() +
						                  "] after drawing - first time " +
						                  "cols right justified");
						//It seems that the scrollbar max and extent are not
						//updated in this scenario when the app first opens a
						//file, so we will calculate the initial scroll
						//position thusly
						int tmpscrollpos = maxStrLen -
							scrollPane.getViewport().getSize().height;
						getSecondaryScrollBar().setValue(tmpscrollpos);
						lastScrollColPos    = tmpscrollpos;
						lastScrollColEndPos = maxStrLen;
						lastScrollColEndGap = 0;
					} else {
						LogBuffer.println("Scrolling to [" + lastScrollColPos +
						                  "] after drawing - rememberred " +
						                  "cols left justified");
						getSecondaryScrollBar().setValue(lastScrollColPos);
					}
				}
			}

//			lastScrollRowEndGap = secondaryScrollEndGap;
//			lastScrollRowPos = secondaryScrollPos;
//			lastScrollRowEndPos = secondaryScrollEndPos;
		} else {
			setPoints(HINTFONTSIZE);
			g2d.setColor(Color.black);

			int xPos = getHintX(g2d,stringX);
			int yPos = getHintY(g2d);
			final FontMetrics metrics = getFontMetrics(g2d.getFont());
			boolean isBeginJustified;
			int scrollSize;

			if(isGeneAxis) {
				//Reduce the size of the scrollpane to just what is visible
				setPreferredSize(new Dimension(scrollPane.getViewport()
				                                         .getSize().width,
				                               scrollPane.getViewport()
				                                         .getSize().height));

				isBeginJustified = isRightJustified;
				scrollSize = offscreenSize.width;
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
				if(debug) {
					LogBuffer.println("Row Hint position: [" + xPos + "/" +
					                  yPos + "] stringX: [" + stringX +
					                  "] zoomHint: [" + zoomHint +
					                  "] height [" + offscreenSize.height +
					                  "] width [" + offscreenSize.width +
					                  "] + HintStrLen [" +
					                  metrics.stringWidth(zoomHint) + "]");
				}
			} else {
				//Reduce the size of the scrollpane to just what is visible
				setPreferredSize(new Dimension(scrollPane.getViewport()
				                               .getSize().width,
				                               scrollPane.getViewport()
				                               .getSize().height));

				isBeginJustified = !isRightJustified;
				scrollSize = offscreenSize.height;
				xPos = (offscreenSize.width -
				        metrics.stringWidth(zoomHint)) / 2;
				yPos = scrollPane.getViewport().getSize().height / 2;
				if(debug) {
					LogBuffer.println("Col Hint position: [" + xPos + "/" +
					                  yPos + "] stringX: [" + stringX +
					                  "] zoomHint: [" + zoomHint +
					                  "] height [" + offscreenSize.height +
					                  "] width [" + offscreenSize.width +
					                  "] + HintStrLen [" +
					                  metrics.stringWidth(zoomHint) + "]");
				}
			}

			if(isBeginJustified) {
				for(int i = scrollSize - yPos;i > 0;i -= yPos * 2) {
					g2d.drawString(zoomHint,xPos,i);
				}
			} else {
				for(int i = yPos;i < scrollSize;i += yPos * 2) {
					g2d.drawString(zoomHint,xPos,i);
				}
			}

			//Adding this useless scroll actually causes the pane to update
			//properly - and reflect the settings made above.  Without it, the
			//scrollbar still implies that the panel is its previous size and
			//the hint is off center
			getSecondaryScrollBar().setValue(0);
		}
	}

	public int getJustifiedPosition(FontMetrics metrics) {
		int min =
			isGeneAxis ? scrollPane.getViewport().getSize().width :
						 scrollPane.getViewport().getSize().height
						 /* - indicatorThickness */;
		int len = getMaxStringLength(metrics);
		return len > min ? len : min;
	}

	/**
	 * Returns either the number of pixels in length the longest string is, or
	 * the dimension of the panel the strings will be drawn in (minus the
	 * indicator thickness) - whichever is larger)
	 */
	public int getMaxStringLength(FontMetrics metrics) {
		/* Draw the labels */
		int end = map.getMaxIndex();
		int maxStrLen = 0;
		String maxStr = "";
		if(lastDrawnFace == face  && lastDrawnStyle == style &&
		   longest_str_index > -1 && lastDrawnSize  == size &&
			/* TODO: Assuming if the string is the same, it's the same overall
		     * data - it would be better to catch when the data changes */
			longest_str.equals(headerSummary.getSummary(headerInfo,
		                                               longest_str_index))) {
			LogBuffer.println("Regurgitating maxstrlen");
			if(debug) {
				maxStr = headerSummary.getSummary(headerInfo,longest_str_index);
			}
			maxStrLen = longest_str_length;
		} else if(lastDrawnFace == face && lastDrawnStyle == style &&
		          longest_str_index > -1 &&
		          lastDrawnSize != size &&
		          /* TODO: Assuming if the string is the same, it's the same
				   * overall data - it would be better to catch when the data
				   * changes */
		          longest_str.equals(headerSummary.getSummary(headerInfo,
		                                                      longest_str_index)
		         )) {
			LogBuffer.println("Refining maxstrlen");
			maxStr = headerSummary.getSummary(headerInfo,longest_str_index);
			maxStrLen = metrics.stringWidth(maxStr);
		} else {
			LogBuffer.println("Calculating maxstrlen because not [" +
			                  "lastDrawnFace == face && lastDrawnStyle == " +
			                  "style && longest_str_index > -1 && " +
			                  "lastDrawnSize != size && longest_str." +
			                  "equals(headerSummary.getSummary(headerInfo," +
			                  "longest_str_index))]");
			LogBuffer.println("Calculating maxstrlen because not [" +
			                  lastDrawnFace + " == " + face + " && " +
			                  lastDrawnStyle + " == " + style + " && " +
			                  longest_str_index + " > -1 && " + lastDrawnSize +
			                  " != " + size + " && " + longest_str +
			                  ".equals(headerSummary.getSummary(headerInfo," +
			                  longest_str_index + "))]");
			for(int j = 0;j <= end;j++) {
				try {
					String out = headerSummary.getSummary(headerInfo,j);

					if(out == null) {
						out = "No Label";
					}

					if(maxStrLen < metrics.stringWidth(out)) {
						maxStrLen = metrics.stringWidth(out);
						longest_str_index = j;
						if(debug) {
							maxStr = out;
						}
					}

				}
				catch(final java.lang.ArrayIndexOutOfBoundsException e) {
					LogBuffer.logException(e);
					break;
				}
			}
			longest_str = maxStr;
		}
		lastDrawnFace = face;
		lastDrawnStyle = style;
		lastDrawnSize = size;
		longest_str_length = maxStrLen;
		if(debug) {
			LogBuffer.println((isGeneAxis ? "ROW" : "COL") + ": MaxStrLen: [" +
			                  maxStrLen + "] MaxStr: [" + maxStr +
			                  "] Start Index: [" + 0 + "] End Index: [" + end +
			                  "] height [" + offscreenSize.height +
			                  "] width [" + offscreenSize.width + "]");
		}

		return maxStrLen;
	}

	/**
	 * Gets x-position for the hint label based on axis and justification.
	 * 
	 * @param g2d
	 * @param stringX
	 * @return x-position for hint label.
	 */
	private int getHintX(Graphics2D g2d,int stringX) {

		int xPos = 0;
		int offSet = 10;
		final FontMetrics metrics = getFontMetrics(g2d.getFont());

		if(isGeneAxis) {
			if(isRightJustified) {
				xPos = stringX - metrics.stringWidth(zoomHint) - offSet;
			} else {
				xPos = offSet;
			}

		} else {
			xPos = (offscreenSize.width - metrics.stringWidth(zoomHint)) / 2;
		}

		return xPos;
	}

	/**
	 * Gets y-position for the hint label based on axis and justification.
	 * 
	 * @param g2d
	 * @return y-position for hint label.
	 */
	private int getHintY(Graphics2D g2d) {

		int yPos = 0;
		if(isGeneAxis) {
			yPos = panel.getHeight() / 2;

		} else {
			if(isRightJustified) {
				yPos = panel.getHeight() / 2;

			} else {
				yPos = (int) offscreenSize.getHeight() - panel.getHeight() / 2;
			}
		}

		return yPos;
	}

	/**
	 * Sets a dynamic font size based on current scale of the dependent
	 * axis map.
	 */
	private void adaptFontSizeToMapScale() {
		int newPoints = (int) map.getScale() - SQUEEZE;
		if(newPoints > getMax()) {
			newPoints = getMax();
		} else if(newPoints < getMin()) {
			newPoints = getMin();
		}

		if(!isFixed && newPoints != getPoints()) {
			if(debug) {
				LogBuffer.println("Adapting to new font size from [" +
				                  getPoints() + "] to [" + newPoints +
				                  "] for [" +
				                  (isGeneAxis ? "rows" : "cols") + "]");
			}
			setSavedPoints(newPoints);
		}
	}

	/**
	 * Sets a dynamic font size based on current scale of the dependent
	 * axis map.
	 */
	private void adaptTempFontSizeToMapScale(int newPoints) {
		if(newPoints < 1) {
			newPoints = 1;
		}
		if(debug) {
			LogBuffer.println("Adapting to new font size from [" + getPoints() +
			                  "] to [" + newPoints + "] for [" +
			                  (isGeneAxis ? "rows" : "cols") + "]");
		}
		setFont(new Font(face,style,newPoints));
	}

	@Override
	public void mouseDragged(final MouseEvent e) {}

	protected abstract void    setSecondaryScrollBarPolicyAsNeeded();
	protected abstract void    setSecondaryScrollBarPolicyAlways();
	protected abstract void    setSecondaryScrollBarPolicyNever();
	protected abstract int     getPrimaryHoverPosition(final MouseEvent e);
	protected abstract boolean isJustifiedToMatrixEdge();

	@Override
	public void mouseMoved(final MouseEvent e) {
		hoverIndex = map.getIndex(getPrimaryHoverPosition(e));
		repaint();
	}

	public int getPrimaryHoverIndex(final MouseEvent e) {
		return(map.getIndex(getPrimaryHoverPosition(e)));
	}

	public boolean doDrawLabelPort() {
		return(inLabelPortMode() && map.overALabelPortLinkedView() &&
		       (!isFixed && map.getScale() < getMin() + SQUEEZE || isFixed &&
		        map.getScale() < last_size));
	}

	@Override
	public void mouseEntered(final MouseEvent e) {}

	@Override
	public void mouseExited(final MouseEvent e) {
		hoverIndex = -1;
		repaint();
	}

	public void resetSecondaryScroll() {
		LogBuffer.println("Resetting last secondary scroll position values");
		lastScrollRowPos    = -1;
		lastScrollRowEndGap = -1;
		lastScrollRowEndPos = -1;
		lastScrollColPos    = -1;
		lastScrollColEndGap = -1;
		lastScrollColEndPos = -1;
	}

	public void adjustSecondaryScroll() {
		LogBuffer.println("Resetting last secondary scroll position values");
		if(isGeneAxis) {
			//Do not adjust if the scrollbars are not properly set
			if(lastScrollRowPos    == -1 ||
			   lastScrollRowEndPos == -1 ||
			   lastScrollRowEndGap == -1) {
				return;
			}
			if(isRightJustified) {
				lastScrollRowPos =
					lastScrollRowEndPos -
				    getSecondaryScrollBar().getModel().getExtent();
			} else {
				lastScrollRowEndPos =
					lastScrollRowPos +
					getSecondaryScrollBar().getModel().getExtent();
				lastScrollRowEndGap = getSecondaryScrollBar().getMaximum() -
				                      lastScrollRowEndPos;
			}
		} else {
			//Do not adjust if the scrollbars are not properly set
			if(lastScrollColPos == -1 || lastScrollColEndPos == -1 ||
			   lastScrollColEndGap == -1) { return; }
			if(isRightJustified) {
				LogBuffer.println("Adjusting columns for bottom justification");
				lastScrollColEndPos = lastScrollColPos +
					                  getSecondaryScrollBar().getModel().getExtent();
				lastScrollColEndGap = getSecondaryScrollBar().getMaximum() -
				                      lastScrollColEndPos;
			} else {
				LogBuffer.println("Adjusting columns for top justification");
				lastScrollColPos = lastScrollColEndPos -
				                   getSecondaryScrollBar().getModel().getExtent();
			}
		}
	}

	public boolean secondaryPaneSizeChanged() {
		boolean changed = false;
		if(isGeneAxis) {
			if(scrollPane.getViewport().getSize().width != rowLabelPaneSize) {
				changed = true;
				rowLabelPaneSize = scrollPane.getViewport().getSize().width;
			}
		} else {
			if(scrollPane.getViewport().getSize().height != colLabelPaneSize) {
				changed = true;
				colLabelPaneSize = scrollPane.getViewport().getSize().height;
			}
		}
		return changed;
	}
}
