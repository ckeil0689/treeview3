package edu.stanford.genetics.treeview;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import Utilities.CustomDialog;
import Utilities.GUIFactory;
import Utilities.StringRes;

/**
 * A class that generates a JDialog to display some small amounts of
 * information.
 *
 * @author CKeil
 *
 */
public class AboutDialog extends CustomDialog {

	/**
	 * Default serial version ID to keep Eclipse happy...
	 */
	private static final long serialVersionUID = 1L;
	
	private final TreeViewFrame tvFrame;

	public AboutDialog(final TreeViewFrame tvFrame) {

		super(StringRes.dlg_about);
		this.tvFrame = tvFrame;

		setupLayout();

	}

	/**
	 * Setup the layout and content of the mainPanel and add it to the dialog.
	 */
	private void setupLayout() {

		/*
		 * TextArea because JLabel cannot properly line break without manual
		 * HTML....
		 */
		final JTextArea text = GUIFactory.createWrappableTextArea();
		text.setText(StringRes.appName
				+ " was created by Chris Keil, Lance Parsons, "
				+ "Robert Leach & Anastasia Baryshnikova and "
				+ "is based on Alok Saldanha's Java TreeView.");

		final JLabel version = GUIFactory.createLabel("Version:",
				GUIFactory.FONTS);

		final JLabel tag = GUIFactory.createLabel(StringRes.versionTag,
				GUIFactory.FONTS);

		final JPanel contentPanel = GUIFactory.createJPanel(false,
				GUIFactory.DEFAULT);

		final JPanel detailPanel = GUIFactory.createJPanel(false,
				GUIFactory.DEFAULT);
		detailPanel.setBorder(BorderFactory.createTitledBorder("Details"));

		contentPanel.add(text, "span, grow, wrap");
		detailPanel.add(version);
		detailPanel.add(tag, "wrap");

		final JLabel hp = GUIFactory.createLabel("Homepage:", GUIFactory.FONTS);

		detailPanel.add(hp);
		detailPanel.add(new JTextField(StringRes.updateUrl));

		JButton yesB = GUIFactory.createBtn("Open");
		yesB.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				tvFrame.displayURL(StringRes.updateUrl);
			}

		});
		detailPanel.add(yesB, "wrap");

		final JLabel announce = GUIFactory.createLabel("Announcements:",
				GUIFactory.FONTS);

		detailPanel.add(announce);
		detailPanel.add(new JTextField(StringRes.announcementUrl));

		yesB = GUIFactory.createBtn("Sign Up");
		yesB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {

				tvFrame.displayURL(StringRes.announcementUrl);
			}
		});
		detailPanel.add(yesB, "wrap");
		
		mainPanel.add(detailPanel, "push, grow");
	}
}
