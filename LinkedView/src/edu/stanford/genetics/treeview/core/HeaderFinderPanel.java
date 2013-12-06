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

import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.ViewFrame;

/**
 * This class allows users to look for row or column elements by choosing
 * them in a drop down menu. The menu is populated with headers from the 
 * loaded data matrix. The class is abstract and a basis for the GeneFinderPanel
 * class as well as the ArrayFinderPanel class. 
 * 
 * It extends JPanel and can be used as a Swing component.
 *
 */
public abstract class HeaderFinderPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private static Font fontS = new Font("Sans Serif", Font.PLAIN, 18);
	
	protected TreeSelectionI geneSelection;
	protected ViewFrame viewFrame;
	
    private HeaderInfo headerInfo;
    private int choices[];
    private int nchoices = 0;
	
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
	
		this.setLayout(new MigLayout());
		this.setOpaque(false);
		
		String[][] hA = headerInfo.getHeaderArray();
		
		genef = new JLabel("Find " + type + " Element: ");
		genef.setForeground(GUIParams.TEXT);
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
		    }
		});
		
		this.add(genef, "span, pushx, wrap");
		this.add(genefBox, "growx, pushx, width 70%");
		this.add(genefButton, "pushx, width 20%");
    }
	
	/**
	 * Extracts the header infos into a String array, so the array can
	 * fill the comboBox with values to choose from.
	 * @param hA
	 * @return
	 */
	public String[] getGenes(String[][] hA) {
		
		String[] geneArray = new String[headerInfo.getNumHeaders()];
		String[] names = headerInfo.getNames();
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
  		button.setBackground(GUIParams.ELEMENT);
  		button.setForeground(GUIParams.BG_COLOR);
  		
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
	
	public void seekAll() {
		
		int [] selected = {0};
		geneSelection.setSelectedNode(null);
		geneSelection.deselectAllIndexes();
		
		int geneIndex = 0;
		geneIndex = geneList.indexOf((String)genefBox.getSelectedItem());
		geneSelection.setIndex(geneIndex, true);
		
		geneSelection.notifyObservers();
		
		if ((viewFrame != null) && (selected.length > 0)) {
			scrollToIndex(choices[selected[0]]);		
		}
	}
	
    private void findGenes(String sub) {
		
    	nchoices = 0;
		
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
		
//		String [] strings = headerInfo.getHeader(j);
//		String id = "";
//		for (int i = 1; i < strings.length; i++) {		    
//			if (strings[i] != null) {
//				id += strings[i] + "; ";
//			}
//		}
//		
//		if (strings[0] != null) {
//			id += strings[0] + "; ";
//		}
		
		choices[nchoices++] = j;
	}
  
	abstract public void scrollToIndex(int i);

}
