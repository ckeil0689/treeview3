/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: ModelView.java,v $
 * $Revision: 1.14 $
 * $Date: 2005-12-05 05:27:53 $
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
package edu.stanford.genetics.treeview;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Observer;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * superclass, to hold info and code common to all model views
 *
 * This currently consists of buffer management, status and hints panels.
 * Interestingly, but necessarily, it has no dependency on any models.
 */
public abstract class ModelView extends JPanel implements Observer,
		MouseListener {

	private static final long serialVersionUID = 1L;

	protected ViewFrame viewFrame = null;
	protected JFrame applicationFrame = null;

	protected boolean hasMouse = false;

	/* here so that subclass will work with BufferedModelView too */
	protected boolean offscreenValid = false;
	protected boolean offscreenChanged = false;
	protected Dimension offscreenSize = null;

	/**
	 * holds actual thing to be displayed...
	 */
	protected JComponent panel;

	/* Stores whether to draw a port around squares corresponding to what's
	 * shown in the label pane(s) */
	protected boolean labelPortMode = false;

	public int debug; //This is a verbosity level

	protected ModelView() {

		super(false);
	}

	/**
	 * viewName, returns name of view suitable for printing perhaps this should
	 * be replaced by reflection?
	 *
	 * @return String containing name of view.
	 */
	public abstract String viewName();

	public void setViewFrame(final ViewFrame m) {

		viewFrame = m;
		applicationFrame = m.getAppFrame();
	}

	public ViewFrame getViewFrame() {

		return viewFrame;
	}

	public JComponent getComponent() {

		return panel;
	}

	/**
	 * Update the double buffer, if buffered. Otherwise, just called by
	 * paintComponent to paint the main component.
	 *
	 * called only when offscreen buffer is marked as invalid, or if the
	 * onscreen size has changed.
	 *
	 *
	 * note: now actually called by paintcomponent to update the swing double
	 * buffer.
	 */
	abstract protected void updateBuffer(Graphics g);

	/**
	 * This is a stub so that components which work with this will also work
	 * with the ModelViewBuffered. importantly, no buffer is ever actually
	 * allocated. This method is used on the zoomed Dendrograms (ATRZoomView +
	 * GTRZoomView).
	 */
	@Override
	public synchronized void paintComponent(final Graphics g) {

		// Call JComponent's paintComponent method to clear panel
		// before every redraw.
		Graphics2D g2d = (Graphics2D) g;

		super.paintComponent(g2d);

		final Rectangle clip = g2d.getClipBounds();
		g2d.setColor(this.getBackground());
		g2d.fillRect(clip.x, clip.y, clip.width, clip.height);

		final Dimension reqSize = getSize();
		if (reqSize == null) {
			return;
		}

		// monitor size changes
		if ((offscreenSize == null) || (reqSize.width != offscreenSize.width)
				|| (reqSize.height != offscreenSize.height)) {

			offscreenChanged = true;
			offscreenSize = reqSize;
		}

		if (isEnabled()) {
			offscreenValid = false;
			updateBuffer(g2d);
			paintComposite(g2d);
		}
	}

	/**
	 * This call is to be used to add a quick addition to the component which
	 * you don't want to put on the doublebuffer. The composite could
	 * potentially be another buffer.
	 *
	 * Currently, this is only used by globalview for adding the zoom rect and
	 * focus rect.
	 */
	public void paintComposite(final Graphics g) {

		return;
	}

	@Override
	public void addNotify() {

		super.addNotify();
	}

	public Window enclosingWindow() {

		Object f = getParent();

		while (!(f instanceof Window)) {
			f = ((Component) f).getParent();
		}

		return (Window) f;
	}

	/**
	 * This does the following: 1) requests focus 2) sets status and hint panels
	 * appropriately 3) keeps track of whether we have the mouse.
	 */
	@Override
	public void mouseEntered(final MouseEvent e) {

		if (viewFrame == null) {
			LogBuffer.println("viewFrame null in ModelView.mouseEntered. "
					+ "Instance " + this);
			return;
		}

		hasMouse = true;
	}

	/**
	 * keeps track of when mouse not present.
	 */
	@Override
	public void mouseExited(final MouseEvent e) {

		hasMouse = false;
	}

	/* a bunch of stubs so we can claim to be a MouseListener */
	@Override
	public void mouseClicked(final MouseEvent e) {
	}

	@Override
	public void mousePressed(final MouseEvent e) {
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
	}

	public boolean inLabelPortMode() {
		
		return(labelPortMode);
	}

	public void setLabelPortMode(boolean m) {
		
		labelPortMode = m;
	}

	public void debug(String msg, int level) {
		
		if(level == debug) {
			LogBuffer.println(msg);
		}
	}
}
