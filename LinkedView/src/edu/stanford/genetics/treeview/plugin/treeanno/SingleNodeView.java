/*
 * Created on Mar 13, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.plugin.treeanno;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.TreeSelectionI;

/**
 * This View displays an editable representation of the currently selected Tree
 * Node
 */
public class SingleNodeView extends ModelView {
	private TreeSelectionI selection;
	private final HeaderInfo headerInfo;
	/**
	 * index of node currently being edited
	 */
	private int editingIndex;

	public void setSelection(final TreeSelectionI sel) {
		if (selection != null) {
			selection.deleteObserver(this);
		}
		selection = sel;
		selection.addObserver(this);
		if (selection != null) {
			update(selection, null);
		}
	}

	private final JTextField nameF = new JTextField(10);
	private final JTextField annoF = new JTextField(20);

	/**
	 * @param nodeInfo
	 */
	public SingleNodeView(final HeaderInfo nodeInfo) {
		headerInfo = nodeInfo;
		headerInfo.addObserver(this);

		annoF.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				headerInfo.setHeader(editingIndex, "ANNOTATION",
						annoF.getText());
			}
		});
		nameF.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				headerInfo.setHeader(editingIndex, "NAME", nameF.getText());
			}
		});

		final JPanel nameP = new JPanel();
		nameP.add(new JLabel("Name"));
		nameP.add(nameF);

		final JPanel annoP = new JPanel();
		annoP.add(new JLabel("Annotation"));
		annoP.add(annoF);

		final JPanel mainP = new JPanel();
		mainP.add(nameP);
		mainP.add(annoP);

		setLayout(new BorderLayout());
		add(mainP);
	}

	@Override
	public String viewName() {
		return "Single Node Editor";
	}

	@Override
	protected void updateBuffer(final Graphics g) {
		// no buffer here.
	}

	@Override
	public void update(final Observable o, final Object arg) {
		update((Object) o, arg);
	}

	public void update(final Object o, final Object arg) {

		if (o == selection) {
			final String node = selection.getSelectedNode();
			if (node != null) {
				final int i = headerInfo.getHeaderIndex(node);
				if (i >= 0) {
					editingIndex = i;
					synchronizeFrom();
				}
			}
		} else if (o == headerInfo) {
			synchronizeFrom();
		}

	}

	/**
	 * copies values from node into fields for editing.
	 */
	private void synchronizeFrom() {
		nameF.setText(headerInfo.getHeader(editingIndex, "NAME"));
		annoF.setText(headerInfo.getHeader(editingIndex, "ANNOTATION"));
	}

}
