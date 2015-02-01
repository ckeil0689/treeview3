/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: FileMruEditor.java,v $
 * $Revision: 1.2 $
 * $Date: 2010-05-02 13:39:00 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular,
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER
 */
package edu.stanford.genetics.treeview.core;

import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.CdtFilter;
import edu.stanford.genetics.treeview.FileSet;

/**
 * This class allows you to edit the file mru, and also get some info about them
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.2 $ $Date: 2010-05-02 13:39:00 $
 */
public class FileMruEditor {

	private final FileMru client;
	private Window window;
	private JPanel mainPanel;
	private FileSetDisplay fileSetDisplay;
	private ButtonArrangement buttonArrangement;

	private static String[] options = new String[] { "Find...", "Remove",
			"Cancel" };
	/**
	 * Constant signifying what type of action to take. Used to keep track of
	 * options.
	 */
	public final static int FIND = 0;
	/**
	 * Constant signifying what type of action to take. Used to keep track of
	 * options.
	 */
	public final static int REMOVE = 1;
	/**
	 * Constant signifying what type of action to take. Used to keep track of
	 * options.
	 */
	public final static int CANCEL = 2;

	/**
	 * This constructs a full edit panel
	 *
	 * @param fm
	 *            the FileMru to be edited
	 */
	public FileMruEditor(final FileMru fm) {

		client = fm;
		setupWidgets();
	}

	/**
	 * This just offers a search for a particular node...
	 *
	 * @param node
	 *            Node to search for
	 * @param parentComponent
	 *            parent to block
	 * @param message
	 *            text to be displayed
	 * @return Should be one of FIND, DELETE, CANCEL
	 */
	public static int offerSearch(final FileSet node,
			final Window parentComponent, final String message) {

		final JOptionPane pane = new JOptionPane(message,
				JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION, null,
				options);

		final JDialog dialog = pane.createDialog(parentComponent,
				"Problems Loading File!");

		dialog.setVisible(true);
		final Object selectedValue = pane.getValue();

		if (selectedValue == null)
			return CANCEL;

		if (options[0].equals(selectedValue)) {
			// must execute find...
			if (searchFile(node, parentComponent))
				return FIND;
		}

		if (options[1].equals(selectedValue))
			return REMOVE;

		return CANCEL;
	}

	/**
	 * put editor in a top level frame and show
	 */
	public void showDialog(final JFrame appFrame) {

		final JDialog dialog = new JDialog();
		dialog.setTitle(getTitle());
		dialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		dialog.getContentPane().add(mainPanel);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		dialog.pack();
		dialog.setLocationRelativeTo(appFrame);

		window = dialog;
		dialog.setVisible(true);
	}

	/**
	 * Gets the title attribute of the FileMruEditor object
	 *
	 * @return The title value
	 */
	private String getTitle() {

		return "Edit File List";
	}

	/**
	 * sets up widgets
	 */
	private void setupWidgets() {

		mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
		fileSetDisplay = new FileSetDisplay();
		buttonArrangement = new ButtonArrangement();
		buttonArrangement.setThingsSelected(false);

		final JLabel l1 = GUIFactory.createLabel(getTitle(), GUIFactory.FONTS);
		final JPanel upper = fileSetDisplay.getFileSetPanel();
		final JPanel buttonPanel = buttonArrangement.getButtonPanel();

		mainPanel.add(l1, "pushx, alignx 50%, wrap");
		mainPanel.add(upper, "push, grow, alignx 50%, wrap");
		mainPanel.add(buttonPanel, "pushx, alignx 50%");

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	/**
	 * Internal class to encapsulate displaying the FileSets
	 *
	 * @author Alok Saldanha <alok@genome.stanford.edu>
	 */
	private class FileSetDisplay {

		private final JList<FileSet> list;
		private final JPanel fileSetDisplay;

		/**
		 * Constructor for the FileSetPanel object
		 */
		FileSetDisplay() {

			list = new JList<FileSet>();
			list.addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(final ListSelectionEvent e) {
					final int i = list.getSelectedIndex();
					// System.out.println("got selection event, selected is "
					// + i);
					buttonArrangement.setThingsSelected(i >= 0);
				}
			});
			regenList();
			fileSetDisplay = GUIFactory.createJPanel(false, GUIFactory.DEFAULT,
					null);
			final JScrollPane scrollPane = new JScrollPane(list);
			fileSetDisplay.add(scrollPane, "push, grow");
		}

		public JPanel getFileSetPanel() {

			return fileSetDisplay;
		}

		/**
		 * Regenerate list if the datamodel (list) has changed
		 */
		private void regenList() {

			final Preferences[] nodes = client.getConfigs();
			FileSet[] files;
			files = new FileSet[nodes.length];
			for (int i = 0; i < nodes.length; i++) {
				files[i] = new FileSet(nodes[i]);
			}
			list.setListData(files);
		}

		/**
		 * Removes the currently selected files, if any
		 *
		 * @return The index of the first file removed, or -1
		 */
		public int removeSelected() {

			final int[] toRemove = list.getSelectedIndices();
			if (toRemove == null) {
				regenList();
				return -1;
			}

			for (int i = (toRemove.length - 1); i >= 0; i--) {
				final int file = toRemove[i];
				client.removeFile(file);
			}
			client.notifyObservers();

			regenList();
			list.setSelectedIndex(toRemove[0]);

			return toRemove[0];
		}

		/**
		 * removes all fileSets.
		 */
		public void removeAll() {

			final int max = list.getModel().getSize();
			for (int i = (max - 1); i >= 0; i--) {
				client.removeFile(i);
			}
			client.notifyObservers();

			regenList();
		}

		/**
		 * Offers a search for the selected file. Useful if you moved the file.
		 */
		public void searchSelected() {

			final int i = list.getSelectedIndex();
			searchFile(new FileSet(client.getConfig(i)), window);
			client.notifyFileSetModified();
			client.notifyObservers();
			regenList();
		}
	}

	/**
	 * Offers a search for a file corresponding to a fileset. Useful if you
	 * moved the file.
	 *
	 * @param fileSet
	 *            FileSet to find
	 * @param w
	 *            Window to block
	 * @return true if fileset was reassigned.
	 */
	public static boolean searchFile(final FileSet fileSet, final Window w) {

		final JFileChooser fileDialog = new JFileChooser();
		fileDialog.setFileFilter(new CdtFilter());

		final String string = fileSet.getDir();
		if (string != null) {
			fileDialog.setCurrentDirectory(new File(string));
		}

		final int retVal = fileDialog.showOpenDialog(w);
		if (retVal == JFileChooser.APPROVE_OPTION) {
			final File chosen = fileDialog.getSelectedFile();
			fileSet.setCdt(chosen.getName());
			fileSet.setDir(chosen.getParent() + File.separator);
			return true;
		}
		return false;
	}

	/**
	 * Class to encapsulate buttons and callbacks for buttons
	 *
	 * @author Alok Saldanha <alok@genome.stanford.edu>
	 */
	private class ButtonArrangement {

		private final JButton openButton, searchButton, deleteButton,
				deleteAllButton, closeButton;
		private final JPanel buttonPanel;

		/**
		 * Constructor for the ButtonPanel object
		 */
		private ButtonArrangement() {

			buttonPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT,
					null);

			openButton = GUIFactory.createBtn("Open");
			openButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

				}
			});
			// this.add(openButton); not sure about this...

			searchButton = GUIFactory.createBtn("Find");
			searchButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					fileSetDisplay.searchSelected();
				}
			});
			buttonPanel.add(searchButton, "pushx");

			deleteButton = GUIFactory.createBtn("Remove");
			deleteButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					fileSetDisplay.removeSelected();
				}
			});
			buttonPanel.add(deleteButton, "pushx");

			deleteAllButton = GUIFactory.createBtn("Remove All");
			deleteAllButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					fileSetDisplay.removeAll();
				}
			});
			buttonPanel.add(deleteAllButton, "pushx");

			closeButton = GUIFactory.createBtn("Close");
			closeButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {
					window.dispose();
				}
			});
			buttonPanel.add(closeButton, "pushx");
		}

		public JPanel getButtonPanel() {

			return buttonPanel;
		}

		/**
		 * This method is called to let the button panel know if anything is
		 * selected. The button panel will (dis)enable buttons as required.
		 *
		 * @param thingsSelected
		 *            The new thingsSelected value
		 */
		public void setThingsSelected(final boolean thingsSelected) {

			deleteButton.setEnabled(thingsSelected);
			searchButton.setEnabled(thingsSelected);
		}
	}

	/**
	 * test code, loads an XmlConfig...
	 *
	 * @param args
	 *            The command line arguments
	 */
	public final static void main(final String[] args) {

		// final XmlConfig c = new XmlConfig(args[0], "TestConfig");
		// final FileMru fm = new FileMru();
		//
		// fm.bindConfig(c.getNode("FileMru"));
		// final FileMruEditor fme = new FileMruEditor(fm);
		// fme.makeTop();
	}

}
