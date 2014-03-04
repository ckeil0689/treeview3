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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

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
public abstract class HeaderFinderPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	protected TreeSelectionI geneSelection;
	protected ViewFrame viewFrame;

	private final HeaderInfo headerInfo;
	private final int choices[];
	private int nchoices = 0;
	
	private final ArrayList<String> geneList;
	private String[] genefHeaders = { "" };
	private final String type;
	private final WideComboBox genefBox;
	private final JButton genefButton;

	// "Search Gene Text for Substring"
	public HeaderFinderPanel(final ViewFrame f, final HeaderInfo hI,
			final TreeSelectionI geneSelection, final String type) {

		this(f.getAppFrame(), hI, geneSelection, type);
		this.viewFrame = f;
	}

	private HeaderFinderPanel(final JFrame f, final HeaderInfo hI,
			final TreeSelectionI geneSelection, final String type) {

		super();
		this.viewFrame = null;
		this.headerInfo = hI;
		this.geneSelection = geneSelection;
		this.type = type;
		this.choices = new int[hI.getNumHeaders()]; // could be wasteful of
													// ram...

		setLayout(new MigLayout());
		setOpaque(false);

		final String[][] hA = headerInfo.getHeaderArray();

		String genef = "Search " + type + " Labels... ";

		geneList = new ArrayList<String>();
		genefHeaders = getGenes(hA);
		
		String [] labeledHeaders = new String[genefHeaders.length + 1];
		
		labeledHeaders[0] = genef;
		
		Arrays.sort(genefHeaders);
		
		for(int i = 0; i < genefHeaders.length; i++) {
			
			labeledHeaders[i + 1] = genefHeaders[i];
		}
		
		for (final String gene : genefHeaders) {

			geneList.add(gene);
		}

		genefBox = GUIParams.setComboLayout(labeledHeaders);
		genefBox.setEditable(true);
		AutoCompleteDecorator.decorate(genefBox);
		
		genefButton = GUIParams.setButtonLayout(null, "searchIcon");
		genefButton.setToolTipText("Highlights the selected label.");
		genefButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {

				final String gene = genefBox.getSelectedItem().toString();
				findGenes(gene);
				seekAll();
			}
		});

		add(genefBox, "w 85%");
		add(genefButton, "w 15%, wrap");
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

		int geneIndex = 0;
		geneIndex = geneList.indexOf(genefBox.getSelectedItem());
		geneSelection.setIndex(geneIndex, true);

		geneSelection.notifyObservers();

		if ((viewFrame != null) && (selected.length > 0)) {
			scrollToIndex(choices[selected[0]]);
		}
	}

	private void findGenes(final String sub) {

		nchoices = 0;

		final int jmax = headerInfo.getNumHeaders();
		for (int j = 0; j < jmax; j++) {

			final String[] strings = headerInfo.getHeader(j);
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

	private void selectGene(final int j) {

		// String [] strings = headerInfo.getHeader(j);
		// String id = "";
		// for (int i = 1; i < strings.length; i++) {
		// if (strings[i] != null) {
		// id += strings[i] + "; ";
		// }
		// }
		//
		// if (strings[0] != null) {
		// id += strings[0] + "; ";
		// }

		choices[nchoices++] = j;
	}

	abstract public void scrollToIndex(int i);

}
