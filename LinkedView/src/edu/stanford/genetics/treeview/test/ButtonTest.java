package edu.stanford.genetics.treeview.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;

import edu.stanford.genetics.treeview.app.LinkedViewApp;
public class ButtonTest {
	public static final void main(final String[] argv) {
		JDialog jd = new JDialog();
		JButton but = new JButton("Push Me");
		but.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LinkedViewApp.main(argv);
			}
		});
		jd.add(but);
		jd.setVisible(true);
	}
}
