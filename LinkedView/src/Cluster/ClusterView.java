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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import Utilities.GUIFactory;
import Utilities.StringRes;

/**
 * This class exists to internalize the clustering process directly into
 * TreeView. It provides a GUI which is called from the slightly adjusted
 * original Java TreeView menubar. The GUI content is loaded into the program in
 * a similar manner as the DendroView class.
 * 
 * @author CKeil <ckeil@princeton.edu>
 * @version 0.1
 */
public class ClusterView {

	// Instance
	protected Preferences root;

	// Various GUI elements
	private JPanel mainPanel;
	private JPanel optionsPanel;
	private JPanel buttonPanel;
	private JPanel loadPanel;

	private JComboBox<String> geneCombo;
	private JComboBox<String> arrayCombo;
	private JComboBox<String> clusterType;
	private JComboBox<String> clusterChoice;

	private JButton cluster_btn;
	private JButton cancel_button;

	private JSpinner enterRC;
	private JSpinner enterCC;
	private JSpinner enterRIt;
	private JSpinner enterCIt;

	private JLabel loadLabel;
	private JProgressBar pBar;

	private final String[] clusterMethods = { "Average Linkage", 
			"Single Linkage", "Complete Linkage" };

	private final String clusterTypeID;

	/**
	 * Chained constructor for the ClusterView object note this will reuse any
	 * existing MainView subnode of the documentconfig.
	 * 
	 * @param cVModel
	 *            model this ClusterView is to represent
	 */
	public ClusterView(final String clusterTypeID) {

		this(null, clusterTypeID);
	}

	/**
	 * Constructor for the ClusterView object which binds to an explicit
	 * confignode
	 * 
	 * @param root
	 *            Confignode to which to bind this DendroView
	 * @param name
	 *            name of this view.
	 */
	public ClusterView(final Preferences root, final String clusterTypeID) {

		this.clusterTypeID = clusterTypeID;
		
		// Setting up the interactive components with listeners
		setupInteractiveComponents();
	}
	
	public JPanel makeClusterPanel() {
		
		// Main background panel
		mainPanel = GUIFactory.createJPanel(false, false, null);
		
		// Background Panel for the Cluster Options
		optionsPanel = GUIFactory.createJPanel(false, true, null);
		
		// Panel for the Cluster/ Cancel buttons
		buttonPanel = GUIFactory.createJPanel(false, true, null);
		
		// ProgressBar Component
		loadPanel = GUIFactory.createJPanel(false, true, null);
		
		setupLayout();
		
		return mainPanel;
	}

	/**
	 * Sets up the layout for all panels and GUI components.
	 */
	public void setupLayout() {

		mainPanel.removeAll();

		// Add all components to panels
		setLoadPanel();
		setOptionsPanel();
		setMainPanel();

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	/**
	 * Prepares the buttons to be used in the layout.
	 */
	public void setupInteractiveComponents() {

		// Button to begin Clustering
		cluster_btn = GUIFactory.createBtn(StringRes.btn_Cluster);

		// Button to cancel worker thread in the controller
		cancel_button = GUIFactory.createBtn(StringRes.btn_Cancel);

		// Label for cluster process
		loadLabel = GUIFactory.createLabel(StringRes.clusterInfo_Ready, 
				GUIFactory.FONTS);
		
		// ProgressBar for clustering process
		pBar = GUIFactory.createPBar();
		pBar.setEnabled(false); //initially disabled

		// ComboBox to choose cluster method
		final String[] clusterNames = { StringRes.menu_title_Hier,
				StringRes.menu_title_KMeans };

		clusterType = GUIFactory.createComboBox(clusterNames);

		clusterType.setSelectedIndex(Arrays.asList(clusterNames).indexOf(
				clusterTypeID));

		final String[] measurements = { StringRes.cluster_DoNot,
				StringRes.cluster_pearsonUn,
				StringRes.cluster_pearsonCentered,
				StringRes.cluster_absoluteUn,
				StringRes.cluster_absoluteCentered,
				StringRes.cluster_spearman, StringRes.cluster_euclidean,
				StringRes.cluster_cityBlock };

		// Drop-down menu for row selection
		geneCombo = GUIFactory.createComboBox(measurements);
		geneCombo.setSelectedIndex(1);

		// Drop-down menu for column selection
		arrayCombo = GUIFactory.createComboBox(measurements);
		arrayCombo.setSelectedIndex(1);

		// Linkage choice drop-down menu
		clusterChoice = GUIFactory.createComboBox(clusterMethods);

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

		// Panel for the Buttons
		buttonPanel.removeAll();
		buttonPanel.add(cluster_btn, "pushx, alignx 50%");

		mainPanel.add(optionsPanel, "pushx, growx, w 90%, wrap");
		mainPanel.add(loadPanel, "pushx, w 90%, growx, span, wrap");
		mainPanel.add(buttonPanel, "pushx, growx, w 90%");
	}

	/**
	 * Sets up general elements of the optionsPanel
	 */
	public void setOptionsPanel() {
		
		optionsPanel.removeAll();
		
		// Panel with user choices
		JPanel choicePanel = setupChoicePanel();
		
		// Cluster Info Panel
		JPanel infoPanel;
		
		if(clusterType.getSelectedIndex() == 0) {
			infoPanel = ClusterInfoFactory.makeHierInfoPanel(
					clusterChoice.getSelectedIndex());
			
		} else {
			infoPanel = ClusterInfoFactory.makeKmeansInfoPanel();
		}
	

		optionsPanel.add(choicePanel, "aligny 10%, w 40%, h 100%");
		optionsPanel.add(new JSeparator(JSeparator.VERTICAL), "pushx, w 1%, "
				+ "h 100%");
		optionsPanel.add(infoPanel, "aligny 10%, w 59%, h 100%, wrap");
	}

	/**
	 * Sets up the layout of the inner panels which are part of OptionPanel.
	 * These inner panels contain all of the GUI components that the user
	 * can interact with.
	 */
	public JPanel setupChoicePanel() {

		JPanel choicePanel = GUIFactory.createJPanel(false, true, null);
		
		// Components for choosing Cluster type
		final JLabel type = GUIFactory.createLabel("Cluster Type", 
				GUIFactory.FONTL);
		final JLabel switchLabel = GUIFactory.createLabel("Switch: ", 
				GUIFactory.FONTS);
		
		choicePanel.add(type, "pushx, alignx 0%, span, wrap");
		choicePanel.add(switchLabel, "pushx, alignx 0%, w 20%");
		choicePanel.add(clusterType, "pushx, alignx 0%, w 80%, wrap 20px");
		
		// Components for similarity measure options
		final JLabel similarity = GUIFactory.createLabel("Similarity "
				+ "Metric", GUIFactory.FONTL);
		final JLabel rowLabel = GUIFactory.createLabel("Rows: ", 
				GUIFactory.FONTS);
		final JLabel colLabel = GUIFactory.createLabel("Columns: ", 
				GUIFactory.FONTS);

		choicePanel.add(similarity, "pushx, alignx 0%, span, wrap");
		choicePanel.add(rowLabel, "pushx, alignx 0%, w 20%");
		choicePanel.add(geneCombo, "w 80%, wrap");
		choicePanel.add(colLabel, "pushx, alignx 0%, w 20%");
		choicePanel.add(arrayCombo, "w 80%, wrap 20px");

		// Components for linkage choice
		// Hierarchical
		if (clusterType.getSelectedIndex() == 0) {
			final JLabel method = GUIFactory.createLabel("Linkage Method", 
					GUIFactory.FONTL);
			final JLabel choose = GUIFactory.createLabel("Choose: ", 
					GUIFactory.FONTS);
			
			choicePanel.add(method, "pushx, alignx 0%, span, wrap");
			choicePanel.add(choose, "pushx, alignx 0%, w 20%");
			choicePanel.add(clusterChoice, "pushx, alignx 0%, w 80%, wrap");

		// K-means
		} else {
			final JLabel kMeans = GUIFactory.createLabel("K-Means Options", 
					GUIFactory.FONTL);
			final JLabel clusters = GUIFactory.createLabel("Clusters: ", 
					GUIFactory.FONTS);
			final JLabel its = GUIFactory.createLabel("Cycles: ", 
					GUIFactory.FONTS);
			final JLabel filler = GUIFactory.createLabel(StringRes.empty, 
					GUIFactory.FONTS);
			final JLabel rows = GUIFactory.createLabel(StringRes.main_rows, 
					GUIFactory.FONTS);
			final JLabel cols = GUIFactory.createLabel(StringRes.main_cols, 
					GUIFactory.FONTS);
			
			choicePanel.add(kMeans, "pushx, alignx 0%, span, wrap");
			
			choicePanel.add(filler, "w 20%!");
			choicePanel.add(rows, "split 2");
			choicePanel.add(cols, "wrap");
			
			choicePanel.add(clusters, "w 20%!");
			choicePanel.add(enterRC, "split 2");
			choicePanel.add(enterCC, "wrap");

			choicePanel.add(its, "w 20%!");
			choicePanel.add(enterRIt, "split 2");
			choicePanel.add(enterCIt);
		}
		
		return choicePanel;
	}

	/**
	 * This method sets up and returns a JSpinner for entering numerical
	 * input.
	 * 
	 * @return JSpinner
	 */
	public JSpinner setupSpinner() {

		final SpinnerNumberModel amountChoice = new SpinnerNumberModel(0, 0,
				5000, 1);
		final JSpinner jft = new JSpinner(amountChoice);

		return jft;
	}

	/**
	 * Get the mainPanel for reference
	 */
	public JPanel getMainPanel() {

		return mainPanel;
	}

	/**
	 * Get the similarity measure choice for row clustering
	 * 
	 * @return
	 */
	public JComboBox<String> getGeneCombo() {

		return geneCombo;
	}

	/**
	 * Get the similarity measure choice for column clustering
	 * 
	 * @return
	 */
	public JComboBox<String> getArrayCombo() {

		return arrayCombo;
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
	 * Adds a listener to cluster_button to register user interaction and notify
	 * ClusterController to call the performCluster() method.
	 * 
	 * @param cluster
	 */
	public void addClusterListener(final ActionListener cluster) {

		cluster_btn.addActionListener(cluster);
	}

//	/**
//	 * Performs the clustering action when the cluster_button is clicked, based
//	 * on the user choices.
//	 */
//	public void displayClusterProcess(final String choice,
//			final String choice2, final String linkageMethod) {
//
//		loadPanel.removeAll();
//		buttonPanel.remove(cluster_button);
//
//		setLoadPanel(choice, choice2);
//
//		optionsPanel.add(loadPanel, "pushx, w 90%, alignx 50%, span, wrap");
//		
//		buttonPanel.add(cancel_button, "pushx, alignx 50%");
//		mainPanel.add(buttonPanel, "pushx, alignx 50%, h 15%");
//
//		mainPanel.revalidate();
//		mainPanel.repaint();
//	}
	
	public void setClustering(boolean clustering) {
		
		if(clustering) {
			buttonPanel.remove(cluster_btn);
			buttonPanel.add(cancel_button, "pushx, alignx 50%");
			pBar.setEnabled(true);
			
		} else {
			buttonPanel.remove(cancel_button);
			buttonPanel.add(cluster_btn, "pushx, alignx 50%");
			pBar.setEnabled(false);
		}
		
		buttonPanel.revalidate();
		buttonPanel.repaint();
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
	
	public void addClusterTypeListener(final ActionListener l) {
		
		clusterType.addActionListener(l);
	}

	/**
	 * Attempts to cancel the worker thread when the cancel button is clicked.
	 * Also resets the view accordingly.
	 */
	public void cancel() {

		pBar.setValue(0);
		loadLabel.setText(StringRes.clusterInfo_Ready);
		setClustering(false);
	}

	/**
	 * Adds a load progress bar to loadpanel if a similarity measure has been
	 * selected.
	 * 
	 * @param choice
	 * @param choice2
	 */
	public void setLoadPanel() {
		
		loadPanel.add(loadLabel, "pushx, alignx 50%, wrap");
		loadPanel.add(pBar, "pushx, w 90%, alignx 50%, wrap");
	}

	/**
	 * Displays an error message if not at least one similarity measure has been
	 * selected, since the data cannot be clustered in that case.
	 */
	public void displayErrorLabel() {

		loadLabel.setText(StringRes.clusterError_invalid);

		loadPanel.revalidate();
		loadPanel.repaint();
	}

	/**
	 * Changes of the layout when the worker thread in controller has completed
	 * its calculations. (worker calls done())
	 */
	public void displayCompleted(final String finalFilePath) {

		buttonPanel.remove(cancel_button);

		final TextDisplay status1 = new TextDisplay(
				StringRes.clusterTip_completed);

		final TextDisplay status2 = new TextDisplay(
				StringRes.clusterTip_filePath + finalFilePath);

		buttonPanel.add(cluster_btn, "pushx, alignx 50%");

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
}
