/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.core.Debug;

/**
 * A Panel which is divided into a rectangular grid which the user can change.
 * Components may be added to the grid, and may take up any rectangular set of
 * cells within the grid. The user may drag the borders between the components
 * to change the relative sizes of the cells, and thus the sizes of the
 * components.
 */
public class DragGridPanel extends JPanel implements MouseListener,
		MouseMotionListener, ComponentListener, FocusListener,
		ContainerListener {

	private static final long serialVersionUID = 1L;

	static RCSVersion version = new RCSVersion(
			"$Id: DragGridPanel.java,v 1.12 2008-07-06 00:25:17 alokito Exp $");

	// ****************************
	// Constructors
	// ****************************
	/**
	 * Constructor.
	 * 
	 * @param x
	 *            the number of columns.
	 * @param y
	 *            the number of rows.
	 */
	public DragGridPanel(final int x, final int y) {

		super((LayoutManager) null);
		setBackground(Color.white);

		components = new Component[x][y];
		xpos = new int[x + 1];
		ypos = new int[y + 1];
		xsizes = new float[x];
		ysizes = new float[y];
		xintsizes = new int[x - 1];
		yintsizes = new int[y - 1];
		staticxsizes = false;
		staticysizes = false;
		// Space everything evenly
		for (int i = 0; i < x; i++)
			xsizes[i] = 1.0f / x;
		for (int i = 0; i < y; i++)
			ysizes[i] = 1.0f / y;
		addMouseListener(this);
		addMouseMotionListener(this);
		addComponentListener(this);
	}

	// ****************************
	// JavaBean Property getters/setters
	// ****************************

	/**
	 * Adjustable - true if use is allowed to drag borders to change cell sizes.
	 */
	public boolean isAdjustable() {

		return adjustable;
	}

	/**
	 * Adjustable - true if use is allowed to drag borders to change cell sizes.
	 */
	public void setAdjustable(final boolean on) {

		adjustable = on;
	}

	/** BorderWidth - width of the draggable borders between Components. */
	public int getBorderWidth() {

		return bwidth;
	}

	/** BorderWidth - width of the draggable borders between Components. */
	public void setBorderWidth(final int width) {

		bwidth = width;
	}

	/** BorderHeight - height of the draggable borders between Components. */
	public int getBorderHeight() {

		return bheight;
	}

	/** BorderHeight - height of the draggable borders between Components. */
	public void setBorderHeight(final int height) {

		bheight = height;
	}

	/** MinimumWidth - minimum width of a component. */
	public int getMinimumWidth() {

		return minwidth;
	}

	/** MinimumWidth - minimum width of a component. */
	public void setMinimumWidth(final int width) {

		minwidth = width;
	}

	/** MinimumHeight - minimum height of a component. */
	public int getMinimumHeight() {

		return minheight;
	}

	/** MinimumHeight - minimum height of a component. */
	public void setMinimumHeight(final int height) {

		minheight = height;
	}

	/** FocusWidth - width of the focus rectangle round each control. */
	public int getFocusWidth() {

		return focuswidth;
	}

	/** FocusWidth - width of the focus rectangle round each control. */
	public void setFocusWidth(final int width) {

		focuswidth = width;
	}

	/** FocusHeight - height of the focus rectangle round each control. */
	public int getFocusHeight() {

		return focusheight;
	}

	/** FocusHeight - height of the focus rectangle round each control. */
	public void setFocusHeight(final int height) {

		focusheight = height;
	}

	/**
	 * Set the proportional widths of each column.
	 * 
	 * @param widths
	 *            the widths.
	 * @exception ArrayIndexOutOfBoundsException
	 *                if not enough widths are supplied.
	 */
	public void setWidths(final float widths[]) {

		float max = 0;
		int i;

		// Copy provided array into class variable
		// This will give the documented exception if the parameter is too small
		System.arraycopy(widths, 0, xsizes, 0, xsizes.length);

		// Calculate total of all passed sizes
		for (i = 0; i < xsizes.length; i++) {

			max += xsizes[i];
		}

		if (max == 0) {

			for (i = 0; i < xsizes.length; i++) {

				xsizes[i] = 1.0f / xsizes.length; // Silly value - space
													// evenly instead
			}
		} else if (max != 1) {

			for (i = 0; i < xsizes.length; i++) {

				xsizes[i] /= max;// Normalize so they add up to 1
			}
		}

		// Resize all the cells accordingly
		doLayout();
		repaint();
	}

	/**
	 * Set absolute/static pixel widths of each column.  Every manipulation of
	 * the component sizes thereafter will always be treated as static, meaning
	 * if the app window is resized, all but the last component will stay set at
	 * the statically determined width. 
	 * 
	 * @param widths
	 *            the widths.
	 */
	public void setWidths(final int widths[]) {

		// Copy provided array into class variable
		// This will give the documented exception if the parameter is too small
		System.arraycopy(widths, 0, xintsizes, 0, xintsizes.length);

		staticxsizes = true;

		// Resize all the cells accordingly
		doLayout();
		repaint();
	}

	/**
	 * Get the proportional widths of each column.
	 * 
	 * @return a float array containing the proportions (adds up to 1).
	 */
	public float[] getWidths() {

		final float[] rwidths = new float[xsizes.length];

		System.arraycopy(xsizes, 0, rwidths, 0, xsizes.length);
		return rwidths;
	}

	/**
	 * Set the proportional heights of each row.
	 * 
	 * @param heights
	 *            array indicating proportional heights.
	 * @exception ArrayIndexOutOfBoundsException
	 *                if not enough heights are supplied.
	 */
	public void setHeights(final float heights[]) {

		float max = 0;
		int i;

		// Copy provided array into class variable
		// This will give the documented exception if the parameter is too small
		System.arraycopy(heights, 0, ysizes, 0, ysizes.length);

		// Calculate total of all passed sizes
		for (i = 0; i < ysizes.length; i++) {

			max += ysizes[i];
		}

		if (max == 0) {

			for (i = 0; i < ysizes.length; i++) {

				ysizes[i] = 1.0f / ysizes.length;// Silly value - space
													// evenly instead
			}
		} else if (max != 1) {

			for (i = 0; i < ysizes.length; i++) {

				ysizes[i] /= max;// Normalise so they add up to 1
			}
		}

		// Resize all the cells accordingly
		doLayout();
		repaint();
	}

	/**
	 * Set the pixel heights of each column.
	 * 
	 * @param widths
	 *            the widths.
	 */
	public void setHeights(final int heights[]) {

		// Copy provided array into class variable
		// This will give the documented exception if the parameter is too small
		System.arraycopy(heights, 0, yintsizes, 0, yintsizes.length);

		staticysizes = true;

		// Resize all the cells accordingly
		doLayout();
		repaint();
	}

	/**
	 * Get the proportional heights of each row.
	 * 
	 * @return a float array containing the proportions (adds up to 1).
	 */
	public float[] getHeights() {

		final float[] rheights = new float[ysizes.length];

		System.arraycopy(ysizes, 0, rheights, 0, ysizes.length);
		return rheights;
	}

	// ****************************
	// Other public methods
	// ****************************

	/**
	 * Adds a component to the DragGridPanel
	 * 
	 * @param component
	 *            The component to be added
	 * @param rectangle
	 *            The location to add it in
	 */
	public void addComponent(final Component component,
			final Rectangle rectangle) {

		if (component != null) {

			addComponent(component, rectangle.x, rectangle.y, rectangle.width,
					rectangle.height);
		}
	}

	/**
	 * Add a component to occupy a single cell.
	 * 
	 * @param x
	 *            Top left cell.
	 * @param y
	 *            Bottom right cell.
	 * @exception ArrayIndexOutofBoundsException
	 *                if any of the parameters extend outside the array.
	 * @exception IllegalArgumentException
	 *                if any of the space is already occupied.
	 */
	public void addComponent(final Component comp, final int x, final int y) {
		if (comp != null) {
			addComponent(comp,x,y,1,1);
		}
	}

	/**
	 * Add a component to occupy a rectangle of cells.
	 * 
	 * @param x
	 *            Top left cell.
	 * @param y
	 *            Bottom right cell.
	 * @param width
	 *            Number of cells across.
	 * @param height
	 *            Number of cells down.
	 * @exception ArrayIndexOutofBoundsException
	 *                if any of the parameters extend outside the array.
	 * @exception IllegalArgumentException
	 *                if any of the space is already occupied.
	 */
	public void addComponent(final Component comp, final int x, final int y,
			final int width, final int height) {

		int i, j;

		// See if cells required are all free
		for (i = x; i < x + width; i++) {

			for (j = y; j < y + height; j++) {

				if (components[i][j] != null)
					throw new IllegalArgumentException("Cells already occupied");
			}
		}

		// Add component to all cells
		for (i = x; i < x + width; i++) {

			for (j = y; j < y + height; j++) {

				components[i][j] = comp;
			}
		}

		// Add component to parent Container
		add(comp);

		// add self as focus listener, for focus display...
		addFocusListenerRecursively(comp);
		repaint();
	}

	/**
	 * Remove a component. Other components will use up the vacated cell(s) if
	 * possible.
	 */
	public void removeComponent(final Component comp) {

		int x, y;

		for (y = 0; y < ysizes.length; y++) {

			for (x = 0; x < ysizes.length; x++) {

				if (components[x][y] == comp) {
					components[x][y] = null;
				}
			}
		}
		removeFocusListenerRecursively(comp);
		remove(comp);
		repaint();
	}

	/**
	 * Remove all components.
	 */
	@Override
	public void removeAll() {

		int x, y;
		List<Component> allcomps = new ArrayList<Component>();

		for (y = 0; y < ysizes.length; y++) {

			for (x = 0; x < ysizes.length; x++) {

				//The same component can occupy multiple cells in the grid
				//We can only remove a component once, so we must find the
				//unique list of components
				boolean already_present = false;
				for(int i = 0;i < allcomps.size();i++) {
					if(components[x][y] == allcomps.get(i)) {
						already_present = true;
					}
				}
				if(!already_present) {
					allcomps.add(components[x][y]);
				}
				components[x][y] = null;
			}
		}
		for(int i = 0;i < allcomps.size();i++) {
			Component comp = allcomps.get(i);
			if(comp != null) {
				removeFocusListenerRecursively(comp);
				remove(comp);
			}
		}
		repaint();
	}

	/** added by alok 10/9/2001 */
	@Override
	public Dimension getPreferredSize() {

		int maxw = 0;

		for (int y = 0; y < ysizes.length; y++) {

			int roww = components[0][y].getPreferredSize().width;

			for (int x = 1; x < xsizes.length; x++) {

				// skip if same as one on left
				if (components[x][y] == components[x - 1][y]) {

					continue;
				}

				if (components[x][y] != null) {

					roww += components[x][y].getPreferredSize().width;
				}
			}

			if (roww > maxw) {

				maxw = roww;
			}
		}

		int maxh = 0;

		for (int x = 0; x < xsizes.length; x++) {

			int colh = components[x][0].getPreferredSize().height;

			for (int y = 1; y < ysizes.length; y++) {

				// skip if same as one above
				if (components[x][y] == components[x][y - 1]) {

					continue;
				}

				if (components[x][y] != null) {

					colh += components[x][y].getPreferredSize().height;
				}
			}

			if (colh > maxh) {

				maxh = colh;
			}
		}

		return new Dimension(maxw, maxh);
	}

	/** added by alok 9/12/2001 */
	private void addFocusListenerRecursively(final Component c) {

		// Add Focus Listener to the Component passed as an argument
		c.addFocusListener(this);

		// Check if the Component is a Container
		if (c instanceof Container) {

			// Component c is a Container. The following cast is safe.
			final Container cont = (Container) c;

			// Add ContainerListener to the Container.
			cont.addContainerListener(this);

			// Get the Container's array of children Components.
			final Component[] children = cont.getComponents();

			// For every child repeat the above operation.
			for (int i = 0; i < children.length; i++) {

				addFocusListenerRecursively(children[i]);
			}
		}
	}

	/** added by alok 9/12/2001 */
	private void removeFocusListenerRecursively(final Component c) {

		// Add Focus Listener to the Component passed as an argument
		c.removeFocusListener(this);

		// Check if the Component is a Container
		if (c instanceof Container) {

			// Component c is a Container. The following cast is safe.
			final Container cont = (Container) c;

			// Add ContainerListener to the Container.
			cont.removeContainerListener(this);

			// Get the Container's array of children Components.
			final Component[] children = cont.getComponents();

			// For every child repeat the above operation.
			for (int i = 0; i < children.length; i++) {

				removeFocusListenerRecursively(children[i]);
			}
		}
	}

	/** added by alok 9/12/2001 */
	@Override
	public void componentAdded(final ContainerEvent e) {

		addFocusListenerRecursively(e.getChild());
	}

	/** added by alok 9/12/2001 */
	@Override
	public void componentRemoved(final ContainerEvent e) {

		removeFocusListenerRecursively(e.getChild());
	}

	/** Recompute cell sizes from proportional sizes, and resize components. */
	@Override
	public void doLayout() {

		int x, y;
		final Dimension s = getSize();
		//LogBuffer.println("Current layout width/height: [" + s.width + "/" + s.height + "].");

		checkPopulateSizes();

		// Do columns - start at left edge
		xpos[0] = 0;
		for (x = 0; x < xsizes.length; x++) {

			xpos[x + 1] = xpos[x] + (int) (s.width * xsizes[x]);
			//LogBuffer.println("Width of column [" + (x + 1) + "] = [" + xpos[x] + " + (int) (" + s.width + " * " + xsizes[x] + ")] = [" + xpos[x + 1] + "].");
		}

		// Fudge right edge in case of rounding errors
		xpos[xsizes.length] = s.width;

		// Do rows - start at top
		ypos[0] = 0;
		for (y = 0; y < ysizes.length; y++) {

			ypos[y + 1] = ypos[y] + (int) (s.height * ysizes[y]);
		}

		// Fudge bottom edge in case of rounding errors
		ypos[ysizes.length] = s.height;
		resizeComponents();
	}

	/**
	 * This updates the xpos and ypos arrays based on the (presumably) changed
	 * contents of xsizes and ysizes. Only has an effect if the sizes were
	 * initially set as static
	 */
	private void updatePositions() {
		int x, y;
		final Dimension s = getSize();

		if(staticxsizes) {
			// Do columns - start at left edge
			int xsum = 0;
			for (x = 0; x < xintsizes.length; x++) {
				//Adjust the xsizes if sizes were set statically
				xsizes[x] = (float) xintsizes[x] / (float) s.width;
				xsum += xintsizes[x];
			}
			xsizes[x] = ((float) s.width - (float) xsum) / (float) s.width;
			xpos[0] = 0;
			for (x = 0; x < xsizes.length; x++) {
				xpos[x + 1] = xpos[x] + (int) (s.width * xsizes[x]);
			}
			// Fudge right edge in case of rounding errors
			xpos[xsizes.length] = s.width;
		}
		if(staticysizes) {
			// Do rows - start at top
			int ysum = 0;
			for (y = 0; y < yintsizes.length; y++) {
				//Adjust the xsizes if sizes were set statically
				ysizes[y] = (float) yintsizes[y] / (float) s.height;
				ysum += yintsizes[y];
			}
			ysizes[y] = ((float) s.height - (float) ysum) / (float) s.height;
			ypos[0] = 0;
			for (y = 0; y < ysizes.length; y++) {
				ypos[y + 1] = ypos[y] + (int) (s.height * ysizes[y]);
			}
			// Fudge bottom edge in case of rounding errors
			ypos[ysizes.length] = s.height;
		}
	}

	private void checkPopulateSizes() {

		final Dimension s = getSize();
		int pxsum = 0;

		if(staticxsizes && xsizes.length > 0 &&
			xintsizes.length > 0 && xintsizes[0] != 0 &&
			s.width > 0) {

			int i = 0;
			for(i = 0;i < xintsizes.length;i++) {

				//If the current size sum plus the current size is too big
				if(//The current accumulated size
					pxsum + xintsizes[i] +
					//The rest of the columns times the min width
					(xintsizes.length - (i + 1) + 1) * minwidth > s.width) {

					xintsizes[i] = s.width -
						//pixel sum + remaining minimum xintsizes + minimum size
						//of last variable cell
						pxsum - (xintsizes.length - (i + 1) + 1) * minwidth;
				}

				pxsum += xintsizes[i];
				xsizes[i] = (float) xintsizes[i] / (float) s.width;
			}
			//See if the xintsizes are too big
			if(pxsum > s.width) {
				xsizes[i] = 1.0f;
				//This will normalize the sizes
				setWidths(xsizes);
			} else {
				xsizes[i] = 1.0f - (float) pxsum / (float) s.width;
			}
		}

		pxsum = 0;

		if(staticysizes && ysizes.length > 0 &&
			yintsizes.length > 0 && yintsizes[0] != 0 &&
			s.height > 0) {

			int i = 0;
			for(i = 0;i < yintsizes.length;i++) {

				//If the current size sum plus the current size is too big
				if(//The current accumulated size
					pxsum + yintsizes[i] +
					//The rest of the columns times the min width
					(yintsizes.length - (i + 1) + 1) * minheight > s.height) {

					yintsizes[i] = s.height -
						//pixel sum + remaining minimum yintsizes + minimum size
						//of last variable cell
						pxsum - (yintsizes.length - (i + 1) + 1) * minheight;
				}

				pxsum += yintsizes[i];
				ysizes[i] = (float) yintsizes[i] / (float) s.height;
			}
			//See if the xintsizes are too big
			if(pxsum > s.height) {
				ysizes[i] = 1.0f;
				//This will normalize the sizes
				setHeights(ysizes);
			} else {
				xsizes[i] = 1.0f - (float) pxsum / (float) s.height;
			}
		}
	}

	// ****************************
	// Overridden public methods
	// ****************************

	/** Standard paint routine. */
	@Override
	public void paintComponent(final Graphics g) {

		// System.out.println("entering DragGridPanel paint " +getName() +
		// " clip " + clip);
		final Dimension newsize = getSize();

		//g.setColor(Color.white);
		g.setColor(GUIFactory.DEFAULT_BG);
		g.fillRect(0, 0, newsize.width, newsize.height);
		if(dragging() || overHorizDragBar || overVertDragBar) {
			g.setColor(Color.LIGHT_GRAY);
			//LogBuffer.println("Painting dragbar a new color.");
		}
		//LogBuffer.println("Hovering horizontally?: [" + overHorizDragBar + "]");
		//LogBuffer.println("Hovering vertically?: [" + overVertDragBar + "]");
		//g.setColor(Color.black);

		int x, y;

		// Do horizontal lines above each component not in top row
		for (y = 1; y < ysizes.length; y++) {
			//LogBuffer.println("y: " + y);

			for (x = 0; x < xsizes.length; x++) {
				//LogBuffer.println("x: " + x);

				final Component c = components[x][y];

				if (components[x][y - 1] == c) {
					LogBuffer.println("skipping");

					continue; // Same component above
				}
				//LogBuffer.println("drawing xpos: " + xpos[x] + " next-xpos: " + xpos[x+1] + " ypos: " + ypos[y] + " height: " + bheight);

				// Draw bar above
				g.fillRect(xpos[x], ypos[y] - bheight, xpos[x + 1] - xpos[x],
						bheight);
			}
		}

		// Do vertical lines to left of each component not in left column
		for (x = 1; x < xsizes.length; x++) {

			for (y = 0; y < ysizes.length; y++) {

				final Component c = components[x][y];

				if (components[x - 1][y] == c) {

					continue; // Same component to left
				}

				// Draw bar to left
				g.fillRect(xpos[x] - bwidth, ypos[y], bwidth, ypos[y + 1]
						- ypos[y]);
			}
		}
		// System.out.println("exiting DragGridPanel paint");
	}

	/** draws focus rectangle, added by alok 9/12/2001 */
	@Override
	public void focusGained(final FocusEvent e) {

		// draw the focus rectangle
		for (int x = 0; x < xsizes.length; x++) {

			for (int y = 0; y < ysizes.length; y++) {

				final Component c = components[x][y];

				if (c == e.getComponent()) {

					drawFocus(getGraphics(), x, y);
					return;
				}

				if (c instanceof Container) {

					// Component c is a Container. The following cast is safe.
					final Container cont = (Container) c;
					if (cont.isAncestorOf(e.getComponent())) {

						drawFocus(getGraphics(), x, y);
						return;
					}
				}
			}
		}
	}

	@Override
	public void focusLost(final FocusEvent e) {

		// undraw the focus rectangle
		for (int x = 0; x < xsizes.length; x++) {

			for (int y = 0; y < ysizes.length; y++) {

				final Component c = components[x][y];

				if (c == e.getComponent()) {

					drawFocus(getGraphics(), x, y);
					return;
				}

				if (c instanceof Container) {

					// Component c is a Container. The following cast is safe.
					final Container cont = (Container) c;

					if (cont.isAncestorOf(e.getComponent())) {

						final Graphics g = getGraphics();

						if (g != null) {

							// sometimes this is called after the
							// graphics context is gone.
							final Color o = g.getColor();
							g.setColor(cont.getBackground());
							drawFocus(g, x, y);
							g.setColor(o);
						}

						return;
					}
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private void drawFocus(final Graphics g, final int x, final int y) {

		final Component c = components[x][y];
		final Dimension size = c.getSize();
		final int w = size.width;
		final int h = size.height;

		if (g == null) {

			return;
		}

		if (c == null) {

			return; // No component here!
		}

		if (x > 0 && components[x - 1][y] == c) {

			return; // Already processed in last column
		}

		if (y > 0 && components[x][y - 1] == c) {

			return; // Already processed in last row
		}

		// need to draw four rectangles
		g.fillRect // left side
		(xpos[x], ypos[y], focuswidth, h + 2 * focusheight);

		g.fillRect // right side
		(xpos[x] + w + focuswidth, ypos[y], focuswidth, h + 2 * focusheight);
		g.fillRect // top
		(xpos[x] + focuswidth, ypos[y], w, focusheight);
		g.fillRect // bottom
		(xpos[x] + focuswidth, ypos[y] + h + focuswidth, w, focusheight);
	}

	/** Standard toString returns "DragGridPanel[x][y]" and Panel info. */
	@Override
	public String toString() {

		final Point p = getLocation();
		return "DragGridPanel(" + p.x + "," + p.y + ")[" + xsizes.length + "]["
				+ ysizes.length + "] " + super.toString();
	}

	// ****************************
	// Event handling
	// ****************************

	/** Change the mouse cursor if over a draggable border. */
	@Override
	public void mouseMoved(final MouseEvent e) {

		debug("mouseMoved", e);
		//LogBuffer.println("mouseMoved");

		if (!dragging()) {

			boolean lastOverBorder = overVertDragBar || overHorizDragBar;
			boolean nowOverBorder = onDragBorder(e.getX(), e.getY());
			//LogBuffer.println("Was over border: " + lastOverBorder + " Now over border: " + nowOverBorder);
			if(lastOverBorder != nowOverBorder) {
				repaint();
			}
		}
		//LogBuffer.println("Dragging? " + dragging());
	}

	/** Select a border to drag. */
	@Override
	public void mousePressed(final MouseEvent e) {

		debug("mousePressed", e);

		if (adjustable && onDragBorder(e.getX(), e.getY())) {
			// Save dragging borders
			coldrag = curscol;
			rowdrag = cursrow;
			//setCursor(handCursor);
			dragBar = new DragBar();
			dragBar.setBounds(0, 0, getWidth(), getHeight());
			dragBar.setMouse(e.getX(), e.getY());
			// add(dragBar, 0);
		}
	}

	/** Stop dragging a border. */
	@Override
	public void mouseReleased(final MouseEvent e) {

		debug("mouseReleased", e);

		if (dragging()) {

			// Reset dragging borders
			coldrag = rowdrag = 0;
			// Set mouse cursor to correct shape
			setCursor(originalCursor);
			remove(dragBar);
		}
		if (resize) {

			resizeComponents();
			repaint();
		}
	}

	@Override
	public void mouseClicked(final MouseEvent e) {

		debug("mouseClicked", e);
	}

	@Override
	public void mouseEntered(final MouseEvent e) {

		debug("mouseEntered", e);
		onDragBorder(e.getX(), e.getY());
		repaint();
	}

	@Override
	public void mouseExited(final MouseEvent e) {

		debug("mouseExited", e);

		if (currentCursor != 0 && !dragging()) {

			setCursor(0);
		}

		overVertDragBar = overHorizDragBar = false;

		repaint();
	}

	boolean resize = false;

	/** If a border is selected, change it's position. */
	@Override
	public void mouseDragged(final MouseEvent e) {

		debug("mouseDragged", e);

		if (dragging()) {

			int x = e.getX(), y = e.getY();
			resize = false;

			if (coldrag > 0) { // Dragging a column

				resize = true;
				// Ensure column is at least minwidth wide
				if (x < xpos[coldrag - 1] + minwidth) {

					x = xpos[coldrag - 1] + minwidth;
				} else if (x > xpos[coldrag + 1] - minwidth) {

					x = xpos[coldrag + 1] - minwidth;
				}
				if (xpos[coldrag] != x) {

					// Move column border
					xpos[coldrag] = x;
					// Recalculate relative sizes
					xsizes[coldrag - 1] = (xpos[coldrag] - xpos[coldrag - 1])
							/ (float) getSize().width;
					xsizes[coldrag] = (xpos[coldrag + 1] - xpos[coldrag])
							/ (float) getSize().width;

					//Adjust the static sizes if sizes were set statically
					if(staticxsizes) {
						xintsizes[coldrag - 1] =
							Math.round((float) getSize().width *
								xsizes[coldrag - 1]);

						if(coldrag < xintsizes.length) {
							xintsizes[coldrag] =
								Math.round((float) getSize().width *
									xsizes[coldrag]);
						}
					}
					// We must redraw
				}
			}
			if (rowdrag > 0) { // Dragging a row

				resize = true;
				// Ensure row is at least minheight high
				if (y < ypos[rowdrag - 1] + minheight) {

					y = ypos[rowdrag - 1] + minheight;
				} else if (y > ypos[rowdrag + 1] - minheight) {

					y = ypos[rowdrag + 1] - minheight;
				}
				if (ypos[rowdrag] != y) {
					// Move row border
					ypos[rowdrag] = y;
					// Recalculate relative sizes
					ysizes[rowdrag - 1] = (ypos[rowdrag] - ypos[rowdrag - 1])
							/ (float) getSize().height;
					ysizes[rowdrag] = (ypos[rowdrag + 1] - ypos[rowdrag])
							/ (float) getSize().height;

					//Adjust the static sizes if sizes were set statically
					if(staticysizes) {
						yintsizes[rowdrag - 1] =
							Math.round((float) getSize().height *
								ysizes[rowdrag - 1]);

						if(rowdrag < yintsizes.length) {
							yintsizes[rowdrag] =
								Math.round((float) getSize().height *
									ysizes[rowdrag]);
						}
					}
					// We must redraw
				}
			}
		}
		dragBar.setMouse(e.getX(), e.getY());
		if(resize) {
			//Live resizing
			resizeComponents();
		}
		repaint();
	}

	public Dimension getGridSize() {
		return(getSize());
	}

	/** Called when this container gets resized. */
	@Override
	public void componentResized(final ComponentEvent e) {

		debug("componentResized", e);
		doLayout();
	}

	@Override
	public void componentMoved(final ComponentEvent e) {

		debug("componentMoved", e);
	}

	@Override
	public void componentShown(final ComponentEvent e) {

		debug("componentShown", e);
	}

	@Override
	public void componentHidden(final ComponentEvent e) {

		debug("componentHidden", e);
	}

	/*
	 * // Debugging code to try to find out what is happening to the focus. //
	 * As there doesn't seem to be a way of keeping track of the focus, // I'm a
	 * bit mystified at present. I'd like to make it possible to // tab and
	 * back-tab between the controls.
	 * 
	 * public boolean gotFocus(Event evt, Object what) { System.out.println(this
	 * + "got focus:" + evt + " " + what); return true; //return
	 * super.gotFocus(evt, what); }
	 * 
	 * public boolean lostFocus(Event evt, Object what) {
	 * System.out.println(this + "lost focus:" + evt + " " + what); return true;
	 * //return super.lostFocus(evt, what); }
	 * 
	 * public boolean keyDown(Event e, int key) { System.out.println(this +
	 * "Key:" + key); if(key == '\t') { if(e.modifiers == 0) { nextFocus(); }
	 * else if(e.modifiers == Event.SHIFT_MASK) { nextFocus(); } } return
	 * super.keyDown(e, key); }
	 */

	// ****************************
	// Package and private methods
	// ****************************

	/** Resize components into new grid. */
	void resizeComponents() {

		int x, y;

		//In case the sizes were initially set statically, update the positions
		//based on the static values
		updatePositions();

		// Reshape the components to fit
		// For each row
		for (y = 0; y < ysizes.length; y++) {

			// For each column
			for (x = 0; x < xsizes.length; x++) {

				final Component c = components[x][y];

				if (c == null) {

					continue; // No component here!
				}

				if (x > 0 && components[x - 1][y] == c) {

					continue; // Already processed in last column
				}

				if (y > 0 && components[x][y - 1] == c) {

					continue; // Already processed in last row
				}

				int mx, my;

				// Find all cells for this component to the right
				for (mx = x + 1; mx < xsizes.length && components[mx][y] == c; mx++)
					;
				// Component occupies cells x through mx inclusive
				// Find all the cells for this component below
				for (my = y + 1; my < ysizes.length && components[x][my] == c; my++)
					;
				// Component occupies cells y through my inclusive
				// Compute the width and height of the component
				int w = xpos[mx] - xpos[x] - 2 * focuswidth;
				int h = ypos[my] - ypos[y] - 2 * focusheight;
				// Allow for the draggable bar if needed
				if (mx < xsizes.length)
					w -= bwidth;
				if (my < ysizes.length)
					h -= bheight;
				// Finally, resize the component
				c.setBounds(xpos[x] + focuswidth, ypos[y] + focusheight, w, h);
				c.validate(); // added by alok, 7/18/2001
			}
		}
	}

	/**
	 * Work out if the mouse cursor is over a draggable border. Only called if
	 * DragGridPanel is adjustable. Sets cursor shape, curscol and cursrow
	 * appropriately.
	 * 
	 * @param x
	 *            mouse position.
	 * @param y
	 *            mouse position.
	 * @return true if cursor on a draggable border.
	 */
	private boolean onDragBorder(final int x, final int y) {

		int col, row;

		curscol = cursrow = 0; // Which border we are in (i.e. none)

		// Find out which column we are in
		for (col = 1; col < xsizes.length; col++) {
			//LogBuffer.println("Evaluating whether we're in column [" + col + "].");
			if (x <= xpos[col]) { // In column col - 1, or its border

				//LogBuffer.println("We're in column [" + col + " - 1] or its border.");
				if (x >= xpos[col] - bwidth) {
					//LogBuffer.println("We're in the border of column [" + col + " - 1].");
					curscol = col; // In border, so set curscol
				}
				break;
			}
		}

		// Find out which row we are in
		for (row = 1; row < ysizes.length; row++) {

			if (y <= ypos[row]) { // In row row - 1, or its border

				if (y >= ypos[row] - bheight)
					cursrow = row; // In border, so set cursrow
				break;
			}
		}
		// Col is set to the column number to the right of the cursor
		// Row is set to the row number immediately below the cursor
		// i.e. if cursor in top left cell, or its border, row and col will both
		// be 1

		int newcursor = 0; // New mouse cursor shape

		if (curscol > 0 || cursrow > 0) { // We may be in a border

			int both = 0;

			if (curscol > 0) { // May be in a column border

				// Check if components above left and right are different
				if (components[curscol][row - 1] != components[col - 1][row - 1]
						// Also check components below left and right
						|| (row < ysizes.length && components[curscol][row] != components[col - 1][row])) {

					newcursor = leftRightCursor;
					overVertDragBar = true;
					both++;
				} else {
					overVertDragBar = false;
					curscol = 0; // In the middle of a component that takes more
								 // than 1 cell
				}
			} else {
				overVertDragBar = false;
			}

			if (cursrow > 0) { // May be in a row border

				// Check if components left above and below are different
				if (components[col - 1][cursrow] != components[col - 1][row - 1]
						// Also check components right above and below
						|| (col < xsizes.length && components[col][cursrow] != components[col][row - 1])) {

					newcursor = upDownCursor;
					overHorizDragBar = true;
					both++;
				} else {

					overHorizDragBar = false;
					cursrow = 0; // In the middle of a component that takes more
								 // than 1 cell
				}
			} else {
				overHorizDragBar = false;
			}

			if(both == 2) {
				newcursor = crossHairCursor;
			}
		} else {
			overVertDragBar = false;
			overHorizDragBar = false;
		}

		if (newcursor != currentCursor) {

			setCursor(newcursor);
		}

		debug("onDragBorder(" + curscol + "," + cursrow + ")");
		//LogBuffer.println("onDragBorder newcursor = [" + newcursor + "]");
		return (newcursor != 0);
	}

	/**
	 * Switch cursor to indicate user can drag a border. Only called if new
	 * cursor is different to current one.
	 * 
	 * @param newcursor
	 *            newcursor to use, -1 to change it back.
	 */
	private void setCursor(final int newcursor) {

		debug("setCursor(" + newcursor + ")");

		if (newcursor > 0) {

			try {

				// Want to set cursor to new shape
				if (originalCursor == null || currentCursor != newcursor) { // Save original shape

					if(originalCursor == null) {
						originalCursor = getCursor();
					}

					switch (newcursor) {

					case upDownCursor:
						setCursor(Cursor
								.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
						break;
					case leftRightCursor:
						setCursor(Cursor
								.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
						break;
					case crossHairCursor:
						//Apple doesn't have a move cursor
						if (System.getProperty("os.name").startsWith("Mac OS X")) {
							setCursor(Cursor
								.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
						} else {
							setCursor(Cursor
								.getPredefinedCursor(Cursor.MOVE_CURSOR));
						}
						break;
					case handCursor:
						setCursor(Cursor
								.getPredefinedCursor(Cursor.HAND_CURSOR));
						break;
					}
				}
			} catch (final java.lang.NullPointerException e) {
				// we failed to set cursor, so we still have original, nothing
				// to do.
				debug("Failed to set cursor");
			}
		} else {

			// Want to restore original cursor
			try {

				setCursor(originalCursor);
				originalCursor = null;
			} catch (final java.lang.NullPointerException e) {

				// ignore, we're still with the "newCursor"
			}
		}
		currentCursor = newcursor;
	}

	private boolean dragging() {

		return (adjustable && (coldrag > 0 || rowdrag > 0));
	}

	private void debug(final String s, final Object o) {

		if (trace) {

			Debug.print(this, s, o);
		}
	}

	private void debug(final String s) {
		if (trace) {

			Debug.print(this, s, null);
		}
	}

	// ****************************
	// Variables
	// ****************************

	/** Set to true if user is allowed to adjust sizes. */
	boolean adjustable = true;
	/** Width of the draggable borders between Components. */
	int bwidth = 6;
	/** Height of the draggable borders between Components. */
	int bheight = 6;
	/** Minimum width of a component. */
	int minwidth = 20;
	/** Minimum height of a component. */
	int minheight = 20;
	/** Width of the focus rectangle round each control. */
	int focuswidth = 0;
	/** Height of the focus rectangle round each control. */
	int focusheight = 0;

	/**
	 * The components, organised in a grid. If a component takes up more than
	 * one cell, it is placed in all the array positions it occupies.
	 */
	Component components[][];
	/**
	 * The x positions of the dividing lines between cells. xpos[0] is always 0,
	 * and xpos[no of columns] is the width of the Panel.
	 */
	int xpos[];
	/**
	 * The y positions of the dividing lines between cells. ypos[0] is always 0,
	 * and ypos[no of rows] is the height of the Panel.
	 */
	int ypos[];
	/** The relative sizes of the cell columns. Add up to 1. */
	float xsizes[];
	/** The relative sizes of the cell rows. Add up to 1. */
	float ysizes[];
	/** The absolute sizes of the all but the last cell columns. */
	int xintsizes[];
	boolean staticxsizes = false;
	/** The absolute sizes of the all but the last cell rows. */
	int yintsizes[];
	boolean staticysizes = false;
	/** column divider being dragged by mouse, or 0. */
	int coldrag;
	/** row divider being dragged by mouse, or 0. */
	int rowdrag;
	/** column divider which cursor is over. */
	int curscol;
	/** row divider which cursor is over. */
	int cursrow;
	/** original cursor type. */
	Cursor originalCursor;
	/** current cursor type. */
	int currentCursor = -1;
	boolean overHorizDragBar = false;
	boolean overVertDragBar = false;
	/*
	 * ContainerFocusTracker to pass focus on to appropriate sub-component.
	 * removed by alok 9/12/2001, as doesn't seem necessary.
	 * 
	 * ContainerFocusTracker tracker = new ContainerFocusTracker(this);
	 */
	/** Left-right cursor */
	static final int leftRightCursor = 1;
	/** Up-down cursor */
	static final int upDownCursor = 2;
	/** Cross-hair cursor */
	static final int crossHairCursor = 3;
	/** Hand cursor */
	static final int handCursor = 4;

	static final boolean trace = false;
	DragBar dragBar = new DragBar();

}

class DragBar extends JComponent {

	private static final long serialVersionUID = 1L;

	public DragBar() {

		setOpaque(false);
	}

	private final Point mouse = new Point();

	public void setMouse(final int x, final int y) {

		mouse.x = x;
		mouse.y = y;
	}

//	@Override
//	public void paintComponent(final Graphics g) {
//
//		g.setColor(Color.green);
//		g.fillRect(mouse.x, 0, 2, getHeight());
//		g.fillRect(0, mouse.y, getWidth(), 2);
//	}
}
