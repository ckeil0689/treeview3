/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: GTRView.java,v $
 * $Revision: 1.1 $
 * $Date: 2006-08-16 19:13:46 $
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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.SwingUtilities;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LinearTransformation;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * Draws a gene tree to show the relations between genes
 * 
 * This object requires a MapContainer to figure out the offsets for the genes.
 */

public class GTRView extends TRView implements MouseListener, 
		MouseMotionListener {

	private static final long serialVersionUID = 1L;;

	protected HeaderSummary headerSummary = new HeaderSummary("GtrSummary");

	private HeaderInfo gtrHI;

	/**
	 * Constructor. You still need to specify a map to have this thing draw.
	 */
	public GTRView() {

		super();
		panel = this;
		
		isLeft = true;

		addMouseListener(this);
		addMouseMotionListener(this);
	}

	/**
	 * Setter for headerSummary
	 * 
	 */
	public void setHeaderSummary(final HeaderSummary headerSummary) {

		this.headerSummary = headerSummary;
	}

	/**
	 * Getter for headerSummary
	 */
	public HeaderSummary getHeaderSummary() {

		return headerSummary;
	}
	
	public void setGTRHeaderInfo(HeaderInfo gtrHI) {
		
		this.gtrHI = gtrHI;
	}

	// method from ModelView
	@Override
	public String viewName() {

		return "GTRView";
	}

	// method from ModelView
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
				final String[] names = gtrHI.getNames();

				for (int i = 0; i < nameIndex.length; i++) {
					status[2 * i] = names[nameIndex[i]] + ":";
					status[2 * i + 1] = " "
							+ gtrHI.getHeader(gtrHI.getHeaderIndex(
									selectedNode.getId()))[nameIndex[i]];
				}
			}
		} else {
			status = new String[2];
			status[0] = "Select Node to ";
			status[1] = "view annotation.";
		}
		return status;
	}

	// method from ModelView
	@Override
	public void updateBuffer(final Graphics g) {

		// System.out.println("GTRView updateBuffer() called offscreenChanged "
		// + offscreenChanged + " valid " + offscreenValid + " yScaleEq "
		// + getYScaleEq());
		if (offscreenChanged) {
			offscreenValid = false;
		}

		if (!offscreenValid && (treePainter != null)) {
			map.setAvailablePixels(offscreenSize.height);

			// clear the pallette...
			g.setColor(this.getBackground());
			g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
			g.setColor(Color.black);

			// calculate Scaling
			destRect.setBounds(0, 0, offscreenSize.width, map.getUsedPixels());
			setXScaleEq(new LinearTransformation(treePainter.getCorrMin(),
					destRect.x, treePainter.getCorrMax(), destRect.x
							+ destRect.width));

			setYScaleEq(new LinearTransformation(map.getIndex(destRect.y),
					destRect.y, map.getIndex(destRect.y + destRect.height),
					destRect.y + destRect.height));

			// System.out.println("yScaleEq " + getYScaleEq());
			// draw
			treePainter.paint(g, getXScaleEq(), getYScaleEq(), destRect,
					selectedNode, isLeft);

		} else {
			// System.out.println("didn't update buffer: valid = "
			// + offscreenValid + " drawer = " + drawer);
		}
	}

	// Mouse Listener
	@Override
	public void mouseClicked(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive()) return;

		if ((treePainter != null) && (getXScaleEq() != null)) {
			if(SwingUtilities.isLeftMouseButton(e)) {
				// the trick is translating back to the normalized space...
				setSelectedNode(treePainter.getClosest(
						getYScaleEq().inverseTransform(e.getY()), getXScaleEq()
								.inverseTransform(e.getX()), getXScaleEq()
								.getSlope() / getYScaleEq().getSlope()));
			} else {
				/* Sequence of these statements matters! */
				treeSelection.deselectAllIndexes();
				treeSelection.notifyObservers();
				setSelectedNode(null);
			}
		} else {
			if (treePainter == null) {
				LogBuffer.println("GTRView.mouseClicked() : drawer is null");
			}

			if (getXScaleEq() == null) {
				LogBuffer.println("GTRView.mouseClicked() : xscaleEq is null");
			}
		}
	}
	
	@Override
	public void mouseMoved(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive()) return;

		if (treePainter != null && treeSelection.getNSelectedIndexes() == 0) {
			// the trick is translating back to the normalized space...
			setHoveredNode(treePainter.getClosest(
					getYScaleEq().inverseTransform(e.getY()), getXScaleEq()
							.inverseTransform(e.getX()), getXScaleEq()
							.getSlope() / getYScaleEq().getSlope()));
		}
	}
	
	@Override
	public void mouseExited(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive()) return;

		setHoveredNode(null);
	}
}
