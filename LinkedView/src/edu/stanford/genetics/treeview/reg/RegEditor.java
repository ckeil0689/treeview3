/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.reg;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Hashtable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import edu.stanford.genetics.treeview.BrowserControl;
import edu.stanford.genetics.treeview.TreeViewApp;

/**
 * 
 * Allows users to edit their registration information prior to submission.
 * 
 * Should allow editing of the user-specified keys from the RegEngine, and
 * display values of the auto-determined fields.
 * 
 * Note that if the key ends with Okay, it will be treated as a boolean for
 * display and editing purposes.
 * 
 * @author aloksaldanha
 * 
 */
public class RegEditor extends JPanel {

	Entry dataSource;
	GridBagLayout gridbag;
	GridBagConstraints gbc;

	/**
	 * @param entry
	 */
	public RegEditor(final Entry entry) {
		dataSource = entry;
		addWidgets();
	}

	/**
	 * 
	 */
	private void addWidgets() {
		// setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		gridbag = new GridBagLayout();
		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		setLayout(gridbag);
		for (int i = 0; i < dataSource.getNumRegKeys(); i++) {
			gbc.gridy = i;
			addAttribute(i, dataSource.isEditable(i));
		}
	}

	Hashtable attr2val = new Hashtable();

	/**
	 * @param i
	 *            index of key corresponding to attribute
	 * @param isEditable
	 *            indicates whether attribute should be editable.
	 */
	private void addAttribute(final int i, final boolean isEditable) {
		final String key = dataSource.getRegKey(i);
		gbc.gridx = 0;
		if (isEditable) {
			final JPanel inner = new JPanel();
			if (key.equals("contactOkay")) {
				final Box inner2 = new Box(BoxLayout.Y_AXIS);
				inner2.add(new JLabel(
						"May we contact you when new\n versions become available?"));
				inner2.add(new JLabel(
						"(Note: a browser will open for mailing list signup)"));
				/*
				 * inner2.add(new JLabel(
				 * "To recieve annoucements about new versions, add yourself to"
				 * )); inner2.add(new JLabel(
				 * "the jtreeview-announce email list. This list is very  low"
				 * )); inner2.add(new JLabel("volume (< 1 email/month)"));
				 */
				inner.add(inner2);
			} else {
				inner.add(new JLabel(key));
			}
			final JLabel star = new JLabel("*");
			star.setForeground(Color.red);
			inner.add(star);
			gridbag.setConstraints(inner, gbc);
			add(inner);
		} else {
			final JLabel label = new JLabel(key);
			gridbag.setConstraints(label, gbc);
			add(label);
		}
		gbc.gridx = 1;
		if (key.equals("contactOkay2")) {
			// this code is never executed, since I decided to have the browser
			// window
			// open when the reg dialog closes.
			final Box box = new Box(BoxLayout.Y_AXIS);
			box.add(new JTextField(TreeViewApp.getAnnouncementUrl()));
			final JButton yesB = new JButton("Open in browser");
			yesB.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent arg0) {
					final BrowserControl bc = BrowserControl
							.getBrowserControl();
					try {
						bc.displayURL(TreeViewApp.getAnnouncementUrl());
					} catch (final IOException e) {
						JOptionPane.showMessageDialog(
								RegEditor.this,
								"Failed to upen url "
										+ TreeViewApp.getAnnouncementUrl());
						e.printStackTrace();
					}
				}

			});
			box.add(yesB);
			gridbag.setConstraints(box, gbc);
			add(box);
		} else if (key.endsWith("Okay")) {
			final JPanel box = new JPanel();
			final ButtonGroup group = new ButtonGroup();
			final JRadioButton yesB = new JRadioButton("Yes");
			final JRadioButton noB = new JRadioButton("No");
			attr2val.put(key, yesB);
			group.add(yesB);
			group.add(noB);
			box.add(yesB);
			box.add(noB);
			if (dataSource.getRegValue(i).equals("N")) {
				noB.setSelected(true);
			} else {
				yesB.setSelected(true);
			}
			gridbag.setConstraints(box, gbc);
			add(box);
		} else {
			final JTextField field = new JTextField(dataSource.getRegValue(i));
			attr2val.put(key, field);
			field.setEditable(isEditable);
			field.setEnabled(isEditable);
			gridbag.setConstraints(field, gbc);
			add(field);
		}
	}

	public String getAttribute(final String attr) {
		final Object control = attr2val.get(attr);
		if (attr.endsWith("Okay")) {
			// boolean attribute
			if (((JRadioButton) control).isSelected()) {
				return "Y";
			} else {
				return "N";
			}
		} else {
			return ((JTextField) control).getText();
		}
	}

	/**
	 * 
	 * Inner class to represent attributes.
	 * 
	 * @author aloksaldanha
	 * 
	 */
	class AttributePanel extends JPanel {

		/**
		 * @param i
		 *            index of reg key to represent
		 * @param isEditable
		 *            indicates whether the user should be able to edit this
		 *            entry.
		 */
		public AttributePanel(final int i, final boolean isEditable) {
			add(new JLabel(dataSource.getRegKey(i)));
			add(new JTextField(dataSource.getRegValue(i)));
		}
	}

}
