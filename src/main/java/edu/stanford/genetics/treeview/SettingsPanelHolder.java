/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * @author aloksaldanha
 *
 *         Makes a JPanel with Save and Cancel buttons. The buttons will hide
 *         the window if the window is not null.
 */
public class SettingsPanelHolder extends JPanel {

	private static final long serialVersionUID = 1L;

	private Window window = null;
	private Preferences configNode = null;

	/**
	 * Please use this constructor.
	 *
	 * @param w
	 *            Window to close on Save or Cancel
	 * @param c
	 *            ConfigNode to store on a save.
	 */
	public SettingsPanelHolder(final Window w, final Preferences c) {

		super();

		window = w;
		configNode = c;
		setLayout(new BorderLayout());
		add(new ButtonPanel(), BorderLayout.SOUTH);
	}

	public void synchronizeTo() {

		final int n = this.getComponentCount();
		for (int i = 0; i < n; i++) {
			synchronizeTo(i);
		}
	}

	public void synchronizeTo(final int i) {

		try {
			((SettingsPanel) getComponent(i)).synchronizeTo();

		} catch (final ClassCastException e) {
			// ignore
		}
	}

	public void synchronizeFrom() {
		final int n = this.getComponentCount();
		for (int i = 0; i < n; i++) {
			synchronizeFrom(i);
		}
	}

	public void synchronizeFrom(final int i) {
		try {
			((SettingsPanel) getComponent(i)).synchronizeFrom();
		} catch (final ClassCastException e) {
			// ignore
		}
	}

	public void addSettingsPanel(final SettingsPanel sP) {
		add((Component) sP, BorderLayout.CENTER);
	}

	class ButtonPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private void hideWindow() {
			if (window == null) {
				LogBuffer.println("SettingsPanelHolder.hideWindow(): "
						+ "window is null");

			} else {
				window.setVisible(false);
			}
		}

		/**
		 * Subclass constructor
		 */
		public ButtonPanel() {

			final JButton save_button = new JButton("Save");
			save_button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					synchronizeTo();
					if (configNode == null) {
						LogBuffer.println("SettingsPanelHolder.Save: "
								+ "configNode is null");

					} else {
						try {
							configNode.flush();

						} catch (final BackingStoreException e1) {
							e1.printStackTrace();
						}
					}
					hideWindow();
				}
			});
			add(save_button);

			final JButton cancel_button = new JButton("Cancel");
			cancel_button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					synchronizeFrom();
					hideWindow();
				}
			});
			add(cancel_button);
		}
	}
}
