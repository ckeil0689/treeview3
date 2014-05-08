/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: DendroView.java,v $
 * $Revision: 1.7 $
 * $Date: 2009-03-23 02:46:51 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by 
 * Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular, 
 *
 * 1) If you modify a source file, make a comment in it containing your name 
 * and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the 
 * Java TreeView maintainers at alok@genome.stanford.edu when they make a 
 * useful addition. It would be nice if significant contributions could 
 * be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER 
 */
package Cluster;

import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.MainProgramArgs;
import edu.stanford.genetics.treeview.StringRes;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
import edu.stanford.genetics.treeview.model.TVModel;

//Explicitly imported because error (unclear TVModel reference) was thrown

/**
 * This class exists to internalize the clustering process directly into
 * TreeView. It provides a GUI which is called from the slightly adjusted
 * original Java TreeView menubar. The GUI content is loaded into the program in
 * a similar manner as the DendroView class.
 * 
 * @author CKeil <ckeil@princeton.edu>
 * @version 0.1
 */
public class ClusterView extends JPanel implements MainPanel {

	private static final long serialVersionUID = 1L;

	// Instance
	private TreeViewFrame tvFrame;
	protected TVModel dataModel;
	protected Preferences root;
	protected ClusterInfoFactory clusterInfo;

	// Various GUI elements
	private JScrollPane scrollPane;
	private JPanel mainPanel;
	private JPanel optionsPanel;
	private JPanel buttonPanel;
	private JPanel similarityPanel;
	private JPanel linkagePanel;
	private JPanel kMeansPanel;
	private JPanel loadPanel;
	private JPanel infoPanel;

	private JComboBox geneCombo;
	private JComboBox arrayCombo;
	private JComboBox clusterType;
	private JComboBox clusterChoice;

	private JButton cluster_button;
	private JButton cancel_button;

	private JSpinner enterRC;
	private JSpinner enterCC;
	private JSpinner enterRIt;
	private JSpinner enterCIt;

	private JLabel loadLabel;
	private JProgressBar pBar;

	private final String[] clusterMethods = { "Single Linkage",
			"Average Linkage", "Complete Linkage" };

	private final String clusterTypeID;

	/**
	 * Chained constructor for the ClusterView object note this will reuse any
	 * existing MainView subnode of the documentconfig.
	 * 
	 * @param cVModel
	 *            model this ClusterView is to represent
	 * @param vFrame
	 *            parent ViewFrame of DendroView
	 */
	public ClusterView(final TreeViewFrame tvFrame, final String clusterTypeID) {

		this(null, tvFrame, StringRes.view_title_Cluster, clusterTypeID);
	}

	/**
	 * Second chained constructor
	 * 
	 * @param cVModel
	 * @param root
	 * @param vFrame
	 */
	public ClusterView(final Preferences root, final TreeViewFrame tvFrame,
			final String clusterTypeID) {

		this(root, tvFrame, StringRes.view_title_Cluster, clusterTypeID);
	}

	/**
	 * Constructor for the ClusterView object which binds to an explicit
	 * confignode
	 * 
	 * @param dataModel
	 *            model this DendroView is to represent
	 * @param root
	 *            Confignode to which to bind this DendroView
	 * @param vFrame
	 *            parent ViewFrame of ClusterView
	 * @param name
	 *            name of this view.
	 */
	public ClusterView(final Preferences root, final TreeViewFrame tvFrame,
			final String name, final String clusterTypeID) {

		super.setName(name);

		this.tvFrame = tvFrame;
		this.dataModel = (TVModel) tvFrame.getDataModel();
		this.clusterTypeID = clusterTypeID;
		this.clusterInfo = new ClusterInfoFactory();
		
		// Set layout for initial window
		setupLayout();
	}

	public void setupLayout() {

		removeAll();
		setLayout(new MigLayout("ins 0"));

		// Setting up the interactive components so they can be added
		// to the layout.
		setupInteractiveComponents();

		setupInnerPanels();
		setOptionsPanel();
		setMainPanel();

		// make mainpanel scrollable by adding it to scrollpane
		scrollPane = new JScrollPane(mainPanel,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setOpaque(false);

		// Add the scrollPane to ClusterView2 Panel
		add(scrollPane, "grow, push");

		revalidate();
		repaint();
	}

	/**
	 * Sets the JPanels to null because JVM does not garbage-collect Swing
	 * components unless the parent frame is disposed.
	 */
	public void reset() {

		scrollPane = null;
		mainPanel = null;
		optionsPanel = null;
		buttonPanel = null;
		similarityPanel = null;
		linkagePanel = null;
		kMeansPanel = null;
		loadPanel = null;

		setupLayout();
	}

	/**
	 * Revalidates and repaints the mainPanel.
	 */
	@Override
	public void refresh() {

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	/**
	 * Prepares the buttons to be used in the layout.
	 */
	public void setupInteractiveComponents() {

		// Button to begin Clustering
		cluster_button = GUIParams.setButtonLayout(StringRes.button_Cluster,
				null);

		// Button to cancel worker thread in the controller
		cancel_button = GUIParams
				.setButtonLayout(StringRes.button_Cancel, null);

		// Label for cluster process
		loadLabel = new JLabel();
		loadLabel.setFont(GUIParams.FONTS);
		loadLabel.setForeground(GUIParams.TEXT);
		// ProgressBar for clustering process
		pBar = GUIParams.setPBarLayout();

		// ComboBox to choose cluster method
		final String[] clusterNames = { StringRes.menu_title_Hier,
				StringRes.menu_title_KMeans };

		clusterType = GUIParams.setComboLayout(clusterNames);

		clusterType.setSelectedIndex(Arrays.asList(clusterNames).indexOf(
				clusterTypeID));

		final String[] measurements = { "Do Not Cluster",
				"Pearson Correlation (uncentered)",
				"Pearson Correlation (centered)",
				"Absolute Correlation (uncentered)",
				"Absolute Correlation (centered)",
				"Spearman Ranked Correlation", "Euclidean Distance",
				"City Block Distance" };

		// Drop-down menu for row selection
		geneCombo = GUIParams.setComboLayout(measurements);

		// Drop-down menu for column selection
		arrayCombo = GUIParams.setComboLayout(measurements);

		// Linkage choice drop-down menu
		clusterChoice = GUIParams.setComboLayout(clusterMethods);

		// Spinners for K-Means
		enterRC = setupSpinner();
		enterCC = setupSpinner();
		enterRIt = setupSpinner();
		enterCIt = setupSpinner();
	}

	// Layout setups for some Swing elements
	/**
	 * Sets up general elements of the mainPanel
	 */
	public void setMainPanel() {

		// Create background panel
		mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout("ins 0"));
		mainPanel.setBackground(GUIParams.BG_COLOR);

		// Panel for the Buttons
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new MigLayout());
		buttonPanel.setOpaque(false);
		buttonPanel.add(cluster_button, "alignx 50%, pushx");

		mainPanel.add(clusterType, "alignx 50%, push, h 5%, span, wrap");
		mainPanel.add(optionsPanel, "pushx, alignx 50%, "
				+ "w 70%, h 70%, wrap");
		mainPanel.add(buttonPanel, "pushx, alignx 50%, h 15%");
	}

	/**
	 * Sets up general elements of the optionsPanel
	 */
	public void setOptionsPanel() {

		// Background Panel for the Cluster Options
		optionsPanel = new JPanel();
		optionsPanel.setLayout(new MigLayout());
		optionsPanel.setBorder(BorderFactory.createLineBorder(
				GUIParams.BORDERS, EtchedBorder.LOWERED));
		optionsPanel.setOpaque(false);

		// ProgressBar Component
		loadPanel = new JPanel();
		loadPanel.setLayout(new MigLayout());
		loadPanel.setOpaque(false);
		
		// Cluster Info Panel
		infoPanel = clusterInfo.makeMethodInfoPanel(
				clusterChoice.getSelectedIndex());

		optionsPanel.add(similarityPanel, "pushx, alignx 50%, h 20%, wrap");

		if (clusterType.getSelectedIndex() == 0) {
			optionsPanel.add(linkagePanel, "pushx, alignx 50%, h 20%, wrap");

		} else {
			optionsPanel.add(kMeansPanel, "pushx, alignx 50%, h 20%, wrap");
		}
		
		optionsPanel.add(infoPanel, "pushx, w 90%, alignx 50%, span, wrap");
	}

	/**
	 * Sets up the layout of the inner panels which are part of OptionPanel.
	 */
	public void setupInnerPanels() {

		// Component for similarity measure options
		similarityPanel = new JPanel();
		similarityPanel.setLayout(new MigLayout());
		similarityPanel.setOpaque(false);

		final JLabel similarity = new JLabel("Similarity Metric");
		similarity.setFont(GUIParams.FONTL);
		similarity.setForeground(GUIParams.MAIN);

		final JLabel rowLabel = new JLabel("Rows: ");
		rowLabel.setFont(GUIParams.FONTS);
		rowLabel.setForeground(GUIParams.TEXT);

		final JLabel colLabel = new JLabel("Columns: ");
		colLabel.setFont(GUIParams.FONTS);
		colLabel.setForeground(GUIParams.TEXT);

		similarityPanel.add(similarity, "pushx, alignx 50%, span, wrap");
		similarityPanel.add(rowLabel);
		similarityPanel.add(geneCombo, "w 40%, wrap");
		similarityPanel.add(colLabel);
		similarityPanel.add(arrayCombo, "w 40%");

		// Component for linkage choices
		linkagePanel = new JPanel();
		linkagePanel.setLayout(new MigLayout());
		linkagePanel.setOpaque(false);

		// Label
		final JLabel method = new JLabel("Linkage Method");
		method.setFont(GUIParams.FONTL);
		method.setForeground(GUIParams.MAIN);

		// Cluster Options
		linkagePanel.add(method, "pushx, alignx 50%, wrap");
		linkagePanel.add(clusterChoice, "pushx, alignx 50%, span");

		// Component for K-Means options
		kMeansPanel = new JPanel();
		kMeansPanel.setLayout(new MigLayout());
		kMeansPanel.setOpaque(false);

		final JLabel kMeans = new JLabel("K-Means Options");
		kMeans.setFont(GUIParams.FONTL);
		kMeans.setForeground(GUIParams.MAIN);

		final JLabel clusters = new JLabel("Clusters: ");
		clusters.setFont(GUIParams.FONTS);
		clusters.setForeground(GUIParams.TEXT);

		final JLabel its = new JLabel("Iterations: ");
		its.setFont(GUIParams.FONTS);
		its.setForeground(GUIParams.TEXT);

		kMeansPanel.add(kMeans, "span, wrap");

		kMeansPanel.add(clusters, "w 10%, h 15%");
		kMeansPanel.add(enterRC, "w 5%");
		kMeansPanel.add(enterCC, "w 5%, wrap");

		kMeansPanel.add(its, "w 10%, h 15%");
		kMeansPanel.add(enterRIt, "w 5%");
		kMeansPanel.add(enterCIt, "w 5%");
	}

	/**
	 * Method to setup the look of an editable TextField
	 * 
	 * @return
	 */
	public JSpinner setupSpinner() {

		final SpinnerNumberModel amountChoice = new SpinnerNumberModel(0, 0,
				5000, 1);
		final JSpinner jft = new JSpinner(amountChoice);

		jft.setFont(GUIParams.FONTS);

		return jft;
	}

	/**
	 * Get the mainPanel for reference
	 */
	public JPanel getMainPanel() {

		return mainPanel;
	}

	/**
	 * Get the optionsPanel for reference
	 */
	public JPanel getOptionsPanel() {

		return optionsPanel;
	}

	/**
	 * Get the buttonPanel for reference
	 */
	public JPanel getButtonPanel() {

		return buttonPanel;
	}

	/**
	 * Get the similarity measure choice for row clustering
	 * 
	 * @return
	 */
	public JComboBox getGeneCombo() {

		return geneCombo;
	}

	/**
	 * Get the similarity measure choice for column clustering
	 * 
	 * @return
	 */
	public JComboBox getArrayCombo() {

		return arrayCombo;
	}

	/** Getter for viewFrame */
	public TreeViewFrame getViewFrame() {

		return tvFrame;
	}

	/**
	 * Getter for DataModel
	 */
	protected DataModel getDataModel() {

		return this.dataModel;
	}

	/**
	 * Getter for root
	 */
	@Override
	public Preferences getConfigNode() {

		return root;
	}

	// Setters
	/** Setter for viewFrame */
	public void setViewFrame(final TreeViewFrame viewFrame) {

		this.tvFrame = viewFrame;
	}

	/**
	 * Setter for dataModel
	 * 
	 * */
	protected void setDataModel(final TVModel dataModel) {

		this.dataModel = dataModel;
	}

	/**
	 * Setter for root - may not work properly
	 * 
	 * @param root
	 */
	public void setConfigNode(final Preferences root) {

		this.root = root;
	}

	// Implementing methods to connect with Controller
	/**
	 * Adds a listener to the combobox for choosing the clustering type.
	 * (K-Means vs. Hierarchical).
	 * 
	 * @param switcher
	 */
	public void addClusterMenuListener(final ActionListener clusterMenu) {

		clusterType.addActionListener(clusterMenu);
	}

	/**
	 * Resets the view to display the appropriate options for the selected
	 * cluster method.
	 */
	public void setupClusterMenu(final boolean hierarchical) {

		mainPanel.removeAll();
		optionsPanel.removeAll();

		setOptionsPanel();
		setMainPanel();

		scrollPane.setViewportView(mainPanel);

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	/**
	 * Adds a listener to cluster_button to register user interaction and notify
	 * ClusterController to call the performCluster() method.
	 * 
	 * @param cluster
	 */
	public void addClusterListener(final ActionListener cluster) {

		cluster_button.addActionListener(cluster);
	}

	/**
	 * Performs the clustering action when the cluster_button is clicked, based
	 * on the user choices.
	 */
	public void displayClusterProcess(final String choice,
			final String choice2, final String linkageMethod) {

		loadPanel.removeAll();
		buttonPanel.remove(cluster_button);

		setLoadPanel(choice, choice2);

		optionsPanel.add(loadPanel, "pushx, w 90%, alignx 50%, span, wrap");
		optionsPanel.add(infoPanel, "pushx, w 90%, alignx 50%, span, wrap");
		
		buttonPanel.add(cancel_button, "pushx, alignx 50%");
		mainPanel.add(buttonPanel, "pushx, alignx 50%, " + "h 15%");

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	/**
	 * Adds a listener to cancel_button to register user interaction and notify
	 * ClusterController to call the cancel() method.
	 * 
	 * @param cancel
	 */
	public void addCancelListener(final ActionListener cancel) {

		cancel_button.addActionListener(cancel);
	}
	
	/**
	 * Listener for the clusterChoice JComboBox in order to change the display
	 * of information about cluster methods when the selection is changed.
	 * @param l
	 */
	public void addClusterChoiceListener(final ActionListener l) {
		
		clusterChoice.addActionListener(l);
	}

	/**
	 * Attempts to cancel the worker thread when the cancel button is clicked.
	 * Also resets the view accordingly.
	 */
	public void cancel() {

		loadPanel.removeAll();
		optionsPanel.remove(loadPanel);
		buttonPanel.remove(cancel_button);

		buttonPanel.add(cluster_button);
		cluster_button.setEnabled(true);

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	/**
	 * Adds a load progress bar to loadpanel if a similarity measure has been
	 * selected.
	 * 
	 * @param choice
	 * @param choice2
	 */
	public void setLoadPanel(final String choice, final String choice2) {

		if (!choice.contentEquals("Do Not Cluster")
				|| !choice2.contentEquals("Do Not Cluster")) {
			loadPanel.add(loadLabel, "pushx, alignx 50%, wrap");
			loadPanel.add(pBar, "pushx, w 90%, alignx 50%, wrap");
		}
	}

	/**
	 * Displays an error message if not at least one similarity measure has been
	 * selected, since the data cannot be clustered in that case.
	 */
	public void showError(final boolean hierarchical) {

		loadPanel.removeAll();

		final String hint = "Woah, that's too quick!";
		String hint2 = "";
		String hint3 = "";

		if (hierarchical) {
			hint2 = "Select at least one similarity metric for "
					+ "either rows or columns to begin clustering!";

		} else {
			hint2 = "Select at least one similarity metric for "
					+ "either rows or columns to begin clustering!";

			hint3 = "The amount of clusters and iterations must "
					+ "be greater than 0.";
		}

		final TextDisplay error1 = new TextDisplay(hint);
		error1.setForeground(GUIParams.RED1);

		final TextDisplay error2 = new TextDisplay(hint2);
		final TextDisplay error3 = new TextDisplay(hint3);

		loadPanel.add(error1, "pushx, alignx 50%, span, wrap");
		loadPanel.add(error2, "span, wrap");
		loadPanel.add(error3, "span");

		optionsPanel.add(loadPanel, "alignx 50%, pushx, w 90%, " + "span");

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	/**
	 * Changes of the layout when the worker thread in controller has completed
	 * its calculations. (worker calls done())
	 */
	public void displayCompleted(final String finalFilePath) {

		buttonPanel.remove(cancel_button);

		final TextDisplay status1 = new TextDisplay("The file has been saved "
				+ "in the original directory.");

		final TextDisplay status2 = new TextDisplay("File Path: "
				+ finalFilePath);

		buttonPanel.add(cluster_button, "pushx, alignx 50%");

		loadPanel.add(status1, "pushx, alignx 50%, span, wrap");
		loadPanel.add(status2, "pushx, alignx 50%, span, wrap");

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	/**
	 * Returns the selection of the cluster method as a String.
	 * 
	 * @return
	 */
	public String getClusterMethod() {

		return (String) clusterType.getSelectedItem();
	}

	/**
	 * Returns the selected similarity measure for the rows as a String.
	 * 
	 * @return String choice
	 */
	public String getRowSimilarity() {

		return (String) geneCombo.getSelectedItem();
	}

	/**
	 * Returns the selected similarity measure for the columns as a String.
	 * 
	 * @return String choice
	 */
	public String getColSimilarity() {

		return (String) arrayCombo.getSelectedItem();
	}

	/**
	 * Returns the selected linkage method as a String.
	 * 
	 * @return String choice
	 */
	public String getLinkageMethod() {

		return (String) clusterChoice.getSelectedItem();
	}

	/**
	 * Returns the current values the user defined for the different spinners.
	 * 
	 * @return
	 */
	public Integer[] getSpinnerValues() {

		final int spinners = 4;
		final Integer[] spinnerValues = new Integer[spinners];

		spinnerValues[0] = (Integer) enterRC.getValue();
		spinnerValues[1] = (Integer) enterRIt.getValue();
		spinnerValues[2] = (Integer) enterCC.getValue();
		spinnerValues[3] = (Integer) enterCIt.getValue();

		return spinnerValues;
	}

	/**
	 * Defines the text to be displayed by loadLabel in ClusterView.
	 * 
	 * @param text
	 */
	public void setLoadText(final String text) {

		loadLabel.setText(text);
	}

	/**
	 * Sets the maximum for the JProgressBar.
	 * 
	 * @param max
	 */
	public void setPBarMax(final int max) {

		pBar.setMaximum(max);
	}

	/**
	 * Updates the value of the JProgressBar in ClusterView.
	 * 
	 * @param i
	 */
	public void updatePBar(final int i) {

		pBar.setValue(i);
	}

	// Empty methods
	@Override
	public void populateSettingsMenu(final TreeviewMenuBarI menubar) {
	}

	@Override
	public void populateAnalysisMenu(final TreeviewMenuBarI menubar) {
	}

	@Override
	public void populateExportMenu(final TreeviewMenuBarI menubar) {
	}

	@Override
	public void scrollToGene(final int i) {
	}

	@Override
	public void scrollToArray(final int i) {
	}

	@Override
	public ImageIcon getIcon() {

		return null;
	}

	@Override
	public void export(final MainProgramArgs args) throws ExportException {
	}

	@Override
	public void syncConfig() {
	}

}
