/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: ATRView.java,v $
 * $Revision: 1.2 $
 * $Date: 2010-05-02 13:39:00 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by 
 * Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular,
 *
 * 1) If you modify a source file, make a comment in it containing your name 
 * and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the 
 * Java TreeView maintainers at alok@genome.stanford.edu when they make a 
 * useful addition. It would be nice if significant contributions could be 
 * merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER
 */
package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LinearTransformation;

/**
 * Draws an array tree to show the relations between arrays. This object
 * requires a MapContainer to figure out the offsets for the arrays.
 * Furthermore, it sets up a scrollbar to scroll the tree, although there is
 * currently no way to specify how large you would like the scrollable area to
 * be.
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.2 $ $Date: 2010-05-02 13:39:00 $
 */
public class ATRView extends TRView implements MouseListener, 
		MouseMotionListener {

	private static final long serialVersionUID = 1L;

	protected HeaderSummary headerSummary = new HeaderSummary("AtrSummary");

	private HeaderInfo atrHI;
	private final JScrollBar scrollbar;

	/** Constructor, sets up AWT components */
	public ATRView() {

		super();

		scrollbar = new JScrollBar(Adjustable.VERTICAL, 0, 1, 0, 1);
		
		// EDIT
		panel.add(scrollbar, BorderLayout.NORTH);
		
		isLeft = false;

		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
	}

	// method from ModelView
	/**
	 * Implements abstract method from ModelView. In this case, returns
	 * "ATRView".
	 * 
	 * @return name of this subclass of modelview
	 */
	@Override
	public String viewName() {

		return "ATRView";
	}

	// method from ModelView
	/**
	 * Gets the status attribute of the ATRView object. The status is some
	 * information which the user might find useful.
	 * 
	 * @return The status value
	 */
	@Override
	public String[] getStatus() {

		String[] status;
		if (selectedNode != null) {
			if (selectedNode.isLeaf()) {
				status = new String[2];
				status[0] = "Leaf Node " + selectedNode.getId();
				status[1] = "Pos " + selectedNode.getCorr();

			} else {
				final int[] nameIndex = getHeaderSummary().getIncluded();
				status = new String[nameIndex.length * 2];
				final String[] names = atrHI.getNames();
				for (int i = 0; i < nameIndex.length; i++) {
					status[2 * i] = names[nameIndex[i]] + ":";
					status[2 * i + 1] = " "
							+ atrHI.getHeader(atrHI.getHeaderIndex(
									selectedNode.getId()))[nameIndex[i]];
				}
			}
		} else {
			status = new String[2];
			status[0] = "Select node to ";
			status[1] = "view annotation.";
		}

		return status;
	}

	/** Setter for headerSummary */
	public void setHeaderSummary(final HeaderSummary headerSummary) {

		this.headerSummary = headerSummary;
	}

	/** Getter for headerSummary */
	public HeaderSummary getHeaderSummary() {

		return headerSummary;
	}
	
	public void setATRHeaderInfo(final HeaderInfo atrHI) {
		
		this.atrHI = atrHI;
	}
	
	/* inherit description */
	@Override
	public void updateBuffer(final Graphics g) {

		if (offscreenChanged) {
			offscreenValid = false;
		}

		if ((!offscreenValid) && (treePainter != null)) {
			map.setAvailablePixels(offscreenSize.width);

			// clear the pallette...
			g.setColor(this.getBackground());
			g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
			g.setColor(Color.black);

			// calculate Scaling
			destRect.setBounds(0, 0, map.getUsedPixels(), offscreenSize.height);
			xScaleEq = new LinearTransformation(map.getIndex(destRect.x),
					destRect.x, map.getIndex(destRect.x + destRect.width),
					destRect.x + destRect.width);
			yScaleEq = new LinearTransformation(treePainter.getCorrMin(),
					destRect.y, treePainter.getCorrMax(), destRect.y
							+ destRect.height);

			// draw
			treePainter.paint(g, xScaleEq, yScaleEq, destRect, selectedNode, isLeft);

		} else {
			// System.out.println("didn't update buffer: valid =
			// " + offscreenValid + " drawer = " + drawer);
		}
	}

	// Mouse Listener
	/**
	 * When a mouse is clicked, a node is selected.
	 */
	@Override
	public void mouseClicked(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive()) return;

		if (treePainter != null) {
			
			if(SwingUtilities.isLeftMouseButton(e)) {
				// the trick is translating back to the normalized space...
				setSelectedNode(treePainter.getClosest(
						xScaleEq.inverseTransform(e.getX()),
						yScaleEq.inverseTransform(e.getY()),
						// weight must have correlation slope on top
						yScaleEq.getSlope() / xScaleEq.getSlope()));
			} else {
				/* Sequence of these statements matters! */
				treeSelection.deselectAllIndexes();
				treeSelection.notifyObservers();
				setSelectedNode(null);
			}
		}
	}
	
	@Override
	public void mouseMoved(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive()) return;

		if (treePainter != null && treeSelection.getNSelectedIndexes() == 0) {
			// the trick is translating back to the normalized space...
			setHoveredNode(treePainter.getClosest(
					xScaleEq.inverseTransform(e.getX()),
					yScaleEq.inverseTransform(e.getY()),
					// weight must have correlation slope on top
					yScaleEq.getSlope() / xScaleEq.getSlope()));
		}
	}
	
	@Override
	public void mouseExited(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive()) return;

		setHoveredNode(null);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
}
