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

	public ColumnTreeView() {

		super(false);

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

	public void setATRHeaderInfo(final HeaderInfo atrHI) {
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
