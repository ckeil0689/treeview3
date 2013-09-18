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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

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
import edu.stanford.genetics.treeview.LogBuffer;

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
	
	//Instance variable in which the loaded data array is being stored
	private double[] dataArray;
	
	//Various GUI elements
	private JScrollPane scrollPane;
	private JPanel mainPanel = new JPanel();
	private HeaderPanel head1, head2, head3;
	private InitialPanel initialPanel;
	private GeneClusterPanel geneClusterPanel;
	private ArrayClusterPanel arrayClusterPanel;
	private ClusterOptionsPanel coPanel;
	private FinalOptionsPanel finalPanel;
	
	private JCheckBox weightGeneCheck, weightArrayCheck;
	private JComboBox<String> geneCombo, arrayCombo;
	
	//label used by 2 classes
	private JLabel similarity;
	
	//Options for comboboxes used by 2 classes
	private String[] measurements = {"Do Not Cluster", "Pearson Correlation (uncentered)", 
			"Pearson Correlation (centered)", "Absolute Correlation (uncentered)", 
			"Absolute Correlation (centered)", "Euclidean Distance", "City Block Distance"};
	
	/**
	 *  Constructor for the DendroView object
	 * note this will reuse any existing MainView subnode of the documentconfig.
	 *
	 * @param  tVModel   model this DendroView is to represent
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
		viewFrame = vFrame;

		this.dataModel = dataModel;
		outer = (ClusterModel) dataModel;
		matrix = outer.getDataMatrix();
		dataArray = matrix.getExprData();

		viewFrame.setResizable(true);
		
		this.setLayout(new MigLayout());
		
		//set layout for initial window
		mainPanel.setLayout(new MigLayout());
		
		//header
		head1 = new HeaderPanel("Hierarchical Clustering");
		head1.setColor(Color.black);
		mainPanel.add(head1, "pushx, growx, span, wrap");
		
		//Data Info Panel
		initialPanel = new InitialPanel();
		mainPanel.add(initialPanel, "growx, pushx, span, wrap");
		
		//Cluster Options Panel
		coPanel = new ClusterOptionsPanel();
		mainPanel.add(coPanel, "growx, pushx, span, wrap");
		
		finalPanel = new FinalOptionsPanel();
		mainPanel.add(finalPanel, "growx, pushx");
		
		mainPanel.setBackground(new Color(240, 240, 240, 255));
		
		//make mainpanel scrollable by adding it to scrollpane
		scrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		//Add the mainPanel to the ContentPane
		this.add(scrollPane, "grow, push");
		
	}
	
	class HeaderPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		
		private JLabel text;
		
		public HeaderPanel(String header){
			
			this.setLayout(new MigLayout());
			setOpaque(false);
			
			text = new JLabel(header);
			text.setFont(new Font("Sans Serif", Font.PLAIN, 36));
			text.setForeground(new Color(60, 180, 220, 255));
			
			this.add(text);
			
		}
		
		public void setColor(Color color){
			
			text.setForeground(color);
		}
		
		public void setSmall(){
			text.setFont(new Font("Sans Serif", Font.PLAIN, 22));
		}
	}
	
	class InitialPanel extends JPanel {	
	  
		private static final long serialVersionUID = 1L;
		
		//Instance variables
		private int nRows, nCols, sumMatrix; 
		private JLabel label1, label2, label3, numColLabel, numRowLabel;
		private JButton loadNew_button, view_button;
	    
		//Constructor
		public InitialPanel() {
			this.setLayout(new MigLayout());
			//this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
			setOpaque(false);
			
	    	HeaderInfo infoArray = dataModel.getArrayHeaderInfo();
	    	HeaderInfo infoGene = dataModel.getGeneHeaderInfo();
	    	
	    	nCols = infoArray.getNumHeaders();
	    	nRows = infoGene.getNumHeaders();
	    	
	    	label1 = new JLabel();
	    	label1.setFont(new Font("Sans Serif", Font.PLAIN, 20));
	    	if(dataModel.isLoaded()){
	    		
	    		label1.setText("Loading file successful!");
	    	}
	    	else{
	    		
	    		label1.setText("File loading unsuccessful :(");
	    	}
	    	this.add(label1, "top, wrap");
	   
	    	
	    	label2 = new JLabel("Matrix Dimensions:");
	    	label2.setFont(new Font("Sans Serif", Font.BOLD, 20));
	    	this.add(label2, "alignx 50%, wrap");
	    	
	    	//panel with dimensions of the dataMatrix
	    	JPanel numPane = new JPanel();
	    	numPane.setLayout(new MigLayout());
	    	numPane.setOpaque(false);
	    	
	    	numColLabel = new JLabel(nCols + " columns");
	    	numColLabel.setFont(new Font("Sans Serif", Font.BOLD, 20));
	    	numColLabel.setForeground(new Color(240, 80, 50, 255));
	    	
	    	numRowLabel = new JLabel(nRows + " rows X ");
	    	numRowLabel.setForeground(new Color(240, 80, 50, 255));
	    	numRowLabel.setFont(new Font("Sans Serif", Font.BOLD, 20));
	    	
	    	numPane.add(numRowLabel, "span, split 2, alignx 50%");
	    	numPane.add(numColLabel, "wrap");
	    	
	    	label3 = new JLabel("Data Points:");
	    	label3.setFont(new Font("Sans Serif", Font.PLAIN, 18));
	    	numPane.add(label3, "span, split 2, alignx 50%");
	    	
	    	sumMatrix = nCols*nRows;
	    	JLabel sumM = new JLabel(Integer.toString(sumMatrix));
	    	sumM.setForeground(new Color(240, 80, 50, 255));
	    	sumM.setFont(new Font("Sans Serif", Font.PLAIN, 18));
	    	numPane.add(sumM, "alignx 50%, wrap");
	    	
	    	this.add(numPane, "alignx 50%, pushx, wrap");
	
	    	//buttonPane
	    	JPanel buttonPane = new JPanel();
	    	buttonPane.setLayout(new MigLayout());
	    	buttonPane.setOpaque(true);
	    	
	    	loadNew_button = new JButton("Load New File");
	    	loadNew_button.setOpaque(true);
	    	loadNew_button.setBackground(new Color(60, 180, 220, 255));
	    	loadNew_button.setForeground(Color.white);
			Dimension d = loadNew_button.getPreferredSize();
			d.setSize(d.getWidth()*1.5, d.getHeight()*1.5);
			loadNew_button.setFont(new Font("Sans Serif", Font.PLAIN, 18));
	    	loadNew_button.addActionListener(new ActionListener(){
	
				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					try {
						ClusterFileSet fileSet = clusterSelection();
						viewFrame.loadClusterFileSet(fileSet); 
//						fileSet = clusterfileMru.addUnique(fileSet); File MRU = most recently used files
//						clusterfileMru.setLast(fileSet);
//						clusterfileMru.notifyObservers();
						viewFrame.setLoaded(true);
					} catch (LoadException e) {
						if ((e.getType() != LoadException.INTPARSE)
								&& (e.getType() != LoadException.NOFILE)) {
							LogBuffer.println("Could not open file: "
									+ e.getMessage());
							e.printStackTrace();
							}
						}
				}
	    	});
	    	buttonPane.add(loadNew_button, "alignx 50%, pushx");
	    	
	    	view_button = new JButton("View Loaded Data");
	    	view_button.setOpaque(true);
	    	view_button.setBackground(new Color(60, 180, 220, 255));
	    	view_button.setForeground(Color.white);
			Dimension d2 = view_button.getPreferredSize();
			d2.setSize(d2.getWidth()*1.5, d2.getHeight()*1.5);
			view_button.setFont(new Font("Sans Serif", Font.PLAIN, 18));
	    	view_button.addActionListener(new ActionListener(){
	
				@Override
				public void actionPerformed(ActionEvent arg0) {
					new DataViewDialog(outer, viewFrame);
					
				}	
	    	});
	    	
	    	buttonPane.add(view_button, "alignx 50%");
	    	this.add(buttonPane, "alignx 50%, pushx");
		  }
	}
	
	class ClusterOptionsPanel extends JPanel {	
		
//		private ClusterFrame clusterDialog;
//		private JButton advanced_button;
		
		private static final long serialVersionUID = 1L;
			    
		public ClusterOptionsPanel() {
			
			this.setLayout(new MigLayout());
			this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
			setBackground(Color.white);
			
			//headers
			head2 = new HeaderPanel("Rows");
			head2.setSmall();
			head2.setBackground(new Color(110, 210, 255, 150));
			this.add(head2, "alignx 50%, pushx");
			 
			head3 = new HeaderPanel("Columns");
			head3.setSmall();
			head3.setBackground(new Color(110, 210, 255, 150));
			this.add(head3, "alignx 50%, pushx, wrap");
			 
			//Component 1
			geneClusterPanel = new GeneClusterPanel();
			this.add(geneClusterPanel, "center, grow, push");
		
			//Component 2
			arrayClusterPanel = new ArrayClusterPanel();
			this.add(arrayClusterPanel, "center, grow, push, wrap");
			
//			//Button Component
//			JPanel buttonPanel = new JPanel();
//			buttonPanel.setLayout(new MigLayout());
//			buttonPanel.setOpaque(false);
//		
//			//Advanced Options Button
//			advanced_button = new JButton("Advanced Options >>");
//			advanced_button.setOpaque(true);
//			advanced_button.setBackground(new Color(60, 180, 220, 255));
//			advanced_button.setForeground(Color.white);
//			Dimension d = advanced_button.getPreferredSize();
//			d.setSize(d.getWidth()*1.5, d.getHeight()*1.5);
//			advanced_button.setFont(new Font("Sans Serif", Font.PLAIN, 18));
//		
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
//			buttonPanel.add(advanced_button, "alignx 50%, span, pushx");
//		
//			this.add(buttonPanel, "span, pushx, growx");
		}
	}
	
	class GeneClusterPanel extends JPanel {
	      
		private static final long serialVersionUID = 1L;

		public GeneClusterPanel() {
					
	  		//set this panel's layout
	  		this.setLayout(new MigLayout("", "[]push[]"));
			this.setBackground(Color.white);
			this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
	  			
	  		weightGeneCheck = new JCheckBox ("Calculate Weights");
	  		weightGeneCheck.setFont(new Font("Sans Serif", Font.PLAIN, 18));
	  		weightGeneCheck.setBackground(Color.white);
	  		this.add(weightGeneCheck, "wrap");
	  		
	  		geneCombo = new JComboBox<String>(measurements);
	  		geneCombo.setBackground(Color.white);
	  		
	  		similarity = new JLabel("Similarity Metric");
	  		similarity.setFont(new Font("Sans Serif", Font.PLAIN, 18));
	  		similarity.setBackground(Color.white);
	  		
	  		this.add(similarity, "alignx 50%, span, wrap");
	  		this.add(geneCombo, "alignx 50%, span");
		}
	}
	  
	class ArrayClusterPanel extends JPanel {
	      
		private static final long serialVersionUID = 1L;
			
		//Constructor
		public ArrayClusterPanel() {
				
	  		//set this panel's layout
	  		this.setLayout(new MigLayout("", "[]push[]"));
			this.setBackground(Color.white);
			this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
	  			
	  		weightArrayCheck = new JCheckBox ("Calculate Weights");
	  		weightArrayCheck.setFont(new Font("Sans Serif", Font.PLAIN, 18));
	  		weightArrayCheck.setBackground(Color.white);
	  		this.add(weightArrayCheck, "wrap");
	  		
	  		arrayCombo = new JComboBox<String>(measurements);
	  		arrayCombo.setBackground(Color.white);
	  		similarity = new JLabel("Similarity Metric");
	  		similarity.setFont(new Font("Sans Serif", Font.PLAIN, 18));
	  		similarity.setBackground(Color.white);
	  		
	  		this.add(similarity, "alignx 50%, span, wrap");
	  		this.add(arrayCombo, "alignx 50%, span");
		}
	}
		
	class FinalOptionsPanel extends JPanel {	
			  
		private static final long serialVersionUID = 1L;
			
		//Instance variables
		private JButton cluster_button, cancel_button, dendro_button;
		private JComboBox<String> clusterChoice;
		private JLabel status1, status2, method, error1, error2, clusterLabel, opLabel;
		private String path, clusterMethod;
		private File file;
		private JProgressBar pBar, pBar2;
		private final JPanel buttonPanel, loadPanel, choicePanel;
		final SwingWorker<Void, Void> worker;
		    
		//Constructor
		public FinalOptionsPanel() {
			
			this.setLayout(new MigLayout());
			setOpaque(false);
			
			//worker thread for calculation off the EDT to give Swing elements time to update
			worker = new SwingWorker<Void, Void>() {	
				
				public Void doInBackground() {

		        	try {
		        		
		        		HierarchicalCluster clusterTarget = 
		        				new HierarchicalCluster((ClusterModel)dataModel, viewFrame, 
		        						ClusterView.this, pBar, pBar2, opLabel, dataArray);
		        		
		        		clusterTarget.hCluster(clusterMethod);
		        		
					} catch (InterruptedException e) {
					
						e.printStackTrace();
					} catch (ExecutionException e) {
						
						e.printStackTrace();
					}
					return null;
				}
					
				protected void done(){
					
					clusterLabel.setText("Clustering complete!");
					pBar.setForeground(new Color(0, 200, 0, 255));
					pBar2.setForeground(new Color(0, 200, 0, 255));
					
					cluster_button.setEnabled(true);
					
					buttonPanel.remove(cancel_button);
					buttonPanel.add(cluster_button, "pushx, alignx 50%");
					buttonPanel.add(dendro_button, "pushx, alignx 50%");
					
					status1 = new JLabel("The file has been saved in the original directory.");
					status1.setFont(new Font("Sans Serif", Font.PLAIN, 18));
					loadPanel.add(status1, "growx, pushx, wrap");
					
					status2 = new JLabel("File Path: " + path);
					status2.setFont(new Font("Sans Serif", Font.ITALIC, 18));
					loadPanel.add(status2, "growx, pushx, wrap");
					
					mainPanel.revalidate();
					mainPanel.repaint();	
				}
			};
			
			//Button Component
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new MigLayout());
			
			choicePanel = new JPanel();
			choicePanel.setLayout(new MigLayout());
			
			//ProgressBar Component
			loadPanel = new JPanel();
			loadPanel.setLayout(new MigLayout());

			//distance measure chooser
			method = new JLabel("Method: ");
			method.setFont(new Font("Sans Serif", Font.PLAIN, 22));
			choicePanel.add(method, "alignx 50%, pushx");
	    	
			//ClusterChoice ComboBox
			String[] clusterMethods = {"Single Linkage", "Centroid Linkage", "Average Linkage", "Complete Linkage"};
			clusterChoice = new JComboBox<String>(clusterMethods);
			Dimension d = clusterChoice.getPreferredSize();
			d.setSize(d.getWidth()*1.5, d.getHeight()*1.5);
			clusterChoice.setPreferredSize(d);
			clusterChoice.setFont(new Font("Sans Serif", Font.PLAIN, 18));
			clusterChoice.setBackground(Color.white);
			
			choicePanel.add(clusterChoice, "alignx 50%, wrap");
			
	    	//button with action listener
	    	dendro_button = new JButton("Dendrogram");
	  		Dimension d2 = dendro_button.getPreferredSize();
	  		d2.setSize(d2.getWidth()*2, d2.getHeight()*2);
	  		dendro_button.setPreferredSize(d2);
	  		dendro_button.setFont(new Font("Sans Serif", Font.PLAIN, 20));
	  		dendro_button.setOpaque(true);
	  		dendro_button.setBackground(new Color(60, 180, 220, 255));
	  		dendro_button.setForeground(Color.white);
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
	    	
	    	//button with action listener
	    	cluster_button = new JButton("Cluster");
	  		Dimension d3 = cluster_button.getPreferredSize();
	  		d3.setSize(d3.getWidth()*2, d3.getHeight()*2);
	  		cluster_button.setPreferredSize(d3);
	  		cluster_button.setFont(new Font("Sans Serif", Font.PLAIN, 20));
	  		cluster_button.setOpaque(true);
	  		cluster_button.setBackground(new Color(60, 180, 220, 255));
	  		cluster_button.setForeground(Color.white);
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
					
					//precise progressbar
					pBar = new JProgressBar();
					pBar.setMinimum(0);
					pBar.setStringPainted(true);
					pBar.setForeground(new Color(60, 180, 220, 255));
					pBar.setUI(new BasicProgressBarUI(){
						protected Color getSelectionBackground(){return Color.black;};
						protected Color getSelectionForeground(){return Color.white;};
					});
					pBar.setVisible(true);
					
					//general progressbar
					pBar2 = new JProgressBar();
					pBar2.setMinimum(0);
					pBar2.setStringPainted(true);
					pBar2.setForeground(new Color(60, 180, 220, 255));
					pBar2.setUI(new BasicProgressBarUI(){
						protected Color getSelectionBackground(){return Color.black;};
						protected Color getSelectionForeground(){return Color.white;};
					});
					pBar2.setString("Overall Progress");
					pBar2.setVisible(true);
					
					clusterLabel = new JLabel("Working...");
					clusterLabel.setFont(new Font("Sans Serif", Font.PLAIN, 22));
					loadPanel.add(clusterLabel, "alignx 50%, span, wrap");
					
					//Add it to JPanel Object
					loadPanel.add(pBar, "pushx, growx, span, wrap");
					
					//Add it to JPanel Object
					loadPanel.add(pBar2, "pushx, growx, span, wrap");
					
					opLabel = new JLabel();
					loadPanel.add(opLabel, "alignx 50%, wrap");
					
					loadPanel.setBackground(Color.white);
					loadPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
					
					finalPanel.remove(buttonPanel);
					
					finalPanel.add(loadPanel, "pushx, growx, span, wrap");
					cluster_button.setEnabled(false);
					
					cancel_button = new JButton("Cancel");
			  		Dimension d2 = cancel_button.getPreferredSize();
			  		d2.setSize(d2.getWidth()*2, d2.getHeight()*2);
			  		cancel_button.setPreferredSize(d2);
			  		cancel_button.setFont(new Font("Sans Serif", Font.PLAIN, 20));
			  		cancel_button.setOpaque(true);
			  		cancel_button.setBackground(new Color(60, 180, 220, 255));
			  		cancel_button.setForeground(Color.white);
					cancel_button.addActionListener(new ActionListener(){

						@Override
						public void actionPerformed(ActionEvent arg0) {
							
							worker.cancel(true);	
							cluster_button.setEnabled(true);
							mainPanel.remove(finalPanel);
							finalPanel = new FinalOptionsPanel();
							mainPanel.add(finalPanel, "growx, pushx");
							
							mainPanel.revalidate();
							mainPanel.repaint();	
						}
						
					});
					
					//buttonPanel.add(cluster_button, "pushx");
					buttonPanel.add(cancel_button, "pushx");
					
					finalPanel.add(buttonPanel, "pushx, alignx 50%");
					
					mainPanel.revalidate();
					mainPanel.repaint();
						
					//actual calculations, off the EDT
//					final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {	
//							
//						public Void doInBackground() {
//
//				        	try {
//				        		
//				        		HierarchicalCluster clusterTarget = 
//				        				new HierarchicalCluster((ClusterModel)dataModel, viewFrame, 
//				        						ClusterView.this, pBar, opLabel, opLabel2, opLabel3, dataArray);
//				        		
//				        		clusterTarget.hCluster(clusterMethod);
//				        		
//							} catch (InterruptedException e) {
//							
//								e.printStackTrace();
//							} catch (ExecutionException e) {
//								
//								e.printStackTrace();
//							}
//							return null;
//						}
//							
//						protected void done(){
//							
//							clusterLabel.setText("Clustering complete!");
//							pBar.setForeground(new Color(0, 200, 0, 255));
//							
//							status1 = new JLabel("The file has been saved in the original directory.");
//							status1.setFont(new Font("Sans Serif", Font.PLAIN, 18));
//							loadPanel.add(status1, "growx, pushx, wrap");
//							
//							status2 = new JLabel("File Path: " + path);
//							status2.setFont(new Font("Sans Serif", Font.ITALIC, 18));
//							loadPanel.add(status2, "growx, pushx, wrap");
//							
//							mainPanel.revalidate();
//							mainPanel.repaint();	
//						}
//					};
					
					//start new cluster process
					worker.execute();
				}
				//display error message
				else{
					
					error1 = new JLabel("Woah, that's too quick!");
					error1.setFont(new Font("Sans Serif", Font.PLAIN, 22));
					error1.setForeground(new Color(240, 80, 50, 255));
					loadPanel.add(error1, "alignx 50%, span, wrap");
					
					error2 = new JLabel("Please select either a similarity metric for rows, " +
							"columns, or both to begin clustering!");
					error2.setFont(new Font("Sans Serif", Font.PLAIN, 22));
					
					loadPanel.setBackground(Color.white);
					loadPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
					loadPanel.add(error2, "alignx 50%, span");
					finalPanel.add(loadPanel, "alignx 50%, pushx, span");
					
					mainPanel.revalidate();
					mainPanel.repaint();
				}
			}	
	    	});
	    	buttonPanel.add(cluster_button, "pushx");
	    	
	    	buttonPanel.setOpaque(false);
	    	choicePanel.setOpaque(false);
	    	
	    	this.add(choicePanel, "alignx 50%, pushx, wrap");
	    	this.add(buttonPanel, "alignx 50%, pushx, wrap");
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
		TxtFilter ff = new TxtFilter();
		try {
			fileDialog.addChoosableFileFilter(ff);
			// will fail on pre-1.3 swings
			fileDialog.setAcceptAllFileFilterUsed(true);
		} catch (Exception e) {
			// hmm... I'll just assume that there's no accept all.
			fileDialog.addChoosableFileFilter(new javax.swing.filechooser
					.FileFilter() {
				public boolean accept (File f) {
					return true;
				}
				public String getDescription () {
					return "All Files";
				}
			});
		}
		fileDialog.setFileFilter(ff);
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
	public ConfigNode getConfigNode() {
		return root;
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
