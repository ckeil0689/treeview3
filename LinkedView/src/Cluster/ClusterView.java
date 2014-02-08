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
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.MainProgramArgs;
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
	protected DataModel dataModel;
	protected ConfigNode root;

	private TreeViewFrame viewFrame;
	//private boolean hierarchical;

	// Various GUI elements
	private JScrollPane scrollPane;
	private JPanel mainPanel;
	private JPanel optionsPanel;
	private JPanel buttonPanel;
	private JPanel similarityPanel;
	private JPanel linkagePanel;
	private JPanel kMeansPanel;
	private JPanel loadPanel;
	
	private JComboBox geneCombo;
	private JComboBox arrayCombo;
	private JComboBox clusterType;
	private JComboBox clusterChoice;

	private JButton cluster_button;
	private JButton cancel_button;
	private JButton dendro_button;
	
	private JSpinner enterRC;
	private JSpinner enterCC;
	private JSpinner enterRIt;
	private JSpinner enterCIt;
	
	private JProgressBar pBar;
	private JProgressBar pBar2;
	private JProgressBar pBar3;
	private JProgressBar pBar4;
	
	private ClickableIcon infoIcon;

	private final String[] clusterMethods = { "Single Linkage",
			"Centroid Linkage", "Average Linkage", "Complete Linkage" };

	/**
	 * Chained constructor for the ClusterView object note this will reuse any
	 * existing MainView subnode of the documentconfig.
	 * 
	 * @param cVModel
	 *            model this ClusterView is to represent
	 * @param vFrame
	 *            parent ViewFrame of DendroView
	 */
	public ClusterView(final TreeViewFrame vFrame) {

		this(null, vFrame, "Cluster View");
	}

	/**
	 * Second chained constructor
	 * 
	 * @param cVModel
	 * @param root
	 * @param vFrame
	 */
	public ClusterView(final ConfigNode root, final TreeViewFrame vFrame) {

		this(root, vFrame, "Cluster View");
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
	public ClusterView (final ConfigNode root, final TreeViewFrame vFrame, 
			final String name) {

		super.setName(name);

		this.viewFrame = vFrame;
		this.dataModel = viewFrame.getDataModel();

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
	public void refresh() {
		
		mainPanel.revalidate();
		mainPanel.repaint();
	}
	
	/**
	 * Prepares the buttons to be used in the layout.
	 */
	public void setupInteractiveComponents() {
		
		// Button to begin Clustering
		cluster_button = GUIParams.setButtonLayout("Cluster", null);
		
		// Button to show Heat Map
		dendro_button = GUIParams.setButtonLayout("Clustergram", 
				"forwardIcon");
		dendro_button.setEnabled(false);
		
		// Button to cancel worker thread in the controller
		cancel_button = GUIParams.setButtonLayout("Cancel", null);
		
		// ProgressBars for clustering process
		pBar = GUIParams.setPBarLayout();

		pBar2 = GUIParams.setPBarLayout();

		pBar3 = GUIParams.setPBarLayout();

		pBar4 = GUIParams.setPBarLayout();
		
		// ComboBox to choose cluster method
		String[] clusterNames = { "Hierarchical Clustering",
		"K-Means" };
		
		clusterType = GUIParams.setComboLayout(clusterNames);
		
		clusterType.setSelectedIndex(0);
		
		String[] measurements = { "Do Not Cluster",
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
		
		// header
		JLabel head1 = new JLabel("Options");
		head1.setFont(GUIParams.FONTL);
		head1.setForeground(GUIParams.ELEMENT);
		
		// Panel for the Buttons
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new MigLayout());
		buttonPanel.setOpaque(false);
		buttonPanel.add(cluster_button, "alignx 50%, pushx");
		
		mainPanel.add(head1, "pushx, alignx 50%, h 15%, span, wrap");
		mainPanel.add(clusterType, "alignx 50%, push, h 5%, span, wrap");
		mainPanel.add(optionsPanel, "pushx, alignx 50%, "
				+ "w 70%, h 60%, wrap");
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
		loadPanel.setBorder(BorderFactory.createLineBorder(GUIParams.BORDERS,
				EtchedBorder.LOWERED));
		
		optionsPanel.add(similarityPanel, "pushy, h 20%, span, wrap");

		if (clusterType.getSelectedIndex() == 0) {
			optionsPanel.add(linkagePanel, "pushy, h 20%, span, wrap");

		} else {
			optionsPanel.add(kMeansPanel, "pushy, h 20%, span");
		}
	}
	
	/**
	 * Sets up the layout of the inner panels which are part of OptionPanel.
	 */
	public void setupInnerPanels() {
		
		// Component for similarity measure options
		similarityPanel = new JPanel();
		similarityPanel.setLayout(new MigLayout());
		similarityPanel.setOpaque(false);
		
		JLabel similarity = new JLabel("Similarity Metric");
		similarity.setFont(GUIParams.FONTL);
		similarity.setForeground(GUIParams.ELEMENT);

		// Labels
		JLabel head2 = new JLabel("Rows:");
		head2.setFont(GUIParams.FONTS);
		head2.setForeground(GUIParams.TEXT);

		JLabel head3 = new JLabel("Columns:");
		head3.setFont(GUIParams.FONTS);
		head3.setForeground(GUIParams.TEXT);
		
		similarityPanel.add(similarity, "w 100%, span, wrap");
		similarityPanel.add(head2, "h 10%, w 10%");
		similarityPanel.add(geneCombo, "w 40%, wrap");
		similarityPanel.add(head3, "h 10%, w 10%");
		similarityPanel.add(arrayCombo, "w 40%");
		
		// Component for linkage choices
		linkagePanel = new JPanel();
		linkagePanel.setLayout(new MigLayout());
		linkagePanel.setOpaque(false);

		// Label
		JLabel method = new JLabel("Linkage Method");
		method.setFont(GUIParams.FONTL);
		method.setForeground(GUIParams.ELEMENT);

		// Clickable Panel to call InfoFrame
		infoIcon = new ClickableIcon(viewFrame, GUIParams.QUESTIONICON);
		
		linkagePanel.add(method, "span, wrap");
		linkagePanel.add(clusterChoice, "w 40%");
		linkagePanel.add(infoIcon, "w 10%");
		
		// Component for K-Means options
		kMeansPanel = new JPanel();
		kMeansPanel.setLayout(new MigLayout());
		kMeansPanel.setOpaque(false);
		
		JLabel kMeans = new JLabel("K-Means Options");
		kMeans.setFont(GUIParams.FONTL);
		kMeans.setForeground(GUIParams.ELEMENT);

		JLabel clusters = new JLabel("Clusters: ");
		clusters.setFont(GUIParams.FONTS);
		clusters.setForeground(GUIParams.TEXT);

		JLabel its = new JLabel("Iterations: ");
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

		return viewFrame;
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
	public ConfigNode getConfigNode() {

		return root;
	}

	// Setters
	/** Setter for viewFrame */
	public void setViewFrame(final TreeViewFrame viewFrame) {

		this.viewFrame = viewFrame;
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
	public void setConfigNode(final ConfigNode root) {

		this.root = root;
	}
	
	// Implementing methods to connect with Controller
	/**
	 * Adds a listener to the combobox for choosing the clustering type.
	 * (K-Means vs. Hierarchical).
	 * @param switcher
	 */
	public void addClusterMenuListener(ActionListener clusterMenu) {
		
		clusterType.addActionListener(clusterMenu);
	}
	
	/**
	 * Resets the view to display the appropriate options for the selected
	 * cluster method.
	 */
	public void setupClusterMenu(boolean hierarchical) {
		
		mainPanel.removeAll();
		optionsPanel.removeAll();

		setOptionsPanel();
		setMainPanel();
		
		scrollPane.setViewportView(mainPanel);

		mainPanel.revalidate();
		mainPanel.repaint();
	}
	
	/**
	 * Adds a listener to dendro_button to register user interaction and
	 * notify ClusterController to call the visualizeData() method.
	 * @param visual
	 */
	public void addVisualizeListener(ActionListener visual) {
		
		dendro_button.addActionListener(visual);
	}
	
//	/**
//	 * Sets a new DendroView with the new data loaded into TVModel, displaying
//	 * an updated HeatMap. It should also close the ClusterViewFrame.
//	 */
//	public void visualizeData() {
//		
//		JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
//		
//		final FileSet fileSet = new FileSet(file.getName(), file
//				.getParent() + File.separator);
//
//		try {
//			viewFrame.loadFileSet(fileSet);
//
//		} catch (final LoadException e) {
//
//		}
//		viewFrame.setLoaded(true);
//		topFrame.dispose();
//	}
	
	/**
	 * Adds a listener to cluster_button to register user interaction and
	 * notify ClusterController to call the performCluster() method.
	 * @param cluster
	 */
	public void addClusterListener(ActionListener cluster) {
		
		cluster_button.addActionListener(cluster);
	}
	
	/**
	 * Performs the clustering action when the cluster_button is clicked,
	 * based on the user choices.
	 */
	public void displayClusterProcess(String choice, String choice2, 
			String linkageMethod) {
		

		loadPanel.removeAll();
		dendro_button.setEnabled(false);
		buttonPanel.remove(cluster_button);
			
		setLoadPanel(choice, choice2);

		optionsPanel.add(loadPanel, "pushx, w 90%, alignx 50%, " +
				"span, wrap");
		
		buttonPanel.add(cancel_button, "pushx, alignx 50%");
		buttonPanel.add(dendro_button, "pushx, alignx 50%");
		mainPanel.add(buttonPanel, "pushx, alignx 50%, "
				+ "h 15%");
		
		mainPanel.revalidate();
		mainPanel.repaint();
	}
	
	/**
	 * Adds a listener to cancel_button to register user interaction and
	 * notify ClusterController to call the cancel() method.
	 * @param cancel
	 */
	public void addCancelListener(ActionListener cancel) {
		
		cancel_button.addActionListener(cancel);
	}
	
	/**
	 * Attempts to cancel the worker thread when the cancel button is clicked.
	 * Also resets the view accordingly.
	 */
	public void cancel() {

		loadPanel.removeAll();
		optionsPanel.remove(loadPanel);
		buttonPanel.remove(cancel_button);
		buttonPanel.remove(dendro_button);
		cluster_button.setEnabled(true);
		
		viewFrame.setLoaded(false);
		viewFrame.setLoaded(true);
		
		mainPanel.revalidate();
		mainPanel.repaint();
	}
	
	public void setLoadPanel(String choice, String choice2) {
		
		if (!choice.contentEquals("Do Not Cluster")
				&& !choice2.contentEquals("Do Not Cluster")) {
			loadPanel.add(pBar, "pushx, w 90%, alignx 50%, wrap");
			loadPanel.add(pBar2, "pushx, w 90%, alignx 50%, wrap");
			loadPanel.add(pBar3, "pushx, w 90%, alignx 50%, wrap");
			loadPanel.add(pBar4, "pushx, w 90%, alignx 50%, wrap");

		} else if (!choice.contentEquals("Do Not Cluster")) {
			loadPanel.add(pBar, "pushx, w 90%, alignx 50%, wrap");
			loadPanel.add(pBar2, "pushx, w 90%, alignx 50%, wrap");

		} else if (!choice2.contentEquals("Do Not Cluster")) {
			loadPanel.add(pBar3, "pushx, w 90%, alignx 50%, wrap");
			loadPanel.add(pBar4, "pushx, w 90%, alignx 50%, wrap");
		}
	}
	
	/**
	 * Displays an error message if not at least one similarity measure has
	 * been selected, since the data cannot be clustered in that case.
	 */
	public void showError(boolean hierarchical) {
		
		loadPanel.removeAll();

		String hint = "Woah, that's too quick!";
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

		TextDisplay error1 = new TextDisplay(hint);
		error1.setForeground(GUIParams.RED1);
		
		TextDisplay error2 = new TextDisplay(hint2);
		TextDisplay error3 = new TextDisplay(hint3);

		loadPanel.add(error1, "pushx, w 90%, alignx 50%, span, wrap");
		loadPanel.add(error2, "w 90%, span, wrap");
		loadPanel.add(error3, "w 90%, span");
		
		optionsPanel.add(loadPanel, "alignx 50%, push, w 90%, " +
				"span");

		mainPanel.revalidate();
		mainPanel.repaint();
	}
	
	/**
	 * Changes of the layout when the worker thread in controller has completed 
	 * its calculations. (worker calls done())
	 */
	public void displayCompleted(String finalFilePath) {
		
		buttonPanel.remove(cancel_button);

		TextDisplay status1 = new TextDisplay("The file has been saved "
				+ "in the original directory.");

		TextDisplay status2 = new TextDisplay("File Path: " + finalFilePath);

		dendro_button.setEnabled(true);
		buttonPanel.add(cluster_button, "pushx, alignx 50%");
		buttonPanel.add(dendro_button, "pushx, alignx 50%");

		loadPanel.add(status1, "growx, pushx, wrap");
		loadPanel.add(status2, "growx, pushx, wrap");

		mainPanel.revalidate();
		mainPanel.repaint();
	}
	
	/**
	 * Returns the selection of the cluster method as a String.
	 * @return
	 */
	public String getClusterMethod() {
		
		return (String) clusterType.getSelectedItem();
	}
	
	/**
	 * Returns the selected similarity measure for the rows as a String.
	 * @return String choice
	 */
	public String getRowSimilarity() {
		
		return (String) geneCombo.getSelectedItem();
	}
	
	/**
	 * Returns the selected similarity measure for the columns as a String.
	 * @return String choice
	 */
	public String getColSimilarity() {
		
		return (String) arrayCombo.getSelectedItem();
	}
	
	/**
	 * Returns the selected linkage method as a String.
	 * @return String choice
	 */
	public String getLinkageMethod() {
		
		return (String) clusterChoice.getSelectedItem();
	}
	
	/**
	 * Returns the current values the user defined for the different spinners.
	 * @return
	 */
	public Integer[] getSpinnerValues() {
		
		int spinners = 4;
		Integer[] spinnerValues = new Integer[spinners];
		
		spinnerValues[0] = (Integer) enterRC.getValue();
		spinnerValues[1] = (Integer) enterRIt.getValue();
		spinnerValues[2] = (Integer) enterCC.getValue();
		spinnerValues[3] = (Integer) enterCIt.getValue();
		
		return spinnerValues;
	}
	
	/**
	 * Sets the maximum for the JProgressBar to be used.
	 * pBarNum specifies which of the 4 pBars in ClusterView is addressed.
	 * @param max
	 * @param pBarNum
	 */
	public void setPBarMax(int max, int pBarNum) {
		
		switch(pBarNum) {
		
		case 1:	pBar.setMaximum(max);
				break;
		case 2:	pBar2.setMaximum(max);
				break;
		case 3:	pBar3.setMaximum(max);
				break;
		case 4:	pBar4.setMaximum(max);
				break;
		}
	}
	
	/**
	 * Updates the value of the specified JProgressBar in ClusterView.
	 * @param i
	 * @param pBarNum
	 */
	public void updatePBar(int i, int pBarNum) {
		
		switch(pBarNum) {
		
		case 1:	pBar.setValue(i);
				break;
		case 2:	pBar2.setValue(i);
				break;
		case 3:	pBar3.setValue(i);
				break;
		case 4:	pBar4.setValue(i);
				break;
		}
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
