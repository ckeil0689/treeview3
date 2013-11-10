package edu.stanford.genetics.treeview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import Cluster.DataViewPanel;

/**
 * Subclass to add the initial JPanel containing some info and the data
 * preview table to the background panel.
 * @author CKeil
 *
 */
class LoadCheckView extends JPanel {	

	private static final long serialVersionUID = 1L;
	
	//Colors
	private static final Color BLUE1 = new Color(60, 180, 220, 255);
	private static final Color RED1 = new Color(240, 80, 50, 255);
	private static final Color BG_COLOR = new Color(252, 252, 252, 255);
	
	//Two Font Sizes
	private static Font fontS = new Font("Sans Serif", Font.PLAIN, 18);
	private static Font fontL = new Font("Sans Serif", Font.PLAIN, 24);
	
	//Instance variables
	private int nRows; 
	private int nCols; 
	private int sumMatrix; 
	private JLabel sumM; 
	private JLabel label2; 
	private JLabel label3; 
	private JLabel numColLabel; 
	private JLabel numRowLabel;
	private JButton loadNewButton;
	private JButton advanceButton;
	private JPanel numPanel;
	private JPanel buttonPanel;
    
	/**
	 * Constructor
	 * Setting up the layout of the panel.
	 */
	public LoadCheckView(DataModel dataModel, final TreeViewFrame viewFrame) {
		
		this.setLayout(new MigLayout());
		this.setBackground(BG_COLOR);
		
    	HeaderInfo infoArray = dataModel.getArrayHeaderInfo();
    	HeaderInfo infoGene = dataModel.getGeneHeaderInfo();
    	
    	nCols = infoArray.getNumHeaders();
    	nRows = infoGene.getNumHeaders();
   
    	label2 = new JLabel("Matrix Dimensions:");
    	label2.setFont(fontL);
    	
    	//Matrix Information
    	numPanel = new JPanel();
    	numPanel.setLayout(new MigLayout());
    	numPanel.setOpaque(false);
    	
    	numColLabel = new JLabel(nCols + " columns");
    	numColLabel.setFont(fontS);
    	numColLabel.setForeground(RED1);
    	
    	numRowLabel = new JLabel(nRows + " rows X ");
    	numRowLabel.setFont(fontS);
    	numRowLabel.setForeground(RED1);
    	
    	label3 = new JLabel("Data Points:");
    	label3.setFont(fontS);
    	
    	sumMatrix = nCols * nRows;
    	sumM = new JLabel(Integer.toString(sumMatrix));
    	sumM.setFont(fontS);
    	sumM.setForeground(RED1);
    	
    	//ButtonPanel
    	buttonPanel = new JPanel();
    	buttonPanel.setLayout(new MigLayout());
    	buttonPanel.setOpaque(false);
    	 
    	//Data Preview
    	DataViewPanel dataView = new DataViewPanel(dataModel);
    	
		loadNewButton = new JButton("Load New File");
		setButtonLayout(loadNewButton);
		loadNewButton.setBackground(BLUE1);
		loadNewButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				viewFrame.openFile();
			}
		});
		
		advanceButton = new JButton("Next >");
		setButtonLayout(advanceButton);
		advanceButton.setBackground(BLUE1);
		advanceButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				viewFrame.resetLayout();
			}
		});
  		
  		buttonPanel.add(loadNewButton, "alignx 50%, pushx");
    	buttonPanel.add(advanceButton, "alignx 50%, pushx");
    	
    	numPanel.add(numRowLabel, "span, split 2, alignx 50%");
    	numPanel.add(numColLabel, "wrap");
    	numPanel.add(label3, "span, split 2, alignx 50%");
    	numPanel.add(sumM, "alignx 50%, wrap");
    	
    	this.add(label2, "alignx 50%, wrap");
    	this.add(numPanel, "alignx 50%, pushx, wrap");
    	this.add(dataView, "push, grow, alignx 50%, width 80%:95%:95%, wrap");
    	this.add(buttonPanel, "alignx 50%, pushx");
	  }
	
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
}
