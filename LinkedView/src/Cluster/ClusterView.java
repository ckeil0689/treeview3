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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.ExecutionException;

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
import javax.swing.SwingWorker;
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
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;

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

	private JFrame topFrame;
	private TreeViewFrame viewFrame;
	private final TVDataMatrix matrix;
	private final double[] dataArray;
	private boolean hierarchical;

	// Various GUI elements
	private JScrollPane scrollPane;
	private JPanel mainPanel;
	private JPanel optionsPanel;
	private JPanel buttonPanel;
	private JPanel similarityPanel;
	private JPanel linkagePanel;
	private JPanel kMeansPanel;
	private JLabel head1;
	private JLabel head2;
	private JLabel head3;
	private JComboBox geneCombo;
	private JComboBox arrayCombo;
	private JComboBox clusterType;

	// Similarity Measure Label
	private JLabel similarity;

	// ComboBox Options
	private final String[] measurements = { "Do Not Cluster",
			"Pearson Correlation (uncentered)",
			"Pearson Correlation (centered)",
			"Absolute Correlation (uncentered)",
			"Absolute Correlation (centered)", "Spearman Ranked Correlation",
			"Euclidean Distance", "City Block Distance" };

	private final String[] clusterNames = { "Hierarchical Clustering",
			"K-Means" };

	private JButton cluster_button;
	private JButton cancel_button;
	private JButton dendro_button;
	private TextDisplay status1;
	private TextDisplay status2;
	private JLabel method;
	private JLabel kMeans;
	private TextDisplay error1;
	private TextDisplay error2;
	private TextDisplay error3;
	private JLabel clusters;
	private JSpinner enterRC;
	private JSpinner enterCC;
	private JLabel its;
	private JSpinner enterRIt;
	private JSpinner enterCIt;
	private JProgressBar pBar;
	private JProgressBar pBar2;
	private JProgressBar pBar3;
	private JProgressBar pBar4;
	private JPanel loadPanel;
	private JComboBox clusterChoice;
	private ClickableIcon infoIcon;

	private String path;
	private File file;

	private String linkageMethod = null;

	private SwingWorker<Void, Void> worker;
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
	public ClusterView(final DataModel cVModel, final TreeViewFrame vFrame,
			final boolean hierarchical) {

		this(cVModel, null, vFrame, "Cluster View", hierarchical);
	}

	/**
	 * Second chained constructor
	 * 
	 * @param cVModel
	 * @param root
	 * @param vFrame
	 */
	public ClusterView(final DataModel cVModel, final ConfigNode root,
			final TreeViewFrame vFrame, final boolean hierarchical) {

		this(cVModel, root, vFrame, "Cluster View", hierarchical);
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
	public ClusterView (final DataModel dataModel, final ConfigNode root,
			final TreeViewFrame vFrame, final String name,
			final boolean hierarchical) {

		super.setName(name);

		this.dataModel = dataModel;
		this.viewFrame = vFrame;
		this.topFrame = (JFrame) SwingUtilities
				.getRoot(this);
		this.hierarchical = hierarchical;

		// Reference to loaded data
		matrix = (TVDataMatrix) dataModel.getDataMatrix();
		dataArray = matrix.getExprData();

		// Set layout for initial window
		setupLayout();
	}

	public void setupLayout() {

		removeAll();
		setLayout(new MigLayout("ins 0"));

		// Create background panel
		mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout("ins 0"));
		mainPanel.setBackground(GUIParams.BG_COLOR);

		// Background Panel for the Cluster Options
		optionsPanel = new JPanel();
		optionsPanel.setLayout(new MigLayout());
		optionsPanel.setBorder(BorderFactory.createLineBorder(
				GUIParams.BORDERS, EtchedBorder.LOWERED));
		optionsPanel.setOpaque(false);

		// Panel for the Buttons
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new MigLayout());
		buttonPanel.setOpaque(false);

		// header
		head1 = new JLabel("Options");
		head1.setFont(GUIParams.FONTL);
		head1.setForeground(GUIParams.ELEMENT);

		clusterType = GUIParams.setComboLayout(clusterNames);
		if (hierarchical) {
			clusterType.setSelectedIndex(0);

		} else {
			clusterType.setSelectedIndex(1);
		}
		
		clusterType.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {

				mainPanel.removeAll();
				optionsPanel.removeAll();

				setOptionsPanel();
				setMainPanel();

				mainPanel.revalidate();
				mainPanel.repaint();
			}
		});

		// make mainpanel scrollable by adding it to scrollpane
		scrollPane = new JScrollPane(mainPanel,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setOpaque(false);

		// Component for similarity measure options
		similarityPanel = new JPanel();
		similarityPanel.setLayout(new MigLayout());
		similarityPanel.setOpaque(false);
		
		similarity = new JLabel("Similarity Metric");
		similarity.setFont(GUIParams.FONTL);
		similarity.setForeground(GUIParams.ELEMENT);

		// Labels
		head2 = new JLabel("Rows:");
		head2.setFont(GUIParams.FONTS);
		head2.setForeground(GUIParams.TEXT);

		head3 = new JLabel("Columns:");
		head3.setFont(GUIParams.FONTS);
		head3.setForeground(GUIParams.TEXT);

		// Drop-down menu for row selection
		geneCombo = GUIParams.setComboLayout(measurements);

		// Drop-down menu for column selection
		arrayCombo = GUIParams.setComboLayout(measurements);
		
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
		method = new JLabel("Linkage Method");
		method.setFont(GUIParams.FONTL);
		method.setForeground(GUIParams.ELEMENT);

		// Clickable Panel to call InfoFrame
		infoIcon = new ClickableIcon(viewFrame, GUIParams.QUESTIONICON);

		// Linkage choice drop-down menu
		clusterChoice = GUIParams.setComboLayout(clusterMethods);
		
		linkagePanel.add(method, "span, wrap");
		linkagePanel.add(clusterChoice, "w 40%");
		linkagePanel.add(infoIcon, "w 10%");
		
		// Component for K-Means options
		kMeansPanel = new JPanel();
		kMeansPanel.setLayout(new MigLayout());
		kMeansPanel.setOpaque(false);
		
		kMeans = new JLabel("K-Means Options");
		kMeans.setFont(GUIParams.FONTL);
		kMeans.setForeground(GUIParams.ELEMENT);

		clusters = new JLabel("Clusters: ");
		clusters.setFont(GUIParams.FONTS);
		clusters.setForeground(GUIParams.TEXT);

		its = new JLabel("Iterations: ");
		its.setFont(GUIParams.FONTS);
		its.setForeground(GUIParams.TEXT);

		enterRC = setupSpinner();
		enterCC = setupSpinner();
		enterRIt = setupSpinner();
		enterCIt = setupSpinner();
		
		kMeansPanel.add(kMeans, "span, wrap");
		
		kMeansPanel.add(clusters, "w 10%, h 15%");
		kMeansPanel.add(enterRC, "w 5%");
		kMeansPanel.add(enterCC, "w 5%, wrap");

		kMeansPanel.add(its, "w 10%, h 15%");
		kMeansPanel.add(enterRIt, "w 5%");
		kMeansPanel.add(enterCIt, "w 5%");

		// ProgressBar Component
		loadPanel = new JPanel();
		loadPanel.setLayout(new MigLayout());
		loadPanel.setOpaque(false);
		loadPanel.setBorder(BorderFactory.createLineBorder(GUIParams.BORDERS,
				EtchedBorder.LOWERED));
		
		setupWorkerThread();

		// Button to show DendroView
		dendro_button = GUIParams.setButtonLayout("Clustergram", 
				"forwardIcon");
		dendro_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				final FileSet fileSet = new FileSet(file.getName(), file
						.getParent() + File.separator);

				try {
					viewFrame.loadFileSet(fileSet);

				} catch (final LoadException e) {

				}
				viewFrame.setLoaded(true);
				topFrame.dispose();
			}
		});

		// Button to begin Clustering
		cluster_button = GUIParams.setButtonLayout("Cluster", null);
		cluster_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				final String choice = (String) geneCombo.getSelectedItem();
				final String choice2 = (String) arrayCombo.getSelectedItem();

				if (isHierarchical()) {
					linkageMethod = (String) clusterChoice.getSelectedItem();
				}

				// needs at least one box to be selected
				// otherwise display error
				if (checkSelections(choice, choice2)) {

					loadPanel.removeAll();
					dendro_button.setEnabled(false);
					buttonPanel.remove(cluster_button);

					// ProgressBars
					pBar = GUIParams.setPBarLayout("Row Distance Matrix");

					pBar2 = GUIParams.setPBarLayout("Row Clustering");

					pBar3 = GUIParams.setPBarLayout("Column Distance Matrix");

					pBar4 = GUIParams.setPBarLayout("Column Clustering");

					// Button to cancel process
					cancel_button = GUIParams.setButtonLayout("Cancel", null);
					cancel_button.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(final ActionEvent arg0) {

							worker.cancel(true);

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
					});

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

					optionsPanel.add(loadPanel, "pushx, w 90%, alignx 50%, " +
							"span, wrap");

					buttonPanel.add(cancel_button, "pushx, alignx 50%");
					buttonPanel.add(dendro_button, "pushx, alignx 50%");
					mainPanel.add(buttonPanel, "pushx, alignx 50%, "
							+ "h 15%");

					mainPanel.revalidate();
					mainPanel.repaint();

					// start new cluster process
					worker.execute();

				} else {
					loadPanel.removeAll();

					String hint = "Woah, that's too quick!";
					String hint2 = "";
					String hint3 = "";

					if (isHierarchical()) {
						hint2 = "Select at least one similarity metric for "
								+ "either rows or columns to begin clustering!";

					} else {
						hint2 = "Select at least one similarity metric for "
								+ "either rows or columns to begin clustering!";

						hint3 = "The amount of clusters and iterations must "
								+ "be greater than 0.";
					}

					error1 = new TextDisplay(hint);
					error1.setForeground(GUIParams.RED1);
					
					error2 = new TextDisplay(hint2);
					error3 = new TextDisplay(hint3);

					loadPanel.add(error1, "w 90%, alignx 50%, span, wrap");
					loadPanel.add(error2, "w 90%, span, wrap");
					loadPanel.add(error3, "w 90%, span");
					
					optionsPanel.add(loadPanel, "alignx 50%, push, w 90%, " +
							"span");

					mainPanel.revalidate();
					mainPanel.repaint();
				}
			}
		});

		setOptionsPanel();

		dendro_button.setEnabled(false);
		buttonPanel.add(cluster_button, "alignx 50%, pushx");

		setMainPanel();

		// Add the scrollPane to ClusterView2 Panel
		add(scrollPane, "grow, push");

		revalidate();
		repaint();
	}

	@Override
	public void refresh() {

		setupLayout();
	}

	// Layout setups for some Swing elements
	/**
	 * Sets up general elements of the mainPanel
	 */
	public void setMainPanel() {

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

		optionsPanel.add(similarityPanel, "pushy, h 20%, span, wrap");

		if (isHierarchical()) {
			optionsPanel.add(linkagePanel, "pushy, h 20%, span, wrap");

		} else {
			optionsPanel.add(kMeansPanel, "pushy, h 20%, span");
		}
	}
	
	/**
	 * Sets up the worker thread to start calculations without 
	 * affecting the GUI.
	 */
	public void setupWorkerThread() {
		
		worker = new SwingWorker<Void, Void>() {

			@Override
			public Void doInBackground() {

				try {
					int row_clusterN = 0;
					int row_iterations = 0;
					int col_clusterN = 0;
					int col_iterations = 0;
					
					// Set integers only if KMeans options are shown
					if (!isHierarchical()) {
						row_clusterN = (Integer) enterRC.getValue();
						col_clusterN = (Integer) enterCC.getValue();

						row_iterations = (Integer) enterRIt.getValue();
						col_iterations = (Integer) enterCIt.getValue();
					}

					// Setup a ClusterProcessor
					final ClusterProcessor clusterTarget = new ClusterProcessor(
							ClusterView.this, pBar, pBar2, pBar3, pBar4,
							linkageMethod, row_clusterN, row_iterations,
							col_clusterN, col_iterations);

					// Begin the actual clustering, hierarchical or kmeans
					clusterTarget.cluster(isHierarchical());

				} catch (final InterruptedException e) {

				} catch (final ExecutionException e) {

				}

				return null;
			}

			@Override
			protected void done() {

				buttonPanel.remove(cancel_button);

				status1 = new TextDisplay("The file has been saved "
						+ "in the original directory.");

				status2 = new TextDisplay("File Path: " + path);

				dendro_button.setEnabled(true);
				buttonPanel.add(cluster_button, "pushx, alignx 50%");
				buttonPanel.add(dendro_button, "pushx, alignx 50%");

				loadPanel.add(status1, "growx, pushx, wrap");
				loadPanel.add(status2, "growx, pushx, wrap");

				mainPanel.revalidate();
				mainPanel.repaint();
			}
		};
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

//		final Dimension d = jft.getPreferredSize();
//		d.setSize(d.getWidth(), d.getHeight() * 2);
//		jft.setPreferredSize(d);
		jft.setFont(GUIParams.FONTS);

		return jft;
	}

	/**
	 * Returns the choice of the cluster drop down menu as a string
	 * 
	 * @return
	 */
	public boolean isHierarchical() {

		final String choice = (String) clusterType.getSelectedItem();
		hierarchical = choice.equalsIgnoreCase("Hierarchical Clustering");
		return hierarchical;
	}

	/**
	 * Checks whether all needed options are selected to perform clustering.
	 * 
	 * @param choice
	 * @param choice2
	 * @return
	 */
	public boolean checkSelections(final String choice, final String choice2) {

		final int clustersR = (Integer) enterRC.getValue();
		final int itsR = (Integer) enterRIt.getValue();
		final int clustersC = (Integer) enterCC.getValue();
		final int itsC = (Integer) enterCIt.getValue();

		if (isHierarchical()) {
			if (!choice.contentEquals("Do Not Cluster")
					|| !choice2.contentEquals("Do Not Cluster")) {
				return true;

			} else {
				return false;
			}
		} else {
			if ((!choice.contentEquals("Do Not Cluster") 
					&& (clustersR > 0 && itsR > 0))
					|| (!choice2.contentEquals("Do Not Cluster") 
							&& (clustersC > 0 && itsC > 0))) {
				return true;

			} else {
				return false;
			}
		}
	}

	/**
	 * Setter for file path
	 * 
	 * @param filePath
	 */
	public void setPath(final String filePath) {

		path = filePath;
	}

	/**
	 * Setter for file
	 * 
	 * @param cdtFile
	 */
	public void setFile(final File cdtFile) {

		file = cdtFile;
	}

	public JLabel getTitle() {

		return head1;
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
	 * Getter for the dataArray
	 * 
	 * @return double[] dataArray
	 */
	public double[] getDataArray() {

		return dataArray;
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
