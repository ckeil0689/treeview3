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
	protected boolean d_justified;
	protected boolean d_fixed = false;

	/* Custom label settings */
	protected String face;
	protected int style;
	protected int size;
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

		setFace(configNode.get("face", d_face));
		setStyle(configNode.getInt("style", d_style));
		setPoints(configNode.getInt("size", d_size));
		setJustifyOption(configNode.getBoolean("isRightJustified", d_justified));
		setFixed(configNode.getBoolean("isFixed", d_fixed));

		getHeaderSummary().setConfigNode(configNode);
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
	public JScrollBar getScrollBar() {

		if (isGeneAxis) {
			return scrollPane.getHorizontalScrollBar();
		}
		
		return scrollPane.getVerticalScrollBar();
	}

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
		final int end = map.getIndex(map.getUsedPixels()) - 1;
		
		final Graphics2D g2d = (Graphics2D) g;
		final AffineTransform orig = g2d.getTransform();
		
		/* Draw labels if zoom level allows it */
		if (map.getScale() > 10.0) {
			
			if(!isFixed) {
				setDynamicFontSize();
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
						// ingore...
					}
					g.fillRect(0, map.getMiddlePixel(j) - ascent / 2, stringX,
							ascent);
				}
				g.setColor(back);
			}

			/* Draw the labels */
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

			g2d.setTransform(orig);

		} else {
			setPoints(HINTFONTSIZE);
			
			int xPos = getHintX(g2d, stringX);
			int yPos = getHintY(g2d);
			
			g2d.setColor(Color.black);
			g2d.drawString(zoomHint, xPos, yPos);
		}
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
	 * Sets a dynamic font size based on current scale of each independent
	 * axis map.
	 */
	private void setDynamicFontSize() {
		
		double scale = map.getScale();
		
		int multiple = (int) scale / 10;
		
		/* Guarantee max font size of 14 */
		if(multiple > 10) {
			multiple = 10;
		}
		
		int newPoints;
		if(multiple < 2) {
			newPoints = 6;
		} else {
			/* rate of 2 is too much, rate of 1 too little... */
			newPoints = 6 + 2 * multiple;
		}
		
		if(newPoints != getPoints()) {
			setPoints(newPoints);
		}
	}

	@Override
	public void mouseExited(final MouseEvent e) {

		hoverIndex = -1;
		repaint();
	}

	@Override
	public void mouseDragged(final MouseEvent e) {

	}

	@Override
	public void mouseMoved(final MouseEvent e) {

	}
}
