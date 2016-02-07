/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import utilities.GUIFactory;

/**
 * this is a dialog which displays a single cancelable settings panel
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.6 $ $Date: 2004-12-21 03:28:13 $
 */
public class CancelableSettingsDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	SettingsPanel settingsPanel;
	JDialog settingsFrame;

	/**
	 *
	 * @param frame
	 *            <code>JFrame</code> to block on
	 * @param title
	 *            a title for the dialog
	 * @param panel
	 *            A <code>SettingsPanel</code> to feature in the dialog.
	 */
	public CancelableSettingsDialog(final JFrame frame, final String title,
			final SettingsPanel panel) {

		super(frame, title, true);
		settingsPanel = panel;
		settingsFrame = this;
		final JPanel inner = GUIFactory.createJPanel(false, GUIFactory.DEFAULT,
				null);

		inner.add((Component) panel, "push, grow, wrap");
		inner.add(new ButtonPanel(), "pushx, growx, alignx 50%");

		getContentPane().add(inner);
		pack();
	}

	class ButtonPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		ButtonPanel() {

			final JButton save_button = GUIFactory.createBtn("Save");
			save_button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					try {
						settingsPanel.synchronizeTo();
						settingsFrame.dispose();

					} catch (final java.lang.OutOfMemoryError ex) {

						final JPanel temp = new JPanel();
						temp.add(new JLabel("Out of memory, try smaller "
								+ "pixel settings or allocate more RAM"));
						temp.add(new JLabel("see Chapter 3 of "
								+ "Help->Documentation... for Out of Memory "
								+ "and Image Export)"));

						JOptionPane.showMessageDialog(
								CancelableSettingsDialog.this, temp);
					}
				}
			});
			add(save_button);

			final JButton cancel_button = GUIFactory.createBtn("Cancel");
			cancel_button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					settingsPanel.synchronizeFrom();
					settingsFrame.dispose();
				}
			});
			add(cancel_button);
		}
	}
}
