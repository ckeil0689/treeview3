package edu.stanford.genetics.treeview;

/**
 * This class is used as a utility class. It contains only static String fields
 * so that other classes throughout the program can centrally access them.
 * This helps to prevent spelling errors, ensures consistency etc. 
 * when certain Strings are used more than once.
 * 
 * @author CKeil
 *
 */
public class StringRes {

	
	// Global Application Fields
	/** Version of application */
	public final static String versionTag = "0.1";

	/** Homepage for updates */
	public final static String updateUrl = "https://www.princeton.edu/" +
			"~abarysh/treeview/";
	
	/** url of announcements mailing list */
	public final static String announcementUrl = "https://www.princeton.edu/" +
			"~abarysh/treeview/";
	
	public static final String appName = "TreeView 3";
	
	// View Names
	public static final String view_Welcome = "WelcomeView";
	public static final String view_Dendro = "DendroView";
	
	// View Titles
	public static final String view_title_Cluster = "Cluster View";
	
	// Preferences Menu Titles
	public static final String menu_title_Theme = "Theme";
	public static final String menu_title_Font = "Font";
	public static final String menu_title_URL = "URL";
	public static final String menu_title_RowAndCol = "Row and Column Labels";
	public static final String menu_title_Color = "Color Settings";
	public static final String menu_title_Hier = "Hierarchical";
	public static final String menu_title_KMeans = "K-Means";
	
	// MenuBar
	public static final String menubar_clearPrefs = "Clear Preferences";
	
	// Preferences API
	// Nodes
	public static final String pref_node_Preferences = "Preferences";
	public static final String pref_node_DendroView = "DendroView";
	public static final String pref_node_File = "File";
	
	// Button Texts
	public static final String button_OK = "OK";
	public static final String button_Cancel = "Cancel";
	public static final String button_Cluster = "Cluster";
	public static final String button_customLabels = "Load Custom Labels";
	
	// RadioButton Texts
	public static final String rButton_dark = "Dark";
	public static final String rButton_light = "Light";
	
	// Labels
	public static final String label_ZoomColLabels = "Zoom to See Column " +
			"Labels";
	public static final String label_ZoomRowLabels = "Zoom to See Row Labels";
	
	// Cluster Options
	public static final String cluster_DoNot = "Do Not Cluster";
	
	// Frame Title - Testingo
	public static final String test_title_FontSelector = "Font Settings Test";
}
