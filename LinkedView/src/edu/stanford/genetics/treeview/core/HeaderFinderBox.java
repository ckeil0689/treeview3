/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.core;

// for summary view...
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.text.JTextComponent;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.WideComboBox;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;
import net.miginfocom.swing.MigLayout;

/**
 * This class allows users to look for row or column elements by choosing them
 * in a drop down menu. The menu is populated with headers from the loaded data
 * matrix. The class is abstract and a basis for the GeneFinderPanel class
 * as well as the ArrayFinderPanel class.
 *
 * It extends JPanel and can be used as a Swing component.
 *
 */
public abstract class HeaderFinderBox {

	protected TreeSelectionI searchSelection;

	private HeaderInfo headerInfo;
	private HeaderSummary headerSummary;

	private String[][] searchDataList;
	private int primarySearchIndex;
	private int maxSearchIndex;
	private boolean[] searchExclusions;
	private WideComboBox searchTermBox;
	private final String type;

	// These are in order to determine whether a search result is currently
	// visible and if so, to zoom out
	protected TreeSelectionI otherSelection;
	private MapContainer globalSmap;
	private MapContainer globalOmap;

	private static final Color FOUNDCOLOR    = Color.WHITE;
	private static final Color NOTFOUNDCOLOR = Color.RED;

	//Handle into the other search box in order to have a single search box be
	//able to initiate the search using the contents of both boxes
	protected HeaderFinderBox companionBox;

	//defaultText is what's in the finder box before a term is entered.
	protected String defaultText;

//	boolean dropdownclicked = false;

	/**
	 * Constructor
	 * @param type - A string describing what's searched (e.g. Row or Column)
	 */
	public HeaderFinderBox(final String type) {

		this.type = type;
	}

	/**
	 * Setting this data member allows a search initiated in 1 box to be able to
	 * trigger the search in the other box as well so that every search utilizes
	 * the contents of both search boxes.
	 * @author rleach
	 * @param companionBox the companionBox to set
	 */
	public void setCompanionBox(HeaderFinderBox companionBox) {
		this.companionBox = companionBox;
	}

	/* >>>> Update the object with new data <<<<<< */
	public void setSelection(final TreeSelectionI searchSelection,
			final TreeSelectionI otherSelection) {

		this.searchSelection = searchSelection;
		this.otherSelection = otherSelection;
	}

	public void setHeaderSummary(final HeaderSummary headerSummary) {

		this.headerSummary = headerSummary;
	}

	public void setHeaderInfo(final HeaderInfo searchHI) {

		this.headerInfo = searchHI;
	}

	public void setMapContainers(final MapContainer searchMap,
			final MapContainer otherMap) {

		this.globalSmap = searchMap;
		this.globalOmap = otherMap;
	}

	public void setNewSearchTermBox() {

		if (headerSummary == null || headerInfo == null) {
			setEmptySearchTermBox();
			return;
		}

		final String[] labels = setupData();

		this.searchTermBox = GUIFactory.createWideComboBox(labels);
		searchTermBox.setEditable(true);
		searchTermBox.setBorder(null);
		searchTermBox.setBackground(GUIFactory.DARK_BG);
		AutoCompleteDecorator.decorate(searchTermBox);

		searchTermBox.getEditor().getEditorComponent()
				.addKeyListener(new BoxKeyListener());

		//NOTE: This may be removed without side-effect if a better way of
		//initiating a search upon combobox dropdown click is found
		addSearchMouseCommitListener();
	}

	//NOTE: This may be removed without side-effect if a better way of
	//initiating a search upon combobox dropdown click is found
	/**
	 * This method adds a mouse listener (SearchMouseCommitListener) to the
	 * dropdown menu of a combobox. Its purpose is to detect only 1 event: when
	 * a user clicks an item in the dropdown so we can capitalize on that
	 * trigger to initiate the search and display the results in the matrix as
	 * a yellow highlight.
	 * @author rleach
	 */
	private void addSearchMouseCommitListener() {
		//Support for catching a mouse-click selection from a combobox dropdown
		//menu is not standardly supported (without inadvertently triggering
		//upon other unrelated events as well).  The following code is the
		//result of extensive searching and tests to find a way to call seekAll
		//upon mouse selection of an item from the combobox's dropdown menu.
		//Every other method tried (ActionListener, ItemListener,
		//PopupMenuListener, MouseListeners attached in various other simple
		//ways, and even various combinations of multiple listeners did not work
		//well.  To see some details on those failed attempts, refer to:
		//http://stackoverflow.com/questions/33268726/how-do-you-detect-when-a-user-commits-a-jcombobox-selection-via-mouse-click-in-j
		//This solution below can be found here:
		//http://engin-tekin.blogspot.com/2009/10/hrefhttpkfd.html
		try {
			Field popupInBasicComboBoxUI =
				BasicComboBoxUI.class.getDeclaredField("popup");
			popupInBasicComboBoxUI.setAccessible(true);
			BasicComboPopup popup = (BasicComboPopup) popupInBasicComboBoxUI
				.get(searchTermBox.getUI());
			
			Field scrollerInBasicComboPopup =
				BasicComboPopup.class.getDeclaredField("scroller");
			scrollerInBasicComboPopup.setAccessible(true);
			JScrollPane scroller =
				(JScrollPane) scrollerInBasicComboPopup.get(popup);
			
			scroller.getViewport().getView()
				.addMouseListener(new SearchMouseCommitListener());
		}
		catch (NoSuchFieldException e) {
			e.printStackTrace();  
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();  
		}
	}

	//NOTE: This may be removed without side-effect if a better way of
	//initiating a search upon combobox dropdown click is found
	/**
	 * The following listener only performs 1 function: initiates a search when
	 * a user clicks a dropdown menu item from a combobox.
	 * @author rleach
	 */
	private class SearchMouseCommitListener extends MouseAdapter {
		//Upon mouseReleased, it is assumed that the user has just clicked an
		//item in a dropdown list in a combobox.  The search is initiated by
		//simulating an enter keypress using a robot which is caught
		//by the keyListener.  This is circuitous, but reliable, and easy to
		//replace using another method if a better way to do this is found.
		@Override
		public void mouseReleased(java.awt.event.MouseEvent e){
			//Calling seekAll() directly did not work. It worked occasionally
			//when called from ActionListener's actionPerformed method (only
			//when dropdownclicked was set to true here).  It turns out that
			//simulating an enter key keypress here works really well, albeit
			//admittedly hackily, but there's no standard way to initiate an
			//action only when an item in the dropdown is clicked without
			//initiating that action in a bunch of unrelated events as well.
			//http://stackoverflow.com/questions/18169598/how-can-i-programmatically-generate-keypress-events
			try {
				Robot robot = new Robot();
				robot.keyPress(KeyEvent.VK_ENTER);
				robot.keyRelease(KeyEvent.VK_ENTER);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void updateSearchIndexes() {
		if(searchDataList == null || searchDataList.length == 0
				|| headerSummary.getIncluded().length == 0) {
			setEmptySearchTermBox();
			primarySearchIndex = -1;
			maxSearchIndex = -1;
			return;
		}

		primarySearchIndex = headerSummary.getIncluded()[0];

		//If the saved label index to use (e.g. GID, UID, NAME, etc.) does
		//not exist among the headers in the file (which can be the case if
		//you had a file open with a bunch of header labels and the last one
		//was selected, and then you open a new file with fewer header
		//labels), revert the saved index to 0
//		if(primarySearchIndex >= searchDataList[0].length) {
//			primarySearchIndex = 0;
//			headerSummary.setIncluded(new int[] { 0 });
//		}

		maxSearchIndex = searchDataList[0].length - 1;
	}

	/**
	 * Used for errors.
	 */
	public void setEmptySearchTermBox() {

		final String[] labels = { "No data" };

		this.searchTermBox = GUIFactory.createWideComboBox(labels);
		searchTermBox.setEditable(true);
		searchTermBox.setFocusable(true);
		searchTermBox.setBorder(null);
		searchTermBox.setBackground(GUIFactory.DARK_BG);
	}

	private String[] setupData() {

		final String[][] hA = headerInfo.getHeaderArray();

		defaultText = "Search " + type + "s...";

		searchDataList    = copy2DStringArray(hA);
		String[] searchDataHeaders = { "" };
		updateSearchIndexes();

		//Determine which label types to skip
		searchExclusions = new boolean[maxSearchIndex + 1];
		int numExclusions = 0;
		for(int i = 0;i <= maxSearchIndex;i++) {
			if(allStringsEqual(hA,i) && i != primarySearchIndex) {
				searchExclusions[i] = true;
				numExclusions++;
				continue;
			} else {
				searchExclusions[i] = false;
			}
		}

		int dropdownSize =
			//Full number of all labels
			(searchDataList.length * (maxSearchIndex + 1)) -
			//Number of excluded labels
			searchDataList.length * numExclusions +
			//For the "Search rows..." default text
			1;

		final String[] labeledHeaders = new String[dropdownSize];

		labeledHeaders[0] = defaultText;

		int startIndex = 1;

		for(int i = 0;i <= maxSearchIndex;i++) {
			if(searchExclusions[i]) {
				continue;
			}

			searchDataHeaders = getHeaders(hA,i);

			Arrays.sort(searchDataHeaders);
	
			System.arraycopy(searchDataHeaders, 0, labeledHeaders, startIndex,
					searchDataHeaders.length);

			startIndex += searchDataHeaders.length;
		}

		//Returns an array of labels for use by the combobox's dropdown list
		return(labeledHeaders);
	}

	/**
	 * Determines whether all strings located at hA[*][i] are equal so that
	 * label types which have no discerning search power do not waste space in
	 * the combobox.
	 * @author rleach
	 * @param hA, i
	 * @return boolean
	 */
	public boolean allStringsEqual(String[][] hA,int i) {
		for(int j = 1;j < hA.length;j++) {
			if(!hA[0][i].equals(hA[j][i])) {
				return(false);
			}
		}
		return(true);
	}

	/**
	 * Returns the content panel which keeps the GUI components.
	 *
	 * @return JPanel
	 */
	public WideComboBox getSearchTermBox() {

		return searchTermBox;
	}

	/**
	 * Copies a 2D array of strings for searching.
	 * @param old
	 * @return String[][]
	 */
	public String[][] copy2DStringArray(String[][] old) {
		if(old == null || old[0] == null) {
			return(old);
		}
		String[][] copy = new String[old.length][old[0].length];
		for(int i=0; i<old.length; i++)
			for(int j=0; j<old[i].length; j++)
				copy[i][j]=old[i][j];
		return(copy);
	}

	/**
	 * Obtains the visible column/row labels.
	 *
	 * @param hA
	 * @return
	 */
	public String[] getHeaders(final String[][] hA) {
		return(getHeaders(hA,primarySearchIndex));
	}

	/**
	 * Obtains all column/row labels.
	 *
	 * @param hA
	 * @return
	 */
	public String[] getHeaders(final String[][] hA,int index) {

		final String[] headerArray = new String[hA.length];
		updateSearchIndexes();

		if(hA.length == 0 || (index + 1) > hA[0].length) {
			return(headerArray);
		}

		for (int i = 0; i < hA.length; i++) {

			final String yorf = hA[i][index];
			headerArray[i] = yorf;
		}

		return(headerArray);
	}

	/**
	 * Initiates searches using search terms from this search term box and the
	 * companion search term box. (i.e. both a row and column search)
	 * Controls the text color in the box based on whether something was found.
	 * @author rleach
	 */
	public void seekAll() {
		boolean gotSomething = false;

		//Deselect my previous search results so that the companion search isn't
		//limited (which will be narrowed down afterward)
		searchSelection.deselectAllIndexes();

		//If the companion box has anything in it, perform that search first so
		//that the search in the focused finder box will work as originally
		//designed (as a search that respects existing search results in the
		//companion box)
		if(companionBox.isSearchTermEntered()) {
			companionBox.updateSearchTextColor(companionBox.seekAllHelper());

			if(otherSelection.getNSelectedIndexes() > 0 &&
				isSearchTermEntered()) {

				gotSomething = this.seekAllHelper();
				updateSearchTextColor(gotSomething);
				
				//If this search box produced no results, default to the results
				//from the other search box
				if(!gotSomething) {
					companionBox.seekAllHelper();
				}

				//Commented this out because the red background's prominence is
				//good enough to indicate that there's a problem, therefore
				//highlighting the good results from the other search box should
				//not be a problem.
//				//If there were no results from this search, deselect the
//				//companion's search results
//				if(searchSelection.getNSelectedIndexes() == 0) {
//					otherSelection.deselectAllIndexes();
//				}
			} else {
				if(isSearchTermEntered()) {
					gotSomething = this.seekAllHelper();
				} else {
					//The search box is empty, so there's no reason for it to
					//possibly remain red from a previously failed search.
					gotSomething = true;
				}
				updateSearchTextColor(gotSomething);
			}
		} else {
			//The companion search box is empty, so there's no reason for it to
			//possibly remain red from a previously failed search.
			companionBox.updateSearchTextColor(true);
			gotSomething = this.seekAllHelper();
			updateSearchTextColor(gotSomething);
		}
	}

	public void updateSearchTextColor(final boolean gotSomething) {
		if(gotSomething) {
			LogBuffer.println("Changed to found color");
			searchTermBox.getEditor().getEditorComponent().
				setBackground(FOUNDCOLOR);
		} else {
			LogBuffer.println("Changed to notfound color");
			searchTermBox.getEditor().getEditorComponent().
				setBackground(NOTFOUNDCOLOR);
		}
	}

	/**
	 * Searches this class's search term box.  Respects selections already found
	 * from a companion search.
	 */
	public boolean seekAllHelper() {

		searchSelection.deselectAllIndexes();

		final List<Integer> indexList = findSelected();

		// Initialize the min and max index used to determine whether result is
		// currently visible
		int minIndex = 0;
		int maxIndex = 0;
		if (indexList.size() > 0) {
			minIndex = indexList.get(0);
			maxIndex = indexList.get(0);
		}

		// Set the found indexes as selected and determine min/max selected
		// indexes
		for (int i = 0; i < indexList.size(); i++) {

			if (indexList.get(i) < minIndex) {
				minIndex = indexList.get(i);
			}
			if (indexList.get(i) > maxIndex) {
				maxIndex = indexList.get(i);
			}
			searchSelection.setIndexSelection(indexList.get(i), true);
		}

		searchSelection.notifyObservers();

		// Determine pre-selected min/max from the other dimension to see if
		// they are visible
		int otherMinIndex = otherSelection.getMinIndex();
		int otherMaxIndex = otherSelection.getMaxIndex();

		if ((indexList.size() > 0) &&
		// At least part of the found min/max selected area is not visible
		// This assumes that min is less than max and that the visible area is a
		// contiguous block of visible indexes
				(minIndex < globalSmap.getFirstVisible() || maxIndex > (globalSmap
						.getFirstVisible() + globalSmap.getNumVisible() - 1))) {

			globalSmap.setMinScale();
		}

		if ((otherSelection.getNSelectedIndexes() == 0 ||
			otherMinIndex < globalOmap.getFirstVisible() ||
			otherMaxIndex > (globalOmap.getFirstVisible() +
				globalOmap.getNumVisible() - 1))) {

			globalOmap.setMinScale();
		}

		if(indexList.size() > 0) {
			return(true);
		}
		return(false);
	}

	/**
	 * Determines whether anything was entered into the finer box.  Returns
	 * false if the finder box is empty or if its contents are the default text.
	 * @author rleach
	 * @return boolean
	 */
	public boolean isSearchTermEntered() {
		if(getSearchTerm() == "" || getSearchTerm() == defaultText) {
			return(false);
		}
		return(true);
	}

	private List<Integer> findSelected() {

		final List<Integer> primaryIndexList    = new ArrayList<Integer>();
		final List<Integer> primarySubstrList   = new ArrayList<Integer>();
		final List<Integer> secondaryIndexList  = new ArrayList<Integer>();
		final List<Integer> secondarySubstrList = new ArrayList<Integer>();

		final String sub = getSearchTerm();

		String wildcardsub = sub;
		if(!"*".equalsIgnoreCase(wildcardsub.substring(0, 1))) {
			wildcardsub = "*" + wildcardsub;
		}
		if("*".equals(wildcardsub.substring((wildcardsub.length() - 1),
			wildcardsub.length()))) {

			wildcardsub = wildcardsub + "*";
		}

		for(int i = 0; i < searchDataList.length;i++) {
			String header = searchDataList[i][primarySearchIndex];
			if(wildCardMatch(header, sub)) {
				primaryIndexList.add(i);
			}
			if(wildCardMatch(header, wildcardsub)) {
				primarySubstrList.add(i);
			}

			//This searches secondary labels (those not visible)
			for(int j = 0; j <= maxSearchIndex;j++) {
				if(j == primarySearchIndex || searchExclusions[j]) {
					continue;
				}
				header = searchDataList[i][j];
				if(wildCardMatch(header, sub)) {
					secondaryIndexList.add(i);
					break;
				}
				if(wildCardMatch(header, wildcardsub)) {
					secondarySubstrList.add(i);
					break;
				}
			}
		}

		//Only returns a perfect match in the visible label type if a match
		//exists.  If not, it steps through: substring matches in the visible
		//label type, perfect matches to labels that are not visible, and
		//substring matches to labels that are not visible
		if(primaryIndexList.size() > 0) {
			return(primaryIndexList);
		} else if(primarySubstrList.size() > 0) {
			return(primarySubstrList);
		} else if(secondaryIndexList.size() > 0) {
			return(secondaryIndexList);
		} else {
			return(secondarySubstrList);
		}
	}

	/**
	 * This method retrieves the search term that was entered into the finder
	 * box by the user.
	 * @author rleach
	 * @return String
	 */
	public String getSearchTerm() {
		return(searchTermBox.getSelectedItem().toString());
	}

	/**
	 * Performs a wildcard matching for the text and pattern provided. Matching
	 * is done based on regex patterns.
	 *
	 * @param text
	 *            the text to be tested for matches.
	 *
	 * @param pattern
	 *            the pattern to be matched for. This can contain the wildcard
	 *            character '*' (asterisk).
	 *
	 * @return <tt>true</tt> if a match is found, <tt>false</tt> otherwise.
	 */
	public static boolean wildCardMatch(final String text, String pattern) {

		if (text == null || pattern == null)
			return false;

		// Escape all metacharacters except our supported wildcards
		pattern = pattern.replaceAll("([^A-Za-z0-9 \\?\\*])", "\\\\$1");
		// Convert our wildcards to regular expression syntax
		pattern = pattern.replaceAll("\\?", ".");
		pattern = pattern.replaceAll("\\*", ".*");

		// Check if generated regex matches, store result in boolean.
		boolean isMatch = false;
		if (text.matches(pattern)) {
			isMatch = true;
		}

		return isMatch;
	}

	abstract public void scrollToIndex(int i);

	/**
	 * KeyListener to implement search by pressing enter when the combobox has
	 * focus.
	 *
	 * @author CKeil
	 *
	 */
	class BoxKeyListener extends KeyAdapter {

		private int selStartPressed = 0;
		private int selEndPressed = 0;
		private int lenPressed = 0;
		private int selIndexPressed = -1;
		private String fullTextPressed = "";
		private int selStartTyped = 0;
		private int selEndTyped = 0;
		private int lenTyped = 0;
		private int selIndexTyped = -1;
		private final boolean debug = false;
		private boolean changed = false;
		JTextComponent editor = ((JTextField) searchTermBox.getEditor()
				.getEditorComponent());

		// NOTE: command-w never worked to close the search window before adding
		// anything in this class other than the seekAll function, though
		// command-q WOULD quit the app

		@Override
		public void keyPressed(final KeyEvent e) {
			selStartPressed = editor.getSelectionStart();
			selEndPressed = editor.getSelectionEnd();
			lenPressed = editor.getText().length();
			selIndexPressed = searchTermBox.getSelectedIndex();
			fullTextPressed = editor.getText();

			if (debug) {
				LogBuffer.println("Pressed - Selection start: ["
						+ selStartPressed + "] " + "Selection end: ["
						+ selEndPressed + "] " + "String length: ["
						+ lenPressed + "]. " + "Selected text: ["
						+ editor.getSelectedText() + "]. " + "Full text: ["
						+ editor.getText() + "]. " + "Selected index is: ["
						+ selIndexPressed + "]. " + "Character: ["
						+ e.getKeyChar() + "]. " + "When cast to int: ["
						+ (int) e.getKeyChar() + "].");
			}

			//Commented this out so that the background would possibly remain
			//red until good results are found.
//			LogBuffer.println("Color reset to found color");
//			searchTermBox.getEditor().getEditorComponent().
//				setBackground(FOUNDCOLOR);
		}

		// The delete key is selecting what one tries to delete, thus if
		// you search for 'GENE105' and then decide you want 'GENE10',
		// hitting delete does not result in the default selection of
		// anything other than 'GENE105'. The following is an attempt
		// to make it work better...
		@Override
		public void keyReleased(final KeyEvent e) {

			/* If enter key is pressed, search */
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				LogBuffer.println("search initialized");
				seekAll();
				return;
			}

			final int selStartRel = editor.getSelectionStart(); // Selection
			// start before
			// having typed
			final int selEndRel = editor.getSelectionEnd(); // Selection end
			// before having
			// typed
			final int lenRel = editor.getText().length(); // Length before
			// having typed
			final int selIndexRel = searchTermBox.getSelectedIndex(); // Selected
			// index
			// before
			// having
			// typed

			if (debug) {
				LogBuffer.println("  Relsd - Selection start: [" + selStartRel
						+ "] " + "Selection end: [" + selEndRel + "] "
						+ "String length: [" + lenRel + "]. "
						+ "Selected text: [" + editor.getSelectedText() + "]. "
						+ "Full text: [" + editor.getText() + "]. "
						+ "Selected index is: [" + selIndexRel + "]. "
						+ "Character typed: [" + e.getKeyChar() + "]. "
						+ "When cast to int: [" + (int) e.getKeyChar() + "].");
			}

			// If the contents of the text field have changed and nothing in the
			// select list is selected
			if ((changed || lenPressed != lenRel)
					&& searchTermBox.getSelectedIndex() == -1) {
				if ((e.getKeyChar()) == 127 || // Delete (forward)
						(e.getKeyChar()) == 8) { // Backspace
					editor.setSelectionStart(selStartTyped);
					editor.setSelectionEnd(selStartTyped);
				} else {
					editor.setSelectionStart(selStartRel);
					editor.setSelectionEnd(selStartRel);
				}
				// searchTermBox.setPopupVisible(false);
			}
			// Else if no text changed, there was selected text, and a left or
			// right arrow was pressed without modifiers
			else if (!changed && lenPressed == lenRel) {
				if (selStartPressed != selEndPressed
						&& (e.getKeyCode() == KeyEvent.VK_RIGHT || e
								.getKeyCode() == KeyEvent.VK_LEFT)
						&& e.getModifiers() == 0) {
					if (debug) {
						LogBuffer
								.println("Positioning cursor at edge of selection...");
					}
					if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
						editor.setSelectionStart(selEndPressed);
						editor.setSelectionEnd(selEndPressed);
					} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
						editor.setSelectionStart(selStartPressed);
						editor.setSelectionEnd(selStartPressed);
					}
				} else if ((e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN)
						&& e.getModifiers() == 1
						&& e.getModifiersEx() == InputEvent.SHIFT_DOWN_MASK) {
					if (debug) {
						LogBuffer.println("Expanding selection to end...");
					}
					if (e.getKeyCode() == KeyEvent.VK_UP) {
						editor.setSelectionStart(0);
						editor.setSelectionEnd(selEndPressed);
					} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
						editor.setSelectionStart(selStartPressed);
						editor.setSelectionEnd(lenPressed);
					}
				}
			} else {
				if (debug) {
					LogBuffer.println("Nothing to do because changed is "
							+ (changed ? "" : "not ") + "true, the length has "
							+ (lenPressed == lenRel ? "not " : "")
							+ "changed and not sideways arrow keys were "
							+ "pressed and there were " + e.getModifiers()
							+ " modifiers.");
				}
			}

			changed = false;
		}

		@Override
		public void keyTyped(final KeyEvent e) {

			// There's a weird case where sometimes hitting backspace after
			// having manually selected some text causes the selection end to
			// decrement. The selection end in the keyPressed function always
			// seems to be right, so we'll make sure we keep it:
			editor.setSelectionEnd(selEndPressed);

			selStartTyped = editor.getSelectionStart(); // Selection start
			// before having typed
			selEndTyped = editor.getSelectionEnd(); // Selection end before
			// having typed
			lenTyped = editor.getText().length(); // Length before having typed
			selIndexTyped = searchTermBox.getSelectedIndex(); // Selected index
			// before having
			// typed

			if (debug) {
				LogBuffer.println("  Typed - Selection start: ["
						+ selStartTyped + "] " + "Selection end: ["
						+ selEndTyped + "] " + "String length: [" + lenTyped
						+ "]. " + "Selected text: [" + editor.getSelectedText()
						+ "]. " + "Full text: [" + editor.getText() + "]. "
						+ "Selected index is: [" + selIndexTyped + "]. "
						+ "Character typed: [" + e.getKeyChar() + "]. "
						+ "When cast to int: [" + (int) e.getKeyChar() + "].");
			}

			// If the backspace was typed, there was a selection, and the
			// selStartPressed is 1 more than the selStartTyped,
			// it most likely means that text was highlighted from right to left
			// and the text will be incorrectly edited
			// (a bug of the parent class - because they didn't anticipate text
			// to be manipulated this way), so let's nip that in the bud
			if ((e.getKeyChar()) == 8 && selStartPressed != selEndPressed
					&& (selStartPressed - 1) == selStartTyped) {
				selStartTyped = selStartPressed;
				editor.setSelectionStart(selStartPressed);
			}
			// Else if the selStartPressed is the same, but the selEndPressed ==
			// lenPressed (and not equal to selStartPressed)
			// and the selEndTyped is equal to selStartTyped and selIndexPressed
			// is > -1 and selIndexTyped == -1 and a backspace was typed,
			// it most likely means that text was previously (on a previous
			// backspace keystroke) selected and deleted to create a resulting
			// matching string
			// and the current backspace inadvertently does nothing (a bug of
			// the parent class as well), so let's nip that in the bud.
			else if (selStartPressed == selStartTyped
					&& selEndPressed == lenPressed
					&& selEndPressed != selStartPressed
					&& selEndTyped == selStartTyped && selIndexPressed > -1
					&& selIndexTyped == -1 && (e.getKeyChar()) == 8) {
				editor.setText(fullTextPressed);
				selStartTyped = selStartPressed;
				editor.setSelectionStart(selStartPressed);
				selEndTyped = selEndPressed;
				editor.setSelectionEnd(selEndPressed);
				lenTyped = lenPressed;
				selIndexTyped = selIndexPressed;
				searchTermBox.setSelectedIndex(selIndexPressed);
			}

			// searchTermBox.setPopupVisible(true);

			// Perform the search to highlight the result in the main window if
			// enter key is typed
			if (e.getKeyChar() == KeyEvent.VK_ENTER) {
				seekAll();
			}
			// If the delete or backspace is typed, set the new text content to
			// force autocomplete to update
			else if ((e.getKeyChar()) == 127 || // Delete (forward)
					(e.getKeyChar()) == 8) { // Backspace
				// Only need to manipulate the edit action if there's a
				// currently selected index
				if (selIndexPressed > -1) {
					if (lenTyped > 0) {
						if (selEndTyped != lenTyped && selStartTyped != 0) {
							if (debug) {
								LogBuffer.println("Edited case 1a.");
							}
							if (selStartTyped != selEndTyped) {
								final String editedText = editor.getText()
										.substring(0, selStartTyped)
										+ editor.getText().substring(
												selEndTyped, lenTyped);
								if (debug) {
									LogBuffer.println("Setting text to ["
											+ editedText + "].");
								}
								editor.setText(editedText);
							} else {
								editor.setText(editor.getText().substring(0,
										selStartTyped)
										+ editor.getText().substring(
												selEndTyped, lenTyped));
							}
						} else if (selEndTyped == lenTyped) {
							if (debug) {
								LogBuffer
										.println("Edited case 2a: substring(0, "
												+ selStartTyped + ").");
							}
							if (selStartTyped == 0) {
								searchTermBox.setSelectedIndex(0);
							} else {
								editor.setText(editor.getText().substring(0,
										selStartTyped));
								if (searchTermBox.getSelectedIndex() == -1) {
									editor.setSelectionStart(selStartTyped);
									editor.setSelectionEnd(selStartTyped);
									// searchTermBox.setPopupVisible(false);
								}
							}
						} else if (selStartTyped == 0) {
							if (debug) {
								LogBuffer.println("Edited case 3a.");
							}
							editor.setText(editor.getText().substring(
									selEndTyped, lenTyped));
							if (searchTermBox.getSelectedIndex() == -1) {
								editor.setSelectionStart(selStartTyped);
								editor.setSelectionEnd(selStartTyped);
								// searchTermBox.setPopupVisible(false);
							}
						}
					} else {
						searchTermBox.setSelectedIndex(0);
					}
				}
			}
			// Else if a character was entered into the field, ensure the
			// selected text was replaced
			else if ((e.getKeyChar()) != 27) {
				// If the previous selected index is -1, make the edit manually
				// because typing over selected text doesn't seem to work
				// otherwise
				if (searchTermBox.getSelectedIndex() == -1) {
					if (debug) {
						LogBuffer
								.println("Trying to force editing manually selected "
										+ "text to work");
					}

					if (lenTyped > 0) {
						if (selEndTyped != lenTyped && selStartTyped != 0) {
							if (debug) {
								LogBuffer.println("Edited case 1b.");
							}
							if (selStartTyped != selEndTyped) {
								// All I have to do is remove the selected text
								// and what was typed will be inserted between
								// here and keyReleased
								editor.setText(editor.getText().substring(0,
										selStartTyped)
										+ editor.getText().substring(
												(selEndTyped), lenTyped));
							} else {
								// Actually the default behavior appears to work
								// automatically in this instance - it's only
								// when there's selected text when there's a
								// problem
								// editor.setText(editor.getText().substring(0,
								// selStartTyped) + e.getKeyChar() +
								// editor.getText().substring(selEndTyped,
								// lenTyped));
							}
						} else if (selEndTyped == lenTyped) {
							if (debug) {
								LogBuffer
										.println("Edited case 2b: substring(0, "
												+ selStartTyped + ").");
							}
							if (selStartTyped == 0) {
								searchTermBox.setSelectedIndex(0);
							} else {
								// All I have to do is remove the selected text
								// and what was typed will be inserted between
								// here and keyReleased
								editor.setText(editor.getText().substring(0,
										selStartTyped));// + e.getKeyChar());
								if (searchTermBox.getSelectedIndex() == -1) {
									editor.setSelectionStart(selStartTyped);
									editor.setSelectionEnd(selStartTyped);
									// searchTermBox.setPopupVisible(false);
								}
							}
						} else if (selStartTyped == 0) {
							if (debug) {
								LogBuffer.println("Edited case 3b.");
							}
							// The parent class will add the typed character at
							// the beginning between keyTyped and keyReleased
							// (but apparently we need to delete the selected
							// text manually)
							editor.setText(/* e.getKeyChar() + */editor
									.getText().substring(selEndTyped, lenTyped));
							if (searchTermBox.getSelectedIndex() == -1) {
								editor.setSelectionStart(selStartTyped);
								editor.setSelectionEnd(selStartTyped);
								// searchTermBox.setPopupVisible(false);
							}
						}
					} else {
						searchTermBox.setSelectedIndex(0);
					}
				}
			}

			// If the content of the text field changed (i.e. an escape or enter
			// character was not typed - assumes no other non-chars get in here)
			// Ensure autocomplete worked, the selected index is correct, and
			// that the text selection/cursor-placement is accurate
			if ((e.getKeyChar()) != 27 && e.getKeyChar() != KeyEvent.VK_ENTER) {
				// If the previous selected index is -1, try to force a matching
				// index to be selected
				if (searchTermBox.getSelectedIndex() == -1
						&& selIndexTyped == -1) {

					// Get the current text content
					final String content = editor.getText();

					if (debug) {
						LogBuffer
								.println("Trying to force a selection to be made 1.  "
										+ "Current text: [" + content + "].");
					}

					// searchTermBox.setKeySelectionManager(
					// searchTermBox.getKeySelectionManager());
					// //Doesn't work
					// searchTermBox.setEnabled(false);
					// searchTermBox.setEnabled(true);
					// //Doesn't work
					// Force entry into a mode where indexes are selected by
					// entering an S (Corresponding to the default text field
					// entry of "Search Row/Column Labels... "
					searchTermBox.selectWithKeyChar('S');
					// Now reset the text back to what it was to force a
					// selection (if one exists)
					if (content.length() > 0) {
						editor.setText(content);
					}
					// The above puts the cursor at the end of the total text
					// Now put the cursor back where it was
					// If the typed key was backspace or delete
					if ((e.getKeyChar()) == 127) { // Delete (forward)
						// Put the cursor at the beginning of the previously
						// selected text (because that text is now gone)
						editor.setSelectionStart(selStartTyped);
						// If there is still not a selected index, also set the
						// end position there (otherwise, it will stay at the
						// end)
						if (searchTermBox.getSelectedIndex() == -1) {
							editor.setSelectionStart(selStartTyped);
						}
						if (searchTermBox.getSelectedIndex() > -1) {
							editor.setSelectionEnd(editor.getText().length());
						}
					} else if ((e.getKeyChar()) == 8) { // Backspace
						// Put the cursor at 1 before the beginning of the
						// previously selected text (because that text is now
						// gone and the way this is set up to behave is that the
						// selected text contains autofill characters, not an
						// explicit selection to delete without deleting a new
						// character)
						editor.setSelectionStart(selStartTyped);
						// If there is still not a selected index, also set the
						// end position there (otherwise, it will stay at the
						// end)
						if (searchTermBox.getSelectedIndex() == -1) {
							editor.setSelectionStart(selStartTyped);
						} else if (searchTermBox.getSelectedIndex() > -1) {
							editor.setSelectionEnd(editor.getText().length());
						}
					} else {
						// Put the cursor after the typed character
						editor.setSelectionStart(selStartTyped + 1);
						// If there is still not a selected index, also set the
						// end position there (otherwise, it will stay at the
						// end)
						if (searchTermBox.getSelectedIndex() == -1) {
							editor.setSelectionStart(selStartTyped);
							editor.setSelectionEnd(selStartTyped);
						}
						if (searchTermBox.getSelectedIndex() > -1) {
							editor.setSelectionEnd(editor.getText().length());
						}
					}
				} else {
					if ((e.getKeyChar()) == 8 && // Backspace
							searchTermBox.getSelectedIndex() > -1) {
						if (selEndTyped == lenTyped) {
							if (debug) {
								LogBuffer.println("Trying to force the "
										+ "selection to regress.  "
										+ "Current selected index: ["
										+ searchTermBox.getSelectedIndex()
										+ "].");
							}
							// Put the cursor at 1 before the beginning of the
							// previously selected text (because that text is
							// now gone and the way this is set up to behave is
							// that the selected text contains autofill
							// characters, not an explicit selection to delete
							// without deleting a new character)
							if (selStartTyped == selStartPressed) {
								editor.setSelectionStart(selStartTyped - 1);
								editor.setSelectionEnd(editor.getText()
										.length());

								String content = "";
								if (editor.getText().length() >= (selStartTyped - 1)
										&& selStartTyped > 1) {
									if (debug) {
										LogBuffer.println("Remaining text "
												+ "length: ["
												+ editor.getText().length()
												+ "]. selStartTyped: " + "["
												+ selStartTyped + "]");
									}
									// Get the current unselected text content
									content = editor.getText().substring(0,
											selStartTyped - 1);
								}

								if (debug) {
									LogBuffer.println("Trying to force a " +
										"selection to be made 2.  Current " +
										"text: [" + content + "].");
								}

								// searchTermBox.setKeySelectionManager(
								// searchTermBox.getKeySelectionManager());
								// //Doesn't work
								// searchTermBox.setEnabled(false);
								// searchTermBox.setEnabled(true);
								// //Doesn't work
								// Force entry into a mode where indexes are
								// selected by entering an S (Corresponding to
								// the default text field entry of
								// "Search Row/Column Labels... ")
								searchTermBox.selectWithKeyChar('S');
								searchTermBox.setSelectedIndex(0); // We're
																	// doing
																	// this just
																	// in case
																	// there's a
																	// different
																	// S match
																	// and
																	// content
																	// is an
																	// empty
																	// string
								if (content.length() > 0) {
									// Now reset the text back to what it was to
									// force a selection (if one exists)
									editor.setText(content);
								}
							}
						} else if (searchTermBox.getSelectedIndex() > -1) {
							editor.setSelectionEnd(editor.getText().length());
						}
					}
				}

				// There's a weird case where the second backspace after
				// removing a non-matching character to create a matching string
				// causes the end of the selection to decrement instead of the
				// beginning of the selection to decrement and the character
				// preceding the selStart isn't removed. So...
				if (selIndexTyped == -1 && selIndexPressed > -1
						&& (e.getKeyChar()) == 8
						&& selStartTyped == selStartPressed
						&& (selEndTyped + 1) == selEndPressed) {
					if (debug) {
						LogBuffer.println("Trying to force a selection to be " +
							"made 3");
					}
					if ((selStartTyped - 1) > 0) {
						// Get the current text content
						final String content = editor.getText().substring(0,
								selStartTyped - 1);
						// Force entry into a mode where indexes are selected by
						// entering an S (Corresponding to the default text
						// field entry of "Search Row/Column Labels... "
						searchTermBox.selectWithKeyChar('S');
						// Now reset the text back to what it was to force a
						// selection (if one exists)
						editor.setText(content);
						// Now set the selected text properly
						editor.setSelectionStart(selStartTyped - 1);
						if (searchTermBox.getSelectedIndex() > -1) {
							editor.setSelectionEnd(editor.getText().length());
						} else {
							editor.setSelectionEnd(selStartTyped - 1);
						}
					} else {
						searchTermBox.setSelectedIndex(0);
					}

				}
				changed = true;
			}
		}
	}

	/**
	 * Test method for wild card search.
	 *
	 * @param args
	 */
	public static void main(final String[] args) {

		// Swing thread
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				final JDialog dialog = new JDialog();
				dialog.setTitle("WildCard Search Test");
				dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog.setSize(new Dimension(400, 150));

				final JPanel container = new JPanel();
				container.setLayout(new MigLayout());

				dialog.getContentPane().add(container);

				final JTextField tf1 = new JTextField();
				tf1.setEditable(true);

				final JTextField tf2 = new JTextField();
				tf2.setEditable(true);

				final JLabel matchStatus = new JLabel("WildCard Match:");

				final JButton matchStrings = new JButton("Check match");
				matchStrings.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(final ActionEvent arg0) {

						final boolean match = wildCardMatch(tf1.getText(),
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
