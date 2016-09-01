/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Note: I, Alok, didn't write this class.
 *
 * Class for Containers which won't take the keyboard focus. Because of what
 * appears to be a bug in Java 1.1, when you move the focus on to a Frame, or
 * other Container, it keeps the focus, rather than handing it on to an
 * appropriate tabstop inside it. This class collects all focus events for the
 * Container and its contents, and will correct the problem. It also monitors
 * the Container, and keeps track if the focussed component is removed from the
 * container. To use, just create a tracker, passing it the Container you want
 * to deal with.
 */
public class ContainerFocusTracker implements FocusListener, ContainerListener {
	static RCSVersion version = new RCSVersion(
			"$Id: ContainerFocusTracker.java,v 1.5 2004-12-21 03:28:13 alokito Exp $");

	// ****************************
	// Constructors
	// ****************************
	public ContainerFocusTracker(final Container c) {
		if (debug) {
			System.out.println("FocusTracker(" + c.getName() + ")");
		}
		container = c;
		addComponent(c);
	}

	// ****************************
	// Event handling
	// ****************************

	@Override
	public void componentAdded(final ContainerEvent e) {
		if (debug) {
			System.out.println(container.getName() + " - Adding...");
		}
		addComponent(e.getChild());
	}

	@Override
	public void componentRemoved(final ContainerEvent e) {
		if (debug) {
			System.out.println(container.getName() + " - Removing...");
		}
		removeComponent(e.getChild());
	}

	@Override
	public void focusGained(final FocusEvent e) {
		final Component c = e.getComponent();

		if (c == container) {
			if (debug) {
				System.out.println("Container " + container.getName()
						+ " got focus");
			}
			if (focus != null) {
				if (debug) {
					System.out.println("Returning focus to " + focus.getName());
				}
				focus.requestFocus();
			} else {
				switchFocus(container);
			}
		} else if (c.isVisible() && c.isEnabled() && c.isFocusable()) {
			if (debug) {
				System.out.println(container.getName()
						+ " - Tracking focus to " + e.getComponent().getName());
			}
			focus = c;
		}
	}

	@Override
	public void focusLost(final FocusEvent e) {
	}

	// ****************************
	// Package and private methods
	// ****************************

	private boolean switchFocus(final Container container) {
		synchronized (container.getTreeLock()) {
			for (int i = 0; i < container.getComponentCount(); i++) {
				final Component c = container.getComponent(i);

				if (c == null) {
					break;
				}
				if (c.isVisible() && c.isEnabled() && c.isFocusable()) {
					if (debug) {
						System.out.println(this.container.getName()
								+ " - Giving focus to " + c.getName());
					}
					c.requestFocus();
					return true;
				} else if (c instanceof Container) {
					if (switchFocus((Container) c))
						return true;
				} else if (debug) {
					System.out.println("Not giving focus to " + c.getName()
							+ " vis:" + c.isVisible() + " ena:" + c.isEnabled()
							+ " tab:" + c.isFocusable());
				}
			}
		}
		return false;
	}

	private void addComponent(final Component c) {
		if (debug) {
			System.out.println(" " + c.getName());
		}
		c.addFocusListener(this);
		if (c instanceof Container) {
			addContainer((Container) c);
		}
	}

	private void addContainer(final Container container) {
		container.addContainerListener(this);
		synchronized (container.getTreeLock()) {
			for (int i = 0; i < container.getComponentCount(); i++) {
				final Component c = container.getComponent(i);
				addComponent(c);
			}
		}
	}

	private void removeComponent(final Component c) {
		if (debug) {
			System.out.println(" " + c.getName());
		}
		if (c == focus) {
			focus = null;
		}
		c.removeFocusListener(this);
		if (c instanceof Container) {
			removeContainer((Container) c);
		}
	}

	private void removeContainer(final Container container) {
		container.removeContainerListener(this);
		synchronized (container.getTreeLock()) {
			for (int i = 0; i < container.getComponentCount(); i++) {
				final Component c = container.getComponent(i);
				removeComponent(c);
			}
		}
	}

	// ****************************
	// Variables
	// ****************************

	Container container;
	Component focus;
	final static boolean debug = false;
}
