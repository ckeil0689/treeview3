/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import utilities.GUIFactory;

/**
 * this is a dialog which displays a modeless settings dialog. it includes a
 * close button, which will dispose of the dialog when it is pressed. It could
 * be extended to include a hide button, which would not dispose but just hide.
 */
public class ModelessSettingsDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	SettingsPanel settingsPanel;
	JDialog settingsFrame;

	public ModelessSettingsDialog(final JFrame frame, final String title,
			final SettingsPanel panel) {

		super(frame, title, false);
		settingsPanel = panel;
		settingsFrame = this;

		final JPanel inner = GUIFactory.createJPanel(false, GUIFactory.DEFAULT,
				null);

		inner.add((JPanel) panel, "push, grow, wrap");
		inner.add(new ButtonPanel(), "pushx, growx, alignx 50%");

		getContentPane().add(inner);
	}

	@Override
	public void setVisible(final boolean val) {

		super.setVisible(val);
		settingsPanel.synchronizeFrom();
	}

	class ButtonPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		public ButtonPanel() {

			final JButton close_button = GUIFactory.createBtn("Close");
			close_button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					settingsPanel.synchronizeTo();
					settingsFrame.dispose();
				}
			});
			add(close_button);

			final JButton cancel_button = GUIFactory.createBtn("Cancel");
			cancel_button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					settingsPanel.synchronizeFrom();
					settingsFrame.dispose();
				}
			});
			// add(cancel_button);
		}
	}
}
