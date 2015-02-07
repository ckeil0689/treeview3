/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: TabbedSettingsPanel.java,v $
 * $Revision: 1.7 $
 * $Date: 2008-06-11 01:58:57 $
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
