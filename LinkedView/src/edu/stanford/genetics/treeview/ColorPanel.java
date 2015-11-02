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
import javax.swing.JPanel;

/**
 * encapsulates a panel which can be used to edit a single color within a color
 * set.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.4 $ $Date: 2004-12-21 03:28:13 $
 */
public class ColorPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	ColorIcon colorIcon;
	int type;
	ColorSetI colorSet;

	/**
	 * @param i
	 *            the type of color panel. This is the type as defined in the
	 *            <code>ColorSetI</code>, and is how this panel knows what color
	 *            it is supposed to represent.
	 * @param colorS
	 *            the <code>ColorSetI</code> which this panel represents and
	 *            modifies.
	 */
	public ColorPanel(final int i, final ColorSetI colorS) {
		colorSet = colorS;
		type = i;
		redoComps();
	}

	/** refresh the color of the icon from the <code>ColorSetI</code> */
	public void redoColor() {
		colorIcon.setColor(getColor());
	}

	/** Remake the UI components */
	public void redoComps() {
		removeAll();
		colorIcon = new ColorIcon(10, 10, getColor());
		final JButton pushButton = new JButton(getLabel(), colorIcon);
		pushButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final Color trial = JColorChooser.showDialog(ColorPanel.this,
						"Pick Color for " + getLabel(), getColor());
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
