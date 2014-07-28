/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: ModelessSettingsDialog.java,v $
 * $Revision: 1.4 $
 * $Date: 2004-12-21 03:28:14 $
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import Utilities.GUIFactory;

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

		final JPanel inner = GUIFactory.createJPanel(false, false, null);
		
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
