package edu.stanford.genetics.treeview;

/**
 * This class is used as a utility class. It contains only static String fields
 * so that other classes throughout the program can centrally access them. This
 * helps to prevent spelling errors, ensures consistency etc. when certain
 * Strings are used more than once.
 * 
 * @author CKeil
 * 
 */
public class StringRes {

	// Global Application Fields
	/** Version of application */
	public final static String versionTag = "0.1";

	/** Homepage for updates */
	public final static String updateUrl = "https://www.princeton.edu/"
			+ "~abarysh/treeview/";

	/** url of announcements mailing list */
	public final static String announcementUrl = "https://www.princeton.edu/"
			+ "~abarysh/treeview/";

	public static final String appName = "TreeView 3";
	
	// General
	public static final String empty = "";
	public static final String dot = ".";
	

	// View Names
	public static final String view_Welcome = "WelcomeView";
	public static final String view_Dendro = "DendroView";
	public static final String view_LoadProg = "LoadProgressView";
	public static final String view_LoadError = "LoadErrorView";

	// Frame Titles
	public static final String view_title_Cluster = "Cluster View";
	public static final String dialog_title_search = "Search Labels";
	public static final String dialog_title_exitConfirm = "Confirm Exit";
	public static final String dialog_title_about = "About...";
	
	// Main Words
	public static final String main_rows = "Rows";
	public static final String main_cols = "Columns";
	
	// Title Screen
	public static final String title_Hello = "Hello, how are you Gentlepeople?";
	public static final String title_Welcome = "Welcome to ";
	
	// Loading
	public static final String loading_OneSec = "One moment, please.";
	public static final String loading_active = "Loading you data!";
	public static final String loading_Ohoh = "Oh oh!";
	public static final String loading_Error = "Looks like we ran into the " +
	"following issue: ";

	// Menu Titles
	public static final String menu_title_Open = "Open...";
	public static final String menu_title_OpenRecent = "Open Recent";
	public static final String menu_title_EditRecent = "Edit Recent Files";
	public static final String menu_title_Save = "Save";
	public static final String menu_title_SaveAs = "Save As...";
	public static final String menu_title_Prefs = "Preferences";
	public static final String menu_title_Theme = "Theme";
	public static final String menu_title_Font = "Font";
	public static final String menu_title_URL = "URL";
	public static final String menu_title_RowAndCol = "Row and Column Labels";
	public static final String menu_title_Color = "Color Settings";
	public static final String menu_title_Hier = "Hierarchical";
	public static final String menu_title_KMeans = "K-Means";
	public static final String menu_title_Help = "Help";
	public static final String menu_title_Stats = "Stats...";
	public static final String menu_title_About = "About...";
	public static final String menu_title_ShowLog = "Show Log...";
	public static final String menu_title_Docs = "Documentation...";
	public static final String menu_title_Feedback = "Send Feedback...";
	public static final String menu_title_NewWindow = "New Window...";
	public static final String menu_title_QuitWindow = "Close Window...";

	// MenuBar
	public static final String menubar_file = "File";
	public static final String menubar_view = "View";
	public static final String menubar_cluster = "Cluster";
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
	public static final String button_customLabels = "Load more labels...";
	public static final String button_home = "Home";
	public static final String button_searchLabels = "Search labels";
	public static final String button_showTrees = "Show trees";
	public static final String button_hideTrees = "Hide trees";
	public static final String button_toggleMatrixSize = "Change Matrix Size";
	public static final String button_loadNewFile = "Load New File";
	
	// Icon Names
	public static final String icon_home = "homeIcon";
	public static final String icon_zoomIn = "zoomInIcon";
	public static final String icon_zoomOut = "zoomOutIcon";
	public static final String icon_zoomAll = "fullscreenIcon";
	
	// Tooltips
	public static final String tooltip_searchRowCol = "Find row " +
			"or column elements";
	public static final String tooltip_showTrees = "Determine dendrogram" +
			"visibility";
	public static final String tooltip_xZoomIn = "Zoom in on rows";
	public static final String tooltip_xZoomOut = "Zoom out of rows";
	public static final String tooltip_yZoomIn = "Zoom in on columns";
	public static final String tooltip_yZoomOut = "Zoom out of columns";
	public static final String tooltip_home = "Zoom into the selected area";

	// RadioButton Texts
	public static final String rButton_dark = "Dark";
	public static final String rButton_light = "Light";

	// Labels
	public static final String label_ZoomColLabels = "Zoom to see column "
			+ "labels";
	public static final String label_ZoomRowLabels = "Zoom to see row labels";

	// Cluster Options
	public static final String cluster_DoNot = "Do not cluster";
	public static final String cluster_pearsonUn = "Pearson "
			+ "Correlation (uncentered)";
	public static final String cluster_pearsonCentered = "Pearson "
			+ "Correlation (centered)";
	public static final String cluster_absoluteUn = "Absolute "
			+ "Correlation (uncentered)";
	public static final String cluster_absoluteCentered = "Absolute "
			+ "Correlation (centered)";
	public static final String cluster_spearman = "Spearman Ranked Correlation";
	public static final String cluster_euclidean = "Euclidean Distance";
	public static final String cluster_cityBlock = "City Block Distance";
	
	// Cluster Info
	public static final String clusterInfo_Single_Similarity = "Joins the " +
			"closest two clusters by finding the most similar pair of points.";
	public static final String clusterInfo_Single_Type = "Long straggly, " +
			"chains, ellipsoidal.";
	public static final String clusterInfo_Single_Time = "O(N**3). " +
			"Implementation of SLINK (Sibson, 1972) will reduce " +
			"complexity to O(N**2).";
	public static final String clusterInfo_Single_Adv = "Theoretical " +
			"properties, efficient implementations, widely used. " +
			"No cluster centroid or representative required: " +
			"no need to recalculate similarity matrix.";
	public static final String clusterInfo_Single_DisAdv = "Unsuitable for " +
			"isolating spherical or poorly separated clusters.";
	public static final String clusterInfo_Ready = "Ready to cluster!";
	
	public static final String clusterInfo_Compl_Similarity = "Joins the " +
			"most distant two clusters by finding the least similar pair " +
			"of points.";
	public static final String clusterInfo_Compl_Type = "All entries in a " +
			"cluster are linked to one another within some minimum " +
			"similarity. Small, tightly bound clusters.";
	public static final String clusterInfo_Compl_Time = "O(N**3).";
	public static final String clusterInfo_Compl_Adv = "Good results from " +
			"(Voorhees) comparative studies.";
	public static final String clusterInfo_Compl_DisAdv = "Difficult to " +
			"apply to large data sets due to computational complexity.";
	
	public static final String clusterInfo_Avg_Similarity = "Use the average " +
			"value of the pairwise links within a cluster, based upon all " +
			"objects in the cluster.";
	public static final String clusterInfo_Avg_Type = "Intermediate in " +
			"tightness between single link and complete link.";
	public static final String clusterInfo_Avg_Time = "O(N**2).";
	public static final String clusterInfo_Avg_Adv = "Ranked well in " +
			"evaluation studies.";
	public static final String clusterInfo_Avg_DisAdv = "Expensive for " +
			"large collections.";
	
	// Cluster Info - Tip
	public static final String clusterTip_Memory = "Memory tip.";
	public static final String clusterTip_completed = "The file has been saved "
			+ "in the original directory.";
	public static final String clusterTip_filePath = "File Path: ";
	
	// Cluster Error - Hints
	public static final String clusterError_invalid = "Please pick valid "
			+ "cluster options to begin clustering!";

	// Frame Title - Testing
	public static final String test_title_FontSelector = "Font Settings Test";
}
