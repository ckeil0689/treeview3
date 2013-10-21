package Cluster;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;

import net.miginfocom.swing.MigLayout;
import Cluster.ClusterView.HeaderPanel;
import Cluster.ClusterView.InitialPanel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.TreeViewFrame;

/**
 * Subclass to be added to the options panel.
 * Offers the choice between different clustering methods and generates
 * the panel in which ProgressBars and calculation status are displayed.
 * @author CKeil
 *
 */
class FinalOptionsPanel extends JPanel {	
		  
	private static final long serialVersionUID = 1L;
		
	//Instance variables
	private TreeViewFrame viewFrame;
	private JButton cluster_button; 
	private JButton back_button;
	private JButton cancel_button;
	private JButton dendro_button;
	private JLabel status1; 
	private JLabel status2;
	private JLabel method;
	private JLabel error1;
	private JLabel error2;
	private JLabel row_clusters;
	private JLabel col_clusters;
	private JTextField enterRC;
	private JTextField enterCC;
	private JLabel row_its;
	private JLabel col_its;
	private JTextField enterRIt;
	private JTextField enterCIt;
	private JProgressBar pBar;
	private JProgressBar pBar2;
	private JProgressBar pBar3;
	private JProgressBar pBar4;
	private JProgressBar pBar5;
	private JPanel loadPanel;
	private JPanel choicePanel;
	private JPanel mainPanel;
	private HeaderPanel head1;
	private InitialPanel initialPanel;
	private JPanel buttonPanel;
	private JPanel optionsPanel;
	private final SwingWorker<Void, Void> worker;
	private JComboBox<String> clusterChoice;
	private JComboBox<String> geneCombo;
	private JComboBox<String> arrayCombo;
	private final String[] clusterMethods = {"Single Linkage", 
			"Centroid Linkage", "Average Linkage", "Complete Linkage"};
	private String path;
	private String linkageMethod = null;
	private File file;
	private ClusterModel outer;
	private int row_clusterN = 0;
	private int row_iterations = 0;
	private int col_clusterN = 0;
	private int col_iterations = 0;
	
	private final int TEXTFIELD_SIZE = 10;
	
	
	//Colors
	private final Color BLUE1 = new Color(60, 180, 220, 255);
	
	    
	/**
	 * Constructor
	 * Setting up layout and functionality for buttons.
	 */
	public FinalOptionsPanel(final ClusterView cView,
			final boolean hierarchical) {
		
		this.setLayout(new MigLayout());
		this.setBackground(Color.white);
		
		this.viewFrame = cView.getViewFrame();
		this.mainPanel = cView.getMainPanel();
		this.optionsPanel = cView.getOptionsPanel();
		this.buttonPanel = cView.getButtonPanel();
		this.outer = cView.getClusterModel();
		this.geneCombo = cView.getGeneCombo();
		this.arrayCombo = cView.getArrayCombo();
		this.initialPanel = cView.getInitialPanel();
		this.head1 = cView.getHeadPanel();
		
		final ClusterFrame clusterFrame = 
				new ClusterFrameWindow(viewFrame, outer);
		
		//worker thread for calculation off the EDT to give 
		//Swing elements time to update
		worker = new SwingWorker<Void, Void>() {	
			
			@Override
			public Void doInBackground() {

	        	try {
	        		
	        		if(!hierarchical) {
		        		row_clusterN = Integer.parseInt(enterRC.getText());
		        		col_clusterN = Integer.parseInt(enterCC.getText());
		        		
		        		row_iterations = Integer.parseInt(enterRIt.getText());
		        		col_iterations = Integer.parseInt(enterCIt.getText());
	        		}
	        		
	        		ClusterProcessor clusterTarget = 
	        				new ClusterProcessor( cView, pBar, pBar2, pBar3, 
	        						pBar4, linkageMethod, row_clusterN, 
	        						row_iterations, col_clusterN, 
	        						col_iterations);
	        		
	        		clusterTarget.cluster(hierarchical);
	        		
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
		method.setFont(new Font("Sans Serif", Font.PLAIN, 28));
		
		//Clickable Panel to call ClusterFrame
		ClickableIcon infoIcon = 
				new ClickableIcon(clusterFrame, "infoIcon.png");
    	
		//Linkage choice drop-down menu
		clusterChoice = new JComboBox<String>(clusterMethods);
		clusterChoice = setComboLayout(clusterChoice);
		
		row_clusters = new JLabel("# of Row Clusters: ");
		row_clusters.setFont(new Font("Sans Serif", Font.PLAIN, 22));
		
		col_clusters = new JLabel("# of Column Clusters: ");
		col_clusters.setFont(new Font("Sans Serif", Font.PLAIN, 22));
		
		row_its = new JLabel("# of Row Iterations: ");
		row_its.setFont(new Font("Sans Serif", Font.PLAIN, 22));
		
		col_its = new JLabel("# of Column Iterations: ");
		col_its.setFont(new Font("Sans Serif", Font.PLAIN, 22));
		
		enterRC = new JTextField(TEXTFIELD_SIZE);
		enterCC = new JTextField(TEXTFIELD_SIZE);
		
//		row_clusterN = Integer.parseInt(enterRC.getText());
//		col_clusterN = Integer.parseInt(enterCC.getText());
		
		enterRIt = new JTextField(TEXTFIELD_SIZE);
		enterCIt = new JTextField(TEXTFIELD_SIZE);
		
//		row_iterations = Integer.parseInt(enterRIt.getText());
//		col_iterations = Integer.parseInt(enterCIt.getText());
		
		//ProgressBar Component
		loadPanel = new JPanel();
		loadPanel.setLayout(new MigLayout());
		loadPanel.setBackground(Color.white);
		loadPanel.setBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    	
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
				
				cView.setInitialPanel();
				
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
    	cluster_button = setButtonLayout(cluster_button);
    	cluster_button.addActionListener(new ActionListener(){
	    		
			@Override
			public void actionPerformed(ActionEvent arg0) {
					
				final String choice = (String)geneCombo.getSelectedItem();
				final String choice2 = (String)arrayCombo.getSelectedItem();
				
				if(hierarchical) {
					linkageMethod = (String)clusterChoice.getSelectedItem();
					
				} else {
					
				}
				
				//needs at least one box to be selected 
				//otherwise display error
				if(!choice.contentEquals("Do Not Cluster")
						||!choice2.contentEquals("Do Not Cluster")){
					
					loadPanel.removeAll();
					dendro_button.setEnabled(false);
					buttonPanel.remove(cluster_button);
					buttonPanel.remove(back_button);
					
					//ProgressBars
					pBar = new JProgressBar();
					pBar = setPBarLayout(pBar);
					pBar.setString("Row Distance Matrix");
					
					pBar2 = new JProgressBar();
					pBar2 = setPBarLayout(pBar2);
					pBar2.setString("Row Clustering");
					
					pBar3 = new JProgressBar();
					pBar3 = setPBarLayout(pBar3);
					pBar3.setString("Column Distance Matrix");
					
					pBar4 = new JProgressBar();
					pBar4 = setPBarLayout(pBar4);
					pBar4.setString("Column Clustering");
					
					pBar5 = new JProgressBar();
					pBar5 = setPBarLayout(pBar5);
					pBar5.setString("Saving");
					
					//Button to cancel process
					cancel_button = new JButton("Cancel");
					cancel_button = setButtonLayout(cancel_button);
					cancel_button.addActionListener(new ActionListener(){

						@Override
						public void actionPerformed(ActionEvent arg0) {
							
							worker.cancel(true);
							buttonPanel.removeAll();
							
							optionsPanel.remove(cView.getFinalPanel());
							optionsPanel.remove(loadPanel);
							
							if(hierarchical) {
								cView.setHPanel();
								
							} else { 
								cView.setKMPanel();
							}
							
							optionsPanel.add(cView.getFinalPanel(), 
									"pushx, alignx 50%, wrap");
							
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
						loadPanel.add(pBar, "push, grow, span, wrap");
						loadPanel.add(pBar2, "push, grow, span, wrap");
						
					} else if(!choice2.contentEquals("Do Not Cluster")) {
						loadPanel.add(pBar3, "pushx, growx, span, wrap");
						loadPanel.add(pBar4, "pushx, growx, span, wrap");
					}
				
					optionsPanel.add(loadPanel, "push, grow, span, wrap");
					
					buttonPanel.add(cancel_button, "pushx, alignx 50%");
					buttonPanel.add(dendro_button, "pushx, alignx 50%");
					mainPanel.add(buttonPanel, 
							"pushx, alignx 50%, height 15%::");
					
					mainPanel.revalidate();
					mainPanel.repaint();

					//start new cluster process
					worker.execute();
					
				} else {
					
					error1 = new JLabel("Woah, that's too quick!");
					error1.setFont(new Font("Sans Serif", Font.PLAIN, 22));
					error1.setForeground(new Color(240, 80, 50, 255));
					
					error2 = new JLabel("Please select either a " +
							"similarity metric for rows, columns, " +
							"or both to begin clustering!");
					error2.setFont(new Font("Sans Serif", Font.PLAIN, 22));
					
					loadPanel.add(error1, "alignx 50%, span, wrap");
					loadPanel.add(error2, "alignx 50%, span");
					optionsPanel.add(loadPanel, "alignx 50%, pushx, span");
					
					mainPanel.revalidate();
					mainPanel.repaint();
				}
			}	
    	});
    	
    	if(hierarchical) {
	    	choicePanel.add(method, "alignx 50%, pushx");
	    	choicePanel.add(infoIcon, "pushx, alignx 50%, wrap");
	    	choicePanel.add(clusterChoice, "span, alignx 50%, wrap");
	    	
    	} else {
    		choicePanel.add(row_clusters);
    		choicePanel.add(col_clusters, "wrap");
    		choicePanel.add(enterRC);
    		choicePanel.add(enterCC, "wrap");
    		choicePanel.add(row_its);
    		choicePanel.add(col_its, "wrap");
    		choicePanel.add(enterRIt);
    		choicePanel.add(enterCIt, "wrap");
    	}
    	
    	dendro_button.setEnabled(false);
  		buttonPanel.add(back_button, "alignx 50%, pushx");
  		buttonPanel.add(cluster_button, "alignx 50%, pushx");
  		
    	this.add(choicePanel, "alignx 50%, push, grow, wrap");
	}
	
	//Layout setups from some Swing elements
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
		pBar.setMaximumSize(new Dimension(2000, 40));
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
}
