/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

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

		this.isLeft = false;

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

	@Override
	protected void setWhizzingDestRectBounds() {
		destRect.setBounds(
			0,
			0,
			getUsedWhizzingLength(),
			getSecondaryPaneSize(offscreenSize));
	}

	@Override
	protected void drawWhizBackground(final Graphics g) {
		g.fillRect(getWhizzingDestRectStart(),0,getWhizzingDestRectLength(),
			getSecondaryPaneSize(offscreenSize));
	}

	@Override
	protected void drawFittedWhizBackground(final Graphics g,
		LinearTransformation scaleEq) {

		if(map == null || map.getFirstVisibleLabel() < 0 ||
			map.getNumVisibleLabels() < 1 || scaleEq == null) {

			return;
		}

		g.fillRect((int) scaleEq.transform((double) map.getFirstVisibleLabel()),
			0,(int) scaleEq.transform((double) map.getFirstVisible() +
				(double) map.getNumVisibleLabels()),
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
		return(destRect.y + destRect.height - 1);
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
	protected LinearTransformation getPrimaryScaleEq() {
		return(getXScaleEq());
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
	 * searches up the tree in a direct hierarchical line to find the parent
	 * node which the cursor has just passed (before it reaches the next parent
	 * node)
	 * @param mouse event e
	 * @return closest parent node
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

	/**
	 * Returns the pixel index relative to the data
	 * @param mouse event e
	 * @return pixel index
	 */
	protected int getPrimaryPixelIndex(final MouseEvent e) {
		return(e.getX());
	}

	/**
	 * Determines whether the scroll is for scrolling the data or not
	 * @param mouse event e
	 * @return boolean
	 */
	@Override
	protected boolean isAPrimaryScroll(final MouseWheelEvent e) {
		return(e.isShiftDown());
	}
	
	@Override
	protected void setExportPreviewScale(Rectangle dest) {
		
		/* Scale trees for complete painting */
		int firstVisIndex = map.getIndex(getFittedDestRectStart());
		int lastVisIndex  = map.getIndex(getFittedDestRectEnd());

		setPrimaryScaleEq(new LinearTransformation(
			firstVisIndex,
			dest.x,
			lastVisIndex,
			dest.x + dest.width));
		
		setSecondaryScaleEq(new LinearTransformation(
				treePainter.getCorrMin(),
				dest.y,
				treePainter.getCorrMax(),
				dest.y + dest.height));
	}
	
	@Override
	protected int getSnapShotDestRectStart(final Rectangle dest) {
		if(dest == null) {
			return(-1);
		}
		return(dest.x);
	}

	@Override
	protected int getSnapShotDestRectEnd(final Rectangle dest) {
		if(dest == null) {
			return(-1);
		}
		return(dest.x + dest.width);
	}

	@Override
	protected void setDataTickerValue(MouseEvent e) {
		int from = (int) hoveredNode.getLeftLeaf().getIndex();
		int to = (int) hoveredNode.getRightLeaf().getIndex();
		ticker.setValue( dataModel.getDataMatrix().getColAverage(from, to) + " [tree ave]");
	}
	
}
