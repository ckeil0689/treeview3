///* BEGIN_HEADER                                              Java TreeView
// *
// * $Author: alokito $
// * $RCSfile: HeaderFinder.java,v $
// * $Revision: 1.1 $
// * $Date: 2009-08-26 11:48:27 $
// * $Name:  $
// *
// * This file is part of Java TreeView
// * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
// *
// * This software is provided under the GNU GPL Version 2. In particular, 
// *
// * 1) If you modify a source file, make a comment in it containing your name and the date.
// * 2) If you distribute a modified version, you must do it under the GPL 2.
// * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
// *
// * A full copy of the license can be found in gpl.txt or online at
// * http://www.gnu.org/licenses/gpl.txt
// *
// * END_HEADER 
// */
//package Cluster;
//
//// for summary view...
//import java.awt.BorderLayout;
//import java.awt.Color;
//import java.awt.Component;
//import java.awt.Dimension;
//import java.awt.FlowLayout;
//import java.awt.Frame;
//import java.awt.GridLayout;
//import java.awt.event.*;
//
//import javax.swing.*;
//
//import edu.stanford.genetics.treeview.TreeSelectionI;
//import edu.stanford.genetics.treeview.ViewFrame;
///**
// *  The purpose of this class is to allow searching on HeaderInfo objects.
// * The display of the headers and the matching is handled by this class,
// * whereas the actual manipulation of the selection objects and the 
// * associated views is handled by the relevant subclass.
// * 
// * @author ChrisK
// *
// */
//public abstract class ClusterLoad extends JDialog {
//
//	private static final long serialVersionUID = 1L;
//	
////"Search Gene Text for Substring"
//  protected ClusterLoad(ViewFrame f, String title) {
//	this((Frame) f, title);
//	this.viewFrame = f;
//  }
//  
//  private ClusterLoad(Frame f, String title) {
//	super(f, title);
//	this.viewFrame = null;
//	
//	//Create the mainPanel
//	JPanel mainPanel = new JPanel();
//	mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
//	
//	//Add closeButton to mainPanel
//	mainPanel.add(new ClosePanel(), mainPanel);
//	
//	//Add the mainPanel to the ContentPane
//	getContentPane().add(mainPanel);
//	addWindowListener(new WindowAdapter () {
//		public void windowClosing(WindowEvent we) {
//		    setVisible(false);
//		}
//	    });
//	pack();
//    }
//  
//
//	//FILTER COMPONENTS
//	ValueTextField present_text;
//	JCheckBox caseBox;
//	
//    class PresentPanel extends JPanel {	
//	  public PresentPanel() {
//		setBackground( Color.GREEN );
//		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
//		setMaximumSize(new Dimension(400, 50));
//		
//		caseBox = new JCheckBox("% Present >=");
//		add(caseBox);
//	    present_text = new ValueTextField(10);
//	    present_text.addActionListener(present_text);
//	    add(present_text);
//	  }
//	}
//    
//	ValueTextField SD_text;
//	JCheckBox caseBox2;
//	
//    class SDPanel extends JPanel {	
//	  public SDPanel() {
//		setBackground( Color.RED );
//		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
//		setMaximumSize(new Dimension(400, 50));
//		
//		caseBox2 = new JCheckBox("SD (Gene Vector)");
//		add(caseBox2);
//	    SD_text = new ValueTextField(5);
//	    SD_text.addActionListener(SD_text);
//	    add(SD_text);
//	  }
//	}
//    
//	ValueTextField absolute_text1;
//	ValueTextField absolute_text2;
//	JCheckBox caseBox3;
//	JLabel label;
//	
//    class AbsolutePanel extends JPanel {	
//	  public AbsolutePanel() {
//		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
//		setMaximumSize(new Dimension(400, 50));
//		
//		caseBox3 = new JCheckBox("At least");
//		add(caseBox3);
//	    absolute_text1 = new ValueTextField(5);
//	    absolute_text1.addActionListener(absolute_text1);
//	    add(absolute_text1);
//	    label = new JLabel("observations with abs(Val)>=");
//	    add(label);
//	    absolute_text2 = new ValueTextField(5);
//	    absolute_text2.addActionListener(absolute_text2);
//	    add(absolute_text2);
//	  }
//	}
//    
//	
//    //COMMON COMPONENTS
//    /**
//     * Used to create the close-button in the Filter window
//     * @author ChrisK
//     *
//     */
//    class ClosePanel extends JPanel {
//	public ClosePanel () {
// 	    JButton close_button = new JButton("Close");
//	    close_button.addActionListener(new ActionListener() {
//		    public void actionPerformed(ActionEvent e) {
//			ClusterLoad.this.setVisible(false);
//		    }
//		});
//	    add(close_button);
//		}
//    }
//    
//    class ValueTextField extends JTextField implements ActionListener {
//	    // why does java make me write this dumb constructor?
//		//cols = size of textfield
//	    ValueTextField(int cols) {super(cols);}
//
//	    public void actionPerformed(ActionEvent e) {
//	    	
//	    }
//	}
//        
//
//	protected TreeSelectionI geneSelection;
//	protected ViewFrame viewFrame;
//    private JButton apply_button, accept_button;
//}
