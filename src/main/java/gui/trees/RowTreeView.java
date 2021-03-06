/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package gui.trees;

import model.data.matrix.LinearTransformation;
import model.data.trees.TreeDrawerNode;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Draws a gene tree to show the relations between genes
 *
 * This object requires a MapContainer to figure out the offsets for the genes.
 */

public class RowTreeView extends TRView {

	private static final long serialVersionUID = 1L;;

	public RowTreeView() {

		super(true);

		this.isLeft = true;

		addMouseListener(this);
		addMouseMotionListener(this);
	}

	@Override
	public String viewName() {

		return "GTRView";
	}

	@Override
	protected int getSecondaryPaneSize(final Dimension dims) {
		
		return(dims.width);
	}

	@Override
	protected int getPrimaryPaneSize(final Dimension dims) {
		
		return(dims.height);
	}

	@Override
	protected void setWhizzingDestRectBounds() {
		destRect.setBounds(
			0,
			0,
			getSecondaryPaneSize(offscreenSize),
			getUsedWhizzingLength());
	}

	@Override
	protected void drawWhizBackground(final Graphics g) {
		g.fillRect(0,getWhizzingDestRectStart(),
			getSecondaryPaneSize(offscreenSize),getWhizzingDestRectLength());
	}

	/**
	 * When the whizzing labels are active, but the tree is drawn static/linked
	 * to the data instead of the labels, this is used to draw a different color
	 * background behind the portion of the tree the labels are shown for
	 * @param g - graphics object
	 * @param scaleEq - scaling factor object
	 */
	@Override
	protected void drawFittedWhizBackground(final Graphics g,
		LinearTransformation scaleEq) {

		if(map == null || map.getFirstVisibleLabel() < 0 ||
			map.getNumVisibleLabels() < 1 || scaleEq == null) {

			return;
		}

		g.fillRect(0,(int) scaleEq.transform(
			(double) map.getFirstVisibleLabel()),
			getSecondaryPaneSize(offscreenSize),
			(int) scaleEq.transform((double) map.getFirstVisible() +
				(double) map.getNumVisibleLabels()));
	}

	@Override
	protected int getWhizzingDestRectStart() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.y + map.getFirstVisibleLabelOffsetCapacity());
	}

	@Override
	protected int getWhizzingDestRectEnd() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.y + map.getFirstVisibleLabelOffsetCapacity() +
			destRect.height);
	}

	@Override
	protected void setFittedDestRectBounds() {
		destRect.setBounds(
			0,
			0,
			getSecondaryPaneSize(offscreenSize),
			map.getUsedPixels());
	}

	@Override
	protected int getFittedDestRectStart() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.y);
	}

	@Override
	protected int getFittedDestRectEnd() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.y + destRect.height);
	}

	@Override
	protected int getFittedDestRectLength() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.height);
	}

	@Override
	protected int getSecondaryDestRectStart() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.x);
	}

	@Override
	protected int getSecondaryDestRectEnd() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.x + destRect.width - 1);
	}

	@Override
	protected int getUsedWhizzingLength() {
		return(map.getUsedPixels() - map.getFirstVisibleLabelOffsetCapacity() -
			map.getLastVisibleLabelOffsetCapacity());
	}

	@Override
	protected void setPrimaryScaleEq(final LinearTransformation scaleEq) {
		setYScaleEq(scaleEq);
	}

	@Override
	protected LinearTransformation getPrimaryScaleEq() {
		return(getYScaleEq());
	}

	@Override
	protected void setSecondaryScaleEq(final LinearTransformation scaleEq) {
		setXScaleEq(scaleEq);
	}

	/**
	 * This method gets the closest internal node "as the crow flies"
	 * @param MouseEvent
	 * @return TreeDrawerNode
	 */
	@Override
	protected TreeDrawerNode getClosestNode(final MouseEvent e) {
		return(treePainter.getClosest(
			getYScaleEq().inverseTransform(e.getY()),
			getXScaleEq().inverseTransform(e.getX()),
			getXScaleEq().getSlope() / getYScaleEq().getSlope()));
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
			getXScaleEq().inverseTransform(e.getX())));
	}

	/**
	 * Returns the pixel index relative to the data
	 * @param mouse event e
	 * @return pixel index
	 */
	protected int getPrimaryPixelIndex(final MouseEvent e) {
		return(e.getY());
	}

	/**
	 * Determines whether the scroll is for scrolling the data or not
	 * @param mouse event e
	 * @return boolean
	 */
	@Override
	protected boolean isAPrimaryScroll(final MouseWheelEvent e) {
		
		return(!e.isShiftDown());
	}

	@Override
	protected void setExportPreviewScale(Rectangle dest) {

		/* Scale trees for complete painting */
		int firstVisIndex = map.getIndex(getFittedDestRectStart());
		int lastVisIndex  = map.getIndex(getFittedDestRectEnd());

		setPrimaryScaleEq(new LinearTransformation(
				firstVisIndex,
				dest.y,
				lastVisIndex,
				dest.y + dest.height));

		setSecondaryScaleEq(new LinearTransformation(
				treePainter.getCorrMin(),
				dest.x,
				treePainter.getCorrMax(),
				dest.x + dest.width));
	}
	
	@Override
	protected int getSnapShotDestRectStart(final Rectangle dest) {
		if(dest == null) {
			return(-1);
		}
		return(dest.y);
	}

	@Override
	protected int getSnapShotDestRectEnd(final Rectangle dest) {
		if(dest == null) {
			return(-1);
		}
		return(dest.y + dest.height);
	}

	@Override
	protected void setDataTickerValue(MouseEvent e) {
		int from = (int) hoveredNode.getLeftLeaf().getIndex();
		int to = (int) hoveredNode.getRightLeaf().getIndex();
		ticker.setText("Subtree Average:");
		ticker.setValue( dataModel.getDataMatrix().getRowAverage(from, to));
	}
}
