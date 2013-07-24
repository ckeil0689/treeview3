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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;

import net.miginfocom.swing.MigLayout;
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
	private JScrollPane scrollPane;
	private JPanel mainPanel;
	//private QuestionPanel questionPanel;
	private JLabel head1, head2;
	private FilterOptionsPanel filterOptions;
	private RemovePercentPanel percentPanel;
	private RemoveSDPanel sdPanel;
	private RemoveAbsPanel absPanel;
	private AdjustOptionsPanel aoPanel;
	private MaxMinPanel maxMinPanel;
	private GeneAdjustPanel geneAdjustPanel;
	private ArrayAdjustPanel arrayAdjustPanel;
	private JPanel closeButtonPane;
	private JButton back, close;
	
	//Object of the loaded model and matrix 
	private ClusterModel outer;
	private ClusterModel.ClusterDataMatrix matrix;
	
	//Instance variable in which the loaded data array is being stored
	private double[] dataArray;
	private double[] rangeArray;
	
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
		
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	
		
		//setup frame options
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setResizable(true);
		
		//set layout for initial window
		mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.setBackground(Color.white);
		mainPanel.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
		
		head1 = new JLabel("Filter Data >");
		head1.setFont(new Font("Sans Serif", Font.BOLD, 28));
		head1.setForeground(new Color(110, 210, 255, 255));
		mainPanel.add(head1, "pushx, growx, wrap");
		head1.setVisible(true);
		
		filterOptions = new FilterOptionsPanel();
		
		head2 = new JLabel("Adjust Data >");
		head2.setFont(new Font("Sans Serif", Font.BOLD, 28));
		head2.setForeground(new Color(110, 210, 255, 255));
		mainPanel.add(head2, "pushx, growx, wrap");
		head2.setVisible(true);
		
		aoPanel = new AdjustOptionsPanel();
		
		closeButtonPane = new JPanel();
		closeButtonPane.setLayout(new MigLayout());
		closeButtonPane.setVisible(true);
		
		
		close = new JButton("<< Back");
		close.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				ClusterFrame.this.dispose();
				
			}
			
		});
		closeButtonPane.setOpaque(false);
		closeButtonPane.add(close);
		
		mainPanel.add(closeButtonPane, "alignx 50%, pushx, bottom");
		
		head1.addMouseListener(new MouseListener(){

			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				if(filterOptions.isShowing()){
					
					mainPanel.removeAll();
					mainPanel.add(head1, "pushx, growx, wrap");
					mainPanel.add(head2, "pushx, growx, wrap");
					head1.setText("Filter Data >");
					head2.setText("Adjust Data >");
					mainPanel.add(closeButtonPane, "alignx 50%, pushx");
					mainPanel.revalidate();
					mainPanel.repaint();
				}
				else{
					mainPanel.removeAll();
					head1.setText("Filter Data v");
					mainPanel.add(head1, "pushx, growx, wrap");
					mainPanel.add(filterOptions, "grow, push, wrap");
					head2.setText("Adjust Data >");
					mainPanel.add(head2, "pushx, growx, wrap");
					mainPanel.add(closeButtonPane, "alignx 50%, pushx");
					mainPanel.revalidate();
					mainPanel.repaint();
				}


			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				
				head1.setForeground(Color.LIGHT_GRAY);
				
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				
				head1.setForeground(new Color(110, 210, 255, 255));
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		head2.addMouseListener(new MouseListener(){

			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				if(aoPanel.isShowing()){
					
					mainPanel.removeAll();
					mainPanel.add(head1, "alignx 50%, pushx, growx, wrap");
					mainPanel.add(head2, "alignx 50%, pushx, growx, wrap");
					head1.setText("Filter Data >");
					head2.setText("Adjust Data >");
					mainPanel.add(closeButtonPane, "alignx 50%, pushx");
					mainPanel.revalidate();
					mainPanel.repaint();
				}
				else{
					mainPanel.removeAll();
					head1.setText("Filter Data >");
					mainPanel.add(head1, "alignx 50%, pushx, growx, wrap");
					head2.setText("Adjust Data v");
					mainPanel.add(head2, "alignx 50%, pushx, growx, wrap");
					mainPanel.add(aoPanel, "grow, push, wrap");
					mainPanel.add(closeButtonPane, "alignx 50%, pushx");
					mainPanel.revalidate();
					mainPanel.repaint();
				}

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				
				head2.setForeground(Color.LIGHT_GRAY);
				
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				
				head2.setForeground(new Color(110, 210, 255, 255));
				
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		
		//make mainpanel scrollable by adding it to scrollpane
		scrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		//Add the mainPanel to the ContentPane
		getContentPane().add(scrollPane);
		
		//Makes the frame invisible when the window is closed
		addWindowListener(new WindowAdapter () {
			public void windowClosing(WindowEvent we) {
			    setVisible(false);
			}
		    });
		
		//packs items so that the frame fits them
		pack();
		setLocationRelativeTo(null);
    }
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~New Panels~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~    
	
	//Check boxes throughout the GUI, declared outside their panels to increase scope for use in filter/ adjustment/cluster methods
	private JCheckBox check1, check2, check3, check4, logCheck, centerGenes, normGenes, centerArrays, 
	  			normArrays;

	
	 class FilterOptionsPanel extends JPanel {	
	
		private static final long serialVersionUID = 1L;
		
		private JButton remove_button;
		private JTextArea instructions;
		    
		public FilterOptionsPanel() {
			
			this.setLayout(new MigLayout());
			this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
			setResizable(false);
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
			
	    	//panel with all the buttons
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
		
			private JLabel label;
			private JTextField percentField;
			private int textFieldSize = 5;
			private JButton info_button;

			  public RemovePercentPanel() {
				this.setLayout(new MigLayout("", "[]push[]"));
				setResizable(true);
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
		    	
		    	label = new JLabel();
		    	label.setText("Enter Percentage: ");
		    	label.setFont(new Font("Sans Serif", Font.PLAIN, 16));
		    	
		    	valuePane.add(label, "alignx 0%");
		    	valuePane.add(percentField);

		    	this.add(valuePane);
			  }
			}
  
	  class RemoveSDPanel extends JPanel {	
	
			private static final long serialVersionUID = 1L;
			
			private JTextField sdField;
			private JLabel enterLabel;
			private int textFieldSize = 5;
			private JButton info_button;
			    
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
			
			private JLabel obsvLabel, absLabel;
			private JTextField obsvField, absField;;
			private int textFieldSize = 5;
			private JButton info_button;
			    
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
			
			private JLabel diffLabel;
			private JTextField diffField;;
			private int textFieldSize = 5;
			private JButton info_button;
			    
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
			private JButton adjust_button, info_button;
			private JTextArea instructions;
			    
			public AdjustOptionsPanel() {
				
				this.setLayout(new MigLayout());
				this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
				setResizable(true);
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
		  private JRadioButton mean;
		  private JRadioButton median;
		  private ButtonGroup bg;
		  private JButton info_button;
	      
			private static final long serialVersionUID = 1L;
			
			//Constructor
			public GeneAdjustPanel() {
				
		  		//set this panel's layout
		  		this.setLayout(new MigLayout("", "[]push[]"));
		  		//setBackground(new Color(210, 210, 210, 70));
		    	
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
	      private JRadioButton mean;
	      private JRadioButton median;
	      private ButtonGroup bg;
	      private JButton info_button;
	      
	      private static final long serialVersionUID = 1L;
			
			//Constructor
			public ArrayAdjustPanel() {
				//set this panel's layout
				this.setLayout(new MigLayout("", "[]push[]"));
				//setBackground(new Color(210, 210, 210, 70));
				
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

}
