package gui.labels;

import util.LogBuffer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Observable;
import java.util.prefs.Preferences;

public class ColumnLabelView extends LabelView {

	private static final long serialVersionUID = 1L;

	public ColumnLabelView() {

		super();
		labelAttr.setDefaultJustified(false);
	}

	@Override
	protected boolean labelAndScrollCoordsAreOpposite() {

		return(true);
	}

	/** This is only here for use by fudge factors that I suspect have to do with
	 * the rotation of the graphics.
	 * 
	 * @return boolean */
	@Override
	protected boolean isAColumnPane() {

		return(true);
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if(parentNode == null) {
			LogBuffer.println("Error: Could not find or create " +
				"ColumnLabelView node because parentNode was null.");
			return;
		}

		super.setConfigNode(parentNode.node(this.getClass().getSimpleName()));
	}

	@Override
	public int determineCursorPixelIndex(Point p) {

		debug("Cursor x coordinate relative to column labels: [" + p.x + "]", 8);
		return(p.x);
	}

	@Override
	public void orientLabelPane(Graphics2D g2d) {
		g2d.rotate(Math.PI * 3 / 2);
		g2d.translate(-offscreenSize.height,0);
	}

	@Override
	public void orientLabelPreview(Graphics2D g2d,final int maxLabelLen) {
		g2d.rotate(Math.PI * 3 / 2);
		g2d.translate(-maxLabelLen,0);
	}

	@Override
	public void orientLabelExport(Graphics2D g2d,final int xIndent,
		final int yIndent,final int longestLabelLen) {

		g2d.rotate(Math.PI * 3 / 2);

		//The following takes advantage of the fact that the trees are always
		//the same height, which it uses to get the row label width.  Note, the
		//whole canvas is rotating.  The labels' top-left corner is where the
		//top left of the column labels panel should be, but the labels were
		//horizontal.

		//After rotating counterclockwise from the top left point, we need to
		//move the labels down by the height of the trees (which brings the
		//bottom of the labels to the top of the image just off the canvas), the
		//amount of vertical indent they had before rotation (which brings the
		//bottom of the labels to the point where their top should be), and then
		//the length of the longest label (because they have a new topleft
		//corner).
		int topleft_vert_translation = -xIndent-yIndent-longestLabelLen;

		//After rotating counterclockwise from the top left point, we need to
		//move the labels back the whole way left by subtracting the yIndent and
		//then over to their original xIndent because the x indent got messed up
		//by the rotation.
		int topleft_horiz_translation = xIndent-yIndent;

		g2d.translate(topleft_vert_translation,topleft_horiz_translation);
	}

	@Override
	public void orientHintPane(Graphics2D g2d) {}

	@Override
	protected void setLabelPaneSize(int offscreenPrimarySize,
		int offscreenSecondarySize) {

		// Set the size of the scrollpane to match the longest string
		debug("Setting col pane height to [" + offscreenSecondarySize + "]", 6);
		setPreferredSize(new Dimension(	offscreenPrimarySize,
			offscreenSecondarySize));
		debug("Resizing col labels panel to [" +	offscreenPrimarySize + "x" +
					offscreenSecondarySize + "].", 1);
	}

	@Override
	protected String getSummaryName() {

		return("ColSummary");
	}

	@Override
	protected String getPaneType() {

		return("Column");
	}

	@Override
	public void setHoverPosition(final MouseEvent e) {

		map.setHoverPixel(e.getX());
	}

	/** This method is necessary to determine whether an indent offset is
	 * necessary for the start coordinate of the label. It is dependent on the
	 * isRightJustified data member of the parent class and whether the pane's
	 * position is on the left or top of the matrix
	 * 
	 * @param none
	 * @author rleach
	 * @return boolean */
	@Override
	protected boolean isMatrixJustified() {
		return(!labelAttr.isRightJustified());
	}

	/** This method should return true if the start of the label string is closer
	 * to the data matrix than the end of the label string. It is assumed that
	 * the pre-rotated x position of the start of the string is lesser than the
	 * pre-rotated x position of the end of the string. The value returned is
	 * used to infer that the scroll 0 position either corresponds to the string
	 * 0 position (true) or is oriented in the opposite direction (false). */
	@Override
	protected boolean isLabelStartNearMatrix() {

		return(true);
	}

	@Override
	public JScrollBar getPrimaryScrollBar() {

		return scrollPane.getHorizontalScrollBar();
	}

	@Override
	public JScrollBar getSecondaryScrollBar() {

		return scrollPane.getVerticalScrollBar();
	}

	@Override
	public int getPrimaryHoverPosition(final MouseEvent e) {

		return(e.getX());
	}

	@Override
	public void update(final Observable o, final Object arg) {
		if(o == map || // location changed
				o == drawSelection || o == otherSelection || // selection change
				o == labelSummary) { // annotation change
			selectionChanged();
		}
		else {
			LogBuffer.println("Warning: LabelView got funny update!");
		}
	}

	@Override
	protected int getPrimaryViewportSize() {

		return(scrollPane.getViewport().getSize().width);
	}

	@Override
	protected int getSecondaryViewportSize() {

		return(scrollPane.getViewport().getSize().height);
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
	protected void setSecondaryPaneSize(final Dimension dims, int Size) {

		secondaryPaneSize = Size;
		dims.height = Size;
	}

	@Override
	public int getLabelOrientation() {

		return(Adjustable.VERTICAL);
	}

	@Override
	protected boolean isASecondaryScroll(final MouseWheelEvent e) {

		return(!e.isShiftDown());
	}

	@Override
	protected void setDataTickerValue(MouseEvent e) {
		int colIdx = getPrimaryHoverIndex(e);
		ticker.setText("Column Average:");
		ticker.setValue(dataModel.getDataMatrix().getColAverage(colIdx, colIdx));
	}
}
