/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import javax.swing.JTabbedPane;

/**
 * This class is actually a settings panel container. You can put multiple
 * settings panels within this container and tab between them. It does not
 * provide a save/cancel button, for that use a SettingsPanelHolder.
 *
 * @author aloksaldanha
 *
 */
public class TabbedSettingsPanel extends JTabbedPane implements SettingsPanel {

	private static final long serialVersionUID = 1L;

	@Override
	public void setSelectedIndex(final int i) {
		synchronizeFrom(i);
		super.setSelectedIndex(i);
	}

	@Override
	public void synchronizeTo() {

		final int n = getTabCount();
		for (int i = 0; i < n; i++) {

			synchronizeTo(i);
		}
	}

	public void synchronizeTo(final int i) {

		((SettingsPanel) getComponentAt(i)).synchronizeTo();
	}

	@Override
	public void synchronizeFrom() {

		final int n = getTabCount();
		for (int i = 0; i < n; i++) {

			((SettingsPanel) getComponentAt(i)).synchronizeFrom();
		}
	}

	public void synchronizeFrom(final int i) {

		((SettingsPanel) getComponentAt(i)).synchronizeFrom();
	}

	public void addSettingsPanel(final String name, final SettingsPanel sP) {
		addTab(name, (java.awt.Component) sP);
	}
}
