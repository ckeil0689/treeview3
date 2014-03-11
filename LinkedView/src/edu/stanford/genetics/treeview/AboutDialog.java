package edu.stanford.genetics.treeview;

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

import net.miginfocom.swing.MigLayout;

/**
 * A class that generates a JDialog to display some 
 * small amounts of information.
 * @author CKeil
 *
 */
public class AboutDialog {

	private JDialog aboutDialog;
	private TreeViewFrame tvFrame;
	
	public AboutDialog(final TreeViewFrame tvFrame) {
		
		this.tvFrame = tvFrame;
		
		aboutDialog = new JDialog();
		aboutDialog.setTitle("About...");
		aboutDialog.setModalityType(JDialog.DEFAULT_MODALITY_TYPE);
		aboutDialog.setResizable(false);
		
		final Dimension mainDim = GUIParams.getScreenSize();
		
		aboutDialog.setSize(mainDim.width * 1/2, 
				mainDim.height * 1/2);
		
		aboutDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
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
		message.setBackground(GUIParams.BG_COLOR);
		
		JLabel text = new JLabel(tvFrame.getAppName() + 
				" was created by Chris Keil based on Alok Saldhana's " +
				"Java TreeView.");
		text.setFont(GUIParams.FONTS);
		text.setForeground(GUIParams.TEXT);
		
		JLabel version = new JLabel("Version: " + TreeViewApp.getVersionTag());
		version.setFont(GUIParams.FONTS);
		version.setForeground(GUIParams.TEXT);
		
		message.add(text, "span, wrap");
		message.add(version, "span, wrap");
		
		JLabel hp = new JLabel("Homepage");
		hp.setFont(GUIParams.FONTS);
		hp.setForeground(GUIParams.TEXT);
		
		message.add(hp);
		message.add(new JTextField(TreeViewApp.getUpdateUrl()));

		JButton yesB = GUIParams.setButtonLayout("Open", null);
		yesB.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {
				
				tvFrame.displayURL(TreeViewApp.getUpdateUrl());
			}

		});
		message.add(yesB, "wrap");

		JLabel announce = new JLabel("Announcements");
		announce.setFont(GUIParams.FONTS);
		announce.setForeground(GUIParams.TEXT);
		
		message.add(announce);
		message.add(new JTextField(TreeViewApp.getAnnouncementUrl()));

		yesB = GUIParams.setButtonLayout("Sign Up", null);
		yesB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				
				tvFrame.displayURL(TreeViewApp.getAnnouncementUrl());
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
