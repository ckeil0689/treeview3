/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: ZoomView.java,v $
 * $Revision: 1.2 $
 * $Date: 2008-03-09 21:06:33 $
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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Observable;
import javax.swing.JOptionPane;

import net.miginfocom.swing.MigLayout;

import edu.stanford.genetics.treeview.*;

/**
 * Implements zoomed in view of the data array
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version Alpha
 * 

 * The zoom view listens for mouse motion so that it can report status
 * and usage appropriately.

*/
class ZoomView extends ModelViewProduced implements MouseMotionListener, 
MouseWheelListener, KeyListener {
	
	private static final long serialVersionUID = 1L;
    
	protected TreeSelectionI geneSelection, arraySelection;
	private int overx, overy;
    private ArrayDrawer drawer;
    private String [] statustext = new String [] {"Mouseover Selection","",""};
    private Rectangle sourceRect = new Rectangle();
    private Rectangle destRect = new Rectangle();
    private MapContainer xmap, ymap;
	private HeaderInfo arrayHI, geneHI; // to get gene and array names...
	
    /**
     * Allocate a new ZoomView
     */
    public ZoomView(){
    	
		super();
		this.setLayout(new MigLayout());
		panel = this;
        
		setToolTipText("This Turns Tooltips On");
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
    }
	
	/**
	* showVal indicates whether the zoom should draw the value of each cell on 
	* the canvas on top of the corresponding square. Used to display 
	* IUPAC symbols for alignment view.
	*/
	boolean showVal = false;
	
	/**
	* showVal indicates whether the zoom should draw the value of each cell on 
	* the canvas on top of the corresponding square. Used to display 
	* IUPAC symbols for alignment view.
	*/
	public boolean getShowVal() {
		
		return showVal;
	}
	
	/**
	* showVal indicates whether the zoom should draw the value of each cell on 
	* the canvas on top of the corresponding square. Used to display IUPAC 
	* symbols for alignment view.
	*/
	public void setShowVal(boolean showVal) {
		
		this.showVal = showVal;
	}

	
    @Override
	public Dimension getPreferredSize() {
		
    	// return super.getPreferredSize();
		return new Dimension(xmap.getRequiredPixels(), 
				ymap.getRequiredPixels());
    }
    
    /** 
     * Set geneSelection
     *
     * @param geneSelection The TreeSelection which is set by 
     * selecting genes in the ZoomView
     */
    public void setGeneSelection(TreeSelectionI geneSelection) {
	  
    	if (this.geneSelection != null) {
    		this.geneSelection.deleteObserver(this);	
    	}
    	
    	this.geneSelection = geneSelection;
    	if (this.geneSelection != null) {
    		this.geneSelection.addObserver(this);
    	}
    }

	/** 
     * Set arraySelection
     *
     * @param arraySelection The TreeSelection which is set by selecting genes 
     * in the ZoomView
     */
    public void setArraySelection(TreeSelectionI arraySelection) {
	  
    	if (this.arraySelection != null) {
    		this.arraySelection.deleteObserver(this);	
    	}
	  
    	this.arraySelection = arraySelection;
    	if (this.arraySelection != null) {
    		this.arraySelection.addObserver(this);
    	}
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
		if (drawer != null) {
			drawer.addObserver(this);
		}
    }

    /** 
     * Get ArrayDrawer
     *
     * @return The current ArrayDrawer
     */
    public ArrayDrawer getArrayDrawer() {
    	
    	return drawer;
    }

    /**
     * set the xmapping for this view
     *
     * @param m   the new mapping
     */
    public void setXMap(MapContainer m) {
	
    	if (xmap != null) {
    		xmap.deleteObserver(this);	    
    	}
    	
    	xmap = m;
    	if (xmap != null) {
    		xmap.addObserver(this);
    	}
    }

    /**
     * set the ymapping for this view
     *
     * @param m   the new mapping
     */
    public void setYMap(MapContainer m) {
	
    	if (ymap != null)  {
    		ymap.deleteObserver(this);	    
    	}
    	
    	ymap = m;
    	if (ymap != null) {
    		ymap.addObserver(this);
    	}
    }

    /**
     * get the xmapping for this view
     *
     * @return   the current mapping
     */
    public MapContainer getXMap() {
    	
    	return xmap;
    }
    
    public MapContainer getZoomXmap() {
    	
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
    
    public MapContainer getZoomYmap() {
    	
    	return ymap;
    }

    // method from ModelView
    @Override
	public String viewName() { 
    	
    	return "ZoomView";
    }

    // method from ModelView
    @Override
	public String[] getStatus() {
    	
    	try {
			if (xmap.contains(overx) 
					&& ymap.contains(overy)) {
				statustext[0] = "Row:    " + (overy + 1);
				
				if (geneHI != null) {
					int realGene = overy;
					try {
						statustext[0] += " (" + geneHI.getHeader(realGene, 1) //WRONG FOR .TXT
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

	// method from ModelView
	@Override
	public void updateBuffer(Graphics g) {	
		
		if (offscreenChanged) {
			xmap.setAvailablePixels(offscreenSize.width);
			ymap.setAvailablePixels(offscreenSize.height);
			xmap.notifyObservers();
			ymap.notifyObservers();
		}
		
		if (offscreenValid == false) {
			
			// clear the pallette...
			g.setColor(Color.white);
			g.fillRect
			(0,0, offscreenSize.width, offscreenSize.height);
			g.setColor(Color.black);
			
			
			destRect.setBounds(0,0,xmap.getUsedPixels(), 
					ymap.getUsedPixels());
			
			sourceRect.setBounds(xmap.getIndex(0), ymap.getIndex(0),
			xmap.getIndex(destRect.width) - xmap.getIndex(0), 
			ymap.getIndex(destRect.height) - ymap.getIndex(0));
			
			if ((sourceRect.x >= 0) && (sourceRect.y >= 0)) {
				drawer.paint(g, sourceRect, destRect, null);
			}
		}
	}

	@Override
	public void paintComposite(Graphics g) {
			
		if (getShowVal()) {
				// need to draw values on screen!
				try {
					((CharArrayDrawer)drawer).paintChars(g, xmap, ymap, 
							destRect);
					
				} catch (Exception e) {
					JOptionPane.showMessageDialog(this, 
							"ZoomView had trouble compositing:" + e);
					setShowVal(false);
				}
			}
	}
    
	@Override
	public void updatePixels() {	
		
		if (offscreenChanged) {
			xmap.setAvailablePixels(offscreenSize.width);
			ymap.setAvailablePixels(offscreenSize.height);
			xmap.notifyObservers();
			ymap.notifyObservers();
		}
		
		if (offscreenValid == false) {
			
			destRect.setBounds(0,0,xmap.getUsedPixels(), ymap.getUsedPixels());
			
			sourceRect.setBounds(xmap.getIndex(0), ymap.getIndex(0),
			xmap.getIndex(destRect.width) - xmap.getIndex(0), 
			ymap.getIndex(destRect.height) - ymap.getIndex(0));
			
			if ((sourceRect.x >= 0) && (sourceRect.y >= 0)) {
				drawer.paint(offscreenPixels, sourceRect, destRect, 
						offscreenScanSize);
			}
			offscreenSource.newPixels();
		}
	}
	
    /**
     * Watch for updates from ArrayDrawer and the two maps
     * The appropriate response for both is to trigger a redraw.
     */
	 @Override
	public void update(Observable o, Object arg) {	
		 
		 if (o == drawer) {
			 //	    System.out.println("got drawer update");
			 offscreenValid = false;
			 
		 } else if ((o == xmap) || ( o == ymap)) {
			 offscreenValid = false;	
			 
		 } else if ((o == geneSelection) || (o == arraySelection)) {
			 /*
			 if (cdtSelection.getNSelectedArrays() == 0) {
				if (cdtSelection.getNSelectedGenes() != 0) {
					cdtSelection.selectAllArrays();
					cdtSelection.notifyObservers();
				}
			} else {
				*/
				// Hmm... it almost seems like you could get rid of the 
			 	//zoom map as a mechanism of communication... but not quite, 
			 	//because the globalview, textview and atrzview depend on it 
			 	//to know what is visible in the zoom window.
				MapContainer zoomXmap = getZoomXmap();
				MapContainer zoomYmap = getZoomYmap();
				zoomYmap.setIndexRange(geneSelection.getMinIndex(),  
						geneSelection.getMaxIndex());
				zoomXmap.setIndexRange(arraySelection.getMinIndex(), 
						arraySelection.getMaxIndex());
				
				zoomXmap.notifyObservers();
				zoomYmap.notifyObservers();

		 } else {
			 LogBuffer.println("ZoomView got weird update : " + o);
		 }
		 
		 if (offscreenValid == false) {
			 repaint();	
		 }
    }

    // MouseMotionListener
    @Override
	public void mouseMoved(MouseEvent e) {
    	
		int ooverx = overx;
		int oovery = overy;
		overx = xmap.getIndex(e.getX());
		overy = ymap.getIndex(e.getY());
		if (oovery != overy || ooverx != overx)
		    if (status != null) 
			status.setMessages(getStatus());
    }
    
    Point mousePt;
    @Override
    public void mousePressed(MouseEvent e) {
    	
    	mousePt = e.getPoint();
    	this.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {

    	this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

	@Override
	public String getToolTipText(MouseEvent e) {
		/* Do we want to do mouseovers if value already visible? 
		if (getShowVal()) return null; 
		// don't do tooltips and vals at same time.
		 */
		String ret = "";
		if (drawer != null) {
			int geneRow = overy;
			if (xmap.contains(overx) && ymap.contains(overy)) {
				if (drawer.isMissing(overx, geneRow)) {
					ret = "No data";
				} else if (drawer.isEmpty(overx, geneRow)) {
					ret = null;
				} else {
					int row = geneRow + 1;
					int col = overx + 1;
					ret = "Row: " + row + " Column: " + col + " Value: " 
					+ drawer.getSummary(overx, geneRow);
				}
			}
		}
		return ret;
	}
	
	public void setHeaders(HeaderInfo ghi, HeaderInfo ahi) {
		
		geneHI = ghi;
		arrayHI = ahi;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		
		int notches = e.getWheelRotation();
		int shift = 3;
		double zoomVal = 0.5;
		
		if(getZoomXmap().getScale() <= 1.0 
				&& getZoomYmap().getScale() <= 1.0) {
			zoomVal = 0.1;
			
		} else if(getZoomXmap().getScale() <= 0.1 
				&& getZoomYmap().getScale() <= 0.1) {
			zoomVal = 0.01;
		} 
		
		if(!e.isControlDown()) {
			if(notches < 0) {
				getZoomYmap().scrollBy(-shift);
				
			} else {
				getZoomYmap().scrollBy(shift);
			}
		} else {
			if(notches < 0) {
				getZoomXmap().setScale(getZoomXmap().getScale() + zoomVal);
				getZoomYmap().setScale(getZoomYmap().getScale() + zoomVal);
				
			} else {
				getZoomXmap().setScale(getZoomXmap().getScale() - zoomVal);
				getZoomYmap().setScale(getZoomYmap().getScale() - zoomVal);
			}
		}
	}

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
			getZoomXmap().scrollBy(-shift);
			break;
		case KeyEvent.VK_RIGHT:
			getZoomXmap().scrollBy(shift);
			break;
		case KeyEvent.VK_UP:
			getZoomYmap().scrollBy(-shift);
			break;
		case KeyEvent.VK_DOWN:
			getZoomYmap().scrollBy(shift);
			break;
		}		
	}
}

