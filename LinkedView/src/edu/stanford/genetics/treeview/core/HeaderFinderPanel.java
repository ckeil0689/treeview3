/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: HeaderFinder.java,v $
 * $Revision: 1.1 $
 * $Date: 2009-08-26 11:48:27 $
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

// for summary view...
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;

import edu.stanford.genetics.treeview.GUIColors;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.core.HeaderFinder.ResultsPanel.ListSeeker;
/**
 *  The purpose of this class is to allow searching on HeaderInfo objects.
 * The display of the headers and the matching is handled by this class,
 * whereas the actual manipulation of the selection objects and the 
 * associated views is handled by the relevant subclass.
 * 
 * @author aloksaldanha
 *
 */
public abstract class HeaderFinderPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private static Font fontS = new Font("Sans Serif", Font.PLAIN, 18);
	
	protected TreeSelectionI geneSelection;
	protected ViewFrame viewFrame;
	
//    private JButton search_button, seek_button, seekNext_button, seekAll_button, 
//    summary_button;
//    private ResultsPanel rpanel;
    private HeaderInfo headerInfo;
    private int choices[];
    private int nchoices = 0;
    private JList results;
	private DefaultListModel resultsModel;
	
	private JLabel genef;
	private ArrayList<String> geneList;
	private String[] genefHeaders = {""};
	private JComboBox genefBox;
	private JButton genefButton;
	
	//"Search Gene Text for Substring"
	public HeaderFinderPanel(ViewFrame f, HeaderInfo hI, 
			TreeSelectionI geneSelection, String type) {
		
		this((Frame) f, hI, geneSelection, type);
		this.viewFrame = f;
	}
	
	private HeaderFinderPanel(Frame f, HeaderInfo hI, 
			TreeSelectionI geneSelection, String type) {
	
		super();
		this.viewFrame = null;
		this.headerInfo = hI;
		this.geneSelection = geneSelection;
		choices = new int[hI.getNumHeaders()]; // could be wasteful of ram...
	
//		JPanel mainPanel = new JPanel();
//		mainPanel.setLayout(new BorderLayout());
//		mainPanel.add(new SearchPanel(), BorderLayout.NORTH);
//		
//		rpanel = new ResultsPanel();
//		mainPanel.add(new JScrollPane(rpanel), BorderLayout.CENTER);
//		
//		mainPanel.add(new ClosePanel(), BorderLayout.SOUTH);
//	
//		mainPanel.add(new SeekPanel() , BorderLayout.EAST);
//		getContentPane().add(mainPanel);
//		addWindowListener(new WindowAdapter () {
//			@Override
//			public void windowClosing(WindowEvent we) {
//			    setVisible(false);
//			}
//		    });
//		pack();
		this.setLayout(new MigLayout());
		this.setOpaque(true);
		
		String[][] hA = headerInfo.getHeaderArray();
		
		genef = new JLabel("Find " + type + " Element: ");
		genef.setFont(fontS);
		
		geneList = new ArrayList<String>();
		genefHeaders = getGenes(hA);
		
		for(String gene : genefHeaders) {
			
			geneList.add(gene);
		}
		
		Arrays.sort(genefHeaders);
		genefBox = setComboLayout(genefHeaders);
		genefButton = setButtonLayout("Go!");
		genefButton.addActionListener(new ActionListener(){
			
		    @Override
			public void actionPerformed(ActionEvent e) {
			
		    	String gene = genefBox.getSelectedItem().toString();
		    	findGenes(gene);
		    	seekAll();
		    	//showSubDataModel();
		    }
		});
		
		this.add(genef, "span, wrap, pushx");
		this.add(genefBox, "growx, push, growx");
		this.add(genefButton);
    }
	
	/**
	 * Extracts the header infos into a String array, so the array can
	 * fill the comboBox with values to choose from.
	 * @param hA
	 * @return
	 */
	public String[] getGenes(String[][] hA) {
		
		String[] geneArray = new String[headerInfo.getNumHeaders()];
		int idIndex = headerInfo.getIndex("ORF");
		
		for(int i = 0; i < hA.length; i++) {
			
			String yorf = hA[i][idIndex];
			geneArray[i] = yorf;
		}
		
		return geneArray;
	}
	
	/**
	 * Setting up a general layout for a button object
	 * The method is used to make all buttons appear consistent in aesthetics
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
  		button.setBackground(GUIColors.ELEMENT);
  		button.setForeground(GUIColors.BG_COLOR);
  		
  		return button;
	}
	
	/**
	 * Setting up a general layout for a ComboBox object
	 * The method is used to make all ComboBoxes appear consistent in aesthetics
	 * @param combo
	 * @return
	 */
	public JComboBox setComboLayout(String[] combos){
		
		JComboBox comboBox = new JComboBox(combos);
		Dimension d = comboBox.getPreferredSize();
		d.setSize(d.getWidth() * 1.5, d.getHeight() * 1.5);
		comboBox.setPreferredSize(d);
		comboBox.setFont(fontS);
		comboBox.setBackground(Color.white);
		
		return comboBox;
	}
    
//	/**
//	* selects all the genes which are currently selected in the results panel.
//	*/
//	private void seek() {
//		
//		int first = rpanel.getFirstSelectedIndex(); 
//		// in some jdks, selected index is set to -1 between selections.
//		if (first == -1) {
//			return;
//		}
//		
//		int [] selected = results.getSelectedIndices();
//		if (selected.length == 0) {
//			return;
//		}
//
//		geneSelection.deselectAllIndexes();
//		for (int i = 0; i < selected.length; i++) {
//			geneSelection.setIndex(choices[selected[i]], true);
//		}
//		geneSelection.notifyObservers();
//		scrollToIndex(choices[first]);
//	}
	
	/* 		if (viewFrame != null)
	viewFrame.scrollToGene(choices[first]);
	*/
	
//    private void seekNext() {
//    	
//		int currentIndex = rpanel.getFirstSelectedIndex();
//		if (currentIndex == -1) {
//			return; // no current selection.
//		}
//		
//		int nextIndex = (currentIndex + 1) % resultsModel.getSize();
//		rpanel.setSelectedIndex(nextIndex);
//		results.ensureIndexIsVisible(nextIndex);
//		seek();
//    }
//    
	public void seekAll() {
		
		//results.setSelectionInterval(0, resultsModel.getSize() - 1);
		int [] selected = {0};//results.getSelectedIndices();
		geneSelection.setSelectedNode(null);
		geneSelection.deselectAllIndexes();
		
		//for (int i = 0; i < selected.length; i++) {
		int geneIndex = 0;
		geneIndex = geneList.indexOf((String)genefBox.getSelectedItem());
		geneSelection.setIndex(geneIndex, true);//choices[selected[i]], true);
		//}
		
		geneSelection.notifyObservers();
		//results.repaint();
		
		if ((viewFrame != null) && (selected.length > 0)) {
			scrollToIndex(choices[selected[0]]);
				
		}
	}
	
	/**
	* selects all genes which match the specified id in their id column...
	*/
	public void findGenesById(String [] subs) {
		
		nchoices = 0;
		//resultsModel.removeAllElements();
		
		int jmax  = headerInfo.getNumHeaders();
		int idIndex = headerInfo.getIndex("YORF"); 
		
		//actually, just 0, or 1 if 0 is GID.
		for  (int j = 0; j < jmax; j++) {
			String [] headers = headerInfo.getHeader(j);
			if (headers == null) {
				continue;
			}
			
			String id = headers[idIndex];
			if (id == null) {
				continue;
			}
			
			boolean match = false;
			for (int i=0; i < subs.length; i++) {
				if (subs[i] == null) {
					System.out.println("eek! HeaderFinder substring " + i 
							+ " was null!");
				}
				
				if (id.indexOf(subs[i]) >= 0) {
					match = true;
					break;
				}
			}
			
			if (match) {
				selectGene(j);
			}
		}
	}
	
    private void findGenes(String sub) {
		
    	nchoices = 0;
		//resultsModel.removeAllElements();
		
		int jmax = headerInfo.getNumHeaders();
		for (int j = 0; j < jmax; j++) {
			
			String[] strings = headerInfo.getHeader(j);
			if (strings == null) {
				continue;
			}
			
			boolean match = false;
			for (int i = 0; i < strings.length; i++) {
				if (strings[i] == null) {
					continue;
				}
				
				String cand;
				cand = strings[i].toUpperCase();
				
				if (cand.indexOf(sub) >= 0) {
					match = true;
					break;
				}
			}
			
			if (match) {
				selectGene(j);
			}
		}
	}

	private void selectGene(int j) {
		
		String [] strings = headerInfo.getHeader(j);
		String id = "";
		for (int i = 1; i < strings.length; i++) {		    
			if (strings[i] != null) {
				id += strings[i] + "; ";
			}
		}
		
		if (strings[0] != null) {
			id += strings[0] + "; ";
		}
		
		//resultsModel.addElement(id);
		choices[nchoices++] = j;
	}
  
	abstract public void scrollToIndex(int i);
    
//	class ResultsPanel extends JPanel {
//
//		private static final long serialVersionUID = 1L;
//		
//		public ResultsPanel() {
//
////			setLayout(new BorderLayout());
//			resultsModel = new DefaultListModel();
//			results = new JList(resultsModel);
//			results.setVisibleRowCount(10);
//			results.addListSelectionListener(new ListSeeker());
////			add(results, BorderLayout.CENTER);
//			add(results);
//		}
//		
//		class ListSeeker implements ListSelectionListener {
//			@Override
//			public void valueChanged(ListSelectionEvent e) {
//				results.repaint();
//				seek();
//			}
//		}
//		
//		public int getFirstSelectedIndex() {return results.getSelectedIndex();}
//		public int [] getSelectedIndices() {return results.getSelectedIndices();}
//		public void setSelectedIndex(int i) {results.setSelectedIndex(i);}
//	}

//    class SeekPanel extends JPanel {
//
//		private static final long serialVersionUID = 1L;
//
//		public SeekPanel () {
//			
//			super();
//			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); 
//		    search_button = new JButton("Search");
//		    search_button.addActionListener(search_text);
//		    add(search_button);
//	
//	 	    seek_button = new JButton("Seek");
//		    seek_button.addActionListener(new ActionListener() {
//			    
//		    	@Override
//				public void actionPerformed(ActionEvent evt) {
//				
//		    		seek();
//			    }
//			});
//	
//		    // add(seek_button);
//	 	    seekNext_button = new JButton("Next");
//		    seekNext_button.addActionListener(new ActionListener() {
//			    
//		    	@Override
//				public void actionPerformed(ActionEvent evt) {
//				
//		    		seekNext();
//			    }
//			});
//		    add(seekNext_button);
//	
//	 	    seekAll_button = new JButton("All");
//		    seekAll_button.addActionListener(new ActionListener() {
//			    
//		    	@Override
//				public void actionPerformed(ActionEvent evt) {
//				
//		    		seekAll();
//			    }
//			});
//		    add(seekAll_button);
//			
//			summary_button = new JButton("Summary Popup");
//			summary_button.addActionListener(new ActionListener() {
//				
//				@Override
//				public void actionPerformed(ActionEvent evt) {
//						
//					showSubDataModel();
//				}
//	
//			});
//			add(summary_button);
//	
//			add(Box.createVerticalGlue());
//		}
//    }

//    class ClosePanel extends JPanel {
//
//		private static final long serialVersionUID = 1L;
//
//		public ClosePanel () {
//	 	    
//			JButton close_button = new JButton("Close");
//		    close_button.addActionListener(new ActionListener() {
//			   
//		    	@Override
//				public void actionPerformed(ActionEvent e) {
//		    		
//		    		HeaderFinderPanel.this.setVisible(false);
//			    }
//			});
//		    add(close_button);
//		}
//    }
        
//	protected abstract void showSubDataModel();
}
