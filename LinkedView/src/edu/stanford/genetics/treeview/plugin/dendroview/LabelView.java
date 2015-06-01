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

	/* Custom label settings */
	protected String face;
	protected int style;
	protected int size;
	protected int min;
	protected int max;
	protected int last_size;
	protected boolean isFixed;

	/* Panel sizing */
	protected int maxlength = 0;

	/* Keeps track of label index with mouse cursor on top */
	protected int hoverIndex;

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

	public LabelView(final int axis_id) {
		
		super();

		this.isGeneAxis = (axis_id == ROW);
		this.setLayout(new MigLayout());

		final String summary = (isGeneAxis) ? "GeneSummary" : "ArraySummary";
		this.headerSummary = new HeaderSummary(summary);

		// this.urlExtractor = uExtractor;

		addMouseMotionListener(this);
		addMouseListener(this);

		scrollPane = new JScrollPane(this,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
		
		setFace(node.get("face", d_face));
		setStyle(node.getInt("style", d_style));
		setSavedPoints(node.getInt("size", d_size));
		setJustifyOption(node.getBoolean("isRightJustified", d_justified));
		setFixed(node.getBoolean("isFixed", d_fixed));

		getHeaderSummary().setConfigNode(node);
	}

	@Override
	public String getFace() {

		return face;
	}

	@Override
	public int getPoints() {

		if(size > 0)
			return size;
		return(d_size);
	}
	
	@Override 
	public int getLastSize() {
		
		return last_size;
	};
	
	@Override
	public int getMin() {

		if(min > 0)
			return min;
		return(d_min);
	}
	
	@Override
	public int getMax() {

		if(max > 0)
			return max;
		return(d_max);
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
		if(i > max) {
			new_i = max;
			
		} else if(i < min) {
			new_i = min;
		}
		
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
			adjustScrollBar();
			repaint();
		}
	}
	
	@Override
	public void setMin(final int i) {

		LogBuffer.println("setMin(" + i + ") called");
		if(i < 1 || i > max) {
			return;
		}

		if (min != i) {
			min = i;
			if (configNode != null) {
				configNode.putInt("min", min);
			}
			setFont(new Font(face, style, size));
			adjustScrollBar();
			repaint();
		}
	}
	
	@Override
	public void setMax(final int i) {

		LogBuffer.println("setMax(" + i + ") called");
		if(i < 1 || i < min) {
			return;
		}

		if (max != i) {
			max = i;
			if (configNode != null) {
				configNode.putInt("max", max);
			}
			setFont(new Font(face, style, size));
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
		/* Shouldn't draw if there's no TreeSelection defined */
		if (drawSelection == null) {
			LogBuffer.println("drawSelection not defined. Can't draw labels.");
			return;
		}

		final int stringX = (isGeneAxis) ? offscreenSize.width
				: offscreenSize.height;

		/* Get label indices range */
		final int start = map.getIndex(0);
		//This used to have getUsedPixels(), but on full zoom-out, the resulting
		//data index was 119 instead of 133. I changed it to getAvailablePixels
		//and I got the correct 133 result. I suspect used pixels assumes a
		//scrollbar takes up space...
		final int end = map.getIndex(map.getAvailablePixels()) - 1;
		
		final Graphics2D g2d = (Graphics2D) g;
		final AffineTransform orig = g2d.getTransform();
		
		/* Draw labels if zoom level allows it */
		final boolean hasFixedOverlap = isFixed && map.getScale() < last_size;
		
		if (map.getScale() >= (getMin() + SQUEEZE) && !hasFixedOverlap) {
			
			if(isFixed) {
				setSavedPoints(last_size);
			} else {
				adaptFontSizeToMapScale();
			} 

			/* Rotate plane for array axis (not for zoomHint) */
			if (!isGeneAxis) {
				g2d.rotate(Math.PI * 3 / 2);
				g2d.translate(-offscreenSize.height, 0);
			}

			final int colorIndex = headerInfo.getIndex("FGCOLOR");
			g.setFont(new Font(face, style, size));

			final FontMetrics metrics = getFontMetrics(g.getFont());
			final int ascent = metrics.getAscent();

			/* Draw label backgrounds first if color is defined */
			final int bgColorIndex = headerInfo.getIndex("BGCOLOR");
			if (bgColorIndex > 0) {
				LogBuffer.println("BgLabel");
				final Color back = g.getColor();
				for (int j = start; j <= end; j++) {
					final String[] strings = headerInfo.getHeader(j);
					try {
						g.setColor(TreeColorer.getColor(strings[bgColorIndex]));

					} catch (final Exception e) {
						// ignore...
					}
					g.fillRect(0, map.getMiddlePixel(j) - ascent / 2, stringX,
							ascent);
				}
				g.setColor(back);
			}

			/* Draw the labels */
			int maxStrLen = 0;
			String maxStr = "";
			final Color fore = GUIFactory.MAIN;
			for (int j = start; j <= end; j++) {

				try {
					String out = headerSummary.getSummary(headerInfo, j);
					final String[] headers = headerInfo.getHeader(j);

					if (out == null) {
						out = "No Label";
					}

					/* Set label color */
					if (drawSelection.isIndexSelected(j) || j == hoverIndex) {
						if (colorIndex > 0) {
							g.setColor(TreeColorer
									.getColor(headers[colorIndex]));
						}

						g2d.setColor(fore);

						if (colorIndex > 0) {
							g.setColor(fore);
						}

					} else {
						g2d.setColor(Color.black);
					}

					if(maxStrLen < metrics.stringWidth(out)) {
						maxStrLen = metrics.stringWidth(out);
						maxStr = out;
					}
					/* Finally draw label (alignment-dependent) */
					int xPos = 0;
					if (isRightJustified) {
						xPos = stringX - metrics.stringWidth(out);
					}

					g2d.drawString(out, xPos, map.getMiddlePixel(j) + ascent
							/ 2);

				} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
					LogBuffer.logException(e);
					break;
				}
			}
			LogBuffer.println((isGeneAxis ? "ROW" : "COL") + ": MaxStrLen: [" +
			maxStrLen + "] MaxStr: [" + maxStr + "] Start Index: [" + start +
			"] End Index: [" + end + "] height [" + offscreenSize.height +
			"] width [" + offscreenSize.width + "]");

			g2d.setTransform(orig);

			//Set the size of the scrollpane to match the longest string
			if(isGeneAxis) {
				//+ 15 allows for the appearance & disappearance of the main
				//scrollbar, which I believe is only for Macs since macs draw
				//the scrollbar over the content when set AS_NEEDED
				setPreferredSize(new Dimension((getSecondaryScrollBar().isVisible() ? maxStrLen + 15 : maxStrLen),
						offscreenSize.height));
			} else {
				//+ 15 allows for the appearance & disappearance of the main
				//scrollbar, which I believe is only for Macs since macs draw
				//the scrollbar over the content when set AS_NEEDED
				setPreferredSize(new Dimension(offscreenSize.width,
						(getSecondaryScrollBar().isVisible() ? maxStrLen + 15 : maxStrLen)));
			}

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
								offscreenSize.height));

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
				setPreferredSize(new Dimension(offscreenSize.width,
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

	protected int shiftForScrollbar = 0;
	protected int shiftedForScrollbar = 0;

	@Override
	public void mouseMoved(final MouseEvent e) {

		if(shiftForScrollbar > 0) {
			//Shift the scroll position to accommodate the scrollbar that
			//appeared (I think this may only be for Macs, according to what I
			//read.  They draw the scrollbar on top of content when it is set
			//"AS_NEEDED")
			getSecondaryScrollBar().setValue(shiftForScrollbar);
			if(getSecondaryScrollBar().getValue() == shiftForScrollbar) {
				shiftedForScrollbar = shiftForScrollbar;
				shiftForScrollbar = 0;
			}
		}
		hoverIndex = map.getIndex(getPrimaryHoverPosition(e));
		repaint();
	}

	//@Override
	public void mouseEntered(final MouseEvent e) {
		//This method call is why these mouse functions
		setSecondaryScrollBarPolicyAlways();
		//scrollPane.setVerticalScrollBarPolicy(
		//		ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		int ts = getSecondaryScrollBar().getValue();
		int tw = getSecondaryScrollBar().getModel().getExtent();
		int tm = getSecondaryScrollBar().getMaximum();
		
		//We do not want to shift the scrollbar if the user has manually left-
		//justified his/her labels, so only shift to accommodate the scrollbar
		//when the scroll position is more than half way
		boolean nearBeginning = (ts < ((tm - tw) / 2));
		LogBuffer.println("ENTER Setting temp scroll value: [" + ts +
				"] width [" + tw + "] max [" + tm + "]");
		if(isJustifiedToMatrixEdge() && !nearBeginning) {
			LogBuffer.println("Adjusting scrollbar that is positioned near " +
					"end. Now position: [" + ts + " + 15] New max: [" +
					getSecondaryScrollBar().getMaximum() + " + 15]");
			//Width of the vertical scrollbar is 15
			int newWidth = getSecondaryScrollBar().getMaximum() + 15;
			getSecondaryScrollBar().setMaximum(newWidth);
			shiftForScrollbar = ts + 15;
			getSecondaryScrollBar().setValue(shiftForScrollbar);
		}
		LogBuffer.println("ENTER New scroll values: [" +
				getSecondaryScrollBar().getValue() + "] width [" +
				getSecondaryScrollBar().getModel().getExtent() + "] max [" +
				getSecondaryScrollBar().getMaximum() + "]");
	}

	//@Override
	public void mouseExited(final MouseEvent e) {
		int ts = getSecondaryScrollBar().getValue();
		setSecondaryScrollBarPolicyNever();
		//scrollPane.setVerticalScrollBarPolicy(
		//		ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		LogBuffer.println("EXIT Setting temp scroll value: [" + ts + "]");
		if(shiftedForScrollbar > 0) {
			LogBuffer.println("Adjusting scrollbar that is positioned near end.");
			getSecondaryScrollBar().setValue(ts - 15);
			shiftedForScrollbar = 0;
		}
		scrollPane.revalidate();
		LogBuffer.println("EXIT New scroll values: [" + getSecondaryScrollBar().getValue() + "] width [" + getSecondaryScrollBar().getModel().getExtent() + "] max [" + getSecondaryScrollBar().getMaximum() + "]");
		hoverIndex = -1;
		repaint();
	}
}
