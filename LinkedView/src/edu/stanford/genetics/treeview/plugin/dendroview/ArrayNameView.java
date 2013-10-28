
/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: ArrayNameView.java,v $
 * $Revision: 1.4 $
 * $Date: 2010-05-02 13:39:00 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular,
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER
 */
package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.*;
import java.awt.event.*;
import java.util.Observable;

import javax.swing.JScrollPane;

import edu.stanford.genetics.treeview.*;

/**
 *  Renders the names of the arrays.
 *
 * Actually, renders the first element in a HeaderInfo as vertical text. Could easily be generalized.
 *
 * @author     Alok Saldanha <alok@genome.stanford.edu>
 * @version    @version $Revision: 1.4 $ $Date: 2010-05-02 13:39:00 $
 */
public class ArrayNameView extends ModelView implements MouseListener, FontSelectable, ConfigNodePersistent {

	private static final long serialVersionUID = 1L;
	
	private final String d_face        = "Courier";
	private final int d_style          = 0;
	private final int d_size           = 12;

	/**  HeaderInfo containing the names of the arrays. */
	protected HeaderInfo headerInfo = null;
	protected DataModel dataModel = null;
	
	public HeaderInfo getHeaderInfo() {
		return headerInfo;
	}
	
	public DataModel getDataModel()
	{
		return dataModel;
	}
	
	public void setHeaderInfo(HeaderInfo headerInfo) {
		this.headerInfo = headerInfo;
	}
	
	public void setDataModel(DataModel dataModel) {
			
			if(dataModel != null)
			{
				((Observable)dataModel).deleteObserver(this);
			}
			this.dataModel = dataModel;
			((Observable)dataModel).addObserver(this);
	}
	
	private ConfigNode root            = null;

	private String face;
	private int style;
	private int size;

	private UrlExtractor urlExtractor  = null;
	private MapContainer map;
	private int maxlength              = 0;
	private boolean backBufferValid    = false;
	private Image backBuffer;

	private JScrollPane scrollPane;

	/* inherit description */
	@Override
	public String viewName() {
		
		return "ArrayNameView";
	}


	/* inherit description */
	@Override
	public String[] getHints() {
		String[] hints  = {
				" - Click and drag to scroll",
				};
		return hints;
	}


	/**
	 *  Constructs an <code>ArrayNameView</code> with the given <code>HeaderInfo</code> 
	 * as a source of array names.
	 *
	 * @param  hInfo  Header containing array names as first row.
	 */
	public ArrayNameView(HeaderInfo hInfo) {
		
		super();
		headerInfo = hInfo;
		headerSummary = new HeaderSummary();
		headerSummary.setIncluded(new int [] {0});
		scrollPane = new JScrollPane(this);
		scrollPane.setBorder(null);
		panel = scrollPane;
		addMouseListener(this);
	}

	// Canvas methods
	/**  updates a horizontally oriented test buffer, which will later be rotated to make
	* vertical text.
	* This is only used in the abscence of Graphics2D
	*/
	public void updateBackBuffer() {
		Graphics g           = backBuffer.getGraphics();
		int start            = map.getIndex(0);
		int end              = map.getIndex(map.getUsedPixels()) - 1;

		g.setColor(Color.white);
		g.fillRect(0, 0, maxlength, offscreenSize.width);
		g.setColor(Color.black);

		int gidRow           = headerInfo.getIndex("GID");
		if (gidRow == -1) {
			gidRow = 0;
		}
		int colorIndex       = headerInfo.getIndex("FGCOLOR");

		g.setFont(new Font(face, style, size));
		FontMetrics metrics  = getFontMetrics(g.getFont());
		int ascent           = metrics.getAscent();
	    // draw backgrounds first...
	    int bgColorIndex = headerInfo.getIndex("BGCOLOR");
	    if (bgColorIndex > 0) {
		    Color back = g.getColor();
		    for (int j = start; j < end;j++) {
				    String [] strings = headerInfo.getHeader(j);
				    try {
				    g.setColor(TreeColorer.getColor(strings[bgColorIndex]));
				    } catch (Exception e) {
				    }
				    g.fillRect(0, map.getMiddlePixel(j) - ascent / 2, maxlength, ascent);
		    }
		    g.setColor(back);
	    }

		Color back           = g.getColor();
		for (int j = start; j <= end; j++) {
			try {
				String out = headerSummary.getSummary(headerInfo, j);
			String[] headers  = headerInfo.getHeader(j);

				//		System.out.println("Got row " + gidRow + " value " + out);
				if (out == null) {
					continue;
				}
				if ((arraySelection == null) || arraySelection.isIndexSelected(j)) {

					if (colorIndex > 0) {
						g.setColor(TreeColorer.getColor(headers[colorIndex]));
					}
					g.drawString(out, 0, map.getMiddlePixel(j) + ascent / 2);
					if (colorIndex > 0) {
						g.setColor(back);
					}
				} else {
					g.setColor(Color.gray);
					g.drawString(out, 0, map.getMiddlePixel(j) + ascent / 2);
					g.setColor(back);
				}
			} catch (java.lang.ArrayIndexOutOfBoundsException e) {
			}
		}
		backBuffer = RotateImageFilter.rotate(this, backBuffer);
	}


	/*inherit description */
	@Override
	public void updateBuffer(Graphics g) {
		updateBuffer(g, offscreenSize);
	}
	public void updateBuffer(Image buf) {
		updateBuffer(buf.getGraphics(), new Dimension(buf.getWidth(null), buf.getHeight(null)));
	}
	public void updateBuffer(Graphics g, Dimension offscreenSize) {
		g.setColor(Color.white);
		g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
		g.setColor(Color.black);


		/* This code is for java2.it's worth supporting two ways. */
		try {
			Graphics2D g2d = (Graphics2D) g;
			g2d.rotate(-90 * 3.14159/180);
			g2d.translate(-offscreenSize.height, 0);

			int start            = map.getIndex(0);
			int end              = map.getIndex(map.getUsedPixels()) - 1;
			int gidRow           = headerInfo.getIndex("GID");
			if (gidRow == -1) {gidRow = 0;}
			int colorIndex       = headerInfo.getIndex("FGCOLOR");
			g.setFont(new Font(face, style, size));
			FontMetrics metrics = getFontMetrics(g.getFont());
			int ascent = metrics.getAscent();

	    // draw backgrounds first...
	    int bgColorIndex = headerInfo.getIndex("BGCOLOR");
	    if (bgColorIndex > 0) {
		    Color back = g.getColor();
		    for (int j = start; j <= end;j++) {
				    String [] strings = headerInfo.getHeader(j);
				    try {
				    g.setColor(TreeColorer.getColor(strings[bgColorIndex]));
				    } catch (Exception e) {
					    // ingore...
				    }
				    g.fillRect(0, map.getMiddlePixel(j) - ascent / 2, offscreenSize.height, ascent);
		    }
		    g.setColor(back);
	    }



			Color back = g.getColor();
			for (int j = start;j <= end;j++) {
				try { 
			String out = headerSummary.getSummary(headerInfo, j);

			String[] headers  = headerInfo.getHeader(j);
/*
			String out        = headers[gidRow];
			*/
					if (out != null) {
						if ((arraySelection == null) || arraySelection.isIndexSelected(j)) {
							if (colorIndex > 0)
								g.setColor(TreeColorer.getColor(headers[colorIndex]));
							g.drawString(out, 0, map.getMiddlePixel(j) + ascent / 2);
							if (colorIndex > 0)
								g.setColor(back);
						} else {
							g.setColor(Color.gray);
							g.drawString(out, 0, map.getMiddlePixel(j) + ascent / 2);
							g.setColor(back);
						}
						
					}
				} catch (java.lang.ArrayIndexOutOfBoundsException e) {
				}
			}
			g2d.translate(offscreenSize.height, 0);
			g2d.rotate(90 * 3.14159/180);
			
		} catch (java.lang.NoClassDefFoundError e) {
			
			if (backBufferValid == false) {
				int tstart  = map.getIndex(0);
				int tend    = map.getIndex(map.getUsedPixels());
				if ((tstart >= 0 && tend > tstart) &&
				(offscreenSize.width > 0)) {
					
					/* Should have been done by selectionChanged()
					String [][] aHeaders = model.getArrayHeaders();
					int gidRow = model.getGIDIndex();
					int colorIndex = model.getRowIndex("FGCOLOR");
					
					g.setFont(new Font(face, style, size));
					FontMetrics metrics = getFontMetrics(g.getFont());
					int ascent = metrics.getAscent();
					
					// calculate maxlength
					maxlength = 1;
					// for some reason, stop at end -1?
					int start = map.getMinIndex();
					int end = map.getMaxIndex();
					for (int j = start;j < end;j++) {
						String out = aHeaders[j][gidRow];
						if (out == null) continue;
						int length = metrics.stringWidth(out);
						if (maxlength < length) {
							maxlength = length;
						}
					}
					*/
					
					backBuffer = createImage(maxlength, offscreenSize.width);
					updateBackBuffer();// this flips the backbuffer...
					
				} else {
					// some kind of blank default image?
				}
				backBufferValid = true;
				
			}
			
			if (offscreenSize.height < maxlength) {
				g.drawImage(backBuffer, 0, 0, null);
			} else {
				if ((g != null) && (backBuffer != null)) {
					g.drawImage(backBuffer, 0, offscreenSize.height - maxlength, null);
				}
			}
		}

	}


	/**
	 *  Sets the urlExtractor to be used when an array name is clicked on.
	 *
	 * @param  ue  Will be fed array indexes.
	 */
	public void setUrlExtractor(UrlExtractor ue) {
		urlExtractor = ue;
	}


	/**
	 *  Used to space the array names.
	 *
	 * @param  im  A new mapcontainer.
	 */
	public void setMapping(MapContainer im) {
		if (map != null) {
			map.deleteObserver(this);
		}
		map = im;
		map.addObserver(this);
	}


	private int oldHeight = 0;

	/**
	 *  This method is called when the selection is changed. It causes the component
	 *  to recalculate it's width, and call repaint.
	 */
	private void selectionChanged() {
		offscreenValid = false;
		backBufferValid = false;

		int start                = map.getMinIndex();
		int end                  = map.getMaxIndex();
		int gidRow               = headerInfo.getIndex("GID");
		if (gidRow == -1) {
			gidRow = 0;
		}
		FontMetrics fontMetrics  = getFontMetrics
				(new Font(face, style, size));
		maxlength = 1;
		for (int j = start; j < end; j++) {
				String out = headerSummary.getSummary(headerInfo, j);
/*
			String[] headers  = headerInfo.getHeader(j);
				String out        = headers[gidRow];
				*/
			if (out == null) {
				continue;
			}
			int length        = fontMetrics.stringWidth(out);
			if (maxlength < length) {
				maxlength = length;
			}
		}

		Rectangle visible        = getVisibleRect();
		setPreferredSize(new Dimension(map.getUsedPixels(), maxlength));
		revalidate();

		repaint();

		if (maxlength > oldHeight) {
			//	    System.out.println("old height "  + oldHeight +" new height " + maxlength + ", visible " + visible);
			visible.y += maxlength - oldHeight;
			//	    System.out.println("new visible " + visible);
			scrollRectToVisible(visible);
		}
		oldHeight = maxlength;

		/* The rest is done inside paintComponent...
	// calculate maxlength
	int start = map.getIndex(0);
	int end =   map.getIndex(map.getUsedPixels());
 	repaint();
	if (maxlength > oldHeight) {
	    //	    System.out.println("old height "  + oldHeight +" new height " + maxlength + ", visible " + visible);
	    visible.y += maxlength - oldHeight;
	    //	    System.out.println("new visible " + visible);
	    scrollRectToVisible(visible);
	}
	oldHeight = maxlength;
	*/
	}

	// Observer
	/**
	 *  Expects to see updates only from the map, when the array name spacing changes.
	 *
	 */
	@Override
	public void update(Observable o, Object arg) {
		if (o == map || o == dataModel) {
			selectionChanged();
		  } else if (o == arraySelection) {
			selectionChanged(); // which genes are selected changed
		  } else {
			System.out.println("ArrayNameView got funny update!");
		}
	}

	// MouseListener
	/**
	 *  Starts external browser if the urlExtractor is enabled.
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		if (urlExtractor == null) {
			return;
		}
		if (urlExtractor.isEnabled() == false) {
			return;
		}
		// now, want mouse click to signal browser...
		int index  = map.getIndex(e.getX());
		if (map.contains(index)) {
			viewFrame.displayURL(urlExtractor.getUrl(index));
		}
	}

	//FontSelectable
	/*inherit description */
	@Override
	public String getFace() {
		return face;
	}


	/*inherit description */
	@Override
	public int getPoints() {
		return size;
	}


	/*inherit description */
	@Override
	public int getStyle() {
		return style;
	}


	/*inherit description */
	@Override
	public void setFace(String string) {
        if ((face == null) ||(!face.equals(string))) {
			face = string;
			if (root != null)
			root.setAttribute("face", face, d_face);
			setFont(new Font(face, style, size));
			backBufferValid = false;
			repaint();
		}
	}

	/*inherit description */
	@Override
	public void setPoints(int i) {
		if (size != i) {
			size = i;
			if (root != null)
			root.setAttribute("size", size, d_size);
			setFont(new Font(face, style, size));
			backBufferValid = false;
			repaint();
		}
	}


	/*inherit description */
	@Override
	public void setStyle(int i) {
		if (style != i) {
			style = i;
			backBufferValid = false;
			if (root != null)
			root.setAttribute("style", style, d_style);
			setFont(new Font(face, style, size));
			repaint();
		}
	}

	private HeaderSummary headerSummary;
	/** Setter for headerSummary */
	public void setHeaderSummary(HeaderSummary headerSummary) {
		this.headerSummary = headerSummary;
	}
	/** Getter for headerSummary */
	public HeaderSummary getHeaderSummary() {
		return headerSummary;
	}

	/*inherit description */
	@Override
	public void bindConfig(ConfigNode configNode) {
		root = configNode;
		if (configNode.fetchFirst("ArraySummary") == null) {
			getHeaderSummary().bindConfig(configNode.create("ArraySummary"));
			getHeaderSummary().setIncluded(new int [] {0});
		} else {
			getHeaderSummary().bindConfig(configNode.fetchFirst("ArraySummary"));
		}
		setFace(root.getAttribute("face", d_face));
		setStyle(root.getAttribute("style", d_style));
		setPoints(root.getAttribute("size", d_size));
	}
	private TreeSelectionI arraySelection;
    /** 
     * Set geneSelection
     *
     * @param geneSelection The TreeSelection which is set by selecting genes in the GlobalView
     */
    public void setArraySelection(TreeSelectionI arraySelection) {
	  if (this.arraySelection != null)
	    this.arraySelection.deleteObserver(this);	
	  this.arraySelection = arraySelection;
	  this.arraySelection.addObserver(this);
    }
}

