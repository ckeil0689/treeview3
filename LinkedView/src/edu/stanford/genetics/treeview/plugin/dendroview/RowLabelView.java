package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Adjustable;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.prefs.Preferences;

import javax.swing.JScrollBar;
import javax.swing.ScrollPaneConstants;

import Utilities.StringRes;
import edu.stanford.genetics.treeview.LogBuffer;

public class RowLabelView extends LabelView {

	private static final long serialVersionUID = 1L;

	public RowLabelView() {
		super();
		d_justified = true;
		zoomHint = StringRes.lbl_ZoomRowLabels;
	}

	protected boolean labelAndScrollCoordsAreOpposite() {
		return(false);
	}

	/**
	 * This is only here for use by fudge factors that I suspect have to do with
	 * the rotation of the graphics.
	 * @author rleach
	 * @param 
	 * @return boolean
	 */
	protected boolean isAColumnPane() {
		return(false);
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {
		if (parentNode != null) {
			super.setConfigNode(parentNode.node("RowLabelView"));
		} else {
			LogBuffer.println("Error: Could not find or create ArrayameView"
					+ "node because parentNode was null.");
			return;
		}
	}

	public void setSecondaryScrollBarPolicyAlways() {
		scrollPane.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
	}

	public void setSecondaryScrollBarPolicyNever() {
		scrollPane.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
	}

	public void setSecondaryScrollBarPolicyAsNeeded() {
		scrollPane.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
	}

	/**
	 * This method should return true if the start of the label string is closer
	 * to the data matrix than the end of the label string. It is assumed that
	 * the pre-rotated x position of the start of the string is lesser than the
	 * pre-rotated x position of the end of the string. The value returned is
	 * used to infer that the scroll 0 position either corresponds to the string
	 * 0 position (true) or is oriented in the opposite direction (false).
	 */
	protected boolean isLabelStartNearMatrix() {
		return(false);
	}

	public int getPrimaryHoverPosition(final MouseEvent e) {
		return(e.getY());
	}

	/**
	 * This method is necessary to determine whether an indent offset is
	 * necessary for the start coordinate of the label.  It is dependent on the
	 * isRightJustified data member of the parent class and whether the pane's
	 * position is on the left or top of the matrix
	 * @param none
	 * @author rleach
	 * @return boolean
	 */
	protected boolean isMatrixJustified() {
		return(isRightJustified);
	}

	@Override
	public JScrollBar getPrimaryScrollBar() {
		return scrollPane.getVerticalScrollBar();
	}

	@Override
	public JScrollBar getSecondaryScrollBar() {
		return scrollPane.getHorizontalScrollBar();
	}

	public void setHoverPosition(final MouseEvent e) {
		hoverPixel = e.getY();
	}

	public int determineCursorPixelIndex(Point p) {
		debug("Cursor y coordinate relative to column labels: [" + p.y + "]",8);
		return(p.y);
	}

	public void orientLabelPane(Graphics2D g2d) {}

	public void orientHintPane(Graphics2D g2d) {
		g2d.rotate(Math.PI * 3 / 2);
		g2d.translate(-getPrimaryViewportSize(),0);
	}

	protected int getPrimaryViewportSize() {
		return(scrollPane.getViewport().getSize().height);
	}

	protected String getPaneType() {
		return("Row");
	}

	protected String getSummary() {
		return("RowSummary");
	}

	protected void setLabelPaneSize(int offscreenPrimarySize,int offscreenSecondarySize) {
		//Set the size of the scrollpane to match the longest string
		setPreferredSize(new Dimension(offscreenSecondarySize,
		                               offscreenPrimarySize));
		debug("Resizing row labels panel to [" + offscreenSecondarySize + "x" +
			offscreenPrimarySize + "].",2);
	}

	protected int getSecondaryViewportSize() {
		return(scrollPane.getViewport().getSize().width);
	}

	protected int getSecondaryPaneSize(final Dimension dims) {
		return(dims.width);
	}

	protected int getPrimaryPaneSize(final Dimension dims) {
		return(dims.height);
	}

	protected void setSecondaryPaneSize(final Dimension dims,int Size) {
		secondaryPaneSize = Size;
		dims.width = Size;
	}

	public int getLabelOrientation() {
		return(Adjustable.HORIZONTAL);
	}

	protected boolean isASecondaryScroll(final MouseWheelEvent e) {
		return(e.isShiftDown());
	}
}