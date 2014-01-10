package edu.stanford.genetics.treeview.core;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeViewApp;

public class GlobalPrefInfo extends JPanel implements Observer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JPanel msgPanel = new JPanel();
	private final JTextField txtField = new JTextField();
	private final JTextArea logArea = new JTextArea();
	private TreeViewApp app = null;

	public GlobalPrefInfo(final TreeViewApp appArg) {
		app = appArg;
		setLayout(new BorderLayout());
		txtField.setText(TreeViewApp.globalConfigName());
		txtField.setEditable(false);
		final JButton testButton = new JButton("Test!");
		testButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final LogBuffer lb = LogBuffer.getSingleton();
				final boolean origStatus = lb.getLog();
				lb.setLog(true);
				lb.addObserver(GlobalPrefInfo.this);
				app.getGlobalConfig().getNode("Test")
						.setAttribute("Hello", "Hello", "");
				app.getGlobalConfig().getNode("Test")
						.setAttribute("Hello", "World", "");
				app.getGlobalConfig().store();

				lb.deleteObserver(GlobalPrefInfo.this);
				lb.setLog(origStatus);
			}
		});
		msgPanel.add(new JLabel("Global preferences are stored in "));
		msgPanel.add(txtField);
		msgPanel.add(testButton);
		add(msgPanel, BorderLayout.NORTH);
		logArea.setAutoscrolls(true);
		logArea.setRows(10);

		add(new JScrollPane(logArea), BorderLayout.CENTER);
	}

	@Override
	public void update(final Observable arg0, final Object arg1) {
		logArea.append((String) arg1);
		logArea.append("\n");
	}
}
