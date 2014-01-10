/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: LoadProgress.java,v $
 * $Revision: 1.7 $
 * $Date: 2008-06-11 01:58:58 $
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
package edu.stanford.genetics.treeview.model;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * A simple progress dialog. It has a progress bar, a text area which lines can
 * be added to, and a cancel button with customizable text.
 * 
 * @author aloksaldanha
 * 
 */
public class LoadProgress extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JProgressBar progressBar;
	private final JTextArea taskOutput;
	private final String newline = "\n";
	private final JButton closeButton;

	private boolean canceled;

	public void clear() {

		taskOutput.setText("");
	}

	public void println(final String s) {

		taskOutput.append(s + newline);
		taskOutput.setCaretPosition(taskOutput.getDocument().getLength());
	}

	public void setButtonText(final String text) {

		closeButton.setText(text);
	}

	public void setLength(final int i) {

		setIndeterminate(false);
		if (progressBar.getMaximum() != i) {
			progressBar.setMinimum(0);
			progressBar.setMaximum(i);
		}
	}

	public void setValue(final int i) {

		progressBar.setValue(i);
	}

	public void setIndeterminate(final boolean flag) {
		// actually, this only works in jdk 1.4 and up...
		// progressBar.setIndeterminate(flag);
	}

	public LoadProgress(final String title, final Frame f) {

		super(f, title, true);

		JPanel panel, contentPane;

		progressBar = new JProgressBar();
		progressBar.setValue(0);
		progressBar.setStringPainted(true);

		taskOutput = new JTextArea(10, 40);
		taskOutput.setMargin(new Insets(5, 5, 5, 5));
		taskOutput.setEditable(false);

		panel = new JPanel();
		panel.add(progressBar);

		contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(panel, BorderLayout.NORTH);
		contentPane.add(new JScrollPane(taskOutput), BorderLayout.CENTER);

		closeButton = new JButton("Cancel");
		closeButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				setCanceled(true);
				LoadProgress.this.dispose();
			}
		});

		panel = new JPanel();
		panel.add(closeButton);
		contentPane.add(panel, BorderLayout.SOUTH);

		contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		setContentPane(contentPane);
	}

	/** Setter for canceled */
	public void setCanceled(final boolean canceled) {

		this.canceled = canceled;
	}

	/** Getter for canceled */
	public boolean getCanceled() {

		return canceled;
	}
}
