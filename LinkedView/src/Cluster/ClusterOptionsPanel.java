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
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;

import net.miginfocom.swing.MigLayout;
//import Cluster.ClusterView.InitialPanel;
import edu.stanford.genetics.treeview.DataModel;
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
class ClusterOptionsPanel extends JPanel {	
		  
	private static final long serialVersionUID = 1L;
	
	private static final Color RED1 = new Color(240, 80, 50, 255);
	private final static Color BG_COLOR = new Color(252, 252, 252, 255);
	
	//Instance Variables
	private TreeViewFrame viewFrame;
	private JButton cluster_button; 
	private JButton back_button;
	private JButton cancel_button;
	private JButton dendro_button;
	private JLabel head1;
	private JLabel status1; 
	private JLabel status2;
	private JLabel method;
	private JLabel error1;
	private JLabel error2;
	private JLabel clusters;
	private JSpinner enterRC;
	private JSpinner enterCC;
	private JLabel its;
	private JSpinner enterRIt;
	private JSpinner enterCIt;
	private JProgressBar pBar;
	private JProgressBar pBar2;
	private JProgressBar pBar3;
	private JProgressBar pBar4;
	private JPanel loadPanel;
	private JPanel mainPanel;
	private JPanel buttonPanel;
	private JPanel optionsPanel;
//	private InitialPanel initialPanel;
	private JComboBox clusterChoice;
	private JComboBox geneCombo;
	private JComboBox arrayCombo;
	
	private String path;
	private File file;
	private DataModel outer;
	
	private Font fontS;
	private Font fontL;
	
	private String linkageMethod = null;
	private int row_clusterN = 0;
	private int row_iterations = 0;
	private int col_clusterN = 0;
	private int col_iterations = 0;
	
	private final SwingWorker<Void, Void> worker;
	private final String[] clusterMethods = {"Single Linkage", 
			"Centroid Linkage", "Average Linkage", "Complete Linkage"};
		    
	/**
	 * Constructor
	 * Setting up layout and functionality for buttons.
	 */
	public ClusterOptionsPanel(final ClusterView cView,
			final boolean hierarchical) {
		
		this.setLayout(new MigLayout());
		this.setBackground(BG_COLOR);
		
		this.viewFrame = cView.getViewFrame();
		this.mainPanel = cView.getMainPanel();
		this.optionsPanel = cView.getOptionsPanel();
		this.buttonPanel = cView.getButtonPanel();
		this.outer = cView.getDataModel();
		this.geneCombo = cView.getGeneCombo();
		this.arrayCombo = cView.getArrayCombo();
//		this.initialPanel = cView.getInitialPanel();
		this.head1 = cView.getTitle();
		this.fontS = cView.getSmallFont();
		this.fontL = cView.getLargeFont();
		
		final ClusterFrame clusterFrame = 
				new ClusterFrameWindow(viewFrame, outer);
		
		//worker thread for calculation off the EDT to give 
		//Swing elements time to update
		worker = new SwingWorker<Void, Void>() {	
			
			@Override
			public Void doInBackground() {

	        	try {
	        		//Set integers only if KMeans options are shown
	        		if(!hierarchical) {
		        		row_clusterN = (Integer) enterRC.getValue();
		        		col_clusterN = (Integer) enterCC.getValue();
		        		
		        		row_iterations = (Integer) enterRIt.getValue();
		        		col_iterations = (Integer) enterCIt.getValue();
	        		}
	        		
	        		//Setup a ClusterProcessor
	        		ClusterProcessor clusterTarget = 
	        				new ClusterProcessor( cView, pBar, pBar2, pBar3, 
	        						pBar4, linkageMethod, row_clusterN, 
	        						row_iterations, col_clusterN, 
	        						col_iterations);
	        		
	        		//Begin the actual clustering, hierarchical or kmeans
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
				status1.setFont(fontS);
				
				status2 = new JLabel("File Path: " + path);
				status2.setFont(fontS);
				
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
		//Label
		method = new JLabel("Linkage Method:");
		method.setFont(fontL);
		
		//Clickable Panel to call ClusterFrame
		ClickableIcon infoIcon = 
				new ClickableIcon(clusterFrame, "infoIcon.png");
    	
		//Linkage choice drop-down menu
		clusterChoice = new JComboBox(clusterMethods);
		clusterChoice = ClusterView.setComboLayout(clusterChoice);
		
		clusters = new JLabel("Clusters: ");
		clusters.setFont(fontL);
		
		its = new JLabel("Iterations: ");
		its.setFont(fontL);
		
		enterRC = setupSpinner();
		enterCC = setupSpinner();
		enterRIt = setupSpinner();
		enterCIt = setupSpinner();
		
		//ProgressBar Component
		loadPanel = new JPanel();
		loadPanel.setLayout(new MigLayout());
		loadPanel.setBackground(BG_COLOR);
		loadPanel.setBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    	
  		//Button to go back to data preview
    	back_button = new JButton("< Back");
    	back_button = ClusterView.setButtonLayout(back_button);
		back_button.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				mainPanel.removeAll();
				optionsPanel.removeAll();
				buttonPanel.removeAll();
				
				head1.setText("Data Preview");
				
//				cView.setInitialPanel();
				
				mainPanel.add(head1, "alignx 50%, pushx, wrap");
				mainPanel.add(cView, "grow, push, span, wrap");
				mainPanel.add(buttonPanel, "alignx 50%, height 15%::");
				
				mainPanel.revalidate();
				mainPanel.repaint();	
			}
    	});
		
		//Button to show DendroView
    	dendro_button = new JButton("Clustergram > ");
    	dendro_button = ClusterView.setButtonLayout(dendro_button);
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
    	cluster_button = ClusterView.setButtonLayout(cluster_button);
    	cluster_button.addActionListener(new ActionListener() {
	    		
			@Override
			public void actionPerformed(ActionEvent arg0) {
					
				final String choice = (String)geneCombo.getSelectedItem();
				final String choice2 = (String)arrayCombo.getSelectedItem();
				
				if(hierarchical) {
					linkageMethod = (String)clusterChoice.getSelectedItem();
				}
				
				//needs at least one box to be selected 
				//otherwise display error
				if(!choice.contentEquals("Do Not Cluster")
						||!choice2.contentEquals("Do Not Cluster")) {
					
					loadPanel.removeAll();
					dendro_button.setEnabled(false);
					buttonPanel.remove(cluster_button);
					buttonPanel.remove(back_button);
					
					//ProgressBars
					pBar = ClusterView.setPBarLayout(pBar, 
							"Row Distance Matrix");
					
					pBar2 = ClusterView.setPBarLayout(pBar2, "Row Clustering");
					
					pBar3 = ClusterView.setPBarLayout(pBar3, 
							"Column Distance Matrix");
					
					pBar4 = ClusterView.setPBarLayout(pBar4, 
							"Column Clustering");
					
					//Button to cancel process
					cancel_button = new JButton("Cancel");
					cancel_button = ClusterView.setButtonLayout(cancel_button);
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
									"pushx, alignx 50%, span, wrap");
							
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
						loadPanel.add(pBar, "pushx, growx, span, wrap");
						loadPanel.add(pBar2, "pushx, growx, span, wrap");
						
					} else if(!choice2.contentEquals("Do Not Cluster")) {
						loadPanel.add(pBar3, "pushx, growx, span, wrap");
						loadPanel.add(pBar4, "pushx, growx, span, wrap");
					}
				
					optionsPanel.add(loadPanel, "push, growx, span, wrap");
					
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
					error1.setFont(fontS);
					error1.setForeground(RED1);
					
					error2 = new JLabel("Please select either a " +
							"similarity metric for rows, columns, " +
							"or both to begin clustering!");
					error2.setFont(fontS);
					
					loadPanel.add(error1, "alignx 50%, span, wrap");
					loadPanel.add(error2, "alignx 50%, span");
					optionsPanel.add(loadPanel, "alignx 50%, pushx, span");
					
					mainPanel.revalidate();
					mainPanel.repaint();
				}
			}	
    	});
    	
    	if(hierarchical) {
	    	this.add(method, "pushx");
	    	this.add(clusterChoice, "alignx 50%");
	    	this.add(infoIcon);
	    	
    	} else {
    		this.add(clusters, "pushx");
    		this.add(enterRC, "pushx");
    		this.add(enterCC, "pushx, wrap");
    		this.add(its, "pushx");
    		this.add(enterRIt, "pushx");
    		this.add(enterCIt, "pushx");
    	}
    	
    	dendro_button.setEnabled(false);
  		buttonPanel.add(back_button, "alignx 50%, pushx");
  		buttonPanel.add(cluster_button, "alignx 50%, pushx");
	}
	
	/**
	 * Method to setup the look of an editable TextField
	 * @return
	 */
	public JSpinner setupSpinner() {
		
		SpinnerNumberModel amountChoice = new SpinnerNumberModel(0, 0, 5000, 1);
		JSpinner jft = new JSpinner(amountChoice);
		
		Dimension d = jft.getPreferredSize();
		d.setSize(d.getWidth(), d.getHeight()*2);
		jft.setPreferredSize(d);
		jft.setFont(fontS);
		
		return jft;
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
