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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import Utilities.GUIFactory;
import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.WideComboBox;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;

/**
 * This class allows users to look for row or column elements by choosing them
 * in a drop down menu. The menu is populated with headers from the loaded data
 * matrix. The class is abstract and a basis for the defaultTextinderPanel class as
 * well as the ArrayFinderPanel class.
 * 
 * It extends JPanel and can be used as a Swing component.
 * 
 */
public abstract class HeaderFinderBox {

	protected TreeSelectionI searchSelection;
	protected ViewFrame viewFrame;

	private final HeaderInfo headerInfo;
	private final HeaderSummary headerSummary;

	private final List<String> searchDataList;
	private String[] searchDataHeaders = { "" };
	private final WideComboBox searchTermBox;
	private final JButton searchButton;

	private final JPanel contentPanel;
	
	//These are in order to determine whether a search result is currently visible and if so, to zoom out
	protected TreeSelectionI otherSelection;
	private MapContainer globalSmap;
	private MapContainer globalOmap;
	private final HeaderInfo otherHeaderInfo;
	private final List<String> otherDataList;
	private String[] otherDataHeaders = { "" };
	//private GlobalView2 globalview;

	// "Search for Substring"
	public HeaderFinderBox(final ViewFrame f, final HeaderInfo hI, 
			final HeaderSummary headerSummary, final TreeSelectionI 
			searchSelection, final String type,
			final MapContainer globalSmap, final MapContainer globalOmap, final TreeSelectionI otherSelection, final HeaderInfo ohI) {

		this(f.getAppFrame(), hI, headerSummary, searchSelection, otherSelection, type, ohI);
		this.viewFrame = f;
		
		//Hopefully this isn't too late of a place to set this, otherwise, I'll have to pass it along instead of set it here
		this.globalSmap = globalSmap;
		this.globalOmap = globalOmap;
		//this.globalview = globalview;
	}

	private HeaderFinderBox(final JFrame f, final HeaderInfo hI, 
			final HeaderSummary headerSummary,
			final TreeSelectionI searchSelection, final TreeSelectionI otherSelection, final String type, final HeaderInfo ohI) {

		super();
		this.viewFrame       = null;
		this.headerInfo      = hI;
		this.headerSummary   = headerSummary;
		this.searchSelection = searchSelection;
		this.otherSelection  = otherSelection;
		this.otherHeaderInfo = ohI;

		contentPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);

		final String[][] hA = headerInfo.getHeaderArray();

		final String defaultText = "Search " + type + " Labels... ";

		searchDataList = new ArrayList<String>();
		searchDataHeaders = getHeaders(hA);

		for (final String gene : searchDataHeaders) {

			searchDataList.add(gene);
		}

		final String[] labeledHeaders = new String[searchDataHeaders.length + 1];

		labeledHeaders[0] = defaultText;

		Arrays.sort(searchDataHeaders);

		//Going to keep track of the other dimension's headers so that we can determine if the search results are currently visible (at current zoom level)
		final String[][] ohA = otherHeaderInfo.getHeaderArray();
		otherDataList = new ArrayList<String>();
		otherDataHeaders = getHeaders(ohA);
		for (final String item : otherDataHeaders) {

			otherDataList.add(item);
		}

		System.arraycopy(searchDataHeaders, 0, labeledHeaders, 1,
				searchDataHeaders.length);

		searchTermBox = GUIFactory.createWideComboBox(labeledHeaders);
		searchTermBox.setEditable(true);
		AutoCompleteDecorator.decorate(searchTermBox);
		
		searchTermBox.getEditor().getEditorComponent().addKeyListener(
				new BoxKeyListener());

		
		searchButton = GUIFactory.createNavBtn("searchIcon");
		searchButton.setToolTipText("Highlights the selected label.");
		searchButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				
				seekAll();
			}
		});

		contentPanel.add(searchTermBox);
		contentPanel.add(searchButton);
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
	public String[] getHeaders(final String[][] hA) {

		final String[] headerArray = new String[hA.length];
		int idIndex = headerSummary.getIncluded()[0];

		for (int i = 0; i < hA.length; i++) {

			final String yorf = hA[i][idIndex];
			headerArray[i] = yorf;
		}

		return headerArray;
	}

	public void seekAll() {
		
		searchSelection.setSelectedNode(null);
		searchSelection.deselectAllIndexes();

		List<Integer> indexList = findSelected();
		
		//Initialize the min and max index used to determine whether result is currently visible
		int minIndex = 0;
		int maxIndex = 0;
		if(indexList.size() > 0) {
			minIndex = indexList.get(0);
			maxIndex = indexList.get(0);
		}
		
		//Set the found indexes as selected and determine min/max selected indexes
		for(int i = 0; i < indexList.size(); i++) {
			
			if(indexList.get(i) < minIndex) {
				minIndex = indexList.get(i);
			}
			if(indexList.get(i) > maxIndex) {
				maxIndex = indexList.get(i);
			}
			searchSelection.setIndexSelection(indexList.get(i), true);
		}

		searchSelection.notifyObservers();
		
		//Determine pre-selected min/max from the other dimension to see if they are visible
		List<Integer> otherIndexList = getOtherSelected();
		int otherMinIndex = 0;
		int otherMaxIndex = 0;
		if(indexList.size() > 0) {
			otherMinIndex = otherIndexList.get(0);
			otherMaxIndex = otherIndexList.get(0);
		}
		for(int i = 0; i < otherIndexList.size(); i++) {
			
			if(otherIndexList.get(i) < otherMinIndex) {
				otherMinIndex = otherIndexList.get(i);
			}
			if(otherIndexList.get(i) > otherMaxIndex) {
				otherMaxIndex = otherIndexList.get(i);
			}
		}

		if((viewFrame != null) && (indexList.size() > 0) &&
				//At least part of the found min/max selected area is not visible
				//This assumes that min is less than max and that the visible area is a contiguous block of visible indexes
				(minIndex < globalSmap.getFirstVisible() ||
				 maxIndex > (globalSmap.getFirstVisible() + globalSmap.getNumVisible() - 1))) {
			
			//LogBuffer.println("The search result is outside the visible area.");
			//LogBuffer.println("The search result is outside the visible area: [" + minIndex + " < " + globalSmap.getFirstVisible() + "] || [" + maxIndex + " > (" + globalSmap.getFirstVisible() + " + " + globalSmap.getNumVisible() + " - 1)].");
			globalSmap.setHome();
//Commented this out because it wasn't doing anything anyway
//			scrollToIndex(indexList.get(0));
		}

		if((viewFrame != null) && (otherIndexList.size() == 0 ||
				 otherMinIndex < globalOmap.getFirstVisible() ||
				 otherMaxIndex > (globalOmap.getFirstVisible() + globalOmap.getNumVisible() - 1))) {
			
			//LogBuffer.println("Search result: [" + minIndex + " < " + globalSmap.getFirstVisible() + "] || [" + maxIndex + " > (" + globalSmap.getFirstVisible() + " + " + globalSmap.getNumVisible() + " - 1)].");
			//LogBuffer.println("A whole row is being returned or the already-selected data is outside of the visible area: [" + otherIndexList.size() + " == 0] || [" + otherMinIndex + " < " + globalOmap.getFirstVisible() + "] || [" + otherMaxIndex + " > (" + globalOmap.getFirstVisible() + " + " + globalOmap.getNumVisible() + " - 1)].");
			globalOmap.setHome();
		}
	}
	
	private List<Integer> findSelected() {
		
		List<Integer> indexList = new ArrayList<Integer>();
		
		String sub = searchTermBox.getSelectedItem().toString();
		
		for(String header : searchDataList) {
			
			if(wildCardMatch(header, sub)) {
				indexList.add(searchDataList.indexOf(header));
			}
		}
		
		return indexList;
	}
	
	private List<Integer> getOtherSelected() {
		
		List<Integer> indexList = new ArrayList<Integer>();
		
		for(String item : otherDataList) {
			
			if(otherSelection.isIndexSelected(otherDataList.indexOf(item))) {
				indexList.add(otherDataList.indexOf(item));
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

    	//Escape all metacharacters except our supported wildcards
    	pattern = pattern.replaceAll("([^A-Za-z0-9 \\?\\*])", "\\\\$1");
    	//Convert our wildcards to regular expression syntax
    	pattern = pattern.replaceAll("\\?", ".");
    	pattern = pattern.replaceAll("\\*", ".*");
		//LogBuffer.println("Searching for [" + pattern + "]");

		// Check if generated regex matches, store result in boolean.
        boolean isMatch = false;
        if(text.matches(pattern)) {
        	isMatch = true;
        }
        
        return isMatch;
    }

	abstract public void scrollToIndex(int i);
	
	/**
	 * KeyListener to implement search by pressing enter when the 
	 * combobox has focus.
	 * @author CKeil
	 *
	 */
	class BoxKeyListener extends KeyAdapter {

		private boolean recordSelectedIndex = false;
		private boolean textSelected = false;
		private List<Integer> selectedIndexStack = new ArrayList<Integer>();
		private int curNumChars = 0;
		private int selStart = 0;
		private int selEnd = 0;
		private int len = 0;
		private boolean debug = false;

		//NOTE: command-w never worked to close the search window before adding anything in this class other than the seekAll function, though command-q WOULD quit the app

		@Override
		public void keyPressed(KeyEvent e) {
			//This gives us access to the text box contents and highlight control
			JTextComponent editor = ((JTextField) searchTermBox.getEditor().getEditorComponent());
			selStart = editor.getSelectionStart();
			selEnd   = editor.getSelectionEnd() - 1;
			len      = editor.getText().length();
			if(selEnd < selStart) selEnd = selStart;
			//When text is selected from right to left, the values are off - couldn't figure out a way to detect this!  It will cause the deleted text to be expanded 1 character on each side
			if(debug) LogBuffer.println("Pressed - Selection start: [" + selStart + "] Selection end: [" + selEnd + "] curNumChars: [" + curNumChars + "] String length: [" + len + "].  Selected text: [" + editor.getSelectedText() + "].  Full text: [" + editor.getText() + "]");

			//The only purpose of the following code is to keep the popup menu hidden when the user is typing in a wildcard string that is not a perfect match to anything
			//It tries to not get in the way though when certain other meaningful characters are typed
			if(	//There are characters in the term field and
				//the popup is not visible because there are more characters than there are selections in the stack
				(curNumChars > 0 && curNumChars > selectedIndexStack.size()) ||
					//The typed character has not been entered into the term field
					((e.getKeyChar() < ' ' || e.getKeyChar() > '~') &&
							//The typed key is not an up arrow, not a down arrow, and is not the enter key
							e.getKeyCode() != KeyEvent.VK_DOWN && e.getKeyCode() != KeyEvent.VK_UP && e.getKeyCode() != KeyEvent.VK_ENTER && e.getKeyCode() != KeyEvent.VK_SHIFT)) {

				searchTermBox.setPopupVisible(false);
			}
		}

		//The delete key is selecting what one tries to delete, thus if
		//you search for 'GENE105' and then decide you want 'GENE10',
		//hitting delete does not result in the default selection of
		//anything other than 'GENE105'.  The following is an attempt
		//to make it work better...
		@Override
		public void keyReleased(KeyEvent e) {
			//This gives us access to the text box contents and highlight control
			JTextComponent editor = ((JTextField) searchTermBox.getEditor().getEditorComponent());
			//Handle the case where we added a null character to allow the user to delete the end of a matched string
			if(editor.getText().endsWith("\0")) {
				if(debug) LogBuffer.println("Removing null character.");
				editor.setText(editor.getText().trim());
				selStart--;
				selEnd = editor.getText().length();
				editor.setSelectionStart(selStart);
				editor.setSelectionEnd(selEnd);
				if(!searchTermBox.isPopupVisible()) searchTermBox.setPopupVisible(true);
			}

			//When text is selected from right to left, the values are off - couldn't figure out a way to detect this!  It will cause the deleted text to be expanded 1 character on each side
			if(debug) LogBuffer.println("Released - Selection start: [" + selStart + "] Selection end: [" + selEnd + "] curNumChars: [" + curNumChars + "] String length: [" + len + "].  Selected text: [" + editor.getSelectedText() + "].  Full text: [" + editor.getText() + "]");
			if(debug) LogBuffer.println("Released - the selected index is: [" + searchTermBox.getSelectedIndex() + "]  Selected index stack size: [" + selectedIndexStack.size() + "].");
			
			//We determine whether we're going to record the currently selected index in
			//the stack based on whether there was a valid character entered into the term box
			if(recordSelectedIndex == true) {
				if(debug) LogBuffer.println("Recording selected index.");
				int selectedIndex = searchTermBox.getSelectedIndex();
				//If the user has manually changed the highlight to be at a position smaller than the stack size, pop off the difference
				if(selStart < selectedIndexStack.size()) {
					int diff = selectedIndexStack.size() - selStart;
					selectedIndexStack.subList(selectedIndexStack.size() - diff, selectedIndexStack.size()).clear();
				}
				if(selectedIndex >= 0) {
					if(debug) LogBuffer.println("Adding selected index [" + selectedIndex + "] to stack");
					selectedIndexStack.add(selectedIndex);
					selStart = editor.getSelectionStart();
					if(selStart == selEnd) {
						selEnd   = editor.getSelectionEnd();
						len      = editor.getText().length();
					}
					if(!searchTermBox.isPopupVisible()) searchTermBox.setPopupVisible(true);
				}
				//If there is no search result, eliminate the contextual menu
				else {
					if(searchTermBox.isPopupVisible()) searchTermBox.setPopupVisible(false);

					if(debug) LogBuffer.println("There was no selected index.");
					//Whether there was text selected or not, we're either keeping no text selected,
					//or we're not selecting more text after its deletion from typing a character
					selStart++;
					selEnd = selStart;
					editor.setSelectionStart(selStart);
					editor.setSelectionEnd(selStart);
				}
				curNumChars = selStart;
				if(debug) LogBuffer.println("Released[after recording index] - Selection start: [" + selStart + "] Selection end: [" + selEnd + "] curNumChars: [" + curNumChars + "] String length: [" + len + "].  Selected text: [" + editor.getSelectedText() + "].  Full text: [" + editor.getText() + "]");
				recordSelectedIndex = false;
			}
			else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
				//For some reason, between keyPressed and here, when selected text is deleted, the selection positions get changed, so I'm resetting them to what was saved in keyPressed
				editor.setSelectionStart(selStart);
				editor.setSelectionEnd(selEnd);

				//If there are characters in the text box
				if(curNumChars > 0) {
					//If the selection start is at the current number of typed characters (and a single character was deleted or highlighted)
					if(selStart == curNumChars && len <= (editor.getText().length() + 1)) {
						//If the number of characters is equal to the number of previously selected indexes in the popup menu (meaning there are no non-matching characters)
						if(curNumChars == selectedIndexStack.size()) {
							//Pop the last thing off the selected index stack (each index is for an item in the select list)
							if(selectedIndexStack.size() > 0) {
								if(debug) LogBuffer.println("Popping off the selected index stack");
								selectedIndexStack.subList(selectedIndexStack.size() - 1, selectedIndexStack.size()).clear();
							}
						}
						if(selStart == selEnd) {
							selEnd--;
							editor.setSelectionEnd(selEnd);
						}
						selStart--;
						editor.setSelectionStart(selStart);
						editor.setSelectionEnd(selEnd + 1);
						curNumChars--;
						if(debug) LogBuffer.println("Released - Setting new Selection start: [" + selStart + "] Selection end: [" + selEnd + "] curNumChars: [" + curNumChars + "].");
					}
					//Else - we're no longer doing the stack, so empty it out
					else {
						if(debug) LogBuffer.println("RELEASED - selStart [" + selStart + "] differs from curNumChars: [" + curNumChars + "].  Setting selEnd: [" + selEnd + "] and clearing stack.");
						selectedIndexStack.clear();
						editor.setSelectionEnd(selEnd);
					}
				}

				//If all the text in the text field is selected
				if(selStart == 0 && (selEnd + 1) == len) {
					if(searchTermBox.isPopupVisible()) searchTermBox.setPopupVisible(false);
					searchTermBox.setSelectedIndex(0);
					curNumChars = 0;
					selectedIndexStack.clear();
					return;
				}
				//Else if the selection does not go to the end or the selection start is not the number of typed characters -
				//implying the user wants to delete that specific text
				else if(selStart != selEnd &&
						//When prev typed char was a backspace, the last char ends up not selected and the beginning of the selection has not backed up at the time of this check, thus check that selection end is not equal to length and not equal to length - 1 and that the start is not equal to curnumchars and not equalt to that plus 1
						((selEnd + 1) < editor.getText().length() || (selStart != curNumChars && selStart != (curNumChars + 1)))) {
					if(debug) LogBuffer.println("Manual cursor manipulation detected with selection.  Selection start: [" + selStart +
							"] Selection end: [" + selEnd + "] Current Selection end: [" + editor.getText().length() + "] curNumChars: [" + curNumChars + "] String length: [" + len + "].");
					if(debug && searchTermBox.getSelectedIndex() > -1) LogBuffer.println("Old String: [" + editor.getText() + "].  New string: [" +
							editor.getText().substring(0, selStart) + editor.getText().substring((selEnd + 1), editor.getText().length()) + "].");
					//Clear out the stack and set the curnumchars to the cursor position
					selectedIndexStack.clear();
					curNumChars = selStart;
					if(searchTermBox.getSelectedIndex() > -1 && len == editor.getText().length()) {
						if((selEnd + 1) != len && selStart != 0) {
							if(debug) LogBuffer.println("Edited case 1.");
							editor.setText(editor.getText().substring(0, selStart) + editor.getText().substring((selEnd + 1), editor.getText().length()));
						}
						else if((selEnd + 1) == len) {
							if(debug) LogBuffer.println("Edited case 2: substring(0, " + selStart + ").");
							//If they have manually selected some point in the middle to the end,
							//backspace should delete, but unless we add a trailing null character,
							//autocomplete will re-fill in what they just tried to delete
							editor.setText(editor.getText().substring(0, selStart) + "\0");
						}
						else if(selStart == 0) {
							if(debug) LogBuffer.println("Edited case 3.");
							editor.setText(editor.getText().substring((selEnd + 1), len));
						}
					}
					editor.setSelectionStart(curNumChars);
					selStart = curNumChars;
					editor.setSelectionEnd(curNumChars);
					selEnd = curNumChars;
					if(debug) LogBuffer.println("Edited String: [" + editor.getText() + "].");
					curNumChars = editor.getText().length();
				}
				//Or nothing is selected and the cursor is not at the number of curNumChars and is not at the end
				else if(selStart == selEnd && selStart != curNumChars && curNumChars != len) {
					if(debug) LogBuffer.println("Manual cursor manipulation detected without selection.");
					//Clear out the stack and set the curnumchars to the cursor position
					curNumChars = selStart;
					selectedIndexStack.clear();
				}

				//This happens after the stack and curNumChars have been updated
				//If the stack and the number of current characters typed are in sync
				if(curNumChars == selectedIndexStack.size()) {
					//If there is still text in the field
					if(selectedIndexStack.size() > 0) {
						if(debug) LogBuffer.println("Setting the next-to-last selected index: [" + selectedIndexStack.get(selectedIndexStack.size() - 1) + "]");
						//Set the last selected index as selected
						searchTermBox.setSelectedIndex(selectedIndexStack.get(selectedIndexStack.size() - 1));
						//Set the selected text in the text box to the start of the autocomplete string
						editor.setSelectionStart(selectedIndexStack.size());
						selStart = selectedIndexStack.size();
						//searchTermBox.getEditor().setSelectionEnd();

						//If there is no search result
						if(searchSelection.getNumIndexes() == 0) {
							if(searchTermBox.isPopupVisible()) searchTermBox.setPopupVisible(false);
						} else {
							if(!searchTermBox.isPopupVisible()) searchTermBox.setPopupVisible(true);
						}
					}
					else {
						if(debug) LogBuffer.println("Clearing out the selected index stack");
						searchTermBox.setSelectedIndex(0);
						if(searchTermBox.isPopupVisible()) searchTermBox.setPopupVisible(false);
						curNumChars = 0;
					}
				}
				//Else if there is still text in the field that the user has typed
				else if (curNumChars > 0) {
					if(debug) LogBuffer.println("Released[stack != curNumChars] - Selection start: [" + selStart + "] Selection end: [" + selEnd + "] curNumChars: [" + curNumChars + "] String length: [" + len + "].  Selected text: [" + editor.getSelectedText() + "].  Full text: [" + editor.getText() + "]");
					if(debug) LogBuffer.println("Released[stack != curNumChars] - the selected index is: [" + searchTermBox.getSelectedIndex() + "]  Selected index stack size: [" + selectedIndexStack.size() + "].");

					//If the selection Start (i.e. the cursor) is at the current number of characters, the stack is empty, and there is selected text: delete the selected text
					if(curNumChars == selStart && selectedIndexStack.size() == 0 && selStart != selEnd) {
						if((selEnd + 1) != len && selStart != 0) {
							if(debug) LogBuffer.println("Edited case 1a.");
							editor.setText(editor.getText().substring(0, selStart) + editor.getText().substring((selEnd + 1), len));
						}
						else if((selEnd + 1) == len) {
							if(debug) LogBuffer.println("Edited case 2a: substring(0, " + selStart + ").");
							editor.setText(editor.getText().substring(0, selStart));
						}
						else if(selStart == 0) {
							if(debug) LogBuffer.println("Edited case 3a.");
							editor.setText(editor.getText().substring((selEnd + 1), len));
						}
						
						
					}


				}
				//searchTermBox.setPopupVisible(false);
			}
			else if(e.getKeyCode() == KeyEvent.VK_DOWN && e.getModifiers() == -1) {
				if(debug) LogBuffer.println("Arrowing down with modifiers [" + e.getModifiers() + "].");
				if(!searchTermBox.isPopupVisible()) searchTermBox.setPopupVisible(true);
				selectedIndexStack.clear();
				curNumChars = 0;
			}
			else if(e.getKeyCode() == KeyEvent.VK_UP && e.getModifiers() == -1) {
				if(debug) LogBuffer.println("Arrowing up with modifiers [" + e.getModifiers() + "].");
				if(!searchTermBox.isPopupVisible()) searchTermBox.setPopupVisible(true);
				selectedIndexStack.clear();
				curNumChars = 0;
			}
			else if(e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER) {
				if(searchTermBox.isPopupVisible()) searchTermBox.setPopupVisible(false);
			}
			//Else assume that something has been pasted or cut and make sure no text is left selected
			else {
				if(debug) LogBuffer.println("Released - Previous selection start: [" + selStart + "] Selection end: [" + selEnd + "] curNumChars: [" + curNumChars + "] String length: [" + len + "].  Selected text: [" + editor.getSelectedText() + "].  Full text: [" + editor.getText() + "]");
				if(debug) LogBuffer.println("Released - Current selection start: [" + editor.getSelectionStart() + "] Selection end: [" + editor.getSelectionEnd() + "] curNumChars: [" + curNumChars + "] String length: [" + editor.getText().length() + "].  Selected text: [" + editor.getSelectedText() + "].  Full text: [" + editor.getText() + "]");
				//If the string got longer, set the selection end as the current selection start
				if(len < editor.getText().length()) {
					editor.setSelectionEnd(editor.getSelectionStart());
				}
				//When the string gets shorter, it appears to work correctly, though I haven't tested to see what happens if the resulting text matches anything

				//If nothing is selected, hide the popup
				if(searchTermBox.getSelectedIndex() == -1) {
					if(searchTermBox.isPopupVisible()) searchTermBox.setPopupVisible(false);
				}
			}
		}

		@Override
		public void keyTyped(KeyEvent e) {
			//This gives us access to the text box contents and highlight control
			JTextComponent editor = ((JTextField) searchTermBox.getEditor().getEditorComponent());
			//Get the length of the text in the box (including auto-completed characters)
			int len = editor.getText().length();
			//When text is selected from right to left, the values are off - couldn't figure out a way to detect this!  It will cause the deleted text to be expanded 1 character on each side
			if(debug) LogBuffer.println("Typed - Selection start: [" + selStart + "] Selection end: [" + selEnd + "] curNumChars: [" + curNumChars + "] String length: [" + len + "].  Selected text: [" + editor.getSelectedText() + "].  Full text: [" + editor.getText() + "]");
			if(debug) LogBuffer.println("Typed - the selected index is: [" + searchTermBox.getSelectedIndex() + "].");

			if(e.getKeyChar() == KeyEvent.VK_ENTER) {
				selectedIndexStack.clear();
				curNumChars = 0;
				seekAll();
			}
			//Else if the typed character is a real character and not some sort of modifier
			//else if(e.getKeyCode() != KeyEvent.VK_SHIFT && e.getKeyCode() != KeyEvent.VK_DOWN && e.getKeyCode() != KeyEvent.VK_UP && e.getKeyCode() != KeyEvent.CHAR_UNDEFINED) {
			//else if(e.getKeyChar() >= ' ' && e.getKeyChar() <= '~') {    //<--ASCII support. V--Unicode support
			else if(((int) e.getKeyChar()) != 27  &&                       //Escape
					((int) e.getKeyChar()) != 127 &&                       //Delete (forward)
					((int) e.getKeyChar()) != 8) {                         //Backspace
				curNumChars++;
				if(debug) LogBuffer.println("Adding character [" + e.getKeyChar() + "].");

				//Record whether text was selected when this character was typed so that the selection state can
				//be preserved in KeyReleased or a new selection can be made if the term matches something in the select list
				if(selStart == selEnd) {
					textSelected = false;
				}
				else {
					textSelected = true;
				}

				//We determine whether we're going to record the currently selected index in
				//the stack based on whether there was a valid character entered into the term box
				//The current selected index upon keyTyped is actually one out-of-date, so we're going to tell
				//keyReleased to do it by setting this flag to true
				recordSelectedIndex = true;
			}
			if(debug) LogBuffer.println("Character typed: [" + e.getKeyChar() + "]. Undefined?: [" + ((e.getKeyChar() == KeyEvent.CHAR_UNDEFINED) ? "yes" : "no") + "]." +
					"When cast to int: [" + (int) e.getKeyChar() + "].");
		}
	}
	
	/**
	 * Test method for wild card search.
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Swing thread
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				
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
						
						boolean match = wildCardMatch(tf1.getText(), 
								tf2.getText());
						matchStatus.setText("WildCard Match: " + match);
					}
				});
				
				container.add(tf1, "growx, span, wrap");
				container.add(tf2, "growx, span, wrap");
				container.add(matchStrings, "span, pushx, alignx 50%, wrap");
				container.add(matchStatus, "span, pushx, alignx 50%");
				
				dialog.setVisible(true);
			}
		});
	}

}
