/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * This class allows editing of a color set.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.4 $ $Date: 2004-12-21 03:28:14 $
 */

public class ConfigColorSetEditor extends JPanel {

	private static final long serialVersionUID = 1L;

	private final ConfigColorSet colorSet;

	/**
	 * Constructor for the ConfigColorSetEditor object
	 *
	 * @param colorSet
	 *            A ConfigColorSet to be edited
	 */
	public ConfigColorSetEditor(final ConfigColorSet colorSet) {
		this.colorSet = colorSet;
		final String[] types = colorSet.getTypes();
		for (int i = 0; i < types.length; i++) {
			add(new ColorPanel(i));
		}
	}

	/**
	 * A simple test program. Allowed editing, and prints out the results.
	 *
	 * @param argv
	 *            none required.
	 */
	public final static void main(final String[] argv) {
		final String[] types = new String[] { "Up", "Down", "Left", "Right" };
		final String[] colors = new String[] { "#FF0000", "#FFFF00", "#FF00FF",
				"#00FFFF" };

		final ConfigColorSet temp = new ConfigColorSet("Bob", types, colors);
		final ConfigColorSetEditor cse = new ConfigColorSetEditor(temp);
		final JFrame frame = new JFrame("ColorSetEditor Test");
		frame.getContentPane().add(cse);
		frame.pack();
		frame.setVisible(true);
	}

	/** this has been superceded by the general ColorPanel class */
	class ColorPanel extends JPanel {

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
							ConfigColorSetEditor.this, "Pick Color for "
									+ getLabel(), getColor());
					if (trial != null) {
						setColor(trial);
					}
				}
			});

			add(pushButton);
		}

		private void setColor(final Color c) {
			colorSet.setColor(type, c);
			colorIcon.setColor(getColor());
			repaint();
		}

		private String getLabel() {
			return colorSet.getType(type);
		}

		private Color getColor() {
			return colorSet.getColor(type);
		}
	}

}
