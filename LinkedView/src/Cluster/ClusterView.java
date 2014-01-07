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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;

import net.miginfocom.swing.MigLayout;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.MainProgramArgs;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;

import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;
//Explicitly imported because error (unclear TVModel reference) was thrown

/**
 *  This class exists to internalize the clustering process 
 *  directly into TreeView. It provides a GUI which is called from the 
 *  slightly adjusted original Java TreeView menubar. The GUI content 
 *  is loaded into the program in a similar manner as the DendroView class.
 *
 * @author    	CKeil <ckeil@princeton.edu>
 * @version 	0.1
 */
public class ClusterView extends JPanel implements MainPanel {
	
	private static final long serialVersionUID = 1L;
	
	//Instance
	protected DataModel dataModel;
	protected ConfigNode root;
	
	private TreeViewFrame viewFrame;
	private TVDataMatrix matrix;
	private double[] dataArray;
	private boolean hierarchical;
	
	//Various GUI elements
	private JScrollPane scrollPane;
	private JPanel mainPanel;
	private JPanel optionsPanel;
	private JPanel buttonPanel;
	private JPanel emptyPanel;
	private JLabel head1;
	private JLabel head2;
	private JLabel head3;
	private JComboBox geneCombo; 
	private JComboBox arrayCombo;
	private JComboBox clusterType;
	
	//Similarity Measure Label
	private JLabel similarity;
	
	//ComboBox Options
	private final String[] measurements = {"Do Not Cluster", 
			"Pearson Correlation (uncentered)", 
			"Pearson Correlation (centered)", 
			"Absolute Correlation (uncentered)", 
			"Absolute Correlation (centered)", "Spearman Ranked Correlation", 
			"Euclidean Distance", "City Block Distance"};
	
	private final String[] clusterNames = {"Hierarchical Clustering", 
			"K-Means"};
	
	private JButton cluster_button; 
	private JButton back_button;
	private JButton cancel_button;
	private JButton dendro_button;
	private JLabel status1; 
	private JLabel status2;
	private JLabel method;
	private JLabel error1;
	private JLabel error2;
	private JLabel error3;
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
	private int row_clusterN = 0;
	private int row_iterations = 0;
	private int col_clusterN = 0;
	private int col_iterations = 0;
	
	private SwingWorker<Void, Void> worker;
	private final String[] clusterMethods = {"Single Linkage", 
			"Centroid Linkage", "Average Linkage", "Complete Linkage"};

	/**
	 * Chained constructor for the ClusterView object
	 * note this will reuse any existing MainView subnode of the documentconfig.
	 *
	 * @param  cVModel model this ClusterView is to represent
	 * @param  vFrame  parent ViewFrame of DendroView
	 */
	public ClusterView(DataModel cVModel, TreeViewFrame vFrame, 
			boolean hierarchical) {
		
		this(cVModel, null, vFrame, "Cluster View", hierarchical);
	}
	
	/**
	 * Second chained constructor
	 * @param cVModel
	 * @param root
	 * @param vFrame
	 */
	public ClusterView(DataModel cVModel, ConfigNode root, 
			TreeViewFrame vFrame, boolean hierarchical) {
		
		this(cVModel, root, vFrame, "Cluster View", hierarchical);
	}
	
	/**
	 *  Constructor for the ClusterView object which 
	 *  binds to an explicit confignode
	 *
	 * @param  dataModel   model this DendroView is to represent
	 * @param  root   Confignode to which to bind this DendroView
	 * @param  vFrame  parent ViewFrame of ClusterView
	 * @param  name name of this view.
	 */
	public ClusterView(DataModel dataModel, ConfigNode root, 
			TreeViewFrame vFrame, String name, boolean hierarchical) {
		
		super.setName(name);
		
		this.dataModel = dataModel;
		this.viewFrame = vFrame;
		this.hierarchical = hierarchical;
		
		//Reference to loaded data
		matrix = (TVDataMatrix)dataModel.getDataMatrix();
		dataArray = matrix.getExprData();
		
		//Setting the UIManager up for Win vs. Mac
		try{
			UIManager.setLookAndFeel(
					UIManager.getCrossPlatformLookAndFeelClassName());
			
		} catch (Exception e){
			
		}

		viewFrame.setResizable(true);
		
		//Set layout for initial window
		setupLayout();
	}	
	
	public void setupLayout() {
		
		this.removeAll();
		this.setLayout(new MigLayout("ins 0"));
		
		//Create background panel
		mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout("ins 0"));
		mainPanel.setBackground(GUIParams.BG_COLOR);
		
		//Background Panel for the Cluster Options
		optionsPanel = new JPanel();
		optionsPanel.setLayout(new MigLayout());
		optionsPanel.setBorder(BorderFactory.createLineBorder(
				GUIParams.BORDERS, EtchedBorder.LOWERED));
		optionsPanel.setOpaque(false);
		
		//Panel for the Buttons
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new MigLayout());
		buttonPanel.setOpaque(false);
		
		//header
		head1 = new JLabel("Options");
		head1.setFont(GUIParams.FONTL);
		head1.setForeground(GUIParams.ELEMENT);
		
		clusterType = setComboLayout(clusterNames);
		if(hierarchical) {
			clusterType.setSelectedIndex(0);
			
		} else {
			clusterType.setSelectedIndex(1);
		}
		clusterType.addActionListener(new ActionListener(){
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				mainPanel.removeAll();
				optionsPanel.removeAll();
				
		  		setOptionsPanel();
				setMainPanel();
				
				mainPanel.revalidate();
				mainPanel.repaint();
			}
		});
		
		//make mainpanel scrollable by adding it to scrollpane
		scrollPane = new JScrollPane(mainPanel, 
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, 
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setOpaque(false);
		
		similarity = new JLabel("Similarity Metric: ");
  		similarity.setFont(GUIParams.FONTL);
  		similarity.setForeground(GUIParams.TEXT);
  		
  		//filler component
  		emptyPanel = new JPanel();
  		emptyPanel.setLayout(new MigLayout());
  		emptyPanel.setOpaque(false);
  		
		//Labels
		head2 = new JLabel("Rows");
		head2.setFont(GUIParams.FONTL);
		head2.setForeground(GUIParams.ELEMENT);
		
		head3 = new JLabel("Columns");
		head3.setFont(GUIParams.FONTL);
		head3.setForeground(GUIParams.ELEMENT);
		 
		//Drop-down menu for row selection
  		geneCombo = setComboLayout(measurements);
	
		//Drop-down menu for column selection
  		arrayCombo = setComboLayout(measurements);
  
  		
		worker = new SwingWorker<Void, Void>() {	
			
			@Override
			public Void doInBackground() {

	        	try {
	        		//Set integers only if KMeans options are shown
	        		if(!isHierarchical()) {
		        		row_clusterN = (Integer) enterRC.getValue();
		        		col_clusterN = (Integer) enterCC.getValue();
		        		
		        		row_iterations = (Integer) enterRIt.getValue();
		        		col_iterations = (Integer) enterCIt.getValue();
	        		}
	        		
	        		//Setup a ClusterProcessor
	        		ClusterProcessor clusterTarget = 
	        				new ClusterProcessor(ClusterView.this, pBar, pBar2, 
	        						pBar3, pBar4, linkageMethod, row_clusterN, 
	        						row_iterations, col_clusterN, 
	        						col_iterations);
	        		
	        		//Begin the actual clustering, hierarchical or kmeans
	        		clusterTarget.cluster(isHierarchical());
	        		
				} catch (InterruptedException e) {
					
					
				} catch (ExecutionException e) {
					
				}
	        	
				return null;
			}
				
			@Override
			protected void done(){
				
				buttonPanel.remove(cancel_button);
				
				status1 = new JLabel("The file has been saved " +
						"in the original directory.");
				status1.setForeground(GUIParams.TEXT);
				status1.setFont(GUIParams.FONTS);
				
				status2 = new JLabel("File Path: " + path);
				status2.setForeground(GUIParams.TEXT);
				status2.setFont(GUIParams.FONTS);
				
				dendro_button.setEnabled(true);
				buttonPanel.add(back_button, "pushx, alignx 50%");
				buttonPanel.add(cluster_button, "pushx, alignx 50%");
				buttonPanel.add(dendro_button, "pushx, alignx 50%");
				
				loadPanel.add(status1, "growx, pushx, wrap");
				loadPanel.add(status2, "growx, pushx, wrap");
				
				mainPanel.revalidate();
				mainPanel.repaint();	
			}
		};
		
		//Label
		method = new JLabel("Linkage Method:");
		method.setFont(GUIParams.FONTL);
		method.setForeground(GUIParams.TEXT);
		
		//Clickable Panel to call InfoFrame
		infoIcon = new ClickableIcon(viewFrame, "infoIcon.png");
    	
		//Linkage choice drop-down menu
		clusterChoice = setComboLayout(clusterMethods);
		
		clusters = new JLabel("Clusters: ");
		clusters.setFont(GUIParams.FONTL);
		clusters.setForeground(GUIParams.TEXT);
		
		its = new JLabel("Iterations: ");
		its.setFont(GUIParams.FONTL);
		its.setForeground(GUIParams.TEXT);
		
		enterRC = setupSpinner();
		enterCC = setupSpinner();
		enterRIt = setupSpinner();
		enterCIt = setupSpinner();
		
		//ProgressBar Component
		loadPanel = new JPanel();
		loadPanel.setLayout(new MigLayout());
		loadPanel.setOpaque(false);
		loadPanel.setBorder(
				BorderFactory.createLineBorder(GUIParams.BORDERS, 
						EtchedBorder.LOWERED));
    	
  		//Button to go back to data preview
    	back_button = new JButton("< Back");
    	back_button = ClusterView.setButtonLayout(back_button);
		back_button.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				viewFrame.setLoaded(false);
			}
    	});
		
		//Button to show DendroView
    	dendro_button = new JButton("Clustergram > ");
    	dendro_button = ClusterView.setButtonLayout(dendro_button);
  		dendro_button.addActionListener(new ActionListener(){
	    		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				FileSet fileSet = new FileSet(file.getName(), 
						file.getParent() + File.separator);
				
				try {
					viewFrame.loadFileSet(fileSet);
					
				} catch (LoadException e) {
					
				}
				viewFrame.setLoaded(true);
			}
    	});
    	
    	//Button to begin Clustering
    	cluster_button = new JButton("Cluster");
    	cluster_button = ClusterView.setButtonLayout(cluster_button);
    	cluster_button.addActionListener(new ActionListener() {
	    		
			@Override
			public void actionPerformed(ActionEvent arg0) {
					
				final String choice = (String)geneCombo.getSelectedItem();
				final String choice2 = (String)arrayCombo.getSelectedItem();
				
				if(isHierarchical()) {
					linkageMethod = (String)clusterChoice.getSelectedItem();
				}
				
				//needs at least one box to be selected 
				//otherwise display error
				if(checkSelections(choice, choice2)) {
					
					loadPanel.removeAll();
					dendro_button.setEnabled(false);
					buttonPanel.remove(cluster_button);
					buttonPanel.remove(back_button);
					
					//ProgressBars
					pBar = ClusterView.setPBarLayout(pBar, 
							"Row Distance Matrix");
					
					pBar2 = ClusterView.setPBarLayout(pBar2, "Row Clustering");
					
					pBar3 = ClusterView.setPBarLayout(pBar3, 
							"Column Distance Matrix");
					
					pBar4 = ClusterView.setPBarLayout(pBar4, 
							"Column Clustering");
					
					//Button to cancel process
					cancel_button = new JButton("Cancel");
					cancel_button = ClusterView.setButtonLayout(cancel_button);
					cancel_button.addActionListener(new ActionListener(){

						@Override
						public void actionPerformed(ActionEvent arg0) {
							
							worker.cancel(true);
							
							loadPanel.removeAll();
							optionsPanel.remove(loadPanel);
							buttonPanel.remove(cancel_button);
							buttonPanel.remove(dendro_button);
							cluster_button.setEnabled(true);
							
							mainPanel.revalidate();
							mainPanel.repaint();	
						}
					});

					if(!choice.contentEquals("Do Not Cluster")
							&& !choice2.contentEquals("Do Not Cluster")) {
						loadPanel.add(pBar, "pushx, growx, span, wrap");
						loadPanel.add(pBar2, "pushx, growx, span, wrap");
						loadPanel.add(pBar3, "pushx, growx, span, wrap");
						loadPanel.add(pBar4, "pushx, growx, span, wrap");
						
					} else if(!choice.contentEquals("Do Not Cluster")) {
						loadPanel.add(pBar, "pushx, growx, span, wrap");
						loadPanel.add(pBar2, "pushx, growx, span, wrap");
						
					} else if(!choice2.contentEquals("Do Not Cluster")) {
						loadPanel.add(pBar3, "pushx, growx, span, wrap");
						loadPanel.add(pBar4, "pushx, growx, span, wrap");
					}
				
					optionsPanel.add(loadPanel, "push, growx, span, wrap");
					
					buttonPanel.add(cancel_button, "pushx, alignx 50%");
					buttonPanel.add(dendro_button, "pushx, alignx 50%");
					mainPanel.add(buttonPanel, "pushx, alignx 50%, " +
							"height 15%::");
					
					mainPanel.revalidate();
					mainPanel.repaint();

					//start new cluster process
					worker.execute();
					
				} else {
					loadPanel.removeAll();
					error1 = new JLabel("Woah, that's too quick!");
					error1.setFont(GUIParams.FONTS);
					error1.setForeground(GUIParams.RED1);
					
					String hint = "";
					String hint2 = "";
					
					if(isHierarchical()) {
						hint = "Select at least one similarity metric for " +
								"either rows or columns to begin clustering!";
						
					} else {
						hint = "Select at least one similarity metric for " +
								"either rows or columns to begin clustering!";
						
						hint2 = "The amount of clusters and iterations must" +
								"be greater than 0.";
					}
					
					error2 = new JLabel(hint);
					error2.setFont(GUIParams.FONTS);
					error2.setForeground(GUIParams.TEXT);
					
					error3 = new JLabel(hint2);
					error3.setFont(GUIParams.FONTS);
					error3.setForeground(GUIParams.TEXT);
					
					loadPanel.add(error1, "alignx 50%, span, wrap");
					loadPanel.add(error2, "alignx 50%, span, wrap");
					loadPanel.add(error3, "alignx 50%, span");
					optionsPanel.add(loadPanel, "alignx 50%, push, span");
					
					mainPanel.revalidate();
					mainPanel.repaint();
				}
			}	
    	});
    	
    	setOptionsPanel();
    	
    	dendro_button.setEnabled(false);
  		buttonPanel.add(back_button, "alignx 50%, pushx");
  		buttonPanel.add(cluster_button, "alignx 50%, pushx");
  		
  		setMainPanel();
		
		//Add the scrollPane to ClusterView2 Panel
		this.add(scrollPane, "grow, push");
		
		this.revalidate();
		this.repaint();
	}
	
	public void refresh() {
		
		setupLayout();
	}
	
	//Layout setups for some Swing elements
	/**
	 * Sets up general elements of the mainPanel
	 */
	public void setMainPanel() {
		
		mainPanel.add(head1, "alignx 50%, push, wrap");
		mainPanel.add(clusterType, "alignx 50%, pushx, wrap");
		mainPanel.add(optionsPanel, "push, alignx 50%, " +
				"width 70%:70%:70%, height 50%::, wrap");
		mainPanel.add(buttonPanel, "alignx 50%, height 15%::");
	}
	
	/**
	 * Sets up general elements of the optionsPanel
	 */
	public void setOptionsPanel() {
		
  		optionsPanel.add(emptyPanel, "pushx, height 10%::");
  		optionsPanel.add(head2, "alignx 50%, pushx, height 10%::");
  		optionsPanel.add(head3, "alignx 50%, pushx, height 10%::, wrap");
  		
  		optionsPanel.add(similarity, "pushx, height 20%::");
  		optionsPanel.add(geneCombo, "alignx 50%, pushx, growx");
  		optionsPanel.add(arrayCombo,"alignx 50%, pushx, growx, wrap");
  		
  		if(isHierarchical()) {
	  		
			optionsPanel.add(method, "pushx, height 15%::");
			optionsPanel.add(clusterChoice, "alignx 50%, growx, width 20%");
			optionsPanel.add(infoIcon, "pushx, wrap");
			
		} else {
			
			optionsPanel.add(clusters, "pushx, growx, height 15%::");
			optionsPanel.add(enterRC, "alignx 50%, pushx");
			optionsPanel.add(enterCC, "alignx 50%, pushx, wrap");
			
			optionsPanel.add(its, "pushx, height 15%::");
			optionsPanel.add(enterRIt, "alignx 50%, pushx");
			optionsPanel.add(enterCIt, "alignx 50%, pushx, wrap");
		}
	}
	
	/**
	 * Setting up a general layout for a button object
	 * The method is used to make all buttons appear consistent in aesthetics
	 * @param button
	 * @return
	 */
	public static JButton setButtonLayout(JButton button){
		
  		Dimension d = button.getPreferredSize();
  		d.setSize(d.getWidth() * 1.5, d.getHeight() * 1.5);
  		button.setPreferredSize(d);
  		
  		button.setFont(GUIParams.FONTS);
  		button.setOpaque(true);
  		button.setBackground(GUIParams.ELEMENT);
  		button.setForeground(GUIParams.BG_COLOR);
  		
  		return button;
	}
	
	/**
	 * Setting up a general layout for a ComboBox object
	 * The method is used to make all ComboBoxes appear consistent in aesthetics
	 * @param combo
	 * @return
	 */
	public static JComboBox setComboLayout(String[] combos){
		
		JComboBox comboBox = new JComboBox(combos);
		Dimension d = comboBox.getPreferredSize();
		d.setSize(d.getWidth() * 1.5, d.getHeight() * 1.5);
		comboBox.setPreferredSize(d);
		comboBox.setFont(GUIParams.FONTS);
		comboBox.setBackground(Color.white);
		
		return comboBox;
	}
	
	/**
	 * Method to setup a JProgressBar
	 * @param pBar
	 * @param text
	 * @return
	 */
	public static JProgressBar setPBarLayout(JProgressBar pBar, String text){
		
		pBar = new JProgressBar();
		pBar.setMinimum(0);
		pBar.setStringPainted(true);
		pBar.setMaximumSize(new Dimension(2000, 40));
		pBar.setForeground(GUIParams.ELEMENT);
		pBar.setUI(new BasicProgressBarUI(){
			
			@Override
			protected Color getSelectionBackground(){
				return GUIParams.PROGRESS1;
			};
			
			@Override
			protected Color getSelectionForeground(){
				return GUIParams.PROGRESS2;
			};
		});
		pBar.setString(text);
		pBar.setVisible(true);
		
		return pBar;
	}
	
	/**
	 * Method to setup the look of an editable TextField
	 * @return
	 */
	public JSpinner setupSpinner() {
		
		SpinnerNumberModel amountChoice = new SpinnerNumberModel(0, 0, 5000, 1);
		JSpinner jft = new JSpinner(amountChoice);
		
		Dimension d = jft.getPreferredSize();
		d.setSize(d.getWidth(), d.getHeight()*2);
		jft.setPreferredSize(d);
		jft.setFont(GUIParams.FONTS);
		
		return jft;
	}
	
	/**
	 * Returns the choice of the cluster drop down menu as a string
	 * @return
	 */
	public boolean isHierarchical() {
		
		String choice = (String)clusterType.getSelectedItem();
		hierarchical = choice.equalsIgnoreCase("Hierarchical Clustering");
		return hierarchical;
	}
	
	/**
	 * Checks whether all needed options are selected to perform clustering.
	 * @param choice
	 * @param choice2
	 * @return
	 */
	public boolean checkSelections(String choice, String choice2) {
		
		int clustersR = (Integer) enterRC.getValue();
		int itsR = (Integer) enterRIt.getValue();
		int clustersC = (Integer) enterCC.getValue();
		int itsC = (Integer) enterCIt.getValue();
		
		if(isHierarchical()) {
			if(!choice.contentEquals("Do Not Cluster")
					|| !choice2.contentEquals("Do Not Cluster")){
						return true;
						
					} else {
						return false;
					}	
		} else {
			if((!choice.contentEquals("Do Not Cluster") 
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
	 * @param filePath
	 */
	public void setPath(String filePath){
		
		path = filePath;
	}
	
	/**
	 * Setter for file
	 * @param cdtFile
	 */
	public void setFile(File cdtFile){
		
		file = cdtFile;
	}
	
	public JLabel getTitle() {
		
		return head1;
	}
	
	/**
	 * Get the mainPanel for reference
	 */
	public JPanel getMainPanel(){
		
		return mainPanel;
	}
	
	/**
	 * Get the optionsPanel for reference
	 */
	public JPanel getOptionsPanel(){
		
		return optionsPanel;
	}
	
	/**
	 * Get the buttonPanel for reference
	 */
	public JPanel getButtonPanel(){
		
		return buttonPanel;
	}
	
	/**
	 * Get the similarity measure choice for row clustering
	 * @return 
	 */
	public JComboBox getGeneCombo(){
		
		return geneCombo;
	}
	
	/**
	 * Get the similarity measure choice for column clustering
	 * @return 
	 */
	public JComboBox getArrayCombo(){
		
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
	
	
	//Setters
	/** Setter for viewFrame */
	public void setViewFrame(TreeViewFrame viewFrame) {
		
		this.viewFrame = viewFrame;
	}

	/** Setter for dataModel 
	 * 
	 * */
	protected void setDataModel(TVModel dataModel) {
		
		this.dataModel = dataModel;
	}

	/** 
	 * Setter for root  - may not work properly
	 * @param root
	 */
	public void setConfigNode(ConfigNode root) {
		
		this.root = root;
	}
	
	//Empty methods
	@Override
	public void populateSettingsMenu(TreeviewMenuBarI menubar) {}
	
	@Override
	public void populateAnalysisMenu(TreeviewMenuBarI menubar) {}
	
	@Override
	public void populateExportMenu(TreeviewMenuBarI menubar) {}
	
	@Override
	public void scrollToGene(int i) {}
	
	@Override
	public void scrollToArray(int i) {}
	
	@Override
	public ImageIcon getIcon() {
		
		return null;
	}
	
	@Override
	public void export(MainProgramArgs args) throws ExportException {}
	
	@Override
	public void syncConfig() {}

}
