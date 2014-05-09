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
	public static final String button_customLabels = "Load custom labels";
	public static final String button_home = "Home";
	public static final String button_searchLabels = "Search labels";
	public static final String button_showTrees = "Show trees";
	public static final String button_hideTrees = "Hide trees";

	// RadioButton Texts
	public static final String rButton_dark = "Dark";
	public static final String rButton_light = "Light";

	// Labels
	public static final String label_ZoomColLabels = "Zoom to see column "
			+ "labels";
	public static final String label_ZoomRowLabels = "Zoom to see row labels";

	// Cluster Options
	public static final String cluster_DoNot = "Do not cluster";
	
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

	// Frame Title - Testing
	public static final String test_title_FontSelector = "Font Settings Test";
}
