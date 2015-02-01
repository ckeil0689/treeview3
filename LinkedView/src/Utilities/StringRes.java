package Utilities;

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

	/* Global Application Fields */
	/** Version of application */
	public final static String versionTag = "3.0beta";

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

	// JDialog Titles
	public static final String dlg_Cluster = "Cluster View";
	public static final String dlg_search = "Search Labels";
	public static final String dlg_loadLabels = "Loading Labels";
	public static final String dlg_prefs = "Preferences";
	public static final String dlg_exitConfirm = "Confirm Exit";
	public static final String dlg_about = "About";

	// Main Words
	public static final String main_rows = "Rows";
	public static final String main_cols = "Columns";

	// Title Screen
	public static final String title_Hello = "Hello, how are you Gentlepeople?";
	public static final String title_Welcome = "Welcome to ";

	// Loading
	public static final String load_OneSec = "One moment, please.";
	public static final String load_active = "Loading your data!";
	public static final String load_Ohoh = "Oh oh!";
	public static final String load_Error = "Looks like we ran into the "
			+ "following issue: ";

	// Menu Titles
	public static final String menu_Open = "Open...";
	public static final String menu_OpenRecent = "Open Recent...";
	public static final String menu_EditRecent = "Edit Recent Files";
	public static final String menu_Save = "Save...";
	public static final String menu_SaveAs = "Save As...";
	public static final String menu_Font = "Font...";
	public static final String menu_URL = "URL...";
	public static final String menu_Prefs = "Preferences";
	public static final String menu_ResetPrefs = "Reset Preferences";
	public static final String menu_RowAndCol = "Row and Column Labels...";
	public static final String menu_Color = "Color Settings...";
	public static final String menu_Hier = "Hierarchical";
	public static final String menu_KMeans = "K-Means";
	public static final String menu_Stats = "Stats...";
	public static final String menu_Shortcuts = "Keyboard shortcuts...";
	public static final String menu_About = "About...";
	public static final String menu_ShowLog = "Show Log...";
	// There's currently no documentation page - don't need it for the first
	// release
	// public static final String menu_Docs = "Documentation...";
	public static final String menu_Feedback = "Send Feedback...";
	public static final String menu_NewWindow = "New Window";
	public static final String menu_QuitWindow = "Close Window";

	// MenuBar
	public static final String mbar_File = "File";
	public static final String mbar_View = "View";
	public static final String mbar_Cluster = "Cluster";
	public static final String mbar_Help = "Help";

	// Preferences API
	// Nodes
	public static final String pnode_Preferences = "Preferences";
	public static final String pnode_DendroView = "DendroView";
	public static final String pnode_File = "File";
	public static final String pnode_TVFrame = "TreeViewFrame";
	public static final String pnode_FileMRU = "FileMRU";

	// Button Texts
	public static final String btn_OK = "OK";
	public static final String btn_Cancel = "Cancel";
	public static final String btn_Cluster = "Cluster";
	public static final String btn_CustomLabels = "Load more labels...";
	public static final String btn_SearchLabels = "Search...";
	public static final String btn_ShowTrees = "Show trees";
	public static final String btn_LoadNewFile = "Load New File...";

	// Icon Names
	public static final String icon_home = "homeIcon";
	public static final String icon_zoomIn = "zoomInIcon";
	public static final String icon_zoomOut = "zoomOutIcon";
	public static final String icon_zoomAll = "fullscreenIcon";
	public static final String icon_fullZoomIn = "full_zoom_in_icon.png";
	public static final String icon_fullZoomOut = "full_zoom_out_icon.png";

	// Tooltips
	public static final String tt_searchRowCol = "Find row or column elements";
	public static final String tt_showTrees = "Determine dendrogram visibility";
	public static final String tt_xZoomIn = "Zoom in on rows";
	public static final String tt_xyZoomIn = "Zoom in both axes";
	public static final String tt_xZoomOut = "Zoom out of rows";
	public static final String tt_xyZoomOut = "Zoom out both axes";
	public static final String tt_yZoomIn = "Zoom in on columns";
	public static final String tt_yZoomOut = "Zoom out of columns";
	public static final String tt_home = "Zoom into the selected area";

	// Labels
	public static final String lbl_ZoomColLabels = "Zoom to see column labels";
	public static final String lbl_ZoomRowLabels = "Zoom to see row labels";

	/* Distance Measures */
	public static final String cluster_DoNot = "Do not cluster";
	public static final String cluster_pearsonUn = "Pearson Correlation "
			+ "(uncentered)";
	public static final String cluster_pearsonCtrd = "Pearson Correlation "
			+ "(centered)";
	public static final String cluster_absCorrUn = "Absolute Correlation "
			+ "(uncentered)";
	public static final String cluster_absCorrCtrd = "Absolute Correlation "
			+ "(centered)";
	public static final String cluster_spearman = "Spearman Ranked Correlation";
	public static final String cluster_euclidean = "Euclidean Distance";
	public static final String cluster_cityBlock = "City Block Distance";

	/* Linkage Methods */
	public static final String cluster_link_Avg = "Average Linkage";

	/* Linkage Method & clustering infos */
	/* Single Linkage */
	public static final String clusterInfo_Single_Similarity = "Joins the "
			+ "closest two clusters by finding the most similar pair of points.";

	public static final String clusterInfo_Single_Type = "Long straggly, "
			+ "chains, ellipsoidal.";

	public static final String clusterInfo_Single_Time = "O(N log N) to "
			+ "O(N**5). Implementation of SLINK (Sibson, 1972) will reduce "
			+ "complexity to O(N**2).";

	public static final String clusterInfo_Single_Adv = "Theoretical "
			+ "properties, efficient implementations, widely used. "
			+ "No cluster centroid or representative required: "
			+ "no need to recalculate similarity matrix.";

	public static final String clusterInfo_Single_DisAdv = "Unsuitable for "
			+ "isolating spherical or poorly separated clusters.";

	public static final String clusterInfo_Ready = "Ready to cluster!";

	/* Complete Linkage */
	public static final String clusterInfo_Compl_Similarity = "Joins the "
			+ "most distant two clusters by finding the least similar pair "
			+ "of points.";

	public static final String clusterInfo_Compl_Type = "All entries in a "
			+ "cluster are linked to one another within some minimum "
			+ "similarity. Small, tightly bound clusters.";

	public static final String clusterInfo_Compl_Time = "O(N**3).";

	public static final String clusterInfo_Compl_Adv = "Good results from "
			+ "(Voorhees) comparative studies.";

	public static final String clusterInfo_Compl_DisAdv = "Difficult to "
			+ "apply to large data sets due to computational complexity.";

	/* Average Linkage */
	public static final String clusterInfo_Avg_Similarity = "Use the average "
			+ "value of the pairwise links within a cluster, based upon all "
			+ "objects in the cluster.";

	public static final String clusterInfo_Avg_Type = "Intermediate in "
			+ "tightness between single link and complete link.";

	public static final String clusterInfo_Avg_Time = "O(N**2).";

	public static final String clusterInfo_Avg_Adv = "Ranked well in "
			+ "evaluation studies.";

	public static final String clusterInfo_Avg_DisAdv = "Expensive for "
			+ "large collections.";

	/* k-means */
	public static final String clusterInfo_KMeans = "Splits the data into "
			+ "k clusters by initially choosing k random centroids (means) "
			+ "from the distance matrix rows or columns. The rest of the "
			+ "rows or columns is then each assigned to the group with the "
			+ "closest centroid. ";

	public static final String clusterInfo_KMeans_Adv = "Faster than "
			+ "hierarchical clustering, if k is small; "
			+ "Produces tighter clusters than hierarchical clustering.";

	public static final String clusterInfo_KMeans_DisAdv = "Fixed number of "
			+ "clusters: User has to decide what value k should be; "
			+ "Unsuitable for non-globular clusters; "
			+ "Randomly selected seed clusters generate different clusters.";

	// Cluster Info - Tip
	public static final String clusterTip_Memory = "Memory tip.";
	public static final String clusterTip_completed = "The file has been saved "
			+ "in the original directory.";
	public static final String clusterTip_filePath = "File Path: ";

	// Cluster Error - Hints
	public static final String clusterError_invalid = "Clustering parameters"
			+ " are missing or incorrect. Check the input again!";
	public static final String clusterError_notLoaded = "DataModel not "
			+ "properly loaded.";

	// Frame Title - Testing
	public static final String test_title_FontSelector = "Font Settings Test";
}
