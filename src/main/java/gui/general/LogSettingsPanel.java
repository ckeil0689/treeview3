/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package gui.general;

import util.LogBuffer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

public class LogSettingsPanel extends JPanel implements Observer {

	private static final long serialVersionUID = 1L;

	private final LogBuffer logBuffer;
	private final JCheckBox logBox = new JCheckBox("Log Messages");

	public LogSettingsPanel(final LogBuffer buffer) {
		super();
		add(logBox);
		logBuffer = buffer;
		logBuffer.addObserver(this);
		logBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				synchronizeTo();
			}

		});

		synchronizeFrom();
	}

	private void synchronizeFrom() {
		if (logBox.isSelected() != logBuffer.getLog()) {
			logBox.setSelected(logBuffer.getLog());
		}
	}

	private void synchronizeTo() {
		logBuffer.setLog(logBox.isSelected());
	}

	@Override
	public void update(final Observable arg0, final Object arg1) {
		if (arg1 == null) {
			synchronizeFrom();
		}
	}
}
