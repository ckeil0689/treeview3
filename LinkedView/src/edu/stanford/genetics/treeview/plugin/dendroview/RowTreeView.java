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
import edu.stanford.genetics.treeview.LinearTransformation;

/**
 * Draws a gene tree to show the relations between genes
 *
 * This object requires a MapContainer to figure out the offsets for the genes.
 */

public class RowTreeView extends TRView implements MouseMotionListener,
		MouseListener {

	private static final long serialVersionUID = 1L;;

	public RowTreeView() {

		super(true);

		isLeft = true;

		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void setGTRHeaderInfo(final HeaderInfo gtrHI) {
	}

	@Override
	public String viewName() {

		return "GTRView";
	}

	@Override
	public void updateBuffer(final Graphics g) {

		if (treePainter == null) {
			return;
		}

		if (offscreenChanged) {
			offscreenValid = false;
		}

		if (!offscreenValid) {
			map.setAvailablePixels(offscreenSize.height);

			/* clear the panel */
			g.setColor(this.getBackground());
			g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
			g.setColor(Color.black);

			/* calculate Scaling */
			destRect.setBounds(0, 0, offscreenSize.width, map.getUsedPixels());
			setXScaleEq(new LinearTransformation(treePainter.getCorrMin(),
					destRect.x, treePainter.getCorrMax(), destRect.x
							+ destRect.width));

			setYScaleEq(new LinearTransformation(map.getIndex(destRect.y),
					destRect.y, map.getIndex(destRect.y + destRect.height),
					destRect.y + destRect.height));

			/* draw trees */
			treePainter.paint(g, getXScaleEq(), getYScaleEq(), destRect,
					selectedNode, isLeft);

		} else {
			// System.out.println("didn't update buffer: valid = "
			// + offscreenValid + " drawer = " + drawer);
		}
	}

	@Override
	public void mouseClicked(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive())
			return;
		if (treePainter == null)
			return;

		if (SwingUtilities.isLeftMouseButton(e)) {
			setSelectedNode(treePainter.getClosest(getYScaleEq()
					.inverseTransform(e.getY()), getXScaleEq()
					.inverseTransform(e.getX()), getXScaleEq().getSlope()
					/ getYScaleEq().getSlope()));
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

		setHoveredNode(treePainter.getClosest(
				getYScaleEq().inverseTransform(e.getY()), getXScaleEq()
						.inverseTransform(e.getX()), getXScaleEq().getSlope()
						/ getYScaleEq().getSlope()));
	}

	@Override
	public void mouseExited(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive())
			return;

		setHoveredNode(null);
	}

	@Override
	public void mouseDragged(final MouseEvent e) {
		// TODO Auto-generated method stub

	}
}
