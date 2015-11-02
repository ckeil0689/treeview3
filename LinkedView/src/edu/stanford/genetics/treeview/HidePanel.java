/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.awt.Button;
import java.awt.Panel;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class simply displays a close button centered in a panel. Clicking the
 * close button hides the window.
 */
class HidePanel extends Panel {

	private static final long serialVersionUID = 1L;

	private final Window m_window;

	public HidePanel(final Window window) {

		m_window = window;
		final Button hide_button = new Button("Close");

		hide_button.addActionListener(new ActionListener() {

			// called when close button hit
			@Override
			public void actionPerformed(final ActionEvent evt) {

				if (evt.getSource() == hide_button) {

					m_window.setVisible(false);
				}
			}
		});
		add(hide_button);
	}
}
