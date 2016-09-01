/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.core;

import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JTextArea;

import edu.stanford.genetics.treeview.LogBuffer;

public class LogMessagesPanel extends JTextArea implements Observer {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final LogBuffer logBuffer;

	public LogMessagesPanel(final LogBuffer buffer) {
		super(null, 20, 50);
		logBuffer = buffer;
		logBuffer.addObserver(this);
		synchronizeFrom();
	}

	private void synchronizeFrom() {
		final Enumeration elements = logBuffer.getMessages();
		while (elements.hasMoreElements()) {
			append((String) elements.nextElement());
			append("\n");
		}
	}

	@Override
	public void update(final Observable arg0, final Object arg1) {
		if (arg1 != null) {
			this.append((String) arg1);
			this.append("\n");
		}
	}

}
