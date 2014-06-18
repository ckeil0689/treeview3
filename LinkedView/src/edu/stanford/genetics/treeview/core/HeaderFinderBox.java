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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.WideComboBox;

/**
 * This class allows users to look for row or column elements by choosing them
 * in a drop down menu. The menu is populated with headers from the loaded data
 * matrix. The class is abstract and a basis for the GeneFinderPanel class as
 * well as the ArrayFinderPanel class.
 * 
 * It extends JPanel and can be used as a Swing component.
 * 
 */
public abstract class HeaderFinderBox {

	protected TreeSelectionI geneSelection;
	protected ViewFrame viewFrame;

	private final HeaderInfo headerInfo;
//	private final int choices[];
//	private int nchoices = 0;

	private final ArrayList<String> geneList;
	private String[] genefHeaders = { "" };
	private final String type;
	private final WideComboBox genefBox;
	private final JButton genefButton;

	private final JPanel contentPanel;

	// "Search Gene Text for Substring"
	public HeaderFinderBox(final ViewFrame f, final HeaderInfo hI,
			final TreeSelectionI geneSelection, final String type) {

		this(f.getAppFrame(), hI, geneSelection, type);
		this.viewFrame = f;
	}

	private HeaderFinderBox(final JFrame f, final HeaderInfo hI,
			final TreeSelectionI geneSelection, final String type) {

		super();
		this.viewFrame = null;
		this.headerInfo = hI;
		this.geneSelection = geneSelection;
		this.type = type;
//		this.choices = new int[hI.getNumHeaders()]; // could be wasteful of
													// ram...

		contentPanel = new JPanel();
		contentPanel.setLayout(new MigLayout());
		contentPanel.setOpaque(false);

		final String[][] hA = headerInfo.getHeaderArray();

		final String genef = "Search " + type + " Labels... ";

		geneList = new ArrayList<String>();
		genefHeaders = getGenes(hA);

		for (final String gene : genefHeaders) {

			geneList.add(gene);
		}

		final String[] labeledHeaders = new String[genefHeaders.length + 1];

		labeledHeaders[0] = genef;

		Arrays.sort(genefHeaders);

		System.arraycopy(genefHeaders, 0, labeledHeaders, 1,
				genefHeaders.length);

		genefBox = GUIParams.setWideComboLayout(labeledHeaders);
		genefBox.setEditable(true);
//		AutoCompleteDecorator.decorate(genefBox);

		genefButton = GUIParams.setButtonLayout(null, "searchIcon");
		genefButton.setToolTipText("Highlights the selected label.");
		genefButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				
				seekAll();
			}
		});

		contentPanel.add(genefBox);
		contentPanel.add(genefButton);
	}

	/**
	 * Returns the content panel which keeps the GUI components.
	 * 
	 * @return JPanel
	 */
	public JPanel getContentPanel() {

		return contentPanel;
	}

	/**
	 * Extracts the header infos into a String array, so the array can fill the
	 * comboBox with values to choose from.
	 * 
	 * @param hA
	 * @return
	 */
	public String[] getGenes(final String[][] hA) {

		final String[] geneArray = new String[hA.length];
		int idIndex = 0;

		if (type.equalsIgnoreCase("Row")) {
			if (headerInfo.getIndex("ORF") != -1) {
				idIndex = headerInfo.getIndex("ORF");
			}
		} else {
			if (headerInfo.getIndex("GID") != -1) {
				idIndex = headerInfo.getIndex("GID");

			} else {
				if (headerInfo.getIndex("ORF") != -1) {
					idIndex = headerInfo.getIndex("ORF");
				}
			}
		}

		for (int i = 0; i < hA.length; i++) {

			final String yorf = hA[i][idIndex];
			geneArray[i] = yorf;
		}

		return geneArray;
	}

	public void seekAll() {
		
		final int[] selected = { 0 };
		geneSelection.setSelectedNode(null);
		geneSelection.deselectAllIndexes();

		ArrayList<Integer> indexList = findSelected();
		
		for(int i = 0; i < indexList.size(); i++) {
			
			geneSelection.setIndexSelection(indexList.get(i), true);
		}

		geneSelection.notifyObservers();

		if ((viewFrame != null) && (selected.length > 0)) {
			scrollToIndex(geneList.indexOf(genefBox.getSelectedItem()));
		}
	}
	
	private ArrayList<Integer> findSelected() {
		
		ArrayList<Integer> indexList = new ArrayList<Integer>();
		
		String sub = genefBox.getSelectedItem().toString();
		
		for(String gene : geneList) {
			
			if(wildCardMatch(gene, sub)) {
				indexList.add(geneList.indexOf(gene));
			}
		}
		
		return indexList;
	}
	
	/**
     * Performs a wildcard matching for the text and pattern 
     * provided. Matching is done based on regex patterns.
     * 
     * @param text the text to be tested for matches.
     * 
     * @param pattern the pattern to be matched for.
     * This can contain the wildcard character '*' (asterisk).
     * 
     * @return <tt>true</tt> if a match is found, <tt>false</tt> 
     * otherwise.
     */
    public static boolean wildCardMatch(String text, String pattern) {
        
    	if(text == null || pattern == null) {
    		return false;
    	}
    	
    	// Define CharSequences to replaced in given String.
    	CharSequence singleChar = "?";
    	CharSequence regexSingleChar = ".";
    	
    	CharSequence multiChar = "*";
    	CharSequence regexMultiChar = "(.)*";
    	
    	// Transform String to Regex
    	pattern = pattern.replace(singleChar, regexSingleChar);
    	pattern = pattern.replace(multiChar, regexMultiChar);
        
    	// Check if generated regex matches, store result in boolean.
        boolean isMatch = false;
        if(text.matches(pattern)) {
        	isMatch = true;
        }
        
        return isMatch;
    }

	abstract public void scrollToIndex(int i);
	
	// Testing WildCard search
	public static void main(String[] args) {
		
		JDialog dialog = new JDialog();
		dialog.setTitle("WildCard Search Test");
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setSize(new Dimension(400, 150));
		
		JPanel container = new JPanel();
		container.setLayout(new MigLayout());
		
		dialog.getContentPane().add(container);
		
		final JTextField tf1 = new JTextField();
		tf1.setEditable(true);
		
		final JTextField tf2 = new JTextField();
		tf2.setEditable(true);
		
		final JLabel matchStatus = new JLabel("WildCard Match:");
		
		JButton matchStrings = new JButton("Check match");
		matchStrings.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				boolean match = wildCardMatch(tf1.getText(), tf2.getText());
				matchStatus.setText("WildCard Match: " + match);
			}
		});
		
		container.add(tf1, "growx, span, wrap");
		container.add(tf2, "growx, span, wrap");
		container.add(matchStrings, "span, pushx, alignx 50%, wrap");
		container.add(matchStatus, "span, pushx, alignx 50%");
		
		dialog.setVisible(true);
	}

}
