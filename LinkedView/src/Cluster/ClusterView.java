/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: DendroView.java,v $
 * $Revision: 1.7 $
 * $Date: 2009-03-23 02:46:51 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular, 
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
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
import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;

import net.miginfocom.swing.MigLayout;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.MainProgramArgs;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;


import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LoadException;

import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.ViewFrame;
//Explicitly imported because error (unclear TVModel reference) was thrown

/**
 *  This class exists to internalize the clustering process directly into TreeView. 
 *  It provides a GUI which is called from the slightly adjusted original Java TreeView menubar.
 *  The GUI content is loaded into the program in a similar manner as the DendroView class.
 *
 * @author    	Chris Keil <ckeil@princeton.edu>
 * @version 	0.1
 */
public class ClusterView extends JPanel implements MainPanel {

	private static final long serialVersionUID = 1L;
	
	//Instance variables
	private TreeViewFrame viewFrame;
	protected DataModel dataModel;
	
	//Object of the loaded model and matrix 
	private ClusterModel outer;
	private ClusterModel.ClusterDataMatrix matrix;
	private double[] dataArray;
	
	//Various GUI elements
	private JScrollPane scrollPane;
	private JPanel mainPanel, optionsPanel, buttonPanel;
	private HeaderPanel head1, head2, head3;
	private InitialPanel initialPanel;
	private ClusterOptionsPanel coPanel;
	private FinalOptionsPanel finalPanel;
	private JComboBox<String> geneCombo, arrayCombo;
	
	//label used by 2 classes
	private JLabel similarity;
	
	//Options for comboboxes used by 2 classes
	private final String[] measurements = {"Do Not Cluster", "Pearson Correlation (uncentered)", 
			"Pearson Correlation (centered)", "Absolute Correlation (uncentered)", 
			"Absolute Correlation (centered)", "Spearman Ranked Correlation", "Euclidean Distance", "City Block Distance"};
	/**
	 * Colors
	 */
	private final Color BLUE1 = new Color(60, 180, 220, 255);
	private final Color BLUE2 = new Color(110, 210, 255, 150);
	private final Color RED1 = new Color(240, 80, 50, 255);
	private final Color BG_COLOR = new Color(230, 230, 230, 255);
	
	/**
	 * Constructor for the DendroView object
	 * note this will reuse any existing MainView subnode of the documentconfig.
	 *
	 * @param  cVModel model this ClusterView is to represent
	 * @param  vFrame  parent ViewFrame of DendroView
	 */
	public ClusterView(DataModel cVModel, TreeViewFrame vFrame) {
		
		this(cVModel, null, vFrame, "Cluster View");
	}
	
	public ClusterView(DataModel cVModel, ConfigNode root, TreeViewFrame vFrame) {
		
		this(cVModel, root, vFrame, "Cluster View");
	}
	/**
	 *  Constructor for the DendroView object which binds to an explicit confignode
	 *
	 * @param  dataModel   model this DendroView is to represent
	 * @param  root   Confignode to which to bind this DendroView
	 * @param  vFrame  parent ViewFrame of ClusterView
	 * @param  name name of this view.
	 */
	public ClusterView(DataModel dataModel, ConfigNode root, TreeViewFrame vFrame, String name) {
		
		super.setName(name);
		this.setLayout(new MigLayout("ins 0"));
		this.setBackground(Color.white);
		
		viewFrame = vFrame;
		
		//Reference to loaded data
		this.dataModel = dataModel;
		outer = (ClusterModel) dataModel;
		matrix = outer.getDataMatrix();
		dataArray = matrix.getExprData();
		
		//setting the UIManager up for Win vs. Mac
		try{
			
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} 
		catch (Exception e){
			
			e.printStackTrace();
		}

		viewFrame.setResizable(true);
		
		//set layout for initial window
		mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout("ins 0"));
		mainPanel.setBackground(BG_COLOR);
		
		//Background Panel for the Cluster Options
		optionsPanel = new JPanel();
		optionsPanel.setLayout(new MigLayout());
		optionsPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		optionsPanel.setBackground(Color.white);
		
		//Panel for the Buttons
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new MigLayout());
		buttonPanel.setOpaque(false);
		
		//header
		head1 = new HeaderPanel("Cluster ", " Data Preview");
		head1.setColor(Color.BLACK);
		
		//Data Info Panel
		initialPanel = new InitialPanel();
		
		//make mainpanel scrollable by adding it to scrollpane
		scrollPane = new JScrollPane(mainPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		//Add components to mainPanel
		mainPanel.add(head1, "push, alignx 50%, height 8%:8%:8%, wrap");
		mainPanel.add(initialPanel, "grow, pushx, span, wrap");
		mainPanel.add(buttonPanel, "alignx 50%, height 15%::");
		
		//Add the scrollPane to ClusterView2 Panel
		this.add(scrollPane, "grow, push");
	}
	
	class HeaderPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		
		private JLabel text;
		private JLabel text2;
		
		public HeaderPanel(String header, String header2){
			
			this.setLayout(new MigLayout());
			setOpaque(false);
			
			text = new JLabel(header);
			text.setFont(new Font("Sans Serif", Font.PLAIN, 44));
			
			text2 = new JLabel(header2);
			text2.setFont(new Font("Sans Serif", Font.PLAIN, 44));
			text2.setForeground(BLUE1);
			
			this.add(text, "pushx, alignx 50%");
			this.add(text2, "pushx, alignx 50%");
		}
		
		public HeaderPanel(String header){
			
			this.setLayout(new MigLayout());
			setOpaque(false);
			
			text = new JLabel(header);
			text.setFont(new Font("Sans Serif", Font.PLAIN, 44));
			text.setForeground(BLUE1);
			
			this.add(text, "pushx, alignx 50%");
		}
		
		public void setColor(Color color){
			
			text.setForeground(color);
		}
		
		public void setText(String newText){
			
			text2.setText(newText);
		}
		
		public void setSmall(){
			text.setFont(new Font("Sans Serif", Font.PLAIN, 22));
		}
	}
	
	class InitialPanel extends JPanel {	
	  
		private static final long serialVersionUID = 1L;
		
		//Instance variables
		private int nRows, nCols, sumMatrix; 
		private JLabel sumM, label2, label3, numColLabel, numRowLabel;
		private JButton cluster_button, closeButton;
		private JPanel numPanel;
	    
		//Constructor
		public InitialPanel() {
			
			this.setLayout(new MigLayout());
			setOpaque(false);
			
	    	HeaderInfo infoArray = dataModel.getArrayHeaderInfo();
	    	HeaderInfo infoGene = dataModel.getGeneHeaderInfo();
	    	
	    	nCols = infoArray.getNumHeaders();
	    	nRows = infoGene.getNumHeaders();
	   
	    	label2 = new JLabel("Matrix Dimensions:");
	    	label2.setFont(new Font("Sans Serif", Font.BOLD, 25));
	    	
	    	//Matrix Information
	    	numPanel = new JPanel();
	    	numPanel.setLayout(new MigLayout());
	    	numPanel.setOpaque(false);
	    	
	    	numColLabel = new JLabel(nCols + " columns");
	    	numColLabel.setFont(new Font("Sans Serif", Font.BOLD, 20));
	    	numColLabel.setForeground(RED1);
	    	
	    	numRowLabel = new JLabel(nRows + " rows X ");
	    	numRowLabel.setFont(new Font("Sans Serif", Font.BOLD, 20));
	    	numRowLabel.setForeground(RED1);
	    	
	    	label3 = new JLabel("Data Points:");
	    	label3.setFont(new Font("Sans Serif", Font.PLAIN, 18));
	    	
	    	sumMatrix = nCols * nRows;
	    	sumM = new JLabel(Integer.toString(sumMatrix));
	    	sumM.setFont(new Font("Sans Serif", Font.PLAIN, 18));
	    	sumM.setForeground(RED1);
	    	 
	    	//Data Preview
	    	DataViewPanel dataView = new DataViewPanel(outer);
	    	
	    	cluster_button = new JButton("Hierarchical Cluster >");
	    	cluster_button = setButtonLayout(cluster_button);
			cluster_button.addActionListener(new ActionListener(){
	
				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					mainPanel.removeAll();
					buttonPanel.removeAll();
					
					//Cluster Options Panel
					coPanel = new ClusterOptionsPanel();
					
					//Linkage Choice and ProgressBar
					finalPanel = new FinalOptionsPanel();
					
					head1.setText("Options");
					
					optionsPanel.add(coPanel, "pushx, alignx 50%, wrap");
					optionsPanel.add(finalPanel, "pushx, alignx 50%, wrap");
					
					mainPanel.add(head1, "alignx 50%, pushx, wrap");
					mainPanel.add(optionsPanel, "push, alignx 50%, width 70%:70%:70%, height 50%::, wrap");
					mainPanel.add(buttonPanel, "alignx 50%, height 15%::");
					
					mainPanel.revalidate();
					mainPanel.repaint();
					
				}
	    	});
			
			closeButton = new JButton("Close");
			setButtonLayout(closeButton);
	  		closeButton.setBackground(RED1);
	  		closeButton.addActionListener(new ActionListener(){

				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					viewFrame.setLoaded(false);
				}
			});
	  		
	  		buttonPanel.add(closeButton, "alignx 50%, pushx");
	    	buttonPanel.add(cluster_button, "alignx 50%, pushx");
	    	
	    	numPanel.add(numRowLabel, "span, split 2, alignx 50%");
	    	numPanel.add(numColLabel, "wrap");
	    	numPanel.add(label3, "span, split 2, alignx 50%");
	    	numPanel.add(sumM, "alignx 50%, wrap");
	    	
	    	this.add(label2, "alignx 50%, wrap");
	    	this.add(numPanel, "alignx 50%, pushx, wrap");
	    	this.add(dataView, "push, grow, alignx 50%, width 80%:95%:95%");
		  }
	}
	
	class ClusterOptionsPanel extends JPanel {	
		
//		private ClusterFrame clusterDialog;
//		private JButton advanced_button;
		private JPanel rowPanel, colPanel;
		
		private static final long serialVersionUID = 1L;
			    
		public ClusterOptionsPanel() {
			
			//Panel Layout
			this.setLayout(new MigLayout());
			this.setBackground(Color.white);
			
			//Header
			similarity = new JLabel("Similarity Metric");
	  		similarity.setFont(new Font("Sans Serif", Font.PLAIN, 26));
	  		similarity.setBackground(Color.white);
	  		
	  		//Component for Row Distance Measure Selection
	  		rowPanel = new JPanel();
	  		rowPanel.setLayout(new MigLayout());
	  		rowPanel.setOpaque(false);
	  		
	  		//Component for Column Distance Measure Selection
	  		colPanel = new JPanel();
	  		colPanel.setLayout(new MigLayout());
	  		colPanel.setOpaque(false);
	  		
			//Labels
			head2 = new HeaderPanel("Rows");
			head2.setSmall();
			head2.setBackground(BLUE2);
			
			head3 = new HeaderPanel("Columns");
			head3.setSmall();
			head3.setBackground(BLUE2);
			 
			//Drop-down menu for row selection
	  		geneCombo = new JComboBox<String>(measurements);
	  		geneCombo = setComboLayout(geneCombo);
		
			//Drop-down menu for column selection
	  		arrayCombo = new JComboBox<String>(measurements);
	  		arrayCombo = setComboLayout(arrayCombo);
	  		
	  		rowPanel.add(head2, "alignx 50%, span, wrap");
	  		rowPanel.add(geneCombo, "alignx 50%, grow, push");
	  		colPanel.add(head3, "alignx 50%, span, wrap");
			colPanel.add(arrayCombo,"alignx 50%, grow, push");
			
			this.add(similarity, "alignx 50%, span, wrap");
			this.add(rowPanel, "growx, pushx");
			this.add(colPanel, "growx, pushx");

//			//Advanced Options Button
//			advanced_button = new JButton("Advanced Options >>");
//			advanced_button = setButtonlayout(advanced_button);
//			advanced_button.addActionListener(new ActionListener(){
//		
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				
//				clusterDialog = new ClusterFrameWindow(viewFrame, outer);
//				clusterDialog.setVisible(true);
//				
//				}
//			});
		}
	}
		
	class FinalOptionsPanel extends JPanel {	
			  
		private static final long serialVersionUID = 1L;
			
		//Instance variables
		private JButton cluster_button, back_button, cancel_button, dendro_button;
		private JComboBox<String> clusterChoice;
		private JLabel status1, status2, method, error1, error2, opLabel;
		private JProgressBar pBar, pBar2, pBar3, pBar4, pBar5;
		private final JPanel loadPanel, choicePanel;
		private final SwingWorker<Void, Void> worker;
		private final String[] clusterMethods = {"Single Linkage", "Centroid Linkage", "Average Linkage", "Complete Linkage"};
		private String path, clusterMethod;
		private File file;
		    
		//Constructor
		public FinalOptionsPanel() {
			
			this.setLayout(new MigLayout());
			this.setBackground(Color.white);
			
			//worker thread for calculation off the EDT to give Swing elements time to update
			worker = new SwingWorker<Void, Void>() {	
				
				@Override
				public Void doInBackground() {

		        	try {
		        		
		        		HierarchicalCluster clusterTarget = 
		        				new HierarchicalCluster(outer, viewFrame, 
		        						ClusterView.this, pBar, pBar2, pBar3, 
		        						pBar4, opLabel, dataArray);
		        		
		        		clusterTarget.hCluster(clusterMethod);
		        		
					} catch (InterruptedException e) {
					
						e.printStackTrace();
					} catch (ExecutionException e) {
						
						e.printStackTrace();
					}
					return null;
				}
					
				@Override
				protected void done(){
					
					loadPanel.remove(opLabel);
					buttonPanel.remove(cancel_button);
					
					status1 = new JLabel("The file has been saved in the original directory.");
					status1.setFont(new Font("Sans Serif", Font.PLAIN, 18));
					
					status2 = new JLabel("File Path: " + path);
					status2.setFont(new Font("Sans Serif", Font.ITALIC, 18));
					
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
			
			//Panel containing the Linkage Choice
			choicePanel = new JPanel();
			choicePanel.setLayout(new MigLayout());
			choicePanel.setBackground(Color.white);
			
			//Label
			method = new JLabel("Linkage Method");
			method.setFont(new Font("Sans Serif", Font.PLAIN, 26));
	    	
			//Linkage choice drop-down menu
			clusterChoice = new JComboBox<String>(clusterMethods);
			clusterChoice = setComboLayout(clusterChoice);
			
			//ProgressBar Component
			loadPanel = new JPanel();
			loadPanel.setLayout(new MigLayout());
			loadPanel.setBackground(Color.white);
			loadPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
	    	
	  		//Button to go back to data preview
	    	back_button = new JButton("< Back");
	    	back_button = setButtonLayout(back_button);
			back_button.addActionListener(new ActionListener(){
	
				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					mainPanel.removeAll();
					optionsPanel.removeAll();
					buttonPanel.removeAll();
					
					head1.setText("Data Preview");
					
					initialPanel = new InitialPanel();
					
					mainPanel.add(head1, "alignx 50%, pushx, wrap");
					mainPanel.add(initialPanel, "grow, push, span, wrap");
					mainPanel.add(buttonPanel, "alignx 50%, height 15%::");
					
					mainPanel.revalidate();
					mainPanel.repaint();
					
				}
	    	});
			
			//Button to show DendroView
	    	dendro_button = new JButton("Clustergram > ");
	    	dendro_button = setButtonLayout(dendro_button);
	  		dendro_button.addActionListener(new ActionListener(){
		    		
				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					FileSet fileSet = new FileSet(file.getName(), file.getParent()+
							 File.separator);
					try {
						
						viewFrame.loadFileSet(fileSet);
						
					} catch (LoadException e) {
						
						e.printStackTrace();
					}
					viewFrame.setLoaded(true);
				}
	    	});
	    	
	    	//Button to begin Clustering
	    	cluster_button = new JButton("Cluster");
	    	cluster_button = setButtonLayout(cluster_button);
	    	cluster_button.addActionListener(new ActionListener(){
		    		
				@Override
				public void actionPerformed(ActionEvent arg0) {
						
					final String choice = (String)geneCombo.getSelectedItem();
					final String choice2 = (String)arrayCombo.getSelectedItem();
					clusterMethod = (String)clusterChoice.getSelectedItem();
					
					//needs at least one box to be selected otherwise display error
					if(!choice.contentEquals("Do Not Cluster")||!choice2.contentEquals("Do Not Cluster")){
						
						loadPanel.removeAll();
						buttonPanel.remove(cluster_button);
						buttonPanel.remove(back_button);
						
						//precise ProgressBar
						pBar = new JProgressBar();
						pBar = setPBarLayout(pBar);
						pBar.setString("Row Distance Matrix");
						
						//overall ProgressBar
						pBar2 = new JProgressBar();
						pBar2 = setPBarLayout(pBar2);
						pBar2.setString("Row Clustering");
						
						//overall ProgressBar
						pBar3 = new JProgressBar();
						pBar3 = setPBarLayout(pBar3);
						pBar3.setString("Column Distance Matrix");
						
						//overall ProgressBar
						pBar4 = new JProgressBar();
						pBar4 = setPBarLayout(pBar4);
						pBar4.setString("Column Clustering");
						
						//overall ProgressBar
						pBar5 = new JProgressBar();
						pBar5 = setPBarLayout(pBar5);
						pBar5.setString("Saving");
						
						//Status label
						opLabel = new JLabel();
						
						cancel_button = new JButton("Cancel");
						cancel_button = setButtonLayout(cancel_button);
						cancel_button.addActionListener(new ActionListener(){
	
							@Override
							public void actionPerformed(ActionEvent arg0) {
								
								worker.cancel(true);
								buttonPanel.removeAll();
								optionsPanel.remove(finalPanel);
								optionsPanel.remove(loadPanel);
								finalPanel = new FinalOptionsPanel();
								optionsPanel.add(finalPanel, "pushx, alignx 50%, wrap");
								
								mainPanel.revalidate();
								mainPanel.repaint();	
							}
							
						});
						
//						loadPanel.add(opLabel, "alignx 50%, wrap");

						if(!choice.contentEquals("Do Not Cluster")&& !choice2.contentEquals("Do Not Cluster")){
							
							loadPanel.add(pBar, "pushx, growx, span, wrap");
							loadPanel.add(pBar2, "pushx, growx, span, wrap");
							loadPanel.add(pBar3, "pushx, growx, span, wrap");
							loadPanel.add(pBar4, "pushx, growx, span, wrap");
						}
						else if(!choice.contentEquals("Do Not Cluster")){
							
							loadPanel.add(pBar, "pushx, growx, span, wrap");
							loadPanel.add(pBar2, "pushx, growx, span, wrap");
						}
						else if(!choice2.contentEquals("Do Not Cluster")){
							
							loadPanel.add(pBar3, "pushx, growx, span, wrap");
							loadPanel.add(pBar4, "pushx, growx, span, wrap");
						}
						
//						loadPanel.add(pBar5, "pushx, growx, span, wrap");
						
						optionsPanel.add(loadPanel, "pushx, growx, span, wrap");
						
						buttonPanel.add(cancel_button, "pushx, alignx 50%");
						buttonPanel.add(dendro_button, "pushx, alignx 50%");
						mainPanel.add(buttonPanel, "pushx, alignx 50%, height 15%::");
						
						mainPanel.revalidate();
						mainPanel.repaint();
	
						//start new cluster process
						worker.execute();
					}
					//display error message
					else{
						
						error1 = new JLabel("Woah, that's too quick!");
						error1.setFont(new Font("Sans Serif", Font.PLAIN, 22));
						error1.setForeground(new Color(240, 80, 50, 255));
						
						error2 = new JLabel("Please select either a similarity metric for rows, " +
								"columns, or both to begin clustering!");
						error2.setFont(new Font("Sans Serif", Font.PLAIN, 22));
						
						loadPanel.add(error1, "alignx 50%, span, wrap");
						loadPanel.add(error2, "alignx 50%, span");
						optionsPanel.add(loadPanel, "alignx 50%, pushx, span");
						
						mainPanel.revalidate();
						mainPanel.repaint();
					}
				}	
	    	});
	    	
	    	choicePanel.add(method, "alignx 50%, pushx, wrap");
	    	choicePanel.add(clusterChoice, "alignx 50%, wrap");
	    	
	    	dendro_button.setEnabled(false);
	  		buttonPanel.add(back_button, "alignx 50%, pushx");
	  		buttonPanel.add(cluster_button, "alignx 50%, pushx");
	  		
	    	this.add(choicePanel, "alignx 50%, push, grow, wrap");
		}
		
		public void setPath(String filePath){
			path = filePath;
		}
		
		public void setFile(File cdtFile){
			
			file = cdtFile;
		}
		
	}	
	
	/**
	* Open a dialog for cluster program 
	* which allows the user to select a new data file
	*
	* @return The fileset corresponding to the dataset.
	*/
	protected ClusterFileSet clusterSelection()
	 throws LoadException
	 {
		 ClusterFileSet fileSet1; // will be chosen...
		 
		 JFileChooser fileDialog = new JFileChooser();
		 setupClusterFileDialog(fileDialog);
		 int retVal = fileDialog.showOpenDialog(this);
		 if (retVal == JFileChooser.APPROVE_OPTION) {
			 File chosen = fileDialog.getSelectedFile();
			 
			 fileSet1 = new ClusterFileSet(chosen.getName(), chosen.getParent()+
					 File.separator);
			 
		 } else {
			 throw new LoadException("File Dialog closed without selection...", 
					 LoadException.NOFILE);
		 }
		 
		 return fileSet1;
	 }
	
	 protected void setupClusterFileDialog(JFileChooser fileDialog) {
		//TxtFilter ff = new TxtFilter();
		try {
			//fileDialog.addChoosableFileFilter(ff);
			// will fail on pre-1.3 swings
			fileDialog.setAcceptAllFileFilterUsed(true);
		} catch (Exception e) {
			// hmm... I'll just assume that there's no accept all.
			fileDialog.addChoosableFileFilter(new javax.swing.filechooser
					.FileFilter() {
				@Override
				public boolean accept (File f) {
					return true;
				}
				@Override
				public String getDescription () {
					return "All Files";
				}
			});
		}
		//fileDialog.setFileFilter(ff);
		fileDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
	 }

	
	/**
	 * Get the finalPanel for reference in clustering class
	 */
	public JPanel getMainPanel(){
		
		return mainPanel;
	}
	
	/**
	 * Get the finalPanel for reference in clustering class
	 */
	public FinalOptionsPanel getFinalPanel(){
		
		return finalPanel;
	}
	
	/**
	 * Get the similarity measure choice for row clustering
	 * @return 
	 */
	public JComboBox<String> getGeneCombo(){
		
		return geneCombo;
	}
	
	/**
	 * Get the similarity measure choice for column clustering
	 * @return 
	 */
	public JComboBox<String> getArrayCombo(){
		
		return arrayCombo;
	}
	
	/** Setter for viewFrame */
	public void setViewFrame(TreeViewFrame viewFrame) {
		this.viewFrame = viewFrame;
	}
	/** Getter for viewFrame */
	public ViewFrame getViewFrame() {
		return viewFrame;
	}

	/** Setter for dataModel 
	 * 
	 * */
	protected void setDataModel(DataModel dataModel) {
		this.dataModel = dataModel;
	}
	/** 
	 * 	* gets the model this dendroview is based on
	 */
	protected DataModel getDataModel() {
		return this.dataModel;
	}

	protected ConfigNode root;
	/** Setter for root  - may not work properly
	public void setConfigNode(ConfigNode root) {
		this.root = root;
	}
	/** Getter for root */
	@Override
	public ConfigNode getConfigNode() {
		return root;
	}
	
	/**
	 * Setting up a general layout for a button object
	 * The method is used to make all buttons appear consistent in aesthetics
	 * @param button
	 * @return
	 */
	public JButton setButtonLayout(JButton button){

		Font buttonFont = new Font("Sans Serif", Font.PLAIN, 20);
		
  		Dimension d = button.getPreferredSize();
  		d.setSize(d.getWidth()*1.5, d.getHeight()*1.5);
  		button.setPreferredSize(d);
  		
  		button.setFont(buttonFont);
  		button.setOpaque(true);
  		button.setBackground(new Color(60, 180, 220, 255));
  		button.setForeground(Color.white);
  		
  		return button;
	}
	
	/**
	 * Setting up a general layout for a ComboBox object
	 * The method is used to make all ComboBoxes appear consistent in aesthetics
	 * @param combo
	 * @return
	 */
	public JComboBox<String> setComboLayout(JComboBox<String> combo){
		
		Dimension d = combo.getPreferredSize();
		d.setSize(d.getWidth()*1.5, d.getHeight()*1.5);
		combo.setPreferredSize(d);
		combo.setFont(new Font("Sans Serif", Font.PLAIN, 18));
		combo.setBackground(Color.white);
		
		return combo;
	}
	
	/**
	 * Method to setup a JProgressBar
	 * @param pBar
	 * @param text
	 * @return
	 */
	public JProgressBar setPBarLayout(JProgressBar pBar){
		
		pBar.setMinimum(0);
		pBar.setStringPainted(true);
		pBar.setForeground(BLUE1);
		pBar.setUI(new BasicProgressBarUI(){
			@Override
			protected Color getSelectionBackground(){return Color.black;};
			@Override
			protected Color getSelectionForeground(){return Color.white;};
		});
		pBar.setVisible(true);
		
		return pBar;
	}
	
	@Override
	public void populateSettingsMenu(TreeviewMenuBarI menubar) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void populateAnalysisMenu(TreeviewMenuBarI menubar) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void populateExportMenu(TreeviewMenuBarI menubar) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void scrollToGene(int i) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void scrollToArray(int i) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public ImageIcon getIcon() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void export(MainProgramArgs args) throws ExportException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void syncConfig() {
		// TODO Auto-generated method stub
		
	}

}
