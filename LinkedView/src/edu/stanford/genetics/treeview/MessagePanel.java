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

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

public class MessagePanel extends JPanel {

	private static final long serialVersionUID = 1L;

	protected MessageCanvas messagecanvas;
	protected Vector<String> messages;

	/**
	 * Main constructor
	 * 
	 * @param t
	 * @param bgColor
	 */
	public MessagePanel() {

		super();
		setBorder(null);
		setOpaque(false);
		setLayout(new MigLayout());

		messages = new Vector<String>(5, 5);
		messagecanvas = new MessageCanvas();

		this.add(messagecanvas, "pushx, growx");
	}

	/**
	 * MessageCanvas subclass
	 */
	public class MessageCanvas extends JPanel {

		private static final long serialVersionUID = 1L;

		private final JLabel row;
		private final JLabel col;
		private final JLabel val;
		private final JLabel last;

		private final ArrayList<JLabel> labelList;

		public MessageCanvas() {

			super();
			this.setLayout(new MigLayout("ins 0"));

			row = setupLabel();
			this.add(row, "pushx, wrap");

			col = setupLabel();
			this.add(col, "pushx, wrap");

			val = setupLabel();
			this.add(val, "pushx, wrap");

			last = setupLabel();
			this.add(last, "pushx");

			labelList = new ArrayList<JLabel>();
			labelList.add(row);
			labelList.add(col);
			labelList.add(val);
			labelList.add(last);
		}

		public JLabel setupLabel() {

			final JLabel label = new JLabel();
			label.setFont(GUIParams.FONTS);
			label.setForeground(GUIParams.TEXT);

			return label;
		}

		@Override
		public void paintComponent(final Graphics g) {

			// int xoff = 0;
			// FontMetrics metrics = getFontMetrics(g.getFont());
			// int ascent = metrics.getAscent();
			// int height = 0;
			// Enumeration<String> e = messages.elements();

			final Dimension size = getSize();
			g.clearRect(0, 0, size.width, size.height);

			g.setColor(GUIParams.BG_COLOR);
			g.fillRect(0, 0, size.width, size.height);

			g.setColor(GUIParams.TEXT);

			// while (e.hasMoreElements()) {
			// String message = (String) e.nextElement();
			// if (message == null) {
			// continue;
			// }

			// resetting labels
			for (final JLabel label : labelList) {

				label.setText("");
			}

			// Setting text for all labels
			for (final String message : messages) {

				labelList.get(messages.indexOf(message)).setText(message);
			}

			this.revalidate();
			this.repaint();
			// height += ascent;
			// g.drawString(message, -xoff, height);
			// }
		}

//		@Override
//		public Dimension getPreferredSize() {
//
//			final FontMetrics metrics = getFontMetrics(getFont());
//			final int ascent = metrics.getAscent();
//
//			// for title...
//			int height = ascent;
//			int width = metrics.stringWidth(title);
//			final Enumeration<String> e = messages.elements();
//			while (e.hasMoreElements()) {
//				final String message = e.nextElement();
//				if (message == null) {
//					continue;
//				}
//				height += ascent;
//				final int length = metrics.stringWidth(message);
//				if (width < length) {
//					width = length;
//				}
//			}
//			return new Dimension(width, height);
//		}
	}

	public void setMessages(final String[] m) {

		resetMessages();
		int i;
		for (i = 0; i < m.length; i++) {
			addMessage(m[i]);
		}
		layoutMessages();
	}

	public void resetMessages() {

		messages.removeAllElements();
	}

	public void addMessage(final String message) {

		messages.addElement(message);
	}

	public void layoutMessages() {

		revalidate();
		repaint();
	}
}
