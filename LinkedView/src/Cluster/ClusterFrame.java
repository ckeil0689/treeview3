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
	private JPanel mainPanel, backgroundPanel, closeButtonPane, optionsPanel;
	private JLabel head1, head2, titleLine, description1, description2;
	private FilterOptionsPanel filterOptions;
	private RemovePercentPanel percentPanel;
	private RemoveSDPanel sdPanel;
	private RemoveAbsPanel absPanel;
	private AdjustOptionsPanel aoPanel;
	private MaxMinPanel maxMinPanel;
	private GeneAdjustPanel geneAdjustPanel;
	private ArrayAdjustPanel arrayAdjustPanel;
	private JButton close_button;
	
	//Object of the loaded model and matrix 
	private ClusterModel outer;
	private ClusterModel.ClusterDataMatrix matrix;
	
	//Instance variable in which the loaded data array is being stored
	private double[] dataArray;
	
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
		
		//setup frame options
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setResizable(true);
		
		//set layout for initial window
		mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.setBackground(Color.white);
		
		backgroundPanel = new JPanel();
		backgroundPanel.setLayout(new MigLayout());
		backgroundPanel.setPreferredSize(viewFrame.getSize());
		
		titleLine = new JLabel("Advanced Options");
		titleLine.setFont(new Font("Sans Serif", Font.BOLD, 28));
		mainPanel.add(titleLine, "pushx, growx, wrap");
		
		description1 = new JLabel("This menu allows you to filter out unwanted elements from your dataset.");
		description1.setFont(new Font("Sans Serif", Font.PLAIN, 22));
		mainPanel.add(description1, "pushx, growx, wrap");
		
		description2 = new JLabel("Additionally, you can perform mathematical adjustments.");
		description2.setFont(new Font("Sans Serif", Font.PLAIN, 22));
		mainPanel.add(description2, "pushx, growx, wrap");
		
		optionsPanel = new JPanel();
		optionsPanel.setLayout(new MigLayout());
		optionsPanel.setOpaque(false);
		optionsPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		
		head1 = new JLabel("Filter Data >>");
		head1.setFont(new Font("Sans Serif", Font.PLAIN, 28));
		head1.setForeground(new Color(60, 180, 220, 255));
		optionsPanel.add(head1, "pushx, growx, span, wrap");
		head1.setVisible(true);
		
		filterOptions = new FilterOptionsPanel();
		
		head2 = new JLabel("Adjust Data >>");
		head2.setFont(new Font("Sans Serif", Font.PLAIN, 28));
		head2.setForeground(new Color(60, 180, 220, 255));
		optionsPanel.add(head2, "pushx, growx, span");
		head2.setVisible(true);
		
		mainPanel.add(optionsPanel, "grow, push");
		
		aoPanel = new AdjustOptionsPanel();
		
		closeButtonPane = new JPanel();
		closeButtonPane.setLayout(new MigLayout());
		closeButtonPane.setVisible(true);
		
		
		close_button = new JButton("Close");
		close_button.setOpaque(true);
		close_button.setBackground(new Color(60, 180, 220, 255));
		close_button.setForeground(Color.white);
		Dimension d = close_button.getPreferredSize();
		d.setSize(d.getWidth()*1.5, d.getHeight()*1.5);
		close_button.setFont(new Font("Sans Serif", Font.PLAIN, 18));
		close_button.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				ClusterFrame.this.dispose();
				
			}
			
		});
		closeButtonPane.setOpaque(false);
		closeButtonPane.add(close_button);
		
		head1.addMouseListener(new MouseListener(){

			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				if(filterOptions.isShowing()){
					
					optionsPanel.removeAll();
					optionsPanel.add(head1, "pushx, growx, wrap");
					optionsPanel.add(head2, "pushx, growx, wrap");
					head1.setText("Filter Data >>");
					head2.setText("Adjust Data >>");
					head1.setOpaque(false);
					mainPanel.revalidate();
					mainPanel.repaint();
				}
				else{
					optionsPanel.removeAll();
					head1.setText("Filter Data v");
					//head1.setBackground(new Color(110, 210, 255, 255));
					head1.setForeground(new Color(240, 80, 50, 255));
					//head1.setOpaque(true);
					optionsPanel.add(head1, "pushx, growx, wrap");
					optionsPanel.add(filterOptions, "grow, push, wrap");
					head2.setText("Adjust Data >>");
					head2.setForeground(new Color(60, 180, 220, 255));
					head2.setOpaque(false);
					optionsPanel.add(head2, "pushx, growx, wrap");
					mainPanel.revalidate();
					mainPanel.repaint();
				}


			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				
				if(filterOptions.isShowing()){
					
				}
				else{
					
					head1.setForeground(Color.LIGHT_GRAY);

				}
				
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				
				if(filterOptions.isShowing()){
					
				}
				else{
					
					head1.setForeground(new Color(60, 180, 220, 255));
				}
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
					
					optionsPanel.removeAll();
					optionsPanel.add(head1, "alignx 50%, pushx, growx, wrap");
					optionsPanel.add(head2, "alignx 50%, pushx, growx, wrap");
					head1.setText("Filter Data >>");
					head2.setText("Adjust Data >>");
					head2.setOpaque(false);
					mainPanel.revalidate();
					mainPanel.repaint();
				}
				else{
					optionsPanel.removeAll();
					head1.setText("Filter Data >>");
					head1.setForeground(new Color(60, 180, 220, 255));
					head1.setOpaque(false);
					optionsPanel.add(head1, "alignx 50%, pushx, growx, wrap");
					head2.setText("Adjust Data v");
					//head2.setBackground(new Color(110, 210, 255, 255));
					head2.setForeground(new Color(240, 80, 50, 255));
					//head2.setOpaque(false);
					optionsPanel.add(head2, "alignx 50%, pushx, growx, wrap");
					optionsPanel.add(aoPanel, "grow, push, wrap");
					mainPanel.revalidate();
					mainPanel.repaint();
				}

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				
				if(aoPanel.isShowing()){
					
				}
				else{
					
					head2.setForeground(Color.LIGHT_GRAY);
				}
				
				
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				
				if(aoPanel.isShowing()){
					
				}
				else{
					
					head2.setForeground(new Color(60, 180, 220, 255));
				}
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
		
		backgroundPanel.add(mainPanel, "growx, pushx, wrap");
		backgroundPanel.add(closeButtonPane, "alignx 50%, pushx");
		
		//make scrollable by adding it to scrollpane
		scrollPane = new JScrollPane(backgroundPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
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
		private JLabel instructions;
		    
		public FilterOptionsPanel() {
			
			this.setLayout(new MigLayout());
			this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
			setResizable(false);
			setBackground(Color.WHITE);
	    
			//Instructions
			instructions = new JLabel("Check all filter options you would like to apply. Then click 'Remove'.");
			instructions.setFont(new Font("Sans Serif", Font.PLAIN, 22));
	    	instructions.setOpaque(false);
			//this.add(instructions, "wrap");
			
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
	    	remove_button.setOpaque(true);
	    	remove_button.setBackground(new Color(60, 180, 220, 255));
	    	remove_button.setForeground(Color.white);
			Dimension d = remove_button.getPreferredSize();
			d.setSize(d.getWidth()*1.5, d.getHeight()*1.5);
			remove_button.setFont(new Font("Sans Serif", Font.PLAIN, 18));
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
		
			private JLabel label, description;
			private JTextField percentField;
			private int textFieldSize = 5;

			  public RemovePercentPanel() {
				this.setLayout(new MigLayout("", "[]push[]"));
				setResizable(true);
				
		    	check1 = new JCheckBox("Incomplete Elements");
		    	check1.setFont(new Font("Sans Serif", Font.BOLD, 18));
		    	//check1.setForeground(new Color(60, 180, 220, 255));
		    	check1.setOpaque(false);
		    	this.add(check1, "alignx 0%, wrap");
		    		    	
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
		    	label.setFont(new Font("Sans Serif", Font.PLAIN, 18));
		    	
		    	valuePane.add(label, "alignx 0%, pushx");
		    	valuePane.add(percentField);

		    	this.add(valuePane, "span, wrap");
		    	
		    	description = new JLabel("> ?");
		    	description.setFont(new Font("Sans Serif", Font.BOLD, 14));
		    	description.setForeground(new Color(60, 180, 220, 255));
		    	description.addMouseListener(new MouseListener(){

					@Override
					public void mouseClicked(MouseEvent arg0) {
						
						if(description.getText().equalsIgnoreCase("> ?")){
							
							description.setText("< Remove all elements which exceed a percentage limit " +
									"of missing values in their columns.");
							description.setFont(new Font("Sans Serif", Font.ITALIC, 14));
							description.setForeground(Color.black);
						}
						else{
							
							description.setText("> ?");
							description.setFont(new Font("Sans Serif", Font.BOLD, 14));
							description.setForeground(new Color(60, 180, 220, 255));
						}
						
					}

					@Override
					public void mouseEntered(MouseEvent arg0) {
						
						if(description.getText().equalsIgnoreCase("> ?")){
							
							description.setForeground(new Color(240, 80, 50, 255));
							
						}
						else{
							
							description.setForeground(Color.GRAY);
						}
					}

					@Override
					public void mouseExited(MouseEvent arg0) {
						
						if(description.getText().equalsIgnoreCase("> ?")){
							
							description.setForeground(new Color(60, 180, 220, 255));
						}
						else{
							
							description.setForeground(Color.BLACK);
						}
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
		    	this.add(description);
			  }
	  }
  
	  class RemoveSDPanel extends JPanel {	
	
			private static final long serialVersionUID = 1L;
			
			private JTextField sdField;
			private JLabel enterLabel, description;
			private int textFieldSize = 5;
			    
			public RemoveSDPanel() {
					  
				this.setLayout(new MigLayout("", "[]push[]"));
				setSize(this.getSize());
		    	
		    	check2 = new JCheckBox("Minimum Standard Deviation");
		    	check2.setFont(new Font("Sans Serif", Font.BOLD, 18));
		    	//check2.setForeground(new Color(60, 180, 220, 255));
		    	check2.setOpaque(false);
		    	this.add(check2, "alignx 0%, wrap");
		    	
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
		    	enterLabel.setFont(new Font("Sans Serif", Font.PLAIN, 18));
		    	
		    	valuePane.add(enterLabel, "alignx 0%, pushx");
		    	valuePane.add(sdField);

		    	this.add(valuePane, "span, wrap");
		    	
		    	description = new JLabel("> ?");
		    	description.setFont(new Font("Sans Serif", Font.BOLD, 14));
		    	description.setForeground(new Color(60, 180, 220, 255));
		    	description.addMouseListener(new MouseListener(){

					@Override
					public void mouseClicked(MouseEvent arg0) {
						
						if(description.getText().equalsIgnoreCase("> ?")){
							
							description.setText("< Remove all elements with standard deviations " +
									"of their values less than a set value.");
							description.setFont(new Font("Sans Serif", Font.ITALIC, 14));
							description.setForeground(Color.black);
						}
						else{
							
							description.setText("> ?");
							description.setFont(new Font("Sans Serif", Font.BOLD, 14));
							description.setForeground(new Color(60, 180, 220, 255));
						}
						
					}

					@Override
					public void mouseEntered(MouseEvent arg0) {
						
						if(description.getText().equalsIgnoreCase("> ?")){
							
							description.setForeground(new Color(240, 80, 50, 255));
							
						}
					}

					@Override
					public void mouseExited(MouseEvent arg0) {
						
						if(description.getText().equalsIgnoreCase("> ?")){
							
							description.setForeground(new Color(60, 180, 220, 255));
						}
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
		    	this.add(description);
			}
	  }
  
	  class RemoveAbsPanel extends JPanel {	
		  
	
			private static final long serialVersionUID = 1L;
			
			private JLabel obsvLabel, absLabel, description;
			private JTextField obsvField, absField;;
			private int textFieldSize = 5;
			    
			public RemoveAbsPanel() {
				
				this.setLayout(new MigLayout("", "[]push[]"));
			    	
		    	check3 = new JCheckBox("Observations of Minimum Absolute Values");
		    	check3.setFont(new Font("Sans Serif", Font.BOLD, 18));
		    	//check3.setForeground(new Color(60, 180, 220, 255));
		    	check3.setOpaque(false);
		    	this.add(check3, "alignx 0%, wrap");
		    	
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
		    	obsvLabel.setFont(new Font("Sans Serif", Font.PLAIN, 18));
		    	absLabel = new JLabel("Enter Specified Absolute Value: ");
		    	absLabel.setFont(new Font("Sans Serif", Font.PLAIN, 18));

		    	valuePane.add(obsvLabel, "alignx 0%");
		    	valuePane.add(obsvField, "wrap");
		    	valuePane.add(absLabel);
		    	valuePane.add(absField);

		    	this.add(valuePane, "span, wrap");
		    	
		    	description = new JLabel("> ?");
		    	description.setFont(new Font("Sans Serif", Font.BOLD, 14));
		    	description.setForeground(new Color(60, 180, 220, 255));
		    	description.addMouseListener(new MouseListener(){

					@Override
					public void mouseClicked(MouseEvent arg0) {
						
						if(description.getText().equalsIgnoreCase("> ?")){
							
							description.setText("< Remove all elements below a minimum amount " +
		    			"of observations of absolute values less than a specified value.");
							description.setFont(new Font("Sans Serif", Font.ITALIC, 14));
							description.setForeground(Color.black);
						}
						else{
							
							description.setText("> ?");
							description.setFont(new Font("Sans Serif", Font.BOLD, 14));
							description.setForeground(new Color(60, 180, 220, 255));
						}
						
					}

					@Override
					public void mouseEntered(MouseEvent arg0) {
						
						if(description.getText().equalsIgnoreCase("> ?")){
							
							description.setForeground(new Color(240, 80, 50, 255));
							
						}
					}

					@Override
					public void mouseExited(MouseEvent arg0) {
						
						if(description.getText().equalsIgnoreCase("> ?")){
							
							description.setForeground(new Color(60, 180, 220, 255));
						}
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
		    	this.add(description);
			}
	  }
  
	  class MaxMinPanel extends JPanel {	
	
			private static final long serialVersionUID = 1L;
			
			private JLabel diffLabel, description;
			private JTextField diffField;;
			private int textFieldSize = 5;
			    
			public MaxMinPanel() {
				
				this.setLayout(new MigLayout("", "[]push[]"));
		    	
		    	check4 = new JCheckBox("Difference of Maximum and Minimum Values");
		    	check4.setFont(new Font("Sans Serif", Font.BOLD, 18));
		    	//check4.setForeground(new Color(60, 180, 220, 255));
		    	check4.setOpaque(false);
		    	this.add(check4, "alignx 0%, wrap");
		    	
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
		    	diffLabel.setFont(new Font("Sans Serif", Font.PLAIN, 18));

		    	valuePane.add(diffLabel, "alignx 0%" );
		    	valuePane.add(diffField);

		    	this.add(valuePane, "span, wrap");
		    	
		    	description = new JLabel("> ?");
		    	description.setFont(new Font("Sans Serif", Font.BOLD, 14));
		    	description.setForeground(new Color(60, 180, 220, 255));
		    	description.addMouseListener(new MouseListener(){

					@Override
					public void mouseClicked(MouseEvent arg0) {
						
						if(description.getText().equalsIgnoreCase("> ?")){
							
							description.setText("< Remove all elements with a difference of their maximum " +
									"and minimum values below a set value.");
							description.setFont(new Font("Sans Serif", Font.ITALIC, 14));
							description.setForeground(Color.black);
						}
						else{
							
							description.setText("> ?");
							description.setFont(new Font("Sans Serif", Font.BOLD, 14));
							description.setForeground(new Color(60, 180, 220, 255));
						}
						
					}

					@Override
					public void mouseEntered(MouseEvent arg0) {
						
						if(description.getText().equalsIgnoreCase("> ?")){
							
							description.setForeground(new Color(240, 80, 50, 255));
							
						}
					}

					@Override
					public void mouseExited(MouseEvent arg0) {
						
						if(description.getText().equalsIgnoreCase("> ?")){
							
							description.setForeground(new Color(60, 180, 220, 255));
						}
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
		    	this.add(description);
		    	
			}
	  }
  
	  class AdjustOptionsPanel extends JPanel {	
	
			private static final long serialVersionUID = 1L;
				
			//Instance variables
			private JButton adjust_button;
			private JLabel instructions, title;
			    
			public AdjustOptionsPanel() {
				
				this.setLayout(new MigLayout());
				this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
				setResizable(true);
				setBackground(Color.WHITE);
		    
				//Instructions
				instructions = new JLabel("Check all adjustment options you would like to apply. Then click 'Adjust'.");
				instructions.setFont(new Font("Sans Serif", Font.PLAIN, 22));
				instructions.setOpaque(false);
				//this.add(instructions, "grow, push, wrap");
				
				//Splitting up the content of this panel into several other panels
				//Component 1
				JPanel logPanel = new JPanel();
				logPanel.setLayout(new MigLayout("", "[]push[]"));
				logPanel.setOpaque(false);
				
		  		title = new JLabel("All Data");
		  		title.setFont(new Font("Sans Serif", Font.PLAIN, 28));
		  		title.setForeground(new Color(60, 180, 220, 255));
				logPanel.add(title, "pushx, growx, span, wrap");
				title.setVisible(true);
				
		    	logCheck = new JCheckBox("Log Transform");
		    	logCheck.setOpaque(false);
		    	logCheck.setFont(new Font("Sans Serif", Font.BOLD, 18));
		    	logPanel.add(logCheck, "grow, push");
		    	
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
		    	adjust_button.setOpaque(true);
		    	adjust_button.setBackground(new Color(60, 180, 220, 255));
		    	adjust_button.setForeground(Color.white);
				Dimension d = adjust_button.getPreferredSize();
				d.setSize(d.getWidth()*1.5, d.getHeight()*1.5);
				adjust_button.setFont(new Font("Sans Serif", Font.PLAIN, 18));
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
		  private JLabel title;
	      
			private static final long serialVersionUID = 1L;
			
			//Constructor
			public GeneAdjustPanel() {
				
		  		//set this panel's layout
		  		this.setLayout(new MigLayout("", "[]push[]"));
		  		this.setOpaque(false);
		  		
		  		title = new JLabel("Rows");
		  		title.setFont(new Font("Sans Serif", Font.PLAIN, 28));
		  		title.setForeground(new Color(60, 180, 220, 255));
				this.add(title, "pushx, growx, span, wrap");
				title.setVisible(true);
		    	
		  		//create checkbox
		  		centerGenes = new JCheckBox("Center");
		  		centerGenes.setFont(new Font("Sans Serif", Font.BOLD, 18));
		  		centerGenes.setOpaque(false);
		  		this.add(centerGenes, "wrap");
		  		
		  		//Create a new ButtonGroup
		  		bg = new ButtonGroup();
		  		
		  		mean = new JRadioButton("Mean", true);
		  		mean.setFont(new Font("Sans Serif", Font.BOLD, 18));
		  		median = new JRadioButton("Median", false);
		  		median.setFont(new Font("Sans Serif", Font.BOLD, 18));
		  		mean.setOpaque(false);
		  		mean.setEnabled(false);
		  		median.setOpaque(false);
		  		median.setEnabled(false);
		  		
		  		//add Radio Buttons to ButtonGroup
		  		bg.add(mean);
		  		bg.add(median);
		  		
		  		//Add Buttons to panel
		  		this.add(mean, "wrap");
		  		this.add(median, "wrap");
		  		
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
		  			
		  		normGenes = new JCheckBox ("Normalize");
		  		normGenes.setFont(new Font("Sans Serif", Font.BOLD, 18));
		  		normGenes.setOpaque(false);
		  		this.add(normGenes);
		  	}
	  }
  
	  class ArrayAdjustPanel extends JPanel {
		  
		  //Local variables for this class
	      private JRadioButton mean;
	      private JRadioButton median;
	      private ButtonGroup bg;
	      private JLabel title;
	      
	      private static final long serialVersionUID = 1L;
			
			//Constructor
			public ArrayAdjustPanel() {
				//set this panel's layout
				this.setLayout(new MigLayout("", "[]push[]"));
				this.setOpaque(false);
				
		  		title = new JLabel("Columns");
		  		title.setFont(new Font("Sans Serif", Font.PLAIN, 28));
		  		title.setForeground(new Color(60, 180, 220, 255));
				this.add(title, "pushx, growx, span, wrap");
				title.setVisible(true);
				
				//create checkbox
				centerArrays = new JCheckBox("Center");
				centerArrays.setFont(new Font("Sans Serif", Font.BOLD, 18));
				centerArrays.setOpaque(false);
				this.add(centerArrays, "wrap");
				
				//Create a new ButtonGroup
				bg = new ButtonGroup();
				
				mean = new JRadioButton("Mean", true);
				mean.setFont(new Font("Sans Serif", Font.BOLD, 18));
				median = new JRadioButton("Median", false);
				median.setFont(new Font("Sans Serif", Font.BOLD, 18));
				mean.setOpaque(false);
				mean.setEnabled(false);
				median.setOpaque(false);
				median.setEnabled(false);
				
				//add Radio Buttons to ButtonGroup
				bg.add(mean);
				bg.add(median);
				
				//Add Buttons to panel
				this.add(mean, "wrap");
				this.add(median, "wrap");
				
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
					
				normArrays = new JCheckBox ("Normalize");
				normArrays.setFont(new Font("Sans Serif", Font.BOLD, 18));
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
