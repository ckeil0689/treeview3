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
public class ColumnTreeView extends TRView implements MouseMotionListener,
		MouseListener {

	private static final long serialVersionUID = 1L;

	private HeaderInfo atrHI;
	private final JScrollBar scrollbar;

	public ColumnTreeView() {

		super(false);

		scrollbar = new JScrollBar(Adjustable.VERTICAL, 0, 1, 0, 1);

		panel.add(scrollbar, BorderLayout.NORTH);

		isLeft = false;

		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
	}

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

	/**
	 * Gets the status attribute of the ATRView object. The status is some
	 * information which the user might find useful.
	 *
	 * @return The status value
	 */
	@Override
	public String[] getStatus() {

		String[] status;
//<<<<<<< HEAD
//		if (selectedNode != null) {
//			if (selectedNode.isLeaf()) {
//				status = new String[2];
//				status[0] = "Leaf Node " + selectedNode.getId();
//				status[1] = "Pos " + selectedNode.getCorr();
//
//			} else {
//				final int[] nameIndex = getHeaderSummary().getIncluded();
//				status = new String[nameIndex.length * 2];
//				final String[] names = atrHI.getNames();
//				for (int i = 0; i < nameIndex.length; i++) {
//					status[2 * i] = names[nameIndex[i]] + ":";
//					status[2 * i + 1] = " "
//							+ atrHI.getHeader(atrHI.getHeaderIndex(selectedNode
//									.getId()))[nameIndex[i]];
//				}
//			}
//		} else {
//			status = new String[2];
//			status[0] = "Select node to ";
//			status[1] = "view annotation.";
//		}
//=======
//		if (selectedNode != null) {
//			if (selectedNode.isLeaf()) {
//				status = new String[2];
//				status[0] = "Leaf Node " + selectedNode.getId();
//				status[1] = "Pos " + selectedNode.getCorr();
//
//			} else {
//				final int[] nameIndex = getHeaderSummary().getIncluded();
//				status = new String[nameIndex.length * 2];
//				final String[] names = atrHI.getNames();
//				for (int i = 0; i < nameIndex.length; i++) {
//					status[2 * i] = names[nameIndex[i]] + ":";
//					status[2 * i + 1] = " "
//							+ atrHI.getHeader(atrHI.getHeaderIndex(
//									selectedNode.getId()))[nameIndex[i]];
//				}
//			}
//		} else {
//			status = new String[2];
//			status[0] = "Select node to ";
//			status[1] = "view annotation.";
//		}
		
		/* TODO temporary solution until we decide what info to display" */
		status = new String[3];
//>>>>>>> bugFix

		return status;
	}

	public void setATRHeaderInfo(final HeaderInfo atrHI) {

		this.atrHI = atrHI;
	}

	@Override
	public void updateBuffer(final Graphics g) {

		if (treePainter == null)
			return;

		if (offscreenChanged) {
			offscreenValid = false;
		}

		if (!offscreenValid) {
			map.setAvailablePixels(offscreenSize.width);

			/* clear the panel */
			g.setColor(this.getBackground());
			g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
			g.setColor(Color.black);

			/* calculate scaling */
			destRect.setBounds(0, 0, map.getUsedPixels(), offscreenSize.height);
			xScaleEq = new LinearTransformation(map.getIndex(destRect.x),
					destRect.x, map.getIndex(destRect.x + destRect.width),
					destRect.x + destRect.width);
			yScaleEq = new LinearTransformation(treePainter.getCorrMin(),
					destRect.y, treePainter.getCorrMax(), destRect.y
					+ destRect.height);

			/* draw trees */
			treePainter.paint(g, xScaleEq, yScaleEq, destRect, selectedNode,
					isLeft);

		} else {
			// System.out.println("didn't update buffer: valid =
			// " + offscreenValid + " drawer = " + drawer);
		}
	}

	/**
	 * When a mouse is clicked, a node is selected.
	 */
	@Override
	public void mouseClicked(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive())
			return;
		if (treePainter == null)
			return;

		if (SwingUtilities.isLeftMouseButton(e)) {
			setSelectedNode(treePainter.getClosest(
					xScaleEq.inverseTransform(e.getX()),
					yScaleEq.inverseTransform(e.getY()), yScaleEq.getSlope()
							/ xScaleEq.getSlope()));
		} else {
			treeSelection.deselectAllIndexes();
			treeSelection.notifyObservers();
		}
	}

	@Override
	public void mouseMoved(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive())
			return;
		if (treePainter == null)
			return;

		// if (treeSelection.getNSelectedIndexes() == 0) {
		setHoveredNode(treePainter.getClosest(
				xScaleEq.inverseTransform(e.getX()),
				yScaleEq.inverseTransform(e.getY()), yScaleEq.getSlope()
						/ xScaleEq.getSlope()));
		// }
	}

	@Override
	public void mouseDragged(final MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive())
			return;

		setHoveredNode(null);
	}
}
