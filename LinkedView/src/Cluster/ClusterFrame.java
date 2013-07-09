/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: HeaderFinder.java,v $
 * $Revision: 1.1 $
 * $Date: 2009-08-26 11:48:27 $
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;

import Cluster.HierarchicalCluster.TimerListener;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.DataMatrix;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeViewFrame;
/**
 * This class describes the GUI for the Cluster Application. It is separated into several panels which are
 * generated and painted when the user presses the related button.
 * There are also methods bound to buttons which invoke other classes to take over the calculation of
 * clustering algorithms etc.
 * 
 * @author ChrisK
 *
 */
public class ClusterFrame extends JFrame{

	private static final long serialVersionUID = 1L;
	
	//Frame and Model instance variables declared
	protected TreeViewFrame viewFrame;
	protected DataModel clusterModel;
	
	//Various GUI Panels
	JPanel mainPanel = new JPanel();
	InitialPanel initialPanel;
	FilterOptionsPanel filterOptions;
	RemovePercentPanel percentPanel;
	RemoveSDPanel sdPanel;
	RemoveAbsPanel absPanel;
	AdjustQPanel adjustQPanel;
	AdjustOptionsPanel aoPanel;
	MaxMinPanel maxMinPanel;
	GeneAdjustPanel geneAdjustPanel;
	ArrayAdjustPanel arrayAdjustPanel;
	GeneClusterPanel geneClusterPanel;
	ArrayClusterPanel arrayClusterPanel;
	ClusterOptionsPanel coPanel;
	
	Dimension screenSize;
	Point middle;
	Point newLocation;
	
	//Object of the loaded model and matrix 
	ClusterModel outer;
	ClusterModel.ClusterDataMatrix matrix;
	
	//Instance variable in which the loaded data array is being stored
	private double[] dataArray;
	double[] rangeArray;
	
	//variables to use for the nextPanel method to distinguish which buttons were pressed
	public static final int NOSKIP = 0;
	public static final int SKIPA = 1;
	public static final int SKIPC = 2;
	
	//Constructor
	protected ClusterFrame(TreeViewFrame f, String title, DataModel dataModel) { 
	  
		//Inherit constructor from JFrame, title passed from ClusterFrameWindow  
		super(title);
		
		//Initialize instance variables
		this.viewFrame = f;
		this.clusterModel = dataModel;
		
		outer = (ClusterModel) clusterModel;
		matrix = outer.getDataMatrix();
		dataArray = matrix.getExprData();
		rangeArray = Arrays.copyOfRange(dataArray, 50, 100);
	
		
		//Create the mainPanel
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(true);
		
		
		//set layout for initial window
		mainPanel.setLayout(new MigLayout());
		
		//First Window
		initialPanel = new InitialPanel();
		mainPanel.add(initialPanel, "grow, push, center");
		mainPanel.setBackground(new Color(51,204,255,100));
		
		//Add the mainPanel to the ContentPane
		getContentPane().add(mainPanel);
		
		//Makes the frame invisible when the window is closed
		addWindowListener(new WindowAdapter () {
			public void windowClosing(WindowEvent we) {
			    setVisible(false);
			}
		    });
		
		//packs items so that the frame fits them
		pack();
		screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		middle = new Point(screenSize.width / 2, screenSize.height / 2);
		newLocation = new Point(middle.x - (getWidth() / 2), 
		                              middle.y - (getHeight() / 2));
		setLocation(newLocation);
    }
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~New Panels~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~    
	
	//Check boxes throughout the GUI, declared outside their panels to increase scope for use in filter/ adjustment/cluster methods
	JCheckBox check1, check2, check3, check4, logCheck, centerGenes, normGenes, centerArrays, 
	  			normArrays, clusterGeneCheck, weightGeneCheck, clusterArrayCheck, weightArrayCheck;
	
	//ComboBoxes
	JComboBox geneCombo, arrayCombo;
  
	class InitialPanel extends JPanel {	
	  
		private static final long serialVersionUID = 1L;
		
	//	private URL rUrl;
	//	private BufferedImage img;
		
		//Instance variables
		int nRows, nCols; 
		JLabel label1, label2, numColLabel, numRowLabel;
		JButton yes_button, no_button, no2_button, loadNew_button;
		JTextArea textArea;
	    
		//Constructor
		public InitialPanel() {
			this.setLayout(new MigLayout());
			this.setBorder(BorderFactory.createTitledBorder("Data Information"));
			setResizable(true);
			setBackground(Color.white);
			
	    	HeaderInfo infoArray = clusterModel.getArrayHeaderInfo();
	    	HeaderInfo infoGene = clusterModel.getGeneHeaderInfo();
	    	
	    	//Accessing the loaded matrix
	//    	ClusterModel outer = (ClusterModel) clusterModel; 
	//    	ClusterModel.ClusterDataMatrix matrix = outer.getDataMatrix();
	//    	
//	    	System.out.println("Matrix Length: " + dataArray.length);
//	    	System.out.println("Matrix Sample: " + Arrays.toString(rangeArray));
	    	
	    	nCols = infoArray.getNumHeaders();
	    	nRows = infoGene.getNumHeaders();
//	    	System.out.println("Rows: " + nRows);
//	    	System.out.println("Columns: " + nCols);
	    	
	    	//textpanel
	//    	JPanel textPane = new JPanel();
	//    	textPane.setLayout(new MigLayout());
	    	
	//        try {
	//            rUrl = getParent().getClass().getResource("checkmark.png");
	//            System.out.println(rUrl);
	//            if (rUrl != null) {
	//                img = ImageIO.read(rUrl);
	//            }
	//        } catch (IOException ex) {
	//            
	//        }
	//    	
	//    	JLabel picLabel = new JLabel(new ImageIcon(img));
	//    	textPane.add(picLabel);
	    	
	    	label1 = new JLabel("Your file has been loaded!");
	    	label1.setFont(new Font("Sans Serif", Font.PLAIN, 16));
	    	label1.setBackground(Color.white);
	    	this.add(label1, "top, wrap");
	   
	    	
	    	label2 = new JLabel("It appears to have:");
	    	label2.setFont(new Font("Sans Serif", Font.PLAIN, 16));
	    	label2.setBackground(Color.white);
	    	this.add(label2, "wrap");
	    	
	//    	this.add(textPane, "wrap, grow, push");
	    	
	    	//panel with dimensions of the dataMatrix
	    	JPanel numPane = new JPanel();
	    	numPane.setLayout(new MigLayout());
	    	numPane.setBackground(Color.white);
	    	
	    	numColLabel = new JLabel(nCols + " columns");
	    	numColLabel.setBackground(Color.white);
	    	numColLabel.setFont(new Font("Sans Serif", Font.BOLD, 16));
	    	numPane.add(numColLabel, "span, split 2, center");
	    	
	    	numRowLabel = new JLabel(nRows + " rows");
	    	numRowLabel.setBackground(Color.white);
	    	numRowLabel.setFont(new Font("Sans Serif", Font.BOLD, 16));
	    	numPane.add(numRowLabel,  "gapleft 10%");
	    	
	    	this.add(numPane, "alignx 50%, growy, pushy");
	
	    	//buttonPane
	    	JPanel buttonPane1 = new JPanel();
	    	buttonPane1.setLayout(new MigLayout());
	    	buttonPane1.setBackground(Color.white);
	    	
	    	loadNew_button = new JButton("Load New File");
	    	loadNew_button.addActionListener(new ActionListener(){
	
				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					//ClusterFrame.this.dispatchEvent(new WindowEvent(ClusterFrame.this, WindowEvent.WINDOW_CLOSING));
					
					try {
						ClusterFileSet fileSet = clusterSelection();
						viewFrame.loadClusterFileSet(fileSet); 
	//					fileSet = clusterfileMru.addUnique(fileSet); File MRU = most recently used files
	//					clusterfileMru.setLast(fileSet);
	//					clusterfileMru.notifyObservers();
						viewFrame.setLoaded(true);
						ClusterFrame.this.dispatchEvent(new WindowEvent(ClusterFrame.this, WindowEvent.WINDOW_CLOSING));
						System.out.println("model: " + viewFrame.getDataModel());
						viewFrame.getClusterDialogWindow(viewFrame.getDataModel()).setVisible(true); //this doesnt load a new Dialog yet, doesnt laod clusterfilewindow
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
	 
	    	this.add(buttonPane1, "wrap, grow, push");
	    	
	    	textArea = new JTextArea("Would you like to filter out unwanted elements?");
	    	textArea.setFont(new Font("Sans Serif", Font.PLAIN, 16));
	    	textArea.setLineWrap(false);
	    	textArea.setOpaque(false);
	    	textArea.setEditable(false);
	    	
	    	this.add(textArea, "alignx 50%, span, wrap");
	    	
	    	JPanel buttonPane2 = new JPanel();
	    	buttonPane2.setLayout(new MigLayout());
	    	buttonPane2.setBackground(Color.white);
	    	
	    	yes_button = new JButton("Yes");
	    	yes_button.addActionListener(new ActionListener(){
	
				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					nextPanel(initialPanel, NOSKIP);
				}	
	    	});
	    	buttonPane2.add(yes_button);
	    	
	    	no_button = new JButton("Skip to 'Adjust Data'");
	    	no_button.addActionListener(new ActionListener(){
	
				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					nextPanel(initialPanel, SKIPA);
				}	
	    	});
	    	buttonPane2.add(no_button, "wrap");
	    	
	    	no2_button = new JButton("Skip to 'Cluster'");
	    	no2_button.addActionListener(new ActionListener(){
	
				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					nextPanel(initialPanel, SKIPC);
				}	
	    	});
	    	buttonPane2.add(no2_button, "span, alignx 50%");
	    	
	    	this.add(buttonPane2, "alignx 50%, span");	
		  }
		}
 
  
	  class FilterOptionsPanel extends JPanel {	
	
		private static final long serialVersionUID = 1L;
		
			JButton remove_button, noFilter_button, noFilter2_button;
			JTextArea instructions;
		    
			  public FilterOptionsPanel() {
				this.setLayout(new MigLayout());
				this.setBorder(BorderFactory.createTitledBorder("Filtering Data"));
				setResizable(false);
				setBackground(new Color(51,204,255,0));
		    
				//Instructions
				instructions = new JTextArea("Check all filter options which you would like to apply " +
						"to your dataset, then click 'Remove'.");
				instructions.setFont(new Font("Sans Serif", Font.BOLD, 16));
		    	instructions.setLineWrap(false);
		    	instructions.setOpaque(false);
		    	instructions.setEditable(false);
				this.add(instructions, "height :5%:, wrap");
				
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
		    	buttonPane.setBackground(new Color(51,204,255,0));
		    	
		    	//remove button with action listener
		    	remove_button = new JButton("Remove");
		    	remove_button.addActionListener(new ActionListener(){
	
					@Override
					public void actionPerformed(ActionEvent arg0) {
						
						nextPanel(filterOptions, NOSKIP);
					}	
		    	});
		    	buttonPane.add(remove_button);
		    	
		    	//Skip to adjust button
		    	noFilter_button = new JButton("Skip to 'Adjust Data'");
		    	noFilter_button.addActionListener(new ActionListener(){
	
					@Override
					public void actionPerformed(ActionEvent arg0) {
						
						nextPanel(filterOptions, SKIPA);
					}	
		    	});
		    	buttonPane.add(noFilter_button);
		    	
		    	//Skip to Cluster button
		    	noFilter2_button = new JButton("Skip to 'Cluster'");
		    	noFilter2_button.addActionListener(new ActionListener(){
	
					@Override
					public void actionPerformed(ActionEvent arg0) {
						
						nextPanel(filterOptions, SKIPC);
					}	
		    	});
		    	buttonPane.add(noFilter2_button);
		    	
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
				this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
				setResizable(true);
				setBackground(Color.white);
				
		    	check1 = new JCheckBox("Data Completeness per Element");
		    	check1.setBackground(Color.white);
		    	this.add(check1, "alignx 0%");
		    	
		    	JPanel bPane = new JPanel();
		    	bPane.setBackground(Color.white);
		    	info_button = new JButton("?");
		    	
		    	info_button.addActionListener(new ActionListener(){

					@Override
					public void actionPerformed(ActionEvent arg0) {
						new CFHelpDialog(1);
					}	
		    	});
		    	
		    	bPane.add(info_button);
		    	this.add(bPane, "wrap, pushx, growx");
		    	
		    	
		    	//valuefield
		    	percentField = new JTextField(textFieldSize);
		    	
		    	JPanel valuePane = new JPanel();
		    	valuePane.setLayout(new MigLayout());
		    	valuePane.setBackground(Color.white);
		    	
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
			//JLabel label1, label2, numColLabel, numRowLabel;
		    JTextArea textArea;
			JTextField sdField;
			JLabel enterLabel;
			int textFieldSize = 5;
			JButton info_button;
			    
				  public RemoveSDPanel() {
					this.setLayout(new MigLayout("", "[]push[]"));
					this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
					setSize(this.getSize());
					setResizable(true);
					setBackground(Color.white);
			    	
			    	check2 = new JCheckBox("Standard Deviation (Gene Vector)");
			    	check2.setBackground(Color.white);
			    	this.add(check2, "alignx 0%");
			    	
			    	JPanel bPane = new JPanel();
			    	bPane.setBackground(Color.white);
			    	info_button = new JButton("?");
			    	
			    	info_button.addActionListener(new ActionListener(){
	
						@Override
						public void actionPerformed(ActionEvent arg0) {
							new CFHelpDialog(2);
						}
			    	});
			    	
			    	bPane.add(info_button);
			    	this.add(bPane, " wrap");
			    	
			    	JPanel valuePane = new JPanel();
			    	valuePane.setLayout(new MigLayout());
			    	valuePane.setBackground(Color.white);
			    	
			    	sdField = new JTextField(textFieldSize);
			    	
			    	enterLabel = new JLabel();
			    	enterLabel.setBackground(Color.white);
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
					this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
					setResizable(true);
					setBackground(Color.white);
			    	
			    	check3 = new JCheckBox("Minimum Amount of Absolute Values");
			    	check3.setBackground(Color.white);
			    	this.add(check3, "alignx 0%");
			    	
			    	JPanel bPane = new JPanel();
			    	bPane.setBackground(Color.white);
			    	info_button = new JButton("?");
			    	
			    	info_button.addActionListener(new ActionListener(){
	
						@Override
						public void actionPerformed(ActionEvent arg0) {
							new CFHelpDialog(3);
						}
			    	});
			    	
			    	bPane.add(info_button, "alignx 0%");
			    	this.add(bPane, "wrap");
			    	
			    	obsvField = new JTextField(textFieldSize);
			    	
			    	absField = new JTextField(textFieldSize);
			    	
			    	JPanel valuePane = new JPanel();
			    	valuePane.setLayout(new MigLayout());
			    	valuePane.setBackground(Color.white);
			    	
			    	obsvLabel = new JLabel("Enter # of Observations: ");
			    	obsvLabel.setFont(new Font("Sans Serif", Font.PLAIN, 16));
			    	absLabel = new JLabel("Enter Specified Absolute Value: ");
			    	absLabel.setFont(new Font("Sans Serif", Font.PLAIN, 16));
			    	
			    	//valuePane.setBackground(Color.GREEN);
	
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
					this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
					setResizable(true);
					setBackground(Color.white);
			    	
			    	check4 = new JCheckBox("Difference of Maximum and Minimum Data Values");
			    	check4.setBackground(Color.white);
			    	this.add(check4, "alignx 0%");
			    	
			    	JPanel bPane = new JPanel();
			    	bPane.setBackground(Color.white);
			    	info_button = new JButton("?");
			    	
			    	info_button.addActionListener(new ActionListener(){
	
						@Override
						public void actionPerformed(ActionEvent arg0) {
							new CFHelpDialog(4);
						}
			    	});
			    	
			    	bPane.add(info_button, "alignx 100%");
			    	this.add(bPane, "wrap");
			    	
			    	diffField = new JTextField(textFieldSize);
			    	
			    	JPanel valuePane = new JPanel();
			    	valuePane.setBackground(Color.white);
			    	valuePane.setLayout(new MigLayout());
			    	
			    	diffLabel = new JLabel("Enter Specified Difference: ");
			    	diffLabel.setFont(new Font("Sans Serif", Font.PLAIN, 16));
	
			    	valuePane.add(diffLabel);
			    	valuePane.add(diffField,"gapleft 5%");
	
			    	this.add(valuePane);
			    	
				  }
				}
  
	  class AdjustQPanel extends JPanel {	
		  
		private static final long serialVersionUID = 1L;
		
		//Instance Variables
		int nRows, nCols; 
		JLabel label2, numColLabel, numRowLabel;
		JButton yes_button, no_button, no2_button, loadNew_button;
		JTextArea textArea;
		
		//Constructor
		public AdjustQPanel() {
			
			this.setLayout(new MigLayout());
			setResizable(true);
			setBackground(Color.white);
	
			this.setBorder(BorderFactory.createTitledBorder("Data Information"));
			
			HeaderInfo infoArray = clusterModel.getArrayHeaderInfo();
			HeaderInfo infoGene = clusterModel.getGeneHeaderInfo();
			
			nCols = infoArray.getNumHeaders();
			nRows = infoGene.getNumHeaders();
//			System.out.println("Rows: " + nRows);
//			System.out.println("Columns: " + nCols);
			
			//textpanel
			JPanel textPane = new JPanel();
			textPane.setLayout(new MigLayout());
			textPane.setBackground(Color.white);
			
			label2 = new JLabel("New dataset dimensions:");
			label2.setFont(new Font("Sans Serif", Font.PLAIN, 16));
			label2.setBackground(Color.white);
			textPane.add(label2);
			
			this.add(textPane, "wrap, grow, push");
			
			//panel with dimensions of the dataMatrix
			JPanel numPane = new JPanel();
			numPane.setLayout(new MigLayout());
			numPane.setBackground(Color.white);
			
			numColLabel = new JLabel(nCols + " columns");
			numColLabel.setFont(new Font("Sans Serif", Font.BOLD, 16));
			numColLabel.setBackground(Color.white);
			numPane.add(numColLabel);
			
			numRowLabel = new JLabel(nRows + " rows");
			numRowLabel.setFont(new Font("Sans Serif", Font.BOLD, 16));
			numRowLabel.setBackground(Color.white);
			numPane.add(numRowLabel, "gapleft 10%");
			
			this.add(numPane, "center, growy, pushy, wrap");
	
			
			textArea = new JTextArea("Would you like to mathematically adjust the dataset (log transform, normalizing etc.)?");
			textArea.setFont(new Font("Sans Serif", Font.PLAIN, 16));
			textArea.setLineWrap(false);
			textArea.setOpaque(false);
			textArea.setEditable(false);
			this.add(textArea, "alignx 50%, wrap");
			
			
			JPanel buttonPane2 = new JPanel();
			buttonPane2.setLayout(new MigLayout());
			buttonPane2.setBackground(Color.white);
			
			//Button to confirm intent to adjust data
			yes_button = new JButton("Yes");
			yes_button.addActionListener(new ActionListener(){
		
				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					nextPanel(adjustQPanel, NOSKIP);
				}	
			});
			buttonPane2.add(yes_button);
			
			//button to skip to clustering
			no_button = new JButton("Skip to 'Cluster'");
			no_button.addActionListener(new ActionListener(){
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					nextPanel(adjustQPanel, SKIPC);
				}	
			});
			buttonPane2.add(no_button, "wrap");
			
			//button to cancel the entire operation
			no2_button = new JButton("Cancel");
			no2_button.addActionListener(new ActionListener(){
				
				@Override
				public void actionPerformed(ActionEvent arg0){
					ClusterFrame.this.dispatchEvent(new WindowEvent(ClusterFrame.this, WindowEvent.WINDOW_CLOSING));
				}
			});
			
			buttonPane2.add(no2_button, "alignx 50%, span");
			
			this.add(buttonPane2, "alignx 50%, wrap");
		  	}
	  }
  
	  class AdjustOptionsPanel extends JPanel {	
	
			private static final long serialVersionUID = 1L;
				
			//Instance variables
			JButton adjust_button, noAdjust_button, info_button;
			JTextArea instructions;
			    
			public AdjustOptionsPanel() {
				
				this.setLayout(new MigLayout());
				this.setBorder(BorderFactory.createTitledBorder("Adjusting Data"));
				setResizable(true);
				setBackground(new Color(51,204,255,0));
		    
				//Instructions
				instructions = new JTextArea("Check all adjustment options which you would like to apply to your dataset, then click 'Adjust'.");
				instructions.setFont(new Font("Sans Serif", Font.BOLD, 16));
				instructions.setLineWrap(false);
				instructions.setOpaque(false);
				instructions.setEditable(false);
				this.add(instructions, "grow, push, wrap");
				
				//Splitting up the content of this panel into several other panels
				//Component 1
				JPanel logPanel = new JPanel();
				logPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
				logPanel.setLayout(new MigLayout("", "[]push[]"));
		    	logCheck = new JCheckBox("Log Transform");
		    	logCheck.setBackground(Color.white);
		    	logPanel.setBackground(Color.white);
		    	logPanel.add(logCheck, "grow, push");
		    	
		    	//Button to receiv more info about the log transform feature
		    	JPanel bPane = new JPanel();
		    	info_button = new JButton("?");
		    	bPane.add(info_button, "alignx 100%");
		    	bPane.setBackground(Color.white);
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
		    	adjust_button = new JButton("Adjust");
		    	adjust_button.addActionListener(new ActionListener(){
	
					@Override
					public void actionPerformed(ActionEvent arg0) {
						
						nextPanel(aoPanel, NOSKIP);
					}	
		    	});
		    	buttonPane.add(adjust_button);
		    	
		    	noAdjust_button = new JButton("Skip to 'Cluster'");
		    	noAdjust_button.addActionListener(new ActionListener(){
	
					@Override
					public void actionPerformed(ActionEvent arg0) {
						
						nextPanel(aoPanel, SKIPC);
					}	
		    	});
		    	buttonPane.add(noAdjust_button);
		    	
		    	buttonPane.setBackground(new Color(51,204,255,0));
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
		  		this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		  		setBackground(Color.white);
		    	
		  		//create checkbox
		  		centerGenes = new JCheckBox("Center Genes");
		  		centerGenes.setBackground(Color.white);
		  		this.add(centerGenes);
		  		
		    	JPanel bPane = new JPanel();
		    	info_button = new JButton("?");
		    	bPane.add(info_button, "alignx 100%");
		    	bPane.setBackground(Color.white);
		    	this.add(bPane, "wrap");
		  		
		  		//Create a new ButtonGroup
		  		bg = new ButtonGroup();
		  		
		  		mean = new JRadioButton("Mean", true);
		  		median = new JRadioButton("Median", false);
		  		mean.setBackground(Color.white);
		  		mean.setEnabled(false);
		  		median.setBackground(Color.white);
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
		  		normGenes.setBackground(Color.white);
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
		  		this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		  		setBackground(Color.white);
		  		
		  		//create checkbox
		  		centerArrays = new JCheckBox("Center Arrays");
		  		centerArrays.setBackground(Color.white);
		  		this.add(centerArrays);
		  		
		    	JPanel bPane = new JPanel();
		    	info_button = new JButton("?");
		    	bPane.add(info_button, "alignx 100%");
		    	bPane.setBackground(Color.white);
		    	this.add(bPane, "wrap");
		  		
		  		//Create a new ButtonGroup
		  		bg = new ButtonGroup();
		  		
		  		mean = new JRadioButton("Mean", true);
		  		median = new JRadioButton("Median", false);
		  		mean.setBackground(Color.white);
		  		mean.setEnabled(false);
		  		median.setBackground(Color.white);
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
		  		normArrays.setBackground(Color.white);
		  		this.add(normArrays);
				}
	  	}
		
		class ClusterOptionsPanel extends JPanel {	
		
			private static final long serialVersionUID = 1L;
			
			JButton centroid_button, single_button, complete_button, average_button;
			    
				 public ClusterOptionsPanel() {
					 this.setLayout(new MigLayout());
					 this.setBorder(BorderFactory.createTitledBorder("Hierarchical Clustering"));
					 setResizable(true);
					 setTitle("Hierarchical Clustering");
					 setBackground(new Color(51,204,255,0));
					
					//Component 1
					geneClusterPanel = new GeneClusterPanel();
					this.add(geneClusterPanel, "center, grow, push, split 2");
					
					//Component 2
					arrayClusterPanel = new ArrayClusterPanel();
					this.add(arrayClusterPanel, "center, grow, push, wrap");
					
					//Component 3
					JPanel buttonPanel = new JPanel();
					buttonPanel.setBorder(BorderFactory.createTitledBorder("Choose Cluster Method"));
					buttonPanel.setLayout(new MigLayout());
					
					//Component 4
					final JPanel loadPanel = new JPanel();
					loadPanel.setBorder(BorderFactory.createTitledBorder("Loading"));
					loadPanel.setLayout(new MigLayout());
			    	
			    	//button with action listener
			    	centroid_button = new JButton("Centroid Linkage");
			    	centroid_button.addActionListener(new ActionListener(){
			    		
			    
						@Override
						public void actionPerformed(ActionEvent arg0) {
							// All on Event Dispatch Thread
							//Build ProgressBar
							final JProgressBar pBar = new JProgressBar();
							pBar.setMinimum(0);
							pBar.setStringPainted(true);
							//pBar.setIndeterminate(true);
							pBar.setVisible(true);
							
							final JLabel clusterLabel = new JLabel("Clustering");
							loadPanel.add(clusterLabel, "wrap");
							
							//Add it to JPanel Object
							loadPanel.add(pBar, "push, grow");
							loadPanel.setBackground(Color.white);
							coPanel.add(loadPanel);
							mainPanel.revalidate();
							mainPanel.repaint();
							ClusterFrame.this.pack();
							
							final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {	
								
					    		//All not on Event Dispatch
								public Void doInBackground() {

						        	try {
										hCluster(2, dataArray, "What", pBar);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (ExecutionException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									return null;
								}
								
								protected void done(){
									
									clusterLabel.setForeground(Color.green);
									clusterLabel.setText("Clustering done!");
									mainPanel.revalidate();
									mainPanel.repaint();
									ClusterFrame.this.pack();	
								}
							};
							worker.execute();	
						}	
			    	});
			    	buttonPanel.add(centroid_button);
			    	
			    	//button with action listener
			    	single_button = new JButton("Single Linkage");
			    	single_button.addActionListener(new ActionListener(){
	
						@Override
						public void actionPerformed(ActionEvent arg0) {
							
							//nextPanel(filterOptions, NOSKIP);
						}	
			    	});
			    	buttonPanel.add(single_button);
			    	
			    	//button with action listener
			    	complete_button = new JButton("Complete Linkage");
			    	complete_button.addActionListener(new ActionListener(){
	
						@Override
						public void actionPerformed(ActionEvent arg0) {
							
							//nextPanel(filterOptions, NOSKIP);
						}	
			    	});
			    	buttonPanel.add(complete_button);
			    	
			    	//button with action listener
			    	average_button = new JButton("Average Linkage");
			    	average_button.addActionListener(new ActionListener(){
	
						@Override
						public void actionPerformed(ActionEvent arg0) {
							
							//nextPanel(filterOptions, NOSKIP);
						}	
			    	});
			    	buttonPanel.add(average_button);
			    	
	
			    	buttonPanel.setBackground(Color.white);
			    	this.add(buttonPanel, "top, alignx 50%, push, wrap");
			    	
				  }
				}
		
		//Options for comboboxes used by 2 classes
		String[] measurements = {"Correlation (uncentered)", "Correlation (centered)", "Absolute Correlation (uncentered)",
				"Absolute Correlation (centered)", "Spearman Rank Correlation", "Kendall's Tau", "Euclidean Distance", "City Block Distance"};
	
		//label used by 2 classes
		JLabel similarity;
	
		class GeneClusterPanel extends JPanel {

			JButton info_button;
	      
			private static final long serialVersionUID = 1L;

				public GeneClusterPanel() {
					
			  		//set this panel's layout
			  		this.setLayout(new MigLayout("", "[]push[]"));
			  		this.setBorder(BorderFactory.createTitledBorder("Genes"));
					setResizable(true);
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
			  		
			  		geneCombo = new JComboBox(measurements);
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
		  		this.setBorder(BorderFactory.createTitledBorder("Arrays"));
				setResizable(true);
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
		  		
		  		arrayCombo = new JComboBox(measurements);
		  		arrayCombo.setBackground(Color.white);
		  		similarity = new JLabel("Similarity Metric");
		  		similarity.setBackground(Color.white);
		  		
		  		this.add(similarity, "alignx 50%, span, wrap");
		  		this.add(arrayCombo, "alignx 50%, span");
			}
		}
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/**
	 * Erases previous panel from mainPanel and draws a new one on it
	 * @param the new Panel or dialog to be opened
	 */
  	private void nextPanel(JPanel oldPanel, int skip){
  		
  		if(skip == NOSKIP){
  			
  			if(oldPanel == initialPanel){
 
  			
  			mainPanel.remove(initialPanel);
  			
  			filterOptions = new FilterOptionsPanel();
  			mainPanel.add(filterOptions, "grow, push, center");
  			
  			mainPanel.revalidate();
  			mainPanel.repaint();
  			this.pack();
  			setLocation(newLocation);
  			filterOptions.setVisible(true);
	  		}
	  		else if(oldPanel == filterOptions){
	  			
	  			mainPanel.remove(filterOptions);
	  			
	  			adjustQPanel = new AdjustQPanel();
	  			mainPanel.add(adjustQPanel, "grow, push, center");
	  			
	  			mainPanel.revalidate();
	  			mainPanel.repaint();
	  			this.pack();
	  			setLocation(newLocation);
	  			adjustQPanel.setVisible(true);
	  		}
	  		
	  		else if(oldPanel == adjustQPanel){
	  			
	  			mainPanel.remove(adjustQPanel);
	  			
	  			aoPanel = new AdjustOptionsPanel();
	  			mainPanel.add(aoPanel, "grow, push, center");
	  			
	  			mainPanel.revalidate();
	  			mainPanel.repaint();
	  			this.pack();
	  			setLocation(newLocation);
	  			aoPanel.setVisible(true);
	  		}
	  		else if(oldPanel == aoPanel){
	  			
	  			mainPanel.remove(aoPanel);
	  			
	  			coPanel = new ClusterOptionsPanel();
	  			coPanel.setVisible(true);
	  			mainPanel.add(coPanel,"grow, push, center");
	  			
	  			mainPanel.revalidate();
	  			mainPanel.repaint();
	  			this.pack();
	  			setLocation(newLocation);
	  			coPanel.setVisible(true);
	  		}
  		}
  		else if (skip == SKIPA){
  			mainPanel.remove(oldPanel);
  			
  			aoPanel = new AdjustOptionsPanel();
  			aoPanel.setVisible(true);
  			mainPanel.add(aoPanel, "grow, push, center");
  			
  			mainPanel.revalidate();
  			mainPanel.repaint();
  			this.pack();
  		}
  		
  		else if (skip == SKIPC){
  			mainPanel.remove(oldPanel);
  			
  			coPanel = new ClusterOptionsPanel();
  			coPanel.setVisible(true);
  			mainPanel.add(coPanel, "grow, push, center");
  			
  			mainPanel.revalidate();
  			mainPanel.repaint();
  			this.pack();
  		}
  	}
	
  	javax.swing.Timer loadTimer;

//	/**
//	* Function for the hierarchical clustering operation
//	 * @throws LoadException 
//	* 
//	*/
	private void hCluster(int method, double[] currentArray, String similarityM, JProgressBar pBar) 
			throws InterruptedException, ExecutionException{
		
		System.out.println("EventDispatch hCluster: " + javax.swing.SwingUtilities.isEventDispatchThread());
		//make a HierarchicalCluster object
		HierarchicalCluster clusterTarget = new HierarchicalCluster((ClusterModel)clusterModel, currentArray, method, similarityM);
		
		//begin operations on clusterTarget...
		List<double[]> aoArrays = clusterTarget.splitArray(currentArray, pBar);
		
		//use int method to determine the distance/ similarity matrix algorithm
		//return distance/ similarity matrix and use as input to clustering function
		
		//int listRep = aoArrays.size();
		
//		System.out.println("ArrayList Length: " + listRep);
//		System.out.println("ArrayList Element: " + Arrays.toString(aoArrays.get(0)));

		List<double[]> geneDistances  = clusterTarget.euclid(aoArrays, pBar);
		
		clusterTarget.cluster(geneDistances, pBar);
		
	}
	
    /**
     * Remove genes from the originally loaded data set according to
     * which checkboxes have been selected.
     * Is called when apply button is clicked.
     * 
     * @param sub
     * @param caseSensative
     */
    private void removeGenes(String sub, boolean casePresent, boolean caseSD,
    		boolean caseAbsolute, boolean caseMaxMin) {
		
		
		if (casePresent == true){
			
			
		}
		
		else if(caseSD == true){
			
		}
		
		else if(caseAbsolute == true){
			
		}
		
		else if(caseMaxMin == true){
	
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
    
    ValueTextField MaxMin_text;
	JCheckBox caseBox4;
	
    
    class NumPanel extends JPanel {	
    	
    	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		int nRows;
        int nCols; 
        JLabel numRowLabel;
        JLabel numColLabel;

        
//        IntHeaderInfoCluster info = new IntHeaderInfoCluster();
//        int geneN = info.getNumHeaders();
        
      public NumPanel() {

    	int strut = 20;
    	setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    	setMaximumSize(new Dimension(400, 50));
    	
    	if(clusterModel == null){
		System.out.println("Hnnnnng");
    	}else{
    		System.out.println("Informationics:" + clusterModel);
    	}
    	
    	HeaderInfo infoArray = clusterModel.getArrayHeaderInfo();
    	HeaderInfo infoGene = clusterModel.getGeneHeaderInfo();
    	
    	//Access content directly!
    	//When accessing the dataMatrix the program does not leave out initial description cells (EWEIGHT etc.)
 
    	
    	DataMatrix dM = clusterModel.getDataMatrix();
    	double valueD = dM.getValue(0, 213);				//starts with DATA values and with index 0 (first data element: 0, 0)
    	System.out.println("DataMatrixValue: " + valueD);
    	
    	//get the double[] somehow....
    	
//    	Accessing header (first two) elements directly
    	
    	String[] aName = infoArray.getHeader(6);
    	System.out.println("aName: " + aName[0]);
    	System.out.println("aName: " + aName[1]);
    	
    	String[] gName = infoGene.getHeader(6);
    	System.out.println("gName: " + gName[0]);
    	System.out.println("gName: " + gName[1]);
    	
    	nCols = infoArray.getNumHeaders();
    	nRows = infoGene.getNumHeaders();
    	System.out.println("Rows: " + nRows);
    	System.out.println("Columns: " + nCols);
    	
    	
    	numColLabel = new JLabel("Number of Columns: " + nCols);
    	add(numColLabel);
    	add(Box.createHorizontalStrut(strut));
    	numRowLabel = new JLabel("Number of Rows: " + nRows);
    	add(numRowLabel);
    	numRowLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    	numColLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
      }
    }
    
    class ValueTextField extends JTextField implements ActionListener {
	    /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		// why does java make me write this dumb constructor?
		//cols = size of textfield
	    ValueTextField(int cols) {super(cols);}

	    public void actionPerformed(ActionEvent e) {
//	    	removeGenes(getText(), caseBox.isSelected(),caseBox2.isSelected(),
//	    			caseBox3.isSelected(), caseBox4.isSelected());
	    }
	}
    
	class TimerListener implements ActionListener { // manages the FileLoader
		// this method is invoked every few hundred ms
		public void actionPerformed(ActionEvent evt) {
		
			//Toolkit.getDefaultToolkit().beep();

		}
	}

//	protected TreeSelectionI geneSelection;
//	protected TreeViewFrame viewFrame;
//	protected DataModel clusterModel;
}
