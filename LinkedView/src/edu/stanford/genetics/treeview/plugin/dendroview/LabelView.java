package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.util.Observable;
import java.util.prefs.Preferences;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

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
MouseMotionListener, FontSelectable, ConfigNodePersistent {

	private static final long serialVersionUID = 1L;

	/* Axis IDs */
	protected final static int ROW = 0;
	protected final static int COL = 1;
	
	protected final static int HINTFONTSIZE = 14; 
	//The user sets the lower bound in the prefs
	//protected final static double LOWER_BOUND = 10.0;

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
	int matrixBarThickness = 7; //Thickness of the matrix position bar - Must be an odd number! (so red hover line is centered)
	int labelBarThickness = 7; //Must be an odd number! (so red hover line is centered)
	int labelIndicatorProtrusion = 3; //Distance label hover indicator protrudes from label bar - point on indicator
	int matrixIndicatorIntrusion = (labelIndicatorProtrusion > matrixBarThickness ? (int) Math.ceil(matrixBarThickness / 2) : labelIndicatorProtrusion); //Point on indicator
	int labelIndent = 3; //Distance label text starts after the end of the label indicator
	int indicatorThickness = matrixBarThickness + labelBarThickness + labelIndicatorProtrusion + labelIndent; //Should be assigned a value summing the following values if drawing a label port
	int lastScrollRowEndGap = -1;
	int lastScrollRowPos = -1;
	int lastScrollRowEndPos = -1;
	int lastScrollColEndGap = -1;
	int lastScrollColPos = -1;
	int lastScrollColEndPos = -1;
	int rowLabelPaneSize = -1;
	int colLabelPaneSize = -1;

	protected boolean debug = false;

	public LabelView(final int axis_id) {
		
		super();

		this.isGeneAxis = (axis_id == ROW);
		this.setLayout(new MigLayout());

		final String summary = (isGeneAxis) ? "GeneSummary" : "ArraySummary";
		this.headerSummary = new HeaderSummary(summary);

		// this.urlExtractor = uExtractor;

		addMouseMotionListener(this);
		addMouseListener(this);

		setLabelPortMode(true);

		scrollPane = new JScrollPane(this,
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

		if (map != null) {
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

		if (this.geneSelection != null) {
			this.geneSelection.deleteObserver(this);
		}

		this.geneSelection = geneSelection;
		this.geneSelection.addObserver(this);

		if (isGeneAxis) {
			this.drawSelection = geneSelection;
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

		if (this.arraySelection != null) {
			this.arraySelection.deleteObserver(this);
		}
		this.arraySelection = arraySelection;
		this.arraySelection.addObserver(this);

		if (!isGeneAxis) {
			this.drawSelection = arraySelection;
		}
	}

	@Override
	public void update(final Observable o, final Object arg) {

	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode;
			
		} else {
			LogBuffer.println("parentNode for LabelView was null.");
			return;
		}

		LogBuffer.println("Setting new label configNode");
		importSettingsFromNode(configNode);
	}
	
	public void importSettingsFromNode(Preferences node) {
		
		setMin(node.getInt("min", d_min));
		setMax(node.getInt("max", d_max));
		LogBuffer.println("Setting min font size from preferences to [" + min + "]");
		setFace(node.get("face", d_face));
		setStyle(node.getInt("style", d_style));
		setSavedPoints(node.getInt("size", d_size));
		setJustifyOption(node.getBoolean("isRightJustified", d_justified));
		setFixed(node.getBoolean("isFixed", d_fixed));

		/* TODO: I need to catch when the remembered scroll info has been reset when deciding where to draw the indicator - then I can uncomment the line below. This will cause the indicator to be drawn in the correct place when the font settings are changed and it will allow the scroll position to be reset properly. */
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

		if ((face == null) || (!face.equals(string))) {
			face = string;
			if (configNode != null) {
				configNode.put("face", face);
			}
			setFont(new Font(face, style, size));
			resetSecondaryScroll();
			adjustScrollBar();
			repaint();
		}
	}
	
	/**
	 * Wrapper for setPoints which allows to save the newly set points to an
	 * instance variable.
	 * @param i
	 */
	@Override
	public void setSavedPoints(final int i) {
		
		/* Stay within boundaries */
		int new_i = i;
//		if(i > max) {
//			new_i = max;
//			
//		} else if(i < min) {
//			new_i = min;
//		}
		
		last_size = new_i;
		setPoints(new_i);
	}

	@Override
	public void setPoints(final int i) {
		
		if (size != i) {
			size = i;
			if (configNode != null) {
				configNode.putInt("size", size);
			}
			setFont(new Font(face, style, size));
			resetSecondaryScroll();
			adjustScrollBar();
			repaint();
		}
	}

	@Override
	public void setStyle(final int i) {

		if (style != i) {
			style = i;
			if (configNode != null) {
				configNode.putInt("style", style);
			}
			setFont(new Font(face, style, size));
			resetSecondaryScroll();
			adjustScrollBar();
			repaint();
		}
	}
	
	@Override
	public void setMin(final int i) {

		if(debug)
			LogBuffer.println("setMin(" + i + ") called");
		if(i < 1 || (max > 0 && i > max)) {
			return;
		}

		if (min != i) {
			min = i;
			if (configNode != null) {
				LogBuffer.println("Saving new min font size [" + min + "]");
				configNode.putInt("min", min);
			}
			setFont(new Font(face, style, size));
			resetSecondaryScroll();
			adjustScrollBar();
			repaint();
		}
	}
	
	@Override
	public void setMax(final int i) {

		if(debug)
			LogBuffer.println("setMax(" + i + ") called");
		if(i < 1 || (min > 0 && i < min)) {
			return;
		}

		if (max != i) {
			max = i;
			if (configNode != null) {
				configNode.putInt("max", max);
			}
			setFont(new Font(face, style, size));
			resetSecondaryScroll();
			adjustScrollBar();
			repaint();
		}
	}

	@Override
	public void setJustifyOption(final boolean isRightJustified) {

		this.isRightJustified = isRightJustified;

		if (configNode != null) {
			configNode.putBoolean("isRightJustified", isRightJustified);
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
		
		this.isFixed = fixed;

		resetSecondaryScroll();

		if (configNode != null) {
			configNode.putBoolean("isFixed", fixed);
		}
	};

	@Override
	public String viewName() {

		return "LabelView";
	}

	/* inherit description */
	@Override
	public void updateBuffer(final Graphics g) {

		updateBuffer(g, offscreenSize);
	}

	public void updateBuffer(final Image buf) {

		updateBuffer(buf.getGraphics(),
				new Dimension(buf.getWidth(null), buf.getHeight(null)));
	}

	public void updateBuffer(final Graphics g, final Dimension offscreenSize) {

		if(debug)
			LogBuffer.println("Updating the label pane graphics");
		/** TODO: Make sure that the number of visible labels is up to date */

		/* Shouldn't draw if there's no TreeSelection defined */
		if (drawSelection == null) {
			LogBuffer.println("drawSelection not defined. Can't draw labels.");
			return;
		}

		int matrixHoverIndex = map.getHoverIndex();

		final int stringX = (isGeneAxis) ? offscreenSize.width
				: offscreenSize.height;

		/* Get label indices range */
		//final int start = map.getIndex(0);
		//This used to have getUsedPixels(), but on full zoom-out, the resulting
		//data index was 119 instead of 133. I changed it to getAvailablePixels
		//and I got the correct 133 result. I suspect used pixels assumes a
		//scrollbar takes up space...
		//final int end = map.getIndex(map.getAvailablePixels()) - 1;

		final Graphics2D g2d = (Graphics2D) g;
		final AffineTransform orig = g2d.getTransform();
		
		/* Draw labels if zoom level allows it */
		final boolean hasFixedOverlap = isFixed && map.getScale() < last_size;
		boolean resetJustification =
				(isGeneAxis ?
				 scrollPane.getViewport().getSize().width  == offscreenSize.width :
				 scrollPane.getViewport().getSize().height == offscreenSize.height);
		if(resetJustification && debug)
			LogBuffer.println("Resetting justification for " + (isGeneAxis ? "ROW" : "COL") + "s!!!!");

		if ((map.getScale() >= (getMin() + SQUEEZE) && !hasFixedOverlap) ||
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
			if(map.getScale() >= (getMin() + SQUEEZE) && !hasFixedOverlap) {
				//Update the number of visible labels to equal the number of
				//visible squares in the matrix
				map.setNumVisibleLabels(map.getNumVisible());
			} else {
				map.setNumVisibleLabels((int) Math.floor(
						map.getAvailablePixels() / (curFontSize + SQUEEZE)));
			}
			//Shift the labels in case the number of labels in the label pane
			//necessitates it (this only affects the stored values in map for
			//firstVisibleLabel and numVisibleLabels)
			map.pullLabels();

			boolean drawLabelPort = doDrawLabelPort();
			if(drawLabelPort && debug)
				LogBuffer.println("Drawing a label port");

			//Drawing only visible labels
			final int start  = map.getFirstVisibleLabel();
			final int end    = map.getFirstVisibleLabel() +
					map.getNumVisibleLabels() - 1;
			//offset = abs(pixel index of data index 0 (could be negative))
			//final int offset = Math.abs(map.getPixel(0));
			final int primaryScrollPos =
					map.getFirstVisibleLabel() * (curFontSize + SQUEEZE);
			final int secondaryScrollPos =
					getSecondaryScrollBar().getValue();
			final int secondaryScrollEndGap =
					getSecondaryScrollBar().getMaximum() - (secondaryScrollPos +
							getSecondaryScrollBar().getModel().getExtent());
			int secondaryScrollEndPos = secondaryScrollPos + getSecondaryScrollBar().getModel().getExtent();
			if(debug)
				LogBuffer.println("Secondary scroll Pos: [" + secondaryScrollPos + "] Secondary scroll end gap: [" + secondaryScrollEndGap + "].");

			/* Rotate plane for array axis (not for zoomHint) */
			if (!isGeneAxis) {
				g2d.rotate(Math.PI * 3 / 2);
				g2d.translate(-offscreenSize.height, 0);
			}

			final int colorIndex = headerInfo.getIndex("FGCOLOR");
			g.setFont(new Font(face, style, size));

			final FontMetrics metrics = getFontMetrics(g.getFont());
			final int ascent = metrics.getAscent();

			if(debug)
				LogBuffer.println("Label port mode is " + (drawLabelPort ? "" : "in") + "active.  Map square size: [" + map.getScale() + "] Font size: [" + curFontSize + "] SQUEEZE: [" + SQUEEZE + "] ascent: [" + ascent + "]");

			/* Draw label backgrounds first if color is defined */
			final int bgColorIndex = headerInfo.getIndex("BGCOLOR");
			Color bgColor = g.getColor();
			if (bgColorIndex > 0) {
				if(debug)
					LogBuffer.println("BgLabel");
				final Color back = g.getColor();
				int stackPos = start * (curFontSize + SQUEEZE);
				for (int j = start; j <= end; j++) {
					final String[] strings = headerInfo.getHeader(j);
					try {
						g.setColor(TreeColorer.getColor(strings[bgColorIndex]));

					} catch (final Exception e) {
						// ignore...
					}
					g.fillRect(0,
							   (drawLabelPort ?
								stackPos + ascent / 2 :
								map.getMiddlePixel(j) - ascent / 2),
							   stringX,
							   ascent);
					stackPos += curFontSize + SQUEEZE;
				}
				g.setColor(back);
			}

			/* Draw the labels */
			int maxStrLen = getJustifiedPosition(metrics);
			final Color fore = GUIFactory.MAIN;
			final Color labelPortColor = new Color(30,144,251);
			int stackPos = start * (curFontSize + SQUEEZE);
			for (int j = start; j <= end; j++) {

				try {
					String out = headerSummary.getSummary(headerInfo, j);
					final String[] headers = headerInfo.getHeader(j);

					if (out == null) {
						out = "No Label";
					}

					/* Set label color */
					if (drawSelection.isIndexSelected(j) || j == hoverIndex || j == matrixHoverIndex) {
						if (colorIndex > 0) {
							g.setColor(TreeColorer
									.getColor(headers[colorIndex]));
						}

						if(j == hoverIndex || j == matrixHoverIndex) {
							g2d.setColor(Color.red);
						} else {
							g2d.setColor(fore);
						}

						if (colorIndex > 0) {
							g.setColor(fore);
						}

					} else {
						g2d.setColor(Color.black);
					}

//					if(maxStrLen < metrics.stringWidth(out)) {
//						maxStrLen = metrics.stringWidth(out);
//					}
					/* Finally draw label (alignment-dependent) */
					int xPos = 0;
					if (isRightJustified) {
						xPos = stringX - metrics.stringWidth(out);
					}

//					if(debug)
//						LogBuffer.println("Drawing " + (isGeneAxis ? "ROW" : "COL") + " label at X pixel [" +
//								(xPos + (drawLabelPort ? indicatorThickness : labelIndent) * (isGeneAxis ? -1 : 1)) +
//								"] xPos [" +
//								xPos + "] indicatorThickness [" + indicatorThickness * (isGeneAxis ? -1 : 1) + "].");
					g2d.drawString(out,
								   //This [(isGeneAxis == isRightJustified)] means: both are false or both are true
								   xPos + ((isGeneAxis == isRightJustified) ? (drawLabelPort ? indicatorThickness : labelIndent) * (isGeneAxis ? -1 : 1) : 0),
								   (drawLabelPort ?
									stackPos + (int) Math.round((curFontSize + SQUEEZE) / 2) + (int) Math.floor(ascent / 2) - 1 :
									map.getMiddlePixel(j) + ascent / 2));

				} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
					LogBuffer.logException(e);
					break;
				}
				stackPos += curFontSize + SQUEEZE;
			}
			if(debug)
				LogBuffer.println((isGeneAxis ? "ROW" : "COL") +
						": MaxStrLen: [" + maxStrLen + "]Start Index: [" +
						start + "] End Index: [" + end + "] height [" +
						offscreenSize.height + "] width [" +
						offscreenSize.width + "]");

			/* Draw the "position indicator" if the label port is active */
			//See variables initialized above the string drawing section
			if(drawLabelPort) {
				if(!isGeneAxis) {
					if(isRightJustified) {
						LogBuffer.println("Resetting justification via drawing for rows!  Will draw at maxstrlen + indicatorthickness - lastendgap [" + maxStrLen + " + " + indicatorThickness + " - " + lastScrollRowEndGap + " = " + (maxStrLen + indicatorThickness - lastScrollRowEndGap) + "] - indicatorThickness [" + indicatorThickness + "] instead of current [" + secondaryScrollPos + "] - indicatorThickness [" + indicatorThickness + "]. MaxStrLen: [" + maxStrLen + "]");
						secondaryScrollEndPos = maxStrLen + indicatorThickness - lastScrollRowEndGap;
					} else {
						LogBuffer.println("Resetting justification via drawing for rows!  Will draw at lastScrollRowEndPos [" + lastScrollRowEndPos + "] instead of current [" + secondaryScrollEndPos + "] - indicatorThickness [" + indicatorThickness + "]. MaxStrLen: [" + maxStrLen + "]");
						secondaryScrollEndPos = (lastScrollRowEndPos == -1 ? scrollPane.getViewport().getSize().width : lastScrollRowEndPos);
					}

					//Switch to the background color
					g2d.setColor(bgColor);
					//Draw a background box to blank-out and partially scrolled labels
					g.fillRect(
							secondaryScrollEndGap,
							start * (curFontSize + SQUEEZE) - 1,
							indicatorThickness,
							(end + 1) * (curFontSize + SQUEEZE));
					//Switch to the label port color
					g2d.setColor(labelPortColor);
					//Draw the first border line
					g2d.drawLine(
							secondaryScrollEndGap + indicatorThickness -
								(labelIndent + labelIndicatorProtrusion),
							start * (curFontSize + SQUEEZE),
							indicatorThickness + maxStrLen,
							start * (curFontSize + SQUEEZE));
					//Draw the second border line
					g2d.drawLine(
							secondaryScrollEndGap + indicatorThickness -
								(labelIndent + labelIndicatorProtrusion),
							(end + 1) * (curFontSize + SQUEEZE) - 1,
							indicatorThickness + maxStrLen,
							(end + 1) * (curFontSize + SQUEEZE) - 1);
					//Draw the label breadth bar
					g.fillRect(
							secondaryScrollEndGap + matrixBarThickness,
							start * (curFontSize + SQUEEZE),
							labelBarThickness,
							(end + 1) * (curFontSize + SQUEEZE) - start * (curFontSize + SQUEEZE) + 1);
					//Draw the matrix breadth bar
					g.fillRect(
							secondaryScrollEndGap,
							start * (curFontSize + SQUEEZE) + map.getPixel(start),
							matrixBarThickness,
							map.getPixel(end + 1) - map.getPixel(start) + 1);
					//If there is a data index that is hovered over
					int activeHoverDataIndex =
							(hoverIndex == -1 ? matrixHoverIndex : hoverIndex);
					if(activeHoverDataIndex > -1) {
						//Change to the hover color
						g2d.setColor(Color.red);
						//Draw the hover matrix position indicator
						int matrixPixel = start * (curFontSize + SQUEEZE) + map.getPixel(activeHoverDataIndex);
						LogBuffer.println("Hover matrix start: [" + start + "] activeHoverIndex: [" + activeHoverDataIndex + "]");
						int labelPixel = activeHoverDataIndex * (curFontSize + SQUEEZE);
						boolean outOfBounds = activeHoverDataIndex > end || activeHoverDataIndex < start;
						boolean drawAMatrixPoint = ((int) Math.round(map.getScale()) > 2 && !outOfBounds);
						if(drawAMatrixPoint) {
							//Draw the base of the indicator
							g.fillRect(
									secondaryScrollEndGap + matrixIndicatorIntrusion,
									matrixPixel,
									matrixBarThickness - matrixIndicatorIntrusion + 1,
									(int) Math.round(map.getScale()));
							//Draw the point of the indicator
							//Do a 4 point polygon if there's an even number of pixels in order to have a stubby but symmetrical point
							int baseWidth = (int) Math.floor(map.getScale());
							int center = matrixPixel + (int) Math.ceil((int) Math.floor(map.getScale()) / 2);
							if(baseWidth % 2 == 1) {
								int[] exes = {secondaryScrollEndGap + matrixIndicatorIntrusion,secondaryScrollEndGap + matrixIndicatorIntrusion,secondaryScrollEndGap};
								int[] whys = {matrixPixel,matrixPixel + (int) Math.ceil(map.getScale()) - 1,center};
								//g2d.setColor(Color.green);
								g.fillPolygon(exes, whys, 3);
							} else {
								int[] exes = {secondaryScrollEndGap + matrixIndicatorIntrusion,secondaryScrollEndGap + matrixIndicatorIntrusion,secondaryScrollEndGap,secondaryScrollEndGap};
								int[] whys = {matrixPixel,matrixPixel + (int) Math.ceil(map.getScale()) - 1,center - 1,center};
								//g2d.setColor(Color.black);
								g.fillPolygon(exes, whys, 4);
							}
							//g2d.setColor(Color.red);
						} else {
							g.fillRect(
									secondaryScrollEndGap,
									matrixPixel,
									(outOfBounds ?
											(int) Math.ceil(matrixBarThickness / 2) + 1 : matrixBarThickness + 1),
									(int) Math.round(map.getScale()));
						}
						if(!outOfBounds) {
							//Draw the hover label position indicator
//							g.fillRect(
//									secondaryScrollEndGap + matrixBarThickness + (int) Math.floor(labelBarThickness / 2),
//									labelPixel,
//									(int) Math.ceil(labelBarThickness / 2) + labelIndicatorProtrusion + 1,
//									curFontSize + SQUEEZE);
							//Draw the base of the indicator
							g.fillRect(
									secondaryScrollEndGap + matrixBarThickness,
									labelPixel,
									labelBarThickness + 1,
									curFontSize + SQUEEZE);
							//Draw the point of the indicator
							//Do a 4 point polygon if there's an even number of pixels in order to have a stubby but symmetrical point
							if((curFontSize + SQUEEZE) % 2 == 1) {
								//g2d.setColor(Color.gray);
								int[] exes = {secondaryScrollEndGap + matrixBarThickness + labelBarThickness,secondaryScrollEndGap + matrixBarThickness + labelBarThickness,secondaryScrollEndGap + matrixBarThickness + labelBarThickness + labelIndicatorProtrusion + 1};
								int[] whys = {labelPixel - 1,labelPixel + curFontSize + SQUEEZE,labelPixel + (int) Math.ceil((curFontSize + SQUEEZE) / 2)};
								g.fillPolygon(exes, whys, 3);
							} else {
								//g2d.setColor(Color.green);
								int[] exes = {secondaryScrollEndGap + matrixBarThickness + labelBarThickness,secondaryScrollEndGap + matrixBarThickness + labelBarThickness,secondaryScrollEndGap + matrixBarThickness + labelBarThickness + labelIndicatorProtrusion,secondaryScrollEndGap + matrixBarThickness + labelBarThickness + labelIndicatorProtrusion};
								int[] whys = {labelPixel - 1,labelPixel + curFontSize + SQUEEZE,labelPixel + (int) (curFontSize + SQUEEZE) / 2 - 1,labelPixel + (int) (curFontSize + SQUEEZE) / 2};
								g.fillPolygon(exes, whys, 4);
							}
							//g2d.setColor(Color.red);
						}
						//Draw the connection btwn hvr label & matrix pos indicators
						g2d.drawLine(
								secondaryScrollEndGap + (outOfBounds ? (int) Math.floor(matrixBarThickness / 2) : matrixBarThickness) + 1,
								matrixPixel,
								secondaryScrollEndGap + (outOfBounds ? (int) Math.floor(matrixBarThickness / 2) : matrixBarThickness) + 1,
								(outOfBounds ? (activeHoverDataIndex > end ? (end + 1) * (curFontSize + SQUEEZE) - 1 : 0) : labelPixel));
						if(activeHoverDataIndex > end) {
							//Draw an arrow off the right side
							g2d.drawLine(secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2),
										 (end + 1) * (curFontSize + SQUEEZE) - 2,
										 secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2) + 2,
										 (end + 1) * (curFontSize + SQUEEZE) - 2);
							g2d.drawLine(secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2) - 1,
									 (end + 1) * (curFontSize + SQUEEZE) - 3,
									 secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2) + 3,
									 (end + 1) * (curFontSize + SQUEEZE) - 3);
						} else if(activeHoverDataIndex < start) {
							//Draw an arrow off the left side
							g2d.drawLine(secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2),
										 start * (curFontSize + SQUEEZE) + 1,
										 secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2) + 2,
										 start * (curFontSize + SQUEEZE) + 1);
							g2d.drawLine(secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2) - 1,
									 start * (curFontSize + SQUEEZE) + 2,
									 secondaryScrollEndGap + (int) Math.floor(matrixBarThickness / 2) + 3,
									 start * (curFontSize + SQUEEZE) + 2);
						}
					}
				} else {
					//int secondaryScrollEndPos = secondaryScrollPos + getSecondaryScrollBar().getModel().getExtent();
					if(isRightJustified) {
						LogBuffer.println("Resetting justification via drawing for rows!  Will draw at maxstrlen + indicatorthickness - lastendgap [" + maxStrLen + " + " + indicatorThickness + " - " + lastScrollRowEndGap + " = " + (maxStrLen + indicatorThickness - lastScrollRowEndGap) + "] - indicatorThickness [" + indicatorThickness + "] instead of current [" + secondaryScrollPos + "] - indicatorThickness [" + indicatorThickness + "]. MaxStrLen: [" + maxStrLen + "]");
						secondaryScrollEndPos = maxStrLen + indicatorThickness - lastScrollRowEndGap;
					} else {
						LogBuffer.println("Resetting justification via drawing for rows!  Will draw at lastScrollRowEndPos [" + (lastScrollRowEndPos == -1 ? scrollPane.getViewport().getSize().width : lastScrollRowEndPos) + "] instead of current [" + secondaryScrollEndPos + "] - indicatorThickness [" + indicatorThickness + "]. MaxStrLen: [" + maxStrLen + "]");
						secondaryScrollEndPos = (lastScrollRowEndPos == -1 ? scrollPane.getViewport().getSize().width : lastScrollRowEndPos);
					}

					//Switch to the background color
					g2d.setColor(bgColor);
					//resetJustification = (secondaryScrollPos == 0 && secondaryScrollEndGap == 0);
					//Draw a background box to blank-out and partially scrolled labels
					LogBuffer.println("Starting indicator drawing at position [" + (secondaryScrollEndPos - indicatorThickness) + "]");
					g.fillRect(
							secondaryScrollEndPos - indicatorThickness,
							start * (curFontSize + SQUEEZE) - 1,
							indicatorThickness,
							(end + 1) * (curFontSize + SQUEEZE));
					//Switch to the label port color
					g2d.setColor(labelPortColor);
					//Draw the first border line
					g2d.drawLine(
							secondaryScrollEndPos - indicatorThickness +
								(labelIndent + labelIndicatorProtrusion),
							start * (curFontSize + SQUEEZE),
							0,
							start * (curFontSize + SQUEEZE));
					//Draw the second border line
					g2d.drawLine(
							secondaryScrollEndPos - indicatorThickness +
								(labelIndent + labelIndicatorProtrusion),
							(end + 1) * (curFontSize + SQUEEZE) - 1,
							0,
							(end + 1) * (curFontSize + SQUEEZE) - 1);
					//Draw the label breadth bar
					g.fillRect(
							secondaryScrollEndPos - matrixBarThickness - labelBarThickness,
							start * (curFontSize + SQUEEZE),
							labelBarThickness,
							(end + 1) * (curFontSize + SQUEEZE) - start * (curFontSize + SQUEEZE));
					//Draw the matrix breadth bar
					g.fillRect(
							secondaryScrollEndPos - matrixBarThickness,
							start * (curFontSize + SQUEEZE) + map.getPixel(start),
							matrixBarThickness,
							map.getPixel(end + 1) - map.getPixel(start) + 1);
					//If there is a data index that is hovered over
					int activeHoverDataIndex =
							(hoverIndex == -1 ? matrixHoverIndex : hoverIndex);
					if(activeHoverDataIndex > -1) {
						//Change to the hover color
						g2d.setColor(Color.red);
						//Draw the hover matrix position indicator
						int matrixPixel = start * (curFontSize + SQUEEZE) + map.getPixel(activeHoverDataIndex);
						int labelPixel = activeHoverDataIndex * (curFontSize + SQUEEZE);
						boolean outOfBounds = activeHoverDataIndex > end || activeHoverDataIndex < start;
						g.fillRect(
								secondaryScrollEndPos - (outOfBounds ?
										(int) Math.ceil(matrixBarThickness / 2) : matrixBarThickness) - 1,
								matrixPixel,
								(outOfBounds ?
										(int) Math.ceil(matrixBarThickness / 2) : matrixBarThickness) + 1,
								(int) Math.round(map.getScale()));
						if(!outOfBounds) {
							//Draw the hover label position indicator
							g.fillRect(
									secondaryScrollEndPos - indicatorThickness + labelIndent,
									//secondaryScrollPos + matrixBarThickness + (int) Math.floor(labelBarThickness / 2),
									labelPixel,
									labelBarThickness + labelIndicatorProtrusion,
									curFontSize + SQUEEZE);
						}
						//Draw the connection btwn hvr label & matrix pos indicators
						g2d.drawLine(
								secondaryScrollEndPos - (outOfBounds ? (int) Math.floor(matrixBarThickness / 2) : matrixBarThickness) - 1,
								matrixPixel,
								secondaryScrollEndPos - (outOfBounds ? (int) Math.floor(matrixBarThickness / 2) : matrixBarThickness) - 1,
								(outOfBounds ? (activeHoverDataIndex > end ? (end + 1) * (curFontSize + SQUEEZE) - 1 : 0) : labelPixel));
						if(activeHoverDataIndex > end) {
							//Draw an arrow off the right side
							g2d.drawLine(secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2),
										 (end + 1) * (curFontSize + SQUEEZE) - 2,
										 secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2) - 2,
										 (end + 1) * (curFontSize + SQUEEZE) - 2);
							g2d.drawLine(secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2) + 1,
									 (end + 1) * (curFontSize + SQUEEZE) - 3,
									 secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2) - 3,
									 (end + 1) * (curFontSize + SQUEEZE) - 3);
						} else if(activeHoverDataIndex < start) {
							//Draw an arrow off the left side
							g2d.drawLine(secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2),
										 start * (curFontSize + SQUEEZE) + 1,
										 secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2) - 2,
										 start * (curFontSize + SQUEEZE) + 1);
							g2d.drawLine(secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2) + 1,
									 start * (curFontSize + SQUEEZE) + 2,
									 secondaryScrollEndPos - (int) Math.floor(matrixBarThickness / 2) - 3,
									 start * (curFontSize + SQUEEZE) + 2);
						}
					}
				}
			}

			g2d.setTransform(orig);

			/* Set the Pane size (for the new label length and the length of the
			 * label port (if active - otherwise set it to visible pane
			 * width)) */
			int labelStackSize = (map.getMaxIndex() + 1) * (curFontSize + SQUEEZE);
			//Add a bit to the end of the label stack size so that when you're scrolled all the way to the right/bottom, things are lined up on the left/top
			//Basically, add the height/width of the pane modulus the font height
			labelStackSize += (isGeneAxis ? scrollPane.getViewport().getSize().width : scrollPane.getViewport().getSize().height) % (curFontSize + SQUEEZE);
			int matrixSize = ((map.getPixel(map.getMaxIndex() + 1) - 1) - map.getPixel(0));
			int fullPaneSize = 0;
			if(!(drawLabelPort)) {
				fullPaneSize = matrixSize;
			} else {
				fullPaneSize = labelStackSize;
			}
			//Set the size of the scrollpane to match the longest string
			if(isGeneAxis) {
				//+ 15 allows for the appearance & disappearance of the main
				//scrollbar, which I believe is only for Macs since macs draw
				//the scrollbar over the content when set AS_NEEDED
//				setPreferredSize(new Dimension((getSecondaryScrollBar().isVisible() ? maxStrLen + 15 : maxStrLen),
//						offscreenSize.height));
				setPreferredSize(new Dimension(maxStrLen + (drawLabelPort ? indicatorThickness : 0),
						fullPaneSize));
				//if(debug)
					LogBuffer.println("Resizing row labels panel to [" + (maxStrLen + (drawLabelPort ? indicatorThickness : 0)) + "x" + scrollPane.getViewport().getSize().height + "].");
			} else {
				//+ 15 allows for the appearance & disappearance of the main
				//scrollbar, which I believe is only for Macs since macs draw
				//the scrollbar over the content when set AS_NEEDED
//				setPreferredSize(new Dimension(offscreenSize.width,
//						(getSecondaryScrollBar().isVisible() ? maxStrLen + 15 : maxStrLen)));
				setPreferredSize(new Dimension(fullPaneSize,
						maxStrLen + (drawLabelPort ? indicatorThickness : 0)));
				//if(debug)
					LogBuffer.println("Resizing col labels panel to [" + scrollPane.getViewport().getSize().width + "x" + (maxStrLen + (drawLabelPort ? indicatorThickness : 0)) + "].");
			}

			/* Scroll to the position that matches the matrix */
			getPrimaryScrollBar().setValue(primaryScrollPos);
			/* Scroll to the position that is equivalent to the previous position */
			if(isRightJustified) {
//				getSecondaryScrollBar().setValue(maxStrLen +
//						//Make space for the "position indicator"
//						(drawLabelPort ? indicatorThickness : 0));
				if(isGeneAxis) {
					if(lastScrollRowPos == -1) {
						LogBuffer.println("Scrolling to [" + (getSecondaryScrollBar().getMaximum() - getSecondaryScrollBar().getModel().getExtent()) + "] max - extent [" + getSecondaryScrollBar().getMaximum() + " - " + getSecondaryScrollBar().getModel().getExtent() + "] after drawing - first time rows right justified");
						//getSecondaryScrollBar().setValue(getSecondaryScrollBar().getMaximum() - getSecondaryScrollBar().getModel().getExtent());
						//It seems that the scrollbar max and extent are not updated in this scenario when the app first opens a file, so we will calculate the initial scroll position thusly
						int tmpscrollpos = maxStrLen + (drawLabelPort ? indicatorThickness : 0) - scrollPane.getViewport().getSize().width;
						getSecondaryScrollBar().setValue(tmpscrollpos);
						//lastScrollRowPos = getSecondaryScrollBar().getMaximum() - getSecondaryScrollBar().getModel().getExtent();
						lastScrollRowPos = tmpscrollpos;
						//lastScrollRowEndPos = lastScrollRowPos + getSecondaryScrollBar().getModel().getExtent();
						lastScrollRowEndPos = maxStrLen + (drawLabelPort ? indicatorThickness : 0);
						//lastScrollRowEndGap = getSecondaryScrollBar().getMaximum() - lastScrollRowEndPos;
						lastScrollRowEndGap = 0;
					} else {
						LogBuffer.println("Scrolling to [" + lastScrollRowPos + "] after drawing - rememberred rows right justified");
						getSecondaryScrollBar().setValue(lastScrollRowPos);
					}
				} else {
					/* TODO: Save column scroll position - below is copied from row logic above - edit it! */
					if(lastScrollColPos == -1) {
						LogBuffer.println("Scrolling to [0] after drawing - first time cols left justified");
						getSecondaryScrollBar().setValue(0);
						//It seems that the scrollbar max and extent are not updated in this scenario when the app first opens a file, so we will calculate the initial scroll position thusly
						lastScrollColPos = 0;
						//lastScrollColEndPos = getSecondaryScrollBar().getModel().getExtent();
						lastScrollColEndPos = scrollPane.getViewport().getSize().width;
						//lastScrollColEndGap = getSecondaryScrollBar().getMaximum() - lastScrollColEndPos;
						lastScrollColEndGap = maxStrLen + (drawLabelPort ? indicatorThickness : 0) - scrollPane.getViewport().getSize().width;
					} else {
						LogBuffer.println("Scrolling to [" + lastScrollColPos + "] after drawing - rememberred cols right justified");
						getSecondaryScrollBar().setValue(lastScrollColPos);
					}
				}
			} else {
				if(isGeneAxis) {
					if(lastScrollRowPos == -1) {
						LogBuffer.println("Scrolling to [0] after drawing - first time rows left justified");
						getSecondaryScrollBar().setValue(0);
						//It seems that the scrollbar max and extent are not updated in this scenario when the app first opens a file, so we will calculate the initial scroll position thusly
						lastScrollRowPos = 0;
						//lastScrollRowEndPos = getSecondaryScrollBar().getModel().getExtent();
						lastScrollRowEndPos = scrollPane.getViewport().getSize().width;
						//lastScrollRowEndGap = getSecondaryScrollBar().getMaximum() - lastScrollRowEndPos;
						lastScrollRowEndGap = maxStrLen + (drawLabelPort ? indicatorThickness : 0) - scrollPane.getViewport().getSize().width;
					} else {
						LogBuffer.println("Scrolling to [" + lastScrollRowPos + "] after drawing - rememberred rows left justified");
						getSecondaryScrollBar().setValue(lastScrollRowPos);
					}
				} else {
					/* TODO: Save column scroll position - below is copied from row logic above - edit it! */
					if(lastScrollColPos == -1) {
						LogBuffer.println("Scrolling to [" + (getSecondaryScrollBar().getMaximum() - getSecondaryScrollBar().getModel().getExtent()) + "] max - extent [" + getSecondaryScrollBar().getMaximum() + " - " + getSecondaryScrollBar().getModel().getExtent() + "] after drawing - first time cols right justified");
						//getSecondaryScrollBar().setValue(getSecondaryScrollBar().getMaximum() - getSecondaryScrollBar().getModel().getExtent());
						//It seems that the scrollbar max and extent are not updated in this scenario when the app first opens a file, so we will calculate the initial scroll position thusly
						int tmpscrollpos = maxStrLen + (drawLabelPort ? indicatorThickness : 0) - scrollPane.getViewport().getSize().height;
						getSecondaryScrollBar().setValue(tmpscrollpos);
						//lastScrollColPos = getSecondaryScrollBar().getMaximum() - getSecondaryScrollBar().getModel().getExtent();
						lastScrollColPos = tmpscrollpos;
						//lastScrollColEndPos = lastScrollColPos + getSecondaryScrollBar().getModel().getExtent();
						lastScrollColEndPos = maxStrLen + (drawLabelPort ? indicatorThickness : 0);
						//lastScrollColEndGap = getSecondaryScrollBar().getMaximum() - lastScrollColEndPos;
						lastScrollColEndGap = 0;
					} else {
						LogBuffer.println("Scrolling to [" + lastScrollColPos + "] after drawing - rememberred cols left justified");
						getSecondaryScrollBar().setValue(lastScrollColPos);
					}
				}
			}

//			lastScrollRowEndGap = secondaryScrollEndGap;
//			lastScrollRowPos = secondaryScrollPos;
//			lastScrollRowEndPos = secondaryScrollEndPos;

			//if(getSecondaryScrollBar().getMaximum() != maxStrLen) {
			//	getSecondaryScrollBar().setMaximum(maxStrLen);
			//	scrollPane.revalidate();
			//}
		} else {
			setPoints(HINTFONTSIZE);
			g2d.setColor(Color.black);

			int xPos = getHintX(g2d, stringX);
			int yPos = getHintY(g2d);
			final FontMetrics metrics = getFontMetrics(g2d.getFont());
			boolean isBeginJustified;
			int scrollSize;

			if (isGeneAxis) {
				//Reduce the size of the scrollpane to just what is visible
				setPreferredSize(
						new Dimension(scrollPane.getViewport().getSize().width,
								scrollPane.getViewport().getSize().height));

				isBeginJustified = isRightJustified;
				scrollSize = offscreenSize.width;
				//After rotate, the current width will be the "height" and the
				//zero point we need to calculate from is the bottom left of the
				//panel
				yPos = scrollPane.getViewport().getSize().width / 2;
				//After rotate, this is the distance up from the bottom where to
				//start the hint string
				xPos = ((offscreenSize.height - metrics.stringWidth(zoomHint)) /
						2);
				g2d.rotate(Math.PI * 3 / 2);
				//Need to translate the coordinates to/from? the rotated state
				g2d.translate(-offscreenSize.height, 0);
				if(debug)
					LogBuffer.println("Row Hint position: [" + xPos + "/" + yPos +
						"] stringX: [" + stringX + "] zoomHint: [" + zoomHint +
						"] height [" + offscreenSize.height + "] width [" +
						offscreenSize.width + "] + HintStrLen [" +
						metrics.stringWidth(zoomHint) + "]");

//				if(isRightJustified) {
//					for(int i = offscreenSize.width - yPos;i > 0;i -= yPos * 2) {
//						g2d.drawString(zoomHint, xPos, i);
//					}
//				} else {
//					for(int i = yPos;i < offscreenSize.width;i += yPos * 2) {
//						g2d.drawString(zoomHint, xPos, i);
//					}
//				}
			} else {
				//Reduce the size of the scrollpane to just what is visible
				setPreferredSize(new Dimension(scrollPane.getViewport().getSize().width,
						scrollPane.getViewport().getSize().height));

				isBeginJustified = !isRightJustified;
				scrollSize = offscreenSize.height;
				//offscreenSize.height = scrollPane.getViewport().getSize().height;
				//offscreenSize.setSize(offscreenSize.width, scrollPane.getViewport().getSize().height);
				//setSize(offscreenSize.width,scrollPane.getViewport().getSize().height);
				//scrollPane.getVerticalScrollBar().setMaximum(offscreenSize.height);
				//revalidate();
				//scrollPane.revalidate();
				//scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
				xPos = (offscreenSize.width - metrics.stringWidth(zoomHint)) /
						2;
				yPos = scrollPane.getViewport().getSize().height / 2;
				if(debug)
					LogBuffer.println("Col Hint position: [" + xPos + "/" + yPos +
						"] stringX: [" + stringX + "] zoomHint: [" + zoomHint +
						"] height [" + offscreenSize.height + "] width [" +
						offscreenSize.width + "] + HintStrLen [" +
						metrics.stringWidth(zoomHint) + "]");

//				if(!isRightJustified) {
//					for(int i = offscreenSize.height - yPos;i > 0;i -= yPos * 2) {
//						g2d.drawString(zoomHint, xPos, i);
//					}
//				} else {
//					for(int i = yPos;i < offscreenSize.height;i += yPos * 2) {
//						g2d.drawString(zoomHint, xPos, i);
//					}
//				}
			}

			if(isBeginJustified) {
				for(int i = scrollSize - yPos;i > 0;i -= yPos * 2) {
					g2d.drawString(zoomHint, xPos, i);
				}
			} else {
				for(int i = yPos;i < scrollSize;i += yPos * 2) {
					g2d.drawString(zoomHint, xPos, i);
				}
			}

			//Adding this useless scroll actually causes the pane to update
			//properly - and reflect the settings made above.  Without it, the
			//scrollbar still implies that the panel is its previous size and
			//the hint is off center
			getSecondaryScrollBar().setValue(0);

			//g2d.setColor(Color.black);
			//g2d.drawString(zoomHint, xPos, yPos);
			//scrollPane.revalidate();
			//final FontMetrics metrics = getFontMetrics(g2d.getFont());
			//int maxStrLen = metrics.stringWidth(zoomHint);
			//if(getSecondaryScrollBar().getMaximum() != maxStrLen) {
			//	getSecondaryScrollBar().setMaximum(maxStrLen);
			//	scrollPane.revalidate();
			//}
		}
		//revalidate();
		//scrollPane.revalidate();
	}

	public int getJustifiedPosition(FontMetrics metrics) {
		int min = (isGeneAxis ? scrollPane.getViewport().getSize().width : scrollPane.getViewport().getSize().height) - indicatorThickness;
		int len = getMaxStringLength(metrics);
		return(len > min ? len : min);
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
		if(lastDrawnFace == face && lastDrawnStyle == style &&
		   longest_str_index > -1 && lastDrawnSize == size &&
		   /* TODO: Assuming if the string is the same, it's the same overall data - it would be better to catch when the data changes */
		   longest_str.equals(headerSummary.getSummary(headerInfo,longest_str_index))) {
			LogBuffer.println("Regurgitating maxstrlen");
			if(debug)
				maxStr = headerSummary.getSummary(headerInfo,longest_str_index);
			maxStrLen = longest_str_length;
		} else if(lastDrawnFace == face && lastDrawnStyle == style &&
				  longest_str_index > -1 && lastDrawnSize != size &&
				  /* TODO: Assuming if the string is the same, it's the same overall data - it would be better to catch when the data changes */
				  longest_str.equals(headerSummary.getSummary(headerInfo,longest_str_index))) {
			LogBuffer.println("Refining maxstrlen");
			maxStr = headerSummary.getSummary(headerInfo,longest_str_index);
			maxStrLen = metrics.stringWidth(maxStr);
		} else {
			LogBuffer.println("Calculating maxstrlen because not [lastDrawnFace == face && lastDrawnStyle == style && longest_str_index > -1 && lastDrawnSize != size && longest_str.equals(headerSummary.getSummary(headerInfo,longest_str_index))]");
			LogBuffer.println("Calculating maxstrlen because not [" + lastDrawnFace + " == " + face + " && " + lastDrawnStyle + " == " + style + " && " + longest_str_index + " > -1 && " + lastDrawnSize + " != " + size + " && " + longest_str + ".equals(headerSummary.getSummary(headerInfo," + longest_str_index + "))]");
			for (int j = 0; j <= end; j++) {
				try {
					String out = headerSummary.getSummary(headerInfo, j);
	
					if (out == null) {
						out = "No Label";
					}
	
					if(maxStrLen < metrics.stringWidth(out)) {
						maxStrLen = metrics.stringWidth(out);
						longest_str_index = j;
						if(debug)
							maxStr = out;
					}
	
				} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
					LogBuffer.logException(e);
					break;
				}
			}
			longest_str = maxStr;
		}
		lastDrawnFace  = face;
		lastDrawnStyle = style;
		lastDrawnSize  = size;
		longest_str_length = maxStrLen;
		//if(debug)
			LogBuffer.println((isGeneAxis ? "ROW" : "COL") + ": MaxStrLen: [" +
					maxStrLen + "] MaxStr: [" + maxStr + "] Start Index: [" +
					0 + "] End Index: [" + end + "] height [" +
					offscreenSize.height + "] width [" + offscreenSize.width +
					"]");

		return(maxStrLen);
	}

	/**
	 * Gets x-position for the hint label based on axis and justification.
	 * @param g2d
	 * @param stringX
	 * @return x-position for hint label.
	 */
	private int getHintX(Graphics2D g2d, int stringX) {
		
		int xPos = 0;
		int offSet = 10;
		final FontMetrics metrics = getFontMetrics(g2d.getFont());
			
		if (isGeneAxis) {
			if(isRightJustified) {
				xPos = stringX - metrics.stringWidth(zoomHint) - offSet;
			} else {
				xPos = offSet;
			}
			
		} else {
			xPos = (offscreenSize.width  - metrics.stringWidth(zoomHint)) / 2;
		}
		
		return xPos;
	}
	
	/**
	 * Gets y-position for the hint label based on axis and justification.
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
				yPos = (int) offscreenSize.getHeight() - (panel.getHeight() / 2);
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
			if(debug)
				LogBuffer.println("Adapting to new font size from [" + getPoints() + "] to [" + newPoints + "] for [" + (isGeneAxis ? "rows" : "cols") + "]");
			setSavedPoints(newPoints);
		}
	}

//	@Override
//	public void mouseExited(final MouseEvent e) {
//
//		hoverIndex = -1;
//		repaint();
//	}

	@Override
	public void mouseDragged(final MouseEvent e) {

	}

//	@Override
//	public void mouseMoved(final MouseEvent e) {
//
//	}

	protected abstract void setSecondaryScrollBarPolicyAsNeeded();
	protected abstract void setSecondaryScrollBarPolicyAlways();
	protected abstract void setSecondaryScrollBarPolicyNever();
	protected abstract int getPrimaryHoverPosition(final MouseEvent e);
	protected abstract boolean isJustifiedToMatrixEdge();

//	protected int shiftForScrollbar = 0;
//	protected int shiftedForScrollbar = 0;

	@Override
	public void mouseMoved(final MouseEvent e) {

//		if(shiftForScrollbar > 0) {
//			//Shift the scroll position to accommodate the scrollbar that
//			//appeared (I think this may only be for Macs, according to what I
//			//read.  They draw the scrollbar on top of content when it is set
//			//"AS_NEEDED")
//			getSecondaryScrollBar().setValue(shiftForScrollbar);
//			if(getSecondaryScrollBar().getValue() == shiftForScrollbar) {
//				shiftedForScrollbar = shiftForScrollbar;
//				shiftForScrollbar = 0;
//			}
//		}
		if(doDrawLabelPort()) {
			hoverIndex = (int) Math.floor(getPrimaryHoverPosition(e) / (size + SQUEEZE));
		} else {
			hoverIndex = map.getIndex(getPrimaryHoverPosition(e));
		}
		repaint();
	}

	public int getPrimaryHoverIndex(final MouseEvent e) {
		int hoverIndex;
		if(doDrawLabelPort()) {
			hoverIndex = (int) Math.floor(getPrimaryHoverPosition(e) / (size + SQUEEZE));
		} else {
			hoverIndex = map.getIndex(getPrimaryHoverPosition(e));
		}
		return(hoverIndex);
	}

	public boolean doDrawLabelPort() {
		return(inLabelPortMode() && map.overALabelPortLinkedView() &&
			   ((!isFixed && map.getScale() < (getMin() + SQUEEZE)) ||
			    (isFixed  && map.getScale() < last_size)));
	}

	//@Override
	public void mouseEntered(final MouseEvent e) {
		//This method call is why these mouse functions
		//setSecondaryScrollBarPolicyAsNeeded();
		//scrollPane.setVerticalScrollBarPolicy(
		//		ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
//		int ts = getSecondaryScrollBar().getValue();
//		int tw = getSecondaryScrollBar().getModel().getExtent();
//		int tm = getSecondaryScrollBar().getMaximum();
//		
//		//We do not want to shift the scrollbar if the user has manually left-
//		//justified his/her labels, so only shift to accommodate the scrollbar
//		//when the scroll position is more than half way
//		boolean nearBeginning = (ts < ((tm - tw) / 2));
//		LogBuffer.println("ENTER Setting temp scroll value: [" + ts +
//				"] width [" + tw + "] max [" + tm + "]");
//		if(isJustifiedToMatrixEdge() && !nearBeginning) {
//			LogBuffer.println("Adjusting scrollbar that is positioned near " +
//					"end. Now position: [" + ts + " + 15] New max: [" +
//					getSecondaryScrollBar().getMaximum() + " + 15]");
//			//Width of the vertical scrollbar is 15
//			int newWidth = getSecondaryScrollBar().getMaximum() + 15;
//			getSecondaryScrollBar().setMaximum(newWidth);
//			shiftForScrollbar = ts + 15;
//			getSecondaryScrollBar().setValue(shiftForScrollbar);
//		}
//		LogBuffer.println("ENTER New scroll values: [" +
//				getSecondaryScrollBar().getValue() + "] width [" +
//				getSecondaryScrollBar().getModel().getExtent() + "] max [" +
//				getSecondaryScrollBar().getMaximum() + "]");
	}

	//@Override
	public void mouseExited(final MouseEvent e) {
//		int ts = getSecondaryScrollBar().getValue();
		//setSecondaryScrollBarPolicyNever();
		//scrollPane.setVerticalScrollBarPolicy(
		//		ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
//		LogBuffer.println("EXIT Setting temp scroll value: [" + ts + "]");
//		if(shiftedForScrollbar > 0) {
//			LogBuffer.println("Adjusting scrollbar that is positioned near end.");
//			getSecondaryScrollBar().setValue(ts - 15);
//			shiftedForScrollbar = 0;
//		}
//		scrollPane.revalidate();
//		LogBuffer.println("EXIT New scroll values: [" + getSecondaryScrollBar().getValue() + "] width [" + getSecondaryScrollBar().getModel().getExtent() + "] max [" + getSecondaryScrollBar().getMaximum() + "]");
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
			if(lastScrollRowPos == -1 || lastScrollRowEndPos == -1 || lastScrollRowEndGap == -1) {
				return;
			}
			if(isRightJustified) {
				lastScrollRowPos = lastScrollRowEndPos - getSecondaryScrollBar().getModel().getExtent();
			} else {
				lastScrollRowEndPos = lastScrollRowPos + getSecondaryScrollBar().getModel().getExtent();
				lastScrollRowEndGap = getSecondaryScrollBar().getMaximum() - lastScrollRowEndPos;
			}
		} else {
			//Do not adjust if the scrollbars are not properly set
			if(lastScrollColPos == -1 || lastScrollColEndPos == -1 || lastScrollColEndGap == -1) {
				return;
			}
			if(isRightJustified) {
				LogBuffer.println("Adjusting columns for bottom justification");
				lastScrollColEndPos = lastScrollColPos + getSecondaryScrollBar().getModel().getExtent();
				lastScrollColEndGap = getSecondaryScrollBar().getMaximum() - lastScrollColEndPos;
			} else {
				LogBuffer.println("Adjusting columns for top justification");
				lastScrollColPos = lastScrollColEndPos - getSecondaryScrollBar().getModel().getExtent();
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
		return(changed);
	}
}
