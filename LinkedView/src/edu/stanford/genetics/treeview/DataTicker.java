/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import Utilities.GUIFactory;
import net.miginfocom.swing.MigLayout;

public class DataTicker {

	private final JPanel tickerPanel;
	private final JTextArea valTextArea;
	/** 
	 * describes which of the average values that is being displayed
	 * e.g. Data Value, Row Ave, Col Ave...
	 */
	private final JLabel textLabel;

	/**
	 * Creates a new DataTicker instance.
	 */
	public DataTicker() {

		this.valTextArea = GUIFactory.createWrappableTextArea();
		this.textLabel = GUIFactory.createLabel("",GUIFactory.FONTS);
		textLabel.setHorizontalAlignment(SwingConstants.RIGHT);
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
//			setBorder(BorderFactory.createTitledBorder("Tile Value"));

			setupDataTicker();
		}

		private void setupDataTicker() {
			
			final JLabel val = GUIFactory.createLabel(":",
					GUIFactory.FONTS);
			add(textLabel, "w 80!");
			add(val);
			add(valTextArea, "wrap");

			revalidate();
			repaint();
		}
	}

	/**
	 * Updates the JTextArea which displays the value as a String.
	 * @param val - The value to be displayed as String.
	 */
	public void setValue(final double val) {
		
		String val_s = Double.toString(val);
		valTextArea.setText(val_s);
	}
	
	/**
	 * Updates the JTextArea which displays the String.
	 * @param text - The value to be displayed as String.
	 */
	public void setText(String text){
		textLabel.setText(text);
	}
	
}
