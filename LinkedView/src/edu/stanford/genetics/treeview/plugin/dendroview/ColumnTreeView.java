/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LinearTransformation;
import edu.stanford.genetics.treeview.TreeDrawerNode;

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
public class ColumnTreeView extends TRView {

	private static final long serialVersionUID = 1L;

	public ColumnTreeView() {

		super(false);

		isLeft = false;

		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);

		debug = 0;
		//14 = debug tree repaints linked to whizzing labels
		//15 = debug tree hover highlighting
		//16 = debug tree alignment
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

	public void setATRHeaderInfo(final HeaderInfo atrHI) {}

	@Override
	protected void setWhizzingDestRectBounds() {
		destRect.setBounds(
			0,
			0,
			getUsedWhizzingLength(),
			getSecondaryPaneSize(offscreenSize));
	}

	@Override
	protected int getWhizzingDestRectStart() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.x + map.getFirstVisibleLabelOffset());
	}

	@Override
	protected int getWhizzingDestRectEnd() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.x + map.getFirstVisibleLabelOffset() + destRect.width);
	}

	@Override
	protected void setFittedDestRectBounds() {
		destRect.setBounds(
			0,
			0,
			map.getUsedPixels(),
			getSecondaryPaneSize(offscreenSize));
	}

	@Override
	protected int getFittedDestRectStart() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.x);
	}

	@Override
	protected int getFittedDestRectEnd() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.x + destRect.width);
	}

	@Override
	protected int getFittedDestRectLength() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.width);
	}

	@Override
	protected int getSecondaryDestRectStart() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.y);
	}

	@Override
	protected int getSecondaryDestRectEnd() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.y + destRect.height);
	}

	@Override
	protected int getUsedWhizzingLength() {
		return(map.getUsedPixels() - map.getFirstVisibleLabelOffset() -
			map.getLastVisibleLabelOffset());
	}

	@Override
	protected int getSecondaryPaneSize(final Dimension dims) {
		
		return(dims.height);
	}

	@Override
	protected int getPrimaryPaneSize(final Dimension dims) {
		
		return(dims.width);
	}

	@Override
	protected void setPrimaryScaleEq(final LinearTransformation scaleEq) {
		setXScaleEq(scaleEq);
	}

	@Override
	protected void setSecondaryScaleEq(final LinearTransformation scaleEq) {
		setYScaleEq(scaleEq);
	}

	/**
	 * This method gets the closest internal node "as the crow flies"
	 * @param MouseEvent
	 * @return TreeDrawerNode
	 */
	@Override
	protected TreeDrawerNode getClosestNode(final MouseEvent e) {
		return(treePainter.getClosest(
			getXScaleEq().inverseTransform(e.getX()),
			getYScaleEq().inverseTransform(e.getY()),
			getYScaleEq().getSlope() / getXScaleEq().getSlope()));
	}

	/**
	 * This method figures out the leaf at the current cursor position and
	 * searches up the tree in a direct heirarchical line to find the parent
	 * node which the cursor has just passed (before it reaches the next parent
	 * node)
	 */
	@Override
	protected TreeDrawerNode getClosestParentNode(final MouseEvent e) {
		int i = getPrimaryHoverIndex();
		if(i < 0) {
			return(null);
		}
		return(treePainter.getClosestParent(treePainter.getLeaf(i),
			getYScaleEq().inverseTransform(e.getY())));
	}

	protected int getPrimaryPixelIndex(final MouseEvent e) {
		return(e.getX());
	}
}
