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
		panel = this;
		
		this.setLayout(new MigLayout());
		
		setToolTipText("This Turns Tooltips On");
		
		addComponentListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
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

    /**
     * get the xmapping for this view
     *
     * @return  the current mapping
     */
    public MapContainer getXMap() {
    	
    	return xmap;
    }

    /**
     * get the ymapping for this view
     *
     * @return   the current mapping
     */
    public MapContainer getYMap() {
    	
    	return ymap;
    }
    

    /** DEPRECATE
     * set the xmapping for this view
     *
     * @param m   the new mapping
     */
    public void setZoomXMap(MapContainer m) {
	
    	if (zoomXmap != null) {
    		zoomXmap.deleteObserver(this);	    
    	}
	
    	zoomXmap = m;
    	zoomXmap.addObserver(this);
    }

    /** DEPRECATE
     * set the ymapping for this view
     *
     * @param m   the new mapping
     */
    public void setZoomYMap(MapContainer m) {
	
    	if (zoomYmap != null) {
    		zoomYmap.deleteObserver(this);	    
    	}
    	
    	zoomYmap = m;
    	zoomYmap.addObserver(this);
    }


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
			Rectangle destRect = new Rectangle
			(0,0, xmap.getUsedPixels(), ymap.getUsedPixels());
			
			Rectangle sourceRect = new Rectangle
			(xmap.getIndex(0), ymap.getIndex(0),
			xmap.getIndex(destRect.width) - xmap.getIndex(0), 
			ymap.getIndex(destRect.height) - ymap.getIndex(0));

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
		
		revalidate();
		repaint();
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
				if ((zoomYmap.getUsedPixels() == 0) && (zoomXmap.getUsedPixels() != 0)) {
					zoomYmap.setIndexRange(ymap.getMinIndex(), ymap.getMaxIndex());
					zoomYmap.notifyObservers();
				}
			} else if (o == zoomYmap) {
				if ((zoomXmap.getUsedPixels() == 0) && (zoomYmap.getUsedPixels() != 0)) {
					zoomXmap.setIndexRange(xmap.getMinIndex(), xmap.getMaxIndex());
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
		
		revalidate();
		repaint();
	}

    // Mouse Listener 
    @Override
	public void mousePressed(MouseEvent e) {
		
    	if (enclosingWindow().isActive() == false) {
    		return;
    	}
	
		startPoint.setLocation(xmap.getIndex(e.getX()),
				       ymap.getIndex(e.getY()));
		endPoint.setLocation(startPoint.x, startPoint.y);
		dragRect.setLocation(startPoint.x, startPoint.y);
		dragRect.setSize(endPoint.x - dragRect.x,
				 endPoint.y - dragRect.y);
	
		drawBand(dragRect);
    }
    
    @Override
	public void mouseReleased(MouseEvent e) {
		
    	if (enclosingWindow().isActive() == false) {
    		return;
    	}
    	
		mouseDragged(e);
		drawBand(dragRect);	
		
		if(SwingUtilities.isLeftMouseButton(e)) {
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
			selectAndZoom(startPoint, endPoint);
		}
		
		revalidate();
		repaint();
    }
    
    // MouseMotionListener
    @Override
	public void mouseDragged(MouseEvent e) {
		
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
		
		switch (c) {
		case KeyEvent.VK_LEFT:
			getXMap().scrollBy(-shift);
			break;
		case KeyEvent.VK_RIGHT:
			getXMap().scrollBy(shift);
			break;
		case KeyEvent.VK_UP:
			getYMap().scrollBy(-shift);
			break;
		case KeyEvent.VK_DOWN:
			getYMap().scrollBy(shift);
			break;
		case KeyEvent.VK_MINUS:
			getXMap().zoomOut();
			getYMap().zoomOut();
			break;
		case KeyEvent.VK_EQUALS:
			getXMap().zoomIn();
			getYMap().zoomIn();
			break;
		}
		
		revalidate();
		repaint();
		
//		int c = e.getKeyCode();
//		startPoint.setLocation(arraySelection.getMinIndex(), 
//				geneSelection.getMinIndex());
//		endPoint.setLocation (arraySelection.getMaxIndex(), 
//				geneSelection.getMaxIndex());
//		
//		if (e.isControlDown()) {
//			switch (c) {
//				case KeyEvent.VK_UP:
//					startPoint.translate(0, -1); 
//					endPoint.translate(0, 1); 
//					break;
//				case KeyEvent.VK_DOWN:
//					startPoint.translate(0, 1); 
//					endPoint.translate(0, -1); 
//					break;
//				case KeyEvent.VK_LEFT:
//					startPoint.translate(1, 0); 
//					endPoint.translate(-1, 0); 
//					break;
//				case KeyEvent.VK_RIGHT:
//					startPoint.translate(-1, 0); 
//					endPoint.translate(1, 0); 
//					break;
//			}
//		} else {
//			
//			switch (c) {
//				case KeyEvent.VK_UP:
//					startPoint.translate(0, -1); 
//					endPoint.translate(0, -1); 
//					break;
//				case KeyEvent.VK_DOWN:
//					startPoint.translate(0, 1); 
//					endPoint.translate(0, 1); 
//					break;
//				case KeyEvent.VK_LEFT:
//					startPoint.translate(-1, 0); 
//					endPoint.translate(-1, 0); 
//					break;
//				case KeyEvent.VK_RIGHT:
//					startPoint.translate(1, 0); 
//					endPoint.translate(1, 0); 
//					break;
//				case KeyEvent.VK_SHIFT:
//					// should we do something if shift is pressed during drag?
//					break;
//			}
//		}
//		
//		// make sure it all fits...
//		int overx = 0; int overy = 0;
//		if (startPoint.x < xmap.getMinIndex()) {
//			overx += startPoint.x - xmap.getMinIndex();
//		}
//		
//		if (startPoint.y < ymap.getMinIndex()) {
//			overy += startPoint.y - ymap.getMinIndex();
//		}
//		
//		if (endPoint.x > xmap.getMaxIndex()) {
//			overx += endPoint.x - xmap.getMaxIndex();
//		}
//		
//		if (startPoint.y < ymap.getMinIndex()) {
//			overy += startPoint.y - ymap.getMinIndex();
//		}
//		
//		startPoint.x -= overx;
//		endPoint.x -= overx;
//		startPoint.y -= overy;
//		endPoint.y -= overy;
//		
//		selectRectangle(startPoint, endPoint);
	}
	
	/**
	 * Zooming when the mouse wheel is used in conjunction with the shift key.
	 * Vertical scrolling if the shift key is not pressed.
	 */
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		
		int notches = e.getWheelRotation();
		int shift = 3;
		
//		Point mouseXY = MouseInfo.getPointerInfo().getLocation();
		
		if(!e.isShiftDown()) {
			if(notches < 0) {
				getYMap().scrollBy(-shift);
				
			} else {
				getYMap().scrollBy(shift);
			}
		} else {
			if(notches < 0) {
				getXMap().zoomIn();
				getYMap().zoomIn();
				
			} else {
				getXMap().zoomOut();
				getYMap().zoomOut();
			}
		}
		
		revalidate();
		repaint();
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
	 * When the right mouse button is used to select a rectangle in GlobalView,
	 * the view will be zoomed to the area that has been selected.
	 * @param start
	 * @param end
	 */
	public void selectAndZoom(Point start, Point end) {
		
		selectRectangle(start, end);
		
		//Zooming in when making a selection
		double scaleFactor = 1;
		
		int scrollX = 0; 
		double newScale;
		double currentXScale = getXMap().getScale();
		
		int scrollY = 0;
		double newScale2;
		double currentYScale = getYMap().getScale();
		
		if(currentXScale <= 10 && currentYScale <= 10) {
			scaleFactor = 2;
			
		} else if(currentXScale <= 30 && currentYScale <= 30) {
			scaleFactor = 1.5;
			
		} else {
			scaleFactor = 1;
		}
		
		newScale = currentXScale * scaleFactor;
		getXMap().setScale(newScale);
		
		newScale2 = currentYScale * scaleFactor;
		getYMap().setScale(newScale2);
		
		//Scrolling to remain in the selected area when updating scale (zoom)
		if(scaleFactor > 1) {
			scrollX = (end.x + start.x)/2;
			scrollY = (end.y + start.y)/2;
			
			getXMap().scrollToIndex(scrollX);
			getYMap().scrollToIndex(scrollY);
		}
	}
	
	public void setHeaders(HeaderInfo ghi, HeaderInfo ahi) {
		
		geneHI = ghi;
		arrayHI = ahi;
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentResized(ComponentEvent arg0) {
		
		xmap.setScale(xmap.getScale());
		//ymap.recalculateScale();
		ymap.setScale(ymap.getScale());
		
		this.repaint();
		this.revalidate();
	}

	@Override
	public void componentShown(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}


