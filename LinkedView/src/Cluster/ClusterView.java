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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;

import net.miginfocom.swing.MigLayout;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.DataModel;

import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.MainProgramArgs;

import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
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
public class ClusterView extends JPanel implements ConfigNodePersistent, MainPanel, Observer {

	private static final long serialVersionUID = 1L;
	
	//Instance variable in which the loaded data array is being stored
	private TreeViewFrame viewFrame;
	protected DataModel dataModel;
	
	//Various GUI Panels
	JScrollPane scrollPane;
	JPanel mainPanel = new JPanel();
	HeaderPanel head1, head2, head3, head4;
	InitialPanel initialPanel;
	InfoPanel infoPanel;
	FilterOptionsPanel filterOptions;
	RemovePercentPanel percentPanel;
	RemoveSDPanel sdPanel;
	RemoveAbsPanel absPanel;
	AdjustOptionsPanel aoPanel;
	MaxMinPanel maxMinPanel;
	GeneAdjustPanel geneAdjustPanel;
	ArrayAdjustPanel arrayAdjustPanel;
	GeneClusterPanel geneClusterPanel;
	ArrayClusterPanel arrayClusterPanel;
	ClusterOptionsPanel coPanel;
	FinalOptionsPanel finalPanel;
	
	//Object of the loaded model and matrix 
	private ClusterModel outer;
	private ClusterModel.ClusterDataMatrix matrix;
	
	//Instance variable in which the loaded data array is being stored
	private double[] dataArray;
	private double[] rangeArray;
	
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
	 * @param  vFrame  parent ViewFrame of DendroView
	 * @param  name name of this view.
	 */
	public ClusterView(DataModel dataModel, ConfigNode root, TreeViewFrame vFrame, String name) {
		super.setName(name);
		viewFrame = vFrame;

		this.dataModel = dataModel;
		outer = (ClusterModel) dataModel;
		matrix = outer.getDataMatrix();
		dataArray = matrix.getExprData();
		rangeArray = Arrays.copyOfRange(dataArray, 50, 100);

		viewFrame.setResizable(true);
		
		this.setLayout(new MigLayout());
		
		//set layout for initial window
		mainPanel.setLayout(new MigLayout());
		
		//header
		head1 = new HeaderPanel("Hierarchical Clustering");
		mainPanel.add(head1, "pushx, growx, span, wrap");
		
		//Data Info Panel
		initialPanel = new InitialPanel();
		mainPanel.add(initialPanel, "growx, pushx, span, wrap");
		
		//Cluster Options Panel
		coPanel = new ClusterOptionsPanel();
		mainPanel.add(coPanel, "growx, pushx, span, wrap");
		
		finalPanel = new FinalOptionsPanel();
		mainPanel.add(finalPanel, "grow, push");
		
		//mainPanel.setBackground(new Color(210, 210, 210, 100));
		
		//make mainpanel scrollable by adding it to scrollpane
		scrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		//Add the mainPanel to the ContentPane
		this.add(scrollPane, "grow, push");
		
	}
	
	//Check boxes throughout the GUI, declared outside their panels to increase scope for use in filter/ adjustment/cluster methods
	JCheckBox check1, check2, check3, check4, logCheck, centerGenes, normGenes, centerArrays, 
	  			normArrays, clusterGeneCheck, weightGeneCheck, clusterArrayCheck, weightArrayCheck;
	
	//ComboBoxes
	JComboBox<String> geneCombo, arrayCombo;
	
	class HeaderPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		
		JLabel text;
		
		public HeaderPanel(String header){
			
			this.setLayout(new MigLayout());
			setBackground(new Color(110, 210, 255, 255));
			
			text = new JLabel(header);
			text.setFont(new Font("Sans Serif", Font.BOLD, 22));
			
			this.add(text);
			
		}
		
		public void setColor(Color color){
			
			setBackground(color);
		}
		
		public void setSmall(){
			text.setFont(new Font("Sans Serif", Font.PLAIN, 18));
		}
	}
	class InitialPanel extends JPanel {	
	  
		private static final long serialVersionUID = 1L;
		
		//Instance variables
		int nRows, nCols; 
		JLabel label1, label2, numColLabel, numRowLabel;
		JButton yes_button, no_button, no2_button, loadNew_button, view_button;
		JTextArea textArea;
	    
		//Constructor
		public InitialPanel() {
			this.setLayout(new MigLayout());
			//this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
			setOpaque(false);
			
	    	HeaderInfo infoArray = dataModel.getArrayHeaderInfo();
	    	HeaderInfo infoGene = dataModel.getGeneHeaderInfo();
	    	
	    	nCols = infoArray.getNumHeaders();
	    	nRows = infoGene.getNumHeaders();
	    	
	    	
	    	label1 = new JLabel("Loading file successful!");
	    	label1.setFont(new Font("Sans Serif", Font.PLAIN, 16));
	    	this.add(label1, "top, wrap");
	   
	    	
	    	label2 = new JLabel("Matrix Dimensions:");
	    	label2.setFont(new Font("Sans Serif", Font.PLAIN, 16));
	    	this.add(label2, "wrap");
	    	
	    	//panel with dimensions of the dataMatrix
	    	JPanel numPane = new JPanel();
	    	numPane.setLayout(new MigLayout());
	    	numPane.setOpaque(false);
	    	
	    	numColLabel = new JLabel(nCols + " columns");
	    	numColLabel.setFont(new Font("Sans Serif", Font.BOLD, 16));
	    	numColLabel.setForeground(new Color(240, 80, 50, 255));
	    	numPane.add(numColLabel, "span, split 2, center");
	    	
	    	numRowLabel = new JLabel(nRows + " rows");
	    	numRowLabel.setForeground(new Color(240, 80, 50, 255));
	    	numRowLabel.setFont(new Font("Sans Serif", Font.BOLD, 16));
	    	numPane.add(numRowLabel,  "gapleft 10%");
	    	
	    	this.add(numPane, "alignx 50%, growy, pushy");
	
	    	//buttonPane
	    	JPanel buttonPane1 = new JPanel();
	    	buttonPane1.setLayout(new MigLayout());
	    	buttonPane1.setOpaque(false);
	    	
	    	loadNew_button = new JButton("Load New File");
	    	loadNew_button.addActionListener(new ActionListener(){
	
				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					ClusterView.this.removeAll();
					
					try {
						ClusterFileSet fileSet = clusterSelection();
						viewFrame.loadClusterFileSet(fileSet); 
	//					fileSet = clusterfileMru.addUnique(fileSet); File MRU = most recently used files
	//					clusterfileMru.setLast(fileSet);
	//					clusterfileMru.notifyObservers();
						viewFrame.setLoaded(true);
						//viewFrame.getClusterDialogWindow(viewFrame.getDataModel()).setVisible(true); //this doesnt load a new Dialog yet, doesnt laod clusterfilewindow
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
	    	buttonPane1.add(loadNew_button, "alignx 0%");
	    	
	    	view_button = new JButton("View Loaded Data");
	    	view_button.addActionListener(new ActionListener(){
	
				@Override
				public void actionPerformed(ActionEvent arg0) {
					new DataViewDialog(outer, viewFrame);
					
				}	
	    	});
	    	buttonPane1.add(view_button);
	 
	    	this.add(buttonPane1, "wrap, grow, push");
		  }
		}
	
	 class InfoPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		
		JLabel content;
		
		public InfoPanel(){
			
			this.setLayout(new MigLayout());
			setBackground(new Color(255, 255, 15, 150));
			
			content = new JLabel();
			add(content);
			
		}
		
		public void setText(String text){
			
			content.setText(text);
		}
	 }
	
	 class FilterOptionsPanel extends JPanel {	
	
		private static final long serialVersionUID = 1L;
		
		JButton remove_button, noFilter_button, noFilter2_button;
		JTextArea instructions;
		    
		public FilterOptionsPanel() {
			
			this.setLayout(new MigLayout());
			this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
			setBackground(Color.WHITE);
	    
			//Instructions
			instructions = new JTextArea("Check all filter options which you would like to apply " +
					"to your dataset, then click 'Remove'.");
			instructions.setFont(new Font("Sans Serif", Font.PLAIN, 16));
	    	instructions.setLineWrap(false);
	    	instructions.setOpaque(false);
	    	instructions.setEditable(false);
			this.add(instructions, "wrap");
			
			//Inner components of this panel, objects of other inner classes
			//Component 1 
			percentPanel = new RemovePercentPanel();
			this.add(percentPanel, "push, grow, wrap");
			
			//Component 2
			sdPanel = new RemoveSDPanel();
			this.add(sdPanel, "push, grow, wrap");
			
			//Component 3
			absPanel = new RemoveAbsPanel();
			this.add(absPanel, "push, grow, wrap");
			
			//Component 3
			maxMinPanel = new MaxMinPanel();
			this.add(maxMinPanel, "push, grow, wrap");
			
	    	//panle with all the buttons
	    	JPanel buttonPane = new JPanel();
	    	buttonPane.setLayout(new MigLayout());
	    	buttonPane.setBackground(Color.white);
	    	
	    	//remove button with action listener
	    	remove_button = new JButton("Filter Data");
	    	remove_button.addActionListener(new ActionListener(){

				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					//nextPanel(filterOptions, NOSKIP);
				}	
	    	});
	    	buttonPane.add(remove_button);
	    	
	    	this.add(buttonPane, "span, alignx 50%");	
		  }
	  }

	  class RemovePercentPanel extends JPanel {	

		private static final long serialVersionUID = 1L;
		
			JLabel label1, label2;
			JTextArea textArea;
			JTextField percentField;
			int textFieldSize = 5;
			JButton info_button;

			  public RemovePercentPanel() {
				this.setLayout(new MigLayout("", "[]push[]"));
				//setBackground(new Color(210, 210, 210, 70));
				
		    	check1 = new JCheckBox("Data Completeness per Element");
		    	check1.setOpaque(false);
		    	this.add(check1, "alignx 0%");
		    	
		    	JPanel bPane = new JPanel();
		    	bPane.setOpaque(false);
		    	info_button = new JButton("Info");
		    	
		    	info_button.addActionListener(new ActionListener(){

					@Override
					public void actionPerformed(ActionEvent arg0) {
						infoPanel.setText("This option removes elements with a specified percentage of missing values.");
					}	
		    	});
		    	
		    	bPane.add(info_button);
		    	this.add(bPane, "wrap, pushx, growx");
		    	
		    	
		    	//valuefield
		    	percentField = new JTextField(textFieldSize);
		    	percentField.addFocusListener(new FocusListener(){

					@Override
					public void focusGained(FocusEvent arg0) {
						check1.setSelected(true);
						
					}

					@Override
					public void focusLost(FocusEvent arg0) {
						
					}
		    		
		    	});
		    	
		    	
		    	JPanel valuePane = new JPanel();
		    	valuePane.setLayout(new MigLayout());
		    	valuePane.setOpaque(false);
		    	
		    	label2 = new JLabel();
		    	label2.setText("Enter Percentage: ");
		    	label2.setFont(new Font("Sans Serif", Font.PLAIN, 16));
		    	
		    	valuePane.add(label2, "alignx 0%");
		    	valuePane.add(percentField);

		    	this.add(valuePane);
			  }
			}
  
	  class RemoveSDPanel extends JPanel {	
	
			private static final long serialVersionUID = 1L;
			
		    int nRows, nCols;
		    JTextArea textArea;
			JTextField sdField;
			JLabel enterLabel;
			int textFieldSize = 5;
			JButton info_button;
			    
				  public RemoveSDPanel() {
					this.setLayout(new MigLayout("", "[]push[]"));
					setSize(this.getSize());
					//setBackground(new Color(210, 210, 210, 70));
			    	
			    	check2 = new JCheckBox("Standard Deviation (Gene Vector)");
			    	check2.setOpaque(false);
			    	this.add(check2, "alignx 0%");
			    	
			    	JPanel bPane = new JPanel();
			    	bPane.setOpaque(false);
			    	info_button = new JButton("Info");
			    	
			    	info_button.addActionListener(new ActionListener(){
	
						@Override
						public void actionPerformed(ActionEvent arg0) {
							infoPanel.setText("This option removes elements with a standard deviation " +
									"of less than the user specified value.");
						}
			    	});
			    	
			    	bPane.add(info_button);
			    	this.add(bPane, " wrap");
			    	
			    	JPanel valuePane = new JPanel();
			    	valuePane.setLayout(new MigLayout());
			    	valuePane.setOpaque(false);
			    	
			    	sdField = new JTextField(textFieldSize);
			    	sdField.addFocusListener(new FocusListener(){

						@Override
						public void focusGained(FocusEvent arg0) {
							check2.setSelected(true);
							
						}

						@Override
						public void focusLost(FocusEvent arg0) {
							
							
						}
			    		
			    	});
			    	
			    	enterLabel = new JLabel();
			    	enterLabel.setOpaque(false);
			    	enterLabel.setText("Enter a Standard Deviation: ");
			    	enterLabel.setFont(new Font("Sans Serif", Font.PLAIN, 16));
			    	
			    	valuePane.add(enterLabel);
			    	valuePane.add(sdField, "gapright 15%");
	
			    	this.add(valuePane, "span");
				  }
				}
  
	  class RemoveAbsPanel extends JPanel {	
	
			private static final long serialVersionUID = 1L;
			
		    int nRows, nCols;
			JLabel label1, numColLabel, numRowLabel, obsvLabel, absLabel;
			JTextArea textArea;
			JTextField obsvField, absField;;
			int textFieldSize = 5;
			JButton info_button;
			    
				  public RemoveAbsPanel() {
					this.setLayout(new MigLayout("", "[]push[]"));
					//setBackground(new Color(210, 210, 210, 70));
			    	
			    	check3 = new JCheckBox("Minimum Amount of Absolute Values");
			    	check3.setOpaque(false);
			    	this.add(check3, "alignx 0%");
			    	
			    	JPanel bPane = new JPanel();
			    	bPane.setOpaque(false);
			    	info_button = new JButton("Info");
			    	
			    	info_button.addActionListener(new ActionListener(){
	
						@Override
						public void actionPerformed(ActionEvent arg0) {
							
						}
			    	});
			    	
			    	bPane.add(info_button, "alignx 0%");
			    	this.add(bPane, "wrap");
			    	
			    	obsvField = new JTextField(textFieldSize);
			    	obsvField.addFocusListener(new FocusListener(){

						@Override
						public void focusGained(FocusEvent arg0) {
							check3.setSelected(true);
							
						}

						@Override
						public void focusLost(FocusEvent arg0) {
							
							
						}
			    		
			    	});
			    	
			    	absField = new JTextField(textFieldSize);
			    	absField.addFocusListener(new FocusListener(){

						@Override
						public void focusGained(FocusEvent arg0) {
							check3.setSelected(true);
							
						}

						@Override
						public void focusLost(FocusEvent arg0) {
							
							
						}
			    		
			    	});
			    	
			    	JPanel valuePane = new JPanel();
			    	valuePane.setLayout(new MigLayout());
			    	valuePane.setOpaque(false);
			    	
			    	obsvLabel = new JLabel("Enter # of Observations: ");
			    	obsvLabel.setFont(new Font("Sans Serif", Font.PLAIN, 16));
			    	absLabel = new JLabel("Enter Specified Absolute Value: ");
			    	absLabel.setFont(new Font("Sans Serif", Font.PLAIN, 16));
	
			    	valuePane.add(obsvLabel);
			    	valuePane.add(obsvField, "wrap, gapleft 5%");
			    	valuePane.add(absLabel);
			    	valuePane.add(absField, "gapleft 5%");
	
			    	this.add(valuePane, "span");
				  }
				}
  
	  class MaxMinPanel extends JPanel {	
	
			private static final long serialVersionUID = 1L;
			
		    int nRows, nCols;
			JLabel label1, numColLabel, numRowLabel, diffLabel;
			JTextArea textArea;
			JTextField diffField;;
			int textFieldSize = 5;
			JButton info_button;
			    
				  public MaxMinPanel() {
					this.setLayout(new MigLayout("", "[]push[]"));
					//setBackground(new Color(210, 210, 210, 70));
			    	
			    	check4 = new JCheckBox("Difference of Maximum and Minimum Data Values");
			    	check4.setOpaque(false);
			    	this.add(check4, "alignx 0%");
			    	
			    	JPanel bPane = new JPanel();
			    	bPane.setOpaque(false);
			    	info_button = new JButton("Info");
			    	
			    	info_button.addActionListener(new ActionListener(){
	
						@Override
						public void actionPerformed(ActionEvent arg0) {
							
						}
			    	});
			    	
			    	bPane.add(info_button, "alignx 100%");
			    	this.add(bPane, "wrap");
			    	
			    	diffField = new JTextField(textFieldSize);
			    	diffField.addFocusListener(new FocusListener(){

						@Override
						public void focusGained(FocusEvent arg0) {
							check4.setSelected(true);
							
						}

						@Override
						public void focusLost(FocusEvent arg0) {
							
							
						}
			    		
			    	});
			    	
			    	JPanel valuePane = new JPanel();
			    	valuePane.setOpaque(false);
			    	valuePane.setLayout(new MigLayout());
			    	
			    	diffLabel = new JLabel("Enter Specified Difference: ");
			    	diffLabel.setFont(new Font("Sans Serif", Font.PLAIN, 16));
	
			    	valuePane.add(diffLabel);
			    	valuePane.add(diffField,"gapleft 5%");
	
			    	this.add(valuePane);
			    	
				  }
				}
  
	  class AdjustOptionsPanel extends JPanel {	
	
			private static final long serialVersionUID = 1L;
				
			//Instance variables
			JButton adjust_button, noAdjust_button, info_button;
			JTextArea instructions;
			    
			public AdjustOptionsPanel() {
				
				this.setLayout(new MigLayout());
				this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
				setBackground(Color.WHITE);
		    
				//Instructions
				instructions = new JTextArea("Check all adjustment options which you would like to apply to your dataset, then click 'Adjust'.");
				instructions.setFont(new Font("Sans Serif", Font.PLAIN, 16));
				instructions.setLineWrap(false);
				instructions.setOpaque(false);
				instructions.setEditable(false);
				this.add(instructions, "grow, push, wrap");
				
				//Splitting up the content of this panel into several other panels
				//Component 1
				JPanel logPanel = new JPanel();
				logPanel.setLayout(new MigLayout("", "[]push[]"));
		    	logCheck = new JCheckBox("Log Transform");
		    	logCheck.setOpaque(false);
		    	//logPanel.setBackground(new Color(210, 210, 210, 70));
		    	logPanel.add(logCheck, "grow, push");
		    	
		    	//Button to receiv more info about the log transform feature
		    	JPanel bPane = new JPanel();
		    	info_button = new JButton("Info");
		    	bPane.add(info_button, "alignx 100%");
		    	bPane.setOpaque(false);
		    	logPanel.add(bPane);
		    	
		    	this.add(logPanel,"wrap");
				
				//Component 2
				geneAdjustPanel = new GeneAdjustPanel();
				this.add(geneAdjustPanel, "grow, push, split 2");
				
				//Component 3
				arrayAdjustPanel = new ArrayAdjustPanel();
				this.add(arrayAdjustPanel, "grow, push, wrap");
				
		    	//button panel containing all the buttons for confirmation and navigation
		    	JPanel buttonPane = new JPanel();
		    	buttonPane.setLayout(new MigLayout());
		    	
		    	//button to confirm adjustment operations with action listener
		    	adjust_button = new JButton("Adjust Data");
		    	adjust_button.addActionListener(new ActionListener(){
	
					@Override
					public void actionPerformed(ActionEvent arg0) {
						
						//nextPanel(aoPanel, NOSKIP);
					}	
		    	});
		    	buttonPane.add(adjust_button);
		    	
		    	buttonPane.setOpaque(false);
		    	this.add(buttonPane, "alignx 50%");
		    	
			  }
				}
  
	  class GeneAdjustPanel extends JPanel {
		  
		  //Local variables for this class (might have to take Radiobuttons out!)
	      JRadioButton mean;
	      JRadioButton median;
	      ButtonGroup bg;
	      JButton info_button;
	      
			private static final long serialVersionUID = 1L;
			
			//Constructor
			public GeneAdjustPanel() {
				
		  		//set this panel's layout
		  		this.setLayout(new MigLayout("", "[]push[]"));
		    	
		  		//create checkbox
		  		centerGenes = new JCheckBox("Center Genes");
		  		centerGenes.setOpaque(false);
		  		this.add(centerGenes);
		  		
		    	JPanel bPane = new JPanel();
		    	info_button = new JButton("Info");
		    	bPane.add(info_button, "alignx 100%");
		    	bPane.setOpaque(false);
		    	this.add(bPane, "wrap");
		  		
		  		//Create a new ButtonGroup
		  		bg = new ButtonGroup();
		  		
		  		mean = new JRadioButton("Mean", true);
		  		median = new JRadioButton("Median", false);
		  		mean.setOpaque(false);
		  		mean.setEnabled(false);
		  		median.setOpaque(false);
		  		median.setEnabled(false);
		  		
		  		//add Radio Buttons to ButtonGroup
		  		bg.add(mean);
		  		bg.add(median);
		  		
		  		//Add Buttons to panel
		  		this.add(mean, "wrap, gapleft 5%");
		  		this.add(median, "wrap, gapleft 5%");
		  		
		  		centerGenes.addItemListener(new ItemListener() {
		
						@Override
						public void itemStateChanged(ItemEvent arg0)
						{
							if(mean.isEnabled() == false) {
		  					
								mean.setEnabled(true);
								median.setEnabled(true);
							}
							else {	
								mean.setEnabled(false);
								median.setEnabled(false);
							}
						}
		  		});
		  			
		  		normGenes = new JCheckBox ("Normalize Genes");
		  		normGenes.setOpaque(false);
		  		this.add(normGenes);
		  	}
	  }
  
	  class ArrayAdjustPanel extends JPanel {
		  
		  //Local variables for this class
	      JRadioButton mean;
	      JRadioButton median;
	      ButtonGroup bg;
	      JButton info_button;
	      
			private static final long serialVersionUID = 1L;
			
			//Constructor
			public ArrayAdjustPanel() {
				
		  		//set this panel's layout
		  		this.setLayout(new MigLayout("", "[]push[]"));
		  		
		  		//create checkbox
		  		centerArrays = new JCheckBox("Center Arrays");
		  		centerArrays.setOpaque(false);
		  		this.add(centerArrays);
		  		
		    	JPanel bPane = new JPanel();
		    	info_button = new JButton("Info");
		    	bPane.add(info_button, "alignx 100%");
		    	bPane.setOpaque(false);
		    	this.add(bPane, "wrap");
		  		
		  		//Create a new ButtonGroup
		  		bg = new ButtonGroup();
		  		
		  		mean = new JRadioButton("Mean", true);
		  		median = new JRadioButton("Median", false);
		  		mean.setOpaque(false);
		  		mean.setEnabled(false);
		  		median.setOpaque(false);
		  		median.setEnabled(false);
		  		
		  		//add Radio Buttons to ButtonGroup
		  		bg.add(mean);
		  		bg.add(median);
		  		
		  		//Add Buttons to panel
		  		this.add(mean, "wrap, gapleft 5%");
		  		this.add(median, "wrap, gapleft 5%");
		  		
		  		centerArrays.addItemListener(new ItemListener() {
		
						@Override
						public void itemStateChanged(ItemEvent arg0)
						{
							if(mean.isEnabled() == false) {
		  					
								mean.setEnabled(true);
								median.setEnabled(true);
							}
							else {	
								mean.setEnabled(false);
								median.setEnabled(false);
							}
						}
		  		});
		  			
		  		normArrays = new JCheckBox ("Normalize Arrays");
		  		normArrays.setOpaque(false);
		  		this.add(normArrays);
				}
	  	}
		
		class ClusterOptionsPanel extends JPanel {	
		
			private static final long serialVersionUID = 1L;
			    
				 public ClusterOptionsPanel() {
					this.setLayout(new MigLayout());
					this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
					setBackground(Color.white);
					
					//headers
					head2 = new HeaderPanel("Genes");
					head2.setSmall();
					head2.setBackground(new Color(110, 210, 255, 150));
					this.add(head2, "pushx, growx");
					 
					head3 = new HeaderPanel("Arrays");
					head3.setSmall();
					head3.setBackground(new Color(110, 210, 255, 150));
					this.add(head3, "pushx, growx, wrap");
					 
					//Component 1
					geneClusterPanel = new GeneClusterPanel();
					this.add(geneClusterPanel, "center, grow, push");
					
					//Component 2
					arrayClusterPanel = new ArrayClusterPanel();
					this.add(arrayClusterPanel, "center, grow, push, wrap");
			    	
				  }
				}
		
		//Options for comboboxes used by 2 classes
		private String[] measurements = {"Correlation (uncentered)", "Correlation (centered)", "Absolute Correlation (uncentered)",
				"Absolute Correlation (centered)", "Spearman Rank Correlation", "Kendall's Tau", "Euclidean Distance", "City Block Distance"};
	
		//label used by 2 classes
		private JLabel similarity;
	
		class GeneClusterPanel extends JPanel {

			JButton info_button;
	      
			private static final long serialVersionUID = 1L;

				public GeneClusterPanel() {
					
			  		//set this panel's layout
			  		this.setLayout(new MigLayout("", "[]push[]"));
			  		//this.setBorder(BorderFactory.createTitledBorder("Genes"));
					setBackground(Color.white);
			  		
			  		//create checkbox
			  		clusterGeneCheck = new JCheckBox("Cluster");
			  		clusterGeneCheck.setBackground(Color.white);
			  		this.add(clusterGeneCheck);
			  		
			    	JPanel bPane = new JPanel();
			    	info_button = new JButton("?");
			    	bPane.add(info_button, "grow, push, alignx 100%");
			    	bPane.setBackground(Color.white);
			    	this.add(bPane, "wrap");
			  			
			  		weightGeneCheck = new JCheckBox ("Calculate Weights");
			  		weightGeneCheck.setBackground(Color.white);
			  		this.add(weightGeneCheck, "wrap");
			  		
			  		geneCombo = new JComboBox<String>(measurements);
			  		geneCombo.setBackground(Color.white);
			  		
			  		similarity = new JLabel("Similarity Metric");
			  		similarity.setBackground(Color.white);
			  		
			  		this.add(similarity, "alignx 50%, span, wrap");
			  		this.add(geneCombo, "alignx 50%, span");
				}
		}
	  
		class ArrayClusterPanel extends JPanel {
	      
			private static final long serialVersionUID = 1L;
			
			JButton info_button;
			
			//Constructor
			public ArrayClusterPanel() {
				
		  		//set this panel's layout
		  		this.setLayout(new MigLayout("", "[]push[]"));
		  		//this.setBorder(BorderFactory.createTitledBorder("Arrays"));
				setBackground(Color.white);
		  		
		  		//create checkbox
		  		clusterArrayCheck = new JCheckBox("Cluster");
		  		clusterArrayCheck.setBackground(Color.white);
		  		this.add(clusterArrayCheck);
		  		
		    	JPanel bPane = new JPanel();
		    	info_button = new JButton("?");
		    	bPane.add(info_button, "alignx 100%");
		    	bPane.setBackground(Color.white);
		    	this.add(bPane, "wrap");
		  			
		  		weightArrayCheck = new JCheckBox ("Calculate Weights");
		  		weightArrayCheck.setBackground(Color.white);
		  		this.add(weightArrayCheck, "wrap");
		  		
		  		arrayCombo = new JComboBox<String>(measurements);
		  		arrayCombo.setBackground(Color.white);
		  		similarity = new JLabel("Similarity Metric");
		  		similarity.setBackground(Color.white);
		  		
		  		this.add(similarity, "alignx 50%, span, wrap");
		  		this.add(arrayCombo, "alignx 50%, span");
			}
		}
		
		class FinalOptionsPanel extends JPanel {	
			  
			private static final long serialVersionUID = 1L;
			
			//Instance variables
			private JButton cluster_button, advanced_button;
			private JComboBox<String> clusterChoice;
			private ClusterFrame clusterDialog;
		    
			//Constructor
			public FinalOptionsPanel() {
				this.setLayout(new MigLayout());
				setOpaque(false);
		
				//Button Component
				JPanel buttonPanel = new JPanel();
				//buttonPanel.setBorder(BorderFactory.createTitledBorder("Choose Cluster Method"));
				buttonPanel.setLayout(new MigLayout());
				
				//Advanced Options Button
				advanced_button = new JButton("Advanced Options >>");
				advanced_button.addActionListener(new ActionListener(){

					@Override
					public void actionPerformed(ActionEvent arg0) {
						
						clusterDialog = new ClusterFrameWindow(viewFrame, outer);
						clusterDialog.setVisible(true);
						
					}
					
				});
				buttonPanel.add(advanced_button, "alignx 50%, span, wrap");
				
				//ProgressBar Component
				final JPanel loadPanel = new JPanel();
				loadPanel.setLayout(new MigLayout());
		    	
				//ClusterChoice ComboBox
				String[] clusterMethods = {"Single Linkage", "Centroid Linkage", "Average Linkage", "Complete Linkage"};
				clusterChoice = new JComboBox<String>(clusterMethods);
				clusterChoice.setBackground(Color.white);
				
				buttonPanel.add(clusterChoice);
				
		    	//button with action listener
		    	cluster_button = new JButton("Cluster");
		    	cluster_button.addActionListener(new ActionListener(){
		    		
		    
					@Override
					public void actionPerformed(ActionEvent arg0) {
						// All on Event Dispatch Thread
						//Build ProgressBar
						final JProgressBar pBar = new JProgressBar();
						
						pBar.setMinimum(0);
						pBar.setStringPainted(true);
						pBar.setForeground(new Color(100, 200, 255, 255));
						pBar.setUI(new BasicProgressBarUI(){
							protected Color getSelectionBackground(){return Color.black;};
							protected Color getSelectionForeground(){return Color.white;};
						});
						pBar.setVisible(true);
						
						final JLabel clusterLabel = new JLabel("Clustering...");
						clusterLabel.setFont(new Font("Sans Serif", Font.PLAIN, 16));
						loadPanel.add(clusterLabel, "alignx 50%, span, wrap");
						
						//Add it to JPanel Object
						loadPanel.add(pBar, "pushx, growx, span");
						loadPanel.setBackground(Color.white);
						loadPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
						finalPanel.add(loadPanel, "pushx, growx");
						mainPanel.revalidate();
						mainPanel.repaint();
						
						final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {	
							
				    		//All not on Event Dispatch
							public Void doInBackground() {

					        	try {
									hCluster(2, dataArray, "What", pBar);
								} catch (InterruptedException e) {
								
									e.printStackTrace();
								} catch (ExecutionException e) {
									
									e.printStackTrace();
								}
								return null;
							}
							
							protected void done(){
								
								clusterLabel.setText("Clustering done!");
								pBar.setForeground(new Color(0, 200, 0, 255));
								mainPanel.revalidate();
								mainPanel.repaint();	
							}
						};
						worker.execute();	
					}	
		    	});
		    	buttonPanel.add(cluster_button);

		    	buttonPanel.setOpaque(false);
		    	this.add(buttonPanel, "alignx 50%, pushx, wrap");
			  }
			}	
		

	private void hCluster(int method, double[] currentArray, String similarityM, JProgressBar pBar) 
			throws InterruptedException, ExecutionException{
		
		//make a HierarchicalCluster object
		HierarchicalCluster clusterTarget = new HierarchicalCluster((ClusterModel)dataModel, 
				currentArray, method, similarityM, viewFrame);
		
		List<Double> currentList = new ArrayList<Double>();
		
		for(double d : currentArray){
			
			currentList.add(d);
		}
		
		//begin operations on clusterTarget...
		List<List<Double>> aoArrays = clusterTarget.splitList(currentList, pBar);
		
		//use int method to determine the distance/ similarity matrix algorithm
		//return distance/ similarity matrix and use as input to clustering function
		
		//int listRep = aoArrays.size();
		
//		System.out.println("ArrayList Length: " + listRep);
//		System.out.println("ArrayList Element: " + Arrays.toString(aoArrays.get(0)));

		List<List<Double>> geneDistances  = clusterTarget.euclid(aoArrays, pBar);
		
		clusterTarget.cluster(geneDistances, pBar);
		
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
	 *  this function changes the info in the confignode to match the current panel sizes. 
	 * this is a hack, since I don't know how to intercept panel resizing.
	 * Actually, in the current layout this isn't even used.
	 */
	public void syncConfig() {
		/*
		DragGridPanel running   = this;
		floa	t[] heights         = running.getHeights();
		ConfigNode heightNodes[]  = root.fetch("Height");
		for (int i = 0; i < heights.length; i++) {
			if (i < heightNodes.length) {
				heightNodes[i].setAttribute("value", (double) heights[i],
						1.0 / heights.length);
			} else {
				ConfigNode n  = root.create("Height");
					n.setAttribute("value", (double) heights[i],
							1.0 / heights.length);
			}
		}

	float[] widths          = running.getWidths();
	ConfigNode widthNodes[]   = root.fetch("Width");
		for (int i = 0; i < widths.length; i++) {
			if (i < widthNodes.length) {
				widthNodes[i].setAttribute("value", (double) widths[i],
						1.0 / widths.length);
			} else {
			ConfigNode n  = root.create("Width");
				n.setAttribute("value", (double) widths[i], 1.0 / widths.length);
			}
		}
*/
	}


	/**
	 *  binds this dendroView to a particular confignode, resizing the panel sizes
	 *  appropriately.
	 *
	 * @param  configNode  ConfigNode to bind to
	 */

	public void bindConfig(ConfigNode configNode) {
		root = configNode;
		/*
	ConfigNode heightNodes[]  = root.fetch("Height");
	ConfigNode widthNodes[]   = root.fetch("Width");

	float heights[];
	float widths[];
		if (heightNodes.length != 0) {
			heights = new float[heightNodes.length];
			widths = new float[widthNodes.length];
			for (int i = 0; i < heights.length; i++) {
				heights[i] = (float) heightNodes[i].getAttribute("value", 1.0 / heights.length);
			}
			for (int j = 0; j < widths.length; j++) {
				widths[j] = (float) widthNodes[j].getAttribute("value", 1.0 / widths.length);
			}
		} else {
			widths = new float[]{2 / 11f, 3 / 11f, 3 / 11f, 3 / 11f};
			heights = new float[]{3 / 16f, 1 / 16f, 3 / 4f};
		}
		setHeights(heights);
		setWidths(widths);
		*/
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
	/**
	 * The following arrays allow translation to and from screen and datamatrix 
	 * I had to add these in order to have gaps in the dendroview of k-means
	 */
	private int [] arrayIndex  = null;
	private int [] geneIndex   = null;

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
	public void update(Observable arg0, Object arg1) {
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
	public void export(MainProgramArgs args) throws ExportException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public ImageIcon getIcon() {
		// TODO Auto-generated method stub
		return null;
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

}
