package edu.stanford.genetics.treeview;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import Utilities.GUIFactory;
import Utilities.StringRes;

/**
 * A class that generates a JDialog to display some small amounts of
 * information.
 * 
 * @author CKeil
 * 
 */
public class AboutDialog {

	private final JDialog aboutDialog;
	private final TreeViewFrame tvFrame;

	public AboutDialog(final TreeViewFrame tvFrame) {

		this.tvFrame = tvFrame;

		aboutDialog = new JDialog();
		aboutDialog.setTitle(StringRes.dialog_title_about);
		aboutDialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		aboutDialog.setResizable(false);

//		final Dimension mainDim = GUIFactory.getScreenSize();
//
//		aboutDialog.setSize(mainDim.width * 1 / 2, mainDim.height * 1 / 2);

		aboutDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		aboutDialog.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent we) {

				aboutDialog.dispose();
			}
		});

		setupLayout();

		aboutDialog.pack();
		aboutDialog.setLocationRelativeTo(tvFrame.getAppFrame());
	}

	public void setupLayout() {

		final JPanel message = GUIFactory.createJPanel(false, true, null);

		final JLabel text = GUIFactory.createLabel(StringRes.appName
				+ " was created by Chris Keil based on Alok Saldhana's "
				+ "Java TreeView.", GUIFactory.FONTS);

		final JLabel version = GUIFactory.createLabel("Version: " 
				+ StringRes.versionTag, GUIFactory.FONTS);

		message.add(text, "span, wrap");
		message.add(version, "span, wrap");

		final JLabel hp = GUIFactory.createLabel("Homepage", GUIFactory.FONTS);

		message.add(hp);
		message.add(new JTextField(StringRes.updateUrl));

		JButton yesB = GUIFactory.createBtn("Open");
		yesB.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				tvFrame.displayURL(StringRes.updateUrl);
			}

		});
		message.add(yesB, "wrap");

		final JLabel announce = GUIFactory.createLabel("Announcements", 
				GUIFactory.FONTS);

		message.add(announce);
		message.add(new JTextField(StringRes.announcementUrl));

		yesB = GUIFactory.createBtn("Sign Up");
		yesB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {

				tvFrame.displayURL(StringRes.announcementUrl);
			}
		});
		message.add(yesB);

		aboutDialog.add(message);
	}

	/**
	 * Opens the "About" JDialog.
	 */
	public void openAboutDialog() {

		aboutDialog.setVisible(true);
	}
}
