/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.SwingUtilities;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LinearTransformation;
import edu.stanford.genetics.treeview.TreeDrawerNode;

/**
 * Draws a gene tree to show the relations between genes
 *
 * This object requires a MapContainer to figure out the offsets for the genes.
 */

public class RowTreeView extends TRView {

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
	protected int getWhizzingDestRectStart() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.y + map.getFirstVisibleLabelOffset());
	}

	@Override
	protected int getWhizzingDestRectEnd() {
		if(destRect == null) {
			return(-1);
		}
		return(destRect.y + map.getFirstVisibleLabelOffset() + destRect.height);
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
		return(destRect.x + destRect.width);
	}

	@Override
	protected int getUsedWhizzingLength() {
		return(map.getUsedPixels() - map.getFirstVisibleLabelOffset() -
			map.getLastVisibleLabelOffset());
	}

	@Override
	protected void setPrimaryScaleEq(final LinearTransformation scaleEq) {
		setYScaleEq(scaleEq);
	}

	@Override
	protected void setSecondaryScaleEq(final LinearTransformation scaleEq) {
		setXScaleEq(scaleEq);
	}

	@Override
	protected TreeDrawerNode getClosestNode(final MouseEvent e) {
		return(treePainter.getClosest(
			getYScaleEq().inverseTransform(e.getY()),
			getXScaleEq().inverseTransform(e.getX()),
			getXScaleEq().getSlope() / getYScaleEq().getSlope()));
	}
}
