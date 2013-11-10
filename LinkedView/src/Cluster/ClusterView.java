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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.ScrollPaneConstants;
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
	
	//Static Variables
	private static final long serialVersionUID = 1L;
	private static final Color BLUE1 = new Color(60, 180, 220, 255);
	private final static Color BG_COLOR = new Color(252, 252, 252, 255);
	
	//Two Font Sizes
	private static Font fontS = new Font("Sans Serif", Font.PLAIN, 18);
	private static Font fontL = new Font("Sans Serif", Font.PLAIN, 24);
	
	//Instance
	protected DataModel dataModel;
	protected ConfigNode root;
	
	private TreeViewFrame viewFrame;
	private TVDataMatrix matrix;
	private double[] dataArray;
	
	//Various GUI elements
	private JScrollPane scrollPane;
	private JPanel mainPanel;
	private JPanel optionsPanel;
	private JPanel buttonPanel;
	private JLabel head1;
	private JLabel head2;
	private JLabel head3;
//	private InitialPanel initialPanel;
	private DistanceOptionsPanel doPanel;
	private ClusterOptionsPanel clusterPanel;
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

	/**
	 * Chained constructor for the ClusterView object
	 * note this will reuse any existing MainView subnode of the documentconfig.
	 *
	 * @param  cVModel model this ClusterView is to represent
	 * @param  vFrame  parent ViewFrame of DendroView
	 */
	public ClusterView(DataModel cVModel, TreeViewFrame vFrame) {
		
		this(cVModel, null, vFrame, "Cluster View");
	}
	
	/**
	 * Second chained constructor
	 * @param cVModel
	 * @param root
	 * @param vFrame
	 */
	public ClusterView(DataModel cVModel, ConfigNode root, 
			TreeViewFrame vFrame) {
		
		this(cVModel, root, vFrame, "Cluster View");
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
			TreeViewFrame vFrame, String name) {
		
		super.setName(name);
		
		this.dataModel = dataModel;
		this.viewFrame = vFrame;
		
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
		this.setLayout(new MigLayout("ins 0"));
		this.setBackground(BG_COLOR);
		
		//Create background panel
		mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout("ins 0"));
		mainPanel.setBackground(BG_COLOR);
		
		//Background Panel for the Cluster Options
		optionsPanel = new JPanel();
		optionsPanel.setLayout(new MigLayout());
		optionsPanel.setBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		optionsPanel.setBackground(BG_COLOR);
		
		//Panel for the Buttons
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new MigLayout());
		buttonPanel.setOpaque(false);
		
		//header
		head1 = new JLabel("Options");
		head1.setFont(fontL);
		head1.setForeground(BLUE1);
		
		clusterType = new JComboBox(clusterNames);
		clusterType.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				
				String choice = (String)clusterType.getSelectedItem();
				
				mainPanel.removeAll();
				
				
				if(choice.equalsIgnoreCase("Hierarchical Clustering")) {
					clusterPanel = new ClusterOptionsPanel(
							ClusterView.this, true);
				} else {
					
					clusterPanel = new ClusterOptionsPanel(
							ClusterView.this, false);
				}
				
				optionsPanel.add(doPanel, "pushx, growx, wrap");
				optionsPanel.add(clusterPanel, "pushx, growx, wrap");
				
				mainPanel.add(head1, "alignx 50%, pushx, wrap");
				mainPanel.add(clusterType, "wrap");
				mainPanel.add(optionsPanel, "push, alignx 50%, " +
						"width 70%:70%:70%, height 50%::, wrap");
				
				mainPanel.add(buttonPanel, "alignx 50%, height 15%::");
				
				mainPanel.revalidate();
				mainPanel.repaint();
			}
			
		});
		
		//make mainpanel scrollable by adding it to scrollpane
		scrollPane = new JScrollPane(mainPanel, 
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, 
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		//Cluster Options Panel
		doPanel = new DistanceOptionsPanel();
		
		//Linkage Choice and ProgressBar
		clusterPanel = new ClusterOptionsPanel(
				ClusterView.this, true);
		
		optionsPanel.add(doPanel, "pushx, growx, wrap");
		optionsPanel.add(clusterPanel, "pushx, growx, wrap");
		
		mainPanel.add(head1, "alignx 50%, pushx, wrap");
		mainPanel.add(clusterType, "wrap");
		mainPanel.add(optionsPanel, "push, alignx 50%, " +
				"width 70%:70%:70%, height 50%::, wrap");
		
		mainPanel.add(buttonPanel, "alignx 50%, height 15%::");
		
		//Add the scrollPane to ClusterView2 Panel
		this.add(scrollPane, "grow, push");
	}
	
//	/**
//	 * Subclass to add the initial JPanel containing some info and the data
//	 * preview table to the background panel.
//	 * @author CKeil
//	 *
//	 */
//	class InitialPanel extends JPanel {	
//	  
//		private static final long serialVersionUID = 1L;
//		
//		//Instance variables
//		private int nRows; 
//		private int nCols; 
//		private int sumMatrix; 
//		private JLabel sumM; 
//		private JLabel label2; 
//		private JLabel label3; 
//		private JLabel numColLabel; 
//		private JLabel numRowLabel;
//		private JButton hcluster_button;
//		private JButton kmcluster_button;
//		private JButton closeButton;
//		private JPanel numPanel;
//	    
//		/**
//		 * Constructor
//		 * Setting up the layout of the panel.
//		 */
//		public InitialPanel() {
//			
//			this.setLayout(new MigLayout());
//			setOpaque(false);
//			
//	    	HeaderInfo infoArray = dataModel.getArrayHeaderInfo();
//	    	HeaderInfo infoGene = dataModel.getGeneHeaderInfo();
//	    	
//	    	nCols = infoArray.getNumHeaders();
//	    	nRows = infoGene.getNumHeaders();
//	   
//	    	label2 = new JLabel("Matrix Dimensions:");
//	    	label2.setFont(fontL);
//	    	
//	    	//Matrix Information
//	    	numPanel = new JPanel();
//	    	numPanel.setLayout(new MigLayout());
//	    	numPanel.setOpaque(false);
//	    	
//	    	numColLabel = new JLabel(nCols + " columns");
//	    	numColLabel.setFont(fontS);
//	    	numColLabel.setForeground(RED1);
//	    	
//	    	numRowLabel = new JLabel(nRows + " rows X ");
//	    	numRowLabel.setFont(fontS);
//	    	numRowLabel.setForeground(RED1);
//	    	
//	    	label3 = new JLabel("Data Points:");
//	    	label3.setFont(fontS);
//	    	
//	    	sumMatrix = nCols * nRows;
//	    	sumM = new JLabel(Integer.toString(sumMatrix));
//	    	sumM.setFont(fontS);
//	    	sumM.setForeground(RED1);
//	    	 
//	    	//Data Preview
//	    	DataViewPanel dataView = new DataViewPanel(dataModel);
//	    	
//	    	//Hierarchical Cluster Button
//	    	hcluster_button = new JButton("Hierarchical Cluster >");
//	    	hcluster_button = setButtonLayout(hcluster_button);
//			hcluster_button.addActionListener(new ActionListener(){
//	
//				@Override
//				public void actionPerformed(ActionEvent arg0) {
//					
//					mainPanel.removeAll();
//					buttonPanel.removeAll();
//					
//					//Cluster Options Panel
//					doPanel = new DistanceOptionsPanel();
//					
//					//Linkage Choice and ProgressBar
//					clusterPanel = new ClusterOptionsPanel(
//							ClusterView.this, true);
//					
//					head1.setText("Options");
//					
//					optionsPanel.add(doPanel, "pushx, growx, wrap");
//					optionsPanel.add(clusterPanel, "pushx, growx, wrap");
//					
//					mainPanel.add(head1, "alignx 50%, pushx, wrap");
//					mainPanel.add(optionsPanel, "push, alignx 50%, " +
//							"width 70%:70%:70%, height 50%::, wrap");
//					mainPanel.add(buttonPanel, "alignx 50%, height 15%::");
//					
//					mainPanel.revalidate();
//					mainPanel.repaint();
//				}
//	    	});
//			
//			kmcluster_button = new JButton("K-Means >");
//	    	kmcluster_button = setButtonLayout(kmcluster_button);
//			kmcluster_button.addActionListener(new ActionListener(){
//	
//				@Override
//				public void actionPerformed(ActionEvent arg0) {
//					
//					mainPanel.removeAll();
//					buttonPanel.removeAll();
//					
//					//Cluster Options Panel
//					doPanel = new DistanceOptionsPanel();
//					
//					//Linkage Choice and ProgressBar
//					clusterPanel = new ClusterOptionsPanel(
//							ClusterView.this, false);
//					
//					head1.setText("Options");
//					
//					optionsPanel.add(doPanel, "pushx, growx, wrap");
//					optionsPanel.add(clusterPanel, "pushx, growx, wrap");
//					
//					mainPanel.add(head1, "alignx 50%, pushx, wrap");
//					mainPanel.add(optionsPanel, "push, alignx 50%, " +
//							"width 70%:70%:70%, height 50%::, wrap");
//					mainPanel.add(buttonPanel, "alignx 50%, height 15%::");
//					
//					mainPanel.revalidate();
//					mainPanel.repaint();
//				}
//	    	});
//			
//			closeButton = new JButton("< Back");
//			setButtonLayout(closeButton);
//	  		closeButton.setBackground(BLUE1);
//	  		closeButton.addActionListener(new ActionListener(){
//
//				@Override
//				public void actionPerformed(ActionEvent arg0) {
//					
//					viewFrame.setLoaded(false);
//				}
//			});
//	  		
//	  		buttonPanel.add(closeButton, "alignx 50%, pushx");
//	    	buttonPanel.add(hcluster_button, "alignx 50%, pushx");
//	    	buttonPanel.add(kmcluster_button, "alignx 50%, pushx");
//	    	
//	    	numPanel.add(numRowLabel, "span, split 2, alignx 50%");
//	    	numPanel.add(numColLabel, "wrap");
//	    	numPanel.add(label3, "span, split 2, alignx 50%");
//	    	numPanel.add(sumM, "alignx 50%, wrap");
//	    	
//	    	this.add(label2, "alignx 50%, wrap");
//	    	this.add(numPanel, "alignx 50%, pushx, wrap");
//	    	this.add(dataView, "push, grow, alignx 50%, width 80%:95%:95%");
//		  }
//	}
	
	/**
	 * Subclass to be added to the background panel. It offers a choice of 
	 * similarity measures for both the rows and columns of the input matrix.
	 * @author CKeil
	 *
	 */
	class DistanceOptionsPanel extends JPanel {	
		
		private static final long serialVersionUID = 1L;
		
		private JPanel emptyPanel;
		
		/**
		 * Constructor
		 * Setting up layout of this panel.
		 */
		public DistanceOptionsPanel() {
			
			//Panel Layout
			this.setLayout(new MigLayout());
			this.setBackground(BG_COLOR);
			
			//Header
			similarity = new JLabel("Similarity Metric: ");
	  		similarity.setFont(fontL);
	  		similarity.setBackground(BG_COLOR);
	  		
	  		//filler component
	  		emptyPanel = new JPanel();
	  		emptyPanel.setLayout(new MigLayout());
	  		emptyPanel.setOpaque(false);
	  		
			//Labels
			head2 = new JLabel("Rows");
			head2.setFont(fontL);
			head2.setForeground(BLUE1);
			
			head3 = new JLabel("Columns");
			head3.setFont(fontL);
			head3.setForeground(BLUE1);
			 
			//Drop-down menu for row selection
	  		geneCombo = new JComboBox(measurements);
	  		geneCombo = setComboLayout(geneCombo);
		
			//Drop-down menu for column selection
	  		arrayCombo = new JComboBox(measurements);
	  		arrayCombo = setComboLayout(arrayCombo);
	  		
	  		this.add(emptyPanel, "pushx");
	  		this.add(head2, "pushx");
	  		this.add(head3, "pushx, wrap");
	  		
	  		this.add(similarity, "pushx");
	  		this.add(geneCombo, "growx, pushx");
	  		this.add(arrayCombo,"growx, pushx");
		}
	}	
	
	//Layout setups for some Swing elements
	/**
	 * Setting up a general layout for a button object
	 * The method is used to make all buttons appear consistent in aesthetics
	 * @param button
	 * @return
	 */
	public static JButton setButtonLayout(JButton button){
		
  		Dimension d = button.getPreferredSize();
  		d.setSize(d.getWidth()*1.5, d.getHeight()*1.5);
  		button.setPreferredSize(d);
  		
  		button.setFont(fontS);
  		button.setOpaque(true);
  		button.setBackground(BLUE1);
  		button.setForeground(Color.white);
  		
  		return button;
	}
	
	/**
	 * Setting up a general layout for a ComboBox object
	 * The method is used to make all ComboBoxes appear consistent in aesthetics
	 * @param combo
	 * @return
	 */
	public static JComboBox setComboLayout(JComboBox combo){
		
		Dimension d = combo.getPreferredSize();
		d.setSize(d.getWidth(), d.getHeight()*1.5);
		combo.setPreferredSize(d);
		combo.setFont(fontS);
		combo.setBackground(Color.white);
		
		return combo;
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
		pBar.setForeground(BLUE1);
		pBar.setUI(new BasicProgressBarUI(){
			@Override
			protected Color getSelectionBackground(){return Color.black;};
			@Override
			protected Color getSelectionForeground(){return Color.white;};
		});
		pBar.setString(text);
		pBar.setVisible(true);
		
		return pBar;
	}
	
	
	//Getters
	public Font getSmallFont() {
		
		return fontS;
	}
	
	public Font getLargeFont() {
		
		return fontL;
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
	
//	/**
//	 * Get the optionsPanel for reference
//	 */
//	public InitialPanel getInitialPanel(){
//		
//		return initialPanel;
//	}
	
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
	 * Get the finalPanel for reference
	 */
	public ClusterOptionsPanel getFinalPanel(){
		
		return clusterPanel;
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
	
//	/**
//	 * Setting up a new InitialPanel
//	 */
//	public void setInitialPanel() {
//		
//		initialPanel = new InitialPanel();
//	}
	
	/**
	 * Setting up a new finalPanel
	 */
	public void setKMPanel() {
		
		clusterPanel = new ClusterOptionsPanel(this, false);
	}
	
	/**
	 * Setting up a new finalPanel
	 */
	public void setHPanel() {
		
		clusterPanel = new ClusterOptionsPanel(this, true);
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
