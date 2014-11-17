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
package Views;

import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;

import edu.stanford.genetics.treeview.LogBuffer;
import Cluster.DistMatrixCalculator;
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

	public static final int HIER = 0;
	public static final int KMEANS = 1;
	
	/* Static variables for updates from clustering classes */
	public static JLabel loadLabel;
	public static JProgressBar pBar;
	
	/* Start values for k-means JSpinners */
	private final int CLUSTER_START = 10;
	private final int ITERATION_START = 100;
	
	/* GUI components */
	private JPanel mainPanel;
	private JPanel optionsPanel;
	private JPanel btnPanel;
	private JPanel loadPanel;

	private JComboBox<String> rowDistChooser;
	private JComboBox<String> colDistChooser;
	private JComboBox<String> clusterChooser;
	private JComboBox<String> linkageChooser;

	private JButton cluster_btn;
	private JButton cancel_btn;

	private JSpinner rowClusterSettr;
	private JSpinner colClusterSettr;
	private JSpinner rowIterationsSettr;
	private JSpinner colIterationsSettr;

	private final String[] linkageMethods = {"Single Linkage", 
			"Complete Linkage", "Average Linkage" };

	private int clusterType;

	/**
	 * Constructor for the ClusterView object which binds to an explicit
	 * Preferences parent node.
	 */
	public ClusterView() {
		
	}
	
	/**
	 * Generates the mainPanel which is put into ClusterDialog. This base 
	 * layer contains all the other layers with the information and 
	 * interactive components.
	 * @param int Integer that represents the type of clustering.
	 * @return The base JPanel.
	 */
	public JPanel makeClusterPanel(final int clusterType) {
		
		this.clusterType = clusterType;
		
		/* Main background panel */
		mainPanel = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING, null);
		
		/* Background panel for the cluster options */
		optionsPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
		
		/* Panel for the cluster/ cancel buttons */
		btnPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
		
		/* ProgressBar panel */
		loadPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
		
		/* Setting up the interactive components with listeners */
		setupInteractiveComponents();
		
		/* Setting up Swing layout */
		setupLayout();
		
		return mainPanel;
	}

	/**
	 * Sets up the layout for all panels and GUI components.
	 */
	public void setupLayout() {

		mainPanel.removeAll();

		/* Add all components to panels */
		setLoadPanel();
		setOptionsPanel();
		setMainPanel();

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	/**
	 * Sets up all the interactive components to be used in the layout.
	 */
	public void setupInteractiveComponents() {

		/* Button to begin clustering */
		cluster_btn = GUIFactory.createBtn(StringRes.btn_Cluster);

		/* Button to cancel worker thread in the controller */
		cancel_btn = GUIFactory.createBtn(StringRes.btn_Cancel);

		/* Label for cluster process */
		loadLabel = GUIFactory.createLabel(StringRes.clusterInfo_Ready, 
				GUIFactory.FONTS);
		
		/* ProgressBar for clustering process */
		pBar = GUIFactory.createPBar();

		/* ComboBox to choose cluster method */
		final String[] clusterNames = { StringRes.menu_Hier,
				StringRes.menu_KMeans };

		clusterChooser = GUIFactory.createComboBox(clusterNames);

		clusterChooser.setSelectedIndex(clusterType);

		final String[] measurements = { 
				StringRes.cluster_DoNot,
				StringRes.cluster_pearsonUn,
				StringRes.cluster_pearsonCtrd,
				StringRes.cluster_absCorrUn,
				StringRes.cluster_absCorrCtrd,
				StringRes.cluster_spearman, 
				StringRes.cluster_euclidean,
				StringRes.cluster_cityBlock 
		};

		/* Initially shown distance measure Euclidean for k-means! */
		int initialDist;
		if (clusterType == HIER) {
			initialDist = DistMatrixCalculator.PEARSON_UN;
		} else {
			initialDist = DistMatrixCalculator.EUCLIDEAN;
		}
		
		/* Drop-down menu for row selection */
		rowDistChooser = GUIFactory.createComboBox(measurements);
		rowDistChooser.setSelectedIndex(initialDist);

		/* Drop-down menu for column selection */
		colDistChooser = GUIFactory.createComboBox(measurements);
		colDistChooser.setSelectedIndex(initialDist);

		/* Linkage choice drop-down menu */
		linkageChooser = GUIFactory.createComboBox(linkageMethods);

		/* Spinners for k-means */
		rowClusterSettr = setupSpinner(CLUSTER_START);
		colClusterSettr = setupSpinner(CLUSTER_START);
		rowIterationsSettr = setupSpinner(ITERATION_START);
		colIterationsSettr = setupSpinner(ITERATION_START);
	}

	/* Layout setups for main UI elements */
	/**
	 * Sets up general elements of the mainPanel.
	 */
	public void setMainPanel() {

		// Panel for the Buttons
		btnPanel.removeAll();
		btnPanel.add(cluster_btn, "pushx, alignx 50%");

		mainPanel.add(optionsPanel, "pushx, growx, w 90%, wrap");
		mainPanel.add(loadPanel, "pushx, w 90%, growx, span, wrap");
		mainPanel.add(btnPanel, "pushx, growx, w 90%");
	}

	/**
	 * Sets up general elements of the optionsPanel.
	 */
	public void setOptionsPanel() {
		
		optionsPanel.removeAll();
		
		// Panel with user choices
		JPanel choicePanel = setupChoicePanel();
		
		// Cluster Info Panel
		JPanel infoPanel;
		
		if(clusterChooser.getSelectedIndex() == 0) {
			infoPanel = ClusterInfoFactory.makeHierInfoPanel(
					linkageChooser.getSelectedIndex());
			
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

		JPanel choicePanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, 
				null);
		
		// Components for choosing Cluster type
		final JLabel type = GUIFactory.createLabel("Cluster Type", 
				GUIFactory.FONTL);
		final JLabel switchLabel = GUIFactory.createLabel("Switch: ", 
				GUIFactory.FONTS);
		
		choicePanel.add(type, "pushx, alignx 0%, span, wrap");
		choicePanel.add(switchLabel, "pushx, alignx 0%, w 20%");
		choicePanel.add(clusterChooser, "pushx, alignx 0%, w 80%, wrap 20px");
		
		// Components for similarity measure options
		final JLabel similarity = GUIFactory.createLabel("Similarity "
				+ "Metric", GUIFactory.FONTL);
		final JLabel rowLabel = GUIFactory.createLabel("Rows: ", 
				GUIFactory.FONTS);
		final JLabel colLabel = GUIFactory.createLabel("Columns: ", 
				GUIFactory.FONTS);

		choicePanel.add(similarity, "pushx, alignx 0%, span, wrap");
		choicePanel.add(rowLabel, "pushx, alignx 0%, w 20%");
		choicePanel.add(rowDistChooser, "w 80%, wrap");
		choicePanel.add(colLabel, "pushx, alignx 0%, w 20%");
		choicePanel.add(colDistChooser, "w 80%, wrap 20px");

		/* Components for hierarchical linkage choice */
		if (clusterChooser.getSelectedIndex() == 0) {
			final JLabel method = GUIFactory.createLabel("Linkage Method", 
					GUIFactory.FONTL);
			final JLabel choose = GUIFactory.createLabel("Choose: ", 
					GUIFactory.FONTS);
			
			choicePanel.add(method, "pushx, alignx 0%, span, wrap");
			choicePanel.add(choose, "pushx, alignx 0%, w 20%");
			choicePanel.add(linkageChooser, "pushx, alignx 0%, w 80%, wrap");

		/* k-means */
		} else {
			final JLabel kMeans = GUIFactory.createLabel("K-Means Options", 
					GUIFactory.FONTL);
			final JLabel clusters = GUIFactory.createLabel("Clusters*: ", 
					GUIFactory.FONTS);
			final JLabel its = GUIFactory.createLabel("Cycles*: ", 
					GUIFactory.FONTS);
			final JLabel filler = GUIFactory.createLabel(StringRes.empty, 
					GUIFactory.FONTS);
			final JLabel rows = GUIFactory.createLabel(StringRes.main_rows, 
					GUIFactory.FONTS);
			final JLabel cols = GUIFactory.createLabel(StringRes.main_cols, 
					GUIFactory.FONTS);
			final JLabel req = GUIFactory.createLabel("* required", 
					GUIFactory.FONTS);
			
			choicePanel.add(kMeans, "pushx, alignx 0%, span, wrap");
			
			choicePanel.add(filler, "w 25%!");
			choicePanel.add(rows, "w 25%!, split 2");
			choicePanel.add(cols, "wrap");
			
			choicePanel.add(clusters, "w 25%!");
			choicePanel.add(rowClusterSettr, "w 25%!, split 2");
			choicePanel.add(colClusterSettr, "wrap");

			choicePanel.add(its, "w 25%!");
			choicePanel.add(rowIterationsSettr, "w 25%!, split 2");
			choicePanel.add(colIterationsSettr, "wrap");
			choicePanel.add(req);
		}
		
		return choicePanel;
	}

	/**
	 * This method sets up and returns a JSpinner for entering numerical
	 * input.
	 * 
	 * @return JSpinner
	 */
	public JSpinner setupSpinner(int start) {

		final SpinnerNumberModel amountChoice = new SpinnerNumberModel(start, 0,
				5000, 1);
		final JSpinner jft = new JSpinner(amountChoice);

		return jft;
	}
	
	/**
	 * Sets the state of clustering. If true, clustering is considered in
	 * progress and the UI has to change accordingly. The cluster button will
	 * be replaced by a cancel button, and vice versa.
	 * @param isInProgress
	 */
	public void setClustering(boolean isInProgress) {
		
		updatePBar(0); // set ProgressBar
		
		if(isInProgress) {
			loadLabel.setForeground(UIManager.getColor("Label.foreground"));
			btnPanel.remove(cluster_btn);
			btnPanel.add(cancel_btn, "pushx, alignx 50%");
			
		} else {
			loadLabel.setText(StringRes.clusterInfo_Ready);
			btnPanel.remove(cancel_btn);
			btnPanel.add(cluster_btn, "pushx, alignx 50%");
		}
		
		btnPanel.revalidate();
		btnPanel.repaint();
	}

	/*-------Listeners ------*/
	/**
	 * Adds a listener to cluster_button to register user interaction and notify
	 * ClusterController to call the performCluster() method.
	 * 
	 * @param cluster
	 */
	public void addClusterListener(final ActionListener cluster) {

		cluster_btn.addActionListener(cluster);
	}
	
	/**
	 * Adds a listener to cancel_btn to register user interaction and notifies
	 * ClusterController to call the cancel() method.
	 * 
	 * @param cancel
	 */
	public void addCancelListener(final ActionListener cancel) {

		cancel_btn.addActionListener(cancel);
	}
	
	/**
	 * Listener for the linkage JComboBox in order to change the display
	 * of information about linkage methods when the selection is changed.
	 * @param l
	 */
	public void addLinkageListener(final ActionListener l) {
		
		linkageChooser.addActionListener(l);
	}
	
	public void addClusterTypeListener(final ActionListener l) {
		
		clusterChooser.addActionListener(l);
	}
	
	public void addRowDistListener(final ItemListener l) {
		
		rowDistChooser.addItemListener(l);
	}
	
	public void addColDistListener(final ItemListener l) {
		
		colDistChooser.addItemListener(l);
	}
	
	public void addSpinnerListener(final ChangeListener l) {
		
		rowClusterSettr.addChangeListener(l);
		colClusterSettr.addChangeListener(l);
		rowIterationsSettr.addChangeListener(l);
		colIterationsSettr.addChangeListener(l);
	}

	/**
	 * Adds a load progress bar to loadPanel if a similarity measure has been
	 * selected.
	 */
	public void setLoadPanel() {
		
		loadPanel.add(loadLabel, "pushx, alignx 50%, wrap");
		loadPanel.add(pBar, "pushx, w 90%, alignx 50%, wrap");
	}

	/**
	 * Displays an error message if not at least one similarity measure has been
	 * selected, since the data cannot be clustered in that case.
	 */
	public void displayReadyStatus(boolean ready) {

		if(ready) {
			loadLabel.setForeground(UIManager.getColor("Label.foreground"));
			loadLabel.setText(StringRes.clusterInfo_Ready);
			
		} else {
			loadLabel.setText(StringRes.clusterError_invalid);
			loadLabel.setForeground(GUIFactory.RED1);
		}
		
		cluster_btn.setEnabled(ready);

		loadPanel.revalidate();
		loadPanel.repaint();
	}

	/* Getters to pass user input to the controller */
	/**
	 * Returns the selection of the cluster method as a String.
	 * 
	 * @return
	 */
	public String getClusterMethod() {

		return (String) clusterChooser.getSelectedItem();
	}

	/**
	 * Returns the selected similarity measure for the rows as a String.
	 * 
	 * @return int choice
	 */
	public int getRowSimilarity() {

		return rowDistChooser.getSelectedIndex();
	}

	/**
	 * Returns the selected similarity measure for the columns as a String.
	 * 
	 * @return int choice
	 */
	public int getColSimilarity() {

		return colDistChooser.getSelectedIndex();
	}

	/**
	 * Returns the selected linkage method as a String.
	 * 
	 * @return int choice
	 */
	public int getLinkMethod() {

		return linkageChooser.getSelectedIndex();
	}

	/**
	 * Returns the current values the user defined for the different spinners.
	 * 
	 * @return
	 */
	public Integer[] getSpinnerValues() {

		final int spinners = 4;
		final Integer[] spinnerValues = new Integer[spinners];

		spinnerValues[0] = (Integer) rowClusterSettr.getValue();
		spinnerValues[1] = (Integer) rowIterationsSettr.getValue();
		spinnerValues[2] = (Integer) colClusterSettr.getValue();
		spinnerValues[3] = (Integer) colIterationsSettr.getValue();

		return spinnerValues;
	}

	/* GUI update methods */
	/**
	 * Defines the text to be displayed by loadLabel in ClusterView.
	 * 
	 * @param text Text to be shown by the loading indicator label.
	 */
	public static void setLoadText(final String text) {

		loadLabel.setText(text);
	}

	/**
	 * Sets the maximum for the JProgressBar.
	 * 
	 * @param max Maximum value of the JProgressBar.
	 */
	public static void setPBarMax(final int max) {

		LogBuffer.println("Setting pBar max: " + max);
		pBar.setMaximum(max); 
	}

	/**
	 * Updates the value of the JProgressBar in ClusterView.
	 * 
	 * @param i Value to set the JProgressBar
	 */
	public static void updatePBar(final int i) {

		pBar.setValue(i);
	}
}
