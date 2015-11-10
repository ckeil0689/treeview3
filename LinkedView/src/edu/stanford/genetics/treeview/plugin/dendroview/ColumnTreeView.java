/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Observer;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

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

	public void setATRHeaderInfo(final HeaderInfo atrHI) {
	}

	@Override
	public void updateBuffer(final Graphics g) {

		if (treePainter == null) {
			return;
		}

		if (offscreenChanged) {
			offscreenValid = false;
		}

		debug("updateBuffer called for column trees",14);

		updateTreeRepaintTimers();

		if(!offscreenValid || map.isLabelAnimeRunning()) {
			map.setAvailablePixels(offscreenSize.width);

			/* clear the panel */
			g.setColor(this.getBackground());
			g.fillRect(0, 0, offscreenSize.width, offscreenSize.height);
			g.setColor(Color.black);

			int firstVisIndex;
			int lastVisIndex;

			//If we're in label port/whizzing label mode
			if(map.isLabelAnimeRunning() && map.overALabelPortLinkedView() &&
				!map.shouldKeepTreeGlobal() &&
				map.getFirstVisibleLabel() > -1 &&
				map.getLastVisibleLabel() > -1) {

				/* calculate scaling */
				destRect.setBounds(
					0,
					0,
					map.getUsedPixels() -
					map.getFirstVisibleLabelOffset() -
					map.getLastVisibleLabelOffset(),
					offscreenSize.height);
				
				debug("pixels used [" + map.getUsedPixels() + "] left offset [" + map.getFirstVisibleLabelOffset() + "] right offset [" + map.getLastVisibleLabelOffset() + "]",16);

				firstVisIndex = map.getFirstVisibleLabel();
				lastVisIndex  = map.getLastVisibleLabel();

				debug("first visible label index [" + firstVisIndex + "] last visible label index [" + lastVisIndex + "]",16);
				debug("destRect.x [" + destRect.x + "] map.getFirstVisibleLabelOffset() [" + map.getFirstVisibleLabelOffset() + "] destRect.width [" + destRect.width + "]",16);

				xScaleEq = new LinearTransformation(
					firstVisIndex,
					destRect.x + map.getFirstVisibleLabelOffset(),
					lastVisIndex + 1,
					destRect.x + map.getFirstVisibleLabelOffset() + destRect.width);

				map.setLastTreeModeGlobal(false);
			} else {
				/* calculate scaling */
				destRect.setBounds(0, 0, map.getUsedPixels(), offscreenSize.height);

				firstVisIndex = map.getIndex(destRect.x);
				lastVisIndex  = map.getIndex(destRect.x + destRect.width);

				xScaleEq = new LinearTransformation(
					firstVisIndex,
					destRect.x,
					lastVisIndex + 1,
					destRect.x + destRect.width);

				map.setLastTreeModeGlobal(true);
			}

			yScaleEq = new LinearTransformation(treePainter.getCorrMin(),
					destRect.y, treePainter.getCorrMax(), destRect.y
							+ destRect.height);

			/* draw trees */
			treePainter.paint(g, xScaleEq, yScaleEq, destRect, selectedNode,
					isLeft);

			/* Repaint the hovered node */
			repaintHoveredNode();
		} else {
			// System.out.println("didn't update buffer: valid =
			// " + offscreenValid + " drawer = " + drawer);
		}
	}

	//This is an attempt to get the hovering of the mouse over the matrix to get
	//the tree panes to update more quickly and regularly, as the
	//notifyObservers method called from MapContainer was resulting in sluggish
	//updates
	private int repaintInterval = 50;      //update every 50 milliseconds
	private int slowRepaintInterval = 1000;//update every 1s if mouse not moving
	private int lastHoverIndex = -1;
	private Timer repaintTimer =
		new Timer(repaintInterval,
		          new ActionListener() {
			/**
			 * The timer "ticks" by calling
			 * this method every _timeslice
			 * milliseconds
			 */
			@Override
			public void
			actionPerformed(ActionEvent e) {
				//This shouldn't be necessary, but when I change setPoints() to
				//setTemporaryPoints in the drawing of the HINT, the timer never
				//stops despite stop being continually called, so I'm going to
				//call stop in here if the map says that the animation is
				//supposed to have been stopped...
				if(!map.isLabelAnimeRunning()) {
					repaintTimer.stop();
				}
				debug("Repainting column tree",14);
				repaint();
			}
		});

	//Timer to wait a bit before slowing down the slice _timer for painting.
	//This conserves processor cycles in the interests of performance.  Note
	//that there is a pair of timers for each axis.
	final private int delay = 1000;
	private javax.swing.Timer slowDownRepaintTimer;
	ActionListener slowDownRepaintListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent evt) {
			if(evt.getSource() == slowDownRepaintTimer) {
				/* Stop timer */
				slowDownRepaintTimer.stop();
				slowDownRepaintTimer = null;

				//If we are still over a label port view panel, just slow the
				//repaint timer, because this was triggered by the mouse not
				//moving
				if(map.overALabelPortLinkedView()) {
					debug("Slowing the repaint interval presumably because " +
					      "of lack of mouse movement",9);
					repaintTimer.setDelay(slowRepaintInterval);
				} else {
					repaintTimer.stop();
					map.setLabelAnimeRunning(false);
				}
			}
		}
	};

	public void updateTreeRepaintTimers() {
		//If the mouse is not hovering over the IMV, stop both timers, set the
		//last hover index, and tell mapcontainer that the animation has stopped
		if(!map.overALabelPortLinkedView()) {
			if(repaintTimer != null && repaintTimer.isRunning()) {
				debug("Not hovering over a label port linked view - stopping animation",9);
				repaintTimer.stop();
				lastHoverIndex = -1;
				//Disable the turnOffRepaintTimer if it is running, because we've
				//already stopped repaints
				if(slowDownRepaintTimer != null) {
					slowDownRepaintTimer.stop();
					slowDownRepaintTimer = null;
				}
			} else {
				debug("The repaint timer is not running. This updateBuffer " +
					"call was initiated by something else.",9);
			}
		}
		//Else, assume the mouse is hovering, and if the animation is not
		//running, start it up
		else if(!map.isLabelAnimeRunning()) {
			if(repaintTimer == null || !repaintTimer.isRunning()) {
				debug("Hovering across matrix - starting up animation",9);
				repaintTimer.start();
				lastHoverIndex = getPrimaryHoverIndex();
				//Disable any slowDownRepaintTimer that might have been left over
				if(slowDownRepaintTimer != null) {
					slowDownRepaintTimer.stop();
					slowDownRepaintTimer = null;
				}
			} else {
				debug("The repaint timer was in fact running even though map.isLabelAnimeRunning() said it wasn't.",9);
			}
		}
		//Else if the mouse hasn't moved, start the second timer to slow down
		//the first after 1 second (this mitigates delays upon mouse motion
		//after a brief period of no motion)
		else if(map.overALabelPortLinkedView() &&
			getPrimaryHoverIndex() == lastHoverIndex) {
			if(repaintTimer.getDelay() == repaintInterval) {
				debug("Hovering on one spot [" + lastHoverIndex +
				      "] - slowing animation",9);
				if(slowDownRepaintTimer == null) {
					slowDownRepaintTimer = new Timer(delay,slowDownRepaintListener);
					slowDownRepaintTimer.start();
				}
			} else {
				debug("Animation already slowed down to [" + repaintTimer.getDelay() + "ms].",9);
			}
		}
		//Else, disable the slowDownRepaintTimer, update the hover index, and
		//set the repaint interval to normal speed
		else {
			debug("Hovering across matrix - keeping animation going",9);
			debug("Last hover Index: [" + lastHoverIndex +
				"] current hover index [" + getPrimaryHoverIndex() + "]",9);
			if(repaintTimer != null && !repaintTimer.isRunning()) {
				repaintTimer.start();
			} else if(repaintTimer.getDelay() == slowRepaintInterval) {
				debug("Speeding up the repaint interval because mouse " +
				      "movement detected",9);
				repaintTimer.setDelay(repaintInterval);
				repaintTimer.restart();
			}
			//Disable the slowDownRepaintTimer because we have detected
			//continued mouse motion
			if(slowDownRepaintTimer != null) {
				slowDownRepaintTimer.stop();
				slowDownRepaintTimer = null;
			}
			lastHoverIndex = getPrimaryHoverIndex();
		}
	}

	public int getPrimaryHoverIndex() {
		return(map.getHoverIndex());
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
	public void mouseEntered(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive())
			return;

		if(map.wasLastTreeModeGlobal()) {
			map.setKeepTreeGlobal(true);
		}
		map.setOverTree(true);
	}

	@Override
	public void mouseExited(final MouseEvent e) {

		if (!isEnabled() || !enclosingWindow().isActive())
			return;

		map.setKeepTreeGlobal(false);
		map.setOverTree(false);
		setHoveredNode(null);
	}
}
