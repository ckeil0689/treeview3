/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.ColorIcon;

/**
 * This class allows editing of a color set...
 *
 * NOTE: This is superceded by the ConfigColorSet stuff in
 * edu.stanford.genetics.treeview, although this code is still used within the
 * dendroview package.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.1 $ $Date: 2006-08-16 19:13:46 $
 */

public class ColorSetEditor extends JPanel {

	private static final long serialVersionUID = 1L;

	private final static int UP = 0;
	private final static int ZERO = 1;
	private final static int DOWN = 2;
	private final static int MISSING = 3;
	private final ColorSet colorSet;

	/**
	 * Constructor for the ColorSetEditor object
	 *
	 * @param colorSet
	 *            <code>ColorSet</code> to be edited
	 */
	public ColorSetEditor(final ColorSet colorSet) {

		this.colorSet = colorSet;
		add(new ColorPanel(UP));
		add(new ColorPanel(ZERO));
		add(new ColorPanel(DOWN));
		add(new ColorPanel(MISSING));
	}

	// /**
	// * A simple test program
	// *
	// * @param argv
	// * ignored
	// */
	// public final static void main(final String[] argv) {
	//
	// final ColorSet temp = new ColorSet();
	// final ColorSetEditor cse = new ColorSetEditor(temp);
	// final JFrame frame = new JFrame("ColorSetEditor Test");
	// frame.getContentPane().add(cse);
	// frame.pack();
	// frame.setVisible(true);
	// }

	class ColorPanel extends JPanel {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		ColorIcon colorIcon;
		int type;

		ColorPanel(final int i) {
			type = i;
			redoComps();
		}

		public void redoComps() {
			removeAll();
			colorIcon = new ColorIcon(10, 10, getColor());
			final JButton pushButton = new JButton(getLabel(), colorIcon);
			pushButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					final Color trial = JColorChooser.showDialog(
							ColorSetEditor.this,
							"Pick Color for " + getLabel(), getColor());
					if (trial != null) {
						setColor(trial);
					}
				}
			});

			add(pushButton);
		}

		private void setColor(final Color c) {
			switch (type) {
			// case UP:
			// colorSet.setUp(c);
			// break;
			// case ZERO:
			// colorSet.setZero(c);
			// break;
			// case DOWN:
			// colorSet.setDown(c);
			// break;
			case MISSING:
				colorSet.setMissing(c);
				break;
			}
			colorIcon.setColor(getColor());
			// redoComps();
			repaint();
		}

		private String getLabel() {
			switch (type) {
			case UP:
				return "Positive";
			case ZERO:
				return "Zero";
			case DOWN:
				return "Negative";
			case MISSING:
				return "Missing";
			}
			return null;
		}

		private Color getColor() {
			switch (type) {
			// case UP:
			// return colorSet.getUp();
			// case ZERO:
			// return colorSet.getZero();
			// case DOWN:
			// return colorSet.getDown();
			case MISSING:
				return colorSet.getMissing();
			}
			return null;
		}
	}

}
