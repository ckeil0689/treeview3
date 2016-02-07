/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
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
import utilities.GUIFactory;

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

			final JTextArea rowText = GUIFactory.createWrappableTextArea();
			add(rowText, "w 90%, growx, wrap");

			final JLabel col = GUIFactory.createLabel("Column:",
					GUIFactory.FONTS);
			add(col, "w 10%");

			final JTextArea colText = GUIFactory.createWrappableTextArea();
			add(colText, "w 90%, growx, wrap");

			final JLabel val = GUIFactory.createLabel("Value:",
					GUIFactory.FONTS);
			add(val, "w 10%");

			final JTextArea valText = GUIFactory.createWrappableTextArea();
			add(valText, "w 90%, growx, wrap");

			textList.add(rowText);
			textList.add(colText);
			textList.add(valText);

			revalidate();
			repaint();
		}
	}

	public void setMessages(final String[] m) {

		for (int i = 0; i < m.length; i++) {

			if (i >= textList.size()) {
				break;
			}

			String message = m[i];

			if (message == null || message.length() == 0) {
				message = "-";
			}

			textList.get(i).setText(message);
		}
	}
}
