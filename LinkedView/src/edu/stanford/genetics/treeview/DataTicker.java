/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: MessagePanel.java,v $
 * $Revision: 1.6 $
 * $Date: 2004-12-21 03:28:13 $
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

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;
import Utilities.GUIFactory;
	
public class DataTicker {

	private final JPanel tickerPanel;
	private final List<JTextArea> textList;

	/**
	 * Creates a new DataTicker instance.
	 */
	public DataTicker() {

		this.textList = new ArrayList<JTextArea>();
		this.tickerPanel = new TickerPanel();
	}

	public JPanel getTickerPanel() {

		return tickerPanel;
	}

	/**
	 * MessageCanvas subclass
	 */
	public class TickerPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		public TickerPanel() {

			super();

			setLayout(new MigLayout());
			setOpaque(false);
			setBorder(BorderFactory.createTitledBorder("Data Ticker"));

			setupDataTicker();
		}

		private void setupDataTicker() {

			final JLabel row = GUIFactory.createLabel("Row:", GUIFactory.FONTS);
			add(row, "w 10%");

			final JTextArea rowText = setupLabel();
			add(rowText, "w 90%, growx, wrap");

			final JLabel col = GUIFactory.createLabel("Column:",
					GUIFactory.FONTS);
			add(col, "w 10%");

			final JTextArea colText = setupLabel();
			add(colText, "w 90%, growx, wrap");

			final JLabel val = GUIFactory.createLabel("Value:",
					GUIFactory.FONTS);
			add(val, "w 10%");

			final JTextArea valText = setupLabel();
			add(valText, "w 90%, growx, wrap");

			textList.add(rowText);
			textList.add(colText);
			textList.add(valText);

			revalidate();
			repaint();
		}

		private JTextArea setupLabel() {

			final JTextArea label = new JTextArea();
			label.setFont(GUIFactory.FONTS);
			label.setBorder(null);
			label.setOpaque(false);
			label.setEditable(false);
			label.setFocusable(false);
			label.setLineWrap(true);
			label.setWrapStyleWord(true);

			return label;
		}
	}

	public void setMessages(final String[] m) {
		
		for(int i = 0; i < m.length; i++) {
			
			String message = m[i];
			
			if(message == null || message.length() == 0) {
				message = "-";
			}
			
			if(i >= textList.size()) break;
			
			textList.get(i).setText(message);
		}
	}
}
