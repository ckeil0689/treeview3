/*
 * Created on Sep 21, 2006
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package jtvexample.separate;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;
import javax.swing.text.BadLocationException;

import edu.stanford.genetics.treeview.*;
import edu.stanford.genetics.treeview.app.LinkedViewApp;
import edu.stanford.genetics.treeview.TreeViewApp;

@SuppressWarnings("serial")
public class ControlPanel extends JPanel {
	public class FilePanel extends JPanel {
		JTextField textField = new JTextField(20);
		public FilePanel() {
			add(new JLabel("CDT:"));
			add(textField);
			JButton b_browse = new JButton("Browse...");
			b_browse.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					JFileChooser chooser = new JFileChooser();
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				    int returnVal = chooser.showOpenDialog(FilePanel.this);
				    if(returnVal == JFileChooser.APPROVE_OPTION) {
				    	ControlPanel.this.setFileName(chooser.getSelectedFile().getAbsolutePath());
				    }
				}
			});
			add(b_browse);
			JButton b_load = new JButton("Load");
			b_load.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					ControlPanel.this.loadCDT();
				}
			});
			add(b_load);
		}
		public String getFileName() {
			return textField.getText();
		}
	}
	public void setFileName(String text) {
    	filePanel.textField.setText(text);
    	filePanel.validate();
	}

	public class EventPanel extends JPanel implements Observer {
		JTextArea textArea= new JTextArea("", 20, 30);
		public EventPanel() {
			setLayout(new BorderLayout());
			add(new JLabel("Events from JTV"), BorderLayout.NORTH);
			add(new JScrollPane(textArea), BorderLayout.CENTER);
			JButton b_clear = new JButton("Clear");
			b_clear.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					textArea.setText("");
				}
			});
			add(b_clear, BorderLayout.SOUTH);
		}
		@Override
		public void update(Observable arg0, Object arg1) {
			textArea.append("Event " +arg0.toString() + "\n");
			TreeSelectionI treeSelection = (TreeSelectionI) arg0;
			textArea.append("Selected from " + treeSelection.getMinIndex() + 
					" to " +treeSelection.getMaxIndex() +"\n" );
			ViewFrame [] frames = treeViewApp.getWindows();
			for (int i =0; i < frames.length; i++) {
				if (treeSelection == frames[i].getGeneSelection()) {
					HeaderInfo info = frames[i].getDataModel().getGeneHeaderInfo();
					for (int j = treeSelection.getMinIndex(); j <= treeSelection.getMaxIndex(); j++) {
						if (j >= 0) {
							if (treeSelection.isIndexSelected(j)) {
								textArea.append(joinString(info.getHeader(j)) + "\n");
							}
						}
					}
				}
			}
		}
		private String joinString(String[] header) {
			String ret = header[0];
			for (int i = 1; i < header.length; i++)
				ret += ", " + header[i];
			return ret;
		}
	}	
	public class SelectPanel extends JPanel {
		JTextArea textArea= new JTextArea("", 20, 30);
		public SelectPanel() {
			setLayout(new BorderLayout());
			add(new JLabel("Send Genes to JTV"), BorderLayout.NORTH);
			add(new JScrollPane(textArea), BorderLayout.CENTER);
			JButton b_clear = new JButton("Send");
			b_clear.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					String [] ids = extractStrings();
					selectStrings(ids);
				}


			});
			add(b_clear, BorderLayout.SOUTH);
		}
		private void selectStrings(String[] ids) {
			ViewFrame [] frames = treeViewApp.getWindows();
			TreeSelectionI sel = frames[0].getGeneSelection();
			HeaderInfo info = frames[0].getDataModel().getGeneHeaderInfo();
			if (ids.length > 0) {
				sel.deselectAllIndexes();
				sel.setSelectedNode(null);
				for (int i = 0; i < ids.length; i++) {
					int index = info.getHeaderIndex(ids[i]);
					System.out.println(i+": " + ids[i] + " was " +index);
					if (index >= 0)
						sel.setIndex(index, true);
				}
				sel.notifyObservers();
			}
		}
		private String[] extractStrings() {
			String[] ret = new String[textArea.getLineCount()];
			for (int i = 0; i < ret.length; i++) {
				int start;
				try {
					start = textArea.getLineStartOffset(i);
					String cand = textArea.getText(start,
							textArea.getLineEndOffset(i)-start);
					cand = cand.trim();
					if (cand == "") {
						ret[i] = null;
					} else{
						ret[i] = cand;
					}
				} catch (BadLocationException e) {
					ret[i] = null;
					e.printStackTrace();
				}
			}
			return ret;
		} 

	}
	FilePanel filePanel = new FilePanel();
	EventPanel eventPanel = new EventPanel();
	SelectPanel selectPanel = new SelectPanel();
	TreeViewApp treeViewApp = new LinkedViewApp();
	/**
	 * This component has three major interconnected windows, 
	 * a panel for selecting a CDT file to load
	 * a panel that displays selection events
	 * a panel that allows the user to select a set of genes
	 * These are all defined later as inner classes
	 */
	public ControlPanel() {
		treeViewApp.setExitOnWindowsClosed(false);
		Box inner = new Box(BoxLayout.X_AXIS);
		inner.add(eventPanel);
		inner.add(selectPanel);

		setLayout(new BorderLayout());
		add(filePanel, BorderLayout.NORTH);
		add(inner, BorderLayout.CENTER);
	}
	
	
	/**
	 * Should call this with the name of a CDT file.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ControlPanel cp = new ControlPanel();
		if (args.length > 0) {
			cp.setFileName(args[0]);
			cp.loadCDT();
		}
		JFrame top = new JFrame();
		top.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		top.getContentPane().add(cp);
		top.pack();
		top.setVisible(true);
	}
	/**
	 * Everything above is GUI fluff, this is where the real action happens.
	 *
	 */
	private void loadCDT() {
		String sFilePath = filePanel.getFileName();
		FileSet fileSet;
		if (sFilePath.startsWith("http://")) {
			fileSet = new FileSet(sFilePath,"");
		} else {
			File file = new File(sFilePath);
			fileSet = new FileSet(file.getName(), file.getParent()+File.separator);
		}
		fileSet.setStyle("auto");
		
		try {
			ViewFrame viewframe = treeViewApp.openNewNW(fileSet);
			TreeSelectionI treeSelection = viewframe.getGeneSelection();
			treeSelection.addObserver(eventPanel);
			viewframe.setVisible(true);
		} catch (LoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
