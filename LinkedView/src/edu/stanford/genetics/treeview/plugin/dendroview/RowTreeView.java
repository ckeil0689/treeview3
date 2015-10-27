/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
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
		panel = this;

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
	public String[] getStatus() {

		String[] status;
		// <<<<<<< HEAD
		// if (selectedNode != null) {
		// if (selectedNode.isLeaf()) {
		// status = new String[2];
		// status[0] = "Leaf Node " + selectedNode.getId();
		// status[1] = "Pos " + selectedNode.getCorr();
		//
		// } else {
		// final int[] nameIndex = getHeaderSummary().getIncluded();
		// status = new String[nameIndex.length * 2];
		// final String[] names = gtrHI.getNames();
		//
		// for (int i = 0; i < nameIndex.length; i++) {
		// status[2 * i] = names[nameIndex[i]] + ":";
		// status[2 * i + 1] = " "
		// + gtrHI.getHeader(gtrHI.getHeaderIndex(selectedNode
		// .getId()))[nameIndex[i]];
		// }
		// }
		// } else {
		// status = new String[2];
		// status[0] = "Select Node to ";
		// status[1] = "view annotation.";
		// }
		// =======
		// if (selectedNode != null) {
		// if (selectedNode.isLeaf()) {
		// status = new String[2];
		// status[0] = "Leaf Node " + selectedNode.getId();
		// status[1] = "Pos " + selectedNode.getCorr();
		//
		// } else {
		// final int[] nameIndex = getHeaderSummary().getIncluded();
		// status = new String[nameIndex.length * 2];
		// final String[] names = gtrHI.getNames();
		//
		// for (int i = 0; i < nameIndex.length; i++) {
		// status[2 * i] = names[nameIndex[i]] + ":";
		// status[2 * i + 1] = " "
		// + gtrHI.getHeader(gtrHI.getHeaderIndex(
		// selectedNode.getId()))[nameIndex[i]];
		// }
		// }
		// } else {
		// status = new String[2];
		// status[0] = "Select Node to ";
		// status[1] = "view annotation.";
		// }

		/* TODO temporary solution until we decide what info to display" */
		status = new String[3];

		// >>>>>>> bugFix
		return status;
	}

	@Override
	public void updateBuffer(final Graphics g) {

		if (treePainter == null)
			return;

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
