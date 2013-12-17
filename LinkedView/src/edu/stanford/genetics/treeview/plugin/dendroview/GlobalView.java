/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: GlobalView.java,v $
 * $Revision: 1.1 $
 * $Date: 2006-08-16 19:13:45 $
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

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import edu.stanford.genetics.treeview.*;

class GlobalView extends ModelViewProduced implements  MouseMotionListener,
    MouseListener, MouseWheelListener, KeyListener, ComponentListener {    

	private static final long serialVersionUID = 1L;
	
	protected boolean hasDrawn = false;
	protected TreeSelectionI geneSelection;
	protected TreeSelectionI arraySelection;
	protected MapContainer xmap;
	protected MapContainer ymap;
    protected MapContainer zoomXmap;
    protected MapContainer zoomYmap;
	private String [] statustext = new String [] {"Mouseover Selection", 
			"", "", "Active Map: Global"};
	private HeaderInfo arrayHI;
	private HeaderInfo geneHI;
    private ArrayDrawer drawer;
    private int overx;
    private int overy;
    private JScrollPane scrollPane; 

    /**
     * Points to track candidate selected rows/cols
     * should reflect where the mouse has actually been
     */
    private Point startPoint = new Point();
    private Point endPoint = new Point();

    /**
     * This rectangle keeps track of where the drag rect was drawn
     */
    private Rectangle dragRect = new Rectangle();
    
    /**
     * Rectangle to track yellow selected rectangle (pixels)
     */
    private Rectangle selectionRect = null;

    /**
     * Rectangle to track blue zoom rectangle (pixels)
     */
   // private Rectangle zoomRect = null;
	
	/**
	* GlobalView also likes to have an globalxmap and globalymap 
	* (both of type MapContainer) to help it figure out where to draw things. 
	* It also tries to
	*/
    public GlobalView() {
		
    	super();
		//panel = this;
		
		this.setLayout(new MigLayout());
		
		scrollPane = new JScrollPane(panel);
		//this.add(scrollPane);
		
		setToolTipText("This Turns Tooltips On");

		addComponentListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
    }
    
    public JScrollBar getXScroll() {
    	
    	return scrollPane.getHorizontalScrollBar();
    }
    
    public JScrollBar getYScroll() {
    	
    	return scrollPane.getVerticalScrollBar();
    }

    @Override
	public Dimension getPreferredSize() {
	
    	Dimension p = new Dimension(xmap.getRequiredPixels(),
				    ymap.getRequiredPixels());
    	return p;
    }

//    @Override
//	public String[] getStatus() {
//	  
//    	String [] status = new String[4];
//    	if ((geneSelection == null) || (arraySelection == null)) {
//    		status[0] = "ERROR: GlobalView improperly configured";
//    		status[1] = " geneSelection is null";
//    		status[2] = " thus, gene selection will not work.";
//    		status[3] = "";
//    		
//    	} else {
//    		int sx = arraySelection.getMinIndex();
//    		int ex = arraySelection.getMaxIndex();
//		  
//    		int sy = geneSelection.getMinIndex();
//    		int ey = geneSelection.getMaxIndex();
//		  
//    		status[0] = (ey - sy + 1)  + " genes selected";
//    		status[1] = (ex - sx + 1) + " arrays selected";
//    		status[2] = "Genes from " + sy + " to " + ey;
//    		status[3] = "Arrays from " + sx + " to " + ex;
//    	}
//    	
//    	return status;
//    }
    
    @Override
	public String[] getStatus() {
    	
    	try {
			if (xmap.contains(overx) 
					&& ymap.contains(overy)) {
				statustext[0] = "Row:    " + (overy + 1);
				
				if (geneHI != null) {
					int realGene = overy;
					try {
						statustext[0] += " (" + geneHI.getHeader(realGene, 1)
								+ ")";
						
					} catch (java.lang.ArrayIndexOutOfBoundsException e) {
						statustext[0] += " (N/A)";
					}
				}
				statustext[1] = "Column: " + (overx + 1);
				if (arrayHI != null) {
					try {
						statustext[1] += " (" + arrayHI.getHeader(overx, 0) 
								+ ")";
						
					} catch (java.lang.ArrayIndexOutOfBoundsException e) {
						statustext[1] += " (N/A)";
					}
				}
				
				if (drawer.isMissing(overx, overy)) {
					statustext[2] = "Value:  No Data";	
					
				} else if (drawer.isEmpty(overx, overy)) {
					statustext[2] = "";
					
				} else {
					statustext[2] = "Value:  " + drawer.getSummary(overx, 
							overy);
				}
			}	
		} catch (ArrayIndexOutOfBoundsException ex) {
			// ignore silently?
		}
    	return statustext;
    }

    /** 
     * Set geneSelection
     *
     * @param geneSelection The TreeSelection which is set by selecting 
     * genes in the GlobalView
     */
    public void setGeneSelection(TreeSelectionI geneSelection) {
	  
    	if (this.geneSelection != null) {
    		this.geneSelection.deleteObserver(this);	
    	}
    	
    	this.geneSelection = geneSelection;
    	this.geneSelection.addObserver(this);
    }
    
    /** 
     * Set arraySelection
     *
     * @param arraySelection The TreeSelection which is set by selecting 
     * arrays in the GlobalView
     */
    public void setArraySelection(TreeSelectionI arraySelection) {
	 
    	if (this.arraySelection != null) {
    		this.arraySelection.deleteObserver(this);	
    	}
    	
    	this.arraySelection = arraySelection;
    	this.arraySelection.addObserver(this);
    }

    /** 
     * Set ArrayDrawer
     *
     * @param arrayDrawer The ArrayDrawer to be used as a source
     */
    public void setArrayDrawer(ArrayDrawer arrayDrawer) {
	
    	if (drawer != null) {
    		drawer.deleteObserver(this);	
    	}
    	
		drawer = arrayDrawer;
		drawer.addObserver(this);
    }

    /** 
     * Get ArrayDrawer
     *
     * @return The current ArrayDrawer
     */
    public ArrayDrawer getArrayDrawer() {
	
    	return drawer;
    }

    /** DEPRECATE
     * set the xmapping for this view
     *
     * @param m   the new mapping
     */
    public void setXMap(MapContainer m) {
	
    	if (xmap != null) {
    		xmap.deleteObserver(this);	    
    	}
    	
    	xmap = m;
    	xmap.addObserver(this);
    }

    /** DEPRECATE
     * set the ymapping for this view
     *
     * @param m   the new mapping
     */
    public void setYMap(MapContainer m) {
	
    	if (ymap != null) {
    		ymap.deleteObserver(this);	    
    	}
	
    	ymap = m;
    	ymap.addObserver(this);
    }

//    /**
//     * get the xmapping for this view
//     *
//     * @return  the current mapping
//     */
//    public MapContainer getXMap() {
//    	
//    	return xmap;
//    }
//
//    /**
//     * get the ymapping for this view
//     *
//     * @return   the current mapping
//     */
//    public MapContainer getYMap() {
//    	
//    	return ymap;
//    }
    

//    /** DEPRECATE
//     * set the xmapping for this view
//     *
//     * @param m   the new mapping
//     */
//    public void setZoomXMap(MapContainer m) {
//	
//    	if (zoomXmap != null) {
//    		zoomXmap.deleteObserver(this);	    
//    	}
//	
//    	zoomXmap = m;
//    	zoomXmap.addObserver(this);
//    }
//
//    /** DEPRECATE
//     * set the ymapping for this view
//     *
//     * @param m   the new mapping
//     */
//    public void setZoomYMap(MapContainer m) {
//	
//    	if (zoomYmap != null) {
//    		zoomYmap.deleteObserver(this);	    
//    	}
//    	
//    	zoomYmap = m;
//    	zoomYmap.addObserver(this);
//    }


    @Override
	public String viewName() {
    	
    	return "GlobalView";
    }

    //Canvas Methods
	/**
	* This method updates the graphics object directly by asking the 
	* ArrayDrawer to draw on it directly. The alternative is to have a 
	* pixel buffer which you update using updatePixels.
	*/
	@Override
	protected void updateBuffer(Graphics g) {
		
		if (offscreenChanged) {
			xmap.setAvailablePixels(offscreenSize.width);
			ymap.setAvailablePixels(offscreenSize.height);
			
			if (hasDrawn == false) {
				// total kludge, but addnotify isn't working correctly...
				xmap.recalculateScale();
				ymap.recalculateScale();
				hasDrawn = true;
			}
			
			xmap.notifyObservers();
			ymap.notifyObservers();
		}
		
		if (offscreenValid == false) {
			// clear the pallette...
			g.setColor(Color.white);
			g.fillRect(0,0, offscreenSize.width, offscreenSize.height);
			g.setColor(Color.black);
			
			Rectangle destRect = new Rectangle
			(0,0,xmap.getUsedPixels(), ymap.getUsedPixels());
			
			Rectangle sourceRect = new Rectangle
			(xmap.getIndex(0), ymap.getIndex(0),
			xmap.getIndex(destRect.width) - xmap.getIndex(0), 
			ymap.getIndex(destRect.height) - ymap.getIndex(0));
			
			if ((sourceRect.x >= 0) && (sourceRect.y >= 0)) {
				drawer.paint(g, sourceRect, destRect, null);
			}
		}
    }

	/** 
	* This method updates a 
	* pixel buffer. The alternative is to update the graphics object
	* directly by calling updateBuffer.
	*/
	@Override
	protected void updatePixels() {
		
		if (offscreenChanged) {
			offscreenValid = false;
			xmap.setAvailablePixels(offscreenSize.width);
			ymap.setAvailablePixels(offscreenSize.height);

			if (hasDrawn == false) {
				// total kludge, but addnotify isn't working correctly...
				xmap.recalculateScale();
				ymap.recalculateScale();
				hasDrawn = true;
			}
			
			xmap.notifyObservers();
			ymap.notifyObservers();
		}
		
		if (offscreenValid == false) {
			Rectangle destRect = new Rectangle(0, 0, xmap.getUsedPixels(), 
					ymap.getUsedPixels());
			
			Rectangle sourceRect = new Rectangle(xmap.getIndex(0), 
					ymap.getIndex(0), xmap.getIndex(destRect.width) 
					- xmap.getIndex(0), ymap.getIndex(destRect.height) - 
					ymap.getIndex(0));

			if ((sourceRect.x >= 0) && (sourceRect.y >= 0)) {
				drawer.paint(offscreenPixels, sourceRect, 
						destRect, offscreenScanSize);
			}
			
			offscreenSource.newPixels();
		}
	}

    @Override
	public synchronized void paintComposite (Graphics g) {
		
    	// composite the rectangles...
		if (selectionRect != null) {	    
//		    if (zoomRect != null) {
//				g.setColor(Color.cyan);
//				g.drawRect(zoomRect.x, zoomRect.y, 
//					   zoomRect.width, zoomRect.height);
//		    }
		    
		    g.setColor(Color.yellow);
		    g.drawRect(selectionRect.x, selectionRect.y, 
			       selectionRect.width, selectionRect.height);
		}
    }
    
	protected void recalculateOverlay() {
		
		if ((geneSelection == null) || (arraySelection == null)) {
			return;
		}
		
		int spx, spy, epx, epy;
		spx = xmap.getPixel(arraySelection.getMinIndex());
		// last pixel of last block
		epx = xmap.getPixel(arraySelection.getMaxIndex() + 1) - 1; 
		
		
		spy = ymap.getPixel(geneSelection.getMinIndex());
		epy = ymap.getPixel(geneSelection.getMaxIndex() + 1) - 1;
		
		if (epy < spy) {
			epy = spy; 
			// correct for roundoff error above
		}
		
		if (selectionRect == null) {
			selectionRect = new Rectangle(spx, spy, epx - spx, epy - spy);
			
		} else {
			selectionRect.setBounds(spx, spy, epx - spx, epy - spy);
		}
		
		this.revalidate();
		this.repaint();
	}

    protected void recalculateZoom() {
		
    	if (selectionRect == null) {
    		return;
    	}
    	
		//int spx, epx, spy, epy;
//		try {
//		    spx = xmap.getPixel(zoomXmap.getIndex(0));
//		    epx = xmap.getPixel(zoomXmap.getIndex(
//		    		zoomXmap.getUsedPixels())) - 1;
//		    
//		    spy = ymap.getPixel(zoomYmap.getIndex(0));
//		    epy = ymap.getPixel(zoomYmap.getIndex(
//		    		zoomYmap.getUsedPixels())) - 1;
//		    
//		} catch (java.lang.ArithmeticException e) {
//		    // silently ignore div zero exceptions, which arise when 
//		    // some dimension is zero and fillmap is selected...
//		    return;
//		}
	
//		if (zoomRect == null) {
//			zoomRect = new Rectangle(spx, spy, epx - spx, epy - spy);
//			
//		} else {
//			zoomRect.setBounds(spx, spy, epx - spx, epy - spy);
//		}
    }

    
    // Observer Methods
	@Override
	public void update(Observable o, Object arg) {
		
		if (o == geneSelection) {
			if (arraySelection.getNSelectedIndexes() == 0) {
				if (geneSelection.getNSelectedIndexes() != 0) {
					// select all arrays if some genes selected...
					arraySelection.selectAllIndexes();
					// notifies self...
					arraySelection.notifyObservers();
					return;
				}
			}
			recalculateOverlay();
			
		} else if (o == arraySelection) {
			if (geneSelection.getNSelectedIndexes() == 0) {
				if (arraySelection.getNSelectedIndexes() != 0) {
					// select all genes if some arrays selected...
					geneSelection.selectAllIndexes();
					// notifies self...
					geneSelection.notifyObservers();
					return;
				}
			}
			recalculateOverlay();
			
		} else if ((o == xmap) || o == ymap) {
			recalculateZoom(); // it moves around, you see...
			recalculateOverlay();
			offscreenValid = false;
			
		} else if ((o == zoomYmap) || (o == zoomXmap)) {
			recalculateZoom();
			/*
			if (o == zoomXmap) {
				if ((zoomYmap.getUsedPixels() == 0) 
				&& (zoomXmap.getUsedPixels() != 0)) {
					zoomYmap.setIndexRange(ymap.getMinIndex(), 
					ymap.getMaxIndex());
					zoomYmap.notifyObservers();
				}
			} else if (o == zoomYmap) {
				if ((zoomXmap.getUsedPixels() == 0) 
				&& (zoomYmap.getUsedPixels() != 0)) {
					zoomXmap.setIndexRange(xmap.getMinIndex(), 
					xmap.getMaxIndex());
					zoomXmap.notifyObservers();
				}
			}
			*/
			if ((status != null) && hasMouse) {
				status.setMessages(getStatus());
			}
			
		} else if (o == drawer) {
			/* signal from drawer means that it need to
			draw something different. 
			*/
			offscreenValid = false;
			
		} else {
			 LogBuffer.println("GlobalView got weird update : " + o);
		}
		
		this.revalidate();
		this.repaint();
	}

    // Mouse Listener 
    @Override
	public void mousePressed(MouseEvent e) {
		
    	if (enclosingWindow().isActive() == false) {
    		return;
    	}
	
    	//if left button is used
    	if(SwingUtilities.isLeftMouseButton(e)) {
			startPoint.setLocation(xmap.getIndex(e.getX()),
					       ymap.getIndex(e.getY()));
			endPoint.setLocation(startPoint.x, startPoint.y);
			dragRect.setLocation(startPoint.x, startPoint.y);
			dragRect.setSize(endPoint.x - dragRect.x,
					 endPoint.y - dragRect.y);
		
			drawBand(dragRect);
    	}
    }
    
    @Override
	public void mouseReleased(MouseEvent e) {
		
    	if (enclosingWindow().isActive() == false) {
    		return;
    	}	
		
		//When left button is used
		if(SwingUtilities.isLeftMouseButton(e)) {
			mouseDragged(e);
			drawBand(dragRect);
			
			if (e.isShiftDown()) {
			    Point start = new Point(xmap.getMinIndex(), ymap.getMinIndex());
					    //startPoint.y);
			    Point end = new Point(xmap.getMaxIndex(), ymap.getMaxIndex());
					 //endPoint.y);
			    selectRectangle(start, end);
			    
			} else {
				selectRectangle(startPoint, endPoint);
			}
		} else {
			//do something else?
		}
		
		this.revalidate();
		this.repaint();
    }
    
    // MouseMotionListener
    @Override
	public void mouseDragged(MouseEvent e) {
		
    	//When left button is used
    	if(SwingUtilities.isLeftMouseButton(e)) {
	    	//rubber band?
			drawBand(dragRect);	
			endPoint.setLocation(xmap.getIndex(e.getX()), ymap.getIndex(e.getY()));
			
			if (e.isShiftDown()) {
				dragRect.setLocation(xmap.getMinIndex(), startPoint.y);
			    dragRect.setSize(0,0);
			    dragRect.add(xmap.getMaxIndex(), endPoint.y);
		    
			} else {
			    dragRect.setLocation(startPoint.x, startPoint.y);
			    dragRect.setSize(0,0);
			    dragRect.add(endPoint.x, endPoint.y);
			}
			
			drawBand(dragRect);
    	}
    }
    
    @Override
	public void mouseMoved(MouseEvent e) {
    	
		int ooverx = overx;
		int oovery = overy;
		overx = xmap.getIndex(e.getX());
		overy = ymap.getIndex(e.getY());
		if (oovery != overy || ooverx != overx) {
			if (status != null) {
		    	status.setMessages(getStatus());
		    }
		}
    }
    
    @Override
	public String getToolTipText(MouseEvent e) {
		/* Do we want to do mouseovers if value already visible? 
		if (getShowVal()) return null; 
		// don't do tooltips and vals at same time.
		 */
		String ret = "";
		String row = "";
		String col = "";
		
		if (drawer != null) {
			
			int geneRow = overy;
			int geneCol = overx;
			
			if (xmap.contains(overx) && ymap.contains(overy)) {
				
				if(geneHI != null) {
					row = geneHI.getHeader(geneRow, 1);
					
				} else {
					row = "N/A";
				}
				
				if(arrayHI != null) {
					col = arrayHI.getHeader(geneCol, 0);
					
				} else {
					col = "N/A";
				}
				
				if (drawer.isMissing(overx, geneRow)) {
					ret = "No data";
					
				} else if (drawer.isEmpty(overx, geneRow)) {
					ret = null;
					
				} else {
					ret = "Row: " + row + " Column: " + col + " Value: " 
					+ drawer.getSummary(overx, geneRow);
				}
			}
		}
		return ret;
	}
    
    private void drawBand(Rectangle l) { 
		
    	Graphics g = getGraphics();
		g.setXORMode(getBackground());
		
		int x = xmap.getPixel(l.x);
		int y = ymap.getPixel(l.y);
		int w = xmap.getPixel(l.x + l.width  + 1) - x;
		int h = ymap.getPixel(l.y + l.height + 1) - y;
		
		g.drawRect(x, y, w, h);
		g.setPaintMode();
    }
    
    //KeyListener 
	@Override
	public void keyPressed(KeyEvent e) {
		
		int c = e.getKeyCode();
		int shift;
		
		if (e.isShiftDown()) {
			shift = 10;
			
		} else {
			shift = 1;
		}
		
		double zoomValX = 10.0;
		double zoomValY = 10.0;
		
		if(xmap.getScale() >= 10) {
			zoomValX = 1.0;
		}
		
		if(ymap.getScale() >= 10) {
			zoomValY = 1.0;
		}
		
		switch (c) {
		case KeyEvent.VK_LEFT:
			xmap.scrollBy(-shift);
			break;
		case KeyEvent.VK_RIGHT:
			xmap.scrollBy(shift);
			break;
		case KeyEvent.VK_UP:
			ymap.scrollBy(-shift);
			break;
		case KeyEvent.VK_DOWN:
			ymap.scrollBy(shift);
			break;
		case KeyEvent.VK_MINUS:
			xmap.zoomOut(zoomValX);
			ymap.zoomOut(zoomValY);
			break;
		case KeyEvent.VK_EQUALS:
			xmap.zoomIn(zoomValX);
			ymap.zoomIn(zoomValY);
			break;
		}
		
		this.revalidate();
		this.repaint();
	}
	
	/**
	 * Zooming when the mouse wheel is used in conjunction with the shift key.
	 * Vertical scrolling if the shift key is not pressed.
	 */
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		
		int notches = e.getWheelRotation();
		int shift = 3;
		double zoomValX = 10.0;
		double zoomValY = 10.0;
		
		if(xmap.getScale() >= 10) {
			zoomValX = 1.0;
		}
		
		if(ymap.getScale() >= 10) {
			zoomValY = 1.0;
		}
		
		if(!e.isShiftDown()) {
			if(notches < 0) {
				ymap.scrollBy(-shift);
				
			} else {
				ymap.scrollBy(shift);
			}
		} else {
			if(notches < 0) {
				xmap.zoomIn(zoomValX);
				ymap.zoomIn(zoomValY);
				
			} else {
				xmap.zoomOut(zoomValX);
				ymap.zoomOut(zoomValY);
			}
		}
		
		this.revalidate();
		this.repaint();
	}
	
	/**
	 * Selecting a rectangular area in GlobalView
	 * @param start
	 * @param end
	 */
	private void selectRectangle(Point start, Point end) {
		
		// sort so that ep is upper left corner
		if (end.x < start.x) {
			int x = end.x;
			end.x = start.x;
			start.x = x;
		}
		
		if (end.y < start.y) {
			int y = end.y;
			end.y = start.y;
			start.y = y;
		}
		
		// nodes
		geneSelection.setSelectedNode(null);
		// genes...
		geneSelection.deselectAllIndexes();
		
		for (int i = start.y; i <= end.y; i++) {
			
			geneSelection.setIndex(i, true);
		}
		
		// arrays...
		arraySelection.setSelectedNode(null);
		arraySelection.deselectAllIndexes();
		
		for (int i = start.x; i <= end.x; i++) {
			
			arraySelection.setIndex(i, true);
		}
		
		geneSelection.notifyObservers();
		arraySelection.notifyObservers();	
	}
	
	/**
	 * Uses the array- and geneSelection and currently available pixels on
	 * screen retrieved from the MapContainer objects to calculate a new
	 * scale and zoom in on it by working in conjunction with 
	 * centerSelection().
	 */
	public void zoomSelection(){
		
		double newScale = 0.0;
		double newScale2 = 0.0;
		
		int arrayIndexes = arraySelection.getNSelectedIndexes();
		int geneIndexes = geneSelection.getNSelectedIndexes();
		
		if(arrayIndexes > 0 && geneIndexes > 0) {
			newScale = xmap.getAvailablePixels()/ arrayIndexes;
			xmap.setScale(newScale);
			
			newScale2 = ymap.getAvailablePixels()/ geneIndexes;
			ymap.setScale(newScale2);
		}
	}
	
	/**
	 * Scrolls to the center of the selected rectangle
	 */
	public void centerSelection() {
		
		int scrollX;
		int scrollY;
		
		if(startPoint != null && endPoint != null) {
			scrollX = (endPoint.x + startPoint.x)/2;
			scrollY = (endPoint.y + startPoint.y)/2;
			
			System.out.println("SX: " + scrollX);
				
			xmap.scrollToIndex(scrollX);
			ymap.scrollToIndex(scrollY);
		}
	}
	
	/**
	 * Scrolls to the center of the selected rectangle
	 */
	public void centerView(int scrollX, int scrollY) {
				
		scrollX = scrollX + (xmap.getScroll().getVisibleAmount()/2);
		scrollY = scrollY + (ymap.getScroll().getVisibleAmount()/2);
		
		xmap.scrollToIndex(scrollX);
		ymap.scrollToIndex(scrollY);
	}
	
	/**
	 * Sets the gene header instance variables of GlobalView.
	 * @param ghi
	 * @param ahi
	 */
	public void setHeaders(HeaderInfo ghi, HeaderInfo ahi) {
		
		geneHI = ghi;
		arrayHI = ahi;
	}
	
	//Component Listeners
	@Override
	public void componentHidden(ComponentEvent arg0) {}

	@Override
	public void componentMoved(ComponentEvent arg0) {}

	//Keep view centered and zoomed on visible part, also refreshing the 
	//MapContainer with setHome to always fill out the entire GlobalView 
	//panel
	@Override
	public void componentResized(ComponentEvent arg0) {
		
		double scaleFactorX = 1.0;
		double scaleFactorY = 1.0;
		
		int scrollX = xmap.getScroll().getValue();
		int scrollY = ymap.getScroll().getValue();
		
		if(xmap.getMinScale() > 0.0) {
			scaleFactorX = xmap.getScale()/ xmap.getMinScale();
		}
		xmap.setHome();
		xmap.setScale(xmap.getMinScale() * scaleFactorX);
		
		if(ymap.getMinScale() > 0.0) {
			scaleFactorY = ymap.getScale()/ ymap.getMinScale();
		}
		ymap.setHome();
		ymap.setScale(ymap.getMinScale() * scaleFactorY);
		
		centerView(scrollX, scrollY);
		
		this.repaint();
		this.revalidate();
	}

	@Override
	public void componentShown(ComponentEvent arg0) {}
}


