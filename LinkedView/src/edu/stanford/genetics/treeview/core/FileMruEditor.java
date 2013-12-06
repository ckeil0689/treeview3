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


import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import net.miginfocom.swing.MigLayout;

import edu.stanford.genetics.treeview.*;
/**
 *  This class allows you to edit the file mru, and also get some info about
 *  them
 *
 * @author     Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.2 $ $Date: 2010-05-02 13:39:00 $
 */
public class FileMruEditor extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private FileMru client;
	private Window window;
	private FileSetPanel fileSetPanel;
	private ButtonPanel buttonPanel;

	private static String[] options    = new String[]
			{"Find...", "Remove", "Cancel"};
	/**
	 *  Constant signifying what type of action to take. 
	 *  Used to keep track of options.
	 */
	public final static int FIND       = 0;
	/**
	 *  Constant signifying what type of action to take. 
	 *  Used to keep track of options.
	 */
	public final static int REMOVE     = 1;
	/**
	 *  Constant signifying what type of action to take. 
	 *  Used to keep track of options.
	 */
	public final static int CANCEL     = 2;


	/**
	 *  This constructs a full edit panel
	 *
	 * @param  fm  the FileMru to be edited
	 */
	public FileMruEditor(FileMru fm) {
		super();
		client = fm;
		setupWidgets();
	}


	/**
	 *  This just offers a search for a particular node...
	 *
	 * @param  node             Node to search for
	 * @param  parentComponent  parent to block
	 * @param  message          text to be displayed
	 * @return                  Should be one of FIND, DELETE, CANCEL
	 */
	public static int offerSearch(FileSet node, Window parentComponent, 
			String message) {

		JOptionPane pane = new JOptionPane(message, JOptionPane.ERROR_MESSAGE, 
				JOptionPane.DEFAULT_OPTION, null, options);
	
		JDialog dialog  = pane.createDialog(parentComponent, 
				"Problems Loading File!");

		dialog.setVisible(true);
		Object selectedValue = pane.getValue();
		
		if (selectedValue == null) {
			return CANCEL;
		}

		if (options[0].equals(selectedValue)) {
			// must execute find...
			if (searchFile(node, parentComponent)) {
				return FIND;
			}
		}

		if (options[1].equals(selectedValue)) {
			return REMOVE;
		}

		return CANCEL;
	}


	/**
	 *  put editor in a top level frame and show
	 */
	public void makeTop() {
		
		Frame top = new Frame(getTitle());
		top.add(this);
		top.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent we) {
				we.getWindow().dispose();
			}
		});
		top.pack();
		
		Dimension d  = top.getSize();
		if (d.width < 600) {
			top.setSize(600, d.height);
		}
		window = top;
		top.setVisible(true);
	}


	/**
	 *  put editor in a dialog
	 *
	 * @param  f  Window to block
	 */
	public void showDialog(Frame f) {
		
		JDialog d  = new JDialog(f, getTitle(), true);
		d.setContentPane(this);
		d.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent we) {
				we.getWindow().dispose();
			}
		});
		d.pack();
		
		window = d;
		Dimension ts = d.getSize();
		Dimension dim  = f.getSize();
		if (dim.height/2 < ts.height ) {
			d.setSize(ts.width, dim.height/2);
		}
		System.out.println("Size of parent " + dim + " my size " + ts);
		d.setVisible(true);
	}


	/**
	 *  Gets the title attribute of the FileMruEditor object
	 *
	 * @return    The title value
	 */
	private String getTitle() {
		
		return "Edit File List";
	}


	/**
	 *  sets up widgets
	 */
	private void setupWidgets() {
		
		fileSetPanel = new FileSetPanel();
		buttonPanel = new ButtonPanel();
		buttonPanel.setThingsSelected(false);
		
		JPanel upper  = fileSetPanel;
		upper.setSize(300,200);
		this.setLayout(new MigLayout());
		this.setBackground(GUIParams.BG_COLOR);
		
		JLabel l1 = new JLabel(getTitle());
		l1.setForeground(GUIParams.TEXT);
		
		this.add(l1, "pushx, alignx 50%, wrap");
		this.add(upper, "push, grow, alignx 50%, wrap");
		this.add(buttonPanel, "pushx, alignx 50%");
		this.revalidate();
		this.repaint();
	}

	/**
	 *  Internal class to encapsulate displaying the FileSets
	 *
	 * @author     Alok Saldanha <alok@genome.stanford.edu>
	 */
	private class FileSetPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		
		private JList list;

		/**
		 *  Constructor for the FileSetPanel object
		 */
		FileSetPanel() {
			
			list = new JList();
			list.addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
					int i  = list.getSelectedIndex();
//						System.out.println("got selection event, selected is " 
//					+ i);
						buttonPanel.setThingsSelected(i >= 0);
					}
				});
			regenList();
			this.setLayout(new MigLayout("ins 0"));
			list.setMinimumSize(new Dimension(10,10));
			JScrollPane scrollPane = new JScrollPane(list); 
			this.add(scrollPane, "push, grow");
		}


		/**
		 *  Regenerate list if the datamodel (list) has changed
		 */
		private void regenList() {
		
			ConfigNode[] nodes  = client.getConfigs();
			FileSet[] files;
			files = new FileSet[nodes.length];
			for (int i = 0; i < nodes.length; i++) {
				files[i] = new FileSet(nodes[i]);
			}
			list.setListData(files);
		}

		/**
		 *  Removes the currently selected files, if any
		 *
		 * @return    The index of the first file removed, or -1
		 */
		public int removeSelected() {
		  	
			int [] toRemove  = list.getSelectedIndices();
			if (toRemove == null) {
			  regenList();
			  return -1;
			}

			for (int i = (toRemove.length - 1); i >= 0; i--) {
			  int file = toRemove[i];
			  client.removeFile(file);
			}
			client.notifyObservers();

			regenList();
			list.setSelectedIndex(toRemove[0]);

			return toRemove[0];
		}
		
		/**
		*  removes all filsets.
		*/
		@Override
		public void removeAll() {
			
			int max = list.getModel().getSize();
		  	for (int i = (max - 1); i >= 0; i--) {
			  client.removeFile(i);
			}
			client.notifyObservers();

			regenList();
		}
		

		/**
		 *  Offers a search for the seleced file. Useful if you moved the file.
		 */
		public void searchSelected() {
		
			int i  = list.getSelectedIndex();
			searchFile(new FileSet(client.getConfig(i)), window);
			client.notifyFileSetModified();
			client.notifyObservers();
			regenList();
		}
	}


	/**
	 *  Offers a search for a file corresponding to a fileset. 
	 *  Useful if you moved the file.
	 *
	 * @param  fileSet  FileSet to find
	 * @param  w        Window to block
	 * @return          true if fileset was reassigned.
	 */
	public static boolean searchFile(FileSet fileSet, Window w) {
	
		JFileChooser fileDialog  = new JFileChooser();
		fileDialog.setFileFilter(new CdtFilter());

		String string = fileSet.getDir();
		if (string != null) {
			fileDialog.setCurrentDirectory(new File(string));
		}
		
		int retVal = fileDialog.showOpenDialog(w);
		if (retVal == JFileChooser.APPROVE_OPTION) {
			File chosen  = fileDialog.getSelectedFile();
			fileSet.setCdt(chosen.getName());
			fileSet.setDir(chosen.getParent() + File.separator);
			return true;
		}
		return false;
	}

	/**
	 *  Class to encapsulate buttons and callbacks for buttons
	 *
	 * @author     Alok Saldanha <alok@genome.stanford.edu>
	 */
	private class ButtonPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		
		private JButton openButton, searchButton, deleteButton, deleteAllButton, 
		closeButton;


		/**
		 *  Constructor for the ButtonPanel object
		 */
		private ButtonPanel() {
			
			this.setLayout(new MigLayout());
			this.setOpaque(false);
			
			openButton = setButtonLayout("Open");
			openButton.addActionListener(new ActionListener() {
					
				@Override
				public void actionPerformed(ActionEvent e) {
					
				}
			});
			//this.add(openButton); not sure about this...

			searchButton = setButtonLayout("Find");
			searchButton.addActionListener(new ActionListener() {
					
				@Override
				public void actionPerformed(ActionEvent e) {
					
					fileSetPanel.searchSelected();
				}
			});
			this.add(searchButton, "pushx");

			deleteButton = setButtonLayout("Remove");
			deleteButton.addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
					  fileSetPanel.removeSelected();
					}
				});
			this.add(deleteButton, "pushx");

			deleteAllButton = setButtonLayout("Remove All");
			deleteAllButton.addActionListener(new ActionListener() {
					
				@Override
				public void actionPerformed(ActionEvent e) {
					fileSetPanel.removeAll();
				}
			});
			this.add(deleteAllButton, "pushx");

			closeButton = setButtonLayout("Close");
			closeButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					window.dispose();
				}
			});
			this.add(closeButton, "pushx");
		}

		/**
		 *  This method is called to let the button panel know if anything is
		 *  selected. The button panel will (dis)enable buttons as required.
		 *
		 * @param  thingsSelected  The new thingsSelected value
		 */
		public void setThingsSelected(boolean thingsSelected) {
			
			deleteButton.setEnabled(thingsSelected);
			searchButton.setEnabled(thingsSelected);
		}
		
		/**
		 * Setting up a general layout for a button object
		 * The method is used to make all buttons appear consistent 
		 * in aesthetics.
		 * @param button
		 * @return
		 */
		public JButton setButtonLayout(String title){
			
			Font buttonFont = new Font("Sans Serif", Font.PLAIN, 14);
			
			JButton button = new JButton(title);
	  		Dimension d = button.getPreferredSize();
	  		d.setSize(d.getWidth()*1.5, d.getHeight()*1.5);
	  		button.setPreferredSize(d);
	  		
	  		button.setFont(buttonFont);
	  		button.setOpaque(true);
	  		button.setBackground(GUIParams.ELEMENT);
	  		button.setForeground(GUIParams.BG_COLOR);
	  		
	  		return button;
		}
	}


	/**
	 *  test code, loads an XmlConfig...
	 *
	 * @param  args  The command line arguments
	 */
	public final static void main(String[] args) {
	
		XmlConfig c = new XmlConfig(args[0], "TestConfig");
		FileMru fm = new FileMru();
		
		fm.bindConfig(c.getNode("FileMru"));
		FileMruEditor fme = new FileMruEditor(fm);
		fme.makeTop();
	}

}

