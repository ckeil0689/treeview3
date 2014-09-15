package edu.stanford.genetics.treeview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
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
public class AboutDialog extends CustomDialog{

	private final TreeViewFrame tvFrame;

	public AboutDialog(final TreeViewFrame tvFrame) {

		super(StringRes.dlg_about);
		this.tvFrame = tvFrame;

		setupLayout();

		dialog.pack();
		dialog.setLocationRelativeTo(tvFrame.getAppFrame());
	}

	/**
	 * Setup the layout and content of the mainPanel and add it to the
	 * dialog.
	 */
	public void setupLayout() {

		final JLabel text = GUIFactory.createLabel(StringRes.appName
				+ " was created by Chris Keil based on Alok Saldhana's "
				+ "Java TreeView.", GUIFactory.FONTS);

		final JLabel version = GUIFactory.createLabel("Version: " 
				+ StringRes.versionTag, GUIFactory.FONTS);

		mainPanel.add(text, "span, wrap");
		mainPanel.add(version, "span, wrap");

		final JLabel hp = GUIFactory.createLabel("Homepage", GUIFactory.FONTS);

		mainPanel.add(hp);
		mainPanel.add(new JTextField(StringRes.updateUrl));

		JButton yesB = GUIFactory.createBtn("Open");
		yesB.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				tvFrame.displayURL(StringRes.updateUrl);
			}

		});
		mainPanel.add(yesB, "wrap");

		final JLabel announce = GUIFactory.createLabel("Announcements", 
				GUIFactory.FONTS);

		mainPanel.add(announce);
		mainPanel.add(new JTextField(StringRes.announcementUrl));

		yesB = GUIFactory.createBtn("Sign Up");
		yesB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {

				tvFrame.displayURL(StringRes.announcementUrl);
			}
		});
		mainPanel.add(yesB);

		dialog.add(mainPanel);
	}
}
