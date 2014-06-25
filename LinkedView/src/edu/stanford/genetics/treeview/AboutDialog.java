package edu.stanford.genetics.treeview;

import java.awt.Dialog;
import java.awt.Dimension;
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

import net.miginfocom.swing.MigLayout;

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
		aboutDialog.setTitle("About...");
		aboutDialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		aboutDialog.setResizable(false);

		final Dimension mainDim = GUIFactory.getScreenSize();

		aboutDialog.setSize(mainDim.width * 1 / 2, mainDim.height * 1 / 2);

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

		final JPanel message = new JPanel();
		message.setLayout(new MigLayout());
		message.setBackground(GUIFactory.BG_COLOR);

		final JLabel text = new JLabel(StringRes.appName
				+ " was created by Chris Keil based on Alok Saldhana's "
				+ "Java TreeView.");
		text.setFont(GUIFactory.FONTS);
		text.setForeground(GUIFactory.TEXT);

		final JLabel version = new JLabel("Version: " + StringRes.versionTag);
		version.setFont(GUIFactory.FONTS);
		version.setForeground(GUIFactory.TEXT);

		message.add(text, "span, wrap");
		message.add(version, "span, wrap");

		final JLabel hp = new JLabel("Homepage");
		hp.setFont(GUIFactory.FONTS);
		hp.setForeground(GUIFactory.TEXT);

		message.add(hp);
		message.add(new JTextField(StringRes.updateUrl));

		JButton yesB = GUIFactory.setButtonLayout("Open", null);
		yesB.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				tvFrame.displayURL(StringRes.updateUrl);
			}

		});
		message.add(yesB, "wrap");

		final JLabel announce = new JLabel("Announcements");
		announce.setFont(GUIFactory.FONTS);
		announce.setForeground(GUIFactory.TEXT);

		message.add(announce);
		message.add(new JTextField(StringRes.announcementUrl));

		yesB = GUIFactory.setButtonLayout("Sign Up", null);
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
